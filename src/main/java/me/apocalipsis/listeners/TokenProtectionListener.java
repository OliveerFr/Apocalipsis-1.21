package me.apocalipsis.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.apocalipsis.Apocalipsis;

/**
 * Protege los tokens y fragmentos de stream para que solo se usen en el sistema de canje
 * Bloquea: crafting, anvil, grindstone, smithing, brewing, drop intencional
 */
public class TokenProtectionListener implements Listener {
    
    private final Apocalipsis plugin;
    
    public TokenProtectionListener(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Verifica si un item es un token o fragmento de stream
     */
    private boolean isStreamItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) return false;
        
        String name = meta.getDisplayName();
        
        // Verificar por nombre y tipo
        if (item.getType() == Material.NETHER_STAR && name.contains("Token de Stream")) {
            return true;
        }
        
        if (item.getType() == Material.EMERALD && name.contains("Fragmento del Stream")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Bloquea crafting con tokens/fragmentos
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraft(CraftItemEvent event) {
        // Verificar si algún ingrediente es un token/fragmento
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (isStreamItem(item)) {
                event.setCancelled(true);
                
                if (event.getWhoClicked() instanceof Player) {
                    Player player = (Player) event.getWhoClicked();
                    player.sendMessage("§c✗ No puedes usar Tokens/Fragmentos de Stream en crafting.");
                    player.sendMessage("§7Úsalos con §e/avo canjear §7para obtener recompensas.");
                }
                return;
            }
        }
    }
    
    /**
     * Bloquea uso en anvil (yunque)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnvil(PrepareAnvilEvent event) {
        ItemStack[] items = event.getInventory().getContents();
        
        for (ItemStack item : items) {
            if (isStreamItem(item)) {
                event.setResult(null);
                
                if (event.getView().getPlayer() instanceof Player) {
                    Player player = (Player) event.getView().getPlayer();
                    player.sendMessage("§c✗ No puedes usar Tokens/Fragmentos de Stream en el yunque.");
                }
                return;
            }
        }
    }
    
    /**
     * Bloquea uso en grindstone (piedra de afilar)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGrindstone(PrepareGrindstoneEvent event) {
        ItemStack[] items = event.getInventory().getContents();
        
        for (ItemStack item : items) {
            if (isStreamItem(item)) {
                event.setResult(null);
                return;
            }
        }
    }
    
    /**
     * Bloquea uso en smithing table
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSmithing(PrepareSmithingEvent event) {
        ItemStack[] items = event.getInventory().getContents();
        
        for (ItemStack item : items) {
            if (isStreamItem(item)) {
                event.setResult(null);
                return;
            }
        }
    }
    
    /**
     * Bloquea uso en brewing stand (soporte de pociones)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBrew(BrewEvent event) {
        ItemStack ingredient = event.getContents().getIngredient();
        
        if (isStreamItem(ingredient)) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Advierte al tirar tokens (no bloquea pero avisa)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        
        if (isStreamItem(item)) {
            Player player = event.getPlayer();
            player.sendMessage("§e⚠ Cuidado: Estás tirando un Token/Fragmento de Stream.");
            player.sendMessage("§7Úsalo con §e/avo canjear §7o guárdalo en un cofre seguro.");
        }
    }
    
    /**
     * Bloquea mover tokens a través de inventarios de forma automatizada
     * (previene hoppers, etc.)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        
        if (isStreamItem(item)) {
            // Solo bloquear si NO es un jugador moviéndolo manualmente
            if (event.getSource().getHolder() == null || !(event.getSource().getHolder() instanceof Player)) {
                event.setCancelled(true);
            }
        }
    }
}
