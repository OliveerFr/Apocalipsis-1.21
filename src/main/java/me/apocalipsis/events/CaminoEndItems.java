package me.apocalipsis.events;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.apocalipsis.Apocalipsis;

/**
 * Sistema de items custom para el evento El Camino al End
 */
public class CaminoEndItems {
    
    private final Apocalipsis plugin;
    
    public CaminoEndItems(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Fragmento del Vacío - Item recolectable de anomalías
     * Usado para rastrear progreso del evento
     */
    public ItemStack crearFragmentoDelVacio() {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
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
     * Marca del Observador - Recompensa final del evento
     * Item único y especial
     */
    public ItemStack crearMarcaDelObservador() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("§5§l⚡ Marca del Observador"));
            meta.lore(Arrays.asList(
                net.kyori.adventure.text.Component.text("§7Recompensa del Camino al End"),
                net.kyori.adventure.text.Component.text("§7"),
                net.kyori.adventure.text.Component.text("§dUna marca dimensional que prueba"),
                net.kyori.adventure.text.Component.text("§dque exploraste lo desconocido."),
                net.kyori.adventure.text.Component.text("§7"),
                net.kyori.adventure.text.Component.text("§7El Observador reconoce tu valentía"),
                net.kyori.adventure.text.Component.text("§7al acercarte a lo que no debería estar."),
                net.kyori.adventure.text.Component.text("§7"),
                net.kyori.adventure.text.Component.text("§5§o\"El camino existe... aunque incompleto.\"")
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
     * Brújula del Vacío - Item de navegación hacia anomalías cercanas
     * (Opcional, para implementación futura)
     */
    public ItemStack crearBrujulaDelVacio() {
        ItemStack item = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("§5Brújula del Vacío"));
            meta.lore(Arrays.asList(
                net.kyori.adventure.text.Component.text("§7Una brújula que apunta hacia"),
                net.kyori.adventure.text.Component.text("§7las anomalías dimensionales."),
                net.kyori.adventure.text.Component.text("§7"),
                net.kyori.adventure.text.Component.text("§7Gira erráticamente cuando"),
                net.kyori.adventure.text.Component.text("§7hay una anomalía cerca."),
                net.kyori.adventure.text.Component.text("§7"),
                net.kyori.adventure.text.Component.text("§5§o\"Busca lo que no pertenece...\"")
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
     * Verifica si un item es un Fragmento del Vacío
     */
    public boolean esFragmentoDelVacio(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        
        String displayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(meta.displayName());
        return displayName.contains("Fragmento del Vacío");
    }
    
    /**
     * Verifica si un item es una Marca del Observador
     */
    public boolean esMarcaDelObservador(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        
        String displayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(meta.displayName());
        return displayName.contains("Marca del Observador");
    }
    
    /**
     * Verifica si un item es una Brújula del Vacío
     */
    public boolean esBrujulaDelVacio(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        
        String displayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(meta.displayName());
        return displayName.contains("Brújula del Vacío");
    }
}
