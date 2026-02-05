package me.apocalipsis.state;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.ui.MessageBus;

public class StateManager {

    private final Apocalipsis plugin;
    private final TimeService timeService;
    private final File stateFile;
    private FileConfiguration stateConfig; // Cache en memoria

    private ServerState currentState = ServerState.DETENIDO;
    private String activeDisasterId = null;
    private String lastDisasterId = null;
    // Constantes de seguridad
    private static final long MIN_DAY_INTERVAL_MS = 3600000L; // 1 hora mínimo entre días
    private static final long MAX_DAY_INTERVAL_MS = 172800000L; // 48 horas máximo
    private static final long DEFAULT_DAY_INTERVAL_MS = 86400000L; // 24 horas por defecto
    private static final int MAX_DAY_VALUE = 36500; // 100 años máximo
    
    private int currentDay = 0;
    private long lastEndEpochMs = 0L;
    private long nextDayEpochMs = 0L; // Timestamp del próximo cambio de día
    private long lastDayChangeMs = 0L; // Último cambio de día ejecutado
    private boolean dayChangeLock = false; // Lock para evitar cambios concurrentes
    private boolean safeModeActive = false;
    private boolean prepForzada = false;
    private boolean saving = false;

    public StateManager(Apocalipsis plugin, TimeService timeService, MessageBus messageBus) {
        this.plugin = plugin;
        this.timeService = timeService;
        this.stateFile = new File(plugin.getDataFolder(), "state.yml");
        reloadStateConfig();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // HELPERS state.yml (fuente ÚNICA de verdad - cero-drift)
    // ═══════════════════════════════════════════════════════════════════
    
    private void reloadStateConfig() {
        if (!stateFile.exists()) {
            try {
                stateFile.getParentFile().mkdirs();
                stateFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Error creando state.yml: " + e.getMessage());
            }
        }
        this.stateConfig = YamlConfiguration.loadConfiguration(stateFile);
    }
    
    /**
     * Obtiene el estado actual desde state.yml (estado: DETENIDO|PREPARACION|ACTIVO|SAFE_MODE)
     */
    public String getEstado() {
        if (stateConfig == null) reloadStateConfig();
        return stateConfig.getString("estado", ServerState.DETENIDO.name());
    }
    
    /**
     * Establece el estado en state.yml y persiste
     */
    public void setEstado(String estado) {
        if (stateConfig == null) reloadStateConfig();
        stateConfig.set("estado", estado);
        saveState();
        
        // Sincronizar con currentState (backward compatibility)
        try {
            this.currentState = ServerState.valueOf(estado);
        } catch (IllegalArgumentException e) {
            this.currentState = ServerState.DETENIDO;
        }
    }
    
    /**
     * Obtiene un long desde state.yml
     */
    public long getLong(String key, long def) {
        if (stateConfig == null) reloadStateConfig();
        return stateConfig.getLong(key, def);
    }
    
    /**
     * Establece un long en state.yml (NO persiste automáticamente, llamar saveState())
     */
    public void setLong(String key, long value) {
        if (stateConfig == null) reloadStateConfig();
        stateConfig.set(key, value);
    }
    
    /**
     * Obtiene un string desde state.yml
     */
    public String getString(String key, String def) {
        if (stateConfig == null) reloadStateConfig();
        return stateConfig.getString(key, def);
    }
    
    /**
     * Establece un string en state.yml (NO persiste automáticamente, llamar saveState())
     */
    public void setString(String key, String value) {
        if (stateConfig == null) reloadStateConfig();
        stateConfig.set(key, value);
    }
    
    /**
     * Persiste state.yml a disco
     */
    public synchronized void saveState() {
        if (saving) {
            plugin.getLogger().warning("[State] Guardado ignorado: ya hay una operación en curso.");
            return;
        }
        saving = true;
        try {
            // Asegurar que no se concatene contenido viejo:
            org.bukkit.configuration.file.YamlConfiguration yaml = new org.bukkit.configuration.file.YamlConfiguration();
            
            // Establecer todas las claves desde las variables actuales
            yaml.set("estado", currentState.name());
            yaml.set("desastre_actual", activeDisasterId != null ? activeDisasterId : "");
            yaml.set("ultimo_desastre", lastDisasterId != null ? lastDisasterId : "");
            yaml.set("current_day", currentDay);
            yaml.set("next_day_epoch_ms", nextDayEpochMs);
            yaml.set("last_day_change_ms", lastDayChangeMs);
            yaml.set("last_end_epoch_ms", lastEndEpochMs);
            yaml.set("prep_forzada", prepForzada);
            
            // Claves auxiliares de tiempo (si existen en stateConfig actual)
            if (stateConfig != null) {
                yaml.set("start_epoch_ms", stateConfig.getLong("start_epoch_ms", 0L));
                yaml.set("end_epoch_ms", stateConfig.getLong("end_epoch_ms", 0L));
                yaml.set("remaining_seconds", stateConfig.getInt("remaining_seconds", 0));
                yaml.set("planned_seconds", stateConfig.getInt("planned_seconds", 900));
            } else {
                yaml.set("start_epoch_ms", 0L);
                yaml.set("end_epoch_ms", 0L);
                yaml.set("remaining_seconds", 0);
                yaml.set("planned_seconds", 900);
            }
            
            yaml.save(stateFile);
            
            // Bonus: log para verificar guardados únicos
            plugin.getLogger().info("[State] Guardado completado (" + System.currentTimeMillis() + ")");
            
        } catch (Exception e) {
            plugin.getLogger().severe("[State] Error guardando state.yml: " + e.getMessage());
        } finally {
            saving = false;
        }
    }

    public ServerState getCurrentState() {
        return currentState;
    }

    public void setState(ServerState newState) {
        if (this.currentState != newState) {
            this.currentState = newState;
            // Sincronizar con state.yml usando claves estandarizadas
            if (stateConfig == null) reloadStateConfig();
            stateConfig.set("estado", newState.name());
            saveState();
        }
    }

    public String getActiveDisasterId() {
        return activeDisasterId;
    }

    public void setActiveDisasterId(String disasterId) {
        if (this.activeDisasterId != null) {
            this.lastDisasterId = this.activeDisasterId;
        }
        this.activeDisasterId = disasterId;
        
        // Sincronizar con state.yml usando claves estandarizadas
        if (stateConfig == null) reloadStateConfig();
        stateConfig.set("desastre_actual", disasterId);
        if (this.lastDisasterId != null) {
            stateConfig.set("ultimo_desastre", this.lastDisasterId);
        }
        saveState();
    }

    public String getLastDisasterId() {
        return lastDisasterId;
    }

    public void setLastDisasterId(String disasterId) {
        this.lastDisasterId = disasterId;
        if (stateConfig == null) reloadStateConfig();
        stateConfig.set("ultimo_desastre", disasterId);
        saveState();
    }

    public int getCurrentDay() {
        return currentDay;
    }

    public void setCurrentDay(int day) {
        this.currentDay = day;
        if (stateConfig == null) reloadStateConfig();
        stateConfig.set("current_day", day);
        saveState();
    }

    /**
     * Incrementa el día actual con múltiples capas de seguridad
     * @return true si el incremento fue exitoso, false si fue bloqueado por seguridad
     */
    public boolean incrementDay() {
        return incrementDay(false);
    }
    
    /**
     * Incrementa el día actual con múltiples capas de seguridad
     * @param force Si es true, ignora el cooldown de tiempo mínimo
     * @return true si el incremento fue exitoso, false si fue bloqueado por seguridad
     */
    public boolean incrementDay(boolean force) {
        // [SEGURIDAD 1] Verificar lock - evitar cambios concurrentes
        if (dayChangeLock) {
            plugin.getLogger().warning("[DaySafety] Cambio de día bloqueado: operación ya en curso");
            return false;
        }
        
        // [SEGURIDAD 2] Verificar cooldown mínimo (anti-spam) - SALTEABLE CON FORCE
        long now = System.currentTimeMillis();
        if (!force && lastDayChangeMs > 0 && (now - lastDayChangeMs) < MIN_DAY_INTERVAL_MS) {
            long remainingMs = MIN_DAY_INTERVAL_MS - (now - lastDayChangeMs);
            plugin.getLogger().warning("[DaySafety] Cambio de día bloqueado: cooldown activo (" + (remainingMs / 60000) + " min restantes)");
            return false;
        }
        
        // Log si se está forzando
        if (force && lastDayChangeMs > 0 && (now - lastDayChangeMs) < MIN_DAY_INTERVAL_MS) {
            long remainingMs = MIN_DAY_INTERVAL_MS - (now - lastDayChangeMs);
            plugin.getLogger().info("[DaySafety] ⚡ FORZANDO cambio de día ignorando cooldown (" + (remainingMs / 60000) + " min restantes)");
        }
        
        // [SEGURIDAD 3] Verificar límite máximo de días
        if (currentDay >= MAX_DAY_VALUE) {
            plugin.getLogger().severe("[DaySafety] Cambio de día bloqueado: límite máximo alcanzado (" + MAX_DAY_VALUE + ")");
            return false;
        }
        
        try {
            // Activar lock
            dayChangeLock = true;
            
            // [BACKUP] Guardar estado previo
            int previousDay = this.currentDay;
            long previousNextDay = this.nextDayEpochMs;
            
            // Incrementar día
            this.currentDay++;
            this.lastDayChangeMs = now;
            
            // Persistir cambios
            if (stateConfig == null) reloadStateConfig();
            stateConfig.set("current_day", this.currentDay);
            stateConfig.set("last_day_change_ms", this.lastDayChangeMs);
            
            // Intentar guardar
            saveState();
            
            // Verificar que se guardó correctamente
            reloadStateConfig();
            int savedDay = stateConfig.getInt("current_day", -1);
            
            if (savedDay != this.currentDay) {
                // [ROLLBACK] Falló el guardado
                plugin.getLogger().severe("[DaySafety] ERROR: Fallo al guardar día " + this.currentDay + ", ejecutando rollback");
                this.currentDay = previousDay;
                this.nextDayEpochMs = previousNextDay;
                stateConfig.set("current_day", previousDay);
                stateConfig.set("next_day_epoch_ms", previousNextDay);
                saveState();
                return false;
            }
            
            plugin.getLogger().info("[DaySafety] ✓ Día incrementado exitosamente: " + previousDay + " → " + this.currentDay);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().severe("[DaySafety] ERROR crítico al incrementar día: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            // Siempre liberar lock
            dayChangeLock = false;
        }
    }

    public void loadState() {
        reloadStateConfig(); // Recargar desde disco
        
        // Cargar estado (usar "estado" como clave estandarizada)
        String stateName = stateConfig.getString("estado", ServerState.DETENIDO.name());
        try {
            this.currentState = ServerState.valueOf(stateName);
        } catch (IllegalArgumentException e) {
            this.currentState = ServerState.DETENIDO;
        }

        // Cargar IDs de desastres (usar "desastre_actual" como clave estandarizada)
        this.activeDisasterId = stateConfig.getString("desastre_actual", null);
        this.lastDisasterId = stateConfig.getString("ultimo_desastre", null);
        this.currentDay = stateConfig.getInt("current_day", 0);
        this.nextDayEpochMs = stateConfig.getLong("next_day_epoch_ms", 0L);
        this.lastDayChangeMs = stateConfig.getLong("last_day_change_ms", 0L);
        this.lastEndEpochMs = stateConfig.getLong("last_end_epoch_ms", 0L);
        this.prepForzada = stateConfig.getBoolean("prep_forzada", false);
        
        // [SEGURIDAD] Verificar integridad de datos cargados
        validateAndFixDayIntegrity();

        int remainingSeconds = stateConfig.getInt("remaining_seconds", 0);
        int plannedSeconds = stateConfig.getInt("planned_seconds", 900);

        if (remainingSeconds > 0 && activeDisasterId != null) {
            // Restaurar tiempo del desastre activo
            timeService.startDisaster(activeDisasterId, plannedSeconds);
        }

        plugin.getLogger().info("Estado cargado: " + currentState + ", Desastre: " + activeDisasterId + ", Día: " + currentDay);

        // [FIX] NO reanudar automáticamente tras boot a menos que start_on_boot=true
        FileConfiguration desastresConfig = plugin.getConfigManager().getDesastresConfig();
        boolean startOnBoot = desastresConfig.getBoolean("ciclo.start_on_boot", false);
        
        if (currentState == ServerState.ACTIVO && activeDisasterId != null && remainingSeconds > 0 && startOnBoot) {
            plugin.getLogger().info("[Cycle] Reanudando desastre tras boot (start_on_boot=true): " + activeDisasterId);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getDisasterController().resumeDisaster(activeDisasterId);
            }, 20L);
        } else if (currentState == ServerState.ACTIVO && !startOnBoot) {
            plugin.getLogger().warning("[Cycle] Desastre activo detectado pero start_on_boot=false. Transicionando a DETENIDO.");
            this.currentState = ServerState.DETENIDO;
            this.activeDisasterId = null;
            
            // Escribir en stateConfig y persistir
            stateConfig.set("estado", ServerState.DETENIDO.name());
            stateConfig.set("desastre_actual", null);
            saveState();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Getters/Setters adicionales
    // ═══════════════════════════════════════════════════════════════════
    
    public long getLastEndEpochMs() {
        return lastEndEpochMs;
    }
    
    public void setLastEndEpochMs(long epochMs) {
        this.lastEndEpochMs = epochMs;
        if (stateConfig == null) reloadStateConfig();
        stateConfig.set("last_end_epoch_ms", epochMs);
        saveState();
    }
    
    public boolean isSafeModeActive() {
        return safeModeActive || currentState == ServerState.SAFE_MODE;
    }
    
    public void setSafeModeActive(boolean active) {
        this.safeModeActive = active;
        if (active && currentState != ServerState.SAFE_MODE) {
            setState(ServerState.SAFE_MODE);
        }
    }
    
    public boolean isPrepForzada() {
        return prepForzada;
    }
    
    public void setPrepForzada(boolean prepForzada) {
        this.prepForzada = prepForzada;
        if (stateConfig == null) reloadStateConfig();
        stateConfig.set("prep_forzada", prepForzada);
        saveState();
    }
    
    public boolean isSaving() {
        return saving;
    }
    
    public long getNextDayEpochMs() {
        return nextDayEpochMs;
    }
    
    /**
     * Establece el timestamp del próximo día con validaciones de seguridad
     */
    public void setNextDayEpochMs(long epochMs) {
        // [VALIDACIÓN 1] Timestamp no puede ser negativo
        if (epochMs < 0) {
            plugin.getLogger().warning("[DaySafety] Timestamp inválido (negativo): " + epochMs + ", ignorando");
            return;
        }
        
        long now = System.currentTimeMillis();
        long delta = epochMs - now;
        
        // [VALIDACIÓN 2] Timestamp no puede estar en el pasado (con margen de 1 min)
        if (delta < -60000L) {
            plugin.getLogger().warning("[DaySafety] Timestamp en el pasado: " + new java.util.Date(epochMs) + ", ajustando a ahora + 24h");
            epochMs = now + DEFAULT_DAY_INTERVAL_MS;
            delta = DEFAULT_DAY_INTERVAL_MS;
        }
        
        // [VALIDACIÓN 3] Intervalo mínimo de seguridad (1 hora)
        if (delta > 0 && delta < MIN_DAY_INTERVAL_MS) {
            plugin.getLogger().warning("[DaySafety] Intervalo muy corto (" + (delta / 60000) + " min), ajustando a mínimo (1h)");
            epochMs = now + MIN_DAY_INTERVAL_MS;
        }
        
        // [VALIDACIÓN 4] Intervalo máximo de seguridad (48 horas)
        if (delta > MAX_DAY_INTERVAL_MS) {
            plugin.getLogger().warning("[DaySafety] Intervalo muy largo (" + (delta / 3600000) + " h), ajustando a máximo (48h)");
            epochMs = now + MAX_DAY_INTERVAL_MS;
        }
        
        this.nextDayEpochMs = epochMs;
        if (stateConfig == null) reloadStateConfig();
        stateConfig.set("next_day_epoch_ms", epochMs);
        saveState();
        
        plugin.getLogger().info("[DaySafety] Próximo día programado: " + new java.util.Date(epochMs) + " (en " + ((epochMs - now) / 60000) + " min)");
    }
    
    /**
     * Valida y corrige la integridad de los datos del sistema de días
     */
    private void validateAndFixDayIntegrity() {
        long now = System.currentTimeMillis();
        boolean needsSave = false;
        
        // [CHECK 1] Día negativo o excesivamente alto
        if (currentDay < 0 || currentDay > MAX_DAY_VALUE) {
            plugin.getLogger().severe("[DaySafety] CORRUPCIÓN: current_day inválido (" + currentDay + "), reseteando a 0");
            currentDay = 0;
            needsSave = true;
        }
        
        // [CHECK 2] next_day_epoch_ms inválido
        if (nextDayEpochMs < 0) {
            plugin.getLogger().warning("[DaySafety] CORRUPCIÓN: next_day_epoch_ms negativo, corrigiendo");
            nextDayEpochMs = now + DEFAULT_DAY_INTERVAL_MS;
            needsSave = true;
        }
        
        // [CHECK 3] next_day_epoch_ms muy lejano en el futuro
        long delta = nextDayEpochMs - now;
        if (delta > MAX_DAY_INTERVAL_MS * 2) { // 96 horas
            plugin.getLogger().warning("[DaySafety] CORRUPCIÓN: next_day_epoch_ms muy lejano (" + (delta / 3600000) + "h), ajustando");
            nextDayEpochMs = now + DEFAULT_DAY_INTERVAL_MS;
            needsSave = true;
        }
        
        // [CHECK 4] Timestamp del próximo día es 0 (primera vez)
        if (nextDayEpochMs == 0L) {
            plugin.getLogger().info("[DaySafety] Inicializando sistema de días (primera ejecución)");
            nextDayEpochMs = now + DEFAULT_DAY_INTERVAL_MS;
            needsSave = true;
        }
        
        // [CHECK 5] Verificar consistencia con last_day_change_ms
        if (lastDayChangeMs > now) {
            plugin.getLogger().warning("[DaySafety] CORRUPCIÓN: last_day_change_ms en el futuro, corrigiendo");
            lastDayChangeMs = now;
            needsSave = true;
        }
        
        if (needsSave) {
            plugin.getLogger().warning("[DaySafety] Corrigiendo datos corruptos y guardando...");
            if (stateConfig == null) reloadStateConfig();
            stateConfig.set("current_day", currentDay);
            stateConfig.set("next_day_epoch_ms", nextDayEpochMs);
            stateConfig.set("last_day_change_ms", lastDayChangeMs);
            saveState();
            plugin.getLogger().info("[DaySafety] ✓ Integridad restaurada");
        } else {
            plugin.getLogger().info("[DaySafety] ✓ Integridad verificada: Día " + currentDay + ", próximo en " + (delta / 60000) + " min");
        }
    }
    
    /**
     * Obtiene el tiempo restante hasta el próximo día en milisegundos
     */
    public long getTimeUntilNextDay() {
        long now = System.currentTimeMillis();
        long remaining = nextDayEpochMs - now;
        return Math.max(0, remaining);
    }
    
    /**
     * Verifica si es momento de cambiar de día
     */
    public boolean isTimeForNewDay() {
        return System.currentTimeMillis() >= nextDayEpochMs && nextDayEpochMs > 0;
    }
}
