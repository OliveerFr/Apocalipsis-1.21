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
 */
public class BackpackService implements Listener {
    
    private final Apocalipsis plugin;
    private final SkillService skillService;
    private final File dataFile;
    
    // UUID -> contenido de la mochila
    private final Map<UUID, ItemStack[]> backpacks = new HashMap<>();
    
    public BackpackService(Apocalipsis plugin, SkillService skillService) {
        this.plugin = plugin;
        this.skillService = skillService;
        this.dataFile = new File(plugin.getDataFolder(), "backpacks.yml");
        loadBackpacks();
    }
    
    // ==================== INVENTORY HOLDER ====================
    
    public class BackpackHolder implements InventoryHolder {
        private final UUID owner;
        private Inventory inventory;
        
        public BackpackHolder(UUID owner) {
            this.owner = owner;
        }
        
        public UUID getOwner() { return owner; }
        
        @Override
        public Inventory getInventory() { return inventory; }
        public void setInventory(Inventory inv) { this.inventory = inv; }
    }
    
    // ==================== MOCHILA ====================
    
    /**
     * Abre la mochila del jugador
     */
    public void openBackpack(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Verificar habilidad
        int size = getBackpackSize(uuid);
        if (size == 0) {
            player.sendMessage("§c✗ No tienes desbloqueada la habilidad de mochila.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            return;
        }
        
        BackpackHolder holder = new BackpackHolder(uuid);
        String title = getBackpackTitle(uuid);
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);
        
        // Cargar contenido
        ItemStack[] contents = backpacks.get(uuid);
        if (contents != null) {
            for (int i = 0; i < Math.min(contents.length, size); i++) {
                inv.setItem(i, contents[i]);
            }
        }
        
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.7f, 1.2f);
    }
    
    /**
     * Obtiene el tamaño de la mochila según las habilidades del jugador
     */
    public int getBackpackSize(UUID uuid) {
        if (skillService.hasSkill(uuid, Skill.INVENTARIO_INFINITO)) {
            return 54; // Cofre doble
        } else if (skillService.hasSkill(uuid, Skill.BOLSILLOS_SIN_FONDO)) {
            return 27; // Cofre simple
        } else if (skillService.hasSkill(uuid, Skill.BOLSILLOS_PROFUNDOS)) {
            return 9; // 1 fila
        }
        return 0; // Sin mochila
    }
    
    /**
     * Obtiene el título de la mochila según el nivel
     */
    private String getBackpackTitle(UUID uuid) {
        if (skillService.hasSkill(uuid, Skill.INVENTARIO_INFINITO)) {
            return "§6§l✦ §eInventario Infinito §6§l✦";
        } else if (skillService.hasSkill(uuid, Skill.BOLSILLOS_SIN_FONDO)) {
            return "§6§l✦ §eBolsillos Sin Fondo §6§l✦";
        } else if (skillService.hasSkill(uuid, Skill.BOLSILLOS_PROFUNDOS)) {
            return "§6§l✦ §eBolsillos Profundos §6§l✦";
        }
        return "§eMochila";
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
        if (!(inv.getHolder() instanceof BackpackHolder holder)) return;
        
        UUID uuid = holder.getOwner();
        
        // Guardar contenido
        ItemStack[] contents = new ItemStack[inv.getSize()];
        for (int i = 0; i < inv.getSize(); i++) {
            contents[i] = inv.getItem(i);
        }
        backpacks.put(uuid, contents);
        
        // Guardar a archivo
        saveBackpacks();
        
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.7f, 1.2f);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof BackpackHolder)) return;
        
        // Permitir todas las operaciones normales de inventario
        // No cancelamos nada aquí
    }
    
    // ==================== PERSISTENCIA ====================
    
    private void loadBackpacks() {
        if (!dataFile.exists()) return;
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection playersSection = config.getConfigurationSection("backpacks");
        
        if (playersSection == null) return;
        
        for (String uuidStr : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                List<?> itemsList = playersSection.getList(uuidStr);
                
                if (itemsList != null) {
                    ItemStack[] contents = new ItemStack[54]; // Máximo tamaño
                    for (int i = 0; i < itemsList.size() && i < 54; i++) {
                        Object item = itemsList.get(i);
                        if (item instanceof ItemStack) {
                            contents[i] = (ItemStack) item;
                        }
                    }
                    backpacks.put(uuid, contents);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error cargando mochila de " + uuidStr + ": " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Cargadas " + backpacks.size() + " mochilas.");
    }
    
    public void saveBackpacks() {
        FileConfiguration config = new YamlConfiguration();
        
        for (Map.Entry<UUID, ItemStack[]> entry : backpacks.entrySet()) {
            String path = "backpacks." + entry.getKey().toString();
            config.set(path, Arrays.asList(entry.getValue()));
        }
        
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando mochilas: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene el contenido de la mochila para un jugador
     */
    public ItemStack[] getBackpackContents(UUID uuid) {
        return backpacks.get(uuid);
    }
    
    /**
     * Verifica si un jugador tiene acceso a mochila
     */
    public boolean hasBackpack(UUID uuid) {
        return getBackpackSize(uuid) > 0;
    }
}
