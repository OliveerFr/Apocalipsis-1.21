package me.apocalipsis.skills;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.missions.MissionRank;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * GUI del árbol de habilidades con menú principal.
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
     * Holder para el menú principal de habilidades
     */
    public class MainMenuHolder implements InventoryHolder {
        private Inventory inventory;
        
        @Override
        public Inventory getInventory() { return inventory; }
        public void setInventory(Inventory inv) { this.inventory = inv; }
    }
    
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
    
    /**
     * Abre el menú principal de habilidades
     */
    public void openMainMenu(Player player) {
        MainMenuHolder holder = new MainMenuHolder();
        String title = "§6§l✦ §e§lHabilidades §6§l✦";
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);
        
        renderMainMenu(inv, player);
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.7f, 1.2f);
    }
    
    /**
     * Abre el árbol de una rama específica
     */
    public void openBranchMenu(Player player, SkillBranch branch) {
        TreeMenuHolder holder = new TreeMenuHolder(branch);
        String title = getBranchTitle(branch);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);
        
        renderTreeMenu(inv, player, branch);
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.0f);
    }
    
    /**
     * Abre confirmación de compra
     */
    public void openConfirmMenu(Player player, Skill skill, SkillBranch returnBranch) {
        ConfirmMenuHolder holder = new ConfirmMenuHolder(skill, returnBranch);
        String title = "§8Confirmar: §6" + skill.getDisplayName();
        Inventory inv = Bukkit.createInventory(holder, 27, title);
        holder.setInventory(inv);
        
        renderConfirmMenu(inv, player, skill);
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
    }
    
    // ==================== RENDERIZADO MENÚ PRINCIPAL ====================
    
    private void renderMainMenu(Inventory inv, Player player) {
        UUID uuid = player.getUniqueId();
        
        // Fondo decorativo
        ItemStack blackBg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack grayBg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        
        // Fondo negro
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, blackBg);
        }
        
        // Bordes decorativos grises
        int[] graySlots = {0, 8, 9, 17, 36, 44, 45, 53};
        for (int slot : graySlots) {
            inv.setItem(slot, grayBg);
        }
        
        // === CABEZA DEL JUGADOR CON STATS ===
        ItemStack playerHead = createPlayerHead(player);
        inv.setItem(4, playerHead);
        
        // === TRES RAMAS ===
        
        // Almacenamiento (izquierda)
        inv.setItem(20, createBranchButton(SkillBranch.ALMACENAMIENTO, player));
        
        // Utilidad (centro)
        inv.setItem(22, createBranchButton(SkillBranch.UTILIDAD, player));
        
        // Supervivencia (derecha)
        inv.setItem(24, createBranchButton(SkillBranch.SUPERVIVENCIA, player));
        
        // === INFO EXTRA ===
        
        // Total XP gastada
        int totalXpGastada = skillService.getTotalXpGastada(uuid);
        int totalSkills = skillService.getUnlockedSkills(uuid).size();
        int maxSkills = Skill.values().length;
        
        List<String> statsLore = new ArrayList<>();
        statsLore.add("§7Habilidades: §e" + totalSkills + "§7/§6" + maxSkills);
        statsLore.add("§7XP invertida: §a" + String.format("%,d", totalXpGastada));
        statsLore.add("");
        statsLore.add("§8Selecciona una rama para ver");
        statsLore.add("§8el árbol de habilidades.");
        
        ItemStack statsItem = createItem(Material.BOOK, "§e§lTu Progreso", statsLore);
        inv.setItem(40, statsItem);
        
        // Cerrar
        inv.setItem(49, createItem(Material.BARRIER, "§c§lCerrar", "§7Click para cerrar"));
    }
    
    private ItemStack createPlayerHead(Player player) {
        UUID uuid = player.getUniqueId();
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName("§6§l" + player.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            
            // XP actual
            int xp = plugin.getExperienceService().getXP(player);
            lore.add("§7XP actual: §a" + String.format("%,d", xp));
            
            // Rango
            MissionRank rank = plugin.getRankService().getRank(player);
            lore.add("§7Rango: " + rank.getDisplayName());
            
            // Skills desbloqueadas
            int skills = skillService.getUnlockedSkills(uuid).size();
            lore.add("§7Habilidades: §e" + skills);
            
            lore.add("");
            lore.add("§8Árbol de Habilidades");
            
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        
        return head;
    }
    
    private ItemStack createBranchButton(SkillBranch branch, Player player) {
        UUID uuid = player.getUniqueId();
        Material mat;
        String name;
        String color;
        
        switch (branch) {
            case ALMACENAMIENTO -> {
                mat = Material.CHEST;
                name = "Almacenamiento";
                color = "§6";
            }
            case UTILIDAD -> {
                mat = Material.DIAMOND_PICKAXE;
                name = "Utilidad";
                color = "§b";
            }
            case SUPERVIVENCIA -> {
                mat = Material.TOTEM_OF_UNDYING;
                name = "Supervivencia";
                color = "§c";
            }
            default -> {
                mat = Material.BARRIER;
                name = "???";
                color = "§7";
            }
        }
        
        // Contar skills de esta rama
        List<Skill> branchSkills = Skill.getByBranch(branch);
        int unlocked = 0;
        for (Skill skill : branchSkills) {
            if (skillService.hasSkill(uuid, skill)) {
                unlocked++;
            }
        }
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Progreso: " + color + unlocked + "§7/" + color + branchSkills.size());
        lore.add("");
        
        // Descripción de la rama
        switch (branch) {
            case ALMACENAMIENTO -> {
                lore.add("§7Expande tu inventario y");
                lore.add("§7ender chest. Mejora la");
                lore.add("§7recolección de items.");
            }
            case UTILIDAD -> {
                lore.add("§7Velocidad, minería eficiente,");
                lore.add("§7menos hambre y bonificaciones");
                lore.add("§7de recursos.");
            }
            case SUPERVIVENCIA -> {
                lore.add("§7Más vida, resistencias,");
                lore.add("§7regeneración y habilidades");
                lore.add("§7que te salvan la vida.");
            }
        }
        
        lore.add("");
        lore.add("§e▶ Click para ver árbol");
        
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color + "§l" + name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    // ==================== RENDERIZADO DEL ÁRBOL ====================
    
    private void renderTreeMenu(Inventory inv, Player player, SkillBranch branch) {
        // Fondo
        ItemStack darkBg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, darkBg);
        }
        
        // === FILA 0: PESTAÑAS ===
        ItemStack grayBg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
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
        // Volver al menú principal
        inv.setItem(45, createItem(Material.ARROW, "§e§lVolver", "§7Al menú principal"));
        
        // Leyenda
        inv.setItem(47, createLegendItem());
        
        // Info del jugador
        inv.setItem(49, createPlayerInfo(player));
        
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
        int fromRow = from / 9;
        int fromCol = from % 9;
        int toRow = to / 9;
        int toCol = to % 9;
        
        ItemStack line = createItem(material, " ");
        
        // Línea horizontal
        if (fromRow == toRow) {
            int minCol = Math.min(fromCol, toCol) + 1;
            int maxCol = Math.max(fromCol, toCol);
            for (int col = minCol; col < maxCol; col++) {
                int slot = fromRow * 9 + col;
                if (inv.getItem(slot) == null || 
                    inv.getItem(slot).getType() == Material.BLACK_STAINED_GLASS_PANE) {
                    inv.setItem(slot, line);
                }
            }
        }
        // Línea vertical
        else if (fromCol == toCol) {
            int minRow = Math.min(fromRow, toRow) + 1;
            int maxRow = Math.max(fromRow, toRow);
            for (int row = minRow; row < maxRow; row++) {
                int slot = row * 9 + fromCol;
                if (inv.getItem(slot) == null || 
                    inv.getItem(slot).getType() == Material.BLACK_STAINED_GLASS_PANE) {
                    inv.setItem(slot, line);
                }
            }
        }
        // Línea diagonal/L
        else {
            // Primero horizontal, luego vertical
            int midSlot = fromRow * 9 + toCol;
            if (inv.getItem(midSlot) == null || 
                inv.getItem(midSlot).getType() == Material.BLACK_STAINED_GLASS_PANE) {
                inv.setItem(midSlot, line);
            }
        }
    }
    
    // ==================== POSICIONES DE SKILLS ====================
    
    private Map<Skill, Integer> getSkillPositions(SkillBranch branch) {
        Map<Skill, Integer> positions = new HashMap<>();
        
        switch (branch) {
            case ALMACENAMIENTO -> {
                /*
                 * Layout Almacenamiento:
                 * Tier 1          Tier 2          Tier 3
                 * 
                 * [BOLSILLOS] ─── [SIN_FONDO] ─── [INFINITO]
                 *      └───────── [AUTO_REC]
                 * 
                 * [COFRE_INT] ─── [COFRE_DIM] ─── [VOID]
                 */
                // Tier 1
                positions.put(Skill.BOLSILLOS_PROFUNDOS, 19);
                positions.put(Skill.COFRE_INTERIOR, 37);
                
                // Tier 2
                positions.put(Skill.BOLSILLOS_SIN_FONDO, 21);
                positions.put(Skill.AUTO_RECOLECCION, 28);
                positions.put(Skill.COFRE_DIMENSIONAL, 39);
                
                // Tier 3
                positions.put(Skill.INVENTARIO_INFINITO, 23);
                positions.put(Skill.VOID_STORAGE, 41);
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
                 */
                // Tier 1
                positions.put(Skill.PASO_LIGERO, 19);
                positions.put(Skill.MINERO_EFICIENTE, 28);
                positions.put(Skill.ESTOMAGO_HIERRO, 37);
                
                // Tier 2
                positions.put(Skill.ZANCADAS, 21);
                positions.put(Skill.TOQUE_FORTUNA, 30);
                positions.put(Skill.METABOLISMO_LENTO, 39);
                
                // Tier 3
                positions.put(Skill.VELOCISTA, 23);
                positions.put(Skill.SEDA_NATURAL, 32);
                positions.put(Skill.AUTOSUFICIENTE, 41);
            }
            
            case SUPERVIVENCIA -> {
                /*
                 * Layout Supervivencia (bien ordenado):
                 * 
                 * [PIEL_GR] ─── [TANQUE] ───── [INMORTAL]
                 *      └─────── [REGEN] ────── [FENIX]
                 * 
                 * [CAIDA] ───── [PLUMA] ────── [VUELO_EM]
                 * 
                 * [RES_FUE] ─── [IGNIFUGO]
                 * 
                 * [NADADOR] ─── [BRANQ] ────── [ANFIBIO]
                 */
                // Tier 1 - Columna izquierda
                positions.put(Skill.PIEL_GRUESA, 10);
                positions.put(Skill.CAIDA_SUAVE, 28);
                positions.put(Skill.RESISTENCIA_FUEGO, 37);
                positions.put(Skill.NADADOR, 46);
                
                // Tier 2 - Columna central
                positions.put(Skill.TANQUE, 12);
                positions.put(Skill.REGENERACION_PASIVA, 21);
                positions.put(Skill.PLUMA, 30);
                positions.put(Skill.IGNIFUGO, 39);
                positions.put(Skill.BRANQUIAS, 48);
                
                // Tier 3 - Columna derecha
                positions.put(Skill.INMORTAL, 14);
                positions.put(Skill.FENIX, 23);
                positions.put(Skill.VUELO_EMERGENCIA, 32);
                positions.put(Skill.ANFIBIO, 50);
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
                connections.put(19, List.of(21));  // PASO -> ZANCADAS
                connections.put(21, List.of(23));  // ZANCADAS -> VELOCISTA
                connections.put(28, List.of(30));  // MINERO -> FORTUNA
                connections.put(30, List.of(32));  // FORTUNA -> SEDA
                connections.put(37, List.of(39));  // ESTOMAGO -> METAB
                connections.put(39, List.of(41));  // METAB -> AUTOSUF
            }
            
            case SUPERVIVENCIA -> {
                connections.put(10, Arrays.asList(12, 21)); // PIEL -> TANQUE, REGEN
                connections.put(12, List.of(14));           // TANQUE -> INMORTAL
                connections.put(21, List.of(23));           // REGEN -> FENIX
                connections.put(28, List.of(30));           // CAIDA -> PLUMA
                connections.put(30, List.of(32));           // PLUMA -> VUELO
                connections.put(37, List.of(39));           // RES_FUEGO -> IGNIFUGO
                connections.put(46, List.of(48));           // NADADOR -> BRANQUIAS
                connections.put(48, List.of(50));           // BRANQUIAS -> ANFIBIO
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
        ItemStack grayBg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, grayBg);
        }
        
        // Info de la habilidad en el centro
        inv.setItem(13, createSkillItem(player, skill));
        
        // Preview de compra
        SkillService.PurchasePreview preview = skillService.previewPurchase(player, skill);
        
        // Confirmar (izquierda)
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add("");
        confirmLore.add("§7Costo: §e" + skill.getBaseCost() + " XP");
        confirmLore.add("");
        confirmLore.add("§7XP actual: §a" + String.format("%,d", preview.currentXP));
        confirmLore.add("§7XP después: §" + (preview.newXP < 0 ? "c" : "e") + String.format("%,d", preview.newXP));
        
        if (preview.willDropRank) {
            confirmLore.add("");
            confirmLore.add("§c§l⚠ Perderás tu rango actual!");
            confirmLore.add("§c" + preview.currentRank.getDisplayName() + " §7→ " + preview.newRank.getDisplayName());
        }
        
        confirmLore.add("");
        confirmLore.add("§a▶ Click para comprar");
        
        ItemStack confirmBtn = createItem(Material.LIME_CONCRETE, "§a§l✓ Confirmar", confirmLore);
        inv.setItem(11, confirmBtn);
        
        // Cancelar (derecha)
        ItemStack cancelBtn = createItem(Material.RED_CONCRETE, "§c§l✗ Cancelar", "§7Volver al árbol");
        inv.setItem(15, cancelBtn);
    }
    
    // ==================== CREAR ITEMS ====================
    
    private ItemStack createBranchTab(SkillBranch branch, boolean selected, Player player) {
        Material mat;
        String name;
        String color;
        
        switch (branch) {
            case ALMACENAMIENTO -> {
                mat = selected ? Material.ORANGE_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
                name = "Almacenamiento";
                color = "§6";
            }
            case UTILIDAD -> {
                mat = selected ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
                name = "Utilidad";
                color = "§b";
            }
            case SUPERVIVENCIA -> {
                mat = selected ? Material.RED_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
                name = "Supervivencia";
                color = "§c";
            }
            default -> {
                mat = Material.GRAY_STAINED_GLASS_PANE;
                name = "???";
                color = "§7";
            }
        }
        
        // Contar progreso
        List<Skill> branchSkills = Skill.getByBranch(branch);
        int unlocked = 0;
        for (Skill skill : branchSkills) {
            if (skillService.hasSkill(player, skill)) {
                unlocked++;
            }
        }
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Progreso: " + color + unlocked + "§7/" + color + branchSkills.size());
        if (!selected) {
            lore.add("");
            lore.add("§e▶ Click para ver");
        }
        
        return createItem(mat, color + (selected ? "§l" : "") + name, lore);
    }
    
    private ItemStack createSkillItem(Player player, Skill skill) {
        boolean owned = skillService.hasSkill(player, skill);
        boolean meetsReqs = skillService.meetsRequirements(player, skill);
        boolean isEnabled = owned && skill.isToggleable() && skillService.isSkillEnabled(player, skill);
        
        Material displayMat;
        String prefix;
        
        if (owned) {
            displayMat = skill.getIcon();
            prefix = "§a§l✓ ";
            if (skill.isToggleable()) {
                prefix = isEnabled ? "§a§l⚡ " : "§7§l○ ";
            }
        } else if (meetsReqs) {
            displayMat = skill.getIcon();
            prefix = "§e§l◈ ";
        } else {
            displayMat = Material.COAL_BLOCK;
            prefix = "§8§l✗ ";
        }
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        
        // Descripción
        lore.add("§7" + skill.getDescription());
        lore.add("");
        
        // Tier y Rareza
        lore.add("§8Tier: " + skill.getTier().getDisplayName() + " §8| " + skill.getRarity().getDisplayName());
        
        // Requisitos
        if (skill.getRequirements().length > 0) {
            lore.add("");
            lore.add("§7Requisitos:");
            for (String reqId : skill.getRequirements()) {
                Skill req = Skill.fromId(reqId);
                if (req != null) {
                    boolean hasReq = skillService.hasSkill(player, req);
                    String reqColor = hasReq ? "§a✓ " : "§c✗ ";
                    lore.add("  " + reqColor + req.getDisplayName());
                }
            }
        }
        
        lore.add("");
        
        // Estado y acciones
        if (owned) {
            lore.add("§a§l✓ DESBLOQUEADA");
            if (skill.isToggleable()) {
                String state = isEnabled ? "§aACTIVA" : "§cDESACTIVA";
                lore.add("§7Estado: " + state);
                lore.add("");
                lore.add("§e▶ Click para alternar");
            }
        } else if (meetsReqs) {
            lore.add("§7Costo: §e" + skill.getBaseCost() + " XP");
            lore.add("");
            lore.add("§e▶ Click para comprar");
        } else {
            lore.add("§c§l✗ BLOQUEADA");
            lore.add("§7Desbloquea los requisitos primero.");
        }
        
        ItemStack item = new ItemStack(displayMat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(prefix + skill.getColoredName());
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createPlayerInfo(Player player) {
        int xp = plugin.getExperienceService().getXP(player);
        MissionRank rank = plugin.getRankService().getRank(player);
        int totalSkills = skillService.getUnlockedSkills(player.getUniqueId()).size();
        int totalXpGastada = skillService.getTotalXpGastada(player.getUniqueId());
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7XP disponible: §a" + String.format("%,d", xp));
        lore.add("§7XP gastada: §e" + String.format("%,d", totalXpGastada));
        lore.add("§7Rango: " + rank.getDisplayName());
        lore.add("");
        lore.add("§7Habilidades: §e" + totalSkills + "§7/§6" + Skill.values().length);
        
        return createItem(Material.EXPERIENCE_BOTTLE, "§b§lTu Info", lore);
    }
    
    private ItemStack createLegendItem() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§a§l✓ §aDesbloqueada §7- Ya la tienes");
        lore.add("§e§l◈ §eDisponible §7- Puedes comprar");
        lore.add("§8§l✗ §8Bloqueada §7- Faltan requisitos");
        lore.add("");
        lore.add("§7Líneas de conexión:");
        lore.add("  §a━ §7Ambas desbloqueadas");
        lore.add("  §e━ §7Puedes avanzar");
        lore.add("  §8━ §7Bloqueada");
        
        return createItem(Material.PAINTING, "§d§lLeyenda", lore);
    }
    
    private String getBranchTitle(SkillBranch branch) {
        return switch (branch) {
            case ALMACENAMIENTO -> "§6§l✦ Almacenamiento";
            case UTILIDAD -> "§b§l✦ Utilidad";
            case SUPERVIVENCIA -> "§c§l✦ Supervivencia";
        };
    }
    
    private ItemStack createItem(Material material, String name) {
        return createItem(material, name, (List<String>) null);
    }
    
    private ItemStack createItem(Material material, String name, String lore) {
        return createItem(material, name, lore == null ? null : Collections.singletonList(lore));
    }
    
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    // ==================== EVENTOS ====================
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        Inventory inv = event.getInventory();
        InventoryHolder holder = inv.getHolder();
        
        // Verificar si es uno de nuestros menús
        if (holder instanceof MainMenuHolder) {
            event.setCancelled(true);
            handleMainMenuClick(player, event.getRawSlot());
        } else if (holder instanceof TreeMenuHolder treeHolder) {
            event.setCancelled(true);
            handleTreeClick(player, event.getRawSlot(), treeHolder.getBranch());
        } else if (holder instanceof ConfirmMenuHolder confirmHolder) {
            event.setCancelled(true);
            handleConfirmClick(player, event.getRawSlot(), confirmHolder);
        }
    }
    
    private void handleMainMenuClick(Player player, int slot) {
        switch (slot) {
            case 20 -> openBranchMenu(player, SkillBranch.ALMACENAMIENTO);
            case 22 -> openBranchMenu(player, SkillBranch.UTILIDAD);
            case 24 -> openBranchMenu(player, SkillBranch.SUPERVIVENCIA);
            case 49 -> player.closeInventory();
        }
    }
    
    private void handleTreeClick(Player player, int slot, SkillBranch currentBranch) {
        // === PESTAÑAS ===
        if (slot == 2) {
            if (currentBranch != SkillBranch.ALMACENAMIENTO) {
                openBranchMenu(player, SkillBranch.ALMACENAMIENTO);
            }
            return;
        }
        if (slot == 4) {
            if (currentBranch != SkillBranch.UTILIDAD) {
                openBranchMenu(player, SkillBranch.UTILIDAD);
            }
            return;
        }
        if (slot == 6) {
            if (currentBranch != SkillBranch.SUPERVIVENCIA) {
                openBranchMenu(player, SkillBranch.SUPERVIVENCIA);
            }
            return;
        }
        
        // === VOLVER ===
        if (slot == 45) {
            openMainMenu(player);
            return;
        }
        
        // === CERRAR ===
        if (slot == 53) {
            player.closeInventory();
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
        SkillBranch branch = holder.getReturnBranch();
        
        if (slot == 11) {
            // Confirmar compra
            SkillService.PurchaseResult result = skillService.purchaseSkill(player, skill);
            
            switch (result) {
                case SUCCESS -> {
                    player.sendMessage("§a§l✓ §a¡Has desbloqueado §e" + skill.getDisplayName() + "§a!");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                    
                    // Volver al árbol
                    openBranchMenu(player, branch);
                }
                case NOT_ENOUGH_XP -> {
                    player.sendMessage("§c§l✗ §cNo tienes suficiente XP.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
                }
                case WOULD_DROP_TOO_LOW -> {
                    player.sendMessage("§c§l✗ §cEsto te dejaría con muy poca XP.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
                }
                case ALREADY_OWNED -> {
                    player.sendMessage("§7Ya tienes esta habilidad.");
                    openBranchMenu(player, branch);
                }
                case REQUIREMENTS_NOT_MET -> {
                    player.sendMessage("§c§l✗ §cNo cumples los requisitos.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
                }
                default -> {
                    player.sendMessage("§c§l✗ §cError al comprar.");
                }
            }
        } else if (slot == 15) {
            // Cancelar
            openBranchMenu(player, branch);
        }
    }
}
