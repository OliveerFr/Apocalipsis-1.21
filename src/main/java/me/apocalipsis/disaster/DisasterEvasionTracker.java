package me.apocalipsis.disaster;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import me.apocalipsis.Apocalipsis;

/**
 * Sistema de rastreo de evasión de desastres.
 * Detecta jugadores que se desconectan durante desastres activos y aplica penalizaciones.
 * 
 * Requisitos para evitar penalización:
 * - Permanecer al menos 60 segundos en el desastre
 * - No desconectarse durante un desastre activo
 * 
 * Penalizaciones por evasión:
 * - Primera vez: -10% PS, advertencia
 * - Segunda vez: -20% PS, misión fallida aleatoria
 * - Tercera+ vez: -30% PS, todas las misiones fallidas, 10min cooldown
 */
public class DisasterEvasionTracker {
    
    private final Apocalipsis plugin;
    private final File dataFile;
    
    // UUID -> tiempo (ms) en que entró al desastre actual
    private final Map<UUID, Long> playerJoinTime = new HashMap<>();
    
    // UUID -> número de evasiones totales
    private final Map<UUID, Integer> evasionCount = new HashMap<>();
    
    // Tiempo mínimo requerido en el desastre (configurable desde evasiones.yml)
    private long minRequiredTimeMs;
    
    // Ventana de gracia para reconexión (configurable desde evasiones.yml)
    private long graceReconnectWindowMs;
    
    // Tiempo reducido para late-joiners (configurable desde evasiones.yml)
    private long lateJoinMinTimeMs;
    
    // Threshold para detectar late-joiners (configurable desde evasiones.yml)
    private long lateJoinThresholdMs;
    
    // Cooldown entre resets de evasiones (configurable desde evasiones.yml)
    private long evasionResetTimeMs;
    
    // UUID -> último timestamp de evasión (para reset progresivo)
    private final Map<UUID, Long> lastEvasionTime = new HashMap<>();
    
    // UUID -> nivel de castigo pendiente al reconectar
    private final Map<UUID, Integer> pendingPunishment = new HashMap<>();
    
    // UUID -> tiempo en que se desconectó (para detectar reconexiones rápidas)
    private final Map<UUID, Long> lastDisconnectTime = new HashMap<>();
    
    // Timestamp de cuando empezó el desastre actual (para detectar late-joiners)
    private long currentDisasterStartTime = 0L;
    
    // Tracking de desastre activo (evitar falsos positivos)
    private boolean disasterActive = false;
    
    // ID del desastre actual (para requisitos por tipo)
    private String currentDisasterId = null;
    
    // UUID -> desastres completados sin evadir (para reducción por buen comportamiento)
    private final Map<UUID, Integer> completedDisastersCount = new HashMap<>();
    
    // UUID -> último tiempo sin evadir (para reducción por tiempo)
    private final Map<UUID, Long> lastGoodBehaviorCheck = new HashMap<>();
    
    // UUID -> avisos ya enviados (para no repetir)
    private final Map<UUID, java.util.Set<Integer>> sentWarnings = new HashMap<>();
    
    // Tarea de guardado automático
    private int autoSaveTaskId = -1;
    
    // Tarea de avisos proactivos
    private int proactiveWarningTaskId = -1;
    
    public DisasterEvasionTracker(Apocalipsis plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "evasion_data.yml");
        loadConfig();
        loadData();
    }
    
    /**
     * Carga la configuración desde evasiones.yml
     */
    private void loadConfig() {
        minRequiredTimeMs = plugin.getConfigManager().getEvasionMinTiempoSegundos() * 1000L;
        graceReconnectWindowMs = plugin.getConfigManager().getEvasionVentanaGraciaSegundos() * 1000L;
        lateJoinMinTimeMs = plugin.getConfigManager().getEvasionLateJoinMinTiempoSegundos() * 1000L;
        lateJoinThresholdMs = plugin.getConfigManager().getEvasionLateJoinThresholdSegundos() * 1000L;
        evasionResetTimeMs = plugin.getConfigManager().getEvasionResetAutomaticoHoras() * 3600000L;
    }
    
    /**
     * Recarga la configuración (útil para /reload)
     */
    public void reloadConfig() {
        loadConfig();
    }
    
    /**
     * Marca el inicio de un desastre para todos los jugadores online.
     * Se llama UNA vez cuando el desastre comienza.
     */
    public void onDisasterStartGlobal() {
        onDisasterStartGlobal(null);
    }
    
    /**
     * Marca el inicio de un desastre con ID específico.
     * @param disasterId ID del desastre (para requisitos por tipo)
     */
    public void onDisasterStartGlobal(String disasterId) {
        long now = System.currentTimeMillis();
        currentDisasterStartTime = now;
        currentDisasterId = disasterId;
        disasterActive = true;
        sentWarnings.clear();
        
        // Iniciar guardado automático durante desastre
        startAutoSave();
        
        // Iniciar avisos proactivos
        startProactiveWarnings();
        
        if (plugin.getConfigManager().isEvasionDebug()) {
            plugin.getLogger().info("[EvasionTracker] Desastre iniciado" + 
                (disasterId != null ? " (" + disasterId + ")" : "") + " - tracking activado");
        }
    }
    
    /**
     * Inicia el guardado automático durante el desastre
     */
    private void startAutoSave() {
        if (autoSaveTaskId != -1) {
            org.bukkit.Bukkit.getScheduler().cancelTask(autoSaveTaskId);
        }
        
        int intervalSeconds = plugin.getConfigManager().getEvasionGuardarCadaSegundos();
        autoSaveTaskId = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            saveData();
            if (plugin.getConfigManager().isEvasionDebug()) {
                plugin.getLogger().info("[EvasionTracker] Auto-guardado durante desastre");
            }
        }, intervalSeconds * 20L, intervalSeconds * 20L).getTaskId();
    }
    
    /**
     * Inicia los avisos proactivos a jugadores durante el desastre
     */
    private void startProactiveWarnings() {
        if (!plugin.getConfigManager().isEvasionNotificacionesJugadorEnabled()) {
            return;
        }
        
        if (proactiveWarningTaskId != -1) {
            org.bukkit.Bukkit.getScheduler().cancelTask(proactiveWarningTaskId);
        }
        
        // Revisar cada 5 segundos
        proactiveWarningTaskId = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!disasterActive) return;
            
            long now = System.currentTimeMillis();
            
            for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                Long joinTime = playerJoinTime.get(uuid);
                
                if (joinTime == null) continue;
                
                long timeInDisaster = (now - joinTime) / 1000; // En segundos
                long requiredTime = getRequiredTimeForPlayer(uuid) / 1000;
                
                // Obtener avisos ya enviados
                java.util.Set<Integer> sent = sentWarnings.computeIfAbsent(uuid, k -> new java.util.HashSet<>());
                
                // Avisos a 30s, 45s y cuando cumple el tiempo
                if (timeInDisaster >= 30 && timeInDisaster < 45 && !sent.contains(30)) {
                    sent.add(30);
                    player.sendMessage("§e⏰ Llevas 30s en el desastre. Mínimo requerido: §f" + requiredTime + "s");
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.0f);
                } else if (timeInDisaster >= 45 && timeInDisaster < requiredTime && !sent.contains(45)) {
                    sent.add(45);
                    long faltante = requiredTime - timeInDisaster;
                    player.sendMessage("§e⏰ Llevas 45s. Faltan §f" + faltante + "s §epara salir sin penalización");
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.2f);
                } else if (timeInDisaster >= requiredTime && !sent.contains(60)) {
                    sent.add(60);
                    player.sendMessage("§a✓ Ya puedes desconectarte sin penalización");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
                    // Partículas de celebración
                    player.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, 
                        player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);
                }
            }
        }, 100L, 100L).getTaskId(); // Cada 5 segundos
    }
    
    /**
     * Obtiene el tiempo requerido para un jugador (considerando tipo de desastre y late-join)
     */
    private long getRequiredTimeForPlayer(UUID uuid) {
        Long joinTime = playerJoinTime.get(uuid);
        if (joinTime == null) return minRequiredTimeMs;
        
        // Verificar si es late-joiner
        if (currentDisasterStartTime > 0 && joinTime > currentDisasterStartTime + lateJoinThresholdMs) {
            return lateJoinMinTimeMs;
        }
        
        // Verificar requisitos por tipo de desastre
        if (currentDisasterId != null && plugin.getConfigManager().isEvasionPorDesastreEnabled()) {
            return plugin.getConfigManager().getEvasionPorDesastreMinTiempo(currentDisasterId) * 1000L;
        }
        
        return minRequiredTimeMs;
    }
    
    /**
     * Detiene las tareas de guardado automático y avisos
     */
    private void stopScheduledTasks() {
        if (autoSaveTaskId != -1) {
            org.bukkit.Bukkit.getScheduler().cancelTask(autoSaveTaskId);
            autoSaveTaskId = -1;
        }
        if (proactiveWarningTaskId != -1) {
            org.bukkit.Bukkit.getScheduler().cancelTask(proactiveWarningTaskId);
            proactiveWarningTaskId = -1;
        }
    }
    
    /**
     * Registra que un jugador ha entrado en un desastre activo.
     * Este método se llama tanto al inicio del desastre para todos los jugadores online,
     * como cuando un jugador se une al servidor durante un desastre.
     */
    public void onDisasterStart(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        // Verificar si es una reconexión rápida dentro de la ventana de gracia
        Long lastDisconnect = lastDisconnectTime.get(uuid);
        if (lastDisconnect != null) {
            long timeSinceDisconnect = now - lastDisconnect;
            
            if (timeSinceDisconnect <= graceReconnectWindowMs) {
                // Reconexión rápida - restaurar tiempo anterior (si existía)
                Long previousJoinTime = playerJoinTime.get(uuid);
                if (previousJoinTime != null) {
                    // Mantener el tiempo original para no penalizar
                    if (plugin.getConfigManager().isEvasionDebug()) {
                        plugin.getLogger().info("[EvasionTracker] " + player.getName() + 
                            " reconectó en " + (timeSinceDisconnect/1000) + "s - tiempo restaurado");
                    }
                    return; // No actualizar playerJoinTime, mantener el original
                }
            }
            // Limpiar registro de desconexión si ya pasó la ventana
            lastDisconnectTime.remove(uuid);
        }
        
        // Registrar tiempo de entrada
        playerJoinTime.put(uuid, now);
        
        // Detectar late-joiner (jugador que entra después del inicio del desastre)
        if (disasterActive && currentDisasterStartTime > 0) {
            long timeAfterStart = now - currentDisasterStartTime;
            if (timeAfterStart > lateJoinThresholdMs) { // Más de X segundos después del inicio
                if (plugin.getConfigManager().isEvasionDebug()) {
                    plugin.getLogger().info("[EvasionTracker] " + player.getName() + 
                        " entró " + (timeAfterStart/1000) + "s después del inicio - tiempo reducido aplicado");
                }
            }
        }
        
        if (plugin.getConfigManager().isEvasionDebug()) {
            plugin.getLogger().info("[EvasionTracker] Jugador " + player.getName() + " entró al desastre");
        }
    }
    
    /**
     * Registra que un jugador está saliendo del servidor durante un desastre.
     * Evalúa si es evasión y aplica penalizaciones.
     * 
     * @return true si fue evasión, false si salida legítima
     */
    public boolean onPlayerQuitDuringDisaster(Player player) {
        UUID uuid = player.getUniqueId();
        Long joinTime = playerJoinTime.get(uuid);
        
        // Si no hay desastre activo, no puede ser evasión
        if (!disasterActive) {
            if (plugin.getConfigManager().isEvasionDebug()) {
                plugin.getLogger().info("[EvasionTracker] " + player.getName() + 
                    " se desconectó pero no hay desastre activo");
            }
            return false;
        }
        
        // Si no tenía registro, no estaba en el desastre (o ya había terminado)
        if (joinTime == null) {
            if (plugin.getConfigManager().isEvasionDebug()) {
                plugin.getLogger().info("[EvasionTracker] " + player.getName() + 
                    " se desconectó sin registro de entrada (posible late-join)");
            }
            return false;
        }
        
        long now = System.currentTimeMillis();
        long timeInDisaster = now - joinTime;
        
        // Determinar tiempo mínimo requerido (diferente para late-joiners)
        long requiredTime = minRequiredTimeMs;
        boolean isLateJoiner = false;
        
        if (currentDisasterStartTime > 0 && joinTime > currentDisasterStartTime + lateJoinThresholdMs) {
            // Late-joiner: jugador que entró más de Xs después del inicio
            requiredTime = lateJoinMinTimeMs;
            isLateJoiner = true;
            
            if (plugin.getConfigManager().isEvasionDebug()) {
                plugin.getLogger().info("[EvasionTracker] " + player.getName() + 
                    " identificado como late-joiner - tiempo requerido: " + (requiredTime/1000) + "s");
            }
        }
        
        // Si estuvo más del tiempo mínimo, no es evasión
        if (timeInDisaster >= requiredTime) {
            playerJoinTime.remove(uuid);
            if (plugin.getConfigManager().isEvasionDebug()) {
                plugin.getLogger().info("[EvasionTracker] " + player.getName() + 
                    " se desconectó legítimamente (" + (timeInDisaster/1000) + "s >= " + (requiredTime/1000) + "s)");
            }
            return false;
        }
        
        // Registrar tiempo de desconexión para ventana de gracia
        lastDisconnectTime.put(uuid, now);
        
        // POSIBLE EVASIÓN - dar ventana de gracia de 30s para reconexión
        if (plugin.getConfigManager().isEvasionDebug()) {
            plugin.getLogger().warning("[EvasionTracker] " + player.getName() + 
                " se desconectó temprano (" + (timeInDisaster/1000) + "s / " + (requiredTime/1000) + 
                "s) - ventana de gracia de 30s para reconexión");
        }
        
        // Programar verificación de evasión después de la ventana de gracia
        final long finalTimeInDisaster = timeInDisaster;
        final boolean finalIsLateJoiner = isLateJoiner;
        
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Verificar si el jugador reconectó dentro de la ventana de gracia
            Long reconnectCheck = lastDisconnectTime.get(uuid);
            if (reconnectCheck != null && reconnectCheck.equals(now)) {
                // No reconectó - aplicar penalización
                if (plugin.getConfigManager().isEvasionDebug()) {
                    plugin.getLogger().warning("[EvasionTracker] " + player.getName() + 
                        " NO reconectó en 30s - EVASIÓN CONFIRMADA");
                }
                
                // Aplicar penalización (offline)
                applyEvasionPenaltyOffline(uuid, player.getName(), finalTimeInDisaster, finalIsLateJoiner);
                
                // Limpiar registros
                playerJoinTime.remove(uuid);
                lastDisconnectTime.remove(uuid);
            } else {
                // Reconectó - no es evasión
                if (plugin.getConfigManager().isEvasionDebug()) {
                    plugin.getLogger().info("[EvasionTracker] " + player.getName() + 
                        " reconectó dentro de la ventana de gracia - NO es evasión");
                }
                lastDisconnectTime.remove(uuid);
            }
        }, graceReconnectWindowMs / 50); // Convertir ms a ticks (50ms por tick)
        
        // NO remover playerJoinTime aún, esperar la ventana de gracia
        return false; // Retornar false por ahora, se confirmará después
    }
    
    /**
     * Limpia el registro cuando el desastre termina naturalmente
     */
    public void onDisasterEnd() {
        // Procesar jugadores que completaron el desastre para reducción por buen comportamiento
        processCompletedDisasters();
        
        // Detener tareas programadas
        stopScheduledTasks();
        
        // Limpiar registros
        playerJoinTime.clear();
        lastDisconnectTime.clear();
        sentWarnings.clear();
        disasterActive = false;
        currentDisasterStartTime = 0L;
        currentDisasterId = null;
        
        // Guardar datos finales
        saveData();
        
        if (plugin.getConfigManager().isEvasionDebug()) {
            plugin.getLogger().info("[EvasionTracker] Desastre finalizado - registros limpiados, tracking desactivado");
        }
    }
    
    /**
     * Procesa jugadores que completaron el desastre sin evadir
     * para el sistema de reducción por buen comportamiento
     */
    private void processCompletedDisasters() {
        if (!plugin.getConfigManager().isEvasionReduccionEnabled() || 
            !plugin.getConfigManager().isEvasionReduccionPorDesastresEnabled()) {
            return;
        }
        
        long now = System.currentTimeMillis();
        int tiempoMinimo = plugin.getConfigManager().getEvasionReduccionDesastresTiempoMinimo() * 1000;
        int desastresNecesarios = plugin.getConfigManager().getEvasionReduccionDesastresNecesarios();
        
        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            Long joinTime = playerJoinTime.get(uuid);
            
            if (joinTime == null) continue;
            
            long timeInDisaster = now - joinTime;
            
            // Si estuvo el tiempo mínimo, cuenta como desastre completado
            if (timeInDisaster >= tiempoMinimo) {
                int completed = completedDisastersCount.getOrDefault(uuid, 0) + 1;
                completedDisastersCount.put(uuid, completed);
                
                // Verificar si debe reducir evasiones
                if (completed >= desastresNecesarios) {
                    int currentEvasions = evasionCount.getOrDefault(uuid, 0);
                    if (currentEvasions > 0) {
                        evasionCount.put(uuid, currentEvasions - 1);
                        completedDisastersCount.put(uuid, 0); // Resetear contador
                        
                        player.sendMessage("§a✓ ¡Buen comportamiento! Se te ha reducido 1 evasión por completar " + 
                            desastresNecesarios + " desastres correctamente.");
                        player.sendMessage("§7Evasiones actuales: §e" + (currentEvasions - 1));
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                        
                        if (plugin.getConfigManager().isEvasionDebug()) {
                            plugin.getLogger().info("[EvasionTracker] " + player.getName() + 
                                " redujo 1 evasión por buen comportamiento (" + (currentEvasions - 1) + " restantes)");
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Verifica y aplica reducción por tiempo sin evadir
     * Llamar periódicamente (ej: cada hora o al inicio del día)
     */
    public void checkTimeBasedReduction() {
        if (!plugin.getConfigManager().isEvasionReduccionEnabled() || 
            !plugin.getConfigManager().isEvasionReduccionPorTiempoEnabled()) {
            return;
        }
        
        long now = System.currentTimeMillis();
        long reducirCadaMs = plugin.getConfigManager().getEvasionReduccionCadaHoras() * 3600000L;
        
        for (UUID uuid : new java.util.HashSet<>(evasionCount.keySet())) {
            Long lastCheck = lastGoodBehaviorCheck.get(uuid);
            Long lastEvasion = lastEvasionTime.get(uuid);
            
            if (lastEvasion == null) continue;
            
            // Si no hay check previo, usar la última evasión como referencia
            long referenceTime = lastCheck != null ? lastCheck : lastEvasion;
            long timeSinceReference = now - referenceTime;
            
            // Si pasó suficiente tiempo sin evadir
            if (timeSinceReference >= reducirCadaMs) {
                int currentEvasions = evasionCount.getOrDefault(uuid, 0);
                if (currentEvasions > 0) {
                    evasionCount.put(uuid, currentEvasions - 1);
                    lastGoodBehaviorCheck.put(uuid, now);
                    
                    // Notificar si está online
                    org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(uuid);
                    if (player != null) {
                        player.sendMessage("§a✓ Por no evadir en " + 
                            (reducirCadaMs / 3600000) + "h, se te redujo 1 evasión.");
                        player.sendMessage("§7Evasiones actuales: §e" + (currentEvasions - 1));
                    }
                    
                    if (plugin.getConfigManager().isEvasionDebug()) {
                        plugin.getLogger().info("[EvasionTracker] UUID " + uuid + 
                            " redujo 1 evasión por tiempo sin evadir");
                    }
                }
            }
        }
        
        saveData();
    }
    
    /**
     * Aplica penalizaciones cuando el jugador ya se desconectó (versión offline)
     */
    private void applyEvasionPenaltyOffline(UUID uuid, String playerName, long timeInDisaster, boolean isLateJoiner) {
        // Resetear contador si pasó más tiempo del configurado desde última evasión
        Long lastEvasion = lastEvasionTime.get(uuid);
        if (lastEvasion != null) {
            long timeSinceLastEvasion = System.currentTimeMillis() - lastEvasion;
            if (timeSinceLastEvasion > evasionResetTimeMs) {
                evasionCount.put(uuid, 0);
            }
        }
        
        // Incrementar contador de evasiones
        int evasions = evasionCount.getOrDefault(uuid, 0) + 1;
        evasionCount.put(uuid, evasions);
        lastEvasionTime.put(uuid, System.currentTimeMillis());
        
        // Obtener PS actual
        int currentPs = plugin.getMissionService().getPlayerPs(uuid);
        
        // Obtener porcentaje de penalización desde config (nivel máximo: 4)
        int nivel = Math.min(evasions, 4);
        double porcentaje = plugin.getConfigManager().getEvasionPenalizacionPsPorcentaje(nivel) / 100.0;
        
        // Calcular pérdida de PS (proporcional si está configurado)
        int psLoss = calcularPsLoss(currentPs, porcentaje, timeInDisaster, isLateJoiner);
        
        // Programar castigos físicos solo si están habilitados
        if (plugin.getConfigManager().isEvasionCastigosFisicosEnabled()) {
            scheduleReconnectPunishment(uuid, nivel);
        }
        
        // Aplicar penalizaciones de misiones según config
        int misionesRandom = plugin.getConfigManager().getEvasionPenalizacionMisionesRandom(nivel);
        boolean todasMisiones = plugin.getConfigManager().getEvasionPenalizacionTodasMisiones(nivel);
        
        if (todasMisiones) {
            plugin.getMissionService().failAllMissions(uuid);
        } else if (misionesRandom > 0) {
            for (int i = 0; i < misionesRandom; i++) {
                plugin.getMissionService().failRandomMission(uuid);
            }
        }
        
        // Aplicar pérdida de PS
        if (psLoss > 0) {
            int newPs = Math.max(0, currentPs - psLoss);
            plugin.getMissionService().setPlayerPs(uuid, newPs);
        }
        
        // Notificar a administradores si está habilitado
        notifyAdmins(uuid, playerName, nivel, evasions, psLoss);
        
        // Log detallado
        String lateJoinNote = isLateJoiner ? " (LATE-JOINER)" : "";
        plugin.getLogger().warning(String.format(
            "[EvasionTracker] EVASIÓN CONFIRMADA%s - Jugador: %s, Tiempo: %.1fs, Evasiones: %d, Nivel: %d, PS perdidos: %d",
            lateJoinNote,
            playerName,
            timeInDisaster / 1000.0,
            evasions,
            nivel,
            psLoss
        ));
        
        // Guardar historial si está habilitado
        if (plugin.getConfigManager().isEvasionEstadisticasEnabled() && 
            plugin.getConfigManager().isEvasionEstadisticasGuardarHistorial()) {
            saveEvasionHistory(uuid, playerName, timeInDisaster, psLoss, nivel, isLateJoiner);
        }
        
        // Guardar cambios
        saveData();
    }
    
    /**
     * Calcula la pérdida de PS según el tipo de cálculo configurado
     */
    private int calcularPsLoss(int currentPs, double porcentaje, long timeInDisaster, boolean isLateJoiner) {
        String tipoCalculo = plugin.getConfigManager().getEvasionPenalizacionTipoCalculo();
        
        if ("proporcional".equalsIgnoreCase(tipoCalculo)) {
            // Proporcional: reduce según el tiempo que estuvo en el desastre
            long tiempoRequerido = isLateJoiner ? lateJoinMinTimeMs : minRequiredTimeMs;
            double factorTiempo = 1.0 - (timeInDisaster / (double) tiempoRequerido);
            factorTiempo = Math.max(0.5, Math.min(1.0, factorTiempo)); // Entre 50% y 100%
            return (int) (currentPs * porcentaje * factorTiempo);
        } else {
            // Fijo: aplica el porcentaje completo
            return (int) (currentPs * porcentaje);
        }
    }
    
    /**
     * Notifica a administradores sobre una evasión
     */
    private void notifyAdmins(UUID uuid, String playerName, int nivel, int totalEvasiones, int psLoss) {
        if (!plugin.getConfigManager().isEvasionNotificacionesAdminsEnabled()) {
            return;
        }
        
        int alertarDesdeNivel = plugin.getConfigManager().getEvasionNotificacionesAlertarDesdeNivel();
        if (nivel < alertarDesdeNivel) {
            return;
        }
        
        String permiso = plugin.getConfigManager().getEvasionNotificacionesPermiso();
        String mensaje = plugin.getConfigManager().getEvasionNotificacionesMensaje()
            .replace("{player}", playerName)
            .replace("{uuid}", uuid.toString().substring(0, 8))
            .replace("{evasiones}", String.valueOf(totalEvasiones))
            .replace("{nivel}", String.valueOf(nivel))
            .replace("{ps_perdidos}", String.valueOf(psLoss));
        mensaje = mensaje.replace("&", "§");
        
        final String finalMensaje = mensaje;
        
        // Notificar a todos los admins online
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            for (org.bukkit.entity.Player admin : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (admin.hasPermission(permiso) || admin.hasPermission("avo.admin")) {
                    admin.sendMessage(finalMensaje);
                    admin.playSound(admin.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
                }
            }
        });
    }
    
    /**
     * Guarda el historial de evasión para estadísticas
     */
    private void saveEvasionHistory(UUID uuid, String playerName, long timeInDisaster, int psLoss, int nivel, boolean isLateJoiner) {
        try {
            File historyFile = new File(plugin.getDataFolder(), "evasion_history.yml");
            FileConfiguration history = YamlConfiguration.loadConfiguration(historyFile);
            
            String path = uuid.toString();
            java.util.List<String> entries = history.getStringList(path);
            
            // Limitar entradas según configuración
            int maxEntradas = plugin.getConfigManager().getEvasionEstadisticasHistorialMaxEntradas();
            while (entries.size() >= maxEntradas) {
                entries.remove(0);
            }
            
            // Agregar nueva entrada
            String entry = String.format("%d|%s|%.1fs|%d|%d|%s",
                System.currentTimeMillis(),
                playerName,
                timeInDisaster / 1000.0,
                psLoss,
                nivel,
                isLateJoiner ? "late" : "normal");
            entries.add(entry);
            
            history.set(path, entries);
            history.save(historyFile);
        } catch (Exception e) {
            plugin.getLogger().warning("[EvasionTracker] Error guardando historial: " + e.getMessage());
        }
    }
    
    /**
     * Aplica penalizaciones progresivas por evasión (PS + castigos físicos)
     * Versión para cuando el jugador todavía está online
     */
    private void applyEvasionPenalty(Player player, long timeInDisaster) {
        UUID uuid = player.getUniqueId();
        
        // Resetear contador si pasó el tiempo configurado desde última evasión
        Long lastEvasion = lastEvasionTime.get(uuid);
        if (lastEvasion != null) {
            long timeSinceLastEvasion = System.currentTimeMillis() - lastEvasion;
            if (timeSinceLastEvasion > evasionResetTimeMs) {
                evasionCount.put(uuid, 0);
            }
        }
        
        // Incrementar contador de evasiones
        int evasions = evasionCount.getOrDefault(uuid, 0) + 1;
        evasionCount.put(uuid, evasions);
        lastEvasionTime.put(uuid, System.currentTimeMillis());
        
        // Obtener PS actual
        int currentPs = plugin.getMissionService().getPlayerPs(player);
        
        // Obtener nivel y porcentaje desde config (nivel máximo: 4)
        int nivel = Math.min(evasions, 4);
        double porcentaje = plugin.getConfigManager().getEvasionPenalizacionPsPorcentaje(nivel) / 100.0;
        
        // Calcular pérdida de PS
        int psLoss = calcularPsLoss(currentPs, porcentaje, timeInDisaster, false);
        
        // Programar castigos físicos solo si están habilitados
        if (plugin.getConfigManager().isEvasionCastigosFisicosEnabled()) {
            scheduleReconnectPunishment(uuid, nivel);
        }
        
        // Aplicar penalizaciones de misiones según config
        int misionesRandom = plugin.getConfigManager().getEvasionPenalizacionMisionesRandom(nivel);
        boolean todasMisiones = plugin.getConfigManager().getEvasionPenalizacionTodasMisiones(nivel);
        
        if (todasMisiones) {
            failAllMissions(player);
        } else if (misionesRandom > 0) {
            for (int i = 0; i < misionesRandom; i++) {
                failRandomMission(player);
            }
        }
        
        // Enviar mensaje de advertencia
        sendWarningMessage(player, nivel, psLoss, timeInDisaster);
        
        // Aplicar pérdida de PS
        if (psLoss > 0) {
            int newPs = Math.max(0, currentPs - psLoss);
            plugin.getMissionService().setPlayerPs(uuid, newPs);
        }
        
        // Notificar a administradores
        notifyAdmins(uuid, player.getName(), nivel, evasions, psLoss);
        
        // Log
        plugin.getLogger().warning(String.format(
            "[EvasionTracker] EVASIÓN DETECTADA - Jugador: %s, Tiempo: %.1fs, Evasiones: %d, Nivel: %d, PS perdidos: %d",
            player.getName(),
            timeInDisaster / 1000.0,
            evasions,
            nivel,
            psLoss
        ));
        
        // Guardar historial si está habilitado
        if (plugin.getConfigManager().isEvasionEstadisticasEnabled() && 
            plugin.getConfigManager().isEvasionEstadisticasGuardarHistorial()) {
            saveEvasionHistory(uuid, player.getName(), timeInDisaster, psLoss, nivel, false);
        }
    }
    
    /**
     * Envía mensaje de advertencia personalizado según nivel de evasión
     */
    private void sendWarningMessage(Player player, int evasionLevel, int psLoss, long timeInDisaster) {
        int secondsInDisaster = (int) (timeInDisaster / 1000);
        int requiredSeconds = (int) (minRequiredTimeMs / 1000);
        
        player.sendMessage("");
        player.sendMessage("§c§l⚠ ═══════════════════════════════════════ ⚠");
        player.sendMessage("§c§l         EVASIÓN DE DESASTRE DETECTADA");
        player.sendMessage("§c§l⚠ ═══════════════════════════════════════ ⚠");
        player.sendMessage("");
        player.sendMessage("§7Tiempo mínimo requerido: §e" + requiredSeconds + "s");
        player.sendMessage("§7Tiempo que estuviste: §c" + secondsInDisaster + "s");
        player.sendMessage("");
        player.sendMessage("§c§lPENALIZACIONES INMEDIATAS:");
        player.sendMessage("§7  • Pérdida de PS: §c-" + psLoss + " PS");
        
        if (evasionLevel == 2) {
            player.sendMessage("§7  • Una misión marcada como §cFALLIDA");
        } else if (evasionLevel >= 3) {
            player.sendMessage("§7  • §cTODAS§7 tus misiones marcadas como §cFALLIDAS");
        }
        
        // Advertencias de castigos físicos
        player.sendMessage("");
        player.sendMessage("§4§l⚡ CASTIGO AL RECONECTAR:");
        switch (evasionLevel) {
            case 1:
                player.sendMessage("§e  • §73 rayos de advertencia");
                break;
            case 2:
                player.sendMessage("§6  • §75 rayos con daño real");
                player.sendMessage("§6  • §7Efectos de debilidad temporal");
                break;
            case 3:
                player.sendMessage("§c  • §710 rayos devastadores");
                player.sendMessage("§c  • §7Lluvia de bolas de fuego");
                player.sendMessage("§c  • §7Efectos debilitantes prolongados");
                break;
            default:
                player.sendMessage("§4§l  • SUPER METEORITO DESTRUCTOR");
                player.sendMessage("§4§l  • 15 explosiones en tu base");
                player.sendMessage("§4§l  • Efectos devastadores por 1 minuto");
                player.sendMessage("§4§l  • §cTU BASE SERÁ DESTRUIDA");
                break;
        }
        
        player.sendMessage("");
        player.sendMessage("§e§l⚠ ADVERTENCIA:");
        player.sendMessage("§7No evadas los desastres desconectándote.");
        player.sendMessage("§7Las penalizaciones §c§laumentan§7 con cada evasión.");
        player.sendMessage("§7Evasiones totales: §c" + evasionLevel);
        player.sendMessage("");
        player.sendMessage("§c§l⚠ ═══════════════════════════════════════ ⚠");
        player.sendMessage("");
    }
    
    /**
     * Falla una misión aleatoria del jugador
     */
    private void failRandomMission(Player player) {
        plugin.getMissionService().failRandomMission(player.getUniqueId());
    }
    
    /**
     * Falla todas las misiones del jugador
     */
    private void failAllMissions(Player player) {
        plugin.getMissionService().failAllMissions(player.getUniqueId());
    }
    
    /**
     * Obtiene el número de evasiones de un jugador
     */
    public int getEvasionCount(UUID uuid) {
        return evasionCount.getOrDefault(uuid, 0);
    }
    
    /**
     * Resetea las evasiones de un jugador (admin command)
     */
    public void resetEvasions(UUID uuid) {
        evasionCount.remove(uuid);
        lastEvasionTime.remove(uuid);
        playerJoinTime.remove(uuid);
        pendingPunishment.remove(uuid);
    }
    
    /**
     * Limpia todos los datos (llamar al desactivar plugin)
     */
    public void clearAll() {
        playerJoinTime.clear();
        evasionCount.clear();
        lastEvasionTime.clear();
        pendingPunishment.clear();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // PERSISTENCIA DE DATOS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Carga datos persistidos de evasiones y castigos pendientes
     */
    private void loadData() {
        if (!dataFile.exists()) {
            return;
        }
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            
            // Cargar contador de evasiones
            if (config.contains("evasions")) {
                ConfigurationSection evasionsSection = config.getConfigurationSection("evasions");
                if (evasionsSection != null) {
                    for (String key : evasionsSection.getKeys(false)) {
                        try {
                            UUID uuid = UUID.fromString(key);
                            int count = config.getInt("evasions." + key + ".count", 0);
                            long lastTime = config.getLong("evasions." + key + ".lastTime", 0);
                            
                            evasionCount.put(uuid, count);
                            if (lastTime > 0) {
                                lastEvasionTime.put(uuid, lastTime);
                            }
                        } catch (IllegalArgumentException e) {
                            // UUID inválido, ignorar
                        }
                    }
                }
            }
            
            // Cargar castigos pendientes
            if (config.contains("pending_punishments")) {
                ConfigurationSection punishmentsSection = config.getConfigurationSection("pending_punishments");
                if (punishmentsSection != null) {
                    for (String key : punishmentsSection.getKeys(false)) {
                        try {
                            UUID uuid = UUID.fromString(key);
                            int level = config.getInt("pending_punishments." + key);
                            pendingPunishment.put(uuid, level);
                        } catch (IllegalArgumentException e) {
                            // UUID inválido, ignorar
                        }
                    }
                }
            }
            
            plugin.getLogger().info("[EvasionTracker] Cargados " + evasionCount.size() + " registros de evasión y " + pendingPunishment.size() + " castigos pendientes");
        } catch (Exception e) {
            plugin.getLogger().warning("[EvasionTracker] Error cargando datos: " + e.getMessage());
        }
    }
    
    /**
     * Guarda datos de evasiones y castigos pendientes (llamar al desactivar plugin)
     */
    public void saveData() {
        try {
            FileConfiguration config = new YamlConfiguration();
            
            // Guardar contador de evasiones
            for (Map.Entry<UUID, Integer> entry : evasionCount.entrySet()) {
                String key = entry.getKey().toString();
                config.set("evasions." + key + ".count", entry.getValue());
                
                Long lastTime = lastEvasionTime.get(entry.getKey());
                if (lastTime != null) {
                    config.set("evasions." + key + ".lastTime", lastTime);
                }
            }
            
            // Guardar castigos pendientes
            for (Map.Entry<UUID, Integer> entry : pendingPunishment.entrySet()) {
                config.set("pending_punishments." + entry.getKey().toString(), entry.getValue());
            }
            
            config.save(dataFile);
            plugin.getLogger().info("[EvasionTracker] Guardados " + evasionCount.size() + " registros de evasión y " + pendingPunishment.size() + " castigos pendientes");
        } catch (IOException e) {
            plugin.getLogger().warning("[EvasionTracker] Error guardando datos: " + e.getMessage());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE CASTIGOS FÍSICOS AL RECONECTAR
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Programa un castigo físico para cuando el jugador se reconecte
     */
    private void scheduleReconnectPunishment(UUID uuid, int evasionLevel) {
        pendingPunishment.put(uuid, evasionLevel);
        saveData(); // Guardar inmediatamente para persistir el castigo
        
        if (plugin.getConfigManager().isEvasionDebug()) {
            plugin.getLogger().info("[EvasionTracker] Castigo nivel " + evasionLevel + " programado y guardado para UUID: " + uuid);
        }
    }
    
    /**
     * Aplica el castigo físico cuando el jugador se reconecta
     * Este método debe ser llamado desde PlayerJoinEvent
     */
    public void applyReconnectPunishment(Player player) {
        UUID uuid = player.getUniqueId();
        Integer punishmentLevel = pendingPunishment.get(uuid);
        
        // [DEBUG] Log para tracking
        plugin.getLogger().info("[EvasionTracker] Login check para " + player.getName() + " - Castigo pendiente: " + (punishmentLevel != null ? "Nivel " + punishmentLevel : "Ninguno"));
        
        if (punishmentLevel == null) {
            return; // No tiene castigos pendientes
        }
        
        // Verificar si los castigos físicos están habilitados
        if (!plugin.getConfigManager().isEvasionCastigosFisicosEnabled()) {
            // Solo notificar sin aplicar castigos físicos
            pendingPunishment.remove(uuid);
            saveData();
            
            player.sendMessage("");
            player.sendMessage("§e§l⚠ ════════════════════════════════════ ⚠");
            player.sendMessage("§e§l   ADVERTENCIA POR EVASIÓN DE DESASTRE");
            player.sendMessage("§e§l⚠ ════════════════════════════════════ ⚠");
            player.sendMessage("");
            player.sendMessage("§7Se detectó que te desconectaste durante un desastre.");
            player.sendMessage("§7Ya has recibido la penalización de §cPS§7 y §cmisiones§7.");
            player.sendMessage("");
            player.sendMessage("§7Nivel de evasión: §c" + punishmentLevel);
            player.sendMessage("§a✓ Los castigos físicos están desactivados.");
            player.sendMessage("");
            player.sendMessage("§e§l⚠ ════════════════════════════════════ ⚠");
            player.sendMessage("");
            return;
        }
        
        // Remover el castigo pendiente
        pendingPunishment.remove(uuid);
        saveData();
        
        // Mensaje de advertencia INMEDIATO al conectarse
        player.sendMessage("");
        player.sendMessage("§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
        player.sendMessage("  §c§lTIENES UN CASTIGO PENDIENTE");
        player.sendMessage("  §7Por evadir un desastre al desconectarte");
        player.sendMessage("");
        player.sendMessage("  §7Nivel de evasión: §c" + punishmentLevel);
        player.sendMessage("  §7Los dioses aplicarán tu castigo en §e3 segundos§7...");
        player.sendMessage("");
        player.sendMessage("§4§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
        
        // Sonido de advertencia
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
        
        // Aplicar castigo según nivel
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            switch (punishmentLevel) {
                case 1:
                    applyLevel1Punishment(player);
                    break;
                case 2:
                    applyLevel2Punishment(player);
                    break;
                case 3:
                    applyLevel3Punishment(player);
                    break;
                default:
                    applyLevel4Punishment(player);
                    break;
            }
        }, 60L); // 3 segundos después de conectarse
    }
    
    /**
     * NIVEL 1: Advertencia suave - 3 rayos
     */
    private void applyLevel1Punishment(Player player) {
        player.sendMessage("");
        player.sendMessage("§e§l⚠ ════════════════════════════════════ ⚠");
        player.sendMessage("§e§l      CASTIGO POR EVASIÓN - NIVEL 1");
        player.sendMessage("§e§l⚠ ════════════════════════════════════ ⚠");
        player.sendMessage("§7Los dioses te recuerdan que §cNO debes evadir desastres§7.");
        player.sendMessage("");
        
        org.bukkit.Location loc = player.getLocation();
        
        // 3 rayos con intervalos
        for (int i = 0; i < 3; i++) {
            final int delay = i * 10; // 0.5s entre rayos
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.getWorld().strikeLightningEffect(loc);
                    player.playSound(loc, org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                }
            }, delay);
        }
        
        plugin.getLogger().info("[EvasionTracker] Castigo NIVEL 1 aplicado a " + player.getName());
    }
    
    /**
     * NIVEL 2: Castigo moderado - 5 rayos + daño + efecto
     */
    private void applyLevel2Punishment(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l⚠ ════════════════════════════════════ ⚠");
        player.sendMessage("§6§l      CASTIGO POR EVASIÓN - NIVEL 2");
        player.sendMessage("§6§l⚠ ════════════════════════════════════ ⚠");
        player.sendMessage("§7Los dioses están §cenojados§7. Segunda advertencia.");
        player.sendMessage("");
        
        org.bukkit.Location loc = player.getLocation();
        
        // 5 rayos con daño progresivo
        for (int i = 0; i < 5; i++) {
            final int delay = i * 8; // Más rápido
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.getWorld().strikeLightning(loc); // Rayo REAL con daño
                    player.damage(4.0); // 2 corazones
                    player.playSound(loc, org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.8f);
                }
            }, delay);
        }
        
        // Efecto de ralentización
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOWNESS, 200, 1)); // 10s
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.WEAKNESS, 200, 0)); // 10s
            }
        }, 40L);
        
        plugin.getLogger().info("[EvasionTracker] Castigo NIVEL 2 aplicado a " + player.getName());
    }
    
    /**
     * NIVEL 3: Castigo severo - 10 rayos + lluvia de fuego
     */
    private void applyLevel3Punishment(Player player) {
        player.sendMessage("");
        player.sendMessage("§c§l⚠ ════════════════════════════════════ ⚠");
        player.sendMessage("§c§l      CASTIGO POR EVASIÓN - NIVEL 3");
        player.sendMessage("§c§l⚠ ════════════════════════════════════ ⚠");
        player.sendMessage("§7Los dioses están §4§lFURIOSOS§7. Última advertencia.");
        player.sendMessage("");
        
        org.bukkit.Location loc = player.getLocation();
        org.bukkit.World world = player.getWorld();
        
        // 10 rayos devastadores
        for (int i = 0; i < 10; i++) {
            final int delay = i * 5; // Muy rápido
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    world.strikeLightning(loc);
                    player.damage(6.0); // 3 corazones
                    player.playSound(loc, org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);
                }
            }, delay);
        }
        
        // Lluvia de bolas de fuego en un radio de 5 bloques
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 * i) / 8;
                    double x = Math.cos(angle) * 5;
                    double z = Math.sin(angle) * 5;
                    org.bukkit.Location fireLoc = loc.clone().add(x, 10, z);
                    
                    org.bukkit.entity.Fireball fireball = world.spawn(
                        fireLoc, org.bukkit.entity.Fireball.class);
                    fireball.setDirection(new org.bukkit.util.Vector(0, -1, 0));
                    fireball.setYield(2.0f); // Explosión moderada
                }
            }
        }, 60L);
        
        // Efectos debilitantes
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOWNESS, 400, 2)); // 20s
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.WEAKNESS, 400, 1)); // 20s
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.MINING_FATIGUE, 400, 1)); // 20s
                player.setFireTicks(100); // 5 segundos en fuego
            }
        }, 70L);
        
        plugin.getLogger().warning("[EvasionTracker] Castigo NIVEL 3 aplicado a " + player.getName());
    }
    
    /**
     * NIVEL 4+: CASTIGO EXTREMO - Super meteorito destructor
     */
    private void applyLevel4Punishment(Player player) {
        player.sendMessage("");
        player.sendMessage("§4§l⚠⚠⚠ ═══════════════════════════════ ⚠⚠⚠");
        player.sendMessage("§4§l    CASTIGO POR EVASIÓN - NIVEL MÁXIMO");
        player.sendMessage("§4§l⚠⚠⚠ ═══════════════════════════════ ⚠⚠⚠");
        player.sendMessage("§c§lLOS DIOSES HAN PERDIDO LA PACIENCIA.");
        player.sendMessage("§7Un §4§lSUPER METEORITO§7 destruirá tu base.");
        player.sendMessage("");
        
        org.bukkit.Location loc = player.getLocation();
        org.bukkit.World world = player.getWorld();
        
        // Advertencia sonora dramática
        for (int i = 0; i < 5; i++) {
            final int delay = i * 20;
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.playSound(loc, org.bukkit.Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.5f);
                    net.kyori.adventure.title.Title warning = net.kyori.adventure.title.Title.title(
                        net.kyori.adventure.text.Component.text("§4§l⚠"),
                        net.kyori.adventure.text.Component.text("§cMETEORITO ENTRANTE"),
                        net.kyori.adventure.title.Title.Times.times(
                            java.time.Duration.ZERO,
                            java.time.Duration.ofSeconds(1),
                            java.time.Duration.ofMillis(500)
                        )
                    );
                    player.showTitle(warning);
                }
            }, delay);
        }
        
        // SUPER METEORITO después de 5 segundos
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                org.bukkit.Location currentLoc = player.getLocation();
                
                // Anuncio global
                org.bukkit.Bukkit.getServer().broadcast(
                    net.kyori.adventure.text.Component.text("§4§l⚠⚠⚠ Un SUPER METEORITO impactará en la base de §f" + player.getName() + " §4§l⚠⚠⚠")
                );
                
                // Impacto del meteorito - múltiples explosiones
                for (int i = 0; i < 15; i++) {
                    final int delay = i * 2;
                    final double offsetX = (Math.random() - 0.5) * 10;
                    final double offsetZ = (Math.random() - 0.5) * 10;
                    
                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        org.bukkit.Location explosionLoc = currentLoc.clone().add(offsetX, 0, offsetZ);
                        
                        // Explosión grande
                        world.createExplosion(explosionLoc, 4.0f, true, true);
                        
                        // Rayo en cada explosión
                        world.strikeLightning(explosionLoc);
                        
                        // Partículas dramáticas
                        world.spawnParticle(org.bukkit.Particle.EXPLOSION_EMITTER, 
                            explosionLoc, 3, 1, 1, 1, 0.1);
                        world.spawnParticle(org.bukkit.Particle.LAVA, 
                            explosionLoc, 50, 2, 2, 2, 0.1);
                        world.spawnParticle(org.bukkit.Particle.FLAME, 
                            explosionLoc, 100, 3, 3, 3, 0.2);
                    }, delay);
                }
                
                // Daño masivo al jugador
                player.damage(15.0); // 7.5 corazones
                
                // Efectos devastadores permanentes por 1 minuto
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOWNESS, 1200, 3));
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.WEAKNESS, 1200, 2));
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.MINING_FATIGUE, 1200, 2));
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.HUNGER, 1200, 2));
                player.setFireTicks(400); // 20 segundos en fuego
            }
        }, 100L);
        
        plugin.getLogger().severe("[EvasionTracker] CASTIGO NIVEL MÁXIMO aplicado a " + player.getName() + " - Super Meteorito lanzado");
    }
    
    /**
     * Verifica si un jugador tiene castigos pendientes
     */
    public boolean hasPendingPunishment(UUID uuid) {
        return pendingPunishment.containsKey(uuid);
    }
    
    /**
     * Obtiene el nivel de castigo pendiente
     */
    public int getPendingPunishmentLevel(UUID uuid) {
        return pendingPunishment.getOrDefault(uuid, 0);
    }
    
    /**
     * Limpia todas las evasiones y castigos pendientes de un jugador específico
     */
    public void clearPlayerEvasions(UUID uuid) {
        evasionCount.remove(uuid);
        lastEvasionTime.remove(uuid);
        pendingPunishment.remove(uuid);
        playerJoinTime.remove(uuid);
        saveData();
        
        plugin.getLogger().info("[EvasionTracker] Evasiones limpiadas para UUID: " + uuid);
    }
    
    /**
     * Limpia todas las evasiones y castigos pendientes de todos los jugadores
     */
    public void clearAllEvasions() {
        int count = evasionCount.size() + pendingPunishment.size();
        
        evasionCount.clear();
        lastEvasionTime.clear();
        pendingPunishment.clear();
        playerJoinTime.clear();
        saveData();
        
        plugin.getLogger().info("[EvasionTracker] Todas las evasiones limpiadas (" + count + " registros)");
    }
    
    /**
     * Obtiene información de evasiones de un jugador
     */
    public String getPlayerEvasionInfo(UUID uuid) {
        int evasions = evasionCount.getOrDefault(uuid, 0);
        int pendingLevel = pendingPunishment.getOrDefault(uuid, 0);
        Long lastTime = lastEvasionTime.get(uuid);
        
        if (evasions == 0 && pendingLevel == 0) {
            return "§aSin evasiones registradas";
        }
        
        StringBuilder info = new StringBuilder();
        info.append("§7Evasiones totales: §c").append(evasions);
        
        if (pendingLevel > 0) {
            info.append("\n§7Castigo pendiente: §c").append("Nivel ").append(pendingLevel);
        }
        
        if (lastTime != null) {
            long hoursAgo = (System.currentTimeMillis() - lastTime) / 3600000;
            info.append("\n§7Última evasión: §e").append(hoursAgo).append("h atrás");
        }
        
        return info.toString();
    }
    
    /**
     * Obtiene estadísticas globales de evasiones
     */
    public java.util.Map<String, Object> getGlobalStats() {
        java.util.Map<String, Object> stats = new HashMap<>();
        
        int totalEvasiones = 0;
        for (int count : evasionCount.values()) {
            totalEvasiones += count;
        }
        
        double nivelPromedio = 0;
        if (!evasionCount.isEmpty()) {
            nivelPromedio = totalEvasiones / (double) evasionCount.size();
        }
        
        stats.put("jugadores_con_evasiones", evasionCount.size());
        stats.put("evasiones_totales", totalEvasiones);
        stats.put("castigos_pendientes", pendingPunishment.size());
        stats.put("nivel_promedio", nivelPromedio);
        stats.put("jugadores_online_trackeados", playerJoinTime.size());
        
        return stats;
    }
    
    /**
     * Obtiene el historial de evasiones de un jugador desde el archivo
     */
    public java.util.List<String> getPlayerHistory(UUID uuid) {
        java.util.List<String> history = new java.util.ArrayList<>();
        
        try {
            File historyFile = new File(plugin.getDataFolder(), "evasion_history.yml");
            if (historyFile.exists()) {
                FileConfiguration historyConfig = YamlConfiguration.loadConfiguration(historyFile);
                history = historyConfig.getStringList(uuid.toString());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[EvasionTracker] Error leyendo historial: " + e.getMessage());
        }
        
        return history;
    }
    
    /**
     * Reduce el número de evasiones de un jugador
     * @return Número de evasiones reducidas
     */
    public int reduceEvasions(UUID uuid, int cantidad) {
        int current = evasionCount.getOrDefault(uuid, 0);
        if (current <= 0) {
            return 0;
        }
        
        int toReduce = Math.min(current, cantidad);
        int newCount = current - toReduce;
        
        if (newCount <= 0) {
            evasionCount.remove(uuid);
            lastEvasionTime.remove(uuid);
        } else {
            evasionCount.put(uuid, newCount);
        }
        
        saveData();
        plugin.getLogger().info("[EvasionTracker] Reducidas " + toReduce + " evasiones de UUID: " + uuid);
        
        return toReduce;
    }
}
