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
    private int currentDay = 0;
    private long lastEndEpochMs = 0L;
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

    public void incrementDay() {
        this.currentDay++;
        if (stateConfig == null) reloadStateConfig();
        stateConfig.set("current_day", this.currentDay);
        saveState();
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
        this.lastEndEpochMs = stateConfig.getLong("last_end_epoch_ms", 0L);
        this.prepForzada = stateConfig.getBoolean("prep_forzada", false);

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
}
