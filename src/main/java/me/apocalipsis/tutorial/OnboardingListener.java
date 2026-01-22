/*
 * Apocalipsis Plugin - Listener de Onboarding
 * Detecta acciones del jugador para completar hitos
 */
package me.apocalipsis.tutorial;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener que detecta acciones para el onboarding
 */
public class OnboardingListener implements Listener {
    
    private final OnboardingManager onboardingManager;
    
    // Tracking de distancia caminada
    private final Map<UUID, org.bukkit.Location> lastLocations;
    
    public OnboardingListener(OnboardingManager onboardingManager) {
        this.onboardingManager = onboardingManager;
        this.lastLocations = new HashMap<>();
    }
    
    /**
     * Detecta movimiento para contar bloques caminados
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        if (!onboardingManager.isOnboardingActive(uuid)) {
            return;
        }
        
        org.bukkit.Location from = event.getFrom();
        org.bukkit.Location to = event.getTo();
        
        if (to == null) return;
        
        // Solo horizontal (no contar saltos)
        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        
        // Calcular distancia
        org.bukkit.Location lastLoc = lastLocations.get(uuid);
        if (lastLoc == null) {
            lastLocations.put(uuid, to.clone());
            return;
        }
        
        // Verificar que estén en el mismo mundo antes de calcular distancia
        if (!lastLoc.getWorld().equals(to.getWorld())) {
            lastLocations.put(uuid, to.clone());
            return;
        }
        
        double distance = lastLoc.distance(to);
        if (distance > 0.5) { // Solo si se movió más de medio bloque
            int blocks = (int) Math.floor(distance);
            if (blocks > 0) {
                onboardingManager.onPlayerWalk(player, blocks);
                lastLocations.put(uuid, to.clone());
            }
        }
    }
    
    /**
     * Detecta crafteo
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();
        
        if (!onboardingManager.isOnboardingActive(uuid)) {
            return;
        }
        
        onboardingManager.onPlayerCraft(player);
    }
    
    /**
     * Detecta colocación de bloques
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        if (!onboardingManager.isOnboardingActive(uuid)) {
            return;
        }
        
        onboardingManager.onPlayerPlaceBlock(player);
    }
    
    /**
     * Limpieza al desconectar
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastLocations.remove(uuid);
    }
}
