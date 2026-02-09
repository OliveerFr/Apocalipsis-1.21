package me.apocalipsis.skills;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

import me.apocalipsis.Apocalipsis;

/**
 * Sistema de mochila virtual para habilidades de almacenamiento.
 * Soporta múltiples mochilas (hasta 10 por jugador).
 */
public class BackpackService implements Listener {
    
    private final Apocalipsis plugin;
    private final SkillService skillService;
    private final File dataFile;
    
    // NUEVA ESTRUCTURA: UUID -> Mundo -> número de mochila (1-10) -> contenido de la mochila
    // Esto separa las mochilas por ciclo/mundo para evitar transferencia entre ciclos
    private final Map<UUID, Map<String, Map<Integer, ItemStack[]>>> backpacks = new HashMap<>();
    
    public BackpackService(Apocalipsis plugin, SkillService skillService) {
        this.plugin = plugin;
        this.skillService = skillService;
        this.dataFile = new File(plugin.getDataFolder(), "backpacks.yml");
        loadBackpacks();
    }
    
    // ==================== INVENTORY HOLDER ====================
    
    private static class BackpackHolder implements InventoryHolder {
        private final UUID owner;
        private final int backpackNumber;
        private final String worldName;
        private Inventory inventory;
        
        public BackpackHolder(UUID owner) {
            this(owner, 1, "world");
        }
        
        public BackpackHolder(UUID owner, int backpackNumber, String worldName) {
            this.owner = owner;
            this.backpackNumber = backpackNumber;
            this.worldName = worldName;
        }
        
        public UUID getOwner() { return owner; }
        public int getBackpackNumber() { return backpackNumber; }
        public String getWorldName() { return worldName; }
        
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
        String worldName = player.getWorld().getName();
        
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
        
        BackpackHolder holder = new BackpackHolder(uuid, backpackNumber, worldName);
        String title = getBackpackTitle(uuid, backpackNumber) + " §8(§b" + worldName + "§8)";
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);
        
        // Cargar contenido (preservando todo el contenido previo)
        ItemStack[] contents = getBackpackContents(uuid, worldName, backpackNumber);
        if (contents != null) {
            // Si el tamaño guardado es menor que el actual, expandir
            if (contents.length < size) {
                plugin.getLogger().info("[Backpack] Expandiendo mochila #" + backpackNumber + " de " + player.getName() + 
                    " de " + contents.length + " a " + size + " slots");
                ItemStack[] expanded = new ItemStack[size];
                System.arraycopy(contents, 0, expanded, 0, contents.length);
                setBackpackContents(uuid, worldName, backpackNumber, expanded);
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
            player.sendMessage("§e§l✦ §6Mochila #" + backpackNumber + " abierta §8(§b" + worldName + "§8)");
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
            String worldName = holder.getWorldName();
            
            // Guardar contenido con el tamaño actual
            int currentSize = inv.getSize();
            ItemStack[] contents = new ItemStack[currentSize];
            
            // Guardar contenido actual
            for (int i = 0; i < currentSize; i++) {
                contents[i] = inv.getItem(i);
            }
            
            setBackpackContents(uuid, worldName, backpackNumber, contents);
            
            // Guardar a archivo
            saveBackpacks();
            
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.7f, 1.2f);
        }
        
        // Mochila vista por moderador (también guardar cambios)
        if (inv.getHolder() instanceof ModViewHolder modHolder) {
            UUID ownerUuid = modHolder.getOwner();
            int backpackNumber = modHolder.getBackpackNumber();
            String worldName = modHolder.getWorldName();
            
            // Guardar contenido modificado
            ItemStack[] contents = new ItemStack[inv.getSize()];
            for (int i = 0; i < inv.getSize(); i++) {
                contents[i] = inv.getItem(i);
            }
            
            setBackpackContents(ownerUuid, worldName, backpackNumber, contents);
            saveBackpacks();
            
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.7f, 0.8f);
            player.sendMessage("§a✓ Cambios guardados en la mochila #" + backpackNumber + " (§e" + worldName + "§a).");
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
                ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidStr);
                
                if (playerSection == null) {
                    // Formato muy antiguo: backpacks.uuid = lista directa
                    List<?> itemsList = playersSection.getList(uuidStr);
                    if (itemsList != null) {
                        ItemStack[] contents = new ItemStack[54];
                        for (int i = 0; i < itemsList.size() && i < 54; i++) {
                            Object item = itemsList.get(i);
                            if (item instanceof ItemStack) {
                                contents[i] = (ItemStack) item;
                            }
                        }
                        
                        // Migrar a formato nuevo: UUID -> "world" -> 1
                        Map<String, Map<Integer, ItemStack[]>> worldBackpacks = new HashMap<>();
                        Map<Integer, ItemStack[]> playerBackpacks = new HashMap<>();
                        playerBackpacks.put(1, contents);
                        worldBackpacks.put("world", playerBackpacks);
                        backpacks.put(uuid, worldBackpacks);
                        
                        migratedCount++;
                        plugin.getLogger().info("[Backpack] Migrado formato antiguo de " + uuidStr + " a world/mochila #1");
                    }
                    continue;
                }
                
                Map<String, Map<Integer, ItemStack[]>> worldBackpacks = new HashMap<>();
                
                // Verificar si tiene subsecciones de mundos o es formato sin mundos
                boolean hasWorldSections = false;
                for (String key : playerSection.getKeys(false)) {
                    if (playerSection.isConfigurationSection(key)) {
                        // Si tiene subsecciones, verificar si son mundos (no números)
                        try {
                            Integer.parseInt(key);
                            // Es un número, formato antiguo sin mundos
                        } catch (NumberFormatException e) {
                            // No es un número, probablemente es un nombre de mundo
                            hasWorldSections = true;
                            break;
                        }
                    }
                }
                
                if (hasWorldSections) {
                    // Formato nuevo: backpacks.uuid.worldName.1
                    for (String worldName : playerSection.getKeys(false)) {
                        ConfigurationSection worldSection = playerSection.getConfigurationSection(worldName);
                        if (worldSection == null) continue;
                        
                        Map<Integer, ItemStack[]> playerBackpacks = new HashMap<>();
                        
                        for (String numberStr : worldSection.getKeys(false)) {
                            try {
                                int backpackNumber = Integer.parseInt(numberStr);
                                if (backpackNumber < 1 || backpackNumber > 10) continue;
                                
                                List<?> itemsList = worldSection.getList(numberStr);
                                if (itemsList != null) {
                                    ItemStack[] contents = new ItemStack[54];
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
                            worldBackpacks.put(worldName, playerBackpacks);
                        }
                    }
                } else {
                    // Formato antiguo sin mundo: backpacks.uuid.1, backpacks.uuid.2, etc.
                    Map<Integer, ItemStack[]> playerBackpacks = new HashMap<>();
                    
                    for (String numberStr : playerSection.getKeys(false)) {
                        try {
                            int backpackNumber = Integer.parseInt(numberStr);
                            if (backpackNumber < 1 || backpackNumber > 10) continue;
                            
                            List<?> itemsList = playerSection.getList(numberStr);
                            if (itemsList != null) {
                                ItemStack[] contents = new ItemStack[54];
                                for (int i = 0; i < itemsList.size() && i < 54; i++) {
                                    Object item = itemsList.get(i);
                                    if (item instanceof ItemStack) {
                                        contents[i] = (ItemStack) item;
                                    }
                                }
                                playerBackpacks.put(backpackNumber, contents);
                            }
                        } catch (NumberFormatException e) {
                            // Ignorar claves que no sean números
                        }
                    }
                    
                    if (!playerBackpacks.isEmpty()) {
                        // Migrar a formato con mundo
                        worldBackpacks.put("world", playerBackpacks);
                        migratedCount++;
                        plugin.getLogger().info("[Backpack] Migrado " + uuidStr + " a formato con mundos");
                    }
                }
                
                if (!worldBackpacks.isEmpty()) {
                    backpacks.put(uuid, worldBackpacks);
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
        
        for (Map.Entry<UUID, Map<String, Map<Integer, ItemStack[]>>> entry : backpacks.entrySet()) {
            String uuidStr = entry.getKey().toString();
            Map<String, Map<Integer, ItemStack[]>> worldBackpacks = entry.getValue();
            
            for (Map.Entry<String, Map<Integer, ItemStack[]>> worldEntry : worldBackpacks.entrySet()) {
                String worldName = worldEntry.getKey();
                Map<Integer, ItemStack[]> playerBackpacks = worldEntry.getValue();
                
                for (Map.Entry<Integer, ItemStack[]> backpackEntry : playerBackpacks.entrySet()) {
                    int backpackNumber = backpackEntry.getKey();
                    ItemStack[] contents = backpackEntry.getValue();
                    
                    String path = "backpacks." + uuidStr + "." + worldName + "." + backpackNumber;
                    config.set(path, Arrays.asList(contents));
                }
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
        return getBackpackContents(uuid, "world", 1);
    }
    
    /**
     * Obtiene el contenido de la mochila para un jugador (con número específico)
     */
    public ItemStack[] getBackpackContents(UUID uuid, int backpackNumber) {
        return getBackpackContents(uuid, "world", backpackNumber);
    }
    
    /**
     * Obtiene el contenido de la mochila para un jugador en un mundo específico
     */
    public ItemStack[] getBackpackContents(UUID uuid, String worldName, int backpackNumber) {
        Map<String, Map<Integer, ItemStack[]>> worldBackpacks = backpacks.get(uuid);
        if (worldBackpacks == null) return null;
        
        Map<Integer, ItemStack[]> playerBackpacks = worldBackpacks.get(worldName);
        if (playerBackpacks == null) return null;
        
        return playerBackpacks.get(backpackNumber);
    }
    
    /**
     * Establece el contenido de una mochila específica
     */
    private void setBackpackContents(UUID uuid, int backpackNumber, ItemStack[] contents) {
        setBackpackContents(uuid, "world", backpackNumber, contents);
    }
    
    /**
     * Establece el contenido de una mochila específica en un mundo específico
     */
    private void setBackpackContents(UUID uuid, String worldName, int backpackNumber, ItemStack[] contents) {
        Map<String, Map<Integer, ItemStack[]>> worldBackpacks = backpacks.computeIfAbsent(uuid, k -> new HashMap<>());
        Map<Integer, ItemStack[]> playerBackpacks = worldBackpacks.computeIfAbsent(worldName, k -> new HashMap<>());
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
        private final String worldName;
        private Inventory inventory;
        
        public ModViewHolder(UUID owner, UUID moderator) {
            this(owner, moderator, 1, "world");
        }
        
        public ModViewHolder(UUID owner, UUID moderator, int backpackNumber, String worldName) {
            this.owner = owner;
            this.moderator = moderator;
            this.backpackNumber = backpackNumber;
            this.worldName = worldName;
        }
        
        public UUID getOwner() { return owner; }
        public UUID getModerator() { return moderator; }
        public int getBackpackNumber() { return backpackNumber; }
        public String getWorldName() { return worldName; }
        
        @Override
        public Inventory getInventory() { return inventory; }
        public void setInventory(Inventory inv) { this.inventory = inv; }
    }
    
    /**
     * Permite a un moderador ver la mochila de otro jugador (mochila 1 por defecto)
     */
    public void openBackpackAsAdmin(Player moderator, UUID targetUuid, String targetName) {
        // Detectar mundo del jugador objetivo
        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        String worldName;
        if (targetPlayer != null) {
            // Si está online, usar su mundo actual
            worldName = targetPlayer.getWorld().getName();
        } else {
            // Si está offline, usar ciclo activo o mundo del moderador
            String activeCycle = plugin.getCicloManager().getActiveCycle();
            worldName = activeCycle != null ? activeCycle : moderator.getWorld().getName();
        }
        openBackpackAsAdmin(moderator, targetUuid, targetName, 1, worldName);
    }
    
    /**
     * Permite a un moderador ver la mochila de otro jugador (con número específico)
     */
    public void openBackpackAsAdmin(Player moderator, UUID targetUuid, String targetName, int backpackNumber) {
        // Detectar mundo del jugador objetivo o usar "world" por defecto
        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        String worldName = targetPlayer != null ? targetPlayer.getWorld().getName() : "world";
        openBackpackAsAdmin(moderator, targetUuid, targetName, backpackNumber, worldName);
    }
    
    /**
     * Permite a un moderador ver la mochila de otro jugador en un mundo específico
     */
    public void openBackpackAsAdmin(Player moderator, UUID targetUuid, String targetName, int backpackNumber, String worldName) {
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
        
        ModViewHolder holder = new ModViewHolder(targetUuid, moderator.getUniqueId(), backpackNumber, worldName);
        String title = "§c[MOD] §eMochila #" + backpackNumber + " de " + targetName + " §8(§b" + worldName + "§8)";
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);
        
        // Cargar contenido
        ItemStack[] contents = getBackpackContents(targetUuid, worldName, backpackNumber);
        if (contents != null) {
            for (int i = 0; i < Math.min(contents.length, size); i++) {
                inv.setItem(i, contents[i]);
            }
        }
        
        moderator.openInventory(inv);
        moderator.playSound(moderator.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 0.8f);
        moderator.sendMessage("§a✓ Viendo mochila #" + backpackNumber + " de §e" + targetName + " §7en mundo §b" + worldName);
        
        // Log para seguridad
        plugin.getLogger().info("[MOCHILA-MOD] " + moderator.getName() + " abrió mochila #" + backpackNumber + " de " + targetName + " (mundo: " + worldName + ")");
    }
    
    /**
     * Lista todas las mochilas con contenido
     */
    public List<String> getBackpackList() {
        List<String> list = new ArrayList<>();
        
        for (Map.Entry<UUID, Map<String, Map<Integer, ItemStack[]>>> entry : backpacks.entrySet()) {
            UUID uuid = entry.getKey();
            Map<String, Map<Integer, ItemStack[]>> worldBackpacks = entry.getValue();
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            String displayName = name != null ? name : uuid.toString().substring(0, 8);
            
            // Iterar sobre cada mundo
            for (Map.Entry<String, Map<Integer, ItemStack[]>> worldEntry : worldBackpacks.entrySet()) {
                String worldName = worldEntry.getKey();
                Map<Integer, ItemStack[]> playerBackpacks = worldEntry.getValue();
                
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
                        String entry_text = displayName + " (mochila #" + backpackNumber + ", mundo: " + worldName + ", " + itemCount + " items)";
                        list.add(entry_text);
                    }
                }
            }
        }
        
        return list;
    }
    
    /**
     * Vacía la mochila de un jugador (comando de moderación) - mochila 1 por defecto
     */
    public boolean clearBackpack(UUID targetUuid, Player moderator) {
        // Detectar mundo del jugador objetivo
        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        String worldName;
        if (targetPlayer != null) {
            worldName = targetPlayer.getWorld().getName();
        } else {
            String activeCycle = plugin.getCicloManager().getActiveCycle();
            worldName = activeCycle != null ? activeCycle : moderator.getWorld().getName();
        }
        return clearBackpack(targetUuid, moderator, 1, worldName);
    }
    
    /**
     * Vacía la mochila de un jugador (comando de moderación) - con número específico
     */
    public boolean clearBackpack(UUID targetUuid, Player moderator, int backpackNumber) {
        // Detectar mundo del jugador objetivo o usar "world" por defecto
        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        String worldName = targetPlayer != null ? targetPlayer.getWorld().getName() : "world";
        return clearBackpack(targetUuid, moderator, backpackNumber, worldName);
    }
    
    /**
     * Vacía la mochila de un jugador en un mundo específico (comando de moderación)
     */
    public boolean clearBackpack(UUID targetUuid, Player moderator, int backpackNumber, String worldName) {
        // Validar número de mochila
        if (backpackNumber < 1 || backpackNumber > 10) {
            moderator.sendMessage("§c✗ Número de mochila inválido. Usa del 1 al 10.");
            return false;
        }
        
        Map<String, Map<Integer, ItemStack[]>> worldBackpacks = backpacks.get(targetUuid);
        if (worldBackpacks == null) {
            return false;
        }
        
        Map<Integer, ItemStack[]> playerBackpacks = worldBackpacks.get(worldName);
        if (playerBackpacks == null || !playerBackpacks.containsKey(backpackNumber)) {
            return false;
        }
        
        playerBackpacks.put(backpackNumber, new ItemStack[54]);
        saveBackpacks();
        
        // Log para seguridad
        String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();
        plugin.getLogger().warning("[MOCHILA-MOD] " + moderator.getName() + " vació la mochila #" + backpackNumber + " de " + targetName + " (mundo: " + worldName + ")");
        
        return true;
    }
}
