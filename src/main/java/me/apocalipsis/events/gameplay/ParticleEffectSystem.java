package me.apocalipsis.events.gameplay;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import me.apocalipsis.Apocalipsis;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema avanzado de efectos de partículas para eventos
 * 
 * Funcionalidades:
 * - Trails de partículas siguiendo entidades
 * - Auras pulsantes con ondas expansivas
 * - Partículas flotantes ambientales
 * - Efectos de distorsión visual
 * - Rastros de sombra en el suelo
 * - Símbolos formados con partículas
 */
public class ParticleEffectSystem {
    
    private final Apocalipsis plugin;
    
    // Tracking de efectos activos
    private final Map<UUID, BukkitTask> entityTrails = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> entityAuras = new ConcurrentHashMap<>();
    private final List<BukkitTask> ambientEffects = new ArrayList<>();
    private final Map<String, BukkitTask> symbolEffects = new ConcurrentHashMap<>();
    
    public ParticleEffectSystem(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // PARTICLE TRAILS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Trail de sombras que sigue a una entidad
     */
    public void startShadowTrail(Entity entity, ParticleTrailType type) {
        stopEntityTrail(entity);
        
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final Queue<Location> trail = new LinkedList<>();
            final int maxTrailLength = 20;
            
            @Override
            public void run() {
                if (!entity.isValid()) {
                    cancel();
                    entityTrails.remove(entity.getUniqueId());
                    return;
                }
                
                Location current = entity.getLocation();
                trail.add(current.clone());
                
                if (trail.size() > maxTrailLength) {
                    trail.poll();
                }
                
                // Dibujar trail con fade
                int index = 0;
                for (Location loc : trail) {
                    float alpha = (float) index / maxTrailLength;
                    
                    switch (type) {
                        case SHADOW:
                            loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 0.2, 0), 
                                2, 0.2, 0.1, 0.2, 0);
                            break;
                        case SOUL:
                            loc.getWorld().spawnParticle(Particle.SOUL, loc.clone().add(0, 0.5, 0), 
                                1, 0.1, 0.2, 0.1, 0.02);
                            break;
                        case PORTAL:
                            loc.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(0, 1, 0), 
                                3, 0.2, 0.3, 0.2, 0.1);
                            break;
                        case FLAME:
                            loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 0.3, 0), 
                                1, 0.1, 0.1, 0.1, 0.01);
                            break;
                    }
                    
                    index++;
                }
                
                // Rastro en el suelo cada 5 ticks
                if (ticks % 5 == 0) {
                    Location ground = current.clone();
                    ground.setY(ground.getWorld().getHighestBlockYAt(ground));
                    ground.getWorld().spawnParticle(Particle.SMOKE, ground.clone().add(0, 0.1, 0), 
                        5, 0.3, 0, 0.3, 0.01);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
        
        entityTrails.put(entity.getUniqueId(), task);
    }
    
    /**
     * Detiene el trail de una entidad
     */
    public void stopEntityTrail(Entity entity) {
        BukkitTask task = entityTrails.remove(entity.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
    
    public enum ParticleTrailType {
        SHADOW,
        SOUL,
        PORTAL,
        FLAME
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // AURAS PULSANTES
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Aura pulsante con ondas expansivas
     */
    public void startPulsingAura(Entity entity, AuraStyle style, int radiusBlocks) {
        stopEntityAura(entity);
        
        BukkitTask task = new BukkitRunnable() {
            double phase = 0;
            int pulseTicks = 0;
            
            @Override
            public void run() {
                if (!entity.isValid()) {
                    cancel();
                    entityAuras.remove(entity.getUniqueId());
                    return;
                }
                
                Location center = entity.getLocation().add(0, 1, 0);
                World world = center.getWorld();
                
                // Aura constante
                double radius = 1.5 + Math.sin(phase) * 0.5;
                for (int i = 0; i < 360; i += 15) {
                    double radians = Math.toRadians(i);
                    Location particleLoc = center.clone().add(
                        Math.cos(radians) * radius,
                        Math.sin(phase * 2) * 0.5,
                        Math.sin(radians) * radius
                    );
                    
                    switch (style) {
                        case DARK:
                            world.spawnParticle(Particle.SQUID_INK, particleLoc, 1, 0, 0, 0, 0);
                            world.spawnParticle(Particle.SMOKE, particleLoc, 1, 0.1, 0.1, 0.1, 0);
                            break;
                        case MYSTIC:
                            world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                            world.spawnParticle(Particle.REVERSE_PORTAL, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                            break;
                        case ETHEREAL:
                            world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0, 0, 0, 0);
                            world.spawnParticle(Particle.SOUL, particleLoc, 1, 0.1, 0.1, 0.1, 0.02);
                            break;
                        case CORRUPTED:
                            world.spawnParticle(Particle.ASH, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                            world.spawnParticle(Particle.LAVA, particleLoc, 1, 0, 0, 0, 0);
                            break;
                    }
                }
                
                // Onda expansiva cada 40 ticks (2 segundos)
                if (pulseTicks % 40 == 0) {
                    createExpandingRing(center, radiusBlocks, style);
                    
                    // Sonido de pulso
                    world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.5f, 0.5f);
                }
                
                phase += 0.1;
                pulseTicks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
        
        entityAuras.put(entity.getUniqueId(), task);
    }
    
    /**
     * Crea una onda expansiva
     */
    private void createExpandingRing(Location center, int maxRadius, AuraStyle style) {
        new BukkitRunnable() {
            double currentRadius = 0;
            
            @Override
            public void run() {
                if (currentRadius > maxRadius) {
                    cancel();
                    return;
                }
                
                World world = center.getWorld();
                
                for (int i = 0; i < 360; i += 10) {
                    double radians = Math.toRadians(i);
                    Location particleLoc = center.clone().add(
                        Math.cos(radians) * currentRadius,
                        0,
                        Math.sin(radians) * currentRadius
                    );
                    
                    switch (style) {
                        case DARK:
                            world.spawnParticle(Particle.SQUID_INK, particleLoc, 1, 0, 0, 0, 0);
                            break;
                        case MYSTIC:
                            world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0.2, 0, 0);
                            break;
                        case ETHEREAL:
                            world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0, 0, 0, 0);
                            break;
                        case CORRUPTED:
                            world.spawnParticle(Particle.SMOKE, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                            break;
                    }
                }
                
                currentRadius += 0.5;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * Detiene el aura de una entidad
     */
    public void stopEntityAura(Entity entity) {
        BukkitTask task = entityAuras.remove(entity.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
    
    public enum AuraStyle {
        DARK,       // Negro/gris oscuro
        MYSTIC,     // Púrpura/end rod
        ETHEREAL,   // Azul alma/soul flame
        CORRUPTED   // Rojo/ceniza
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // PARTÍCULAS FLOTANTES AMBIENTALES
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Partículas flotantes en una zona
     */
    public void startAmbientParticles(Location center, int radius, AmbientStyle style) {
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                World world = center.getWorld();
                
                // Spawn random en área
                for (int i = 0; i < 10; i++) {
                    double x = center.getX() + (Math.random() - 0.5) * radius * 2;
                    double y = center.getY() + Math.random() * 10;
                    double z = center.getZ() + (Math.random() - 0.5) * radius * 2;
                    
                    Location particleLoc = new Location(world, x, y, z);
                    
                    switch (style) {
                        case FLOATING_SOULS:
                            world.spawnParticle(Particle.SOUL, particleLoc, 1, 0.1, 0.2, 0.1, 0.01);
                            break;
                        case DUST_MOTES:
                            world.spawnParticle(Particle.ASH, particleLoc, 1, 0, 0.1, 0, 0);
                            break;
                        case VOID_PARTICLES:
                            world.spawnParticle(Particle.REVERSE_PORTAL, particleLoc, 2, 0.1, 0.1, 0.1, 0.05);
                            break;
                        case EMBERS:
                            world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0.05, 0.05, 0.05, 0.01);
                            break;
                    }
                }
                
                // Sonido ambiente cada 5 segundos
                if (ticks % 100 == 0) {
                    world.playSound(center, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.3f, 0.8f);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 5L);
        
        ambientEffects.add(task);
    }
    
    public enum AmbientStyle {
        FLOATING_SOULS,
        DUST_MOTES,
        VOID_PARTICLES,
        EMBERS
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // EFECTOS DE DISTORSIÓN
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Efecto de distorsión visual cerca de entidades
     */
    public void applyDistortionEffect(Player player, int durationTicks, DistortionType type) {
        new BukkitRunnable() {
            int remaining = durationTicks;
            
            @Override
            public void run() {
                if (!player.isOnline() || remaining <= 0) {
                    cancel();
                    return;
                }
                
                Location loc = player.getLocation().add(0, 1.6, 0);
                
                switch (type) {
                    case WARPING:
                        player.spawnParticle(Particle.REVERSE_PORTAL, loc, 20, 1, 1, 1, 0.5);
                        if (remaining % 10 == 0) {
                            player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 1.5f);
                        }
                        break;
                    case GLITCHING:
                        player.spawnParticle(Particle.END_ROD, loc, 10, 0.5, 0.5, 0.5, 0.1);
                        player.spawnParticle(Particle.SQUID_INK, loc, 5, 0.3, 0.3, 0.3, 0);
                        break;
                    case SHADOWY:
                        player.spawnParticle(Particle.SMOKE, loc, 15, 0.7, 0.7, 0.7, 0.02);
                        player.spawnParticle(Particle.ASH, loc, 10, 0.5, 0.5, 0.5, 0);
                        break;
                }
                
                remaining -= 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }
    
    public enum DistortionType {
        WARPING,
        GLITCHING,
        SHADOWY
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SÍMBOLOS CON PARTÍCULAS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Dibuja un símbolo con partículas en el aire
     */
    public void createFloatingSymbol(Location center, SymbolType symbol, int durationSeconds) {
        String key = symbol.name() + "_" + center.hashCode();
        
        BukkitTask existing = symbolEffects.get(key);
        if (existing != null) {
            existing.cancel();
        }
        
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = durationSeconds * 20;
            double rotation = 0;
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    symbolEffects.remove(key);
                    cancel();
                    return;
                }
                
                World world = center.getWorld();
                
                switch (symbol) {
                    case CIRCLE:
                        drawCircle(center, 2.0, rotation, world);
                        break;
                    case PENTAGRAM:
                        drawPentagram(center, 2.0, rotation, world);
                        break;
                    case SPIRAL:
                        drawSpiral(center, 2.0, rotation, world);
                        break;
                    case RUNES:
                        drawRunes(center, 1.5, rotation, world);
                        break;
                }
                
                rotation += 0.05;
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
        
        symbolEffects.put(key, task);
    }
    
    private void drawCircle(Location center, double radius, double rotation, World world) {
        for (int i = 0; i < 360; i += 5) {
            double radians = Math.toRadians(i + rotation * 10);
            Location particleLoc = center.clone().add(
                Math.cos(radians) * radius,
                Math.sin(rotation * 5) * 0.3,
                Math.sin(radians) * radius
            );
            world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
        }
    }
    
    private void drawPentagram(Location center, double size, double rotation, World world) {
        double[][] points = {
            {0, 1}, {0.95, 0.31}, {0.59, -0.81}, {-0.59, -0.81}, {-0.95, 0.31}
        };
        
        for (int i = 0; i < 5; i++) {
            int next = (i + 2) % 5;
            drawLine(
                center.clone().add(points[i][0] * size, Math.sin(rotation) * 0.5, points[i][1] * size),
                center.clone().add(points[next][0] * size, Math.sin(rotation) * 0.5, points[next][1] * size),
                world
            );
        }
    }
    
    private void drawSpiral(Location center, double maxRadius, double rotation, World world) {
        for (double r = 0; r < maxRadius; r += 0.1) {
            double angle = r * 2 + rotation * 5;
            Location particleLoc = center.clone().add(
                Math.cos(angle) * r,
                r * 0.5,
                Math.sin(angle) * r
            );
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0, 0, 0, 0);
        }
    }
    
    private void drawRunes(Location center, double radius, double rotation, World world) {
        String runes = "ᚠᚢᚦᚨᚱᚲ";
        int count = 6;
        
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i + rotation;
            Location particleLoc = center.clone().add(
                Math.cos(angle) * radius,
                Math.sin(rotation * 3) * 0.5,
                Math.sin(angle) * radius
            );
            
            // Cluster de partículas para simular runa
            for (int j = 0; j < 5; j++) {
                world.spawnParticle(Particle.ENCHANT, 
                    particleLoc.clone().add(
                        (Math.random() - 0.5) * 0.3,
                        (Math.random() - 0.5) * 0.3,
                        (Math.random() - 0.5) * 0.3
                    ), 1, 0, 0, 0, 0);
            }
        }
    }
    
    private void drawLine(Location start, Location end, World world) {
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();
        direction.normalize();
        
        for (double d = 0; d < distance; d += 0.2) {
            Location particleLoc = start.clone().add(direction.clone().multiply(d));
            world.spawnParticle(Particle.REVERSE_PORTAL, particleLoc, 1, 0, 0, 0, 0);
        }
    }
    
    public enum SymbolType {
        CIRCLE,
        PENTAGRAM,
        SPIRAL,
        RUNES
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Limpia efectos de una entidad
     */
    public void cleanupEntity(Entity entity) {
        stopEntityTrail(entity);
        stopEntityAura(entity);
    }
    
    /**
     * Limpia todos los efectos activos
     */
    public void cleanupAll() {
        entityTrails.values().forEach(BukkitTask::cancel);
        entityTrails.clear();
        
        entityAuras.values().forEach(BukkitTask::cancel);
        entityAuras.clear();
        
        ambientEffects.forEach(BukkitTask::cancel);
        ambientEffects.clear();
        
        symbolEffects.values().forEach(BukkitTask::cancel);
        symbolEffects.clear();
    }
}
