package me.apocalipsis.skills;

import me.apocalipsis.Apocalipsis;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Sistema de mochila virtual para habilidades de almacenamiento.
 * Soporta múltiples mochilas (hasta 10 por jugador).
 */
public class BackpackService implements Listener {
    
    private final Apocalipsis plugin;
    private final SkillService skillService;
    private final File dataFile;
    
    // UUID -> número de mochila (1-10) -> contenido de la mochila
    private final Map<UUID, Map<Integer, ItemStack[]>> backpacks = new HashMap<>();
    
    public BackpackService(Apocalipsis plugin, SkillService skillService) {
        this.plugin = plugin;
        this.skillService = skillService;
        this.dataFile = new File(plugin.getDataFolder(), "backpacks.yml");
        loadBackpacks();
    }
    
    // ==================== INVENTORY HOLDER ====================
    
    public class BackpackHolder implements InventoryHolder {
        private final UUID owner;
        private final int backpackNumber;
        private Inventory inventory;
        
        public BackpackHolder(UUID owner) {
            this(owner, 1);
        }
        
        public BackpackHolder(UUID owner, int backpackNumber) {
            this.owner = owner;
            this.backpackNumber = backpackNumber;
        }
        
        public UUID getOwner() { return owner; }
        public int getBackpackNumber() { return backpackNumber; }
        
        @Override
        public Inventory getInventory() { return inventory; }
        public void setInventory(Inventory inv) { this.inventory = inv; }
    }
    
    // ==================== MOCHILA ====================
    
    /**
     * Abre la mochila del jugador (mochila 1 por defecto)
     */
    public void openBackpack(Player player) {
        openBackpack(player, 1);
    }
    
    /**
     * Abre la mochila del jugador (con número específico)
     */
    public void openBackpack(Player player, int backpackNumber) {
        UUID uuid = player.getUniqueId();
        
        // Validar número de mochila
        if (backpackNumber < 1 || backpackNumber > 2) {
            player.sendMessage("§c✗ Número de mochila inválido. Usa 1 o 2.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            return;
        }
        
        // Verificar habilidad
        int size = getBackpackSize(uuid);
        if (size == 0) {
            player.sendMessage("§c✗ No tienes desbloqueada la habilidad de mochila.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            return;
        }
        
        BackpackHolder holder = new BackpackHolder(uuid, backpackNumber);
        String title = getBackpackTitle(uuid, backpackNumber);
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);
        
        // Cargar contenido (preservando todo el contenido previo)
        ItemStack[] contents = getBackpackContents(uuid, backpackNumber);
        if (contents != null) {
            // Si el tamaño guardado es menor que el actual, expandir
            if (contents.length < size) {
                plugin.getLogger().info("[Backpack] Expandiendo mochila #" + backpackNumber + " de " + player.getName() + 
                    " de " + contents.length + " a " + size + " slots");
                ItemStack[] expanded = new ItemStack[size];
                System.arraycopy(contents, 0, expanded, 0, contents.length);
                setBackpackContents(uuid, backpackNumber, expanded);
                contents = expanded;
            }
            
            // Cargar todo el contenido disponible
            for (int i = 0; i < Math.min(contents.length, size); i++) {
                if (contents[i] != null) {
                    inv.setItem(i, contents[i]);
                }
            }
        }
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.2f);
        
        // Mensaje si es mochila 2+
        if (backpackNumber > 1) {
            player.sendMessage("§e§l✦ §6Mochila #" + backpackNumber + " abierta");
        }
    }
    
    /**
     * Obtiene el tamaño de la mochila según las habilidades y niveles del jugador
     */
    public int getBackpackSize(UUID uuid) {
        // INVENTARIO_INFINITO siempre da 54 slots (no tiene niveles)
        if (skillService.hasSkill(uuid, Skill.INVENTARIO_INFINITO)) {
            return 54;
        }
        
        // BOLSILLOS_SIN_FONDO: 27/36/45 slots según nivel
        if (skillService.hasSkill(uuid, Skill.BOLSILLOS_SIN_FONDO)) {
            double effect = skillService.getLevelEffect(uuid, Skill.BOLSILLOS_SIN_FONDO);
            return (int) effect; // 27, 36, o 45
        }
        
        // BOLSILLOS_PROFUNDOS: 9/18/27 slots según nivel
        if (skillService.hasSkill(uuid, Skill.BOLSILLOS_PROFUNDOS)) {
            double effect = skillService.getLevelEffect(uuid, Skill.BOLSILLOS_PROFUNDOS);
            return (int) effect; // 9, 18, o 27
        }
        
        return 0; // Sin mochila
    }
    
    /**
     * Obtiene el título de la mochila según el nivel
     */
    private String getBackpackTitle(UUID uuid, int backpackNumber) {
        String suffix = backpackNumber > 1 ? " #" + backpackNumber : "";
        
        if (skillService.hasSkill(uuid, Skill.INVENTARIO_INFINITO)) {
            return "§6§l✦ §eInventario Infinito" + suffix + " §6§l✦";
        } else if (skillService.hasSkill(uuid, Skill.BOLSILLOS_SIN_FONDO)) {
            SkillLevel level = skillService.getSkillLevel(uuid, Skill.BOLSILLOS_SIN_FONDO);
            return "§6§l✦ §eBolsillos Sin Fondo " + level.getColor() + level.getRoman() + suffix + " §6§l✦";
        } else if (skillService.hasSkill(uuid, Skill.BOLSILLOS_PROFUNDOS)) {
            SkillLevel level = skillService.getSkillLevel(uuid, Skill.BOLSILLOS_PROFUNDOS);
            return "§6§l✦ §eBolsillos Profundos " + level.getColor() + level.getRoman() + suffix + " §6§l✦";
        }
        return "§eMochila" + suffix;
    }
    
    // ==================== ENDER CHEST PORTABLE ====================
    
    /**
     * Abre el ender chest del jugador (si tiene la habilidad)
     */
    public void openPortableEnderChest(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!skillService.hasSkill(uuid, Skill.COFRE_INTERIOR)) {
            player.sendMessage("§c✗ No tienes desbloqueada la habilidad de Cofre Interior.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            return;
        }
        
        player.openInventory(player.getEnderChest());
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.7f, 1.0f);
        player.sendMessage("§d✦ §5Cofre Interior abierto");
    }
    
    // ==================== EVENTOS ====================
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        Inventory inv = event.getInventory();
        
        // Mochila propia
        if (inv.getHolder() instanceof BackpackHolder holder) {
            UUID uuid = holder.getOwner();
            int backpackNumber = holder.getBackpackNumber();
            
            // Guardar contenido con el tamaño actual
            int currentSize = inv.getSize();
            ItemStack[] contents = new ItemStack[currentSize];
            
            // Guardar contenido actual
            for (int i = 0; i < currentSize; i++) {
                contents[i] = inv.getItem(i);
            }
            
            setBackpackContents(uuid, backpackNumber, contents);
            
            // Guardar a archivo
            saveBackpacks();
            
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.7f, 1.2f);
        }
        
        // Mochila vista por moderador (también guardar cambios)
        if (inv.getHolder() instanceof ModViewHolder modHolder) {
            UUID ownerUuid = modHolder.getOwner();
            int backpackNumber = modHolder.getBackpackNumber();
            
            // Guardar contenido modificado
            ItemStack[] contents = new ItemStack[inv.getSize()];
            for (int i = 0; i < inv.getSize(); i++) {
                contents[i] = inv.getItem(i);
            }
            
            setBackpackContents(ownerUuid, backpackNumber, contents);
            saveBackpacks();
            
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.7f, 0.8f);
            player.sendMessage("§a✓ Cambios guardados en la mochila #" + backpackNumber + ".");
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Inventory inv = event.getInventory();
        
        // Permitir interacción normal con mochilas propias
        if (inv.getHolder() instanceof BackpackHolder) return;
        
        // Permitir interacción de moderadores con mochilas ajenas
        if (inv.getHolder() instanceof ModViewHolder) return;
    }
    
    // ==================== PERSISTENCIA ====================
    
    private void loadBackpacks() {
        if (!dataFile.exists()) return;
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection playersSection = config.getConfigurationSection("backpacks");
        
        if (playersSection == null) return;
        
        int migratedCount = 0;
        int loadedCount = 0;
        
        for (String uuidStr : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                
                // Verificar si es formato nuevo (tiene subsecciones numéricas) o antiguo (lista directa)
                ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidStr);
                
                if (playerSection != null) {
                    // Formato nuevo: backpacks.uuid.1, backpacks.uuid.2, etc.
                    Map<Integer, ItemStack[]> playerBackpacks = new HashMap<>();
                    
                    for (String numberStr : playerSection.getKeys(false)) {
                        try {
                            int backpackNumber = Integer.parseInt(numberStr);
                            if (backpackNumber < 1 || backpackNumber > 10) continue;
                            
                            List<?> itemsList = playerSection.getList(numberStr);
                            if (itemsList != null) {
                                ItemStack[] contents = new ItemStack[54]; // Máximo tamaño
                                for (int i = 0; i < itemsList.size() && i < 54; i++) {
                                    Object item = itemsList.get(i);
                                    if (item instanceof ItemStack) {
                                        contents[i] = (ItemStack) item;
                                    }
                                }
                                playerBackpacks.put(backpackNumber, contents);
                                loadedCount++;
                            }
                        } catch (NumberFormatException e) {
                            // Ignorar claves que no sean números
                        }
                    }
                    
                    if (!playerBackpacks.isEmpty()) {
                        backpacks.put(uuid, playerBackpacks);
                    }
                } else {
                    // Formato antiguo: backpacks.uuid = lista
                    List<?> itemsList = playersSection.getList(uuidStr);
                    
                    if (itemsList != null) {
                        ItemStack[] contents = new ItemStack[54]; // Máximo tamaño
                        for (int i = 0; i < itemsList.size() && i < 54; i++) {
                            Object item = itemsList.get(i);
                            if (item instanceof ItemStack) {
                                contents[i] = (ItemStack) item;
                            }
                        }
                        
                        // Migrar a formato nuevo (mochila #1)
                        Map<Integer, ItemStack[]> playerBackpacks = new HashMap<>();
                        playerBackpacks.put(1, contents);
                        backpacks.put(uuid, playerBackpacks);
                        
                        migratedCount++;
                        plugin.getLogger().info("[Backpack] Migrado formato antiguo de " + uuidStr + " a mochila #1");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error cargando mochila de " + uuidStr + ": " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Cargadas " + backpacks.size() + " mochilas de jugadores (" + loadedCount + " mochilas nuevas, " + migratedCount + " migradas).");
        
        // Guardar inmediatamente si hubo migraciones
        if (migratedCount > 0) {
            saveBackpacks();
            plugin.getLogger().info("Mochilas migradas guardadas en formato nuevo.");
        }
    }
    
    public void saveBackpacks() {
        FileConfiguration config = new YamlConfiguration();
        
        for (Map.Entry<UUID, Map<Integer, ItemStack[]>> entry : backpacks.entrySet()) {
            String uuidStr = entry.getKey().toString();
            Map<Integer, ItemStack[]> playerBackpacks = entry.getValue();
            
            for (Map.Entry<Integer, ItemStack[]> backpackEntry : playerBackpacks.entrySet()) {
                int backpackNumber = backpackEntry.getKey();
                ItemStack[] contents = backpackEntry.getValue();
                
                String path = "backpacks." + uuidStr + "." + backpackNumber;
                config.set(path, Arrays.asList(contents));
            }
        }
        
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando mochilas: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene el contenido de la mochila para un jugador (mochila 1 por defecto)
     */
    public ItemStack[] getBackpackContents(UUID uuid) {
        return getBackpackContents(uuid, 1);
    }
    
    /**
     * Obtiene el contenido de la mochila para un jugador (con número específico)
     */
    public ItemStack[] getBackpackContents(UUID uuid, int backpackNumber) {
        Map<Integer, ItemStack[]> playerBackpacks = backpacks.get(uuid);
        if (playerBackpacks == null) return null;
        return playerBackpacks.get(backpackNumber);
    }
    
    /**
     * Establece el contenido de una mochila específica
     */
    private void setBackpackContents(UUID uuid, int backpackNumber, ItemStack[] contents) {
        Map<Integer, ItemStack[]> playerBackpacks = backpacks.computeIfAbsent(uuid, k -> new HashMap<>());
        playerBackpacks.put(backpackNumber, contents);
    }
    
    /**
     * Verifica si un jugador tiene acceso a mochila
     */
    public boolean hasBackpack(UUID uuid) {
        return getBackpackSize(uuid) > 0;
    }
    
    // ==================== MODERACIÓN ====================
    
    /**
     * Holder para mochilas vistas por moderadores
     */
    public class ModViewHolder implements InventoryHolder {
        private final UUID owner;
        private final UUID moderator;
        private final int backpackNumber;
        private Inventory inventory;
        
        public ModViewHolder(UUID owner, UUID moderator) {
            this(owner, moderator, 1);
        }
        
        public ModViewHolder(UUID owner, UUID moderator, int backpackNumber) {
            this.owner = owner;
            this.moderator = moderator;
            this.backpackNumber = backpackNumber;
        }
        
        public UUID getOwner() { return owner; }
        public UUID getModerator() { return moderator; }
        public int getBackpackNumber() { return backpackNumber; }
        
        @Override
        public Inventory getInventory() { return inventory; }
        public void setInventory(Inventory inv) { this.inventory = inv; }
    }
    
    /**
     * Permite a un moderador ver la mochila de otro jugador (mochila 1 por defecto)
     */
    public void openBackpackAsAdmin(Player moderator, UUID targetUuid, String targetName) {
        openBackpackAsAdmin(moderator, targetUuid, targetName, 1);
    }
    
    /**
     * Permite a un moderador ver la mochila de otro jugador (con número específico)
     */
    public void openBackpackAsAdmin(Player moderator, UUID targetUuid, String targetName, int backpackNumber) {
        // Validar número de mochila
        if (backpackNumber < 1 || backpackNumber > 10) {
            moderator.sendMessage("§c✗ Número de mochila inválido. Usa del 1 al 10.");
            return;
        }
        
        int size = getBackpackSize(targetUuid);
        if (size == 0) {
            moderator.sendMessage("§c✗ " + targetName + " no tiene mochila desbloqueada.");
            return;
        }
        
        ModViewHolder holder = new ModViewHolder(targetUuid, moderator.getUniqueId(), backpackNumber);
        String title = "§c[MOD] §eMochila #" + backpackNumber + " de " + targetName;
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);
        
        // Cargar contenido
        ItemStack[] contents = getBackpackContents(targetUuid, backpackNumber);
        if (contents != null) {
            for (int i = 0; i < Math.min(contents.length, size); i++) {
                inv.setItem(i, contents[i]);
            }
        }
        
        moderator.openInventory(inv);
        moderator.playSound(moderator.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 0.8f);
        moderator.sendMessage("§a✓ Viendo mochila #" + backpackNumber + " de §e" + targetName);
        
        // Log para seguridad
        plugin.getLogger().info("[MOCHILA-MOD] " + moderator.getName() + " abrió mochila #" + backpackNumber + " de " + targetName);
    }
    
    /**
     * Lista todas las mochilas con contenido
     */
    public List<String> getBackpackList() {
        List<String> list = new ArrayList<>();
        
        for (Map.Entry<UUID, Map<Integer, ItemStack[]>> entry : backpacks.entrySet()) {
            UUID uuid = entry.getKey();
            Map<Integer, ItemStack[]> playerBackpacks = entry.getValue();
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            String displayName = name != null ? name : uuid.toString().substring(0, 8);
            
            // Iterar sobre cada mochila del jugador
            for (Map.Entry<Integer, ItemStack[]> backpackEntry : playerBackpacks.entrySet()) {
                int backpackNumber = backpackEntry.getKey();
                ItemStack[] contents = backpackEntry.getValue();
                
                int itemCount = 0;
                for (ItemStack item : contents) {
                    if (item != null && item.getType() != Material.AIR) {
                        itemCount++;
                    }
                }
                
                if (itemCount > 0) {
                    String entry_text = displayName + " (mochila #" + backpackNumber + ", " + itemCount + " items)";
                    list.add(entry_text);
                }
            }
        }
        
        return list;
    }
    
    /**
     * Vacía la mochila de un jugador (comando de moderación) - mochila 1 por defecto
     */
    public boolean clearBackpack(UUID targetUuid, Player moderator) {
        return clearBackpack(targetUuid, moderator, 1);
    }
    
    /**
     * Vacía la mochila de un jugador (comando de moderación) - con número específico
     */
    public boolean clearBackpack(UUID targetUuid, Player moderator, int backpackNumber) {
        // Validar número de mochila
        if (backpackNumber < 1 || backpackNumber > 10) {
            moderator.sendMessage("§c✗ Número de mochila inválido. Usa del 1 al 10.");
            return false;
        }
        
        Map<Integer, ItemStack[]> playerBackpacks = backpacks.get(targetUuid);
        if (playerBackpacks == null || !playerBackpacks.containsKey(backpackNumber)) {
            return false;
        }
        
        playerBackpacks.put(backpackNumber, new ItemStack[54]);
        saveBackpacks();
        
        // Log para seguridad
        String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();
        plugin.getLogger().warning("[MOCHILA-MOD] " + moderator.getName() + " vació la mochila #" + backpackNumber + " de " + targetName);
        
        return true;
    }
}
