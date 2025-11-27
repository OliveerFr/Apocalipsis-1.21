package me.apocalipsis.skills;

import me.apocalipsis.Apocalipsis;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Listener que aplica los efectos de las habilidades del árbol.
 */
public class SkillEffectListener implements Listener {
    
    private final Apocalipsis plugin;
    private final SkillService skillService;
    
    // Cache de jugadores cayendo para Vuelo de Emergencia
    private final Set<UUID> playersGliding = new HashSet<>();
    
    // Jugadores que deben revivir con Fénix
    private final Map<UUID, Location> phoenixRevive = new HashMap<>();
    
    // Items que ya fueron procesados por auto-recolección
    private final Set<UUID> processedItems = new HashSet<>();
    
    public SkillEffectListener(Apocalipsis plugin, SkillService skillService) {
        this.plugin = plugin;
        this.skillService = skillService;
        
        // Limpiar items procesados cada minuto
        Bukkit.getScheduler().runTaskTimer(plugin, processedItems::clear, 1200L, 1200L);
    }
    
    // ==================== JOIN/QUIT ====================
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Aplicar efectos al unirse
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            skillService.applySkillEffects(player);
        }, 20L);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        playersGliding.remove(uuid);
        phoenixRevive.remove(uuid);
    }
    
    // ==================== DAÑO ====================
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        UUID uuid = player.getUniqueId();
        EntityDamageEvent.DamageCause cause = event.getCause();
        double damage = event.getDamage();
        double reduction = 0;
        
        // === CAÍDA SUAVE / PLUMA ===
        if (cause == EntityDamageEvent.DamageCause.FALL) {
            if (skillService.hasSkill(uuid, Skill.PLUMA)) {
                reduction = 0.50; // -50%
            } else if (skillService.hasSkill(uuid, Skill.CAIDA_SUAVE)) {
                reduction = 0.25; // -25%
            }
        }
        
        // === RESISTENCIA AL FUEGO / IGNÍFUGO ===
        if (cause == EntityDamageEvent.DamageCause.FIRE || 
            cause == EntityDamageEvent.DamageCause.FIRE_TICK ||
            cause == EntityDamageEvent.DamageCause.LAVA) {
            
            if (skillService.hasSkill(uuid, Skill.IGNIFUGO)) {
                reduction = 0.40; // -40%
                // Inmune a daño por pisar fuego
                if (cause == EntityDamageEvent.DamageCause.FIRE_TICK) {
                    Block below = player.getLocation().subtract(0, 1, 0).getBlock();
                    if (below.getType() == Material.FIRE || below.getType() == Material.SOUL_FIRE) {
                        event.setCancelled(true);
                        return;
                    }
                }
            } else if (skillService.hasSkill(uuid, Skill.RESISTENCIA_FUEGO)) {
                reduction = 0.20; // -20%
            }
        }
        
        // Aplicar reducción
        if (reduction > 0) {
            double newDamage = damage * (1 - reduction);
            event.setDamage(newDamage);
        }
    }
    
    // ==================== VUELO DE EMERGENCIA ====================
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Verificar Vuelo de Emergencia
        if (!skillService.hasSkill(uuid, Skill.VUELO_EMERGENCIA)) return;
        if (!skillService.isSkillEnabled(uuid, Skill.VUELO_EMERGENCIA)) return;
        if (playersGliding.contains(uuid)) return;
        if (player.isGliding() || player.isFlying()) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        
        // Detectar caída mortal (>= 15 bloques)
        double fallDistance = player.getFallDistance();
        
        if (fallDistance >= 15 && player.getVelocity().getY() < -0.5) {
            if (!skillService.isGlideReady(player)) return;
            activateEmergencyGlide(player);
        }
    }
    
    private void activateEmergencyGlide(Player player) {
        UUID uuid = player.getUniqueId();
        playersGliding.add(uuid);
        skillService.useGlide(player);
        
        player.sendMessage("§6§l⚡ §eVuelo de Emergencia activado!");
        player.playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, 1.0f, 1.0f);
        
        // Ralentizar la caída
        Vector velocity = player.getVelocity();
        velocity.setY(-0.5);
        player.setVelocity(velocity);
        
        // Efecto de planeo por 3 segundos
        player.setGliding(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0, true, true));
        
        // Partículas
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
        
        // Terminar después de 3 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            playersGliding.remove(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.setGliding(false);
                p.sendMessage("§7Vuelo de Emergencia terminado. §8(Cooldown: 1 min)");
            }
        }, 60L);
    }
    
    // ==================== FÉNIX ====================
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        
        // === VOID STORAGE - No dropear items ===
        if (skillService.hasSkill(uuid, Skill.VOID_STORAGE)) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setKeepLevel(true);
            event.setDroppedExp(0);
            player.sendMessage("§d§l✦ §5Void Storage protegió tu inventario!");
        }
        
        // === FÉNIX ===
        if (skillService.hasSkill(uuid, Skill.FENIX)) {
            if (skillService.isPhoenixReady(player)) {
                // Marcar para revivir
                phoenixRevive.put(uuid, player.getLocation().clone());
                skillService.usePhoenix(player);
                
                event.setDeathMessage(null);
                player.sendMessage("§6§l✦ §e¡Fénix activado! Revivirás en tu ubicación...");
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        Location reviveLocation = phoenixRevive.remove(uuid);
        if (reviveLocation == null) return;
        
        // Respawnear en la ubicación de muerte
        event.setRespawnLocation(reviveLocation);
        
        // Aplicar efectos después de respawn
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.setHealth(6); // 3 corazones
                p.setFoodLevel(10);
                
                // Efectos visuales
                p.getWorld().spawnParticle(Particle.FLAME, p.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);
                p.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
                
                Bukkit.broadcastMessage("§6§l✦ §e" + p.getName() + " §fha renacido de las cenizas!");
                
                long remaining = skillService.getPhoenixCooldownRemaining(p);
                long hours = remaining / (60 * 60 * 1000);
                p.sendMessage("§7Fénix en cooldown por §e" + hours + " horas§7.");
            }
        }, 1L);
    }
    
    // ==================== HAMBRE ====================
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        UUID uuid = player.getUniqueId();
        
        // Solo afectar pérdida de hambre
        if (event.getFoodLevel() >= player.getFoodLevel()) return;
        
        double reduction = 0;
        
        if (skillService.hasSkill(uuid, Skill.METABOLISMO_LENTO)) {
            reduction = 0.40;
        } else if (skillService.hasSkill(uuid, Skill.ESTOMAGO_HIERRO)) {
            reduction = 0.20;
        }
        
        if (reduction > 0) {
            if (Math.random() < reduction) {
                event.setCancelled(true);
            }
        }
    }
    
    // ==================== MINERÍA Y AUTO-RECOLECCIÓN ====================
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Block block = event.getBlock();
        
        // === AUTO-RECOLECCIÓN ===
        if (skillService.hasSkill(uuid, Skill.AUTO_RECOLECCION) && 
            skillService.isSkillEnabled(uuid, Skill.AUTO_RECOLECCION)) {
            
            // Obtener los drops antes de que se rompan
            Collection<ItemStack> drops = block.getDrops(player.getInventory().getItemInMainHand());
            
            // Cancelar drops normales
            event.setDropItems(false);
            
            // Dar los items directamente al jugador
            for (ItemStack drop : drops) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(drop);
                // Si no cabe, dropearlo
                for (ItemStack item : leftover.values()) {
                    block.getWorld().dropItemNaturally(block.getLocation(), item);
                }
            }
            
            // También dar experiencia si aplica
            int expToDrop = getExpToDrop(block.getType());
            if (expToDrop > 0) {
                player.giveExp(expToDrop);
            }
        }
        
        // === TOQUE DE FORTUNA (después de auto-recolección) ===
        if (skillService.hasSkill(uuid, Skill.TOQUE_FORTUNA) && isOre(block.getType())) {
            if (Math.random() < 0.10) {
                // +10% drop extra
                Collection<ItemStack> bonusDrops = block.getDrops(player.getInventory().getItemInMainHand());
                for (ItemStack drop : bonusDrops) {
                    if (skillService.hasSkill(uuid, Skill.AUTO_RECOLECCION)) {
                        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(drop);
                        for (ItemStack item : leftover.values()) {
                            block.getWorld().dropItemNaturally(block.getLocation(), item);
                        }
                    } else {
                        block.getWorld().dropItemNaturally(block.getLocation(), drop);
                    }
                }
                player.sendMessage("§a§l⚡ §a¡Toque de Fortuna! (+10% drops)");
            }
        }
        
        // === TOQUE DE SEDA NATURAL ===
        if (skillService.hasSkill(uuid, Skill.SEDA_NATURAL)) {
            if (Math.random() < 0.05 && canSilkTouch(block.getType())) {
                // 5% chance de silk touch
                if (!skillService.hasSkill(uuid, Skill.AUTO_RECOLECCION)) {
                    event.setDropItems(false);
                }
                ItemStack silkDrop = new ItemStack(block.getType());
                if (skillService.hasSkill(uuid, Skill.AUTO_RECOLECCION)) {
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(silkDrop);
                    for (ItemStack item : leftover.values()) {
                        block.getWorld().dropItemNaturally(block.getLocation(), item);
                    }
                } else {
                    block.getWorld().dropItemNaturally(block.getLocation(), silkDrop);
                }
                player.sendMessage("§d✦ §fToque de Seda Natural!");
            }
        }
    }
    
    // Recoger items cercanos para auto-recolección (items que no son de minado)
    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        UUID uuid = player.getUniqueId();
        if (!skillService.hasSkill(uuid, Skill.AUTO_RECOLECCION)) return;
        if (!skillService.isSkillEnabled(uuid, Skill.AUTO_RECOLECCION)) return;
        
        // Aumentar el rango de recogida de items
        Item item = event.getItem();
        if (processedItems.contains(item.getUniqueId())) return;
        
        // Marcar como procesado
        processedItems.add(item.getUniqueId());
    }
    
    private int getExpToDrop(Material material) {
        return switch (material) {
            case COAL_ORE, DEEPSLATE_COAL_ORE -> (int) (Math.random() * 2) + 1;
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> (int) (Math.random() * 5) + 3;
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> (int) (Math.random() * 5) + 3;
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> (int) (Math.random() * 4) + 2;
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> (int) (Math.random() * 4) + 1;
            case NETHER_QUARTZ_ORE -> (int) (Math.random() * 4) + 2;
            case SPAWNER -> (int) (Math.random() * 29) + 15;
            default -> 0;
        };
    }
    
    private boolean isOre(Material material) {
        return switch (material) {
            case COAL_ORE, DEEPSLATE_COAL_ORE,
                 IRON_ORE, DEEPSLATE_IRON_ORE,
                 GOLD_ORE, DEEPSLATE_GOLD_ORE,
                 DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                 EMERALD_ORE, DEEPSLATE_EMERALD_ORE,
                 LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                 REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE,
                 COPPER_ORE, DEEPSLATE_COPPER_ORE,
                 NETHER_GOLD_ORE, NETHER_QUARTZ_ORE,
                 ANCIENT_DEBRIS -> true;
            default -> false;
        };
    }
    
    private boolean canSilkTouch(Material material) {
        return switch (material) {
            case STONE, COBBLESTONE, GRASS_BLOCK, DIRT, 
                 COAL_ORE, DEEPSLATE_COAL_ORE,
                 DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                 EMERALD_ORE, DEEPSLATE_EMERALD_ORE,
                 LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                 REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE,
                 GLASS, GLASS_PANE, ICE, BLUE_ICE, PACKED_ICE,
                 GLOWSTONE, SEA_LANTERN -> true;
            default -> false;
        };
    }
    
    // ==================== NADADOR ====================
    
    public void applySwimBoost(Player player) {
        if (!skillService.hasSkill(player, Skill.NADADOR)) return;
        if (!skillService.isSkillEnabled(player, Skill.NADADOR)) return;
        
        if (player.isInWater()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 40, 0, true, false));
        }
    }
    
    // ==================== BRANQUIAS / ANFIBIO ====================
    
    public void applyWaterBreathing(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (skillService.hasSkill(uuid, Skill.ANFIBIO)) {
            if (player.isInWater()) {
                player.setRemainingAir(player.getMaximumAir());
            }
        } else if (skillService.hasSkill(uuid, Skill.BRANQUIAS)) {
            if (player.isInWater() && player.getRemainingAir() < player.getMaximumAir()) {
                // Restaurar aire más lento que perderlo
                int newAir = Math.min(player.getRemainingAir() + 30, player.getMaximumAir());
                player.setRemainingAir(newAir);
            }
        }
    }
}
