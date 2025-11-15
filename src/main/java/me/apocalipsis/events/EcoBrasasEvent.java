package me.apocalipsis.events;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;
import me.apocalipsis.experience.ExperienceService;
import me.apocalipsis.missions.MissionRank;

/**
 * Eco de Brasas - Evento narrativo cooperativo con 3 fases
 * 
 * Contexto narrativo:
 * Desde que el Nether se abrió, el calor del inframundo se filtró a la superficie.
 * El Observador siente grietas que aparecen al azar, como si el mundo exhalara fuego para no colapsar.
 * Tu tarea no es apagarlo, sino aprender a controlarlo.
 * 
 * "El fuego busca forma... no enemigos." — El Observador
 * 
 * Fases del evento:
 * 1. RECOLECCIÓN (25 min): Grietas aparecen lejos de jugadores, deben ir y cerrarlas
 * 2. ESTABILIZACIÓN (45 min): 3 Anclas requieren entregas cooperativas
 * 3. RITUAL FINAL (15 min): Llenar sello de energía, aparece Guardián
 */
public class EcoBrasasEvent extends EventBase {
    
    // ═══════════════════════════════════════════════════════════════════
    // ESTADO DEL EVENTO
    // ═══════════════════════════════════════════════════════════════════
    
    private enum Fase {
        INTRO,          // Cinemática inicial
        RECOLECCION,    // Fase 1
        TRANSICION_2,   // Cinemática transición
        ESTABILIZACION, // Fase 2
        TRANSICION_3,   // Cinemática transición
        RITUAL_FINAL,   // Fase 3
        VICTORIA        // Cinemática final
    }
    
    private Fase faseActual;
    private int ticksEnFase;
    private int ticksTotales;
    
    // Sistema de tracking de participación para recompensas PS
    private Map<UUID, Integer> participacionGrietas = new HashMap<>(); // Grietas cerradas por jugador
    private Map<UUID, Integer> participacionAnclas = new HashMap<>();  // Anclas completadas por jugador
    private Map<UUID, Boolean> participacionGuardian = new HashMap<>(); // Participó en matar guardián
    
    // Configuración cargada de eventos.yml
    private ConfigurationSection config;
    
    // Tareas programadas
    private BukkitTask dialogoTask;
    private boolean eventoActivo = false;
    
    // Datos temporales para cinematics
    private List<String> mensajesPendientes;
    private int mensajeActualIndex;
    
    private final Random random = new Random();
    
    // Listener para interacción con grietas
    private EcoBrasasListener grietaListener;
    
    // ═══════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════
    
    public EcoBrasasEvent(Apocalipsis plugin, MessageBus messageBus, SoundUtil soundUtil) {
        super(plugin, messageBus, soundUtil, "eco_brasas");
        loadConfig();
        
        // Crear y registrar listener
        grietaListener = new EcoBrasasListener(this);
        Bukkit.getPluginManager().registerEvents(grietaListener, plugin);
    }
    
    private void loadConfig() {
        config = plugin.getConfigManager().getEventosConfig()
            .getConfigurationSection("eventos.eco_brasas");
        
        if (config == null) {
            plugin.getLogger().warning("[EcoBrasas] Configuración no encontrada en eventos.yml");
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // MÉTODOS ABSTRACTOS IMPLEMENTADOS
    // ═══════════════════════════════════════════════════════════════════
    
    @Override
    public void onStart() {
        eventoActivo = true;
        faseActual = Fase.INTRO;
        ticksEnFase = 0;
        ticksTotales = 0;
        
        plugin.getLogger().info("[EcoBrasas] Evento iniciado - comenzando cinemática intro");
        
        // Programar cinemática intro
        scheduleIntroCinematic();
        
        // Iniciar diálogos periódicos
        startDialogueSystem();
    }
    
    @Override
    public void onStop() {
        eventoActivo = false;
        plugin.getLogger().info("[EcoBrasas] Evento detenido");
        
        // Cancelar tareas
        if (dialogoTask != null) {
            dialogoTask.cancel();
            dialogoTask = null;
        }
        
        // Limpiar entidades/bloques del evento
        cleanup();
    }
    
    @Override
    public void onTick() {
        ticksEnFase++;
        ticksTotales++;
        
        // Ejecutar lógica según fase actual
        switch (faseActual) {
            case INTRO:
                // La cinemática se ejecuta por tasks programadas
                break;
                
            case RECOLECCION:
                tickFaseRecoleccion();
                break;
                
            case TRANSICION_2:
                // Cinemática programada
                break;
                
            case ESTABILIZACION:
                tickFaseEstabilizacion();
                break;
                
            case TRANSICION_3:
                // Cinemática programada
                break;
                
            case RITUAL_FINAL:
                tickFaseRitual();
                break;
                
            case VICTORIA:
                // Cinemática final
                break;
        }
    }
    
    @Override
    public String getDisplayName() {
        return "§5§lEco de Brasas";
    }
    
    @Override
    public String getDescription() {
        return "Evento cooperativo narrativo del Observador";
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // CINEMÁTICA INTRO
    // ═══════════════════════════════════════════════════════════════════
    
    private void scheduleIntroCinematic() {
        ConfigurationSection intro = config.getConfigurationSection("narrativa.intro");
        if (intro == null) {
            plugin.getLogger().warning("[EcoBrasas] Configuración intro no encontrada");
            transicionarFase(Fase.RECOLECCION);
            return;
        }
        
        int delaySeg = intro.getInt("delay_seg", 5);
        List<String> mensajes = intro.getStringList("mensajes");
        
        // Esperar delay inicial
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Sonido inicial
            playSoundToAll(Sound.ENTITY_WITHER_SPAWN, 0.7f, 0.8f);
            
            // Mostrar mensajes uno por uno
            showMessagesSequentially(mensajes, 40, () -> {
                // Mostrar instrucciones claras
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    messageBus.broadcast("§7§m                                                ", "separator");
                    messageBus.broadcast("§6§l>>> INSTRUCCIONES - FASE 1: RECOLECCIÓN <<<", "instrucciones_titulo");
                    messageBus.broadcast("", "space");
                    messageBus.broadcast("§e1. §fGrietas de fuego aparecerán en el mundo", "instruccion1");
                    messageBus.broadcast("§e2. §fRevisa el §cchat §fpara ver §ccoordenadas§f de cada grieta", "instruccion2");
                    messageBus.broadcast("§e3. §fUsa tu §aActionBar §f(barra superior) para ver:", "instruccion3");
                    messageBus.broadcast("   §7• §fDistancia a la grieta más cercana", "instruccion3a");
                    messageBus.broadcast("   §7• §fDirección cardinal (N/S/E/W)", "instruccion3b");
                    messageBus.broadcast("   §7• §fBarra de proximidad visual", "instruccion3c");
                    messageBus.broadcast("§e4. §fAcércate a las grietas y §eciérralas", "instruccion4");
                    messageBus.broadcast("§e5. §fMeta: Cerrar §e" + grietasMetaTotal + " grietas §fen §e25 minutos", "instruccion5");
                    messageBus.broadcast("", "space");
                    messageBus.broadcast("§7§m                                                ", "separator");
                    
                    playSoundToAll(Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                }, 40L);
                
                // Sonido adicional y título
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    playSoundToAll(Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.5f);
                    
                    showTitleToAll(
                        "§5§lECO DE BRASAS",
                        "§7Fase I: §e§lRECOLECCIÓN §7• §cRevisa el chat",
                        20, 80, 20
                    );
                    
                    // Transicionar a Fase 1
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        transicionarFase(Fase.RECOLECCION);
                        messageBus.broadcast("§a§l¡FASE 1 INICIADA! §7La primera grieta aparecerá pronto...", "fase1_start");
                    }, 100L); // 5 segundos después del título
                }, 80L); // 4 segundos después de las instrucciones
            });
        }, delaySeg * 20L);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // FASE 1: RECOLECCIÓN
    // ═══════════════════════════════════════════════════════════════════
    
    // Datos de grietas (Location -> ArmorStand flotante)
    private Map<Location, org.bukkit.entity.ArmorStand> grietasActivas = new ConcurrentHashMap<>();
    private Map<Location, Integer> grietaHealth = new ConcurrentHashMap<>(); // Vida de cada grieta
    private int nextGrietaSpawnTick = 0;
    private int grietasCerradasCount = 0;
    private int grietasMetaTotal = 10; // Meta de grietas a cerrar
    private static final int GRIETA_MAX_HEALTH = 100; // Golpes necesarios para cerrar
    private static final int GRIETA_TIMEOUT_TICKS = 6000; // 5 min (300 seg)
    
    // Set de ubicaciones de bloques que ya pueden romperse (después de completar fase)
    private java.util.Set<org.bukkit.Location> bloquesRompibles = new java.util.HashSet<>();
    
    private void tickFaseRecoleccion() {
        // Spawn de primera grieta al inicio
        if (ticksEnFase == 1) {
            spawnGrieta();
        }
        
        // Mostrar actionbar con progreso y distancia
        if (ticksEnFase % 20 == 0) { // Cada segundo
            for (Player player : Bukkit.getOnlinePlayers()) {
                showGrietaActionBar(player);
            }
        }
        
        // Efectos visuales en grietas activas cada 10 ticks (0.5 seg)
        if (ticksEnFase % 10 == 0) {
            for (Map.Entry<Location, org.bukkit.entity.ArmorStand> entry : grietasActivas.entrySet()) {
                Location loc = entry.getKey();
                org.bukkit.entity.ArmorStand marker = entry.getValue();
                
                if (marker != null && !marker.isDead()) {
                    // Partículas flotantes
                    loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 2, 0), 20, 1, 1, 1, 0.05);
                    loc.getWorld().spawnParticle(Particle.LAVA, loc.clone().add(0, 1.5, 0), 5, 0.5, 0.5, 0.5, 0);
                    loc.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(0, 2.5, 0), 10, 0.8, 0.8, 0.8, 0.02);
                    
                    // Sonido ambiental cada 2 seg
                    if (ticksEnFase % 40 == 0) {
                        loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 0.8f);
                        loc.getWorld().playSound(loc, Sound.BLOCK_LAVA_POP, 0.3f, 1.2f);
                    }
                }
            }
        }
        
        // Ya NO spawneamos grietas periódicamente, ahora se generan al cerrar cada una
        
        // Verificar solo tiempo límite (la meta se verifica en cerrarGrieta())
        int duracionSeg = config.getConfigurationSection("fase1").getInt("duracion_seg", 1500); // 25 min
        if (ticksEnFase >= duracionSeg * 20) {
            scheduleTransicion2();
        }
    }
    
    private void spawnGrieta() {
        // Buscar ubicación lejos de jugadores (150-300 bloques)
        Location spawnLoc = findRemoteLocationFar();
        if (spawnLoc == null) {
            plugin.getLogger().warning("[EcoBrasas] No se pudo encontrar ubicación para grieta");
            return;
        }
        
        World world = spawnLoc.getWorld();
        
        // [VISUAL MEJORADO] Crear estructura de grieta con bloques
        createGrietaStructure(spawnLoc);
        
        // Crear INTERACTION ENTITY como hitbox para detección de golpes (mejor que Magma Cube)
        org.bukkit.entity.Interaction hitbox = world.spawn(
            spawnLoc.clone().add(0, 1.0, 0), 
            org.bukkit.entity.Interaction.class
        );
        hitbox.setInteractionWidth(2.0f);  // Ancho generoso (2 bloques)
        hitbox.setInteractionHeight(2.0f); // Alto generoso (2 bloques)
        hitbox.setResponsive(true); // Responde a interacciones
        hitbox.addScoreboardTag("eco_grieta_hitbox");
        
        // Crear ArmorStand con item visual (bloque de magma flotante)
        org.bukkit.entity.ArmorStand visual = world.spawn(spawnLoc.clone().add(0, 1.5, 0), org.bukkit.entity.ArmorStand.class);
        visual.setVisible(false);
        visual.setGravity(false);
        visual.setInvulnerable(true);
        visual.setMarker(true);
        visual.getEquipment().setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.MAGMA_BLOCK));
        visual.addScoreboardTag("eco_grieta_visual");
        
        // Crear ArmorStand para nombre visible (más arriba)
        org.bukkit.entity.ArmorStand marker = world.spawn(spawnLoc.clone().add(0, 3.0, 0), org.bukkit.entity.ArmorStand.class);
        marker.setVisible(false);
        marker.setGravity(false);
        marker.setInvulnerable(true);
        marker.setMarker(true);
        marker.customName(net.kyori.adventure.text.Component.text("§c§l⚠ GRIETA DE VAPOR ⚠"));
        marker.setCustomNameVisible(true);
        marker.addScoreboardTag("eco_grieta_label");
        
        // Segundo ArmorStand con instrucción
        org.bukkit.entity.ArmorStand instruccion = world.spawn(spawnLoc.clone().add(0, 2.5, 0), org.bukkit.entity.ArmorStand.class);
        instruccion.setVisible(false);
        instruccion.setGravity(false);
        instruccion.setInvulnerable(true);
        instruccion.setMarker(true);
        instruccion.customName(net.kyori.adventure.text.Component.text("§e§l>>> GOLPEA AQUÍ <<<"));
        instruccion.setCustomNameVisible(true);
        instruccion.addScoreboardTag("eco_grieta_label");
        
        // Registrar grieta
        grietasActivas.put(spawnLoc, marker);
        grietaHealth.put(spawnLoc, GRIETA_MAX_HEALTH);
        
        int x = spawnLoc.getBlockX();
        int z = spawnLoc.getBlockZ();
        plugin.getLogger().info(String.format("[EcoBrasas] Grieta spawneada en X: %d Z: %d (Health: %d)", x, z, GRIETA_MAX_HEALTH));
        
        // NOTIFICAR a todos los jugadores sobre la nueva grieta con coordenadas
        double distanciaMinJugador = Double.MAX_VALUE;
        for (Player p : Bukkit.getOnlinePlayers()) {
            double dist = p.getLocation().distance(spawnLoc);
            if (dist < distanciaMinJugador) {
                distanciaMinJugador = dist;
            }
        }
        
        String coordsShort = String.format("§c[X: %d, Z: %d]", x, z);
        String distMsg = distanciaMinJugador > 200 ? " §7(§c" + (int)distanciaMinJugador + "m§7)" : "";
        messageBus.broadcast("§c§l⚠ GRIETA §7spawneada " + coordsShort + distMsg, "grieta_spawn");
        
        // Sonido de alerta para todos
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 0.7f, 0.8f);
        }
        
        // Efectos visuales iniciales mejorados
        world.spawnParticle(Particle.EXPLOSION, spawnLoc.clone().add(0, 1, 0), 15, 2, 2, 2, 0);
        world.spawnParticle(Particle.LAVA, spawnLoc.clone().add(0, 1, 0), 150, 2, 2, 2, 0.2);
        world.spawnParticle(Particle.FLAME, spawnLoc.clone().add(0, 1, 0), 200, 2, 2, 2, 0.3);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, spawnLoc.clone().add(0, 1, 0), 80, 1.5, 1.5, 1.5, 0.15);
        world.spawnParticle(Particle.DRIPPING_LAVA, spawnLoc.clone().add(0, 2, 0), 50, 1, 0.5, 1, 0);
        
        // Sonidos dramáticos
        world.playSound(spawnLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
        world.playSound(spawnLoc, Sound.BLOCK_PORTAL_AMBIENT, 2.0f, 0.6f);
        world.playSound(spawnLoc, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.8f);
        world.playSound(spawnLoc, Sound.BLOCK_LAVA_POP, 1.0f, 0.7f);
        
        // Mensaje del Observador con coordenadas
        String coords = String.format("X: %d Z: %d", 
            spawnLoc.getBlockX(), spawnLoc.getBlockZ());
        
        messageBus.broadcast("§7§m                                                ", "separator");
        messageBus.broadcast("§6§l🌀 OBSERVADOR:", "observador");
        messageBus.broadcast("§f\"Detecté un pulso inestable cerca de §c" + coords + "§f.\"", "mensaje1");
        messageBus.broadcast("§f\"El calor busca equilibrio.\"", "mensaje2");
        messageBus.broadcast("", "space");
        messageBus.broadcast("§e» Acércate y §c§lGOLPEA LA GRIETA §epara cerrarla", "instruccion");
        messageBus.broadcast("§7  Se necesitan §c" + GRIETA_MAX_HEALTH + " golpes §7para cerrarla", "info");
        messageBus.broadcast("§7  Tiempo límite: §e5 minutos", "timeout");
        messageBus.broadcast("§7§m                                                ", "separator");
        
        playSoundToAll(Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 0.8f);
        playSoundToAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.6f);
        
        // Title de alerta
        showTitleToAll(
            "§c§l⚠ GRIETA ABIERTA ⚠",
            "§7Golpéala para cerrarla §8• §e" + coords,
            10, 60, 20
        );
        
        plugin.getLogger().info(String.format("[EcoBrasas] Grieta spawneada en %s (Health: %d)", 
            coords, GRIETA_MAX_HEALTH));
    }
    
    /**
     * Crea estructura visual de grieta con bloques
     */
    private void createGrietaStructure(Location center) {
        World world = center.getWorld();
        int x = center.getBlockX();
        int y = center.getBlockY();
        int z = center.getBlockZ();
        
        // Crear cruz de netherrack y magma en el suelo (patrón de grieta)
        world.getBlockAt(x, y, z).setType(org.bukkit.Material.MAGMA_BLOCK);
        world.getBlockAt(x+1, y, z).setType(org.bukkit.Material.NETHERRACK);
        world.getBlockAt(x-1, y, z).setType(org.bukkit.Material.NETHERRACK);
        world.getBlockAt(x, y, z+1).setType(org.bukkit.Material.NETHERRACK);
        world.getBlockAt(x, y, z-1).setType(org.bukkit.Material.NETHERRACK);
        world.getBlockAt(x+1, y, z+1).setType(org.bukkit.Material.MAGMA_BLOCK);
        world.getBlockAt(x-1, y, z-1).setType(org.bukkit.Material.MAGMA_BLOCK);
        world.getBlockAt(x+1, y, z-1).setType(org.bukkit.Material.MAGMA_BLOCK);
        world.getBlockAt(x-1, y, z+1).setType(org.bukkit.Material.MAGMA_BLOCK);
        
        // Crear fuegos pequeños alrededor
        spawnFireEffect(new Location(world, x+1, y+1, z));
        spawnFireEffect(new Location(world, x-1, y+1, z));
        spawnFireEffect(new Location(world, x, y+1, z+1));
        spawnFireEffect(new Location(world, x, y+1, z-1));
    }
    
    private void spawnFireEffect(Location loc) {
        if (loc.getBlock().getType() == org.bukkit.Material.AIR) {
            loc.getBlock().setType(org.bukkit.Material.FIRE);
        }
    }
    
    /**
     * Buscar ubicación lejos de jugadores (150-300 bloques)
     * OPTIMIZADO: Solo verifica chunks ya cargados para evitar lag
     */
    private Location findRemoteLocationFar() {
        World world = Bukkit.getWorlds().get(0);
        int rangoMin = 150;
        int rangoMax = 300;
        int maxIntentos = 50;
        
        // Intentar encontrar ubicación en chunks YA CARGADOS
        for (int intento = 0; intento < maxIntentos; intento++) {
            int distancia = random.nextInt(rangoMax - rangoMin + 1) + rangoMin;
            double angulo = random.nextDouble() * 2 * Math.PI;
            int x = (int) (distancia * Math.cos(angulo));
            int z = (int) (distancia * Math.sin(angulo));
            
            // CRÍTICO: Solo verificar si el chunk está CARGADO
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            
            int y = world.getHighestBlockYAt(x, z);
            Location loc = new Location(world, x, y, z);
            
            if (!isValidSurfaceLocation(loc)) {
                continue;
            }
            
            boolean lejos = true;
            for (Player player : Bukkit.getOnlinePlayers()) {
                double dist = player.getLocation().distance(loc);
                if (dist < rangoMin) {
                    lejos = false;
                    break;
                }
            }
            
            if (lejos) {
                plugin.getLogger().info("[EcoBrasas] Grieta encontrada en chunks cargados");
                return loc.add(0, 1, 0);
            }
        }
        
        // Fallback: Buscar cerca de jugadores (chunks cargados)
        if (!Bukkit.getOnlinePlayers().isEmpty()) {
            Player randomPlayer = (Player) Bukkit.getOnlinePlayers().toArray()[random.nextInt(Bukkit.getOnlinePlayers().size())];
            Location playerLoc = randomPlayer.getLocation();
            
            for (int intento = 0; intento < 30; intento++) {
                double angulo = random.nextDouble() * 2 * Math.PI;
                int offsetDist = rangoMin + random.nextInt(rangoMax - rangoMin);
                int x = playerLoc.getBlockX() + (int) (offsetDist * Math.cos(angulo));
                int z = playerLoc.getBlockZ() + (int) (offsetDist * Math.sin(angulo));
                
                if (world.isChunkLoaded(x >> 4, z >> 4)) {
                    int y = world.getHighestBlockYAt(x, z);
                    Location loc = new Location(world, x, y, z);
                    
                    if (isValidSurfaceLocation(loc)) {
                        plugin.getLogger().info("[EcoBrasas] Grieta spawneada cerca de jugador");
                        return loc.add(0, 1, 0);
                    }
                }
            }
        }
        
        // Último recurso: usar ubicación aleatoria relativa al spawn con offset
        plugin.getLogger().warning("[EcoBrasas] Grieta usando ubicación aleatoria cerca del spawn");
        Location spawn = world.getSpawnLocation();
        int offsetX = random.nextInt(200) - 100; // -100 a +100
        int offsetZ = random.nextInt(200) - 100;
        int x = spawn.getBlockX() + offsetX;
        int z = spawn.getBlockZ() + offsetZ;
        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x, y + 1, z);
    }
    
    /**
     * Valida que una ubicación sea superficie sólida válida (no agua, área suficiente)
     */
    private boolean isValidSurfaceLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        
        World world = loc.getWorld();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        
        // 1. El bloque donde se spawneará debe ser AIR (espacio libre)
        org.bukkit.Material blockAt = world.getBlockAt(x, y + 1, z).getType();
        if (blockAt != org.bukkit.Material.AIR && blockAt != org.bukkit.Material.CAVE_AIR) {
            return false; // No hay espacio
        }
        
        // 2. El bloque debajo debe ser SUPERFICIE SÓLIDA (no agua, lava, aire)
        org.bukkit.Material groundBlock = world.getBlockAt(x, y, z).getType();
        if (!groundBlock.isSolid() || 
            groundBlock == org.bukkit.Material.WATER || 
            groundBlock == org.bukkit.Material.LAVA ||
            groundBlock == org.bukkit.Material.ICE ||
            groundBlock == org.bukkit.Material.MAGMA_BLOCK) {
            return false; // No es superficie válida
        }
        
        // 3. Verificar área de 5x5 alrededor (suficiente superficie)
        int solidCount = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                org.bukkit.Material checkGround = world.getBlockAt(x + dx, y, z + dz).getType();
                if (checkGround.isSolid() && 
                    checkGround != org.bukkit.Material.WATER && 
                    checkGround != org.bukkit.Material.LAVA) {
                    solidCount++;
                }
            }
        }
        
        // Al menos 18 de 25 bloques deben ser sólidos (72%)
        return solidCount >= 18;
    }
    
    private void showGrietaActionBar(Player player) {
        if (grietasActivas.isEmpty()) {
            player.sendActionBar("§7[§6Eco de Brasas§7] §aNo hay grietas activas §7• §e" + grietasCerradasCount + "/" + grietasMetaTotal + " cerradas");
            return;
        }
        
        // Encontrar grieta más cercana
        Location playerLoc = player.getLocation();
        Location grietaMasCercana = null;
        double distanciaMin = Double.MAX_VALUE;
        
        for (Location grieta : grietasActivas.keySet()) {
            org.bukkit.entity.ArmorStand marker = grietasActivas.get(grieta);
            if (marker == null || marker.isDead()) continue; // Skip muertas
            
            double dist = playerLoc.distance(grieta);
            if (dist < distanciaMin) {
                distanciaMin = dist;
                grietaMasCercana = grieta;
            }
        }
        
        if (grietaMasCercana == null) {
            player.sendActionBar("§7[§6Eco de Brasas§7] §aNo hay grietas activas §7• §e" + grietasCerradasCount + "/" + grietasMetaTotal + " cerradas");
            return;
        }
        
        int distancia = (int) distanciaMin;
        
        // Obtener vida de la grieta
        int health = grietaHealth.getOrDefault(grietaMasCercana, GRIETA_MAX_HEALTH);
        int healthPercent = (health * 100) / GRIETA_MAX_HEALTH;
        
        // Si está CERCA (< 10 bloques), mostrar barra de vida y daño
        if (distancia < 10) {
            String healthBar = getHealthBar(healthPercent);
            player.sendActionBar(String.format(
                "§c§l⚠ GRIETA §7• %s §c%d%% §7(§c%d§7/§c%d§7) §7• §e%d/%d cerradas",
                healthBar, healthPercent, health, GRIETA_MAX_HEALTH, grietasCerradasCount, grietasMetaTotal
            ));
        } else {
            // Si está LEJOS, mostrar distancia y dirección
            String direccion = getCardinalDirection(playerLoc, grietaMasCercana);
            String barraProximidad = getProximityBar(distancia);
            
            player.sendActionBar(String.format(
                "§7[§6Grieta más cercana§7] §c%dm %s §7• %s §7• §c❤ %d%% §7• §e%d/%d cerradas",
                distancia, direccion, barraProximidad, healthPercent, grietasCerradasCount, grietasMetaTotal
            ));
        }
    }
    
    private String getCardinalDirection(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        angle = (angle + 360 + 90) % 360; // Normalizar
        
        if (angle < 45 || angle >= 315) return "§fN";
        if (angle < 135) return "§fE";
        if (angle < 225) return "§fS";
        return "§fW";
    }
    
    private String getProximityBar(int distancia) {
        if (distancia < 10) return "§c▓▓▓▓▓▓▓▓▓▓ §c§lMUY CERCA";
        if (distancia < 30) return "§6▓▓▓▓▓▓▓▓░░ §6§lCERCA";
        if (distancia < 60) return "§e▓▓▓▓▓▓░░░░ §e§lMEDIA";
        if (distancia < 100) return "§f▓▓▓▓░░░░░░ §f§lLEJOS";
        return "§7▓▓░░░░░░░░ §7§lMUY LEJOS";
    }
    
    /**
     * Buscar ubicación lejos de jugadores (50+ bloques) para anclas
     * OPTIMIZADO: Solo verifica chunks ya cargados para evitar lag
     */
    private Location findRemoteLocation() {
        World world = Bukkit.getWorlds().get(0);
        int rangoMin = 50;
        int rangoMax = 200;
        int maxIntentos = 40;
        
        // Intentar en chunks YA CARGADOS
        for (int intento = 0; intento < maxIntentos; intento++) {
            int distancia = random.nextInt(rangoMax - rangoMin + 1) + rangoMin;
            double angulo = random.nextDouble() * 2 * Math.PI;
            int x = (int) (distancia * Math.cos(angulo));
            int z = (int) (distancia * Math.sin(angulo));
            
            // CRÍTICO: Solo chunks cargados
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            
            int y = world.getHighestBlockYAt(x, z);
            Location loc = new Location(world, x, y, z);
            
            if (!isValidSurfaceLocation(loc)) {
                continue;
            }
            
            boolean lejos = true;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getLocation().distance(loc) < rangoMin) {
                    lejos = false;
                    break;
                }
            }
            
            if (lejos) {
                plugin.getLogger().info("[EcoBrasas] Ancla encontrada en chunks cargados");
                return loc.add(0, 1, 0);
            }
        }
        
        // Fallback: Cerca de jugadores (chunks cargados)
        if (!Bukkit.getOnlinePlayers().isEmpty()) {
            Player randomPlayer = (Player) Bukkit.getOnlinePlayers().toArray()[random.nextInt(Bukkit.getOnlinePlayers().size())];
            Location playerLoc = randomPlayer.getLocation();
            
            for (int intento = 0; intento < 20; intento++) {
                double angulo = random.nextDouble() * 2 * Math.PI;
                int dist = rangoMin + random.nextInt(rangoMax - rangoMin);
                int x = playerLoc.getBlockX() + (int) (dist * Math.cos(angulo));
                int z = playerLoc.getBlockZ() + (int) (dist * Math.sin(angulo));
                
                if (world.isChunkLoaded(x >> 4, z >> 4)) {
                    int y = world.getHighestBlockYAt(x, z);
                    Location loc = new Location(world, x, y, z);
                    
                    if (isValidSurfaceLocation(loc)) {
                        plugin.getLogger().info("[EcoBrasas] Ancla spawneada cerca de jugador");
                        return loc.add(0, 1, 0);
                    }
                }
            }
        }
        
        plugin.getLogger().warning("[EcoBrasas] Ancla usando spawn");
        return world.getSpawnLocation();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // TRANSICIÓN FASE 2
    // ═══════════════════════════════════════════════════════════════════
    
    private void scheduleTransicion2() {
        faseActual = Fase.TRANSICION_2;
        ticksEnFase = 0;
        
        ConfigurationSection trans = config.getConfigurationSection("narrativa.transicion_fase2");
        if (trans == null) {
            transicionarFase(Fase.ESTABILIZACION);
            return;
        }
        
        int delaySeg = trans.getInt("delay_seg", 10);
        List<String> mensajes = trans.getStringList("mensajes");
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            playSoundToAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.6f);
            
            showMessagesSequentially(mensajes, 40, () -> {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    playSoundToAll(Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.0f, 1.0f);
                    
                    showTitleToAll(
                        "§6§lFASE II",
                        "§7Estabilización - §e§lANCLAS DE FUEGO",
                        20, 60, 20
                    );
                    
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        transicionarFase(Fase.ESTABILIZACION);
                    }, 100L);
                }, 60L);
            });
        }, delaySeg * 20L);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // FASE 2: ESTABILIZACIÓN
    // ═══════════════════════════════════════════════════════════════════
    
    private Map<Integer, Location> anclas = new HashMap<>(); // 1, 2, 3
    private Map<Integer, org.bukkit.entity.ArmorStand> anclaMarkers = new HashMap<>(); // Markers visuales
    private Map<Integer, Map<String, Integer>> anclaProgreso = new HashMap<>(); // Progreso por tipo
    
    // Requerimientos por ancla (según diseño: 30 Ceniza, 10 Fulgor, 1 Eco Roto)
    private static final int ANCLA_REQ_CENIZA = 30;
    private static final int ANCLA_REQ_FULGOR = 10;
    private static final int ANCLA_REQ_ECO_ROTO = 1;
    
    private void tickFaseEstabilizacion() {
        // Inicializar anclas si es el primer o segundo tick (seguridad)
        if (ticksEnFase <= 1 && anclas.isEmpty()) {
            inicializarAnclas();
        }
        
        // Efectos visuales en anclas cada 10 ticks
        if (ticksEnFase % 10 == 0) {
            for (Map.Entry<Integer, Location> entry : anclas.entrySet()) {
                Location loc = entry.getValue();
                loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 2, 0), 10, 1, 1, 1, 0.05);
                loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 1.5, 0), 5, 0.5, 0.5, 0.5, 0.02);
            }
        }
        
        // Mostrar actionbar con progreso de anclas
        if (ticksEnFase % 20 == 0) { // Cada segundo
            for (Player player : Bukkit.getOnlinePlayers()) {
                showAnclaActionBar(player);
            }
        }
        
        // Verificar si todas las anclas están completas
        boolean todasCompletas = true;
        for (int i = 1; i <= 3; i++) {
            if (!isAnclaCompleta(i)) {
                todasCompletas = false;
                break;
            }
        }
        
        if (todasCompletas) {
            scheduleTransicion3();
            return;
        }
        
        // Verificar tiempo límite
        int duracionSeg = config.getConfigurationSection("fase2").getInt("duracion_seg", 2700); // 45 min
        if (ticksEnFase >= duracionSeg * 20) {
            scheduleTransicion3();
        }
    }
    
    private boolean isAnclaCompleta(int anclaId) {
        Map<String, Integer> progreso = anclaProgreso.get(anclaId);
        if (progreso == null) return false;
        
        return progreso.getOrDefault("ceniza", 0) >= ANCLA_REQ_CENIZA &&
               progreso.getOrDefault("fulgor", 0) >= ANCLA_REQ_FULGOR &&
               progreso.getOrDefault("eco_roto", 0) >= ANCLA_REQ_ECO_ROTO;
    }
    
    private void inicializarAnclas() {
        World world = Bukkit.getWorlds().get(0);
        
        // Primero mostrar diálogos e instrucciones
        messageBus.broadcast("§7§m                                                ", "separator");
        messageBus.broadcast("§6§l🌀 OBSERVADOR:", "observador");
        messageBus.broadcast("§f\"Cerraron las grietas... pero el calor no desapareció.\"", "mensaje1");
        messageBus.broadcast("§f\"Tres §danclas§f sostienen el equilibrio.\"", "mensaje2");
        messageBus.broadcast("§f\"Si las §destabilizan§f, el mundo respira mejor.\"", "mensaje3");
        messageBus.broadcast("", "space");
        messageBus.broadcast("§7=== §d§l✦ FASE 2: ESTABILIZACIÓN ✦ §7===", "fase2_start");
        messageBus.broadcast("§e» Objetivo: §fEstabilizar las §d3 Anclas", "fase2_objetivo");
        messageBus.broadcast("§e» Cómo: §fLleva fragmentos y haz §cclic derecho §fen cada ancla", "fase2_como");
        messageBus.broadcast("", "space2");
        messageBus.broadcast("§7Cada ancla requiere:", "req_titulo");
        messageBus.broadcast("§7  • §e30x §7Ceniza §8(60% drop de grietas)", "req1");
        messageBus.broadcast("§7  • §e10x §6Fulgor §8(25% drop de grietas)", "req2");
        messageBus.broadcast("§7  • §e1x §5Eco Roto §8(6% drop de grietas, RARO)", "req3");
        messageBus.broadcast("", "space3");
        messageBus.broadcast("§a⚠ §7Los jugadores §apueden cooperar §7para completar las anclas", "cooperativo");
        messageBus.broadcast("", "space4");
        messageBus.broadcast("§d§l⚡ UBICACIONES DE ANCLAS:", "ubicaciones_titulo");
        
        // Spawn 3 anclas en ubicaciones aleatorias
        for (int i = 1; i <= 3; i++) {
            Location loc = findRemoteLocation();
            if (loc != null) {
                anclas.put(i, loc);
                
                // Inicializar progreso vacío
                Map<String, Integer> progreso = new HashMap<>();
                progreso.put("ceniza", 0);
                progreso.put("fulgor", 0);
                progreso.put("eco_roto", 0);
                anclaProgreso.put(i, progreso);
                
                // [VISUAL MEJORADO] Crear estructura de ancla con bloques
                createAnclaStructure(loc, i);
                
                // Crear INTERACTION ENTITY como hitbox para detección de clics (mejor que Shulker)
                org.bukkit.entity.Interaction hitbox = world.spawn(
                    loc.clone().add(0, 1.5, 0), 
                    org.bukkit.entity.Interaction.class
                );
                hitbox.setInteractionWidth(1.5f);  // Ancho de la hitbox (1.5 bloques)
                hitbox.setInteractionHeight(1.5f); // Alto de la hitbox (1.5 bloques)
                hitbox.setResponsive(true); // Responde a interacciones
                hitbox.addScoreboardTag("eco_ancla_hitbox");
                hitbox.addScoreboardTag("eco_ancla_" + i);
                
                // Crear ArmorStand con item visual (respawn anchor)
                org.bukkit.entity.ArmorStand visual = world.spawn(loc.clone().add(0, 1.5, 0), org.bukkit.entity.ArmorStand.class);
                visual.setVisible(false);
                visual.setGravity(false);
                visual.setInvulnerable(true);
                visual.setMarker(true);
                visual.getEquipment().setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.RESPAWN_ANCHOR));
                visual.addScoreboardTag("eco_ancla_visual");
                visual.addScoreboardTag("eco_ancla_" + i);
                
                // Crear ArmorStand para nombre (flotante encima)
                org.bukkit.entity.ArmorStand marker = world.spawn(loc.clone().add(0, 3.0, 0), org.bukkit.entity.ArmorStand.class);
                marker.setVisible(false);
                marker.setGravity(false);
                marker.setInvulnerable(true);
                marker.setMarker(true);
                marker.customName(net.kyori.adventure.text.Component.text(String.format("§d§l⚡ ANCLA %d ⚡", i)));
                marker.setCustomNameVisible(true);
                marker.addScoreboardTag("eco_ancla_label");
                marker.addScoreboardTag("eco_ancla_" + i);
                anclaMarkers.put(i, marker);
                
                // ArmorStand con instrucción
                org.bukkit.entity.ArmorStand instruccion = world.spawn(loc.clone().add(0, 2.5, 0), org.bukkit.entity.ArmorStand.class);
                instruccion.setVisible(false);
                instruccion.setGravity(false);
                instruccion.setInvulnerable(true);
                instruccion.setMarker(true);
                instruccion.customName(net.kyori.adventure.text.Component.text("§e§l>>> CLIC DERECHO CON FRAGMENTOS <<<"));
                instruccion.setCustomNameVisible(true);
                instruccion.addScoreboardTag("eco_ancla_label");
                instruccion.addScoreboardTag("eco_ancla_" + i);
                
                // Efectos visuales mejorados
                loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 1, 0), 100, 2, 2, 2, 0.1);
                loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 1, 0), 80, 1.5, 1.5, 1.5, 0.08);
                loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 2, 0), 50, 1, 2, 1, 0.1);
                loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 1.5, 0), 30, 1, 1, 1, 0.05);
                loc.getWorld().playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.5f, 1.0f);
                loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
                
                // NOTIFICAR coordenadas del ancla con distancia
                int x = loc.getBlockX();
                int z = loc.getBlockZ();
                double distanciaMinJugador = Double.MAX_VALUE;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    double dist = p.getLocation().distance(loc);
                    if (dist < distanciaMinJugador) {
                        distanciaMinJugador = dist;
                    }
                }
                String coords = String.format("X: %d, Z: %d", x, z);
                String distMsg = distanciaMinJugador > 150 ? " (§e" + (int)distanciaMinJugador + "m§7)" : "";
                
                messageBus.broadcast(String.format("§7  %d. §d[%s]%s", i, coords, distMsg), "ancla_spawn_" + i);
            }
        }
        
        messageBus.broadcast("§7§m                                                ", "separator");
        
        showTitleToAll(
            "§d§l⚡ FASE 2: ESTABILIZACIÓN ⚡",
            "§7Lleva fragmentos a las §d3 Anclas §7(clic derecho)",
            10, 100, 20
        );
    }
    
    /**
     * Crea estructura visual de ancla con bloques
     */
    private void createAnclaStructure(Location center, int anclaNum) {
        World world = center.getWorld();
        int x = center.getBlockX();
        int y = center.getBlockY();
        int z = center.getBlockZ();
        
        // Crear base de piedra del End (3x3)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(x+dx, y, z+dz).setType(org.bukkit.Material.END_STONE);
            }
        }
        
        // Respawn Anchor en el centro (nivel 1)
        world.getBlockAt(x, y+1, z).setType(org.bukkit.Material.RESPAWN_ANCHOR);
        
        // End Rods decorativos en cruz (esquinas, nivel 1)
        world.getBlockAt(x+1, y+1, z).setType(org.bukkit.Material.END_ROD);
        world.getBlockAt(x-1, y+1, z).setType(org.bukkit.Material.END_ROD);
        world.getBlockAt(x, y+1, z+1).setType(org.bukkit.Material.END_ROD);
        world.getBlockAt(x, y+1, z-1).setType(org.bukkit.Material.END_ROD);
        
        // Glowstone en esquinas diagonales para iluminación
        world.getBlockAt(x+1, y+1, z+1).setType(org.bukkit.Material.GLOWSTONE);
        world.getBlockAt(x-1, y+1, z-1).setType(org.bukkit.Material.GLOWSTONE);
        world.getBlockAt(x+1, y+1, z-1).setType(org.bukkit.Material.GLOWSTONE);
        world.getBlockAt(x-1, y+1, z+1).setType(org.bukkit.Material.GLOWSTONE);
    }
    
    private void showAnclaActionBar(Player player) {
        if (anclas.isEmpty()) {
            player.sendActionBar("§7[§dEstabilización§7] §7Esperando anclas...");
            return;
        }
        
        // Encontrar ancla más cercana
        Location playerLoc = player.getLocation();
        int anclaMasCercana = -1;
        double distanciaMin = Double.MAX_VALUE;
        
        for (Map.Entry<Integer, Location> entry : anclas.entrySet()) {
            double dist = playerLoc.distance(entry.getValue());
            if (dist < distanciaMin) {
                distanciaMin = dist;
                anclaMasCercana = entry.getKey();
            }
        }
        
        if (anclaMasCercana == -1) return;
        
        // Progreso de la ancla más cercana (TODO: implementar correctamente)
        Map<String, Integer> progreso = anclaProgreso.get(anclaMasCercana);
        int ceniza = progreso != null ? progreso.getOrDefault("ceniza", 0) : 0;
        int totalReq = ANCLA_REQ_CENIZA + ANCLA_REQ_FULGOR + ANCLA_REQ_ECO_ROTO; // 41 total
        String barraProgreso = getProgressBar(ceniza, ANCLA_REQ_CENIZA);
        
        // Dirección
        String direccion = getCardinalDirection(playerLoc, anclas.get(anclaMasCercana));
        int distancia = (int) distanciaMin;
        
        // Progreso total (simplificado)
        int totalEntregado = ceniza;
        
        player.sendActionBar(String.format(
            "§7[§dAncla %d§7] %s §c%dm %s §7• §aCeniza: §e%d/%d",
            anclaMasCercana, barraProgreso, distancia, direccion, ceniza, ANCLA_REQ_CENIZA
        ));
    }
    
    private String getProgressBar(int progreso, int maximo) {
        int porcentaje = (int) ((double) progreso / maximo * 10);
        StringBuilder bar = new StringBuilder("§a");
        
        for (int i = 0; i < 10; i++) {
            if (i < porcentaje) {
                bar.append("▓");
            } else {
                bar.append("§7░");
            }
        }
        
        return bar.toString() + " §e" + progreso + "/" + maximo;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // TRANSICIÓN FASE 3
    // ═══════════════════════════════════════════════════════════════════
    
    private void scheduleTransicion3() {
        faseActual = Fase.TRANSICION_3;
        ticksEnFase = 0;
        
        ConfigurationSection trans = config.getConfigurationSection("narrativa.transicion_fase3");
        if (trans == null) {
            transicionarFase(Fase.RITUAL_FINAL);
            return;
        }
        
        int delaySeg = trans.getInt("delay_seg", 15);
        List<String> mensajes = trans.getStringList("mensajes");
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            playSoundToAll(Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.2f);
            
            showMessagesSequentially(mensajes, 60, () -> {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    playSoundToAll(Sound.BLOCK_END_PORTAL_SPAWN, 0.8f, 0.7f);
                    
                    showTitleToAll(
                        "§c§lFASE III",
                        "§7Ritual Final - §d§lEL SELLO",
                        20, 80, 20
                    );
                    
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        transicionarFase(Fase.RITUAL_FINAL);
                    }, 120L);
                }, 80L);
            });
        }, delaySeg * 20L);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // FASE 3: RITUAL FINAL
    // ═══════════════════════════════════════════════════════════════════
    
    // Estados de las oleadas
    private enum OleadaState {
        ESPERANDO,      // No hay oleada activa
        ACTIVA,         // Oleada en progreso, bloquea pulsos
        COMPLETADA      // Oleada eliminada, permite continuar
    }
    
    private Location altarLocation;
    private int pulsoActual = 0;
    private int pulsoMaximo = 8; // Se carga desde config en inicializarAltar()
    private int pulsoSpawnGuardian = 6; // 75% de 8 = 6
    private boolean guardianSpawned = false;
    
    // Sistema de oleadas dinámicas
    private OleadaState oleadaState = OleadaState.ESPERANDO;
    private int oleadaActual = 0;
    private List<Entity> enemigosOleada = new ArrayList<>();
    private int intensidadRitual = 0; // 0-100, aumenta con cada pulso
    private int ultimaOleadaPulso = -1; // Evita spawns múltiples
    private int intervaloOleadas = 4; // Se calcula en inicializarAltar() basado en pulsoMaximo
    
    private void tickFaseRitual() {
        // Inicializar altar en el primer o segundo tick (seguridad)
        if (ticksEnFase <= 1 && altarLocation == null) {
            inicializarAltar();
        }
        
        // Mostrar actionbar con progreso del ritual
        if (ticksEnFase % 20 == 0) { // Cada segundo
            for (Player player : Bukkit.getOnlinePlayers()) {
                showRitualActionBar(player);
            }
        }
        
        // Efectos visuales progresivos basados en intensidad
        tickEfectosRitual();
        
        // Sistema de oleadas dinámicas
        tickOleadas();
        
        // Spawn guardián al alcanzar pulso específico (6/8)
        if (!guardianSpawned && pulsoActual >= pulsoSpawnGuardian) {
            spawnGuardian();
            guardianSpawned = true;
        }
        
        // Victoria si se completa el ritual
        if (pulsoActual >= pulsoMaximo) {
            scheduleVictoria();
            return;
        }
        
        // Verificar tiempo límite
        int duracionSeg = config.getConfigurationSection("fase3").getInt("duracion_seg", 900);
        if (ticksEnFase >= duracionSeg * 20) {
            messageBus.broadcast("§c¡Tiempo agotado! El ritual no pudo completarse.", "ritual_fail");
            scheduleVictoria(); // Forzar fin aunque no se complete
        }
    }
    
    private void inicializarAltar() {
        // RESET: Resetear pulsos al inicializar altar
        pulsoActual = 0;
        guardianSpawned = false;
        oleadaActual = 0;
        oleadaState = OleadaState.ESPERANDO;
        enemigosOleada.clear();
        ultimaOleadaPulso = -1;
        
        // Cargar configuración de pulsos
        pulsoMaximo = config.getConfigurationSection("fase3.altar").getInt("pulsos_requeridos", 8);
        pulsoSpawnGuardian = config.getConfigurationSection("fase3.guardian").getInt("spawn_en_pulso", (int)(pulsoMaximo * 0.75));
        
        // Calcular intervalo de oleadas dinámicamente (cada 20% del total, mínimo 2)
        intervaloOleadas = Math.max(2, pulsoMaximo / 5);
        
        plugin.getLogger().info(String.format("[EcoBrasas] Altar inicializado - Pulsos: 0/%d, Guardián: %d, Oleadas cada: %d pulsos",
            pulsoMaximo, pulsoSpawnGuardian, intervaloOleadas));
        
        // Buscar ubicación central
        altarLocation = findCentralLocation();
        
        if (altarLocation != null) {
            org.bukkit.World world = altarLocation.getWorld();
            
            // [VISUAL MEJORADO] Crear estructura de altar con bloques
            createAltarStructure(altarLocation);
            
            // Crear INTERACTION ENTITY como hitbox para detección de clics (mejor que Shulker)
            org.bukkit.entity.Interaction hitbox = world.spawn(
                altarLocation.clone().add(0, 2.0, 0), 
                org.bukkit.entity.Interaction.class
            );
            hitbox.setInteractionWidth(2.0f);  // Ancho de la hitbox (2 bloques)
            hitbox.setInteractionHeight(2.0f); // Alto de la hitbox (2 bloques)
            hitbox.setResponsive(true); // Responde a interacciones
            hitbox.addScoreboardTag("eco_altar_hitbox");
            hitbox.addScoreboardTag("eco_altar");
            
            // Crear ArmorStand con item visual (beacon)
            org.bukkit.entity.ArmorStand visual = world.spawn(
                altarLocation.clone().add(0, 1.5, 0), 
                org.bukkit.entity.ArmorStand.class
            );
            visual.setVisible(false);
            visual.setGravity(false);
            visual.setInvulnerable(true);
            visual.setMarker(true);
            visual.getEquipment().setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BEACON));
            visual.addScoreboardTag("eco_altar_visual");
            visual.addScoreboardTag("eco_altar");
            
            // Crear ArmorStand para el nombre flotante
            org.bukkit.entity.ArmorStand altarMarker = world.spawn(
                altarLocation.clone().add(0, 3.5, 0), 
                org.bukkit.entity.ArmorStand.class
            );
            altarMarker.setVisible(false);
            altarMarker.setGravity(false);
            altarMarker.setInvulnerable(true);
            altarMarker.setMarker(true);
            altarMarker.customName(net.kyori.adventure.text.Component.text("§c§l⚡ ALTAR DEL ECO ⚡"));
            altarMarker.setCustomNameVisible(true);
            altarMarker.addScoreboardTag("eco_altar_label");
            altarMarker.addScoreboardTag("eco_altar");
            
            // ArmorStand con instrucción
            org.bukkit.entity.ArmorStand instruccion = world.spawn(
                altarLocation.clone().add(0, 3.0, 0), 
                org.bukkit.entity.ArmorStand.class
            );
            instruccion.setVisible(false);
            instruccion.setGravity(false);
            instruccion.setInvulnerable(true);
            instruccion.setMarker(true);
            instruccion.customName(net.kyori.adventure.text.Component.text("§e§l>>> CLIC DERECHO PARA PULSO <<<"));
            instruccion.setCustomNameVisible(true);
            instruccion.addScoreboardTag("eco_altar_label");
            instruccion.addScoreboardTag("eco_altar");
            
            // Efectos visuales masivos mejorados
            altarLocation.getWorld().spawnParticle(Particle.END_ROD, altarLocation.clone().add(0, 1, 0), 200, 3, 3, 3, 0.2);
            altarLocation.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, altarLocation.clone().add(0, 1, 0), 120, 2.5, 2.5, 2.5, 0.12);
            altarLocation.getWorld().spawnParticle(Particle.FLAME, altarLocation.clone().add(0, 1.5, 0), 180, 2, 2, 2, 0.18);
            altarLocation.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, altarLocation.clone().add(0, 2, 0), 50, 1.5, 1.5, 1.5, 0.1);
            altarLocation.getWorld().spawnParticle(Particle.ENCHANT, altarLocation.clone().add(0, 0, 0), 100, 3, 0.5, 3, 1);
            altarLocation.getWorld().playSound(altarLocation, Sound.BLOCK_END_PORTAL_SPAWN, 2.0f, 0.7f);
            altarLocation.getWorld().playSound(altarLocation, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.0f);
            altarLocation.getWorld().playSound(altarLocation, Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.5f);
            
            String coords = String.format("§d[X: %d, Y: %d, Z: %d]", 
                altarLocation.getBlockX(), altarLocation.getBlockY(), altarLocation.getBlockZ());
            
            messageBus.broadcast("§7§m                                                ", "separator");
            messageBus.broadcast("§6§l🌀 OBSERVADOR:", "observador");
            messageBus.broadcast("§f\"Las anclas están completas. El calor se §cconcentra§f.\"", "mensaje1");
            messageBus.broadcast("§f\"Un §dAltar§f nació del equilibrio.\"", "mensaje2");
            messageBus.broadcast("§f\"Tócalo. Siente su pulso. §c§lLibéralo.§f\"", "mensaje3");
            messageBus.broadcast("", "space");
            messageBus.broadcast("§7=== §c§l⚡ FASE 3: RITUAL FINAL ⚡ §7===", "fase3_start");
            messageBus.broadcast(String.format("§e» Objetivo: §fCompletar §c%d pulsos §fen el altar", pulsoMaximo), "fase3_objetivo");
            messageBus.broadcast("§e» Cómo: §fHaz §cclic derecho §fen el altar para cargar energía", "fase3_como");
            messageBus.broadcast("", "space2");
            messageBus.broadcast("§d§l⚡ ALTAR §fspawneado en " + coords, "altar_spawn");
            messageBus.broadcast("", "space3");
            messageBus.broadcast(String.format("§c⚠ §7Al §c75%% §7(pulso %d/%d) aparecerá un §4§lGUARDIÁN", pulsoSpawnGuardian, pulsoMaximo), "guardian_warning");
            messageBus.broadcast("§a⚠ §7Jugadores §acerca del altar §7cargan más rápido", "cooperativo");
            messageBus.broadcast("§7§m                                                ", "separator");
            
            showTitleToAll(
                "§c§l⚡ FASE 3: RITUAL FINAL ⚡",
                String.format("§7Haz clic en el §dAltar §7para cargar §c%d pulsos", pulsoMaximo),
                10, 100, 20
            );
        }
    }
    
    /**
     * Crea estructura visual de altar con bloques (plataforma ritual con beacon)
     */
    private void createAltarStructure(Location center) {
        World world = center.getWorld();
        int x = center.getBlockX();
        int y = center.getBlockY();
        int z = center.getBlockZ();
        
        // Base de obsidiana (5x5)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.getBlockAt(x+dx, y, z+dz).setType(org.bukkit.Material.OBSIDIAN);
            }
        }
        
        // Piedra del End en capa 1 (3x3 interior)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(x+dx, y+1, z+dz).setType(org.bukkit.Material.END_STONE);
            }
        }
        
        // Beacon en el centro (nivel 2)
        world.getBlockAt(x, y+2, z).setType(org.bukkit.Material.BEACON);
        
        // End Rods en cruz alrededor del beacon (nivel 2)
        world.getBlockAt(x+1, y+2, z).setType(org.bukkit.Material.END_ROD);
        world.getBlockAt(x-1, y+2, z).setType(org.bukkit.Material.END_ROD);
        world.getBlockAt(x, y+2, z+1).setType(org.bukkit.Material.END_ROD);
        world.getBlockAt(x, y+2, z-1).setType(org.bukkit.Material.END_ROD);
        
        // Skulls decorativos en esquinas (nivel 2)
        placeSkull(world, x+1, y+2, z+1, org.bukkit.Material.WITHER_SKELETON_SKULL);
        placeSkull(world, x-1, y+2, z-1, org.bukkit.Material.WITHER_SKELETON_SKULL);
        placeSkull(world, x+1, y+2, z-1, org.bukkit.Material.WITHER_SKELETON_SKULL);
        placeSkull(world, x-1, y+2, z+1, org.bukkit.Material.WITHER_SKELETON_SKULL);
        
        // Velas púrpura en bordes exteriores (nivel 1)
        world.getBlockAt(x+2, y+1, z).setType(org.bukkit.Material.PURPLE_CANDLE);
        world.getBlockAt(x-2, y+1, z).setType(org.bukkit.Material.PURPLE_CANDLE);
        world.getBlockAt(x, y+1, z+2).setType(org.bukkit.Material.PURPLE_CANDLE);
        world.getBlockAt(x, y+1, z-2).setType(org.bukkit.Material.PURPLE_CANDLE);
        
        // Linternas de alma en esquinas exteriores (nivel 1)
        world.getBlockAt(x+2, y+1, z+2).setType(org.bukkit.Material.SOUL_LANTERN);
        world.getBlockAt(x-2, y+1, z-2).setType(org.bukkit.Material.SOUL_LANTERN);
        world.getBlockAt(x+2, y+1, z-2).setType(org.bukkit.Material.SOUL_LANTERN);
        world.getBlockAt(x-2, y+1, z+2).setType(org.bukkit.Material.SOUL_LANTERN);
    }
    
    private void placeSkull(World world, int x, int y, int z, org.bukkit.Material skullType) {
        if (world.getBlockAt(x, y, z).getType() == org.bukkit.Material.AIR) {
            world.getBlockAt(x, y, z).setType(skullType);
        }
    }
    
    private void showRitualActionBar(Player player) {
        if (altarLocation == null) {
            player.sendActionBar("§7[§cRitual Final§7] §7Preparando altar...");
            return;
        }
        
        int distancia = (int) player.getLocation().distance(altarLocation);
        String direccion = getCardinalDirection(player.getLocation(), altarLocation);
        String barraProgreso = getProgressBar(pulsoActual, pulsoMaximo);
        
        // Estado del guardián
        String estadoGuardian = guardianSpawned ? "§c§l¡GUARDIÁN ACTIVO!" : (pulsoActual >= (pulsoMaximo * 3 / 4) ? "§e§lCerca del 75%..." : "");
        
        player.sendActionBar(String.format(
            "§7[§dRitual§7] %s §c%dm %s %s",
            barraProgreso, distancia, direccion, estadoGuardian
        ));
    }
    
    /**
     * Buscar ubicación central entre jugadores para el Altar
     * OPTIMIZADO: Solo verifica chunks ya cargados para evitar lag
     */
    private Location findCentralLocation() {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return Bukkit.getWorlds().get(0).getSpawnLocation();
        }
        
        double sumX = 0, sumY = 0, sumZ = 0;
        int count = 0;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location loc = player.getLocation();
            sumX += loc.getX();
            sumY += loc.getY();
            sumZ += loc.getZ();
            count++;
        }
        
        World world = Bukkit.getOnlinePlayers().iterator().next().getWorld();
        int centerX = (int) (sumX / count);
        int centerZ = (int) (sumZ / count);
        
        // CRÍTICO: Verificar si el chunk central está cargado
        if (world.isChunkLoaded(centerX >> 4, centerZ >> 4)) {
            int centerY = world.getHighestBlockYAt(centerX, centerZ);
            Location centerLoc = new Location(world, centerX, centerY, centerZ);
            
            if (isValidSurfaceLocation(centerLoc)) {
                plugin.getLogger().info("[EcoBrasas] Altar en centro exacto");
                return centerLoc.add(0, 1, 0);
            }
        }
        
        // Buscar en círculos, SOLO chunks cargados
        for (int radio = 10; radio <= 100; radio += 10) {
            for (int intento = 0; intento < 12; intento++) {
                double angulo = (Math.PI * 2 * intento) / 12;
                int testX = centerX + (int) (Math.cos(angulo) * radio);
                int testZ = centerZ + (int) (Math.sin(angulo) * radio);
                
                if (!world.isChunkLoaded(testX >> 4, testZ >> 4)) {
                    continue;
                }
                
                int testY = world.getHighestBlockYAt(testX, testZ);
                Location testLoc = new Location(world, testX, testY, testZ);
                
                if (isValidSurfaceLocation(testLoc)) {
                    plugin.getLogger().info("[EcoBrasas] Altar encontrado a " + radio + " bloques");
                    return testLoc.add(0, 1, 0);
                }
            }
        }
        
        // Fallback: Cerca del jugador más cercano al centro
        Player nearestPlayer = null;
        double minDist = Double.MAX_VALUE;
        Location centerPoint = new Location(world, centerX, 64, centerZ);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            double dist = player.getLocation().distance(centerPoint);
            if (dist < minDist) {
                minDist = dist;
                nearestPlayer = player;
            }
        }
        
        if (nearestPlayer != null) {
            Location playerLoc = nearestPlayer.getLocation();
            for (int radio = 20; radio <= 80; radio += 20) {
                for (int i = 0; i < 8; i++) {
                    double angulo = (Math.PI * 2 * i) / 8;
                    int x = playerLoc.getBlockX() + (int) (Math.cos(angulo) * radio);
                    int z = playerLoc.getBlockZ() + (int) (Math.sin(angulo) * radio);
                    
                    if (world.isChunkLoaded(x >> 4, z >> 4)) {
                        int y = world.getHighestBlockYAt(x, z);
                        Location loc = new Location(world, x, y, z);
                        
                        if (isValidSurfaceLocation(loc)) {
                            plugin.getLogger().info("[EcoBrasas] Altar cerca de jugador");
                            return loc.add(0, 1, 0);
                        }
                    }
                }
            }
        }
        
        plugin.getLogger().warning("[EcoBrasas] Altar usando spawn");
        return world.getSpawnLocation();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // CINEMÁTICA VICTORIA
    // ═══════════════════════════════════════════════════════════════════
    
    private void scheduleVictoria() {
        faseActual = Fase.VICTORIA;
        ticksEnFase = 0;
        
        // LIMPIEZA: Eliminar todas las entidades visuales del altar y enemigos
        limpiarEntidadesAltar();
        enemigosOleada.clear();
        
        ConfigurationSection vic = config.getConfigurationSection("narrativa.victoria");
        if (vic == null) {
            plugin.getLogger().info("[EcoBrasas] Victoria - config no encontrada");
            onStop();
            return;
        }
        
        List<String> mensajes = vic.getStringList("mensajes");
        
        // Sonido victoria
        playSoundToAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        
        showMessagesSequentially(mensajes, 40, () -> {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                playSoundToAll(Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                
                showTitleToAll(
                    "§a§l✓ EVENTO COMPLETADO",
                    "§7El Eco de Brasas §e§ose ha calmado",
                    20, 100, 30
                );
                
                // Fuegos artificiales
                spawnFireworks(5);
                
                // Detener evento
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    onStop();
                }, 100L);
            }, 40L);
        });
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE DIÁLOGOS PERIÓDICOS
    // ═══════════════════════════════════════════════════════════════════
    
    private void startDialogueSystem() {
        int intervaloSeg = config.getConfigurationSection("dialogos_observador")
            .getInt("intervalo_seg", 180);
        
        dialogoTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isActive()) {
                return;
            }
            
            String dialogo = getRandomDialogue();
            if (dialogo != null) {
                broadcastNarrative(dialogo);
            }
        }, intervaloSeg * 20L, intervaloSeg * 20L);
    }
    
    private String getRandomDialogue() {
        ConfigurationSection dialogos = config.getConfigurationSection("dialogos_observador");
        if (dialogos == null) {
            return null;
        }
        
        List<String> lista = null;
        switch (faseActual) {
            case RECOLECCION:
                lista = dialogos.getStringList("fase1");
                break;
            case ESTABILIZACION:
                lista = dialogos.getStringList("fase2");
                break;
            case RITUAL_FINAL:
                lista = dialogos.getStringList("fase3");
                break;
            default:
                return null;
        }
        
        if (lista == null || lista.isEmpty()) {
            return null;
        }
        
        return lista.get(random.nextInt(lista.size()));
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // UTILIDADES DE CINEMÁTICAS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Muestra mensajes secuencialmente con delays
     */
    private void showMessagesSequentially(List<String> messages, int delayTicks, Runnable onComplete) {
        if (messages == null || messages.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        
        showMessageRecursive(messages, 0, delayTicks, onComplete);
    }
    
    private void showMessageRecursive(List<String> messages, int index, int delayTicks, Runnable onComplete) {
        if (index >= messages.size()) {
            if (onComplete != null) {
                Bukkit.getScheduler().runTaskLater(plugin, onComplete, delayTicks);
            }
            return;
        }
        
        broadcastNarrative(messages.get(index));
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            showMessageRecursive(messages, index + 1, delayTicks, onComplete);
        }, delayTicks);
    }
    
    /**
     * Muestra título a todos los jugadores
     */
    private void showTitleToAll(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }
    }
    
    /**
     * Spawn fuegos artificiales
     */
    private void spawnFireworks(int count) {
        // TODO: Implementar spawn de fuegos artificiales
        plugin.getLogger().info("[EcoBrasas] Spawneando " + count + " fuegos artificiales");
    }
    
    /**
     * Transicionar a nueva fase
     */
    private void transicionarFase(Fase nuevaFase) {
        plugin.getLogger().info(String.format("[EcoBrasas] Transición: %s → %s", faseActual, nuevaFase));
        
        // LIMPIEZA DE FASE ANTERIOR
        limpiarFaseAnterior(faseActual);
        
        faseActual = nuevaFase;
        ticksEnFase = 0;
    }
    
    /**
     * Limpia todos los elementos de la fase anterior (grietas, anclas, altares, etc.)
     */
    private void limpiarFaseAnterior(Fase faseAnterior) {
        switch (faseAnterior) {
            case INTRO:
            case TRANSICION_2:
            case TRANSICION_3:
            case VICTORIA:
                // Cinematics: solo cancelar tareas de diálogo (ya hecho en cancelarCinematicasActivas)
                break;
                
            case RECOLECCION:
                // Eliminar todas las grietas activas (ArmorStands + Shulkers)
                for (Map.Entry<Location, org.bukkit.entity.ArmorStand> entry : grietasActivas.entrySet()) {
                    Location loc = entry.getKey();
                    org.bukkit.entity.ArmorStand marker = entry.getValue();
                    
                    // Eliminar ArmorStand (label)
                    if (marker != null && !marker.isDead()) {
                        marker.remove();
                    }
                    
                    // Eliminar Shulker (hitbox) cercano
                    if (loc != null && loc.getWorld() != null) {
                        loc.getWorld().getNearbyEntities(loc, 3, 3, 3).stream()
                            .filter(e -> e instanceof org.bukkit.entity.Shulker)
                            .filter(e -> e.getScoreboardTags().contains("eco_grieta_hitbox"))
                            .forEach(org.bukkit.entity.Entity::remove);
                    }
                }
                grietasActivas.clear();
                grietaHealth.clear();
                plugin.getLogger().info("[EcoBrasas] Limpieza Fase 1: grietas eliminadas");
                break;
                
            case ESTABILIZACION:
                // Eliminar anclas (ArmorStands + Shulkers)
                for (Location loc : anclas.values()) {
                    if (loc != null && loc.getWorld() != null) {
                        loc.getWorld().getNearbyEntities(loc, 3, 3, 3).stream()
                            .filter(e -> (e instanceof org.bukkit.entity.ArmorStand || e instanceof org.bukkit.entity.Shulker))
                            .filter(e -> e.getScoreboardTags().contains("eco_ancla_label") || 
                                       e.getScoreboardTags().contains("eco_ancla_hitbox"))
                            .forEach(org.bukkit.entity.Entity::remove);
                    }
                }
                anclas.clear();
                anclaProgreso.clear();
                plugin.getLogger().info("[EcoBrasas] Limpieza Fase 2: anclas eliminadas");
                break;
                
            case RITUAL_FINAL:
                // Eliminar altar (ArmorStand + Shulker) y guardián si existen
                if (altarLocation != null && altarLocation.getWorld() != null) {
                    altarLocation.getWorld().getNearbyEntities(altarLocation, 5, 5, 5).stream()
                        .filter(e -> e instanceof org.bukkit.entity.ArmorStand || 
                                   e instanceof org.bukkit.entity.Shulker ||
                                   e instanceof org.bukkit.entity.Monster)
                        .filter(e -> e.getScoreboardTags().contains("eco_altar_label") || 
                                   e.getScoreboardTags().contains("eco_altar_hitbox") ||
                                   e.getScoreboardTags().contains("eco_guardian"))
                        .forEach(org.bukkit.entity.Entity::remove);
                }
                altarLocation = null;
                plugin.getLogger().info("[EcoBrasas] Limpieza Fase 3: altar/guardián eliminados");
                break;
        }
    }
    
    /**
     * Limpieza al detener evento
     */
    private void cleanup() {
        plugin.getLogger().info("[EcoBrasas] Iniciando limpieza completa del evento...");
        
        // LIBERAR TODOS LOS BLOQUES DEL EVENTO (grietas, anclas, altar)
        liberarTodosLosBloques();
        
        // Eliminar todas las grietas (ArmorStands + Shulkers)
        for (Map.Entry<Location, org.bukkit.entity.ArmorStand> entry : grietasActivas.entrySet()) {
            Location loc = entry.getKey();
            org.bukkit.entity.ArmorStand marker = entry.getValue();
            
            // Eliminar ArmorStand (label)
            if (marker != null && !marker.isDead()) {
                marker.remove();
            }
            
            // Eliminar Shulker (hitbox) cercano
            if (loc != null && loc.getWorld() != null) {
                loc.getWorld().getNearbyEntities(loc, 3, 3, 3).stream()
                    .filter(e -> e instanceof org.bukkit.entity.Shulker)
                    .filter(e -> e.getScoreboardTags().contains("eco_grieta_hitbox"))
                    .forEach(org.bukkit.entity.Entity::remove);
            }
        }
        grietasActivas.clear();
        grietaHealth.clear();
        
        // Eliminar anclas (ArmorStands + Shulkers)
        for (Location loc : anclas.values()) {
            if (loc != null && loc.getWorld() != null) {
                loc.getWorld().getNearbyEntities(loc, 3, 3, 3).stream()
                    .filter(e -> (e instanceof org.bukkit.entity.ArmorStand || e instanceof org.bukkit.entity.Shulker))
                    .filter(e -> e.getScoreboardTags().contains("eco_ancla_label") || 
                               e.getScoreboardTags().contains("eco_ancla_hitbox"))
                    .forEach(org.bukkit.entity.Entity::remove);
            }
        }
        anclas.clear();
        anclaMarkers.clear();
        anclaProgreso.clear();
        
        // Eliminar altar (Shulker + ArmorStand)
        if (altarLocation != null && altarLocation.getWorld() != null) {
            altarLocation.getWorld().getNearbyEntities(altarLocation, 3, 3, 3).stream()
                .filter(e -> (e instanceof org.bukkit.entity.ArmorStand || e instanceof org.bukkit.entity.Shulker))
                .filter(e -> e.getScoreboardTags().contains("eco_altar_label") || 
                           e.getScoreboardTags().contains("eco_altar_hitbox"))
                .forEach(org.bukkit.entity.Entity::remove);
            altarLocation = null;
        }
        
        // Eliminar guardián si está vivo
        for (World world : Bukkit.getWorlds()) {
            world.getEntities().stream()
                .filter(e -> e.getScoreboardTags().contains("eco_guardian"))
                .forEach(org.bukkit.entity.Entity::remove);
        }
        guardianSpawned = false;
        
        // Limpiar tracking de participación
        participacionGrietas.clear();
        participacionAnclas.clear();
        participacionGuardian.clear();
        
        plugin.getLogger().info("[EcoBrasas] Limpieza completada - todas las entidades eliminadas y bloques liberados");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // INTERACCIÓN CON GRIETAS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Llamado cuando un jugador golpea una grieta
     */
    public void onGrietaGolpeada(Location markerLoc, Player player) {
        if (faseActual != Fase.RECOLECCION) return; // Solo en fase 1
        
        // Buscar el marker más cercano (el ArmorStand que fue golpeado)
        org.bukkit.entity.ArmorStand markerGolpeado = null;
        Location grietaBase = null;
        double distMin = Double.MAX_VALUE;
        
        for (Map.Entry<Location, org.bukkit.entity.ArmorStand> entry : grietasActivas.entrySet()) {
            org.bukkit.entity.ArmorStand marker = entry.getValue();
            if (marker == null || marker.isDead()) continue;
            
            double dist = marker.getLocation().distance(markerLoc);
            if (dist < distMin) {
                distMin = dist;
                markerGolpeado = marker;
                grietaBase = entry.getKey();
            }
        }
        
        if (grietaBase == null || distMin > 3) {
            // Debug
            player.sendMessage("§cDebug: No se encontró grieta cerca (dist: " + String.format("%.2f", distMin) + ")");
            return;
        }
        
        int health = grietaHealth.getOrDefault(grietaBase, GRIETA_MAX_HEALTH);
        health -= 1; // Reducir vida
        
        if (health <= 0) {
            // Grieta cerrada
            cerrarGrieta(grietaBase, markerGolpeado, player);
        } else {
            // Actualizar vida
            grietaHealth.put(grietaBase, health);
            
            // Efectos visuales de daño
            grietaBase.getWorld().spawnParticle(Particle.FLAME, grietaBase.clone().add(0, 2, 0), 10, 0.5, 0.5, 0.5, 0.05);
            grietaBase.getWorld().playSound(grietaBase, Sound.ENTITY_BLAZE_HURT, 0.7f, 1.2f);
            
            // Actualizar nombre del marker con progreso
            int healthPercent = (health * 100) / GRIETA_MAX_HEALTH;
            String healthBar = getHealthBar(healthPercent);
            markerGolpeado.customName(net.kyori.adventure.text.Component.text("§c§l⚠ GRIETA §7" + healthBar + " §c" + healthPercent + "%"));
            
            // ActionBar con feedback inmediato al hacer daño
            player.sendActionBar(String.format(
                "§a§l✔ DAÑO §7• %s §c%d%% §7restante §8(§c%d§7/§c%d§8) §7• §e%d/%d cerradas",
                healthBar, healthPercent, health, GRIETA_MAX_HEALTH, grietasCerradasCount, grietasMetaTotal
            ));
            
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.3f, 1.5f);
        }
    }
    
    /**
     * Cierra una grieta completamente
     */
    private void cerrarGrieta(Location grieta, org.bukkit.entity.ArmorStand marker, Player player) {
        grietasCerradasCount++;
        grietasActivas.remove(grieta);
        grietaHealth.remove(grieta);
        
        // TRACKING: Registrar participación del jugador
        UUID uuid = player.getUniqueId();
        participacionGrietas.put(uuid, participacionGrietas.getOrDefault(uuid, 0) + 1);
        
        // RECOMPENSA XP: 50 XP por cerrar grieta
        ExperienceService expService = plugin.getExperienceService();
        if (expService != null) {
            expService.addXP(player, 50, "Grieta Cerrada", false);
        }
        
        // Eliminar marker (ArmorStand label)
        if (marker != null && !marker.isDead()) {
            marker.remove();
        }
        
        // Eliminar Shulker (hitbox) cercano
        if (grieta != null && grieta.getWorld() != null) {
            grieta.getWorld().getNearbyEntities(grieta, 3, 3, 3).stream()
                .filter(e -> e instanceof org.bukkit.entity.Shulker)
                .filter(e -> e.getScoreboardTags().contains("eco_grieta_hitbox"))
                .forEach(org.bukkit.entity.Entity::remove);
        }
        
        // Efectos visuales masivos
        grieta.getWorld().spawnParticle(Particle.EXPLOSION, grieta, 5, 0, 0, 0, 0);
        grieta.getWorld().spawnParticle(Particle.FLASH, grieta, 3, 0, 0, 0, 0);
        grieta.getWorld().spawnParticle(Particle.SMOKE, grieta.clone().add(0, 2, 0), 100, 2, 2, 2, 0.1);
        
        // Sonidos de cierre
        grieta.getWorld().playSound(grieta, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
        grieta.getWorld().playSound(grieta, Sound.BLOCK_FIRE_EXTINGUISH, 1.5f, 0.8f);
        grieta.getWorld().playSound(grieta, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        
        // Drops de fragmentos
        dropFragments(grieta, player);
        
        // RECOMPENSA: Moneda de Brasa (ítem coleccionable)
        ItemStack moneda = EcoBrasasItems.createMonedaBrasa(1);
        player.getInventory().addItem(moneda);
        player.sendMessage("§6§l[+] §fMoneda de Brasa §7(recuerdo de grieta cerrada)");
        
        // Broadcast
        String coords = String.format("X: %d Z: %d", grieta.getBlockX(), grieta.getBlockZ());
        messageBus.broadcast(String.format("§a§l✓ GRIETA CERRADA §7por §e%s §7en §f%s §8[§e%d§7/§e%d§8]", 
            player.getName(), coords, grietasCerradasCount, grietasMetaTotal), "grieta_cerrada");
        
        playSoundToAll(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
        
        // Title al jugador
        player.sendTitle("§a§l✓ GRIETA CERRADA", 
            String.format("§7Progreso: §e%d§7/§e%d", grietasCerradasCount, grietasMetaTotal),
            10, 40, 10);
        
        plugin.getLogger().info(String.format("[EcoBrasas] Grieta cerrada por %s en %s (%d/%d)", 
            player.getName(), coords, grietasCerradasCount, grietasMetaTotal));
        
        // Generar siguiente grieta inmediatamente si no hemos alcanzado la meta
        if (grietasCerradasCount < grietasMetaTotal) {
            // Spawn después de 3 segundos para dar tiempo a ver la animación
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (faseActual == Fase.RECOLECCION) { // Verificar que sigue en fase 1
                    spawnGrieta();
                }
            }, 60L); // 3 segundos
        } else {
            // Meta completada, transicionar a siguiente fase
            messageBus.broadcast("§6§l✓ ¡TODAS LAS GRIETAS CERRADAS!", "fase1_completa");
            messageBus.broadcast("§7Transicionando a §dFase 2§7...", "transicion");
            
            // LIMPIEZA: Eliminar todas las entidades visuales restantes
            limpiarEntidadesGrietas();
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                transicionarFase(Fase.ESTABILIZACION);
            }, 100L); // 5 segundos
        }
    }
    
    /**
     * Llamado cuando un jugador interactúa con un ancla (Fase 2)
     */
    public void onAnclaInteractuada(Location anclaMarkerLoc, Player player) {
        if (faseActual != Fase.ESTABILIZACION) return; // Solo en Fase 2
        
        // Buscar el ancla más cercana
        int anclaId = -1;
        double distMin = Double.MAX_VALUE;
        
        for (Map.Entry<Integer, Location> entry : anclas.entrySet()) {
            double dist = entry.getValue().distance(anclaMarkerLoc);
            if (dist < distMin && dist < 5) { // Radio de 5 bloques
                distMin = dist;
                anclaId = entry.getKey();
            }
        }
        
        if (anclaId == -1) {
            player.sendMessage("§7[§dEstabilización§7] §cNo se encontró ancla cercana");
            return;
        }
        
        // Verificar si ya está completa
        if (isAnclaCompleta(anclaId)) {
            player.sendMessage("§7[§dEstabilización§7] §a¡Esta ancla ya está completa!");
            return;
        }
        
        // Verificar inventario del jugador
        int ceniza = EcoBrasasItems.countFragments(player, "ceniza");
        int fulgor = EcoBrasasItems.countFragments(player, "fulgor");
        int ecoRoto = EcoBrasasItems.countFragments(player, "eco_roto");
        
        Map<String, Integer> progreso = anclaProgreso.get(anclaId);
        if (progreso == null) {
            progreso = new HashMap<>();
            anclaProgreso.put(anclaId, progreso);
        }
        
        int cenizaActual = progreso.getOrDefault("ceniza", 0);
        int fulgorActual = progreso.getOrDefault("fulgor", 0);
        int ecoRotoActual = progreso.getOrDefault("eco_roto", 0);
        
        // Calcular cuánto falta
        int cenizaNecesaria = Math.max(0, ANCLA_REQ_CENIZA - cenizaActual);
        int fulgorNecesaria = Math.max(0, ANCLA_REQ_FULGOR - fulgorActual);
        int ecoRotoNecesario = Math.max(0, ANCLA_REQ_ECO_ROTO - ecoRotoActual);
        
        // Verificar si tiene algo para entregar
        if (ceniza == 0 && fulgor == 0 && ecoRoto == 0) {
            player.sendMessage("§7[§dEstabilización§7] §cNo tienes fragmentos para entregar");
            player.sendMessage(String.format("§7Falta: §e%d§7C §e%d§7F §e%d§7ER", 
                cenizaNecesaria, fulgorNecesaria, ecoRotoNecesario));
            return;
        }
        
        // Entregar lo que tenga (hasta el máximo necesario)
        int cenizaEntregada = Math.min(ceniza, cenizaNecesaria);
        int fulgorEntregada = Math.min(fulgor, fulgorNecesaria);
        int ecoRotoEntregado = Math.min(ecoRoto, ecoRotoNecesario);
        
        // Consumir items del inventario
        boolean consumido = false;
        if (cenizaEntregada > 0) {
            EcoBrasasItems.consumeFragments(player, "ceniza", cenizaEntregada);
            progreso.put("ceniza", cenizaActual + cenizaEntregada);
            consumido = true;
        }
        if (fulgorEntregada > 0) {
            EcoBrasasItems.consumeFragments(player, "fulgor", fulgorEntregada);
            progreso.put("fulgor", fulgorActual + fulgorEntregada);
            consumido = true;
        }
        if (ecoRotoEntregado > 0) {
            EcoBrasasItems.consumeFragments(player, "eco_roto", ecoRotoEntregado);
            progreso.put("eco_roto", ecoRotoActual + ecoRotoEntregado);
            consumido = true;
        }
        
        if (!consumido) {
            player.sendMessage("§7[§dEstabilización§7] §cNo se pudo entregar nada");
            return;
        }
        
        // Efectos visuales
        Location anclaLoc = anclas.get(anclaId);
        World world = anclaLoc.getWorld();
        world.spawnParticle(Particle.END_ROD, anclaLoc.clone().add(0, 1, 0), 30, 0.3, 1, 0.3, 0.1);
        world.playSound(anclaLoc, Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.0f, 1.5f);
        
        // Mensaje de feedback
        player.sendMessage(String.format("§7[§dEstabilización§7] §aEntregado: §e%d§7C §e%d§7F §e%d§7ER", 
            cenizaEntregada, fulgorEntregada, ecoRotoEntregado));
        
        // Verificar si completó el ancla
        if (isAnclaCompleta(anclaId)) {
            // TRACKING: Registrar participación del jugador
            UUID uuid = player.getUniqueId();
            participacionAnclas.put(uuid, participacionAnclas.getOrDefault(uuid, 0) + 1);
            
            // RECOMPENSA XP: 100 XP por completar ancla
            ExperienceService expService = plugin.getExperienceService();
            if (expService != null) {
                expService.addXP(player, 100, "Ancla Completada", false);
            }
            
            // RECOMPENSA: Cristal de Ancla (ítem coleccionable)
            ItemStack cristal = EcoBrasasItems.createCristalAncla(1);
            player.getInventory().addItem(cristal);
            player.sendMessage("§d§l[+] §fCristal de Ancla §7(recuerdo de ancla estabilizada)");
            
            messageBus.broadcast(String.format("§d§l[Eco de Brasas] §a%s completó el Ancla #%d", 
                player.getName(), anclaId), "ancla_completa");
            world.spawnParticle(Particle.FLASH, anclaLoc, 10, 0, 0, 0, 0);
            world.playSound(anclaLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            
            // Verificar si todas las anclas están completas
            verificarTodasAnclasCompletas();
        } else {
            // Mostrar progreso restante
            int cenizaRestante = ANCLA_REQ_CENIZA - progreso.getOrDefault("ceniza", 0);
            int fulgorRestante = ANCLA_REQ_FULGOR - progreso.getOrDefault("fulgor", 0);
            int ecoRotoRestante = ANCLA_REQ_ECO_ROTO - progreso.getOrDefault("eco_roto", 0);
            player.sendMessage(String.format("§7Falta: §e%d§7C §e%d§7F §e%d§7ER", 
                cenizaRestante, fulgorRestante, ecoRotoRestante));
        }
    }
    
    /**
     * Verifica si todas las anclas están completas para transicionar a Fase 3
     */
    private void verificarTodasAnclasCompletas() {
        boolean todasCompletas = true;
        for (int i = 1; i <= 3; i++) {
            if (!isAnclaCompleta(i)) {
                todasCompletas = false;
                break;
            }
        }
        
        if (todasCompletas) {
            messageBus.broadcast("§d§l[Eco de Brasas] §6¡Todas las anclas estabilizadas!", "transicion_fase3");
            messageBus.broadcast("§d§l[Eco de Brasas] §7Preparando ritual final...", "transicion_fase3_2");
            
            // LIMPIEZA: Eliminar todas las entidades visuales de anclas
            limpiarEntidadesAnclas();
            
            // Transicionar a Fase 3 después de 5 segundos
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                transicionarFase(Fase.RITUAL_FINAL);
            }, 100L); // 5 segundos
        }
    }
    
    /**
     * Llamado cuando un jugador interactúa con el altar (Fase 3)
     */
    public void onAltarInteractuado(Location altarMarkerLoc, Player player) {
        if (faseActual != Fase.RITUAL_FINAL) {
            player.sendMessage("§cDebug: Fase actual es " + faseActual.name() + ", se requiere RITUAL_FINAL");
            return; // Solo en Fase 3
        }
        
        if (altarLocation == null) {
            player.sendMessage("§cDebug: altarLocation es null");
            return;
        }
        
        double distancia = altarLocation.distance(altarMarkerLoc);
        if (distancia > 5) {
            player.sendMessage("§7[§cRitual Final§7] §cNo se encontró altar cercano (dist: " + String.format("%.2f", distancia) + ")");
            return;
        }
        
        // CRÍTICO: Verificar oleada activa ANTES de verificar guardián
        if (oleadaState == OleadaState.ACTIVA) {
            int restantes = enemigosOleada.size();
            
            plugin.getLogger().info(String.format("[EcoBrasas] Ritual BLOQUEADO - Oleada %d activa, %d defensores vivos",
                oleadaActual, restantes));
            
            player.sendMessage(String.format("§c§l[Oleada Activa] §7Derrota a los §c%d defensores §7para continuar", restantes));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 0.8f);
            
            // Efectos de bloqueo
            altarLocation.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, altarLocation.clone().add(0, 2, 0), 10, 0.5, 1, 0.5, 0);
            return;
        }
        
        // CRÍTICO: Verificar guardián ANTES de incrementar pulso
        if (guardianSpawned) {
            // Buscar guardián vivo en radio de 100 bloques
            long guardianesVivos = altarLocation.getWorld().getNearbyEntities(altarLocation, 100, 100, 100).stream()
                .filter(e -> e.getScoreboardTags().contains("eco_guardian"))
                .filter(e -> e instanceof org.bukkit.entity.LivingEntity)
                .filter(e -> !e.isDead())
                .count();
            
            plugin.getLogger().info(String.format("[EcoBrasas] Check guardián: spawned=%b, vivos=%d, pulso actual=%d/%d",
                guardianSpawned, guardianesVivos, pulsoActual, pulsoMaximo));
            
            if (guardianesVivos > 0) {
                plugin.getLogger().info("[EcoBrasas] Ritual BLOQUEADO - Guardián vivo, pulso actual: " + pulsoActual + "/" + pulsoMaximo);
                
                player.sendMessage("§c§l[Ritual Bloqueado] §7El §4Guardián del Eco §7protege el altar");
                player.sendMessage("§7Derrota al guardián para continuar el ritual");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 0.8f);
                
                // Efectos visuales de bloqueo
                altarLocation.getWorld().spawnParticle(Particle.SMOKE, altarLocation.clone().add(0, 2, 0), 10, 0.5, 0.5, 0.5, 0);
                altarLocation.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, altarLocation.clone().add(0, 2, 0), 5, 0.5, 0.5, 0.5, 0);
                
                // NO registrar cooldown - permitir reintentar inmediatamente después de matar guardián
                return;
            } else {
                plugin.getLogger().info("[EcoBrasas] Guardián muerto - permitiendo pulso " + (pulsoActual + 1) + "/" + pulsoMaximo);
            }
        }
        
        // Agregar pulso (sin cooldown)
        pulsoActual++;
        
        plugin.getLogger().info(String.format("[EcoBrasas] Pulso agregado por %s - Progreso: %d/%d", 
            player.getName(), pulsoActual, pulsoMaximo));
        
        // Efectos visuales
        World world = altarLocation.getWorld();
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, altarLocation.clone().add(0, 2, 0), 50, 0.5, 1, 0.5, 0.05);
        world.playSound(altarLocation, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.8f + (pulsoActual * 0.1f));
        
        // Broadcast progreso
        messageBus.broadcast(String.format("§c§l[Ritual] §f%s §7realizó un pulso §e(%d/%d)", 
            player.getName(), pulsoActual, pulsoMaximo), "pulso_ritual");
        
        // Spawn guardián al alcanzar pulso específico (ANTES de verificar victoria)
        if (pulsoActual == pulsoSpawnGuardian && !guardianSpawned) {
            plugin.getLogger().info("[EcoBrasas] Spawning guardian at pulse " + pulsoActual);
            spawnGuardian();
        }
        
        // Verificar victoria SOLO si se alcanza el máximo exacto
        if (pulsoActual >= pulsoMaximo) {
            plugin.getLogger().info("[EcoBrasas] Victoria triggered - Pulsos: " + pulsoActual + "/" + pulsoMaximo);
            victoria();
        }
    }
    
    /**
     * Spawn del guardián del altar (Fase 3, 75% progreso)
     */
    private void spawnGuardian() {
        if (altarLocation == null) {
            plugin.getLogger().warning("[EcoBrasas] spawnGuardian() - altarLocation es NULL");
            return;
        }
        
        World world = altarLocation.getWorld();
        
        // Buscar una buena ubicación cerca del altar (en el suelo, no flotando)
        Location spawnLoc = altarLocation.clone().add(5, 0, 5);
        
        // Asegurar que el bloque de spawn sea sólido (buscar el suelo si está en el aire)
        while (spawnLoc.getBlock().getType().isAir() && spawnLoc.getBlockY() > world.getMinHeight()) {
            spawnLoc.subtract(0, 1, 0);
        }
        // Subir 1 bloque para estar encima del suelo
        spawnLoc.add(0, 1, 0);
        
        // Asegurar que el chunk esté cargado
        if (!world.isChunkLoaded(spawnLoc.getBlockX() >> 4, spawnLoc.getBlockZ() >> 4)) {
            world.loadChunk(spawnLoc.getBlockX() >> 4, spawnLoc.getBlockZ() >> 4);
            plugin.getLogger().info("[EcoBrasas] Chunk cargado para spawn del guardián");
        }
        
        // Validar ubicación de spawn
        plugin.getLogger().info(String.format("[EcoBrasas] Spawning guardian en X:%d Y:%d Z:%d (tipo bloque abajo: %s, chunk loaded: %b)",
            spawnLoc.getBlockX(), spawnLoc.getBlockY(), spawnLoc.getBlockZ(), 
            spawnLoc.clone().subtract(0, 1, 0).getBlock().getType(),
            world.isChunkLoaded(spawnLoc.getBlockX() >> 4, spawnLoc.getBlockZ() >> 4)));
        
        // Calcular nivel promedio de jugadores para escalar dificultad
        int nivelPromedio = calcularNivelPromedioJugadores();
        double hpBase = 200; // 100 corazones base
        double hpMultiplier = 1.0 + (nivelPromedio / 15.0); // +6.67% por nivel
        double damageBase = 10;
        double damageMultiplier = 1.0 + (nivelPromedio / 25.0); // +4% por nivel
        
        int nivelGuardian = 50 + (nivelPromedio * 2); // Nivel visual del guardián
        
        // Spawn Wither Skeleton como guardián usando spawn consumer para configurar ANTES del spawn
        org.bukkit.entity.WitherSkeleton guardian = world.spawn(spawnLoc, org.bukkit.entity.WitherSkeleton.class, (entity) -> {
            // Configurar INMEDIATAMENTE al crear (antes de que aparezca en el mundo)
            double hpFinal = hpBase * hpMultiplier;
            entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(hpFinal);
            entity.setHealth(hpFinal);
            entity.customName(net.kyori.adventure.text.Component.text(
                String.format("§4§l⚔ Guardián del Eco §c[Lv.%d]", nivelGuardian)
            ));
            entity.setCustomNameVisible(true);
            entity.addScoreboardTag("eco_guardian");
            entity.setRemoveWhenFarAway(false);
            entity.setPersistent(true);
            entity.setInvulnerable(false);
            entity.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(damageBase * damageMultiplier);
            entity.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.3 + (nivelPromedio / 200.0));
        });
        
        plugin.getLogger().info(String.format("[EcoBrasas] Guardián creado - UUID: %s, isDead: %b, health: %.1f/%.1f",
            guardian.getUniqueId(), guardian.isDead(), guardian.getHealth(), guardian.getAttribute(Attribute.MAX_HEALTH).getValue()));
        
        // Equipamiento
        guardian.getEquipment().setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHERITE_HELMET));
        guardian.getEquipment().setChestplate(new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHERITE_CHESTPLATE));
        guardian.getEquipment().setItemInMainHand(new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHERITE_SWORD));
        
        // Efectos visuales de spawn
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, spawnLoc, 100, 1, 2, 1, 0.1);
        world.spawnParticle(Particle.EXPLOSION, spawnLoc, 5, 0, 0, 0, 0);
        world.playSound(spawnLoc, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.8f);
        
        messageBus.broadcast("§c§l[RITUAL] §4¡El Guardián del Eco ha despertado!", "guardian_spawn");
        
        guardianSpawned = true;
        
        plugin.getLogger().info(String.format("[EcoBrasas] Guardián spawneado - Location: %s, Tags: %s, Persistent: %b",
            spawnLoc, guardian.getScoreboardTags(), guardian.isPersistent()));
        
        // Monitoreo periódico del guardián para detectar desaparición
        final java.util.UUID guardianUUID = guardian.getUniqueId();
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!guardianSpawned) return; // Ya no importa si ya terminó
            
            // Buscar el guardián por UUID
            org.bukkit.entity.Entity entity = Bukkit.getEntity(guardianUUID);
            
            if (entity == null || entity.isDead()) {
                plugin.getLogger().severe(String.format("[EcoBrasas] ¡GUARDIÁN DESAPARECIÓ! UUID: %s, entity==null: %b, isDead: %b",
                    guardianUUID, entity == null, entity != null && entity.isDead()));
            } else {
                plugin.getLogger().info(String.format("[EcoBrasas] Guardián vivo - Health: %.1f/%.1f, Location: %s",
                    ((org.bukkit.entity.LivingEntity)entity).getHealth(),
                    ((org.bukkit.entity.LivingEntity)entity).getAttribute(Attribute.MAX_HEALTH).getValue(),
                    entity.getLocation()));
            }
        }, 20L, 20L); // Cada segundo
        
        // Verificar inmediatamente que sigue vivo
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (guardian.isDead()) {
                plugin.getLogger().warning("[EcoBrasas] ¡¡GUARDIÁN MURIÓ INMEDIATAMENTE!! Cause: " + guardian.getLastDamageCause());
            } else {
                plugin.getLogger().info("[EcoBrasas] Guardián confirmado vivo después de 1 tick - Health: " + guardian.getHealth());
            }
        }, 1L);
    }
    
    /**
     * Sistema de oleadas dinámicas - gestiona spawns y progresión
     */
    private void tickOleadas() {
        // Verificar si hay oleada activa
        if (oleadaState == OleadaState.ACTIVA) {
            // Limpiar enemigos muertos de la lista
            enemigosOleada.removeIf(e -> e == null || e.isDead());
            
            // Si no quedan enemigos, oleada completada
            if (enemigosOleada.isEmpty()) {
                completarOleada();
            }
            
            // Efectos visuales de oleada activa cada 2 segundos
            if (ticksEnFase % 40 == 0) {
                mostrarMarcadorOleada();
            }
        }
        
        // Verificar si debe spawnear nueva oleada
        boolean debeSpawnear = false;
        
        // Oleadas cada X pulsos (calculado dinámicamente: cada 20% del total)
        if (pulsoActual > 0 && pulsoActual % intervaloOleadas == 0 && ultimaOleadaPulso != pulsoActual) {
            debeSpawnear = true;
        }
        
        // Spawn guardián como oleada especial en pulso específico
        if (!guardianSpawned && pulsoActual >= pulsoSpawnGuardian && oleadaState == OleadaState.ESPERANDO) {
            spawnGuardian();
            guardianSpawned = true;
            oleadaState = OleadaState.ACTIVA; // Bloquea pulsos hasta matar guardián
            ultimaOleadaPulso = pulsoActual;
        }
        // Spawn oleada normal
        else if (debeSpawnear && oleadaState == OleadaState.ESPERANDO) {
            spawnOleada();
            ultimaOleadaPulso = pulsoActual;
        }
    }
    
    /**
     * Spawn oleada de enemigos según intensidad del ritual
     */
    private void spawnOleada() {
        oleadaActual++;
        oleadaState = OleadaState.ACTIVA;
        enemigosOleada.clear();
        
        // Calcular nivel promedio de jugadores
        int nivelPromedio = calcularNivelPromedioJugadores();
        
        // Calcular cantidad y tipo de enemigos según intensidad (0-100) Y nivel de jugadores
        intensidadRitual = (int) ((pulsoActual / (double) pulsoMaximo) * 100);
        
        // Base + intensidad + nivel jugadores (más desafío)
        int cantidadEnemigos = 2 + (intensidadRitual / 20) + (nivelPromedio / 10); // 2-12+ enemigos
        World world = altarLocation.getWorld();
        
        // Anunciar oleada
        String fase = intensidadRitual < 25 ? "§7Inicial" :
                      intensidadRitual < 50 ? "§eMedia" :
                      intensidadRitual < 75 ? "§6Avanzada" : "§c§lFinal";
        
        messageBus.broadcast(String.format("§c§l⚔ OLEADA %d §8[%s§8] §7- §c%d defensores §7aparecen!", 
            oleadaActual, fase, cantidadEnemigos), "oleada_" + oleadaActual);
        
        // Sonido épico
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.2f);
        }
        
        // Spawn enemigos en círculo alrededor del altar
        double radius = 10.0;
        for (int i = 0; i < cantidadEnemigos; i++) {
            double angle = (2 * Math.PI * i) / cantidadEnemigos;
            double x = altarLocation.getX() + radius * Math.cos(angle);
            double z = altarLocation.getZ() + radius * Math.sin(angle);
            
            // Encontrar suelo
            Location spawnLoc = new Location(world, x, altarLocation.getY(), z);
            for (int y = 0; y < 10; y++) {
                Location check = spawnLoc.clone().add(0, -y, 0);
                if (check.getBlock().getType().isSolid()) {
                    spawnLoc = check.clone().add(0, 1, 0);
                    break;
                }
            }
            
            // Tipo de enemigo según intensidad
            org.bukkit.entity.EntityType tipoEnemigo = seleccionarTipoEnemigo(intensidadRitual);
            
            // Spawn con configuración
            org.bukkit.entity.Entity spawnedEntity = world.spawnEntity(spawnLoc, tipoEnemigo);
            
            if (spawnedEntity instanceof org.bukkit.entity.LivingEntity living) {
                // Configurar HP según intensidad Y nivel de jugadores (más desafío)
                double hpMultiplier = 1.0 + (intensidadRitual / 100.0) + (nivelPromedio / 20.0);
                double damageMultiplier = 1.0 + (nivelPromedio / 30.0);
                
                living.getAttribute(Attribute.MAX_HEALTH).setBaseValue(
                    living.getAttribute(Attribute.MAX_HEALTH).getValue() * hpMultiplier
                );
                living.setHealth(living.getAttribute(Attribute.MAX_HEALTH).getValue());
                
                // Aumentar daño según nivel
                if (living.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
                    living.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(
                        living.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * damageMultiplier
                    );
                }
                
                // Nombre personalizado con nivel efectivo
                int nivelEnemigo = oleadaActual + (nivelPromedio / 5);
                living.customName(net.kyori.adventure.text.Component.text(
                    String.format("§c⚔ Defensor Lv.%d", nivelEnemigo)
                ));
                living.setCustomNameVisible(true);
                
                // Añadir a lista de tracking
                enemigosOleada.add(spawnedEntity);
                
                // Efectos de spawn
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, spawnLoc.clone().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.05);
            }
        }
    }
    
    /**
     * Calcula el nivel promedio de todos los jugadores online
     */
    private int calcularNivelPromedioJugadores() {
        ExperienceService expService = plugin.getExperienceService();
        if (expService == null) {
            return 1; // Fallback nivel básico
        }
        
        int totalNivel = 0;
        int count = 0;
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            totalNivel += expService.getLevel(p);
            count++;
        }
        
        return count > 0 ? (totalNivel / count) : 1;
    }
    
    /**
     * Seleccionar tipo de enemigo según intensidad del ritual
     */
    private org.bukkit.entity.EntityType seleccionarTipoEnemigo(int intensidad) {
        if (intensidad < 25) {
            // Fase inicial: zombies y esqueletos
            return Math.random() < 0.5 ? 
                org.bukkit.entity.EntityType.ZOMBIE : 
                org.bukkit.entity.EntityType.SKELETON;
        } else if (intensidad < 50) {
            // Fase media: añadir spiders y creepers
            double rand = Math.random();
            if (rand < 0.3) return org.bukkit.entity.EntityType.ZOMBIE;
            if (rand < 0.6) return org.bukkit.entity.EntityType.SKELETON;
            if (rand < 0.8) return org.bukkit.entity.EntityType.SPIDER;
            return org.bukkit.entity.EntityType.CREEPER;
        } else if (intensidad < 75) {
            // Fase avanzada: enemigos más fuertes
            double rand = Math.random();
            if (rand < 0.25) return org.bukkit.entity.EntityType.ZOMBIE;
            if (rand < 0.5) return org.bukkit.entity.EntityType.SKELETON;
            if (rand < 0.7) return org.bukkit.entity.EntityType.BLAZE;
            return org.bukkit.entity.EntityType.PIGLIN_BRUTE;
        } else {
            // Fase final: élite
            double rand = Math.random();
            if (rand < 0.3) return org.bukkit.entity.EntityType.BLAZE;
            if (rand < 0.6) return org.bukkit.entity.EntityType.PIGLIN_BRUTE;
            if (rand < 0.8) return org.bukkit.entity.EntityType.WITHER_SKELETON;
            return org.bukkit.entity.EntityType.RAVAGER;
        }
    }
    
    /**
     * Completar oleada actual y permitir progreso
     */
    private void completarOleada() {
        oleadaState = OleadaState.COMPLETADA;
        
        messageBus.broadcast("§a§l✓ OLEADA COMPLETADA §8- §7El ritual puede continuar...", "oleada_completada");
        
        // Efectos de victoria
        altarLocation.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, 
            altarLocation.clone().add(0, 2, 0), 50, 2, 2, 2, 0.1);
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
        
        // Reset para próxima oleada
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            oleadaState = OleadaState.ESPERANDO;
            enemigosOleada.clear();
        }, 60L); // 3 segundos de gracia
    }
    
    /**
     * Mostrar marcador visual de oleada activa
     */
    private void mostrarMarcadorOleada() {
        int restantes = enemigosOleada.size();
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendActionBar(net.kyori.adventure.text.Component.text(
                String.format("§c⚔ OLEADA %d §8- §7Defensores: §c%d", oleadaActual, restantes)
            ));
        }
        
        // Beam visual en el altar
        altarLocation.getWorld().spawnParticle(Particle.END_ROD, 
            altarLocation.clone().add(0, 1, 0), 10, 0.2, 3, 0.2, 0.02);
    }
    
    /**
     * Efectos visuales progresivos según intensidad del ritual
     */
    private void tickEfectosRitual() {
        // Actualizar intensidad (0-100)
        intensidadRitual = (int) ((pulsoActual / (double) pulsoMaximo) * 100);
        
        // Frecuencia de efectos según intensidad
        int intervalo = Math.max(20, 100 - intensidadRitual); // 20-100 ticks
        
        if (ticksEnFase % intervalo != 0) return;
        
        World world = altarLocation.getWorld();
        Location center = altarLocation.clone().add(0, 2, 0);
        
        // Fase 1 (0-25%): Efectos sutiles
        if (intensidadRitual < 25) {
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, center, 5, 0.5, 1, 0.5, 0.01);
            if (ticksEnFase % 80 == 0) { // Sonido ocasional
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(altarLocation, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.3f, 0.8f);
                }
            }
        }
        // Fase 2 (25-50%): Efectos moderados
        else if (intensidadRitual < 50) {
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, center, 10, 0.7, 1.5, 0.7, 0.02);
            world.spawnParticle(Particle.SMOKE, center, 5, 0.5, 1, 0.5, 0.01);
            if (ticksEnFase % 60 == 0) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(altarLocation, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.5f, 1.0f);
                }
            }
        }
        // Fase 3 (50-75%): Efectos intensos
        else if (intensidadRitual < 75) {
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, center, 15, 1, 2, 1, 0.03);
            world.spawnParticle(Particle.FLAME, center, 10, 1, 1.5, 1, 0.02);
            world.spawnParticle(Particle.LAVA, center, 5, 0.5, 1, 0.5, 0);
            if (ticksEnFase % 40 == 0) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(altarLocation, Sound.ENTITY_WITHER_AMBIENT, 0.4f, 1.5f);
                }
            }
        }
        // Fase 4 (75-100%): Efectos épicos
        else {
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, center, 25, 1.5, 2.5, 1.5, 0.05);
            world.spawnParticle(Particle.FLAME, center, 20, 1.5, 2, 1.5, 0.03);
            world.spawnParticle(Particle.LAVA, center, 10, 1, 1.5, 1, 0);
            world.spawnParticle(Particle.END_ROD, center, 15, 2, 3, 2, 0.02);
            
            // Beam vertical
            for (int y = 0; y < 10; y++) {
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, 
                    altarLocation.clone().add(0, y, 0), 3, 0.1, 0, 0.1, 0);
            }
            
            if (ticksEnFase % 30 == 0) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(altarLocation, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.8f);
                }
            }
        }
    }
    
    /**
     * Completar evento con victoria
     */
    private void victoria() {
        messageBus.broadcast("§6§l═════════════════════════════════════", "victoria_1");
        messageBus.broadcast("§e§l          ¡RITUAL COMPLETADO!", "victoria_2");
        messageBus.broadcast("§6§l═════════════════════════════════════", "victoria_3");
        
        // Efectos visuales épicos en el altar
        World world = altarLocation.getWorld();
        for (int i = 0; i < 5; i++) {
            int delay = i * 10;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                world.spawnParticle(Particle.END_ROD, altarLocation.clone().add(0, 3, 0), 200, 2, 3, 2, 0.3);
                world.spawnParticle(Particle.FLASH, altarLocation, 20, 0, 0, 0, 0);
                world.playSound(altarLocation, Sound.UI_TOAST_CHALLENGE_COMPLETE, 2.0f, 1.0f);
            }, delay);
        }
        
        // Diálogo final del Observador explicando el propósito (simple y nostálgico)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            messageBus.broadcast("", "space1");
            messageBus.broadcast("§7§m                                                ", "separator1");
            messageBus.broadcast("§6§l🌀 OBSERVADOR:", "observador");
            messageBus.broadcast("§f\"El fuego nunca murió.\"", "mensaje1");
            messageBus.broadcast("§f\"Ardía bajo tierra, esperando escapar.\"", "mensaje2");
            messageBus.broadcast("", "space2");
            messageBus.broadcast("§f\"Cerramos las grietas. Estabilizamos las anclas.\"", "mensaje3");
            messageBus.broadcast("§f\"El ritual convirtió su rabia en luz.\"", "mensaje4");
            messageBus.broadcast("", "space3");
            messageBus.broadcast("§f\"Lo que casi nos §cconsumiò§f...\"", "mensaje5");
            messageBus.broadcast("§f\"...ahora §enos protege§f.\"", "mensaje6");
            messageBus.broadcast("", "space4");
            messageBus.broadcast("§7Gracias por estar aquí.", "gracias1");
            messageBus.broadcast("§7Este momento §eno se repetirá§7.", "gracias2");
            messageBus.broadcast("§7§m                                                ", "separator2");
            messageBus.broadcast("", "space5");
        }, 60L); // 3 segundos después del inicio de victoria
        
        // Recompensas finales (después del diálogo)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ExperienceService expService = plugin.getExperienceService();
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                
                int grietas = participacionGrietas.getOrDefault(uuid, 0);
                int anclasCompletadas = participacionAnclas.getOrDefault(uuid, 0);
                boolean guardiàn = participacionGuardian.getOrDefault(uuid, false);
                
                // XP base + bonos
                int xpTotal = 200; // Base por completar evento
                xpTotal += grietas * 30; // +30 XP por grieta
                xpTotal += anclasCompletadas * 50; // +50 XP por ancla
                if (guardiàn) xpTotal += 150; // +150 XP por guardián
                
                // Dar XP
                if (expService != null && xpTotal > 0) {
                    expService.addXP(player, xpTotal, "Eco de Brasas Completado", false);
                    player.sendMessage(String.format("§e§l[XP] §a+%d XP §7por participación en el evento", xpTotal));
                }
                
                // PS = XP (sincronizado)
                int psGanados = xpTotal;
                if (psGanados > 0) {
                    plugin.getMissionService().addPS(uuid, psGanados, "Evento: Eco de Brasas");
                    player.sendMessage(String.format("§e§l[PS] §a+%d PS §7(sincronizado con XP)", psGanados));
                }
                
                // Luz Templada (útil)
                org.bukkit.inventory.ItemStack luzTemplada = EcoBrasasItems.createLuzTemplada(1);
                player.getInventory().addItem(luzTemplada);
                
                // Emblema de Victoria (recuerdo de participación)
                org.bukkit.inventory.ItemStack emblema = EcoBrasasItems.createEmblemaVictoria();
                player.getInventory().addItem(emblema);
                
                player.sendMessage("§6§l[Recompensa] §fRecibiste §e§lLuz Templada");
                player.sendMessage("§e§l[Emblema] §fEmblema del Eco Templado §7(recuerdo único)");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
            
            messageBus.broadcast("§e§l✦ §7Todos recibieron §e§lLuz Templada§7, §e§lEmblema§7, §aXP§7 y §aPS §7por participación §e§l✦", "recompensa");
        }, 260L); // 13 segundos después (10s más que el diálogo)
        
        // Transicionar a VICTORIA y detener evento
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            transicionarFase(Fase.VICTORIA);
            stop(); // Detener evento completamente
        }, 320L); // 16 segundos después (dar tiempo a leer todo)
    }
    
    /**
     * Barra de vida visual
     */
    private String getHealthBar(int percent) {
        int bars = (percent / 10);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i < bars) {
                sb.append("§c▓");
            } else {
                sb.append("§7░");
            }
        }
        return sb.toString();
    }
    
    /**
     * Da fragmentos directamente al inventario del jugador (con cantidades aumentadas)
     */
    private void dropFragments(Location loc, Player player) {
        World world = loc.getWorld();
        
        // Ceniza - 15 por grieta (necesita 90 total para 10 grietas → 6 grietas cubren 1 ancla)
        ItemStack ceniza = EcoBrasasItems.createCeniza(15);
        player.getInventory().addItem(ceniza);
        player.sendMessage("§7[§6Recompensa§7] §f+15 Fragmentos de §7Ceniza");
        
        // Fulgor - 6 por grieta (necesita 30 total para 10 grietas → 5 grietas cubren 1 ancla)
        ItemStack fulgor = EcoBrasasItems.createFulgor(6);
        player.getInventory().addItem(fulgor);
        player.sendMessage("§7[§6Recompensa§7] §f+6 Fragmentos de §6Fulgor");
        
        // Eco Roto - 1 cada ~3 grietas (33% chance, necesita 3 total)
        if (random.nextInt(100) < 33) {
            ItemStack ecoRoto = EcoBrasasItems.createEcoRoto(1);
            player.getInventory().addItem(ecoRoto);
            player.sendMessage("§7[§6Recompensa§7] §fEco §5Roto §7§l(¡RARO!)");
        }
        
        // Efectos de recompensa
        world.spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        world.playSound(loc, Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // MÉTODOS DE LIMPIEZA DE ENTIDADES
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Limpia todas las entidades visuales de grietas (ArmorStands, hitboxes)
     */
    private void limpiarEntidadesGrietas() {
        plugin.getLogger().info("[EcoBrasas] Limpiando entidades de grietas...");
        
        // Marcar ubicaciones de grietas como rompibles
        for (Location loc : grietasActivas.keySet()) {
            if (loc != null) {
                // Marcar el bloque de la grieta como rompible
                bloquesRompibles.add(loc.clone());
            }
        }
        
        // Eliminar todos los ArmorStands de grietas
        for (org.bukkit.entity.ArmorStand marker : grietasActivas.values()) {
            if (marker != null && !marker.isDead()) {
                // Eliminar ArmorStands visuales cercanos (el item flotante)
                marker.getWorld().getNearbyEntities(marker.getLocation(), 3, 3, 3).stream()
                    .filter(e -> e instanceof org.bukkit.entity.ArmorStand)
                    .filter(e -> e.getScoreboardTags().contains("eco_grieta"))
                    .forEach(org.bukkit.entity.Entity::remove);
                
                marker.remove();
            }
        }
        
        // Eliminar todos los Shulkers (hitboxes) con tag eco_grieta_hitbox
        for (Location loc : grietasActivas.keySet()) {
            if (loc != null && loc.getWorld() != null) {
                loc.getWorld().getNearbyEntities(loc, 5, 5, 5).stream()
                    .filter(e -> e instanceof org.bukkit.entity.Shulker)
                    .filter(e -> e.getScoreboardTags().contains("eco_grieta_hitbox"))
                    .forEach(org.bukkit.entity.Entity::remove);
            }
        }
        
        grietasActivas.clear();
        plugin.getLogger().info("[EcoBrasas] Limpieza de grietas completada - bloques ahora rompibles");
    }
    
    /**
     * Limpia todas las entidades visuales de anclas (ArmorStands)
     */
    private void limpiarEntidadesAnclas() {
        plugin.getLogger().info("[EcoBrasas] Limpiando entidades de anclas...");
        
        // Marcar todas las ubicaciones de anclas (3x3 + estructuras) como rompibles
        for (Location anclaLoc : anclas.values()) {
            if (anclaLoc != null) {
                World world = anclaLoc.getWorld();
                int x = anclaLoc.getBlockX();
                int y = anclaLoc.getBlockY();
                int z = anclaLoc.getBlockZ();
                
                // Marcar todos los bloques de la estructura del ancla (3x3 base + decoraciones)
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        bloquesRompibles.add(new Location(world, x+dx, y, z+dz));
                        bloquesRompibles.add(new Location(world, x+dx, y+1, z+dz));
                    }
                }
            }
        }
        
        // Eliminar todos los ArmorStands de anclas
        for (org.bukkit.entity.ArmorStand marker : anclaMarkers.values()) {
            if (marker != null && !marker.isDead()) {
                // Eliminar ArmorStands visuales cercanos (el item flotante + instrucciones)
                marker.getWorld().getNearbyEntities(marker.getLocation(), 3, 3, 3).stream()
                    .filter(e -> e instanceof org.bukkit.entity.ArmorStand)
                    .filter(e -> e.getScoreboardTags().contains("eco_ancla"))
                    .forEach(org.bukkit.entity.Entity::remove);
                
                marker.remove();
            }
        }
        
        anclaMarkers.clear();
        plugin.getLogger().info("[EcoBrasas] Limpieza de anclas completada - bloques ahora rompibles");
    }
    
    /**
     * Limpia todas las entidades visuales del altar (ArmorStands)
     */
    private void limpiarEntidadesAltar() {
        plugin.getLogger().info("[EcoBrasas] Limpiando entidades del altar...");
        
        if (altarLocation != null && altarLocation.getWorld() != null) {
            World world = altarLocation.getWorld();
            int x = altarLocation.getBlockX();
            int y = altarLocation.getBlockY();
            int z = altarLocation.getBlockZ();
            
            // Marcar todos los bloques de la estructura del altar (5x5 base + 3 niveles) como rompibles
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    bloquesRompibles.add(new Location(world, x+dx, y, z+dz));     // Base obsidiana
                    bloquesRompibles.add(new Location(world, x+dx, y+1, z+dz));   // Nivel 1
                    bloquesRompibles.add(new Location(world, x+dx, y+2, z+dz));   // Nivel 2 (beacon + decoraciones)
                }
            }
            
            // Eliminar todos los ArmorStands con tags de altar
            altarLocation.getWorld().getNearbyEntities(altarLocation, 10, 10, 10).stream()
                .filter(e -> e instanceof org.bukkit.entity.ArmorStand)
                .filter(e -> e.getScoreboardTags().contains("eco_altar"))
                .forEach(org.bukkit.entity.Entity::remove);
        }
        
        plugin.getLogger().info("[EcoBrasas] Limpieza del altar completada - bloques ahora rompibles");
    }
    
    /**
     * Libera TODOS los bloques del evento (grietas, anclas, altar) para que puedan romperse
     * Se llama al finalizar el evento (victoria o cleanup)
     */
    private void liberarTodosLosBloques() {
        plugin.getLogger().info("[EcoBrasas] Liberando TODOS los bloques del evento...");
        
        // Liberar bloques de grietas (todas las que existieron)
        for (Location grietaLoc : grietasActivas.keySet()) {
            if (grietaLoc != null) {
                bloquesRompibles.add(grietaLoc.clone());
            }
        }
        
        // Liberar bloques de anclas (3x3 + estructuras completas)
        for (Location anclaLoc : anclas.values()) {
            if (anclaLoc != null) {
                World world = anclaLoc.getWorld();
                int x = anclaLoc.getBlockX();
                int y = anclaLoc.getBlockY();
                int z = anclaLoc.getBlockZ();
                
                // Marcar todos los bloques de la estructura del ancla
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        bloquesRompibles.add(new Location(world, x+dx, y, z+dz));
                        bloquesRompibles.add(new Location(world, x+dx, y+1, z+dz));
                    }
                }
            }
        }
        
        // Liberar bloques del altar (5x5 base + 3 niveles)
        if (altarLocation != null && altarLocation.getWorld() != null) {
            World world = altarLocation.getWorld();
            int x = altarLocation.getBlockX();
            int y = altarLocation.getBlockY();
            int z = altarLocation.getBlockZ();
            
            // Marcar todos los bloques de la estructura del altar
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    bloquesRompibles.add(new Location(world, x+dx, y, z+dz));     // Base
                    bloquesRompibles.add(new Location(world, x+dx, y+1, z+dz));   // Nivel 1
                    bloquesRompibles.add(new Location(world, x+dx, y+2, z+dz));   // Nivel 2
                }
            }
        }
        
        plugin.getLogger().info("[EcoBrasas] " + bloquesRompibles.size() + " bloques liberados - ahora pueden romperse");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // API PÚBLICA (para comandos)
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Verifica si un bloque del evento puede ser roto
     * @param blockLoc Ubicación del bloque
     * @return true si el bloque puede romperse (ha sido liberado), false si está protegido
     */
    public boolean puedeRomperseBloque(org.bukkit.Location blockLoc) {
        // Si el evento NO está activo, todos los bloques pueden romperse
        if (!eventoActivo) {
            return true;
        }
        
        // Normalizar la ubicación (solo coordenadas de bloque)
        org.bukkit.Location normalized = new org.bukkit.Location(
            blockLoc.getWorld(),
            blockLoc.getBlockX(),
            blockLoc.getBlockY(),
            blockLoc.getBlockZ()
        );
        
        return bloquesRompibles.contains(normalized);
    }
    
    /**
     * Verifica si el evento está actualmente activo
     * @return true si el evento está activo
     */
    public boolean isEventoActivo() {
        return eventoActivo;
    }
    
    public String getFaseActual() {
        return faseActual.name();
    }
    
    /**
     * Obtiene el progreso de la fase actual (0-100%)
     */
    public int getProgresoFase() {
        switch (faseActual) {
            case INTRO:
            case TRANSICION_2:
            case TRANSICION_3:
            case VICTORIA:
                return 0; // Cinematics no tienen progreso medible
                
            case RECOLECCION:
                // Progreso basado en grietas cerradas
                if (grietasMetaTotal == 0) return 0;
                return (grietasCerradasCount * 100) / grietasMetaTotal;
                
            case ESTABILIZACION:
                // Progreso promedio de las 3 anclas
                int totalProgreso = 0;
                int totalRequerido = ANCLA_REQ_CENIZA + ANCLA_REQ_FULGOR + ANCLA_REQ_ECO_ROTO; // 41
                for (int i = 1; i <= 3; i++) {
                    Map<String, Integer> progreso = anclaProgreso.get(i);
                    if (progreso != null) {
                        int entregado = progreso.getOrDefault("ceniza", 0) + 
                                       progreso.getOrDefault("fulgor", 0) + 
                                       progreso.getOrDefault("eco_roto", 0);
                        totalProgreso += (entregado * 100) / totalRequerido;
                    }
                }
                return totalProgreso / 3;
                
            case RITUAL_FINAL:
                return (pulsoActual * 100) / pulsoMaximo;
                
            default:
                return 0;
        }
    }
    
    /**
     * Forzar cambio de fase por comando
     */
    public boolean forzarFase(String fase) {
        Fase faseObjetivo = null;
        
        switch (fase.toLowerCase()) {
            case "1":
            case "recoleccion":
                faseObjetivo = Fase.RECOLECCION;
                break;
            case "2":
            case "estabilizacion":
                faseObjetivo = Fase.ESTABILIZACION;
                break;
            case "3":
            case "ritual":
                faseObjetivo = Fase.RITUAL_FINAL;
                break;
            default:
                return false;
        }
        
        if (faseActual == faseObjetivo) {
            return false; // Ya estamos en esa fase
        }
        
        // Cancelar cualquier tarea de diálogo en progreso
        cancelarCinematicasActivas();
        
        // Transicionar a la fase
        transicionarFase(faseObjetivo);
        
        plugin.getLogger().info(String.format("[EcoBrasas] Forzada fase: %s -> %s", 
            faseActual.name(), faseObjetivo.name()));
        
        return true;
    }
    
    /**
     * Avanzar a la siguiente fase (saltando cinematics si hay)
     */
    public boolean forzarSiguienteFase() {
        Fase siguienteFase = null;
        
        switch (faseActual) {
            case INTRO:
            case TRANSICION_2:
                siguienteFase = Fase.RECOLECCION;
                break;
            case RECOLECCION:
                siguienteFase = Fase.ESTABILIZACION;
                break;
            case ESTABILIZACION:
            case TRANSICION_3:
                siguienteFase = Fase.RITUAL_FINAL;
                break;
            case RITUAL_FINAL:
                siguienteFase = Fase.VICTORIA;
                break;
            case VICTORIA:
                return false; // Ya terminó
        }
        
        if (siguienteFase == null) {
            return false;
        }
        
        // Cancelar cinematics activas
        cancelarCinematicasActivas();
        
        // Transicionar
        transicionarFase(siguienteFase);
        
        plugin.getLogger().info(String.format("[EcoBrasas] Avanzado: %s -> %s", 
            faseActual.name(), siguienteFase.name()));
        
        return true;
    }
    
    /**
     * Cancela tasks de cinematics activas para permitir skip
     */
    private void cancelarCinematicasActivas() {
        if (dialogoTask != null && !dialogoTask.isCancelled()) {
            dialogoTask.cancel();
            dialogoTask = null;
        }
    }
    
    public String getInfoDetallada() {
        StringBuilder sb = new StringBuilder();
        sb.append("§e§l=== ECO DE BRASAS - INFO ===\n");
        sb.append("§7Fase: §e").append(faseActual.name()).append("\n");
        sb.append("§7Progreso: §e").append(getProgresoFase()).append("%\n");
        sb.append("§7Tiempo en fase: §e").append(ticksEnFase / 20).append("s (")
          .append(ticksEnFase / 1200).append("m)\n");
        sb.append("§7Tiempo total: §e").append(ticksTotales / 20).append("s (")
          .append(ticksTotales / 1200).append("m)\n");
        
        // Info específica por fase
        switch (faseActual) {
            case RECOLECCION:
                sb.append("§7Grietas cerradas: §e").append(grietasCerradasCount)
                  .append("§7/§e").append(grietasMetaTotal).append("\n");
                sb.append("§7Grietas activas: §e").append(grietasActivas.size());
                break;
                
            case ESTABILIZACION:
                sb.append("§7Anclas completas: §e");
                int completas = 0;
                for (int i = 1; i <= 3; i++) {
                    if (isAnclaCompleta(i)) {
                        completas++;
                    }
                }
                sb.append(completas).append("§7/§e3\n");
                
                for (int i = 1; i <= 3; i++) {
                    Map<String, Integer> prog = anclaProgreso.get(i);
                    int ceniza = prog != null ? prog.getOrDefault("ceniza", 0) : 0;
                    int fulgor = prog != null ? prog.getOrDefault("fulgor", 0) : 0;
                    int ecoRoto = prog != null ? prog.getOrDefault("eco_roto", 0) : 0;
                    sb.append(String.format("§7  Ancla %d: §e%d§7C §e%d§7F §e%d§7ER §7(§e%d/%d§7/§e%d§7)\n", 
                        i, ceniza, fulgor, ecoRoto, ANCLA_REQ_CENIZA, ANCLA_REQ_FULGOR, ANCLA_REQ_ECO_ROTO));
                }
                break;
                
            case RITUAL_FINAL:
                int progresoRitual = (pulsoActual * 100) / pulsoMaximo;
                sb.append("§7Ritual progreso: §e").append(progresoRitual).append("%\n");
                sb.append("§7Pulso actual: §e").append(pulsoActual).append("§7/§e").append(pulsoMaximo).append("\n");
                sb.append("§7Guardián spawneado: §e")
                  .append(guardianSpawned ? "Sí" : "No");
                break;
                
            default:
                break;
        }
        
        return sb.toString();
    }
    
    /**
     * Completar ancla por comando
     */
    public boolean completarAncla(int anclaId) {
        if (anclaId < 1 || anclaId > 3) {
            return false;
        }
        
        if (faseActual != Fase.ESTABILIZACION) {
            return false; // Solo funciona en fase 2
        }
        
        if (!anclas.containsKey(anclaId)) {
            return false; // Ancla no existe
        }
        
        // Marcar como completa (llenar todo a máximo)
        Map<String, Integer> completo = new HashMap<>();
        completo.put("ceniza", ANCLA_REQ_CENIZA);
        completo.put("fulgor", ANCLA_REQ_FULGOR);
        completo.put("eco_roto", ANCLA_REQ_ECO_ROTO);
        anclaProgreso.put(anclaId, completo);
        
        // Efectos visuales
        Location loc = anclas.get(anclaId);
        loc.getWorld().spawnParticle(Particle.FLASH, loc, 5, 0, 0, 0, 0);
        loc.getWorld().spawnParticle(Particle.FLAME, loc, 200, 3, 3, 3, 0.2);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 1.2f);
        
        messageBus.broadcast(String.format("§a§l✓ ANCLA %d COMPLETADA §7(comando admin)", anclaId), "ancla_completada");
        
        plugin.getLogger().info(String.format("[EcoBrasas] Ancla %d completada por comando", anclaId));
        
        return true;
    }
    
    /**
     * Añadir pulsos al ritual (fase 3)
     */
    public void addPulsoGlobal(int cantidad) {
        if (faseActual != Fase.RITUAL_FINAL) {
            plugin.getLogger().warning("[EcoBrasas] addPulsoGlobal solo funciona en fase 3");
            return;
        }
        
        // Usar pulsoActual, no ritualProgreso
        int antes = pulsoActual;
        pulsoActual = Math.max(0, Math.min(pulsoMaximo, pulsoActual + cantidad));
        
        plugin.getLogger().info(String.format("[EcoBrasas] Pulso ajustado: %d + %d = %d", 
            antes, cantidad, pulsoActual));
        
        // Efectos visuales si hay altar
        if (altarLocation != null) {
            altarLocation.getWorld().spawnParticle(Particle.FLAME, altarLocation, 50, 2, 2, 2, 0.1);
            altarLocation.getWorld().playSound(altarLocation, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    //                   SISTEMA DE RECOMPENSAS PS POR EVENTO
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Registra participación en muerte del guardián
     */
    public void trackGuardianKill(Player player) {
        participacionGuardian.put(player.getUniqueId(), true);
        plugin.getLogger().info(String.format("[EcoBrasas] %s participó en matar al guardián", player.getName()));
    }
    
    /**
     * Calcula PS ganados por un jugador según su participación
     * 
     * Sistema de puntos:
     * - Base de participación: 50 PS (solo por estar presente al finalizar)
     * - Por grieta cerrada: 15 PS cada una
     * - Por ancla completada: 25 PS cada una
     * - Por matar guardián: 40 PS (bonus especial)
     * 
     * Participación completa (10 grietas + 3 anclas + guardián) = 50 + 150 + 75 + 40 = 315 PS
     */
    private int calcularPSPorParticipacion(UUID uuid) {
        int ps = 50; // Base por participar
        
        // Grietas cerradas (15 PS cada una)
        int grietas = participacionGrietas.getOrDefault(uuid, 0);
        ps += grietas * 15;
        
        // Anclas completadas (25 PS cada una)
        int anclas = participacionAnclas.getOrDefault(uuid, 0);
        ps += anclas * 25;
        
        // Guardián derrotado (40 PS bonus)
        if (participacionGuardian.getOrDefault(uuid, false)) {
            ps += 40;
        }
        
        plugin.getLogger().info(String.format(
            "[EcoBrasas] PS para %s: Base=50 + Grietas=%d×15 + Anclas=%d×25 + Guardián=%s = %d PS total",
            uuid, grietas, anclas, participacionGuardian.getOrDefault(uuid, false) ? "40" : "0", ps
        ));
        
        return ps;
    }
}
