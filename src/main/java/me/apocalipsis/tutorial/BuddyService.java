/*
 * Apocalipsis Plugin - Sistema de Buddy (Mentor/Aprendiz)
 * Copyright (c) 2026 Apocalipsis Plugin
 * 
 * Empareja automáticamente nuevos jugadores con veteranos
 * Recompensas mutuas por progreso conjunto
 */
package me.apocalipsis.tutorial;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.missions.MissionRank;

import java.util.*;

/**
 * Gestiona emparejamiento automático de nuevos con veteranos
 */
public class BuddyService {
    
    private final Apocalipsis plugin;
    
    // Emparejamientos activos: UUID del nuevo → UUID del mentor
    private final Map<UUID, UUID> activeBuddies;
    
    // Recompensas pendientes por entregar
    private final Map<UUID, Integer> pendingMentorRewards;
    
    // Tiempo de duración del buddy (7 días en millis)
    private static final long BUDDY_DURATION = 7L * 24L * 60L * 60L * 1000L;
    
    // Timestamps de inicio de buddy
    private final Map<UUID, Long> buddyStartTimes;
    
    /**
     * Razones de recompensa para el mentor
     */
    public enum BuddyRewardReason {
        APPRENTICE_MISSION_COMPLETED(25, 50, "completó una misión"),
        APPRENTICE_RANK_UP(100, 100, "subió de rango"),
        BOTH_SURVIVED_DISASTER(50, 50, "sobrevivieron juntos un desastre"),
        DAILY_BUDDY_TIME(25, 25, "jugaron 1 hora juntos");
        
        private final int psReward;
        private final int xpReward;
        private final String description;
        
        BuddyRewardReason(int psReward, int xpReward, String description) {
            this.psReward = psReward;
            this.xpReward = xpReward;
            this.description = description;
        }
        
        public int getPsReward() { return psReward; }
        public int getXpReward() { return xpReward; }
        public String getDescription() { return description; }
    }
    
    public BuddyService(Apocalipsis plugin) {
        this.plugin = plugin;
        this.activeBuddies = new HashMap<>();
        this.pendingMentorRewards = new HashMap<>();
        this.buddyStartTimes = new HashMap<>();
    }
    
    /**
     * Intenta emparejar a un nuevo jugador con un mentor veterano
     */
    public boolean tryMatchBuddy(Player newPlayer) {
        UUID newUuid = newPlayer.getUniqueId();
        
        // Ya tiene mentor
        if (activeBuddies.containsKey(newUuid)) {
            return false;
        }
        
        // Buscar mentor disponible
        Player mentor = findAvailableMentor(newPlayer);
        if (mentor == null) {
            plugin.getLogger().info("[Buddy] No se encontró mentor disponible para " + newPlayer.getName());
            return false;
        }
        
        // Crear emparejamiento
        createBuddyPair(newPlayer, mentor);
        return true;
    }
    
    /**
     * Encuentra un mentor veterano disponible
     */
    private Player findAvailableMentor(Player newPlayer) {
        List<Player> candidates = new ArrayList<>();
        
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(newPlayer.getUniqueId())) {
                continue; // No emparejarse consigo mismo
            }
            
            // Verificar que no tenga aprendiz actualmente
            if (isMentoringAnyone(online.getUniqueId())) {
                continue;
            }
            
            // Verificar rango mínimo (EXPLORADOR o superior)
            MissionRank rank = plugin.getRankService().getRank(online);
            if (rank.ordinal() < MissionRank.EXPLORADOR.ordinal()) {
                continue;
            }
            
            candidates.add(online);
        }
        
        // Retornar aleatorio de los candidatos
        if (candidates.isEmpty()) {
            return null;
        }
        
        return candidates.get(new Random().nextInt(candidates.size()));
    }
    
    /**
     * Verifica si un jugador está siendo mentor de alguien
     */
    private boolean isMentoringAnyone(UUID uuid) {
        return activeBuddies.containsValue(uuid);
    }
    
    /**
     * Crea emparejamiento entre nuevo y mentor
     */
    private void createBuddyPair(Player newPlayer, Player mentor) {
        UUID newUuid = newPlayer.getUniqueId();
        UUID mentorUuid = mentor.getUniqueId();
        
        activeBuddies.put(newUuid, mentorUuid);
        buddyStartTimes.put(newUuid, System.currentTimeMillis());
        
        // Notificar a ambos
        newPlayer.sendMessage("");
        newPlayer.sendMessage("§6§l╔═══════════════════════════════════════╗");
        newPlayer.sendMessage("§6§l║      🤝 MENTOR ASIGNADO 🤝         ║");
        newPlayer.sendMessage("§6§l╚═══════════════════════════════════════╝");
        newPlayer.sendMessage("§a✓ §6" + mentor.getName() + " §7es ahora tu mentor");
        newPlayer.sendMessage("§7Pueden ayudarse mutuamente y ganar recompensas");
        newPlayer.sendMessage("§7Duración: §e7 días");
        newPlayer.sendMessage("");
        
        mentor.sendMessage("");
        mentor.sendMessage("§6§l╔═══════════════════════════════════════╗");
        mentor.sendMessage("§6§l║     🎓 NUEVO APRENDIZ 🎓          ║");
        mentor.sendMessage("§6§l╚═══════════════════════════════════════╝");
        mentor.sendMessage("§a✓ §6" + newPlayer.getName() + " §7es ahora tu aprendiz");
        mentor.sendMessage("§7Cuando progrese, tú también recibirás recompensas");
        mentor.sendMessage("§7Duración: §e7 días");
        mentor.sendMessage("");
        
        // Sonidos
        newPlayer.playSound(newPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        mentor.playSound(mentor.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        
        plugin.getLogger().info("[Buddy] Emparejamiento creado: " + newPlayer.getName() + " (aprendiz) ← " + mentor.getName() + " (mentor)");
    }
    
    /**
     * Otorga recompensa al mentor cuando el aprendiz logra algo
     */
    public void rewardMentor(UUID apprenticeUuid, BuddyRewardReason reason) {
        UUID mentorUuid = activeBuddies.get(apprenticeUuid);
        if (mentorUuid == null) {
            return; // No tiene mentor
        }
        
        // Verificar expiración (7 días)
        Long startTime = buddyStartTimes.get(apprenticeUuid);
        if (startTime != null && System.currentTimeMillis() - startTime > BUDDY_DURATION) {
            removeBuddyPair(apprenticeUuid);
            return;
        }
        
        Player mentor = Bukkit.getPlayer(mentorUuid);
        Player apprentice = Bukkit.getPlayer(apprenticeUuid);
        
        // Otorgar PS
        if (plugin.getMissionService() != null) {
            int currentPS = plugin.getMissionService().getPS(mentorUuid);
            plugin.getMissionService().setPS(mentorUuid, currentPS + reason.getPsReward());
        }
        
        // Otorgar XP
        if (mentor != null && plugin.getExperienceService() != null) {
            plugin.getExperienceService().addXP(mentor, reason.getXpReward(), "Mentor: " + reason.getDescription(), false);
        }
        
        // Notificar
        if (mentor != null) {
            String apprenticeName = apprentice != null ? apprentice.getName() : "tu aprendiz";
            mentor.sendMessage("§a§l[MENTOR] §7Tu aprendiz §6" + apprenticeName + " §7" + reason.getDescription());
            mentor.sendMessage("§a§l[+] §e+" + reason.getPsReward() + " PS §7| §e+" + reason.getXpReward() + " XP");
            mentor.playSound(mentor.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
        }
        
        if (apprentice != null) {
            String mentorName = mentor != null ? mentor.getName() : "tu mentor";
            apprentice.sendMessage("§a§l[APRENDIZ] §7Tu mentor §6" + mentorName + " §7recibió recompensa por tu progreso");
        }
        
        plugin.getLogger().info("[Buddy] Recompensa otorgada a mentor " + mentorUuid + " por: " + reason.getDescription());
    }
    
    /**
     * Remueve emparejamiento (al expirar o desconectar)
     */
    public void removeBuddyPair(UUID apprenticeUuid) {
        UUID mentorUuid = activeBuddies.remove(apprenticeUuid);
        buddyStartTimes.remove(apprenticeUuid);
        
        if (mentorUuid != null) {
            Player mentor = Bukkit.getPlayer(mentorUuid);
            Player apprentice = Bukkit.getPlayer(apprenticeUuid);
            
            if (mentor != null) {
                String apprenticeName = apprentice != null ? apprentice.getName() : "Tu aprendiz";
                mentor.sendMessage("§e[MENTOR] §7El período de mentoría con §6" + apprenticeName + " §7ha terminado");
            }
            
            if (apprentice != null) {
                String mentorName = mentor != null ? mentor.getName() : "Tu mentor";
                apprentice.sendMessage("§e[APRENDIZ] §7El período de mentoría con §6" + mentorName + " §7ha terminado");
            }
            
            plugin.getLogger().info("[Buddy] Emparejamiento terminado: " + apprenticeUuid + " ↔ " + mentorUuid);
        }
    }
    
    /**
     * Verifica si un jugador tiene mentor activo
     */
    public boolean hasMentor(UUID uuid) {
        return activeBuddies.containsKey(uuid);
    }
    
    /**
     * Obtiene el UUID del mentor de un aprendiz
     */
    public UUID getMentor(UUID apprenticeUuid) {
        return activeBuddies.get(apprenticeUuid);
    }
    
    /**
     * Obtiene el UUID del aprendiz de un mentor
     */
    public UUID getApprentice(UUID mentorUuid) {
        for (Map.Entry<UUID, UUID> entry : activeBuddies.entrySet()) {
            if (entry.getValue().equals(mentorUuid)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * Limpieza al desconectar
     */
    public void onPlayerQuit(UUID uuid) {
        // Si es aprendiz, remover
        if (activeBuddies.containsKey(uuid)) {
            removeBuddyPair(uuid);
        }
    }
    
    /**
     * Empareja manualmente un aprendiz con un mentor
     */
    public void matchBuddy(UUID apprenticeUuid, UUID mentorUuid) {
        // Remover emparejamientos anteriores
        activeBuddies.remove(apprenticeUuid);
        
        // Nuevo emparejamiento
        activeBuddies.put(apprenticeUuid, mentorUuid);
        buddyStartTimes.put(apprenticeUuid, System.currentTimeMillis());
    }
    
    /**
     * Desempareja un jugador (ya sea aprendiz o mentor)
     */
    public void unmatchBuddy(UUID uuid) {
        // Si es aprendiz
        if (activeBuddies.containsKey(uuid)) {
            removeBuddyPair(uuid);
            return;
        }
        
        // Si es mentor, remover su aprendiz
        for (Map.Entry<UUID, UUID> entry : new HashMap<>(activeBuddies).entrySet()) {
            if (entry.getValue().equals(uuid)) {
                activeBuddies.remove(entry.getKey());
                buddyStartTimes.remove(entry.getKey());
                return;
            }
        }
    }
    
    /**
     * Verifica si un jugador tiene buddy activo (como aprendiz o mentor)
     */
    public boolean isBuddyActive(UUID uuid) {
        // Es aprendiz
        if (activeBuddies.containsKey(uuid)) {
            return true;
        }
        
        // Es mentor
        for (UUID mentorUuid : activeBuddies.values()) {
            if (mentorUuid.equals(uuid)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Guarda el estado del buddy system en YAML
     */
    public void saveToYaml() {
        if (plugin.getConfig().getConfigurationSection("buddy-data") == null) {
            plugin.getConfig().createSection("buddy-data");
        }
        
        org.bukkit.configuration.ConfigurationSection buddySection = plugin.getConfig().getConfigurationSection("buddy-data");
        buddySection.getKeys(false).forEach(key -> buddySection.set(key, null));
        
        // Guardar emparejamientos activos
        for (Map.Entry<UUID, UUID> entry : activeBuddies.entrySet()) {
            UUID apprenticeUuid = entry.getKey();
            UUID mentorUuid = entry.getValue();
            Long startTime = buddyStartTimes.get(apprenticeUuid);
            
            String path = "buddy-data.pairs." + apprenticeUuid.toString();
            plugin.getConfig().set(path + ".mentor", mentorUuid.toString());
            plugin.getConfig().set(path + ".startTime", startTime != null ? startTime : System.currentTimeMillis());
        }
        
        // Guardar recompensas pendientes
        for (Map.Entry<UUID, Integer> entry : pendingMentorRewards.entrySet()) {
            String path = "buddy-data.rewards." + entry.getKey().toString();
            plugin.getConfig().set(path, entry.getValue());
        }
        
        plugin.saveConfig();
    }
    
    /**
     * Carga el estado del buddy system desde YAML
     */
    public void loadFromYaml() {
        if (plugin.getConfig().getConfigurationSection("buddy-data") == null) {
            return;
        }
        
        // Cargar emparejamientos
        org.bukkit.configuration.ConfigurationSection pairsSection = plugin.getConfig().getConfigurationSection("buddy-data.pairs");
        if (pairsSection != null) {
            for (String apprenticeStr : pairsSection.getKeys(false)) {
                try {
                    UUID apprenticeUuid = UUID.fromString(apprenticeStr);
                    UUID mentorUuid = UUID.fromString(plugin.getConfig().getString("buddy-data.pairs." + apprenticeStr + ".mentor"));
                    Long startTime = plugin.getConfig().getLong("buddy-data.pairs." + apprenticeStr + ".startTime");
                    
                    // Verificar si el emparejamiento aún es válido (menos de 7 días)
                    long durationMs = System.currentTimeMillis() - startTime;
                    if (durationMs < BUDDY_DURATION) {
                        activeBuddies.put(apprenticeUuid, mentorUuid);
                        buddyStartTimes.put(apprenticeUuid, startTime);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
        
        // Cargar recompensas pendientes
        org.bukkit.configuration.ConfigurationSection rewardsSection = plugin.getConfig().getConfigurationSection("buddy-data.rewards");
        if (rewardsSection != null) {
            for (String mentorStr : rewardsSection.getKeys(false)) {
                try {
                    UUID mentorUuid = UUID.fromString(mentorStr);
                    int rewards = plugin.getConfig().getInt("buddy-data.rewards." + mentorStr);
                    pendingMentorRewards.put(mentorUuid, rewards);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }
}
