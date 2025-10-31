/*
 * Apocalipsis Plugin - Minecraft Disaster Survival System
 * Copyright (c) 2025 Apocalipsis Plugin
 * 
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package me.apocalipsis;

import me.apocalipsis.commands.ApocalipsisCommand;
import me.apocalipsis.commands.AvoTabCompleter;
import me.apocalipsis.disaster.DisasterController;
import me.apocalipsis.disaster.DisasterEvasionTracker;
import me.apocalipsis.disaster.DisasterRegistry;
import me.apocalipsis.disaster.adapters.PerformanceAdapter;
import me.apocalipsis.listeners.BlockTrackListener;
import me.apocalipsis.listeners.DisasterEvasionListener;
import me.apocalipsis.listeners.MissionListener;
import me.apocalipsis.listeners.PlayerListener;
import me.apocalipsis.missions.MissionService;
import me.apocalipsis.missions.RankService;
import me.apocalipsis.state.StateManager;
import me.apocalipsis.state.TimeService;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.ScoreboardManager;
import me.apocalipsis.ui.SoundUtil;
import me.apocalipsis.ui.TablistManager;
import me.apocalipsis.utils.BlockOwnershipTracker;
import me.apocalipsis.utils.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Apocalipsis extends JavaPlugin {

    private static Apocalipsis instance;

    // Servicios centrales
    private ConfigManager configManager;
    private StateManager stateManager;
    private TimeService timeService;
    private DisasterRegistry disasterRegistry;
    private DisasterController disasterController;
    private MissionService missionService;
    private RankService rankService;
    private PerformanceAdapter performanceAdapter;

    // UI
    private MessageBus messageBus;
    private SoundUtil soundUtil;
    private ScoreboardManager scoreboardManager;
    private TablistManager tablistManager;

    // Listeners
    private MissionListener missionListener;
    
    // Utils
    private BlockOwnershipTracker blockTracker;
    private DisasterEvasionTracker evasionTracker;

    @Override
    public void onEnable() {
        instance = this;

        // Guardar archivos por defecto
        saveDefaultConfig();
        saveResource("desastres.yml", false);
        saveResource("misiones_new.yml", false);
        saveResource("rangos.yml", false);
        saveResource("alonsolevels.yml", false);

        // Inicializar servicios
        configManager = new ConfigManager(this);
        messageBus = new MessageBus(this);
        soundUtil = new SoundUtil(this);
        timeService = new TimeService(this);
        stateManager = new StateManager(this, timeService, messageBus);
        
        // Inicializar PerformanceAdapter
        performanceAdapter = new PerformanceAdapter(this);
        
        // Inicializar servicios de misiones y rangos
        missionService = new MissionService(this, messageBus);
        rankService = new RankService(this, missionService);
        
        // Inicializar block tracker (anti-griefing)
        blockTracker = new BlockOwnershipTracker(this);
        
        // Inicializar evasion tracker (anti-disconnect)
        evasionTracker = new DisasterEvasionTracker(this);
        
        // Inicializar disaster system
        disasterRegistry = new DisasterRegistry();
        disasterController = new DisasterController(this, stateManager, timeService, disasterRegistry, messageBus, soundUtil);
        
        // Inicializar UI
        scoreboardManager = new ScoreboardManager(this, stateManager, disasterController, missionService, rankService);
        tablistManager = new TablistManager(this, stateManager, performanceAdapter, rankService);

        // Registrar desastres (ahora con PerformanceAdapter)
        disasterRegistry.registerDefaults(this, messageBus, soundUtil, timeService, performanceAdapter);

        // Registrar comandos y tab completer
        getCommand("avo").setExecutor(new ApocalipsisCommand(this, stateManager, disasterController, missionService, timeService, messageBus));
        getCommand("avo").setTabCompleter(new AvoTabCompleter(this));

        // Registrar listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this, scoreboardManager, tablistManager), this);
        missionListener = new MissionListener(missionService);
        getServer().getPluginManager().registerEvents(missionListener, this);
        getServer().getPluginManager().registerEvents(new me.apocalipsis.utils.ExplosionGuard(this), this);
        getServer().getPluginManager().registerEvents(new BlockTrackListener(this), this);
        getServer().getPluginManager().registerEvents(new DisasterEvasionListener(this), this);

        // Cargar estado
        stateManager.loadState();

        // Iniciar tasks
        performanceAdapter.startMonitoring(); // Iniciar monitoreo de TPS
        disasterController.startTask();
        scoreboardManager.startTask();
        tablistManager.startTask();
        
        // [DATA.YML] Scheduler para tiempo jugado (cada 60 segundos)
        // TODO: Implementar sistema de data.yml completo
        // getServer().getScheduler().runTaskTimer(this, () -> {
        //     configManager.tickPlayedTime();
        // }, 1200L, 1200L);  // 60 segundos = 1200 ticks

        getLogger().info("§a✓ Apocalipsis activado correctamente");
    }

    @Override
    public void onDisable() {
        // Guardar estado
        if (stateManager != null) {
            stateManager.saveState();
        }
        
        // Guardar block tracker
        if (blockTracker != null) {
            blockTracker.saveData();
        }
        
        // Limpiar evasion tracker y guardar datos
        if (evasionTracker != null) {
            evasionTracker.saveData();
            evasionTracker.clearAll();
        }
        
        // [DATA.YML] Guardar datos de jugadores
        // TODO: Implementar sistema de data.yml completo
        // if (configManager != null) {
        //     configManager.saveDataYamlNow();
        // }

        // Detener desastres
        if (disasterController != null) {
            disasterController.stopAllDisasters(false);
            disasterController.cancelTask();
        }
        
        // Detener PerformanceAdapter
        if (performanceAdapter != null) {
            performanceAdapter.stopMonitoring();
        }

        // Limpiar UI
        if (scoreboardManager != null) {
            scoreboardManager.cancelTask();
            scoreboardManager.clearAll();
        }

        if (tablistManager != null) {
            tablistManager.cancelTask();
            tablistManager.clearAll();
        }

        getLogger().info("§c✗ Apocalipsis desactivado");
    }

    public static Apocalipsis getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StateManager getStateManager() {
        return stateManager;
    }

    public TimeService getTimeService() {
        return timeService;
    }

    public DisasterRegistry getDisasterRegistry() {
        return disasterRegistry;
    }

    public DisasterController getDisasterController() {
        return disasterController;
    }

    public MissionService getMissionService() {
        return missionService;
    }

    public MessageBus getMessageBus() {
        return messageBus;
    }

    public SoundUtil getSoundUtil() {
        return soundUtil;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public TablistManager getTablistManager() {
        return tablistManager;
    }

    public RankService getRankService() {
        return rankService;
    }

    public PerformanceAdapter getPerformanceAdapter() {
        return performanceAdapter;
    }

    public MissionListener getMissionListener() {
        return missionListener;
    }
    
    public BlockOwnershipTracker getBlockTracker() {
        return blockTracker;
    }
    
    public DisasterEvasionTracker getDisasterEvasionTracker() {
        return evasionTracker;
    }
}
