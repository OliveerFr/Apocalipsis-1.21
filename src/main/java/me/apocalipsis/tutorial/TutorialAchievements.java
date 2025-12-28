/*
 * Apocalipsis Plugin - Sistema de Logros del Tutorial
 * Copyright (c) 2025 Apocalipsis Plugin
 * 
 * Licensed under the MIT License.
 */
package me.apocalipsis.tutorial;

import me.apocalipsis.Apocalipsis;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Gestiona logros específicos del tutorial
 */
public class TutorialAchievements {
    
    private final Apocalipsis plugin;
    private final Map<UUID, Set<String>> unlockedAchievements;
    
    public TutorialAchievements(Apocalipsis plugin) {
        this.plugin = plugin;
        this.unlockedAchievements = new HashMap<>();
    }
    
    /**
     * Desbloquea un logro para un jugador
     */
    public void unlockAchievement(Player player, String achievementId) {
        UUID uuid = player.getUniqueId();
        
        Set<String> playerAchievements = unlockedAchievements.computeIfAbsent(uuid, k -> new HashSet<>());
        
        if (playerAchievements.contains(achievementId)) {
            return; // Ya desbloqueado
        }
        
        playerAchievements.add(achievementId);
        
        // Mostrar notificación
        showAchievementUnlocked(player, achievementId);
        
        // Dar recompensa
        giveAchievementReward(player, achievementId);
    }
    
    private void showAchievementUnlocked(Player player, String achievementId) {
        String title = getAchievementTitle(achievementId);
        String description = getAchievementDescription(achievementId);
        
        player.sendTitle(
            ChatColor.translateAlternateColorCodes('&', "&6&l✓ LOGRO DESBLOQUEADO"),
            ChatColor.translateAlternateColorCodes('&', title),
            10, 70, 20
        );
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&l✓ LOGRO DESBLOQUEADO"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', title));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7" + description));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        
        try {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        } catch (Exception e) {
            // Ignorar si el sonido no existe
        }
    }
    
    private void giveAchievementReward(Player player, String achievementId) {
        switch (achievementId) {
            case "primer_desastre_evadido":
                // XP bonus ya se da en el sistema de evasión
                break;
                
            case "primera_mision_completada":
                // XP bonus ya se da en el sistema de misiones
                break;
                
            case "treinta_minutos_supervivencia":
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                    "give " + player.getName() + " minecraft:golden_apple 3");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&a[Recompensa] &7+3 Manzanas Doradas"));
                break;
                
            case "alcanzar_fase_3":
                // Dar XP bonus
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&a[Recompensa] &7+200 XP"));
                break;
                
            case "tutorial_completado":
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                    "give " + player.getName() + " minecraft:diamond 8");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                    "give " + player.getName() + " minecraft:golden_apple 5");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&a[Recompensa] &7+8 Diamantes, +5 Manzanas Doradas, +500 XP"));
                break;
        }
    }
    
    private String getAchievementTitle(String achievementId) {
        switch (achievementId) {
            case "primer_desastre_evadido":
                return "&a✓ Primera Evasión";
            case "primera_mision_completada":
                return "&a✓ Misionero Novato";
            case "treinta_minutos_supervivencia":
                return "&a✓ Sobreviviente";
            case "alcanzar_fase_3":
                return "&6✓ Veterano en Entrenamiento";
            case "tutorial_completado":
                return "&6&l✓ TUTORIAL COMPLETADO";
            default:
                return "&a✓ Logro Desbloqueado";
        }
    }
    
    private String getAchievementDescription(String achievementId) {
        switch (achievementId) {
            case "primer_desastre_evadido":
                return "Evadiste tu primer desastre";
            case "primera_mision_completada":
                return "Completaste tu primera misión";
            case "treinta_minutos_supervivencia":
                return "Sobreviviste 30 minutos";
            case "alcanzar_fase_3":
                return "Alcanzaste la Fase 3";
            case "tutorial_completado":
                return "¡Completaste el tutorial completo!";
            default:
                return "";
        }
    }
    
    public boolean hasAchievement(UUID uuid, String achievementId) {
        Set<String> achievements = unlockedAchievements.get(uuid);
        return achievements != null && achievements.contains(achievementId);
    }
    
    public void clearPlayerAchievements(UUID uuid) {
        unlockedAchievements.remove(uuid);
    }
}
