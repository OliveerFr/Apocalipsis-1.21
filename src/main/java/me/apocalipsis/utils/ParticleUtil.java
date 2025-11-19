package me.apocalipsis.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Utilidad para spawning de partículas con optimizaciones de performance
 * Reduce duplicación de código y centraliza configuraciones comunes
 */
public class ParticleUtil {
    
    // Configuraciones predefinidas para efectos comunes
    public static class ParticleConfig {
        public final Particle particle;
        public final int count;
        public final double offsetX;
        public final double offsetY;
        public final double offsetZ;
        public final double extra;
        
        public ParticleConfig(Particle particle, int count, double offsetX, double offsetY, double offsetZ, double extra) {
            this.particle = particle;
            this.count = count;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.extra = extra;
        }
        
        // Presets comunes
        public static final ParticleConfig EXPLOSION_SMALL = new ParticleConfig(Particle.EXPLOSION, 5, 1, 1, 1, 0);
        public static final ParticleConfig EXPLOSION_LARGE = new ParticleConfig(Particle.EXPLOSION_EMITTER, 3, 1, 1, 1, 0);
        public static final ParticleConfig FLAME_SMALL = new ParticleConfig(Particle.FLAME, 5, 0.5, 0.5, 0.5, 0.01);
        public static final ParticleConfig FLAME_LARGE = new ParticleConfig(Particle.FLAME, 100, 3, 3, 3, 0.2);
        public static final ParticleConfig LAVA = new ParticleConfig(Particle.LAVA, 2, 0.3, 0.3, 0.3, 0);
        public static final ParticleConfig PORTAL = new ParticleConfig(Particle.PORTAL, 20, 1, 1, 1, 0.5);
        public static final ParticleConfig SMOKE = new ParticleConfig(Particle.CAMPFIRE_COSY_SMOKE, 80, 4, 4, 4, 0.1);
        public static final ParticleConfig END_ROD = new ParticleConfig(Particle.END_ROD, 10, 0.3, 0, 0.3, 0.1);
        public static final ParticleConfig SOUL_FLAME = new ParticleConfig(Particle.SOUL_FIRE_FLAME, 3, 0.2, 0.2, 0.2, 0.05);
    }
    
    /**
     * Spawn partículas básico
     */
    public static void spawn(World world, Location location, Particle particle, int count, 
                            double offsetX, double offsetY, double offsetZ, double extra) {
        if (world == null || location == null) return;
        world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
    }
    
    /**
     * Spawn con configuración predefinida
     */
    public static void spawn(World world, Location location, ParticleConfig config) {
        spawn(world, location, config.particle, config.count, config.offsetX, config.offsetY, config.offsetZ, config.extra);
    }
    
    /**
     * Spawn múltiples efectos en una ubicación (explosión completa)
     */
    public static void spawnExplosion(World world, Location location) {
        spawn(world, location, ParticleConfig.EXPLOSION_LARGE);
        spawn(world, location, ParticleConfig.FLAME_LARGE);
        spawn(world, location, new ParticleConfig(Particle.LAVA, 50, 2, 2, 2, 0));
        spawn(world, location, ParticleConfig.SMOKE);
    }
    
    /**
     * Spawn partículas solo para jugadores específicos (optimización para eventos exclusivos)
     */
    public static void spawnForPlayers(Collection<Player> players, Location location, Particle particle,
                                      int count, double offsetX, double offsetY, double offsetZ, double extra) {
        if (players == null || players.isEmpty()) return;
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
            }
        }
    }
    
    /**
     * Spawn partículas con configuración para jugadores específicos
     */
    public static void spawnForPlayers(Collection<Player> players, Location location, ParticleConfig config) {
        spawnForPlayers(players, location, config.particle, config.count, 
                       config.offsetX, config.offsetY, config.offsetZ, config.extra);
    }
    
    /**
     * Spawn línea de partículas entre dos puntos
     */
    public static void spawnLine(World world, Location start, Location end, Particle particle, 
                                int particlesPerBlock, double offset) {
        if (world == null || start == null || end == null) return;
        if (!start.getWorld().equals(end.getWorld())) return;
        
        double distance = start.distance(end);
        int totalParticles = (int) (distance * particlesPerBlock);
        
        for (int i = 0; i <= totalParticles; i++) {
            double ratio = (double) i / totalParticles;
            double x = start.getX() + (end.getX() - start.getX()) * ratio;
            double y = start.getY() + (end.getY() - start.getY()) * ratio;
            double z = start.getZ() + (end.getZ() - start.getZ()) * ratio;
            
            Location particleLoc = new Location(world, x, y, z);
            world.spawnParticle(particle, particleLoc, 1, offset, offset, offset, 0);
        }
    }
    
    /**
     * Spawn círculo de partículas horizontal
     */
    public static void spawnCircle(World world, Location center, double radius, Particle particle, 
                                   int points, double offsetY) {
        if (world == null || center == null) return;
        
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            
            Location particleLoc = new Location(world, x, center.getY() + offsetY, z);
            world.spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
        }
    }
    
    /**
     * Spawn esfera de partículas
     */
    public static void spawnSphere(World world, Location center, double radius, Particle particle, int density) {
        if (world == null || center == null) return;
        
        int points = density * density;
        double increment = Math.PI * (3.0 - Math.sqrt(5.0)); // Golden angle
        double offset = 2.0 / points;
        
        for (int i = 0; i < points; i++) {
            double y = i * offset - 1 + (offset / 2);
            double r = Math.sqrt(1 - y * y);
            double phi = i * increment;
            
            double x = center.getX() + radius * Math.cos(phi) * r;
            double yPos = center.getY() + radius * y;
            double z = center.getZ() + radius * Math.sin(phi) * r;
            
            Location particleLoc = new Location(world, x, yPos, z);
            world.spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
        }
    }
}
