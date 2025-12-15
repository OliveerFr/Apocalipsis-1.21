package me.apocalipsis.events;

import java.util.*;
import java.io.*;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;

/**
 * Evento Navidad - Pausa narrativa sin combates
 * 
 * Filosofía:
 * - NO es un evento de combate
 * - Es una pausa en la narrativa del mundo
 * - Control manual por comandos
 * - Ambiente sutil y no invasivo
 * - Entrega fragmentos misteriosos
 * 
 * El Observador deja pensamientos sueltos, sin hablar directamente.
 * Santa aparece como símbolo, entrega recuerdos.
 * Los fragmentos no explican su uso... todavía.
 */
public class NavidadEvent extends EventBase {
    
    // ═══════════════════════════════════════════════════════════════════
    // ESTADO DEL EVENTO
    // ═══════════════════════════════════════════════════════════════════
    
    private boolean eventoActivo = false;
    private boolean ambienteActivo = false;
    private boolean regalosActivos = false;
    private boolean arbolActivado = false;
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DEL ÁRBOL
    // ═══════════════════════════════════════════════════════════════════
    
    private Location arbolLocation = null;
    private BoundingBox arbolBoundingBox = null;
    private boolean arbolConfigurado = false;
    private boolean bloquesArbolDetectados = false;
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE SANTA
    // ═══════════════════════════════════════════════════════════════════
    
    private Villager santaEntity = null;
    private UUID santaUUID = null;
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE REGALOS
    // ═══════════════════════════════════════════════════════════════════
    
    private Set<UUID> jugadoresConRegalo = new HashSet<>();
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE FRAGMENTOS
    // ═══════════════════════════════════════════════════════════════════
    
    private Map<UUID, Integer> fragmentosPorJugador = new HashMap<>();
    private NavidadItems navidadItems;
    
    // ═══════════════════════════════════════════════════════════════════
    // CONTROL Y LÍMITES
    // ═══════════════════════════════════════════════════════════════════
    
    private long ultimoCliffhanger = 0;
    private int contadorPensamientosObservador = 0;
    private long inicioEvento = 0;
    private final Map<UUID, Boolean> jugadoresEnCinematica = new HashMap<>();
    
    // ═══════════════════════════════════════════════════════════════════
    // TASKS DE AMBIENTE
    // ═══════════════════════════════════════════════════════════════════
    
    private BukkitTask ambienteParticleTask;
    private BukkitTask ambienteSoundTask;
    private BukkitTask arbolParticleTask;
    private BukkitTask observadorTask;
    
    // ═══════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════════
    
    private ConfigurationSection config;
    private FileConfiguration navidadConfig;
    
    private final Random random = new Random();
    
    // Listener
    private NavidadListener navidadListener;
    
    // ═══════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════
    
    public NavidadEvent(Apocalipsis plugin, MessageBus messageBus, SoundUtil soundUtil) {
        super(plugin, messageBus, soundUtil, "navidad");
        loadConfig();
        
        // Crear sistema de items
        navidadItems = new NavidadItems(plugin);
        
        // Crear y registrar listener
        navidadListener = new NavidadListener(this);
        Bukkit.getPluginManager().registerEvents(navidadListener, plugin);
        
        // Cargar datos persistentes
        loadPersistentData();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // CARGA DE CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════════
    
    private void loadConfig() {
        navidadConfig = plugin.getConfigManager().getNavidadConfig();
        config = navidadConfig.getConfigurationSection("navidad");
        
        if (config == null) {
            plugin.getLogger().warning("[Navidad] Configuración no encontrada en navidad.yml");
        }
        
        // Cargar ubicación del árbol si existe
        loadArbolLocation();
    }
    
    private void loadArbolLocation() {
        if (config == null) return;
        
        ConfigurationSection arbolConfig = config.getConfigurationSection("arbol");
        if (arbolConfig == null) return;
        
        boolean configurado = arbolConfig.getBoolean("configurado", false);
        if (!configurado) return;
        
        String worldName = arbolConfig.getString("world", "world");
        World world = Bukkit.getWorld(worldName);
        
        if (world == null) {
            plugin.getLogger().warning("[Navidad] Mundo del árbol no encontrado: " + worldName);
            return;
        }
        
        double x = arbolConfig.getDouble("x", 0);
        double y = arbolConfig.getDouble("y", 64);
        double z = arbolConfig.getDouble("z", 0);
        
        arbolLocation = new Location(world, x, y, z);
        arbolConfigurado = true;
        
        // Detectar bloques del árbol
        detectarBloquesArbol();
        
        plugin.getLogger().info("[Navidad] Árbol cargado en: " + 
            world.getName() + " " + x + ", " + y + ", " + z);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // PERSISTENCIA DE DATOS
    // ═══════════════════════════════════════════════════════════════════
    
    private void loadPersistentData() {
        File dataFile = new File(plugin.getDataFolder(), "navidad_data.yml");
        if (!dataFile.exists()) return;
        
        try {
            FileConfiguration data = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(dataFile);
            
            // Cargar fragmentos
            ConfigurationSection fragmentos = data.getConfigurationSection("fragmentos");
            if (fragmentos != null) {
                for (String uuidStr : fragmentos.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        int cantidad = fragmentos.getInt(uuidStr, 0);
                        fragmentosPorJugador.put(uuid, cantidad);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("[Navidad] UUID inválido en datos: " + uuidStr);
                    }
                }
            }
            
            // Cargar jugadores que recibieron regalo
            List<String> regalosEntregados = data.getStringList("regalos_entregados");
            for (String uuidStr : regalosEntregados) {
                try {
                    jugadoresConRegalo.add(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("[Navidad] UUID inválido en regalos: " + uuidStr);
                }
            }
            
            plugin.getLogger().info("[Navidad] Datos persistentes cargados: " + 
                fragmentosPorJugador.size() + " jugadores con fragmentos, " + 
                jugadoresConRegalo.size() + " jugadores con regalos");
                
        } catch (Exception e) {
            plugin.getLogger().severe("[Navidad] Error cargando datos persistentes: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void savePersistentData() {
        File dataFile = new File(plugin.getDataFolder(), "navidad_data.yml");
        FileConfiguration data = new org.bukkit.configuration.file.YamlConfiguration();
        
        try {
            // Guardar fragmentos
            ConfigurationSection fragmentos = data.createSection("fragmentos");
            for (Map.Entry<UUID, Integer> entry : fragmentosPorJugador.entrySet()) {
                fragmentos.set(entry.getKey().toString(), entry.getValue());
            }
            
            // Guardar jugadores que recibieron regalo
            List<String> regalosEntregados = new ArrayList<>();
            for (UUID uuid : jugadoresConRegalo) {
                regalosEntregados.add(uuid.toString());
            }
            data.set("regalos_entregados", regalosEntregados);
            
            // Guardar archivo
            data.save(dataFile);
            
        } catch (IOException e) {
            plugin.getLogger().severe("[Navidad] Error guardando datos persistentes: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // MÉTODOS ABSTRACTOS IMPLEMENTADOS
    // ═══════════════════════════════════════════════════════════════════
    
    @Override
    public void onStart() {
        eventoActivo = true;
        inicioEvento = System.currentTimeMillis();
        contadorPensamientosObservador = 0;
        plugin.getLogger().info("[Navidad] Evento iniciado - Iniciando cinemática");
        
        // Iniciar cinemática de inicio épica
        iniciarCinematicaInicio();
    }
    
    /**
     * Cinemática épica de inicio del evento Navidad
     * Secuencia de ~30 segundos con efectos visuales, sonoros y narrativos
     */
    private void iniciarCinematicaInicio() {
        // Congelar jugadores durante cinemática (opcional)
        boolean congelarJugadores = config != null && config.getBoolean("cinematica.congelar_jugadores", false);
        Map<UUID, Boolean> jugadoresCongelados = new HashMap<>();
        
        if (congelarJugadores) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                jugadoresCongelados.put(player.getUniqueId(), player.getAllowFlight());
                player.setAllowFlight(true);
                player.setFlying(true);
                player.setWalkSpeed(0f);
            }
        }
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 1: EL MUNDO SE DETIENE (0-5 seg)
        // ═══════════════════════════════════════════════════════════════
        
        // T=0s - Silencio inicial
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle("§f", "§7§o...", 10, 40, 10);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.3f, 0.6f);
        }
        
        // T=2s - Primera señal
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                jugadoresEnCinematica.put(player.getUniqueId(), true);
                player.sendTitle("§f✦", "§7§oAlgo cambia en el aire...", 5, 50, 10);
                
                // Partículas blancas suaves alrededor
                Location loc = player.getLocation();
                World world = loc.getWorld();
                if (world != null) {
                    world.spawnParticle(Particle.SNOWFLAKE, 
                        loc.clone().add(0, 2, 0), 30, 3, 2, 3, 0.01);
                }
                
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.2f, 1.8f);
            }
        }, 40L); // 2 segundos
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 2: LA CALMA DESCIENDE (5-15 seg)
        // ═══════════════════════════════════════════════════════════════
        
        // T=5s - Mensaje del Observador
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            messageBus.broadcast("", "navidad-fase2");
            messageBus.broadcast("§8§o...Incluso los mundos rotos necesitan un respiro...", "navidad-fase2");
            messageBus.broadcast("", "navidad-fase2");
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.8f);
                
                // Más partículas - nieve cayendo
                Location loc = player.getLocation();
                World world = loc.getWorld();
                if (world != null) {
                    for (int i = 0; i < 20; i++) {
                        world.spawnParticle(Particle.SNOWFLAKE,
                            loc.clone().add(random.nextDouble() * 20 - 10, 10, random.nextDouble() * 20 - 10),
                            1, 0, 0, 0, 0);
                    }
                }
            }
        }, 100L); // 5 segundos
        
        // T=8s - El cielo se aclara
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                World world = player.getWorld();
                
                // Clear weather
                world.setStorm(false);
                world.setThundering(false);
                
                player.sendTitle("§f✦ §c✦ §f✦", "§7§oEl mundo entra en calma...", 5, 60, 15);
                
                // Círculo de partículas doradas
                Location loc = player.getLocation();
                for (int i = 0; i < 36; i++) {
                    double angle = Math.toRadians(i * 10);
                    double x = Math.cos(angle) * 5;
                    double z = Math.sin(angle) * 5;
                    world.spawnParticle(Particle.END_ROD,
                        loc.clone().add(x, 1, z), 1, 0, 0, 0, 0.01);
                }
                
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.2f);
            }
        }, 160L); // 8 segundos
        
        // T=12s - Mensaje poético
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            messageBus.broadcast("§7El mundo entra en un momento de calma.", "navidad-fase2b");
            messageBus.broadcast("§7Las tormentas cesan. Los ecos descansan.", "navidad-fase2b");
            messageBus.broadcast("§7Por ahora... solo paz.", "navidad-fase2b");
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
                
                // Luces cálidas flotantes
                Location loc = player.getLocation();
                World world = loc.getWorld();
                if (world != null) {
                    for (int i = 0; i < 15; i++) {
                        world.spawnParticle(Particle.FIREWORK,
                            loc.clone().add(
                                random.nextDouble() * 10 - 5,
                                random.nextDouble() * 5,
                                random.nextDouble() * 10 - 5
                            ), 1, 0, 0.1, 0, 0.01);
                    }
                }
            }
        }, 240L); // 12 segundos
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 3: LA NAVIDAD LLEGA (15-25 seg)
        // ═══════════════════════════════════════════════════════════════
        
        // T=16s - Título principal
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle(
                    "§c§l✦ §f§lNAVIDAD §c§l✦",
                    "§7Ha llegado al mundo",
                    10, 80, 20
                );
                
                // Explosión de partículas festivas
                Location loc = player.getLocation();
                loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 2, 0), 50, 2, 2, 2, 0.2);
                loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc.clone().add(0, 5, 0), 100, 5, 3, 5, 0.1);
                loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 3, 0), 30, 3, 2, 3, 0.05);
                
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
            }
        }, 320L); // 16 segundos
        
        // T=20s - Mensaje del evento
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            messageBus.broadcast("", "navidad-inicio");
            messageBus.broadcast("§c§l✦ §f§lEVENTO NAVIDAD §c§l✦", "navidad-inicio");
            messageBus.broadcast("", "navidad-inicio");
            messageBus.broadcast("§7Un momento de paz desciende sobre el mundo.", "navidad-inicio");
            messageBus.broadcast("§7No hay enemigos. No hay batallas. Solo calma.", "navidad-inicio");
            messageBus.broadcast("", "navidad-inicio");
            messageBus.broadcast("§7Reúnanse. Compartan. Descansen.", "navidad-inicio");
            messageBus.broadcast("§7Los fragmentos de recuerdo esperan...", "navidad-inicio");
            messageBus.broadcast("", "navidad-inicio");
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
            }
        }, 400L); // 20 segundos
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 4: FINALIZACIÓN (25-30 seg)
        // ═══════════════════════════════════════════════════════════════
        
        // T=25s - Última oleada de efectos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Location loc = player.getLocation();
                
                // Espiral de partículas ascendente
                for (int i = 0; i < 50; i++) {
                    int finalI = i;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double angle = Math.toRadians(finalI * 20);
                        double radius = 3;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        double y = finalI * 0.2;
                        
                        loc.getWorld().spawnParticle(Particle.WAX_ON,
                            loc.clone().add(x, y, z), 1, 0, 0, 0, 0);
                    }, i);
                }
                
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.8f);
            }
        }, 500L); // 25 segundos
        
        // T=30s - Fin de cinemática, activar evento
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Descongelar jugadores
            if (congelarJugadores) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    if (jugadoresCongelados.containsKey(uuid)) {
                        player.setAllowFlight(jugadoresCongelados.get(uuid));
                        player.setFlying(false);
                    }
                    player.setWalkSpeed(0.2f); // Velocidad normal
                }
            }
            
            // Título final
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle("§a✓", "§7Que comience la celebración", 10, 40, 20);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            }
            
            // Activar ambiente automáticamente
            activarAmbiente();
            
            // Iniciar pensamientos del Observador
            iniciarObservador();
            
            // Limpiar tracking de cinemática
            jugadoresEnCinematica.clear();
            
            plugin.getLogger().info("[Navidad] Cinemática completada - Evento activo");
            
        }, 600L); // 30 segundos
    }
    
    @Override
    public void onStop() {
        eventoActivo = false;
        plugin.getLogger().info("[Navidad] Evento detenido");
        
        // Mensaje global
        String mensajeFin = config != null ?
            config.getString("mensajes.fin", "§7El mundo vuelve a la normalidad.") :
            "§7El mundo vuelve a la normalidad.";
        
        messageBus.broadcast(mensajeFin, "navidad-fin");
        
        // Limpiar todo
        cleanup();
    }
    
    @Override
    public void onTick() {
        // El evento de Navidad no tiene tick activo constante
        // Todo es manejado por tasks programadas
    }
    
    @Override
    public String getDisplayName() {
        return "§c§l✦ Navidad ✦";
    }
    
    @Override
    public String getDescription() {
        return "Pausa narrativa - Un momento de calma en el mundo";
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE AMBIENTE
    // ═══════════════════════════════════════════════════════════════════
    
    public void activarAmbiente() {
        if (ambienteActivo) return;
        
        ambienteActivo = true;
        plugin.getLogger().info("[Navidad] Ambiente activado");
        
        // Iniciar partículas ambiente
        iniciarParticulasAmbiente();
        
        // Iniciar sonidos ambiente
        iniciarSonidosAmbiente();
    }
    
    public void desactivarAmbiente() {
        if (!ambienteActivo) return;
        
        ambienteActivo = false;
        plugin.getLogger().info("[Navidad] Ambiente desactivado");
        
        // Cancelar tasks
        if (ambienteParticleTask != null) {
            ambienteParticleTask.cancel();
            ambienteParticleTask = null;
        }
        
        if (ambienteSoundTask != null) {
            ambienteSoundTask.cancel();
            ambienteSoundTask = null;
        }
    }
    
    private void iniciarParticulasAmbiente() {
        if (config == null) return;
        
        int intervalo = config.getInt("ambiente.particulas.intervalo_ticks", 40);
        double intensidad = config.getDouble("ambiente.particulas.intensidad", 0.5);
        
        ambienteParticleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Location loc = player.getLocation();
                World world = player.getWorld();
                
                // Nieve suave
                if (config.getBoolean("ambiente.particulas.nieve", true)) {
                    world.spawnParticle(Particle.SNOWFLAKE, 
                        loc.clone().add(random.nextDouble() * 20 - 10, 5, random.nextDouble() * 20 - 10),
                        (int)(3 * intensidad), 2, 0, 2, 0);
                }
                
                // Luces cálidas
                if (config.getBoolean("ambiente.particulas.luces", true)) {
                    world.spawnParticle(Particle.END_ROD,
                        loc.clone().add(random.nextDouble() * 15 - 7.5, 3, random.nextDouble() * 15 - 7.5),
                        1, 0, 0, 0, 0.01);
                }
                
                // Chispas ocasionales
                if (config.getBoolean("ambiente.particulas.chispas", true) && random.nextDouble() < 0.1) {
                    world.spawnParticle(Particle.FIREWORK,
                        loc.clone().add(random.nextDouble() * 10 - 5, 4, random.nextDouble() * 10 - 5),
                        1, 0, 0, 0, 0.01);
                }
            }
        }, intervalo, intervalo);
    }
    
    private void iniciarSonidosAmbiente() {
        if (config == null) return;
        
        int intervaloSeg = config.getInt("ambiente.sonidos.intervalo_seg", 30);
        
        ambienteSoundTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Campanas lejanas
                if (config.getBoolean("ambiente.sonidos.campanas", true)) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 
                        0.3f, 0.8f);
                }
                
                // Viento suave (ocasional)
                if (config.getBoolean("ambiente.sonidos.viento", true) && random.nextDouble() < 0.3) {
                    player.playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING,
                        0.2f, 0.5f);
                }
            }
        }, intervaloSeg * 20L, intervaloSeg * 20L);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DEL ÁRBOL
    // ═══════════════════════════════════════════════════════════════════
    
    public void establecerArbol(Location location) {
        this.arbolLocation = location.clone();
        this.arbolConfigurado = true;
        
        // Detectar bloques del árbol
        detectarBloquesArbol();
        
        // Guardar en config
        guardarArbolEnConfig();
        
        plugin.getLogger().info("[Navidad] Árbol establecido en: " + 
            location.getWorld().getName() + " " + 
            location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
    }
    
    private void detectarBloquesArbol() {
        if (arbolLocation == null) return;
        
        // Crear BoundingBox en lugar de Set de locations (optimización de memoria)
        int radio = 5;
        int alturaExtra = 10;
        
        Location min = arbolLocation.clone().add(-radio, -radio, -radio);
        Location max = arbolLocation.clone().add(radio, alturaExtra, radio);
        
        arbolBoundingBox = new BoundingBox(
            min.getX(), min.getY(), min.getZ(),
            max.getX(), max.getY(), max.getZ()
        );
        
        bloquesArbolDetectados = true;
        plugin.getLogger().info("[Navidad] Árbol definido con BoundingBox: " + 
            radio + " bloques de radio, " + alturaExtra + " bloques de altura");
    }
    
    private void guardarArbolEnConfig() {
        if (config == null || arbolLocation == null) return;
        
        config.set("arbol.world", arbolLocation.getWorld().getName());
        config.set("arbol.x", arbolLocation.getX());
        config.set("arbol.y", arbolLocation.getY());
        config.set("arbol.z", arbolLocation.getZ());
        config.set("arbol.configurado", true);
        
        plugin.getConfigManager().saveNavidadConfig();
    }
    
    public void activarArbol() {
        if (!arbolConfigurado || arbolLocation == null) return;
        if (arbolActivado) return;
        
        arbolActivado = true;
        plugin.getLogger().info("[Navidad] Árbol activado");
        
        // Partículas concentradas en el árbol
        arbolParticleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!eventoActivo || arbolLocation == null) return;
            
            World world = arbolLocation.getWorld();
            if (world == null) return;
            
            // Verificar que el chunk esté cargado
            Chunk chunk = arbolLocation.getChunk();
            if (!chunk.isLoaded()) return;
            
            // Partículas mágicas concentradas
            world.spawnParticle(Particle.ENCHANT,
                arbolLocation.clone().add(0.5, 2, 0.5),
                20, 2, 3, 2, 0.5);
            
            // Glow
            world.spawnParticle(Particle.GLOW,
                arbolLocation.clone().add(0.5, 2, 0.5),
                10, 1.5, 2, 1.5, 0.1);
            
            // Sonido suave del árbol
            world.playSound(arbolLocation, Sound.BLOCK_BEACON_AMBIENT, 0.3f, 1.2f);
            
        }, 20L, 20L); // Cada segundo
    }
    
    public void desactivarArbol() {
        if (!arbolActivado) return;
        
        arbolActivado = false;
        plugin.getLogger().info("[Navidad] Árbol desactivado");
        
        if (arbolParticleTask != null) {
            arbolParticleTask.cancel();
            arbolParticleTask = null;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE SANTA
    // ═══════════════════════════════════════════════════════════════════
    
    public void spawnearSanta() {
        if (!arbolConfigurado || arbolLocation == null) {
            plugin.getLogger().warning("[Navidad] No se puede spawnear Santa: árbol no configurado");
            return;
        }
        
        if (santaEntity != null && santaEntity.isValid()) {
            plugin.getLogger().warning("[Navidad] Santa ya está spawneado");
            return;
        }
        
        // Obtener offset desde config
        int offset = config != null ? config.getInt("santa.spawn_offset", 5) : 5;
        
        // Ubicación cerca del árbol
        Location spawnLoc = arbolLocation.clone().add(offset, 0, 0);
        World world = spawnLoc.getWorld();
        
        if (world == null) return;
        
        // Spawnear villager
        santaEntity = (Villager) world.spawnEntity(spawnLoc, EntityType.VILLAGER);
        santaEntity.setProfession(Villager.Profession.NONE);
        santaEntity.setVillagerType(Villager.Type.SNOW);
        santaEntity.setAI(false);
        santaEntity.setInvulnerable(true);
        santaEntity.setSilent(true);
        
        // Nombre custom
        String nombre = config != null ? 
            config.getString("santa.nombre", "§c§l✦ Santa §c§l✦") :
            "§c§l✦ Santa §c§l✦";
        santaEntity.setCustomName(nombre);
        santaEntity.setCustomNameVisible(true);
        
        santaUUID = santaEntity.getUniqueId();
        
        // Efectos de aparición
        world.spawnParticle(Particle.CLOUD, spawnLoc.clone().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.1);
        world.spawnParticle(Particle.HAPPY_VILLAGER, spawnLoc.clone().add(0, 1, 0), 20, 0.5, 1, 0.5, 0);
        world.playSound(spawnLoc, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.0f);
        
        // Mensaje
        String mensaje = config != null ?
            config.getString("mensajes.santa_spawn", "§c✦ Santa ha aparecido cerca del árbol...") :
            "§c✦ Santa ha aparecido cerca del árbol...";
        messageBus.broadcast(mensaje, "navidad-santa-spawn");
        
        plugin.getLogger().info("[Navidad] Santa spawneado");
    }
    
    private void iniciarVerificacionSanta() {
        // Verificar cada 5 segundos que Santa sigue vivo
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!eventoActivo) return;
            if (santaEntity == null) return;
            
            // Si Santa fue removido/murió, notificar
            if (!santaEntity.isValid()) {
                plugin.getLogger().warning("[Navidad] Santa desapareció inesperadamente");
                santaEntity = null;
                santaUUID = null;
            }
        }, 100L, 100L); // Cada 5 segundos
    }
    
    public void despawnearSanta() {
        if (santaEntity == null || !santaEntity.isValid()) {
            plugin.getLogger().warning("[Navidad] Santa no está spawneado");
            santaEntity = null;
            santaUUID = null;
            return;
        }
        
        Location loc = santaEntity.getLocation();
        World world = loc.getWorld();
        
        if (world != null) {
            // Efectos de desaparición
            world.spawnParticle(Particle.CLOUD, loc.clone().add(0, 1, 0), 40, 0.5, 1, 0.5, 0.05);
            world.playSound(loc, Sound.ENTITY_VILLAGER_NO, 0.5f, 0.8f);
        }
        
        // Mensaje
        String mensaje = config != null ?
            config.getString("mensajes.santa_despawn", "§c✦ Santa se desvanece lentamente...") :
            "§c✦ Santa se desvanece lentamente...";
        messageBus.broadcast(mensaje, "navidad-santa-despawn");
        
        // Remover entidad
        santaEntity.remove();
        santaEntity = null;
        santaUUID = null;
        
        plugin.getLogger().info("[Navidad] Santa despawneado");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE REGALOS
    // ═══════════════════════════════════════════════════════════════════
    
    public void activarRegalos() {
        regalosActivos = true;
        
        String mensaje = config != null ?
            config.getString("mensajes.regalos_activados", "§a✦ Los regalos han sido activados.") :
            "§a✦ Los regalos han sido activados.";
        messageBus.broadcast(mensaje, "navidad-regalos-on");
        
        plugin.getLogger().info("[Navidad] Regalos activados");
    }
    
    public void desactivarRegalos() {
        regalosActivos = false;
        
        String mensaje = config != null ?
            config.getString("mensajes.regalos_desactivados", "§c✦ Los regalos han sido desactivados.") :
            "§c✦ Los regalos han sido desactivados.";
        messageBus.broadcast(mensaje, "navidad-regalos-off");
        
        plugin.getLogger().info("[Navidad] Regalos desactivados");
    }
    
    public boolean yaRecibioRegalo(Player player) {
        return jugadoresConRegalo.contains(player.getUniqueId());
    }
    
    public void marcarRegaloRecibido(Player player) {
        jugadoresConRegalo.add(player.getUniqueId());
    }
    
    public String getMensajeRegaloRecibido() {
        return config != null ?
            config.getString("mensajes.regalo_recibido", "§a✦ Has recibido un regalo.") :
            "§a✦ Has recibido un regalo.";
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE FRAGMENTOS
    // ═══════════════════════════════════════════════════════════════════
    
    public void darFragmentos(Player player, int cantidad) {
        UUID uuid = player.getUniqueId();
        int actual = fragmentosPorJugador.getOrDefault(uuid, 0);
        fragmentosPorJugador.put(uuid, actual + cantidad);
        
        // Dar item físico
        player.getInventory().addItem(navidadItems.crearFragmentoRecuerdo(cantidad));
        
        player.sendMessage("§d✦ Has recibido §f" + cantidad + " §dfragmento(s) de recuerdo.");
        
        savePersistentData();
    }
    
    public void darFragmentosTodos(int cantidad) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            darFragmentos(player, cantidad);
        }
    }
    
    public int getFragmentos(Player player) {
        return fragmentosPorJugador.getOrDefault(player.getUniqueId(), 0);
    }
    
    public void mostrarInfoFragmentos(Player player) {
        int cantidad = getFragmentos(player);
        String mensaje = config != null ?
            config.getString("mensajes.fragmento_info", "§7Tienes §d{cantidad} §7fragmentos de recuerdo.")
                .replace("{cantidad}", String.valueOf(cantidad)) :
            "§7Tienes §d" + cantidad + " §7fragmentos de recuerdo.";
        
        player.sendMessage(mensaje);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DEL OBSERVADOR
    // ═══════════════════════════════════════════════════════════════════
    
    private void iniciarObservador() {
        if (config == null) return;
        
        int intervaloMin = config.getInt("observador.intervalo_min_seg", 300);
        double probabilidad = config.getDouble("observador.probabilidad", 0.3);
        List<String> pensamientos = config.getStringList("observador.pensamientos");
        
        if (pensamientos.isEmpty()) return;
        
        observadorTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!eventoActivo) return;
            
            // Probabilidad de mostrar pensamiento
            if (random.nextDouble() > probabilidad) return;
            
            // Seleccionar pensamiento aleatorio
            String pensamiento = pensamientos.get(random.nextInt(pensamientos.size()));
            
            // Broadcast sutil
            messageBus.broadcast(pensamiento, "navidad-observador");
            
        }, intervaloMin * 20L, intervaloMin * 20L);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // CLIFFHANGER
    // ═══════════════════════════════════════════════════════════════════
    
    public void activarCliffhanger() {
        if (config == null) return;
        
        plugin.getLogger().info("[Navidad] Activando cliffhanger...");
        
        String sonido = config.getString("cliffhanger.sonido", "AMBIENT_CAVE");
        double intensidad = config.getDouble("cliffhanger.temblor_intensidad", 0.3);
        String mensajeFinal = config.getString("cliffhanger.mensaje_final", 
            "§8§o...y las puertas no se abren solas.");
        int duracion = config.getInt("cliffhanger.duracion_seg", 10);
        
        // Secuencia de cliffhanger
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Sonido profundo
            try {
                Sound sound = Sound.valueOf(sonido);
                player.playSound(player.getLocation(), sound, 1.0f, 0.5f);
            } catch (IllegalArgumentException e) {
                player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1.0f, 0.5f);
            }
        }
        
        // Temblor leve (después de 2 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Efecto de temblor visual
                player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.8f);
                
                // Partículas oscuras
                player.getWorld().spawnParticle(Particle.SMOKE,
                    player.getLocation(), 10, 1, 0.5, 1, 0.01);
            }
        }, 40L);
        
        // Fragmento que se apaga (después de 5 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.3f, 0.5f);
            }
        }, 100L);
        
        // Mensaje final (después de 8 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            messageBus.broadcast(mensajeFinal, "navidad-cliffhanger");
        }, 160L);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // LIMPIEZA Y RESET
    // ═══════════════════════════════════════════════════════════════════
    
    public void reset() {
        plugin.getLogger().info("[Navidad] Reset del evento");
        
        // Detener todo
        cleanup();
        
        // Reiniciar estados
        eventoActivo = false;
        ambienteActivo = false;
        regalosActivos = false;
        arbolActivado = false;
        
        // Limpiar regalos
        jugadoresConRegalo.clear();
        
        // NO borrar fragmentos (son permanentes)
        // NO borrar ubicación del árbol
    }
    
    private void cleanup() {
        // Guardar datos antes de limpiar
        savePersistentData();
        
        // Desactivar ambiente
        if (ambienteActivo) {
            desactivarAmbiente();
        }
        
        // Desactivar árbol
        if (arbolActivado) {
            desactivarArbol();
        }
        
        // Despawnear Santa si existe
        if (santaEntity != null) {
            if (santaEntity.isValid()) {
                santaEntity.remove();
            }
            santaEntity = null;
            santaUUID = null;
        }
        
        // Cancelar task del Observador
        if (observadorTask != null && !observadorTask.isCancelled()) {
            observadorTask.cancel();
            observadorTask = null;
        }
        
        // Limpiar tracking de cinemática
        jugadoresEnCinematica.clear();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════
    
    public boolean esParteDeLArbol(Location location) {
        if (!arbolConfigurado || arbolBoundingBox == null) return false;
        
        // Verificar que el mundo sea el correcto
        if (arbolLocation == null || !location.getWorld().equals(arbolLocation.getWorld())) {
            return false;
        }
        
        // Lazy load de detección
        if (!bloquesArbolDetectados) {
            detectarBloquesArbol();
        }
        
        // Usar BoundingBox (mucho más eficiente que Set<Location>)
        return arbolBoundingBox.contains(location.toVector());
    }
    
    public boolean isAmbienteActivo() {
        return ambienteActivo;
    }
    
    public boolean isRegalosActivos() {
        return regalosActivos;
    }
    
    public boolean isArbolActivado() {
        return arbolActivado;
    }
    
    public boolean isArbolConfigurado() {
        return arbolConfigurado;
    }
    
    public Location getArbolLocation() {
        return arbolLocation != null ? arbolLocation.clone() : null;
    }
    
    public boolean isSantaSpawneado() {
        return santaEntity != null && santaEntity.isValid();
    }
}
