package me.apocalipsis.listeners;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.missions.StreamFeaturesManager;

/**
 * Listener para manejar drops especiales de stream
 * cuando el streamer está online
 */
public class StreamDropListener implements Listener {
    
    private final Apocalipsis plugin;
    private final StreamFeaturesManager streamManager;
    
    public StreamDropListener(Apocalipsis plugin, StreamFeaturesManager streamManager) {
        this.plugin = plugin;
        this.streamManager = streamManager;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        // Solo si el streamer está online
        if (!streamManager.isStreamerOnline()) return;
        
        LivingEntity entity = event.getEntity();
        
        // Solo mobs hostiles
        if (!isHostileMob(entity.getType())) return;
        
        // Solo si fue matado por un jugador
        Player killer = entity.getKiller();
        if (killer == null) return;
        
        // Procesar drop especial (ahora el streamer también recibe drops)
        streamManager.processStreamDrop(killer);
    }
    
    /**
     * Verifica si el tipo de entidad es un mob hostil
     */
    private boolean isHostileMob(EntityType type) {
        switch (type) {
            case ZOMBIE:
            case SKELETON:
            case CREEPER:
            case SPIDER:
            case CAVE_SPIDER:
            case ENDERMAN:
            case BLAZE:
            case WITCH:
            case GUARDIAN:
            case ELDER_GUARDIAN:
            case SHULKER:
            case VINDICATOR:
            case EVOKER:
            case PILLAGER:
            case RAVAGER:
            case VEX:
            case PHANTOM:
            case DROWNED:
            case HUSK:
            case STRAY:
            case WITHER_SKELETON:
            case ZOMBIFIED_PIGLIN:
            case PIGLIN:
            case PIGLIN_BRUTE:
            case HOGLIN:
            case ZOGLIN:
            case SLIME:
            case MAGMA_CUBE:
            case SILVERFISH:
            case ENDERMITE:
                return true;
            default:
                return false;
        }
    }
}
