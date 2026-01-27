package me.apocalipsis.ciclos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;

/**
 * Sanitizador de items para prevenir transferencia entre mundos.
 * Detecta y elimina items problemáticos como Shulker Boxes con contenido,
 * Bundles, items con NBT personalizado, y otros items que puedan usarse para transferir entre mundos.
 */
public class ItemSanitizer {
    
    // NBT tag para marcar items con mundo de origen
    private static final String NBT_WORLD_TAG = "CicloWorld";
    private static final String NBT_QUARANTINE_TAG = "CicloQuarantine";
    
    private final Set<Material> blockedMaterials;
    private final boolean sanitizeEnabled;
    private final Set<Material> whitelistedMaterials;
    private final boolean checkCustomNBT;
    private final boolean useWorldTagging; // Nueva opción para marcar items
    
    public ItemSanitizer(Set<Material> blockedMaterials, boolean sanitizeEnabled) {
        this.blockedMaterials = blockedMaterials != null ? blockedMaterials : new HashSet<>();
        this.sanitizeEnabled = sanitizeEnabled;
        this.whitelistedMaterials = createDefaultWhitelist();
        this.checkCustomNBT = true; // Siempre verificar NBT
        this.useWorldTagging = true; // Activar sistema de marcado por defecto
    }
    
    /**
     * Resultado de la sanitización
     */
    public static class SanitizeResult {
        private final List<ItemStack> removedItems;
        private final int totalRemoved;
        private final boolean hadProblematicItems;
        
        public SanitizeResult(List<ItemStack> removedItems, int totalRemoved, boolean hadProblematicItems) {
            this.removedItems = removedItems;
            this.totalRemoved = totalRemoved;
            this.hadProblematicItems = hadProblematicItems;
        }
        
        public List<ItemStack> getRemovedItems() { return removedItems; }
        public int getTotalRemoved() { return totalRemoved; }
        public boolean hadProblematicItems() { return hadProblematicItems; }
    }
    
    /**
     * Sanitiza un inventario completo
     * 
     * @param inventory Array de ItemStacks a sanitizar
     * @return Resultado de la sanitización
     */
    public SanitizeResult sanitizeInventory(ItemStack[] inventory) {
        if (!sanitizeEnabled || inventory == null) {
            return new SanitizeResult(new ArrayList<>(), 0, false);
        }
        
        List<ItemStack> removedItems = new ArrayList<>();
        int totalRemoved = 0;
        boolean hadProblematic = false;
        
        for (int i = 0; i < inventory.length; i++) {
            ItemStack item = inventory[i];
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            
            if (isProblematicItem(item)) {
                removedItems.add(item.clone());
                totalRemoved += item.getAmount();
                inventory[i] = null;
                hadProblematic = true;
            }
        }
        
        return new SanitizeResult(removedItems, totalRemoved, hadProblematic);
    }
    
    /**
     * Verifica si un item es problemático (puede usarse para transferir entre mundos)
     */
    public boolean isProblematicItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        Material type = item.getType();
        
        // 0. Verificar si está en whitelist (siempre permitir)
        if (whitelistedMaterials.contains(type)) {
            return false;
        }
        
        // 1. Verificar si está en la lista de materiales bloqueados
        if (blockedMaterials.contains(type)) {
            return true;
        }
        
        // 2. Verificar Shulker Boxes (todos los tipos)
        if (isShulkerBox(type)) {
            // Si tiene contenido, es problemático
            return hasShulkerBoxContent(item);
        }
        
        // 3. Verificar Bundles
        if (type == Material.BUNDLE) {
            return hasBundleContent(item);
        }
        
        // 4. Otros items con inventario interno
        if (hasInternalInventory(type)) {
            return true;
        }
        
        // 5. Verificar NBT personalizado sospechoso
        if (checkCustomNBT && hasCustomNBT(item)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Verifica si un material es una Shulker Box
     */
    public static boolean isShulkerBox(Material material) {
        String name = material.name();
        return name.equals("SHULKER_BOX") || name.endsWith("_SHULKER_BOX");
    }
    
    /**
     * Verifica si una Shulker Box tiene contenido
     */
    private boolean hasShulkerBoxContent(ItemStack item) {
        if (!(item.getItemMeta() instanceof BlockStateMeta)) {
            return false;
        }
        
        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        if (!(meta.getBlockState() instanceof ShulkerBox)) {
            return false;
        }
        
        ShulkerBox shulker = (ShulkerBox) meta.getBlockState();
        ItemStack[] contents = shulker.getInventory().getContents();
        
        for (ItemStack content : contents) {
            if (content != null && content.getType() != Material.AIR) {
                return true; // Tiene contenido
            }
        }
        
        return false;
    }
    
    /**
     * Verifica si un Bundle tiene contenido
     */
    private boolean hasBundleContent(ItemStack item) {
        if (!(item.getItemMeta() instanceof BundleMeta)) {
            return false;
        }
        
        BundleMeta meta = (BundleMeta) item.getItemMeta();
        List<ItemStack> items = meta.getItems();
        
        return items != null && !items.isEmpty();
    }
    
    /**
     * Verifica si un tipo de material tiene inventario interno
     */
    private boolean hasInternalInventory(Material type) {
        String name = type.name();
        
        // Contenedores con inventario
        return name.contains("CHEST_MINECART") ||
               name.contains("HOPPER_MINECART") ||
               name.contains("FURNACE_MINECART");
    }
    
    /**
     * Verifica si un item tiene NBT personalizado sospechoso
     * NBT personalizado puede indicar items modificados o de plugins que podrían
     * usarse para transferir valor entre mundos
     */
    private boolean hasCustomNBT(ItemStack item) {
        if (!item.hasItemMeta()) {
            return false;
        }
        
        var meta = item.getItemMeta();
        
        // Verificar lore personalizado (puede contener datos)
        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (String line : lore) {
                    // Detectar lore que parezca contener datos serializados
                    if (line.contains("{") || line.contains("[") || line.contains("data:")) {
                        return true;
                    }
                }
            }
        }
        
        // Verificar atributos personalizados
        if (meta.hasAttributeModifiers()) {
            // Items con modificadores de atributos personalizados son sospechosos
            return true;
        }
        
        // Verificar nombre personalizado extremadamente largo (podría contener datos)
        if (meta.hasDisplayName()) {
            String displayName = meta.getDisplayName();
            if (displayName.length() > 100) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Marca un item con el mundo de origen usando NBT
     * Esto permite rastrear de qué mundo vino el item
     * 
     * @param item Item a marcar
     * @param worldName Nombre del mundo
     */
    public static void tagItemWithWorld(ItemStack item, String worldName) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return;
        }
        
        var meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        
        // Usar PersistentDataContainer para marcar el item
        var pdc = meta.getPersistentDataContainer();
        var key = new org.bukkit.NamespacedKey("apocalipsis", NBT_WORLD_TAG.toLowerCase());
        pdc.set(key, org.bukkit.persistence.PersistentDataType.STRING, worldName);
        
        item.setItemMeta(meta);
    }
    
    /**
     * Obtiene el mundo de origen de un item marcado
     * 
     * @param item Item a verificar
     * @return Nombre del mundo o null si no está marcado
     */
    public static String getItemWorld(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return null;
        }
        
        var meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        var pdc = meta.getPersistentDataContainer();
        var key = new org.bukkit.NamespacedKey("apocalipsis", NBT_WORLD_TAG.toLowerCase());
        
        if (pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
            return pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
        }
        
        return null;
    }
    
    /**
     * Marca un item como "en cuarentena" (sospechoso)
     * 
     * @param item Item a marcar
     * @param reason Razón de la cuarentena
     */
    public static void quarantineItem(ItemStack item, String reason) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return;
        }
        
        var meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        
        // Marcar como cuarentena
        var pdc = meta.getPersistentDataContainer();
        var key = new org.bukkit.NamespacedKey("apocalipsis", NBT_QUARANTINE_TAG.toLowerCase());
        pdc.set(key, org.bukkit.persistence.PersistentDataType.STRING, reason);
        
        // Añadir lore visible de advertencia
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        lore.add("§c§l⚠ ITEM EN CUARENTENA");
        lore.add("§7Razón: " + reason);
        lore.add("§7Contacta a un admin si crees que es un error");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
    }
    
    /**
     * Verifica si un item está en cuarentena
     * 
     * @param item Item a verificar
     * @return true si está en cuarentena
     */
    public static boolean isQuarantined(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        
        var meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        var pdc = meta.getPersistentDataContainer();
        var key = new org.bukkit.NamespacedKey("apocalipsis", NBT_QUARANTINE_TAG.toLowerCase());
        
        return pdc.has(key, org.bukkit.persistence.PersistentDataType.STRING);
    }
    
    /**
     * Valida que un item pertenece al mundo actual
     * 
     * @param item Item a validar
     * @param currentWorld Mundo actual del jugador
     * @return true si el item es válido para el mundo actual
     */
    public static boolean validateItemWorld(ItemStack item, String currentWorld) {
        String itemWorld = getItemWorld(item);
        
        // Si no tiene marca, permitir (item nuevo o de antes del sistema)
        if (itemWorld == null) {
            return true;
        }
        
        // Verificar que coincida con el mundo actual
        return itemWorld.equals(currentWorld);
    }
    
    /**
     * Crea una whitelist por defecto de items siempre permitidos
     */
    private Set<Material> createDefaultWhitelist() {
        Set<Material> whitelist = new HashSet<>();
        
        // Bloques básicos siempre permitidos
        whitelist.add(Material.DIRT);
        whitelist.add(Material.STONE);
        whitelist.add(Material.COBBLESTONE);
        whitelist.add(Material.SAND);
        whitelist.add(Material.GRAVEL);
        whitelist.add(Material.OAK_LOG);
        whitelist.add(Material.SPRUCE_LOG);
        whitelist.add(Material.BIRCH_LOG);
        whitelist.add(Material.JUNGLE_LOG);
        whitelist.add(Material.ACACIA_LOG);
        whitelist.add(Material.DARK_OAK_LOG);
        
        // Herramientas básicas
        whitelist.add(Material.WOODEN_PICKAXE);
        whitelist.add(Material.WOODEN_AXE);
        whitelist.add(Material.WOODEN_SHOVEL);
        whitelist.add(Material.WOODEN_SWORD);
        whitelist.add(Material.STONE_PICKAXE);
        whitelist.add(Material.STONE_AXE);
        whitelist.add(Material.STONE_SHOVEL);
        whitelist.add(Material.STONE_SWORD);
        
        // Comida básica
        whitelist.add(Material.BREAD);
        whitelist.add(Material.APPLE);
        whitelist.add(Material.COOKED_BEEF);
        whitelist.add(Material.COOKED_PORKCHOP);
        whitelist.add(Material.COOKED_CHICKEN);
        
        return whitelist;
    }
    
    /**
     * Crea un reporte legible de items removidos
     */
    public static String createRemovalReport(SanitizeResult result) {
        if (!result.hadProblematicItems()) {
            return "No se encontraron items problemáticos";
        }
        
        Map<Material, Integer> itemCounts = new HashMap<>();
        for (ItemStack item : result.getRemovedItems()) {
            itemCounts.merge(item.getType(), item.getAmount(), Integer::sum);
        }
        
        StringBuilder report = new StringBuilder();
        report.append("Items removidos:\n");
        for (Map.Entry<Material, Integer> entry : itemCounts.entrySet()) {
            report.append("  - ")
                  .append(entry.getKey().name())
                  .append(" x")
                  .append(entry.getKey())
                  .append("\n");
        }
        
        return report.toString();
    }
}
