package me.apocalipsis.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.utils.BlockOwnershipTracker;

/**
 * Listener que rastrea quién coloca y rompe bloques para prevenir griefing.
 * Prioridad MONITOR para ejecutarse después de otros plugins de protección.
 */
public class BlockTrackListener implements Listener {
    
    private final BlockOwnershipTracker tracker;
    
    public BlockTrackListener(Apocalipsis plugin) {
        this.tracker = plugin.getBlockTracker();
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Solo trackear si el evento no fue cancelado por otro plugin (protecciones)
        tracker.trackBlockPlacement(event.getBlock(), event.getPlayer());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // Limpiar el tracking cuando un bloque es roto
        tracker.trackBlockBreak(event.getBlock());
    }
}
