package me.apocalipsis.events;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.*;
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
 * El Susurro en la Piedra Rota - Mini-evento narrativo de 4 actos
 * 
 * Contexto narrativo:
 * Después del Eco de Sombras, algo "de afuera" reaccionó.
 * La figura desconocida cambió la memoria del mundo.
 * Ciertas estructuras antiguas están comenzando a "despertar".
 * 
 * Este evento introduce el concepto de "forma" (complemento de "sombra").
 * 
 * Actos del evento:
 * 1. LA PIEDRA ROTA DESPIERTA (5 min): 3-5 fragmentos de piedra aparecen,
 *    hablan al acercarse con mensajes fragmentados
 * 2. LA PIEDRA SE QUIEBRA (5 min): Grieta de Forma aparece, spawn de
 *    Criaturas de Forma en oleadas rápidas
 * 3. EL NÚCLEO DE FORMA (5 min): Aparece el Fragmento de Forma Desviada
 *    (item único permanente)
 * 4. EL SEGUNDO SUSURRO (5 min): Mensajes inquietantes, pensamiento del
 *    Observador, cliffhanger con símbolo en el cielo
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
    // ACTO 1: FRAGMENTOS DE PIEDRA ROTA
    // ═══════════════════════════════════════════════════════════════════
    
    private List<Location> fragmentosLocations = new ArrayList<>();
    private Set<Location> fragmentosInspeccionados = new HashSet<>();
    private Map<UUID, Set<Location>> jugadoresFragmentosVistos = new HashMap<>();
    private BukkitTask fragmentosParticleTask;
    
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
        
        int cantidadMin = acto1Config.getInt("cantidad_min", 3);
        int cantidadMax = acto1Config.getInt("cantidad_max", 5);
        int distanciaMin = acto1Config.getInt("distancia_min_spawn", 50);
        int distanciaMax = acto1Config.getInt("distancia_max_spawn", 150);
        int distanciaEntreFragmentos = acto1Config.getInt("distancia_entre_fragmentos", 30);
        
        int cantidad = cantidadMin + new Random().nextInt(cantidadMax - cantidadMin + 1);
        
        World world = Bukkit.getWorlds().get(0);
        Location spawn = world.getSpawnLocation();
        
        broadcastNarrative("§5⧖ Buscando ubicaciones perfectas para los fragmentos...");
        playSoundToAll(Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 0.8f);
        
        // Mensajes del OBSERVADOR con efectos dramáticos (más espaciados)
        // Mensaje 1: Después de 10 segundos
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
        }, 200L); // 10 segundos
        
        // Mensaje 2: Después de 25 segundos
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
        }, 500L); // 25 segundos
        
        // Mensaje 3: Después de 40 segundos
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
        }, 800L); // 40 segundos
        
        // BUSCAR UBICACIONES DE FORMA ASÍNCRONA para no congelar el servidor
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Location> ubicacionesEncontradas = new ArrayList<>();
            primerFragmentoEncontroLugarPerfecto = true; // Reset
            
            for (int i = 0; i < cantidad; i++) {
                final boolean esPrimerFragmento = (i == 0);
                Location loc = encontrarLocationValidaAsync(world, spawn, distanciaMin, distanciaMax, distanciaEntreFragmentos, ubicacionesEncontradas, esPrimerFragmento);
                
                if (loc != null) {
                    ubicacionesEncontradas.add(loc);
                    plugin.getLogger().info(String.format(
                        "[SusurroPiedraRota] Fragmento #%d ubicación %s: %s",
                        i + 1,
                        primerFragmentoEncontroLugarPerfecto ? "encontrada" : "creada",
                        locationToString(loc)
                    ));
                } else {
                    plugin.getLogger().warning(String.format(
                        "[SusurroPiedraRota] ⚠ No se pudo generar ubicación para fragmento #%d",
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
                            
                            plugin.getLogger().info(String.format(
                                "[SusurroPiedraRota] Fragmento #%d generado en: %s",
                                indice + 1,
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
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (participantesOriginales.contains(p.getUniqueId())) {
                            Location objetivo = encontrarFragmentoMasCercano(p.getLocation());
                            if (objetivo != null) {
                                objetivosPorJugador.put(p.getUniqueId(), objetivo);
                                plugin.getLogger().info("[ActionBar] Asignado objetivo inicial a " + p.getName());
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
        
        int cantidadMin = acto1Config.getInt("cantidad_min", 3);
        int cantidadMax = acto1Config.getInt("cantidad_max", 5);
        int distanciaMin = acto1Config.getInt("distancia_min_spawn", 1000); // MÍNIMO 1000 bloques
        int distanciaMax = acto1Config.getInt("distancia_max_spawn", 1500); // MÁXIMO 1500 bloques
        int distanciaEntreFragmentos = acto1Config.getInt("distancia_entre_fragmentos", 150); // 150 bloques entre fragmentos
        
        int cantidad = cantidadMin + new Random().nextInt(cantidadMax - cantidadMin + 1);
        
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
     * Si el primer fragmento no encuentra lugar perfecto, los siguientes DIRECTAMENTE crean el lugar
     */
    private Location encontrarLocationValidaAsync(World world, Location spawn, int distMin, int distMax, int distEntreFragmentos, List<Location> ubicacionesExistentes, boolean esPrimerFragmento) {
        Random random = new Random();
        
        // ⚡ OPTIMIZACIÓN: Si el primer fragmento no encontró lugar perfecto, SKIP búsqueda
        if (!esPrimerFragmento && !primerFragmentoEncontroLugarPerfecto) {
            plugin.getLogger().info("[SusurroPiedraRota] ⚡ Fragmento subsecuente - Creando lugar directamente (sin búsqueda)");
            return crearLugarPerfectoDirectamente(world, spawn, distMin, distMax, distEntreFragmentos, ubicacionesExistentes, random);
        }
        
        // SOLO el primer fragmento hace búsqueda exhaustiva
        plugin.getLogger().info(String.format(
            "[SusurroPiedraRota] [ASYNC] Buscando ubicación perfecta (Radio: %d-%d bloques, mín 600 de jugadores, %d intentos)",
            distMin, distMax, esPrimerFragmento ? 100 : 50
        ));
        
        int radioActual = distMax;
        int maxIntentos = esPrimerFragmento ? 100 : 50; // ⚡ Menos intentos si no es el primero
        
        for (int intento = 0; intento < maxIntentos; intento++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = distMin + random.nextDouble() * (radioActual - distMin);
            
            int x = spawn.getBlockX() + (int)(Math.cos(angle) * distance);
            int z = spawn.getBlockZ() + (int)(Math.sin(angle) * distance);
            
            // OPERACIÓN ASYNC-SAFE
            int y = world.getHighestBlockYAt(x, z);
            Location loc = new Location(world, x, y, z);
            
            // 🔒 VALIDACIÓN: Mínimo 600 bloques de TODOS los jugadores
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
            
            // VALIDACIÓN ESTRICTA
            if (!esUbicacionPerfecta(world, loc)) {
                continue;
            }
            
            // Verificar distancia mínima 100 bloques con otros fragmentos
            boolean lejosDeOtros = true;
            for (Location existente : ubicacionesExistentes) {
                if (existente.distance(loc) < 100) { // Aumentado de distEntreFragmentos a 100
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
            plugin.getLogger().info("[SusurroPiedraRota] [ASYNC] ⚠ Primer fragmento: No encontró lugar perfecto, creando uno...");
        } else {
            plugin.getLogger().info("[SusurroPiedraRota] [ASYNC] ⚡ Fragmento subsecuente: Creando lugar perfecto...");
        }
        
        return crearLugarPerfectoDirectamente(world, spawn, distMin, distMax, distEntreFragmentos, ubicacionesExistentes, random);
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
     * Terraforma un área 17x17 para crear el lugar perfecto para un fragmento
     * Garantiza terreno plano, sólido, sin agua, y con bordes decorativos
     */
    private void terraformarLugarPerfecto(World world, Location centro) {
        int cx = centro.getBlockX();
        int cy = centro.getBlockY();
        int cz = centro.getBlockZ();
        Random rand = new Random();
        
        // 🎭 Área ÉPICA: 37x37 (radio 18) - MUCHO MÁS GRANDE
        for (int x = cx - 18; x <= cx + 18; x++) {
            for (int z = cz - 18; z <= cz + 18; z++) {
                double distCentro = Math.sqrt(Math.pow(x - cx, 2) + Math.pow(z - cz, 2));
                
                // 1. Base sólida más profunda (3 capas)
                world.getBlockAt(x, cy - 3, z).setType(Material.DEEPSLATE);
                world.getBlockAt(x, cy - 2, z).setType(Material.STONE);
                world.getBlockAt(x, cy - 1, z).setType(Material.STONE);
                
                // 2. Limpiar espacio arriba (8 bloques - ALTAR MÁS ALTO)
                for (int dy = 0; dy < 8; dy++) {
                    world.getBlockAt(x, cy + dy, z).setType(Material.AIR);
                }
                
                // 3. Decoración épica en anillos concéntricos
                if (distCentro >= 16 && distCentro <= 18) {
                    // Anillo exterior - Piedra agrietada
                    if (rand.nextDouble() < 0.4) {
                        world.getBlockAt(x, cy, z).setType(Material.CRACKED_STONE_BRICKS);
                    }
                } else if (distCentro >= 12 && distCentro < 16) {
                    // Anillo medio - Piedra musgosa
                    if (rand.nextDouble() < 0.3) {
                        world.getBlockAt(x, cy, z).setType(Material.MOSSY_COBBLESTONE);
                    }
                } else if (distCentro >= 8 && distCentro < 12) {
                    // Anillo interno - Ladrillos de piedra
                    if (rand.nextDouble() < 0.25) {
                        world.getBlockAt(x, cy, z).setType(Material.STONE_BRICKS);
                    }
                }
                
                // 4. Columnas decorativas en puntos cardinales
                int distX = Math.abs(x - cx);
                int distZ = Math.abs(z - cz);
                if ((distX == 15 && distZ == 0) || (distX == 0 && distZ == 15)) {
                    for (int h = 1; h <= 3; h++) {
                        world.getBlockAt(x, cy + h, z).setType(Material.CHISELED_STONE_BRICKS);
                    }
                    world.getBlockAt(x, cy + 4, z).setType(Material.LANTERN);
                }
            }
        }
        
        // 🎆 Efecto visual ÉPICO al terminar terraformación
        for (int i = 0; i < 360; i += 20) {
            double angle = Math.toRadians(i);
            double radius = 18.0;
            double x = centro.getX() + Math.cos(angle) * radius;
            double z = centro.getZ() + Math.sin(angle) * radius;
            
            world.spawnParticle(
                Particle.SOUL_FIRE_FLAME,
                x, centro.getY() + 1, z,
                10,
                0.3, 1.0, 0.3,
                0.05
            );
            world.spawnParticle(
                Particle.ELECTRIC_SPARK,
                x, centro.getY() + 2, z,
                5,
                0.2, 0.5, 0.2,
                0.08
            );
        }
        
        // Sonido épico de terraformación
        world.playSound(centro, Sound.ENTITY_WITHER_SPAWN, 0.3f, 0.6f);
        world.playSound(centro, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.5f, 0.8f);
        
        plugin.getLogger().info(String.format(
            "[SusurroPiedraRota] 🎭 Terraformado ÉPICO 37x37 completado en %s (base profunda, altar alto 8 bloques)",
            locationToString(centro)
        ));
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
                    onFragmentoDescubierto(player, fragmento);
                    fragmentosInspeccionados.add(fragmento);
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
        
        // ✨ NUEVO: Actualizar BossBar con progreso
        if (bossBarProgreso != null) {
            double progreso = (double) fragmentosInspeccionados.size() / fragmentosLocations.size();
            bossBarProgreso.setProgress(Math.min(progreso, 1.0));
            bossBarProgreso.setTitle("§5Fragmentos: " + fragmentosInspeccionados.size() + "/" + fragmentosLocations.size());
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
            net.kyori.adventure.text.Component.text("§7" + fragmentosInspeccionados.size() + "/" + fragmentosLocations.size()),
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
        String[] mensajes = {
            "§8⧖ §7...hijo del eco...",
            "§8⧖ §7...fragmento incorrecto...",
            "§8⧖ §7...la forma busca...",
            "§8⧖ §7...memoria rota...",
            "§8⧖ §7...no debería existir..."
        };
        
        int index = fragmentosLocations.indexOf(fragmento);
        String mensaje = mensajes[index % mensajes.length];
        
        enviarMensajeDescubrimiento(
            player,
            "FRAGMENTO DESCUBIERTO",
            fragmentosInspeccionados.size() + 1,
            fragmentosLocations.size()
        );
        
        // Susurro animado después del descubrimiento
        mostrarSusurroFragmento(player, mensaje, 40L);
        
        // Destello suave al descubrir fragmento
        crearDestello(player, 3);
        
        soundUtil.playSound(player, Sound.ENTITY_ENDERMAN_STARE, 0.3f, 0.5f);
        
        // Asignar siguiente fragmento como objetivo
        Location siguienteFragmento = encontrarFragmentoMasCercano(player.getLocation());
        if (siguienteFragmento != null) {
            objetivosPorJugador.put(player.getUniqueId(), siguienteFragmento);
        } else {
            objetivosPorJugador.remove(player.getUniqueId());
        }
        
        plugin.getLogger().info(String.format(
            "[SusurroPiedraRota] %s descubrió fragmento #%d (%d/%d)",
            player.getName(),
            index + 1,
            fragmentosInspeccionados.size(),
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
            int restantes = fragmentosLocations.size() - fragmentosInspeccionados.size();
            if (restantes > 0) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    mostrarMensajeNarrativoAnimado(p, "§5⚡ Quedan " + restantes + " fragmentos por descubrir", 0L);
                }
            }
        }
        
        // SISTEMA DE HINTS PROGRESIVOS
        verificarHintsFragmentos();
        
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
    // ACTO 2: LA PIEDRA SE QUIEBRA
    // ═══════════════════════════════════════════════════════════════════
    
    private void iniciarActo2() {
        plugin.getLogger().info("[SusurroPiedraRota] Iniciando transición cinemática a Acto 2");
        
        // 🎬 TRANSICIÓN CINEMÁTICA ÉPICA
        // 1. Fadeout (pantalla negra)
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§0▬", "", 10, 60, 20); // Fade prolongado
        }
        playSoundToAll(Sound.ENTITY_WITHER_DEATH, 0.5f, 0.5f);
        
        // 2. Pausa dramática de 3 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            actoActual = Acto.PIEDRA_QUIEBRA;
            ticksEnActo = 0;
            
            // Intensificar ambiente para Acto 2
            intensificarAmbienteActo2();
            
            // 3. TÍTULO ÉPICO con fade-in
            enviarTituloCinematicoTodos(
                "§c§lACTO II",
                "§8✦ §5§lLA DEFENSA DEL ALTAR §8✦",
                80
            );
            
            // Sonidos épicos de transición
            playSoundToAll(Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.6f);
            playSoundToAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 0.5f);
            playSoundToAll(Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.8f);
            
            // 4. Mensaje narrativo con formato mejorado
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!isActive()) return;
                
                broadcastNarrative("§8§m══════════════════════════════════════════════════");
                broadcastNarrative("");
                broadcastNarrative("          §c§l⧗ ACTO 2: §5§lLA DEFENSA DEL ALTAR §c§l⧗");
                broadcastNarrative("");
                broadcastNarrative("    §7✦ Una grieta dimensional se ha abierto en el altar");
                broadcastNarrative("    §7✦ Oleadas de criaturas emergen desde el vacío");
                broadcastNarrative("    §7✦ Defiende el altar a toda costa");
                broadcastNarrative("");
                broadcastNarrative("§8§m══════════════════════════════════════════════════");
                
                playSoundToAll(Sound.BLOCK_BEACON_POWER_SELECT, 0.7f, 1.2f);
            }, 40L);
            
            // 5. Generar grieta con efecto dramático
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!isActive()) return;
                
                // 🗣️ DIÁLOGO - Inicio del Acto 2
                mostrarDialogoForma("ACTO2_INICIO");
                
                // 🎬 PAUSA DRAMÁTICA: Detener todo para el diálogo
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (participantesOriginales.contains(p.getUniqueId())) {
                        // Efecto de distorsión temporal
                        p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.SLOWNESS,
                            120, // 6 segundos
                            3,
                            true,
                            false
                        ));
                    }
                }
                
                // 🎬 EFECTO DE CÁMARA: Zoom in + screen shake para transición dramática
                for (Player p : Bukkit.getOnlinePlayers()) {
                    aplicarZoomIn(p, 3);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        aplicarScreenShakeIntenso(p, 2);
                    }, 40L);
                }
                
                generarGrietaForma();
                
                // Delay MAYOR para construir tensión y leer el diálogo
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (isActive() && actoActual == Acto.PIEDRA_QUIEBRA) {
                        // Oleadas se inician automáticamente en el sistema principal
                        broadcastNarrative("§c⚔ Las criaturas comienzan a aparecer...");
                    }
                }, 60L);
            }, 80L);
            
        }, 60L); // 3 segundos de pausa
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
        
        // Oleadas subsiguientes cada 20 segundos
        for (int i = 1; i < oleadasTotales; i++) {
            final long delay = 60L + (i * 400L); // 3s + (i * 20s)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (isActive() && actoActual == Acto.PIEDRA_QUIEBRA) {
                    spawnearOleada();
                }
            }, delay);
        }
    }
    
    private void spawnearOleada() {
        oleadaActual++;
        
        // 🎯 MINI-BOSS cada 2 oleadas
        boolean esOleadaBoss = (oleadaActual % 2 == 0);
        
        int cantidadCriaturas = 3 + new Random().nextInt(3); // 3-5 criaturas
        if (esOleadaBoss) {
            cantidadCriaturas += 2; // +2 criaturas en oleadas boss
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
            broadcastNarrative("§c§l⚠ ¡OLEADA INTENSIFICADA! - " + oleadaActual + "/" + oleadasTotales);
            broadcastNarrative("§6★ Más criaturas y mayor dificultad ★");
        } else {
            broadcastNarrative(String.format("§5⚠ Oleada %d/%d - Elimina las criaturas de Forma", oleadaActual, oleadasTotales));
        }
        broadcastNarrative(barraOleadas);
        
        // Título cinematográfico de oleada con contador visual
        String simbolos = generarSimbolosOleada(oleadaActual, oleadasTotales);
        if (esOleadaBoss) {
            enviarTituloCinematicoTodos(
                "§c⚔ OLEADA INTENSIFICADA " + oleadaActual + "/" + oleadasTotales + " ⚔",
                "§6★ " + simbolos + " - " + cantidadCriaturas + " criaturas ★",
                40
            );
        } else {
            enviarTituloCinematicoTodos(
                "⚔ OLEADA " + oleadaActual + "/" + oleadasTotales + " ⚔",
                simbolos + " - " + cantidadCriaturas + " criaturas",
                40
            );
        }
        
        // 🎲 Asignar tipo de oleada aleatoriamente
        Random rand = new Random();
        if (esOleadaBoss) {
            tipoOleadaActual = "MIXTO"; // Oleadas boss siempre mixtas
        } else {
            String[] tipos = {"RAPIDA", "TANQUE", "EXPLOSIVA", "INVOCADORA", "MIXTO"};
            tipoOleadaActual = tipos[rand.nextInt(tipos.length)];
        }
        
        plugin.getLogger().info("[SusurroPiedraRota] Tipo de oleada: " + tipoOleadaActual);
        
        // Efecto de pulso de energía al spawnear oleada
        efectoPulsoEnergiaTodos();
        
        // SONIDOS DE TENSIÓN PROGRESIVA según oleada
        reproducirSonidosTension(oleadaActual, oleadasTotales);
        
        // 🗣️ DIÁLOGO - Criaturas aparecen (primera oleada)
        if (oleadaActual == 1) {
            mostrarDialogoForma("CRIATURAS_SPAWN");
            
            // 🎬 EFECTO DE CÁMARA: Slow motion para introducción de criaturas
            for (Player p : Bukkit.getOnlinePlayers()) {
                aplicarSlowMotion(p, 3);
            }
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
            // 🎲 TIPOS DIVERSOS DE CRIATURAS según tipo de oleada
            Random rand = new Random();
            String tipo;
            
            if ("MIXTO".equals(tipoOleadaActual)) {
                // Oleada mixta - distribución normal
                double tipoRoll = rand.nextDouble();
                if (tipoRoll < 0.45) tipo = "RAPIDA"; // 45%
                else if (tipoRoll < 0.70) tipo = "TANQUE"; // 25%
                else if (tipoRoll < 0.85) tipo = "EXPLOSIVA"; // 15%
                else tipo = "INVOCADORA"; // 15%
            } else {
                // Oleada especializada - 80% del tipo, 20% mixto
                if (rand.nextDouble() < 0.8) {
                    tipo = tipoOleadaActual;
                } else {
                    String[] otros = {"RAPIDA", "TANQUE", "EXPLOSIVA", "INVOCADORA"};
                    tipo = otros[rand.nextInt(otros.length)];
                }
            }
            
            Silverfish criatura = (Silverfish) world.spawnEntity(
                spawnAereo, // Spawn 25 bloques arriba
                EntityType.SILVERFISH
            );
            
            // Configurar según tipo
            switch (tipo) {
                case "RAPIDA":
                    criatura.customName(net.kyori.adventure.text.Component.text("§b⚡ Forma Veloz"));
                    criatura.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(10.0);
                    criatura.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(0.4);
                    criatura.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SPEED,
                        999999, 2, false, false
                    ));
                    break;
                case "TANQUE":
                    criatura.customName(net.kyori.adventure.text.Component.text("§c⚔ Forma Colosal"));
                    criatura.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(40.0);
                    criatura.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(0.15);
                    criatura.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.RESISTANCE,
                        999999, 1, false, false
                    ));
                    break;
                case "EXPLOSIVA":
                    criatura.customName(net.kyori.adventure.text.Component.text("§e💥 Forma Volátil"));
                    criatura.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(8.0);
                    criatura.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(0.35);
                    break;
                case "INVOCADORA":
                    criatura.customName(net.kyori.adventure.text.Component.text("§d👁 Forma Arcana"));
                    criatura.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(15.0);
                    criatura.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(0.25);
                    criatura.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.INVISIBILITY,
                        40, 0, false, false // Invisible 2 segundos al spawn
                    ));
                    break;
            }
            
            criatura.setCustomNameVisible(true);
            criatura.setHealth(criatura.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
            
            // NO usar efecto GLOWING permanente - ahora será dinámico
            tipoCriatura.put(criatura.getUniqueId(), tipo);
            glowIntensidad.put(criatura.getUniqueId(), 50); // Intensidad base 50%
            
            // ✨ SLOW FALLING durante caída para efecto dramático
            criatura.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SLOW_FALLING,
                80, // 4 segundos
                0,
                false,
                false
            ));
            
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
                
                // Rastro al moverse
                if (criatura.getVelocity().lengthSquared() > 0.01) {
                    world.spawnParticle(particula, loc, 2, 0.2, 0.2, 0.2, 0.01);
                }
                
                // 💥 EXPLOSIVO - verificar jugador cerca
                if (tipoFinal.equals("EXPLOSIVA") && cercano != null && distMin < 3) {
                    world.createExplosion(loc, 2.0f, false, false);
                    criatura.damage(999);
                }
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
                
                // Cancelar aura si existe
                if (e instanceof Silverfish) {
                    Silverfish criatura = (Silverfish) e;
                    
                    // 👁 INVOCADORA: Spawn de 2 minions al morir
                    String tipo = tipoCriatura.get(e.getUniqueId());
                    if ("INVOCADORA".equals(tipo)) {
                        spawnearMinionsInvocadora(deathLoc, world);
                    }
                    
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
            
            // 🗣️ DIÁLOGO - Oleada completada
            mostrarDialogoForma("OLEADA_COMPLETADA");
            
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
        plugin.getLogger().info("[SusurroPiedraRota] Iniciando transición cinemática a Acto 3 FINAL");
        
        // 🎬 TRANSICIÓN CINEMÁTICA ÉPICA FINAL
        // 1. Fadeout con explosión de partículas
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§8▬", "", 10, 80, 20); // Fade más largo para acto final
            Location pLoc = p.getLocation();
            for (int i = 0; i < 100; i++) {
                double offsetX = (Math.random() - 0.5) * 15;
                double offsetY = (Math.random()) * 8;
                double offsetZ = (Math.random() - 0.5) * 15;
                pLoc.getWorld().spawnParticle(
                    Particle.REVERSE_PORTAL,
                    pLoc.clone().add(offsetX, offsetY, offsetZ),
                    3, 0.2, 0.2, 0.2, 0.05
                );
            }
        }
        playSoundToAll(Sound.ENTITY_WITHER_DEATH, 0.6f, 0.3f);
        playSoundToAll(Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 0.5f);
        
        // 2. Pausa dramática de 4 segundos (acto final merece más pausa)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isActive()) return;
            
            actoActual = Acto.NUCLEO_FORMA;
            ticksEnActo = 0;
            
            // Intensificar ambiente dramáticamente para Acto Final
            intensificarAmbienteActo3();
            
            // ✨ ACTIVAR SISTEMA DE BREADCRUMBS
            iniciarBreadcrumbs();
            
            // 3. TÍTULO ÉPICO FINAL con máxima dramaticidad
            enviarTituloCinematicoTodos(
                "§d§lACTO FINAL",
                "§8✦ §5§lEL CORAZÓN DEL ABISMO §8✦",
                100
            );
            
            // Sonidos épicos múltiples superpuestos
            playSoundToAll(Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
            playSoundToAll(Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.6f);
            playSoundToAll(Sound.BLOCK_END_PORTAL_SPAWN, 0.8f, 0.8f);
            playSoundToAll(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 0.6f);
            
            // Relámpagos cinemáticos
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!isActive()) return;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    for (int i = 0; i < 3; i++) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            p.getWorld().strikeLightningEffect(p.getLocation().clone().add(
                                (Math.random() - 0.5) * 80,
                                0,
                                (Math.random() - 0.5) * 80
                            ));
                        }, i * 10L);
                    }
                }
                playSoundToAll(Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 0.7f);
            }, 20L);
            
            // 4. Mensaje narrativo épico
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!isActive()) return;
                
                broadcastNarrative("§8§m══════════════════════════════════════════════════");
                broadcastNarrative("");
                broadcastNarrative("          §d§l⧗ ACTO FINAL: §5§lEL CORAZÓN DEL ABISMO §d§l⧗");
                broadcastNarrative("");
                broadcastNarrative("    §7✦ La realidad se deforma... algo emerge del vacío");
                broadcastNarrative("    §7✦ El núcleo palpita con energía ancestral");
                broadcastNarrative("    §c✦ Este es el momento final... ¿te atreves?");
                broadcastNarrative("");
                broadcastNarrative("§8§m══════════════════════════════════════════════════");
                
                playSoundToAll(Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);
                playSoundToAll(Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.8f, 1.2f);
            }, 60L);
            
            // 5. Spawn del núcleo con efecto dramático
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!isActive()) return;
                
                // 🗣️ DIÁLOGO - Inicio del Acto 3
                mostrarDialogoForma("ACTO3_INICIO");
                
                // 🎬 PAUSA ÉPICA: Momento crucial de la historia
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (participantesOriginales.contains(p.getUniqueId())) {
                        // Slow motion épico
                        p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.SLOWNESS,
                            140, // 7 segundos
                            4,
                            true,
                            false
                        ));
                        
                        // Efecto de cámara dramático
                        aplicarSlowMotion(p, 7);
                    }
                }
                
                // Delay LARGO para momento épico
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!isActive()) return;
                    
                    spawnearNucleoForma();
                    
                    // Iniciar puzzle de símbolos
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (isActive() && actoActual == Acto.NUCLEO_FORMA) {
                            iniciarPuzzleActo3();
                        }
                    }, 60L);
                }, 140L); // 7 segundos para leer diálogo
            }, 80L);
            
        }, 80L); // 4 segundos de pausa épica
    }
    
    private void spawnearNucleoForma() {
        // NÚCLEO ALEJADO: Colocar en el extremo opuesto del laberinto (radio 15)
        // Calcular posición promedio de jugadores para spawn del laberinto
        Location ubicacionPromedio = calcularUbicacionPromedioJugadores();
        double anguloAlejado = Math.random() * Math.PI * 2; // Ángulo aleatorio
        int radioLaberinto = 15;
        
        nucleoLocation = grietaLocation.clone().add(
            Math.cos(anguloAlejado) * radioLaberinto,
            1.5,
            Math.sin(anguloAlejado) * radioLaberinto
        );
        
        plugin.getLogger().info("[SusurroPiedraRota] Núcleo colocado a " + radioLaberinto + " bloques del centro");
        
        // Distorsión dimensional al formar el núcleo
        efectoDistorsionDimensionalTodos(60);
        
        // Pulso de energía épico
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            efectoPulsoEnergiaTodos();
        }, 30L);
        
        // Crear item frame invisible
        nucleoFrame = (ItemFrame) nucleoLocation.getWorld().spawnEntity(
            nucleoLocation,
            EntityType.ITEM_FRAME
        );
        
        nucleoFrame.setVisible(false);
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
        
        // 🎮 NÚCLEO ÉPICO: Spawn continuo de 1-2 criaturas cada 10 segundos
        BukkitTask spawnTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (actoActual != Acto.NUCLEO_FORMA || nucleoRecogido) {
                return;
            }
            
            Random rand = new Random();
            int cantidadCriaturas = 1 + rand.nextInt(2); // 1-2 criaturas
            
            plugin.getLogger().info(String.format(
                "[SusurroPiedraRota] Núcleo spawneando %d criatura(s) defensiva(s)",
                cantidadCriaturas
            ));
            
            // Anuncio visual cada 2 spawns (cada 20 segundos)
            if (rand.nextBoolean()) {
                broadcastNarrative("§5⚡ El núcleo invoca defensores...");
            }
            
            // Spawn las criaturas cerca del núcleo
            for (int i = 0; i < cantidadCriaturas; i++) {
                final int delay = 5 + (i * 10); // Pequeño delay entre cada criatura
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (isActive() && actoActual == Acto.NUCLEO_FORMA && !nucleoRecogido) {
                        Location spawnLoc = encontrarSpawnSeguro(nucleoLocation, 4, 8);
                        if (spawnLoc == null) {
                            spawnLoc = nucleoLocation.clone().add(0, 1, 0);
                        }
                        spawnearEnUbicacion(spawnLoc);
                    }
                }, delay);
            }
        }, 200L, 200L); // Cada 10 segundos (200 ticks)
        
        // Almacenar la tarea para cancelarla cuando se recoja el núcleo
        nucleoSpawnTask = spawnTask;
    }
    
    private void iniciarEfectosNucleo() {
        // Partículas cinematográficas épicas con múltiples capas AAA + latidos sincronizados
        nucleoParticleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (actoActual != Acto.NUCLEO_FORMA || nucleoRecogido) {
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
        
        // Cancelar efectos
        if (nucleoParticleTask != null) nucleoParticleTask.cancel();
        if (nucleoBeamTask != null) nucleoBeamTask.cancel();
        
        // Completar Acto 3
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                completarActo3();
            }
        }, 40L);
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
        // Verificar progreso del laberinto para cada jugador
        if (!puzzleActo3Completado) {
            // Null safety: verificar que nucleoLocation esté inicializado
            if (nucleoLocation == null) {
                plugin.getLogger().warning("[SusurroPiedraRota] nucleoLocation es null en tickActo3");
                return;
            }
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld() == nucleoLocation.getWorld()) {
                    verificarProgresoLaberinto(p);
                }
            }
        }
        
        // El acto se completa SOLO cuando el puzzle del laberinto esté completado
        // El núcleo solo aparece después de completar el laberinto
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
        
        plugin.getLogger().info("[SusurroPiedraRota] Iniciando Acto 4: El Segundo Susurro");
        
        // Sonido fuerte de roca desgarrándose
        playSoundToAll(Sound.ENTITY_WITHER_BREAK_BLOCK, 1.5f, 0.4f);
        
        // Secuencia de mensajes
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                broadcastNarrative("§8⧖ §7...no aprendieron...");
            }
        }, 40L);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                broadcastNarrative("§8⧖ §7...otra vez...");
            }
        }, 60L);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                broadcastNarrative("");
                broadcastNarrative("§8§o...la figura recuerda...");
                playSoundToAll(Sound.ENTITY_ENDERMAN_STARE, 0.3f, 0.5f);
            }
        }, 100L);
        
        // Pensamiento del Observador (antes del final)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                mostrarPensamientoObservador();
            }
        }, 200L); // 10 segundos
        
        // Cliffhanger
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                mostrarCliffhanger();
            }
        }, 280L); // 14 segundos
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
     * Muestra la presentación del Acto 1 con animación.
     */
    private void mostrarPresentacionActo1(Player player) {
        // Destello épico
        player.sendTitle("§5§l⧖", "§d§lACTO 1", 10, 40, 20);
        soundUtil.playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.2f);
        soundUtil.playSound(player, Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.5f, 0.8f);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            player.sendMessage("");
            player.sendMessage("§8§m══════════════════════════════════════════════════");
            player.sendMessage("");
            
            // Título principal con animación
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(formatearCentrado("§d§l⧖ ACTO I: §5§lLA BÚSQUEDA DE LOS FRAGMENTOS §d§l⧖"));
                soundUtil.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.0f);
            }, 10L);
            
            player.sendMessage("");
            
            // Objetivos con aparición secuencial
            String[] objetivos = {
                "§7✦ Fragmentos antiguos de piedra brillante yacen dispersos",
                "§7✦ Cada fragmento guarda secretos y revelaciones",
                "§7✦ Reúne todos los fragmentos para descubrir la verdad"
            };
            
            for (int i = 0; i < objetivos.length; i++) {
                final int indice = i;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendMessage("    " + objetivos[indice]);
                    soundUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 0.3f, 1.5f + (indice * 0.2f));
                    
                    // Partículas
                    Location loc = player.getLocation();
                    loc.getWorld().spawnParticle(Particle.ENCHANT, loc, 10, 1, 1, 1, 0.5);
                }, 30L + (i * 20L));
            }
            
            // Cierre
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage("");
                player.sendMessage("§8§m══════════════════════════════════════════════════");
            }, 110L);
            
        }, 20L);
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
        plugin.getLogger().info("[SusurroPiedraRota] Evento completado");
        
        actoActual = Acto.VICTORIA;
        tiempoCompletadoEvento = System.currentTimeMillis();
        
        // 🗣️ DIÁLOGO FINAL - Éxito
        mostrarDialogoForma("FINAL_EXITO");
        
        // 🎬 EFECTO DE CÁMARA: Efecto épico final para celebrar victoria
        for (Player p : Bukkit.getOnlinePlayers()) {
            aplicarEfectoEpicoCombinado(p);
        }
        
        // 🏆 Calcular rank final de cada jugador
        calcularRankFinal();
        
        // Calcular rangos de recompensa basados en tiempo (sistema antiguo - mantener por compatibilidad)
        calcularRangosRecompensa();
        
        // Destello épico de victoria
        crearDestelloTodos(10);
        
        // Sacudida celebratoria después del destello
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            sacudirPantallaTodos(2);
        }, 12L);
        
        // Título épico de victoria
        enviarTituloCinematicoTodos(
            "★ EVENTO COMPLETADO ★",
            "El susurro ha sido silenciado... por ahora.",
            80
        );
        
        // Mensaje final
        broadcastNarrative("§8§m                                                    ");
        broadcastNarrative("");
        broadcastNarrative("§5§lEl Susurro en la Piedra Rota");
        broadcastNarrative("§7ha sido completado");
        broadcastNarrative("");
        broadcastNarrative("§8Un nuevo misterio se ha revelado...");
        broadcastNarrative("§8La forma busca recordar.");
        broadcastNarrative("");
        broadcastNarrative("§8§m                                                    ");
        
        playSoundToAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        
        // Entregar recompensas
        entregarRecompensas();
        
        // TÍTULO DE AGRADECIMIENTO
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID uuid : participantesOriginales) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    p.sendTitle(
                        "§d✨ ¡GRACIAS POR JUGAR! ✨",
                        "§7El Observador seguirá vigilando...",
                        20, 100, 30
                    );
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                }
            }
        }, 120L);
        
        // Detener evento y limpieza completa
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            limpiezaCompletaEvento();
            stop();
        }, 200L);
    }
    
    private void calcularRangosRecompensa() {
        long tiempoTotal = tiempoCompletadoEvento - tiempoInicioEvento;
        double minutosTotal = tiempoTotal / 60000.0;
        
        for (UUID uuid : participantesOriginales) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }
            
            int fragmentos = participacionFragmentos.getOrDefault(uuid, 0);
            int puzzles = puzzlesCompletados.getOrDefault(uuid, 0);
            
            // Sistema de rangos progresivos
            String rango;
            if (minutosTotal <= 8 && fragmentos >= 4 && puzzles >= 2) {
                rango = "§b§lPLATINUM"; // Menos de 8 minutos, excelente participación
            } else if (minutosTotal <= 12 && fragmentos >= 3) {
                rango = "§6§lGOLD"; // Menos de 12 minutos, buena participación
            } else if (minutosTotal <= 18 && fragmentos >= 2) {
                rango = "§7§lSILVER"; // Menos de 18 minutos
            } else {
                rango = "§c§lBRONZE"; // Completado pero más lento
            }
            
            rangoRecompensa.put(uuid, rango);
        }
    }
    
    private void entregarRecompensas() {
        ConfigurationSection recompensasConfig = config.getConfigurationSection("recompensas_ps");
        
        // Mini-evento: XP máximo 250
        int baseParticipacion = recompensasConfig.getInt("base_participacion", 30);
        int porFragmento = recompensasConfig.getInt("por_fragmento_inspeccionado", 10);
        int porCriatura = recompensasConfig.getInt("por_criatura_eliminada", 5);
        int porNucleo = recompensasConfig.getInt("por_recoger_nucleo", 50);
        
        for (UUID uuid : participantesOriginales) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }
            
            int psTotal = baseParticipacion;
            
            // PS por fragmentos
            int fragmentos = participacionFragmentos.getOrDefault(uuid, 0);
            psTotal += fragmentos * porFragmento;
            
            // PS por criaturas (aproximado)
            int criaturas = participacionCriaturas.getOrDefault(uuid, 0);
            psTotal += criaturas * porCriatura;
            
            // PS por recoger núcleo
            if (uuid.equals(jugadorQueRecogio)) {
                psTotal += porNucleo;
            }
            
            // BONO POR RANGO FINAL (sistemas de ranks)
            String rankFinal = rangoRecompensa.getOrDefault(uuid, "C");
            int bonusRank = 0;
            
            switch (rankFinal) {
                case "S": bonusRank = 100; break; // Perfecto
                case "A": bonusRank = 70; break;  // Excelente
                case "B": bonusRank = 40; break;  // Bueno
                case "C": bonusRank = 20; break;  // Completado
            }
            
            psTotal += bonusRank;
            
            // LÍMITE MÁXIMO: 250 XP (es mini-evento)
            psTotal = Math.min(psTotal, 250);
            
            // Dar PS
            if (plugin.getExperienceService() != null) {
                plugin.getExperienceService().addXP(player, psTotal, "Evento: El Susurro en la Piedra Rota");
            }
            
            // Mensaje de recompensas épico
            enviarMensajeRecompensas(player, fragmentos, uuid.equals(jugadorQueRecogio), psTotal, rankFinal);
            
            plugin.getLogger().info(String.format(
                "[SusurroPiedraRota] %s recibió %d PS (fragmentos: %d, rank: %s)",
                player.getName(),
                psTotal,
                fragmentos,
                rankFinal
            ));
        }
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
                    
                    // 🧹 Limpiar action bar si está CERCA de la estructura (menos de 15 bloques)
                    if (distancia < 15.0) {
                        player.sendActionBar(net.kyori.adventure.text.Component.text(""));
                        
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
        player.sendTitle(
            formatearGradiente(titulo),
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
     * 🎭 DIÁLOGOS DE LA FORMA
     * Mensajes estilizados que aparecen en momentos clave del evento.
     * La Forma se comunica con los jugadores de manera misteriosa y ominosa.
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
                dialogos.add("§5§l◈ §5§o\"Los fragmentos... me llaman...\"");
                dialogos.add("§5§l◈ §5§o\"¿Pueden escucharme? Deben escucharme...\"");
                dialogos.add("§5§l◈ §5§o\"Estoy dividido... ayúdenme a recordar...\"");
                break;
                
            case "FRAGMENTO_ENCONTRADO":
                dialogos.add("§5§l◈ §5§o\"Sí... eso es parte de mí...\"");
                dialogos.add("§5§l◈ §5§o\"Más cerca... cada vez más cerca...\"");
                dialogos.add("§5§l◈ §5§o\"Puedo sentir mi esencia retornando...\"");
                break;
                
            case "ACTO2_INICIO":
                dialogos.add("§5§l◈ §5§o\"La grieta se abre... mi nacimiento se acerca...\"");
                dialogos.add("§5§l◈ §5§o\"No teman a mis hijos... son parte de mí...\"");
                dialogos.add("§5§l◈ §5§o\"Defiendan este lugar... es sagrado...\"");
                break;
                
            case "CRIATURAS_SPAWN":
                dialogos.add("§5§l◈ §5§o\"Mis fragmentos toman forma...\"");
                dialogos.add("§5§l◈ §5§o\"No es malicia... es necesidad...\"");
                dialogos.add("§5§l◈ §5§o\"Perdonen a mis ecos... no saben lo que hacen...\"");
                break;
                
            case "JUGADOR_MUERTE":
                dialogos.add("§5§l◈ §5§o\"No... necesito que vivan...\"");
                dialogos.add("§5§l◈ §5§o\"Su esencia... se desvanece...\"");
                dialogos.add("§5§l◈ §5§o\"Levántense... aún no es su hora...\"");
                break;
                
            case "OLEADA_COMPLETADA":
                dialogos.add("§5§l◈ §5§o\"Bien hecho... la calma antes de la tormenta...\"");
                dialogos.add("§5§l◈ §5§o\"Respiren... vendrá otra ola...\"");
                dialogos.add("§5§l◈ §5§o\"Sus almas brillan con determinación...\"");
                break;
                
            case "ACTO3_INICIO":
                dialogos.add("§5§l◈ §5§o\"Mi núcleo late... ¿pueden sentirlo?\"");
                dialogos.add("§5§l◈ §5§o\"Estoy tan cerca de la plenitud...\"");
                dialogos.add("§5§l◈ §5§o\"El laberinto guarda mi corazón...\"");
                break;
                
            case "CERCA_NUCLEO":
                dialogos.add("§5§l◈ §5§o\"Lo sienten... mi esencia palpitante...\"");
                dialogos.add("§5§l◈ §5§o\"Tan cerca... tan terriblemente cerca...\"");
                dialogos.add("§5§l◈ §5§o\"Mi corazón resuena con sus pasos...\"");
                break;
                
            case "FINAL_EXITO":
                dialogos.add("§5§l◈ §5§o\"Gracias... soy completo nuevamente...\"");
                dialogos.add("§5§l◈ §5§o\"Recordaré... recordaré sus nombres...\"");
                dialogos.add("§5§l◈ §5§o\"La Forma renace... gracias a ustedes...\"");
                break;
                
            case "FINAL_FRACASO":
                dialogos.add("§5§l◈ §5§o\"No... me desvanezco otra vez...\"");
                dialogos.add("§5§l◈ §5§o\"Estaré aquí... esperando... siempre esperando...\"");
                dialogos.add("§5§l◈ §5§o\"Mi susurro permanecerá en la piedra...\"");
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
        
        // 🔊 Sonido espeluznante con eco
        soundUtil.playSound(player, Sound.ENTITY_ENDERMAN_AMBIENT, 0.6f, 0.5f);
        soundUtil.playSound(player, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.4f, 0.7f);
        soundUtil.playSound(player, Sound.ENTITY_WARDEN_HEARTBEAT, 0.3f, 0.8f);
        
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
            
            // Sonido de eco
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                soundUtil.playSound(player, Sound.ENTITY_ENDERMAN_AMBIENT, 0.3f, 0.6f);
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
    private void calcularRankFinal() {
        long tiempoTotal = (System.currentTimeMillis() - tiempoInicioEvento) / 1000;
        
        for (UUID uuid : participantesOriginales) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            
            final int muertes = contadorMuertes;
            final boolean todoLoreRecolectado = loreRecolectado.getOrDefault(uuid, new HashSet<>()).size() >= 3;
            
            // Calcular puntos base
            int puntosBase = rendimientoJugador.getOrDefault(uuid, 0);
            
            // +200 por completar el evento
            puntosBase += 200;
            
            // Bonus por tiempo (máximo 300 puntos)
            // Menos de 10 min = 300, menos de 15 = 200, menos de 20 = 100
            if (tiempoTotal < 600) puntosBase += 300;
            else if (tiempoTotal < 900) puntosBase += 200;
            else if (tiempoTotal < 1200) puntosBase += 100;
            
            // Penalización por muertes (-100 por muerte)
            puntosBase -= (muertes * 100);
            
            // Bonus por lore completo
            if (todoLoreRecolectado) puntosBase += 150;
            
            final int puntos = puntosBase;
            
            // Determinar rank
            String rank;
            String mensaje;
            String dialogo;
            
            if (puntos >= 800 && muertes == 0 && todoLoreRecolectado) {
                rank = "S";
                mensaje = "§5§l⚡ RANK S - PERFECTO ⚡";
                dialogo = "FINAL_RANK_S";
            } else if (puntos >= 600 && muertes <= 1) {
                rank = "A";
                mensaje = "§d§l★ RANK A - EXCELENTE ★";
                dialogo = "FINAL_RANK_A";
            } else if (puntos >= 400 && muertes <= 3) {
                rank = "B";
                mensaje = "§b§l◆ RANK B - BUENO ◆";
                dialogo = "FINAL_RANK_B";
            } else {
                rank = "C";
                mensaje = "§7§l✓ RANK C - COMPLETADO ✓";
                dialogo = "FINAL_RANK_C";
            }
            
            rangoRecompensa.put(uuid, rank);
            
            // Mostrar resultado al jugador
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) {
                    p.sendMessage("");
                    p.sendMessage(formatearCentrado("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                    p.sendMessage(formatearCentrado(mensaje));
                    p.sendMessage(formatearCentrado("§7Puntuación: §f" + puntos));
                    p.sendMessage(formatearCentrado("§7Tiempo: §f" + formatearTiempo(tiempoTotal)));
                    p.sendMessage(formatearCentrado("§7Muertes: §f" + muertes));
                    p.sendMessage(formatearCentrado("§7Lore: §f" + (todoLoreRecolectado ? "§a✓ Completo" : "§c✗ Incompleto")));
                    p.sendMessage(formatearCentrado("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
                    p.sendMessage("");
                    
                    // Diálogo especial de La Forma según el rank
                    mostrarDialogoRank(p, rank);
                }
            }, 100L);
        }
    }
    
    /**
     * Muestra diálogo especial según el rank obtenido.
     */
    private void mostrarDialogoRank(Player player, String rank) {
        String dialogo;
        
        switch (rank) {
            case "S":
                dialogo = "§5§l◈ §5§o\"Perfección absoluta... eres digno de mi memoria completa...\"";
                soundUtil.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                break;
            case "A":
                dialogo = "§5§l◈ §5§o\"Excelente... me has dado forma nuevamente...\"";
                soundUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                break;
            case "B":
                dialogo = "§5§l◈ §5§o\"Bien hecho... aunque podría haber sido mejor...\"";
                soundUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
                break;
            default:
                dialogo = "§5§l◈ §5§o\"Completado... pero mi forma sigue incompleta...\"";
                soundUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
                break;
        }
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            mostrarDialogoFormaAnimado(player, dialogo);
        }, 20L);
    }
    
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
}

