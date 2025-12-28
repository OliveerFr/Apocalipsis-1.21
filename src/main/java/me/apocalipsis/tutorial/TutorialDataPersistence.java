/*
 * Apocalipsis Plugin - Sistema de Persistencia para Tutorial
 * Copyright (c) 2025 Apocalipsis Plugin
 * 
 * Licensed under the MIT License.
 */
package me.apocalipsis.tutorial;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.apocalipsis.Apocalipsis;

/**
 * Gestiona la persistencia de datos del tutorial en archivos YAML
 */
public class TutorialDataPersistence {
    
    private final Apocalipsis plugin;
    private final File dataFolder;
    
    public TutorialDataPersistence(Apocalipsis plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "tutorial_data");
        
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }
    
    /**
     * Guarda el estado del tutorial de un jugador
     */
    public void saveTutorialState(UUID uuid, TutorialManager.TutorialState state, long firstJoinTime) {
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        
        config.set("uuid", uuid.toString());
        config.set("first_join_time", firstJoinTime);
        config.set("welcomed", state.isWelcomed());
        config.set("tutorial_started", state.isTutorialStarted());
        config.set("kit_given", state.isKitGiven());
        config.set("current_stage", state.getCurrentStage());
        config.set("rank_demo_shown", state.isRankDemoShown());
        config.set("completed", state.isCompleted());
        config.set("start_time", state.getStartTime());
        config.set("last_phase_number", state.getLastPhaseNumber());
        config.set("last_save", System.currentTimeMillis());
        
        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[Tutorial] Error guardando datos de " + uuid + ": " + e.getMessage());
        }
    }
    
    /**
     * Carga el estado del tutorial de un jugador
     */
    public TutorialManager.TutorialState loadTutorialState(UUID uuid) {
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        
        if (!playerFile.exists()) {
            return null;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        
        TutorialManager.TutorialState state = new TutorialManager.TutorialState();
        state.setWelcomed(config.getBoolean("welcomed", false));
        state.setTutorialStarted(config.getBoolean("tutorial_started", false));
        state.setKitGiven(config.getBoolean("kit_given", false));
        state.setCurrentStage(config.getInt("current_stage", 0));
        state.setRankDemoShown(config.getBoolean("rank_demo_shown", false));
        state.setCompleted(config.getBoolean("completed", false));
        state.setLastPhaseNumber(config.getInt("last_phase_number", 1));
        
        return state;
    }
    
    /**
     * Carga el timestamp de primer join
     */
    public Long loadFirstJoinTime(UUID uuid) {
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        
        if (!playerFile.exists()) {
            return null;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        long time = config.getLong("first_join_time", 0);
        
        return time > 0 ? time : null;
    }
    
    /**
     * Verifica si existen datos guardados para un jugador
     */
    public boolean hasData(UUID uuid) {
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        return playerFile.exists();
    }
    
    /**
     * Elimina los datos de un jugador (para reset)
     */
    public void deletePlayerData(UUID uuid) {
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        if (playerFile.exists()) {
            playerFile.delete();
        }
    }
    
    /**
     * Guarda datos periódicamente (llamar cada 5 minutos)
     */
    public void saveAllData(TutorialManager tutorialManager, ProgressiveDifficultySystem difficultySystem) {
        // Este método se llamará desde TutorialManager para guardar todos los datos en memoria
    }
}
