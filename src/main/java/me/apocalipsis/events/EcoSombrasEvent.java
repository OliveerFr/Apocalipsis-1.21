package me.apocalipsis.events;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;
import me.apocalipsis.events.gameplay.QTESystem;
import me.apocalipsis.events.gameplay.TelegraphedAttack;
import me.apocalipsis.events.gameplay.EventAudioSystem;
import me.apocalipsis.events.gameplay.EnvironmentSystem;
import me.apocalipsis.events.gameplay.DialogSystem;
import me.apocalipsis.events.gameplay.LoreSystem;
import me.apocalipsis.events.gameplay.ChoiceSystem;
import me.apocalipsis.events.gameplay.ProtectionSystem;
import me.apocalipsis.events.gameplay.SpectatorSystem;
import me.apocalipsis.events.gameplay.DifficultyScaler;

/**
 * El Eco de las Sombras Largas - Evento cinematográfico de 2-3 horas
 * 
 * Contexto narrativo:
 * Un eco desconocido se ha registrado. Las sombras se mueven solas,
 * se alargan, buscan forma. El Observador percibe algo que viene
 * "de más lejos" - algo que no debería existir.
 * 
 * "El mundo no recuerda así. Esto viene de más lejos." — El Observador
 * 
 * Actos del evento:
 * 0. ACTIVACIÓN (1 min): Oscurecimiento, mensaje inicial
 * 1. MANCHAS (15 min): Sombras pequeñas que huyen
 * 2. SOMBRAS LARGAS (20 min): Mobs silenciosos aparecen
 * 3. NÚCLEO (20 min): Entidad flotante que se teleporta
 * 4. ANCLAS (15 min): Sellar 5 anclas con fragmentos
 * 5. RITUAL (30 min): Arena + oleadas + Guardián boss
 * 6. CLIFFHANGER (2 min): Símbolo misterioso + figura en horizonte
 */
public class EcoSombrasEvent extends EventBase {
    
    // ═══════════════════════════════════════════════════════════════════
    // ESTADO DEL EVENTO
    // ═══════════════════════════════════════════════════════════════════
    
    public enum Acto {
        ACTIVACION,         // Acto 0
        MANCHAS,            // Acto 1
        SOMBRAS_LARGAS,     // Acto 2
        NUCLEO,             // Acto 3
        ANCLAS,             // Acto 4
        RITUAL,             // Acto 5
        CLIFFHANGER         // Acto 6
    }
    
    private Acto actoActual;
    private int ticksEnActo;
    private int ticksTotales;
    
    // Tracking de progreso por acto
    private int manchasActivas = 0;
    private int sombrasLargasMuertas = 0;
    private Location nucleoLocation;
    private Entity nucleoEntity;
    private int nucleoTeleportes = 0;
    private double nucleoVidaActual = 0;
    
    private List<Location> anclaLocations = new ArrayList<>();
    private Set<Integer> anclasSelladas = new HashSet<>();
    
    private Location arenaCenter;
    private int oleadaActual = 0;
    private boolean guardianSpawneado = false;
    private Entity guardianEntity;
    private boolean guardianDerrotado = false; // 🔧 FIX #13: Flag para evitar múltiples triggers
    
    // Tracking de participación para recompensas
    private Map<UUID, Integer> participacionSombras = new HashMap<>();
    private Map<UUID, Integer> participacionAnclas = new HashMap<>();
    private Map<UUID, Boolean> participacionGuardian = new HashMap<>();
    private Set<UUID> participantesOriginales = new HashSet<>();
    
    // Configuración del evento
    private FileConfiguration config;
    
    // Tareas programadas
    private BukkitTask mainTask;
    private BukkitTask manchasTask;
    private BukkitTask spawnTask;
    private BukkitTask oleadaTask;
    private BukkitTask itemSupplyTask; // 🔧 FIX: Task para suministro de items
    
    // Entidades del evento
    private Set<UUID> entidadesEvento = new HashSet<>();
    private List<Location> manchasLocations = new ArrayList<>();
    
    private final Random random = new Random();
    private EcoSombrasListener listener;
    private EcoSombrasItems items;
    
    // Sistemas de gameplay interactivo
    private QTESystem qteSystem;
    private TelegraphedAttack telegraphedAttack;
    private Map<UUID, Integer> playerQTEScores = new HashMap<>();
    
    // Sistemas avanzados de UI y feedback
    private me.apocalipsis.ui.UIManager uiManager;
    private me.apocalipsis.ui.FeedbackSystem feedbackSystem;
    private me.apocalipsis.events.gameplay.GuardianPhaseSystem guardianPhaseSystem;
    private me.apocalipsis.events.gameplay.ParticleEffectSystem particleSystem;
    
    // Sistema de recompensas y dificultad
    private me.apocalipsis.events.gameplay.EventLootSystem lootSystem;
    private me.apocalipsis.events.gameplay.EventLootSystem.Difficulty difficulty;
    private boolean eventoFinalizado = false;
    
    // Sistema cinematógrafico
    private me.apocalipsis.events.gameplay.CinematicSystem cinematicSystem;
    
    // Sistema de audio avanzado
    private EventAudioSystem audioSystem;
    
    // Sistema de ambiente inmersivo
    private EnvironmentSystem environmentSystem;
    
    // Sistemas de narrativa (NUEVO - Categoría 7)
    private DialogSystem dialogSystem;
    private LoreSystem loreSystem;
    private ChoiceSystem choiceSystem;
    
    // Sistema de protecciones (NUEVO - Categoría 9)
    private ProtectionSystem protectionSystem;
    
    // Sistema de espectador (NUEVO - Categoría 10)
    private SpectatorSystem spectatorSystem;
    
    // Sistema de balanceo de dificultad (NUEVO - Categoría 15)
    private DifficultyScaler difficultyScaler;
    
    // ═══════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════
    
    public EcoSombrasEvent(Apocalipsis plugin, MessageBus messageBus, SoundUtil soundUtil) {
        super(plugin, messageBus, soundUtil, "eco_sombras");
        loadConfig();
        
        items = new EcoSombrasItems();
        listener = new EcoSombrasListener(this, items);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        
        // Inicializar sistemas de gameplay
        qteSystem = new QTESystem(plugin);
        telegraphedAttack = new TelegraphedAttack(plugin);
        
        // Inicializar UI y feedback
        uiManager = new me.apocalipsis.ui.UIManager(plugin);
        feedbackSystem = new me.apocalipsis.ui.FeedbackSystem(plugin);
        particleSystem = new me.apocalipsis.events.gameplay.ParticleEffectSystem(plugin);
        
        // Inicializar sistema de loot
        difficulty = me.apocalipsis.events.gameplay.EventLootSystem.Difficulty.NORMAL;
        lootSystem = new me.apocalipsis.events.gameplay.EventLootSystem(difficulty);
        
        // Inicializar sistema cinematógrafico
        cinematicSystem = new me.apocalipsis.events.gameplay.CinematicSystem(plugin);
        
        // Inicializar sistema de audio
        audioSystem = new EventAudioSystem(plugin);
        
        // Inicializar sistema de ambiente
        environmentSystem = new EnvironmentSystem(plugin);
        
        // Inicializar sistemas de narrativa (NUEVO - Categoría 7)
        dialogSystem = new DialogSystem(plugin);
        loreSystem = new LoreSystem(plugin);
        choiceSystem = new ChoiceSystem(plugin);
        
        // Inicializar sistema de protecciones (NUEVO - Categoría 9)
        protectionSystem = new ProtectionSystem(plugin);
        
        // Inicializar sistema de espectador (NUEVO - Categoría 10)
        spectatorSystem = new SpectatorSystem(plugin);
        
        // Inicializar balanceador de dificultad (NUEVO - Categoría 15)
        difficultyScaler = new DifficultyScaler(plugin, 3, 6, 1.0);
    }
    
    private void loadConfig() {
        try {
            java.io.File configFile = new java.io.File(plugin.getDataFolder(), "eco_sombras.yml");
            if (!configFile.exists()) {
                plugin.saveResource("eco_sombras.yml", false);
            }
            config = YamlConfiguration.loadConfiguration(configFile);
            plugin.getLogger().info("[EcoSombras] Configuración cargada desde eco_sombras.yml");
        } catch (Exception e) {
            plugin.getLogger().severe("[EcoSombras] Error al cargar configuración: " + e.getMessage());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // MÉTODOS ABSTRACTOS IMPLEMENTADOS
    // ═══════════════════════════════════════════════════════════════════
    
    @Override
    public void onStart() {
        plugin.getLogger().info("[EcoSombras] Iniciando evento...");
        
        // Validar jugadores mínimos
        int jugadoresMin = config.getInt("metadata.jugadores_minimos", 3);
        int jugadoresOnline = Bukkit.getOnlinePlayers().size();
        
        if (jugadoresOnline < jugadoresMin) {
            plugin.getLogger().warning("[EcoSombras] Jugadores insuficientes: " + jugadoresOnline + "/" + jugadoresMin);
            return;
        }
        
        // Registrar participantes originales
        for (Player p : Bukkit.getOnlinePlayers()) {
            participantesOriginales.add(p.getUniqueId());
            participacionSombras.put(p.getUniqueId(), 0);
            participacionAnclas.put(p.getUniqueId(), 0);
            participacionGuardian.put(p.getUniqueId(), false);
        }
        
        // Iniciar con acto de activación
        actoActual = Acto.ACTIVACION;
        ticksEnActo = 0;
        ticksTotales = 0;
        
        // 🛡️ PROTECCIÓN: Activar sistema de protecciones (NUEVO - Categoría 9)
        protectionSystem.enable("eco_sombras");
        protectionSystem.setProtectionMode(
            true,  // preventBlockBreak
            true,  // preventBlockPlace
            true,  // preventExplosions
            true,  // preventMobSpawn
            true,  // preventPvP
            true   // preventContainerAccess
        );
        protectionSystem.enableRollback();
        
        // 👁️ ESPECTADOR: Activar modo espectador (NUEVO - Categoría 10)
        spectatorSystem.enable("eco_sombras");
        spectatorSystem.configure(
            true,   // allowFlying
            true,   // showEventInfo
            true,   // muteDeathMessages
            true,   // preventInteraction
            null    // spectatorSpawn (null = mantener ubicación actual)
        );
        
        // 🔧 FIX: Dar kit inicial y iniciar suministro de items
        darKitInicial();
        iniciarSuministroItems();
        
        iniciarActoActivacion();
    }
    
    /**
     * Obtiene el nombre en español de un tipo de ataque
     */
    private String getAttackName(TelegraphedAttack.AttackType type) {
        switch (type) {
            case SLAM:
                return "GOLPE AL SUELO";
            case BEAM:
                return "RAYO LÁSER";
            case CONE:
                return "BARRIDO FRONTAL";
            case PULSE:
                return "ONDA EXPANSIVA";
            case RAIN:
                return "LLUVIA DE PROYECTILES";
            case CHARGE:
                return "EMBESTIDA";
            default:
                return "ATAQUE DESCONOCIDO";
        }
    }
    
    /**
     * Limpia todos los sistemas de gameplay al detener el evento
     */
    @Override
    public void onStop() {
        plugin.getLogger().info("[EcoSombras] Deteniendo evento...");
        
        // Cancelar todas las tareas
        if (mainTask != null) mainTask.cancel();
        if (manchasTask != null) manchasTask.cancel();
        if (spawnTask != null) spawnTask.cancel();
        if (oleadaTask != null) oleadaTask.cancel();
        if (itemSupplyTask != null) itemSupplyTask.cancel(); // 🔧 FIX
        
        // Limpiar entidades
        cleanup();
        
        // Limpiar sistema cinematográfico
        cinematicSystem.cleanupAll();
        
        // 🎵 AUDIO: Limpiar sistema de audio
        audioSystem.cleanupAll();
        
        // 🌫️ AMBIENTE: Restaurar ambiente completo
        environmentSystem.cleanupAll();
        
        // 📖 NARRATIVA: Limpiar sistemas narrativos (NUEVO - Categoría 7)
        dialogSystem.cleanup();
        loreSystem.cleanup();
        choiceSystem.cleanup();
        
        // 🛡️ PROTECCIÓN: Desactivar sistema de protecciones (NUEVO - Categoría 9)
        protectionSystem.disable();
        
        // 👁️ ESPECTADOR: Desactivar modo espectador (NUEVO - Categoría 10)
        spectatorSystem.disable();
    }
    
    @Override
    public void onTick() {
        ticksEnActo++;
        ticksTotales++;
        
        // Lógica por acto
        switch (actoActual) {
            case ACTIVACION:
                tickActoActivacion();
                break;
            case MANCHAS:
                tickActoManchas();
                break;
            case SOMBRAS_LARGAS:
                tickActoSombrasLargas();
                
                // 📖 NARRATIVA: Susurros aleatorios (NUEVO - Categoría 7)
                if (ticksEnActo % 600 == 0 && Math.random() < 0.3) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        dialogSystem.randomWhisper(p);
                    }
                }
                break;
            case NUCLEO:
                tickActoNucleo();
                break;
            case ANCLAS:
                tickActoAnclas();
                break;
            case RITUAL:
                tickActoRitual();
                break;
            case CLIFFHANGER:
                tickActoCliffhanger();
                break;
        }
    }
    
    @Override
    public String getDisplayName() {
        return config.getString("metadata.nombre_display", "§8§lEl Eco de las Sombras Largas");
    }
    
    @Override
    public String getDescription() {
        return config.getString("metadata.descripcion", "Un eco desconocido despierta...");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ACTO 0: ACTIVACIÓN
    // ═══════════════════════════════════════════════════════════════════
    
    private void iniciarActoActivacion() {
        plugin.getLogger().info("[EcoSombras] Iniciando Acto 0: Activación");
        
        // 🌫️ AMBIENTE: Tormenta oscura + niebla densa
        World world = Bukkit.getWorlds().get(0);
        environmentSystem.setDynamicWeather(world, EnvironmentSystem.WeatherType.DARK_STORM, 0);
        environmentSystem.createVolumetricFog(world, EnvironmentSystem.FogIntensity.DENSE, 0);
        environmentSystem.adjustWorldLighting(world, 18000, true); // Medianoche bloqueada
        
        // 🎬 CINEMATOGRAFfromA: Zoom + Letterbox para todos
        for (Player p : Bukkit.getOnlinePlayers()) {
            // 🎵 AUDIO: Música de activación tensa
            audioSystem.playActMusic(p, EventAudioSystem.MusicTrack.ACTIVATION);
            
            // 🎵 AUDIO: Ambiente de tensión
            audioSystem.startAmbientTension(p);
            
            // 🎵 AUDIO: Iniciar heartbeat system
            audioSystem.startHeartbeat(p);
            
            // Zoom in cinematógrafico (0.5 = alejado)
            cinematicSystem.smoothZoom(p, 0.6f, 100);
            
            // Letterbox de 5 segundos
            cinematicSystem.showLetterbox(p, 100);
            
            // Blur inicial
            cinematicSystem.applyBlur(p, 2, 60);
            
            // Fade desde negro (blindness largo)
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 2, false, false));
            p.sendTitle("§0§l━━━━━━━━━━━━━━━", "§8§o...", 10, 60, 30);
            
            // Respiración profunda continua
            Location loc = p.getLocation();
            for (int i = 0; i < 5; i++) {
                final int index = i;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    p.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.3f);
                    // Partículas de portal flotando
                    loc.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(0, 2, 0), 30, 3, 2, 3, 0.2);
                    loc.getWorld().spawnParticle(Particle.SMOKE, loc, 15, 2, 1, 2, 0.05);
                }, index * 30L);
            }
        }
        
        // Efectode oscurecimiento
        ConfigurationSection efectos = config.getConfigurationSection("actos.acto_0_activacion.efectos.oscurecimiento");
        if (efectos != null && efectos.getBoolean("enabled", true)) {
            aplicarOscurecimiento(efectos);
        }
        
        // Mensaje inicial (delay 2 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String mensaje = config.getString("actos.acto_0_activacion.mensajes.inicial.texto",
                "§8Un eco desconocido se ha registrado en el mundo…");
            messageBus.broadcast(mensaje, "eco_sombras");
            
            // 📖 NARRATIVA: Secuencia de diálogos intro (NUEVO - Categoría 7)
            dialogSystem.broadcastDialogSequence(DialogSystem.createIntroSequence());
            
            // Sonido
            String sonidoStr = config.getString("actos.acto_0_activacion.sonidos.inicial.tipo", "ENTITY_WARDEN_HEARTBEAT");
            try {
                Sound sonido = Sound.valueOf(sonidoStr);
                float pitch = (float) config.getDouble("actos.acto_0_activacion.sonidos.inicial.pitch", 0.5);
                playSoundToAll(sonido, 1.0f, pitch);
            } catch (Exception e) {
                plugin.getLogger().warning("[EcoSombras] Sonido no válido: " + sonidoStr);
            }
        }, 40L); // 2 segundos
    }
    
    private void aplicarOscurecimiento(ConfigurationSection config) {
        int duracion = config.getInt("duracion_seg", 5);
        int tiempo = config.getInt("tiempo_minecraft", 13000);
        boolean restaurar = config.getBoolean("restaurar_tiempo", true);
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            World world = p.getWorld();
            long tiempoOriginal = world.getTime();
            
            // Cambiar tiempo gradualmente
            world.setTime(tiempo);
            
            // Partículas globales
            world.spawnParticle(Particle.ASH, p.getLocation().add(0, 50, 0), 50, 50, 10, 50, 0.01);
            
            // Restaurar después
            if (restaurar) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    world.setTime(tiempoOriginal);
                }, duracion * 20L);
            }
        }
    }
    
    private void tickActoActivacion() {
        int duracion = config.getInt("actos.acto_0_activacion.duracion_seg", 60) * 20;
        
        if (ticksEnActo >= duracion) {
            // Avanzar al siguiente acto
            transicionarActo(Acto.MANCHAS);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ACTO 1: MANCHAS DE SOMBRA
    // ═══════════════════════════════════════════════════════════════════
    
    private void iniciarActoManchas() {
        plugin.getLogger().info("[EcoSombras] Iniciando Acto 1: Manchas de Sombra");
        
        // 🎨 PARTÍCULAS AMBIENTALES
        Location center = Bukkit.getWorlds().get(0).getSpawnLocation();
        particleSystem.startAmbientParticles(center, 50, 
            me.apocalipsis.events.gameplay.ParticleEffectSystem.AmbientStyle.DUST_MOTES);
        
        ConfigurationSection manchasConfig = config.getConfigurationSection("actos.acto_1_manchas.manchas_sombra");
        if (manchasConfig == null || !manchasConfig.getBoolean("enabled", true)) {
            transicionarActo(Acto.SOMBRAS_LARGAS);
            return;
        }
        
        // Iniciar spawn periódico de manchas
        manchasTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (manchasActivas < 8) {
                spawnearMancha();
            }
        }, 100L, 60L); // Cada 3 segundos
        
        // Mensaje del Observador después de 20 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String mensaje = config.getString("actos.acto_1_manchas.mensajes.observador.texto",
                "§7§o\"No deberían moverse solas… eso pasó antes… y terminó mal.\"");
            messageBus.broadcast(mensaje, "eco_sombras");
        }, 400L);
    }
    
    private void spawnearMancha() {
        // Elegir jugador aleatorio
        List<Player> jugadores = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (jugadores.isEmpty()) return;
        
        Player target = jugadores.get(random.nextInt(jugadores.size()));
        Location loc = target.getLocation();
        
        // Spawn a 10-30 bloques
        int distancia = 10 + random.nextInt(21);
        double angulo = random.nextDouble() * 2 * Math.PI;
        
        Location spawnLoc = loc.clone().add(
            Math.cos(angulo) * distancia,
            0,
            Math.sin(angulo) * distancia
        );
        
        // Ajustar Y al suelo
        spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc) + 1);
        
        manchasLocations.add(spawnLoc);
        manchasActivas++;
        
        // 🔧 FIX: Spawn SILVERFISH visible en lugar de solo partículas
        Silverfish mancha = (Silverfish) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.SILVERFISH);
        
        // Configurar para visibilidad máxima
        mancha.customName(net.kyori.adventure.text.Component.text("§8§o◊ Mancha de Sombra ◊"));
        mancha.setCustomNameVisible(true);
        mancha.setAI(true);
        mancha.setSilent(false);
        mancha.setInvulnerable(false);
        
        // 🔧 GLOWING permanente para destacar
        mancha.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
        mancha.setGlowing(true);
        
        // Velocidad aumentada para huir
        mancha.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2, false, false));
        
        // Registrar en entidades del evento
        entidadesEvento.add(mancha.getUniqueId());
        protectionSystem.registerEventEntity(mancha);
        
        // 🔧 Partículas MULTI-COLOR continuas para destacar
        BukkitTask particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!mancha.isValid() || mancha.isDead()) {
                manchasLocations.remove(spawnLoc);
                manchasActivas--;
                return;
            }
            
            Location manchaLoc = mancha.getLocation().add(0, 0.5, 0);
            
            // Aura negra constante
            manchaLoc.getWorld().spawnParticle(Particle.SQUID_INK, manchaLoc, 5, 0.3, 0.3, 0.3, 0.05);
            
            // Partículas moradas para contraste
            manchaLoc.getWorld().spawnParticle(Particle.PORTAL, manchaLoc, 3, 0.2, 0.2, 0.2, 0);
            
            // Dust morado brillante
            manchaLoc.getWorld().spawnParticle(Particle.DUST, manchaLoc, 2, 0.2, 0.2, 0.2, 
                new Particle.DustOptions(org.bukkit.Color.fromRGB(138, 43, 226), 1.5f));
        }, 0L, 2L); // Cada 0.1 segundos
        
        // 🔧 FIX #8: Detector de proximidad para desaparecer manchas
        BukkitTask proximityTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!mancha.isValid() || mancha.isDead()) {
                return;
            }
            
            // Detectar jugadores cercanos (radio 2.5 bloques)
            boolean playerNearby = mancha.getNearbyEntities(2.5, 2.5, 2.5).stream()
                .anyMatch(e -> e instanceof Player);
            
            if (playerNearby) {
                // Partículas de desaparición (humo negro)
                Location manchaLoc2 = mancha.getLocation();
                manchaLoc2.getWorld().spawnParticle(Particle.SMOKE, manchaLoc2, 30, 0.5, 0.5, 0.5, 0.05);
                manchaLoc2.getWorld().spawnParticle(Particle.SQUID_INK, manchaLoc2, 20, 0.3, 0.3, 0.3, 0.02);
                
                // Sonido de desvanecimiento
                manchaLoc2.getWorld().playSound(manchaLoc2, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.8f);
                
                // Remover mancha
                mancha.remove();
                manchasLocations.remove(spawnLoc);
                manchasActivas--;
                particleTask.cancel();
            }
        }, 10L, 10L); // Revisar cada 0.5s
        
        // 🔧 Sonido periódico para localización
        BukkitTask soundTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!mancha.isValid() || mancha.isDead()) {
                return;
            }
            mancha.getWorld().playSound(mancha.getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT, 0.3f, 0.5f);
        }, 0L, 40L); // Cada 2 segundos
        
        // Timeout de 30 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (mancha.isValid() && !mancha.isDead()) {
                mancha.remove();
                manchasLocations.remove(spawnLoc);
                manchasActivas--;
            }
        }, 600L);
    }
    
    private void huidaMancha(Location manchaLoc, Location playerLoc) {
        // Dirección opuesta al jugador
        Vector direccion = manchaLoc.toVector().subtract(playerLoc.toVector()).normalize();
        Location nuevaLoc = manchaLoc.clone().add(direccion.multiply(random.nextInt(6) + 15));
        nuevaLoc.setY(nuevaLoc.getWorld().getHighestBlockYAt(nuevaLoc) + 1);
        
        // Efectos
        manchaLoc.getWorld().spawnParticle(Particle.SMOKE, manchaLoc, 10, 0.5, 0.5, 0.5, 0.05);
        manchaLoc.getWorld().playSound(manchaLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.3f);
        
        // Actualizar posición
        manchasLocations.remove(manchaLoc);
        manchasLocations.add(nuevaLoc);
    }
    
    private void tickActoManchas() {
        int duracion = config.getInt("actos.acto_1_manchas.duracion_seg", 900) * 20;
        
        if (ticksEnActo >= duracion) {
            if (manchasTask != null) manchasTask.cancel();
            manchasLocations.clear();
            manchasActivas = 0;
            transicionarActo(Acto.SOMBRAS_LARGAS);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ACTO 2: SOMBRAS LARGAS (MOBS)
    // ═══════════════════════════════════════════════════════════════════
    
    private void iniciarActoSombrasLargas() {
        plugin.getLogger().info("[EcoSombras] Iniciando Acto 2: Sombras Largas");
        
        // Spawn periódico de Sombras Largas
        int intervalo = config.getInt("actos.acto_2_sombras_largas.spawn_sombras.intervalo_spawn_seg", 17);
        
        spawnTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int maximo = config.getInt("actos.acto_2_sombras_largas.spawn_sombras.maximo_activas", 12);
            if (contarSombrasActivas() < maximo) {
                spawnearSombraLarga();
            }
        }, 100L, intervalo * 20L);
    }
    
    private void spawnearSombraLarga() {
        List<Player> jugadores = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (jugadores.isEmpty()) return;
        
        Player target = jugadores.get(random.nextInt(jugadores.size()));
        Location spawnLoc = encontrarPosicionSpawn(target.getLocation(), 10, 40);
        
        if (spawnLoc == null) return;
        
        // 🎬 SLOW MOTION a todos los jugadores cerca (2 segundos congelados)
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getLocation().distance(spawnLoc) < 30) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 9, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 5, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 40, 250, false, false)); // No puede saltar
                
                // Susurros distorsionados
                p.playSound(p.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 0.8f, 0.3f);
                p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1.0f, 0.4f);
                p.sendTitle("", "§8§o...algo emerge...", 5, 30, 10);
            }
        }
        
        // Spawn del mob
        ConfigurationSection mobConfig = config.getConfigurationSection("actos.acto_2_sombras_largas.spawn_sombras.configuracion_mob");
        Zombie sombra = (Zombie) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
        
        configurarSombraLarga(sombra, mobConfig);
        entidadesEvento.add(sombra.getUniqueId());
        protectionSystem.registerEventEntity(sombra); // 🛡️ Registrar en protección
        
        // 🎨 TRAIL Y AURA DE SOMBRA
        particleSystem.startShadowTrail(sombra, me.apocalipsis.events.gameplay.ParticleEffectSystem.ParticleTrailType.SHADOW);
        
        // 🎬 Partículas de spawn MASIVAS con distorsión
        spawnLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, spawnLoc, 50, 1, 2, 1, 0.15);
        spawnLoc.getWorld().spawnParticle(Particle.SQUID_INK, spawnLoc, 30, 0.8, 1.5, 0.8, 0.1);
        spawnLoc.getWorld().spawnParticle(Particle.PORTAL, spawnLoc, 40, 1, 1, 1, 0.5);
        spawnLoc.getWorld().spawnParticle(Particle.END_ROD, spawnLoc, 20, 0.5, 1, 0.5, 0.1);
        
        // Explosión visual sin daño
        spawnLoc.getWorld().spawnParticle(Particle.EXPLOSION, spawnLoc, 3, 0.5, 0.5, 0.5, 0);
        spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.5f);
        
        // Partículas de sombra proyectada en el suelo
        for (int i = 0; i < 20; i++) {
            double angle = (Math.PI * 2 / 20) * i;
            Location groundLoc = spawnLoc.clone().add(Math.cos(angle) * 2, -0.5, Math.sin(angle) * 2);
            spawnLoc.getWorld().spawnParticle(Particle.SQUID_INK, groundLoc, 5, 0.1, 0, 0.1, 0);
        }
    }
    
    private void configurarSombraLarga(Zombie sombra, ConfigurationSection config) {
        // Nombre
        sombra.setCustomName(config.getString("nombre", "§8Sombra Larga"));
        sombra.setCustomNameVisible(false);
        
        // Atributos mejorados para Netherite Prot 4
        sombra.getAttribute(Attribute.MAX_HEALTH).setBaseValue(config.getDouble("atributos.vida", 60));
        sombra.setHealth(sombra.getAttribute(Attribute.MAX_HEALTH).getValue());
        sombra.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(config.getDouble("atributos.danio", 14));
        sombra.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(config.getDouble("atributos.velocidad", 0.26));
        sombra.getAttribute(Attribute.ARMOR).setBaseValue(config.getDouble("atributos.armadura", 8));
        sombra.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(config.getDouble("atributos.knockback_resistance", 0.3));
        
        // Visual
        sombra.setInvisible(config.getBoolean("invisible", true));
        sombra.setSilent(config.getBoolean("silencioso", true));
        
        // Equipamiento (casco negro)
        ItemStack casco = new ItemStack(Material.LEATHER_HELMET);
        org.bukkit.inventory.meta.LeatherArmorMeta meta = (org.bukkit.inventory.meta.LeatherArmorMeta) casco.getItemMeta();
        meta.setColor(org.bukkit.Color.BLACK);
        casco.setItemMeta(meta);
        sombra.getEquipment().setHelmet(casco);
        sombra.getEquipment().setHelmetDropChance(0f);
        
        // Baby = false
        sombra.setBaby(false);
    }
    
    private int contarSombrasActivas() {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entidadesEvento.contains(entity.getUniqueId()) && entity instanceof Zombie) {
                    count++;
                }
            }
        }
        return count;
    }
    
    private void tickActoSombrasLargas() {
        // Mensaje del Observador tras matar 5 sombras
        if (sombrasLargasMuertas == 5) {
            String mensaje = config.getString("actos.acto_2_sombras_largas.mensajes.observador.texto",
                "§7§o\"Estiran su forma buscando un anfitrión… como lo hicieron en aquel mundo…\"");
            messageBus.broadcast(mensaje, "eco_sombras");
        }
        
        // 🔧 FIX: TRANSICIÓN AUTOMÁTICA al matar 15 sombras (antes 20) → NÚCLEO (antes ANCLAS)
        if (sombrasLargasMuertas >= 15) {
            if (spawnTask != null) spawnTask.cancel();
            efectoCinematico("§5§l⚡ EL NÚCLEO EMERGE", 10, 60, 20);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                limpiarEntidadesActoAnterior();
                transicionarActo(Acto.NUCLEO);
            }, 60L);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ACTO 3: NÚCLEO DE SOMBRA LARGA
    // ═══════════════════════════════════════════════════════════════════
    
    private void iniciarActoNucleo() {
        plugin.getLogger().info("[EcoSombras] Iniciando Acto 3: Núcleo");
        
        // 🎨 CAMBIAR PARTÍCULAS AMBIENTALES A MÁS INTENSAS
        Location center = Bukkit.getWorlds().get(0).getSpawnLocation();
        particleSystem.startAmbientParticles(center, 60, 
            me.apocalipsis.events.gameplay.ParticleEffectSystem.AmbientStyle.VOID_PARTICLES);
        
        // 🎬 FADE TO BLACK TOTAL (3 segundos de oscuridad completa)
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 5, false, false));
            p.sendTitle("§0§l━━━━━━━━━━━━━━━", "", 10, 60, 30);
            
            // Corazón latiendo en la oscuridad
            for (int i = 0; i < 6; i++) {
                final int index = i;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 2.0f, 0.4f);
                }, index * 15L);
            }
        }
        
        // Spawn del Núcleo después de la oscuridad
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            List<Player> jugadores = new ArrayList<>(Bukkit.getOnlinePlayers());
            if (jugadores.isEmpty()) return;
            
            Player target = jugadores.get(random.nextInt(jugadores.size()));
            nucleoLocation = encontrarPosicionSpawn(target.getLocation(), 20, 50);
            
            if (nucleoLocation == null) return;
            
            // Elevar 3 bloques
            nucleoLocation.add(0, 3, 0);
            
            // 🎬 EXPLOSIÓN DE LUZ al aparecer
            for (int i = 0; i < 360; i += 15) {
                double radians = Math.toRadians(i);
                for (int r = 1; r <= 10; r++) {
                    Location particleLoc = nucleoLocation.clone().add(
                        Math.cos(radians) * r,
                        0,
                        Math.sin(radians) * r
                    );
                    nucleoLocation.getWorld().spawnParticle(Particle.FLASH, particleLoc, 1, 0, 0, 0, 0);
                    nucleoLocation.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                }
            }
            
            // Partículas verticales masivas
            for (int y = 0; y < 30; y++) {
                nucleoLocation.getWorld().spawnParticle(Particle.REVERSE_PORTAL, nucleoLocation.clone().add(0, y, 0), 10, 0.5, 0, 0.5, 0.1);
            }
            
            // Spawn Shulker como base
            Shulker nucleo = (Shulker) nucleoLocation.getWorld().spawnEntity(nucleoLocation, EntityType.SHULKER);
            configurarNucleo(nucleo);
            
            // 🔧 FIX: GLOWING permanente para visibilidad máxima
            nucleo.setGlowing(true);
            nucleo.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
            
            // 🔧 FIX: INVULNERABLE hasta que se sellen las anclas
            nucleo.setInvulnerable(true);
            
            nucleoEntity = nucleo;
            entidadesEvento.add(nucleo.getUniqueId());
            protectionSystem.registerEventEntity(nucleo); // 🛡️ Registrar en protección
            
            // 🔧 FIX: Partículas INTENSAS permanentes
            BukkitTask nucleoParticles = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!nucleo.isValid() || nucleo.isDead() || actoActual != Acto.NUCLEO) {
                    return;
                }
                
                Location loc = nucleo.getLocation().add(0, 1, 0);
                
                // Múltiples capas de partículas
                loc.getWorld().spawnParticle(Particle.END_ROD, loc, 10, 0.5, 0.5, 0.5, 0.1);
                loc.getWorld().spawnParticle(Particle.PORTAL, loc, 15, 0.7, 0.7, 0.7, 0.5);
                loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc, 10, 0.5, 0.5, 0.5, 0.3);
                loc.getWorld().spawnParticle(Particle.SQUID_INK, loc, 5, 0.3, 0.3, 0.3, 0.05);
                
                // Dust morado brillante
                loc.getWorld().spawnParticle(Particle.DUST, loc, 8, 0.5, 0.5, 0.5, 
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(138, 43, 226), 2.5f));
                
                // Sonic boom cada 2 segundos
                if (ticksEnActo % 40 == 0) {
                    loc.getWorld().spawnParticle(Particle.SONIC_BOOM, loc, 5, 1, 1, 1, 0);
                    loc.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.5f, 1.5f);
                }
            }, 0L, 2L); // Cada 0.1 segundos
            
            // 🔧 FIX: BEACON VERTICAL permanente
            BukkitTask nucleoBeam = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!nucleo.isValid() || nucleo.isDead() || actoActual != Acto.NUCLEO) {
                    return;
                }
                
                Location base = nucleo.getLocation();
                for (int y = 1; y <= 50; y++) {
                    base.getWorld().spawnParticle(Particle.END_ROD, base.clone().add(0, y, 0), 
                        2, 0.1, 0, 0.1, 0);
                }
            }, 0L, 10L); // Cada 0.5 segundos
            
            // 🔧 FIX: Sonido ambiente constante
            BukkitTask nucleoSound = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!nucleo.isValid() || nucleo.isDead() || actoActual != Acto.NUCLEO) {
                    return;
                }
                
                Location loc = nucleo.getLocation();
                loc.getWorld().playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 1.0f, 0.5f);
                loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 0.8f);
            }, 0L, 60L); // Cada 3 segundos
            
            // 🔧 FIX: WAYPOINT visual con action bar
            BukkitTask nucleoWaypoint = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!nucleo.isValid() || nucleo.isDead() || actoActual != Acto.NUCLEO) {
                    return;
                }
                
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.setCompassTarget(nucleoLocation);
                    double distance = p.getLocation().distance(nucleoLocation);
                    p.sendActionBar(net.kyori.adventure.text.Component.text(
                        String.format("§5§l⬡ NÚCLEO §7[%.0fm] §c§lINVULNERABLE", distance)
                    ));
                }
            }, 0L, 20L); // Cada segundo
            
            // 🎨 AURA PULSANTE MÍSTICA DEL NÚCLEO
            particleSystem.startPulsingAura(nucleo, 
                me.apocalipsis.events.gameplay.ParticleEffectSystem.AuraStyle.MYSTIC, 8);
            
            // 🎨 SÍMBOLO FLOTANTE DE PENTAGRAM
            particleSystem.createFloatingSymbol(nucleoLocation.clone().add(0, 3, 0), 
                me.apocalipsis.events.gameplay.ParticleEffectSystem.SymbolType.PENTAGRAM, 120);
            
            // Mensaje dramático
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle("§5§lUna raíz de la sombra", "§7ha despertado", 10, 60, 20);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
                
                // 🌫️ AMBIENTE: Transición a ceniza + corrupción
                World world = nucleoLocation.getWorld();
                environmentSystem.setDynamicWeather(world, EnvironmentSystem.WeatherType.ASHEN_FOG, 0);
                environmentSystem.spawnAtmosphericEffect(world, EnvironmentSystem.AtmosphericEffect.ASH_FALL, 0);
                
                // Corrupción del terreno alrededor del núcleo
                environmentSystem.alterWorldTemporarily(nucleoLocation, 15, 
                    EnvironmentSystem.CorruptionType.NETHERRACK_SPREAD);
                
                // 🎵 AUDIO: Transición a música de núcleo épica
                audioSystem.playActMusic(p, EventAudioSystem.MusicTrack.NUCLEUS);
                
                // 🎵 AUDIO: Sonido posicional del spawn con reverb TEMPLE
                audioSystem.playPositionalSoundWithReverb(p, nucleoLocation, 
                    EventAudioSystem.SoundType.NUCLEUS_SPAWN, 64.0, 
                    EventAudioSystem.ReverbType.TEMPLE);
                
                // 🎵 AUDIO: Stinger musical de núcleo
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (p.isOnline()) {
                        audioSystem.playStinger(p, EventAudioSystem.StingerType.NUCLEUS_SPAWNED);
                    }
                }, 20L);
                
                // 🎬 CINEMATOGRAFÍA: Camera shake + Orbit alrededor del núcleo
                cinematicSystem.cameraShake(p, 
                    me.apocalipsis.events.gameplay.CinematicSystem.ShakeIntensity.MEDIUM, 60);
                
                // Orbit camera alrededor del núcleo (3 segundos, 90°/s, radio 8)
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (p.isOnline() && nucleoEntity != null && nucleoEntity.isValid()) {
                        cinematicSystem.orbitCamera(p, nucleoLocation, 90, 3, 8, 5);
                    }
                }, 60L);
            }
            
            String mensaje = config.getString("actos.acto_3_nucleo.mensajes.aparicion.chat");
            messageBus.broadcast(mensaje, "eco_sombras");
            
            // Sonidos superpuestos
            nucleoLocation.getWorld().playSound(nucleoLocation, Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.6f);
            nucleoLocation.getWorld().playSound(nucleoLocation, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.3f);
            
            // Efectos visuales periódicos
            iniciarEfectosNucleo();
        }, 80L); // Después de 4 segundos de oscuridad
    }
    
    private void configurarNucleo(Shulker nucleo) {
        ConfigurationSection config = this.config.getConfigurationSection("actos.acto_3_nucleo.nucleo");
        
        nucleo.setCustomName(config.getString("nombre", "§5§l§nNúcleo de Sombra Larga"));
        nucleo.setCustomNameVisible(true);
        nucleo.setAI(false);
        nucleo.setGravity(false);
        nucleo.setInvulnerable(false);
        
        // Vida aumentada para desafío con Netherite
        double vida = config.getDouble("atributos.vida", 400);
        nucleo.getAttribute(Attribute.MAX_HEALTH).setBaseValue(vida);
        nucleo.setHealth(vida);
        nucleoVidaActual = vida;
    }
    
    private void iniciarEfectosNucleo() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (nucleoEntity == null || !nucleoEntity.isValid() || actoActual != Acto.NUCLEO) {
                return;
            }
            
            Location loc = nucleoEntity.getLocation();
            
            // Partículas
            loc.getWorld().spawnParticle(Particle.PORTAL, loc, 5, 2, 2, 2, 0.1);
            loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc, 3, 1.5, 1.5, 1.5, 0.05);
            
            // Sonido ambiental (cada 5 segundos)
            if (ticksEnActo % 100 == 0) {
                loc.getWorld().playSound(loc, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.7f);
            }
        }, 0L, 5L);
    }
    
    private void tickActoNucleo() {
        if (nucleoEntity == null || !nucleoEntity.isValid()) {
            return;
        }
        
        LivingEntity nucleo = (LivingEntity) nucleoEntity;
        double vidaActual = nucleo.getHealth();
        
        // 🔧 FIX: TRANSICIÓN AUTOMÁTICA: Núcleo destruido → ANCLAS (antes RITUAL)
        if (vidaActual <= 0 || !nucleo.isValid()) {
            if (spawnTask != null) spawnTask.cancel();
            efectoCinematico("§5§l⚡ LAS ANCLAS EMERGEN ⚡", 10, 60, 20);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                limpiarEntidadesActoAnterior();
                transicionarActo(Acto.ANCLAS);
            }, 60L);
            return;
        }
        
        // Teleporte cada 50 HP de daño o cada 25 seg
        if ((nucleoVidaActual - vidaActual) >= 50 || ticksEnActo % 500 == 0) {
            teleportarNucleo();
            nucleoVidaActual = vidaActual;
        }
    }
    
    private void teleportarNucleo() {
        if (nucleoEntity == null || !nucleoEntity.isValid()) return;
        
        Location actualLoc = nucleoEntity.getLocation();
        Location nuevaLoc = encontrarPosicionSpawn(actualLoc, 30, 50);
        
        if (nuevaLoc == null) return;
        
        nuevaLoc.add(0, 3, 0);
        
        // Efectos pre-TP
        actualLoc.getWorld().spawnParticle(Particle.PORTAL, actualLoc, 50, 1, 1, 1, 0.5);
        
        // Teleportar
        nucleoEntity.teleport(nuevaLoc);
        nucleoLocation = nuevaLoc;
        
        // Efectos post-TP
        nuevaLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, nuevaLoc, 1);
        nuevaLoc.getWorld().playSound(nuevaLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
        
        // Invulnerabilidad temporal
        ((LivingEntity) nucleoEntity).setInvulnerable(true);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (nucleoEntity != null && nucleoEntity.isValid()) {
                ((LivingEntity) nucleoEntity).setInvulnerable(false);
            }
        }, 40L);
        
        nucleoTeleportes++;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ACTO 4: ANCLAS DEL MUNDO
    // ═══════════════════════════════════════════════════════════════════
    
    private void iniciarActoAnclas() {
        plugin.getLogger().info("[EcoSombras] Iniciando Acto 4: Anclas del Mundo");
        
        // 🔧 FIX #12: Escalar anclas según cantidad de jugadores (3 anclas para ≤3 jugadores)
        int jugadoresActivos = Bukkit.getOnlinePlayers().size();
        int cantidad;
        if (jugadoresActivos <= 3) {
            cantidad = 3; // 3 anclas para grupos pequeños
        } else {
            cantidad = config.getInt("actos.acto_4_anclas.anclas.cantidad", 5); // 5 anclas por defecto
        }
        
        plugin.getLogger().info("[EcoSombras] Generando " + cantidad + " anclas para " + jugadoresActivos + " jugadores");
        
        if (nucleoLocation == null) return;
        
        for (int i = 0; i < cantidad; i++) {
            double angulo = (2 * Math.PI / cantidad) * i;
            int distancia = 40 + random.nextInt(41); // 40-80 bloques
            
            Location anclaLoc = nucleoLocation.clone().add(
                Math.cos(angulo) * distancia,
                0,
                Math.sin(angulo) * distancia
            );
            
            anclaLoc.setY(anclaLoc.getWorld().getHighestBlockYAt(anclaLoc));
            
            generarEstructuraAncla(anclaLoc, i);
            anclaLocations.add(anclaLoc);
            
            // 🛡️ PROTECCIÓN: Añadir zona protegida para ancla (NUEVO - Categoría 9)
            protectionSystem.addProtectedZone(anclaLoc, 10, "Ancla " + (i + 1));
        }
        
        // Mensaje
        messageBus.broadcast("§5§lLas Anclas del Mundo han emergido.", "eco_sombras");
        messageBus.broadcast("§7Selladlas con §8Fragmentos de Sombra§7.", "eco_sombras");
    }
    
    private void generarEstructuraAncla(Location center, int id) {
        World world = center.getWorld();
        
        // 🎬 RAYO DEL CIELO al spawn (múltiples explosiones verticales)
        for (int y = 100; y >= center.getY(); y -= 5) {
            Location rayLoc = center.clone();
            rayLoc.setY(y);
            world.spawnParticle(Particle.FLASH, rayLoc, 3, 0.2, 0.2, 0.2, 0);
            world.spawnParticle(Particle.EXPLOSION, rayLoc, 1, 0, 0, 0, 0);
        }
        
        // Sonido de trueno al spawn
        world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.8f);
        world.playSound(center, Sound.ITEM_TRIDENT_THUNDER, 1.5f, 1.2f);
        
        // 🔧 FIX: Base 5x5 con patrón visible (en lugar de 3x3)
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Location loc = center.clone().add(x, 0, z);
                // Alternar bloques para patrón visible
                if ((x + z) % 2 == 0) {
                    loc.getBlock().setType(Material.BLACKSTONE);
                } else {
                    loc.getBlock().setType(Material.CRYING_OBSIDIAN);
                }
            }
        }
        
        // 🔧 FIX: RESPAWN ANCHOR más alto (nivel 2) y CARGADO al máximo
        Location anchorLoc = center.clone().add(0, 2, 0);
        anchorLoc.getBlock().setType(Material.RESPAWN_ANCHOR);
        
        org.bukkit.block.data.type.RespawnAnchor anchor = 
            (org.bukkit.block.data.type.RespawnAnchor) anchorLoc.getBlock().getBlockData();
        anchor.setCharges(4); // Máxima carga = máximo brillo
        anchorLoc.getBlock().setBlockData(anchor);
        
        // 🔧 FIX: PILARES de velas moradas (4 pilares de 3 bloques)
        for (int dir = 0; dir < 4; dir++) {
            int offsetX = 0, offsetZ = 0;
            switch (dir) {
                case 0: offsetX = 3; break;   // Este
                case 1: offsetX = -3; break;  // Oeste
                case 2: offsetZ = 3; break;   // Sur
                case 3: offsetZ = -3; break;  // Norte
            }
            
            // Pilar de 3 velas apiladas
            for (int y = 0; y < 3; y++) {
                Location candleLoc = center.clone().add(offsetX, 1 + y, offsetZ);
                candleLoc.getBlock().setType(Material.PURPLE_CANDLE);
                
                org.bukkit.block.data.type.Candle candle = 
                    (org.bukkit.block.data.type.Candle) candleLoc.getBlock().getBlockData();
                candle.setLit(true);
                candle.setCandles(4); // Máximo de velas = más luz
                candleLoc.getBlock().setBlockData(candle);
            }
            
            // End Rod en la cima para beacon visual
            Location topLoc = center.clone().add(offsetX, 4, offsetZ);
            topLoc.getBlock().setType(Material.END_ROD);
        }
        
        // 🔧 FIX: BEAM TRIPLE más intenso hasta el cielo
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (anclasSelladas.contains(id) || actoActual != Acto.ANCLAS) {
                return;
            }
            
            // Beam vertical TODOS los bloques hasta el cielo (no cada 2)
            for (int y = 1; y <= 100; y++) {
                // Triple beam: END_ROD + REVERSE_PORTAL + DUST morado
                world.spawnParticle(Particle.END_ROD, center.clone().add(0, y, 0), 
                    3, 0.1, 0, 0.1, 0);
                world.spawnParticle(Particle.REVERSE_PORTAL, center.clone().add(0, y, 0), 
                    5, 0.2, 0, 0.2, 0);
                world.spawnParticle(Particle.DUST, center.clone().add(0, y, 0), 
                    2, 0.1, 0, 0.1, new Particle.DustOptions(org.bukkit.Color.fromRGB(138, 43, 226), 2.0f));
            }
            
            // 🔧 FIX: Pulso radial CONSTANTE (cada tick)
            for (int angle = 0; angle < 360; angle += 20) {
                double radians = Math.toRadians(angle);
                for (double r = 0; r <= 8; r += 0.3) {
                    Location pulseLoc = center.clone().add(
                        Math.cos(radians) * r,
                        0.5,
                        Math.sin(radians) * r
                    );
                    world.spawnParticle(Particle.SONIC_BOOM, pulseLoc, 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.PORTAL, pulseLoc, 1, 0, 0, 0, 0);
                }
            }
            
            // Sonido ambiental constante
            world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.8f, 1.5f);
            
        }, 0L, 5L); // Cada 0.25 segundos
        
        // 🔧 FIX: WAYPOINT visual - compass apunta + action bar con distancia
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (anclasSelladas.contains(id) || actoActual != Acto.ANCLAS) {
                return;
            }
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.setCompassTarget(center);
                double distance = p.getLocation().distance(center);
                p.sendActionBar(net.kyori.adventure.text.Component.text(
                    String.format("§5§l⚡ ANCLA %d §7[%.0fm]", (id + 1), distance)
                ));
            }
        }, 0L, 20L); // Cada segundo
    }
    
    private void tickActoAnclas() {
        // 🔧 FIX: Verificar si todas las anclas están selladas
        if (anclasSelladas.size() >= anclaLocations.size()) {
            // 🔧 FIX: Hacer núcleo VULNERABLE en lugar de matarlo automáticamente
            if (nucleoEntity != null && nucleoEntity.isValid()) {
                LivingEntity nucleo = (LivingEntity) nucleoEntity;
                nucleo.setInvulnerable(false);
                
                // Efectos visuales de vulnerabilidad
                nucleoLocation.getWorld().spawnParticle(Particle.EXPLOSION, nucleoLocation, 10, 1, 1, 1);
                nucleoLocation.getWorld().playSound(nucleoLocation, Sound.ENTITY_WITHER_BREAK_BLOCK, 2.0f, 0.5f);
                
                messageBus.broadcast("§c§l¡El Núcleo es ahora VULNERABLE!", "eco_sombras");
                messageBus.broadcast("§7¡Destrúyelo antes de que sea tarde!", "eco_sombras");
                
                // Actualizar action bar para todos
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendActionBar(net.kyori.adventure.text.Component.text("§5§l⬡ NÚCLEO §a§lVULNERABLE §7- ¡DESTRÚYELO!"));
                }
            }
            
            // 🔧 FIX: Verificar si el núcleo ha sido destruido después de hacerlo vulnerable
            if (nucleoEntity == null || !nucleoEntity.isValid() || ((LivingEntity) nucleoEntity).getHealth() <= 0) {
                messageBus.broadcast("§5§l¡El Núcleo ha sido destruido!", "eco_sombras");
                efectoCinematico("§5§l⚡ EL RITUAL COMIENZA ⚡", 10, 60, 20);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    limpiarEntidadesActoAnterior();
                    transicionarActo(Acto.RITUAL);
                }, 60L);
            }
        }
    }
    
    // Método público para el listener
    public void sellarAncla(int id, Player jugador) {
        if (anclasSelladas.contains(id)) return;
        
        // Validar que las anclas existen y el ID es válido
        if (anclaLocations.isEmpty()) {
            jugador.sendMessage("§c¡Las anclas aún no han sido generadas!");
            plugin.getLogger().warning("[EcoSombras] Intento de sellar ancla antes de ser generadas");
            return;
        }
        
        if (id < 0 || id >= anclaLocations.size()) {
            jugador.sendMessage("§c¡ID de ancla inválido!");
            return;
        }
        
        // 🎮 MINI-JUEGO QTE: Secuencia de clicks para sellar
        iniciarMiniJuegoAncla(id, jugador);
    }
    
    private void iniciarMiniJuegoAncla(int id, Player jugador) {
        Location anclaLoc = anclaLocations.get(id);
        
        // Efectos pre-QTE
        jugador.sendTitle("§5§l⚡ SELLA EL ANCLA", "§7¡Completa la secuencia!", 5, 40, 10);
        anclaLoc.getWorld().playSound(anclaLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.5f);
        
        // Iniciar QTE de secuencia
        qteSystem.startQTE(jugador, QTESystem.QTEType.SEQUENCE, 100, new QTESystem.QTECallback() {
            @Override
            public void onSuccess(Player player, int score) {
                // Registrar score
                playerQTEScores.merge(player.getUniqueId(), score, Integer::sum);
                
                // Completar sellado
                completarSelladoAncla(id, jugador);
            }
            
            @Override
            public void onFailure(Player player) {
                player.sendMessage("§c✗ No pudiste sellar el ancla. ¡Inténtalo de nuevo!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
            }
            
            @Override
            public void onCooperativeComplete(Collection<Player> players, int successCount) {
                // No usado aquí
            }
        });
    }
    
    private void completarSelladoAncla(int id, Player jugador) {
        anclasSelladas.add(id);
        
        // Incrementar participación
        participacionAnclas.merge(jugador.getUniqueId(), 1, Integer::sum);
        
        // Efectos
        Location anclaLoc = anclaLocations.get(id);
        
        // 🎵 AUDIO: Sonido posicional del ancla sellada con reverb
        for (Player p : Bukkit.getOnlinePlayers()) {
            audioSystem.playPositionalSoundWithReverb(p, anclaLoc, 
                EventAudioSystem.SoundType.ANCHOR_SEALED, 48.0, 
                EventAudioSystem.ReverbType.TEMPLE);
            
            // Stinger musical de ancla
            audioSystem.playStinger(p, EventAudioSystem.StingerType.ANCHOR_SEALED);
        }
        
        // 🎬 SCREEN SHAKE FUERTE a todos los jugadores
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 10; i++) {
                final int index = i;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Vector shake = new Vector(
                        (random.nextDouble() - 0.5) * 0.5,
                        (random.nextDouble() - 0.5) * 0.3,
                        (random.nextDouble() - 0.5) * 0.5
                    );
                    p.setVelocity(shake);
                }, index * 1L);
            }
        }
        
        // Explosión visual masiva
        anclaLoc.getWorld().spawnParticle(Particle.END_ROD, anclaLoc.clone().add(0, 1, 0), 100, 1, 20, 1, 0.5);
        anclaLoc.getWorld().spawnParticle(Particle.FLASH, anclaLoc.clone().add(0, 1, 0), 10, 0.5, 1, 0.5, 0);
        anclaLoc.getWorld().spawnParticle(Particle.EXPLOSION, anclaLoc.clone().add(0, 10, 0), 5, 2, 2, 2, 0);
        
        // Sonidos superpuestos
        anclaLoc.getWorld().playSound(anclaLoc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 2.0f, 0.5f);
        anclaLoc.getWorld().playSound(anclaLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.0f);
        anclaLoc.getWorld().playSound(anclaLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.8f);
        
        // Mensaje
        String msg = String.format("§5Ancla %d/%d sellada", anclasSelladas.size(), anclaLocations.size());
        messageBus.broadcast(msg, "eco_sombras");
        
        // Sonido global
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.5f, 1.5f);
        }
        
        // Mensaje del Observador (primera vez)
        if (anclasSelladas.size() == 1) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                String obs = config.getString("actos.acto_4_anclas.mensajes.observador.texto",
                    "§7§o\"Sellan la herida, pero no la causa…\"");
                messageBus.broadcast(obs, "eco_sombras");
            }, 40L);
        }
        
        // 🎬 FADE TO WHITE al sellar la 5ta ancla + CÁMARA LENTA
        if (anclasSelladas.size() >= 5) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                // Fade a blanco con Glowing + Blindness
                p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 3, false, false));
                
                // Cámara lenta extrema
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 9, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 9, false, false));
                
                p.sendTitle("§f§l━━━━━━━━━━━━━━━", "§7§oLas anclas han sido selladas...", 10, 40, 20);
                p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 2.0f, 2.0f);
            }
            
            efectoCinematico("§8§l⬢ EL NÚCLEO SE MANIFIESTA", 10, 60, 20);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                limpiarEntidadesActoAnterior();
                transicionarActo(Acto.NUCLEO);
            }, 60L);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════════
    
    private Location encontrarPosicionSpawn(Location center, int minDist, int maxDist) {
        for (int intento = 0; intento < 20; intento++) {
            int distancia = minDist + random.nextInt(maxDist - minDist);
            double angulo = random.nextDouble() * 2 * Math.PI;
            
            Location loc = center.clone().add(
                Math.cos(angulo) * distancia,
                0,
                Math.sin(angulo) * distancia
            );
            
            loc.setY(loc.getWorld().getHighestBlockYAt(loc) + 1);
            
            // Validar suelo sólido
            if (loc.getBlock().getRelative(0, -1, 0).getType().isSolid()) {
                return loc;
            }
        }
        return null;
    }
    
    private void transicionarActo(Acto nuevoActo) {
        plugin.getLogger().info("[EcoSombras] Transición: " + actoActual + " -> " + nuevoActo);
        actoActual = nuevoActo;
        ticksEnActo = 0;
        
        // Actualizar UI global con progreso
        String actName = getActoName(nuevoActo);
        float progress = getActoProgress(nuevoActo);
        net.kyori.adventure.bossbar.BossBar.Color color = getActoColor(nuevoActo);
        
        uiManager.updateBossbar(actName, progress, color);
        
        switch (nuevoActo) {
            case ACTIVACION:
                break;
            case MANCHAS:
                iniciarActoManchas();
                break;
            case SOMBRAS_LARGAS:
                iniciarActoSombrasLargas();
                break;
            case NUCLEO:
                iniciarActoNucleo();
                break;
            case ANCLAS:
                iniciarActoAnclas();
                break;
            case RITUAL:
                iniciarActoRitual();
                break;
            case CLIFFHANGER:
                iniciarActoCliffhanger();
                break;
        }
    }
    
    private String getActoName(Acto acto) {
        switch (acto) {
            case ACTIVACION: return "ACTIVACIÓN";
            case MANCHAS: return "MANCHAS";
            case SOMBRAS_LARGAS: return "SOMBRAS LARGAS";
            case NUCLEO: return "NÚCLEO";
            case ANCLAS: return "ANCLAS";
            case RITUAL: return "RITUAL";
            case CLIFFHANGER: return "CIERRE";
            default: return "DESCONOCIDO";
        }
    }
    
    private float getActoProgress(Acto acto) {
        switch (acto) {
            case ACTIVACION: return 0.05f;
            case MANCHAS: return 0.2f;
            case SOMBRAS_LARGAS: return 0.4f;
            case NUCLEO: return 0.6f;
            case ANCLAS: return 0.75f;
            case RITUAL: return 0.9f;
            case CLIFFHANGER: return 1.0f;
            default: return 0.0f;
        }
    }
    
    private net.kyori.adventure.bossbar.BossBar.Color getActoColor(Acto acto) {
        switch (acto) {
            case ACTIVACION: return net.kyori.adventure.bossbar.BossBar.Color.WHITE;
            case MANCHAS: return net.kyori.adventure.bossbar.BossBar.Color.BLUE;
            case SOMBRAS_LARGAS: return net.kyori.adventure.bossbar.BossBar.Color.PURPLE;
            case NUCLEO: return net.kyori.adventure.bossbar.BossBar.Color.RED;
            case ANCLAS: return net.kyori.adventure.bossbar.BossBar.Color.PINK;
            case RITUAL: return net.kyori.adventure.bossbar.BossBar.Color.RED;
            case CLIFFHANGER: return net.kyori.adventure.bossbar.BossBar.Color.WHITE;
            default: return net.kyori.adventure.bossbar.BossBar.Color.WHITE;
        }
    }
    
    private void tickActoRitual() {
        // Sistema de oleadas de mobs que convergen en la arena
        ConfigurationSection ritualConfig = config.getConfigurationSection("actos.acto_5_ritual");
        if (ritualConfig == null) return;
        
        // Spawn de oleadas cada 20 segundos
        if (ticksEnActo % 400 == 0 && oleadaActual < 3) {
            oleadaActual++;
            spawnearOleada(oleadaActual);
            
            String oleadaMsg = ritualConfig.getString("mensajes.oleada_" + oleadaActual + ".texto",
                "§5§lOleada " + oleadaActual + " de 3");
            messageBus.broadcast(oleadaMsg, "eco_sombras");
        }
        
        // Después de 3 oleadas, spawn del Guardián
        if (ticksEnActo > 1200 && !guardianSpawneado) {
            spawnearGuardian();
        }
        
        // 🔧 FIX #13: Verificar MUERTE REAL del Guardian con flag para evitar múltiples triggers
        if (guardianSpawneado && guardianEntity != null && guardianEntity.isValid() && !guardianDerrotado) {
            LivingEntity guardian = (LivingEntity) guardianEntity;
            
            // Solo transicionar si está realmente muerto o vida <= 0
            if (guardian.isDead() || guardian.getHealth() <= 0) {
                guardianDerrotado = true; // Marcar como derrotado
                
                // Mensaje dramático con title
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle(
                        "§5§l◆ VICTORIA ◆",
                        "§7El Guardian ha sido derrotado",
                        10, 80, 20
                    );
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
                }
                
                messageBus.broadcast("§5§l¡El Guardián del Umbral ha caído!", "eco_sombras");
                
                // Efectos visuales de victoria
                Location loc = guardian.getLocation();
                loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 3);
                loc.getWorld().spawnParticle(Particle.WITCH, loc, 100, 3, 3, 3, 0.2);
                
                // Recompensas para todos los participantes
                for (UUID uuid : participantesOriginales) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        participacionGuardian.put(uuid, true);
                        p.getInventory().addItem(items.crearEcoResonante());
                    }
                }
                
                // Delay de 10 segundos para efectos post-muerte
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // Verificar que el evento sigue activo
                    if (actoActual == Acto.RITUAL) {
                        transicionarActo(Acto.CLIFFHANGER);
                        
                        // Mensaje de transición
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.sendMessage("§7[§5EcoSombras§7] §aAvanzando al Acto Final...");
                        }
                    }
                }, 200L); // 10 segundos = 200 ticks
            }
        }
    }
    
    private void iniciarActoRitual() {
        oleadaActual = 0;
        guardianSpawneado = false;
        guardianDerrotado = false; // 🔧 FIX #13: Reset flag
        
        ConfigurationSection ritualConfig = config.getConfigurationSection("actos.acto_5_ritual");
        if (ritualConfig == null) return;
        
        // Determinar centro de arena (promedio de posiciones de jugadores)
        List<Player> jugadores = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (jugadores.isEmpty()) return;
        
        double sumX = 0, sumY = 0, sumZ = 0;
        World world = jugadores.get(0).getWorld();
        
        for (Player p : jugadores) {
            sumX += p.getLocation().getX();
            sumY += p.getLocation().getY();
            sumZ += p.getLocation().getZ();
        }
        
        arenaCenter = new Location(
            world,
            sumX / jugadores.size(),
            sumY / jugadores.size(),
            sumZ / jugadores.size()
        );
        
        // Generar estructura de arena (círculo de bloques)
        generarArenaRitual();
        
        // 🛡️ PROTECCIÓN: Añadir zona protegida para la arena (NUEVO - Categoría 9)
        protectionSystem.addProtectedZone(arenaCenter, 30, "Arena Ritual");
        
        // 🎨 PARTÍCULAS AMBIENTALES INTENSAS EN ARENA
        particleSystem.startAmbientParticles(arenaCenter, 30, 
            me.apocalipsis.events.gameplay.ParticleEffectSystem.AmbientStyle.EMBERS);
        
        // 🎨 CÍRCULO RITUAL EN EL SUELO
        particleSystem.createFloatingSymbol(arenaCenter.clone().add(0, 0.5, 0), 
            me.apocalipsis.events.gameplay.ParticleEffectSystem.SymbolType.CIRCLE, 600);
        
        // Mensaje inicial
        String inicioMsg = ritualConfig.getString("mensajes.inicio.texto",
            "§5§l⚠ EL RITUAL COMIENZA ⚠");
        messageBus.broadcast(inicioMsg, "eco_sombras");
        
        // Sonido dramático
        for (Player p : jugadores) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        }
    }
    
    private void generarArenaRitual() {
        if (arenaCenter == null) return;
        
        ConfigurationSection estructuraConfig = config.getConfigurationSection("estructuras.arena_ritual");
        if (estructuraConfig == null) return;
        
        int radio = estructuraConfig.getInt("radio", 20);
        String materialName = estructuraConfig.getString("material", "BLACKSTONE");
        Material material = Material.getMaterial(materialName);
        if (material == null) material = Material.BLACKSTONE;
        
        World world = arenaCenter.getWorld();
        int centerX = arenaCenter.getBlockX();
        // 🔧 FIX: Usar getHighestBlockYAt para terreno sólido
        int centerY = world.getHighestBlockYAt(centerX, arenaCenter.getBlockZ());
        int centerZ = arenaCenter.getBlockZ();
        
        // Actualizar arenaCenter con Y correcto
        arenaCenter.setY(centerY);
        
        messageBus.broadcast("§8§oGenerando arena ritual...", "eco_sombras");
        
        // 🔧 FIX: Limpiar área primero (remover bloques que bloqueen)
        for (int x = -radio - 2; x <= radio + 2; x++) {
            for (int z = -radio - 2; z <= radio + 2; z++) {
                // Limpiar 5 bloques arriba del suelo
                for (int y = 1; y <= 5; y++) {
                    Location clearLoc = new Location(world, centerX + x, centerY + y, centerZ + z);
                    Material blockType = clearLoc.getBlock().getType();
                    if (!blockType.isSolid() || blockType == Material.TALL_GRASS || 
                        blockType == Material.SHORT_GRASS || blockType == Material.FERN ||
                        blockType == Material.LARGE_FERN || blockType == Material.DEAD_BUSH) {
                        clearLoc.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
        
        // 🔧 FIX: CÍRCULO COMPLETO (rellenar todo, no solo anillos)
        for (int x = -radio; x <= radio; x++) {
            for (int z = -radio; z <= radio; z++) {
                double distancia = Math.sqrt(x * x + z * z);
                
                if (distancia <= radio) {
                    Location loc = new Location(world, centerX + x, centerY, centerZ + z);
                    
                    // Patrón complejo visible
                    if (distancia >= radio - 1 && distancia <= radio) {
                        // Borde exterior - BLACKSTONE
                        loc.getBlock().setType(material);
                    } else if ((int)distancia % 5 == 0) {
                        // Anillos concéntricos - CRYING_OBSIDIAN
                        loc.getBlock().setType(Material.CRYING_OBSIDIAN);
                    } else if ((x + z) % 2 == 0) {
                        // Patrón de tablero - POLISHED_BLACKSTONE
                        loc.getBlock().setType(Material.POLISHED_BLACKSTONE);
                    } else {
                        // Relleno - BLACKSTONE normal
                        loc.getBlock().setType(Material.BLACKSTONE);
                    }
                }
            }
        }
        
        // 🔧 FIX: Pilares MÁS ALTOS y VISIBLES (8 bloques en lugar de 5)
        Material pilarMaterial = Material.OBSIDIAN;
        int pilarHeight = 8;
        
        for (int dir = 0; dir < 4; dir++) {
            int offsetX = 0, offsetZ = 0;
            switch (dir) {
                case 0: offsetX = radio; break;     // Este
                case 1: offsetX = -radio; break;    // Oeste
                case 2: offsetZ = radio; break;     // Sur
                case 3: offsetZ = -radio; break;    // Norte
            }
            
            // Base del pilar (3x3)
            for (int bx = -1; bx <= 1; bx++) {
                for (int bz = -1; bz <= 1; bz++) {
                    Location baseLoc = new Location(world, centerX + offsetX + bx, centerY + 1, centerZ + offsetZ + bz);
                    baseLoc.getBlock().setType(Material.POLISHED_BLACKSTONE_BRICKS);
                }
            }
            
            // Pilar vertical
            for (int y = 0; y < pilarHeight; y++) {
                Location loc = new Location(world, centerX + offsetX, centerY + 2 + y, centerZ + offsetZ);
                loc.getBlock().setType(pilarMaterial);
            }
            
            // Cima: Respawn Anchor cargado
            Location topLoc = new Location(world, centerX + offsetX, centerY + 2 + pilarHeight, centerZ + offsetZ);
            topLoc.getBlock().setType(Material.RESPAWN_ANCHOR);
            
            org.bukkit.block.data.type.RespawnAnchor anchor = 
                (org.bukkit.block.data.type.RespawnAnchor) topLoc.getBlock().getBlockData();
            anchor.setCharges(4);
            topLoc.getBlock().setBlockData(anchor);
            
            // Soul Lanterns alrededor
            Location[] lanterns = {
                topLoc.clone().add(1, 0, 0),
                topLoc.clone().add(-1, 0, 0),
                topLoc.clone().add(0, 0, 1),
                topLoc.clone().add(0, 0, -1)
            };
            for (Location lanternLoc : lanterns) {
                lanternLoc.getBlock().setType(Material.SOUL_LANTERN);
            }
        }
        
        // CENTRO: Símbolo ritual (5x5 de Crying Obsidian)
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) + Math.abs(z) <= 3) { // Forma de diamante
                    Location symbolLoc = new Location(world, centerX + x, centerY, centerZ + z);
                    symbolLoc.getBlock().setType(Material.CRYING_OBSIDIAN);
                }
            }
        }
        
        // EFECTOS VISUALES POST-GENERACIÓN
        world.spawnParticle(Particle.EXPLOSION_EMITTER, arenaCenter, 10, radio, 2, radio);
        world.playSound(arenaCenter, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);
        
        messageBus.broadcast("§d✦ Arena ritual completada ✦", "eco_sombras");
        
        // Mensaje con coordenadas para cada jugador
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(String.format("§5Arena en: §7X=%d Y=%d Z=%d §8[%.0fm]", 
                centerX, centerY, centerZ, p.getLocation().distance(arenaCenter)));
        }
    }
    
    private void spawnearOleada(int numeroOleada) {
        if (arenaCenter == null) return;
        
        ConfigurationSection mobsConfig = config.getConfigurationSection("mobs");
        if (mobsConfig == null) return;
        
        // 🔧 FIX #12: Escalar cantidad de mobs según jugadores activos (3 mínimo)
        int jugadoresActivos = Bukkit.getOnlinePlayers().size();
        int cantidadBase = 3 + (numeroOleada * 2); // Oleada 1: 5, Oleada 2: 7, Oleada 3: 9
        
        // Escalado: 1-2 jugadores = 60%, 3 jugadores = 100%, 4+ jugadores = +25% por jugador extra
        double mobScaling;
        if (jugadoresActivos <= 2) {
            mobScaling = 0.6; // 60% para 1-2 jugadores
        } else if (jugadoresActivos == 3) {
            mobScaling = 1.0; // 100% para 3 jugadores (base)
        } else {
            mobScaling = 1.0 + (jugadoresActivos - 3) * 0.25; // 4p=125%, 5p=150%, etc.
        }
        
        int cantidadFinal = Math.max(2, (int) Math.round(cantidadBase * mobScaling));
        
        plugin.getLogger().info("[EcoSombras] Oleada " + numeroOleada + ": " + cantidadFinal + " mobs (" + 
            jugadoresActivos + " jugadores, " + String.format("%.0f%%", mobScaling * 100) + " scaling)");
        
        for (int i = 0; i < cantidadFinal; i++) {
            Location spawnLoc = encontrarPosicionSpawn(arenaCenter, 15, 25);
            if (spawnLoc == null) continue;
            
            // Alternancia de tipos de sombra
            String tipoSombra = (i % 2 == 0) ? "sombra_larga" : "sombra_rapida";
            ConfigurationSection mobConfig = mobsConfig.getConfigurationSection(tipoSombra);
            if (mobConfig == null) continue;
            
            Zombie sombra = (Zombie) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
            configurarSombraLarga(sombra, mobConfig);
            entidadesEvento.add(sombra.getUniqueId());
            protectionSystem.registerEventEntity(sombra); // 🛡️ Registrar en protección
            
            // Partículas de spawn
            spawnLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, spawnLoc, 30, 0.5, 1, 0.5, 0.1);
        }
        
        // Sonido de oleada
        arenaCenter.getWorld().playSound(arenaCenter, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.8f);
    }
    
    private void spawnearGuardian() {
        if (arenaCenter == null) return;
        
        guardianSpawneado = true;
        
        // 🔧 FIX #10: Spawn seguro +5 bloques sobre superficie
        World bossWorld = arenaCenter.getWorld();
        Location safeLoc = arenaCenter.clone().add(0, 5, 0);
        safeLoc.setY(safeLoc.getWorld().getHighestBlockYAt(safeLoc) + 5); // +5 sobre terreno
        
        // 🔧 FIX #10: Limpiar área de spawn (5x5x10) para evitar obstrucciones
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 10; y++) {
                    Location clearLoc = safeLoc.clone().add(x, y, z);
                    if (clearLoc.getBlock().getType().isSolid()) {
                        clearLoc.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
        
        // 🔧 FIX #10: Teleportar jugadores a posición segura ANTES de spawn
        for (Player p : Bukkit.getOnlinePlayers()) {
            Location playerSafeLoc = arenaCenter.clone().add(
                random.nextInt(10) - 5,  // X aleatorio (-5 a +5)
                10,                      // Y +10 sobre arena
                random.nextInt(10) - 5   // Z aleatorio
            );
            playerSafeLoc.setY(playerSafeLoc.getWorld().getHighestBlockYAt(playerSafeLoc) + 2);
            p.teleport(playerSafeLoc);
            
            // Efecto visual de teleport + Slow falling para prevenir caída
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0)); // 3s slow fall
        }
        
        // 🌫️ AMBIENTE: Lluvia sangrienta + grietas del vacío + corrupción extrema
        environmentSystem.setDynamicWeather(bossWorld, EnvironmentSystem.WeatherType.BLOOD_RAIN, 0);
        environmentSystem.spawnAtmosphericEffect(bossWorld, EnvironmentSystem.AtmosphericEffect.VOID_CRACKS, 0);
        environmentSystem.spawnAtmosphericEffect(bossWorld, EnvironmentSystem.AtmosphericEffect.CORRUPTION_SPREAD, 0);
        
        // Corrupción masiva del terreno (void corruption)
        environmentSystem.alterWorldTemporarily(arenaCenter, 25, 
            EnvironmentSystem.CorruptionType.VOID_CORRUPTION);
        
        // 🎬 CINEMATOGRÁFICO COMPLETO: Slow motion + Freeze + Shake
        for (Player p : Bukkit.getOnlinePlayers()) {
            // 🎵 AUDIO: Transición a música de boss épica
            audioSystem.playActMusic(p, EventAudioSystem.MusicTrack.GUARDIAN);
            
            // 🎵 AUDIO: Sonido posicional del guardián spawn con reverb CAVE
            audioSystem.playPositionalSoundWithReverb(p, arenaCenter, 
                EventAudioSystem.SoundType.GUARDIAN_SPAWN, 96.0, 
                EventAudioSystem.ReverbType.CAVE);
            
            // 🎵 AUDIO: Stinger épico de guardián
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) {
                    audioSystem.playStinger(p, EventAudioSystem.StingerType.GUARDIAN_SPAWNED);
                }
            }, 40L);
            
            // Freeze frame inicial (2 segundos)
            cinematicSystem.freezeFrame(p, 40);
            
            // Slow motion al descongelar
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) {
                    cinematicSystem.slowMotion(p, 60);
                }
            }, 40L);
            
            // Letterbox + Zoom in al boss
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) {
                    cinematicSystem.showLetterbox(p, 100);
                    cinematicSystem.smoothZoom(p, 0.3f, 80); // Zoom muy cercano
                }
            }, 60L);
            
            // Camera shake extremo al spawn
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) {
                    cinematicSystem.cameraShake(p, 
                        me.apocalipsis.events.gameplay.CinematicSystem.ShakeIntensity.EXTREME, 40);
                }
            }, 100L);
            
            // Reset gradual al final
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) {
                    cinematicSystem.resetZoom(p);
                }
            }, 200L);
            
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 9, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 9, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, 250, false, false));
            
            // Blindness inicial para fade in
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 5, false, false));
        }
        
        // Oscurecer el mundo temporalmente
        long tiempoOriginal = bossWorld.getTime();
        bossWorld.setTime(18000); // Medianoche
        
        // 🎬 Secuencia de efectos superpuestos (🔧 FIX #11: Reducidos 60%)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Explosión de partículas masiva (REDUCIDA)
            for (int i = 0; i < 360; i += 30) { // 🔧 Cada 30° en lugar de 10°
                double radians = Math.toRadians(i);
                for (int r = 1; r <= 15; r++) {
                    Location particleLoc = arenaCenter.clone().add(
                        Math.cos(radians) * r,
                        5,
                        Math.sin(radians) * r
                    );
                    bossWorld.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 1, 0.2, 0.2, 0.2, 0.05); // 🔧 3→1
                    bossWorld.spawnParticle(Particle.SQUID_INK, particleLoc, 1, 0.1, 0.1, 0.1, 0); // 🔧 2→1
                }
            }
            
            // Efecto cinematográfico con título
            efectoCinematico("§5§l⚔ GUARDIÁN DEL UMBRAL ⚔", 10, 80, 20);
            
            // Partículas verticales (🔧 FIX #11: REDUCIDAS 60%)
            for (int y = 0; y < 50; y += 2) { // 🔧 Saltar de 2 en 2
                bossWorld.spawnParticle(Particle.REVERSE_PORTAL, arenaCenter.clone().add(0, y, 0), 8, 0.5, 0, 0.5, 0.3); // 🔧 20→8
                bossWorld.spawnParticle(Particle.END_ROD, arenaCenter.clone().add(0, y, 0), 4, 0.3, 0, 0.3, 0.1); // 🔧 10→4
            }
            
            bossWorld.spawnParticle(Particle.EXPLOSION_EMITTER, arenaCenter.clone().add(0, 5, 0), 6, 3, 3, 3); // 🔧 15→6
            
            // 🎬 Sonidos superpuestos cinematográficos
            bossWorld.playSound(arenaCenter, Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.4f);
            bossWorld.playSound(arenaCenter, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.5f, 0.2f);
            bossWorld.playSound(arenaCenter, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.5f);
            bossWorld.playSound(arenaCenter, Sound.AMBIENT_BASALT_DELTAS_MOOD, 2.0f, 0.3f);
            
            // Sonidos a cada jugador para efecto 3D
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.6f);
                p.playSound(p.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.2f);
            }
        }, 20L); // Después de 1 segundo
        
        // Rayo visual dramático (sin daño) con delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (int i = 0; i < 5; i++) {
                Location rayLoc = arenaCenter.clone().add(
                    (Math.random() - 0.5) * 10,
                    0,
                    (Math.random() - 0.5) * 10
                );
                rayLoc.setY(rayLoc.getWorld().getHighestBlockYAt(rayLoc));
                rayLoc.getWorld().strikeLightningEffect(rayLoc);
            }
        }, 30L);
        
        // 🎬 SPAWN del Guardian con delay dramático
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Location spawnLoc = arenaCenter.clone().add(0, 1, 0);
            
            // USAR WITHER SKELETON GRANDE EN LUGAR DE GIANT (tiene IA funcional)
            WitherSkeleton guardian = (WitherSkeleton) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.WITHER_SKELETON);
            
            // Configuración
            String nombre = "§5§l§n⬢ GUARDIÁN DEL UMBRAL ⬢";
            guardian.customName(net.kyori.adventure.text.Component.text(nombre));
            guardian.setCustomNameVisible(true);
            guardian.setRemoveWhenFarAway(false);
            guardian.setAI(true);
            
            // Calcular atributos según número de jugadores y dificultad
            int numJugadores = Math.max(1, participantesOriginales.size());
            double diffMultiplier = difficulty.multiplier;
            
            // 🔧 FIX #12: Escalado ajustado para 3 jugadores mínimo (en lugar de 5)
            // Fórmula anterior: 1.0 + (numJugadores - 1) * 0.3 → 100% base + 30% por jugador extra
            // Nueva fórmula: Escalar desde 3 jugadores base
            double playerScaling;
            if (numJugadores <= 3) {
                // Para 1-3 jugadores: Escalar a la baja desde 100%
                playerScaling = 0.6 + (numJugadores - 1) * 0.2; // 1p=60%, 2p=80%, 3p=100%
            } else {
                // Para 4+ jugadores: Escalar al alza desde 100%
                playerScaling = 1.0 + (numJugadores - 3) * 0.3; // 4p=130%, 5p=160%, etc.
            }
            
            plugin.getLogger().info("[EcoSombras] Escalado Guardian: " + numJugadores + " jugadores → " + 
                String.format("%.0f%%", playerScaling * 100));
            
            // Atributos épicos escalados (para Netherite Prot 4)
            double baseHealth = 400.0 * diffMultiplier * playerScaling;  // 240-3600 corazones (escalado)
            guardian.getAttribute(Attribute.MAX_HEALTH).setBaseValue(baseHealth);
            guardian.setHealth(baseHealth);
            
            double baseDamage = 12.0 * diffMultiplier * playerScaling;  // Escalado por dificultad y jugadores
            guardian.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(baseDamage);
            
            double baseSpeed = 0.30 * Math.min(1.5, diffMultiplier);  // Velocidad moderada
            guardian.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(baseSpeed);
            
            guardian.getAttribute(Attribute.ARMOR).setBaseValue(15.0 + (diffMultiplier * 5));
            guardian.getAttribute(Attribute.ARMOR_TOUGHNESS).setBaseValue(8.0 + (diffMultiplier * 4));
            guardian.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(0.7 + (diffMultiplier * 0.1));
            
            // Equipamiento Netherite completo
            EntityEquipment equip = guardian.getEquipment();
            if (equip != null) {
            // Armadura Netherite encantada
            ItemStack helmet = new ItemStack(Material.NETHERITE_HELMET);
            helmet.addEnchantment(Enchantment.PROTECTION, 4);
            helmet.addEnchantment(Enchantment.UNBREAKING, 3);
            
            ItemStack chestplate = new ItemStack(Material.NETHERITE_CHESTPLATE);
            chestplate.addEnchantment(Enchantment.PROTECTION, 4);
            chestplate.addEnchantment(Enchantment.UNBREAKING, 3);
            
            ItemStack leggings = new ItemStack(Material.NETHERITE_LEGGINGS);
            leggings.addEnchantment(Enchantment.PROTECTION, 4);
            leggings.addEnchantment(Enchantment.UNBREAKING, 3);
            
            ItemStack boots = new ItemStack(Material.NETHERITE_BOOTS);
            boots.addEnchantment(Enchantment.PROTECTION, 4);
            boots.addEnchantment(Enchantment.UNBREAKING, 3);
            boots.addEnchantment(Enchantment.FEATHER_FALLING, 4);
            
            // Espada Netherite mejorada
            ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
            sword.addEnchantment(Enchantment.SHARPNESS, 5);
            sword.addEnchantment(Enchantment.KNOCKBACK, 2);
            sword.addEnchantment(Enchantment.FIRE_ASPECT, 2);
            sword.addEnchantment(Enchantment.UNBREAKING, 3);
            
            equip.setHelmet(helmet);
            equip.setChestplate(chestplate);
            equip.setLeggings(leggings);
            equip.setBoots(boots);
            equip.setItemInMainHand(sword);
            
            equip.setHelmetDropChance(0);
            equip.setChestplateDropChance(0);
            equip.setLeggingsDropChance(0);
            equip.setBootsDropChance(0);
            equip.setItemInMainHandDropChance(0);
            }
            
            guardianEntity = guardian;
            entidadesEvento.add(guardian.getUniqueId());
            protectionSystem.registerEventEntity(guardian); // 🛡️ Registrar en protección
            
            // 🎮 INICIALIZAR SISTEMA DE FASES
            guardianPhaseSystem = new me.apocalipsis.events.gameplay.GuardianPhaseSystem(
                plugin, 
                guardian, 
                arenaCenter
            );
            
            // 🎨 AURA CORRUPTA DEL GUARDIÁN
            particleSystem.startPulsingAura(guardian, 
                me.apocalipsis.events.gameplay.ParticleEffectSystem.AuraStyle.CORRUPTED, 12);
            
            // 🎨 TRAIL DE SOMBRA AL MOVERSE
            particleSystem.startShadowTrail(guardian, 
                me.apocalipsis.events.gameplay.ParticleEffectSystem.ParticleTrailType.FLAME);
            
            // 🎨 RUNAS FLOTANTES ALREDEDOR
            particleSystem.createFloatingSymbol(guardian.getLocation().clone().add(0, 10, 0), 
                me.apocalipsis.events.gameplay.ParticleEffectSystem.SymbolType.RUNES, 600);
            
            // Mensaje dramático con stats
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                messageBus.broadcast("§8§o\"Ro… po… sis… ten…\"", "eco_sombras");
                messageBus.broadcast("§7Jugadores: §e" + numJugadores + " §7| Dificultad: " + difficulty.displayName, "eco_sombras");
                arenaCenter.getWorld().playSound(arenaCenter, Sound.ENTITY_WARDEN_ROAR, 2.0f, 0.3f);
            }, 40L);
            
            // 🔧 FIX #11: BossBar para visibilidad del Guardian (reemplaza efectos excesivos)
            BossBar guardianBar = Bukkit.createBossBar(
                "§5§lGuardián del Umbral", 
                BarColor.PURPLE, 
                BarStyle.SEGMENTED_20
            );
            guardianBar.setProgress(1.0);
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                guardianBar.addPlayer(p);
            }
            
            // 🔧 FIX #11: Efecto de aura reducido 60% (6+4+3 en lugar de 15+10+8)
            BukkitTask auraTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (guardian.isValid()) {
                    Location loc = guardian.getLocation();
                    
                    // Actualizar BossBar con vida del Guardian
                    double healthPercent = guardian.getHealth() / guardian.getAttribute(Attribute.MAX_HEALTH).getValue();
                    guardianBar.setProgress(Math.max(0.0, Math.min(1.0, healthPercent)));
                    
                    // Cambiar color según vida
                    if (healthPercent < 0.25) {
                        guardianBar.setColor(BarColor.RED);
                    } else if (healthPercent < 0.50) {
                        guardianBar.setColor(BarColor.YELLOW);
                    }
                    
                    // 🔧 FIX #11: Partículas reducidas 60% (antes: 15+10+8, ahora: 6+4+3)
                    loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 3, 0), 6, 1.5, 3, 1.5, 0.05); // 15→6
                    loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 2, 0), 4, 1, 2, 1, 0.03); // 10→4
                    loc.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(0, 1, 0), 3, 1, 1.5, 1, 0.02); // 8→3
                    
                    // 🔧 FIX #11: Glowing level 2 para mejor visibilidad
                    if (!guardian.hasPotionEffect(PotionEffectType.GLOWING)) {
                        guardian.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 999999, 1, false, false));
                    }
                    
                    // Efecto de respiración (cada 3 segundos)
                    if (ticksEnActo % 60 == 0) {
                        loc.getWorld().playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.5f);
                    }
                    
                    // Aplicar efectos a jugadores cercanos
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getWorld().equals(loc.getWorld()) && p.getLocation().distance(loc) < 15) {
                            // Debuff leve cerca del guardián
                            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.WEAKNESS, 60, 0, false, false
                            ));
                        }
                    }
                } else {
                    // Remover BossBar si el Guardian muere
                    guardianBar.removeAll();
                }
            }, 0L, 20L);  // Cada segundo
            
            oleadaTask = auraTask;
            
            // Habilidades especiales del Guardián
            iniciarHabilidadesGuardian(guardian);
            
            // Restaurar tiempo del mundo
            bossWorld.setTime(tiempoOriginal);
        }, 60L); // Spawn después de 3 segundos de oscuridad
    }
    
    private void iniciarHabilidadesGuardian(LivingEntity guardian) {
        // 🎮 SISTEMA DE ATAQUES TELEGRAFADOS
        // El Guardián alterna entre diferentes ataques cada 12 segundos
        List<TelegraphedAttack.AttackType> attackRotation = Arrays.asList(
            TelegraphedAttack.AttackType.SLAM,
            TelegraphedAttack.AttackType.PULSE,
            TelegraphedAttack.AttackType.BEAM,
            TelegraphedAttack.AttackType.CONE
        );
        
        BukkitTask attackTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int attackIndex = 0;
            
            @Override
            public void run() {
                if (!guardian.isValid()) {
                    return;
                }
                
                // Seleccionar ataque de la rotación
                TelegraphedAttack.AttackType currentAttack = attackRotation.get(attackIndex % attackRotation.size());
                attackIndex++;
                
                // Mensaje de advertencia
                String attackName = getAttackName(currentAttack);
                messageBus.broadcast("§c§l⚠ " + attackName + " ⚠", "eco_sombras");
                
                // Ejecutar ataque telegrafado
                telegraphedAttack.executeAttack(guardian, currentAttack, (type, hitPlayers) -> {
                    // Callback cuando el ataque termina
                    if (hitPlayers.isEmpty()) {
                        messageBus.broadcast("§7Todos esquivaron el ataque...", "eco_sombras");
                    } else {
                        messageBus.broadcast("§c" + hitPlayers.size() + " jugadores fueron golpeados.", "eco_sombras");
                    }
                });
            }
        }, 240L, 240L);  // Cada 12 segundos
        
        // Pulso de Sombra cada 15 segundos (ataque adicional no telegrafado para presión)
        BukkitTask pulsoTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!guardian.isValid()) {
                return;
            }
            
            Location loc = guardian.getLocation();
            
            // Efecto visual
            loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 3, 0), 200, 10, 3, 10, 0.1);
            loc.getWorld().spawnParticle(Particle.SONIC_BOOM, loc.clone().add(0, 3, 0), 1);
            loc.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.5f);
            
            // Daño en área
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld().equals(loc.getWorld()) && p.getLocation().distance(loc) < 12) {
                    p.damage(12.0);  // 6 corazones
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.WITHER, 100, 1
                    ));
                    
                    // Empuje radial
                    org.bukkit.util.Vector direction = p.getLocation().toVector()
                        .subtract(loc.toVector()).normalize();
                    p.setVelocity(direction.multiply(1.5).setY(0.8));
                }
            }
            
            messageBus.broadcast("§8§l⚡ PULSO DE SOMBRA ⚡", "eco_sombras");
            
        }, 300L, 300L);  // Cada 15 segundos
        
        // Invocar refuerzos cada 30 segundos
        BukkitTask invocacionTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!guardian.isValid()) {
                return;
            }
            
            Location loc = guardian.getLocation();
            messageBus.broadcast("§8El Guardián invoca refuerzos…", "eco_sombras");
            
            for (int i = 0; i < 4; i++) {
                double angulo = (2 * Math.PI / 4) * i;
                Location spawnLoc = loc.clone().add(
                    Math.cos(angulo) * 8,
                    0,
                    Math.sin(angulo) * 8
                );
                
                // Spawn una sombra larga simple
                Zombie sombra = (Zombie) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
                ConfigurationSection mobConfig = config.getConfigurationSection("actos.acto_2_sombras_largas.spawn_sombras.configuracion_mob");
                if (mobConfig != null) {
                    configurarSombraLarga(sombra, mobConfig);
                    entidadesEvento.add(sombra.getUniqueId());
                }
            }
            
        }, 600L, 600L);  // Cada 30 segundos
        
        // Fase de furia al 30% de vida
        BukkitTask furiaCheck = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!guardian.isValid()) {
                return;
            }
            
            double vidaActual = guardian.getHealth();
            double vidaMax = guardian.getAttribute(Attribute.MAX_HEALTH).getValue();
            double porcentaje = (vidaActual / vidaMax) * 100;
            
            if (porcentaje <= 30 && porcentaje > 29) {
                // Activar furia
                messageBus.broadcast("§c§l⚠ ¡EL GUARDIÁN ENTRA EN FURIA! ⚠", "eco_sombras");
                
                Location loc = guardian.getLocation();
                loc.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, loc.clone().add(0, 3, 0), 100, 2, 3, 2, 0.5);
                loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
                
                // Aumentar stats
                guardian.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.45);
                guardian.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(30.0);
                
                // Efecto visual permanente de furia
                BukkitTask furiaVisual = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    if (guardian.isValid()) {
                        Location l = guardian.getLocation();
                        l.getWorld().spawnParticle(Particle.LAVA, l.clone().add(0, 3, 0), 5, 1, 2, 1, 0);
                        l.getWorld().spawnParticle(Particle.FLAME, l.clone().add(0, 2, 0), 10, 1.5, 2, 1.5, 0.1);
                    }
                }, 0L, 10L);
            }
            
        }, 20L, 20L);  // Cada segundo
    }
    
    private void tickActoCliffhanger() {
        ConfigurationSection cliffConfig = config.getConfigurationSection("actos.acto_6_cliffhanger");
        if (cliffConfig == null) return;
        
        // ═══════════════════════════════════════════════════════════════════
        // 🎬 MOMENTO 1: VICTORIA CINEMATOGRÁFICA (0-4 segundos)
        // ═══════════════════════════════════════════════════════════════════
        
        if (ticksEnActo == 20) {
            // Slow-motion dramático + zoom
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (participacionGuardian.getOrDefault(p.getUniqueId(), false)) {
                    cinematicSystem.smoothZoom(p, 0.5f, 80);
                    cinematicSystem.slowMotion(p, 80);
                    p.sendTitle("§5§l⬢ VICTORIA ⬢", "§7El Guardián ha caído", 10, 60, 20);
                    audioSystem.playStinger(p, EventAudioSystem.StingerType.VICTORY);
                }
            }
            
            // Efectos de partículas épicos
            if (arenaCenter != null) {
                final World world = arenaCenter.getWorld();
                BukkitTask particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    if (world != null) {
                        world.spawnParticle(Particle.PORTAL, arenaCenter, 50, 2, 2, 2, 0.5);
                        world.spawnParticle(Particle.END_ROD, arenaCenter, 30, 1, 1, 1, 0.2);
                        world.spawnParticle(Particle.TOTEM_OF_UNDYING, arenaCenter, 20, 1.5, 1.5, 1.5, 0.1);
                    }
                }, 0L, 5L);
                Bukkit.getScheduler().runTaskLater(plugin, particleTask::cancel, 80L);
            }
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // 🎁 MOMENTO 2: DROPS LEGENDARIOS + AGRADECIMIENTOS (3-6 segundos)
        // ═══════════════════════════════════════════════════════════════════
        
        if (ticksEnActo == 60) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (participacionGuardian.getOrDefault(p.getUniqueId(), false)) {
                    // Drop legendario único del Guardián
                    ItemStack legendaryDrop = lootSystem.generateGuardianLegendaryDrop(p);
                    lootSystem.giveRewards(p, Arrays.asList(legendaryDrop));
                    
                    // Recompensas de agradecimiento
                    List<ItemStack> thankYouRewards = lootSystem.generateThankYouRewards();
                    lootSystem.giveRewards(p, thankYouRewards);
                    
                    p.sendMessage("");
                    p.sendMessage("§5§l━━━━━━━ §d§lGRACIAS POR JUGAR §5§l━━━━━━━");
                    p.sendMessage("§7Has recibido recompensas especiales");
                    p.sendMessage("§7como agradecimiento por participar en");
                    p.sendMessage("§5§l✦ El Eco de las Sombras Largas ✦");
                    p.sendMessage("§5§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    p.sendMessage("");
                }
            }
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // 📖 MOMENTO 3: NARRATIVA - Formación del símbolo (10 segundos)
        // ═══════════════════════════════════════════════════════════════════
        
        if (ticksEnActo == 200) {
            generarSimboloFinal();
            String simboloMsg = cliffConfig.getString("mensajes.simbolo.texto",
                "§7§oLos fragmentos se reorganizan en el aire...");
            messageBus.broadcast(simboloMsg, "eco_sombras");
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // 📖 MOMENTO 4: NARRATIVA - Monólogo del Observador (15-45 segundos)
        // ═══════════════════════════════════════════════════════════════════
        
        // 📖 NARRATIVA: Secuencia de diálogos del Observador (NUEVO - Categoría 7)
        if (ticksEnActo == 300) {
            dialogSystem.broadcastDialogSequence(DialogSystem.createCliffhangerSequence());
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // 📖 MOMENTO 5: NARRATIVA - Aparición figura misteriosa (60 segundos)
        // ═══════════════════════════════════════════════════════════════════
        
        if (ticksEnActo == 1200) {
            aparicionFiguraMisteriosa();
            
            // 📖 NARRATIVA: Choice final con la figura (NUEVO - Categoría 7)
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (participacionGuardian.getOrDefault(p.getUniqueId(), false)) {
                    choiceSystem.presentChoice(p, ChoiceSystem.createFigureChoice(loreSystem));
                }
            }
        }
        
        if (ticksEnActo == 1400) {
            // 📖 NARRATIVA: Revelación final para jugadores con alto karma
            for (Player p : Bukkit.getOnlinePlayers()) {
                int karma = choiceSystem.getKarma(p);
                if (Math.abs(karma) >= 3) {
                    loreSystem.revealFragment(p, "figure_revelation");
                }
            }
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // 🧹 MOMENTO 6: CLEANUP DE SISTEMAS (80 segundos)
        // ═══════════════════════════════════════════════════════════════════
        
        if (ticksEnActo == 1600) {
            messageBus.broadcast("§7§oLas sombras se desvanecen...", "eco_sombras");
            
            // Cleanup completo de todos los sistemas
            audioSystem.cleanupAll();
            environmentSystem.cleanupAll();
            cinematicSystem.cleanupAll();
            
            messageBus.broadcast("§7§oEl mundo vuelve a la normalidad.", "eco_sombras");
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // 🏁 FINALIZACIÓN DEL EVENTO (120 segundos)
        // ═══════════════════════════════════════════════════════════════════
        
        // 🔧 FIX: Cambiar de 1800 a 2400 ticks (120 segundos en vez de 90)
        if (ticksEnActo >= 2400) {
            finalizarEvento();
        }
    }
    
    private void iniciarActoCliffhanger() {
        ConfigurationSection cliffConfig = config.getConfigurationSection("actos.acto_6_cliffhanger");
        if (cliffConfig == null) return;
        
        // Mensaje inicial
        String inicioMsg = cliffConfig.getString("mensajes.inicio.texto",
            "§8§l...silencio...");
        messageBus.broadcast(inicioMsg, "eco_sombras");
        
        // Efecto de calma tras la tormenta
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1.0f, 0.5f);
        }
    }
    
    private void generarSimboloFinal() {
        if (arenaCenter == null) {
            // Si no hay arena, usar centro de jugadores
            List<Player> jugadores = new ArrayList<>(Bukkit.getOnlinePlayers());
            if (jugadores.isEmpty()) return;
            
            double sumX = 0, sumY = 0, sumZ = 0;
            World world = jugadores.get(0).getWorld();
            
            for (Player p : jugadores) {
                sumX += p.getLocation().getX();
                sumY += p.getLocation().getY();
                sumZ += p.getLocation().getZ();
            }
            
            arenaCenter = new Location(
                world,
                sumX / jugadores.size(),
                sumY / jugadores.size(),
                sumZ / jugadores.size()
            );
        }
        
        ConfigurationSection simboloConfig = config.getConfigurationSection("estructuras.simbolo_final");
        if (simboloConfig == null) return;
        
        Location center = arenaCenter.clone().add(0, 15, 0); // 15 bloques en el aire
        World world = center.getWorld();
        
        // Centro del símbolo
        center.getBlock().setType(Material.CRYING_OBSIDIAN);
        
        // Estrella de 5 puntas
        for (int i = 0; i < 5; i++) {
            double angulo = (i * 72 - 90) * Math.PI / 180;
            int x = (int) Math.round(Math.cos(angulo) * 5);
            int z = (int) Math.round(Math.sin(angulo) * 5);
            
            Location punta = center.clone().add(x, 0, z);
            punta.getBlock().setType(Material.END_ROD);
            
            // Líneas hacia el centro
            for (int j = 1; j < 5; j++) {
                int lineX = (int) Math.round(Math.cos(angulo) * j);
                int lineZ = (int) Math.round(Math.sin(angulo) * j);
                Location lineLoc = center.clone().add(lineX, 0, lineZ);
                lineLoc.getBlock().setType(Material.PURPUR_PILLAR);
            }
        }
        
        // Partículas continuas
        final Location finalCenter = center;
        BukkitTask simboloTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (actoActual != Acto.CLIFFHANGER) return;
            
            world.spawnParticle(Particle.END_ROD, finalCenter, 20, 5, 0.5, 5, 0.05);
            world.spawnParticle(Particle.PORTAL, finalCenter, 10, 3, 0.5, 3, 0.5);
            world.spawnParticle(Particle.SOUL, finalCenter, 5, 2, 0.5, 2, 0.02);
        }, 0L, 5L);
        
        // Cancelar cuando termine el acto
        if (manchasTask != null) manchasTask.cancel();
        manchasTask = simboloTask;
    }
    
    private void aparicionFiguraMisteriosa() {
        if (arenaCenter == null) return;
        
        ConfigurationSection figuraConfig = config.getConfigurationSection("estructuras.figura_misteriosa");
        if (figuraConfig == null) return;
        
        // Spawn 20 bloques sobre el símbolo
        Location figuraLoc = arenaCenter.clone().add(0, 35, 0);
        World world = figuraLoc.getWorld();
        
        // Efectos dramáticos
        world.spawnParticle(Particle.EXPLOSION_EMITTER, figuraLoc, 3);
        world.spawnParticle(Particle.PORTAL, figuraLoc, 100, 2, 2, 2, 1);
        world.playSound(figuraLoc, Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.3f);
        
        // Spawn de la figura (Armor Stand invisible con efectos)
        ArmorStand figura = (ArmorStand) world.spawnEntity(figuraLoc, EntityType.ARMOR_STAND);
        figura.setVisible(false);
        figura.setGravity(false);
        figura.setInvulnerable(true);
        figura.setCustomName("§5§l§k|||§r §5§l? ? ?§r §5§l§k|||");
        figura.setCustomNameVisible(true);
        
        entidadesEvento.add(figura.getUniqueId());
        protectionSystem.registerEventEntity(figura); // 🛡️ Registrar en protección
        
        // Aura constante
        BukkitTask figuraTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (figura.isValid()) {
                Location loc = figura.getLocation();
                world.spawnParticle(Particle.SMOKE, loc, 30, 1, 2, 1, 0.05);
                world.spawnParticle(Particle.SOUL, loc, 15, 0.5, 1, 0.5, 0.02);
                world.spawnParticle(Particle.END_ROD, loc, 10, 0.3, 1, 0.3, 0.1);
            }
        }, 0L, 2L);
        
        if (manchasTask != null) manchasTask.cancel();
        manchasTask = figuraTask;
        
        // Mensaje final ominoso
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String finalMsg = figuraConfig.getString("mensaje_final",
                "§5§l§o\"Nos volveremos a encontrar... en las sombras.\"");
            messageBus.broadcast(finalMsg, "eco_sombras");
            
            // Desvanecimiento
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                figura.remove();
                world.spawnParticle(Particle.PORTAL, figuraLoc, 50, 1, 1, 1, 0.5);
            }, 100L);
        }, 200L);
    }
    
    private void finalizarEvento() {
        // Prevenir ejecución múltiple
        if (eventoFinalizado) {
            return;
        }
        eventoFinalizado = true;
        
        // Calcular y otorgar recompensas
        otorgarRecompensasFinales();
        
        // Mensaje de finalización
        messageBus.broadcast("§5§l━━━━━━━━━━━━━━━━━━━━━━━━━━", "eco_sombras");
        messageBus.broadcast("§5§l   EL ECO DE LAS SOMBRAS LARGAS", "eco_sombras");
        messageBus.broadcast("§7§l         HA CONCLUIDO", "eco_sombras");
        messageBus.broadcast("§5§l━━━━━━━━━━━━━━━━━━━━━━━━━━", "eco_sombras");
        
        // Detener el evento
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            stop();
        }, 60L);
    }
    
    private void otorgarRecompensasFinales() {
        ConfigurationSection recompensasConfig = config.getConfigurationSection("recompensas");
        if (recompensasConfig == null) return;
        
        int psBase = recompensasConfig.getInt("ps.base", 100);
        int psPorSombra = recompensasConfig.getInt("ps.por_sombra", 5);
        int psPorAncla = recompensasConfig.getInt("ps.por_ancla", 20);
        int psPorGuardian = recompensasConfig.getInt("ps.por_guardian", 50);
        int psBonusGrupal = recompensasConfig.getInt("ps.bonus_grupal", 25);
        int psBonusKarma = recompensasConfig.getInt("ps.bonus_karma", 10); // NUEVO
        
        for (UUID uuid : participantesOriginales) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) continue;
            
            int psTotal = psBase;
            
            // PS por sombras eliminadas
            int sombras = participacionSombras.getOrDefault(uuid, 0);
            psTotal += sombras * psPorSombra;
            
            // PS por anclas selladas
            int anclas = participacionAnclas.getOrDefault(uuid, 0);
            psTotal += anclas * psPorAncla;
            
            // PS por derrotar al guardián
            boolean mateGuardian = participacionGuardian.getOrDefault(uuid, false);
            if (mateGuardian) {
                psTotal += psPorGuardian;
            }
            
            // Bonus grupal si hay 3+ jugadores
            if (participantesOriginales.size() >= 3) {
                psTotal += psBonusGrupal;
            }
            
            // NUEVO - Bonus por karma positivo/negativo extremo
            int karma = choiceSystem.getKarma(p);
            if (Math.abs(karma) >= 5) {
                psTotal += psBonusKarma;
            }
            
            // ═══════════════════════════════════════════════════════════
            // 🎁 SISTEMA DE LOOT ITEMS
            // ═══════════════════════════════════════════════════════════
            
            // Calcular participación (0-100)
            int participationScore = Math.min(100, 
                (sombras * 5) +  // Cada sombra = 5 puntos
                (anclas * 15) +   // Cada ancla = 15 puntos
                (mateGuardian ? 30 : 0)  // Guardián = 30 puntos
            );
            
            // Items por participación general
            List<org.bukkit.inventory.ItemStack> rewards = lootSystem.generateParticipationReward(participationScore);
            
            // Items extra por matar al Guardián
            if (mateGuardian) {
                rewards.addAll(lootSystem.generateBossKillerReward());
            }
            
            // Otorgar items
            lootSystem.giveRewards(p, rewards);
            
            // Experiencia extra
            int xpAmount = (int) (500 * difficulty.multiplier * (participationScore / 100.0));
            p.giveExp(xpAmount);
            
            // Otorgar PS (integración con sistema de misiones)
            // TODO: Integrar con MissionService o sistema de economía
            
            // Mensaje de recompensas
            p.sendMessage("§5§l━━━━━━━ RECOMPENSAS ━━━━━━━");
            p.sendMessage("§7PS Base: §e+" + psBase);
            if (sombras > 0) p.sendMessage("§7Sombras eliminadas: §e+" + (sombras * psPorSombra) + " §8(" + sombras + " sombras)");
            if (anclas > 0) p.sendMessage("§7Anclas selladas: §e+" + (anclas * psPorAncla) + " §8(" + anclas + " anclas)");
            if (mateGuardian) {
                p.sendMessage("§7Guardián derrotado: §e+" + psPorGuardian);
                p.sendMessage("§6§l✦ LOOT DE BOSS: " + rewards.size() + " items legendarios");
            }
            if (participantesOriginales.size() >= 3) p.sendMessage("§7Bonus grupal: §e+" + psBonusGrupal);
            p.sendMessage("§d§lEXPERIENCIA: §a+" + xpAmount + " XP");
            p.sendMessage("§5§lTOTAL PS: §e§l+" + psTotal + " PS");
            p.sendMessage("§7Participación: §b" + participationScore + "§7/§b100");
            p.sendMessage("§5§l━━━━━━━━━━━━━━━━━━━━━━━━━");
            
            // Sonido de recompensa
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }
    
    public void onSombraLargaMuerta(Player killer) {
        sombrasLargasMuertas++;
        if (killer != null) {
            participacionSombras.merge(killer.getUniqueId(), 1, Integer::sum);
            
            // Drop chance de items por mob
            List<org.bukkit.inventory.ItemStack> mobDrops = lootSystem.generateMobKillReward();
            if (!mobDrops.isEmpty()) {
                lootSystem.giveRewards(killer, mobDrops);
                killer.sendMessage("§8§l⬢ §7Has obtenido: §8" + mobDrops.get(0).getItemMeta().getDisplayName());
            }
            
            // Experiencia por kill
            killer.giveExp((int) (10 * difficulty.multiplier));
            
            // 🎵 AUDIO: Sonido posicional de muerte con reverb
            Location deathLoc = killer.getLocation();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld().equals(deathLoc.getWorld())) {
                    audioSystem.playPositionalSoundWithReverb(p, deathLoc, 
                        EventAudioSystem.SoundType.SHADOW_DEATH, 32.0, 
                        audioSystem.detectReverbType(p, deathLoc));
                }
            }
            
            // Stinger sutil de kill
            audioSystem.playStinger(killer, EventAudioSystem.StingerType.SHADOW_KILLED);
            
            // Sonido
            killer.playSound(killer.getLocation(), Sound.ENTITY_PHANTOM_DEATH, 0.8f, 0.5f);
            
            // Efecto visual de muerte
            Location loc = killer.getLocation();
            loc.getWorld().spawnParticle(Particle.SMOKE, loc, 20, 0.5, 0.5, 0.5, 0.1);
        }
    }
    
    /**
     * Notifica que el Guardián del Umbral ha sido derrotado
     * Transiciona al Acto 6 (CLIFFHANGER) donde ocurre la victoria
     */
    public void onGuardianDerrotado() {
        if (!guardianSpawneado) return;
        
        // Registrar participación de todos los jugadores cercanos
        if (guardianEntity != null) {
            Location loc = guardianEntity.getLocation();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld().equals(loc.getWorld()) && p.getLocation().distance(loc) < 100) {
                    participacionGuardian.put(p.getUniqueId(), true);
                }
            }
        }
        
        // Mensaje de transición
        efectoCinematico("§8§l... El silencio ...", 10, 100, 30);
        
        // TRANSICIÓN AUTOMÁTICA: Guardián muerto → CLIFFHANGER (Acto 6 - Victoria)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            limpiarEntidadesActoAnterior();
            transicionarActo(Acto.CLIFFHANGER);
        }, 100L);
    }
    
    private void cleanup() {
        plugin.getLogger().info("[EcoSombras] 🔧 FIX #14: Iniciando cleanup completo...");
        
        int entidadesRemovidas = 0;
        int bloquesCuriosos = 0;
        
        // 🔧 FIX #14: Remover TODAS las entidades con metadata de evento
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                // Verificar por UUID en la lista de entidades del evento
                if (entidadesEvento.contains(entity.getUniqueId())) {
                    entity.remove();
                    entidadesRemovidas++;
                    continue;
                }
                
                // 🔧 FIX #14: Verificar por metadata (backup por si UUID no coincide)
                if (entity.hasMetadata("eco_sombras_evento")) {
                    entity.remove();
                    entidadesRemovidas++;
                    continue;
                }
                
                // 🔧 FIX #14: Verificar por nombres custom (manchas, sombras, guardian)
                if (entity.getCustomName() != null) {
                    String nombre = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(entity.customName());
                    if (nombre.contains("Mancha") || nombre.contains("Sombra") || 
                        nombre.contains("Guardián") || nombre.contains("Núcleo")) {
                        entity.remove();
                        entidadesRemovidas++;
                    }
                }
            }
        }
        
        // 🔧 FIX #14: Cancelar TODAS las tareas programadas (por si quedan algunas)
        if (mainTask != null && !mainTask.isCancelled()) mainTask.cancel();
        if (manchasTask != null && !manchasTask.isCancelled()) manchasTask.cancel();
        if (spawnTask != null && !spawnTask.isCancelled()) spawnTask.cancel();
        if (oleadaTask != null && !oleadaTask.isCancelled()) oleadaTask.cancel();
        if (itemSupplyTask != null && !itemSupplyTask.isCancelled()) itemSupplyTask.cancel();
        
        // 🔧 FIX #14: Limpiar estructuras (anclas, arena ritual, símbolos)
        // Remover anclas del mundo
        for (Location anclaLoc : anclaLocations) {
            if (anclaLoc == null || anclaLoc.getWorld() == null) continue;
            
            // Limpiar estructura 7x7 de cada ancla
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    for (int y = 0; y <= 4; y++) {
                        Location blockLoc = anclaLoc.clone().add(x, y, z);
                        Material type = blockLoc.getBlock().getType();
                        
                        // Solo remover bloques del evento (Crying Obsidian, End Rod, etc.)
                        if (type == Material.CRYING_OBSIDIAN || type == Material.BLACKSTONE ||
                            type == Material.RESPAWN_ANCHOR || type == Material.PURPLE_CANDLE ||
                            type == Material.END_ROD) {
                            blockLoc.getBlock().setType(Material.AIR);
                            bloquesCuriosos++;
                        }
                    }
                }
            }
        }
        
        // 🔧 FIX #14: Limpiar arena ritual si existe
        if (arenaCenter != null && arenaCenter.getWorld() != null) {
            int radio = 25;
            for (int x = -radio; x <= radio; x++) {
                for (int z = -radio; z <= radio; z++) {
                    if (x*x + z*z > radio*radio) continue; // Solo dentro del círculo
                    
                    Location blockLoc = arenaCenter.clone().add(x, 0, z);
                    Material type = blockLoc.getBlock().getType();
                    
                    // Remover bloques de ritual
                    if (type == Material.BLACKSTONE || type == Material.SOUL_SAND ||
                        type == Material.SOUL_LANTERN || type == Material.CRYING_OBSIDIAN ||
                        type == Material.BASALT || type == Material.POLISHED_BLACKSTONE) {
                        blockLoc.getBlock().setType(Material.AIR);
                        bloquesCuriosos++;
                    }
                }
            }
        }
        
        // 🎨 LIMPIAR TODOS LOS EFECTOS DE PARTÍCULAS
        particleSystem.cleanupAll();
        
        // 🔧 FIX #14: Limpiar sistemas de UI y feedback
        uiManager.cleanupAll();
        feedbackSystem.cleanupAll();
        
        // 🔧 FIX #14: Limpiar QTE system (cancelar QTEs activos)
        qteSystem.cleanup();
        
        // 🔧 FIX #14: Limpiar telegraphed attacks (si tiene método cleanup)
        // telegraphedAttack.cleanup(); // No disponible en esta versión
        
        // 🔧 FIX #14: Limpiar guardian phase system si existe (si tiene método cleanup)
        // if (guardianPhaseSystem != null) {
        //     guardianPhaseSystem.cleanup();
        // }
        
        // 🔧 FIX #14: Limpiar listas y mapas de datos
        entidadesEvento.clear();
        manchasLocations.clear();
        anclaLocations.clear();
        anclasSelladas.clear();
        participacionSombras.clear();
        participacionAnclas.clear();
        participacionGuardian.clear();
        participantesOriginales.clear();
        
        plugin.getLogger().info("[EcoSombras] ✅ Cleanup completo: " + 
            entidadesRemovidas + " entidades, " + bloquesCuriosos + " bloques removidos");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // GETTERS PARA LISTENER Y COMANDOS
    // ═══════════════════════════════════════════════════════════════════
    
    public Acto getActoActual() {
        return actoActual;
    }
    
    public List<Location> getAnclaLocations() {
        return new ArrayList<>(anclaLocations);
    }
    
    public Set<UUID> getEntidadesEvento() {
        return new HashSet<>(entidadesEvento);
    }
    
    public EcoSombrasItems getItems() {
        return items;
    }
    
    public QTESystem getQTESystem() {
        return qteSystem;
    }
    
    public TelegraphedAttack getTelegraphedAttack() {
        return telegraphedAttack;
    }
    
    public me.apocalipsis.ui.FeedbackSystem getFeedbackSystem() {
        return feedbackSystem;
    }
    
    public me.apocalipsis.events.gameplay.GuardianPhaseSystem getGuardianPhaseSystem() {
        return guardianPhaseSystem;
    }
    
    public int getJugadoresMinimos() {
        return config.getInt("metadata.jugadores_minimos", 3);
    }
    
    public int getSombrasLargasMuertas() {
        return sombrasLargasMuertas;
    }
    
    public int getAnclasSelladas() {
        return anclasSelladas.size();
    }
    
    public int getOleadaActual() {
        return oleadaActual;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // GETTERS PARA SISTEMAS NARRATIVOS (NUEVO - Categoría 7)
    // ═══════════════════════════════════════════════════════════════════
    
    public DialogSystem getDialogSystem() {
        return dialogSystem;
    }
    
    public LoreSystem getLoreSystem() {
        return loreSystem;
    }
    
    public ChoiceSystem getChoiceSystem() {
        return choiceSystem;
    }
    
    /**
     * Fuerza la transición a un acto específico (para comandos admin)
     */
    public void forzarActo(int numeroActo) {
        Acto nuevoActo;
        
        switch (numeroActo) {
            case 0:
                nuevoActo = Acto.ACTIVACION;
                break;
            case 1:
                nuevoActo = Acto.MANCHAS;
                break;
            case 2:
                nuevoActo = Acto.SOMBRAS_LARGAS;
                break;
            case 3:
                nuevoActo = Acto.NUCLEO;
                break;
            case 4:
                nuevoActo = Acto.ANCLAS;
                break;
            case 5:
                nuevoActo = Acto.RITUAL;
                break;
            case 6:
                nuevoActo = Acto.CLIFFHANGER;
                break;
            default:
                plugin.getLogger().warning("[EcoSombras] Acto inválido: " + numeroActo);
                return;
        }
        
        // Limpiar entidades del acto anterior
        limpiarEntidadesActoAnterior();
        
        transicionarActo(nuevoActo);
        plugin.getLogger().info("[EcoSombras] Acto forzado a: " + nuevoActo);
    }
    
    /**
     * Efecto cinematográfico con título en pantalla, pantalla negra y sonidos
     */
    private void efectoCinematico(String titulo, int fadeIn, int stay, int fadeOut) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(titulo, "", fadeIn, stay, fadeOut);
            
            Location loc = p.getLocation();
            
            // SCREEN SHAKE real con velocidad
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (int i = 0; i < 5; i++) {
                    final int index = i;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        Vector shake = new Vector(
                            (random.nextDouble() - 0.5) * 0.3,
                            (random.nextDouble() - 0.5) * 0.2,
                            (random.nextDouble() - 0.5) * 0.3
                        );
                        p.setVelocity(shake);
                    }, index * 2L);
                }
            }, fadeIn);
            
            // FADE TO BLACK/WHITE con Blindness
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, fadeIn + stay + fadeOut, 0, false, false));
            
            // Sonidos superpuestos cinematográficos
            p.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.7f);
            p.playSound(loc, Sound.AMBIENT_CAVE, 1.2f, 0.4f);
            p.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.5f);
            p.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.4f, 0.3f);
            
            // Partículas masivas superpuestas
            loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 50, 3, 2, 3, 0.15);
            loc.getWorld().spawnParticle(Particle.SQUID_INK, loc, 30, 2, 1.5, 2, 0.1);
            loc.getWorld().spawnParticle(Particle.ASH, loc.clone().add(0, 10, 0), 40, 5, 3, 5, 0.05);
            loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc, 20, 1, 1, 1, 0.3);
        }
    }
    
    /**
     * Avanza al siguiente acto en la secuencia
     */
    public void avanzarActo() {
        Acto siguiente;
        
        switch (actoActual) {
            case ACTIVACION:
                siguiente = Acto.MANCHAS;
                break;
            case MANCHAS:
                siguiente = Acto.SOMBRAS_LARGAS;
                break;
            case SOMBRAS_LARGAS:
                siguiente = Acto.NUCLEO;
                break;
            case NUCLEO:
                siguiente = Acto.ANCLAS;
                break;
            case ANCLAS:
                siguiente = Acto.RITUAL;
                break;
            case RITUAL:
                siguiente = Acto.CLIFFHANGER;
                break;
            case CLIFFHANGER:
                plugin.getLogger().info("[EcoSombras] Ya estás en el último acto");
                return;
            default:
                plugin.getLogger().warning("[EcoSombras] Acto actual inválido: " + actoActual);
                return;
        }
        
        // Limpiar entidades del acto anterior
        limpiarEntidadesActoAnterior();
        
        transicionarActo(siguiente);
        plugin.getLogger().info("[EcoSombras] Avanzado de " + actoActual + " a " + siguiente);
    }
    
    /**
     * Limpia entidades del acto anterior antes de transicionar
     */
    private void limpiarEntidadesActoAnterior() {
        // Cancelar tareas del acto anterior
        if (manchasTask != null) {
            manchasTask.cancel();
            manchasTask = null;
        }
        if (spawnTask != null) {
            spawnTask.cancel();
            spawnTask = null;
        }
        if (oleadaTask != null) {
            oleadaTask.cancel();
            oleadaTask = null;
        }
        
        // Remover entidades del acto anterior
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entidadesEvento.contains(entity.getUniqueId())) {
                    entity.remove();
                }
            }
        }
        
        // Limpiar listas
        entidadesEvento.clear();
        manchasLocations.clear();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE ITEMS BÁSICOS (FIX #5)
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Da kit inicial de supervivencia a todos los participantes
     */
    private void darKitInicial() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            // 🔧 FIX #9: Kit ampliado con 6 Ender Eyes + Blaze Powder para anclas
            ItemStack[] startKit = {
                new ItemStack(Material.IRON_SWORD),
                new ItemStack(Material.BOW),
                new ItemStack(Material.ARROW, 64),
                new ItemStack(Material.COOKED_BEEF, 32),
                new ItemStack(Material.GOLDEN_APPLE, 4),
                new ItemStack(Material.TORCH, 32),
                new ItemStack(Material.ENDER_PEARL, 4),
                new ItemStack(Material.ENDER_EYE, 6),       // 🔧 FIX #9: 6 Ender Eyes para anclas
                new ItemStack(Material.BLAZE_POWDER, 8)     // 🔧 FIX #9: Blaze Powder adicional
            };
            
            for (ItemStack item : startKit) {
                p.getInventory().addItem(item);
            }
            
            p.sendMessage("§d§l[Eco de las Sombras] §aKit inicial recibido");
            // 🔧 FIX #9: Notificar sobre items para anclas
            p.sendMessage("§7Incluye §eEnder Eyes §7y §eBlaze Powder §7para las anclas");
        }
    }
    
    /**
     * Inicia sistema de suministro periódico de items cada 5 minutos
     */
    private void iniciarSuministroItems() {
        // Cada 5 minutos, dar items básicos a todos los participantes
        itemSupplyTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : participantesOriginales) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) continue;
                
                // 🔧 FIX #9: Kit de supervivencia ampliado con 60% más Ender Eyes y Blaze Powder
                ItemStack[] supplies = {
                    new ItemStack(Material.COOKED_BEEF, 16),
                    new ItemStack(Material.GOLDEN_APPLE, 2),
                    new ItemStack(Material.ARROW, 32),
                    new ItemStack(Material.TORCH, 16),
                    new ItemStack(Material.OAK_PLANKS, 32),
                    new ItemStack(Material.COBBLESTONE, 32),
                    new ItemStack(Material.ENDER_PEARL, 2),
                    new ItemStack(Material.ENDER_EYE, 4),        // 🔧 FIX #9: +60% (antes 0, ahora 4)
                    new ItemStack(Material.BLAZE_POWDER, 4)       // 🔧 FIX #9: +4 Blaze Powder
                };
                
                // Poción de curación
                ItemStack healPotion = new ItemStack(Material.POTION);
                org.bukkit.inventory.meta.PotionMeta meta = 
                    (org.bukkit.inventory.meta.PotionMeta) healPotion.getItemMeta();
                meta.setBasePotionType(org.bukkit.potion.PotionType.HEALING);
                healPotion.setItemMeta(meta);
                
                // Dar items solo si no tiene inventario lleno de ese tipo
                for (ItemStack item : supplies) {
                    if (!p.getInventory().contains(item.getType(), 64)) {
                        p.getInventory().addItem(item);
                    }
                }
                p.getInventory().addItem(healPotion);
                
                p.sendMessage("§a§l[+] Suministros recibidos");
                p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
            }
        }, 6000L, 6000L); // Cada 5 minutos (6000 ticks)
    }
}

