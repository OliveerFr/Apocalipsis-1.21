package me.apocalipsis.skills;

import me.apocalipsis.Apocalipsis;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private int waypointCooldownTeleport = 300;
    private boolean waypointPersistencia = true;
    private long mensajeCooldownMs = 2000;
    private boolean statsEnabled = true;
    private boolean cacheEnabled = true;
    private int cacheTtlSegundos = 30;
    
    // === LEÑADOR CONFIG (3 niveles) ===
    // Nivel 1 (Nato): cooldown 5s, max 256 bloques
    // Nivel 2 (Experto): cooldown 2s, max 384 bloques  
    // Nivel 3 (Maestro): SIN cooldown, max 512 bloques, auto-replant siempre, bonus XP
    private int lenadorCooldownNivel1 = 5;    // Leñador Nato
    private int lenadorCooldownNivel2 = 2;    // Leñador Experto
    private int lenadorCooldownNivel3 = 0;    // Leñador Maestro (sin cooldown)
    private int lenadorMaxBloquesNivel1 = 256;
    private int lenadorMaxBloquesNivel2 = 384;
    private int lenadorMaxBloquesNivel3 = 512;
    private boolean lenadorDesactivarSneaking = true;
    private boolean lenadorAutoReplant = true;
    private boolean lenadorVerificarArbolReal = true;
    private int lenadorRadioBuscarHojas = 4;
    private int lenadorMinHojasRequeridas = 3;
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
        waypointCooldownTeleport = skillsConfig.getInt("waypoints.cooldown_teleport", 300);
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
    }
    
    public void shutdown() {
        // Cancelar tasks
        if (cleanupTaskId != -1) Bukkit.getScheduler().cancelTask(cleanupTaskId);
        if (waterEffectsTaskId != -1) Bukkit.getScheduler().cancelTask(waterEffectsTaskId);
        if (cacheCleanupTaskId != -1) Bukkit.getScheduler().cancelTask(cacheCleanupTaskId);
        if (statsAutoSaveTaskId != -1) Bukkit.getScheduler().cancelTask(statsAutoSaveTaskId);
        
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
        UUID uuid = event.getPlayer().getUniqueId();
        playersGliding.remove(uuid);
        phoenixRevive.remove(uuid);
        skillCache.remove(uuid);
        glideCooldowns.remove(uuid);
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
            
            if (hasSkillCached(uuid, Skill.IGNIFUGO)) {
                reduction = ignifugoReduccion;
                trackSkillUsage(uuid, Skill.IGNIFUGO);
                // Inmune a daño por pisar fuego
                if (cause == EntityDamageEvent.DamageCause.FIRE_TICK) {
                    Block below = player.getLocation().subtract(0, 1, 0).getBlock();
                    if (below.getType() == Material.FIRE || below.getType() == Material.SOUL_FIRE) {
                        event.setCancelled(true);
                        return;
                    }
                }
            } else if (hasSkillCached(uuid, Skill.RESISTENCIA_FUEGO)) {
                reduction = resistenciaFuegoReduccion;
                trackSkillUsage(uuid, Skill.RESISTENCIA_FUEGO);
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
        
        if (hasSkillCached(uuid, Skill.METABOLISMO_LENTO)) {
            reduction = metabolismoLentoReduccion;
            usedSkill = Skill.METABOLISMO_LENTO;
        } else if (hasSkillCached(uuid, Skill.ESTOMAGO_HIERRO)) {
            reduction = estomagoHierroReduccion;
            usedSkill = Skill.ESTOMAGO_HIERRO;
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
                
                // Buscar en las 26 direcciones (cubo 3x3x3 alrededor)
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            
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
     */
    private boolean isRealTree(Block logBlock) {
        int leavesCount = 0;
        int radius = lenadorRadioBuscarHojas;
        
        // Buscar hojas en un cubo alrededor del tronco
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= radius * 2; dy++) { // Más arriba que abajo
                for (int dz = -radius; dz <= radius; dz++) {
                    Block check = logBlock.getRelative(dx, dy, dz);
                    if (isLeaves(check.getType())) {
                        leavesCount++;
                        if (leavesCount >= lenadorMinHojasRequeridas) {
                            return true; // Suficientes hojas encontradas
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
    
    // ==================== WAYPOINT (EXPLORADOR) - CON PERSISTENCIA ====================
    
    private final Map<UUID, Location> playerWaypoints = new ConcurrentHashMap<>();
    
    /**
     * Establece un waypoint en la ubicación actual del jugador.
     */
    public void setWaypoint(Player player) {
        UUID uuid = player.getUniqueId();
        Location loc = player.getLocation();
        playerWaypoints.put(uuid, loc);
        
        // Guardar inmediatamente si persistencia está activa
        if (waypointPersistencia) {
            saveWaypoints();
        }
        
        player.sendMessage("§a✓ §eWaypoint establecido en: §f" + 
            loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.2f);
        
        // Efectos visuales mejorados
        spawnWaypointSetParticles(loc);
        trackSkillUsage(uuid, Skill.WAYPOINT);
    }
    
    private void spawnWaypointSetParticles(Location loc) {
        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.05);
        loc.getWorld().spawnParticle(Particle.ENCHANT, loc.clone().add(0, 0.5, 0), 20, 0.3, 0.3, 0.3, 0.5);
    }
    
    /**
     * Teleporta al jugador a su waypoint guardado.
     */
    public void teleportToWaypoint(Player player) {
        UUID uuid = player.getUniqueId();
        Location waypoint = playerWaypoints.get(uuid);
        
        if (waypoint == null) {
            player.sendMessage("§c✖ §7No tienes un waypoint establecido. Usa §e/waypoint set §7primero.");
            return;
        }
        
        // Verificar cooldown
        if (isWaypointOnCooldown(uuid)) {
            long remaining = getWaypointCooldownRemaining(uuid) / 1000;
            player.sendMessage("§c✖ §7Waypoint en cooldown: §e" + remaining + "s§7 restantes.");
            return;
        }
        
        // Verificar que el mundo siga cargado
        if (waypoint.getWorld() == null) {
            player.sendMessage("§c✖ §7El mundo del waypoint ya no está disponible.");
            playerWaypoints.remove(uuid);
            return;
        }
        
        player.teleport(waypoint);
        setWaypointCooldown(uuid);
        
        player.sendMessage("§a✓ §eTeletransportado al waypoint.");
        player.playSound(waypoint, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.0f);
        
        // Efectos visuales mejorados
        spawnWaypointTeleportParticles(waypoint);
        trackSkillUsage(uuid, Skill.WAYPOINT);
    }
    
    private void spawnWaypointTeleportParticles(Location loc) {
        loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
        loc.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(0, 0.5, 0), 30, 0.3, 0.3, 0.3, 0.5);
    }
    
    public Location getWaypoint(UUID uuid) {
        return playerWaypoints.get(uuid);
    }
    
    public boolean hasWaypoint(UUID uuid) {
        return playerWaypoints.containsKey(uuid);
    }
    
    public void removeWaypoint(UUID uuid) {
        playerWaypoints.remove(uuid);
        if (waypointPersistencia) {
            saveWaypoints();
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
    
    // ==================== PERSISTENCIA WAYPOINTS ====================
    
    private void loadWaypoints() {
        if (!waypointPersistencia) return;
        
        File waypointsFile = new File(plugin.getDataFolder(), "waypoints.yml");
        if (!waypointsFile.exists()) return;
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(waypointsFile);
        
        if (!config.isConfigurationSection("waypoints")) return;
        
        for (String uuidStr : config.getConfigurationSection("waypoints").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String path = "waypoints." + uuidStr;
                
                String worldName = config.getString(path + ".world");
                if (worldName == null) continue;
                
                org.bukkit.World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("[Skills] Mundo no encontrado para waypoint: " + worldName);
                    continue;
                }
                
                double x = config.getDouble(path + ".x");
                double y = config.getDouble(path + ".y");
                double z = config.getDouble(path + ".z");
                float yaw = (float) config.getDouble(path + ".yaw");
                float pitch = (float) config.getDouble(path + ".pitch");
                
                playerWaypoints.put(uuid, new Location(world, x, y, z, yaw, pitch));
                
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[Skills] UUID inválido en waypoints.yml: " + uuidStr);
            }
        }
        
        plugin.getLogger().info("[Skills] Cargados " + playerWaypoints.size() + " waypoints");
    }
    
    private void saveWaypoints() {
        if (!waypointPersistencia) return;
        
        FileConfiguration config = new YamlConfiguration();
        
        for (Map.Entry<UUID, Location> entry : playerWaypoints.entrySet()) {
            String path = "waypoints." + entry.getKey().toString();
            Location loc = entry.getValue();
            
            if (loc.getWorld() != null) {
                config.set(path + ".world", loc.getWorld().getName());
                config.set(path + ".x", loc.getX());
                config.set(path + ".y", loc.getY());
                config.set(path + ".z", loc.getZ());
                config.set(path + ".yaw", loc.getYaw());
                config.set(path + ".pitch", loc.getPitch());
            }
        }
        
        try {
            config.save(new File(plugin.getDataFolder(), "waypoints.yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("[Skills] Error guardando waypoints: " + e.getMessage());
        }
    }
}