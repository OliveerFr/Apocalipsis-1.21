package me.apocalipsis.events;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
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
        
        // Limpiar entidades
        cleanup();
        
        // Limpiar sistema cinematográfico
        cinematicSystem.cleanupAll();
        
        // 🎵 AUDIO: Limpiar sistema de audio
        audioSystem.cleanupAll();
        
        // 🌫️ AMBIENTE: Restaurar ambiente completo
        environmentSystem.cleanupAll();
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
        
        // Efecto visual continuo
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!manchasLocations.contains(spawnLoc) || ticks++ > 600) { // 30 seg max
                    manchasLocations.remove(spawnLoc);
                    manchasActivas--;
                    return;
                }
                
                // Partículas
                spawnLoc.getWorld().spawnParticle(Particle.SQUID_INK, spawnLoc, 20, 1, 0.1, 1, 0);
                
                // IA de huida
                for (Player p : spawnLoc.getWorld().getPlayers()) {
                    if (p.getLocation().distance(spawnLoc) < 5) {
                        // Huir
                        huidaMancha(spawnLoc, p.getLocation());
                    }
                }
            }
        }, 0L, 5L);
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
        
        // TRANSICIÓN AUTOMÁTICA al matar 20 sombras
        if (sombrasLargasMuertas >= 20) {
            if (spawnTask != null) spawnTask.cancel();
            efectoCinematico("§5§l⚡ LAS ANCLAS DIMENSIONALES SE REVELAN", 10, 60, 20);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                limpiarEntidadesActoAnterior();
                transicionarActo(Acto.ANCLAS);
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
            
            nucleoEntity = nucleo;
            entidadesEvento.add(nucleo.getUniqueId());
            
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
        
        // TRANSICIÓN AUTOMÁTICA: Núcleo destruido → RITUAL
        if (vidaActual <= 0 || !nucleo.isValid()) {
            if (spawnTask != null) spawnTask.cancel();
            efectoCinematico("§5§l⚡ EL RITUAL COMIENZA ⚡", 10, 60, 20);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                limpiarEntidadesActoAnterior();
                transicionarActo(Acto.RITUAL);
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
        
        // Generar 5 anclas alrededor del núcleo
        int cantidad = config.getInt("actos.acto_4_anclas.anclas.cantidad", 5);
        
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
        
        // Base 3x3 de DEEPSLATE_TILES
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location loc = center.clone().add(x, 0, z);
                loc.getBlock().setType(Material.DEEPSLATE_TILES);
            }
        }
        
        // Centro: RESPAWN_ANCHOR
        center.clone().add(0, 1, 0).getBlock().setType(Material.RESPAWN_ANCHOR);
        
        // Velas en esquinas
        Location[] velas = {
            center.clone().add(1, 1, 1),
            center.clone().add(1, 1, -1),
            center.clone().add(-1, 1, 1),
            center.clone().add(-1, 1, -1)
        };
        
        for (Location velaLoc : velas) {
            velaLoc.getBlock().setType(Material.PURPLE_CANDLE);
            // Encender vela
            org.bukkit.block.data.type.Candle candle = (org.bukkit.block.data.type.Candle) velaLoc.getBlock().getBlockData();
            candle.setLit(true);
            velaLoc.getBlock().setBlockData(candle);
        }
        
        // 🎬 BEAM DE LUZ vertical continuo + Pulso de energía
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (anclasSelladas.contains(id) || actoActual != Acto.ANCLAS) {
                return;
            }
            
            // Beam vertical hasta el cielo
            for (int y = 1; y <= 50; y++) {
                if (y % 2 == 0) { // Optimizado: solo cada 2 bloques
                    world.spawnParticle(Particle.END_ROD, center.clone().add(0, y, 0), 1, 0.1, 0, 0.1, 0);
                    world.spawnParticle(Particle.REVERSE_PORTAL, center.clone().add(0, y, 0), 2, 0.15, 0, 0.15, 0);
                }
            }
            
            // 🎬 Pulso de energía radial (cada 2 segundos)
            if (ticksEnActo % 40 == 0) {
                for (int angle = 0; angle < 360; angle += 30) {
                    double radians = Math.toRadians(angle);
                    for (double r = 0; r <= 5; r += 0.5) {
                        Location pulseLoc = center.clone().add(
                            Math.cos(radians) * r,
                            0.5,
                            Math.sin(radians) * r
                        );
                        world.spawnParticle(Particle.SONIC_BOOM, pulseLoc, 1, 0, 0, 0, 0);
                    }
                }
                world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.5f, 1.5f);
            }
        }, 0L, 10L);
    }
    
    private void tickActoAnclas() {
        // Verificar si todas están selladas
        if (anclasSelladas.size() >= anclaLocations.size()) {
            // Hacer núcleo vulnerable y transicionar
            if (nucleoEntity != null && nucleoEntity.isValid()) {
                ((LivingEntity) nucleoEntity).setHealth(0);
            }
            
            messageBus.broadcast("§5§lTodas las anclas han sido selladas.", "eco_sombras");
            messageBus.broadcast("§7El Núcleo ha sido destruido.", "eco_sombras");
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                transicionarActo(Acto.RITUAL);
            }, 100L);
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
        
        // Check si el Guardián fue derrotado
        if (guardianSpawneado && (guardianEntity == null || !guardianEntity.isValid())) {
            messageBus.broadcast("§5§l¡El Guardián de las Sombras Largas ha caído!", "eco_sombras");
            
            // Recompensas para todos los participantes
            for (UUID uuid : participantesOriginales) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    participacionGuardian.put(uuid, true);
                    p.getInventory().addItem(items.crearEcoResonante());
                }
            }
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                transicionarActo(Acto.CLIFFHANGER);
            }, 60L);
        }
    }
    
    private void iniciarActoRitual() {
        oleadaActual = 0;
        guardianSpawneado = false;
        
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
        int centerY = world.getHighestBlockYAt(arenaCenter) - 1;
        int centerZ = arenaCenter.getBlockZ();
        
        // Círculo en el suelo
        for (int x = -radio; x <= radio; x++) {
            for (int z = -radio; z <= radio; z++) {
                double distancia = Math.sqrt(x * x + z * z);
                
                // Anillo exterior
                if (distancia >= radio - 1 && distancia <= radio) {
                    Location loc = new Location(world, centerX + x, centerY, centerZ + z);
                    loc.getBlock().setType(material);
                }
                
                // Anillos interiores (cada 5 bloques)
                if (distancia > 0 && (int)distancia % 5 == 0 && distancia < radio) {
                    Location loc = new Location(world, centerX + x, centerY, centerZ + z);
                    loc.getBlock().setType(Material.CRYING_OBSIDIAN);
                }
            }
        }
        
        // Pilares en 4 puntos cardinales
        Material pilarMaterial = Material.OBSIDIAN;
        int pilarHeight = 5;
        
        for (int dir = 0; dir < 4; dir++) {
            int offsetX = 0, offsetZ = 0;
            switch (dir) {
                case 0: offsetX = radio; break;     // Este
                case 1: offsetX = -radio; break;    // Oeste
                case 2: offsetZ = radio; break;     // Sur
                case 3: offsetZ = -radio; break;    // Norte
            }
            
            for (int y = 0; y < pilarHeight; y++) {
                Location loc = new Location(world, centerX + offsetX, centerY + 1 + y, centerZ + offsetZ);
                loc.getBlock().setType(pilarMaterial);
            }
            
            // Antorcha soul en la cima
            Location torchLoc = new Location(world, centerX + offsetX, centerY + 1 + pilarHeight, centerZ + offsetZ);
            torchLoc.getBlock().setType(Material.SOUL_TORCH);
        }
        
        // Actualizar centro a nivel del suelo
        arenaCenter.setY(centerY + 1);
    }
    
    private void spawnearOleada(int numeroOleada) {
        if (arenaCenter == null) return;
        
        ConfigurationSection mobsConfig = config.getConfigurationSection("mobs");
        if (mobsConfig == null) return;
        
        // Cantidad de mobs según la oleada
        int cantidadBase = 3 + (numeroOleada * 2); // Oleada 1: 5, Oleada 2: 7, Oleada 3: 9
        
        for (int i = 0; i < cantidadBase; i++) {
            Location spawnLoc = encontrarPosicionSpawn(arenaCenter, 15, 25);
            if (spawnLoc == null) continue;
            
            // Alternancia de tipos de sombra
            String tipoSombra = (i % 2 == 0) ? "sombra_larga" : "sombra_rapida";
            ConfigurationSection mobConfig = mobsConfig.getConfigurationSection(tipoSombra);
            if (mobConfig == null) continue;
            
            Zombie sombra = (Zombie) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
            configurarSombraLarga(sombra, mobConfig);
            entidadesEvento.add(sombra.getUniqueId());
            
            // Partículas de spawn
            spawnLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, spawnLoc, 30, 0.5, 1, 0.5, 0.1);
        }
        
        // Sonido de oleada
        arenaCenter.getWorld().playSound(arenaCenter, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.8f);
    }
    
    private void spawnearGuardian() {
        if (arenaCenter == null) return;
        
        guardianSpawneado = true;
        
        // 🌫️ AMBIENTE: Lluvia sangrienta + grietas del vacío + corrupción extrema
        World bossWorld = arenaCenter.getWorld();
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
        World world = arenaCenter.getWorld();
        long tiempoOriginal = world.getTime();
        world.setTime(18000); // Medianoche
        
        // 🎬 Secuencia de efectos superpuestos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Explosión de partículas masiva
            for (int i = 0; i < 360; i += 10) {
                double radians = Math.toRadians(i);
                for (int r = 1; r <= 15; r++) {
                    Location particleLoc = arenaCenter.clone().add(
                        Math.cos(radians) * r,
                        5,
                        Math.sin(radians) * r
                    );
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 3, 0.2, 0.2, 0.2, 0.05);
                    world.spawnParticle(Particle.SQUID_INK, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                }
            }
            
            // Efecto cinematográfico con título
            efectoCinematico("§5§l⚔ GUARDIÁN DEL UMBRAL ⚔", 10, 80, 20);
            
            // Partículas verticales masivas
            for (int y = 0; y < 50; y++) {
                world.spawnParticle(Particle.REVERSE_PORTAL, arenaCenter.clone().add(0, y, 0), 20, 0.5, 0, 0.5, 0.3);
                world.spawnParticle(Particle.END_ROD, arenaCenter.clone().add(0, y, 0), 10, 0.3, 0, 0.3, 0.1);
            }
            
            world.spawnParticle(Particle.EXPLOSION_EMITTER, arenaCenter.clone().add(0, 5, 0), 15, 3, 3, 3);
            
            // 🎬 Sonidos superpuestos cinematográficos
            world.playSound(arenaCenter, Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.4f);
            world.playSound(arenaCenter, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.5f, 0.2f);
            world.playSound(arenaCenter, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.5f);
            world.playSound(arenaCenter, Sound.AMBIENT_BASALT_DELTAS_MOOD, 2.0f, 0.3f);
            
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
            double playerScaling = 1.0 + (numJugadores - 1) * 0.3; // +30% stats por jugador extra
            
            // Atributos épicos escalados (para Netherite Prot 4)
            double baseHealth = 400.0 * diffMultiplier * playerScaling;  // 400-3600 corazones
            guardian.getAttribute(Attribute.MAX_HEALTH).setBaseValue(baseHealth);
            guardian.setHealth(baseHealth);
            
            double baseDamage = 12.0 * diffMultiplier * playerScaling;  // Escalado por dificultad
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
            
            // Efecto de aura constante con partículas épicas
            BukkitTask auraTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (guardian.isValid()) {
                    Location loc = guardian.getLocation();
                    // Aura de sombras
                    loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 3, 0), 15, 1.5, 3, 1.5, 0.05);
                    loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 2, 0), 10, 1, 2, 1, 0.03);
                    loc.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(0, 1, 0), 8, 1, 1.5, 1, 0.02);
                    
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
                }
            }, 0L, 20L);  // Cada segundo
            
            oleadaTask = auraTask;
            
            // Habilidades especiales del Guardián
            iniciarHabilidadesGuardian(guardian);
            
            // Restaurar tiempo del mundo
            world.setTime(tiempoOriginal);
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
        
        // Momento 1: Formación del símbolo (primeros 10 segundos)
        if (ticksEnActo == 20) {
            generarSimboloFinal();
            
            String simboloMsg = cliffConfig.getString("mensajes.simbolo.texto",
                "§7§oLos fragmentos se reorganizan en el aire...");
            messageBus.broadcast(simboloMsg, "eco_sombras");
        }
        
        // Momento 2: Monólogo del Observador (15-45 segundos)
        if (ticksEnActo == 300) {
            String obs1 = cliffConfig.getString("mensajes.observador_1.texto",
                "§7§o\"Han sellado la grieta... pero no la fuente.\"");
            messageBus.broadcast(obs1, "eco_sombras");
        }
        
        if (ticksEnActo == 500) {
            String obs2 = cliffConfig.getString("mensajes.observador_2.texto",
                "§7§o\"El eco persiste. La sombra recuerda.\"");
            messageBus.broadcast(obs2, "eco_sombras");
        }
        
        if (ticksEnActo == 700) {
            String obs3 = cliffConfig.getString("mensajes.observador_3.texto",
                "§7§o\"Lo que viene... no tiene forma. Aún.\"");
            messageBus.broadcast(obs3, "eco_sombras");
        }
        
        // Momento 3: Aparición de la figura misteriosa (60 segundos)
        if (ticksEnActo == 1200) {
            aparicionFiguraMisteriosa();
        }
        
        // Finalizar evento (90 segundos)
        if (ticksEnActo >= 1800) {
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
                sumY / jugadores.size() + 10,
                sumZ / jugadores.size()
            );
        }
        
        ConfigurationSection simboloConfig = config.getConfigurationSection("estructuras.simbolo_final");
        if (simboloConfig == null) return;
        
        Location center = arenaCenter.clone().add(0, 15, 0); // 15 bloques en el aire
        World world = center.getWorld();
        
        // Símbolo flotante usando bloques de END_ROD y CRYING_OBSIDIAN
        // Patrón en forma de estrella de 5 puntas
        
        // Centro
        center.getBlock().setType(Material.CRYING_OBSIDIAN);
        
        // 5 puntas de la estrella
        for (int i = 0; i < 5; i++) {
            double angulo = (i * 72 - 90) * Math.PI / 180; // -90 para empezar arriba
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
        
        // Partículas continuas alrededor del símbolo
        BukkitTask simboloTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            world.spawnParticle(Particle.END_ROD, center, 20, 5, 0.5, 5, 0.05);
            world.spawnParticle(Particle.PORTAL, center, 10, 3, 0.5, 3, 0.5);
            world.spawnParticle(Particle.SOUL, center, 5, 2, 0.5, 2, 0.02);
        }, 0L, 5L);
        
        // Guardar tarea para limpiar después
        if (spawnTask != null) spawnTask.cancel();
        spawnTask = simboloTask;
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
     * ═══════════════════════════════════════════════════════════════════
     * 🏆 VICTORIA: Cinematografía + Cleanup + Recompensas + Finalización
     * ═══════════════════════════════════════════════════════════════════
     */
    public void onGuardianDerrotado() {
        if (!guardianSpawneado) return;
        
        // Registrar participación de todos los jugadores cercanos
        Location guardianLoc = null;
        if (guardianEntity != null) {
            guardianLoc = guardianEntity.getLocation().clone();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld().equals(guardianLoc.getWorld()) && p.getLocation().distance(guardianLoc) < 100) {
                    participacionGuardian.put(p.getUniqueId(), true);
                }
            }
        }
        
        final Location finalGuardianLoc = guardianLoc;
        
        // ═══════════════════════════════════════════════════════════════════
        // 🎬 CINEMATOGRAFÍA DE VICTORIA
        // ═══════════════════════════════════════════════════════════════════
        
        // Slow-motion dramático
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participacionGuardian.getOrDefault(p.getUniqueId(), false)) {
                // Cinematic zoom + slow motion (0.5 = zoom in)
                cinematicSystem.smoothZoom(p, 0.5f, 80);
                cinematicSystem.slowMotion(p, 80);
                
                // Título de victoria
                p.sendTitle("§5§l⬢ VICTORIA ⬢", "§7El Guardián ha caído", 10, 60, 20);
            }
        }
        
        // 🎵 AUDIO: Stinger de victoria épico
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (participacionGuardian.getOrDefault(p.getUniqueId(), false)) {
                audioSystem.playStinger(p, EventAudioSystem.StingerType.VICTORY);
            }
        }
        
        // 🎨 Efectos de partículas épicos en la ubicación del Guardián
        if (finalGuardianLoc != null) {
            final World world = finalGuardianLoc.getWorld();
            BukkitTask particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (world != null) {
                    world.spawnParticle(Particle.PORTAL, finalGuardianLoc, 50, 2, 2, 2, 0.5);
                    world.spawnParticle(Particle.END_ROD, finalGuardianLoc, 30, 1, 1, 1, 0.2);
                    world.spawnParticle(Particle.TOTEM_OF_UNDYING, finalGuardianLoc, 20, 1.5, 1.5, 1.5, 0.1);
                }
            }, 0L, 5L);
            
            // Cancelar después de 80 ticks
            Bukkit.getScheduler().runTaskLater(plugin, particleTask::cancel, 80L);
        }
        
        // ═══════════════════════════════════════════════════════════════════
        // 🎁 DROPS DEL GUARDIÁN (LEGENDARIOS + AGRADECIMIENTOS)
        // ═══════════════════════════════════════════════════════════════════
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (participacionGuardian.getOrDefault(p.getUniqueId(), false)) {
                    
                    // 1. Drop legendario único del Guardián (one-time)
                    ItemStack legendaryDrop = lootSystem.generateGuardianLegendaryDrop(p);
                    lootSystem.giveRewards(p, Arrays.asList(legendaryDrop));
                    
                    // Notificar drop legendario
                    if (lootSystem.hasReceivedUniqueItem(p, "guardian_legendary_artifact")) {
                        p.sendMessage("§5§l✦ §dHas obtenido: §5§l✦ Estrella del Umbral ✦");
                        p.sendMessage("§7§oUn artefacto legendario del Guardián del Umbral");
                    } else {
                        p.sendMessage("§d§l✦ §7Has obtenido una recompensa épica del Guardián");
                    }
                    
                    // 2. Recompensas de agradecimiento por jugar
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
        }, 60L);
        
        // ═══════════════════════════════════════════════════════════════════
        // 🧹 CLEANUP DE SISTEMAS
        // ═══════════════════════════════════════════════════════════════════
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Mensaje de cierre
            efectoCinematico("§8§l... El silencio regresa ...", 10, 60, 30);
            
            // Limpiar entidades del acto anterior
            limpiarEntidadesActoAnterior();
            
            // Cleanup completo de todos los sistemas
            messageBus.broadcast("§7§oLas sombras se desvanecen...", "eco_sombras");
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Audio cleanup
                audioSystem.cleanupAll();
                
                // Environment cleanup  
                environmentSystem.cleanupAll();
                
                // Cinematic cleanup
                cinematicSystem.cleanupAll();
                
                messageBus.broadcast("§7§oEl mundo vuelve a la normalidad.", "eco_sombras");
                
            }, 40L);
            
        }, 100L);
        
        // ═══════════════════════════════════════════════════════════════════
        // 🏁 FINALIZAR EVENTO
        // ═══════════════════════════════════════════════════════════════════
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Transicionar a CLIFFHANGER (manteniendo consistencia con narrativa)
            transicionarActo(Acto.CLIFFHANGER);
            
            // Finalizar evento después del cliffhanger
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                finalizarEvento();
            }, 100L);
            
        }, 160L);
    }
    
    private void cleanup() {
        // Remover entidades del evento
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entidadesEvento.contains(entity.getUniqueId())) {
                    entity.remove();
                }
            }
        }
        
        // 🎨 LIMPIAR TODOS LOS EFECTOS DE PARTÍCULAS
        particleSystem.cleanupAll();
        
        // Limpiar sistemas de UI
        uiManager.cleanupAll();
        feedbackSystem.cleanupAll();
        
        entidadesEvento.clear();
        manchasLocations.clear();
        anclaLocations.clear();
        anclasSelladas.clear();
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
}
