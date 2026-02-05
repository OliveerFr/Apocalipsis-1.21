package me.apocalipsis.disaster;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.disaster.adapters.PerformanceAdapter;
import me.apocalipsis.state.TimeService;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ❄️ TORMENTA GLACIAL - Desastre de Ciclo 2
 * 
 * Frío extremo que congela el mundo progresivamente.
 * Reemplazo del Huracán con mecánicas completamente nuevas.
 */
public class TormentaGlacial extends DisasterBase {

    private final Random random = new Random();
    private final Set<FallingBlock> cristalesActivos = ConcurrentHashMap.newKeySet();
    private final Set<FallingBlock> estalactitasActivas = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Double> temperaturaJugador = new ConcurrentHashMap<>();
    private final Map<UUID, Long> ultimaRafaga = new ConcurrentHashMap<>();
    
    // Tracking de bloques modificados para limpieza
    private final Map<Location, Material> bloquesCambiados = new ConcurrentHashMap<>();
    
    // Config
    private boolean congelacionEnabled;
    private int congelacionRadio;
    private int congelacionIntervalo;
    
    private boolean hipotermiaEnabled;
    private double hipotermiaBase;
    private int hipotermiaIntervaloBase;
    private double hipotermiaIncremento;
    
    private boolean rafagasEnabled;
    private int rafagasIntervalo;
    private int rafagasSlowness;
    private int rafagasMiningFatigue;
    
    private boolean cristalesEnabled;
    private int cristalesIntervalo;
    private int cristalesMin;
    private int cristalesMax;
    private double cristalesDamage;
    
    private boolean estalactitasEnabled;
    private int estalactitasAdvertencia;
    private double estalactitasDamageMin;
    private double estalactitasDamageMax;
    
    private boolean nieblaEnabled;
    private boolean nieblaSoloPico;
    private int nieblaIntervalo;
    private int nieblaDuracion;
    
    // Protección
    private boolean zonasCalientesEnabled;
    private int zonasCalientesRadio;
    private double zonasCalientesReduccion;
    private Set<Material> fuentesCalor = new HashSet<>();
    
    private double cueroReduccion;
    private double netheriteReduccion;
    
    private double techoReduccion;
    private double paredesReduccion;
    
    // Tasks
    private BukkitRunnable congelacionTask;
    private BukkitRunnable hipotermiaTask;
    private BukkitRunnable rafagasTask;
    private BukkitRunnable cristalesTask;
    private BukkitRunnable estalactitasTask;
    private BukkitRunnable nieblaTask;
    
    // Fases
    private boolean fasesEnabled;
    private double faseMultiplicador = 1.0;

    public TormentaGlacial(Apocalipsis plugin, MessageBus messageBus, SoundUtil soundUtil,
                          TimeService timeService, PerformanceAdapter performanceAdapter) {
        super(plugin, messageBus, soundUtil, timeService, performanceAdapter, "tormenta_glacial");
        loadConfiguration();
    }

    private void loadConfiguration() {
        ConfigurationSection cfg = plugin.getConfigManager().getDesastresConfig()
            .getConfigurationSection("desastres.tormenta_glacial");
        
        if (cfg == null) {
            plugin.getLogger().warning("[TormentaGlacial] Configuración no encontrada, usando valores default");
            setDefaultConfig();
            return;
        }
        
        // Duración
        int duracionSegundos = cfg.getInt("duracion_segundos", 75);
        setMaxTicks(duracionSegundos);
        
        // Congelación
        ConfigurationSection cong = cfg.getConfigurationSection("congelacion");
        if (cong != null) {
            congelacionEnabled = cong.getBoolean("enabled", true);
            congelacionRadio = cong.getInt("radio", 3);
            congelacionIntervalo = cong.getInt("intervalo_ticks", 100);
        } else {
            congelacionEnabled = true;
            congelacionRadio = 3;
            congelacionIntervalo = 100;
        }
        
        // Hipotermia
        ConfigurationSection hipo = cfg.getConfigurationSection("hipotermia");
        if (hipo != null) {
            hipotermiaEnabled = hipo.getBoolean("enabled", true);
            hipotermiaBase = hipo.getDouble("damage_base", 0.5);
            hipotermiaIntervaloBase = hipo.getInt("intervalo_base_ticks", 200);
            hipotermiaIncremento = hipo.getDouble("incremento_acumulativo", 0.25);
        } else {
            hipotermiaEnabled = true;
            hipotermiaBase = 0.5;
            hipotermiaIntervaloBase = 200;
            hipotermiaIncremento = 0.25;
        }
        
        // Ráfagas
        ConfigurationSection raf = cfg.getConfigurationSection("rafagas_heladas");
        if (raf != null) {
            rafagasEnabled = raf.getBoolean("enabled", true);
            rafagasIntervalo = raf.getInt("intervalo_ticks", 400);
            rafagasSlowness = raf.getInt("slowness_level", 2);
            rafagasMiningFatigue = raf.getInt("mining_fatigue_level", 1);
        } else {
            rafagasEnabled = true;
            rafagasIntervalo = 400;
            rafagasSlowness = 2;
            rafagasMiningFatigue = 1;
        }
        
        // Cristales
        ConfigurationSection cris = cfg.getConfigurationSection("cristales_hielo");
        if (cris != null) {
            cristalesEnabled = cris.getBoolean("enabled", true);
            cristalesIntervalo = cris.getInt("intervalo_ticks", 300);
            cristalesMin = cris.getInt("min_cristales", 3);
            cristalesMax = cris.getInt("max_cristales", 6);
            cristalesDamage = cris.getDouble("damage", 1.0);
        } else {
            cristalesEnabled = true;
            cristalesIntervalo = 300;
            cristalesMin = 3;
            cristalesMax = 6;
            cristalesDamage = 1.0;
        }
        
        // Estalactitas
        ConfigurationSection esta = cfg.getConfigurationSection("estalactitas");
        if (esta != null) {
            estalactitasEnabled = esta.getBoolean("enabled", true);
            estalactitasAdvertencia = esta.getInt("advertencia_ticks", 40);
            estalactitasDamageMin = esta.getDouble("damage_min", 2.0);
            estalactitasDamageMax = esta.getDouble("damage_max", 4.0);
        } else {
            estalactitasEnabled = true;
            estalactitasAdvertencia = 40;
            estalactitasDamageMin = 2.0;
            estalactitasDamageMax = 4.0;
        }
        
        // Niebla
        ConfigurationSection nieb = cfg.getConfigurationSection("niebla");
        if (nieb != null) {
            nieblaEnabled = nieb.getBoolean("enabled", true);
            nieblaSoloPico = nieb.getBoolean("solo_en_pico", true);
            nieblaIntervalo = nieb.getInt("intervalo_ticks", 800);
            nieblaDuracion = nieb.getInt("duracion_ticks", 100);
        } else {
            nieblaEnabled = true;
            nieblaSoloPico = true;
            nieblaIntervalo = 800;
            nieblaDuracion = 100;
        }
        
        // Protección
        ConfigurationSection prot = cfg.getConfigurationSection("proteccion");
        if (prot != null) {
            ConfigurationSection zc = prot.getConfigurationSection("zonas_calientes");
            if (zc != null) {
                zonasCalientesEnabled = zc.getBoolean("enabled", true);
                zonasCalientesRadio = zc.getInt("radio_deteccion", 4);
                zonasCalientesReduccion = zc.getDouble("reduccion_damage", 0.80);
                List<String> fuentes = zc.getStringList("fuentes");
                for (String f : fuentes) {
                    try {
                        fuentesCalor.add(Material.valueOf(f));
                    } catch (IllegalArgumentException e) {
                        // Ignorar
                    }
                }
            } else {
                zonasCalientesEnabled = true;
                zonasCalientesRadio = 4;
                zonasCalientesReduccion = 0.80;
            }
            
            ConfigurationSection arm = prot.getConfigurationSection("armadura");
            if (arm != null) {
                cueroReduccion = arm.getDouble("cuero_reduccion", 0.30);
                netheriteReduccion = arm.getDouble("netherite_reduccion", 0.50);
            } else {
                cueroReduccion = 0.30;
                netheriteReduccion = 0.50;
            }
            
            ConfigurationSection ref = prot.getConfigurationSection("refugio");
            if (ref != null) {
                techoReduccion = ref.getDouble("techo_reduccion", 0.40);
                paredesReduccion = ref.getDouble("paredes_completas_reduccion", 0.60);
            } else {
                techoReduccion = 0.40;
                paredesReduccion = 0.60;
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
        setMaxTicks(75);
        congelacionEnabled = true;
        congelacionRadio = 3;
        congelacionIntervalo = 100;
        
        hipotermiaEnabled = true;
        hipotermiaBase = 0.5;
        hipotermiaIntervaloBase = 200;
        hipotermiaIncremento = 0.25;
        
        rafagasEnabled = true;
        rafagasIntervalo = 400;
        rafagasSlowness = 2;
        rafagasMiningFatigue = 1;
        
        cristalesEnabled = true;
        cristalesIntervalo = 300;
        cristalesMin = 3;
        cristalesMax = 6;
        cristalesDamage = 1.0;
        
        estalactitasEnabled = true;
        estalactitasAdvertencia = 40;
        estalactitasDamageMin = 2.0;
        estalactitasDamageMax = 4.0;
        
        nieblaEnabled = true;
        nieblaSoloPico = true;
        nieblaIntervalo = 800;
        nieblaDuracion = 100;
        
        zonasCalientesEnabled = true;
        zonasCalientesRadio = 4;
        zonasCalientesReduccion = 0.80;
        
        cueroReduccion = 0.30;
        netheriteReduccion = 0.50;
        
        techoReduccion = 0.40;
        paredesReduccion = 0.60;
        
        fasesEnabled = true;
    }

    @Override
    protected void onStart() {
        cristalesActivos.clear();
        estalactitasActivas.clear();
        temperaturaJugador.clear();
        ultimaRafaga.clear();
        bloquesCambiados.clear();
        faseMultiplicador = 1.0;
        
        // Anuncios
        messageBus.broadcast("§8§m                                                    ", "disaster");
        messageBus.broadcast("§b§l      ❄️ TORMENTA GLACIAL ❄️", "disaster");
        messageBus.broadcast("", "disaster");
        messageBus.broadcast("  §f§l¡Frío extremo congela el mundo!", "disaster");
        messageBus.broadcast("  §7Cristales de hielo caen del cielo", "disaster");
        messageBus.broadcast("", "disaster");
        messageBus.broadcast("  §e⚠ Busca fuentes de calor", "disaster");
        messageBus.broadcast("  §7Fogatas y lava protegen del frío", "disaster");
        messageBus.broadcast("§8§m                                                    ", "disaster");
        
        // Iniciar tasks
        if (congelacionEnabled) startCongelacion();
        if (hipotermiaEnabled) startHipotermia();
        if (rafagasEnabled) startRafagas();
        if (cristalesEnabled) startCristales();
        if (estalactitasEnabled) startEstalactitas();
        if (nieblaEnabled) startNiebla();
        
        plugin.getLogger().info("[TormentaGlacial] Desastre iniciado");
    }

    @Override
    protected void onTick() {
        // Actualizar multiplicador de fase
        if (fasesEnabled) {
            actualizarFase();
        }
        
        // Limpiar entities caídas
        cristalesActivos.removeIf(fb -> fb.isDead() || !fb.isValid());
        estalactitasActivas.removeIf(fb -> fb.isDead() || !fb.isValid());
    }

    @Override
    protected void onStop() {
        // Cancelar tasks
        if (congelacionTask != null) {
            congelacionTask.cancel();
            congelacionTask = null;
        }
        if (hipotermiaTask != null) {
            hipotermiaTask.cancel();
            hipotermiaTask = null;
        }
        if (rafagasTask != null) {
            rafagasTask.cancel();
            rafagasTask = null;
        }
        if (cristalesTask != null) {
            cristalesTask.cancel();
            cristalesTask = null;
        }
        if (estalactitasTask != null) {
            estalactitasTask.cancel();
            estalactitasTask = null;
        }
        if (nieblaTask != null) {
            nieblaTask.cancel();
            nieblaTask = null;
        }
        
        // Limpiar entities
        for (FallingBlock fb : cristalesActivos) {
            if (fb.isValid()) fb.remove();
        }
        for (FallingBlock fb : estalactitasActivas) {
            if (fb.isValid()) fb.remove();
        }
        
        // [LIMPIEZA] Restaurar bloques modificados (hielo → agua)
        int bloquesProcesados = 0;
        for (Map.Entry<Location, Material> entry : bloquesCambiados.entrySet()) {
            Location loc = entry.getKey();
            Material originalMaterial = entry.getValue();
            
            if (loc.getWorld() != null && loc.getBlock().getType() == Material.ICE) {
                setBlockTracked(loc.getBlock(), originalMaterial);
                bloquesProcesados++;
            }
        }
        
        if (bloquesProcesados > 0) {
            plugin.getLogger().info("[TormentaGlacial] Limpieza: " + bloquesProcesados + " bloques de hielo restaurados");
        }
        
        // Limpiar data
        cristalesActivos.clear();
        estalactitasActivas.clear();
        temperaturaJugador.clear();
        ultimaRafaga.clear();
        bloquesCambiados.clear();
        
        messageBus.broadcast("§b§l❄️ §fLa tormenta glacial se calma...", "disaster");
        
        plugin.getLogger().info("[TormentaGlacial] Desastre detenido");
    }
    
    @Override
    public void applyEffects(Player player) {
        // Efectos aplicados por tasks específicas
    }
    
    @Override
    protected String getDisasterName() {
        return "TORMENTA GLACIAL";
    }
    
    @Override
    protected String[] getPhaseNames() {
        return new String[] {
            "§7Frío Suave",
            "§fNieve Ligera",
            "§b§lVENTISCA INTENSA",
            "§9§lFRÍO EXTREMO",
            "§3§l¡CONGELACIÓN TOTAL!"
        };
    }
    
    // ============================================
    // MECÁNICAS ESPECÍFICAS
    // ============================================
    
    private void actualizarFase() {
        double progreso = (double) tickCounter / maxTicks;
        
        if (progreso < 0.25) {
            // INICIO
            faseMultiplicador = 0.7;
        } else if (progreso < 0.75) {
            // PICO
            faseMultiplicador = 1.5;
        } else {
            // DECLIVE
            faseMultiplicador = 0.9;
        }
    }
    
    private String getCurrentPhaseString() {
        double progreso = (double) tickCounter / maxTicks;
        
        if (progreso < 0.25) return "INICIO";
        else if (progreso < 0.75) return "PICO";
        else return "DECLIVE";
    }
    
    private void startCongelacion() {
        congelacionTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) {
                    cancel();
                    return;
                }
                
                congelarAlrededor();
            }
        };
        congelacionTask.runTaskTimer(plugin, congelacionIntervalo, congelacionIntervalo);
    }
    
    private void congelarAlrededor() {
        List<Player> jugadores = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isPlayerExempt(p)) {
                jugadores.add(p);
            }
        }
        
        for (Player p : jugadores) {
            Location centro = p.getLocation();
            
            for (int x = -congelacionRadio; x <= congelacionRadio; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -congelacionRadio; z <= congelacionRadio; z++) {
                        Block b = centro.getWorld().getBlockAt(
                            centro.getBlockX() + x,
                            centro.getBlockY() + y,
                            centro.getBlockZ() + z
                        );
                        
                        if (b.getType() == Material.WATER) {
                            // Guardar estado original antes de congelar
                            bloquesCambiados.putIfAbsent(b.getLocation(), Material.WATER);
                            setBlockTracked(b, Material.ICE);
                        }
                    }
                }
            }
        }
    }
    
    private void startHipotermia() {
        hipotermiaTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) {
                    cancel();
                    return;
                }
                
                aplicarHipotermia();
            }
        };
        hipotermiaTask.runTaskTimer(plugin, hipotermiaIntervaloBase, hipotermiaIntervaloBase);
    }
    
    private void aplicarHipotermia() {
        List<Player> jugadores = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isPlayerExempt(p)) {
                jugadores.add(p);
            }
        }
        
        for (Player p : jugadores) {
            double temp = temperaturaJugador.getOrDefault(p.getUniqueId(), 0.0);
            temp += hipotermiaIncremento;
            temperaturaJugador.put(p.getUniqueId(), temp);
            
            double damage = (hipotermiaBase + temp) * faseMultiplicador;
            double damageOriginal = damage;
            boolean tieneProteccion = false;
            String proteccionActiva = "";
            
            // Reducción por protecciones
            if (zonasCalientesEnabled && tieneFuenteCalorCerca(p)) {
                damage *= (1.0 - zonasCalientesReduccion);
                tieneProteccion = true;
                proteccionActiva = "§e🔥 Fuego Cercano";
                p.spawnParticle(Particle.FLAME, p.getLocation(), 5, 0.5, 0.5, 0.5, 0.02);
            }
            
            if (tieneArmaduraCuero(p)) {
                damage *= (1.0 - cueroReduccion);
                if (!tieneProteccion) {
                    tieneProteccion = true;
                    proteccionActiva = "§6🛡 Armadura Cuero";
                }
            } else if (tieneArmaduraNetherite(p)) {
                damage *= (1.0 - netheriteReduccion);
                if (!tieneProteccion) {
                    tieneProteccion = true;
                    proteccionActiva = "§5🛡 Armadura Netherite";
                }
            }
            
            // Refugio
            if (tieneRefugio(p)) {
                double refugioMult = tieneRefugioCompleto(p) ? (1.0 - paredesReduccion) : (1.0 - techoReduccion);
                damage *= refugioMult;
                if (!tieneProteccion) {
                    tieneProteccion = true;
                    proteccionActiva = "§7🏠 Refugio";
                }
            }
            
            if (damage > 0) {
                p.damage(damage);
                p.spawnParticle(Particle.SNOWFLAKE, p.getLocation(), 20, 1, 1, 1, 0.05);
                
                // Feedback de protección
                if (tieneProteccion) {
                    double reduccion = ((damageOriginal - damage) / damageOriginal) * 100;
                    p.sendActionBar(String.format("§a❄ %.1f°C §8| %s §a-%.0f%%", 
                        -temp * 10, proteccionActiva, reduccion));
                    p.playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.3f, 1.5f);
                } else {
                    // SIN PROTECCIÓN - Diagnóstico completo
                    String diagnostico = diagnosticarProteccionGlacial(p);
                    p.sendActionBar(String.format("§c§l⚠ HIPOTERMIA §8| §c%.1f°C §8- §7%s", -temp * 10, diagnostico));
                    
                    // Consejo cada 15 segundos
                    if (tickCounter % 300 == 0) {
                        p.sendMessage("§c❄ §7TORMENTA GLACIAL: Necesitas protección contra el frío");
                        p.sendMessage("§7  §8→ §7Coloca §eFuego§7/§eLava§7/§eFogatas§7 en radio de §e" + zonasCalientesRadio + " bloques§7 para §a-50%");
                        p.sendMessage("§7  §8→ §7Usa §earmadura de cuero completa§7 para §a-30%");
                        p.sendMessage("§7  §8→ §7Construye §etecho + paredes§7 para §a-40%");
                    }
                }
            }
        }
    }
    
    /**
     * Diagnostica por QUÉ el jugador no tiene protección contra frío
     */
    private String diagnosticarProteccionGlacial(Player p) {
        boolean tieneRefugio = tieneRefugioCompleto(p) || tieneRefugio(p);
        boolean tieneFuego = tieneFuenteCalorCerca(p);
        boolean tieneCuero = tieneArmaduraCuero(p);
        
        java.util.List<String> faltantes = new java.util.ArrayList<>();
        
        if (!tieneFuego) {
            // Buscar si hay fuego cerca pero lejos
            int distanciaFuego = buscarDistanciaFuenteCalor(p);
            if (distanciaFuego > 0) {
                faltantes.add("Fuego a §e" + distanciaFuego + " bloques§7 (máx §e" + zonasCalientesRadio + "§7)");
            } else {
                faltantes.add("Sin fuego/lava cerca");
            }
        }
        
        if (!tieneCuero) {
            int piezasCuero = contarPiezasCuero(p);
            if (piezasCuero > 0) {
                faltantes.add("Solo §e" + piezasCuero + "§7/4 piezas de cuero");
            } else {
                faltantes.add("Sin armadura de cuero");
            }
        }
        
        if (!tieneRefugio) {
            faltantes.add("Sin techo/paredes");
        }
        
        if (faltantes.isEmpty()) {
            return "Busca protección térmica";
        } else if (faltantes.size() == 1) {
            return faltantes.get(0);
        } else {
            return faltantes.get(0) + " §8+ §e" + (faltantes.size() - 1) + " más";
        }
    }
    
    /**
     * Busca la distancia a la fuente de calor más cercana
     */
    private int buscarDistanciaFuenteCalor(Player p) {
        Location centro = p.getLocation();
        int distanciaMinima = 999;
        
        // Escanear radio amplio (15 bloques) para diagnóstico
        for (int x = -15; x <= 15; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -15; z <= 15; z++) {
                    Block b = centro.getWorld().getBlockAt(
                        centro.getBlockX() + x,
                        centro.getBlockY() + y,
                        centro.getBlockZ() + z
                    );
                    
                    if (fuentesCalor.contains(b.getType())) {
                        int distancia = (int) Math.sqrt(x*x + y*y + z*z);
                        distanciaMinima = Math.min(distanciaMinima, distancia);
                    }
                }
            }
        }
        
        return distanciaMinima == 999 ? 0 : distanciaMinima;
    }
    
    /**
     * Cuenta cuántas piezas de armadura de cuero tiene el jugador
     */
    private int contarPiezasCuero(Player p) {
        int piezas = 0;
        if (p.getInventory().getHelmet() != null && p.getInventory().getHelmet().getType() == Material.LEATHER_HELMET) piezas++;
        if (p.getInventory().getChestplate() != null && p.getInventory().getChestplate().getType() == Material.LEATHER_CHESTPLATE) piezas++;
        if (p.getInventory().getLeggings() != null && p.getInventory().getLeggings().getType() == Material.LEATHER_LEGGINGS) piezas++;
        if (p.getInventory().getBoots() != null && p.getInventory().getBoots().getType() == Material.LEATHER_BOOTS) piezas++;
        return piezas;
    }
    
    private boolean tieneFuenteCalorCerca(Player p) {
        Location centro = p.getLocation();
        
        for (int x = -zonasCalientesRadio; x <= zonasCalientesRadio; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -zonasCalientesRadio; z <= zonasCalientesRadio; z++) {
                    Block b = centro.getWorld().getBlockAt(
                        centro.getBlockX() + x,
                        centro.getBlockY() + y,
                        centro.getBlockZ() + z
                    );
                    
                    if (fuentesCalor.contains(b.getType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private boolean tieneArmaduraCuero(Player p) {
        return p.getInventory().getHelmet() != null && 
               p.getInventory().getHelmet().getType() == Material.LEATHER_HELMET;
    }
    
    private boolean tieneArmaduraNetherite(Player p) {
        return p.getInventory().getChestplate() != null && 
               p.getInventory().getChestplate().getType() == Material.NETHERITE_CHESTPLATE;
    }
    
    private boolean tieneRefugio(Player p) {
        Block techo = p.getLocation().add(0, 2, 0).getBlock();
        return techo.getType().isSolid();
    }
    
    private boolean tieneRefugioCompleto(Player p) {
        Location loc = p.getLocation();
        Block techo = loc.clone().add(0, 2, 0).getBlock();
        if (!techo.getType().isSolid()) return false;
        
        // Verificar paredes
        int paredesSolidas = 0;
        if (loc.clone().add(1, 0, 0).getBlock().getType().isSolid()) paredesSolidas++;
        if (loc.clone().add(-1, 0, 0).getBlock().getType().isSolid()) paredesSolidas++;
        if (loc.clone().add(0, 0, 1).getBlock().getType().isSolid()) paredesSolidas++;
        if (loc.clone().add(0, 0, -1).getBlock().getType().isSolid()) paredesSolidas++;
        
        return paredesSolidas >= 3;
    }
    
    private void startRafagas() {
        rafagasTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) {
                    cancel();
                    return;
                }
                
                aplicarRafaga();
            }
        };
        rafagasTask.runTaskTimer(plugin, rafagasIntervalo, rafagasIntervalo);
    }
    
    private void aplicarRafaga() {
        messageBus.broadcast("§b§l🌨️ ¡Ráfaga helada!", "disaster");
        
        List<Player> jugadores = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isPlayerExempt(p)) {
                jugadores.add(p);
            }
        }
        
        for (Player p : jugadores) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, rafagasSlowness, false, true));
            p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 80, rafagasMiningFatigue, false, true));
            p.spawnParticle(Particle.SNOWFLAKE, p.getLocation(), 100, 2, 2, 2, 0.1);
            ultimaRafaga.put(p.getUniqueId(), System.currentTimeMillis());
        }
    }
    
    private void startCristales() {
        cristalesTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) {
                    cancel();
                    return;
                }
                
                lanzarCristales();
            }
        };
        cristalesTask.runTaskTimer(plugin, cristalesIntervalo, cristalesIntervalo);
    }
    
    private void lanzarCristales() {
        int cantidad = random.nextInt(cristalesMax - cristalesMin + 1) + cristalesMin;
        
        List<Player> jugadores = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isPlayerExempt(p)) {
                jugadores.add(p);
            }
        }
        
        if (jugadores.isEmpty()) return;
        
        // CINEMÁTICO: Mensaje dramático global
        messageBus.broadcast("§b§l❄§l §f§lLLUVIA DE CRISTALES §b§l❄", "crystal_rain");
        
        // CINEMÁTICO: Sonidos de advertencia épicos
        soundUtil.playSoundAll(Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        soundUtil.playSoundAll(Sound.BLOCK_BELL_USE, 0.8f, 1.8f);
        
        // CINEMÁTICO: Countdown visual de 3 segundos
        for (Player p : jugadores) {
            p.sendTitle("§b§l❄ CRISTALES", "§f§l3 segundos...", 10, 30, 10);
        }
        
        // Countdown 2 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player p : jugadores) {
                p.sendTitle("§b§l2", "", 0, 15, 5);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
            }
        }, 20L);
        
        // Countdown 1 segundo
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player p : jugadores) {
                p.sendTitle("§b§l1", "", 0, 15, 5);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
            }
        }, 40L);
        
        // CINEMÁTICO: Lanzar cristales después del countdown (3 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            // Sonidos épicos de impacto
            soundUtil.playSoundAll(Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.5f);
            soundUtil.playSoundAll(Sound.BLOCK_GLASS_BREAK, 1.5f, 0.8f);
            
            for (int i = 0; i < cantidad; i++) {
                Player target = jugadores.get(random.nextInt(jugadores.size()));
                
                Location spawn = target.getLocation().add(
                    random.nextDouble() * 10 - 5,
                    15 + random.nextDouble() * 10,
                    random.nextDouble() * 10 - 5
                );
                
                // CINEMÁTICO: Partículas de formación de cristal
                for (int y = 0; y < 8; y++) {
                    Location particleLoc = spawn.clone().subtract(0, y, 0);
                    spawn.getWorld().spawnParticle(Particle.SNOWFLAKE, particleLoc, 5, 0.3, 0.3, 0.3, 0.02);
                    spawn.getWorld().spawnParticle(Particle.FIREWORK, particleLoc, 2, 0.2, 0.2, 0.2, 0.01);
                }
                
                FallingBlock fb = spawn.getWorld().spawnFallingBlock(spawn, Material.BLUE_ICE.createBlockData());
                fb.setDropItem(false);
                fb.setHurtEntities(true);
                cristalesActivos.add(fb);
                
                // CINEMÁTICO: Estela de partículas mientras cae
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (!fb.isValid() || ticks++ > 100) {
                            cancel();
                            return;
                        }
                        Location loc = fb.getLocation();
                        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 3, 0.2, 0.2, 0.2, 0.01);
                        loc.getWorld().spawnParticle(Particle.CLOUD, loc, 1, 0.1, 0.1, 0.1, 0);
                    }
                }.runTaskTimer(plugin, 0L, 2L);
            }
            
            // Título de impacto para jugadores
            for (Player p : jugadores) {
                p.sendTitle("§b§l❄ ¡IMPACTO!", "", 5, 20, 10);
            }
        }, 60L);
    }
    
    private void startEstalactitas() {
        estalactitasTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) {
                    cancel();
                    return;
                }
                
                if (random.nextDouble() < 0.15) {
                    lanzarEstalactita();
                }
            }
        };
        estalactitasTask.runTaskTimer(plugin, 100L, 100L);
    }
    
    private void lanzarEstalactita() {
        List<Player> jugadores = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isPlayerExempt(p)) {
                jugadores.add(p);
            }
        }
        
        if (jugadores.isEmpty()) return;
        
        Player target = jugadores.get(random.nextInt(jugadores.size()));
        Location spawn = target.getLocation().add(0, 20, 0);
        
        // CINEMÁTICO: Sonidos dramáticos de advertencia
        spawn.getWorld().playSound(spawn, Sound.BLOCK_BELL_USE, 1.2f, 0.8f);
        spawn.getWorld().playSound(spawn, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.5f);
        
        // CINEMÁTICO: Título de advertencia para jugador objetivo
        target.sendTitle("§f§l⚠", "§b§lEStalactita sobre ti", 10, 40, 10);
        
        // CINEMÁTICO: Advertencia visual mejorada con columna épica
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= estalactitasAdvertencia / 10 || !isActive()) {
                    cancel();
                    return;
                }
                
                // Columna de advertencia de 25 bloques con partículas densas
                for (int y = 0; y < 25; y++) {
                    Location particleLoc = spawn.clone().subtract(0, y, 0);
                    
                    // Partículas de hielo brillante
                    spawn.getWorld().spawnParticle(Particle.SNOWFLAKE, particleLoc, 8, 0.4, 0.3, 0.4, 0.05);
                    spawn.getWorld().spawnParticle(Particle.FIREWORK, particleLoc, 3, 0.2, 0.2, 0.2, 0.01);
                    spawn.getWorld().spawnParticle(Particle.CLOUD, particleLoc, 2, 0.3, 0.2, 0.3, 0);
                    
                    // Partículas azules brillantes cada 3 bloques
                    if (y % 3 == 0) {
                        spawn.getWorld().spawnParticle(Particle.DUST,  particleLoc, 4, 0.3, 0.2, 0.3,
                            new Particle.DustOptions(org.bukkit.Color.fromRGB(100, 200, 255), 1.5f));
                    }
                }
                
                // Anillo expansivo en el suelo
                for (int angle = 0; angle < 360; angle += 30) {
                    double rad = Math.toRadians(angle);
                    double radius = 1.5 + (ticks * 0.1);
                    double x = spawn.getX() + radius * Math.cos(rad);
                    double z = spawn.getZ() + radius * Math.sin(rad);
                    Location ringLoc = new Location(spawn.getWorld(), x, spawn.getY() - 20, z);
                    spawn.getWorld().spawnParticle(Particle.SNOWFLAKE, ringLoc, 1, 0, 0, 0, 0);
                }
                
                // Sonido pulsante cada segundo
                if (ticks % 2 == 0) {
                    spawn.getWorld().playSound(spawn, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 2.0f);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 10L);
        
        // CINEMÁTICO: Lanzar con efectos épicos
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) {
                    cancel();
                    return;
                }
                
                // Efectos de formación
                spawn.getWorld().spawnParticle(Particle.EXPLOSION, spawn, 3, 0.5, 0.5, 0.5, 0);
                spawn.getWorld().spawnParticle(Particle.SNOWFLAKE, spawn, 30, 1, 1, 1, 0.1);
                spawn.getWorld().playSound(spawn, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.6f);
                spawn.getWorld().playSound(spawn, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.8f);
                
                FallingBlock fb = spawn.getWorld().spawnFallingBlock(spawn, Material.POINTED_DRIPSTONE.createBlockData());
                fb.setDropItem(false);
                fb.setHurtEntities(true);
                estalactitasActivas.add(fb);
                
                // CINEMÁTICO: Estela de partículas mientras cae
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (!fb.isValid() || ticks++ > 100) {
                            cancel();
                            return;
                        }
                        Location loc = fb.getLocation();
                        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 5, 0.3, 0.3, 0.3, 0.02);
                        loc.getWorld().spawnParticle(Particle.CLOUD, loc, 2, 0.2, 0.2, 0.2, 0);
                        loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 1, 0.1, 0.1, 0.1, 0);
                    }
                }.runTaskTimer(plugin, 0L, 2L);
                
                // Título de impacto
                target.sendTitle("§b§l⚠ ¡CUIDADO!", "", 5, 15, 5);
            }
        }.runTaskLater(plugin, estalactitasAdvertencia);
    }
    
    private void startNiebla() {
        nieblaTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) {
                    cancel();
                    return;
                }
                
                String phase = getCurrentPhaseString();
                if (nieblaSoloPico && !phase.equals("PICO")) {
                    return;
                }
                
                aplicarNiebla();
            }
        };
        nieblaTask.runTaskTimer(plugin, nieblaIntervalo, nieblaIntervalo);
    }
    
    private void aplicarNiebla() {
        messageBus.broadcast("§f§l🌫️ Niebla congelante...", "disaster");
        
        List<Player> jugadores = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!isPlayerExempt(p)) {
                jugadores.add(p);
            }
        }
        
        for (Player p : jugadores) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, nieblaDuracion, 0, false, true));
            p.spawnParticle(Particle.SNOWFLAKE, p.getLocation(), 300, 5, 3, 5, 0.05);
        }
    }
}
