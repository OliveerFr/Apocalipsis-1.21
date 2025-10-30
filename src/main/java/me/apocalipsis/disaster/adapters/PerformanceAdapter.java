package me.apocalipsis.disaster.adapters;

import org.bukkit.Bukkit;

import me.apocalipsis.Apocalipsis;

public class PerformanceAdapter {

    private final Apocalipsis plugin;
    
    private PerformanceState currentState = PerformanceState.NORMAL;
    private double lastTPS = 20.0;
    private long stateChangeTime = 0;
    private int taskId = -1;

    // Configuración
    private double normalThreshold = 18.0;
    private double degradedThreshold = 14.0;
    private double criticalThreshold = 10.0;
    private int safeModeHoldSecs = 10;
    private int recoveryHoldSecs = 10;

    public PerformanceAdapter(Apocalipsis plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        try {
            normalThreshold = plugin.getConfigManager().getConfig().getDouble("rendimiento.tps_thresholds.normal", 18.0);
            degradedThreshold = plugin.getConfigManager().getConfig().getDouble("rendimiento.tps_thresholds.degraded", 14.0);
            criticalThreshold = plugin.getConfigManager().getConfig().getDouble("rendimiento.tps_thresholds.critical", 10.0);
            safeModeHoldSecs = plugin.getConfigManager().getConfig().getInt("rendimiento.tps_thresholds.safe_mode_hold_secs", 10);
            recoveryHoldSecs = plugin.getConfigManager().getConfig().getInt("rendimiento.tps_thresholds.recovery_hold_secs", 10);
        } catch (Exception e) {
            plugin.getLogger().warning("Error cargando config de PerformanceAdapter: " + e.getMessage());
        }
    }

    public void startMonitoring() {
        // Recargar configuración antes de iniciar
        loadConfig();
        // Tick cada 2 segundos (40 ticks)
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 40L).getTaskId();
    }

    public void stopMonitoring() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void tick() {
        lastTPS = getAverageTPS();
        PerformanceState newState = calculateState();

        // Cambio de estado con hold time
        if (newState != currentState) {
            long now = System.currentTimeMillis();
            long holdTime = (newState.ordinal() > currentState.ordinal()) ? 
                safeModeHoldSecs * 1000L : recoveryHoldSecs * 1000L;

            if (stateChangeTime == 0) {
                stateChangeTime = now;
            } else if (now - stateChangeTime >= holdTime) {
                changeState(newState);
                stateChangeTime = 0;
            }
        } else {
            stateChangeTime = 0;
        }
    }

    private PerformanceState calculateState() {
        if (lastTPS < criticalThreshold) {
            return PerformanceState.SAFE_MODE;
        } else if (lastTPS < degradedThreshold) {
            return PerformanceState.CRITICAL;
        } else if (lastTPS < normalThreshold) {
            return PerformanceState.DEGRADED;
        }
        return PerformanceState.NORMAL;
    }

    private void changeState(PerformanceState newState) {
        PerformanceState oldState = currentState;
        currentState = newState;

        plugin.getLogger().info("§ePerformanceAdapter: " + oldState + " → " + newState + " (TPS: " + String.format("%.1f", lastTPS) + ")");

        // Notificar al DisasterController si entramos/salimos de SAFE_MODE
        if (newState == PerformanceState.SAFE_MODE && oldState != PerformanceState.SAFE_MODE) {
            plugin.getDisasterController().enterSafeMode();
        } else if (newState != PerformanceState.SAFE_MODE && oldState == PerformanceState.SAFE_MODE) {
            plugin.getDisasterController().exitSafeMode();
        }
    }

    /**
     * Obtiene el multiplicador de escala según el estado actual
     */
    public double getScale() {
        return switch (currentState) {
            case NORMAL -> 1.0;
            case DEGRADED -> 0.7;
            case CRITICAL -> 0.45;
            case SAFE_MODE -> 0.0; // Desactivar efectos pesados
        };
    }

    public PerformanceState getCurrentState() {
        return currentState;
    }

    public double getLastTPS() {
        return lastTPS;
    }

    private double getAverageTPS() {
        try {
            return Bukkit.getTPS()[0];
        } catch (Exception e) {
            return 20.0;
        }
    }

    public enum PerformanceState {
        NORMAL,
        DEGRADED,
        CRITICAL,
        SAFE_MODE
    }
}
