package me.apocalipsis.state;

import me.apocalipsis.Apocalipsis;

/**
 * TimeService - Sistema de tiempo WALL-CLOCK PURO
 * NO usa ticks, solo System.currentTimeMillis()
 */
public final class TimeService {
    private final Apocalipsis plugin;
    private volatile boolean running;
    private volatile long startEpochMs;
    private volatile int plannedSeconds;
    private volatile String currentId;

    public TimeService(Apocalipsis plugin) {
        this.plugin = plugin;
    }

    public void startDisaster(String id, int seconds) {
        this.currentId = id;
        this.plannedSeconds = Math.max(1, seconds);   // segundos exactos (900 = 15m)
        this.startEpochMs = System.currentTimeMillis();
        this.running = true;
        
        // LOG para verificar que baja 1 por segundo
        plugin.getLogger().info("[TimeService] INICIO - ID: " + id + " | Planeado: " + plannedSeconds + "s | Epoch: " + startEpochMs);
    }

    public void startPreparationMinutes(int minutes) {
        this.currentId = "PREPARACION";
        this.plannedSeconds = Math.max(1, minutes * 60);
        this.startEpochMs = System.currentTimeMillis();
        this.running = true;
        
        plugin.getLogger().info("[TimeService] PREPARACION - Minutos: " + minutes + " | Planeado: " + plannedSeconds + "s");
    }

    public void end() { 
        this.running = false; 
        plugin.getLogger().info("[TimeService] FIN - ID: " + currentId + " | Remaining: " + getRemainingSeconds() + "s");
    }

    public int getRemainingSeconds() {
        if (!running) return 0;
        long end = startEpochMs + (plannedSeconds * 1000L);
        return (int) Math.max(0, (end - System.currentTimeMillis()) / 1000L);
    }

    public double getProgress01() {
        if (!running || plannedSeconds <= 0) return 0;
        double elapsed = (System.currentTimeMillis() - startEpochMs) / 1000.0;
        return Math.max(0, Math.min(1, 1 - (elapsed / plannedSeconds)));
    }

    public String getClockMMSS() {
        int s = getRemainingSeconds();
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    public boolean isFinished() { 
        return running && getRemainingSeconds() <= 0; 
    }

    public int getPlannedSeconds() { return plannedSeconds; }
    public String getCurrentId() { return currentId; }
    public boolean isRunning() { return running; }
    
    /**
     * Ajusta dinámicamente el tiempo restante (usado por /avo time set|add)
     * @param newRemainingSeconds Nuevos segundos restantes
     */
    public void setRemainingSeconds(int newRemainingSeconds) {
        if (!running) return;
        
        // Recalcular startEpochMs para que getRemainingSeconds() devuelva el nuevo valor
        long now = System.currentTimeMillis();
        this.plannedSeconds = newRemainingSeconds;
        this.startEpochMs = now;
        
        plugin.getLogger().info("[TimeService] Tiempo ajustado a " + newRemainingSeconds + "s restantes");
    }
    
    /**
     * Añade tiempo al contador actual
     * @param additionalSeconds Segundos a añadir
     */
    public void addTime(int additionalSeconds) {
        if (!running) return;
        
        int currentRemaining = getRemainingSeconds();
        setRemainingSeconds(currentRemaining + additionalSeconds);
        
        plugin.getLogger().info("[TimeService] Añadidos " + additionalSeconds + "s (total: " + (currentRemaining + additionalSeconds) + "s)");
    }
}
