/*
 * Apocalipsis Plugin - Sistema de Reclamación de Recompensas
 * Sistema centralizado para almacenar y reclamar recompensas con expiración
 */
package me.apocalipsis.ui;

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
import org.bukkit.scheduler.BukkitTask;

import me.apocalipsis.Apocalipsis;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema centralizado de recompensas reclamables.
 * Permite almacenar recompensas de cualquier evento y reclamarlas
 * con /recompensa dentro de un tiempo límite (1 hora por defecto).
 */
public class RewardClaimSystem implements Listener {
    
    private final Apocalipsis plugin;
    
    // Almacén de recompensas pendientes por jugador
    // UUID -> Lista de RewardPackage (pueden tener múltiples de diferentes eventos)
    private final Map<UUID, List<RewardPackage>> pendingRewards = new ConcurrentHashMap<>();
    
    // Jugadores con menú abierto
    private final Set<UUID> playersWithMenuOpen = new HashSet<>();
    
    // Título del inventario (para identificar el menú)
    private static final String MENU_TITLE = "§5§l✦ §dRecompensas Pendientes §5§l✦";
    private static final String CLAIM_TITLE = "§a§l✦ §eReclamar Recompensas §a§l✦";
    
    // Tarea de limpieza de recompensas expiradas
    private BukkitTask cleanupTask;
    
    public RewardClaimSystem(Apocalipsis plugin) {
        this.plugin = plugin;
        
        // Registrar listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Iniciar tarea de limpieza cada 5 minutos
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupExpiredRewards, 
            20L * 60 * 5, // Inicio: 5 minutos
            20L * 60 * 5  // Cada 5 minutos
        );
        
        plugin.getLogger().info("[RewardClaimSystem] ✓ Sistema de recompensas reclamables iniciado");
    }
    
    /**
     * Paquete de recompensas de un evento específico
     */
    public static class RewardPackage {
        private final String eventName;
        private final String eventDisplayName;
        private final List<ItemStack> items;
        private final long expirationTime;
        private final String rankAchieved;
        private final int psAwarded;
        
        public RewardPackage(String eventName, String eventDisplayName, List<ItemStack> items, 
                           long expirationMillis, String rankAchieved, int psAwarded) {
            this.eventName = eventName;
            this.eventDisplayName = eventDisplayName;
            this.items = new ArrayList<>(items);
            this.expirationTime = System.currentTimeMillis() + expirationMillis;
            this.rankAchieved = rankAchieved;
            this.psAwarded = psAwarded;
        }
        
        public String getEventName() { return eventName; }
        public String getEventDisplayName() { return eventDisplayName; }
        public List<ItemStack> getItems() { return items; }
        public long getExpirationTime() { return expirationTime; }
        public String getRankAchieved() { return rankAchieved; }
        public int getPsAwarded() { return psAwarded; }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
        
        public long getTimeRemaining() {
            return Math.max(0, expirationTime - System.currentTimeMillis());
        }
        
        public String getTimeRemainingFormatted() {
            long remaining = getTimeRemaining();
            if (remaining <= 0) return "§c¡Expirado!";
            
            long minutes = (remaining / 1000) / 60;
            long seconds = (remaining / 1000) % 60;
            
            if (minutes >= 60) {
                long hours = minutes / 60;
                minutes = minutes % 60;
                return String.format("§a%dh %dm", hours, minutes);
            }
            return String.format("§e%dm %ds", minutes, seconds);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // API PÚBLICA - Agregar recompensas
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Añade recompensas pendientes para un jugador.
     * @param playerUUID UUID del jugador
     * @param eventName ID interno del evento
     * @param eventDisplayName Nombre visible del evento
     * @param items Lista de items a entregar
     * @param expirationMinutes Minutos hasta que expire (ej: 60 = 1 hora)
     * @param rankAchieved Rango logrado (PLATINUM, GOLD, SILVER, BRONZE)
     * @param psAwarded PS otorgados
     */
    public void addRewards(UUID playerUUID, String eventName, String eventDisplayName, 
                          List<ItemStack> items, int expirationMinutes, String rankAchieved, int psAwarded) {
        if (items == null || items.isEmpty()) return;
        
        RewardPackage pkg = new RewardPackage(
            eventName, 
            eventDisplayName, 
            items, 
            expirationMinutes * 60 * 1000L,
            rankAchieved,
            psAwarded
        );
        
        pendingRewards.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(pkg);
        
        // Notificar al jugador si está online
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            notifyPlayerRewards(player, pkg);
        }
        
        plugin.getLogger().info(String.format(
            "[RewardClaimSystem] Recompensas añadidas para %s: %d items del evento %s (expira en %dm)",
            playerUUID, items.size(), eventName, expirationMinutes
        ));
    }
    
    /**
     * Verifica si un jugador tiene recompensas pendientes
     */
    public boolean hasRewards(UUID playerUUID) {
        List<RewardPackage> rewards = pendingRewards.get(playerUUID);
        if (rewards == null || rewards.isEmpty()) return false;
        
        // Verificar que al menos una no esté expirada
        return rewards.stream().anyMatch(pkg -> !pkg.isExpired());
    }
    
    /**
     * Obtiene el número de recompensas pendientes (items totales)
     */
    public int getTotalPendingItems(UUID playerUUID) {
        List<RewardPackage> rewards = pendingRewards.get(playerUUID);
        if (rewards == null) return 0;
        
        return rewards.stream()
            .filter(pkg -> !pkg.isExpired())
            .mapToInt(pkg -> pkg.getItems().size())
            .sum();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // MENÚ DE RECOMPENSAS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Abre el menú de recompensas para un jugador
     */
    public void openRewardsMenu(Player player) {
        try {
            UUID uuid = player.getUniqueId();
            List<RewardPackage> rewards = pendingRewards.get(uuid);
            
            // Filtrar expirados
            if (rewards != null) {
                rewards.removeIf(RewardPackage::isExpired);
            }
            
            if (rewards == null || rewards.isEmpty()) {
                player.sendMessage("");
                player.sendMessage("§8§m═══════════════════════════════════════════");
                player.sendMessage("");
                player.sendMessage("  §c✘ §7No tienes recompensas pendientes.");
                player.sendMessage("");
                player.sendMessage("  §8Completa eventos para obtener recompensas.");
                player.sendMessage("");
                player.sendMessage("§8§m═══════════════════════════════════════════");
                player.sendMessage("");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            
            // Crear menú principal (lista de paquetes de recompensas)
            Inventory menu = Bukkit.createInventory(null, 54, MENU_TITLE);
            
            // Decoración superior
            ItemStack border = createBorderItem();
            for (int i = 0; i < 9; i++) {
                menu.setItem(i, border);
            }
            for (int i = 45; i < 54; i++) {
                menu.setItem(i, border);
            }
            
            // Mostrar cada paquete de recompensas como un item
            int slot = 10;
            for (int i = 0; i < rewards.size() && slot < 44; i++) {
                RewardPackage pkg = rewards.get(i);
                if (pkg.isExpired()) continue;
                
                ItemStack packageItem = createPackageDisplayItem(pkg, i);
                menu.setItem(slot, packageItem);
                
                slot++;
                if ((slot + 1) % 9 == 0) slot += 2; // Saltar bordes
            }
            
            // Info item
            ItemStack infoItem = new ItemStack(Material.BOOK);
            ItemMeta infoMeta = infoItem.getItemMeta();
            if (infoMeta != null) {
                infoMeta.setDisplayName("§e§lℹ Información");
                infoMeta.setLore(Arrays.asList(
                    "",
                    "§7Click en una recompensa para",
                    "§7abrir el cofre y reclamar items.",
                    "",
                    "§7Los items se colocan en tu inventario.",
                    "§7Si está lleno, caerán al suelo.",
                    "",
                    "§c⚠ §7Las recompensas expiran después",
                    "§c⚠ §7del tiempo indicado.",
                    ""
                ));
                infoItem.setItemMeta(infoMeta);
            }
            menu.setItem(49, infoItem);
            
            playersWithMenuOpen.add(uuid);
            player.openInventory(menu);
            player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.2f);
        } catch (Exception e) {
            player.sendMessage("§c✘ Error al abrir el menú de recompensas. Contacta a un admin.");
            plugin.getLogger().severe("[RewardClaimSystem] Error en openRewardsMenu: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Abre el cofre de un paquete específico para reclamar
     */
    private void openClaimChest(Player player, int packageIndex) {
        UUID uuid = player.getUniqueId();
        List<RewardPackage> rewards = pendingRewards.get(uuid);
        
        if (rewards == null || packageIndex >= rewards.size()) {
            player.sendMessage("§c✘ Recompensa no encontrada.");
            return;
        }
        
        RewardPackage pkg = rewards.get(packageIndex);
        if (pkg.isExpired()) {
            player.sendMessage("§c✘ Esta recompensa ha expirado.");
            rewards.remove(packageIndex);
            return;
        }
        
        // Crear inventario del tamaño necesario
        int size = Math.min(54, ((pkg.getItems().size() / 9) + 1) * 9);
        if (size < 9) size = 9;
        if (size > 54) size = 54;
        
        String title = CLAIM_TITLE + " §7#" + (packageIndex + 1);
        Inventory claimInv = Bukkit.createInventory(null, size, title);
        
        // Poner los items
        for (int i = 0; i < pkg.getItems().size() && i < size; i++) {
            claimInv.setItem(i, pkg.getItems().get(i).clone());
        }
        
        player.openInventory(claimInv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // LISTENERS
    // ═══════════════════════════════════════════════════════════════════
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Menú principal de recompensas
        if (title.equals(MENU_TITLE)) {
            event.setCancelled(true);
            
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) return;
            if (clicked.getType() == Material.BOOK) return;
            
            // Buscar índice del paquete
            if (clicked.hasItemMeta() && clicked.getItemMeta().hasLore()) {
                List<String> lore = clicked.getItemMeta().getLore();
                if (lore != null) {
                    for (String line : lore) {
                        if (line.contains("§8ID: #")) {
                            try {
                                int index = Integer.parseInt(line.replace("§8ID: #", "").trim());
                                player.closeInventory();
                                Bukkit.getScheduler().runTaskLater(plugin, () -> openClaimChest(player, index), 2L);
                                return;
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
            return;
        }
        
        // Cofre de reclamación
        if (title.startsWith(CLAIM_TITLE)) {
            // Permitir sacar items (shift-click o click normal para tomar)
            // Pero no permitir meter items
            
            if (event.getClickedInventory() == player.getInventory()) {
                // Click en inventario del jugador - OK
                return;
            }
            
            // Click en el cofre de recompensas
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) {
                event.setCancelled(true);
                return;
            }
            
            // [v2.0] Verificar si es un bloque de protección especial
            if (isProtectionBlockItem(clicked)) {
                event.setCancelled(true);
                
                // Extraer el comando original del lore
                String originalCommand = extractProtectionCommand(clicked);
                if (originalCommand != null && !originalCommand.isEmpty()) {
                    final String playerName = player.getName();
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            // [FIX v2.3] Convertir "ps give" a "ps admin give" para entrega directa
                            // ps admin give <player> <block> [amount] - da directamente al inventario
                            String adminCommand = originalCommand;
                            
                            // Convertir: "ps give Player 15" -> "ps admin give Player 15"
                            // Convertir: "ps give Player 15 2" -> "ps admin give Player 15 2"
                            if (adminCommand.toLowerCase().startsWith("ps give ")) {
                                adminCommand = "ps admin give " + adminCommand.substring(8);
                            }
                            
                            plugin.getLogger().info("[RewardClaimSystem] Ejecutando: " + adminCommand);
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), adminCommand);
                            plugin.getLogger().info("[RewardClaimSystem] ✓ Bloque de protección entregado a " + playerName);
                            
                            // Notificar al jugador
                            player.sendMessage("");
                            player.sendMessage("§a§l🛡 §a¡Bloque de protección reclamado!");
                            player.sendMessage("§7El bloque ha sido añadido a tu inventario.");
                            player.sendMessage("");
                            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
                        } catch (Exception e) {
                            player.sendMessage("§c✘ Error al reclamar bloque de protección. Contacta a un admin.");
                            plugin.getLogger().severe("[RewardClaimSystem] Error: " + e.getMessage());
                        }
                    });
                    
                    // Remover el item del inventario del menú
                    event.getClickedInventory().setItem(event.getSlot(), null);
                } else {
                    player.sendMessage("§c✘ Error: Comando de protección no válido.");
                }
                return;
            }
            
            // Permitir tomar otros items normalmente
            // El item se removerá automáticamente
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        String title = event.getView().getTitle();
        
        playersWithMenuOpen.remove(uuid);
        
        // Si cerró el cofre de reclamación, actualizar el paquete
        if (title.startsWith(CLAIM_TITLE)) {
            // Extraer índice del título
            try {
                String indexStr = title.substring(title.lastIndexOf("#") + 1).trim();
                int packageIndex = Integer.parseInt(indexStr) - 1;
                
                List<RewardPackage> rewards = pendingRewards.get(uuid);
                if (rewards != null && packageIndex < rewards.size()) {
                    RewardPackage pkg = rewards.get(packageIndex);
                    
                    // Verificar qué items quedaron en el inventario
                    Inventory inv = event.getInventory();
                    List<ItemStack> remainingItems = new ArrayList<>();
                    
                    ItemStack[] contents = inv.getContents();
                    if (contents != null) {
                        for (ItemStack item : contents) {
                            if (item != null && item.getType() != Material.AIR) {
                                remainingItems.add(item);
                            }
                        }
                    }
                    
                    if (remainingItems.isEmpty()) {
                        // Todos los items reclamados - eliminar paquete
                        rewards.remove(packageIndex);
                        player.sendMessage("");
                        player.sendMessage("§a✓ §7¡Todas las recompensas de §d" + pkg.getEventDisplayName() + " §7reclamadas!");
                        player.sendMessage("");
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                        
                        if (rewards.isEmpty()) {
                            pendingRewards.remove(uuid);
                        }
                    } else {
                        // Actualizar items restantes
                        pkg.getItems().clear();
                        pkg.getItems().addAll(remainingItems);
                        player.sendMessage("§e⚠ §7Quedan §f" + remainingItems.size() + " §7items por reclamar.");
                    }
                }
            } catch (Exception e) {
                // Ignorar errores de parsing
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════════
    
    private void notifyPlayerRewards(Player player, RewardPackage pkg) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String rank = pkg.getRankAchieved() != null ? pkg.getRankAchieved() : "BRONZE";
            player.sendMessage("");
            player.sendMessage("§8§m═══════════════════════════════════════════");
            player.sendMessage("");
            player.sendMessage("  §a§l✦ §e¡RECOMPENSAS DISPONIBLES! §a§l✦");
            player.sendMessage("");
            player.sendMessage("  §7Evento: §d" + pkg.getEventDisplayName());
            player.sendMessage("  §7Rango: " + getRankColor(rank) + rank);
            player.sendMessage("  §7Items: §f" + pkg.getItems().size() + " §7objetos únicos");
            player.sendMessage("  §7Expira en: " + pkg.getTimeRemainingFormatted());
            player.sendMessage("");
            player.sendMessage("  §eUsa §a/recompensa §epara reclamar");
            player.sendMessage("");
            player.sendMessage("§8§m═══════════════════════════════════════════");
            player.sendMessage("");
            
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.8f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
        }, 40L); // 2 segundos de delay
    }
    
    private ItemStack createBorderItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createPackageDisplayItem(RewardPackage pkg, int index) {
        Material material;
        String rankColor;
        String rank = pkg.getRankAchieved();
        
        // Null-check para evitar NPE en switch
        if (rank == null) {
            rank = "BRONZE";
        }
        
        switch (rank) {
            case "PLATINUM":
                material = Material.NETHER_STAR;
                rankColor = "§b";
                break;
            case "GOLD":
                material = Material.GOLD_INGOT;
                rankColor = "§6";
                break;
            case "SILVER":
                material = Material.IRON_INGOT;
                rankColor = "§7";
                break;
            default:
                material = Material.BRICK;
                rankColor = "§c";
                break;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(rankColor + "§l✦ " + pkg.getEventDisplayName());
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Rango: " + rankColor + rank);
        lore.add("§7Items: §f" + pkg.getItems().size() + " §7objetos");
        if (pkg.getPsAwarded() > 0) {
            lore.add("§7PS: §a+" + pkg.getPsAwarded());
        }
        lore.add("");
        lore.add("§7Tiempo restante: " + pkg.getTimeRemainingFormatted());
        lore.add("");
        lore.add("§e▶ Click para abrir cofre");
        lore.add("");
        lore.add("§8ID: #" + index);
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private String getRankColor(String rank) {
        if (rank == null) {
            return "§c"; // Default para BRONZE
        }
        switch (rank) {
            case "PLATINUM": return "§b";
            case "GOLD": return "§6";
            case "SILVER": return "§7";
            default: return "§c";
        }
    }
    
    private void cleanupExpiredRewards() {
        int cleaned = 0;
        
        for (Map.Entry<UUID, List<RewardPackage>> entry : pendingRewards.entrySet()) {
            List<RewardPackage> rewards = entry.getValue();
            int before = rewards.size();
            rewards.removeIf(RewardPackage::isExpired);
            cleaned += before - rewards.size();
            
            if (rewards.isEmpty()) {
                pendingRewards.remove(entry.getKey());
            }
        }
        
        if (cleaned > 0) {
            plugin.getLogger().info("[RewardClaimSystem] Limpiados " + cleaned + " paquetes de recompensas expirados");
        }
    }
    
    /**
     * Detiene el sistema y cancela tareas
     */
    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        pendingRewards.clear();
        playersWithMenuOpen.clear();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // PROTECCIÓN BLOCK HELPERS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Verifica si un ItemStack es un bloque de protección especial
     * (creado por RewardService para comandos ps give)
     */
    private boolean isProtectionBlockItem(ItemStack item) {
        if (item == null || item.getType() != Material.EMERALD_BLOCK) {
            return false;
        }
        
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return false;
        }
        
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null) return false;
        
        // Buscar el marcador especial
        for (String line : lore) {
            if (line.startsWith("§8§oPS_CMD:")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Extrae el comando PS del lore de un item de bloque de protección
     */
    private String extractProtectionCommand(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return null;
        }
        
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null) return null;
        
        for (String line : lore) {
            if (line.startsWith("§8§oPS_CMD:")) {
                return line.replace("§8§oPS_CMD:", "").trim();
            }
        }
        
        return null;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════
    
    public Map<UUID, List<RewardPackage>> getPendingRewards() {
        return Collections.unmodifiableMap(pendingRewards);
    }
}
