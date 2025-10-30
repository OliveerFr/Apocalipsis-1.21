package me.apocalipsis.disaster;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.disaster.adapters.PerformanceAdapter;
import me.apocalipsis.state.TimeService;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;
import me.apocalipsis.utils.DisasterDamage;
import me.apocalipsis.utils.ParticleCompat;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;


public class HuracanNew extends DisasterBase {

    private double maxHorizontal;
    private double clampTotal;
    private double bajoTechoReduction;
    private double agachadoReduction;
    private boolean enableLluvia;
    private boolean enableTruenos;
    
    // Rayos mejorados
    private boolean rayosEnabled;
    private int rayosMinDelay;
    private int rayosMaxDelay;
    private int rayosNextTick;
    private boolean rayosTargetExposed;
    private boolean rayosStaticEffect;
    
    // [#10] Debris visuales (FallingBlock sin gravity)
    private boolean visualDebrisEnabled;
    private java.util.List<Material> debrisMaterials;
    private int debrisMaxPorJugador;
    private int debrisDespawnSeg;
    private final Map<UUID, Integer> debrisCountPerPlayer = new HashMap<>();
    private final java.util.List<org.bukkit.entity.FallingBlock> activeDebris = new java.util.ArrayList<>();
    
    // NUEVO: Sistema de rachas de viento
    private boolean rachaSistemaEnabled;
    private int rachaDuracionTicks;
    private int rachaIntervaloTicks;
    private double rachaMultiplicador;
    private int rachaNextTick;
    private boolean rachaActiva;
    
    // NUEVO: Objetos voladores
    private boolean objetosVoladoresEnabled;
    private int objetosMaxActivos;
    private int objetosSpawnInterval;
    private double objetosDamage;
    private final List<Item> activeItems = new ArrayList<>();
    private final List<Material> objetosMaterials = new ArrayList<>();
    private final Map<Item, Integer> itemTaskIds = new HashMap<>(); // Rastrear tasks por item
    
    // NUEVO: Inundación progresiva
    private boolean inundacionEnabled;
    private int inundacionNivelMax;
    private int inundacionTickInterval;
    private final List<Block> waterBlocks = new ArrayList<>();
    
    // NUEVO: Visibilidad reducida
    private boolean blindnessEnabled;
    private int blindnessDuracion;
    private int blindnessInterval;
    
    // NUEVO: Sistema de fases (inicio/pico/declive)
    private boolean fasesEnabled;
    private double faseMultiplicador;
    
    private final Random random = new Random();
    private final Set<UUID> playersWithStatic = new HashSet<>();

    public HuracanNew(Apocalipsis plugin, MessageBus messageBus, SoundUtil soundUtil, 
                     TimeService timeService, PerformanceAdapter performanceAdapter) {
        super(plugin, messageBus, soundUtil, timeService, performanceAdapter, "huracan");
        loadConfig();
    }

    private void loadConfig() {
        ConfigurationSection config = plugin.getConfigManager().getDesastresConfig()
            .getConfigurationSection("desastres.huracan");

        if (config != null) {
            ConfigurationSection push = config.getConfigurationSection("push");
            if (push != null) {
                maxHorizontal = push.getDouble("max_horizontal", 0.18);
                clampTotal = push.getDouble("clamp_total", 0.22);
                bajoTechoReduction = push.getDouble("bajo_techo_reduction", 0.6);
                agachadoReduction = push.getDouble("agachado_reduction", 0.55);
            }

            ConfigurationSection clima = config.getConfigurationSection("clima");
            if (clima != null) {
                enableLluvia = clima.getBoolean("lluvia", true);
                enableTruenos = clima.getBoolean("truenos", true);
            }
            
            // Rayos mejorados
            ConfigurationSection rayos = config.getConfigurationSection("rayos");
            if (rayos != null) {
                rayosEnabled = rayos.getBoolean("enabled", true);
                rayosMinDelay = rayos.getInt("min_delay_ticks", 80);
                rayosMaxDelay = rayos.getInt("max_delay_ticks", 180);
                rayosTargetExposed = rayos.getBoolean("target_exposed_players", true);
                rayosStaticEffect = rayos.getBoolean("static_effect", true);
            } else {
                rayosEnabled = true;
                rayosMinDelay = 80;
                rayosMaxDelay = 180;
                rayosTargetExposed = true;
                rayosStaticEffect = true;
            }
            
            // [#10] Visual Debris
            ConfigurationSection visualDebris = config.getConfigurationSection("visual_debris");
            if (visualDebris != null) {
                visualDebrisEnabled = visualDebris.getBoolean("enabled", true);
                java.util.List<String> matNames = visualDebris.getStringList("materiales");
                debrisMaterials = new java.util.ArrayList<>();
                for (String name : matNames) {
                    try {
                        debrisMaterials.add(Material.valueOf(name));
                    } catch (IllegalArgumentException e) {
                        // Ignorar materiales inválidos
                    }
                }
                if (debrisMaterials.isEmpty()) {
                    debrisMaterials.add(Material.DIRT);
                }
                debrisMaxPorJugador = visualDebris.getInt("max_por_jugador", 8);
                debrisDespawnSeg = visualDebris.getInt("tiempo_despawn_seg", 20);
            } else {
                visualDebrisEnabled = true;
                debrisMaterials = java.util.Arrays.asList(Material.DIRT, Material.COBBLESTONE);
                debrisMaxPorJugador = 8;
                debrisDespawnSeg = 20;
            }
            
            // NUEVO: Rachas de viento
            ConfigurationSection rachas = config.getConfigurationSection("rachas_viento");
            if (rachas != null) {
                rachaSistemaEnabled = rachas.getBoolean("enabled", true);
                rachaDuracionTicks = rachas.getInt("duracion_ticks", 100);
                rachaIntervaloTicks = rachas.getInt("intervalo_ticks", 300);
                rachaMultiplicador = rachas.getDouble("multiplicador", 2.5);
            } else {
                rachaSistemaEnabled = true;
                rachaDuracionTicks = 100;
                rachaIntervaloTicks = 300;
                rachaMultiplicador = 2.5;
            }
            
            // NUEVO: Objetos voladores
            ConfigurationSection objetos = config.getConfigurationSection("objetos_voladores");
            if (objetos != null) {
                objetosVoladoresEnabled = objetos.getBoolean("enabled", true);
                objetosMaxActivos = objetos.getInt("max_activos", 15);
                objetosSpawnInterval = objetos.getInt("spawn_interval_ticks", 20);
                objetosDamage = objetos.getDouble("damage", 1.5);
                
                List<String> matNames = objetos.getStringList("materiales");
                objetosMaterials.clear();
                for (String name : matNames) {
                    try {
                        objetosMaterials.add(Material.valueOf(name));
                    } catch (IllegalArgumentException e) {
                        // Ignorar
                    }
                }
                if (objetosMaterials.isEmpty()) {
                    objetosMaterials.addAll(Arrays.asList(Material.STICK, Material.DIRT, 
                        Material.COBBLESTONE, Material.OAK_LEAVES));
                }
            } else {
                objetosVoladoresEnabled = true;
                objetosMaxActivos = 15;
                objetosSpawnInterval = 20;
                objetosDamage = 1.5;
                objetosMaterials.addAll(Arrays.asList(Material.STICK, Material.DIRT, 
                    Material.COBBLESTONE, Material.OAK_LEAVES));
            }
            
            // NUEVO: Inundación
            ConfigurationSection inundacion = config.getConfigurationSection("inundacion");
            if (inundacion != null) {
                inundacionEnabled = inundacion.getBoolean("enabled", true);
                inundacionNivelMax = inundacion.getInt("nivel_maximo", 2);
                inundacionTickInterval = inundacion.getInt("tick_interval", 100);
            } else {
                inundacionEnabled = true;
                inundacionNivelMax = 2;
                inundacionTickInterval = 100;
            }
            
            // NUEVO: Blindness
            ConfigurationSection blindness = config.getConfigurationSection("visibilidad_reducida");
            if (blindness != null) {
                blindnessEnabled = blindness.getBoolean("enabled", true);
                blindnessDuracion = blindness.getInt("duracion_ticks", 60);
                blindnessInterval = blindness.getInt("interval_ticks", 200);
            } else {
                blindnessEnabled = true;
                blindnessDuracion = 60;
                blindnessInterval = 200;
            }
            
            // NUEVO: Sistema de fases
            ConfigurationSection fases = config.getConfigurationSection("fases");
            if (fases != null) {
                fasesEnabled = fases.getBoolean("enabled", true);
            } else {
                fasesEnabled = true;
            }
            
        } else {
            // Valores por defecto completos
            maxHorizontal = 0.18;
            clampTotal = 0.22;
            bajoTechoReduction = 0.6;
            agachadoReduction = 0.55;
            enableLluvia = true;
            enableTruenos = true;
            rayosEnabled = true;
            rayosMinDelay = 80;
            rayosMaxDelay = 180;
            rayosTargetExposed = true;
            rayosStaticEffect = true;
            visualDebrisEnabled = true;
            debrisMaterials = java.util.Arrays.asList(Material.DIRT, Material.COBBLESTONE);
            debrisMaxPorJugador = 8;
            debrisDespawnSeg = 20;
            rachaSistemaEnabled = true;
            rachaDuracionTicks = 100;
            rachaIntervaloTicks = 300;
            rachaMultiplicador = 2.5;
            objetosVoladoresEnabled = true;
            objetosMaxActivos = 15;
            objetosSpawnInterval = 20;
            objetosDamage = 1.5;
            objetosMaterials.addAll(Arrays.asList(Material.STICK, Material.DIRT, 
                Material.COBBLESTONE, Material.OAK_LEAVES));
            inundacionEnabled = true;
            inundacionNivelMax = 2;
            inundacionTickInterval = 100;
            blindnessEnabled = true;
            blindnessDuracion = 60;
            blindnessInterval = 200;
            fasesEnabled = true;
        }
        
        // Programar primer rayo y racha
        scheduleNextLightning();
        rachaNextTick = rachaIntervaloTicks;
        rachaActiva = false;
        faseMultiplicador = 1.0;
    }

    @Override
    protected void onStart() {
        // Aplicar clima
        for (World world : Bukkit.getWorlds()) {
            if (enableLluvia) {
                world.setStorm(true);
                world.setWeatherDuration(999999);
            }
            if (enableTruenos) {
                world.setThundering(true);
                world.setThunderDuration(999999);
            }
        }
        
        // Reset counters y listas
        debrisCountPerPlayer.clear();
        activeItems.clear();
        waterBlocks.clear();
        playersWithStatic.clear();
        scheduleNextLightning();
        rachaNextTick = rachaIntervaloTicks;
        rachaActiva = false;
        faseMultiplicador = 1.0;
    }

    @Override
    protected void onStop() {
        // Restaurar clima
        for (World world : Bukkit.getWorlds()) {
            world.setStorm(false);
            world.setThundering(false);
        }
        
        // Cleanup de debris visuales
        cleanupDebris();
        
        // [FIX] Limpiar el mapa de tasks (ya no es necesario con el nuevo sistema)
        itemTaskIds.clear();
        
        // Cleanup de objetos voladores
        for (Item item : activeItems) {
            if (item != null && item.isValid()) {
                item.remove();
            }
        }
        activeItems.clear();
        
        // Cleanup de inundación
        for (Block block : waterBlocks) {
            if (block != null && block.getType() == Material.WATER) {
                block.setType(Material.AIR);
            }
        }
        waterBlocks.clear();
        
        // Remover efectos de estática
        for (UUID uuid : playersWithStatic) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.setGlowing(false);
            }
        }
        playersWithStatic.clear();
    }

    @Override
    protected void onTick() {
        // Calcular fase actual para multiplicador de intensidad
        updatePhaseMultiplier();
        
        // Sonidos ambientales cada 3 segundos
        if (tickCounter % 60 == 0) {
            soundUtil.playSoundAll(Sound.ITEM_ELYTRA_FLYING, 0.3f, 0.8f);
        }

        // Truenos aleatorios cada 5 segundos
        if (enableTruenos && tickCounter % 100 == 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (Math.random() < 0.3) {
                    soundUtil.playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.0f);
                }
            }
        }
        
        // Sistema de rachas de viento
        updateRachas();
        
        // Rayos mejorados
        if (rayosEnabled && tickCounter >= rayosNextTick) {
            spawnImprovedLightning();
            scheduleNextLightning();
        }
        
        // Visual Debris (FallingBlock sin gravity)
        if (visualDebrisEnabled && tickCounter % 40 == 0) {
            spawnVisualDebris();
        }
        
        // NUEVO: Objetos voladores
        if (objetosVoladoresEnabled && tickCounter % objetosSpawnInterval == 0) {
            spawnFlyingObjects();
        }
        
        // NUEVO: Inundación progresiva
        if (inundacionEnabled && tickCounter % inundacionTickInterval == 0) {
            expandFlood();
        }
        
        // Cleanup de objetos voladores fuera de rango
        if (tickCounter % 20 == 0) {
            cleanupFlyingObjects();
        }
    }
    
    private void scheduleNextLightning() {
        rayosNextTick = tickCounter + rayosMinDelay + random.nextInt(rayosMaxDelay - rayosMinDelay + 1);
    }
    
    /**
     * [#10] Spawn debris visuales seguros (FallingBlock sin gravity, no duplica bloques)
     */
    private void spawnVisualDebris() {
        double scale = getPerformanceScale();
        if (scale <= 0) return;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            // [#5] Skip jugadores con excepción
            if (isPlayerExempt(player)) continue;
            
            UUID uuid = player.getUniqueId();
            int current = debrisCountPerPlayer.getOrDefault(uuid, 0);
            
            // Cap por jugador
            if (current >= debrisMaxPorJugador) continue;
            
            // Probabilidad reducida
            if (random.nextDouble() > 0.4 * scale) continue;
            
            Location playerLoc = player.getLocation();
            
            // [#10] Seleccionar material aleatorio de la whitelist (NO del suelo)
            Material debrisMat = debrisMaterials.get(random.nextInt(debrisMaterials.size()));
            BlockData blockData = debrisMat.createBlockData();
            
            // Spawn FallingBlock cerca del jugador
            double offsetX = (random.nextDouble() - 0.5) * 6;
            double offsetZ = (random.nextDouble() - 0.5) * 6;
            Location spawnLoc = playerLoc.clone().add(offsetX, 2, offsetZ); // Más alto para efecto visual
            
            // [#10] Spawn sin gravity = visual puro, no duplica bloques
            @SuppressWarnings("deprecation")
            org.bukkit.entity.FallingBlock debris = player.getWorld().spawnFallingBlock(spawnLoc, blockData);
            
            debris.setGravity(false); // [#10] SIN GRAVEDAD = no colisiona ni duplica
            debris.setDropItem(false);
            debris.setHurtEntities(false);
            
            // Velocity horizontal para efecto de viento
            double velX = (random.nextDouble() - 0.5) * 0.3;
            double velY = -0.05; // Caída lenta visual
            double velZ = (random.nextDouble() - 0.5) * 0.3;
            debris.setVelocity(new Vector(velX, velY, velZ));
            
            activeDebris.add(debris);
            debrisCountPerPlayer.put(uuid, current + 1);
            
            // Remover tras tiempo configurado
            long despawnTicks = debrisDespawnSeg * 20L;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!debris.isDead()) {
                    debris.remove();
                    activeDebris.remove(debris);
                }
                debrisCountPerPlayer.put(uuid, Math.max(0, debrisCountPerPlayer.getOrDefault(uuid, 0) - 1));
            }, despawnTicks);
        }
    }
    
    /**
     * [#10] Cleanup de todos los debris activos
     */
    private void cleanupDebris() {
        for (org.bukkit.entity.FallingBlock debris : activeDebris) {
            if (!debris.isDead()) {
                debris.remove();
            }
        }
        activeDebris.clear();
        debrisCountPerPlayer.clear();
    }

    @Override
    public void applyEffects(Player player) {
        if (shouldSkipTick(2)) return; // Aplicar cada 2 ticks
        
        // [#5] Verificar excepciones administrativas
        if (isPlayerExempt(player)) return;

        // Obtener escala de rendimiento
        double scale = getPerformanceScale();
        if (scale <= 0) return; // SAFE_MODE: no aplicar efectos

        // Aplicar multiplicador de fase
        double effectiveScale = scale * faseMultiplicador;

        // Empuje horizontal con sistema de rachas
        double rachaFactor = rachaActiva && rachaSistemaEnabled ? rachaMultiplicador : 1.0;
        double pushX = (Math.random() - 0.5) * 2 * maxHorizontal * effectiveScale * rachaFactor;
        double pushZ = (Math.random() - 0.5) * 2 * maxHorizontal * effectiveScale * rachaFactor;

        // Reducción si está bajo techo (zona segura)
        if (isUnderRoof(player)) {
            pushX *= (1 - bajoTechoReduction);
            pushZ *= (1 - bajoTechoReduction);
        }

        // Reducción si está agachado
        if (player.isSneaking()) {
            pushX *= (1 - agachadoReduction);
            pushZ *= (1 - agachadoReduction);
        }

        // NUEVO: Durante ráfaga fuerte, añadir levitate
        if (rachaActiva && rachaSistemaEnabled && !isUnderRoof(player)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20, 0, false, false));
        }

        Vector velocity = player.getVelocity();
        velocity.add(new Vector(pushX, 0, pushZ));

        // Clamp velocidad total
        double totalSpeed = Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ());
        if (totalSpeed > clampTotal * rachaFactor) {
            velocity.multiply(clampTotal * rachaFactor / totalSpeed);
        }

        player.setVelocity(velocity);
        player.setFallDistance(0); // Evitar daño por caída del empuje

        // Partículas de viento cada 10 ticks
        if (tickCounter % 10 == 0) {
            spawnWindParticles(player);
        }
        
        // NUEVO: Visibilidad reducida (blindness periódica)
        if (blindnessEnabled && tickCounter % blindnessInterval == 0) {
            if (!isUnderRoof(player)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 
                    blindnessDuracion, 0, false, false));
            }
        }

        // Daño aleatorio cada 40 ticks
        if (tickCounter % 40 == 0) {
            ConfigurationSection config = plugin.getConfigManager().getDesastresConfig();
            DisasterDamage.maybeDamage(player, "huracan", config, messageBus, soundUtil);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // NUEVOS MÉTODOS - MEJORAS DEL HURACÁN
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Actualiza el multiplicador de fase (inicio 0.5x / pico 1.5x / declive 0.8x)
     */
    private void updatePhaseMultiplier() {
        if (!fasesEnabled) {
            faseMultiplicador = 1.0;
            return;
        }
        
        int totalSeconds = timeService.getPlannedSeconds();
        int remainingSeconds = timeService.getRemainingSeconds();
        int elapsedSeconds = totalSeconds - remainingSeconds;
        
        double progress = (double) elapsedSeconds / totalSeconds;
        
        // Fases: 0-20% inicio, 20-80% pico, 80-100% declive
        if (progress < 0.2) {
            // Fase inicio: 0.5x → 1.0x
            faseMultiplicador = 0.5 + (progress / 0.2) * 0.5;
        } else if (progress < 0.8) {
            // Fase pico: 1.0x → 1.5x en el centro
            double pikoProg = (progress - 0.2) / 0.6; // 0 a 1
            if (pikoProg < 0.5) {
                faseMultiplicador = 1.0 + pikoProg * 1.0; // 1.0 → 1.5
            } else {
                faseMultiplicador = 1.5 - (pikoProg - 0.5) * 1.0; // 1.5 → 1.0
            }
            
            // Advertencia en pico máximo
            if (elapsedSeconds == totalSeconds / 2) {
                messageBus.broadcast("§c§l⚠ ¡EL HURACÁN ALCANZA SU MÁXIMA INTENSIDAD!", "huracan_peak");
                soundUtil.playSoundAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
            }
        } else {
            // Fase declive: 1.0x → 0.8x
            double decliveProg = (progress - 0.8) / 0.2;
            faseMultiplicador = 1.0 - decliveProg * 0.2;
        }
    }
    
    /**
     * Sistema de rachas de viento (intervalos de viento calma vs ráfagas extremas)
     */
    private void updateRachas() {
        if (!rachaSistemaEnabled) return;
        
        if (tickCounter >= rachaNextTick) {
            rachaActiva = !rachaActiva;
            
            if (rachaActiva) {
                // Iniciar ráfaga
                rachaNextTick = tickCounter + rachaDuracionTicks;
                messageBus.broadcast("§e⚠ §c¡RÁFAGA DE VIENTO FUERTE!", "racha_start");
                soundUtil.playSoundAll(Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.6f);
                soundUtil.playSoundAll(Sound.ITEM_ELYTRA_FLYING, 1.0f, 0.5f);
            } else {
                // Fin de ráfaga, programar siguiente
                rachaNextTick = tickCounter + rachaIntervaloTicks;
                messageBus.broadcast("§7El viento se calma momentáneamente...", "racha_end");
            }
        }
    }
    
    /**
     * Rayos mejorados con targeting a jugadores expuestos y efecto de estática
     */
    private void spawnImprovedLightning() {
        double scale = getPerformanceScale();
        if (scale <= 0) return;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isPlayerExempt(player)) continue;
            
            // Mayor chance si está expuesto (sin techo)
            double chance = rayosTargetExposed && !isUnderRoof(player) ? 0.15 : 0.03;
            
            if (random.nextDouble() < chance * scale) {
                Location loc = player.getLocation();
                
                // Efecto de estática 2s antes
                if (rayosStaticEffect) {
                    player.setGlowing(true);
                    playersWithStatic.add(player.getUniqueId());
                    player.sendMessage("§e⚡ §7Sientes electricidad estática...");
                    soundUtil.playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 2.0f);
                    
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.setGlowing(false);
                        playersWithStatic.remove(player.getUniqueId());
                        
                        if (!isActive()) return;
                        
                        // Rayo real
                        loc.getWorld().strikeLightning(loc);
                        player.sendMessage("§c⚡ ¡IMPACTO DE RAYO!");
                    }, 40L); // 2 segundos
                } else {
                    // Rayo directo sin advertencia
                    loc.getWorld().strikeLightningEffect(loc);
                }
            }
        }
    }
    
    /**
     * Spawn de objetos voladores que pueden golpear jugadores
     */
    private void spawnFlyingObjects() {
        if (activeItems.size() >= objetosMaxActivos) return;
        
        double scale = getPerformanceScale();
        if (scale <= 0) return;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isPlayerExempt(player)) continue;
            if (random.nextDouble() > 0.3 * scale) continue;
            
            Location loc = player.getLocation().add(
                (random.nextDouble() - 0.5) * 10,
                3 + random.nextDouble() * 3,
                (random.nextDouble() - 0.5) * 10
            );
            
            Material mat = objetosMaterials.get(random.nextInt(objetosMaterials.size()));
            Item item = loc.getWorld().dropItem(loc, new org.bukkit.inventory.ItemStack(mat));
            item.setPickupDelay(Integer.MAX_VALUE);
            item.setVelocity(new Vector(
                (random.nextDouble() - 0.5) * 0.8,
                -0.2,
                (random.nextDouble() - 0.5) * 0.8
            ));
            
            activeItems.add(item);
            
            // [FIX] Verificar colisión más eficientemente - el item se auto-limpia en cleanupFlyingObjects
            // No crear un task por item, usar el cleanup cada 20 ticks
        }
    }
    
    /**
     * Cleanup de objetos voladores fuera de rango o antiguos + detección de colisiones
     */
    private void cleanupFlyingObjects() {
        activeItems.removeIf(item -> {
            if (!item.isValid() || item.getTicksLived() > 600) { // 30 segundos
                if (item.isValid()) item.remove();
                return true;
            }
            
            // [FIX] Detectar colisiones aquí en lugar de crear task por item
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getLocation().distance(item.getLocation()) < 1.5) {
                    if (!isPlayerExempt(p)) {
                        p.damage(objetosDamage);
                        p.sendMessage("§e💨 §7¡Objeto volador te golpeó!");
                        soundUtil.playSound(p, Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
                    }
                    item.remove();
                    return true;
                }
            }
            
            return false;
        });
    }
    
    /**
     * Expande la inundación en zonas bajas
     */
    private void expandFlood() {
        double scale = getPerformanceScale();
        if (scale <= 0) return;
        
        // Limitar cantidad de bloques de agua
        if (waterBlocks.size() >= 100) return;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isPlayerExempt(player)) continue;
            
            Location loc = player.getLocation();
            
            // Solo en zonas bajas (Y < 70)
            if (loc.getY() > 70) continue;
            
            // Buscar bloques aire bajos cerca del jugador
            for (int attempts = 0; attempts < 3; attempts++) {
                int offsetX = random.nextInt(10) - 5;
                int offsetZ = random.nextInt(10) - 5;
                
                Location checkLoc = loc.clone().add(offsetX, -1, offsetZ);
                Block below = checkLoc.getBlock();
                
                // Encontrar superficie
                while (below.getType() == Material.AIR && below.getY() > loc.getWorld().getMinHeight()) {
                    below = below.getRelative(0, -1, 0);
                }
                
                Block waterPos = below.getRelative(0, 1, 0);
                
                // Verificar que sea válido para agua
                if (waterPos.getType() == Material.AIR && waterPos.getY() < loc.getY()) {
                    // Verificar nivel máximo de inundación
                    int currentLevel = 0;
                    Block check = waterPos;
                    while (check.getType() == Material.WATER && currentLevel < inundacionNivelMax) {
                        currentLevel++;
                        check = check.getRelative(0, 1, 0);
                    }
                    
                    if (currentLevel < inundacionNivelMax) {
                        waterPos.setType(Material.WATER);
                        waterBlocks.add(waterPos);
                        
                        // Partículas de splash
                        waterPos.getWorld().spawnParticle(Particle.SPLASH, 
                            waterPos.getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3);
                    }
                }
            }
        }
    }
    
    /**
     * Partículas de viento mejoradas
     */
    private void spawnWindParticles(Player player) {
        Location loc = player.getLocation();
        
        // Nubes de viento
        player.getWorld().spawnParticle(ParticleCompat.cloud(), 
            loc.clone().add(0, 1, 0), 5, 0.8, 0.5, 0.8, 0.05);
        
        // Humo del suelo
        player.getWorld().spawnParticle(ParticleCompat.smokeNormal(), 
            loc, 3, 0.5, 0.2, 0.5, 0.02);
        
        // BlockDust del suelo
        Location groundLoc = loc.clone().subtract(0, 1, 0);
        BlockData data = groundLoc.getBlock().getBlockData();
        player.getWorld().spawnParticle(ParticleCompat.blockDust(), 
            loc, 3, 0.5, 0.1, 0.5, 0.02, data);
        
        // Durante ráfagas: partículas más intensas
        if (rachaActiva && rachaSistemaEnabled) {
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, 
                loc.clone().add(0, 1, 0), 2, 1, 0.5, 1);
        }
    }

    private boolean isUnderRoof(Player player) {
        Location loc = player.getLocation();
        for (int y = 1; y <= 5; y++) {
            Block block = loc.clone().add(0, y, 0).getBlock();
            if (block.getType().isSolid()) {
                return true;
            }
        }
        return false;
    }
}

