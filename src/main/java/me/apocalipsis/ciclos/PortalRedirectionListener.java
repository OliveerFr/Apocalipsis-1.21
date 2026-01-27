package me.apocalipsis.ciclos;

import me.apocalipsis.Apocalipsis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Redirige portales Nether/End al mundo correcto del ciclo
 * 
 * Previene que jugadores usen portales del Overworld original
 * para acceder al Nether/End de un ciclo diferente.
 * 
 * IMPORTANTE: El End es COMPARTIDO entre todos los ciclos.
 * Este listener recuerda de qué mundo vino cada jugador para
 * retornarlo al mundo correcto al salir del End.
 * 
 * Ejemplo:
 * - Jugador en "ciclo_1" usa portal Nether → va a "ciclo_1_nether"
 * - Jugador en "ciclo_1" usa portal End → va a "world_the_end" (compartido)
 * - Jugador sale del End → regresa a "ciclo_1" (su mundo de origen)
 */
public class PortalRedirectionListener implements Listener {
    
    private final Apocalipsis plugin;
    private final CicloManager cicloManager;
    
    // Rastreo de mundo de origen para el End compartido
    private final Map<UUID, String> endOriginWorld = new HashMap<>();
    
    // Archivo de persistencia
    private final File dataFile;
    private FileConfiguration dataConfig;
    
    public PortalRedirectionListener(Apocalipsis plugin, CicloManager cicloManager) {
        this.plugin = plugin;
        this.cicloManager = cicloManager;
        
        // Inicializar archivo de persistencia
        this.dataFile = new File(plugin.getDataFolder(), "portal_origins.yml");
        loadEndOriginData();
    }
    
    /**
     * Maneja el uso de portales para redirigir al mundo correcto
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        TeleportCause cause = event.getCause();
        
        // Solo manejar portales Nether y End
        if (cause != TeleportCause.NETHER_PORTAL && cause != TeleportCause.END_PORTAL) {
            return;
        }
        
        World fromWorld = from.getWorld();
        String fromWorldName = fromWorld.getName();
        World.Environment fromEnv = fromWorld.getEnvironment();
        
        // Determinar el mundo de destino correcto
        String targetWorldName = getCorrectPortalDestination(fromWorldName, cause);
        
        if (targetWorldName == null) {
            // Sin mundo de destino, dejar comportamiento por defecto
            return;
        }
        
        // Si va al End, recordar mundo de origen
        if (cause == TeleportCause.END_PORTAL && fromEnv == World.Environment.NORMAL) {
            endOriginWorld.put(player.getUniqueId(), fromWorldName);
            plugin.getLogger().info("[PortalRedirection] Guardado origen de " + player.getName() + ": " + fromWorldName);
        }
        
        // Si sale del End, obtener mundo de origen y limpiar
        if (cause == TeleportCause.END_PORTAL && fromEnv == World.Environment.THE_END) {
            String originWorld = endOriginWorld.remove(player.getUniqueId());
            if (originWorld != null) {
                targetWorldName = originWorld;
                plugin.getLogger().info("[PortalRedirection] " + player.getName() + " regresa a mundo origen: " + originWorld);
            }
        }
        
        World targetWorld = Bukkit.getWorld(targetWorldName);
        
        if (targetWorld == null) {
            // Mundo no existe, crearlo automáticamente
            targetWorld = createPortalWorld(targetWorldName, cause);
            
            if (targetWorld == null) {
                player.sendMessage("§c✗ Error: No se pudo crear el mundo de destino.");
                event.setCancelled(true);
                return;
            }
        }
        
        // Calcular ubicación de destino
        Location destination = calculatePortalDestination(from, targetWorld, cause);
        
        // Redirigir al mundo correcto
        event.setTo(destination);
        
        plugin.getLogger().info("[PortalRedirection] " + player.getName() + 
            " portal " + cause.name() + ": " + fromWorldName + " → " + targetWorldName);
    }
    
    /**
     * Determina el nombre del mundo de destino correcto según el portal usado
     */
    private String getCorrectPortalDestination(String fromWorldName, TeleportCause cause) {
        // Obtener ambiente del mundo actual
        World fromWorld = Bukkit.getWorld(fromWorldName);
        if (fromWorld == null) {
            return null;
        }
        
        World.Environment fromEnv = fromWorld.getEnvironment();
        
        // CASO 1: Portal Nether
        if (cause == TeleportCause.NETHER_PORTAL) {
            if (fromEnv == World.Environment.NORMAL) {
                // Overworld → Nether
                // Si es "ciclo_1" → "ciclo_1_nether"
                // Si es "world" → "world_nether"
                return fromWorldName + "_nether";
                
            } else if (fromEnv == World.Environment.NETHER) {
                // Nether → Overworld
                // Si es "ciclo_1_nether" → "ciclo_1"
                // Si es "world_nether" → "world"
                if (fromWorldName.endsWith("_nether")) {
                    return fromWorldName.substring(0, fromWorldName.length() - 7); // Quitar "_nether"
                }
                return fromWorldName; // Fallback
            }
        }
        
        // CASO 2: Portal End
        if (cause == TeleportCause.END_PORTAL) {
            if (fromEnv == World.Environment.NORMAL) {
                // Overworld → End
                // TODOS los ciclos van al MISMO End compartido
                // "ciclo_1" → "world_the_end"
                // "ciclo_2" → "world_the_end"
                // "world" → "world_the_end"
                return "world_the_end";
                
            } else if (fromEnv == World.Environment.THE_END) {
                // End → Overworld
                // El destino real se determina desde endOriginWorld
                // Esto es solo un fallback si no hay registro
                return "world"; // Mundo original por defecto
            }
        }
        
        return null; // No redirigir
    }
    
    /**
     * Crea un mundo de portal automáticamente si no existe
     */
    private World createPortalWorld(String worldName, TeleportCause cause) {
        try {
            plugin.getLogger().info("[PortalRedirection] Creando mundo automáticamente: " + worldName);
            
            org.bukkit.WorldCreator creator = new org.bukkit.WorldCreator(worldName);
            
            // Determinar ambiente según el tipo de portal
            if (cause == TeleportCause.NETHER_PORTAL) {
                creator.environment(World.Environment.NETHER);
            } else if (cause == TeleportCause.END_PORTAL) {
                creator.environment(World.Environment.THE_END);
            }
            
            World world = creator.createWorld();
            
            if (world != null) {
                world.setKeepSpawnInMemory(true);
                plugin.getLogger().info("[PortalRedirection] ✓ Mundo creado: " + worldName);
                return world;
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("[PortalRedirection] Error creando mundo: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Calcula la ubicación de destino en el mundo del portal
     */
    private Location calculatePortalDestination(Location from, World targetWorld, TeleportCause cause) {
        if (cause == TeleportCause.NETHER_PORTAL) {
            // Conversión Overworld ↔ Nether (1:8)
            if (from.getWorld().getEnvironment() == World.Environment.NORMAL) {
                // Overworld → Nether (dividir por 8)
                double x = from.getX() / 8.0;
                double z = from.getZ() / 8.0;
                return new Location(targetWorld, x, from.getY(), z);
                
            } else if (from.getWorld().getEnvironment() == World.Environment.NETHER) {
                // Nether → Overworld (multiplicar por 8)
                double x = from.getX() * 8.0;
                double z = from.getZ() * 8.0;
                return new Location(targetWorld, x, from.getY(), z);
            }
        }
        
        if (cause == TeleportCause.END_PORTAL) {
            if (from.getWorld().getEnvironment() == World.Environment.NORMAL) {
                // Overworld → End (plataforma de spawn del End)
                return targetWorld.getSpawnLocation();
                
            } else if (from.getWorld().getEnvironment() == World.Environment.THE_END) {
                // End → Overworld (spawn del Overworld)
                return targetWorld.getSpawnLocation();
            }
        }
        
        // Fallback: spawn del mundo destino
        return targetWorld.getSpawnLocation();
    }
    
    /**
     * Carga los datos de mundo de origen desde el archivo
     * Se ejecuta al iniciar el plugin
     */
    private void loadEndOriginData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
                plugin.getLogger().info("[PortalRedirection] Archivo de persistencia creado");
            } catch (IOException e) {
                plugin.getLogger().warning("[PortalRedirection] Error creando archivo de persistencia: " + e.getMessage());
                return;
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        // Cargar datos del archivo al HashMap
        if (dataConfig.contains("end_origins")) {
            for (String uuidStr : dataConfig.getConfigurationSection("end_origins").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String worldName = dataConfig.getString("end_origins." + uuidStr);
                    
                    if (worldName != null && !worldName.isEmpty()) {
                        endOriginWorld.put(uuid, worldName);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("[PortalRedirection] UUID inválido en persistencia: " + uuidStr);
                }
            }
            
            plugin.getLogger().info("[PortalRedirection] Cargados " + endOriginWorld.size() + " orígenes del End desde archivo");
        }
    }
    
    /**
     * Guarda los datos de mundo de origen al archivo
     * Se ejecuta cuando jugadores se desconectan y al apagar el servidor
     */
    public void saveEndOriginData() {
        try {
            // Limpiar sección existente
            dataConfig.set("end_origins", null);
            
            // Guardar HashMap actual
            for (Map.Entry<UUID, String> entry : endOriginWorld.entrySet()) {
                dataConfig.set("end_origins." + entry.getKey().toString(), entry.getValue());
            }
            
            // Guardar timestamp
            dataConfig.set("last_save", System.currentTimeMillis());
            
            // Escribir a disco
            dataConfig.save(dataFile);
            
        } catch (IOException e) {
            plugin.getLogger().severe("[PortalRedirection] Error guardando datos de origen: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene el número de jugadores rastreados en el End
     * Útil para depuración
     */
    public int getTrackedPlayersCount() {
        return endOriginWorld.size();
    }
    
    /**
     * Limpia el rastreo de mundo de origen cuando un jugador se desconecta
     * Previene memory leaks del HashMap
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String removed = endOriginWorld.remove(uuid);
        
        if (removed != null) {
            plugin.getLogger().info("[PortalRedirection] Limpiado origen de " + 
                event.getPlayer().getName() + " (desconexión en End)");
        }
        
        // Guardar datos persistentes
        saveEndOriginData();
    }
}
