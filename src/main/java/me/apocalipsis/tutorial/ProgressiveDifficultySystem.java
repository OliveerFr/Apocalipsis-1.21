/*
 * Apocalipsis Plugin - Sistema de Dificultad Progresiva para Nuevos Jugadores
 * Copyright (c) 2025 Apocalipsis Plugin
 * 
 * Licensed under the MIT License.
 */
package me.apocalipsis.tutorial;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import me.apocalipsis.Apocalipsis;

/**
 * Sistema de dificultad progresiva para nuevos jugadores.
 * 
 * Los desastres empiezan muy suaves (10% dificultad) y aumentan gradualmente
 * durante las primeras 4 horas de juego hasta alcanzar la dificultad global (100%).
 * 
 * Fases:
 * - Fase 1 (0-30 min): 10% daño, 20% frecuencia - MUY FÁCIL
 * - Fase 2 (30-60 min): 25% daño, 40% frecuencia - FÁCIL
 * - Fase 3 (1-2 horas): 50% daño, 60% frecuencia - MODERADO
 * - Fase 4 (2-4 horas): 75% daño, 80% frecuencia - NORMAL
 * - Fase Final (4+ horas): 100% daño, 100% frecuencia - DIFICULTAD GLOBAL
 */
public class ProgressiveDifficultySystem {
    
    private final Apocalipsis plugin;
    private final FileConfiguration config;
    
    // Almacena el timestamp de primera conexión de cada jugador
    private final Map<UUID, Long> firstJoinTime;
    
    // Cache de fases para evitar recalcular constantemente
    private final Map<UUID, DifficultyPhase> phaseCache;
    private final Map<UUID, Long> phaseCacheTimestamp;
    private static final long CACHE_DURATION_MS = 30000; // 30 segundos
    
    private boolean enabled;
    private boolean verboseLogging;
    
    /**
     * Representa una fase de dificultad con sus multiplicadores
     */
    public static class DifficultyPhase {
        private final int phaseNumber;
        private final String name;
        private final String description;
        private final double damageMultiplier;
        private final double frequencyMultiplier;
        private final boolean allowKnockback;
        private final double xpBonus;
        private final boolean passiveRegeneration;
        private final int percentDifficulty;
        
        public DifficultyPhase(int phaseNumber, String name, String description,
                              double damageMultiplier, double frequencyMultiplier,
                              boolean allowKnockback, double xpBonus,
                              boolean passiveRegeneration) {
            this.phaseNumber = phaseNumber;
            this.name = name;
            this.description = description;
            this.damageMultiplier = damageMultiplier;
            this.frequencyMultiplier = frequencyMultiplier;
            this.allowKnockback = allowKnockback;
            this.xpBonus = xpBonus;
            this.passiveRegeneration = passiveRegeneration;
            this.percentDifficulty = (int) (damageMultiplier * 100);
        }
        
        public int getPhaseNumber() { return phaseNumber; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public double getDamageMultiplier() { return damageMultiplier; }
        public double getFrequencyMultiplier() { return frequencyMultiplier; }
        public boolean allowKnockback() { return allowKnockback; }
        public double getXpBonus() { return xpBonus; }
        public boolean hasPassiveRegeneration() { return passiveRegeneration; }
        public int getPercentDifficulty() { return percentDifficulty; }
        
        public boolean isGlobalDifficulty() {
            return damageMultiplier >= 1.0 && frequencyMultiplier >= 1.0;
        }
    }
    
    public ProgressiveDifficultySystem(Apocalipsis plugin, FileConfiguration tutorialConfig) {
        this.plugin = plugin;
        this.config = tutorialConfig;
        this.firstJoinTime = new HashMap<>();
        this.phaseCache = new HashMap<>();
        this.phaseCacheTimestamp = new HashMap<>();
        
        loadConfig();
    }
    
    /**
     * Carga la configuración desde tutorial.yml
     */
    private void loadConfig() {
        this.enabled = config.getBoolean("dificultad_progresiva.enabled", true);
        this.verboseLogging = config.getBoolean("tutorial.verbose_logging", false);
        
        if (verboseLogging) {
            plugin.getLogger().info("[Tutorial] Sistema de dificultad progresiva " + 
                                   (enabled ? "activado" : "desactivado"));
        }
    }
    
    /**
     * Registra el primer join de un jugador
     */
    public void registerFirstJoin(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Solo registrar si no existe ya
        if (!firstJoinTime.containsKey(uuid)) {
            long now = System.currentTimeMillis();
            firstJoinTime.put(uuid, now);
            
            if (verboseLogging) {
                plugin.getLogger().info(String.format(
                    "[Tutorial] Jugador %s registrado. Inicio de dificultad progresiva.",
                    player.getName()
                ));
            }
        }
    }
    
    /**
     * Obtiene el tiempo jugado de un jugador en minutos
     */
    public long getPlayedTimeMinutes(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!firstJoinTime.containsKey(uuid)) {
            return 0;
        }
        
        long startTime = firstJoinTime.get(uuid);
        long currentTime = System.currentTimeMillis();
        long diffMs = currentTime - startTime;
        
        return TimeUnit.MILLISECONDS.toMinutes(diffMs);
    }
    
    /**
     * Obtiene la fase actual de dificultad para un jugador
     */
    public DifficultyPhase getPlayerPhase(Player player) {
        if (!enabled) {
            return createFinalPhase(); // Dificultad global si está desactivado
        }
        
        UUID uuid = player.getUniqueId();
        
        // Verificar cache
        if (phaseCache.containsKey(uuid)) {
            long cacheTime = phaseCacheTimestamp.getOrDefault(uuid, 0L);
            if (System.currentTimeMillis() - cacheTime < CACHE_DURATION_MS) {
                return phaseCache.get(uuid);
            }
        }
        
        // Calcular fase
        long playedMinutes = getPlayedTimeMinutes(player);
        DifficultyPhase phase = calculatePhase(playedMinutes);
        
        // Actualizar cache
        phaseCache.put(uuid, phase);
        phaseCacheTimestamp.put(uuid, System.currentTimeMillis());
        
        return phase;
    }
    
    /**
     * Calcula la fase según los minutos jugados
     */
    private DifficultyPhase calculatePhase(long playedMinutes) {
        ConfigurationSection phases = config.getConfigurationSection("dificultad_progresiva");
        
        if (phases == null) {
            return createFinalPhase();
        }
        
        // Fase 1: 0-30 minutos
        int phase1Duration = phases.getInt("fase_1.duracion_minutos", 30);
        if (playedMinutes < phase1Duration) {
            return createPhaseFromConfig(1, phases.getConfigurationSection("fase_1"));
        }
        
        // Fase 2: 30-60 minutos
        int phase2Duration = phases.getInt("fase_2.duracion_minutos", 30);
        if (playedMinutes < phase1Duration + phase2Duration) {
            return createPhaseFromConfig(2, phases.getConfigurationSection("fase_2"));
        }
        
        // Fase 3: 1-2 horas
        int phase3Duration = phases.getInt("fase_3.duracion_minutos", 60);
        if (playedMinutes < phase1Duration + phase2Duration + phase3Duration) {
            return createPhaseFromConfig(3, phases.getConfigurationSection("fase_3"));
        }
        
        // Fase 4: 2-4 horas
        int phase4Duration = phases.getInt("fase_4.duracion_minutos", 120);
        if (playedMinutes < phase1Duration + phase2Duration + phase3Duration + phase4Duration) {
            return createPhaseFromConfig(4, phases.getConfigurationSection("fase_4"));
        }
        
        // Fase Final: 4+ horas (dificultad global)
        return createPhaseFromConfig(5, phases.getConfigurationSection("fase_final"));
    }
    
    /**
     * Crea un objeto DifficultyPhase desde la configuración
     */
    private DifficultyPhase createPhaseFromConfig(int phaseNumber, ConfigurationSection section) {
        if (section == null) {
            return createFinalPhase();
        }
        
        String name = section.getString("nombre", "&7Fase " + phaseNumber);
        String description = section.getString("descripcion", "Normal");
        double damageMultiplier = section.getDouble("multiplicador_daño", 1.0);
        double frequencyMultiplier = section.getDouble("multiplicador_frecuencia", 1.0);
        boolean allowKnockback = section.getBoolean("permitir_knockback", true);
        double xpBonus = section.getDouble("xp_bonus", 1.0);
        boolean passiveRegen = section.getBoolean("regeneracion_pasiva", false);
        
        return new DifficultyPhase(
            phaseNumber, name, description,
            damageMultiplier, frequencyMultiplier,
            allowKnockback, xpBonus, passiveRegen
        );
    }
    
    /**
     * Crea la fase final (dificultad global 100%)
     */
    private DifficultyPhase createFinalPhase() {
        return new DifficultyPhase(
            5, "&4💀 Dificultad Global", "Sin protección",
            1.0, 1.0, true, 1.0, false
        );
    }
    
    /**
     * Verifica si un jugador ha alcanzado la dificultad global
     */
    public boolean hasReachedGlobalDifficulty(Player player) {
        DifficultyPhase phase = getPlayerPhase(player);
        return phase.isGlobalDifficulty();
    }
    
    /**
     * Obtiene el tiempo restante hasta la próxima fase en minutos
     */
    public long getRemainingTimeToNextPhase(Player player) {
        long playedMinutes = getPlayedTimeMinutes(player);
        ConfigurationSection phases = config.getConfigurationSection("dificultad_progresiva");
        
        if (phases == null) {
            return 0;
        }
        
        int phase1Duration = phases.getInt("fase_1.duracion_minutos", 30);
        int phase2Duration = phases.getInt("fase_2.duracion_minutos", 30);
        int phase3Duration = phases.getInt("fase_3.duracion_minutos", 60);
        int phase4Duration = phases.getInt("fase_4.duracion_minutos", 120);
        
        // Calcular próximo hito
        if (playedMinutes < phase1Duration) {
            return phase1Duration - playedMinutes;
        } else if (playedMinutes < phase1Duration + phase2Duration) {
            return (phase1Duration + phase2Duration) - playedMinutes;
        } else if (playedMinutes < phase1Duration + phase2Duration + phase3Duration) {
            return (phase1Duration + phase2Duration + phase3Duration) - playedMinutes;
        } else if (playedMinutes < phase1Duration + phase2Duration + phase3Duration + phase4Duration) {
            return (phase1Duration + phase2Duration + phase3Duration + phase4Duration) - playedMinutes;
        }
        
        return 0; // Ya alcanzó dificultad global
    }
    
    /**
     * Formatea el tiempo restante en formato legible
     */
    public String formatRemainingTime(long minutes) {
        if (minutes <= 0) {
            return "Dificultad global alcanzada";
        }
        
        if (minutes < 60) {
            return minutes + " minutos";
        }
        
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        
        if (remainingMinutes == 0) {
            return hours + (hours == 1 ? " hora" : " horas");
        }
        
        return String.format("%d %s y %d min",
            hours, (hours == 1 ? "hora" : "horas"), remainingMinutes);
    }
    
    /**
     * Limpia el cache de un jugador al desconectarse
     */
    public void clearPlayerCache(UUID uuid) {
        phaseCache.remove(uuid);
        phaseCacheTimestamp.remove(uuid);
    }
    
    /**
     * Limpia todos los datos de un jugador (para comando reset)
     */
    public void resetPlayer(UUID uuid) {
        firstJoinTime.remove(uuid);
        clearPlayerCache(uuid);
        
        if (verboseLogging) {
            plugin.getLogger().info(String.format(
                "[Tutorial] Datos de dificultad progresiva reseteados para UUID: %s",
                uuid.toString()
            ));
        }
    }
    
    /**
     * Establece manualmente la fase de un jugador (para testing/admin)
     */
    public void setPlayerPhase(UUID uuid, int phaseNumber) {
        ConfigurationSection phases = config.getConfigurationSection("dificultad_progresiva");
        if (phases == null) return;
        
        // Calcular tiempo necesario para esa fase
        long targetMinutes = 0;
        
        switch (phaseNumber) {
            case 1:
                targetMinutes = 0;
                break;
            case 2:
                targetMinutes = phases.getInt("fase_1.duracion_minutos", 30);
                break;
            case 3:
                targetMinutes = phases.getInt("fase_1.duracion_minutos", 30) +
                               phases.getInt("fase_2.duracion_minutos", 30);
                break;
            case 4:
                targetMinutes = phases.getInt("fase_1.duracion_minutos", 30) +
                               phases.getInt("fase_2.duracion_minutos", 30) +
                               phases.getInt("fase_3.duracion_minutos", 60);
                break;
            case 5:
                targetMinutes = 999999; // Dificultad global
                break;
        }
        
        // Ajustar el timestamp de primera conexión
        long targetTimestamp = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(targetMinutes);
        firstJoinTime.put(uuid, targetTimestamp);
        clearPlayerCache(uuid);
        
        if (verboseLogging) {
            plugin.getLogger().info(String.format(
                "[Tutorial] Fase manual establecida para UUID %s: Fase %d",
                uuid.toString(), phaseNumber
            ));
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void reload() {
        loadConfig();
        phaseCache.clear();
        phaseCacheTimestamp.clear();
    }
}
