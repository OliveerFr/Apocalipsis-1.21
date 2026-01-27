package me.apocalipsis.ciclos;

import me.apocalipsis.Apocalipsis;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;

/**
 * Protege el End compartido para prevenir duplicación de items entre ciclos.
 * 
 * PROBLEMA: El End es compartido entre todos los ciclos, por lo que:
 * 1. Jugador en ciclo_1 deja items en cofre en el End
 * 2. Jugador cambia a ciclo_2
 * 3. Jugador recoge los items del cofre en el End (DUPLICACIÓN)
 * 
 * SOLUCIÓN: Bloquear colocación/rotura de contenedores y drops en el End
 */
public class EndProtectionListener implements Listener {
    
    private final Apocalipsis plugin;
    private final CicloManager cicloManager;
    private final boolean protectEnd;
    private final boolean blockContainers;
    private final boolean blockItemDrops;
    
    public EndProtectionListener(Apocalipsis plugin, CicloManager cicloManager) {
        this.plugin = plugin;
        this.cicloManager = cicloManager;
        
        // Cargar configuración
        var config = cicloManager.getCiclosConfig();
        this.protectEnd = config.getBoolean("protecciones_end.activar_proteccion", true);
        this.blockContainers = config.getBoolean("protecciones_end.bloquear_contenedores", true);
        this.blockItemDrops = config.getBoolean("protecciones_end.bloquear_drops", true);
    }
    
    /**
     * Verifica si un mundo es el End
     */
    private boolean isEndWorld(World world) {
        return world.getEnvironment() == World.Environment.THE_END;
    }
    
    /**
     * Verifica si un material es un contenedor de almacenamiento
     */
    private boolean isStorageContainer(Material material) {
        return material == Material.CHEST ||
               material == Material.TRAPPED_CHEST ||
               material == Material.BARREL ||
               material == Material.HOPPER ||
               material == Material.DROPPER ||
               material == Material.DISPENSER ||
               material == Material.FURNACE ||
               material == Material.BLAST_FURNACE ||
               material == Material.SMOKER ||
               material.name().contains("SHULKER_BOX");
    }
    
    /**
     * Bloquea colocación de contenedores en el End
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onContainerPlace(BlockPlaceEvent event) {
        if (!protectEnd || !blockContainers) {
            return;
        }
        
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Solo en el End
        if (!isEndWorld(player.getWorld())) {
            return;
        }
        
        // Bypass para admins
        if (player.hasPermission("apocalipsis.ciclo.bypass")) {
            return;
        }
        
        // Verificar si es un contenedor
        if (isStorageContainer(block.getType())) {
            event.setCancelled(true);
            
            String msg = cicloManager.getMessage("end_contenedor_bloqueado");
            player.sendMessage(msg);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            
            plugin.getLogger().warning("[EndProtection] Bloqueada colocación de " + 
                block.getType().name() + " en el End por " + player.getName());
        }
    }
    
    /**
     * Bloquea rotura de contenedores en el End (para evitar obtener items ajenos)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onContainerBreak(BlockBreakEvent event) {
        if (!protectEnd || !blockContainers) {
            return;
        }
        
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Solo en el End
        if (!isEndWorld(player.getWorld())) {
            return;
        }
        
        // Bypass para admins
        if (player.hasPermission("apocalipsis.ciclo.bypass")) {
            return;
        }
        
        // Verificar si es un contenedor
        if (isStorageContainer(block.getType())) {
            event.setCancelled(true);
            
            String msg = cicloManager.getMessage("end_contenedor_bloqueado");
            player.sendMessage(msg);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            
            plugin.getLogger().warning("[EndProtection] Bloqueada rotura de " + 
                block.getType().name() + " en el End por " + player.getName());
        }
    }
    
    /**
     * Bloquea apertura de contenedores en el End
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onContainerOpen(PlayerInteractEvent event) {
        if (!protectEnd || !blockContainers) {
            return;
        }
        
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Solo en el End
        if (!isEndWorld(player.getWorld())) {
            return;
        }
        
        // Bypass para admins
        if (player.hasPermission("apocalipsis.ciclo.bypass")) {
            return;
        }
        
        // Verificar si es un contenedor
        if (isStorageContainer(block.getType())) {
            event.setCancelled(true);
            
            String msg = cicloManager.getMessage("end_contenedor_bloqueado");
            player.sendMessage(msg);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            
            plugin.getLogger().warning("[EndProtection] Bloqueada apertura de " + 
                block.getType().name() + " en el End por " + player.getName());
        }
    }
    
    /**
     * Bloquea que jugadores tiren items en el End
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!protectEnd || !blockItemDrops) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Solo en el End
        if (!isEndWorld(player.getWorld())) {
            return;
        }
        
        // Bypass para admins
        if (player.hasPermission("apocalipsis.ciclo.bypass")) {
            return;
        }
        
        event.setCancelled(true);
        
        String msg = cicloManager.getMessage("end_drop_bloqueado");
        player.sendMessage(msg);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
        
        plugin.getLogger().info("[EndProtection] Bloqueado drop de item en el End por " + 
            player.getName());
    }
    
    /**
     * Bloquea spawn natural de items en el End (de bloques rotos, mobs, etc.)
     * Esto previene que items "flotantes" queden en el End
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!protectEnd || !blockItemDrops) {
            return;
        }
        
        Item item = event.getEntity();
        
        // Solo en el End
        if (!isEndWorld(item.getWorld())) {
            return;
        }
        
        // Permitir drops del dragón (para que se pueda completar)
        if (item.getItemStack().getType() == Material.DRAGON_EGG ||
            item.getItemStack().getType() == Material.EXPERIENCE_BOTTLE) {
            return;
        }
        
        // Cancelar otros spawns de items
        event.setCancelled(true);
        
        plugin.getLogger().info("[EndProtection] Bloqueado spawn de item " + 
            item.getItemStack().getType().name() + " en el End");
    }
    
    /**
     * Bloquea movimiento automático de items por hoppers/droppers en el End
     * Previene sistemas automáticos de transferencia de items
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (!protectEnd || !blockContainers) {
            return;
        }
        
        // Verificar si origen o destino está en el End
        if (event.getSource() != null && event.getSource().getLocation() != null) {
            if (isEndWorld(event.getSource().getLocation().getWorld())) {
                event.setCancelled(true);
                plugin.getLogger().info("[EndProtection] Bloqueado movimiento automático de items desde el End");
                return;
            }
        }
        
        if (event.getDestination() != null && event.getDestination().getLocation() != null) {
            if (isEndWorld(event.getDestination().getLocation().getWorld())) {
                event.setCancelled(true);
                plugin.getLogger().info("[EndProtection] Bloqueado movimiento automático de items hacia el End");
                return;
            }
        }
    }
    
    /**
     * Bloquea drops de items por explosiones en el End (TNT, Creepers, etc.)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!protectEnd || !blockItemDrops) {
            return;
        }
        
        // Solo en el End
        if (!isEndWorld(event.getLocation().getWorld())) {
            return;
        }
        
        // Cancelar drops de bloques por explosión (previene items flotantes)
        event.setYield(0.0f);
        
        plugin.getLogger().info("[EndProtection] Explosión en el End - drops de bloques cancelados");
    }
    
    /**
     * Bloquea drops de items por explosiones de bloques en el End (Respawn Anchor, End Crystal, etc.)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!protectEnd || !blockItemDrops) {
            return;
        }
        
        // Solo en el End
        if (!isEndWorld(event.getBlock().getWorld())) {
            return;
        }
        
        // Cancelar drops de bloques por explosión
        event.setYield(0.0f);
        
        plugin.getLogger().info("[EndProtection] Explosión de bloque en el End - drops cancelados");
    }
}
