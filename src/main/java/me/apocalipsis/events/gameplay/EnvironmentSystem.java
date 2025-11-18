package me.apocalipsis.events.gameplay;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EnvironmentSystem - Sistema de ambiente inmersivo AAA
 * 
 * Características:
 * - Clima dinámico (tormentas oscuras, lluvia sangrienta, niebla ceniza)
 * - Niebla volumétrica con 3 intensidades
 * - Modificaciones temporales del mundo (reversibles)
 * - Efectos atmosféricos (ceniza, wisps, grietas dimensionales)
 * - Control de iluminación y tiempo
 * - Restauración automática al finalizar
 * 
 * @author Apocalipsis Team
 * @version 1.0
 */
public class EnvironmentSystem {
    
    private final Plugin plugin;
    
    // Estado del ambiente por mundo
    private final Map<World, WeatherType> activeWeather = new ConcurrentHashMap<>();
    private final Map<World, FogIntensity> activeFog = new ConcurrentHashMap<>();
    private final Map<World, BukkitTask> weatherTasks = new ConcurrentHashMap<>();
    private final Map<World, BukkitTask> fogTasks = new ConcurrentHashMap<>();
    private final Map<World, BukkitTask> atmosphericTasks = new ConcurrentHashMap<>();
    
    // Tracking de bloques modificados para restauración
    private final Map<Location, BlockData> originalBlocks = new ConcurrentHashMap<>();
    private final Map<World, Long> originalWorldTimes = new ConcurrentHashMap<>();
    private final Map<World, Boolean> originalStorms = new ConcurrentHashMap<>();
    
    // Configuración
    private static final int FOG_PARTICLE_COUNT = 50;
    private static final int ATMOSPHERIC_EFFECT_RADIUS = 32;
    private static final int MAX_WORLD_MODIFICATIONS = 500;
    
    /**
     * Tipos de clima dinámico
     */
    public enum WeatherType {
        CLEAR("Despejado", false, false),
        DARK_STORM("Tormenta Oscura", true, true),
        BLOOD_RAIN("Lluvia Sangrienta", true, false),
        ASHEN_FOG("Niebla de Ceniza", false, false),
        VOID_SKY("Cielo del Vacío", false, false),
        ECLIPSE("Eclipse Total", false, false);
        
        private final String displayName;
        private final boolean hasRain;
        private final boolean hasThunder;
        
        WeatherType(String displayName, boolean hasRain, boolean hasThunder) {
            this.displayName = displayName;
            this.hasRain = hasRain;
            this.hasThunder = hasThunder;
        }
        
        public String getDisplayName() { return displayName; }
        public boolean hasRain() { return hasRain; }
        public boolean hasThunder() { return hasThunder; }
    }
    
    /**
     * Intensidades de niebla
     */
    public enum FogIntensity {
        LIGHT("Ligera", 0.3f, 8, 10),
        MEDIUM("Media", 0.5f, 5, 8),
        DENSE("Densa", 0.8f, 3, 6);
        
        private final String displayName;
        private final float densityFactor;
        private final int viewDistance;      // chunks
        private final int simulationDistance; // chunks
        
        FogIntensity(String displayName, float densityFactor, int viewDistance, int simulationDistance) {
            this.displayName = displayName;
            this.densityFactor = densityFactor;
            this.viewDistance = viewDistance;
            this.simulationDistance = simulationDistance;
        }
        
        public String getDisplayName() { return displayName; }
        public float getDensityFactor() { return densityFactor; }
        public int getViewDistance() { return viewDistance; }
        public int getSimulationDistance() { return simulationDistance; }
    }
    
    /**
     * Efectos atmosféricos
     */
    public enum AtmosphericEffect {
        ASH_FALL("Caída de Ceniza", Particle.ASH, Particle.SMOKE, Sound.BLOCK_FIRE_AMBIENT),
        SHADOW_WISPS("Wisps Oscuros", Particle.SQUID_INK, Particle.SOUL, Sound.ENTITY_VEX_AMBIENT),
        VOID_CRACKS("Grietas del Vacío", Particle.REVERSE_PORTAL, Particle.END_ROD, Sound.BLOCK_PORTAL_AMBIENT),
        CORRUPTION_SPREAD("Corrupción Visual", Particle.CRIMSON_SPORE, Particle.WARPED_SPORE, Sound.BLOCK_NETHER_SPROUTS_BREAK),
        BLOOD_PARTICLES("Partículas de Sangre", Particle.CHERRY_LEAVES, Particle.BLOCK_MARKER, Sound.BLOCK_HONEY_BLOCK_BREAK),
        SOUL_ESSENCE("Esencia de Almas", Particle.SOUL_FIRE_FLAME, Particle.SOUL, Sound.ENTITY_PHANTOM_AMBIENT);
        
        private final String displayName;
        private final Particle primaryParticle;
        private final Particle secondaryParticle;
        private final Sound ambientSound;
        
        AtmosphericEffect(String displayName, Particle primary, Particle secondary, Sound sound) {
            this.displayName = displayName;
            this.primaryParticle = primary;
            this.secondaryParticle = secondary;
            this.ambientSound = sound;
        }
        
        public String getDisplayName() { return displayName; }
        public Particle getPrimaryParticle() { return primaryParticle; }
        public Particle getSecondaryParticle() { return secondaryParticle; }
        public Sound getAmbientSound() { return ambientSound; }
    }
    
    public EnvironmentSystem(Plugin plugin) {
        this.plugin = plugin;
    }
    
    // ==================== CLIMA DINÁMICO ====================
    
    /**
     * Establece un clima dinámico en el mundo
     * 
     * @param world Mundo objetivo
     * @param weatherType Tipo de clima
     * @param durationSeconds Duración en segundos (0 = permanente)
     */
    public void setDynamicWeather(World world, WeatherType weatherType, int durationSeconds) {
        // Guardar estado original
        if (!originalWorldTimes.containsKey(world)) {
            originalWorldTimes.put(world, world.getTime());
            originalStorms.put(world, world.hasStorm());
        }
        
        // Cancelar clima anterior
        clearWeather(world);
        
        activeWeather.put(world, weatherType);
        
        // Aplicar efectos según tipo
        switch (weatherType) {
            case DARK_STORM:
                applyDarkStorm(world);
                break;
            case BLOOD_RAIN:
                applyBloodRain(world);
                break;
            case ASHEN_FOG:
                applyAshenFog(world);
                break;
            case VOID_SKY:
                applyVoidSky(world);
                break;
            case ECLIPSE:
                applyEclipse(world);
                break;
            case CLEAR:
                applyClearWeather(world);
                break;
        }
        
        // Programar limpieza si tiene duración
        if (durationSeconds > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    clearWeather(world);
                }
            }.runTaskLater(plugin, durationSeconds * 20L);
        }
    }
    
    /**
     * Tormenta oscura con rayos frecuentes
     */
    private void applyDarkStorm(World world) {
        world.setStorm(true);
        world.setThundering(true);
        world.setWeatherDuration(Integer.MAX_VALUE);
        world.setTime(18000); // Medianoche
        
        // Rayos periódicos
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (!activeWeather.containsKey(world) || activeWeather.get(world) != WeatherType.DARK_STORM) {
                    cancel();
                    return;
                }
                
                // Rayos aleatorios cada 2-5 segundos
                if (ticks % (40 + new Random().nextInt(60)) == 0) {
                    for (Player p : world.getPlayers()) {
                        Location strikeLoc = p.getLocation().add(
                            (new Random().nextDouble() - 0.5) * 40,
                            0,
                            (new Random().nextDouble() - 0.5) * 40
                        );
                        world.strikeLightningEffect(strikeLoc);
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        weatherTasks.put(world, task);
    }
    
    /**
     * Lluvia sangrienta con partículas rojas
     */
    private void applyBloodRain(World world) {
        world.setStorm(true);
        world.setThundering(false);
        world.setWeatherDuration(Integer.MAX_VALUE);
        world.setTime(13000); // Atardecer
        
        // Partículas de sangre cayendo
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeWeather.containsKey(world) || activeWeather.get(world) != WeatherType.BLOOD_RAIN) {
                    cancel();
                    return;
                }
                
                for (Player p : world.getPlayers()) {
                    Location loc = p.getLocation().add(0, 20, 0);
                    
                    // Lluvia de partículas rojas
                    for (int i = 0; i < 15; i++) {
                        Location particleLoc = loc.clone().add(
                            (new Random().nextDouble() - 0.5) * 10,
                            new Random().nextDouble() * 5,
                            (new Random().nextDouble() - 0.5) * 10
                        );
                        world.spawnParticle(Particle.CHERRY_LEAVES, particleLoc, 3, 0.1, 0.5, 0.1, 0);
                        world.spawnParticle(Particle.DUST, particleLoc, 2, 0.1, 0.5, 0.1, 
                            new Particle.DustOptions(Color.MAROON, 1.5f));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
        
        weatherTasks.put(world, task);
    }
    
    /**
     * Niebla de ceniza con partículas flotantes
     */
    private void applyAshenFog(World world) {
        world.setStorm(false);
        world.setTime(6000); // Día nublado
        
        // Niebla densa + ceniza cayendo
        createVolumetricFog(world, FogIntensity.DENSE, 0);
        spawnAtmosphericEffect(world, AtmosphericEffect.ASH_FALL, 0);
    }
    
    /**
     * Cielo del vacío con oscuridad total
     */
    private void applyVoidSky(World world) {
        world.setStorm(false);
        world.setTime(18000);
        
        // Oscuridad extrema
        for (Player p : world.getPlayers()) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, Integer.MAX_VALUE, 0, false, false));
        }
        
        // Grietas dimensionales
        spawnAtmosphericEffect(world, AtmosphericEffect.VOID_CRACKS, 0);
    }
    
    /**
     * Eclipse total con penumbra
     */
    private void applyEclipse(World world) {
        world.setStorm(false);
        world.setTime(6000); // Mediodía
        
        // Oscurecer con darkness effect
        for (Player p : world.getPlayers()) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, Integer.MAX_VALUE, 1, false, false));
        }
        
        // Wisps oscuros
        spawnAtmosphericEffect(world, AtmosphericEffect.SHADOW_WISPS, 0);
    }
    
    /**
     * Clima despejado normal
     */
    private void applyClearWeather(World world) {
        world.setStorm(false);
        world.setThundering(false);
        world.setWeatherDuration(6000);
    }
    
    /**
     * Limpia el clima de un mundo
     */
    public void clearWeather(World world) {
        // Cancelar tasks
        BukkitTask task = weatherTasks.remove(world);
        if (task != null) {
            task.cancel();
        }
        
        activeWeather.remove(world);
        
        // Restaurar efectos
        for (Player p : world.getPlayers()) {
            p.removePotionEffect(PotionEffectType.DARKNESS);
        }
    }
    
    // ==================== NIEBLA VOLUMÉTRICA ====================
    
    /**
     * Crea niebla volumétrica en el mundo
     * 
     * @param world Mundo objetivo
     * @param intensity Intensidad de la niebla
     * @param durationSeconds Duración (0 = permanente)
     */
    public void createVolumetricFog(World world, FogIntensity intensity, int durationSeconds) {
        activeFog.put(world, intensity);
        
        // Reducir render distance de jugadores
        for (Player p : world.getPlayers()) {
            p.setViewDistance(intensity.getViewDistance());
            p.setSimulationDistance(intensity.getSimulationDistance());
            
            // Ceguera parcial según intensidad
            int blindnessDuration = durationSeconds > 0 ? durationSeconds * 20 : Integer.MAX_VALUE;
            int blindnessLevel = intensity == FogIntensity.DENSE ? 0 : -1;
            
            if (blindnessLevel >= 0) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 
                    blindnessDuration, blindnessLevel, false, false));
            }
        }
        
        // Partículas de niebla
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeFog.containsKey(world)) {
                    cancel();
                    return;
                }
                
                for (Player p : world.getPlayers()) {
                    Location loc = p.getLocation();
                    int particleCount = (int) (FOG_PARTICLE_COUNT * intensity.getDensityFactor());
                    
                    // Capas de niebla
                    for (int layer = 0; layer < 5; layer++) {
                        double y = loc.getY() + layer - 2;
                        
                        for (int i = 0; i < particleCount; i++) {
                            Location fogLoc = new Location(world,
                                loc.getX() + (new Random().nextDouble() - 0.5) * 20,
                                y + new Random().nextDouble() * 2,
                                loc.getZ() + (new Random().nextDouble() - 0.5) * 20
                            );
                            
                            world.spawnParticle(Particle.SQUID_INK, fogLoc, 1, 0.5, 0.1, 0.5, 0);
                            world.spawnParticle(Particle.SMOKE, fogLoc, 2, 0.3, 0.1, 0.3, 0.01);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
        
        fogTasks.put(world, task);
        
        // Programar limpieza
        if (durationSeconds > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    clearFog(world);
                }
            }.runTaskLater(plugin, durationSeconds * 20L);
        }
    }
    
    /**
     * Limpia la niebla de un mundo
     */
    public void clearFog(World world) {
        BukkitTask task = fogTasks.remove(world);
        if (task != null) {
            task.cancel();
        }
        
        activeFog.remove(world);
        
        // Restaurar render distance
        for (Player p : world.getPlayers()) {
            p.setViewDistance(10); // Default
            p.setSimulationDistance(10);
            p.removePotionEffect(PotionEffectType.BLINDNESS);
        }
    }
    
    // ==================== MODIFICACIONES DEL MUNDO ====================
    
    /**
     * Altera bloques del mundo temporalmente
     * 
     * @param center Centro del área
     * @param radius Radio de modificación
     * @param corruptionType Tipo de corrupción
     */
    public void alterWorldTemporarily(Location center, int radius, CorruptionType corruptionType) {
        World world = center.getWorld();
        if (world == null) return;
        
        int modified = 0;
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius/2; y <= radius/2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (modified >= MAX_WORLD_MODIFICATIONS) break;
                    
                    Location loc = center.clone().add(x, y, z);
                    if (loc.distance(center) > radius) continue;
                    
                    Block block = loc.getBlock();
                    Material original = block.getType();
                    
                    // Guardar original
                    if (!originalBlocks.containsKey(loc)) {
                        originalBlocks.put(loc, block.getBlockData());
                    }
                    
                    // Aplicar corrupción
                    Material corrupted = corruptionType.getCorruptedMaterial(original);
                    if (corrupted != null && corrupted != original) {
                        block.setType(corrupted);
                        modified++;
                    }
                }
            }
        }
        
        plugin.getLogger().info("[Environment] Modificados " + modified + " bloques (" + corruptionType.name() + ")");
    }
    
    /**
     * Tipos de corrupción de bloques
     */
    public enum CorruptionType {
        NETHERRACK_SPREAD,  // Grass → Netherrack
        BLOOD_POOLS,        // Water → Red concrete powder
        DEAD_TREES,         // Logs → Stripped logs, Leaves → Air
        FROZEN_WASTELAND,   // Grass → Snow, Water → Ice
        VOID_CORRUPTION;    // Todo → Blackstone/Basalt
        
        public Material getCorruptedMaterial(Material original) {
            switch (this) {
                case NETHERRACK_SPREAD:
                    if (original == Material.GRASS_BLOCK) return Material.NETHERRACK;
                    if (original == Material.DIRT) return Material.SOUL_SOIL;
                    break;
                    
                case BLOOD_POOLS:
                    if (original == Material.WATER) return Material.RED_CONCRETE_POWDER;
                    break;
                    
                case DEAD_TREES:
                    if (original.name().contains("_LOG")) {
                        return Material.valueOf("STRIPPED_" + original.name());
                    }
                    if (original.name().contains("_LEAVES")) return Material.AIR;
                    break;
                    
                case FROZEN_WASTELAND:
                    if (original == Material.GRASS_BLOCK) return Material.SNOW_BLOCK;
                    if (original == Material.WATER) return Material.ICE;
                    if (original == Material.DIRT) return Material.SNOW_BLOCK;
                    break;
                    
                case VOID_CORRUPTION:
                    if (original.isSolid()) {
                        return new Random().nextBoolean() ? Material.BLACKSTONE : Material.BASALT;
                    }
                    break;
            }
            return null;
        }
    }
    
    /**
     * Restaura todos los bloques modificados
     */
    public void restoreAllBlocks() {
        int restored = 0;
        
        for (Map.Entry<Location, BlockData> entry : originalBlocks.entrySet()) {
            Location loc = entry.getKey();
            BlockData originalData = entry.getValue();
            
            if (loc.getWorld() != null) {
                loc.getBlock().setBlockData(originalData);
                restored++;
            }
        }
        
        originalBlocks.clear();
        plugin.getLogger().info("[Environment] Restaurados " + restored + " bloques");
    }
    
    // ==================== EFECTOS ATMOSFÉRICOS ====================
    
    /**
     * Genera un efecto atmosférico en el mundo
     * 
     * @param world Mundo objetivo
     * @param effect Tipo de efecto
     * @param durationSeconds Duración (0 = permanente)
     */
    public void spawnAtmosphericEffect(World world, AtmosphericEffect effect, int durationSeconds) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : world.getPlayers()) {
                    Location center = p.getLocation();
                    
                    // Generar efectos alrededor del jugador
                    for (int i = 0; i < 10; i++) {
                        Location effectLoc = center.clone().add(
                            (new Random().nextDouble() - 0.5) * ATMOSPHERIC_EFFECT_RADIUS,
                            new Random().nextDouble() * 15,
                            (new Random().nextDouble() - 0.5) * ATMOSPHERIC_EFFECT_RADIUS
                        );
                        
                        // Partículas primarias
                        world.spawnParticle(effect.getPrimaryParticle(), effectLoc, 
                            3, 0.2, 0.2, 0.2, 0.05);
                        
                        // Partículas secundarias
                        world.spawnParticle(effect.getSecondaryParticle(), effectLoc, 
                            1, 0.1, 0.1, 0.1, 0.02);
                    }
                    
                    // Sonido ambiente ocasional
                    if (new Random().nextInt(40) == 0) {
                        p.playSound(p.getLocation(), effect.getAmbientSound(), 0.3f, 
                            0.8f + new Random().nextFloat() * 0.4f);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        
        atmosphericTasks.put(world, task);
        
        // Programar limpieza
        if (durationSeconds > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    task.cancel();
                    atmosphericTasks.remove(world);
                }
            }.runTaskLater(plugin, durationSeconds * 20L);
        }
    }
    
    // ==================== ILUMINACIÓN DINÁMICA ====================
    
    /**
     * Ajusta la iluminación del mundo
     * 
     * @param world Mundo objetivo
     * @param timeOfDay Tiempo del día (0-24000)
     * @param lockTime Si se debe bloquear el tiempo
     */
    public void adjustWorldLighting(World world, long timeOfDay, boolean lockTime) {
        if (!originalWorldTimes.containsKey(world)) {
            originalWorldTimes.put(world, world.getTime());
        }
        
        world.setTime(timeOfDay);
        
        if (lockTime) {
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        }
    }
    
    /**
     * Restaura la iluminación original
     */
    public void restoreWorldLighting(World world) {
        Long originalTime = originalWorldTimes.remove(world);
        if (originalTime != null) {
            world.setTime(originalTime);
        }
        
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
    }
    
    // ==================== CLEANUP ====================
    
    /**
     * Limpia el ambiente de un mundo específico
     */
    public void cleanupWorld(World world) {
        clearWeather(world);
        clearFog(world);
        restoreWorldLighting(world);
        
        // Cancelar efectos atmosféricos
        BukkitTask task = atmosphericTasks.remove(world);
        if (task != null) {
            task.cancel();
        }
        
        // Restaurar tormenta original
        Boolean originalStorm = originalStorms.remove(world);
        if (originalStorm != null) {
            world.setStorm(originalStorm);
        }
    }
    
    /**
     * Limpia todo el sistema de ambiente
     */
    public void cleanupAll() {
        // Cancelar todas las tasks
        weatherTasks.values().forEach(BukkitTask::cancel);
        weatherTasks.clear();
        
        fogTasks.values().forEach(BukkitTask::cancel);
        fogTasks.clear();
        
        atmosphericTasks.values().forEach(BukkitTask::cancel);
        atmosphericTasks.clear();
        
        // Restaurar bloques
        restoreAllBlocks();
        
        // Restaurar mundos
        for (World world : new HashSet<>(activeWeather.keySet())) {
            cleanupWorld(world);
        }
        
        // Limpiar mapas
        activeWeather.clear();
        activeFog.clear();
        originalWorldTimes.clear();
        originalStorms.clear();
        
        plugin.getLogger().info("[Environment] Sistema de ambiente limpiado completamente");
    }
}
