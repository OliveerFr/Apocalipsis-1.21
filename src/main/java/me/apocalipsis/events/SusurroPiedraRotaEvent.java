package me.apocalipsis.events;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;

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
    
    private Acto actoActual;
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
    private boolean oleadasCompletadas = false;
    
    // ═══════════════════════════════════════════════════════════════════
    // ACTO 3: NÚCLEO DE FORMA
    // ═══════════════════════════════════════════════════════════════════
    
    private Location nucleoLocation;
    private ItemFrame nucleoFrame;  // Item frame invisible que muestra el núcleo
    private BukkitTask nucleoParticleTask;
    private BukkitTask nucleoBeamTask;
    private boolean nucleoRecogido = false;
    private UUID jugadorQueRecogio = null;
    
    // ═══════════════════════════════════════════════════════════════════
    // TRACKING DE PARTICIPACIÓN
    // ═══════════════════════════════════════════════════════════════════
    
    private Map<UUID, Integer> participacionFragmentos = new HashMap<>();
    private Map<UUID, Integer> participacionCriaturas = new HashMap<>();
    private Set<UUID> participantesOriginales = new HashSet<>();
    
    // Sistema de guía con action bar
    private BukkitTask guiaActionBarTask;
    private Map<UUID, Location> objetivosPorJugador = new ConcurrentHashMap<>();
    
    // ═══════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════
    
    public SusurroPiedraRotaEvent(Apocalipsis plugin, MessageBus messageBus, SoundUtil soundUtil) {
        super(plugin, messageBus, soundUtil, "susurro_piedra_rota");
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
        
        // Iniciar sistema de guía
        iniciarGuiaActionBar();
        
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
        // Limpiar fragmentos
        limpiarFragmentos();
        
        // Limpiar grieta
        limpiarGrieta();
        
        // Limpiar núcleo
        limpiarNucleo();
        
        // Detener sistema de guía
        detenerGuiaActionBar();
        
        // Cancelar tasks
        if (fragmentosParticleTask != null) fragmentosParticleTask.cancel();
        if (grietaParticleTask != null) grietaParticleTask.cancel();
        if (grietaSoundTask != null) grietaSoundTask.cancel();
        if (nucleoParticleTask != null) nucleoParticleTask.cancel();
        if (nucleoBeamTask != null) nucleoBeamTask.cancel();
        
        // Matar criaturas activas
        for (Entity criatura : criaturasActivas) {
            if (criatura != null && criatura.isValid()) {
                criatura.remove();
            }
        }
        criaturasActivas.clear();
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
        broadcastNarrative("§8§m                                                    ");
        broadcastNarrative("");
        broadcastNarrative("§7Un susurro se repite entre la piedra...");
        broadcastNarrative("§7Algo está intentando formarse.");
        broadcastNarrative("");
        broadcastNarrative("§8§m                                                    ");
        
        playSoundToAll(Sound.BLOCK_STONE_BREAK, 1.0f, 0.5f);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ACTO 1: LA PIEDRA ROTA DESPIERTA
    // ═══════════════════════════════════════════════════════════════════
    
    private void iniciarActo1() {
        actoActual = Acto.PIEDRA_DESPIERTA;
        ticksEnActo = 0;
        
        plugin.getLogger().info("[SusurroPiedraRota] Iniciando Acto 1: La Piedra Rota Despierta");
        
        // Mensaje claro de objetivo
        broadcastNarrative("§8§m                                                    ");
        broadcastNarrative("");
        broadcastNarrative("§5§lACTO 1: LA PIEDRA ROTA DESPIERTA");
        broadcastNarrative("");
        broadcastNarrative("§7→ Busca fragmentos de piedra brillantes dispersos");
        broadcastNarrative("§7→ Acércate a cada fragmento para inspeccionarlo");
        broadcastNarrative("");
        broadcastNarrative("§8§m                                                    ");
        
        playSoundToAll(Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.2f);
        
        // Generar fragmentos de piedra
        generarFragmentosPiedra();
        
        // Asignar fragmento más cercano a cada jugador como objetivo
        for (Player p : Bukkit.getOnlinePlayers()) {
            Location fragmentoMasCercano = encontrarFragmentoMasCercano(p.getLocation());
            if (fragmentoMasCercano != null) {
                objetivosPorJugador.put(p.getUniqueId(), fragmentoMasCercano);
            }
        }
        
        // Iniciar efectos de partículas para fragmentos
        iniciarEfectosFragmentos();
    }
    
    private void generarFragmentosPiedra() {
        ConfigurationSection acto1Config = config.getConfigurationSection("acto_1.fragmentos_piedra");
        
        int cantidadMin = acto1Config.getInt("cantidad_min", 3);
        int cantidadMax = acto1Config.getInt("cantidad_max", 5);
        int distanciaMin = acto1Config.getInt("distancia_min_spawn", 50);
        int distanciaMax = acto1Config.getInt("distancia_max_spawn", 150);
        int distanciaEntreFragmentos = acto1Config.getInt("distancia_entre_fragmentos", 30);
        
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
        int intentos = 0;
        int maxIntentos = 50;
        
        while (intentos < maxIntentos) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = distMin + random.nextDouble() * (distMax - distMin);
            
            int x = spawn.getBlockX() + (int)(Math.cos(angle) * distance);
            int z = spawn.getBlockZ() + (int)(Math.sin(angle) * distance);
            int y = world.getHighestBlockYAt(x, z);
            
            Location loc = new Location(world, x, y, z);
            
            // Verificar que no esté muy cerca de otros fragmentos
            boolean lejosDeOtros = true;
            for (Location existente : fragmentosLocations) {
                if (existente.distance(loc) < distEntreFragmentos) {
                    lejosDeOtros = false;
                    break;
                }
            }
            
            if (lejosDeOtros) {
                return loc;
            }
            
            intentos++;
        }
        
        return null;
    }
    
    private void construirFragmentoPiedra(Location loc) {
        World world = loc.getWorld();
        
        // Mini altar 3x3
        // Base de deepslate
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location blockLoc = loc.clone().add(x, 0, z);
                world.getBlockAt(blockLoc).setType(Material.DEEPSLATE);
            }
        }
        
        // Piedra en el centro
        loc.getBlock().setType(Material.STONE);
        loc.clone().add(0, 1, 0).getBlock().setType(Material.STONE);
        
        // Vela apagada en el centro arriba
        Location candleLoc = loc.clone().add(0, 2, 0);
        candleLoc.getBlock().setType(Material.CANDLE);
    }
    
    private void iniciarEfectosFragmentos() {
        fragmentosParticleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (actoActual != Acto.PIEDRA_DESPIERTA) {
                return;
            }
            
            for (Location fragmento : fragmentosLocations) {
                // Partículas idle
                fragmento.getWorld().spawnParticle(
                    Particle.SMOKE,
                    fragmento.clone().add(0.5, 1, 0.5),
                    3,
                    0.3, 0.5, 0.3,
                    0.01
                );
            }
        }, 0L, 20L); // Cada segundo
    }
    
    private void verificarProximidadFragmentos() {
        if (ticksEnActo % 10 != 0) return; // Verificar cada 0.5s
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (Location fragmento : fragmentosLocations) {
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
        
        // Efectos visuales
        fragmento.getWorld().spawnParticle(
            Particle.PORTAL,
            fragmento.clone().add(0.5, 1, 0.5),
            30,
            0.5, 0.5, 0.5,
            0.5
        );
        
        // Sonido
        fragmento.getWorld().playSound(fragmento, Sound.BLOCK_STONE_HIT, 1.0f, 0.7f);
        
        // Mensaje fragmentado
        String[] mensajes = {
            "§8⧖ §7...hijo del eco...",
            "§8⧖ §7...fragmento incorrecto...",
            "§8⧖ §7...la forma busca...",
            "§8⧖ §7...memoria rota...",
            "§8⧖ §7...no debería existir..."
        };
        
        int index = fragmentosLocations.indexOf(fragmento);
        String mensaje = mensajes[index % mensajes.length];
        
        player.sendMessage("");
        player.sendMessage(mensaje);
        player.sendMessage("§a✓ Fragmento descubierto: " + (fragmentosInspeccionados.size() + 1) + "/" + fragmentosLocations.size());
        player.sendMessage("");
        
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
        // Verificar si todos los fragmentos fueron inspeccionados
        if (fragmentosInspeccionados.size() >= fragmentosLocations.size()) {
            // Completar Acto 1
            completarActo1();
        }
    }
    
    private void completarActo1() {
        plugin.getLogger().info("[SusurroPiedraRota] Acto 1 completado");
        
        // Transición al Acto 2
        actoActual = Acto.TRANSICION_2;
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                iniciarActo2();
            }
        }, 60L); // 3 segundos de transición
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ACTO 2: LA PIEDRA SE QUIEBRA
    // ═══════════════════════════════════════════════════════════════════
    
    private void iniciarActo2() {
        actoActual = Acto.PIEDRA_QUIEBRA;
        ticksEnActo = 0;
        
        plugin.getLogger().info("[SusurroPiedraRota] Iniciando Acto 2: La Piedra se Quiebra");
        
        // Mensaje de transición
        broadcastNarrative("§8§m                                                    ");
        broadcastNarrative("");
        broadcastNarrative("§5§lACTO 2: LA PIEDRA SE QUIEBRA");
        broadcastNarrative("");
        broadcastNarrative("§7→ Una grieta de forma se ha abierto");
        broadcastNarrative("§7→ Defiende la posición de 3 oleadas de criaturas");
        broadcastNarrative("");
        broadcastNarrative("§8§m                                                    ");
        
        playSoundToAll(Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.7f);
        
        // Generar grieta
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                generarGrietaForma();
            }
        }, 60L);
    }
    
    private void generarGrietaForma() {
        World world = Bukkit.getWorlds().get(0);
        Location spawn = world.getSpawnLocation();
        
        // Spawn aleatorio
        Random random = new Random();
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = 40 + random.nextDouble() * 60; // 40-100 bloques
        
        int x = spawn.getBlockX() + (int)(Math.cos(angle) * distance);
        int z = spawn.getBlockZ() + (int)(Math.sin(angle) * distance);
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
        World world = grietaLocation.getWorld();
        int radio = 3;
        int altura = 8;
        
        // Crear agujero vertical
        for (int h = 0; h < altura; h++) {
            for (int x = -radio; x <= radio; x++) {
                for (int z = -radio; z <= radio; z++) {
                    double distancia = Math.sqrt(x * x + z * z);
                    if (distancia <= radio) {
                        Location blockLoc = grietaLocation.clone().add(x, h, z);
                        blockLoc.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
    }
    
    private void iniciarEfectosGrieta() {
        // Partículas violetas
        grietaParticleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (actoActual != Acto.PIEDRA_QUIEBRA) {
                return;
            }
            
            Location center = grietaLocation.clone().add(0.5, 4, 0.5);
            
            // Partículas en espiral ascendente
            for (int i = 0; i < 30; i++) {
                double t = i * 0.2;
                double radius = 2.5;
                double x = radius * Math.cos(t);
                double z = radius * Math.sin(t);
                double y = t * 0.3;
                
                Location particleLoc = center.clone().add(x, y, z);
                grietaLocation.getWorld().spawnParticle(
                    Particle.REVERSE_PORTAL,
                    particleLoc,
                    1,
                    0, 0, 0,
                    0
                );
            }
        }, 0L, 2L); // Cada 0.1 segundos
        
        // Sonido ambiental
        grietaSoundTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (actoActual != Acto.PIEDRA_QUIEBRA) {
                return;
            }
            
            grietaLocation.getWorld().playSound(
                grietaLocation,
                Sound.BLOCK_PORTAL_AMBIENT,
                0.3f,
                0.8f
            );
        }, 0L, 100L); // Cada 5 segundos
    }
    
    private void programarOleadas() {
        oleadaActual = 0;
        oleadasTotales = 3;
        
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
        
        int cantidadCriaturas = 3 + new Random().nextInt(3); // 3-5 criaturas
        
        plugin.getLogger().info(String.format(
            "[SusurroPiedraRota] Spawneando oleada %d/%d (%d criaturas)",
            oleadaActual,
            oleadasTotales,
            cantidadCriaturas
        ));
        
        broadcastNarrative(String.format("§5⚠ Oleada %d/%d - Elimina las criaturas de Forma", oleadaActual, oleadasTotales));
        playSoundToAll(Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        
        // Actualizar objetivo para todos los jugadores
        for (Player p : Bukkit.getOnlinePlayers()) {
            objetivosPorJugador.put(p.getUniqueId(), grietaLocation);
        }
        
        // Spawn sincrónico para asegurar que se creen
        for (int i = 0; i < cantidadCriaturas; i++) {
            final int delay = i * 10; // 0.5s entre cada spawn
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (isActive() && actoActual == Acto.PIEDRA_QUIEBRA) {
                    spawnearCriaturaForma();
                }
            }, delay);
        }
    }
    
    private void spawnearCriaturaForma() {
        if (grietaLocation == null) return;
        
        // Buscar ubicación válida alrededor de la grieta
        Location spawnLoc = encontrarSpawnSeguro(grietaLocation, 3, 5);
        if (spawnLoc == null) {
            spawnLoc = grietaLocation.clone().add(0, 1, 0); // Fallback
        }
        
        Silverfish criatura = (Silverfish) grietaLocation.getWorld().spawnEntity(
            spawnLoc,
            EntityType.SILVERFISH
        );
        
        // Usar método moderno para custom name
        criatura.customName(net.kyori.adventure.text.Component.text("§5Criatura de Forma"));
        criatura.setCustomNameVisible(true);
        criatura.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(10.0); // 5 corazones
        criatura.setHealth(10.0);
        criatura.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).setBaseValue(0.3); // Rápido
        criatura.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.GLOWING,
            999999,
            0,
            false,
            false
        ));
        
        criaturasActivas.add(criatura);
        
        // Efectos de spawn
        spawnLoc.getWorld().spawnParticle(
            Particle.REVERSE_PORTAL,
            spawnLoc,
            30,
            0.5, 0.5, 0.5,
            0.1
        );
        spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.7f);
    }
    
    private void tickActo2() {
        // Verificar si todas las criaturas están muertas
        criaturasActivas.removeIf(e -> !e.isValid() || e.isDead());
        
        // Si todas las oleadas spawnearon y todas las criaturas murieron
        if (oleadaActual >= oleadasTotales && criaturasActivas.isEmpty() && !oleadasCompletadas) {
            oleadasCompletadas = true;
            completarActo2();
        }
    }
    
    private void completarActo2() {
        plugin.getLogger().info("[SusurroPiedraRota] Acto 2 completado");
        
        // Limpiar efectos de grieta
        if (grietaParticleTask != null) grietaParticleTask.cancel();
        if (grietaSoundTask != null) grietaSoundTask.cancel();
        
        // Transición al Acto 3
        actoActual = Acto.TRANSICION_3;
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                iniciarActo3();
            }
        }, 60L);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ACTO 3: EL NÚCLEO DE FORMA
    // ═══════════════════════════════════════════════════════════════════
    
    private void iniciarActo3() {
        actoActual = Acto.NUCLEO_FORMA;
        ticksEnActo = 0;
        
        plugin.getLogger().info("[SusurroPiedraRota] Iniciando Acto 3: El Núcleo de Forma");
        
        // Mensaje global con instrucciones claras
        broadcastNarrative("§8§m                                                    ");
        broadcastNarrative("");
        broadcastNarrative("§5§lACTO 3: EL NÚCLEO DE FORMA");
        broadcastNarrative("");
        broadcastNarrative("§7→ La forma se deformó y dejó un núcleo");
        broadcastNarrative("§7→ Acércate y recógelo para completar el evento");
        broadcastNarrative("");
        broadcastNarrative("§8§m                                                    ");
        
        playSoundToAll(Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.8f);
        
        // Spawn del núcleo
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive()) {
                spawnearNucleoForma();
            }
        }, 40L);
    }
    
    private void spawnearNucleoForma() {
        nucleoLocation = grietaLocation.clone().add(0.5, 1.5, 0.5);
        
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
    }
    
    private void iniciarEfectosNucleo() {
        // Partículas alrededor del núcleo
        nucleoParticleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (actoActual != Acto.NUCLEO_FORMA || nucleoRecogido) {
                return;
            }
            
            // Partículas orbitando
            double t = System.currentTimeMillis() / 500.0;
            for (int i = 0; i < 8; i++) {
                double angle = (t + i * Math.PI / 4) % (Math.PI * 2);
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                
                Location particleLoc = nucleoLocation.clone().add(x, 0, z);
                nucleoLocation.getWorld().spawnParticle(
                    Particle.END_ROD,
                    particleLoc,
                    1,
                    0, 0, 0,
                    0
                );
            }
        }, 0L, 2L);
        
        // Beam de luz
        nucleoBeamTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (actoActual != Acto.NUCLEO_FORMA || nucleoRecogido) {
                return;
            }
            
            for (int y = 0; y < 50; y++) {
                Location beamLoc = nucleoLocation.clone().add(0, y, 0);
                nucleoLocation.getWorld().spawnParticle(
                    Particle.END_ROD,
                    beamLoc,
                    1,
                    0.1, 0, 0.1,
                    0
                );
            }
        }, 0L, 10L);
    }
    
    private void verificarProximidadNucleo() {
        if (ticksEnActo % 5 != 0) return; // Verificar cada 0.25s
        if (nucleoLocation == null) return; // Validación null safety
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getLocation().distance(nucleoLocation) < 2.0) {
                recogerNucleo(player);
                break;
            }
        }
    }
    
    private void recogerNucleo(Player player) {
        if (nucleoRecogido) return;
        
        nucleoRecogido = true;
        jugadorQueRecogio = player.getUniqueId();
        
        plugin.getLogger().info(String.format(
            "[SusurroPiedraRota] %s recogió el Núcleo de Forma",
            player.getName()
        ));
        
        // Dar item al jugador
        player.getInventory().addItem(SusurroPiedraRotaItems.createNucleoForma());
        
        // Mensaje
        player.sendMessage("");
        player.sendMessage("§5✦ Has recogido el §lFragmento de Forma Desviada");
        player.sendMessage("");
        
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
    
    private void tickActo3() {
        // El acto se completa cuando el núcleo es recogido
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
            player.sendMessage("");
            player.sendMessage("§7§oLa piedra no debería hablar...");
            player.sendMessage("§7§osi lo hace, es porque algo antiguo vuelve a tomar forma.");
            player.sendMessage("");
        }
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
        
        // Detener evento
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            stop();
        }, 100L);
    }
    
    private void entregarRecompensas() {
        ConfigurationSection recompensasConfig = config.getConfigurationSection("recompensas_ps");
        
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
            
            // Dar PS
            if (plugin.getExperienceService() != null) {
                plugin.getExperienceService().addXP(player, psTotal, "Evento: El Susurro en la Piedra Rota");
            }
            
            // Mensaje
            player.sendMessage("");
            player.sendMessage("§6§l⚡ RECOMPENSAS DEL EVENTO");
            player.sendMessage("§7Fragmentos inspeccionados: §e" + fragmentos);
            if (uuid.equals(jugadorQueRecogio)) {
                player.sendMessage("§7Núcleo recogido: §a✓");
            }
            player.sendMessage("§7PS ganados: §6+" + psTotal);
            player.sendMessage("");
            
            plugin.getLogger().info(String.format(
                "[SusurroPiedraRota] %s recibió %d PS (fragmentos: %d)",
                player.getName(),
                psTotal,
                fragmentos
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
                    
                    // Si llegó al objetivo (menos de 3 bloques), quitar guía
                    if (distancia < 3.0) {
                        objetivosPorJugador.remove(player.getUniqueId());
                        continue;
                    }
                    
                    // Calcular dirección
                    String direccion = calcularDireccion(player.getLocation(), objetivo);
                    
                    // Mostrar en action bar usando API moderna
                    player.sendActionBar(net.kyori.adventure.text.Component.text(
                        String.format(
                            "§5⦿ Objetivo: §f%s §7(%d bloques)",
                            direccion,
                            (int)distancia
                        )
                    ));
                }
            }
        }, 0L, 20L); // Cada segundo
    }
    
    private String calcularDireccion(Location desde, Location hacia) {
        Vector direccion = hacia.toVector().subtract(desde.toVector()).normalize();
        
        double x = direccion.getX();
        double z = direccion.getZ();
        
        // Determinar dirección cardinal principal
        if (Math.abs(x) > Math.abs(z)) {
            return x > 0 ? "→ Este" : "← Oeste";
        } else {
            return z > 0 ? "↓ Sur" : "↑ Norte";
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
        Location superficie = loc.getWorld().getHighestBlockAt(loc).getLocation().add(0, 1, 0);
        
        // 3. Verificar que no sea agua
        if (superficie.getBlock().getType() == Material.WATER) {
            return false;
        }
        
        // 4. Verificar espacio libre (2 bloques de altura)
        if (!superficie.clone().add(0, 1, 0).getBlock().getType().isAir()) {
            return false;
        }
        
        // 5. Verificar que el bloque debajo sea sólido
        if (!superficie.clone().add(0, -1, 0).getBlock().getType().isSolid()) {
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
    // GETTERS PARA ESTADO
    // ═══════════════════════════════════════════════════════════════════
    
    public Acto getActoActual() {
        return actoActual;
    }
    
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
}
