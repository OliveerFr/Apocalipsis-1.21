/*
 * Apocalipsis Plugin - Minecraft Disaster Survival System
 * Copyright (c) 2025 Apocalipsis Plugin
 * 
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package me.apocalipsis;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import me.apocalipsis.commands.ApocalipsisCommand;
import me.apocalipsis.commands.AvoTabCompleter;
import me.apocalipsis.commands.RecompensaCommand;
import me.apocalipsis.disaster.DisasterController;
import me.apocalipsis.disaster.DisasterEvasionTracker;
import me.apocalipsis.disaster.DisasterRegistry;
import me.apocalipsis.disaster.adapters.PerformanceAdapter;
import me.apocalipsis.events.EcoBrasasEvent;
import me.apocalipsis.events.EcoSombrasEvent;
import me.apocalipsis.events.EventController;
import me.apocalipsis.events.NavidadEvent;
import me.apocalipsis.events.SusurroPiedraRotaEvent;
import me.apocalipsis.events.CaminoEndEvent;
import me.apocalipsis.events.AperturaEndEvent;
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
import me.apocalipsis.skills.BackpackService;
import me.apocalipsis.skills.SkillEffectListener;
import me.apocalipsis.skills.SkillService;
import me.apocalipsis.skills.SkillTreeGUI;
import me.apocalipsis.state.StateManager;
import me.apocalipsis.state.TimeService;
import me.apocalipsis.tutorial.ProgressiveDifficultySystem;
import me.apocalipsis.tutorial.TutorialListener;
import me.apocalipsis.tutorial.TutorialManager;
import me.apocalipsis.ui.MainMenuManager;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.RewardClaimSystem;
import me.apocalipsis.ui.ScoreboardManager;
import me.apocalipsis.ui.SoundUtil;
import me.apocalipsis.ui.TablistManager;
import me.apocalipsis.utils.BlockOwnershipTracker;
import me.apocalipsis.utils.ConfigManager;
import me.apocalipsis.utils.OnlinePlayersCache;
import me.apocalipsis.utils.VelocityManager;

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
    
    // Servicios de rangos permanentes
    private me.apocalipsis.missions.PermRankManager permRankManager;
    
    // Servicios de stream features
    private me.apocalipsis.missions.StreamFeaturesManager streamFeaturesManager;
    private me.apocalipsis.ui.StreamMenuGUI streamMenuGUI;

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
    private MainMenuManager mainMenuManager;
    
    // Sistema de cartas
    private me.apocalipsis.ui.CartasManager cartasManager;

    // Listeners
    private MissionListener missionListener;
    private ExperienceListener experienceListener;
    
    // Utils
    private BlockOwnershipTracker blockTracker;
    private DisasterEvasionTracker evasionTracker;
    private OnlinePlayersCache onlinePlayersCache; // [OPTIMIZACIÓN] Cache de jugadores online
    private VelocityManager velocityManager; // [FIX] Sistema anti-cheat safe para velocity
    
    // Tutorial system
    private ProgressiveDifficultySystem progressiveDifficultySystem;
    private TutorialManager tutorialManager;
    private FileConfiguration tutorialConfig;

    @Override
    public void onEnable() {
        instance = this;

        // Guardar archivos por defecto
        saveDefaultConfig();
        saveResource("desastres.yml", false);
        saveResource("eventos.yml", false);
        saveResource("misiones_new.yml", false);
        saveResource("rangos.yml", false);
        
        // AUTO-UPDATE: Verificar versión de recompensas.yml
        checkAndUpdateConfig("recompensas.yml", getDescription().getVersion());
        
        saveResource("evasiones.yml", false);
        saveResource("protecciones.yml", false);
        saveResource("skills.yml", false);
        saveResource("tutorial.yml", false);
        saveResource("stream_features.yml", false);
        saveResource("rangos_permanentes.yml", false);
        saveResource("navidad.yml", false);

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
        
        // Inicializar sistema de rangos permanentes
        permRankManager = new me.apocalipsis.missions.PermRankManager(this);
        getLogger().info("[PermRankManager] ✓ Sistema de rangos permanentes iniciado");
        
        // Inicializar sistema de stream features
        streamFeaturesManager = new me.apocalipsis.missions.StreamFeaturesManager(this);
        streamMenuGUI = new me.apocalipsis.ui.StreamMenuGUI(this, streamFeaturesManager);
        getLogger().info("[StreamFeaturesManager] ✓ Sistema de stream features iniciado");
        
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
        
        // Cargar configuración de tutorial
        File tutorialFile = new File(getDataFolder(), "tutorial.yml");
        tutorialConfig = YamlConfiguration.loadConfiguration(tutorialFile);
        
        // Inicializar sistema de dificultad progresiva para nuevos
        progressiveDifficultySystem = new ProgressiveDifficultySystem(this, tutorialConfig);
        getLogger().info("[Tutorial] ✓ Sistema de dificultad progresiva iniciado");
        
        // Inicializar tutorial manager
        tutorialManager = new TutorialManager(this, tutorialConfig, progressiveDifficultySystem, messageBus);
        getLogger().info("[Tutorial] ✓ Sistema de tutorial para nuevos jugadores iniciado");
        
        // Registrar listener de tutorial
        TutorialListener tutorialListener = new TutorialListener(this, tutorialManager);
        getServer().getPluginManager().registerEvents(tutorialListener, this);
        
        // Registrar listener de onboarding (logros épicos de primera hora)
        me.apocalipsis.tutorial.OnboardingListener onboardingListener = 
            new me.apocalipsis.tutorial.OnboardingListener(tutorialManager.getOnboardingManager());
        getServer().getPluginManager().registerEvents(onboardingListener, this);
        
        // Registrar listener de muertes en tutorial
        me.apocalipsis.tutorial.TutorialDeathListener tutorialDeathListener = 
            new me.apocalipsis.tutorial.TutorialDeathListener(this, tutorialManager);
        getServer().getPluginManager().registerEvents(tutorialDeathListener, this);
        
        getLogger().info("[Tutorial] ✓ Listeners de tutorial registrados");
        
        // Inicializar disaster system (ahora con dificultad progresiva)
        disasterRegistry = new DisasterRegistry();
        disasterController = new DisasterController(this, stateManager, timeService, disasterRegistry, messageBus, soundUtil, progressiveDifficultySystem);
        
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
        
        // Inicializar menú principal
        mainMenuManager = new MainMenuManager(this);
        
        // Sistema de cartas
        cartasManager = new me.apocalipsis.ui.CartasManager(getDataFolder(), getLogger());

        // Registrar desastres (ahora con PerformanceAdapter)
        disasterRegistry.registerDefaults(this, messageBus, soundUtil, timeService, performanceAdapter);
        
        // Registrar eventos narrativos
        EcoBrasasEvent ecoBrasasEvent = new EcoBrasasEvent(this, messageBus, soundUtil);
        eventController.registerEvent(ecoBrasasEvent);
        
        EcoSombrasEvent ecoSombrasEvent = new EcoSombrasEvent(this, messageBus, soundUtil);
        eventController.registerEvent(ecoSombrasEvent);
        
        SusurroPiedraRotaEvent susurroEvent = new SusurroPiedraRotaEvent(this, messageBus, soundUtil);
        eventController.registerEvent(susurroEvent);
        
        NavidadEvent navidadEvent = new NavidadEvent(this, messageBus, soundUtil);
        eventController.registerEvent(navidadEvent);
        
        CaminoEndEvent caminoEndEvent = new CaminoEndEvent(this, messageBus, soundUtil);
        eventController.registerEvent(caminoEndEvent);
        
        AperturaEndEvent aperturaEndEvent = new AperturaEndEvent(this, messageBus, soundUtil);
        eventController.registerEvent(aperturaEndEvent);
        
        getLogger().info("[EventController] ✓ Eventos narrativos registrados (Eco de Brasas, Eco de Sombras, Susurro Piedra Rota, Navidad, Camino al End, Apertura del End)");

        // Registrar comandos y tab completer
        ApocalipsisCommand avoCommand = new ApocalipsisCommand(this, stateManager, disasterController, eventController, missionService, timeService, messageBus);
        getCommand("avo").setExecutor(avoCommand);
        getCommand("avo").setTabCompleter(new AvoTabCompleter(this));
        getCommand("recompensa").setExecutor(new RecompensaCommand(this));
        
        // Comando de tutorial
        me.apocalipsis.tutorial.TutorialCommand tutorialCommand = 
            new me.apocalipsis.tutorial.TutorialCommand(this, tutorialManager, progressiveDifficultySystem, tutorialManager.getMetrics());
        getCommand("tutorial").setExecutor(tutorialCommand);
        getCommand("tutorial").setTabCompleter(tutorialCommand);
        getLogger().info("[Tutorial] ✓ Comando /tutorial registrado");
        
        // Comando /misionestuto para ver hitos del onboarding
        me.apocalipsis.commands.OnboardingCommand onboardingCommand = 
            new me.apocalipsis.commands.OnboardingCommand(this);
        getCommand("misionestuto").setExecutor(onboardingCommand);
        getLogger().info("[Onboarding] ✓ Comando /misionestuto registrado (aliases: /tuto, /hitostuto)");
        
        // Sistema de cartas
        cartasManager = new me.apocalipsis.ui.CartasManager(getDataFolder(), getLogger());
        
        // Comando /carta para enviar cartas a Santa
        getCommand("carta").setExecutor((sender, cmd, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                cartasManager.abrirMenuCarta(player);
            } else {
                sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            }
            return true;
        });
        
        // Comando /cartas para admins ver todas las cartas
        getCommand("cartas").setExecutor((sender, cmd, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                if (!player.hasPermission("apocalipsis.admin")) {
                    player.sendMessage("§cNo tienes permiso para ver las cartas.");
                    return true;
                }
                cartasManager.abrirMenuCartasAdmin(player);
            } else {
                sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            }
            return true;
        });
        
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
        
        // Comando de waypoint (habilidad de exploración) - Soporte para múltiples waypoints
        getCommand("waypoint").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
                return true;
            }
            
            // Sin argumentos: mostrar ayuda
            if (args.length == 0) {
                player.sendMessage("§e§l⚑ Waypoints - Uso:");
                player.sendMessage("  §f/waypoint set <nombre> §7- Guardar waypoint");
                player.sendMessage("  §f/waypoint tp <nombre> §7- Teleportarse a waypoint");
                player.sendMessage("  §f/waypoint list §7- Ver tus waypoints");
                player.sendMessage("  §f/waypoint delete <nombre> §7- Eliminar waypoint");
                
                // Mostrar límite del jugador
                int limit = this.skillEffectListener.getWaypointLimit(player);
                player.sendMessage("§7Límite actual: §e" + limit + " waypoint" + (limit > 1 ? "s" : ""));
                
                var permRank = this.permRankManager.getPlayerPermRank(player.getUniqueId());
                if (permRank != null && permRank.getId().equalsIgnoreCase("hunter_adventurer")) {
                    player.sendMessage("§a✓ §7Rango §fHunter_Adventurer§7: 10 waypoints disponibles");
                }
                return true;
            }
            
            String subCmd = args[0].toLowerCase();
            
            switch (subCmd) {
                case "set":
                    if (args.length < 2) {
                        player.sendMessage("§cUso: /waypoint set <nombre>");
                        return true;
                    }
                    String setName = args[1].toLowerCase();
                    
                    // Validar nombre
                    if (!setName.matches("[a-z0-9_-]+")) {
                        player.sendMessage("§c✖ §7El nombre solo puede contener letras, números, guiones y guiones bajos.");
                        return true;
                    }
                    
                    if (setName.length() > 16) {
                        player.sendMessage("§c✖ §7El nombre no puede tener más de 16 caracteres.");
                        return true;
                    }
                    
                    this.skillEffectListener.setWaypoint(player, setName);
                    break;
                    
                case "tp":
                case "teleport":
                    if (args.length < 2) {
                        player.sendMessage("§cUso: /waypoint tp <nombre>");
                        return true;
                    }
                    String tpName = args[1].toLowerCase();
                    this.skillEffectListener.teleportToWaypoint(player, tpName);
                    break;
                    
                case "list":
                case "lista":
                    this.skillEffectListener.listWaypoints(player);
                    break;
                    
                case "delete":
                case "del":
                case "remove":
                    if (args.length < 2) {
                        player.sendMessage("§cUso: /waypoint delete <nombre>");
                        return true;
                    }
                    String delName = args[1].toLowerCase();
                    this.skillEffectListener.deleteWaypoint(player, delName);
                    break;
                    
                default:
                    player.sendMessage("§cSubcomando desconocido. Usa §e/waypoint §cpara ver la ayuda.");
                    break;
            }
            
            return true;
        });
        getCommand("waypoint").setTabCompleter((sender, cmd, label, args) -> {
            if (args.length == 1) {
                return java.util.Arrays.asList("set", "tp", "list", "delete").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            if (args.length == 2 && (args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("delete"))) {
                // Autocompletar con los nombres de waypoints del jugador
                if (sender instanceof org.bukkit.entity.Player player) {
                    var waypoints = this.skillEffectListener.getWaypoints(player.getUniqueId());
                    if (waypoints != null) {
                        return waypoints.keySet().stream()
                            .filter(s -> s.startsWith(args[1].toLowerCase()))
                            .collect(java.util.stream.Collectors.toList());
                    }
                }
            }
            
            return java.util.Collections.emptyList();
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
        getCommand("toggle").setTabCompleter((sender, cmd, label, args) -> {
            if (args.length == 1 && sender instanceof org.bukkit.entity.Player player) {
                return java.util.Arrays.stream(me.apocalipsis.skills.Skill.values())
                    .filter(s -> s.isToggleable() && skillService.hasSkill(player, s))
                    .map(s -> s.getId())
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
            }
            return java.util.Collections.emptyList();
        });

        // Comando para invocar entidades
        getCommand("invocar").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
                return true;
            }
            
            if (args.length == 0) {
                player.sendMessage("§6§l🐺 Invocaciones Disponibles:");
                player.sendMessage("  §7- §elobo §8- Lobo Compañero");
                player.sendMessage("  §7- §egato §8- Gato Guardián");
                player.sendMessage("  §7- §eallay §8- Allay Recolector");
                player.sendMessage("  §7- §eabejas §8- Abejas Protectoras");
                player.sendMessage("  §7- §egolem §8- Gólem Protector");
                player.sendMessage("  §7- §evex §8- Vex Vengador");
                player.sendMessage("  §7- §ewarden §8- Warden Temporal");
                player.sendMessage("  §7- §cdespawn §8- Despedir invocaciones");
                player.sendMessage("§7Uso: §e/invocar <entidad>");
                return true;
            }
            
            String entidad = args[0].toLowerCase();
            switch (entidad) {
                case "lobo", "lobos", "wolf" -> skillService.invocarLobo(player);
                case "gato", "cat", "guardian" -> skillService.invocarGato(player);
                case "allay", "recolector" -> skillService.invocarAllay(player);
                case "abejas", "bees", "abeja" -> skillService.invocarAbejas(player);
                case "golem", "iron", "hierro" -> skillService.invocarGolem(player);
                case "vex", "vengador" -> {
                    // Obtener entidad objetivo con ray trace
                    org.bukkit.util.RayTraceResult result = player.getWorld().rayTraceEntities(
                        player.getEyeLocation(),
                        player.getLocation().getDirection(),
                        30,
                        entity -> entity instanceof org.bukkit.entity.LivingEntity && entity != player
                    );
                    org.bukkit.entity.LivingEntity target = null;
                    if (result != null && result.getHitEntity() instanceof org.bukkit.entity.LivingEntity living) {
                        target = living;
                    }
                    skillService.invocarVex(player, target);
                }
                case "warden", "guardian_oscuro" -> skillService.invocarWarden(player);
                case "despawn", "dismiss", "despedir" -> {
                    skillService.despawnEntidades(player.getUniqueId());
                    player.sendMessage("§7Tus invocaciones han sido despedidas.");
                }
                default -> player.sendMessage("§cEntidad desconocida: §e" + entidad + "§c. Usa §e/invocar §cpara ver las opciones.");
            }
            return true;
        });
        getCommand("invocar").setTabCompleter((sender, cmd, label, args) -> {
            if (args.length == 1) {
                java.util.List<String> opciones = java.util.Arrays.asList(
                    "lobo", "gato", "allay", "abejas", "golem", "vex", "warden", "despawn"
                );
                return opciones.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
            }
            return java.util.Collections.emptyList();
        });

        // Comando /menu - Atajo para /avo menu
        getCommand("menu").setExecutor(new me.apocalipsis.commands.MenuCommand(this));

        // Registrar listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this, scoreboardManager, tablistManager), this);
        missionListener = new MissionListener(missionService);
        getServer().getPluginManager().registerEvents(missionListener, this);
        experienceListener = new ExperienceListener(this);
        experienceListener.initXPManager(); // Inicializar sistema dinámico de XP
        getServer().getPluginManager().registerEvents(experienceListener, this);
        getServer().getPluginManager().registerEvents(new me.apocalipsis.utils.ExplosionGuard(this), this);
        getServer().getPluginManager().registerEvents(new BlockTrackListener(this), this);
        getServer().getPluginManager().registerEvents(new DisasterEvasionListener(this), this);
        getServer().getPluginManager().registerEvents(new me.apocalipsis.events.SusurroPiedraRotaListener(this), this);
        
        // Registrar listener de stream drops
        getServer().getPluginManager().registerEvents(new me.apocalipsis.listeners.StreamDropListener(this, streamFeaturesManager), this);
        getLogger().info("[StreamDropListener] ✓ Listener de drops de stream registrado");
        
        // Registrar protección de tokens (bloquea uso en crafting, anvil, etc)
        getServer().getPluginManager().registerEvents(new me.apocalipsis.listeners.TokenProtectionListener(this), this);
        getLogger().info("[TokenProtection] ✓ Protección de tokens activada");
        
        // Registrar mejoras de creepers (explosiones mucho más poderosas)
        getServer().getPluginManager().registerEvents(new me.apocalipsis.listeners.CreeperEnhancer(this), this);
        getLogger().info("[CreeperEnhancer] ✓ Creepers mejorados activados - ¡PELIGRO EXTREMO!");
        
        // Registrar listener de cartas
        getServer().getPluginManager().registerEvents(new me.apocalipsis.listeners.CartasListener(this, cartasManager), this);
        getLogger().info("[Cartas] ✓ Sistema de cartas a Santa activado");

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
        
        // Guardar datos del skill effect listener (waypoints, stats)
        if (skillEffectListener != null) {
            skillEffectListener.shutdown();
        }
        
        // Detener stream features
        if (streamFeaturesManager != null) {
            streamFeaturesManager.shutdown();
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
    
    public me.apocalipsis.missions.PermRankManager getPermRankManager() {
        return permRankManager;
    }
    
    public me.apocalipsis.missions.StreamFeaturesManager getStreamFeaturesManager() {
        return streamFeaturesManager;
    }
    
    public me.apocalipsis.ui.StreamMenuGUI getStreamMenuGUI() {
        return streamMenuGUI;
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
    
    public ExperienceListener getExperienceListener() {
        return experienceListener;
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
    
    public MainMenuManager getMainMenuManager() {
        return mainMenuManager;
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
    
    public SkillEffectListener getSkillEffectListener() {
        return skillEffectListener;
    }
    
    /**
     * [OPTIMIZACIÓN] Obtiene el cache de jugadores online
     * Usar en lugar de Bukkit.getOnlinePlayers() para mejor rendimiento
     */
    public OnlinePlayersCache getOnlinePlayersCache() {
        return onlinePlayersCache;
    }
    
    /**
     * Obtiene el sistema de dificultad progresiva para nuevos jugadores
     */
    public ProgressiveDifficultySystem getProgressiveDifficultySystem() {
        return progressiveDifficultySystem;
    }
    
    /**
     * Obtiene el gestor de tutorial para nuevos jugadores
     */
    public TutorialManager getTutorialManager() {
        return tutorialManager;
    }
    
    /**
     * AUTO-UPDATE: Verifica la versión de un archivo de configuración y lo actualiza si es necesario
     * @param fileName Nombre del archivo (ej: "recompensas.yml")
     * @param requiredVersion Versión mínima requerida (del plugin)
     */
    private void checkAndUpdateConfig(String fileName, String requiredVersion) {
        File configFile = new File(getDataFolder(), fileName);
        
        // Si el archivo no existe, crearlo
        if (!configFile.exists()) {
            saveResource(fileName, false);
            getLogger().info("[AUTO-UPDATE] Creado " + fileName + " v" + requiredVersion);
            return;
        }
        
        // Leer versión actual
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String currentVersion = config.getString("version", "0");
        
        // Verificar si necesita actualización (comparar versiones)
        if (!currentVersion.equals(requiredVersion)) {
            getLogger().warning("[AUTO-UPDATE] " + fileName + " está desactualizado (v" + currentVersion + " → v" + requiredVersion + ")");
            
            // Hacer backup del archivo viejo
            File backup = new File(getDataFolder(), fileName + ".backup-v" + currentVersion);
            try {
                java.nio.file.Files.copy(configFile.toPath(), backup.toPath(), 
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                getLogger().info("[AUTO-UPDATE] Backup guardado: " + backup.getName());
            } catch (Exception e) {
                getLogger().warning("[AUTO-UPDATE] No se pudo crear backup: " + e.getMessage());
            }
            
            // Eliminar el archivo viejo
            configFile.delete();
            
            // Copiar el nuevo del JAR
            saveResource(fileName, true);
            getLogger().info("[AUTO-UPDATE] ✓ " + fileName + " actualizado a v" + requiredVersion);
        } else {
            getLogger().info("[AUTO-UPDATE] " + fileName + " está actualizado (v" + currentVersion + ")");
        }
    }
}
