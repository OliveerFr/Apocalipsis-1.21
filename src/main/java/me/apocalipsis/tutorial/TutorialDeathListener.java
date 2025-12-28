/*
 * Apocalipsis Plugin - Listener de Muertes para Tutorial
 * Copyright (c) 2025 Apocalipsis Plugin
 * 
 * Licensed under the MIT License.
 */
package me.apocalipsis.tutorial;

import me.apocalipsis.Apocalipsis;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Maneja muertes durante el tutorial con mensajes contextuales y protección
 */
public class TutorialDeathListener implements Listener {
    
    private final Apocalipsis plugin;
    private final TutorialManager tutorialManager;
    
    public TutorialDeathListener(Apocalipsis plugin, TutorialManager tutorialManager) {
        this.plugin = plugin;
        this.tutorialManager = tutorialManager;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Solo procesar si está en tutorial
        if (!tutorialManager.isInTutorial(player)) {
            return;
        }
        
        long playedMinutes = tutorialManager.getPlayedTimeMinutes(player);
        String deathMessage = event.getDeathMessage();
        
        // Protección de items en primeros 15 minutos
        if (playedMinutes < 15) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a[Tutorial] &7Items protegidos (primeros 15 minutos)"));
        }
        
        // Registrar muerte en métricas
        int phase = plugin.getProgressiveDifficultySystem().getPlayerPhase(player).getPhaseNumber();
        tutorialManager.getMetrics().recordDeathInPhase(phase);
        
        // Enviar tip contextual según causa de muerte
        sendContextualDeathTip(player, deathMessage);
    }
    
    /**
     * Envía tips específicos según la causa de muerte
     */
    private void sendContextualDeathTip(Player player, String deathMessage) {
        if (deathMessage == null) {
            return;
        }
        
        String message = deathMessage.toLowerCase();
        
        if (message.contains("fell") || message.contains("cayó") || message.contains("caída")) {
            sendInteractiveHint(player, 
                "&e💡 TIP: &7Durante desastres, evita lugares altos y ten agua preparada.",
                "/avo menu");
            
        } else if (message.contains("fire") || message.contains("fuego") || message.contains("lava")) {
            sendInteractiveHint(player,
                "&e💡 TIP: &7Usa cubos de agua contra la Lluvia de Fuego. &aClick aquí para más info →",
                "/avo menu");
            
        } else if (message.contains("suffocation") || message.contains("sofocación") || message.contains("wall")) {
            sendInteractiveHint(player,
                "&e💡 TIP: &7Durante Terremotos, sal de espacios cerrados y evita el epicentro.",
                "/avo menu");
            
        } else if (message.contains("mob") || message.contains("zombie") || message.contains("skeleton")) {
            sendInteractiveHint(player,
                "&e💡 TIP: &7Construye un refugio seguro. Los desastres son más peligrosos que los mobs.",
                "/avo menu");
            
        } else {
            // Tip genérico
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&e💡 TIP: &7Los desastres aumentan gradualmente. Usa &f/tutorial &7para ver tu progreso."));
        }
    }
    
    /**
     * Envía un hint interactivo clickeable
     */
    private void sendInteractiveHint(Player player, String message, String command) {
        TextComponent hint = new TextComponent(ChatColor.translateAlternateColorCodes('&', message));
        hint.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        hint.setHoverEvent(new HoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', "&aClick para ejecutar")).create()
        ));
        player.spigot().sendMessage(hint);
    }
}
