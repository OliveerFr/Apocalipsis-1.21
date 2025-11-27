package me.apocalipsis.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

/**
 * Items únicos del evento "El Susurro en la Piedra Rota"
 * Sistema de recompensas dinámicas basado en participación y dificultad
 */
public class SusurroPiedraRotaItems {
    
    // ═══════════════════════════════════════════════════════════════════
    // ITEM PRINCIPAL DEL EVENTO
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Crea el Fragmento de Forma Desviada - Item único y permanente
     * Este item será clave para eventos futuros (End y finales).
     */
    public static ItemStack createNucleoForma() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD, 1);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§5§lFragmento de Forma Desviada");
            
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
            
            meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE RECOMPENSAS DINÁMICAS
    // ═══════════════════════════════════════════════════════════════════
    
    public enum RangoRecompensa {
        PLATINUM("§b§l✧ PLATINUM ✧", 4),  // Mejor rendimiento
        GOLD("§6§l★ GOLD ★", 3),          // Excelente
        SILVER("§7§l☆ SILVER ☆", 2),      // Bueno
        BRONZE("§c§l◆ BRONZE ◆", 1);      // Participó
        
        public final String nombre;
        public final int nivel;
        
        RangoRecompensa(String nombre, int nivel) {
            this.nombre = nombre;
            this.nivel = nivel;
        }
    }
    
    /**
     * Genera recompensas dinámicas basadas en el rango del jugador
     */
    // ✨ Contador estático para garantizar casco y peto primero
    private static int contadorArmadura = 0;
    
    public static List<ItemStack> generarRecompensas(RangoRecompensa rango, boolean recogioNucleo, int fragmentosVisitados) {
        List<ItemStack> recompensas = new ArrayList<>();
        Random rand = new Random();
        
        // ✨ RESETEAR contador para cada jugador
        contadorArmadura = 0;
        
        // === ARMADURA ÚNICA DEL EVENTO ===
        // ✨ MEJORADO: Primera pieza SIEMPRE es CASCO, segunda SIEMPRE es PETO
        recompensas.add(generarArmaduraEco(rango, rand));
        
        // PLATINUM y GOLD reciben 2 piezas de armadura (peto garantizado)
        if (rango.nivel >= 3) {
            recompensas.add(generarArmaduraEco(rango, rand));
        }
        
        // === ARMA ESPECIAL ===
        // Solo PLATINUM y GOLD reciben arma
        if (rango.nivel >= 3) {
            recompensas.add(generarArmaEco(rango, rand));
        }
        
        // === ITEMS CONSUMIBLES NARRATIVOS ===
        recompensas.add(generarPociónEco(rango));
        
        // === MATERIALES VALIOSOS ===
        recompensas.addAll(generarMateriales(rango, rand));
        
        // === BONUS POR RECOGER NÚCLEO ===
        if (recogioNucleo) {
            recompensas.add(crearReliquiaObservador());
        }
        
        // === BONUS POR VISITAR MUCHOS FRAGMENTOS ===
        if (fragmentosVisitados >= 4) {
            recompensas.add(crearAmuletoMemoria());
        }
        
        return recompensas;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ARMADURAS DEL ECO - Únicas del evento
    // ═══════════════════════════════════════════════════════════════════
    
    private static ItemStack generarArmaduraEco(RangoRecompensa rango, Random rand) {
        // Elegir pieza - ✨ MEJORADO: Casco primero, luego peto, luego aleatorio
        Material[] piezas;
        String[] nombres;
        
        // Rango determina material base
        if (rango.nivel >= 4) { // PLATINUM - Netherite
            piezas = new Material[]{Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, 
                                    Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS};
            nombres = new String[]{"Yelmo", "Coraza", "Grebas", "Botas"};
        } else if (rango.nivel >= 3) { // GOLD - Diamante
            piezas = new Material[]{Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, 
                                    Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS};
            nombres = new String[]{"Yelmo", "Coraza", "Grebas", "Botas"};
        } else if (rango.nivel >= 2) { // SILVER - Hierro
            piezas = new Material[]{Material.IRON_HELMET, Material.IRON_CHESTPLATE, 
                                    Material.IRON_LEGGINGS, Material.IRON_BOOTS};
            nombres = new String[]{"Yelmo", "Coraza", "Grebas", "Botas"};
        } else { // BRONZE - Cuero teñido morado
            return crearArmaduraCueroEco(rand);
        }
        
        // ✨ NUEVO: Garantizar orden Casco → Peto → Aleatorio
        int idx;
        if (contadorArmadura == 0) {
            idx = 0; // Casco (HELMET)
        } else if (contadorArmadura == 1) {
            idx = 1; // Peto (CHESTPLATE)
        } else {
            // A partir de la tercera pieza, aleatorio entre grebas y botas
            idx = 2 + rand.nextInt(2); // 2 o 3
        }
        contadorArmadura++;
        
        ItemStack item = new ItemStack(piezas[idx]);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Nombre épico
            meta.setDisplayName("§5§l" + nombres[idx] + " del Eco Roto");
            
            // Lore narrativo
            List<String> lore = new ArrayList<>();
            lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━");
            lore.add("");
            lore.add("§7Forjado en el vacío entre dimensiones,");
            lore.add("§7este " + nombres[idx].toLowerCase() + " retiene la esencia");
            lore.add("§7de la Forma que casi despertó.");
            lore.add("");
            lore.add("§5\"El Observador sonríe...\"");
            lore.add("§5\"pero nunca explica por qué.\"");
            lore.add("");
            lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━");
            lore.add("");
            lore.add("§d✦ El Susurro en la Piedra Rota");
            lore.add("§8Rango: " + rango.nombre);
            meta.setLore(lore);
            
            // Encantamientos según rango
            int nivelProteccion = rango.nivel; // 1-4
            meta.addEnchant(Enchantment.PROTECTION, nivelProteccion, true);
            
            if (rango.nivel >= 3) {
                meta.addEnchant(Enchantment.UNBREAKING, rango.nivel - 1, true);
            }
            if (rango.nivel >= 4) {
                meta.addEnchant(Enchantment.MENDING, 1, true);
            }
            
            meta.setUnbreakable(false);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private static ItemStack crearArmaduraCueroEco(Random rand) {
        Material[] piezas = {Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, 
                            Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS};
        String[] nombres = {"Capucha", "Túnica", "Pantalones", "Sandalias"};
        
        int idx = rand.nextInt(piezas.length);
        ItemStack item = new ItemStack(piezas[idx]);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        
        if (meta != null) {
            // Color morado/violeta del eco
            meta.setColor(Color.fromRGB(138, 43, 226)); // Púrpura
            
            meta.setDisplayName("§5" + nombres[idx] + " del Peregrino Roto");
            
            List<String> lore = new ArrayList<>();
            lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━");
            lore.add("");
            lore.add("§7Vestimenta de aquellos que");
            lore.add("§7presenciaron el Susurro.");
            lore.add("");
            lore.add("§5\"No todos sobreviven...\"");
            lore.add("§5\"pero los que lo hacen, recuerdan.\"");
            lore.add("");
            lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━");
            lore.add("");
            lore.add("§d✦ El Susurro en la Piedra Rota");
            lore.add("§8Rango: §c§l◆ BRONZE ◆");
            meta.setLore(lore);
            
            meta.addEnchant(Enchantment.PROTECTION, 2, true);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ARMAS DEL ECO - Solo para rangos altos
    // ═══════════════════════════════════════════════════════════════════
    
    private static ItemStack generarArmaEco(RangoRecompensa rango, Random rand) {
        boolean esEspada = rand.nextBoolean();
        
        Material material;
        if (rango.nivel >= 4) {
            material = esEspada ? Material.NETHERITE_SWORD : Material.NETHERITE_AXE;
        } else {
            material = esEspada ? Material.DIAMOND_SWORD : Material.DIAMOND_AXE;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String tipoArma = esEspada ? "Espada" : "Hacha";
            meta.setDisplayName("§5§l" + tipoArma + " del Vacío Recordado");
            
            List<String> lore = new ArrayList<>();
            lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━");
            lore.add("");
            lore.add("§7Un arma que corta no solo carne,");
            lore.add("§7sino también los hilos de la realidad.");
            lore.add("§7Cada golpe resuena en el vacío.");
            lore.add("");
            lore.add("§5\"La Forma quiso destruir...\"");
            lore.add("§5\"ahora su fuerza te pertenece.\"");
            lore.add("");
            lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━");
            lore.add("");
            lore.add("§d✦ El Susurro en la Piedra Rota");
            lore.add("§8Rango: " + rango.nombre);
            meta.setLore(lore);
            
            // Encantamientos
            meta.addEnchant(Enchantment.SHARPNESS, rango.nivel, true);
            meta.addEnchant(Enchantment.UNBREAKING, rango.nivel - 1, true);
            
            if (rango.nivel >= 4) {
                meta.addEnchant(Enchantment.LOOTING, 2, true);
                meta.addEnchant(Enchantment.MENDING, 1, true);
            }
            
            if (esEspada && rango.nivel >= 3) {
                meta.addEnchant(Enchantment.SWEEPING_EDGE, 2, true);
            }
            
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // POCIONES Y CONSUMIBLES NARRATIVOS
    // ═══════════════════════════════════════════════════════════════════
    
    private static ItemStack generarPociónEco(RangoRecompensa rango) {
        ItemStack item = new ItemStack(Material.DRAGON_BREATH, rango.nivel);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§5§lAliento del Observador");
            
            List<String> lore = new ArrayList<>();
            lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━");
            lore.add("");
            lore.add("§7El aliento congelado del ser");
            lore.add("§7que vigila desde el otro lado.");
            lore.add("");
            lore.add("§e⚗ Úsalo para crear pociones");
            lore.add("§e⚗ de Lingering extraordinarias.");
            lore.add("");
            lore.add("§5\"¿Escuchas su respiración?\"");
            lore.add("");
            lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━");
            lore.add("§d✦ El Susurro en la Piedra Rota");
            meta.setLore(lore);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // MATERIALES VALIOSOS
    // ═══════════════════════════════════════════════════════════════════
    
    private static List<ItemStack> generarMateriales(RangoRecompensa rango, Random rand) {
        List<ItemStack> materiales = new ArrayList<>();
        
        // Echo Shards - más para mejor rango
        int echoShards = rango.nivel + rand.nextInt(3);
        ItemStack echos = new ItemStack(Material.ECHO_SHARD, echoShards);
        ItemMeta echoMeta = echos.getItemMeta();
        if (echoMeta != null) {
            echoMeta.setDisplayName("§5Fragmento de Eco Puro");
            List<String> lore = new ArrayList<>();
            lore.add("§7Resonancia cristalizada del evento.");
            lore.add("§8Útil para crafting especial.");
            echoMeta.setLore(lore);
            echos.setItemMeta(echoMeta);
        }
        materiales.add(echos);
        
        // Diamantes para rangos altos
        if (rango.nivel >= 3) {
            int diamantes = rango.nivel + rand.nextInt(4);
            materiales.add(new ItemStack(Material.DIAMOND, diamantes));
        }
        
        // Netherite Scraps solo para PLATINUM
        if (rango.nivel >= 4) {
            int scraps = 1 + rand.nextInt(2);
            ItemStack netheriteScrap = new ItemStack(Material.NETHERITE_SCRAP, scraps);
            ItemMeta scrapMeta = netheriteScrap.getItemMeta();
            if (scrapMeta != null) {
                scrapMeta.setDisplayName("§5Escoria del Vacío");
                List<String> lore = new ArrayList<>();
                lore.add("§7Netherite infundido con esencia");
                lore.add("§7del espacio entre realidades.");
                lore.add("");
                lore.add("§5\"Más fuerte que lo ordinario...\"");
                scrapMeta.setLore(lore);
                netheriteScrap.setItemMeta(scrapMeta);
            }
            materiales.add(netheriteScrap);
        }
        
        // Golden Apples
        int manzanas = rango.nivel;
        materiales.add(new ItemStack(Material.GOLDEN_APPLE, manzanas));
        
        // Totems para GOLD y PLATINUM
        if (rango.nivel >= 3) {
            materiales.add(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
        }
        
        return materiales;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // RELIQUIAS ESPECIALES - Bonus por logros
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Reliquia especial para quien recogió el núcleo
     */
    private static ItemStack crearReliquiaObservador() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§d§l✧ Ojo del Observador ✧");
            
            List<String> lore = new ArrayList<>();
            lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━");
            lore.add("");
            lore.add("§7Un fragmento de la consciencia");
            lore.add("§7del ser que vigila el vacío.");
            lore.add("");
            lore.add("§7Fuiste tú quien tocó el núcleo.");
            lore.add("§7Ahora, §lél te ha visto§7.");
            lore.add("");
            lore.add("§5\"No mires atrás...\"");
            lore.add("§5\"ya es demasiado tarde para eso.\"");
            lore.add("");
            lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━");
            lore.add("");
            lore.add("§e✦ RELIQUIA ÚNICA");
            lore.add("§d✦ El Susurro en la Piedra Rota");
            lore.add("§8Otorgada por recoger el Núcleo");
            meta.setLore(lore);
            
            meta.addEnchant(Enchantment.FORTUNE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Amuleto para exploradores que visitaron muchos fragmentos
     */
    private static ItemStack crearAmuletoMemoria() {
        ItemStack item = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§b§l✧ Amuleto de la Memoria Rota ✧");
            
            List<String> lore = new ArrayList<>();
            lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━");
            lore.add("");
            lore.add("§7Visitaste casi todos los fragmentos.");
            lore.add("§7Tu curiosidad despertó algo...");
            lore.add("§7o tal vez solo lo hiciste recordar.");
            lore.add("");
            lore.add("§5\"Los exploradores son peligrosos.\"");
            lore.add("§5\"Descubren lo que debería olvidarse.\"");
            lore.add("");
            lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━");
            lore.add("");
            lore.add("§e✦ RELIQUIA ÚNICA");
            lore.add("§d✦ El Susurro en la Piedra Rota");
            lore.add("§8Por visitar 4+ fragmentos");
            meta.setLore(lore);
            
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // VERIFICADORES
    // ═══════════════════════════════════════════════════════════════════
    
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
    
    public static String getNucleoDisplayName() {
        return "§5§lFragmento de Forma Desviada";
    }
}
