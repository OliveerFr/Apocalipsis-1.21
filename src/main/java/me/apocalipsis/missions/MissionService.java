package me.apocalipsis.missions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.ui.MessageBus;

public class MissionService {

    private final Apocalipsis plugin;
    private final MessageBus messageBus;
    private final File dataFile;
    
    private final List<MissionCatalog> catalog = new ArrayList<>();
    private final Map<UUID, List<MissionAssignment>> playerAssignments = new HashMap<>();
    private final Map<UUID, Integer> playerPs = new HashMap<>(); // Puntos de supervivencia

    private int maxPorDia = 5;
    private final Map<MissionRank, Integer> porRango = new HashMap<>();
    private final Map<MissionDifficulty, Integer> pesosPorDificultad = new HashMap<>();
    
    // [1.21.8] Control de celebración por jugador (evita retrigger)
    private final Set<UUID> playerDailyCompleteFired = new HashSet<>();
    
    // [ALTURA] Variables mantenidas por compatibilidad (tipo deshabilitado pero código residual aún referenciado)
    private final Map<UUID, Integer> heightSeconds = new HashMap<>();
    private int heightTaskId = -1;
    private boolean debugExplore = false;

    public MissionService(Apocalipsis plugin, MessageBus messageBus) {
        this.plugin = plugin;
        this.messageBus = messageBus;
        this.dataFile = new File(plugin.getDataFolder(), "mission_data.yml");
        loadCatalog();
        loadPlayerData();
        
        // [REMOVAL] Schedulers de EXPLORAR y ALTURA deshabilitados (tipos removidos)
        // startExploreTracker();
        // startHeightTracker();
        // startDebugExploreTracker();
    }

    private void loadCatalog() {
        catalog.clear();
        FileConfiguration config = plugin.getConfigManager().getMisionesConfig();
        
        List<Map<?, ?>> misionList = config.getMapList("misiones");
        for (Map<?, ?> misionMap : misionList) {
            try {
                String id = (String) misionMap.get("id");
                String nombre = (String) misionMap.get("nombre");
                MissionType tipo = MissionType.valueOf((String) misionMap.get("tipo"));
                String objetivo = (String) misionMap.get("objetivo");
                int cantidad = (int) misionMap.get("cantidad");
                MissionDifficulty dificultad = MissionDifficulty.valueOf((String) misionMap.get("dificultad"));
                
                @SuppressWarnings("unchecked")
                List<String> rangosStr = (List<String>) misionMap.get("rangos");
                List<MissionRank> rangos = rangosStr.stream()
                    .map(MissionRank::valueOf)
                    .collect(Collectors.toList());
                
                int recompensaPs = (int) misionMap.get("recompensa_ps");
                
                // [REMOVAL] Filtrar tipos deshabilitados (EXPLORAR, ALTURA)
                if (!tipo.isEnabled()) {
                    plugin.getLogger().info("[MISIONES] Omitiendo misión '" + id + "' (tipo " + tipo + " deshabilitado)");
                    continue;
                }
                
                catalog.add(new MissionCatalog(id, nombre, tipo, objetivo, cantidad, dificultad, rangos, recompensaPs));
            } catch (Exception e) {
                plugin.getLogger().warning("Error cargando misión: " + e.getMessage());
            }
        }

        // Cargar configuración
        ConfigurationSection cfg = config.getConfigurationSection("config");
        if (cfg != null) {
            maxPorDia = cfg.getInt("max_por_dia", 5);
            
            ConfigurationSection porRangoCfg = cfg.getConfigurationSection("por_rango");
            if (porRangoCfg != null) {
                for (String key : porRangoCfg.getKeys(false)) {
                    try {
                        MissionRank rank = MissionRank.valueOf(key.toUpperCase());
                        porRango.put(rank, porRangoCfg.getInt(key));
                    } catch (Exception ignored) {}
                }
            }

            ConfigurationSection pesosCfg = cfg.getConfigurationSection("pesos_por_dificultad");
            if (pesosCfg != null) {
                for (String key : pesosCfg.getKeys(false)) {
                    try {
                        MissionDifficulty diff = MissionDifficulty.valueOf(key.toUpperCase());
                        pesosPorDificultad.put(diff, pesosCfg.getInt(key));
                    } catch (Exception ignored) {}
                }
            }
        }

        plugin.getLogger().info("Cargadas " + catalog.size() + " misiones del catálogo");
    }

    /**
     * Asigna misiones para un nuevo día a todos los jugadores online.
     * IMPORTANTE: Limpia las misiones del día anterior antes de asignar nuevas.
     * Este método se llama desde /avo newday
     */
    public void assignMissionsForDay(int day) {
        // [FIX] Limpiar todas las misiones del día anterior
        playerAssignments.clear();
        
        // Asignar nuevas misiones a todos los jugadores online
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            assignMissionsToPlayer(player);
        }
        
        savePlayerData();
        
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[MISIONES] Nuevo día " + day + " iniciado. Misiones anteriores limpiadas, nuevas asignadas a " + plugin.getServer().getOnlinePlayers().size() + " jugadores.");
        }
    }

    public void assignMissionsToPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        
        // [FIX] Si el jugador ya tiene misiones asignadas, no reasignar
        List<MissionAssignment> existing = playerAssignments.get(uuid);
        if (existing != null && !existing.isEmpty()) {
            // Ya tiene misiones, no asignar nuevas
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[MISIONES] Jugador " + player.getName() + " ya tiene " + existing.size() + " misiones, no se reasignan");
            }
            return;
        }
        
        int ps = playerPs.getOrDefault(uuid, 0);
        MissionRank rank = MissionRank.fromPs(ps);
        
        // [RANGOS.YML] Usar misionesDiarias del rango configurado
        int maxMissions = rank.getMisionesDiarias();
        if (maxMissions <= 0) {
            // Fallback a porRango si no está configurado
            maxMissions = porRango.getOrDefault(rank, 3);
        }
        
        // [CONFIG] Limitar al máximo global configurado en misiones_new.yml
        if (maxMissions > maxPorDia) {
            plugin.getLogger().warning("[MISIONES] Rango " + rank + " intenta asignar " + maxMissions + " misiones, limitando a max_por_dia=" + maxPorDia);
            maxMissions = maxPorDia;
        }
        
        // Filtrar misiones elegibles (y tipos habilitados)
        List<MissionCatalog> eligible = catalog.stream()
            .filter(m -> m.isValidForRank(rank))
            .filter(m -> m.getTipo().isEnabled())  // [REMOVAL] Excluir tipos deshabilitados
            .collect(Collectors.toList());
        
        if (eligible.isEmpty()) {
            plugin.getLogger().warning("No hay misiones elegibles para rango " + rank);
            return;
        }

        // Seleccionar misiones con peso por dificultad
        List<MissionCatalog> selected = selectWeightedMissions(eligible, maxMissions);
        
        List<MissionAssignment> assignments = selected.stream()
            .map(MissionAssignment::new)
            .collect(Collectors.toList());
        
        playerAssignments.put(uuid, assignments);
        savePlayerData();
        
        // [LATE-JOIN] Mensaje específico para jugadores que reciben misiones
        messageBus.sendMessage(player, "§e✓ Se te han asignado §f" + assignments.size() + " §emisiones para hoy.");
        
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[MISIONES] Late-join: Asignadas " + assignments.size() + " misiones a " + player.getName() + " (Rango: " + rank + ")");
        }
    }

    private List<MissionCatalog> selectWeightedMissions(List<MissionCatalog> pool, int count) {
        List<MissionCatalog> selected = new ArrayList<>();
        List<MissionCatalog> available = new ArrayList<>(pool);
        Random random = new Random();
        
        for (int i = 0; i < count && !available.isEmpty(); i++) {
            int totalWeight = available.stream()
                .mapToInt(m -> pesosPorDificultad.getOrDefault(m.getDificultad(), 1))
                .sum();
            
            int roll = random.nextInt(totalWeight);
            int accum = 0;
            
            MissionCatalog chosen = null;
            for (MissionCatalog m : available) {
                accum += pesosPorDificultad.getOrDefault(m.getDificultad(), 1);
                if (roll < accum) {
                    chosen = m;
                    break;
                }
            }
            
            if (chosen != null) {
                selected.add(chosen);
                available.remove(chosen);
            }
        }
        
        return selected;
    }

    public void progressMission(Player player, MissionType type, String target, int amount) {
        // [REMOVAL] No-op para tipos deshabilitados
        if (!type.isEnabled()) {
            return;
        }
        
        UUID uuid = player.getUniqueId();
        List<MissionAssignment> assignments = playerAssignments.get(uuid);
        if (assignments == null) return;

        boolean anyProgress = false;
        for (MissionAssignment assignment : assignments) {
            if (assignment.isCompleted() || assignment.isFailed()) continue;
            
            MissionCatalog mission = assignment.getMission();
            if (mission.getTipo() == type && mission.getObjetivo().equalsIgnoreCase(target)) {
                int oldProgress = assignment.getProgress();
                assignment.addProgress(amount);
                
                if (assignment.getProgress() > oldProgress) {
                    anyProgress = true;
                    
                    // **EFECTOS DE PROGRESO MEJORADOS**
                    int progress = assignment.getProgress();
                    int targetAmount = mission.getCantidad();
                    double percentage = (double) progress / targetAmount;
                    
                    // Barra de progreso visual
                    String progressBar = createProgressBar(progress, targetAmount, 10, '█', '▒');
                    
                    // Mensaje de progreso mejorado
                    messageBus.sendActionBar(player, 
                        "§e" + mission.getNombre() + " §7[" + progressBar + "§7] §f" + progress + "/" + targetAmount);
                    
                    // Sonidos y partículas sutiles en hitos (25%, 50%, 75%)
                    if (percentage >= 0.25 && percentage < 0.30 && oldProgress < targetAmount * 0.25) {
                        // 25%
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.0f);
                        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1.5, 0), 5, 0.3, 0.3, 0.3, 0.02);
                    } else if (percentage >= 0.50 && percentage < 0.55 && oldProgress < targetAmount * 0.50) {
                        // 50%
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.2f);
                        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1.5, 0), 8, 0.4, 0.4, 0.4, 0.03);
                        messageBus.sendMessage(player, "§e⚡ ¡Mitad del camino en " + mission.getNombre() + "!");
                    } else if (percentage >= 0.75 && percentage < 0.80 && oldProgress < targetAmount * 0.75) {
                        // 75%
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.4f);
                        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1.5, 0), 10, 0.5, 0.5, 0.5, 0.04);
                        player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 1.5, 0), 3, 0.3, 0.3, 0.3, 0.02);
                        messageBus.sendMessage(player, "§e⚡ ¡Casi terminas " + mission.getNombre() + "!");
                    }
                    
                    // Si se completó
                    if (assignment.isCompleted()) {
                        rewardPlayer(player, mission);
                    }
                }
            }
        }

        if (anyProgress) {
            savePlayerData();
        }
    }

    private void rewardPlayer(Player player, MissionCatalog mission) {
        UUID uuid = player.getUniqueId();
        int currentPs = playerPs.getOrDefault(uuid, 0);
        int newPs = currentPs + mission.getRecompensaPs();
        
        // [DATA.YML] Detectar rank up
        me.apocalipsis.missions.MissionRank oldRank = me.apocalipsis.missions.MissionRank.fromPs(currentPs);
        me.apocalipsis.missions.MissionRank newRank = me.apocalipsis.missions.MissionRank.fromPs(newPs);
        
        playerPs.put(uuid, newPs);
        
        // **EFECTOS MEJORADOS POR DIFICULTAD**
        playMissionCompleteEffects(player, mission);
        
        messageBus.sendMessage(player, "§a§l✓ Misión completada: §f" + mission.getNombre() + " §7(§e+" + mission.getRecompensaPs() + " PS§7)");
        savePlayerData();
        
        // [DATA.YML] Hooks - TODO: Implementar sistema completo de data.yml
        // plugin.getConfigManager().onMissionCompleted(uuid);
        // plugin.getConfigManager().onPsChange(uuid, currentPs, newPs);
        
        // Si hubo rank up, registrarlo
        if (oldRank != newRank) {
            // TODO: Implementar onRankUp en ConfigManager
            // plugin.getConfigManager().onRankUp(uuid, newRank.name(), newRank.getDisplayName());
            playRankUpEffects(player, newRank);
        }
        
        // [FIX] Actualizar scoreboard y tablist inmediatamente para reflejar cambio de PS/rango
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updatePlayer(player);
        }
        if (plugin.getTablistManager() != null) {
            plugin.getTablistManager().updatePlayer(player);
        }
        
        // [1.21.8] Chequeo: ¿Este jugador completó TODAS sus misiones del día?
        if (!playerDailyCompleteFired.contains(uuid) && areAllDailyMissionsCompletedFor(uuid)) {
            playerDailyCompleteFired.add(uuid);
            triggerPlayerDailyCompletionCelebration(player);
        }
    }
    
    /**
     * [NUEVO] Efectos mejorados al completar misión (según dificultad)
     */
    private void playMissionCompleteEffects(Player player, MissionCatalog mission) {
        Location loc = player.getLocation().add(0, 1.5, 0);
        
        // Sonidos y partículas según dificultad
        switch (mission.getDificultad()) {
            case FACIL:
                player.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 10, 0.5, 0.5, 0.5, 0.02);
                break;
            case MEDIA:
                player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.3f);
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 15, 0.6, 0.6, 0.6, 0.03);
                player.getWorld().spawnParticle(Particle.FIREWORK, loc, 5, 0.3, 0.3, 0.3, 0.05);
                break;
            case DIFICIL:
                player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 20, 0.7, 0.7, 0.7, 0.04);
                player.getWorld().spawnParticle(Particle.FIREWORK, loc, 10, 0.4, 0.4, 0.4, 0.08);
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 5, 0.3, 0.3, 0.3, 0.05);
                
                // Fuego artificial para misiones difíciles
                Firework fw = (Firework) player.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.setPower(1);
                meta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.BURST)
                    .withColor(org.bukkit.Color.YELLOW, org.bukkit.Color.ORANGE)
                    .withFade(org.bukkit.Color.WHITE)
                    .withTrail()
                    .withFlicker()
                    .build());
                fw.setFireworkMeta(meta);
                break;
        }
        
        // ActionBar con progreso visual
        int completed = getCompletedCount(player);
        int total = getActiveAssignments(player).size();
        String progressBar = createProgressBar(completed, total, 10, '█', '▒');
        messageBus.sendActionBar(player, "§e" + mission.getNombre() + " §a✓ §7[" + progressBar + "§7] §f" + completed + "/" + total);
    }
    
    /**
     * [NUEVO] Efectos de rank up con colores de rango
     */
    private void playRankUpEffects(Player player, me.apocalipsis.missions.MissionRank rank) {
        Location loc = player.getLocation().add(0, 1.5, 0);
        
        // Colores por rango
        org.bukkit.Color primary, secondary;
        FireworkEffect.Type type;
        
        switch (rank) {
            case LEYENDA:
                primary = org.bukkit.Color.RED;
                secondary = org.bukkit.Color.ORANGE;
                type = FireworkEffect.Type.STAR;
                break;
            case VETERANO:
                primary = org.bukkit.Color.ORANGE;
                secondary = org.bukkit.Color.YELLOW;
                type = FireworkEffect.Type.BALL_LARGE;
                break;
            case SOBREVIVIENTE:
                primary = org.bukkit.Color.YELLOW;
                secondary = org.bukkit.Color.WHITE;
                type = FireworkEffect.Type.BALL;
                break;
            case EXPLORADOR:
                primary = org.bukkit.Color.AQUA;
                secondary = org.bukkit.Color.BLUE;
                type = FireworkEffect.Type.BURST;
                break;
            default: // NOVATO
                primary = org.bukkit.Color.LIME;
                secondary = org.bukkit.Color.GREEN;
                type = FireworkEffect.Type.BALL;
                break;
        }
        
        // Sonidos épicos
        player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 1.0f);
        player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.2f, 0.8f);
        
        // Partículas múltiples
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 40, 1.0, 1.0, 1.0, 0.1);
        player.getWorld().spawnParticle(Particle.FIREWORK, loc, 30, 0.8, 0.8, 0.8, 0.15);
        player.getWorld().spawnParticle(Particle.END_ROD, loc, 20, 0.6, 0.6, 0.6, 0.08);
        
        // Fuegos artificiales con colores del rango
        for (int i = 0; i < 3; i++) {
            Firework fw = (Firework) player.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
            FireworkMeta meta = fw.getFireworkMeta();
            meta.setPower(1);
            meta.addEffect(FireworkEffect.builder()
                .with(type)
                .withColor(primary)
                .withFade(secondary)
                .withTrail()
                .withFlicker()
                .build());
            fw.setFireworkMeta(meta);
        }
        
        // Título animado
        String rankName = rank.getDisplayName();
        player.showTitle(net.kyori.adventure.title.Title.title(
            net.kyori.adventure.text.Component.text("§6§l¡NUEVO RANGO!"),
            net.kyori.adventure.text.Component.text("§f" + rankName),
            net.kyori.adventure.title.Title.Times.times(
                java.time.Duration.ofMillis(500),
                java.time.Duration.ofMillis(3000),
                java.time.Duration.ofMillis(1000)
            )
        ));
        
        // Mensaje público
        Bukkit.getServer().broadcast(
            net.kyori.adventure.text.Component.text("§6§l★ " + player.getName() + " §eha alcanzado el rango " + rankName + "§6§l ★")
        );
    }
    
    /**
     * [NUEVO] Crea barra de progreso visual
     */
    private String createProgressBar(int current, int max, int barLength, char filled, char empty) {
        if (max <= 0) return "§a" + String.valueOf(filled).repeat(barLength);
        
        int filledBars = (int) ((double) current / max * barLength);
        int emptyBars = barLength - filledBars;
        
        return "§a" + String.valueOf(filled).repeat(Math.max(0, filledBars)) + 
               "§7" + String.valueOf(empty).repeat(Math.max(0, emptyBars));
    }

    /**
     * Finaliza el día actual marcando todas las misiones no completadas como fallidas.
     * Este método se llama desde /avo endday
     */
    public void endDay() {
        int totalFailed = 0;
        int totalCompleted = 0;
        
        for (UUID uuid : playerAssignments.keySet()) {
            List<MissionAssignment> assignments = playerAssignments.get(uuid);
            for (MissionAssignment assignment : assignments) {
                if (assignment.isCompleted()) {
                    totalCompleted++;
                } else if (!assignment.isFailed()) {
                    assignment.setFailed(true);
                    totalFailed++;
                }
            }
        }
        
        savePlayerData();
        
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[MISIONES] Día finalizado. Completadas: " + totalCompleted + ", Fallidas: " + totalFailed);
        }
    }

    public List<MissionAssignment> getActiveAssignments(Player player) {
        return playerAssignments.getOrDefault(player.getUniqueId(), Collections.emptyList());
    }

    public int getCompletedCount(Player player) {
        return (int) getActiveAssignments(player).stream()
            .filter(MissionAssignment::isCompleted)
            .count();
    }

    public int getPlayerPs(Player player) {
        return playerPs.getOrDefault(player.getUniqueId(), 0);
    }

    private void loadPlayerData() {
        if (!dataFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        
        ConfigurationSection playersSection = config.getConfigurationSection("players");
        if (playersSection == null) return;

        for (String uuidStr : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidStr);
                
                int ps = playerSection.getInt("ps", 0);
                playerPs.put(uuid, ps);
                
                List<MissionAssignment> assignments = new ArrayList<>();
                List<Map<?, ?>> assignmentsList = playerSection.getMapList("assignments");
                
                for (Map<?, ?> assignmentMap : assignmentsList) {
                    String missionId = (String) assignmentMap.get("mission_id");
                    int progress = (int) assignmentMap.get("progress");
                    boolean completed = (boolean) assignmentMap.get("completed");
                    boolean failed = (boolean) assignmentMap.get("failed");
                    
                    MissionCatalog mission = catalog.stream()
                        .filter(m -> m.getId().equals(missionId))
                        .findFirst()
                        .orElse(null);
                    
                    if (mission != null) {
                        // [REMOVAL] Si es tipo deshabilitado, marcar como completada sin PS
                        if (!mission.getTipo().isEnabled()) {
                            MissionAssignment assignment = new MissionAssignment(mission);
                            assignment.setProgress(mission.getCantidad());
                            assignment.setCompleted(true);
                            assignment.setFailed(false);
                            assignments.add(assignment);
                            plugin.getLogger().info("[MISIONES] Misión '" + missionId + "' (tipo " + mission.getTipo() + ") marcada como completada por depreciación");
                        } else {
                            MissionAssignment assignment = new MissionAssignment(mission);
                            assignment.setProgress(progress);
                            assignment.setCompleted(completed);
                            assignment.setFailed(failed);
                            assignments.add(assignment);
                        }
                    }
                }
                
                playerAssignments.put(uuid, assignments);
            } catch (Exception e) {
                plugin.getLogger().warning("Error cargando datos de jugador: " + e.getMessage());
            }
        }
    }

    public void savePlayerData() {
        try {
            if (!dataFile.exists()) {
                dataFile.createNewFile();
            }

            FileConfiguration config = new YamlConfiguration();
            
            for (Map.Entry<UUID, List<MissionAssignment>> entry : playerAssignments.entrySet()) {
                String path = "players." + entry.getKey().toString();
                
                config.set(path + ".ps", playerPs.getOrDefault(entry.getKey(), 0));
                
                List<Map<String, Object>> assignmentsList = new ArrayList<>();
                for (MissionAssignment assignment : entry.getValue()) {
                    Map<String, Object> assignmentMap = new HashMap<>();
                    assignmentMap.put("mission_id", assignment.getMission().getId());
                    assignmentMap.put("progress", assignment.getProgress());
                    assignmentMap.put("completed", assignment.isCompleted());
                    assignmentMap.put("failed", assignment.isFailed());
                    assignmentsList.add(assignmentMap);
                }
                
                config.set(path + ".assignments", assignmentsList);
            }

            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando mission_data.yml: " + e.getMessage());
        }
    }
    
    // [1.21.8] Resetear flag de celebración por jugador (llamado en /avo newday y /avo endday)
    public void resetPlayerDailyCompleteFired() {
        this.playerDailyCompleteFired.clear();
    }
    
    // [1.21.8] Verifica si el jugador completó TODAS sus misiones asignadas
    private boolean areAllDailyMissionsCompletedFor(UUID uuid) {
        List<MissionAssignment> assignments = playerAssignments.get(uuid);
        if (assignments == null || assignments.isEmpty()) {
            return false;
        }
        
        // Retorna true si todas están completadas
        return assignments.stream().allMatch(MissionAssignment::isCompleted);
    }
    
    // [1.21.8] Celebración por jugador según desastres.yml
    private void triggerPlayerDailyCompletionCelebration(Player p) {
        FileConfiguration c = plugin.getConfigManager().getDesastresConfig();
        String base = "efectos_al_completar_mis_misiones.";
        
        if (!c.getBoolean(base + "enabled", true)) {
            return;
        }
        
        // **CELEBRACIÓN MEJORADA CON COLORES DE RANGO**
        me.apocalipsis.missions.MissionRank rank = plugin.getRankService().getRank(p);
        Location loc = p.getLocation().add(0, 1.5, 0);
        
        // Colores según rango
        org.bukkit.Color primary, secondary;
        switch (rank) {
            case LEYENDA:
                primary = org.bukkit.Color.RED;
                secondary = org.bukkit.Color.ORANGE;
                break;
            case VETERANO:
                primary = org.bukkit.Color.ORANGE;
                secondary = org.bukkit.Color.YELLOW;
                break;
            case SOBREVIVIENTE:
                primary = org.bukkit.Color.YELLOW;
                secondary = org.bukkit.Color.WHITE;
                break;
            case EXPLORADOR:
                primary = org.bukkit.Color.AQUA;
                secondary = org.bukkit.Color.BLUE;
                break;
            default: // NOVATO
                primary = org.bukkit.Color.LIME;
                secondary = org.bukkit.Color.GREEN;
                break;
        }
        
        // Título animado
        String title = c.getString(base + "title", "§b§l¡MISIONES COMPLETADAS!");
        String subtitle = c.getString(base + "subtitle", "§7Has completado todas tus misiones del día");
        int stay = c.getInt(base + "title_stay_ticks", 60);
        
        if (title != null) title = title.replace("&", "§");
        if (subtitle != null) subtitle = subtitle.replace("&", "§");
        
        p.showTitle(net.kyori.adventure.title.Title.title(
            net.kyori.adventure.text.Component.text(title != null ? title : ""),
            net.kyori.adventure.text.Component.text(subtitle != null ? subtitle : ""),
            net.kyori.adventure.title.Title.Times.times(
                java.time.Duration.ofMillis(800),
                java.time.Duration.ofMillis(stay * 50L),
                java.time.Duration.ofMillis(800)
            )
        ));
        
        // **SONIDOS MÚLTIPLES EPICÓS**
        p.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 1.0f);
        p.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.2f, 1.1f);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            p.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.3f);
        }, 10L);
        
        // **PARTÍCULAS MÚLTIPLES DINÁMICAS**
        p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 50, 1.0, 1.0, 1.0, 0.05);
        p.getWorld().spawnParticle(Particle.FIREWORK, loc, 30, 0.8, 0.8, 0.8, 0.1);
        p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 20, 0.6, 0.6, 0.6, 0.08);
        p.getWorld().spawnParticle(Particle.END_ROD, loc, 15, 0.5, 0.5, 0.5, 0.05);
        
        // Partículas adicionales en círculo
        for (int i = 0; i < 16; i++) {
            double angle = 2 * Math.PI * i / 16;
            double x = Math.cos(angle) * 1.5;
            double z = Math.sin(angle) * 1.5;
            Location particleLoc = loc.clone().add(x, 0, z);
            p.getWorld().spawnParticle(Particle.FIREWORK, particleLoc, 2, 0.1, 0.1, 0.1, 0.02);
        }
        
        // **FUEGOS ARTIFICIALES CON COLORES DEL RANGO**
        int fireworks = c.getInt(base + "fireworks", 3);
        int power = c.getInt(base + "fireworks_power", 1);
        
        for (int i = 0; i < fireworks; i++) {
            final int index = i;
            final int delay = index * 10;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Firework fw = (Firework) p.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.setPower(Math.max(0, Math.min(2, power)));
                
                // Alternar tipos de efectos
                FireworkEffect.Type[] types = {
                    FireworkEffect.Type.BALL_LARGE,
                    FireworkEffect.Type.STAR,
                    FireworkEffect.Type.BURST
                };
                
                meta.addEffect(FireworkEffect.builder()
                    .with(types[index % types.length])
                    .withColor(primary)
                    .withFade(secondary)
                    .withTrail()
                    .withFlicker()
                    .build());
                fw.setFireworkMeta(meta);
            }, delay);
        }
        
        // **MENSAJE PÚBLICO DE CELEBRACIÓN**
        String rankDisplay = rank.getDisplayName();
        Bukkit.getServer().broadcast(
            net.kyori.adventure.text.Component.text("§6§l⭐ " + rankDisplay + " §e" + p.getName() + 
                " §7ha completado todas sus misiones del día! §6§l⭐")
        );
        
        // Comandos opcionales
        List<String> cmds = c.getStringList(base + "commands_on_complete");
        if (cmds != null && !cmds.isEmpty()) {
            for (String raw : cmds) {
                String cmd = raw.replace("%player%", p.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }
    }
    
    private Particle safeParticle(String name, Particle def) {
        try {
            return Particle.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return def;
        }
    }
    
    private Sound safeSound(String name, Sound def) {
        try {
            // [1.21+] Usar Key en lugar de valueOf (deprecated)
            String keyStr = name.toLowerCase(Locale.ROOT).replace("_", ".");
            if (!keyStr.contains(":")) {
                keyStr = "minecraft:" + keyStr;
            }
            org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.fromString(keyStr);
            if (key == null) return def;
            
            Sound sound = org.bukkit.Registry.SOUNDS.get(key);
            return sound != null ? sound : def;
        } catch (Exception e) {
            return def;
        }
    }
    
    // ═════════════════════════════════════════════════════════════════
    // [REMOVED] Código de EXPLORAR/ALTURA eliminado (tipos deshabilitados)
    // ═════════════════════════════════════════════════════════════════
    
    /**
     * Activa/desactiva el modo debug para EXPLORAR
     */
    public void setDebugExplore(boolean on) {
        this.debugExplore = on;
    }
    
    /**
     * Toggle del modo debug para EXPLORAR (retorna el estado actual)
     */
    public boolean toggleDebugExplore() {
        this.debugExplore = !this.debugExplore;
        return this.debugExplore;
    }
    
    // ═════════════════════════════════════════════════════════════════
    // [ALTURA] Métodos mantenidos por compatibilidad (tipo deshabilitado)
    // ═════════════════════════════════════════════════════════════════
    
    /**
     * Resetea los contadores de altura (llamado en /avo newday y /avo endday)
     */
    public void resetHeightCounters() {
        heightSeconds.clear();
    }
    
    /**
     * Detiene el scheduler de altura (llamado al desactivar plugin)
     */
    public void stopHeightTracker() {
        if (heightTaskId != -1) {
            Bukkit.getScheduler().cancelTask(heightTaskId);
            heightTaskId = -1;
        }
    }
    
    /**
     * [INTEGRACIÓN ALONSOLEVELS] Ejecuta comandos AlonsoLevels al completar una misión.
     * Calcula EXP desde PS usando factores configurables y ejecuta comandos desde consola.
     * 
     * Solo se ejecuta si:
     * - La configuración alonsolevels.enabled = true
     * - La misión está marcada como completada
     * 
     * @param player Jugador que completó la misión
     * @param mission Misión completada
     */
    private void dispatchAlonsoLevelsOnMissionCompleted(Player player, MissionCatalog mission) {
        // Cargar alonsolevels.yml
        FileConfiguration a = plugin.getConfigManager().getAlonsoLevelsConfig();
        if (!a.getBoolean("enabled", true)) return;

        final String missionId = mission.getId();
        final String dificultad = mission.getDificultad() != null ? mission.getDificultad().name() : "DESCONOCIDA";
        final String rango = plugin.getRankService().getRank(player).name();

        // PS base desde la misión
        int psBase = mission.getRecompensaPs();

        // Multiplicadores
        double mult = 1.0;
        if (a.getBoolean("multipliers.enabled", true)) {
            double mDif = a.getDouble("multipliers.por_dificultad." + dificultad, 1.0);
            mult *= mDif;
        }
        if (a.getBoolean("multipliers_por_rango.enabled", false)) {
            double mR = a.getDouble("multipliers_por_rango." + rango, 1.0);
            mult *= mR;
        }
        double psCalc = psBase * mult;

        // EXP
        long exp = 0L;
        if (a.getBoolean("conversion.use_exp", true)) {
            double factor = a.getDouble("conversion.exp_factor", 10.0);
            double raw = psCalc * factor;
            String mode = a.getString("conversion.exp_round", "FLOOR");
            exp = roundValue(raw, mode);
        }

        // LEVELS (opcional)
        long levels = 0L;
        if (a.getBoolean("conversion.use_levels", false)) {
            double factor = a.getDouble("conversion.level_factor", 0.10);
            double raw = psCalc * factor;
            String mode = a.getString("conversion.level_round", "FLOOR");
            levels = roundValue(raw, mode);
        }

        // Construir y ejecutar comandos
        List<String> cmds = a.getStringList("commands");
        if (cmds == null || cmds.isEmpty()) return;

        final String sPlayer = player.getName();
        final String sPs    = String.valueOf((long)Math.floor(psCalc));
        final String sExp   = String.valueOf(exp);
        final String sLvl   = String.valueOf(levels);

        Bukkit.getScheduler().runTask(plugin, () -> {
            boolean log = a.getBoolean("log", true);
            for (String tpl : cmds) {
                String cmd = tpl
                    .replace("%player%", sPlayer)
                    .replace("%mission_id%", missionId)
                    .replace("%ps%", sPs)
                    .replace("%exp%", sExp)
                    .replace("%level%", sLvl)
                    .replace("%dificultad%", dificultad)
                    .replace("%rango%", rango);

                boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                if (log) plugin.getLogger().info("[AlonsoLevels] Comando: /" + cmd + " -> " + (ok ? "OK" : "ERROR"));
            }
        });
    }

    private long roundValue(double value, String mode) {
        switch (mode.toUpperCase(Locale.ROOT)) {
            case "CEIL":  return (long) Math.ceil(value);
            case "ROUND": return Math.round(value);
            default:      return (long) Math.floor(value); // FLOOR
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // [ADMIN COMMANDS] Métodos para comandos de administración
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Ajusta los PS de un jugador manualmente (usado por /avo setps)
     */
    public void setPlayerPS(UUID uuid, int ps) {
        playerPs.put(uuid, ps);
        savePlayerData();
    }

    /**
     * Fuerza completar todas las misiones pendientes de un jugador
     * @param completeAll Si es true, completa todas; si es false, solo completa las autocompletables
     * @return Número de misiones completadas
     */
    public int forceCompleteAllMissions(Player player, boolean completeAll) {
        UUID uuid = player.getUniqueId();
        List<MissionAssignment> assignments = playerAssignments.get(uuid);
        
        if (assignments == null || assignments.isEmpty()) {
            return 0;
        }

        int completed = 0;
        for (MissionAssignment assignment : assignments) {
            if (!assignment.isCompleted() && !assignment.isFailed()) {
                MissionCatalog mission = assignment.getMission();
                
                // Si no es completeAll, solo completar las autocompletables
                if (!completeAll && !isAutoCompletable(mission.getTipo())) {
                    continue;
                }
                
                // Marcar como completada
                assignment.setCompleted(true);
                assignment.setProgress(mission.getCantidad());
                
                // Dar recompensa
                int currentPs = playerPs.getOrDefault(uuid, 0);
                int newPs = currentPs + mission.getRecompensaPs();
                playerPs.put(uuid, newPs);
                
                // Ejecutar comandos AlonsoLevels
                dispatchAlonsoLevelsOnMissionCompleted(player, mission);
                
                completed++;
            }
        }

        if (completed > 0) {
            savePlayerData();
            
            // [FIX] Actualizar scoreboard y tablist inmediatamente
            if (plugin.getScoreboardManager() != null) {
                plugin.getScoreboardManager().updatePlayer(player);
            }
            if (plugin.getTablistManager() != null) {
                plugin.getTablistManager().updatePlayer(player);
            }
            
            String type = completeAll ? "todas" : "autocompletables";
            messageBus.sendMessage(player, "§a§l✓ " + completed + " misiones " + type + " completadas por administrador.");
        }

        return completed;
    }
    
    /**
     * Versión legacy para compatibilidad (completa todas)
     */
    public int forceCompleteAllMissions(Player player) {
        return forceCompleteAllMissions(player, true);
    }
    
    /**
     * Determina si un tipo de misión es autocompletable
     * Autocompletables: MATAR, ROMPER, CRAFTEAR, COCINAR, CONSUMIR
     * No autocompletables: EXPLORAR, ALTURA (ya removidos), y cualquier futura que requiera verificación manual
     */
    private boolean isAutoCompletable(MissionType type) {
        switch (type) {
            case MATAR:
            case ROMPER:
            case CRAFTEAR:
            case COCINAR:
            case CONSUMIR:
                return true;
            default:
                return false;
        }
    }

    /**
     * Limpia todas las misiones de un jugador
     */
    public void clearPlayerMissions(UUID uuid) {
        playerAssignments.remove(uuid);
        heightSeconds.remove(uuid);
        savePlayerData();
    }
    
    /**
     * Falla una misión aleatoria del jugador (penalización por evasión)
     */
    public void failRandomMission(UUID uuid) {
        List<MissionAssignment> assignments = playerAssignments.get(uuid);
        if (assignments == null || assignments.isEmpty()) return;
        
        // Filtrar misiones que no están completadas ni fallidas
        List<MissionAssignment> active = assignments.stream()
            .filter(a -> !a.isCompleted() && !a.isFailed())
            .collect(Collectors.toList());
        
        if (active.isEmpty()) return;
        
        // Seleccionar una misión al azar y marcarla como fallida
        Random rand = new Random();
        MissionAssignment toFail = active.get(rand.nextInt(active.size()));
        toFail.setFailed(true);
        
        savePlayerData();
        
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[MISIONES] Misión '" + toFail.getMission().getId() + "' fallada por evasión (UUID: " + uuid + ")");
        }
    }
    
    /**
     * Falla todas las misiones del jugador (penalización severa por evasión repetida)
     */
    public void failAllMissions(UUID uuid) {
        List<MissionAssignment> assignments = playerAssignments.get(uuid);
        if (assignments == null || assignments.isEmpty()) return;
        
        int failedCount = 0;
        for (MissionAssignment assignment : assignments) {
            if (!assignment.isCompleted() && !assignment.isFailed()) {
                assignment.setFailed(true);
                failedCount++;
            }
        }
        
        savePlayerData();
        
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[MISIONES] " + failedCount + " misiones falladas por evasión múltiple (UUID: " + uuid + ")");
        }
    }
    
    /**
     * Obtiene los PS de un jugador por UUID
     */
    public int getPS(UUID uuid) {
        return playerPs.getOrDefault(uuid, 0);
    }
    
    /**
     * Establece los PS de un jugador directamente (para castigos/recompensas externas)
     */
    public void setPS(UUID uuid, int ps) {
        playerPs.put(uuid, ps);
        savePlayerData();
    }
    
    /**
     * Establece los PS de un jugador por UUID (alias de setPS)
     */
    public void setPlayerPs(UUID uuid, int ps) {
        playerPs.put(uuid, ps);
        savePlayerData();
    }

    /**
     * Añade una misión personalizada creada por admin
     * @return true si se añadió exitosamente
     */
    public boolean addCustomMission(UUID uuid, MissionType tipo, String objetivo, int meta, MissionRank rank) {
        try {
            // Crear un MissionCatalog temporal para la misión personalizada
            // Constructor: (id, nombre, tipo, objetivo, cantidad, dificultad, rangos, recompensaPs)
            MissionCatalog customCatalog = new MissionCatalog(
                "CUSTOM_" + System.currentTimeMillis(),  // id
                "Misión Especial",                        // nombre
                tipo,                                     // tipo
                objetivo,                                 // objetivo
                meta,                                     // cantidad
                MissionDifficulty.MEDIA,                  // dificultad
                java.util.Arrays.asList(rank),            // rangos
                10                                        // recompensaPs
            );

            MissionAssignment assignment = new MissionAssignment(customCatalog);
            
            List<MissionAssignment> assignments = playerAssignments.computeIfAbsent(uuid, k -> new ArrayList<>());
            assignments.add(assignment);
            
            savePlayerData();
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("[MissionService] Error creando misión custom: " + e.getMessage());
            return false;
        }
    }
}
