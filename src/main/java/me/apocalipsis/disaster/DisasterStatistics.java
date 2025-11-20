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
        public final double damageReceived;
        
        public PlayerStats(UUID uuid, String playerName, int highestPhaseReached, int deaths, long survivalTicks, double damageReceived) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.highestPhaseReached = highestPhaseReached;
            this.deaths = deaths;
            this.survivalTicks = survivalTicks;
            this.damageReceived = damageReceived;
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
                                          Map<UUID, Integer> deathsMap,
                                          Map<UUID, Double> damageMap) {
        
        // Preparar lista de estadísticas
        List<PlayerStats> allStats = new ArrayList<>();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            int phaseReached = survivalPhases.getOrDefault(uuid, 1);
            int deaths = deathsMap.getOrDefault(uuid, 0);
            double damage = damageMap.getOrDefault(uuid, 0.0);
            
            allStats.add(new PlayerStats(uuid, player.getName(), phaseReached, deaths, totalTicks, damage));
        }
        
        // Ordenar por fase alcanzada (descendente), luego por muertes (ascendente)
        allStats.sort(Comparator
            .comparingInt((PlayerStats s) -> s.highestPhaseReached)
            .reversed()
            .thenComparingInt(s -> s.deaths));
        
        // Mostrar resumen a todos los jugadores (bypass chat plugins)
        broadcastRaw("");
        broadcastRaw("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        broadcastRaw("§c§l  ☠  RESUMEN DEL DESASTRE  ☠");
        broadcastRaw("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        broadcastRaw("");
        broadcastRaw("§7Desastre: §e" + disasterName);
        broadcastRaw("§7Duración: §e" + formatTime(totalTicks));
        broadcastRaw("§7Fases totales: §e" + totalPhases);
        broadcastRaw("");
        
        // Mostrar top 3 supervivientes
        showTopSurvivors(allStats, totalPhases);
        
        broadcastRaw("");
        
        // Mostrar top 3 daño recibido
        showTopDamageReceived(allStats);
        
        broadcastRaw("");
        broadcastRaw("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        broadcastRaw("");
        
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
        broadcastRaw("§6§l  ⚔ TOP SUPERVIVIENTES ⚔");
        broadcastRaw("");
        
        int rank = 1;
        for (int i = 0; i < Math.min(3, allStats.size()); i++) {
            PlayerStats stats = allStats.get(i);
            
            String medal = getMedal(rank);
            String playerDisplay = medal + " §f" + stats.playerName;
            String phaseInfo = getPhaseColor(stats.highestPhaseReached, totalPhases) + 
                              "Fase " + stats.highestPhaseReached + "/" + totalPhases;
            String deathsInfo = stats.deaths == 0 ? "§a§l¡Sin muertes!" : "§7(" + stats.deaths + " muerte" + (stats.deaths == 1 ? "" : "s") + ")";
            
            broadcastRaw(playerDisplay + " §8- " + phaseInfo + " " + deathsInfo);
            rank++;
        }
        
        if (allStats.isEmpty()) {
            broadcastRaw("§7  No hay supervivientes registrados");
        }
    }
    
    /**
     * Muestra el top 3 de daño recibido
     */
    private static void showTopDamageReceived(List<PlayerStats> allStats) {
        broadcastRaw("§c§l  ⚠ TOP DAÑO RECIBIDO ⚠");
        broadcastRaw("");
        
        // Ordenar por daño recibido (descendente)
        List<PlayerStats> damageRanking = new ArrayList<>(allStats);
        damageRanking.sort(Comparator.comparingDouble((PlayerStats s) -> s.damageReceived).reversed());
        
        int rank = 1;
        for (int i = 0; i < Math.min(3, damageRanking.size()); i++) {
            PlayerStats stats = damageRanking.get(i);
            
            if (stats.damageReceived <= 0) continue; // Saltar jugadores sin daño
            
            String medal = getMedal(rank);
            String playerDisplay = medal + " §f" + stats.playerName;
            String damageInfo = "§c" + String.format("%.1f", stats.damageReceived) + " ❤";
            String heartsInfo = "§7(" + String.format("%.1f", stats.damageReceived / 2.0) + " corazones)";
            
            broadcastRaw(playerDisplay + " §8- " + damageInfo + " " + heartsInfo);
            rank++;
        }
        
        if (damageRanking.isEmpty() || damageRanking.get(0).damageReceived <= 0) {
            broadcastRaw("§7  No se registró daño");
        }
    }
    
    /**
     * Envía estadísticas personales al jugador
     */
    private static void sendPersonalStats(Player player, PlayerStats stats, int totalPhases) {
        player.spigot().sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText(""));
        player.spigot().sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§8§l┌─ §6§lTUS ESTADÍSTICAS §8§l─┐"));
        player.spigot().sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§8│"));
        
        // Fase alcanzada
        String phaseColor = getPhaseColor(stats.highestPhaseReached, totalPhases);
        player.spigot().sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§8│ §7Fase máxima alcanzada: " + phaseColor + "§l" + stats.highestPhaseReached + "/" + totalPhases));
        
        // Evaluación de rendimiento
        String performance = getPerformanceEvaluation(stats.highestPhaseReached, totalPhases, stats.deaths);
        player.spigot().sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§8│ §7Rendimiento: " + performance));
        
        // Muertes
        if (stats.deaths == 0) {
            player.spigot().sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§8│ §a§l✓ §a¡Sobreviviste sin morir!"));
        } else {
            player.spigot().sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§8│ §7Muertes: §c" + stats.deaths));
        }
        
        // Tiempo
        player.spigot().sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§8│ §7Tiempo en el desastre: §e" + stats.getSurvivalTimeFormatted()));
        
        // Daño recibido
        if (stats.damageReceived > 0) {
            String damageColor = stats.damageReceived >= 40 ? "§4" : stats.damageReceived >= 20 ? "§c" : "§e";
            player.spigot().sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§8│ §7Daño recibido: " + damageColor + String.format("%.1f", stats.damageReceived) + " ❤ §7(" + String.format("%.1f", stats.damageReceived / 2.0) + " corazones)"));
        }
        
        player.spigot().sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§8│"));
        player.spigot().sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§8└─────────────────────┘"));
        player.spigot().sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText(""));
    }
    
    /**
     * Envía un mensaje a todos los jugadores bypasseando plugins de chat
     */
    private static void broadcastRaw(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.spigot().sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
        }
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
