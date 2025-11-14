package me.apocalipsis.events;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Items custom para el evento Eco de Brasas
 */
public class EcoBrasasItems {
    
    // IDs persistentes para identificar items
    private static final String TAG_CENIZA = "eco_ceniza";
    private static final String TAG_FULGOR = "eco_fulgor";
    private static final String TAG_ECO_ROTO = "eco_eco_roto";
    private static final String TAG_EQUILIBRIO = "eco_equilibrio";
    private static final String TAG_LUZ_TEMPLADA = "eco_luz_templada";
    
    /**
     * Fragmento de Ceniza (60% drop rate)
     */
    public static ItemStack createCeniza(int cantidad) {
        ItemStack item = new ItemStack(Material.GUNPOWDER, cantidad);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(net.kyori.adventure.text.Component.text("§7Fragmento de Ceniza"));
        
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.text("§8Eco de Brasas"));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text("§7Resto de mundos quemados."));
        lore.add(net.kyori.adventure.text.Component.text("§7Aún late con calor residual."));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text("§8\"La ceniza recuerda lo que fue.\""));
        meta.lore(lore);
        
        // Enchant glow
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        // Tag persistente
        meta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey("apocalipsis", TAG_CENIZA),
            org.bukkit.persistence.PersistentDataType.BYTE,
            (byte) 1
        );
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Fragmento de Fulgor (25% drop rate)
     */
    public static ItemStack createFulgor(int cantidad) {
        ItemStack item = new ItemStack(Material.BLAZE_POWDER, cantidad);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(net.kyori.adventure.text.Component.text("§6Fragmento de Fulgor"));
        
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.text("§8Eco de Brasas"));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text("§7Brillo de un fuego que nunca"));
        lore.add(net.kyori.adventure.text.Component.text("§7se apagó completamente."));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text("§8\"El fulgor busca regresar.\""));
        meta.lore(lore);
        
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        meta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey("apocalipsis", TAG_FULGOR),
            org.bukkit.persistence.PersistentDataType.BYTE,
            (byte) 1
        );
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Eco Roto (6% drop rate)
     */
    public static ItemStack createEcoRoto(int cantidad) {
        ItemStack item = new ItemStack(Material.ECHO_SHARD, cantidad);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(net.kyori.adventure.text.Component.text("§5Eco Roto"));
        
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.text("§8Eco de Brasas"));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text("§7Fragmento de un susurro antiguo."));
        lore.add(net.kyori.adventure.text.Component.text("§7Ya no habla, pero aún resuena."));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text("§8\"Lo roto no olvida su forma.\""));
        meta.lore(lore);
        
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        meta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey("apocalipsis", TAG_ECO_ROTO),
            org.bukkit.persistence.PersistentDataType.BYTE,
            (byte) 1
        );
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Fragmento de Equilibrio (drop del minijefe)
     */
    public static ItemStack createEquilibrio(int cantidad) {
        ItemStack item = new ItemStack(Material.NETHER_STAR, cantidad);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(net.kyori.adventure.text.Component.text("§d§lFragmento de Equilibrio"));
        
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.text("§8Eco de Brasas"));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text("§7Condensado de las tres anclas."));
        lore.add(net.kyori.adventure.text.Component.text("§7El fuego encuentra su centro."));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text("§8\"Lo que arde en equilibrio, no consume.\""));
        meta.lore(lore);
        
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        meta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey("apocalipsis", TAG_EQUILIBRIO),
            org.bukkit.persistence.PersistentDataType.BYTE,
            (byte) 1
        );
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Luz Templada (reward final)
     */
    public static ItemStack createLuzTemplada(int cantidad) {
        ItemStack item = new ItemStack(Material.END_CRYSTAL, cantidad);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(net.kyori.adventure.text.Component.text("§e§l✦ Luz Templada ✦"));
        
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.text("§8Eco de Brasas - Recompensa Final"));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text("§7Energía estabilizada del portal."));
        lore.add(net.kyori.adventure.text.Component.text("§7El fuego cedió por hoy."));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text("§8\"Cada chispa que guardan,"));
        lore.add(net.kyori.adventure.text.Component.text("§8alguna vez ya ardió en otro mundo.\""));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text("§6Se usará en el evento de Navidad"));
        meta.lore(lore);
        
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        meta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey("apocalipsis", TAG_LUZ_TEMPLADA),
            org.bukkit.persistence.PersistentDataType.BYTE,
            (byte) 1
        );
        
        item.setItemMeta(meta);
        return item;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // VERIFICADORES
    // ═══════════════════════════════════════════════════════════════════
    
    public static boolean isCeniza(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(
            new org.bukkit.NamespacedKey("apocalipsis", TAG_CENIZA),
            org.bukkit.persistence.PersistentDataType.BYTE
        );
    }
    
    public static boolean isFulgor(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(
            new org.bukkit.NamespacedKey("apocalipsis", TAG_FULGOR),
            org.bukkit.persistence.PersistentDataType.BYTE
        );
    }
    
    public static boolean isEcoRoto(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(
            new org.bukkit.NamespacedKey("apocalipsis", TAG_ECO_ROTO),
            org.bukkit.persistence.PersistentDataType.BYTE
        );
    }
    
    public static boolean isEquilibrio(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(
            new org.bukkit.NamespacedKey("apocalipsis", TAG_EQUILIBRIO),
            org.bukkit.persistence.PersistentDataType.BYTE
        );
    }
    
    public static boolean isLuzTemplada(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(
            new org.bukkit.NamespacedKey("apocalipsis", TAG_LUZ_TEMPLADA),
            org.bukkit.persistence.PersistentDataType.BYTE
        );
    }
    
    /**
     * Cuenta cuántos items del tipo especificado tiene el jugador
     */
    public static int countFragments(org.bukkit.entity.Player player, String tipo) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            
            boolean match = false;
            switch (tipo) {
                case "ceniza": match = isCeniza(item); break;
                case "fulgor": match = isFulgor(item); break;
                case "eco_roto": match = isEcoRoto(item); break;
                case "equilibrio": match = isEquilibrio(item); break;
            }
            
            if (match) {
                count += item.getAmount();
            }
        }
        return count;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    //                    RECOMPENSAS PROGRESIVAS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Moneda de Brasa - Drop por cerrar grietas
     */
    public static ItemStack createMonedaBrasa(int cantidad) {
        ItemStack item = new ItemStack(Material.GOLD_NUGGET, cantidad);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(net.kyori.adventure.text.Component.text("§6Moneda de Brasa"));
        
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.text("§8Eco de Brasas"));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text("§7Una grieta cerrada."));
        lore.add(net.kyori.adventure.text.Component.text("§7Un fragmento de esperanza."));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text("§e✦ Recuerdo de Fase 1 ✦"));
        meta.lore(lore);
        
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        meta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey("apocalipsis", "eco_moneda_brasa"),
            org.bukkit.persistence.PersistentDataType.BYTE,
            (byte) 1
        );
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Cristal de Ancla - Drop por completar anclas
     */
    public static ItemStack createCristalAncla(int cantidad) {
        ItemStack item = new ItemStack(Material.PRISMARINE_SHARD, cantidad);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(net.kyori.adventure.text.Component.text("§dCristal de Ancla"));
        
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.text("§8Eco de Brasas"));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text("§7El ancla canalizó el caos."));
        lore.add(net.kyori.adventure.text.Component.text("§7Esto quedó cristalizado."));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text("§d✦ Recuerdo de Fase 2 ✦"));
        meta.lore(lore);
        
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        meta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey("apocalipsis", "eco_cristal_ancla"),
            org.bukkit.persistence.PersistentDataType.BYTE,
            (byte) 1
        );
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Espada del Guardián - Drop al derrotar al guardián (RARA)
     */
    public static ItemStack createEspadaGuardian() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(net.kyori.adventure.text.Component.text("§4Espada del Guardián Caído"));
        
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.text("§8Eco de Brasas - Fase 3"));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text("§7El Guardián protegía el altar."));
        lore.add(net.kyori.adventure.text.Component.text("§7Fue necesario detenerlo."));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text("§c✦ Solo quien lo derrotó la porta ✦"));
        meta.lore(lore);
        
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        meta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey("apocalipsis", "eco_espada_guardian"),
            org.bukkit.persistence.PersistentDataType.BYTE,
            (byte) 1
        );
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Emblema de Victoria - Recompensa de participación final
     */
    public static ItemStack createEmblemaVictoria() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(net.kyori.adventure.text.Component.text("§e§l✦ Emblema del Eco Templado ✦"));
        
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.text("§8Eco de Brasas - Victoria"));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text("§7El fuego primordial ardía sin control."));
        lore.add(net.kyori.adventure.text.Component.text("§7Amenazaba con consumirlo todo."));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text("§7Cerramos las grietas. Estabilizamos anclas."));
        lore.add(net.kyori.adventure.text.Component.text("§7El ritual transformó su furia en luz."));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text("§6El eco ahora late en paz."));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text("§e✦ Gracias por estar aquí ✦"));
        meta.lore(lore);
        
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 10, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        meta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey("apocalipsis", "eco_emblema_victoria"),
            org.bukkit.persistence.PersistentDataType.BYTE,
            (byte) 1
        );
        
        item.setItemMeta(meta);
        return item;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Consume items del inventario del jugador
     */
    public static boolean consumeFragments(org.bukkit.entity.Player player, String tipo, int cantidad) {
        int remaining = cantidad;
        
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null) continue;
            
            boolean match = false;
            switch (tipo) {
                case "ceniza": match = isCeniza(item); break;
                case "fulgor": match = isFulgor(item); break;
                case "eco_roto": match = isEcoRoto(item); break;
            }
            
            if (match) {
                int amount = item.getAmount();
                if (amount <= remaining) {
                    remaining -= amount;
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(amount - remaining);
                    remaining = 0;
                }
                
                if (remaining == 0) break;
            }
        }
        
        return remaining == 0;
    }
}
