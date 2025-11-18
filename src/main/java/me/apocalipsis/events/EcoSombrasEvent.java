package me.apocalipsis.events;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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
    
    // ═══════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════
    
    public EcoSombrasEvent(Apocalipsis plugin, MessageBus messageBus, SoundUtil soundUtil) {
        super(plugin, messageBus, soundUtil, "eco_sombras");
        loadConfig();
        
        items = new EcoSombrasItems();
        listener = new EcoSombrasListener(this, items);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
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
        
        // Efecto de oscurecimiento
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
        
        // Spawn del mob
        ConfigurationSection mobConfig = config.getConfigurationSection("actos.acto_2_sombras_largas.spawn_sombras.configuracion_mob");
        Zombie sombra = (Zombie) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
        
        configurarSombraLarga(sombra, mobConfig);
        entidadesEvento.add(sombra.getUniqueId());
        
        // Partículas de spawn
        spawnLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, spawnLoc, 20, 0.5, 1, 0.5, 0.1);
    }
    
    private void configurarSombraLarga(Zombie sombra, ConfigurationSection config) {
        // Nombre
        sombra.setCustomName(config.getString("nombre", "§8Sombra Larga"));
        sombra.setCustomNameVisible(false);
        
        // Atributos
        sombra.getAttribute(Attribute.MAX_HEALTH).setBaseValue(config.getDouble("atributos.vida", 35));
        sombra.setHealth(sombra.getAttribute(Attribute.MAX_HEALTH).getValue());
        sombra.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(config.getDouble("atributos.danio", 7));
        sombra.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(config.getDouble("atributos.velocidad", 0.23));
        
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
        
        // Transición al matar 15 sombras
        if (sombrasLargasMuertas >= 15) {
            if (spawnTask != null) spawnTask.cancel();
            transicionarActo(Acto.NUCLEO);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ACTO 3: NÚCLEO DE SOMBRA LARGA
    // ═══════════════════════════════════════════════════════════════════
    
    private void iniciarActoNucleo() {
        plugin.getLogger().info("[EcoSombras] Iniciando Acto 3: Núcleo");
        
        // Spawn del Núcleo
        List<Player> jugadores = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (jugadores.isEmpty()) return;
        
        Player target = jugadores.get(random.nextInt(jugadores.size()));
        nucleoLocation = encontrarPosicionSpawn(target.getLocation(), 20, 50);
        
        if (nucleoLocation == null) return;
        
        // Elevar 3 bloques
        nucleoLocation.add(0, 3, 0);
        
        // Spawn Shulker como base
        Shulker nucleo = (Shulker) nucleoLocation.getWorld().spawnEntity(nucleoLocation, EntityType.SHULKER);
        configurarNucleo(nucleo);
        
        nucleoEntity = nucleo;
        entidadesEvento.add(nucleo.getUniqueId());
        
        // Mensaje dramático
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§5§lUna raíz de la sombra", "§7ha despertado", 10, 60, 20);
        }
        
        String mensaje = config.getString("actos.acto_3_nucleo.mensajes.aparicion.chat");
        messageBus.broadcast(mensaje, "eco_sombras");
        
        // Sonido
        nucleoLocation.getWorld().playSound(nucleoLocation, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.8f);
        
        // Efectos visuales periódicos
        iniciarEfectosNucleo();
    }
    
    private void configurarNucleo(Shulker nucleo) {
        ConfigurationSection config = this.config.getConfigurationSection("actos.acto_3_nucleo.nucleo");
        
        nucleo.setCustomName(config.getString("nombre", "§5§l§nNúcleo de Sombra Larga"));
        nucleo.setCustomNameVisible(true);
        nucleo.setAI(false);
        nucleo.setGravity(false);
        nucleo.setInvulnerable(false);
        
        double vida = config.getDouble("atributos.vida", 250);
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
        
        // Check teleporte
        double vidaActual = ((LivingEntity) nucleoEntity).getHealth();
        double vidaMax = ((LivingEntity) nucleoEntity).getAttribute(Attribute.MAX_HEALTH).getValue();
        double porcentaje = (vidaActual / vidaMax) * 100;
        
        // Al 40% de vida, spawn anclas
        if (porcentaje <= 40 && anclaLocations.isEmpty()) {
            if (spawnTask != null) spawnTask.cancel();
            transicionarActo(Acto.ANCLAS);
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
        
        // Efectos visuales periódicos
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (anclasSelladas.contains(id) || actoActual != Acto.ANCLAS) {
                return;
            }
            
            // Partículas verticales
            for (int y = 1; y <= 4; y++) {
                world.spawnParticle(Particle.END_ROD, center.clone().add(0, y, 0), 3, 0.2, 0.2, 0.2, 0);
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
        
        anclasSelladas.add(id);
        
        // Incrementar participación
        participacionAnclas.merge(jugador.getUniqueId(), 1, Integer::sum);
        
        // Efectos
        Location anclaLoc = anclaLocations.get(id);
        anclaLoc.getWorld().spawnParticle(Particle.END_ROD, anclaLoc.clone().add(0, 1, 0), 50, 0.5, 20, 0.5, 0.3);
        anclaLoc.getWorld().playSound(anclaLoc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f, 0.5f);
        
        // Mensaje
        String msg = String.format("§5Ancla %d/%d sellada", anclasSelladas.size(), anclaLocations.size());
        messageBus.broadcast(msg, "eco_sombras");
        
        // Sonido global
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.5f);
        }
        
        // Mensaje del Observador (primera vez)
        if (anclasSelladas.size() == 1) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                String obs = config.getString("actos.acto_4_anclas.mensajes.observador.texto",
                    "§7§o\"Sellan la herida, pero no la causa…\"");
                messageBus.broadcast(obs, "eco_sombras");
            }, 40L);
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
        
        switch (nuevoActo) {
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
        
        ConfigurationSection guardianConfig = config.getConfigurationSection("mobs.guardian");
        if (guardianConfig == null) return;
        
        // Spawn 5 bloques sobre el centro
        Location spawnLoc = arenaCenter.clone().add(0, 5, 0);
        
        // Efectos pre-spawn
        arenaCenter.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, spawnLoc, 5);
        arenaCenter.getWorld().playSound(arenaCenter, Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.5f);
        
        // Spawn del Guardian (Wither Skeleton con atributos custom)
        WitherSkeleton guardian = (WitherSkeleton) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.WITHER_SKELETON);
        
        // Configuración
        String nombre = guardianConfig.getString("nombre", "§5§l§nGuardián de las Sombras Largas");
        guardian.setCustomName(nombre);
        guardian.setCustomNameVisible(true);
        guardian.setRemoveWhenFarAway(false);
        
        // Atributos
        double vida = guardianConfig.getDouble("atributos.vida", 500);
        guardian.getAttribute(Attribute.MAX_HEALTH).setBaseValue(vida);
        guardian.setHealth(vida);
        
        double danio = guardianConfig.getDouble("atributos.danio", 15);
        guardian.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(danio);
        
        double velocidad = guardianConfig.getDouble("atributos.velocidad", 0.28);
        guardian.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(velocidad);
        
        // Equipamiento
        EntityEquipment equip = guardian.getEquipment();
        if (equip != null) {
            equip.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
            equip.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            equip.setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
            equip.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
            equip.setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
            
            equip.setHelmetDropChance(0);
            equip.setChestplateDropChance(0);
            equip.setLeggingsDropChance(0);
            equip.setBootsDropChance(0);
            equip.setItemInMainHandDropChance(0);
        }
        
        guardianEntity = guardian;
        entidadesEvento.add(guardian.getUniqueId());
        
        // Mensaje dramático
        String guardianMsg = guardianConfig.getString("spawn_mensaje",
            "§5§l⚠ EL GUARDIÁN HA DESPERTADO ⚠");
        messageBus.broadcast(guardianMsg, "eco_sombras");
        
        // Efecto de aura constante
        BukkitTask auraTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (guardian.isValid()) {
                Location loc = guardian.getLocation();
                loc.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(0, 1, 0), 10, 0.5, 1, 0.5, 0.05);
                loc.getWorld().spawnParticle(Particle.SOUL, loc.clone().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.02);
            } else {
                // Cancelar tarea si el guardián murió
                if (oleadaTask != null) oleadaTask.cancel();
            }
        }, 0L, 10L);
        
        oleadaTask = auraTask;
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
            if (participacionGuardian.getOrDefault(uuid, false)) {
                psTotal += psPorGuardian;
            }
            
            // Bonus grupal si hay 3+ jugadores
            if (participantesOriginales.size() >= 3) {
                psTotal += psBonusGrupal;
            }
            
            // Otorgar PS (integración con sistema de misiones)
            // TODO: Integrar con MissionService o sistema de economía
            
            // Mensaje de recompensas
            p.sendMessage("§5§l━━━━━━━ RECOMPENSAS ━━━━━━━");
            p.sendMessage("§7PS Base: §e+" + psBase);
            if (sombras > 0) p.sendMessage("§7Sombras eliminadas: §e+" + (sombras * psPorSombra) + " §8(" + sombras + " sombras)");
            if (anclas > 0) p.sendMessage("§7Anclas selladas: §e+" + (anclas * psPorAncla) + " §8(" + anclas + " anclas)");
            if (participacionGuardian.getOrDefault(uuid, false)) p.sendMessage("§7Guardián derrotado: §e+" + psPorGuardian);
            if (participantesOriginales.size() >= 3) p.sendMessage("§7Bonus grupal: §e+" + psBonusGrupal);
            p.sendMessage("§5§lTOTAL: §e§l+" + psTotal + " PS");
            p.sendMessage("§5§l━━━━━━━━━━━━━━━━━━━━━━━━━");
            
            // Sonido de recompensa
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }
    
    public void onSombraLargaMuerta(Player killer) {
        sombrasLargasMuertas++;
        if (killer != null) {
            participacionSombras.merge(killer.getUniqueId(), 1, Integer::sum);
        }
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
