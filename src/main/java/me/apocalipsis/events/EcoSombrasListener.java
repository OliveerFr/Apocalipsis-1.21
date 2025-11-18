package me.apocalipsis.events;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Listener para interacciones del evento Eco de las Sombras Largas
 */
public class EcoSombrasListener implements Listener {
    
    private final EcoSombrasEvent evento;
    private final EcoSombrasItems items;
    
    public EcoSombrasListener(EcoSombrasEvent evento, EcoSombrasItems items) {
        this.evento = evento;
        this.items = items;
    }
    
    /**
     * Maneja la muerte de Sombras Largas y Guardian
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        
        // Verificar si es una entidad del evento
        if (!evento.getEntidadesEvento().contains(entity.getUniqueId())) {
            return;
        }
        
        Player killer = event.getEntity().getKiller();
        
        // Verificar si es el Guardian
        if (entity.getCustomName() != null && entity.getCustomName().contains("Guardián del Umbral")) {
            evento.onGuardianDerrotado();
            event.getDrops().clear();
            return;
        }
        
        // Solo procesar Zombies (Sombras Largas)
        if (!(entity instanceof Zombie)) {
            return;
        }
        
        // Notificar al evento
        evento.onSombraLargaMuerta(killer);
        
        // Drop: Fragmento de Sombra
        if (evento.getActoActual() == EcoSombrasEvent.Acto.SOMBRAS_LARGAS ||
            evento.getActoActual() == EcoSombrasEvent.Acto.NUCLEO ||
            evento.getActoActual() == EcoSombrasEvent.Acto.ANCLAS) {
            
            event.getDrops().clear(); // Limpiar drops normales
            
            // 1-2 fragmentos
            int cantidad = 1 + (Math.random() < 0.5 ? 1 : 0);
            ItemStack fragmento = items.crearFragmentoSombra();
            fragmento.setAmount(cantidad);
            
            event.getDrops().add(fragmento);
        }
    }
    
    /**
     * Maneja el sellado de Anclas del Mundo
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Solo click derecho
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        // Solo durante acto de anclas
        if (evento.getActoActual() != EcoSombrasEvent.Acto.ANCLAS) {
            return;
        }
        
        // Verificar que clickeó un RESPAWN_ANCHOR
        if (event.getClickedBlock() == null || 
            event.getClickedBlock().getType() != Material.RESPAWN_ANCHOR) {
            return;
        }
        
        Location clickedLoc = event.getClickedBlock().getLocation();
        Player player = event.getPlayer();
        
        // Buscar qué ancla es
        List<Location> anclas = evento.getAnclaLocations();
        for (int i = 0; i < anclas.size(); i++) {
            Location anclaLoc = anclas.get(i);
            
            // Verificar que está cerca del centro del ancla
            if (clickedLoc.distance(anclaLoc.clone().add(0, 1, 0)) < 2) {
                // Verificar que tiene suficientes fragmentos
                int requeridos = 5;
                if (items.contarFragmentos(player) < requeridos) {
                    player.sendMessage("§cNecesitas §8" + requeridos + " Fragmentos de Sombra §cpara sellar esta ancla.");
                    player.sendMessage("§7Tienes: §e" + items.contarFragmentos(player));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    event.setCancelled(true);
                    return;
                }
                
                // Consumir fragmentos
                if (items.consumirFragmentos(player, requeridos)) {
                    // Sellar ancla
                    evento.sellarAncla(i, player);
                    player.sendMessage("§aHas sellado el ancla con §8" + requeridos + " Fragmentos de Sombra§a.");
                }
                
                event.setCancelled(true);
                return;
            }
        }
    }
}
