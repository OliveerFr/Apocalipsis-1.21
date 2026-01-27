package me.apocalipsis.events;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.ciclos.CicloManager;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public class Evento6MundoOlvidado {
    
    private final Apocalipsis plugin;
    private final CicloManager cicloManager;
    
    private FileConfiguration config;
    private File configFile;
    
    // Estado del evento
    private boolean eventoActivo = false;
    private MundoOlvidadoFase faseActual = MundoOlvidadoFase.INACTIVO;
    private long tiempoInicio = 0;
    private Set<UUID> participantes = new HashSet<>();
    private boolean modoSkipActivo = false; // Flag para deshabilitar progresión automática cuando se usa /next
    
    // Tareas programadas
    private BukkitRunnable tareaProgresion;
    private Map<MundoOlvidadoFase, BukkitRunnable> tareasActos = new HashMap<>();
    
    public Evento6MundoOlvidado(Apocalipsis plugin) {
        this.plugin = plugin;
        this.cicloManager = plugin.getCicloManager();
        cargarConfiguracion();
    }
    
    private void cargarConfiguracion() {
        configFile = new File(plugin.getDataFolder(), "evento6_mundo_olvidado.yml");
        if (!configFile.exists()) {
            plugin.saveResource("evento6_mundo_olvidado.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    /**
     * Iniciar el evento
     * @param iniciador Jugador que inició el evento
     * @return true si se inició correctamente
     */
    public boolean iniciarEvento(Player iniciador) {
        // Verificaciones previas
        if (eventoActivo) {
            iniciador.sendMessage(config.getString("mensajes.error_ya_activo", "§c✗ El Evento 6 ya está activo."));
            return false;
        }
        
        // Verificar que el CicloManager esté disponible
        if (cicloManager == null) {
            iniciador.sendMessage("§c✗ ERROR: Sistema de ciclos no inicializado.");
            iniciador.sendMessage("§c   Contacta a un administrador.");
            plugin.getLogger().severe("[Evento 6] CicloManager es NULL - Error crítico de inicialización");
            return false;
        }
        
        // Verificar que el sistema de ciclos esté habilitado en ciclos.yml
        if (!cicloManager.isEnabled()) {
            iniciador.sendMessage("§c✗ El sistema de ciclos está desactivado en la configuración.");
            iniciador.sendMessage("§e   Para activarlo:");
            iniciador.sendMessage("§e   1. Edita plugins/Apocalipsis/ciclos.yml");
            iniciador.sendMessage("§e   2. Cambia 'enabled: false' a 'enabled: true'");
            iniciador.sendMessage("§e   3. Ejecuta /avo reload");
            plugin.getLogger().warning("[Evento 6] Sistema de ciclos deshabilitado en ciclos.yml (config.enabled = false)");
            return false;
        }
        
        // Iniciar evento
        eventoActivo = true;
        tiempoInicio = System.currentTimeMillis();
        faseActual = MundoOlvidadoFase.ACTO_1_NORMALIDAD;
        
        // Registrar participantes (todos online)
        for (Player player : Bukkit.getOnlinePlayers()) {
            participantes.add(player.getUniqueId());
        }
        
        // Broadcast inicio
        Bukkit.broadcastMessage(config.getString("mensajes.evento_iniciado", 
            "§8[§7EVENTO 6§8] §7Cuando el Mundo Decide Olvidar §8- §7Iniciado"));
        
        // Iniciar progresión automática
        iniciarProgresionAutomatica();
        
        // Ejecutar primer acto
        cambiarAActo(MundoOlvidadoFase.ACTO_1_NORMALIDAD);
        
        // Log
        plugin.getLogger().info("[Evento 6] Iniciado por " + iniciador.getName());
        plugin.getLogger().info("[Evento 6] Participantes: " + participantes.size());
        
        return true;
    }
    
    /**
     * Sistema de progresión automática por tiempo
     */
    private void iniciarProgresionAutomatica() {
        tareaProgresion = new BukkitRunnable() {
            @Override
            public void run() {
                if (!eventoActivo) {
                    cancel();
                    return;
                }
                
                // Si está en modo skip, no cambiar actos automáticamente
                if (modoSkipActivo) {
                    return;
                }
                
                long tiempoTranscurrido = (System.currentTimeMillis() - tiempoInicio) / 1000; // segundos
                
                // Verificar cambio de acto según tiempo
                MundoOlvidadoFase siguienteActo = obtenerActoPorTiempo(tiempoTranscurrido);
                
                if (siguienteActo != faseActual) {
                    cambiarAActo(siguienteActo);
                }
            }
        };
        
        // Ejecutar cada 10 segundos
        tareaProgresion.runTaskTimer(plugin, 0L, 200L);
    }
    
    /**
     * Determina qué acto debe estar activo según el tiempo transcurrido
     */
    private MundoOlvidadoFase obtenerActoPorTiempo(long segundos) {
        if (segundos < 300) return MundoOlvidadoFase.ACTO_1_NORMALIDAD;
        else if (segundos < 600) return MundoOlvidadoFase.ACTO_2_RAREZAS;
        else if (segundos < 750) return MundoOlvidadoFase.ACTO_3_INESTABILIDAD;
        else if (segundos < 870) return MundoOlvidadoFase.ACTO_4_QUIEBRE;
        else if (segundos < 900) return MundoOlvidadoFase.ACTO_5_REINICIO;
        else if (segundos < 1200) return MundoOlvidadoFase.ACTO_6_NUEVO_MUNDO;
        else if (segundos < 1500) return MundoOlvidadoFase.ACTO_7_COMPRENSION;
        else if (segundos < 1800) return MundoOlvidadoFase.ACTO_8_FRACTURA;
        else if (segundos < 2100) return MundoOlvidadoFase.ACTO_9_END_PERMANECE;
        else if (segundos < 2400) return MundoOlvidadoFase.ACTO_10_CIERRE;
        else return MundoOlvidadoFase.COMPLETADO;
    }
    
    /**
     * Cambiar al acto especificado
     */
    private void cambiarAActo(MundoOlvidadoFase nuevoActo) {
        // Cancelar tareas del acto anterior
        if (tareasActos.containsKey(faseActual)) {
            BukkitRunnable tarea = tareasActos.get(faseActual);
            if (tarea != null) {
                tarea.cancel();
            }
        }
        
        // Actualizar fase
        MundoOlvidadoFase actoAnterior = faseActual;
        faseActual = nuevoActo;
        
        // Log
        plugin.getLogger().info("[Evento 6] Cambio: " + actoAnterior + " → " + nuevoActo);
        
        // Ejecutar lógica del nuevo acto
        switch (nuevoActo) {
            case ACTO_1_NORMALIDAD:
                ejecutarActo1();
                break;
            case ACTO_2_RAREZAS:
                ejecutarActo2();
                break;
            case ACTO_3_INESTABILIDAD:
                ejecutarActo3();
                break;
            case ACTO_4_QUIEBRE:
                ejecutarActo4();
                break;
            case ACTO_5_REINICIO:
                ejecutarActo5();
                break;
            case ACTO_6_NUEVO_MUNDO:
                ejecutarActo6();
                break;
            case ACTO_7_COMPRENSION:
                ejecutarActo7();
                break;
            case ACTO_8_FRACTURA:
                ejecutarActo8();
                break;
            case ACTO_9_END_PERMANECE:
                ejecutarActo9();
                break;
            case ACTO_10_CIERRE:
                ejecutarActo10();
                break;
            case COMPLETADO:
                finalizarEvento();
                break;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // IMPLEMENTACIÓN DE ACTOS
    // ═══════════════════════════════════════════════════════════════
    
    private void ejecutarActo1() {
        // ACTO 1: NORMALIDAD - Silencio casi total
        
        BukkitRunnable tarea = new BukkitRunnable() {
            int contador = 0;
            
            @Override
            public void run() {
                contador++;
                
                // A los 5 minutos (300 seg / 10 = 30 iteraciones)
                if (contador == 30) {
                    enviarMensajeObservador("§8[§7...§8]", false);
                }
            }
        };
        
        tarea.runTaskTimer(plugin, 0L, 200L); // Cada 10 segundos
        tareasActos.put(MundoOlvidadoFase.ACTO_1_NORMALIDAD, tarea);
    }
    
    private void ejecutarActo2() {
        // ACTO 2: PRIMERAS RAREZAS - Efectos sutiles
        
        BukkitRunnable tarea = new BukkitRunnable() {
            int contador = 0;
            
            @Override
            public void run() {
                contador++;
                
                // Mensaje a los 3 minutos
                if (contador == 18) {
                    enviarMensajeObservador("§8[§7...§8] §7Hmm...", false);
                }
                
                // Mensaje a los 9 minutos
                if (contador == 54) {
                    enviarMensajeObservador("§8[§7...§8] §7No todavía...", false);
                }
                
                // Efectos aleatorios cada 2-5 minutos
                if (contador % 12 == 0 && Math.random() < 0.4) {
                    ejecutarEfectoAleatorioRarezas();
                }
            }
        };
        
        tarea.runTaskTimer(plugin, 0L, 200L);
        tareasActos.put(MundoOlvidadoFase.ACTO_2_RAREZAS, tarea);
    }
    
    private void ejecutarActo3() {
        // ACTO 3: INESTABILIDAD - Efectos constantes
        
        // Mensaje inicial
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            enviarMensajeObservador("§8[§7...§8] §7Otra vez no...", true);
        }, 6000L); // 5 minutos
        
        BukkitRunnable tarea = new BukkitRunnable() {
            @Override
            public void run() {
                // Partículas de ceniza
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (participantes.contains(player.getUniqueId())) {
                        player.getWorld().spawnParticle(
                            Particle.ASH,
                            player.getLocation().add(
                                Math.random() * 10 - 5,
                                Math.random() * 5,
                                Math.random() * 10 - 5
                            ),
                            5
                        );
                    }
                }
                
                // Efectos aleatorios adicionales
                if (Math.random() < 0.1) {
                    ejecutarEfectoInestabilidad();
                }
            }
        };
        
        tarea.runTaskTimer(plugin, 0L, 100L); // Cada 5 segundos
        tareasActos.put(MundoOlvidadoFase.ACTO_3_INESTABILIDAD, tarea);
    }
    
    private void ejecutarActo4() {
        // ACTO 4: EL QUIEBRE - Secuencia automática dramática
        
        // Paso 1: Ceguera (0 segundos)
        aplicarEfectoTodos(PotionEffectType.BLINDNESS, 100);
        reproducirSonidoTodos(Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.0f);
        
        // Paso 2: Sonido profundo (5 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            reproducirSonidoTodos(Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.3f);
        }, 100L);
        
        // Paso 3: Vibración - Partículas (6 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (participantes.contains(player.getUniqueId())) {
                    player.getWorld().spawnParticle(
                        Particle.ELECTRIC_SPARK,
                        player.getLocation(),
                        1000,
                        50, 50, 50,
                        0.5
                    );
                }
            }
        }, 120L);
        
        // Paso 4: Congelación (7 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            aplicarEfectoTodos(PotionEffectType.SLOWNESS, 60);
            aplicarEfectoTodos(PotionEffectType.MINING_FATIGUE, 60);
        }, 140L);
        
        // Paso 5: Mensaje final (10 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (participantes.contains(player.getUniqueId())) {
                    player.sendTitle(
                        "§8§l...",
                        "§cEl mundo ya tomó la decisión.",
                        20, 100, 30
                    );
                }
            }
            reproducirSonidoTodos(Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 0.5f);
        }, 200L);
    }
    
    private void ejecutarActo5() {
        // ACTO 5: EL REINICIO - Usa el sistema de ciclos
        
        // Paso 1: Pantalla negra (2 segundos)
        aplicarEfectoTodos(PotionEffectType.BLINDNESS, 40);
        
        // Paso 2: Crear nuevo ciclo usando CicloManager (2 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String nombreCicloNuevo = config.getString("evento.mundo_nuevo_nombre", "Ciclo_2_Los_Que_Recuerdan");
            
            try {
                // Verificar si el mundo ya existe
                World mundoExistente = Bukkit.getWorld(nombreCicloNuevo);
                if (mundoExistente != null) {
                    plugin.getLogger().warning("[Evento 6] El mundo '" + nombreCicloNuevo + "' ya existe, usando mundo existente");
                    
                    // Activar ciclo en mundo existente (sin teleportar aún)
                    boolean activado = cicloManager.activateCycle(nombreCicloNuevo, false);
                    if (!activado) {
                        plugin.getLogger().severe("[Evento 6] ERROR: No se pudo activar el ciclo!");
                    }
                } else {
                    // Crear y activar nuevo ciclo (sin teleportar aún, lo haremos manualmente)
                    boolean creado = cicloManager.createAndActivateCycle(
                        nombreCicloNuevo,
                        World.Environment.NORMAL,
                        Difficulty.HARD,
                        false  // NO teleportar automáticamente
                    );
                    
                    if (!creado) {
                        plugin.getLogger().severe("[Evento 6] ERROR: No se pudo crear el nuevo ciclo!");
                        return;
                    }
                    
                    plugin.getLogger().info("[Evento 6] ✓ Nuevo ciclo creado: " + nombreCicloNuevo);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("[Evento 6] ERROR al crear/activar ciclo: " + e.getMessage());
                e.printStackTrace();
            }
            
        }, 40L);
        
        // Paso 3: Teleportar todos los participantes a la SUPERFICIE (2.5 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String nombreCicloNuevo = config.getString("evento.mundo_nuevo_nombre", "Ciclo_2_Los_Que_Recuerdan");
            
            try {
                World nuevoMundo = Bukkit.getWorld(nombreCicloNuevo);
                if (nuevoMundo == null) {
                    plugin.getLogger().severe("[Evento 6] ERROR: Mundo nuevo no encontrado!");
                    return;
                }
                
                // Obtener spawn y asegurar que esté en SUPERFICIE
                Location spawn = nuevoMundo.getSpawnLocation();
                
                // Buscar superficie segura desde el spawn
                Location superficieSegura = encontrarSuperficieSegura(nuevoMundo, spawn);
                if (superficieSegura == null) {
                    plugin.getLogger().warning("[Evento 6] No se encontró superficie segura, usando spawn default");
                    superficieSegura = spawn;
                }
                
                // Asegurar que esté en superficie (Y >= 60)
                if (superficieSegura.getY() < 60) {
                    superficieSegura.setY(nuevoMundo.getHighestBlockYAt(superficieSegura.getBlockX(), superficieSegura.getBlockZ()) + 1);
                }
                
                plugin.getLogger().info("[Evento 6] Spawn en superficie: " + superficieSegura.getBlockX() + ", " + superficieSegura.getBlockY() + ", " + superficieSegura.getBlockZ());
                
                // Teleportar cada participante
                for (UUID uuid : participantes) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        // Guardar datos del mundo actual ANTES de teleportar
                        String mundoActual = player.getWorld().getName();
                        cicloManager.handlePlayerLeaveWorld(player, mundoActual);
                        
                        // Teleportar al nuevo ciclo en SUPERFICIE
                        player.teleport(superficieSegura);
                        
                        // El WorldChangeListener se encargará de cargar los datos del nuevo ciclo
                        plugin.getLogger().info("[Evento 6] Teleportado: " + player.getName() + " al ciclo " + nombreCicloNuevo);
                    }
                }
                
                plugin.getLogger().info("[Evento 6] ✓ Todos los jugadores teleportados al nuevo ciclo en superficie");
            } catch (Exception e) {
                plugin.getLogger().severe("[Evento 6] ERROR al teleportar: " + e.getMessage());
                e.printStackTrace();
            }
            
        }, 50L);
        
        // Paso 4: Efectos de spawn (3 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID uuid : participantes) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.getWorld().spawnParticle(
                        Particle.END_ROD,
                        player.getLocation(),
                        200,
                        2, 2, 2,
                        0.1
                    );
                    player.playSound(
                        player.getLocation(),
                        Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,
                        1.0f, 1.0f
                    );
                }
            }
            
            // Dar items iniciales según configuración
            darItemsIniciales();
            
        }, 60L);
    }
    
    private void ejecutarActo6() {
        // ACTO 6: NUEVO MUNDO - Mensajes de revelación
        
        // Mensaje 1: "No los borró..." (10 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (participantes.contains(player.getUniqueId())) {
                    player.sendTitle(
                        "§8§o...",
                        "§7§oNo los borró...",
                        20, 60, 20
                    );
                }
            }
        }, 200L);
        
        // Mensaje 2: "Solo borró el lugar." (14 segundos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            enviarMensajeObservador("§8[§7...§8] §7Solo borró el lugar.", false);
        }, 280L);
        
        // Mensaje 3: Pausa (5 minutos)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            enviarMensajeObservador("§8[§7...§8]", false);
        }, 6000L);
    }
    
    private void ejecutarActo7() {
        // ACTO 7: COMPRENSIÓN - Mensajes espaciados
        
        // Mensaje 1 (3 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (participantes.contains(player.getUniqueId())) {
                    player.sendTitle(
                        "§8§o...",
                        "§7§oEl mundo hace esto cuando se cansa.",
                        20, 60, 20
                    );
                }
            }
        }, 3600L);
        
        // Mensaje 2 (9 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            enviarMensajeObservador("§8[§7...§8] §7Reiniciar es más fácil que cambiar.", false);
        }, 10800L);
        
        // Mensaje 3 (13 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            enviarMensajeObservador("§8[§7...§8]", false);
        }, 15600L);
    }
    
    private void ejecutarActo8() {
        // ACTO 8: LA FRACTURA - Nether no se reseteó
        
        // Mensaje (2 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (participantes.contains(player.getUniqueId())) {
                    player.sendTitle(
                        "§8§o...",
                        "§7§oLo que está debajo...",
                        20, 60, 20
                    );
                }
            }
        }, 2400L);
        
        // Mensaje 2 (3 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            enviarMensajeObservador("§8[§7...§8] §7no olvida tan fácil.", false);
        }, 3600L);
    }
    
    private void ejecutarActo9() {
        // ACTO 9: EL END PERMANECE
        
        // Mensaje (3 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (participantes.contains(player.getUniqueId())) {
                    player.sendTitle(
                        "§8§o...",
                        "§7§oAlgunos lugares no se reinician.",
                        20, 60, 20
                    );
                }
            }
        }, 3600L);
        
        // Mensaje 2 (4 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            enviarMensajeObservador("§8[§7...§8] §7Solo observan.", false);
        }, 4800L);
    }
    
    private void ejecutarActo10() {
        // ACTO 10: CIERRE - Mensaje final y recompensas
        
        // Mensaje final 1 (10 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (participantes.contains(player.getUniqueId())) {
                    player.sendTitle(
                        "§8§l...",
                        "§7§oEste no es un comienzo.",
                        20, 60, 20
                    );
                }
            }
        }, 12000L);
        
        // Mensaje final 2 (11 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            enviarMensajeObservador("§8§l[§7§l...§8§l] §7Es una repetición.", false);
        }, 13200L);
        
        // Mensaje 3 (12 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            enviarMensajeObservador("§8[§7...§8]", false);
        }, 14400L);
        
        // Dar recompensas finales (14.5 min)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            darRecompensasFinales();
        }, 17400L);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MÉTODOS AUXILIARES
    // ═══════════════════════════════════════════════════════════════
    
    private void enviarMensajeObservador(String mensaje, boolean esTitle) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (participantes.contains(player.getUniqueId())) {
                if (esTitle) {
                    player.sendTitle("§8§o...", mensaje, 20, 60, 20);
                } else {
                    player.sendMessage(mensaje);
                }
            }
        }
    }
    
    private void aplicarEfectoTodos(PotionEffectType efecto, int duracion) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (participantes.contains(player.getUniqueId())) {
                player.addPotionEffect(
                    new PotionEffect(efecto, duracion, 5, false, false)
                );
            }
        }
    }
    
    private void reproducirSonidoTodos(Sound sonido, float volumen, float pitch) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (participantes.contains(player.getUniqueId())) {
                player.playSound(player.getLocation(), sonido, volumen, pitch);
            }
        }
    }
    
    private void ejecutarEfectoAleatorioRarezas() {
        double random = Math.random();
        
        if (random < 0.25) {
            // Trueno sin lluvia
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (participantes.contains(player.getUniqueId())) {
                    player.getWorld().strikeLightningEffect(player.getLocation().add(
                        (Math.random() - 0.5) * 50,
                        0,
                        (Math.random() - 0.5) * 50
                    ));
                }
            }
        } else if (random < 0.5) {
            // Sonido lejano
            Sound[] sonidos = {Sound.ENTITY_WARDEN_HEARTBEAT, Sound.BLOCK_PORTAL_AMBIENT, Sound.ENTITY_ENDER_DRAGON_AMBIENT};
            Sound sonido = sonidos[(int)(Math.random() * sonidos.length)];
            reproducirSonidoTodos(sonido, 0.3f, 0.5f);
        } else if (random < 0.75) {
            // Mob estático (freeze temporal)
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (participantes.contains(player.getUniqueId())) {
                    player.getWorld().getNearbyEntities(player.getLocation(), 30, 30, 30).stream()
                        .filter(e -> e instanceof LivingEntity && !(e instanceof Player))
                        .findFirst()
                        .ifPresent(e -> {
                            LivingEntity mob = (LivingEntity) e;
                            mob.setAI(false);
                            Bukkit.getScheduler().runTaskLater(plugin, () -> mob.setAI(true), 100L);
                        });
                }
            }
        }
    }
    
    private void ejecutarEfectoInestabilidad() {
        double random = Math.random();
        
        if (random < 0.33) {
            // Lag simulado
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (participantes.contains(player.getUniqueId())) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 10, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 20, 10, false, false));
                }
            }
        } else if (random < 0.66) {
            // Portal sonidos
            reproducirSonidoTodos(Sound.BLOCK_PORTAL_TRIGGER, 0.8f, 1.0f);
        }
    }
    
    private void darItemsIniciales() {
        for (UUID uuid : participantes) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // 16x Madera
                ItemStack madera = new ItemStack(Material.OAK_LOG, 16);
                ItemMeta maderaMeta = madera.getItemMeta();
                if (maderaMeta != null) {
                    maderaMeta.setDisplayName("§7Resto del Anterior");
                    maderaMeta.setLore(Arrays.asList(
                        "§8Evento 6: Mundo Olvidado",
                        "",
                        "§7Todo lo demás se fue.",
                        "§7Esto quedó."
                    ));
                    madera.setItemMeta(maderaMeta);
                }
                
                // 8x Pan
                ItemStack pan = new ItemStack(Material.BREAD, 8);
                ItemMeta panMeta = pan.getItemMeta();
                if (panMeta != null) {
                    panMeta.setDisplayName("§ePan Persistente");
                    panMeta.setLore(Arrays.asList(
                        "§8Evento 6: Mundo Olvidado",
                        "",
                        "§7Alimento básico.",
                        "§7Lo suficiente para empezar."
                    ));
                    pan.setItemMeta(panMeta);
                }
                
                player.getInventory().addItem(madera, pan);
                player.sendMessage("§7Has recibido items iniciales.");
            }
        }
    }
    
    private void darRecompensasFinales() {
        int psBase = config.getInt("recompensas_ps.base_presencia", 100);
        int psActo = config.getInt("recompensas_ps.por_acto_completo", 20);
        int psBonus = config.getInt("recompensas_ps.bonus_comprension", 50);
        int psTotal = psBase + (psActo * 10) + psBonus;
        
        for (UUID uuid : participantes) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Dar PS
                if (plugin.getMissionService() != null) {
                    plugin.getMissionService().addPS(uuid, psTotal, "Evento 6: Mundo Olvidado");
                    player.sendMessage("§a+ " + psTotal + " PS §7(Participación en Evento 6)");
                }
                
                // Fragmento de Memoria
                ItemStack fragmento = new ItemStack(Material.ECHO_SHARD);
                ItemMeta fragmentoMeta = fragmento.getItemMeta();
                if (fragmentoMeta != null) {
                    fragmentoMeta.setDisplayName("§d§lFragmento de Memoria");
                    fragmentoMeta.setLore(Arrays.asList(
                        "§8Evento 6: Mundo Olvidado",
                        "",
                        "§7El mundo olvidó el lugar.",
                        "§7Pero ustedes recuerdan todo.",
                        "",
                        "§8§o\"Reiniciar es más fácil que cambiar.\""
                    ));
                    fragmento.setItemMeta(fragmentoMeta);
                }
                
                // Cicatriz Temporal
                ItemStack cicatriz = new ItemStack(Material.NETHERITE_SCRAP);
                ItemMeta cicatrizMeta = cicatriz.getItemMeta();
                if (cicatrizMeta != null) {
                    cicatrizMeta.setDisplayName("§c§lCicatriz Temporal");
                    cicatrizMeta.setLore(Arrays.asList(
                        "§8Evento 6: Mundo Olvidado",
                        "",
                        "§7Lo que está debajo no olvida.",
                        "§7El Nether permanece intacto.",
                        "",
                        "§4§o\"Algunos lugares no se reinician.\""
                    ));
                    cicatriz.setItemMeta(cicatrizMeta);
                }
                
                // Eco de la Repetición
                ItemStack eco = new ItemStack(Material.RECOVERY_COMPASS);
                ItemMeta ecoMeta = eco.getItemMeta();
                if (ecoMeta != null) {
                    ecoMeta.setDisplayName("§8§l✦ Eco de la Repetición ✦");
                    ecoMeta.setLore(Arrays.asList(
                        "§8Evento 6: Mundo Olvidado",
                        "",
                        "§7El mundo decidió olvidar.",
                        "§7Borró el lugar, no las personas.",
                        "",
                        "§7Este no es un comienzo.",
                        "§8§lEs una repetición.",
                        "",
                        "§d✦ Emblema de Supervivencia ✦"
                    ));
                    eco.setItemMeta(ecoMeta);
                }
                
                player.getInventory().addItem(fragmento, cicatriz, eco);
                player.sendMessage("§7Recibiste: §d§lFragmento de Memoria");
                player.sendMessage("§7Recibiste: §c§lCicatriz Temporal");
                player.sendMessage("§7Recibiste: §8§l✦ Eco de la Repetición ✦");
            }
        }
        
        Bukkit.broadcastMessage("§8[§7EVENTO 6§8] §a✓ Recompensas entregadas");
    }
    
    private void finalizarEvento() {
        eventoActivo = false;
        
        // Cancelar todas las tareas
        if (tareaProgresion != null) {
            tareaProgresion.cancel();
        }
        for (BukkitRunnable tarea : tareasActos.values()) {
            if (tarea != null) {
                tarea.cancel();
            }
        }
        
        Bukkit.broadcastMessage(config.getString("mensajes.evento_completado", 
            "§8[§7EVENTO 6§8] §a✓ Evento completado"));
        plugin.getLogger().info("[Evento 6] Evento finalizado");
    }
    
    /**
     * Detener el evento manualmente
     */
    public void detenerEvento() {
        if (!eventoActivo) {
            return;
        }
        
        finalizarEvento();
        Bukkit.broadcastMessage("§8[§7EVENTO 6§8] §c✗ Evento detenido manualmente");
        plugin.getLogger().warning("[Evento 6] Evento detenido manualmente");
    }
    
    /**
     * Encuentra una superficie segura para spawn
     * Busca un área plana en superficie, evitando agua y lava
     */
    private Location encontrarSuperficieSegura(World mundo, Location origen) {
        int x = origen.getBlockX();
        int z = origen.getBlockZ();
        
        // Buscar en un radio de 50 bloques
        for (int radio = 0; radio < 50; radio += 5) {
            for (int angulo = 0; angulo < 360; angulo += 45) {
                int offsetX = (int) (radio * Math.cos(Math.toRadians(angulo)));
                int offsetZ = (int) (radio * Math.sin(Math.toRadians(angulo)));
                
                int testX = x + offsetX;
                int testZ = z + offsetZ;
                
                // Obtener Y más alto
                int y = mundo.getHighestBlockYAt(testX, testZ);
                
                // Verificar que sea superficie válida (Y >= 60, no agua, no lava)
                if (y >= 60) {
                    Location test = new Location(mundo, testX + 0.5, y + 1, testZ + 0.5);
                    Material bloqueAbajo = test.clone().subtract(0, 1, 0).getBlock().getType();
                    Material bloqueEnPos = test.getBlock().getType();
                    
                    // Verificar que no sea agua ni lava
                    if (bloqueAbajo.isSolid() && 
                        bloqueAbajo != Material.LAVA && 
                        bloqueAbajo != Material.WATER &&
                        bloqueEnPos == Material.AIR) {
                        
                        plugin.getLogger().info("[Evento 6] Superficie segura encontrada en: " + testX + ", " + y + ", " + testZ);
                        return test;
                    }
                }
            }
        }
        
        return null; // No se encontró superficie segura
    }
    
    // Getters
    public boolean isEventoActivo() { 
        return eventoActivo; 
    }
    
    public MundoOlvidadoFase getFaseActual() { 
        return faseActual; 
    }
    
    public Set<UUID> getParticipantes() { 
        return new HashSet<>(participantes); 
    }
    
    public long getTiempoTranscurridoSegundos() {
        if (!eventoActivo) return 0;
        return (System.currentTimeMillis() - tiempoInicio) / 1000;
    }
    
    /**
     * Activa el modo skip - desactiva la progresión automática por tiempo
     * Usado cuando se usa el comando /avo evento6 next
     */
    public void activarModoSkip() {
        this.modoSkipActivo = true;
    }
    
    /**
     * Desactiva el modo skip - reactiva la progresión automática por tiempo
     */
    public void desactivarModoSkip() {
        this.modoSkipActivo = false;
    }
    
    /**
     * Verifica si el modo skip está activo
     */
    public boolean isModoSkipActivo() {
        return modoSkipActivo;
    }
}
