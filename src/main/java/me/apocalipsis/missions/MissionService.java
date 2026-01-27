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

    // [PERFORMANCE] Cache de contadores para evitar recalcular
    private final Map<UUID, Integer> completedCountCache = new HashMap<>();
    private final Map<UUID, Integer> failedCountCache = new HashMap<>();
    
    // [OPTIMIZACIÓN] Índice por tipo de misión para búsqueda O(1)
    private final Map<MissionType, List<MissionCatalog>> catalogByType = new HashMap<>();

    private int maxPorDia = 5;
    private final Map<MissionRank, Integer> porRango = new HashMap<>();
    private final Map<MissionDifficulty, Integer> pesosPorDificultad = new HashMap<>();
    
    // [1.21.8] Control de celebración por jugador (evita retrigger)
    private final Set<UUID> playerDailyCompleteFired = new HashSet<>();
    
    // [v2.0] Sistema de castigos pendientes y tracking de actividad
    private final Map<UUID, Integer> playerLastActiveDay = new HashMap<>();      // Último día en que el jugador estuvo activo
    private final Map<UUID, PendingPenalty> pendingPenalties = new HashMap<>();  // Castigos pendientes para jugadores offline
    
    // [ALTURA] Variables mantenidas por compatibilidad (tipo deshabilitado pero código residual aún referenciado)
    private final Map<UUID, Integer> heightSeconds = new HashMap<>();
    private int heightTaskId = -1;
    private boolean debugExplore = false;
    
    /**
     * Clase para almacenar castigos pendientes
     */
    public static class PendingPenalty {
        public final int xpLoss;
        public final int failedMissionsCount;
        public final int dayFailed;
        
        public PendingPenalty(int xpLoss, int failedMissionsCount, int dayFailed) {
            this.xpLoss = xpLoss;
            this.failedMissionsCount = failedMissionsCount;
            this.dayFailed = dayFailed;
        }
    }

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
        catalogByType.clear(); // Limpiar índice
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
                
                MissionCatalog mission = new MissionCatalog(id, nombre, tipo, objetivo, cantidad, dificultad, rangos, recompensaPs);
                catalog.add(mission);
                
                // [OPTIMIZACIÓN] Agregar al índice por tipo
                catalogByType.computeIfAbsent(tipo, k -> new ArrayList<>()).add(mission);
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
     * IMPORTANTE: 
     * - Primero verifica y aplica castigos por misiones fallidas del día anterior
     * - Solo castiga a jugadores que estuvieron activos el día anterior
     * - Guarda castigos pendientes para jugadores offline
     * - Luego limpia las misiones y asigna nuevas
     * 
     * Este método se llama desde /avo newday
     */
    public void assignMissionsForDay(int day) {
        int previousDay = day - 1;
        
        // [v2.0] PASO 1: Verificar y aplicar castigos del día anterior
        // Solo para jugadores que estuvieron activos ese día
        processDayEndPenalties(previousDay);
        
        // [FIX] Limpiar todas las misiones del día anterior
        playerAssignments.clear();
        
        // [PERFORMANCE] Limpiar caches
        completedCountCache.clear();
        failedCountCache.clear();
        
        // Asignar nuevas misiones a todos los jugadores online
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            assignMissionsToPlayer(player, day);
        }
        
        savePlayerData();
        
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[MISIONES] Nuevo día " + day + " iniciado. Misiones anteriores limpiadas, nuevas asignadas a " + plugin.getServer().getOnlinePlayers().size() + " jugadores.");
        }
    }
    
    /**
     * Procesa los castigos por misiones fallidas del día anterior.
     * - Aplica castigos inmediatamente a jugadores online
     * - Guarda castigos pendientes para jugadores offline
     * - Solo afecta a jugadores que estuvieron activos ese día
     */
    private void processDayEndPenalties(int dayToCheck) {
        if (dayToCheck <= 0) return; // No hay día anterior válido
        
        // Copiar para evitar ConcurrentModification
        Map<UUID, List<MissionAssignment>> assignmentsCopy = new HashMap<>(playerAssignments);
        
        for (Map.Entry<UUID, List<MissionAssignment>> entry : assignmentsCopy.entrySet()) {
            UUID uuid = entry.getKey();
            List<MissionAssignment> assignments = entry.getValue();
            
            // Verificar si el jugador estuvo activo el día que estamos cerrando
            Integer lastActiveDay = playerLastActiveDay.get(uuid);
            if (lastActiveDay == null || lastActiveDay != dayToCheck) {
                // El jugador no estuvo activo ese día, no penalizar
                if (plugin.getConfigManager().isDebugCiclo()) {
                    plugin.getLogger().info("[CASTIGOS] Jugador " + uuid + " no estuvo activo día " + dayToCheck + ", sin castigo");
                }
                continue;
            }
            
            // Contar misiones fallidas (no completadas)
            List<MissionAssignment> failedMissions = assignments.stream()
                .filter(a -> !a.isCompleted() && !a.isFailed())
                .collect(Collectors.toList());
            
            if (failedMissions.isEmpty()) {
                // No hay misiones fallidas, nada que hacer
                continue;
            }
            
            // Calcular pérdida de XP
            int totalXPLoss = calculateXPLoss(failedMissions);
            
            // Crear castigo pendiente
            PendingPenalty penalty = new PendingPenalty(totalXPLoss, failedMissions.size(), dayToCheck);
            
            // Verificar si el jugador está online
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Aplicar castigo inmediatamente
                applyPendingPenalty(player, penalty);
            } else {
                // Guardar castigo pendiente para cuando entre
                pendingPenalties.put(uuid, penalty);
                
                if (plugin.getConfigManager().isDebugCiclo()) {
                    plugin.getLogger().info("[CASTIGOS] Castigo pendiente guardado para " + uuid + 
                        ": -" + totalXPLoss + " XP por " + failedMissions.size() + " misiones fallidas");
                }
            }
            
            // Marcar misiones como fallidas
            for (MissionAssignment assignment : failedMissions) {
                assignment.setFailed(true);
            }
        }
        
        savePlayerData();
    }
    
    /**
     * Calcula la pérdida de XP total por misiones fallidas
     */
    private int calculateXPLoss(List<MissionAssignment> failedMissions) {
        int totalXPLoss = 0;
        for (MissionAssignment assignment : failedMissions) {
            MissionDifficulty difficulty = assignment.getMission().getDificultad();
            int xpLoss = switch (difficulty) {
                case FACIL -> 15;
                case MEDIA -> 30;
                case DIFICIL -> 50;
                default -> 20;
            };
            totalXPLoss += xpLoss;
        }
        return totalXPLoss;
    }
    
    /**
     * Aplica castigo pendiente a un jugador que acaba de conectarse.
     * Llamado desde PlayerListener.onPlayerJoin()
     * @return true si se aplicó un castigo pendiente
     */
    public boolean applyPendingPenalty(Player player) {
        UUID uuid = player.getUniqueId();
        PendingPenalty penalty = pendingPenalties.remove(uuid);
        
        if (penalty == null) {
            return false;
        }
        
        // Obtener XP actual y calcular nuevo valor
        int currentXP = plugin.getExperienceService().getXP(player);
        int newXP = Math.max(0, currentXP - penalty.xpLoss);
        
        // Solo aplicar si hay algo que quitar
        if (penalty.xpLoss > 0 && currentXP > 0) {
            plugin.getExperienceService().setXP(player, newXP);
            
            // Sonido de penalización
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.7f);
            
            // Mensaje explicativo
            messageBus.sendMessage(player, "§c§l⚠ Castigo Pendiente Aplicado");
            if (penalty.failedMissionsCount == 1) {
                messageBus.sendMessage(player, "§7Fallaste §c1§7 misión del día §e" + penalty.dayFailed + "§7. (§c-" + penalty.xpLoss + " XP§7)");
            } else {
                messageBus.sendMessage(player, "§7Fallaste §c" + penalty.failedMissionsCount + "§7 misiones del día §e" + penalty.dayFailed + "§7. (§c-" + penalty.xpLoss + " XP§7)");
            }
            messageBus.sendMessage(player, "§7Tip: Completa tus misiones antes de desconectarte.");
            
            // ActionBar
            messageBus.sendActionBar(player, "§c⚠ -" + penalty.xpLoss + " XP por misiones fallidas");
            
            // Partículas sutiles
            player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1.5, 0), 10, 0.3, 0.3, 0.3, 0.02);
            
            // Log
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[CASTIGOS] Castigo pendiente aplicado a " + player.getName() + 
                    ": -" + penalty.xpLoss + " XP por " + penalty.failedMissionsCount + " misiones del día " + penalty.dayFailed);
            }
            
            // Actualizar scoreboard
            if (plugin.getScoreboardManager() != null) {
                plugin.getScoreboardManager().updatePlayer(player);
            }
        }
        
        savePlayerData();
        return true;
    }
    
    /**
     * Aplica un castigo directamente a un jugador online.
     * Usado por processDayEndPenalties cuando el jugador está conectado.
     */
    private void applyPendingPenalty(Player player, PendingPenalty penalty) {
        // Obtener XP actual y calcular nuevo valor
        int currentXP = plugin.getExperienceService().getXP(player);
        int newXP = Math.max(0, currentXP - penalty.xpLoss);
        
        // Solo aplicar si hay algo que quitar
        if (penalty.xpLoss > 0 && currentXP > 0) {
            plugin.getExperienceService().setXP(player, newXP);
            
            // Sonido de penalización
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.7f);
            
            // Mensaje explicativo
            if (penalty.failedMissionsCount == 1) {
                messageBus.sendMessage(player, "§c✗ §7Misión fallida del día anterior §7(§c-" + penalty.xpLoss + " XP§7)");
            } else {
                messageBus.sendMessage(player, "§c✗ §7Fallaste §c" + penalty.failedMissionsCount + "§7 misiones del día anterior. (§c-" + penalty.xpLoss + " XP§7)");
            }
            
            // ActionBar
            messageBus.sendActionBar(player, "§c⚠ -" + penalty.xpLoss + " XP por misiones fallidas");
            
            // Partículas sutiles
            player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1.5, 0), 10, 0.3, 0.3, 0.3, 0.02);
            
            // Sonido adicional si muchas misiones fallidas
            if (penalty.failedMissionsCount >= 3) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                }, 10L);
            }
            
            // Log
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[CASTIGOS] Castigo aplicado a " + player.getName() + 
                    ": -" + penalty.xpLoss + " XP por " + penalty.failedMissionsCount + " misiones");
            }
            
            // Actualizar scoreboard
            if (plugin.getScoreboardManager() != null) {
                plugin.getScoreboardManager().updatePlayer(player);
            }
        }
    }

    public void assignMissionsToPlayer(Player player) {
        assignMissionsToPlayer(player, plugin.getStateManager().getCurrentDay());
    }
    
    /**
     * Asigna misiones a un jugador específico y marca el día como activo
     */
    public void assignMissionsToPlayer(Player player, int currentDay) {
        UUID uuid = player.getUniqueId();
        
        // [v2.0] Marcar el día actual como activo para este jugador
        // Esto es importante para saber si debe recibir castigo al finalizar el día
        playerLastActiveDay.put(uuid, currentDay);
        
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
        MissionRank rank = MissionRank.fromXp(ps);
        
        // [ONBOARDING] Marcar si está en onboarding (para priorizar en UI, pero no limitar misiones)
        final boolean isOnboarding;
        if (plugin.getTutorialManager() != null && 
            plugin.getTutorialManager().getOnboardingManager() != null) {
            isOnboarding = !plugin.getTutorialManager().getOnboardingManager().hasCompletedOnboarding(uuid);
        } else {
            isOnboarding = false;
        }
        
        // [RANGOS.YML] Usar misionesDiarias del rango configurado (sin restricción por onboarding)
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
            .filter(m -> isOnboarding ? m.getDificultad() == MissionDifficulty.FACIL : m.isValidForRank(rank))
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
            
            // [MEJORA] Matching flexible para troncos y minerales
            boolean matches = false;
            if (mission.getTipo() == type) {
                String objetivo = mission.getObjetivo();
                
                // Si la misión pide un log específico o "ANY_LOG", aceptar cualquier tronco
                if (objetivo.equals("ANY_LOG") || objetivo.endsWith("_LOG")) {
                    matches = isWoodLog(target) && (objetivo.equals("ANY_LOG") || objetivo.equalsIgnoreCase(target));
                } 
                // [NUEVO] Si la misión pide un mineral, aceptar también su variante deepslate
                else if (objetivo.endsWith("_ORE")) {
                    matches = matchesOreVariant(objetivo, target);
                } 
                else {
                    // Match normal para otros materiales
                    matches = objetivo.equalsIgnoreCase(target);
                }
            }
            
            if (matches) {
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
                        // [PERFORMANCE] Invalidar cache
                        completedCountCache.remove(uuid);
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
        me.apocalipsis.missions.MissionRank oldRank = me.apocalipsis.missions.MissionRank.fromXp(currentPs);
        me.apocalipsis.missions.MissionRank newRank = me.apocalipsis.missions.MissionRank.fromXp(newPs);
        
        playerPs.put(uuid, newPs);
        
        // **EFECTOS MEJORADOS POR DIFICULTAD**
        playMissionCompleteEffects(player, mission);
        
        messageBus.sendMessage(player, "§a§l✓ Misión completada: §f" + mission.getNombre() + " §7(§e+" + mission.getRecompensaPs() + " PS§7)");
        
        // [ONBOARDING SYSTEM] Notificar completación de misión al sistema de onboarding
        if (plugin.getTutorialManager() != null && plugin.getTutorialManager().getOnboardingManager() != null) {
            plugin.getTutorialManager().getOnboardingManager().onPlayerCompleteMission(player);
        }
        
        // [BUDDY SYSTEM] Recompensar mentor cuando aprendiz completa misión
        if (plugin.getTutorialManager() != null && plugin.getTutorialManager().getBuddyService() != null) {
            plugin.getTutorialManager().getBuddyService().rewardMentor(uuid, me.apocalipsis.tutorial.BuddyService.BuddyRewardReason.APPRENTICE_MISSION_COMPLETED);
        }
        
        savePlayerData();
        
        // [XP SYSTEM] Otorgar experiencia por completar la misión
        if (plugin.getExperienceService() != null) {
            plugin.getExperienceService().addMissionXP(player, mission.getRecompensaPs(), mission.getDificultad());
        }
        
        // [REWARD SYSTEM] Recompensa aleatoria por completar misión (incentivo adicional)
        if (plugin.getRewardService() != null) {
            plugin.getRewardService().deliverMissionReward(player, mission.getDificultad());
        }
        
        // [DATA.YML] Hooks - TODO: Implementar sistema completo de data.yml
        // plugin.getConfigManager().onMissionCompleted(uuid);
        // plugin.getConfigManager().onPsChange(uuid, currentPs, newPs);
        
        // Si hubo rank up, registrarlo
        if (oldRank != newRank) {
            // TODO: Implementar onRankUp en ConfigManager
            // plugin.getConfigManager().onRankUp(uuid, newRank.name(), newRank.getDisplayName());
            playRankUpEffects(player, newRank);
            
            // [ONBOARDING SYSTEM] Notificar al onboarding sobre rank up
            if (plugin.getTutorialManager() != null && plugin.getTutorialManager().getOnboardingManager() != null) {
                plugin.getTutorialManager().getOnboardingManager().onPlayerRankUp(player);
            }
            
            // [REWARD SYSTEM] Entregar recompensas de rango
            if (plugin.getRewardService() != null) {
                plugin.getRewardService().deliverRewards(player, newRank);
            }
            
            // [BUDDY SYSTEM] Recompensar mentor si el aprendiz subió de rango
            if (plugin.getTutorialManager() != null && plugin.getTutorialManager().getBuddyService() != null) {
                plugin.getTutorialManager().getBuddyService().rewardMentor(uuid, me.apocalipsis.tutorial.BuddyService.BuddyRewardReason.APPRENTICE_RANK_UP);
            }
        }
        
        // [FIX] Actualizar scoreboard y tablist inmediatamente para reflejar cambio de PS/rango
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updatePlayer(player);
        }
        if (plugin.getTablistManager() != null) {
            plugin.getTablistManager().updatePlayer(player);
        }
        
        // [BUDDY SYSTEM] Recompensar mentor cuando aprendiz completa misión
        if (plugin.getTutorialManager() != null && plugin.getTutorialManager().getBuddyService() != null) {
            plugin.getTutorialManager().getBuddyService().rewardMentor(uuid, me.apocalipsis.tutorial.BuddyService.BuddyRewardReason.APPRENTICE_MISSION_COMPLETED);
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

    public List<MissionAssignment> getActiveAssignments(Player player) {
        return playerAssignments.getOrDefault(player.getUniqueId(), Collections.emptyList());
    }

    public int getCompletedCount(Player player) {
        UUID uuid = player.getUniqueId();
        
        // [PERFORMANCE] Usar cache si existe
        if (completedCountCache.containsKey(uuid)) {
            return completedCountCache.get(uuid);
        }
        
        // Calcular y cachear
        int count = (int) getActiveAssignments(player).stream()
            .filter(MissionAssignment::isCompleted)
            .count();
        
        completedCountCache.put(uuid, count);
        return count;
    }

    public int getPlayerPs(Player player) {
        return playerPs.getOrDefault(player.getUniqueId(), 0);
    }
    
    /**
     * Obtiene los PS de un jugador por UUID (para sistemas offline como evasiones)
     */
    public int getPlayerPs(UUID uuid) {
        return playerPs.getOrDefault(uuid, 0);
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
                
                // [COMPATIBILIDAD] Leer tanto "xp" como "ps" (ps es legacy)
                int xp = playerSection.getInt("xp", -1);
                if (xp == -1) {
                    // Fallback a "ps" si "xp" no existe (compatibilidad con datos antiguos)
                    xp = playerSection.getInt("ps", 0);
                }
                playerPs.put(uuid, xp);
                
                // [v2.0] Cargar día activo
                int lastActiveDay = playerSection.getInt("last_active_day", 0);
                if (lastActiveDay > 0) {
                    playerLastActiveDay.put(uuid, lastActiveDay);
                }
                
                // [v2.0] Cargar castigo pendiente
                ConfigurationSection penaltySection = playerSection.getConfigurationSection("pending_penalty");
                if (penaltySection != null) {
                    int xpLoss = penaltySection.getInt("xp_loss", 0);
                    int failedCount = penaltySection.getInt("failed_count", 0);
                    int dayFailed = penaltySection.getInt("day_failed", 0);
                    if (xpLoss > 0 && failedCount > 0) {
                        pendingPenalties.put(uuid, new PendingPenalty(xpLoss, failedCount, dayFailed));
                    }
                }
                
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
            
            // Guardar datos de jugadores con assignments
            for (Map.Entry<UUID, List<MissionAssignment>> entry : playerAssignments.entrySet()) {
                String path = "players." + entry.getKey().toString();
                UUID uuid = entry.getKey();
                
                // [UNIFICACIÓN] Guardar como "xp" (el sistema unificado)
                config.set(path + ".xp", playerPs.getOrDefault(uuid, 0));
                
                // [v2.0] Guardar día activo
                Integer lastActiveDay = playerLastActiveDay.get(uuid);
                if (lastActiveDay != null && lastActiveDay > 0) {
                    config.set(path + ".last_active_day", lastActiveDay);
                }
                
                // [v2.0] Guardar castigo pendiente si existe
                PendingPenalty penalty = pendingPenalties.get(uuid);
                if (penalty != null) {
                    config.set(path + ".pending_penalty.xp_loss", penalty.xpLoss);
                    config.set(path + ".pending_penalty.failed_count", penalty.failedMissionsCount);
                    config.set(path + ".pending_penalty.day_failed", penalty.dayFailed);
                }
                
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
            
            // [v2.0] Guardar también jugadores que solo tienen castigo pendiente (sin assignments actuales)
            for (Map.Entry<UUID, PendingPenalty> entry : pendingPenalties.entrySet()) {
                UUID uuid = entry.getKey();
                if (!playerAssignments.containsKey(uuid)) {
                    String path = "players." + uuid.toString();
                    config.set(path + ".xp", playerPs.getOrDefault(uuid, 0));
                    
                    Integer lastActiveDay = playerLastActiveDay.get(uuid);
                    if (lastActiveDay != null && lastActiveDay > 0) {
                        config.set(path + ".last_active_day", lastActiveDay);
                    }
                    
                    PendingPenalty penalty = entry.getValue();
                    config.set(path + ".pending_penalty.xp_loss", penalty.xpLoss);
                    config.set(path + ".pending_penalty.failed_count", penalty.failedMissionsCount);
                    config.set(path + ".pending_penalty.day_failed", penalty.dayFailed);
                }
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
        
        // [REWARD SYSTEM] Entregar recompensas diarias completas
        if (plugin.getRewardService() != null) {
            plugin.getRewardService().deliverDailyCompletionReward(p);
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
    
    /**
     * [NUEVO] Verifica si un mineral matchea con su objetivo, incluyendo variantes deepslate
     * Por ejemplo: DIAMOND_ORE matchea con DIAMOND_ORE y DEEPSLATE_DIAMOND_ORE
     */
    private boolean matchesOreVariant(String objetivo, String minedBlock) {
        // Match exacto
        if (objetivo.equalsIgnoreCase(minedBlock)) {
            return true;
        }
        
        // Si el objetivo es un ORE normal, aceptar también su variante DEEPSLATE
        if (objetivo.endsWith("_ORE") && !objetivo.startsWith("DEEPSLATE_")) {
            // Obtener el tipo de mineral (ej: DIAMOND de DIAMOND_ORE)
            String oreType = objetivo.replace("_ORE", "");
            String deepslateVariant = "DEEPSLATE_" + oreType + "_ORE";
            
            if (deepslateVariant.equalsIgnoreCase(minedBlock)) {
                return true;
            }
        }
        
        // Si el objetivo es DEEPSLATE_X_ORE, aceptar también X_ORE normal
        if (objetivo.startsWith("DEEPSLATE_") && objetivo.endsWith("_ORE")) {
            String normalVariant = objetivo.replace("DEEPSLATE_", "");
            
            if (normalVariant.equalsIgnoreCase(minedBlock)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * [NUEVO] Verifica si un material es un tronco de árbol (cualquier tipo)
     * Incluye: OAK, BIRCH, SPRUCE, JUNGLE, ACACIA, DARK_OAK, CRIMSON, WARPED, MANGROVE, CHERRY
     */
    private boolean isWoodLog(String material) {
        return material.equals("OAK_LOG") ||
               material.equals("BIRCH_LOG") ||
               material.equals("SPRUCE_LOG") ||
               material.equals("JUNGLE_LOG") ||
               material.equals("ACACIA_LOG") ||
               material.equals("DARK_OAK_LOG") ||
               material.equals("CRIMSON_STEM") ||    // Nether
               material.equals("WARPED_STEM") ||     // Nether
               material.equals("MANGROVE_LOG") ||
               material.equals("CHERRY_LOG") ||
               material.equals("STRIPPED_OAK_LOG") ||
               material.equals("STRIPPED_BIRCH_LOG") ||
               material.equals("STRIPPED_SPRUCE_LOG") ||
               material.equals("STRIPPED_JUNGLE_LOG") ||
               material.equals("STRIPPED_ACACIA_LOG") ||
               material.equals("STRIPPED_DARK_OAK_LOG") ||
               material.equals("STRIPPED_CRIMSON_STEM") ||
               material.equals("STRIPPED_WARPED_STEM") ||
               material.equals("STRIPPED_MANGROVE_LOG") ||
               material.equals("STRIPPED_CHERRY_LOG");
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
     * Añade PS a un jugador (para recompensas progresivas, tiempo jugado, eventos)
     * @param uuid UUID del jugador
     * @param amount Cantidad de PS a añadir
     * @param reason Razón para el log (ej: "Tiempo jugado", "Evento: Eco de Brasas")
     */
    public void addPS(UUID uuid, int amount, String reason) {
        int current = playerPs.getOrDefault(uuid, 0);
        int newAmount = current + amount;
        
        // Detectar rank up ANTES de actualizar los PS
        me.apocalipsis.missions.MissionRank oldRank = me.apocalipsis.missions.MissionRank.fromXp(current);
        me.apocalipsis.missions.MissionRank newRank = me.apocalipsis.missions.MissionRank.fromXp(newAmount);
        
        playerPs.put(uuid, newAmount);
        savePlayerData();
        
        // Log para tracking
        plugin.getLogger().info(String.format("[PS] %s ganó %d PS (%s) [%d -> %d]", 
            uuid, amount, reason, current, newAmount));
        
        // Si hubo rank up, ejecutar efectos y recompensas
        if (oldRank != newRank) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                playRankUpEffects(player, newRank);
                
                // Entregar recompensas de rango
                if (plugin.getRewardService() != null) {
                    plugin.getRewardService().deliverRewards(player, newRank);
                }
                
                // [BUDDY SYSTEM] Recompensar mentor si el aprendiz subió de rango
                if (plugin.getTutorialManager() != null && plugin.getTutorialManager().getBuddyService() != null) {
                    plugin.getTutorialManager().getBuddyService().rewardMentor(uuid, me.apocalipsis.tutorial.BuddyService.BuddyRewardReason.APPRENTICE_RANK_UP);
                }
                
                // Actualizar scoreboard y tablist
                if (plugin.getScoreboardManager() != null) {
                    plugin.getScoreboardManager().updatePlayer(player);
                }
                if (plugin.getTablistManager() != null) {
                    plugin.getTablistManager().updatePlayer(player);
                }
            }
        }
    }
    
    /**
     * Establece los PS de un jugador directamente (para castigos/recompensas externas)
     */
    public void setPS(UUID uuid, int ps) {
        int current = playerPs.getOrDefault(uuid, 0);
        
        // Detectar rank up
        me.apocalipsis.missions.MissionRank oldRank = me.apocalipsis.missions.MissionRank.fromXp(current);
        me.apocalipsis.missions.MissionRank newRank = me.apocalipsis.missions.MissionRank.fromXp(ps);
        
        playerPs.put(uuid, ps);
        savePlayerData();
        
        // Si hubo rank up, ejecutar efectos y recompensas
        if (oldRank != newRank) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                playRankUpEffects(player, newRank);
                
                // Entregar recompensas de rango
                if (plugin.getRewardService() != null) {
                    plugin.getRewardService().deliverRewards(player, newRank);
                }
                
                // [BUDDY SYSTEM] Recompensar mentor si el aprendiz subió de rango
                if (plugin.getTutorialManager() != null && plugin.getTutorialManager().getBuddyService() != null) {
                    plugin.getTutorialManager().getBuddyService().rewardMentor(uuid, me.apocalipsis.tutorial.BuddyService.BuddyRewardReason.APPRENTICE_RANK_UP);
                }
                
                // Actualizar scoreboard y tablist
                if (plugin.getScoreboardManager() != null) {
                    plugin.getScoreboardManager().updatePlayer(player);
                }
                if (plugin.getTablistManager() != null) {
                    plugin.getTablistManager().updatePlayer(player);
                }
            }
        }
    }
    
    /**
     * Establece los PS de un jugador por UUID (alias de setPS)
     */
    public void setPlayerPs(UUID uuid, int ps) {
        int current = playerPs.getOrDefault(uuid, 0);
        
        // Detectar rank up
        me.apocalipsis.missions.MissionRank oldRank = me.apocalipsis.missions.MissionRank.fromXp(current);
        me.apocalipsis.missions.MissionRank newRank = me.apocalipsis.missions.MissionRank.fromXp(ps);
        
        playerPs.put(uuid, ps);
        savePlayerData();
        
        // Si hubo rank up, ejecutar efectos y recompensas
        if (oldRank != newRank) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                playRankUpEffects(player, newRank);
                
                // Entregar recompensas de rango
                if (plugin.getRewardService() != null) {
                    plugin.getRewardService().deliverRewards(player, newRank);
                }
                
                // [BUDDY SYSTEM] Recompensar mentor si el aprendiz subió de rango
                if (plugin.getTutorialManager() != null && plugin.getTutorialManager().getBuddyService() != null) {
                    plugin.getTutorialManager().getBuddyService().rewardMentor(uuid, me.apocalipsis.tutorial.BuddyService.BuddyRewardReason.APPRENTICE_RANK_UP);
                }
                
                // Actualizar scoreboard y tablist
                if (plugin.getScoreboardManager() != null) {
                    plugin.getScoreboardManager().updatePlayer(player);
                }
                if (plugin.getTablistManager() != null) {
                    plugin.getTablistManager().updatePlayer(player);
                }
            }
        }
    }

    /**
     * Resetea todas las misiones de un jugador
     * Usado cuando un jugador entra a un mundo de ciclo nuevo
     * 
     * @param uuid UUID del jugador
     */
    public void resetPlayerMissions(UUID uuid) {
        // Limpiar asignaciones activas
        playerAssignments.remove(uuid);
        
        // Limpiar caches de contadores
        completedCountCache.remove(uuid);
        failedCountCache.remove(uuid);
        
        // Limpiar tracking de actividad
        playerLastActiveDay.remove(uuid);
        pendingPenalties.remove(uuid);
        playerDailyCompleteFired.remove(uuid);
        
        // NO resetear PS - eso se hace en WorldDataManager
        
        plugin.getLogger().info("[MissionService] Misiones reseteadas para " + uuid);
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
    
    /**
     * [OPTIMIZACIÓN] Obtiene misiones del catálogo filtradas por tipo
     * Búsqueda O(1) usando índice pre-compilado
     * @param type Tipo de misión a buscar
     * @return Lista de misiones del tipo especificado (nunca null)
     */
    public List<MissionCatalog> getMissionsByType(MissionType type) {
        return catalogByType.getOrDefault(type, Collections.emptyList());
    }
    
    /**
     * Obtiene todas las misiones del catálogo
     * @return Copia defensiva del catálogo
     */
    public List<MissionCatalog> getAllMissions() {
        return new ArrayList<>(catalog);
    }
}
