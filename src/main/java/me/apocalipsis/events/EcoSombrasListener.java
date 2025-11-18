package me.apocalipsis.events;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Giant;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import me.apocalipsis.events.gameplay.QTESystem;

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
     * Maneja daño para feedback visual y sistema de fases del Guardián
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        Entity damager = event.getDamager();
        
        // Si un jugador golpea al Guardián
        if (damaged instanceof Giant && damaged.getCustomName() != null && 
            damaged.getCustomName().contains("Guardián del Umbral") && damager instanceof Player) {
            
            Player attacker = (Player) damager;
            
            // Procesar daño con el sistema de fases
            if (evento.getGuardianPhaseSystem() != null) {
                evento.getGuardianPhaseSystem().processDamage(event);
            }
            
            // Registrar hit para combo system
            if (evento.getFeedbackSystem() != null && !event.isCancelled()) {
                evento.getFeedbackSystem().registerHit(attacker);
                
                // Determinar si fue crítico (20% chance)
                boolean isCritical = Math.random() < 0.2;
                evento.getFeedbackSystem().showHitMarker(attacker, isCritical);
            }
        }
        
        // Si el Guardián o una sombra golpea a un jugador
        if (damaged instanceof Player && damager instanceof LivingEntity) {
            Player player = (Player) damaged;
            
            // Mostrar indicador direccional de daño
            if (evento.getFeedbackSystem() != null && evento.getEntidadesEvento().contains(damager.getUniqueId())) {
                evento.getFeedbackSystem().showDirectionalDamageIndicator(player, damager.getLocation());
            }
        }
        
        // Si un jugador mata una sombra larga (mostrar damage number)
        if (damaged instanceof Zombie && damager instanceof Player) {
            Player attacker = (Player) damager;
            
            if (evento.getFeedbackSystem() != null && evento.getEntidadesEvento().contains(damaged.getUniqueId())) {
                boolean isCritical = Math.random() < 0.15;
                evento.getFeedbackSystem().showDamageNumber(
                    damaged.getLocation().add(0, 1, 0), 
                    event.getFinalDamage(), 
                    isCritical
                );
            }
        }
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
     * Maneja el sellado de Anclas del Mundo y detección de clicks para QTE
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // 🎮 REGISTRAR CLICKS PARA QTE SYSTEM
        // Detectar tipo de input
        QTESystem.InputType inputType = null;
        
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            inputType = QTESystem.InputType.LEFT_CLICK;
        } else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            inputType = QTESystem.InputType.RIGHT_CLICK;
        }
        
        // Registrar input en el sistema QTE (si hay QTE activo para el jugador)
        if (inputType != null) {
            evento.getQTESystem().registerInput(player, inputType);
        }
        
        // SELLADO DE ANCLAS - Solo durante acto de anclas
        if (evento.getActoActual() != EcoSombrasEvent.Acto.ANCLAS) {
            return;
        }
        
        // Solo click derecho en bloques
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        // Verificar que clickeó un RESPAWN_ANCHOR
        if (event.getClickedBlock() == null || 
            event.getClickedBlock().getType() != Material.RESPAWN_ANCHOR) {
            return;
        }
        
        Location clickedLoc = event.getClickedBlock().getLocation();
        
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
