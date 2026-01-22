package me.apocalipsis.events.apertura;

import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import me.apocalipsis.Apocalipsis;

/**
 * Gestiona el sistema de waypoints progresivos
 */
public class AperturaEndWaypointManager {
    
    private final Apocalipsis plugin;
    private Location portalLocation;
    
    // Sistema de waypoints
    private Location waypointActual = null;
    private int waypointNumero = 0;
    private List<Location> waypointsGenerados = new ArrayList<>();
    private BukkitTask waypointParticlesTask = null;
    private int waypointsCompletados = 0;
    private final int TOTAL_WAYPOINTS = 3;
    private BukkitTask waypointCheckTask = null;
    
    // Sistema de agrupación
    private boolean jugadoresAgrupados = false;
    private BukkitTask agrupacionCheckTask = null;
    private final double DISTANCIA_MAXIMA_AGRUPACION = 30.0;
    
    public AperturaEndWaypointManager(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    public void setPortalLocation(Location portalLocation) {
        this.portalLocation = portalLocation;
    }
    
    public Location getPortalLocation() {
        return this.portalLocation;
    }
    
    public void intentarCrearWaypoint(double factorDistancia, String nombre, String mensaje) {
        if (verificarJugadoresAgrupados()) {
            jugadoresAgrupados = true;
            crearWaypoint(factorDistancia, nombre, mensaje);
            return;
        }
        
        // No están agrupados - pedir que se junten
        mostrarMensajeAgrupacion();
        iniciarVerificacionAgrupacion(factorDistancia, nombre, mensaje);
    }
    
    private void mostrarMensajeAgrupacion() {
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§5§l⚡ EL OBSERVADOR ESPERA ⚡");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7El Observador no puede ver claramente...");
        Bukkit.broadcastMessage("§7Los fragmentos están §cdispersos§7.");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§e§l⚠ REQUERIMIENTO:");
        Bukkit.broadcastMessage("§7Todos deben estar §ajuntos §7(máx. §e30 bloques§7)");
        Bukkit.broadcastMessage("§7para recibir la siguiente señal.");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.8f);
            p.spawnParticle(Particle.SMOKE, p.getLocation().add(0, 2, 0), 50, 1, 1, 1, 0.05);
        }
    }
    
    private void iniciarVerificacionAgrupacion(double factorDistancia, String nombre, String mensaje) {
        if (agrupacionCheckTask != null && !agrupacionCheckTask.isCancelled()) {
            agrupacionCheckTask.cancel();
        }
        
        agrupacionCheckTask = new BukkitRunnable() {
            int intentos = 0;
            
            @Override
            public void run() {
                intentos++;
                
                if (verificarJugadoresAgrupados()) {
                    // ¡Se agruparon!
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§5§l⚡ FRAGMENTOS UNIDOS ⚡");
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§7El Observador puede ver claramente ahora...");
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.2f);
                        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
                        p.spawnParticle(Particle.ENCHANT, p.getLocation().add(0, 2, 0), 100, 2, 2, 2, 0.3);
                    }
                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            crearWaypoint(factorDistancia, nombre, mensaje);
                        }
                    }.runTaskLater(plugin, 60L);
                    
                    cancel();
                } else if (intentos >= 24) { // 2 minutos
                    Bukkit.broadcastMessage("§8[§7...§8] §7El Observador suspira. Esperará más tiempo.");
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 100L, 100L);
    }
    
    private boolean verificarJugadoresAgrupados() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.size() <= 1) return true;
        
        World mundo = players.get(0).getWorld();
        for (Player p : players) {
            if (!p.getWorld().equals(mundo)) {
                return false;
            }
        }
        
        double maxDistancia = 0;
        for (int i = 0; i < players.size(); i++) {
            for (int j = i + 1; j < players.size(); j++) {
                double dist = players.get(i).getLocation().distance(players.get(j).getLocation());
                if (dist > maxDistancia) {
                    maxDistancia = dist;
                }
            }
        }
        
        return maxDistancia <= DISTANCIA_MAXIMA_AGRUPACION;
    }
    
    private void crearWaypoint(double factorDistancia, String nombre, String mensaje) {
        if (portalLocation == null) return;
        
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
        Bukkit.broadcastMessage("§7Una señal ha aparecido guiándolos...");
        Bukkit.broadcastMessage("§5§l⚡ §7Pilar de luz púrpura §5§l⚡ §8| §7Rastro de partículas encantadas");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        iniciarEfectosWaypoint();
        iniciarVerificacionLlegada();
    }
    
    private void iniciarEfectosWaypoint() {
        if (waypointParticlesTask != null && !waypointParticlesTask.isCancelled()) {
            waypointParticlesTask.cancel();
        }
        
        waypointParticlesTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (waypointActual == null) {
                    cancel();
                    return;
                }
                
                // Pilar de luz hacia el cielo
                for (int y = 0; y < 100; y += 2) {
                    Location particleLoc = waypointActual.clone().add(0, y, 0);
                    waypointActual.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0.1, 0.1, 0.1, 0);
                    
                    if (y % 10 == 0) {
                        waypointActual.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 3, 0.3, 0.3, 0.3, 0.1);
                    }
                }
                
                // Anillo en el suelo
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 24) {
                    double x = Math.cos(angle) * 8;
                    double z = Math.sin(angle) * 8;
                    Location ringLoc = waypointActual.clone().add(x, 0.5, z);
                    waypointActual.getWorld().spawnParticle(Particle.ENCHANT, ringLoc, 1, 0, 0, 0, 0.1);
                }
            }
        }.runTaskTimer(plugin, 60L, 10L);
    }
    
    private void iniciarVerificacionLlegada() {
        if (waypointCheckTask != null && !waypointCheckTask.isCancelled()) {
            waypointCheckTask.cancel();
        }
        
        waypointCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (waypointActual == null) {
                    cancel();
                    return;
                }
                
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getWorld().equals(waypointActual.getWorld()) && 
                        p.getLocation().distance(waypointActual) <= 8.0) {
                        
                        waypointCompletado();
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 40L, 20L);
    }
    
    private void waypointCompletado() {
        waypointsCompletados++;
        
        // Efectos de completación
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(waypointActual, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.5f);
            p.playSound(waypointActual, Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 1.2f);
            p.spawnParticle(Particle.TOTEM_OF_UNDYING, waypointActual.add(0, 1, 0), 100, 3, 3, 3, 0.3);
        }
        
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§a§l✓ WAYPOINT " + waypointNumero + " ALCANZADO");
        Bukkit.broadcastMessage("§7(" + waypointsCompletados + "/" + TOTAL_WAYPOINTS + " completados)");
        Bukkit.broadcastMessage("");
        
        // Limpiar waypoint actual
        if (waypointParticlesTask != null) {
            waypointParticlesTask.cancel();
        }
        waypointActual = null;
        
        // Crear siguiente waypoint si no es el último
        if (waypointsCompletados < TOTAL_WAYPOINTS) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    double factor = 1.0 - (0.3 * (waypointsCompletados + 1));
                    intentarCrearWaypoint(factor, "§5§l⚡ ECO MÁS CERCANO", "§7La señal se intensifica...");
                }
            }.runTaskLater(plugin, 100L);
        }
    }
    
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
    
    public void detener() {
        if (waypointParticlesTask != null) {
            waypointParticlesTask.cancel();
        }
        if (waypointCheckTask != null) {
            waypointCheckTask.cancel();
        }
        if (agrupacionCheckTask != null) {
            agrupacionCheckTask.cancel();
        }
        
        waypointActual = null;
        waypointsGenerados.clear();
        waypointsCompletados = 0;
    }
    
    // Getters
    public int getWaypointsCompletados() { return waypointsCompletados; }
    public List<Location> getWaypointsGenerados() { return new ArrayList<>(waypointsGenerados); }
    
    /**
     * Revelar dirección del portal cuando se completan las tareas
     */
    public void revelarDireccion() {
        if (portalLocation != null) {
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§5§l📍 UBICACIÓN REVELADA");
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§7Portal en: §e" + portalLocation.getBlockX() + "§7, §e" + portalLocation.getBlockY() + "§7, §e" + portalLocation.getBlockZ());
            Bukkit.broadcastMessage("§7Mundo: §e" + portalLocation.getWorld().getName());
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
            }
        }
    }
    
    /**
     * Revelar waypoint del portal cuando se completan las tareas
     */
    public void revelarWaypointPortal() {
        if (portalLocation != null) {
            // Añadir waypoint del portal para todos los jugadores
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                    // Comando para crear waypoint (compatible con diversos plugins)
                    String comando = String.format("/waypoint add Portal %d %d %d", 
                        portalLocation.getBlockX(), 
                        portalLocation.getBlockY(), 
                        portalLocation.getBlockZ());
                    
                    try {
                        player.performCommand(comando);
                    } catch (Exception e) {
                        // Fallback: mensaje con coordenadas
                        player.sendMessage("§e§l🗺 WAYPOINT: §7Portal en §e" + 
                            portalLocation.getBlockX() + "§7, §e" + 
                            portalLocation.getBlockY() + "§7, §e" + 
                            portalLocation.getBlockZ());
                    }
                }
            }
            
            plugin.getLogger().info("[Apertura End] Waypoint del portal revelado a todos los jugadores");
        }
    }
}