package me.apocalipsis.ciclos;

import me.apocalipsis.Apocalipsis;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener que maneja cambios de mundo y login/logout de jugadores.
 * Coordina con CicloManager para guardar/cargar inventarios y datos.
 */
public class WorldChangeListener implements Listener {
    
    private final Apocalipsis plugin;
    private final CicloManager cicloManager;
    
    public WorldChangeListener(Apocalipsis plugin, CicloManager cicloManager) {
        this.plugin = plugin;
        this.cicloManager = cicloManager;
    }
    
    /**
     * Se ejecuta cuando un jugador cambia de mundo
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String fromWorld = event.getFrom().getName();
        String toWorld = player.getWorld().getName();
        
        plugin.getLogger().info("[WorldChange] " + player.getName() + 
                                " cambió de " + fromWorld + " a " + toWorld);
        
        // Delegar al CicloManager para manejar el cambio
        cicloManager.handleWorldChange(player, fromWorld, toWorld);
        
        // Mostrar BossBar con el nombre del ciclo actual
        if (plugin.getCicloBossBarManager() != null) {
            plugin.getCicloBossBarManager().showCycleBossBar(player, toWorld);
        }
        
        // Notificar en chat si está habilitado
        org.bukkit.configuration.file.FileConfiguration config = plugin.getCicloConfig();
        if (config != null && config.getBoolean("notificaciones.jugador_cambio_mundo", false)) {
            String mensaje = config.getString("mensajes.jugador_cambio_mundo", 
                "&7Has cambiado al mundo: &b{mundo}")
                .replace("{mundo}", toWorld)
                .replace("&", "§");
            player.sendMessage(mensaje);
        }
    }
    
    /**
     * Se ejecuta cuando un jugador se conecta
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        
        plugin.getLogger().info("[WorldChange] " + player.getName() + 
                                " se conectó en mundo: " + worldName);
        
        // Cargar inventario y datos para el mundo actual
        cicloManager.handlePlayerJoin(player, worldName);
        
        // Mostrar BossBar con el nombre del ciclo actual
        if (plugin.getCicloBossBarManager() != null) {
            plugin.getCicloBossBarManager().showCycleBossBar(player, worldName);
        }
    }
    
    /**
     * Se ejecuta cuando un jugador se desconecta
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        
        plugin.getLogger().info("[WorldChange] " + player.getName() + 
                                " se desconectó del mundo: " + worldName);
        
        // Guardar inventario y datos del mundo actual
        cicloManager.handlePlayerQuit(player, worldName);
    }
}
