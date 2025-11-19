package me.apocalipsis.events.gameplay;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * Sistema de lore progresivo que revela fragmentos de historia
 * a medida que los jugadores avanzan en el evento.
 */
public class LoreSystem {
    
    private final Plugin plugin;
    private final Map<UUID, Set<String>> discoveredLore = new HashMap<>();
    private final Map<String, LoreFragment> allFragments = new LinkedHashMap<>();
    
    /**
     * Representa un fragmento de lore que puede ser descubierto
     */
    public static class LoreFragment {
        private final String id;
        private final String title;
        private final String content;
        private final Material itemIcon;
        private final int tier; // 1-5, determina rareza
        
        public LoreFragment(String id, String title, String content, Material itemIcon, int tier) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.itemIcon = itemIcon;
            this.tier = tier;
        }
        
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public Material getItemIcon() { return itemIcon; }
        public int getTier() { return tier; }
        
        public String getRarityColor() {
            return switch (tier) {
                case 1 -> "§f"; // Común
                case 2 -> "§a"; // Poco común
                case 3 -> "§9"; // Raro
                case 4 -> "§5"; // Épico
                case 5 -> "§6"; // Legendario
                default -> "§7";
            };
        }
        
        public String getRarityName() {
            return switch (tier) {
                case 1 -> "Común";
                case 2 -> "Poco Común";
                case 3 -> "Raro";
                case 4 -> "Épico";
                case 5 -> "Legendario";
                default -> "Desconocido";
            };
        }
    }
    
    public LoreSystem(Plugin plugin) {
        this.plugin = plugin;
        initializeFragments();
    }
    
    /**
     * Inicializa todos los fragmentos de lore disponibles
     */
    private void initializeFragments() {
        // Tier 1 - Común (información básica)
        allFragments.put("shadow_nature", new LoreFragment(
            "shadow_nature",
            "La Naturaleza de las Sombras",
            "§7Las Sombras Largas no son criaturas vivas.\n§7Son ecos... memorias de algo que fue.\n§7Repetición sin propósito, movimiento sin vida.",
            Material.ECHO_SHARD,
            1
        ));
        
        allFragments.put("observer_presence", new LoreFragment(
            "observer_presence",
            "El Observador",
            "§7Una voz que observa sin ser vista.\n§7Habla de acontecimientos que aún no ocurren.\n§7¿Testigo... o arquitecto?",
            Material.ENDER_EYE,
            1
        ));
        
        // Tier 2 - Poco común (contexto histórico)
        allFragments.put("first_incident", new LoreFragment(
            "first_incident",
            "El Primer Incidente",
            "§aHace siglos, algo se rompió.\n§aNo en el mundo físico, sino en algo más profundo.\n§aLa primera grieta entre lo que es y lo que fue.",
            Material.DAMAGED_ANVIL,
            2
        ));
        
        allFragments.put("anchor_purpose", new LoreFragment(
            "anchor_purpose",
            "Las Anclas Dimensionales",
            "§aNo fueron construidas para contener.\n§aFueron construidas para recordar.\n§aPuntos de fijación donde el tiempo se ancló.",
            Material.RESPAWN_ANCHOR,
            2
        ));
        
        // Tier 3 - Raro (revelaciones importantes)
        allFragments.put("core_truth", new LoreFragment(
            "core_truth",
            "La Verdad del Núcleo",
            "§9El Núcleo no es una entidad.\n§9Es una cicatriz. Una herida que no cierra.\n§9El punto donde la realidad se dobló sobre sí misma.",
            Material.NETHER_STAR,
            3
        ));
        
        allFragments.put("guardian_origin", new LoreFragment(
            "guardian_origin",
            "Origen del Guardián",
            "§9El Guardián no fue creado para proteger.\n§9Fue sellado para olvidar.\n§9Ahora despierta sin recordar su propósito original.",
            Material.NETHERITE_CHESTPLATE,
            3
        ));
        
        // Tier 4 - Épico (conexiones profundas)
        allFragments.put("echo_mechanism", new LoreFragment(
            "echo_mechanism",
            "El Mecanismo del Eco",
            "§5El eco no es un reflejo del pasado.\n§5Es el futuro intentando nacer en el presente.\n§5Las sombras no recuerdan... anticipan.",
            Material.DRAGON_EGG,
            4
        ));
        
        allFragments.put("observer_identity", new LoreFragment(
            "observer_identity",
            "Identidad del Observador",
            "§5El Observador existe fuera del tiempo.\n§5Ha visto este ciclo repetirse antes.\n§5Cada vez, los resultados son ligeramente diferentes.",
            Material.END_CRYSTAL,
            4
        ));
        
        // Tier 5 - Legendario (verdades absolutas)
        allFragments.put("final_truth", new LoreFragment(
            "final_truth",
            "La Verdad Final",
            "§6La grieta no puede ser sellada.\n§6Solo puede ser pospuesta.\n§6Lo que viene no tiene forma porque aún está eligiendo.\n§6Y ustedes... están en su memoria.",
            Material.TOTEM_OF_UNDYING,
            5
        ));
        
        allFragments.put("figure_revelation", new LoreFragment(
            "figure_revelation",
            "La Figura Revelada",
            "§6La figura misteriosa no es una amenaza.\n§6Es una advertencia. Un emisario.\n§6Representa lo que podría ser si el ciclo continúa.\n§6La forma que el eco está intentando alcanzar.",
            Material.WITHER_SKELETON_SKULL,
            5
        ));
    }
    
    /**
     * Revela un fragmento de lore a un jugador
     * 
     * @return true si es un descubrimiento nuevo
     */
    public boolean revealFragment(Player player, String fragmentId) {
        UUID uuid = player.getUniqueId();
        
        if (!allFragments.containsKey(fragmentId)) {
            return false;
        }
        
        // Obtener o crear set de lore descubierto
        Set<String> discovered = discoveredLore.computeIfAbsent(uuid, k -> new HashSet<>());
        
        // Si ya lo descubrió, retornar false
        if (discovered.contains(fragmentId)) {
            return false;
        }
        
        // Agregar al set de descubiertos
        discovered.add(fragmentId);
        
        // Notificar al jugador
        LoreFragment fragment = allFragments.get(fragmentId);
        player.sendMessage("");
        player.sendMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§e§l✦ FRAGMENTO DE LORE DESCUBIERTO ✦");
        player.sendMessage("");
        player.sendMessage(fragment.getRarityColor() + "§l" + fragment.getTitle());
        player.sendMessage(fragment.getRarityColor() + "[" + fragment.getRarityName() + "]");
        player.sendMessage("");
        player.sendMessage(fragment.getContent());
        player.sendMessage("");
        player.sendMessage("§7Progreso: §e" + discovered.size() + "/" + allFragments.size());
        player.sendMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
        
        // Dar item físico del fragmento
        giveFragmentItem(player, fragment);
        
        return true;
    }
    
    /**
     * Da un item físico representando el fragmento de lore
     */
    private void giveFragmentItem(Player player, LoreFragment fragment) {
        ItemStack item = new ItemStack(fragment.getItemIcon());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(Component.text(fragment.getRarityColor() + "§l" + fragment.getTitle()));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(fragment.getRarityColor() + "[" + fragment.getRarityName() + "]"));
            lore.add(Component.text(""));
            
            // Dividir contenido en líneas
            String[] lines = fragment.getContent().split("\n");
            for (String line : lines) {
                lore.add(Component.text(line));
            }
            
            lore.add(Component.text(""));
            lore.add(Component.text("§8Fragmento de lore del evento"));
            lore.add(Component.text("§8El Eco de las Sombras Largas"));
            
            meta.lore(lore);
            
            // Glow effect según tier
            if (fragment.getTier() >= 3) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            
            item.setItemMeta(meta);
        }
        
        // Agregar al inventario
        player.getInventory().addItem(item);
    }
    
    /**
     * Revela un fragmento aleatorio según el tier
     */
    public void revealRandomFragment(Player player, int maxTier) {
        List<String> availableFragments = new ArrayList<>();
        Set<String> discovered = discoveredLore.getOrDefault(player.getUniqueId(), new HashSet<>());
        
        for (Map.Entry<String, LoreFragment> entry : allFragments.entrySet()) {
            if (entry.getValue().getTier() <= maxTier && !discovered.contains(entry.getKey())) {
                availableFragments.add(entry.getKey());
            }
        }
        
        if (!availableFragments.isEmpty()) {
            String randomId = availableFragments.get((int) (Math.random() * availableFragments.size()));
            revealFragment(player, randomId);
        }
    }
    
    /**
     * Obtiene el progreso de lore de un jugador
     */
    public int getProgress(Player player) {
        return discoveredLore.getOrDefault(player.getUniqueId(), new HashSet<>()).size();
    }
    
    /**
     * Obtiene el total de fragmentos disponibles
     */
    public int getTotalFragments() {
        return allFragments.size();
    }
    
    /**
     * Verifica si un jugador ha descubierto un fragmento específico
     */
    public boolean hasDiscovered(Player player, String fragmentId) {
        return discoveredLore.getOrDefault(player.getUniqueId(), new HashSet<>()).contains(fragmentId);
    }
    
    /**
     * Muestra el codex de lore (todos los fragmentos descubiertos)
     */
    public void showCodex(Player player) {
        Set<String> discovered = discoveredLore.getOrDefault(player.getUniqueId(), new HashSet<>());
        
        player.sendMessage("");
        player.sendMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§e§l📖 CODEX DE LORE");
        player.sendMessage("§7Progreso: §e" + discovered.size() + "/" + allFragments.size());
        player.sendMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
        
        for (Map.Entry<String, LoreFragment> entry : allFragments.entrySet()) {
            LoreFragment fragment = entry.getValue();
            boolean isDiscovered = discovered.contains(entry.getKey());
            
            if (isDiscovered) {
                player.sendMessage(fragment.getRarityColor() + "✓ " + fragment.getTitle() + " §7[" + fragment.getRarityName() + "]");
            } else {
                player.sendMessage("§8✗ §k" + fragment.getTitle() + " §8[???]");
            }
        }
        
        player.sendMessage("");
        player.sendMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
    }
    
    /**
     * Limpia el progreso de un jugador (para testing)
     */
    public void resetProgress(Player player) {
        discoveredLore.remove(player.getUniqueId());
    }
    
    /**
     * Limpia todos los datos
     */
    public void cleanup() {
        discoveredLore.clear();
    }
}
