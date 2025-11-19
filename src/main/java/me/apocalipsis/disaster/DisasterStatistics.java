package me.apocalipsis.disaster;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Sistema de estadísticas post-desastre
 * Muestra resumen de supervivencia, ranking y estadísticas sin otorgar recompensas
 */
public class DisasterStatistics {
    
    /**
     * Datos de estadísticas de un jugador
     */
    public static class PlayerStats {
        public final UUID uuid;
        public final String playerName;
        public final int highestPhaseReached;
        public final int deaths;
        public final long survivalTicks;
        
        public PlayerStats(UUID uuid, String playerName, int highestPhaseReached, int deaths, long survivalTicks) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.highestPhaseReached = highestPhaseReached;
            this.deaths = deaths;
            this.survivalTicks = survivalTicks;
        }
        
        /**
         * Obtiene tiempo de supervivencia en formato legible
         */
        public String getSurvivalTimeFormatted() {
            long seconds = survivalTicks / 20;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%dm %ds", minutes, seconds);
        }
    }
    
    /**
     * Muestra el resumen completo de estadísticas del desastre
     */
    public static void showDisasterSummary(String disasterName, int totalPhases, long totalTicks,
                                          Map<UUID, Integer> survivalPhases, 
                                          Map<UUID, Integer> deathsMap) {
        
        // Preparar lista de estadísticas
        List<PlayerStats> allStats = new ArrayList<>();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            int phaseReached = survivalPhases.getOrDefault(uuid, 1);
            int deaths = deathsMap.getOrDefault(uuid, 0);
            
            allStats.add(new PlayerStats(uuid, player.getName(), phaseReached, deaths, totalTicks));
        }
        
        // Ordenar por fase alcanzada (descendente), luego por muertes (ascendente)
        allStats.sort(Comparator
            .comparingInt((PlayerStats s) -> s.highestPhaseReached)
            .reversed()
            .thenComparingInt(s -> s.deaths));
        
        // Mostrar resumen a todos los jugadores
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage("§c§l  ☠  RESUMEN DEL DESASTRE  ☠");
        Bukkit.broadcastMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7Desastre: §e" + disasterName);
        Bukkit.broadcastMessage("§7Duración: §e" + formatTime(totalTicks));
        Bukkit.broadcastMessage("§7Fases totales: §e" + totalPhases);
        Bukkit.broadcastMessage("");
        
        // Mostrar top 3 supervivientes
        showTopSurvivors(allStats, totalPhases);
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage("");
        
        // Enviar estadísticas individuales a cada jugador
        for (PlayerStats stats : allStats) {
            Player player = Bukkit.getPlayer(stats.uuid);
            if (player != null && player.isOnline()) {
                sendPersonalStats(player, stats, totalPhases);
            }
        }
        
        // Sonido de finalización
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }
    
    /**
     * Muestra el top 3 de supervivientes
     */
    private static void showTopSurvivors(List<PlayerStats> allStats, int totalPhases) {
        Bukkit.broadcastMessage("§6§l  ⚔ TOP SUPERVIVIENTES ⚔");
        Bukkit.broadcastMessage("");
        
        int rank = 1;
        for (int i = 0; i < Math.min(3, allStats.size()); i++) {
            PlayerStats stats = allStats.get(i);
            
            String medal = getMedal(rank);
            String playerDisplay = medal + " §f" + stats.playerName;
            String phaseInfo = getPhaseColor(stats.highestPhaseReached, totalPhases) + 
                              "Fase " + stats.highestPhaseReached + "/" + totalPhases;
            String deathsInfo = stats.deaths == 0 ? "§a§l¡Sin muertes!" : "§7(" + stats.deaths + " muerte" + (stats.deaths == 1 ? "" : "s") + ")";
            
            Bukkit.broadcastMessage(playerDisplay + " §8- " + phaseInfo + " " + deathsInfo);
            rank++;
        }
        
        if (allStats.isEmpty()) {
            Bukkit.broadcastMessage("§7  No hay supervivientes registrados");
        }
    }
    
    /**
     * Envía estadísticas personales al jugador
     */
    private static void sendPersonalStats(Player player, PlayerStats stats, int totalPhases) {
        player.sendMessage("");
        player.sendMessage("§8§l┌─ §6§lTUS ESTADÍSTICAS §8§l─┐");
        player.sendMessage("§8│");
        
        // Fase alcanzada
        String phaseColor = getPhaseColor(stats.highestPhaseReached, totalPhases);
        player.sendMessage("§8│ §7Fase máxima alcanzada: " + phaseColor + "§l" + stats.highestPhaseReached + "/" + totalPhases);
        
        // Evaluación de rendimiento
        String performance = getPerformanceEvaluation(stats.highestPhaseReached, totalPhases, stats.deaths);
        player.sendMessage("§8│ §7Rendimiento: " + performance);
        
        // Muertes
        if (stats.deaths == 0) {
            player.sendMessage("§8│ §a§l✓ §a¡Sobreviviste sin morir!");
        } else {
            player.sendMessage("§8│ §7Muertes: §c" + stats.deaths);
        }
        
        // Tiempo
        player.sendMessage("§8│ §7Tiempo en el desastre: §e" + stats.getSurvivalTimeFormatted());
        
        player.sendMessage("§8│");
        player.sendMessage("§8└─────────────────────┘");
        player.sendMessage("");
    }
    
    /**
     * Obtiene la medalla del ranking
     */
    private static String getMedal(int rank) {
        switch (rank) {
            case 1: return "§6§l🥇";
            case 2: return "§7§l🥈";
            case 3: return "§c§l🥉";
            default: return "§8" + rank + ".";
        }
    }
    
    /**
     * Obtiene el color según la fase alcanzada
     */
    private static String getPhaseColor(int phase, int totalPhases) {
        double progress = (double) phase / totalPhases;
        
        if (progress >= 1.0) return "§d§l"; // Completó todas las fases - Púrpura
        if (progress >= 0.8) return "§6§l"; // Fase 4-5 - Dorado
        if (progress >= 0.6) return "§e";   // Fase 3 - Amarillo
        if (progress >= 0.4) return "§f";   // Fase 2 - Blanco
        return "§7";                         // Fase 1 - Gris
    }
    
    /**
     * Obtiene evaluación de rendimiento
     */
    private static String getPerformanceEvaluation(int phase, int totalPhases, int deaths) {
        double progress = (double) phase / totalPhases;
        
        if (progress >= 1.0 && deaths == 0) {
            return "§d§l★★★ LEGENDARIO";
        } else if (progress >= 1.0 && deaths <= 2) {
            return "§6§l★★★ EXCELENTE";
        } else if (progress >= 0.8 && deaths <= 1) {
            return "§6§l★★ MUY BUENO";
        } else if (progress >= 0.6 || deaths <= 3) {
            return "§e★★ BUENO";
        } else if (progress >= 0.4) {
            return "§f★ REGULAR";
        } else {
            return "§7PRINCIPIANTE";
        }
    }
    
    /**
     * Formatea tiempo en formato legible
     */
    private static String formatTime(long ticks) {
        long seconds = ticks / 20;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }
}
