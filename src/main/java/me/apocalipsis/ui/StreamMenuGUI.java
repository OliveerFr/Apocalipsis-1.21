package me.apocalipsis.ui;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.missions.StreamFeaturesManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Menú GUI para Stream Features
 * Incluye visualización de tokens y acceso a la maleta de tokens
 */
public class StreamMenuGUI implements Listener {
    
    private final Apocalipsis plugin;
    private final StreamFeaturesManager streamManager;
    
    private static final String MENU_TITLE = "§6§l⭐ Stream Features";
    private static final String WALLET_TITLE = "§6§l💰 Maleta de Tokens";
    
    // Inventarios de maletas por jugador (UUID -> Inventory)
    private final Map<UUID, Inventory> playerWallets = new HashMap<>();
    
    public StreamMenuGUI(Apocalipsis plugin, StreamFeaturesManager streamManager) {
        this.plugin = plugin;
        this.streamManager = streamManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Abre el menú principal de Stream Features
     */
    public void openMainMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, MENU_TITLE);
        
        int tokens = streamManager.getPlayerTokens(player.getUniqueId());
        boolean streamerOnline = streamManager.isStreamerOnline();
        
        // Slot 10: Info de tokens
        menu.setItem(10, createInfoItem(tokens, streamerOnline));
        
        // Slot 12: Maleta de tokens
        menu.setItem(12, createWalletItem(player));
        
        // Slot 14: Canjear tokens
        menu.setItem(14, createRedeemItem(tokens));
        
        // Slot 16: Estado del streamer
        menu.setItem(16, createStreamerStatusItem(streamerOnline));
        
        // Decoración
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }
        
        for (int i = 0; i < 27; i++) {
            if (menu.getItem(i) == null) {
                menu.setItem(i, glass);
            }
        }
        
        player.openInventory(menu);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.8f, 1.2f);
    }
    
    /**
     * Abre la maleta de tokens del jugador
     */
    public void openWallet(Player player) {
        Inventory wallet = playerWallets.computeIfAbsent(player.getUniqueId(), uuid -> 
            Bukkit.createInventory(null, 27, WALLET_TITLE)
        );
        
        player.openInventory(wallet);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.0f);
        player.sendMessage("§6§l[MALETA] §7Guarda tus tokens aquí para canjearlos después.");
        player.sendMessage("§7Los tokens guardados aquí se §econvierten automáticamente§7 al cerrar.");
    }
    
    /**
     * Cuenta los tokens en la maleta del jugador
     */
    public int countTokensInWallet(Player player) {
        Inventory wallet = playerWallets.get(player.getUniqueId());
        if (wallet == null) return 0;
        
        int count = 0;
        for (ItemStack item : wallet.getContents()) {
            if (item != null && isTokenItem(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }
    
    /**
     * Verifica si un item es un token de stream
     */
    private boolean isTokenItem(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR) return false;
        if (!item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        
        String name = meta.getDisplayName();
        return name.contains("Token de Stream");
    }
    
    /**
     * Verifica si un item es un fragmento de stream
     */
    private boolean isFragmentItem(ItemStack item) {
        if (item == null || item.getType() != Material.EMERALD) return false;
        if (!item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        
        String name = meta.getDisplayName();
        return name.contains("Fragmento del Stream");
    }
    
    private ItemStack createInfoItem(int tokens, boolean streamerOnline) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§6§l⭐ Tus Tokens");
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Tokens disponibles: §e§l" + tokens);
            lore.add("");
            lore.add("§7Estado del streamer:");
            if (streamerOnline) {
                lore.add("§a§l✓ ONLINE");
                lore.add("§7¡Mata mobs para obtener tokens!");
            } else {
                lore.add("§c§l✗ OFFLINE");
                lore.add("§7Los drops están desactivados");
            }
            lore.add("");
            lore.add("§7Los tokens se obtienen matando");
            lore.add("§7mobs hostiles cuando el streamer");
            lore.add("§7está conectado.");
            lore.add("");
            lore.add("§8ID: info");
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createWalletItem(Player player) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§6§l💰 Maleta de Tokens");
            
            int tokensInWallet = countTokensInWallet(player);
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Tokens guardados: §e" + tokensInWallet);
            lore.add("");
            lore.add("§7Guarda tus tokens físicos aquí");
            lore.add("§7para organizarlos mejor.");
            lore.add("");
            lore.add("§7Al cerrar la maleta, los tokens");
            lore.add("§7se convierten automáticamente");
            lore.add("§7en tokens canjeables.");
            lore.add("");
            lore.add("§e▶ Click para abrir maleta");
            lore.add("§8ID: wallet");
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createRedeemItem(int tokens) {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§a§l💎 Canjear Tokens");
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Tokens disponibles: §e" + tokens);
            lore.add("");
            lore.add("§7Canjea tus tokens por");
            lore.add("§7recompensas exclusivas:");
            lore.add("");
            lore.add("§8• §eKit Diamante §7(8 tokens)");
            lore.add("§8• §eÉlitro Especial §7(15 tokens)");
            lore.add("§8• §eKit Netherite §7(25 tokens)");
            lore.add("§8• §eMega Pack §7(40 tokens)");
            lore.add("");
            lore.add("§e▶ Click para ver recompensas");
            lore.add("§8ID: redeem");
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createStreamerStatusItem(boolean online) {
        ItemStack item = new ItemStack(online ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(online ? "§a§l✓ Streamer Online" : "§7§l✗ Streamer Offline");
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            if (online) {
                lore.add("§a¡El streamer está conectado!");
                lore.add("");
                lore.add("§7Beneficios activos:");
                lore.add("§8• §aDrops especiales de tokens");
                lore.add("§8• §aFragmentos de esmeralda");
                lore.add("§8• §aMisiones exclusivas");
            } else {
                lore.add("§7El streamer no está conectado.");
                lore.add("");
                lore.add("§7Los beneficios especiales");
                lore.add("§7están desactivados hasta");
                lore.add("§7que vuelva a conectarse.");
            }
            lore.add("");
            lore.add("§8ID: status");
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        String title = event.getView().getTitle();
        
        // Click en el menú principal
        if (title.equals(MENU_TITLE)) {
            event.setCancelled(true);
            
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || !meta.hasLore()) return;
            
            List<String> lore = meta.getLore();
            if (lore == null) return;
            
            // Buscar ID en el lore
            String id = null;
            for (String line : lore) {
                if (line.startsWith("§8ID: ")) {
                    id = line.replace("§8ID: ", "");
                    break;
                }
            }
            
            if (id == null) return;
            
            player.closeInventory();
            
            switch (id) {
                case "wallet":
                    openWallet(player);
                    break;
                case "redeem":
                    streamManager.showRedeemMenu(player);
                    break;
                case "info":
                case "status":
                    // Solo información, no hacer nada
                    player.openInventory(event.getInventory());
                    break;
            }
        }
        // Click en la maleta - permitir movimiento de items
        else if (title.equals(WALLET_TITLE)) {
            // Permitir solo tokens y fragmentos
            ItemStack clicked = event.getCurrentItem();
            ItemStack cursor = event.getCursor();
            
            // Verificar item siendo colocado
            if (cursor != null && cursor.getType() != Material.AIR) {
                if (!isTokenItem(cursor) && !isFragmentItem(cursor)) {
                    event.setCancelled(true);
                    player.sendMessage("§c§l✗ §7Solo puedes guardar §eTokens de Stream §7y §aFragmentos§7.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                }
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        String title = event.getView().getTitle();
        
        // Al cerrar la maleta, convertir tokens a la base de datos
        if (title.equals(WALLET_TITLE)) {
            Inventory wallet = playerWallets.get(player.getUniqueId());
            if (wallet == null) return;
            
            int tokenCount = 0;
            int fragmentCount = 0;
            
            // Contar y remover tokens/fragmentos
            for (int i = 0; i < wallet.getSize(); i++) {
                ItemStack item = wallet.getItem(i);
                if (item == null) continue;
                
                if (isTokenItem(item)) {
                    tokenCount += item.getAmount();
                    wallet.setItem(i, null);
                } else if (isFragmentItem(item)) {
                    fragmentCount += item.getAmount();
                    wallet.setItem(i, null);
                }
            }
            
            // Agregar tokens a la base de datos
            if (tokenCount > 0) {
                streamManager.addPlayerTokens(player.getUniqueId(), tokenCount, "Guardado desde maleta");
                player.sendMessage("§a§l✓ §7Convertidos §e" + tokenCount + " tokens §7a tu saldo.");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
            }
            
            if (fragmentCount > 0) {
                player.sendMessage("§a§l✓ §7Guardados §a" + fragmentCount + " fragmentos §7en tu maleta.");
            }
        }
    }
    
    /**
     * Limpia la maleta de un jugador
     */
    public void clearWallet(UUID uuid) {
        playerWallets.remove(uuid);
    }
    
    /**
     * Guarda todas las maletas (para cuando se apaga el servidor)
     */
    public void saveAllWallets() {
        // Las maletas se limpian al cerrar, no necesitan persistencia adicional
        playerWallets.clear();
    }
}
