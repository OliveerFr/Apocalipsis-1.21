package me.apocalipsis.events;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.apocalipsis.Apocalipsis;

/**
 * Items especiales del Evento Navidad
 * 
 * Define y crea items únicos:
 * - Fragmentos de Recuerdo (misteriosos, sin explicar su uso)
 * - Items de regalo (opcionales)
 */
public class NavidadItems {
    
    private final Apocalipsis plugin;
    private ConfigurationSection config;
    
    public NavidadItems(Apocalipsis plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    private void loadConfig() {
        config = plugin.getConfigManager().getNavidadConfig()
            .getConfigurationSection("navidad.fragmentos");
        
        if (config == null) {
            plugin.getLogger().warning("[NavidadItems] Configuración no encontrada en navidad.yml");
        }
    }
    
    /**
     * Crea un Fragmento de Recuerdo
     * @param cantidad Cantidad de fragmentos a crear
     * @return ItemStack del fragmento
     */
    public ItemStack crearFragmentoRecuerdo(int cantidad) {
        if (config == null) {
            plugin.getLogger().warning("[NavidadItems] Config no cargada, usando valores por defecto");
            return crearFragmentoDefault(cantidad);
        }
        
        String materialStr = config.getString("material", "AMETHYST_SHARD");
        Material material;
        try {
            material = Material.valueOf(materialStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[NavidadItems] Material inválido: " + materialStr + ", usando AMETHYST_SHARD");
            material = Material.AMETHYST_SHARD;
        }
        
        ItemStack item = new ItemStack(material, cantidad);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Nombre
            String nombre = config.getString("nombre", "§d✦ Fragmento de Recuerdo ✦");
            meta.setDisplayName(nombre);
            
            // Lore
            List<String> lore = config.getStringList("lore");
            if (lore.isEmpty()) {
                lore = new ArrayList<>();
                lore.add("§8Navidad");
                lore.add("");
                lore.add("§7Un fragmento que resuena...");
                lore.add("§7No explica su propósito.");
                lore.add("");
                lore.add("§8\"Algunos recuerdos pesan más que otros.\"");
            }
            meta.setLore(lore);
            
            // Glow effect
            boolean glow = config.getBoolean("glow", true);
            if (glow) {
                meta.addEnchant(Enchantment.FORTUNE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Crea un fragmento con valores por defecto (fallback)
     */
    private ItemStack crearFragmentoDefault(int cantidad) {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD, cantidad);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§d✦ Fragmento de Recuerdo ✦");
            
            List<String> lore = new ArrayList<>();
            lore.add("§8Navidad");
            lore.add("");
            lore.add("§7Un fragmento que resuena...");
            lore.add("§7No explica su propósito.");
            lore.add("");
            lore.add("§8\"Algunos recuerdos pesan más que otros.\"");
            meta.setLore(lore);
            
            meta.addEnchant(Enchantment.FORTUNE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Verifica si un item es un Fragmento de Recuerdo
     */
    public boolean esFragmentoRecuerdo(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) {
            return false;
        }
        
        String nombre = meta.getDisplayName();
        return nombre.contains("Fragmento de Recuerdo");
    }
    
    /**
     * Crea un item de regalo simple
     * @param material Material del regalo
     * @param nombre Nombre del regalo
     * @param cantidad Cantidad
     * @return ItemStack del regalo
     */
    public ItemStack crearRegalo(Material material, String nombre, int cantidad) {
        ItemStack item = new ItemStack(material, cantidad);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§a✦ " + nombre + " ✦");
            
            List<String> lore = new ArrayList<>();
            lore.add("§8Regalo de Navidad");
            lore.add("");
            lore.add("§7Un regalo especial del evento.");
            meta.setLore(lore);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
