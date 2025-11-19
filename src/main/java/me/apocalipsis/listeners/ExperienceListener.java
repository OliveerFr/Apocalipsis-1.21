package me.apocalipsis.listeners;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;

import me.apocalipsis.Apocalipsis;

/**
 * Listener que captura eventos para otorgar XP de múltiples fuentes
 */
public class ExperienceListener implements Listener {
    
    private final Apocalipsis plugin;
    
    public ExperienceListener(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Otorga XP por matar mobs
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        // Early returns para reducir procesamiento
        if (plugin.getExperienceService() == null) return;
        
        Player killer = event.getEntity().getKiller();
        if (killer == null || !killer.isOnline()) return;
        
        EntityType entityType = event.getEntityType();
        plugin.getExperienceService().addMobKillXP(killer, entityType);
    }
    
    /**
     * Otorga XP por minar bloques (principalmente minerales)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // Early returns para performance
        if (plugin.getExperienceService() == null) return;
        
        Player player = event.getPlayer();
        if (player == null || !player.isOnline()) return;
        
        Material material = event.getBlock().getType();
        
        // Verificar que no sea un bloque colocado por jugador
        if (plugin.getBlockTracker() != null) {
            UUID owner = plugin.getBlockTracker().getBlockOwner(event.getBlock());
            if (owner != null) return; // Bloque colocado por jugador, no dar XP
        }
        
        // Dar XP (el servicio verifica si el bloque está configurado y tiene cooldown)
        plugin.getExperienceService().addMiningXP(player, material);
    }
    
    /**
     * Otorga XP por cosechar (farming)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHarvest(PlayerHarvestBlockEvent event) {
        if (plugin.getExperienceService() == null) return;
        
        Player player = event.getPlayer();
        if (player == null || !player.isOnline()) return;
        
        // Verificar si es un crop válido
        Material material = event.getHarvestedBlock().getType();
        if (isCrop(material)) {
            plugin.getExperienceService().addXP(player, 1, "cosechar", true);
        }
    }
    
    /**
     * Otorga XP por craftear items
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (plugin.getExperienceService() == null) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        if (!player.isOnline()) return;
        
        Material material = event.getRecipe().getResult().getType();
        
        // Obtener XP del config
        int xp = plugin.getConfigManager().getRecompensasConfig()
            .getInt("fuentes_xp.craftear.items." + material.name(), 0);
        
        if (xp > 0) {
            plugin.getExperienceService().addXP(player, xp, "craftear", true);
        }
    }
    
    /**
     * Otorga XP por pescar
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (plugin.getExperienceService() == null) return;
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        
        Player player = event.getPlayer();
        if (player == null || !player.isOnline()) return;
        
        int xp = plugin.getConfigManager().getRecompensasConfig()
            .getInt("fuentes_xp.pescar.xp", 2);
        
        if (xp > 0) {
            plugin.getExperienceService().addXP(player, xp, "pescar", true);
        }
    }
    
    /**
     * Verifica si un material es un crop
     */
    private boolean isCrop(Material material) {
        switch (material) {
            case WHEAT:
            case CARROTS:
            case POTATOES:
            case BEETROOTS:
            case NETHER_WART:
            case COCOA:
            case SWEET_BERRY_BUSH:
                return true;
            default:
                return false;
        }
    }
}
