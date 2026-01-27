package me.apocalipsis.gui;

import me.apocalipsis.Apocalipsis;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Menú GUI para navegación de ciclos
 */
public class CicloMenuGUI implements InventoryHolder {
    
    private final Apocalipsis plugin;
    private final Inventory inventory;
    private final Player player;
    
    public CicloMenuGUI(Apocalipsis plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, "§b§lCICLOS MULTI-MUNDO");
        
        loadCycles();
    }
    
    /**
     * Carga los ciclos disponibles en el inventario
     */
    private void loadCycles() {
        FileConfiguration config = plugin.getCicloConfig();
        ConfigurationSection ciclosSection = config.getConfigurationSection("ciclos");
        
        if (ciclosSection == null) {
            // No hay ciclos, mostrar mensaje
            ItemStack noData = createItem(Material.BARRIER, "§c§lNo hay ciclos disponibles",
                "§7Crea un nuevo ciclo con",
                "§e/ciclo nuevo");
            inventory.setItem(22, noData);
            return;
        }
        
        Set<String> ciclos = ciclosSection.getKeys(false);
        int slot = 10;
        
        for (String cicloName : ciclos) {
            if (slot >= 44) break; // Límite de slots
            
            // Saltar slots decorativos
            if ((slot + 1) % 9 == 0 || slot % 9 == 0) {
                slot++;
            }
            
            ConfigurationSection ciclo = ciclosSection.getConfigurationSection(cicloName);
            if (ciclo == null) continue;
            
            // Obtener datos del ciclo
            boolean activo = ciclo.getBoolean("activo", false);
            String fecha = ciclo.getString("fecha_creacion", "Desconocida");
            String descripcion = ciclo.getString("descripcion", "Sin descripción");
            
            // Contar jugadores en el mundo
            World world = Bukkit.getWorld(cicloName);
            int jugadores = world != null ? world.getPlayers().size() : 0;
            
            // Crear ítem
            Material material = activo ? Material.EMERALD_BLOCK : Material.GRASS_BLOCK;
            ItemStack item = createItem(material, 
                (activo ? "§a§l" : "§7") + cicloName,
                "§7" + descripcion,
                "",
                "§bJugadores: §f" + jugadores,
                "§bCreado: §f" + fecha,
                "§bEstado: " + (activo ? "§a✓ Activo" : "§7○ Inactivo"),
                "",
                activo ? "§a§l→ YA ESTÁS AQUÍ" : "§e§l» Click para viajar"
            );
            
            inventory.setItem(slot, item);
            slot++;
        }
        
        // Ítem para crear nuevo ciclo
        ItemStack crearNuevo = createItem(Material.NETHER_STAR, "§a§l+ CREAR NUEVO CICLO",
            "§7Inicia el proceso de creación",
            "§7de un nuevo ciclo multi-mundo",
            "",
            "§e§l» Click para crear");
        inventory.setItem(49, crearNuevo);
        
        // Ítem de información
        ItemStack info = createItem(Material.BOOK, "§b§lINFORMACIÓN",
            "§7Sistema de ciclos multi-mundo",
            "§7para gestión de temporadas.",
            "",
            "§7Cada ciclo es un mundo separado",
            "§7con progresión independiente.",
            "",
            "§aRangos y habilidades: §fGLOBALES",
            "§cXP y PS: §fPor ciclo");
        inventory.setItem(45, info);
        
        // Decoración
        fillBorders();
    }
    
    /**
     * Crea un ítem con nombre y lore
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.setLore(loreList);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Rellena los bordes con cristales grises
     */
    private void fillBorders() {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            border.setItemMeta(meta);
        }
        
        // Fila superior e inferior
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 45, border);
        }
        
        // Columnas laterales
        for (int i = 1; i < 5; i++) {
            inventory.setItem(i * 9, border);
            inventory.setItem(i * 9 + 8, border);
        }
    }
    
    /**
     * Abre el menú para el jugador
     */
    public void open() {
        player.openInventory(inventory);
    }
    
    /**
     * Maneja el click en un ítem
     */
    public void handleClick(ItemStack clickedItem, int slot) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        Material type = clickedItem.getType();
        
        // Crear nuevo ciclo
        if (type == Material.NETHER_STAR) {
            player.closeInventory();
            player.performCommand("ciclo nuevo");
            return;
        }
        
        // Viajar a ciclo
        if (type == Material.GRASS_BLOCK || type == Material.EMERALD_BLOCK) {
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = meta.getDisplayName();
                // Remover formato
                String cicloName = displayName.replaceAll("§[0-9a-fk-or]", "");
                
                player.closeInventory();
                player.performCommand("ciclo cambiar " + cicloName);
            }
            return;
        }
        
        // Información - no hacer nada, solo mostrar
        if (type == Material.BOOK) {
            return;
        }
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
