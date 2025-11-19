package me.apocalipsis.listeners;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.disaster.Disaster;
import me.apocalipsis.disaster.DisasterBase;
import me.apocalipsis.disaster.DisasterEvasionTracker;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener que detecta desconexiones durante desastres activos
 * y delega al DisasterEvasionTracker para aplicar penalizaciones.
 */
public class DisasterEvasionListener implements Listener {
    
    private final Apocalipsis plugin;
    
    public DisasterEvasionListener(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Si NO hay desastre activo, no hay nada que hacer
        if (!plugin.getDisasterController().hasActiveDisaster()) {
            return;
        }
        
        // Si el jugador tiene exención de desastres, no aplicar penalización
        if (player.hasPermission("apocalipsis.exempt")) {
            return;
        }
        
        // Verificar evasión y aplicar penalizaciones
        DisasterEvasionTracker tracker = plugin.getDisasterEvasionTracker();
        boolean wasEvasion = tracker.onPlayerQuitDuringDisaster(player);
        
        if (wasEvasion && plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().warning("[EVASIÓN] Jugador " + player.getName() + " se desconectó durante desastre activo");
        }
    }
    
    /**
     * Registra el daño recibido por jugadores durante desastres
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        // Solo procesar daño a jugadores
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // Si NO hay desastre activo, no registrar
        if (!plugin.getDisasterController().hasActiveDisaster()) {
            return;
        }
        
        // Obtener el desastre actual y registrar daño
        Disaster activeDisaster = plugin.getDisasterController().getCurrentDisaster();
        if (activeDisaster instanceof DisasterBase) {
            double damage = event.getFinalDamage();
            ((DisasterBase) activeDisaster).handlePlayerDamageInDisaster(player, damage);
        }
    }
}
