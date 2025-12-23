package me.apocalipsis.events;

import java.util.*;
import java.io.*;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
    
    private Player santaPlayer = null; // Santa es el jugador que ejecuta el comando
    private UUID santaUUID = null;
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE REGALOS
    // ═══════════════════════════════════════════════════════════════════
    
    private Set<UUID> jugadoresConRegalo = new HashSet<>();
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE AMIGO SECRETO
    // ═══════════════════════════════════════════════════════════════════
    
    private boolean amigoSecretoActivo = false;
    private Map<UUID, UUID> asignacionesAmigoSecreto = new HashMap<>();  // Quien da -> Quien recibe
    private Map<UUID, Integer> regalosEntregados = new HashMap<>();      // Cuántos regalos ha entregado cada uno
    private Map<UUID, Integer> regalosRecibidos = new HashMap<>();       // Cuántos regalos ha recibido cada uno
    private Map<UUID, Double> valorTotalRegalos = new HashMap<>();       // Valor total de regalos dados
    private Map<UUID, Integer> recompensasXPPendientes = new HashMap<>();    // XP pendiente por entregar al final
    private Map<UUID, Integer> recompensasFragmentosPendientes = new HashMap<>(); // Fragmentos pendientes
    private BukkitTask recordatorioTask;
    private int contadorRecordatorios = 0;
    
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
    private BukkitTask weatherSnowTask;
    private BukkitTask snowAccumulationTask;
    
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
        
        // ACTIVAR AMBIENTE AUTOMÁTICAMENTE al iniciar
        activarAmbiente();
        
        // Spawnear muñecos de nieve decorativos alrededor del mundo
        spawnearMuñecosDeNieve();
        
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
        
        // T=5s - Efectos visuales
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
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
        
        // T=12s - Efectos visuales
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
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
        
        // T=20s - Efectos de sonido
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
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
        plugin.getLogger().info("[Navidad] Evento detenido - Iniciando cinemática final");
        
        // Entregar recompensas pendientes del amigo secreto ANTES de la cinemática
        if (amigoSecretoActivo) {
            entregarRecompensasPendientes();
        }
        
        // Iniciar cinemática final feliz ANTES de limpiar
        iniciarCinematicaFinal();
        
        // Cleanup se ejecutará al final de la cinemática (después de 40 segundos)
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
        
        // Iniciar clima de nieve constante
        iniciarClimaNavidad();
        
        // Iniciar partículas ambiente
        iniciarParticulasAmbiente();
        
        // Iniciar sonidos ambiente
        iniciarSonidosAmbiente();
        
        // Iniciar acumulación de nieve en el suelo
        iniciarAcumulacionNieve();
    }
    
    public void desactivarAmbiente() {
        if (!ambienteActivo) return;
        
        ambienteActivo = false;
        plugin.getLogger().info("[Navidad] Ambiente desactivado");
        
        // Cancelar tasks
        if (weatherSnowTask != null) {
            weatherSnowTask.cancel();
            weatherSnowTask = null;
        }
        
        if (ambienteParticleTask != null) {
            ambienteParticleTask.cancel();
            ambienteParticleTask = null;
        }
        
        if (ambienteSoundTask != null) {
            ambienteSoundTask.cancel();
            ambienteSoundTask = null;
        }
        
        if (snowAccumulationTask != null) {
            snowAccumulationTask.cancel();
            snowAccumulationTask = null;
        }
        
        // Limpiar clima en todos los mundos
        for (World world : Bukkit.getWorlds()) {
            world.setStorm(false);
            world.setThundering(false);
        }
    }
    
    /**
     * Mantiene el clima despejado en todos los mundos durante el evento de Navidad
     * (excepto Nether y End). La nieve visual se logra 100% con partículas densas.
     */
    private void iniciarClimaNavidad() {
        // Desactivar lluvia/tormenta inmediatamente en todos los mundos (excepto Nether y End)
        for (World world : Bukkit.getWorlds()) {
            // Excluir Nether y End - no pueden tener clima
            if (world.getEnvironment() == World.Environment.NETHER || 
                world.getEnvironment() == World.Environment.THE_END) {
                continue;
            }
            
            // CLIMA DESPEJADO - sin lluvia ni tormenta
            world.setStorm(false);
            world.setThundering(false);
            world.setClearWeatherDuration(Integer.MAX_VALUE); // Despejado infinito
            world.setWeatherDuration(0);
        }
        
        plugin.getLogger().info("[Navidad] Clima despejado activado - nevada 100% visual con partículas");
        
        // Task que verifica y mantiene el clima despejado cada 5 segundos
        weatherSnowTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (World world : Bukkit.getWorlds()) {
                // Excluir Nether y End
                if (world.getEnvironment() == World.Environment.NETHER || 
                    world.getEnvironment() == World.Environment.THE_END) {
                    continue;
                }
                
                // Si el clima cambió a lluvia/tormenta, restaurar despejado
                if (world.hasStorm() || world.isThundering()) {
                    world.setStorm(false);
                    world.setThundering(false);
                    world.setClearWeatherDuration(Integer.MAX_VALUE);
                    world.setWeatherDuration(0);
                }
            }
        }, 100L, 100L); // Cada 5 segundos (100 ticks)
    }
    
    /**
     * Acumula capas de nieve gradualmente en el suelo durante el evento
     */
    private void iniciarAcumulacionNieve() {
        plugin.getLogger().info("[Navidad] Acumulación de nieve en el suelo activada");
        
        // Task que acumula nieve cada 10 segundos
        snowAccumulationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                World world = player.getWorld();
                
                // No acumular nieve en Nether ni End
                if (world.getEnvironment() == World.Environment.NETHER || 
                    world.getEnvironment() == World.Environment.THE_END) {
                    continue;
                }
                
                Location playerLoc = player.getLocation();
                
                // Intentar colocar 3-5 capas de nieve en un radio de 15 bloques del jugador
                int intentos = 3 + random.nextInt(3);
                
                for (int i = 0; i < intentos; i++) {
                    // Ubicación aleatoria alrededor del jugador
                    double offsetX = (random.nextDouble() * 30) - 15;
                    double offsetZ = (random.nextDouble() * 30) - 15;
                    
                    int blockX = playerLoc.getBlockX() + (int)offsetX;
                    int blockZ = playerLoc.getBlockZ() + (int)offsetZ;
                    
                    // Obtener el bloque más alto en esa ubicación
                    int highestY = world.getHighestBlockYAt(blockX, blockZ);
                    Location snowLoc = new Location(world, blockX, highestY, blockZ);
                    
                    Block blockBelow = snowLoc.getBlock();
                    Block blockAbove = snowLoc.clone().add(0, 1, 0).getBlock();
                    
                    // Verificar que el bloque de abajo sea sólido y arriba esté vacío
                    if (!blockBelow.getType().isSolid() || !blockAbove.getType().isAir()) {
                        continue;
                    }
                    
                    // No colocar nieve sobre ciertos bloques (lava, agua, etc)
                    Material belowType = blockBelow.getType();
                    if (belowType == Material.LAVA || 
                        belowType == Material.WATER ||
                        belowType == Material.MAGMA_BLOCK ||
                        belowType.name().contains("LEAVES")) {
                        continue;
                    }
                    
                    // Si ya hay nieve, aumentar la capa (máximo 8 capas = bloque completo)
                    if (blockAbove.getType() == Material.SNOW) {
                        org.bukkit.block.data.type.Snow snowData = 
                            (org.bukkit.block.data.type.Snow) blockAbove.getBlockData();
                        
                        int currentLayers = snowData.getLayers();
                        if (currentLayers < snowData.getMaximumLayers()) {
                            snowData.setLayers(currentLayers + 1);
                            blockAbove.setBlockData(snowData, false);
                        }
                    } else if (blockAbove.getType().isAir()) {
                        // Colocar primera capa de nieve
                        blockAbove.setType(Material.SNOW, false);
                        org.bukkit.block.data.type.Snow snowData = 
                            (org.bukkit.block.data.type.Snow) blockAbove.getBlockData();
                        snowData.setLayers(1);
                        blockAbove.setBlockData(snowData, false);
                    }
                }
            }
        }, 200L, 200L); // Cada 10 segundos (200 ticks)
    }
    
    private void iniciarParticulasAmbiente() {
        if (config == null) return;
        
        // Intervalo ultra rápido para nevada MASIVA que cubra la lluvia en biomas cálidos (10 ticks = 0.5 segundos)
        int intervalo = config.getInt("ambiente.particulas.intervalo_ticks", 10);
        double intensidad = config.getDouble("ambiente.particulas.intensidad", 1.5);
        
        ambienteParticleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Location loc = player.getLocation();
                World world = player.getWorld();
                
                // ═════ NEVADA INTENSA Y CONSTANTE ═════
                if (config.getBoolean("ambiente.particulas.nieve", true)) {
                    // Excluir Nether - allí no nieva
                    if (world.getEnvironment() == World.Environment.NETHER) {
                        continue;
                    }
                    
                    // CLIMA DESPEJADO + PARTÍCULAS MASIVAS = Nevada visual perfecta sin lluvia
                    // Esta densidad extrema compensa la ausencia de nieve nativa de MC
                    
                    // Nieve cayendo MASIVAMENTE desde arriba (60 copos por tick)
                    for (int i = 0; i < 60; i++) {
                        double offsetX = random.nextDouble() * 50 - 25;
                        double offsetZ = random.nextDouble() * 50 - 25;
                        double height = 8 + random.nextDouble() * 10;
                        world.spawnParticle(Particle.SNOWFLAKE, 
                            loc.clone().add(offsetX, height, offsetZ),
                            2, 0.3, 0.7, 0.3, 0.02);
                    }
                    
                    // Nieve EXTREMADAMENTE densa cerca del jugador (45 copos)
                    for (int i = 0; i < 45; i++) {
                        world.spawnParticle(Particle.SNOWFLAKE,
                            loc.clone().add(
                                random.nextDouble() * 20 - 10,
                                random.nextDouble() * 5,
                                random.nextDouble() * 20 - 10
                            ),
                            3, 0.7, 0.7, 0.7, 0.03);
                    }
                    
                    // Capa adicional de nieve a media altura (30 copos)
                    for (int i = 0; i < 30; i++) {
                        world.spawnParticle(Particle.SNOWFLAKE,
                            loc.clone().add(
                                random.nextDouble() * 14 - 7,
                                2 + random.nextDouble() * 3,
                                random.nextDouble() * 14 - 7
                            ),
                            1, 0.4, 0.4, 0.4, 0.02);
                    }
                    
                    // Nieve acumulándose en el suelo (más frecuente)
                    if (random.nextDouble() < 0.7) {
                        world.spawnParticle(Particle.BLOCK,
                            loc.clone().add(
                                random.nextDouble() * 12 - 6,
                                0.1,
                                random.nextDouble() * 12 - 6
                            ),
                            5, 1.5, 0.1, 1.5, 0,
                            Material.SNOW_BLOCK.createBlockData());
                    }
                    
                    // WHITE_ASH adicional para efecto de nevada densa
                    for (int i = 0; i < 15; i++) {
                        world.spawnParticle(Particle.WHITE_ASH,
                            loc.clone().add(
                                random.nextDouble() * 15 - 7.5,
                                random.nextDouble() * 6,
                                random.nextDouble() * 15 - 7.5
                            ),
                            1, 0.5, 1, 0.5, 0.01);
                    }
                }
                
                // ═════ LUCES NAVIDEÑAS FESTIVAS (Rojo, Verde, Dorado, Blanco) ═════
                if (config.getBoolean("ambiente.particulas.luces", true)) {
                    // Luces doradas cálidas (estilo guirnaldas)
                    for (int i = 0; i < 5; i++) {
                        world.spawnParticle(Particle.END_ROD,
                            loc.clone().add(
                                random.nextDouble() * 25 - 12.5,
                                2 + random.nextDouble() * 4,
                                random.nextDouble() * 25 - 12.5
                            ),
                            1, 0, 0, 0, 0.01);
                    }
                    
                    // Luces rojas festivas
                    if (random.nextDouble() < 0.6) {
                        world.spawnParticle(Particle.CHERRY_LEAVES,
                            loc.clone().add(
                                random.nextDouble() * 20 - 10,
                                2.5 + random.nextDouble() * 3,
                                random.nextDouble() * 20 - 10
                            ),
                            3, 0.3, 0.3, 0.3, 0.02);
                    }
                    
                    // Luces verdes navideñas
                    if (random.nextDouble() < 0.4) {
                        world.spawnParticle(Particle.HAPPY_VILLAGER,
                            loc.clone().add(
                                random.nextDouble() * 18 - 9,
                                2 + random.nextDouble() * 3.5,
                                random.nextDouble() * 18 - 9
                            ),
                            2, 0.2, 0.2, 0.2, 0);
                    }
                    
                    // Destellos blancos brillantes
                    if (random.nextDouble() < 0.3) {
                        world.spawnParticle(Particle.GLOW_SQUID_INK,
                            loc.clone().add(
                                random.nextDouble() * 15 - 7.5,
                                2.5 + random.nextDouble() * 3,
                                random.nextDouble() * 15 - 7.5
                            ),
                            1, 0.1, 0.1, 0.1, 0.05);
                    }
                }
                
                // ═════ ESTRELLAS BRILLANTES NAVIDEÑAS ═════
                if (random.nextDouble() < 0.25) {
                    world.spawnParticle(Particle.GLOW,
                        loc.clone().add(
                            random.nextDouble() * 30 - 15,
                            6 + random.nextDouble() * 4,
                            random.nextDouble() * 30 - 15
                        ),
                        2, 0.1, 0.1, 0.1, 0.08);
                    
                    // Estrella dorada grande ocasional
                    if (random.nextDouble() < 0.3) {
                        world.spawnParticle(Particle.WAX_OFF,
                            loc.clone().add(
                                random.nextDouble() * 25 - 12.5,
                                7 + random.nextDouble() * 3,
                                random.nextDouble() * 25 - 12.5
                            ),
                            1, 0, 0, 0, 0);
                    }
                }
                
                // ═════ FUEGOS ARTIFICIALES FESTIVOS ═════
                if (config.getBoolean("ambiente.particulas.chispas", true) && random.nextDouble() < 0.12) {
                    // Fuegos artificiales rojos y verdes
                    Particle fireworkColor = random.nextBoolean() ? Particle.FIREWORK : Particle.CHERRY_LEAVES;
                    world.spawnParticle(fireworkColor,
                        loc.clone().add(
                            random.nextDouble() * 20 - 10,
                            5 + random.nextDouble() * 4,
                            random.nextDouble() * 20 - 10
                        ),
                        5, 0.5, 0.5, 0.5, 0.08);
                }
                
                // ═════ COPOS DE NIEVE GRANDES OCASIONALES ═════
                if (random.nextDouble() < 0.08) {
                    world.spawnParticle(Particle.ITEM_SNOWBALL,
                        loc.clone().add(
                            random.nextDouble() * 14 - 7,
                            6 + random.nextDouble() * 3,
                            random.nextDouble() * 14 - 7
                        ),
                        1, 0, 0, 0, 0);
                }
                
                // ═════ PARTÍCULAS DE ESCARCHA EN EL AIRE ═════
                if (random.nextDouble() < 0.2) {
                    world.spawnParticle(Particle.WHITE_ASH,
                        loc.clone().add(
                            random.nextDouble() * 10 - 5,
                            1 + random.nextDouble() * 2,
                            random.nextDouble() * 10 - 5
                        ),
                        3, 0.8, 0.5, 0.8, 0.01);
                }
                
                // ═════ BRILLOS MÁGICOS NAVIDEÑOS ═════
                if (random.nextDouble() < 0.15) {
                    world.spawnParticle(Particle.ENCHANT,
                        loc.clone().add(
                            random.nextDouble() * 12 - 6,
                            random.nextDouble() * 3,
                            random.nextDouble() * 12 - 6
                        ),
                        4, 0.5, 1, 0.5, 0.5);
                }
                
                // ═════ REGALOS ENVUELTOS (Partículas de Nota) ═════
                if (random.nextDouble() < 0.1) {
                    world.spawnParticle(Particle.NOTE,
                        loc.clone().add(
                            random.nextDouble() * 8 - 4,
                            0.5,
                            random.nextDouble() * 8 - 4
                        ),
                        2, 0.5, 0.5, 0.5, 0);
                }
            }
        }, intervalo, intervalo);
    }
    
    private void iniciarSonidosAmbiente() {
        if (config == null) return;
        
        // Sonidos cada 15 segundos para ambiente constante
        int intervaloSeg = config.getInt("ambiente.sonidos.intervalo_seg", 15);
        
        ambienteSoundTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Location loc = player.getLocation();
                
                // ═════ CAMPANAS NAVIDEÑAS FESTIVAS ═════
                if (config.getBoolean("ambiente.sonidos.campanas", true)) {
                    // Campanas de iglesia lejanas
                    player.playSound(loc, Sound.BLOCK_BELL_USE, 
                        0.4f, 0.8f);
                    
                    // Campanas de trineo (jingle bells)
                    if (random.nextDouble() < 0.6) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL,
                                0.5f, 1.2f);
                        }, 10L);
                        
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL,
                                0.5f, 1.4f);
                        }, 15L);
                    }
                    
                    // Notas musicales navideñas
                    if (random.nextDouble() < 0.4) {
                        player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME,
                            0.3f, 1.0f);
                    }
                }
                
                // ═════ VIENTO INVERNAL SUAVE ═════
                if (config.getBoolean("ambiente.sonidos.viento", true) && random.nextDouble() < 0.5) {
                    player.playSound(loc, Sound.ITEM_ELYTRA_FLYING,
                        0.15f, 0.4f);
                }
                
                // ═════ SONIDOS MÁGICOS FESTIVOS ═════
                if (random.nextDouble() < 0.35) {
                    player.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                        0.25f, 1.2f);
                }
                
                // ═════ CORO NAVIDEÑO (Notas armónicas) ═════
                if (random.nextDouble() < 0.3) {
                    // Secuencia de notas armoniosas
                    player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP,
                        0.2f, 0.9f);
                    
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP,
                            0.2f, 1.2f);
                    }, 5L);
                }
                
                // ═════ FUEGOS ARTIFICIALES LEJANOS ═════
                if (random.nextDouble() < 0.15) {
                    player.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST,
                        0.3f, 0.8f + random.nextFloat() * 0.4f);
                    
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE,
                            0.25f, 1.0f);
                    }, 8L);
                }
                
                // ═════ SONIDO DE NIEVE CAYENDO (sutil) ═════
                if (random.nextDouble() < 0.2) {
                    player.playSound(loc, Sound.BLOCK_SNOW_BREAK,
                        0.1f, 0.5f);
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
    
    /**
     * Convierte al jugador que ejecuta el comando en Santa
     * @param player El jugador que será Santa
     */
    public void convertirEnSanta(Player player) {
        if (santaPlayer != null) {
            player.sendMessage("§c✦ Ya hay un Santa activo: " + santaPlayer.getName());
            return;
        }
        
        santaPlayer = player;
        santaUUID = player.getUniqueId();
        
        Location loc = player.getLocation();
        World world = loc.getWorld();
        
        if (world != null) {
            // Efectos visuales épicos
            world.spawnParticle(Particle.SNOWFLAKE, loc.clone().add(0, 1, 0), 100, 1, 2, 1, 0.1);
            world.spawnParticle(Particle.FIREWORK, loc.clone().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.1);
            world.spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 50, 1, 1, 1, 0.05);
            world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            world.playSound(loc, Sound.BLOCK_BELL_USE, 1.0f, 1.0f);
        }
        
        // Título para el jugador
        player.sendTitle("§c§l✦ HO HO HO ✦", "§eEres Santa Claus", 10, 60, 20);
        
        // Mensaje global
        String mensaje = "§c§l✦ " + player.getName() + " es ahora Santa Claus §c§l✦";
        messageBus.broadcast(mensaje, "navidad-santa-spawn");
        
        plugin.getLogger().info("[Navidad] " + player.getName() + " convertido en Santa");
    }
    
    /**
     * Spawnea muñecos de nieve decorativos alrededor de los jugadores
     * para crear ambiente navideño clásico
     */
    private void spawnearMuñecosDeNieve() {
        // Spawnear 5-8 muñecos de nieve cerca de cada jugador online
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location playerLoc = player.getLocation();
            World world = playerLoc.getWorld();
            if (world == null) continue;
            
            int cantidad = 5 + random.nextInt(4); // 5-8 muñecos por jugador
            
            for (int i = 0; i < cantidad; i++) {
                // Ubicación aleatoria alrededor del jugador (radio 8-25 bloques)
                double angle = random.nextDouble() * Math.PI * 2;
                double radius = 8 + random.nextDouble() * 17;
                double x = playerLoc.getX() + Math.cos(angle) * radius;
                double z = playerLoc.getZ() + Math.sin(angle) * radius;
                double y = world.getHighestBlockYAt((int)x, (int)z) + 1;
                
                Location snowmanLoc = new Location(world, x, y, z);
                
                // Spawnear muñeco de nieve
                Snowman snowman = (Snowman) world.spawnEntity(snowmanLoc, EntityType.SNOW_GOLEM);
                snowman.setAI(true); // AI activada para que se muevan
                snowman.setInvulnerable(true);
                snowman.setSilent(false);
                
                // Nombres festivos variados
                String[] nombres = {
                    "§f❄ Muñeco Navideño ❄",
                    "§f⛄ Frosty ⛄",
                    "§f❄ Sr. Nieve ❄",
                    "§f⛄ Guardián Invernal ⛄",
                    "§f❄ Olaf ❄"
                };
                snowman.setCustomName(nombres[random.nextInt(nombres.length)]);
                snowman.setCustomNameVisible(true);
                snowman.setPersistent(true);
                
                // Efectos de aparición festivos
                world.spawnParticle(Particle.SNOWFLAKE, snowmanLoc.clone().add(0, 1, 0), 40, 0.5, 1, 0.5, 0.08);
                world.spawnParticle(Particle.END_ROD, snowmanLoc.clone().add(0, 0.5, 0), 15, 0.3, 0.5, 0.3, 0.02);
                world.playSound(snowmanLoc, Sound.BLOCK_SNOW_PLACE, 0.6f, 0.9f);
            }
        }
        
        plugin.getLogger().info("[Navidad] Muñecos de nieve decorativos spawneados abundantemente");
    }
    
    public void quitarSanta() {
        if (santaPlayer == null) {
            plugin.getLogger().warning("[Navidad] No hay Santa activo");
            return;
        }
        
        Location loc = santaPlayer.getLocation();
        World world = loc.getWorld();
        
        if (world != null) {
            // Efectos de transición
            world.spawnParticle(Particle.SNOWFLAKE, loc.clone().add(0, 1, 0), 50, 1, 2, 1, 0.1);
            world.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.0f);
        }
        
        // Mensaje al jugador
        santaPlayer.sendTitle("§7✦", "§7Ya no eres Santa", 10, 40, 10);
        
        // Mensaje global
        String mensaje = "§7✦ " + santaPlayer.getName() + " ya no es Santa §7✦";
        messageBus.broadcast(mensaje, "navidad-santa-despawn");
        
        santaPlayer = null;
        santaUUID = null;
        
        plugin.getLogger().info("[Navidad] Santa removido");
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
        // Pensamientos del Observador desactivados
        // El admin pondrá mensajes manualmente durante el evento
        // Método mantenido para compatibilidad pero sin funcionalidad
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // CINEMÁTICA FINAL - Finalización feliz del evento
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Cinemática final feliz del evento de Navidad
     * Incluye efectos visuales, sonoros, mensajes emotivos y entrega de recompensas
     * Duración: ~45 segundos (mejorado para más dinamismo)
     */
    private void iniciarCinematicaFinal() {
        plugin.getLogger().info("[Navidad] Iniciando cinemática final feliz");
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 1: DESPEDIDA Y AGRADECIMIENTO (0-10s)
        // ═══════════════════════════════════════════════════════════════
        
        // T=0s - Anuncio inicial con efectos mejorados
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle("§c§l✦ §f§lNAVIDAD §c§l✦", "§7Llega a su fin...", 10, 60, 20);
            player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 0.8f);
            
            Location loc = player.getLocation();
            // Nieve suave cayendo + partículas festivas
            loc.getWorld().spawnParticle(Particle.SNOWFLAKE, 
                loc.clone().add(0, 5, 0), 80, 5, 3, 5, 0.05);
            loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                loc.clone().add(0, 2, 0), 20, 3, 2, 3, 0);
        }
        
        messageBus.broadcast("", "navidad-final");
        messageBus.broadcast("§c§l✦ §f§lEL EVENTO DE NAVIDAD TERMINA §c§l✦", "navidad-final");
        messageBus.broadcast("", "navidad-final");
        
        // T=3s - Mensaje emotivo con efectos de luz
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            messageBus.broadcast("§7La calma desciende una última vez...", "navidad-final-msg");
            messageBus.broadcast("§7Los recuerdos permanecen. §e✦", "navidad-final-msg");
            messageBus.broadcast("", "navidad-final-msg");
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.9f);
                Location loc = player.getLocation();
                // Luces doradas girando
                for (int i = 0; i < 20; i++) {
                    double angle = Math.toRadians(i * 18);
                    double x = Math.cos(angle) * 3;
                    double z = Math.sin(angle) * 3;
                    loc.getWorld().spawnParticle(Particle.END_ROD,
                        loc.clone().add(x, 2, z), 1, 0, 0, 0, 0.01);
                }
            }
        }, 60L);
        
        // T=7s - Agradecimiento con fuegos artificiales
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            messageBus.broadcast("§e✦ §lGracias por participar en este momento de paz §e✦", "navidad-thanks");
            messageBus.broadcast("§7Tu presencia hizo este evento especial", "navidad-thanks");
            messageBus.broadcast("", "navidad-thanks");
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
                Location loc = player.getLocation();
                
                // Explosión de felicidad
                loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                    loc.clone().add(0, 2, 0), 50, 2, 1, 2, 0);
                loc.getWorld().spawnParticle(Particle.FIREWORK,
                    loc.clone().add(0, 3, 0), 30, 2, 1, 2, 0.15);
            }
        }, 140L);
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 2: ENTREGA DE RECOMPENSAS (10-30s) - MEJORADA
        // ═══════════════════════════════════════════════════════════════
        
        // T=10s - Anuncio de recompensas con efectos épicos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle(
                    "§6§l✦ RECOMPENSAS ✦",
                    "§e¡Por tu participación!",
                    10, 70, 20
                );
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                
                Location loc = player.getLocation();
                // Explosión épica de partículas doradas
                loc.getWorld().spawnParticle(Particle.END_ROD,
                    loc.clone().add(0, 2, 0), 80, 3, 2, 3, 0.12);
                loc.getWorld().spawnParticle(Particle.FIREWORK,
                    loc.clone().add(0, 1, 0), 50, 2, 1, 2, 0.18);
                loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                    loc.clone().add(0, 2, 0), 40, 2, 2, 2, 0.1);
            }
            
            messageBus.broadcast("", "navidad-rewards");
            messageBus.broadcast("§6§l✦ ENTREGANDO RECOMPENSAS NAVIDEÑAS ✦", "navidad-rewards");
            messageBus.broadcast("§eHerramientas, armadura y materiales únicos...", "navidad-rewards");
            messageBus.broadcast("", "navidad-rewards");
        }, 200L); // 10 segundos
        
        // T=13s - Entregar recompensas a todos los jugadores
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            entregarRecompensasNavidad();
        }, 260L); // 13 segundos
        
        // T=18s - Efectos de celebración intensificados
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            messageBus.broadcast("§a§l✦ §e¡Las recompensas han sido entregadas! §a§l✦", "navidad-claim");
            messageBus.broadcast("§7Usa §e/recompensas §7para reclamarlas", "navidad-claim");
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                Location loc = player.getLocation();
                
                // Secuencia de fuegos artificiales más dinámica
                for (int i = 0; i < 12; i++) {
                    int delay = i * 3;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        // Fuegos artificiales en círculo
                        double angle = Math.toRadians(delay * 30);
                        double x = Math.cos(angle) * 5;
                        double z = Math.sin(angle) * 5;
                        
                        loc.getWorld().spawnParticle(Particle.FIREWORK,
                            loc.clone().add(x, 5 + random.nextDouble() * 3, z), 
                            20, 1, 1, 1, 0.2);
                        
                        // Variedad de sonidos festivos
                        if (delay % 2 == 0) {
                            player.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.7f, 1.0f + random.nextFloat() * 0.5f);
                        } else {
                            player.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.6f, 1.2f);
                        }
                    }, delay);
                }
                
                // Campanas celebratorias
                player.playSound(loc, Sound.BLOCK_BELL_USE, 1.0f, 1.2f);
            }
        }, 360L); // 18 segundos
        
        // T=25s - Efectos adicionales de celebración
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Location loc = player.getLocation();
                
                // Espiral ascendente de partículas de colores
                for (int i = 0; i < 30; i++) {
                    int delay = i * 2;
                    double height = i * 0.3;
                    double radius = 2 - (i * 0.05);
                    
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        double angle = Math.toRadians(delay * 20);
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        
                        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                            loc.clone().add(x, height, z), 3, 0.1, 0.1, 0.1, 0);
                        loc.getWorld().spawnParticle(Particle.END_ROD,
                            loc.clone().add(x, height, z), 2, 0.05, 0.05, 0.05, 0.01);
                    }, delay);
                }
            }
        }, 500L); // 25 segundos
        
        // ═══════════════════════════════════════════════════════════════
        // FASE 3: CELEBRACIÓN Y MENSAJE FINAL (30-45s) - MÁS DINÁMICA
        // ═══════════════════════════════════════════════════════════════
        
        // T=30s - Mensaje final emotivo
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            messageBus.broadcast("", "navidad-final2");
            messageBus.broadcast("§7El invierno seguirá su curso...", "navidad-final2");
            messageBus.broadcast("§7Pero los recuerdos de esta Navidad permanecerán.", "navidad-final2");
            messageBus.broadcast("§7Las herramientas y armadura te acompañarán en tu aventura.", "navidad-final2");
            messageBus.broadcast("", "navidad-final2");
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
            }
        }, 600L); // 30 segundos
        
        // T=35s - Título final FELIZ con efectos épicos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            messageBus.broadcast("§c§l🎄 §e§l¡FELIZ NAVIDAD A TODOS! §c§l🎄", "navidad-feliz");
            messageBus.broadcast("§6¡Nos vemos en el próximo evento!", "navidad-feliz");
            messageBus.broadcast("", "navidad-feliz");
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle(
                    "§c§l🎄 §f§lFELIZ NAVIDAD §c§l🎄",
                    "§e¡Gracias por participar!",
                    10, 80, 20
                );
                
                Location loc = player.getLocation();
                
                // EXPLOSIÓN FINAL ÉPICA de partículas navideñas
                loc.getWorld().spawnParticle(Particle.SNOWFLAKE,
                    loc.clone().add(0, 3, 0), 150, 4, 3, 4, 0.1);
                loc.getWorld().spawnParticle(Particle.END_ROD,
                    loc.clone().add(0, 2, 0), 80, 3, 2, 3, 0.1);
                loc.getWorld().spawnParticle(Particle.FIREWORK,
                    loc.clone().add(0, 1, 0), 60, 3, 2, 3, 0.2);
                loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                    loc.clone().add(0, 1, 0), 50, 2, 1, 2, 0);
                loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                    loc.clone().add(0, 2, 0), 40, 2, 2, 2, 0.05);
                
                // Sonidos finales festivos ÉPICOS
                player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.5f);
                player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                player.playSound(loc, Sound.BLOCK_BELL_USE, 1.0f, 1.0f);
                player.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.8f, 1.2f);
                
                // Secuencia de notas musicales navideñas mejorada
                float[] notas = {1.0f, 1.2f, 1.5f, 1.8f, 2.0f};
                for (int i = 0; i < notas.length; i++) {
                    int delay = i * 5;
                    float nota = notas[i];
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, nota);
                        player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, nota);
                    }, delay);
                }
            }
        }, 700L); // 35 segundos
        
        // T=40s - Efectos finales continuados
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Location loc = player.getLocation();
                
                // Lluvia de estrellas final
                for (int i = 0; i < 20; i++) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        loc.getWorld().spawnParticle(Particle.END_ROD,
                            loc.clone().add(
                                random.nextDouble() * 8 - 4,
                                8,
                                random.nextDouble() * 8 - 4
                            ), 1, 0, -2, 0, 0.1);
                    }, i * 2L);
                }
            }
        }, 800L); // 40 segundos
        
        // T=45s - Cleanup final y mensaje de despedida
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String mensajeFin = config != null ?
                config.getString("mensajes.fin", "§7El mundo vuelve a la normalidad.") :
                "§7El mundo vuelve a la normalidad.";
            
            messageBus.broadcast(mensajeFin, "navidad-fin");
            messageBus.broadcast("§7¡Que las recompensas te acompañen en tu aventura!", "navidad-fin");
            messageBus.broadcast("", "navidad-fin");
            
            // Limpiar todo después de la cinemática
            cleanup();
            
            plugin.getLogger().info("[Navidad] Cinemática final completada - Evento finalizado");
        }, 900L); // 45 segundos
    }
    
    /**
     * Entrega las recompensas navideñas a todos los jugadores online
     * usando el sistema RewardClaimSystem
     * Incluye: Herramientas únicas, Armadura de Santa, Materiales y XP de Rango
     */
    private void entregarRecompensasNavidad() {
        plugin.getLogger().info("[Navidad] Entregando recompensas navideñas a jugadores");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                // ═══════════════════════════════════════════════════════════
                // XP DE RANGO - 1000 XP (aumentado de 500)
                // ═══════════════════════════════════════════════════════════
                if (plugin.getExperienceService() != null) {
                    plugin.getExperienceService().addXP(player, 1000, "evento_navidad", false);
                    player.sendMessage("§a§l✦ §e+1000 XP de Rango recibidos");
                }
                
                // ═══════════════════════════════════════════════════════════
                // RECOMPENSAS TEMÁTICAS - Herramientas y Armadura
                // ═══════════════════════════════════════════════════════════
                if (plugin.getRewardClaimSystem() != null) {
                    // Obtener todas las recompensas temáticas desde NavidadRewards
                    List<org.bukkit.inventory.ItemStack> allRewards = NavidadRewards.obtenerTodasLasRecompensas();
                    
                    // Añadir paquete de recompensas al sistema
                    plugin.getRewardClaimSystem().addRewards(
                        player.getUniqueId(),
                        "navidad",
                        "§c§l✦ Recompensas Navideñas §c§l✦",
                        allRewards,
                        60, // 60 minutos = 1 hora para reclamar
                        "SPECIAL",
                        0
                    );
                    
                    // Mensaje detallado de recompensas
                    player.sendMessage("");
                    player.sendMessage("§8§m═══════════════════════════════════════════");
                    player.sendMessage("");
                    player.sendMessage("     §c§l🎄 §f§lRECOMPENSAS NAVIDEÑAS §c§l🎄");
                    player.sendMessage("");
                    player.sendMessage("§7Has recibido recompensas especiales:");
                    player.sendMessage("");
                    player.sendMessage("§b§l⚒ HERRAMIENTAS DEL INVIERNO:");
                    player.sendMessage("  §8▪ §b❄ Pico de Escarcha §7(Eficiencia V, Fortuna III)");
                    player.sendMessage("  §8▪ §c🎄 Hacha de Santa §7(Eficiencia V, Filo V)");
                    player.sendMessage("  §8▪ §6⭐ Pala del Regalo §7(Eficiencia V, Fortuna III)");
                    player.sendMessage("  §8▪ §f❄ Espada del Invierno Eterno §7(Filo V)");
                    player.sendMessage("");
                    player.sendMessage("§c§l⚔ ARMADURA DE SANTA:");
                    player.sendMessage("  §8▪ §c🎅 Gorro de Santa §7(Protección IV)");
                    player.sendMessage("  §8▪ §c🎁 Traje de Santa §7(Protección IV, Espinas III)");
                    player.sendMessage("  §8▪ §c🎄 Pantalones de Santa §7(Protección IV)");
                    player.sendMessage("  §8▪ §c❄ Botas de Santa §7(Protección IV, Caída IV)");
                    player.sendMessage("");
                    player.sendMessage("§e§l💎 MATERIALES ÚTILES:");
                    player.sendMessage("  §8▪ §b16 Diamantes, 12 Esmeraldas, 1 Netherite");
                    player.sendMessage("  §8▪ §610 Manzanas Doradas, 3 Manzanas Encantadas");
                    player.sendMessage("  §8▪ §d2 Tótems de Inmortalidad");
                    player.sendMessage("  §8▪ §f32 Perlas de Ender, 32 Botellas de XP");
                    player.sendMessage("  §8▪ §7Bloques decorativos y comida temática");
                    player.sendMessage("");
                    player.sendMessage("§7Reclama tus recompensas con §e/recompensas");
                    player.sendMessage("§7§oTienes §e1 hora §7§opara reclamarlas");
                    player.sendMessage("");
                    player.sendMessage("§a§l✦ §eÚsalas sabiamente en el próximo evento");
                    player.sendMessage("");
                    player.sendMessage("§8§m═══════════════════════════════════════════");
                    player.sendMessage("");
                }
                
                // ═══════════════════════════════════════════════════════════
                // EFECTOS VISUALES Y SONOROS
                // ═══════════════════════════════════════════════════════════
                Location loc = player.getLocation();
                
                // Partículas celebratorias intensas
                loc.getWorld().spawnParticle(Particle.FIREWORK,
                    loc.clone().add(0, 1, 0), 50, 2, 2, 2, 0.2);
                loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                    loc.clone().add(0, 2, 0), 40, 1, 1, 1, 0);
                loc.getWorld().spawnParticle(Particle.END_ROD,
                    loc.clone().add(0, 2, 0), 30, 1.5, 1.5, 1.5, 0.1);
                loc.getWorld().spawnParticle(Particle.SNOWFLAKE,
                    loc.clone().add(0, 3, 0), 60, 2, 2, 2, 0.05);
                
                // Sonidos festivos
                player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                player.playSound(loc, Sound.BLOCK_BELL_USE, 0.8f, 1.5f);
                
                // Título en pantalla
                player.sendTitle(
                    "§c§l🎁 §f§lRECOMPENSAS §c§l🎁",
                    "§eRecibidas exitosamente",
                    10, 60, 20
                );
                
            } catch (Exception e) {
                plugin.getLogger().severe("[Navidad] Error entregando recompensas a " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        plugin.getLogger().info("[Navidad] Recompensas entregadas a " + Bukkit.getOnlinePlayers().size() + " jugadores");
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
        
        // Quitar Santa si existe (ahora es un jugador, no una entidad)
        if (santaPlayer != null) {
            quitarSanta();
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
        return santaPlayer != null && santaPlayer.isOnline();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE AMIGO SECRETO - Intercambio de Regalos
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Inicia el sorteo de amigo secreto
     * Asigna a cada jugador online otro jugador aleatorio para darle regalos
     */
    public void iniciarAmigoSecreto() {
        if (amigoSecretoActivo) {
            plugin.getLogger().warning("[Navidad] El amigo secreto ya está activo");
            return;
        }
        
        List<Player> jugadores = new ArrayList<>(Bukkit.getOnlinePlayers());
        
        // Verificar mínimo de jugadores
        int minJugadores = config != null ? config.getInt("amigo_secreto.jugadores_minimos", 2) : 2;
        if (jugadores.size() < minJugadores) {
            messageBus.broadcast("§c✦ Se necesitan al menos " + minJugadores + " jugadores para el amigo secreto.", "navidad-amigo-secreto");
            return;
        }
        
        // Limpiar datos anteriores
        asignacionesAmigoSecreto.clear();
        regalosEntregados.clear();
        regalosRecibidos.clear();
        contadorRecordatorios = 0;
        
        // Crear lista de receptores (shuffle)
        List<Player> receptores = new ArrayList<>(jugadores);
        Collections.shuffle(receptores);
        
        // Asignar cada jugador a otro (asegurando que nadie se tenga a sí mismo)
        for (int i = 0; i < jugadores.size(); i++) {
            Player dador = jugadores.get(i);
            Player receptor = receptores.get(i);
            
            // Si alguien se asignó a sí mismo, intercambiar con el siguiente
            if (dador.equals(receptor)) {
                int nextIndex = (i + 1) % jugadores.size();
                Player temp = receptores.get(nextIndex);
                receptores.set(nextIndex, receptor);
                receptor = temp;
            }
            
            asignacionesAmigoSecreto.put(dador.getUniqueId(), receptor.getUniqueId());
            regalosEntregados.put(dador.getUniqueId(), 0);
            regalosRecibidos.put(receptor.getUniqueId(), 0);
        }
        
        amigoSecretoActivo = true;
        
        // Anuncio público
        String mensajeSorteo = config != null ? 
            config.getString("amigo_secreto.mensajes.sorteo_iniciado", "§c§l🎄 AMIGO SECRETO NAVIDEÑO §c§l🎄") : 
            "§c§l🎄 AMIGO SECRETO NAVIDEÑO §c§l🎄";
        String mensajeDesc = config != null ?
            config.getString("amigo_secreto.mensajes.sorteo_descripcion", "§7Se ha realizado el sorteo. ¡Revisa tu chat!") :
            "§7Se ha realizado el sorteo. ¡Revisa tu chat!";
            
        messageBus.broadcast("", "navidad-sorteo");
        messageBus.broadcast(mensajeSorteo, "navidad-sorteo");
        messageBus.broadcast(mensajeDesc, "navidad-sorteo");
        messageBus.broadcast("", "navidad-sorteo");
        
        // Notificación privada a cada jugador
        int cantidadRegalos = config != null ? config.getInt("amigo_secreto.regalos_requeridos", 2) : 2;
        
        for (Player dador : jugadores) {
            UUID receptorUUID = asignacionesAmigoSecreto.get(dador.getUniqueId());
            Player receptor = Bukkit.getPlayer(receptorUUID);
            
            if (receptor != null) {
                String mensajePrivado = config != null ?
                    config.getString("amigo_secreto.mensajes.asignacion_privada", "")
                        .replace("{jugador}", receptor.getName())
                        .replace("{cantidad}", String.valueOf(cantidadRegalos)) :
                    "§c§lTu amigo secreto es: §e§l" + receptor.getName();
                
                dador.sendMessage("");
                dador.sendMessage(mensajePrivado);
                dador.sendMessage("");
                
                // Instrucciones claras
                dador.sendMessage("§e§l▸ INSTRUCCIONES:");
                dador.sendMessage("§71. §fAcércate a §e" + receptor.getName());
                dador.sendMessage("§72. §fPon el item en tu §emano principal");
                dador.sendMessage("§73. §fUsa: §a/avo navidad entregar");
                dador.sendMessage("");
                
                // Efectos
                dador.playSound(dador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                dador.playSound(dador.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.2f);
                
                Location loc = dador.getLocation();
                loc.getWorld().spawnParticle(Particle.HEART, loc.clone().add(0, 2, 0), 10, 0.5, 0.5, 0.5, 0);
            }
        }
        
        // Iniciar recordatorios
        iniciarRecordatorios();
        
        plugin.getLogger().info("[Navidad] Amigo secreto iniciado con " + jugadores.size() + " participantes");
    }
    
    /**
     * Sistema de recordatorios para entregar regalos
     */
    private void iniciarRecordatorios() {
        if (!config.getBoolean("amigo_secreto.recordatorios.enabled", true)) return;
        
        int intervaloMin = config.getInt("amigo_secreto.recordatorios.intervalo_minutos", 10);
        int maxRecordatorios = config.getInt("amigo_secreto.recordatorios.max_recordatorios", 3);
        
        recordatorioTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!amigoSecretoActivo || contadorRecordatorios >= maxRecordatorios) {
                if (recordatorioTask != null) recordatorioTask.cancel();
                return;
            }
            
            int cantidadRequerida = config.getInt("amigo_secreto.regalos_requeridos", 2);
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                if (!asignacionesAmigoSecreto.containsKey(uuid)) continue;
                
                int entregados = regalosEntregados.getOrDefault(uuid, 0);
                if (entregados >= cantidadRequerida) continue; // Ya completó
                
                UUID receptorUUID = asignacionesAmigoSecreto.get(uuid);
                Player receptor = Bukkit.getPlayer(receptorUUID);
                
                if (receptor != null) {
                    // Mensaje más cálido del config
                    String recordatorio = config.getString("amigo_secreto.mensajes.recordatorio", 
                        "§e§l✦ Recordatorio Navideño ✦\n\nTu amigo secreto aún espera tu gesto.\nNo olvides compartir un poco de ti.");
                    
                    player.sendMessage("");
                    player.sendMessage(recordatorio);
                    player.sendMessage("");
                    player.sendMessage("§7(Comando: §a/avo navidad entregar§7)");
                    player.sendMessage("");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
                }
            }
            
            contadorRecordatorios++;
        }, intervaloMin * 60 * 20L, intervaloMin * 60 * 20L);
    }
    
    /**
     * Regala un item a cualquier jugador (no necesariamente el amigo secreto)
     * Generosidad extra voluntaria
     */
    public void regalarAJugador(Player dador, Player receptor, String mensajePersonal) {
        if (!eventoActivo) {
            dador.sendMessage("§c✦ El evento de Navidad no está activo.");
            return;
        }
        
        if (dador.equals(receptor)) {
            dador.sendMessage("§c✦ No puedes regalarte a ti mismo.");
            return;
        }
        
        // Verificar distancia (deben estar cerca)
        if (dador.getLocation().distance(receptor.getLocation()) > 10) {
            dador.sendMessage("§c✦ Debes estar cerca de §e" + receptor.getName() + " §cpara entregarle el regalo.");
            return;
        }
        
        // Obtener item de la mano del jugador
        ItemStack regalo = dador.getInventory().getItemInMainHand();
        if (regalo == null || regalo.getType() == Material.AIR) {
            dador.sendMessage("§c✦ Debes tener un item en tu mano principal.");
            dador.sendMessage("§7Pon el regalo que quieres dar en tu mano.");
            return;
        }
        
        // Clonar el item para el receptor
        ItemStack regaloClonado = regalo.clone();
        
        // Calcular valor del regalo (suma al total si amigo secreto está activo)
        double valorRegalo = calcularValorItem(regalo);
        
        // Remover item de la mano del dador
        dador.getInventory().setItemInMainHand(null);
        
        // Dar el regalo al receptor
        receptor.getInventory().addItem(regaloClonado);
        
        // Si el amigo secreto está activo, sumar al valor total
        UUID dadorUUID = dador.getUniqueId();
        if (amigoSecretoActivo) {
            double valorTotal = valorTotalRegalos.getOrDefault(dadorUUID, 0.0) + valorRegalo;
            valorTotalRegalos.put(dadorUUID, valorTotal);
            
            // Actualizar recompensas pendientes
            double multiplicador = calcularMultiplicadorRecompensa(valorTotal);
            int xpBase = config.getInt("amigo_secreto.recompensas.base.xp", 150);
            int fragmentosBase = config.getInt("amigo_secreto.recompensas.base.fragmentos", 2);
            
            int xpFinal = (int)(xpBase * multiplicador);
            int fragmentosFinal = (int)(fragmentosBase * multiplicador);
            
            recompensasXPPendientes.put(dadorUUID, xpFinal);
            recompensasFragmentosPendientes.put(dadorUUID, fragmentosFinal);
        }
        
        // Mensajes discretos
        String itemName = regalo.hasItemMeta() && regalo.getItemMeta().hasDisplayName() 
            ? regalo.getItemMeta().getDisplayName() 
            : "§f" + regalo.getType().name().toLowerCase().replace("_", " ");
        
        dador.sendMessage("");
        dador.sendMessage("§a§l✦ Regalo Entregado ✦");
        dador.sendMessage("");
        dador.sendMessage("§7Has compartido tu regalo con §e" + receptor.getName());
        dador.sendMessage("§7Un gesto de generosidad voluntaria.");
        dador.sendMessage("");
        
        receptor.sendMessage("");
        receptor.sendMessage("§d§l✦ ¡Has recibido un regalo! ✦");
        receptor.sendMessage("");
        if (mensajePersonal != null && !mensajePersonal.trim().isEmpty()) {
            receptor.sendMessage("§e" + dador.getName() + " §7te ha dado:");
            receptor.sendMessage("§7" + itemName);
            receptor.sendMessage("§8Mensaje: §f\"" + mensajePersonal + "\"");
        } else {
            receptor.sendMessage("§e" + dador.getName() + " §7te ha dado:");
            receptor.sendMessage("§7" + itemName);
        }
        receptor.sendMessage("");
        receptor.sendMessage("§c❤ §7Qué gesto tan hermoso");
        receptor.sendMessage("");
        
        // Efectos visuales
        Location locDador = dador.getLocation();
        Location locReceptor = receptor.getLocation();
        
        locDador.getWorld().spawnParticle(Particle.HEART, locDador.clone().add(0, 2, 0), 15, 0.5, 0.5, 0.5, 0);
        locReceptor.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, locReceptor.clone().add(0, 2, 0), 20, 0.5, 0.5, 0.5, 0);
        
        dador.playSound(locDador, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        receptor.playSound(locReceptor, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
        
        plugin.getLogger().info("[Navidad] " + dador.getName() + " regaló a " + receptor.getName() + " (voluntario)");
    }
    
    /**
     * Entrega un regalo al amigo secreto asignado
     */
    public void entregarRegaloAmigoSecreto(Player dador) {
        entregarRegaloAmigoSecreto(dador, null);
    }
    
    /**
     * Entrega un regalo al amigo secreto asignado con mensaje opcional
     */
    public void entregarRegaloAmigoSecreto(Player dador, String mensajePersonal) {
        if (!amigoSecretoActivo) {
            dador.sendMessage("§c✦ El amigo secreto no está activo.");
            return;
        }
        
        UUID dadorUUID = dador.getUniqueId();
        if (!asignacionesAmigoSecreto.containsKey(dadorUUID)) {
            dador.sendMessage("§c✦ No estás participando en el amigo secreto.");
            return;
        }
        
        int cantidadRequerida = config.getInt("amigo_secreto.regalos_requeridos", 2);
        int entregados = regalosEntregados.getOrDefault(dadorUUID, 0);
        
        // Permitir dar más de lo requerido (generosidad extra)
        // No hay límite, solo informar si ya cumplieron el mínimo
        if (entregados >= cantidadRequerida) {
            dador.sendMessage("§e✦ Ya completaste el mínimo requerido.");
            dador.sendMessage("§7Pero puedes seguir dando si tu corazón lo desea...");
            dador.sendMessage("");
        }
        
        UUID receptorUUID = asignacionesAmigoSecreto.get(dadorUUID);
        Player receptor = Bukkit.getPlayer(receptorUUID);
        
        if (receptor == null || !receptor.isOnline()) {
            dador.sendMessage("§c✦ Tu amigo secreto no está online.");
            return;
        }
        
        // Verificar distancia (deben estar cerca)
        if (dador.getLocation().distance(receptor.getLocation()) > 10) {
            dador.sendMessage("§c✦ Debes estar cerca de §e" + receptor.getName() + " §cpara entregarle el regalo.");
            return;
        }
        
        // Obtener item de la mano del jugador
        ItemStack regalo = dador.getInventory().getItemInMainHand();
        if (regalo == null || regalo.getType() == Material.AIR) {
            dador.sendMessage("§c✦ Debes tener un item en tu mano principal.");
            dador.sendMessage("§7Pon el regalo que quieres dar en tu mano.");
            return;
        }
        
        // Clonar el item para el receptor
        ItemStack regaloClonado = regalo.clone();
        
        // Calcular valor del regalo
        double valorRegalo = calcularValorItem(regalo);
        
        // Remover item de la mano del dador
        dador.getInventory().setItemInMainHand(null);
        
        // Dar el regalo al receptor
        receptor.getInventory().addItem(regaloClonado);
        
        // Actualizar contadores
        entregados++;
        regalosEntregados.put(dadorUUID, entregados);
        int recibidos = regalosRecibidos.getOrDefault(receptorUUID, 0) + 1;
        regalosRecibidos.put(receptorUUID, recibidos);
        
        // Actualizar valor total (silenciosamente)
        double valorTotal = valorTotalRegalos.getOrDefault(dadorUUID, 0.0) + valorRegalo;
        valorTotalRegalos.put(dadorUUID, valorTotal);
        
        // Mensajes discretos sin mostrar valores
        String itemName = regalo.hasItemMeta() && regalo.getItemMeta().hasDisplayName() 
            ? regalo.getItemMeta().getDisplayName() 
            : "§f" + regalo.getType().name().toLowerCase().replace("_", " ");
        
        dador.sendMessage("");
        dador.sendMessage("§a§l✦ Regalo Entregado ✦");
        dador.sendMessage("");
        dador.sendMessage("§7Has entregado tu regalo con el corazón.");
        dador.sendMessage("§e" + receptor.getName() + " §7lo apreciará mucho.");
        if (entregados < cantidadRequerida) {
            int faltan = cantidadRequerida - entregados;
            dador.sendMessage("");
            dador.sendMessage("§7Aún te falta" + (faltan > 1 ? "n §c" + faltan + " regalos" : " §c1 regalo") + "§7.");
        } else if (entregados == cantidadRequerida) {
            dador.sendMessage("");
            dador.sendMessage("§a✓ Has completado tu intercambio.");
            dador.sendMessage("§7Santa te recompensará al finalizar el evento.");
        }
        dador.sendMessage("");
        
        receptor.sendMessage("");
        receptor.sendMessage("§d§l✦ ¡Has recibido un regalo! ✦");
        receptor.sendMessage("");
        if (mensajePersonal != null && !mensajePersonal.trim().isEmpty()) {
            receptor.sendMessage("§7" + itemName);
            receptor.sendMessage("§8Mensaje: §f\"" + mensajePersonal + "\"");
        } else {
            receptor.sendMessage("§7Alguien te ha dado: " + itemName);
        }
        receptor.sendMessage("");
        receptor.sendMessage("§c❤ §7Qué gesto tan hermoso");
        receptor.sendMessage("");
        
        // Efectos visuales
        Location locDador = dador.getLocation();
        Location locReceptor = receptor.getLocation();
        
        locDador.getWorld().spawnParticle(Particle.HEART, locDador.clone().add(0, 2, 0), 15, 0.5, 0.5, 0.5, 0);
        locReceptor.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, locReceptor.clone().add(0, 2, 0), 20, 0.5, 0.5, 0.5, 0);
        
        dador.playSound(locDador, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        receptor.playSound(locReceptor, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
        
        // Actualizar recompensas pendientes para el final
        double multiplicador = calcularMultiplicadorRecompensa(valorTotal);
        int xpBase = config.getInt("amigo_secreto.recompensas.base.xp", 150);
        int fragmentosBase = config.getInt("amigo_secreto.recompensas.base.fragmentos", 2);
        
        int xpFinal = (int)(xpBase * multiplicador);
        int fragmentosFinal = (int)(fragmentosBase * multiplicador);
        
        // Guardar para entregar al final del evento
        recompensasXPPendientes.put(dadorUUID, xpFinal);
        recompensasFragmentosPendientes.put(dadorUUID, fragmentosFinal);
        
        // Si completó el mínimo requerido, verificar completación global
        if (entregados == cantidadRequerida) {
            verificarCompletacionTotal();
        }
        
        plugin.getLogger().info("[Navidad] " + dador.getName() + " entregó regalo " + entregados + "/" + cantidadRequerida + " a " + receptor.getName());
    }
    
    /**
     * Calcula el valor de un item para el sistema de regalos
     */
    private double calcularValorItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 0.0;
        
        Material material = item.getType();
        int cantidad = item.getAmount();
        double valorUnitario = 1.0;
        
        // Obtener valores desde config o usar defaults
        ConfigurationSection valoresConfig = config.getConfigurationSection("amigo_secreto.valores_items");
        
        // Materiales preciosos
        if (material == Material.NETHERITE_INGOT) valorUnitario = 100.0;
        else if (material == Material.DIAMOND) valorUnitario = 50.0;
        else if (material == Material.EMERALD) valorUnitario = 40.0;
        else if (material == Material.GOLD_INGOT) valorUnitario = 25.0;
        else if (material == Material.IRON_INGOT) valorUnitario = 10.0;
        
        // Items encantados tienen bonus
        else if (material.name().contains("DIAMOND_") && material.name().contains("_")) valorUnitario = 45.0;
        else if (material.name().contains("NETHERITE_") && material.name().contains("_")) valorUnitario = 95.0;
        else if (material.name().contains("GOLDEN_")) valorUnitario = 20.0;
        else if (material.name().contains("IRON_")) valorUnitario = 8.0;
        
        // Bloques especiales
        else if (material == Material.BEACON) valorUnitario = 200.0;
        else if (material == Material.ANCIENT_DEBRIS) valorUnitario = 150.0;
        else if (material == Material.NETHERITE_BLOCK) valorUnitario = 900.0;
        else if (material == Material.DIAMOND_BLOCK) valorUnitario = 450.0;
        else if (material == Material.EMERALD_BLOCK) valorUnitario = 360.0;
        else if (material == Material.GOLD_BLOCK) valorUnitario = 225.0;
        
        // Comida valiosa
        else if (material == Material.GOLDEN_APPLE) valorUnitario = 30.0;
        else if (material == Material.ENCHANTED_GOLDEN_APPLE) valorUnitario = 100.0;
        
        // Pociones y experiencia
        else if (material == Material.EXPERIENCE_BOTTLE) valorUnitario = 15.0;
        else if (material.name().contains("POTION")) valorUnitario = 12.0;
        
        // Items comunes
        else if (material.name().contains("_SWORD") || material.name().contains("_AXE") 
                || material.name().contains("_PICKAXE") || material.name().contains("_SHOVEL")) valorUnitario = 5.0;
        else if (material.isEdible()) valorUnitario = 2.0;
        
        // Bonus por encantamientos
        if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
            int nivelEncantamientos = item.getItemMeta().getEnchants().values().stream()
                .mapToInt(Integer::intValue).sum();
            valorUnitario *= (1.0 + (nivelEncantamientos * 0.2));
        }
        
        // Aplicar cantidad
        return valorUnitario * cantidad;
    }
    
    /**
     * Obtiene la categoría del regalo basado en su valor
     */
    private String obtenerCategoriaRegalo(double valor) {
        if (valor >= 100.0) return "§6§l✦✦✦ LEGENDARIO";
        if (valor >= 50.0) return "§5§l✦✦ ÉPICO";
        if (valor >= 20.0) return "§b§l✦ RARO";
        if (valor >= 5.0) return "§a§l✦ POCO COMÚN";
        return "§f✦ COMÚN";
    }
    
    /**
     * Calcula el multiplicador de recompensa según valor total
     */
    private double calcularMultiplicadorRecompensa(double valorTotal) {
        if (valorTotal >= 200.0) return 3.0;  // x3 recompensas
        if (valorTotal >= 100.0) return 2.5;  // x2.5
        if (valorTotal >= 50.0) return 2.0;   // x2
        if (valorTotal >= 20.0) return 1.5;   // x1.5
        if (valorTotal >= 10.0) return 1.2;   // x1.2
        return 1.0;                           // x1 (base)
    }
    
    /**
     * Obtiene el tier de recompensa según valor total
     */
    private String obtenerTierRecompensa(double valorTotal) {
        if (valorTotal >= 200.0) return "§6§l✦✦✦ BENEFACTOR LEGENDARIO";
        if (valorTotal >= 100.0) return "§5§l✦✦ GENEROSO ÉPICO";
        if (valorTotal >= 50.0) return "§b§l✦ AMIGO GENEROSO";
        if (valorTotal >= 20.0) return "§a✦ BUEN COMPAÑERO";
        if (valorTotal >= 10.0) return "§f✦ PARTICIPANTE";
        return "§7✦ MODESTO";
    }
    
    /**
     * Verifica si todos los jugadores completaron el intercambio
     */
    private void verificarCompletacionTotal() {
        int cantidadRequerida = config.getInt("amigo_secreto.regalos_requeridos", 2);
        
        for (UUID uuid : asignacionesAmigoSecreto.keySet()) {
            int entregados = regalosEntregados.getOrDefault(uuid, 0);
            if (entregados < cantidadRequerida) {
                return; // Alguien aún no completó
            }
        }
        
        // ¡Todos completaron!
        String mensajeTodos = config.getString("amigo_secreto.mensajes.todos_completaron", "");
        messageBus.broadcast("", "navidad-amigo-secreto-completo");
        messageBus.broadcast(mensajeTodos, "navidad-amigo-secreto-completo");
        messageBus.broadcast("", "navidad-amigo-secreto-completo");
        
        // Efectos épicos para todos
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location loc = player.getLocation();
            loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 2, 0), 50, 2, 2, 2, 0.2);
            loc.getWorld().spawnParticle(Particle.HEART, loc.clone().add(0, 2, 0), 30, 1, 1, 1, 0);
            player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        }
        
        plugin.getLogger().info("[Navidad] ¡Todos completaron el intercambio de amigo secreto!");
    }
    
    /**
     * Detiene el sistema de amigo secreto y entrega recompensas pendientes
     */
    public void detenerAmigoSecreto() {
        entregarRecompensasPendientes();
        
        amigoSecretoActivo = false;
        if (recordatorioTask != null) {
            recordatorioTask.cancel();
            recordatorioTask = null;
        }
        
        // Limpiar datos
        asignacionesAmigoSecreto.clear();
        regalosEntregados.clear();
        regalosRecibidos.clear();
        valorTotalRegalos.clear();
        recompensasXPPendientes.clear();
        recompensasFragmentosPendientes.clear();
        contadorRecordatorios = 0;
        
        plugin.getLogger().info("[Navidad] Amigo secreto detenido");
    }
    
    /**
     * Entrega todas las recompensas pendientes del amigo secreto
     * Se llama cuando el evento termina
     */
    public void entregarRecompensasPendientes() {
        if (recompensasXPPendientes.isEmpty() && recompensasFragmentosPendientes.isEmpty()) {
            return; // No hay recompensas pendientes
        }
        
        // Mensaje global de Santa - Apoyo espiritual y unión
        messageBus.broadcast("", "navidad-recompensas");
        messageBus.broadcast("§c§l✦ ═══════════════════════════ ✦", "navidad-recompensas");
        messageBus.broadcast("§f§l        SANTA CLAUS", "navidad-recompensas");
        messageBus.broadcast("§c§l✦ ═══════════════════════════ ✦", "navidad-recompensas");
        messageBus.broadcast("", "navidad-recompensas");
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Map.Entry<UUID, Integer> entry : recompensasXPPendientes.entrySet()) {
                UUID uuid = entry.getKey();
                Player player = Bukkit.getPlayer(uuid);
                
                if (player != null && player.isOnline()) {
                    int xp = entry.getValue();
                    int fragmentos = recompensasFragmentosPendientes.getOrDefault(uuid, 0);
                    double valorTotal = valorTotalRegalos.getOrDefault(uuid, 0.0);
                    
                    // Dar recompensas
                    if (plugin.getExperienceService() != null) {
                        plugin.getExperienceService().addXP(player, xp, "amigo_secreto", false);
                    }
                    if (fragmentos > 0) {
                        darFragmentos(player, fragmentos);
                    }
                    
                    // Mensaje personal cálido y esperanzador
                    player.sendMessage("");
                    player.sendMessage("§c§l✦ Un regalo de Santa ✦");
                    player.sendMessage("");
                    player.sendMessage("§7" + player.getName() + ", he visto tu generosidad.");
                    player.sendMessage("§7Sigue siendo capaz de superarte.");
                    player.sendMessage("§7Alcanzarás tus metas.");
                    player.sendMessage("");
                    player.sendMessage("§7  • §b+" + xp + " XP");
                    player.sendMessage("§7  • §d+" + fragmentos + " Fragmentos");
                    player.sendMessage("");
                    player.sendMessage("§c❤ §7Este es el camino. Juntos.");
                    player.sendMessage("");
                    
                    // Efectos especiales para los más generosos
                    double multiplicador = calcularMultiplicadorRecompensa(valorTotal);
                    if (multiplicador >= 2.0) {
                        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    }
                    
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                }
            }
            
            plugin.getLogger().info("[Navidad] Recompensas de amigo secreto entregadas a " + recompensasXPPendientes.size() + " jugadores");
        }, 60L); // 3 segundos después del mensaje
    }
    
    public boolean isAmigoSecretoActivo() {
        return amigoSecretoActivo;
    }
    
    public Map<UUID, UUID> getAsignacionesAmigoSecreto() {
        return new HashMap<>(asignacionesAmigoSecreto);
    }
}

