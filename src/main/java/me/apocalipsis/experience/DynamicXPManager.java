package me.apocalipsis.experience;

import me.apocalipsis.Apocalipsis;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Sistema dinámico de XP con bonificaciones, rachas, combos, milestones y protección anti-farm
 */
public class DynamicXPManager {
    
    private final Apocalipsis plugin;
    private final ExperienceService experienceService;
    
    // Cache de configuración
    private final Map<XPSource, Integer> baseXPCache = new EnumMap<>(XPSource.class);
    private final Map<XPSource, Integer> cooldownCache = new EnumMap<>(XPSource.class);
    
    // Tracking por jugador
    private final Map<UUID, PlayerXPTracker> playerTrackers = new ConcurrentHashMap<>();
    
    // Leaderboard diario
    private final Map<UUID, Integer> dailyLeaderboard = new ConcurrentHashMap<>();
    private LocalDate leaderboardDate = LocalDate.now();
    
    // Hora feliz
    private boolean horaFelizActiva = false;
    private double horaFelizMultiplier = 2.0;
    private BukkitTask horaFelizTask;
    
    // Configuración global
    private double weekendMultiplier = 1.5;
    private double nightMultiplier = 1.2;
    private double streakMultiplier = 0.1;
    private int maxStreakDays = 7;
    private boolean verboseLogging = true;
    
    // Combos
    private double comboMultiplier = 1.3;
    private int comboTimeWindowMs = 5000;
    private int comboMinActions = 3;
    private int comboMaxLimit = 10;         // Límite máximo de combo
    
    // Multiplicadores por dimensión
    private double overworldMultiplier = 1.0;
    private double netherMultiplier = 1.3;
    private double endMultiplier = 1.5;
    
    // Anti-farm
    private boolean antiFarmEnabled = true;
    private int antiFarmThreshold = 50;
    private int antiFarmWindowSeconds = 60;
    private double antiFarmPenalty = 0.1;
    
    // ActionBar
    private boolean actionBarEnabled = true;
    
    // Presencia del Streamer
    private boolean presenciaStreamerEnabled = false;
    private String streamerUsername = "OliveerF";
    private double multiplicadorOnline = 1.0;
    private double multiplicadorOffline = 0.2;
    
    // Horas felices programadas
    private List<HoraFelizSchedule> horasFelicesProgramadas = new ArrayList<>();
    
    public DynamicXPManager(Apocalipsis plugin, ExperienceService experienceService) {
        this.plugin = plugin;
        this.experienceService = experienceService;
        reloadConfig();
        startHoraFelizScheduler();
    }
    
    /**
     * Recarga toda la configuración de XP
     */
    public void reloadConfig() {
        FileConfiguration config = plugin.getConfigManager().getRecompensasConfig();
        
        // Cargar multiplicadores globales
        weekendMultiplier = config.getDouble("xp_dinamico.multiplicadores.fin_de_semana", 1.5);
        nightMultiplier = config.getDouble("xp_dinamico.multiplicadores.noche", 1.2);
        streakMultiplier = config.getDouble("xp_dinamico.multiplicadores.racha_por_dia", 0.1);
        maxStreakDays = config.getInt("xp_dinamico.multiplicadores.max_dias_racha", 7);
        verboseLogging = config.getBoolean("xp_dinamico.verbose_logging", true);
        
        // Combos
        comboMultiplier = config.getDouble("xp_dinamico.combos.multiplicador", 1.3);
        comboTimeWindowMs = config.getInt("xp_dinamico.combos.ventana_ms", 5000);
        comboMinActions = config.getInt("xp_dinamico.combos.acciones_minimas", 3);
        comboMaxLimit = config.getInt("xp_dinamico.combos.limite_maximo", 10);
        
        // Dimensiones
        overworldMultiplier = config.getDouble("xp_dinamico.dimensiones.overworld", 1.0);
        netherMultiplier = config.getDouble("xp_dinamico.dimensiones.nether", 1.3);
        endMultiplier = config.getDouble("xp_dinamico.dimensiones.end", 1.5);
        
        // Anti-farm
        antiFarmEnabled = config.getBoolean("xp_dinamico.anti_farm.enabled", true);
        antiFarmThreshold = config.getInt("xp_dinamico.anti_farm.umbral_acciones", 50);
        antiFarmWindowSeconds = config.getInt("xp_dinamico.anti_farm.ventana_segundos", 60);
        antiFarmPenalty = config.getDouble("xp_dinamico.anti_farm.penalizacion", 0.1);
        
        // ActionBar
        actionBarEnabled = config.getBoolean("xp_dinamico.actionbar.enabled", true);
        
        // Presencia del Streamer
        presenciaStreamerEnabled = config.getBoolean("xp_dinamico.presencia_streamer.enabled", false);
        streamerUsername = config.getString("xp_dinamico.presencia_streamer.streamer_username", "OliveerF");
        multiplicadorOnline = config.getDouble("xp_dinamico.presencia_streamer.multiplicador_online", 1.0);
        multiplicadorOffline = config.getDouble("xp_dinamico.presencia_streamer.multiplicador_offline", 0.2);
        
        // Log de sistema de presencia del streamer
        if (presenciaStreamerEnabled) {
            plugin.getLogger().info("[XP-Streamer] Sistema activado: " + streamerUsername + 
                " | Online=" + multiplicadorOnline + "x | Offline=" + multiplicadorOffline + "x");
        } else {
            plugin.getLogger().warning("[XP-Streamer] Sistema DESACTIVADO en recompensas.yml");
        }
        
        // Hora feliz
        horaFelizMultiplier = config.getDouble("xp_dinamico.hora_feliz.multiplicador", 2.0);
        
        // Cargar horas felices programadas
        horasFelicesProgramadas.clear();
        if (config.contains("xp_dinamico.hora_feliz.programadas")) {
            for (String key : config.getConfigurationSection("xp_dinamico.hora_feliz.programadas").getKeys(false)) {
                String path = "xp_dinamico.hora_feliz.programadas." + key;
                int horaInicio = config.getInt(path + ".hora_inicio", 20);
                int minutoInicio = config.getInt(path + ".minuto_inicio", 0);
                int duracionMinutos = config.getInt(path + ".duracion_minutos", 60);
                List<String> dias = config.getStringList(path + ".dias");
                horasFelicesProgramadas.add(new HoraFelizSchedule(horaInicio, minutoInicio, duracionMinutos, dias));
            }
        }
        
        // Cargar XP base y cooldowns para cada fuente
        baseXPCache.clear();
        cooldownCache.clear();
        
        for (XPSource source : XPSource.values()) {
            int baseXP = config.getInt(source.getConfigPath() + ".xp", getDefaultXP(source));
            int cooldown = config.getInt(source.getConfigPath() + ".cooldown_ms", getDefaultCooldown(source));
            baseXPCache.put(source, baseXP);
            cooldownCache.put(source, cooldown);
        }
        
        plugin.getLogger().info("[XP-Dinámico] Configuración cargada - " + baseXPCache.size() + " fuentes de XP");
        plugin.getLogger().info("[XP-Dinámico] Horas felices programadas: " + horasFelicesProgramadas.size());
    }
    
    /**
     * Inicia el scheduler para verificar horas felices
     */
    private void startHoraFelizScheduler() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkHoraFelizProgramada();
            }
        }.runTaskTimer(plugin, 20L * 60, 20L * 60); // Cada minuto
    }
    
    /**
     * Verifica si el rango es LEYENDA o superior (bloqueo de XP pasiva)
     */
    private boolean isHighRank(me.apocalipsis.missions.MissionRank rank) {
        return rank == me.apocalipsis.missions.MissionRank.LEYENDA ||
               rank == me.apocalipsis.missions.MissionRank.MAESTRO ||
               rank == me.apocalipsis.missions.MissionRank.TITAN ||
               rank == me.apocalipsis.missions.MissionRank.ABSOLUTO;
    }
    
    // Cooldown para notificación de rango alto (evita spam)
    private final Map<UUID, Long> highRankNotificationCooldown = new ConcurrentHashMap<>();
    private static final long HIGH_RANK_NOTIFY_COOLDOWN_MS = 60000; // 1 minuto
    
    /**
     * Notifica al jugador de rango alto que no recibe XP pasiva (con cooldown)
     */
    private void notifyHighRankBlocked(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastNotify = highRankNotificationCooldown.get(uuid);
        
        if (lastNotify != null && (now - lastNotify) < HIGH_RANK_NOTIFY_COOLDOWN_MS) {
            return; // Aún en cooldown
        }
        
        highRankNotificationCooldown.put(uuid, now);
        
        // Notificación sutil via ActionBar
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
            new TextComponent("§8[§c★§8] §7Rango alto: solo misiones dan XP"));
    }
    
    /**
     * Verifica si debe activarse una hora feliz programada
     */
    private void checkHoraFelizProgramada() {
        LocalDateTime now = LocalDateTime.now();
        String diaActual = now.getDayOfWeek().name();
        
        for (HoraFelizSchedule schedule : horasFelicesProgramadas) {
            if (schedule.isActiveNow(now, diaActual)) {
                if (!horaFelizActiva) {
                    activarHoraFeliz(schedule.duracionMinutos, true);
                }
                return;
            }
        }
        
        // Si ninguna hora feliz programada está activa, desactivar (solo si fue programada)
        // Las manuales se mantienen
    }
    
    /**
     * Resetea los combos de un jugador (llamar al desconectarse)
     */
    public void resetPlayerCombos(Player player) {
        PlayerXPTracker tracker = playerTrackers.get(player.getUniqueId());
        if (tracker != null) {
            tracker.resetCombos();
        }
    }
    
    /**
     * Otorga XP dinámico con todos los multiplicadores y bonificaciones
     */
    public XPResult giveXP(Player player, XPSource source, String detail) {
        return giveXP(player, source, detail, 1.0);
    }
    
    /**
     * Otorga XP con multiplicador adicional
     */
    public XPResult giveXP(Player player, XPSource source, String detail, double extraMultiplier) {
        if (!isSourceEnabled(source)) {
            return XPResult.disabled();
        }
        
        // [LEYENDA+] Bloquear XP pasiva para rangos LEYENDA o superiores
        // Solo pueden ganar XP por misiones
        if (source.isPassive()) {
            me.apocalipsis.missions.MissionRank rank = plugin.getRankService().getRank(player);
            if (isHighRank(rank)) {
                // Notificar al jugador (con cooldown para no spamear)
                notifyHighRankBlocked(player);
                return XPResult.blockedByRank();
            }
        }
        
        PlayerXPTracker tracker = getTracker(player);
        
        // Verificar cooldown
        if (!tracker.checkCooldown(source, cooldownCache.getOrDefault(source, 0))) {
            return XPResult.cooldown();
        }
        
        // Anti-farm check
        if (antiFarmEnabled && tracker.isLikelyFarming(source, antiFarmThreshold, antiFarmWindowSeconds)) {
            extraMultiplier *= antiFarmPenalty;
            if (verboseLogging) {
                plugin.getLogger().warning("[XP-AntiF] " + player.getName() + " posible farm detectado en " + source.name());
            }
        }
        
        // Calcular XP base
        int baseXP = baseXPCache.getOrDefault(source, 1);
        
        // Aplicar multiplicadores
        double totalMultiplier = 1.0;
        List<String> appliedBonuses = new ArrayList<>();
        
        // Multiplicador extra pasado como parámetro
        if (extraMultiplier != 1.0) {
            totalMultiplier *= extraMultiplier;
        }
        
        // Multiplicador por dimensión
        double dimMult = getDimensionMultiplier(player.getWorld());
        if (dimMult != 1.0) {
            totalMultiplier *= dimMult;
            String dimName = getDimensionName(player.getWorld());
            appliedBonuses.add("§5+" + dimName + " x" + String.format("%.1f", dimMult));
        }
        
        // Bonus nocturno (in-game)
        if (isNightTime(player) && nightMultiplier > 1.0) {
            totalMultiplier *= nightMultiplier;
            appliedBonuses.add("§9+Noche x" + nightMultiplier);
        }
        
        // Hora feliz
        if (horaFelizActiva) {
            totalMultiplier *= horaFelizMultiplier;
            appliedBonuses.add("§c+HoraFeliz x" + horaFelizMultiplier);
        }
        
        // Bonus de racha diaria
        int streakDays = tracker.getLoginStreak();
        if (streakDays > 1) {
            double streakBonus = 1.0 + (Math.min(streakDays, maxStreakDays) * streakMultiplier);
            totalMultiplier *= streakBonus;
            appliedBonuses.add("§6+Racha x" + String.format("%.1f", streakBonus));
        }
        
        // Combo rápido (con límite máximo, excepto para OliveerF que tiene combo ilimitado)
        int comboCount = tracker.updateCombo(source, comboTimeWindowMs);
        boolean isOwner = player.getName().equalsIgnoreCase("OliveerF");
        int effectiveCombo = isOwner ? comboCount : Math.min(comboCount, comboMaxLimit); // Sin límite para OliveerF
        if (effectiveCombo >= comboMinActions) {
            double comboBonus = 1.0 + ((effectiveCombo - comboMinActions + 1) * (comboMultiplier - 1.0));
            if (!isOwner) {
                comboBonus = Math.min(comboBonus, 2.5); // Cap adicional de seguridad (no aplica a OliveerF)
            }
            totalMultiplier *= comboBonus;
            String maxIndicator = !isOwner && comboCount >= comboMaxLimit ? " §c[MAX]" : (isOwner ? " §d[∞]" : "");
            appliedBonuses.add("§a+Combo x" + effectiveCombo + " (" + String.format("%.1f", comboBonus) + "x)" + maxIndicator);
        }
        
        // Bonus de primer XP del día en esta categoría
        if (tracker.isFirstOfCategory(source.getCategory())) {
            totalMultiplier *= 1.5;
            appliedBonuses.add("§e+Primero del día x1.5");
            tracker.markCategoryUsed(source.getCategory());
        }
        
        // ═══════════════════════════════════════════════════════════════
        // PRESENCIA DEL STREAMER - Se aplica AL FINAL sobre el total
        // Esto asegura que sea una penalización REAL del 80% cuando está offline
        // ═══════════════════════════════════════════════════════════════
        if (presenciaStreamerEnabled) {
            boolean streamerOnline = isStreamerOnline();
            double streamerMult = streamerOnline ? multiplicadorOnline : multiplicadorOffline;
            totalMultiplier *= streamerMult;
            
            if (streamerOnline) {
                if (multiplicadorOnline != 1.0) {
                    appliedBonuses.add("§6+Streamer ON x" + String.format("%.1f", multiplicadorOnline));
                }
            } else {
                appliedBonuses.add("§7-Streamer OFF x" + String.format("%.1f", multiplicadorOffline));
            }
        }
        
        // Calcular XP final
        int finalXP = (int) Math.round(baseXP * totalMultiplier);
        finalXP = Math.max(1, finalXP);
        
        // Otorgar XP
        boolean leveledUp = experienceService.addXP(player, finalXP, source.getDisplayName(), false);
        
        // Actualizar estadísticas del tracker
        tracker.addXPGained(source, finalXP);
        
        // Actualizar leaderboard
        updateLeaderboard(player.getUniqueId(), finalXP);
        
        // Verificar milestones
        checkMilestones(player, tracker, source);
        
        // ActionBar notification
        if (actionBarEnabled) {
            sendActionBar(player, source, finalXP, totalMultiplier, appliedBonuses);
        }
        
        // Logging detallado
        if (verboseLogging) {
            String bonusStr = appliedBonuses.isEmpty() ? "" : " " + String.join(" ", appliedBonuses);
            plugin.getLogger().info(String.format("[XP] %s +%d XP | %s %s | %s%s",
                player.getName(),
                finalXP,
                source.getIcon(),
                source.getDisplayName(),
                detail,
                bonusStr.isEmpty() ? "" : " |" + bonusStr.replace("§", "")
            ));
        }
        
        return new XPResult(true, finalXP, baseXP, totalMultiplier, appliedBonuses, leveledUp);
    }
    
    /**
     * Envía ActionBar con información de XP
     */
    private void sendActionBar(Player player, XPSource source, int xp, double mult, List<String> bonuses) {
        String bonusText = bonuses.isEmpty() ? "" : " §7" + String.join(" ", bonuses);
        String message = String.format("§a+%d XP §7%s %s%s", 
            xp, source.getIcon(), source.getDisplayName(), bonusText);
        
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }
    
    /**
     * Obtiene multiplicador por dimensión
     */
    private double getDimensionMultiplier(World world) {
        return switch (world.getEnvironment()) {
            case NETHER -> netherMultiplier;
            case THE_END -> endMultiplier;
            default -> overworldMultiplier;
        };
    }
    
    private String getDimensionName(World world) {
        return switch (world.getEnvironment()) {
            case NETHER -> "Nether";
            case THE_END -> "End";
            default -> "Overworld";
        };
    }
    
    /**
     * Obtiene el tracker de un jugador
     */
    private PlayerXPTracker getTracker(Player player) {
        return playerTrackers.computeIfAbsent(player.getUniqueId(), 
            uuid -> new PlayerXPTracker(uuid));
    }
    
    /**
     * Actualiza el leaderboard diario
     */
    private void updateLeaderboard(UUID playerId, int xp) {
        // Resetear si es nuevo día
        if (!leaderboardDate.equals(LocalDate.now())) {
            dailyLeaderboard.clear();
            leaderboardDate = LocalDate.now();
        }
        dailyLeaderboard.merge(playerId, xp, Integer::sum);
    }
    
    /**
     * Obtiene el top jugadores del día
     */
    public List<Map.Entry<String, Integer>> getTopPlayers(int limit) {
        // Resetear si es nuevo día
        if (!leaderboardDate.equals(LocalDate.now())) {
            dailyLeaderboard.clear();
            leaderboardDate = LocalDate.now();
        }
        
        return dailyLeaderboard.entrySet().stream()
            .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
            .limit(limit)
            .map(e -> {
                String name = Bukkit.getOfflinePlayer(e.getKey()).getName();
                return Map.entry(name != null ? name : "Desconocido", e.getValue());
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Verifica y otorga milestones
     */
    private void checkMilestones(Player player, PlayerXPTracker tracker, XPSource source) {
        FileConfiguration config = plugin.getConfigManager().getRecompensasConfig();
        if (!config.getBoolean("xp_dinamico.milestones.enabled", true)) return;
        
        String category = source.getCategory();
        int actionsInCategory = tracker.getCategoryActions(category);
        
        // Milestones por categoría
        int[] milestones = {50, 100, 250, 500, 1000, 2500, 5000};
        int[] rewards = {25, 50, 100, 200, 500, 1000, 2000};
        
        for (int i = 0; i < milestones.length; i++) {
            String milestoneKey = category + "_" + milestones[i];
            if (actionsInCategory == milestones[i] && !tracker.hasMilestone(milestoneKey)) {
                tracker.markMilestone(milestoneKey);
                int bonus = rewards[i];
                
                // Otorgar bonus
                experienceService.addXP(player, bonus, "Milestone " + category, false);
                
                // Notificar
                player.sendMessage("");
                player.sendMessage("§6§l⭐ ¡MILESTONE ALCANZADO! ⭐");
                player.sendMessage("§7" + category + ": §e" + milestones[i] + " acciones");
                player.sendMessage("§a+" + bonus + " XP de bonus!");
                player.sendMessage("");
                
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                
                if (verboseLogging) {
                    plugin.getLogger().info("[XP-Milestone] " + player.getName() + " alcanzó " + milestoneKey + " (+" + bonus + " XP)");
                }
            }
        }
    }
    
    /**
     * Activa hora feliz manualmente o programada
     */
    public void activarHoraFeliz(int duracionMinutos, boolean programada) {
        if (horaFelizActiva) return;
        
        horaFelizActiva = true;
        
        // Anunciar a todos
        String tipo = programada ? "programada" : "activada por admin";
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§c§l🎉 ¡HORA FELIZ ACTIVADA! 🎉");
        Bukkit.broadcastMessage("§7Todo el XP tiene §ex" + horaFelizMultiplier + " §7durante §e" + duracionMinutos + " minutos!");
        Bukkit.broadcastMessage("§8(" + tipo + ")");
        Bukkit.broadcastMessage("");
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        }
        
        plugin.getLogger().info("[XP-HoraFeliz] Activada por " + duracionMinutos + " minutos (" + tipo + ")");
        
        // Programar desactivación
        if (horaFelizTask != null) {
            horaFelizTask.cancel();
        }
        
        horaFelizTask = new BukkitRunnable() {
            @Override
            public void run() {
                desactivarHoraFeliz();
            }
        }.runTaskLater(plugin, 20L * 60 * duracionMinutos);
    }
    
    /**
     * Desactiva hora feliz
     */
    public void desactivarHoraFeliz() {
        if (!horaFelizActiva) return;
        
        horaFelizActiva = false;
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7La §c§lHora Feliz §7ha terminado. ¡Gracias por participar!");
        Bukkit.broadcastMessage("");
        
        plugin.getLogger().info("[XP-HoraFeliz] Desactivada");
    }
    
    public boolean isHoraFelizActiva() {
        return horaFelizActiva;
    }
    
    /**
     * Actualiza la racha de login de un jugador
     */
    public void updateLoginStreak(Player player) {
        PlayerXPTracker tracker = getTracker(player);
        tracker.updateLoginStreak();
        
        int streak = tracker.getLoginStreak();
        if (streak > 1) {
            player.sendMessage("§6§l🔥 ¡Racha de " + streak + " días! §7(+" + 
                (int)(streak * streakMultiplier * 100) + "% XP bonus)");
        }
    }
    
    /**
     * Resetea la racha si el jugador no jugó ayer
     */
    public void checkStreakReset(Player player) {
        getTracker(player).checkStreakReset();
    }
    
    /**
     * Verifica si es fin de semana
     */
    private boolean isWeekend() {
        DayOfWeek day = LocalDate.now().getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
    
    /**
     * Verifica si es de noche en el mundo del jugador
     */
    private boolean isNightTime(Player player) {
        long time = player.getWorld().getTime();
        return time >= 13000 && time <= 23000;
    }
    
    /**
     * Verifica si una fuente está habilitada
     */
    private boolean isSourceEnabled(XPSource source) {
        return plugin.getConfigManager().getRecompensasConfig()
            .getBoolean(source.getConfigPath() + ".enabled", true);
    }
    
    /**
     * Obtiene XP por defecto para una fuente
     */
    private int getDefaultXP(XPSource source) {
        return switch (source) {
            case KILL_BOSS -> 100;
            case KILL_HOSTILE -> 2;
            case KILL_PASSIVE -> 1;
            case KILL_PLAYER -> 50;
            case MINE_EPIC -> 10;
            case MINE_RARE -> 5;
            case MINE_COMMON -> 1;
            case HARVEST -> 1;
            case FISH -> 3;
            case FISH_TREASURE -> 10;
            case SHEAR -> 1;
            case TAME -> 8;
            case BREED -> 3;
            case CRAFT_EPIC -> 15;
            case CRAFT_RARE -> 5;
            case CRAFT_COMMON -> 1;
            case ENCHANT -> 5;
            case SMITH -> 20;
            case BREW -> 3;
            case TRADE -> 3;
            case TRADE_RARE -> 8;
            case SMELT -> 1;
            case ADVANCEMENT -> 15;
            case ADVANCEMENT_RARE -> 30;
            case ADVANCEMENT_EPIC -> 75;
            case BIOME_DISCOVER -> 5;
            case STRUCTURE_DISCOVER -> 15;
            case PLACE_SPECIAL -> 10;
            case CONSUME_SPECIAL -> 3;
            case MISSION_EASY -> 50;
            case MISSION_MEDIUM -> 100;
            case MISSION_HARD -> 200;
            case EVENT_PARTICIPATION -> 25;
            case EVENT_WIN -> 100;
            case STREAK_BONUS -> 50;
            case FIRST_OF_DAY -> 10;
            case WEEKEND_BONUS -> 20;
        };
    }
    
    /**
     * Obtiene cooldown por defecto para una fuente (en ms)
     */
    private int getDefaultCooldown(XPSource source) {
        return switch (source) {
            case KILL_HOSTILE, KILL_PASSIVE -> 0;
            case KILL_BOSS, KILL_PLAYER -> 0;
            case MINE_COMMON, MINE_RARE, MINE_EPIC -> 500;
            case HARVEST -> 1000;
            case FISH, FISH_TREASURE -> 0;
            case SHEAR -> 2000;
            case TAME, BREED -> 5000;
            case CRAFT_COMMON, CRAFT_RARE, CRAFT_EPIC -> 3000;
            case ENCHANT, SMITH, BREW -> 0;
            case TRADE, TRADE_RARE -> 2000;
            case SMELT -> 3000;
            case ADVANCEMENT, ADVANCEMENT_RARE, ADVANCEMENT_EPIC -> 0;
            case BIOME_DISCOVER, STRUCTURE_DISCOVER -> 60000;
            case PLACE_SPECIAL -> 30000;
            case CONSUME_SPECIAL -> 0;
            default -> 0;
        };
    }
    
    /**
     * Obtiene estadísticas de un jugador
     */
    public Map<String, Object> getPlayerStats(Player player) {
        PlayerXPTracker tracker = getTracker(player);
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("streak", tracker.getLoginStreak());
        stats.put("xp_hoy", tracker.getTodayXP());
        stats.put("acciones_hoy", tracker.getTodayActions());
        stats.put("mejor_fuente", tracker.getTopSource());
        stats.put("multiplicador_actual", calculateCurrentMultiplier(player));
        
        return stats;
    }
    
    /**
     * Calcula el multiplicador actual para un jugador
     */
    public double calculateCurrentMultiplier(Player player) {
        double mult = 1.0;
        
        if (isWeekend()) mult *= weekendMultiplier;
        if (isNightTime(player)) mult *= nightMultiplier;
        
        int streak = getTracker(player).getLoginStreak();
        if (streak > 1) {
            mult *= (1.0 + Math.min(streak, maxStreakDays) * streakMultiplier);
        }
        
        return mult;
    }
    
    /**
     * Limpia datos de jugadores offline
     */
    public void cleanupOfflinePlayers() {
        playerTrackers.entrySet().removeIf(entry -> 
            Bukkit.getPlayer(entry.getKey()) == null);
    }
    
    /**
     * Verifica si el streamer está online (en tiempo real)
     */
    private boolean isStreamerOnline() {
        Player streamer = Bukkit.getPlayerExact(streamerUsername);
        return streamer != null && streamer.isOnline();
    }
    
    /**
     * Guarda datos persistentes
     */
    public void saveData() {
        // Los datos de racha se guardan en el tracker del jugador
        // que persiste en experience_data.yml
    }
    
    /**
     * Apaga el sistema limpiamente
     */
    public void shutdown() {
        if (horaFelizTask != null) {
            horaFelizTask.cancel();
        }
        playerTrackers.clear();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CLASE INTERNA: Tracker por jugador
    // ═══════════════════════════════════════════════════════════════
    
    private static class PlayerXPTracker {
        private final UUID playerId;
        private final Map<XPSource, Long> cooldowns = new EnumMap<>(XPSource.class);
        private final Map<XPSource, Integer> xpGained = new EnumMap<>(XPSource.class);
        private final Map<String, Integer> categoryActions = new HashMap<>();
        private final Set<String> usedCategoriesToday = new HashSet<>();
        private final Set<String> achievedMilestones = new HashSet<>();
        
        // Anti-farm tracking
        private final Map<XPSource, List<Long>> recentActions = new EnumMap<>(XPSource.class);
        
        // Combo tracking (separado por fuente)
        private final Map<XPSource, Long> lastActionTimeBySource = new EnumMap<>(XPSource.class);
        private final Map<XPSource, Integer> comboCountBySource = new EnumMap<>(XPSource.class);
        private XPSource lastComboSource = null;
        
        private int loginStreak = 1;
        private LocalDate lastLoginDate = LocalDate.now();
        private LocalDate lastXPDate = LocalDate.now();
        private int todayXP = 0;
        private int todayActions = 0;
        
        public PlayerXPTracker(UUID playerId) {
            this.playerId = playerId;
        }
        
        public boolean checkCooldown(XPSource source, int cooldownMs) {
            if (cooldownMs <= 0) return true;
            
            long now = System.currentTimeMillis();
            Long last = cooldowns.get(source);
            
            if (last != null && (now - last) < cooldownMs) {
                return false;
            }
            
            cooldowns.put(source, now);
            return true;
        }
        
        /**
         * Actualiza combo y retorna el contador actual
         * El combo solo se mantiene si es la MISMA fuente de XP
         */
        public int updateCombo(XPSource source, int windowMs) {
            long now = System.currentTimeMillis();
            Long lastTime = lastActionTimeBySource.get(source);
            int currentCombo = comboCountBySource.getOrDefault(source, 0);
            
            // Si es una fuente diferente a la última, resetear combos de otras fuentes
            if (lastComboSource != null && lastComboSource != source) {
                // Resetear combo de la fuente anterior
                comboCountBySource.put(lastComboSource, 0);
            }
            
            // Verificar si está dentro de la ventana de tiempo
            if (lastTime != null && (now - lastTime) <= windowMs) {
                currentCombo++;
            } else {
                currentCombo = 1; // Primera acción o se acabó el tiempo
            }
            
            lastActionTimeBySource.put(source, now);
            comboCountBySource.put(source, currentCombo);
            lastComboSource = source;
            
            return currentCombo;
        }
        
        /**
         * Resetea todos los combos (llamar al desconectarse)
         */
        public void resetCombos() {
            comboCountBySource.clear();
            lastActionTimeBySource.clear();
            lastComboSource = null;
        }
        
        /**
         * Detecta posible farming
         */
        public boolean isLikelyFarming(XPSource source, int threshold, int windowSeconds) {
            long now = System.currentTimeMillis();
            long windowMs = windowSeconds * 1000L;
            
            List<Long> actions = recentActions.computeIfAbsent(source, k -> new ArrayList<>());
            
            // Limpiar acciones antiguas
            actions.removeIf(time -> now - time > windowMs);
            
            // Agregar acción actual
            actions.add(now);
            
            return actions.size() >= threshold;
        }
        
        public void addXPGained(XPSource source, int xp) {
            xpGained.merge(source, xp, Integer::sum);
            categoryActions.merge(source.getCategory(), 1, Integer::sum);
            
            // Resetear stats diarias si es nuevo día
            if (!lastXPDate.equals(LocalDate.now())) {
                lastXPDate = LocalDate.now();
                todayXP = 0;
                todayActions = 0;
                usedCategoriesToday.clear();
            }
            
            todayXP += xp;
            todayActions++;
        }
        
        public int getCategoryActions(String category) {
            return categoryActions.getOrDefault(category, 0);
        }
        
        public boolean hasMilestone(String key) {
            return achievedMilestones.contains(key);
        }
        
        public void markMilestone(String key) {
            achievedMilestones.add(key);
        }
        
        public void updateLoginStreak() {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            
            if (lastLoginDate.equals(yesterday)) {
                loginStreak++;
            } else if (!lastLoginDate.equals(today)) {
                loginStreak = 1;
            }
            
            lastLoginDate = today;
        }
        
        public void checkStreakReset() {
            LocalDate today = LocalDate.now();
            if (lastLoginDate.isBefore(today.minusDays(1))) {
                loginStreak = 1;
            }
        }
        
        public boolean isFirstOfCategory(String category) {
            return !usedCategoriesToday.contains(category);
        }
        
        public void markCategoryUsed(String category) {
            usedCategoriesToday.add(category);
        }
        
        public int getLoginStreak() {
            return loginStreak;
        }
        
        public int getTodayXP() {
            return todayXP;
        }
        
        public int getTodayActions() {
            return todayActions;
        }
        
        public String getTopSource() {
            return xpGained.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().getDisplayName())
                .orElse("Ninguna");
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CLASE: Hora Feliz Programada
    // ═══════════════════════════════════════════════════════════════
    
    private static class HoraFelizSchedule {
        final int horaInicio;
        final int minutoInicio;
        final int duracionMinutos;
        final List<String> dias;
        
        HoraFelizSchedule(int horaInicio, int minutoInicio, int duracionMinutos, List<String> dias) {
            this.horaInicio = horaInicio;
            this.minutoInicio = minutoInicio;
            this.duracionMinutos = duracionMinutos;
            this.dias = dias.stream().map(String::toUpperCase).collect(Collectors.toList());
        }
        
        boolean isActiveNow(LocalDateTime now, String diaActual) {
            if (!dias.isEmpty() && !dias.contains(diaActual.toUpperCase())) {
                return false;
            }
            
            LocalTime inicio = LocalTime.of(horaInicio, minutoInicio);
            LocalTime fin = inicio.plusMinutes(duracionMinutos);
            LocalTime ahora = now.toLocalTime();
            
            return !ahora.isBefore(inicio) && ahora.isBefore(fin);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CLASE: Resultado de XP
    // ═══════════════════════════════════════════════════════════════
    
    public record XPResult(
        boolean success,
        int finalXP,
        int baseXP,
        double multiplier,
        List<String> bonuses,
        boolean leveledUp
    ) {
        public static XPResult disabled() {
            return new XPResult(false, 0, 0, 0, List.of(), false);
        }
        
        public static XPResult cooldown() {
            return new XPResult(false, 0, 0, 0, List.of(), false);
        }
        
        public static XPResult blockedByRank() {
            return new XPResult(false, 0, 0, 0, List.of(), false);
        }
    }
}
