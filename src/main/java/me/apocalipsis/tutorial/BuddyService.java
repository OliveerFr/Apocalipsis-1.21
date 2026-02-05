/*
 * Apocalipsis Plugin - Sistema de Buddy (Mentor/Aprendiz)
 * Copyright (c) 2026 Apocalipsis Plugin
 * 
 * Empareja automáticamente nuevos jugadores con veteranos
 * Recompensas mutuas por progreso conjunto
 */
package me.apocalipsis.tutorial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.missions.MissionRank;

/**
 * Gestiona emparejamiento automático de nuevos con veteranos
 */
public class BuddyService {
    
    private final Apocalipsis plugin;
    
    // ═══════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DINÁMICA DEL SISTEMA BUDDY
    // ═══════════════════════════════════════════════════════════════
    
    // Configuración de rangos para mentor/aprendiz
    // Estos valores se calculan dinámicamente basados en los rangos disponibles
    private final int MIN_MENTOR_RANK_INDEX;     // Mínimo rango para ser mentor
    private final int MAX_APPRENTICE_RANK_INDEX; // Máximo rango para ser aprendiz
    
    // Emparejamientos activos: UUID del nuevo → UUID del mentor
    private final Map<UUID, UUID> activeBuddies;
    
    // Recompensas pendientes por entregar
    private final Map<UUID, Integer> pendingMentorRewards;
    
    // Tiempo de duración del buddy (1 día en millis)
    private static final long BUDDY_DURATION = 1L * 24L * 60L * 60L * 1000L;
    
    // Timestamps de inicio de buddy
    private final Map<UUID, Long> buddyStartTimes;
    
    // Historial de mentores previos: UUID del aprendiz → Lista de UUIDs de mentores
    private final Map<UUID, List<UUID>> mentorHistory;
    
    // Tracking de estadísticas por mentor
    private final Map<UUID, BuddyStats> mentorStats;
    
    // Tracking de tiempo jugado juntos hoy (apprentice UUID -> millis)
    private final Map<UUID, Long> dailyTimeTogetherStart;
    private final Map<UUID, Long> dailyTimeTogether;
    
    // Último día verificado para reset diario
    private int lastDayChecked;
    
    /**
     * Estadísticas de un mentor
     */
    public static class BuddyStats {
        private int missionsRewarded;
        private int rankUpsRewarded;
        private int disastersRewarded;
        private int dailyTimeRewarded;
        private int totalPsEarned;
        private int totalXpEarned;
        
        public void recordReward(BuddyRewardReason reason) {
            switch (reason) {
                case APPRENTICE_MISSION_COMPLETED:
                    missionsRewarded++;
                    break;
                case APPRENTICE_RANK_UP:
                    rankUpsRewarded++;
                    break;
                case BOTH_SURVIVED_DISASTER:
                    disastersRewarded++;
                    break;
                case DAILY_BUDDY_TIME:
                    dailyTimeRewarded++;
                    break;
            }
            totalPsEarned += reason.getPsReward();
            totalXpEarned += reason.getXpReward();
        }
        
        public int getMissionsRewarded() { return missionsRewarded; }
        public int getRankUpsRewarded() { return rankUpsRewarded; }
        public int getDisastersRewarded() { return disastersRewarded; }
        public int getDailyTimeRewarded() { return dailyTimeRewarded; }
        public int getTotalPsEarned() { return totalPsEarned; }
        public int getTotalXpEarned() { return totalXpEarned; }
        public int getTotalRewards() { return missionsRewarded + rankUpsRewarded + disastersRewarded + dailyTimeRewarded; }
    }
    
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
        
        // ═══════════════════════════════════════════════════════════════
        // INICIALIZACIÓN DINÁMICA DE RANGOS
        // ═══════════════════════════════════════════════════════════════
        
        // Calcular rangos dinámicamente según la configuración disponible
        int totalRanks = MissionRank.values().length;
        
        // Mentor: debe ser al menos rango 2 (tercer rango), pero no más alto que rango total-1
        // Ejemplos: Con 8 rangos (0-7) → mínimo rango 2
        //          Con 5 rangos (0-4) → mínimo rango 2  
        //          Con 3 rangos (0-2) → mínimo rango 2 (último rango)
        this.MIN_MENTOR_RANK_INDEX = Math.min(2, totalRanks - 1);
        
        // Aprendiz: máximo los primeros 2 rangos (0-1), pero ajustado si hay pocos rangos
        // Ejemplos: Con 8 rangos → máximo rango 1 (NOVATO=0, EXPLORADOR=1)
        //          Con 3 rangos → máximo rango 0 (solo primer rango puede ser aprendiz)
        this.MAX_APPRENTICE_RANK_INDEX = Math.min(1, totalRanks - 2);
        
        plugin.getLogger().info("[Buddy] Configuración dinámica cargada:");
        plugin.getLogger().info("[Buddy] - Total rangos: " + totalRanks);
        plugin.getLogger().info("[Buddy] - Rango mínimo mentor: " + MIN_MENTOR_RANK_INDEX + " (" + 
                                MissionRank.values()[MIN_MENTOR_RANK_INDEX].name() + ")");
        plugin.getLogger().info("[Buddy] - Rango máximo aprendiz: " + MAX_APPRENTICE_RANK_INDEX + " (" + 
                                MissionRank.values()[MAX_APPRENTICE_RANK_INDEX].name() + ")");
        
        // ═══════════════════════════════════════════════════════════════
        // INICIALIZACIÓN DE ESTRUCTURAS DE DATOS
        // ═══════════════════════════════════════════════════════════════
        
        this.activeBuddies = new HashMap<>();
        this.pendingMentorRewards = new HashMap<>();
        this.buddyStartTimes = new HashMap<>();
        this.mentorStats = new HashMap<>();
        this.dailyTimeTogetherStart = new HashMap<>();
        this.dailyTimeTogether = new HashMap<>();
        this.mentorHistory = new HashMap<>();
        this.lastDayChecked = -1;
        
        // Iniciar scheduler de tiempo jugado juntos
        startDailyTimeScheduler();
    }
    
    /**
     * Intenta emparejar a un nuevo jugador con un mentor veterano
     */
    public boolean tryMatchBuddy(Player newPlayer) {
        UUID newUuid = newPlayer.getUniqueId();
        
        // Verificar que el jugador tenga rango elegible para ser aprendiz
        MissionRank playerRank = plugin.getRankService().getRank(newPlayer);
        if (playerRank.ordinal() > MAX_APPRENTICE_RANK_INDEX) {
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[Buddy] " + newPlayer.getName() + " tiene rango " + playerRank + 
                                       ", no necesita mentor (máximo elegible: " + 
                                       MissionRank.values()[MAX_APPRENTICE_RANK_INDEX].name() + ")");
            }
            return false;
        }
        
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
     * Maneja la conexión de cualquier jugador para auto-asignación de mentor si aplica.
     * - Si el que entra es un posible mentor y no tiene aprendiz, intenta asignarle
     *   automáticamente el primer novato elegible online sin mentor.
     */
    public void handlePlayerJoin(Player player) {
        // ¿Es elegible como mentor?
        if (!isEligibleMentor(player)) {
            return;
        }
        // ¿Ya está mentoreando a alguien? entonces no asignar otro
        if (isMentoringAnyone(player.getUniqueId())) {
            return;
        }
        // Buscar un novato online sin mentor y con onboarding no completado (si existe el sistema)
        for (Player candidate : Bukkit.getOnlinePlayers()) {
            if (candidate.getUniqueId().equals(player.getUniqueId())) continue;
            if (!isNoviceNeedingMentor(candidate)) continue;
            // Emparejar directamente con este mentor que acaba de entrar
            createBuddyPair(candidate, player);
            break; // Solo un aprendiz por mentor
        }
    }
    
    private boolean isEligibleMentor(Player p) {
        MissionRank rank = plugin.getRankService().getRank(p);
        return rank.ordinal() >= MIN_MENTOR_RANK_INDEX;
    }
    
    private boolean isNoviceNeedingMentor(Player p) {
        UUID uuid = p.getUniqueId();
        // Ya tiene mentor
        if (activeBuddies.containsKey(uuid)) {
            return false;
        }
        
        // Verificar rango: usar configuración dinámica
        MissionRank rank = plugin.getRankService().getRank(p);
        if (rank.ordinal() > MAX_APPRENTICE_RANK_INDEX) {
            return false;
        }
        
        // Si hay sistema de onboarding, usarlo para priorizar novatos reales
        if (plugin.getTutorialManager() != null && plugin.getTutorialManager().getOnboardingManager() != null) {
            try {
                return !plugin.getTutorialManager().getOnboardingManager().hasCompletedOnboarding(uuid);
            } catch (Throwable t) {
                // En caso de cualquier excepción, hacer fallback a permitir por rango
            }
        }
        // Fallback: solo por rango
        return true;
    }
    
    /**
     * Encuentra un mentor veterano disponible
     */
    private Player findAvailableMentor(Player newPlayer) {
        List<Player> candidates = new ArrayList<>();
        UUID newUuid = newPlayer.getUniqueId();
        List<UUID> previousMentors = mentorHistory.getOrDefault(newUuid, new ArrayList<>());
        
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(newPlayer.getUniqueId())) {
                continue; // No emparejarse consigo mismo
            }
            
            // Verificar que no tenga aprendiz actualmente
            if (isMentoringAnyone(online.getUniqueId())) {
                continue;
            }
            
            // Verificar rango mínimo para ser mentor
            MissionRank rank = plugin.getRankService().getRank(online);
            if (rank.ordinal() < MIN_MENTOR_RANK_INDEX) {
                continue;
            }
            
            // No permitir el mismo mentor otra vez
            if (previousMentors.contains(online.getUniqueId())) {
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
        
        // Agregar al historial de mentores
        List<UUID> history = mentorHistory.computeIfAbsent(newUuid, k -> new ArrayList<>());
        history.add(mentorUuid);
        
        // Notificar a ambos
        newPlayer.sendMessage("");
        newPlayer.sendMessage("§6§l╔═══════════════════════════════════════╗");
        newPlayer.sendMessage("§6§l║      🤝 MENTOR ASIGNADO 🤝         ║");
        newPlayer.sendMessage("§6§l╚═══════════════════════════════════════╝");
        newPlayer.sendMessage("§a✓ §6" + mentor.getName() + " §7es ahora tu mentor");
        newPlayer.sendMessage("§7Pueden ayudarse mutuamente y ganar recompensas");
        newPlayer.sendMessage("§7Duración: §e1 día");
        newPlayer.sendMessage("");
        
        mentor.sendMessage("");
        mentor.sendMessage("§6§l╔═══════════════════════════════════════╗");
        mentor.sendMessage("§6§l║     🎓 NUEVO APRENDIZ 🎓          ║");
        mentor.sendMessage("§6§l╚═══════════════════════════════════════╝");
        mentor.sendMessage("§a✓ §6" + newPlayer.getName() + " §7es ahora tu aprendiz");
        mentor.sendMessage("§7Cuando progrese, tú también recibirás recompensas");
        mentor.sendMessage("§7Duración: §e1 día");
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
        
        // Verificar expiración (1 día)
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
        
        // Registrar estadísticas
        BuddyStats stats = mentorStats.computeIfAbsent(mentorUuid, k -> new BuddyStats());
        stats.recordReward(reason);
        
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
        
        // Agregar al historial de mentores si no existe
        List<UUID> history = mentorHistory.computeIfAbsent(apprenticeUuid, k -> new ArrayList<>());
        if (!history.contains(mentorUuid)) {
            history.add(mentorUuid);
        }
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
     * Inicia scheduler que verifica tiempo jugado juntos
     */
    private void startDailyTimeScheduler() {
        // Verificar cada 5 minutos (6000 ticks)
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            checkDailyTimeTogether();
        }, 6000L, 6000L);
    }
    
    /**
     * Verifica y recompensa por tiempo jugado juntos
     */
    private void checkDailyTimeTogether() {
        long currentTime = System.currentTimeMillis();
        
        // Reset diario si cambió el día
        int currentDay = (int) (System.currentTimeMillis() / (24L * 60L * 60L * 1000L));
        if (lastDayChecked != currentDay) {
            dailyTimeTogether.clear();
            dailyTimeTogetherStart.clear();
            lastDayChecked = currentDay;
        }
        
        for (Map.Entry<UUID, UUID> entry : new HashMap<>(activeBuddies).entrySet()) {
            UUID apprenticeUuid = entry.getKey();
            UUID mentorUuid = entry.getValue();
            
            Player apprentice = Bukkit.getPlayer(apprenticeUuid);
            Player mentor = Bukkit.getPlayer(mentorUuid);
            
            // Ambos deben estar online
            if (apprentice == null || mentor == null) {
                dailyTimeTogetherStart.remove(apprenticeUuid);
                continue;
            }
            
            // Iniciar contador si no existe
            if (!dailyTimeTogetherStart.containsKey(apprenticeUuid)) {
                dailyTimeTogetherStart.put(apprenticeUuid, currentTime);
            }
            
            // Calcular tiempo acumulado
            long startTime = dailyTimeTogetherStart.get(apprenticeUuid);
            long sessionTime = currentTime - startTime;
            long totalTime = dailyTimeTogether.getOrDefault(apprenticeUuid, 0L) + sessionTime;
            
            // Si alcanzaron 1 hora (3,600,000 ms), dar recompensa
            if (totalTime >= 3600000L && dailyTimeTogether.getOrDefault(apprenticeUuid, 0L) < 3600000L) {
                rewardMentor(apprenticeUuid, BuddyRewardReason.DAILY_BUDDY_TIME);
                dailyTimeTogether.put(apprenticeUuid, totalTime);
            } else {
                dailyTimeTogether.put(apprenticeUuid, totalTime);
            }
            
            // Resetear inicio para próxima verificación
            dailyTimeTogetherStart.put(apprenticeUuid, currentTime);
        }
    }
    
    /**
     * Obtiene información completa de un buddy
     */
    public Map<String, Object> getBuddyInfo(UUID uuid) {
        Map<String, Object> info = new HashMap<>();
        
        // Es aprendiz?
        if (activeBuddies.containsKey(uuid)) {
            UUID mentorUuid = activeBuddies.get(uuid);
            Long startTime = buddyStartTimes.get(uuid);
            
            info.put("role", "apprentice");
            info.put("mentorUuid", mentorUuid);
            info.put("mentorName", Bukkit.getOfflinePlayer(mentorUuid).getName());
            
            if (startTime != null) {
                long elapsed = System.currentTimeMillis() - startTime;
                long remaining = BUDDY_DURATION - elapsed;
                info.put("daysRemaining", remaining / (24L * 60L * 60L * 1000L));
                info.put("startTime", startTime);
            }
        }
        
        // Es mentor?
        UUID apprenticeUuid = getApprentice(uuid);
        if (apprenticeUuid != null) {
            Long startTime = buddyStartTimes.get(apprenticeUuid);
            
            info.put("role", "mentor");
            info.put("apprenticeUuid", apprenticeUuid);
            info.put("apprenticeName", Bukkit.getOfflinePlayer(apprenticeUuid).getName());
            
            if (startTime != null) {
                long elapsed = System.currentTimeMillis() - startTime;
                long remaining = BUDDY_DURATION - elapsed;
                info.put("daysRemaining", remaining / (24L * 60L * 60L * 1000L));
                info.put("startTime", startTime);
            }
            
            // Agregar estadísticas
            BuddyStats stats = mentorStats.get(uuid);
            if (stats != null) {
                info.put("stats", stats);
            }
        }
        
        return info;
    }
    
    /**
     * Obtiene todos los emparejamientos activos
     */
    public Map<UUID, UUID> getAllBuddyPairs() {
        return new HashMap<>(activeBuddies);
    }
    
    /**
     * Obtiene estadísticas de un mentor
     */
    public BuddyStats getMentorStats(UUID mentorUuid) {
        return mentorStats.getOrDefault(mentorUuid, new BuddyStats());
    }
    
    /**
     * Obtiene estadísticas globales del sistema
     */
    public Map<String, Integer> getGlobalStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("activePairs", activeBuddies.size());
        
        int totalMissions = 0;
        int totalRankUps = 0;
        int totalDisasters = 0;
        int totalDailyTime = 0;
        int totalPs = 0;
        int totalXp = 0;
        
        for (BuddyStats s : mentorStats.values()) {
            totalMissions += s.getMissionsRewarded();
            totalRankUps += s.getRankUpsRewarded();
            totalDisasters += s.getDisastersRewarded();
            totalDailyTime += s.getDailyTimeRewarded();
            totalPs += s.getTotalPsEarned();
            totalXp += s.getTotalXpEarned();
        }
        
        stats.put("totalMissions", totalMissions);
        stats.put("totalRankUps", totalRankUps);
        stats.put("totalDisasters", totalDisasters);
        stats.put("totalDailyTime", totalDailyTime);
        stats.put("totalPs", totalPs);
        stats.put("totalXp", totalXp);
        
        return stats;
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
        
        // Guardar historial de mentores
        for (Map.Entry<UUID, List<UUID>> entry : mentorHistory.entrySet()) {
            List<String> mentorStrings = new ArrayList<>();
            for (UUID mentorUuid : entry.getValue()) {
                mentorStrings.add(mentorUuid.toString());
            }
            String path = "buddy-data.history." + entry.getKey().toString();
            plugin.getConfig().set(path, mentorStrings);
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
                    
                    // Verificar si el emparejamiento aún es válido (menos de 1 día)
                    long durationMs = System.currentTimeMillis() - startTime;
                    if (durationMs < BUDDY_DURATION) {
                        activeBuddies.put(apprenticeUuid, mentorUuid);
                        buddyStartTimes.put(apprenticeUuid, startTime);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
        
        // Cargar historial de mentores
        org.bukkit.configuration.ConfigurationSection historySection = plugin.getConfig().getConfigurationSection("buddy-data.history");
        if (historySection != null) {
            for (String apprenticeStr : historySection.getKeys(false)) {
                try {
                    UUID apprenticeUuid = UUID.fromString(apprenticeStr);
                    List<String> mentorStrings = plugin.getConfig().getStringList("buddy-data.history." + apprenticeStr);
                    List<UUID> mentors = new ArrayList<>();
                    for (String mentorStr : mentorStrings) {
                        try {
                            mentors.add(UUID.fromString(mentorStr));
                        } catch (IllegalArgumentException ignored) {}
                    }
                    if (!mentors.isEmpty()) {
                        mentorHistory.put(apprenticeUuid, mentors);
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
    
    // ═══════════════════════════════════════════════════════════════
    // MÉTODOS DE DIAGNÓSTICO Y VALIDACIÓN
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Obtiene información de configuración del sistema buddy para diagnóstico
     */
    public Map<String, Object> getDiagnosticInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // Información de configuración
        info.put("totalRanks", MissionRank.values().length);
        info.put("minMentorRankIndex", MIN_MENTOR_RANK_INDEX);
        info.put("maxApprenticeRankIndex", MAX_APPRENTICE_RANK_INDEX);
        info.put("minMentorRankName", MissionRank.values()[MIN_MENTOR_RANK_INDEX].name());
        info.put("maxApprenticeRankName", MissionRank.values()[MAX_APPRENTICE_RANK_INDEX].name());
        
        // Estadísticas de emparejamiento
        info.put("activeBuddies", activeBuddies.size());
        info.put("mentorStats", mentorStats.size());
        info.put("pendingRewards", pendingMentorRewards.size());
        
        // Información actual de jugadores online
        int potentialMentors = 0;
        int potentialApprentices = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isEligibleMentor(p) && !isMentoringAnyone(p.getUniqueId())) {
                potentialMentors++;
            }
            if (isNoviceNeedingMentor(p)) {
                potentialApprentices++;
            }
        }
        info.put("potentialMentorsOnline", potentialMentors);
        info.put("potentialApprenticesOnline", potentialApprentices);
        
        return info;
    }
    
    /**
     * Valida la configuración y reporta posibles problemas
     */
    public List<String> validateConfiguration() {
        List<String> issues = new ArrayList<>();
        
        // Verificar que hay suficientes rangos
        int totalRanks = MissionRank.values().length;
        if (totalRanks < 3) {
            issues.add("CRÍTICO: Solo " + totalRanks + " rangos disponibles. Se recomiendan al menos 3 rangos para el sistema buddy.");
        }
        
        // Verificar configuración coherente
        if (MIN_MENTOR_RANK_INDEX <= MAX_APPRENTICE_RANK_INDEX) {
            issues.add("ADVERTENCIA: Rango mínimo mentor (" + MIN_MENTOR_RANK_INDEX + 
                      ") debe ser mayor que rango máximo aprendiz (" + MAX_APPRENTICE_RANK_INDEX + ").");
        }
        
        // Verificar que hay jugadores que pueden ser mentores
        boolean hasPotentialMentors = false;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isEligibleMentor(p)) {
                hasPotentialMentors = true;
                break;
            }
        }
        if (!hasPotentialMentors) {
            issues.add("INFO: No hay jugadores online elegibles como mentores (rango mínimo: " + 
                      MissionRank.values()[MIN_MENTOR_RANK_INDEX].name() + ").");
        }
        
        return issues;
    }
}
