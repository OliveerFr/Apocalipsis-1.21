package me.apocalipsis.disaster;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.disaster.adapters.PerformanceAdapter;
import me.apocalipsis.state.TimeService;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🌋 ERUPCIÓN VOLCÁNICA - Desastre de Ciclo 2
 * 
 * La tierra expulsa lava y rocas incandescentes.
 * Reemplazo del Terremoto con mecánicas completamente nuevas.
 */
public class ErupcionVolcanica extends DisasterBase {

    // Tracking
    private final Set<Location> geisersActivos = ConcurrentHashMap.newKeySet();
    private final Set<Location> grietasActivas = ConcurrentHashMap.newKeySet();
    private final Set<FallingBlock> rocasActivas = ConcurrentHashMap.newKeySet();
    private final Map<Location, Long> geiserAdvertencias = new ConcurrentHashMap<>();
    private final Random random = new Random();
    
    // Tracking de bloques modificados para limpieza
    private final Map<Location, Material> bloquesCambiados = new ConcurrentHashMap<>();
    private final Set<Location> fuegoColocado = ConcurrentHashMap.newKeySet();
    private final Set<Location> lavaColocada = ConcurrentHashMap.newKeySet();
    
    // Config
    private int geiserAlturaMin;
    private int geiserAlturaMax;
    private int geiserAdvertenciaTicks;
    private int geiserDuracionTicks;
    private double geiserDamage;
    
    private boolean rocasEnabled;
    private int rocasMin;
    private int rocasMax;
    private int rocasIntervalo;
    private double rocasExplosionPower;
    private int rocasRadioFuego;
    
    private boolean grietasEnabled;
    private int grietasLongitudMin;
    private int grietasLongitudMax;
    private int grietasProfundidadMin;
    private int grietasProfundidadMax;
    private int grietasMaxActivas;
    
    private boolean cenizaEnabled;
    private int cenizaIntervalo;
    private int cenizaRadio;
    
    private boolean bombasEnabled;
    private boolean bombasSoloPico;
    private int bombasIntervalo;
    private double bombasExplosionPower;
    
    private boolean temblorEnabled;
    private int temblorDuracion;
    private double temblorIntensidad;
    
    // Protección
    private boolean resistentesEnabled;
    private boolean obsidianaInmune;
    private double piedraReduccion;
    
    private boolean waterBucketsEnabled;
    private double geiserRompeAguaChance;
    
    private boolean alturaEnabled;
    private int alturaMinima;
    private double alturaReduccionRocas;
    
    private boolean hieloEnabled;
    private int hieloRadio;
    private double hieloReduccion;
    
    // Tasks
    private BukkitRunnable geisersTask;
    private BukkitRunnable rocasTask;
    private BukkitRunnable grietasTask;
    private BukkitRunnable cenizaTask;
    private BukkitRunnable bombasTask;
    
    // Fases
    private boolean fasesEnabled;
    private double faseMultiplicador = 1.0;

    public ErupcionVolcanica(Apocalipsis plugin, MessageBus messageBus, SoundUtil soundUtil,
                             TimeService timeService, PerformanceAdapter performanceAdapter) {
        super(plugin, messageBus, soundUtil, timeService, performanceAdapter, "erupcion_volcanica");
        loadConfiguration();
    }

    private void loadConfiguration() {
        ConfigurationSection cfg = plugin.getConfigManager().getDesastresConfig()
            .getConfigurationSection("desastres.erupcion_volcanica");
        
        if (cfg == null) {
            plugin.getLogger().warning("[ErupcionVolcanica] Configuración no encontrada, usando valores default");
            setDefaultConfig();
            return;
        }
        
        // Duración
        int duracionSegundos = cfg.getInt("duracion_segundos", 80);
        setMaxTicks(duracionSegundos);
        
        // Géiseres
        ConfigurationSection gei = cfg.getConfigurationSection("geiseres_lava");
        if (gei != null) {
            geiserAlturaMin = gei.getInt("altura_min", 5);
            geiserAlturaMax = gei.getInt("altura_max", 10);
            geiserAdvertenciaTicks = gei.getInt("advertencia_ticks", 60);
            geiserDuracionTicks = gei.getInt("duracion_ticks", 160);
            geiserDamage = gei.getDouble("damage", 2.0);
        } else {
            geiserAlturaMin = 5;
            geiserAlturaMax = 10;
            geiserAdvertenciaTicks = 60;
            geiserDuracionTicks = 160;
            geiserDamage = 2.0;
        }
        
        // Rocas
        ConfigurationSection roc = cfg.getConfigurationSection("rocas_volcanicas");
        if (roc != null) {
            rocasEnabled = roc.getBoolean("enabled", true);
            rocasMin = roc.getInt("min_rocas", 8);
            rocasMax = roc.getInt("max_rocas", 12);
            rocasIntervalo = roc.getInt("intervalo_ticks", 240);
            rocasExplosionPower = roc.getDouble("explosion_power", 1.5);
            rocasRadioFuego = roc.getInt("radio_fuego", 2);
        } else {
            rocasEnabled = true;
            rocasMin = 8;
            rocasMax = 12;
            rocasIntervalo = 240;
            rocasExplosionPower = 1.5;
            rocasRadioFuego = 2;
        }
        
        // Grietas
        ConfigurationSection gri = cfg.getConfigurationSection("grietas_magmaticas");
        if (gri != null) {
            grietasEnabled = gri.getBoolean("enabled", true);
            grietasLongitudMin = gri.getInt("longitud_min", 4);
            grietasLongitudMax = gri.getInt("longitud_max", 8);
            grietasProfundidadMin = gri.getInt("profundidad_min", 2);
            grietasProfundidadMax = gri.getInt("profundidad_max", 3);
            grietasMaxActivas = gri.getInt("max_activas", 6);
        } else {
            grietasEnabled = true;
            grietasLongitudMin = 4;
            grietasLongitudMax = 8;
            grietasProfundidadMin = 2;
            grietasProfundidadMax = 3;
            grietasMaxActivas = 6;
        }
        
        // Ceniza
        ConfigurationSection cen = cfg.getConfigurationSection("ceniza_volcanica");
        if (cen != null) {
            cenizaEnabled = cen.getBoolean("enabled", true);
            cenizaIntervalo = cen.getInt("intervalo_ticks", 600);
            cenizaRadio = cen.getInt("radio", 15);
        } else {
            cenizaEnabled = true;
            cenizaIntervalo = 600;
            cenizaRadio = 15;
        }
        
        // Bombas
        ConfigurationSection bom = cfg.getConfigurationSection("bombas_magma");
        if (bom != null) {
            bombasEnabled = bom.getBoolean("enabled", true);
            bombasSoloPico = bom.getBoolean("solo_en_pico", true);
            bombasIntervalo = bom.getInt("intervalo_ticks", 350);
            bombasExplosionPower = bom.getDouble("explosion_power", 2.5);
        } else {
            bombasEnabled = true;
            bombasSoloPico = true;
            bombasIntervalo = 350;
            bombasExplosionPower = 2.5;
        }
        
        // Temblor
        ConfigurationSection tem = cfg.getConfigurationSection("temblores_previos");
        if (tem != null) {
            temblorEnabled = tem.getBoolean("enabled", true);
            temblorDuracion = tem.getInt("duracion_ticks", 100);
            temblorIntensidad = tem.getDouble("intensidad_vibration", 0.15);
        } else {
            temblorEnabled = true;
            temblorDuracion = 100;
            temblorIntensidad = 0.15;
        }
        
        // Protección
        ConfigurationSection prot = cfg.getConfigurationSection("proteccion");
        if (prot != null) {
            ConfigurationSection res = prot.getConfigurationSection("bloques_resistentes");
            if (res != null) {
                resistentesEnabled = res.getBoolean("enabled", true);
                obsidianaInmune = res.getBoolean("obsidiana_inmune", true);
                piedraReduccion = res.getDouble("piedra_reduccion", 0.60);
            } else {
                resistentesEnabled = true;
                obsidianaInmune = true;
                piedraReduccion = 0.60;
            }
            
            ConfigurationSection wat = prot.getConfigurationSection("water_buckets");
            if (wat != null) {
                waterBucketsEnabled = wat.getBoolean("solidificar_lava", true);
                geiserRompeAguaChance = wat.getDouble("geiser_rompe_agua_chance", 0.20);
            } else {
                waterBucketsEnabled = true;
                geiserRompeAguaChance = 0.20;
            }
            
            ConfigurationSection alt = prot.getConfigurationSection("altura_elevada");
            if (alt != null) {
                alturaEnabled = alt.getBoolean("inmunidad_grietas", true);
                alturaMinima = alt.getInt("altura_minima", 10);
                alturaReduccionRocas = alt.getDouble("reduccion_damage_rocas", 0.40);
            } else {
                alturaEnabled = true;
                alturaMinima = 10;
                alturaReduccionRocas = 0.40;
            }
            
            ConfigurationSection hie = prot.getConfigurationSection("bloques_hielo");
            if (hie != null) {
                hieloEnabled = hie.getBoolean("enabled", true);
                hieloRadio = hie.getInt("radio_efecto", 3);
                hieloReduccion = hie.getDouble("reduccion_geiser", 0.50);
            } else {
                hieloEnabled = true;
                hieloRadio = 3;
                hieloReduccion = 0.50;
            }
        }
        
        // Fases
        ConfigurationSection fases = cfg.getConfigurationSection("fases");
        if (fases != null) {
            fasesEnabled = fases.getBoolean("enabled", true);
        } else {
            fasesEnabled = true;
        }
    }
    
    private void setDefaultConfig() {
        setMaxTicks(80);
        geiserAlturaMin = 5;
        geiserAlturaMax = 10;
        geiserAdvertenciaTicks = 60;
        geiserDuracionTicks = 160;
        geiserDamage = 2.0;
        
        rocasEnabled = true;
        rocasMin = 8;
        rocasMax = 12;
        rocasIntervalo = 240;
        rocasExplosionPower = 1.5;
        rocasRadioFuego = 2;
        
        grietasEnabled = true;
        grietasLongitudMin = 4;
        grietasLongitudMax = 8;
        grietasProfundidadMin = 2;
        grietasProfundidadMax = 3;
        grietasMaxActivas = 6;
        
        cenizaEnabled = true;
        cenizaIntervalo = 600;
        cenizaRadio = 15;
        
        bombasEnabled = true;
        bombasSoloPico = true;
        bombasIntervalo = 350;
        bombasExplosionPower = 2.5;
        
        temblorEnabled = true;
        temblorDuracion = 100;
        temblorIntensidad = 0.15;
        
        resistentesEnabled = true;
        obsidianaInmune = true;
        piedraReduccion = 0.60;
        
        waterBucketsEnabled = true;
        geiserRompeAguaChance = 0.20;
        
        alturaEnabled = true;
        alturaMinima = 10;
        alturaReduccionRocas = 0.40;
        
        hieloEnabled = true;
        hieloRadio = 3;
        hieloReduccion = 0.50;
        
        fasesEnabled = true;
    }

    @Override
    protected void onStart() {
        geisersActivos.clear();
        grietasActivas.clear();
        rocasActivas.clear();
        geiserAdvertencias.clear();
        bloquesCambiados.clear();
        fuegoColocado.clear();
        lavaColocada.clear();
        faseMultiplicador = 1.0;
        
        // Anuncios
        messageBus.broadcast("§8§m                                                    ", "disaster");
        messageBus.broadcast("§c§l      🌋 ERUPCIÓN VOLCÁNICA 🌋", "disaster");
        messageBus.broadcast("", "disaster");
        messageBus.broadcast("  §f§l¡Géiseres de lava emergen del suelo!", "disaster");
        messageBus.broadcast("  §7Rocas incandescentes llueven del cielo", "disaster");
        messageBus.broadcast("", "disaster");
        messageBus.broadcast("  §e⚠ Usa agua para solidificar lava", "disaster");
        messageBus.broadcast("  §7Bloques de piedra/obsidiana resisten", "disaster");
        messageBus.broadcast("§8§m                                                    ", "disaster");
        
        // Iniciar tasks
        startGeiseres();
        if (rocasEnabled) startRocas();
        if (grietasEnabled) startGrietas();
        if (cenizaEnabled) startCeniza();
        if (bombasEnabled) startBombas();
        
        plugin.getLogger().info("[ErupcionVolcanica] Desastre iniciado");
    }

    @Override
    protected void onTick() {
        // Actualizar multiplicador de fase
        if (fasesEnabled) {
            actualizarFase();
        }
        
        // Limpiar entities caídas
        rocasActivas.removeIf(fb -> fb.isDead() || !fb.isValid());
        
        // Limpiar advertencias antiguas
        long now = System.currentTimeMillis();
        geiserAdvertencias.entrySet().removeIf(e -> (now - e.getValue()) > 5000);
    }

    @Override
    protected void onStop() {
        // Cancelar tasks
        if (geisersTask != null) {
            geisersTask.cancel();
            geisersTask = null;
        }
        if (rocasTask != null) {
            rocasTask.cancel();
            rocasTask = null;
        }
        if (grietasTask != null) {
            grietasTask.cancel();
            grietasTask = null;
        }
        if (cenizaTask != null) {
            cenizaTask.cancel();
            cenizaTask = null;
        }
        if (bombasTask != null) {
            bombasTask.cancel();
            bombasTask = null;
        }
        
        // Limpiar entities
        for (FallingBlock fb : rocasActivas) {
            if (fb.isValid()) {
                fb.remove();
            }
        }
        
        // [LIMPIEZA] Restaurar bloques destruidos por grietas
        int bloquesProcesados = 0;
        for (Map.Entry<Location, Material> entry : bloquesCambiados.entrySet()) {
            Location loc = entry.getKey();
            Material originalMaterial = entry.getValue();
            
            if (loc.getWorld() != null) {
                setBlockTracked(loc.getBlock(), originalMaterial);
                bloquesProcesados++;
            }
        }
        
        // [LIMPIEZA] Eliminar fuego colocado
        int fuegoEliminado = 0;
        for (Location loc : fuegoColocado) {
            if (loc.getWorld() != null && 
                (loc.getBlock().getType() == Material.FIRE || loc.getBlock().getType() == Material.SOUL_FIRE)) {
                setBlockTracked(loc.getBlock(), Material.AIR);
                fuegoEliminado++;
            }
        }
        
        // [LIMPIEZA] Eliminar lava colocada
        int lavaEliminada = 0;
        for (Location loc : lavaColocada) {
            if (loc.getWorld() != null && loc.getBlock().getType() == Material.LAVA) {
                setBlockTracked(loc.getBlock(), Material.AIR);
                lavaEliminada++;
            }
        }
        
        if (bloquesProcesados > 0 || fuegoEliminado > 0 || lavaEliminada > 0) {
            plugin.getLogger().info(String.format("[ErupcionVolcanica] Limpieza: %d bloques restaurados, %d fuego eliminado, %d lava eliminada",
                bloquesProcesados, fuegoEliminado, lavaEliminada));
        }
        
        // Limpiar data
        geisersActivos.clear();
        grietasActivas.clear();
        rocasActivas.clear();
        geiserAdvertencias.clear();
        bloquesCambiados.clear();
        fuegoColocado.clear();
        lavaColocada.clear();
        
        messageBus.broadcast("§c§l🌋 §fLa erupción volcánica se calma...", "disaster");
        
        plugin.getLogger().info("[ErupcionVolcanica] Desastre detenido");
    }
    
    @Override
    public void applyEffects(Player player) {
        // Efectos aplicados por tasks específicas
    }
    
    @Override
    protected String getDisasterName() {
        return "ERUPCIÓN VOLCÁNICA";
    }
    
    @Override
    protected String[] getPhaseNames() {
        return new String[] {
            "§7Temblores Iniciales",
            "§eActividad Moderada",
            "§6Erupción Intensa",
            "§c§lESTALLIDO VOLCÁNICO",
            "§4§l¡CATACLISMO MAGMÁTICO!"
        };
    }
    
    // ============================================
    // MECÁNICAS ESPECÍFICAS
    // ============================================
    
    private void actualizarFase() {
        double progreso = (double) tickCounter / maxTicks;
        
        if (progreso < 0.30) {
            // INICIO
            faseMultiplicador = 0.7;
        } else if (progreso < 0.70) {
            // PICO
            faseMultiplicador = 1.5;
        } else {
            // DECLIVE
            faseMultiplicador = 0.9;
        }
    }
    
    private String getCurrentPhaseString() {
        double progreso = (double) tickCounter / maxTicks;
        
        if (progreso < 0.30) return "INICIO";
        else if (progreso < 0.70) return "PICO";
        else return "DECLIVE";
    }
    
    private void startGeiseres() {
        geisersTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) {
                    cancel();
                    return;
                }
                
                String phase = getCurrentPhaseString();
                int cantidad = getCantidadGeiseresPorFase(phase);
                
                for (int i = 0; i < cantidad; i++) {
                    crearGeiser();
                }
            }
        };
        
        long intervalo = getIntervaloGeiseresPorFase();
        geisersTask.runTaskTimer(plugin, 60L, intervalo);
    }
    
    private int getCantidadGeiseresPorFase(String phase) {
        switch (phase) {
            case "INICIO": return 1;
            case "PICO": return 5;
            case "DECLIVE": return 2;
            default: return 1;
        }
    }
    
    private long getIntervaloGeiseresPorFase() {
        String phase = getCurrentPhaseString();
        switch (phase) {
            case "INICIO": return 400L; // 20s
            case "PICO": return 200L;   // 10s
            case "DECLIVE": return 360L; // 18s
            default: return 300L;
        }
    }
    
    private void crearGeiser() {
        List<Player> jugadores = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isPlayerExempt(p)) {
                jugadores.add(p);
            }
        }
        
        if (jugadores.isEmpty()) return;
        
        Player target = jugadores.get(random.nextInt(jugadores.size()));
        
        Location spawn = target.getLocation().add(
            random.nextDouble() * 12 - 6,
            0,
            random.nextDouble() * 12 - 6
        );
        
        // Buscar suelo
        spawn.setY(spawn.getWorld().getHighestBlockYAt(spawn) + 1);
        
        // Verificar hielo cercano
        if (hieloEnabled && tieneHieloCerca(spawn)) {
            if (random.nextDouble() < hieloReduccion) {
                // Notificar a jugadores cercanos
                for (Player p : spawn.getWorld().getPlayers()) {
                    if (p.getLocation().distance(spawn) < 15) {
                        p.sendActionBar("§b❄ ¡Hielo enfrió el géiser!");
                        p.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.5f, 2.0f);
                        p.spawnParticle(Particle.SNOWFLAKE, spawn, 30, 2, 2, 2, 0.1);
                    }
                }
                return; // Cancelar géiser
            }
        }
        
        // Temblor previo
        if (temblorEnabled) {
            aplicarTemblor(spawn);
        }
        
        // Advertencia
        geiserAdvertencias.put(spawn.clone(), System.currentTimeMillis());
        advertirGeiser(spawn);
        
        // Lanzar géiser después de advertencia
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) {
                    cancel();
                    return;
                }
                lanzarGeiser(spawn);
            }
        }.runTaskLater(plugin, geiserAdvertenciaTicks);
    }
    
    private boolean tieneHieloCerca(Location loc) {
        for (int x = -hieloRadio; x <= hieloRadio; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -hieloRadio; z <= hieloRadio; z++) {
                    Block b = loc.getWorld().getBlockAt(
                        loc.getBlockX() + x,
                        loc.getBlockY() + y,
                        loc.getBlockZ() + z
                    );
                    if (b.getType() == Material.BLUE_ICE || b.getType() == Material.PACKED_ICE) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private boolean estaSobreBloqueResistente(Player p) {
        Block below = p.getLocation().subtract(0, 1, 0).getBlock();
        Material tipo = below.getType();
        
        if (obsidianaInmune && (tipo == Material.OBSIDIAN || tipo == Material.CRYING_OBSIDIAN)) {
            return true;
        }
        
        return tipo == Material.STONE || tipo == Material.DEEPSLATE || 
               tipo == Material.COBBLESTONE || tipo == Material.STONE_BRICKS ||
               tipo == Material.ANDESITE || tipo == Material.DIORITE || tipo == Material.GRANITE;
    }
    
    private void aplicarTemblor(Location centro) {
        List<Player> jugadores = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isPlayerExempt(p)) {
                jugadores.add(p);
            }
        }
        
        for (Player p : jugadores) {
            if (p.getLocation().distance(centro) < 15) {
                Vector shake = new Vector(
                    (random.nextDouble() - 0.5) * temblorIntensidad,
                    random.nextDouble() * temblorIntensidad * 0.5,
                    (random.nextDouble() - 0.5) * temblorIntensidad
                );
                p.setVelocity(p.getVelocity().add(shake));
                p.spawnParticle(Particle.BLOCK, p.getLocation(), 10, 1, 0.1, 1, Material.STONE.createBlockData());
            }
        }
    }
    
    private void advertirGeiser(Location loc) {
        // CINEMÁTICO: Sonidos épicos de advertencia
        loc.getWorld().playSound(loc, Sound.ENTITY_WARDEN_DIG, 1.2f, 0.6f);
        loc.getWorld().playSound(loc, Sound.BLOCK_LAVA_POP, 1.0f, 0.5f);
        
        // CINEMÁTICO: Advertencia visual mejorada con erupción progresiva
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= geiserAdvertenciaTicks / 10 || !isActive()) {
                    cancel();
                    return;
                }
                
                // Columna de advertencia de 15 bloques con partículas de lava
                for (int y = 0; y < 15; y++) {
                    Location particleLoc = loc.clone().add(0, y * 0.3, 0);
                    
                    // Partículas de lava y fuego intensas
                    loc.getWorld().spawnParticle(Particle.LAVA, particleLoc, 10, 0.6, 0.3, 0.6, 0);
                    loc.getWorld().spawnParticle(Particle.FLAME, particleLoc, 8, 0.5, 0.2, 0.5, 0.05);
                    loc.getWorld().spawnParticle(Particle.SMOKE, particleLoc, 6, 0.5, 0.3, 0.5, 0.03);
                    
                    // Partículas naranjas brillantes cada 2 bloques
                    if (y % 2 == 0) {
                        loc.getWorld().spawnParticle(Particle.DUST, particleLoc, 5, 0.4, 0.2, 0.4,
                            new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 80, 0), 2.5f));
                    }
                }
                
                // CINEMÁTICO: Grietas visuales en el suelo
                Block below = loc.clone().subtract(0, 1, 0).getBlock();
                if (below.getType().isSolid()) {
                    loc.getWorld().spawnParticle(Particle.BLOCK, loc, 15, 0.5, 0.2, 0.5, below.getBlockData());
                }
                
                // CINEMÁTICO: Anillos expansivos de fuego
                for (int angle = 0; angle < 360; angle += 30) {
                    double rad = Math.toRadians(angle);
                    double radius = 1.5 + (ticks * 0.15);
                    double x = loc.getX() + radius * Math.cos(rad);
                    double z = loc.getZ() + radius * Math.sin(rad);
                    Location ringLoc = new Location(loc.getWorld(), x, loc.getY(), z);
                    loc.getWorld().spawnParticle(Particle.FLAME, ringLoc, 2, 0.1, 0, 0.1, 0);
                    loc.getWorld().spawnParticle(Particle.LAVA, ringLoc, 1, 0, 0, 0, 0);
                }
                
                // Sonido de burbujeo creciente
                if (ticks % 2 == 0) {
                    loc.getWorld().playSound(loc, Sound.BLOCK_LAVA_POP, 0.8f, 0.8f + (ticks * 0.1f));
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 10L);
        
        // CINEMÁTICO: Advertir a jugadores cercanos con títulos
        for (Player player : loc.getWorld().getPlayers()) {
            if (isPlayerExempt(player)) continue;
            if (player.getLocation().distance(loc) < 20) {
                player.sendTitle("§c§l🌋 GÉISER", "§6§l3 segundos...", 10, 40, 10);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                
                // Countdown 2, 1
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendTitle("§c§l2", "", 0, 15, 5);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.6f);
                }, 20L);
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendTitle("§c§l1", "", 0, 15, 5);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.7f);
                }, 40L);
            }
        }
    }
    
    private void lanzarGeiser(Location loc) {
        geisersActivos.add(loc.clone());
        
        int altura = random.nextInt(geiserAlturaMax - geiserAlturaMin + 1) + geiserAlturaMin;
        altura = (int) (altura * faseMultiplicador);
        
        // CINEMÁTICO: Efectos épicos de erupción inicial
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 5, 1, 1, 1, 0);
        loc.getWorld().spawnParticle(Particle.LAVA, loc, 60, 2, 2, 2, 0.2);
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 80, 1.5, 1.5, 1.5, 0.15);
        loc.getWorld().spawnParticle(Particle.FLASH, loc, 3, 0.5, 0.5, 0.5, 0);
        
        // CINEMÁTICO: Sonidos épicos combinados
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.7f);
        loc.getWorld().playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.2f, 0.6f);
        loc.getWorld().playSound(loc, Sound.ENTITY_WARDEN_ROAR, 0.8f, 0.8f);
        
        // CINEMÁTICO: Ondas de choque expansivas de fuego
        for (int radius = 1; radius <= 10; radius++) {
            final int r = radius;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (int angle = 0; angle < 360; angle += 20) {
                    double rad = Math.toRadians(angle);
                    double x = loc.getX() + r * Math.cos(rad);
                    double z = loc.getZ() + r * Math.sin(rad);
                    Location shockwaveLoc = new Location(loc.getWorld(), x, loc.getY(), z);
                    
                    loc.getWorld().spawnParticle(Particle.FLAME, shockwaveLoc, 6, 0.3, 0.5, 0.3, 0.05);
                    loc.getWorld().spawnParticle(Particle.LAVA, shockwaveLoc, 3, 0.2, 0.2, 0.2, 0);
                    loc.getWorld().spawnParticle(Particle.BLOCK, shockwaveLoc, 5, 0.3, 0.2, 0.3, Material.MAGMA_BLOCK.createBlockData());
                }
            }, (long) r * 2);
        }
        
        // CINEMÁTICO: Columna de lava mejorada con múltiples efectos
        for (int y = 0; y < altura; y++) {
            Location particleLoc = loc.clone().add(0, y, 0);
            
            // Partículas densas de lava y fuego
            loc.getWorld().spawnParticle(Particle.LAVA, particleLoc, 25, 0.6, 0.6, 0.6, 0.15);
            loc.getWorld().spawnParticle(Particle.FLAME, particleLoc, 20, 0.4, 0.4, 0.4, 0.08);
            loc.getWorld().spawnParticle(Particle.SMOKE, particleLoc, 10, 0.5, 0.5, 0.5, 0.05);
            
            // Partículas naranjas brillantes cada 3 bloques
            if (y % 3 == 0) {
                loc.getWorld().spawnParticle(Particle.DUST, particleLoc, 8, 0.5, 0.5, 0.5,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 60, 0), 2.0f));
            }
        }
        
        // Damage a jugadores cercanos
        final int finalAltura = altura;
        new BukkitRunnable() {
            int duration = 0;
            
            @Override
            public void run() {
                if (duration++ >= geiserDuracionTicks / 20 || !isActive()) {
                    geisersActivos.remove(loc);
                    cancel();
                    return;
                }
                
                // Partículas continuas con espiral
                for (int y = 0; y < finalAltura; y++) {
                    if (random.nextDouble() < 0.4) {
                        // Efecto espiral
                        double angle = Math.toRadians(duration * 40 + y * 15);
                        double radius = 0.5 + (y * 0.05);
                        double offsetX = Math.cos(angle) * radius;
                        double offsetZ = Math.sin(angle) * radius;
                        
                        Location p = loc.clone().add(offsetX, y, offsetZ);
                        p.getWorld().spawnParticle(Particle.LAVA, p, 5);
                        p.getWorld().spawnParticle(Particle.FLAME, p, 3);
                    }
                }
                
                // Daño y efectos para jugadores
                List<Player> jugadores = new ArrayList<>();
                for (Player pl : Bukkit.getOnlinePlayers()) {
                    if (!isPlayerExempt(pl)) {
                        jugadores.add(pl);
                    }
                }
                
                for (Player p : jugadores) {
                    double distance = p.getLocation().distance(loc);
                    
                    if (distance < 2.5) {
                        double damage = geiserDamage * faseMultiplicador;
                        boolean tieneProteccion = false;
                        String proteccionMsg = "";
                        
                        // Protección por altura
                        if (alturaEnabled) {
                            int alturaJugador = p.getLocation().getBlockY();
                            int alturaSuperficie = p.getWorld().getHighestBlockYAt(p.getLocation());
                            int alturaRelativa = alturaJugador - alturaSuperficie;
                            
                            if (alturaRelativa > alturaMinima) {
                                damage *= alturaReduccionRocas;
                                tieneProteccion = true;
                                proteccionMsg = String.format("§b⛰ Altura +%d §a-%.0f%%", 
                                    alturaRelativa, alturaReduccionRocas * 100);
                                p.spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation(), 5, 0.5, 0.5, 0.5, 0);
                            }
                        }
                        
                        // Protección por bloques resistentes bajo los pies
                        if (resistentesEnabled && estaSobreBloqueResistente(p)) {
                            damage *= piedraReduccion;
                            if (!tieneProteccion) {
                                tieneProteccion = true;
                                proteccionMsg = String.format("§7🛡 Piedra §a-%.0f%%", 
                                    (1.0 - piedraReduccion) * 100);
                            }
                        }
                        
                        p.damage(damage);
                        p.setFireTicks(40);
                        
                        if (tieneProteccion) {
                            p.sendActionBar("§a🌋 Géiser §8| " + proteccionMsg);
                            p.playSound(p.getLocation(), Sound.BLOCK_STONE_BREAK, 0.5f, 0.8f);
                        } else {
                            // SIN PROTECCIÓN - Diagnóstico completo
                            String diagnostico = diagnosticarProteccionVolcanica(p);
                            p.sendActionBar("§c§l⚠ SIN PROTECCIÓN §8| §7" + diagnostico);
                            
                            // Consejo cada 15 segundos
                            if (tickCounter % 300 == 0) {
                                p.sendMessage("§c🌋 §7ERUPCIÓN VOLCÁNICA: Necesitas protección contra géiseres");
                                p.sendMessage("§7  §8→ §7Sube a §eY>90§7 para reducir daño por altura");
                                p.sendMessage("§7  §8→ §7Coloca §bHielo Compactado§7/§bHielo Azul§7 en radio §e" + hieloRadio + " bloques§7 para cancelar géiseres");
                                p.sendMessage("§7  §8→ §7Párate sobre §7Piedra/Obsidiana§7 para §a-30% §7daño");
                            }
                        }
                    }
                    
                    // CINEMÁTICO: Título de impacto para jugadores muy cercanos
                    if (distance < 5 && duration == 1) {
                        p.sendTitle("§c§l🌋 ERUPCIÓN", "", 5, 20, 10);
                        p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.8f);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    /**
     * Diagnostica por QUÉ el jugador no tiene protección volcánica
     */
    private String diagnosticarProteccionVolcanica(Player p) {
        double alturaActual = p.getLocation().getY();
        Block bloqueDebajo = p.getLocation().subtract(0, 1, 0).getBlock();
        boolean tieneHieloCerca = tieneHieloCerca(p.getLocation());
        
        java.util.List<String> problemas = new java.util.ArrayList<>();
        
        // Verificar altura
        if (alturaActual < 90) {
            problemas.add("Altura §c" + String.format("%.0f", alturaActual) + "§7 (sube a §e90+§7)");
        }
        
        // Verificar bloques bajo los pies
        Material tipoBajo = bloqueDebajo.getType();
        boolean esPiedra = tipoBajo == Material.STONE || tipoBajo == Material.DEEPSLATE || 
                          tipoBajo == Material.COBBLESTONE || tipoBajo == Material.STONE_BRICKS;
        boolean esObsidiana = tipoBajo == Material.OBSIDIAN || tipoBajo == Material.CRYING_OBSIDIAN;
        
        if (!esPiedra && !esObsidiana) {
            String nombreBloque = tipoBajo.name().toLowerCase().replace("_", " ");
            problemas.add("Estás sobre §c" + nombreBloque + "§7 (usa piedra/obsidiana)");
        }
        
        // Verificar hielo cercano
        if (!tieneHieloCerca) {
            problemas.add("Sin §bhielo compactado§7 cerca (radio §e" + hieloRadio + "§7)");
        }
        
        if (problemas.isEmpty()) {
            return "Géiser activo - busca protección";
        } else if (problemas.size() == 1) {
            return problemas.get(0);
        } else {
            // Mostrar el problema más crítico
            return problemas.get(0) + " §8+ §e" + (problemas.size() - 1) + " más";
        }
    }
    
    private void startRocas() {
        rocasTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) {
                    cancel();
                    return;
                }
                
                lanzarRocas();
            }
        };
        rocasTask.runTaskTimer(plugin, rocasIntervalo, rocasIntervalo);
    }
    
    private void lanzarRocas() {
        int cantidad = random.nextInt(rocasMax - rocasMin + 1) + rocasMin;
        
        List<Player> jugadores = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isPlayerExempt(p)) {
                jugadores.add(p);
            }
        }
        
        if (jugadores.isEmpty()) return;
        
        for (int i = 0; i < cantidad; i++) {
            Player target = jugadores.get(random.nextInt(jugadores.size()));
            
            Location spawn = target.getLocation().add(
                random.nextDouble() * 16 - 8,
                20 + random.nextDouble() * 10,
                random.nextDouble() * 16 - 8
            );
            
            FallingBlock fb = spawn.getWorld().spawnFallingBlock(spawn, Material.MAGMA_BLOCK.createBlockData());
            fb.setDropItem(false);
            fb.setHurtEntities(true);
            fb.setVelocity(new Vector(
                (target.getLocation().getX() - spawn.getX()) * 0.1,
                -0.5,
                (target.getLocation().getZ() - spawn.getZ()) * 0.1
            ));
            
            rocasActivas.add(fb);
            
            // Monitorear impacto
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!fb.isValid() || fb.isDead() || !isActive()) {
                        cancel();
                        return;
                    }
                    
                    if (fb.isOnGround()) {
                        Location impacto = fb.getLocation();
                        impacto.getWorld().createExplosion(impacto, (float) (rocasExplosionPower * faseMultiplicador), false, false);
                        
                        // Fuego alrededor
                        for (int x = -rocasRadioFuego; x <= rocasRadioFuego; x++) {
                            for (int z = -rocasRadioFuego; z <= rocasRadioFuego; z++) {
                                Block b = impacto.getWorld().getBlockAt(
                                    impacto.getBlockX() + x,
                                    impacto.getBlockY(),
                                    impacto.getBlockZ() + z
                                );
                                if (b.getType() == Material.AIR && b.getRelative(BlockFace.DOWN).getType().isSolid()) {
                                    if (random.nextDouble() < 0.4) {
                                        fuegoColocado.add(b.getLocation());
                                        setBlockTracked(b, Material.FIRE);
                                    }
                                }
                            }
                        }
                        
                        fb.remove();
                        rocasActivas.remove(fb);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 2L);
        }
    }
    
    private void startGrietas() {
        grietasTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive() || grietasActivas.size() >= grietasMaxActivas) {
                    return;
                }
                
                crearGrieta();
            }
        };
        grietasTask.runTaskTimer(plugin, 200L, 300L);
    }
    
    private void crearGrieta() {
        List<Player> jugadores = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isPlayerExempt(p)) {
                jugadores.add(p);
            }
        }
        
        if (jugadores.isEmpty()) return;
        
        Player target = jugadores.get(random.nextInt(jugadores.size()));
        
        // Verificar altura
        if (alturaEnabled && target.getLocation().getY() - target.getWorld().getHighestBlockYAt(target.getLocation()) > alturaMinima) {
            return; // Inmune por altura
        }
        
        Location start = target.getLocation().clone();
        start.setY(start.getWorld().getHighestBlockYAt(start));
        
        int longitud = random.nextInt(grietasLongitudMax - grietasLongitudMin + 1) + grietasLongitudMin;
        int profundidad = random.nextInt(grietasProfundidadMax - grietasProfundidadMin + 1) + grietasProfundidadMin;
        
        // Dirección aleatoria
        Vector direccion = new Vector(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5).normalize();
        
        Location current = start.clone();
        for (int i = 0; i < longitud; i++) {
            current.add(direccion);
            grietasActivas.add(current.clone());
            
            // Excavar
            for (int y = 0; y < profundidad; y++) {
                Block b = current.clone().subtract(0, y, 0).getBlock();
                
                // Verificar resistencia
                if (obsidianaInmune && (b.getType() == Material.OBSIDIAN || b.getType() == Material.CRYING_OBSIDIAN)) {
                    continue;
                }
                
                if (b.getType().isSolid()) {
                    // Guardar estado original antes de destruir
                    bloquesCambiados.putIfAbsent(b.getLocation(), b.getType());
                    setBlockTracked(b, Material.AIR);
                    b.getWorld().spawnParticle(Particle.BLOCK, b.getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, b.getBlockData());
                }
            }
            
            // Lava en el fondo
            Block fondo = current.clone().subtract(0, profundidad, 0).getBlock();
            if (fondo.getType() == Material.AIR || fondo.getType() == Material.WATER) {
                // Guardar estado original y trackear lava
                bloquesCambiados.putIfAbsent(fondo.getLocation(), fondo.getType());
                lavaColocada.add(fondo.getLocation());
                setBlockTracked(fondo, Material.LAVA);
            }
            
            // Partículas
            current.getWorld().spawnParticle(Particle.LAVA, current, 10, 0.5, profundidad * 0.5, 0.5, 0);
        }
    }
    
    private void startCeniza() {
        cenizaTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) {
                    cancel();
                    return;
                }
                
                aplicarCeniza();
            }
        };
        cenizaTask.runTaskTimer(plugin, cenizaIntervalo, cenizaIntervalo);
    }
    
    private void aplicarCeniza() {
        messageBus.broadcast("§8§l💨 Ceniza volcánica...", "disaster");
        
        List<Player> jugadores = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isPlayerExempt(p)) {
                jugadores.add(p);
            }
        }
        
        for (Player p : jugadores) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 120, 1, false, true));
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0, false, true));
            p.spawnParticle(Particle.ASH, p.getLocation(), 500, cenizaRadio, cenizaRadio / 2, cenizaRadio, 0.1);
        }
    }
    
    private void startBombas() {
        bombasTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) {
                    cancel();
                    return;
                }
                
                String phase = getCurrentPhaseString();
                if (bombasSoloPico && !phase.equals("PICO")) {
                    return;
                }
                
                lanzarBomba();
            }
        };
        bombasTask.runTaskTimer(plugin, bombasIntervalo, bombasIntervalo);
    }
    
    private void lanzarBomba() {
        List<Player> jugadores = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isPlayerExempt(p)) {
                jugadores.add(p);
            }
        }
        
        if (jugadores.isEmpty()) return;
        
        Player target = jugadores.get(random.nextInt(jugadores.size()));
        Location spawn = target.getLocation().add(0, 30, 0);
        
        messageBus.broadcast("§c§l💣 ¡BOMBA DE MAGMA ENTRANTE!", "disaster");
        
        // Bomba grande (FallingBlock o efecto visual masivo)
        new BukkitRunnable() {
            Location current = spawn.clone();
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks++ > 100 || !isActive()) {
                    cancel();
                    return;
                }
                
                current.add(0, -0.8, 0);
                current.getWorld().spawnParticle(Particle.LAVA, current, 30, 1, 1, 1, 0.1);
                current.getWorld().spawnParticle(Particle.FLAME, current, 20, 0.8, 0.8, 0.8, 0.05);
                
                if (current.getBlock().getType().isSolid()) {
                    // Explosión masiva
                    current.getWorld().createExplosion(current, (float) (bombasExplosionPower * faseMultiplicador), false, false);
                    
                    // Fuego persistente en área grande
                    for (int x = -3; x <= 3; x++) {
                        for (int z = -3; z <= 3; z++) {
                            Block b = current.getWorld().getBlockAt(
                                current.getBlockX() + x,
                                current.getBlockY(),
                                current.getBlockZ() + z
                            );
                            if (b.getType() == Material.AIR && b.getRelative(BlockFace.DOWN).getType().isSolid()) {
                                fuegoColocado.add(b.getLocation());
                                setBlockTracked(b, random.nextBoolean() ? Material.FIRE : Material.SOUL_FIRE);
                            }
                        }
                    }
                    
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}
