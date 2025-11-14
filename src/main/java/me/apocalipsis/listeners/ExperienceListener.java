package me.apocalipsis.listeners;

import me.apocalipsis.Apocalipsis;
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
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        
        EntityType entityType = event.getEntityType();
        
        // Verificar que el servicio existe
        if (plugin.getExperienceService() == null) {
            plugin.getLogger().warning("[XP] ExperienceService es null!");
            return;
        }
        
        boolean success = plugin.getExperienceService().addMobKillXP(killer, entityType);
        
        // Log para debug
        plugin.getLogger().info("[XP-Debug] " + killer.getName() + " mató " + entityType.name() + " - XP otorgado: " + (success ? "SI" : "NO"));
    }
    
    /**
     * Otorga XP por minar bloques
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();
        
        // Filtrar bloques que no son naturales (colocados por jugadores)
        // Esto se puede mejorar con el BlockTrackListener existente
        boolean success = plugin.getExperienceService().addMiningXP(player, material);
        
        // Log para debug (solo materiales importantes)
        if (success && (material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE)) {
            plugin.getLogger().info("[XP] " + player.getName() + " ganó XP por minar " + material.name());
        }
    }
    
    /**
     * Otorga XP por cosechar (farming)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHarvest(PlayerHarvestBlockEvent event) {
        Player player = event.getPlayer();
        
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
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
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
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        
        Player player = event.getPlayer();
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
