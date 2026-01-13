package me.apocalipsis.events;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;

/**
 * El Camino al End - Mini-evento de exploración y descubrimiento
 * 
 * Contexto narrativo:
 * Anomalías dimensionales comienzan a aparecer en el mundo.
 * El Observador percibe ecos de un lugar que no debería estar cerca.
 * Los jugadores deben explorar, recolectar fragmentos del eco y descubrir
 * la verdad sobre el End sin combate directo.
 * 
 * "Esto... no debería estar aquí..." — El Observador
 * 
 * Fases del evento:
 * 1. ANOMALIAS (30-45 min): Anomalías aparecen en el mundo, emiten partículas y sonidos
 * 2. ECOS (45-60 min): Al acercarse a anomalías, aparecen fragmentos del eco
 * 3. REVELACION (15-30 min): Al recolectar 40 fragmentos globalmente, se revela un portal incompleto
 * 
 * Filosofía: Exploración pura, sin combate. Colaboración natural sin forzarla.
 * El evento NO culmina en victoria, sino en más preguntas (portal incompleto).
 */
public class CaminoEndEvent extends EventBase {
    
    // ═══════════════════════════════════════════════════════════════════
    // ESTADO DEL EVENTO
    // ═══════════════════════════════════════════════════════════════════
    
    public enum Fase {
        ANOMALIAS,      // Fase 1: Anomalías aparecen en el mundo
        RESONANCIA,     // Fase 2: Fragmentos dimensionales disponibles
        REVELACION      // Fase 3: Portal se revela al alcanzar 40 fragmentos
    }
    
    private Fase faseActual;
    private int ticksEnFase;
    private int ticksTotales;
    
    // Tracking de progreso global
    private int fragmentosRecolectadosGlobalmente = 0;
    private static final int FRAGMENTOS_OBJETIVO = 40;
    
    // Anomalías activas
    private Map<Location, AnomaliaData> anomaliasActivas = new ConcurrentHashMap<>();
    private static final int MAX_ANOMALIAS_SIMULTANEAS = 8;
    private static final int DISTANCIA_MINIMA_SPAWN = 150;
    
    // Portal final
    private Location portalLocation;
    private boolean portalGenerado = false;
    
    // Tracking de participación para recompensas
    private Map<UUID, Integer> fragmentosPorJugador = new HashMap<>();
    private Set<UUID> participantes = new HashSet<>();
    
    // Sistema de inmersión
    private BossBar bossBar;
    private Set<UUID> jugadoresQueVieronPrimeraAnomalia = new HashSet<>();
    private int ticksSinFragmento = 0;
    private int ultimoMensajePista = 0;
    
    // Tracking de anomalías visitadas por jugador
    private Map<UUID, Set<Location>> anomaliasVisitadasPorJugador = new HashMap<>();
    private Map<UUID, Location> anomaliaActualPorJugador = new HashMap<>();
    
    // Sistema de brújula funcional
    private BukkitTask brujulaTask;
    
    // Sistema de mini-eventos aleatorios
    private int ticksDesdeUltimoMiniEvento = 0;
    private static final int MIN_TICKS_ENTRE_MINIEVENTO = 9600;  // 8 minutos
    private static final int MAX_TICKS_ENTRE_MINIEVENTO = 14400; // 12 minutos
    private int proximoMiniEventoEn = 0;
    
    // Control de cliffhanger (para evitar ejecuciones múltiples)
    private boolean cliffhangerEjecutado = false;
    
    // ══════════════════════════════════════════════════════════════════
    // SISTEMA DE HORDAS Y WARDEN (FASE RESONANCIA)
    // ══════════════════════════════════════════════════════════════════
    private int ticksDesdeUltimaHorda = 0;
    private static final int INTERVALO_HORDAS_MIN = 6000;  // 5 minutos mínimo
    private static final int INTERVALO_HORDAS_MAX = 12000; // 10 minutos máximo
    private int proximaHordaEn = 0;
    
    private org.bukkit.entity.Warden wardenActivo = null;
    private boolean wardenSpawneado = false;
    private BukkitTask wardenDistanceTask = null;
    private static final double WARDEN_MAX_DISTANCE = 100.0; // Radio máximo del Warden
    
    // Sistema de desafío "Caza de Anomalías"
    private boolean desafioCazaActivo = false;
    private boolean desafioCazaOfrecido = false;
    private int ticksDesafioCaza = 0;
    private static final int DURACION_DESAFIO_CAZA_TICKS = 6000; // 5 minutos
    private int anomaliasEncontradasDesafio = 0;
    private static final int ANOMALIAS_REQUERIDAS_DESAFIO = 3;
    private Set<UUID> participantesDesafio = new HashSet<>();
    
    // Configuración del evento
    private FileConfiguration config;
    
    // Tareas programadas
    private BukkitTask mainTask;
    private BukkitTask anomaliaSpawnTask;
    private Map<Location, BukkitTask> anomaliaParticleTasks = new HashMap<>();
    private Map<Location, BukkitTask> anomaliaSoundTasks = new HashMap<>();
    
    private final Random random = new Random();
    private CaminoEndListener listener;
    private CaminoEndItems items;
    
    // ═══════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════
    
    public CaminoEndEvent(Apocalipsis plugin, MessageBus messageBus, SoundUtil soundUtil) {
        super(plugin, messageBus, soundUtil, "camino_end");
        loadConfig();
        this.items = new CaminoEndItems(plugin);
        this.listener = new CaminoEndListener(this, plugin);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════════
    
    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "camino_end.yml");
        if (!configFile.exists()) {
            plugin.saveResource("camino_end.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // IMPLEMENTACIÓN DE EVENTBASE
    // ═══════════════════════════════════════════════════════════════════
    
    @Override
    public String getDisplayName() {
        return config.getString("evento.nombre_display", "§5§lEl Camino al End");
    }
    
    @Override
    public String getDescription() {
        return config.getString("evento.descripcion", "Anomalías dimensionales aparecen en el mundo...");
    }
    
    @Override
    public void onStart() {
        faseActual = Fase.ANOMALIAS;
        ticksEnFase = 0;
        ticksTotales = 0;
        fragmentosRecolectadosGlobalmente = 0;
        ticksSinFragmento = 0;
        jugadoresQueVieronPrimeraAnomalia.clear();
        
        // Crear BossBar
        crearBossBar();
        
        // Registrar listener
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        
        // Cambiar clima a atmosférico
        cambiarClimaFase(Fase.ANOMALIAS);
        
        // Anuncio inicial
        anunciarInicio();
        
        // Iniciar task principal
        mainTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 0L, 1L);
        
        // Iniciar spawn de anomalías
        anomaliaSpawnTask = plugin.getServer().getScheduler().runTaskTimer(plugin, 
            this::intentarSpawnAnomalias, 
            100L,  // Delay inicial: 5 segundos
            200L   // Intervalo: 10 segundos
        );
        
        // Registrar listener
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        
        // Inicializar sistema de mini-eventos
        ticksDesdeUltimoMiniEvento = 0;
        proximoMiniEventoEn = MIN_TICKS_ENTRE_MINIEVENTO + random.nextInt(MAX_TICKS_ENTRE_MINIEVENTO - MIN_TICKS_ENTRE_MINIEVENTO);
        
        // Inicializar sistema de hordas
        ticksDesdeUltimaHorda = 0;
        proximaHordaEn = INTERVALO_HORDAS_MIN + random.nextInt(INTERVALO_HORDAS_MAX - INTERVALO_HORDAS_MIN);
        
        plugin.getLogger().info("[CaminoEndEvent] Evento iniciado - Fase: " + faseActual);
    }
    
    @Override
    public void onStop() {
        // Cancelar todas las tasks
        if (mainTask != null) mainTask.cancel();
        if (anomaliaSpawnTask != null) anomaliaSpawnTask.cancel();
        
        // Cancelar tasks de partículas y sonidos
        for (BukkitTask task : anomaliaParticleTasks.values()) {
            if (task != null) task.cancel();
        }
        anomaliaParticleTasks.clear();
        
        for (BukkitTask task : anomaliaSoundTasks.values()) {
            if (task != null) task.cancel();
        }
        anomaliaSoundTasks.clear();
        
        // Limpiar anomalías
        anomaliasActivas.clear();
        
        // Limpiar tracking de visitas
        anomaliasVisitadasPorJugador.clear();
        anomaliaActualPorJugador.clear();
        
        // Remover BossBar
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        
        // Restaurar clima
        restaurarClima();
        
        // Anuncio de finalización
        anunciarFinalizacion();
        
        plugin.getLogger().info("[CaminoEndEvent] Evento detenido");
    }
    
    @Override
    public void onTick() {
        ticksEnFase++;
        ticksTotales++;
        ticksSinFragmento++;
        
        // Actualizar BossBar cada segundo
        if (ticksTotales % 20 == 0) {
            actualizarBossBar();
        }
        
        // Sistema de pistas si no hay progreso (cada 3 minutos en fase RESONANCIA)
        if (faseActual == Fase.RESONANCIA && ticksSinFragmento > 3600 && ticksSinFragmento - ultimoMensajePista > 3600) {
            enviarPista();
            ultimoMensajePista = ticksSinFragmento;
        }
        
        // Tensión ambiental progresiva durante RESONANCIA
        if (faseActual == Fase.RESONANCIA && ticksTotales % 1200 == 0) { // Cada minuto
            aplicarTensionAmbiental();
        }
        
        // Actualizar brújulas y guía en action bar (cada segundo)
        if (ticksTotales % 20 == 0) {
            if (faseActual == Fase.ANOMALIAS) {
                actualizarBrujulas();
                mostrarGuiaAnomalias(); // Guía hacia anomalías en Fase I
            } else if (faseActual == Fase.RESONANCIA) {
                actualizarBrujulas();
                mostrarGuiaFragmentos(); // Guía hacia fragmentos en Fase II
            }
        }
        
        // Guía hacia el portal incompleto durante REVELACION
        if (ticksTotales % 20 == 0 && faseActual == Fase.REVELACION && portalLocation != null) {
            mostrarGuiaPortal();
        }
        
        // Actualizar anomalías existentes
        actualizarAnomalias();
        
        // Sistema de mini-eventos aleatorios (solo en fases activas)
        if (faseActual == Fase.ANOMALIAS || faseActual == Fase.RESONANCIA) {
            ticksDesdeUltimoMiniEvento++;
            if (ticksDesdeUltimoMiniEvento >= proximoMiniEventoEn) {
                activarMiniEventoAleatorio();
                ticksDesdeUltimoMiniEvento = 0;
                proximoMiniEventoEn = MIN_TICKS_ENTRE_MINIEVENTO + random.nextInt(MAX_TICKS_ENTRE_MINIEVENTO - MIN_TICKS_ENTRE_MINIEVENTO);
            }
        }
        
        // Sistema de desafío Caza de Anomalías
        if (desafioCazaActivo) {
            ticksDesafioCaza++;
            
            // Actualizar action bar cada segundo
            if (ticksDesafioCaza % 20 == 0) {
                int segundosRestantes = (DURACION_DESAFIO_CAZA_TICKS - ticksDesafioCaza) / 20;
                int minutosRestantes = segundosRestantes / 60;
                int segundos = segundosRestantes % 60;
                String tiempoRestante = String.format("%d:%02d", minutosRestantes, segundos);
                
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    p.sendActionBar("§e§l⚡ DESAFÍO: §7" + anomaliasEncontradasDesafio + "/" + ANOMALIAS_REQUERIDAS_DESAFIO + 
                        " anomalías §8| §c⏱ " + tiempoRestante);
                }
            }
            
            // Verificar timeout
            if (ticksDesafioCaza >= DURACION_DESAFIO_CAZA_TICKS) {
                fallarDesafioCaza();
            }
            
            // Verificar éxito
            if (anomaliasEncontradasDesafio >= ANOMALIAS_REQUERIDAS_DESAFIO) {
                completarDesafioCaza();
            }
        }
        
        // Verificar transiciones de fase
        verificarTransicionesFase();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE FASES
    // ═══════════════════════════════════════════════════════════════════
    
    private void verificarTransicionesFase() {
        switch (faseActual) {
            case ANOMALIAS:
                // Transición automática a RESONANCIA después de 30-45 minutos (36000-54000 ticks)
                int duracionMinAnomalias = config.getInt("fases.anomalias.duracion_min_ticks", 36000);
                if (ticksEnFase >= duracionMinAnomalias) {
                    transicionarAFase(Fase.RESONANCIA);
                }
                break;
                
            case RESONANCIA:
                // Transición a REVELACION cuando se alcancen 40 fragmentos
                if (fragmentosRecolectadosGlobalmente >= FRAGMENTOS_OBJETIVO && !portalGenerado) {
                    // Verificar si el Warden está vivo para el último fragmento
                    if (wardenActivo != null && !wardenActivo.isDead()) {
                        // NO transicionar hasta que maten al Warden
                        if (ticksEnFase % 100 == 0) { // Cada 5 segundos
                            messageBus.broadcast("§c§l⚠ EL GUARDIÁN BLOQUEA EL CAMINO ⚠", "warden_obligatorio");
                            messageBus.broadcast("§7§oEl Guardián de las Profundidades impide el avance...", "warden_obligatorio");
                            messageBus.broadcast("", "warden_obligatorio");
                            messageBus.broadcast("§5§l⚡ EL OBSERVADOR:", "warden_obligatorio");
                            messageBus.broadcast("§8§o\"...No permitirá que avances...\"", "warden_obligatorio");
                            messageBus.broadcast("§c§o\"...Debe ser derrotado para continuar...\"", "warden_obligatorio");
                        }
                    } else {
                        transicionarAFase(Fase.REVELACION);
                    }
                }
                
                // ════════════════════════════════════════════════════════════
                // SISTEMA DE HORDAS DE ENTIDADES
                // ════════════════════════════════════════════════════════════
                ticksDesdeUltimaHorda++;
                if (ticksDesdeUltimaHorda >= proximaHordaEn) {
                    spawnearHorda();
                    ticksDesdeUltimaHorda = 0;
                    proximaHordaEn = INTERVALO_HORDAS_MIN + random.nextInt(INTERVALO_HORDAS_MAX - INTERVALO_HORDAS_MIN);
                }
                
                // ════════════════════════════════════════════════════════════
                // SPAWN DE WARDEN EN LOS ÚLTIMOS FRAGMENTOS
                // ════════════════════════════════════════════════════════════
                if (!wardenSpawneado && fragmentosRecolectadosGlobalmente >= 35) {
                    // Entre 35-39 fragmentos, spawn del Warden cuando alguien esté cerca de una anomalía
                    verificarSpawnWarden();
                }
                
                // Efectos visuales progresivos
                if (ticksEnFase % 200 == 0) { // Cada 10 segundos
                    aplicarEfectosVisualesResonancia();
                }
                
                // Efectos de corrupción progresiva
                if (ticksEnFase % 600 == 0) { // Cada 30 segundos
                    aplicarCorrupcionProgresiva();
                }
                break;
                
            case REVELACION:
                // Mantener fase hasta que se detenga manualmente o timeout
                int duracionMaxRevelacion = config.getInt("fases.revelacion.duracion_max_ticks", 36000);
                if (ticksEnFase >= duracionMaxRevelacion) {
                    // Auto-detener evento después de 30 minutos en revelación
                    plugin.getLogger().info("[CaminoEnd] Timeout en REVELACION - Ejecutando cliffhanger");
                    ejecutarCliffhangerYFinalizar();
                }
                break;
        }
    }
    
    private void transicionarAFase(Fase nuevaFase) {
        Fase faseAnterior = faseActual;
        faseActual = nuevaFase;
        ticksEnFase = 0;
        
        plugin.getLogger().info("[CaminoEndEvent] Transición: " + faseAnterior + " → " + nuevaFase);
        
        // Cambiar clima atmosférico
        cambiarClimaFase(nuevaFase);
        
        // Limpiar tracking de anomalías visitadas al cambiar de fase
        if (nuevaFase == Fase.RESONANCIA) {
            // Al entrar en RESONANCIA, limpiar anomalías activas para evitar guías desfasadas
            if (!anomaliasActivas.isEmpty()) {
                List<Location> actuales = new ArrayList<>(anomaliasActivas.keySet());
                for (Location loc : actuales) {
                    despawnearAnomalia(loc);
                }
                anomaliasActivas.clear();
            }
            // Reset parcial: mantener historial pero permitir revisitar
            for (UUID uuid : anomaliasVisitadasPorJugador.keySet()) {
                anomaliasVisitadasPorJugador.get(uuid).clear();
            }
            anomaliaActualPorJugador.clear();
        }
        
        switch (nuevaFase) {
            case ANOMALIAS:
                // Ya está en fase de anomalías, no hacer nada adicional
                break;
                
            case RESONANCIA:
                anunciarFaseResonancia();
                break;
                
            case REVELACION:
                anunciarFaseRevelacion();
                generarPortalIncompleto();
                break;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE ANOMALÍAS
    // ═══════════════════════════════════════════════════════════════════
    
    private void intentarSpawnAnomalias() {
        if (anomaliasActivas.size() >= MAX_ANOMALIAS_SIMULTANEAS) {
            return;
        }
        
        // Obtener jugadores online
        List<Player> jugadoresOnline = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        if (jugadoresOnline.isEmpty()) {
            return;
        }
        
        // Seleccionar jugador aleatorio
        Player jugadorObjetivo = jugadoresOnline.get(random.nextInt(jugadoresOnline.size()));
        
        // Generar ubicación aleatoria cerca del jugador
        Location ubicacionSpawn = generarUbicacionAnomaliaAleatoria(jugadorObjetivo.getLocation());
        
        if (ubicacionSpawn != null) {
            spawnearAnomalia(ubicacionSpawn);
        }
    }
    
    private Location generarUbicacionAnomaliaAleatoria(Location centro) {
        World world = centro.getWorld();
        if (world == null) return null;
        
        int intentos = 0;
        while (intentos < 10) {
            // Generar ubicación aleatoria en radio 100-300 bloques
            int distancia = 100 + random.nextInt(200);
            double angulo = random.nextDouble() * 2 * Math.PI;
            
            int offsetX = (int) (distancia * Math.cos(angulo));
            int offsetZ = (int) (distancia * Math.sin(angulo));
            
            int x = centro.getBlockX() + offsetX;
            int z = centro.getBlockZ() + offsetZ;
            int y = world.getHighestBlockYAt(x, z) + 1;
            
            Location candidata = new Location(world, x + 0.5, y, z + 0.5);
            
            // Verificar que no esté muy cerca de otras anomalías
            boolean muyCerca = false;
            for (Location anomaliaExistente : anomaliasActivas.keySet()) {
                if (anomaliaExistente.distance(candidata) < DISTANCIA_MINIMA_SPAWN) {
                    muyCerca = true;
                    break;
                }
            }
            
            if (!muyCerca) {
                return candidata;
            }
            
            intentos++;
        }
        
        return null; // No se encontró ubicación válida
    }
    
    private void spawnearAnomalia(Location ubicacion) {
        // Determinar tipo de anomalía aleatoriamente
        TipoAnomalia tipo = TipoAnomalia.obtenerAleatorio(random);
        
        AnomaliaData anomalia = new AnomaliaData(ubicacion, System.currentTimeMillis(), tipo);
        anomaliasActivas.put(ubicacion, anomalia);
        
        // ══════════════════════════════════════════════════════════════════
        // GENERAR ESTRUCTURA DE BLOQUES DEL END
        // ══════════════════════════════════════════════════════════════════
        generarEstructuraEnd(ubicacion, tipo);
        
        // Mensajes especiales según tipo
        if (tipo == TipoAnomalia.ANTIGUA) {
            messageBus.broadcast("§5§l⚡ EL OBSERVADOR:", "observador");
            messageBus.broadcast("§5§o\"...esto es diferente... MÁS VIEJO...\"", "anomalia_antigua");
            
            // Título y sonido para todos los jugadores
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.sendTitle("§5§l⚡ ANOMALÍA ANTIGUA ⚡", "§5§o...más vieja que el tiempo...", 10, 50, 15);
                p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.7f, 0.6f);
                p.playSound(p.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.4f, 0.5f);
            }
        }
        
        // ══════════════════════════════════════════════════════════════════
        // SPAWN DE GUARDIANES DIMENSIONALES - TODAS LAS ANOMALÍAS
        // Cantidad y poder según tipo de anomalía
        // ══════════════════════════════════════════════════════════════════
        World world = ubicacion.getWorld();
        if (world != null) {
            // Determinar cantidad según tipo de anomalía
            int cantidadEndermans = switch (tipo) {
                case NORMAL -> 1;                    // 1 enderman
                case INESTABLE -> 2;                 // 2 endermans
                case ECO_BRASAS, ECO_SOMBRAS, ECO_PIEDRA -> 2;  // 2 endermans
                case OCULTA -> 3;                    // 3 endermans
                case ANTIGUA -> 4;                   // 4 endermans
            };
            
            int cantidadEndermites = switch (tipo) {
                case NORMAL -> 2 + random.nextInt(2);        // 2-3 endermites
                case INESTABLE -> 3 + random.nextInt(3);     // 3-5 endermites
                case ECO_BRASAS, ECO_SOMBRAS, ECO_PIEDRA -> 4 + random.nextInt(3);  // 4-6 endermites
                case OCULTA -> 5 + random.nextInt(4);        // 5-8 endermites
                case ANTIGUA -> 6 + random.nextInt(5);       // 6-10 endermites
            };
            
            // Spawn inmediato de guardianes
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!anomaliasActivas.containsKey(ubicacion)) return;
                
                java.util.List<Enderman> endermansSpawneados = new java.util.ArrayList<>();
                
                // Spawn múltiples Endermans según tipo
                for (int e = 0; e < cantidadEndermans; e++) {
                    Location endermanLoc = ubicacion.clone().add(
                        (random.nextDouble() - 0.5) * 4,
                        0.5,
                        (random.nextDouble() - 0.5) * 4
                    );
                    
                    Enderman enderman = (Enderman) world.spawnEntity(endermanLoc, EntityType.ENDERMAN);
                    
                    // Nombres según tipo de anomalía
                    String nombreEnderman = switch (tipo) {
                        case ANTIGUA -> "§5§l§k|§r §5§lGuardián Ancestral§r §5§l§k|";
                        case OCULTA -> "§d§lCustodio Oculto";
                        case INESTABLE -> "§e§lCentinela Inestable";
                        case ECO_BRASAS -> "§c§lGuardián de Brasas";
                        case ECO_SOMBRAS -> "§8§lGuardián de Sombras";
                        case ECO_PIEDRA -> "§7§lGuardián de Piedra";
                        default -> "§5§lGuardián del Vacío";
                    };
                    
                    enderman.setCustomName(nombreEnderman);
                    enderman.setCustomNameVisible(true);
                    enderman.setRemoveWhenFarAway(false);
                    enderman.getPersistentDataContainer().set(
                        new org.bukkit.NamespacedKey(plugin, "anomalia_guardian"),
                        org.bukkit.persistence.PersistentDataType.BYTE,
                        (byte) 1
                    );
                    
                    // Más vida y daño para anomalías poderosas
                    if (tipo == TipoAnomalia.ANTIGUA) {
                        enderman.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(80.0);
                        enderman.setHealth(80.0);
                    } else if (tipo == TipoAnomalia.OCULTA) {
                        enderman.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(60.0);
                        enderman.setHealth(60.0);
                    }
                    
                    endermansSpawneados.add(enderman);
                }
                
                // Spawn Endermites acompañantes
                for (int i = 0; i < cantidadEndermites; i++) {
                    Location endermiteLoc = ubicacion.clone().add(
                        (random.nextDouble() - 0.5) * 5,
                        0.5,
                        (random.nextDouble() - 0.5) * 5
                    );
                    org.bukkit.entity.Endermite endermite = (org.bukkit.entity.Endermite) world.spawnEntity(
                        endermiteLoc,
                        EntityType.ENDERMITE
                    );
                    
                    String nombreEndermite = switch (tipo) {
                        case ANTIGUA -> "§5§oParásito Ancestral";
                        case OCULTA -> "§d§oParásito Oculto";
                        default -> "§7§oParásito del Vacío";
                    };
                    
                    endermite.setCustomName(nombreEndermite);
                    endermite.setCustomNameVisible(true);
                    endermite.getPersistentDataContainer().set(
                        new org.bukkit.NamespacedKey(plugin, "anomalia_endermite"),
                        org.bukkit.persistence.PersistentDataType.BYTE,
                        (byte) 1
                    );
                }
                
                // Mensaje según tipo
                if (tipo == TipoAnomalia.INESTABLE || tipo == TipoAnomalia.OCULTA || tipo == TipoAnomalia.ANTIGUA) {
                    messageBus.broadcast("§c§l⚠ Los guardianes despiertan cerca de la anomalía...", "guardianes");
                }
                
                // Metadata para tracking de bonus
                final long tiempoSpawn = System.currentTimeMillis();
                
                // Despawn automático en 45 segundos (más tiempo para anomalías difíciles)
                int tiempoDespawn = tipo == TipoAnomalia.ANTIGUA ? 900 : (tipo == TipoAnomalia.OCULTA ? 700 : 600);
                BukkitTask despawnTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    for (Enderman end : endermansSpawneados) {
                        if (!end.isDead()) {
                            end.getWorld().spawnParticle(Particle.PORTAL, end.getLocation(), 50, 0.5, 1, 0.5, 0.5);
                            end.getWorld().playSound(end.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.7f);
                            end.remove();
                        }
                    }
                }, tiempoDespawn);
                
                // Listener de muerte para bonus (primer enderman)
                if (!endermansSpawneados.isEmpty()) {
                    Enderman endermanPrincipal = endermansSpawneados.get(0);
                    plugin.getServer().getScheduler().runTaskTimer(plugin, new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (endermanPrincipal.isDead()) {
                                long tiempoMuerte = System.currentTimeMillis();
                                long tiempoTranscurrido = (tiempoMuerte - tiempoSpawn) / 1000;
                                
                                Player asesino = endermanPrincipal.getKiller();
                                if (asesino != null) {
                                    if (tiempoTranscurrido <= 20) {
                                        // Bonus por rapidez
                                        asesino.sendMessage("§a§l✓ BONUS DE VELOCIDAD");
                                        asesino.sendMessage("§7Derrotado en §e" + tiempoTranscurrido + "s §7- Fragmentos adicionales");
                                        
                                        ItemStack fragmentoBonus = items.crearFragmentoDelVacio();
                                        fragmentoBonus.setAmount(tipo == TipoAnomalia.ANTIGUA ? 3 : 2);
                                        asesino.getInventory().addItem(fragmentoBonus);
                                        
                                        asesino.playSound(asesino.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
                                    } else {
                                        // Recompensa base
                                        asesino.sendMessage("§7Guardián derrotado - Fragmentos obtenidos");
                                        asesino.getInventory().addItem(items.crearFragmentoDelVacio());
                                    }
                                }
                                
                                despawnTask.cancel();
                                this.cancel();
                            } else if (!anomaliasActivas.containsKey(ubicacion)) {
                                // Anomalía desaparecida, cancelar
                                despawnTask.cancel();
                                this.cancel();
                            }
                        }
                    }, 10L, 10L);
                }
            });
        }
        
        // Mensajes especiales para ecos
        if (tipo.esEco()) {
            // Mensaje del Observador para anomalías de eco (25% probabilidad)
            if (random.nextInt(4) == 0 && tipo.mensajeObservador != null) {
                messageBus.broadcast("§5§l⚡ EL OBSERVADOR:", "observador");
                messageBus.broadcast(tipo.mensajeObservador, "anomalia_eco");
            }
        } else if (tipo == TipoAnomalia.OCULTA) {
            // Las anomalías ocultas no se anuncian - son secretas
            // Solo un susurro muy bajo
            if (random.nextInt(5) == 0) {
                messageBus.broadcast("§8§o\"...algo se oculta...\"", "anomalia_oculta");
            }
        }
        
        // Iniciar efectos visuales
        iniciarEfectosVisualesAnomalia(ubicacion, tipo);
        
        // Iniciar efectos de sonido
        iniciarEfectosSonidosAnomalia(ubicacion, tipo);
        
        plugin.getLogger().info("[CaminoEndEvent] Anomalía " + tipo.name() + " spawneada en: " + 
            ubicacion.getBlockX() + ", " + ubicacion.getBlockY() + ", " + ubicacion.getBlockZ());
    }
    
    /**
     * Genera estructura de bloques del End alrededor de la anomalía
     */
    private void generarEstructuraEnd(Location centro, TipoAnomalia tipo) {
        World world = centro.getWorld();
        if (world == null) return;
        
        int baseX = centro.getBlockX();
        int baseY = centro.getBlockY();
        int baseZ = centro.getBlockZ();
        
        // Limpiar área central (espacio para efectos)
        for (int y = 0; y <= 3; y++) {
            world.getBlockAt(baseX, baseY + y, baseZ).setType(Material.AIR);
        }
        
        // ════════════════════════════════════════════════════════════
        // ESTRUCTURAS ÉPICAS SEGÚN TIPO DE ANOMALÍA
        // ════════════════════════════════════════════════════════════
        
        if (tipo == TipoAnomalia.ANTIGUA) {
            // ═══ ANOMALÍA ANTIGUA: Templo Ancestral Mini (7x7) ═══
            // Plataforma épica de End Stone Bricks
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    Location loc = centro.clone().add(x, -1, z);
                    if (!loc.getBlock().getType().isSolid()) {
                        // Patrón alternado de End Stone Bricks y Purpur
                        if ((x + z) % 2 == 0) {
                            loc.getBlock().setType(Material.END_STONE_BRICKS);
                        } else {
                            loc.getBlock().setType(Material.PURPUR_BLOCK);
                        }
                    }
                }
            }
            
            // 4 Pilares monumentales en las esquinas (altura 6-8)
            construirPilarMonumental(world, baseX - 3, baseY, baseZ - 3, 8, true);  // NO
            construirPilarMonumental(world, baseX + 3, baseY, baseZ - 3, 7, true);  // NE
            construirPilarMonumental(world, baseX - 3, baseY, baseZ + 3, 6, true);  // SO
            construirPilarMonumental(world, baseX + 3, baseY, baseZ + 3, 7, true);  // SE
            
            // Arco fragmentado en el centro (2 columnas + dintel)
            world.getBlockAt(baseX - 1, baseY, baseZ).setType(Material.PURPUR_PILLAR);
            world.getBlockAt(baseX - 1, baseY + 1, baseZ).setType(Material.PURPUR_PILLAR);
            world.getBlockAt(baseX - 1, baseY + 2, baseZ).setType(Material.PURPUR_PILLAR);
            world.getBlockAt(baseX + 1, baseY, baseZ).setType(Material.PURPUR_PILLAR);
            world.getBlockAt(baseX + 1, baseY + 1, baseZ).setType(Material.PURPUR_PILLAR);
            world.getBlockAt(baseX + 1, baseY + 2, baseZ).setType(Material.PURPUR_PILLAR);
            world.getBlockAt(baseX, baseY + 3, baseZ).setType(Material.PURPUR_SLAB);
            
            // End Rods místicos flotantes
            world.getBlockAt(baseX, baseY + 4, baseZ).setType(Material.END_ROD);
            world.getBlockAt(baseX - 2, baseY + 1, baseZ - 2).setType(Material.END_ROD);
            world.getBlockAt(baseX + 2, baseY + 1, baseZ + 2).setType(Material.END_ROD);
            
            // Bloques flotantes místicos
            world.getBlockAt(baseX - 1, baseY + 5, baseZ - 1).setType(Material.PURPUR_BLOCK);
            world.getBlockAt(baseX + 1, baseY + 5, baseZ + 1).setType(Material.PURPUR_BLOCK);
            
        } else if (tipo == TipoAnomalia.OCULTA) {
            // ═══ ANOMALÍA OCULTA: Cámara Secreta Mini (5x5) ═══
            // Plataforma de End Stone con patrón oculto
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Location loc = centro.clone().add(x, -1, z);
                    if (!loc.getBlock().getType().isSolid()) {
                        loc.getBlock().setType(Material.END_STONE);
                    }
                }
            }
            
            // Pilares bajos y discretos en esquinas (altura 3)
            construirPilarMonumental(world, baseX - 2, baseY, baseZ - 2, 3, false);
            construirPilarMonumental(world, baseX + 2, baseY, baseZ - 2, 3, false);
            construirPilarMonumental(world, baseX - 2, baseY, baseZ + 2, 3, false);
            construirPilarMonumental(world, baseX + 2, baseY, baseZ + 2, 3, false);
            
            // End Rods sutiles
            world.getBlockAt(baseX, baseY + 2, baseZ).setType(Material.END_ROD);
            world.getBlockAt(baseX - 1, baseY, baseZ - 1).setType(Material.PURPUR_BLOCK);
            world.getBlockAt(baseX + 1, baseY, baseZ + 1).setType(Material.PURPUR_BLOCK);
            
        } else if (tipo == TipoAnomalia.INESTABLE) {
            // ═══ ANOMALÍA INESTABLE: Fragmentos Caóticos (5x5 irregular) ═══
            // Plataforma fragmentada e irregular
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Location loc = centro.clone().add(x, -1, z);
                    if (!loc.getBlock().getType().isSolid() && random.nextDouble() < 0.7) {
                        // 70% de probabilidad de colocar bloque (irregular)
                        loc.getBlock().setType(Material.END_STONE);
                    }
                }
            }
            
            // Pilares de alturas variables e irregulares (2-5)
            construirPilarMonumental(world, baseX - 2, baseY, baseZ - 2, 2 + random.nextInt(3), false);
            construirPilarMonumental(world, baseX + 2, baseY, baseZ - 2, 3 + random.nextInt(3), false);
            construirPilarMonumental(world, baseX - 2, baseY, baseZ + 2, 2 + random.nextInt(4), false);
            construirPilarMonumental(world, baseX + 2, baseY, baseZ + 2, 4 + random.nextInt(2), false);
            
            // Bloques flotantes caóticos
            world.getBlockAt(baseX, baseY + 3, baseZ).setType(Material.PURPUR_BLOCK);
            world.getBlockAt(baseX - 1, baseY + 2, baseZ + 1).setType(Material.END_STONE);
            world.getBlockAt(baseX + 1, baseY + 4, baseZ - 1).setType(Material.END_STONE_BRICKS);
            
            // End Rods en posiciones extrañas
            world.getBlockAt(baseX + 1, baseY + 1, baseZ).setType(Material.END_ROD);
            world.getBlockAt(baseX - 1, baseY + 2, baseZ - 1).setType(Material.END_ROD);
            
        } else if (tipo == TipoAnomalia.ECO_BRASAS) {
            // ═══ ECO DE BRASAS: Altar de Fuego (5x5) ═══
            // Plataforma de End Stone con patrón de fuego
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Location loc = centro.clone().add(x, -1, z);
                    if (!loc.getBlock().getType().isSolid()) {
                        loc.getBlock().setType(Material.END_STONE);
                    }
                }
            }
            
            // Pilares con decoración de Purpur (altura 4)
            construirPilarMonumental(world, baseX - 2, baseY, baseZ - 2, 4, false);
            construirPilarMonumental(world, baseX + 2, baseY, baseZ - 2, 4, false);
            construirPilarMonumental(world, baseX - 2, baseY, baseZ + 2, 4, false);
            construirPilarMonumental(world, baseX + 2, baseY, baseZ + 2, 4, false);
            
            // Tazón de fuego central
            world.getBlockAt(baseX, baseY, baseZ).setType(Material.NETHERRACK);
            world.getBlockAt(baseX, baseY + 1, baseZ).setType(Material.FIRE);
            
            // Decoración de Purpur alrededor
            world.getBlockAt(baseX - 1, baseY, baseZ).setType(Material.PURPUR_SLAB);
            world.getBlockAt(baseX + 1, baseY, baseZ).setType(Material.PURPUR_SLAB);
            world.getBlockAt(baseX, baseY, baseZ - 1).setType(Material.PURPUR_SLAB);
            world.getBlockAt(baseX, baseY, baseZ + 1).setType(Material.PURPUR_SLAB);
            
        } else if (tipo == TipoAnomalia.ECO_SOMBRAS) {
            // ═══ ECO DE SOMBRAS: Cámara Oscura (5x5) ═══
            // Plataforma de End Stone Bricks oscuro
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Location loc = centro.clone().add(x, -1, z);
                    if (!loc.getBlock().getType().isSolid()) {
                        loc.getBlock().setType(Material.END_STONE_BRICKS);
                    }
                }
            }
            
            // Pilares oscuros (altura 3-4)
            construirPilarMonumental(world, baseX - 2, baseY, baseZ - 2, 4, false);
            construirPilarMonumental(world, baseX + 2, baseY, baseZ - 2, 3, false);
            construirPilarMonumental(world, baseX - 2, baseY, baseZ + 2, 3, false);
            construirPilarMonumental(world, baseX + 2, baseY, baseZ + 2, 4, false);
            
            // Altar central oscuro
            world.getBlockAt(baseX, baseY, baseZ).setType(Material.PURPUR_BLOCK);
            world.getBlockAt(baseX, baseY + 1, baseZ).setType(Material.END_ROD);
            
        } else if (tipo == TipoAnomalia.ECO_PIEDRA) {
            // ═══ ECO DE PIEDRA: Ruinas Fragmentadas (5x5) ═══
            // Plataforma parcialmente destruida
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Location loc = centro.clone().add(x, -1, z);
                    if (!loc.getBlock().getType().isSolid() && random.nextDouble() < 0.8) {
                        // Mezcla de bloques rotos
                        int rand = random.nextInt(3);
                        if (rand == 0) {
                            loc.getBlock().setType(Material.END_STONE_BRICKS);
                        } else if (rand == 1) {
                            loc.getBlock().setType(Material.CRACKED_STONE_BRICKS);
                        } else {
                            loc.getBlock().setType(Material.END_STONE);
                        }
                    }
                }
            }
            
            // Pilares parcialmente destruidos (alturas variables 1-3)
            construirPilarMonumental(world, baseX - 2, baseY, baseZ - 2, 1 + random.nextInt(3), false);
            construirPilarMonumental(world, baseX + 2, baseY, baseZ - 2, 2 + random.nextInt(2), false);
            construirPilarMonumental(world, baseX - 2, baseY, baseZ + 2, 1 + random.nextInt(3), false);
            construirPilarMonumental(world, baseX + 2, baseY, baseZ + 2, 2 + random.nextInt(2), false);
            
            // Bloques caídos alrededor
            world.getBlockAt(baseX - 1, baseY, baseZ - 1).setType(Material.CRACKED_STONE_BRICKS);
            world.getBlockAt(baseX + 1, baseY, baseZ + 1).setType(Material.CRACKED_STONE_BRICKS);
            
        } else {
            // ═══ ANOMALÍA NORMAL: Estructura Estándar (5x5) ═══
            // Plataforma de End Stone estándar
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Location loc = centro.clone().add(x, -1, z);
                    if (!loc.getBlock().getType().isSolid()) {
                        loc.getBlock().setType(Material.END_STONE);
                    }
                }
            }
            
            // 4 Pilares simples en esquinas (altura 3)
            construirPilarMonumental(world, baseX - 2, baseY, baseZ - 2, 3, false);
            construirPilarMonumental(world, baseX + 2, baseY, baseZ - 2, 3, false);
            construirPilarMonumental(world, baseX - 2, baseY, baseZ + 2, 3, false);
            construirPilarMonumental(world, baseX + 2, baseY, baseZ + 2, 3, false);
            
            // End Rod central
            world.getBlockAt(baseX, baseY + 2, baseZ).setType(Material.END_ROD);
        }
        
        plugin.getLogger().info("[CaminoEnd] Estructura épica generada para anomalía " + tipo.name());
    }
    
    /**
     * Construye un pilar monumental con decoración
     * @param elaborado Si es true, añade coronas y decoración extra
     */
    private void construirPilarMonumental(World world, int x, int y, int z, int altura, boolean elaborado) {
        // Pilar principal
        for (int i = 0; i < altura; i++) {
            Material mat = (i % 2 == 0) ? Material.PURPUR_PILLAR : Material.END_STONE_BRICKS;
            world.getBlockAt(x, y + i, z).setType(mat);
        }
        
        // Si es elaborado, añadir decoración
        if (elaborado && altura >= 4) {
            // Corona del pilar
            world.getBlockAt(x, y + altura, z).setType(Material.PURPUR_BLOCK);
            world.getBlockAt(x, y + altura + 1, z).setType(Material.PURPUR_SLAB);
            
            // Decoración lateral (brazos)
            if (altura >= 6) {
                world.getBlockAt(x + 1, y + altura - 2, z).setType(Material.PURPUR_SLAB);
                world.getBlockAt(x - 1, y + altura - 2, z).setType(Material.PURPUR_SLAB);
                world.getBlockAt(x, y + altura - 2, z + 1).setType(Material.PURPUR_SLAB);
                world.getBlockAt(x, y + altura - 2, z - 1).setType(Material.PURPUR_SLAB);
            }
        }
    }
    
    private void iniciarEfectosVisualesAnomalia(Location ubicacion, TipoAnomalia tipo) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!anomaliasActivas.containsKey(ubicacion)) {
                return;
            }
            
            World world = ubicacion.getWorld();
            if (world == null) return;
            
            // Haz de luz vertical (visible desde lejos) - NO para anomalías ocultas
            if (ticksTotales % 5 == 0 && tipo != TipoAnomalia.OCULTA) { // Cada 5 ticks
                for (int altura = 0; altura < 30; altura += 2) {
                    Location hazLoc = ubicacion.clone().add(0, altura, 0);
                    Particle particleHaz;
                    if (tipo == TipoAnomalia.ANTIGUA) {
                        particleHaz = Particle.DRAGON_BREATH;
                    } else if (tipo == TipoAnomalia.INESTABLE) {
                        particleHaz = Particle.SOUL_FIRE_FLAME;
                    } else if (tipo == TipoAnomalia.ECO_BRASAS) {
                        particleHaz = Particle.FLAME;
                    } else if (tipo == TipoAnomalia.ECO_SOMBRAS) {
                        particleHaz = Particle.SQUID_INK;
                    } else if (tipo == TipoAnomalia.ECO_PIEDRA) {
                        particleHaz = Particle.ASH;
                    } else {
                        particleHaz = Particle.END_ROD;
                    }
                    world.spawnParticle(particleHaz, hazLoc, 1, 0.1, 0, 0.1, 0);
                }
            }
            
            // Partículas en espiral (común a todas)
            for (int i = 0; i < 3; i++) {
                double offsetY = (ticksTotales % 40) * 0.1;
                double angulo = (ticksTotales + i * 120) * 0.1;
                double radio = 0.5;
                
                double offsetX = radio * Math.cos(angulo);
                double offsetZ = radio * Math.sin(angulo);
                
                Location particleLoc = ubicacion.clone().add(offsetX, offsetY, offsetZ);
                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
            }
            
            // Partículas específicas del tipo
            if (tipo == TipoAnomalia.NORMAL) {
                world.spawnParticle(Particle.PORTAL, ubicacion, 5, 0.3, 0.3, 0.3, 0.01);
                
            } else if (tipo == TipoAnomalia.INESTABLE) {
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, ubicacion, 8, 0.3, 0.5, 0.3, 0.02);
                world.spawnParticle(Particle.PORTAL, ubicacion, 3, 0.3, 0.3, 0.3, 0.01);
                
                // Ocasionalmente explota en partículas
                if (random.nextInt(50) == 0) {
                    world.spawnParticle(Particle.EXPLOSION, ubicacion.clone().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                }
                
            } else if (tipo == TipoAnomalia.ECO_BRASAS) {
                world.spawnParticle(Particle.FLAME, ubicacion, 10, 0.3, 0.5, 0.3, 0.02);
                world.spawnParticle(Particle.LAVA, ubicacion.clone().add(0, 0.5, 0), 2, 0.2, 0.2, 0.2, 0);
                world.spawnParticle(Particle.PORTAL, ubicacion, 3, 0.3, 0.3, 0.3, 0.01);
                
            } else if (tipo == TipoAnomalia.ECO_SOMBRAS) {
                world.spawnParticle(Particle.SQUID_INK, ubicacion, 8, 0.4, 0.5, 0.4, 0.01);
                world.spawnParticle(Particle.SMOKE, ubicacion, 5, 0.3, 0.5, 0.3, 0.01);
                world.spawnParticle(Particle.PORTAL, ubicacion, 3, 0.3, 0.3, 0.3, 0.01);
                
            } else if (tipo == TipoAnomalia.ECO_PIEDRA) {
                world.spawnParticle(Particle.ASH, ubicacion, 10, 0.4, 0.5, 0.4, 0.01);
                world.spawnParticle(Particle.CLOUD, ubicacion, 3, 0.2, 0.3, 0.2, 0);
                world.spawnParticle(Particle.PORTAL, ubicacion, 3, 0.3, 0.3, 0.3, 0.01);
                
            } else if (tipo == TipoAnomalia.OCULTA) {
                // Anomalía oculta: partículas MUY sutiles, casi invisibles
                if (random.nextInt(3) == 0) { // Solo 33% del tiempo
                    world.spawnParticle(Particle.END_ROD, ubicacion, 1, 0.1, 0.1, 0.1, 0.01);
                }
                // Sin haz de luz para hacerla difícil de ver
                
            } else if (tipo == TipoAnomalia.ANTIGUA) {
                world.spawnParticle(Particle.DRAGON_BREATH, ubicacion, 12, 0.4, 0.7, 0.4, 0.01);
                world.spawnParticle(Particle.PORTAL, ubicacion, 2, 0.3, 0.3, 0.3, 0.01);
                world.spawnParticle(Particle.ENCHANTED_HIT, ubicacion.clone().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.5);
                
                // Aura continua
                world.spawnParticle(Particle.SCULK_SOUL, ubicacion, 3, 0.6, 0.6, 0.6, 0.01);
            }
            
        }, 0L, 2L); // Cada 2 ticks
        
        anomaliaParticleTasks.put(ubicacion, task);
    }
    
    private void iniciarEfectosSonidosAnomalia(Location ubicacion, TipoAnomalia tipo) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!anomaliasActivas.containsKey(ubicacion)) {
                return;
            }
            
            World world = ubicacion.getWorld();
            if (world == null) return;
            
            // Sonido específico del tipo de anomalía
            float volumen = tipo == TipoAnomalia.ANTIGUA ? 0.4f : (tipo == TipoAnomalia.INESTABLE ? 0.3f : 0.2f);
            world.playSound(ubicacion, tipo.sonido, volumen, tipo == TipoAnomalia.ANTIGUA ? 0.8f : 1.0f);
            
            // Capas comunes
            if (tipo != TipoAnomalia.ANTIGUA) {
                world.playSound(ubicacion, Sound.BLOCK_PORTAL_AMBIENT, 0.2f, 1.2f);
            }
            
            world.playSound(ubicacion, Sound.AMBIENT_CAVE, 0.15f, tipo == TipoAnomalia.ANTIGUA ? 0.3f : 0.5f);
            world.playSound(ubicacion, Sound.BLOCK_BEACON_AMBIENT, 0.25f, tipo == TipoAnomalia.INESTABLE ? 0.9f : 0.7f);
            
            // Latido dimensional (más frecuente en anomalías antiguas)
            int chance = tipo == TipoAnomalia.ANTIGUA ? 1 : 3; // 100% vs 33%
            if (random.nextInt(chance + 1) == 0) {
                world.playSound(ubicacion, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.3f, 0.6f);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (anomaliasActivas.containsKey(ubicacion)) {
                        world.playSound(ubicacion, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.3f, 0.6f);
                    }
                }, 10L);
            }
            
        }, 0L, 100L); // Cada 5 segundos
        
        anomaliaSoundTasks.put(ubicacion, task);
    }
    
    private void actualizarAnomalias() {
        // Verificar timeout de anomalías (despawn después de 3 minutos sin interacción)
        long tiempoActual = System.currentTimeMillis();
        List<Location> aDespawnear = new ArrayList<>();
        
        for (Map.Entry<Location, AnomaliaData> entry : anomaliasActivas.entrySet()) {
            long tiempoVida = tiempoActual - entry.getValue().tiempoSpawn;
            
            // Advertencia 30 segundos antes de desaparecer
            if (tiempoVida > 150000 && tiempoVida < 151000) { // Entre 2:30 y 2:31
                Location loc = entry.getKey();
                World world = loc.getWorld();
                if (world != null) {
                    world.spawnParticle(Particle.SMOKE, loc.clone().add(0, 2, 0), 30, 0.5, 1, 0.5, 0.05);
                    world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 0.5f);
                }
            }
            
            if (tiempoVida > 180000) { // 3 minutos
                aDespawnear.add(entry.getKey());
            }
        }
        
        for (Location loc : aDespawnear) {
            despawnearAnomalia(loc);
        }
    }
    
    private void despawnearAnomalia(Location ubicacion) {
        anomaliasActivas.remove(ubicacion);
        
        // Limpiar tracking de visitas para esta anomalía
        for (Set<Location> visitadas : anomaliasVisitadasPorJugador.values()) {
            visitadas.remove(ubicacion);
        }
        
        BukkitTask particleTask = anomaliaParticleTasks.remove(ubicacion);
        if (particleTask != null) particleTask.cancel();
        
        BukkitTask soundTask = anomaliaSoundTasks.remove(ubicacion);
        if (soundTask != null) soundTask.cancel();
        
        World world = ubicacion.getWorld();
        if (world != null) {
            world.spawnParticle(Particle.EXPLOSION, ubicacion, 1);
            world.playSound(ubicacion, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE FRAGMENTOS
    // ═══════════════════════════════════════════════════════════════════
    
    public void onJugadorRecolectaFragmento(Player jugador, Location anomaliaUbicacion, TipoAnomalia tipo) {
        // Solo en fase RESONANCIA o REVELACION
        if (faseActual != Fase.RESONANCIA && faseActual != Fase.REVELACION) {
            return;
        }
        
        // Incrementar contadores (con multiplicador según tipo)
        fragmentosRecolectadosGlobalmente++;
        fragmentosPorJugador.put(jugador.getUniqueId(), 
            fragmentosPorJugador.getOrDefault(jugador.getUniqueId(), 0) + 1);
        participantes.add(jugador.getUniqueId());
        
        // Reset contador de inactividad
        ticksSinFragmento = 0;
        
        // Dar item del fragmento
        ItemStack fragmento = items.crearFragmentoDelVacio();
        jugador.getInventory().addItem(fragmento);
        
        // Mensaje específico del tipo de anomalía
        if (tipo == TipoAnomalia.ANTIGUA) {
            jugador.sendMessage("§5§l⚡ EL OBSERVADOR:");
            jugador.sendMessage("§5§o\"...este fragmento... DIFERENTE... más viejo que el tiempo...\"");
            
            // Título y sonido especial
            jugador.sendTitle("§5§l⚡ FRAGMENTO ANCESTRAL ⚡", "§5§o...eco del pasado lejano...", 10, 60, 15);
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.7f);
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.0f);
            
        } else if (tipo == TipoAnomalia.INESTABLE) {
            jugador.sendMessage("§5§l⚡ EL OBSERVADOR:");
            jugador.sendMessage("§e§o\"...cuidado... este fragmento pulsa con poder...\"");
            
            // Título y sonido especial
            jugador.sendTitle("§e§l⚠ FRAGMENTO INESTABLE ⚠", "§e§o...energía caótica...", 10, 50, 10);
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.0f, 0.8f);
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 1.2f);
            
        } else {
            // Mensajes progresivos del Observador según cantidad de fragmentos (solo para normales)
            jugador.sendMessage("§5§l⚡ EL OBSERVADOR:");
            String mensajeObservador = obtenerMensajeProgresivo(fragmentosRecolectadosGlobalmente);
            jugador.sendMessage(mensajeObservador);
        }
        
        // Efectos visuales personalizados para el jugador
        aplicarEfectosVisualesRecoleccion(jugador, anomaliaUbicacion);
        
        // Sonidos progresivos según fase emocional
        aplicarSonidosProgresivos(jugador, fragmentosRecolectadosGlobalmente);
        
        // Título si es un hito importante (cada 10 fragmentos)
        if (fragmentosRecolectadosGlobalmente % 10 == 0) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.sendTitle(
                    "§5§l⚡ §e" + fragmentosRecolectadosGlobalmente + "§7/§40 FRAGMENTOS",
                    "§7El camino se hace más claro...",
                    5, 40, 10
                );
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.2f);
            }
        }
        
        // Hitos épicos especiales
        aplicarHitosEspeciales(fragmentosRecolectadosGlobalmente);
        
        // Ofrecer desafío "Caza de Anomalías" a los 15 fragmentos
        if (fragmentosRecolectadosGlobalmente == 15 && !desafioCazaOfrecido) {
            ofrecerDesafioCaza();
            desafioCazaOfrecido = true;
        }
        
        // Si el desafío está activo, contar la anomalía
        if (desafioCazaActivo) {
            anomaliasEncontradasDesafio++;
            participantesDesafio.add(jugador.getUniqueId());
            
            // Feedback inmediato
            jugador.sendTitle(
                "§a§l✓ ANOMALÍA ENCONTRADA",
                "§7" + anomaliasEncontradasDesafio + "/" + ANOMALIAS_REQUERIDAS_DESAFIO,
                5, 30, 10
            );
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
        }
        
        // Sistema de tensión: Countdown cuando quedan 5 fragmentos
        int fragmentosFaltantes = FRAGMENTOS_OBJETIVO - fragmentosRecolectadosGlobalmente;
        if (fragmentosFaltantes <= 5 && fragmentosFaltantes > 0) {
            aplicarCountdownTension(fragmentosFaltantes);
        }
        
        // Enviar mensaje narrativo dinámico
        listener.enviarMensajeProgreso(fragmentosRecolectadosGlobalmente);
        
        // Enviar mensaje al encontrar anomalía rara
        if (tipo == TipoAnomalia.ANTIGUA || tipo == TipoAnomalia.OCULTA || tipo.esEco()) {
            listener.enviarMensajeAnomaliaRara(tipo);
        }
        
        // Anuncio global de progreso (con indicador de tipo raro)
        int progreso = (fragmentosRecolectadosGlobalmente * 100) / FRAGMENTOS_OBJETIVO;
        String indicadorTipo = tipo == TipoAnomalia.ANTIGUA ? " §5§l[ANTIGUA]" : 
                               (tipo == TipoAnomalia.INESTABLE ? " §e[INESTABLE]" : "");
        
        messageBus.broadcast("§7[§5✦§7] §fFragmento recolectado" + indicadorTipo + ": §e" + fragmentosRecolectadosGlobalmente + 
            "§7/§e" + FRAGMENTOS_OBJETIVO + " §7(§a" + progreso + "%§7)", "fragmento");
        
        // Sistema de "ecos" - otros jugadores ven dirección del recolector
        notificarEcoRecoleccion(jugador, anomaliaUbicacion);
        
        // Despawnear anomalía (fragmento solo se obtiene 1 vez por anomalía)
        despawnearAnomalia(anomaliaUbicacion);
        
        plugin.getLogger().info("[CaminoEndEvent] Fragmento " + tipo.name() + " recolectado por " + jugador.getName() + 
            " - Total: " + fragmentosRecolectadosGlobalmente);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE PORTAL INCOMPLETO
    // ═══════════════════════════════════════════════════════════════════
    
    private void generarPortalIncompleto() {
        if (portalGenerado) return;
        
        // Obtener ubicación desde config o generar aleatoria
        ConfigurationSection portalConfig = config.getConfigurationSection("portal");
        if (portalConfig != null && portalConfig.contains("ubicacion")) {
            String worldName = portalConfig.getString("ubicacion.world", "world");
            int x = portalConfig.getInt("ubicacion.x", 0);
            int y = portalConfig.getInt("ubicacion.y", 64);
            int z = portalConfig.getInt("ubicacion.z", 0);
            
            World world = plugin.getServer().getWorld(worldName);
            if (world != null) {
                portalLocation = new Location(world, x, y, z);
            }
        }
        
        // Si no hay ubicación configurada, usar spawn del mundo
        if (portalLocation == null) {
            World world = plugin.getServer().getWorlds().get(0);
            // Buscar ubicación en el suelo cercana al spawn
            Location spawn = world.getSpawnLocation();
            portalLocation = encontrarSueloSeguro(spawn);
        }
        
        // Construir estructura del portal incompleto
        construirPortalIncompleto(portalLocation);
        
        // Notificar ubicación del portal a todos los jugadores
        notificarUbicacionPortal(portalLocation);
        
        portalGenerado = true;
        
        plugin.getLogger().info("[CaminoEndEvent] Portal incompleto generado en: " + 
            portalLocation.getBlockX() + ", " + portalLocation.getBlockY() + ", " + portalLocation.getBlockZ());
    }
    
    /**
     * Busca una ubicación segura en el suelo cerca de la posición dada
     */
    private Location encontrarSueloSeguro(Location origen) {
        World world = origen.getWorld();
        if (world == null) return origen;
        
        int x = origen.getBlockX();
        int z = origen.getBlockZ();
        
        // Buscar desde Y alto hacia abajo hasta encontrar suelo sólido
        for (int y = world.getMaxHeight() - 1; y > world.getMinHeight(); y--) {
            Location loc = new Location(world, x, y, z);
            if (loc.getBlock().getType().isSolid() && 
                loc.clone().add(0, 1, 0).getBlock().getType().isAir() &&
                loc.clone().add(0, 2, 0).getBlock().getType().isAir()) {
                return loc.clone().add(0, 1, 0); // Retornar 1 bloque arriba del suelo
            }
        }
        
        return origen.clone().add(0, -origen.getY() + 70, 0); // Fallback a Y=70
    }
    
    private void construirPortalIncompleto(Location centro) {
        World world = centro.getWorld();
        if (world == null) return;
        
        int baseX = centro.getBlockX();
        int baseY = centro.getBlockY();
        int baseZ = centro.getBlockZ();
        
        // ════════════════════════════════════════════════════════════
        // PLATAFORMA ÉPICA BASE (11x11)
        // ════════════════════════════════════════════════════════════
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                // Capa base
                world.getBlockAt(baseX + x, baseY - 1, baseZ + z).setType(Material.END_STONE_BRICKS);
                
                // Borde decorativo con End Stone
                if (Math.abs(x) == 5 || Math.abs(z) == 5) {
                    world.getBlockAt(baseX + x, baseY, baseZ + z).setType(Material.END_STONE);
                }
                
                // Esquinas con Purpur Blocks
                if ((Math.abs(x) == 5 && Math.abs(z) == 5)) {
                    world.getBlockAt(baseX + x, baseY, baseZ + z).setType(Material.PURPUR_BLOCK);
                    world.getBlockAt(baseX + x, baseY + 1, baseZ + z).setType(Material.PURPUR_BLOCK);
                }
            }
        }
        
        // ════════════════════════════════════════════════════════════
        // MARCO DE PORTAL FRAGMENTADO (9x9) - DRAMÁTICAMENTE INCOMPLETO
        // ════════════════════════════════════════════════════════════
        
        // Lado NORTE (5 de 7 bloques) - Mayormente completo
        world.getBlockAt(baseX - 3, baseY, baseZ - 4).setType(Material.END_PORTAL_FRAME);
        world.getBlockAt(baseX - 2, baseY, baseZ - 4).setType(Material.END_PORTAL_FRAME);
        world.getBlockAt(baseX - 1, baseY, baseZ - 4).setType(Material.END_PORTAL_FRAME);
        // Hueco intencional en baseX, baseZ - 4
        world.getBlockAt(baseX + 1, baseY, baseZ - 4).setType(Material.END_PORTAL_FRAME);
        world.getBlockAt(baseX + 2, baseY, baseZ - 4).setType(Material.END_PORTAL_FRAME);
        // Hueco en baseX + 3, baseZ - 4
        
        // Lado ESTE (4 de 7 bloques) - Bastante dañado
        world.getBlockAt(baseX + 4, baseY, baseZ - 3).setType(Material.END_PORTAL_FRAME);
        // Hueco en baseX + 4, baseZ - 2
        world.getBlockAt(baseX + 4, baseY, baseZ - 1).setType(Material.END_PORTAL_FRAME);
        // Hueco en baseX + 4, baseZ (centro)
        world.getBlockAt(baseX + 4, baseY, baseZ + 1).setType(Material.END_PORTAL_FRAME);
        world.getBlockAt(baseX + 4, baseY, baseZ + 2).setType(Material.END_PORTAL_FRAME);
        // Hueco en baseX + 4, baseZ + 3
        
        // Lado SUR (2 de 7 bloques) - MUY INCOMPLETO (crítico)
        // Solo 2 bloques en los extremos
        world.getBlockAt(baseX - 3, baseY, baseZ + 4).setType(Material.END_PORTAL_FRAME);
        // Huecos masivos
        world.getBlockAt(baseX + 3, baseY, baseZ + 4).setType(Material.END_PORTAL_FRAME);
        
        // Lado OESTE (3 de 7 bloques) - Muy dañado
        // Hueco en baseX - 4, baseZ - 3
        world.getBlockAt(baseX - 4, baseY, baseZ - 2).setType(Material.END_PORTAL_FRAME);
        // Hueco en baseX - 4, baseZ - 1
        // Hueco en baseX - 4, baseZ (centro)
        world.getBlockAt(baseX - 4, baseY, baseZ + 1).setType(Material.END_PORTAL_FRAME);
        // Hueco en baseX - 4, baseZ + 2
        world.getBlockAt(baseX - 4, baseY, baseZ + 3).setType(Material.END_PORTAL_FRAME);
        
        // ════════════════════════════════════════════════════════════
        // PILARES FRAGMENTADOS EN LAS ESQUINAS
        // ════════════════════════════════════════════════════════════
        construirPilarFragmentado(world, baseX - 4, baseY, baseZ - 4, 5); // Noroeste
        construirPilarFragmentado(world, baseX + 4, baseY, baseZ - 4, 4); // Noreste
        construirPilarFragmentado(world, baseX - 4, baseY, baseZ + 4, 3); // Suroeste
        construirPilarFragmentado(world, baseX + 4, baseY, baseZ + 4, 6); // Sureste
        
        // ════════════════════════════════════════════════════════════
        // BLOQUES FLOTANTES "ROTOS" CERCA DEL PORTAL
        // ════════════════════════════════════════════════════════════
        // Simulan fragmentos del portal flotando cerca
        world.getBlockAt(baseX - 2, baseY + 2, baseZ - 3).setType(Material.END_STONE);
        world.getBlockAt(baseX + 3, baseY + 3, baseZ + 2).setType(Material.END_STONE);
        world.getBlockAt(baseX + 1, baseY + 1, baseZ - 4).setType(Material.PURPUR_BLOCK);
        world.getBlockAt(baseX - 3, baseY + 2, baseZ + 3).setType(Material.END_STONE_BRICKS);
        
        // ════════════════════════════════════════════════════════════
        // EFECTOS VISUALES PERMANENTES
        // ════════════════════════════════════════════════════════════
        iniciarEfectosPortal(centro);
        iniciarCorazonPortal(centro);
        
        plugin.getLogger().info("[CaminoEndEvent] Portal épico 9x9 construido - 14 de 28 frames (50% incompleto)");
    }
    
    /**
     * Construye un pilar decorativo fragmentado con alturas variables
     */
    private void construirPilarFragmentado(World world, int x, int y, int z, int altura) {
        for (int i = 1; i <= altura; i++) {
            // Alternar entre End Stone Bricks y Purpur
            Material mat = (i % 2 == 0) ? Material.PURPUR_PILLAR : Material.END_STONE_BRICKS;
            world.getBlockAt(x, y + i, z).setType(mat);
            
            // Algunos pilares tienen bloques rotos (huecos)
            if (altura >= 5 && i == altura - 1) {
                world.getBlockAt(x, y + i, z).setType(Material.AIR); // Hueco dramático
            }
        }
        
        // Corona del pilar (si es alto)
        if (altura >= 4) {
            world.getBlockAt(x, y + altura, z).setType(Material.PURPUR_BLOCK);
        }
    }
    
    private void iniciarEfectosPortal(Location centro) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            World world = centro.getWorld();
            if (world == null) return;
            
            // ════════════════════════════════════════════════════════════
            // PARTÍCULAS PORTAL MASIVAS EN EL CENTRO
            // ════════════════════════════════════════════════════════════
            world.spawnParticle(Particle.PORTAL, centro, 40, 3.5, 1.0, 3.5, 0.08);
            world.spawnParticle(Particle.REVERSE_PORTAL, centro, 25, 3.0, 0.8, 3.0, 0.05);
            
            // ════════════════════════════════════════════════════════════
            // ESPIRAL ASCENDENTE ÉPICA (END_ROD)
            // ════════════════════════════════════════════════════════════
            double radioEspiral = 4.0; // Radio más amplio
            for (int i = 0; i < 16; i++) { // Más puntos en la espiral
                double angulo = (ticksTotales + i * 22.5) * 0.05;
                double offsetX = radioEspiral * Math.cos(angulo);
                double offsetZ = radioEspiral * Math.sin(angulo);
                double offsetY = ((ticksTotales + i * 10) % 120) * 0.1; // Espiral más alta
                
                Location particleLoc = centro.clone().add(offsetX, offsetY, offsetZ);
                world.spawnParticle(Particle.END_ROD, particleLoc, 2, 0.1, 0.1, 0.1, 0);
            }
            
            // ════════════════════════════════════════════════════════════
            // ANILLO DE DRAGÓN GIRATORIO
            // ════════════════════════════════════════════════════════════
            double radioAnillo = 5.5;
            int puntosAnillo = 32;
            for (int i = 0; i < puntosAnillo; i++) {
                double angulo = (ticksTotales * 0.03) + (i * 2 * Math.PI / puntosAnillo);
                double offsetX = radioAnillo * Math.cos(angulo);
                double offsetZ = radioAnillo * Math.sin(angulo);
                
                Location anilloLoc = centro.clone().add(offsetX, 0.3, offsetZ);
                world.spawnParticle(Particle.DRAGON_BREATH, anilloLoc, 1, 0, 0, 0, 0);
            }
            
            // ════════════════════════════════════════════════════════════
            // RAYOS VERTICALES EN ESQUINAS
            // ════════════════════════════════════════════════════════════
            if (ticksTotales % 10 == 0) {
                for (int offset = -4; offset <= 4; offset += 8) {
                    for (int offsetZ = -4; offsetZ <= 4; offsetZ += 8) {
                        for (double y = 0; y < 6; y += 0.5) {
                            Location rayoLoc = centro.clone().add(offset, y, offsetZ);
                            world.spawnParticle(Particle.ENCHANT, rayoLoc, 1, 0.1, 0.1, 0.1, 0);
                        }
                    }
                }
            }
            
            // ════════════════════════════════════════════════════════════
            // PULSOS DE ENERGÍA OCASIONALES
            // ════════════════════════════════════════════════════════════
            if (ticksTotales % 80 == 0) {
                world.spawnParticle(Particle.EXPLOSION, centro, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.SOUL, centro, 50, 4.0, 2.0, 4.0, 0.1);
            }
            
        }, 0L, 2L);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ANUNCIOS Y MENSAJES
    // ═══════════════════════════════════════════════════════════════════
    
    private void anunciarInicio() {
        ConfigurationSection intro = config.getConfigurationSection("mensajes.inicio");
        if (intro == null) {
            messageBus.broadcast("§5§l⚡ EL CAMINO AL END", "evento_inicio");
            return;
        }
        
        // Título cinematográfico con fade largo
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.sendTitle(
                intro.getString("titulo", "§5§l⚡ EL CAMINO AL END"),
                intro.getString("subtitulo", "§7Anomalías dimensionales detectadas..."),
                20, 100, 30
            );
        }
        
        // Mensajes espaciados para lectura cómoda (3 segundos entre cada uno)
        List<String> mensajes = intro.getStringList("mensajes");
        for (int i = 0; i < mensajes.size(); i++) {
            final int index = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                messageBus.broadcast(mensajes.get(index), "inicio_" + index);
            }, 60L * (i + 1)); // 3 segundos entre mensajes
        }
        
        // Sonido
        soundUtil.playSoundAll(Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.5f, 0.8f);
    }
    
    private void anunciarFaseResonancia() {
        ConfigurationSection res = config.getConfigurationSection("mensajes.fase_resonancia");
        String observador = res != null ? res.getString("observador", "§5§l⚡ EL OBSERVADOR:") : "§5§l⚡ EL OBSERVADOR:";
        String msg1 = res != null ? res.getString("mensaje", "§7§o\"Las barreras... se debilitan...\"") : "§7§o\"Las barreras... se debilitan...\"";
        String msg2 = res != null ? res.getString("mensaje2", "§7§o\"Ahora... puedo sentir los fragmentos...\"") : "§7§o\"Ahora... puedo sentir los fragmentos...\"";
        java.util.List<String> extras = res != null ? res.getStringList("mensajes_extra") : java.util.Collections.emptyList();

        messageBus.broadcast(observador, "observador");
        messageBus.broadcast(msg1, "fase_resonancia");
        for (String extra : extras) {
            messageBus.broadcast(extra, "fase_resonancia_extra");
        }
        messageBus.broadcast(msg2, "fase_resonancia2");

        String titulo = res != null ? res.getString("titulo", "§d§l⚡ FASE II: RESONANCIA ⚡") : "§d§l⚡ FASE II: RESONANCIA ⚡";
        String subtitulo = res != null ? res.getString("subtitulo", "§7§oRecolecten los fragmentos del vacío") : "§7§oRecolecten los fragmentos del vacío";
        String soundName = res != null ? res.getString("sound", "BLOCK_SCULK_SENSOR_CLICKING") : "BLOCK_SCULK_SENSOR_CLICKING";
        float vol = res != null ? (float) res.getDouble("volumen", 0.5) : 0.5f;
        float pitch = res != null ? (float) res.getDouble("pitch", 1.0) : 1.0f;
        Sound soundTemp;
        try { soundTemp = Sound.valueOf(soundName); } catch (Exception e) { soundTemp = Sound.BLOCK_SCULK_SENSOR_CLICKING; }
        final Sound s = soundTemp;

        // Título y sonido para todos
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.sendTitle(titulo, subtitulo, 10, 70, 20);
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
            p.playSound(p.getLocation(), s, vol, pitch);
        }
    }
    
    private void anunciarFaseRevelacion() {
        ConfigurationSection rev = config.getConfigurationSection("mensajes.fase_revelacion");
        String observador = rev != null ? rev.getString("observador", "§5§l⚡ EL OBSERVADOR:") : "§5§l⚡ EL OBSERVADOR:";
        String mensaje = rev != null ? rev.getString("mensaje", "§7§o\"Suficiente... la puerta se abre...\"") : "§7§o\"Suficiente... la puerta se abre...\"";
        String titulo = rev != null ? rev.getString("titulo", "§5§l⚡ LA PUERTA SE ABRE... ⚡") : "§5§l⚡ LA PUERTA SE ABRE... ⚡";
        String subtitulo = rev != null ? rev.getString("subtitulo", "§7§o...suficiente...") : "§7§o...suficiente...";
        String soundName = rev != null ? rev.getString("sound", "ENTITY_ENDER_DRAGON_GROWL") : "ENTITY_ENDER_DRAGON_GROWL";
        float vol = rev != null ? (float) rev.getDouble("volumen", 0.6) : 0.6f;
        float pitch = rev != null ? (float) rev.getDouble("pitch", 0.6) : 0.6f;
        Sound soundTemp;
        try { soundTemp = Sound.valueOf(soundName); } catch (Exception e) { soundTemp = Sound.ENTITY_ENDER_DRAGON_GROWL; }
        final Sound s = soundTemp;

        // Momento cinemático: Silencio inicial
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            messageBus.broadcast(observador, "observador");
            messageBus.broadcast(mensaje, "fase_revelacion");
            
            // Título inicial dramático
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.sendTitle(titulo, subtitulo, 10, 50, 15);
                p.playSound(p.getLocation(), s, vol, pitch);
            }
        }, 20L);
        
        // Slow motion + efectos visuales (1.5 segundos después)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                // Efecto de slow motion temporal
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 2, false, false, false));
                
                // Camera shake con partículas
                p.spawnParticle(Particle.EXPLOSION, p.getEyeLocation(), 3, 0.5, 0.5, 0.5, 0);
                p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 0.8f);
            }
        }, 30L);
        
        // Título dramático (2.5 segundos después) - Más tiempo para absorber
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.sendTitle(
                    "§5§l⚡ REVELACIÓN",
                    "§7...incompleta...",
                    20, 90, 25
                );
            }
        }, 50L);
        
        soundUtil.playSoundAll(Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 1.2f);
        
        // Secuencia de conclusión épica después del portal
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            anunciarConclusionPortal();
        }, 200L); // 10 segundos después
    }
    
    /**
     * Secuencia cinemática final que explica la naturaleza incompleta del portal
     */
    private void anunciarConclusionPortal() {
        // Marcar que el cliffhanger está siendo ejecutado
        cliffhangerEjecutado = true;
        plugin.getLogger().info("[CaminoEnd] Ejecutando secuencia de cliffhanger (anunciarConclusionPortal)");
        
        // Fase 1: Realización (0s) - Pausa dramática
        messageBus.broadcast("", "conclusión_espacio1");
        messageBus.broadcast("§5§l⚡ EL OBSERVADOR:", "conclusión_observador");
        
        // Pausa de 2 segundos antes de la revelación
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            messageBus.broadcast("§7§o\"...incompleto.\"", "conclusión_1");
        }, 40L);
        
        // Fase 2: Pánico contenido (5s) - Más tiempo para asimilar
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            messageBus.broadcast("§c§l§o\"No... NO.\"", "conclusión_2");
            soundUtil.playSoundAll(Sound.ENTITY_ENDERMAN_SCREAM, 0.3f, 0.8f);
            
            // Título de pánico
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.sendTitle("§c§l✗ NO... NO ✗", "§7§oAlgo está mal...", 10, 40, 10);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 0.5f, 0.6f);
            }
        }, 100L);
        
        // Fase 3: Explicación ominosa (9s) - Primera línea
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            messageBus.broadcast("§7§o\"Este portal... no debería existir aquí.\"", "conclusión_3");
        }, 180L);
        
        // Segunda línea de explicación (12s)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            messageBus.broadcast("§7§o\"Está... roto. Fragmentado entre dimensiones.\"", "conclusión_4");
        }, 240L);
        
        // Fase 4: Implicaciones (16s) - Primera revelación
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            messageBus.broadcast("§4§l§o\"Algo lo atravesó desde el otro lado...\"", "conclusión_5");
            
            // Título de terror
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.sendTitle("§4§l⚠ ALGO LO ATRAVESÓ ⚠", "§7§o...desde el otro lado...", 10, 50, 15);
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.4f, 0.5f);
            }
        }, 320L);
        
        // Segunda revelación más inquietante (19s)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            messageBus.broadcast("§4§l§o\"...y dejó esto atrás.\"", "conclusión_6");
            
            // Efecto visual global
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.getWorld().spawnParticle(
                    Particle.SCULK_SOUL,
                    p.getLocation().add(0, 3, 0),
                    30, 4, 2, 4, 0.05
                );
                p.playSound(p.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.4f, 0.5f);
            }
        }, 380L);
        
        // Fase 5: Pregunta perturbadora (24s) - Más tiempo para el impacto
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            messageBus.broadcast("", "conclusión_espacio2");
            messageBus.broadcast("§5§l⚡ EL OBSERVADOR:", "conclusión_observador2");
        }, 480L);
        
        // La pregunta final 2 segundos después (26s)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            messageBus.broadcast("§7§o\"...¿Qué estaba intentando escapar?\"", "conclusión_7");
            messageBus.broadcast("", "conclusión_espacio3");
            
            // Título final inquietante
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.sendTitle("§8§l§k||§r §4§l¿QUÉ ESCAPABA?§r §8§l§k||", "§7§o...la pregunta sin respuesta...", 15, 80, 25);
                p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 0.5f);
            }
        }, 520L);
        
        // Fase 6: Título final (30s) - Cierre cinematográfico
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.sendTitle(
                    "§5§l§k|||§r §4§lPORTAL INCOMPLETO§r §5§l§k|||",
                    "§7§oLa historia continúa...",
                    20, 100, 30
                );
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.3f, 0.5f);
            }
        }, 340L);
    }
    
    private void anunciarFinalizacion() {
        ConfigurationSection fin = config.getConfigurationSection("mensajes.finalizacion");
        if (fin == null) {
            messageBus.broadcast("§5§l⚡ EL OBSERVADOR:", "observador");
            messageBus.broadcast("§7§o\"El camino... aún no está completo...\"", "finalizacion");
            messageBus.broadcast("§7§o\"Pero ahora sabemos... que existe.\"", "finalizacion2");
            return;
        }
        String observador = fin.getString("observador", "§5§l⚡ EL OBSERVADOR:");
        java.util.List<String> mensajes = fin.getStringList("mensajes");
        messageBus.broadcast(observador, "observador");
        for (String m : mensajes) {
            messageBus.broadcast(m, "finalizacion");
        }
    }
    
    private void finalizarEvento() {
        // Distribuir recompensas
        distribuirRecompensas();
        
        // Detener evento
        finalizarEventoSinRecompensas();
    }
    
    /**
     * Finaliza el evento sin distribuir recompensas (ya fueron distribuidas)
     */
    private void finalizarEventoSinRecompensas() {
        // Mensaje final
        anunciarFinalizacion();
        
        // Detener evento
        stop();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ITEMS RESONANTES (RECOMPENSAS FINALES)
    // ═══════════════════════════════════════════════════════════════════
    
    private ItemStack crearEspadaResonante() {
        ItemStack espada = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = espada.getItemMeta();
        
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("§d§lEspada Resonante"));
            meta.lore(java.util.Arrays.asList(
                net.kyori.adventure.text.Component.text("§7Recompensa del Camino al End"),
                net.kyori.adventure.text.Component.text(""),
                net.kyori.adventure.text.Component.text("§7Una espada imbuida con energía"),
                net.kyori.adventure.text.Component.text("§7dimensional estable."),
                net.kyori.adventure.text.Component.text(""),
                net.kyori.adventure.text.Component.text("§9Filo III"),
                net.kyori.adventure.text.Component.text("§9Empuje I"),
                net.kyori.adventure.text.Component.text("§9Irrompibilidad II"),
                net.kyori.adventure.text.Component.text(""),
                net.kyori.adventure.text.Component.text("§d§o\"Resuena con el vacío...\"")
            ));
            
            meta.addEnchant(Enchantment.SHARPNESS, 3, true);
            meta.addEnchant(Enchantment.KNOCKBACK, 1, true);
            meta.addEnchant(Enchantment.UNBREAKING, 2, true);
            
            espada.setItemMeta(meta);
        }
        
        return espada;
    }
    
    private ItemStack crearPicoResonante() {
        ItemStack pico = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = pico.getItemMeta();
        
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("§d§lPico Resonante"));
            meta.lore(java.util.Arrays.asList(
                net.kyori.adventure.text.Component.text("§7Recompensa del Camino al End"),
                net.kyori.adventure.text.Component.text(""),
                net.kyori.adventure.text.Component.text("§7Un pico fortalecido con ecos"),
                net.kyori.adventure.text.Component.text("§7dimensionales."),
                net.kyori.adventure.text.Component.text(""),
                net.kyori.adventure.text.Component.text("§9Eficiencia III"),
                net.kyori.adventure.text.Component.text("§9Fortuna II"),
                net.kyori.adventure.text.Component.text("§9Irrompibilidad II"),
                net.kyori.adventure.text.Component.text(""),
                net.kyori.adventure.text.Component.text("§d§o\"Extrae lo oculto...\"")
            ));
            
            meta.addEnchant(Enchantment.EFFICIENCY, 3, true);
            meta.addEnchant(Enchantment.FORTUNE, 2, true);
            meta.addEnchant(Enchantment.UNBREAKING, 2, true);
            
            pico.setItemMeta(meta);
        }
        
        return pico;
    }
    
    private ItemStack crearPetoResonante() {
        ItemStack peto = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta meta = peto.getItemMeta();
        
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("§d§lPeto Resonante"));
            meta.lore(java.util.Arrays.asList(
                net.kyori.adventure.text.Component.text("§7Recompensa del Camino al End"),
                net.kyori.adventure.text.Component.text(""),
                net.kyori.adventure.text.Component.text("§7Armadura reforzada con cristales"),
                net.kyori.adventure.text.Component.text("§7del vacío dimensional."),
                net.kyori.adventure.text.Component.text(""),
                net.kyori.adventure.text.Component.text("§9Protección III"),
                net.kyori.adventure.text.Component.text("§9Irrompibilidad II"),
                net.kyori.adventure.text.Component.text(""),
                net.kyori.adventure.text.Component.text("§d§o\"Protección dimensional...\"")
            ));
            
            meta.addEnchant(Enchantment.PROTECTION, 3, true);
            meta.addEnchant(Enchantment.UNBREAKING, 2, true);
            
            peto.setItemMeta(meta);
        }
        
        return peto;
    }
    
    private ItemStack crearPantalonesResonantes() {
        ItemStack pantalones = new ItemStack(Material.DIAMOND_LEGGINGS);
        ItemMeta meta = pantalones.getItemMeta();
        
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("§d§lPantalones Resonantes"));
            meta.lore(java.util.Arrays.asList(
                net.kyori.adventure.text.Component.text("§7Recompensa del Camino al End"),
                net.kyori.adventure.text.Component.text(""),
                net.kyori.adventure.text.Component.text("§7Pantalones imbuidos con energía"),
                net.kyori.adventure.text.Component.text("§7del eco dimensional."),
                net.kyori.adventure.text.Component.text(""),
                net.kyori.adventure.text.Component.text("§9Protección III"),
                net.kyori.adventure.text.Component.text("§9Irrompibilidad II"),
                net.kyori.adventure.text.Component.text(""),
                net.kyori.adventure.text.Component.text("§d§o\"Agilidad del vacío...\"")
            ));
            
            meta.addEnchant(Enchantment.PROTECTION, 3, true);
            meta.addEnchant(Enchantment.UNBREAKING, 2, true);
            
            pantalones.setItemMeta(meta);
        }
        
        return pantalones;
    }
    
    private void distribuirRecompensas() {
        ConfigurationSection recompensas = config.getConfigurationSection("recompensas");
        if (recompensas == null) return;
        
        int psBase = recompensas.getInt("ps_base", 100);
        int psPorFragmento = recompensas.getInt("ps_por_fragmento", 5);
        
        // Calcular Top 3 Recolectores
        List<Map.Entry<UUID, Integer>> ranking = new ArrayList<>(fragmentosPorJugador.entrySet());
        ranking.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        
        // Anunciar Top 3
        anunciarTop3(ranking);
        
        for (UUID uuid : participantes) {
            Player jugador = plugin.getServer().getPlayer(uuid);
            if (jugador == null) continue;
            
            int fragmentos = fragmentosPorJugador.getOrDefault(uuid, 0);
            int psTotal = psBase + (fragmentos * psPorFragmento);
            
            // Verificar si está en el top 3
            int posicion = obtenerPosicionRanking(uuid, ranking);
            int bonoTop = 0;
            String tituloTop = null;
            String rangoRecompensa = "PARTICIPANTE";
            
            if (posicion == 1) {
                bonoTop = recompensas.getInt("bono_top1", 300);
                tituloTop = "§5§l🌟 MAESTRO DEL VACÍO";
                rangoRecompensa = "MAESTRO";
            } else if (posicion == 2) {
                bonoTop = recompensas.getInt("bono_top2", 200);
                tituloTop = "§d§l✦ EXPLORADOR RESONANTE";
                rangoRecompensa = "EXPLORADOR";
            } else if (posicion == 3) {
                bonoTop = recompensas.getInt("bono_top3", 100);
                tituloTop = "§e§l⚡ VIDENTE DEL UMBRAL";
                rangoRecompensa = "VIDENTE";
            }
            
            psTotal += bonoTop;
            
            // XP de rango
            int xpRango = 500 + (fragmentos * 10);
            if (posicion <= 3) {
                xpRango += 300; // Bonus XP para top 3
            }
            
            if (plugin.getExperienceService() != null) {
                plugin.getExperienceService().addXP(jugador, xpRango, "camino_end", false);
            }
            
            // Preparar recompensas para RewardClaimSystem
            List<ItemStack> recompensasItems = new ArrayList<>();
            
            // Item conmemorativo: Marca del Observador
            ItemStack marca = items.crearMarcaDelObservador();
            recompensasItems.add(marca);
            
            // Fragmentos del Vacío extra (basado en participación)
            int fragmentosExtra = Math.max(3, fragmentos / 2);
            ItemStack fragmentosVacio = items.crearFragmentoDelVacio();
            fragmentosVacio.setAmount(fragmentosExtra);
            recompensasItems.add(fragmentosVacio);
            
            // Materiales útiles según posición
            if (posicion == 1) {
                // Top 1: Recompensas premium
                recompensasItems.add(new ItemStack(Material.NETHERITE_INGOT, 2));
                recompensasItems.add(new ItemStack(Material.DIAMOND, 16));
                recompensasItems.add(new ItemStack(Material.ENDER_PEARL, 32));
                recompensasItems.add(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
                
                // Armadura de Diamante encantada (buena pero no tan única como drops del Warden)
                recompensasItems.add(crearPetoResonante());
                recompensasItems.add(crearPantalonesResonantes());
                // Herramientas y armas encantadas
                recompensasItems.add(crearEspadaResonante());
                recompensasItems.add(crearPicoResonante());
                
            } else if (posicion == 2) {
                // Top 2: Recompensas altas
                recompensasItems.add(new ItemStack(Material.NETHERITE_INGOT, 1));
                recompensasItems.add(new ItemStack(Material.DIAMOND, 12));
                recompensasItems.add(new ItemStack(Material.ENDER_PEARL, 24));
                
                // Armadura de Diamante encantada
                recompensasItems.add(crearPetoResonante());
                // Herramientas encantadas
                recompensasItems.add(crearEspadaResonante());
                recompensasItems.add(crearPicoResonante());
                
            } else if (posicion == 3) {
                // Top 3: Recompensas buenas
                recompensasItems.add(new ItemStack(Material.DIAMOND, 8));
                recompensasItems.add(new ItemStack(Material.ENDER_PEARL, 16));
                
                // Herramientas encantadas
                recompensasItems.add(crearEspadaResonante());
                recompensasItems.add(crearPicoResonante());
                
            } else {
                // Participantes: Recompensas base + herramientas básicas
                recompensasItems.add(new ItemStack(Material.DIAMOND, 4));
                recompensasItems.add(new ItemStack(Material.ENDER_PEARL, 8));
                
                // Herramienta básica encantada
                recompensasItems.add(crearEspadaResonante());
            }
            
            // Recursos comunes para todos
            recompensasItems.add(new ItemStack(Material.OBSIDIAN, 16));
            recompensasItems.add(new ItemStack(Material.END_STONE, 32));
            recompensasItems.add(new ItemStack(Material.EXPERIENCE_BOTTLE, 16));
            
            // Registrar recompensas en el sistema
            if (plugin.getRewardClaimSystem() != null) {
                plugin.getRewardClaimSystem().addRewards(
                    uuid,
                    "camino_end",
                    "§5§l⚡ El Camino al End",
                    recompensasItems,
                    60, // 60 minutos = 1 hora para reclamar
                    rangoRecompensa,
                    psTotal
                );
                
                // Mensaje de recompensa
                jugador.sendMessage("");
                jugador.sendMessage("§8§m═══════════════════════════════════════════");
                jugador.sendMessage("");
                jugador.sendMessage("     §5§l⚡ §f§lRECOMPENSAS DEL CAMINO AL END §5§l⚡");
                jugador.sendMessage("");
                jugador.sendMessage("§7Has recibido recompensas por tu exploración:");
                jugador.sendMessage("");
                jugador.sendMessage("§5§l✦ XP Y PROGRESO:");
                jugador.sendMessage("  §8▪ §e+" + xpRango + " XP de Rango");
                jugador.sendMessage("  §8▪ §b+" + psTotal + " Bloques de Protección");
                if (bonoTop > 0) {
                    jugador.sendMessage("     §7└ §e+" + bonoTop + " bono por TOP " + posicion);
                }
                jugador.sendMessage("");
                jugador.sendMessage("§5§l✦ ITEMS RECLAMABLES:");
                jugador.sendMessage("  §8▪ §5⭐ Marca del Observador §7(conmemorativo)");
                jugador.sendMessage("  §8▪ §d" + fragmentosExtra + " Fragmentos del Vacío");
                if (posicion == 1) {
                    jugador.sendMessage("  §8▪ §32 Netherite Ingot, 16 Diamantes");
                    jugador.sendMessage("  §8▪ §f32 Perlas de Ender, 1 Tótem");
                } else if (posicion == 2) {
                    jugador.sendMessage("  §8▪ §31 Netherite Ingot, 12 Diamantes");
                    jugador.sendMessage("  §8▪ §f24 Perlas de Ender");
                } else if (posicion == 3) {
                    jugador.sendMessage("  §8▪ §38 Diamantes, 16 Perlas de Ender");
                } else {
                    jugador.sendMessage("  §8▪ §34 Diamantes, 8 Perlas de Ender");
                }
                jugador.sendMessage("  §8▪ §7Obsidiana, End Stone, XP embotellado");
                jugador.sendMessage("");
                jugador.sendMessage("§7Reclama tus items con §e/recompensas");
                jugador.sendMessage("§7§oTienes §e1 hora §7§opara reclamarlos");
                jugador.sendMessage("");
                
                // Título especial para top 3
                if (tituloTop != null) {
                    jugador.sendMessage("§7  " + tituloTop);
                    jugador.sendMessage("");
                    jugador.sendTitle(tituloTop, "§7Has demostrado tu conexión con el Vacío", 10, 60, 20);
                    
                    // Efectos visuales especiales para top 3
                    aplicarEfectosTop(jugador, posicion);
                }
                
                jugador.sendMessage("§8§m═══════════════════════════════════════════");
                jugador.sendMessage("");
            }
            
            // Dar PS directamente vía comando
            String comando = "ps give 15 " + jugador.getName() + " " + psTotal;
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comando);
            });
            
            // Efectos visuales base
            Location loc = jugador.getLocation();
            jugador.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 1, 0), 30, 1, 1, 1, 0.1);
            jugador.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
            jugador.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            jugador.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
            
            plugin.getLogger().info("[CaminoEndEvent] Recompensa entregada a " + jugador.getName() + 
                ": " + psTotal + " PS, " + xpRango + " XP (" + fragmentos + " fragmentos, posición: " + posicion + ")");
        }
    }
    
    private void anunciarTop3(List<Map.Entry<UUID, Integer>> ranking) {
        messageBus.broadcast("", "");
        messageBus.broadcast("§5§l╔═══════════════════════════════════════╗", "top_header");
        messageBus.broadcast("§5§l║     🌟 EXPLORADORES DEL VACÍO 🌟     ║", "top_title");
        messageBus.broadcast("§5§l╚═══════════════════════════════════════╝", "top_footer");
        messageBus.broadcast("", "");
        
        for (int i = 0; i < Math.min(3, ranking.size()); i++) {
            UUID uuid = ranking.get(i).getKey();
            int fragmentos = ranking.get(i).getValue();
            Player jugador = plugin.getServer().getPlayer(uuid);
            
            if (jugador == null) continue;
            
            String medalla;
            String titulo;
            if (i == 0) {
                medalla = "§6§l👑";
                titulo = "§5§lMAESTRO DEL VACÍO";
            } else if (i == 1) {
                medalla = "§d§l✦";
                titulo = "§d§lEXPLORADOR RESONANTE";
            } else {
                medalla = "§e§l⚡";
                titulo = "§e§lVIDENTE DEL UMBRAL";
            }
            
            messageBus.broadcast(medalla + " §7#" + (i + 1) + " §f" + jugador.getName() + 
                " §7- §e" + fragmentos + " fragmentos §7- " + titulo, "top_" + (i + 1));
        }
        
        messageBus.broadcast("", "");
    }
    
    private int obtenerPosicionRanking(UUID uuid, List<Map.Entry<UUID, Integer>> ranking) {
        for (int i = 0; i < ranking.size(); i++) {
            if (ranking.get(i).getKey().equals(uuid)) {
                return i + 1;
            }
        }
        return -1;
    }
    
    private void aplicarEfectosTop(Player jugador, int posicion) {
        Location loc = jugador.getLocation();
        
        if (posicion == 1) {
            // Top 1: Explosión de partículas del END + Dragon Breath
            jugador.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc.add(0, 1, 0), 50, 0.5, 1.0, 0.5, 0.05);
            jugador.getWorld().spawnParticle(Particle.END_ROD, loc, 30, 0.5, 1.0, 0.5, 0.2);
            jugador.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.5f);
            
            // Efecto de brillo temporal
            jugador.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0, false, false));
            
        } else if (posicion == 2) {
            // Top 2: Portal particles + Soul
            jugador.getWorld().spawnParticle(Particle.PORTAL, loc.add(0, 1, 0), 40, 0.5, 1.0, 0.5, 0.5);
            jugador.getWorld().spawnParticle(Particle.SOUL, loc, 20, 0.5, 1.0, 0.5, 0.05);
            jugador.playSound(loc, Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 1.5f);
            
        } else if (posicion == 3) {
            // Top 3: Sculk Soul + End Rod
            jugador.getWorld().spawnParticle(Particle.SCULK_SOUL, loc.add(0, 1, 0), 30, 0.5, 1.0, 0.5, 0.05);
            jugador.getWorld().spawnParticle(Particle.END_ROD, loc, 15, 0.5, 1.0, 0.5, 0.1);
            jugador.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // GETTERS PÚBLICOS
    // ═══════════════════════════════════════════════════════════════════
    
    public Fase getFaseActual() {
        return faseActual;
    }
    
    public Map<Location, AnomaliaData> getAnomaliasActivas() {
        return anomaliasActivas;
    }
    
    public int getFragmentosRecolectados() {
        return fragmentosRecolectadosGlobalmente;
    }
    
    public CaminoEndItems getItems() {
        return items;
    }
    
    public Random getRandom() {
        return random;
    }
    
    /**
     * Resetea el Warden cuando es derrotado (para permitir transición a REVELACION)
     */
    public void resetearWarden() {
        if (wardenActivo != null) {
            wardenActivo = null;
            plugin.getLogger().info("[CaminoEnd] Warden derrotado - Transición a REVELACION desbloqueada");
        }
    }
    
    /**
     * Fuerza transición a la siguiente fase (para comando admin)
     */
    public void forzarSiguienteFase() {
        switch (faseActual) {
            case ANOMALIAS:
                transicionarAFase(Fase.RESONANCIA);
                plugin.getLogger().info("[CaminoEnd] Fase forzada: ANOMALIAS → RESONANCIA");
                break;
            case RESONANCIA:
                transicionarAFase(Fase.REVELACION);
                plugin.getLogger().info("[CaminoEnd] Fase forzada: RESONANCIA → REVELACION");
                break;
            case REVELACION:
                // Ejecutar cliffhanger antes de finalizar
                plugin.getLogger().info("[CaminoEnd] Comando 'next' en REVELACION - Ejecutando cliffhanger");
                
                // Notificar a los jugadores que el cliffhanger está comenzando
                messageBus.broadcast("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", "cliffhanger_inicio");
                messageBus.broadcast("§5§l⚡ SECUENCIA FINAL", "cliffhanger_inicio");
                messageBus.broadcast("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", "cliffhanger_inicio");
                
                ejecutarCliffhangerYFinalizar();
                break;
        }
    }
    
    /**
     * Ejecuta la secuencia de cliffhanger completa y luego finaliza el evento
     */
    private void ejecutarCliffhangerYFinalizar() {
        // Evitar ejecuciones múltiples
        if (cliffhangerEjecutado) {
            plugin.getLogger().info("[CaminoEnd] Cliffhanger ya ejecutado - Finalizando directamente");
            finalizarEvento();
            return;
        }
        
        cliffhangerEjecutado = true;
        plugin.getLogger().info("[CaminoEnd] Ejecutando secuencia de cliffhanger...");
        
        // Anunciar la secuencia de conclusión del portal (cliffhanger)
        anunciarConclusionPortal();
        
        // Programar distribución de recompensas DESPUÉS del cliffhanger (32 segundos)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getLogger().info("[CaminoEnd] Cliffhanger completado - Distribuyendo recompensas");
            
            // Mensaje de transición
            messageBus.broadcast("", "recompensas_inicio");
            messageBus.broadcast("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", "recompensas_inicio");
            messageBus.broadcast("§d§l★ RECOMPENSAS DEL EVENTO ★", "recompensas_inicio");
            messageBus.broadcast("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", "recompensas_inicio");
            messageBus.broadcast("", "recompensas_inicio");
            
            // Título para todos los jugadores
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.sendTitle("§d§l★ RECOMPENSAS ★", "§7El Observador reconoce tu valoría...", 10, 60, 20);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            }
            
            // Distribuir recompensas
            distribuirRecompensas();
            
            // Programar finalización 8 segundos después de las recompensas
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getLogger().info("[CaminoEnd] Recompensas entregadas - Finalizando evento");
                finalizarEventoSinRecompensas();
            }, 160L); // 8 segundos después
            
        }, 640L); // 32 segundos (cliffhanger completo dura ~30 segundos)
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE BRÚJULA FUNCIONAL
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Actualiza las brújulas de todos los jugadores que las tienen equipadas
     */
    private void actualizarBrujulas() {
        for (Player jugador : plugin.getServer().getOnlinePlayers()) {
            ItemStack itemMano = jugador.getInventory().getItemInMainHand();
            ItemStack itemOffHand = jugador.getInventory().getItemInOffHand();
            
            boolean tieneBrujula = (itemMano != null && items.esBrujulaDelVacio(itemMano)) ||
                                   (itemOffHand != null && items.esBrujulaDelVacio(itemOffHand));
            
            if (!tieneBrujula) continue;
            
            // Buscar anomalía más cercana
            Location anomaliaCercana = encontrarAnomaliaMasCercana(jugador.getLocation());
            
            if (anomaliaCercana != null) {
                jugador.setCompassTarget(anomaliaCercana);
                
                // Sonido sutil cada 3 segundos
                if (ticksTotales % 60 == 0) {
                    jugador.playSound(jugador.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.1f, 1.5f);
                }
            }
        }
    }
    
    /**
     * Muestra guía de anomalías en action bar para TODOS los jugadores
     * Con flechas direccionales y distancia
     */
    private void mostrarGuiaAnomalias() {
        if (anomaliasActivas.isEmpty()) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.sendActionBar("§5§l⚡ §7Esperando nuevas anomalías...");
            }
            return;
        }
        
        for (Player jugador : plugin.getServer().getOnlinePlayers()) {
            UUID uuid = jugador.getUniqueId();
            
            // Buscar anomalía más cercana (excluyendo las ya visitadas)
            Location anomaliaCercana = encontrarAnomaliaMasCercanaNoVisitada(jugador);
            
            if (anomaliaCercana == null) {
                // Si todas fueron visitadas, mostrar mensaje especial
                int visitadas = anomaliasVisitadasPorJugador.getOrDefault(uuid, new HashSet<>()).size();
                if (visitadas > 0) {
                    jugador.sendActionBar("§a✓ §7Has explorado §e" + visitadas + " §7anomalías. §8Esperando más...");
                } else {
                    jugador.sendActionBar("§5§l⚡ §7Esperando nuevas anomalías...");
                }
                continue;
            }
            
            AnomaliaData data = anomaliasActivas.get(anomaliaCercana);
            double distancia = jugador.getLocation().distance(anomaliaCercana);
            String flecha = obtenerFlechaDireccional(jugador, anomaliaCercana);
            
            // Verificar si el jugador llegó a la anomalía
            if (distancia < 8) {
                marcarAnomaliaComoVisitada(jugador, anomaliaCercana);
            }
            
            // Mensaje de progresión dinámico según distancia
            String mensaje = obtenerMensajeProgresion(distancia, flecha, data, jugador);
            
            jugador.sendActionBar(mensaje);
        }
    }

    /**
     * Muestra guía hacia fragmentos durante la fase RESONANCIA.
     * Reutiliza las ubicaciones de anomalías pero ajusta el texto
     * para centrar al jugador en recolectar fragmentos.
     */
    private void mostrarGuiaFragmentos() {
        if (anomaliasActivas.isEmpty()) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.sendActionBar("§5§l⚡ §7Buscando señales de fragmentos...");
            }
            return;
        }

        for (Player jugador : plugin.getServer().getOnlinePlayers()) {
            UUID uuid = jugador.getUniqueId();

            // Encontrar anomalía más cercana que NO esté visitada y no haya dado fragmento
            Location objetivo = null;
            double distanciaMin = Double.MAX_VALUE;
            Set<Location> visitadas = anomaliasVisitadasPorJugador.getOrDefault(uuid, new HashSet<>());

            for (Map.Entry<Location, AnomaliaData> entry : anomaliasActivas.entrySet()) {
                Location loc = entry.getKey();
                AnomaliaData data = entry.getValue();
                if (visitadas.contains(loc)) continue;
                if (data.fragmentoObtenido) continue;
                if (loc.getWorld() == null || !loc.getWorld().equals(jugador.getWorld())) continue;
                double d = jugador.getLocation().distance(loc);
                if (d < distanciaMin) {
                    distanciaMin = d;
                    objetivo = loc;
                }
            }

            if (objetivo == null) {
                int recolectadas = fragmentosPorJugador.getOrDefault(uuid, 0);
                jugador.sendActionBar("§a✓ §7Fragmentos propios: §e" + recolectadas + " §8Esperando nuevos ecos...");
                continue;
            }

            AnomaliaData data = anomaliasActivas.get(objetivo);
            String flecha = obtenerFlechaDireccional(jugador, objetivo);
            double distancia = jugador.getLocation().distance(objetivo);

            // Marcar como visitada si está muy cerca para ayudar al avance
            if (distancia < 8) {
                marcarAnomaliaComoVisitada(jugador, objetivo);
            }

            // Mensajes centrados en fragmentos según distancia
            String tipoNombre = data != null ? data.tipo.getNombre() : "§7Desconocida";
            String mensaje;
            if (distancia < 5) {
                mensaje = "§d✦ §5§lFRAGMENTO CERCANO §d✦ §8[" + tipoNombre + "]";
            } else if (distancia < 20) {
                mensaje = String.format("§5§l⚡ FRAGMENTO %s §e%.0fm §8[%s]", flecha, distancia, tipoNombre);
            } else if (distancia < 50) {
                mensaje = String.format("§5⚡ Sigue el eco %s §e%.0fm §8[%s]", flecha, distancia, tipoNombre);
            } else {
                mensaje = String.format("§5⚡ %s §7%.0fm §8(Fragmento)", flecha, distancia);
            }

            jugador.sendActionBar(mensaje);
        }
    }
    
    /**
     * Obtiene mensaje de progresión dinámico según distancia y tipo de anomalía
     */
    private String obtenerMensajeProgresion(double distancia, String flecha, AnomaliaData data, Player jugador) {
        UUID uuid = jugador.getUniqueId();
        int visitadas = anomaliasVisitadasPorJugador.getOrDefault(uuid, new HashSet<>()).size();
        String tipoNombre = data.tipo.getNombre();
        TipoAnomalia tipo = data.tipo;
        
        // Muy cerca (< 8m) - Llegaste - Mensajes únicos por tipo
        if (distancia < 8) {
            if (tipo == TipoAnomalia.ANTIGUA) {
                String[] mensajes = {
                    "§d✦ §5§lANOMALÍA ANTIGUA §d✦ §8[§a✓ " + (visitadas + 1) + "§8]",
                    "§5§l⚡ PRESENCIA ANCESTRAL §d✦ §7" + tipoNombre,
                    "§d§l✦ ECO DEL PASADO LEJANO §d✦ §8[Explorada]"
                };
                return mensajes[random.nextInt(mensajes.length)];
            } else if (tipo == TipoAnomalia.INESTABLE) {
                String[] mensajes = {
                    "§e⚡ §l§lANOMALÍA INESTABLE §e⚡ §8[§a✓ " + (visitadas + 1) + "§8]",
                    "§e§l⚡ ENERGÍA CAÓTICA ALCANZADA §d✦",
                    "§c§l⚠ ZONA INESTABLE §e⚡ §8[Peligro]"
                };
                return mensajes[random.nextInt(mensajes.length)];
            } else {
                String[] mensajes = {
                    "§d✦ §5§lLLEGASTE §d✦ §7" + tipoNombre + " §8[§a✓ " + (visitadas + 1) + "§8]",
                    "§5§l⚡ ANOMALÍA ALCANZADA §d✦ §7" + tipoNombre,
                    "§a§l✓ EXPLORADA §d✦ §7" + tipoNombre + " §8[" + (visitadas + 1) + " encontradas§8]"
                };
                return mensajes[random.nextInt(mensajes.length)];
            }
        }
        
        // Cerca (8-20m) - Mensajes por tipo
        if (distancia < 20) {
            if (tipo == TipoAnomalia.ANTIGUA) {
                String[] mensajes = {
                    "§5§l⚡ ECO ANCESTRAL CERCANO " + flecha + " §e" + String.format("%.0fm", distancia),
                    "§d§l✦ SIENTES SU EDAD " + flecha + " §e" + String.format("%.0fm", distancia),
                    "§5§l⚡ PRESENCIA ANTIGUA " + flecha + " §e" + String.format("%.0fm", distancia) + " §8[Más vieja que el tiempo]"
                };
                return mensajes[random.nextInt(mensajes.length)];
            } else if (tipo == TipoAnomalia.INESTABLE) {
                String[] mensajes = {
                    "§e§l⚡ ZONA CAÓTICA CERCANA " + flecha + " §e" + String.format("%.0fm", distancia),
                    "§c§l⚠ ENERGÍA INESTABLE " + flecha + " §e" + String.format("%.0fm", distancia) + " §8[Peligro]",
                    "§e§l⚡ PULSOS ERRÁTICOS " + flecha + " §e" + String.format("%.0fm", distancia)
                };
                return mensajes[random.nextInt(mensajes.length)];
            } else {
                String[] mensajes = {
                    "§5§l⚡ MUY CERCA §d✦ " + flecha + " §e" + String.format("%.0fm", distancia) + " §7" + tipoNombre,
                    "§d§l⚡ CASI AHÍ " + flecha + " §e" + String.format("%.0fm", distancia) + " §8[" + tipoNombre + "§8]",
                    "§5§l⚡ LA SIENTES " + flecha + " §e" + String.format("%.0fm", distancia) + " §7" + tipoNombre
                };
                return mensajes[random.nextInt(mensajes.length)];
            }
        }
        
        // Media distancia (20-50m) - Mensajes diferenciados
        if (distancia < 50) {
            if (tipo == TipoAnomalia.ANTIGUA) {
                return String.format("§5§l⚡ Eco Ancestral " + flecha + " §e%.0fm §8[Antigua - %d exploradas]",
                    distancia, visitadas);
            } else if (tipo == TipoAnomalia.INESTABLE) {
                return String.format("§e⚡ Zona Inestable " + flecha + " §e%.0fm §c⚠",
                    distancia);
            } else {
                String[] mensajes = {
                    "§5§l⚡ Anomalía detectada " + flecha + " §e" + String.format("%.0fm", distancia) + " §7" + tipoNombre,
                    "§5⚡ " + flecha + " §e" + String.format("%.0fm", distancia) + " §8[" + tipoNombre + "§8]",
                    "§5§l⚡ Señal " + flecha + " §e" + String.format("%.0fm", distancia) + " §7" + tipoNombre
                };
                return mensajes[random.nextInt(mensajes.length)];
            }
        }
        
        // Lejos (50-100m) - Indicador de tipo
        if (distancia < 100) {
            String indicador = tipo == TipoAnomalia.ANTIGUA ? " §5[ANCESTRAL]" :
                             (tipo == TipoAnomalia.INESTABLE ? " §e[CAÓTICA]" : "");
            return String.format("§5⚡ %s §e%.0fm §8[%s - %d encontradas]%s",
                flecha, distancia, tipoNombre, visitadas, indicador);
        }
        
        // Muy lejos (100+m)
        return String.format("§5⚡ %s §7%.0fm §8(%s)",
            flecha, distancia, tipoNombre);
    }
    
    /**
     * Encuentra anomalía más cercana que el jugador NO haya visitado
     */
    private Location encontrarAnomaliaMasCercanaNoVisitada(Player jugador) {
        UUID uuid = jugador.getUniqueId();
        Set<Location> visitadas = anomaliasVisitadasPorJugador.getOrDefault(uuid, new HashSet<>());
        
        Location masCercana = null;
        double distanciaMinima = Double.MAX_VALUE;
        
        for (Location anomalia : anomaliasActivas.keySet()) {
            if (anomalia.getWorld() == null || !anomalia.getWorld().equals(jugador.getWorld())) {
                continue;
            }
            
            // Saltar si ya fue visitada
            if (visitadas.contains(anomalia)) {
                continue;
            }
            
            double distancia = jugador.getLocation().distance(anomalia);
            if (distancia < distanciaMinima) {
                distanciaMinima = distancia;
                masCercana = anomalia;
            }
        }
        
        return masCercana;
    }
    
    /**
     * Marca una anomalía como visitada por el jugador
     */
    private void marcarAnomaliaComoVisitada(Player jugador, Location anomalia) {
        UUID uuid = jugador.getUniqueId();
        
        Set<Location> visitadas = anomaliasVisitadasPorJugador.computeIfAbsent(uuid, k -> new HashSet<>());
        
        // Si ya estaba visitada, no hacer nada
        if (visitadas.contains(anomalia)) {
            return;
        }
        
        // Marcar como visitada
        visitadas.add(anomalia);
        anomaliaActualPorJugador.put(uuid, anomalia);
        
        // Mensaje de confirmación
        AnomaliaData data = anomaliasActivas.get(anomalia);
        if (data != null) {
            jugador.sendMessage("§5§l⚡ §7Has explorado: §d" + data.tipo.getNombre());
            jugador.sendMessage("§8   Anomalías exploradas: §e" + visitadas.size() + "§8/§e" + anomaliasActivas.size());
            
            // Efecto visual y sonido
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);
            jugador.getWorld().spawnParticle(
                Particle.END_ROD,
                jugador.getLocation().add(0, 1, 0),
                30, 0.5, 0.5, 0.5, 0.1
            );
            
            // Si exploró todas las anomalías activas
            if (visitadas.size() >= anomaliasActivas.size() && anomaliasActivas.size() > 0) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        jugador.sendTitle(
                            "§d✦ §5§lEXPLORADOR §d✦",
                            "§7Has explorado todas las anomalías activas",
                            10, 60, 20
                        );
                        jugador.playSound(jugador.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                    }
                }.runTaskLater(plugin, 20L);
            }
        }
    }
    
    /**
     * Obtiene flecha direccional según hacia dónde mira el jugador
     */
    private String obtenerFlechaDireccional(Player jugador, Location destino) {
        // Calcular ángulo entre la dirección del jugador y el destino
        Location from = jugador.getLocation();
        
        // Ángulo del jugador (yaw)
        float yawJugador = from.getYaw();
        
        // Ángulo hacia el destino
        double dx = destino.getX() - from.getX();
        double dz = destino.getZ() - from.getZ();
        double yawDestino = Math.toDegrees(Math.atan2(-dx, dz));
        
        // Diferencia angular
        double diff = yawDestino - yawJugador;
        
        // Normalizar a -180 a 180
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        
        // Determinar flecha según ángulo
        if (diff >= -22.5 && diff < 22.5) {
            return "§a↑ ADELANTE";  // Adelante
        } else if (diff >= 22.5 && diff < 67.5) {
            return "§e↗ ADELANTE-DERECHA";  // Adelante-Derecha
        } else if (diff >= 67.5 && diff < 112.5) {
            return "§6→ DERECHA";  // Derecha
        } else if (diff >= 112.5 && diff < 157.5) {
            return "§c↘ ATRÁS-DERECHA";  // Atrás-Derecha
        } else if (diff >= 157.5 || diff < -157.5) {
            return "§4↓ ATRÁS";  // Atrás
        } else if (diff >= -157.5 && diff < -112.5) {
            return "§c↙ ATRÁS-IZQUIERDA";  // Atrás-Izquierda
        } else if (diff >= -112.5 && diff < -67.5) {
            return "§6← IZQUIERDA";  // Izquierda
        } else {
            return "§e↖ ADELANTE-IZQUIERDA";  // Adelante-Izquierda
        }
    }
    
    /**
     * Muestra guía hacia el portal incompleto en action bar
     */
    private void mostrarGuiaPortal() {
        if (portalLocation == null) return;
        
        for (Player jugador : plugin.getServer().getOnlinePlayers()) {
            double distancia = jugador.getLocation().distance(portalLocation);
            String flecha = obtenerFlechaDireccional(jugador, portalLocation);
            
            String mensaje;
            if (distancia < 10) {
                mensaje = "§5§l⚡ EL PORTAL ESTÁ AQUÍ §d✦ §e" + String.format("%.0fm", distancia);
            } else if (distancia < 30) {
                mensaje = String.format("§5§l⚡ PORTAL INCOMPLETO %s §e%.0fm §d✦", flecha, distancia);
            } else {
                mensaje = String.format("§5§l⚡ Portal del End %s §e%.0fm", flecha, distancia);
            }
            
            jugador.sendActionBar(mensaje);
        }
    }
    
    /**
     * Encuentra la anomalía más cercana a una ubicación
     */
    private Location encontrarAnomaliaMasCercana(Location desde) {
        Location masCercana = null;
        double distanciaMinima = Double.MAX_VALUE;
        
        for (Location anomalia : anomaliasActivas.keySet()) {
            if (anomalia.getWorld() == null || !anomalia.getWorld().equals(desde.getWorld())) {
                continue;
            }
            
            double distancia = desde.distance(anomalia);
            if (distancia < distanciaMinima) {
                distanciaMinima = distancia;
                masCercana = anomalia;
            }
        }
        
        return masCercana;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE INMERSIÓN
    // ═══════════════════════════════════════════════════════════════════
    
    private void crearBossBar() {
        bossBar = Bukkit.createBossBar(
            "§5§l⚡ El Camino al End §7| §fFase: Anomalías",
            BarColor.PURPLE,
            BarStyle.SOLID
        );
        bossBar.setProgress(0.0);
        
        // Añadir a todos los jugadores
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            bossBar.addPlayer(p);
        }
    }
    
    private void actualizarBossBar() {
        if (bossBar == null) return;
        
        // Añadir jugadores nuevos
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!bossBar.getPlayers().contains(p)) {
                bossBar.addPlayer(p);
            }
        }
        
        switch (faseActual) {
            case ANOMALIAS:
                bossBar.setTitle("§5§l⚡ El Camino al End §7| §fFase: §dAnomalías");
                bossBar.setColor(BarColor.PURPLE);
                bossBar.setProgress(Math.min(1.0, ticksEnFase / 36000.0));
                break;
                
            case RESONANCIA:
                double progreso = (double) fragmentosRecolectadosGlobalmente / FRAGMENTOS_OBJETIVO;
                bossBar.setTitle("§5§l⚡ Fragmentos: §e" + fragmentosRecolectadosGlobalmente + "§7/§e40 §7(§a" + (int)(progreso * 100) + "%§7)");
                bossBar.setColor(BarColor.BLUE);
                bossBar.setProgress(progreso);
                break;
                
            case REVELACION:
                bossBar.setTitle("§5§l⚡ REVELACIÓN §7| §fEl portal incompleto ha aparecido...");
                bossBar.setColor(BarColor.PINK);
                bossBar.setProgress(1.0);
                break;
        }
    }
    
    private void cambiarClimaFase(Fase fase) {
        for (World world : plugin.getServer().getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL) continue;
            
            switch (fase) {
                case ANOMALIAS:
                    // Clima del End - SIN LLUVIA, cielo despejado con tinte violeta
                    world.setStorm(false); // SIN lluvia
                    world.setThundering(false);
                    world.setWeatherDuration(72000); // 1 hora
                    
                    // FASE 1: Atardecer misterioso (luz violeta del End)
                    world.setTime(13000); // Atardecer temprano (luz violeta)
                    
                    // Efectos ambientales del End
                    plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                        if (faseActual != Fase.ANOMALIAS) return;
                        
                        // Partículas violeta del End en el cielo ocasionalmente
                        if (random.nextInt(5) == 0) {
                            for (Player p : plugin.getServer().getOnlinePlayers()) {
                                Location skyLoc = p.getLocation().clone().add(
                                    (random.nextDouble() - 0.5) * 20,
                                    40 + random.nextDouble() * 20,
                                    (random.nextDouble() - 0.5) * 20
                                );
                                p.getWorld().spawnParticle(Particle.PORTAL, skyLoc, 3, 0.5, 0.5, 0.5, 0.02);
                                p.getWorld().spawnParticle(Particle.REVERSE_PORTAL, skyLoc, 2, 0.3, 0.3, 0.3, 0.01);
                            }
                        }
                    }, 100L, 60L); // Cada 3 segundos
                    break;
                    
                case RESONANCIA:
                    // Intensificar - tormenta con rayos ocasionales
                    world.setStorm(true);
                    world.setThundering(true);
                    world.setWeatherDuration(72000);
                    
                    // FASE 2: ATARDECER (tensión creciente)
                    world.setTime(12000); // Atardecer
                    
                    // Efectos de entorno progresivos
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (faseActual == Fase.RESONANCIA) {
                            for (Player p : plugin.getServer().getOnlinePlayers()) {
                                p.sendMessage("§7§o[El aire se vuelve más denso...]");
                                p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 200, 0, false, false));
                            }
                        }
                    }, 100L);
                    break;
                    
                case REVELACION:
                    // Claro dramático
                    world.setStorm(false);
                    world.setThundering(false);
                    world.setWeatherDuration(36000);
                    
                    // FASE 3: NOCHE (culminación dramática)
                    world.setTime(18000); // Noche
                    
                    // Efectos avanzados de entorno
                    for (Player p : plugin.getServer().getOnlinePlayers()) {
                        p.sendMessage("§d§o[La realidad se quiebra...]");
                        
                        // Slow falling periódico (efecto anti-gravedad)
                        BukkitRunnable slowFallingTask = new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (faseActual != Fase.REVELACION) {
                                    this.cancel();
                                    return;
                                }
                                
                                for (Player player : plugin.getServer().getOnlinePlayers()) {
                                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 0, false, false));
                                    if (random.nextInt(3) == 0) {
                                        player.sendActionBar("§d§o[Sientes la gravedad cambiar...]");
                                    }
                                }
                            }
                        };
                        slowFallingTask.runTaskTimer(plugin, 100L, 1200L); // Cada minuto
                    }
                    break;
            }
        }
    }
    
    private void restaurarClima() {
        for (World world : plugin.getServer().getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL) continue;
            world.setStorm(false);
            world.setThundering(false);
        }
    }
    
    private void enviarPista() {
        // El Observador da una pista si llevan mucho sin encontrar fragmentos
        messageBus.broadcast("§5§l⚡ EL OBSERVADOR:", "pista");
        
        String[] pistas = {
            "§7§o\"Las anomalías... están ahí fuera... esperando...\"",
            "§7§o\"Exploren más lejos... los ecos se ocultan...\"",
            "§7§o\"La distancia... es mayor de lo que piensan...\"",
            "§7§o\"No se rindan... el camino continúa...\""
        };
        
        messageBus.broadcast(pistas[random.nextInt(pistas.length)], "pista_texto");
        soundUtil.playSoundAll(Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.4f, 0.7f);
    }
    
    /**
     * Mensaje especial cuando un jugador encuentra su primera anomalía
     */
    public void onJugadorEncuentraPrimeraAnomalia(Player jugador) {
        if (jugadoresQueVieronPrimeraAnomalia.contains(jugador.getUniqueId())) {
            return; // Ya vio una antes
        }
        
        jugadoresQueVieronPrimeraAnomalia.add(jugador.getUniqueId());
        
        // Título dramático
        jugador.sendTitle(
            "§5§l⚡ ANOMALÍA DETECTADA",
            "§7Acércate para investigar...",
            10, 50, 20
        );
        
        // Mensaje del Observador
        jugador.sendMessage("");
        jugador.sendMessage("§5§l⚡ EL OBSERVADOR:");
        jugador.sendMessage("§7§o\"Esto... esto no debería estar aquí...\"");
        jugador.sendMessage("§7§o\"Acércate... investiga con cuidado...\"");
        jugador.sendMessage("");
        
        // Sonido misterioso
        jugador.playSound(jugador.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.5f, 0.8f);
        jugador.playSound(jugador.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.3f, 1.5f);
    }
    
    /**
     * Obtiene mensaje contextual del Observador según progresión emocional
     * 0-10: Curiosidad cautelosa
     * 11-20: Inquietud creciente
     * 21-30: Urgencia
     * 31-40: Revelación/Terror contenido
     */
    private String obtenerMensajeProgresivo(int fragmentos) {
        if (fragmentos <= 10) {
            // Fase 1: Curiosidad cautelosa
            String[] mensajes = {
                "§7§o\"Un fragmento... resuena con el vacío...\"",
                "§7§o\"Interesante... puedo sentir su origen...\"",
                "§7§o\"Esto... no pertenece aquí...\"",
                "§7§o\"La resonancia... es débil aún...\""
            };
            return mensajes[random.nextInt(mensajes.length)];
        } else if (fragmentos <= 20) {
            // Fase 2: Inquietud creciente
            String[] mensajes = {
                "§7§o\"La dimensión lejana... se acerca...\"",
                "§7§o\"Siento... que algo observa...\"",
                "§7§o\"Cada fragmento... debilita las barreras...\"",
                "§7§o\"¿Esto... es correcto...?\""
            };
            return mensajes[random.nextInt(mensajes.length)];
        } else if (fragmentos <= 30) {
            // Fase 3: Urgencia
            String[] mensajes = {
                "§c§o\"La resonancia... es demasiado fuerte...\"",
                "§c§o\"Algo... viene desde el otro lado...\"",
                "§c§o\"No... no puedo detenerlo ahora...\"",
                "§c§o\"Las barreras... se quiebran...\""
            };
            return mensajes[random.nextInt(mensajes.length)];
        } else {
            // Fase 4: Revelación/Terror
            String[] mensajes = {
                "§4§o\"ESTÁ AQUÍ... EL VACÍO ESTÁ AQUÍ...\"",
                "§4§o\"YA ES TARDE... LA PUERTA SE ABRE...\"",
                "§4§o\"PUEDO VERLO... EL CAMINO AL END...\"",
                "§4§o\"NO HAY VUELTA ATRÁS...\""
            };
            return mensajes[random.nextInt(mensajes.length)];
        }
    }
    
    /**
     * Aplica efectos visuales personalizados cuando un jugador recolecta fragmento
     */
    private void aplicarEfectosVisualesRecoleccion(Player jugador, Location anomaliaLoc) {
        Location jugadorLoc = jugador.getLocation();
        
        // Partículas SOUL que vuelan desde la anomalía hacia el jugador
        plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 20) { // 1 segundo de animación
                    return;
                }
                
                double progress = ticks / 20.0;
                Location particleLoc = anomaliaLoc.clone().add(
                    (jugadorLoc.getX() - anomaliaLoc.getX()) * progress,
                    (jugadorLoc.getY() - anomaliaLoc.getY()) * progress + 1,
                    (jugadorLoc.getZ() - anomaliaLoc.getZ()) * progress
                );
                
                jugador.spawnParticle(Particle.SOUL, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                jugador.spawnParticle(Particle.ENCHANT, particleLoc, 5, 0.2, 0.2, 0.2, 0);
                
                ticks++;
            }
        }, 0L, 1L);
        
        // Flash de partículas al recolectar
        jugador.spawnParticle(Particle.END_ROD, jugador.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        jugador.spawnParticle(Particle.PORTAL, jugador.getLocation(), 30, 0.3, 1, 0.3, 0.5);
        
        // Efecto de "corrupción dimensional" sutil
        if (fragmentosRecolectadosGlobalmente % 10 == 0) {
            jugador.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.NAUSEA, 40, 0, false, false, true));
            
            jugador.sendActionBar("§5§o...sientes el vacío observándote...");
        }
    }
    
    /**
     * Aplica sonidos progresivos según cantidad de fragmentos
     */
    private void aplicarSonidosProgresivos(Player jugador, int fragmentos) {
        if (fragmentos <= 10) {
            // Sonidos suaves y misteriosos
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.5f, 1.2f);
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.5f);
        } else if (fragmentos <= 20) {
            // Sonidos más intensos
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.6f, 1.0f);
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.3f);
            jugador.playSound(jugador.getLocation(), Sound.AMBIENT_CAVE, 0.3f, 0.8f);
        } else if (fragmentos <= 30) {
            // Sonidos inquietantes
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.4f, 1.5f);
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.5f, 0.7f);
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.0f);
        } else {
            // Sonidos épicos/aterradores
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.3f, 1.5f);
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 1.2f);
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 0.8f);
        }
    }
    
    /**
     * Sistema de tensión cuando quedan pocos fragmentos para completar
     */
    private void aplicarCountdownTension(int fragmentosFaltantes) {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.sendTitle(
                "§c§l⚠ FALTAN " + fragmentosFaltantes,
                "§7§oEl vacío... se acerca...",
                5, 30, 10
            );
            
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f + (0.2f * (5 - fragmentosFaltantes)));
            
            // Partículas de advertencia
            p.spawnParticle(Particle.SCULK_SOUL, p.getLocation().add(0, 2, 0), 10, 0.5, 0.5, 0.5, 0.02);
        }
        
        // Cambiar BossBar a rojo pulsante
        if (bossBar != null) {
            bossBar.setColor(BarColor.RED);
        }
        
        messageBus.broadcast("§c§l⚠ EL OBSERVADOR:", "tension");
        
        String[] mensajesTension = {
            "§c§o\"FALTAN " + fragmentosFaltantes + "... CASI ESTÁ AQUÍ...\"",
            "§c§o\"LA BARRERA SE QUIEBRA... SOLO " + fragmentosFaltantes + " MÁS...\"",
            "§c§o\"" + fragmentosFaltantes + " FRAGMENTOS... EL PORTAL DESPIERTA...\"",
            "§c§o\"SIENTO SU PRESENCIA... QUEDAN " + fragmentosFaltantes + "...\""
        };
        
        messageBus.broadcast(mensajesTension[random.nextInt(mensajesTension.length)], "tension_msg");
    }
    
    /**
     * Aplica efectos visuales de corrupción progresiva durante RESONANCIA
     */
    // ════════════════════════════════════════════════════════════
    // SISTEMA DE HORDAS (FASE RESONANCIA)
    // ════════════════════════════════════════════════════════════
    
    /**
     * Spawna una horda de entidades peligrosas cerca de los jugadores
     */
    private void spawnearHorda() {
        if (plugin.getServer().getOnlinePlayers().isEmpty()) return;
        
        // Elegir tipo de horda aleatoriamente
        int tipoHorda = random.nextInt(100);
        String nombreHorda;
        
        // Anuncio global dramático
        messageBus.broadcast("", "horda");
        messageBus.broadcast("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", "horda");
        messageBus.broadcast("§c§l⚠ HORDA DIMENSIONAL ⚠", "horda");
        
        // Efectos visuales globales
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.7f, 0.6f);
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.DARKNESS, 100, 0, false, false, true));
        }
        
        // Elegir jugador aleatorio como centro
        Player[] jugadores = plugin.getServer().getOnlinePlayers().toArray(new Player[0]);
        Player targetPlayer = jugadores[random.nextInt(jugadores.length)];
        Location spawnCenter = targetPlayer.getLocation();
        
        if (tipoHorda < 40) {
            // 40%: HORDA DE BRUTOS PIGLIN
            nombreHorda = "§c§oBrutos del Vacío";
            messageBus.broadcast("§7§o" + nombreHorda + " emergen de las sombras...", "horda");
            
            int cantidad = 3 + random.nextInt(3); // 3-5 Brutos
            for (int i = 0; i < cantidad; i++) {
                Location spawnLoc = spawnCenter.clone().add(
                    (random.nextDouble() - 0.5) * 15,
                    0,
                    (random.nextDouble() - 0.5) * 15
                );
                spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);
                
                org.bukkit.entity.PiglinBrute bruto = (org.bukkit.entity.PiglinBrute) spawnLoc.getWorld().spawnEntity(
                    spawnLoc, org.bukkit.entity.EntityType.PIGLIN_BRUTE);
                bruto.setCustomName("§c§lBruto del Vacío");
                bruto.setCustomNameVisible(true);
                bruto.setImmuneToZombification(true);
                bruto.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "horda_bruto"),
                    org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                
                // Efecto de spawn
                spawnLoc.getWorld().spawnParticle(Particle.LAVA, spawnLoc, 30, 0.5, 1, 0.5, 0.1);
            }
            
        } else if (tipoHorda < 70) {
            // 30%: HORDA DE ENDERMAN (Villagers del End)
            nombreHorda = "§5§oCiudadanos del End";
            messageBus.broadcast("§7§o" + nombreHorda + " atraviesan el vacío...", "horda");
            
            int cantidad = 4 + random.nextInt(4); // 4-7 Enderman
            for (int i = 0; i < cantidad; i++) {
                Location spawnLoc = spawnCenter.clone().add(
                    (random.nextDouble() - 0.5) * 15,
                    0,
                    (random.nextDouble() - 0.5) * 15
                );
                spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);
                
                Enderman enderman = (Enderman) spawnLoc.getWorld().spawnEntity(
                    spawnLoc, EntityType.ENDERMAN);
                enderman.setCustomName("§5§lCiudadano del End");
                enderman.setCustomNameVisible(true);
                enderman.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "horda_enderman"),
                    org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                
                // Efecto de spawn
                spawnLoc.getWorld().spawnParticle(Particle.PORTAL, spawnLoc, 50, 0.5, 1, 0.5, 0.2);
                spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.8f);
            }
            
        } else {
            // 30%: HORDA MIXTA (Brutos + Enderman + Endermites)
            nombreHorda = "§4§oLegión del Vacío";
            messageBus.broadcast("§7§o" + nombreHorda + " invade la realidad...", "horda");
            
            // 2 Brutos
            for (int i = 0; i < 2; i++) {
                Location spawnLoc = spawnCenter.clone().add(
                    (random.nextDouble() - 0.5) * 12,
                    0,
                    (random.nextDouble() - 0.5) * 12
                );
                spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);
                
                org.bukkit.entity.PiglinBrute bruto = (org.bukkit.entity.PiglinBrute) spawnLoc.getWorld().spawnEntity(
                    spawnLoc, org.bukkit.entity.EntityType.PIGLIN_BRUTE);
                bruto.setCustomName("§c§lBruto de la Legión");
                bruto.setCustomNameVisible(true);
                bruto.setImmuneToZombification(true);
                bruto.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "horda_bruto"),
                    org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                
                spawnLoc.getWorld().spawnParticle(Particle.LAVA, spawnLoc, 30, 0.5, 1, 0.5, 0.1);
            }
            
            // 3 Enderman
            for (int i = 0; i < 3; i++) {
                Location spawnLoc = spawnCenter.clone().add(
                    (random.nextDouble() - 0.5) * 12,
                    0,
                    (random.nextDouble() - 0.5) * 12
                );
                spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);
                
                Enderman enderman = (Enderman) spawnLoc.getWorld().spawnEntity(
                    spawnLoc, EntityType.ENDERMAN);
                enderman.setCustomName("§5§lTeleportador de la Legión");
                enderman.setCustomNameVisible(true);
                enderman.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "horda_enderman"),
                    org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
                
                spawnLoc.getWorld().spawnParticle(Particle.PORTAL, spawnLoc, 50, 0.5, 1, 0.5, 0.2);
            }
            
            // 5-8 Endermites
            int cantidadMites = 5 + random.nextInt(4);
            for (int i = 0; i < cantidadMites; i++) {
                Location spawnLoc = spawnCenter.clone().add(
                    (random.nextDouble() - 0.5) * 10,
                    0,
                    (random.nextDouble() - 0.5) * 10
                );
                spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);
                
                org.bukkit.entity.Endermite mite = (org.bukkit.entity.Endermite) spawnLoc.getWorld().spawnEntity(
                    spawnLoc, EntityType.ENDERMITE);
                mite.setCustomName("§7Parásito de la Legión");
                mite.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "horda_endermite"),
                    org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            }
        }
        
        messageBus.broadcast("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", "horda");
        messageBus.broadcast("", "horda");
        
        // Título para todos
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.sendTitle("§c§l⚠ HORDA ⚠", "§7" + nombreHorda, 10, 40, 10);
        }
        
        plugin.getLogger().info("[CaminoEnd] Horda spawneada: " + nombreHorda);
    }
    
    // ════════════════════════════════════════════════════════════
    // SISTEMA DE WARDEN (ÚLTIMOS FRAGMENTOS)
    // ════════════════════════════════════════════════════════════
    
    private void verificarSpawnWarden() {
        // Solo verificar cada 5 segundos
        if (ticksTotales % 100 != 0) return;
        
        // Verificar si algún jugador está cerca de una anomalía
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            for (Location anomaliaLoc : anomaliasActivas.keySet()) {
                if (p.getLocation().distance(anomaliaLoc) < 20.0) {
                    spawnearWarden(anomaliaLoc);
                    return;
                }
            }
        }
    }
    
    private void spawnearWarden(Location cerca) {
        if (wardenSpawneado) return;
        
        wardenSpawneado = true;
        
        // Efectos de pre-spawn épicos
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.DARKNESS, 200, 1, false, false, true));
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SLOWNESS, 100, 2, false, false, false));
            p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_EMERGE, 1.0f, 0.5f);
            p.playSound(p.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.0f, 0.6f);
        }
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Location spawnLoc = cerca.clone().add(
                (random.nextDouble() - 0.5) * 10,
                0,
                (random.nextDouble() - 0.5) * 10
            );
            spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);
            
            // Spawn del Warden
            wardenActivo = (org.bukkit.entity.Warden) spawnLoc.getWorld().spawnEntity(
                spawnLoc, EntityType.WARDEN);
            wardenActivo.setCustomName("§4§l☠ GUARDIÁN DE LAS PROFUNDIDADES ☠");
            wardenActivo.setCustomNameVisible(true);
            wardenActivo.setRemoveWhenFarAway(false);
            
            // Hacer al Warden MUCHO más resistente
            wardenActivo.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(1000.0); // 500 corazones
            wardenActivo.setHealth(1000.0);
            wardenActivo.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).setBaseValue(40.0);
            wardenActivo.getAttribute(org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
            
            wardenActivo.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "warden_final"),
                org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            
            // Anuncio global DRAMÁTICO con reacción del Observador
            messageBus.broadcast("", "warden");
            messageBus.broadcast("", "warden");
            messageBus.broadcast("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", "warden");
            messageBus.broadcast("§4§l☠☠☠ ALGO EMERGE DESDE LO PROFUNDO ☠☠☠", "warden");
            messageBus.broadcast("", "warden");
            messageBus.broadcast("§5§l⚡ EL OBSERVADOR §8§o[ALARMADO]:", "warden");
            messageBus.broadcast("§c§l§o\"...¿QUÉ... QUÉ ES ESO?...\"", "warden");
            messageBus.broadcast("", "warden");
            messageBus.broadcast("§7§o\"Eso... eso no es del End...\"", "warden");
            messageBus.broadcast("§7§o\"No es de NINGÚN mundo que yo conozca...\"", "warden");
            messageBus.broadcast("", "warden");
            messageBus.broadcast("§c§l§o\"...Viene desde las PROFUNDIDADES...\"", "warden");
            messageBus.broadcast("§4§l§o\"...Alguien... o ALGO... lo envió...\"", "warden");
            messageBus.broadcast("", "warden");
            messageBus.broadcast("§8§o\"¿Está... protegiendo algo? ¿O es una advertencia?\"", "warden");
            messageBus.broadcast("§8§o\"Esto... esto va más allá del End...\"", "warden");
            messageBus.broadcast("", "warden");
            messageBus.broadcast("§6§l⚠ ADVERTENCIA:", "warden");
            messageBus.broadcast("§7El Guardián de las Profundidades bloquea el paso.", "warden");
            messageBus.broadcast("§7Debe ser derrotado para continuar.", "warden");
            messageBus.broadcast("", "warden");
            messageBus.broadcast("§5§l§o\"...Hay fuerzas en juego que aún no comprendemos...\"", "warden");
            messageBus.broadcast("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", "warden");
            messageBus.broadcast("", "warden");
            
            // Título épico con el misterio del Warden
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.sendTitle("§4§l☠ GUARDIÁN DE LAS PROFUNDIDADES ☠", "§8§o¿Protector? ¿Advertencia? ¿Heraldo de algo peor?", 20, 100, 25);
            }
            
            // Efectos visuales masivos
            for (int i = 0; i < 100; i++) {
                spawnLoc.getWorld().spawnParticle(Particle.SCULK_SOUL, spawnLoc, 1, 
                    (random.nextDouble() - 0.5) * 5, random.nextDouble() * 3, (random.nextDouble() - 0.5) * 5, 0.1);
            }
            spawnLoc.getWorld().spawnParticle(Particle.EXPLOSION, spawnLoc, 20, 2, 1, 2, 0.5);
            
            // Iniciar task de monitoreo de distancia
            iniciarMonitoreoDistanciaWarden();
            
        }, 60L); // 3 segundos de tensión
    }
    
    private void iniciarMonitoreoDistanciaWarden() {
        if (wardenDistanceTask != null) {
            wardenDistanceTask.cancel();
        }
        
        wardenDistanceTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (wardenActivo == null || wardenActivo.isDead()) {
                if (wardenDistanceTask != null) wardenDistanceTask.cancel();
                return;
            }
            
            // Verificar si hay algún jugador cerca
            boolean hayJugadorCerca = false;
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (p.getLocation().distance(wardenActivo.getLocation()) <= WARDEN_MAX_DISTANCE) {
                    hayJugadorCerca = true;
                    break;
                }
            }
            
            // Si NO alcanzamos 40 fragmentos y no hay jugadores cerca, matar al Warden
            if (!hayJugadorCerca && fragmentosRecolectadosGlobalmente < 40) {
                messageBus.broadcast("", "warden_lejos");
                messageBus.broadcast("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", "warden_lejos");
                messageBus.broadcast("§e§l⚠ EL GUARDIÁN RETROCEDE §e§l⚠", "warden_lejos");
                messageBus.broadcast("", "warden_lejos");
                messageBus.broadcast("§7Todos los jugadores se alejaron demasiado.", "warden_lejos");
                messageBus.broadcast("", "warden_lejos");
                messageBus.broadcast("§5§l⚡ EL OBSERVADOR:", "warden_lejos");
                messageBus.broadcast("§8§o\"...Regresa a las profundidades...\"", "warden_lejos");
                messageBus.broadcast("§8§o\"...Su misión aún no está completa...\"", "warden_lejos");
                messageBus.broadcast("§c§o\"...Volverá cuando te acerques de nuevo...\"", "warden_lejos");
                messageBus.broadcast("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", "warden_lejos");
                messageBus.broadcast("", "warden_lejos");
                
                wardenActivo.getWorld().spawnParticle(Particle.PORTAL, wardenActivo.getLocation(), 200, 2, 2, 2, 1.0);
                wardenActivo.getWorld().spawnParticle(Particle.SCULK_SOUL, wardenActivo.getLocation(), 100, 1, 1, 1, 0.5);
                wardenActivo.getWorld().playSound(wardenActivo.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.0f, 0.5f);
                wardenActivo.getWorld().playSound(wardenActivo.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.8f, 0.6f);
                
                wardenActivo.remove();
                wardenSpawneado = false; // Permitir re-spawn
                
                if (wardenDistanceTask != null) {
                    wardenDistanceTask.cancel();
                    wardenDistanceTask = null;
                }
            }
            
            // Si ya alcanzamos 40 fragmentos, el Warden NO se va (es obligatorio matarlo)
            
        }, 100L, 100L); // Verificar cada 5 segundos
    }
    
    /**
     * Aplica efectos visuales progresivos durante la fase RESONANCIA
     */
    private void aplicarEfectosVisualesResonancia() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Location loc = p.getLocation();
            World world = loc.getWorld();
            
            // Efectos de corrupción visual
            if (fragmentosRecolectadosGlobalmente >= 30) {
                // Cerca del final: Darkness periódico
                if (random.nextDouble() < 0.3) {
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.DARKNESS, 80, 0, false, false, true));
                }
                
                // Partículas de Sculk Soul intensas
                world.spawnParticle(Particle.SCULK_SOUL, loc, 10, 3, 2, 3, 0.05);
                world.spawnParticle(Particle.SOUL, loc, 5, 2, 1, 2, 0.02);
            } else if (fragmentosRecolectadosGlobalmente >= 20) {
                // Mitad: Partículas más intensas
                world.spawnParticle(Particle.PORTAL, loc, 15, 2, 2, 2, 0.1);
                world.spawnParticle(Particle.REVERSE_PORTAL, loc, 8, 1.5, 1.5, 1.5, 0.05);
            } else {
                // Inicio: Partículas sutiles
                world.spawnParticle(Particle.PORTAL, loc, 5, 1, 1, 1, 0.05);
            }
            
            // Sonidos ambientales
            if (random.nextDouble() < 0.2) {
                p.playSound(loc, Sound.ENTITY_ENDERMAN_AMBIENT, 0.2f, 0.7f);
            }
        }
    }
    
    private void aplicarCorrupcionProgresiva() {
        double progreso = fragmentosRecolectadosGlobalmente / (double) FRAGMENTOS_OBJETIVO;
        
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            // Partículas de corrupción alrededor del jugador
            int cantidadParticulas = (int) (progreso * 15); // 0-15 partículas según progreso
            p.getWorld().spawnParticle(
                Particle.SCULK_SOUL,
                p.getLocation().add(0, 1, 0),
                cantidadParticulas,
                1.5, 1.5, 1.5,
                0.02
            );
            
            // Oscuridad ocasional al 50%+ progreso
            if (progreso >= 0.5 && random.nextInt(100) < 15) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0, false, false));
            }
            
            // Susurros personalizados
            if (random.nextInt(100) < 10) { // 10% de probabilidad
                String[] susurros = {
                    "§7§o...te observa...",
                    "§7§o...más cerca...",
                    "§7§o...casi aquí...",
                    "§7§o...no hay escape...",
                    "§7§o...el vacío llama...",
                    "§5§o...fragmentos...",
                    "§5§o...complétalo..."
                };
                p.sendMessage(susurros[random.nextInt(susurros.length)]);
                p.playSound(p.getLocation(), Sound.ENTITY_VEX_AMBIENT, 0.2f, 0.5f);
            }
        }
    }
    
    /**
     * Notifica a todos los jugadores la ubicación del portal revelado
     */
    private void notificarUbicacionPortal(Location portalLoc) {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            double distancia = p.getLocation().distance(portalLoc);
            int distanciaInt = (int) distancia;
            
            p.sendMessage("");
            p.sendMessage("§5§l⚡ UBICACIÓN DEL PORTAL REVELADA:");
            p.sendMessage("§7  Coordenadas: §e" + portalLoc.getBlockX() + "§7, §e" + portalLoc.getBlockY() + "§7, §e" + portalLoc.getBlockZ());
            p.sendMessage("§7  Distancia: §e~" + distanciaInt + " bloques");
            p.sendMessage("§7  Mundo: §e" + portalLoc.getWorld().getName());
            p.sendMessage("");
            
            // Partículas en dirección al portal
            Location direccion = portalLoc.clone().subtract(p.getLocation()).toVector().normalize()
                .multiply(3).toLocation(p.getWorld()).add(p.getEyeLocation());
            p.spawnParticle(Particle.END_ROD, direccion, 30, 0.5, 0.5, 0.5, 0.02);
            
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 0.8f);
        }
    }
    
    /**
     * Sistema de "ecos" - notifica a otros jugadores cuando alguien recolecta
     */
    private void notificarEcoRecoleccion(Player recolector, Location ubicacion) {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.equals(recolector)) continue; // Skip al recolector
            
            double distancia = p.getLocation().distance(ubicacion);
            
            // Solo notificar si están relativamente cerca (500 bloques)
            if (distancia > 500) continue;
            
            // Calcular dirección
            String direccion = obtenerDireccionCardinal(p.getLocation(), ubicacion);
            int distanciaInt = (int) distancia;
            
            // Mensaje sutil en action bar
            p.sendActionBar("§5✦ §7Eco detectado al §e" + direccion + " §7(§e~" + distanciaInt + "m§7)");
            
            // Partícula sutil en dirección
            Location particleLoc = p.getEyeLocation().add(
                ubicacion.clone().subtract(p.getLocation()).toVector().normalize().multiply(2)
            );
            p.spawnParticle(Particle.SOUL, particleLoc, 3, 0.1, 0.1, 0.1, 0.01);
            
            // Sonido muy sutil
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.2f, 1.8f);
        }
    }
    
    /**
     * Tensión ambiental progresiva durante RESONANCIA
     */
    private void aplicarTensionAmbiental() {
        int progreso = (fragmentosRecolectadosGlobalmente * 100) / FRAGMENTOS_OBJETIVO;
        
        // Solo aplicar si hay progreso significativo
        if (progreso < 20) return;
        
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            // Efectos que aumentan con el progreso
            if (progreso >= 50) {
                // 50%+ : Susurros ocasionales
                if (random.nextInt(3) == 0) {
                    p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 0.15f, 0.6f);
                }
            }
            
            if (progreso >= 70) {
                // 70%+ : Distorsión visual leve
                if (random.nextInt(5) == 0) {
                    p.spawnParticle(Particle.PORTAL, p.getEyeLocation(), 2, 0.5, 0.5, 0.5, 0);
                }
            }
            
            if (progreso >= 85) {
                // 85%+ : Latidos dimensionales
                p.playSound(p.getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.2f, 0.5f);
                
                // Mensaje ocasional en action bar
                if (random.nextInt(4) == 0) {
                    String[] susurros = {
                        "§5§o...lo sientes...?",
                        "§5§o...algo despierta...",
                        "§5§o...la barrera se quiebra...",
                        "§5§o...casi está aquí..."
                    };
                    p.sendActionBar(susurros[random.nextInt(susurros.length)]);
                }
            }
        }
    }
    
    /**
     * Corazón pulsante del portal - bloque central que cambia
     */
    private void iniciarCorazonPortal(Location centro) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            int tick = 0;
            
            @Override
            public void run() {
                if (!portalGenerado) return;
                
                World world = centro.getWorld();
                if (world == null) return;
                
                Location corazon = centro.clone();
                
                // Latido: alterna entre AIR y CRYING_OBSIDIAN
                tick++;
                boolean latido = (tick % 40) < 20; // Late cada 2 segundos
                
                if (latido) {
                    corazon.getBlock().setType(Material.CRYING_OBSIDIAN);
                    
                    // Efecto de latido
                    world.spawnParticle(Particle.SCULK_SOUL, corazon.clone().add(0.5, 1, 0.5), 5, 0.3, 0.3, 0.3, 0.02);
                    world.playSound(corazon, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.3f, 0.7f);
                } else {
                    corazon.getBlock().setType(Material.AIR);
                }
            }
        }, 0L, 1L);
    }
    
    /**
     * Obtiene dirección cardinal entre dos ubicaciones
     */
    private String obtenerDireccionCardinal(Location desde, Location hacia) {
        double dx = hacia.getX() - desde.getX();
        double dz = hacia.getZ() - desde.getZ();
        
        double angulo = Math.toDegrees(Math.atan2(dz, dx));
        if (angulo < 0) angulo += 360;
        
        if (angulo >= 337.5 || angulo < 22.5) return "Este";
        if (angulo >= 22.5 && angulo < 67.5) return "Sureste";
        if (angulo >= 67.5 && angulo < 112.5) return "Sur";
        if (angulo >= 112.5 && angulo < 157.5) return "Suroeste";
        if (angulo >= 157.5 && angulo < 202.5) return "Oeste";
        if (angulo >= 202.5 && angulo < 247.5) return "Noroeste";
        if (angulo >= 247.5 && angulo < 292.5) return "Norte";
        if (angulo >= 292.5 && angulo < 337.5) return "Noreste";
        
        return "Desconocida";
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE MINI-EVENTOS ALEATORIOS
    // ═══════════════════════════════════════════════════════════════════
    
    public enum MiniEvento {
        ECO_BRASAS(30, "§c§o\"El fuego... aún arde bajo tierra...\""),
        ECO_SOMBRAS(30, "§8§o\"Las sombras recuerdan...\""),
        ECO_PIEDRA(20, "§7§o\"Fragmentos de un mundo roto...\""),
        RESONANCIA(15, "§d§l✦ RESONANCIA"),
        OBSERVACION(35, "§5§l⚡ EL OBSERVADOR:");
        
        public final int probabilidad;
        public final String mensaje;
        
        MiniEvento(int probabilidad, String mensaje) {
            this.probabilidad = probabilidad;
            this.mensaje = mensaje;
        }
        
        public static MiniEvento obtenerAleatorio() {
            int total = 0;
            for (MiniEvento tipo : values()) {
                total += tipo.probabilidad;
            }
            
            int rand = new Random().nextInt(total);
            int acumulado = 0;
            
            for (MiniEvento tipo : values()) {
                acumulado += tipo.probabilidad;
                if (rand < acumulado) {
                    return tipo;
                }
            }
            return OBSERVACION;
        }
    }
    
    private void activarMiniEventoAleatorio() {
        MiniEvento evento = MiniEvento.obtenerAleatorio();
        
        switch (evento) {
            case ECO_BRASAS:
                miniEventoEcoBrasas();
                break;
            case ECO_SOMBRAS:
                miniEventoEcoSombras();
                break;
            case ECO_PIEDRA:
                miniEventoEcoPiedra();
                break;
            case RESONANCIA:
                miniEventoResonancia();
                break;
            case OBSERVACION:
                miniEventoObservacion();
                break;
        }
    }
    
    private void miniEventoEcoBrasas() {
        List<Player> jugadores = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        if (jugadores.isEmpty()) return;
        
        // Anuncio
        for (Player p : jugadores) {
            p.sendMessage("");
            p.sendMessage("§c§l✦ ECO DE BRASAS");
            p.sendMessage("§c§o\"El fuego... aún arde bajo tierra...\"");
            p.sendMessage("");
            p.playSound(p.getLocation(), Sound.BLOCK_LAVA_POP, 0.5f, 0.8f);
        }
        
        // Efecto: Lava aparece brevemente en el cielo
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    Location loc = p.getLocation().add(0, 15, 0);
                    p.getWorld().spawnParticle(Particle.FLAME, loc, 30, 5, 2, 5, 0.1);
                    p.getWorld().spawnParticle(Particle.LAVA, loc, 10, 5, 2, 5, 0);
                }
                
                ticks++;
                if (ticks >= 15) { // 15 segundos
                    // Spawn fragmento bonus cerca de un jugador aleatorio
                    if (!jugadores.isEmpty()) {
                        Player jugadorAleatorio = jugadores.get(random.nextInt(jugadores.size()));
                        Location spawnLoc = jugadorAleatorio.getLocation().add(
                            (random.nextDouble() - 0.5) * 30,
                            0,
                            (random.nextDouble() - 0.5) * 30
                        );
                        spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);
                        spawnearAnomalia(spawnLoc);
                        
                        for (Player p : plugin.getServer().getOnlinePlayers()) {
                            p.sendMessage("§7§o[El eco se desvanece... dejando algo atrás]");
                        }
                    }
                }
            }
        }, 0L, 20L);
        
        plugin.getServer().getScheduler().runTaskLater(plugin, task::cancel, 300L);
    }
    
    private void miniEventoEcoSombras() {
        List<Player> jugadores = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        if (jugadores.isEmpty()) return;
        
        // Anuncio
        for (Player p : jugadores) {
            p.sendMessage("");
            p.sendMessage("§8§l✦ ECO DE SOMBRAS");
            p.sendMessage("§8§o\"Las sombras recuerdan...\"");
            p.sendMessage("");
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.6f);
        }
        
        // Efecto: Oscuridad total durante 20 segundos
        for (Player p : jugadores) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 400, 1, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 400, 0, false, false)); // Para que no sea frustrante
        }
        
        // Partículas de sombra
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    Location loc = p.getLocation().add(0, 1, 0);
                    p.getWorld().spawnParticle(Particle.SQUID_INK, loc, 20, 3, 1, 3, 0.05);
                    p.getWorld().spawnParticle(Particle.SMOKE, loc, 15, 3, 1, 3, 0.1);
                }
                
                ticks++;
                if (ticks >= 20) { // 20 segundos
                    if (!jugadores.isEmpty()) {
                        Player jugadorAleatorio = jugadores.get(random.nextInt(jugadores.size()));
                        Location spawnLoc = jugadorAleatorio.getLocation().add(
                            (random.nextDouble() - 0.5) * 25,
                            0,
                            (random.nextDouble() - 0.5) * 25
                        );
                        spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);
                        spawnearAnomalia(spawnLoc);
                        
                        for (Player p : plugin.getServer().getOnlinePlayers()) {
                            p.sendMessage("§8§o[Las sombras retroceden...]");
                        }
                    }
                }
            }
        }, 0L, 20L);
        
        plugin.getServer().getScheduler().runTaskLater(plugin, task::cancel, 400L);
    }
    
    private void miniEventoEcoPiedra() {
        List<Player> jugadores = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        if (jugadores.isEmpty()) return;
        
        // Anuncio
        for (Player p : jugadores) {
            p.sendMessage("");
            p.sendMessage("§7§l✦ ECO DE PIEDRA ROTA");
            p.sendMessage("§7§o\"Fragmentos de un mundo roto...\"");
            p.sendMessage("");
            p.playSound(p.getLocation(), Sound.BLOCK_STONE_BREAK, 0.5f, 0.5f);
        }
        
        // Efecto: Bloques flotan brevemente
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    Location loc = p.getLocation().add(0, 3, 0);
                    p.getWorld().spawnParticle(Particle.ASH, loc, 40, 4, 2, 4, 0.05);
                    p.getWorld().spawnParticle(Particle.CLOUD, loc, 20, 4, 2, 4, 0.02);
                    p.getWorld().spawnParticle(Particle.BLOCK, loc, 15, 3, 2, 3, 0, Material.DEEPSLATE.createBlockData());
                }
                
                ticks++;
                if (ticks >= 10) { // 10 segundos
                    if (!jugadores.isEmpty()) {
                        Player jugadorAleatorio = jugadores.get(random.nextInt(jugadores.size()));
                        Location spawnLoc = jugadorAleatorio.getLocation().add(
                            (random.nextDouble() - 0.5) * 20,
                            0,
                            (random.nextDouble() - 0.5) * 20
                        );
                        spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);
                        spawnearAnomalia(spawnLoc);
                        
                        for (Player p : plugin.getServer().getOnlinePlayers()) {
                            p.sendMessage("§7§o[Los fragmentos caen... uno permanece]");
                        }
                    }
                }
            }
        }, 0L, 20L);
        
        plugin.getServer().getScheduler().runTaskLater(plugin, task::cancel, 200L);
    }
    
    private void miniEventoResonancia() {
        List<Player> jugadores = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        if (jugadores.isEmpty()) return;
        
        // Anuncio
        for (Player p : jugadores) {
            p.sendTitle(
                "§d§l✦ RESONANCIA ✦",
                "§7§oLas anomalías resuenan...",
                10, 60, 15
            );
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
        }
        
        // Efecto: Todas las anomalías brillan durante 10 segundos
        List<Location> anomaliasParaResaltar = new ArrayList<>(anomaliasActivas.keySet());
        
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                for (Location anomalia : anomaliasParaResaltar) {
                    if (!anomaliasActivas.containsKey(anomalia)) continue;
                    
                    World world = anomalia.getWorld();
                    if (world == null) continue;
                    
                    // Partículas brillantes intensas
                    world.spawnParticle(Particle.GLOW, anomalia.clone().add(0, 2, 0), 50, 0.5, 1.5, 0.5, 0.1);
                    world.spawnParticle(Particle.END_ROD, anomalia.clone().add(0, 3, 0), 30, 0.3, 1, 0.3, 0.05);
                    world.spawnParticle(Particle.ELECTRIC_SPARK, anomalia.clone().add(0, 2.5, 0), 20, 0.4, 1.2, 0.4, 0.02);
                    
                    // Sonido sutil
                    if (ticks % 10 == 0) {
                        world.playSound(anomalia, Sound.BLOCK_BEACON_AMBIENT, 0.3f, 1.8f);
                    }
                }
                
                ticks++;
            }
        }, 0L, 5L); // Cada 0.25 segundos para efecto intenso
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            task.cancel();
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.sendMessage("§7§o[La resonancia se desvanece...]");
            }
        }, 200L); // 10 segundos
    }
    
    private void miniEventoObservacion() {
        List<Player> jugadores = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        if (jugadores.isEmpty()) return;
        
        // Mensajes aleatorios del Observador
        String[] mensajes = {
            "§7§o\"Llevan... ¿cuánto tiempo? ¿Minutos? ¿Horas?\"",
            "§7§o\"El tiempo se distorsiona cerca del vacío.\"",
            "§8§o\"Veo sus movimientos... como sombras.\"",
            "§7§o\"Cada fragmento que recogen... me acerca.\"",
            "§8§o\"¿A qué? No lo sé. Aún.\"",
            "§7§o\"Puedo sentir... curiosidad. ¿Es eso lo que sienten?\"",
            "§8§o\"Este mundo... es diferente a los anteriores.\"",
            "§7§o\"Los veo buscar. Explorar. Como yo una vez.\"",
            "§8§o\"¿Recuerdo haber sido... como ustedes?\"",
            "§7§o\"No. Eso fue en otro tiempo. Otro lugar.\""
        };
        
        String mensajeSeleccionado = mensajes[random.nextInt(mensajes.length)];
        
        for (Player p : jugadores) {
            p.sendMessage("");
            p.sendMessage("§5§l⚡ EL OBSERVADOR:");
            p.sendMessage(mensajeSeleccionado);
            p.sendMessage("");
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.4f, 0.7f);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE DESAFÍO "CAZA DE ANOMALÍAS"
    // ═══════════════════════════════════════════════════════════════════
    
    private void ofrecerDesafioCaza() {
        // Anuncio dramático
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.sendMessage("");
                p.sendMessage("§5§l⚡ EL OBSERVADOR:");
                p.sendMessage("§7§o\"Puedo... sentir anomalías cercanas.\"");
                p.sendMessage("§7§o\"Si las encuentran rápido... daré una recompensa.\"");
                p.sendMessage("");
                p.sendMessage("§6§l[DESAFÍO INICIADO]");
                p.sendMessage("§7Encuentra §d3 anomalías§7 en §e5 minutos");
                p.sendMessage("");
                
                p.sendTitle(
                    "§e§l⚡ DESAFÍO ACTIVADO ⚡",
                    "§7Encuentra 3 anomalías en 5 minutos",
                    10, 70, 20
                );
                
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.2f);
                p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.5f);
            }
        });
        
        // Iniciar desafío después de 3 segundos
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            desafioCazaActivo = true;
            ticksDesafioCaza = 0;
            anomaliasEncontradasDesafio = 0;
            participantesDesafio.clear();
            
            plugin.getLogger().info("[CaminoEndEvent] Desafío 'Caza de Anomalías' iniciado");
        }, 60L);
    }
    
    private void completarDesafioCaza() {
        desafioCazaActivo = false;
        
        // Anuncio de éxito
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.sendMessage("");
            p.sendMessage("§a§l✓ DESAFÍO COMPLETADO");
            p.sendMessage("§5§l⚡ EL OBSERVADOR:");
            p.sendMessage("§7§o\"Impresionante... su velocidad... su coordinación...\"");
            p.sendMessage("§a§o\"Tomen esto. Lo han ganado.\"");
            p.sendMessage("");
            
            p.sendTitle(
                "§a§l✓ DESAFÍO COMPLETADO ✓",
                "§7El Observador está... satisfecho.",
                10, 60, 20
            );
            
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        }
        
        // Dar recompensas a los participantes
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (UUID uuid : participantesDesafio) {
                Player jugador = plugin.getServer().getPlayer(uuid);
                if (jugador != null && jugador.isOnline()) {
                    // 10 fragmentos bonus
                    ItemStack fragmento = items.crearFragmentoDelVacio();
                    fragmento.setAmount(10);
                    jugador.getInventory().addItem(fragmento);
                    
                    // 30 PS bonus (usando el sistema de experiencia si está disponible)
                    jugador.sendMessage("§a+ 10 Fragmentos del Vacío");
                    jugador.sendMessage("§a+ 30 PS");
                    
                    // Efectos visuales
                    jugador.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, 
                        jugador.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_EVOKER_CELEBRATE, 1.0f, 1.0f);
                }
            }
            
            fragmentosRecolectadosGlobalmente += 10; // Añadir al contador global
            
        }, 40L);
        
        plugin.getLogger().info("[CaminoEndEvent] Desafío 'Caza de Anomalías' completado - Participantes: " + participantesDesafio.size());
    }
    
    private void fallarDesafioCaza() {
        desafioCazaActivo = false;
        
        // Mensaje suave (no penalizar)
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.sendMessage("");
            p.sendMessage("§7§l⏱ Tiempo agotado");
            p.sendMessage("§5§l⚡ EL OBSERVADOR:");
            p.sendMessage("§7§o\"No importa... había otras.\"");
            p.sendMessage("§8§o\"El tiempo es... relativo aquí.\"");
            p.sendMessage("");
            
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 0.8f);
        }
        
        plugin.getLogger().info("[CaminoEndEvent] Desafío 'Caza de Anomalías' fallado - Encontradas: " + 
            anomaliasEncontradasDesafio + "/" + ANOMALIAS_REQUERIDAS_DESAFIO);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // CLASE INTERNA: DATOS DE ANOMALÍA
    // ═══════════════════════════════════════════════════════════════════
    
    public static class AnomaliaData {
        public final Location ubicacion;
        public final long tiempoSpawn;
        public boolean fragmentoObtenido;
        public final TipoAnomalia tipo;
        
        public AnomaliaData(Location ubicacion, long tiempoSpawn, TipoAnomalia tipo) {
            this.ubicacion = ubicacion;
            this.tiempoSpawn = tiempoSpawn;
            this.fragmentoObtenido = false;
            this.tipo = tipo;
        }
    }
    
    /**
     * Aplicar hitos épicos especiales en fragmentos específicos
     */
    private void aplicarHitosEspeciales(int fragmentos) {
        World world = plugin.getServer().getWorlds().get(0);
        
        // 25 fragmentos: PÁNICO - El Observador pierde la calma
        if (fragmentos == 25) {
            // Título dramático inicial
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.sendTitle(
                    "§4§l⚠ EL VACÍO ESTÁ DESPERTANDO",
                    "§c§oEl tiempo se agota...",
                    15, 80, 20
                );
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.3f, 0.5f);
            }
            
            // Mensaje del Observador después de 2 segundos (dar tiempo al título)
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    p.sendMessage("");
                    p.sendMessage("§5§l⚡ EL OBSERVADOR:");
                    p.sendMessage("§c§l§o\"¡NO, NO, NO! ¡ES DEMASIADO RÁPIDO!\"");
                }
            }, 40L);
            
            // Segundo mensaje 2 segundos después
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    p.sendMessage("§7§o\"¡Deben encontrar el resto AHORA!\"");
                    p.sendMessage("");
                    p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 0, false, false));
                }
            }, 80L);
            
            // Tormenta extrema
            if (world != null) {
                world.setStorm(true);
                world.setThundering(true);
                world.setThunderDuration(12000);
                // Rayos dramáticos cerca de jugadores
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    for (Player p : plugin.getServer().getOnlinePlayers()) {
                        Location strikeLocation = p.getLocation().add(
                            (Math.random() - 0.5) * 20,
                            0,
                            (Math.random() - 0.5) * 20
                        );
                        world.strikeLightningEffect(strikeLocation);
                    }
                }, 20L);
            }
        }
        
        // 35 fragmentos: DISTORSIÓN - La realidad se quiebra
        else if (fragmentos == 35) {
            // Título inicial de impacto
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.sendTitle(
                    "§5§l§k|||§r §4§lLA BARRERA SE ROMPE §5§l§k|||",
                    "§4§o¿Qué has hecho...?",
                    20, 90, 20
                );
                p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 0.5f);
            }
            
            // Primera línea del Observador después de 3 segundos
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    p.sendMessage("");
                    p.sendMessage("§5§l⚡ EL OBSERVADOR:");
                    p.sendMessage("§4§l§o\"SIENTO... ALGO... OBSERVÁNDOME...\"");
                    p.playSound(p.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 1.0f, 0.5f);
                }
            }, 60L);
            
            // Segunda línea más inquietante 2.5 segundos después
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    p.sendMessage("§7§o\"Está... aquí... con nosotros...\"");
                    p.sendMessage("");
                    
                    // Oscuridad temporal
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0, false, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 1, false, false));
                    
                    // Explosión de partículas SCULK_SOUL
                    p.getWorld().spawnParticle(
                        Particle.SCULK_SOUL,
                        p.getLocation().add(0, 1, 0),
                        100, 3, 3, 3, 0.1
                    );
                }
            }, 110L);
        }
        
        // 39 fragmentos: CUENTA REGRESIVA ÉPICA
        else if (fragmentos == 39) {
            // Título impactante inicial
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.sendTitle(
                    "§5§l§k|||§r §4§lUN FRAGMENTO MÁS §5§l§k|||",
                    "§c§l¡EL PORTAL ESTÁ CASI COMPLETO!",
                    20, 100, 15
                );
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 0.5f);
            }
            
            // Primer mensaje del Observador después de 3.5 segundos
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    p.sendMessage("");
                    p.sendMessage("§5§l⚡ EL OBSERVADOR:");
                    p.sendMessage("§4§l§o\"UN FRAGMENTO MÁS... SOLO UNO MÁS...\"");
                }
            }, 70L);
            
            // Grito final 2 segundos después
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    p.sendMessage("§c§l§o\"¡Y PODREMOS VERLO!\"");
                    p.sendMessage("");
                    p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 1.5f);
                }
            }, 110L);
            
            // Efecto visual global pulsante
            new BukkitRunnable() {
                int pulsos = 0;
                @Override
                public void run() {
                    if (pulsos >= 5) {
                        cancel();
                        return;
                    }
                    
                    for (Player p : plugin.getServer().getOnlinePlayers()) {
                        p.getWorld().spawnParticle(
                            Particle.SCULK_SOUL,
                            p.getLocation().add(0, 3, 0),
                            50, 5, 2, 5, 0.05
                        );
                        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.3f, 2.0f);
                    }
                    pulsos++;
                }
            }.runTaskTimer(plugin, 0L, 10L);
        }
    }
    
    /**
     * Tipos de anomalías con características únicas
     * 
     * NORMAL (40%): Anomalía estándar, fácil de recolectar
     * INESTABLE (25%): Spawna Enderman hostil, más fragmentos si se derrota rápido
     * ECO_* (20% total): Referencias a eventos pasados (Brasas/Sombras/Piedra)
     * OCULTA (10%): Invisible, requiere Brújula del Vacío para detectar
     * ANTIGUA (5%): Muy rara, puzzle especial con gran recompensa
     */
    public enum TipoAnomalia {
        NORMAL(1.0, "§7Normal", Particle.PORTAL, Sound.BLOCK_PORTAL_AMBIENT, null),
        INESTABLE(1.5, "§e§lInestable", Particle.SOUL_FIRE_FLAME, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, null),
        ECO_BRASAS(1.3, "§c§lEco de Brasas", Particle.FLAME, Sound.BLOCK_LAVA_POP, "§c§o\"Fuego que nunca murió...\""),
        ECO_SOMBRAS(1.3, "§8§lEco de Sombras", Particle.SQUID_INK, Sound.ENTITY_ENDERMAN_TELEPORT, "§8§o\"Se mueven... como lo hice yo...\""),
        ECO_PIEDRA(1.3, "§7§lEco de Piedra", Particle.ASH, Sound.BLOCK_STONE_BREAK, "§7§o\"Memorias rotas...\""),
        OCULTA(1.6, "§d§lOculta", Particle.END_ROD, Sound.BLOCK_AMETHYST_BLOCK_CHIME, null),
        ANTIGUA(2.0, "§5§l§kA§r §5§lAntigua§r §5§l§kA", Particle.DRAGON_BREATH, Sound.ENTITY_ENDER_DRAGON_AMBIENT, null);
        
        public final double multiplicadorPS;  // Multiplicador de recompensa
        public final String nombre;
        public final Particle particula;
        public final Sound sonido;
        public final String mensajeObservador; // Mensaje del Observador al encontrarla
        
        TipoAnomalia(double multiplicadorPS, String nombre, Particle particula, Sound sonido, String mensajeObservador) {
            this.multiplicadorPS = multiplicadorPS;
            this.nombre = nombre;
            this.particula = particula;
            this.sonido = sonido;
            this.mensajeObservador = mensajeObservador;
        }
        
        public String getNombre() {
            return nombre;
        }
        
        public boolean esEco() {
            return this == ECO_BRASAS || this == ECO_SOMBRAS || this == ECO_PIEDRA;
        }
        
        public static TipoAnomalia obtenerAleatorio(Random random) {
            int valor = random.nextInt(100);
            
            if (valor < 40) return NORMAL;           // 40% normal
            if (valor < 65) return INESTABLE;        // 25% inestable
            if (valor < 85) {                        // 20% ecos (distribuido)
                int eco = random.nextInt(3);
                if (eco == 0) return ECO_BRASAS;
                if (eco == 1) return ECO_SOMBRAS;
                return ECO_PIEDRA;
            }
            if (valor < 95) return OCULTA;           // 10% oculta
            return ANTIGUA;                           // 5% antigua (rara)
        }
    }
}

