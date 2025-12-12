/*
 * Apocalipsis Plugin - Tutorial Listener
 * Copyright (c) 2025 Apocalipsis Plugin
 * 
 * Licensed under the MIT License.
 */
package me.apocalipsis.tutorial;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.missions.MissionRank;

/**
 * Listener para eventos relacionados con el sistema de tutorial
 */
public class TutorialListener implements Listener {
    
    private final Apocalipsis plugin;
    private final TutorialManager tutorialManager;
    private final FileConfiguration config;
    
    public TutorialListener(Apocalipsis plugin, TutorialManager tutorialManager) {
        this.plugin = plugin;
        this.tutorialManager = tutorialManager;
        this.config = plugin.getConfigManager().getTutorialConfig();
    }
    
    /**
     * Maneja el primer join de un jugador
     * CRITERIOS DE DETECCIÓN MEJORADOS:
     * 1. Rango NOVATO (no ha progresado de nivel)
     * 2. Tiempo jugado menor al límite configurado (por defecto 60 min)
     * 3. No tiene datos previos de tutorial O cumple criterios 1 y 2
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Leer configuración
        int maxTiempoMinutos = config.getInt("tutorial.max_tiempo_jugado_minutos", 60);
        boolean soloNovato = config.getBoolean("tutorial.solo_rango_novato", true);
        
        // Obtener datos del jugador
        MissionRank rank = plugin.getRankService().getRank(player);
        long playedMinutes = plugin.getProgressiveDifficultySystem().getPlayedTimeMinutes(player);
        boolean hasPlayerData = plugin.getProgressiveDifficultySystem().hasPlayerData(player);
        
        // VERIFICACIÓN MEJORADA: Debe cumplir TODAS estas condiciones
        boolean isNovato = (rank == MissionRank.NOVATO);
        boolean isNewTime = (playedMinutes < maxTiempoMinutos);
        
        // Decidir si necesita tutorial
        boolean needsTutorial;
        if (!hasPlayerData) {
            // Nunca ha tenido tutorial, verificar requisitos
            needsTutorial = (!soloNovato || isNovato) && isNewTime;
        } else {
            // Ya tiene datos pero verificamos si aún califica (por si reseteo manual)
            needsTutorial = false;
        }
        
        plugin.getLogger().info(String.format(
            "[Tutorial] Jugador %s | Rango=%s | Tiempo=%dmin/%dmin | Datos=%s | Califica=%s",
            player.getName(), rank.name(), playedMinutes, maxTiempoMinutos, hasPlayerData, needsTutorial
        ));
        
        // Iniciar tutorial SOLO si cumple todos los criterios
        if (needsTutorial) {
            plugin.getLogger().info(String.format(
                "[Tutorial] ✓ Iniciando tutorial para %s (NOVATO con %d minutos de juego)",
                player.getName(), playedMinutes
            ));
            tutorialManager.handleFirstJoin(player);
        } else {
            String motivo;
            if (hasPlayerData) {
                motivo = "Ya tiene datos de tutorial";
            } else if (!isNovato && soloNovato) {
                motivo = "Rango " + rank.name() + " (requiere NOVATO)";
            } else if (!isNewTime) {
                motivo = "Tiempo jugado " + playedMinutes + "min (límite: " + maxTiempoMinutos + "min)";
            } else {
                motivo = "Condiciones no cumplidas";
            }
            
            plugin.getLogger().info(String.format(
                "[Tutorial] ✗ Saltando tutorial para %s. Motivo: %s",
                player.getName(), motivo
            ));
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
