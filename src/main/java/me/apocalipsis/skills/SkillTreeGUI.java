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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI del árbol de habilidades estilo logros de Minecraft.
 * Vista de mapa con conexiones entre habilidades.
 */
public class SkillTreeGUI implements Listener {
    
    private final Apocalipsis plugin;
    private final SkillService skillService;
    
    // Estado de cada jugador
    private final Map<UUID, SkillBranch> playerBranch = new HashMap<>();
    private final Map<UUID, Boolean> inConfirmMenu = new HashMap<>();
    private final Map<UUID, Skill> playerConfirmSkill = new HashMap<>();
    private final Set<UUID> openMenus = new HashSet<>();
    
    private static final String TITLE_PREFIX = "§0";
    
    public SkillTreeGUI(Apocalipsis plugin, SkillService skillService) {
        this.plugin = plugin;
        this.skillService = skillService;
    }
    
    // ==================== ABRIR MENÚ PRINCIPAL ====================
    
    public void openMainMenu(Player player) {
        openBranchMenu(player, SkillBranch.ALMACENAMIENTO);
    }
    
    // ==================== MENÚ DE RAMA (ESTILO LOGROS) ====================
    
    public void openBranchMenu(Player player, SkillBranch branch) {
        // Inventario 6x9 = 54 slots
        String title = TITLE_PREFIX + getBranchTitle(branch);
        Inventory inv = Bukkit.createInventory(null, 54, title);
        
        UUID uuid = player.getUniqueId();
        playerBranch.put(uuid, branch);
        inConfirmMenu.put(uuid, false);
        openMenus.add(uuid);
        
        // Fondo oscuro (como el fondo de los logros)
        fillBackground(inv);
        
        // Pestañas de ramas (fila superior)
        renderBranchTabs(inv, branch);
        
        // Renderizar el árbol de habilidades según la rama
        renderSkillTree(inv, player, branch);
        
        // Info del jugador (esquina inferior izquierda)
        renderPlayerInfo(inv, player);
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.0f);
    }
    
    // ==================== RENDERIZADO ====================
    
    private void fillBackground(Inventory inv) {
        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, bg);
        }
    }
    
    private void renderBranchTabs(Inventory inv, SkillBranch currentBranch) {
        // Fila 0: tabs de las 3 ramas (slots 1, 4, 7)
        
        // Tab Almacenamiento
        boolean isAlmac = currentBranch == SkillBranch.ALMACENAMIENTO;
        ItemStack almacTab = createTab(
            isAlmac ? Material.CHEST : Material.GRAY_SHULKER_BOX,
            "§6§l📦 ALMACENAMIENTO",
            isAlmac ? "§a▶ Rama actual" : "§7Click para ver",
            isAlmac
        );
        inv.setItem(1, almacTab);
        
        // Tab Utilidad
        boolean isUtil = currentBranch == SkillBranch.UTILIDAD;
        ItemStack utilTab = createTab(
            isUtil ? Material.COMPASS : Material.GRAY_DYE,
            "§e§l⚡ UTILIDAD",
            isUtil ? "§a▶ Rama actual" : "§7Click para ver",
            isUtil
        );
        inv.setItem(4, utilTab);
        
        // Tab Supervivencia
        boolean isSurv = currentBranch == SkillBranch.SUPERVIVENCIA;
        ItemStack survTab = createTab(
            isSurv ? Material.SHIELD : Material.GRAY_BANNER,
            "§c§l🛡 SUPERVIVENCIA",
            isSurv ? "§a▶ Rama actual" : "§7Click para ver",
            isSurv
        );
        inv.setItem(7, survTab);
    }
    
    private void renderSkillTree(Inventory inv, Player player, SkillBranch branch) {
        Map<Skill, Integer> skillPositions = getSkillPositions(branch);
        Map<Integer, List<Integer>> connections = getConnections(branch);
        
        // Primero dibujar las conexiones
        for (Map.Entry<Integer, List<Integer>> entry : connections.entrySet()) {
            int fromSlot = entry.getKey();
            for (int toSlot : entry.getValue()) {
                drawConnection(inv, player, fromSlot, toSlot, branch);
            }
        }
        
        // Luego dibujar las habilidades encima
        for (Map.Entry<Skill, Integer> entry : skillPositions.entrySet()) {
            Skill skill = entry.getKey();
            int slot = entry.getValue();
            ItemStack skillItem = createSkillItem(player, skill);
            inv.setItem(slot, skillItem);
        }
    }
    
    private void drawConnection(Inventory inv, Player player, int fromSlot, int toSlot, SkillBranch branch) {
        Skill fromSkill = getSkillAtSlot(fromSlot, branch);
        Skill toSkill = getSkillAtSlot(toSlot, branch);
        
        boolean fromUnlocked = fromSkill != null && skillService.hasSkill(player, fromSkill);
        boolean toUnlocked = toSkill != null && skillService.hasSkill(player, toSkill);
        
        Material lineMaterial;
        if (fromUnlocked && toUnlocked) {
            lineMaterial = Material.LIME_STAINED_GLASS_PANE; // Completada
        } else if (fromUnlocked) {
            lineMaterial = Material.YELLOW_STAINED_GLASS_PANE; // Disponible
        } else {
            lineMaterial = Material.GRAY_STAINED_GLASS_PANE; // Bloqueada
        }
        
        int fromCol = fromSlot % 9;
        int fromRow = fromSlot / 9;
        int toCol = toSlot % 9;
        int toRow = toSlot / 9;
        
        // Conexión horizontal
        if (fromRow == toRow && Math.abs(toCol - fromCol) == 2) {
            int midSlot = fromSlot + 1;
            inv.setItem(midSlot, createItem(lineMaterial, " "));
        }
        // Conexión vertical
        else if (fromCol == toCol && Math.abs(toRow - fromRow) == 1) {
            // No hay slot intermedio, pero podemos dejarlo así
        }
        // Conexión diagonal o especial
        else if (Math.abs(toCol - fromCol) <= 2 && Math.abs(toRow - fromRow) <= 1) {
            // Dibujar línea intermedia si hay espacio
            if (toCol > fromCol) {
                int midSlot = fromSlot + 1;
                if (inv.getItem(midSlot) != null && inv.getItem(midSlot).getType() == Material.BLACK_STAINED_GLASS_PANE) {
                    inv.setItem(midSlot, createItem(lineMaterial, " "));
                }
            }
        }
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
    
    /**
     * Define las posiciones de cada habilidad en el inventario.
     * Layout visual estilo árbol de logros.
     * 
     * Grid 6x9:
     * Fila 0: Pestañas (slots 0-8)
     * Filas 1-5: Árbol (slots 9-53)
     * 
     * Tier 1: Columnas 1-2
     * Tier 2: Columnas 4-5
     * Tier 3: Columnas 7-8
     */
    private Map<Skill, Integer> getSkillPositions(SkillBranch branch) {
        Map<Skill, Integer> positions = new HashMap<>();
        
        switch (branch) {
            case ALMACENAMIENTO -> {
                // Tier 1 (izquierda)
                positions.put(Skill.BOLSILLOS_PROFUNDOS, 19); // Fila 2, col 1
                positions.put(Skill.COFRE_INTERIOR, 37);       // Fila 4, col 1
                
                // Tier 2 (centro)
                positions.put(Skill.BOLSILLOS_SIN_FONDO, 21); // Fila 2, col 3
                positions.put(Skill.AUTO_RECOLECCION, 30);     // Fila 3, col 3
                positions.put(Skill.COFRE_DIMENSIONAL, 39);   // Fila 4, col 3
                
                // Tier 3 (derecha)
                positions.put(Skill.INVENTARIO_INFINITO, 23); // Fila 2, col 5
                positions.put(Skill.VOID_STORAGE, 41);         // Fila 4, col 5
            }
            
            case UTILIDAD -> {
                // Tier 1
                positions.put(Skill.PASO_LIGERO, 10);        // Fila 1, col 1
                positions.put(Skill.MINERO_EFICIENTE, 19);   // Fila 2, col 1
                positions.put(Skill.ESTOMAGO_HIERRO, 28);    // Fila 3, col 1
                positions.put(Skill.CRAFTEO_RAPIDO, 37);     // Fila 4, col 1
                
                // Tier 2
                positions.put(Skill.ZANCADAS, 12);            // Fila 1, col 3
                positions.put(Skill.TOQUE_FORTUNA, 21);       // Fila 2, col 3
                positions.put(Skill.METABOLISMO_LENTO, 30);   // Fila 3, col 3
                
                // Tier 3
                positions.put(Skill.VELOCISTA, 14);           // Fila 1, col 5
                positions.put(Skill.SEDA_NATURAL, 23);        // Fila 2, col 5
                positions.put(Skill.AUTOSUFICIENTE, 32);      // Fila 3, col 5
                positions.put(Skill.MESA_PORTATIL, 39);       // Fila 4, col 3
            }
            
            case SUPERVIVENCIA -> {
                // Tier 1 (columna 1)
                positions.put(Skill.PIEL_GRUESA, 10);         // Fila 1, col 1
                positions.put(Skill.CAIDA_SUAVE, 19);         // Fila 2, col 1
                positions.put(Skill.RESISTENCIA_FUEGO, 28);   // Fila 3, col 1
                positions.put(Skill.NADADOR, 37);             // Fila 4, col 1
                
                // Tier 2 (columnas 3-4)
                positions.put(Skill.TANQUE, 12);               // Fila 1, col 3 (de PIEL_GRUESA)
                positions.put(Skill.REGENERACION_PASIVA, 13);  // Fila 1, col 4 (también de PIEL_GRUESA)
                positions.put(Skill.PLUMA, 21);                // Fila 2, col 3 (de CAIDA_SUAVE)
                positions.put(Skill.IGNIFUGO, 30);             // Fila 3, col 3 (de RESISTENCIA_FUEGO)
                positions.put(Skill.BRANQUIAS, 39);            // Fila 4, col 3 (de NADADOR)
                
                // Tier 3 (columnas 5-6)
                positions.put(Skill.INMORTAL, 14);             // Fila 1, col 5 (de TANQUE)
                positions.put(Skill.FENIX, 15);                // Fila 1, col 6 (de REGENERACION_PASIVA)
                positions.put(Skill.VUELO_EMERGENCIA, 23);     // Fila 2, col 5 (de PLUMA)
                positions.put(Skill.ANFIBIO, 41);              // Fila 4, col 5 (de BRANQUIAS)
            }
        }
        
        return positions;
    }
    
    /**
     * Define las conexiones entre habilidades.
     */
    private Map<Integer, List<Integer>> getConnections(SkillBranch branch) {
        Map<Integer, List<Integer>> connections = new HashMap<>();
        
        switch (branch) {
            case ALMACENAMIENTO -> {
                // BOLSILLOS_PROFUNDOS -> BOLSILLOS_SIN_FONDO
                connections.put(19, Arrays.asList(21, 30));
                // BOLSILLOS_SIN_FONDO -> INVENTARIO_INFINITO
                connections.put(21, Arrays.asList(23));
                // COFRE_INTERIOR -> COFRE_DIMENSIONAL
                connections.put(37, Arrays.asList(39));
                // COFRE_DIMENSIONAL -> VOID_STORAGE
                connections.put(39, Arrays.asList(41));
            }
            
            case UTILIDAD -> {
                // PASO_LIGERO -> ZANCADAS
                connections.put(10, Arrays.asList(12));
                // ZANCADAS -> VELOCISTA
                connections.put(12, Arrays.asList(14));
                // MINERO_EFICIENTE -> TOQUE_FORTUNA
                connections.put(19, Arrays.asList(21));
                // TOQUE_FORTUNA -> SEDA_NATURAL
                connections.put(21, Arrays.asList(23));
                // ESTOMAGO_HIERRO -> METABOLISMO_LENTO
                connections.put(28, Arrays.asList(30));
                // METABOLISMO_LENTO -> AUTOSUFICIENTE
                connections.put(30, Arrays.asList(32));
                // CRAFTEO_RAPIDO -> MESA_PORTATIL
                connections.put(37, Arrays.asList(39));
            }
            
            case SUPERVIVENCIA -> {
                // PIEL_GRUESA -> TANQUE, REGENERACION_PASIVA
                connections.put(10, Arrays.asList(12, 13));
                // TANQUE -> INMORTAL
                connections.put(12, Arrays.asList(14));
                // REGENERACION_PASIVA -> FENIX
                connections.put(13, Arrays.asList(15));
                // CAIDA_SUAVE -> PLUMA
                connections.put(19, Arrays.asList(21));
                // PLUMA -> VUELO_EMERGENCIA
                connections.put(21, Arrays.asList(23));
                // RESISTENCIA_FUEGO -> IGNIFUGO
                connections.put(28, Arrays.asList(30));
                // NADADOR -> BRANQUIAS
                connections.put(37, Arrays.asList(39));
                // BRANQUIAS -> ANFIBIO
                connections.put(39, Arrays.asList(41));
            }
        }
        
        return connections;
    }
    
    private void renderPlayerInfo(Inventory inv, Player player) {
        int xp = plugin.getExperienceService().getXP(player);
        String rank = plugin.getRankService().getRank(player).getDisplayName();
        int skills = skillService.getSkillCount(player);
        int total = skillService.getTotalSkillCount();
        
        ItemStack infoItem = createItem(Material.PLAYER_HEAD, "§e§l⭐ Tu Progreso",
            "",
            "§7XP Disponible: §a" + xp,
            "§7Rango: " + rank,
            "§7Habilidades: §b" + skills + "§7/§b" + total,
            "",
            "§8Comprar habilidades consume XP",
            "§8y puede bajar tu rango."
        );
        inv.setItem(45, infoItem);
        
        // Cerrar
        ItemStack closeItem = createItem(Material.BARRIER, "§c§lCerrar", "§7Cerrar el menú");
        inv.setItem(53, closeItem);
    }
    
    // ==================== MENÚ DE CONFIRMACIÓN ====================
    
    public void openConfirmMenu(Player player, Skill skill) {
        UUID uuid = player.getUniqueId();
        playerConfirmSkill.put(uuid, skill);
        inConfirmMenu.put(uuid, true);
        
        String title = TITLE_PREFIX + "§6Confirmar: " + skill.getDisplayName();
        Inventory inv = Bukkit.createInventory(null, 27, title);
        
        // Fondo
        ItemStack bg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, bg);
        }
        
        // Skill en el centro
        ItemStack skillItem = createSkillItem(player, skill);
        inv.setItem(4, skillItem);
        
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
            previewLore.add("§c⚠ Nuevo Rango: " + preview.newRank.getDisplayName() + " §c(BAJADA)");
        } else {
            previewLore.add("§7Nuevo Rango: " + preview.newRank.getDisplayName() + " §a(sin cambio)");
        }
        
        ItemStack previewItem = createItem(Material.PAPER, "§e§lResumen de Compra", 
            previewLore.toArray(new String[0]));
        inv.setItem(13, previewItem);
        
        // Botón confirmar
        ItemStack confirmBtn = createItem(Material.LIME_WOOL, "§a§l✓ CONFIRMAR COMPRA",
            "",
            "§7Gastarás §e" + preview.cost + " XP",
            preview.willDropRank ? "§c⚠ ¡Bajarás de rango!" : ""
        );
        inv.setItem(11, confirmBtn);
        
        // Botón cancelar
        ItemStack cancelBtn = createItem(Material.RED_WOOL, "§c§l✗ CANCELAR",
            "",
            "§7Volver al árbol"
        );
        inv.setItem(15, cancelBtn);
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
    }
    
    // ==================== EVENTOS ====================
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        UUID uuid = player.getUniqueId();
        if (!openMenus.contains(uuid)) return;
        
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;
        
        // ¿Está en menú de confirmación?
        Boolean isConfirm = inConfirmMenu.get(uuid);
        if (isConfirm != null && isConfirm) {
            handleConfirmClick(player, slot, clicked);
            return;
        }
        
        // Menú de árbol
        handleTreeClick(player, slot, clicked);
    }
    
    private void handleTreeClick(Player player, int slot, ItemStack clicked) {
        UUID uuid = player.getUniqueId();
        SkillBranch currentBranch = playerBranch.get(uuid);
        
        // Pestañas de rama
        if (slot == 1) {
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
        if (slot == 7) {
            if (currentBranch != SkillBranch.SUPERVIVENCIA) {
                openBranchMenu(player, SkillBranch.SUPERVIVENCIA);
            }
            return;
        }
        
        // Cerrar
        if (slot == 53 && clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }
        
        // ¿Es una habilidad?
        Skill skill = getSkillAtSlot(slot, currentBranch);
        if (skill != null) {
            handleSkillClick(player, skill);
        }
    }
    
    private void handleSkillClick(Player player, Skill skill) {
        boolean owned = skillService.hasSkill(player, skill);
        boolean meetsReqs = skillService.meetsRequirements(player, skill);
        
        if (owned) {
            // Toggle si es toggleable
            if (skill.isToggleable()) {
                skillService.toggleSkill(player, skill);
                // Refrescar
                openBranchMenu(player, playerBranch.get(player.getUniqueId()));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
            } else {
                player.sendMessage("§7Ya tienes esta habilidad.");
            }
        } else if (meetsReqs) {
            // Abrir confirmación de compra
            openConfirmMenu(player, skill);
        } else {
            // Mostrar qué falta
            List<Skill> missing = skillService.getMissingRequirements(player, skill);
            player.sendMessage("§c§l✗ §cNecesitas desbloquear primero:");
            for (Skill req : missing) {
                player.sendMessage("§c  • " + req.getDisplayName());
            }
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
        }
    }
    
    private void handleConfirmClick(Player player, int slot, ItemStack clicked) {
        UUID uuid = player.getUniqueId();
        Skill skill = playerConfirmSkill.get(uuid);
        SkillBranch branch = playerBranch.get(uuid);
        
        if (slot == 11 && clicked.getType() == Material.LIME_WOOL) {
            // Confirmar
            if (skill != null) {
                SkillService.PurchaseResult result = skillService.purchaseSkill(player, skill);
                
                switch (result) {
                    case SUCCESS -> {
                        player.sendMessage("§a§l✓ §a¡Desbloqueaste " + skill.getColoredName() + "§a!");
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                        // Volver al árbol
                        inConfirmMenu.put(uuid, false);
                        openBranchMenu(player, branch);
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
                    default -> {
                        player.sendMessage("§c§l✗ §cNo se pudo completar la compra.");
                    }
                }
            }
        } else if (slot == 15 && clicked.getType() == Material.RED_WOOL) {
            // Cancelar
            inConfirmMenu.put(uuid, false);
            openBranchMenu(player, branch);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        
        openMenus.remove(uuid);
        playerBranch.remove(uuid);
        inConfirmMenu.remove(uuid);
        playerConfirmSkill.remove(uuid);
    }
    
    // ==================== UTILIDADES ====================
    
    private String getBranchTitle(SkillBranch branch) {
        return switch (branch) {
            case ALMACENAMIENTO -> "§6📦 Árbol: Almacenamiento";
            case UTILIDAD -> "§e⚡ Árbol: Utilidad";
            case SUPERVIVENCIA -> "§c🛡 Árbol: Supervivencia";
        };
    }
    
    private ItemStack createSkillItem(Player player, Skill skill) {
        boolean owned = skillService.hasSkill(player, skill);
        boolean meetsReqs = skillService.meetsRequirements(player, skill);
        
        Material icon;
        String prefix;
        List<String> lore = new ArrayList<>();
        
        // Descripción siempre primero
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
                lore.add(enabled ? "§aEstado: §lON" : "§cEstado: §lOFF");
                lore.add("§e▶ Click para cambiar");
            }
        } else if (meetsReqs) {
            // DISPONIBLE
            icon = skill.getIcon();
            prefix = "§e◆ ";
            lore.add("§e§l◆ DISPONIBLE");
            lore.add("");
            lore.add("§7Costo: §e" + skill.getFinalCost() + " XP");
            lore.add("§a▶ Click para comprar");
        } else {
            // BLOQUEADA
            icon = Material.COAL_BLOCK;
            prefix = "§8✗ ";
            lore.add("§c§l✗ BLOQUEADA");
            lore.add("");
            lore.add("§7Requisitos:");
            for (Skill req : skillService.getMissingRequirements(player, skill)) {
                lore.add("§c  ✗ " + req.getDisplayName());
            }
        }
        
        ItemStack item = createItem(icon, prefix + skill.getColoredName(), lore.toArray(new String[0]));
        
        // Añadir efecto de encantamiento si está desbloqueada
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
    
    private ItemStack createTab(Material material, String name, String status, boolean selected) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(status);
        
        ItemStack item = createItem(material, name, lore.toArray(new String[0]));
        
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
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    if (line != null && !line.isEmpty()) {
                        loreList.add(line);
                    }
                }
                if (!loreList.isEmpty()) {
                    meta.setLore(loreList);
                }
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
}
