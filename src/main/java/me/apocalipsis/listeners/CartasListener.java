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
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        String title = event.getView().getTitle();
        
        // En el menú de cartas admin (solo lectura)
        if (title.startsWith("§c§lCartas para Santa")) {
            int slot = event.getRawSlot();
            
            // Si es un slot del menú (no del inventario del jugador)
            if (slot >= 0 && slot < event.getInventory().getSize()) {
                ItemStack item = event.getCurrentItem();
                
                // Permitir leer libros con click derecho, pero no sacarlos
                if (item != null && item.getType() == Material.WRITTEN_BOOK) {
                    if (event.isRightClick()) {
                        // Clonar el libro para que el admin pueda leerlo
                        ItemStack libroClonado = item.clone();
                        event.setCancelled(true);
                        
                        // Abrir el libro en el siguiente tick
                        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                            player.closeInventory();
                            player.openBook(libroClonado);
                        });
                        return;
                    }
                }
                
                // Cancelar cualquier otro tipo de click en el menú
                event.setCancelled(true);
            }
            return;
        }
        
        // En el menú de enviar carta
        if (title.equals("§d§lEnviar Carta a Santa")) {
            int slot = event.getRawSlot();
            
            // Permitir interacciones con el inventario del jugador (slots >= 27)
            if (slot >= 27) {
                return; // Dejar que el jugador interactúe con su inventario
            }
            
            // En el menú, solo permitir interactuar con el slot 11
            if (slot == 11) {
                // Permitir poner/sacar items del slot 11
                // Validar que sea un libro escrito
                ItemStack cursor = event.getCursor();
                ItemStack current = event.getCurrentItem();
                
                // Si está poniendo algo, verificar que sea un libro
                if (cursor != null && cursor.getType() != Material.AIR) {
                    if (cursor.getType() != Material.WRITTEN_BOOK && cursor.getType() != Material.WRITABLE_BOOK) {
                        player.sendMessage("§c✦ Solo puedes poner libros aquí (escritos o firmados).");
                        event.setCancelled(true);
                        return;
                    }
                }
                
                // Permitir la acción
                return;
            } else {
                // Cancelar interacciones con otros slots del menú
                event.setCancelled(true);
            }
        }
    }
}
