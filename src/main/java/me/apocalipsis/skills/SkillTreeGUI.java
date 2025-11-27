package me.apocalipsis.skills;

import me.apocalipsis.Apocalipsis;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI del árbol de habilidades estilo logros de Minecraft.
 * Usa InventoryHolder personalizado para identificar los menús.
 */
public class SkillTreeGUI implements Listener {
    
    private final Apocalipsis plugin;
    private final SkillService skillService;
    
    public SkillTreeGUI(Apocalipsis plugin, SkillService skillService) {
        this.plugin = plugin;
        this.skillService = skillService;
    }
    
    // ==================== INVENTORY HOLDERS ====================
    
    /**
     * Holder para el menú del árbol de habilidades
     */
    public class TreeMenuHolder implements InventoryHolder {
        private final SkillBranch branch;
        private Inventory inventory;
        
        public TreeMenuHolder(SkillBranch branch) {
            this.branch = branch;
        }
        
        public SkillBranch getBranch() { return branch; }
        
        @Override
        public Inventory getInventory() { return inventory; }
        
        public void setInventory(Inventory inv) { this.inventory = inv; }
    }
    
    /**
     * Holder para el menú de confirmación
     */
    public class ConfirmMenuHolder implements InventoryHolder {
        private final Skill skill;
        private final SkillBranch returnBranch;
        private Inventory inventory;
        
        public ConfirmMenuHolder(Skill skill, SkillBranch returnBranch) {
            this.skill = skill;
            this.returnBranch = returnBranch;
        }
        
        public Skill getSkill() { return skill; }
        public SkillBranch getReturnBranch() { return returnBranch; }
        
        @Override
        public Inventory getInventory() { return inventory; }
        
        public void setInventory(Inventory inv) { this.inventory = inv; }
    }
    
    // ==================== ABRIR MENÚS ====================
    
    public void openMainMenu(Player player) {
        openBranchMenu(player, SkillBranch.ALMACENAMIENTO);
    }
    
    public void openBranchMenu(Player player, SkillBranch branch) {
        TreeMenuHolder holder = new TreeMenuHolder(branch);
        String title = getBranchTitle(branch);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);
        
        // Renderizar
        renderTreeMenu(inv, player, branch);
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.0f);
    }
    
    public void openConfirmMenu(Player player, Skill skill, SkillBranch returnBranch) {
        ConfirmMenuHolder holder = new ConfirmMenuHolder(skill, returnBranch);
        String title = "§8Confirmar: §6" + skill.getDisplayName();
        Inventory inv = Bukkit.createInventory(holder, 27, title);
        holder.setInventory(inv);
        
        renderConfirmMenu(inv, player, skill);
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
    }
    
    // ==================== RENDERIZADO DEL ÁRBOL ====================
    
    private void renderTreeMenu(Inventory inv, Player player, SkillBranch branch) {
        // Fondo
        ItemStack darkBg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack grayBg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, darkBg);
        }
        
        // === FILA 0: PESTAÑAS ===
        // Borde izquierdo y derecho
        inv.setItem(0, grayBg);
        inv.setItem(8, grayBg);
        
        // Tab Almacenamiento (slot 2)
        boolean isAlmac = branch == SkillBranch.ALMACENAMIENTO;
        inv.setItem(2, createBranchTab(SkillBranch.ALMACENAMIENTO, isAlmac, player));
        
        // Tab Utilidad (slot 4)
        boolean isUtil = branch == SkillBranch.UTILIDAD;
        inv.setItem(4, createBranchTab(SkillBranch.UTILIDAD, isUtil, player));
        
        // Tab Supervivencia (slot 6)
        boolean isSurv = branch == SkillBranch.SUPERVIVENCIA;
        inv.setItem(6, createBranchTab(SkillBranch.SUPERVIVENCIA, isSurv, player));
        
        // === ÁREA DEL ÁRBOL (filas 1-4) ===
        // Renderizar conexiones primero
        renderConnections(inv, player, branch);
        
        // Renderizar habilidades
        Map<Skill, Integer> positions = getSkillPositions(branch);
        for (Map.Entry<Skill, Integer> entry : positions.entrySet()) {
            inv.setItem(entry.getValue(), createSkillItem(player, entry.getKey()));
        }
        
        // === FILA 5: INFO Y CONTROLES ===
        // Info del jugador
        inv.setItem(45, createPlayerInfo(player));
        
        // Leyenda
        inv.setItem(47, createLegendItem());
        
        // Cerrar
        inv.setItem(53, createItem(Material.BARRIER, "§c§lCerrar", "§7Click para cerrar"));
    }
    
    private void renderConnections(Inventory inv, Player player, SkillBranch branch) {
        Map<Integer, List<Integer>> connections = getConnections(branch);
        
        for (Map.Entry<Integer, List<Integer>> entry : connections.entrySet()) {
            int fromSlot = entry.getKey();
            Skill fromSkill = getSkillAtSlot(fromSlot, branch);
            boolean fromUnlocked = fromSkill != null && skillService.hasSkill(player, fromSkill);
            
            for (int toSlot : entry.getValue()) {
                Skill toSkill = getSkillAtSlot(toSlot, branch);
                boolean toUnlocked = toSkill != null && skillService.hasSkill(player, toSkill);
                
                // Determinar color de conexión
                Material lineMat;
                if (fromUnlocked && toUnlocked) {
                    lineMat = Material.LIME_STAINED_GLASS_PANE;
                } else if (fromUnlocked) {
                    lineMat = Material.YELLOW_STAINED_GLASS_PANE;
                } else {
                    lineMat = Material.GRAY_STAINED_GLASS_PANE;
                }
                
                // Dibujar línea entre slots
                drawLine(inv, fromSlot, toSlot, lineMat);
            }
        }
    }
    
    private void drawLine(Inventory inv, int from, int to, Material material) {
        int fromCol = from % 9;
        int fromRow = from / 9;
        int toCol = to % 9;
        int toRow = to / 9;
        
        // Línea horizontal
        if (fromRow == toRow) {
            int minCol = Math.min(fromCol, toCol);
            int maxCol = Math.max(fromCol, toCol);
            for (int col = minCol + 1; col < maxCol; col++) {
                int slot = fromRow * 9 + col;
                if (inv.getItem(slot) == null || inv.getItem(slot).getType() == Material.BLACK_STAINED_GLASS_PANE) {
                    inv.setItem(slot, createItem(material, "§7→"));
                }
            }
        }
        // Línea vertical (una fila de diferencia)
        else if (fromCol == toCol && Math.abs(toRow - fromRow) == 1) {
            // No hay slot intermedio
        }
        // Diagonal (una fila abajo/arriba y columnas a la derecha)
        else if (Math.abs(toRow - fromRow) == 1) {
            // Poner conexión en el slot intermedio horizontal
            int minCol = Math.min(fromCol, toCol);
            int maxCol = Math.max(fromCol, toCol);
            int row = Math.min(fromRow, toRow);
            for (int col = minCol + 1; col < maxCol; col++) {
                int slot = row * 9 + col;
                if (inv.getItem(slot) == null || inv.getItem(slot).getType() == Material.BLACK_STAINED_GLASS_PANE) {
                    inv.setItem(slot, createItem(material, "§7↘"));
                }
            }
        }
    }
    
    // ==================== POSICIONES DE HABILIDADES ====================
    
    private Map<Skill, Integer> getSkillPositions(SkillBranch branch) {
        Map<Skill, Integer> positions = new HashMap<>();
        
        switch (branch) {
            case ALMACENAMIENTO -> {
                /*
                 * Layout Almacenamiento:
                 * 
                 *    Col: 1     2     3     4     5     6     7
                 * Row 1: [BOL_PROF] ─── [BOL_SIN] ─── [INV_INF]
                 *             │
                 * Row 2:      └──── [AUTO_REC]
                 * 
                 * Row 3: [COFRE_INT] ── [COFRE_DIM] ── [VOID_ST]
                 */
                // Tier 1
                positions.put(Skill.BOLSILLOS_PROFUNDOS, 19);  // Row 2, Col 1
                positions.put(Skill.COFRE_INTERIOR, 37);       // Row 4, Col 1
                
                // Tier 2
                positions.put(Skill.BOLSILLOS_SIN_FONDO, 21);  // Row 2, Col 3
                positions.put(Skill.AUTO_RECOLECCION, 28);     // Row 3, Col 1 (rama alternativa)
                positions.put(Skill.COFRE_DIMENSIONAL, 39);    // Row 4, Col 3
                
                // Tier 3
                positions.put(Skill.INVENTARIO_INFINITO, 23);  // Row 2, Col 5
                positions.put(Skill.VOID_STORAGE, 41);         // Row 4, Col 5
            }
            
            case UTILIDAD -> {
                /*
                 * Layout Utilidad:
                 * 
                 * [PASO_LIG] ─── [ZANCADAS] ─── [VELOCISTA]
                 * 
                 * [MINERO] ───── [FORTUNA] ──── [SEDA_NAT]
                 * 
                 * [ESTOMAGO] ─── [METAB] ────── [AUTOSUF]
                 * 
                 * [CRAFTEO] ─────────────────── [MESA_PORT]
                 */
                // Tier 1
                positions.put(Skill.PASO_LIGERO, 10);         // Row 1, Col 1
                positions.put(Skill.MINERO_EFICIENTE, 19);    // Row 2, Col 1
                positions.put(Skill.ESTOMAGO_HIERRO, 28);     // Row 3, Col 1
                positions.put(Skill.CRAFTEO_RAPIDO, 37);      // Row 4, Col 1
                
                // Tier 2
                positions.put(Skill.ZANCADAS, 12);            // Row 1, Col 3
                positions.put(Skill.TOQUE_FORTUNA, 21);       // Row 2, Col 3
                positions.put(Skill.METABOLISMO_LENTO, 30);   // Row 3, Col 3
                
                // Tier 3
                positions.put(Skill.VELOCISTA, 14);           // Row 1, Col 5
                positions.put(Skill.SEDA_NATURAL, 23);        // Row 2, Col 5
                positions.put(Skill.AUTOSUFICIENTE, 32);      // Row 3, Col 5
                positions.put(Skill.MESA_PORTATIL, 39);       // Row 4, Col 3
            }
            
            case SUPERVIVENCIA -> {
                /*
                 * Layout Supervivencia:
                 * 
                 * [PIEL_GR] ─┬─ [TANQUE] ───── [INMORTAL]
                 *            └─ [REGEN] ────── [FENIX]
                 * 
                 * [CAIDA] ───── [PLUMA] ────── [VUELO_EM]
                 * 
                 * [RES_FUE] ─── [IGNIFUGO]
                 * 
                 * [NADADOR] ─── [BRANQ] ────── [ANFIBIO]
                 */
                // Tier 1
                positions.put(Skill.PIEL_GRUESA, 10);          // Row 1, Col 1
                positions.put(Skill.CAIDA_SUAVE, 19);          // Row 2, Col 1
                positions.put(Skill.RESISTENCIA_FUEGO, 28);    // Row 3, Col 1
                positions.put(Skill.NADADOR, 37);              // Row 4, Col 1
                
                // Tier 2
                positions.put(Skill.TANQUE, 12);               // Row 1, Col 3
                positions.put(Skill.REGENERACION_PASIVA, 21);  // Row 2, Col 3 (desde PIEL_GRUESA también)
                positions.put(Skill.PLUMA, 30);                // Row 3, Col 3 (viene de CAIDA_SUAVE row2)
                positions.put(Skill.IGNIFUGO, 39);             // Row 4, Col 3
                positions.put(Skill.BRANQUIAS, 46);            // Row 5, Col 1
                
                // Tier 3
                positions.put(Skill.INMORTAL, 14);             // Row 1, Col 5
                positions.put(Skill.FENIX, 23);                // Row 2, Col 5
                positions.put(Skill.VUELO_EMERGENCIA, 32);     // Row 3, Col 5
                positions.put(Skill.ANFIBIO, 48);              // Row 5, Col 3
            }
        }
        
        return positions;
    }
    
    private Map<Integer, List<Integer>> getConnections(SkillBranch branch) {
        Map<Integer, List<Integer>> connections = new HashMap<>();
        
        switch (branch) {
            case ALMACENAMIENTO -> {
                connections.put(19, Arrays.asList(21, 28)); // BOLSILLOS -> SIN_FONDO, AUTO_REC
                connections.put(21, List.of(23));           // SIN_FONDO -> INFINITO
                connections.put(37, List.of(39));           // COFRE_INT -> COFRE_DIM
                connections.put(39, List.of(41));           // COFRE_DIM -> VOID
            }
            
            case UTILIDAD -> {
                connections.put(10, List.of(12));  // PASO -> ZANCADAS
                connections.put(12, List.of(14));  // ZANCADAS -> VELOCISTA
                connections.put(19, List.of(21));  // MINERO -> FORTUNA
                connections.put(21, List.of(23));  // FORTUNA -> SEDA
                connections.put(28, List.of(30));  // ESTOMAGO -> METAB
                connections.put(30, List.of(32));  // METAB -> AUTOSUF
                connections.put(37, List.of(39));  // CRAFTEO -> MESA
            }
            
            case SUPERVIVENCIA -> {
                connections.put(10, Arrays.asList(12, 21)); // PIEL -> TANQUE, REGEN
                connections.put(12, List.of(14));           // TANQUE -> INMORTAL
                connections.put(21, List.of(23));           // REGEN -> FENIX
                connections.put(19, List.of(30));           // CAIDA -> PLUMA
                connections.put(30, List.of(32));           // PLUMA -> VUELO
                connections.put(28, List.of(39));           // RES_FUEGO -> IGNIFUGO
                connections.put(37, List.of(46));           // NADADOR -> BRANQUIAS
                connections.put(46, List.of(48));           // BRANQUIAS -> ANFIBIO
            }
        }
        
        return connections;
    }
    
    private Skill getSkillAtSlot(int slot, SkillBranch branch) {
        Map<Skill, Integer> positions = getSkillPositions(branch);
        for (Map.Entry<Skill, Integer> entry : positions.entrySet()) {
            if (entry.getValue() == slot) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    // ==================== RENDERIZADO DE CONFIRMACIÓN ====================
    
    private void renderConfirmMenu(Inventory inv, Player player, Skill skill) {
        // Fondo
        ItemStack bg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, bg);
        }
        
        // Skill info (centro arriba)
        inv.setItem(4, createSkillItem(player, skill));
        
        // Preview de compra
        SkillService.PurchasePreview preview = skillService.previewPurchase(player, skill);
        
        List<String> previewLore = new ArrayList<>();
        previewLore.add("");
        previewLore.add("§7XP Actual: §e" + preview.currentXP);
        previewLore.add("§7Costo: §c-" + preview.cost + " XP");
        previewLore.add("§7XP Después: §a" + preview.newXP);
        previewLore.add("");
        previewLore.add("§7Rango Actual: " + preview.currentRank.getDisplayName());
        if (preview.willDropRank) {
            previewLore.add("§c⚠ Nuevo Rango: " + preview.newRank.getDisplayName());
            previewLore.add("§c   ¡BAJARÁS DE RANGO!");
        } else {
            previewLore.add("§7Nuevo Rango: " + preview.newRank.getDisplayName());
        }
        
        inv.setItem(13, createItem(Material.PAPER, "§e§lResumen de Compra", previewLore.toArray(new String[0])));
        
        // Botón confirmar
        ItemStack confirm = createItem(Material.LIME_CONCRETE, "§a§l✓ CONFIRMAR",
            "",
            "§7Gastarás §e" + preview.cost + " XP",
            "",
            preview.willDropRank ? "§c⚠ ¡Bajarás de rango!" : "§aTu rango no cambiará",
            "",
            "§a▶ Click para comprar"
        );
        inv.setItem(11, confirm);
        
        // Botón cancelar
        ItemStack cancel = createItem(Material.RED_CONCRETE, "§c§l✗ CANCELAR",
            "",
            "§7Volver al árbol",
            "",
            "§c▶ Click para cancelar"
        );
        inv.setItem(15, cancel);
    }
    
    // ==================== CREACIÓN DE ITEMS ====================
    
    private ItemStack createBranchTab(SkillBranch branch, boolean selected, Player player) {
        Material mat;
        String name;
        
        int unlocked = countUnlockedInBranch(player, branch);
        int total = Skill.getByBranch(branch).size();
        
        switch (branch) {
            case ALMACENAMIENTO -> {
                mat = selected ? Material.CHEST : Material.BARREL;
                name = "§6§l📦 ALMACENAMIENTO";
            }
            case UTILIDAD -> {
                mat = selected ? Material.COMPASS : Material.CLOCK;
                name = "§e§l⚡ UTILIDAD";
            }
            case SUPERVIVENCIA -> {
                mat = selected ? Material.SHIELD : Material.IRON_CHESTPLATE;
                name = "§c§l🛡 SUPERVIVENCIA";
            }
            default -> {
                mat = Material.BARRIER;
                name = "§c???";
            }
        }
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Progreso: §b" + unlocked + "§7/§b" + total);
        lore.add("");
        if (selected) {
            lore.add("§a▶ Rama actual");
        } else {
            lore.add("§e▶ Click para ver");
        }
        
        ItemStack item = createItem(mat, name, lore.toArray(new String[0]));
        
        if (selected) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }
        
        return item;
    }
    
    private ItemStack createSkillItem(Player player, Skill skill) {
        boolean owned = skillService.hasSkill(player, skill);
        boolean meetsReqs = skillService.meetsRequirements(player, skill);
        
        Material icon;
        String prefix;
        List<String> lore = new ArrayList<>();
        
        // Info básica
        lore.add("§7" + skill.getDescription());
        lore.add("");
        lore.add(skill.getTier().getDisplayName() + " §8| " + skill.getRarity().getDisplayName());
        lore.add("");
        
        if (owned) {
            // DESBLOQUEADA
            icon = skill.getIcon();
            prefix = "§a✓ ";
            lore.add("§a§l✓ DESBLOQUEADA");
            
            if (skill.isToggleable()) {
                boolean enabled = skillService.isSkillEnabled(player, skill);
                lore.add("");
                lore.add(enabled ? "§aEstado: §l⬤ ON" : "§cEstado: §l○ OFF");
                lore.add("§e▶ Click para " + (enabled ? "desactivar" : "activar"));
            }
        } else if (meetsReqs) {
            // DISPONIBLE PARA COMPRAR
            icon = skill.getIcon();
            prefix = "§e◆ ";
            lore.add("§e§l◆ DISPONIBLE");
            lore.add("");
            lore.add("§7Costo: §e" + skill.getFinalCost() + " XP");
            lore.add("");
            lore.add("§a▶ Click para comprar");
        } else {
            // BLOQUEADA
            icon = Material.COAL_BLOCK;
            prefix = "§8✗ ";
            lore.add("§c§l✗ BLOQUEADA");
            lore.add("");
            lore.add("§7Requisitos:");
            List<Skill> missing = skillService.getMissingRequirements(player, skill);
            for (Skill req : missing) {
                lore.add("§c  ✗ " + req.getDisplayName());
            }
        }
        
        ItemStack item = createItem(icon, prefix + skill.getColoredName(), lore.toArray(new String[0]));
        
        if (owned) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }
        
        return item;
    }
    
    private ItemStack createPlayerInfo(Player player) {
        int xp = plugin.getExperienceService().getXP(player);
        String rank = plugin.getRankService().getRank(player).getDisplayName();
        int skills = skillService.getSkillCount(player);
        int total = skillService.getTotalSkillCount();
        int xpGastada = skillService.getXpGastada(player);
        
        return createItem(Material.PLAYER_HEAD, "§e§l⭐ Tu Progreso",
            "",
            "§7XP Disponible: §a" + xp,
            "§7XP Gastada en Skills: §c" + xpGastada,
            "§7Rango: " + rank,
            "",
            "§7Habilidades: §b" + skills + "§7/§b" + total,
            "",
            "§8Comprar habilidades consume XP",
            "§8y puede bajar tu rango."
        );
    }
    
    private ItemStack createLegendItem() {
        return createItem(Material.BOOK, "§f§lLeyenda",
            "",
            "§a✓ Verde §8= Desbloqueada",
            "§e◆ Amarillo §8= Disponible",
            "§c✗ Rojo §8= Bloqueada",
            "",
            "§aLínea verde §8= Camino completo",
            "§eLínea amarilla §8= Siguiente paso",
            "§7Línea gris §8= Camino cerrado"
        );
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    if (line != null) {
                        loreList.add(line);
                    }
                }
                meta.setLore(loreList);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private int countUnlockedInBranch(Player player, SkillBranch branch) {
        int count = 0;
        for (Skill skill : skillService.getUnlockedSkills(player)) {
            if (skill.getBranch() == branch) count++;
        }
        return count;
    }
    
    private String getBranchTitle(SkillBranch branch) {
        return switch (branch) {
            case ALMACENAMIENTO -> "§8Árbol: §6📦 Almacenamiento";
            case UTILIDAD -> "§8Árbol: §e⚡ Utilidad";
            case SUPERVIVENCIA -> "§8Árbol: §c🛡 Supervivencia";
        };
    }
    
    // ==================== EVENTOS ====================
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        Inventory inv = event.getInventory();
        
        // Verificar si es nuestro menú
        if (inv.getHolder() instanceof TreeMenuHolder holder) {
            event.setCancelled(true);
            handleTreeClick(player, event.getRawSlot(), holder);
        }
        else if (inv.getHolder() instanceof ConfirmMenuHolder holder) {
            event.setCancelled(true);
            handleConfirmClick(player, event.getRawSlot(), holder);
        }
    }
    
    private void handleTreeClick(Player player, int slot, TreeMenuHolder holder) {
        SkillBranch currentBranch = holder.getBranch();
        
        // === PESTAÑAS (fila 0) ===
        if (slot == 2 && currentBranch != SkillBranch.ALMACENAMIENTO) {
            openBranchMenu(player, SkillBranch.ALMACENAMIENTO);
            return;
        }
        if (slot == 4 && currentBranch != SkillBranch.UTILIDAD) {
            openBranchMenu(player, SkillBranch.UTILIDAD);
            return;
        }
        if (slot == 6 && currentBranch != SkillBranch.SUPERVIVENCIA) {
            openBranchMenu(player, SkillBranch.SUPERVIVENCIA);
            return;
        }
        
        // === CERRAR ===
        if (slot == 53) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }
        
        // === HABILIDAD ===
        Skill skill = getSkillAtSlot(slot, currentBranch);
        if (skill != null) {
            handleSkillClick(player, skill, currentBranch);
        }
    }
    
    private void handleSkillClick(Player player, Skill skill, SkillBranch branch) {
        boolean owned = skillService.hasSkill(player, skill);
        boolean meetsReqs = skillService.meetsRequirements(player, skill);
        
        if (owned) {
            // Toggle si es toggleable
            if (skill.isToggleable()) {
                skillService.toggleSkill(player, skill);
                // Refrescar menú para mostrar nuevo estado
                openBranchMenu(player, branch);
            } else {
                player.sendMessage("§7Ya tienes §e" + skill.getDisplayName() + "§7 (pasiva permanente).");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            }
        } else if (meetsReqs) {
            // Abrir menú de confirmación
            openConfirmMenu(player, skill, branch);
        } else {
            // Mostrar requisitos faltantes
            List<Skill> missing = skillService.getMissingRequirements(player, skill);
            player.sendMessage("§c§l✗ §cNecesitas desbloquear primero:");
            for (Skill req : missing) {
                player.sendMessage("§c   • " + req.getDisplayName());
            }
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
        }
    }
    
    private void handleConfirmClick(Player player, int slot, ConfirmMenuHolder holder) {
        Skill skill = holder.getSkill();
        SkillBranch returnBranch = holder.getReturnBranch();
        
        // Confirmar (slot 11)
        if (slot == 11) {
            SkillService.PurchaseResult result = skillService.purchaseSkill(player, skill);
            
            switch (result) {
                case SUCCESS -> {
                    player.sendMessage("§a§l✓ §a¡Desbloqueaste " + skill.getColoredName() + "§a!");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    // Volver al árbol
                    openBranchMenu(player, returnBranch);
                }
                case NOT_ENOUGH_XP -> {
                    player.sendMessage("§c§l✗ §cNo tienes suficiente XP.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
                case WOULD_DROP_TOO_LOW -> {
                    player.sendMessage("§c§l✗ §cNo puedes quedar con menos de 100 XP.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
                case DURING_DISASTER -> {
                    player.sendMessage("§c§l✗ §cNo puedes comprar durante un desastre activo.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
                case ALREADY_OWNED -> {
                    player.sendMessage("§7Ya tienes esta habilidad.");
                    openBranchMenu(player, returnBranch);
                }
                default -> {
                    player.sendMessage("§c§l✗ §cError al comprar la habilidad.");
                }
            }
        }
        // Cancelar (slot 15)
        else if (slot == 15) {
            openBranchMenu(player, returnBranch);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
    }
}
