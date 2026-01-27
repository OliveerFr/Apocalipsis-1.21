package me.apocalipsis.ciclos;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.apocalipsis.Apocalipsis;

/**
 * Gestiona inventarios separados por mundo.
 * Cada mundo tiene su propio conjunto de inventarios de jugadores.
 * Cuando un jugador cambia de mundo, su inventario se guarda y se carga el del nuevo mundo.
 */
public class WorldInventoryManager {
    
    private final Apocalipsis plugin;
    private final File dataFile;
    
    // UUID -> Mundo -> PlayerInventoryData
    private final Map<UUID, Map<String, PlayerInventoryData>> worldInventories = new HashMap<>();
    
    /**
     * Datos de inventario de un jugador en un mundo específico
     */
    public static class PlayerInventoryData {
        private ItemStack[] inventory;          // 36 slots (9 hotbar + 27 main)
        private ItemStack[] armorContents;      // 4 slots (helmet, chest, legs, boots)
        private ItemStack offHand;              // 1 slot
        private int level;                      // Nivel de experiencia
        private float exp;                      // Progreso al siguiente nivel
        private int foodLevel;                  // Hambre (0-20)
        private float saturation;               // Saturación
        private double health;                  // Vida
        
        public PlayerInventoryData() {
            this.inventory = new ItemStack[36];
            this.armorContents = new ItemStack[4];
            this.offHand = null;
            this.level = 0;
            this.exp = 0.0f;
            this.foodLevel = 20;
            this.saturation = 5.0f;
            this.health = 20.0;
        }
        
        public ItemStack[] getInventory() { return inventory; }
        public void setInventory(ItemStack[] inventory) { this.inventory = inventory; }
        
        public ItemStack[] getArmorContents() { return armorContents; }
        public void setArmorContents(ItemStack[] armorContents) { this.armorContents = armorContents; }
        
        public ItemStack getOffHand() { return offHand; }
        public void setOffHand(ItemStack offHand) { this.offHand = offHand; }
        
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        
        public float getExp() { return exp; }
        public void setExp(float exp) { this.exp = exp; }
        
        public int getFoodLevel() { return foodLevel; }
        public void setFoodLevel(int foodLevel) { this.foodLevel = foodLevel; }
        
        public float getSaturation() { return saturation; }
        public void setSaturation(float saturation) { this.saturation = saturation; }
        
        public double getHealth() { return health; }
        public void setHealth(double health) { this.health = health; }
    }
    
    public WorldInventoryManager(Apocalipsis plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "world_inventories.yml");
        loadData();
    }
    
    /**
     * Guarda el inventario actual del jugador para su mundo actual
     */
    public void saveInventory(Player player, String worldName) {
        UUID uuid = player.getUniqueId();
        
        PlayerInventoryData data = new PlayerInventoryData();
        
        // Guardar inventario principal
        data.setInventory(player.getInventory().getContents());
        
        // Guardar armadura
        data.setArmorContents(player.getInventory().getArmorContents());
        
        // Guardar offhand
        data.setOffHand(player.getInventory().getItemInOffHand());
        
        // Guardar experiencia vanilla
        data.setLevel(player.getLevel());
        data.setExp(player.getExp());
        
        // Guardar estado del jugador
        data.setFoodLevel(player.getFoodLevel());
        data.setSaturation(player.getSaturation());
        data.setHealth(player.getHealth());
        
        // Almacenar
        worldInventories.computeIfAbsent(uuid, k -> new HashMap<>()).put(worldName, data);
        
        plugin.getLogger().info("[WorldInventory] Guardado inventario de " + player.getName() + 
                                " para mundo: " + worldName);
    }
    
    /**
     * Carga el inventario del jugador para un mundo específico
     * Si no existe, crea uno vacío
     */
    public void loadInventory(Player player, String worldName) {
        UUID uuid = player.getUniqueId();
        
        PlayerInventoryData data = worldInventories
            .getOrDefault(uuid, new HashMap<>())
            .get(worldName);
        
        if (data == null) {
            // No existe inventario para este mundo, crear uno vacío
            plugin.getLogger().info("[WorldInventory] No hay inventario guardado para " + 
                                    player.getName() + " en mundo: " + worldName + ". Creando vacío.");
            data = new PlayerInventoryData();
        }
        
        // Limpiar inventario actual
        player.getInventory().clear();
        
        // Cargar inventario
        if (data.getInventory() != null) {
            player.getInventory().setContents(data.getInventory());
        }
        
        // Cargar armadura
        if (data.getArmorContents() != null) {
            player.getInventory().setArmorContents(data.getArmorContents());
        }
        
        // Cargar offhand
        if (data.getOffHand() != null) {
            player.getInventory().setItemInOffHand(data.getOffHand());
        }
        
        // Cargar experiencia vanilla
        player.setLevel(data.getLevel());
        player.setExp(data.getExp());
        
        // Cargar estado
        player.setFoodLevel(data.getFoodLevel());
        player.setSaturation(data.getSaturation());
        player.setHealth(Math.min(data.getHealth(), player.getMaxHealth()));
        
        plugin.getLogger().info("[WorldInventory] Cargado inventario de " + player.getName() + 
                                " para mundo: " + worldName);
    }
    
    /**
     * Limpia el inventario del jugador (todo a cero)
     */
    public void clearPlayerInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        player.setLevel(0);
        player.setExp(0);
        player.setFoodLevel(20);
        player.setSaturation(5.0f);
        
        plugin.getLogger().info("[WorldInventory] Limpiado inventario de " + player.getName());
    }
    
    /**
     * Verifica si existe un inventario guardado para el jugador en un mundo
     */
    public boolean hasInventory(UUID uuid, String worldName) {
        return worldInventories.containsKey(uuid) && 
               worldInventories.get(uuid).containsKey(worldName);
    }
    
    /**
     * Elimina el inventario guardado de un jugador en un mundo
     */
    public void deleteInventory(UUID uuid, String worldName) {
        if (worldInventories.containsKey(uuid)) {
            worldInventories.get(uuid).remove(worldName);
            plugin.getLogger().info("[WorldInventory] Eliminado inventario de " + uuid + 
                                    " para mundo: " + worldName);
        }
    }
    
    // ==================== PERSISTENCIA ====================
    
    public void loadData() {
        if (!dataFile.exists()) {
            plugin.getLogger().info("[WorldInventory] No existe world_inventories.yml, creando nuevo");
            return;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        
        for (String uuidStr : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                
                for (String worldName : config.getConfigurationSection(uuidStr).getKeys(false)) {
                    String path = uuidStr + "." + worldName;
                    
                    PlayerInventoryData data = new PlayerInventoryData();
                    
                    // Cargar inventario
                    @SuppressWarnings("unchecked")
                    List<ItemStack> invList = (List<ItemStack>) config.getList(path + ".inventory");
                    if (invList != null) {
                        data.setInventory(invList.toArray(new ItemStack[0]));
                    }
                    
                    // Cargar armadura
                    @SuppressWarnings("unchecked")
                    List<ItemStack> armorList = (List<ItemStack>) config.getList(path + ".armor");
                    if (armorList != null) {
                        data.setArmorContents(armorList.toArray(new ItemStack[0]));
                    }
                    
                    // Cargar offhand
                    ItemStack offhand = config.getItemStack(path + ".offhand");
                    data.setOffHand(offhand);
                    
                    // Cargar experiencia y estado
                    data.setLevel(config.getInt(path + ".level", 0));
                    data.setExp((float) config.getDouble(path + ".exp", 0.0));
                    data.setFoodLevel(config.getInt(path + ".food", 20));
                    data.setSaturation((float) config.getDouble(path + ".saturation", 5.0));
                    data.setHealth(config.getDouble(path + ".health", 20.0));
                    
                    worldInventories.computeIfAbsent(uuid, k -> new HashMap<>()).put(worldName, data);
                }
                
            } catch (Exception e) {
                plugin.getLogger().warning("[WorldInventory] Error cargando inventario: " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("[WorldInventory] Cargados inventarios de " + 
                                worldInventories.size() + " jugadores");
    }
    
    public void saveData() {
        FileConfiguration config = new YamlConfiguration();
        
        for (Map.Entry<UUID, Map<String, PlayerInventoryData>> playerEntry : worldInventories.entrySet()) {
            String uuidStr = playerEntry.getKey().toString();
            
            for (Map.Entry<String, PlayerInventoryData> worldEntry : playerEntry.getValue().entrySet()) {
                String worldName = worldEntry.getKey();
                PlayerInventoryData data = worldEntry.getValue();
                String path = uuidStr + "." + worldName;
                
                // Guardar inventario
                config.set(path + ".inventory", Arrays.asList(data.getInventory()));
                
                // Guardar armadura
                config.set(path + ".armor", Arrays.asList(data.getArmorContents()));
                
                // Guardar offhand
                config.set(path + ".offhand", data.getOffHand());
                
                // Guardar experiencia y estado
                config.set(path + ".level", data.getLevel());
                config.set(path + ".exp", data.getExp());
                config.set(path + ".food", data.getFoodLevel());
                config.set(path + ".saturation", data.getSaturation());
                config.set(path + ".health", data.getHealth());
            }
        }
        
        try {
            config.save(dataFile);
            plugin.getLogger().info("[WorldInventory] Guardados inventarios de " + 
                                    worldInventories.size() + " jugadores");
        } catch (IOException e) {
            plugin.getLogger().severe("[WorldInventory] Error guardando inventarios: " + e.getMessage());
        }
    }
}
