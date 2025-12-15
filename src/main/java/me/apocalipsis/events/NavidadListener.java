package me.apocalipsis.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

/**
 * Listener para interacciones del Evento Navidad
 * 
 * Maneja:
 * - Protección de bloques decorativos del evento
 * - Interacción con cofres/regalos
 * - Interacción con el árbol de Navidad
 */
public class NavidadListener implements Listener {
    
    private final NavidadEvent navidadEvent;
    
    public NavidadListener(NavidadEvent navidadEvent) {
        this.navidadEvent = navidadEvent;
    }
    
    /**
     * Protege bloques decorativos del evento de ser rotos
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        
        if (!navidadEvent.isActive()) return;
        
        Block block = event.getBlock();
        
        // Proteger bloques del árbol si está configurado
        if (navidadEvent.esParteDeLArbol(block.getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c✦ No puedes romper el árbol de Navidad durante el evento.");
        }
    }
    
    /**
     * Maneja interacción con cofres/regalos
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!navidadEvent.isActive()) return;
        
        Block block = event.getClickedBlock();
        if (block == null) return;
        
        // Detectar apertura de cofres si los regalos están activos
        if (navidadEvent.isRegalosActivos() && 
            (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST)) {
            
            Player player = event.getPlayer();
            
            // Verificar si el jugador ya abrió un regalo
            if (navidadEvent.yaRecibioRegalo(player)) {
                player.sendMessage("§c✦ Ya has recibido tu regalo de Navidad.");
                return;
            }
            
            // Marcar como que recibió regalo
            navidadEvent.marcarRegaloRecibido(player);
            
            // Efectos visuales y sonoros
            player.getWorld().spawnParticle(
                org.bukkit.Particle.FIREWORK,
                block.getLocation().add(0.5, 1, 0.5),
                20, 0.3, 0.3, 0.3, 0.05
            );
            
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            
            // Mensaje
            String mensaje = navidadEvent.getMensajeRegaloRecibido();
            player.sendMessage(mensaje);
        }
    }
}
