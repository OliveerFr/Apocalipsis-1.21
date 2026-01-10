/*
 * Apocalipsis Plugin - Sistema de Tutorial para Nuevos Jugadores
 * Copyright (c) 2025 Apocalipsis Plugin
 * 
 * Licensed under the MIT License.
 */
package me.apocalipsis.tutorial;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.tutorial.ProgressiveDifficultySystem.DifficultyPhase;
import me.apocalipsis.ui.MessageBus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

/**
 * Gestor del sistema de tutorial para nuevos jugadores.
 * 
 * Funciones principales:
 * - Espera 5 minutos antes de iniciar el tutorial
 * - Entrega kit de inicio automáticamente
 * - Muestra demo de rangos (efectos temporales)
 * - Gestiona tips progresivos basados en tiempo jugado
 * - Notifica cambios de fase de dificultad
 * - Trackea progreso del tutorial por etapas
 */
public class TutorialManager {
    
    private final Apocalipsis plugin;
    private final FileConfiguration config;
    private final ProgressiveDifficultySystem difficultySystem;
    private final MessageBus messageBus;
    private final TutorialDataPersistence dataPersistence;
    private final TutorialMetrics metrics;
    private final TutorialAchievements achievements;
    
    // NUEVO: Onboarding épico y Buddy system
    private final OnboardingManager onboardingManager;
    private final BuddyService buddyService;
    
    // Trackeo de estado del tutorial
    private final Map<UUID, TutorialState> tutorialStates;
    private final Map<UUID, BukkitTask> scheduledTutorials;
    private final Map<UUID, BukkitTask> tipTasks;
    private final Map<UUID, Integer> lastTipIndex;
    private final Map<UUID, BukkitTask> actionBarTasks;
    
    // Demo de rangos
    private final Map<UUID, BukkitTask> rankDemoTasks;
    
    // Verificación de cambios de fase
    private final Map<UUID, BukkitTask> phaseCheckTasks;
    
    private boolean enabled;
    private boolean verboseLogging;
    private int tutorialDelayMinutes;
    
    /**
     * Estado del tutorial de un jugador
     */
    public static class TutorialState {
        private boolean welcomed;
        private boolean tutorialStarted;
        private boolean kitGiven;
        private int currentStage;
        private boolean rankDemoShown;
        private boolean completed;
        private long startTime;
        private int lastPhaseNumber;
        
        public TutorialState() {
            this.welcomed = false;
            this.tutorialStarted = false;
            this.kitGiven = false;
            this.currentStage = 0;
            this.rankDemoShown = false;
            this.completed = false;
            this.startTime = System.currentTimeMillis();
            this.lastPhaseNumber = 1;
        }
        
        // Getters y setters
        public boolean isWelcomed() { return welcomed; }
        public void setWelcomed(boolean welcomed) { this.welcomed = welcomed; }
        
        public boolean isTutorialStarted() { return tutorialStarted; }
        public void setTutorialStarted(boolean started) { this.tutorialStarted = started; }
        
        public boolean isKitGiven() { return kitGiven; }
        public void setKitGiven(boolean given) { this.kitGiven = given; }
        
        public int getCurrentStage() { return currentStage; }
        public void setCurrentStage(int stage) { this.currentStage = stage; }
        
        public boolean isRankDemoShown() { return rankDemoShown; }
        public void setRankDemoShown(boolean shown) { this.rankDemoShown = shown; }
        
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        
        public long getStartTime() { return startTime; }
        
        public int getLastPhaseNumber() { return lastPhaseNumber; }
        public void setLastPhaseNumber(int phase) { this.lastPhaseNumber = phase; }
    }
    
    public TutorialManager(Apocalipsis plugin, FileConfiguration tutorialConfig,
                          ProgressiveDifficultySystem difficultySystem, MessageBus messageBus) {
        this.plugin = plugin;
        this.config = tutorialConfig;
        this.difficultySystem = difficultySystem;
        this.messageBus = messageBus;
        this.dataPersistence = new TutorialDataPersistence(plugin);
        this.metrics = new TutorialMetrics(plugin);
        this.achievements = new TutorialAchievements(plugin);
        this.onboardingManager = new OnboardingManager(plugin);
        this.buddyService = new BuddyService(plugin);
        
        this.tutorialStates = new HashMap<>();
        this.scheduledTutorials = new HashMap<>();
        this.tipTasks = new HashMap<>();
        this.lastTipIndex = new HashMap<>();
        this.actionBarTasks = new HashMap<>();
        this.rankDemoTasks = new HashMap<>();
        this.phaseCheckTasks = new HashMap<>();
        
        loadConfig();
        
        // Cargar datos de onboarding y buddy al iniciar
        onboardingManager.loadFromYaml();
        buddyService.loadFromYaml();
        
        startAutoSaveTask();
    }
    
    private void loadConfig() {
        this.enabled = config.getBoolean("tutorial.enabled", true);
        this.verboseLogging = config.getBoolean("tutorial.verbose_logging", false);
        this.tutorialDelayMinutes = config.getInt("tutorial.retraso_inicio_minutos", 5);
        
        if (verboseLogging) {
            plugin.getLogger().info("[Tutorial] Sistema de tutorial " + 
                                   (enabled ? "activado" : "desactivado"));
        }
    }
    
    /**
     * Maneja el primer join de un jugador
     */
    public void handleFirstJoin(Player player) {
        if (!enabled) return;
        
        UUID uuid = player.getUniqueId();
        
        // [IMPORTANTE] NO registrar en el sistema de dificultad todavía
        // Esto se hará cuando el tutorial termine exitosamente
        // Si lo hacemos ahora, hasPlayerData() devuelve true y cancela el tutorial
        
        // Intentar cargar datos guardados
        TutorialState loadedState = dataPersistence.loadTutorialState(uuid);
        Long loadedTime = dataPersistence.loadFirstJoinTime(uuid);
        
        // Crear o usar estado cargado
        if (loadedState != null && loadedTime != null) {
            tutorialStates.put(uuid, loadedState);
            plugin.getLogger().info(String.format(
                "[Tutorial] Datos cargados para %s (Etapa %d)",
                player.getName(), loadedState.getCurrentStage()
            ));
        } else {
            tutorialStates.put(uuid, new TutorialState());
            metrics.recordTutorialStarted();
        }
        
        // ═══════════════════════════════════════════════════════════
        // SIN KIT DE INICIO - SOLO EFECTOS/BUFEOS
        // Los jugadores reciben efectos temporales que se van quitando
        // conforme avanzan en el tutorial
        // ═══════════════════════════════════════════════════════════
        // giveStarterKit(player); // DESHABILITADO - Solo efectos
        
        // ═══════════════════════════════════════════════════════════
        // INICIAR SISTEMAS DE ONBOARDING Y BUDDY
        // ═══════════════════════════════════════════════════════════
        onboardingManager.startOnboarding(player); // Iniciar logros épicos de onboarding
        buddyService.tryMatchBuddy(player); // Intentar emparejar con mentor veterano
        
        // ═══════════════════════════════════════════════════════════
        // APLICAR BUFF DE REGENERACIÓN INICIAL Y MONITORIZAR CAMBIOS
        // ═══════════════════════════════════════════════════════════
        updateTutorialBuffs(player); // Aplicar buffs según fase actual
        startPhaseMonitoring(player); // Iniciar monitorización de cambios de fase
        
        // Mensaje de bienvenida inmediato (no invasivo)
        showWelcomeMessage(player);
        
        // Programar inicio del tutorial después de 5 minutos
        scheduleTutorialStart(player);
        
        // Iniciar sistema de tips progresivos
        startProgressiveTips(player);
        
        // Iniciar ActionBar de progreso
        startActionBarProgress(player);
        
        plugin.getLogger().info(String.format(
            "[Tutorial] Jugador %s iniciado. Tutorial comenzará en %d minutos.",
            player.getName(), tutorialDelayMinutes
        ));
    }
    
    /**
     * Muestra el mensaje de bienvenida inicial
     */
    private void showWelcomeMessage(Player player) {
        ConfigurationSection welcome = config.getConfigurationSection("tutorial.mensaje_bienvenida");
        if (welcome == null || !welcome.getBoolean("enabled", true)) return;
        
        TutorialState state = tutorialStates.get(player.getUniqueId());
        if (state != null && state.isWelcomed()) return;
        
        // Título
        String title = ChatColor.translateAlternateColorCodes('&',
            welcome.getString("titulo", "&6&l🌋 APOCALIPSIS SURVIVAL"));
        String subtitle = ChatColor.translateAlternateColorCodes('&',
            welcome.getString("subtitulo", "&e¡Bienvenido!"))
            .replace("%player%", player.getName());
        
        player.sendTitle(title, subtitle, 10, 70, 20);
        
        // Mensaje en chat
        String chatMessage = welcome.getString("chat", "");
        if (!chatMessage.isEmpty()) {
            for (String line : chatMessage.split("\n")) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    line.replace("%player%", player.getName())));
            }
        }
        
        if (state != null) {
            state.setWelcomed(true);
        }
    }
    
    /**
     * Programa el inicio del tutorial después del delay configurado
     */
    private void scheduleTutorialStart(Player player) {
        UUID uuid = player.getUniqueId();
        
        plugin.getLogger().info(String.format(
            "[Tutorial] Programando inicio de tutorial para %s en %d minutos (%d ticks)",
            player.getName(), tutorialDelayMinutes, tutorialDelayMinutes * 20L * 60L
        ));
        
        // Cancelar tarea anterior si existe
        if (scheduledTutorials.containsKey(uuid)) {
            scheduledTutorials.get(uuid).cancel();
        }
        
        // Programar inicio (convertir minutos a ticks: 1 minuto = 1200 ticks)
        long delayTicks = tutorialDelayMinutes * 20L * 60L;
        
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getLogger().info(String.format(
                    "[Tutorial] Ejecutando tarea programada para %s",
                    player.getName()
                ));
                startTutorial(player);
            } else {
                plugin.getLogger().info(String.format(
                    "[Tutorial] Jugador %s offline, cancelando tutorial",
                    player.getName()
                ));
            }
        }, delayTicks);
        
        scheduledTutorials.put(uuid, task);
    }
    
    /**
     * Inicia el tutorial y entrega el kit
     */
    private void startTutorial(Player player) {
        UUID uuid = player.getUniqueId();
        TutorialState state = tutorialStates.get(uuid);
        
        plugin.getLogger().info(String.format(
            "[Tutorial] startTutorial llamado para %s. Estado: %s",
            player.getName(), (state == null ? "null" : "existe")
        ));
        
        if (state == null || state.isTutorialStarted()) {
            plugin.getLogger().warning(String.format(
                "[Tutorial] Tutorial ya iniciado o estado null para %s. Saltando.",
                player.getName()
            ));
            return;
        }
        
        state.setTutorialStarted(true);
        
        // [IMPORTANTE] Registrar en el sistema de dificultad AHORA
        // Esto marca que el jugador ya recibió el tutorial
        difficultySystem.registerFirstJoin(player);
        
        plugin.getLogger().info(String.format(
            "[Tutorial] Tutorial iniciado para %s. Kit entregado y registrado en sistema.",
            player.getName()
        ));
    }
    
    /**
     * Actualiza los buffs del tutorial basándose en la fase actual de dificultad.
     * Sin kit de inicio - solo efectos/bufeos que se van quitando conforme avanzan.
     * Método público para uso por TutorialCommand y TutorialListener.
     */
    public void updateTutorialBuffs(Player player) {
        DifficultyPhase phase = difficultySystem.getPlayerPhase(player);
        
        // Quitar todos los buffs anteriores
        player.removePotionEffect(PotionEffectType.REGENERATION);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.HASTE);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        
        // Si la fase tiene regeneración pasiva habilitada, aplicarla
        if (phase.hasPassiveRegeneration()) {
            int duration = Integer.MAX_VALUE;
            
            // FASE 1: Máximo apoyo (muy fácil)
            if (phase.getPhaseNumber() == 1) {
                // Regeneration II (muy fuerte)
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION, duration, 1,
                    false, false
                ));
                // Speed I (moverse rápido)
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, duration, 0,
                    false, false
                ));
                // Haste I (minar rápido)
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.HASTE, duration, 0,
                    false, false
                ));
                
                if (verboseLogging) {
                    plugin.getLogger().info(String.format(
                        "[Tutorial] %s: Buffs MÁXIMOS aplicados (Regeneration II, Speed I, Haste I) - Fase Tutorial",
                        player.getName()
                    ));
                }
            }
            // FASE 2: Apoyo moderado (fácil)
            else if (phase.getPhaseNumber() == 2) {
                // Regeneration I (moderado)
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION, duration, 0,
                    false, false
                ));
                // Speed I (moverse rápido)
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, duration, 0,
                    false, false
                ));
                
                if (verboseLogging) {
                    plugin.getLogger().info(String.format(
                        "[Tutorial] %s: Buffs MODERADOS aplicados (Regeneration I, Speed I) - Fase Adaptación",
                        player.getName()
                    ));
                }
            }
        } else {
            // Sin regeneración pasiva (fases 3 en adelante)
            if (verboseLogging && !phase.isGlobalDifficulty()) {
                plugin.getLogger().info(String.format(
                    "[Tutorial] %s: Todos los buffs removidos (Fase %d)",
                    player.getName(), phase.getPhaseNumber()
                ));
            }
        }
    }
    
    /**
     * Inicia la monitorización periódica de cambios de fase para actualizar buffs automáticamente.
     * Verifica cada 30 segundos si el jugador cambió de fase.
     */
    private void startPhaseMonitoring(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Cancelar tarea anterior si existe
        if (phaseCheckTasks.containsKey(uuid)) {
            phaseCheckTasks.get(uuid).cancel();
        }
        
        // Obtener fase inicial
        TutorialState state = tutorialStates.get(uuid);
        if (state != null) {
            DifficultyPhase currentPhase = difficultySystem.getPlayerPhase(player);
            state.setLastPhaseNumber(currentPhase.getPhaseNumber());
        }
        
        // Verificar cada 30 segundos (600 ticks)
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            
            TutorialState playerState = tutorialStates.get(uuid);
            if (playerState == null) {
                return;
            }
            
            DifficultyPhase currentPhase = difficultySystem.getPlayerPhase(player);
            int lastPhase = playerState.getLastPhaseNumber();
            
            // Si cambió de fase, actualizar buffs y notificar
            if (currentPhase.getPhaseNumber() != lastPhase) {
                if (verboseLogging) {
                    plugin.getLogger().info(String.format(
                        "[Tutorial] %s cambió de fase %d a %d",
                        player.getName(), lastPhase, currentPhase.getPhaseNumber()
                    ));
                }
                
                // Actualizar buffs según nueva fase
                updateTutorialBuffs(player);
                
                // Notificar cambio si es relevante
                if (currentPhase.getPhaseNumber() > lastPhase) {
                    notifyBuffChange(player, currentPhase);
                    
                    // Verificar logros por fase
                    if (currentPhase.getPhaseNumber() == 3) {
                        achievements.unlockAchievement(player, "alcanzar_fase_3");
                    }
                    
                    // Guardar progreso
                    savePlayerProgress(player);
                }
                
                playerState.setLastPhaseNumber(currentPhase.getPhaseNumber());
            }
            
            // Verificar logro de 30 minutos
            long playedMinutes = difficultySystem.getPlayedTimeMinutes(player);
            if (playedMinutes >= 30 && !achievements.hasAchievement(uuid, "treinta_minutos_supervivencia")) {
                achievements.unlockAchievement(player, "treinta_minutos_supervivencia");
            }
            
            // Si alcanzó dificultad global, detener monitorización
            if (currentPhase.isGlobalDifficulty()) {
                if (phaseCheckTasks.containsKey(uuid)) {
                    phaseCheckTasks.get(uuid).cancel();
                    phaseCheckTasks.remove(uuid);
                }
            }
        }, 600L, 600L); // Cada 30 segundos
        
        phaseCheckTasks.put(uuid, task);
    }
    
    /**
     * Notifica al jugador sobre cambios en sus buffs de tutorial.
     */
    private void notifyBuffChange(Player player, DifficultyPhase newPhase) {
        if (newPhase.hasPassiveRegeneration()) {
            if (newPhase.getPhaseNumber() == 1) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a[Tutorial] &7Buffs MÁXIMOS activos: &eRegeneración II, Velocidad, Rapidez&7. &8(&e" + 
                    newPhase.getName() + "&8)"));
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a[Tutorial] &7Buffs MODERADOS activos: &eRegeneración I, Velocidad&7. &8(&e" + 
                    newPhase.getName() + "&8)"));
            }
        } else if (!newPhase.isGlobalDifficulty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&e[Tutorial] &7Se quitaron todos tus buffs de protección. &8(&c" + newPhase.getName() + "&8)"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&7¡Ya estás más preparado para sobrevivir por tu cuenta!"));
        }
    }
    
    /**
     * Entrega el kit de inicio al jugador
     */
    private void giveStarterKit(Player player) {
        ConfigurationSection kitSection = config.getConfigurationSection("kit_inicio");
        if (kitSection == null || !kitSection.getBoolean("enabled", true)) return;
        
        TutorialState state = tutorialStates.get(player.getUniqueId());
        if (state != null && state.isKitGiven()) return;
        
        // Ejecutar comandos de items
        List<String> items = kitSection.getStringList("items");
        for (String command : items) {
            String finalCommand = command.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
        }
        
        // Mensaje de entrega
        ConfigurationSection messageSection = kitSection.getConfigurationSection("mensaje_entrega");
        if (messageSection != null) {
            // Título
            if (messageSection.getBoolean("mostrar_titulo", true)) {
                String title = ChatColor.translateAlternateColorCodes('&',
                    messageSection.getString("titulo", "&6&l🎁 KIT DE INICIO"));
                String subtitle = ChatColor.translateAlternateColorCodes('&',
                    messageSection.getString("subtitulo", "&e¡Comienza tu aventura!"));
                
                player.sendTitle(title, subtitle, 10, 70, 20);
            }
            
            // Sonido
            String soundName = messageSection.getString("sonido", "ENTITY_PLAYER_LEVELUP");
            try {
                player.playSound(player.getLocation(), Sound.valueOf(soundName), 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                // Ignorar si el sonido no existe
            }
            
            // Mensaje en chat
            String chatMessage = messageSection.getString("mensaje_chat", "");
            if (!chatMessage.isEmpty()) {
                for (String line : chatMessage.split("\n")) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
                }
            }
        }
        
        if (state != null) {
            state.setKitGiven(true);
        }
    }
    
    /**
     * Muestra la demo de rangos con efectos temporales
     */
    public void showRankDemo(Player player) {
        ConfigurationSection demoSection = config.getConfigurationSection("demo_rangos");
        if (demoSection == null || !demoSection.getBoolean("enabled", true)) return;
        
        UUID uuid = player.getUniqueId();
        TutorialState state = tutorialStates.get(uuid);
        
        if (state != null && state.isRankDemoShown()) {
            player.sendMessage(ChatColor.RED + "Ya has visto la demo de rangos.");
            return;
        }
        
        // Mensaje de inicio
        ConfigurationSection startMsg = demoSection.getConfigurationSection("mensaje_inicio");
        if (startMsg != null) {
            if (startMsg.getBoolean("mostrar_titulo", true)) {
                String title = ChatColor.translateAlternateColorCodes('&',
                    startMsg.getString("titulo", "&b&l✨ DEMO DE RANGO"));
                String subtitle = ChatColor.translateAlternateColorCodes('&',
                    startMsg.getString("subtitulo", "&eExperimentando EXPLORADOR..."));
                
                player.sendTitle(title, subtitle, 10, 70, 20);
            }
            
            String soundName = startMsg.getString("sonido", "ENTITY_PLAYER_LEVELUP");
            try {
                player.playSound(player.getLocation(), Sound.valueOf(soundName), 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                // Ignorar
            }
            
            String chatMessage = startMsg.getString("mensaje_chat", "");
            if (!chatMessage.isEmpty()) {
                for (String line : chatMessage.split("\n")) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
                }
            }
        }
        
        // Aplicar efectos de EXPLORADOR temporalmente
        ConfigurationSection effectsSection = demoSection.getConfigurationSection("simular_efectos_explorador");
        if (effectsSection != null && effectsSection.getBoolean("enabled", true)) {
            int durationSeconds = effectsSection.getInt("duracion_segundos", 60);
            List<String> effects = effectsSection.getStringList("efectos");
            
            for (String effectStr : effects) {
                String[] parts = effectStr.split(":");
                if (parts.length >= 2) {
                    try {
                        PotionEffectType type = PotionEffectType.getByName(parts[0]);
                        int amplifier = Integer.parseInt(parts[1]) - 1; // Level 1 = amplifier 0
                        
                        if (type != null) {
                            player.addPotionEffect(new PotionEffect(
                                type, durationSeconds * 20, amplifier, false, false, true
                            ));
                        }
                    } catch (Exception e) {
                        // Ignorar efectos inválidos
                    }
                }
            }
            
            // Programar mensaje de fin
            BukkitTask endTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    showRankDemoEnd(player);
                }
            }, durationSeconds * 20L);
            
            rankDemoTasks.put(uuid, endTask);
        }
        
        if (state != null) {
            state.setRankDemoShown(true);
        }
        
        if (verboseLogging) {
            plugin.getLogger().info(String.format(
                "[Tutorial] Demo de rangos mostrada a %s",
                player.getName()
            ));
        }
    }
    
    /**
     * Muestra el mensaje de fin de la demo de rangos
     */
    private void showRankDemoEnd(Player player) {
        ConfigurationSection demoSection = config.getConfigurationSection("demo_rangos");
        if (demoSection == null) return;
        
        ConfigurationSection endMsg = demoSection.getConfigurationSection("mensaje_fin");
        if (endMsg != null) {
            if (endMsg.getBoolean("mostrar_titulo", true)) {
                String title = ChatColor.translateAlternateColorCodes('&',
                    endMsg.getString("titulo", "&c&lDemo Finalizada"));
                String subtitle = ChatColor.translateAlternateColorCodes('&',
                    endMsg.getString("subtitulo", "&7¡Sube de rango para tenerlo siempre!"));
                
                player.sendTitle(title, subtitle, 10, 70, 20);
            }
            
            String soundName = endMsg.getString("sonido", "BLOCK_NOTE_BLOCK_PLING");
            try {
                player.playSound(player.getLocation(), Sound.valueOf(soundName), 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                // Ignorar
            }
            
            String chatMessage = endMsg.getString("mensaje_chat", "");
            if (!chatMessage.isEmpty()) {
                for (String line : chatMessage.split("\n")) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
                }
            }
        }
    }
    
    /**
     * Inicia el sistema de tips progresivos basado en tiempo jugado
     */
    private void startProgressiveTips(Player player) {
        ConfigurationSection tipsSection = config.getConfigurationSection("tips_progresivos");
        if (tipsSection == null || !tipsSection.getBoolean("enabled", true)) return;
        
        UUID uuid = player.getUniqueId();
        int intervalMinutes = tipsSection.getInt("intervalo_minutos", 8);
        
        // Cancelar tarea anterior si existe
        if (tipTasks.containsKey(uuid)) {
            tipTasks.get(uuid).cancel();
        }
        
        // Programar tips periódicos
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (player.isOnline()) {
                showNextTip(player);
            }
        }, intervalMinutes * 20L * 60L, intervalMinutes * 20L * 60L);
        
        tipTasks.put(uuid, task);
        lastTipIndex.put(uuid, -1);
    }
    
    /**
     * Muestra el siguiente tip apropiado según el tiempo jugado
     */
    private void showNextTip(Player player) {
        ConfigurationSection tipsSection = config.getConfigurationSection("tips_progresivos");
        if (tipsSection == null) return;
        
        UUID uuid = player.getUniqueId();
        long playedMinutes = difficultySystem.getPlayedTimeMinutes(player);
        
        // Verificar si ya pasó el tiempo máximo para tips
        int maxTime = tipsSection.getInt("solo_para_tiempo_jugado_minutos", 240);
        if (playedMinutes > maxTime) {
            stopTips(uuid);
            return;
        }
        
        // Buscar tip apropiado
        List<Map<?, ?>> tips = tipsSection.getMapList("tips");
        int currentIndex = lastTipIndex.getOrDefault(uuid, -1);
        
        for (int i = 0; i < tips.size(); i++) {
            if (i <= currentIndex) continue; // Ya mostrado
            
            Map<?, ?> tipMap = tips.get(i);
            Object minTimeObj = tipMap.get("tiempo_minimo_minutos");
            Object maxTimeObj = tipMap.get("tiempo_maximo_minutos");
            int minTime = (minTimeObj instanceof Number) ? ((Number) minTimeObj).intValue() : 0;
            int maxTipTime = (maxTimeObj instanceof Number) ? ((Number) maxTimeObj).intValue() : 999999;
            
            if (playedMinutes >= minTime && playedMinutes < maxTipTime) {
                String message = ChatColor.translateAlternateColorCodes('&',
                    (String) tipMap.get("mensaje"));
                
                boolean showInActionBar = tipsSection.getBoolean("mostrar_en_actionbar", true);
                
                if (showInActionBar) {
                    messageBus.sendActionBar(player, message);
                } else {
                    player.sendMessage(message);
                }
                
                // Sonido opcional
                String soundName = tipsSection.getString("sonido", "BLOCK_NOTE_BLOCK_PLING");
                try {
                    player.playSound(player.getLocation(), Sound.valueOf(soundName), 0.5f, 1.0f);
                } catch (IllegalArgumentException e) {
                    // Ignorar
                }
                
                lastTipIndex.put(uuid, i);
                break;
            }
        }
    }
    
    /**
     * Inicia el ActionBar de progreso de dificultad
     */
    private void startActionBarProgress(Player player) {
        ConfigurationSection actionBarSection = config.getConfigurationSection("dificultad_progresiva.actionbar_progreso");
        if (actionBarSection == null || !actionBarSection.getBoolean("enabled", true)) return;
        
        UUID uuid = player.getUniqueId();
        int intervalMinutes = actionBarSection.getInt("intervalo_minutos", 10);
        
        // Cancelar tarea anterior si existe
        if (actionBarTasks.containsKey(uuid)) {
            actionBarTasks.get(uuid).cancel();
        }
        
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (player.isOnline()) {
                showProgressActionBar(player);
            }
        }, intervalMinutes * 20L * 60L, intervalMinutes * 20L * 60L);
        
        actionBarTasks.put(uuid, task);
    }
    
    /**
     * Muestra el ActionBar de progreso
     */
    private void showProgressActionBar(Player player) {
        DifficultyPhase phase = difficultySystem.getPlayerPhase(player);
        if (phase.isGlobalDifficulty()) return; // Ya alcanzó dificultad global
        
        ConfigurationSection actionBarSection = config.getConfigurationSection("dificultad_progresiva.actionbar_progreso");
        if (actionBarSection == null) return;
        
        long remainingMinutes = difficultySystem.getRemainingTimeToNextPhase(player);
        String timeFormatted = difficultySystem.formatRemainingTime(remainingMinutes);
        
        String format = actionBarSection.getString("formato",
            "&e⏰ Dificultad: &c%porcentaje%% &8| &7Próxima fase en: &e%tiempo_restante% &8| &f/avo menu");
        
        String message = ChatColor.translateAlternateColorCodes('&', format
            .replace("%porcentaje%", String.valueOf(phase.getPercentDifficulty()))
            .replace("%tiempo_restante%", timeFormatted));
        
        messageBus.sendActionBar(player, message);
    }
    
    /**
     * Notifica al jugador sobre un cambio de fase
     */
    public void notifyPhaseChange(Player player, DifficultyPhase oldPhase, DifficultyPhase newPhase) {
        ConfigurationSection notifySection = config.getConfigurationSection("dificultad_progresiva.notificar_cambio_fase");
        if (notifySection == null || !notifySection.getBoolean("enabled", true)) return;
        
        // Si alcanzó dificultad global, mostrar mensaje especial
        if (newPhase.isGlobalDifficulty()) {
            notifyGlobalDifficultyReached(player);
            return;
        }
        
        // Título
        if (notifySection.getBoolean("mostrar_titulo", true)) {
            String title = ChatColor.translateAlternateColorCodes('&',
                notifySection.getString("titulo", "&e⚠️ CAMBIO DE DIFICULTAD"));
            String subtitle = ChatColor.translateAlternateColorCodes('&',
                notifySection.getString("subtitulo", ""))
                .replace("%fase_anterior%", oldPhase.getName())
                .replace("%fase_nueva%", newPhase.getName());
            
            player.sendTitle(title, subtitle, 10, 70, 20);
        }
        
        // Sonido
        String soundName = notifySection.getString("sonido", "ENTITY_PLAYER_LEVELUP");
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            // Ignorar
        }
        
        // Mensaje en chat
        String chatMessage = notifySection.getString("mensaje_chat", "");
        if (!chatMessage.isEmpty()) {
            long remainingMinutes = difficultySystem.getRemainingTimeToNextPhase(player);
            String timeFormatted = difficultySystem.formatRemainingTime(remainingMinutes);
            
            for (String line : chatMessage.split("\n")) {
                String formattedLine = ChatColor.translateAlternateColorCodes('&', line
                    .replace("%fase_anterior%", oldPhase.getName())
                    .replace("%fase_nueva%", newPhase.getName())
                    .replace("%porcentaje_anterior%", String.valueOf(oldPhase.getPercentDifficulty()))
                    .replace("%porcentaje_nuevo%", String.valueOf(newPhase.getPercentDifficulty()))
                    .replace("%descripcion%", newPhase.getDescription())
                    .replace("%tiempo_restante%", timeFormatted));
                
                player.sendMessage(formattedLine);
            }
        }
    }
    
    /**
     * Notifica al jugador que alcanzó la dificultad global
     */
    private void notifyGlobalDifficultyReached(Player player) {
        ConfigurationSection globalMsg = config.getConfigurationSection("dificultad_progresiva.mensaje_dificultad_global");
        if (globalMsg == null || !globalMsg.getBoolean("enabled", true)) return;
        
        // Título
        if (globalMsg.getBoolean("mostrar_titulo", true)) {
            String title = ChatColor.translateAlternateColorCodes('&',
                globalMsg.getString("titulo", "&4&l⚔️ DIFICULTAD GLOBAL"));
            String subtitle = ChatColor.translateAlternateColorCodes('&',
                globalMsg.getString("subtitulo", "&c¡Fase de adaptación completada!"));
            
            player.sendTitle(title, subtitle, 10, 100, 20);
        }
        
        // Sonido
        String soundName = globalMsg.getString("sonido", "UI_TOAST_CHALLENGE_COMPLETE");
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            // Ignorar
        }
        
        // Fuegos artificiales
        int fireworks = globalMsg.getInt("fuegos_artificiales", 0);
        if (fireworks > 0) {
            spawnFireworks(player, fireworks);
        }
        
        // Logro de tutorial completado
        achievements.unlockAchievement(player, "tutorial_completado");
        
        // Marcar como completado en estado
        TutorialState state = tutorialStates.get(player.getUniqueId());
        if (state != null) {
            state.setCompleted(true);
            dataPersistence.saveTutorialState(player.getUniqueId(), state, 
                difficultySystem.getPlayedTimeMinutes(player));
        }
        
        metrics.recordTutorialCompleted();
        
        // Mensaje en chat
        String chatMessage = globalMsg.getString("mensaje_chat", "");
        if (!chatMessage.isEmpty()) {
            for (String line : chatMessage.split("\n")) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
            }
        }
        
        // Detener ActionBar de progreso
        UUID uuid = player.getUniqueId();
        if (actionBarTasks.containsKey(uuid)) {
            actionBarTasks.get(uuid).cancel();
            actionBarTasks.remove(uuid);
        }
    }
    
    /**
     * Detiene los tips de un jugador
     */
    private void stopTips(UUID uuid) {
        if (tipTasks.containsKey(uuid)) {
            tipTasks.get(uuid).cancel();
            tipTasks.remove(uuid);
        }
        lastTipIndex.remove(uuid);
    }
    
    /**
     * Limpia todos los datos de un jugador al desconectarse
     */
    public void handlePlayerQuit(UUID uuid) {
        // Cancelar tareas
        if (scheduledTutorials.containsKey(uuid)) {
            scheduledTutorials.get(uuid).cancel();
            scheduledTutorials.remove(uuid);
        }
        
        if (tipTasks.containsKey(uuid)) {
            tipTasks.get(uuid).cancel();
            tipTasks.remove(uuid);
        }
        
        if (actionBarTasks.containsKey(uuid)) {
            actionBarTasks.get(uuid).cancel();
            actionBarTasks.remove(uuid);
        }
        
        if (rankDemoTasks.containsKey(uuid)) {
            rankDemoTasks.get(uuid).cancel();
            rankDemoTasks.remove(uuid);
        }
        
        if (phaseCheckTasks.containsKey(uuid)) {
            phaseCheckTasks.get(uuid).cancel();
            phaseCheckTasks.remove(uuid);
        }
        
        // Guardar progreso de onboarding y buddy antes de desconectar
        if (onboardingManager != null) {
            onboardingManager.saveToYaml();
        }
        if (buddyService != null) {
            buddyService.saveToYaml();
        }
        
        // Mantener estado del tutorial en memoria (se puede guardar a DB)
        // tutorialStates.remove(uuid);
        
        lastTipIndex.remove(uuid);
    }
    
    /**
     * Reinicia el tutorial de un jugador (comando admin)
     */
    public void resetPlayerTutorial(UUID uuid) {
        tutorialStates.remove(uuid);
        stopTips(uuid);
        
        if (scheduledTutorials.containsKey(uuid)) {
            scheduledTutorials.get(uuid).cancel();
            scheduledTutorials.remove(uuid);
        }
        
        if (actionBarTasks.containsKey(uuid)) {
            actionBarTasks.get(uuid).cancel();
            actionBarTasks.remove(uuid);
        }
        
        if (rankDemoTasks.containsKey(uuid)) {
            rankDemoTasks.get(uuid).cancel();
            rankDemoTasks.remove(uuid);
        }
        
        if (phaseCheckTasks.containsKey(uuid)) {
            phaseCheckTasks.get(uuid).cancel();
            phaseCheckTasks.remove(uuid);
        }
        
        difficultySystem.resetPlayer(uuid);
        
        if (verboseLogging) {
            plugin.getLogger().info(String.format(
                "[Tutorial] Tutorial reseteado para UUID: %s",
                uuid.toString()
            ));
        }
    }
    
    public TutorialState getTutorialState(UUID uuid) {
        return tutorialStates.get(uuid);
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void reload() {
        loadConfig();
    }
    
    /**
     * Spawner fuegos artificiales para celebración
     */
    private void spawnFireworks(Player player, int count) {
        for (int i = 0; i < count; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    org.bukkit.FireworkEffect effect = org.bukkit.FireworkEffect.builder()
                        .with(org.bukkit.FireworkEffect.Type.BALL_LARGE)
                        .withColor(org.bukkit.Color.RED, org.bukkit.Color.ORANGE, org.bukkit.Color.YELLOW)
                        .withFade(org.bukkit.Color.WHITE)
                        .trail(true)
                        .build();
                    
                    org.bukkit.entity.Firework fw = player.getWorld().spawn(
                        player.getLocation().add(0, 1, 0), 
                        org.bukkit.entity.Firework.class
                    );
                    
                    org.bukkit.inventory.meta.FireworkMeta meta = fw.getFireworkMeta();
                    meta.addEffect(effect);
                    meta.setPower(1);
                    fw.setFireworkMeta(meta);
                } catch (Exception e) {
                    plugin.getLogger().warning("[Tutorial] Error spawneando fuegos artificiales: " + e.getMessage());
                }
            }, i * 10L);
        }
    }
    
    /**
     * Guarda el progreso de un jugador
     */
    private void savePlayerProgress(Player player) {
        UUID uuid = player.getUniqueId();
        TutorialState state = tutorialStates.get(uuid);
        
        if (state != null) {
            Long firstJoinTime = difficultySystem.hasPlayerData(player) 
                ? System.currentTimeMillis() - (difficultySystem.getPlayedTimeMinutes(player) * 60000)
                : System.currentTimeMillis();
            
            dataPersistence.saveTutorialState(uuid, state, firstJoinTime);
        }
    }
    
    /**
     * Inicia tarea de guardado automático cada 5 minutos
     */
    private void startAutoSaveTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (tutorialStates.containsKey(player.getUniqueId())) {
                    savePlayerProgress(player);
                }
            }
            
            if (verboseLogging) {
                plugin.getLogger().info("[Tutorial] Guardado automático completado");
            }
        }, 6000L, 6000L); // Cada 5 minutos
    }
    
    /**
     * Verifica si un jugador está en tutorial activo
     */
    public boolean isInTutorial(Player player) {
        TutorialState state = tutorialStates.get(player.getUniqueId());
        return state != null && !state.isCompleted();
    }
    
    /**
     * Obtiene el tiempo jugado en minutos
     */
    public long getPlayedTimeMinutes(Player player) {
        return difficultySystem.getPlayedTimeMinutes(player);
    }
    
    /**
     * Obtiene referencia al sistema de métricas
     */
    public TutorialMetrics getMetrics() {
        return metrics;
    }
    
    /**
     * Obtiene referencia al sistema de onboarding
     */
    public OnboardingManager getOnboardingManager() {
        return onboardingManager;
    }
    
    /**
     * Obtiene referencia al servicio de buddy/mentores
     */
    public BuddyService getBuddyService() {
        return buddyService;
    }
    
    /**
     * Obtiene referencia al sistema de logros
     */
    public TutorialAchievements getAchievements() {
        return achievements;
    }
}
