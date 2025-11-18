package me.apocalipsis.events.gameplay;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import me.apocalipsis.Apocalipsis;

import java.util.*;

/**
 * Sistema de ataques telegrafados para el boss.
 * 
 * Los ataques muestran indicadores visuales antes de ejecutarse,
 * dando a los jugadores tiempo para reaccionar y esquivar.
 * 
 * Tipos de ataques:
 * - SLAM: Golpe de área circular
 * - BEAM: Rayo en línea recta
 * - CONE: Ataque en cono frontal
 * - RAIN: Lluvia de proyectiles en área
 * - PULSE: Onda expansiva circular
 * - CHARGE: Embestida en dirección
 */
public class TelegraphedAttack {
    
    private final Apocalipsis plugin;
    private final Random random = new Random();
    
    public TelegraphedAttack(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    public enum AttackType {
        SLAM,       // Golpe al suelo - círculo
        BEAM,       // Rayo láser - línea
        CONE,       // Cono frontal
        RAIN,       // Lluvia de proyectiles
        PULSE,      // Onda expansiva
        CHARGE      // Embestida
    }
    
    /**
     * Ejecuta un ataque telegrafado desde el boss
     */
    public void executeAttack(LivingEntity boss, AttackType type, AttackCallback callback) {
        switch (type) {
            case SLAM:
                executeSlamAttack(boss, callback);
                break;
            case BEAM:
                executeBeamAttack(boss, callback);
                break;
            case CONE:
                executeConeAttack(boss, callback);
                break;
            case RAIN:
                executeRainAttack(boss, callback);
                break;
            case PULSE:
                executePulseAttack(boss, callback);
                break;
            case CHARGE:
                executeChargeAttack(boss, callback);
                break;
        }
    }
    
    /**
     * SLAM - Golpe circular al suelo
     */
    private void executeSlamAttack(LivingEntity boss, AttackCallback callback) {
        Location center = boss.getLocation();
        World world = center.getWorld();
        
        // Fase 1: Telegraph (2 segundos)
        new BukkitRunnable() {
            int ticks = 0;
            final int telegraphDuration = 40; // 2 segundos
            final double radius = 8.0;
            
            @Override
            public void run() {
                if (!boss.isValid() || ticks >= telegraphDuration) {
                    if (boss.isValid()) {
                        executeImpact();
                    }
                    cancel();
                    return;
                }
                
                // Intensidad aumenta con el tiempo
                float intensity = (float) ticks / telegraphDuration;
                
                // Advertencia visual: círculo rojo de partículas en el suelo
                for (int i = 0; i < 360; i += 10) {
                    double angle = Math.toRadians(i);
                    double x = center.getX() + Math.cos(angle) * radius;
                    double z = center.getZ() + Math.sin(angle) * radius;
                    Location particleLoc = new Location(world, x, center.getY() + 0.1, z);
                    
                    if (intensity > 0.7f) {
                        world.spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);
                    } else {
                        world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, 
                            new Particle.DustOptions(Color.RED, 1.0f));
                    }
                }
                
                // Advertencia auditiva creciente
                if (ticks % 10 == 0) {
                    float pitch = 0.5f + (intensity * 1.0f);
                    world.playSound(center, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, pitch);
                }
                
                // Boss levanta brazos (si es humanoide)
                if (boss instanceof Zombie || boss instanceof Skeleton) {
                    // Animación sugerida visualmente
                }
                
                ticks++;
            }
            
            private void executeImpact() {
                // Efectos de impacto
                world.spawnParticle(Particle.EXPLOSION, center, 20, radius * 0.5, 0.5, radius * 0.5, 0);
                world.spawnParticle(Particle.LAVA, center, 50, radius, 1, radius, 0);
                world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                world.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 2.0f, 0.5f);
                
                // Daño a jugadores en área
                List<Player> hitPlayers = new ArrayList<>();
                for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
                    if (entity instanceof Player) {
                        Player player = (Player) entity;
                        double distance = player.getLocation().distance(center);
                        
                        if (distance <= radius) {
                            // Daño escalado por distancia
                            double damageMult = 1.0 - (distance / radius);
                            double damage = 10.0 * damageMult;
                            
                            player.damage(damage, boss);
                            player.setVelocity(player.getLocation().toVector()
                                .subtract(center.toVector()).normalize().multiply(1.5).setY(0.8));
                            
                            hitPlayers.add(player);
                        }
                    }
                }
                
                callback.onAttackComplete(AttackType.SLAM, hitPlayers);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * BEAM - Rayo láser en línea recta
     */
    private void executeBeamAttack(LivingEntity boss, AttackCallback callback) {
        Location start = boss.getEyeLocation();
        Vector direction = start.getDirection();
        World world = start.getWorld();
        
        new BukkitRunnable() {
            int ticks = 0;
            final int telegraphDuration = 30; // 1.5 segundos
            final double beamLength = 20.0;
            
            @Override
            public void run() {
                if (!boss.isValid() || ticks >= telegraphDuration) {
                    if (boss.isValid()) {
                        fireBeam();
                    }
                    cancel();
                    return;
                }
                
                // Telegraph: línea de partículas rojas
                for (double d = 0; d < beamLength; d += 0.5) {
                    Location particleLoc = start.clone().add(direction.clone().multiply(d));
                    float intensity = (float) ticks / telegraphDuration;
                    
                    if (intensity > 0.6f) {
                        world.spawnParticle(Particle.FLAME, particleLoc, 1, 0.1, 0.1, 0.1, 0);
                    } else {
                        world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.ORANGE, 0.8f));
                    }
                }
                
                // Sonido de carga
                if (ticks % 5 == 0) {
                    float pitch = 0.8f + ((float) ticks / telegraphDuration) * 0.8f;
                    world.playSound(start, Sound.BLOCK_BEACON_AMBIENT, 0.5f, pitch);
                }
                
                ticks++;
            }
            
            private void fireBeam() {
                // Efectos visuales del beam
                world.playSound(start, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2.0f, 1.5f);
                world.playSound(start, Sound.ENTITY_BLAZE_SHOOT, 2.0f, 0.5f);
                
                List<Player> hitPlayers = new ArrayList<>();
                
                // Trazar el beam
                for (double d = 0; d < beamLength; d += 0.3) {
                    Location loc = start.clone().add(direction.clone().multiply(d));
                    
                    world.spawnParticle(Particle.FLAME, loc, 3, 0.2, 0.2, 0.2, 0.1);
                    world.spawnParticle(Particle.LAVA, loc, 1, 0, 0, 0, 0);
                    
                    // Chequear colisión con jugadores
                    for (Entity entity : world.getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                        if (entity instanceof Player && !hitPlayers.contains(entity)) {
                            Player player = (Player) entity;
                            player.damage(12.0, boss);
                            player.setFireTicks(100);
                            hitPlayers.add(player);
                        }
                    }
                    
                    // Chequear colisión con bloques
                    if (loc.getBlock().getType().isSolid()) {
                        world.spawnParticle(Particle.EXPLOSION, loc, 1);
                        break;
                    }
                }
                
                callback.onAttackComplete(AttackType.BEAM, hitPlayers);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * CONE - Ataque en cono frontal
     */
    private void executeConeAttack(LivingEntity boss, AttackCallback callback) {
        Location center = boss.getLocation();
        Vector direction = center.getDirection();
        World world = center.getWorld();
        
        new BukkitRunnable() {
            int ticks = 0;
            final int telegraphDuration = 35;
            final double coneLength = 12.0;
            final double coneAngle = 45.0; // Grados
            
            @Override
            public void run() {
                if (!boss.isValid() || ticks >= telegraphDuration) {
                    if (boss.isValid()) {
                        executeCone();
                    }
                    cancel();
                    return;
                }
                
                // Telegraph: cono de partículas
                for (double dist = 2; dist < coneLength; dist += 1.0) {
                    double width = Math.tan(Math.toRadians(coneAngle)) * dist;
                    
                    for (double angle = -coneAngle; angle <= coneAngle; angle += 5) {
                        Vector rotated = rotateVector(direction.clone(), angle);
                        Location particleLoc = center.clone().add(rotated.multiply(dist));
                        
                        float intensity = (float) ticks / telegraphDuration;
                        Color color = intensity > 0.7f ? Color.RED : Color.ORANGE;
                        
                        world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(color, 1.0f));
                    }
                }
                
                // Boss carga el ataque
                if (ticks % 8 == 0) {
                    world.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.5f);
                }
                
                ticks++;
            }
            
            private void executeCone() {
                world.playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2.0f, 0.8f);
                world.spawnParticle(Particle.SWEEP_ATTACK, center.clone().add(0, 1, 0), 5, 3, 1, 3, 0);
                
                List<Player> hitPlayers = new ArrayList<>();
                
                // Daño en cono
                for (Entity entity : world.getNearbyEntities(center, coneLength, 5, coneLength)) {
                    if (entity instanceof Player) {
                        Player player = (Player) entity;
                        Vector toPlayer = player.getLocation().toVector().subtract(center.toVector()).normalize();
                        
                        // Chequear si está en el cono
                        double angle = Math.toDegrees(direction.angle(toPlayer));
                        if (angle <= coneAngle) {
                            double distance = player.getLocation().distance(center);
                            double damageMult = 1.0 - (distance / coneLength);
                            double damage = 8.0 * damageMult;
                            
                            player.damage(damage, boss);
                            player.setVelocity(toPlayer.multiply(1.2).setY(0.6));
                            hitPlayers.add(player);
                        }
                    }
                }
                
                callback.onAttackComplete(AttackType.CONE, hitPlayers);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * PULSE - Onda expansiva circular
     */
    private void executePulseAttack(LivingEntity boss, AttackCallback callback) {
        Location center = boss.getLocation();
        World world = center.getWorld();
        
        new BukkitRunnable() {
            int ticks = 0;
            final int telegraphDuration = 40;
            final double maxRadius = 12.0;
            
            @Override
            public void run() {
                if (!boss.isValid() || ticks >= telegraphDuration) {
                    if (boss.isValid()) {
                        releasePulse();
                    }
                    cancel();
                    return;
                }
                
                // Telegraph: ondas crecientes
                double telegraphRadius = (maxRadius * ticks) / telegraphDuration;
                
                for (int i = 0; i < 360; i += 8) {
                    double angle = Math.toRadians(i);
                    double x = center.getX() + Math.cos(angle) * telegraphRadius;
                    double z = center.getZ() + Math.sin(angle) * telegraphRadius;
                    Location particleLoc = new Location(world, x, center.getY() + 0.5, z);
                    
                    world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.PURPLE, 1.2f));
                }
                
                // Sonido pulsante
                if (ticks % 10 == 0) {
                    world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 1.5f, 0.5f);
                }
                
                // Boss levita
                if (boss.isOnGround()) {
                    boss.setVelocity(new Vector(0, 0.2, 0));
                }
                
                ticks++;
            }
            
            private void releasePulse() {
                world.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 3.0f, 0.5f);
                world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.8f);
                
                List<Player> hitPlayers = new ArrayList<>();
                
                // Onda expansiva animada
                new BukkitRunnable() {
                    double currentRadius = 0;
                    
                    @Override
                    public void run() {
                        if (currentRadius >= maxRadius) {
                            cancel();
                            callback.onAttackComplete(AttackType.PULSE, hitPlayers);
                            return;
                        }
                        
                        // Visuales de la onda
                        for (int i = 0; i < 360; i += 5) {
                            double angle = Math.toRadians(i);
                            double x = center.getX() + Math.cos(angle) * currentRadius;
                            double z = center.getZ() + Math.sin(angle) * currentRadius;
                            Location particleLoc = new Location(world, x, center.getY() + 0.5, z);
                            
                            world.spawnParticle(Particle.SONIC_BOOM, particleLoc, 1, 0, 0, 0, 0);
                            world.spawnParticle(Particle.CLOUD, particleLoc, 2, 0.2, 0.2, 0.2, 0.1);
                        }
                        
                        // Daño a jugadores en el anillo actual
                        for (Entity entity : world.getNearbyEntities(center, currentRadius + 2, 5, currentRadius + 2)) {
                            if (entity instanceof Player && !hitPlayers.contains(entity)) {
                                Player player = (Player) entity;
                                double distance = player.getLocation().distance(center);
                                
                                if (Math.abs(distance - currentRadius) <= 2.0) {
                                    player.damage(15.0, boss);
                                    
                                    // Knockback radial
                                    Vector knockback = player.getLocation().toVector()
                                        .subtract(center.toVector()).normalize().multiply(2.0).setY(1.0);
                                    player.setVelocity(knockback);
                                    
                                    // Stun
                                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 5));
                                    
                                    hitPlayers.add(player);
                                }
                            }
                        }
                        
                        currentRadius += 2.0;
                    }
                }.runTaskTimer(plugin, 0L, 2L);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * RAIN - Lluvia de proyectiles en área
     */
    private void executeRainAttack(LivingEntity boss, AttackCallback callback) {
        Location center = boss.getLocation();
        World world = center.getWorld();
        
        new BukkitRunnable() {
            int ticks = 0;
            final int telegraphDuration = 45;
            final double radius = 15.0;
            
            @Override
            public void run() {
                if (!boss.isValid() || ticks >= telegraphDuration) {
                    if (boss.isValid()) {
                        executeRain();
                    }
                    cancel();
                    return;
                }
                
                // Telegraph: marcas en el suelo donde caerán proyectiles
                if (ticks % 5 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double dist = Math.random() * radius;
                        double x = center.getX() + Math.cos(angle) * dist;
                        double z = center.getZ() + Math.sin(angle) * dist;
                        Location markLoc = new Location(world, x, center.getY() + 0.1, z);
                        
                        world.spawnParticle(Particle.DUST, markLoc, 3, 0.3, 0, 0.3, 0,
                            new Particle.DustOptions(Color.ORANGE, 1.5f));
                    }
                }
                
                if (ticks % 15 == 0) {
                    world.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.5f);
                }
                
                ticks++;
            }
            
            private void executeRain() {
                world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 2.0f, 0.8f);
                List<Player> hitPlayers = new ArrayList<>();
                
                // 20 proyectiles caen aleatoriamente
                for (int i = 0; i < 20; i++) {
                    final int index = i;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double angle = Math.random() * Math.PI * 2;
                        double dist = Math.random() * radius;
                        Location impactLoc = center.clone().add(
                            Math.cos(angle) * dist,
                            0,
                            Math.sin(angle) * dist
                        );
                        
                        // Línea de partículas cayendo
                        for (int y = 20; y >= 0; y--) {
                            Location particleLoc = impactLoc.clone().add(0, y, 0);
                            world.spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);
                        }
                        
                        // Explosión al impactar
                        world.spawnParticle(Particle.EXPLOSION, impactLoc, 5, 1, 0.5, 1, 0);
                        world.spawnParticle(Particle.LAVA, impactLoc, 10, 1.5, 0.5, 1.5, 0);
                        world.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
                        
                        // Daño en área pequeña
                        for (Entity entity : world.getNearbyEntities(impactLoc, 2.5, 2.5, 2.5)) {
                            if (entity instanceof Player && !hitPlayers.contains(entity)) {
                                Player player = (Player) entity;
                                player.damage(6.0, boss);
                                player.setFireTicks(60);
                                hitPlayers.add(player);
                            }
                        }
                    }, index * 3L);
                }
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    callback.onAttackComplete(AttackType.RAIN, hitPlayers);
                }, 60L);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * CHARGE - Embestida en dirección
     */
    private void executeChargeAttack(LivingEntity boss, AttackCallback callback) {
        Location start = boss.getLocation();
        Vector direction = start.getDirection().normalize();
        World world = start.getWorld();
        
        new BukkitRunnable() {
            int ticks = 0;
            final int telegraphDuration = 35;
            final double chargeDistance = 20.0;
            
            @Override
            public void run() {
                if (!boss.isValid() || ticks >= telegraphDuration) {
                    if (boss.isValid()) {
                        executeCharge();
                    }
                    cancel();
                    return;
                }
                
                // Telegraph: línea de partículas mostrando la trayectoria
                for (double d = 0; d < chargeDistance; d += 1.0) {
                    Location particleLoc = start.clone().add(direction.clone().multiply(d));
                    world.spawnParticle(Particle.DUST, particleLoc, 2, 0.3, 0.5, 0.3, 0,
                        new Particle.DustOptions(Color.YELLOW, 1.2f));
                }
                
                // Boss preparándose
                if (ticks % 10 == 0) {
                    world.playSound(start, Sound.ENTITY_RAVAGER_STEP, 2.0f, 0.8f);
                }
                
                ticks++;
            }
            
            private void executeCharge() {
                world.playSound(start, Sound.ENTITY_RAVAGER_ATTACK, 2.0f, 0.5f);
                world.playSound(start, Sound.ENTITY_IRON_GOLEM_ATTACK, 2.0f, 0.8f);
                
                List<Player> hitPlayers = new ArrayList<>();
                
                // Embestida animada
                new BukkitRunnable() {
                    double distance = 0;
                    Location currentLoc = start.clone();
                    
                    @Override
                    public void run() {
                        if (distance >= chargeDistance || !boss.isValid()) {
                            cancel();
                            callback.onAttackComplete(AttackType.CHARGE, hitPlayers);
                            return;
                        }
                        
                        // Mover al boss
                        currentLoc.add(direction.clone().multiply(1.5));
                        boss.teleport(currentLoc);
                        
                        // Efectos visuales del charge
                        world.spawnParticle(Particle.CLOUD, currentLoc, 10, 0.5, 0.5, 0.5, 0.1);
                        world.spawnParticle(Particle.CRIT, currentLoc, 5, 0.5, 0.5, 0.5, 0);
                        world.playSound(currentLoc, Sound.ENTITY_RAVAGER_STEP, 1.0f, 1.0f);
                        
                        // Daño a jugadores en el camino
                        for (Entity entity : world.getNearbyEntities(currentLoc, 3, 3, 3)) {
                            if (entity instanceof Player && !hitPlayers.contains(entity)) {
                                Player player = (Player) entity;
                                player.damage(18.0, boss);
                                
                                // Knockback en dirección del charge
                                player.setVelocity(direction.clone().multiply(2.5).setY(1.0));
                                hitPlayers.add(player);
                            }
                        }
                        
                        // Chequear colisión con bloques
                        if (currentLoc.getBlock().getType().isSolid()) {
                            world.spawnParticle(Particle.EXPLOSION, currentLoc, 3);
                            world.playSound(currentLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
                            cancel();
                            callback.onAttackComplete(AttackType.CHARGE, hitPlayers);
                        }
                        
                        distance += 1.5;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * Rota un vector alrededor del eje Y
     */
    private Vector rotateVector(Vector vector, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        
        return new Vector(x, vector.getY(), z);
    }
    
    /**
     * Callback para resultados de ataque
     */
    public interface AttackCallback {
        void onAttackComplete(AttackType type, List<Player> hitPlayers);
    }
}
