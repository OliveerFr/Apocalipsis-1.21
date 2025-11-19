package me.apocalipsis.events.gameplay;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.apocalipsis.Apocalipsis;

/**
 * Sistema de balanceo dinámico de dificultad
 * 
 * Ajusta automáticamente:
 * - Stats de mobs según número de jugadores
 * - Cantidad de spawns según performance del servidor
 * - Duraciones de fases según progreso
 * - Recompensas según dificultad efectiva
 */
public class DifficultyScaler {
    
    private final Apocalipsis plugin;
    
    // Configuración base
    private final int minPlayers;
    private final int maxPlayers;
    private final double baseMultiplier;
    
    // Multiplicadores por jugador
    private static final double HEALTH_PER_PLAYER = 0.30;    // +30% HP por jugador extra
    private static final double DAMAGE_PER_PLAYER = 0.15;    // +15% daño por jugador extra
    private static final double SPAWN_PER_PLAYER = 0.25;     // +25% spawns por jugador extra
    
    // Límites de escala
    private static final double MIN_MULTIPLIER = 0.5;        // Mínimo 50% stats
    private static final double MAX_MULTIPLIER = 3.0;        // Máximo 300% stats
    
    // Performance tracking
    private final List<Long> tickTimes = new ArrayList<>();
    private static final int SAMPLE_SIZE = 100;
    private double currentTPS = 20.0;
    
    /**
     * Constructor
     */
    public DifficultyScaler(Apocalipsis plugin, int minPlayers, int maxPlayers, double baseMultiplier) {
        this.plugin = plugin;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.baseMultiplier = baseMultiplier;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ESCALADO POR JUGADORES
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Calcula multiplicador de HP según jugadores
     */
    public double getHealthMultiplier(int playerCount) {
        int adjustedCount = Math.max(minPlayers, Math.min(maxPlayers, playerCount));
        double multiplier = baseMultiplier * (1.0 + (adjustedCount - minPlayers) * HEALTH_PER_PLAYER);
        return clamp(multiplier, MIN_MULTIPLIER, MAX_MULTIPLIER);
    }
    
    /**
     * Calcula multiplicador de daño según jugadores
     */
    public double getDamageMultiplier(int playerCount) {
        int adjustedCount = Math.max(minPlayers, Math.min(maxPlayers, playerCount));
        double multiplier = baseMultiplier * (1.0 + (adjustedCount - minPlayers) * DAMAGE_PER_PLAYER);
        return clamp(multiplier, MIN_MULTIPLIER, MAX_MULTIPLIER);
    }
    
    /**
     * Calcula cantidad de spawns según jugadores
     */
    public int getScaledSpawnCount(int baseCount, int playerCount) {
        int adjustedCount = Math.max(minPlayers, Math.min(maxPlayers, playerCount));
        double multiplier = 1.0 + (adjustedCount - minPlayers) * SPAWN_PER_PLAYER;
        return Math.max(1, (int) Math.round(baseCount * multiplier));
    }
    
    /**
     * Calcula velocidad según jugadores (más jugadores = más lento para balance)
     */
    public double getSpeedMultiplier(int playerCount) {
        int adjustedCount = Math.max(minPlayers, Math.min(maxPlayers, playerCount));
        // Velocidad NO aumenta tanto (solo +5% por jugador extra)
        double multiplier = baseMultiplier * (1.0 + (adjustedCount - minPlayers) * 0.05);
        return clamp(multiplier, 0.8, 1.5); // Límites más estrictos
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ESCALADO POR PERFORMANCE
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Actualiza medición de TPS
     */
    public void updatePerformance(long tickTime) {
        tickTimes.add(tickTime);
        
        // Mantener solo últimas N muestras
        if (tickTimes.size() > SAMPLE_SIZE) {
            tickTimes.remove(0);
        }
        
        // Calcular TPS promedio
        if (!tickTimes.isEmpty()) {
            double avgTickTime = tickTimes.stream().mapToLong(Long::longValue).average().orElse(50.0);
            currentTPS = Math.min(20.0, 1000.0 / avgTickTime * 20.0);
        }
    }
    
    /**
     * Obtiene TPS actual
     */
    public double getCurrentTPS() {
        return currentTPS;
    }
    
    /**
     * Verifica si el servidor está bajo lag
     */
    public boolean isUnderLag() {
        return currentTPS < 18.0;
    }
    
    /**
     * Calcula multiplicador de spawns según performance
     */
    public double getPerformanceSpawnMultiplier() {
        if (currentTPS >= 19.0) {
            return 1.0; // Performance óptima
        } else if (currentTPS >= 17.0) {
            return 0.8; // Reducir 20% spawns
        } else if (currentTPS >= 15.0) {
            return 0.6; // Reducir 40% spawns
        } else {
            return 0.4; // Reducir 60% spawns (servidor muy lagueado)
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ESTADÍSTICAS Y REPORTING
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Genera reporte de dificultad actual
     */
    public Map<String, Object> getDifficultyReport(int playerCount) {
        Map<String, Object> report = new HashMap<>();
        
        report.put("players", playerCount);
        report.put("health_multiplier", String.format("%.2f", getHealthMultiplier(playerCount)));
        report.put("damage_multiplier", String.format("%.2f", getDamageMultiplier(playerCount)));
        report.put("speed_multiplier", String.format("%.2f", getSpeedMultiplier(playerCount)));
        report.put("current_tps", String.format("%.1f", currentTPS));
        report.put("spawn_multiplier", String.format("%.2f", getPerformanceSpawnMultiplier()));
        report.put("is_lagging", isUnderLag());
        
        return report;
    }
    
    /**
     * Genera mensaje formateado de dificultad
     */
    public String getDifficultyMessage(int playerCount) {
        double healthMult = getHealthMultiplier(playerCount);
        double damageMult = getDamageMultiplier(playerCount);
        
        StringBuilder msg = new StringBuilder();
        msg.append("§7§l━━━━━━ DIFICULTAD ━━━━━━\n");
        msg.append("§7Jugadores: §e").append(playerCount).append("\n");
        msg.append("§7HP Mobs: §e").append(String.format("%.0f%%", healthMult * 100)).append("\n");
        msg.append("§7Daño Mobs: §e").append(String.format("%.0f%%", damageMult * 100)).append("\n");
        msg.append("§7TPS: §e").append(String.format("%.1f", currentTPS));
        
        if (isUnderLag()) {
            msg.append(" §c(Reduciendo spawns)");
        }
        
        msg.append("\n§7§l━━━━━━━━━━━━━━━━━━━━━━");
        
        return msg.toString();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // AJUSTES DINÁMICOS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Calcula recompensas ajustadas por dificultad
     */
    public int getScaledReward(int baseReward, int playerCount) {
        double multiplier = getHealthMultiplier(playerCount);
        
        // Recompensas aumentan más moderadamente que dificultad
        double rewardMultiplier = 1.0 + (multiplier - 1.0) * 0.5;
        
        return (int) Math.round(baseReward * rewardMultiplier);
    }
    
    /**
     * Calcula duración ajustada de fase
     */
    public int getScaledDuration(int baseDuration, int playerCount) {
        // Más jugadores = más tiempo para completar
        int adjustedCount = Math.max(minPlayers, Math.min(maxPlayers, playerCount));
        double multiplier = 1.0 + (adjustedCount - minPlayers) * 0.15; // +15% por jugador
        
        return (int) Math.round(baseDuration * multiplier);
    }
    
    /**
     * Sugiere reducir dificultad si hay problemas
     */
    public boolean shouldReduceDifficulty(int playerCount, int consecutiveDeaths) {
        // Si muchos jugadores mueren consecutivamente, sugerir reducción
        return consecutiveDeaths >= playerCount * 2;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Limita un valor entre min y max
     */
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Obtiene número de jugadores en línea
     */
    public static int getOnlinePlayerCount() {
        return Bukkit.getOnlinePlayers().size();
    }
    
    /**
     * Obtiene número de jugadores vivos (no en espectador)
     */
    public static int getAlivePlayerCount() {
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                count++;
            }
        }
        return count;
    }
}
