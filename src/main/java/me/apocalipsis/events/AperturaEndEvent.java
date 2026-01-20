package me.apocalipsis.events;

import java.io.File;
import java.util.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;

// Model Engine imports (opcionales - solo si está instalado)
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;

/**
 * La Apertura del End - Evento 5
 * 
 * Evento raid épico contra el Desolador del Vacío
 * 
 * El portal fragmentado revelado en "El Camino al End" se activa.
 * Los jugadores enfrentan un dragón corrompido que protege el End.
 * 
 * Funciona con MythicMobs + ModelEngine (modo épico) o vanilla mejorado.
 * La detección es automática - el código se adapta según plugins disponibles.
 * 
 * Fases del evento:
 * 1. PREPARACION (30 min): Jugadores se preparan
 * 2. PORTAL_ABIERTO: Portal se activa, esperando jugadores
 * 3. COMBATE: Dragón activo con 4 fases de combate
 * 4. VICTORIA: Cinemática y recompensas
 */
public class AperturaEndEvent extends EventBase {
    
    // ═══════════════════════════════════════════════════════════════════
    // ESTADO DEL EVENTO
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Fases narrativas del evento:
     * 1. DESCUBRIMIENTO - Jugadores en Overworld, viajan al portal (30 min)
     * 2. LLEGADA - Jugadores llegan al portal, se activa lentamente
     * 3. COMBATE - En el End, batalla contra el dragón (4 subfases)
     * 4. VICTORIA - Silencio tras la muerte del dragón
     * 5. CLIFFHANGER - Mensaje final misterioso
     */
    public enum EventPhase {
        INACTIVO,
        DESCUBRIMIENTO,    // Fase 1: Overworld, viaje al portal
        LLEGADA,           // Fase 2: Portal se activa
        PORTAL_ABIERTO,    // Fase 2b: Portal activado, esperando entrada
        COMBATE,           // Fase 3: Batalla en el End
        VICTORIA,          // Fase 4: Dragón muerto, silencio
        CLIFFHANGER        // Fase 5: Mensaje final
    }
    
    /**
     * Subfases del combate (Fase 3) basadas en HP del dragón
     */
    public enum DragonPhase {
        FASE_1_AEREO,      // 100-75% HP
        FASE_2_INVOCADOR,  // 75-50% HP
        FASE_3_DESESPERADO,// 50-25% HP
        FASE_4_FURIA       // 25-0% HP
    }
    
    private EventPhase faseEvento = EventPhase.INACTIVO;
    private DragonPhase faseDragon = DragonPhase.FASE_1_AEREO;
    
    // Timers
    private int descubrimientoTimer = 2700; // 45 minutos en segundos (Fase 1)
    private int combateTicks = 0;
    
    // Portal en Overworld
    private Location portalLocation = null;
    private Location ubicacionIniciador = null;  // Ubicación del jugador que inició el evento
    private boolean portalActivandose = false;
    private int portalActivacionTicks = 0;
    private boolean portalGenerado = false;  // Track si el portal fue construido físicamente
    private BukkitTask efectosPortalTask;
    
    // Dragón
    private EnderDragon dragon = null;
    private double dragonMaxHP = 500.0;
    private int cristalesRestantes = 10;
    
    // Model Engine (modo épico)
    private boolean modelEngineDisponible = false;
    private ModeledEntity modeledDragon = null;
    private String modelId = "corrupted_dragon"; // ID del modelo en Model Engine
    
    // Tracking de participación
    private Map<UUID, Double> damageTracker = new HashMap<>();
    private Set<UUID> participantes = new HashSet<>();
    
    // UI
    private BossBar bossBar;
    
    // Configuración
    private FileConfiguration config;
    
    // Tareas programadas
    private BukkitTask mainTask;
    private BukkitTask preparacionTask;
    private BukkitTask spawnsTask;  // Spawns dramáticos durante el viaje
    private BukkitTask brujulaTask;  // Mostrar dirección al portal
    
    // Sistema de desbloqueo de información
    private int tareasCompletadas = 0;
    private int tareasRequeridas = 3; // Deben completar 3 tareas para desbloquear ubicación
    private Set<String> tareasRealizadas = new HashSet<>();
    private boolean esperandoJugadoresCercaPortal = false;
    
    // Sistema de spawns dramáticos
    private int proximoSpawnTicks = 0;
    private boolean direccionRevelada = false;  // Track si ya se reveló la dirección
    
    // Sistema de waypoints progresivos
    private Location waypointActual = null;
    private int waypointNumero = 0;
    private List<Location> waypointsGenerados = new ArrayList<>();
    private BukkitTask waypointParticlesTask = null;
    
    private final Random random = new Random();
    
    // ═══════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════
    
    public AperturaEndEvent(Apocalipsis plugin, MessageBus messageBus, SoundUtil soundUtil) {
        super(plugin, messageBus, soundUtil, "apertura_end");
        loadConfig();
        detectarModelEngine();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Detecta si Model Engine está instalado y disponible
     */
    private void detectarModelEngine() {
        try {
            Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
            modelEngineDisponible = Bukkit.getPluginManager().isPluginEnabled("ModelEngine");
            
            if (modelEngineDisponible) {
                plugin.getLogger().info("[Apertura End] ✓ Model Engine detectado - Modo épico activado");
                
                // Leer ID del modelo desde config
                modelId = config.getString("modelo.model_engine_id", "corrupted_dragon");
                plugin.getLogger().info("[Apertura End] Modelo configurado: " + modelId);
            } else {
                plugin.getLogger().info("[Apertura End] Model Engine no detectado - Usando dragón vanilla mejorado");
            }
        } catch (ClassNotFoundException e) {
            modelEngineDisponible = false;
            plugin.getLogger().info("[Apertura End] Model Engine no instalado - Usando dragón vanilla mejorado");
        }
    }
    
    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "apertura_end.yml");
        
        if (!configFile.exists()) {
            plugin.saveResource("apertura_end.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Cargar configuración básica
        descubrimientoTimer = config.getInt("evento.duracion_fase_descubrimiento_segundos", 2700);
        dragonMaxHP = config.getDouble("evento.escalado.hp_base", 500.0);
        
        plugin.getLogger().info("[Apertura End] Configuración cargada");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // IMPLEMENTACIÓN DE EventBase
    // ═══════════════════════════════════════════════════════════════════
    
    @Override
    public void onStart() {
        faseEvento = EventPhase.DESCUBRIMIENTO;
        descubrimientoTimer = config.getInt("evento.duracion_fase_descubrimiento_segundos", 2700);
        
        // Limpiar estado
        damageTracker.clear();
        participantes.clear();
        dragon = null;
        cristalesRestantes = 10;
        portalLocation = null;
        portalActivandose = false;
        portalActivacionTicks = 0;
        
        // Generar ubicación del portal (lejos del spawn)
        generarUbicacionPortal();
        
        // Crear BossBar
        bossBar = Bukkit.createBossBar(
            "§8Descubrimiento: 30:00",
            BarColor.PURPLE,
            BarStyle.SEGMENTED_10
        );
        
        // Añadir todos los jugadores online
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(player);
        }
        bossBar.setVisible(true);
        
        // Mensaje inicial (SIN dirección aún)
        mostrarMensajeInicialSinCoordenadas();
        
        // Iniciar verificación de agrupación para revelar dirección
        verificarAgrupacionParaDireccion();
        
        // Iniciar countdown
        iniciarCountdown();
        
        // Iniciar spawns dramáticos
        iniciarSpawnsDramaticos();
        
        plugin.getLogger().info("[Apertura End] Evento iniciado - Fase de preparación");
    }
    
    @Override
    public void onStop() {
        // Limpiar dragón si existe
        if (dragon != null && !dragon.isDead()) {
            
            // Limpiar modelo de Model Engine primero
            if (modelEngineDisponible && modeledDragon != null) {
                try {
                    modeledDragon.destroy();
                    plugin.getLogger().info("[Apertura End] Modelo de Model Engine destruido");
                } catch (Exception e) {
                    plugin.getLogger().warning("[Apertura End] Error al destruir modelo: " + e.getMessage());
                }
                modeledDragon = null;
            }
            
            dragon.remove();
        }
        
        // Limpiar BossBar
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
        
        // Cancelar tareas
        if (mainTask != null) {
            mainTask.cancel();
        }
        if (preparacionTask != null) {
            preparacionTask.cancel();
        }
        if (efectosPortalTask != null) {
            efectosPortalTask.cancel();
        }
        if (spawnsTask != null) {
            spawnsTask.cancel();
        }
        if (brujulaTask != null) {
            brujulaTask.cancel();
        }
        
        faseEvento = EventPhase.INACTIVO;
        
        plugin.getLogger().info("[Apertura End] Evento detenido");
    }
    
    @Override
    public void onTick() {
        // El tick principal se maneja en las tareas programadas
    }
    
    @Override
    public String getDisplayName() {
        return config.getString("metadata.nombre_display", "§5§lLa Apertura del End");
    }
    
    @Override
    public String getDescription() {
        return "Evento raid épico contra el Desolador del Vacío";
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // GESTIÓN DE FASES
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Genera ubicación aleatoria del portal en el Overworld
     * Lejos del spawn (2000-5000 bloques)
     */
    private void generarUbicacionPortal() {
        World overworld = Bukkit.getWorld(config.getString("evento.mundo_overworld", "world"));
        if (overworld == null) {
            plugin.getLogger().warning("[Apertura End] Mundo Overworld no encontrado!");
            return;
        }
        
        int minDist = config.getInt("evento.portal.distancia_minima_spawn", 2000);
        int maxDist = config.getInt("evento.portal.distancia_maxima_spawn", 5000);
        
        // Usar ubicación del iniciador si está disponible, sino usar spawn del mundo
        Location puntoReferencia;
        if (ubicacionIniciador != null) {
            puntoReferencia = ubicacionIniciador;
            plugin.getLogger().info("[Apertura End] Generando portal desde ubicación del iniciador");
        } else {
            puntoReferencia = overworld.getSpawnLocation();
            plugin.getLogger().info("[Apertura End] Generando portal desde spawn del mundo (sin iniciador)");
        }
        
        // Generar coordenadas aleatorias desde el punto de referencia
        int distancia = minDist + random.nextInt(maxDist - minDist);
        double angulo = random.nextDouble() * 2 * Math.PI;
        
        int x = puntoReferencia.getBlockX() + (int) (distancia * Math.cos(angulo));
        int z = puntoReferencia.getBlockZ() + (int) (distancia * Math.sin(angulo));
        int y = overworld.getHighestBlockYAt(x, z);
        
        portalLocation = new Location(overworld, x, y, z);
        
        plugin.getLogger().info(String.format("[Apertura End] Portal generado en: X=%d Y=%d Z=%d (distancia: %d bloques)", 
            x, y, z, (int) puntoReferencia.distance(portalLocation)));
    }
    
    /**
     * FASE 1: Countdown de descubrimiento (45 minutos)
     * Los jugadores deben viajar al portal
     */
    private void iniciarCountdown() {
        // Mensaje inicial con instrucciones claras
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§5§l⚡ INSTRUCCIONES DEL OBSERVADOR ⚡");
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§7El portal al §5End §7está materializándose...");
                Bukkit.broadcastMessage("§7Pero su ubicación está §8borrosa§7.");
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§e§lDebéis completar 3 tareas para revelar la ubicación:");
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§8  1. §5Eliminar un Enderman §8- Los emisarios del End están aquí");
                Bukkit.broadcastMessage("§8  2. §7Recolectar Obsidiana §8- El material del portal");
                Bukkit.broadcastMessage("§8  3. §eConseguir un Ojo de Ender §8- La clave dimensional");
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§8§oLos Endermans aparecerán continuamente...");
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
                    p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 1.5f);
                }
            }
        }.runTaskLater(plugin, 40L);
        
        // Spawn continuo de Endermans durante la fase de descubrimiento
        spawnsTask = new BukkitRunnable() {
            int ticksTranscurridos = 0;
            
            @Override
            public void run() {
                if (faseEvento != EventPhase.DESCUBRIMIENTO) {
                    cancel();
                    return;
                }
                
                ticksTranscurridos++;
                
                // Spawnear Endermans cada 2 minutos (2400 ticks)
                if (ticksTranscurridos % 2400 == 0) {
                    List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
                    if (onlinePlayers.isEmpty()) return;
                    
                    // Spawnear 1-3 Endermans cerca de jugadores aleatorios
                    int cantidadEndermans = 1 + random.nextInt(3);
                    
                    for (int i = 0; i < cantidadEndermans; i++) {
                        Player targetPlayer = onlinePlayers.get(random.nextInt(onlinePlayers.size()));
                        Location spawnLoc = targetPlayer.getLocation().clone();
                        
                        // Spawnear a 15-30 bloques del jugador
                        double distancia = 15 + random.nextDouble() * 15;
                        double angulo = random.nextDouble() * 2 * Math.PI;
                        
                        spawnLoc.add(
                            distancia * Math.cos(angulo),
                            0,
                            distancia * Math.sin(angulo)
                        );
                        
                        spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);
                        
                        org.bukkit.entity.Enderman enderman = (org.bukkit.entity.Enderman) spawnLoc.getWorld().spawnEntity(
                            spawnLoc, org.bukkit.entity.EntityType.ENDERMAN
                        );
                        
                        enderman.customName(net.kyori.adventure.text.Component.text("§5Emisario del End"));
                        enderman.setCustomNameVisible(true);
                        
                        // Efectos visuales de aparición
                        spawnLoc.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, spawnLoc, 50, 0.5, 1, 0.5, 0.1);
                        spawnLoc.getWorld().spawnParticle(org.bukkit.Particle.REVERSE_PORTAL, spawnLoc, 30, 0.5, 1, 0.5, 0.05);
                        
                        // Sonido de aparición
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.getWorld().equals(spawnLoc.getWorld()) && p.getLocation().distance(spawnLoc) < 50) {
                                p.playSound(spawnLoc, Sound.ENTITY_ENDERMAN_AMBIENT, 1.0f, 0.8f);
                                p.playSound(spawnLoc, Sound.BLOCK_END_PORTAL_SPAWN, 0.3f, 1.5f);
                            }
                        }
                    }
                    
                    if (!tareasRealizadas.contains("matar_enderman")) {
                        Bukkit.broadcastMessage("§8[§7...§8] §5Emisarios del End §7han aparecido en el mundo...");
                    }
                }
            }
        }.runTaskTimer(plugin, 1200L, 20L); // Iniciar después de 1 minuto, check cada segundo
        
        preparacionTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (faseEvento != EventPhase.DESCUBRIMIENTO) {
                    cancel();
                    return;
                }
                
                descubrimientoTimer--;
                
                // Actualizar BossBar
                int minutos = descubrimientoTimer / 60;
                int segundos = descubrimientoTimer % 60;
                bossBar.setTitle(String.format("§8Descubrimiento: %02d:%02d", minutos, segundos));
                bossBar.setProgress(Math.max(0.0, (double) descubrimientoTimer / 2700.0));
                
                // ═══ MENSAJES DEL OBSERVADOR CON TÍTULOS/SUBTÍTULOS ═══
                
                // 40 minutos (2400s) - Misterio inicial + PRIMER WAYPOINT (80% distancia)
                if (descubrimientoTimer == 2400) {
                    // Fade to black inicial
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle("§8§l█████████████", "§8§l█████████████", 20, 40, 20);
                    }
                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Bukkit.broadcastMessage("§8[§7...§8] §7Huele a… antes.");
                            
                            // Efectos de cenizas (smoke particles)
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                Location loc = p.getLocation();
                                p.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(0, 2, 0), 30, 2, 1, 2, 0.02);
                                p.getWorld().spawnParticle(Particle.ASH, loc, 50, 3, 2, 3, 0.01);
                                p.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.3f, 0.5f);
                            }
                        }
                    }.runTaskLater(plugin, 60L);
                    
                    programarMensajeRetrasado("§8[§7...§8] §7Cenizas. Vacío. Vestigios.", 120);
                    
                    // CREAR PRIMER WAYPOINT (80% del camino)
                    crearWaypoint(0.8, "§5§l⚡ ECO DISTANTE", "§7Algo resuena al norte...");
                }
                
                // 35 minutos (2100s) - Nostalgia profunda + efectos de memoria
                if (descubrimientoTimer == 2100) {
                    // Secuencia cinematográfica
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        // Pantalla oscura inicial
                        p.sendTitle("§8§l...", "", 15, 50, 15);
                        p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1.5f, 0.6f);
                        p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 0.3f);
                        
                        // DARKNESS - Oscuridad envolvente (5 segundos)
                        p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.DARKNESS, 100, 0, false, false, false));
                    }
                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            mostrarTituloObserver("§8§o...", "§7§oEste umbral… lo recuerdo sellado.", 10, 80, 30);
                            
                            // Partículas de recuerdo (enchant + portal)
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                Location loc = p.getLocation();
                                p.getWorld().spawnParticle(Particle.ENCHANT, loc.clone().add(0, 2.5, 0), 40, 2, 0.5, 2, 0.5);
                                p.getWorld().spawnParticle(Particle.PORTAL, loc, 20, 1.5, 1, 1.5, 0.1);
                            }
                        }
                    }.runTaskLater(plugin, 50L);
                }
                
                // 30 minutos (1800s) - Reflexión temporal + distorsión + SEGUNDO WAYPOINT
                if (descubrimientoTimer == 1800) {
                    Bukkit.broadcastMessage("§8[§7...§8] §7Antes, esto tomaba más tiempo.");
                    
                    // Efecto de distorsión temporal
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        Location loc = p.getLocation();
                        p.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, 1, 0), 60, 2, 2, 2, 0.5);
                        p.playSound(loc, Sound.BLOCK_PORTAL_TRAVEL, 0.3f, 1.8f);
                        p.playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 0.5f);
                    }
                    
                    programarMensajeRetrasado("§8[§7...§8] §7Siglos. No minutos.", 80);
                    
                    // CREAR SEGUNDO WAYPOINT más cercano (60% del camino)
                    crearWaypoint(0.6, "§5§l⚡ RESONANCIA CRECIENTE", "§7La energía se intensifica...");
                }
                
                // 25 minutos (1500s) - Tristeza profunda + oscuridad envolvente
                if (descubrimientoTimer == 1500) {
                    // Fade oscuro dramático + DARKNESS
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle("§8§l█", "§8§l█", 20, 60, 20);
                        
                        // DARKNESS - Oscuridad profunda (8 segundos)
                        p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.DARKNESS, 160, 0, false, false, false));
                    }
                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            mostrarTituloObserver("§8§o...", "§7§oCuántas veces he visto este momento.", 10, 100, 30);
                            
                            // Efectos de tristeza (warped spore + soul)
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                Location loc = p.getLocation();
                                p.getWorld().spawnParticle(Particle.WARPED_SPORE, loc, 40, 3, 2, 3, 0.02);
                                p.getWorld().spawnParticle(Particle.SOUL, loc.clone().add(0, 0.5, 0), 15, 1, 0.5, 1, 0.01);
                                p.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.4f, 0.5f);
                                p.playSound(loc, Sound.PARTICLE_SOUL_ESCAPE, 0.8f, 0.6f);
                            }
                        }
                    }.runTaskLater(plugin, 80L);
                    
                    programarMensajeRetrasado("§8[§7...§8] §7Y cuántas más faltan.", 140);
                }
                
                // 20 minutos (1200s) - Tensión creciente + persecución
                if (descubrimientoTimer == 1200) {
                    Bukkit.broadcastMessage("§8[§7...§8] §7El mundo está… apurado.");
                    
                    // Efectos de persecución
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        Location loc = p.getLocation();
                        p.getWorld().spawnParticle(Particle.SCULK_SOUL, loc.clone().add(0, 0.2, 0), 25, 2, 0.2, 2, 0.1);
                        p.playSound(loc, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.3f, 0.8f);
                    }
                    
                    new BukkitRunnable() {
                        int tick = 0;
                        @Override
                        public void run() {
                            if (tick == 0) {
                                mostrarTituloObserver("§8§o...", "§7§oComo si algo lo persiguiera.", 10, 70, 20);
                            }
                            
                            // Latidos aumentando
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f + (tick * 0.1f), 0.8f);
                                
                                if (tick % 20 == 0) {
                                    Location loc = p.getLocation();
                                    p.getWorld().spawnParticle(Particle.CRIMSON_SPORE, loc, 10, 1.5, 1, 1.5, 0.05);
                                }
                            }
                            
                            tick++;
                            if (tick >= 60) {
                                this.cancel();
                                
                                // TERCER WAYPOINT - Mucho más cerca (40% del camino)
                                crearWaypoint(0.4, "§5§l⚡ LLAMADO DEL VACÍO", "§7El portal está cerca...");
                            }
                        }
                    }.runTaskTimer(plugin, 60L, 1L);
                }
                
                // 15 minutos (900s) - URGENCIA + efectos épicos
                if (descubrimientoTimer == 900) {
                    // Efecto de revelacióncinematográfico
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle("§5§l⚡", "§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬", 10, 40, 10);
                    }
                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            mostrarTituloObserver("§8§l15 MINUTOS", "§7El portal se acerca", 10, 60, 20);
                            
                            // Explosión de partículas épica
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                Location loc = p.getLocation();
                                p.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 3, 0), 50, 0.5, 2, 0.5, 0.2);
                                p.getWorld().spawnParticle(Particle.PORTAL, loc, 100, 3, 2, 3, 1.0);
                                p.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 40, 2, 1, 2, 0.05);
                                
                                p.playSound(loc, Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.2f, 0.8f);
                                p.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 1.5f);
                                p.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 1.8f);
                            }
                        }
                    }.runTaskLater(plugin, 30L);
                    
                    programarMensajeRetrasado("§8[§7...§8] §7No fueron suficientes la última vez.", 80);
                    programarMensajeRetrasado("§8[§7...§8] §7Ni la anterior.", 140);
                }
                
                // 10 minutos (600s) - Vacío envolvente + DARKNESS
                if (descubrimientoTimer == 600) {
                    // Pantalla de vacío + DARKNESS
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle("§0§l████", "§0§l████", 20, 60, 20);
                        
                        // DARKNESS - Vacío total (10 segundos)
                        p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.DARKNESS, 200, 0, false, false, false));
                    }
                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            mostrarTituloObserver("§8§o...", "§7§oPuedo sentir el vacío desde aquí.", 15, 80, 25);
                            
                            // Vacío envolvente
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                Location loc = p.getLocation();
                                p.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 2, 0), 60, 3, 2, 3, 0.02);
                                p.getWorld().spawnParticle(Particle.SMOKE, loc, 80, 2, 1, 2, 0.05);
                                p.getWorld().spawnParticle(Particle.SCULK_SOUL, loc, 30, 2, 1.5, 2, 0.1);
                                
                                p.playSound(loc, Sound.AMBIENT_CAVE, 2.0f, 0.4f);
                                p.playSound(loc, Sound.ENTITY_WARDEN_AMBIENT, 0.6f, 0.5f);
                                p.playSound(loc, Sound.AMBIENT_BASALT_DELTAS_MOOD, 0.8f, 0.3f);
                            }
                        }
                    }.runTaskLater(plugin, 80L);
                    
                    programarMensajeRetrasado("§8[§7...§8] §7Frío. Silencioso. Esperando.", 160);
                    
                    // CUARTO WAYPOINT muy cerca (20% del camino) + EMPEZAR EMPUJES
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            crearWaypoint(0.2, "§5§l⚡ PORTAL EMERGENTE", "§c§l¡EL PORTAL ESTÁ MUY CERCA!");
                            
                            // EMPEZAR EMPUJES HACIA EL PORTAL cada 30 segundos
                            iniciarEmpujesHaciaPortal();
                        }
                    }.runTaskLater(plugin, 200L);
                }
                
                // 5 minutos (300s) - ALTA TENSIÓN + DARKNESS
                if (descubrimientoTimer == 300) {
                    // Secuencia apocalíptica
                    new BukkitRunnable() {
                        int fase = 0;
                        @Override
                        public void run() {
                            if (fase == 0) {
                                // Flash rojo + DARKNESS
                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    p.sendTitle("§4§l⚠", "§4§l⚠⚠⚠⚠⚠⚠⚠⚠⚠", 5, 15, 5);
                                    
                                    // DARKNESS - Terror creciente (6 segundos)
                                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                        org.bukkit.potion.PotionEffectType.DARKNESS, 120, 0, false, false, false));
                                }
                            } else if (fase == 1) {
                                mostrarTituloObserver("§c§l5 MINUTOS", "§7§oYa casi están ahí", 10, 60, 20);
                                
                                // Efectos apocalípticos
                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    Location loc = p.getLocation();
                                    
                                    // Explosión de partículas dramática
                                    p.getWorld().spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 1, 0), 5);
                                    p.getWorld().spawnParticle(Particle.LAVA, loc, 40, 2, 1, 2, 0.1);
                                    p.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 80, 3, 2, 3, 0.1);
                                    p.getWorld().spawnParticle(Particle.PORTAL, loc, 150, 4, 2, 4, 1.5);
                                    p.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 3, 0), 60, 1, 2, 1, 0.3);
                                    
                                    p.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.7f);
                                    p.playSound(loc, Sound.ENTITY_WARDEN_ROAR, 0.8f, 0.5f);
                                    p.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.8f);
                                }
                            }
                            
                            fase++;
                            if (fase >= 2) {
                                this.cancel();
                            }
                        }
                    }.runTaskTimer(plugin, 0L, 20L);
                    
                    programarMensajeRetrasado("§8[§7...§8] §7Si cruzan ese umbral, no hay marcha atrás.", 80);
                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            mostrarTituloObserver("§8§o...", "§7§oNunca la hay.", 10, 60, 20);
                            
                            // Eco oscuro
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                Location loc = p.getLocation();
                                p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 0.3, 0), 30, 2, 0.5, 2, 0.02);
                                p.getWorld().spawnParticle(Particle.SMOKE, loc, 60, 2, 2, 2, 0.05);
                                p.playSound(loc, Sound.ENTITY_WARDEN_LISTENING, 0.8f, 0.5f);
                            }
                        }
                    }.runTaskLater(plugin, 140L);
                }
                
                // 3 minutos (180s) - Advertencia final + ambiente terrorífico + DARKNESS
                if (descubrimientoTimer == 180) {
                    // DARKNESS inicial - Ambiente de terror (4 segundos)
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.DARKNESS, 80, 0, false, false, false));
                    }
                    
                    Bukkit.broadcastMessage("§8[§7...§8] §7El End no olvida.");
                    
                    // Ambiente de terror
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        Location loc = p.getLocation();
                        p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 0.3, 0), 30, 2, 0.5, 2, 0.02);
                        p.getWorld().spawnParticle(Particle.SMOKE, loc, 60, 2, 2, 2, 0.05);
                        p.playSound(loc, Sound.ENTITY_WARDEN_LISTENING, 0.8f, 0.5f);
                    }
                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            mostrarTituloObserver("§8§l...", "§7§oNi perdona.", 10, 70, 20);
                            
                            // Partículas de condena
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                Location loc = p.getLocation();
                                p.getWorld().spawnParticle(Particle.SCULK_CHARGE, loc, 40, 3, 1, 3, 0.5);
                                p.getWorld().spawnParticle(Particle.CRIMSON_SPORE, loc, 50, 2, 2, 2, 0.1);
                                p.playSound(loc, Sound.ENTITY_WARDEN_AMBIENT, 1.0f, 0.6f);
                                p.playSound(loc, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.5f, 0.7f);
                            }
                        }
                    }.runTaskLater(plugin, 100L);
                    
                    // WAYPOINT FINAL - Casi en el portal (5% del camino) + TELETRANSPORTE INTENSIVO
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            crearWaypoint(0.05, "§c§l⚡ EPICENTRO DEL VACÍO", "§c§l¡¡EL PORTAL ESTÁ AQUÍ!!");
                            
                            // TELETRANSPORTE MÁS AGRESIVO cada 15 segundos
                            iniciarTeletransporteIntensivo();
                        }
                    }.runTaskLater(plugin, 120L);
                }
                
                // 1 minuto (60s) - MÁXIMA TENSIÓN + secuencia épica final + DARKNESS TOTAL
                if (descubrimientoTimer == 60) {
                    // Secuencia cinematográfica final
                    new BukkitRunnable() {
                        int tick = 0;
                        @Override
                        public void run() {
                            if (tick == 0) {
                                // Pantalla negra inicial + DARKNESS máxima
                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    p.sendTitle("§0§l███████", "§0§l███████", 10, 30, 10);
                                    
                                    // DARKNESS - Máxima oscuridad (15 segundos)
                                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                        org.bukkit.potion.PotionEffectType.DARKNESS, 300, 0, false, false, false));
                                }
                            } else if (tick == 40) {
                                // Revelación dramática
                                mostrarTituloObserver("§c§l1 MINUTO", "§8§oEl portal despierta", 15, 70, 25);
                                
                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    Location loc = p.getLocation();
                                    
                                    // Explosión masiva de efectos
                                    p.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc.clone().add(0, 2, 0), 3);
                                    p.getWorld().spawnParticle(Particle.PORTAL, loc, 200, 5, 3, 5, 2.0);
                                    p.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 120, 4, 2, 4, 0.5);
                                    p.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 4, 0), 80, 2, 3, 2, 0.5);
                                    p.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc, 100, 3, 2, 3, 1.0);
                                    
                                    p.playSound(loc, Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 2.0f);
                                    p.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.5f);
                                    p.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
                                    p.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, 1.5f, 0.5f);
                                }
                            }
                            
                            tick++;
                            if (tick >= 80) {
                                this.cancel();
                            }
                        }
                    }.runTaskTimer(plugin, 0L, 1L);
                    
                    programarMensajeRetrasado("§8[§7...§8] §7Deberían haber traído más que armadura.", 100);
                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            mostrarTituloObserver("§8§o...", "§7§oDeberían haber traído esperanza.", 15, 80, 25);
                            
                            // Partículas de desesperanza
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                Location loc = p.getLocation();
                                p.getWorld().spawnParticle(Particle.SOUL, loc, 60, 3, 2, 3, 0.05);
                                p.getWorld().spawnParticle(Particle.ASH, loc, 80, 3, 2, 3, 0.1);
                            }
                        }
                    }.runTaskLater(plugin, 160L);
                }
                
                // Countdown final (10 segundos) - Intensidad máxima + DARKNESS pulsante
                if (descubrimientoTimer <= 10 && descubrimientoTimer > 0) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendTitle("§c§l" + descubrimientoTimer, "§8§oEl vacío espera", 5, 10, 5);
                        
                        Location loc = player.getLocation();
                        // Efectos crecientes según se acerca el final
                        int intensidad = (11 - descubrimientoTimer) * 10;
                        player.getWorld().spawnParticle(Particle.PORTAL, loc, intensidad, 1, 1, 1, 0.5);
                        player.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 2, 0), intensidad / 2, 0.5, 1, 0.5, 0.1);
                        
                        player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HAT, 1.5f, 0.8f);
                        player.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f + (intensidad * 0.01f), 1.0f);
                        
                        if (descubrimientoTimer <= 5) {
                            player.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 2.0f);
                            
                            // DARKNESS pulsante en últimos 5 segundos
                            if (descubrimientoTimer % 2 == 0) {
                                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                    org.bukkit.potion.PotionEffectType.DARKNESS, 40, 0, false, false, false));
                            }
                        }
                    }
                }
                
                // Verificar si hay jugadores cerca del portal
                if (portalLocation != null && direccionRevelada) {
                    checkJugadoresCercaPortal();
                }
                
                // Si se acaba el tiempo, NO forzar - esperar a que lleguen
                if (descubrimientoTimer <= 0) {
                    if (!esperandoJugadoresCercaPortal) {
                        esperandoJugadoresCercaPortal = true;
                        Bukkit.broadcastMessage("");
                        Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        Bukkit.broadcastMessage("");
                        Bukkit.broadcastMessage("§c§l⚠ TIEMPO AGOTADO ⚠");
                        Bukkit.broadcastMessage("");
                        Bukkit.broadcastMessage("§7El portal está listo...");
                        Bukkit.broadcastMessage("§7Pero nadie está cerca para verlo activarse.");
                        Bukkit.broadcastMessage("");
                        Bukkit.broadcastMessage("§8El evento continuará cuando alguien se acerque.");
                        Bukkit.broadcastMessage("");
                        Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
                        }
                    }
                    // NO cancelar el task, seguir verificando proximidad
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Cada segundo
    }
    
    /**
     * Muestra un título del Observer a todos los jugadores
     * Usado para mensajes de alta carga emocional (nostalgia, tristeza, tensión)
     */
    private void mostrarTituloObserver(String titulo, String subtitulo, int fadeIn, int stay, int fadeOut) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(titulo, subtitulo, fadeIn, stay, fadeOut);
        }
    }
    
    /**
     * Programa un mensaje del Observer con delay (en ticks)
     */
    private void programarMensajeRetrasado(String mensaje, long delayTicks) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage(mensaje);
            }
        }.runTaskLater(plugin, delayTicks);
    }
    
    /**
     * Crea un waypoint que guía progresivamente hacia el portal
     * @param factorDistancia 0.0 = portal, 1.0 = posición inicial
     */
    private void crearWaypoint(double factorDistancia, String nombre, String mensaje) {
        if (portalLocation == null) return;
        
        // Calcular posición del waypoint entre los jugadores y el portal
        Location promedioJugadores = calcularCentroJugadores();
        if (promedioJugadores == null) return;
        
        // Interpolar entre posición actual y portal
        double x = promedioJugadores.getX() + (portalLocation.getX() - promedioJugadores.getX()) * (1.0 - factorDistancia);
        double z = promedioJugadores.getZ() + (portalLocation.getZ() - promedioJugadores.getZ()) * (1.0 - factorDistancia);
        double y = portalLocation.getWorld().getHighestBlockYAt((int)x, (int)z) + 1;
        
        waypointActual = new Location(portalLocation.getWorld(), x, y, z);
        waypointNumero++;
        waypointsGenerados.add(waypointActual.clone());
        
        // Anuncio
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(nombre);
        Bukkit.broadcastMessage(mensaje);
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7Una señal ha aparecido guiándoos...");
        Bukkit.broadcastMessage("§8§oSigue el rastro de partículas");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Efectos de aparición del waypoint
        for (int i = 0; i < 3; i++) {
            final int iteration = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    waypointActual.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, waypointActual.clone().add(0, 2, 0), 1);
                    waypointActual.getWorld().spawnParticle(Particle.END_ROD, waypointActual, 100, 2, 3, 2, 0.3);
                    waypointActual.getWorld().spawnParticle(Particle.PORTAL, waypointActual, 200, 3, 2, 3, 1.0);
                    
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.2f + (iteration * 0.2f));
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.5f);
                    }
                }
            }.runTaskLater(plugin, i * 20L);
        }
        
        // Cancelar partículas anteriores si existen
        if (waypointParticlesTask != null && !waypointParticlesTask.isCancelled()) {
            waypointParticlesTask.cancel();
        }
        
        // Iniciar partículas continuas del waypoint
        waypointParticlesTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (faseEvento != EventPhase.DESCUBRIMIENTO || waypointActual == null) {
                    cancel();
                    return;
                }
                
                // Pilar de luz hacia el cielo
                for (int y = 0; y < 50; y += 2) {
                    Location particleLoc = waypointActual.clone().add(0, y, 0);
                    waypointActual.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 2, 0.1, 0, 0.1, 0.01);
                    waypointActual.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 5, 0.3, 0, 0.3, 0.05);
                }
                
                // Anillo en el suelo
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                    double x = Math.cos(angle) * 3;
                    double z = Math.sin(angle) * 3;
                    Location ringLoc = waypointActual.clone().add(x, 0.2, z);
                    waypointActual.getWorld().spawnParticle(Particle.REVERSE_PORTAL, ringLoc, 1, 0, 0, 0, 0);
                }
                
                // Guiar a jugadores con partículas individuales
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.getWorld().equals(waypointActual.getWorld())) continue;
                    
                    Location playerLoc = p.getLocation();
                    double distancia = playerLoc.distance(waypointActual);
                    
                    if (distancia > 10 && distancia < 500) {
                        // Crear camino de partículas hacia el waypoint
                        org.bukkit.util.Vector direction = waypointActual.toVector().subtract(playerLoc.toVector()).normalize();
                        
                        for (int i = 1; i <= 10; i++) {
                            Location pathLoc = playerLoc.clone().add(direction.clone().multiply(i * 3));
                            pathLoc.setY(playerLoc.getY() + 1);
                            p.spawnParticle(Particle.ENCHANT, pathLoc, 2, 0.1, 0.1, 0.1, 0);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 60L, 10L);
    }
    
    /**
     * Calcula el centro promedio de todos los jugadores online
     */
    private Location calcularCentroJugadores() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.isEmpty()) return null;
        
        double sumX = 0, sumY = 0, sumZ = 0;
        World world = players.get(0).getWorld();
        
        for (Player p : players) {
            if (p.getWorld().equals(world)) {
                sumX += p.getLocation().getX();
                sumY += p.getLocation().getY();
                sumZ += p.getLocation().getZ();
            }
        }
        
        int count = players.size();
        return new Location(world, sumX / count, sumY / count, sumZ / count);
    }
    
    /**
     * Inicia sistema de empujes suaves hacia el portal (cada 30 segundos)
     */
    private void iniciarEmpujesHaciaPortal() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (faseEvento != EventPhase.DESCUBRIMIENTO || portalLocation == null) {
                    cancel();
                    return;
                }
                
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.getWorld().equals(portalLocation.getWorld())) continue;
                    
                    double distancia = p.getLocation().distance(portalLocation);
                    
                    // Solo empujar si están lejos (más de 100 bloques)
                    if (distancia > 100) {
                        // Vector hacia el portal
                        org.bukkit.util.Vector direction = portalLocation.toVector()
                            .subtract(p.getLocation().toVector())
                            .normalize()
                            .multiply(0.3); // Empuje suave
                        
                        p.setVelocity(p.getVelocity().add(direction));
                        
                        // Efectos visuales
                        p.spawnParticle(Particle.PORTAL, p.getLocation(), 20, 1, 1, 1, 0.1);
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 1.5f);
                        p.sendActionBar("§5§l» §7Algo te atrae hacia el portal... §5§l«");
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 600L); // Cada 30 segundos
    }
    
    /**
     * Inicia teletransporte intensivo hacia el portal (últimos 3 minutos)
     */
    private void iniciarTeletransporteIntensivo() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (faseEvento != EventPhase.DESCUBRIMIENTO || portalLocation == null) {
                    cancel();
                    return;
                }
                
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.getWorld().equals(portalLocation.getWorld())) continue;
                    
                    double distancia = p.getLocation().distance(portalLocation);
                    
                    // Teletransportar si están muy lejos (más de 200 bloques)
                    if (distancia > 200) {
                        // Calcular punto intermedio (acercar 100 bloques hacia el portal)
                        org.bukkit.util.Vector direction = portalLocation.toVector()
                            .subtract(p.getLocation().toVector())
                            .normalize()
                            .multiply(100);
                        
                        Location newLoc = p.getLocation().clone().add(direction);
                        newLoc.setY(portalLocation.getWorld().getHighestBlockYAt(newLoc) + 1);
                        
                        // Efectos pre-teletransporte
                        p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation(), 100, 1, 2, 1, 1.0);
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
                        
                        // Teletransportar
                        p.teleport(newLoc);
                        
                        // Efectos post-teletransporte
                        p.getWorld().spawnParticle(Particle.REVERSE_PORTAL, newLoc, 100, 1, 2, 1, 1.0);
                        p.playSound(newLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
                        
                        p.sendTitle("§5§l⚡", "§7El Vacío te arrastra", 10, 30, 10);
                        p.sendMessage("§8[§7...§8] §5El portal te llama...");
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 300L); // Cada 15 segundos
    }
    
    /**
     * Verifica si hay jugadores cerca del portal para activar Fase 2
     */
    private void checkJugadoresCercaPortal() {
        if (portalLocation == null) return;
        if (faseEvento != EventPhase.DESCUBRIMIENTO) return;
        
        int radioDeteccion = config.getInt("evento.portal.radio_deteccion_llegada", 50);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(portalLocation.getWorld())) {
                double distancia = player.getLocation().distance(portalLocation);
                
                if (distancia <= radioDeteccion) {
                    // ¡Jugadores llegaron al portal!
                    iniciarFaseLlegada();
                    if (preparacionTask != null) {
                        preparacionTask.cancel();
                    }
                    return;
                }
            }
        }
    }
    
    /**
     * FASE 2: Llegada al portal
     * Jugadores detectados cerca, portal se activa lentamente
     */
    private void iniciarFaseLlegada() {
        faseEvento = EventPhase.LLEGADA;
        portalActivandose = true;
        portalActivacionTicks = 0;
        
        // Anunciar llegada
        List<String> broadcast = config.getStringList("mensajes.fase_2_deteccion.broadcast");
        for (String msg : broadcast) {
            Bukkit.broadcastMessage(msg);
        }
        
        // Mensajes del Observador
        Bukkit.broadcastMessage("§8[§7...§8] §7Aquí fue donde todo terminó… más de una vez.");
        
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("§8[§7...§8] §7Y aun así, siguen entrando.");
            }
        }.runTaskLater(plugin, 40L);
        
        // CONSTRUIR PORTAL ÉPICO (si no existe ya)
        if (portalLocation != null && !portalGenerado) {
            construirPortalEpico(portalLocation);
            portalGenerado = true;
        }
        
        // Iniciar animación de activación del portal
        activarPortalLentamente();
        
        plugin.getLogger().info("[Apertura End] Fase 2: LLEGADA - Portal activándose");
    }
    
    /**
     * FASE 2: Activación lenta del portal (10 segundos)
     */
    private void activarPortalLentamente() {
        bossBar.setTitle("§8El portal se activa...");
        bossBar.setColor(BarColor.WHITE);
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (faseEvento != EventPhase.LLEGADA) {
                    cancel();
                    return;
                }
                
                ticks++;
                portalActivacionTicks = ticks;
                
                // Partículas y sonidos incrementales durante la activación
                if (portalLocation != null && ticks % 10 == 0) {
                    Location loc = portalLocation.clone().add(0, 1, 0);
                    
                    // Intensidad creciente
                    int intensidad = (ticks * 20) / 200; // 0-20 durante 200 ticks
                    
                    portalLocation.getWorld().spawnParticle(
                        org.bukkit.Particle.END_ROD, 
                        loc, 
                        10 + intensidad, 
                        2, 2, 2, 
                        0.02
                    );
                    
                    portalLocation.getWorld().spawnParticle(
                        org.bukkit.Particle.PORTAL,
                        loc,
                        15 + intensidad,
                        1.5, 1.5, 1.5,
                        0.05
                    );
                    
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getWorld().equals(portalLocation.getWorld())) {
                            player.playSound(portalLocation, Sound.BLOCK_END_PORTAL_SPAWN, 0.5f + (ticks / 200.0f), 0.8f);
                        }
                    }
                }
                
                // Después de 10 segundos (200 ticks), portal activado
                if (ticks >= 200) {
                    portalActivado();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * FASE 2: Portal completamente activado
     * Esperando a que jugadores entren al End
     */
    private void portalActivado() {
        faseEvento = EventPhase.PORTAL_ABIERTO;
        
        // Título y mensaje
        String titulo = config.getString("mensajes.fase_2_activacion.titulo", "§8§l...");
        String subtitulo = config.getString("mensajes.fase_2_activacion.subtitulo", "§7El portal está activo");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(titulo, subtitulo, 10, 60, 20);
            player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 2.0f, 0.8f);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.5f);
        }
        
        // Broadcast
        List<String> broadcast = config.getStringList("mensajes.fase_2_activacion.broadcast");
        for (String msg : broadcast) {
            Bukkit.broadcastMessage(msg);
        }
        
        Bukkit.broadcastMessage("§8El portal espera.");
        
        bossBar.setTitle("§8El portal está activo - Entra al End");
        bossBar.setColor(BarColor.PURPLE);
        
        // INICIAR EFECTOS ÉPICOS DEL PORTAL
        if (portalLocation != null) {
            iniciarEfectosPortalEpico(portalLocation.clone().add(0, 1, 0));
        }
        
        // Detectar cuando jugadores entren al End
        detectarEntradaEnd();
        
        plugin.getLogger().info("[Apertura End] Fase 2: Portal activado - Esperando jugadores");
    }
    
    private void detectarEntradaEnd() {
        mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (faseEvento != EventPhase.PORTAL_ABIERTO) {
                    cancel();
                    return;
                }
                
                // Contar jugadores en el End
                long jugadoresEnEnd = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.getWorld().getEnvironment() == World.Environment.THE_END)
                    .count();
                
                if (jugadoresEnEnd >= 1) {
                    AperturaEndEvent.this.iniciarFaseCombate();
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    /**
     * FASE 3: Combate (End)
     * Los jugadores entraron al End, comienza la batalla
     * Llamar este método cuando se detecte que jugadores entraron al End
     */
    public void iniciarFaseCombate() {
        faseEvento = EventPhase.COMBATE;
        faseDragon = DragonPhase.FASE_1_AEREO;
        
        // Mensajes del Observador al entrar
        List<String> broadcast = config.getStringList("mensajes.fase_3_entrada_end.broadcast");
        for (String msg : broadcast) {
            Bukkit.broadcastMessage(msg);
        }
        
        Bukkit.broadcastMessage("§8[§7...§8] §7No es el mismo lugar.");
        
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("§8[§7...§8] §7Nunca lo es.");
            }
        }.runTaskLater(plugin, 40L);
        
        // Esperar 20 segundos antes de spawnear el dragón (tensión)
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                ticks++;
                
                // Efectos de tensión
                if (ticks % 40 == 0) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getWorld().getEnvironment() == World.Environment.THE_END) {
                            player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1.0f, 0.3f);
                            player.spawnParticle(org.bukkit.Particle.SMOKE, player.getLocation(), 10, 2, 2, 2, 0.01);
                        }
                    }
                }
                
                // Después de 20 segundos, spawnear dragón
                if (ticks >= 400) {
                    spawnearDragon();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        plugin.getLogger().info("[Apertura End] Fase 3: COMBATE - Tensión antes del dragón");
    }
    
    /**
     * Spawnea el dragón con efectos sutiles
     */
    private void spawnearDragon() {
        World endWorld = Bukkit.getWorld(config.getString("evento.mundo_end", "world_the_end"));
        
        if (endWorld == null) {
            plugin.getLogger().severe("[Apertura End] ¡Mundo End no encontrado!");
            Bukkit.broadcastMessage("§c§lERROR: No se pudo spawnear el dragón.");
            onStop();
            return;
        }
        
        // Spawn en el centro del End (0, 80, 0)
        Location spawnLoc = new Location(endWorld, 0, 80, 0);
        
        // Mensaje y título antes del spawn
        String titulo = config.getString("mensajes.fase_3_dragon_spawn.titulo", "§8...");
        String subtitulo = config.getString("mensajes.fase_3_dragon_spawn.subtitulo", "§5El dragón emerge");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(titulo, subtitulo, 10, 80, 20);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.6f);
            player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1.0f, 0.4f);
        }
        
        // Broadcast
        List<String> broadcast = config.getStringList("mensajes.fase_3_dragon_spawn.broadcast");
        for (String msg : broadcast) {
            Bukkit.broadcastMessage(msg);
        }
        
        // Calcular jugadores para escalar HP
        int jugadoresEnEnd = (int) Bukkit.getOnlinePlayers().stream()
            .filter(p -> p.getWorld().getEnvironment() == World.Environment.THE_END)
            .count();
        
        dragonMaxHP = calcularHPEscalado(jugadoresEnEnd);
        
        // MODO MODEL ENGINE (si está disponible)
        if (modelEngineDisponible) {
            spawnearDragonModelEngine(spawnLoc, jugadoresEnEnd);
        } else {
            // MODO VANILLA MEJORADO
            spawnearDragonVanilla(spawnLoc);
        }
        
        // Actualizar BossBar
        bossBar.setTitle("§8§l━━━ §5I §8§l━━━");
        bossBar.setColor(BarColor.PURPLE);
        bossBar.setProgress(1.0);
        
        // Mensajes del Observador
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("§8[§7...§8] §7Antes era un final.");
            }
        }.runTaskLater(plugin, 60L);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("§8[§7...§8] §7Ahora… es solo otro paso.");
            }
        }.runTaskLater(plugin, 100L);
        
        // Iniciar tracking del dragón
        iniciarTrackingDragon();
        
        plugin.getLogger().info("[Apertura End] Dragón spawneado - HP: " + dragonMaxHP + " (Jugadores: " + jugadoresEnEnd + ")");
    }
    
    /**
     * Spawnea dragón con Model Engine (modo épico)
     */
    private void spawnearDragonModelEngine(Location loc, int jugadores) {
        try {
            // Spawnear dragón vanilla invisible (hitbox)
            dragon = (EnderDragon) loc.getWorld().spawnEntity(loc, EntityType.ENDER_DRAGON);
            dragon.setCustomName("§8El Desolador del Vacío");
            dragon.setCustomNameVisible(false);
            dragon.setInvulnerable(false);
            dragon.setAI(true);
            dragon.setMaxHealth(dragonMaxHP);
            dragon.setHealth(dragonMaxHP);
            
            // Hacer el dragón vanilla invisible
            dragon.setInvisible(true);
            dragon.setSilent(false); // Mantener sonidos
            
            // Crear modelo de Model Engine (API R4+)
            modeledDragon = ModelEngineAPI.createModeledEntity(dragon);
            
            if (modeledDragon != null) {
                // Añadir el modelo al dragón
                ActiveModel model = ModelEngineAPI.createActiveModel(modelId);
                
                if (model != null) {
                    modeledDragon.addModel(model, true);
                    model.setCanHurt(true);
                    
                    plugin.getLogger().info("[Apertura End] ✓ Modelo '" + modelId + "' aplicado al dragón");
                    
                    // Efectos visuales épicos
                    loc.getWorld().spawnParticle(Particle.PORTAL, loc, 200, 2, 2, 2, 0.5);
                    loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 100, 3, 3, 3, 0.1);
                    loc.getWorld().spawnParticle(Particle.END_ROD, loc, 50, 1, 1, 1, 0.2);
                    
                    Bukkit.broadcastMessage("§8[§5...§8] §d§lEl Desolador del Vacío ha despertado.");
                } else {
                    plugin.getLogger().warning("[Apertura End] ⚠ No se pudo cargar el modelo '" + modelId + "' - usando vanilla");
                    fallbackVanilla();
                }
            } else {
                plugin.getLogger().warning("[Apertura End] ⚠ No se pudo crear ModeledEntity - usando vanilla");
                fallbackVanilla();
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("[Apertura End] ✖ Error al spawnear con Model Engine: " + e.getMessage());
            e.printStackTrace();
            fallbackVanilla();
        }
    }
    
    /**
     * Spawnea dragón vanilla mejorado (modo compatibilidad)
     */
    private void spawnearDragonVanilla(Location loc) {
        dragon = (EnderDragon) loc.getWorld().spawnEntity(loc, EntityType.ENDER_DRAGON);
        dragon.setCustomName("§8El Desolador del Vacío");
        dragon.setCustomNameVisible(false);
        dragon.setMaxHealth(dragonMaxHP);
        dragon.setHealth(dragonMaxHP);
        
        // Efectos visuales vanilla mejorados
        loc.getWorld().spawnParticle(Particle.PORTAL, loc, 100, 2, 2, 2, 0.3);
        loc.getWorld().spawnParticle(Particle.SMOKE, loc, 50, 1, 1, 1, 0.1);
        
        Bukkit.broadcastMessage("§8[§7...§8] §7Un dragón antiguo emerge de las sombras.");
    }
    
    /**
     * Fallback a modo vanilla si Model Engine falla
     */
    private void fallbackVanilla() {
        if (dragon != null) {
            dragon.setInvisible(false); // Hacer visible el vanilla
            Bukkit.broadcastMessage("§8[§7...§8] §7Un dragón antiguo emerge de las sombras.");
        }
    }
    
    private double calcularHPEscalado(int jugadores) {
        double hpBase = config.getDouble("escalado.hp_base", 500.0);
        double multiplicador = config.getDouble("escalado.hp_por_jugador", 0.15);
        
        return hpBase * (1.0 + (jugadores * multiplicador));
    }
    
    private void iniciarTrackingDragon() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (faseEvento != EventPhase.COMBATE || dragon == null || dragon.isDead()) {
                    if (dragon != null && dragon.isDead()) {
                        onDragonDeath();
                    }
                    cancel();
                    return;
                }
                
                combateTicks++;
                
                // Actualizar BossBar
                double hpPercent = dragon.getHealth() / dragonMaxHP;
                bossBar.setProgress(Math.max(0.0, Math.min(1.0, hpPercent)));
                
                // Actualizar fase del dragón
                actualizarFaseDragon(hpPercent);
                
                // Mostrar diálogos del Observador
                mostrarDialogosFase(hpPercent);
            }
        }.runTaskTimer(plugin, 0L, 20L); // Cada segundo
    }
    
    private void actualizarFaseDragon(double hpPercent) {
        DragonPhase faseAnterior = faseDragon;
        
        if (hpPercent <= 0.25) {
            faseDragon = DragonPhase.FASE_4_FURIA;
        } else if (hpPercent <= 0.50) {
            faseDragon = DragonPhase.FASE_3_DESESPERADO;
        } else if (hpPercent <= 0.75) {
            faseDragon = DragonPhase.FASE_2_INVOCADOR;
        } else {
            faseDragon = DragonPhase.FASE_1_AEREO;
        }
        
        // Anunciar cambio de fase (sutil)
        if (faseAnterior != faseDragon) {
            String nombreFase = getNombreFase(faseDragon);
            bossBar.setTitle(nombreFase);
            
            // Color de bossbar según fase
            if (faseDragon == DragonPhase.FASE_4_FURIA) {
                bossBar.setColor(BarColor.RED);
            } else {
                bossBar.setColor(BarColor.PURPLE);
            }
            
            // Sonido sutil
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.6f);
            }
            
            plugin.getLogger().info("[Apertura End] Fase del dragón: " + faseDragon + " (" + (hpPercent * 100) + "% HP)");
        }
    }
    
    private String getNombreFase(DragonPhase fase) {
        switch (fase) {
            case FASE_1_AEREO: return "§8§l━━━ §5I §8§l━━━";
            case FASE_2_INVOCADOR: return "§8§l━━━ §5II §8§l━━━";
            case FASE_3_DESESPERADO: return "§8§l━━━ §5III §8§l━━━";
            case FASE_4_FURIA: return "§8§l━━━ §4IV §8§l━━━";
            default: return "§8...";
        }
    }
    
    private String getDescripcionFase(DragonPhase fase) {
        String path = "fases_evento.fase_3_combate.subfases_dragon.";
        switch (fase) {
            case FASE_1_AEREO: 
                return config.getString(path + "subfase_1.descripcion", "El dragón se mueve con fuerza renovada.");
            case FASE_2_INVOCADOR: 
                return config.getString(path + "subfase_2.descripcion", "La corrupción se manifiesta.");
            case FASE_3_DESESPERADO: 
                return config.getString(path + "subfase_3.descripcion", "Algo cambia en su comportamiento.");
            case FASE_4_FURIA: 
                return config.getString(path + "subfase_4.descripcion", "El final se acerca.");
            default: 
                return "";
        }
    }
    
    /**
     * Muestra diálogos del Observador según % de HP del dragón
     * Los diálogos están definidos en apertura_end.yml
     */
    private void mostrarDialogosFase(double hpPercent) {
        int hpPercentInt = (int) (hpPercent * 100);
        
        // Construir path según la fase actual
        String subfaseKey = "subfase_" + getFaseNumero(faseDragon);
        String path = "fases_evento.fase_3_combate.subfases_dragon." + subfaseKey + ".dialogos_observador." + hpPercentInt;
        
        if (config.contains(path)) {
            String dialogo = config.getString(path);
            
            // Solo mostrar una vez por HP
            if (dialogo != null && !hasShownDialogue(hpPercentInt)) {
                Bukkit.broadcastMessage(dialogo);
                markDialogueShown(hpPercentInt);
            }
        }
    }
    
    private int getFaseNumero(DragonPhase fase) {
        switch (fase) {
            case FASE_1_AEREO: return 1;
            case FASE_2_INVOCADOR: return 2;
            case FASE_3_DESESPERADO: return 3;
            case FASE_4_FURIA: return 4;
            default: return 0;
        }
    }
    
    private Set<Integer> dialogosShown = new HashSet<>();
    
    private boolean hasShownDialogue(int hp) {
        return dialogosShown.contains(hp);
    }
    
    private void markDialogueShown(int hp) {
        dialogosShown.add(hp);
    }
    
    /**
     * FASE 4: Victoria
     * El dragón ha muerto, comienza la secuencia de victoria
     */
    private void onDragonDeath() {
        faseEvento = EventPhase.VICTORIA;
        
        plugin.getLogger().info("[Apertura End] Fase 4: VICTORIA - Dragón derrotado");
        
        // Esperar a que termine la cinemática vanilla (3 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            mostrarVictoria();
        }, 60L);
        
        // Cliffhanger después de la victoria
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            mostrarCliffhanger();
        }, 260L); // 13 segundos después de la muerte
        
        // Dar recompensas
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            distribuirRecompensas();
        }, 200L);
    }
    
    /**
     * FASE 4: Mostrar mensajes de victoria (sin celebración)
     */
    private void mostrarVictoria() {
        // Título sutil
        String titulo = config.getString("mensajes.fase_4_victoria.titulo", "§8...");
        String subtitulo = config.getString("mensajes.fase_4_victoria.subtitulo", "§7El End guarda silencio");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(titulo, subtitulo, 10, 60, 20);
        }
        
        // Broadcast de la secuencia
        List<String> primeraLinea = new ArrayList<>();
        primeraLinea.add("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        primeraLinea.add("§f");
        primeraLinea.add("§8El End guarda silencio.");
        primeraLinea.add("§f");
        
        for (String msg : primeraLinea) {
            Bukkit.broadcastMessage(msg);
        }
        
        // Segunda parte después de 3 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.broadcastMessage("§7El portal de regreso aparece.");
            Bukkit.broadcastMessage("§8Pero algo ha cambiado.");
        }, 60L);
        
        // Cerrar el marco después de 5 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.broadcastMessage("§f");
            Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            Bukkit.broadcastMessage("§7Revisen sus inventarios...");
        }, 100L);
        
        // Sonido sutil
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1.0f, 0.4f);
        }
        
        // Actualizar BossBar
        bossBar.setTitle("§8...");
        bossBar.setColor(BarColor.WHITE);
        bossBar.setProgress(0.0);
    }
    
    /**
     * FASE 5: Cliffhanger
     * Mensaje final misterioso que no resuelve nada
     */
    private void mostrarCliffhanger() {
        faseEvento = EventPhase.CLIFFHANGER;
        
        plugin.getLogger().info("[Apertura End] Fase 5: CLIFFHANGER");
        
        // Título misterioso
        String titulo = config.getString("mensajes.fase_5_cliffhanger.titulo", "§8§l...");
        String subtitulo = config.getString("mensajes.fase_5_cliffhanger.subtitulo", "§7...");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(titulo, subtitulo, 10, 100, 20);
            player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 2.0f, 0.3f);
        }
        
        // Mensaje final
        Bukkit.broadcastMessage("§f");
        Bukkit.broadcastMessage("§8§l⚡ §f§oAlgo se ha activado más allá de este mundo. §8§l⚡");
        Bukkit.broadcastMessage("§f");
        
        // Actualizar BossBar
        bossBar.setTitle("§8§l...");
        bossBar.setVisible(true);
        
        // Desactivar BossBar después de 10 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (bossBar != null) {
                bossBar.setVisible(false);
            }
        }, 200L);
        
        plugin.getLogger().info("[Apertura End] Evento completado - Cliffhanger mostrado");
    }
    
    private void distribuirRecompensas() {
        plugin.getLogger().info("[Apertura End] Distribuyendo recompensas...");
        
        // TODO: Implementar sistema de recompensas
        // - Top 3 jugadores con más daño
        // - Recompensas de participación
        // - Drops custom
        
        Bukkit.broadcastMessage("§a[Sistema] Recompensas distribuidas (WIP)");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // MÉTODOS PÚBLICOS PARA COMANDOS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Comando /avo evento5 skip
     * Salta la Fase 1 (Descubrimiento) y va directo a Fase 2 (Llegada)
     */
    public void saltarDescubrimiento() {
        if (faseEvento == EventPhase.DESCUBRIMIENTO) {
            descubrimientoTimer = 0;
            if (preparacionTask != null) {
                preparacionTask.cancel();
            }
            iniciarFaseLlegada();
        }
    }
    
    /**
     * Método legacy para compatibilidad
     */
    public void saltarPreparacion() {
        saltarDescubrimiento();
    }
    
    public void forzarFase(int fase) {
        if (dragon == null || dragon.isDead()) {
            return;
        }
        
        double[] hpThresholds = {1.0, 0.75, 0.50, 0.25, 0.0};
        
        if (fase >= 1 && fase <= 4) {
            double nuevoHP = dragonMaxHP * hpThresholds[fase];
            dragon.setHealth(Math.max(1.0, nuevoHP));
        }
    }
    
    public void añadirDaño(Player jugador, double cantidad) {
        UUID uuid = jugador.getUniqueId();
        damageTracker.put(uuid, damageTracker.getOrDefault(uuid, 0.0) + cantidad);
        participantes.add(uuid);
    }
    
    public void matarDragon() {
        if (dragon != null && !dragon.isDead()) {
            dragon.setHealth(0);
        }
    }
    
    public String getEstadoEvento() {
        StringBuilder sb = new StringBuilder();
        sb.append("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("§8  ESTADO DEL EVENTO\n");
        sb.append("§8━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("\n");
        sb.append("§e► Fase: §7").append(getNombreFaseEvento(faseEvento)).append("\n");
        
        if (faseEvento == EventPhase.DESCUBRIMIENTO && portalLocation != null) {
            sb.append("§e► Portal ubicado en: §7X=").append(portalLocation.getBlockX())
                .append(" Z=").append(portalLocation.getBlockZ()).append("\n");
            int minutos = descubrimientoTimer / 60;
            int segundos = descubrimientoTimer % 60;
            sb.append("§e► Tiempo restante: §7").append(String.format("%02d:%02d", minutos, segundos)).append("\n");
        }
        
        if (dragon != null && !dragon.isDead()) {
            double hpPercent = (dragon.getHealth() / dragonMaxHP) * 100;
            sb.append("§e► Fase dragón: §5").append(getNombreFase(faseDragon)).append("\n");
            sb.append("§e► HP del dragón: §c").append(String.format("%.0f", dragon.getHealth()))
                .append("§7/§c").append(String.format("%.0f", dragonMaxHP))
                .append(" §7(").append(String.format("%.1f", hpPercent)).append("%)\n");
        }
        
        sb.append("§e► Cristales restantes: §d").append(cristalesRestantes).append("§7/§d10\n");
        sb.append("§e► Participantes: §b").append(participantes.size()).append("\n");
        
        return sb.toString();
    }
    
    private String getNombreFaseEvento(EventPhase fase) {
        switch (fase) {
            case DESCUBRIMIENTO: return "§8Descubrimiento (Viaje al portal)";
            case LLEGADA: return "§8Llegada (Portal activándose)";
            case COMBATE: return "§5Combate (Batalla en el End)";
            case VICTORIA: return "§7Victoria (Silencio)";
            case CLIFFHANGER: return "§8...";
            default: return "Inactivo";
        }
    }
    
    public EventPhase getFaseEvento() {
        return faseEvento;
    }
    
    public int getDescubrimientoTimer() {
        return descubrimientoTimer;
    }
    
    public Location getPortalLocation() {
        return portalLocation;
    }
    
    /**
     * Salta el tiempo de descubrimiento al siguiente diálogo importante
     * Permite testear cada mensaje/animación sin esperar los 45 minutos completos
     * IMPORTANTE: Pone el timer 5 segundos ANTES del checkpoint para que el diálogo se ejecute
     * @return segundos a los que saltó, o -1 si ya terminó
     */
    public int saltarAlSiguienteDialogo() {
        if (faseEvento != EventPhase.DESCUBRIMIENTO) {
            return -1;
        }
        
        // Diálogos en orden descendente (segundos restantes)
        int[] dialogos = {2400, 2100, 1800, 1500, 1200, 900, 600, 300, 180, 60, 10, 0};
        
        // Encontrar el siguiente diálogo menor al tiempo actual
        for (int dialogo : dialogos) {
            if (descubrimientoTimer > dialogo) {
                // Poner el timer 5 segundos ANTES del checkpoint para que se ejecute correctamente
                // Excepción: para countdown final (<=10s) saltar exacto
                int timerTarget = (dialogo > 10) ? dialogo + 5 : dialogo;
                descubrimientoTimer = timerTarget;
                plugin.getLogger().info(String.format("[Apertura End] Timer saltado a %d segundos (%d min) - Checkpoint: %d", 
                    timerTarget, timerTarget / 60, dialogo));
                return timerTarget;
            }
        }
        
        // Si ya estamos en 0 o menos, forzar fin
        descubrimientoTimer = 0;
        return 0;
    }
    
    /**
     * Establece la ubicación del jugador que inició el evento
     * Debe llamarse ANTES de start() para que el portal se genere desde esta ubicación
     */
    public void setUbicacionIniciador(Location ubicacion) {
        this.ubicacionIniciador = ubicacion;
        plugin.getLogger().info(String.format("[Apertura End] Ubicación iniciador establecida: X=%d Y=%d Z=%d", 
            ubicacion.getBlockX(), ubicacion.getBlockY(), ubicacion.getBlockZ()));
    }
    
    public void registrarDaño(UUID jugadorUUID, double daño) {
        damageTracker.put(jugadorUUID, damageTracker.getOrDefault(jugadorUUID, 0.0) + daño);
        participantes.add(jugadorUUID);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE PORTAL ÉPICO (Mejorado desde Evento 4)
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Construye el portal épico completo con CONSTRUCCIÓN GRADUAL ANIMADA
     * El portal se materializa durante 30 segundos con efectos sorprendentes
     */
    private void construirPortalEpico(Location centro) {
        World world = centro.getWorld();
        if (world == null) return;
        
        int baseX = centro.getBlockX();
        int baseY = centro.getBlockY();
        int baseZ = centro.getBlockZ();
        
        plugin.getLogger().info("[Apertura End] ⚡⚡⚡ INICIANDO CONSTRUCCIÓN ÉPICA DEL PORTAL ⚡⚡⚡");
        Bukkit.getServer().broadcast(Component.text("§5§l⚡ ⚡ ⚡ ALGO MASIVO SE ESTÁ MATERIALIZANDO... ⚡ ⚡ ⚡").color(NamedTextColor.DARK_PURPLE));
        
        // CONSTRUCCIÓN GRADUAL ANIMADA - 30 SEGUNDOS DE ESPECTÁCULO ÉPICO
        new BukkitRunnable() {
            int fase = 0;
            
            @Override
            public void run() {
                try {
                    switch (fase) {
                        case 0:
                            // FASE 1: TERRAFORMACIÓN (0-3 seg)
                            Bukkit.getServer().broadcast(Component.text("§8⚡ El terreno tiembla violentamente... §8⚡").color(NamedTextColor.DARK_GRAY));
                            terraformarAreaPortal(world, baseX, baseY, baseZ);
                            efectoExplosionMasiva(centro, 30);
                            break;
                            
                        case 1:
                            // FASE 2: GRIETAS (3-6 seg)
                            Bukkit.getServer().broadcast(Component.text("§5⚡ Grietas dimensionales desgarran la realidad... §5⚡").color(NamedTextColor.DARK_PURPLE));
                            generarGrietasDimensionales(world, baseX, baseY, baseZ);
                            efectoRayosEnergia(centro, 20);
                            break;
                            
                        case 2:
                            // FASE 3: ANILLOS (6-9 seg)
                            Bukkit.getServer().broadcast(Component.text("§d⚡ Anillos de energía se forman en el aire... §d⚡").color(NamedTextColor.LIGHT_PURPLE));
                            construirAnillosConcentricos(world, baseX, baseY, baseZ);
                            efectoOndasExpansivas(centro, 5);
                            break;
                            
                        case 3:
                            // FASE 4: PLATAFORMA MASIVA (9-12 seg)
                            Bukkit.getServer().broadcast(Component.text("§b⚡ Una plataforma colosal emerge del vacío... §b⚡").color(NamedTextColor.AQUA));
                            construirPlataformaMasiva(world, baseX, baseY, baseZ);
                            efectoLluviaCristales(centro, 50);
                            break;
                            
                        case 4:
                            // FASE 5: TORRES (12-15 seg)
                            Bukkit.getServer().broadcast(Component.text("§e⚡ Torres monumentales de 25 bloques se alzan... §e⚡").color(NamedTextColor.GOLD));
                            construirTorresMonumentales(world, baseX, baseY, baseZ);
                            efectoErupcionEnergia(centro, 8);
                            break;
                            
                        case 5:
                            // FASE 6: CRISTALES (15-18 seg)
                            Bukkit.getServer().broadcast(Component.text("§3⚡ Cristales gigantes brotan del suelo corrupto... §3⚡").color(NamedTextColor.DARK_AQUA));
                            generarCristalesGigantes(world, baseX, baseY, baseZ);
                            efectoLluviaParticulas(centro, 70, Particle.END_ROD);
                            break;
                            
                        case 6:
                            // FASE 7: MARCO (18-24 seg)
                            Bukkit.getServer().broadcast(Component.text("§c⚡ El marco del portal se materializa bloque a bloque... §c⚡").color(NamedTextColor.RED));
                            construirMarcoPortalGradual(world, baseX, baseY, baseZ);
                            efectoEspiralAscendente(centro, 50);
                            break;
                            
                        case 7:
                            // FASE 8: ACTIVACIÓN (24-30 seg)
                            Bukkit.getServer().broadcast(Component.text("§5§l⚡ ⚡ ⚡ LOS OJOS DEL END SE ACTIVAN UNO POR UNO... ⚡ ⚡ ⚡").color(NamedTextColor.DARK_PURPLE));
                            activarPortalProgresivo(world, baseX, baseY, baseZ);
                            efectoExplosionEpica(centro);
                            break;
                            
                        case 8:
                            // FINALIZACIÓN
                            Bukkit.getServer().broadcast(Component.text("§d§l⚡ ⚡ ⚡ PORTAL DEL END COMPLETAMENTE ACTIVADO ⚡ ⚡ ⚡").color(NamedTextColor.LIGHT_PURPLE));
                            generarVegetacionCorrupta(world, baseX, baseY, baseZ);
                            generarEscombrosFlotantes(world, baseX, baseY, baseZ);
                            efectoAuraMasiva(centro);
                            plugin.getLogger().info("[Apertura End] ✓✓✓ PORTAL ÉPICO COMPLETADO ✓✓✓");
                            cancel();
                            return;
                    }
                    
                    fase++;
                    
                } catch (Exception e) {
                    plugin.getLogger().severe("[Apertura End] Error en construcción: " + e.getMessage());
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 60L); // Cada 3 segundos
    }
    
    /**
     * Terraforma el área alrededor del portal para integración natural
     */
    private void terraformarAreaPortal(World world, int baseX, int baseY, int baseZ) {
        int radius = 25;
        
        // ═══════════════════════════════════════════════════════════════
        // PASO 1: LIMPIEZA TOTAL DEL ÁREA (REMOVER ÁRBOLES Y VEGETACIÓN)
        // ═══════════════════════════════════════════════════════════════
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distancia = Math.sqrt(x * x + z * z);
                if (distancia > radius) continue;
                
                // Limpiar desde el suelo hasta bien arriba (remover TODO)
                for (int y = baseY - 5; y < baseY + 30; y++) {
                    Block block = world.getBlockAt(baseX + x, y, baseZ + z);
                    Material type = block.getType();
                    
                    // Remover completamente árboles, hojas, plantas, etc.
                    if (type.toString().contains("LEAVES") || 
                        type.toString().contains("LOG") ||
                        type.toString().contains("WOOD") ||
                        type == Material.SHORT_GRASS ||
                        type == Material.TALL_GRASS ||
                        type == Material.FERN ||
                        type == Material.LARGE_FERN ||
                        type.toString().contains("FLOWER") ||
                        type.toString().contains("SAPLING") ||
                        type == Material.VINE ||
                        type == Material.BAMBOO ||
                        type.toString().contains("MUSHROOM")) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
        
        // ═══════════════════════════════════════════════════════════════
        // PASO 2: NIVELAR Y TERRAFORMAR EL TERRENO
        // ═══════════════════════════════════════════════════════════════
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distancia = Math.sqrt(x * x + z * z);
                if (distancia > radius) continue;
                
                // Calcular intensidad basada en distancia
                double intensidad = 1.0 - (distancia / radius);
                
                // No modificar área inmediata del portal (radio 8)
                if (distancia < 8) continue;
                
                // Crear depresión suave hacia el portal
                int depresion = (int)((1.0 - intensidad) * 2);
                
                // Aplicar terraformación desde el suelo base
                for (int y = baseY - depresion - 3; y < baseY + 2; y++) {
                    Block block = world.getBlockAt(baseX + x, y, baseZ + z);
                    Material type = block.getType();
                    
                    // Reemplazar tierra/piedra con materiales corruptos
                    if (type == Material.GRASS_BLOCK || 
                        type == Material.DIRT ||
                        type == Material.STONE ||
                        type == Material.DEEPSLATE ||
                        type == Material.SAND ||
                        type == Material.GRAVEL) {
                        
                        // Gradiente de corrupción según intensidad
                        if (intensidad > 0.7 && Math.random() < 0.7) {
                            block.setType(Material.END_STONE);
                        } else if (intensidad > 0.5 && Math.random() < 0.6) {
                            block.setType(Material.NETHERRACK);
                        } else if (intensidad > 0.3 && Math.random() < 0.4) {
                            block.setType(Material.END_STONE);
                        }
                    }
                    
                    // Parches de obsidiana (como si se hubiera quemado)
                    if (intensidad > 0.6 && Math.random() < 0.12) {
                        block.setType(Material.OBSIDIAN);
                    }
                }
            }
        }
    }
    
    /**
     * Genera grietas dimensionales que irradian desde el portal
     */
    private void generarGrietasDimensionales(World world, int baseX, int baseY, int baseZ) {
        // Crear 8 grietas radiales desde el centro
        for (int i = 0; i < 8; i++) {
            double angulo = (i * Math.PI * 2) / 8;
            
            // Cada grieta se extiende entre 15-25 bloques
            int longitud = 15 + random.nextInt(11);
            
            for (int dist = 8; dist < longitud; dist++) {
                int offsetX = (int)(Math.cos(angulo) * dist);
                int offsetZ = (int)(Math.sin(angulo) * dist);
                
                // Ancho de la grieta (se reduce con la distancia)
                int ancho = Math.max(1, 3 - (dist / 8));
                
                for (int w = -ancho; w <= ancho; w++) {
                    // Perpendicular a la dirección de la grieta
                    int perpX = (int)(-Math.sin(angulo) * w);
                    int perpZ = (int)(Math.cos(angulo) * w);
                    
                    // Profundidad de la grieta (irregular)
                    int profundidad = 2 + random.nextInt(3);
                    
                    for (int y = 0; y < profundidad; y++) {
                        Block block = world.getBlockAt(baseX + offsetX + perpX, baseY - y - 1, baseZ + offsetZ + perpZ);
                        
                        // Centro de la grieta: vacío
                        if (Math.abs(w) < ancho) {
                            block.setType(Material.AIR);
                        }
                        
                        // Bordes de la grieta: End Stone agrietado
                        if (Math.abs(w) == ancho || y == profundidad - 1) {
                            if (Math.random() < 0.7) {
                                block.setType(Material.END_STONE);
                            }
                        }
                        
                        // Fondo de grietas profundas: Crying Obsidian (infranqueable)
                        if (y == profundidad - 1 && profundidad > 2) {
                            block.setType(Material.CRYING_OBSIDIAN);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Construye la plataforma principal con degradado natural
     */
    private void construirPlataformaPortal(World world, int baseX, int baseY, int baseZ) {
        int radius = 10;
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distancia = Math.sqrt(x * x + z * z);
                
                if (distancia > radius) continue;
                
                // Capa base sólida
                Block baseBlock = world.getBlockAt(baseX + x, baseY - 1, baseZ + z);
                
                // Gradiente desde el centro
                if (distancia < 5) {
                    baseBlock.setType(Material.END_STONE_BRICKS);
                } else if (distancia < 7) {
                    baseBlock.setType(Material.CHISELED_POLISHED_BLACKSTONE);
                } else if (distancia < 9) {
                    baseBlock.setType(Material.END_STONE);
                } else {
                    // Borde irregular
                    if (Math.random() < 0.7) {
                        baseBlock.setType(Material.NETHERRACK);
                    }
                }
                
                // Detalles decorativos en la superficie
                if (distancia >= 6 && distancia <= 8 && Math.random() < 0.2) {
                    world.getBlockAt(baseX + x, baseY, baseZ + z).setType(Material.SOUL_SAND);
                }
                
                // Bordes con Purpur
                if (Math.abs(x) == radius || Math.abs(z) == radius) {
                    if (Math.random() < 0.5) {
                        world.getBlockAt(baseX + x, baseY, baseZ + z).setType(Material.PURPUR_BLOCK);
                    }
                }
            }
        }
        
        // Esquinas elevadas dramáticas
        int[] corners = {-7, 7};
        for (int xOff : corners) {
            for (int zOff : corners) {
                world.getBlockAt(baseX + xOff, baseY, baseZ + zOff).setType(Material.PURPUR_PILLAR);
                world.getBlockAt(baseX + xOff, baseY + 1, baseZ + zOff).setType(Material.PURPUR_PILLAR);
                world.getBlockAt(baseX + xOff, baseY + 2, baseZ + zOff).setType(Material.PURPUR_BLOCK);
            }
        }
    }
    
    /**
     * Construye el marco del portal COMPLETO (activado, no fragmentado como en Evento 4)
     * El portal del Evento 5 debe estar listo para ser atravesado
     */
    private void construirMarcoPortalCompleto(World world, int baseX, int baseY, int baseZ) {
        // Marco COMPLETO de 5x5 con END_PORTAL_FRAME (todos los lados completos)
        
        // Lado NORTE (5 bloques completos)
        for (int i = -2; i <= 2; i++) {
            Block frame = world.getBlockAt(baseX + i, baseY, baseZ - 4);
            frame.setType(Material.END_PORTAL_FRAME);
            // Activar el frame con ojo
            org.bukkit.block.data.BlockData data = frame.getBlockData();
            if (data instanceof org.bukkit.block.data.type.EndPortalFrame) {
                ((org.bukkit.block.data.type.EndPortalFrame) data).setEye(true);
                frame.setBlockData(data);
            }
        }
        
        // Lado ESTE (5 bloques completos)
        for (int i = -2; i <= 2; i++) {
            Block frame = world.getBlockAt(baseX + 4, baseY, baseZ + i);
            frame.setType(Material.END_PORTAL_FRAME);
            org.bukkit.block.data.BlockData data = frame.getBlockData();
            if (data instanceof org.bukkit.block.data.type.EndPortalFrame) {
                ((org.bukkit.block.data.type.EndPortalFrame) data).setEye(true);
                frame.setBlockData(data);
            }
        }
        
        // Lado SUR (5 bloques completos)
        for (int i = -2; i <= 2; i++) {
            Block frame = world.getBlockAt(baseX + i, baseY, baseZ + 4);
            frame.setType(Material.END_PORTAL_FRAME);
            org.bukkit.block.data.BlockData data = frame.getBlockData();
            if (data instanceof org.bukkit.block.data.type.EndPortalFrame) {
                ((org.bukkit.block.data.type.EndPortalFrame) data).setEye(true);
                frame.setBlockData(data);
            }
        }
        
        // Lado OESTE (5 bloques completos)
        for (int i = -2; i <= 2; i++) {
            Block frame = world.getBlockAt(baseX - 4, baseY, baseZ + i);
            frame.setType(Material.END_PORTAL_FRAME);
            org.bukkit.block.data.BlockData data = frame.getBlockData();
            if (data instanceof org.bukkit.block.data.type.EndPortalFrame) {
                ((org.bukkit.block.data.type.EndPortalFrame) data).setEye(true);
                frame.setBlockData(data);
            }
        }
        
        // ACTIVAR EL PORTAL (llenar interior con END_PORTAL)
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                // Solo el interior 3x3
                if (Math.abs(x) >= 2 || Math.abs(z) >= 2) continue;
                world.getBlockAt(baseX + x, baseY, baseZ + z).setType(Material.END_PORTAL);
            }
        }
    }
    
    /**
     * Construye pilares monumentales alrededor del portal
     */
    private void construirPilaresMonumentales(World world, int baseX, int baseY, int baseZ) {
        // 4 pilares principales en esquinas (altura variable 8-15 bloques)
        construirPilarMonumental(world, baseX - 6, baseY, baseZ - 6, 12);
        construirPilarMonumental(world, baseX + 6, baseY, baseZ - 6, 10);
        construirPilarMonumental(world, baseX - 6, baseY, baseZ + 6, 9);
        construirPilarMonumental(world, baseX + 6, baseY, baseZ + 6, 14);
        
        // 4 pilares secundarios (más pequeños)
        construirPilarMonumental(world, baseX - 8, baseY, baseZ, 7);
        construirPilarMonumental(world, baseX + 8, baseY, baseZ, 6);
        construirPilarMonumental(world, baseX, baseY, baseZ - 8, 8);
        construirPilarMonumental(world, baseX, baseY, baseZ + 8, 5);
    }
    
    /**
     * Construye un pilar monumental con detalles
     */
    private void construirPilarMonumental(World world, int x, int y, int z, int altura) {
        // Base del pilar (3x3)
        for (int xOff = -1; xOff <= 1; xOff++) {
            for (int zOff = -1; zOff <= 1; zOff++) {
                world.getBlockAt(x + xOff, y - 1, z + zOff).setType(Material.POLISHED_BLACKSTONE);
                
                // Centro de la base
                if (xOff == 0 && zOff == 0) {
                    world.getBlockAt(x, y, z).setType(Material.CHISELED_POLISHED_BLACKSTONE);
                }
            }
        }
        
        // Cuerpo principal del pilar
        for (int i = 1; i <= altura; i++) {
            Material mat;
            
            // Patrón alternado con daño simulado
            if (i % 3 == 0) {
                mat = Material.PURPUR_PILLAR;
            } else if (i % 3 == 1) {
                mat = Material.END_STONE_BRICKS;
            } else {
                mat = Material.POLISHED_BLACKSTONE_BRICKS;
            }
            
            // Algunos bloques "rotos" (huecos)
            if (altura > 8 && i == altura - 3 && Math.random() < 0.4) {
                mat = Material.AIR;
            }
            
            world.getBlockAt(x, y + i, z).setType(mat);
            
            // Decoración lateral ocasional
            if (i % 4 == 0 && i < altura - 2) {
                int lado = random.nextInt(4);
                int[] offsets = {-1, 1, 0, 0};
                int xOffset = (lado < 2) ? offsets[lado] : 0;
                int zOffset = (lado >= 2) ? offsets[lado] : 0;
                
                world.getBlockAt(x + xOffset, y + i, z + zOffset).setType(Material.END_STONE);
            }
        }
        
        // Corona del pilar
        if (altura >= 6) {
            for (int xOff = -1; xOff <= 1; xOff++) {
                for (int zOff = -1; zOff <= 1; zOff++) {
                    if (Math.abs(xOff) == 1 || Math.abs(zOff) == 1) {
                        world.getBlockAt(x + xOff, y + altura, z + zOff).setType(Material.PURPUR_SLAB);
                    }
                }
            }
            world.getBlockAt(x, y + altura, z).setType(Material.END_ROD);
        }
    }
    
    /**
     * Genera escombros flotantes dramáticos
     */
    private void generarEscombrosFlotantes(World world, int baseX, int baseY, int baseZ) {
        // 20-30 bloques flotantes en posiciones aleatorias
        int cantidadEscombros = 25 + random.nextInt(6);
        
        for (int i = 0; i < cantidadEscombros; i++) {
            // Posición aleatoria alrededor del portal
            int offsetX = -10 + random.nextInt(21);
            int offsetZ = -10 + random.nextInt(21);
            int offsetY = 2 + random.nextInt(8); // Entre 2-10 bloques de altura
            
            // No colocar muy cerca del centro
            if (Math.abs(offsetX) < 5 && Math.abs(offsetZ) < 5) continue;
            
            Location escombro = new Location(world, baseX + offsetX, baseY + offsetY, baseZ + offsetZ);
            
            // Materiales variados del End
            Material[] materiales = {
                Material.END_STONE,
                Material.END_STONE_BRICKS,
                Material.PURPUR_BLOCK,
                Material.OBSIDIAN,
                Material.CRYING_OBSIDIAN,
                Material.NETHERRACK
            };
            
            Material mat = materiales[random.nextInt(materiales.length)];
            escombro.getBlock().setType(mat);
            
            // Algunos escombros son grupos de 2-3 bloques
            if (random.nextBoolean()) {
                escombro.clone().add(1, 0, 0).getBlock().setType(mat);
            }
            if (random.nextInt(100) < 30) {
                escombro.clone().add(0, 1, 0).getBlock().setType(mat);
            }
        }
    }
    
    /**
     * Genera vegetación corrupta y cristales
     */
    private void generarVegetacionCorrupta(World world, int baseX, int baseY, int baseZ) {
        int radius = 15;
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distancia = Math.sqrt(x * x + z * z);
                
                // Solo en el borde exterior
                if (distancia < 8 || distancia > radius) continue;
                
                if (Math.random() < 0.15) {
                    Location loc = new Location(world, baseX + x, baseY, baseZ + z);
                    Block below = loc.clone().subtract(0, 1, 0).getBlock();
                    
                    // Solo si hay suelo debajo
                    if (!below.getType().isSolid()) continue;
                    
                    // Flores del End (Chorus)
                    if (Math.random() < 0.3) {
                        loc.getBlock().setType(Material.CHORUS_PLANT);
                        loc.clone().add(0, 1, 0).getBlock().setType(Material.CHORUS_FLOWER);
                    }
                    // Hongos
                    else if (Math.random() < 0.5) {
                        loc.getBlock().setType(Math.random() < 0.5 ? Material.BROWN_MUSHROOM : Material.RED_MUSHROOM);
                    }
                    // Fuego del alma
                    else if (Math.random() < 0.3) {
                        below.setType(Material.SOUL_SOIL);
                        loc.getBlock().setType(Material.SOUL_FIRE);
                    }
                    // Cristales de amatista (decorativos)
                    else {
                        loc.getBlock().setType(Material.AMETHYST_CLUSTER);
                    }
                }
            }
        }
        
        // Grupos de cristales grandes cerca de los pilares
        int[] pillarPositions = {-6, 6};
        for (int xOff : pillarPositions) {
            for (int zOff : pillarPositions) {
                // Cluster de cristales alrededor de cada pilar
                for (int i = 0; i < 5; i++) {
                    int randX = xOff + random.nextInt(3) - 1;
                    int randZ = zOff + random.nextInt(3) - 1;
                    
                    Location crystalLoc = new Location(world, baseX + randX, baseY, baseZ + randZ);
                    if (crystalLoc.getBlock().getType() == Material.AIR) {
                        crystalLoc.getBlock().setType(Material.AMETHYST_CLUSTER);
                    }
                }
            }
        }
    }
    
    /**
     * Inicia efectos visuales permanentes del portal ÉPICO
     * Se activa cuando el portal se abre (Fase 2: LLEGADA)
     */
    private void iniciarEfectosPortalEpico(Location centro) {
        efectosPortalTask = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticksEfectos = 0;
            
            @Override
            public void run() {
                World world = centro.getWorld();
                if (world == null || faseEvento == EventPhase.COMBATE || faseEvento == EventPhase.VICTORIA || faseEvento == EventPhase.CLIFFHANGER) {
                    // Detener efectos cuando los jugadores entren al End
                    if (efectosPortalTask != null) {
                        efectosPortalTask.cancel();
                    }
                    return;
                }
                
                ticksEfectos++;
                
                // ════════════════════════════════════════════════════════════
                // PARTÍCULAS PORTAL MASIVAS EN EL CENTRO
                // ════════════════════════════════════════════════════════════
                world.spawnParticle(Particle.PORTAL, centro, 50, 3.5, 1.0, 3.5, 0.08);
                world.spawnParticle(Particle.REVERSE_PORTAL, centro, 30, 3.0, 0.8, 3.0, 0.05);
                
                // ════════════════════════════════════════════════════════════
                // ESPIRAL ASCENDENTE ÉPICA (END_ROD)
                // ════════════════════════════════════════════════════════════
                double radioEspiral = 4.5;
                for (int i = 0; i < 20; i++) {
                    double angulo = (ticksEfectos + i * 18) * 0.05;
                    double offsetX = radioEspiral * Math.cos(angulo);
                    double offsetZ = radioEspiral * Math.sin(angulo);
                    double offsetY = ((ticksEfectos + i * 10) % 140) * 0.12;
                    
                    Location particleLoc = centro.clone().add(offsetX, offsetY, offsetZ);
                    world.spawnParticle(Particle.END_ROD, particleLoc, 3, 0.1, 0.1, 0.1, 0);
                }
                
                // ════════════════════════════════════════════════════════════
                // ANILLO DE DRAGÓN GIRATORIO
                // ════════════════════════════════════════════════════════════
                double radioAnillo = 5.5;
                int puntosAnillo = 40;
                for (int i = 0; i < puntosAnillo; i++) {
                    double angulo = (ticksEfectos * 0.03) + (i * 2 * Math.PI / puntosAnillo);
                    double offsetX = radioAnillo * Math.cos(angulo);
                    double offsetZ = radioAnillo * Math.sin(angulo);
                    
                    Location anilloLoc = centro.clone().add(offsetX, 0.3, offsetZ);
                    world.spawnParticle(Particle.DRAGON_BREATH, anilloLoc, 1, 0, 0, 0, 0);
                }
                
                // ════════════════════════════════════════════════════════════
                // RAYOS VERTICALES EN ESQUINAS
                // ════════════════════════════════════════════════════════════
                if (ticksEfectos % 10 == 0) {
                    for (int offset = -4; offset <= 4; offset += 8) {
                        for (int offsetZ = -4; offsetZ <= 4; offsetZ += 8) {
                            for (double y = 0; y < 8; y += 0.5) {
                                Location rayoLoc = centro.clone().add(offset, y, offsetZ);
                                world.spawnParticle(Particle.ENCHANT, rayoLoc, 1, 0.1, 0.1, 0.1, 0);
                            }
                        }
                    }
                }
                
                // ════════════════════════════════════════════════════════════
                // PULSOS DE ENERGÍA OCASIONALES
                // ════════════════════════════════════════════════════════════
                if (ticksEfectos % 100 == 0) {
                    world.spawnParticle(Particle.EXPLOSION, centro, 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.SOUL, centro, 60, 4.5, 2.5, 4.5, 0.12);
                    
                    // Sonido épico
                    world.playSound(centro, Sound.BLOCK_END_PORTAL_SPAWN, 2.0f, 0.6f);
                }
                
                // Sonido ambiente
                if (ticksEfectos % 60 == 0) {
                    world.playSound(centro, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.5f);
                }
            }
        }, 0L, 2L);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // MÉTODOS DE CONSTRUCCIÓN ÉPICA MEJORADOS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Plataforma MASIVA de 25x25 con anillos concéntricos de materiales
     */
    private void construirPlataformaMasiva(World world, int baseX, int baseY, int baseZ) {
        int radius = 15;
        
        // 3 CAPAS DE PROFUNDIDAD SÓLIDA
        for (int layer = -2; layer <= 0; layer++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt(x * x + z * z);
                    if (dist > radius) continue;
                    
                    Block b = world.getBlockAt(baseX + x, baseY + layer, baseZ + z);
                    
                    // 6 ANILLOS CONCÉNTRICOS
                    if (dist < 3) {
                        b.setType(Material.CRYING_OBSIDIAN);
                    } else if (dist < 5) {
                        b.setType(Material.ANCIENT_DEBRIS);
                    } else if (dist < 7) {
                        b.setType(Material.END_STONE_BRICKS);
                    } else if (dist < 9) {
                        b.setType(Material.POLISHED_BLACKSTONE_BRICKS);
                    } else if (dist < 12) {
                        b.setType(Material.PURPUR_BLOCK);
                    } else {
                        b.setType(Material.END_STONE);
                    }
                }
            }
        }
        
        // RUNAS LUMINOSAS EN CÍRCULOS
        for (double angle = 0; angle < 360; angle += 12) {
            double rad = Math.toRadians(angle);
            for (int r = 5; r <= 13; r += 4) {
                int x = (int) (Math.cos(rad) * r);
                int z = (int) (Math.sin(rad) * r);
                world.getBlockAt(baseX + x, baseY + 1, baseZ + z).setType(Material.GLOWSTONE);
                world.getBlockAt(baseX + x + 1, baseY + 1, baseZ + z).setType(Material.REDSTONE_BLOCK);
            }
        }
        
        // TORRES EN ESQUINAS (5 bloques altura)
        int[] corners = {-11, 11};
        for (int xc : corners) {
            for (int zc : corners) {
                for (int y = 0; y < 6; y++) {
                    Material mat = (y == 5) ? Material.SEA_LANTERN : Material.PURPUR_PILLAR;
                    world.getBlockAt(baseX + xc, baseY + y, baseZ + zc).setType(mat);
                }
            }
        }
    }
    
    /**
     * Anillos concéntricos flotantes de energía
     */
    private void construirAnillosConcentricos(World world, int baseX, int baseY, int baseZ) {
        for (int ring = 1; ring <= 4; ring++) {
            int radius = 7 + (ring * 3);
            int altura = baseY + (ring * 2);
            
            for (double angle = 0; angle < 360; angle += 8) {
                double rad = Math.toRadians(angle);
                int x = (int) (Math.cos(rad) * radius);
                int z = (int) (Math.sin(rad) * radius);
                
                Material mat = switch (ring) {
                    case 1 -> Material.PURPLE_STAINED_GLASS;
                    case 2 -> Material.MAGENTA_STAINED_GLASS;
                    case 3 -> Material.PINK_STAINED_GLASS;
                    default -> Material.WHITE_STAINED_GLASS;
                };
                
                world.getBlockAt(baseX + x, altura, baseZ + z).setType(mat);
            }
        }
    }
    
    /**
     * 8 TORRES MONUMENTALES de 25 bloques con arquitectura épica
     */
    private void construirTorresMonumentales(World world, int baseX, int baseY, int baseZ) {
        // 4 TORRES PRINCIPALES (25 bloques)
        construirTorreMonumental(world, baseX - 13, baseY, baseZ - 13, 25, true);
        construirTorreMonumental(world, baseX + 13, baseY, baseZ - 13, 24, true);
        construirTorreMonumental(world, baseX - 13, baseY, baseZ + 13, 23, true);
        construirTorreMonumental(world, baseX + 13, baseY, baseZ + 13, 26, true);
        
        // 4 TORRES SECUNDARIAS (18 bloques)
        construirTorreMonumental(world, baseX - 17, baseY, baseZ, 18, false);
        construirTorreMonumental(world, baseX + 17, baseY, baseZ, 17, false);
        construirTorreMonumental(world, baseX, baseY, baseZ - 17, 19, false);
        construirTorreMonumental(world, baseX, baseY, baseZ + 17, 16, false);
    }
    
    /**
     * Construye una torre épica con detalles arquitectónicos
     */
    private void construirTorreMonumental(World world, int x, int y, int z, int altura, boolean principal) {
        int baseSize = principal ? 5 : 3;
        
        // CIMIENTOS (5x5 o 3x3)
        for (int xOff = -baseSize/2; xOff <= baseSize/2; xOff++) {
            for (int zOff = -baseSize/2; zOff <= baseSize/2; zOff++) {
                world.getBlockAt(x + xOff, y - 2, z + zOff).setType(Material.REINFORCED_DEEPSLATE);
                world.getBlockAt(x + xOff, y - 1, z + zOff).setType(Material.POLISHED_BLACKSTONE_BRICKS);
            }
        }
        
        // CUERPO CON PATRÓN
        for (int i = 0; i < altura; i++) {
            Material mat;
            int seccion = i / 5;
            
            if (seccion % 3 == 0) {
                mat = Material.PURPUR_PILLAR;
            } else if (seccion % 3 == 1) {
                mat = Material.END_STONE_BRICKS;
            } else {
                mat = Material.POLISHED_BLACKSTONE_BRICKS;
            }
            
            world.getBlockAt(x, y + i, z).setType(mat);
            
            // Detalles cada 5 bloques
            if (i % 5 == 0 && i > 0) {
                for (int xOff = -1; xOff <= 1; xOff++) {
                    for (int zOff = -1; zOff <= 1; zOff++) {
                        if (xOff == 0 && zOff == 0) continue;
                        world.getBlockAt(x + xOff, y + i, z + zOff).setType(Material.CHISELED_POLISHED_BLACKSTONE);
                    }
                }
            }
        }
        
        // CIMA LUMINOSA
        world.getBlockAt(x, y + altura, z).setType(Material.SEA_LANTERN);
        for (int xOff = -1; xOff <= 1; xOff++) {
            for (int zOff = -1; zOff <= 1; zOff++) {
                if (xOff == 0 && zOff == 0) continue;
                world.getBlockAt(x + xOff, y + altura, z + zOff).setType(Material.PURPUR_BLOCK);
            }
        }
    }
    
    /**
     * Cristales gigantes de 10-15 bloques de altura
     */
    private void generarCristalesGigantes(World world, int baseX, int baseY, int baseZ) {
        int[][] posiciones = {
            {-9, -9}, {9, -9}, {-9, 9}, {9, 9},
            {-14, 0}, {14, 0}, {0, -14}, {0, 14}
        };
        
        for (int[] pos : posiciones) {
            int altura = 10 + (int)(Math.random() * 6);
            
            for (int y = 0; y < altura; y++) {
                Material mat;
                if (y < altura / 3) {
                    mat = Material.AMETHYST_BLOCK;
                } else if (y < altura * 2 / 3) {
                    mat = Material.BUDDING_AMETHYST;
                } else {
                    mat = Material.PURPLE_STAINED_GLASS;
                }
                
                world.getBlockAt(baseX + pos[0], baseY + y, baseZ + pos[1]).setType(mat);
                
                // Ramificaciones cada 3 bloques
                if (y % 3 == 0 && y > 0) {
                    world.getBlockAt(baseX + pos[0] + 1, baseY + y, baseZ + pos[1]).setType(Material.SMALL_AMETHYST_BUD);
                    world.getBlockAt(baseX + pos[0] - 1, baseY + y, baseZ + pos[1]).setType(Material.MEDIUM_AMETHYST_BUD);
                    world.getBlockAt(baseX + pos[0], baseY + y, baseZ + pos[1] + 1).setType(Material.LARGE_AMETHYST_BUD);
                }
            }
            
            // Cima luminosa
            world.getBlockAt(baseX + pos[0], baseY + altura, baseZ + pos[1]).setType(Material.SEA_LANTERN);
        }
    }
    
    /**
     * Construye el marco del portal GRADUALMENTE bloque por bloque
     */
    private void construirMarcoPortalGradual(World world, int baseX, int baseY, int baseZ) {
        // NORTE
        for (int i = -2; i <= 2; i++) {
            Block frame = world.getBlockAt(baseX + i, baseY, baseZ - 4);
            frame.setType(Material.END_PORTAL_FRAME);
        }
        
        // ESTE
        for (int i = -2; i <= 2; i++) {
            Block frame = world.getBlockAt(baseX + 4, baseY, baseZ + i);
            frame.setType(Material.END_PORTAL_FRAME);
        }
        
        // SUR
        for (int i = -2; i <= 2; i++) {
            Block frame = world.getBlockAt(baseX + i, baseY, baseZ + 4);
            frame.setType(Material.END_PORTAL_FRAME);
        }
        
        // OESTE
        for (int i = -2; i <= 2; i++) {
            Block frame = world.getBlockAt(baseX - 4, baseY, baseZ + i);
            frame.setType(Material.END_PORTAL_FRAME);
        }
    }
    
    /**
     * Activa el portal PROGRESIVAMENTE ojo por ojo
     */
    private void activarPortalProgresivo(World world, int baseX, int baseY, int baseZ) {
        // ACTIVAR OJOS UNO POR UNO (20 frames)
        Block[] frames = new Block[20];
        int idx = 0;
        
        for (int i = -2; i <= 2; i++) {
            frames[idx++] = world.getBlockAt(baseX + i, baseY, baseZ - 4);
            frames[idx++] = world.getBlockAt(baseX + 4, baseY, baseZ + i);
            frames[idx++] = world.getBlockAt(baseX + i, baseY, baseZ + 4);
            frames[idx++] = world.getBlockAt(baseX - 4, baseY, baseZ + i);
        }
        
        // Activar todos los ojos
        for (Block frame : frames) {
            if (frame != null && frame.getType() == Material.END_PORTAL_FRAME) {
                org.bukkit.block.data.BlockData data = frame.getBlockData();
                if (data instanceof org.bukkit.block.data.type.EndPortalFrame frameData) {
                    frameData.setEye(true);
                    frame.setBlockData(frameData);
                }
            }
        }
        
        // SPAWN PORTAL BLOCKS (interior 3x3)
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (Math.abs(x) >= 2 || Math.abs(z) >= 2) continue;
                world.getBlockAt(baseX + x, baseY, baseZ + z).setType(Material.END_PORTAL);
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // EFECTOS VISUALES ÉPICOS
    // ═══════════════════════════════════════════════════════════════════
    
    private void efectoExplosionMasiva(Location centro, int radius) {
        World world = centro.getWorld();
        if (world == null) return;
        
        for (int i = 0; i < 12; i++) {
            double angle = Math.random() * Math.PI * 2;
            double dist = Math.random() * radius;
            double x = Math.cos(angle) * dist;
            double z = Math.sin(angle) * dist;
            
            Location loc = centro.clone().add(x, 2, z);
            world.spawnParticle(Particle.EXPLOSION, loc, 3);
            world.spawnParticle(Particle.LAVA, loc, 15, 2, 1, 2);
        }
        
        world.playSound(centro, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 3.0f, 0.5f);
        world.playSound(centro, Sound.ENTITY_GENERIC_EXPLODE, 2.5f, 0.6f);
    }
    
    private void efectoRayosEnergia(Location centro, int cantidad) {
        World world = centro.getWorld();
        if (world == null) return;
        
        for (int i = 0; i < cantidad; i++) {
            double angle = (i * 18) * Math.PI / 180;
            double dist = 15 + Math.random() * 10;
            double x = Math.cos(angle) * dist;
            double z = Math.sin(angle) * dist;
            
            Location start = centro.clone().add(x, 1, z);
            Location end = centro.clone().add(0, 15, 0);
            
            for (double t = 0; t <= 1; t += 0.1) {
                Location point = start.clone().add(
                    (end.getX() - start.getX()) * t,
                    (end.getY() - start.getY()) * t,
                    (end.getZ() - start.getZ()) * t
                );
                world.spawnParticle(Particle.ELECTRIC_SPARK, point, 2);
            }
        }
        
        world.playSound(centro, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.8f);
    }
    
    private void efectoOndasExpansivas(Location centro, int ondas) {
        World world = centro.getWorld();
        if (world == null) return;
        
        for (int onda = 0; onda < ondas; onda++) {
            double radius = 5 + (onda * 4);
            for (double angle = 0; angle < 360; angle += 5) {
                double rad = Math.toRadians(angle);
                double x = Math.cos(rad) * radius;
                double z = Math.sin(rad) * radius;
                
                Location loc = centro.clone().add(x, 0.5, z);
                world.spawnParticle(Particle.SONIC_BOOM, loc, 1);
                world.spawnParticle(Particle.ENCHANT, loc, 5, 0.2, 0.2, 0.2);
            }
        }
        
        world.playSound(centro, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.8f);
    }
    
    private void efectoLluviaCristales(Location centro, int cantidad) {
        World world = centro.getWorld();
        if (world == null) return;
        
        for (int i = 0; i < cantidad; i++) {
            double x = (Math.random() - 0.5) * 30;
            double z = (Math.random() - 0.5) * 30;
            double y = 10 + Math.random() * 15;
            
            Location loc = centro.clone().add(x, y, z);
            world.spawnParticle(Particle.END_ROD, loc, 3, 0.1, 0.5, 0.1, 0.02);
            world.spawnParticle(Particle.GLOW, loc, 5, 0.2, 0.2, 0.2);
        }
        
        world.playSound(centro, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 2.0f, 1.2f);
    }
    
    private void efectoErupcionEnergia(Location centro, int cantidad) {
        World world = centro.getWorld();
        if (world == null) return;
        
        for (int i = 0; i < cantidad; i++) {
            double angle = (i * 45) * Math.PI / 180;
            double dist = 10 + Math.random() * 8;
            double x = Math.cos(angle) * dist;
            double z = Math.sin(angle) * dist;
            
            Location base = centro.clone().add(x, 0, z);
            
            for (double y = 0; y < 20; y += 0.5) {
                Location loc = base.clone().add(0, y, 0);
                world.spawnParticle(Particle.FLAME, loc, 3, 0.3, 0.1, 0.3, 0.02);
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 2);
            }
        }
        
        world.playSound(centro, Sound.ITEM_FIRECHARGE_USE, 2.0f, 0.5f);
    }
    
    private void efectoLluviaParticulas(Location centro, int cantidad, Particle tipo) {
        World world = centro.getWorld();
        if (world == null) return;
        
        for (int i = 0; i < cantidad; i++) {
            double x = (Math.random() - 0.5) * 25;
            double z = (Math.random() - 0.5) * 25;
            double y = Math.random() * 20;
            
            Location loc = centro.clone().add(x, y, z);
            world.spawnParticle(tipo, loc, 5, 0.2, 0.2, 0.2);
        }
    }
    
    private void efectoEspiralAscendente(Location centro, int puntos) {
        World world = centro.getWorld();
        if (world == null) return;
        
        for (int i = 0; i < puntos; i++) {
            double angle = (i * 18) * Math.PI / 180;
            double radius = 5;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = i * 0.5;
            
            Location loc = centro.clone().add(x, y, z);
            world.spawnParticle(Particle.END_ROD, loc, 8, 0.1, 0.1, 0.1, 0.05);
            world.spawnParticle(Particle.ENCHANT, loc, 10, 0.2, 0.2, 0.2);
        }
        
        world.playSound(centro, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.5f);
    }
    
    private void efectoExplosionEpica(Location centro) {
        World world = centro.getWorld();
        if (world == null) return;
        
        world.spawnParticle(Particle.EXPLOSION_EMITTER, centro, 5);
        world.spawnParticle(Particle.FLASH, centro, 3);
        world.spawnParticle(Particle.END_ROD, centro, 200, 6, 3, 6, 0.2);
        world.spawnParticle(Particle.DRAGON_BREATH, centro, 150, 5, 2, 5, 0.15);
        world.spawnParticle(Particle.PORTAL, centro, 300, 8, 4, 8, 1);
        
        world.playSound(centro, Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.6f);
        world.playSound(centro, Sound.BLOCK_END_PORTAL_SPAWN, 3.0f, 1.0f);
        world.playSound(centro, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
    }
    
    private void efectoAuraMasiva(Location centro) {
        World world = centro.getWorld();
        if (world == null) return;
        
        for (int radio = 5; radio <= 30; radio += 5) {
            for (double angle = 0; angle < 360; angle += 3) {
                double rad = Math.toRadians(angle);
                double x = Math.cos(rad) * radio;
                double z = Math.sin(rad) * radio;
                
                Location loc = centro.clone().add(x, 1, z);
                world.spawnParticle(Particle.PORTAL, loc, 3, 0.1, 0.5, 0.1);
                world.spawnParticle(Particle.ENCHANT, loc, 5, 0.2, 0.2, 0.2);
            }
        }
        
        world.playSound(centro, Sound.BLOCK_BEACON_POWER_SELECT, 2.0f, 0.8f);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE AGRUPACIÓN PARA COORDENADAS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Mensaje inicial SIN coordenadas
     */
    private void mostrarMensajeInicialSinCoordenadas() {
        // Broadcast del mensaje inicial
        List<String> broadcast = config.getStringList("mensajes.fase_1_inicio.broadcast");
        for (String msg : broadcast) {
            Bukkit.broadcastMessage(msg);
        }
        
        // NO anunciar coordenadas todavía
        
        // Mensaje del Observador
        Bukkit.broadcastMessage("§8[§7...§8] §7No debería existir tan pronto.");
        
        // Sonidos
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1.5f, 0.5f);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.6f);
        }
    }
    
    /**
     * Verificar agrupación de jugadores para revelar dirección al portal
     * NUEVO: Ahora requiere completar tareas primero
     */
    private void verificarAgrupacionParaDireccion() {
        // Primero deben completar las tareas
        if (tareasCompletadas < tareasRequeridas) {
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§7El Observador siente algo...");
            Bukkit.broadcastMessage("§7Pero la ubicación aún es borrosa.");
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§8§o'Deben descubrir más pistas...'");
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§8§oTareas completadas: §e" + tareasCompletadas + "§8/§e" + tareasRequeridas);
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§e§lTareas pendientes:");
            if (!tareasRealizadas.contains("matar_enderman")) {
                Bukkit.broadcastMessage("§8  • §5Eliminar un Enderman §8- Emisarios del End");
            }
            if (!tareasRealizadas.contains("recolectar_obsidiana")) {
                Bukkit.broadcastMessage("§8  • §7Recolectar Obsidiana §8- Material del portal");
            }
            if (!tareasRealizadas.contains("conseguir_ojo_ender")) {
                Bukkit.broadcastMessage("§8  • §eConseguir Ojo de Ender §8- Clave dimensional");
            }
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
            iniciarTareasDesbloqueo();
            return;
        }
        
        if (!config.getBoolean("evento.portal.agrupacion_requerida.enabled", true)) {
            // Si no está activado, revelar dirección inmediatamente
            revelarDireccionPortal();
            direccionRevelada = true;
            return;
        }
        
        // Tarea que verifica cada 5 segundos si los jugadores están agrupados
        new BukkitRunnable() {
            private int intentos = 0;
            
            @Override
            public void run() {
                if (faseEvento != EventPhase.DESCUBRIMIENTO || direccionRevelada) {
                    cancel();
                    return;
                }
                
                // Cada 30 segundos (6 intentos) recordar que deben agruparse
                if (intentos % 6 == 0 && intentos > 0) {
                    String msgDispersos = config.getString("evento.portal.agrupacion_requerida.mensaje_dispersos", 
                        "§c⚠ Los jugadores están muy dispersos. Agrúpense para descubrir el camino al portal.");
                    Bukkit.broadcastMessage(msgDispersos);
                    
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    }
                }
                
                if (verificarJugadoresAgrupados()) {
                    // ¡Están agrupados!
                    String msgAgrupacion = config.getString("evento.portal.agrupacion_requerida.mensaje_agrupacion",
                        "§8[§7...§8] §7Deben estar juntos para descubrir el camino.");
                    Bukkit.broadcastMessage(msgAgrupacion);
                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            revelarDireccionPortal();
                            direccionRevelada = true;
                            
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 2.0f, 1.0f);
                                p.spawnParticle(Particle.PORTAL, p.getLocation().add(0, 1, 0), 100, 0.5, 0.5, 0.5, 0.5);
                            }
                        }
                    }.runTaskLater(plugin, 60L); // 3 segundos después
                    
                    cancel();
                }
                
                intentos++;
            }
        }.runTaskTimer(plugin, 100L, 100L); // Cada 5 segundos
    }
    
    /**
     * Verificar si los jugadores están agrupados
     */
    private boolean verificarJugadoresAgrupados() {
        List<Player> jugadores = new ArrayList<>(Bukkit.getOnlinePlayers());
        
        int jugadoresMinimos = config.getInt("evento.portal.agrupacion_requerida.jugadores_minimos", 2);
        if (jugadores.size() < jugadoresMinimos) {
            return true; // Si hay menos jugadores del mínimo, dar coordenadas directamente
        }
        
        double radioMaximo = config.getDouble("evento.portal.agrupacion_requerida.radio_maximo", 50.0);
        
        // Tomar el primer jugador como referencia
        Player referencia = jugadores.get(0);
        Location locReferencia = referencia.getLocation();
        
        // Verificar que todos los demás estén dentro del radio
        for (int i = 1; i < jugadores.size(); i++) {
            Player jugador = jugadores.get(i);
            
            // Verificar mismo mundo
            if (!jugador.getWorld().equals(referencia.getWorld())) {
                return false;
            }
            
            // Verificar distancia
            if (jugador.getLocation().distance(locReferencia) > radioMaximo) {
                return false;
            }
        }
        
        return true; // Todos están agrupados
    }
    
    /**
     * Revelar dirección al portal (sin coordenadas exactas)
     */
    private void revelarDireccionPortal() {
        if (portalLocation == null) return;
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§5§l⚡ §fEl camino se revela §5§l⚡");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7Sigan la flecha...");
        Bukkit.broadcastMessage("§8El portal está muy lejos.");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Iniciar sistema de brújula (actionbar con dirección)
        iniciarBrujulaPortal();
        
        plugin.getLogger().info("[Apertura End] Sistema de brújula activado - Los jugadores deben seguir la flecha");
    }
    
    /**
     * Iniciar sistema de brújula que muestra dirección en actionbar
     */
    private void iniciarBrujulaPortal() {
        brujulaTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (faseEvento != EventPhase.DESCUBRIMIENTO) {
                    cancel();
                    return;
                }
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (portalLocation == null) continue;
                    if (!player.getWorld().equals(portalLocation.getWorld())) continue;
                    
                    // Calcular dirección y distancia
                    Location playerLoc = player.getLocation();
                    double distancia = playerLoc.distance(portalLocation);
                    
                    // Calcular ángulo
                    double dx = portalLocation.getX() - playerLoc.getX();
                    double dz = portalLocation.getZ() - playerLoc.getZ();
                    double angulo = Math.toDegrees(Math.atan2(dz, dx)) - 90; // -90 para ajustar Norte
                    
                    // Normalizar ángulo
                    if (angulo < 0) angulo += 360;
                    
                    // Obtener yaw del jugador (dirección que mira)
                    float yaw = playerLoc.getYaw();
                    if (yaw < 0) yaw += 360;
                    
                    // Calcular ángulo relativo (hacia dónde debe ir desde donde mira)
                    double anguloRelativo = angulo - yaw;
                    if (anguloRelativo < 0) anguloRelativo += 360;
                    if (anguloRelativo > 180) anguloRelativo -= 360;
                    
                    // Obtener dirección cardinal y flecha
                    String direccion = obtenerDireccionCardinal(angulo);
                    String flecha = obtenerFlecha(anguloRelativo);
                    
                    // Formatear distancia
                    String distanciaStr;
                    if (distancia > 1000) {
                        distanciaStr = String.format("§c%.1f km", distancia / 1000.0);
                    } else if (distancia > 500) {
                        distanciaStr = String.format("§6%.0f bloques", distancia);
                    } else if (distancia > 100) {
                        distanciaStr = String.format("§e%.0f bloques", distancia);
                    } else {
                        distanciaStr = String.format("§a%.0f bloques", distancia);
                    }
                    
                    // Mostrar en actionbar
                    Component actionbar = Component.text(flecha + " ", NamedTextColor.LIGHT_PURPLE)
                        .append(Component.text(direccion + " ", NamedTextColor.WHITE))
                        .append(Component.text("⚡ ", NamedTextColor.DARK_PURPLE))
                        .append(Component.text(distanciaStr));
                    
                    player.sendActionBar(actionbar);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L); // Actualizar cada 0.5 segundos
    }
    
    /**
     * Obtener dirección cardinal basada en ángulo absoluto
     */
    private String obtenerDireccionCardinal(double angulo) {
        // Normalizar entre 0-360
        while (angulo < 0) angulo += 360;
        while (angulo >= 360) angulo -= 360;
        
        if (angulo >= 337.5 || angulo < 22.5) return "Norte";
        if (angulo >= 22.5 && angulo < 67.5) return "Noreste";
        if (angulo >= 67.5 && angulo < 112.5) return "Este";
        if (angulo >= 112.5 && angulo < 157.5) return "Sureste";
        if (angulo >= 157.5 && angulo < 202.5) return "Sur";
        if (angulo >= 202.5 && angulo < 247.5) return "Suroeste";
        if (angulo >= 247.5 && angulo < 292.5) return "Oeste";
        if (angulo >= 292.5 && angulo < 337.5) return "Noroeste";
        return "Norte";
    }
    
    /**
     * Obtener flecha Unicode basada en ángulo relativo
     */
    private String obtenerFlecha(double angulo) {
        // Normalizar entre -180 y 180
        while (angulo > 180) angulo -= 360;
        while (angulo < -180) angulo += 360;
        
        if (angulo >= -22.5 && angulo < 22.5) return "⬆";      // Adelante
        if (angulo >= 22.5 && angulo < 67.5) return "⬈";       // Adelante-Derecha
        if (angulo >= 67.5 && angulo < 112.5) return "➡";      // Derecha
        if (angulo >= 112.5 && angulo < 157.5) return "⬊";     // Atrás-Derecha
        if (angulo >= 157.5 || angulo < -157.5) return "⬇";    // Atrás
        if (angulo >= -157.5 && angulo < -112.5) return "⬋";   // Atrás-Izquierda
        if (angulo >= -112.5 && angulo < -67.5) return "⬅";    // Izquierda
        if (angulo >= -67.5 && angulo < -22.5) return "⬉";     // Adelante-Izquierda
        return "⬆";
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE SPAWNS DRAMÁTICOS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Iniciar spawns dramáticos durante el viaje al portal
     */
    private void iniciarSpawnsDramaticos() {
        if (!config.getBoolean("evento.spawns_dramaticos.enabled", true)) {
            return;
        }
        
        // Programar el primer spawn
        int intervaloMin = config.getInt("evento.spawns_dramaticos.intervalo_minimo", 120);
        int intervaloMax = config.getInt("evento.spawns_dramaticos.intervalo_maximo", 300);
        proximoSpawnTicks = (intervaloMin + random.nextInt(intervaloMax - intervaloMin + 1)) * 20;
        
        spawnsTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (faseEvento != EventPhase.DESCUBRIMIENTO) {
                    cancel();
                    return;
                }
                
                proximoSpawnTicks--;
                
                if (proximoSpawnTicks <= 0) {
                    // Ejecutar spawn dramático
                    ejecutarSpawnDramatico();
                    
                    // Programar siguiente spawn
                    int intervaloMin = config.getInt("evento.spawns_dramaticos.intervalo_minimo", 120);
                    int intervaloMax = config.getInt("evento.spawns_dramaticos.intervalo_maximo", 300);
                    proximoSpawnTicks = (intervaloMin + random.nextInt(intervaloMax - intervaloMin + 1)) * 20;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Cada tick
    }
    
    /**
     * Ejecutar un spawn dramático cerca de jugadores
     */
    private void ejecutarSpawnDramatico() {
        List<Player> jugadores = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (jugadores.isEmpty()) return;
        
        // Elegir un jugador aleatorio como punto de spawn
        Player objetivo = jugadores.get(random.nextInt(jugadores.size()));
        
        // Obtener lista de entidades configuradas
        var entidadesConfig = config.getConfigurationSection("evento.spawns_dramaticos.entidades");
        if (entidadesConfig == null) return;
        
        // Calcular probabilidad total
        int probabilidadTotal = 0;
        List<String> tiposEntidad = new ArrayList<>(entidadesConfig.getKeys(false));
        for (String tipo : tiposEntidad) {
            probabilidadTotal += config.getInt("evento.spawns_dramaticos.entidades." + tipo + ".probabilidad", 0);
        }
        
        // Elegir entidad basada en probabilidad
        int roll = random.nextInt(probabilidadTotal);
        int acumulado = 0;
        String tipoElegido = null;
        
        for (String tipo : tiposEntidad) {
            int prob = config.getInt("evento.spawns_dramaticos.entidades." + tipo + ".probabilidad", 0);
            acumulado += prob;
            if (roll < acumulado) {
                tipoElegido = tipo;
                break;
            }
        }
        
        if (tipoElegido == null) return;
        
        // Obtener configuración de la entidad elegida
        String path = "evento.spawns_dramaticos.entidades." + tipoElegido + ".";
        String tipoEntidad = config.getString(path + "tipo", "ENDERMAN");
        int cantidadMin = config.getInt(path + "cantidad_min", 2);
        int cantidadMax = config.getInt(path + "cantidad_max", 5);
        String mensaje = config.getString(path + "mensaje", "");
        String sonido = config.getString(path + "sonido", "");
        
        int cantidad = cantidadMin + random.nextInt(cantidadMax - cantidadMin + 1);
        
        // Spawn de entidades
        double radioSpawn = config.getDouble("evento.spawns_dramaticos.radio_spawn", 30.0);
        Location centro = objetivo.getLocation();
        
        for (int i = 0; i < cantidad; i++) {
            // Posición aleatoria alrededor del jugador
            double angulo = random.nextDouble() * 2 * Math.PI;
            double distancia = 15 + random.nextDouble() * (radioSpawn - 15); // Entre 15 y radioSpawn bloques
            
            double x = centro.getX() + Math.cos(angulo) * distancia;
            double z = centro.getZ() + Math.sin(angulo) * distancia;
            double y = centro.getWorld().getHighestBlockYAt((int) x, (int) z) + 1;
            
            Location spawnLoc = new Location(centro.getWorld(), x, y, z);
            
            try {
                EntityType entityType = EntityType.valueOf(tipoEntidad);
                centro.getWorld().spawnEntity(spawnLoc, entityType);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[Apertura End] Tipo de entidad inválido: " + tipoEntidad);
            }
        }
        
        // Mensaje y efectos
        if (!mensaje.isEmpty()) {
            Bukkit.broadcastMessage(mensaje);
        }
        
        // Efectos de aparición
        List<String> efectos = config.getStringList(path + "efectos_aparicion");
        for (String efecto : efectos) {
            String[] parts = efecto.split(":");
            if (parts.length >= 3) {
                try {
                    Particle particula = Particle.valueOf(parts[0]);
                    int cantidadParticulas = Integer.parseInt(parts[1]);
                    double radio = Double.parseDouble(parts[2]);
                    
                    centro.getWorld().spawnParticle(particula, centro.add(0, 1, 0), 
                        cantidadParticulas, radio, radio, radio, 0.1);
                } catch (Exception e) {
                    // Ignorar efectos inválidos
                }
            }
        }
        
        // Sonido
        if (!sonido.isEmpty()) {
            String[] parts = sonido.split(":");
            try {
                Sound sound = Sound.valueOf(parts[0]);
                float volumen = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
                float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
                
                for (Player p : jugadores) {
                    p.playSound(p.getLocation(), sound, volumen, pitch);
                }
            } catch (Exception e) {
                // Ignorar sonidos inválidos
            }
        }
        
        plugin.getLogger().info(String.format("[Apertura End] Spawn dramático: %d x %s cerca de %s", 
            cantidad, tipoEntidad, objetivo.getName()));
    }
    
    /**
     * Sistema de tareas para desbloquear la ubicación del portal
     * Los jugadores deben completar acciones durante la Fase 1
     */
    private void iniciarTareasDesbloqueo() {
        // Listeners para detectar acciones
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onEntityDeath(org.bukkit.event.entity.EntityDeathEvent e) {
                if (faseEvento != EventPhase.DESCUBRIMIENTO) return;
                if (!(e.getEntity().getKiller() instanceof Player)) return;
                
                String tipo = e.getEntityType().name();
                Player killer = e.getEntity().getKiller();
                
                // Tarea 1: Matar Enderman
                if (tipo.equals("ENDERMAN") && !tareasRealizadas.contains("matar_enderman")) {
                    tareasRealizadas.add("matar_enderman");
                    tareasCompletadas++;
                    
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§5§l✓ TAREA COMPLETADA");
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§7" + killer.getName() + " §7ha eliminado un §5Enderman");
                    Bukkit.broadcastMessage("§8§o'El End siente la pérdida...'");
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§e§lProgreso: §e" + tareasCompletadas + "§8/§e" + tareasRequeridas + " tareas");
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.0f, 0.7f);
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                    }
                    
                    verificarTareasCompletas();
                }
            }
            
            @org.bukkit.event.EventHandler
            public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent e) {
                if (faseEvento != EventPhase.DESCUBRIMIENTO) return;
                Player player = e.getPlayer();
                
                // Tarea 2: Recolectar Obsidiana
                if (e.getBlock().getType() == org.bukkit.Material.OBSIDIAN && !tareasRealizadas.contains("recolectar_obsidiana")) {
                    tareasRealizadas.add("recolectar_obsidiana");
                    tareasCompletadas++;
                    
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§5§l✓ TAREA COMPLETADA");
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§7" + player.getName() + " §7ha recolectado §8Obsidiana");
                    Bukkit.broadcastMessage("§8§o'El material del portal resuena...'");
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§e§lProgreso: §e" + tareasCompletadas + "§8/§e" + tareasRequeridas + " tareas");
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                    }
                    
                    verificarTareasCompletas();
                }
            }
            
            @org.bukkit.event.EventHandler
            public void onPickupItem(org.bukkit.event.entity.EntityPickupItemEvent e) {
                if (!(e.getEntity() instanceof Player)) return;
                if (faseEvento != EventPhase.DESCUBRIMIENTO) return;
                
                Player player = (Player) e.getEntity();
                
                // Tarea 3: Conseguir Ojo de Ender
                if (e.getItem().getItemStack().getType() == org.bukkit.Material.ENDER_EYE && !tareasRealizadas.contains("conseguir_ojo_ender")) {
                    tareasRealizadas.add("conseguir_ojo_ender");
                    tareasCompletadas++;
                    
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§5§l✓ TAREA COMPLETADA");
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§7" + player.getName() + " §7ha obtenido un §eOjo de Ender");
                    Bukkit.broadcastMessage("§8§o'La clave dimensional vibra con poder...'");
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§e§lProgreso: §e" + tareasCompletadas + "§8/§e" + tareasRequeridas + " tareas");
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.0f, 1.2f);
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                    }
                    
                    verificarTareasCompletas();
                }
            }
        }, plugin);
    }
    
    /**
     * Verificar si se completaron todas las tareas requeridas
     */
    private void verificarTareasCompletas() {
        if (tareasCompletadas >= tareasRequeridas && !direccionRevelada) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§5§l⚡ VISIÓN DESBLOQUEADA ⚡");
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§7El Observador puede ver más claramente...");
                    Bukkit.broadcastMessage("§7La ubicación se revela.");
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 1.0f);
                        p.spawnParticle(Particle.END_ROD, p.getLocation().add(0, 2, 0), 50, 0.5, 1, 0.5, 0.1);
                    }
                    
                    // Ahora sí revelar la dirección
                    revelarDireccionPortal();
                    direccionRevelada = true;
                }
            }.runTaskLater(plugin, 40L);
        }
    }
}

