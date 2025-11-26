package me.apocalipsis.events;

import me.apocalipsis.Apocalipsis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.UUID;

/**
 * Listener para el evento "El Susurro en la Piedra Rota"
 * Maneja:
 * - Death de Criaturas de Forma
 * - Tracking de participación en combate
 */
public class SusurroPiedraRotaListener implements Listener {
    
    private final Apocalipsis plugin;
    
    public SusurroPiedraRotaListener(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Trackear cuando un jugador daña una criatura de forma
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        
        if (!(event.getEntity() instanceof Silverfish)) {
            return;
        }
        
        Silverfish criatura = (Silverfish) event.getEntity();
        
        // Verificar si es una Criatura de Forma
        if (criatura.customName() == null) {
            return;
        }
        
        String customName = criatura.customName().toString();
        if (!customName.contains("Criatura de Forma")) {
            return;
        }
        
        Player player = (Player) event.getDamager();
        
        // Obtener el evento activo
        SusurroPiedraRotaEvent evento = getEventoActivo();
        if (evento == null || !evento.isActive()) {
            return;
        }
        
        // Registrar participación
        UUID uuid = player.getUniqueId();
        evento.getParticipacionCriaturas().merge(uuid, 1, Integer::sum);
    }
    
    /**
     * Manejar muerte de Criaturas de Forma
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Silverfish)) {
            return;
        }
        
        Silverfish criatura = (Silverfish) event.getEntity();
        
        // Verificar si es una Criatura de Forma
        if (criatura.customName() == null) {
            return;
        }
        
        String customName = criatura.customName().toString();
        if (!customName.contains("Criatura de Forma")) {
            return;
        }
        
        // Obtener el evento activo
        SusurroPiedraRotaEvent evento = getEventoActivo();
        if (evento == null || !evento.isActive()) {
            return;
        }
        
        // La lógica de tracking de criaturas muertas está en el evento mismo
        // Solo nos aseguramos de que las drops sean las correctas
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        plugin.getLogger().info("[SusurroPiedraRota] Criatura de Forma eliminada");
    }
    
    /**
     * Detectar cuando jugador pisa pressure plate del puzzle de memoria
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        SusurroPiedraRotaEvent evento = getEventoActivo();
        if (evento == null || !evento.isActive()) {
            return;
        }
        
        Player player = event.getPlayer();
        Location to = event.getTo();
        
        if (to == null) return;
        
        // LISTENER DE PATRÓN ELIMINADO - Ya no hay minijuego de patrón
        // Verificar si pisó pressure plate del puzzle de memoria (Acto 2)
        // Material bloqueAbajo = to.clone().subtract(0, 1, 0).getBlock().getType();
        // if (bloqueAbajo == Material.GOLD_BLOCK || bloqueAbajo == Material.GLOWSTONE) {
        //     Location bloquePatron = to.clone().subtract(0, 1, 0);
        //     evento.verificarPatronJugadorPublic(player, bloquePatron);
        // }
    }
    
    /**
     * Obtener el evento activo si existe
     */
    private SusurroPiedraRotaEvent getEventoActivo() {
        EventController controller = plugin.getEventController();
        if (controller == null) {
            return null;
        }
        
        EventBase currentEvent = controller.getActiveEvent();
        if (currentEvent instanceof SusurroPiedraRotaEvent) {
            return (SusurroPiedraRotaEvent) currentEvent;
        }
        
        return null;
    }
}
