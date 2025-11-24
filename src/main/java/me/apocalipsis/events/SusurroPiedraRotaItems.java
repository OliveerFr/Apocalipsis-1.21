package me.apocalipsis.events;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Items únicos del evento "El Susurro en la Piedra Rota"
 */
public class SusurroPiedraRotaItems {
    
    /**
     * Crea el Fragmento de Forma Desviada - Item único y permanente
     * Este item será clave para eventos futuros (End y finales).
     */
    public static ItemStack createNucleoForma() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD, 1);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Nombre
            meta.setDisplayName("§5§lFragmento de Forma Desviada");
            
            // Lore
            List<String> lore = new ArrayList<>();
            lore.add("§8El Susurro en la Piedra Rota");
            lore.add("");
            lore.add("§7Un núcleo que no debería existir.");
            lore.add("§7Vibra con una frecuencia incorrecta.");
            lore.add("§7Como si recordara una forma que nunca tuvo.");
            lore.add("");
            lore.add("§5\"La forma se deformó...\"");
            lore.add("§5\"pero dejó un núcleo tras de sí.\"");
            lore.add("");
            lore.add("§8✦ Item único y permanente");
            lore.add("§8✦ Será clave en eventos futuros");
            meta.setLore(lore);
            
            // Enchantment glow
            meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
            
            // Ocultar enchants
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            
            // Unbreakable (no se rompe)
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Verifica si un item es el Núcleo de Forma
     */
    public static boolean isNucleoForma(ItemStack item) {
        if (item == null || item.getType() != Material.ECHO_SHARD) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        
        return meta.getDisplayName().equals("§5§lFragmento de Forma Desviada");
    }
    
    /**
     * Obtiene el nombre display del núcleo
     */
    public static String getNucleoDisplayName() {
        return "§5§lFragmento de Forma Desviada";
    }
}
