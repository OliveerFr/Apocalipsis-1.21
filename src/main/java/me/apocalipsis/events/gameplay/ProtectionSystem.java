package me.apocalipsis.events.gameplay;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitTask;

import me.apocalipsis.Apocalipsis;

/**
 * Sistema de protecciones para eventos
 * 
 * Previene:
 * - Griefing de bloques (colocar/romper)
 * - Explosiones de creepers/TNT
 * - Spawn de mobs no deseados
 * - PvP entre participantes
 * - Interacción con contenedores
 * 
 * Permite:
 * - Bloques específicos del evento (anclas, estructuras)
 * - Explosiones controladas del evento
 * - Spawns de mobs del evento
 */
public class ProtectionSystem implements Listener {
    
    private final Apocalipsis plugin;
    
    // Estado de protección
    private boolean protectionEnabled = false;
    private String eventId = null;
    
    // Zonas protegidas
    private final Set<ProtectedZone> protectedZones = ConcurrentHashMap.newKeySet();
    
    // Bloques permitidos para interacción
    private final Set<Location> allowedBlocks = ConcurrentHashMap.newKeySet();
    
    // Entidades permitidas (del evento)
    private final Set<UUID> allowedEntities = ConcurrentHashMap.newKeySet();
    
    // Tipos de bloques permitidos para romper/colocar
    private final Set<Material> allowedMaterials = new HashSet<>();
    
    // Modos de protección
    private boolean preventBlockBreak = true;
    private boolean preventBlockPlace = true;
    private boolean preventExplosions = true;
    private boolean preventMobSpawn = true;
    private boolean preventPvP = true;
    private boolean preventContainerAccess = true;
    
    // Sistema de rollback
    private final Map<Location, BlockState> originalBlocks = new ConcurrentHashMap<>();
    private boolean trackChanges = false;
    
    // Limpieza automática
    private BukkitTask cleanupTask;
    
    /**
     * Constructor
     */
    public ProtectionSystem(Apocalipsis plugin) {
        this.plugin = plugin;
        
        // Registrar listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Materiales por defecto permitidos (items del evento)
        allowedMaterials.add(Material.ECHO_SHARD); // Fragmento de Sombra
        allowedMaterials.add(Material.NETHER_STAR); // Eco Resonante
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ACTIVACIÓN/DESACTIVACIÓN
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Activa el sistema de protección
     */
    public void enable(String eventId) {
        this.eventId = eventId;
        this.protectionEnabled = true;
        
        plugin.getLogger().info("[ProtectionSystem] Protección activada para evento: " + eventId);
    }
    
    /**
     * Desactiva el sistema de protección
     */
    public void disable() {
        this.protectionEnabled = false;
        
        // Rollback si está configurado
        if (trackChanges && !originalBlocks.isEmpty()) {
            performRollback();
        }
        
        // Limpiar datos
        protectedZones.clear();
        allowedBlocks.clear();
        allowedEntities.clear();
        originalBlocks.clear();
        
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        
        plugin.getLogger().info("[ProtectionSystem] Protección desactivada");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Configura los modos de protección
     */
    public void setProtectionMode(boolean blockBreak, boolean blockPlace, 
                                   boolean explosions, boolean mobSpawn, 
                                   boolean pvp, boolean containerAccess) {
        this.preventBlockBreak = blockBreak;
        this.preventBlockPlace = blockPlace;
        this.preventExplosions = explosions;
        this.preventMobSpawn = mobSpawn;
        this.preventPvP = pvp;
        this.preventContainerAccess = containerAccess;
    }
    
    /**
     * Activa el tracking de cambios para rollback
     */
    public void enableRollback() {
        this.trackChanges = true;
    }
    
    /**
     * Añade material permitido
     */
    public void addAllowedMaterial(Material material) {
        allowedMaterials.add(material);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ZONAS PROTEGIDAS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Añade una zona protegida
     */
    public void addProtectedZone(Location center, int radius, String name) {
        protectedZones.add(new ProtectedZone(center, radius, name));
        plugin.getLogger().info("[ProtectionSystem] Zona protegida añadida: " + name + 
                               " (radio: " + radius + ")");
    }
    
    /**
     * Verifica si una ubicación está en zona protegida
     */
    private boolean isInProtectedZone(Location loc) {
        for (ProtectedZone zone : protectedZones) {
            if (zone.contains(loc)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Clase interna para zona protegida
     */
    private static class ProtectedZone {
        private final Location center;
        private final int radius;
        private final String name;
        
        public ProtectedZone(Location center, int radius, String name) {
            this.center = center;
            this.radius = radius;
            this.name = name;
        }
        
        public boolean contains(Location loc) {
            if (!loc.getWorld().equals(center.getWorld())) return false;
            return loc.distanceSquared(center) <= radius * radius;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // BLOQUES PERMITIDOS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Añade un bloque permitido para interacción
     */
    public void addAllowedBlock(Location loc) {
        allowedBlocks.add(loc.getBlock().getLocation());
    }
    
    /**
     * Permite un bloque temporalmente (para estructuras del evento)
     */
    public void allowBlockTemporarily(Block block, int durationTicks) {
        Location loc = block.getLocation();
        allowedBlocks.add(loc);
        
        // Remover después del tiempo especificado
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            allowedBlocks.remove(loc);
        }, durationTicks);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ENTIDADES PERMITIDAS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Registra una entidad del evento
     */
    public void registerEventEntity(Entity entity) {
        allowedEntities.add(entity.getUniqueId());
    }
    
    /**
     * Verifica si una entidad es del evento
     */
    public boolean isEventEntity(Entity entity) {
        return allowedEntities.contains(entity.getUniqueId());
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ROLLBACK
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Guarda el estado de un bloque antes de modificarlo
     */
    private void saveBlockState(Block block) {
        if (trackChanges && !originalBlocks.containsKey(block.getLocation())) {
            originalBlocks.put(block.getLocation(), block.getState());
        }
    }
    
    /**
     * Ejecuta rollback de todos los cambios
     */
    private void performRollback() {
        int count = 0;
        
        for (Map.Entry<Location, BlockState> entry : originalBlocks.entrySet()) {
            try {
                entry.getValue().update(true, false);
                count++;
            } catch (Exception e) {
                plugin.getLogger().warning("[ProtectionSystem] Error al restaurar bloque: " + 
                                          e.getMessage());
            }
        }
        
        plugin.getLogger().info("[ProtectionSystem] Rollback completado: " + count + 
                               " bloques restaurados");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // EVENT HANDLERS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Previene romper bloques
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!protectionEnabled || !preventBlockBreak) return;
        
        Block block = event.getBlock();
        Location loc = block.getLocation();
        
        // Permitir si es bloque permitido
        if (allowedBlocks.contains(loc)) return;
        
        // Permitir si es material permitido
        if (allowedMaterials.contains(block.getType())) return;
        
        // Cancelar y notificar
        event.setCancelled(true);
        event.getPlayer().sendMessage("§c⚠ No puedes romper bloques durante el evento");
    }
    
    /**
     * Previene colocar bloques
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!protectionEnabled || !preventBlockPlace) return;
        
        Block block = event.getBlock();
        
        // Permitir si es material permitido
        if (allowedMaterials.contains(block.getType())) {
            // Guardar estado para rollback
            saveBlockState(block);
            return;
        }
        
        // Cancelar y notificar
        event.setCancelled(true);
        event.getPlayer().sendMessage("§c⚠ No puedes colocar bloques durante el evento");
    }
    
    /**
     * Previene explosiones
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!protectionEnabled || !preventExplosions) return;
        
        Entity entity = event.getEntity();
        
        // Permitir si es entidad del evento
        if (isEventEntity(entity)) {
            // Guardar estados de bloques afectados
            for (Block block : event.blockList()) {
                saveBlockState(block);
            }
            return;
        }
        
        // Cancelar explosión
        event.setCancelled(true);
    }
    
    /**
     * Previene explosiones de bloques
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!protectionEnabled || !preventExplosions) return;
        
        // Siempre cancelar explosiones de bloques durante evento
        event.setCancelled(true);
    }
    
    /**
     * Previene spawn de mobs no deseados
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!protectionEnabled || !preventMobSpawn) return;
        
        Entity entity = event.getEntity();
        
        // Permitir spawns del evento (plugin/custom)
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM ||
            event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            registerEventEntity(entity);
            return;
        }
        
        // Cancelar spawns naturales en zona protegida
        if (isInProtectedZone(event.getLocation())) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Previene PvP entre participantes
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!protectionEnabled || !preventPvP) return;
        
        // Solo prevenir PvP
        if (!(event.getEntity() instanceof Player)) return;
        
        Player damaged = (Player) event.getEntity();
        Player damager = null;
        
        // Identificar atacante
        if (event.getDamager() instanceof Player) {
            damager = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                damager = (Player) proj.getShooter();
            }
        }
        
        // Cancelar si es PvP
        if (damager != null) {
            event.setCancelled(true);
            damager.sendMessage("§c⚠ No puedes atacar a otros jugadores durante el evento");
        }
    }
    
    /**
     * Previene acceso a contenedores
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!protectionEnabled || !preventContainerAccess) return;
        
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        
        Block block = event.getClickedBlock();
        if (block == null) return;
        
        // Detectar contenedores
        Material type = block.getType();
        if (type == Material.CHEST || type == Material.TRAPPED_CHEST ||
            type == Material.BARREL || type == Material.SHULKER_BOX ||
            type == Material.HOPPER || type == Material.DISPENSER ||
            type == Material.DROPPER || type == Material.FURNACE) {
            
            // Permitir si es bloque permitido
            if (allowedBlocks.contains(block.getLocation())) return;
            
            // Cancelar
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c⚠ No puedes acceder a contenedores durante el evento");
        }
    }
    
    /**
     * Limpia entidades muertas del registro
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!protectionEnabled) return;
        
        Entity entity = event.getEntity();
        allowedEntities.remove(entity.getUniqueId());
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Programa limpieza automática de entidades del evento
     */
    public void scheduleEntityCleanup(int delayTicks) {
        if (cleanupTask != null) cleanupTask.cancel();
        
        cleanupTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int count = 0;
            
            for (UUID uuid : allowedEntities) {
                Entity entity = Bukkit.getEntity(uuid);
                if (entity != null && entity.isValid()) {
                    entity.remove();
                    count++;
                }
            }
            
            allowedEntities.clear();
            plugin.getLogger().info("[ProtectionSystem] Limpieza de entidades: " + count + 
                                   " entidades removidas");
        }, delayTicks);
    }
    
    /**
     * Limpia todo el sistema
     */
    public void cleanup() {
        disable();
    }
    
    /**
     * Obtiene estadísticas
     */
    public Map<String, Integer> getStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("protected_zones", protectedZones.size());
        stats.put("allowed_blocks", allowedBlocks.size());
        stats.put("event_entities", allowedEntities.size());
        stats.put("original_blocks", originalBlocks.size());
        return stats;
    }
}
