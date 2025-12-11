/*
 * Apocalipsis Plugin - Tutorial Listener
 * Copyright (c) 2025 Apocalipsis Plugin
 * 
 * Licensed under the MIT License.
 */
package me.apocalipsis.tutorial;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.apocalipsis.Apocalipsis;

/**
 * Listener para eventos relacionados con el sistema de tutorial
 */
public class TutorialListener implements Listener {
    
    private final Apocalipsis plugin;
    private final TutorialManager tutorialManager;
    
    public TutorialListener(Apocalipsis plugin, TutorialManager tutorialManager) {
        this.plugin = plugin;
        this.tutorialManager = tutorialManager;
    }
    
    /**
     * Maneja el primer join de un jugador
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Solo iniciar tutorial si es la primera vez que entra
        if (!player.hasPlayedBefore()) {
            tutorialManager.handleFirstJoin(player);
        }
    }
    
    /**
     * Limpia datos temporales al salir
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        tutorialManager.handlePlayerQuit(event.getPlayer().getUniqueId());
    }
}
