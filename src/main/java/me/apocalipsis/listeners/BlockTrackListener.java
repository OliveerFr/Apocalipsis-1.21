package me.apocalipsis.listeners;

import java.util.List;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.utils.BlockOwnershipTracker;

/**
 * Listener que rastrea quién coloca y rompe bloques para prevenir griefing.
 * 
 * Características:
 * - Rastrea colocación/destrucción de bloques
 * - Protege contra pistons moviendo bloques ajenos
 * - Protege contra TNT/explosiones destruyendo bloques ajenos
 * - Auto-protege cofres dobles completos
 * 
 * Prioridad MONITOR para ejecutarse después de otros plugins de protección.
 */
public class BlockTrackListener implements Listener {
    
    private final Apocalipsis plugin;
    private final BlockOwnershipTracker tracker;
    
    public BlockTrackListener(Apocalipsis plugin) {
        this.plugin = plugin;
        this.tracker = plugin.getBlockTracker();
    }
    
    // ==================== Colocación y Rotura ====================
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        
        // Trackear el bloque principal
        tracker.trackBlockPlacement(block, player);
        
        // Si es un cofre, verificar si forma parte de un cofre doble
        if (isChest(block.getType())) {
            protectDoubleChest(block, player);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        tracker.trackBlockBreak(event.getBlock());
    }
    
    // ==================== Protección contra Pistons ====================
    
    /**
     * Protección contra pistons moviendo bloques ajenos.
     * 
     * Reglas:
     * - Si el pistón tiene dueño, puede mover bloques del mismo dueño
     * - Si el pistón NO tiene dueño (natural), solo puede mover bloques sin dueño
     * - Bloques de jugadores activos diferentes al dueño del pistón → BLOQUEADO
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        Block pistonBlock = event.getBlock();
        UUID pistonOwner = tracker.getBlockOwner(pistonBlock);
        
        // Verificar cada bloque que el pistón va a mover
        for (Block block : event.getBlocks()) {
            UUID blockOwner = tracker.getBlockOwner(block);
            
            // Si el bloque no tiene dueño, se puede mover
            if (blockOwner == null) continue;
            
            // Si el dueño del bloque es el mismo que el del pistón, se puede mover
            if (blockOwner.equals(pistonOwner)) continue;
            
            // Si el dueño del bloque está inactivo, se puede mover
            if (!tracker.isPlayerActivePublic(blockOwner)) continue;
            
            // Bloque de jugador activo diferente → BLOQUEAR
            event.setCancelled(true);
            
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[BlockTracker] Pistón bloqueado: intentó mover bloque de " + 
                    getPlayerName(blockOwner) + " (dueño pistón: " + 
                    (pistonOwner != null ? getPlayerName(pistonOwner) : "ninguno") + ")");
            }
            return;
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        Block pistonBlock = event.getBlock();
        UUID pistonOwner = tracker.getBlockOwner(pistonBlock);
        
        // Verificar bloques que serán retraídos
        for (Block block : event.getBlocks()) {
            UUID blockOwner = tracker.getBlockOwner(block);
            
            // Si el bloque no tiene dueño, se puede mover
            if (blockOwner == null) continue;
            
            // Si el dueño del bloque es el mismo que el del pistón, se puede mover
            if (blockOwner.equals(pistonOwner)) continue;
            
            // Si el dueño del bloque está inactivo, se puede mover
            if (!tracker.isPlayerActivePublic(blockOwner)) continue;
            
            // Bloque de jugador activo diferente → BLOQUEAR
            event.setCancelled(true);
            
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[BlockTracker] Pistón bloqueado: intentó retraer bloque de " + 
                    getPlayerName(blockOwner) + " (dueño pistón: " + 
                    (pistonOwner != null ? getPlayerName(pistonOwner) : "ninguno") + ")");
            }
            return;
        }
    }
    
    // ==================== Protección contra Explosiones ====================
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        
        // Obtener el "culpable" de la explosión si es TNT
        UUID exploder = null;
        if (entity instanceof TNTPrimed tnt) {
            Entity source = tnt.getSource();
            if (source instanceof Player player) {
                exploder = player.getUniqueId();
            }
        }
        
        // Filtrar bloques protegidos de la lista de explosión
        filterProtectedBlocks(event.blockList(), exploder);
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        // Explosiones de bloques (camas en nether, respawn anchors, etc.)
        filterProtectedBlocks(event.blockList(), null);
    }
    
    /**
     * Filtra bloques protegidos de una lista de explosión
     */
    private void filterProtectedBlocks(List<Block> blocks, UUID exploder) {
        blocks.removeIf(block -> {
            UUID owner = tracker.getBlockOwner(block);
            if (owner == null) {
                // Verificar bloques conectados
                owner = tracker.findConnectedOwnerPublic(block, exploder);
            }
            
            if (owner != null && !owner.equals(exploder)) {
                // Si el dueño está activo, proteger el bloque
                if (tracker.isPlayerActivePublic(owner)) {
                    if (plugin.getConfigManager().isDebugCiclo()) {
                        plugin.getLogger().info("[BlockTracker] Explosión: protegido bloque de " + 
                            getPlayerName(owner));
                    }
                    return true; // Remover de la lista (no explotar)
                }
            }
            return false;
        });
    }
    
    // ==================== Protección de Cofres Dobles ====================
    
    /**
     * Protege ambas partes de un cofre doble
     */
    private void protectDoubleChest(Block chestBlock, Player player) {
        if (!(chestBlock.getBlockData() instanceof Chest chestData)) {
            return;
        }
        
        Chest.Type type = chestData.getType();
        if (type == Chest.Type.SINGLE) {
            return; // No es cofre doble
        }
        
        // Encontrar la otra mitad
        BlockFace facing = ((Directional) chestData).getFacing();
        BlockFace otherHalf = getOtherChestHalf(type, facing);
        
        if (otherHalf != null) {
            Block otherChest = chestBlock.getRelative(otherHalf);
            if (isChest(otherChest.getType())) {
                // Proteger la otra mitad también
                tracker.trackBlockPlacementDirect(otherChest, player.getUniqueId());
            }
        }
    }
    
    /**
     * Obtiene la dirección de la otra mitad del cofre
     */
    private BlockFace getOtherChestHalf(Chest.Type type, BlockFace facing) {
        return switch (type) {
            case LEFT -> switch (facing) {
                case NORTH -> BlockFace.EAST;
                case SOUTH -> BlockFace.WEST;
                case EAST -> BlockFace.SOUTH;
                case WEST -> BlockFace.NORTH;
                default -> null;
            };
            case RIGHT -> switch (facing) {
                case NORTH -> BlockFace.WEST;
                case SOUTH -> BlockFace.EAST;
                case EAST -> BlockFace.NORTH;
                case WEST -> BlockFace.SOUTH;
                default -> null;
            };
            default -> null;
        };
    }
    
    private boolean isChest(Material material) {
        return material == Material.CHEST || material == Material.TRAPPED_CHEST;
    }
    
    private String getPlayerName(UUID uuid) {
        return plugin.getServer().getOfflinePlayer(uuid).getName();
    }
}
