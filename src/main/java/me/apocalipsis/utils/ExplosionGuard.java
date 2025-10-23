package me.apocalipsis.utils;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.state.ServerState;

/**
 * Limita el daño por explosiones a los jugadores durante Lluvia de Fuego
 * Para evitar muertes instantáneas por explosiones de SmallFireball
 * 
 * [#9] Añadido: control TNT masiva y bloqueo en SAFE_MODE
 */
public class ExplosionGuard implements Listener {

    private final Apocalipsis plugin;
    private final double maxHeartsFromExplosion;

    public ExplosionGuard(Apocalipsis plugin) {
        this.plugin = plugin;
        // Cargar desde config, por defecto 1.5 corazones (3.0 HP)
        this.maxHeartsFromExplosion = plugin.getConfig().getDouble("ajustes.max_explosion_hearts", 1.5);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onExplosionDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        DamageCause cause = event.getCause();
        if (cause != DamageCause.ENTITY_EXPLOSION && cause != DamageCause.BLOCK_EXPLOSION) {
            return;
        }

        // Limitar daño a maxHeartsFromExplosion
        double maxDamage = maxHeartsFromExplosion * 2.0; // 1 corazón = 2.0 HP
        if (event.getDamage() > maxDamage) {
            event.setDamage(maxDamage);
        }
    }
    
    /**
     * [#9] Control de explosiones TNT y desastres en SAFE_MODE
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        ServerState estado = plugin.getStateManager().getCurrentState();
        
        // Contar explosiones TNT
        if (event.getEntity() instanceof TNTPrimed) {
            plugin.getDisasterController().onTNTExplosion();
            
            // [#9] Bloquear TNT en SAFE_MODE
            if (estado == ServerState.SAFE_MODE && plugin.getConfigManager().isBloquearTNTEnSafe()) {
                event.setCancelled(true);
                event.blockList().clear();
                return;
            }
        }
        
        // [#9] Bloquear explosiones de desastres en SAFE_MODE
        if (estado == ServerState.SAFE_MODE && plugin.getConfigManager().isBloquearExplosionesDesastres()) {
            // Verificar si la explosión es de un desastre (SmallFireball)
            if (event.getEntityType() == EntityType.SMALL_FIREBALL) {
                event.setCancelled(true);
                event.blockList().clear();
            }
        }
    }
}
