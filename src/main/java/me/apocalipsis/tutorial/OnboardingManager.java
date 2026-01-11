/*
 * Apocalipsis Plugin - Sistema de Onboarding Épico
 * Copyright (c) 2026 Apocalipsis Plugin
 * 
 * Gestiona los hitos de onboarding para nuevos jugadores
 * Hitos: Caminar, Craftear, Construir, Misión, Desastre Tutorial
 */
package me.apocalipsis.tutorial;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import me.apocalipsis.Apocalipsis;

import java.util.*;

/**
 * Gestiona el onboarding épico de nuevos jugadores
 * Hitos en los primeros 30 minutos que definen la experiencia
 */
public class OnboardingManager {
    
    private final Apocalipsis plugin;
    
    // Tracking de hitos por jugador
    private final Map<UUID, OnboardingProgress> playerProgress;
    
    // Tareas programadas
    private final Map<UUID, BukkitTask> checkTasks;
    
    /**
     * Hitos del onboarding
     */
    public enum OnboardingMilestone {
        WALK_100_BLOCKS("Explorador Novato", 50, "§a✓ EXPLORADOR NOVATO", "§7Has dado tus primeros pasos en este mundo"),
        CRAFT_FIRST_ITEM("Artesano Inicial", 100, "§a✓ ARTESANO INICIAL", "§7Tu primera creación toma forma"),
        BUILD_SHELTER("Constructor de Refugios", 150, "§6✓ CONSTRUCTOR", "§7Tu primer refugio está listo"),
        COMPLETE_FIRST_MISSION("Trabajador Dedicado", 100, "§6✓ TRABAJADOR", "§7Primera misión completada"),
        SURVIVE_TUTORIAL_DISASTER("Sobreviviente Valiente", 300, "§e§l✓ SOBREVIVIENTE", "§7¡Has superado tu primer desastre!");
        
        private final String displayName;
        private final int xpReward;
        private final String title;
        private final String subtitle;
        
        OnboardingMilestone(String displayName, int xpReward, String title, String subtitle) {
            this.displayName = displayName;
            this.xpReward = xpReward;
            this.title = title;
            this.subtitle = subtitle;
        }
        
        public String getDisplayName() { return displayName; }
        public int getXpReward() { return xpReward; }
        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
    }
    
    /**
     * Progreso de onboarding de un jugador
     */
    public static class OnboardingProgress {
        private final Set<OnboardingMilestone> completed;
        private final long startTime;
        
        // Tracking de progreso
        private int blocksWalked;
        private boolean hasCrafted;
        private int blocksPlaced;
        private boolean hasCompletedMission;
        private boolean hasSurvivedDisaster;
        
        public OnboardingProgress() {
            this.completed = new HashSet<>();
            this.startTime = System.currentTimeMillis();
            this.blocksWalked = 0;
            this.hasCrafted = false;
            this.blocksPlaced = 0;
            this.hasCompletedMission = false;
            this.hasSurvivedDisaster = false;
        }
        
        public boolean isCompleted(OnboardingMilestone milestone) {
            return completed.contains(milestone);
        }
        
        public void complete(OnboardingMilestone milestone) {
            completed.add(milestone);
        }
        
        public int getCompletedCount() {
            return completed.size();
        }
        
        public boolean isFullyCompleted() {
            return completed.size() >= OnboardingMilestone.values().length;
        }
        
        // Getters y setters
        public int getBlocksWalked() { return blocksWalked; }
        public void addBlocksWalked(int blocks) { this.blocksWalked += blocks; }
        public void setBlocksWalked(int blocks) { this.blocksWalked = blocks; }
        
        public boolean hasCrafted() { return hasCrafted; }
        public void setCrafted(boolean crafted) { this.hasCrafted = crafted; }
        
        public int getBlocksPlaced() { return blocksPlaced; }
        public void addBlocksPlaced(int blocks) { this.blocksPlaced += blocks; }
        public void setBlocksPlaced(int blocks) { this.blocksPlaced = blocks; }
        
        public boolean hasCompletedMission() { return hasCompletedMission; }
        public void setCompletedMission(boolean completed) { this.hasCompletedMission = completed; }
        
        public boolean hasSurvivedDisaster() { return hasSurvivedDisaster; }
        public void setSurvivedDisaster(boolean survived) { this.hasSurvivedDisaster = survived; }
        
        public long getStartTime() { return startTime; }
    }
    
    public OnboardingManager(Apocalipsis plugin) {
        this.plugin = plugin;
        this.playerProgress = new HashMap<>();
        this.checkTasks = new HashMap<>();
    }
    
    /**
     * Inicia el onboarding para un jugador nuevo
     */
    public void startOnboarding(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (playerProgress.containsKey(uuid)) {
            return; // Ya tiene onboarding en progreso
        }
        
        OnboardingProgress progress = new OnboardingProgress();
        playerProgress.put(uuid, progress);
        
        // Mensaje inicial del Observador
        player.sendMessage("");
        player.sendMessage("§5§l⚡ EL OBSERVADOR: §7§o¿Otro que despierta aquí?");
        player.sendMessage("§7§oEscucha... el mundo aún respira, pero algo ha cambiado.");
        player.sendMessage("");
        
        // Iniciar verificación periódica
        startProgressCheck(player);
        
        plugin.getLogger().info("[Onboarding] Iniciado para " + player.getName());
    }
    
    /**
     * Inicia verificación periódica de progreso
     */
    private void startProgressCheck(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Verificar cada 5 segundos (100 ticks)
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopProgressCheck(uuid);
                return;
            }
            
            OnboardingProgress progress = playerProgress.get(uuid);
            if (progress == null || progress.isFullyCompleted()) {
                stopProgressCheck(uuid);
                return;
            }
            
            checkMilestones(player, progress);
        }, 100L, 100L);
        
        checkTasks.put(uuid, task);
    }
    
    /**
     * Detiene verificación de progreso
     */
    private void stopProgressCheck(UUID uuid) {
        BukkitTask task = checkTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Verifica y completa hitos automáticamente
     */
    private void checkMilestones(Player player, OnboardingProgress progress) {
        UUID uuid = player.getUniqueId();
        
        // HITO 1: Caminar 100 bloques
        if (!progress.isCompleted(OnboardingMilestone.WALK_100_BLOCKS)) {
            if (progress.getBlocksWalked() >= 100) {
                completeMilestone(player, OnboardingMilestone.WALK_100_BLOCKS);
            }
        }
        
        // HITO 2: Craftear primer item
        if (!progress.isCompleted(OnboardingMilestone.CRAFT_FIRST_ITEM)) {
            if (progress.hasCrafted()) {
                completeMilestone(player, OnboardingMilestone.CRAFT_FIRST_ITEM);
            }
        }
        
        // HITO 3: Construir refugio (15 bloques)
        if (!progress.isCompleted(OnboardingMilestone.BUILD_SHELTER)) {
            if (progress.getBlocksPlaced() >= 15) {
                completeMilestone(player, OnboardingMilestone.BUILD_SHELTER);
            }
        }
        
        // HITO 4: Completar primera misión
        if (!progress.isCompleted(OnboardingMilestone.COMPLETE_FIRST_MISSION)) {
            if (progress.hasCompletedMission()) {
                completeMilestone(player, OnboardingMilestone.COMPLETE_FIRST_MISSION);
            }
        }
        
        // HITO 5: Sobrevivir desastre tutorial
        if (!progress.isCompleted(OnboardingMilestone.SURVIVE_TUTORIAL_DISASTER)) {
            if (progress.hasSurvivedDisaster()) {
                completeMilestone(player, OnboardingMilestone.SURVIVE_TUTORIAL_DISASTER);
                onOnboardingCompleted(player);
            }
        }
    }
    
    /**
     * Completa un hito y otorga recompensas
     */
    private void completeMilestone(Player player, OnboardingMilestone milestone) {
        UUID uuid = player.getUniqueId();
        OnboardingProgress progress = playerProgress.get(uuid);
        
        if (progress == null || progress.isCompleted(milestone)) {
            return;
        }
        
        progress.complete(milestone);
        
        // Título épico
        player.sendTitle(milestone.getTitle(), milestone.getSubtitle(), 10, 70, 20);
        
        // Sonido
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        
        // XP
        if (plugin.getExperienceService() != null) {
            plugin.getExperienceService().addXP(player, milestone.getXpReward(), "Onboarding: " + milestone.getDisplayName(), false);
        }
        
        // Mensaje
        player.sendMessage("");
        player.sendMessage("§6§l═══════════════════════════════════");
        player.sendMessage("§e§l  ✓ " + milestone.getDisplayName().toUpperCase());
        player.sendMessage("§7  +" + milestone.getXpReward() + " XP");
        player.sendMessage("§6§l═══════════════════════════════════");
        player.sendMessage("");
        
        // Mensaje del Observador
        sendObserverMessage(player, milestone);
        
        plugin.getLogger().info("[Onboarding] " + player.getName() + " completó: " + milestone.name());
    }
    
    /**
     * Mensajes del Observador por hito
     */
    private void sendObserverMessage(Player player, OnboardingMilestone milestone) {
        String message = "";
        
        switch (milestone) {
            case WALK_100_BLOCKS:
                message = "§5§l⚡ EL OBSERVADOR: §7§oEl Observador siente tu movimiento. Continúa.";
                break;
            case CRAFT_FIRST_ITEM:
                message = "§5§l⚡ EL OBSERVADOR: §7§oHas trazado tu marca. Bien.";
                break;
            case BUILD_SHELTER:
                message = "§5§l⚡ EL OBSERVADOR: §7§oTu refugio toma forma. La protección es el primer aprendizaje.";
                break;
            case COMPLETE_FIRST_MISSION:
                message = "§5§l⚡ EL OBSERVADOR: §7§oEntiendes el trabajo. Sigue avanzando.";
                break;
            case SURVIVE_TUTORIAL_DISASTER:
                message = "§5§l⚡ EL OBSERVADOR: §7§oSobreviviste. Ya no eres nuevo. Eres superviviente.";
                break;
        }
        
        if (!message.isEmpty()) {
            player.sendMessage(message);
        }
    }
    
    /**
     * Cuando completa todo el onboarding
     */
    private void onOnboardingCompleted(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Mensaje épico
        player.sendMessage("");
        player.sendMessage("§6§l╔═══════════════════════════════════════╗");
        player.sendMessage("§6§l║   🌟 ONBOARDING COMPLETADO 🌟      ║");
        player.sendMessage("§6§l╚═══════════════════════════════════════╝");
        player.sendMessage("§7Ya no necesitas mi guía.");
        player.sendMessage("§7Usa §f/avo menu §7para ver todo lo disponible.");
        player.sendMessage("");
        
        // Bonus XP
        if (plugin.getExperienceService() != null) {
            plugin.getExperienceService().addXP(player, 200, "Onboarding Completado", false);
        }
        
        stopProgressCheck(uuid);
        
        plugin.getLogger().info("[Onboarding] " + player.getName() + " completó TODO el onboarding");
    }
    
    // ═══════════════════════════════════════════════════════════════
    // API PÚBLICA - Para listeners externos
    // ═══════════════════════════════════════════════════════════════
    
    public void onPlayerWalk(Player player, int blocks) {
        OnboardingProgress progress = playerProgress.get(player.getUniqueId());
        if (progress != null && !progress.isCompleted(OnboardingMilestone.WALK_100_BLOCKS)) {
            progress.addBlocksWalked(blocks);
        }
    }
    
    public void onPlayerCraft(Player player) {
        OnboardingProgress progress = playerProgress.get(player.getUniqueId());
        if (progress != null && !progress.hasCrafted()) {
            progress.setCrafted(true);
        }
    }
    
    public void onPlayerPlaceBlock(Player player) {
        OnboardingProgress progress = playerProgress.get(player.getUniqueId());
        if (progress != null && !progress.isCompleted(OnboardingMilestone.BUILD_SHELTER)) {
            progress.addBlocksPlaced(1);
        }
    }
    
    public void onPlayerCompleteMission(Player player) {
        OnboardingProgress progress = playerProgress.get(player.getUniqueId());
        if (progress != null && !progress.hasCompletedMission()) {
            progress.setCompletedMission(true);
        }
    }
    
    public void onPlayerSurviveDisaster(Player player) {
        OnboardingProgress progress = playerProgress.get(player.getUniqueId());
        if (progress != null && !progress.hasSurvivedDisaster()) {
            progress.setSurvivedDisaster(true);
        }
    }
    
    public boolean isOnboardingActive(UUID uuid) {
        return playerProgress.containsKey(uuid);
    }
    
    /**
     * Verifica si un jugador ha completado todos los hitos de onboarding
     * Usado por MissionService para determinar si debe recibir misiones diarias
     */
    public boolean hasCompletedOnboarding(UUID uuid) {
        OnboardingProgress progress = playerProgress.get(uuid);
        if (progress == null) {
            // Si no tiene progreso registrado, asumimos que ya lo completó
            // (jugadores antiguos que no tuvieron este sistema)
            return true;
        }
        return progress.isFullyCompleted();
    }
    
    public OnboardingProgress getProgress(UUID uuid) {
        return playerProgress.get(uuid);
    }
    
    /**
     * Obtiene la lista de hitos pendientes de un jugador
     * @return Lista de nombres de hitos sin completar
     */
    public java.util.List<String> getPendingMilestones(UUID uuid) {
        OnboardingProgress progress = playerProgress.get(uuid);
        if (progress == null || progress.isFullyCompleted()) {
            return java.util.Collections.emptyList();
        }
        
        java.util.List<String> pending = new java.util.ArrayList<>();
        for (OnboardingMilestone milestone : OnboardingMilestone.values()) {
            if (!progress.isCompleted(milestone)) {
                pending.add(milestone.name());
            }
        }
        return pending;
    }
    
    public void removePlayer(UUID uuid) {
        stopProgressCheck(uuid);
        playerProgress.remove(uuid);
    }
    
    /**
     * Notificación cuando un jugador sube de rango
     */
    public void onPlayerRankUp(Player player) {
        OnboardingProgress progress = playerProgress.get(player.getUniqueId());
        if (progress != null && !progress.isFullyCompleted()) {
            // Notificar al observador (solo si no completó onboarding aún)
            player.sendMessage("§5§l⚡ EL OBSERVADOR: §7§oBuen trabajo. Tu poder crece.");
        }
    }
    
    /**
     * Guarda el estado del onboarding en YAML
     */
    public void saveToYaml() {
        if (plugin.getConfig().getConfigurationSection("onboarding-data") == null) {
            plugin.getConfig().createSection("onboarding-data");
        }
        
        org.bukkit.configuration.ConfigurationSection onboardingSection = plugin.getConfig().getConfigurationSection("onboarding-data");
        onboardingSection.getKeys(false).forEach(key -> onboardingSection.set(key, null));
        
        for (Map.Entry<UUID, OnboardingProgress> entry : playerProgress.entrySet()) {
            UUID uuid = entry.getKey();
            OnboardingProgress progress = entry.getValue();
            
            String path = "onboarding-data." + uuid.toString();
            plugin.getConfig().set(path + ".blocksWalked", progress.getBlocksWalked());
            plugin.getConfig().set(path + ".hasCrafted", progress.hasCrafted());
            plugin.getConfig().set(path + ".blocksPlaced", progress.getBlocksPlaced());
            plugin.getConfig().set(path + ".hasCompletedMission", progress.hasCompletedMission());
            plugin.getConfig().set(path + ".hasSurvivedDisaster", progress.hasSurvivedDisaster());
            
            List<String> completedMilestones = new java.util.ArrayList<>();
            for (OnboardingMilestone milestone : progress.completed) {
                completedMilestones.add(milestone.toString());
            }
            plugin.getConfig().set(path + ".completedMilestones", completedMilestones);
        }
        
        plugin.saveConfig();
    }
    
    /**
     * Carga el estado del onboarding desde YAML
     */
    public void loadFromYaml() {
        if (plugin.getConfig().getConfigurationSection("onboarding-data") == null) {
            return;
        }
        
        org.bukkit.configuration.ConfigurationSection onboardingSection = plugin.getConfig().getConfigurationSection("onboarding-data");
        
        for (String uuidStr : onboardingSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String path = "onboarding-data." + uuidStr;
                
                OnboardingProgress progress = new OnboardingProgress();
                progress.setBlocksWalked(plugin.getConfig().getInt(path + ".blocksWalked", 0));
                progress.setCrafted(plugin.getConfig().getBoolean(path + ".hasCrafted", false));
                progress.setBlocksPlaced(plugin.getConfig().getInt(path + ".blocksPlaced", 0));
                progress.setCompletedMission(plugin.getConfig().getBoolean(path + ".hasCompletedMission", false));
                progress.setSurvivedDisaster(plugin.getConfig().getBoolean(path + ".hasSurvivedDisaster", false));
                
                List<String> completedMilestones = plugin.getConfig().getStringList(path + ".completedMilestones");
                for (String milestone : completedMilestones) {
                    try {
                        progress.complete(OnboardingMilestone.valueOf(milestone));
                    } catch (IllegalArgumentException ignored) {}
                }
                
                playerProgress.put(uuid, progress);
            } catch (IllegalArgumentException ignored) {
                // UUID inválido, saltar
            }
        }
    }
}
