package me.apocalipsis.ciclos;

import me.apocalipsis.Apocalipsis;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.AbstractHorseInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Protege contra transferencia de items usando entidades
 * (caballos con cofre, llamas, mascotas, etc.)
 */
public class EntityProtectionListener implements Listener {
    
    private final Apocalipsis plugin;
    private final CicloManager cicloManager;
    
    public EntityProtectionListener(Apocalipsis plugin, CicloManager cicloManager) {
        this.plugin = plugin;
        this.cicloManager = cicloManager;
    }
    
    /**
     * Previene que jugadores monten animales con inventario antes de cambiar de mundo
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        
        // Solo verificar en mundos de ciclo
        if (!cicloManager.isCycleWorld(player.getWorld().getName())) {
            return;
        }
        
        // Bypass para admins
        if (player.hasPermission("apocalipsis.ciclo.bypass")) {
            return;
        }
        
        // Verificar si es un animal con inventario
        if (entity instanceof ChestedHorse) {
            ChestedHorse horse = (ChestedHorse) entity;
            
            // Si tiene cofre, bloquear montaje
            if (horse.isCarryingChest()) {
                event.setCancelled(true);
                player.sendMessage("§c✖ No puedes montar animales con cofre en mundos de ciclo.");
                player.sendMessage("§7Razón: Prevención de transferencia de items entre mundos.");
                
                plugin.getLogger().warning("[EntityProtection] Bloqueado intento de montar " + 
                    entity.getType().name() + " con cofre por " + player.getName());
            }
        }
        
        // Verificar llamas con cofre
        if (entity instanceof Llama) {
            Llama llama = (Llama) entity;
            
            if (llama.getInventory() != null && hasItemsInInventory(llama.getInventory())) {
                event.setCancelled(true);
                player.sendMessage("§c✖ No puedes interactuar con llamas que llevan items en mundos de ciclo.");
                
                plugin.getLogger().warning("[EntityProtection] Bloqueado intento de acceder a llama con items por " + 
                    player.getName());
            }
        }
    }
    
    /**
     * Bloquea el teletransporte de entidades con items entre mundos
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleportWithEntity(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        // Verificar si está cambiando de mundo
        if (event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            return;
        }
        
        String fromWorld = event.getFrom().getWorld().getName();
        String toWorld = event.getTo().getWorld().getName();
        
        // Solo verificar si involucra un mundo de ciclo
        if (!cicloManager.isCycleWorld(fromWorld) && !cicloManager.isCycleWorld(toWorld)) {
            return;
        }
        
        // Bypass para admins
        if (player.hasPermission("apocalipsis.ciclo.bypass")) {
            return;
        }
        
        // Verificar si el jugador está montando algo
        Entity vehicle = player.getVehicle();
        if (vehicle != null && hasInventoryWithItems(vehicle)) {
            event.setCancelled(true);
            player.sendMessage("§c✖ No puedes teleportarte entre mundos mientras montas un animal con items.");
            player.sendMessage("§7Desmonta y vacía el inventario del animal primero.");
            
            plugin.getLogger().warning("[EntityProtection] Bloqueado teleporte con " + 
                vehicle.getType().name() + " por " + player.getName());
        }
    }
    
    /**
     * Bloquea portales de entidades con inventario entre mundos
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        Entity entity = event.getEntity();
        
        // Solo verificar entidades con inventario
        if (!hasInventoryWithItems(entity)) {
            return;
        }
        
        // Verificar si está cambiando de mundo
        if (event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            return;
        }
        
        String fromWorld = event.getFrom().getWorld().getName();
        String toWorld = event.getTo().getWorld().getName();
        
        // Solo bloquear si involucra un mundo de ciclo
        if (!cicloManager.isCycleWorld(fromWorld) && !cicloManager.isCycleWorld(toWorld)) {
            return;
        }
        
        // Bloquear el portal
        event.setCancelled(true);
        
        plugin.getLogger().warning("[EntityProtection] Bloqueado portal de " + 
            entity.getType().name() + " con items desde " + fromWorld + " a " + toWorld);
    }
    
    /**
     * Verifica si una entidad tiene inventario con items
     */
    private boolean hasInventoryWithItems(Entity entity) {
        if (entity instanceof ChestedHorse) {
            ChestedHorse horse = (ChestedHorse) entity;
            if (horse.isCarryingChest()) {
                return hasItemsInInventory(horse.getInventory());
            }
        }
        
        if (entity instanceof AbstractHorse) {
            AbstractHorse horse = (AbstractHorse) entity;
            return hasItemsInInventory(horse.getInventory());
        }
        
        if (entity instanceof Llama) {
            Llama llama = (Llama) entity;
            return hasItemsInInventory(llama.getInventory());
        }
        
        return false;
    }
    
    /**
     * Verifica si un inventario tiene items
     */
    private boolean hasItemsInInventory(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                return true;
            }
        }
        
        return false;
    }
}
