package me.apocalipsis.events.gameplay;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import me.apocalipsis.Apocalipsis;

import java.util.*;

/**
 * Sistema avanzado de combate para el Guardián del Umbral
 * 
 * Funcionalidades:
 * - 3 fases distintas con mecánicas únicas
 * - Weak points (cabeza = 2x daño)
 * - Invulnerabilidad entre fases
 * - Enrage timer (más fuerte después de tiempo)
 * - Patrones de ataque complejos
 * - Visual feedback por fase
 */
public class GuardianPhaseSystem {
    
    private final Apocalipsis plugin;
    private final Giant guardian;
    private final Location arenaCenter;
    
    private PhaseType currentPhase = PhaseType.PHASE_1;
    private boolean isInvulnerable = false;
    private long phaseStartTime;
    private long combatStartTime;
    
    private static final int ENRAGE_TIME_SECONDS = 600; // 10 minutos
    private boolean isEnraged = false;
    
    public GuardianPhaseSystem(Apocalipsis plugin, Giant guardian, Location arenaCenter) {
        this.plugin = plugin;
        this.guardian = guardian;
        this.arenaCenter = arenaCenter;
        this.phaseStartTime = System.currentTimeMillis();
        this.combatStartTime = System.currentTimeMillis();
        
        startPhase1();
        startEnrageTimer();
    }
    
    /**
     * Tipos de fase
     */
    public enum PhaseType {
        PHASE_1,  // 100-66% HP - Básico
        PHASE_2,  // 66-33% HP - Agresivo
        PHASE_3   // 33-0% HP - Desesperado
    }
    
    /**
     * Procesa daño al guardián
     */
    public void processDamage(EntityDamageByEntityEvent event) {
        if (isInvulnerable) {
            event.setCancelled(true);
            
            if (event.getDamager() instanceof Player) {
                Player player = (Player) event.getDamager();
                player.sendActionBar("§c§l✗ EL GUARDIÁN ES INVULNERABLE");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 2.0f);
            }
            return;
        }
        
        // Verificar si golpeó weak point (cabeza)
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            
            if (isHeadshot(attacker)) {
                // Daño doble en cabeza
                event.setDamage(event.getDamage() * 2.0);
                
                attacker.sendActionBar("§e§l✦ PUNTO DÉBIL §e§l✦");
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.5f);
                attacker.spawnParticle(Particle.ENCHANTED_HIT, guardian.getEyeLocation(), 20, 0.3, 0.3, 0.3, 0.1);
                
                // Mostrar damage number crítico
                Location displayLoc = guardian.getEyeLocation().add(0, 1, 0);
                showCriticalHit(displayLoc, event.getDamage());
            }
        }
        
        // Bonus de daño si está enraged
        if (isEnraged) {
            event.setDamage(event.getDamage() * 0.9); // 10% de reducción cuando está enraged
        }
        
        // Chequear transición de fase
        checkPhaseTransition();
    }
    
    /**
     * Verifica si el golpe fue en la cabeza
     */
    private boolean isHeadshot(Player attacker) {
        Location eyeLoc = attacker.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        
        // Raycast hacia donde mira el jugador
        Location targetLoc = eyeLoc.clone();
        for (int i = 0; i < 50; i++) {
            targetLoc.add(direction.clone().multiply(0.5));
            
            // Verificar si el rayo intersecta con la cabeza del guardian
            Location headLoc = guardian.getEyeLocation();
            if (targetLoc.distance(headLoc) < 2.0) {
                return true;
            }
            
            if (targetLoc.getBlock().getType().isSolid()) {
                break;
            }
        }
        
        return false;
    }
    
    /**
     * Muestra un hit crítico
     */
    private void showCriticalHit(Location location, double damage) {
        new BukkitRunnable() {
            int ticks = 0;
            double y = 0;
            
            @Override
            public void run() {
                if (ticks >= 20) {
                    cancel();
                    return;
                }
                
                Location displayLoc = location.clone().add(0, y, 0);
                
                // Hologram
                ArmorStand hologram = location.getWorld().spawn(displayLoc, ArmorStand.class);
                hologram.setVisible(false);
                hologram.setGravity(false);
                hologram.setMarker(true);
                hologram.setCustomName("§c§l✦ " + String.format("%.1f", damage) + " §c§l✦");
                hologram.setCustomNameVisible(true);
                
                Bukkit.getScheduler().runTaskLater(plugin, hologram::remove, 1L);
                
                y += 0.15;
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * Chequea si debe cambiar de fase
     */
    private void checkPhaseTransition() {
        if (!guardian.isValid()) return;
        
        double healthPercent = guardian.getHealth() / guardian.getAttribute(Attribute.MAX_HEALTH).getValue();
        
        if (healthPercent <= 0.66 && currentPhase == PhaseType.PHASE_1) {
            transitionToPhase2();
        } else if (healthPercent <= 0.33 && currentPhase == PhaseType.PHASE_2) {
            transitionToPhase3();
        }
    }
    
    /**
     * FASE 1: Básica (100-66%)
     */
    private void startPhase1() {
        currentPhase = PhaseType.PHASE_1;
        phaseStartTime = System.currentTimeMillis();
        
        // Aura normal
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!guardian.isValid() || currentPhase != PhaseType.PHASE_1) {
                    cancel();
                    return;
                }
                
                Location loc = guardian.getLocation();
                loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 3, 0), 10, 1, 2, 1, 0.03);
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    /**
     * Transición a FASE 2: Agresiva (66-33%)
     */
    private void transitionToPhase2() {
        if (currentPhase != PhaseType.PHASE_1) return;
        
        currentPhase = PhaseType.PHASE_2;
        isInvulnerable = true;
        
        // Animación de transición
        Location loc = guardian.getLocation();
        World world = loc.getWorld();
        
        // Mensaje global
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§5§l⚠ FASE 2 ⚠", "§7El Guardián se enfurece...", 10, 40, 10);
            p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_ROAR, 2.0f, 0.5f);
        }
        
        // Knockback radial
        for (Entity entity : world.getNearbyEntities(loc, 15, 15, 15)) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                Vector knockback = player.getLocation().toVector()
                    .subtract(loc.toVector()).normalize().multiply(2.0).setY(1.0);
                player.setVelocity(knockback);
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
            }
        }
        
        // Explosión visual
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc.clone().add(0, 3, 0), 10, 2, 2, 2);
        world.spawnParticle(Particle.SONIC_BOOM, loc, 5, 3, 3, 3, 0);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.5f);
        
        // Después de 3 segundos, quitar invulnerabilidad
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            isInvulnerable = false;
            startPhase2Mechanics();
        }, 60L);
    }
    
    /**
     * Mecánicas de FASE 2
     */
    private void startPhase2Mechanics() {
        phaseStartTime = System.currentTimeMillis();
        
        // Aumentar velocidad
        guardian.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.45);
        
        // Aura agresiva
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!guardian.isValid() || currentPhase != PhaseType.PHASE_2) {
                    cancel();
                    return;
                }
                
                Location loc = guardian.getLocation();
                loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 3, 0), 15, 1.5, 2, 1.5, 0.05);
                loc.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(0, 2, 0), 10, 1, 1.5, 1, 0.03);
                
                // Aplicar debuff más fuerte cerca
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getWorld().equals(loc.getWorld()) && p.getLocation().distance(loc) < 20) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1, false, false));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        
        // Invocar refuerzos más frecuentemente
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!guardian.isValid() || currentPhase != PhaseType.PHASE_2) {
                    cancel();
                    return;
                }
                
                summonReinforcements(4);
            }
        }.runTaskTimer(plugin, 100L, 400L); // Cada 20 segundos
    }
    
    /**
     * Transición a FASE 3: Desesperada (33-0%)
     */
    private void transitionToPhase3() {
        if (currentPhase != PhaseType.PHASE_2) return;
        
        currentPhase = PhaseType.PHASE_3;
        isInvulnerable = true;
        
        Location loc = guardian.getLocation();
        World world = loc.getWorld();
        
        // Mensaje global épico
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§4§l⚡ FASE FINAL ⚡", "§c§lEl Guardián desata todo su poder", 10, 60, 20);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.3f);
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.5f);
        }
        
        // Oscurecer el mundo
        world.setTime(18000);
        
        // Explosión masiva
        for (int i = 0; i < 3; i++) {
            final int index = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                world.spawnParticle(Particle.EXPLOSION_EMITTER, loc.clone().add(0, 5, 0), 20, 5, 5, 5);
                world.spawnParticle(Particle.END_ROD, loc, 100, 8, 5, 8, 0.3);
                world.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 3.0f, 0.5f);
            }, index * 10L);
        }
        
        // Knockback extremo
        for (Entity entity : world.getNearbyEntities(loc, 20, 20, 20)) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                Vector knockback = player.getLocation().toVector()
                    .subtract(loc.toVector()).normalize().multiply(3.0).setY(1.5);
                player.setVelocity(knockback);
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
            }
        }
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            isInvulnerable = false;
            startPhase3Mechanics();
        }, 80L);
    }
    
    /**
     * Mecánicas de FASE 3
     */
    private void startPhase3Mechanics() {
        phaseStartTime = System.currentTimeMillis();
        
        // Aumentar velocidad y daño
        guardian.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.55);
        guardian.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(32.0);
        
        // Regeneración gradual
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!guardian.isValid() || currentPhase != PhaseType.PHASE_3) {
                    cancel();
                    return;
                }
                
                double currentHealth = guardian.getHealth();
                double maxHealth = guardian.getAttribute(Attribute.MAX_HEALTH).getValue();
                
                if (currentHealth < maxHealth) {
                    guardian.setHealth(Math.min(currentHealth + 5, maxHealth));
                    
                    Location loc = guardian.getLocation();
                    loc.getWorld().spawnParticle(Particle.HEART, loc.clone().add(0, 4, 0), 3, 1, 1, 1);
                }
            }
        }.runTaskTimer(plugin, 0L, 100L); // Cada 5 segundos
        
        // Aura mortal
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!guardian.isValid() || currentPhase != PhaseType.PHASE_3) {
                    cancel();
                    return;
                }
                
                Location loc = guardian.getLocation();
                
                // Partículas intensas
                loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 3, 0), 25, 2, 3, 2, 0.1);
                loc.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(0, 2, 0), 20, 1.5, 2, 1.5, 0.05);
                loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, 1, 0), 15, 1, 1.5, 1, 0.1);
                
                // Debuff severo en área extendida
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getWorld().equals(loc.getWorld()) && p.getLocation().distance(loc) < 25) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 2, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 1, false, false));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        
        // Invocar oleadas constantes
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!guardian.isValid() || currentPhase != PhaseType.PHASE_3) {
                    cancel();
                    return;
                }
                
                summonReinforcements(6);
            }
        }.runTaskTimer(plugin, 100L, 300L); // Cada 15 segundos
    }
    
    /**
     * Invoca refuerzos
     */
    private void summonReinforcements(int count) {
        Location loc = guardian.getLocation();
        
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            Location spawnLoc = loc.clone().add(
                Math.cos(angle) * 8,
                0,
                Math.sin(angle) * 8
            );
            
            // Spawn zombie como refuerzo
            Zombie reinforcement = (Zombie) loc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
            reinforcement.setCustomName("§8Sombra Invocada");
            reinforcement.setCustomNameVisible(true);
            reinforcement.getAttribute(Attribute.MAX_HEALTH).setBaseValue(40.0);
            reinforcement.setHealth(40.0);
            
            // Efectos de spawn
            spawnLoc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, spawnLoc, 30, 1, 1, 1, 0.1);
            spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_ZOMBIE_AMBIENT, 1.0f, 0.5f);
        }
    }
    
    /**
     * Timer de enrage
     */
    private void startEnrageTimer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!guardian.isValid()) {
                    cancel();
                    return;
                }
                
                long elapsedSeconds = (System.currentTimeMillis() - combatStartTime) / 1000;
                
                if (elapsedSeconds >= ENRAGE_TIME_SECONDS && !isEnraged) {
                    triggerEnrage();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 100L); // Check cada 5 segundos
    }
    
    /**
     * Activa el enrage
     */
    private void triggerEnrage() {
        isEnraged = true;
        
        // Mensaje global
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§4§l⚡ FURIA DESATADA ⚡", "§c§l¡El Guardián se ha enfurecido!", 10, 60, 20);
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.3f);
        }
        
        // Aumentar stats
        guardian.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(
            guardian.getAttribute(Attribute.MOVEMENT_SPEED).getValue() * 1.3
        );
        guardian.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(
            guardian.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.5
        );
        
        // Aura de furia
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!guardian.isValid()) {
                    cancel();
                    return;
                }
                
                Location loc = guardian.getLocation();
                loc.getWorld().spawnParticle(Particle.LAVA, loc.clone().add(0, 3, 0), 30, 2, 3, 2, 0);
                loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 2, 0), 20, 1.5, 2, 1.5, 0.1);
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
    
    /**
     * Getters
     */
    public PhaseType getCurrentPhase() {
        return currentPhase;
    }
    
    public boolean isInvulnerable() {
        return isInvulnerable;
    }
    
    public boolean isEnraged() {
        return isEnraged;
    }
    
    public long getPhaseTimeSeconds() {
        return (System.currentTimeMillis() - phaseStartTime) / 1000;
    }
    
    public long getCombatTimeSeconds() {
        return (System.currentTimeMillis() - combatStartTime) / 1000;
    }
}
