package me.apocalipsis.disaster;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.disaster.adapters.PerformanceAdapter;
import me.apocalipsis.state.TimeService;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;
import me.apocalipsis.utils.DisasterDamage;
import me.apocalipsis.utils.EffectUtil;
import me.apocalipsis.utils.ParticleCompat;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TerremotoNew extends DisasterBase {

    private double shakeIntensity;
    private int shakeEveryTicks;
    private double breakChance;
    private int breakRadius;
    private boolean romperBloques;
    private Set<Material> romperWhitelist;
    private Map<Material, Double> materialDurability; // Probabilidad por material
    
    private final Map<UUID, Long> lastNauseaTime = new HashMap<>();
    private static final long NAUSEA_COOLDOWN_MS = 5000;
    
    // NUEVO: Grietas en el suelo
    private boolean grietasEnabled;
    private int grietasMaxActivas;
    private int grietasLongitud;
    private final List<Block> grietaBlocks = new ArrayList<>();
    
    // NUEVO: Derrumbes de bloques superiores
    private boolean derrumbesEnabled;
    private double derrumbesChance;
    private int derrumbesMaxAltura;
    
    // NUEVO: Sistema de réplicas (aftershocks)
    private boolean aftershocksEnabled;
    private int aftershockIntervalo;
    private double aftershockMultiplicador;
    private int nextAfterShock;
    private boolean isAfterShock;
    
    // NUEVO: Ondas sísmicas visuales
    private boolean ondasEnabled;
    private int ondasIntervalo;
    
    // NUEVO: Efectos de desorientación mejorados
    private boolean desorientacionEnabled;
    private int desorientacionDuracion;
    
    // NUEVO: Sistema de fases
    private boolean fasesEnabled;
    private double faseMultiplicador;
    
    // NUEVO: Sistema de absorción de impacto
    private boolean absorcionEnabled;
    private int absorcionRadio;
    private double absorcionReduccionShake;
    private double absorcionReduccionBreak;
    private double absorcionReduccionDamage;
    private int absorcionMaxBloques;
    private Set<Material> absorcionMateriales;
    
    // NUEVO: Rotura de bloques de protección
    private boolean romperProteccionEnabled;
    private double romperProteccionProbabilidad;
    private int romperProteccionCantidad;
    
    private final Random random = new Random();
    private final List<Location> epicentros = new ArrayList<>();

    public TerremotoNew(Apocalipsis plugin, MessageBus messageBus, SoundUtil soundUtil, 
                       TimeService timeService, PerformanceAdapter performanceAdapter) {
        super(plugin, messageBus, soundUtil, timeService, performanceAdapter, "terremoto");
        loadConfig();
    }

    private void loadConfig() {
        ConfigurationSection config = plugin.getConfigManager().getDesastresConfig()
            .getConfigurationSection("desastres.terremoto");

        if (config != null) {
            shakeIntensity = config.getDouble("shake_intensity", 0.08);
            shakeEveryTicks = config.getInt("shake_every_ticks", 3);
            breakChance = config.getDouble("break_chance", 0.02);
            breakRadius = config.getInt("break_radius", 3);
            romperBloques = config.getBoolean("romper_bloques", true);
            
            // Cargar whitelist de materiales rompibles
            java.util.List<String> whitelistNames = config.getStringList("romper_whitelist");
            romperWhitelist = new java.util.HashSet<>();
            for (String name : whitelistNames) {
                try {
                    romperWhitelist.add(Material.valueOf(name));
                } catch (IllegalArgumentException e) {
                    // Ignorar materiales inválidos
                }
            }
            if (romperWhitelist.isEmpty()) {
                romperWhitelist = EnumSet.of(Material.DIRT, Material.GRASS_BLOCK, Material.COBBLESTONE, 
                    Material.STONE, Material.GRAVEL, Material.SAND);
            }
            
            // Cargar probabilidades por dureza de material
            loadMaterialDurability(config);
            
            // NUEVO: Grietas
            ConfigurationSection grietasConf = config.getConfigurationSection("grietas");
            if (grietasConf != null) {
                grietasEnabled = grietasConf.getBoolean("enabled", true);
                grietasMaxActivas = grietasConf.getInt("max_activas", 5);
                grietasLongitud = grietasConf.getInt("longitud_maxima", 8);
            } else {
                grietasEnabled = true;
                grietasMaxActivas = 5;
                grietasLongitud = 8;
            }
            
            // NUEVO: Derrumbes
            ConfigurationSection derrumbesConf = config.getConfigurationSection("derrumbes");
            if (derrumbesConf != null) {
                derrumbesEnabled = derrumbesConf.getBoolean("enabled", true);
                derrumbesChance = derrumbesConf.getDouble("chance_por_tick", 0.05);
                derrumbesMaxAltura = derrumbesConf.getInt("max_altura_busqueda", 10);
            } else {
                derrumbesEnabled = true;
                derrumbesChance = 0.05;
                derrumbesMaxAltura = 10;
            }
            
            // NUEVO: Aftershocks (réplicas)
            ConfigurationSection afterConf = config.getConfigurationSection("aftershocks");
            if (afterConf != null) {
                aftershocksEnabled = afterConf.getBoolean("enabled", true);
                aftershockIntervalo = afterConf.getInt("intervalo_ticks", 300);
                aftershockMultiplicador = afterConf.getDouble("multiplicador_intensidad", 1.5);
            } else {
                aftershocksEnabled = true;
                aftershockIntervalo = 300;
                aftershockMultiplicador = 1.5;
            }
            
            // NUEVO: Ondas sísmicas
            ConfigurationSection ondasConf = config.getConfigurationSection("ondas_sismicas");
            if (ondasConf != null) {
                ondasEnabled = ondasConf.getBoolean("enabled", true);
                ondasIntervalo = ondasConf.getInt("intervalo_ticks", 80);
            } else {
                ondasEnabled = true;
                ondasIntervalo = 80;
            }
            
            // NUEVO: Desorientación
            ConfigurationSection desorConf = config.getConfigurationSection("desorientacion");
            if (desorConf != null) {
                desorientacionEnabled = desorConf.getBoolean("enabled", true);
                desorientacionDuracion = desorConf.getInt("duracion_ticks", 100);
            } else {
                desorientacionEnabled = true;
                desorientacionDuracion = 100;
            }
            
            // NUEVO: Sistema de fases
            fasesEnabled = config.getBoolean("fases.enabled", true);
            
            // NUEVO: Sistema de absorción de impacto
            ConfigurationSection absorcionConf = config.getConfigurationSection("absorcion_impacto");
            if (absorcionConf != null) {
                absorcionEnabled = absorcionConf.getBoolean("enabled", true);
                absorcionRadio = absorcionConf.getInt("radio_deteccion", 5);
                absorcionReduccionShake = absorcionConf.getDouble("reduccion_shake_por_bloque", 0.15);
                absorcionReduccionBreak = absorcionConf.getDouble("reduccion_break_por_bloque", 0.20);
                absorcionReduccionDamage = absorcionConf.getDouble("reduccion_damage_por_bloque", 0.25);
                absorcionMaxBloques = absorcionConf.getInt("max_bloques_efectivos", 4);
                
                // Cargar materiales absorbentes
                java.util.List<String> materiales = absorcionConf.getStringList("materiales");
                absorcionMateriales = new java.util.HashSet<>();
                for (String mat : materiales) {
                    try {
                        absorcionMateriales.add(Material.valueOf(mat));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("[Terremoto] Material absorbente inválido: " + mat);
                    }
                }
                
                // Defaults si no hay materiales configurados
                if (absorcionMateriales.isEmpty()) {
                    absorcionMateriales = EnumSet.of(
                        Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL, Material.LIGHT_BLUE_WOOL,
                        Material.YELLOW_WOOL, Material.LIME_WOOL, Material.PINK_WOOL, Material.GRAY_WOOL,
                        Material.LIGHT_GRAY_WOOL, Material.CYAN_WOOL, Material.PURPLE_WOOL, Material.BLUE_WOOL,
                        Material.BROWN_WOOL, Material.GREEN_WOOL, Material.RED_WOOL, Material.BLACK_WOOL,
                        Material.SLIME_BLOCK, Material.BLUE_ICE, Material.HAY_BLOCK
                    );
                }
            } else {
                absorcionEnabled = true;
                absorcionRadio = 5;
                absorcionReduccionShake = 0.15;
                absorcionReduccionBreak = 0.20;
                absorcionReduccionDamage = 0.25;
                absorcionMaxBloques = 4;
                absorcionMateriales = EnumSet.of(
                    Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL, Material.LIGHT_BLUE_WOOL,
                    Material.YELLOW_WOOL, Material.LIME_WOOL, Material.PINK_WOOL, Material.GRAY_WOOL,
                    Material.LIGHT_GRAY_WOOL, Material.CYAN_WOOL, Material.PURPLE_WOOL, Material.BLUE_WOOL,
                    Material.BROWN_WOOL, Material.GREEN_WOOL, Material.RED_WOOL, Material.BLACK_WOOL,
                    Material.SLIME_BLOCK, Material.BLUE_ICE, Material.HAY_BLOCK
                );
            }
            
            // NUEVO: Rotura de bloques de protección
            ConfigurationSection romperProtConf = config.getConfigurationSection("romper_proteccion");
            if (romperProtConf != null) {
                romperProteccionEnabled = romperProtConf.getBoolean("enabled", true);
                romperProteccionProbabilidad = romperProtConf.getDouble("probabilidad", 0.25);
                romperProteccionCantidad = romperProtConf.getInt("cantidad_bloques", 1);
            } else {
                romperProteccionEnabled = true;
                romperProteccionProbabilidad = 0.25; // 25% de probabilidad por tick
                romperProteccionCantidad = 1; // Romper 1 bloque absorbente
            }
            
        } else {
            shakeIntensity = 0.08;
            shakeEveryTicks = 3;
            breakChance = 0.02;
            breakRadius = 3;
            romperBloques = true;
            romperWhitelist = EnumSet.of(Material.DIRT, Material.GRASS_BLOCK, Material.COBBLESTONE, 
                Material.STONE, Material.GRAVEL, Material.SAND);
            loadMaterialDurability(null); // Cargar valores por defecto
            grietasEnabled = true;
            grietasMaxActivas = 5;
            grietasLongitud = 8;
            derrumbesEnabled = true;
            derrumbesChance = 0.05;
            derrumbesMaxAltura = 10;
            aftershocksEnabled = true;
            aftershockIntervalo = 300;
            aftershockMultiplicador = 1.5;
            ondasEnabled = true;
            ondasIntervalo = 80;
            desorientacionEnabled = true;
            desorientacionDuracion = 100;
            fasesEnabled = true;
        }
    }

    /**
     * Carga las probabilidades de rotura según la dureza del material.
     * Escala: 2% (blandos) → 1% (medios) → 0.5% (duros) → 0.25% (muy duros)
     */
    private void loadMaterialDurability(ConfigurationSection config) {
        materialDurability = new HashMap<>();
        
        // TIER 1: Materiales blandos (2% - más fácil de romper)
        double softProbability = (config != null) ? config.getDouble("durability.soft", 0.02) : 0.02;
        materialDurability.put(Material.DIRT, softProbability);
        materialDurability.put(Material.GRASS_BLOCK, softProbability);
        materialDurability.put(Material.COARSE_DIRT, softProbability);
        materialDurability.put(Material.PODZOL, softProbability);
        materialDurability.put(Material.MYCELIUM, softProbability);
        materialDurability.put(Material.SAND, softProbability);
        materialDurability.put(Material.RED_SAND, softProbability);
        materialDurability.put(Material.GRAVEL, softProbability);
        materialDurability.put(Material.CLAY, softProbability);
        materialDurability.put(Material.SOUL_SAND, softProbability);
        materialDurability.put(Material.SOUL_SOIL, softProbability);
        
        // TIER 2: Materiales medios (1% - resistencia moderada)
        double mediumProbability = (config != null) ? config.getDouble("durability.medium", 0.01) : 0.01;
        materialDurability.put(Material.COBBLESTONE, mediumProbability);
        materialDurability.put(Material.NETHERRACK, mediumProbability);
        materialDurability.put(Material.END_STONE, mediumProbability);
        materialDurability.put(Material.SANDSTONE, mediumProbability);
        materialDurability.put(Material.RED_SANDSTONE, mediumProbability);
        
        // TIER 3: Materiales duros (0.5% - alta resistencia)
        double hardProbability = (config != null) ? config.getDouble("durability.hard", 0.005) : 0.005;
        materialDurability.put(Material.STONE, hardProbability);
        materialDurability.put(Material.ANDESITE, hardProbability);
        materialDurability.put(Material.DIORITE, hardProbability);
        materialDurability.put(Material.GRANITE, hardProbability);
        materialDurability.put(Material.DEEPSLATE, hardProbability);
        materialDurability.put(Material.COBBLED_DEEPSLATE, hardProbability);
        
        // TIER 4: Materiales muy duros (0.25% - máxima resistencia)
        double veryHardProbability = (config != null) ? config.getDouble("durability.very_hard", 0.0025) : 0.0025;
        materialDurability.put(Material.TUFF, veryHardProbability);
        materialDurability.put(Material.CALCITE, veryHardProbability);
        materialDurability.put(Material.BASALT, veryHardProbability);
        materialDurability.put(Material.BLACKSTONE, veryHardProbability);
    }

    @Override
    protected void onStart() {
        grietaBlocks.clear();
        epicentros.clear();
        nextAfterShock = tickCounter + aftershockIntervalo;
        isAfterShock = false;
        faseMultiplicador = 1.0;
    }

    @Override
    protected void onStop() {
        lastNauseaTime.clear();
        
        // Restaurar grietas
        for (Block b : grietaBlocks) {
            if (b.getType() == Material.AIR || b.getType() == Material.LAVA) {
                b.setType(Material.STONE);
            }
        }
        grietaBlocks.clear();
        epicentros.clear();
    }

    @Override
    protected void onTick() {
        // Actualizar sistema de fases
        updatePhaseMultiplier();
        
        // Actualizar aftershocks
        updateAfterShocks();
        
        // Sonidos ambiente
        if (tickCounter % 60 == 0) {
            soundUtil.playSoundAll(Sound.BLOCK_ANVIL_PLACE, 0.4f, 0.6f);
        }

        if (tickCounter % 80 == 0) {
            soundUtil.playSoundAll(Sound.BLOCK_STONE_BREAK, 0.5f, 0.5f);
        }
        
        // Ondas sísmicas visuales
        if (ondasEnabled && tickCounter % ondasIntervalo == 0) {
            spawnSeismicWave();
        }
        
        // Generar nuevas grietas
        if (grietasEnabled && tickCounter % 100 == 0) {
            spawnCracks();
        }
    }

    @Override
    public void applyEffects(Player player) {
        if (isPlayerExempt(player)) return;
        
        double scale = getPerformanceScale();
        if (scale <= 0) return;

        // [NUEVO] Calcular absorción de impacto
        AbsorptionInfo absorption = calculateAbsorption(player.getLocation());
        
        // Mostrar partículas de protección cada 20 ticks si hay absorción
        if (absorption.blockCount > 0 && tickCounter % 20 == 0) {
            spawnAbsorptionParticles(player.getLocation(), absorption.blockCount);
            
            // [NUEVO] Intentar romper bloques de protección con probabilidad
            maybeBreakProtectionBlocks(player.getLocation(), absorption.blockCount);
        }

        // Aplicar shake con multiplicador de fase, aftershock Y absorción
        double currentIntensity = shakeIntensity * faseMultiplicador * absorption.shakeMultiplier;
        if (isAfterShock) {
            currentIntensity *= aftershockMultiplicador;
        }
        applyShake(player, scale, currentIntensity);
        
        // Romper bloques del suelo (reducido por absorción)
        maybeBreakGround(player, absorption.breakMultiplier);

        // Partículas cada 5 ticks
        if (tickCounter % 5 == 0) {
            spawnGroundParticles(player);
        }
        
        // Columnas de polvo cuando rompe bloques
        if (romperBloques && tickCounter % 40 == 0) {
            spawnDustColumns(player);
        }

        // Desorientación mejorada
        if (desorientacionEnabled && tickCounter % 100 == 0) {
            applyDisorientation(player);
        }
        
        // Náusea con cooldown
        if (tickCounter % 100 == 0) {
            applyNauseaWithCooldown(player);
        }
        
        // Derrumbes de bloques superiores (reducido por absorción)
        double adjustedDerrumbesChance = derrumbesChance * scale * absorption.damageMultiplier;
        if (derrumbesEnabled && random.nextDouble() < adjustedDerrumbesChance) {
            spawnFallingDebris(player);
        }

        // Daño aleatorio cada 50 ticks (reducido por absorción)
        if (tickCounter % 50 == 0) {
            ConfigurationSection config = plugin.getConfigManager().getDesastresConfig();
            
            // Si hay absorción significativa (2+ bloques), reducir daño
            if (absorption.blockCount >= 2 && random.nextDouble() > absorption.damageMultiplier) {
                // Skip damage - protegido por absorción
                return;
            }
            
            DisasterDamage.maybeDamage(player, "terremoto", config, messageBus, soundUtil);
        }
    }

    private void applyShake(Player p, double scale, double intensity) {
        if (tickCounter % shakeEveryTicks != 0) return;
        
        double k = intensity * scale;
        if (k <= 0) return;

        // Reducción extra si está agachado o bajo techo
        if (p.isSneaking() || isUnderRoof(p)) {
            k *= 0.4;
        }

        Vector v = new Vector(
            ThreadLocalRandom.current().nextDouble(-k, k),
            0.0,
            ThreadLocalRandom.current().nextDouble(-k, k)
        );
        p.setVelocity(p.getVelocity().add(v));
        p.setFallDistance(0f);
    }

    private boolean isUnderRoof(Player p) {
        Location loc = p.getLocation();
        World w = p.getWorld();
        int y = loc.getBlockY();
        
        for (int i = 1; i <= 3; i++) {
            Block above = w.getBlockAt(loc.getBlockX(), y + i, loc.getBlockZ());
            if (above.getType().isSolid()) {
                return true;
            }
        }
        return false;
    }

    private void maybeBreakGround(Player p, double absorptionMultiplier) {
        // [#6] Si romper_bloques está deshabilitado, mantener efectos visuales pero no romper
        if (!romperBloques) return;
        
        // Reducir probabilidad según absorción
        double adjustedChance = breakChance * absorptionMultiplier;
        if (ThreadLocalRandom.current().nextDouble() > adjustedChance) return;
        
        World w = p.getWorld();
        Location c = p.getLocation();
        int tried = 0, broken = 0, limit = 3; // máx 3 por pulso

        while (tried++ < 10 && broken < limit) {
            int dx = ThreadLocalRandom.current().nextInt(-breakRadius, breakRadius + 1);
            int dz = ThreadLocalRandom.current().nextInt(-breakRadius, breakRadius + 1);
            Location l = c.clone().add(dx, -1, dz);
            Block b = l.getBlock();
            
            // [#11] Solo romper bloques en la whitelist
            if (!romperWhitelist.contains(b.getType())) continue;
            
            // Obtener probabilidad según dureza del material
            double probability = materialDurability.getOrDefault(b.getType(), breakChance);
            if (ThreadLocalRandom.current().nextDouble() > probability) continue;

            // Partículas y sonido previos
            Location particleLoc = l.clone().add(0.5, 1.0, 0.5);
            w.spawnParticle(ParticleCompat.blockCrack(), particleLoc, 12, 0.25, 0.15, 0.25, 0.01, b.getBlockData());
            w.playSound(particleLoc, Sound.BLOCK_STONE_BREAK, 0.6f, 0.9f);

            b.setType(Material.AIR, false); // sin actualizar física masiva
            broken++;
        }
    }

    private void spawnGroundParticles(Player player) {
        Location loc = player.getLocation();
        Material groundMaterial = loc.clone().subtract(0, 1, 0).getBlock().getType();
        
        BlockData blockData;
        if (groundMaterial.isSolid()) {
            blockData = groundMaterial.createBlockData();
        } else {
            blockData = Material.STONE.createBlockData();
        }
        
        player.getWorld().spawnParticle(ParticleCompat.blockCrack(), loc, 8, 0.5, 0.1, 0.5, 0.1, blockData);
        player.getWorld().spawnParticle(ParticleCompat.blockDust(), loc, 5, 0.3, 0.05, 0.3, 0.01, blockData);
    }

    private void applyNauseaWithCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastTime = lastNauseaTime.get(uuid);
        
        if (lastTime == null || (now - lastTime) >= NAUSEA_COOLDOWN_MS) {
            PotionEffect nausea = EffectUtil.createNauseaEffect(40, 0);
            if (nausea != null) {
                player.addPotionEffect(nausea);
                lastNauseaTime.put(uuid, now);
            }
        }
    }
    
    // ==================== NUEVOS MÉTODOS ====================
    
    /**
     * Sistema de fases: intensidad varía durante el desastre
     * Fase 1 (0-30%): 0.7x - inicio ligero
     * Fase 2 (30-70%): 1.3x - pico intenso
     * Fase 3 (70-100%): 0.9x - declive
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
        
        if (progress < 0.30) {
            // Fase 1: inicio ligero
            faseMultiplicador = 0.7;
        } else if (progress < 0.70) {
            // Fase 2: pico intenso
            faseMultiplicador = 1.3;
            
            // Advertencia en el centro
            if (elapsedSeconds == totalSeconds / 2) {
                messageBus.broadcast("§c§l⚠ ¡EL TERREMOTO ALCANZA SU MÁXIMA INTENSIDAD!", "terremoto_peak");
                soundUtil.playSoundAll(Sound.ENTITY_WARDEN_ROAR, 1.0f, 0.6f);
            }
        } else {
            // Fase 3: declive
            faseMultiplicador = 0.9;
        }
    }
    
    /**
     * Sistema de aftershocks (réplicas): cada intervalo aumenta intensidad temporalmente
     */
    private void updateAfterShocks() {
        if (!aftershocksEnabled) {
            isAfterShock = false;
            return;
        }
        
        if (tickCounter >= nextAfterShock) {
            isAfterShock = true;
            soundUtil.playSoundAll(Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.5f);
            messageBus.sendActionBarAll("§c§l⚠ RÉPLICA SÍSMICA ⚠", "aftershock");
            
            // Duración de la réplica: 60 ticks (3 segundos)
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                isAfterShock = false;
            }, 60L);
            
            nextAfterShock = tickCounter + aftershockIntervalo;
        }
    }
    
    /**
     * Grietas en el suelo: genera fisuras largas de lava/aire
     */
    private void spawnCracks() {
        double scale = getPerformanceScale();
        if (scale <= 0 || grietaBlocks.size() >= grietasMaxActivas * 8) return;
        
        // Buscar jugadores para centrar grietas
        List<Player> players = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        if (players.isEmpty()) return;
        
        Player target = players.get(random.nextInt(players.size()));
        if (isPlayerExempt(target)) return;
        
        Location start = target.getLocation().clone();
        World world = start.getWorld();
        
        // Dirección aleatoria para la grieta
        int dirX = random.nextBoolean() ? 1 : 0;
        int dirZ = dirX == 0 ? 1 : 0;
        if (random.nextBoolean()) {
            dirX *= -1;
            dirZ *= -1;
        }
        
        int length = random.nextInt(grietasLongitud) + 3;
        
        for (int i = 0; i < length; i++) {
            Location loc = start.clone().add(dirX * i, -1, dirZ * i);
            Block block = loc.getBlock();
            
            if (romperWhitelist.contains(block.getType())) {
                // 70% aire, 30% lava
                Material newType = random.nextDouble() < 0.7 ? Material.AIR : Material.LAVA;
                block.setType(newType, false);
                grietaBlocks.add(block);
                
                // Partículas de grieta
                world.spawnParticle(Particle.LAVA, loc.clone().add(0.5, 1.0, 0.5), 5, 0.3, 0.1, 0.3, 0.01);
                world.spawnParticle(Particle.SMOKE, loc.clone().add(0.5, 1.0, 0.5), 3, 0.2, 0.1, 0.2);
            }
        }
        
        // Sonido de grieta
        world.playSound(start, Sound.BLOCK_STONE_BREAK, 1.0f, 0.6f);
    }
    
    /**
     * Derrumbes: bloques de arriba caen como FallingBlock
     */
    private void spawnFallingDebris(Player player) {
        if (isPlayerExempt(player)) return;
        
        Location loc = player.getLocation();
        World world = loc.getWorld();
        
        // Buscar bloque sólido arriba (hasta derrumbesMaxAltura)
        Block sourceBlock = null;
        for (int y = 1; y <= derrumbesMaxAltura; y++) {
            Block check = world.getBlockAt(loc.getBlockX(), loc.getBlockY() + y, loc.getBlockZ());
            if (romperWhitelist.contains(check.getType())) {
                sourceBlock = check;
                break;
            }
        }
        
        if (sourceBlock == null) return;
        
        // Spawn FallingBlock con método no deprecado
        Location spawnLoc = sourceBlock.getLocation().clone().add(0.5, 0, 0.5);
        Material mat = sourceBlock.getType();
        BlockData blockData = mat.createBlockData();
        sourceBlock.setType(Material.AIR);
        
        world.spawn(spawnLoc, org.bukkit.entity.FallingBlock.class, (fb) -> {
            fb.setBlockData(blockData);
            fb.setDropItem(false);
        });
        
        world.playSound(spawnLoc, Sound.BLOCK_GRAVEL_BREAK, 0.8f, 0.7f);
        
        // Partículas de advertencia
        world.spawnParticle(Particle.BLOCK, spawnLoc, 15, 0.3, 0.3, 0.3, 0.1, blockData);
    }
    
    /**
     * Ondas sísmicas: anillos de partículas expandiéndose desde epicentros
     */
    private void spawnSeismicWave() {
        double scale = getPerformanceScale();
        if (scale <= 0) return;
        
        // Crear nuevo epicentro aleatorio
        List<Player> players = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        if (players.isEmpty()) return;
        
        Player target = players.get(random.nextInt(players.size()));
        Location epicentro = target.getLocation().clone();
        epicentros.add(epicentro);
        
        World world = epicentro.getWorld();
        
        // Animar onda expansiva (3 anillos concéntricos)
        for (int radius = 1; radius <= 8; radius++) {
            final int r = radius;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (int angle = 0; angle < 360; angle += 15) {
                    double rad = Math.toRadians(angle);
                    double x = epicentro.getX() + r * Math.cos(rad);
                    double z = epicentro.getZ() + r * Math.sin(rad);
                    Location particleLoc = new Location(world, x, epicentro.getY(), z);
                    
                    world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, particleLoc, 1, 0, 0, 0, 0.01);
                    world.spawnParticle(ParticleCompat.blockDust(), particleLoc, 2, 0.1, 0, 0.1, 0, Material.STONE.createBlockData());
                }
                
                if (r == 4) {
                    world.playSound(epicentro, Sound.ENTITY_GENERIC_EXPLODE, 0.3f, 0.6f);
                }
            }, (long) r * 2); // Delay progresivo
        }
    }
    
    /**
     * [NUEVO] Sistema de absorción de impacto
     * Detecta bloques absorbentes cerca del jugador y calcula reducción de efectos
     * 
     * @param location Ubicación del jugador
     * @return Estructura con información de protección
     */
    private AbsorptionInfo calculateAbsorption(Location location) {
        if (!absorcionEnabled) {
            return new AbsorptionInfo(0, 1.0, 1.0, 1.0);
        }
        
        World world = location.getWorld();
        int radio = absorcionRadio;
        int count = 0;
        
        // Buscar bloques absorbentes en área cúbica
        for (int x = -radio; x <= radio; x++) {
            for (int y = -radio; y <= radio; y++) {
                for (int z = -radio; z <= radio; z++) {
                    Block block = world.getBlockAt(
                        location.getBlockX() + x,
                        location.getBlockY() + y,
                        location.getBlockZ() + z
                    );
                    
                    if (absorcionMateriales.contains(block.getType())) {
                        count++;
                        if (count >= absorcionMaxBloques) {
                            break; // Límite alcanzado
                        }
                    }
                }
                if (count >= absorcionMaxBloques) break;
            }
            if (count >= absorcionMaxBloques) break;
        }
        
        if (count == 0) {
            return new AbsorptionInfo(0, 1.0, 1.0, 1.0);
        }
        
        // Calcular multiplicadores de reducción (menor = más protección)
        double shakeMultiplier = Math.max(0.1, 1.0 - (count * absorcionReduccionShake));
        double breakMultiplier = Math.max(0.0, 1.0 - (count * absorcionReduccionBreak));
        double damageMultiplier = Math.max(0.1, 1.0 - (count * absorcionReduccionDamage));
        
        return new AbsorptionInfo(count, shakeMultiplier, breakMultiplier, damageMultiplier);
    }
    
    /**
     * Spawn partículas de absorción cuando el jugador está protegido
     */
    private void spawnAbsorptionParticles(Location location, int blockCount) {
        if (blockCount == 0) return;
        
        World world = location.getWorld();
        
        // Partículas verdes de escudo
        world.spawnParticle(Particle.HAPPY_VILLAGER, location.clone().add(0, 1, 0), 
            blockCount * 2, 0.5, 0.5, 0.5, 0);
        
        // Partículas de lana/slime
        world.spawnParticle(Particle.END_ROD, location.clone().add(0, 0.5, 0), 
            blockCount, 0.3, 0.3, 0.3, 0.01);
    }
    
    /**
     * [NUEVO] Romper bloques de protección con probabilidad
     * Fuerza al jugador a reponer constantemente los bloques absorbentes
     */
    private void maybeBreakProtectionBlocks(Location location, int nearbyCount) {
        if (!romperProteccionEnabled) return;
        if (nearbyCount == 0) return;
        if (random.nextDouble() > romperProteccionProbabilidad) return; // 25% por defecto
        
        World world = location.getWorld();
        int radio = absorcionRadio;
        List<Block> protectionBlocks = new ArrayList<>();
        
        // Buscar bloques absorbentes en área
        for (int x = -radio; x <= radio; x++) {
            for (int y = -radio; y <= radio; y++) {
                for (int z = -radio; z <= radio; z++) {
                    Block block = world.getBlockAt(
                        location.getBlockX() + x,
                        location.getBlockY() + y,
                        location.getBlockZ() + z
                    );
                    
                    if (absorcionMateriales.contains(block.getType())) {
                        protectionBlocks.add(block);
                    }
                }
            }
        }
        
        if (protectionBlocks.isEmpty()) return;
        
        // Romper aleatoriamente hasta romperProteccionCantidad bloques
        Collections.shuffle(protectionBlocks);
        int broken = 0;
        int maxAllowed = Math.min(romperProteccionCantidad, protectionBlocks.size());
        
        for (Block block : protectionBlocks) {
            if (broken >= maxAllowed) break;
            
            Material originalType = block.getType();
            block.setType(Material.AIR);
            
            // Partículas de rotura (según tipo de bloque)
            Location breakLoc = block.getLocation().add(0.5, 0.5, 0.5);
            
            if (originalType.name().contains("WOOL")) {
                // Lana: partículas de nube blanca
                world.spawnParticle(Particle.CLOUD, breakLoc, 15, 0.3, 0.3, 0.3, 0.05);
                world.playSound(breakLoc, Sound.BLOCK_WOOL_BREAK, 1.0f, 0.8f);
            } else if (originalType == Material.SLIME_BLOCK) {
                // Slime: partículas verdes
                world.spawnParticle(Particle.ITEM_SLIME, breakLoc, 20, 0.4, 0.4, 0.4, 0.1);
                world.playSound(breakLoc, Sound.BLOCK_SLIME_BLOCK_BREAK, 1.0f, 0.8f);
            } else if (originalType == Material.BLUE_ICE) {
                // Hielo: partículas azules
                world.spawnParticle(Particle.BLOCK, breakLoc, 15, 0.3, 0.3, 0.3, 0.1, 
                    Material.BLUE_ICE.createBlockData());
                world.playSound(breakLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.2f);
            } else if (originalType == Material.HAY_BLOCK) {
                // Heno: partículas amarillas
                world.spawnParticle(Particle.BLOCK, breakLoc, 15, 0.3, 0.3, 0.3, 0.1, 
                    Material.HAY_BLOCK.createBlockData());
                world.playSound(breakLoc, Sound.BLOCK_GRASS_BREAK, 1.0f, 0.7f);
            }
            
            // Partículas adicionales de impacto sísmico
            world.spawnParticle(ParticleCompat.blockCrack(), breakLoc, 10, 0.3, 0.3, 0.3, 0.05, 
                Material.STONE.createBlockData());
            
            broken++;
        }
        
        // Log de debug
        if (broken > 0 && plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[Terremoto] Rotos " + broken + " bloques de protección por vibración");
        }
    }
    
    /**
     * Clase auxiliar para almacenar información de absorción
     */
    private static class AbsorptionInfo {
        final int blockCount;
        final double shakeMultiplier;
        final double breakMultiplier;
        final double damageMultiplier;
        
        AbsorptionInfo(int blockCount, double shake, double breakM, double damage) {
            this.blockCount = blockCount;
            this.shakeMultiplier = shake;
            this.breakMultiplier = breakM;
            this.damageMultiplier = damage;
        }
    }
    
    /**
     * Desorientación: efectos de rotación, slowness y mining fatigue
     */
    private void applyDisorientation(Player player) {
        if (isPlayerExempt(player)) return;
        
        // Rotar jugador aleatoriamente
        Location loc = player.getLocation();
        float newYaw = (float) (random.nextDouble() * 360);
        loc.setYaw(newYaw);
        player.teleport(loc);
        
        // Efectos de desorientación
        player.addPotionEffect(new PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 
            desorientacionDuracion, 1, false, false));
        player.addPotionEffect(new PotionEffect(org.bukkit.potion.PotionEffectType.MINING_FATIGUE, 
            desorientacionDuracion, 1, false, false));
        
        // Sonido de desorientación
        player.playSound(player.getLocation(), Sound.ENTITY_GHAST_AMBIENT, 0.5f, 1.5f);
    }
    
    /**
     * Columnas de polvo: partículas verticales cuando se rompen bloques
     */
    private void spawnDustColumns(Player player) {
        if (isPlayerExempt(player)) return;
        
        Location loc = player.getLocation();
        World world = loc.getWorld();
        
        // 3 columnas aleatorias cerca del jugador
        for (int i = 0; i < 3; i++) {
            int dx = random.nextInt(5) - 2;
            int dz = random.nextInt(5) - 2;
            Location dustLoc = loc.clone().add(dx, 0, dz);
            
            // Columna de 5 bloques de alto
            for (int y = 0; y < 5; y++) {
                Location particleLoc = dustLoc.clone().add(0, y * 0.5, 0);
                world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, particleLoc, 5, 0.2, 0.1, 0.2, 0.01);
                world.spawnParticle(ParticleCompat.blockDust(), particleLoc, 3, 0.1, 0.1, 0.1, 0, Material.DIRT.createBlockData());
            }
        }
    }
}
