package me.apocalipsis.skills;

import me.apocalipsis.Apocalipsis;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
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
    
    // Cache de última ubicación para detectar caída
    private final Map<UUID, Double> lastFallDistance = new HashMap<>();
    
    // Jugadores que deben revivir con Fénix
    private final Map<UUID, Location> phoenixRevive = new HashMap<>();
    
    public SkillEffectListener(Apocalipsis plugin, SkillService skillService) {
        this.plugin = plugin;
        this.skillService = skillService;
    }
    
    // ==================== JOIN/QUIT ====================
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Aplicar efectos al unirse
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            skillService.applySkillEffects(player);
        }, 20L); // 1 segundo después para asegurar que cargó todo
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        playersGliding.remove(uuid);
        lastFallDistance.remove(uuid);
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
                // Inmune a daño por pisar fuego (fire_tick)
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
            // Verificar cooldown
            if (!skillService.isGlideReady(player)) return;
            
            // ¡Activar vuelo de emergencia!
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
        velocity.setY(-0.5); // Reducir velocidad de caída
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
        }, 60L); // 3 segundos
    }
    
    // ==================== FÉNIX ====================
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        
        // Verificar Fénix
        if (!skillService.hasSkill(uuid, Skill.FENIX)) return;
        if (!skillService.isPhoenixReady(player)) return;
        
        // Marcar para revivir
        phoenixRevive.put(uuid, player.getLocation().clone());
        skillService.usePhoenix(player);
        
        // Modificar mensaje de muerte
        event.setDeathMessage(null);
        
        // Notificar
        player.sendMessage("§6§l✦ §e¡Fénix activado! Revivirás en tu ubicación...");
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
                // Establecer vida a 3 corazones (6 HP)
                p.setHealth(6);
                p.setFoodLevel(10);
                
                // Efectos visuales
                p.getWorld().spawnParticle(Particle.FLAME, p.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);
                p.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
                
                // Broadcast
                Bukkit.broadcastMessage("§6§l✦ §e" + p.getName() + " §fha renacido de las cenizas!");
                
                // Mensaje de cooldown
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
            reduction = 0.40; // 40% más lento
        } else if (skillService.hasSkill(uuid, Skill.ESTOMAGO_HIERRO)) {
            reduction = 0.20; // 20% más lento
        }
        
        if (reduction > 0) {
            // Probabilidad de cancelar la pérdida
            if (Math.random() < reduction) {
                event.setCancelled(true);
            }
        }
    }
    
    // ==================== MINERÍA ====================
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Block block = event.getBlock();
        
        // === TOQUE DE FORTUNA ===
        if (skillService.hasSkill(uuid, Skill.TOQUE_FORTUNA)) {
            // +10% drop para minerales
            if (isOre(block.getType())) {
                if (Math.random() < 0.10) {
                    // Duplicar drop
                    for (ItemStack drop : block.getDrops(player.getInventory().getItemInMainHand())) {
                        block.getWorld().dropItemNaturally(block.getLocation(), drop);
                    }
                }
            }
        }
        
        // === TOQUE DE SEDA NATURAL ===
        if (skillService.hasSkill(uuid, Skill.SEDA_NATURAL)) {
            // 5% chance de silk touch
            if (Math.random() < 0.05) {
                // Cancelar drop normal y dropear el bloque
                event.setDropItems(false);
                ItemStack silkDrop = new ItemStack(block.getType());
                block.getWorld().dropItemNaturally(block.getLocation(), silkDrop);
                player.sendMessage("§d✦ §fToque de Seda Natural!");
            }
        }
        
        // === AUTO-RECOLECCIÓN ===
        if (skillService.hasSkill(uuid, Skill.AUTO_RECOLECCION)) {
            // Los items cercanos van al inventario
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location loc = block.getLocation().add(0.5, 0.5, 0.5);
                Collection<Item> nearbyItems = loc.getWorld().getNearbyEntitiesByType(Item.class, loc, 3);
                
                for (Item item : nearbyItems) {
                    if (!item.isDead()) {
                        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.getItemStack());
                        if (leftover.isEmpty()) {
                            item.remove();
                        }
                    }
                }
            }, 5L);
        }
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
    
    // ==================== NADADOR ====================
    
    // Se maneja con un task periódico cuando está en agua
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
            // Respiración infinita
            if (player.isInWater()) {
                player.setRemainingAir(player.getMaximumAir());
            }
        } else if (skillService.hasSkill(uuid, Skill.BRANQUIAS)) {
            // +60% tiempo de respiración
            if (player.isInWater() && player.getRemainingAir() < player.getMaximumAir()) {
                int extraAir = (int) (player.getMaximumAir() * 0.60);
                player.setMaximumAir(player.getMaximumAir() + extraAir);
            }
        }
    }
}
