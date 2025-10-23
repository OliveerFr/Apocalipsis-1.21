package me.apocalipsis.listeners;

import me.apocalipsis.missions.MissionService;
import me.apocalipsis.missions.MissionType;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class MissionListener implements Listener {

    private final MissionService missionService;

    public MissionListener(MissionService missionService) {
        this.missionService = missionService;
    }

    @EventHandler
    public void onEntityKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        EntityType type = event.getEntityType();
        missionService.progressMission(killer, MissionType.MATAR, type.name(), 1);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();
        missionService.progressMission(player, MissionType.ROMPER, material.name(), 1);
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Material result = event.getRecipe().getResult().getType();
        
        // [HOTFIX] Calcular cantidad real con SHIFT+CLICK
        int made;
        if (event.isShiftClick()) {
            made = computeShiftOutput(event.getInventory(), event.getRecipe());
        } else {
            made = event.getRecipe().getResult().getAmount();
        }
        
        if (made <= 0) return;
        
        missionService.progressMission(player, MissionType.CRAFTEAR, result.name(), made);
    }
    
    /**
     * [HOTFIX] Calcula cuántos items se producen con SHIFT+CLICK
     */
    private int computeShiftOutput(org.bukkit.inventory.CraftingInventory inv, org.bukkit.inventory.Recipe recipe) {
        // Calcular máximo número de veces que puede repetirse la receta
        int crafts = Integer.MAX_VALUE;
        for (org.bukkit.inventory.ItemStack i : inv.getMatrix()) {
            if (i == null || i.getType().isAir()) continue;
            crafts = Math.min(crafts, i.getAmount());
        }
        if (crafts == Integer.MAX_VALUE) crafts = 0;

        int perCraft = recipe.getResult().getAmount();
        
        // Límite por espacio en inventario (simplificado)
        org.bukkit.inventory.InventoryHolder holder = inv.getHolder();
        if (holder instanceof Player) {
            Player p = (Player) holder;
            int freeSpace = calcFreeSpace(p.getInventory(), recipe.getResult().getType(), perCraft);
            int total = Math.min(crafts * perCraft, freeSpace);
            return Math.max(total, 0);
        }
        
        return crafts * perCraft;
    }
    
    /**
     * [HOTFIX] Calcula espacio libre en inventario para un material
     */
    private int calcFreeSpace(org.bukkit.inventory.PlayerInventory inv, Material mat, int perCraft) {
        int maxStack = mat.getMaxStackSize();
        int space = 0;
        
        // Contar slots vacíos + espacio en stacks existentes del mismo material
        for (org.bukkit.inventory.ItemStack item : inv.getStorageContents()) {
            if (item == null || item.getType().isAir()) {
                space += maxStack;
            } else if (item.getType() == mat && item.getAmount() < maxStack) {
                space += (maxStack - item.getAmount());
            }
        }
        
        return space;
    }

    @EventHandler
    public void onSmelt(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        Material material = event.getItemType();
        int amount = event.getItemAmount();
        
        missionService.progressMission(player, MissionType.COCINAR, material.name(), amount);
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Material material = event.getItem().getType();
        missionService.progressMission(player, MissionType.CONSUMIR, material.name(), 1);
    }
}
