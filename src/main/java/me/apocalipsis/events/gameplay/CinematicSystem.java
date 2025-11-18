package me.apocalipsis.events.gameplay;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import me.apocalipsis.Apocalipsis;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema cinematográfico avanzado para eventos
 * 
 * Características:
 * - FOV effects (zoom in/out)
 * - Camera rotation (teleport-based)
 * - Letterbox effect (partículas negras)
 * - Camera shake (3 intensidades)
 * - Blur effect (nausea controlada)
 * - Slow motion (slowness + mensaje)
 * - Freeze frame (inmovilización temporal)
 */
public class CinematicSystem {
    
    private final Apocalipsis plugin;
    private final Map<UUID, Integer> originalFOV = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> activeCinematicsTask = new ConcurrentHashMap<>();
    private final Map<UUID, Location> frozenLocations = new ConcurrentHashMap<>();
    
    public enum ShakeIntensity {
        LIGHT(0.05, 2, "leve"),
        MEDIUM(0.15, 3, "medio"),
        EXTREME(0.35, 5, "extremo");
        
        public final double magnitude;
        public final int tickInterval;
        public final String displayName;
        
        ShakeIntensity(double magnitude, int tickInterval, String displayName) {
            this.magnitude = magnitude;
            this.tickInterval = tickInterval;
            this.displayName = displayName;
        }
    }
    
    public CinematicSystem(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Efecto de zoom gradual (FOV effect)
     * @param player Jugador
     * @param targetFOV FOV destino (0.0 = muy alejado, 1.0 = normal, 2.0 = muy cerca)
     * @param durationTicks Duración de la transición en ticks
     */
    public void smoothZoom(Player player, float targetFOV, int durationTicks) {
        UUID uuid = player.getUniqueId();
        
        // Cancelar zoom anterior
        BukkitTask previousTask = activeCinematicsTask.remove(uuid);
        if (previousTask != null) previousTask.cancel();
        
        // Guardar FOV original si es la primera vez
        originalFOV.putIfAbsent(uuid, 1); // FOV normal = 1
        
        // Calcular pasos de transición
        int steps = durationTicks / 2;
        float fovDelta = (targetFOV - 1.0f) / steps;
        
        BukkitTask zoomTask = new BukkitRunnable() {
            private int currentStep = 0;
            
            @Override
            public void run() {
                if (!player.isOnline() || currentStep >= steps) {
                    this.cancel();
                    activeCinematicsTask.remove(uuid);
                    return;
                }
                
                // Simular FOV con efectos de velocidad y salto
                float currentFOV = 1.0f + (fovDelta * currentStep);
                
                if (currentFOV < 1.0f) {
                    // Zoom in: reducir FOV (slowness + jump boost negativo)
                    int slowLevel = (int) ((1.0f - currentFOV) * 3);
                    player.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS, 40, slowLevel, false, false
                    ));
                    
                } else if (currentFOV > 1.0f) {
                    // Zoom out: aumentar FOV (speed)
                    int speedLevel = (int) ((currentFOV - 1.0f) * 2);
                    player.addPotionEffect(new PotionEffect(
                        PotionEffectType.SPEED, 40, speedLevel, false, false
                    ));
                }
                
                currentStep++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
        
        activeCinematicsTask.put(uuid, zoomTask);
    }
    
    /**
     * Resetea FOV a normal
     */
    public void resetZoom(Player player) {
        UUID uuid = player.getUniqueId();
        
        BukkitTask task = activeCinematicsTask.remove(uuid);
        if (task != null) task.cancel();
        
        // Remover efectos
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.SPEED);
        
        originalFOV.remove(uuid);
    }
    
    /**
     * Rotación de cámara forzada (teleport-based)
     * @param player Jugador
     * @param center Centro de rotación
     * @param degreesPerSecond Grados por segundo
     * @param durationSeconds Duración en segundos
     * @param radius Radio de la órbita
     * @param heightOffset Altura sobre el centro
     */
    public void orbitCamera(Player player, Location center, double degreesPerSecond, 
                           int durationSeconds, double radius, double heightOffset) {
        
        UUID uuid = player.getUniqueId();
        
        // Cancelar rotación anterior
        BukkitTask previousTask = activeCinematicsTask.remove(uuid);
        if (previousTask != null) previousTask.cancel();
        
        // Inmovilizar jugador
        frozenLocations.put(uuid, player.getLocation().clone());
        
        BukkitTask orbitTask = new BukkitRunnable() {
            private int ticksElapsed = 0;
            private final int totalTicks = durationSeconds * 20;
            private final double degreesPerTick = degreesPerSecond / 20.0;
            private double currentAngle = 0;
            
            @Override
            public void run() {
                if (!player.isOnline() || ticksElapsed >= totalTicks) {
                    frozenLocations.remove(uuid);
                    activeCinematicsTask.remove(uuid);
                    this.cancel();
                    return;
                }
                
                // Calcular nueva posición orbital
                double radians = Math.toRadians(currentAngle);
                double x = center.getX() + (Math.cos(radians) * radius);
                double z = center.getZ() + (Math.sin(radians) * radius);
                double y = center.getY() + heightOffset;
                
                Location newLoc = new Location(center.getWorld(), x, y, z);
                
                // Calcular dirección hacia el centro
                Vector direction = center.toVector().subtract(newLoc.toVector()).normalize();
                newLoc.setDirection(direction);
                
                // Teleportar jugador
                player.teleport(newLoc);
                
                currentAngle += degreesPerTick;
                ticksElapsed++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        activeCinematicsTask.put(uuid, orbitTask);
    }
    
    /**
     * Efecto de barras negras cinematográficas (letterbox)
     */
    public void showLetterbox(Player player, int durationTicks) {
        UUID uuid = player.getUniqueId();
        
        // Cancelar letterbox anterior
        BukkitTask previousTask = activeCinematicsTask.remove(uuid);
        if (previousTask != null) previousTask.cancel();
        
        BukkitTask letterboxTask = new BukkitRunnable() {
            private int ticksRemaining = durationTicks;
            
            @Override
            public void run() {
                if (!player.isOnline() || ticksRemaining <= 0) {
                    activeCinematicsTask.remove(uuid);
                    this.cancel();
                    return;
                }
                
                Location eyeLoc = player.getEyeLocation();
                
                // Barra superior (partículas negras)
                for (double x = -2; x <= 2; x += 0.2) {
                    Location topLoc = eyeLoc.clone().add(
                        player.getLocation().getDirection().multiply(1.5)
                    ).add(x * 0.3, 1.5, 0);
                    
                    player.spawnParticle(Particle.SQUID_INK, topLoc, 3, 0.1, 0.05, 0.1, 0);
                }
                
                // Barra inferior
                for (double x = -2; x <= 2; x += 0.2) {
                    Location bottomLoc = eyeLoc.clone().add(
                        player.getLocation().getDirection().multiply(1.5)
                    ).add(x * 0.3, -1.2, 0);
                    
                    player.spawnParticle(Particle.SQUID_INK, bottomLoc, 3, 0.1, 0.05, 0.1, 0);
                }
                
                ticksRemaining--;
            }
        }.runTaskTimer(plugin, 0L, 2L);
        
        activeCinematicsTask.put(uuid, letterboxTask);
    }
    
    /**
     * Camera shake con intensidad variable
     */
    public void cameraShake(Player player, ShakeIntensity intensity, int durationTicks) {
        UUID uuid = player.getUniqueId();
        
        // Cancelar shake anterior
        BukkitTask previousTask = activeCinematicsTask.remove(uuid);
        if (previousTask != null) previousTask.cancel();
        
        Location originalLoc = player.getLocation().clone();
        
        BukkitTask shakeTask = new BukkitRunnable() {
            private int ticksRemaining = durationTicks;
            private final Random random = new Random();
            
            @Override
            public void run() {
                if (!player.isOnline() || ticksRemaining <= 0) {
                    // Restaurar posición original
                    if (player.isOnline()) {
                        player.teleport(originalLoc);
                    }
                    activeCinematicsTask.remove(uuid);
                    this.cancel();
                    return;
                }
                
                // Generar offset aleatorio
                double offsetX = (random.nextDouble() - 0.5) * intensity.magnitude;
                double offsetY = (random.nextDouble() - 0.5) * intensity.magnitude * 0.5;
                double offsetZ = (random.nextDouble() - 0.5) * intensity.magnitude;
                
                // Aplicar shake manteniendo la dirección
                Location shakeLoc = player.getLocation().clone();
                shakeLoc.add(offsetX, offsetY, offsetZ);
                player.teleport(shakeLoc);
                
                // Efecto visual
                if (ticksRemaining % 5 == 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_STONE_HIT, 0.3f, 0.8f);
                }
                
                ticksRemaining--;
            }
        }.runTaskTimer(plugin, 0L, intensity.tickInterval);
        
        activeCinematicsTask.put(uuid, shakeTask);
    }
    
    /**
     * Efecto de blur (desenfoque con nausea)
     */
    public void applyBlur(Player player, int intensity, int durationTicks) {
        UUID uuid = player.getUniqueId();
        
        // Nausea controlada para simular blur
        int nauseaLevel = Math.max(0, Math.min(3, intensity - 1));
        
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.NAUSEA,
            durationTicks,
            nauseaLevel,
            false,
            false
        ));
        
        // Efecto visual adicional con partículas
        BukkitTask blurTask = new BukkitRunnable() {
            private int ticksRemaining = durationTicks;
            
            @Override
            public void run() {
                if (!player.isOnline() || ticksRemaining <= 0) {
                    activeCinematicsTask.remove(uuid);
                    this.cancel();
                    return;
                }
                
                // Partículas alrededor de la visión
                Location eyeLoc = player.getEyeLocation();
                player.spawnParticle(Particle.SMOKE, eyeLoc.clone().add(
                    player.getLocation().getDirection().multiply(1)
                ), 8, 0.5, 0.5, 0.5, 0.01);
                
                ticksRemaining -= 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
        
        activeCinematicsTask.put(uuid, blurTask);
    }
    
    /**
     * Efecto de cámara lenta (slow motion)
     */
    public void slowMotion(Player player, int durationTicks) {
        UUID uuid = player.getUniqueId();
        
        // Slowness extrema
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.SLOWNESS,
            durationTicks,
            4,
            false,
            false
        ));
        
        // Mining fatigue para efecto completo
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.MINING_FATIGUE,
            durationTicks,
            2,
            false,
            false
        ));
        
        // Sonido ralentizado
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 0.3f);
        
        // Mensaje visual
        net.kyori.adventure.text.Component slowmoText = net.kyori.adventure.text.Component.text(
            "⏱ CÁMARA LENTA ⏱"
        ).color(net.kyori.adventure.text.format.NamedTextColor.GRAY);
        
        player.sendActionBar(slowmoText);
    }
    
    /**
     * Freeze frame - congela al jugador en el lugar
     */
    public void freezeFrame(Player player, int durationTicks) {
        UUID uuid = player.getUniqueId();
        
        // Cancelar freeze anterior
        BukkitTask previousTask = activeCinematicsTask.remove(uuid);
        if (previousTask != null) previousTask.cancel();
        
        Location frozenLoc = player.getLocation().clone();
        frozenLocations.put(uuid, frozenLoc);
        
        // Efectos de inmovilización
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.SLOWNESS,
            durationTicks,
            255,
            false,
            false
        ));
        
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.JUMP_BOOST,
            durationTicks,
            -255,
            false,
            false
        ));
        
        BukkitTask freezeTask = new BukkitRunnable() {
            private int ticksRemaining = durationTicks;
            
            @Override
            public void run() {
                if (!player.isOnline() || ticksRemaining <= 0) {
                    frozenLocations.remove(uuid);
                    activeCinematicsTask.remove(uuid);
                    this.cancel();
                    return;
                }
                
                // Forzar posición congelada
                player.teleport(frozenLoc);
                player.setVelocity(new Vector(0, 0, 0));
                
                // Efecto visual de "tiempo detenido"
                if (ticksRemaining % 10 == 0) {
                    player.spawnParticle(Particle.END_ROD, 
                        player.getLocation().clone().add(0, 1, 0), 
                        10, 0.5, 1, 0.5, 0.02);
                }
                
                ticksRemaining--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        activeCinematicsTask.put(uuid, freezeTask);
    }
    
    /**
     * Cinemática completa: Combina múltiples efectos
     */
    public void playFullCinematic(Player player, Location focusPoint, int durationSeconds) {
        UUID uuid = player.getUniqueId();
        
        // Fase 1: Zoom in + letterbox (primeros 3 segundos)
        smoothZoom(player, 0.5f, 60);
        showLetterbox(player, 60);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            // Fase 2: Órbita alrededor del punto (4 segundos)
            orbitCamera(player, focusPoint, 45, 4, 5, 3);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                
                // Fase 3: Camera shake + blur (2 segundos)
                cameraShake(player, ShakeIntensity.MEDIUM, 40);
                applyBlur(player, 2, 40);
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline()) return;
                    
                    // Fase 4: Reset
                    resetZoom(player);
                    cleanupPlayer(uuid);
                    
                }, 40L);
            }, 80L);
        }, 60L);
    }
    
    /**
     * Limpia efectos cinematográficos de un jugador
     */
    public void cleanupPlayer(UUID uuid) {
        BukkitTask task = activeCinematicsTask.remove(uuid);
        if (task != null) task.cancel();
        
        frozenLocations.remove(uuid);
        originalFOV.remove(uuid);
        
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.NAUSEA);
            player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        }
    }
    
    /**
     * Limpia todos los efectos cinematográficos
     */
    public void cleanupAll() {
        for (UUID uuid : new HashSet<>(activeCinematicsTask.keySet())) {
            cleanupPlayer(uuid);
        }
        
        activeCinematicsTask.clear();
        frozenLocations.clear();
        originalFOV.clear();
    }
}
