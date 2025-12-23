package me.apocalipsis.listeners;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.ui.CartasManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

/**
 * Listener para el sistema de cartas
 */
public class CartasListener implements Listener {
    
    private final Apocalipsis plugin;
    private final CartasManager cartasManager;
    
    public CartasListener(Apocalipsis plugin, CartasManager cartasManager) {
        this.plugin = plugin;
        this.cartasManager = cartasManager;
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        
        Inventory inventory = event.getInventory();
        String title = event.getView().getTitle();
        
        // Verificar si es el menú de enviar carta
        if (title.equals("§d§lEnviar Carta a Santa")) {
            cartasManager.procesarCierreCarta(player, inventory);
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        String title = event.getView().getTitle();
        
        // En el menú de cartas admin (solo lectura), permitir click derecho en libros para leer
        if (title.startsWith("§c§lCartas para Santa")) {
            // Permitir leer pero no sacar items
            if (event.getCurrentItem() != null) {
                event.setCancelled(true); // No permitir sacar items
            }
            return;
        }
        
        // En el menú de enviar carta
        if (title.equals("§d§lEnviar Carta a Santa")) {
            // Solo permitir interactuar con el slot 11 (donde se pone el libro)
            if (event.getRawSlot() == 11) {
                // Permitir poner/sacar items del slot 11
                return;
            } else {
                // Cancelar interacciones con otros slots
                event.setCancelled(true);
            }
        }
    }
}
