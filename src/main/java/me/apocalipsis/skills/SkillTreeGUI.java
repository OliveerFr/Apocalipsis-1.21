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
import java.util.Arrays;

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
    
    /**
     * Holder para el menú de mejora de skill
     */
    public class UpgradeMenuHolder implements InventoryHolder {
        private final Skill skill;
        private final SkillBranch returnBranch;
        private Inventory inventory;
        
        public UpgradeMenuHolder(Skill skill, SkillBranch returnBranch) {
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
    
    /**
     * Abre el menú de mejora de skill
     */
    public void openUpgradeMenu(Player player, Skill skill, SkillBranch returnBranch) {
        UpgradeMenuHolder holder = new UpgradeMenuHolder(skill, returnBranch);
        String title = "§8Mejorar: §a" + skill.getDisplayName();
        Inventory inv = Bukkit.createInventory(holder, 27, title);
        holder.setInventory(inv);
        
        renderUpgradeMenu(inv, player, skill);
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.5f);
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
        
        // === SIETE RAMAS EN DOS FILAS ===
        
        // Fila superior: Almacenamiento, Utilidad, Supervivencia, Combate
        inv.setItem(19, createBranchButton(SkillBranch.ALMACENAMIENTO, player));
        inv.setItem(21, createBranchButton(SkillBranch.UTILIDAD, player));
        inv.setItem(23, createBranchButton(SkillBranch.SUPERVIVENCIA, player));
        inv.setItem(25, createBranchButton(SkillBranch.COMBATE, player));
        
        // Fila inferior: Exploración, Invocación, Sinergias
        inv.setItem(29, createBranchButton(SkillBranch.EXPLORACION, player));
        inv.setItem(31, createBranchButton(SkillBranch.INVOCACION, player));
        inv.setItem(33, createBranchButton(SkillBranch.SINERGIAS, player));
        
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
        
        // Definir material y nombre según la rama
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
                mat = Material.SHIELD;
                name = "Supervivencia";
                color = "§c";
            }
            case COMBATE -> {
                mat = Material.NETHERITE_SWORD;
                name = "Combate";
                color = "§4";
            }
            case EXPLORACION -> {
                mat = Material.SPYGLASS;
                name = "Exploración";
                color = "§a";
            }
            case INVOCACION -> {
                mat = Material.BONE;
                name = "Invocación";
                color = "§d";
            }
            case SINERGIAS -> {
                mat = Material.NETHER_STAR;
                name = "Sinergias";
                color = "§5";
            }
            default -> {
                mat = Material.BOOK;
                name = "Desconocido";
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
            case COMBATE -> {
                lore.add("§7Daño aumentado, combos,");
                lore.add("§7técnicas de ataque y");
                lore.add("§7bonificaciones ofensivas.");
            }
            case EXPLORACION -> {
                lore.add("§7Velocidad de viaje, visión");
                lore.add("§7nocturna, y habilidades");
                lore.add("§7para explorar el mundo.");
            }
            case INVOCACION -> {
                lore.add("§7Invoca criaturas aliadas");
                lore.add("§7que te ayudan en combate");
                lore.add("§7y recolección de recursos.");
            }
            case SINERGIAS -> {
                lore.add("§7Combina habilidades de");
                lore.add("§7múltiples ramas para crear");
                lore.add("§7poderosos efectos únicos.");
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
        
        // === FILA 0: PESTAÑAS DE 7 RAMAS ===
        // Tab Almacenamiento (slot 1)
        boolean isAlmac = branch == SkillBranch.ALMACENAMIENTO;
        inv.setItem(1, createBranchTab(SkillBranch.ALMACENAMIENTO, isAlmac, player));
        
        // Tab Utilidad (slot 2)
        boolean isUtil = branch == SkillBranch.UTILIDAD;
        inv.setItem(2, createBranchTab(SkillBranch.UTILIDAD, isUtil, player));
        
        // Tab Supervivencia (slot 3)
        boolean isSurv = branch == SkillBranch.SUPERVIVENCIA;
        inv.setItem(3, createBranchTab(SkillBranch.SUPERVIVENCIA, isSurv, player));
        
        // Tab Combate (slot 4)
        boolean isCombat = branch == SkillBranch.COMBATE;
        inv.setItem(4, createBranchTab(SkillBranch.COMBATE, isCombat, player));
        
        // Tab Exploración (slot 5)
        boolean isExplor = branch == SkillBranch.EXPLORACION;
        inv.setItem(5, createBranchTab(SkillBranch.EXPLORACION, isExplor, player));
        
        // Tab Invocación (slot 6)
        boolean isInvoc = branch == SkillBranch.INVOCACION;
        inv.setItem(6, createBranchTab(SkillBranch.INVOCACION, isInvoc, player));
        
        // Tab Sinergias (slot 7)
        boolean isSin = branch == SkillBranch.SINERGIAS;
        inv.setItem(7, createBranchTab(SkillBranch.SINERGIAS, isSin, player));
        
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
                 * Layout Utilidad (añadido LEÑADOR):
                 * 
                 * [PASO_LIG] ─── [ZANCADAS] ─── [VELOCISTA]
                 * 
                 * [MINERO] ───── [FORTUNA] ──── [SEDA_NAT]
                 * 
                 * [ESTOMAGO] ─── [METAB] ────── [AUTOSUF]
                 * 
                 * [LEÑADOR]
                 */
                // Tier 1
                positions.put(Skill.PASO_LIGERO, 19);
                positions.put(Skill.MINERO_EFICIENTE, 28);
                positions.put(/* Skill.ESTOMAGO_HIERRO */ Skill.PASO_LIGERO, 37);
                positions.put(Skill.LENADOR_NATO, 10); // Nueva habilidad
                
                // Tier 2
                positions.put(Skill.ZANCADAS, 21);
                positions.put(Skill.TOQUE_FORTUNA, 30);
                positions.put(/* Skill.METABOLISMO_LENTO */ Skill.MINERO_EFICIENTE, 39);
                
                // Tier 3
                positions.put(Skill.VELOCISTA, 23);
                positions.put(Skill.SEDA_NATURAL, 32);
                positions.put(/* Skill.AUTOSUFICIENTE */ Skill.LENADOR_NATO, 41);
            }
            
            case SUPERVIVENCIA -> {
                /*
                 * Layout Supervivencia:
                 * Fila 1: Vida (PIEL → TANQUE → INMORTAL) y Regen
                 * Fila 2: Espacio/conexiones + FENIX
                 * Fila 3: Caída (CAIDA → PLUMA → VUELO)
                 * Fila 4: Fuego + Agua
                 */
                // === RAMA DE VIDA Y REGENERACIÓN ===
                positions.put(Skill.PIEL_GRUESA, 10);
                positions.put(Skill.TANQUE, 12);
                positions.put(Skill.REGENERACION_PASIVA, 14);
                positions.put(Skill.INMORTAL, 16);
                positions.put(Skill.FENIX, 25);
                
                // === RAMA DE CAÍDA/VUELO ===
                positions.put(Skill.CAIDA_SUAVE, 28);
                positions.put(Skill.PLUMA, 30);
                positions.put(Skill.VUELO_EMERGENCIA, 32);
                
                // === RAMA DE FUEGO ===
                positions.put(/* Skill.RESISTENCIA_FUEGO */ Skill.PIEL_GRUESA, 37);
                positions.put(/* Skill.IGNIFUGO */ Skill.CAIDA_SUAVE, 39);
                
                // === RAMA DE AGUA ===
                positions.put(Skill.NADADOR, 41);
                positions.put(Skill.BRANQUIAS, 43);
                positions.put(Skill.ANFIBIO, 34);
            }
            
            case COMBATE -> {
                /*
                 * Layout Combate:
                 * 
                 * [GOLPE] ─────── [GUERRERO] ─── [EJECUTOR]
                 *      └───────── [FURIA] ─────── [BERSERKER]
                 *                      └───────── [VAMPIRISMO]
                 * 
                 * [ESCAMAS] ──── [BLOQUEO]
                 * 
                 * [ARQUERO] ──── [FRANCO] ────── [MULTISHOT]
                 */
                // Tier 1 - Rama Ofensiva
                positions.put(Skill.GOLPE_CERTERO, 10);
                positions.put(/* Skill.REFLEJOS */ Skill.GOLPE_CERTERO, 12);
                
                // Tier 1 - Rama Defensiva
                positions.put(/* Skill.PIEL_ESCAMAS */ Skill.CONTRAATAQUE, 28);
                
                // Tier 1 - Rama Arquero
                positions.put(Skill.ARQUERO, 37);
                
                // Tier 2
                positions.put(Skill.GUERRERO, 14);
                positions.put(Skill.FURIA, 21);
                positions.put(/* Skill.BLOQUEO_PERFECTO */ Skill.ARMADURA_VIVIENTE, 30);
                positions.put(Skill.FRANCOTIRADOR, 39);
                
                // Tier 3
                positions.put(Skill.EJECUTOR, 16);
                positions.put(Skill.BERSERKER, 23);
                positions.put(Skill.VAMPIRISMO, 25);
                positions.put(Skill.MULTISHOT, 41);
            }
            
            case EXPLORACION -> {
                /*
                 * Layout Exploración:
                 * 
                 * [VISION] ───── [TELESCOPIO] ── [OJO_AGUILA]
                 * 
                 * [BRUJULA] ──── [MAPA] ──────── [WAYPOINT]
                 * 
                 * [RASTRO] ───── [DETECTOR] ──── [XRAY]
                 * 
                 * [PISADAS] ──── [SOMBRA] ────── [FANTASMA]
                 */
                // Tier 1
                positions.put(Skill.VISION_NOCTURNA, 10);
                positions.put(/* Skill.BRUJULA_INTERNA */ Skill.VISION_NOCTURNA, 19);
                positions.put(/* Skill.RASTRO_ORO */ Skill.ORIENTACION, 28);
                positions.put(/* Skill.PISADAS_SILENCIOSAS */ Skill.DETECTOR_TESOROS, 37);
                
                // Tier 2
                positions.put(/* Skill.TELESCOPIO */ Skill.PASO_FANTASMA, 12);
                positions.put(/* Skill.MAPA_MENTAL */ Skill.VISTA_AGUILA, 21);
                positions.put(/* Skill.DETECTOR_SPAWNERS */ Skill.CARTOGRAFO, 30);
                positions.put(/* Skill.SOMBRA */ Skill.CAZADOR_DUNGEONS, 39);
                
                // Tier 3
                positions.put(Skill.OJO_AGUILA, 14);
                positions.put(Skill.WAYPOINT, 23);
                positions.put(/* Skill.XRAY_DIAMANTES */ Skill.CAMUFLAJE, 32);
                positions.put(Skill.FANTASMA, 41);
            }
            
            case INVOCACION -> {
                /*
                 * Layout Invocación:
                 * 
                 * [LOBO] ──────── [MANADA] ───── [WARDEN]
                 * 
                 * [GATO] ──────── [ABEJAS] ───── [GOLEM]
                 * 
                 * [ALLAY] ─────── [VEX] ──────── (avanzado)
                 */
                // Tier 1
                positions.put(Skill.LOBO_COMPANERO, 10);
                positions.put(/* Skill.GATO_GUARDIAN */ Skill.OJO_AGUILA, 19);
                positions.put(/* Skill.ALLAY_RECOLECTOR -> MAESTRO_RASTREO */ Skill.LOBO_COMPANERO, 28);
                
                // Tier 2
                positions.put(Skill.MANADA_LOBOS, 12);
                positions.put(Skill.ABEJAS_PROTECTORAS, 21);
                positions.put(Skill.VEX_VENGADOR, 30);
                
                // Tier 3
                positions.put(Skill.WARDEN_TEMPORAL, 14);
                positions.put(Skill.GOLEM_PROTECTOR, 23);
            }
            
            case SINERGIAS -> {
                /*
                 * Layout Sinergias:
                 * 
                 * [CAZADOR] ──── [DOMADOR] ──── [AVATAR]
                 * 
                 * [MINERO] ───── [GUERRERO] ─── [OMNIPRESENTE]
                 * 
                 * [EXPLORADOR] ─ [MERCADER] ─── (legendario)
                 */
                // Tier 1
                positions.put(/* Skill.CAZADOR_EXPERTO -> SAQUEO_EXPERTO */ Skill.DOMADOR_BESTIAS, 10);
                positions.put(/* Skill.MINERO_GUERRERO -> TROFEO_HUNTER */ Skill.DOMADOR_BESTIAS, 19);
                positions.put(/* Skill.EXPLORADOR_LIGERO -> SUERTE_NATURAL */ Skill.MERCADER_SUPREMO, 28);
                
                // Tier 2
                positions.put(Skill.DOMADOR_BESTIAS, 12);
                positions.put(Skill.GUERRERO_INMORTAL, 21);
                positions.put(Skill.MERCADER_SUPREMO, 30);
                
                // Tier 3
                positions.put(Skill.AVATAR_CAOS, 14);
                positions.put(/* Skill.OMNIPRESENTE -> DOMINIO_TOTAL */ Skill.GUERRERO_INMORTAL, 23);
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
                // Rama de vida
                connections.put(10, List.of(12));           // PIEL -> TANQUE
                connections.put(12, Arrays.asList(14, 16)); // TANQUE -> REGEN, INMORTAL
                connections.put(14, List.of(25));           // REGEN -> FENIX
                
                // Rama de caída
                connections.put(28, List.of(30));           // CAIDA -> PLUMA
                connections.put(30, List.of(32));           // PLUMA -> VUELO
                
                // Rama de fuego
                connections.put(37, List.of(39));           // RES_FUEGO -> IGNIFUGO
                
                // Rama de agua
                connections.put(41, List.of(43));           // NADADOR -> BRANQUIAS
                connections.put(43, List.of(34));           // BRANQUIAS -> ANFIBIO
            }
            
            case COMBATE -> {
                // Rama ofensiva principal
                connections.put(10, Arrays.asList(14, 21)); // GOLPE -> GUERRERO, FURIA
                connections.put(14, List.of(16));           // GUERRERO -> EJECUTOR
                connections.put(21, Arrays.asList(23, 25)); // FURIA -> BERSERKER, VAMPIRISMO
                connections.put(16, List.of(25));           // EJECUTOR -> VAMPIRISMO
                
                // Rama defensiva
                connections.put(28, List.of(30));           // ESCAMAS -> BLOQUEO
                
                // Rama arquero
                connections.put(37, List.of(39));           // ARQUERO -> FRANCO
                connections.put(39, List.of(41));           // FRANCO -> MULTISHOT
            }
            
            case EXPLORACION -> {
                connections.put(10, List.of(12));  // VISION -> TELESCOPIO
                connections.put(12, List.of(14));  // TELESCOPIO -> OJO_AGUILA
                connections.put(19, List.of(21));  // BRUJULA -> MAPA
                connections.put(21, List.of(23));  // MAPA -> WAYPOINT
                connections.put(28, List.of(30));  // RASTRO -> DETECTOR
                connections.put(30, List.of(32));  // DETECTOR -> XRAY
                connections.put(37, List.of(39));  // PISADAS -> SOMBRA
                connections.put(39, List.of(41));  // SOMBRA -> FANTASMA
            }
            
            case INVOCACION -> {
                connections.put(10, List.of(12));  // LOBO -> MANADA
                connections.put(12, List.of(14));  // MANADA -> WARDEN
                connections.put(19, List.of(21));  // GATO -> ABEJAS
                connections.put(21, List.of(23));  // ABEJAS -> GOLEM
                connections.put(28, List.of(30));  // ALLAY -> VEX
            }
            
            case SINERGIAS -> {
                connections.put(10, List.of(12));  // CAZADOR -> DOMADOR
                connections.put(12, List.of(14));  // DOMADOR -> AVATAR
                connections.put(19, List.of(21));  // MINERO -> GUERRERO
                connections.put(21, List.of(23));  // GUERRERO -> OMNIPRESENTE
                connections.put(28, List.of(30));  // EXPLORADOR -> MERCADER
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
    
    // ==================== RENDERIZADO DE MEJORA ====================
    
    private void renderUpgradeMenu(Inventory inv, Player player, Skill skill) {
        // Fondo
        ItemStack grayBg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, grayBg);
        }
        
        // Obtener preview de mejora
        UpgradePreview preview = skillService.getUpgradePreview(player, skill);
        SkillLevel currentLevel = skillService.getSkillLevel(player, skill);
        
        // Info de la habilidad en el centro con nivel actual
        ItemStack skillItem = createSkillItemWithLevel(player, skill);
        inv.setItem(13, skillItem);
        
        // Si ya está en nivel máximo
        if (currentLevel.isMax()) {
            ItemStack maxItem = createItem(Material.NETHER_STAR, "§6§l✦ NIVEL MÁXIMO", 
                Arrays.asList("", "§7Esta habilidad ya está al", "§7máximo nivel posible.", "", 
                "§5Bonus: §d" + (SkillConfig.getLevel3Bonus(skill) != null ? SkillConfig.getLevel3Bonus(skill) : "Efecto potenciado")));
            inv.setItem(11, maxItem);
            
            ItemStack backBtn = createItem(Material.ARROW, "§e§lVolver", "§7Al árbol de habilidades");
            inv.setItem(15, backBtn);
            return;
        }
        
        // Botón de mejorar (izquierda)
        List<String> upgradeLore = new ArrayList<>();
        upgradeLore.add("");
        upgradeLore.add("§7Nivel actual: " + currentLevel.getColor() + currentLevel.getRoman() + 
                        " §7→ " + preview.getNextLevel().getColor() + preview.getNextLevel().getRoman());
        upgradeLore.add("");
        upgradeLore.add("§7Efecto actual: §f" + formatEffect(preview.getCurrentEffect()));
        upgradeLore.add("§7Nuevo efecto: §a" + formatEffect(preview.getNextEffect()) + 
                        " §7(+" + formatEffect(preview.getEffectIncrease()) + ")");
        
        if (preview.isNextLevelMax() && preview.hasLevel3Bonus()) {
            upgradeLore.add("");
            upgradeLore.add("§5§l✦ BONUS NIVEL 3:");
            upgradeLore.add("§d  " + preview.getLevel3Bonus());
        }
        
        upgradeLore.add("");
        upgradeLore.add("§7Costo: §e" + preview.getCost() + " XP");
        upgradeLore.add("§7Tu XP: §" + (preview.canAfford() ? "a" : "c") + preview.getPlayerXP());
        
        if (preview.canAfford()) {
            upgradeLore.add("");
            upgradeLore.add("§a▶ Click para mejorar");
            inv.setItem(11, createItem(Material.LIME_CONCRETE, "§a§l⬆ Mejorar", upgradeLore));
        } else {
            upgradeLore.add("");
            upgradeLore.add("§c✗ Necesitas " + preview.getXPNeeded() + " XP más");
            inv.setItem(11, createItem(Material.GRAY_CONCRETE, "§7§l⬆ Mejorar", upgradeLore));
        }
        
        // Cancelar (derecha)
        ItemStack cancelBtn = createItem(Material.RED_CONCRETE, "§c§l✗ Cancelar", "§7Volver al árbol");
        inv.setItem(15, cancelBtn);
    }
    
    private String formatEffect(double value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        }
        return String.format("%.1f", value);
    }
    
    private ItemStack createSkillItemWithLevel(Player player, Skill skill) {
        SkillLevel level = skillService.getSkillLevel(player, skill);
        double effect = skillService.getLevelEffect(player, skill);
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7" + skill.getDescription());
        lore.add("");
        lore.add("§7Nivel: " + level.getColor() + "§l" + level.getRoman() + 
                 " §8[" + getLevelProgressBar(level) + "§8]");
        lore.add("§7Efecto actual: §a" + formatEffect(effect));
        lore.add("");
        lore.add("§8Tier: " + skill.getTier().getDisplayName() + " §8| " + skill.getRarity().getDisplayName());
        
        if (level.isMax() && SkillConfig.getLevel3Bonus(skill) != null) {
            lore.add("");
            lore.add("§5§l✦ BONUS ACTIVO:");
            lore.add("§d  " + SkillConfig.getLevel3Bonus(skill));
        }
        
        ItemStack item = new ItemStack(skill.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(level.getColor() + "§l" + skill.getDisplayName() + " " + level.getRoman());
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private String getLevelProgressBar(SkillLevel level) {
        StringBuilder bar = new StringBuilder();
        for (int i = 1; i <= 3; i++) {
            if (i <= level.getLevel()) {
                bar.append("§a█");
            } else {
                bar.append("§7░");
            }
        }
        return bar.toString();
    }
    
    // ==================== CREAR ITEMS ====================
    
    private ItemStack createBranchTab(SkillBranch branch, boolean selected, Player player) {
        Material mat = Material.GRAY_STAINED_GLASS_PANE;
        String name = "???";
        String color = "§7";
        
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
            case COMBATE -> {
                mat = selected ? Material.RED_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
                name = "Combate";
                color = "§4";
            }
            case EXPLORACION -> {
                mat = selected ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
                name = "Exploración";
                color = "§a";
            }
            case INVOCACION -> {
                mat = selected ? Material.PINK_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
                name = "Invocación";
                color = "§d";
            }
            case SINERGIAS -> {
                mat = selected ? Material.PURPLE_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
                name = "Sinergias";
                color = "§5";
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
        boolean skillEnabled = skill.isEnabled(); // Verificar si la skill está activa en el juego
        
        Material displayMat;
        String prefix;
        
        if (!skillEnabled) {
            // Skill deshabilitada - usar material gris y marcarla claramente
            displayMat = Material.BARRIER;
            prefix = "§8§l⛔ ";
        } else if (owned) {
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
        if (!skillEnabled) {
            // Skill deshabilitada
            lore.add("§8§l⛔ DESHABILITADA");
            lore.add("§7Esta habilidad está temporalmente");
            lore.add("§7desactivada por los desarrolladores.");
            lore.add("");
            lore.add("§c✗ No se puede comprar");
        } else if (owned) {
            SkillLevel level = skillService.getSkillLevel(player, skill);
            String levelStars = "§6" + "★".repeat(level.getLevel()) + "§8" + "☆".repeat(3 - level.getLevel());
            lore.add("§a§l✓ DESBLOQUEADA " + levelStars);
            
            // Mostrar efecto actual si aplica
            if (SkillConfig.hasLevelEffects(skill.getId())) {
                double currentEffect = skillService.getLevelEffect(player, skill);
                lore.add("§7Efecto actual: §e" + String.format("%.1f", currentEffect));
            }
            
            if (skill.isToggleable()) {
                String state = isEnabled ? "§aACTIVA" : "§cDESACTIVA";
                lore.add("§7Estado: " + state);
                lore.add("");
                lore.add("§e▶ Click para alternar");
            }
            
            // Opción de mejorar si no está al máximo
            if (level.getLevel() < 3) {
                lore.add("");
                lore.add("§6▶ Shift+Click para mejorar");
            } else {
                lore.add("");
                lore.add("§6★ Nivel máximo");
            }
        } else if (meetsReqs) {
            int currentXP = plugin.getExperienceService().getXP(player);
            int cost = skill.getBaseCost();
            boolean canAfford = currentXP >= cost;
            
            // [DEBUG] Log para troubleshooting multimundo
            plugin.getLogger().info("[SkillGUI DEBUG] Player: " + player.getName() + 
                " | World: " + player.getWorld().getName() + 
                " | XP: " + currentXP + 
                " | Cost: " + cost + 
                " | CanAfford: " + canAfford);
            
            lore.add("§7Costo: " + (canAfford ? "§a" : "§c") + cost + " XP");
            lore.add("§7Tienes: " + (canAfford ? "§a" : "§c") + currentXP + " XP");
            lore.add("");
            if (canAfford) {
                lore.add("§a✓ Puedes comprar esta habilidad");
                lore.add("§e▶ Click para comprar");
            } else {
                lore.add("§c✗ No tienes suficiente XP");
                lore.add("§7Necesitas " + (cost - currentXP) + " XP más");
            }
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
            case COMBATE -> "§4§l✦ Combate";
            case EXPLORACION -> "§a§l✦ Exploración";
            case INVOCACION -> "§d§l✦ Invocación";
            case SINERGIAS -> "§5§l✦ Sinergias";
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
            handleTreeClick(player, event.getRawSlot(), treeHolder.getBranch(), event.isShiftClick());
        } else if (holder instanceof ConfirmMenuHolder confirmHolder) {
            event.setCancelled(true);
            handleConfirmClick(player, event.getRawSlot(), confirmHolder);
        } else if (holder instanceof UpgradeMenuHolder upgradeHolder) {
            event.setCancelled(true);
            handleUpgradeClick(player, event.getRawSlot(), upgradeHolder);
        }
    }
    
    private void handleMainMenuClick(Player player, int slot) {
        switch (slot) {
            // Fila superior: slots 19, 21, 23, 25
            case 19 -> openBranchMenu(player, SkillBranch.ALMACENAMIENTO);
            case 21 -> openBranchMenu(player, SkillBranch.UTILIDAD);
            case 23 -> openBranchMenu(player, SkillBranch.SUPERVIVENCIA);
            case 25 -> openBranchMenu(player, SkillBranch.COMBATE);
            // Fila inferior: slots 29, 31, 33
            case 29 -> openBranchMenu(player, SkillBranch.EXPLORACION);
            case 31 -> openBranchMenu(player, SkillBranch.INVOCACION);
            case 33 -> openBranchMenu(player, SkillBranch.SINERGIAS);
            // Cerrar
            case 49 -> player.closeInventory();
        }
    }
    
    private void handleTreeClick(Player player, int slot, SkillBranch currentBranch, boolean isShiftClick) {
        // === PESTAÑAS (slots 1-7) ===
        if (slot == 1) {
            if (currentBranch != SkillBranch.ALMACENAMIENTO) {
                openBranchMenu(player, SkillBranch.ALMACENAMIENTO);
            }
            return;
        }
        if (slot == 2) {
            if (currentBranch != SkillBranch.UTILIDAD) {
                openBranchMenu(player, SkillBranch.UTILIDAD);
            }
            return;
        }
        if (slot == 3) {
            if (currentBranch != SkillBranch.SUPERVIVENCIA) {
                openBranchMenu(player, SkillBranch.SUPERVIVENCIA);
            }
            return;
        }
        if (slot == 4) {
            if (currentBranch != SkillBranch.COMBATE) {
                openBranchMenu(player, SkillBranch.COMBATE);
            }
            return;
        }
        if (slot == 5) {
            if (currentBranch != SkillBranch.EXPLORACION) {
                openBranchMenu(player, SkillBranch.EXPLORACION);
            }
            return;
        }
        if (slot == 6) {
            if (currentBranch != SkillBranch.INVOCACION) {
                openBranchMenu(player, SkillBranch.INVOCACION);
            }
            return;
        }
        if (slot == 7) {
            if (currentBranch != SkillBranch.SINERGIAS) {
                openBranchMenu(player, SkillBranch.SINERGIAS);
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
            handleSkillClick(player, skill, currentBranch, isShiftClick);
        }
    }
    
    private void handleSkillClick(Player player, Skill skill, SkillBranch branch, boolean isShiftClick) {
        boolean owned = skillService.hasSkill(player, skill);
        boolean meetsReqs = skillService.meetsRequirements(player, skill);
        
        if (owned) {
            SkillLevel currentLevel = skillService.getSkillLevel(player, skill);
            
            // Shift+Click siempre abre menú de mejora (si hay niveles disponibles)
            if (isShiftClick) {
                if (!currentLevel.isMax()) {
                    openUpgradeMenu(player, skill, branch);
                } else {
                    player.sendMessage("§6§l✦ §e" + skill.getDisplayName() + " §festá al máximo nivel.");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.5f);
                }
                return;
            }
            
            // Click normal: toggle para toggleables, upgrade para no-toggleables
            if (skill.isToggleable()) {
                skillService.toggleSkill(player, skill);
                openBranchMenu(player, branch);
            } else {
                // Abrir menú de mejora si no está en nivel máximo
                if (!currentLevel.isMax()) {
                    openUpgradeMenu(player, skill, branch);
                } else {
                    player.sendMessage("§6§l✦ §e" + skill.getDisplayName() + " §festá al máximo nivel.");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.5f);
                }
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
                case MISSING_REQUIREMENTS -> {
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
    
    /**
     * Maneja los clicks en el menú de mejora de habilidad
     */
    private void handleUpgradeClick(Player player, int slot, UpgradeMenuHolder holder) {
        Skill skill = holder.getSkill();
        SkillBranch branch = holder.getReturnBranch();
        
        if (slot == 11) {
            // Confirmar mejora
            SkillLevel currentLevel = skillService.getSkillLevel(player, skill);
            
            if (currentLevel.getLevel() >= 3) {
                player.sendMessage("§7Esta habilidad ya está al máximo nivel.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 0.7f, 1.0f);
                return;
            }
            
            int cost = skillService.getUpgradeCost(player, skill);
            int playerXP = plugin.getExperienceService().getXP(player);
            
            if (playerXP < cost) {
                player.sendMessage("§c§l✗ §cNo tienes suficiente XP. Necesitas §e" + cost + " XP§c.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
                return;
            }
            
            // Realizar la mejora
            if (skillService.upgradeSkill(player, skill)) {
                SkillLevel newLevel = skillService.getSkillLevel(player, skill);
                player.sendMessage("§a§l✓ §a¡Has mejorado §e" + skill.getDisplayName() + "§a al nivel §6" + newLevel.getLevel() + "§a!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                
                // Reabrir menú para mostrar nuevo nivel
                if (newLevel.getLevel() < 3) {
                    openUpgradeMenu(player, skill, branch);
                } else {
                    player.sendMessage("§6§l★ §e¡Nivel máximo alcanzado!");
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                    openBranchMenu(player, branch);
                }
            } else {
                player.sendMessage("§c§l✗ §cNo se pudo mejorar la habilidad.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            }
            
        } else if (slot == 15) {
            // Cancelar - volver al árbol
            openBranchMenu(player, branch);
        }
    }
}
