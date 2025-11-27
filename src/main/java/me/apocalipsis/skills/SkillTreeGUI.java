package me.apocalipsis.skills;

import me.apocalipsis.Apocalipsis;
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
 * GUI principal del árbol de habilidades.
 * Muestra las 3 ramas y estadísticas del jugador.
 */
public class SkillTreeGUI implements Listener {
    
    private final Apocalipsis plugin;
    private final SkillService skillService;
    
    // Set de inventarios abiertos para identificar nuestras GUIs
    private final Set<UUID> openMainMenu = new HashSet<>();
    private final Set<UUID> openBranchMenu = new HashSet<>();
    private final Set<UUID> openConfirmMenu = new HashSet<>();
    private final Map<UUID, SkillBranch> playerBranch = new HashMap<>();
    private final Map<UUID, Skill> playerConfirmSkill = new HashMap<>();
    
    private static final String MAIN_TITLE = "§6§l🌳 ÁRBOL DE HABILIDADES";
    
    public SkillTreeGUI(Apocalipsis plugin, SkillService skillService) {
        this.plugin = plugin;
        this.skillService = skillService;
    }
    
    // ==================== MENÚ PRINCIPAL ====================
    
    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MAIN_TITLE);
        
        // Información del jugador (slot 4, arriba centro)
        int playerXP = plugin.getExperienceService().getXP(player);
        String rank = plugin.getRankService().getRank(player).getDisplayName();
        int skillCount = skillService.getSkillCount(player);
        int totalSkills = skillService.getTotalSkillCount();
        
        ItemStack infoItem = createItem(Material.PLAYER_HEAD, "§e§lTu Progreso",
            "§7XP Disponible: §a" + playerXP,
            "§7Rango: " + rank,
            "§7Habilidades: §b" + skillCount + "§7/§b" + totalSkills,
            "",
            "§8Las habilidades cuestan XP.",
            "§8Si gastas mucha XP, bajarás de rango."
        );
        inv.setItem(4, infoItem);
        
        // Rama Almacenamiento (slot 11)
        int almacCount = countUnlockedInBranch(player, SkillBranch.ALMACENAMIENTO);
        int almacTotal = Skill.getByBranch(SkillBranch.ALMACENAMIENTO).size();
        ItemStack almacItem = createItem(Material.CHEST, "§6§l📦 ALMACENAMIENTO",
            "§7Mejora tu inventario y almacenamiento.",
            "",
            "§eDesbloqueadas: §b" + almacCount + "§7/§b" + almacTotal,
            "",
            "§a▶ Click para ver rama"
        );
        inv.setItem(11, almacItem);
        
        // Rama Utilidad (slot 13)
        int utilCount = countUnlockedInBranch(player, SkillBranch.UTILIDAD);
        int utilTotal = Skill.getByBranch(SkillBranch.UTILIDAD).size();
        ItemStack utilItem = createItem(Material.COMPASS, "§e§l⚡ UTILIDAD",
            "§7Velocidad, minería y crafteo.",
            "",
            "§eDesbloqueadas: §b" + utilCount + "§7/§b" + utilTotal,
            "",
            "§a▶ Click para ver rama"
        );
        inv.setItem(13, utilItem);
        
        // Rama Supervivencia (slot 15)
        int survCount = countUnlockedInBranch(player, SkillBranch.SUPERVIVENCIA);
        int survTotal = Skill.getByBranch(SkillBranch.SUPERVIVENCIA).size();
        ItemStack survItem = createItem(Material.SHIELD, "§c§l🛡 SUPERVIVENCIA",
            "§7Vida extra y resistencias.",
            "",
            "§eDesbloqueadas: §b" + survCount + "§7/§b" + survTotal,
            "",
            "§a▶ Click para ver rama"
        );
        inv.setItem(15, survItem);
        
        // Mis Habilidades (slot 22)
        ItemStack mySkillsItem = createItem(Material.BOOK, "§b§l📊 Mis Habilidades",
            "§7Ver todas tus habilidades",
            "§7desbloqueadas y sus estados.",
            "",
            "§a▶ Click para ver"
        );
        inv.setItem(22, mySkillsItem);
        
        // Decoración
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }
        
        player.openInventory(inv);
        openMainMenu.add(player.getUniqueId());
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
    }
    
    // ==================== MENÚ DE RAMA ====================
    
    public void openBranchMenu(Player player, SkillBranch branch) {
        List<Skill> skills = Skill.getByBranch(branch);
        int rows = Math.max(3, (int) Math.ceil(skills.size() / 7.0) + 2);
        Inventory inv = Bukkit.createInventory(null, rows * 9, branch.getDisplayName());
        
        // Info de la rama (slot 4)
        int unlocked = countUnlockedInBranch(player, branch);
        ItemStack branchInfo = createItem(branch.getIcon(), branch.getDisplayName(),
            "§7" + getBranchDescription(branch),
            "",
            "§eDesbloqueadas: §b" + unlocked + "§7/§b" + skills.size()
        );
        inv.setItem(4, branchInfo);
        
        // Organizar por tiers
        int slot = 9; // Empezar en segunda fila
        
        for (SkillTier tier : SkillTier.values()) {
            List<Skill> tierSkills = Skill.getByBranchAndTier(branch, tier);
            if (tierSkills.isEmpty()) continue;
            
            // Label del tier
            ItemStack tierLabel = createItem(Material.PAPER, tier.getDisplayName(),
                "§7Habilidades de " + tier.getDisplayName()
            );
            inv.setItem(slot, tierLabel);
            slot++;
            
            for (Skill skill : tierSkills) {
                if (slot % 9 == 8) slot++; // Saltar última columna
                if (slot >= inv.getSize() - 9) break;
                
                ItemStack skillItem = createSkillItem(player, skill);
                inv.setItem(slot, skillItem);
                slot++;
            }
            
            // Nueva fila para siguiente tier
            slot = ((slot / 9) + 1) * 9;
        }
        
        // Botón volver (última fila, centro)
        ItemStack backItem = createItem(Material.ARROW, "§c§l← Volver",
            "§7Volver al menú principal"
        );
        inv.setItem(inv.getSize() - 5, backItem);
        
        // Decoración
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }
        
        player.openInventory(inv);
        openBranchMenu.add(player.getUniqueId());
        playerBranch.put(player.getUniqueId(), branch);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }
    
    // ==================== MENÚ DE CONFIRMACIÓN ====================
    
    public void openConfirmMenu(Player player, Skill skill) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6Confirmar Compra");
        
        // Skill info (centro arriba)
        ItemStack skillItem = createSkillItem(player, skill);
        inv.setItem(4, skillItem);
        
        // Preview de la compra
        SkillService.PurchasePreview preview = skillService.previewPurchase(player, skill);
        
        List<String> previewLore = new ArrayList<>();
        previewLore.add("§7XP Actual: §e" + preview.currentXP);
        previewLore.add("§7Costo: §c-" + preview.cost + " XP");
        previewLore.add("§7XP Después: §a" + preview.newXP);
        previewLore.add("");
        previewLore.add("§7Rango Actual: " + preview.currentRank.getDisplayName());
        if (preview.willDropRank) {
            previewLore.add("§c⚠ Rango Después: " + preview.newRank.getDisplayName() + " §c(BAJADA)");
        } else {
            previewLore.add("§7Rango Después: " + preview.newRank.getDisplayName());
        }
        
        ItemStack previewItem = createItem(Material.PAPER, "§e§lResumen de Compra", 
            previewLore.toArray(new String[0]));
        inv.setItem(13, previewItem);
        
        // Botón confirmar (verde)
        ItemStack confirmItem = createItem(Material.LIME_WOOL, "§a§l✓ CONFIRMAR",
            "§7Click para comprar la habilidad.",
            "",
            preview.willDropRank ? "§c⚠ ¡Bajarás de rango!" : "§aTu rango no cambiará."
        );
        inv.setItem(11, confirmItem);
        
        // Botón cancelar (rojo)
        ItemStack cancelItem = createItem(Material.RED_WOOL, "§c§l✗ CANCELAR",
            "§7Volver sin comprar."
        );
        inv.setItem(15, cancelItem);
        
        // Decoración
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }
        
        player.openInventory(inv);
        openConfirmMenu.add(player.getUniqueId());
        playerConfirmSkill.put(player.getUniqueId(), skill);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
    }
    
    // ==================== EVENTOS ====================
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        UUID uuid = player.getUniqueId();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // Menú principal
        if (openMainMenu.contains(uuid)) {
            event.setCancelled(true);
            handleMainMenuClick(player, event.getSlot(), clicked);
            return;
        }
        
        // Menú de rama
        if (openBranchMenu.contains(uuid)) {
            event.setCancelled(true);
            handleBranchMenuClick(player, clicked);
            return;
        }
        
        // Menú de confirmación
        if (openConfirmMenu.contains(uuid)) {
            event.setCancelled(true);
            handleConfirmMenuClick(player, event.getSlot(), clicked);
            return;
        }
    }
    
    private void handleMainMenuClick(Player player, int slot, ItemStack clicked) {
        switch (slot) {
            case 11: // Almacenamiento
                openBranchMenu(player, SkillBranch.ALMACENAMIENTO);
                break;
            case 13: // Utilidad
                openBranchMenu(player, SkillBranch.UTILIDAD);
                break;
            case 15: // Supervivencia
                openBranchMenu(player, SkillBranch.SUPERVIVENCIA);
                break;
            case 22: // Mis habilidades
                showMySkills(player);
                break;
        }
    }
    
    private void handleBranchMenuClick(Player player, ItemStack clicked) {
        UUID uuid = player.getUniqueId();
        
        // Botón volver
        if (clicked.getType() == Material.ARROW) {
            openMainMenu(player);
            return;
        }
        
        // Verificar si es una habilidad clickeable
        if (clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
            String displayName = clicked.getItemMeta().getDisplayName();
            
            // Buscar la skill por nombre
            for (Skill skill : Skill.values()) {
                if (displayName.contains(skill.getDisplayName())) {
                    // Verificar si se puede comprar
                    if (!skillService.hasSkill(player, skill) && 
                        skillService.meetsRequirements(player, skill)) {
                        openConfirmMenu(player, skill);
                        return;
                    } else if (skill.isToggleable() && skillService.hasSkill(player, skill)) {
                        // Toggle
                        skillService.toggleSkill(player, skill);
                        openBranchMenu(player, playerBranch.get(uuid));
                        return;
                    }
                    break;
                }
            }
        }
    }
    
    private void handleConfirmMenuClick(Player player, int slot, ItemStack clicked) {
        UUID uuid = player.getUniqueId();
        Skill skill = playerConfirmSkill.get(uuid);
        
        if (skill == null) {
            player.closeInventory();
            return;
        }
        
        if (slot == 11 && clicked.getType() == Material.LIME_WOOL) {
            // Confirmar compra
            SkillService.PurchaseResult result = skillService.purchaseSkill(player, skill);
            
            switch (result) {
                case SUCCESS:
                    player.sendMessage("§a§l✓ §a¡Has desbloqueado " + skill.getColoredName() + "§a!");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    player.closeInventory();
                    break;
                case NOT_ENOUGH_XP:
                    player.sendMessage("§c§l✗ §cNo tienes suficiente XP.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    break;
                case WOULD_DROP_TOO_LOW:
                    player.sendMessage("§c§l✗ §cNo puedes quedar con menos de 100 XP.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    break;
                case DURING_DISASTER:
                    player.sendMessage("§c§l✗ §cNo puedes comprar durante un desastre.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    break;
                default:
                    player.sendMessage("§c§l✗ §cError al comprar la habilidad.");
                    break;
            }
        } else if (slot == 15 && clicked.getType() == Material.RED_WOOL) {
            // Cancelar
            SkillBranch branch = playerBranch.get(uuid);
            if (branch != null) {
                openBranchMenu(player, branch);
            } else {
                openMainMenu(player);
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        
        openMainMenu.remove(uuid);
        openBranchMenu.remove(uuid);
        openConfirmMenu.remove(uuid);
        playerBranch.remove(uuid);
        playerConfirmSkill.remove(uuid);
    }
    
    // ==================== UTILIDADES ====================
    
    private void showMySkills(Player player) {
        Set<Skill> skills = skillService.getUnlockedSkills(player);
        
        if (skills.isEmpty()) {
            player.sendMessage("§c§l✗ §cNo tienes habilidades desbloqueadas.");
            return;
        }
        
        player.sendMessage("§6§l═══════ §eTUS HABILIDADES §6§l═══════");
        
        for (Skill skill : skills) {
            String toggle = "";
            if (skill.isToggleable()) {
                boolean enabled = skillService.isSkillEnabled(player, skill);
                toggle = enabled ? " §a[ON]" : " §c[OFF]";
            }
            player.sendMessage("§7• " + skill.getColoredName() + toggle);
            player.sendMessage("  §8" + skill.getDescription());
        }
        
        player.sendMessage("§6§l════════════════════════════");
        player.sendMessage("§7XP gastada total: §e" + skillService.getXpGastada(player));
    }
    
    private int countUnlockedInBranch(Player player, SkillBranch branch) {
        int count = 0;
        for (Skill skill : skillService.getUnlockedSkills(player)) {
            if (skill.getBranch() == branch) count++;
        }
        return count;
    }
    
    private String getBranchDescription(SkillBranch branch) {
        return switch (branch) {
            case ALMACENAMIENTO -> "Más espacio para tus items.";
            case UTILIDAD -> "Velocidad, minería y comodidad.";
            case SUPERVIVENCIA -> "Vida, resistencias y defensa.";
        };
    }
    
    private ItemStack createSkillItem(Player player, Skill skill) {
        UUID uuid = player.getUniqueId();
        boolean owned = skillService.hasSkill(uuid, skill);
        boolean meetsReqs = skillService.meetsRequirements(player, skill);
        
        Material icon;
        List<String> lore = new ArrayList<>();
        String prefix;
        
        if (owned) {
            icon = skill.getIcon();
            prefix = "§a✓ ";
            lore.add("§a§lDESBLOQUEADA");
            
            if (skill.isToggleable()) {
                boolean enabled = skillService.isSkillEnabled(uuid, skill);
                lore.add(enabled ? "§aEstado: ON" : "§cEstado: OFF");
                lore.add("");
                lore.add("§e▶ Click para toggle");
            }
        } else if (meetsReqs) {
            icon = skill.getIcon();
            prefix = "§e⬡ ";
            lore.add("§e§lDISPONIBLE");
            lore.add("");
            lore.add("§7Costo: §e" + skill.getBaseCost() + " XP");
            lore.add("");
            lore.add("§a▶ Click para comprar");
        } else {
            icon = Material.BARRIER;
            prefix = "§c✗ ";
            lore.add("§c§lBLOQUEADA");
            lore.add("");
            lore.add("§7Requisitos:");
            for (Skill req : skillService.getMissingRequirements(player, skill)) {
                lore.add("§c  • " + req.getDisplayName());
            }
        }
        
        // Info común
        lore.add(0, "§7" + skill.getDescription());
        lore.add(1, "");
        lore.add(2, skill.getTier().getDisplayName() + " §8| " + skill.getRarity().getDisplayName());
        lore.add(3, "");
        
        return createItem(icon, prefix + skill.getColoredName(), lore.toArray(new String[0]));
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
