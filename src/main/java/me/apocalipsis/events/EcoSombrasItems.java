package me.apocalipsis.events;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Sistema de items custom para el evento Eco de las Sombras Largas
 */
public class EcoSombrasItems {
    
    /**
     * Fragmento de Sombra - Drop de Sombras Largas
     * Usado para sellar Anclas del Mundo
     */
    public ItemStack crearFragmentoSombra() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("§8Fragmento de Sombra"));
            meta.lore(Arrays.asList(
                net.kyori.adventure.text.Component.text("§7Resto de una sombra larga."),
                net.kyori.adventure.text.Component.text("§7"),
                net.kyori.adventure.text.Component.text("§7Parece moverse si no lo miras."),
                net.kyori.adventure.text.Component.text("§8\"La sombra recuerda su forma.\"")
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
     * Eco Resonante - Recompensa del Guardián de la Sombra Larga
     * Item único y épico del evento
     */
    public ItemStack crearEcoResonante() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("§5§l✦ Eco Resonante ✦"));
            meta.lore(Arrays.asList(
                net.kyori.adventure.text.Component.text("§7Recompensa del Guardián"),
                net.kyori.adventure.text.Component.text("§7"),
                net.kyori.adventure.text.Component.text("§dUn fragmento de algo antiguo."),
                net.kyori.adventure.text.Component.text("§dResuena con memorias olvidadas."),
                net.kyori.adventure.text.Component.text("§7"),
                net.kyori.adventure.text.Component.text("§8\"El eco persiste tras el silencio.\""),
                net.kyori.adventure.text.Component.text("§8\"Lo que viene no tiene forma… aún.\"")
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
     * Verifica si un item es un Fragmento de Sombra
     */
    public boolean esFragmentoSombra(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        
        String displayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(meta.displayName());
        return displayName.contains("Fragmento de Sombra");
    }
    
    /**
     * Verifica si un item es un Eco Resonante
     */
    public boolean esEcoResonante(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        
        String displayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(meta.displayName());
        return displayName.contains("Eco Resonante");
    }
    
    /**
     * Cuenta cuántos Fragmentos de Sombra tiene un jugador
     */
    public int contarFragmentos(org.bukkit.entity.Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (esFragmentoSombra(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }
    
    /**
     * Consume una cantidad de Fragmentos de Sombra del inventario
     */
    public boolean consumirFragmentos(org.bukkit.entity.Player player, int cantidad) {
        if (contarFragmentos(player) < cantidad) {
            return false;
        }
        
        int restante = cantidad;
        for (ItemStack item : player.getInventory().getContents()) {
            if (esFragmentoSombra(item)) {
                int enStack = item.getAmount();
                if (enStack <= restante) {
                    item.setAmount(0);
                    restante -= enStack;
                } else {
                    item.setAmount(enStack - restante);
                    restante = 0;
                }
                
                if (restante == 0) {
                    break;
                }
            }
        }
        
        player.updateInventory();
        return true;
    }
}
