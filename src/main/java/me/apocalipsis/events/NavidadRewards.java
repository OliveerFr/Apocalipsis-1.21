package me.apocalipsis.events;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Sistema de creación de items de recompensa del Evento de Navidad
 * Todos los items tienen temática navideña y son útiles para próximos eventos
 */
public class NavidadRewards {
    
    // ═══════════════════════════════════════════════════════════════════
    // HERRAMIENTAS ÚNICAS DEL INVIERNO
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Pico de Escarcha - Pico de diamante con Eficiencia V, Fortuna III, Irrompibilidad III
     */
    public static ItemStack crearPicoEscarcha() {
        ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§b§l❄ Pico de Escarcha §b§l❄");
        meta.setLore(Arrays.asList(
            "§7Forjado en las profundidades del invierno",
            "§7Herramienta de Navidad",
            "",
            "§9Eficiencia V",
            "§9Fortuna III",
            "§9Irrompibilidad III",
            "",
            "§8✦ Especial del Evento de Navidad"
        ));
        
        meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        meta.addEnchant(Enchantment.FORTUNE, 3, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Hacha de Santa - Hacha de diamante con Eficiencia V, Filo V, Irrompibilidad III
     */
    public static ItemStack crearHachaSanta() {
        ItemStack item = new ItemStack(Material.DIAMOND_AXE);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§c§l🎄 Hacha de Santa §c§l🎄");
        meta.setLore(Arrays.asList(
            "§7Cortó el árbol perfecto para Navidad",
            "§7Herramienta de Navidad",
            "",
            "§9Eficiencia V",
            "§9Filo V",
            "§9Irrompibilidad III",
            "",
            "§8✦ Especial del Evento de Navidad"
        ));
        
        meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Pala del Regalo - Pala de diamante con Eficiencia V, Fortuna III, Irrompibilidad III
     */
    public static ItemStack crearPalaRegalo() {
        ItemStack item = new ItemStack(Material.DIAMOND_SHOVEL);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§6§l⭐ Pala del Regalo §6§l⭐");
        meta.setLore(Arrays.asList(
            "§7Cava bajo la nieve para encontrar tesoros",
            "§7Herramienta de Navidad",
            "",
            "§9Eficiencia V",
            "§9Fortuna III",
            "§9Irrompibilidad III",
            "",
            "§8✦ Especial del Evento de Navidad"
        ));
        
        meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        meta.addEnchant(Enchantment.FORTUNE, 3, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Espada del Invierno Eterno - Espada de diamante con Filo V, Aspecto ígneo II, Empuje II, Irrompibilidad III
     */
    public static ItemStack crearEspadaInvierno() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§f§l❄ Espada del Invierno Eterno §f§l❄");
        meta.setLore(Arrays.asList(
            "§7Forjada en la paz del invierno",
            "§7Arma de Navidad",
            "",
            "§9Filo V",
            "§9Aspecto ígneo II",
            "§9Empuje II",
            "§9Irrompibilidad III",
            "",
            "§8✦ Especial del Evento de Navidad"
        ));
        
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
        meta.addEnchant(Enchantment.KNOCKBACK, 2, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        item.setItemMeta(meta);
        return item;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ARMADURA DE SANTA - Set completo
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Gorro de Santa - Casco de diamante con Protección IV, Respiración III, Irrompibilidad III
     */
    public static ItemStack crearGorroSanta() {
        ItemStack item = new ItemStack(Material.DIAMOND_HELMET);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§c§l🎅 Gorro de Santa §c§l🎅");
        meta.setLore(Arrays.asList(
            "§7El legendario gorro rojo de Santa",
            "§7Armadura de Navidad",
            "",
            "§9Protección IV",
            "§9Respiración III",
            "§9Irrompibilidad III",
            "",
            "§8✦ Especial del Evento de Navidad"
        ));
        
        meta.addEnchant(Enchantment.PROTECTION, 4, true);
        meta.addEnchant(Enchantment.RESPIRATION, 3, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Traje de Santa - Pechera de diamante con Protección IV, Espinas III, Irrompibilidad III
     */
    public static ItemStack crearTrajeSanta() {
        ItemStack item = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§c§l🎁 Traje de Santa §c§l🎁");
        meta.setLore(Arrays.asList(
            "§7El legendario traje rojo de Santa",
            "§7Armadura de Navidad",
            "",
            "§9Protección IV",
            "§9Espinas III",
            "§9Irrompibilidad III",
            "",
            "§8✦ Especial del Evento de Navidad"
        ));
        
        meta.addEnchant(Enchantment.PROTECTION, 4, true);
        meta.addEnchant(Enchantment.THORNS, 3, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Pantalones de Santa - Pantalones de diamante con Protección IV, Irrompibilidad III
     */
    public static ItemStack crearPantalonesSanta() {
        ItemStack item = new ItemStack(Material.DIAMOND_LEGGINGS);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§c§l🎄 Pantalones de Santa §c§l🎄");
        meta.setLore(Arrays.asList(
            "§7Los legendarios pantalones rojos de Santa",
            "§7Armadura de Navidad",
            "",
            "§9Protección IV",
            "§9Irrompibilidad III",
            "",
            "§8✦ Especial del Evento de Navidad"
        ));
        
        meta.addEnchant(Enchantment.PROTECTION, 4, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Botas de Santa - Botas de diamante con Protección IV, Caída de pluma IV, Irrompibilidad III
     */
    public static ItemStack crearBotasSanta() {
        ItemStack item = new ItemStack(Material.DIAMOND_BOOTS);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§c§l❄ Botas de Santa §c§l❄");
        meta.setLore(Arrays.asList(
            "§7Las legendarias botas negras de Santa",
            "§7Armadura de Navidad",
            "",
            "§9Protección IV",
            "§9Caída de pluma IV",
            "§9Irrompibilidad III",
            "",
            "§8✦ Especial del Evento de Navidad"
        ));
        
        meta.addEnchant(Enchantment.PROTECTION, 4, true);
        meta.addEnchant(Enchantment.FEATHER_FALLING, 4, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        
        item.setItemMeta(meta);
        return item;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // MÉTODO PARA OBTENER TODAS LAS RECOMPENSAS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Obtiene todas las recompensas del evento de Navidad
     * Incluye: herramientas únicas, armadura de Santa y materiales útiles
     */
    public static List<ItemStack> obtenerTodasLasRecompensas() {
        List<ItemStack> recompensas = new ArrayList<>();
        
        // ═══════════════════════════════════════════════════════════
        // HERRAMIENTAS ÚNICAS
        // ═══════════════════════════════════════════════════════════
        recompensas.add(crearPicoEscarcha());
        recompensas.add(crearHachaSanta());
        recompensas.add(crearPalaRegalo());
        recompensas.add(crearEspadaInvierno());
        
        // ═══════════════════════════════════════════════════════════
        // ARMADURA DE SANTA - Set completo
        // ═══════════════════════════════════════════════════════════
        recompensas.add(crearGorroSanta());
        recompensas.add(crearTrajeSanta());
        recompensas.add(crearPantalonesSanta());
        recompensas.add(crearBotasSanta());
        
        // ═══════════════════════════════════════════════════════════
        // MATERIALES ÚTILES - Para próximos eventos
        // ═══════════════════════════════════════════════════════════
        
        // Materiales valiosos
        recompensas.add(new ItemStack(Material.DIAMOND, 16));
        recompensas.add(new ItemStack(Material.EMERALD, 12));
        recompensas.add(new ItemStack(Material.GOLD_INGOT, 24));
        recompensas.add(new ItemStack(Material.IRON_INGOT, 48));
        recompensas.add(new ItemStack(Material.ANCIENT_DEBRIS, 5));
        recompensas.add(new ItemStack(Material.NETHERITE_SCRAP, 4));
        recompensas.add(new ItemStack(Material.NETHERITE_INGOT, 1));
        
        // Items de supervivencia
        recompensas.add(new ItemStack(Material.GOLDEN_APPLE, 10));
        recompensas.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 3));
        recompensas.add(new ItemStack(Material.TOTEM_OF_UNDYING, 2));
        recompensas.add(new ItemStack(Material.ENDER_PEARL, 32));
        recompensas.add(new ItemStack(Material.EXPERIENCE_BOTTLE, 32));
        
        // Bloques decorativos/temáticos
        recompensas.add(new ItemStack(Material.SNOW_BLOCK, 64));
        recompensas.add(new ItemStack(Material.ICE, 32));
        recompensas.add(new ItemStack(Material.PACKED_ICE, 32));
        recompensas.add(new ItemStack(Material.BLUE_ICE, 32));
        recompensas.add(new ItemStack(Material.SEA_LANTERN, 24));
        recompensas.add(new ItemStack(Material.GLOWSTONE, 48));
        
        // Comida temática
        recompensas.add(new ItemStack(Material.CAKE, 8));
        recompensas.add(new ItemStack(Material.COOKIE, 64));
        recompensas.add(new ItemStack(Material.PUMPKIN_PIE, 32));
        
        return recompensas;
    }
}
