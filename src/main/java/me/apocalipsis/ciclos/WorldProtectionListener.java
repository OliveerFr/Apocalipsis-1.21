package me.apocalipsis.ciclos;

import me.apocalipsis.Apocalipsis;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener que bloquea acciones problemáticas que podrían permitir
 * transferencia de items entre mundos.
 */
public class WorldProtectionListener implements Listener {
    
    private final Apocalipsis plugin;
    private final CicloManager cicloManager;
    private final boolean blockEnderChests;
    private final boolean blockShulkerBoxes;
    private final boolean blockBundles;
    
    public WorldProtectionListener(Apocalipsis plugin, CicloManager cicloManager) {
        this.plugin = plugin;
        this.cicloManager = cicloManager;
        
        // Cargar configuración de protecciones
        var config = cicloManager.getCiclosConfig();
        this.blockEnderChests = config.getBoolean("protecciones.bloquear_enderchest_vanilla", true);
        this.blockShulkerBoxes = config.getBoolean("protecciones.bloquear_shulker_boxes", true);
        this.blockBundles = config.getBoolean("protecciones.bloquear_bundles", true);
    }
    
    /**
     * Bloquea la apertura de Ender Chests vanilla en mundos de ciclo
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnderChestOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        
        // Si tiene bypass, permitir
        if (player.hasPermission("apocalipsis.ciclo.bypass")) {
            return;
        }
        
        // Solo bloquear si está habilitado
        if (!blockEnderChests) {
            return;
        }
        
        // Verificar si es Ender Chest
        if (event.getInventory().getType() == InventoryType.ENDER_CHEST) {
            String worldName = player.getWorld().getName();
            
            // Bloquear en mundos de ciclo
            if (cicloManager.isCycleWorld(worldName)) {
                event.setCancelled(true);
                
                String msg = cicloManager.getMessage("enderchest_bloqueado");
                player.sendMessage(msg);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
                
                plugin.getLogger().info("[WorldProtection] Bloqueado Ender Chest para " + 
                                        player.getName() + " en mundo: " + worldName);
            }
        }
    }
    
    /**
     * Bloquea la interacción con bloques de Ender Chest
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnderChestInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENDER_CHEST) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Si tiene bypass, permitir
        if (player.hasPermission("apocalipsis.ciclo.bypass")) {
            return;
        }
        
        // Solo bloquear si está habilitado
        if (!blockEnderChests) {
            return;
        }
        
        String worldName = player.getWorld().getName();
        
        // Bloquear en mundos de ciclo
        if (cicloManager.isCycleWorld(worldName)) {
            event.setCancelled(true);
            
            String msg = cicloManager.getMessage("enderchest_bloqueado");
            player.sendMessage(msg);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
        }
    }
    
    /**
     * Bloquea la colocación de Shulker Boxes
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShulkerPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        
        // Si tiene bypass, permitir
        if (player.hasPermission("apocalipsis.ciclo.bypass")) {
            return;
        }
        
        // Solo bloquear si está habilitado
        if (!blockShulkerBoxes) {
            return;
        }
        
        // Verificar si es Shulker Box
        if (ItemSanitizer.isShulkerBox(item.getType())) {
            String worldName = player.getWorld().getName();
            
            // Bloquear en cualquier mundo (previene almacenamiento para transferir)
            event.setCancelled(true);
            
            String msg = cicloManager.getMessage("shulker_bloqueado");
            player.sendMessage(msg);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            
            plugin.getLogger().info("[WorldProtection] Bloqueada colocación de Shulker Box para " + 
                                    player.getName() + " en mundo: " + worldName);
        }
    }
    
    /**
     * Bloquea el uso de Bundles (si está habilitado)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBundleUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || item.getType() != Material.BUNDLE) {
            return;
        }
        
        // Si tiene bypass, permitir
        if (player.hasPermission("apocalipsis.ciclo.bypass")) {
            return;
        }
        
        // Solo bloquear si está habilitado
        if (!blockBundles) {
            return;
        }
        
        String worldName = player.getWorld().getName();
        
        // Bloquear en mundos de ciclo
        if (cicloManager.isCycleWorld(worldName)) {
            event.setCancelled(true);
            
            String msg = cicloManager.getMessage("item_bloqueado");
            player.sendMessage(msg);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            
            plugin.getLogger().info("[WorldProtection] Bloqueado uso de Bundle para " + 
                                    player.getName() + " en mundo: " + worldName);
        }
    }
    
    /**
     * Bloquea la interacción con Item Frames en mundos de ciclo
     * (podrían usarse para transferir items entre mundos)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemFrameInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        
        if (!(entity instanceof ItemFrame)) {
            return;
        }
        
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        
        // Solo verificar en mundos de ciclo
        if (!cicloManager.isCycleWorld(worldName)) {
            return;
        }
        
        // Bypass para admins
        if (player.hasPermission("apocalipsis.ciclo.bypass")) {
            return;
        }
        
        ItemFrame frame = (ItemFrame) entity;
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        
        // Bloquear si el jugador intenta poner un item en el frame
        if (heldItem != null && heldItem.getType() != Material.AIR && frame.getItem().getType() == Material.AIR) {
            event.setCancelled(true);
            player.sendMessage("§c✖ No puedes usar Item Frames en mundos de ciclo.");
            player.sendMessage("§7Razón: Prevención de transferencia de items entre mundos.");
            
            plugin.getLogger().warning("[WorldProtection] Bloqueado Item Frame para " + 
                player.getName() + " en mundo: " + worldName);
        }
    }
    
    /**
     * Bloquea la manipulación de Armor Stands en mundos de ciclo
     * (podrían usarse para transferir armor/items entre mundos)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        
        // Solo verificar en mundos de ciclo
        if (!cicloManager.isCycleWorld(worldName)) {
            return;
        }
        
        // Bypass para admins
        if (player.hasPermission("apocalipsis.ciclo.bypass")) {
            return;
        }
        
        // Verificar si el jugador está poniendo o quitando items
        ItemStack playerItem = event.getPlayerItem();
        ItemStack armorItem = event.getArmorStandItem();
        
        // Bloquear si hay transferencia de items
        if ((playerItem != null && playerItem.getType() != Material.AIR) || 
            (armorItem != null && armorItem.getType() != Material.AIR)) {
            
            event.setCancelled(true);
            player.sendMessage("§c✖ No puedes manipular Armor Stands en mundos de ciclo.");
            player.sendMessage("§7Razón: Prevención de transferencia de armor/items entre mundos.");
            
            plugin.getLogger().warning("[WorldProtection] Bloqueado Armor Stand para " + 
                player.getName() + " en mundo: " + worldName);
        }
    }
}
