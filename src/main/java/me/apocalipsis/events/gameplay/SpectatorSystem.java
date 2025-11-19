package me.apocalipsis.events.gameplay;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import me.apocalipsis.Apocalipsis;

/**
 * Sistema de modo espectador para eventos
 * 
 * Permite a jugadores muertos:
 * - Seguir viendo el evento sin interferir
 * - Volar libremente en modo espectador
 * - Ver información del progreso del evento
 * - Recibir notificaciones importantes
 * 
 * Restricciones:
 * - No pueden interactuar con entidades
 * - No pueden recoger items
 * - No pueden abrir inventarios
 * - Se restauran al terminar el evento
 */
public class SpectatorSystem implements Listener {
    
    private final Apocalipsis plugin;
    
    // Estado del sistema
    private boolean enabled = false;
    private String eventId = null;
    
    // Jugadores en modo espectador
    private final Set<UUID> spectators = ConcurrentHashMap.newKeySet();
    
    // Estados originales antes de entrar a espectador
    private final Map<UUID, PlayerState> originalStates = new ConcurrentHashMap<>();
    
    // Configuración
    private boolean allowFlying = true;
    private boolean showEventInfo = true;
    private boolean muteDeathMessages = true;
    private boolean preventInteraction = true;
    private Location spectatorSpawn = null;
    
    // Tareas
    private BukkitTask infoTask;
    
    /**
     * Constructor
     */
    public SpectatorSystem(Apocalipsis plugin) {
        this.plugin = plugin;
        
        // Registrar listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ACTIVACIÓN/DESACTIVACIÓN
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Activa el sistema
     */
    public void enable(String eventId) {
        this.eventId = eventId;
        this.enabled = true;
        
        plugin.getLogger().info("[SpectatorSystem] Sistema activado para evento: " + eventId);
    }
    
    /**
     * Desactiva el sistema y restaura jugadores
     */
    public void disable() {
        this.enabled = false;
        
        // Restaurar todos los espectadores
        for (UUID uuid : new ArrayList<>(spectators)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                restorePlayer(p);
            }
        }
        
        // Limpiar datos
        spectators.clear();
        originalStates.clear();
        
        if (infoTask != null) {
            infoTask.cancel();
            infoTask = null;
        }
        
        plugin.getLogger().info("[SpectatorSystem] Sistema desactivado");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Configura el sistema
     */
    public void configure(boolean allowFlying, boolean showInfo, boolean muteDeaths, 
                         boolean preventInteract, Location spawn) {
        this.allowFlying = allowFlying;
        this.showEventInfo = showInfo;
        this.muteDeathMessages = muteDeaths;
        this.preventInteraction = preventInteract;
        this.spectatorSpawn = spawn;
    }
    
    /**
     * Establece la ubicación de spawn para espectadores
     */
    public void setSpectatorSpawn(Location spawn) {
        this.spectatorSpawn = spawn;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // GESTIÓN DE ESPECTADORES
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Convierte un jugador a espectador
     */
    public void makeSpectator(Player player) {
        if (!enabled) return;
        if (spectators.contains(player.getUniqueId())) return;
        
        // Guardar estado original
        savePlayerState(player);
        
        // Convertir a espectador
        spectators.add(player.getUniqueId());
        
        // Cambiar gamemode
        player.setGameMode(GameMode.SPECTATOR);
        
        // Permitir volar si está configurado
        if (allowFlying) {
            player.setAllowFlight(true);
            player.setFlying(true);
        }
        
        // Teleportar a spawn de espectadores si existe
        if (spectatorSpawn != null) {
            player.teleport(spectatorSpawn);
        }
        
        // Efectos visuales
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.NIGHT_VISION, 999999, 0, false, false
        ));
        
        // Mensajes
        player.sendMessage("§7§o━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§7§l§oModo Espectador Activado");
        player.sendMessage("§7");
        player.sendMessage("§7Puedes seguir observando el evento");
        player.sendMessage("§7sin interferir. Serás restaurado");
        player.sendMessage("§7al finalizar.");
        player.sendMessage("§7§o━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.5f);
        
        plugin.getLogger().info("[SpectatorSystem] " + player.getName() + " convertido a espectador");
    }
    
    /**
     * Restaura un jugador a su estado original
     */
    public void restorePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!spectators.contains(uuid)) return;
        
        // Obtener estado original
        PlayerState state = originalStates.get(uuid);
        if (state != null) {
            state.restore(player);
        } else {
            // Fallback: modo supervivencia por defecto
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
            player.setFoodLevel(20);
        }
        
        // Remover de espectadores
        spectators.remove(uuid);
        originalStates.remove(uuid);
        
        // Remover efectos
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        
        // Mensaje
        player.sendMessage("§a✓ Has sido restaurado a tu estado original");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        plugin.getLogger().info("[SpectatorSystem] " + player.getName() + " restaurado");
    }
    
    /**
     * Verifica si un jugador es espectador
     */
    public boolean isSpectator(UUID uuid) {
        return spectators.contains(uuid);
    }
    
    /**
     * Obtiene todos los espectadores
     */
    public Set<UUID> getSpectators() {
        return new HashSet<>(spectators);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ESTADO DE JUGADOR
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Guarda el estado actual del jugador
     */
    private void savePlayerState(Player player) {
        originalStates.put(player.getUniqueId(), new PlayerState(player));
    }
    
    /**
     * Clase interna para guardar estado de jugador
     */
    private static class PlayerState {
        private final GameMode gameMode;
        private final double health;
        private final int foodLevel;
        private final Location location;
        private final boolean allowFlight;
        private final boolean flying;
        
        public PlayerState(Player player) {
            this.gameMode = player.getGameMode();
            this.health = player.getHealth();
            this.foodLevel = player.getFoodLevel();
            this.location = player.getLocation().clone();
            this.allowFlight = player.getAllowFlight();
            this.flying = player.isFlying();
        }
        
        public void restore(Player player) {
            player.setGameMode(gameMode);
            player.setHealth(Math.min(health, player.getAttribute(
                org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()));
            player.setFoodLevel(foodLevel);
            player.teleport(location);
            player.setAllowFlight(allowFlight);
            player.setFlying(flying && allowFlight);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // EVENT HANDLERS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Convierte jugadores muertos a espectadores automáticamente
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!enabled) return;
        
        Player player = event.getEntity();
        
        // Mutear mensaje de muerte si está configurado
        if (muteDeathMessages) {
            event.setDeathMessage(null);
        }
        
        // Convertir a espectador después de respawn
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                makeSpectator(player);
            }
        }, 10L); // 0.5 segundos después de respawn
    }
    
    /**
     * Previene interacción de espectadores con entidades
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!enabled || !preventInteraction) return;
        
        if (isSpectator(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Previene interacción de espectadores con bloques
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!enabled || !preventInteraction) return;
        
        if (isSpectator(event.getPlayer().getUniqueId())) {
            if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK ||
                event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) {
                event.setCancelled(true);
            }
        }
    }
    
    /**
     * Previene que espectadores recojan items
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPickupItem(PlayerAttemptPickupItemEvent event) {
        if (!enabled) return;
        
        if (isSpectator(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Previene que espectadores dropeen items
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!enabled) return;
        
        if (isSpectator(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Maneja salida de jugadores
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!enabled) return;
        
        UUID uuid = event.getPlayer().getUniqueId();
        if (spectators.contains(uuid)) {
            // Guardar estado para restaurar cuando regrese
            spectators.remove(uuid);
            // Mantener originalStates para cuando regrese
        }
    }
    
    /**
     * Restaura jugadores que regresan
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        
        UUID uuid = event.getPlayer().getUniqueId();
        
        // Si tenía un estado guardado, restaurar
        if (originalStates.containsKey(uuid)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (event.getPlayer().isOnline()) {
                    makeSpectator(event.getPlayer());
                }
            }, 20L);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // INFORMACIÓN DEL EVENTO
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Inicia broadcast periódico de información del evento
     */
    public void startEventInfoBroadcast(String info, int intervalTicks) {
        if (!showEventInfo) return;
        
        if (infoTask != null) infoTask.cancel();
        
        infoTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : spectators) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.sendActionBar("§7" + info);
                }
            }
        }, 0L, intervalTicks);
    }
    
    /**
     * Envía mensaje a todos los espectadores
     */
    public void broadcastToSpectators(String message) {
        for (UUID uuid : spectators) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage(message);
            }
        }
    }
    
    /**
     * Reproduce sonido a todos los espectadores
     */
    public void playSoundToSpectators(Sound sound, float volume, float pitch) {
        for (UUID uuid : spectators) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.playSound(p.getLocation(), sound, volume, pitch);
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Limpia todo el sistema
     */
    public void cleanup() {
        disable();
    }
    
    /**
     * Obtiene estadísticas
     */
    public Map<String, Integer> getStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("spectators", spectators.size());
        stats.put("saved_states", originalStates.size());
        return stats;
    }
}
