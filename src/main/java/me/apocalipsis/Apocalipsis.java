/*
 * Apocalipsis Plugin - Minecraft Disaster Survival System
 * Copyright (c) 2025 Apocalipsis Plugin
 * 
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package me.apocalipsis;

import org.bukkit.plugin.java.JavaPlugin;

import me.apocalipsis.commands.ApocalipsisCommand;
import me.apocalipsis.commands.AvoTabCompleter;
import me.apocalipsis.disaster.DisasterController;
import me.apocalipsis.disaster.DisasterEvasionTracker;
import me.apocalipsis.disaster.DisasterRegistry;
import me.apocalipsis.disaster.adapters.PerformanceAdapter;
import me.apocalipsis.events.EcoBrasasEvent;
import me.apocalipsis.events.EcoSombrasEvent;
import me.apocalipsis.events.EventController;
import me.apocalipsis.events.SusurroPiedraRotaEvent;
import me.apocalipsis.events.testing.EventAutoTestingSystem;
import me.apocalipsis.experience.AbilityService;
import me.apocalipsis.experience.ExperienceService;
import me.apocalipsis.experience.RewardService;
import me.apocalipsis.listeners.BlockTrackListener;
import me.apocalipsis.listeners.DisasterEvasionListener;
import me.apocalipsis.listeners.ExperienceListener;
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
import me.apocalipsis.utils.OnlinePlayersCache;
import me.apocalipsis.utils.VelocityManager;
import me.apocalipsis.ui.RewardClaimSystem;
import me.apocalipsis.commands.RecompensaCommand;
import me.apocalipsis.skills.SkillService;
import me.apocalipsis.skills.SkillTreeGUI;
import me.apocalipsis.skills.SkillEffectListener;
import me.apocalipsis.skills.BackpackService;

public final class Apocalipsis extends JavaPlugin {

    private static Apocalipsis instance;

    // Servicios centrales
    private ConfigManager configManager;
    private StateManager stateManager;
    private TimeService timeService;
    private DisasterRegistry disasterRegistry;
    private DisasterController disasterController;
    private EventController eventController;
    private EventAutoTestingSystem autoTestSystem;
    private MissionService missionService;
    private RankService rankService;
    private PerformanceAdapter performanceAdapter;

    // Servicios de experiencia y progresión
    private ExperienceService experienceService;
    private AbilityService abilityService;
    private RewardService rewardService;
    
    // Servicios de árbol de habilidades
    private SkillService skillService;
    private SkillTreeGUI skillTreeGUI;
    private BackpackService backpackService;
    private SkillEffectListener skillEffectListener;

    // UI
    private MessageBus messageBus;
    private SoundUtil soundUtil;
    private ScoreboardManager scoreboardManager;
    private TablistManager tablistManager;
    private RewardClaimSystem rewardClaimSystem;

    // Listeners
    private MissionListener missionListener;
    private ExperienceListener experienceListener;
    
    // Utils
    private BlockOwnershipTracker blockTracker;
    private DisasterEvasionTracker evasionTracker;
    private OnlinePlayersCache onlinePlayersCache; // [OPTIMIZACIÓN] Cache de jugadores online
    private VelocityManager velocityManager; // [FIX] Sistema anti-cheat safe para velocity

    @Override
    public void onEnable() {
        instance = this;

        // Guardar archivos por defecto
        saveDefaultConfig();
        saveResource("desastres.yml", false);
        saveResource("eventos.yml", false);
        saveResource("misiones_new.yml", false);
        saveResource("rangos.yml", false);
        saveResource("recompensas.yml", false);
        saveResource("chat.yml", false);
        saveResource("evasiones.yml", false);

        // Inicializar servicios
        configManager = new ConfigManager(this);
        messageBus = new MessageBus(this);
        soundUtil = new SoundUtil(this);
        
        // [OPTIMIZACIÓN] Cache de jugadores online
        onlinePlayersCache = new OnlinePlayersCache();
        getServer().getPluginManager().registerEvents(onlinePlayersCache, this);
        timeService = new TimeService(this);
        stateManager = new StateManager(this, timeService, messageBus);
        
        // Inicializar PerformanceAdapter
        performanceAdapter = new PerformanceAdapter(this);
        
        // Inicializar servicios de misiones y rangos
        missionService = new MissionService(this, messageBus);
        rankService = new RankService(this, missionService);
        
        // Inicializar servicios de experiencia y progresión
        experienceService = new ExperienceService(this);
        abilityService = new AbilityService(this);
        rewardService = new RewardService(this);
        
        // Inicializar árbol de habilidades
        skillService = new SkillService(this);
        skillTreeGUI = new SkillTreeGUI(this, skillService);
        backpackService = new BackpackService(this, skillService);
        skillEffectListener = new SkillEffectListener(this, skillService);
        getServer().getPluginManager().registerEvents(skillTreeGUI, this);
        getServer().getPluginManager().registerEvents(skillEffectListener, this);
        getServer().getPluginManager().registerEvents(backpackService, this);
        getLogger().info("[SkillService] ✓ Sistema de árbol de habilidades iniciado");
        
        // Iniciar tarea de habilidades pasivas
        abilityService.startTask();
        getLogger().info("[AbilityService] ✓ Sistema de habilidades iniciado");
        
        // Inicializar block tracker (anti-griefing)
        blockTracker = new BlockOwnershipTracker(this);
        
        // Inicializar evasion tracker (anti-disconnect)
        evasionTracker = new DisasterEvasionTracker(this);
        
        // [FIX] Inicializar velocity manager (anti-cheat safe)
        velocityManager = new VelocityManager(this);
        getLogger().info("[VelocityManager] ✓ Sistema de velocity smoothing iniciado");
        
        // Inicializar disaster system
        disasterRegistry = new DisasterRegistry();
        disasterController = new DisasterController(this, stateManager, timeService, disasterRegistry, messageBus, soundUtil);
        
        // Inicializar event system
        eventController = new EventController(this);
        
        // Inicializar sistema de autotesting
        autoTestSystem = new EventAutoTestingSystem(this);
        getLogger().info("[AutoTest] ✓ Sistema de autotesting inicializado");
        
        // Inicializar UI
        scoreboardManager = new ScoreboardManager(this, stateManager, disasterController, missionService, rankService);
        tablistManager = new TablistManager(this, stateManager, performanceAdapter, rankService);
        
        // Inicializar sistema de recompensas reclamables
        rewardClaimSystem = new RewardClaimSystem(this);

        // Registrar desastres (ahora con PerformanceAdapter)
        disasterRegistry.registerDefaults(this, messageBus, soundUtil, timeService, performanceAdapter);
        
        // Registrar eventos narrativos
        EcoBrasasEvent ecoBrasasEvent = new EcoBrasasEvent(this, messageBus, soundUtil);
        eventController.registerEvent(ecoBrasasEvent);
        
        EcoSombrasEvent ecoSombrasEvent = new EcoSombrasEvent(this, messageBus, soundUtil);
        eventController.registerEvent(ecoSombrasEvent);
        
        SusurroPiedraRotaEvent susurroEvent = new SusurroPiedraRotaEvent(this, messageBus, soundUtil);
        eventController.registerEvent(susurroEvent);
        
        getLogger().info("[EventController] ✓ Eventos narrativos registrados (Eco de Brasas, Eco de Sombras, Susurro Piedra Rota)");

        // Registrar comandos y tab completer
        ApocalipsisCommand avoCommand = new ApocalipsisCommand(this, stateManager, disasterController, eventController, missionService, timeService, messageBus);
        getCommand("avo").setExecutor(avoCommand);
        getCommand("avo").setTabCompleter(new AvoTabCompleter(this));
        getCommand("recompensa").setExecutor(new RecompensaCommand(this));
        
        // Comandos directos para jugadores
        getCommand("habilidades").setExecutor((sender, cmd, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                skillTreeGUI.openMainMenu(player);
            } else {
                sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            }
            return true;
        });
        
        getCommand("mochila").setExecutor((sender, cmd, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                backpackService.openBackpack(player);
            } else {
                sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            }
            return true;
        });
        
        getCommand("echest").setExecutor((sender, cmd, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                backpackService.openPortableEnderChest(player);
            } else {
                sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            }
            return true;
        });
        
        // Comando de waypoint (habilidad de exploración)
        getCommand("waypoint").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
                return true;
            }
            if (args.length > 0 && args[0].equalsIgnoreCase("set")) {
                this.skillEffectListener.setWaypoint(player);
            } else {
                this.skillEffectListener.teleportToWaypoint(player);
            }
            return true;
        });
        
        // Comando de coordenadas
        getCommand("coords").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
                return true;
            }
            org.bukkit.Location loc = player.getLocation();
            player.sendMessage("§e⬤ Coordenadas: §fX: §a" + loc.getBlockX() + 
                " §f| Y: §a" + loc.getBlockY() + " §f| Z: §a" + loc.getBlockZ());
            return true;
        });
        
        // Comando para toggle de habilidades
        getCommand("toggle").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
                return true;
            }
            
            if (args.length == 0) {
                // Mostrar lista de habilidades toggleables
                player.sendMessage("§6§l⚡ Habilidades Activables:");
                for (me.apocalipsis.skills.Skill skill : me.apocalipsis.skills.Skill.getToggleable()) {
                    if (skillService.hasSkill(player, skill)) {
                        boolean enabled = skillService.isSkillEnabled(player, skill);
                        String status = enabled ? "§a✓ ACTIVA" : "§c✗ INACTIVA";
                        player.sendMessage("  §7- §e" + skill.getDisplayName() + " §8[" + status + "§8]");
                    }
                }
                player.sendMessage("§7Uso: §e/toggle <nombre_habilidad>");
                return true;
            }
            
            String skillName = String.join("_", args).toLowerCase();
            me.apocalipsis.skills.Skill skill = me.apocalipsis.skills.Skill.fromId(skillName);
            
            if (skill == null) {
                // Buscar por nombre parcial
                for (me.apocalipsis.skills.Skill s : me.apocalipsis.skills.Skill.values()) {
                    if (s.getDisplayName().toLowerCase().contains(args[0].toLowerCase()) ||
                        s.getId().contains(args[0].toLowerCase())) {
                        skill = s;
                        break;
                    }
                }
            }
            
            if (skill == null) {
                player.sendMessage("§cHabilidad no encontrada: §e" + args[0]);
                return true;
            }
            
            if (!skill.isToggleable()) {
                player.sendMessage("§cEsta habilidad no se puede activar/desactivar.");
                return true;
            }
            
            if (!skillService.hasSkill(player, skill)) {
                player.sendMessage("§cNo tienes desbloqueada esta habilidad.");
                return true;
            }
            
            skillService.toggleSkill(player, skill);
            boolean nowEnabled = skillService.isSkillEnabled(player, skill);
            String newStatus = nowEnabled ? "§a§lACTIVADA" : "§c§lDESACTIVADA";
            player.sendMessage("§e" + skill.getDisplayName() + " §7ahora está " + newStatus);
            return true;
        });

        // Registrar listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this, scoreboardManager, tablistManager), this);
        missionListener = new MissionListener(missionService);
        getServer().getPluginManager().registerEvents(missionListener, this);
        experienceListener = new ExperienceListener(this);
        getServer().getPluginManager().registerEvents(experienceListener, this);
        getServer().getPluginManager().registerEvents(new me.apocalipsis.utils.ExplosionGuard(this), this);
        getServer().getPluginManager().registerEvents(new BlockTrackListener(this), this);
        getServer().getPluginManager().registerEvents(new DisasterEvasionListener(this), this);
        getServer().getPluginManager().registerEvents(new me.apocalipsis.listeners.ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new me.apocalipsis.events.SusurroPiedraRotaListener(this), this);

        // Cargar estado
        stateManager.loadState();

        // Iniciar tasks
        performanceAdapter.startMonitoring(); // Iniciar monitoreo de TPS
        disasterController.startTask();
        scoreboardManager.startTask();
        tablistManager.startTask();
        
        // Iniciar tick loop de eventos
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (eventController != null) {
                eventController.tick();
            }
        }, 0L, 1L); // Tick cada 1 tick (50ms)
        
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
        
        // Guardar datos de experiencia
        if (experienceService != null) {
            experienceService.saveData();
        }
        
        // Guardar datos de recompensas
        if (rewardService != null) {
            rewardService.saveData();
        }
        
        // Detener habilidades
        if (abilityService != null) {
            abilityService.stopTask();
        }
        
        // Guardar árbol de habilidades
        if (skillService != null) {
            skillService.shutdown();
        }
        
        // Detener mission height tracker
        if (missionService != null) {
            missionService.stopHeightTracker();
        }
        
        // Guardar block tracker
        if (blockTracker != null) {
            blockTracker.stopCleanupTask();
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
        
        // Detener eventos
        if (eventController != null) {
            eventController.stopActiveEvent();
        }
        
        // [FIX DUPLICACIÓN] Limpiar registro de desastres
        if (disasterRegistry != null) {
            disasterRegistry.clearAll();
        }
        
        // Detener PerformanceAdapter
        if (performanceAdapter != null) {
            performanceAdapter.stopMonitoring();
        }
        
        // [FIX] Detener velocity manager
        if (velocityManager != null) {
            velocityManager.shutdown();
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
        
        // Detener sistema de recompensas
        if (rewardClaimSystem != null) {
            rewardClaimSystem.shutdown();
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

    public EventController getEventController() {
        return eventController;
    }
    
    public EventAutoTestingSystem getAutoTestSystem() {
        return autoTestSystem;
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
    
    public ExperienceService getExperienceService() {
        return experienceService;
    }
    
    public AbilityService getAbilityService() {
        return abilityService;
    }
    
    public RewardService getRewardService() {
        return rewardService;
    }
    
    public VelocityManager getVelocityManager() {
        return velocityManager;
    }
    
    public RewardClaimSystem getRewardClaimSystem() {
        return rewardClaimSystem;
    }
    
    public SkillService getSkillService() {
        return skillService;
    }
    
    public SkillTreeGUI getSkillTreeGUI() {
        return skillTreeGUI;
    }
    
    public BackpackService getBackpackService() {
        return backpackService;
    }
    
    /**
     * [OPTIMIZACIÓN] Obtiene el cache de jugadores online
     * Usar en lugar de Bukkit.getOnlinePlayers() para mejor rendimiento
     */
    public OnlinePlayersCache getOnlinePlayersCache() {
        return onlinePlayersCache;
    }
}
