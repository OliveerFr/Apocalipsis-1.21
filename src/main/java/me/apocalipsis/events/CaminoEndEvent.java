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
    
    // Sistema de brújula funcional
    private BukkitTask brujulaTask;
    
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
        
        // Actualizar brújulas y guía de anomalías (cada segundo)
        if (ticksTotales % 20 == 0 && (faseActual == Fase.ANOMALIAS || faseActual == Fase.RESONANCIA)) {
            actualizarBrujulas();
            mostrarGuiaAnomalias(); // Guía para TODOS los jugadores
        }
        
        // Actualizar anomalías existentes
        actualizarAnomalias();
        
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
                    transicionarAFase(Fase.REVELACION);
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
                    finalizarEvento();
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
        
        switch (nuevaFase) {
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
        
        // Mensaje especial para anomalías raras
        if (tipo == TipoAnomalia.ANTIGUA) {
            messageBus.broadcast("§5§l⚡ EL OBSERVADOR:", "observador");
            messageBus.broadcast("§5§o\"...esto es diferente... MÁS VIEJO...\"", "anomalia_antigua");
        } else if (tipo == TipoAnomalia.INESTABLE) {
            if (random.nextInt(3) == 0) { // 33% chance
                messageBus.broadcast("§e§o\"Una anomalía inestable... ten cuidado...\"", "anomalia_inestable");
            }
        }
        
        // Iniciar efectos visuales
        iniciarEfectosVisualesAnomalia(ubicacion, tipo);
        
        // Iniciar efectos de sonido
        iniciarEfectosSonidosAnomalia(ubicacion, tipo);
        
        plugin.getLogger().info("[CaminoEndEvent] Anomalía " + tipo.name() + " spawneada en: " + 
            ubicacion.getBlockX() + ", " + ubicacion.getBlockY() + ", " + ubicacion.getBlockZ());
    }
    
    private void iniciarEfectosVisualesAnomalia(Location ubicacion, TipoAnomalia tipo) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!anomaliasActivas.containsKey(ubicacion)) {
                return;
            }
            
            World world = ubicacion.getWorld();
            if (world == null) return;
            
            // Haz de luz vertical (visible desde lejos)
            if (ticksTotales % 5 == 0) { // Cada 5 ticks
                for (int altura = 0; altura < 30; altura += 2) {
                    Location hazLoc = ubicacion.clone().add(0, altura, 0);
                    Particle particleHaz = tipo == TipoAnomalia.ANTIGUA ? Particle.DRAGON_BREATH :
                                          (tipo == TipoAnomalia.INESTABLE ? Particle.SOUL_FIRE_FLAME : Particle.END_ROD);
                    world.spawnParticle(particleHaz, hazLoc, 1, 0.1, 0, 0.1, 0);
                }
            }
            
            // Haz de luz vertical (visible desde lejos)
            if (ticksTotales % 5 == 0) { // Cada 5 ticks
                for (int altura = 0; altura < 30; altura += 2) {
                    Location hazLoc = ubicacion.clone().add(0, altura, 0);
                    Particle particleHaz = tipo == TipoAnomalia.ANTIGUA ? Particle.DRAGON_BREATH :
                                          (tipo == TipoAnomalia.INESTABLE ? Particle.SOUL_FIRE_FLAME : Particle.END_ROD);
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
        } else if (tipo == TipoAnomalia.INESTABLE) {
            jugador.sendMessage("§5§l⚡ EL OBSERVADOR:");
            jugador.sendMessage("§e§o\"...cuidado... este fragmento pulsa con poder...\"");
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
        
        // Sistema de tensión: Countdown cuando quedan 5 fragmentos
        int fragmentosFaltantes = FRAGMENTOS_OBJETIVO - fragmentosRecolectadosGlobalmente;
        if (fragmentosFaltantes <= 5 && fragmentosFaltantes > 0) {
            aplicarCountdownTension(fragmentosFaltantes);
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
            portalLocation = world.getSpawnLocation().clone().add(0, 10, 0);
        }
        
        // Construir estructura del portal incompleto
        construirPortalIncompleto(portalLocation);
        
        // Notificar ubicación del portal a todos los jugadores
        notificarUbicacionPortal(portalLocation);
        
        portalGenerado = true;
        
        plugin.getLogger().info("[CaminoEndEvent] Portal incompleto generado en: " + 
            portalLocation.getBlockX() + ", " + portalLocation.getBlockY() + ", " + portalLocation.getBlockZ());
    }
    
    private void construirPortalIncompleto(Location centro) {
        World world = centro.getWorld();
        if (world == null) return;
        
        // Estructura: Marco de End Portal Frame (incompleto - solo 8 de 12 bloques)
        // Layout en forma de cuadrado 5x5
        
        int baseX = centro.getBlockX();
        int baseY = centro.getBlockY();
        int baseZ = centro.getBlockZ();
        
        // Lado norte (3 bloques)
        world.getBlockAt(baseX - 1, baseY, baseZ - 2).setType(Material.END_PORTAL_FRAME);
        world.getBlockAt(baseX, baseY, baseZ - 2).setType(Material.END_PORTAL_FRAME);
        world.getBlockAt(baseX + 1, baseY, baseZ - 2).setType(Material.END_PORTAL_FRAME);
        
        // Lado este (2 bloques)
        world.getBlockAt(baseX + 2, baseY, baseZ - 1).setType(Material.END_PORTAL_FRAME);
        world.getBlockAt(baseX + 2, baseY, baseZ + 1).setType(Material.END_PORTAL_FRAME);
        
        // Lado sur (1 bloque) - INCOMPLETO
        world.getBlockAt(baseX, baseY, baseZ + 2).setType(Material.END_PORTAL_FRAME);
        
        // Lado oeste (2 bloques)
        world.getBlockAt(baseX - 2, baseY, baseZ - 1).setType(Material.END_PORTAL_FRAME);
        world.getBlockAt(baseX - 2, baseY, baseZ + 1).setType(Material.END_PORTAL_FRAME);
        
        // Base decorativa
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (x == 0 && z == 0) continue; // Centro vacío
                world.getBlockAt(baseX + x, baseY - 1, baseZ + z).setType(Material.END_STONE_BRICKS);
            }
        }
        
        // Partículas permanentes alrededor del portal
        iniciarEfectosPortal(centro);
        
        // Corazón pulsante en el centro
        iniciarCorazonPortal(centro);
    }
    
    private void iniciarEfectosPortal(Location centro) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            World world = centro.getWorld();
            if (world == null) return;
            
            // Partículas PORTAL en el centro
            world.spawnParticle(Particle.PORTAL, centro, 20, 1.5, 0.5, 1.5, 0.05);
            
            // Partículas END_ROD en espiral ascendente
            double radio = 2.0;
            for (int i = 0; i < 8; i++) {
                double angulo = (ticksTotales + i * 45) * 0.05;
                double offsetX = radio * Math.cos(angulo);
                double offsetZ = radio * Math.sin(angulo);
                double offsetY = (ticksTotales % 100) * 0.05;
                
                Location particleLoc = centro.clone().add(offsetX, offsetY, offsetZ);
                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
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
        messageBus.broadcast("§5§l⚡ EL OBSERVADOR:", "observador");
        messageBus.broadcast("§7§o\"Las barreras... se debilitan...\"", "fase_resonancia");
        messageBus.broadcast("§7§o\"Ahora... puedo sentir los fragmentos...\"", "fase_resonancia2");
        soundUtil.playSoundAll(Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.5f, 1.0f);
    }
    
    private void anunciarFaseRevelacion() {
        // Momento cinemático: Silencio inicial
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            messageBus.broadcast("§5§l⚡ EL OBSERVADOR:", "observador");
            messageBus.broadcast("§7§o\"Suficiente... la puerta se abre...\"", "fase_revelacion");
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
        messageBus.broadcast("§5§l⚡ EL OBSERVADOR:", "observador");
        messageBus.broadcast("§7§o\"El camino... aún no está completo...\"", "finalizacion");
        messageBus.broadcast("§7§o\"Pero ahora sabemos... que existe.\"", "finalizacion2");
    }
    
    private void finalizarEvento() {
        // Distribuir recompensas
        distribuirRecompensas();
        
        // Detener evento
        stop();
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
            } else if (posicion == 2) {
                // Top 2: Recompensas altas
                recompensasItems.add(new ItemStack(Material.NETHERITE_INGOT, 1));
                recompensasItems.add(new ItemStack(Material.DIAMOND, 12));
                recompensasItems.add(new ItemStack(Material.ENDER_PEARL, 24));
            } else if (posicion == 3) {
                // Top 3: Recompensas buenas
                recompensasItems.add(new ItemStack(Material.DIAMOND, 8));
                recompensasItems.add(new ItemStack(Material.ENDER_PEARL, 16));
            } else {
                // Participantes: Recompensas base
                recompensasItems.add(new ItemStack(Material.DIAMOND, 4));
                recompensasItems.add(new ItemStack(Material.ENDER_PEARL, 8));
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
            // Buscar anomalía más cercana
            Location anomaliaCercana = encontrarAnomaliaMasCercana(jugador.getLocation());
            
            if (anomaliaCercana == null) continue;
            
            AnomaliaData data = anomaliasActivas.get(anomaliaCercana);
            double distancia = jugador.getLocation().distance(anomaliaCercana);
            String flecha = obtenerFlechaDireccional(jugador, anomaliaCercana);
            
            // Mostrar mensaje según distancia
            String mensaje;
            if (distancia < 15) {
                mensaje = String.format("§5§l⚡ ANOMALÍA MUY CERCA %s §e%.0fm §7%s",
                    flecha, distancia, data.tipo.getNombre());
            } else if (distancia < 50) {
                mensaje = String.format("§5§l⚡ Anomalía %s §e%.0fm §7%s",
                    flecha, distancia, data.tipo.getNombre());
            } else {
                mensaje = String.format("§5§l⚡ %s §e%.0fm §7(%s)",
                    flecha, distancia, data.tipo.getNombre());
            }
            
            // Agregar info de cantidad si hay múltiples
            if (anomaliasActivas.size() > 1) {
                mensaje += " §8[" + anomaliasActivas.size() + " activas]";
            }
            
            jugador.sendActionBar(mensaje);
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
                    // Clima misterioso - tormenta ligera
                    world.setStorm(true);
                    world.setWeatherDuration(72000); // 1 hora
                    world.setThundering(false);
                    break;
                    
                case RESONANCIA:
                    // Intensificar - tormenta con rayos ocasionales
                    world.setStorm(true);
                    world.setThundering(true);
                    world.setWeatherDuration(72000);
                    break;
                    
                case REVELACION:
                    // Claro dramático
                    world.setStorm(false);
                    world.setThundering(false);
                    world.setWeatherDuration(36000);
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
     */
    public enum TipoAnomalia {
        NORMAL(1.0, "§7Normal", Particle.PORTAL, Sound.BLOCK_PORTAL_AMBIENT),
        INESTABLE(1.5, "§e§lInestable", Particle.SOUL_FIRE_FLAME, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK),
        ANTIGUA(2.0, "§5§l§kA§r §5§lAntigua§r §5§l§kA", Particle.DRAGON_BREATH, Sound.ENTITY_ENDER_DRAGON_AMBIENT);
        
        public final double multiplicadorPS;  // Multiplicador de recompensa
        public final String nombre;
        public final Particle particula;
        public final Sound sonido;
        
        TipoAnomalia(double multiplicadorPS, String nombre, Particle particula, Sound sonido) {
            this.multiplicadorPS = multiplicadorPS;
            this.nombre = nombre;
            this.particula = particula;
            this.sonido = sonido;
        }
        
        public String getNombre() {
            return nombre;
        }
        
        public static TipoAnomalia obtenerAleatorio(Random random) {
            int valor = random.nextInt(100);
            
            if (valor < 70) return NORMAL;       // 70% normal
            if (valor < 95) return INESTABLE;    // 25% inestable
            return ANTIGUA;                       // 5% antigua (rara)
        }
    }
}

