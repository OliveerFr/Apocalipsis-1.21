package me.apocalipsis.events;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.Rotation;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;
import me.apocalipsis.events.gameplay.EnvironmentSystem;
import me.apocalipsis.events.gameplay.EnvironmentSystem.WeatherType;
import me.apocalipsis.events.gameplay.EnvironmentSystem.FogIntensity;
import me.apocalipsis.events.gameplay.EnvironmentSystem.AtmosphericEffect;

/**
 * El Susurro en la Piedra Rota - Mini-evento narrativo de 3 actos
 * 
 * Contexto narrativo:
 * Después del Eco de Sombras, algo "de afuera" reaccionó.
 * El mundo comienza a recordar mal - fragmentos de memoria se manifiestan físicamente.
 * Los altares antiguos "glitchean", creando copias defectuosas de criaturas.
 * 
 * Este evento representa la memoria fragmentada del mundo, no una entidad comunicandose.
 * El Observador deja pensamientos crípticos (...) como rastros de su presencia.
 * 
 * Actos del evento:
 * 1. LOS SUSURROS APARECEN (5 min): 3-5 altares rotos aparecen con
 *    susurros fragmentados del mundo
 * 2. UN MAL RECUERDO DESPIERTA (5 min): El altar recuerda mal y genera
 *    copias defectuosas de criaturas - errores de memoria
 * 3. EL ECO RESUENA (5 min): Aparece el núcleo corrupto (item único permanente)
 *    con teaser del End - "algo más grande despierta en el vacío"
 */
public class SusurroPiedraRotaEvent extends EventBase {
    
    // ═══════════════════════════════════════════════════════════════════
    // ESTADO DEL EVENTO
    // ═══════════════════════════════════════════════════════════════════
    
    public enum Acto {
        INTRO,                  // Mensaje inicial
        PIEDRA_DESPIERTA,       // Acto 1
        TRANSICION_2,           // Cinemática
        PIEDRA_QUIEBRA,         // Acto 2
        TRANSICION_3,           // Cinemática
        NUCLEO_FORMA,           // Acto 3
        TRANSICION_4,           // Cinemática
        SEGUNDO_SUSURRO,        // Acto 4
        VICTORIA                // Final
    }
    
    // ✨ Sistema de dificultad ajustable
    public enum Dificultad {
        FACIL(3, 0, "§a[Fácil]"),        // 3 fragmentos, sin límite de tiempo
        NORMAL(5, 0, "§e[Normal]"),      // 5 fragmentos, sin límite de tiempo
        DIFICIL(7, 600, "§c[Difícil]");  // 7 fragmentos, 10 minutos límite
        
        private final int numFragmentos;
        private final int tiempoLimiteSegundos; // 0 = sin límite
        private final String displayName;
        
        Dificultad(int fragmentos, int tiempoLimite, String nombre) {
            this.numFragmentos = fragmentos;
            this.tiempoLimiteSegundos = tiempoLimite;
            this.displayName = nombre;
        }
        
        public int getNumFragmentos() { return numFragmentos; }
        public int getTiempoLimite() { return tiempoLimiteSegundos; }
        public String getDisplayName() { return displayName; }
        public boolean tieneLimite() { return tiempoLimiteSegundos > 0; }
    }
    
    private Acto actoActual;
    private Dificultad dificultadEvento = Dificultad.NORMAL; // Por defecto Normal
    private int ticksEnActo;
    private int ticksTotales;
    
    // Configuración cargada de eventos.yml
    private ConfigurationSection config;
    
    // ═══════════════════════════════════════════════════════════════════
    // ACTO 1: FRAGMENTOS DE PIEDRA ROTA - SISTEMA DE ALTARES
    // ═══════════════════════════════════════════════════════════════════
    
    private List<Location> fragmentosLocations = new ArrayList<>();
    private Set<Location> fragmentosInspeccionados = new HashSet<>(); // Fragmentos completados (altares terminados)
    private Set<Location> fragmentosDescubiertos = new HashSet<>(); // Fragmentos descubiertos (jugador llegó cerca)
    private Map<UUID, Set<Location>> jugadoresFragmentosVistos = new HashMap<>();
    private BukkitTask fragmentosParticleTask;
    
    // Sistema de Altares
    private Map<Location, Integer> fragmentoANumeroAltar = new HashMap<>(); // Mapea fragmento → número de altar (1-5)
    private Map<UUID, Set<Integer>> altaresCompletadosPorJugador = new HashMap<>(); // Jugador → altares completados
    private Map<UUID, Integer> altarActualJugador = new HashMap<>(); // Jugador → altar en el que está
    private Map<UUID, Long> tiempoInicioAltarJugador = new HashMap<>(); // Para Altar 1 (permanecer quieto)
    private Map<UUID, Location> posicionInicioAltarJugador = new HashMap<>(); // Para detectar movimiento
    private Map<UUID, Double> vidaInicioAltarJugador = new HashMap<>(); // Para Altar 3 (sacrificio) - LEGACY
    private Map<UUID, Integer> criaturasEliminadasPorJugador = new HashMap<>(); // Para conteo de kills
    private int itemsSacrificadosAltar3 = 0; // Para Altar 3 (sacrificio de items valiosos)
    private Set<UUID> itemsProcesadosSacrificio = new HashSet<>(); // Items ya procesados en sacrificio
    private int mobsHostilesEliminadosAltar4 = 0; // Para Altar 4 (caza de mobs)
    private Location altarActualLocation = null; // Ubicación del altar en progreso
    private Set<UUID> criaturasDeAltar = new HashSet<>(); // UUIDs de criaturas spawneadas por altares
    
    // Sistema de sincronización de altares
    private int altarActualGlobal = 1; // El altar actual que debe completarse (1-5)
    private Set<UUID> jugadoresPresentesEnAltar = new HashSet<>(); // Jugadores cerca del altar actual
    private boolean altarEnProgreso = false; // Si hay una actividad de altar en curso
    private boolean esperandoJugadores = false; // Si está esperando a que lleguen todos
    private long tiempoInicioEspera = 0; // Cuando empezó a esperar jugadores
    private long tiempoInicioActividad = 0; // Cuando empezó la actividad actual (para timeout)
    private static final long TIMEOUT_ACTIVIDAD_MS = 180000; // 3 minutos de timeout para actividades
    private int intentosFallidosActividad = 0; // Contador de fallos para fallback
    
    // ═══════════════════════════════════════════════════════════════════
    // ACTO 2: GRIETA DE FORMA
    // ═══════════════════════════════════════════════════════════════════
    
    private Location grietaLocation;
    private BukkitTask grietaParticleTask;
    private BukkitTask grietaSoundTask;
    
    // Oleadas de criaturas
    private int oleadaActual = 0;
    private int oleadasTotales = 3;
    private List<Entity> criaturasActivas = new ArrayList<>();
    private Map<UUID, String> tipoCriatura = new HashMap<>(); // Tipo: RAPIDA, TANQUE, EXPLOSIVA
    private Map<UUID, Integer> glowIntensidad = new HashMap<>(); // Intensidad glow 0-100
    private String tipoOleadaActual = "MIXTO"; // MIXTO, RAPIDA, TANQUE, EXPLOSIVA, INVOCADORA
    private BukkitTask glowPulsanteTask;
    private boolean oleadasCompletadas = false;
    private int oleadasCompletadasTotal = 0; // Contador para boss cada 3 oleadas
    private boolean bossActivo = false; // Para evitar spawns múltiples
    private long tiempoInicioOleadaActual = 0; // Para timeout de oleadas
    private static final long TIMEOUT_OLEADA_MS = 180000; // 3 minutos de timeout para oleadas
    
    // ═══════════════════════════════════════════════════════════════════
    // ACTO 3: NÚCLEO DE FORMA
    // ═══════════════════════════════════════════════════════════════════
    
    private Location nucleoLocation;
    private ItemFrame nucleoFrame;  // Item frame invisible que muestra el núcleo
    private BukkitTask nucleoParticleTask;
    private BukkitTask nucleoBeamTask;
    private BukkitTask nucleoOrbitasTask;
    private BukkitTask nucleoRelampagosTask;
    private BukkitTask nucleoSpawnTask; // Task para spawn continuo de criaturas
    private boolean nucleoRecogido = false;
    private UUID jugadorQueRecogio = null;
    private double anguloOrbita1 = 0;
    private double anguloOrbita2 = 0;
    private double anguloOrbita3 = 0;
    private int latidoContador = 0;
    
    // Ritual de destrucción del núcleo
    private boolean ritualDestruccionIniciado = false;
    private boolean ritualDestruccionCompletado = false;
    private int ticksRitualDestruccion = 0;
    private Location altarLocation = null; // Ubicación del primer fragmento (altar)
    private ItemFrame pedestalNucleo = null; // Pedestal donde se coloca el núcleo
    private BukkitTask retornoSpawnTask = null; // Task para spawns durante el retorno al altar
    
    // Banderas de completado de actos (evitar múltiples llamadas)
    private boolean acto1Completado = false;
    private boolean acto2Completado = false;
    private boolean acto3Completado = false;
    
    // Sistema de ambiente avanzado
    private EnvironmentSystem environmentSystem;
    
    // Tracking de iluminación
    private long tiempoOriginal = 0;
    private BukkitTask atmosferaTask;
    
    // Sistema de bonos y recompensas progresivas
    private long tiempoInicioEvento = 0;
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE NARRATIVA Y AUDIO DINÁMICO
    // ═══════════════════════════════════════════════════════════════════
    
    private BukkitTask audioTask;
    private int intensidadAudio = 0; // 0-100
    private long ultimoDialogoForma = 0;
    private int contadorMuertes = 0;
    private int contadorKills = 0;
    private List<String> dialogosFormaUsados = new ArrayList<>();
    private Map<UUID, Integer> rendimientoJugador = new HashMap<>(); // Puntuación final
    private boolean finalAlternativoActivado = false;
    private long tiempoCompletadoEvento = 0;
    private Map<UUID, Long> tiemposFragmentos = new HashMap<>(); // Tiempo de inspección por fragmento
    private Map<UUID, Integer> puzzlesCompletados = new HashMap<>(); // Puzzles completados por jugador
    private Map<UUID, String> rangoRecompensa = new HashMap<>(); // PLATINUM, GOLD, SILVER, BRONZE
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE LORE COLECCIONABLE
    // ═══════════════════════════════════════════════════════════════════
    
    private List<ItemFrame> librosLore = new ArrayList<>();
    private Map<UUID, Set<String>> loreRecolectado = new HashMap<>(); // Jugador -> IDs de lore
    
    // ✨ Sistema de tiempo límite (modo difícil)
    private BukkitTask tiempoLimiteTask;
    private long tiempoInicioConLimite = 0;
    private boolean tiempoLimiteActivo = false;
    
    // Sistema de hints progresivos
    private int ultimoHintFragmentos = 0;
    private int ultimoHintOleadas = 0;
    private int ultimoHintLaberinto = 0;
    
    // Sistema de combos
    private Map<UUID, Integer> combosJugador = new HashMap<>(); // Racha de kills
    private Map<UUID, Long> ultimoKillJugador = new HashMap<>(); // Tiempo del último kill
    
    // Sistema de audio cinematográfico
    private BukkitTask musicaTask;
    private BukkitTask sonidosAmbientalesTask;
    private int actoActualMusica = 0;
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE PROGRESO VISUAL (BOSSBAR)
    // ═══════════════════════════════════════════════════════════════════
    
    private BossBar bossBarProgreso;
    private BukkitTask bossBarUpdateTask;
    private long tiempoInicioActo = 0;
    private int duracionObjetivoMinutos = 18; // Objetivo: 15-20 minutos
    
    // ═══════════════════════════════════════════════════════════════════
    // TRACKING DE PARTICIPACIÓN
    // ═══════════════════════════════════════════════════════════════════
    
    private Map<UUID, Integer> participacionFragmentos = new HashMap<>();
    private Map<UUID, Integer> participacionCriaturas = new HashMap<>();
    private Set<UUID> participantesOriginales = new HashSet<>();
    
    // Sistema de guía con action bar
    private BukkitTask guiaActionBarTask;
    private Map<UUID, Location> objetivosPorJugador = new ConcurrentHashMap<>();
    
    // Sistema de efectos de proximidad
    private BukkitTask proximidadTask;
    private Map<UUID, Integer> ultimoIndicadorDistancia = new HashMap<>();
    
    // Sistema de detección de items en área de altar
    private Set<UUID> itemsProcesadosEnAltar = new HashSet<>(); // Items ya consumidos (evitar duplicados)
    private int perlasEntregadasAltar2 = 0; // Contador global de perlas para Altar 2
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE PUZZLES Y MINIJUEGOS
    // ═══════════════════════════════════════════════════════════════════
    
    // Acto 2: Puzzle de memoria de patrones
    private Location altarDefensa;
    private int saludAltar = 100;
    private BukkitTask defensaTask;
    private boolean defensaCompletada = false;
    private List<Location> bloquesPatron = new ArrayList<>();
    private List<Integer> patronActual = new ArrayList<>();
    private List<Integer> patronJugador = new ArrayList<>();
    private int nivelPatron = 3;
    
    // Acto 3: Puzzle de laberinto
    private List<String> simbolosCorrectos = Arrays.asList("⧖", "◆", "✦", "★");
    private Map<UUID, List<String>> simbolosJugador = new HashMap<>();
    private boolean puzzleActo3Completado = false;
    private List<Location> caminoCorrecto = new ArrayList<>();
    private List<Location> caminosFalsos = new ArrayList<>();
    
    // Sistema de iluminación dinámica
    private List<Location> bloquesLuzTemporales = new ArrayList<>();
    
    // Sistema de breadcrumbs
    private Map<UUID, List<Location>> breadcrumbsPorJugador = new HashMap<>();
    private BukkitTask breadcrumbsTask;
    
    // ✨ Sistema de beacons preview
    private Map<Location, org.bukkit.block.Block> beaconsPreview = new HashMap<>();
    
    // ✨ Sistema de bonus por velocidad
    private long tiempoInicioActo1 = 0;
    private static final long TIEMPO_BONUS_ORO = 120000; // 2 minutos
    private static final long TIEMPO_BONUS_PLATA = 180000; // 3 minutos
    private static final long TIEMPO_BONUS_BRONCE = 240000; // 4 minutos
    
    // ⚡ OPTIMIZACIÓN: Control de búsqueda de ubicaciones
    private boolean primerFragmentoEncontroLugarPerfecto = true;
    
    /**
     * ✨ Iniciar contador de tiempo límite para modo difícil
     */
    private void iniciarTiempoLimite() {
        if (!dificultadEvento.tieneLimite()) return;
        
        tiempoInicioConLimite = System.currentTimeMillis();
        tiempoLimiteActivo = true;
        
        tiempoLimiteTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isActive() || !tiempoLimiteActivo) {
                if (tiempoLimiteTask != null) {
                    tiempoLimiteTask.cancel();
                }
                return;
            }
            
            long tiempoTranscurrido = (System.currentTimeMillis() - tiempoInicioConLimite) / 1000;
            long tiempoRestante = dificultadEvento.getTiempoLimite() - tiempoTranscurrido;
            
            if (tiempoRestante <= 0) {
                // ¡Tiempo agotado!
                tiempoLimiteTask.cancel();
                tiempoLimiteActivo = false;
                
                broadcastNarrative("§c§l⏱ ¡TIEMPO AGOTADO!");
                broadcastNarrative("§7El susurro se desvanece... Has fallado.");
                playSoundToAll(Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    stop();
                }, 60L);
                return;
            }
            
            // Avisos cada minuto y en últimos 30 segundos
            if (tiempoRestante == 300 || tiempoRestante == 240 || tiempoRestante == 180 || 
                tiempoRestante == 120 || tiempoRestante == 60 || tiempoRestante == 30 || 
                tiempoRestante == 10) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(String.format("§c⏱ Tiempo restante: %d segundos", tiempoRestante));
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, tiempoRestante <= 30 ? 2.0f : 1.0f);
                }
            }
            
        }, 0L, 20L); // Cada segundo
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════
    
    public SusurroPiedraRotaEvent(Apocalipsis plugin, MessageBus messageBus, SoundUtil soundUtil) {
        super(plugin, messageBus, soundUtil, "susurro_piedra_rota");
        this.environmentSystem = new EnvironmentSystem(plugin);
        loadConfig();
    }
    
    private void loadConfig() {
        config = plugin.getConfigManager().getEventosConfig()
            .getConfigurationSection("eventos.susurro_piedra_rota");
        
        if (config == null) {
            plugin.getLogger().warning("[SusurroPiedraRota] Configuración no encontrada en eventos.yml");
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // MÉTODOS ABSTRACTOS IMPLEMENTADOS
    // ═══════════════════════════════════════════════════════════════════
    
    @Override
    public void onStart() {
        actoActual = Acto.INTRO;
        ticksEnActo = 0;
        ticksTotales = 0;
        
        // Registrar participantes originales
        for (Player p : Bukkit.getOnlinePlayers()) {
            participantesOriginales.add(p.getUniqueId());
        }
        
        // ✨ NUEVO: Crear BossBar de progreso
        bossBarProgreso = Bukkit.createBossBar(
            "§5◆ El Susurro en la Piedra Rota ◆",
            org.bukkit.boss.BarColor.PURPLE,
            org.bukkit.boss.BarStyle.SEGMENTED_10
        );
        bossBarProgreso.setProgress(0.0);
        for (Player p : Bukkit.getOnlinePlayers()) {
            bossBarProgreso.addPlayer(p);
        }
        
        // Configurar atmósfera dramática AAA (VISIBLE)
        World world = Bukkit.getWorlds().get(0);
        tiempoOriginal = world.getTime();
        
        // Sistema de ambiente cinematográfico - CREPÚSCULO para visibilidad
        environmentSystem.adjustWorldLighting(world, 13000, true); // Atardecer/crepúsculo (VISIBLE)
        // ❌ VOID_SKY REMOVIDO - Aplicaba darkness INFINITO que dejaba ciegos a los jugadores
        environmentSystem.createVolumetricFog(world, FogIntensity.LIGHT, 0); // Niebla LIGERA (no bloquea visión)
        environmentSystem.spawnAtmosphericEffect(world, AtmosphericEffect.SHADOW_WISPS, 0); // Wisps oscuros
        
        // 🎬 DARKNESS INICIAL de 5 segundos para ambientación dramática
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.DARKNESS, 100, 0, false, false
            ));
        }
        
        // Iniciar efectos atmosféricos y de audio periódicos
        iniciarAtmosfera();
        iniciarSistemaAudio();
        
        // Mensaje de transición atmosférica
        enviarTituloCinematicoTodos(
            "§8§l◆ ◆ ◆",
            "La oscuridad desciende sobre el mundo...",
            40
        );
        
        // Iniciar sistema de guía
        iniciarGuiaActionBar();
        
        // Iniciar efectos de proximidad
        iniciarEfectosProximidad();
        
        // 🎯 Iniciar actualización de BossBar de progreso
        tiempoInicioActo = System.currentTimeMillis();
        iniciarActualizacionBossBar();
        
        // Mensaje inicial
        mostrarMensajeInicio();
        
        // Programar inicio del Acto 1
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                iniciarActo1();
            }
        }, 100L); // 5 segundos después del inicio
    }
    
    @Override
    public void onStop() {
        // Restaurar atmósfera original con fade out
        World world = Bukkit.getWorlds().get(0);
        
        // Cleanup completo del sistema de ambiente
        environmentSystem.cleanupWorld(world);
        environmentSystem.restoreAllBlocks();
        
        // Restaurar tiempo original
        world.setTime(tiempoOriginal);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        
        // Detener efectos atmosféricos y de audio
        if (atmosferaTask != null) atmosferaTask.cancel();
        if (musicaTask != null) musicaTask.cancel();
        if (sonidosAmbientalesTask != null) sonidosAmbientalesTask.cancel();
        
        // Limpiar fragmentos
        limpiarFragmentos();
        
        // Limpiar grieta
        limpiarGrieta();
        
        // Limpiar núcleo
        limpiarNucleo();
        
        // ✨ NUEVO: Limpiar bloques de luz temporales
        for (Location luzLoc : bloquesLuzTemporales) {
            if (luzLoc != null && luzLoc.getBlock().getType() == Material.LIGHT) {
                luzLoc.getBlock().setType(Material.AIR);
            }
        }
        bloquesLuzTemporales.clear();
        
        // ✨ NUEVO: Remover BossBar
        if (bossBarProgreso != null) {
            bossBarProgreso.removeAll();
            bossBarProgreso = null;
        }
        
        // 🎯 Detener actualización de BossBar
        if (bossBarUpdateTask != null) {
            bossBarUpdateTask.cancel();
            bossBarUpdateTask = null;
        }
        
        // Detener sistema de guía
        detenerGuiaActionBar();
        
        // Detener efectos de proximidad
        if (proximidadTask != null) {
            proximidadTask.cancel();
            proximidadTask = null;
        }
        
        // Cancelar tasks
        if (fragmentosParticleTask != null) fragmentosParticleTask.cancel();
        if (grietaParticleTask != null) grietaParticleTask.cancel();
        if (grietaSoundTask != null) grietaSoundTask.cancel();
        if (nucleoParticleTask != null) nucleoParticleTask.cancel();
        if (nucleoBeamTask != null) nucleoBeamTask.cancel();
        if (nucleoOrbitasTask != null) nucleoOrbitasTask.cancel();
        if (nucleoRelampagosTask != null) nucleoRelampagosTask.cancel();
        if (glowPulsanteTask != null) glowPulsanteTask.cancel();
        if (defensaTask != null) defensaTask.cancel();
        
        // Matar criaturas activas
        for (Entity criatura : criaturasActivas) {
            if (criatura != null && criatura.isValid()) {
                criatura.remove();
            }
        }
        criaturasActivas.clear();
        tipoCriatura.clear();
        glowIntensidad.clear();
        
        plugin.getLogger().info("[SusurroPiedraRota] Evento detenido y limpiado completamente");
    }
    
    /**
     * Limpieza completa de TODAS las estructuras creadas durante el evento
     */
    private void limpiezaCompletaEvento() {
        World world = Bukkit.getWorlds().get(0);
        
        // 1. LIMPIAR LABERINTO (35x35)
        if (nucleoLocation != null) {
            int radioLimpieza = 18;
            int centerX = nucleoLocation.getBlockX();
            int centerY = nucleoLocation.getBlockY();
            int centerZ = nucleoLocation.getBlockZ();
            
            for (int x = -radioLimpieza; x <= radioLimpieza; x++) {
                for (int z = -radioLimpieza; z <= radioLimpieza; z++) {
                    // Restaurar a aire desde 2 bloques abajo hasta 5 arriba
                    for (int dy = -2; dy <= 5; dy++) {
                        Location loc = new Location(world, centerX + x, centerY + dy, centerZ + z);
                        // Solo restaurar bloques artificiales (obsidian, glowstone, soul sand, smooth stone)
                        Material mat = loc.getBlock().getType();
                        if (mat == Material.OBSIDIAN || mat == Material.GLOWSTONE || 
                            mat == Material.SOUL_SAND || mat == Material.SMOOTH_STONE ||
                            mat == Material.MOSSY_COBBLESTONE) {
                            loc.getBlock().setType(Material.AIR);
                        }
                    }
                }
            }
            plugin.getLogger().info("[SusurroPiedraRota] Laberinto limpiado (35x35)");
        }
        
        // 2. LIMPIAR ALTAR DE DEFENSA (Acto 2)
        if (altarDefensa != null) {
            int radioAltar = 8;
            int ax = altarDefensa.getBlockX();
            int ay = altarDefensa.getBlockY();
            int az = altarDefensa.getBlockZ();
            
            for (int x = -radioAltar; x <= radioAltar; x++) {
                for (int z = -radioAltar; z <= radioAltar; z++) {
                    for (int dy = -1; dy <= 3; dy++) {
                        Location loc = new Location(world, ax + x, ay + dy, az + z);
                        Material mat = loc.getBlock().getType();
                        // Restaurar bloques del altar
                        if (mat == Material.QUARTZ_BLOCK || mat == Material.CHISELED_QUARTZ_BLOCK ||
                            mat == Material.GOLD_BLOCK || mat == Material.DIAMOND_BLOCK ||
                            mat.name().contains("WOOL")) {
                            loc.getBlock().setType(Material.AIR);
                        }
                    }
                }
            }
            plugin.getLogger().info("[SusurroPiedraRota] Altar de defensa limpiado");
        }
        
        // 3. LIMPIAR FRAGMENTOS TERRAFORMADOS (17x17 cada uno)
        for (Location frag : fragmentosLocations) {
            int radioFrag = 8;
            int fx = frag.getBlockX();
            int fy = frag.getBlockY();
            int fz = frag.getBlockZ();
            
            for (int x = -radioFrag; x <= radioFrag; x++) {
                for (int z = -radioFrag; z <= radioFrag; z++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        Location loc = new Location(world, fx + x, fy + dy, fz + z);
                        Material mat = loc.getBlock().getType();
                        // Solo limpiar bloques artificiales
                        if (mat == Material.STONE || mat == Material.MOSSY_COBBLESTONE) {
                            loc.getBlock().setType(Material.AIR);
                        }
                    }
                }
            }
        }
        plugin.getLogger().info("[SusurroPiedraRota] " + fragmentosLocations.size() + " áreas de fragmentos limpiadas");
        
        // 4. LIMPIAR LISTAS
        caminoCorrecto.clear();
        caminosFalsos.clear();
        // bloquesPatron.clear(); // COMENTADO - Variable eliminada
        
        // 5. LIMPIAR CONTADORES DE ALTARES
        perlasEntregadasAltar2 = 0;
        itemsSacrificadosAltar3 = 0;
        itemsProcesadosSacrificio.clear();
        mobsHostilesEliminadosAltar4 = 0;
        altarActualLocation = null;
        itemsProcesadosEnAltar.clear();
        
        broadcastNarrative("§7✨ El mundo ha sido restaurado...");
        plugin.getLogger().info("§a[SusurroPiedraRota] Limpieza completa terminada");
    }
    
    @Override
    public void onTick() {
        ticksEnActo++;
        ticksTotales++;
        
        // Actualizar según acto actual
        switch (actoActual) {
            case PIEDRA_DESPIERTA:
                tickActo1();
                break;
            case PIEDRA_QUIEBRA:
                tickActo2();
                break;
            case NUCLEO_FORMA:
                tickActo3();
                break;
            case SEGUNDO_SUSURRO:
                tickActo4();
                break;
            default:
                break;
        }
        
        // Verificar proximidad a fragmentos (Acto 1)
        if (actoActual == Acto.PIEDRA_DESPIERTA) {
            verificarProximidadFragmentos();
        }
        
        // Verificar proximidad al núcleo (Acto 3)
        if (actoActual == Acto.NUCLEO_FORMA && !nucleoRecogido) {
            verificarProximidadNucleo();
        }
    }
    
    @Override
    public String getDisplayName() {
        return "§5El Susurro en la Piedra Rota";
    }
    
    @Override
    public String getDescription() {
        return "Mini-evento narrativo que introduce el concepto de 'forma'";
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // MENSAJES Y CINEMATICAS
    // ═══════════════════════════════════════════════════════════════════
    
    private void mostrarMensajeInicio() {
        // Iniciar sistema de audio dinámico
        iniciarSistemaAudioDinamico();
        
        // Primer diálogo de La Forma
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            mostrarDialogoForma("INICIO");
        }, 100L);
        
        // Introducción narrativa con animación
        for (Player p : Bukkit.getOnlinePlayers()) {
            mostrarIntroduccionAnimada(p);
        }
    }
    
    /**
     * Muestra la introducción con animación cinematográfica.
     */
    private void mostrarIntroduccionAnimada(Player player) {
        // Fade a negro
        oscurecerProgresivo(player, 3);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            player.sendMessage("");
            player.sendMessage("§8§m                                                    ");
            
            // Primera línea con efecto
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(formatearCentrado("§7Un susurro se repite entre la piedra..."));
                soundUtil.playSound(player, Sound.BLOCK_STONE_BREAK, 0.5f, 0.5f);
                soundUtil.playSound(player, Sound.ENTITY_ENDERMAN_STARE, 0.3f, 0.7f);
            }, 20L);
            
            // Segunda línea
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(formatearCentrado("§7Algo está intentando formarse."));
                soundUtil.playSound(player, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.4f, 0.6f);
                
                // Partículas místicas
                Location loc = player.getLocation();
                loc.getWorld().spawnParticle(Particle.SOUL, loc, 20, 2, 1, 2, 0.05);
            }, 60L);
            
            // Cierre
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage("§8§m                                                    ");
                player.sendMessage("");
                
                // Destello sutil
                player.sendTitle("", "§5⧖", 5, 10, 10);
            }, 100L);
        }, 40L);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ACTO 1: LA PIEDRA ROTA DESPIERTA
    // ═══════════════════════════════════════════════════════════════════
    
    private void iniciarActo1() {
        actoActual = Acto.PIEDRA_DESPIERTA;
        ticksEnActo = 0;
        
        plugin.getLogger().info("[SusurroPiedraRota] Iniciando Acto 1: La Piedra Rota Despierta");
        
        // ✨ NUEVO: Limpiar mobs hostiles para la narrativa inicial
        limpiarMobsHostilesCercanos();
        
        // 🎬 SECUENCIA CINEMATOGRÁFICA MEJORADA
        efectoCinematograficoIntro();
        
        // 🗣️ DIÁLOGO DE INICIO - La Forma se presenta
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                mostrarDialogoForma("INICIO");
                
                // 🎬 Pausa dramática después del diálogo inicial
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (isActive()) {
                        broadcastNarrative("§d§l⧖ La búsqueda comienza...");
                    }
                }, 120L); // 6 segundos después
            }
        }, 40L);
        
        // 🎵 INICIAR SISTEMA DE AUDIO DINÁMICO
        iniciarSistemaAudioDinamico();
        
        // Presentación del Acto 1 con animación cinematográfica
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                mostrarPresentacionActo1(p);
            }
        }, 60L);
        
        // Generar fragmentos de piedra con aparición gradual
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            generarFragmentosPiedraConEfectos();
        }, 80L);
        
        // Asignar el PRIMER fragmento a cada jugador como objetivo inicial
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!fragmentosLocations.isEmpty()) {
                    // El primer fragmento como objetivo inicial
                    Location primerFragmento = fragmentosLocations.get(0);
                    objetivosPorJugador.put(p.getUniqueId(), primerFragmento);
                    plugin.getLogger().info("[ActionBar] Asignado objetivo inicial a " + p.getName());
                }
            }
            
            // Asegurar que el action bar esté activo
            if (guiaActionBarTask == null || guiaActionBarTask.isCancelled()) {
                plugin.getLogger().info("[ActionBar] Reiniciando guía de action bar");
                iniciarGuiaActionBar();
            }
        }, 100L);
        
        // Iniciar efectos de partículas para fragmentos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            iniciarEfectosFragmentos();
            
            // 📖 Generar libros de lore después de que aparezcan los fragmentos
            generarLibrosLore();
        }, 120L);
    }
    
    /**
     * 🎬 Efecto cinematográfico de introducción
     */
    private void efectoCinematograficoIntro() {
        // Oscurecer ambiente con partículas negras
        for (Player p : Bukkit.getOnlinePlayers()) {
            Location pLoc = p.getLocation();
            for (int i = 0; i < 50; i++) {
                double offsetX = (Math.random() - 0.5) * 10;
                double offsetY = (Math.random()) * 5;
                double offsetZ = (Math.random() - 0.5) * 10;
                pLoc.getWorld().spawnParticle(
                    Particle.SQUID_INK,
                    pLoc.clone().add(offsetX, offsetY, offsetZ),
                    1, 0, 0, 0, 0
                );
            }
        }
        
        // Sonido ambiente ominoso
        playSoundToAll(Sound.AMBIENT_CAVE, 1.0f, 0.5f);
        playSoundToAll(Sound.ENTITY_WARDEN_HEARTBEAT, 0.3f, 0.8f);
        
        // Relámpagos distantes
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.getWorld().strikeLightningEffect(p.getLocation().clone().add(
                    (Math.random() - 0.5) * 100,
                    0,
                    (Math.random() - 0.5) * 100
                ));
            }
            playSoundToAll(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 0.7f);
        }, 20L);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.getWorld().strikeLightningEffect(p.getLocation().clone().add(
                    (Math.random() - 0.5) * 100,
                    0,
                    (Math.random() - 0.5) * 100
                ));
            }
            playSoundToAll(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 0.8f);
        }, 40L);
    }
    
    /**
     * Generar fragmentos con efectos cinematográficos graduales
     */
    private void generarFragmentosPiedraConEfectos() {
        ConfigurationSection acto1Config = config.getConfigurationSection("acto_1.fragmentos_piedra");
        
        // 🎯 SIEMPRE 5 ALTARES - Ignorar config para garantizar los 5 actos
        int cantidad = 5; // Fijo: 5 altares para los 5 actos del evento
        int distanciaMin = acto1Config.getInt("distancia_min_spawn", 500); // MÍNIMO 500 bloques para Altar 1
        int distanciaMax = acto1Config.getInt("distancia_max_spawn", 1000); // MÁXIMO 1000 bloques para Altar 1
        int distanciaEntreFragmentos = acto1Config.getInt("distancia_entre_fragmentos", 150); // 150 bloques entre fragmentos
        
        plugin.getLogger().info("[SusurroPiedraRota] ✓ Generando exactamente 5 altares para los 5 actos");
        
        // 🎯 Calcular ubicación promedio de jugadores activos como referencia
        Location tempRef = calcularUbicacionPromedioJugadores();
        if (tempRef == null) {
            // Fallback al spawn del mundo si no hay jugadores
            World world = Bukkit.getWorlds().get(0);
            tempRef = world.getSpawnLocation();
        }
        final Location referenciaSpawn = tempRef;
        final World world = referenciaSpawn.getWorld();
        
        broadcastNarrative("§5⧖ Buscando ubicaciones perfectas para los fragmentos...");
        playSoundToAll(Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 0.8f);
        
        // Mensajes del OBSERVADOR con efectos dramáticos (MÁS espaciados para lectura)
        // Mensaje 1: Después de 15 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                mensajeObservadorConEfectos(
                    "§8◆ §5El Observador§8: §7«La piedra resuena con energía ancestral...»",
                    Sound.BLOCK_AMETHYST_BLOCK_RESONATE,
                    0.8f,
                    0.6f,
                    Particle.PORTAL,
                    50
                );
            }
        }, 300L); // 15 segundos
        
        // Mensaje 2: Después de 30 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                mensajeObservadorConEfectos(
                    "§8◆ §5El Observador§8: §7«Fragmentos olvidados despiertan en la sombra...»",
                    Sound.ENTITY_WARDEN_HEARTBEAT,
                    0.7f,
                    0.7f,
                    Particle.SOUL_FIRE_FLAME,
                    40
                );
            }
        }, 600L); // 30 segundos
        
        // Mensaje 3: Después de 50 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                mensajeObservadorConEfectos(
                    "§8◆ §5El Observador§8: §7«Sus susurros atraviesan el velo del tiempo...»",
                    Sound.ENTITY_VEX_AMBIENT,
                    0.9f,
                    0.5f,
                    Particle.END_ROD,
                    60
                );
            }
        }, 1000L); // 50 segundos
        
        // BUSCAR UBICACIONES DE FORMA ASÍNCRONA para no congelar el servidor
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Location> ubicacionesEncontradas = new ArrayList<>();
            primerFragmentoEncontroLugarPerfecto = true; // Reset
            
            // 🎯 DISTANCIAS DIFERENCIADAS:
            // - Altar 1: 600+ bloques del spawn/jugadores (distanciaMin/Max del config)
            // - Altares 2-5: 200-300 bloques del altar ANTERIOR (aumentado para mejor separación)
            final int DISTANCIA_MIN_SUBSECUENTE = 200;
            final int DISTANCIA_MAX_SUBSECUENTE = 300;
            final int DISTANCIA_MINIMA_GLOBAL = 150; // Distancia mínima entre CUALQUIER par de altares
            
            plugin.getLogger().info(String.format(
                "[SusurroPiedraRota] Iniciando generación de %d fragmentos",
                cantidad
            ));
            plugin.getLogger().info(String.format(
                "[SusurroPiedraRota] Altar 1: %d-%d bloques del spawn | Altares 2-5: %d-%d bloques del anterior",
                distanciaMin, distanciaMax, DISTANCIA_MIN_SUBSECUENTE, DISTANCIA_MAX_SUBSECUENTE
            ));
            
            for (int i = 0; i < cantidad; i++) {
                final boolean esPrimerFragmento = (i == 0);
                Location loc;
                
                if (esPrimerFragmento) {
                    // 🎯 ALTAR 1: Lejos del spawn/jugadores (600+ bloques)
                    loc = encontrarLocationValidaAsync(world, referenciaSpawn, distanciaMin, distanciaMax, distanciaEntreFragmentos, ubicacionesEncontradas, true);
                    plugin.getLogger().info("[SusurroPiedraRota] Altar 1: Buscando a 600+ bloques de jugadores...");
                } else {
                    // 🎯 ALTARES 2-5: Cerca del altar ANTERIOR (150-200 bloques)
                    Location altarAnterior = ubicacionesEncontradas.get(ubicacionesEncontradas.size() - 1);
                    loc = encontrarLocationValidaAsync(world, altarAnterior, DISTANCIA_MIN_SUBSECUENTE, DISTANCIA_MAX_SUBSECUENTE, 50, ubicacionesEncontradas, false);
                    plugin.getLogger().info(String.format(
                        "[SusurroPiedraRota] Altar %d: Buscando a %d-%d bloques del altar anterior...",
                        i + 1, DISTANCIA_MIN_SUBSECUENTE, DISTANCIA_MAX_SUBSECUENTE
                    ));
                }
                
                if (loc != null) {
                    ubicacionesEncontradas.add(loc);
                    
                    // Calcular distancia real para el log
                    double distanciaReal = esPrimerFragmento 
                        ? loc.distance(referenciaSpawn)
                        : loc.distance(ubicacionesEncontradas.get(ubicacionesEncontradas.size() - 2));
                    
                    plugin.getLogger().info(String.format(
                        "[SusurroPiedraRota] ✓ Altar %d generado en %s (distancia: %.0f bloques)",
                        i + 1,
                        locationToString(loc),
                        distanciaReal
                    ));
                } else {
                    plugin.getLogger().warning(String.format(
                        "[SusurroPiedraRota] ⚠ No se pudo generar ubicación para altar #%d",
                        i + 1
                    ));
                }
            }
            
            // Volver al hilo principal para generar los fragmentos
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!isActive()) return;
                
                if (ubicacionesEncontradas.isEmpty()) {
                    plugin.getLogger().severe("[SusurroPiedraRota] ❌ ERROR CRÍTICO: No se pudo generar ningún fragmento!");
                    broadcastNarrative("§c❌ Error: No se pudieron generar fragmentos. Contacta a un administrador.");
                    stop();
                    return;
                }
                
                // Generar cada fragmento con efecto visual
                for (int i = 0; i < ubicacionesEncontradas.size(); i++) {
                    final int indice = i;
                    final Location fragmentoLoc = ubicacionesEncontradas.get(i);
                    
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!isActive()) return;
                        
                        // Efecto de aparición gradual
                        efectoAparicionFragmento(fragmentoLoc, () -> {
                            construirFragmentoPiedra(fragmentoLoc);
                            fragmentosLocations.add(fragmentoLoc);
                            
                            // Asignar número de altar (1-5)
                            int numAltar = indice + 1;
                            fragmentoANumeroAltar.put(fragmentoLoc, numAltar);
                            
                            plugin.getLogger().info(String.format(
                                "[SusurroPiedraRota] Fragmento #%d (Altar %d) generado en: %s",
                                indice + 1,
                                numAltar,
                                locationToString(fragmentoLoc)
                            ));
                        });
                    }, i * 40L); // 2 segundos entre cada fragmento
                }
                
                // Mensaje final
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!isActive()) return;
                    
                    broadcastNarrative(String.format("§a✓ %d fragmentos antiguos han despertado", ubicacionesEncontradas.size()));
                    playSoundToAll(Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);
                    
                    // ✨ Iniciar timer para bonus de velocidad
                    tiempoInicioActo1 = System.currentTimeMillis();
                    
                    plugin.getLogger().info(String.format(
                        "[SusurroPiedraRota] ✓ Generados %d/%d fragmentos exitosamente",
                        ubicacionesEncontradas.size(), cantidad
                    ));
                    
                    // 🎯 ASIGNAR OBJETIVOS INICIALES para que el action bar funcione
                    // Siempre asignar el altar actual (altarActualGlobal) no el más cercano
                    Location altarActualLoc = obtenerLocationAltarActual();
                    if (altarActualLoc != null) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (participantesOriginales.contains(p.getUniqueId())) {
                                objetivosPorJugador.put(p.getUniqueId(), altarActualLoc);
                                plugin.getLogger().info("[ActionBar] Asignado objetivo altar " + altarActualGlobal + " a " + p.getName());
                            }
                        }
                    }
                }, (ubicacionesEncontradas.size() * 40L) + 20L);
            });
        });
    }
    
    /**
     * Efecto visual de aparición gradual de fragmento
     */
    private void efectoAparicionFragmento(Location loc, Runnable callback) {
        World world = loc.getWorld();
        
        // Build-up con partículas ascendentes
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (!isActive() || ticks >= 30) {
                    cancel();
                    if (callback != null) callback.run();
                    
                    // Explosión final de aparición
                    world.spawnParticle(Particle.EXPLOSION, loc.clone().add(0.5, 1, 0.5), 3);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0.5, 1, 0.5), 30, 1, 1, 1, 0.1);
                    soundUtil.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 0.8f);
                    soundUtil.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.5f, 1.5f);
                    return;
                }
                
                // Partículas en espiral ascendente
                double angle = (ticks * Math.PI * 2) / 10;
                double radius = 0.5 + (ticks / 30.0) * 0.5;
                double height = (ticks / 30.0) * 2;
                
                Location particleLoc = loc.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
                );
                
                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.PORTAL, particleLoc, 3, 0.1, 0.1, 0.1, 0);
                
                if (ticks % 5 == 0) {
                    soundUtil.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.0f + (ticks / 30.0f));
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void generarFragmentosPiedra() {
        ConfigurationSection acto1Config = config.getConfigurationSection("acto_1.fragmentos_piedra");
        
        // 🎯 SIEMPRE 5 ALTARES
        int cantidad = 5;
        int distanciaMin = acto1Config.getInt("distancia_min_spawn", 500);
        int distanciaMax = acto1Config.getInt("distancia_max_spawn", 1000);
        int distanciaEntreFragmentos = acto1Config.getInt("distancia_entre_fragmentos", 150);
        
        World world = Bukkit.getWorlds().get(0);
        Location spawn = world.getSpawnLocation();
        
        for (int i = 0; i < cantidad; i++) {
            Location fragmentoLoc = encontrarLocationValida(
                world,
                spawn,
                distanciaMin,
                distanciaMax,
                distanciaEntreFragmentos
            );
            
            if (fragmentoLoc != null) {
                construirFragmentoPiedra(fragmentoLoc);
                fragmentosLocations.add(fragmentoLoc);
                
                plugin.getLogger().info(String.format(
                    "[SusurroPiedraRota] Fragmento #%d generado en: %s",
                    i + 1,
                    locationToString(fragmentoLoc)
                ));
            }
        }
        
        plugin.getLogger().info(String.format(
            "[SusurroPiedraRota] %d fragmentos de piedra generados",
            fragmentosLocations.size()
        ));
    }
    
    private Location encontrarLocationValida(World world, Location spawn, int distMin, int distMax, int distEntreFragmentos) {
        Random random = new Random();
        int radioActual = distMax;
        int expansion = 0;
        
        plugin.getLogger().info(String.format(
            "[SusurroPiedraRota] Buscando ubicación perfecta (Radio inicial: %d-%d bloques)",
            distMin, radioActual
        ));
        
        // Intentar en radio actual, si falla expandir en incrementos de 200
        while (expansion < 1000) { // Máximo expandir hasta +1000 bloques
            int intentosEnRadio = 0;
            int maxIntentosEnRadio = 200;
            
            while (intentosEnRadio < maxIntentosEnRadio) {
                double angle = random.nextDouble() * Math.PI * 2;
                double distance = distMin + random.nextDouble() * (radioActual - distMin);
                
                int x = spawn.getBlockX() + (int)(Math.cos(angle) * distance);
                int z = spawn.getBlockZ() + (int)(Math.sin(angle) * distance);
                int y = world.getHighestBlockYAt(x, z);
                
                Location loc = new Location(world, x, y, z);
                
                // VALIDACIÓN ESTRICTA SIEMPRE
                if (!esUbicacionPerfecta(world, loc)) {
                    intentosEnRadio++;
                    continue;
                }
                
                // Verificar que no esté muy cerca de otros fragmentos
                boolean lejosDeOtros = true;
                for (Location existente : fragmentosLocations) {
                    if (existente.distance(loc) < distEntreFragmentos) {
                        lejosDeOtros = false;
                        break;
                    }
                }
                
                if (lejosDeOtros) {
                    plugin.getLogger().info(String.format(
                        "[SusurroPiedraRota] ✓ Ubicación perfecta encontrada en radio %d-%d (Intento %d) en %s",
                        distMin, radioActual, intentosEnRadio + 1, locationToString(loc)
                    ));
                    return loc;
                }
                
                intentosEnRadio++;
            }
            
            // Si no encontró en este radio, expandir
            expansion += 200;
            radioActual += 200;
            plugin.getLogger().info(String.format(
                "[SusurroPiedraRota] No encontrado en radio %d, expandiendo búsqueda a %d bloques...",
                radioActual - 200, radioActual
            ));
        }
        
        plugin.getLogger().warning(String.format(
            "[SusurroPiedraRota] ⚠ No se encontró ubicación válida después de expandir hasta radio %d",
            radioActual
        ));
        return null;
    }
    
    /**
     * ⚡ Versión OPTIMIZADA asíncrona de encontrarLocationValida (thread-safe)
     * - Primer fragmento: 600+ bloques de todos los jugadores
     * - Fragmentos subsecuentes: 150-200 bloques del altar anterior
     */
    private Location encontrarLocationValidaAsync(World world, Location referencia, int distMin, int distMax, int distEntreFragmentos, List<Location> ubicacionesExistentes, boolean esPrimerFragmento) {
        Random random = new Random();
        
        // ⚡ OPTIMIZACIÓN: Si el primer fragmento no encontró lugar perfecto, SKIP búsqueda
        if (!esPrimerFragmento && !primerFragmentoEncontroLugarPerfecto) {
            plugin.getLogger().info("[SusurroPiedraRota] ⚡ Fragmento subsecuente - Creando lugar directamente (sin búsqueda)");
            return crearLugarPerfectoDirectamente(world, referencia, distMin, distMax, distEntreFragmentos, ubicacionesExistentes, random);
        }
        
        // Log descriptivo según el tipo de fragmento
        if (esPrimerFragmento) {
            plugin.getLogger().info(String.format(
                "[SusurroPiedraRota] [ASYNC] Altar 1: Buscando a %d-%d bloques (mín 600 de jugadores)",
                distMin, distMax
            ));
        } else {
            plugin.getLogger().info(String.format(
                "[SusurroPiedraRota] [ASYNC] Altar subsecuente: Buscando a %d-%d bloques del anterior",
                distMin, distMax
            ));
        }
        
        int radioActual = distMax;
        int maxIntentos = esPrimerFragmento ? 100 : 50; // ⚡ Menos intentos si no es el primero
        
        for (int intento = 0; intento < maxIntentos; intento++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = distMin + random.nextDouble() * (radioActual - distMin);
            
            int x = referencia.getBlockX() + (int)(Math.cos(angle) * distance);
            int z = referencia.getBlockZ() + (int)(Math.sin(angle) * distance);
            
            // OPERACIÓN ASYNC-SAFE
            int y = world.getHighestBlockYAt(x, z);
            Location loc = new Location(world, x, y, z);
            
            // 🔒 VALIDACIÓN de distancia de jugadores SOLO para el PRIMER fragmento
            if (esPrimerFragmento) {
                boolean lejosDeTodosJugadores = true;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getLocation().distance(loc) < 600) {
                        lejosDeTodosJugadores = false;
                        break;
                    }
                }
                if (!lejosDeTodosJugadores) {
                    continue;
                }
            }
            
            // VALIDACIÓN ESTRICTA
            if (!esUbicacionPerfecta(world, loc)) {
                continue;
            }
            
            // Verificar distancia mínima con otros fragmentos ya existentes
            // Para subsecuentes: mínimo 50 bloques de separación entre altares
            int distanciaMinimaEntreAltares = esPrimerFragmento ? 100 : 50;
            boolean lejosDeOtros = true;
            for (Location existente : ubicacionesExistentes) {
                if (existente.distance(loc) < distanciaMinimaEntreAltares) {
                    lejosDeOtros = false;
                    break;
                }
            }
            
            if (lejosDeOtros) {
                plugin.getLogger().info(String.format(
                    "[SusurroPiedraRota] [ASYNC] ✓ Ubicación perfecta encontrada (Intento %d) en %s",
                    intento + 1, locationToString(loc)
                ));
                // ⚡ Marcamos que SÍ encontró lugar perfecto
                if (esPrimerFragmento) {
                    primerFragmentoEncontroLugarPerfecto = true;
                }
                return loc;
            }
        }
        
        // No encontró lugar perfecto natural
        if (esPrimerFragmento) {
            primerFragmentoEncontroLugarPerfecto = false; // ⚡ Marcar que NO encontró
            plugin.getLogger().info("[SusurroPiedraRota] [ASYNC] ⚠ Altar 1: No encontró lugar perfecto, creando uno...");
        } else {
            plugin.getLogger().info("[SusurroPiedraRota] [ASYNC] ⚡ Altar subsecuente: Creando lugar perfecto cerca del anterior...");
        }
        
        return crearLugarPerfectoDirectamente(world, referencia, distMin, distMax, distEntreFragmentos, ubicacionesExistentes, random);
    }
    
    /**
     * ⚡ NUEVA FUNCIÓN: Crear lugar perfecto directamente sin búsqueda exhaustiva
     */
    private Location crearLugarPerfectoDirectamente(World world, Location spawn, int distMin, int distMax, int distEntreFragmentos, List<Location> ubicacionesExistentes, Random random) {
        // Buscar ubicación válida para terraformar (solo 50 intentos rápidos)
        for (int i = 0; i < 50; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = distMin + random.nextDouble() * (distMax - distMin);
            
            int x = spawn.getBlockX() + (int)(Math.cos(angle) * distance);
            int z = spawn.getBlockZ() + (int)(Math.sin(angle) * distance);
            int y = world.getHighestBlockYAt(x, z);
            
            Location loc = new Location(world, x, y, z);
            
            // VALIDACIONES RÁPIDAS
            // 1. Suelo sólido
            boolean tieneSueloSolido = false;
            for (int checkY = y - 1; checkY >= Math.max(y - 5, world.getMinHeight()); checkY--) {
                Material mat = world.getBlockAt(x, checkY, z).getType();
                if (mat.isSolid() && mat != Material.WATER && mat != Material.LAVA) {
                    tieneSueloSolido = true;
                    break;
                }
            }
            if (!tieneSueloSolido) continue;
            
            // 2. No agua cercana (check más pequeño)
            boolean hayAgua = false;
            for (int checkX = -5; checkX <= 5 && !hayAgua; checkX++) {
                for (int checkZ = -5; checkZ <= 5; checkZ++) {
                    Material mat = world.getBlockAt(x + checkX, y, z + checkZ).getType();
                    if (mat == Material.WATER || mat == Material.LAVA) {
                        hayAgua = true;
                        break;
                    }
                }
            }
            if (hayAgua) continue;
            
            // 3. Distancia con otros fragmentos
            boolean lejosDeOtros = true;
            for (Location existente : ubicacionesExistentes) {
                if (existente.distance(loc) < distEntreFragmentos) {
                    lejosDeOtros = false;
                    break;
                }
            }
            
            if (lejosDeOtros) {
                // TERRAFORMAR en el hilo principal
                Location finalLoc = loc;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    terraformarLugarPerfecto(world, finalLoc);
                });
                
                plugin.getLogger().info(String.format(
                    "[SusurroPiedraRota] [ASYNC] ✓ Lugar perfecto CREADO en %s (Intento %d/50)",
                    locationToString(loc), i + 1
                ));
                return loc;
            }
        }
        
        plugin.getLogger().warning("[SusurroPiedraRota] [ASYNC] ⚠ No se pudo crear lugar perfecto");
        return null;
    }
    
    /**
     * Terraforma un área 37x37 para crear el lugar perfecto para un fragmento
     * Analiza el bioma circundante y usa sus bloques para integración natural
     * MEJORADO: Detecta el bioma real y adapta completamente la estética
     */
    private void terraformarLugarPerfecto(World world, Location centro) {
        int cx = centro.getBlockX();
        int cy = centro.getBlockY();
        int cz = centro.getBlockZ();
        Random rand = new Random();
        
        // 🌍 DETECCIÓN INTELIGENTE DEL BIOMA
        org.bukkit.block.Biome bioma = world.getBiome(cx, cy, cz);
        BiomaTerraformData biomaData = obtenerDatosBioma(bioma);
        
        // 🔍 ANÁLISIS DEL TERRENO: Detectar bloques predominantes en el área
        Map<Material, Integer> bloquesBioma = new HashMap<>();
        int promedioAltura = 0;
        int contadorAltura = 0;
        
        for (int scanX = cx - 20; scanX <= cx + 20; scanX += 2) {
            for (int scanZ = cz - 20; scanZ <= cz + 20; scanZ += 2) {
                Block bloque = world.getBlockAt(scanX, cy - 1, scanZ);
                Material mat = bloque.getType();
                if (mat.isSolid() && !mat.isAir() && !mat.name().contains("LEAVES")) {
                    bloquesBioma.put(mat, bloquesBioma.getOrDefault(mat, 0) + 1);
                }
                promedioAltura += world.getHighestBlockYAt(scanX, scanZ);
                contadorAltura++;
            }
        }
        promedioAltura = contadorAltura > 0 ? promedioAltura / contadorAltura : cy;
        
        // Obtener bloques comunes del terreno para mezcla
        List<Material> bloquesComunes = bloquesBioma.entrySet().stream()
            .filter(e -> esBloqueTerrenoValido(e.getKey()))
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        // Combinar datos del bioma con bloques detectados
        Material baseSuperficie = !bloquesComunes.isEmpty() ? bloquesComunes.get(0) : biomaData.superficie;
        Material baseSubsuelo = biomaData.subsuelo;
        Material piedraBase = biomaData.piedraBase;
        Material decoracion1 = biomaData.decoracion1;
        Material decoracion2 = biomaData.decoracion2;
        Material acento = biomaData.acento;
        Material vegetacion = biomaData.vegetacion;
        
        plugin.getLogger().info(String.format(
            "[SusurroPiedraRota] 🌍 Bioma: %s - Superficie: %s, Piedra: %s, Vegetación: %s",
            bioma.name(), baseSuperficie, piedraBase, vegetacion != null ? vegetacion : "ninguna"
        ));
        
        // 🎭 Área ÉPICA: 37x37 (radio 18) con transición gradual y adaptación al terreno
        for (int x = cx - 18; x <= cx + 18; x++) {
            for (int z = cz - 18; z <= cz + 18; z++) {
                double distCentro = Math.sqrt(Math.pow(x - cx, 2) + Math.pow(z - cz, 2));
                double angulo = Math.atan2(z - cz, x - cx);
                
                // Variación orgánica usando ruido
                double ruido = Math.sin(x * 0.3) * Math.cos(z * 0.3) * 2;
                double distEfectiva = distCentro + ruido;
                
                // 1. Base sólida adaptativa (4 capas con transición)
                world.getBlockAt(x, cy - 4, z).setType(Material.DEEPSLATE);
                world.getBlockAt(x, cy - 3, z).setType(piedraBase);
                world.getBlockAt(x, cy - 2, z).setType(rand.nextDouble() < 0.7 ? baseSubsuelo : piedraBase);
                world.getBlockAt(x, cy - 1, z).setType(baseSuperficie);
                
                // 2. Limpiar espacio arriba (8 bloques)
                for (int dy = 0; dy < 8; dy++) {
                    world.getBlockAt(x, cy + dy, z).setType(Material.AIR);
                }
                
                // 3. SISTEMA DE CAPAS ORGÁNICAS
                if (distEfectiva >= 16 && distCentro <= 18.5) {
                    // 🌿 Borde exterior - Transición SUAVE con vegetación del bioma
                    double fade = (18.5 - distCentro) / 2.5;
                    if (rand.nextDouble() < fade * 0.8) {
                        // Mantener terreno natural con algunos toques
                        if (rand.nextDouble() < 0.3 && vegetacion != null) {
                            world.getBlockAt(x, cy, z).setType(vegetacion);
                        }
                    }
                    // Ocasionalmente colocar bloques sueltos del bioma
                    if (rand.nextDouble() < 0.15) {
                        world.getBlockAt(x, cy, z).setType(rand.nextBoolean() ? decoracion1 : baseSuperficie);
                    }
                } else if (distEfectiva >= 12 && distEfectiva < 16) {
                    // 🪨 Anillo medio - Ruinas antiguas mezcladas con bioma
                    double probabilidad = rand.nextDouble();
                    if (probabilidad < 0.35) {
                        world.getBlockAt(x, cy, z).setType(decoracion1);
                    } else if (probabilidad < 0.55) {
                        world.getBlockAt(x, cy, z).setType(decoracion2);
                    } else if (probabilidad < 0.7) {
                        world.getBlockAt(x, cy, z).setType(acento);
                    }
                    // Vegetación dispersa
                    if (rand.nextDouble() < 0.1 && vegetacion != null) {
                        world.getBlockAt(x, cy + 1, z).setType(vegetacion);
                    }
                } else if (distEfectiva >= 7 && distEfectiva < 12) {
                    // 🏛️ Anillo interno - Ruinas más definidas
                    double probabilidad = rand.nextDouble();
                    if (probabilidad < 0.4) {
                        world.getBlockAt(x, cy, z).setType(Material.STONE_BRICKS);
                    } else if (probabilidad < 0.6) {
                        world.getBlockAt(x, cy, z).setType(Material.CRACKED_STONE_BRICKS);
                    } else if (probabilidad < 0.75) {
                        world.getBlockAt(x, cy, z).setType(Material.MOSSY_STONE_BRICKS);
                    } else if (probabilidad < 0.85) {
                        world.getBlockAt(x, cy, z).setType(acento);
                    }
                } else if (distEfectiva >= 3 && distEfectiva < 7) {
                    // ⬛ Plataforma central - Piedra oscura ritual
                    if (rand.nextDouble() < 0.6) {
                        world.getBlockAt(x, cy, z).setType(Material.DEEPSLATE_BRICKS);
                    } else if (rand.nextDouble() < 0.5) {
                        world.getBlockAt(x, cy, z).setType(Material.POLISHED_DEEPSLATE);
                    } else {
                        world.getBlockAt(x, cy, z).setType(Material.DEEPSLATE_TILES);
                    }
                }
                
                // 4. 🏛️ Columnas decorativas adaptadas al bioma (8 columnas)
                if (distCentro >= 13 && distCentro <= 15) {
                    double anguloNormalizado = (angulo + Math.PI) / (2 * Math.PI);
                    int sector = (int)(anguloNormalizado * 8);
                    double sectorInicio = sector / 8.0 * 2 * Math.PI - Math.PI;
                    double sectorMedio = sectorInicio + Math.PI / 8;
                    
                    if (Math.abs(angulo - sectorMedio) < 0.15 && rand.nextDouble() < 0.25) {
                        Material columna = biomaData.columna;
                        for (int h = 0; h <= 3 + rand.nextInt(2); h++) {
                            world.getBlockAt(x, cy + h, z).setType(columna);
                        }
                        // Tope decorativo
                        world.getBlockAt(x, cy + 4 + rand.nextInt(2), z).setType(biomaData.topeColumna);
                    }
                }
                
                // 5. 🪦 Ruinas aleatorias dispersas
                if (distCentro >= 10 && distCentro <= 16 && rand.nextDouble() < 0.02) {
                    int alturaRuina = 1 + rand.nextInt(3);
                    for (int h = 0; h < alturaRuina; h++) {
                        world.getBlockAt(x, cy + h, z).setType(
                            rand.nextBoolean() ? Material.COBBLESTONE_WALL : Material.STONE_BRICK_WALL
                        );
                    }
                }
            }
        }
        
        // 6. 🔥 Antorchas/luces adaptadas al bioma
        colocarIluminacionBioma(world, centro, biomaData, rand);
        
        // 7. 🌿 Vegetación extra según el bioma
        if (vegetacion != null) {
            colocarVegetacionBioma(world, centro, vegetacion, biomaData, rand);
        }
        
        // 🎆 Efecto visual ÉPICO al terminar terraformación
        Particle particulaBioma = biomaData.particula;
        for (int i = 0; i < 360; i += 15) {
            double angle = Math.toRadians(i);
            double radius = 18.0;
            double x = centro.getX() + Math.cos(angle) * radius;
            double z = centro.getZ() + Math.sin(angle) * radius;
            
            world.spawnParticle(
                particulaBioma,
                x, centro.getY() + 1, z,
                8,
                0.3, 1.0, 0.3,
                0.05
            );
            world.spawnParticle(
                Particle.ELECTRIC_SPARK,
                x, centro.getY() + 2, z,
                3,
                0.2, 0.5, 0.2,
                0.08
            );
        }
        
        // Sonido épico de terraformación
        world.playSound(centro, Sound.ENTITY_WITHER_SPAWN, 0.3f, 0.6f);
        world.playSound(centro, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.5f, 0.8f);
        
        plugin.getLogger().info(String.format(
            "[SusurroPiedraRota] 🎭 Terraformado ÉPICO 37x37 completado en %s (bioma: %s)",
            locationToString(centro), bioma.name()
        ));
    }
    
    /**
     * Verifica si un material es válido para considerar como terreno
     */
    private boolean esBloqueTerrenoValido(Material mat) {
        String nombre = mat.name();
        return mat.isSolid() && !mat.isAir() &&
               !nombre.contains("LEAVES") && !nombre.contains("LOG") &&
               !nombre.contains("PLANKS") && !nombre.contains("FENCE") &&
               !nombre.contains("DOOR") && !nombre.contains("CHEST") &&
               !nombre.contains("SPAWNER") && !nombre.contains("ORE");
    }
    
    /**
     * Coloca iluminación adaptada al bioma
     */
    private void colocarIluminacionBioma(World world, Location centro, BiomaTerraformData data, Random rand) {
        int cx = centro.getBlockX();
        int cy = centro.getBlockY();
        int cz = centro.getBlockZ();
        
        // 8 puntos de luz en círculo
        for (int i = 0; i < 8; i++) {
            double angulo = i * Math.PI / 4;
            int lx = cx + (int)(Math.cos(angulo) * 12);
            int lz = cz + (int)(Math.sin(angulo) * 12);
            
            // Verificar que hay suelo
            Block base = world.getBlockAt(lx, cy - 1, lz);
            if (base.getType().isSolid()) {
                // Poste de luz
                world.getBlockAt(lx, cy, lz).setType(data.columna);
                world.getBlockAt(lx, cy + 1, lz).setType(data.columna);
                world.getBlockAt(lx, cy + 2, lz).setType(data.luz);
            }
        }
        
        // Luces adicionales más cerca del centro
        for (int i = 0; i < 4; i++) {
            double angulo = i * Math.PI / 2 + Math.PI / 4;
            int lx = cx + (int)(Math.cos(angulo) * 6);
            int lz = cz + (int)(Math.sin(angulo) * 6);
            
            Block base = world.getBlockAt(lx, cy, lz);
            if (base.getType() == Material.AIR) {
                world.getBlockAt(lx, cy, lz).setType(data.luz);
            }
        }
    }
    
    /**
     * Coloca vegetación específica del bioma
     */
    private void colocarVegetacionBioma(World world, Location centro, Material vegetacion, BiomaTerraformData data, Random rand) {
        int cx = centro.getBlockX();
        int cy = centro.getBlockY();
        int cz = centro.getBlockZ();
        
        for (int i = 0; i < 20; i++) {
            double angulo = rand.nextDouble() * 2 * Math.PI;
            double radio = 14 + rand.nextDouble() * 4;
            int vx = cx + (int)(Math.cos(angulo) * radio);
            int vz = cz + (int)(Math.sin(angulo) * radio);
            
            Block suelo = world.getBlockAt(vx, cy - 1, vz);
            Block espacio = world.getBlockAt(vx, cy, vz);
            
            if (suelo.getType().isSolid() && espacio.getType() == Material.AIR) {
                // Colocar vegetación solo sobre superficies compatibles
                if (esSuperficieCompatible(suelo.getType(), vegetacion)) {
                    espacio.setType(vegetacion);
                }
            }
        }
    }
    
    /**
     * Verifica si la vegetación puede colocarse sobre el bloque
     */
    private boolean esSuperficieCompatible(Material suelo, Material vegetacion) {
        String nombreVeg = vegetacion.name();
        String nombreSuelo = suelo.name();
        
        // Plantas normales sobre tierra/grass
        if (nombreVeg.contains("GRASS") || nombreVeg.contains("FERN") || 
            nombreVeg.contains("FLOWER") || nombreVeg.contains("TULIP") ||
            nombreVeg.contains("POPPY") || nombreVeg.contains("DANDELION")) {
            return suelo == Material.GRASS_BLOCK || suelo == Material.DIRT ||
                   suelo == Material.PODZOL || suelo == Material.COARSE_DIRT;
        }
        // Cactus sobre arena
        if (vegetacion == Material.DEAD_BUSH || vegetacion == Material.CACTUS) {
            return nombreSuelo.contains("SAND") || suelo == Material.TERRACOTTA ||
                   nombreSuelo.contains("TERRACOTTA");
        }
        // Hongos
        if (nombreVeg.contains("MUSHROOM") || nombreVeg.contains("FUNGUS")) {
            return suelo == Material.MYCELIUM || suelo == Material.PODZOL ||
                   nombreSuelo.contains("NYLIUM");
        }
        // Nieve
        if (vegetacion == Material.SNOW) {
            return true;
        }
        return false;
    }
    
    /**
     * Clase interna para datos de terraformación por bioma
     */
    private static class BiomaTerraformData {
        Material superficie;
        Material subsuelo;
        Material piedraBase;
        Material decoracion1;
        Material decoracion2;
        Material acento;
        Material columna;
        Material topeColumna;
        Material luz;
        Material vegetacion;
        Particle particula;
        
        BiomaTerraformData(Material superficie, Material subsuelo, Material piedraBase,
                          Material decoracion1, Material decoracion2, Material acento,
                          Material columna, Material topeColumna, Material luz,
                          Material vegetacion, Particle particula) {
            this.superficie = superficie;
            this.subsuelo = subsuelo;
            this.piedraBase = piedraBase;
            this.decoracion1 = decoracion1;
            this.decoracion2 = decoracion2;
            this.acento = acento;
            this.columna = columna;
            this.topeColumna = topeColumna;
            this.luz = luz;
            this.vegetacion = vegetacion;
            this.particula = particula;
        }
    }
    
    /**
     * Obtiene los datos de terraformación específicos para cada bioma
     */
    private BiomaTerraformData obtenerDatosBioma(org.bukkit.block.Biome bioma) {
        String nombre = bioma.name();
        
        // 🏜️ DESIERTO / BADLANDS
        if (nombre.contains("DESERT") || nombre.contains("BADLANDS") || nombre.contains("MESA")) {
            Material superficie = nombre.contains("BADLANDS") ? Material.RED_SAND : Material.SAND;
            Material deco = nombre.contains("BADLANDS") ? Material.TERRACOTTA : Material.SANDSTONE;
            return new BiomaTerraformData(
                superficie, Material.SANDSTONE, Material.SMOOTH_SANDSTONE,
                deco, Material.CHISELED_SANDSTONE, Material.CUT_SANDSTONE,
                Material.SANDSTONE_WALL, Material.SANDSTONE_STAIRS, Material.TORCH,
                Material.DEAD_BUSH, Particle.DUST_PLUME
            );
        }
        
        // ❄️ NIEVE / HIELO
        if (nombre.contains("SNOW") || nombre.contains("ICE") || nombre.contains("FROZEN") || 
            nombre.contains("COLD") || nombre.contains("GROVE") || nombre.contains("PEAKS")) {
            return new BiomaTerraformData(
                Material.SNOW_BLOCK, Material.PACKED_ICE, Material.BLUE_ICE,
                Material.PACKED_ICE, Material.STONE_BRICKS, Material.PRISMARINE_BRICKS,
                Material.PACKED_ICE, Material.ICE, Material.SEA_LANTERN,
                Material.SNOW, Particle.SNOWFLAKE
            );
        }
        
        // 🍄 HONGOS (Mushroom / Nether)
        if (nombre.contains("MUSHROOM")) {
            return new BiomaTerraformData(
                Material.MYCELIUM, Material.DIRT, Material.STONE,
                Material.MUSHROOM_STEM, Material.RED_MUSHROOM_BLOCK, Material.BROWN_MUSHROOM_BLOCK,
                Material.MUSHROOM_STEM, Material.SHROOMLIGHT, Material.SHROOMLIGHT,
                Material.RED_MUSHROOM, Particle.SPORE_BLOSSOM_AIR
            );
        }
        
        // 🌲 TAIGA / BOSQUE OSCURO
        if (nombre.contains("TAIGA") || nombre.contains("DARK_FOREST") || nombre.contains("OLD_GROWTH")) {
            return new BiomaTerraformData(
                Material.PODZOL, Material.COARSE_DIRT, Material.STONE,
                Material.MOSSY_COBBLESTONE, Material.MOSSY_STONE_BRICKS, Material.COBBLESTONE,
                Material.SPRUCE_LOG, Material.SPRUCE_LEAVES, Material.LANTERN,
                Material.FERN, Particle.FALLING_SPORE_BLOSSOM
            );
        }
        
        // 🌴 JUNGLA
        if (nombre.contains("JUNGLE") || nombre.contains("BAMBOO")) {
            return new BiomaTerraformData(
                Material.GRASS_BLOCK, Material.DIRT, Material.MOSSY_COBBLESTONE,
                Material.MOSSY_COBBLESTONE, Material.MOSSY_STONE_BRICKS, Material.VINE,
                Material.JUNGLE_LOG, Material.JUNGLE_LEAVES, Material.LANTERN,
                Material.FERN, Particle.COMPOSTER
            );
        }
        
        // 🌊 PANTANO / MANGLAR
        if (nombre.contains("SWAMP") || nombre.contains("MANGROVE")) {
            return new BiomaTerraformData(
                Material.GRASS_BLOCK, Material.MUD, Material.MUDDY_MANGROVE_ROOTS,
                Material.MOSSY_COBBLESTONE, Material.MUD_BRICKS, Material.PACKED_MUD,
                Material.DARK_OAK_LOG, Material.DARK_OAK_LEAVES, Material.SOUL_LANTERN,
                Material.LILY_PAD, Particle.FALLING_WATER
            );
        }
        
        // 🏔️ MONTAÑA / EXTREME HILLS
        if (nombre.contains("MOUNTAIN") || nombre.contains("HILL") || nombre.contains("WINDSWEPT") ||
            nombre.contains("STONY") || nombre.contains("MEADOW")) {
            return new BiomaTerraformData(
                Material.STONE, Material.COBBLESTONE, Material.ANDESITE,
                Material.COBBLESTONE, Material.MOSSY_COBBLESTONE, Material.ANDESITE,
                Material.STONE_BRICK_WALL, Material.STONE_BRICK_STAIRS, Material.LANTERN,
                Material.SHORT_GRASS, Particle.ASH
            );
        }
        
        // 🌸 CHERRY GROVE
        if (nombre.contains("CHERRY")) {
            return new BiomaTerraformData(
                Material.GRASS_BLOCK, Material.DIRT, Material.STONE,
                Material.CHERRY_PLANKS, Material.PINK_PETALS, Material.CHERRY_LOG,
                Material.CHERRY_LOG, Material.CHERRY_LEAVES, Material.LANTERN,
                Material.PINK_PETALS, Particle.CHERRY_LEAVES
            );
        }
        
        // 🌵 SAVANNA
        if (nombre.contains("SAVANNA")) {
            return new BiomaTerraformData(
                Material.GRASS_BLOCK, Material.COARSE_DIRT, Material.STONE,
                Material.ACACIA_PLANKS, Material.TERRACOTTA, Material.ORANGE_TERRACOTTA,
                Material.ACACIA_LOG, Material.ACACIA_LEAVES, Material.TORCH,
                Material.SHORT_GRASS, Particle.DUST_PLUME
            );
        }
        
        // 🏝️ PLAYA
        if (nombre.contains("BEACH") || nombre.contains("SHORE")) {
            return new BiomaTerraformData(
                Material.SAND, Material.SANDSTONE, Material.STONE,
                Material.SANDSTONE, Material.SMOOTH_SANDSTONE, Material.PRISMARINE,
                Material.OAK_LOG, Material.OAK_PLANKS, Material.LANTERN,
                null, Particle.FALLING_WATER
            );
        }
        
        // 🕳️ DEEP DARK
        if (nombre.contains("DEEP_DARK")) {
            return new BiomaTerraformData(
                Material.SCULK, Material.DEEPSLATE, Material.REINFORCED_DEEPSLATE,
                Material.DEEPSLATE_BRICKS, Material.DEEPSLATE_TILES, Material.SCULK_CATALYST,
                Material.DEEPSLATE_BRICK_WALL, Material.CHISELED_DEEPSLATE, Material.SOUL_LANTERN,
                null, Particle.SCULK_CHARGE_POP
            );
        }
        
        // 🌳 BOSQUE NORMAL (default para forests)
        if (nombre.contains("FOREST") || nombre.contains("BIRCH") || nombre.contains("FLOWER")) {
            Material veg = nombre.contains("FLOWER") ? Material.POPPY : Material.FERN;
            return new BiomaTerraformData(
                Material.GRASS_BLOCK, Material.DIRT, Material.STONE,
                Material.MOSSY_COBBLESTONE, Material.COBBLESTONE, Material.STONE_BRICKS,
                Material.OAK_LOG, Material.OAK_LEAVES, Material.LANTERN,
                veg, Particle.COMPOSTER
            );
        }
        
        // 🌾 LLANURA (default)
        return new BiomaTerraformData(
            Material.GRASS_BLOCK, Material.DIRT, Material.STONE,
            Material.COBBLESTONE, Material.STONE_BRICKS, Material.MOSSY_COBBLESTONE,
            Material.STONE_BRICK_WALL, Material.CHISELED_STONE_BRICKS, Material.LANTERN,
            Material.SHORT_GRASS, Particle.SOUL_FIRE_FLAME
        );
    }
    
    /**
     * Obtiene una variante más oscura del material para el subsuelo
     */
    private Material obtenerVarianteOscura(Material base) {
        return switch (base) {
            case GRASS_BLOCK, DIRT -> Material.COARSE_DIRT;
            case SAND -> Material.SANDSTONE;
            case RED_SAND -> Material.RED_SANDSTONE;
            case SNOW_BLOCK, SNOW -> Material.PACKED_ICE;
            case PODZOL -> Material.DIRT;
            case MYCELIUM -> Material.DIRT;
            case GRAVEL -> Material.STONE;
            case MUD -> Material.PACKED_MUD;
            case SCULK -> Material.DEEPSLATE;
            case TERRACOTTA, RED_TERRACOTTA, ORANGE_TERRACOTTA -> Material.BROWN_TERRACOTTA;
            default -> Material.STONE;
        };
    }
    
    /**
     * Validación exhaustiva para encontrar ubicación PERFECTA en radio 15 bloques
     */
    private boolean esUbicacionPerfecta(World world, Location loc) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        
        // 1. Verificar bloque base
        Material bloqueAbajo = world.getBlockAt(x, y - 1, z).getType();
        if (bloqueAbajo == Material.WATER || bloqueAbajo == Material.LAVA ||
            bloqueAbajo == Material.AIR || bloqueAbajo.name().contains("LEAVES") ||
            bloqueAbajo.name().contains("LOG") || bloqueAbajo == Material.BEDROCK) {
            return false;
        }
        
        if (!bloqueAbajo.isSolid()) {
            return false;
        }
        
        // 2. Verificar NO agua/lava en radio 10 bloques (más estricto)
        for (int checkX = -10; checkX <= 10; checkX++) {
            for (int checkZ = -10; checkZ <= 10; checkZ++) {
                Material checkMat = world.getBlockAt(x + checkX, y - 1, z + checkZ).getType();
                if (checkMat == Material.WATER || checkMat == Material.LAVA) {
                    return false;
                }
            }
        }
        
        // 3. Verificar terreno COMPLETAMENTE PLANO en radio 15 bloques (±1 bloque máximo)
        for (int checkX = -15; checkX <= 15; checkX++) {
            for (int checkZ = -15; checkZ <= 15; checkZ++) {
                int alturaCercana = world.getHighestBlockYAt(x + checkX, z + checkZ);
                if (Math.abs(alturaCercana - y) > 1) { // Solo ±1 bloque de desnivel
                    return false;
                }
            }
        }
        
        // 4. Verificar espacio vertical amplio (7 bloques, más espacio)
        for (int checkY = 0; checkY < 7; checkY++) {
            Material checkBlock = world.getBlockAt(x, y + checkY, z).getType();
            if (checkBlock.isSolid() && checkBlock != Material.AIR) {
                return false;
            }
        }
        
        // 5. Verificar NO árboles cercanos en radio 12 bloques y altura 15 (MÁS ESTRICTO)
        for (int checkX = -12; checkX <= 12; checkX++) {
            for (int checkZ = -12; checkZ <= 12; checkZ++) {
                for (int checkY = 0; checkY < 15; checkY++) {
                    Material checkMat = world.getBlockAt(x + checkX, y + checkY, z + checkZ).getType();
                    if (checkMat.name().contains("LOG") || checkMat.name().contains("LEAVES") ||
                        checkMat.name().contains("WOOD") || checkMat == Material.VINE) {
                        return false;
                    }
                }
            }
        }
        
        // 6. Verificar tipo de superficie jugable (preferir grass, stone, etc.)
        if (bloqueAbajo == Material.SAND || bloqueAbajo == Material.GRAVEL || 
            bloqueAbajo == Material.SOUL_SAND || bloqueAbajo.name().contains("ICE")) {
            return false; // Evitar superficies inestables
        }
        
        return true; // ¡Ubicación PERFECTA!
    }
    
    private void construirFragmentoPiedra(Location loc) {
        World world = loc.getWorld();
        Random rand = new Random();
        
        // 🏛️ ALTAR ÉPICO - BASE 9x9 (antes 5x5)
        // Capa -2: Base profunda con anillos
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                double dist = Math.sqrt(x * x + z * z);
                Location blockLoc = loc.clone().add(x, -2, z);
                
                if (dist <= 4.5) {
                    if (dist > 3.5) {
                        // Anillo exterior - Piedra agrietada
                        world.getBlockAt(blockLoc).setType(Material.CRACKED_DEEPSLATE_BRICKS);
                    } else {
                        // Interior - Deepslate sólido
                        world.getBlockAt(blockLoc).setType(Material.DEEPSLATE);
                    }
                }
            }
        }
        
        // Capa -1: Plataforma principal 7x7
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                Location blockLoc = loc.clone().add(x, -1, z);
                double dist = Math.sqrt(x * x + z * z);
                
                if (dist <= 3.5) {
                    if (dist > 2.5) {
                        // Borde decorativo
                        world.getBlockAt(blockLoc).setType(Material.DEEPSLATE_BRICKS);
                    } else {
                        // Interior
                        world.getBlockAt(blockLoc).setType(Material.DEEPSLATE_TILES);
                    }
                }
            }
        }
        
        // Capa 0: Piso del altar 5x5 con patrón
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Location blockLoc = loc.clone().add(x, 0, z);
                
                if (x == 0 && z == 0) {
                    // Centro - Obsidiana llorosa
                    world.getBlockAt(blockLoc).setType(Material.CRYING_OBSIDIAN);
                } else if ((Math.abs(x) == 2 && z == 0) || (Math.abs(z) == 2 && x == 0)) {
                    // Cruz cardinal - Ladrillos cincelados
                    world.getBlockAt(blockLoc).setType(Material.CHISELED_DEEPSLATE);
                } else if (Math.abs(x) == Math.abs(z)) {
                    // Diagonales - Ladrillos de deepslate
                    world.getBlockAt(blockLoc).setType(Material.DEEPSLATE_BRICKS);
                } else {
                    // Resto - Deepslate pulido
                    world.getBlockAt(blockLoc).setType(Material.POLISHED_DEEPSLATE);
                }
            }
        }
        
        // 🗿 COLUMNA CENTRAL ÉPICA (altura 5 bloques)
        loc.clone().add(0, 1, 0).getBlock().setType(Material.CHISELED_DEEPSLATE);
        loc.clone().add(0, 2, 0).getBlock().setType(Material.DEEPSLATE_BRICKS);
        loc.clone().add(0, 3, 0).getBlock().setType(Material.CHISELED_DEEPSLATE);
        loc.clone().add(0, 4, 0).getBlock().setType(Material.CRYING_OBSIDIAN);
        
        // ⛓️ CADENAS SUSPENDIDAS desde arriba (4 cadenas de 3 bloques)
        // Norte
        loc.clone().add(0, 5, -2).getBlock().setType(Material.CHAIN);
        loc.clone().add(0, 6, -2).getBlock().setType(Material.CHAIN);
        loc.clone().add(0, 7, -2).getBlock().setType(Material.SOUL_LANTERN);
        
        // Sur
        loc.clone().add(0, 5, 2).getBlock().setType(Material.CHAIN);
        loc.clone().add(0, 6, 2).getBlock().setType(Material.CHAIN);
        loc.clone().add(0, 7, 2).getBlock().setType(Material.SOUL_LANTERN);
        
        // Este
        loc.clone().add(2, 5, 0).getBlock().setType(Material.CHAIN);
        loc.clone().add(2, 6, 0).getBlock().setType(Material.CHAIN);
        loc.clone().add(2, 7, 0).getBlock().setType(Material.SOUL_LANTERN);
        
        // Oeste
        loc.clone().add(-2, 5, 0).getBlock().setType(Material.CHAIN);
        loc.clone().add(-2, 6, 0).getBlock().setType(Material.CHAIN);
        loc.clone().add(-2, 7, 0).getBlock().setType(Material.SOUL_LANTERN);
        
        // 🕯️ VELAS MORADAS en esquinas (nivel 1)
        loc.clone().add(-2, 1, -2).getBlock().setType(Material.PURPLE_CANDLE);
        loc.clone().add(2, 1, -2).getBlock().setType(Material.PURPLE_CANDLE);
        loc.clone().add(-2, 1, 2).getBlock().setType(Material.PURPLE_CANDLE);
        loc.clone().add(2, 1, 2).getBlock().setType(Material.PURPLE_CANDLE);
        
        // 🔥 VELAS ADICIONALES en puntos cardinales (nivel 0)
        loc.clone().add(0, 0, -3).getBlock().setType(Material.SOUL_CAMPFIRE);
        loc.clone().add(0, 0, 3).getBlock().setType(Material.SOUL_CAMPFIRE);
        loc.clone().add(3, 0, 0).getBlock().setType(Material.SOUL_CAMPFIRE);
        loc.clone().add(-3, 0, 0).getBlock().setType(Material.SOUL_CAMPFIRE);
        
        // 🗿 COLUMNAS DECORATIVAS en esquinas externas (altura 3)
        int[][] columnPositions = {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}};
        for (int[] pos : columnPositions) {
            world.getBlockAt(loc.clone().add(pos[0], 0, pos[1])).setType(Material.POLISHED_DEEPSLATE);
            world.getBlockAt(loc.clone().add(pos[0], 1, pos[1])).setType(Material.DEEPSLATE_BRICKS);
            world.getBlockAt(loc.clone().add(pos[0], 2, pos[1])).setType(Material.CHISELED_DEEPSLATE);
            world.getBlockAt(loc.clone().add(pos[0], 3, pos[1])).setType(Material.LANTERN);
        }
        
        // 🎆 EFECTO VISUAL ÉPICO DE CONSTRUCCIÓN
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            // Variables efectivamente finales para lambdas anidadas
            final Location finalLoc = loc.clone();
            final World finalWorld = world;
            
            // Anillo de partículas expandiéndose
            for (int i = 0; i < 360; i += 15) {
                final double angle = Math.toRadians(i);
                for (double r = 1.0; r <= 5.0; r += 0.5) {
                    final double finalR = r;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double x = finalLoc.getX() + Math.cos(angle) * finalR;
                        double z = finalLoc.getZ() + Math.sin(angle) * finalR;
                        finalWorld.spawnParticle(
                            Particle.SOUL_FIRE_FLAME,
                            x, finalLoc.getY() + 1, z,
                            3, 0.1, 0.5, 0.1, 0.02
                        );
                        finalWorld.spawnParticle(
                            Particle.REVERSE_PORTAL,
                            x, finalLoc.getY() + 2, z,
                            2, 0.1, 0.3, 0.1, 0.01
                        );
                    }, (long)(finalR * 2));
                }
            }
            
            // Explosión central
            finalWorld.spawnParticle(Particle.SOUL, finalLoc.clone().add(0.5, 1, 0.5), 50, 0.5, 1, 0.5, 0.1);
            finalWorld.spawnParticle(Particle.ENCHANT, finalLoc.clone().add(0.5, 1, 0.5), 30, 1, 2, 1, 0.5);
            
            // Sonidos épicos
            finalWorld.playSound(finalLoc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.8f);
            finalWorld.playSound(finalLoc, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);
            finalWorld.playSound(finalLoc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.6f, 0.6f);
        }, 5L);
    }
    
    private void iniciarEfectosFragmentos() {
        fragmentosParticleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (actoActual != Acto.PIEDRA_DESPIERTA) {
                // ✅ Cancelar el task antes de salir
                if (fragmentosParticleTask != null) {
                    fragmentosParticleTask.cancel();
                }
                return;
            }
            
            // ✅ Validar que hay fragmentos
            if (fragmentosLocations.isEmpty()) {
                return;
            }
            
            long tiempo = System.currentTimeMillis() / 1000;
            double pulso = Math.sin(System.currentTimeMillis() / 500.0) * 0.5 + 0.5; // 0.0 - 1.0
            
            for (Location fragmento : fragmentosLocations) {
                Location centro = fragmento.clone().add(0.5, 1, 0.5);
                
                // 1. Aura pulsante "respiración" - se expande y contrae (REDUCIDO)
                double radioPulso = 1.2 + (pulso * 0.8); // 1.2 - 2.0 bloques
                for (int i = 0; i < 6; i++) { // Reducido de 12 a 6
                    double angle = (i * Math.PI * 2) / 6;
                    double x = Math.cos(angle) * radioPulso;
                    double z = Math.sin(angle) * radioPulso;
                    
                    fragmento.getWorld().spawnParticle(
                        Particle.SOUL_FIRE_FLAME,
                        centro.clone().add(x, pulso * 0.5, z),
                        1,
                        0, 0, 0,
                        0
                    );
                }
                
                // 2. Grietas emanando del fragmento (BLOCK_CRACK) - reducido
                if (tiempo % 3 == 0) { // Cada 3s en lugar de 2s
                    for (int i = 0; i < 4; i++) { // Reducido de 8 a 4
                        double angle = Math.random() * Math.PI * 2;
                        double dist = Math.random() * 2;
                        fragmento.getWorld().spawnParticle(
                            Particle.BLOCK,
                            centro.clone().add(
                                Math.cos(angle) * dist,
                                -0.8,
                                Math.sin(angle) * dist
                            ),
                            2, // Reducido de 3 a 2
                            0.1, 0, 0.1,
                            Material.DEEPSLATE_BRICKS.createBlockData()
                        );
                    }
                }
                
                // 3. Rayo de luz vertical hasta el cielo (Y=320) - MUY REDUCIDO
                for (int y = 2; y < 60; y += 5) { // Cada 5 bloques en lugar de 3
                    fragmento.getWorld().spawnParticle(
                        Particle.END_ROD,
                        centro.clone().add(0, y, 0),
                        1,
                        0.1, 0.1, 0.1,
                        0
                    );
                    
                    // Partículas adicionales cada 15 bloques (antes 10)
                    if (y % 15 == 0) {
                        fragmento.getWorld().spawnParticle(
                            Particle.GLOW,
                            centro.clone().add(0, y, 0),
                            3, // Reducido de 5 a 3
                            0.3, 0.3, 0.3,
                            0
                        );
                    }
                }
                
                // 4. Partículas PORTAL giratorio doble hélice - REDUCIDO
                double t = System.currentTimeMillis() / 1000.0;
                for (int i = 0; i < 3; i++) { // Reducido de 6 a 3
                    double angle1 = (t + i * Math.PI / 1.5) % (Math.PI * 2);
                    double angle2 = (t + i * Math.PI / 1.5 + Math.PI) % (Math.PI * 2);
                    
                    // Primera hélice
                    fragmento.getWorld().spawnParticle(
                        Particle.PORTAL,
                        centro.clone().add(
                            Math.cos(angle1) * 0.8,
                            Math.sin(t * 2 + i) * 0.5,
                            Math.sin(angle1) * 0.8
                        ),
                        1, 0, 0, 0, 0.01
                    );
                    
                    // Segunda hélice
                    fragmento.getWorld().spawnParticle(
                        Particle.REVERSE_PORTAL,
                        centro.clone().add(
                            Math.cos(angle2) * 0.8,
                            Math.sin(t * 2 + i + Math.PI) * 0.5,
                            Math.sin(angle2) * 0.8
                        ),
                        1, 0, 0, 0, 0.01
                    );
                }
                
                // 5. Onda expansiva cada 8 segundos (antes 5)
                if (tiempo % 8 == 0) {
                    for (double radio = 0.5; radio <= 3.0; radio += 0.8) { // Menos ondas
                        final double r = radio;
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            for (int i = 0; i < 12; i++) { // Reducido de 20 a 12
                                double angle = (i * Math.PI * 2) / 12;
                                fragmento.getWorld().spawnParticle(
                                    Particle.SOUL,
                                    centro.clone().add(
                                        Math.cos(angle) * r,
                                        0.1,
                                        Math.sin(angle) * r
                                    ),
                                    0,
                                    Math.cos(angle) * 0.2,
                                    0,
                                    Math.sin(angle) * 0.2,
                                    0.1
                                );
                            }
                        }, (long)(radio * 2));
                    }
                    
                    // Sonido de pulso
                    fragmento.getWorld().playSound(centro, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.5f, 0.8f);
                }
                
                // 6. Partículas ENCHANT levitando - REDUCIDO
                if (tiempo % 2 == 0) { // Solo cada 2 segundos
                    fragmento.getWorld().spawnParticle(
                        Particle.ENCHANT,
                        centro.clone().add(
                            (Math.random() - 0.5) * 0.5,
                            -0.5,
                            (Math.random() - 0.5) * 0.5
                        ),
                        1, 0, 0.5, 0, 0.02
                    );
                }
            }
        }, 0L, 5L); // Cada 5 ticks (0.25s) en lugar de 2 ticks
    }
    
    private void verificarProximidadFragmentos() {
        if (ticksEnActo % 10 != 0) return; // Verificar cada 0.5s
        
        // ✅ Null safety
        if (fragmentosLocations.isEmpty()) return;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (Location fragmento : fragmentosLocations) {
                // ✅ Verificar cada fragmento
                if (fragmento == null || fragmento.getWorld() == null) {
                    continue;
                }
                if (fragmentosInspeccionados.contains(fragmento)) {
                    continue;
                }
                
                double distancia = player.getLocation().distance(fragmento);
                
                if (distancia < 5.0) {
                    // Jugador descubrió fragmento
                    if (!fragmentosDescubiertos.contains(fragmento)) {
                        fragmentosDescubiertos.add(fragmento);
                        onFragmentoDescubierto(player, fragmento);
                    }
                }
            }
        }
    }
    
    private void onFragmentoDescubierto(Player player, Location fragmento) {
        // Tracking
        participacionFragmentos.put(
            player.getUniqueId(),
            participacionFragmentos.getOrDefault(player.getUniqueId(), 0) + 1
        );
        
        // Tracking de jugador individual
        jugadoresFragmentosVistos.putIfAbsent(player.getUniqueId(), new HashSet<>());
        jugadoresFragmentosVistos.get(player.getUniqueId()).add(fragmento);
        
        // ✨ MEJORADO: Actualizar BossBar con progreso de ALTARES completados (no descubiertos)
        if (bossBarProgreso != null) {
            // Mostrar altares completados, no fragmentos descubiertos
            int altaresCompletados = fragmentosInspeccionados.size();
            double progreso = (double) altaresCompletados / fragmentosLocations.size();
            bossBarProgreso.setProgress(Math.min(progreso, 1.0));
            bossBarProgreso.setTitle("§5Altares: " + altaresCompletados + "/" + fragmentosLocations.size() + " §7| §eActual: " + altarActualGlobal);
        }
        
        Location centro = fragmento.clone().add(0.5, 1, 0.5);
        
        // Efectos visuales cinematográficos
        // 1. Explosión de partículas PORTAL
        fragmento.getWorld().spawnParticle(
            Particle.PORTAL,
            centro,
            50,
            0.5, 0.5, 0.5,
            0.5
        );
        
        // 2. Partículas SOUL subiendo
        fragmento.getWorld().spawnParticle(
            Particle.SOUL,
            centro,
            20,
            0.3, 0.2, 0.3,
            0.1
        );
        
        // 3. Anillo de partículas GLOW
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI / 8;
            double x = Math.cos(angle) * 1.5;
            double z = Math.sin(angle) * 1.5;
            fragmento.getWorld().spawnParticle(
                Particle.GLOW,
                centro.clone().add(x, 0, z),
                3,
                0.1, 0.1, 0.1,
                0.02
            );
        }
        
        // 4. Partículas END_ROD elevadas
        fragmento.getWorld().spawnParticle(
            Particle.END_ROD,
            centro,
            15,
            0.5, 1.0, 0.5,
            0.1
        );
        
        // ✨ NUEVO: Checkpoint visual dramático
        for (int i = 0; i < 3; i++) {
            final int index = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Anillo de partículas que se expande
                for (int angle = 0; angle < 360; angle += 15) {
                    double rad = Math.toRadians(angle);
                    double radio = 2.5 + index * 1.5;
                    Location particleLoc = centro.clone().add(
                        Math.cos(rad) * radio,
                        0.2,
                        Math.sin(rad) * radio
                    );
                    fragmento.getWorld().spawnParticle(
                        Particle.TOTEM_OF_UNDYING,
                        particleLoc,
                        1,
                        0, 0, 0,
                        0
                    );
                }
                soundUtil.playSound(centro, Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 1.5f + index * 0.2f);
            }, index * 5L);
        }
        
        // ✨ NUEVO: Sonidos únicos por fragmento (escala musical)
        int fragmentoIndex = fragmentosInspeccionados.size();
        float[] notas = {0.5f, 0.6f, 0.75f, 0.9f, 1.0f, 1.2f, 1.4f}; // Escala pentatónica
        float nota = notas[fragmentoIndex % notas.length];
        soundUtil.playSound(fragmento, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, nota);
        soundUtil.playSound(fragmento, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.5f);
        soundUtil.playSound(fragmento, Sound.BLOCK_BELL_USE, 0.8f, nota * 1.2f);
        
        // 🗣️ DIÁLOGO - Fragmento encontrado
        mostrarDialogoForma("FRAGMENTO_ENCONTRADO");
        
        // 🎮 DESAFÍO: 33% probabilidad de spawn de criaturas al recoger fragmento
        Random rand = new Random();
        if (rand.nextDouble() < 0.33) {
            int cantidadCriaturas = 2 + rand.nextInt(2); // 2-3 criaturas
            broadcastNarrative("§c⚠ ¡El fragmento ha despertado criaturas cercanas!");
            
            plugin.getLogger().info(String.format(
                "[SusurroPiedraRota] Fragmento con desafío - Spawneando %d criaturas",
                cantidadCriaturas
            ));
            
            // Spawn las criaturas cerca del fragmento
            for (int i = 0; i < cantidadCriaturas; i++) {
                final int delay = 10 + (i * 15); // 0.5s, 1.25s, 2s...
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (isActive() && actoActual == Acto.PIEDRA_DESPIERTA) {
                        Location spawnLoc = encontrarSpawnSeguro(fragmento, 3, 8);
                        if (spawnLoc == null) {
                            spawnLoc = fragmento.clone().add(0, 1, 0);
                        }
                        spawnearEnUbicacion(spawnLoc);
                    }
                }, delay);
            }
        }
        
        // 🎬 EFECTO DE CÁMARA: Efecto épico combinado al encontrar fragmento
        aplicarEfectoEpicoCombinado(player);
        
        // ✨ MINI-CINEMÁTICA AL COMPLETAR FRAGMENTO (1.5 segundos)
        // Efecto de "slow motion" con SLOW + sensación de cámara lenta
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.SLOWNESS,
            30, // 1.5 segundos
            2,
            true,
            false
        ));
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.MINING_FATIGUE,
            30,
            1,
            true,
            false
        ));
        
        // Simulación de "zoom in" con FOV (SLOW_FALLING da sensación de cámara lenta)
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.SLOW_FALLING,
            30,
            0,
            true,
            false
        ));
        
        // Título cinemático durante slow motion usando método nativo de Paper/Adventure API
        player.showTitle(net.kyori.adventure.title.Title.title(
            net.kyori.adventure.text.Component.text("§5§l✦ FRAGMENTO DESCUBIERTO ✦"),
            net.kyori.adventure.text.Component.text("§7" + fragmentosDescubiertos.size() + "/" + fragmentosLocations.size()),
            net.kyori.adventure.title.Title.Times.times(
                java.time.Duration.ofMillis(250),
                java.time.Duration.ofSeconds(1),
                java.time.Duration.ofMillis(250)
            )
        ));
        
        // ✨ Preview del próximo fragmento con beacon (Preparado - requiere implementación completa)
        // Location proximoFragmento = encontrarFragmentoMasCercano(player.getLocation());
        // if (proximoFragmento != null && beaconsPreview != null) {
        //     mostrarBeaconPreview(proximoFragmento);
        // }
        
        // Mensaje fragmentado con animación de susurro
        // Cada fragmento tiene un susurro único que da contexto narrativo
        String[] mensajes = {
            "§8⧖ §7...la piedra te reconoce... eres digno de escuchar...",
            "§8⧖ §7...otro fragmento vibra... la memoria se reconstruye...",
            "§8⧖ §7...los ecos se fortalecen... la forma busca existir...",
            "§8⧖ §7...casi completo... pronto la verdad será revelada...",
            "§8⧖ §7...el último susurro... la memoria quiere nacer..."
        };
        
        int index = fragmentosLocations.indexOf(fragmento);
        String mensaje = mensajes[index % mensajes.length];
        
        enviarMensajeDescubrimiento(
            player,
            "FRAGMENTO DESCUBIERTO",
            fragmentosDescubiertos.size(),
            fragmentosLocations.size()
        );
        
        // Susurro animado después del descubrimiento
        mostrarSusurroFragmento(player, mensaje, 40L);
        
        // Destello suave al descubrir fragmento
        crearDestello(player, 3);
        
        soundUtil.playSound(player, Sound.ENTITY_ENDERMAN_STARE, 0.3f, 0.5f);
        
        // Asignar altar actual como objetivo (no el más cercano)
        Location altarActualLoc = obtenerLocationAltarActual();
        if (altarActualLoc != null) {
            objetivosPorJugador.put(player.getUniqueId(), altarActualLoc);
        } else {
            objetivosPorJugador.remove(player.getUniqueId());
        }
        
        plugin.getLogger().info(String.format(
            "[SusurroPiedraRota] %s descubrió fragmento #%d (%d/%d)",
            player.getName(),
            index + 1,
            fragmentosDescubiertos.size(),
            fragmentosLocations.size()
        ));
    }
    
    private void tickActo1() {
        // Mensajes narrativos progresivos con timing y animaciones
        if (ticksEnActo == 100) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                mostrarMensajeNarrativoAnimado(p, "§8Un susurro recorre la piedra...", 0L);
            }
            reproducirSonidoAmbientalMisterioso();
        } else if (ticksEnActo == 400) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                mostrarMensajeNarrativoAnimado(p, "§8Los fragmentos guardan secretos ancestrales.", 0L);
            }
        } else if (ticksEnActo == 800) {
            int restantes = fragmentosLocations.size() - fragmentosDescubiertos.size();
            if (restantes > 0) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    mostrarMensajeNarrativoAnimado(p, "§5⚡ Quedan " + restantes + " fragmentos por descubrir", 0L);
                }
            }
        }
        
        // SISTEMA DE HINTS PROGRESIVOS
        verificarHintsFragmentos();
        
        // SISTEMA DE ALTARES - Verificar proximidad y progreso
        verificarProximidadAltares();
        
        // Verificar si todos los fragmentos fueron inspeccionados
        // IMPORTANTE: Solo verificar si ya se generaron fragmentos y no se ha completado aún
        if (!acto1Completado &&
            fragmentosLocations.size() > 0 && 
            fragmentosInspeccionados.size() >= fragmentosLocations.size() &&
            ticksEnActo > 200) { // Esperar al menos 10 segundos después de iniciar el acto
            // Completar Acto 1
            acto1Completado = true;
            completarActo1();
        }
    }
    
    private void completarActo1() {
        plugin.getLogger().info("[SusurroPiedraRota] Acto 1 completado");
        
        // 🧹 LIMPIEZA: Eliminar fragmentos al completar Acto 1
        limpiarFragmentos();
        
        // ✨ Calcular bonus por velocidad
        if (tiempoInicioActo1 > 0) {
            long tiempoTranscurrido = System.currentTimeMillis() - tiempoInicioActo1;
            int minutos = (int)(tiempoTranscurrido / 60000);
            int segundos = (int)((tiempoTranscurrido % 60000) / 1000);
            String bonusMsg = "";
            
            if (tiempoTranscurrido <= TIEMPO_BONUS_ORO) {
                bonusMsg = "\n§6★★★ ¡BONUS ORO! §e+50% experiencia";
                playSoundToAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            } else if (tiempoTranscurrido <= TIEMPO_BONUS_PLATA) {
                bonusMsg = "\n§7★★☆ ¡BONUS PLATA! §e+30% experiencia";
                playSoundToAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.9f);
            } else if (tiempoTranscurrido <= TIEMPO_BONUS_BRONCE) {
                bonusMsg = "\n§c★☆☆ ¡BONUS BRONCE! §e+10% experiencia";
                playSoundToAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
            }
            
            if (!bonusMsg.isEmpty()) {
                broadcastNarrative("§5Tiempo: §f" + minutos + "m " + segundos + "s" + bonusMsg);
            }
        }
        
        // Transición cinematográfica MEJORADA
        actoActual = Acto.TRANSICION_2;
        
        // ✨ NUEVO: Limpiar mobs hostiles para la transición narrativa
        limpiarMobsHostilesCercanos();
        
        // SECUENCIA DE TRANSICIÓN ÉPICA
        // 1. Congelar jugadores
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participantesOriginales.contains(p.getUniqueId())) {
                p.setWalkSpeed(0f);
            }
        }
        
        // 2. Fadeout negro dramático
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(
                "§8█████████████████",
                "",
                10, 80, 20
            );
        }
        
        // 3. Sonido de impacto dramático en capas
        playSoundToAll(Sound.ENTITY_WITHER_DEATH, 0.8f, 0.5f);
        playSoundToAll(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.7f);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            playSoundToAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 0.6f);
        }, 10L);
        
        // 4. Efecto de distorsión dimensional (grieta rasgando realidad)
        efectoDistorsionDimensionalTodos(40);
        
        // 5. Destello blanco en momento de impacto
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            crearDestelloTodos(8);
            playSoundToAll(Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
        }, 15L);
        
        // 6. Sacudida de pantalla
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            sacudirPantallaTodos(5);
        }, 20L);
        
        // 7. Título épico después del fadeout
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            enviarTituloCinematicoTodos(
                "⚡ LA PIEDRA SE QUIEBRA ⚡",
                "Una grieta dimensional rasga la realidad...",
                60
            );
        }, 40L);
        
        // Partículas de transición masivas en todos los fragmentos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Location frag : fragmentosLocations) {
                if (frag != null && frag.getWorld() != null) {
                    // Explosión de partículas
                    for (int i = 0; i < 100; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double dist = Math.random() * 5;
                        double dx = Math.cos(angle) * dist;
                        double dz = Math.sin(angle) * dist;
                        frag.getWorld().spawnParticle(
                            Particle.SOUL_FIRE_FLAME,
                            frag.clone().add(dx, 1, dz),
                            0,
                            0, 1, 0,
                            0.1
                        );
                    }
                    // Implosión de REVERSE_PORTAL
                    for (int i = 0; i < 50; i++) {
                        frag.getWorld().spawnParticle(
                            Particle.REVERSE_PORTAL,
                            frag.clone().add(
                                (Math.random() - 0.5) * 3,
                                Math.random() * 2,
                                (Math.random() - 0.5) * 3
                            ),
                            0,
                            0, 0, 0,
                            0
                        );
                    }
                }
            }
        }, 20L);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                iniciarActo2();
            }
        }, 100L); // 5 segundos de transición dramática mejorada
        
        // 8. Restaurar movimiento de jugadores
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (participantesOriginales.contains(p.getUniqueId())) {
                    p.setWalkSpeed(0.2f);
                }
            }
        }, 95L);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE ALTARES - ACTO 1
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Verificar proximidad de jugadores a altares y gestionar activaciones SINCRONIZADAS
     */
    private void verificarProximidadAltares() {
        if (ticksEnActo % 10 != 0) return; // Cada 0.5s
        
        // Obtener el altar actual que debe completarse
        Location altarActualLoc = obtenerLocationAltarActual();
        if (altarActualLoc == null) return;
        
        // Contar jugadores en survival cerca del altar actual
        jugadoresPresentesEnAltar.clear();
        int jugadoresEnSurvival = 0;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!participantesOriginales.contains(player.getUniqueId())) continue;
            if (player.getGameMode() != org.bukkit.GameMode.SURVIVAL) continue;
            
            jugadoresEnSurvival++;
            double distancia = player.getLocation().distance(altarActualLoc);
            
            // Mensaje de entrada a zona de altar (25 bloques)
            // ✨ Mejorado: También verificar si el altar registrado es el actual (puede haber cambiado)
            if (distancia < 25 && distancia > 20) {
                Integer altarRegistrado = altarActualJugador.get(player.getUniqueId());
                if (altarRegistrado == null || altarRegistrado != altarActualGlobal) {
                    String nombreAltar = obtenerNombreAltar(altarActualGlobal);
                    player.sendMessage("");
                    player.sendMessage("§5§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    player.sendMessage("");
                    player.sendMessage("§8⧖ " + nombreAltar);
                    player.sendMessage("§7Un fragmento de memoria corrupta");
                    player.sendMessage("");
                    
                    // Mostrar propósito del altar según número
                    switch (altarActualGlobal) {
                        case 1:
                            player.sendMessage("§8El Observador§7: §o\"...siente el susurro de la piedra...\"");
                            player.sendMessage("§8El Observador§7: §o\"...el mundo recuerda... mal...\"");
                            player.sendMessage("§7");
                            player.sendMessage("§7Este altar guarda la primera memoria fragmentada.");
                            player.sendMessage("§7Debes sincronizarte con su resonancia.");
                            break;
                        case 2:
                            player.sendMessage("§8El Observador§7: §o\"...objetos del vacío...\"");
                            player.sendMessage("§8El Observador§7: §o\"...que atravesaron dimensiones...\"");
                            player.sendMessage("§7");
                            player.sendMessage("§7Este altar ansía energía dimensional.");
                            player.sendMessage("§7Necesita ser alimentado con poder del End.");
                            break;
                        case 3:
                            player.sendMessage("§8El Observador§7: §o\"...esencia vital...\"");
                            player.sendMessage("§8El Observador§7: §o\"...el precio del recuerdo...\"");
                            player.sendMessage("§7");
                            player.sendMessage("§7Este altar requiere un sacrificio.");
                            player.sendMessage("§7La vida alimenta la memoria rota.");
                            break;
                        case 4:
                            player.sendMessage("§8El Observador§7: §o\"...copias erróneas emergen...\"");
                            player.sendMessage("§8El Observador§7: §o\"...el mundo se multiplica mal...\"");
                            player.sendMessage("§7");
                            player.sendMessage("§7Este altar está corrompido.");
                            player.sendMessage("§7Elimina los errores para purificarlo.");
                            break;
                        case 5:
                            player.sendMessage("§8El Observador§7: §o\"...los fragmentos resuenan juntos...\"");
                            player.sendMessage("§8El Observador§7: §o\"...pero algo más despierta...\"");
                            player.sendMessage("§7");
                            player.sendMessage("§7Este es el altar final.");
                            player.sendMessage("§7La unión revelará la verdad.");
                            break;
                    }
                    
                    player.sendMessage("");
                    player.sendMessage("§e⚠ Espera a que todos lleguen para comenzar");
                    player.sendMessage("§5§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    player.sendMessage("");
                    soundUtil.playSound(player, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.3f, 0.8f);
                    
                    // ✨ NUEVO: Registrar que este jugador ya vio el mensaje de este altar
                    altarActualJugador.put(player.getUniqueId(), altarActualGlobal);
                }
            }
            
            // ✨ NUEVO: Mostrar ActionBar informativo cuando estás cerca pero sin actividad
            if (distancia < 15.0 && !altarEnProgreso && !esperandoJugadores) {
                player.sendActionBar(net.kyori.adventure.text.Component.text(
                    "§5⧖ §e" + obtenerNombreAltar(altarActualGlobal) + " §7- Acércate más al centro"
                ));
            }
            
            // Registrar jugadores cercanos (15 bloques)
            if (distancia < 15.0) {
                jugadoresPresentesEnAltar.add(player.getUniqueId());
            }
        }
        
        // Verificar si todos están presentes
        boolean todosPresentes = (jugadoresEnSurvival > 0 && jugadoresPresentesEnAltar.size() >= jugadoresEnSurvival);
        
        // ✨ DEBUG: Log cada 5 segundos si hay jugadores cerca
        if (ticksEnActo % 100 == 0 && jugadoresPresentesEnAltar.size() > 0) {
            plugin.getLogger().info(String.format(
                "[SusurroPiedraRota] DEBUG Altar %d: presentes=%d/%d, todosPresentes=%s, enProgreso=%s, esperando=%s",
                altarActualGlobal, jugadoresPresentesEnAltar.size(), jugadoresEnSurvival, 
                todosPresentes, altarEnProgreso, esperandoJugadores
            ));
        }
        
        if (todosPresentes && !altarEnProgreso) {
            if (!esperandoJugadores) {
                // Primer tick con todos presentes - iniciar cuenta regresiva
                esperandoJugadores = true;
                tiempoInicioEspera = System.currentTimeMillis();
                
                plugin.getLogger().info("[SusurroPiedraRota] Altar " + altarActualGlobal + ": Todos reunidos, iniciando cuenta regresiva de 3s");
                
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (jugadoresPresentesEnAltar.contains(p.getUniqueId())) {
                        p.sendTitle("§5⧖", "§7Todos reunidos... preparando ritual", 10, 40, 10);
                        soundUtil.playSound(p, Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.2f);
                    }
                }
            } else {
                // Verificar si pasaron 3 segundos
                long tiempoEspera = System.currentTimeMillis() - tiempoInicioEspera;
                if (tiempoEspera >= 3000 && !altarEnProgreso) {
                    plugin.getLogger().info("[SusurroPiedraRota] Altar " + altarActualGlobal + ": 3s completados, iniciando actividad");
                    iniciarActividadAltar(altarActualGlobal, altarActualLoc);
                }
            }
        } else if (!todosPresentes) {
            // Reset si alguien se aleja
            if (esperandoJugadores && !altarEnProgreso) {
                esperandoJugadores = false;
            }
            
            // Mostrar quién falta (cada 5 segundos)
            if (ticksEnActo % 100 == 0 && jugadoresPresentesEnAltar.size() > 0) {
                int faltantes = jugadoresEnSurvival - jugadoresPresentesEnAltar.size();
                if (faltantes > 0) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (jugadoresPresentesEnAltar.contains(p.getUniqueId())) {
                            p.sendActionBar("§e⚠ Esperando a " + faltantes + " jugador(es)...");
                        }
                    }
                }
            }
        }
        
        // Si hay actividad en progreso, procesarla
        if (altarEnProgreso) {
            procesarActividadAltarActual();
        }
    }
    
    /**
     * Obtener la Location del altar actual que debe completarse
     */
    private Location obtenerLocationAltarActual() {
        for (Map.Entry<Location, Integer> entry : fragmentoANumeroAltar.entrySet()) {
            if (entry.getValue() == altarActualGlobal) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * Obtener nombre del altar según número
     */
    private String obtenerNombreAltar(int numAltar) {
        switch (numAltar) {
            case 1: return "§5Altar del Despertar";
            case 2: return "§5Altar de la Resonancia";
            case 3: return "§5Altar del Sacrificio";
            case 4: return "§5Altar de la Caza";
            case 5: return "§5Altar de la Unión";
            default: return "§5Altar Antiguo";
        }
    }
    
    /**
     * Iniciar la actividad de un altar cuando todos están presentes
     */
    private void iniciarActividadAltar(int numAltar, Location altarLoc) {
        altarEnProgreso = true;
        esperandoJugadores = false;
        tiempoInicioActividad = System.currentTimeMillis(); // Iniciar timer para timeout
        
        // ✨ NUEVO: Limpiar mobs hostiles para leer las instrucciones con calma
        limpiarMobsHostilesCercanos();
        
        // Mensaje de inicio
        String nombreAltar = obtenerNombreAltar(numAltar);
        broadcastNarrative("");
        broadcastNarrative("§5§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        broadcastNarrative("");
        broadcastNarrative("          " + nombreAltar);
        broadcastNarrative("");
        
        // Mostrar diálogo narrativo SOLO si este altar NO ha sido completado aún
        // Esto evita mostrar diálogos de altares futuros cuando llegas a un fragmento
        if (!fragmentosInspeccionados.contains(altarLoc)) {
            // Mostrar diálogo narrativo ANTES de la actividad con suspenso
            switch (numAltar) {
                case 1:
                    broadcastNarrative("    §8El Observador§7: §o\"...el primer fragmento yace ante ustedes...\"");
                    broadcastNarrative("    §8El Observador§7: §o\"...pero está dormido... muy dormido...\"");
                    broadcastNarrative("");
                    // Pausa dramática antes de revelar
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        broadcastNarrative("    §8El Observador§7: §o\"...para despertarlo...\"");
                        broadcastNarrative("    §8El Observador§7: §o\"...deben...\"");
                    }, 30L);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        broadcastNarrative("");
                        broadcastNarrative("    §5§l☽ EL DESPERTAR ☽");
                        broadcastNarrative("    §d§oQuietud absoluta...");
                        broadcastNarrative("");
                        broadcastNarrative("    §e▸ §fQuédense §lcompletamente inmóviles");
                        broadcastNarrative("    §e▸ §fDuración: §a10 segundos");
                        broadcastNarrative("    §e▸ §fDistancia: §a15 bloques §7del altar");
                        broadcastNarrative("");
                        broadcastNarrative("    §8§o\"El altar solo escucha a quienes callan...\"");
                        playSoundToAll(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.6f);
                    }, 60L);
                    break;
                case 2:
                    broadcastNarrative("    §8El Observador§7: §o\"...escuchen... algo resuena en el vacío...\"");
                    broadcastNarrative("    §8El Observador§7: §o\"...energía de otra dimensión...\"");
                    broadcastNarrative("");
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        broadcastNarrative("    §8El Observador§7: §o\"...el fragmento la necesita...\"");
                        broadcastNarrative("    §8El Observador§7: §o\"...dennos...\"");
                    }, 30L);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        broadcastNarrative("");
                        broadcastNarrative("    §5§l◈ LA RESONANCIA ◈");
                        broadcastNarrative("    §d§oObjetos del vacío...");
                        broadcastNarrative("");
                        broadcastNarrative("    §e▸ §fConsigan §b8 Ender Pearls");
                        broadcastNarrative("    §e▸ §fTírenlas cerca del altar §7(tecla Q)");
                        broadcastNarrative("    §e▸ §fProgreso: §a0/8 §7perlas");
                        broadcastNarrative("");
                        broadcastNarrative("    §8§o\"Las perlas atravesaron dimensiones... su energía es pura...\"");
                        playSoundToAll(Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 1.2f);
                    }, 60L);
                    break;
                case 3:
                    broadcastNarrative("    §8El Observador§7: §o\"...este fragmento... tiene hambre...\"");
                    broadcastNarrative("    §8El Observador§7: §o\"...hambre de algo valioso...\"");
                    broadcastNarrative("");
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        broadcastNarrative("    §8El Observador§7: §o\"...sus tesoros...\"");
                        broadcastNarrative("    §8El Observador§7: §o\"...deben...\"");
                    }, 30L);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        broadcastNarrative("");
                        broadcastNarrative("    §6§l✦ EL SACRIFICIO ✦");
                        broadcastNarrative("    §d§oOfrendas de valor...");
                        broadcastNarrative("");
                        broadcastNarrative("    §e▸ §fTiren §6objetos valiosos §fal altar:");
                        broadcastNarrative("       §bDiamante §8= §f3 pts  §7|  §6Oro §8= §f1 pt");
                        broadcastNarrative("       §aEsmeralda §8= §f2 pts  §7|  §4Netherite §8= §f5 pts");
                        broadcastNarrative("    §e▸ §fMeta: §610 puntos §7de ofrendas");
                        broadcastNarrative("");
                        broadcastNarrative("    §8§o\"El altar consume lo que más valoran...\"");
                        playSoundToAll(Sound.BLOCK_FIRE_AMBIENT, 0.8f, 0.8f);
                    }, 60L);
                    break;
                case 4:
                    broadcastNarrative("    §8El Observador§7: §o\"...algo viene...\"");
                    broadcastNarrative("    §8El Observador§7: §o\"...puedo sentirlo...\"");
                    broadcastNarrative("");
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        broadcastNarrative("    §8El Observador§7: §o\"...criaturas del vacío...\"");
                        broadcastNarrative("    §8El Observador§7: §o\"...prepárense para...\"");
                    }, 30L);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        broadcastNarrative("");
                        broadcastNarrative("    §c§l☠ LA CAZA ☠");
                        broadcastNarrative("    §d§oSangre por memorias...");
                        broadcastNarrative("");
                        broadcastNarrative("    §e▸ §fEnemigos van a aparecer cerca del altar");
                        broadcastNarrative("    §e▸ §fEliminen §c5 mobs hostiles");
                        broadcastNarrative("    §e▸ §fMobs naturales §7(50 bloques) §ftambién cuentan");
                        broadcastNarrative("");
                        broadcastNarrative("    §8§o\"La sangre derramada alimenta la memoria rota...\"");
                        playSoundToAll(Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.8f);
                    }, 60L);
                    break;
                case 5:
                    broadcastNarrative("    §8El Observador§7: §o\"...los cuatro fragmentos resuenan...\"");
                    broadcastNarrative("    §8El Observador§7: §o\"...pero algo más despierta...\"");
                    broadcastNarrative("");
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        broadcastNarrative("    §8El Observador§7: §o\"...algo oscuro...\"");
                        broadcastNarrative("    §8El Observador§7: §o\"...algo que no quiere dejarlos ir...\"");
                    }, 30L);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        broadcastNarrative("");
                        broadcastNarrative("    §5§l⚔ LA UNIÓN FINAL ⚔");
                        broadcastNarrative("    §d§oResistan... o mueran...");
                        broadcastNarrative("");
                        broadcastNarrative("    §e▸ §fQuédense cerca del altar §7(15 bloques)");
                        broadcastNarrative("    §e▸ §fOleadas de enemigos vendrán por ustedes");
                        broadcastNarrative("    §e▸ §fAguanten §c30 segundos §fde resistencia");
                        broadcastNarrative("");
                        broadcastNarrative("    §8§o\"La memoria corrupta los probará... ¿son dignos?\"");
                        playSoundToAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 0.6f);
                    }, 60L);
                    break;
            }
            
            broadcastNarrative("");
            broadcastNarrative("§5§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            broadcastNarrative("");
        } else {
            // Este altar ya fue completado - solo mostrar mensaje simple
            broadcastNarrative("    §7Este fragmento ya ha sido activado.");
            broadcastNarrative("");
            broadcastNarrative("§5§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            broadcastNarrative("");
            altarEnProgreso = false;
            esperandoJugadores = false;
            return;
        }
        
        playSoundToAll(Sound.ENTITY_WARDEN_AMBIENT, 0.6f, 0.6f);
        playSoundToAll(Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);
        
        // Inicializar tracking para todos los jugadores presentes
        for (UUID uuid : jugadoresPresentesEnAltar) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || p.getGameMode() != org.bukkit.GameMode.SURVIVAL) continue;
            
            tiempoInicioAltarJugador.put(uuid, System.currentTimeMillis());
            posicionInicioAltarJugador.put(uuid, p.getLocation().clone());
            vidaInicioAltarJugador.put(uuid, p.getHealth());
            criaturasEliminadasPorJugador.put(uuid, 0);
        }
        
        // Limpiar contadores específicos de altares
        if (numAltar == 2) {
            // Reset contadores de Altar 2 (Resonancia - Perlas)
            perlasEntregadasAltar2 = 0;
            itemsProcesadosEnAltar.clear();
            plugin.getLogger().info("[SusurroPiedraRota] Altar 2 iniciado - contador de perlas reseteado");
        }
        
        if (numAltar == 3) {
            // Reset contadores de Altar 3 (Sacrificio de items)
            itemsSacrificadosAltar3 = 0;
            itemsProcesadosSacrificio.clear();
            altarActualLocation = altarLoc;
            plugin.getLogger().info("[SusurroPiedraRota] Altar 3 iniciado - contador de sacrificios reseteado");
        }
        
        if (numAltar == 4) {
            // Reset contadores de Altar 4 (Caza de mobs hostiles)
            mobsHostilesEliminadosAltar4 = 0;
            altarActualLocation = altarLoc;
            criaturasDeAltar.clear();
            plugin.getLogger().info("[SusurroPiedraRota] Altar 4 iniciado - contador de mobs reseteado");
        }
    }
    
    /**
     * Procesar la actividad del altar actual en progreso
     */
    private void procesarActividadAltarActual() {
        Location altarLoc = obtenerLocationAltarActual();
        if (altarLoc == null) return;
        
        // === SISTEMA DE FALLBACK/TIMEOUT ===
        // Si la actividad tarda demasiado, ofrecer opciones
        long tiempoTranscurrido = System.currentTimeMillis() - tiempoInicioActividad;
        
        // Advertencia a los 2 minutos
        if (tiempoTranscurrido >= 120000 && tiempoTranscurrido < 121000) {
            broadcastNarrative("");
            broadcastNarrative("§e⚠ La actividad está tardando demasiado...");
            broadcastNarrative("§7Si están teniendo problemas, el altar se completará automáticamente en 1 minuto.");
            playSoundToAll(Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
        }
        
        // Timeout a los 3 minutos - completar automáticamente
        if (tiempoTranscurrido >= TIMEOUT_ACTIVIDAD_MS) {
            broadcastNarrative("");
            broadcastNarrative("§6⧖ El Observador interviene...");
            broadcastNarrative("§8El Observador§7: §o\"...el tiempo no espera... os ayudaré esta vez...\"");
            broadcastNarrative("");
            playSoundToAll(Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 0.5f);
            
            // Completar el altar automáticamente (fallback)
            intentosFallidosActividad++;
            completarAltarGrupal(altarActualGlobal, altarLoc);
            
            plugin.getLogger().warning(String.format(
                "[SusurroPiedraRota] Altar %d completado por TIMEOUT (fallback). Intentos fallidos totales: %d",
                altarActualGlobal - 1, intentosFallidosActividad
            ));
            return;
        }
        
        // Procesar según el tipo de altar
        for (UUID uuid : jugadoresPresentesEnAltar) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || p.getGameMode() != org.bukkit.GameMode.SURVIVAL) continue;
            
            switch (altarActualGlobal) {
                case 1:
                    procesarAltar1DespertarGrupal(p, altarLoc);
                    break;
                case 2:
                    procesarAltar2ResonanciaGrupal(p, altarLoc);
                    break;
                case 3:
                    procesarAltar3SacrificioGrupal(p, altarLoc); // Sacrificio de items valiosos
                    break;
                case 4:
                    procesarAltar4CazaGrupal(p, altarLoc); // Caza de mobs
                    break;
                case 5:
                    procesarAltar5UnionGrupal(p, altarLoc);
                    break;
            }
        }
    }
    
    /**
     * ALTAR 1 GRUPAL: EL DESPERTAR - Todos deben permanecer quietos 10 segundos
     */
    private void procesarAltar1DespertarGrupal(Player player, Location altarLoc) {
        UUID uuid = player.getUniqueId();
        
        // Verificar movimiento
        Location posInicio = posicionInicioAltarJugador.get(uuid);
        if (posInicio == null) return;
        
        if (player.getLocation().distance(posInicio) > 0.3) {
            // Se movió - reiniciar TODOS
            broadcastNarrative("§c§l✗ " + player.getName() + " §7se movió - §creiniciando ritual...");
            broadcastNarrative("§8(Todos deben permanecer inmóviles)");
            soundUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 0.5f, 0.8f);
            
            // Reiniciar contadores
            for (UUID uid : jugadoresPresentesEnAltar) {
                tiempoInicioAltarJugador.put(uid, System.currentTimeMillis());
                posicionInicioAltarJugador.put(uid, Bukkit.getPlayer(uid).getLocation());
            }
            return;
        }
        
        // Calcular progreso del grupo
        long tiempoMinimo = Long.MAX_VALUE;
        for (UUID uid : jugadoresPresentesEnAltar) {
            Long tiempo = tiempoInicioAltarJugador.get(uid);
            if (tiempo != null && tiempo < tiempoMinimo) {
                tiempoMinimo = tiempo;
            }
        }
        
        long tiempoTranscurrido = System.currentTimeMillis() - tiempoMinimo;
        int segundos = (int)(tiempoTranscurrido / 1000);
        
        // Feedback visual cada segundo
        if (tiempoTranscurrido % 1000 < 100) {
            for (UUID uid : jugadoresPresentesEnAltar) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null) {
                    // ActionBar con barra de progreso visual
                    String barra = crearBarraProgreso(segundos, 10, "§5", "§7");
                    p.sendActionBar("§5⧖ Despertar: " + barra + " §f" + segundos + "/10s");
                }
            }
            
            // Sonido cada 3 segundos
            if (segundos > 0 && segundos % 3 == 0 && tiempoTranscurrido % 1000 < 100) {
                for (UUID uid : jugadoresPresentesEnAltar) {
                    Player p = Bukkit.getPlayer(uid);
                    if (p != null) {
                        soundUtil.playSound(p, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.0f + (segundos * 0.05f));
                    }
                }
            }
            
            // Partículas END_ROD girando para todos
            Location centro = altarLoc.clone().add(0.5, 1, 0.5);
            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(i * 45 + (ticksEnActo * 3));
                centro.getWorld().spawnParticle(
                    Particle.END_ROD,
                    centro.clone().add(Math.cos(angle) * 2, 0, Math.sin(angle) * 2),
                    1, 0, 0, 0, 0
                );
            }
        }
        
        // Completar después de 10 segundos
        if (tiempoTranscurrido >= 10000) {
            completarAltarGrupal(1, altarLoc);
        }
    }
    
    /**
     * ALTAR 2 GRUPAL: LA RESONANCIA - Detectar y consumir Ender Pearls en área del altar
     */
    private void procesarAltar2ResonanciaGrupal(Player player, Location altarLoc) {
        // === SISTEMA DE DETECCIÓN DE ITEMS EN ÁREA DEL ALTAR ===
        // Radio de detección: 5 bloques alrededor del altar
        double radioDeteccion = 5.0;
        
        // Buscar items (Ender Pearls) en el área del altar
        for (Entity entity : altarLoc.getWorld().getNearbyEntities(altarLoc, radioDeteccion, radioDeteccion, radioDeteccion)) {
            if (!(entity instanceof Item)) continue;
            
            Item itemEntity = (Item) entity;
            ItemStack itemStack = itemEntity.getItemStack();
            
            // Solo procesar Ender Pearls
            if (itemStack.getType() != Material.ENDER_PEARL) continue;
            
            // Evitar procesar el mismo item dos veces
            if (itemsProcesadosEnAltar.contains(entity.getUniqueId())) continue;
            
            // Marcar como procesado
            itemsProcesadosEnAltar.add(entity.getUniqueId());
            
            // Cantidad de perlas en el stack
            int cantidad = itemStack.getAmount();
            
            // === ANIMACIÓN DE CONSUMO ===
            Location itemLoc = itemEntity.getLocation();
            World world = itemLoc.getWorld();
            
            // 1. Partículas de absorción hacia el altar
            Location centroAltar = altarLoc.clone().add(0.5, 1.5, 0.5);
            Vector direccion = centroAltar.toVector().subtract(itemLoc.toVector()).normalize();
            
            // Partículas de estela hacia el altar
            for (double d = 0; d < itemLoc.distance(centroAltar); d += 0.3) {
                Location particleLoc = itemLoc.clone().add(direccion.clone().multiply(d));
                world.spawnParticle(Particle.PORTAL, particleLoc, 3, 0.1, 0.1, 0.1, 0.02);
                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0.05, 0.05, 0.05, 0);
            }
            
            // 2. Explosión de partículas en el altar
            world.spawnParticle(Particle.REVERSE_PORTAL, centroAltar, 50, 0.5, 0.5, 0.5, 0.1);
            world.spawnParticle(Particle.ENCHANT, centroAltar, 30, 0.8, 0.8, 0.8, 0.5);
            
            // 3. Sonidos mágicos
            world.playSound(itemLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.3f);
            world.playSound(centroAltar, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 1.2f);
            world.playSound(centroAltar, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.5f);
            
            // 4. Eliminar el item (¡consumido por el altar!)
            itemEntity.remove();
            
            // 5. Incrementar contador de perlas
            perlasEntregadasAltar2 += cantidad;
            
            // 6. Feedback a todos los jugadores en el altar
            for (UUID uid : jugadoresPresentesEnAltar) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null) {
                    p.sendMessage("§5⧖ §dEnder Pearl absorbida por el altar! §f(" + perlasEntregadasAltar2 + "/8)");
                    
                    // Efecto de título sutil
                    if (perlasEntregadasAltar2 <= 8) {
                        p.sendTitle("", "§d✦ §f" + perlasEntregadasAltar2 + "/8 §d✦", 5, 20, 10);
                    }
                }
            }
            
            // Log
            plugin.getLogger().info("[SusurroPiedraRota] Ender Pearl consumida en Altar 2. Total: " + perlasEntregadasAltar2 + "/8");
        }
        
        // Mostrar progreso en ActionBar
        if (ticksEnActo % 20 == 0) {
            String barra = crearBarraProgreso(perlasEntregadasAltar2, 8, "§d", "§7");
            for (UUID uid : jugadoresPresentesEnAltar) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null) {
                    p.sendActionBar("§5⧖ Resonancia: " + barra + " §f" + perlasEntregadasAltar2 + "/8 perlas");
                }
            }
            
            // Partículas de portal intensificándose con el progreso
            if (perlasEntregadasAltar2 > 0) {
                Location centro = altarLoc.clone().add(0.5, 1, 0.5);
                int intensidad = 5 + (perlasEntregadasAltar2 * 3);
                centro.getWorld().spawnParticle(Particle.PORTAL, centro, intensidad, 0.5, 0.5, 0.5, 0.1);
                
                // Órbitas de partículas moradas
                double angle = Math.toRadians(ticksEnActo * 5);
                for (int i = 0; i < perlasEntregadasAltar2; i++) {
                    double offsetAngle = angle + (Math.PI * 2 * i / 8);
                    double x = Math.cos(offsetAngle) * 1.5;
                    double z = Math.sin(offsetAngle) * 1.5;
                    centro.getWorld().spawnParticle(Particle.END_ROD, centro.clone().add(x, 0.5, z), 1, 0, 0, 0, 0);
                }
            }
        }
        
        // Completar altar cuando se alcancen 8 perlas
        if (perlasEntregadasAltar2 >= 8) {
            completarAltarGrupal(2, altarLoc);
        }
    }
    
    /**
     * ALTAR 3 GRUPAL: EL SACRIFICIO - Tirar items valiosos al altar
     * Items aceptados: Diamantes, Oro, Esmeraldas, Netherite
     */
    private void procesarAltar3SacrificioGrupal(Player player, Location altarLoc) {
        // Guardar ubicación del altar
        altarActualLocation = altarLoc;
        
        // Si ya se completó, no seguir procesando
        if (itemsSacrificadosAltar3 >= 10) {
            return;
        }
        
        // === SISTEMA DE DETECCIÓN DE ITEMS VALIOSOS EN ÁREA DEL ALTAR ===
        double radioDeteccion = 5.0;
        
        // Buscar items valiosos en el área del altar
        for (Entity entity : altarLoc.getWorld().getNearbyEntities(altarLoc, radioDeteccion, radioDeteccion, radioDeteccion)) {
            if (!(entity instanceof Item)) continue;
            
            Item itemEntity = (Item) entity;
            ItemStack itemStack = itemEntity.getItemStack();
            
            // Evitar procesar el mismo item dos veces
            if (itemsProcesadosSacrificio.contains(entity.getUniqueId())) continue;
            
            // Determinar valor del item
            int valorItem = obtenerValorSacrificio(itemStack.getType());
            if (valorItem <= 0) continue; // No es un item de sacrificio válido
            
            // Marcar como procesado
            itemsProcesadosSacrificio.add(entity.getUniqueId());
            
            // Cantidad total de valor
            int valorTotal = valorItem * itemStack.getAmount();
            
            // === ANIMACIÓN DE SACRIFICIO ===
            Location itemLoc = itemEntity.getLocation();
            World world = itemLoc.getWorld();
            
            // 1. Partículas de absorción hacia el altar
            Location centroAltar = altarLoc.clone().add(0.5, 1.5, 0.5);
            Vector direccion = centroAltar.toVector().subtract(itemLoc.toVector()).normalize();
            
            // Partículas de estela hacia el altar (rojo/naranja para sacrificio)
            for (double d = 0; d < itemLoc.distance(centroAltar); d += 0.3) {
                Location particleLoc = itemLoc.clone().add(direccion.clone().multiply(d));
                world.spawnParticle(Particle.FLAME, particleLoc, 2, 0.1, 0.1, 0.1, 0.01);
                world.spawnParticle(Particle.DUST, particleLoc, 3, 0.1, 0.1, 0.1, 
                    new Particle.DustOptions(org.bukkit.Color.ORANGE, 1.0f));
            }
            
            // 2. Explosión de partículas en el altar
            world.spawnParticle(Particle.LAVA, centroAltar, 20, 0.3, 0.3, 0.3, 0);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, centroAltar, 30, 0.5, 0.5, 0.5, 0.05);
            
            // 3. Sonidos místicos de sacrificio
            world.playSound(itemLoc, Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 0.5f);
            world.playSound(centroAltar, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 0.8f);
            world.playSound(centroAltar, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.7f, 1.2f);
            
            // 4. Eliminar el item (¡consumido por el altar!)
            itemEntity.remove();
            
            // 5. Incrementar contador
            itemsSacrificadosAltar3 += valorTotal;
            if (itemsSacrificadosAltar3 > 10) itemsSacrificadosAltar3 = 10; // Cap
            
            // 6. Feedback a todos los jugadores en el altar
            String nombreItem = obtenerNombreSacrificio(itemStack.getType());
            for (UUID uid : jugadoresPresentesEnAltar) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null) {
                    p.sendMessage("§5⧖ §6" + nombreItem + " §7sacrificado al altar! §f(" + itemsSacrificadosAltar3 + "/10)");
                    
                    // Efecto de título sutil
                    if (itemsSacrificadosAltar3 <= 10) {
                        p.sendTitle("", "§6✦ §f" + itemsSacrificadosAltar3 + "/10 §6✦", 5, 20, 10);
                    }
                }
            }
            
            // Log
            plugin.getLogger().info("[SusurroPiedraRota] Sacrificio: " + itemStack.getType().name() + " x" + itemStack.getAmount() + " = +" + valorTotal + " (Total: " + itemsSacrificadosAltar3 + "/10)");
        }
        
        // Mostrar progreso en ActionBar
        if (ticksEnActo % 20 == 0) {
            String barra = crearBarraProgreso(itemsSacrificadosAltar3, 10, "§6", "§7");
            for (UUID uid : jugadoresPresentesEnAltar) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null) {
                    p.sendActionBar("§5⧖ Sacrificio: " + barra + " §6" + itemsSacrificadosAltar3 + "/10 ofrendas");
                }
            }
            
            // Partículas de fuego intensificándose con el progreso
            if (itemsSacrificadosAltar3 > 0) {
                Location centro = altarLoc.clone().add(0.5, 1, 0.5);
                int intensidad = 3 + (itemsSacrificadosAltar3 * 2);
                centro.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, centro, intensidad, 0.4, 0.4, 0.4, 0.02);
                
                // Columna de humo
                centro.getWorld().spawnParticle(Particle.SMOKE, centro.clone().add(0, 1, 0), 5, 0.2, 0.5, 0.2, 0.01);
            }
        }
        
        // Completar altar cuando se alcancen 10 puntos de sacrificio
        if (itemsSacrificadosAltar3 >= 10) {
            // Efecto dramático
            for (UUID uid : jugadoresPresentesEnAltar) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null) {
                    soundUtil.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                    p.sendActionBar("§a§l✓ ¡SACRIFICIO ACEPTADO!");
                }
            }
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                completarAltarGrupal(3, altarLoc);
            }, 40L);
        }
    }
    
    /**
     * Obtiene el valor de sacrificio de un item
     */
    private int obtenerValorSacrificio(Material material) {
        switch (material) {
            case DIAMOND:
                return 3; // Diamante = 3 puntos
            case EMERALD:
                return 2; // Esmeralda = 2 puntos
            case GOLD_INGOT:
                return 1; // Oro = 1 punto
            case GOLD_BLOCK:
                return 5; // Bloque de oro = 5 puntos
            case DIAMOND_BLOCK:
                return 10; // Bloque de diamante = 10 puntos (completa instant)
            case EMERALD_BLOCK:
                return 8; // Bloque de esmeralda = 8 puntos
            case NETHERITE_INGOT:
                return 5; // Netherite = 5 puntos
            case NETHERITE_SCRAP:
                return 2; // Scrap = 2 puntos
            case ANCIENT_DEBRIS:
                return 3; // Ancient debris = 3 puntos
            case IRON_INGOT:
                return 1; // Hierro = 1 punto (menos valioso)
            case IRON_BLOCK:
                return 3; // Bloque de hierro = 3 puntos
            case LAPIS_LAZULI:
                return 1; // Lapis = 1 punto
            case LAPIS_BLOCK:
                return 3; // Bloque de lapis = 3 puntos
            default:
                return 0;
        }
    }
    
    /**
     * Obtiene el nombre para mostrar de un item de sacrificio
     */
    private String obtenerNombreSacrificio(Material material) {
        switch (material) {
            case DIAMOND: return "Diamante";
            case DIAMOND_BLOCK: return "Bloque de Diamante";
            case EMERALD: return "Esmeralda";
            case EMERALD_BLOCK: return "Bloque de Esmeralda";
            case GOLD_INGOT: return "Lingote de Oro";
            case GOLD_BLOCK: return "Bloque de Oro";
            case NETHERITE_INGOT: return "Lingote de Netherite";
            case NETHERITE_SCRAP: return "Fragmento de Netherite";
            case ANCIENT_DEBRIS: return "Escombros Ancestrales";
            case IRON_INGOT: return "Lingote de Hierro";
            case IRON_BLOCK: return "Bloque de Hierro";
            case LAPIS_LAZULI: return "Lapislázuli";
            case LAPIS_BLOCK: return "Bloque de Lapislázuli";
            default: return material.name();
        }
    }
    
    /**
     * ALTAR 4 GRUPAL: LA CAZA - Matar mobs hostiles spawneados
     */
    private void procesarAltar4CazaGrupal(Player player, Location altarLoc) {
        // Guardar ubicación del altar para verificación de kills
        altarActualLocation = altarLoc;
        
        // Si ya se completó, no seguir procesando
        if (mobsHostilesEliminadosAltar4 >= 5) {
            return;
        }
        
        // ✨ Spawn progresivo de mobs cada 3 segundos si hay menos de 3 vivos
        if (ticksEnActo % 60 == 0) { // Cada 3 segundos
            // Contar mobs vivos del altar
            int mobsVivos = 0;
            for (UUID mobId : criaturasDeAltar) {
                Entity e = Bukkit.getEntity(mobId);
                if (e != null && e.isValid() && !e.isDead()) {
                    mobsVivos++;
                }
            }
            
            // Spawnear si hay menos de 3 mobs vivos
            if (mobsVivos < 3) {
                spawnearMobAltar4(altarLoc);
            }
        }
        
        // Feedback visual cada segundo
        if (ticksEnActo % 20 == 0) {
            String barra = crearBarraProgreso(mobsHostilesEliminadosAltar4, 5, "§c", "§7");
            for (UUID uid : jugadoresPresentesEnAltar) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null) {
                    p.sendActionBar("§5⧖ Caza: " + barra + " §c" + mobsHostilesEliminadosAltar4 + "/5 ☠");
                }
            }
            
            // Partículas de sangre intensificándose con el progreso
            if (mobsHostilesEliminadosAltar4 > 0) {
                Location centro = altarLoc.clone().add(0.5, 1, 0.5);
                int cantidadParticulas = 3 + (mobsHostilesEliminadosAltar4 * 3);
                centro.getWorld().spawnParticle(Particle.DUST, centro, cantidadParticulas, 
                    0.5, 0.5, 0.5, 
                    new Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
            }
        }
    }
    
    /**
     * Spawnea un mob hostil para el Altar 4 (La Caza)
     */
    private void spawnearMobAltar4(Location altarLoc) {
        // Elegir posición aleatoria alrededor del altar (8-15 bloques)
        double angulo = Math.random() * Math.PI * 2;
        double distancia = 8 + Math.random() * 7;
        Location spawnLoc = altarLoc.clone().add(
            Math.cos(angulo) * distancia,
            0,
            Math.sin(angulo) * distancia
        );
        spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);
        
        // Elegir tipo de mob aleatorio
        EntityType[] tipos = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.HUSK, EntityType.STRAY};
        EntityType tipo = tipos[(int)(Math.random() * tipos.length)];
        
        // Spawnear con efecto visual
        spawnLoc.getWorld().spawnParticle(Particle.SMOKE, spawnLoc, 20, 0.3, 0.5, 0.3, 0.05);
        spawnLoc.getWorld().spawnParticle(Particle.PORTAL, spawnLoc, 30, 0.5, 1, 0.5, 0.5);
        soundUtil.playSound(spawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.6f);
        
        org.bukkit.entity.LivingEntity mob = (org.bukkit.entity.LivingEntity) 
            spawnLoc.getWorld().spawnEntity(spawnLoc, tipo);
        
        // Nombre personalizado
        String[] nombres = {"§cRecuerdo Sangriento", "§cEco de la Caza", "§cVíctima Olvidada", "§cPresa del Vacío"};
        mob.customName(net.kyori.adventure.text.Component.text(nombres[(int)(Math.random() * nombres.length)]));
        mob.setCustomNameVisible(true);
        
        // Stats ligeramente aumentados
        mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(25.0);
        mob.setHealth(25.0);
        
        // ✨ XP DINÁMICO GENEROSO - Las criaturas del altar dan mucha XP
        // Base: 15-25 XP por mob (los mobs normales dan 5)
        int xpBase = 15 + (int)(Math.random() * 11); // 15-25 XP
        if (mob instanceof org.bukkit.entity.Zombie) {
            ((org.bukkit.entity.Zombie) mob).setShouldBurnInDay(false);
        }
        // Guardamos el XP en metadata para darlo al morir
        mob.setMetadata("apocalipsis_xp", new org.bukkit.metadata.FixedMetadataValue(plugin, xpBase));
        mob.setMetadata("apocalipsis_altar", new org.bukkit.metadata.FixedMetadataValue(plugin, 4));
        
        // Registrar como criatura del altar
        criaturasDeAltar.add(mob.getUniqueId());
        
        // Anunciar spawn
        for (UUID uid : jugadoresPresentesEnAltar) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) {
                p.sendMessage("§c⚔ §7Un recuerdo hostil emerge de las sombras...");
            }
        }
        
        plugin.getLogger().info("[SusurroPiedraRota] Altar 4: Spawneado " + tipo.name());
    }
    
    /**
     * Procesa la muerte de un mob hostil para el Altar 4 (La Caza)
     * Llamado desde el listener cuando un jugador mata un mob
     */
    public void procesarKillMobHostilAltar4(Player killer, LivingEntity mob) {
        // Solo procesar si estamos en Acto 1 (Piedra Despierta) y Altar 4 está activo
        if (actoActual != Acto.PIEDRA_DESPIERTA || altarActualGlobal != 4 || !altarEnProgreso) {
            plugin.getLogger().info("[SusurroPiedraRota] Kill ignorado - altar=" + altarActualGlobal + ", enProgreso=" + altarEnProgreso);
            return;
        }
        
        // Verificar que el altar tenga ubicación
        if (altarActualLocation == null) {
            plugin.getLogger().info("[SusurroPiedraRota] Kill ignorado - altarLocation es null");
            return;
        }
        
        // Verificar que el mob esté dentro del rango (50 bloques del altar)
        double distancia = mob.getLocation().distance(altarActualLocation);
        if (distancia > 50) {
            plugin.getLogger().info("[SusurroPiedraRota] Kill ignorado - mob muy lejos: " + distancia + " bloques");
            return;
        }
        
        // ✨ Aceptar tanto mobs spawneados del altar como mobs naturales
        boolean esMobDelAltar = criaturasDeAltar.contains(mob.getUniqueId());
        boolean esMobNatural = esMobHostilNatural(mob);
        
        if (!esMobDelAltar && !esMobNatural) {
            plugin.getLogger().info("[SusurroPiedraRota] Kill ignorado - no es mob válido: " + mob.getType().name());
            return;
        }
        
        // Si es mob del altar, removerlo de la lista
        if (esMobDelAltar) {
            criaturasDeAltar.remove(mob.getUniqueId());
        }
        
        // ¡Contar el kill!
        mobsHostilesEliminadosAltar4++;
        
        // Feedback inmediato al killer
        killer.sendMessage("§5§l[⧖] §7Mob eliminado: §c" + mobsHostilesEliminadosAltar4 + "/5");
        soundUtil.playSound(killer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
        
        // ✨ Actualizar ActionBar inmediatamente a TODOS los jugadores presentes
        String barra = crearBarraProgreso(mobsHostilesEliminadosAltar4, 5, "§c", "§7");
        for (UUID uid : jugadoresPresentesEnAltar) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) {
                p.sendActionBar("§5⧖ Caza: " + barra + " §c" + mobsHostilesEliminadosAltar4 + "/5 ☠");
            }
        }
        
        // Partículas en el mob muerto
        mob.getWorld().spawnParticle(Particle.SOUL, mob.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.05);
        
        // ✨ DAR XP GENEROSO AL JUGADOR
        org.bukkit.NamespacedKey xpKey = new org.bukkit.NamespacedKey(plugin, "evento_xp");
        final int xpGanado;
        
        if (mob.getPersistentDataContainer().has(xpKey, org.bukkit.persistence.PersistentDataType.INTEGER)) {
            // XP configurado para mobs del altar (50-70 XP)
            xpGanado = mob.getPersistentDataContainer().get(xpKey, org.bukkit.persistence.PersistentDataType.INTEGER);
        } else {
            // XP para mobs naturales (menos que los del altar pero aún generoso)
            xpGanado = 15 + (int)(Math.random() * 10); // 15-24 XP para naturales
        }
        
        // Dar experiencia real y orbes visuales
        killer.giveExp(xpGanado);
        mob.getWorld().spawn(mob.getLocation().add(0, 0.5, 0), org.bukkit.entity.ExperienceOrb.class, orb -> {
            orb.setExperience(xpGanado / 4); // Orbes visuales extra (25% bonus visual)
        });
        
        killer.sendMessage("§a§l✦ +" + xpGanado + " XP §a§oBonus del evento!");
        
        // Partículas de XP épicas
        mob.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, mob.getLocation().add(0, 1.2, 0), 20, 0.5, 0.7, 0.5, 0.1);
        mob.getWorld().spawnParticle(Particle.ENCHANT, mob.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.5);
        
        plugin.getLogger().info(String.format(
            "[SusurroPiedraRota] Altar 4: %s eliminó %s (%d/5) +%dXP - delAltar=%s, natural=%s",
            killer.getName(), mob.getType().name(), mobsHostilesEliminadosAltar4, xpGanado, esMobDelAltar, esMobNatural
        ));
        
        // Verificar completado inmediatamente después del kill
        if (mobsHostilesEliminadosAltar4 >= 5) {
            plugin.getLogger().info("[SusurroPiedraRota] Altar 4: ¡Objetivo completado! Iniciando secuencia de completar...");
            
            // Efecto dramático para todos
            for (UUID uid : jugadoresPresentesEnAltar) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null) {
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.STRENGTH, 200, 0));
                    soundUtil.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                    p.sendActionBar("§a§l✓ ¡CAZA COMPLETADA! §7Preparando siguiente fase...");
                }
            }
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                completarAltarGrupal(4, altarActualLocation);
            }, 40L);
        }
    }
    
    /**
     * Verifica si un mob es un mob hostil natural (no del evento)
     */
    private boolean esMobHostilNatural(LivingEntity mob) {
        switch (mob.getType()) {
            case ZOMBIE:
            case SKELETON:
            case CREEPER:
            case SPIDER:
            case CAVE_SPIDER:
            case ENDERMAN:
            case WITCH:
            case PILLAGER:
            case VINDICATOR:
            case RAVAGER:
            case DROWNED:
            case HUSK:
            case STRAY:
            case PHANTOM:
            case BLAZE:
            case GHAST:
            case MAGMA_CUBE:
            case SLIME:
            case SILVERFISH:
            case ENDERMITE:
            case WARDEN:
            case WITHER_SKELETON:
            case PIGLIN:
            case PIGLIN_BRUTE:
            case HOGLIN:
            case ZOGLIN:
            case VEX:
            case EVOKER:
            case ILLUSIONER:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Procesar kill de mobs en Altar 5 - Solo da XP generoso, no cuenta para progreso
     */
    public void procesarKillMobAltar5(Player killer, LivingEntity mob) {
        // Solo procesar si estamos en Acto 1 (Piedra Despierta) y Altar 5 está activo
        if (actoActual != Acto.PIEDRA_DESPIERTA || altarActualGlobal != 5 || !altarEnProgreso) {
            return;
        }
        
        // Verificar que el altar tenga ubicación
        if (altarActualLocation == null) return;
        
        // Verificar que el mob esté dentro del rango (60 bloques del altar)
        double distancia = mob.getLocation().distance(altarActualLocation);
        if (distancia > 60) return;
        
        // ✨ Verificar si es un mob del evento (tiene XP configurado)
        org.bukkit.NamespacedKey xpKey = new org.bukkit.NamespacedKey(plugin, "evento_xp");
        final int xpGanado;
        
        if (mob.getPersistentDataContainer().has(xpKey, org.bukkit.persistence.PersistentDataType.INTEGER)) {
            // XP configurado para mobs de oleadas (60-100+ XP dependiendo de oleada)
            xpGanado = mob.getPersistentDataContainer().get(xpKey, org.bukkit.persistence.PersistentDataType.INTEGER);
        } else {
            // XP para mobs naturales durante altar 5
            xpGanado = 20 + (int)(Math.random() * 15); // 20-34 XP para naturales
        }
        
        // Dar experiencia real y orbes visuales épicos
        killer.giveExp(xpGanado);
        
        // Múltiples orbes de XP visual para efecto dramático
        final Location mobLoc = mob.getLocation();
        final World mobWorld = mob.getWorld();
        for (int i = 0; i < 3; i++) {
            final int delay = i * 2;
            final int xpOrb = xpGanado / 6;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                mobWorld.spawn(mobLoc.clone().add(
                    Math.random() * 0.5 - 0.25, 
                    0.5 + Math.random() * 0.3, 
                    Math.random() * 0.5 - 0.25), 
                    org.bukkit.entity.ExperienceOrb.class, orb -> {
                        orb.setExperience(xpOrb);
                    });
            }, delay);
        }
        
        killer.sendMessage("§6§l⚔ +" + xpGanado + " XP §e§oVictoria en la Unión!");
        
        // Partículas épicas de XP
        mob.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, mob.getLocation().add(0, 1.2, 0), 25, 0.6, 0.8, 0.6, 0.1);
        mob.getWorld().spawnParticle(Particle.ENCHANT, mob.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.7);
        mob.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, mob.getLocation().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.02);
        
        // Sonido satisfactorio
        soundUtil.playSound(killer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.3f);
        
        plugin.getLogger().info(String.format(
            "[SusurroPiedraRota] Altar 5: %s eliminó %s +%dXP",
            killer.getName(), mob.getType().name(), xpGanado
        ));
    }
    
    /**
     * ALTAR 5 GRUPAL: LA UNIÓN - Resistir oleadas
     */
    private void procesarAltar5UnionGrupal(Player player, Location altarLoc) {
        // Inicializar tracking
        if (!tiempoInicioAltarJugador.containsKey(player.getUniqueId())) {
            tiempoInicioAltarJugador.put(player.getUniqueId(), System.currentTimeMillis());
        }
        
        long tiempoTranscurrido = System.currentTimeMillis() - tiempoInicioAltarJugador.values().stream().min(Long::compare).orElse(0L);
        int segundosTranscurridos = (int)(tiempoTranscurrido / 1000);
        
        // Spawnar oleadas cada 10 segundos durante 30 segundos
        if (segundosTranscurridos == 10 || segundosTranscurridos == 20) {
            if (tiempoTranscurrido % 1000 < 100) { // Solo spawnear una vez por segundo
                spawnearOleadaAltar5(altarLoc, segundosTranscurridos / 10);
            }
        }
        
        // Mostrar progreso
        if (ticksEnActo % 20 == 0) {
            int segundosRestantes = Math.max(0, 30 - segundosTranscurridos);
            String barra = crearBarraProgreso(segundosTranscurridos, 30, "§c", "§7");
            for (UUID uid : jugadoresPresentesEnAltar) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null) {
                    p.sendActionBar("§5⧖ Unión: " + barra + " §c" + segundosRestantes + "s restantes");
                }
            }
            
            // Partículas intensificándose con el tiempo
            Location centro = altarLoc.clone().add(0.5, 1, 0.5);
            int intensidad = 5 + (segundosTranscurridos / 3);
            centro.getWorld().spawnParticle(Particle.WITCH, centro, intensidad, 1.5, 1, 1.5, 0.05);
        }
        
        // Completar después de 30 segundos
        if (tiempoTranscurrido >= 30000) {
            completarAltarFinalGrupal(altarLoc);
        }
    }
    
    /**
     * Spawnear oleada de criaturas para Altar 5
     */
    private void spawnearOleadaAltar5(Location altarLoc, int oleada) {
        broadcastNarrative("§c§l⚠ OLEADA " + oleada + " - ¡Recuerdos hostiles emergen!");
        playSoundToAll(Sound.ENTITY_WARDEN_ROAR, 0.6f, 0.8f);
        
        int cantidad = oleada * 4; // Oleada 1: 4 criaturas, Oleada 2: 8 criaturas
        String[] nombres = {
            "§4Eco del Apocalipsis",
            "§4Recuerdo Furioso",
            "§4Sombra Vengativa",
            "§4Fragmento Iracundo"
        };
        
        for (int i = 0; i < cantidad; i++) {
            double angle = Math.toRadians(i * (360.0 / cantidad));
            double radio = 8 + (oleada * 2); // Spawnean más lejos en oleadas posteriores
            Location spawnLoc = altarLoc.clone().add(
                Math.cos(angle) * radio,
                0,
                Math.sin(angle) * radio
            );
            spawnLoc.setY(altarLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);
            
            // Tipos más peligrosos
            EntityType tipo;
            if (oleada == 2) {
                // Segunda oleada más difícil
                tipo = (i % 3 == 0) ? EntityType.WITHER_SKELETON : 
                       (i % 3 == 1) ? EntityType.STRAY : EntityType.HUSK;
            } else {
                tipo = (i % 2 == 0) ? EntityType.ZOMBIE : EntityType.SKELETON;
            }
            
            org.bukkit.entity.LivingEntity criatura = (org.bukkit.entity.LivingEntity) 
                spawnLoc.getWorld().spawnEntity(spawnLoc, tipo);
            
            criatura.customName(net.kyori.adventure.text.Component.text(nombres[i % nombres.length]));
            criatura.setCustomNameVisible(true);
            
            // Stats incrementados según oleada
            double healthMultiplier = 1.5 + (oleada * 0.5);
            criatura.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(20.0 * healthMultiplier);
            criatura.setHealth(20.0 * healthMultiplier);
            criatura.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).setBaseValue(4.0 + oleada);
            criatura.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(0.25 + (oleada * 0.05));
            
            // ✨ XP DINÁMICO MUY GENEROSO - Altar 5 da aún más XP
            // Oleada 1: 20-30 XP | Oleada 2: 30-45 XP (son más difíciles)
            int xpBase = (oleada == 1) ? (20 + (int)(Math.random() * 11)) : (30 + (int)(Math.random() * 16));
            if (criatura instanceof org.bukkit.entity.Zombie) {
                ((org.bukkit.entity.Zombie) criatura).setShouldBurnInDay(false);
            }
            // Guardamos el XP en metadata para darlo al morir
            criatura.setMetadata("apocalipsis_xp", new org.bukkit.metadata.FixedMetadataValue(plugin, xpBase));
            criatura.setMetadata("apocalipsis_altar", new org.bukkit.metadata.FixedMetadataValue(plugin, 5));
            
            criaturasDeAltar.add(criatura.getUniqueId());
            
            // Efectos visuales dramáticos
            spawnLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, spawnLoc, 40, 0.5, 1, 0.5, 0.15);
            spawnLoc.getWorld().spawnParticle(Particle.SMOKE, spawnLoc, 20, 0.5, 0.5, 0.5, 0.1);
            spawnLoc.getWorld().spawnParticle(Particle.LAVA, spawnLoc, 5, 0.3, 0.3, 0.3, 0);
        }
    }
    
    /**
     * Completar un altar grupal y mostrar narrativa
     */
    private void completarAltarGrupal(int numAltar, Location altarLoc) {
        fragmentosInspeccionados.add(altarLoc);
        
        // Efectos visuales épicos
        Location centro = altarLoc.clone().add(0.5, 1, 0.5);
        
        // Explosión de partículas
        centro.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, centro, 50, 1, 1, 1, 0.3);
        centro.getWorld().spawnParticle(Particle.END_ROD, centro, 30, 0.5, 1, 0.5, 0.2);
        centro.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, centro, 20, 0.5, 0.5, 0.5, 0.1);
        
        // Anillos de partículas
        for (int ring = 0; ring < 3; ring++) {
            final int r = ring;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (int angle = 0; angle < 360; angle += 10) {
                    double rad = Math.toRadians(angle);
                    double radio = 2 + r * 1.5;
                    Location particleLoc = centro.clone().add(
                        Math.cos(rad) * radio, 0.2, Math.sin(rad) * radio
                    );
                    centro.getWorld().spawnParticle(Particle.GLOW, particleLoc, 1, 0, 0, 0, 0);
                }
            }, ring * 5L);
        }
        
        playSoundToAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        playSoundToAll(Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.5f);
        playSoundToAll(Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.8f);
        
        // Mensaje de completado
        String nombreAltar = obtenerNombreAltar(numAltar);
        broadcastNarrative("");
        broadcastNarrative("§5§l✓ " + nombreAltar + " COMPLETADO");
        broadcastNarrative("");
        
        // NARRATIVA DEL OBSERVADOR explicando QUÉ pasó y POR QUÉ
        switch (numAltar) {
            case 1:
                broadcastNarrative("    §8El Observador§7: §o\"...¿lo escuchan? El fragmento despierta...\"");
                broadcastNarrative("    §8El Observador§7: §o\"...su quietud permitió que la memoria resonara...\"");
                broadcastNarrative("    §8El Observador§7: §o\"...el primer eco ha sido restaurado...\"");
                broadcastNarrative("");
                broadcastNarrative("    §a✓ §7La calma sincronizó con el fragmento dormido.");
                broadcastNarrative("    §a✓ §7Una fracción del mundo roto se reconectó.");
                break;
            case 2:
                broadcastNarrative("    §8El Observador§7: §o\"...objetos que viajaron por el vacío...\"");
                broadcastNarrative("    §8El Observador§7: §o\"...ocho perlas... suficiente energía dimensional...\"");
                broadcastNarrative("    §8El Observador§7: §o\"...¡la resonancia alcanzó su punto máximo!\"");
                broadcastNarrative("");
                broadcastNarrative("    §a✓ §7Las Ender Pearls vienen del End, dimensión conectada al vacío.");
                broadcastNarrative("    §a✓ §7Su energía combinada desestabilizó la realidad, reparando la memoria.");
                break;
            case 3:
                broadcastNarrative("    §8El Observador§7: §o\"...la esencia vital fluye hacia la piedra...\"");
                broadcastNarrative("    §8El Observador§7: §o\"...el fragmento absorbe su sacrificio...\"");
                broadcastNarrative("    §8El Observador§7: §o\"...las ofrendas fortalecen la conexión...\"");
                broadcastNarrative("");
                broadcastNarrative("    §a✓ §7Los fragmentos están hechos de memorias vivientes del mundo.");
                broadcastNarrative("    §a✓ §7Necesitaban esa energía para recobrar forma tangible.");
                break;
            case 4:
                broadcastNarrative("    §8El Observador§7: §o\"...la plaga se desvanece por completo...\"");
                broadcastNarrative("    §8El Observador§7: §o\"...los ecos erróneos fueron eliminados...\"");
                broadcastNarrative("    §8El Observador§7: §o\"...la corrupción se detuvo... por ahora...\"");
                broadcastNarrative("");
                broadcastNarrative("    §a✓ §7El mundo roto creó copias que no deberían existir.");
                broadcastNarrative("    §a✓ §7Ahora el fragmento puede recordar sin interferencias.");
                break;
        }
        
        broadcastNarrative("");
        
        // Dar recompensas a todos
        org.bukkit.potion.PotionEffect[] efectos = obtenerEfectosAltar(numAltar);
        for (UUID uid : jugadoresPresentesEnAltar) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && p.getGameMode() == org.bukkit.GameMode.SURVIVAL) {
                for (org.bukkit.potion.PotionEffect efecto : efectos) {
                    p.addPotionEffect(efecto);
                }
                
                // Tracking
                participacionFragmentos.put(uid, participacionFragmentos.getOrDefault(uid, 0) + 1);
            }
        }
        
        // Registrar completado
        for (UUID uid : jugadoresPresentesEnAltar) {
            altaresCompletadosPorJugador.putIfAbsent(uid, new HashSet<>());
            altaresCompletadosPorJugador.get(uid).add(numAltar);
        }
        
        // Actualizar BossBar
        if (bossBarProgreso != null) {
            double progreso = (double) fragmentosInspeccionados.size() / fragmentosLocations.size();
            bossBarProgreso.setProgress(Math.min(progreso, 1.0));
            // Mostrar altar completado y cuál es el siguiente
            int siguiente = (numAltar < 5) ? numAltar + 1 : 5;
            bossBarProgreso.setTitle("§a✓ Altar " + numAltar + " completado §7| §eProximo: " + siguiente);
        }
        
        plugin.getLogger().info("[SusurroPiedraRota] Altar " + numAltar + " COMPLETADO. Altares totales: " + fragmentosInspeccionados.size() + "/5");
        
        // Reset y avanzar al siguiente altar
        altarEnProgreso = false;
        altarActualGlobal++;
        esperandoJugadores = false; // ✨ NUEVO: Reset del flag de espera
        jugadoresPresentesEnAltar.clear();
        tiempoInicioAltarJugador.clear();
        posicionInicioAltarJugador.clear();
        vidaInicioAltarJugador.clear();
        criaturasEliminadasPorJugador.clear();
        altarActualJugador.clear(); // ✨ NUEVO: Limpiar para que se muestren mensajes del siguiente altar
        
        // Actualizar objetivos de todos los jugadores al siguiente altar
        Location siguienteAltarLoc = obtenerLocationAltarActual();
        if (siguienteAltarLoc != null) {
            for (UUID uid : participantesOriginales) {
                objetivosPorJugador.put(uid, siguienteAltarLoc);
            }
            plugin.getLogger().info("[ActionBar] Actualizado objetivo a altar " + altarActualGlobal + " para todos los jugadores");
        }
        
        // Mensaje de transición
        if (altarActualGlobal <= 5) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                broadcastNarrative("§5⧖ Buscad el siguiente fragmento: " + obtenerNombreAltar(altarActualGlobal));
                playSoundToAll(Sound.BLOCK_BEACON_AMBIENT, 0.7f, 1.3f);
            }, 40L);
        }
    }
    
    /**
     * Obtener efectos de poción según altar
     */
    private org.bukkit.potion.PotionEffect[] obtenerEfectosAltar(int numAltar) {
        switch (numAltar) {
            case 1:
                return new org.bukkit.potion.PotionEffect[] {
                    new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION, 600, 0)
                };
            case 2:
                return new org.bukkit.potion.PotionEffect[] {
                    new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 900, 1)
                };
            case 3:
                return new org.bukkit.potion.PotionEffect[] {
                    new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 600, 2),
                    new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION, 1200, 1)
                };
            case 4:
                return new org.bukkit.potion.PotionEffect[] {
                    new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, 1200, 1),
                    new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 1200, 0)
                };
            default:
                return new org.bukkit.potion.PotionEffect[0];
        }
    }
    
    /**
     * Completar altar final grupal y transicionar a Acto 2
     */
    private void completarAltarFinalGrupal(Location altarLoc) {
        fragmentosInspeccionados.add(altarLoc);
        
        // Secuencia cinemática épica
        Location centro = altarLoc.clone().add(0.5, 1, 0.5);
        
        broadcastNarrative("");
        broadcastNarrative("§5§l⧖ ALTAR DE LA UNIÓN");
        broadcastNarrative("");
        broadcastNarrative("    §8El Observador§7: §o\"...los fragmentos resuenan juntos...\"");
        broadcastNarrative("    §8El Observador§7: §o\"...pero la memoria sigue incompleta...\"");
        broadcastNarrative("    §8El Observador§7: §o\"...algo más grande... más oscuro...\"");
        broadcastNarrative("    §8El Observador§7: §o\"...despierta en el vacío...\"");
        broadcastNarrative("");
        
        playSoundToAll(Sound.ENTITY_WARDEN_AMBIENT, 0.8f, 0.4f);
        
        // Efectos visuales masivos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Explosión de luz
            centro.getWorld().spawnParticle(Particle.FLASH, centro, 5, 0, 0, 0, 0);
            centro.getWorld().spawnParticle(Particle.ASH, centro, 100, 2, 2, 2, 0.2);
            centro.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, centro, 50, 1, 1, 1, 0.3);
            
            soundUtil.playSound(centro, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
            soundUtil.playSound(centro, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.6f);
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle("§c§l⚠", "§4¡UN MAL RECUERDO DESPIERTA!", 10, 60, 20);
            }
        }, 40L);
        
        // NARRATIVA FINAL explicando la transición
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            broadcastNarrative("");
            broadcastNarrative("    §8El Observador§7: §o\"...los cinco fragmentos están unidos...\"");
            broadcastNarrative("    §8El Observador§7: §o\"...pero no forman la memoria completa...\"");
            broadcastNarrative("    §8El Observador§7: §o\"...abrieron una grieta hacia algo peor...\"");
            broadcastNarrative("    §8El Observador§7: §o\"...un eco del apocalipsis olvidado...\"");
            broadcastNarrative("");
            broadcastNarrative("    §7Los fragmentos eran llaves. Las juntaron.");
            broadcastNarrative("    §7Ahora deben enfrentar lo que sellaron.");
            broadcastNarrative("");
        }, 80L);
        
        // Dar efecto Glowing permanente a todos
        for (UUID uid : jugadoresPresentesEnAltar) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) {
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.GLOWING, 999999, 0, false, false));
            }
        }
        
        // Completar Acto 1
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!acto1Completado) {
                acto1Completado = true;
                completarActo1();
            }
        }, 120L);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // MÉTODOS LEGACY (COMPATIBILIDAD CON LISTENERS ANTIGUOS)
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * ALTAR 1: EL DESPERTAR - Permanecer quieto 10 segundos (LEGACY)
     */
    private void procesarAltar1Despertar(Player player, Location altarLoc) {
        UUID uuid = player.getUniqueId();
        
        // Inicializar tracking si es primera vez
        if (!tiempoInicioAltarJugador.containsKey(uuid)) {
            tiempoInicioAltarJugador.put(uuid, System.currentTimeMillis());
            posicionInicioAltarJugador.put(uuid, player.getLocation().clone());
            
            // Mensaje de instrucción
            player.sendMessage("");
            player.sendMessage("§5§l⧖ ALTAR DEL DESPERTAR");
            player.sendMessage("§7Permanece §einmóvil §7durante §e10 segundos");
            player.sendMessage("§8\"...algo se mueve en la piedra...\"");
            player.sendMessage("");
            
            soundUtil.playSound(player, Sound.ENTITY_WARDEN_HEARTBEAT, 0.4f, 0.8f);
            return;
        }
        
        // Verificar movimiento
        Location posInicio = posicionInicioAltarJugador.get(uuid);
        if (player.getLocation().distance(posInicio) > 0.3) {
            // Se movió - reiniciar
            tiempoInicioAltarJugador.remove(uuid);
            posicionInicioAltarJugador.remove(uuid);
            player.sendMessage("§c§l✗ §7Te has movido - progreso reiniciado");
            soundUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 0.5f, 0.8f);
            return;
        }
        
        // Calcular progreso
        long tiempoTranscurrido = System.currentTimeMillis() - tiempoInicioAltarJugador.get(uuid);
        int segundos = (int)(tiempoTranscurrido / 1000);
        
        // Feedback visual cada segundo
        if (tiempoTranscurrido % 1000 < 100) {
            player.sendActionBar("§5⧖ §eDesespertar: §f" + segundos + "/10 segundos");
            
            // Partículas END_ROD girando
            Location centro = altarLoc.clone().add(0.5, 1, 0.5);
            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(i * 45 + (ticksEnActo * 3));
                centro.getWorld().spawnParticle(
                    Particle.END_ROD,
                    centro.clone().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5),
                    1, 0, 0, 0, 0
                );
            }
        }
        
        // Completar después de 10 segundos
        if (tiempoTranscurrido >= 10000) {
            completarAltar(player, 1, altarLoc, "§8§l⧖ Fragmento del Despertar", 
                new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION, 600, 0));
        }
    }
    
    /**
     * ALTAR 2: LA RESONANCIA - Dropear 3 Ender Pearls
     * (Se verifica en el listener de EntityDropItemEvent)
     */
    private void procesarAltar2Resonancia(Player player, Location altarLoc) {
        UUID uuid = player.getUniqueId();
        
        // Solo mostrar instrucción una vez
        if (!tiempoInicioAltarJugador.containsKey(uuid)) {
            tiempoInicioAltarJugador.put(uuid, System.currentTimeMillis());
            
            player.sendMessage("");
            player.sendMessage("§5§l⧖ ALTAR DE LA RESONANCIA");
            player.sendMessage("§7Dropea §e3 Ender Pearls §7cerca del altar");
            player.sendMessage("§8\"...objetos del vacío...\"");
            player.sendMessage("");
            
            soundUtil.playSound(player, Sound.ENTITY_ENDERMAN_AMBIENT, 0.5f, 0.6f);
        }
    }
    
    /**
     * ALTAR 3: EL SACRIFICIO - Perder 10 corazones
     */
    private void procesarAltar3Sacrificio(Player player, Location altarLoc) {
        UUID uuid = player.getUniqueId();
        
        // Inicializar tracking
        if (!vidaInicioAltarJugador.containsKey(uuid)) {
            vidaInicioAltarJugador.put(uuid, player.getHealth());
            tiempoInicioAltarJugador.put(uuid, System.currentTimeMillis());
            
            player.sendMessage("");
            player.sendMessage("§5§l⧖ ALTAR DEL SACRIFICIO");
            player.sendMessage("§7Pierde §e10 corazones §7de vida cerca del altar");
            player.sendMessage("§8\"...la esencia vital alimenta el recuerdo...\"");
            player.sendMessage("");
            
            soundUtil.playSound(player, Sound.ENTITY_WARDEN_AMBIENT, 0.5f, 0.5f);
            return;
        }
        
        // Calcular vida perdida
        double vidaInicial = vidaInicioAltarJugador.get(uuid);
        double vidaPerdida = vidaInicial - player.getHealth();
        
        if (vidaPerdida < 0) vidaPerdida = 0; // Si se curó
        
        // Feedback visual
        if (ticksEnActo % 20 == 0) {
            int corazonesPerdidos = (int)(vidaPerdida / 2);
            player.sendActionBar("§5⧖ §cSacrificio: §f" + corazonesPerdidos + "/10 corazones");
            
            // Partículas de sangre
            if (corazonesPerdidos > 0) {
                Location centro = altarLoc.clone().add(0.5, 1, 0.5);
                centro.getWorld().spawnParticle(Particle.DUST, centro, 5, 
                    0.5, 0.5, 0.5, 
                    new Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
            }
        }
        
        // Completar si perdió 10+ corazones
        if (vidaPerdida >= 20) { // 20 HP = 10 corazones
            // Darkness dramático
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.DARKNESS, 100, 0));
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                completarAltar(player, 3, altarLoc, "§8§l⧖ Fragmento del Sacrificio",
                    new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 600, 2),
                    new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION, 1200, 1));
            }, 100L);
        }
    }
    
    /**
     * ALTAR 4: LA PURIFICACIÓN - Matar 5 criaturas
     */
    private void procesarAltar4Purificacion(Player player, Location altarLoc) {
        UUID uuid = player.getUniqueId();
        
        // Inicializar y spawnear criaturas
        if (!tiempoInicioAltarJugador.containsKey(uuid)) {
            tiempoInicioAltarJugador.put(uuid, System.currentTimeMillis());
            criaturasEliminadasPorJugador.put(uuid, 0);
            
            player.sendMessage("");
            player.sendMessage("§5§l⧖ ALTAR DE LA PURIFICACIÓN");
            player.sendMessage("§7Elimina §e5 recuerdos defectuosos");
            player.sendMessage("§8\"...copias defectuosas emergen...\"");
            player.sendMessage("");
            
            soundUtil.playSound(player, Sound.ENTITY_WARDEN_AMBIENT, 0.5f, 0.7f);
            
            // Spawnear 5 criaturas
            String[] nombres = {
                "§8Recuerdo Defectuoso",
                "§8Eco Corrupto",
                "§8Memoria Fragmentada",
                "§8Copia Errónea",
                "§8Fragmento Hostil"
            };
            
            for (int i = 0; i < 5; i++) {
                double angle = Math.toRadians(i * 72);
                Location spawnLoc = altarLoc.clone().add(
                    Math.cos(angle) * 4,
                    0,
                    Math.sin(angle) * 4
                );
                spawnLoc.setY(altarLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);
                
                // Alternar entre zombies y skeletons
                EntityType tipo = (i % 2 == 0) ? EntityType.ZOMBIE : EntityType.SKELETON;
                org.bukkit.entity.LivingEntity criatura = (org.bukkit.entity.LivingEntity) 
                    spawnLoc.getWorld().spawnEntity(spawnLoc, tipo);
                
                criatura.customName(net.kyori.adventure.text.Component.text(nombres[i]));
                criatura.setCustomNameVisible(true);
                criaturasDeAltar.add(criatura.getUniqueId());
                
                // Partículas de spawn
                spawnLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, spawnLoc, 20, 0.5, 0.5, 0.5, 0.1);
            }
            
            return;
        }
        
        // Mostrar progreso
        int eliminadas = criaturasEliminadasPorJugador.getOrDefault(uuid, 0);
        if (ticksEnActo % 20 == 0 && eliminadas < 5) {
            player.sendActionBar("§5⧖ §ePurificación: §f" + eliminadas + "/5 eliminados");
        }
        
        // Completar si eliminó 5
        if (eliminadas >= 5) {
            completarAltar(player, 4, altarLoc, "§8§l⧖ Fragmento de Purificación",
                new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, 1200, 1),
                new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 1200, 0));
        }
    }
    
    /**
     * ALTAR 5: LA UNIÓN - Colocar 4 fragmentos anteriores
     * (Simplificado: automáticamente se activa al tener los 4 anteriores)
     */
    private void procesarAltar5Union(Player player, Location altarLoc) {
        UUID uuid = player.getUniqueId();
        Set<Integer> altaresCompletados = altaresCompletadosPorJugador.getOrDefault(uuid, new HashSet<>());
        
        // Verificar que tenga los 4 altares anteriores
        if (altaresCompletados.size() < 4) {
            if (ticksEnActo % 40 == 0) {
                player.sendMessage("§8⧖ ...fragmentos incompletos...");
                player.sendMessage("§7Necesitas completar los §e4 altares anteriores");
            }
            return;
        }
        
        // Mostrar instrucción
        if (!tiempoInicioAltarJugador.containsKey(uuid)) {
            tiempoInicioAltarJugador.put(uuid, System.currentTimeMillis());
            
            player.sendMessage("");
            player.sendMessage("§5§l⧖ ALTAR DE LA UNIÓN");
            player.sendMessage("§7Los fragmentos resuenan...");
            player.sendMessage("§8\"...fragmentos recolectados...\"");
            player.sendMessage("");
            
            soundUtil.playSound(player, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.5f);
        }
        
        // Activar automáticamente después de 3 segundos
        long tiempoTranscurrido = System.currentTimeMillis() - tiempoInicioAltarJugador.get(uuid);
        if (tiempoTranscurrido >= 3000) {
            completarAltarFinal(player, altarLoc);
        }
    }
    
    /**
     * Completar un altar y dar recompensas
     */
    private void completarAltar(Player player, int numAltar, Location altarLoc, String nombreFragmento, 
                               org.bukkit.potion.PotionEffect... efectos) {
        UUID uuid = player.getUniqueId();
        
        // Registrar completado
        altaresCompletadosPorJugador.putIfAbsent(uuid, new HashSet<>());
        altaresCompletadosPorJugador.get(uuid).add(numAltar);
        fragmentosInspeccionados.add(altarLoc);
        
        // Limpiar tracking
        tiempoInicioAltarJugador.remove(uuid);
        posicionInicioAltarJugador.remove(uuid);
        vidaInicioAltarJugador.remove(uuid);
        altarActualJugador.remove(uuid);
        
        // Tracking de participación
        participacionFragmentos.put(uuid, participacionFragmentos.getOrDefault(uuid, 0) + 1);
        
        // Efectos visuales épicos
        Location centro = altarLoc.clone().add(0.5, 1, 0.5);
        
        // Explosión de partículas
        centro.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, centro, 50, 1, 1, 1, 0.3);
        centro.getWorld().spawnParticle(Particle.END_ROD, centro, 30, 0.5, 1, 0.5, 0.2);
        centro.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, centro, 20, 0.5, 0.5, 0.5, 0.1);
        
        // Anillos de partículas
        for (int ring = 0; ring < 3; ring++) {
            final int r = ring;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (int angle = 0; angle < 360; angle += 10) {
                    double rad = Math.toRadians(angle);
                    double radio = 2 + r * 1.5;
                    Location particleLoc = centro.clone().add(
                        Math.cos(rad) * radio, 0.2, Math.sin(rad) * radio
                    );
                    centro.getWorld().spawnParticle(Particle.GLOW, particleLoc, 1, 0, 0, 0, 0);
                }
            }, ring * 5L);
        }
        
        // Sonidos
        soundUtil.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        soundUtil.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.5f);
        soundUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.8f);
        
        // Mensaje de completado
        player.sendMessage("");
        player.sendMessage("§5§l✓ ALTAR COMPLETADO");
        player.sendMessage("§7Has obtenido: " + nombreFragmento);
        player.sendMessage("");
        
        // Aplicar efectos
        for (org.bukkit.potion.PotionEffect efecto : efectos) {
            player.addPotionEffect(efecto);
        }
        
        // Actualizar BossBar
        if (bossBarProgreso != null) {
            double progreso = (double) fragmentosInspeccionados.size() / fragmentosLocations.size();
            bossBarProgreso.setProgress(Math.min(progreso, 1.0));
            bossBarProgreso.setTitle("§5Fragmentos: " + fragmentosInspeccionados.size() + "/" + fragmentosLocations.size());
        }
        
        // Mostrar diálogo según altar
        String contexto = "FRAGMENTO_ENCONTRADO";
        mostrarDialogoForma(contexto);
    }
    
    /**
     * Completar altar final y transicionar a Acto 2
     */
    private void completarAltarFinal(Player player, Location altarLoc) {
        UUID uuid = player.getUniqueId();
        
        // Evitar múltiples activaciones
        if (altaresCompletadosPorJugador.getOrDefault(uuid, new HashSet<>()).contains(5)) {
            return;
        }
        
        altaresCompletadosPorJugador.putIfAbsent(uuid, new HashSet<>());
        altaresCompletadosPorJugador.get(uuid).add(5);
        fragmentosInspeccionados.add(altarLoc);
        
        // Secuencia cinemática épica
        Location centro = altarLoc.clone().add(0.5, 1, 0.5);
        
        player.sendMessage("");
        player.sendMessage("§5§l⧖ ALTAR DE LA UNIÓN");
        player.sendMessage("§8\"...fragmentos recolectados...\"");
        player.sendMessage("§8\"...pero el eco permanece...\"");
        player.sendMessage("§8\"...algo más grande despierta...\"");
        player.sendMessage("§8\"...en el vacío...\"");
        player.sendMessage("");
        
        soundUtil.playSound(player, Sound.ENTITY_WARDEN_AMBIENT, 0.8f, 0.4f);
        
        // Efectos visuales masivos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Explosión de luz
            centro.getWorld().spawnParticle(Particle.FLASH, centro, 5, 0, 0, 0, 0);
            centro.getWorld().spawnParticle(Particle.ASH, centro, 100, 2, 2, 2, 0.2);
            centro.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, centro, 50, 1, 1, 1, 0.3);
            
            soundUtil.playSound(centro, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
            soundUtil.playSound(centro, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.6f);
            
            player.sendTitle("§c§l⚠", "§4¡UN MAL RECUERDO DESPIERTA!", 10, 60, 20);
        }, 40L);
        
        // Dar efecto Glowing permanente
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.GLOWING, 999999, 0, false, false));
        
        // Completar Acto 1 después de la secuencia
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!acto1Completado) {
                acto1Completado = true;
                completarActo1();
            }
        }, 80L);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ACTO 2: LA PIEDRA SE QUIEBRA
    // ═══════════════════════════════════════════════════════════════════
    
    private void iniciarActo2() {
        plugin.getLogger().info("[SusurroPiedraRota] Iniciando transición cinemática ÉPICA a Acto 2");
        
        // ✨ NUEVO: Limpiar mobs hostiles para la narrativa del Acto 2
        limpiarMobsHostilesCercanos();
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 1: FADEOUT DRAMÁTICO (0-5 segundos)
        // ═══════════════════════════════════════════════════════════════
        
        // Pantalla negra progresiva con múltiples capas
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participantesOriginales.contains(p.getUniqueId())) {
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.BLINDNESS, 100, 0, true, false));
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOWNESS, 100, 3, true, false));
                p.sendTitle("§0▬▬▬▬▬▬▬▬▬", "§8§oAlgo despierta...", 20, 60, 20);
            }
        }
        
        playSoundToAll(Sound.ENTITY_WITHER_DEATH, 0.6f, 0.4f);
        playSoundToAll(Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 1.0f, 0.6f);
        
        // Partículas de oscuridad
        for (Player p : Bukkit.getOnlinePlayers()) {
            Location pLoc = p.getLocation();
            for (int i = 0; i < 80; i++) {
                double ox = (Math.random() - 0.5) * 12;
                double oy = Math.random() * 6;
                double oz = (Math.random() - 0.5) * 12;
                pLoc.getWorld().spawnParticle(Particle.SQUID_INK, pLoc.clone().add(ox, oy, oz), 2, 0.1, 0.1, 0.1, 0);
            }
        }
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 2: TRANSICIÓN CON HEARTBEAT (5-8 segundos)
        // ═══════════════════════════════════════════════════════════════
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            // Heartbeat 1
            playSoundToAll(Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.6f);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (participantesOriginales.contains(p.getUniqueId())) {
                    p.sendTitle("§4♥", "", 5, 15, 5);
                }
            }
        }, 100L);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            // Heartbeat 2 más fuerte
            playSoundToAll(Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.7f);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (participantesOriginales.contains(p.getUniqueId())) {
                    p.sendTitle("§c§l♥", "", 5, 15, 5);
                }
            }
        }, 130L);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            // Heartbeat 3 máximo
            playSoundToAll(Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.8f);
            playSoundToAll(Sound.ENTITY_WARDEN_ROAR, 0.5f, 0.5f);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (participantesOriginales.contains(p.getUniqueId())) {
                    p.sendTitle("§c§l❤", "§4§oLos recuerdos despiertan...", 10, 40, 20);
                }
            }
        }, 160L);
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 3: REVELACIÓN DEL ACTO (8-15 segundos)
        // ═══════════════════════════════════════════════════════════════
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            actoActual = Acto.PIEDRA_QUIEBRA;
            ticksEnActo = 0;
            
            // Intensificar ambiente
            intensificarAmbienteActo2();
            
            // Título épico con explosión de partículas
            enviarTituloCinematicoTodos(
                "§c§lACTO II",
                "§8✦ §5§lLA DEFENSA DEL ALTAR §8✦",
                100
            );
            
            // Sonidos épicos superpuestos
            playSoundToAll(Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
            playSoundToAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.4f);
            playSoundToAll(Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.7f);
            playSoundToAll(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.6f);
            
            // Partículas de revelación
            for (Player p : Bukkit.getOnlinePlayers()) {
                Location loc = p.getLocation();
                loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.add(0, 2, 0), 60, 3, 2, 3, 0.1);
                loc.getWorld().spawnParticle(Particle.END_ROD, loc, 40, 2, 1, 2, 0.05);
                
                // Relámpagos cercanos
                loc.getWorld().strikeLightningEffect(loc.clone().add(
                    (Math.random() - 0.5) * 40, 0, (Math.random() - 0.5) * 40));
            }
        }, 200L); // 10 segundos
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 4: PANEL DE OBJETIVOS (15-30 segundos)
        // ═══════════════════════════════════════════════════════════════
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            broadcastNarrative("");
            broadcastNarrative("§c§l⚔ §8§m════════════════════════════════════════════ §c§l⚔");
            playSoundToAll(Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
        }, 300L); // 15 segundos
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            broadcastNarrative("");
            broadcastNarrative("          §c§l⧗ ACTO II: §5§lUN MAL RECUERDO DESPIERTA §c§l⧗");
            playSoundToAll(Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.0f);
        }, 360L); // 18 segundos
        
        // Objetivos con tiempo de lectura extendido
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            broadcastNarrative("");
            broadcastNarrative("    §8◆ §7El altar comienza a recordar eventos olvidados...");
            playSoundToAll(Sound.BLOCK_NOTE_BLOCK_BELL, 0.4f, 0.8f);
        }, 440L); // 22 segundos
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            broadcastNarrative("    §8◆ §7Copias defectuosas emergen de la memoria rota");
            playSoundToAll(Sound.BLOCK_NOTE_BLOCK_BELL, 0.4f, 1.0f);
        }, 520L); // 26 segundos
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            broadcastNarrative("    §8◆ §c§lNo dejes que los recuerdos te consuman");
            playSoundToAll(Sound.BLOCK_NOTE_BLOCK_BELL, 0.4f, 1.2f);
        }, 600L); // 30 segundos
        
        // Mensaje del Observador
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            broadcastNarrative("");
            broadcastNarrative("    §5§o\"Estos ecos... no son míos. Son de algo más antiguo.\"");
            broadcastNarrative("    §8§o— El Observador");
            playSoundToAll(Sound.ENTITY_WARDEN_AMBIENT, 0.4f, 0.5f);
        }, 680L); // 34 segundos
        
        // Cierre
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            broadcastNarrative("");
            broadcastNarrative("§c§l⚔ §8§m════════════════════════════════════════════ §c§l⚔");
            broadcastNarrative("");
            playSoundToAll(Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 0.8f);
        }, 760L); // 38 segundos
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 5: DIÁLOGO DE LA FORMA E INICIO DE OLEADAS (40-50 segundos)
        // ═══════════════════════════════════════════════════════════════
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            // Diálogo de La Forma
            mostrarDialogoForma("ACTO2_INICIO");
            
            // Efecto de distorsión temporal
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (participantesOriginales.contains(p.getUniqueId())) {
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SLOWNESS, 140, 3, true, false));
                    aplicarZoomIn(p, 3);
                }
            }
        }, 800L); // 40 segundos
        
        // Generar grieta
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            generarGrietaForma();
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                aplicarScreenShakeIntenso(p, 2);
            }
        }, 880L); // 44 segundos
        
        // Inicio de criaturas
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive() && actoActual == Acto.PIEDRA_QUIEBRA) {
                broadcastNarrative("");
                broadcastNarrative("§c§l⚠ §7Las criaturas de memoria comienzan a materializarse...");
                broadcastNarrative("");
                playSoundToAll(Sound.ENTITY_WITHER_AMBIENT, 0.7f, 0.6f);
            }
        }, 960L); // 48 segundos
    }
    
    private void generarGrietaForma() {
        World world = Bukkit.getWorlds().get(0);
        
        // 🎯 MEJORA: Altar donde están los jugadores (ubicación promedio directa)
        Location ubicacionPromedio = calcularUbicacionPromedioJugadores();
        
        if (ubicacionPromedio == null) {
            // Fallback: usar spawn del mundo
            ubicacionPromedio = world.getSpawnLocation();
        }
        
        // Generar altar justo donde están los jugadores (en superficie)
        int x = ubicacionPromedio.getBlockX();
        int z = ubicacionPromedio.getBlockZ();
        int y = world.getHighestBlockYAt(x, z);
        
        grietaLocation = new Location(world, x, y, z);
        
        plugin.getLogger().info(String.format(
            "[SusurroPiedraRota] Grieta de Forma generada en: %s",
            locationToString(grietaLocation)
        ));
        
        // Crear estructura de grieta (agujero vertical)
        crearEstructuraGrieta();
        
        // Iniciar efectos visuales
        iniciarEfectosGrieta();
        
        // Programar oleadas
        programarOleadas();
    }
    
    private void crearEstructuraGrieta() {
        int radio = 4;
        int profundidad = 12;
        
        // Crear agujero vertical con forma irregular
        for (int h = 0; h < profundidad; h++) {
            double radioNivel = radio - (h * 0.2); // Se estrecha al bajar
            
            for (int x = -radio; x <= radio; x++) {
                for (int z = -radio; z <= radio; z++) {
                    double distancia = Math.sqrt(x * x + z * z);
                    if (distancia <= radioNivel) {
                        Location blockLoc = grietaLocation.clone().add(x, -h, z);
                        blockLoc.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
        
        // Borde de obsidiana llorosa y deepslate agrietado
        for (int x = -radio - 1; x <= radio + 1; x++) {
            for (int z = -radio - 1; z <= radio + 1; z++) {
                double distancia = Math.sqrt(x * x + z * z);
                if (distancia > radio && distancia <= radio + 1) {
                    Location bordeLoc = grietaLocation.clone().add(x, 0, z);
                    Material borde = Math.random() < 0.3 ? Material.CRYING_OBSIDIAN : Material.CRACKED_DEEPSLATE_BRICKS;
                    bordeLoc.getBlock().setType(borde);
                    
                    // Velas moradas en algunas posiciones del borde
                    if (Math.random() < 0.2) {
                        bordeLoc.clone().add(0, 1, 0).getBlock().setType(Material.PURPLE_CANDLE);
                    }
                }
            }
        }
        
        // Cadenas colgantes desde los bordes
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            int x = (int)(radio * Math.cos(angle));
            int z = (int)(radio * Math.sin(angle));
            
            Location cadenaTop = grietaLocation.clone().add(x, 1, z);
            for (int y = 0; y < 4; y++) {
                Location cadenaLoc = cadenaTop.clone().add(0, -y, 0);
                cadenaLoc.getBlock().setType(Material.CHAIN);
            }
            
            // Linterna al final de la cadena
            cadenaTop.clone().add(0, -4, 0).getBlock().setType(Material.SOUL_LANTERN);
        }
    }
    
    private void iniciarEfectosGrieta() {
        // ✨ SONIDO AMBIENTE CONSTANTE de portal
        grietaSoundTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (actoActual != Acto.PIEDRA_QUIEBRA) {
                if (grietaSoundTask != null) {
                    grietaSoundTask.cancel();
                }
                return;
            }
            
            if (grietaLocation == null || grietaLocation.getWorld() == null) {
                return;
            }
            
            // Sonido de portal ambiente constante
            grietaLocation.getWorld().playSound(
                grietaLocation,
                Sound.BLOCK_PORTAL_AMBIENT,
                0.6f,
                0.7f
            );
            
            // Ocasionalmente añadir susurros inquietantes
            if (Math.random() < 0.2) {
                grietaLocation.getWorld().playSound(
                    grietaLocation,
                    Sound.ENTITY_VEX_AMBIENT,
                    0.3f,
                    0.5f
                );
            }
        }, 0L, 60L); // Cada 3 segundos
        
        // Partículas cinematográficas AAA densas y variadas
        grietaParticleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (actoActual != Acto.PIEDRA_QUIEBRA) {
                // ✅ Cancelar el task antes de salir
                if (grietaParticleTask != null) {
                    grietaParticleTask.cancel();
                }
                return;
            }
            
            // ✅ Null safety
            if (grietaLocation == null || grietaLocation.getWorld() == null) {
                return;
            }
            
            Location center = grietaLocation.clone().add(0.5, 4, 0.5);
            double tiempo = System.currentTimeMillis() / 1000.0;
            
            // ✨ 1. Portal giratorio DOBLE HÉLICE (densidad reducida)
            for (int i = 0; i < 15; i++) { // Reducido de 30 a 15 para rendimiento
                double t = i * 0.5; // Menos denso
                double radius = 2.5;
                double y = t * 0.3;
                
                // Hélice 1 - SOUL_FIRE_FLAME (sentido horario)
                grietaLocation.getWorld().spawnParticle(
                    Particle.SOUL_FIRE_FLAME,
                    center.clone().add(
                        radius * Math.cos(t + tiempo * 2),
                        y,
                        radius * Math.sin(t + tiempo * 2)
                    ),
                    1, 0, 0, 0, 0.01 // 1 partícula por punto
                );
                
                // Hélice 2 - REVERSE_PORTAL (sentido antihorario)
                grietaLocation.getWorld().spawnParticle(
                    Particle.REVERSE_PORTAL,
                    center.clone().add(
                        radius * Math.cos(t - tiempo * 2 + Math.PI),
                        y,
                        radius * Math.sin(t - tiempo * 2 + Math.PI)
                    ),
                    1, 0, 0, 0, 0.01 // Reducido a 1
                );
                
                // Hélice 3 - PORTAL (oscilante)
                grietaLocation.getWorld().spawnParticle(
                    Particle.PORTAL,
                    center.clone().add(
                        radius * Math.cos(t + tiempo + Math.PI * 0.5),
                        y + Math.sin(tiempo + i * 0.5) * 0.3,
                        radius * Math.sin(t + tiempo + Math.PI * 0.5)
                    ),
                    2, 0, 0, 0, 0.01
                );
            }
            
            // ✨ 2. Partículas siendo ABSORBIDAS hacia el centro (más intenso)
            for (int i = 0; i < 20; i++) { // Duplicado de 10 a 20
                double angle = Math.random() * Math.PI * 2;
                double distance = 5 + Math.random() * 5;
                double speed = 0.4; // Más rápido
                
                Location start = center.clone().add(
                    Math.cos(angle) * distance,
                    Math.random() * 4, // Más altura
                    Math.sin(angle) * distance
                );
                
                // Velocidad hacia el centro
                Vector velocity = center.toVector().subtract(start.toVector()).normalize().multiply(speed);
                
                grietaLocation.getWorld().spawnParticle(
                    Particle.WARPED_SPORE,
                    start,
                    0,
                    velocity.getX(),
                    velocity.getY(),
                    velocity.getZ(),
                    0.15
                );
                
                // ✨ Añadir REVERSE_PORTAL también absorbidos
                if (i % 2 == 0) {
                    grietaLocation.getWorld().spawnParticle(
                        Particle.REVERSE_PORTAL,
                        start,
                        0,
                        velocity.getX(),
                        velocity.getY(),
                        velocity.getZ(),
                        0.2
                    );
                }
            }
            
            // ✨ 3. Distorsión visual PRONUNCIADA (espiral invertida más densa)
            for (int i = 0; i < 25; i++) { // Aumentado de 15 a 25
                double angle = tiempo * 3 + (i * Math.PI * 2 / 25); // Más rápido
                double r = 3.5 - (i * 0.12);
                
                grietaLocation.getWorld().spawnParticle(
                    Particle.REVERSE_PORTAL,
                    center.clone().add(
                        Math.cos(angle) * r,
                        Math.sin(tiempo * 2 + i) * 0.7, // Más oscilación vertical
                        Math.sin(angle) * r
                    ),
                    3, 0.15, 0.15, 0.15, 0.4 // Más partículas
                );
                
                // ✨ Anillo secundario externo
                if (i % 3 == 0) {
                    grietaLocation.getWorld().spawnParticle(
                        Particle.PORTAL,
                        center.clone().add(
                            Math.cos(-angle) * (r + 1),
                            Math.cos(tiempo * 1.5 + i) * 0.5,
                            Math.sin(-angle) * (r + 1)
                        ),
                        2, 0.1, 0.1, 0.1, 0.3
                    );
                }
            }
            
            // 4. Núcleo central con vórtex denso
            grietaLocation.getWorld().spawnParticle(
                Particle.REVERSE_PORTAL,
                center,
                30, // Aumentado de 20
                0.4, 0.4, 0.4,
                0.6 // Más spread
            );
            
            grietaLocation.getWorld().spawnParticle(
                Particle.SQUID_INK,
                center,
                15, // Aumentado de 10
                0.3, 0.3, 0.3,
                0.15
            );
            
            // ✨ Añadir SOUL particles en el núcleo
            grietaLocation.getWorld().spawnParticle(
                Particle.SOUL,
                center,
                8,
                0.2, 0.2, 0.2,
                0.05
            );
            
            // 5. Explosiones periódicas de energía (cada 5 segundos)
            if (System.currentTimeMillis() % 5000 < 100) {
                // Onda expansiva 360°
                for (int angle = 0; angle < 360; angle += 10) {
                    final double rad = Math.toRadians(angle);
                    for (double dist = 0; dist < 6; dist += 0.5) {
                        final double finalDist = dist;
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            grietaLocation.getWorld().spawnParticle(
                                Particle.SOUL,
                                center.clone().add(
                                    Math.cos(rad) * finalDist,
                                    0.1,
                                    Math.sin(rad) * finalDist
                                ),
                                0,
                                Math.cos(rad) * 0.3,
                                0.2,
                                Math.sin(rad) * 0.3,
                                0.1
                            );
                        }, (long)(finalDist * 2));
                    }
                }
                
                // Sonidos de explosión
                soundUtil.playSound(grietaLocation, Sound.ENTITY_WITHER_SHOOT, 0.7f, 0.5f);
                soundUtil.playSound(grietaLocation, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.8f);
            }
        }, 0L, 2L);
        
        // Sonidos ambientales ominosos constantes
        grietaSoundTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (actoActual != Acto.PIEDRA_QUIEBRA) {
                // ✅ Cancelar el task antes de salir
                if (grietaSoundTask != null) {
                    grietaSoundTask.cancel();
                }
                return;
            }
            
            // ✅ Null safety
            if (grietaLocation == null || grietaLocation.getWorld() == null) {
                return;
            }
            
            // Sonidos de portal y ambiente
            soundUtil.playSound(grietaLocation, Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 0.6f);
            soundUtil.playSound(grietaLocation, Sound.ENTITY_ENDERMAN_AMBIENT, 0.3f, 0.5f);
            soundUtil.playSound(grietaLocation, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.3f, 0.8f);
        }, 0L, 100L); // Cada 5 segundos
    }
    
    /**
     * 🌟 SISTEMA DE GLOW PULSANTE DINÁMICO
     * Las criaturas brillan con intensidad variable según:
     * - Proximidad a jugadores (más cerca = más intenso)
     * - Estado de salud (menos vida = pulso más rápido)
     * - Color según tipo de criatura
     */
    private void iniciarSistemaGlowPulsante() {
        if (glowPulsanteTask != null) {
            glowPulsanteTask.cancel();
        }
        
        glowPulsanteTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (actoActual != Acto.PIEDRA_QUIEBRA) {
                if (glowPulsanteTask != null) {
                    glowPulsanteTask.cancel();
                }
                return;
            }
            
            // Actualizar cada criatura activa
            Iterator<Entity> iterator = criaturasActivas.iterator();
            while (iterator.hasNext()) {
                Entity e = iterator.next();
                
                if (!e.isValid() || e.isDead() || !(e instanceof LivingEntity)) {
                    iterator.remove();
                    tipoCriatura.remove(e.getUniqueId());
                    glowIntensidad.remove(e.getUniqueId());
                    continue;
                }
                
                LivingEntity criatura = (LivingEntity) e;
                String tipo = tipoCriatura.getOrDefault(e.getUniqueId(), "RAPIDA");
                
                // Calcular distancia al jugador más cercano
                double distMin = Double.MAX_VALUE;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (participantesOriginales.contains(p.getUniqueId())) {
                        double dist = p.getLocation().distance(criatura.getLocation());
                        if (dist < distMin) distMin = dist;
                    }
                }
                
                // Calcular intensidad base (50-100) según proximidad
                int intensidadBase;
                if (distMin < 5) intensidadBase = 90; // Muy cerca
                else if (distMin < 10) intensidadBase = 70;
                else if (distMin < 20) intensidadBase = 55;
                else intensidadBase = 40;
                
                // Ajustar por salud (menos vida = más intenso)
                double saludPorcentaje = criatura.getHealth() / criatura.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                if (saludPorcentaje < 0.3) intensidadBase += 20; // Crítico
                else if (saludPorcentaje < 0.6) intensidadBase += 10;
                
                // Pulso sinusoidal suave (oscila ±15)
                long tiempo = System.currentTimeMillis();
                double frecuencia = saludPorcentaje < 0.3 ? 0.008 : 0.004; // Pulso rápido si está herido
                double pulso = Math.sin(tiempo * frecuencia) * 15;
                int intensidadFinal = Math.max(30, Math.min(100, intensidadBase + (int)pulso));
                
                glowIntensidad.put(e.getUniqueId(), intensidadFinal);
                
                // Aplicar efecto GLOWING dinámico
                int duracion = 20; // 1 segundo (se renueva constantemente)
                criatura.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.GLOWING,
                    duracion,
                    0,
                    false,
                    false
                ), true); // true = sobrescribir efecto anterior
                
                // Partículas de intensidad según estado
                Location loc = criatura.getLocation().add(0, 0.5, 0);
                if (intensidadFinal > 80 && Math.random() < 0.3) {
                    Particle particulaIntensa = switch (tipo) {
                        case "RAPIDA" -> Particle.ELECTRIC_SPARK;
                        case "TANQUE" -> Particle.LAVA;
                        case "EXPLOSIVA" -> Particle.FLAME;
                        default -> Particle.END_ROD;
                    };
                    criatura.getWorld().spawnParticle(
                        particulaIntensa,
                        loc,
                        1,
                        0.1, 0.1, 0.1,
                        0.02
                    );
                }
            }
        }, 0L, 4L); // Cada 4 ticks (5 veces por segundo) para suavidad
    }
    
    /**
     * 👁 SPAWN DE MINIONS - Forma Invocadora
     * Al morir, la Forma Arcana invoca 2 minions pequeños que persiguen a los jugadores
     */
    private void spawnearMinionsInvocadora(Location deathLoc, World world) {
        // Efecto de invocación oscura
        world.spawnParticle(Particle.SMOKE, deathLoc, 50, 0.5, 0.5, 0.5, 0.15);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, deathLoc, 30, 0.3, 0.3, 0.3, 0.1);
        world.playSound(deathLoc, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 0.6f);
        world.playSound(deathLoc, Sound.ENTITY_VEX_AMBIENT, 0.8f, 1.2f);
        
        // Mensaje de alerta
        broadcastNarrative("§d👁 Una Forma Arcana invocó minions antes de morir!");
        
        // Spawn de 2 minions con delay
        for (int i = 0; i < 2; i++) {
            final int minionIndex = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Posición aleatoria cerca de la muerte
                double angle = Math.random() * Math.PI * 2;
                double distance = 1.5 + Math.random();
                Location spawnPos = deathLoc.clone().add(
                    Math.cos(angle) * distance,
                    0,
                    Math.sin(angle) * distance
                );
                
                // Ajustar Y al nivel del suelo
                spawnPos.setY(world.getHighestBlockYAt(spawnPos.getBlockX(), spawnPos.getBlockZ()) + 1);
                
                // Portal de spawn pequeño
                world.spawnParticle(Particle.PORTAL, spawnPos, 20, 0.3, 0.3, 0.3, 0.5);
                world.playSound(spawnPos, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.5f);
                
                // Crear minion (vex pequeño y rápido)
                Vex minion = (Vex) world.spawnEntity(spawnPos, EntityType.VEX);
                minion.customName(net.kyori.adventure.text.Component.text("§5✦ Eco de Forma"));
                minion.setCustomNameVisible(true);
                minion.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(5.0);
                minion.setHealth(5.0);
                minion.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(0.3);
                
                // Efectos visuales del minion
                minion.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.GLOWING,
                    999999, 0, false, false
                ));
                
                // Añadir a tracking
                criaturasActivas.add(minion);
                tipoCriatura.put(minion.getUniqueId(), "MINION");
                glowIntensidad.put(minion.getUniqueId(), 60);
                
                // Aura simple del minion
                BukkitTask auraTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    if (!minion.isValid() || minion.isDead()) {
                        return;
                    }
                    
                    Location loc = minion.getLocation().add(0, 0.5, 0);
                    
                    // Partículas orbitando (más pequeñas que las criaturas principales)
                    for (int j = 0; j < 2; j++) {
                        double orbitAngle = Math.toRadians((System.currentTimeMillis() / 15 + j * 180) % 360);
                        double radius = 0.4;
                        double x = Math.cos(orbitAngle) * radius;
                        double z = Math.sin(orbitAngle) * radius;
                        world.spawnParticle(Particle.WITCH, loc.clone().add(x, 0, z), 1, 0, 0, 0, 0);
                    }
                }, 0L, 3L);
                
                // Guardar task
                minion.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "aura_task"),
                    org.bukkit.persistence.PersistentDataType.INTEGER,
                    auraTask.getTaskId()
                );
                
                // Efecto de aparición
                world.spawnParticle(Particle.SOUL, spawnPos, 15, 0.2, 0.2, 0.2, 0.1);
                
            }, 10L + (minionIndex * 10L)); // 0.5s y 1s de delay
        }
    }
    
    private void spawnearEcoPrimordial() {
        if (grietaLocation == null || grietaLocation.getWorld() == null) return;
        
        World world = grietaLocation.getWorld();
        Location spawnLoc = grietaLocation.clone().add(0, 3, 0); // Spawn cerca del suelo
        
        // Anuncio dramático
        broadcastNarrative("");
        broadcastNarrative("§5§l⚠ ¡EL ECO PRIMORDIAL EMERGE DE LA GRIETA!");
        broadcastNarrative("§8§o...la memoria más oscura de la Forma cobra vida...");
        broadcastNarrative("");
        enviarTituloCinematicoTodos(
            "§5☠ ECO PRIMORDIAL ☠",
            "§c¡El guardián ancestral despierta!",
            70
        );
        
        // Efecto de invocación épica - temblor de tierra
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participantesOriginales.contains(p.getUniqueId())) {
                // Shake de cámara simulado
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.NAUSEA, 40, 0, false, false
                ));
            }
        }
        
        // Efecto de grieta abriéndose
        for (int ring = 1; ring <= 5; ring++) {
            final int r = ring;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (int i = 0; i < 16; i++) {
                    double angle = Math.toRadians(i * 22.5);
                    double x = Math.cos(angle) * r * 1.5;
                    double z = Math.sin(angle) * r * 1.5;
                    world.spawnParticle(
                        Particle.SOUL_FIRE_FLAME,
                        grietaLocation.clone().add(x, 0.5, z),
                        5, 0.1, 0.1, 0.1, 0.02
                    );
                }
                world.playSound(grietaLocation, Sound.BLOCK_SCULK_BREAK, 1.0f, 0.5f);
            }, r * 5L);
        }
        
        // Sonidos épicos escalados
        world.playSound(grietaLocation, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(grietaLocation, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.6f);
        }, 20L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(grietaLocation, Sound.ENTITY_WARDEN_ROAR, 1.0f, 0.8f);
            world.playSound(grietaLocation, Sound.ENTITY_WARDEN_EMERGE, 1.0f, 1.0f);
        }, 35L);
        
        // Crear el boss (Wither Skeleton gigante - más intimidante que Husk)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive() || actoActual != Acto.PIEDRA_QUIEBRA) return;
            
            WitherSkeleton boss = (WitherSkeleton) world.spawnEntity(spawnLoc, EntityType.WITHER_SKELETON);
            boss.customName(net.kyori.adventure.text.Component.text("§5§l☠ ECO PRIMORDIAL ☠"));
            boss.setCustomNameVisible(true);
            
            // Stats épicos
            boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(150.0);
            boss.setHealth(150.0);
            boss.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).setBaseValue(10.0);
            boss.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(0.32);
            boss.getAttribute(org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE).setBaseValue(0.8);
            boss.getAttribute(org.bukkit.attribute.Attribute.ARMOR).setBaseValue(10.0);
            
            // Equipamiento temático
            ItemStack espada = new ItemStack(Material.NETHERITE_SWORD);
            espada.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, 5);
            espada.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 2);
            boss.getEquipment().setItemInMainHand(espada);
            boss.getEquipment().setItemInMainHandDropChance(0);
            
            ItemStack casco = new ItemStack(Material.NETHERITE_HELMET);
            casco.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, 4);
            boss.getEquipment().setHelmet(casco);
            boss.getEquipment().setHelmetDropChance(0);
            
            // Efectos de buff
            boss.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.RESISTANCE, 999999, 1, false, false
            ));
            boss.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, 999999, 0, false, false
            ));
            boss.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.STRENGTH, 999999, 1, false, false
            ));
            
            // Tracking
            boss.addScoreboardTag("eco_primordial");
            criaturasActivas.add(boss);
            tipoCriatura.put(boss.getUniqueId(), "BOSS_PRIMORDIAL");
            glowIntensidad.put(boss.getUniqueId(), 100);
            
            // Partículas de aura permanente
            final UUID bossId = boss.getUniqueId();
            BukkitTask auraTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                Entity entity = Bukkit.getEntity(bossId);
                if (entity == null || !entity.isValid() || entity.isDead()) return;
                
                Location loc = entity.getLocation();
                // Aura de fuego del alma
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians((System.currentTimeMillis() / 20 + i * 45) % 360);
                    double x = Math.cos(angle) * 1.2;
                    double z = Math.sin(angle) * 1.2;
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(x, 1, z), 1, 0, 0, 0, 0);
                }
                // Ojos brillantes
                world.spawnParticle(Particle.DUST, loc.clone().add(0, 2.2, 0), 2,
                    0.1, 0.1, 0.1, new Particle.DustOptions(org.bukkit.Color.fromRGB(128, 0, 255), 0.8f));
            }, 0L, 2L);
            
            // Habilidad: Invocar refuerzos cada 12 segundos
            BukkitTask invocationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                Entity entity = Bukkit.getEntity(bossId);
                if (entity == null || !entity.isValid() || entity.isDead()) return;
                
                Location bossLoc = entity.getLocation();
                
                // Animación de invocación
                world.spawnParticle(Particle.REVERSE_PORTAL, bossLoc.add(0, 1, 0), 50, 1.5, 1, 1.5, 0.1);
                world.playSound(bossLoc, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 0.6f);
                world.playSound(bossLoc, Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 1.5f);
                
                broadcastNarrative("§d☠ ¡El Eco Primordial invoca siervos oscuros!");
                
                // Spawner 2-3 Vex como minions
                int cantidadMinions = 2 + new Random().nextInt(2);
                for (int i = 0; i < cantidadMinions; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    Location minionLoc = bossLoc.clone().add(Math.cos(angle) * 2, 1, Math.sin(angle) * 2);
                    
                    Vex minion = (Vex) world.spawnEntity(minionLoc, EntityType.VEX);
                    minion.customName(net.kyori.adventure.text.Component.text("§8Siervo Oscuro"));
                    minion.setCustomNameVisible(true);
                    minion.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(8.0);
                    minion.setHealth(8.0);
                    minion.addScoreboardTag("susurro_criatura");
                    criaturasActivas.add(minion);
                    tipoCriatura.put(minion.getUniqueId(), "MINION");
                    
                    world.spawnParticle(Particle.SOUL, minionLoc, 10, 0.3, 0.3, 0.3, 0.05);
                }
            }, 240L, 240L); // Cada 12 segundos
            
            boss.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "aura_task"),
                org.bukkit.persistence.PersistentDataType.INTEGER,
                auraTask.getTaskId()
            );
            boss.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "invocation_task"),
                org.bukkit.persistence.PersistentDataType.INTEGER,
                invocationTask.getTaskId()
            );
            
            // Portal de llegada épico
            world.spawnParticle(Particle.REVERSE_PORTAL, spawnLoc, 150, 2, 2, 2, 0.3);
            world.spawnParticle(Particle.SOUL, spawnLoc, 80, 1.5, 1.5, 1.5, 0.15);
            world.spawnParticle(Particle.FLASH, spawnLoc, 2, 0, 0, 0, 0);
            
            plugin.getLogger().info("[SusurroPiedraRota] Eco Primordial spawneado - 150 HP, invoca Vex cada 12s");
        }, 50L);
    }
    
    private void programarOleadas() {
        oleadaActual = 0;
        oleadasTotales = 3;
        
        // 🌟 INICIAR SISTEMA DE GLOW PULSANTE
        iniciarSistemaGlowPulsante();
        
        // Primera oleada inmediata
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                spawnearOleada();
            }
        }, 60L); // 3 segundos
        
        // Oleadas subsiguientes con timing progresivo (30s, luego 25s)
        // Oleada 1 -> 2: 30 segundos (respiro)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive() && actoActual == Acto.PIEDRA_QUIEBRA) {
                spawnearOleada();
            }
        }, 60L + 600L); // 3s + 30s
        
        // Oleada 2 -> 3: 25 segundos (tensión)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive() && actoActual == Acto.PIEDRA_QUIEBRA) {
                spawnearOleada();
            }
        }, 60L + 600L + 500L); // 3s + 30s + 25s
    }
    
    private void spawnearOleada() {
        oleadaActual++;
        tiempoInicioOleadaActual = System.currentTimeMillis(); // Iniciar timer para timeout
        
        // 🎯 Contar jugadores supervivencia vivos
        int jugadoresVivos = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participantesOriginales.contains(p.getUniqueId()) && 
                p.getGameMode() == org.bukkit.GameMode.SURVIVAL) {
                jugadoresVivos++;
            }
        }
        if (jugadoresVivos == 0) jugadoresVivos = 1; // Fallback
        
        // 🎯 MINI-EVENTO RELAJADO: Menos criaturas, más narrativa
        // Solo 2-3 criaturas base, escalando mínimamente
        int cantidadCriaturas = 2 + jugadoresVivos; // 3 para 1 jugador, 4 para 2, etc.
        
        // 🎯 Oleada 3 es siempre boss con +1 criatura (menos que antes)
        boolean esOleadaBoss = (oleadaActual == 3);
        if (esOleadaBoss) {
            cantidadCriaturas += 1; // Solo +1 en vez de +3
        }
        
        plugin.getLogger().info(String.format(
            "[SusurroPiedraRota] Spawneando oleada %d/%d (%d criaturas)%s",
            oleadaActual,
            oleadasTotales,
            cantidadCriaturas,
            esOleadaBoss ? " [MINI-BOSS]" : ""
        ));
        
        // Barra de progreso de oleadas
        String barraOleadas = crearBarraProgreso(oleadaActual, oleadasTotales);
        
        // 🎯 Anuncio diferente para oleadas boss
        if (esOleadaBoss) {
            broadcastNarrative("§c§l⚠ ¡EL NÚCLEO DE LA FORMA ATACA! - " + oleadaActual + "/" + oleadasTotales);
            broadcastNarrative("§6★ Todos los ecos despiertan ★");
        } else if (oleadaActual == 1) {
            broadcastNarrative(String.format("§b⚠ Oleada %d/%d - Phantoms espectrales emergen del pasado", oleadaActual, oleadasTotales));
        } else {
            broadcastNarrative(String.format("§c⚠ Oleada %d/%d - Los ecos de la Forma se intensifican", oleadaActual, oleadasTotales));
        }
        broadcastNarrative(barraOleadas);
        
        // Título cinematográfico con identidad de oleada
        String simbolos = generarSimbolosOleada(oleadaActual, oleadasTotales);
        if (oleadaActual == 1) {
            enviarTituloCinematicoTodos(
                "§b⚡ OLEADA 1: ECOS VELOCES ⚡",
                "§7Phantoms espectrales - " + simbolos + " - " + cantidadCriaturas + " Ecos",
                40
            );
        } else if (oleadaActual == 2) {
            enviarTituloCinematicoTodos(
                "§c⚔ OLEADA 2: LEGIÓN CORRUPTA ⚔",
                "§7Zombies blindados + Creepers - " + simbolos + " - " + cantidadCriaturas + " Ecos",
                40
            );
        } else {
            enviarTituloCinematicoTodos(
                "§5☠ OLEADA FINAL: CAOS TOTAL ☠",
                "§6★ " + simbolos + " - " + cantidadCriaturas + " Ecos + ECO PRIMORDIAL ★",
                50
            );
        }
        
        // 🎯 Oleadas progresivas con identidad única
        Random rand = new Random();
        if (oleadaActual == 1) {
            tipoOleadaActual = "RAPIDA"; // Oleada 1: Tutorial - Solo Phantoms veloces
        } else if (oleadaActual == 2) {
            tipoOleadaActual = "TANQUE_MIXTO"; // Oleada 2: Desafío - Zombies blindados + Creepers
        } else {
            tipoOleadaActual = "BOSS_CAOS"; // Oleada 3: Caos - Todos + Vex + mini-boss
        }
        
        plugin.getLogger().info("[SusurroPiedraRota] Tipo de oleada: " + tipoOleadaActual);
        
        // Efecto de pulso de energía al spawnear oleada
        efectoPulsoEnergiaTodos();
        
        // SONIDOS DE TENSIÓN PROGRESIVA según oleada
        reproducirSonidosTension(oleadaActual, oleadasTotales);
        
        // 🗣️ DIÁLOGOS DEL OBSERVER - Comentarios narrativos por oleada
        if (oleadaActual == 1) {
            mostrarDialogoForma("CRIATURAS_SPAWN");
            
            // 🎬 EFECTO DE CÁMARA: Slow motion para introducción de criaturas
            for (Player p : Bukkit.getOnlinePlayers()) {
                aplicarSlowMotion(p, 3);
            }
        } else if (oleadaActual == 2) {
            // Diálogo para oleada 2 (después de un delay para que lean el título)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (isActive()) {
                    mostrarDialogoForma("OLEADA_2_INICIO");
                }
            }, 40L);
        } else if (oleadaActual == 3) {
            // Diálogo para oleada 3 (oleada boss)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (isActive()) {
                    mostrarDialogoForma("OLEADA_3_INICIO");
                }
            }, 40L);
        }
        
        // Ritual de invocación cinematográfico
        if (grietaLocation != null && grietaLocation.getWorld() != null) {
            World world = grietaLocation.getWorld();
            
            // Círculo de invocación giratorio
            BukkitTask[] ritualTaskHolder = new BukkitTask[1];
            ritualTaskHolder[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                int ticks = 0;
                
                @Override
                public void run() {
                    // ✅ Auto-cancelación al completar
                    if (ticks >= 40) {
                        if (ritualTaskHolder[0] != null) {
                            ritualTaskHolder[0].cancel();
                        }
                        return;
                    }
                    
                    // Círculo de REVERSE_PORTAL girando
                    for (int i = 0; i < 12; i++) {
                        double angle = Math.toRadians((ticks * 18 + i * 30) % 360);
                        double radius = 3.0;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        world.spawnParticle(
                            Particle.REVERSE_PORTAL,
                            grietaLocation.clone().add(x, 0.2, z),
                            1,
                            0, 0, 0,
                            0
                        );
                    }
                    
                    // Símbolos de invocación elevados
                    if (ticks % 5 == 0) {
                        for (int i = 0; i < 8; i++) {
                            double angle = Math.toRadians(i * 45);
                            double x = Math.cos(angle) * 2;
                            double z = Math.sin(angle) * 2;
                            world.spawnParticle(
                                Particle.ENCHANT,
                                grietaLocation.clone().add(x, ticks * 0.05, z),
                                3,
                                0.2, 0.2, 0.2,
                                0
                            );
                        }
                    }
                    
                    ticks++;
                }
            }, 0L, 2L);
            
            // Cancelar el ritual después de 2 segundos (backup)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (ritualTaskHolder[0] != null) {
                    ritualTaskHolder[0].cancel();
                }
            }, 40L);
            
            // Sonidos de ritual
            playSoundToAll(Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
            playSoundToAll(Sound.BLOCK_PORTAL_TRIGGER, 0.8f, 0.8f);
            
            // Pulso de energía al final del ritual
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Explosión de SOUL_FIRE_FLAME en 360°
                for (int angle = 0; angle < 360; angle += 10) {
                    double rad = Math.toRadians(angle);
                    double dist = 4.0;
                    double x = Math.cos(rad) * dist;
                    double z = Math.sin(rad) * dist;
                    world.spawnParticle(
                        Particle.SOUL_FIRE_FLAME,
                        grietaLocation.clone().add(x, 0.5, z),
                        0,
                        Math.cos(rad) * 0.3,
                        0.2,
                        Math.sin(rad) * 0.3,
                        0.1
                    );
                }
                playSoundToAll(Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.6f);
            }, 40L);
        }
        
        // Actualizar objetivo para todos los jugadores
        for (Player p : Bukkit.getOnlinePlayers()) {
            objetivosPorJugador.put(p.getUniqueId(), grietaLocation);
        }
        
        // Spawn sincrónico para asegurar que se creen (después del ritual)
        for (int i = 0; i < cantidadCriaturas; i++) {
            final int delay = 50 + (i * 10); // Después del ritual + 0.5s entre cada spawn
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (isActive() && actoActual == Acto.PIEDRA_QUIEBRA) {
                    spawnearCriaturaForma();
                }
            }, delay);
        }
        
        // 🎯 MINI-BOSS en oleada 3 (Eco Primordial)
        if (oleadaActual == 3) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (isActive() && actoActual == Acto.PIEDRA_QUIEBRA && grietaLocation != null) {
                    spawnearEcoPrimordial();
                }
            }, 100L); // 5 segundos después del inicio de la oleada
        }
    }
    
    private void spawnearCriaturaForma() {
        if (grietaLocation == null) return;
        
        // Buscar jugador más cercano a la grieta
        Player jugadorCercano = encontrarJugadorMasCercano(grietaLocation);
        
        if (jugadorCercano == null) {
            // No hay jugadores, spawn cerca de la grieta
            Location spawnLoc = encontrarSpawnSeguro(grietaLocation, 5, 10);
            if (spawnLoc == null) {
                spawnLoc = grietaLocation.clone().add(0, 1, 0);
            }
            spawnearEnUbicacion(spawnLoc);
            return;
        }
        
        // Spawn JUSTO donde está el jugador (radio muy pequeño 3-8 bloques)
        Location referenciaSpawn = jugadorCercano.getLocation();
        Location spawnLoc = encontrarSpawnSeguro(referenciaSpawn, 3, 8);
        
        if (spawnLoc == null) {
            // Si no encuentra lugar cerca, intentar un poco más lejos
            spawnLoc = encontrarSpawnSeguro(referenciaSpawn, 8, 12);
        }
        
        if (spawnLoc == null) {
            // Último recurso: spawn en la ubicación del jugador
            spawnLoc = referenciaSpawn.clone().add(0, 1, 0);
        }
        
        spawnearEnUbicacion(spawnLoc);
    }
    
    private void spawnearEnUbicacion(Location spawnLoc) {
        
        // ✨ NUEVO: Efecto de implosión antes de aparecer (partículas convergiendo)
        for (int i = 0; i < 25; i++) {
            double angle = Math.random() * Math.PI * 2;
            double distance = 3 + Math.random() * 2;
            double height = Math.random() * 2;
            final Location startLoc = spawnLoc.clone().add(
                Math.cos(angle) * distance,
                height,
                Math.sin(angle) * distance
            );
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Vector velocity = spawnLoc.toVector().subtract(startLoc.toVector()).normalize().multiply(0.6);
                spawnLoc.getWorld().spawnParticle(
                    Particle.SOUL,
                    startLoc,
                    0,
                    velocity.getX(),
                    velocity.getY(),
                    velocity.getZ(),
                    0.4
                );
            }, i);
        }
        
        // Portal de spawn cinematográfico ANTES de crear la criatura
        World world = spawnLoc.getWorld();
        
        // Anillo de portal expandiéndose
        for (int radius = 1; radius <= 3; radius++) {
            final int r = radius;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (int i = 0; i < 16; i++) {
                    double angle = Math.toRadians(i * 22.5);
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    world.spawnParticle(
                        Particle.PORTAL,
                        spawnLoc.clone().add(x, 0.1, z),
                        5,
                        0.1, 0.1, 0.1,
                        0.5
                    );
                }
            }, r * 3L);
        }
        
        // Columna de REVERSE_PORTAL
        for (int y = 0; y < 3; y++) {
            final int height = y;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                world.spawnParticle(
                    Particle.REVERSE_PORTAL,
                    spawnLoc.clone().add(0, height * 0.5, 0),
                    15,
                    0.3, 0.3, 0.3,
                    0.1
                );
            }, y * 2L);
        }
        
        // Sonido de portal
        world.playSound(spawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.7f);
        world.playSound(spawnLoc, Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 1.5f);
        
        // SONIDOS ADICIONALES DE SPAWN MEJORADOS
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(spawnLoc, Sound.ENTITY_VEX_AMBIENT, 0.8f, 0.6f);
            world.playSound(spawnLoc, Sound.BLOCK_SCULK_CATALYST_BLOOM, 0.6f, 1.2f);
        }, 5L);
        
        // ✨ SPAWN AÉREO - Modificar ubicación a 25 bloques arriba
        final Location spawnAereo = spawnLoc.clone().add(0, 25, 0);
        
        // Portal aéreo con nubes
        for (int i = 0; i < 30; i++) {
            double angle = Math.random() * Math.PI * 2;
            double radius = 2;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            world.spawnParticle(
                Particle.CLOUD,
                spawnAereo.clone().add(x, 0, z),
                5,
                0.2, 0.2, 0.2,
                0.05
            );
        }
        
        // Sonido de aparición aérea
        world.playSound(spawnAereo, Sound.ENTITY_PHANTOM_FLAP, 1.0f, 0.6f);
        
        // CREAR CRIATURA después del efecto de portal - EN EL AIRE
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // 🎯 TIPOS DIVERSOS según oleada progresiva
            Random rand = new Random();
            String tipo;
            
            if ("RAPIDA".equals(tipoOleadaActual)) {
                // Oleada 1: Solo formas veloces (tutorial)
                tipo = "RAPIDA";
            } else if ("TANQUE_MIXTO".equals(tipoOleadaActual)) {
                // Oleada 2: 60% tanques, 30% rápidas, 10% explosivas
                double tipoRoll = rand.nextDouble();
                if (tipoRoll < 0.60) tipo = "TANQUE";
                else if (tipoRoll < 0.90) tipo = "RAPIDA";
                else tipo = "EXPLOSIVA";
            } else if ("BOSS_CAOS".equals(tipoOleadaActual)) {
                // Oleada 3: Caos total - distribución equilibrada
                double tipoRoll = rand.nextDouble();
                if (tipoRoll < 0.30) tipo = "TANQUE";
                else if (tipoRoll < 0.55) tipo = "RAPIDA";
                else if (tipoRoll < 0.75) tipo = "EXPLOSIVA";
                else tipo = "INVOCADORA";
            } else {
                // Fallback - distribución normal
                double tipoRoll = rand.nextDouble();
                if (tipoRoll < 0.45) tipo = "RAPIDA";
                else if (tipoRoll < 0.70) tipo = "TANQUE";
                else if (tipoRoll < 0.85) tipo = "EXPLOSIVA";
                else tipo = "INVOCADORA";
            }
            
            // 🎭 CRIATURAS VARIADAS TEMÁTICAS según tipo (NO más Silverfish)
            LivingEntity criatura;
            switch (tipo) {
                case "RAPIDA":
                    // Phantom: Rápido, vuela, temático con "ecos del pasado"
                    Phantom phantom = (Phantom) world.spawnEntity(spawnAereo, EntityType.PHANTOM);
                    phantom.setSize(1); // Tamaño pequeño
                    phantom.customName(net.kyori.adventure.text.Component.text("§b⚡ Eco Veloz"));
                    phantom.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(12.0);
                    phantom.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SPEED, 999999, 1, false, false
                    ));
                    criatura = phantom;
                    break;
                    
                case "TANQUE":
                    // Zombie con armadura: Resistente, lento, intimidante
                    Zombie zombie = (Zombie) world.spawnEntity(spawnAereo, EntityType.ZOMBIE);
                    zombie.setBaby(false);
                    zombie.customName(net.kyori.adventure.text.Component.text("§c⚔ Eco Colosal"));
                    zombie.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(50.0);
                    zombie.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(0.18);
                    zombie.getAttribute(org.bukkit.attribute.Attribute.ARMOR).setBaseValue(8.0);
                    zombie.getAttribute(org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE).setBaseValue(0.6);
                    // Equipar con armadura temática
                    zombie.getEquipment().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
                    zombie.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
                    zombie.getEquipment().setHelmetDropChance(0);
                    zombie.getEquipment().setChestplateDropChance(0);
                    criatura = zombie;
                    break;
                    
                case "EXPLOSIVA":
                    // Creeper cargado: Peligroso pero frágil
                    Creeper creeper = (Creeper) world.spawnEntity(spawnAereo, EntityType.CREEPER);
                    creeper.setPowered(true); // Cargado = más peligroso
                    creeper.customName(net.kyori.adventure.text.Component.text("§e💥 Eco Volátil"));
                    creeper.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(10.0);
                    creeper.setExplosionRadius(3);
                    creeper.setMaxFuseTicks(20); // Fusible más corto
                    criatura = creeper;
                    break;
                    
                case "INVOCADORA":
                default:
                    // Vex: Vuela, atraviesa paredes, invoca
                    Vex vex = (Vex) world.spawnEntity(spawnAereo, EntityType.VEX);
                    vex.customName(net.kyori.adventure.text.Component.text("§d👁 Eco Arcano"));
                    vex.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(20.0);
                    vex.setCharging(true);
                    // Equipar con espada encantada
                    ItemStack espada = new ItemStack(Material.IRON_SWORD);
                    espada.addEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, 2);
                    vex.getEquipment().setItemInMainHand(espada);
                    vex.getEquipment().setItemInMainHandDropChance(0);
                    criatura = vex;
                    break;
            }
            
            criatura.setCustomNameVisible(true);
            criatura.setHealth(criatura.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
            
            // Tag para identificar como criatura del evento
            criatura.addScoreboardTag("susurro_criatura");
            criatura.addScoreboardTag("tipo_" + tipo.toLowerCase());
            
            // ✨ XP dinámico generoso - Acto 2 da MUCHA XP
            int xpBase = 35 + (oleadaActual * 15); // Oleada 1: 50, Oleada 2: 65, Oleada 3: 80
            criatura.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "evento_xp"),
                org.bukkit.persistence.PersistentDataType.INTEGER,
                xpBase + (int)(Math.random() * 25) // +0-24 XP random
            );
            
            // NO usar efecto GLOWING permanente - ahora será dinámico
            tipoCriatura.put(criatura.getUniqueId(), tipo);
            glowIntensidad.put(criatura.getUniqueId(), 50); // Intensidad base 50%
            
            // ✨ SLOW FALLING durante caída para efecto dramático (solo si no vuela)
            if (!(criatura instanceof Phantom) && !(criatura instanceof Vex)) {
                criatura.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOW_FALLING,
                    80, // 4 segundos
                    0,
                    false,
                    false
                ));
            }
            
            // ✨ Trail de partículas durante la caída
            final BukkitTask[] trailTaskHolder = new BukkitTask[1];
            trailTaskHolder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!criatura.isValid() || criatura.isDead() || criatura.isOnGround()) {
                    if (trailTaskHolder[0] != null) {
                        trailTaskHolder[0].cancel();
                    }
                    
                    // ✨ Efecto de impacto al tocar el suelo
                    if (criatura.isValid() && !criatura.isDead()) {
                        Location landLoc = criatura.getLocation();
                        
                        // Onda expansiva
                        for (int i = 0; i < 20; i++) {
                            double angle = Math.random() * Math.PI * 2;
                            double radius = 1 + Math.random();
                            double x = Math.cos(angle) * radius;
                            double z = Math.sin(angle) * radius;
                            world.spawnParticle(
                                Particle.SMOKE,
                                landLoc.clone().add(x, 0.1, z),
                                3,
                                0, 0.1, 0,
                                0.05
                            );
                        }
                        
                        // Sonidos de impacto
                        world.playSound(landLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 1.5f);
                        world.playSound(landLoc, Sound.BLOCK_STONE_BREAK, 0.6f, 0.8f);
                    }
                    return;
                }
                
                // Trail de partículas CLOUD
                world.spawnParticle(
                    Particle.CLOUD,
                    criatura.getLocation().add(0, 0.5, 0),
                    5,
                    0.2, 0.2, 0.2,
                    0.02
                );
                
                // Ocasionalmente SOUL_FIRE_FLAME
                if (Math.random() < 0.3) {
                    world.spawnParticle(
                        Particle.SOUL_FIRE_FLAME,
                        criatura.getLocation().add(0, 0.5, 0),
                        2,
                        0.1, 0.1, 0.1,
                        0.01
                    );
                }
            }, 0L, 2L);
            
            // EFECTO DE SPAWN ÉPICO - Explosión de partículas
            world.spawnParticle(
                Particle.TOTEM_OF_UNDYING,
                spawnAereo.clone().add(0, 1, 0),
                30,
                0.5, 1.0, 0.5,
                0.1
            );
            
            criaturasActivas.add(criatura);
            
            // 🌟 AURA DINÁMICA según tipo y estado
            String tipoFinal = tipo; // Para lambda
            BukkitTask auraTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!criatura.isValid() || criatura.isDead()) {
                    if (criatura.getPersistentDataContainer().has(
                        new org.bukkit.NamespacedKey(plugin, "aura_task"),
                        org.bukkit.persistence.PersistentDataType.INTEGER
                    )) {
                        int taskId = criatura.getPersistentDataContainer().get(
                            new org.bukkit.NamespacedKey(plugin, "aura_task"),
                            org.bukkit.persistence.PersistentDataType.INTEGER
                        );
                        Bukkit.getScheduler().cancelTask(taskId);
                    }
                    return;
                }
                
                if (criatura.getLocation() == null || criatura.getLocation().getWorld() == null) {
                    return;
                }
                
                Location loc = criatura.getLocation().add(0, 0.5, 0);
                
                // Detectar jugador cercano (estado de alerta)
                Player cercano = null;
                double distMin = Double.MAX_VALUE;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (participantesOriginales.contains(p.getUniqueId())) {
                        double dist = p.getLocation().distance(loc);
                        if (dist < distMin) {
                            distMin = dist;
                            cercano = p;
                        }
                    }
                }
                
                // Determinar color y patrón según estado
                Particle particula;
                double radioBase;
                int cantidad;
                
                if (distMin < 5) { // ATACANDO - Rojo intenso
                    particula = switch (tipoFinal) {
                        case "RAPIDA" -> Particle.ELECTRIC_SPARK;
                        case "TANQUE" -> Particle.LAVA;
                        case "EXPLOSIVA" -> Particle.FLAME;
                        case "INVOCADORA" -> Particle.WITCH;
                        default -> Particle.CRIMSON_SPORE;
                    };
                    radioBase = 0.8;
                    cantidad = 4;
                } else if (distMin < 15) { // PERSIGUIENDO - Amarillo
                    particula = switch (tipoFinal) {
                        case "RAPIDA" -> Particle.END_ROD;
                        case "TANQUE" -> Particle.SCULK_SOUL;
                        case "EXPLOSIVA" -> Particle.SOUL_FIRE_FLAME;
                        case "INVOCADORA" -> Particle.PORTAL;
                        default -> Particle.WARPED_SPORE;
                    };
                    radioBase = 0.7;
                    cantidad = 3;
                } else { // PATRULLANDO - Azul normal
                    particula = switch (tipoFinal) {
                        case "RAPIDA" -> Particle.SOUL;
                        case "TANQUE" -> Particle.GLOW;
                        case "EXPLOSIVA" -> Particle.ENCHANT;
                        case "INVOCADORA" -> Particle.REVERSE_PORTAL;
                        default -> Particle.REVERSE_PORTAL;
                    };
                    radioBase = 0.6;
                    cantidad = 3;
                }
                
                // Órbitas de partículas
                for (int i = 0; i < cantidad; i++) {
                    double angle = Math.toRadians((System.currentTimeMillis() / 10 + i * (360.0/cantidad)) % 360);
                    double x = Math.cos(angle) * radioBase;
                    double z = Math.sin(angle) * radioBase;
                    world.spawnParticle(particula, loc.clone().add(x, 0, z), 1, 0, 0, 0, 0);
                }
                
                // Rastro al moverse (solo para mobs terrestres)
                if (!(criatura instanceof Phantom) && !(criatura instanceof Vex)) {
                    if (criatura.getVelocity().lengthSquared() > 0.01) {
                        world.spawnParticle(particula, loc, 2, 0.2, 0.2, 0.2, 0.01);
                    }
                }
                
                // Nota: Los Creepers explotan automáticamente, no necesitamos lógica extra
            }, 0L, 2L);
            
            // Guardar el task para cancelarlo cuando muera
            criatura.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "aura_task"),
                org.bukkit.persistence.PersistentDataType.INTEGER,
                auraTask.getTaskId()
            );
            
            // Explosión final de spawn
            world.spawnParticle(
                Particle.SOUL,
                spawnLoc.clone().add(0, 0.5, 0),
                20,
                0.3, 0.3, 0.3,
                0.1
            );
            world.playSound(spawnLoc, Sound.ENTITY_WITHER_SHOOT, 0.4f, 1.5f);
        }, 10L); // 0.5 segundos después del portal
    }
    
    private void tickActo2() {
        // === SISTEMA DE FALLBACK/TIMEOUT PARA OLEADAS ===
        if (oleadaActual > 0 && !criaturasActivas.isEmpty() && tiempoInicioOleadaActual > 0) {
            long tiempoTranscurrido = System.currentTimeMillis() - tiempoInicioOleadaActual;
            
            // Advertencia a los 2 minutos
            if (tiempoTranscurrido >= 120000 && tiempoTranscurrido < 121000) {
                broadcastNarrative("");
                broadcastNarrative("§e⚠ La oleada está tardando demasiado...");
                broadcastNarrative("§7Las criaturas restantes serán eliminadas en 1 minuto.");
                playSoundToAll(Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            }
            
            // Timeout a los 3 minutos - matar criaturas restantes
            if (tiempoTranscurrido >= TIMEOUT_OLEADA_MS) {
                broadcastNarrative("");
                broadcastNarrative("§6⧖ El Observador interviene...");
                broadcastNarrative("§8El Observador§7: §o\"...los recuerdos se desvanecen...\"");
                broadcastNarrative("");
                playSoundToAll(Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 0.5f);
                
                // Eliminar todas las criaturas con efectos
                for (Entity criatura : new ArrayList<>(criaturasActivas)) {
                    if (criatura.isValid() && !criatura.isDead()) {
                        Location loc = criatura.getLocation();
                        loc.getWorld().spawnParticle(Particle.SOUL, loc.add(0, 0.5, 0), 20, 0.3, 0.3, 0.3, 0.1);
                        criatura.remove();
                    }
                }
                criaturasActivas.clear();
                
                plugin.getLogger().warning(String.format(
                    "[SusurroPiedraRota] Oleada %d completada por TIMEOUT (fallback)",
                    oleadaActual
                ));
                
                tiempoInicioOleadaActual = 0; // Reset timer
            }
        }
        
        // Feedback de combate mejorado
        if (ticksEnActo % 100 == 0) {
            int criaturasVivas = criaturasActivas.size();
            if (criaturasVivas > 0 && oleadaActual > 0) {
                // Actualizar a todos con progreso
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (participantesOriginales.contains(p.getUniqueId())) {
                        String barra = crearBarraProgreso(oleadasTotales - oleadaActual + (criaturasVivas == 0 ? 1 : 0), oleadasTotales);
                        p.sendActionBar("§c⚔ Criaturas: " + criaturasVivas + " " + barra);
                    }
                }
            }
        }
        
        // Verificar si todas las criaturas están muertas y aplicar efectos de muerte
        Iterator<Entity> iterator = criaturasActivas.iterator();
        while (iterator.hasNext()) {
            Entity e = iterator.next();
            if (!e.isValid() || e.isDead()) {
                // Efectos de muerte cinematográficos
                Location deathLoc = e.getLocation().add(0, 0.5, 0);
                World world = deathLoc.getWorld();
                
                // 👁 INVOCADORA (Vex): Spawn de 2 minions al morir
                String tipo = tipoCriatura.get(e.getUniqueId());
                if ("INVOCADORA".equals(tipo)) {
                    spawnearMinionsInvocadora(deathLoc, world);
                }
                
                // Cancelar aura task si existe (ahora para cualquier LivingEntity)
                if (e instanceof LivingEntity) {
                    LivingEntity criatura = (LivingEntity) e;
                    
                    if (criatura.getPersistentDataContainer().has(
                        new org.bukkit.NamespacedKey(plugin, "aura_task"),
                        org.bukkit.persistence.PersistentDataType.INTEGER
                    )) {
                        int taskId = criatura.getPersistentDataContainer().get(
                            new org.bukkit.NamespacedKey(plugin, "aura_task"),
                            org.bukkit.persistence.PersistentDataType.INTEGER
                        );
                        Bukkit.getScheduler().cancelTask(taskId);
                    }
                }
                
                // ✨ EXPLOSIÓN DE FRAGMENTOS OSCUROS MEJORADA
                // 1. Explosión inicial de partículas (20 fragmentos volando)
                for (int i = 0; i < 20; i++) {
                    double angle = Math.toRadians(Math.random() * 360);
                    double pitch = Math.toRadians(Math.random() * 180);
                    double speed = 0.5 + Math.random() * 0.5;
                    Vector velocity = new Vector(
                        Math.cos(angle) * Math.sin(pitch) * speed,
                        Math.cos(pitch) * speed,
                        Math.sin(angle) * Math.sin(pitch) * speed
                    );
                    world.spawnParticle(
                        Particle.SMOKE,
                        deathLoc.clone().add(0, 0.5, 0),
                        0,
                        velocity.getX(),
                        velocity.getY(),
                        velocity.getZ(),
                        0.3
                    );
                }
                
                // 2. Nube de ceniza expansiva
                world.spawnParticle(
                    Particle.ASH,
                    deathLoc.clone().add(0, 0.5, 0),
                    40,
                    0.8, 0.8, 0.8,
                    0.15
                );
                
                // 3. Almas escapando en espiral ascendente
                for (int i = 0; i < 15; i++) {
                    final int delay = i;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double angle = delay * 0.5;
                        double radius = 0.5 + (delay * 0.05);
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        world.spawnParticle(
                            Particle.SOUL,
                            deathLoc.clone().add(x, delay * 0.15, z),
                            0,
                            0, 0.2, 0,
                            0.05
                        );
                    }, i);
                }
                
                // 4. Ondas expansivas en el suelo (3 anillos)
                for (int ring = 1; ring <= 3; ring++) {
                    final int r = ring;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (int i = 0; i < 20; i++) {
                            double angle = Math.toRadians(i * 18);
                            double x = Math.cos(angle) * r * 0.7;
                            double z = Math.sin(angle) * r * 0.7;
                            world.spawnParticle(
                                Particle.SQUID_INK,
                                deathLoc.clone().add(x, 0.1, z),
                                1,
                                0, 0, 0,
                                0
                            );
                        }
                    }, r * 2L);
                }
                
                // 5. Explosión de luz final
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    world.spawnParticle(
                        Particle.FLASH,
                        deathLoc.clone().add(0, 0.5, 0),
                        1,
                        0, 0, 0,
                        0
                    );
                }, 10L);
                
                // SONIDOS MEJORADOS DE MUERTE
                soundUtil.playSound(deathLoc, Sound.ENTITY_VEX_DEATH, 1.0f, 0.6f);
                soundUtil.playSound(deathLoc, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.5f, 1.5f);
                soundUtil.playSound(deathLoc, Sound.PARTICLE_SOUL_ESCAPE, 0.8f, 1.0f);
                soundUtil.playSound(deathLoc, Sound.ENTITY_WITHER_HURT, 0.6f, 1.8f);
                soundUtil.playSound(deathLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.6f);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    world.playSound(deathLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 1.2f);
                }, 10L);
                
                // SISTEMA DE COMBOS - Detectar quién mató
                if (e instanceof LivingEntity) {
                    LivingEntity living = (LivingEntity) e;
                    Player killer = living.getKiller();
                    if (killer != null && participantesOriginales.contains(killer.getUniqueId())) {
                        registrarKillCombo(killer);
                        
                        // Registrar participación en criaturas
                        participacionCriaturas.put(
                            killer.getUniqueId(),
                            participacionCriaturas.getOrDefault(killer.getUniqueId(), 0) + 1
                        );
                        
                        // ✨ DAR XP GENEROSO - Acto 2
                        org.bukkit.NamespacedKey xpKey = new org.bukkit.NamespacedKey(plugin, "evento_xp");
                        final int xpGanado;
                        
                        if (living.getPersistentDataContainer().has(xpKey, org.bukkit.persistence.PersistentDataType.INTEGER)) {
                            xpGanado = living.getPersistentDataContainer().get(xpKey, org.bukkit.persistence.PersistentDataType.INTEGER);
                        } else {
                            xpGanado = 30 + (int)(Math.random() * 15); // Default 30-44 XP
                        }
                        
                        killer.giveExp(xpGanado);
                        
                        // Orbe de XP visual
                        final int xpOrb = xpGanado / 5;
                        deathLoc.getWorld().spawn(deathLoc.clone().add(0, 0.5, 0), org.bukkit.entity.ExperienceOrb.class, orb -> {
                            orb.setExperience(xpOrb);
                        });
                        
                        killer.sendMessage("§a§l✦ +" + xpGanado + " XP §a§o¡Eco eliminado!");
                        killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                    }
                }
                
                iterator.remove();
            }
        }
        
        // Si todas las oleadas spawnearon y todas las criaturas murieron
        if (oleadaActual >= oleadasTotales && criaturasActivas.isEmpty() && !oleadasCompletadas) {
            oleadasCompletadas = true;
            oleadasCompletadasTotal++;
            
            // ✨ SISTEMA DE BOSS CADA 3 OLEADAS
            if (oleadasCompletadasTotal % 3 == 0 && !bossActivo && actoActual == Acto.PIEDRA_QUIEBRA) {
                spawnearBossEspecial();
                return; // No completar acto aún
            }
            
            // FEEDBACK ÉPICO DE VICTORIA DE OLEADAS
            broadcastNarrative("§a§l✓ ¡TODAS LAS OLEADAS COMPLETADAS!");
            enviarTituloCinematicoTodos(
                "§a§l⚔ VICTORIA ⚔",
                "§7Las criaturas han sido eliminadas",
                50
            );
            
            // 🗣️ DIÁLOGO - Victoria post-batalla con narrativa de transición
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (isActive()) {
                    mostrarDialogoForma("VICTORIA_ACTO2");
                }
            }, 40L);
            
            // Sonidos de victoria
            playSoundToAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            playSoundToAll(Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            
            // Efectos de victoria para todos
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (participantesOriginales.contains(p.getUniqueId())) {
                    p.spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
                }
            }
            
            // PROGRESO AUTOMÁTICO - Sin minijuego de patrón
            if (!acto2Completado) {
                acto2Completado = true;
                broadcastNarrative("§e⚡ Preparándose para el acto final...");
                // Delay de 3 segundos antes de completar Acto 2
                Bukkit.getScheduler().runTaskLater(plugin, this::completarActo2, 60L);
            }
        }
    }
    
    private void completarActo2() {
        plugin.getLogger().info("[SusurroPiedraRota] Acto 2 completado");
        
        // 🧹 LIMPIEZA: Eliminar grieta al completar Acto 2
        limpiarGrieta();
        
        // Limpiar efectos de grieta
        if (grietaParticleTask != null) grietaParticleTask.cancel();
        if (grietaSoundTask != null) grietaSoundTask.cancel();
        
        // Transición cinematográfica dramática
        actoActual = Acto.TRANSICION_3;
        
        // Fadeout con temblor de pantalla (simulado con títulos)
        for (Player p : Bukkit.getOnlinePlayers()) {
            // Efecto de temblor con títulos
            p.sendTitle(
                "§c§l⚠ ⚠ ⚠",
                "§4§lLA FORMA SE DEFORMA",
                5, 40, 10
            );
        }
        
        // Título épico después del temblor
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            enviarTituloCinematicoTodos(
                "⚠ LA GRIETA COLAPSA ⚠",
                "El vacío reclama lo que le pertenece...",
                60
            );
        }, 50L);
        
        // Explosión masiva de partículas en la grieta
        if (grietaLocation != null && grietaLocation.getWorld() != null) {
            World world = grietaLocation.getWorld();
            
            // Explosión de SOUL en 360°
            for (int angle = 0; angle < 360; angle += 5) {
                double rad = Math.toRadians(angle);
                for (double dist = 0; dist < 10; dist += 0.5) {
                    double dx = Math.cos(rad) * dist;
                    double dz = Math.sin(rad) * dist;
                    world.spawnParticle(
                        Particle.SOUL,
                        grietaLocation.clone().add(dx, 1, dz),
                        0,
                        0, 0.5, 0,
                        0.2
                    );
                }
            }
            
            // Columna de REVERSE_PORTAL implosionando
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (int y = 0; y < 30; y++) {
                    for (int i = 0; i < 20; i++) {
                        world.spawnParticle(
                            Particle.REVERSE_PORTAL,
                            grietaLocation.clone().add(
                                (Math.random() - 0.5) * 4,
                                y,
                                (Math.random() - 0.5) * 4
                            ),
                            0,
                            0, -1, 0,
                            0.1
                        );
                    }
                }
            }, 10L);
        }
        
        // Sonidos épicos de colapso
        playSoundToAll(Sound.ENTITY_WITHER_DEATH, 1.0f, 0.4f);
        playSoundToAll(Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
        
        // Destello intenso + sacudida de pantalla (colapso dimensional)
        crearDestelloTodos(8);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            sacudirPantallaTodos(5);
        }, 8L);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            playSoundToAll(Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.8f);
        }, 20L);
        
        // Fadeout negro progresivo
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            oscurecerProgresivoTodos(5);
        }, 40L);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                iniciarActo3();
            }
        }, 100L); // 5 segundos de transición épica
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ACTO 3: EL NÚCLEO DE FORMA
    // ═══════════════════════════════════════════════════════════════════
    
    private void iniciarActo3() {
        plugin.getLogger().info("[SusurroPiedraRota] Iniciando transición ÉPICA FINAL a Acto 3");
        
        // ✨ NUEVO: Limpiar mobs hostiles para la narrativa del Acto 3
        limpiarMobsHostilesCercanos();
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 1: DESCENSO AL VACÍO (0-8 segundos)
        // ═══════════════════════════════════════════════════════════════
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participantesOriginales.contains(p.getUniqueId())) {
                // Fadeout con sensación de caída
                p.sendTitle("§0▬▬▬▬▬▬▬▬▬▬▬▬", "§5§oDescendiendo al vacío...", 20, 80, 20);
                
                // Efectos de vértigo
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.NAUSEA, 80, 0, true, false));
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.BLINDNESS, 80, 0, true, false));
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOWNESS, 160, 4, true, false));
                
                // Sonidos de caída
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BREATH, 1.0f, 0.5f);
                p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.8f, 0.4f);
            }
        }
        
        // Sonidos ambientales de descenso
        playSoundToAll(Sound.ENTITY_WITHER_DEATH, 0.7f, 0.3f);
        playSoundToAll(Sound.ENTITY_ENDER_DRAGON_DEATH, 0.6f, 0.4f);
        playSoundToAll(Sound.AMBIENT_CAVE, 1.0f, 0.6f);
        
        // Partículas de distorsión
        for (Player p : Bukkit.getOnlinePlayers()) {
            Location pLoc = p.getLocation();
            for (int i = 0; i < 150; i++) {
                double ox = (Math.random() - 0.5) * 20;
                double oy = Math.random() * 10;
                double oz = (Math.random() - 0.5) * 20;
                pLoc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, pLoc.clone().add(ox, oy, oz), 3, 0.2, 0.2, 0.2, 0.1);
                pLoc.getWorld().spawnParticle(Particle.SQUID_INK, pLoc.clone().add(ox, oy, oz), 1, 0, 0, 0, 0);
            }
        }
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 2: HEARTBEAT DIMENSIONAL (8-14 segundos)
        // ═══════════════════════════════════════════════════════════════
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            // Pulso 1 - débil
            playSoundToAll(Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.5f);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle("§5✦", "§8§o...el vacío respira...", 5, 25, 5);
                Location loc = p.getLocation();
                loc.getWorld().spawnParticle(Particle.SOUL, loc.add(0, 1, 0), 20, 2, 1, 2, 0.05);
            }
        }, 160L);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            // Pulso 2 - medio
            playSoundToAll(Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.6f);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle("§d§l✦", "§5§o...algo toma forma...", 5, 25, 5);
                Location loc = p.getLocation();
                loc.getWorld().spawnParticle(Particle.END_ROD, loc.add(0, 2, 0), 40, 3, 2, 3, 0.08);
            }
        }, 220L);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            // Pulso 3 - fuerte
            playSoundToAll(Sound.ENTITY_WARDEN_HEARTBEAT, 1.3f, 0.7f);
            playSoundToAll(Sound.ENTITY_WARDEN_ROAR, 0.6f, 0.5f);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle("§b§l❖", "§d§oEL NÚCLEO SE MATERIALIZA", 10, 40, 10);
                Location loc = p.getLocation();
                loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc.add(0, 1.5, 0), 60, 4, 2, 4, 0.1);
            }
        }, 280L);
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 3: REVELACIÓN DEL ACTO FINAL (14-20 segundos)
        // ═══════════════════════════════════════════════════════════════
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            actoActual = Acto.NUCLEO_FORMA;
            ticksEnActo = 0;
            
            // Intensificar ambiente al máximo
            intensificarAmbienteActo3();
            iniciarBreadcrumbs();
            
            // TÍTULO ÉPICO FINAL - más largo
            enviarTituloCinematicoTodos(
                "§d§l✦ ACTO FINAL ✦",
                "§8✦ §5§lEL ECO RESUENA §8✦",
                140
            );
            
            // Todos los sonidos épicos
            playSoundToAll(Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.4f);
            playSoundToAll(Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.5f);
            playSoundToAll(Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.7f);
            playSoundToAll(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 0.5f);
            playSoundToAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 0.8f);
            
            // Explosión de partículas épicas
            for (Player p : Bukkit.getOnlinePlayers()) {
                Location loc = p.getLocation();
                loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.add(0, 2, 0), 100, 5, 3, 5, 0.15);
                loc.getWorld().spawnParticle(Particle.END_ROD, loc, 80, 4, 2, 4, 0.1);
                loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 50, 3, 2, 3, 0.5);
                
                // Múltiples relámpagos
                for (int i = 0; i < 5; i++) {
                    final int delay = i * 8;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        loc.getWorld().strikeLightningEffect(loc.clone().add(
                            (Math.random() - 0.5) * 60, 0, (Math.random() - 0.5) * 60));
                    }, delay);
                }
            }
        }, 340L); // 17 segundos
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 4: PANEL DE OBJETIVOS DRAMÁTICO (20-40 segundos)
        // ═══════════════════════════════════════════════════════════════
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            broadcastNarrative("");
            broadcastNarrative("§d§l✦ §8§m════════════════════════════════════════════ §d§l✦");
            playSoundToAll(Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.8f);
        }, 480L); // 24 segundos
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            broadcastNarrative("");
            broadcastNarrative("          §d§l⧗ ACTO FINAL: §5§lEL ECO RESUENA §d§l⧗");
            playSoundToAll(Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.2f);
        }, 560L); // 28 segundos
        
        // Objetivos con MUCHO tiempo de lectura
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            broadcastNarrative("");
            broadcastNarrative("    §8◆ §7El altar pulsa con energía corrupta ancestral");
            playSoundToAll(Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 0.8f);
        }, 660L); // 33 segundos
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            broadcastNarrative("    §e◆ §eUn §lnúcleo de memoria §r§eemerge cerca del altar");
            playSoundToAll(Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.0f);
        }, 760L); // 38 segundos
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            broadcastNarrative("    §b◆ §b§lBusca el BEAM DE LUZ §bvioleta en el cielo");
            playSoundToAll(Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.2f);
        }, 860L); // 43 segundos
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            broadcastNarrative("    §a◆ §a§lUSA TU BRÚJULA §r§a- apunta hacia el núcleo");
            playSoundToAll(Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.4f);
        }, 960L); // 48 segundos
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            broadcastNarrative("    §c◆ §c§lRECOGE el núcleo antes de que sea tarde");
            playSoundToAll(Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.6f);
        }, 1060L); // 53 segundos
        
        // Mensaje del Observador
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            broadcastNarrative("");
            broadcastNarrative("    §5§o\"Este es el momento que la piedra esperaba...\"");
            broadcastNarrative("    §5§o\"No falles ahora, peregrino.\"");
            broadcastNarrative("    §8§o— El Observador");
            playSoundToAll(Sound.ENTITY_WARDEN_AMBIENT, 0.5f, 0.5f);
        }, 1160L); // 58 segundos
        
        // Cierre
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            broadcastNarrative("");
            broadcastNarrative("§d§l✦ §8§m════════════════════════════════════════════ §d§l✦");
            broadcastNarrative("");
            playSoundToAll(Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.8f, 1.0f);
        }, 1260L); // 63 segundos
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 5: DIÁLOGO DE LA FORMA Y SPAWN DEL NÚCLEO (65-80 segundos)
        // ═══════════════════════════════════════════════════════════════
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            // Diálogo épico
            mostrarDialogoForma("ACTO3_INICIO");
            
            // Slow motion épico
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (participantesOriginales.contains(p.getUniqueId())) {
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SLOWNESS, 180, 4, true, false));
                    aplicarSlowMotion(p, 9);
                }
            }
        }, 1300L); // 65 segundos
        
        // Spawn del núcleo con máximo dramatismo
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            spawnearNucleoForma();
            
            // Mensaje de pista
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (participantesOriginales.contains(p.getUniqueId())) {
                        p.sendMessage("");
                        p.sendMessage("§d§l[✦] §e§lPISTA: §7Mira hacia §darriba §7y busca el §brayo de luz violeta");
                        p.sendMessage("§d§l[✦] §7Tu §abrújula mágica §7te guiará hacia el núcleo");
                        p.sendMessage("");
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.5f);
                    }
                }
            }, 100L);
            
            // Diálogo cuando aparece el núcleo
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (isActive()) {
                    mostrarDialogoForma("NUCLEO_APARECE");
                }
            }, 80L);
        }, 1480L); // 74 segundos
    }
    
    private void spawnearNucleoForma() {
        // 🎯 NÚCLEO CERCA DEL ALTAR: Spawnearlo cerca del primer fragmento (5-10 bloques)
        Random rand = new Random();
        double angulo = Math.random() * Math.PI * 2;
        int distancia = 5 + rand.nextInt(6); // 5-10 bloques
        
        // Usar el primer fragmento como punto de referencia si grietaLocation no está disponible
        Location referenciaBase = null;
        if (grietaLocation != null && grietaLocation.getWorld() != null) {
            referenciaBase = grietaLocation;
        } else if (!fragmentosLocations.isEmpty()) {
            referenciaBase = fragmentosLocations.get(0);
        } else {
            // Fallback: usar spawn del mundo
            plugin.getLogger().warning("[SusurroPiedraRota] ⚠️ No hay referencia para núcleo, usando spawn");
            World w = Bukkit.getWorlds().get(0);
            referenciaBase = w.getSpawnLocation();
        }
        
        nucleoLocation = referenciaBase.clone().add(
            Math.cos(angulo) * distancia,
            1.5, // Altura elevada para visibilidad
            Math.sin(angulo) * distancia
        );
        
        // Asegurar que la ubicación sea válida
        nucleoLocation.setY(nucleoLocation.getWorld().getHighestBlockYAt(nucleoLocation) + 1);
        
        plugin.getLogger().info("[SusurroPiedraRota] Núcleo colocado a " + distancia + " bloques del altar en: " + 
            nucleoLocation.getBlockX() + ", " + nucleoLocation.getBlockY() + ", " + nucleoLocation.getBlockZ());
        
        // Distorsión dimensional al formar el núcleo
        efectoDistorsionDimensionalTodos(60);
        
        // Pulso de energía épico
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            efectoPulsoEnergiaTodos();
        }, 30L);
        
        // 🎁 DAR BRÚJULA ESPECIAL A TODOS LOS PARTICIPANTES
        darBrujulaEspecialATodos();
        
        // Crear item frame VISIBLE (no invisible) con glow para que sea más fácil de ver
        nucleoFrame = (ItemFrame) nucleoLocation.getWorld().spawnEntity(
            nucleoLocation,
            EntityType.ITEM_FRAME
        );
        
        nucleoFrame.setVisible(true); // VISIBLE para que se vea
        nucleoFrame.setGlowing(true); // GLOWING para que brille
        nucleoFrame.setFixed(true);
        nucleoFrame.setItem(SusurroPiedraRotaItems.createNucleoForma());
        
        plugin.getLogger().info(String.format(
            "[SusurroPiedraRota] Núcleo de Forma spawneado en: %s",
            locationToString(nucleoLocation)
        ));
        
        // Asignar núcleo como objetivo para todos los jugadores
        for (Player p : Bukkit.getOnlinePlayers()) {
            objetivosPorJugador.put(p.getUniqueId(), nucleoLocation);
        }
        
        // Efectos visuales
        iniciarEfectosNucleo();
        
        // 🎮 NÚCLEO ÉPICO: Spawn con escalado de dificultad por tiempo
        BukkitTask spawnTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (actoActual != Acto.NUCLEO_FORMA || nucleoRecogido || nucleoLocation == null || nucleoLocation.getWorld() == null) {
                return;
            }
            
            // Calcular tiempo en Acto 3 (en segundos)
            int tiempoEnActo3 = ticksEnActo / 20;
            
            Random r = new Random();
            int cantidadCriaturas;
            long intervaloProximo;
            
            // 🔺 MINI-EVENTO: Escalado relajado - muy pocas criaturas
            if (tiempoEnActo3 < 90) {
                // Minuto 0-1.5: Solo 1 criatura cada 15s (muy fácil)
                cantidadCriaturas = 1;
                intervaloProximo = 300L; // 15 segundos
            } else if (tiempoEnActo3 < 180) {
                // Minuto 1.5-3: 1 criatura cada 12s (fácil)
                cantidadCriaturas = 1;
                intervaloProximo = 240L;
            } else {
                // Minuto 3+: 1-2 criaturas cada 12s (medio)
                cantidadCriaturas = 1 + r.nextInt(2);
                intervaloProximo = 240L; // 12 segundos
            }
            
            plugin.getLogger().info(String.format(
                "[SusurroPiedraRota] Núcleo spawneando %d criatura(s) defensiva(s) (Tiempo: %ds)",
                cantidadCriaturas,
                tiempoEnActo3
            ));
            
            // Anuncio visual cada 2 spawns (cada 20 segundos)
            if (r.nextBoolean()) {
                broadcastNarrative("§5⚡ El núcleo invoca defensores...");
            }
            
            // Spawn las criaturas cerca del núcleo
            for (int i = 0; i < cantidadCriaturas; i++) {
                final int delay = 5 + (i * 10); // Pequeño delay entre cada criatura
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (isActive() && actoActual == Acto.NUCLEO_FORMA && !nucleoRecogido && nucleoLocation != null) {
                        Location spawnLoc = encontrarSpawnSeguro(nucleoLocation, 4, 8);
                        if (spawnLoc == null && nucleoLocation != null) {
                            spawnLoc = nucleoLocation.clone().add(0, 1, 0);
                        }
                        if (spawnLoc != null) {
                            spawnearEnUbicacion(spawnLoc);
                        }
                    }
                }, delay);
            }
        }, 200L, 200L); // Primer spawn a los 10 segundos, luego cada 10s (se ajusta dinámicamente)
        
        // Almacenar la tarea para cancelarla cuando se recoja el núcleo
        nucleoSpawnTask = spawnTask;
    }
    
    private void iniciarEfectosNucleo() {
        // 🌟 BEAM DE LUZ GIGANTE Y VISIBLE desde el cielo - Tipo beacon pero de partículas
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (actoActual != Acto.NUCLEO_FORMA || nucleoRecogido || nucleoLocation == null || nucleoLocation.getWorld() == null) {
                return;
            }
            
            World world = nucleoLocation.getWorld();
            int startY = Math.min(world.getMaxHeight() - 10, 250);
            
            // ☀️ BEAM PRINCIPAL - Más visible con FLAME y SOUL_FIRE_FLAME
            for (int y = startY; y > nucleoLocation.getBlockY(); y -= 1) {
                // Partículas principales violetas brillantes
                world.spawnParticle(
                    Particle.DRAGON_BREATH, // Morado/violeta
                    nucleoLocation.getX(),
                    y,
                    nucleoLocation.getZ(),
                    3,
                    0.2, 0, 0.2,
                    0.01
                );
                
                // Añadir resplandor con END_ROD
                if (y % 2 == 0) {
                    world.spawnParticle(
                        Particle.END_ROD,
                        nucleoLocation.getX(),
                        y,
                        nucleoLocation.getZ(),
                        2,
                        0.15, 0, 0.15,
                        0
                    );
                }
                
                // Fuego de alma cada 3 bloques para intensidad
                if (y % 3 == 0) {
                    world.spawnParticle(
                        Particle.SOUL_FIRE_FLAME,
                        nucleoLocation.getX(),
                        y,
                        nucleoLocation.getZ(),
                        5,
                        0.4, 0, 0.4,
                        0.02
                    );
                }
            }
            
            // 🔆 Resplandor grande en el núcleo
            world.spawnParticle(
                Particle.GLOW,
                nucleoLocation.clone().add(0, 0.5, 0),
                15,
                2, 2, 2,
                0.1
            );
            
        }, 0L, 5L); // Cada 0.25 segundos para fluidez
        
        // 📍 Actualizar brújula de todos los jugadores para que apunte al núcleo
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (actoActual != Acto.NUCLEO_FORMA || nucleoRecogido || nucleoLocation == null || nucleoLocation.getWorld() == null) {
                return;
            }
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (participantesOriginales.contains(p.getUniqueId())) {
                    // Actualizar brújula
                    p.setCompassTarget(nucleoLocation);
                    
                    // Mostrar distancia cada 5 segundos
                    if (ticksEnActo % 100 == 0) {
                        double distancia = p.getLocation().distance(nucleoLocation);
                        String direccion = obtenerDireccionRelativa(p.getLocation(), nucleoLocation);
                        
                        p.sendMessage("§8[§d⧖§8] §7El núcleo está a §e" + Math.round(distancia) + " bloques §7hacia el §e" + direccion);
                        
                        // Sonido direccional
                        if (distancia < 50) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.8f);
                        }
                    }
                }
            }
        }, 0L, 20L); // Cada segundo
        
        // Partículas cinematográficas épicas con múltiples capas AAA + latidos sincronizados
        nucleoParticleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (actoActual != Acto.NUCLEO_FORMA || nucleoRecogido || nucleoLocation == null || nucleoLocation.getWorld() == null) {
                return;
            }
            
            double tiempo = System.currentTimeMillis() / 500.0;
            Location center = nucleoLocation.clone();
            
            // Sistema de latidos sincronizados
            latidoContador++;
            if (latidoContador >= 30) { // Cada 1.5 segundos (thump-thump)
                latidoContador = 0;
                
                // Pulso de energía expansivo
                nucleoLocation.getWorld().spawnParticle(
                    Particle.SOUL,
                    nucleoLocation.clone().add(0, 0.5, 0),
                    30,
                    1.2, 1.2, 1.2,
                    0.18
                );
                
                // Doble latido (como corazón)
                soundUtil.playSound(nucleoLocation, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 1.2f);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (actoActual == Acto.NUCLEO_FORMA && !nucleoRecogido) {
                        soundUtil.playSound(nucleoLocation, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 1.1f);
                    }
                }, 5L);
            }
            
            // 1. Órbitas planetarias dinámicas (3 anillos en planos diferentes)
            for (int anillo = 0; anillo < 3; anillo++) {
                double radioAnillo = 1.2 + anillo * 0.6;
                double velocidad = 1.2 + anillo * 0.4;
                double inclinacion = Math.toRadians(60 * anillo); // Planos inclinados
                
                for (int i = 0; i < 10; i++) {
                    double angle = (tiempo * velocidad + i * Math.PI / 5) % (Math.PI * 2);
                    double x = Math.cos(angle) * radioAnillo;
                    double z = Math.sin(angle) * radioAnillo * Math.cos(inclinacion);
                    double y = Math.sin(angle) * radioAnillo * Math.sin(inclinacion) + Math.sin(tiempo * 2) * 0.2;
                    
                    Particle particula = anillo == 0 ? Particle.END_ROD : 
                                        anillo == 1 ? Particle.REVERSE_PORTAL :
                                        Particle.SOUL_FIRE_FLAME;
                    
                    center.getWorld().spawnParticle(
                        particula,
                        center.clone().add(x, y, z),
                        1,
                        0, 0, 0,
                        0
                    );
                }
            }
            
            // 2. Campo de fuerza esférico visible (barrera protectora GLOW)
            if (ticksEnActo % 8 == 0) {
                double radioEsfera = 3.5;
                for (int i = 0; i < 25; i++) {
                    double theta = Math.random() * Math.PI * 2;
                    double phi = Math.random() * Math.PI;
                    
                    double x = radioEsfera * Math.sin(phi) * Math.cos(theta);
                    double y = radioEsfera * Math.cos(phi) + 0.5;
                    double z = radioEsfera * Math.sin(phi) * Math.sin(theta);
                    
                    center.getWorld().spawnParticle(
                        Particle.GLOW,
                        center.clone().add(x, y, z),
                        1,
                        0, 0, 0,
                        0
                    );
                }
            }
            
            // 3. Relámpagos conectando núcleo con el suelo (cada 5 segundos)
            if (System.currentTimeMillis() % 5000 < 100) {
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI * 2 / 4) * i + (Math.random() * 0.4);
                    double radio = 2.5 + (Math.random() * 1.5);
                    double offsetX = Math.cos(angle) * radio;
                    double offsetZ = Math.sin(angle) * radio;
                    
                    Location puntoSuelo = center.clone().add(offsetX, -1.5, offsetZ);
                    Location puntoNucleo = center.clone().add(0, 1.5, 0);
                    
                    // Línea de relámpago
                    Vector direccion = puntoSuelo.toVector().subtract(puntoNucleo.toVector()).normalize();
                    double distancia = puntoNucleo.distance(puntoSuelo);
                    
                    for (double d = 0; d < distancia; d += 0.25) {
                        Location particleLoc = puntoNucleo.clone().add(direccion.clone().multiply(d));
                        particleLoc.getWorld().spawnParticle(
                            Particle.ELECTRIC_SPARK,
                            particleLoc,
                            2,
                            0.08, 0.08, 0.08,
                            0.03
                        );
                    }
                }
                soundUtil.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 1.6f);
            }
            
            // 4. Relámpagos de energía hacia el cielo
            if (System.currentTimeMillis() % 2000 < 100) {
                for (int i = 0; i < 5; i++) {
                    double offsetX = (Math.random() - 0.5) * 0.3;
                    double offsetZ = (Math.random() - 0.5) * 0.3;
                    for (int y = 0; y < 15; y++) {
                        center.getWorld().spawnParticle(
                            Particle.ELECTRIC_SPARK,
                            center.clone().add(offsetX, y * 0.5, offsetZ),
                            2,
                            0.1, 0, 0.1,
                            0.02
                        );
                    }
                }
                soundUtil.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 1.8f);
            }
            
            // Distorsión del aire
            for (int i = 0; i < 3; i++) {
                double offsetX = (Math.random() - 0.5) * 3;
                double offsetY = Math.random() * 2;
                double offsetZ = (Math.random() - 0.5) * 3;
                
                nucleoLocation.getWorld().spawnParticle(
                    Particle.WARPED_SPORE,
                    nucleoLocation.clone().add(offsetX, offsetY, offsetZ),
                    1,
                    0.1, 0.1, 0.1,
                    0.01
                );
            }
        }, 0L, 2L);
        
        // Beam multicolor épico más grueso
        nucleoBeamTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (actoActual != Acto.NUCLEO_FORMA || nucleoRecogido) {
                return;
            }
            
            for (int y = 0; y < 80; y++) {
                Location beamLoc = nucleoLocation.clone().add(0, y, 0);
                
                nucleoLocation.getWorld().spawnParticle(
                    Particle.END_ROD,
                    beamLoc,
                    2,
                    0.2, 0, 0.2,
                    0
                );
                
                if (y % 3 == 0) {
                    nucleoLocation.getWorld().spawnParticle(
                        Particle.REVERSE_PORTAL,
                        beamLoc,
                        5,
                        0.4, 0, 0.4,
                        0.02
                    );
                }
                
                if (y % 5 == 0) {
                    nucleoLocation.getWorld().spawnParticle(
                        Particle.SOUL,
                        beamLoc,
                        3,
                        0.3, 0, 0.3,
                        0.01
                    );
                }
            }
        }, 0L, 10L);
    }
    
    private void verificarProximidadNucleo() {
        if (ticksEnActo % 5 != 0) return; // Verificar cada 0.25s
        if (nucleoLocation == null) return; // Validación null safety
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            double distancia = player.getLocation().distance(nucleoLocation);
            
            // 🗣️ DIÁLOGO - Cerca del núcleo (10-15 bloques)
            if (distancia < 15 && distancia > 10 && ticksEnActo % 100 == 0) {
                mostrarDialogoForma("CERCA_NUCLEO");
            }
            
            if (distancia < 2.0) {
                recogerNucleo(player);
                break;
            }
        }
    }
    
    private void recogerNucleo(Player player) {
        if (nucleoRecogido) return;
        
        nucleoRecogido = true;
        jugadorQueRecogio = player.getUniqueId();
        
        // Registrar puzzle del laberinto completado
        puzzlesCompletados.put(player.getUniqueId(), puzzlesCompletados.getOrDefault(player.getUniqueId(), 0) + 1);
        
        plugin.getLogger().info(String.format(
            "[SusurroPiedraRota] %s recogió el Núcleo de Forma",
            player.getName()
        ));
        
        // Dar item al jugador
        player.getInventory().addItem(SusurroPiedraRotaItems.createNucleoForma());
        
        // Mensaje
        enviarMensajeCinematico(player, "§5§l✦ NÚCLEO DE FORMA RECUPERADO ✦");
        
        soundUtil.playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
        
        // Efectos
        nucleoLocation.getWorld().spawnParticle(
            Particle.END_ROD,
            nucleoLocation,
            100,
            1, 1, 1,
            0.2
        );
        
        // Remover item frame
        if (nucleoFrame != null && nucleoFrame.isValid()) {
            nucleoFrame.remove();
        }
        
        // Cancelar efectos del núcleo en el vacío
        if (nucleoParticleTask != null) nucleoParticleTask.cancel();
        if (nucleoBeamTask != null) nucleoBeamTask.cancel();
        if (nucleoSpawnTask != null) nucleoSpawnTask.cancel();
        
        // 🗣️ DIÁLOGO - Núcleo recogido, instrucción de llevarlo al altar
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                // Establecer ubicación del altar (primer fragmento) con verificación null safety
                if (!fragmentosLocations.isEmpty()) {
                    altarLocation = fragmentosLocations.get(0).clone();
                } else if (grietaLocation != null) {
                    altarLocation = grietaLocation.clone();
                } else {
                    // Fallback: usar spawn del mundo
                    altarLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
                    plugin.getLogger().warning("[SusurroPiedraRota] No hay fragmentos para altar, usando spawn");
                }
                
                // ⚔️ PRIMERO: Spawnear enemigos para que los jugadores los maten
                spawnearOleadaActo3();
                
                // Mensaje de alerta de combate
                broadcastNarrative("");
                broadcastNarrative("    §c§l⚔ ¡DEFENSORES DEL NÚCLEO! ⚔");
                broadcastNarrative("    §7Eliminen a las criaturas antes de continuar...");
                playSoundToAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 0.6f);
                
                // Verificar cuando se eliminen todos los enemigos para continuar narrativa
                esperarEliminacionEnemigosActo3(() -> {
                    if (!isActive()) return;
                    
                    mostrarDialogoForma("NUCLEO_RECOGIDO");
                    
                    // Anuncio en chat con instrucciones claras
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!isActive()) return;
                        broadcastNarrative("§8§m══════════════════════════════════════════════════");
                        broadcastNarrative("");
                        broadcastNarrative("          §5§l⚠ ¡LLEVEN EL NÚCLEO AL ALTAR! ⚠");
                        broadcastNarrative("");
                        broadcastNarrative("    §e✦ El núcleo aún vive y debe ser destruido");
                        broadcastNarrative("    §a✦ Regresen al PRIMER FRAGMENTO (donde empezó todo)");
                        broadcastNarrative("    §c✦ ¡El núcleo invocará defensores mientras huyen!");
                        broadcastNarrative("");
                        broadcastNarrative("§8§m══════════════════════════════════════════════════");
                        
                        // Actualizar brújulas para que apunten al altar
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (participantesOriginales.contains(p.getUniqueId())) {
                                p.setCompassTarget(altarLocation);
                                objetivosPorJugador.put(p.getUniqueId(), altarLocation);
                            }
                        }
                        
                        // Spawn agresivo de criaturas durante el retorno (más presión)
                        iniciarSpawnsRetorno();
                    }, 100L); // 5 segundos después del diálogo
                });
            }
        }, 60L); // 3 segundos para ver efectos
    }
    
    private void iniciarBreadcrumbs() {
        // Inicializar tracking para cada jugador
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participantesOriginales.contains(p.getUniqueId())) {
                breadcrumbsPorJugador.put(p.getUniqueId(), new ArrayList<>());
            }
        }
        
        // Task para actualizar breadcrumbs cada segundo
        breadcrumbsTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (actoActual != Acto.NUCLEO_FORMA) {
                return;
            }
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID uuid = p.getUniqueId();
                if (!breadcrumbsPorJugador.containsKey(uuid)) continue;
                
                List<Location> ruta = breadcrumbsPorJugador.get(uuid);
                Location actual = p.getLocation();
                
                // Agregar ubicación actual si está a más de 2 bloques de la última
                if (ruta.isEmpty() || ruta.get(ruta.size() - 1).distance(actual) > 2.0) {
                    ruta.add(actual.clone());
                    
                    // Limitar a últimas 100 ubicaciones para performance
                    if (ruta.size() > 100) {
                        ruta.remove(0);
                    }
                }
                
                // Mostrar últimas 50 ubicaciones con partículas VILLAGER_HAPPY
                int start = Math.max(0, ruta.size() - 50);
                for (int i = start; i < ruta.size(); i++) {
                    Location loc = ruta.get(i);
                    
                    // Fade effect: partículas más viejas son más tenues
                    int age = ruta.size() - i;
                    if (age % 3 == 0) { // Reducir densidad para performance
                        p.spawnParticle(
                            Particle.HAPPY_VILLAGER,
                            loc.clone().add(0, 0.1, 0),
                            1,
                            0, 0, 0,
                            0
                        );
                    }
                }
            }
        }, 0L, 20L); // Cada segundo
    }
    
    private void tickActo3() {
        // Verificar proximidad al altar si el núcleo fue recogido
        if (nucleoRecogido && !ritualDestruccionIniciado) {
            verificarProximidadAltar();
        }
        
        // Procesar ritual de destrucción si está activo
        if (ritualDestruccionIniciado && !ritualDestruccionCompletado) {
            procesarRitualDestruccion();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // SISTEMA DE OLEADAS ACTO 3 - MATAR ANTES DE NARRATIVA
    // ═══════════════════════════════════════════════════════════════
    
    private Set<UUID> enemigosOleadaActo3 = new HashSet<>();
    private Runnable callbackOleadaCompletada = null;
    
    /**
     * Spawnea una oleada de enemigos que deben ser eliminados antes de continuar
     */
    private void spawnearOleadaActo3() {
        enemigosOleadaActo3.clear();
        
        // Contar jugadores vivos
        int jugadoresVivos = 0;
        Location spawnRef = nucleoLocation != null ? nucleoLocation : altarLocation;
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participantesOriginales.contains(p.getUniqueId()) && 
                p.getGameMode() == org.bukkit.GameMode.SURVIVAL) {
                jugadoresVivos++;
                if (spawnRef == null) spawnRef = p.getLocation();
            }
        }
        if (jugadoresVivos == 0 || spawnRef == null) return;
        
        // Spawnear 4-6 enemigos según jugadores
        int cantidad = 4 + Math.min(jugadoresVivos, 3);
        Random rand = new Random();
        
        for (int i = 0; i < cantidad; i++) {
            double angulo = (2 * Math.PI * i) / cantidad;
            Location spawnLoc = spawnRef.clone().add(
                Math.cos(angulo) * (6 + rand.nextInt(4)),
                0,
                Math.sin(angulo) * (6 + rand.nextInt(4))
            );
            spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);
            
            // Spawnear criatura y trackear
            LivingEntity criatura = spawnearCriaturaTrackeada(spawnLoc, rand);
            if (criatura != null) {
                enemigosOleadaActo3.add(criatura.getUniqueId());
                // Efecto visual de spawn
                spawnLoc.getWorld().spawnParticle(Particle.SOUL, spawnLoc, 15, 0.5, 0.5, 0.5, 0.1);
            }
        }
        
        plugin.getLogger().info("[SusurroPiedraRota] Oleada Acto 3: " + enemigosOleadaActo3.size() + " enemigos spawneados");
    }
    
    /**
     * Spawnea una criatura y la retorna para tracking
     */
    private LivingEntity spawnearCriaturaTrackeada(Location loc, Random rand) {
        EntityType[] tipos = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.WITCH, EntityType.VINDICATOR};
        EntityType tipo = tipos[rand.nextInt(tipos.length)];
        
        org.bukkit.entity.Entity entity = loc.getWorld().spawnEntity(loc, tipo);
        if (!(entity instanceof LivingEntity)) {
            return null;
        }
        LivingEntity criatura = (LivingEntity) entity;
        criatura.setCustomName("§5Defensor del Núcleo");
        criatura.setCustomNameVisible(true);
        
        // Dar XP al morir
        criatura.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "evento_xp"),
            org.bukkit.persistence.PersistentDataType.INTEGER,
            60 + rand.nextInt(30) // 60-90 XP
        );
        
        criaturasActivas.add(criatura);
        return criatura;
    }
    
    /**
     * Espera a que se eliminen todos los enemigos de la oleada para ejecutar callback
     */
    private void esperarEliminacionEnemigosActo3(Runnable callback) {
        this.callbackOleadaCompletada = callback;
        
        // Verificar cada segundo
        Bukkit.getScheduler().runTaskTimer(plugin, new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) {
                    this.cancel();
                    return;
                }
                
                // Limpiar enemigos muertos del set
                enemigosOleadaActo3.removeIf(uuid -> {
                    for (Entity e : criaturasActivas) {
                        if (e.getUniqueId().equals(uuid) && !e.isDead()) {
                            return false;
                        }
                    }
                    return true;
                });
                
                // Mostrar progreso
                if (!enemigosOleadaActo3.isEmpty() && System.currentTimeMillis() % 5000 < 1000) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (participantesOriginales.contains(p.getUniqueId())) {
                            p.sendActionBar(net.kyori.adventure.text.Component.text(
                                "§c⚔ Enemigos restantes: §l" + enemigosOleadaActo3.size() + " §c⚔"
                            ));
                        }
                    }
                }
                
                // Si no quedan enemigos, ejecutar callback
                if (enemigosOleadaActo3.isEmpty()) {
                    this.cancel();
                    
                    // Mensaje de victoria
                    broadcastNarrative("");
                    broadcastNarrative("    §a§l✓ ¡DEFENSORES ELIMINADOS! §a✓");
                    playSoundToAll(Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                    playSoundToAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
                    
                    // Ejecutar callback después de pequeña pausa
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (callbackOleadaCompletada != null) {
                            callbackOleadaCompletada.run();
                            callbackOleadaCompletada = null;
                        }
                    }, 40L); // 2 segundos
                }
            }
        }, 20L, 20L);
    }
    
    /**
     * Llamado cuando un enemigo de la oleada muere
     */
    public void procesarMuerteEnemigoOleadaActo3(UUID enemigoUUID) {
        enemigosOleadaActo3.remove(enemigoUUID);
    }
    
    private void iniciarSpawnsRetorno() {
        if (retornoSpawnTask != null) {
            retornoSpawnTask.cancel();
        }
        
        retornoSpawnTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!nucleoRecogido || ritualDestruccionCompletado) {
                if (retornoSpawnTask != null) {
                    retornoSpawnTask.cancel();
                    retornoSpawnTask = null;
                }
                return;
            }
            
            // Buscar jugadores vivos
            List<Player> jugadoresVivos = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (participantesOriginales.contains(p.getUniqueId()) && 
                    p.getGameMode() == org.bukkit.GameMode.SURVIVAL) {
                    jugadoresVivos.add(p);
                }
            }
            if (jugadoresVivos.isEmpty()) return;
            
            // Spawnear 2-3 criaturas agresivas cerca de jugadores aleatorios
            Random rand = new Random();
            int cantidad = 2 + rand.nextInt(2);
            for (int i = 0; i < cantidad; i++) {
                Player objetivo = jugadoresVivos.get(rand.nextInt(jugadoresVivos.size()));
                Location referenciaSpawn = objetivo.getLocation();
                Location spawnLoc = encontrarSpawnSeguro(referenciaSpawn, 4, 8);
                if (spawnLoc == null) {
                    spawnLoc = referenciaSpawn.clone().add(rand.nextDouble() * 8 - 4, 0, rand.nextDouble() * 8 - 4);
                    spawnLoc.setY(referenciaSpawn.getWorld().getHighestBlockYAt(spawnLoc));
                }
                spawnearEnUbicacion(spawnLoc);
            }
        }, 0L, 100L); // Cada 5 segundos
    }
    
    private void verificarProximidadAltar() {
        if (altarLocation == null) return;
        
        // Contar jugadores vivos y verificar proximidad
        List<Player> jugadoresVivos = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participantesOriginales.contains(p.getUniqueId()) && 
                p.getGameMode() == org.bukkit.GameMode.SURVIVAL) {
                jugadoresVivos.add(p);
            }
        }
        if (jugadoresVivos.isEmpty()) return;
        
        boolean todosProximos = true;
        for (Player p : jugadoresVivos) {
            if (p.getLocation().distance(altarLocation) > 15.0) {
                todosProximos = false;
                break;
            }
        }
        
        if (todosProximos) {
            iniciarRitualDestruccion();
        } else {
            // Recordatorio cada 5 segundos
            if (ticksEnActo % 100 == 0) {
                for (Player p : jugadoresVivos) {
                    p.sendMessage(ChatColor.GOLD + "⚠ " + ChatColor.YELLOW + "Regresad al altar con el núcleo para destruirlo");
                }
            }
        }
    }
    
    private void iniciarRitualDestruccion() {
        ritualDestruccionIniciado = true;
        ticksRitualDestruccion = 0;
        
        // Cancelar spawns de retorno
        if (retornoSpawnTask != null) {
            retornoSpawnTask.cancel();
            retornoSpawnTask = null;
        }
        
        // Crear pedestal visual (ItemFrame con el núcleo)
        Location pedestalLoc = altarLocation.clone().add(0, 1.5, 0);
        pedestalNucleo = altarLocation.getWorld().spawn(pedestalLoc, ItemFrame.class);
        pedestalNucleo.setItem(new ItemStack(Material.HEART_OF_THE_SEA));
        pedestalNucleo.setVisible(false);
        pedestalNucleo.setFixed(true);
        pedestalNucleo.setInvulnerable(true);
        
        // ⚔️ PRIMERO: Spawnear defensores finales que deben ser eliminados
        int jugadoresVivos = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participantesOriginales.contains(p.getUniqueId()) && 
                p.getGameMode() == org.bukkit.GameMode.SURVIVAL) {
                jugadoresVivos++;
            }
        }
        if (jugadoresVivos == 0) jugadoresVivos = 1;
        
        // Título de alerta de combate
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participantesOriginales.contains(p.getUniqueId())) {
                p.sendTitle(
                    ChatColor.DARK_RED + "⚔ ¡ÚLTIMA DEFENSA! ⚔",
                    ChatColor.GRAY + "Eliminen a los defensores del núcleo",
                    10, 80, 20
                );
            }
        }
        
        // Spawnear defensores finales (Forma intenta impedir el ritual)
        enemigosOleadaActo3.clear();
        int cantidadDefensores = 6 + jugadoresVivos * 2;
        Random rand = new Random();
        
        for (int i = 0; i < cantidadDefensores; i++) {
            double angulo = (2 * Math.PI * i) / cantidadDefensores;
            Location spawnLoc = altarLocation.clone().add(
                Math.cos(angulo) * 12,
                0,
                Math.sin(angulo) * 12
            );
            spawnLoc.setY(altarLocation.getWorld().getHighestBlockYAt(spawnLoc) + 1);
            
            LivingEntity criatura = spawnearCriaturaTrackeada(spawnLoc, rand);
            if (criatura != null) {
                enemigosOleadaActo3.add(criatura.getUniqueId());
                criatura.setCustomName("§4Guardián Final");
            }
        }
        
        playSoundToAll(Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.5f);
        broadcastNarrative("");
        broadcastNarrative("    §c§l⚔ ¡GUARDIANES FINALES INVOCADOS! ⚔");
        broadcastNarrative("    §7Elimínenlos para comenzar el ritual de destrucción...");
        
        // Esperar a que maten a todos para continuar con el ritual
        esperarEliminacionEnemigosActo3(() -> {
            if (!isActive()) return;
            
            // AHORA sí mostrar título y diálogo del ritual
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (participantesOriginales.contains(p.getUniqueId())) {
                    p.sendTitle(
                        ChatColor.DARK_PURPLE + "✦ RITUAL DE DESTRUCCIÓN ✦",
                        ChatColor.GRAY + "Permanezcan unidos 10 segundos",
                        10, 80, 20
                    );
                }
            }
            
            // Diálogo del Observador
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (isActive()) {
                    mostrarDialogoForma("RITUAL_DESTRUCCION");
                }
            }, 20L);
            
            // Iniciar efectos visuales y el ritual propiamente
            iniciarEfectosRitual();
        });
    }
    
    private void procesarRitualDestruccion() {
        ticksRitualDestruccion++;
        
        // Buscar jugadores vivos
        List<Player> jugadoresVivos = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participantesOriginales.contains(p.getUniqueId()) && 
                p.getGameMode() == org.bukkit.GameMode.SURVIVAL) {
                jugadoresVivos.add(p);
            }
        }
        
        if (jugadoresVivos.isEmpty()) {
            interrumpirRitual();
            return;
        }
        
        // Verificar que todos los jugadores vivos estén cerca del altar (12 bloques)
        boolean todosProximos = true;
        for (Player p : jugadoresVivos) {
            if (p.getLocation().distance(altarLocation) > 12.0) {
                todosProximos = false;
                break;
            }
        }
        
        if (!todosProximos) {
            interrumpirRitual();
            return;
        }
        
        // Actualizar ActionBar con cuenta regresiva
        int segundosRestantes = 10 - (ticksRitualDestruccion / 20);
        if (segundosRestantes >= 0) {
            String barra = ChatColor.DARK_RED + "⚔ " + ChatColor.RED + "Ritual: " 
                + ChatColor.GOLD + segundosRestantes + "s " 
                + ChatColor.DARK_RED + "⚔";
            for (Player p : jugadoresVivos) {
                p.sendActionBar(barra);
            }
        }
        
        // Completar ritual después de 10 segundos (200 ticks)
        if (ticksRitualDestruccion >= 200) {
            completarRitualDestruccion();
        }
    }
    
    private void interrumpirRitual() {
        ritualDestruccionIniciado = false;
        ticksRitualDestruccion = 0;
        
        // Remover pedestal
        if (pedestalNucleo != null && !pedestalNucleo.isDead()) {
            pedestalNucleo.remove();
            pedestalNucleo = null;
        }
        
        // Mensaje de fallo
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participantesOriginales.contains(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "✖ El ritual ha sido interrumpido. ¡Reagrupaos!");
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.5f);
            }
        }
        
        // Permitir reintento después de 3 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive() && nucleoRecogido && !ritualDestruccionCompletado) {
                iniciarSpawnsRetorno(); // Reiniciar spawns de presión
            }
        }, 60L);
    }
    
    private void completarRitualDestruccion() {
        ritualDestruccionCompletado = true;
        
        // Remover pedestal
        if (pedestalNucleo != null && !pedestalNucleo.isDead()) {
            pedestalNucleo.remove();
            pedestalNucleo = null;
        }
        
        // Matar todas las Formas restantes
        for (Entity ent : altarLocation.getWorld().getEntities()) {
            if (ent.getScoreboardTags().contains("forma_susurro")) {
                ent.remove();
            }
        }
        
        // Buscar jugadores
        List<Player> jugadores = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participantesOriginales.contains(p.getUniqueId()) && 
                p.getGameMode() == org.bukkit.GameMode.SURVIVAL) {
                jugadores.add(p);
            }
        }
        
        // Efecto de slow motion (ralentización temporal)
        for (Player p : jugadores) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 2, false, false));
        }
        
        // Título épico
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participantesOriginales.contains(p.getUniqueId())) {
                p.sendTitle(
                    ChatColor.LIGHT_PURPLE + "☄ EL NÚCLEO SE DESINTEGRA ☄",
                    "",
                    10, 60, 20
                );
            }
        }
        
        // Explosión visual en 5 oleadas expandiendo
        for (int ola = 1; ola <= 5; ola++) {
            final int olaActual = ola;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!isActive()) return;
                
                double radio = olaActual * 3.0;
                int particulas = olaActual * 50;
                
                for (int i = 0; i < particulas; i++) {
                    double angulo = (2 * Math.PI * i) / particulas;
                    Location particleLoc = altarLocation.clone().add(
                        Math.cos(angulo) * radio,
                        1.5 + (olaActual * 0.5),
                        Math.sin(angulo) * radio
                    );
                    
                    altarLocation.getWorld().spawnParticle(
                        Particle.SOUL,
                        particleLoc,
                        10,
                        0.5, 0.5, 0.5,
                        0.05
                    );
                    
                    if (olaActual == 5) {
                        altarLocation.getWorld().spawnParticle(
                            Particle.REVERSE_PORTAL,
                            particleLoc,
                            5,
                            0.3, 0.3, 0.3,
                            0.1
                        );
                    }
                }
                
                // Sonidos en oleadas
                for (Player p : jugadores) {
                    if (olaActual == 1) {
                        p.playSound(altarLocation, Sound.ENTITY_WITHER_DEATH, 1.0f, 0.6f);
                    } else if (olaActual == 3) {
                        p.playSound(altarLocation, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.8f, 1.2f);
                    } else if (olaActual == 5) {
                        p.playSound(altarLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);
                        p.playSound(altarLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
                    }
                }
            }, ola * 10L);
        }
        
        // Flash final de partículas blancas
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            altarLocation.getWorld().spawnParticle(
                Particle.FLASH,
                altarLocation.clone().add(0, 1.5, 0),
                1
            );
            
            altarLocation.getWorld().spawnParticle(
                Particle.END_ROD,
                altarLocation.clone().add(0, 1.5, 0),
                200,
                0, 0, 0,
                0.5
            );
        }, 55L);
        
        // Diálogo final del Observador
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                mostrarDialogoForma("NUCLEO_DESTRUIDO");
            }
        }, 80L);
        
        // Completar Acto 3
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                completarActo3();
            }
        }, 160L);
    }
    
    private void iniciarEfectosRitual() {
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (!ritualDestruccionIniciado || ritualDestruccionCompletado) {
                    return;
                }
                
                ticks++;
                
                // Anillos de partículas convergiendo hacia el núcleo
                double radio = 6.0 - (ticks % 60) * 0.1;
                if (radio > 1.0) {
                    for (int i = 0; i < 30; i++) {
                        double angulo = (2 * Math.PI * i) / 30;
                        Location particleLoc = altarLocation.clone().add(
                            Math.cos(angulo) * radio,
                            1.5,
                            Math.sin(angulo) * radio
                        );
                        
                        altarLocation.getWorld().spawnParticle(
                            Particle.SOUL_FIRE_FLAME,
                            particleLoc,
                            1,
                            0, 0, 0,
                            0
                        );
                    }
                }
                
                // Partículas ascendentes
                Random rand = new Random();
                for (int i = 0; i < 5; i++) {
                    Location particleLoc = altarLocation.clone().add(
                        rand.nextDouble() * 4 - 2,
                        rand.nextDouble() * 2,
                        rand.nextDouble() * 4 - 2
                    );
                    
                    altarLocation.getWorld().spawnParticle(
                        Particle.END_ROD,
                        particleLoc,
                        1,
                        0, 0.5, 0,
                        0.02
                    );
                }
                
                // Pulsación del núcleo cada 30 ticks
                if (ticks % 30 == 0) {
                    altarLocation.getWorld().spawnParticle(
                        Particle.SOUL,
                        altarLocation.clone().add(0, 1.5, 0),
                        20,
                        0.3, 0.3, 0.3,
                        0.05
                    );
                    
                    // Buscar jugadores vivos para el sonido
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (participantesOriginales.contains(p.getUniqueId()) && 
                            p.getGameMode() == org.bukkit.GameMode.SURVIVAL) {
                            p.playSound(altarLocation, Sound.ENTITY_WARDEN_HEARTBEAT, 0.4f, 0.8f);
                        }
                    }
                }
            }
        }, 0L, 3L); // Cada 3 ticks para efectos suaves
    }
    
    private void completarActo3() {
        plugin.getLogger().info("[SusurroPiedraRota] Acto 3 completado");
        
        // Transición al Acto 4
        actoActual = Acto.TRANSICION_4;
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                iniciarActo4();
            }
        }, 40L);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ACTO 4: EL SEGUNDO SUSURRO
    // ═══════════════════════════════════════════════════════════════════
    
    private void iniciarActo4() {
        actoActual = Acto.SEGUNDO_SUSURRO;
        ticksEnActo = 0;
        
        plugin.getLogger().info("[SusurroPiedraRota] Iniciando Acto 4: El Segundo Susurro - ÉPICO");
        
        // ✨ NUEVO: Limpiar mobs hostiles para la narrativa del Acto 4
        limpiarMobsHostilesCercanos();
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 1: DISTORSIÓN TEMPORAL (0-6 segundos)
        // ═══════════════════════════════════════════════════════════════
        
        // Efecto de quiebre de realidad
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participantesOriginales.contains(p.getUniqueId())) {
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.BLINDNESS, 40, 0, true, false));
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOWNESS, 200, 3, true, false));
                p.sendTitle("§0▬▬▬▬▬▬▬▬▬▬", "§8§o...", 10, 40, 10);
                
                // Partículas de fragmentación
                Location loc = p.getLocation();
                loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.add(0, 1, 0), 50, 3, 2, 3, 0.02);
            }
        }
        
        playSoundToAll(Sound.ENTITY_WITHER_BREAK_BLOCK, 1.5f, 0.3f);
        playSoundToAll(Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 1.0f, 0.5f);
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 2: SUSURROS DEL PASADO (6-18 segundos)
        // ═══════════════════════════════════════════════════════════════
        
        // Susurro 1 - más lento y dramático
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (participantesOriginales.contains(p.getUniqueId())) {
                    p.sendTitle("", "§8§o...no aprendieron...", 20, 60, 20);
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.4f, 0.4f);
                    
                    Location loc = p.getLocation();
                    loc.getWorld().spawnParticle(Particle.SMOKE, loc.add(0, 2, 0), 30, 1, 0.5, 1, 0.02);
                }
            }
        }, 120L); // 6 segundos
        
        // Susurro 2
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (participantesOriginales.contains(p.getUniqueId())) {
                    p.sendTitle("", "§5§o...otra vez...", 20, 60, 20);
                    p.playSound(p.getLocation(), Sound.ENTITY_VEX_AMBIENT, 0.5f, 0.5f);
                    
                    Location loc = p.getLocation();
                    loc.getWorld().spawnParticle(Particle.SOUL, loc.add(0, 2, 0), 25, 1, 0.5, 1, 0.03);
                }
            }
        }, 240L); // 12 segundos
        
        // Susurro 3 - más intenso
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (participantesOriginales.contains(p.getUniqueId())) {
                    p.sendTitle("", "§d§o...siempre vuelven...", 20, 80, 20);
                    p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_AMBIENT, 0.5f, 0.5f);
                    
                    Location loc = p.getLocation();
                    loc.getWorld().spawnParticle(Particle.END_ROD, loc.add(0, 2, 0), 35, 2, 1, 2, 0.05);
                }
            }
        }, 360L); // 18 segundos
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 3: LA FIGURA RECUERDA (18-30 segundos)
        // ═══════════════════════════════════════════════════════════════
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            broadcastNarrative("");
            broadcastNarrative("§8§m════════════════════════════════════════════");
            playSoundToAll(Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.5f);
        }, 440L); // 22 segundos
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            broadcastNarrative("");
            broadcastNarrative("§8§o                    ...la figura recuerda...");
            playSoundToAll(Sound.ENTITY_ENDERMAN_STARE, 0.4f, 0.5f);
            
            // Partículas misteriosas
            for (Player p : Bukkit.getOnlinePlayers()) {
                Location loc = p.getLocation();
                loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.add(0, 2, 0), 50, 3, 2, 3, 0.1);
            }
        }, 520L); // 26 segundos
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            broadcastNarrative("");
            broadcastNarrative("§8§m════════════════════════════════════════════");
            broadcastNarrative("");
        }, 600L); // 30 segundos
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 4: PENSAMIENTO DEL OBSERVADOR (30-50 segundos)
        // ═══════════════════════════════════════════════════════════════
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            mostrarPensamientoObservadorEpico();
        }, 600L); // 30 segundos
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 5: CLIFFHANGER ÉPICO (50-65 segundos)
        // ═══════════════════════════════════════════════════════════════
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            mostrarCliffhangerEpico();
        }, 1000L); // 50 segundos
    }
    
    private void mostrarPensamientoObservadorEpico() {
        String[] pensamientos = {
            "§8§o\"La piedra no debería hablar...\"",
            "§8§o\"...pero cuando lo hace, es porque algo antiguo\"",
            "§8§o\"vuelve a tomar forma en las sombras.\"",
            "§5§o\"Y yo... yo estaba allí cuando comenzó.\""
        };
        
        // Oscurecer ambiente
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participantesOriginales.contains(p.getUniqueId())) {
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.BLINDNESS, 20, 0, true, false));
                p.sendTitle("", "§0§l▬", 5, 20, 10);
            }
        }
        
        // Mostrar cada pensamiento con tiempo de lectura extendido
        for (int i = 0; i < pensamientos.length; i++) {
            final int indice = i;
            final String pensamiento = pensamientos[i];
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!isActive()) return;
                
                // Mensaje con formato especial
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (participantesOriginales.contains(p.getUniqueId())) {
                        p.sendMessage("");
                        p.sendMessage(formatearCentrado(pensamiento));
                        
                        // Sonido de susurro progresivo
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 
                            0.2f + (indice * 0.1f), 0.5f + (indice * 0.1f));
                        
                        // Partículas sutiles
                        Location loc = p.getEyeLocation();
                        loc.getWorld().spawnParticle(Particle.SMOKE, loc, 5, 0.3, 0.2, 0.3, 0.01);
                    }
                }
            }, i * 100L); // 5 segundos entre cada línea
        }
        
        // Pausa final antes del cierre
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (participantesOriginales.contains(p.getUniqueId())) {
                    p.sendMessage("");
                    p.sendMessage("    §8§o— El Observador");
                    p.sendMessage("");
                }
            }
            playSoundToAll(Sound.ENTITY_WARDEN_AMBIENT, 0.4f, 0.5f);
        }, pensamientos.length * 100L + 40L);
    }
    
    private void mostrarCliffhangerEpico() {
        // ═══════════════════════════════════════════════════════════════
        // SÍMBOLO EN EL CIELO - Revelación final
        // ═══════════════════════════════════════════════════════════════
        
        // Parpadeo dramático
        for (int flash = 0; flash < 4; flash++) {
            final int f = flash;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!isActive()) return;
                
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (participantesOriginales.contains(p.getUniqueId())) {
                        Location simboloLoc = p.getLocation().clone().add(0, 50, 0);
                        mostrarSimboloMisterioso(simboloLoc);
                        
                        if (f < 2) {
                            p.sendTitle("§5✦", "", 5, 10, 5);
                        } else {
                            p.sendTitle("§d§l✦", "§5§o¿Qué has despertado?", 10, 40, 20);
                        }
                        
                        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 
                            0.5f + (f * 0.2f), 0.8f + (f * 0.1f));
                    }
                }
            }, flash * 30L);
        }
        
        // Sonido de cierre épico
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            playSoundToAll(Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 0.6f);
            playSoundToAll(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 0.5f);
            
            // Relámpago final
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.getWorld().strikeLightningEffect(p.getLocation().clone().add(
                    (Math.random() - 0.5) * 30, 0, (Math.random() - 0.5) * 30));
            }
        }, 120L);
        
        // Mensaje final de transición
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            broadcastNarrative("");
            broadcastNarrative("§8§m════════════════════════════════════════════");
            broadcastNarrative("");
            broadcastNarrative("          §5§lEL SUSURRO... §d§lCONTINÚA...");
            broadcastNarrative("");
            broadcastNarrative("§8§m════════════════════════════════════════════");
            broadcastNarrative("");
            
            playSoundToAll(Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 0.8f);
        }, 180L);
        
        // Completar evento después de la secuencia épica
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                completarEvento();
            }
        }, 300L); // 15 segundos después del cliffhanger
    }

    private void mostrarPensamientoObservador() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            mostrarDialogoProgresivo(player, new String[] {
                "§8§o\"La piedra no debería hablar...\"",
                "§8§o\"...pero cuando lo hace, es porque algo antiguo\"",
                "§8§o\"vuelve a tomar forma en las sombras.\""
            }, 0L);
        }
    }
    
    /**
     * Muestra diálogo con efecto progresivo y typewriter.
     */
    private void mostrarDialogoProgresivo(Player player, String[] lineas, long delayInicial) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            player.sendMessage("");
            
            // Oscurecer ligeramente
            player.sendTitle("", "§0§l▬", 5, 20, 10);
            
            for (int i = 0; i < lineas.length; i++) {
                final int indice = i;
                final String linea = lineas[i];
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // Efecto typewriter por caracteres
                    String lineaLimpia = linea.replaceAll("§.", "");
                    int longitudTotal = lineaLimpia.length();
                    
                    // Mostrar la línea completa de una vez pero con efecto de sonido
                    player.sendMessage(formatearCentrado(linea));
                    
                    // Sonido de susurro
                    soundUtil.playSound(player, Sound.ENTITY_ENDERMAN_STARE, 0.2f, 0.6f + (indice * 0.1f));
                    
                    // Partículas sutiles
                    Location eyeLoc = player.getEyeLocation();
                    eyeLoc.getWorld().spawnParticle(Particle.SMOKE, eyeLoc, 3, 0.2, 0.2, 0.2, 0.01);
                }, i * 60L); // 3 segundos entre líneas
            }
            
            // Línea vacía al final
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage("");
            }, lineas.length * 60L + 20L);
        }, delayInicial);
    }
    
    /**
     * Muestra la presentación del Acto 1 con animación ÉPICA extendida.
     */
    private void mostrarPresentacionActo1(Player player) {
        // ═══════════════════════════════════════════════════════════════
        // FASE 1: Título épico con efectos (0-4 segundos)
        // ═══════════════════════════════════════════════════════════════
        
        // Efecto de parpadeo dimensional
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.BLINDNESS, 20, 0, true, false));
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            // Destello épico con sonidos múltiples
            player.sendTitle("§5§l✦", "", 5, 30, 10);
            soundUtil.playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
            soundUtil.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.8f);
            
            // Partículas épicas
            Location pLoc = player.getLocation();
            pLoc.getWorld().spawnParticle(Particle.END_ROD, pLoc.add(0, 2, 0), 50, 2, 1, 2, 0.05);
            pLoc.getWorld().spawnParticle(Particle.PORTAL, pLoc, 100, 3, 2, 3, 0.5);
        }, 20L);
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 2: Título del Acto (4-8 segundos)
        // ═══════════════════════════════════════════════════════════════
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            player.sendTitle("§d§lACTO I", "§5Los Susurros Aparecen", 20, 80, 20);
            soundUtil.playSound(player, Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.8f, 0.8f);
            soundUtil.playSound(player, Sound.ENTITY_WARDEN_HEARTBEAT, 0.4f, 0.7f);
            
            // Relámpago distante
            player.getWorld().strikeLightningEffect(player.getLocation().clone().add(
                (Math.random() - 0.5) * 60, 0, (Math.random() - 0.5) * 60));
        }, 80L);
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 3: Panel de objetivos con aparición gradual (8-18 segundos)
        // ═══════════════════════════════════════════════════════════════
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            player.sendMessage("");
            player.sendMessage("§5§l⚜ §8§m════════════════════════════════════════════ §5§l⚜");
            player.sendMessage("");
            soundUtil.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.2f);
        }, 160L); // 8 segundos
        
        // Título principal
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            player.sendMessage(formatearCentrado("§d§l⧖ ACTO I: §5§lLOS SUSURROS APARECEN §d§l⧖"));
            soundUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.0f);
            
            // Partículas de énfasis
            Location loc = player.getLocation();
            loc.getWorld().spawnParticle(Particle.ENCHANT, loc.add(0, 1, 0), 30, 1, 0.5, 1, 0.5);
        }, 200L); // 10 segundos
        
        // Objetivos con aparición lenta (5 segundos entre cada uno)
        String[] objetivos = {
            "§8◆ §7Fragmentos antiguos de piedra yacen dispersos por el mundo",
            "§8◆ §7Cada fragmento guarda secretos y revelaciones olvidadas",
            "§8◆ §7Reúne todos los fragmentos para descubrir la verdad oculta"
        };
        
        for (int i = 0; i < objetivos.length; i++) {
            final int indice = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                
                player.sendMessage("");
                player.sendMessage("    " + objetivos[indice]);
                soundUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 0.4f, 1.2f + (indice * 0.15f));
                
                // Partículas sutiles
                Location loc = player.getLocation();
                loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.add(0, 1.5, 0), 8, 0.5, 0.3, 0.5, 0.02);
            }, 260L + (i * 80L)); // 13s, 17s, 21s (4 segundos entre cada objetivo)
        }
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 4: Mensaje del Observador (21-28 segundos)
        // ═══════════════════════════════════════════════════════════════
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            player.sendMessage("");
            player.sendMessage("    §5§o\"La piedra recuerda... ¿recordarás tú?\"");
            player.sendMessage("    §8§o— El Observador");
            soundUtil.playSound(player, Sound.ENTITY_WARDEN_AMBIENT, 0.3f, 0.5f);
            
            // Efecto de susurro
            Location loc = player.getLocation();
            loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.add(0, 1, 0), 15, 1, 0.5, 1, 0.01);
        }, 520L); // 26 segundos
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 5: Cierre épico (28-32 segundos)
        // ═══════════════════════════════════════════════════════════════
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            player.sendMessage("");
            player.sendMessage("§5§l⚜ §8§m════════════════════════════════════════════ §5§l⚜");
            player.sendMessage("");
            
            soundUtil.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.5f);
            soundUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 0.8f);
            
            // Partículas de cierre
            Location loc = player.getLocation();
            loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.add(0, 1.5, 0), 20, 1, 1, 1, 0.3);
        }, 600L); // 30 segundos
        
        // Mensaje de ayuda
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            player.sendMessage("§e§l[!] §7Sigue el §daction bar §7para encontrar los fragmentos");
            player.sendMessage("");
            soundUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
        }, 680L); // 34 segundos
    }
    
    private void mostrarCliffhanger() {
        plugin.getLogger().info("[SusurroPiedraRota] Mostrando cliffhanger");
        
        // Símbolo en el cielo
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location simboloLoc = player.getLocation().clone().add(0, 50, 0);
            
            // Parpadeo 1
            mostrarSimboloMisterioso(simboloLoc);
            
            // Parpadeo 2
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                mostrarSimboloMisterioso(simboloLoc);
            }, 10L);
        }
        
        playSoundToAll(Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 0.8f);
        
        // Completar evento
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                completarEvento();
            }
        }, 40L);
    }
    
    private void mostrarSimboloMisterioso(Location centro) {
        // Símbolo simple: círculo con cruz
        World world = centro.getWorld();
        
        // Círculo
        for (int i = 0; i < 36; i++) {
            double angle = i * Math.PI / 18;
            double x = Math.cos(angle) * 3;
            double z = Math.sin(angle) * 3;
            
            Location particleLoc = centro.clone().add(x, 0, z);
            world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
        }
        
        // Cruz
        for (int i = -3; i <= 3; i++) {
            world.spawnParticle(Particle.END_ROD, centro.clone().add(i, 0, 0), 1, 0, 0, 0, 0);
            world.spawnParticle(Particle.END_ROD, centro.clone().add(0, 0, i), 1, 0, 0, 0, 0);
        }
    }
    
    private void tickActo4() {
        // El acto progresa automáticamente con tareas programadas
    }
    
    private void completarEvento() {
        plugin.getLogger().info("[SusurroPiedraRota] Evento completado - Iniciando secuencia de cierre cinemática");
        
        actoActual = Acto.VICTORIA;
        tiempoCompletadoEvento = System.currentTimeMillis();
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 1: DIÁLOGO DEL OBSERVADOR (0-8 segundos)
        // ═══════════════════════════════════════════════════════════════
        
        // Efecto de cámara inicial
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participantesOriginales.contains(p.getUniqueId())) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 400, 2, true, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, true, false));
            }
        }
        
        // Sonido atmosférico
        playSoundToAll(Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.8f, 0.6f);
        
        // --- Mensaje 1: Observador habla (0s) ---
        broadcastNarrative("");
        broadcastNarrative("§8§m                                                    ");
        broadcastNarrative("");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            broadcastNarrative("    §5§lEl Observador§7: §o\"...¡lo lograron!...\"");
            playSoundToAll(Sound.ENTITY_WARDEN_AMBIENT, 0.5f, 0.5f);
        }, 20L);
        
        // --- Mensaje 2: (2s) ---
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            broadcastNarrative("    §5§lEl Observador§7: §o\"...el núcleo ha sido contenido...\"");
            playSoundToAll(Sound.ENTITY_WARDEN_AMBIENT, 0.4f, 0.6f);
        }, 60L);
        
        // --- Mensaje 3: (4s) El mensaje original en chat ---
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            broadcastNarrative("    §5§lEl Observador§7: §o\"...pero la Forma... la Forma recuerda...\"");
            playSoundToAll(Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.8f);
        }, 100L);
        
        // --- Pausa de suspenso (6s) - silencio inquietante ---
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            playSoundToAll(Sound.ENTITY_WARDEN_HEARTBEAT, 0.4f, 0.5f);
        }, 140L);
        
        // --- Más suspenso (7s) ---
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            playSoundToAll(Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.4f);
        }, 160L);
        
        // --- MOMENTO CLIMÁTICO (8s) - ¡TE RECUERDA! ---
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // ¡EL TÍTULO DRAMÁTICO!
            enviarTituloCinematicoTodos(
                "§4§k..§r §5§lTE RECUERDA... §4§k..",
                "",
                80
            );
            playSoundToAll(Sound.ENTITY_WARDEN_ROAR, 0.8f, 0.4f);
            playSoundToAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 0.5f);
            crearDestelloTodos(20);
        }, 180L);
        
        // --- Mensaje 4: (11s) Después del impacto ---
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            broadcastNarrative("");
            broadcastNarrative("    §5§lEl Observador§7: §o\"...esto no es el final...\"");
            playSoundToAll(Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.7f);
        }, 220L);
        
        // --- Mensaje 5: Advertencia (12.5s) ---
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            broadcastNarrative("    §5§lEl Observador§7: §o\"...solo el comienzo del susurro...\"");
            playSoundToAll(Sound.ENTITY_WARDEN_AMBIENT, 0.4f, 0.5f);
        }, 250L);
        
        // --- Mensaje 6: Despedida críptica (15s) ---
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            broadcastNarrative("");
            broadcastNarrative("    §8§o\"...nos volveremos a ver...\"");
            playSoundToAll(Sound.ENTITY_ENDERMAN_STARE, 0.4f, 0.5f);
        }, 300L);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            broadcastNarrative("    §8§o\"...en el lugar donde las formas se rompen...\"");
            broadcastNarrative("");
            broadcastNarrative("§8§m                                                    ");
            playSoundToAll(Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 0.5f);
        }, 340L);
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 2: NARRATIVA DEL EVENTO (19-25 segundos)
        // ═══════════════════════════════════════════════════════════════
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Efecto visual de transición
            crearDestelloTodos(15);
            playSoundToAll(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 0.8f);
            
            enviarTituloCinematicoTodos(
                "§5§l✦ EL SUSURRO HA CESADO ✦",
                "§8...por ahora...",
                100
            );
        }, 380L);
        
        // Narrativa del evento (21s)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            broadcastNarrative("");
            broadcastNarrative("§8§m══════════════════════════════════════════════════");
            broadcastNarrative("");
            broadcastNarrative("          §5§lEL SUSURRO EN LA PIEDRA ROTA");
            broadcastNarrative("               §7ha sido completado");
            broadcastNarrative("");
            broadcastNarrative("    §7Juntaron todos los fragmentos de memoria.");
            broadcastNarrative("    §7El núcleo de la Forma ha sido capturado.");
            broadcastNarrative("    §7Pero algo cambió en el proceso...");
            broadcastNarrative("");
            playSoundToAll(Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.8f);
        }, 420L);
        
        // Teaser futuro (23s)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            broadcastNarrative("    §5\"El Observador ahora sabe que están aquí.\"");
            broadcastNarrative("    §5\"Y lo que observa... nunca olvida.\"");
            broadcastNarrative("");
            broadcastNarrative("§8§m══════════════════════════════════════════════════");
            broadcastNarrative("");
            playSoundToAll(Sound.ENTITY_WARDEN_HEARTBEAT, 0.4f, 1.0f);
        }, 460L);
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 3: CÁLCULO Y ENTREGA DE RECOMPENSAS (26-32 segundos)
        // ═══════════════════════════════════════════════════════════════
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Calcular rangos
            calcularRangosRecompensaDinamicos();
            
            // Título de recompensas
            enviarTituloCinematicoTodos(
                "§6§l★ RECOMPENSAS ★",
                "§7Tu rango ha sido calculado...",
                60
            );
            
            playSoundToAll(Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f);
        }, 520L);
        
        // Entregar recompensas con delay dramático (28s)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            entregarRecompensasDinamicas();
        }, 560L);
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 4: AGRADECIMIENTO FINAL (34-38 segundos)
        // ═══════════════════════════════════════════════════════════════
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Efecto final
            crearDestelloTodos(8);
            sacudirPantallaTodos(1);
            
            for (UUID uuid : participantesOriginales) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    // Quitar efectos negativos
                    p.removePotionEffect(PotionEffectType.SLOWNESS);
                    p.removePotionEffect(PotionEffectType.BLINDNESS);
                    
                    // Título de agradecimiento
                    p.sendTitle(
                        "§d§l✨ ¡GRACIAS POR JUGAR! ✨",
                        "§7El Observador seguirá vigilando...",
                        20, 100, 30
                    );
                    
                    // Efectos celebratorios
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    p.spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.2);
                }
            }
            
            playSoundToAll(Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.7f, 1.2f);
        }, 600L);
        
        // Mensaje final en chat (36s)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            broadcastNarrative("");
            broadcastNarrative("§d§l✦ ¡Gracias por participar en El Susurro en la Piedra Rota! ✦");
            broadcastNarrative("§7Revisa tu inventario para ver tus recompensas.");
            broadcastNarrative("§8El próximo capítulo llegará pronto...");
            broadcastNarrative("");
        }, 720L);
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 5: LIMPIEZA (40 segundos)
        // ═══════════════════════════════════════════════════════════════
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            limpiezaCompletaEvento();
            stop();
        }, 800L);
    }
    
    /**
     * Calcula rangos dinámicos basados en participación y tiempo
     */
    private void calcularRangosRecompensaDinamicos() {
        long tiempoTotal = tiempoCompletadoEvento - tiempoInicioEvento;
        double minutosTotal = tiempoTotal / 60000.0;
        
        for (UUID uuid : participantesOriginales) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            
            int fragmentos = participacionFragmentos.getOrDefault(uuid, 0);
            int puzzles = puzzlesCompletados.getOrDefault(uuid, 0);
            int criaturas = participacionCriaturas.getOrDefault(uuid, 0);
            boolean recogioNucleo = uuid.equals(jugadorQueRecogio);
            
            // Puntuación total
            int puntuacion = 0;
            puntuacion += fragmentos * 20;      // 20 pts por fragmento
            puntuacion += puzzles * 15;          // 15 pts por puzzle
            puntuacion += criaturas * 2;         // 2 pts por criatura
            if (recogioNucleo) puntuacion += 50; // 50 pts por núcleo
            
            // Bonus por tiempo (más rápido = más puntos)
            if (minutosTotal <= 8) puntuacion += 40;
            else if (minutosTotal <= 12) puntuacion += 25;
            else if (minutosTotal <= 18) puntuacion += 10;
            
            // Determinar rango
            String rango;
            if (puntuacion >= 120) {
                rango = "PLATINUM";
            } else if (puntuacion >= 80) {
                rango = "GOLD";
            } else if (puntuacion >= 40) {
                rango = "SILVER";
            } else {
                rango = "BRONZE";
            }
            
            rangoRecompensa.put(uuid, rango);
            
            plugin.getLogger().info(String.format(
                "[SusurroPiedraRota] %s: %d pts = %s (frags:%d, puzzles:%d, crits:%d, nucleo:%s)",
                player.getName(), puntuacion, rango, fragmentos, puzzles, criaturas, recogioNucleo
            ));
        }
    }
    
    /**
     * Entrega recompensas dinámicas basadas en el sistema de rangos
     * Ahora usa el sistema de reclamación con /recompensa
     */
    private void entregarRecompensasDinamicas() {
        for (UUID uuid : participantesOriginales) {
            String rangoStr = rangoRecompensa.getOrDefault(uuid, "BRONZE");
            SusurroPiedraRotaItems.RangoRecompensa rango;
            
            switch (rangoStr) {
                case "PLATINUM": rango = SusurroPiedraRotaItems.RangoRecompensa.PLATINUM; break;
                case "GOLD": rango = SusurroPiedraRotaItems.RangoRecompensa.GOLD; break;
                case "SILVER": rango = SusurroPiedraRotaItems.RangoRecompensa.SILVER; break;
                default: rango = SusurroPiedraRotaItems.RangoRecompensa.BRONZE; break;
            }
            
            boolean recogioNucleo = uuid.equals(jugadorQueRecogio);
            int fragmentos = participacionFragmentos.getOrDefault(uuid, 0);
            
            // Generar recompensas
            List<ItemStack> recompensas = SusurroPiedraRotaItems.generarRecompensas(rango, recogioNucleo, fragmentos);
            
            // Calcular PS
            int psTotal = calcularPS(uuid);
            
            // Almacenar en sistema de reclamación (1 hora = 60 minutos)
            if (plugin.getRewardClaimSystem() != null && !recompensas.isEmpty()) {
                plugin.getRewardClaimSystem().addRewards(
                    uuid,
                    "susurro_piedra_rota",
                    "El Susurro en la Piedra Rota",
                    recompensas,
                    60, // 60 minutos = 1 hora
                    rangoStr,
                    psTotal
                );
            }
            
            // Dar PS directamente (no va al cofre)
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                if (plugin.getExperienceService() != null) {
                    plugin.getExperienceService().addXP(player, psTotal, "Evento: El Susurro en la Piedra Rota");
                }
                
                // Mensaje personalizado de rango
                enviarMensajeRangoDinamico(player, rango, fragmentos, recogioNucleo, recompensas.size(), psTotal);
                
                // Efecto de notificación
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);
            }
            
            plugin.getLogger().info(String.format(
                "[SusurroPiedraRota] Recompensas almacenadas para %s: %d items, rango %s, %d PS",
                uuid, recompensas.size(), rangoStr, psTotal
            ));
        }
    }
    
    private int calcularPS(UUID uuid) {
        int baseParticipacion = 30;
        int fragmentos = participacionFragmentos.getOrDefault(uuid, 0);
        int criaturas = participacionCriaturas.getOrDefault(uuid, 0);
        
        int psTotal = baseParticipacion + (fragmentos * 10) + (criaturas * 5);
        if (uuid.equals(jugadorQueRecogio)) psTotal += 50;
        
        String rango = rangoRecompensa.getOrDefault(uuid, "BRONZE");
        switch (rango) {
            case "PLATINUM": psTotal += 100; break;
            case "GOLD": psTotal += 70; break;
            case "SILVER": psTotal += 40; break;
            default: psTotal += 20; break;
        }
        
        return Math.min(psTotal, 300);
    }
    
    private void enviarMensajeRangoDinamico(Player player, SusurroPiedraRotaItems.RangoRecompensa rango, 
                                            int fragmentos, boolean recogioNucleo, int itemsRecibidos, int psTotal) {
        player.sendMessage("");
        player.sendMessage("§8§m════════════════════════════════════════════");
        player.sendMessage("");
        player.sendMessage("           " + rango.nombre);
        player.sendMessage("");
        player.sendMessage("  §7Fragmentos visitados: §e" + fragmentos);
        if (recogioNucleo) {
            player.sendMessage("  §d✦ Recogiste el Núcleo de Forma");
        }
        player.sendMessage("");
        player.sendMessage("  §a✦ §f" + itemsRecibidos + " §aitems únicos te esperan");
        player.sendMessage("  §a✦ §f+" + psTotal + " §aPS otorgados");
        player.sendMessage("");
        
        // Mensaje según rango
        switch (rango) {
            case PLATINUM:
                player.sendMessage("  §b\"El Observador está... impresionado.\"");
                player.sendMessage("  §b\"Pocos muestran tal determinación.\"");
                break;
            case GOLD:
                player.sendMessage("  §6\"Has demostrado valor ante el vacío.\"");
                player.sendMessage("  §6\"El Observador te recuerda.\"");
                break;
            case SILVER:
                player.sendMessage("  §7\"Sobreviviste al susurro.\"");
                player.sendMessage("  §7\"Eso ya es un logro.\"");
                break;
            case BRONZE:
                player.sendMessage("  §c\"Participaste... eso es algo.\"");
                player.sendMessage("  §c\"Quizás la próxima vez...\"");
                break;
        }
        
        player.sendMessage("");
        player.sendMessage("  §e⚠ Usa §a/recompensa §epara reclamar tus items");
        player.sendMessage("  §8(Disponible por 1 hora)");
        player.sendMessage("");
        player.sendMessage("§8§m════════════════════════════════════════════");
        player.sendMessage("");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // MÉTODOS PÚBLICOS PARA COMANDOS
    // ═══════════════════════════════════════════════════════════════════
    
    public void forzarActo(int numeroActo) {
        switch (numeroActo) {
            case 1:
                iniciarActo1();
                break;
            case 2:
                iniciarActo2();
                break;
            case 3:
                iniciarActo3();
                break;
            case 4:
                iniciarActo4();
                break;
            default:
                plugin.getLogger().warning("[SusurroPiedraRota] Acto inválido: " + numeroActo);
                break;
        }
    }
    
    public void avanzarActo() {
        switch (actoActual) {
            case INTRO:
            case PIEDRA_DESPIERTA:
                completarActo1();
                break;
            case PIEDRA_QUIEBRA:
                completarActo2();
                break;
            case NUCLEO_FORMA:
                completarActo3();
                break;
            case SEGUNDO_SUSURRO:
                completarEvento();
                break;
            default:
                plugin.getLogger().warning("[SusurroPiedraRota] No se puede avanzar desde: " + actoActual);
                break;
        }
    }
    
    public void forzarSpawnFragmento() {
        if (fragmentosLocations.size() >= 5) {
            plugin.getLogger().warning("[SusurroPiedraRota] Ya hay 5 fragmentos spawneados");
            return;
        }
        
        World world = Bukkit.getWorlds().get(0);
        Location spawn = world.getSpawnLocation();
        
        Location fragmentoLoc = encontrarLocationValida(world, spawn, 50, 150, 30);
        if (fragmentoLoc != null) {
            construirFragmentoPiedra(fragmentoLoc);
            fragmentosLocations.add(fragmentoLoc);
            plugin.getLogger().info("[SusurroPiedraRota] Fragmento adicional spawneado");
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE BOSS ESPECIAL
    // ═══════════════════════════════════════════════════════════════════
    
    private void spawnearBossEspecial() {
        bossActivo = true;
        
        // Anuncio épico
        broadcastNarrative("§c§l⚠ ¡UNA PRESENCIA OSCURA SE MANIFIESTA!");
        enviarTituloCinematicoTodos(
            "§4§l☠ BOSS ☠",
            "§cCriatura de Forma Superior",
            70
        );
        
        // Sonidos de boss spawn
        playSoundToAll(Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        playSoundToAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
        playSoundToAll(Sound.ENTITY_WARDEN_ROAR, 0.8f, 1.0f);
        
        // Encontrar ubicación de spawn cerca de la grieta
        Location spawnLoc = grietaLocation != null ? 
            encontrarSpawnSeguro(grietaLocation, 8, 15) : 
            encontrarJugadorMasCercano(Bukkit.getWorlds().get(0).getSpawnLocation()).getLocation();
        
        if (spawnLoc == null) {
            spawnLoc = grietaLocation.clone().add(0, 1, 0);
        }
        
        // Ritual de invocación del boss (5 segundos)
        final Location finalSpawnLoc = spawnLoc;
        BukkitTask[] ritualBoss = {null};
        
        ritualBoss[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 100) { // 5 segundos
                    ritualBoss[0].cancel();
                    spawnearBossFinal(finalSpawnLoc);
                    return;
                }
                
                // Efectos de ritual
                double angle = ticks * 0.3;
                for (int i = 0; i < 8; i++) {
                    double rad = Math.toRadians(angle + (i * 45));
                    double radius = 5 - (ticks * 0.03);
                    Location ritualLoc = finalSpawnLoc.clone().add(
                        Math.cos(rad) * radius,
                        ticks * 0.05,
                        Math.sin(rad) * radius
                    );
                    finalSpawnLoc.getWorld().spawnParticle(
                        Particle.SOUL_FIRE_FLAME,
                        ritualLoc,
                        2,
                        0, 0, 0,
                        0.01
                    );
                }
                
                // Columna de energía
                finalSpawnLoc.getWorld().spawnParticle(
                    Particle.REVERSE_PORTAL,
                    finalSpawnLoc.clone().add(0, ticks * 0.05, 0),
                    10,
                    0.3, 0.3, 0.3,
                    0.2
                );
                
                if (ticks % 20 == 0) {
                    playSoundToAll(Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 0.5f);
                }
                
                ticks++;
            }
        }, 0L, 1L);
    }
    
    private void spawnearBossFinal(Location spawnLoc) {
        // Explosión de spawn épica
        spawnLoc.getWorld().spawnParticle(
            Particle.EXPLOSION,
            spawnLoc,
            5,
            2, 2, 2,
            0
        );
        
        playSoundToAll(Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
        playSoundToAll(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.0f);
        
        // Crear boss (Silverfish grande y poderoso)
        Silverfish boss = (Silverfish) spawnLoc.getWorld().spawnEntity(
            spawnLoc,
            EntityType.SILVERFISH
        );
        
        // Configurar boss
        boss.customName(net.kyori.adventure.text.Component.text("§4§l☠ Forma Superior ☠"));
        boss.setCustomNameVisible(true);
        boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(40.0); // 20 corazones
        boss.setHealth(40.0);
        boss.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(0.4); // Muy rápido
        boss.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).setBaseValue(8.0); // 4 corazones
        
        // Efectos visuales del boss
        boss.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.GLOWING,
            999999,
            0,
            true,
            false
        ));
        
        boss.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.RESISTANCE,
            999999,
            1,
            true,
            false
        ));
        
        // Aura de partículas constante para el boss
        BukkitTask bossAura = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!boss.isValid() || boss.isDead()) {
                return;
            }
            
            Location bossLoc = boss.getLocation().add(0, 0.5, 0);
            
            // Aura oscura
            for (int i = 0; i < 5; i++) {
                double angle = Math.random() * Math.PI * 2;
                double radius = 1.5;
                bossLoc.getWorld().spawnParticle(
                    Particle.SMOKE,
                    bossLoc.clone().add(
                        Math.cos(angle) * radius,
                        Math.random() * 1.5,
                        Math.sin(angle) * radius
                    ),
                    0,
                    0, 0.1, 0,
                    0.01
                );
            }
            
            // Llamas oscuras
            if (System.currentTimeMillis() % 500 < 50) {
                bossLoc.getWorld().spawnParticle(
                    Particle.SOUL_FIRE_FLAME,
                    bossLoc,
                    10,
                    0.5, 0.5, 0.5,
                    0.05
                );
            }
        }, 0L, 2L);
        
        // Agregar a lista de criaturas activas
        criaturasActivas.add(boss);
        
        // Listener para cuando muere el boss
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (!boss.isValid() || boss.isDead()) {
                    bossActivo = false;
                    bossAura.cancel();
                    
                    // Efectos de muerte épica del boss
                    Location deathLoc = boss.getLocation();
                    
                    // Explosión masiva
                    for (int i = 0; i < 50; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double pitch = Math.random() * Math.PI;
                        double speed = 1.0 + Math.random() * 1.0;
                        Vector velocity = new Vector(
                            Math.cos(angle) * Math.sin(pitch) * speed,
                            Math.cos(pitch) * speed,
                            Math.sin(angle) * Math.sin(pitch) * speed
                        );
                        deathLoc.getWorld().spawnParticle(
                            Particle.SOUL,
                            deathLoc,
                            0,
                            velocity.getX(),
                            velocity.getY(),
                            velocity.getZ(),
                            0.5
                        );
                    }
                    
                    playSoundToAll(Sound.ENTITY_WITHER_DEATH, 1.0f, 0.8f);
                    playSoundToAll(Sound.ENTITY_ENDER_DRAGON_DEATH, 0.8f, 1.2f);
                    
                    broadcastNarrative("§a§l✓ ¡BOSS DERROTADO!");
                    
                    // Continuar con el acto si se cumplieron todas las condiciones
                    if (defensaCompletada && !acto2Completado) {
                        acto2Completado = true;
                        completarActo2();
                    }
                    
                    // Cancelar esta tarea
                    this.run();
                }
            }
        }, 0L, 20L);
    }
    
    public void forzarSpawnGrieta() {
        if (grietaLocation != null) {
            plugin.getLogger().warning("[SusurroPiedraRota] La grieta ya existe");
            return;
        }
        
        generarGrietaForma();
    }
    
    public String getInfo() {
        StringBuilder info = new StringBuilder();
        info.append("§8§l═══════════════════════════════════════════════════\n");
        info.append("§5§l  EL SUSURRO EN LA PIEDRA ROTA - INFO\n");
        info.append("§8§l═══════════════════════════════════════════════════\n");
        info.append("\n");
        info.append("§7Acto actual: §f").append(actoActual.name()).append("\n");
        info.append("§7Ticks en acto: §f").append(ticksEnActo).append("\n");
        info.append("§7Ticks totales: §f").append(ticksTotales).append("\n");
        info.append("\n");
        info.append("§6▸ Acto 1 - Piedra Despierta\n");
        info.append("§7  Fragmentos: §f").append(fragmentosInspeccionados.size())
            .append("/").append(fragmentosLocations.size()).append("\n");
        info.append("\n");
        info.append("§6▸ Acto 2 - Piedra Quiebra\n");
        info.append("§7  Oleada actual: §f").append(oleadaActual).append("/").append(oleadasTotales).append("\n");
        info.append("§7  Criaturas vivas: §f").append(criaturasActivas.size()).append("\n");
        info.append("\n");
        info.append("§6▸ Acto 3 - Núcleo\n");
        info.append("§7  Núcleo recogido: §f").append(nucleoRecogido ? "Sí" : "No").append("\n");
        if (jugadorQueRecogio != null) {
            Player p = Bukkit.getPlayer(jugadorQueRecogio);
            info.append("§7  Recogido por: §f").append(p != null ? p.getName() : "Desconocido").append("\n");
        }
        info.append("\n");
        info.append("§7Participantes: §f").append(participantesOriginales.size()).append("\n");
        info.append("§8§l═══════════════════════════════════════════════════\n");
        
        return info.toString();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════════
    
    private void limpiarFragmentos() {
        // ✨ Limpiar beacons preview
        for (org.bukkit.block.Block beacon : beaconsPreview.values()) {
            if (beacon != null && beacon.getType() == Material.BEACON) {
                beacon.setType(Material.AIR);
            }
        }
        beaconsPreview.clear();
        
        // ✨ Limpiar breadcrumbs
        if (breadcrumbsTask != null) {
            breadcrumbsTask.cancel();
            breadcrumbsTask = null;
        }
        breadcrumbsPorJugador.clear();
        
        fragmentosLocations.clear();
        fragmentosInspeccionados.clear();
        jugadoresFragmentosVistos.clear();
        
        // Limpiar sistema de altares
        fragmentoANumeroAltar.clear();
        altaresCompletadosPorJugador.clear();
        altarActualJugador.clear();
        tiempoInicioAltarJugador.clear();
        posicionInicioAltarJugador.clear();
        vidaInicioAltarJugador.clear();
        criaturasEliminadasPorJugador.clear();
        criaturasDeAltar.clear();
    }
    
    private void limpiarGrieta() {
        if (grietaLocation != null) {
            // Restaurar bloques si es necesario
            grietaLocation = null;
        }
    }
    
    private void limpiarNucleo() {
        if (nucleoFrame != null && nucleoFrame.isValid()) {
            nucleoFrame.remove();
        }
        nucleoLocation = null;
        
        // 🧹 Cancelar spawn continuo de criaturas
        if (nucleoSpawnTask != null) {
            nucleoSpawnTask.cancel();
            nucleoSpawnTask = null;
        }
    }
    
    private String locationToString(Location loc) {
        return String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ());
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE BRÚJULA ESPECIAL PARA EL NÚCLEO
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Da una brújula especial a todos los participantes que apunta al núcleo
     */
    private void darBrujulaEspecialATodos() {
        if (nucleoLocation == null) {
            plugin.getLogger().warning("[SusurroPiedraRota] No hay nucleoLocation para la brújula");
            return;
        }
        
        plugin.getLogger().info("[SusurroPiedraRota] Dando brújulas especiales a todos los participantes");
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participantesOriginales.contains(p.getUniqueId())) {
                darBrujulaEspecial(p);
            }
        }
    }
    
    /**
     * Da una brújula especial a un jugador específico
     */
    private void darBrujulaEspecial(Player player) {
        // Crear brújula especial con lore descriptivo
        ItemStack brujula = new ItemStack(Material.COMPASS);
        org.bukkit.inventory.meta.ItemMeta meta = brujula.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§d§l⧖ Brújula del Eco §d§l⧖");
            
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━━━");
            lore.add("");
            lore.add("§7Esta brújula te guía hacia el");
            lore.add("§5Núcleo de Forma Desviada§7.");
            lore.add("");
            lore.add("§e✦ Sigue la aguja");
            lore.add("§e✦ Busca el rayo de luz violeta");
            lore.add("");
            lore.add("§8Posición del núcleo:");
            lore.add("§d" + nucleoLocation.getBlockX() + ", " + 
                     nucleoLocation.getBlockY() + ", " + 
                     nucleoLocation.getBlockZ());
            lore.add("");
            lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━━━");
            meta.setLore(lore);
            
            // Enchantment glow
            meta.addEnchant(org.bukkit.enchantments.Enchantment.FORTUNE, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            
            brujula.setItemMeta(meta);
        }
        
        // Dar la brújula al jugador
        player.getInventory().addItem(brujula);
        
        // Actualizar la brújula para que apunte al núcleo
        player.setCompassTarget(nucleoLocation);
        
        // Mensaje al jugador
        player.sendMessage("");
        player.sendMessage("§d§l⧖ §7Has recibido la §d§lBrújula del Eco§7!");
        player.sendMessage("§d§l⧖ §eSigue la aguja hacia el núcleo.");
        player.sendMessage("§d§l⧖ §7Coordenadas: §e" + nucleoLocation.getBlockX() + ", " + 
                          nucleoLocation.getBlockY() + ", " + nucleoLocation.getBlockZ());
        player.sendMessage("");
        
        // Sonido especial
        player.playSound(player.getLocation(), Sound.ITEM_LODESTONE_COMPASS_LOCK, 1.0f, 1.2f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.5f);
        
        plugin.getLogger().info("[SusurroPiedraRota] Brújula especial dada a " + player.getName());
    }
    
    /**
     * Verifica si un item es la brújula especial del evento
     */
    public boolean isBrujulaEspecial(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) return false;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return meta.getDisplayName().contains("Brújula del Eco");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE GUÍA CON ACTION BAR
    // ═══════════════════════════════════════════════════════════════════
    
    private void iniciarGuiaActionBar() {
        if (guiaActionBarTask != null) {
            guiaActionBarTask.cancel();
        }
        
        guiaActionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Location objetivo = objetivosPorJugador.get(player.getUniqueId());
                
                if (objetivo != null) {
                    double distancia = player.getLocation().distance(objetivo);
                    
                    // 🛑 NO INTERFERIR si hay una actividad de altar en progreso
                    // Las actividades del altar usan su propio ActionBar para mostrar progreso
                    if (altarEnProgreso && jugadoresPresentesEnAltar.contains(player.getUniqueId())) {
                        // No hacer nada - dejar que la actividad maneje el ActionBar
                        continue;
                    }
                    
                    // Si está CERCA del objetivo (menos de 15 bloques) pero NO hay actividad
                    if (distancia < 15.0) {
                        // ✨ NUEVO: Mostrar mensaje informativo si está esperando otros jugadores
                        if (esperandoJugadores) {
                            player.sendActionBar(net.kyori.adventure.text.Component.text(
                                "§5⧖ §eTodos reunidos... preparando ritual..."
                            ));
                        } else if (actoActual == Acto.PIEDRA_DESPIERTA && !altarEnProgreso) {
                            // Mostrar indicación de que debe acercarse al altar o esperar a otros
                            int presentes = jugadoresPresentesEnAltar.size();
                            int totales = (int) Bukkit.getOnlinePlayers().stream()
                                .filter(p -> participantesOriginales.contains(p.getUniqueId()))
                                .filter(p -> p.getGameMode() == org.bukkit.GameMode.SURVIVAL)
                                .count();
                            if (presentes < totales) {
                                player.sendActionBar(net.kyori.adventure.text.Component.text(
                                    "§e⚠ Esperando jugadores: §f" + presentes + "/" + totales
                                ));
                            } else {
                                player.sendActionBar(net.kyori.adventure.text.Component.text(
                                    "§5⧖ §7Acércate más al centro del altar"
                                ));
                            }
                        }
                        
                        // Si llegó al objetivo (menos de 5 bloques), asignar el siguiente
                        if (distancia < 5.0) {
                            actualizarSiguienteObjetivo(player);
                        }
                        continue;
                    }
                    
                    // Calcular dirección relativa al jugador
                    String direccion = calcularDireccionRelativa(player, objetivo);
                    
                    // Mostrar en action bar usando API moderna (más visible y claro)
                    String simbolo = distancia < 30 ? "§a⬢" : distancia < 60 ? "§e⬢" : "§c⬢";
                    player.sendActionBar(net.kyori.adventure.text.Component.text(
                        String.format(
                            "%s §f%s §7│ §e%dm §7│ %s",
                            simbolo,
                            direccion,
                            (int)distancia,
                            simbolo
                        )
                    ));
                }
            }
        }, 0L, 20L); // Cada segundo
    }
    
    private String calcularDireccionRelativa(Player player, Location objetivo) {
        Location playerLoc = player.getLocation();
        
        // Calcular vector hacia el objetivo
        double dx = objetivo.getX() - playerLoc.getX();
        double dz = objetivo.getZ() - playerLoc.getZ();
        
        // Calcular ángulo hacia el objetivo (en radianes)
        double anguloObjetivo = Math.atan2(-dx, dz); // Invertir dx y usar dz como y
        
        // Obtener yaw del jugador (donde está mirando) en radianes
        float yaw = playerLoc.getYaw();
        double anguloJugador = Math.toRadians(yaw);
        
        // Calcular diferencia angular
        double diferencia = anguloObjetivo - anguloJugador;
        
        // Normalizar a rango -PI a PI
        while (diferencia > Math.PI) diferencia -= 2 * Math.PI;
        while (diferencia < -Math.PI) diferencia += 2 * Math.PI;
        
        // Determinar dirección relativa basada en la diferencia
        double diferenciaGrados = Math.toDegrees(diferencia);
        
        if (diferenciaGrados >= -45 && diferenciaGrados < 45) {
            return "↑ Adelante";
        } else if (diferenciaGrados >= 45 && diferenciaGrados < 135) {
            return "→ Derecha";
        } else if (diferenciaGrados >= -135 && diferenciaGrados < -45) {
            return "← Izquierda";
        } else {
            return "↓ Atrás";
        }
    }
    
    private void actualizarSiguienteObjetivo(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Determinar qué tipo de objetivo asignar según el acto actual
        if (actoActual == Acto.PIEDRA_DESPIERTA) {
            // Buscar siguiente fragmento
            Location siguiente = encontrarFragmentoMasCercano(player.getLocation());
            if (siguiente != null) {
                objetivosPorJugador.put(uuid, siguiente);
            } else {
                // No hay más fragmentos
                objetivosPorJugador.remove(uuid);
            }
        } else if (actoActual == Acto.PIEDRA_QUIEBRA) {
            // Mantener grieta como objetivo
            if (grietaLocation != null) {
                objetivosPorJugador.put(uuid, grietaLocation);
            }
        } else if (actoActual == Acto.NUCLEO_FORMA) {
            // Mantener núcleo como objetivo
            if (nucleoLocation != null) {
                objetivosPorJugador.put(uuid, nucleoLocation);
            }
        } else {
            objetivosPorJugador.remove(uuid);
        }
    }
    
    private void detenerGuiaActionBar() {
        if (guiaActionBarTask != null) {
            guiaActionBarTask.cancel();
            guiaActionBarTask = null;
        }
        objetivosPorJugador.clear();
    }
    
    private Location encontrarFragmentoMasCercano(Location desde) {
        Location masCercano = null;
        double distanciaMinima = Double.MAX_VALUE;
        
        for (Location fragmento : fragmentosLocations) {
            if (fragmentosInspeccionados.contains(fragmento)) {
                continue; // Saltar fragmentos ya inspeccionados
            }
            
            double distancia = desde.distance(fragmento);
            if (distancia < distanciaMinima) {
                distanciaMinima = distancia;
                masCercano = fragmento;
            }
        }
        
        return masCercano;
    }
    
    private Player encontrarJugadorMasCercano(Location desde) {
        Player masCercano = null;
        double distanciaMinima = Double.MAX_VALUE;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() != desde.getWorld()) continue;
            
            double distancia = player.getLocation().distance(desde);
            if (distancia < distanciaMinima) {
                distanciaMinima = distancia;
                masCercano = player;
            }
        }
        
        return masCercano;
    }
    
    private Location calcularUbicacionPromedioJugadores() {
        List<Player> jugadores = new ArrayList<>(Bukkit.getOnlinePlayers());
        
        if (jugadores.isEmpty()) {
            return null;
        }
        
        double x = 0;
        double y = 0;
        double z = 0;
        World mundo = jugadores.get(0).getWorld();
        
        for (Player p : jugadores) {
            if (p.getWorld() != mundo) continue;
            Location loc = p.getLocation();
            x += loc.getX();
            y += loc.getY();
            z += loc.getZ();
        }
        
        int count = jugadores.size();
        return new Location(mundo, x / count, y / count, z / count);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE PUZZLES Y MINIJUEGOS - MINIJUEGO DE PATRÓN ELIMINADO
    // ═══════════════════════════════════════════════════════════════════
    
    // Acto 2: Sistema de oleadas de defensa (SIN minijuego de patrón)
    // El progreso es automático tras completar las oleadas
    // Todos los métodos del patrón de memoria han sido eliminados
    
    private void iniciarPuzzleActo3() {
        // Instrucciones CLARAS para el laberinto
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            broadcastNarrative("§8§m═══════════════════════════════════════════§r");
            broadcastNarrative("");
            broadcastNarrative("§5§l✦ ACTO 3: LABERINTO DEL NÚCLEO");
            broadcastNarrative("");
            broadcastNarrative("§f¿Qué hacer?");
            broadcastNarrative("§7  1. Navega por el laberinto circular");
            broadcastNarrative("§7  2. Sigue los bloques brillantes (glowstone)");
            broadcastNarrative("§7  3. Llega al núcleo en el centro");
            broadcastNarrative("");
            broadcastNarrative("§f¿Qué evitar?");
            broadcastNarrative("§7  → Arena de almas = camino falso");
            broadcastNarrative("§7  → Te ralentiza y marea temporalmente");
            broadcastNarrative("");
            broadcastNarrative("§a✓ Sigue las luces END_ROD para guiarte");
            broadcastNarrative("§8§m═══════════════════════════════════════════§r");
        }, 20L);
        
        // Crear laberinto después de instrucciones
        Bukkit.getScheduler().runTaskLater(plugin, this::construirLaberintoNucleo, 120L);
    }
    
    private void construirLaberintoNucleo() {
        World world = nucleoLocation.getWorld();
        Random rand = new Random();
        
        // TERRAFORMACIÓN: Aplanar área completa 35x35
        int radioTerraform = 18;
        int centerX = nucleoLocation.getBlockX();
        int centerY = nucleoLocation.getBlockY();
        int centerZ = nucleoLocation.getBlockZ();
        
        // Aplanar y crear base sólida
        for (int x = -radioTerraform; x <= radioTerraform; x++) {
            for (int z = -radioTerraform; z <= radioTerraform; z++) {
                // Base de piedra (2 capas)
                world.getBlockAt(centerX + x, centerY - 2, centerZ + z).setType(Material.STONE);
                world.getBlockAt(centerX + x, centerY - 1, centerZ + z).setType(Material.STONE);
                
                // Suelo del laberinto (piedra lisa)
                world.getBlockAt(centerX + x, centerY, centerZ + z).setType(Material.SMOOTH_STONE);
                
                // Limpiar espacio arriba (8 bloques para más altura)
                for (int dy = 1; dy <= 8; dy++) {
                    world.getBlockAt(centerX + x, centerY + dy, centerZ + z).setType(Material.AIR);
                }
            }
        }
        
        // Crear paredes del laberinto (radio 15 bloques)
        int radio = 15;
        caminoCorrecto.clear();
        caminosFalsos.clear();
        
        // Estructura de laberinto en espiral más definida
        for (double angulo = 0; angulo < Math.PI * 8; angulo += 0.2) {
            double r = 5 + (angulo / (Math.PI * 8)) * (radio - 5);
            int x = nucleoLocation.getBlockX() + (int)(Math.cos(angulo) * r);
            int z = nucleoLocation.getBlockZ() + (int)(Math.sin(angulo) * r);
            int y = nucleoLocation.getBlockY();
            
            Location pared = new Location(world, x, y, z);
            
            // Crear paredes de 6 bloques de altura (imponente)
            for (int altura = 1; altura <= 6; altura++) {
                Location bloque = pared.clone().add(0, altura, 0);
                bloque.getBlock().setType(Material.OBSIDIAN);
            }
            
            // Algunas aperturas aleatorias (menos frecuentes)
            if (rand.nextInt(15) < 2) {
                pared.clone().add(0, 1, 0).getBlock().setType(Material.AIR);
                pared.clone().add(0, 2, 0).getBlock().setType(Material.AIR);
            }
        }
        
        // Marcar camino correcto con partículas sutiles
        // ENTRADA: En el borde del laberinto, cerca del centro (grietaLocation)
        Location entrada = grietaLocation.clone().add(radio - 2, 0, 0);
        entrada.getBlock().setType(Material.SMOOTH_STONE);
        caminoCorrecto.add(entrada);
        
        // Crear puntos de checkpoint con efectos en espiral hacia el núcleo
        for (int i = 1; i <= 5; i++) {
            double angulo = (Math.PI * 2 * i) / 5;
            double r = radio - (i * 2.5);
            Location checkpoint = grietaLocation.clone().add(
                Math.cos(angulo) * r,
                0,
                Math.sin(angulo) * r
            );
            
            caminoCorrecto.add(checkpoint);
            
            // Marcar con glowstone y partículas
            checkpoint.getBlock().setType(Material.GLOWSTONE);
            checkpoint.clone().add(0, 1, 0).getBlock().setType(Material.AIR);
            
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (actoActual != Acto.NUCLEO_FORMA || puzzleActo3Completado) return;
                checkpoint.getWorld().spawnParticle(Particle.END_ROD, 
                    checkpoint.clone().add(0.5, 1, 0.5), 3, 0.3, 0.3, 0.3, 0.02);
            }, 0L, 20L);
        }
        
        // Crear trampas en caminos falsos
        crearTrampasCaminos();
        
        plugin.getLogger().info("[SusurroPiedraRota] Laberinto terraformado (35x35, altura 8) con paredes de 6 bloques");
    }
    
    private void crearTrampasCaminos() {
        // Colocar bloques trampa que dan efectos negativos temporales
        Random rand = new Random();
        
        for (int i = 0; i < 20; i++) {
            double angulo = rand.nextDouble() * Math.PI * 2;
            double r = 8 + rand.nextDouble() * 7;
            Location trampa = nucleoLocation.clone().add(
                Math.cos(angulo) * r,
                0,
                Math.sin(angulo) * r
            );
            
            // Evitar poner trampas en el camino correcto
            boolean demasiadoCerca = false;
            for (Location correcto : caminoCorrecto) {
                if (correcto.distance(trampa) < 2) {
                    demasiadoCerca = true;
                    break;
                }
            }
            
            if (demasiadoCerca) continue;
            
            caminosFalsos.add(trampa);
            trampa.getBlock().setType(Material.SOUL_SAND);
            
            // Partículas de advertencia
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (actoActual != Acto.NUCLEO_FORMA || puzzleActo3Completado) return;
                trampa.getWorld().spawnParticle(Particle.SMOKE, 
                    trampa.clone().add(0.5, 0.1, 0.5), 1, 0.1, 0, 0.1, 0);
            }, 0L, 40L);
        }
    }
    
    private void verificarProgresoLaberinto(Player player) {
        Location playerLoc = player.getLocation();
        
        // Verificar si pisó trampa
        for (Location trampa : caminosFalsos) {
            if (playerLoc.distance(trampa) < 1.5) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
                player.sendMessage("§c✗ Camino falso! Te sientes desorientado...");
                soundUtil.playSound(player, Sound.ENTITY_GHAST_AMBIENT, 1.0f, 0.5f);
                playerLoc.getWorld().spawnParticle(Particle.SMOKE, playerLoc, 30, 0.5, 0.5, 0.5, 0.1);
                return;
            }
        }
        
        // Verificar si llegó al núcleo
        if (playerLoc.distance(nucleoLocation) < 3) {
            puzzleActo3Completado = true;
            broadcastNarrative("§a✓ " + player.getName() + " §anavegó el laberinto!");
            playSoundToAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            
            // Limpiar laberinto
            Bukkit.getScheduler().runTaskLater(plugin, this::limpiarLaberinto, 40L);
        }
    }
    
    private void limpiarLaberinto() {
        // Convertir paredes a partículas y eliminar
        World world = nucleoLocation.getWorld();
        int radio = 15;
        
        for (double angulo = 0; angulo < Math.PI * 8; angulo += 0.3) {
            double r = 5 + (angulo / (Math.PI * 8)) * (radio - 5);
            int x = nucleoLocation.getBlockX() + (int)(Math.cos(angulo) * r);
            int z = nucleoLocation.getBlockZ() + (int)(Math.sin(angulo) * r);
            int y = nucleoLocation.getBlockY();
            
            for (int altura = 0; altura < 3; altura++) {
                Location bloque = new Location(world, x, y + altura, z);
                if (bloque.getBlock().getType() == Material.OBSIDIAN) {
                    bloque.getWorld().spawnParticle(Particle.SMOKE, 
                        bloque.clone().add(0.5, 0.5, 0.5), 5, 0.3, 0.3, 0.3, 0);
                    bloque.getBlock().setType(Material.AIR);
                }
            }
        }
        
        broadcastNarrative("§5El laberinto desaparece...");
    }
    
    private void verificarSimboloPedestal(Player player, int pedestal) {
        simbolosJugador.putIfAbsent(player.getUniqueId(), new ArrayList<>());
        List<String> simbolos = simbolosJugador.get(player.getUniqueId());
        
        if (pedestal < 0 || pedestal >= simbolosCorrectos.size()) return;
        
        simbolos.add(simbolosCorrectos.get(pedestal));
        
        // Verificar si la secuencia es correcta
        boolean correctoHastaAhora = true;
        for (int i = 0; i < simbolos.size(); i++) {
            if (!simbolos.get(i).equals(simbolosCorrectos.get(i))) {
                correctoHastaAhora = false;
                break;
            }
        }
        
        if (!correctoHastaAhora) {
            player.sendMessage("§c✗ Orden incorrecto! Reiniciando...");
            simbolos.clear();
            soundUtil.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.5f);
            return;
        }
        
        if (simbolos.size() == simbolosCorrectos.size()) {
            puzzleActo3Completado = true;
            broadcastNarrative("§a✓ " + player.getName() + " §adesció los símbolos!");
            playSoundToAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            
            // Hacer accesible el núcleo
            nucleoRecogido = false;
        } else {
            player.sendMessage("§a✓ Símbolo correcto! (" + simbolos.size() + "/4)");
            soundUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // VALIDACIÓN INTELIGENTE DE SPAWN
    // ═══════════════════════════════════════════════════════════════════
    
    private Location encontrarSpawnSeguro(Location centro, int radioMin, int radioMax) {
        Random random = new Random();
        int intentos = 0;
        int maxIntentos = 30;
        
        while (intentos < maxIntentos) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = radioMin + random.nextDouble() * (radioMax - radioMin);
            
            double x = centro.getX() + Math.cos(angle) * distance;
            double z = centro.getZ() + Math.sin(angle) * distance;
            
            Location spawn = new Location(centro.getWorld(), x, centro.getY(), z);
            
            if (esSpawnSeguro(spawn)) {
                return spawn;
            }
            
            intentos++;
        }
        
        return null; // No se encontró ubicación segura
    }
    
    private boolean esSpawnSeguro(Location loc) {
        // 1. Verificar que el chunk esté cargado
        if (!loc.getChunk().isLoaded()) {
            loc.getChunk().load();
        }
        
        // 2. Obtener superficie sólida
        World world = loc.getWorld();
        Location superficie = world.getHighestBlockAt(loc).getLocation();
        
        // 3. Verificar que no sea agua ni lava EN TODO EL RADIO DE 20 BLOQUES
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                if (x * x + z * z > 400) continue; // Radio circular de 20
                
                Location checkLoc = superficie.clone().add(x, 0, z);
                Material tipo = world.getBlockAt(checkLoc).getType();
                if (tipo == Material.WATER || tipo == Material.LAVA) {
                    return false; // Rechazar si hay agua o lava cerca
                }
                
                // También verificar un bloque abajo
                Material tipoAbajo = checkLoc.clone().add(0, -1, 0).getBlock().getType();
                if (tipoAbajo == Material.WATER || tipoAbajo == Material.LAVA) {
                    return false;
                }
            }
        }
        
        // 4. Verificar espacio libre vertical (15 bloques de altura)
        for (int y = 1; y <= 15; y++) {
            Location blockLoc = superficie.clone().add(0, y, 0);
            if (world.getBlockAt(blockLoc).getType().isSolid()) {
                return false;
            }
        }
        
        // 5. Verificar que el bloque base sea sólido y estable
        if (!superficie.getBlock().getType().isSolid()) {
            return false;
        }
        
        // 6. Verificar área plana más amplia 15x15 (no montañas)
        int baseY = superficie.getBlockY();
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                Location checkLoc = superficie.clone().add(x, 0, z);
                int checkY = world.getHighestBlockYAt(checkLoc);
                
                // Si la diferencia de altura es mayor a 2 bloques, no es plano
                if (Math.abs(checkY - baseY) > 2) {
                    return false;
                }
            }
        }
        
        // 7. Verificar sin obstrucciones en radio de 20 bloques (no muchos árboles)
        int bloquesObstruidos = 0;
        int totalBloques = 0;
        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                if (x * x + z * z > 400) continue; // Radio circular
                totalBloques++;
                
                Location checkLoc = superficie.clone().add(x, 0, z);
                // Verificar que tenga espacio libre arriba (hasta 8 bloques)
                for (int y = 1; y <= 8; y++) {
                    if (checkLoc.clone().add(0, y, 0).getBlock().getType().isSolid()) {
                        bloquesObstruidos++;
                        break;
                    }
                }
            }
        }
        
        // Si más del 30% del área está obstruida, rechazar
        double porcentajeObstruido = (double)bloquesObstruidos / totalBloques;
        if (porcentajeObstruido > 0.3) {
            return false;
        }
        
        return true;
    }
    
    private Location encontrarLocationValidaMejorada(World world, Location spawn, int distMin, int distMax, int distEntreFragmentos) {
        Random random = new Random();
        int intentos = 0;
        int maxIntentos = 100;
        int radioActual = distMax;
        
        while (intentos < maxIntentos) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = distMin + random.nextDouble() * (radioActual - distMin);
            
            int x = spawn.getBlockX() + (int)(Math.cos(angle) * distance);
            int z = spawn.getBlockZ() + (int)(Math.sin(angle) * distance);
            
            // Asegurar que el chunk esté cargado
            Chunk chunk = world.getChunkAt(x >> 4, z >> 4);
            if (!chunk.isLoaded()) {
                chunk.load(true);
            }
            
            int y = world.getHighestBlockYAt(x, z);
            Location loc = new Location(world, x, y, z);
            
            // Verificaciones de calidad
            if (!esSpawnSeguro(loc)) {
                intentos++;
                continue;
            }
            
            // Verificar que no esté muy cerca de otros fragmentos
            boolean lejosDeOtros = true;
            for (Location existente : fragmentosLocations) {
                if (existente.distance(loc) < distEntreFragmentos) {
                    lejosDeOtros = false;
                    break;
                }
            }
            
            if (lejosDeOtros) {
                // Verificar que tenga buen rango de visión (no obstruido arriba)
                boolean buenaVision = true;
                for (int checkY = 1; checkY <= 5; checkY++) {
                    if (loc.clone().add(0, checkY, 0).getBlock().getType().isSolid()) {
                        buenaVision = false;
                        break;
                    }
                }
                
                if (buenaVision) {
                    return loc;
                }
            }
            
            intentos++;
            
            // Expandir radio cada 20 intentos
            if (intentos % 20 == 0 && radioActual < 200) {
                radioActual += 20;
            }
        }
        
        return null;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // EFECTOS ATMOSFÉRICOS
    // ═══════════════════════════════════════════════════════════════════
    
    private void iniciarAtmosfera() {
        World world = Bukkit.getWorlds().get(0);
        
        atmosferaTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (!isActive()) {
                    cancel();
                    return;
                }
                
                // Rayos periódicos dramáticos (cada 15-30 segundos)
                if (ticks % (300 + new Random().nextInt(300)) == 0) {
                    // Elegir ubicación aleatoria cerca del spawn
                    Location spawn = world.getSpawnLocation();
                    double angle = Math.random() * Math.PI * 2;
                    double dist = 30 + Math.random() * 100; // 30-130 bloques
                    
                    int x = spawn.getBlockX() + (int)(Math.cos(angle) * dist);
                    int z = spawn.getBlockZ() + (int)(Math.sin(angle) * dist);
                    int y = world.getHighestBlockYAt(x, z);
                    
                    Location rayoLoc = new Location(world, x, y, z);
                    
                    // Rayo sin fuego
                    world.strikeLightningEffect(rayoLoc);
                    
                    // Efecto de flash para jugadores cercanos
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getWorld().equals(world) && p.getLocation().distance(rayoLoc) < 100) {
                            p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.0f);
                        }
                    }
                }
                
                // Niebla/neblina con partículas cada 5 segundos
                if (ticks % 100 == 0) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (!p.getWorld().equals(world)) continue;
                        
                        Location pLoc = p.getLocation();
                        
                        // Partículas de niebla alrededor del jugador
                        for (int i = 0; i < 15; i++) {
                            double angle = Math.random() * Math.PI * 2;
                            double dist = 5 + Math.random() * 15; // 5-20 bloques
                            double height = Math.random() * 3;
                            
                            double x = Math.cos(angle) * dist;
                            double z = Math.sin(angle) * dist;
                            
                            world.spawnParticle(
                                Particle.CAMPFIRE_COSY_SMOKE,
                                pLoc.clone().add(x, height, z),
                                0,
                                0, 0.02, 0,
                                0.01
                            );
                        }
                    }
                }
                
                // Iluminación coloreada según acto
                if (ticks % 40 == 0) { // Cada 2 segundos
                    Particle particula;
                    switch (actoActual) {
                        case PIEDRA_DESPIERTA:
                            particula = Particle.ENCHANT; // Azul mágico
                            break;
                        case PIEDRA_QUIEBRA:
                            particula = Particle.REVERSE_PORTAL; // Morado oscuro
                            break;
                        case NUCLEO_FORMA:
                            particula = Particle.SOUL; // Azul cyan
                            break;
                        default:
                            particula = Particle.END_ROD; // Blanco neutral
                    }
                    
                    // Partículas atmosféricas flotantes
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (!p.getWorld().equals(world)) continue;
                        
                        Location pLoc = p.getLocation();
                        
                        // 5 partículas flotando arriba del jugador
                        for (int i = 0; i < 5; i++) {
                            double x = (Math.random() - 0.5) * 10;
                            double z = (Math.random() - 0.5) * 10;
                            double y = 10 + Math.random() * 10;
                            
                            world.spawnParticle(
                                particula,
                                pLoc.clone().add(x, y, z),
                                1,
                                0, -0.1, 0,
                                0.01
                            );
                        }
                    }
                }
                
                // Oscurecimiento de áreas con partículas densas
                if (ticks % 20 == 0) { // Cada segundo
                    // Oscurecer área alrededor de estructuras importantes
                    if (grietaLocation != null && actoActual == Acto.PIEDRA_QUIEBRA) {
                        // Cortina de partículas oscuras alrededor de la grieta
                        for (int i = 0; i < 8; i++) {
                            double angle = Math.toRadians(i * 45);
                            double radius = 8.0;
                            double x = Math.cos(angle) * radius;
                            double z = Math.sin(angle) * radius;
                            
                            for (int y = 0; y < 4; y++) {
                                world.spawnParticle(
                                    Particle.SQUID_INK,
                                    grietaLocation.clone().add(x, y * 0.5, z),
                                    2,
                                    0.3, 0.2, 0.3,
                                    0.01
                                );
                            }
                        }
                    }
                    
                    if (nucleoLocation != null && actoActual == Acto.NUCLEO_FORMA) {
                        // Aura morada intensa alrededor del núcleo
                        for (int i = 0; i < 12; i++) {
                            double angle = Math.toRadians(i * 30);
                            double radius = 5.0;
                            double x = Math.cos(angle) * radius;
                            double z = Math.sin(angle) * radius;
                            
                            world.spawnParticle(
                                Particle.SOUL_FIRE_FLAME,
                                nucleoLocation.clone().add(x, 1, z),
                                1,
                                0.2, 0.5, 0.2,
                                0.02
                            );
                        }
                    }
                }
                
                ticks++;
            }
            
            private void cancel() {
                if (atmosferaTask != null) {
                    atmosferaTask.cancel();
                }
            }
        }, 0L, 2L); // Ejecutar cada 0.1 segundos
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // GETTERS PARA ESTADO
    // ═══════════════════════════════════════════════════════════════════
    
    public Acto getActoActual() {
        return actoActual;
    }
    
    // Getters para sistema de altares (usados por Listener)
    public Map<UUID, Integer> getAltarActualJugador() {
        return altarActualJugador;
    }
    
    public Map<UUID, Integer> getCriaturasEliminadasPorJugador() {
        return criaturasEliminadasPorJugador;
    }
    
    public Set<UUID> getCriaturasDeAltar() {
        return criaturasDeAltar;
    }
    
    public List<Location> getFragmentosLocations() {
        return fragmentosLocations;
    }
    
    public Map<Location, Integer> getFragmentoANumeroAltar() {
        return fragmentoANumeroAltar;
    }
    
    /**
     * Obtener el número del altar actual global (1-5)
     */
    public int getAltarActualGlobal() {
        return altarActualGlobal;
    }
    
    /**
     * Método público para completar altares desde el listener
     */
    public void completarAltarPublic(Player player, int numAltar, Location altarLoc, 
                                      String nombreFragmento, org.bukkit.potion.PotionEffect... efectos) {
        completarAltar(player, numAltar, altarLoc, nombreFragmento, efectos);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE AUDIO CINEMATOGRÁFICO
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Inicia el sistema de audio cinematográfico con música por acto y sonidos ambientales.
     */
    private void iniciarSistemaAudio() {
        // Sistema de música por acto (cambia cada 60 segundos)
        musicaTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (Bukkit.getOnlinePlayers().isEmpty()) {
                return;
            }
            
            // Actualizar música según acto actual
            if (actoActual.ordinal() != actoActualMusica) {
                actoActualMusica = actoActual.ordinal();
                reproducirMusicaActo(actoActual);
            }
        }, 0L, 1200L); // Cada 60 segundos
        
        // Sistema de sonidos ambientales (cada 10-20 segundos)
        sonidosAmbientalesTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int ticks = 0;
            private int proximoSonido = 200; // 10 segundos inicial
            
            @Override
            public void run() {
                if (Bukkit.getOnlinePlayers().isEmpty()) {
                    return;
                }
                
                ticks++;
                
                // Reproducir sonido ambiental aleatorio
                if (ticks >= proximoSonido) {
                    reproducirSonidoAmbiental();
                    
                    // Siguiente sonido en 10-20 segundos
                    proximoSonido = ticks + (200 + (int)(Math.random() * 200));
                }
            }
        }, 0L, 2L); // Cada 0.1s para precisión
    }
    
    /**
     * Reproduce la música característica de cada acto.
     */
    private void reproducirMusicaActo(Acto acto) {
        World world = Bukkit.getWorlds().get(0);
        Location centro = world.getSpawnLocation();
        if (!fragmentosLocations.isEmpty()) {
            centro = fragmentosLocations.get(0);
        }
        
        switch (acto) {
            case INTRO:
            case PIEDRA_DESPIERTA:
                // Música de tensión misteriosa (sonidos etéreos, susurros)
                soundUtil.playSound(centro, Sound.AMBIENT_CAVE, 0.8f, 0.7f);
                soundUtil.playSound(centro, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.5f, 0.8f);
                
                // Notificar cambio de música
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage("§5§o♫ Una melodía inquietante susurra en las sombras...");
                }
                break;
                
            case TRANSICION_2:
            case PIEDRA_QUIEBRA:
                // Música de combate épico (tambores, crescendo)
                soundUtil.playSound(centro, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.6f);
                soundUtil.playSound(centro, Sound.ENTITY_WITHER_AMBIENT, 0.6f, 0.5f);
                
                // Notificar cambio de música
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage("§c§o♫ El rugido de la batalla resuena en el aire...");
                }
                break;
                
            case TRANSICION_3:
            case NUCLEO_FORMA:
                // Música climática final (orquesta épica, coros)
                soundUtil.playSound(centro, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 0.8f);
                soundUtil.playSound(centro, Sound.ITEM_TOTEM_USE, 0.8f, 0.6f);
                
                // Notificar cambio de música
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage("§6§o♫ Una sinfonía celestial marca el clímax del destino...");
                }
                break;
                
            case TRANSICION_4:
            case SEGUNDO_SUSURRO:
            case VICTORIA:
                // Música de victoria/resolución
                soundUtil.playSound(centro, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                soundUtil.playSound(centro, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.2f);
                
                // Notificar cambio de música
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage("§a§o♫ El eco de la victoria reverbera en la eternidad...");
                }
                break;
        }
    }
    
    /**
     * Reproduce sonidos ambientales aleatorios según el acto.
     */
    private void reproducirSonidoAmbiental() {
        // Elegir jugador aleatorio para ubicación de sonido
        List<Player> jugadores = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (jugadores.isEmpty()) return;
        
        Player jugador = jugadores.get((int)(Math.random() * jugadores.size()));
        Location loc = jugador.getLocation();
        
        // Sonidos ambientales según acto
        switch (actoActual) {
            case INTRO:
            case PIEDRA_DESPIERTA:
                // Susurros, ecos, crujidos
                int random1 = (int)(Math.random() * 5);
                switch (random1) {
                    case 0:
                        soundUtil.playSound(loc, Sound.ENTITY_ENDERMAN_STARE, 0.3f, 0.5f);
                        break;
                    case 1:
                        soundUtil.playSound(loc, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.4f, 0.7f);
                        break;
                    case 2:
                        soundUtil.playSound(loc, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.5f, 0.8f);
                        break;
                    case 3:
                        soundUtil.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 0.6f);
                        break;
                    case 4:
                        soundUtil.playSound(loc, Sound.ENTITY_VEX_AMBIENT, 0.2f, 0.4f);
                        break;
                }
                break;
                
            case TRANSICION_2:
            case PIEDRA_QUIEBRA:
                // Rugidos, explosiones, alaridos
                int random2 = (int)(Math.random() * 5);
                switch (random2) {
                    case 0:
                        soundUtil.playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 0.5f, 0.7f);
                        break;
                    case 1:
                        soundUtil.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.4f, 0.5f);
                        break;
                    case 2:
                        soundUtil.playSound(loc, Sound.ENTITY_WARDEN_AGITATED, 0.3f, 0.6f);
                        break;
                    case 3:
                        soundUtil.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.4f, 0.8f);
                        break;
                    case 4:
                        soundUtil.playSound(loc, Sound.ENTITY_PHANTOM_AMBIENT, 0.3f, 0.5f);
                        break;
                }
                break;
                
            case TRANSICION_3:
            case NUCLEO_FORMA:
                // Sonidos épicos, místicos, dimensionales
                int random3 = (int)(Math.random() * 5);
                switch (random3) {
                    case 0:
                        soundUtil.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.4f, 0.7f);
                        break;
                    case 1:
                        soundUtil.playSound(loc, Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.5f, 0.8f);
                        break;
                    case 2:
                        soundUtil.playSound(loc, Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.3f, 0.6f);
                        break;
                    case 3:
                        soundUtil.playSound(loc, Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 0.4f, 1.2f);
                        break;
                    case 4:
                        soundUtil.playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 0.3f, 0.9f);
                        break;
                }
                break;
                
            case TRANSICION_4:
            case SEGUNDO_SUSURRO:
            case VICTORIA:
                // Sonidos de resolución, éxito, celestiales
                int random4 = (int)(Math.random() * 3);
                switch (random4) {
                    case 0:
                        soundUtil.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
                        break;
                    case 1:
                        soundUtil.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.4f, 1.2f);
                        break;
                    case 2:
                        soundUtil.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 0.3f, 1.5f);
                        break;
                }
                break;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE EFECTOS DE CÁMARA Y PANTALLA
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Sacude la pantalla del jugador usando títulos rápidos.
     */
    private void sacudirPantalla(Player player, int intensidad) {
        String[] efectos = {"§8█", "§0█", "§8▓", "§0▓"};
        
        for (int i = 0; i < intensidad; i++) {
            final int index = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    String efecto = efectos[index % efectos.length];
                    player.sendTitle(
                        efecto.repeat(3),
                        "",
                        0, 2, 0
                    );
                }
            }, i * 2L);
        }
    }
    
    /**
     * Sacude la pantalla de todos los jugadores.
     */
    private void sacudirPantallaTodos(int intensidad) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            sacudirPantalla(p, intensidad);
        }
    }
    
    /**
     * Crea un destello blanco en la pantalla.
     */
    private void crearDestello(Player player, int duracion) {
        player.sendTitle(
            "§f█████████████████",
            "",
            0, duracion, 5
        );
    }
    
    /**
     * Crea un destello blanco para todos los jugadores.
     */
    private void crearDestelloTodos(int duracion) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            crearDestello(p, duracion);
        }
    }
    
    /**
     * ✨ NUEVO: Elimina mobs hostiles cerca de los jugadores para momentos de narrativa
     * Esto permite leer los diálogos con calma sin interrupciones
     */
    private void limpiarMobsHostilesCercanos() {
        int mobsEliminados = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!participantesOriginales.contains(p.getUniqueId())) continue;
            
            // Radio de limpieza: 30 bloques alrededor de cada jugador
            for (Entity entity : p.getNearbyEntities(30, 30, 30)) {
                if (entity instanceof org.bukkit.entity.Monster) {
                    // No eliminar criaturas del evento (las que spawneamos nosotros)
                    if (criaturasActivas.contains(entity)) continue;
                    if (criaturasDeAltar.contains(entity.getUniqueId())) continue;
                    
                    // Efecto visual de desvanecimiento
                    entity.getWorld().spawnParticle(
                        Particle.SMOKE,
                        entity.getLocation().add(0, 1, 0),
                        10, 0.3, 0.5, 0.3, 0.02
                    );
                    entity.getWorld().spawnParticle(
                        Particle.PORTAL,
                        entity.getLocation().add(0, 1, 0),
                        20, 0.3, 0.5, 0.3, 0.5
                    );
                    
                    entity.remove();
                    mobsEliminados++;
                }
            }
        }
        
        if (mobsEliminados > 0) {
            plugin.getLogger().info("[SusurroPiedraRota] Limpiados " + mobsEliminados + " mobs hostiles para narrativa");
        }
    }
    
    /**
     * Oscurece progresivamente la pantalla.
     */
    private void oscurecerProgresivo(Player player, int pasos) {
        String[] niveles = {
            "§8▓▓▓",           // Ligero
            "§8▓▓▓▓▓▓",       // Medio
            "§0█████████",    // Oscuro
            "§0█████████████", // Muy oscuro
            "§0█████████████████" // Total
        };
        
        for (int i = 0; i < pasos && i < niveles.length; i++) {
            final int index = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.sendTitle(
                        niveles[index],
                        "",
                        5, 20, 5
                    );
                }
            }, i * 10L);
        }
    }
    
    /**
     * Oscurece progresivamente para todos los jugadores.
     */
    private void oscurecerProgresivoTodos(int pasos) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            oscurecerProgresivo(p, pasos);
        }
    }
    
    /**
     * Efecto de pulso de energía (destello + sacudida).
     */
    private void efectoPulsoEnergia(Player player) {
        // Destello inicial
        crearDestello(player, 3);
        
        // Sacudida después del destello
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            sacudirPantalla(player, 3);
        }, 5L);
    }
    
    /**
     * Efecto de pulso de energía para todos.
     */
    private void efectoPulsoEnergiaTodos() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            efectoPulsoEnergia(p);
        }
    }
    
    /**
     * Efecto de cámara lenta (slow motion) con partículas.
     * Reduce velocidad de movimiento y crea atmósfera dramática.
     */
    private void aplicarSlowMotion(Player player, int duracionSegundos) {
        // Efectos de poción para simular cámara lenta
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duracionSegundos * 20, 1, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duracionSegundos * 20, 1, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duracionSegundos * 20, 0, true, false));
        
        // Partículas flotantes para efecto visual
        BukkitRunnable particleTask = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= duracionSegundos * 20) {
                    cancel();
                    return;
                }
                Location loc = player.getLocation();
                player.getWorld().spawnParticle(Particle.END_ROD, loc.add(0, 1, 0), 3, 0.3, 0.5, 0.3, 0.01);
                ticks += 5;
            }
        };
        particleTask.runTaskTimer(plugin, 0L, 5L);
        
        player.sendTitle("§d§l◆", "§5Momento decisivo...", 5, 30, 10);
    }
    
    /**
     * Efecto de sacudida de pantalla intensa con distorsión visual.
     * Simula un impacto extremo o momento crítico.
     */
    private void aplicarScreenShakeIntenso(Player player, int duracionSegundos) {
        // Náusea intensa para simular sacudida
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, duracionSegundos * 20, 2, true, false));
        
        // Títulos parpadeantes para intensificar el efecto
        BukkitRunnable shakeTask = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= duracionSegundos * 20) {
                    cancel();
                    return;
                }
                // Alterna entre símbolos para crear efecto de sacudida visual
                String symbol = (ticks % 10 < 5) ? "§c✦" : "§4✧";
                player.sendTitle(symbol, "", 0, 5, 0);
                ticks += 5;
            }
        };
        shakeTask.runTaskTimer(plugin, 0L, 5L);
        
        // Partículas de explosión para reforzar impacto
        Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.EXPLOSION, loc, 5, 1, 1, 1, 0);
    }
    
    /**
     * Efecto de zoom in (acercamiento) con partículas convergentes.
     * Crea sensación de enfoque en un momento importante.
     */
    private void aplicarZoomIn(Player player, int duracionSegundos) {
        // Lentitud leve para simular acercamiento
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duracionSegundos * 20, 0, true, false));
        
        // Partículas convergentes hacia el centro
        BukkitRunnable zoomTask = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= duracionSegundos * 20) {
                    cancel();
                    return;
                }
                Location loc = player.getEyeLocation();
                // Partículas que convergen hacia el centro de la pantalla
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 * i) / 8;
                    double radius = 2.0 - (ticks / (duracionSegundos * 20.0));
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    player.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(x, 0, z), 1, 0, 0, 0, 0);
                }
                ticks += 2;
            }
        };
        zoomTask.runTaskTimer(plugin, 0L, 2L);
        
        player.sendTitle("§d§l⬤", "§5Enfocando...", 5, duracionSegundos * 20, 10);
    }
    
    /**
     * Efecto de zoom out (alejamiento) con partículas divergentes.
     * Crea sensación de revelación o perspectiva ampliada.
     */
    private void aplicarZoomOut(Player player, int duracionSegundos) {
        // Velocidad leve para simular alejamiento
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duracionSegundos * 20, 0, true, false));
        
        // Partículas divergentes desde el centro
        BukkitRunnable zoomTask = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= duracionSegundos * 20) {
                    cancel();
                    return;
                }
                Location loc = player.getEyeLocation();
                // Partículas que divergen desde el centro
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 * i) / 8;
                    double radius = (ticks / (duracionSegundos * 20.0)) * 3.0;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(x, 0, z), 1, 0, 0, 0, 0);
                }
                ticks += 2;
            }
        };
        zoomTask.runTaskTimer(plugin, 0L, 2L);
        
        player.sendTitle("§5§l◯", "§d¡Revelación!", 5, duracionSegundos * 20, 10);
    }
    
    /**
     * Efecto cinematográfico épico combinado: zoom + slow motion + shake.
     * Usado en momentos climáticos del evento.
     */
    private void aplicarEfectoEpicoCombinado(Player player) {
        // Fase 1: Zoom in + slow motion (3 segundos)
        aplicarZoomIn(player, 3);
        aplicarSlowMotion(player, 3);
        
        // Fase 2: Screen shake intenso (2 segundos, tras 3 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            aplicarScreenShakeIntenso(player, 2);
        }, 60L);
        
        // Fase 3: Zoom out final (2 segundos, tras 5 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            aplicarZoomOut(player, 2);
        }, 100L);
        
        // Mensaje épico
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendTitle("§5§l✦ §d§lÉPICO §5§l✦", "§dUn momento legendario", 10, 40, 20);
        }, 80L);
    }
    
    /**
     * Efecto de distorsión dimensional (colores alternados).
     */
    private void efectoDistorsionDimensional(Player player, int duracion) {
        String[] colores = {"§5", "§d", "§9", "§b", "§3", "§1"};
        int ciclos = duracion / 4;
        
        for (int i = 0; i < ciclos; i++) {
            final int index = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    String color = colores[index % colores.length];
                    player.sendTitle(
                        color + "▓▓▓▓▓▓▓▓▓",
                        "",
                        0, 4, 0
                    );
                }
            }, i * 4L);
        }
    }
    
    /**
     * Efecto de distorsión dimensional para todos.
     */
    private void efectoDistorsionDimensionalTodos(int duracion) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            efectoDistorsionDimensional(p, duracion);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE MENSAJES CINEMATOGRÁFICOS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Envía un título cinematográfico animado con efectos de gradiente.
     */
    private void enviarTituloCinematico(Player player, String titulo, String subtitulo, int duracion) {
        // Si el título ya tiene formato especial (§k para ofuscado, etc), no aplicar gradiente
        String tituloFinal = titulo.contains("§k") || titulo.contains("§l§") ? titulo : formatearGradiente(titulo);
        player.sendTitle(
            tituloFinal,
            formatearItalico(subtitulo),
            10, duracion, 20
        );
    }
    
    /**
     * Envía un título cinematográfico a todos los jugadores.
     */
    private void enviarTituloCinematicoTodos(String titulo, String subtitulo, int duracion) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            enviarTituloCinematico(p, titulo, subtitulo, duracion);
        }
    }
    
    /**
     * Envía un mensaje cinematográfico con borde decorativo.
     */
    private void enviarMensajeCinematico(Player player, String mensaje) {
        enviarMensajeCinematicoAnimado(player, mensaje, 0);
    }
    
    /**
     * Envía un mensaje cinematográfico con animación de aparición.
     */
    private void enviarMensajeCinematicoAnimado(Player player, String mensaje, long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            // Efecto de fade-in con títulos
            player.sendTitle("", "§8§l◆", 0, 5, 5);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage("");
                player.sendMessage("§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                
                // Efecto typewriter
                String[] palabras = mensaje.split(" ");
                StringBuilder acumulado = new StringBuilder();
                
                for (int i = 0; i < palabras.length; i++) {
                    final int indice = i;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        acumulado.append(palabras[indice]).append(" ");
                        player.sendMessage(formatearCentrado(acumulado.toString().trim()));
                        
                        // Sonido sutil por palabra
                        if (indice % 2 == 0) {
                            soundUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_HAT, 0.1f, 1.5f);
                        }
                    }, i * 4L);
                }
                
                // Borde inferior después del texto
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendMessage("§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                    player.sendMessage("");
                }, palabras.length * 4L + 10L);
            }, 10L);
        }, delay);
    }
    
    /**
     * Envía un mensaje de descubrimiento épico.
     */
    private void enviarMensajeDescubrimiento(Player player, String item, int actual, int total) {
        enviarMensajeDescubrimientoAnimado(player, item, actual, total);
    }
    
    /**
     * Envía un mensaje de descubrimiento con animación épica.
     */
    private void enviarMensajeDescubrimientoAnimado(Player player, String item, int actual, int total) {
        // Destello inicial
        player.sendTitle("§5§l◆", "§d§l✦", 2, 10, 8);
        soundUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.4f, 1.5f);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            player.sendMessage("");
            
            // Animación de borde superior expandiéndose
            String[] frames = {
                "§8§m        §r §5§l✦§r §8§m        ",
                "§8§m            §r §5§l✦§r §8§m            ",
                "§8§m                §r §5§l✦§r §8§m                ",
                "§8§m                    §r §5§l✦§r §8§m                    "
            };
            
            for (int i = 0; i < frames.length; i++) {
                final int indice = i;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendMessage(formatearCentrado(frames[indice]));
                    soundUtil.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.2f, 1.0f + (indice * 0.2f));
                }, i * 3L);
            }
            
            // Título con gradiente
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(formatearCentrado(formatearGradiente(item)));
                soundUtil.playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.2f);
            }, 15L);
            
            // Barra de progreso animada
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(formatearCentrado("§7[" + crearBarraProgreso(actual, total, 20, '█', '░') + "§7]"));
                player.sendMessage(formatearCentrado("§f" + actual + " §7/§f " + total));
            }, 25L);
            
            // Borde inferior
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(formatearCentrado("§8§m                    §r §5§l✦§r §8§m                    "));
                player.sendMessage("");
            }, 35L);
        }, 5L);
    }
    
    /**
     * Crea una barra de progreso visual.
     */
    private String crearBarraProgreso(int actual, int total, int longitud, char lleno, char vacio) {
        int progreso = (int)((double)actual / total * longitud);
        StringBuilder barra = new StringBuilder("§a");
        
        for (int i = 0; i < longitud; i++) {
            if (i < progreso) {
                barra.append(lleno);
            } else {
                if (i == progreso) {
                    barra.append("§8");
                }
                barra.append(vacio);
            }
        }
        
        return barra.toString();
    }
    
    /**
     * Formatea texto con gradiente de colores.
     */
    private String formatearGradiente(String texto) {
        // Gradiente púrpura-azul-cyan para efecto místico
        String[] colores = {"§5", "§d", "§9", "§b", "§3"};
        StringBuilder resultado = new StringBuilder();
        String textoLimpio = texto.replaceAll("§.", "");
        
        for (int i = 0; i < textoLimpio.length(); i++) {
            char c = textoLimpio.charAt(i);
            if (c == ' ') {
                resultado.append(' ');
            } else {
                int indiceColor = (i * colores.length) / textoLimpio.length();
                resultado.append(colores[indiceColor]).append(c);
            }
        }
        
        return resultado.toString();
    }
    
    /**
     * Muestra mensaje narrativo con animación sutil.
     */
    private void mostrarMensajeNarrativoAnimado(Player player, String mensaje, long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            // Título sutil
            player.sendTitle("", "§8§o⧖", 5, 15, 10);
            
            // Mensaje con fade-in
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(formatearCentrado(mensaje));
                
                // Sonido ambiental
                soundUtil.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.2f, 0.8f);
                
                // Partículas sutiles
                Location loc = player.getLocation().add(0, 2, 0);
                loc.getWorld().spawnParticle(Particle.ENCHANT, loc, 5, 0.5, 0.5, 0.5, 0.1);
            }, 10L);
        }, delay);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE NARRATIVA - DIÁLOGOS DE LA FORMA
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * 🎭 PENSAMIENTOS DEL OBSERVADOR
     * Mensajes estilizados que aparecen en momentos clave del evento.
     * El Observador deja pensamientos crípticos sobre los glitches del mundo.
     */
    private void mostrarDialogoForma(String contexto) {
        long ahora = System.currentTimeMillis();
        if (ahora - ultimoDialogoForma < 25000) return; // Cooldown 25 segundos (más tiempo para leer)
        
        String dialogo = obtenerDialogoForma(contexto);
        if (dialogo == null || dialogosFormaUsados.contains(dialogo)) return;
        
        dialogosFormaUsados.add(dialogo);
        ultimoDialogoForma = ahora;
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participantesOriginales.contains(p.getUniqueId())) {
                mostrarDialogoFormaAnimado(p, dialogo);
            }
        }
    }
    
    /**
     * Obtiene un diálogo apropiado según el contexto.
     */
    private String obtenerDialogoForma(String contexto) {
        List<String> dialogos = new ArrayList<>();
        
        switch (contexto) {
            case "INICIO":
                dialogos.add("§5§l◈ §8§o\"...algo se mueve en la piedra...\"");
                dialogos.add("§5§l◈ §8§o\"...no debería estar aquí...\"");
                dialogos.add("§5§l◈ §8§o\"...recuerdos rotos...\"");
                break;
                
            case "FRAGMENTO_ENCONTRADO":
                dialogos.add("§5§l◈ §8§o\"...esto no pertenece aquí...\"");
                dialogos.add("§5§l◈ §8§o\"...una pieza del puzzle roto...\"");
                dialogos.add("§5§l◈ §8§o\"...el mundo glitchea...\"");
                break;
                
            case "ACTO2_INICIO":
                dialogos.add("§5§l◈ §8§o\"...veis aquella grieta? No es una herida... es una cicatriz...\"");
                dialogos.add("§5§l◈ §8§o\"...hace eones, sus antepasados sellaron algo aquí...\"");
                dialogos.add("§5§l◈ §8§o\"...los fragmentos que activasteis eran las llaves del sello...\"");
                dialogos.add("§5§l◈ §8§o\"...y ahora... lo que ellos encerraron... busca venganza...\"");
                dialogos.add("§5§l◈ §8§o\"...defended el altar. Si la Forma os alcanza... todo terminará...\"");
                break;
                
            case "CRIATURAS_SPAWN":
                dialogos.add("§5§l◈ §8§o\"...copias defectuosas... recuerdos fragmentados de lo que fue...\"");
                dialogos.add("§5§l◈ §8§o\"...no son reales... pero su odio sí lo es...\"");
                break;
                
            case "JUGADOR_MUERTE":
                dialogos.add("§5§l◈ §8§o\"...la vida se desconecta...\"");
                dialogos.add("§5§l◈ §8§o\"...otro bug en el sistema...\"");
                dialogos.add("§5§l◈ §8§o\"...demasiado frágil...\"");
                break;
                
            case "OLEADA_COMPLETADA":
                dialogos.add("§5§l◈ §8§o\"...pausa momentánea... pero la Forma no descansa...\"");
                dialogos.add("§5§l◈ §8§o\"...preparaos. La memoria se intensifica...\"");
                break;
                
            case "OLEADA_2_INICIO":
                dialogos.add("§5§l◈ §8§o\"...la memoria se intensifica... busca restaurarse...\"");
                dialogos.add("§5§l◈ §8§o\"...no dejéis que os toquen. Un roce... y os verán como enemigos eternos...\"");
                break;
                
            case "OLEADA_3_INICIO":
                dialogos.add("§5§l◈ §8§o\"...¡el núcleo de la Forma despierta!...\"");
                dialogos.add("§5§l◈ §8§o\"...esta es la última barrera... defended el altar o caerá todo el sello!...\"");
                break;
                
            case "VICTORIA_ACTO2":
                dialogos.add("§5§l◈ §8§o\"...¡lo lograron!... la Forma vuelve a dormir... por ahora...\"");
                dialogos.add("§5§l◈ §8§o\"...pero su núcleo aún late... dentro de la grieta...\"");
                dialogos.add("§5§l◈ §8§o\"...si queréis terminar esto... debéis entrar...\"");
                break;
                
            case "ACTO3_INICIO":
                dialogos.add("§5§l◈ §8§o\"...la grieta os llama... podéis sentirlo, ¿verdad?...\"");
                dialogos.add("§5§l◈ §8§o\"...dentro late el corazón de la Forma... su memoria primordial...\"");
                dialogos.add("§5§l◈ §8§o\"...descended al vacío. Buscad la luz violeta en la oscuridad...\"");
                dialogos.add("§5§l◈ §8§o\"...y cuando lo encontréis... destruídlo. O consumirá todo lo que queda...\"");
                break;
                
            case "CERCA_NUCLEO":
                dialogos.add("§5§l◈ §8§o\"...sentís su odio... su miedo... su desesperación...\"");
                dialogos.add("§5§l◈ §8§o\"...fue traicionado una vez... no permitirá que pase de nuevo...\"");
                break;
                
            case "NUCLEO_APARECE":
                dialogos.add("§5§l◈ §8§o\"...ahí está... la memoria primordial palpita...\"");
                dialogos.add("§5§l◈ §8§o\"...cuidado... no os dejará acercaros sin luchar...\"");
                break;
                
            case "NUCLEO_RECOGIDO":
                dialogos.add("§5§l◈ §8§o\"...lo tenéis... pero aún late. Aún vive...\"");
                dialogos.add("§5§l◈ §8§o\"...llevadlo al altar. Allí lo sellaremos... para siempre...\"");
                break;
                
            case "RITUAL_DESTRUCCION":
                dialogos.add("§5§l◈ §8§o\"...¡Todos juntos! ¡El núcleo intenta resistir!...\"");
                dialogos.add("§5§l◈ §8§o\"...no os apartéis del altar... o fallaremos...\"");
                break;
                
            case "NUCLEO_DESTRUIDO":
                dialogos.add("§5§l◈ §8§o\"...se terminó... la Forma ha sido destruida...\"");
                dialogos.add("§5§l◈ §8§o\"...pero su eco permanece... en algún lugar del vacío...\"");
                dialogos.add("§5§l◈ §8§o\"...salvaron este mundo... pero otros aún esperan...\"");
                break;
                
            case "FINAL_EXITO":
                dialogos.add("§5§l◈ §8§o\"...fragmentos recolectados...\"");
                dialogos.add("§5§l◈ §8§o\"...pero el eco permanece...\"");
                dialogos.add("§5§l◈ §8§o\"...algo más grande despierta... en el vacío...\"");
                break;
                
            case "FINAL_FRACASO":
                dialogos.add("§5§l◈ §8§o\"...proceso interrumpido...\"");
                dialogos.add("§5§l◈ §8§o\"...los fragmentos aún esperan...\"");
                dialogos.add("§5§l◈ §8§o\"...el susurro persiste en la piedra...\"");
                break;
        }
        
        if (dialogos.isEmpty()) return null;
        return dialogos.get(new Random().nextInt(dialogos.size()));
    }
    
    /**
     * Animación visual para diálogos de La Forma.
     */
    private void mostrarDialogoFormaAnimado(Player player, String dialogo) {
        // 🎬 PAUSA DRAMÁTICA: Slow motion para enfocar en el diálogo
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.SLOWNESS,
            100, // 5 segundos
            2,
            true,
            false
        ));
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.SLOW_FALLING,
            100,
            0,
            true,
            false
        ));
        
        // 🎬 Efecto de distorsión visual MÁS LARGO
        player.sendTitle("§5§l◈", "§8§o...escucha...", 10, 80, 15);
        
        // 🔊 CAPA 1: Sonidos dimensionales base
        soundUtil.playSound(player, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.6f);
        soundUtil.playSound(player, Sound.ENTITY_WARDEN_AMBIENT, 0.4f, 0.8f);
        
        // 🔊 CAPA 2: Susurros y ecos después de 0.3s
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            soundUtil.playSound(player, Sound.ENTITY_ENDERMAN_AMBIENT, 0.6f, 0.5f);
            soundUtil.playSound(player, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.4f, 0.7f);
        }, 6L);
        
        // 🔊 CAPA 3: Portal dimensiónal después de 0.5s
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            soundUtil.playSound(player, Sound.BLOCK_PORTAL_TRAVEL, 0.3f, 0.4f);
        }, 10L);
        
        // 📖 Mostrar el diálogo más lento y con más énfasis
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage("");
            player.sendMessage("");
            player.sendMessage(formatearCentrado("§5§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            player.sendMessage("");
            player.sendMessage(formatearCentrado(dialogo));
            player.sendMessage("");
            player.sendMessage(formatearCentrado("§5§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            player.sendMessage("");
            player.sendMessage("");
            
            // ✨ Partículas de susurro más intensas
            Location loc = player.getLocation().add(0, 2, 0);
            
            // Círculo de partículas místicas
            for (int i = 0; i < 20; i++) {
                double angle = Math.toRadians(i * 18);
                double x = Math.cos(angle) * 2;
                double z = Math.sin(angle) * 2;
                loc.getWorld().spawnParticle(
                    Particle.SOUL_FIRE_FLAME,
                    loc.clone().add(x, 0, z),
                    2,
                    0.1, 0.1, 0.1,
                    0.02
                );
            }
            
            loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc, 30, 1.0, 0.5, 1.0, 0.1);
            loc.getWorld().spawnParticle(Particle.SMOKE, loc, 15, 0.5, 0.3, 0.5, 0.03);
            
            // 🔊 Eco final más fuerte
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                soundUtil.playSound(player, Sound.ENTITY_ENDERMAN_AMBIENT, 0.5f, 0.6f);
                soundUtil.playSound(player, Sound.ENTITY_WARDEN_HEARTBEAT, 0.3f, 0.5f);
            }, 15L);
        }, 20L);
        
        // 🎬 Oscurecer pantalla durante el diálogo
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.BLINDNESS,
            60, // 3 segundos
            0,
            true,
            false
        ));
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE AUDIO DINÁMICO
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * 🎵 SISTEMA DE AUDIO DINÁMICO
     * La intensidad del audio cambia según el peligro y proximidad a enemigos.
     */
    private void iniciarSistemaAudioDinamico() {
        if (audioTask != null) {
            audioTask.cancel();
        }
        
        audioTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isActive()) {
                if (audioTask != null) audioTask.cancel();
                return;
            }
            
            // Calcular intensidad basada en estado actual
            calcularIntensidadAudio();
            
            // Reproducir sonidos ambientales según intensidad
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (participantesOriginales.contains(p.getUniqueId())) {
                    reproducirAudioAmbiente(p);
                }
            }
        }, 0L, 100L); // Cada 5 segundos
    }
    
    /**
     * Calcula la intensidad del audio basándose en múltiples factores.
     */
    private void calcularIntensidadAudio() {
        int nuevaIntensidad = 0;
        
        // Base por acto
        switch (actoActual) {
            case PIEDRA_DESPIERTA -> nuevaIntensidad = 20;
            case PIEDRA_QUIEBRA -> nuevaIntensidad = 50;
            case NUCLEO_FORMA -> nuevaIntensidad = 70;
            default -> nuevaIntensidad = 10;
        }
        
        // Aumentar con criaturas activas
        int criaturas = criaturasActivas.size();
        nuevaIntensidad += Math.min(criaturas * 5, 20);
        
        // Aumentar si hay jugadores en peligro (baja vida)
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participantesOriginales.contains(p.getUniqueId())) {
                double saludPorcentaje = p.getHealth() / p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                if (saludPorcentaje < 0.3) {
                    nuevaIntensidad += 15;
                    break;
                }
            }
        }
        
        // Suavizar transiciones (no cambios bruscos)
        if (nuevaIntensidad > intensidadAudio) {
            intensidadAudio = Math.min(intensidadAudio + 10, nuevaIntensidad);
        } else {
            intensidadAudio = Math.max(intensidadAudio - 5, nuevaIntensidad);
        }
        
        intensidadAudio = Math.max(0, Math.min(100, intensidadAudio));
    }
    
    /**
     * Reproduce sonidos ambientales según la intensidad actual.
     */
    private void reproducirAudioAmbiente(Player player) {
        if (intensidadAudio < 30) {
            // Ambiente tranquilo
            if (Math.random() < 0.3) {
                soundUtil.playSound(player, Sound.AMBIENT_CAVE, 0.2f, 0.8f);
            }
        } else if (intensidadAudio < 60) {
            // Tensión media
            if (Math.random() < 0.5) {
                soundUtil.playSound(player, Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 0.7f);
                soundUtil.playSound(player, Sound.ENTITY_ENDERMAN_AMBIENT, 0.2f, 0.6f);
            }
        } else {
            // Alta tensión
            if (Math.random() < 0.7) {
                soundUtil.playSound(player, Sound.ENTITY_WARDEN_HEARTBEAT, 0.4f, 0.9f);
                soundUtil.playSound(player, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.3f, 0.8f);
                soundUtil.playSound(player, Sound.AMBIENT_BASALT_DELTAS_LOOP, 0.2f, 0.5f);
            }
        }
        
        // Susurros aleatorios (muy ocasionales)
        if (Math.random() < 0.05) {
            soundUtil.playSound(player, Sound.ENTITY_VEX_AMBIENT, 0.15f, 0.5f);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE LORE COLECCIONABLE
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * 📖 Genera libros de lore en los altares de fragmentos.
     */
    private void generarLibrosLore() {
        if (fragmentosLocations.isEmpty()) return;
        
        // Generar 3 libros en altares aleatorios
        List<Location> altaresConLibro = new ArrayList<>(fragmentosLocations);
        Collections.shuffle(altaresConLibro);
        
        int librosGenerados = 0;
        for (int i = 0; i < Math.min(3, altaresConLibro.size()); i++) {
            Location altarLoc = altaresConLibro.get(i);
            ItemStack libro = crearLibroLore(i + 1);
            
            // Colocar libro en un lectern
            Location lecternLoc = altarLoc.clone().add(0, 4, 0);
            lecternLoc.getBlock().setType(Material.LECTERN);
            
            // Añadir item frame invisible con el libro como decoración extra
            Location frameLoc = altarLoc.clone().add(0, 5, 0);
            ItemFrame frame = (ItemFrame) altarLoc.getWorld().spawnEntity(frameLoc, EntityType.ITEM_FRAME);
            frame.setVisible(false);
            frame.setFixed(true);
            frame.setItem(libro);
            frame.setRotation(Rotation.NONE);
            
            librosLore.add(frame);
            librosGenerados++;
            
            // Partículas mágicas sobre el libro
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (isActive()) {
                    lecternLoc.getWorld().spawnParticle(Particle.ENCHANT, lecternLoc.clone().add(0.5, 1.5, 0.5), 30, 0.3, 0.5, 0.3, 0.05);
                    playSoundToAll(Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.2f);
                }
            }, (i + 1) * 40L);
        }
        
        plugin.getLogger().info("[SusurroPiedraRota] Generados " + librosGenerados + " libros de lore");
        broadcastNarrative("§d✨ Libros antiguos han aparecido en algunos altares...");
    }
    
    /**
     * Crea un libro de lore con historia del evento.
     */
    private ItemStack crearLibroLore(int numero) {
        ItemStack libro = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) libro.getItemMeta();
        
        meta.setTitle("§5Fragmento de Memoria " + numero);
        meta.setAuthor("§8???");
        
        switch (numero) {
            case 1:
                meta.addPage(
                    "§0§lCapítulo I\n§r§0La Piedra Rota\n\n" +
                    "§0Hace eones, una entidad " +
                    "conocida como §5La Forma§0 " +
                    "existía más allá del tiempo.\n\n" +
                    "§0No tenía cuerpo, solo " +
                    "voluntad. No tenía voz, " +
                    "solo §8susurros§0..."
                );
                meta.addPage(
                    "§0Un día, §5La Forma§0 " +
                    "intentó manifestarse en " +
                    "nuestro mundo.\n\n" +
                    "§0Pero algo salió mal.\n\n" +
                    "§0Se §8fragmentó§0.\n" +
                    "Se §8dispersó§0.\n" +
                    "Se §8rompió§0 en mil pedazos."
                );
                break;
                
            case 2:
                meta.addPage(
                    "§0§lCapítulo II\n§r§0Los Susurros\n\n" +
                    "§0Los fragmentos quedaron " +
                    "atrapados en piedras " +
                    "antiguas, esperando.\n\n" +
                    "§0Susurrando.\n" +
                    "§8Llamando§0.\n" +
                    "§8Buscando§0 ser completo " +
                    "nuevamente."
                );
                meta.addPage(
                    "§0Aquellos que escuchan " +
                    "los susurros sienten una " +
                    "§8presencia§0 observándolos.\n\n" +
                    "§0Una §5entidad§0 sin forma " +
                    "tratando de §8recordar§0 " +
                    "quién era.\n\n" +
                    "§0¿Puedes ayudarla?"
                );
                break;
                
            case 3:
                meta.addPage(
                    "§0§lCapítulo III\n§r§0El Núcleo\n\n" +
                    "§0Cuando todos los fragmentos " +
                    "se reúnen, §5La Forma§0 " +
                    "puede manifestarse.\n\n" +
                    "§0Su §5núcleo§0 contiene su " +
                    "esencia pura."
                );
                meta.addPage(
                    "§0Pero ten cuidado...\n\n" +
                    "§0Reunir los fragmentos " +
                    "también despierta a sus " +
                    "§8guardianes§0.\n\n" +
                    "§0Criaturas de forma pura " +
                    "que protegen lo que queda " +
                    "de §5La Forma§0."
                );
                meta.addPage(
                    "§0§oSi estás leyendo esto,\n" +
                    "§0§oya es tarde para huir.\n\n" +
                    "§5§oLa Forma§0§o te ha elegido.\n\n" +
                    "§8§oCompleta su recuerdo...\n" +
                    "§8§o...o conviértete en parte\n" +
                    "§8§ode él para siempre.\n\n" +
                    "§0§l- El Observador"
                );
                break;
        }
        
        libro.setItemMeta(meta);
        return libro;
    }
    
    /**
     * Registra que un jugador recolectó un fragmento de lore.
     */
    private void registrarLoreRecolectado(Player player, String loreId) {
        loreRecolectado.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(loreId);
        
        player.sendMessage("§d✨ §7Has descubierto un fragmento de la historia...");
        soundUtil.playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.5f);
        
        // Bonus por coleccionar lore
        int loreCount = loreRecolectado.get(player.getUniqueId()).size();
        rendimientoJugador.put(player.getUniqueId(), 
            rendimientoJugador.getOrDefault(player.getUniqueId(), 0) + 50);
        
        if (loreCount >= 3) {
            player.sendMessage("§5§l✦ §d¡Has recolectado toda la historia! §5+150 puntos");
            rendimientoJugador.put(player.getUniqueId(), 
                rendimientoJugador.get(player.getUniqueId()) + 100);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE FINAL ALTERNATIVO
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * 🏆 Calcula el rank final de cada jugador basado en su rendimiento.
     * Ranks: S (Perfecto), A (Excelente), B (Bueno), C (Completado)
     */

    
    /**
     * Formatea tiempo en formato MM:SS
     */
    private String formatearTiempo(long segundos) {
        long minutos = segundos / 60;
        long segs = segundos % 60;
        return String.format("%02d:%02d", minutos, segs);
    }
    
    /**
     * Crea una órbita de partículas alrededor de un centro.
     */
    private void crearOrbitaParticulas(Location centro, double radio, double angulo, Particle particula, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            double anguloActual = angulo + (Math.PI * 2 / cantidad) * i;
            double x = Math.cos(anguloActual) * radio;
            double z = Math.sin(anguloActual) * radio;
            
            centro.getWorld().spawnParticle(
                particula,
                centro.clone().add(x, 0, z),
                1,
                0, 0, 0,
                0
            );
        }
    }
    
    /**
     * Muestra susurro de fragmento con efecto inquietante.
     */
    private void mostrarSusurroFragmento(Player player, String susurro, long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            // Oscurecer momentáneamente
            player.sendTitle("§0§l▓", "", 0, 5, 5);
            
            // Susurro palabra por palabra
            String textoLimpio = susurro.replaceAll("§.", "");
            String[] palabras = textoLimpio.split(" ");
            
            for (int i = 0; i < palabras.length; i++) {
                final int indice = i;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // Efecto de texto parpadeante
                    String palabra = palabras[indice];
                    player.sendMessage(formatearCentrado(susurro));
                    
                    // Sonido de susurro escalofriante
                    soundUtil.playSound(player, Sound.ENTITY_ENDERMAN_STARE, 0.15f, 0.5f + (indice * 0.05f));
                    soundUtil.playSound(player, Sound.AMBIENT_CAVE, 0.1f, 1.5f);
                    
                    // Partículas oscuras
                    Location eyeLoc = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.5));
                    eyeLoc.getWorld().spawnParticle(Particle.SMOKE, eyeLoc, 2, 0.1, 0.1, 0.1, 0.02);
                }, i * 8L);
            }
        }, delay);
    }
    
    /**
     * Formatea texto en itálico con color oscuro.
     */
    private String formatearItalico(String texto) {
        return "§7§o" + texto;
    }
    
    /**
     * Centra texto en el chat (aproximación).
     */
    private String formatearCentrado(String texto) {
        // Minecraft chat tiene ~53 caracteres de ancho
        String textoLimpio = texto.replaceAll("§.", "");
        int espacios = (53 - textoLimpio.length()) / 2;
        StringBuilder centrado = new StringBuilder();
        
        for (int i = 0; i < espacios; i++) {
            centrado.append(" ");
        }
        
        centrado.append(texto);
        return centrado.toString();
    }
    
    /**
     * Crea una barra de progreso visual.
     */
    private String crearBarraProgreso(int actual, int total) {
        int longitudBarra = 20;
        int completado = (actual * longitudBarra) / total;
        
        StringBuilder barra = new StringBuilder("§7[");
        for (int i = 0; i < longitudBarra; i++) {
            if (i < completado) {
                barra.append("§a■");
            } else {
                barra.append("§8□");
            }
        }
        barra.append("§7] §e").append(actual).append("§7/§e").append(total);
        
        return barra.toString();
    }
    
    /**
     * Crea barra de progreso con colores personalizables
     */
    private String crearBarraProgreso(int actual, int total, String colorCompletado, String colorPendiente) {
        int longitudBarra = 10;
        int completado = (actual * longitudBarra) / total;
        
        StringBuilder barra = new StringBuilder("§7[");
        for (int i = 0; i < longitudBarra; i++) {
            if (i < completado) {
                barra.append(colorCompletado).append("■");
            } else {
                barra.append(colorPendiente).append("□");
            }
        }
        barra.append("§7]");
        
        return barra.toString();
    }
    
    /**
     * Crea visual de checkpoint en una ubicación.
     */
    private void crearCheckpointVisual(Location loc, int numero) {
        World world = loc.getWorld();
        
        // Pilar de luz de checkpoint
        for (int y = 0; y < 8; y++) {
            world.spawnParticle(
                Particle.END_ROD,
                loc.clone().add(0, y * 0.5, 0),
                3,
                0.1, 0, 0.1,
                0
            );
        }
        
        // Anillo expandible en el suelo
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 20) return; // 1 segundo
                
                double radio = 0.5 + (ticks * 0.15);
                for (int angle = 0; angle < 360; angle += 20) {
                    double rad = Math.toRadians(angle);
                    double x = Math.cos(rad) * radio;
                    double z = Math.sin(rad) * radio;
                    world.spawnParticle(
                        Particle.GLOW,
                        loc.clone().add(x, 0.1, z),
                        1,
                        0, 0, 0,
                        0
                    );
                }
                
                ticks++;
            }
        }, 0L, 1L);
        
        // Número flotante
        world.spawnParticle(
            Particle.ENCHANT,
            loc.clone().add(0, 2, 0),
            numero * 5,
            0.3, 0.5, 0.3,
            0.1
        );
    }
    
    /**
     * Inicia mini-cinematic de victoria al completar fragmentos.
     */
    // Método de cinemática eliminado - no hay puzzle de fragmentos
    
    /**
     * Genera símbolos visuales para oleadas.
     */
    private String generarSimbolosOleada(int actual, int total) {
        StringBuilder simbolos = new StringBuilder();
        for (int i = 1; i <= total; i++) {
            if (i < actual) {
                simbolos.append("§a✓");
            } else if (i == actual) {
                simbolos.append("§e◆");
            } else {
                simbolos.append("§8○");
            }
            if (i < total) simbolos.append(" ");
        }
        return simbolos.toString();
    }
    
    /**
     * Reproduce sonidos de tensión progresiva según la oleada.
     */
    private void reproducirSonidosTension(int oleadaActual, int oleadasTotales) {
        float tension = (float) oleadaActual / oleadasTotales;
        
        if (oleadaActual == 1) {
            // Primera oleada - sonidos ominosos
            playSoundToAll(Sound.AMBIENT_CAVE, 0.8f, 0.8f);
            playSoundToAll(Sound.ENTITY_WARDEN_AMBIENT, 0.3f, 0.5f);
        } else if (oleadaActual == 2) {
            // Segunda oleada - aumenta tensión
            playSoundToAll(Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 1.0f);
            playSoundToAll(Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.4f, 0.8f);
            playSoundToAll(Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.5f, 0.9f);
        } else {
            // Última oleada - máxima tensión
            playSoundToAll(Sound.ENTITY_WARDEN_ROAR, 0.7f, 1.2f);
            playSoundToAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.7f);
            playSoundToAll(Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.6f, 0.5f);
        }
    }
    
    /**
     * Reproduce sonidos ambientales misteriosos para crear atmósfera.
     */
    private void reproducirSonidoAmbientalMisterioso() {
        playSoundToAll(Sound.AMBIENT_CAVE, 0.5f, 0.7f);
        
        // Sonidos adicionales escalonados
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            playSoundToAll(Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 0.6f);
        }, 20L);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            playSoundToAll(Sound.ENTITY_ENDERMAN_STARE, 0.4f, 0.5f);
        }, 40L);
    }
    
    /**
     * Sistema de hints progresivos para fragmentos.
     */
    private void verificarHintsFragmentos() {
        int fragmentosEncontrados = fragmentosInspeccionados.size();
        int fragmentosTotales = fragmentosLocations.size();
        
        // Hint cada 30 segundos (600 ticks) si no hay progreso
        if (ticksEnActo - ultimoHintFragmentos >= 600) {
            if (fragmentosEncontrados < fragmentosTotales) {
                int restantes = fragmentosTotales - fragmentosEncontrados;
                
                // ⚠ ORDEN CORRECTO: Evaluar de mayor a menor para que se ejecute la pista correcta
                if (ticksEnActo > 2400) { // Después de 2 minutos - PISTA MÁS ESPECÍFICA
                    // Revelar ubicación aproximada del fragmento más cercano
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (!participantesOriginales.contains(p.getUniqueId())) continue;
                        
                        // Buscar el fragmento NO descubierto más cercano
                        Location masCercano = null;
                        double distMin = Double.MAX_VALUE;
                        
                        for (Location frag : fragmentosLocations) {
                            if (!fragmentosInspeccionados.contains(frag)) {
                                double dist = p.getLocation().distance(frag);
                                if (dist < distMin) {
                                    distMin = dist;
                                    masCercano = frag;
                                }
                            }
                        }
                        
                        if (masCercano != null) {
                            String direccion = obtenerDireccion(p.getLocation(), masCercano);
                            p.sendMessage("§e⚙ PISTA ESPECÍFICA: §fUn fragmento está a §e" + (int)distMin + " bloques §fhacia " + direccion);
                            // Efecto visual de pista
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.5f);
                        }
                    }
                } else if (ticksEnActo > 1800) { // Después de 1.5 minutos
                    broadcastNarrative("§e⚙ PISTA: §fUsa el §eAction Bar §f(arriba de tu inventario) para ver la distancia");
                    broadcastNarrative("§7Mira las §fflechas direccionales §7para saber hacia dónde ir");
                    playSoundToAll(Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.2f);
                } else if (ticksEnActo > 1200) { // Después de 1 minuto
                    broadcastNarrative("§e⚙ PISTA: §fBusca las §epartículas §fmás §ebrillantes §f(rayos verticales blancos)");
                    broadcastNarrative("§7Los fragmentos emiten §f§lrayos de luz §r§7que suben hasta el cielo");
                    playSoundToAll(Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.0f);
                }
                
                ultimoHintFragmentos = ticksEnActo;
            }
        }
    }
    
    /**
     * Obtiene dirección cardinal aproximada.
     */
    private String obtenerDireccion(Location desde, Location hacia) {
        double dx = hacia.getX() - desde.getX();
        double dz = hacia.getZ() - desde.getZ();
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        
        if (angle < 0) angle += 360;
        
        if (angle >= 337.5 || angle < 22.5) return "§eel ESTE";
        if (angle >= 22.5 && angle < 67.5) return "§eel SURESTE";
        if (angle >= 67.5 && angle < 112.5) return "§eel SUR";
        if (angle >= 112.5 && angle < 157.5) return "§eel SUROESTE";
        if (angle >= 157.5 && angle < 202.5) return "§eel OESTE";
        if (angle >= 202.5 && angle < 247.5) return "§eel NOROESTE";
        if (angle >= 247.5 && angle < 292.5) return "§eel NORTE";
        return "§eel NORESTE";
    }
    
    /**
     * Sistema de combos para combate.
     */
    private void registrarKillCombo(Player player) {
        UUID uuid = player.getUniqueId();
        long ahora = System.currentTimeMillis();
        long ultimoKill = ultimoKillJugador.getOrDefault(uuid, 0L);
        
        // Si mataste dentro de 5 segundos, aumenta combo
        if (ahora - ultimoKill < 5000) {
            int combo = combosJugador.getOrDefault(uuid, 0) + 1;
            combosJugador.put(uuid, combo);
            
            // Feedback de combo
            if (combo >= 3) {
                player.sendTitle(
                    "",
                    "§e§l⚔ COMBO x" + combo + " ⚔",
                    5, 20, 5
                );
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f + (combo * 0.1f));
                
                // Efecto visual
                player.spawnParticle(
                    Particle.ENCHANT,
                    player.getLocation().add(0, 1, 0),
                    combo * 5,
                    0.5, 0.5, 0.5,
                    0.1
                );
                
                if (combo == 5) {
                    broadcastNarrative("§6⚔ " + player.getName() + " §elogró un combo de 5 kills!");
                    player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                } else if (combo == 10) {
                    broadcastNarrative("§c§l⚔ " + player.getName() + " §e§lCOMBO LEGENDARIO x10!");
                    player.spawnParticle(Particle.DRAGON_BREATH, player.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.1);
                }
            }
        } else {
            // Reiniciar combo
            combosJugador.put(uuid, 1);
        }
        
        ultimoKillJugador.put(uuid, ahora);
    }
    
    /**
     * Envía mensaje de recompensas épico.
     */
    private void enviarMensajeRecompensas(Player player, int fragmentos, boolean nucleoRecogido, int psTotal, String rango) {
        player.sendMessage("");
        player.sendMessage("§8§l▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓");
        player.sendMessage(formatearCentrado("§6§l✦ RECOMPENSAS OBTENIDAS ✦"));
        player.sendMessage("");
        player.sendMessage(formatearCentrado("§5§lFragmentos descubiertos: §d" + fragmentos));
        if (nucleoRecogido) {
            player.sendMessage(formatearCentrado("§5§lNúcleo recuperado: §a✓"));
        }
        player.sendMessage("");
        player.sendMessage(formatearCentrado("§6§l+" + psTotal + " PS"));
        player.sendMessage("");
        player.sendMessage(formatearCentrado("§d¡Gracias por participar!"));
        player.sendMessage("");
        player.sendMessage("§8§l▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓");
        player.sendMessage("");
        
        // Efectos de celebración
        player.spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.05);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.5f);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════
    
    public int getFragmentosInspeccionados() {
        return fragmentosInspeccionados.size();
    }
    
    public int getFragmentosTotales() {
        return fragmentosLocations.size();
    }
    
    public int getOleadaActual() {
        return oleadaActual;
    }
    
    public int getCriaturasVivas() {
        return criaturasActivas.size();
    }
    
    public boolean isNucleoRecogido() {
        return nucleoRecogido;
    }
    
    public Map<UUID, Integer> getParticipacionCriaturas() {
        return participacionCriaturas;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE EFECTOS DE PROXIMIDAD
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Inicia el sistema de efectos visuales de proximidad que guía a los jugadores.
     */
    private void iniciarEfectosProximidad() {
        proximidadTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : participantesOriginales) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) continue;
                
                Location objetivo = objetivosPorJugador.get(uuid);
                if (objetivo == null) continue;
                
                double distancia = p.getLocation().distance(objetivo);
                
                // Trail de partículas cada 3 bloques hacia el objetivo
                if (distancia > 15) {
                    crearTrailParticulas(p.getLocation(), objetivo);
                }
                
                // Flechas direccionales flotantes cada 20 bloques
                if (distancia > 20 && distancia <= 100) {
                    crearFlechaDireccional(p, objetivo);
                }
                
                // Indicadores de distancia cada 10 bloques
                int distanciaRedondeada = ((int) distancia / 10) * 10;
                if (distancia > 10 && ultimoIndicadorDistancia.getOrDefault(p.getUniqueId(), 0) != distanciaRedondeada) {
                    mostrarIndicadorDistancia(p, (int) distancia);
                    ultimoIndicadorDistancia.put(p.getUniqueId(), distanciaRedondeada);
                }
            }
        }, 0L, 20L); // Cada segundo
    }
    
    /**
     * Crea un trail de partículas desde el jugador hacia el objetivo.
     */
    private void crearTrailParticulas(Location desde, Location hacia) {
        World world = desde.getWorld();
        if (world == null) return;
        
        // Calcular dirección
        org.bukkit.util.Vector direction = hacia.toVector().subtract(desde.toVector()).normalize();
        
        // Crear 5 partículas en línea hacia el objetivo
        for (int i = 1; i <= 5; i++) {
            Location particleLoc = desde.clone().add(direction.clone().multiply(i * 3));
            particleLoc.add(0, 0.5, 0); // Elevar ligeramente
            
            // Partículas sutiles que flotan
            world.spawnParticle(
                Particle.END_ROD,
                particleLoc,
                1,
                0.1, 0.1, 0.1,
                0.01
            );
            
            // Partículas adicionales en espiral
            double angle = (System.currentTimeMillis() / 50.0) + (i * 0.5);
            double offsetX = Math.cos(angle) * 0.3;
            double offsetZ = Math.sin(angle) * 0.3;
            
            world.spawnParticle(
                Particle.ENCHANT,
                particleLoc.clone().add(offsetX, 0, offsetZ),
                1,
                0, 0, 0,
                0
            );
        }
    }
    
    /**
     * Crea una flecha direccional flotante que apunta al objetivo.
     */
    private void crearFlechaDireccional(Player player, Location objetivo) {
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();
        if (world == null) return;
        
        // Calcular dirección al objetivo
        org.bukkit.util.Vector direction = objetivo.toVector().subtract(playerLoc.toVector()).normalize();
        
        // Posición de la flecha (5 bloques frente al jugador, 3 bloques arriba)
        Location arrowLoc = playerLoc.clone().add(direction.clone().multiply(5)).add(0, 3, 0);
        
        // Flecha de partículas (forma triangular apuntando)
        for (int i = 0; i < 3; i++) {
            Location point = arrowLoc.clone().add(direction.clone().multiply(i * 0.3));
            
            // Punta de la flecha
            world.spawnParticle(
                Particle.SOUL_FIRE_FLAME,
                point,
                2,
                0.05, 0.05, 0.05,
                0.01
            );
            
            // Lados de la flecha
            if (i == 1) {
                org.bukkit.util.Vector perpendicular = new org.bukkit.util.Vector(-direction.getZ(), 0, direction.getX()).normalize();
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, point.clone().add(perpendicular.multiply(0.3)), 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, point.clone().subtract(perpendicular.multiply(0.3)), 1, 0, 0, 0, 0);
            }
        }
        
        // Efecto de brillo en la base
        world.spawnParticle(
            Particle.ELECTRIC_SPARK,
            arrowLoc,
            3,
            0.2, 0.2, 0.2,
            0.05
        );
    }
    
    /**
     * Muestra un indicador visual de distancia al objetivo.
     */
    private void mostrarIndicadorDistancia(Player player, int distancia) {
        Location playerLoc = player.getLocation();
        World world = playerLoc.getWorld();
        if (world == null) return;
        
        // Crear anillo de partículas a la altura del jugador
        int numParticulas = 12;
        double radio = 1.5;
        
        for (int i = 0; i < numParticulas; i++) {
            double angle = (2 * Math.PI / numParticulas) * i;
            double x = Math.cos(angle) * radio;
            double z = Math.sin(angle) * radio;
            
            Location particleLoc = playerLoc.clone().add(x, 0.1, z);
            
            // Color según distancia (verde cerca, amarillo medio, rojo lejos)
            Particle particula;
            if (distancia < 30) {
                particula = Particle.HAPPY_VILLAGER; // Verde
            } else if (distancia < 60) {
                particula = Particle.ENCHANT; // Azul
            } else {
                particula = Particle.SOUL_FIRE_FLAME; // Azul oscuro
            }
            
            world.spawnParticle(particula, particleLoc, 1, 0, 0, 0, 0);
        }
        
        // Sonido sutil de feedback
        player.playSound(playerLoc, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.3f, 1.5f);
        
        // ❌ ACTION BAR REMOVIDO - Ya se muestra en iniciarGuiaActionBar() con direcciones
        // El sistema de guía principal (línea 3923) ya muestra action bar con:
        // - Dirección relativa (↑→←↓)
        // - Distancia con colores (verde/amarillo/rojo)
        // Este action bar competía y sobrescribía el principal
    }
    
    /**
     * Obtiene la dirección cardinal relativa desde una ubicación hacia otra
     */
    private String obtenerDireccionRelativa(Location desde, Location hacia) {
        double dx = hacia.getX() - desde.getX();
        double dz = hacia.getZ() - desde.getZ();
        
        double angulo = Math.toDegrees(Math.atan2(dz, dx));
        
        // Normalizar entre 0 y 360
        if (angulo < 0) angulo += 360;
        
        // Determinar dirección cardinal
        if (angulo >= 337.5 || angulo < 22.5) {
            return "ESTE";
        } else if (angulo >= 22.5 && angulo < 67.5) {
            return "SURESTE";
        } else if (angulo >= 67.5 && angulo < 112.5) {
            return "SUR";
        } else if (angulo >= 112.5 && angulo < 157.5) {
            return "SUROESTE";
        } else if (angulo >= 157.5 && angulo < 202.5) {
            return "OESTE";
        } else if (angulo >= 202.5 && angulo < 247.5) {
            return "NOROESTE";
        } else if (angulo >= 247.5 && angulo < 292.5) {
            return "NORTE";
        } else {
            return "NORESTE";
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE AMBIENTE DINÁMICO POR ACTO
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Intensifica el ambiente para el Acto 2 (Grieta Dimensional).
     */
    private void intensificarAmbienteActo2() {
        World world = Bukkit.getWorlds().get(0);
        
        // Cambiar a niebla más densa
        environmentSystem.createVolumetricFog(world, FogIntensity.MEDIUM, 0);
        
        // Añadir efecto de portal dimensional
        environmentSystem.spawnAtmosphericEffect(world, AtmosphericEffect.VOID_CRACKS, 0);
        
        // Rayos más frecuentes y dramáticos
        for (int i = 0; i < 5; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location spawn = world.getSpawnLocation();
                double angle = Math.random() * Math.PI * 2;
                double dist = 50 + Math.random() * 80;
                
                int x = spawn.getBlockX() + (int)(Math.cos(angle) * dist);
                int z = spawn.getBlockZ() + (int)(Math.sin(angle) * dist);
                int y = world.getHighestBlockYAt(x, z);
                
                Location rayoLoc = new Location(world, x, y, z);
                world.strikeLightningEffect(rayoLoc);
                
                // Explosión de partículas en el punto de impacto
                world.spawnParticle(
                    Particle.REVERSE_PORTAL,
                    rayoLoc.clone().add(0, 1, 0),
                    200,
                    2, 2, 2,
                    0.5
                );
            }, i * 20L);
        }
        
        // Sonido ambiente más tenso
        playSoundToAll(Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 0.7f);
        playSoundToAll(Sound.ENTITY_WITHER_AMBIENT, 0.3f, 0.5f);
    }
    
    /**
     * Intensifica dramáticamente el ambiente para el Acto 3 (Núcleo Final).
     */
    private void intensificarAmbienteActo3() {
        World world = Bukkit.getWorlds().get(0);
        
        // Niebla máxima
        environmentSystem.createVolumetricFog(world, FogIntensity.DENSE, 0);
        
        // Añadir efectos de vacío y energía
        environmentSystem.spawnAtmosphericEffect(world, AtmosphericEffect.SOUL_ESSENCE, 0);
        
        // Tormenta dramática de rayos
        for (int i = 0; i < 8; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location spawn = world.getSpawnLocation();
                
                // Múltiples rayos en círculo
                for (int j = 0; j < 4; j++) {
                    double angle = (Math.PI * 2 / 4) * j + (Math.random() * 0.5);
                    double dist = 60 + Math.random() * 60;
                    
                    int x = spawn.getBlockX() + (int)(Math.cos(angle) * dist);
                    int z = spawn.getBlockZ() + (int)(Math.sin(angle) * dist);
                    int y = world.getHighestBlockYAt(x, z);
                    
                    Location rayoLoc = new Location(world, x, y, z);
                    world.strikeLightningEffect(rayoLoc);
                    
                    // Explosión de partículas soul
                    world.spawnParticle(
                        Particle.SOUL_FIRE_FLAME,
                        rayoLoc.clone().add(0, 1, 0),
                        300,
                        3, 3, 3,
                        0.3
                    );
                    
                    // Columna de partículas
                    for (int k = 0; k < 10; k++) {
                        world.spawnParticle(
                            Particle.ELECTRIC_SPARK,
                            rayoLoc.clone().add(0, k * 2, 0),
                            20,
                            0.5, 0.5, 0.5,
                            0.1
                        );
                    }
                }
            }, i * 15L);
        }
        
        // Partículas atmosféricas intensas en spawn
        Location spawn = world.getSpawnLocation();
        for (int i = 0; i < 50; i++) {
            double angle = Math.random() * Math.PI * 2;
            double dist = 10 + Math.random() * 40;
            double height = Math.random() * 20;
            
            double x = Math.cos(angle) * dist;
            double z = Math.sin(angle) * dist;
            
            world.spawnParticle(
                Particle.END_ROD,
                spawn.clone().add(x, height, z),
                1,
                0, 0.1, 0,
                0.02
            );
        }
        
        // Sonidos épicos superpuestos
        playSoundToAll(Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.6f, 0.5f);
        playSoundToAll(Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.6f);
        playSoundToAll(Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.5f, 0.7f);
        
        // Mensaje dramático
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            broadcastNarrative("§8§l⚡ §5La energía dimensional alcanza su punto máximo... §8§l⚡");
        }, 40L);
    }
    
    /**
     * Mensaje del Observador con efectos dramáticos completos
     */
    private void mensajeObservadorConEfectos(String mensaje, Sound sonido, float volumen, float tono, Particle particula, int cantidadParticulas) {
        // Broadcast del mensaje
        broadcastNarrative(mensaje);
        
        // Sonido dramático
        playSoundToAll(sonido, volumen, tono);
        
        // Efecto visual para todos los jugadores
        for (UUID uuid : participantesOriginales) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                Location playerLoc = p.getLocation();
                World world = p.getWorld();
                
                // Círculo de partículas alrededor del jugador
                for (int i = 0; i < 360; i += 15) {
                    double angle = Math.toRadians(i);
                    double x = playerLoc.getX() + Math.cos(angle) * 3.0;
                    double z = playerLoc.getZ() + Math.sin(angle) * 3.0;
                    double y = playerLoc.getY() + 1.5;
                    
                    world.spawnParticle(
                        particula,
                        x, y, z,
                        2,
                        0.1, 0.1, 0.1,
                        0.01
                    );
                }
                
                // Espiral ascendente de partículas
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (p.isOnline()) {
                        for (int i = 0; i < 20; i++) {
                            final int step = i;
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                if (p.isOnline()) {
                                    Location loc = p.getLocation();
                                    double angle = Math.toRadians(step * 18);
                                    double radius = 1.5 - (step * 0.05);
                                    double x = loc.getX() + Math.cos(angle) * radius;
                                    double z = loc.getZ() + Math.sin(angle) * radius;
                                    double y = loc.getY() + (step * 0.15);
                                    
                                    p.getWorld().spawnParticle(
                                        Particle.ENCHANT,
                                        x, y, z,
                                        3,
                                        0.05, 0.05, 0.05,
                                        0.02
                                    );
                                }
                            }, step);
                        }
                    }
                }, 10L);
                
                // ⚡ Pulso de darkness MUY BREVE (0.5s) solo para flash dramático
                p.addPotionEffect(new PotionEffect(
                    PotionEffectType.DARKNESS,
                    10, // 0.5 segundos - SOLO UN FLASH
                    0,
                    false,
                    false
                ));
                
                // Sonido personal adicional
                p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.3f, 1.5f);
            }
        }
    }
    
    /**
     * ❌ DESACTIVADO - Darkness temporal removido para mejor visibilidad
     * Solo se usan efectos visuales (partículas) y sonidos para atmósfera de terror
     * Darkness solo en momentos MUY específicos y por <1 segundo
     */
    @SuppressWarnings("unused")
    private void aplicarDarknessTemporal(int duracionSegundos) {
        // ❌ FUNCIÓN DESACTIVADA - No aplicar darkness prolongado
        // Solo usar efectos visuales alternativos:
        
        for (UUID uuid : participantesOriginales) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                // En lugar de darkness, usar partículas de suspense
                for (int i = 0; i < 20; i++) {
                    p.spawnParticle(
                        Particle.SQUID_INK,
                        p.getLocation().add(0, 2, 0),
                        10,
                        1, 1, 1,
                        0.1
                    );
                }
            }
        }
        
        // Sonido de suspenso (mantener)
        playSoundToAll(Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.7f);
        
        plugin.getLogger().info(
            "[SusurroPiedraRota] Efecto de suspense con partículas (sin darkness prolongado)"
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE BOSSBAR DE PROGRESO
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Inicia el sistema de actualización del BossBar que muestra:
     * - Progreso actual del evento
     * - Tiempo transcurrido
     * - Información contextual según el acto
     */
    private void iniciarActualizacionBossBar() {
        bossBarUpdateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isActive() || bossBarProgreso == null) return;
            
            actualizarBossBar();
        }, 0L, 10L); // Actualizar cada 0.5 segundos
    }
    
    /**
     * Actualiza el contenido y apariencia del BossBar según el acto actual
     */
    private void actualizarBossBar() {
        if (bossBarProgreso == null) return;
        
        // Calcular tiempo transcurrido
        long tiempoTranscurrido = System.currentTimeMillis() - tiempoInicioActo;
        int minutos = (int) (tiempoTranscurrido / 60000);
        int segundos = (int) ((tiempoTranscurrido % 60000) / 1000);
        String tiempoStr = String.format("%02d:%02d", minutos, segundos);
        
        String titulo = "";
        double progreso = 0.0;
        BarColor color = BarColor.PURPLE;
        
        switch (actoActual) {
            case INTRO:
                titulo = "§5◆ El Susurro en la Piedra Rota ◆ §7| ⏱ " + tiempoStr;
                progreso = 0.0;
                color = BarColor.PURPLE;
                break;
                
            case PIEDRA_DESPIERTA:
                int fragmentosRestantes = dificultadEvento.getNumFragmentos() - fragmentosInspeccionados.size();
                titulo = String.format("§5Acto 1: Fragmentos §7%d/%d §8| ⏱ %s", 
                    fragmentosInspeccionados.size(), 
                    dificultadEvento.getNumFragmentos(),
                    tiempoStr);
                progreso = (double) fragmentosInspeccionados.size() / dificultadEvento.getNumFragmentos();
                color = fragmentosRestantes <= 1 ? BarColor.GREEN : BarColor.BLUE;
                break;
                
            case PIEDRA_QUIEBRA:
                if (oleadasCompletadas) {
                    titulo = String.format("§6Acto 2: ¡Oleadas Completadas! §8| ⏱ %s", tiempoStr);
                    progreso = 1.0;
                    color = BarColor.YELLOW;
                } else {
                    int criaturasVivas = (int) criaturasActivas.stream().filter(e -> e != null && e.isValid()).count();
                    titulo = String.format("§cOleada %d/%d §7| Criaturas: %d §8| ⏱ %s", 
                        oleadaActual, 
                        oleadasTotales,
                        criaturasVivas,
                        tiempoStr);
                    progreso = (double) (oleadaActual - 1) / oleadasTotales;
                    color = criaturasVivas > 10 ? BarColor.RED : BarColor.YELLOW;
                }
                break;
                
            case NUCLEO_FORMA:
                if (nucleoRecogido) {
                    titulo = String.format("§dActo 3: ¡Núcleo Recuperado! §8| ⏱ %s", tiempoStr);
                    progreso = 1.0;
                    color = BarColor.PINK;
                } else if (nucleoFrame != null && !nucleoFrame.isDead()) {
                    // Calcular "salud" del núcleo (simulada por el tiempo que lleva siendo atacado)
                    double saludNucleo = 1.0; // Puedes implementar lógica de daño aquí
                    titulo = String.format("§5Acto 3: Núcleo de Forma §c❤ %.0f%% §8| ⏱ %s", 
                        saludNucleo * 100,
                        tiempoStr);
                    progreso = 0.66; // 66% del evento completado
                    color = saludNucleo > 0.5 ? BarColor.PURPLE : BarColor.RED;
                } else {
                    titulo = String.format("§5Acto 3: Localiza el Núcleo §8| ⏱ %s", tiempoStr);
                    progreso = 0.66;
                    color = BarColor.PURPLE;
                }
                break;
                
            case VICTORIA:
                titulo = String.format("§a✔ ¡Evento Completado! §7| Tiempo: %s", tiempoStr);
                progreso = 1.0;
                color = BarColor.GREEN;
                break;
                
            default:
                titulo = String.format("§5El Susurro en la Piedra Rota §8| ⏱ %s", tiempoStr);
                progreso = 0.0;
                color = BarColor.PURPLE;
                break;
        }
        
        // Actualizar BossBar
        bossBarProgreso.setTitle(titulo);
        bossBarProgreso.setProgress(Math.max(0.0, Math.min(1.0, progreso)));
        bossBarProgreso.setColor(color);
        
        // Asegurar que todos los jugadores online vean el BossBar
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!bossBarProgreso.getPlayers().contains(p)) {
                bossBarProgreso.addPlayer(p);
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE ANIMACIÓN DEL OBSERVADOR
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Envía un mensaje del Observador con animación de máquina de escribir
     * Incluye efectos visuales, sonidos y partículas dramáticas
     */
    private void enviarMensajeObservadorAnimado(String mensaje) {
        enviarMensajeObservadorAnimado(mensaje, 2, true);
    }
    
    /**
     * Envía mensaje del Observador con velocidad personalizada
     * @param mensaje El mensaje a enviar
     * @param ticksPorCaracter Ticks entre cada caracter (menor = más rápido)
     * @param conEfectos Si debe incluir efectos visuales
     */
    private void enviarMensajeObservadorAnimado(String mensaje, int ticksPorCaracter, boolean conEfectos) {
        String textoLimpio = mensaje.replaceAll("§[0-9a-fk-or]", "");
        int longitudTotal = textoLimpio.length();
        
        // Efecto inicial de entrada
        if (conEfectos) {
            for (UUID uuid : participantesOriginales) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    // Flash de darkness muy breve
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 8, 0, false, false));
                    soundUtil.playSound(p, Sound.ENTITY_ENDERMAN_STARE, 0.2f, 0.3f);
                }
            }
        }
        
        // Animación de texto caracter por caracter
        StringBuilder textoActual = new StringBuilder();
        String prefijo = "§8◆ §5El Observador§8: §7§o«";
        String sufijo = "»";
        
        for (int i = 0; i < longitudTotal; i++) {
            final int indice = i;
            final char caracter = textoLimpio.charAt(i);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!isActive()) return;
                
                textoActual.append(caracter);
                String mensajeParcial = prefijo + textoActual.toString() + "§8▌" + sufijo;
                
                for (UUID uuid : participantesOriginales) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        // Enviar por ActionBar para efecto de máquina de escribir
                        p.sendActionBar(mensajeParcial);
                        
                        // Sonido sutil de tecleo
                        if (indice % 3 == 0) {
                            soundUtil.playSound(p, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 0.05f, 1.8f + (float)(Math.random() * 0.4));
                        }
                    }
                }
            }, i * ticksPorCaracter);
        }
        
        // Mensaje final completo en chat después de la animación
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            String mensajeFinal = prefijo + textoLimpio + sufijo;
            broadcastNarrative(mensajeFinal);
            
            // Limpiar ActionBar
            for (UUID uuid : participantesOriginales) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    p.sendActionBar("");
                    
                    // Efecto final
                    if (conEfectos) {
                        soundUtil.playSound(p, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.2f);
                        
                        // Partículas de cierre
                        Location loc = p.getLocation().add(0, 1, 0);
                        loc.getWorld().spawnParticle(Particle.ENCHANT, loc, 15, 0.5, 0.5, 0.5, 0.1);
                    }
                }
            }
        }, (longitudTotal * ticksPorCaracter) + 10);
    }
    
    /**
     * Envía múltiples líneas del Observador con animación secuencial
     */
    private void enviarDialogoObservadorAnimado(String[] lineas, long delayEntreLineas) {
        for (int i = 0; i < lineas.length; i++) {
            final int indice = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!isActive()) return;
                enviarMensajeObservadorAnimado(lineas[indice], 2, indice == 0); // Solo efectos en la primera línea
            }, i * delayEntreLineas);
        }
    }
}

