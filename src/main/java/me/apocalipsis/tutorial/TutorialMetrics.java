/*
 * Apocalipsis Plugin - Sistema de Métricas del Tutorial
 * Copyright (c) 2025 Apocalipsis Plugin
 * 
 * Licensed under the MIT License.
 */
package me.apocalipsis.tutorial;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import me.apocalipsis.Apocalipsis;

/**
 * Sistema de tracking y métricas del tutorial
 */
public class TutorialMetrics {
    
    private final Apocalipsis plugin;
    
    // Métricas globales
    private final AtomicInteger totalTutorialsStarted = new AtomicInteger(0);
    private final AtomicInteger totalTutorialsCompleted = new AtomicInteger(0);
    private final AtomicInteger totalTutorialsAbandoned = new AtomicInteger(0);
    
    // Por fase
    private final Map<Integer, AtomicInteger> deathsPerPhase = new HashMap<>();
    private final Map<Integer, Long> totalTimeInPhase = new HashMap<>();
    private final Map<Integer, Integer> playersInPhase = new HashMap<>();
    
    public TutorialMetrics(Apocalipsis plugin) {
        this.plugin = plugin;
        
        // Inicializar contadores por fase
        for (int i = 1; i <= 5; i++) {
            deathsPerPhase.put(i, new AtomicInteger(0));
            totalTimeInPhase.put(i, 0L);
            playersInPhase.put(i, 0);
        }
    }
    
    public void recordTutorialStarted() {
        totalTutorialsStarted.incrementAndGet();
    }
    
    public void recordTutorialCompleted() {
        totalTutorialsCompleted.incrementAndGet();
    }
    
    public void recordTutorialAbandoned() {
        totalTutorialsAbandoned.incrementAndGet();
    }
    
    public void recordDeathInPhase(int phase) {
        deathsPerPhase.getOrDefault(phase, new AtomicInteger(0)).incrementAndGet();
    }
    
    public void recordPlayerInPhase(int phase) {
        playersInPhase.put(phase, playersInPhase.getOrDefault(phase, 0) + 1);
    }
    
    /**
     * Genera un reporte de métricas
     */
    public void generateReport() {
        int started = totalTutorialsStarted.get();
        int completed = totalTutorialsCompleted.get();
        int abandoned = totalTutorialsAbandoned.get();
        
        double completionRate = started > 0 ? (completed * 100.0) / started : 0;
        double abandonRate = started > 0 ? (abandoned * 100.0) / started : 0;
        
        plugin.getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        plugin.getLogger().info("📊 MÉTRICAS DE TUTORIAL");
        plugin.getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        plugin.getLogger().info(String.format("  Iniciados: %d", started));
        plugin.getLogger().info(String.format("  Completados: %d (%.1f%%)", completed, completionRate));
        plugin.getLogger().info(String.format("  Abandonados: %d (%.1f%%)", abandoned, abandonRate));
        plugin.getLogger().info("");
        plugin.getLogger().info("📈 Por Fase:");
        
        for (int i = 1; i <= 5; i++) {
            int deaths = deathsPerPhase.getOrDefault(i, new AtomicInteger(0)).get();
            int players = playersInPhase.getOrDefault(i, 0);
            plugin.getLogger().info(String.format("  Fase %d: %d jugadores, %d muertes", i, players, deaths));
        }
        
        plugin.getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
    
    /**
     * Resetea las métricas
     */
    public void reset() {
        totalTutorialsStarted.set(0);
        totalTutorialsCompleted.set(0);
        totalTutorialsAbandoned.set(0);
        
        for (int i = 1; i <= 5; i++) {
            deathsPerPhase.get(i).set(0);
            totalTimeInPhase.put(i, 0L);
            playersInPhase.put(i, 0);
        }
    }
    
    public int getTotalStarted() { return totalTutorialsStarted.get(); }
    public int getTotalCompleted() { return totalTutorialsCompleted.get(); }
    public int getTotalAbandoned() { return totalTutorialsAbandoned.get(); }
}
