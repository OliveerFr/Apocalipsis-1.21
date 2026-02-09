package me.apocalipsis.skills;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import me.apocalipsis.Apocalipsis;

/**
 * Listener que aplica los efectos de las habilidades del árbol.
 * Incluye: Persistencia de waypoints, cooldowns reales, estadísticas de uso,
 * caché de skills, anti-spam de mensajes, y valores configurables.
 */
public class SkillEffectListener implements Listener {
    
    private final Apocalipsis plugin;
    private final SkillService skillService;
    
    // === CONFIGURACIÓN ===
    private FileConfiguration skillsConfig;
    
    // Valores configurables (cargados desde skills.yml)
    private double caidaSuaveReduccion = 0.25;
    private double plumaReduccion = 0.50;
    private double resistenciaFuegoReduccion = 0.20;
    private double ignifugoReduccion = 0.40;
    private int vueloAlturaMinima = 15;
    private int vueloDuracionTicks = 60;
    private int vueloCooldownSegundos = 60;
    private int fenixCorazonesRevivir = 3;
    private int fenixComidaRevivir = 10;
    private double estomagoHierroReduccion = 0.20;
    private double metabolismoLentoReduccion = 0.40;
    private double toqueFortunaChance = 0.10;
    private double sedaNaturalChance = 0.05;
    private int nadadorDuracionEfecto = 40;
    private int waypointCooldownTeleport = 30;
    private boolean waypointPersistencia = true;
    private long mensajeCooldownMs = 2000;
    private boolean statsEnabled = true;
    private boolean cacheEnabled = true;
    private int cacheTtlSegundos = 30;
    
    // === LEÑADOR CONFIG (3 niveles) ===
    // MEJORADO: SIN COOLDOWN para todos los niveles + más bloques para árboles altos
    // Nivel 1 (Nato): SIN cooldown, max 512 bloques
    // Nivel 2 (Experto): SIN cooldown, max 768 bloques  
    // Nivel 3 (Maestro): SIN cooldown, max 1024 bloques, auto-replant siempre, bonus XP
    private int lenadorCooldownNivel1 = 0;    // Leñador Nato - SIN COOLDOWN
    private int lenadorCooldownNivel2 = 0;    // Leñador Experto - SIN COOLDOWN
    private int lenadorCooldownNivel3 = 0;    // Leñador Maestro - SIN COOLDOWN
    private int lenadorMaxBloquesNivel1 = 512;  // Incrementado para árboles altos
    private int lenadorMaxBloquesNivel2 = 768;  // Incrementado para árboles altos
    private int lenadorMaxBloquesNivel3 = 1024; // Incrementado para árboles altos
    private boolean lenadorDesactivarSneaking = true;
    private boolean lenadorAutoReplant = true;
    private boolean lenadorVerificarArbolReal = true;
    private int lenadorRadioBuscarHojas = 8;     // Incrementado para mejor detección
    private int lenadorMinHojasRequeridas = 2;   // Reducido para árboles más flexibles
    private int lenadorXpPorArbolBase = 5;
    private int lenadorXpBonusMaestro = 10;   // XP extra para nivel 3
    private boolean lenadorSonidosProgresivos = true;
    private int lenadorDanoHerramientaCada = 3;
    
    // === CACHE DE SKILLS POR JUGADOR ===
    private final Map<UUID, CachedSkillData> skillCache = new ConcurrentHashMap<>();
    
    // === COOLDOWNS ===
    // Vuelo de Emergencia cooldowns (UUID -> timestamp cuando termina)
    private final Map<UUID, Long> glideCooldowns = new ConcurrentHashMap<>();
    // Waypoint teleport cooldowns
    private final Map<UUID, Long> waypointCooldowns = new ConcurrentHashMap<>();
    // Leñador nato cooldowns
    private final Map<UUID, Long> lenadorCooldowns = new ConcurrentHashMap<>();
    
    // === ANTI-SPAM DE MENSAJES ===
    // Clave: UUID + "_" + tipoMensaje, Valor: timestamp último mensaje
    private final Map<String, Long> lastMessages = new ConcurrentHashMap<>();
    
    // === ESTADÍSTICAS DE USO ===
    // Skill ID -> veces activada
    private final Map<String, Long> skillUsageStats = new ConcurrentHashMap<>();
    // UUID -> (Skill ID -> veces usada por jugador)
    private final Map<UUID, Map<String, Integer>> playerSkillStats = new ConcurrentHashMap<>();
    
    // Cache de jugadores cayendo para Vuelo de Emergencia
    private final Set<UUID> playersGliding = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // Jugadores que deben revivir con Fénix
    private final Map<UUID, Location> phoenixRevive = new ConcurrentHashMap<>();
    
    // Items que ya fueron procesados por auto-recolección
    private final Set<UUID> processedItems = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // Task IDs
    private int cleanupTaskId = -1;
    private int waterEffectsTaskId = -1;
    private int cacheCleanupTaskId = -1;
    private int statsAutoSaveTaskId = -1;
    private int waypointAutoSaveTaskId = -1;
    
    public SkillEffectListener(Apocalipsis plugin, SkillService skillService) {
        this.plugin = plugin;
        this.skillService = skillService;
        
        loadConfig();
        loadWaypoints();
        loadStats();
        startTasks();
    }
    
    // ==================== CONFIGURACIÓN ====================
    
    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "skills.yml");
        if (!configFile.exists()) {
            plugin.saveResource("skills.yml", false);
        }
        skillsConfig = YamlConfiguration.loadConfiguration(configFile);
        
        // Cargar valores
        caidaSuaveReduccion = skillsConfig.getDouble("efectos.caida.suave_reduccion", 0.25);
        plumaReduccion = skillsConfig.getDouble("efectos.caida.pluma_reduccion", 0.50);
        resistenciaFuegoReduccion = skillsConfig.getDouble("efectos.fuego.resistencia_reduccion", 0.20);
        ignifugoReduccion = skillsConfig.getDouble("efectos.fuego.ignifugo_reduccion", 0.40);
        vueloAlturaMinima = skillsConfig.getInt("efectos.vuelo_emergencia.altura_minima", 15);
        vueloDuracionTicks = skillsConfig.getInt("efectos.vuelo_emergencia.duracion_ticks", 60);
        vueloCooldownSegundos = skillsConfig.getInt("efectos.vuelo_emergencia.cooldown_segundos", 60);
        fenixCorazonesRevivir = skillsConfig.getInt("efectos.fenix.corazones_revivir", 3);
        fenixComidaRevivir = skillsConfig.getInt("efectos.fenix.comida_revivir", 10);
        estomagoHierroReduccion = skillsConfig.getDouble("efectos.hambre.estomago_hierro_reduccion", 0.20);
        metabolismoLentoReduccion = skillsConfig.getDouble("efectos.hambre.metabolismo_lento_reduccion", 0.40);
        toqueFortunaChance = skillsConfig.getDouble("efectos.mineria.toque_fortuna_chance", 0.10);
        sedaNaturalChance = skillsConfig.getDouble("efectos.mineria.seda_natural_chance", 0.05);
        nadadorDuracionEfecto = skillsConfig.getInt("efectos.nadador.duracion_efecto", 40);
        waypointCooldownTeleport = skillsConfig.getInt("waypoints.cooldown_teleport", 30);
        waypointPersistencia = skillsConfig.getBoolean("waypoints.persistencia", true);
        mensajeCooldownMs = skillsConfig.getLong("mensajes.cooldown_ms", 2000);
        statsEnabled = skillsConfig.getBoolean("estadisticas.enabled", true);
        cacheEnabled = skillsConfig.getBoolean("cache.enabled", true);
        cacheTtlSegundos = skillsConfig.getInt("cache.ttl_segundos", 30);
        
        // Leñador (3 niveles)
        lenadorCooldownNivel1 = skillsConfig.getInt("efectos.lenador.cooldown_nivel1", 5);
        lenadorCooldownNivel2 = skillsConfig.getInt("efectos.lenador.cooldown_nivel2", 2);
        lenadorCooldownNivel3 = skillsConfig.getInt("efectos.lenador.cooldown_nivel3", 0);
        lenadorMaxBloquesNivel1 = skillsConfig.getInt("efectos.lenador.max_bloques_nivel1", 256);
        lenadorMaxBloquesNivel2 = skillsConfig.getInt("efectos.lenador.max_bloques_nivel2", 384);
        lenadorMaxBloquesNivel3 = skillsConfig.getInt("efectos.lenador.max_bloques_nivel3", 512);
        lenadorDesactivarSneaking = skillsConfig.getBoolean("efectos.lenador.desactivar_sneaking", true);
        lenadorAutoReplant = skillsConfig.getBoolean("efectos.lenador.auto_replant", true);
        lenadorVerificarArbolReal = skillsConfig.getBoolean("efectos.lenador.verificar_arbol_real", true);
        lenadorRadioBuscarHojas = skillsConfig.getInt("efectos.lenador.radio_buscar_hojas", 4);
        lenadorMinHojasRequeridas = skillsConfig.getInt("efectos.lenador.min_hojas_requeridas", 3);
        lenadorXpPorArbolBase = skillsConfig.getInt("efectos.lenador.xp_por_arbol_base", 5);
        lenadorXpBonusMaestro = skillsConfig.getInt("efectos.lenador.xp_bonus_maestro", 10);
        lenadorSonidosProgresivos = skillsConfig.getBoolean("efectos.lenador.sonidos_progresivos", true);
        lenadorDanoHerramientaCada = skillsConfig.getInt("efectos.lenador.dano_herramienta_cada", 3);
    }
    
    public void reloadConfig() {
        loadConfig();
        plugin.getLogger().info("[Skills] Configuración recargada");
    }
    
    private void startTasks() {
        // Limpiar items procesados cada minuto
        cleanupTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            processedItems.clear();
            // Limpiar mensajes antiguos
            long now = System.currentTimeMillis();
            lastMessages.entrySet().removeIf(e -> now - e.getValue() > 60000);
        }, 1200L, 1200L).getTaskId();
        
        // Aplicar efectos de agua cada segundo
        waterEffectsTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                applySwimBoost(player);
                applyWaterBreathing(player);
            }
        }, 20L, 20L).getTaskId();
        
        // Limpiar caché expirada
        if (cacheEnabled) {
            cacheCleanupTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                long now = System.currentTimeMillis();
                long ttl = cacheTtlSegundos * 1000L;
                skillCache.entrySet().removeIf(e -> now - e.getValue().timestamp > ttl);
            }, 600L, 600L).getTaskId();
        }
        
        // Auto-guardar estadísticas
        if (statsEnabled) {
            int intervalTicks = skillsConfig.getInt("estadisticas.guardar_intervalo_minutos", 5) * 20 * 60;
            statsAutoSaveTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::saveStats, 
                intervalTicks, intervalTicks).getTaskId();
        }
        
        // Auto-guardar waypoints cada 5 minutos
        if (waypointPersistencia) {
            waypointAutoSaveTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                saveWaypoints();
                plugin.getLogger().info("[Skills] Auto-guardado de waypoints completado");
            }, 6000L, 6000L).getTaskId(); // 5 minutos = 6000 ticks
        }
    }
    
    public void shutdown() {
        // Cancelar tasks
        if (cleanupTaskId != -1) Bukkit.getScheduler().cancelTask(cleanupTaskId);
        if (waterEffectsTaskId != -1) Bukkit.getScheduler().cancelTask(waterEffectsTaskId);
        if (cacheCleanupTaskId != -1) Bukkit.getScheduler().cancelTask(cacheCleanupTaskId);
        if (statsAutoSaveTaskId != -1) Bukkit.getScheduler().cancelTask(statsAutoSaveTaskId);
        if (waypointAutoSaveTaskId != -1) Bukkit.getScheduler().cancelTask(waypointAutoSaveTaskId);
        
        // Guardar datos
        if (waypointPersistencia) saveWaypoints();
        if (statsEnabled) saveStats();
    }
    
    // ==================== CACHÉ DE SKILLS ====================
    
    private record CachedSkillData(Set<Skill> skills, Map<Skill, Boolean> enabledMap, long timestamp) {}
    
    private CachedSkillData getCachedSkills(UUID uuid) {
        if (!cacheEnabled) {
            return new CachedSkillData(
                skillService.getUnlockedSkills(uuid),
                new HashMap<>(),
                System.currentTimeMillis()
            );
        }
        
        return skillCache.computeIfAbsent(uuid, k -> {
            Set<Skill> skills = skillService.getUnlockedSkills(k);
            Map<Skill, Boolean> enabled = new HashMap<>();
            for (Skill skill : skills) {
                if (skill.isToggleable()) {
                    enabled.put(skill, skillService.isSkillEnabled(k, skill));
                }
            }
            return new CachedSkillData(skills, enabled, System.currentTimeMillis());
        });
    }
    
    private boolean hasSkillCached(UUID uuid, Skill skill) {
        CachedSkillData cache = getCachedSkills(uuid);
        return cache.skills.contains(skill);
    }
    
    private boolean isSkillEnabledCached(UUID uuid, Skill skill) {
        CachedSkillData cache = getCachedSkills(uuid);
        return cache.enabledMap.getOrDefault(skill, true);
    }
    
    public void invalidateCache(UUID uuid) {
        skillCache.remove(uuid);
    }
    
    // ==================== ANTI-SPAM MENSAJES ====================
    
    private boolean canSendMessage(UUID uuid, String messageType) {
        String key = uuid.toString() + "_" + messageType;
        long now = System.currentTimeMillis();
        Long lastTime = lastMessages.get(key);
        
        if (lastTime == null || now - lastTime >= mensajeCooldownMs) {
            lastMessages.put(key, now);
            return true;
        }
        return false;
    }
    
    // ==================== ESTADÍSTICAS ====================
    
    private void trackSkillUsage(UUID uuid, Skill skill) {
        if (!statsEnabled) return;
        
        String skillId = skill.getId();
        skillUsageStats.merge(skillId, 1L, Long::sum);
        
        playerSkillStats.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
            .merge(skillId, 1, Integer::sum);
    }
    
    public Map<String, Long> getGlobalStats() {
        return new HashMap<>(skillUsageStats);
    }
    
    public Map<String, Integer> getPlayerStats(UUID uuid) {
        return playerSkillStats.getOrDefault(uuid, new HashMap<>());
    }
    
    public record SkillStatEntry(String skillId, long totalUses) {}
    
    public List<SkillStatEntry> getTopSkills(int limit) {
        return skillUsageStats.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .map(e -> new SkillStatEntry(e.getKey(), e.getValue()))
            .toList();
    }
    
    private void loadStats() {
        if (!statsEnabled) return;
        
        File statsFile = new File(plugin.getDataFolder(), "skill_stats.yml");
        if (!statsFile.exists()) return;
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(statsFile);
        
        // Cargar stats globales
        if (config.isConfigurationSection("global")) {
            for (String skillId : config.getConfigurationSection("global").getKeys(false)) {
                skillUsageStats.put(skillId, config.getLong("global." + skillId));
            }
        }
        
        // Cargar stats por jugador
        if (config.isConfigurationSection("players")) {
            for (String uuidStr : config.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    Map<String, Integer> playerStats = new ConcurrentHashMap<>();
                    for (String skillId : config.getConfigurationSection("players." + uuidStr).getKeys(false)) {
                        playerStats.put(skillId, config.getInt("players." + uuidStr + "." + skillId));
                    }
                    playerSkillStats.put(uuid, playerStats);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }
    
    private void saveStats() {
        if (!statsEnabled) return;
        
        FileConfiguration config = new YamlConfiguration();
        
        // Guardar stats globales
        for (Map.Entry<String, Long> entry : skillUsageStats.entrySet()) {
            config.set("global." + entry.getKey(), entry.getValue());
        }
        
        // Guardar stats por jugador
        for (Map.Entry<UUID, Map<String, Integer>> entry : playerSkillStats.entrySet()) {
            String path = "players." + entry.getKey().toString();
            for (Map.Entry<String, Integer> stat : entry.getValue().entrySet()) {
                config.set(path + "." + stat.getKey(), stat.getValue());
            }
        }
        
        try {
            config.save(new File(plugin.getDataFolder(), "skill_stats.yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("[Skills] Error guardando estadísticas: " + e.getMessage());
        }
    }
    
    // ==================== COOLDOWN VUELO EMERGENCIA ====================
    
    public boolean isGlideOnCooldown(UUID uuid) {
        Long cooldownEnd = glideCooldowns.get(uuid);
        if (cooldownEnd == null) return false;
        return System.currentTimeMillis() < cooldownEnd;
    }
    
    public long getGlideCooldownRemaining(UUID uuid) {
        Long cooldownEnd = glideCooldowns.get(uuid);
        if (cooldownEnd == null) return 0;
        return Math.max(0, cooldownEnd - System.currentTimeMillis());
    }
    
    private void setGlideCooldown(UUID uuid) {
        long cooldownEnd = System.currentTimeMillis() + (vueloCooldownSegundos * 1000L);
        glideCooldowns.put(uuid, cooldownEnd);
    }
    
    // ==================== JOIN/QUIT ====================
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Pre-cargar caché
        if (cacheEnabled) {
            getCachedSkills(uuid);
        }
        
        // Aplicar efectos al unirse
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            skillService.applySkillEffects(player);
        }, 20L);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // ✅ CRÍTICO: Guardar waypoints cuando el jugador se desconecta (SÍNCRONO para evitar pérdida de datos)
        if (playerWaypoints.containsKey(uuid) && waypointPersistencia) {
            saveWaypoints();
            plugin.getLogger().info("[Skills] Waypoints guardados por desconexión de " + player.getName());
        }
        
        // Limpiar cachés
        playersGliding.remove(uuid);
        phoenixRevive.remove(uuid);
        skillCache.remove(uuid);
        glideCooldowns.remove(uuid);
        waypointCooldowns.remove(uuid);
        lenadorCooldowns.remove(uuid);
        
        // Limpiar mensajes anti-spam del jugador
        String prefix = uuid.toString() + "_";
        lastMessages.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
    }
    
    // ==================== DAÑO ====================
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        UUID uuid = player.getUniqueId();
        EntityDamageEvent.DamageCause cause = event.getCause();
        double damage = event.getDamage();
        double reduction = 0;
        
        // === CAÍDA SUAVE / PLUMA ===
        if (cause == EntityDamageEvent.DamageCause.FALL) {
            if (hasSkillCached(uuid, Skill.PLUMA)) {
                reduction = plumaReduccion;
                trackSkillUsage(uuid, Skill.PLUMA);
            } else if (hasSkillCached(uuid, Skill.CAIDA_SUAVE)) {
                reduction = caidaSuaveReduccion;
                trackSkillUsage(uuid, Skill.CAIDA_SUAVE);
            }
        }
        
        // === RESISTENCIA AL FUEGO / IGNÍFUGO ===
        if (cause == EntityDamageEvent.DamageCause.FIRE || 
            cause == EntityDamageEvent.DamageCause.FIRE_TICK ||
            cause == EntityDamageEvent.DamageCause.LAVA) {
            
            if (/* hasSkillCached(uuid, Skill.IGNIFUGO) */ false) {
                reduction = ignifugoReduccion;
                // trackSkillUsage(uuid, Skill.IGNIFUGO);
                // Inmune a daño por pisar fuego
                if (cause == EntityDamageEvent.DamageCause.FIRE_TICK) {
                    Block below = player.getLocation().subtract(0, 1, 0).getBlock();
                    if (below.getType() == Material.FIRE || below.getType() == Material.SOUL_FIRE) {
                        event.setCancelled(true);
                        return;
                    }
                }
            } else if (/* hasSkillCached(uuid, Skill.RESISTENCIA_FUEGO) */ false) {
                reduction = resistenciaFuegoReduccion;
                // trackSkillUsage(uuid, Skill.RESISTENCIA_FUEGO);
            }
        }
        
        // Aplicar reducción
        if (reduction > 0) {
            double newDamage = damage * (1 - reduction);
            event.setDamage(newDamage);
        }
    }
    
    // ==================== VUELO DE EMERGENCIA ====================
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Verificar Vuelo de Emergencia usando caché
        if (!hasSkillCached(uuid, Skill.VUELO_EMERGENCIA)) return;
        if (!isSkillEnabledCached(uuid, Skill.VUELO_EMERGENCIA)) return;
        if (playersGliding.contains(uuid)) return;
        if (player.isGliding() || player.isFlying()) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        
        // Verificar cooldown real
        if (isGlideOnCooldown(uuid)) return;
        
        // Detectar caída mortal (configurable)
        double fallDistance = player.getFallDistance();
        
        if (fallDistance >= vueloAlturaMinima && player.getVelocity().getY() < -0.5) {
            activateEmergencyGlide(player);
        }
    }
    
    private void activateEmergencyGlide(Player player) {
        UUID uuid = player.getUniqueId();
        playersGliding.add(uuid);
        setGlideCooldown(uuid);
        trackSkillUsage(uuid, Skill.VUELO_EMERGENCIA);
        
        player.sendMessage("§6§l⚡ §eVuelo de Emergencia activado!");
        player.playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, 1.0f, 1.0f);
        
        // Ralentizar la caída
        Vector velocity = player.getVelocity();
        velocity.setY(-0.5);
        player.setVelocity(velocity);
        
        // Efecto de planeo (duración configurable)
        player.setGliding(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, vueloDuracionTicks, 0, true, true));
        
        // Partículas mejoradas
        spawnGlideParticles(player);
        
        // Terminar después del tiempo configurado
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            playersGliding.remove(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.setGliding(false);
                p.sendMessage("§7Vuelo de Emergencia terminado. §8(Cooldown: " + vueloCooldownSegundos + "s)");
            }
        }, vueloDuracionTicks);
    }
    
    private void spawnGlideParticles(Player player) {
        Location loc = player.getLocation();
        // Partículas de nube más espectaculares
        player.getWorld().spawnParticle(Particle.CLOUD, loc, 20, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.END_ROD, loc.add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
        player.getWorld().spawnParticle(Particle.FIREWORK, loc, 5, 0.2, 0.2, 0.2, 0.02);
    }
    
    // ==================== FÉNIX ====================
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        
        // === VOID STORAGE - No dropear items ===
        if (hasSkillCached(uuid, Skill.VOID_STORAGE)) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setKeepLevel(true);
            event.setDroppedExp(0);
            player.sendMessage("§d§l✦ §5Void Storage protegió tu inventario!");
            trackSkillUsage(uuid, Skill.VOID_STORAGE);
        }
        
        // === FÉNIX ===
        if (hasSkillCached(uuid, Skill.FENIX)) {
            if (skillService.isPhoenixReady(player)) {
                // Marcar para revivir
                phoenixRevive.put(uuid, player.getLocation().clone());
                skillService.usePhoenix(player);
                trackSkillUsage(uuid, Skill.FENIX);
                
                event.setDeathMessage(null);
                player.sendMessage("§6§l✦ §e¡Fénix activado! Revivirás en tu ubicación...");
                
                // Efectos visuales de muerte épica
                spawnPhoenixDeathParticles(player.getLocation());
            }
        }
    }
    
    private void spawnPhoenixDeathParticles(Location loc) {
        loc.getWorld().spawnParticle(Particle.FLAME, loc.add(0, 1, 0), 100, 1, 1, 1, 0.1);
        loc.getWorld().spawnParticle(Particle.LAVA, loc, 20, 0.5, 0.5, 0.5, 0);
        loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_DEATH, 1.0f, 0.5f);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        Location reviveLocation = phoenixRevive.remove(uuid);
        if (reviveLocation == null) return;
        
        // Respawnear en la ubicación de muerte
        event.setRespawnLocation(reviveLocation);
        
        // Aplicar efectos después de respawn
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.setHealth(fenixCorazonesRevivir * 2.0); // Corazones configurables
                p.setFoodLevel(fenixComidaRevivir);
                
                // Reaplicar habilidades después del respawn
                skillService.applySkillEffects(p);
                
                // Efectos visuales mejorados
                spawnPhoenixReviveParticles(p);
                
                Bukkit.broadcastMessage("§6§l✦ §e" + p.getName() + " §fha renacido de las cenizas!");
                
                long remaining = skillService.getPhoenixCooldownRemaining(p);
                long hours = remaining / (60 * 60 * 1000);
                p.sendMessage("§7Fénix en cooldown por §e" + hours + " horas§7.");
            }
        }, 1L);
    }
    
    private void spawnPhoenixReviveParticles(Player player) {
        Location loc = player.getLocation();
        // Espiral de fuego ascendente
        for (int i = 0; i < 50; i++) {
            double angle = i * 0.25;
            double x = Math.cos(angle) * 0.5;
            double z = Math.sin(angle) * 0.5;
            double y = i * 0.05;
            loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(x, y, z), 2, 0.1, 0.1, 0.1, 0.01);
        }
        loc.getWorld().spawnParticle(Particle.LAVA, loc.add(0, 1, 0), 30, 0.5, 1, 0.5, 0);
        player.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
        player.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
    }
    
    // ==================== HAMBRE ====================
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        UUID uuid = player.getUniqueId();
        
        // Solo afectar pérdida de hambre
        if (event.getFoodLevel() >= player.getFoodLevel()) return;
        
        double reduction = 0;
        Skill usedSkill = null;
        
        if (/* hasSkillCached(uuid, Skill.METABOLISMO_LENTO) */ false) {
            reduction = metabolismoLentoReduccion;
            usedSkill = null; // Skill.METABOLISMO_LENTO;
        } else if (/* hasSkillCached(uuid, Skill.ESTOMAGO_HIERRO) */ false) {
            reduction = estomagoHierroReduccion;
            usedSkill = null; // Skill.ESTOMAGO_HIERRO;
        }
        
        if (reduction > 0) {
            if (Math.random() < reduction) {
                event.setCancelled(true);
                if (usedSkill != null) {
                    trackSkillUsage(uuid, usedSkill);
                }
            }
        }
    }
    
    // ==================== MINERÍA Y AUTO-RECOLECCIÓN ====================
    
    // Set para evitar recursión infinita en leñador nato
    private final Set<UUID> processingTreeFell = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Block block = event.getBlock();
        ItemStack tool = player.getInventory().getItemInMainHand();
        
        // === LEÑADOR (3 NIVELES: Nato, Experto, Maestro) ===
        if (isLog(block.getType()) && !processingTreeFell.contains(uuid)) {
            // Determinar el nivel más alto de leñador que tiene
            int lenadorLevel = 0;
            Skill lenadorSkill = null;
            int cooldown = 0;
            int maxBloques = 0;
            int xpBonus = 0;
            
            if (hasSkillCached(uuid, Skill.LENADOR_MAESTRO) && 
                isSkillEnabledCached(uuid, Skill.LENADOR_MAESTRO)) {
                lenadorLevel = 3;
                lenadorSkill = Skill.LENADOR_MAESTRO;
                cooldown = lenadorCooldownNivel3; // 0 = sin cooldown
                maxBloques = lenadorMaxBloquesNivel3;
                xpBonus = lenadorXpBonusMaestro;
            } else if (hasSkillCached(uuid, Skill.LENADOR_EXPERTO) && 
                       isSkillEnabledCached(uuid, Skill.LENADOR_EXPERTO)) {
                lenadorLevel = 2;
                lenadorSkill = Skill.LENADOR_EXPERTO;
                cooldown = lenadorCooldownNivel2; // 2 segundos
                maxBloques = lenadorMaxBloquesNivel2;
            } else if (hasSkillCached(uuid, Skill.LENADOR_NATO) && 
                       isSkillEnabledCached(uuid, Skill.LENADOR_NATO)) {
                lenadorLevel = 1;
                lenadorSkill = Skill.LENADOR_NATO;
                cooldown = lenadorCooldownNivel1; // 5 segundos
                maxBloques = lenadorMaxBloquesNivel1;
            }
            
            if (lenadorLevel > 0 && lenadorSkill != null) {
                // Mejora 1: Desactivar si está agachado
                if (lenadorDesactivarSneaking && player.isSneaking()) {
                    // Talar solo este bloque normalmente
                } else if (isAxe(tool.getType())) {
                    // Mejora 2: Verificar cooldown (nivel 3 no tiene)
                    long now = System.currentTimeMillis();
                    Long cooldownEnds = lenadorCooldowns.get(uuid);
                    
                    if (cooldown > 0 && cooldownEnds != null && now < cooldownEnds) {
                        long remaining = (cooldownEnds - now) / 1000;
                        if (canSendMessage(uuid, "lenador_cooldown")) {
                            player.sendMessage("§c§l🪓 §cEspera §e" + remaining + "s §cpara volver a talar árboles completos");
                        }
                    } else {
                        // Mejora 4: Verificar que sea un árbol real (tiene hojas cercanas)
                        if (lenadorVerificarArbolReal && !isRealTree(block)) {
                            // No es un árbol real (posiblemente estructura de jugador)
                            // Talar solo este bloque
                        } else {
                            // Marcar como procesando para evitar recursión
                            processingTreeFell.add(uuid);
                            
                            try {
                                // Guardar ubicación base para replant
                                Location baseLocation = block.getLocation().clone();
                                Material logType = block.getType();
                                
                                // Talar el árbol completo (límite según nivel)
                                final int levelMaxBloques = maxBloques;
                                int blocksFelled = fellTreeWithLimit(player, block, tool, levelMaxBloques);
                                
                                if (blocksFelled > 1) {
                                    trackSkillUsage(uuid, lenadorSkill);
                                    
                                    // Aplicar cooldown solo si el nivel lo tiene
                                    if (cooldown > 0) {
                                        lenadorCooldowns.put(uuid, now + (cooldown * 1000L));
                                    }
                                    
                                    // Mejora 5: Auto-replant (siempre para nivel 3, configurable para otros)
                                    boolean shouldReplant = (lenadorLevel == 3) || lenadorAutoReplant;
                                    if (shouldReplant) {
                                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                            plantSapling(baseLocation, logType);
                                        }, 5L); // Pequeño delay para que se vea natural
                                    }
                                    
                                    // Mejora 6: Dar XP por árbol (bonus para nivel 3)
                                    int totalXp = lenadorXpPorArbolBase + xpBonus;
                                    if (totalXp > 0) {
                                        giveSkillXp(player, lenadorSkill, totalXp);
                                    }
                                    
                                    // Mensaje con anti-spam según nivel
                                    if (canSendMessage(uuid, "lenador")) {
                                        String replantMsg = shouldReplant ? " §2(+retoño)" : "";
                                        String xpMsg = totalXp > 0 ? " §b(+" + totalXp + " XP)" : "";
                                        String levelName = lenadorLevel == 3 ? "§6Leñador Maestro" : 
                                                          (lenadorLevel == 2 ? "§eLeñador Experto" : "§aLeñador Nato");
                                        String noCooldownMsg = lenadorLevel == 3 ? " §d[∞]" : "";
                                        player.sendMessage("§a§l🪓 " + levelName + "! §7Talaste §a" + blocksFelled + " §7troncos" + replantMsg + xpMsg + noCooldownMsg);
                                    }
                                    
                                    // Sonido final
                                    if (!lenadorSonidosProgresivos) {
                                        player.playSound(block.getLocation(), Sound.BLOCK_WOOD_BREAK, 1.0f, 0.8f);
                                    }
                                }
                            } finally {
                                processingTreeFell.remove(uuid);
                            }
                        }
                    }
                }
            }
        }
        
        // === AUTO-RECOLECCIÓN ===
        if (hasSkillCached(uuid, Skill.AUTO_RECOLECCION) && 
            isSkillEnabledCached(uuid, Skill.AUTO_RECOLECCION)) {
            
            // Si está en modo creativo, ignorar
            if (player.getGameMode() == GameMode.CREATIVE) return;
            
            // Obtener los drops respetando el encantamiento de la herramienta
            Collection<ItemStack> drops = block.getDrops(tool, player);
            
            // Si no hay drops (ej: rompiendo con herramienta incorrecta), ignorar
            if (drops.isEmpty()) return;
            
            // Cancelar drops normales
            event.setDropItems(false);
            
            // Dar los items directamente al jugador
            int itemsCollected = 0;
            for (ItemStack drop : drops) {
                if (drop == null || drop.getType() == Material.AIR) continue;
                
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(drop.clone());
                itemsCollected += drop.getAmount();
                
                // Si no cabe, dropearlo normalmente
                for (ItemStack item : leftover.values()) {
                    block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), item);
                    itemsCollected -= item.getAmount();
                }
            }
            
            // También dar experiencia si aplica
            int expToDrop = event.getExpToDrop();
            if (expToDrop > 0) {
                player.giveExp(expToDrop);
                event.setExpToDrop(0);
            }
            
            // Efecto visual sutil y mensaje con anti-spam
            if (itemsCollected > 0) {
                trackSkillUsage(uuid, Skill.AUTO_RECOLECCION);
                if (Math.random() < 0.1) {
                    player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, 
                        block.getLocation().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0);
                }
            }
        }
        
        // === TOQUE DE FORTUNA (después de auto-recolección) ===
        if (hasSkillCached(uuid, Skill.TOQUE_FORTUNA) && isOre(block.getType())) {
            if (Math.random() < toqueFortunaChance) {
                // +10% drop extra
                Collection<ItemStack> bonusDrops = block.getDrops(tool, player);
                for (ItemStack drop : bonusDrops) {
                    if (drop == null || drop.getType() == Material.AIR) continue;
                    
                    if (hasSkillCached(uuid, Skill.AUTO_RECOLECCION) && 
                        isSkillEnabledCached(uuid, Skill.AUTO_RECOLECCION)) {
                        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(drop.clone());
                        for (ItemStack item : leftover.values()) {
                            block.getWorld().dropItemNaturally(block.getLocation(), item);
                        }
                    } else {
                        block.getWorld().dropItemNaturally(block.getLocation(), drop);
                    }
                }
                trackSkillUsage(uuid, Skill.TOQUE_FORTUNA);
                // Mensaje con anti-spam
                if (canSendMessage(uuid, "toque_fortuna")) {
                    player.sendMessage("§a§l⚡ §a¡Toque de Fortuna! (+drop extra)");
                }
            }
        }
        
        // === TOQUE DE SEDA NATURAL ===
        if (hasSkillCached(uuid, Skill.SEDA_NATURAL)) {
            if (Math.random() < sedaNaturalChance && canSilkTouch(block.getType())) {
                // chance de silk touch configurable
                if (!hasSkillCached(uuid, Skill.AUTO_RECOLECCION) || 
                    !isSkillEnabledCached(uuid, Skill.AUTO_RECOLECCION)) {
                    event.setDropItems(false);
                }
                ItemStack silkDrop = new ItemStack(block.getType());
                if (hasSkillCached(uuid, Skill.AUTO_RECOLECCION) && 
                    isSkillEnabledCached(uuid, Skill.AUTO_RECOLECCION)) {
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(silkDrop);
                    for (ItemStack item : leftover.values()) {
                        block.getWorld().dropItemNaturally(block.getLocation(), item);
                    }
                } else {
                    block.getWorld().dropItemNaturally(block.getLocation(), silkDrop);
                }
                trackSkillUsage(uuid, Skill.SEDA_NATURAL);
                // Mensaje con anti-spam
                if (canSendMessage(uuid, "seda_natural")) {
                    player.sendMessage("§d✦ §fToque de Seda Natural!");
                }
            }
        }
    }
    
    // Recoger items cercanos para auto-recolección (items que no son de minado)
    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        UUID uuid = player.getUniqueId();
        if (!hasSkillCached(uuid, Skill.AUTO_RECOLECCION)) return;
        if (!isSkillEnabledCached(uuid, Skill.AUTO_RECOLECCION)) return;
        
        // Aumentar el rango de recogida de items
        Item item = event.getItem();
        if (processedItems.contains(item.getUniqueId())) return;
        
        // Marcar como procesado
        processedItems.add(item.getUniqueId());
    }
    
    private boolean isOre(Material material) {
        return switch (material) {
            case COAL_ORE, DEEPSLATE_COAL_ORE,
                 IRON_ORE, DEEPSLATE_IRON_ORE,
                 GOLD_ORE, DEEPSLATE_GOLD_ORE,
                 DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                 EMERALD_ORE, DEEPSLATE_EMERALD_ORE,
                 LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                 REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE,
                 COPPER_ORE, DEEPSLATE_COPPER_ORE,
                 NETHER_GOLD_ORE, NETHER_QUARTZ_ORE,
                 ANCIENT_DEBRIS -> true;
            default -> false;
        };
    }
    
    private boolean canSilkTouch(Material material) {
        return switch (material) {
            case STONE, COBBLESTONE, GRASS_BLOCK, DIRT, 
                 COAL_ORE, DEEPSLATE_COAL_ORE,
                 DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                 EMERALD_ORE, DEEPSLATE_EMERALD_ORE,
                 LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                 REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE,
                 GLASS, GLASS_PANE, ICE, BLUE_ICE, PACKED_ICE,
                 GLOWSTONE, SEA_LANTERN -> true;
            default -> false;
        };
    }
    
    // ==================== LEÑADOR NATO (Tree Feller) ====================
    
    private boolean isLog(Material material) {
        return switch (material) {
            case OAK_LOG, SPRUCE_LOG, BIRCH_LOG, JUNGLE_LOG, 
                 ACACIA_LOG, DARK_OAK_LOG, MANGROVE_LOG, CHERRY_LOG,
                 CRIMSON_STEM, WARPED_STEM,
                 STRIPPED_OAK_LOG, STRIPPED_SPRUCE_LOG, STRIPPED_BIRCH_LOG,
                 STRIPPED_JUNGLE_LOG, STRIPPED_ACACIA_LOG, STRIPPED_DARK_OAK_LOG,
                 STRIPPED_MANGROVE_LOG, STRIPPED_CHERRY_LOG,
                 STRIPPED_CRIMSON_STEM, STRIPPED_WARPED_STEM -> true;
            default -> false;
        };
    }
    
    private boolean isLeaves(Material material) {
        return switch (material) {
            case OAK_LEAVES, SPRUCE_LEAVES, BIRCH_LEAVES, JUNGLE_LEAVES,
                 ACACIA_LEAVES, DARK_OAK_LEAVES, MANGROVE_LEAVES, CHERRY_LEAVES,
                 AZALEA_LEAVES, FLOWERING_AZALEA_LEAVES,
                 NETHER_WART_BLOCK, WARPED_WART_BLOCK -> true;
            default -> false;
        };
    }
    
    private boolean isAxe(Material material) {
        return switch (material) {
            case WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, 
                 DIAMOND_AXE, NETHERITE_AXE -> true;
            default -> false;
        };
    }
    
    /**
     * Tala un árbol completo empezando desde el tronco dado (con límite personalizado).
     * MEJORADO: Detección vertical mejorada para árboles altos y árboles con ramas
     * @return Número de bloques talados
     */
    private int fellTreeWithLimit(Player player, Block startBlock, ItemStack tool, int maxBlocks) {
        Set<Block> logsToBreak = new LinkedHashSet<>();
        Set<Block> visited = new HashSet<>();
        
        // Mejora 3: Usar PriorityQueue para priorizar bloques más altos (talar de abajo hacia arriba se ve mejor)
        PriorityQueue<Block> queue = new PriorityQueue<>((a, b) -> Integer.compare(b.getY(), a.getY()));
        
        queue.add(startBlock);
        visited.add(startBlock);
        
        // Usar límite pasado por parámetro
        final int MAX_BLOCKS = maxBlocks;
        
        // Buscar todos los troncos conectados (BFS con prioridad arriba)
        while (!queue.isEmpty() && logsToBreak.size() < MAX_BLOCKS) {
            Block current = queue.poll();
            
            if (isLog(current.getType())) {
                logsToBreak.add(current);
                
                // MEJORA: Búsqueda optimizada para árboles muy altos y complejos
                // Arriba: área 5x5 (árboles con ramas extensas)
                // Mismo nivel y abajo: 3x3 (troncos gruesos 2x2 como dark oak)
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dy = -1; dy <= 4; dy++) { // MEJORADO: dy va hasta +4 para árboles muy altos
                        for (int dz = -2; dz <= 2; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            
                            // MEJORA: Para bloques arriba (dy > 0), buscar en área más amplia para detectar ramas
                            int searchRadius = (dy > 0) ? 3 : 1; // Incrementado de 2 a 3
                            
                            // Solo aplicar radio ampliado si estamos buscando arriba
                            if (dy > 0 && (Math.abs(dx) > 1 || Math.abs(dz) > 1)) {
                                // Buscar ramas diagonales solo hacia arriba
                                for (int rdx = -searchRadius; rdx <= searchRadius; rdx++) {
                                    for (int rdz = -searchRadius; rdz <= searchRadius; rdz++) {
                                        if (rdx == 0 && rdz == 0) continue;
                                        Block branch = current.getRelative(rdx, dy, rdz);
                                        
                                        if (!visited.contains(branch)) {
                                            visited.add(branch);
                                            if (isLog(branch.getType())) {
                                                queue.add(branch);
                                            }
                                        }
                                    }
                                }
                                continue; // Saltar la búsqueda normal para este dy
                            }
                            
                            Block neighbor = current.getRelative(dx, dy, dz);
                            
                            if (!visited.contains(neighbor)) {
                                visited.add(neighbor);
                                
                                // Solo agregar troncos a la cola
                                if (isLog(neighbor.getType())) {
                                    queue.add(neighbor);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Romper todos los troncos encontrados (excepto el original que se rompe normalmente)
        int brokenCount = 0;
        UUID uuid = player.getUniqueId();
        boolean hasAutoRecoleccion = hasSkillCached(uuid, Skill.AUTO_RECOLECCION) && 
                                      isSkillEnabledCached(uuid, Skill.AUTO_RECOLECCION);
        
        for (Block log : logsToBreak) {
            // El bloque original se rompe por el evento normal
            if (log.equals(startBlock)) {
                brokenCount++;
                continue;
            }
            
            // Obtener drops
            Collection<ItemStack> drops = log.getDrops(tool, player);
            
            // Romper el bloque
            log.setType(Material.AIR);
            brokenCount++;
            
            // Mejora 7: Sonidos progresivos
            if (lenadorSonidosProgresivos && brokenCount % 4 == 0) {
                float pitch = 0.8f + (brokenCount * 0.02f); // Pitch aumenta gradualmente
                player.playSound(log.getLocation(), Sound.BLOCK_WOOD_BREAK, 0.5f, Math.min(pitch, 1.5f));
            }
            
            // Dar drops al jugador o dropearlos
            for (ItemStack drop : drops) {
                if (drop == null || drop.getType() == Material.AIR) continue;
                
                if (hasAutoRecoleccion && player.getGameMode() != GameMode.CREATIVE) {
                    // Dar directamente al inventario
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(drop.clone());
                    // Si no cabe, dropear
                    for (ItemStack item : leftover.values()) {
                        log.getWorld().dropItemNaturally(log.getLocation().add(0.5, 0.5, 0.5), item);
                    }
                } else {
                    // Dropear normalmente
                    log.getWorld().dropItemNaturally(log.getLocation().add(0.5, 0.5, 0.5), drop);
                }
            }
            
            // Aplicar daño a la herramienta (configurable)
            int danoInterval = lenadorDanoHerramientaCada > 0 ? lenadorDanoHerramientaCada : 1;
            if (tool.getType().getMaxDurability() > 0 && brokenCount % danoInterval == 0) {
                applyToolDamage(player, tool);
            }
            
            // Partículas sutiles
            if (brokenCount % 5 == 0) {
                log.getWorld().spawnParticle(Particle.BLOCK, 
                    log.getLocation().add(0.5, 0.5, 0.5), 
                    5, 0.2, 0.2, 0.2, 0.01, 
                    log.getBlockData());
            }
        }
        
        return brokenCount;
    }
    
    /**
     * Aplica daño a la herramienta respetando Unbreaking
     */
    private void applyToolDamage(Player player, ItemStack tool) {
        if (tool == null || tool.getType().getMaxDurability() == 0) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;
        
        // Obtener nivel de Unbreaking
        int unbreakingLevel = tool.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.UNBREAKING);
        
        // Chance de no aplicar daño (basado en Unbreaking)
        // Fórmula de Minecraft: 100/(level+1)% chance de aplicar daño
        if (unbreakingLevel > 0) {
            double chance = 1.0 / (unbreakingLevel + 1);
            if (Math.random() > chance) {
                return; // No aplicar daño
            }
        }
        
        // Aplicar daño
        if (tool.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damageable) {
            int newDamage = damageable.getDamage() + 1;
            
            if (newDamage >= tool.getType().getMaxDurability()) {
                // Herramienta rota
                player.getInventory().setItemInMainHand(null);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            } else {
                damageable.setDamage(newDamage);
                tool.setItemMeta(damageable);
            }
        }
    }
    
    /**
     * Mejora 4: Verifica si un bloque es parte de un árbol real (tiene hojas cercanas).
     * Evita talar estructuras de madera hechas por jugadores.
     * MEJORADO: Búsqueda vertical más inteligente y priorización de hojas arriba
     */
    private boolean isRealTree(Block logBlock) {
        int leavesCount = 0;
        int radius = lenadorRadioBuscarHojas;
        
        // MEJORA 1: Búsqueda optimizada para árboles muy altos (hasta 32 bloques de altura)
        // Esto detecta mejor árboles gigantes y estructuras naturales complejas
        for (int y = 0; y <= Math.max(radius * 3, 32); y++) { // MEJORADO: búsqueda hasta 32 bloques arriba
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    Block check = logBlock.getRelative(x, y, z);
                    if (isLeaves(check.getType())) {
                        leavesCount++;
                        // MEJORA 2: Si encontramos hojas arriba rápidamente, es definitivamente un árbol
                        if (y > 0 && leavesCount >= 1) { // MEJORADO: reducido a 1 hoja para ser más flexible
                            return true; // Hojas arriba = árbol real
                        }
                        if (leavesCount >= lenadorMinHojasRequeridas) {
                            return true; // Suficientes hojas encontradas
                        }
                    }
                }
            }
        }
        
        // MEJORA 3: Buscar también un poco abajo (para árboles con base de hojas)
        for (int y = -1; y >= -2; y--) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    Block check = logBlock.getRelative(x, y, z);
                    if (isLeaves(check.getType())) {
                        leavesCount++;
                        if (leavesCount >= lenadorMinHojasRequeridas) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return leavesCount >= lenadorMinHojasRequeridas;
    }
    
    /**
     * Mejora 5: Planta un retoño apropiado donde estaba la base del árbol.
     */
    private void plantSapling(Location location, Material logType) {
        Block block = location.getBlock();
        
        // Verificar que el bloque está vacío (aire)
        if (block.getType() != Material.AIR) return;
        
        // Verificar que hay tierra debajo
        Block below = block.getRelative(0, -1, 0);
        if (!canPlantSapling(below.getType())) return;
        
        // Obtener el tipo de retoño correspondiente
        Material sapling = getSaplingForLog(logType);
        if (sapling != null) {
            block.setType(sapling);
        }
    }
    
    /**
     * Verifica si se puede plantar un retoño sobre este bloque.
     */
    private boolean canPlantSapling(Material material) {
        return switch (material) {
            case DIRT, GRASS_BLOCK, PODZOL, COARSE_DIRT, ROOTED_DIRT,
                 MUD, MUDDY_MANGROVE_ROOTS, MOSS_BLOCK,
                 MYCELIUM, FARMLAND -> true;
            // Nether stems en bloques de Nether
            case CRIMSON_NYLIUM, WARPED_NYLIUM, NETHERRACK, SOUL_SOIL -> true;
            default -> false;
        };
    }
    
    /**
     * Obtiene el tipo de retoño correspondiente a un tipo de tronco.
     */
    private Material getSaplingForLog(Material logType) {
        return switch (logType) {
            case OAK_LOG, STRIPPED_OAK_LOG -> Material.OAK_SAPLING;
            case SPRUCE_LOG, STRIPPED_SPRUCE_LOG -> Material.SPRUCE_SAPLING;
            case BIRCH_LOG, STRIPPED_BIRCH_LOG -> Material.BIRCH_SAPLING;
            case JUNGLE_LOG, STRIPPED_JUNGLE_LOG -> Material.JUNGLE_SAPLING;
            case ACACIA_LOG, STRIPPED_ACACIA_LOG -> Material.ACACIA_SAPLING;
            case DARK_OAK_LOG, STRIPPED_DARK_OAK_LOG -> Material.DARK_OAK_SAPLING;
            case CHERRY_LOG, STRIPPED_CHERRY_LOG -> Material.CHERRY_SAPLING;
            case MANGROVE_LOG, STRIPPED_MANGROVE_LOG -> Material.MANGROVE_PROPAGULE;
            case CRIMSON_STEM, STRIPPED_CRIMSON_STEM -> Material.CRIMSON_FUNGUS;
            case WARPED_STEM, STRIPPED_WARPED_STEM -> Material.WARPED_FUNGUS;
            default -> null;
        };
    }
    
    /**
     * Mejora 6: Da XP de habilidad al jugador por usar leñador nato.
     */
    private void giveSkillXp(Player player, Skill skill, int amount) {
        // Usar el sistema de XP existente del plugin
        try {
            // Si existe un sistema de XP de habilidades, usarlo
            // Por ahora, simplemente dar XP vanilla relacionada con el uso
            // Esto se puede expandir para integrarse con un sistema de progresión de skills
            
            // Alternativa: Notificar que se ganó XP (para futuro sistema)
            // Por ahora registramos en stats
            UUID uuid = player.getUniqueId();
            String key = skill.name() + "_xp_gained";
            playerSkillStats.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .merge(key, amount, Integer::sum);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error dando XP de skill: " + e.getMessage());
        }
    }
    
    // ==================== NADADOR ====================
    
    public void applySwimBoost(Player player) {
        UUID uuid = player.getUniqueId();
        if (!hasSkillCached(uuid, Skill.NADADOR)) return;
        if (!isSkillEnabledCached(uuid, Skill.NADADOR)) return;
        
        if (player.isInWater()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, nadadorDuracionEfecto, 0, true, false));
            trackSkillUsage(uuid, Skill.NADADOR);
        }
    }
    
    // ==================== BRANQUIAS / ANFIBIO ====================
    
    public void applyWaterBreathing(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (hasSkillCached(uuid, Skill.ANFIBIO)) {
            if (player.isInWater()) {
                player.setRemainingAir(player.getMaximumAir());
                trackSkillUsage(uuid, Skill.ANFIBIO);
            }
        } else if (hasSkillCached(uuid, Skill.BRANQUIAS)) {
            if (player.isInWater() && player.getRemainingAir() < player.getMaximumAir()) {
                // Restaurar aire más lento que perderlo
                int newAir = Math.min(player.getRemainingAir() + 30, player.getMaximumAir());
                player.setRemainingAir(newAir);
                trackSkillUsage(uuid, Skill.BRANQUIAS);
            }
        }
    }
    
    // ==================== WAYPOINT (EXPLORADOR) - CON PERSISTENCIA Y MULTIPLES WAYPOINTS ====================
    
    // Mapa de waypoints: UUID -> (nombre -> Location)
    private final Map<UUID, Map<String, Location>> playerWaypoints = new ConcurrentHashMap<>();
    
    /**
     * Obtiene el límite de waypoints para un jugador según su rango permanente y habilidades
     * MEJORADO: Todos los jugadores tienen acceso básico
     * IMPORTANTE: Este límite solo aplica para CREAR nuevos waypoints.
     * Los waypoints existentes se conservan incluso si exceden el límite actual.
     */
    public int getWaypointLimit(Player player) {
        UUID uuid = player.getUniqueId();
        var permRank = plugin.getPermRankManager().getPlayerPermRank(uuid);
        
        // Si tiene el rango hunter_adventurer, puede tener hasta 15 waypoints (prioridad máxima)
        if (permRank != null && permRank.getId().equalsIgnoreCase("hunter_adventurer")) {
            return 15;
        }
        
        // Si tiene la habilidad WAYPOINT comprada, puede tener 5 waypoints
        if (plugin.getSkillService().hasSkill(uuid, Skill.WAYPOINT)) {
            return 5;
        }
        
        // MEJORADO: Sin habilidad comprada: 2 waypoints (1 personalizable + bed automático)
        return 2;
    }
    
    /**
     * Establece un waypoint con nombre en la ubicación actual del jugador.
     */
    public void setWaypoint(Player player, String name) {
        UUID uuid = player.getUniqueId();
        Location loc = player.getLocation();
        
        // Validar nombre del waypoint (alfanumérico + guiones y guiones bajos)
        if (!isValidWaypointName(name)) {
            player.sendMessage("§c✖ §7Nombre inválido. Usa solo letras, números, - y _");
            player.sendMessage("§7Ejemplos: §ecasa§7, §ebase_nether§7, §egranja-1");
            return;
        }
        
        // Validar longitud del nombre
        if (name.length() > 20) {
            player.sendMessage("§c✖ §7Nombre demasiado largo (máximo 20 caracteres).");
            return;
        }
        
        // Verificar ubicación segura
        if (!isSafeWaypointLocation(loc)) {
            player.sendMessage("§c✖ §7Ubicación peligrosa detectada (lava, void, etc).");
            player.sendMessage("§7Muévete a un lugar más seguro antes de crear el waypoint.");
            return;
        }
        
        // Obtener o crear el mapa de waypoints del jugador
        Map<String, Location> waypoints = playerWaypoints.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        
        // Verificar límite si no está actualizando un waypoint existente
        int limit = getWaypointLimit(player);
        if (!waypoints.containsKey(name) && waypoints.size() >= limit) {
            player.sendMessage("§c✖ §7Has alcanzado el límite de waypoints (§e" + limit + "§7).");
            player.sendMessage("§7Usa §e/waypoint delete <nombre> §7para eliminar uno.");
            player.sendMessage("§8§o(Los waypoints existentes se conservan incluso si exceden tu límite actual)");
            return;
        }
        
        // Verificar si es actualización o creación nueva
        boolean isUpdate = waypoints.containsKey(name);
        waypoints.put(name, loc);
        
        // Guardar inmediatamente si persistencia está activa
        if (waypointPersistencia) {
            saveWaypoints();
        }
        
        String worldName = loc.getWorld().getName();
        String action = isUpdate ? "actualizado" : "creado";
        
        player.sendMessage("§a✓ §eWaypoint '§f" + name + "§e' " + action + ":");
        player.sendMessage("  §7Coordenadas: §f" + loc.getBlockX() + "§7, §f" + 
            loc.getBlockY() + "§7, §f" + loc.getBlockZ());
        player.sendMessage("  §7Mundo: §e" + worldName);
        player.sendMessage("  §7Waypoints: §e" + waypoints.size() + "§7/§e" + limit);
        player.sendMessage("§7Usa §e/wp " + name + " §7para teletransportarte.");
        
        player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, isUpdate ? 1.0f : 1.2f);
        
        // Efectos visuales mejorados
        spawnWaypointSetParticles(loc);
        trackSkillUsage(uuid, Skill.WAYPOINT);
    }
    
    /**
     * Método legacy para compatibilidad - usa waypoint por defecto
     */
    public void setWaypoint(Player player) {
        setWaypoint(player, "default");
    }
    
    private void spawnWaypointSetParticles(Location loc) {
        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.05);
        loc.getWorld().spawnParticle(Particle.ENCHANT, loc.clone().add(0, 0.5, 0), 20, 0.3, 0.3, 0.3, 0.5);
    }
    
    /**
     * NUEVO: Teleporta al jugador a su cama (respawn point)
     */
    public void teleportToBed(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Verificar cooldown solo si tiene la habilidad WAYPOINT (sin habilidad = sin cooldown para bed)
        if (plugin.getSkillService().hasSkill(uuid, Skill.WAYPOINT) && isWaypointOnCooldown(uuid)) {
            long remaining = getWaypointCooldownRemaining(uuid) / 1000;
            player.sendMessage("§c✖ §7Debes esperar §e" + remaining + "s §7antes de usar teleport.");
            return;
        }
        
        // Obtener ubicación de la cama
        Location bedLocation = player.getBedSpawnLocation();
        
        if (bedLocation == null) {
            player.sendMessage("§c✖ §7No tienes una cama establecida como punto de respawn.");
            player.sendMessage("§7Duerme en una cama para establecer tu punto de respawn.");
            return;
        }
        
        // Verificar que el mundo de la cama esté cargado
        if (bedLocation.getWorld() == null) {
            player.sendMessage("§c✖ §7El mundo de tu cama no está disponible.");
            return;
        }
        
        // Verificar que la ubicación sea segura
        if (!isSafeWaypointLocation(bedLocation)) {
            player.sendMessage("§c✖ §7Tu cama ya no es segura (obstruida, destruida, etc).");
            player.sendMessage("§7Vuelve a dormir en una cama para actualizar tu punto de respawn.");
            return;
        }
        
        // Teleportar al jugador
        boolean success = player.teleport(bedLocation);
        
        if (!success) {
            player.sendMessage("§c✖ §7Error al teleportarse a tu cama. Intenta de nuevo.");
            return;
        }
        
        // Solo aplicar cooldown si tiene la habilidad WAYPOINT
        if (plugin.getSkillService().hasSkill(uuid, Skill.WAYPOINT)) {
            setWaypointCooldown(uuid);
        }
        
        // Efectos visuales y mensaje
        double distance = player.getLocation().distance(bedLocation);
        player.sendMessage("§a✓ §7Teletransportado a tu §bcama§7.");
        player.sendMessage("§7Distancia recorrida: §e" + Math.round(distance) + " bloques");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
        
        // Efectos visuales especiales para cama
        spawnBedTeleportParticles(bedLocation);
        trackSkillUsage(uuid, Skill.WAYPOINT); // Trackear como uso de waypoint
    }
    
    /**
     * Efectos visuales específicos para teleport a cama
     */
    private void spawnBedTeleportParticles(Location loc) {
        if (loc.getWorld() == null) return;
        
        // Partículas rosas/rojas como una cama
        loc.getWorld().spawnParticle(Particle.HEART, loc.clone().add(0.5, 1, 0.5), 10, 0.5, 0.5, 0.5, 0.1);
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0.5, 0.5, 0.5), 15, 0.8, 0.5, 0.8, 0.05);
        
        // Sonido especial de cama
        loc.getWorld().playSound(loc, Sound.BLOCK_WOOL_PLACE, 0.5f, 0.8f);
    }
    
    /**
     * Teleporta al jugador a un waypoint guardado con nombre.
     */
    public void teleportToWaypoint(Player player, String name) {
        UUID uuid = player.getUniqueId();
        Map<String, Location> waypoints = playerWaypoints.get(uuid);
        
        if (waypoints == null || waypoints.isEmpty()) {
            player.sendMessage("§c✖ §7No tienes waypoints establecidos. Usa §e/waypoint set <nombre> §7primero.");
            return;
        }
        
        Location waypoint = waypoints.get(name);
        
        if (waypoint == null) {
            player.sendMessage("§c✖ §7Waypoint '§f" + name + "§7' no encontrado.");
            player.sendMessage("§7Waypoints disponibles: §e" + String.join("§7, §e", waypoints.keySet()));
            return;
        }
        
        // Verificar cooldown
        if (isWaypointOnCooldown(uuid)) {
            long remaining = getWaypointCooldownRemaining(uuid) / 1000;
            player.sendMessage("§c✖ §7Waypoint en cooldown: §e" + remaining + "s§7 restantes.");
            return;
        }
        
        // [FIX CRÍTICO] Verificar que el mundo siga cargado
        // IMPORTANTE: NO eliminar el waypoint si el mundo no está cargado,
        // ya que podría ser un ciclo inactivo que volverá a estar disponible
        if (waypoint.getWorld() == null) {
            player.sendMessage("§c✖ §7El waypoint '§f" + name + "§7' está en un mundo no cargado actualmente.");
            player.sendMessage("§7Este waypoint se conserva y estará disponible cuando el mundo se cargue.");
            player.sendMessage("§8§o(Puede ser un ciclo anterior - los waypoints NUNCA se eliminan automáticamente)");
            return;
        }
        
        // NUEVA RESTRICCIÓN: Verificar mundo/ciclo
        String currentWorld = player.getWorld().getName();
        String waypointWorld = waypoint.getWorld().getName();
        
        // Solo permitir si:
        // 1. Mismo mundo
        // 2. Es admin (bypass)
        boolean sameWorld = currentWorld.equals(waypointWorld);
        boolean isAdmin = player.hasPermission("apocalipsis.waypoint.bypass") || player.hasPermission("apocalipsis.admin");
        
        if (!sameWorld && !isAdmin) {
            player.sendMessage("§c✖ §7Este waypoint fue creado en: §e" + waypointWorld);
            player.sendMessage("§c✖ §7Solo puedes usarlo desde el mismo mundo/ciclo.");
            player.sendMessage("§7Tu mundo actual: §e" + currentWorld);
            return;
        }
        
        // [SEGURIDAD] Verificar que el destino siga siendo seguro
        if (!isSafeWaypointLocation(waypoint)) {
            player.sendMessage("§c✖ §7El destino del waypoint '§f" + name + "§7' ya no es seguro (lava, void, etc).");
            player.sendMessage("§7Se recomienda eliminarlo y crear uno nuevo: §e/wp delete " + name);
            
            // Mostrar detalles de ubicación para debugging
            if (player.hasPermission("apocalipsis.admin")) {
                player.sendMessage("§8[DEBUG] Ubicación: " + waypoint.getBlockX() + ", " + 
                    waypoint.getBlockY() + ", " + waypoint.getBlockZ());
                player.sendMessage("§8[DEBUG] Bloque pies: " + waypoint.getBlock().getType());
                player.sendMessage("§8[DEBUG] Bloque cabeza: " + waypoint.clone().add(0, 1, 0).getBlock().getType());
            }
            return;
        }
        
        // [FIX] Teleportar al jugador al waypoint
        boolean success = player.teleport(waypoint);
        
        if (!success) {
            player.sendMessage("§c✖ §7Error al teleportarse. El destino podría estar en un chunk no cargado.");
            player.sendMessage("§7Intenta de nuevo en unos segundos.");
            return;
        }
        
        setWaypointCooldown(uuid);
        
        // Efectos visuales y mensaje
        double distance = player.getLocation().distance(waypoint);
        player.sendMessage("§a✓ §7Teletransportado a waypoint '§f" + name + "§7'.");
        player.sendMessage("§7Distancia recorrida: §e" + Math.round(distance) + " bloques");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        
        // Efectos visuales mejorados
        spawnWaypointTeleportParticles(waypoint);
        trackSkillUsage(uuid, Skill.WAYPOINT);
    }
    
    /**
     * Método legacy para compatibilidad - usa waypoint por defecto
     */
    public void teleportToWaypoint(Player player) {
        teleportToWaypoint(player, "default");
    }
    
    /**
     * Lista todos los waypoints de un jugador
     */
    public void listWaypoints(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, Location> waypoints = playerWaypoints.get(uuid);
        
        if (waypoints == null || waypoints.isEmpty()) {
            player.sendMessage("§c✖ §7No tienes waypoints establecidos.");
            player.sendMessage("§7Usa §e/wp set <nombre> §7para crear uno.");
            return;
        }
        
        int limit = getWaypointLimit(player);
        String currentWorld = player.getWorld().getName();
        boolean isAdmin = player.hasPermission("apocalipsis.waypoint.bypass") || player.hasPermission("apocalipsis.admin");
        boolean onCooldown = isWaypointOnCooldown(uuid);
        
        player.sendMessage("§e§l⚑ Tus Waypoints §7(" + waypoints.size() + "/" + limit + ")§e:");
        player.sendMessage("§7Mundo actual: §e" + currentWorld + (isAdmin ? " §8[ADMIN]" : ""));
        
        // Mostrar estado de cooldown
        if (onCooldown) {
            long remaining = getWaypointCooldownRemaining(uuid) / 1000;
            player.sendMessage("§7Cooldown: §c" + remaining + "s §7restantes");
        } else {
            player.sendMessage("§7Cooldown: §a✓ Disponible");
        }
        
        player.sendMessage("");
        
        for (Map.Entry<String, Location> entry : waypoints.entrySet()) {
            Location loc = entry.getValue();
            
            // [FIX] Manejar mundos no cargados (ciclos inactivos)
            String worldName;
            boolean worldLoaded = loc.getWorld() != null;
            
            if (worldLoaded) {
                worldName = loc.getWorld().getName();
            } else {
                // Mundo no cargado - probablemente un ciclo inactivo
                worldName = "§8[Ciclo inactivo]";
            }
            
            boolean available = isAdmin || (worldLoaded && currentWorld.equals(loc.getWorld().getName()));
            String status = available ? "§a✓" : "§c✗";
            
            // Calcular distancia desde posición actual
            double distance = -1;
            if (available && loc.getWorld() != null && loc.getWorld().equals(player.getWorld())) {
                distance = player.getLocation().distance(loc);
            }
            
            player.sendMessage("  " + status + " §f" + entry.getKey() + " §7→ §f" + 
                loc.getBlockX() + "§7, §f" + loc.getBlockY() + "§7, §f" + loc.getBlockZ());
            
            if (distance >= 0) {
                player.sendMessage("    §8Distancia: " + Math.round(distance) + " bloques");
            } else {
                String availMsg = available ? "" : " §c(no disponible ahora)";
                player.sendMessage("    §8Mundo: " + worldName + availMsg);
            }
        }
        
        player.sendMessage("");
        player.sendMessage("§7Usa §e/wp <nombre> §7para teletransportarte.");
        if (!isAdmin) {
            player.sendMessage("§8§oSolo puedes usar waypoints del mundo/ciclo actual");
            player.sendMessage("§8§oLos waypoints de ciclos inactivos se conservan y estarán disponibles al volver");
        }
    }
    
    /**
     * Elimina un waypoint por nombre
     */
    public void deleteWaypoint(Player player, String name) {
        UUID uuid = player.getUniqueId();
        Map<String, Location> waypoints = playerWaypoints.get(uuid);
        
        if (waypoints == null || !waypoints.containsKey(name)) {
            player.sendMessage("§c✖ §7Waypoint '§f" + name + "§7' no encontrado.");
            if (waypoints != null && !waypoints.isEmpty()) {
                player.sendMessage("§7Waypoints disponibles: §e" + String.join("§7, §e", waypoints.keySet()));
            }
            return;
        }
        
        // Obtener info antes de eliminar para mostrarla
        Location loc = waypoints.get(name);
        String worldName = (loc.getWorld() != null ? loc.getWorld().getName() : "?");
        
        waypoints.remove(name);
        
        if (waypoints.isEmpty()) {
            playerWaypoints.remove(uuid);
        }
        
        if (waypointPersistencia) {
            saveWaypoints();
        }
        
        int limit = getWaypointLimit(player);
        player.sendMessage("§a✓ §7Waypoint '§f" + name + "§7' eliminado.");
        player.sendMessage("  §8Era: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + " (" + worldName + ")");
        player.sendMessage("  §7Waypoints restantes: §e" + waypoints.size() + "§7/§e" + limit);
        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 1.0f);
    }
    
    private void spawnWaypointTeleportParticles(Location loc) {
        loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
        loc.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(0, 0.5, 0), 30, 0.3, 0.3, 0.3, 0.5);
    }
    
    public Map<String, Location> getWaypoints(UUID uuid) {
        return playerWaypoints.get(uuid);
    }
    
    public Location getWaypoint(UUID uuid, String name) {
        Map<String, Location> waypoints = playerWaypoints.get(uuid);
        return waypoints != null ? waypoints.get(name) : null;
    }
    
    public boolean hasWaypoint(UUID uuid, String name) {
        Map<String, Location> waypoints = playerWaypoints.get(uuid);
        return waypoints != null && waypoints.containsKey(name);
    }
    
    public boolean hasWaypoints(UUID uuid) {
        Map<String, Location> waypoints = playerWaypoints.get(uuid);
        return waypoints != null && !waypoints.isEmpty();
    }
    
    public void removeWaypoint(UUID uuid, String name) {
        Map<String, Location> waypoints = playerWaypoints.get(uuid);
        if (waypoints != null) {
            waypoints.remove(name);
            if (waypoints.isEmpty()) {
                playerWaypoints.remove(uuid);
            }
            if (waypointPersistencia) {
                saveWaypoints();
            }
        }
    }
    
    // ==================== COOLDOWN WAYPOINT ====================
    
    private boolean isWaypointOnCooldown(UUID uuid) {
        Long cooldownEnd = waypointCooldowns.get(uuid);
        if (cooldownEnd == null) return false;
        return System.currentTimeMillis() < cooldownEnd;
    }
    
    private long getWaypointCooldownRemaining(UUID uuid) {
        Long cooldownEnd = waypointCooldowns.get(uuid);
        if (cooldownEnd == null) return 0;
        return Math.max(0, cooldownEnd - System.currentTimeMillis());
    }
    
    private void setWaypointCooldown(UUID uuid) {
        long cooldownEnd = System.currentTimeMillis() + (waypointCooldownTeleport * 1000L);
        waypointCooldowns.put(uuid, cooldownEnd);
    }
    
    /**
     * Valida que el nombre del waypoint sea seguro (alfanumérico + guiones)
     */
    private boolean isValidWaypointName(String name) {
        if (name == null || name.isEmpty()) return false;
        // Solo permitir letras, números, guiones y guiones bajos
        return name.matches("^[a-zA-Z0-9_-]+$");
    }
    
    /**
     * Verifica que la ubicación sea segura para un waypoint
     */
    private boolean isSafeWaypointLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        
        // Verificar que no esté en el void
        if (loc.getY() < -60) return false;
        
        // Verificar que no esté en el techo del mundo
        if (loc.getY() > loc.getWorld().getMaxHeight() - 5) return false;
        
        // Verificar que no esté en lava
        org.bukkit.Material feetBlock = loc.getBlock().getType();
        org.bukkit.Material headBlock = loc.clone().add(0, 1, 0).getBlock().getType();
        
        if (feetBlock == org.bukkit.Material.LAVA || headBlock == org.bukkit.Material.LAVA) {
            return false;
        }
        
        // Verificar que no esté en fuego
        if (feetBlock == org.bukkit.Material.FIRE || headBlock == org.bukkit.Material.FIRE) {
            return false;
        }
        
        return true;
    }
    
    // ==================== PERSISTENCIA WAYPOINTS ====================
    
    private void loadWaypoints() {
        if (!waypointPersistencia) {
            plugin.getLogger().info("[Skills] Persistencia de waypoints desactivada, omitiendo carga");
            return;
        }
        
        File waypointsFile = new File(plugin.getDataFolder(), "waypoints.yml");
        
        if (!waypointsFile.exists()) {
            plugin.getLogger().info("[Skills] Archivo waypoints.yml no existe todavía. Se creará cuando se guarde el primer waypoint.");
            plugin.getLogger().info("[Skills] Ruta esperada: " + waypointsFile.getAbsolutePath());
            return;
        }
        
        plugin.getLogger().info("[Skills] Cargando waypoints desde: " + waypointsFile.getAbsolutePath());
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(waypointsFile);
        
        if (!config.isConfigurationSection("waypoints")) {
            plugin.getLogger().warning("[Skills] No se encontró la sección 'waypoints' en el archivo.");
            return;
        }
        
        for (String uuidStr : config.getConfigurationSection("waypoints").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String basePath = "waypoints." + uuidStr;
                
                // Verificar si es el formato nuevo (con nombres) o el viejo (sin nombres)
                ConfigurationSection playerSection = config.getConfigurationSection(basePath);
                if (playerSection == null) continue;
                
                Map<String, Location> waypoints = new ConcurrentHashMap<>();
                
                // Si tiene una clave "world" directamente, es formato viejo
                if (playerSection.contains("world")) {
                    // Formato viejo: solo un waypoint sin nombre
                    String worldName = playerSection.getString("world");
                    if (worldName != null) {
                        org.bukkit.World world = Bukkit.getWorld(worldName);
                        if (world != null) {
                            double x = playerSection.getDouble("x");
                            double y = playerSection.getDouble("y");
                            double z = playerSection.getDouble("z");
                            float yaw = (float) playerSection.getDouble("yaw");
                            float pitch = (float) playerSection.getDouble("pitch");
                            
                            waypoints.put("default", new Location(world, x, y, z, yaw, pitch));
                        }
                    }
                } else {
                    // Formato nuevo: múltiples waypoints con nombres
                    for (String waypointName : playerSection.getKeys(false)) {
                        String wpPath = basePath + "." + waypointName;
                        
                        String worldName = config.getString(wpPath + ".world");
                        if (worldName == null) continue;
                        
                        org.bukkit.World world = Bukkit.getWorld(worldName);
                        if (world == null) {
                            plugin.getLogger().warning("[Skills] Mundo no encontrado para waypoint '" + waypointName + "': " + worldName);
                            continue;
                        }
                        
                        double x = config.getDouble(wpPath + ".x");
                        double y = config.getDouble(wpPath + ".y");
                        double z = config.getDouble(wpPath + ".z");
                        float yaw = (float) config.getDouble(wpPath + ".yaw");
                        float pitch = (float) config.getDouble(wpPath + ".pitch");
                        
                        // Leer creation_world si existe (nuevo formato)
                        String creationWorld = config.getString(wpPath + ".creation_world", worldName);
                        
                        waypoints.put(waypointName, new Location(world, x, y, z, yaw, pitch));
                        
                        // Log si hay discrepancia entre mundos (debugging)
                        if (!worldName.equals(creationWorld)) {
                            plugin.getLogger().info("[Skills] Waypoint '" + waypointName + "' migrado: " + creationWorld + " → " + worldName);
                        }
                    }
                }
                
                if (!waypoints.isEmpty()) {
                    playerWaypoints.put(uuid, waypoints);
                }
                
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[Skills] UUID inválido en waypoints.yml: " + uuidStr);
            }
        }
        
        int totalWaypoints = playerWaypoints.values().stream().mapToInt(Map::size).sum();
        plugin.getLogger().info("[Skills] Cargados " + totalWaypoints + " waypoints de " + playerWaypoints.size() + " jugadores");
    }
    
    private void saveWaypoints() {
        if (!waypointPersistencia) {
            plugin.getLogger().info("[Skills] Persistencia de waypoints desactivada, omitiendo guardado");
            return;
        }
        
        FileConfiguration config = new YamlConfiguration();
        int totalWaypoints = 0;
        
        // Agregar metadata
        config.set("metadata.version", "2.0");
        config.set("metadata.last_save", System.currentTimeMillis());
        config.set("metadata.server_time", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        
        for (Map.Entry<UUID, Map<String, Location>> playerEntry : playerWaypoints.entrySet()) {
            String uuidStr = playerEntry.getKey().toString();
            
            for (Map.Entry<String, Location> wpEntry : playerEntry.getValue().entrySet()) {
                String waypointName = wpEntry.getKey();
                Location loc = wpEntry.getValue();
                String path = "waypoints." + uuidStr + "." + waypointName;
                
                if (loc.getWorld() != null) {
                    config.set(path + ".world", loc.getWorld().getName());
                    config.set(path + ".x", loc.getX());
                    config.set(path + ".y", loc.getY());
                    config.set(path + ".z", loc.getZ());
                    config.set(path + ".yaw", loc.getYaw());
                    config.set(path + ".pitch", loc.getPitch());
                    config.set(path + ".created_time", System.currentTimeMillis());
                    config.set(path + ".creation_world", loc.getWorld().getName());
                    totalWaypoints++;
                } else {
                    plugin.getLogger().warning("[Skills] Waypoint '" + waypointName + "' del jugador " + uuidStr + " tiene mundo null, omitiendo");
                }
            }
        }
        
        config.set("metadata.total_waypoints", totalWaypoints);
        
        try {
            // Asegurar que el directorio del plugin existe
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                boolean created = dataFolder.mkdirs();
                plugin.getLogger().info("[Skills] Directorio del plugin creado: " + created);
            }
            
            File waypointsFile = new File(dataFolder, "waypoints.yml");
            
            // Crear el archivo si no existe
            if (!waypointsFile.exists()) {
                boolean fileCreated = waypointsFile.createNewFile();
                plugin.getLogger().info("[Skills] Archivo waypoints.yml creado: " + fileCreated);
            }
            
            config.save(waypointsFile);
            plugin.getLogger().info("[Skills] ✓ Waypoints guardados exitosamente: " + totalWaypoints + " waypoints de " + playerWaypoints.size() + " jugadores en " + waypointsFile.getAbsolutePath());
        } catch (IOException e) {
            plugin.getLogger().severe("[Skills] ✗ Error crítico guardando waypoints: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== COMANDOS PÚBLICOS ====================
    
    /**
     * Maneja el comando de waypoints para administradores
     */
    public boolean handleWaypointCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String[] args) {
        if (!sender.hasPermission("apocalipsis.admin")) {
            sender.sendMessage("§cNo tienes permisos para este comando.");
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage("§e§l⚑ Comandos de Waypoints Admin:");
            sender.sendMessage("  §f/wp save §7- Forzar guardado de waypoints");
            sender.sendMessage("  §f/wp reload §7- Recargar waypoints desde archivo");
            sender.sendMessage("  §f/wp stats §7- Ver estadísticas de waypoints");
            sender.sendMessage("  §f/wp backup §7- Crear respaldo manual");
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "save":
                sender.sendMessage("§e⏳ Guardando waypoints...");
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    saveWaypoints();
                    sender.sendMessage("§a✓ Waypoints guardados exitosamente");
                });
                return true;
                
            case "reload":
                sender.sendMessage("§e⏳ Recargando waypoints...");
                playerWaypoints.clear();
                loadWaypoints();
                sender.sendMessage("§a✓ Waypoints recargados desde archivo");
                return true;
                
            case "stats":
                int totalPlayers = playerWaypoints.size();
                int totalWaypoints = playerWaypoints.values().stream().mapToInt(Map::size).sum();
                
                // Contar waypoints por mundo
                Map<String, Integer> waypointsByWorld = new HashMap<>();
                for (Map<String, Location> playerWps : playerWaypoints.values()) {
                    for (Location loc : playerWps.values()) {
                        if (loc.getWorld() != null) {
                            String worldName = loc.getWorld().getName();
                            waypointsByWorld.put(worldName, waypointsByWorld.getOrDefault(worldName, 0) + 1);
                        }
                    }
                }
                
                sender.sendMessage("§6§l📊 Estadísticas de Waypoints:");
                sender.sendMessage("  §f• Jugadores con waypoints: §e" + totalPlayers);
                sender.sendMessage("  §f• Total de waypoints: §e" + totalWaypoints);
                sender.sendMessage("  §f• Mundos con waypoints: §e" + waypointsByWorld.size());
                sender.sendMessage("  §f• Persistencia activa: " + (waypointPersistencia ? "§a✓" : "§c✗"));
                
                if (totalPlayers > 0) {
                    double promedio = (double) totalWaypoints / totalPlayers;
                    sender.sendMessage("  §f• Promedio por jugador: §e" + String.format("%.1f", promedio));
                    
                    // Mostrar distribución por mundos
                    sender.sendMessage("  §f• Waypoints por mundo:");
                    waypointsByWorld.entrySet().stream()
                        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                        .forEach(entry -> {
                            sender.sendMessage("    §7- §e" + entry.getKey() + "§7: §f" + entry.getValue() + " waypoints");
                        });
                    
                    // Top 5 jugadores con más waypoints
                    sender.sendMessage("  §f• Top jugadores:");
                    playerWaypoints.entrySet().stream()
                        .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                        .limit(5)
                        .forEach(entry -> {
                            String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                            int count = entry.getValue().size();
                            sender.sendMessage("    §7- §f" + playerName + "§7: §e" + count + " waypoints");
                        });
                }
                return true;
                
            case "backup":
                sender.sendMessage("§e⏳ Creando respaldo...");
                try {
                    String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    File backupDir = new File(plugin.getDataFolder(), "backups");
                    backupDir.mkdirs();
                    
                    File sourceFile = new File(plugin.getDataFolder(), "waypoints.yml");
                    if (sourceFile.exists()) {
                        File backupFile = new File(backupDir, "waypoints_" + timestamp + ".yml");
                        java.nio.file.Files.copy(sourceFile.toPath(), backupFile.toPath());
                        sender.sendMessage("§a✓ Respaldo creado: " + backupFile.getName());
                    } else {
                        sender.sendMessage("§c✗ No existe archivo waypoints.yml para respaldar");
                    }
                } catch (Exception e) {
                    sender.sendMessage("§c✗ Error creando respaldo: " + e.getMessage());
                }
                return true;
                
            default:
                sender.sendMessage("§cSubcomando desconocido. Usa §e/wp §cpara ver la ayuda.");
                return true;
        }
    }

    // ==================== MERCADER SUPREMO - DESCUENTOS EN TRADES ====================
    
    /**
     * Listener para aplicar descuentos de MERCADER_SUPREMO cuando el jugador
     * abre el inventario de comercio con un villager.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onVillagerTradeOpen(org.bukkit.event.inventory.InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getInventory().getType() != InventoryType.MERCHANT) return;
        
        UUID uuid = player.getUniqueId();
        
        // Verificar si tiene la habilidad MERCADER_SUPREMO
        if (!hasSkillCached(uuid, Skill.MERCADER_SUPREMO)) return;
        
        // Obtener el merchant (villager)
        if (!(event.getInventory().getHolder() instanceof org.bukkit.inventory.Merchant merchant)) return;
        
        // Obtener el porcentaje de descuento según el nivel
        double descuentoPercent = skillService.getLevelEffect(uuid, Skill.MERCADER_SUPREMO);
        if (descuentoPercent <= 0) return;
        
        // Aplicar descuento a todos los trades del merchant
        applyTradeDiscount(merchant, descuentoPercent);
        
        // Mensaje sutil de confirmación (con anti-spam)
        if (canSendMessage(uuid, "mercader_supremo")) {
            player.sendMessage("§a§l✦ §eMercader Supremo: §7-" + (int)descuentoPercent + "% en trades");
            trackSkillUsage(uuid, Skill.MERCADER_SUPREMO);
        }
    }
    
    /**
     * Aplica el descuento a los trades del merchant.
     * Esto modifica las recetas cuando el jugador abre el trade.
     */
    private void applyTradeDiscount(org.bukkit.inventory.Merchant merchant, double descuentoPercent) {
        List<MerchantRecipe> originalRecipes = merchant.getRecipes();
        if (originalRecipes == null || originalRecipes.isEmpty()) return;
        
        List<MerchantRecipe> discountedRecipes = new ArrayList<>();
        
        for (MerchantRecipe originalRecipe : originalRecipes) {
            // Obtener ingredientes (precio)
            List<ItemStack> ingredients = originalRecipe.getIngredients();
            List<ItemStack> discountedIngredients = new ArrayList<>();
            
            for (ItemStack ingredient : ingredients) {
                if (ingredient == null || ingredient.getType() == Material.AIR) {
                    discountedIngredients.add(ingredient);
                    continue;
                }
                
                // Aplicar descuento solo a esmeraldas (moneda principal)
                if (ingredient.getType() == Material.EMERALD) {
                    int originalAmount = ingredient.getAmount();
                    int discountedAmount = (int) Math.max(1, Math.ceil(originalAmount * (1 - descuentoPercent / 100.0)));
                    
                    ItemStack discountedItem = ingredient.clone();
                    discountedItem.setAmount(discountedAmount);
                    discountedIngredients.add(discountedItem);
                } else {
                    discountedIngredients.add(ingredient.clone());
                }
            }
            
            // Crear nueva receta con precio reducido
            MerchantRecipe newRecipe = new MerchantRecipe(
                originalRecipe.getResult().clone(),
                originalRecipe.getUses(),
                Math.max(originalRecipe.getMaxUses(), 999), // Trades "infinitos" (mínimo 999 usos)
                originalRecipe.hasExperienceReward(),
                originalRecipe.getVillagerExperience(),
                originalRecipe.getPriceMultiplier(),
                originalRecipe.getDemand(),
                Math.max(originalRecipe.getSpecialPrice() - 5, -30) // Reducción adicional de precio especial (límite -30)
            );
            
            newRecipe.setIngredients(discountedIngredients);
            discountedRecipes.add(newRecipe);
        }
        
        // Aplicar las recetas modificadas al merchant
        merchant.setRecipes(discountedRecipes);
    }
    
    /**
     * Listener alternativo para cuando el jugador hace click en un trade.
     * Puede usarse para tracking o efectos adicionales.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTradeClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory().getType() != InventoryType.MERCHANT) return;
        if (event.getSlot() != 2) return; // Slot del resultado del trade
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        
        UUID uuid = player.getUniqueId();
        
        // Verificar si tiene MERCADER_SUPREMO para mostrar efecto visual
        if (hasSkillCached(uuid, Skill.MERCADER_SUPREMO)) {
            // Efecto visual de descuento aplicado
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, 
                player.getLocation().add(0, 1.5, 0), 5, 0.3, 0.3, 0.3, 0);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.5f);
        }
    }
    
    // ==================== MEJORA DE INVOCACIONES - SEGUIMIENTO Y NOTIFICACIONES ====================
    
    /**
     * Detecta cuando una mascota invocada muere y notifica al jugador
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSummonDeath(EntityDeathEvent event) {
        org.bukkit.entity.LivingEntity entity = event.getEntity();
        UUID entityId = entity.getUniqueId();
        
        // Buscar si esta entidad pertenece a algún jugador
        for (Map.Entry<UUID, java.util.List<UUID>> entry : skillService.getAllInvocaciones().entrySet()) {
            UUID playerUuid = entry.getKey();
            java.util.List<UUID> summons = entry.getValue();
            
            if (summons != null && summons.contains(entityId)) {
                // La mascota del jugador murió
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    String entityName = getEntityDisplayName(entity);
                    player.sendMessage("§c§l☠ §7Tu §e" + entityName + " §7ha muerto!");
                    player.playSound(player.getLocation(), Sound.ENTITY_WOLF_WHINE, 0.7f, 1.0f);
                    
                    // Partículas de luto
                    player.getWorld().spawnParticle(Particle.SOUL, 
                        entity.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
                }
                
                // Remover de la lista
                summons.remove(entityId);
                break;
            }
        }
    }
    
    /**
     * Obtiene el nombre amigable de una entidad invocada
     */
    private String getEntityDisplayName(org.bukkit.entity.LivingEntity entity) {
        if (entity.getCustomName() != null) {
            // Extraer tipo de la mascota desde el nombre custom
            if (entity instanceof org.bukkit.entity.Wolf) return "Lobo Compañero";
            if (entity instanceof org.bukkit.entity.Cat) return "Gato Guardián";
            if (entity instanceof org.bukkit.entity.Allay) return "Allay Recolector";
            if (entity instanceof org.bukkit.entity.Bee) return "Abeja Protectora";
            if (entity instanceof org.bukkit.entity.IronGolem) return "Gólem Protector";
            if (entity instanceof org.bukkit.entity.Vex) return "Vex Vengador";
            if (entity instanceof org.bukkit.entity.Warden) return "Warden Temporal";
        }
        return entity.getType().name().replace("_", " ").toLowerCase();
    }
    
    // ==================== VAMPIRISMO - LIFESTEAL Y HEAL ON KILL ====================
    
    // ==================== MODIFICADORES DE DAÑO - COMBATE ====================
    
    /**
     * Modifica el daño infligido según habilidades de combate
     * Habilidades: GOLPE_CERTERO, GUERRERO, EJECUTOR, FURIA, ARQUERO, FRANCOTIRADOR
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombatDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        Player attacker = null;
        boolean isArrow = false;
        double distance = 0;
        
        // Detectar si el daño es de jugador o flecha
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Arrow arrow) {
            if (arrow.getShooter() instanceof Player) {
                attacker = (Player) arrow.getShooter();
                isArrow = true;
                distance = arrow.getLocation().distance(event.getEntity().getLocation());
            }
        }
        
        if (attacker == null) return;
        
        UUID uuid = attacker.getUniqueId();
        LivingEntity victim = (LivingEntity) event.getEntity();
        double baseDamage = event.getDamage();
        double finalMultiplier = 1.0;
        
        // === DAÑO CUERPO A CUERPO ===
        if (!isArrow) {
            // GOLPE_CERTERO: +5% daño base
            if (hasSkillCached(uuid, Skill.GOLPE_CERTERO)) {
                double level = skillService.getLevelEffect(uuid, Skill.GOLPE_CERTERO);
                finalMultiplier += level / 100.0; // 5%, 8%, 12%
                trackSkillUsage(uuid, Skill.GOLPE_CERTERO);
            }
            
            // GUERRERO: +10% daño cuerpo a cuerpo
            if (hasSkillCached(uuid, Skill.GUERRERO)) {
                double level = skillService.getLevelEffect(uuid, Skill.GUERRERO);
                finalMultiplier += level / 100.0; // 10%, 15%, 20%
                trackSkillUsage(uuid, Skill.GUERRERO);
            }
            
            // EJECUTOR: +25% daño a enemigos <30% vida
            if (hasSkillCached(uuid, Skill.EJECUTOR)) {
                double healthPercent = victim.getHealth() / victim.getAttribute(Attribute.MAX_HEALTH).getValue();
                if (healthPercent < 0.30) {
                    double level = skillService.getLevelEffect(uuid, Skill.EJECUTOR);
                    finalMultiplier += level / 100.0; // 25%, 35%, 50%
                    trackSkillUsage(uuid, Skill.EJECUTOR);
                    
                    // Efecto visual de ejecución
                    victim.getWorld().spawnParticle(Particle.CRIT, 
                        victim.getLocation().add(0, victim.getHeight() / 2, 0), 
                        15, 0.3, 0.5, 0.3, 0.1);
                }
            }
            
            // FURIA: Daño aumenta +1% por cada 1% de vida perdida (toggleable)
            if (hasSkillCached(uuid, Skill.FURIA) && isSkillEnabledCached(uuid, Skill.FURIA)) {
                double maxHealth = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
                double currentHealth = attacker.getHealth();
                double healthLostPercent = ((maxHealth - currentHealth) / maxHealth) * 100.0;
                
                double level = skillService.getSkillLevel(uuid, Skill.FURIA).getLevel();
                double furyBonus = healthLostPercent * (level * 0.5); // Lvl1: x1, Lvl2: x1.5, Lvl3: x2
                finalMultiplier += furyBonus / 100.0;
                
                if (furyBonus > 10) { // Solo mostrar si es significativo
                    trackSkillUsage(uuid, Skill.FURIA);
                    attacker.getWorld().spawnParticle(Particle.ANGRY_VILLAGER,
                        attacker.getLocation().add(0, 2, 0), 2, 0.3, 0.3, 0.3, 0);
                }
            }
        }
        
        // === DAÑO CON ARCO/BALLESTA ===
        else {
            // ARQUERO: +10% daño con arcos
            if (hasSkillCached(uuid, Skill.ARQUERO)) {
                double level = skillService.getLevelEffect(uuid, Skill.ARQUERO);
                finalMultiplier += level / 100.0; // 10%, 15%, 25%
                trackSkillUsage(uuid, Skill.ARQUERO);
            }
            
            // FRANCOTIRADOR: +20% daño a distancia >15 bloques
            if (hasSkillCached(uuid, Skill.FRANCOTIRADOR) && distance > 15.0) {
                double level = skillService.getLevelEffect(uuid, Skill.FRANCOTIRADOR);
                finalMultiplier += level / 100.0; // 15%, 25%, 35%
                trackSkillUsage(uuid, Skill.FRANCOTIRADOR);
                
                // Efecto visual de disparo lejano
                victim.getWorld().spawnParticle(Particle.FLAME,
                    victim.getLocation().add(0, victim.getHeight() / 2, 0),
                    10, 0.3, 0.5, 0.3, 0.05);
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.5f, 1.5f);
            }
        }
        
        // Aplicar multiplicador de daño
        if (finalMultiplier != 1.0) {
            event.setDamage(baseDamage * finalMultiplier);
        }
    }
    
    /**
     * MULTISHOT: 15% chance de disparar 2 flechas extra
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMultishot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        UUID uuid = player.getUniqueId();
        
        if (!hasSkillCached(uuid, Skill.MULTISHOT)) return;
        
        // Obtener chance según nivel
        double chance = skillService.getLevelEffect(uuid, Skill.MULTISHOT); // 15%, 25%, 35%
        
        if (Math.random() * 100 < chance) {
            Arrow originalArrow = (Arrow) event.getProjectile();
            
            // Disparar 2 flechas adicionales con ligero ángulo
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (int i = 0; i < 2; i++) {
                    Arrow extraArrow = player.launchProjectile(Arrow.class);
                    extraArrow.setVelocity(originalArrow.getVelocity().clone().rotateAroundY(Math.toRadians((i == 0 ? -10 : 10))));
                    extraArrow.setShooter(player);
                    extraArrow.setPickupStatus(Arrow.PickupStatus.CREATIVE_ONLY);
                    
                    // Nivel 3: Flechas extras causan 100% daño (por defecto serían 50%)
                    int level = skillService.getSkillLevel(uuid, Skill.MULTISHOT).getLevel();
                    if (level < 3) {
                        extraArrow.setDamage(originalArrow.getDamage() * 0.5);
                    }
                }
                
                // Efectos
                player.getWorld().spawnParticle(Particle.FIREWORK,
                    player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.5)),
                    15, 0.2, 0.2, 0.2, 0.05);
                player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.5f, 0.8f);
                
                trackSkillUsage(uuid, Skill.MULTISHOT);
            }, 1L);
        }
    }
    
    /**
     * Aplica lifesteal cuando un jugador daña a una entidad
     * Nivel 1: 5% lifesteal
     * Nivel 2: 8% lifesteal  
     * Nivel 3: 12% lifesteal + heal on kill
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVampirismoHit(EntityDamageByEntityEvent event) {
        // Solo procesar daño de jugador a entidad viviente
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        Player player = (Player) event.getDamager();
        LivingEntity victim = (LivingEntity) event.getEntity();
        
        // Verificar que el jugador tenga vampirismo
        int vampirismoLevel = skillService.getSkillLevel(player.getUniqueId(), Skill.VAMPIRISMO).getLevel();
        if (vampirismoLevel == 0) return;
        
        // Calcular porcentaje de lifesteal según nivel
        double lifestealPercent;
        switch (vampirismoLevel) {
            case 1:
                lifestealPercent = 0.05; // 5%
                break;
            case 2:
                lifestealPercent = 0.08; // 8%
                break;
            case 3:
                lifestealPercent = 0.12; // 12%
                break;
            default:
                return;
        }
        
        // Calcular vida a recuperar (porcentaje del daño final)
        double finalDamage = event.getFinalDamage();
        double healthToRestore = finalDamage * lifestealPercent;
        
        // Aplicar curación sin exceder la vida máxima
        if (healthToRestore > 0) {
            double currentHealth = player.getHealth();
            double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            double newHealth = Math.min(currentHealth + healthToRestore, maxHealth);
            
            player.setHealth(newHealth);
            
            // Efectos visuales de vampirismo
            player.getWorld().spawnParticle(Particle.HEART, 
                player.getLocation().add(0, 2, 0), 
                3, 0.3, 0.3, 0.3, 0);
            player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                victim.getLocation().add(0, victim.getHeight() / 2, 0),
                5, 0.3, 0.3, 0.3, 0);
            
            // Sonido sutil de absorción
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.3f, 1.5f);
            
            // Incrementar estadísticas
            trackSkillUsage(player.getUniqueId(), Skill.VAMPIRISMO);
        }
    }
    
    /**
     * Aplica heal on kill en nivel 3 de vampirismo
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVampirismoKill(EntityDeathEvent event) {
        // Verificar que fue matado por un jugador
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        
        // Verificar que tenga vampirismo nivel 3
        int vampirismoLevel = skillService.getSkillLevel(killer.getUniqueId(), Skill.VAMPIRISMO).getLevel();
        if (vampirismoLevel != 3) return;
        
        // Curar 2 corazones (4 puntos de vida) al matar
        double currentHealth = killer.getHealth();
        double maxHealth = killer.getAttribute(Attribute.MAX_HEALTH).getValue();
        double newHealth = Math.min(currentHealth + 4.0, maxHealth);
        
        killer.setHealth(newHealth);
        
        // Efectos visuales más intensos para kill heal
        killer.getWorld().spawnParticle(Particle.HEART,
            killer.getLocation().add(0, 2, 0),
            8, 0.5, 0.5, 0.5, 0);
        killer.getWorld().spawnParticle(Particle.ENCHANT,
            killer.getLocation().add(0, 1, 0),
            20, 0.5, 1, 0.5, 1);
        
        // Sonido de victoria
        killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);
        
        // Mensaje
        killer.sendMessage("§c❤ §7Vampirismo: §a+2♥");
    }
}