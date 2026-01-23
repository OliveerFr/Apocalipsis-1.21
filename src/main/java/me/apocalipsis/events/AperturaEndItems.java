package me.apocalipsis.events;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.apocalipsis.Apocalipsis;

/**
 * Sistema de items custom para el evento La Apertura del End
 */
public class AperturaEndItems {
    
    private final Apocalipsis plugin;
    
    public AperturaEndItems(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Fragmento del Vacío - Item recolectable de anomalías
     * Usado para rastrear progreso del evento
     */
    public ItemStack crearFragmentoDelVacio(int cantidad) {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD, cantidad);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("§5Fragmento del Vacío"));
            meta.lore(Arrays.asList(
                net.kyori.adventure.text.Component.text("§7Un cristal que resuena con"),
                net.kyori.adventure.text.Component.text("§7energía dimensional extraña."),
                net.kyori.adventure.text.Component.text("§7"),
                net.kyori.adventure.text.Component.text("§7Emite un zumbido suave y"),
                net.kyori.adventure.text.Component.text("§7partículas púrpuras ocasionales."),
                net.kyori.adventure.text.Component.text("§7"),
                net.kyori.adventure.text.Component.text("§5§o\"Algo llama desde el vacío...\"")
            ));
            
            // Glow effect
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Escama Perfecta - Escama de dragón del End
     * Item épico de recompensa
     */
    public ItemStack crearEscamaPerfecta(int cantidad) {
        ItemStack item = new ItemStack(Material.DRAGON_BREATH, cantidad);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("§5§l⚡ Escama Perfecta"));
            meta.lore(Arrays.asList(
                net.kyori.adventure.text.Component.text("§7Una escama del Dragón del End"),
                net.kyori.adventure.text.Component.text("§7perfectamente conservada."),
                net.kyori.adventure.text.Component.text("§7"),
                net.kyori.adventure.text.Component.text("§dEn ella se refleja un poder"),
                net.kyori.adventure.text.Component.text("§dque ha trascendido dimensiones."),
                net.kyori.adventure.text.Component.text("§7"),
                net.kyori.adventure.text.Component.text("§5§o\"La esencia del End cristalizada\"")
            ));
            
            // Glow effect
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Corazón Desolador - Corazón místico del dragón
     * Item legendario único
     */
    public ItemStack crearCorazonDesolador() {
        ItemStack item = new ItemStack(Material.DRAGON_EGG);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("§5§l§k||§r §5§l⚡ Corazón Desolador §k||"));
            meta.lore(Arrays.asList(
                net.kyori.adventure.text.Component.text("§7El núcleo palpitante del"),
                net.kyori.adventure.text.Component.text("§7Dragón que protegía el End."),
                net.kyori.adventure.text.Component.text("§7"),
                net.kyori.adventure.text.Component.text("§dContiene poder dimensional"),
                net.kyori.adventure.text.Component.text("§dincomprensible para los mortales."),
                net.kyori.adventure.text.Component.text("§7"),
                net.kyori.adventure.text.Component.text("§c§lITEM LEGENDARIO"),
                net.kyori.adventure.text.Component.text("§7"),
                net.kyori.adventure.text.Component.text("§5§o\"El latido que resuena en el vacío\"")
            ));
            
            // Glow effect
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
