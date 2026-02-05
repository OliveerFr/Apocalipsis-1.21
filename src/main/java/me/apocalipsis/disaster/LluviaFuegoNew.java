package me.apocalipsis.disaster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.disaster.adapters.PerformanceAdapter;
import me.apocalipsis.state.ServerState;
import me.apocalipsis.state.TimeService;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;
import me.apocalipsis.utils.BlockOwnershipTracker;
import me.apocalipsis.utils.DisasterDamage;

public class LluviaFuegoNew extends DisasterBase implements Listener {

    private final BlockOwnershipTracker blockTracker;

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
    private int lastPhaseAnnounced = 0;  // Control de mensajes por fase
    
    // NUEVO: Rotura de bloques de protección (agua) - MEJORADO
    private boolean romperProteccionEnabled;
    private double romperProteccionProbabilidad;
    private double romperProteccionProbabilidadMeteorito;
    private int romperProteccionCantidad;
    private int romperProteccionRadio;
    private int romperProteccionCooldown;
    private boolean romperProteccionProtegerProfunda;
    private long lastWaterBreakTime = 0;  // Cooldown tracking
    
    // [v1.18.0] Mecánicas avanzadas
    private EnderDragon fireDragon = null;
    private boolean dragonSpawned = false;
    
    private final Random random = new Random();
    private final java.util.List<org.bukkit.block.Block> fuegosTemporal = new java.util.ArrayList<>();

    public LluviaFuegoNew(Apocalipsis plugin, MessageBus messageBus, SoundUtil soundUtil, 
                         TimeService timeService, PerformanceAdapter performanceAdapter) {
        super(plugin, messageBus, soundUtil, timeService, performanceAdapter, "lluvia_fuego");
        this.blockTracker = plugin.getBlockTracker();
        loadConfig();
        
        // Registrar listener para explosiones
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void loadConfig() {
        ConfigurationSection config = plugin.getConfigManager().getDesastresConfig()
            .getConfigurationSection("desastres.lluvia_fuego");

        if (config != null) {
            // Cargar duración del desastre
            int duracionSegundos = config.getInt("duracion_segundos", 70);
            setMaxTicks(duracionSegundos);
            
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
            
            // Rotura de bloques de protección (agua) - MEJORADO
            ConfigurationSection romperProtConf = config.getConfigurationSection("romper_proteccion");
            if (romperProtConf != null) {
                romperProteccionEnabled = romperProtConf.getBoolean("enabled", true);
                romperProteccionProbabilidad = romperProtConf.getDouble("probabilidad", 0.004);
                romperProteccionProbabilidadMeteorito = romperProtConf.getDouble("probabilidad_meteorito", 0.015);
                romperProteccionCantidad = romperProtConf.getInt("cantidad_bloques", 1);
                romperProteccionRadio = romperProtConf.getInt("radio_busqueda", 2);
                romperProteccionCooldown = romperProtConf.getInt("cooldown_ticks", 180);
                romperProteccionProtegerProfunda = romperProtConf.getBoolean("proteger_agua_profunda", true);
            } else {
                romperProteccionEnabled = true;
                romperProteccionProbabilidad = 0.004; // 0.4% de probabilidad por impacto
                romperProteccionProbabilidadMeteorito = 0.015; // 1.5% para meteoritos
                romperProteccionCantidad = 1;
                romperProteccionRadio = 2;
                romperProteccionCooldown = 180; // 9 segundos
                romperProteccionProtegerProfunda = true;
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
        
        // [v1.17.0] Crear BossBar del desastre
        createDisasterBossBar("LLUVIA DE FUEGO");
        
        // [v1.17.0] Mostrar título de inicio
        showPhaseTitle(1, "LLUVIA DE FUEGO");
        
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
                setBlockTracked(block, Material.AIR);
            }
        }
        fuegosTemporal.clear();
        
        // [OPTIMIZACIÓN v1.22.68] Apagar fuegos globales en async con batch limiting
        if (apagaTodoAlFinalizar) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                java.util.concurrent.atomic.AtomicInteger extinguished = new java.util.concurrent.atomic.AtomicInteger(0);
                java.util.List<org.bukkit.Location> firesToExtinguish = new java.util.ArrayList<>();
                
                // Fase 1: Recolectar bloques de fuego (async scan)
                for (World world : Bukkit.getWorlds()) {
                    Chunk[] chunks = world.getLoadedChunks();
                    
                    // Limitar a 100 chunks por mundo para prevenir lag
                    int maxChunks = Math.min(chunks.length, 100);
                    
                    for (int i = 0; i < maxChunks && firesToExtinguish.size() < 1000; i++) {
                        Chunk chunk = chunks[i];
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                                    org.bukkit.block.Block block = chunk.getBlock(x, y, z);
                                    if (block.getType() == Material.FIRE) {
                                        firesToExtinguish.add(block.getLocation());
                                    }
                                }
                            }
                        }
                    }
                }
                
                plugin.getLogger().info("[LluviaFuego] Encontrados " + firesToExtinguish.size() + " bloques de fuego a apagar");
                
                // Fase 2: Apagar en el hilo principal en lotes pequeños
                final int BATCH_SIZE = 50;
                for (int i = 0; i < firesToExtinguish.size(); i += BATCH_SIZE) {
                    final int startIdx = i;
                    final int endIdx = Math.min(i + BATCH_SIZE, firesToExtinguish.size());
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (int j = startIdx; j < endIdx; j++) {
                            org.bukkit.Location loc = firesToExtinguish.get(j);
                            org.bukkit.block.Block block = loc.getBlock();
                            if (block.getType() == Material.FIRE) {
                                setBlockTracked(block, Material.AIR);
                                extinguished.incrementAndGet();
                            }
                        }
                        
                        // Log final cuando termine el último lote
                        if (endIdx >= firesToExtinguish.size() && extinguished.get() > 0) {
                            plugin.getLogger().info("[LluviaFuego] Apagados " + extinguished.get() + " bloques de fuego");
                        }
                    });
                    
                    // Pequeña pausa entre lotes
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
        
        // [v1.18.0] Remover dragón de fuego
        if (fireDragon != null && fireDragon.isValid()) {
            fireDragon.remove();
        }
        fireDragon = null;
        dragonSpawned = false;
        
        // [v1.17.0] Remover BossBar
        removeDisasterBossBar();
        
        // Restaurar bloques transformados
        for (Block b : blocksTransformados) {
            if (b.getType() == Material.COARSE_DIRT) {
                setBlockTracked(b, Material.GRASS_BLOCK);
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
        
        // [v1.18.0] Columnas de fuego en fases 4-5
        int currentPhase = getCurrentPhaseFromTick();
        
        // [v1.19.0] Rastrear supervivencia de jugadores en esta fase
        for (Player p : Bukkit.getOnlinePlayers()) {
            trackPlayerSurvival(p, currentPhase);
        }
        
        if (currentPhase >= 4 && tickCounter % 40 == 0) {
            createFireColumns();
        }
        
        // [v1.18.0] Spawn dragón de fuego en fase 5
        if (currentPhase == 5 && !dragonSpawned) {
            spawnFireDragon();
            dragonSpawned = true;
        }
        
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
        
        // **NUEVO: Feedback de protección por agua cada 5 segundos**
        if (tickCounter % 100 == 0) {
            sendPlayerWaterProtectionStatus(player);
        }

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
        
        if (plugin.getStateManager().getCurrentState() != ServerState.ACTIVO) {
            return;
        }
        
        SmallFireball fireball = (SmallFireball) event.getEntity();
        Location loc = fireball.getLocation();
        World world = loc.getWorld();
        
        // NUEVO: Verificar protección por agua cercana
        WaterProtection waterInfo = checkWaterProtection(loc);
        float finalExplosionPower = explosionPower;
        boolean canSetFire = prenderFuego;
        
        if (waterInfo.hasWater) {
            // Reducir explosión 60% si hay agua cerca
            finalExplosionPower *= 0.4f;
            canSetFire = false; // Agua evita fuego
            
            // Efectos de vapor mejorados
            world.spawnParticle(Particle.CLOUD, loc, 15, 1.2, 1.2, 1.2, 0.1);
            world.spawnParticle(Particle.BUBBLE_POP, loc, 10, 0.8, 0.8, 0.8, 0.05);
            world.spawnParticle(Particle.DRIPPING_WATER, loc, 7, 0.5, 0.5, 0.5, 0);
            world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1.2f, 1.0f);
            world.playSound(loc, Sound.ENTITY_GENERIC_SPLASH, 0.8f, 1.2f);
            
            // Mensaje de feedback si hay jugador cerca
            sendWaterProtectionFeedback(loc, waterInfo);
        }
        
        // NUEVO: Intentar evaporar/romper bloques de agua cercanos (con probabilidad)
        evaporateNearbyWater(loc, romperProteccionCantidad);
        
        // Partículas de impacto
        spawnParticleForNonExempt(world, Particle.FLAME, loc, 10, 0.3, 0.3, 0.3, 0.1);
        spawnParticleForNonExempt(world, Particle.LAVA, loc, 4, 0.2, 0.2, 0.2, 0);
        spawnParticleForNonExempt(world, Particle.SMOKE, loc, 7, 0.4, 0.4, 0.4, 0.05);
        spawnParticleForNonExempt(world, Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
        
        // Explosión controlada
        boolean breakBlocks = romperBloques && !waterInfo.hasWater; // Agua evita rotura
        world.createExplosion(loc, finalExplosionPower, false, breakBlocks);
        
        // Prender fuego temporal si está habilitado y no hay agua
        if (canSetFire) {
            scheduleTemporalFire(loc);
        }
    }
    
    /**
     * **NUEVO** Clase para almacenar información de protección por agua
     */
    private static class WaterProtection {
        final boolean hasWater;
        final int waterBlocks;
        final boolean isDeep;
        
        WaterProtection(boolean hasWater, int waterBlocks, boolean isDeep) {
            this.hasWater = hasWater;
            this.waterBlocks = waterBlocks;
            this.isDeep = isDeep;
        }
    }
    
    /**
     * **NUEVO** Verifica protección de agua con información detallada
     */
    private WaterProtection checkWaterProtection(Location loc) {
        int waterCount = 0;
        boolean hasDeepWater = false;
        
        // Verificar agua en 3x3x3 alrededor del impacto
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block check = loc.clone().add(x, y, z).getBlock();
                    if (check.getType() == Material.WATER) {
                        waterCount++;
                        if (isDeepWater(check)) {
                            hasDeepWater = true;
                        }
                    }
                }
            }
        }
        
        return new WaterProtection(waterCount > 0, waterCount, hasDeepWater);
    }
    
    /**
     * **NUEVO** Envía feedback de protección por agua a jugadores cercanos
     */
    private void sendWaterProtectionFeedback(Location loc, WaterProtection waterInfo) {
        // Buscar jugadores en 10 bloques
        for (Player player : loc.getWorld().getPlayers()) {
            if (player.getLocation().distance(loc) <= 10 && !isPlayerExempt(player)) {
                int reduccion = 60; // Reducción fija del 60%
                
                if (waterInfo.isDeep) {
                    plugin.getMessageBus().sendActionBar(player,
                        "§b§l💧 AGUA PROFUNDA §8| §7Explosión §a-" + reduccion + "% §8| §7Fuego §aAPAGADO");
                } else {
                    plugin.getMessageBus().sendActionBar(player,
                        "§b§l💧 AGUA PROTECTORA §8| §7Explosión §a-" + reduccion + "% §8| §e" + waterInfo.waterBlocks + " §7bloques");
                }
                
                // Sonido positivo
                if (tickCounter % 40 == 0) {
                    soundUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.5f);
                }
            }
        }
    }
    
    /**
     * **NUEVO** Verifica y muestra el estado de protección de agua del jugador
     */
    private void sendPlayerWaterProtectionStatus(Player player) {
        if (isPlayerExempt(player)) return;
        
        Location loc = player.getLocation();
        WaterProtection waterInfo = checkWaterProtection(loc);
        
        if (waterInfo.hasWater) {
            if (waterInfo.isDeep) {
                // Agua profunda - protección máxima
                plugin.getMessageBus().sendActionBar(player,
                    "§b§l✓ AGUA PROFUNDA §8| §7Reducción §a60% §8| §7Anti-evaporación §aACTIVA");
                
                // Partículas de agua cada 10 segundos
                if (tickCounter % 200 == 0) {
                    player.getWorld().spawnParticle(Particle.DRIPPING_WATER, 
                        loc.clone().add(0, 2, 0), 10, 1, 0.5, 1, 0);
                    soundUtil.playSound(player, Sound.WEATHER_RAIN, 0.4f, 1.2f);
                }
            } else {
                // Agua normal - sugerir mejora
                int bloquesFaltantes = Math.max(0, 5 - waterInfo.waterBlocks);
                plugin.getMessageBus().sendActionBar(player,
                    "§b§l💧 AGUA PROTECTORA §8| §e" + waterInfo.waterBlocks + "§7/5 §8- §7-§a60% §8| §7Profundiza §e+1");
                
                // Consejo cada 15 segundos
                if (tickCounter % 300 == 0 && waterInfo.waterBlocks < 5) {
                    player.sendMessage("§b💧 §7Protección activa (§e" + waterInfo.waterBlocks + " bloques§7). Añade §e+1 bloque de profundidad§7 para anti-evaporación.");
                    player.sendMessage("§7  §8→ §7El agua profunda (§e2+ bloques§7) no se evapora con magma");
                }
            }
        } else {
            // Sin agua - DIAGNÓSTICO COMPLETO
            String diagnostico = diagnosticarProteccionAgua(player);
            
            plugin.getMessageBus().sendActionBar(player,
                "§c§l⚠ SIN PROTECCIÓN §8| §7" + diagnostico);
            
            // Alertas periódicas con instrucciones claras
            if (tickCounter % 400 == 0) {
                player.sendMessage("§c🔥 §7LLUVIA DE FUEGO: Sin protección de agua");
                player.sendMessage("§7  §8→ §7Coloca §bagua§7 en techos y alrededores para §a-60% §7explosiones");
                player.sendMessage("§7  §8→ §7Usa §bagua profunda §7(2+ bloques) para evitar evaporación");
                soundUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
        }
    }
    
    /**
     * Diagnostica por QUÉ el jugador no tiene protección de agua
     */
    private String diagnosticarProteccionAgua(Player player) {
        Location loc = player.getLocation();
        int aguaCerca = 0;
        int aguaProfundaCerca = 0;
        double distanciaMinima = 999;
        int profundidadMaxima = 0;
        
        // Escanear radio amplio (12 bloques) para diagnóstico
        for (int x = -12; x <= 12; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -12; z <= 12; z++) {
                    Block b = loc.getWorld().getBlockAt(
                        loc.getBlockX() + x,
                        loc.getBlockY() + y,
                        loc.getBlockZ() + z
                    );
                    
                    if (b.getType() == Material.WATER) {
                        aguaCerca++;
                        double distancia = Math.sqrt(x*x + y*y + z*z);
                        
                        if (distancia < distanciaMinima) {
                            distanciaMinima = distancia;
                            
                            // Verificar profundidad en este punto
                            int profundidad = 1;
                            Block below = b.getRelative(0, -1, 0);
                            if (below.getType() == Material.WATER) {
                                profundidad = 2;
                                if (below.getRelative(0, -1, 0).getType() == Material.WATER) {
                                    profundidad = 3;
                                }
                            }
                            profundidadMaxima = Math.max(profundidadMaxima, profundidad);
                            
                            if (profundidad >= 2) {
                                aguaProfundaCerca++;
                            }
                        }
                    }
                }
            }
        }
        
        if (aguaCerca == 0) {
            // No hay agua cerca
            return "Coloca §bagua§7 cerca para §a-60% §7daño";
        } else if (distanciaMinima > 8) {
            // Hay agua pero MUY LEJOS
            return "Tienes §e" + aguaCerca + " bloques de agua§7 a §c" + String.format("%.1f", distanciaMinima) + " bloques §7(máx §e8§7)";
        } else if (profundidadMaxima < 2 && aguaCerca > 0) {
            // Hay agua cerca pero es MUY SUPERFICIAL
            return "Tu agua es §esuperficial §7(§e" + profundidadMaxima + " bloque§7) - hazla §e2+ bloques profunda";
        } else {
            // Error: hay agua en radio pero no se detectó
            return "Tienes §e" + aguaCerca + " agua§7 cerca - verifica distancia (máx §e8 bloques§7)";
        }
    }
    
    /**
     * [#11] Encender fuego temporal en la ubicación y programar apagado
     */
    private void scheduleTemporalFire(Location loc) {
        // Buscar bloques cercanos para prender fuego
        int radius = 2;
        World world = loc.getWorld();
        if (world == null) return;
        
        // [FIX] Encontrar jugador más cercano para verificar ownership
        Player nearestPlayer = null;
        double nearestDist = Double.MAX_VALUE;
        for (Player p : world.getPlayers()) {
            if (isPlayerExempt(p)) continue;
            double dist = p.getLocation().distanceSquared(loc);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearestPlayer = p;
            }
        }
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location testLoc = loc.clone().add(x, y, z);
                    org.bukkit.block.Block block = testLoc.getBlock();
                    
                    // Solo prender fuego en bloques de aire con bloque sólido debajo
                    if (block.getType() == Material.AIR) {
                        org.bukkit.block.Block below = block.getRelative(org.bukkit.block.BlockFace.DOWN);
                        if (below.getType().isSolid()) {
                            // [FIX] No poner fuego encima de bloques de otros jugadores
                            if (nearestPlayer != null && !blockTracker.canDisasterDestroyBlock(below, nearestPlayer)) {
                                continue;
                            }
                            
                            setBlockTracked(block, Material.FIRE);
                            fuegosTemporal.add(block);
                            
                            // Registrar para zona persistente
                            if (fuegosPersistentesEnabled) {
                                fuegosPersistentesLocations.add(testLoc.clone());
                            }
                            
                            // Programar apagado
                            long ticks = fuegoDuraSeg * 20L;
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                if (block.getType() == Material.FIRE) {
                                    setBlockTracked(block, Material.AIR);
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
            
            // Mensaje educativo al inicio (una vez)
            if (lastPhaseAnnounced == 0 && elapsedSeconds >= 5) {
                lastPhaseAnnounced = 1;
                messageBus.broadcast("§e§l💡 TIP: §7Sumérgete en §bagua profunda§7 (2+ bloques) para protección completa", "lluvia_tip_1");
                messageBus.broadcast("§7  §8→ El agua superficial se evapora - necesitas profundidad", "lluvia_tip_1b");
            }
            
        } else if (progress < 0.75) {
            // Fase 2: pico intenso 1.4x
            faseMultiplicador = 1.4;
            
            if (elapsedSeconds == totalSeconds / 2) {
                messageBus.broadcast("§c§l⚠ ¡LA LLUVIA DE FUEGO SE INTENSIFICA!", "lluvia_peak");
                soundUtil.playSoundAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.6f);
            }
            
            // Mensaje educativo en fase intensa (una vez)
            if (lastPhaseAnnounced < 2 && progress >= 0.40) {
                lastPhaseAnnounced = 2;
                messageBus.broadcast("§c§l⚠ FASE INTENSA: §7¡Protégete bajo agua o estructuras sólidas!", "lluvia_tip_2");
                messageBus.broadcast("§7  §8→ Evita estar al descubierto - los §cmeteoritos§7 son devastadores", "lluvia_tip_2b");
            }
            
        } else {
            // Fase 3: declive 0.9x
            faseMultiplicador = 0.9;
            
            // Mensaje educativo en fase final (una vez)
            if (lastPhaseAnnounced < 3 && progress >= 0.80) {
                lastPhaseAnnounced = 3;
                messageBus.broadcast("§a§l✓ La lluvia de fuego disminuye... §7¡Resiste un poco más!", "lluvia_tip_3");
                messageBus.broadcast("§7  §8→ Apágate si estás ardiendo y busca agua cercana", "lluvia_tip_3b");
            }
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
     * [CINEMÁTICO] Advertencia visual del meteorito con countdown dramático
     */
    private void spawnMeteorWarning(Location loc) {
        World world = loc.getWorld();
        
        // CINEMÁTICO: Sonidos épicos de advertencia
        soundUtil.playSoundAll(Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.5f);
        soundUtil.playSoundAll(Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 1.8f);
        
        // CINEMÁTICO: Mensaje dramático global
        messageBus.broadcast("§c§l⚠§l §6§lMETEORITO ENTRANTE §c§l⚠", "meteor_warning");
        
        // CINEMÁTICO: Columna de advertencia con múltiples efectos
        int warningDuration = meteoritosAdvertenciaTicks / 5;
        for (int i = 0; i < warningDuration; i++) {
            final int tick = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Columna de partículas con gradiente de color
                for (int y = 0; y < 30; y++) {
                    Location particleLoc = loc.clone().add(0, y, 0);
                    
                    // Partículas rojas/naranjas intensas
                    spawnParticleForNonExempt(world, Particle.FLAME, particleLoc, 8, 0.6, 0.3, 0.6, 0.02);
                    spawnParticleForNonExempt(world, Particle.LAVA, particleLoc, 3, 0.4, 0.2, 0.4, 0);
                    world.spawnParticle(Particle.DUST, particleLoc, 5, 0.5, 0.3, 0.5, 
                        new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 100, 0), 2.0f));
                    
                    // Partículas de humo en el centro
                    if (y % 3 == 0) {
                        spawnParticleForNonExempt(world, Particle.CAMPFIRE_SIGNAL_SMOKE, particleLoc, 2, 0.1, 0.3, 0.1, 0.01);
                    }
                }
                
                // CINEMÁTICO: Anillo expansivo en el suelo
                for (int angle = 0; angle < 360; angle += 20) {
                    double rad = Math.toRadians(angle);
                    double radius = 3 + (tick * 0.5);
                    double x = loc.getX() + radius * Math.cos(rad);
                    double z = loc.getZ() + radius * Math.sin(rad);
                    Location ringLoc = new Location(world, x, loc.getY(), z);
                    spawnParticleForNonExempt(world, Particle.FLAME, ringLoc, 1, 0, 0, 0, 0);
                }
                
                // CINEMÁTICO: Sonido pulsante cada segundo
                if (tick % 4 == 0) {
                    world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                }
            }, (long) i * 5);
        }
        
        // CINEMÁTICO: Countdown en títulos para jugadores cercanos
        for (Player player : world.getPlayers()) {
            if (isPlayerExempt(player)) continue;
            if (player.getLocation().distance(loc) < 30) {
                player.sendTitle("§c§l⚠ METEORITO", "§6§l3 segundos...", 10, 40, 10);
                
                // Countdown 3, 2, 1
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendTitle("§c§l2", "", 0, 15, 5);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.2f);
                }, 20L);
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendTitle("§c§l1", "", 0, 15, 5);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.5f);
                }, 40L);
            }
        }
    }
    
    /**
     * [CINEMÁTICO] Impacto del meteorito con efectos dramáticos épicos
     */
    private void spawnMeteorImpact(Location loc) {
        World world = loc.getWorld();
        
        // CINEMÁTICO: Explosión masiva con efectos múltiples
        world.createExplosion(loc, (float) meteoritosExplosionPower, false, romperBloques);
        
        // CINEMÁTICO: Efectos visuales épicos masivos
        spawnParticleForNonExempt(world, Particle.EXPLOSION_EMITTER, loc, 5, 2, 2, 2, 0);
        spawnParticleForNonExempt(world, Particle.FLAME, loc, 100, 4, 4, 4, 0.3);
        spawnParticleForNonExempt(world, Particle.LAVA, loc, 60, 3, 3, 3, 0.1);
        spawnParticleForNonExempt(world, Particle.CAMPFIRE_SIGNAL_SMOKE, loc, 80, 5, 5, 5, 0.15);
        spawnParticleForNonExempt(world, Particle.FLASH, loc, 3, 1, 1, 1, 0);
        
        // CINEMÁTICO: Ondas de choque expansivas
        for (int radius = 1; radius <= 12; radius++) {
            final int r = radius;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (int angle = 0; angle < 360; angle += 15) {
                    double rad = Math.toRadians(angle);
                    double x = loc.getX() + r * Math.cos(rad);
                    double z = loc.getZ() + r * Math.sin(rad);
                    Location shockwaveLoc = new Location(world, x, loc.getY(), z);
                    
                    spawnParticleForNonExempt(world, Particle.FLAME, shockwaveLoc, 5, 0.3, 0.5, 0.3, 0.05);
                    spawnParticleForNonExempt(world, Particle.BLOCK, shockwaveLoc, 8, 0.4, 0.3, 0.4, 0, Material.MAGMA_BLOCK.createBlockData());
                    spawnParticleForNonExempt(world, Particle.LAVA, shockwaveLoc, 3, 0.2, 0.2, 0.2, 0);
                }
            }, (long) r * 2);
        }
        
        // CINEMÁTICO: Sonidos épicos múltiples
        soundUtil.playSoundAll(Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
        soundUtil.playSoundAll(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);
        soundUtil.playSoundAll(Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 0.7f);
        
        // CINEMÁTICO: Efectos para jugadores cercanos
        for (Player player : world.getPlayers()) {
            if (isPlayerExempt(player)) continue;
            double distance = player.getLocation().distance(loc);
            
            if (distance < 40) {
                player.sendTitle("§4§l☆ IMPACTO ☆", "", 5, 30, 10);
                
                // Shake de cámara proporcional a la distancia
                if (distance < 15) {
                    player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.9f);
                }
            }
        }
        
        // CINEMÁTICO: Intentar romper protección de agua cercana
        evaporateNearbyWater(loc, romperProteccionCantidad * 3); // Más destrucción en meteoritos
        
        // Crear zona de fuego persistente
        if (fuegosPersistentesEnabled) {
            for (int x = -fuegosPersistentesRadius; x <= fuegosPersistentesRadius; x++) {
                for (int z = -fuegosPersistentesRadius; z <= fuegosPersistentesRadius; z++) {
                    Location fireLoc = loc.clone().add(x, 0, z);
                    Block block = fireLoc.getBlock();
                    Block below = block.getRelative(org.bukkit.block.BlockFace.DOWN);
                    
                    if (block.getType() == Material.AIR && below.getType().isSolid()) {
                        setBlockTracked(block, Material.FIRE);
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
                    
                    // [FIX] Verificar que no sea bloque de otro jugador
                    if (!blockTracker.canDisasterDestroyBlock(block, player)) {
                        continue;
                    }
                    
                    if (transformaciones.containsKey(type)) {
                        Material newType = transformaciones.get(type);
                        setBlockTracked(block, newType);
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
                // [FIX] Encontrar jugador más cercano para verificar ownership
                World world = loc.getWorld();
                if (world == null) continue;
                
                Player nearestPlayer = null;
                double nearestDist = Double.MAX_VALUE;
                for (Player p : world.getPlayers()) {
                    if (isPlayerExempt(p)) continue;
                    double dist = p.getLocation().distanceSquared(loc);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearestPlayer = p;
                    }
                }
                
                // [FIX] No poner fuego encima de bloques de otros jugadores
                if (nearestPlayer != null && !blockTracker.canDisasterDestroyBlock(below, nearestPlayer)) {
                    continue;
                }
                
                setBlockTracked(block, Material.FIRE);
                fuegosTemporal.add(block);
            }
        }
    }
    
    /**
     * NUEVO: Evapora/rompe algunos bloques de agua cercanos con probabilidad
     * @param loc Ubicación del impacto
     * @param maxToEvaporate Cantidad máxima de bloques a evaporar
     */
    private void evaporateNearbyWater(Location loc, int maxToEvaporate) {
        // Verificar si está habilitado
        if (!romperProteccionEnabled) return;
        
        // **SISTEMA DE COOLDOWN MEJORADO**
        long currentTime = System.currentTimeMillis();
        long timeSinceLastBreak = currentTime - lastWaterBreakTime;
        int cooldownMs = romperProteccionCooldown * 50; // Convertir ticks a ms (1 tick = 50ms)
        
        if (timeSinceLastBreak < cooldownMs) {
            // Cooldown activo, no romper protección
            if (plugin.getConfigManager().isDebugCiclo() && random.nextDouble() < 0.05) { // Log 5% para no spam
                long remainingSec = (cooldownMs - timeSinceLastBreak) / 1000;
                plugin.getLogger().info("[LluviaFuego] Protección de agua en cooldown (restan " + remainingSec + "s)");
            }
            return;
        }
        
        // Determinar si es meteorito (impacto más destructivo)
        // Asumimos que meteoritos tienen potencia de explosión mayor
        boolean isMeteorito = (explosionPower > 3.0f); // Si la explosión es fuerte, es meteorito
        double effectiveProbability = isMeteorito ? romperProteccionProbabilidadMeteorito : romperProteccionProbabilidad;
        
        // Chequear probabilidad (0.4% normal, 1.5% meteoritos)
        if (random.nextDouble() > effectiveProbability) {
            return; // No pasa el check de probabilidad
        }
        
        // **BÚSQUEDA INTELIGENTE DE AGUA** con radio configurable
        List<Block> waterBlocks = new ArrayList<>();
        int searchRadius = romperProteccionRadio;
        
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    Block check = loc.clone().add(x, y, z).getBlock();
                    if (check.getType() == Material.WATER) {
                        // **PROTECCIÓN DE AGUA PROFUNDA**
                        if (romperProteccionProtegerProfunda && isDeepWater(check)) {
                            // Agua profunda (2+ bloques apilados) está protegida
                            if (plugin.getConfigManager().isDebugCiclo() && random.nextDouble() < 0.1) { // Log 10%
                                plugin.getLogger().info("[LluviaFuego] Agua profunda protegida en " + 
                                    check.getX() + "," + check.getY() + "," + check.getZ());
                            }
                            continue; // No agregar a la lista de evaporables
                        }
                        
                        waterBlocks.add(check);
                    }
                }
            }
        }
        
        // Si no hay agua evaporable, salir
        if (waterBlocks.isEmpty()) return;
        
        // Obtener World una sola vez (optimización)
        World world = loc.getWorld();
        if (world == null) return;
        
        // [FIX] Encontrar jugador más cercano para verificar ownership
        Player nearestPlayer = null;
        double nearestDist = Double.MAX_VALUE;
        for (Player p : world.getPlayers()) {
            if (isPlayerExempt(p)) continue;
            double dist = p.getLocation().distanceSquared(loc);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearestPlayer = p;
            }
        }
        
        // Evaporar aleatoriamente hasta maxToEvaporate bloques (configurado)
        Collections.shuffle(waterBlocks);
        int evaporated = 0;
        int maxAllowed = Math.min(maxToEvaporate, romperProteccionCantidad);
        
        for (Block water : waterBlocks) {
            if (evaporated >= maxAllowed) break;
            
            // [FIX] Verificar que no sea bloque de otro jugador
            if (nearestPlayer != null && !blockTracker.canDisasterDestroyBlock(water, nearestPlayer)) {
                continue;
            }
            
            // Guardar ubicación antes de destruir el bloque
            Location vaporLoc = water.getLocation().add(0.5, 0.5, 0.5);
            
            setBlockTracked(water, Material.AIR);
            
            // Partículas de vapor en el bloque evaporado
            spawnParticleForNonExempt(world, Particle.CLOUD, vaporLoc, 8, 0.3, 0.3, 0.3, 0.05);
            spawnParticleForNonExempt(world, Particle.BUBBLE_POP, vaporLoc, 5, 0.2, 0.2, 0.2, 0.02);
            world.playSound(vaporLoc, Sound.BLOCK_FIRE_EXTINGUISH, 0.4f, 1.5f);
            
            evaporated++;
        }
        
        // **ACTUALIZAR COOLDOWN** si se evaporaron bloques
        if (evaporated > 0) {
            lastWaterBreakTime = currentTime;
            
            if (plugin.getConfigManager().isDebugCiclo()) {
                String impactType = isMeteorito ? "METEORITO" : "fuego";
                plugin.getLogger().info("[LluviaFuego] Evaporados " + evaporated + 
                    " bloques de agua por " + impactType + " (prob=" + 
                    String.format("%.1f%%", effectiveProbability * 100) + 
                    ", cooldown=" + (cooldownMs/1000) + "s)");
            }
        }
    }
    
    /**
     * Verifica si un bloque de agua tiene 2 o más bloques de agua apilados debajo
     * (agua profunda que debe ser más difícil de evaporar)
     */
    private boolean isDeepWater(Block waterBlock) {
        if (waterBlock.getType() != Material.WATER) return false;
        
        // Verificar si hay agua debajo
        Block below = waterBlock.getRelative(0, -1, 0);
        if (below.getType() != Material.WATER) return false;
        
        // Agua profunda: al menos 2 bloques apilados
        return true;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // [v1.18.0] MECÁNICAS AVANZADAS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Crear columnas de fuego verticales en fases 4-5
     */
    private void createFireColumns() {
        List<Player> onlinePlayers = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        if (onlinePlayers.isEmpty()) return;
        
        // Crear 2 columnas aleatorias
        for (int i = 0; i < 2; i++) {
            Player target = onlinePlayers.get(random.nextInt(onlinePlayers.size()));
            Location base = target.getLocation().add(
                random.nextInt(10) - 5,
                0,
                random.nextInt(10) - 5
            );
            
            // Ajustar al suelo
            base.setY(base.getWorld().getHighestBlockYAt(base) + 1);
            
            // Crear columna de fuego de 8 bloques de alto
            for (int y = 0; y < 8; y++) {
                Location fireLoc = base.clone().add(0, y, 0);
                
                // Partículas de fuego
                fireLoc.getWorld().spawnParticle(Particle.FLAME, fireLoc, 15, 0.3, 0.3, 0.3, 0.1);
                fireLoc.getWorld().spawnParticle(Particle.LAVA, fireLoc, 5, 0.2, 0.2, 0.2, 0);
                
                // Sonido en la base
                if (y == 0) {
                    fireLoc.getWorld().playSound(fireLoc, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.8f);
                }
            }
        }
    }
    
    /**
     * Spawn Ender Dragon como "dragón de fuego" en fase 5
     */
    private void spawnFireDragon() {
        List<Player> onlinePlayers = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        if (onlinePlayers.isEmpty()) return;
        
        Player target = onlinePlayers.get(random.nextInt(onlinePlayers.size()));
        Location spawnLoc = target.getLocation().add(0, 50, 0);
        
        // Spawn Ender Dragon
        fireDragon = (EnderDragon) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ENDER_DRAGON);
        fireDragon.setCustomName("§6§lDragón del Apocalipsis");
        fireDragon.setCustomNameVisible(true);
        fireDragon.setPhase(EnderDragon.Phase.CIRCLING);
        
        // Efectos dramáticos
        spawnLoc.getWorld().spawnParticle(Particle.EXPLOSION, spawnLoc, 10, 3, 3, 3, 0);
        spawnLoc.getWorld().spawnParticle(Particle.LAVA, spawnLoc, 50, 2, 2, 2, 0);
        spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.8f);
        spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_WARDEN_ROAR, 2.0f, 0.6f);
        
        // Broadcast dramático
        messageBus.broadcast("§6§l⚠ ¡UN DRAGÓN DEL APOCALIPSIS HA EMERGIDO DEL INFIERNO!", "dragon_spawn");
        soundUtil.playSoundAll(Sound.ENTITY_ENDER_DRAGON_DEATH, 0.8f, 1.2f);
        
        // Task para dejar rastro de fuego
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (fireDragon == null || !fireDragon.isValid() || !isActive()) {
                    this.cancel();
                    return;
                }
                
                // Rastro de fuego y partículas
                Location dragonLoc = fireDragon.getLocation();
                dragonLoc.getWorld().spawnParticle(Particle.FLAME, dragonLoc, 20, 1, 1, 1, 0.1);
                dragonLoc.getWorld().spawnParticle(Particle.LAVA, dragonLoc, 10, 0.5, 0.5, 0.5, 0);
                
                // Ocasionalmente lanzar bola de fuego hacia abajo
                if (random.nextDouble() < 0.3) {
                    Location below = dragonLoc.clone().subtract(0, 10, 0);
                    SmallFireball fireball = dragonLoc.getWorld().spawn(dragonLoc, SmallFireball.class);
                    Vector direction = below.toVector().subtract(dragonLoc.toVector()).normalize();
                    fireball.setDirection(direction);
                    fireball.setYield(explosionPower * 1.5f);
                    fireball.setIsIncendiary(true);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // MÉTODOS REQUERIDOS POR DisasterBase (v1.17.0)
    // ═══════════════════════════════════════════════════════════════════
    
    @Override
    protected String getDisasterName() {
        return "LLUVIA DE FUEGO";
    }
    
    @Override
    protected String[] getPhaseNames() {
        return new String[] {
            "§7Chispas",
            "§eLluvia Ligera",
            "§6Lluvia Intensa",
            "§cInfierno",
            "§4§lAPOCALIPSIS ÍGNEO"
        };
    }
}
