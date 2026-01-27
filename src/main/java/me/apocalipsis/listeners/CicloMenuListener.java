package me.apocalipsis.listeners;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.gui.CicloMenuGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Listener para el menú GUI de ciclos
 */
public class CicloMenuListener implements Listener {
    
    private final Apocalipsis plugin;
    
    public CicloMenuListener(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof CicloMenuGUI)) {
            return;
        }
        
        // Cancelar el evento para que no se muevan ítems
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        CicloMenuGUI menu = (CicloMenuGUI) holder;
        
        // Manejar el click
        menu.handleClick(event.getCurrentItem(), event.getSlot());
    }
}
