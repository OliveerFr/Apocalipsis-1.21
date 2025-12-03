package me.apocalipsis.listeners;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.experience.DynamicXPManager;
import me.apocalipsis.experience.XPSource;

/**
 * Listener dinámico de XP con sistema mejorado de bonificaciones y logging
 */
public class ExperienceListener implements Listener {
    
    private final Apocalipsis plugin;
    private DynamicXPManager xpManager;
    
    public ExperienceListener(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Inicializa el manager de XP dinámico
     */
    public void initXPManager() {
        if (plugin.getExperienceService() != null) {
            this.xpManager = new DynamicXPManager(plugin, plugin.getExperienceService());
            plugin.getLogger().info("[XP] Sistema dinámico de XP inicializado");
        }
    }
    
    public DynamicXPManager getXPManager() {
        return xpManager;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // LOGIN - Actualizar racha
    // ═══════════════════════════════════════════════════════════════
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (xpManager == null) return;
        
        Player player = event.getPlayer();
        xpManager.checkStreakReset(player);
        xpManager.updateLoginStreak(player);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MATAR ENTIDADES
    // ═══════════════════════════════════════════════════════════════
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (xpManager == null) return;
        
        Player killer = event.getEntity().getKiller();
        if (killer == null || !killer.isOnline()) return;
        
        EntityType entityType = event.getEntityType();
        XPSource source = classifyMob(entityType);
        
        if (source != null) {
            xpManager.giveXP(killer, source, entityType.name());
        }
    }
    
    private XPSource classifyMob(EntityType type) {
        return switch (type) {
            // Jefes
            case ENDER_DRAGON, WITHER, ELDER_GUARDIAN, WARDEN -> XPSource.KILL_BOSS;
            
            // Hostiles
            case ZOMBIE, SKELETON, CREEPER, SPIDER, ENDERMAN, BLAZE, WITCH,
                 WITHER_SKELETON, CAVE_SPIDER, PHANTOM, DROWNED, HUSK, STRAY,
                 ZOMBIE_VILLAGER, SILVERFISH, ENDERMITE, SHULKER, GHAST,
                 MAGMA_CUBE, SLIME, PIGLIN, PIGLIN_BRUTE, HOGLIN, ZOGLIN,
                 VINDICATOR, EVOKER, PILLAGER, RAVAGER, VEX, GUARDIAN,
                 BREEZE, BOGGED -> XPSource.KILL_HOSTILE;
            
            // Pasivos
            case COW, PIG, CHICKEN, SHEEP, RABBIT, HORSE, DONKEY, MULE,
                 LLAMA, FOX, WOLF, CAT, PARROT, OCELOT, PANDA, POLAR_BEAR,
                 TURTLE, BEE, GOAT, AXOLOTL, FROG, SNIFFER, CAMEL, ARMADILLO -> XPSource.KILL_PASSIVE;
            
            default -> null;
        };
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MINAR BLOQUES
    // ═══════════════════════════════════════════════════════════════
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (xpManager == null) return;
        
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();
        
        // Verificar bloque colocado por jugador
        if (plugin.getBlockTracker() != null) {
            UUID owner = plugin.getBlockTracker().getBlockOwner(event.getBlock());
            if (owner != null) return;
        }
        
        XPSource source = classifyOre(material);
        if (source != null) {
            xpManager.giveXP(player, source, material.name());
        }
    }
    
    private XPSource classifyOre(Material material) {
        String name = material.name();
        
        // Épicos
        if (name.contains("ANCIENT_DEBRIS") || name.contains("EMERALD_ORE") || name.contains("DIAMOND_ORE")) {
            return XPSource.MINE_EPIC;
        }
        
        // Raros
        if (name.contains("GOLD_ORE") || name.contains("LAPIS_ORE") || name.contains("REDSTONE_ORE") ||
            name.contains("NETHER_GOLD") || name.contains("QUARTZ")) {
            return XPSource.MINE_RARE;
        }
        
        // Comunes
        if (name.contains("COAL_ORE") || name.contains("IRON_ORE") || name.contains("COPPER_ORE")) {
            return XPSource.MINE_COMMON;
        }
        
        return null;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // COSECHAR
    // ═══════════════════════════════════════════════════════════════
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHarvest(PlayerHarvestBlockEvent event) {
        if (xpManager == null) return;
        
        Player player = event.getPlayer();
        Material material = event.getHarvestedBlock().getType();
        
        if (isCrop(material)) {
            xpManager.giveXP(player, XPSource.HARVEST, material.name());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CRAFTEAR
    // ═══════════════════════════════════════════════════════════════
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (xpManager == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        Material material = event.getRecipe().getResult().getType();
        XPSource source = classifyCraft(material);
        
        if (source != null) {
            int cantidad = event.isShiftClick() ? 
                Math.min(event.getRecipe().getResult().getAmount() * 4, 64) : 
                event.getRecipe().getResult().getAmount();
            xpManager.giveXP(player, source, material.name() + " x" + cantidad);
        }
    }
    
    private XPSource classifyCraft(Material material) {
        String name = material.name();
        
        // Épicos
        if (name.contains("NETHERITE") || name.contains("BEACON") || 
            name.contains("ENCHANTING") || name.contains("END_CRYSTAL") ||
            name.contains("CONDUIT")) {
            return XPSource.CRAFT_EPIC;
        }
        
        // Raros
        if (name.contains("DIAMOND") || name.contains("ANVIL") || 
            name.contains("BREWING") || name.contains("ENDER_CHEST")) {
            return XPSource.CRAFT_RARE;
        }
        
        // Comunes (solo items valiosos)
        if (name.contains("IRON") || name.contains("GOLD") || 
            name.contains("HOPPER") || name.contains("PISTON") ||
            name.contains("DISPENSER") || name.contains("OBSERVER") ||
            name.contains("COMPARATOR") || name.contains("REPEATER")) {
            return XPSource.CRAFT_COMMON;
        }
        
        return null;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PESCAR
    // ═══════════════════════════════════════════════════════════════
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (xpManager == null) return;
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        
        Player player = event.getPlayer();
        String caughtItem = "pez";
        boolean isTreasure = false;
        
        if (event.getCaught() instanceof org.bukkit.entity.Item item) {
            caughtItem = item.getItemStack().getType().name();
            isTreasure = caughtItem.contains("ENCHANTED") || caughtItem.contains("BOW") || 
                         caughtItem.contains("ROD") || caughtItem.contains("BOOK") ||
                         caughtItem.contains("SADDLE") || caughtItem.contains("NAME_TAG") ||
                         caughtItem.contains("NAUTILUS");
        }
        
        XPSource source = isTreasure ? XPSource.FISH_TREASURE : XPSource.FISH;
        xpManager.giveXP(player, source, caughtItem);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // DOMAR ANIMALES
    // ═══════════════════════════════════════════════════════════════
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (xpManager == null) return;
        if (!(event.getOwner() instanceof Player player)) return;
        
        EntityType entityType = event.getEntityType();
        
        // Multiplicador para animales difíciles
        double mult = (entityType == EntityType.HORSE || entityType == EntityType.LLAMA || 
                       entityType == EntityType.PARROT) ? 2.0 : 1.0;
        
        xpManager.giveXP(player, XPSource.TAME, entityType.name(), mult);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CRIAR ANIMALES
    // ═══════════════════════════════════════════════════════════════
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (xpManager == null) return;
        if (!(event.getBreeder() instanceof Player player)) return;
        
        xpManager.giveXP(player, XPSource.BREED, event.getEntityType().name());
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ESQUILAR
    // ═══════════════════════════════════════════════════════════════
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        if (xpManager == null) return;
        
        xpManager.giveXP(event.getPlayer(), XPSource.SHEAR, event.getEntity().getType().name());
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ENCANTAR
    // ═══════════════════════════════════════════════════════════════
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (xpManager == null) return;
        
        Player player = event.getEnchanter();
        int levelSpent = event.getExpLevelCost();
        Material item = event.getItem().getType();
        
        // Multiplicador basado en niveles gastados
        double mult = 1.0 + (levelSpent * 0.1);
        
        xpManager.giveXP(player, XPSource.ENCHANT, item.name() + " (Lvl " + levelSpent + ")", mult);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // COMERCIAR
    // ═══════════════════════════════════════════════════════════════
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrade(InventoryClickEvent event) {
        if (xpManager == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory().getType() != InventoryType.MERCHANT) return;
        if (event.getSlot() != 2) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        
        Material traded = event.getCurrentItem().getType();
        boolean isRare = traded.name().contains("ENCHANTED") || traded.name().contains("DIAMOND") ||
                         traded.name().contains("NETHERITE") || traded.name().contains("MENDING");
        
        XPSource source = isRare ? XPSource.TRADE_RARE : XPSource.TRADE;
        xpManager.giveXP(player, source, traded.name());
    }
    
    // ═══════════════════════════════════════════════════════════════
    // FUNDIR
    // ═══════════════════════════════════════════════════════════════
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        if (xpManager == null) return;
        
        Player player = event.getPlayer();
        Material material = event.getItemType();
        int amount = event.getItemAmount();
        
        // Solo para materiales valiosos
        if (material.name().contains("INGOT") || material.name().contains("GOLD") ||
            material.name().contains("IRON") || material.name().contains("COPPER") ||
            material.name().contains("GLASS") || material.name().contains("BRICK")) {
            
            double mult = 1.0 + (amount / 16.0); // Escala con cantidad
            xpManager.giveXP(player, XPSource.SMELT, material.name() + " x" + amount, mult);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // LOGROS
    // ═══════════════════════════════════════════════════════════════
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        if (xpManager == null) return;
        
        Player player = event.getPlayer();
        String key = event.getAdvancement().getKey().getKey();
        
        // Ignorar recetas y logros técnicos (estos se disparan constantemente)
        if (key.startsWith("recipes/")) return;
        if (key.startsWith("technical/")) return; // on_inventory_change, on_effect_change, etc.
        
        // Solo procesar logros reales (story/, adventure/, nether/, end/, husbandry/)
        if (!isRealAdvancement(key)) return;
        
        XPSource source = classifyAdvancement(key);
        xpManager.giveXP(player, source, key);
    }
    
    /**
     * Verifica si es un logro real y no un trigger técnico
     */
    private boolean isRealAdvancement(String key) {
        return key.startsWith("story/") ||
               key.startsWith("adventure/") ||
               key.startsWith("nether/") ||
               key.startsWith("end/") ||
               key.startsWith("husbandry/");
    }
    
    private XPSource classifyAdvancement(String key) {
        // Épicos
        if (key.contains("dragon") || key.contains("wither") || key.contains("elytra") ||
            key.contains("kill_all") || key.contains("uneasy_alliance") || 
            key.contains("great_view") || key.contains("how_did_we_get_here")) {
            return XPSource.ADVANCEMENT_EPIC;
        }
        
        // Raros
        if (key.contains("end/") || key.contains("nether/") || key.contains("adventure/")) {
            return XPSource.ADVANCEMENT_RARE;
        }
        
        return XPSource.ADVANCEMENT;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CONSUMIR ESPECIAL
    // ═══════════════════════════════════════════════════════════════
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (xpManager == null) return;
        
        Material material = event.getItem().getType();
        
        double mult = switch (material) {
            case ENCHANTED_GOLDEN_APPLE -> 5.0;
            case GOLDEN_APPLE -> 2.0;
            case GOLDEN_CARROT -> 1.0;
            case SUSPICIOUS_STEW -> 1.5;
            case CHORUS_FRUIT -> 1.0;
            default -> 0;
        };
        
        if (mult > 0) {
            xpManager.giveXP(event.getPlayer(), XPSource.CONSUME_SPECIAL, material.name(), mult);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // COLOCAR BLOQUES ESPECIALES
    // ═══════════════════════════════════════════════════════════════
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (xpManager == null) return;
        
        Material material = event.getBlock().getType();
        
        double mult = switch (material) {
            case BEACON -> 3.0;
            case CONDUIT -> 2.5;
            case END_CRYSTAL -> 2.0;
            case DRAGON_HEAD, WITHER_SKELETON_SKULL -> 1.5;
            case RESPAWN_ANCHOR -> 2.0;
            default -> 0;
        };
        
        if (mult > 0) {
            xpManager.giveXP(event.getPlayer(), XPSource.PLACE_SPECIAL, material.name(), mult);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // SMITHING
    // ═══════════════════════════════════════════════════════════════
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmith(SmithItemEvent event) {
        if (xpManager == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        ItemStack result = event.getInventory().getResult();
        if (result == null) return;
        
        Material material = result.getType();
        
        if (material.name().contains("NETHERITE") || material.name().contains("TRIM")) {
            double mult = material.name().contains("NETHERITE") ? 2.0 : 1.0;
            xpManager.giveXP(player, XPSource.SMITH, material.name(), mult);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MÉTODOS AUXILIARES
    // ═══════════════════════════════════════════════════════════════
    
    private boolean isCrop(Material material) {
        return switch (material) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART, 
                 COCOA, SWEET_BERRY_BUSH, MELON, PUMPKIN, PITCHER_PLANT,
                 TORCHFLOWER -> true;
            default -> false;
        };
    }
}
