package me.apocalipsis.disaster;

import java.util.Random;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.disaster.adapters.PerformanceAdapter;
import me.apocalipsis.state.TimeService;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;
import me.apocalipsis.utils.DisasterDamage;
import me.apocalipsis.utils.ParticleCompat;

import java.util.*;

public class LluviaFuegoNew extends DisasterBase implements Listener {

    private double densidad;
    private boolean apagaTodoAlFinalizar;
    private float explosionPower;
    private boolean romperBloques;
    private boolean prenderFuego;
    private int fuegoDuraSeg;
    
    // NUEVO: Meteoritos grandes con advertencia
    private boolean meteoritosEnabled;
    private int meteoritosIntervalo;
    private double meteoritosExplosionPower;
    private int meteoritosAdvertenciaTicks;
    private int nextMeteorito;
    
    // NUEVO: Zona de calor extremo
    private boolean calorExtremorEnabled;
    private int calorDamageInterval;
    
    // NUEVO: Lluvia de ceniza
    private boolean cenizaEnabled;
    private int cenizaDuracion;
    
    // NUEVO: Transformación del terreno
    private boolean transformacionEnabled;
    private final Map<Material, Material> transformaciones = new HashMap<>();
    private final List<Block> blocksTransformados = new ArrayList<>();
    
    // NUEVO: Bolas de fuego con trayectorias
    private boolean trayectoriasEnabled;
    private double trayectoriaVelocidad;
    
    // NUEVO: Zonas de fuego persistente
    private boolean fuegosPersistentesEnabled;
    private int fuegosPersistentesRadius;
    private final List<Location> fuegosPersistentesLocations = new ArrayList<>();
    
    // NUEVO: Sistema de fases
    private boolean fasesEnabled;
    private double faseMultiplicador;
    
    // NUEVO: Rotura de bloques de protección (agua)
    private boolean romperProteccionEnabled;
    private double romperProteccionProbabilidad;
    private int romperProteccionCantidad;
    
    private final Random random = new Random();
    private final java.util.List<org.bukkit.block.Block> fuegosTemporal = new java.util.ArrayList<>();

    public LluviaFuegoNew(Apocalipsis plugin, MessageBus messageBus, SoundUtil soundUtil, 
                         TimeService timeService, PerformanceAdapter performanceAdapter) {
        super(plugin, messageBus, soundUtil, timeService, performanceAdapter, "lluvia_fuego");
        loadConfig();
        
        // Registrar listener para explosiones
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void loadConfig() {
        ConfigurationSection config = plugin.getConfigManager().getDesastresConfig()
            .getConfigurationSection("desastres.lluvia_fuego");

        if (config != null) {
            densidad = config.getDouble("densidad", 0.5);  // REDUCIDO de 0.9 a 0.5 (44% menos bolas)
            apagaTodoAlFinalizar = config.getBoolean("apaga_todo_al_finalizar", true);
            explosionPower = (float) config.getDouble("explosion_power", 1.0);  // REDUCIDO de 1.2 a 1.0
            romperBloques = config.getBoolean("romper_bloques", true);
            prenderFuego = config.getBoolean("prender_fuego", true);
            fuegoDuraSeg = config.getInt("fuego_dura_seg", 8);
            
            // NUEVO: Meteoritos grandes
            ConfigurationSection meteoritosConf = config.getConfigurationSection("meteoritos");
            if (meteoritosConf != null) {
                meteoritosEnabled = meteoritosConf.getBoolean("enabled", true);
                meteoritosIntervalo = meteoritosConf.getInt("intervalo_ticks", 400);
                meteoritosExplosionPower = meteoritosConf.getDouble("explosion_power", 3.0);
                meteoritosAdvertenciaTicks = meteoritosConf.getInt("advertencia_ticks", 60);
            } else {
                meteoritosEnabled = true;
                meteoritosIntervalo = 600;  // REDUCIDO de 400 a 600 (30s en vez de 20s)
                meteoritosExplosionPower = 2.5;  // REDUCIDO de 3.0 a 2.5
                meteoritosAdvertenciaTicks = 60;
            }
            
            // NUEVO: Calor extremo
            ConfigurationSection calorConf = config.getConfigurationSection("calor_extremo");
            if (calorConf != null) {
                calorExtremorEnabled = calorConf.getBoolean("enabled", true);
                calorDamageInterval = calorConf.getInt("damage_interval_ticks", 40);
            } else {
                calorExtremorEnabled = true;
                calorDamageInterval = 40;
            }
            
            // NUEVO: Ceniza
            ConfigurationSection cenizaConf = config.getConfigurationSection("ceniza");
            if (cenizaConf != null) {
                cenizaEnabled = cenizaConf.getBoolean("enabled", true);
                cenizaDuracion = cenizaConf.getInt("duracion_ticks", 100);
            } else {
                cenizaEnabled = true;
                cenizaDuracion = 100;
            }
            
            // NUEVO: Transformación del terreno
            ConfigurationSection transConf = config.getConfigurationSection("transformacion_terreno");
            if (transConf != null) {
                transformacionEnabled = transConf.getBoolean("enabled", true);
                
                // Cargar transformaciones
                transformaciones.put(Material.GRASS_BLOCK, Material.COARSE_DIRT);
                transformaciones.put(Material.DIRT, Material.COARSE_DIRT);
                transformaciones.put(Material.OAK_LEAVES, Material.AIR);
                transformaciones.put(Material.BIRCH_LEAVES, Material.AIR);
                transformaciones.put(Material.SPRUCE_LEAVES, Material.AIR);
                transformaciones.put(Material.JUNGLE_LEAVES, Material.AIR);
                transformaciones.put(Material.ACACIA_LEAVES, Material.AIR);
                transformaciones.put(Material.DARK_OAK_LEAVES, Material.AIR);
                transformaciones.put(Material.WATER, Material.AIR);
            } else {
                transformacionEnabled = true;
                transformaciones.put(Material.GRASS_BLOCK, Material.COARSE_DIRT);
                transformaciones.put(Material.DIRT, Material.COARSE_DIRT);
            }
            
            // NUEVO: Trayectorias
            ConfigurationSection trayConf = config.getConfigurationSection("trayectorias");
            if (trayConf != null) {
                trayectoriasEnabled = trayConf.getBoolean("enabled", true);
                trayectoriaVelocidad = trayConf.getDouble("velocidad", 0.5);
            } else {
                trayectoriasEnabled = true;
                trayectoriaVelocidad = 0.5;
            }
            
            // NUEVO: Fuegos persistentes
            ConfigurationSection fuegosConf = config.getConfigurationSection("fuegos_persistentes");
            if (fuegosConf != null) {
                fuegosPersistentesEnabled = fuegosConf.getBoolean("enabled", true);
                fuegosPersistentesRadius = fuegosConf.getInt("radius", 3);
            } else {
                fuegosPersistentesEnabled = true;
                fuegosPersistentesRadius = 3;
            }
            
            // NUEVO: Fases
            fasesEnabled = config.getBoolean("fases.enabled", true);
            
            // NUEVO: Rotura de protección (agua)
            ConfigurationSection romperProtConf = config.getConfigurationSection("romper_proteccion");
            if (romperProtConf != null) {
                romperProteccionEnabled = romperProtConf.getBoolean("enabled", true);
                romperProteccionProbabilidad = romperProtConf.getDouble("probabilidad", 0.35);
                romperProteccionCantidad = romperProtConf.getInt("cantidad_bloques", 2);
            } else {
                romperProteccionEnabled = true;
                romperProteccionProbabilidad = 0.35; // 35% de probabilidad por impacto
                romperProteccionCantidad = 2; // Romper hasta 2 bloques de agua
            }
            
        } else {
            densidad = 0.5;  // REDUCIDO de 0.9 a 0.5
            apagaTodoAlFinalizar = true;
            explosionPower = 1.0f;  // REDUCIDO de 1.2 a 1.0
            romperBloques = true;
            prenderFuego = true;
            fuegoDuraSeg = 8;
            meteoritosEnabled = true;
            meteoritosIntervalo = 600;  // REDUCIDO de 400 a 600
            meteoritosExplosionPower = 2.5;  // REDUCIDO de 3.0 a 2.5
            meteoritosAdvertenciaTicks = 60;
            calorExtremorEnabled = true;
            calorDamageInterval = 40;
            cenizaEnabled = true;
            cenizaDuracion = 100;
            transformacionEnabled = true;
            trayectoriasEnabled = true;
            trayectoriaVelocidad = 0.5;
            fuegosPersistentesEnabled = true;
            fuegosPersistentesRadius = 3;
            fasesEnabled = true;
        }
    }

    @Override
    protected void onStart() {
        fuegosTemporal.clear();
        blocksTransformados.clear();
        fuegosPersistentesLocations.clear();
        nextMeteorito = tickCounter + meteoritosIntervalo;
        faseMultiplicador = 1.0;
        
        // Opcional: aplicar lluvia extra
        if (plugin.getConfigManager().isLluviaFuegoExtraLluvia()) {
            for (World world : Bukkit.getWorlds()) {
                world.setStorm(true);
                world.setWeatherDuration(999999);
            }
        }
    }

    @Override
    protected void onStop() {
        // Apagar fuegos temporales programados
        for (org.bukkit.block.Block block : fuegosTemporal) {
            if (block.getType() == Material.FIRE) {
                block.setType(Material.AIR);
            }
        }
        fuegosTemporal.clear();
        
        // Apagar fuegos globales
        if (apagaTodoAlFinalizar) {
            int extinguished = 0;
            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                                org.bukkit.block.Block block = chunk.getBlock(x, y, z);
                                if (block.getType() == Material.FIRE) {
                                    block.setType(Material.AIR);
                                    extinguished++;
                                }
                            }
                        }
                    }
                }
            }
            
            if (extinguished > 0) {
                plugin.getLogger().info("[LluviaFuego] Apagados " + extinguished + " bloques de fuego");
            }
        }
        
        // Restaurar bloques transformados
        for (Block b : blocksTransformados) {
            if (b.getType() == Material.COARSE_DIRT) {
                b.setType(Material.GRASS_BLOCK);
            }
        }
        blocksTransformados.clear();
        fuegosPersistentesLocations.clear();

        // Restaurar clima
        for (World world : Bukkit.getWorlds()) {
            world.setStorm(false);
        }
    }

    @Override
    protected void onTick() {
        // Actualizar sistema de fases
        updatePhaseMultiplier();
        
        // Actualizar meteoritos
        updateMeteoritos();
        
        // Sonidos ambientales cada 2 segundos
        if (tickCounter % 40 == 0) {
            soundUtil.playSoundAll(Sound.BLOCK_FIRE_AMBIENT, 0.6f, 1.0f);
        }
        
        // Mantener fuegos persistentes
        if (fuegosPersistentesEnabled && tickCounter % 60 == 0) {
            maintainPersistentFires();
        }
    }

    @Override
    public void applyEffects(Player player) {
        // Spawn fireballs cada 2 ticks (era 3, demasiado lento)
        if (shouldSkipTick(8)) return;
        
        if (isPlayerExempt(player)) return;

        double scale = getPerformanceScale();
        if (scale <= 0) return;

        // Densidad ajustada con multiplicador de fase
        double densidadFinal = densidad * scale * faseMultiplicador;
        if (plugin.getConfigManager().isTestMode()) {
            densidadFinal *= 0.5;
        }

        int tries = Math.max(1, (int) Math.ceil(densidadFinal));
        
        for (int i = 0; i < tries; i++) {
            // Probabilidad más alta para asegurar spawns
            if (random.nextDouble() > 0.05) continue; // Era densidadFinal / tries, muy bajo

            Location playerLoc = player.getLocation();
            World world = player.getWorld();

            // Radio y altura aleatorios
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = 8 + random.nextDouble() * 7;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            double height = 10 + random.nextDouble() * 15;

            Location spawnLoc = playerLoc.clone().add(offsetX, height, offsetZ);

            // NUEVO: Trayectorias curvas en lugar de caída vertical
            SmallFireball fireball;
            if (trayectoriasEnabled) {
                fireball = spawnFireballWithTrajectory(world, spawnLoc, playerLoc);
            } else {
                fireball = world.spawn(spawnLoc, SmallFireball.class);
                fireball.setDirection(new Vector(0, -1, 0));
            }
            
            fireball.setYield(explosionPower);
            fireball.setIsIncendiary(prenderFuego);
            
            // Partículas de fuego
            world.spawnParticle(Particle.FLAME, spawnLoc, 3, 0.1, 0.1, 0.1, 0.01);
        }
        
        // NUEVO: Calor extremo - daño constante
        if (calorExtremorEnabled && tickCounter % calorDamageInterval == 0) {
            applyHeatDamage(player);
        }
        
        // NUEVO: Ceniza - ceguera temporal
        if (cenizaEnabled && tickCounter % 100 == 0) {
            applyAshEffect(player);
        }
        
        // NUEVO: Transformación del terreno
        if (transformacionEnabled && tickCounter % 80 == 0) {
            transformTerrain(player);
        }

        // Daño aleatorio
        if (tickCounter % 60 == 0) {
            ConfigurationSection config = plugin.getConfigManager().getDesastresConfig();
            DisasterDamage.maybeDamage(player, "lluvia_fuego", config, messageBus, soundUtil);
        }
    }
    
    /**
     * Listener para crear explosión controlada al impactar SmallFireball
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onFireballHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof SmallFireball)) return;
        if (!active) return;
        
        if (plugin.getStateManager().getCurrentState() != me.apocalipsis.state.ServerState.ACTIVO) {
            return;
        }
        
        SmallFireball fireball = (SmallFireball) event.getEntity();
        Location loc = fireball.getLocation();
        World world = loc.getWorld();
        
        // NUEVO: Verificar protección por agua cercana
        boolean hasWaterProtection = hasNearbyWater(loc);
        float finalExplosionPower = explosionPower;
        boolean canSetFire = prenderFuego;
        
        if (hasWaterProtection) {
            // Reducir explosión 60% si hay agua cerca
            finalExplosionPower *= 0.4f;
            canSetFire = false; // Agua evita fuego
            
            // Efectos de vapor
            world.spawnParticle(Particle.CLOUD, loc, 25, 1.2, 1.2, 1.2, 0.1);
            world.spawnParticle(Particle.BUBBLE_POP, loc, 15, 0.8, 0.8, 0.8, 0.05);
            world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1.2f, 1.0f);
            world.playSound(loc, Sound.ENTITY_GENERIC_SPLASH, 0.8f, 1.2f);
            
            // [CAMBIO] Evaporar agua movido fuera del if - siempre intenta romper con probabilidad
        }
        
        // NUEVO: Intentar evaporar/romper bloques de agua cercanos (con probabilidad)
        // Esto ocurre SIEMPRE, no solo cuando hay protección
        // Fuerza al jugador a reponer los bloques de agua constantemente
        evaporateNearbyWater(loc, romperProteccionCantidad);
        
        // Partículas de impacto
        spawnParticleForNonExempt(world, ParticleCompat.flame(), loc, 15, 0.3, 0.3, 0.3, 0.1);
        spawnParticleForNonExempt(world, ParticleCompat.lava(), loc, 5, 0.2, 0.2, 0.2, 0);
        spawnParticleForNonExempt(world, ParticleCompat.smokeNormal(), loc, 10, 0.4, 0.4, 0.4, 0.05);
        spawnParticleForNonExempt(world, ParticleCompat.explosionNormal(), loc, 1, 0, 0, 0, 0);
        
        // Explosión controlada
        boolean breakBlocks = romperBloques && !hasWaterProtection; // Agua evita rotura
        world.createExplosion(loc, finalExplosionPower, false, breakBlocks);
        
        // Prender fuego temporal si está habilitado y no hay agua
        if (canSetFire) {
            scheduleTemporalFire(loc);
        }
    }
    
    /**
     * [#11] Encender fuego temporal en la ubicación y programar apagado
     */
    private void scheduleTemporalFire(Location loc) {
        // Buscar bloques cercanos para prender fuego
        int radius = 2;
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location testLoc = loc.clone().add(x, y, z);
                    org.bukkit.block.Block block = testLoc.getBlock();
                    
                    // Solo prender fuego en bloques de aire con bloque sólido debajo
                    if (block.getType() == Material.AIR) {
                        org.bukkit.block.Block below = block.getRelative(org.bukkit.block.BlockFace.DOWN);
                        if (below.getType().isSolid()) {
                            block.setType(Material.FIRE);
                            fuegosTemporal.add(block);
                            
                            // Registrar para zona persistente
                            if (fuegosPersistentesEnabled) {
                                fuegosPersistentesLocations.add(testLoc.clone());
                            }
                            
                            // Programar apagado
                            long ticks = fuegoDuraSeg * 20L;
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                if (block.getType() == Material.FIRE) {
                                    block.setType(Material.AIR);
                                }
                                fuegosTemporal.remove(block);
                            }, ticks);
                        }
                    }
                }
            }
        }
    }
    
    // ==================== NUEVOS MÉTODOS ====================
    
    /**
     * Sistema de fases: intensidad varía durante el desastre
     */
    private void updatePhaseMultiplier() {
        if (!fasesEnabled) {
            faseMultiplicador = 1.0;
            return;
        }
        
        int totalSeconds = timeService.getPlannedSeconds();
        int remainingSeconds = timeService.getRemainingSeconds();
        if (totalSeconds <= 0) {
            faseMultiplicador = 1.0;
            return;
        }
        
        int elapsedSeconds = totalSeconds - remainingSeconds;
        double progress = (double) elapsedSeconds / totalSeconds;
        
        if (progress < 0.25) {
            // Fase 1: inicio moderado 0.8x
            faseMultiplicador = 0.8;
        } else if (progress < 0.75) {
            // Fase 2: pico intenso 1.4x
            faseMultiplicador = 1.4;
            
            if (elapsedSeconds == totalSeconds / 2) {
                messageBus.broadcast("§c§l⚠ ¡LA LLUVIA DE FUEGO SE INTENSIFICA!", "lluvia_peak");
                soundUtil.playSoundAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.6f);
            }
        } else {
            // Fase 3: declive 0.9x
            faseMultiplicador = 0.9;
        }
    }
    
    /**
     * Meteoritos grandes: explosiones masivas con advertencia
     */
    private void updateMeteoritos() {
        if (!meteoritosEnabled || tickCounter < nextMeteorito) return;
        
        double scale = getPerformanceScale();
        if (scale <= 0) return;
        
        // Elegir jugador aleatorio
        List<Player> players = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        if (players.isEmpty()) return;
        
        Player target = players.get(random.nextInt(players.size()));
        if (isPlayerExempt(target)) {
            nextMeteorito = tickCounter + meteoritosIntervalo;
            return;
        }
        
        Location loc = target.getLocation().clone();
        
        // Advertencia visual con beacon
        spawnMeteorWarning(loc);
        
        // Programar impacto
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            spawnMeteorImpact(loc);
        }, meteoritosAdvertenciaTicks);
        
        nextMeteorito = tickCounter + meteoritosIntervalo;
    }
    
    /**
     * Advertencia visual del meteorito
     */
    private void spawnMeteorWarning(Location loc) {
        World world = loc.getWorld();
        
        // Sonido de advertencia
        soundUtil.playSoundAll(Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.5f);
        messageBus.sendActionBarAll("§c§l⚠ ¡METEORITO ENTRANTE! ⚠", "meteor_warning");
        
        // Partículas de advertencia (columna roja)
        for (int i = 0; i < meteoritosAdvertenciaTicks / 5; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (int y = 0; y < 20; y++) {
                    Location particleLoc = loc.clone().add(0, y, 0);
                    spawnParticleForNonExempt(world, Particle.FLAME, particleLoc, 5, 0.5, 0.5, 0.5, 0.01);
                    spawnParticleForNonExempt(world, Particle.LAVA, particleLoc, 2, 0.3, 0.3, 0.3, 0);
                }
            }, (long) i * 5);
        }
    }
    
    /**
     * Impacto del meteorito
     */
    private void spawnMeteorImpact(Location loc) {
        World world = loc.getWorld();
        
        // Explosión masiva
        world.createExplosion(loc, (float) meteoritosExplosionPower, false, romperBloques);
        
        // Efectos visuales
        spawnParticleForNonExempt(world, Particle.EXPLOSION_EMITTER, loc, 3, 1, 1, 1, 0);
        spawnParticleForNonExempt(world, Particle.FLAME, loc, 100, 3, 3, 3, 0.2);
        spawnParticleForNonExempt(world, Particle.LAVA, loc, 50, 2, 2, 2, 0);
        spawnParticleForNonExempt(world, Particle.CAMPFIRE_COSY_SMOKE, loc, 80, 4, 4, 4, 0.1);
        
        // Sonidos
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
        world.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2.0f, 0.7f);
        
        // Crear zona de fuego persistente
        if (fuegosPersistentesEnabled) {
            for (int x = -fuegosPersistentesRadius; x <= fuegosPersistentesRadius; x++) {
                for (int z = -fuegosPersistentesRadius; z <= fuegosPersistentesRadius; z++) {
                    Location fireLoc = loc.clone().add(x, 0, z);
                    Block block = fireLoc.getBlock();
                    Block below = block.getRelative(org.bukkit.block.BlockFace.DOWN);
                    
                    if (block.getType() == Material.AIR && below.getType().isSolid()) {
                        block.setType(Material.FIRE);
                        fuegosTemporal.add(block);
                        fuegosPersistentesLocations.add(fireLoc.clone());
                    }
                }
            }
        }
    }
    
    /**
     * Calor extremo: daño constante por estar expuesto
     */
    private void applyHeatDamage(Player player) {
        if (isPlayerExempt(player)) return;
        
        // Reducción si está bajo techo
        Block above = player.getLocation().add(0, 3, 0).getBlock();
        if (above.getType().isSolid()) {
            return; // Protegido del calor
        }
        
        // Daño ligero
        player.damage(0.5);
        player.setFireTicks(40); // 2 segundos de fuego
        
        // Partículas de calor
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.01);
    }
    
    /**
     * Ceniza: ceguera y lentitud temporal
     */
    private void applyAshEffect(Player player) {
        if (isPlayerExempt(player)) return;
        
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, cenizaDuracion, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, cenizaDuracion, 0, false, false));
        
        // Partículas de ceniza
        player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, player.getEyeLocation(), 20, 0.5, 0.5, 0.5, 0.02);
        
        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 0.8f);
    }
    
    /**
     * Transformación del terreno: convierte bloques naturales
     */
    private void transformTerrain(Player player) {
        if (isPlayerExempt(player)) return;
        
        Location loc = player.getLocation();
        World world = loc.getWorld();
        
        // Transformar bloques en radio de 3
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = -1; y <= 1; y++) {
                    Location blockLoc = loc.clone().add(x, y, z);
                    Block block = blockLoc.getBlock();
                    Material type = block.getType();
                    
                    if (transformaciones.containsKey(type)) {
                        Material newType = transformaciones.get(type);
                        block.setType(newType);
                        blocksTransformados.add(block);
                        
                        // Partículas de transformación
                        spawnParticleForNonExempt(world, Particle.FLAME, blockLoc.add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0);
                    }
                }
            }
        }
    }
    
    /**
     * Trayectorias curvas: bolas de fuego con movimiento parabólico
     */
    private SmallFireball spawnFireballWithTrajectory(World world, Location spawn, Location target) {
        SmallFireball fireball = world.spawn(spawn, SmallFireball.class);
        
        // Calcular vector hacia el jugador con componente hacia abajo
        Vector direction = target.toVector().subtract(spawn.toVector()).normalize();
        direction.setY(direction.getY() - trayectoriaVelocidad);
        direction = direction.normalize().multiply(trayectoriaVelocidad);
        
        fireball.setDirection(direction);
        return fireball;
    }
    
    /**
     * Mantener fuegos persistentes: re-encender fuegos en zonas clave
     */
    private void maintainPersistentFires() {
        double scale = getPerformanceScale();
        if (scale <= 0) return;
        
        for (Location loc : new ArrayList<>(fuegosPersistentesLocations)) {
            Block block = loc.getBlock();
            Block below = block.getRelative(org.bukkit.block.BlockFace.DOWN);
            
            if (block.getType() == Material.AIR && below.getType().isSolid()) {
                block.setType(Material.FIRE);
                fuegosTemporal.add(block);
            }
        }
    }
    
    /**
     * NUEVO: Detecta si hay agua cerca del punto de impacto
     */
    private boolean hasNearbyWater(Location loc) {
        // Verificar agua en 3x3x3 alrededor del impacto
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block check = loc.clone().add(x, y, z).getBlock();
                    if (check.getType() == Material.WATER) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * NUEVO: Evapora/rompe algunos bloques de agua cercanos con probabilidad
     * @param loc Ubicación del impacto
     * @param maxToEvaporate Cantidad máxima de bloques a evaporar
     */
    private void evaporateNearbyWater(Location loc, int maxToEvaporate) {
        // Verificar si está habilitado y chequear probabilidad
        if (!romperProteccionEnabled) return;
        if (random.nextDouble() > romperProteccionProbabilidad) return; // 35% de probabilidad por defecto
        
        List<Block> waterBlocks = new ArrayList<>();
        
        // Recolectar bloques de agua en 2x2x2
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block check = loc.clone().add(x, y, z).getBlock();
                    if (check.getType() == Material.WATER) {
                        waterBlocks.add(check);
                    }
                }
            }
        }
        
        // Si no hay agua, salir
        if (waterBlocks.isEmpty()) return;
        
        // Obtener World una sola vez (optimización)
        World world = loc.getWorld();
        if (world == null) return;
        
        // Evaporar aleatoriamente hasta maxToEvaporate bloques (configurado)
        Collections.shuffle(waterBlocks);
        int evaporated = 0;
        int maxAllowed = Math.min(maxToEvaporate, romperProteccionCantidad);
        
        for (Block water : waterBlocks) {
            if (evaporated >= maxAllowed) break;
            
            // Guardar ubicación antes de destruir el bloque
            Location vaporLoc = water.getLocation().add(0.5, 0.5, 0.5);
            
            water.setType(Material.AIR);
            
            // Partículas de vapor en el bloque evaporado
            spawnParticleForNonExempt(world, Particle.CLOUD, vaporLoc, 8, 0.3, 0.3, 0.3, 0.05);
            spawnParticleForNonExempt(world, Particle.BUBBLE_POP, vaporLoc, 5, 0.2, 0.2, 0.2, 0.02);
            world.playSound(vaporLoc, Sound.BLOCK_FIRE_EXTINGUISH, 0.4f, 1.5f);
            
            evaporated++;
        }
        
        // Mensaje de feedback si se evaporaron bloques
        if (evaporated > 0 && plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[LluviaFuego] Evaporados " + evaporated + " bloques de agua por impacto");
        }
    }
}
