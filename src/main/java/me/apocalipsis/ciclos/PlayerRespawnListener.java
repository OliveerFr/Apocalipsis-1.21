package me.apocalipsis.ciclos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import me.apocalipsis.Apocalipsis;

/**
 * Listener que maneja el respawn de jugadores en ciclos
 * Asegura que los jugadores respawneen en el mismo ciclo donde murieron
 */
public class PlayerRespawnListener implements Listener {
    
    private final Apocalipsis plugin;
    @SuppressWarnings("unused")
    private final CicloManager cicloManager;
    
    // Mapa temporal para recordar en qué mundo murió cada jugador
    private final Map<UUID, String> deathWorld = new HashMap<>();
    
    public PlayerRespawnListener(Apocalipsis plugin, CicloManager cicloManager) {
        this.plugin = plugin;
        this.cicloManager = cicloManager;
    }
    
    /**
     * Registra el mundo donde murió el jugador
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String worldName = player.getWorld().getName();
        
        // Guardar el mundo de muerte
        deathWorld.put(player.getUniqueId(), worldName);
        
        plugin.getLogger().info("[PlayerRespawn] " + player.getName() + 
            " murió en mundo: " + worldName);
    }
    
    /**
     * Maneja el respawn del jugador en el mismo ciclo
     * Respeta los spawn points de cama si existen
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Obtener el mundo donde murió
        String deathWorldName = deathWorld.remove(uuid);
        
        if (deathWorldName == null) {
            // No hay registro, dejar respawn por defecto
            return;
        }
        
        // Verificar si el mundo existe
        World world = org.bukkit.Bukkit.getWorld(deathWorldName);
        if (world == null) {
            plugin.getLogger().warning("[PlayerRespawn] Mundo de muerte no existe: " + deathWorldName);
            return;
        }
        
        // IMPORTANTE: Solo interferir si el jugador está cambiando de mundo
        // Si tiene cama en el mismo mundo, Minecraft la respetará automáticamente
        String currentRespawnWorld = event.getRespawnLocation().getWorld().getName();
        
        if (currentRespawnWorld.equals(deathWorldName)) {
            // El jugador está respawneando en el mismo mundo
            // Verificar si tiene cama configurada
            if (event.isBedSpawn()) {
                // Tiene cama, dejar que Minecraft maneje el respawn
                plugin.getLogger().info("[PlayerRespawn] " + player.getName() + 
                    " respawneando en cama en: " + deathWorldName);
                return;
            }
            
            // No tiene cama, el respawn por defecto está bien
            plugin.getLogger().info("[PlayerRespawn] " + player.getName() + 
                " respawneando en spawn del mundo: " + deathWorldName);
            return;
        }
        
        // El jugador está siendo enviado a otro mundo
        // Forzar respawn en el mundo donde murió
        Location respawnLocation;
        
        // Verificar si tiene cama en el mundo de muerte
        Location bedSpawn = player.getBedSpawnLocation();
        if (bedSpawn != null && bedSpawn.getWorld().getName().equals(deathWorldName)) {
            // Tiene cama en el mundo de muerte, usarla
            respawnLocation = bedSpawn;
            plugin.getLogger().info("[PlayerRespawn] " + player.getName() + 
                " respawneando en su cama en: " + deathWorldName);
        } else {
            // No tiene cama, usar spawn del mundo
            respawnLocation = world.getSpawnLocation();
            plugin.getLogger().info("[PlayerRespawn] " + player.getName() + 
                " respawneando en spawn del mundo: " + deathWorldName);
        }
        
        // Establecer ubicación de respawn
        event.setRespawnLocation(respawnLocation);
        
        plugin.getLogger().info("[PlayerRespawn]   → Coordenadas: " + 
            respawnLocation.getBlockX() + ", " + 
            respawnLocation.getBlockY() + ", " + 
            respawnLocation.getBlockZ());
        
        // Mensaje al jugador
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage("§7Has respawneado en §e" + deathWorldName);
        }, 5L);
    }
    
    /**
     * Limpia el mapa cuando un jugador se desconecta
     */
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        deathWorld.remove(event.getPlayer().getUniqueId());
    }
}
