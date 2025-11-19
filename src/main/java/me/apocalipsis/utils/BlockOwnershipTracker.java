package me.apocalipsis.utils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.apocalipsis.Apocalipsis;

/**
 * Sistema de rastreo de bloques para prevenir griefing entre jugadores.
 * Rastrea qué jugador colocó cada bloque para que los desastres no destruyan bloques ajenos.
 * 
 * Optimización:
 * - Usa ConcurrentHashMap para operaciones thread-safe
 * - Limita el tamaño máximo del cache (últimos 10,000 bloques)
 * - Cleanup automático de bloques mayores a 30 minutos
 * - Persiste datos importantes en archivo YAML
 */
public class BlockOwnershipTracker {
    
    private final Apocalipsis plugin;
    private final Map<String, UUID> blockOwners; // "world:x:y:z" -> UUID del jugador
    private final Map<String, Long> blockTimestamps; // "world:x:y:z" -> timestamp en millis
    private final File dataFile;
    private static final int MAX_CACHE_SIZE = 10000; // Límite para evitar uso excesivo de memoria
    private static final long BLOCK_EXPIRE_TIME = 30 * 60 * 1000L; // 30 minutos en millis
    private BukkitRunnable cleanupTask;
    
    public BlockOwnershipTracker(Apocalipsis plugin) {
        this.plugin = plugin;
        this.blockOwners = new ConcurrentHashMap<>();
        this.blockTimestamps = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "block_ownership.yml");
        
        // Cargar datos existentes
        loadData();
        
        // Iniciar cleanup automático cada 5 minutos
        startCleanupTask();
    }
    
    /**
     * Registra que un jugador colocó un bloque
     */
    public void trackBlockPlacement(Block block, Player player) {
        // No trackear bloques de jugadores exentos (admins en modo creativo, etc.)
        if (player.hasPermission("avo.bypass.blocktrack")) {
            return;
        }
        
        String key = getBlockKey(block);
        long now = System.currentTimeMillis();
        
        // Limitar tamaño del cache - cleanup por antigüedad
        if (blockOwners.size() >= MAX_CACHE_SIZE) {
            cleanupOldBlocks();
        }
        
        blockOwners.put(key, player.getUniqueId());
        blockTimestamps.put(key, now);
    }
    
    /**
     * Registra que un bloque fue roto
     */
    public void trackBlockBreak(Block block) {
        String key = getBlockKey(block);
        blockOwners.remove(key);
        blockTimestamps.remove(key);
    }
    
    /**
     * Verifica si un bloque puede ser destruido por un desastre para un jugador específico.
     * 
     * @return true si el bloque puede ser destruido (es del jugador, natural, o sin dueño)
     */
    public boolean canDisasterDestroyBlock(Block block, Player affectedPlayer) {
        String key = getBlockKey(block);
        UUID owner = blockOwners.get(key);
        
        // Si no tiene dueño registrado, es natural o muy antiguo -> permitir
        if (owner == null) {
            return true;
        }
        
        // Si el dueño es el mismo jugador afectado -> permitir
        if (owner.equals(affectedPlayer.getUniqueId())) {
            return true;
        }
        
        // Es de otro jugador -> NO permitir (anti-griefing)
        return false;
    }
    
    /**
     * Obtiene el UUID del dueño de un bloque (o null si no tiene dueño)
     */
    public UUID getBlockOwner(Block block) {
        String key = getBlockKey(block);
        return blockOwners.get(key);
    }
    
    /**
     * Genera una clave única para un bloque
     */
    private String getBlockKey(Block block) {
        Location loc = block.getLocation();
        return String.format("%s:%d:%d:%d", 
            loc.getWorld().getName(),
            loc.getBlockX(),
            loc.getBlockY(),
            loc.getBlockZ()
        );
    }
    
    /**
     * Carga datos de bloques persistidos (solo los más importantes)
     * Limita la carga a MAX_CACHE_SIZE bloques
     */
    private void loadData() {
        if (!dataFile.exists()) {
            return;
        }
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            long now = System.currentTimeMillis();
            int loaded = 0;
            
            for (String key : config.getKeys(false)) {
                if (loaded >= MAX_CACHE_SIZE) break;
                
                try {
                    UUID owner = UUID.fromString(config.getString(key));
                    blockOwners.put(key, owner);
                    blockTimestamps.put(key, now); // Timestamp actual al cargar
                    loaded++;
                } catch (IllegalArgumentException e) {
                    // UUID inválido, ignorar
                }
            }
            
            plugin.getLogger().info("[BlockTracker] Cargados " + loaded + " bloques rastreados");
        } catch (Exception e) {
            plugin.getLogger().warning("[BlockTracker] Error cargando datos: " + e.getMessage());
        }
    }
    
    /**
     * Guarda datos de bloques importantes (llamar al desactivar plugin)
     * Solo guarda una muestra para evitar archivos enormes
     */
    public void saveData() {
        try {
            FileConfiguration config = new YamlConfiguration();
            
            // Guardar solo los primeros 10,000 bloques para evitar archivos gigantes
            int saved = 0;
            for (Map.Entry<String, UUID> entry : blockOwners.entrySet()) {
                if (saved >= 10000) break;
                config.set(entry.getKey(), entry.getValue().toString());
                saved++;
            }
            
            config.save(dataFile);
            plugin.getLogger().info("[BlockTracker] Guardados " + saved + " bloques rastreados");
        } catch (IOException e) {
            plugin.getLogger().warning("[BlockTracker] Error guardando datos: " + e.getMessage());
        }
    }
    
    /**
     * Inicia la tarea de cleanup automático
     */
    private void startCleanupTask() {
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupOldBlocks();
            }
        };
        // Ejecutar cada 5 minutos (6000 ticks)
        cleanupTask.runTaskTimerAsynchronously(plugin, 6000L, 6000L);
    }
    
    /**
     * Limpia bloques que tengan más de 30 minutos
     */
    private void cleanupOldBlocks() {
        long now = System.currentTimeMillis();
        int removed = 0;
        
        for (Map.Entry<String, Long> entry : blockTimestamps.entrySet()) {
            if (now - entry.getValue() > BLOCK_EXPIRE_TIME) {
                String key = entry.getKey();
                blockOwners.remove(key);
                blockTimestamps.remove(key);
                removed++;
            }
        }
        
        if (removed > 0) {
            plugin.getLogger().info("[BlockTracker] Limpiados " + removed + " bloques expirados (>30 min)");
        }
    }
    
    /**
     * Detiene la tarea de cleanup (llamar en onDisable)
     */
    public void stopCleanupTask() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }
    }
    
    /**
     * Limpia todos los datos (útil para mantenimiento)
     */
    public void clearAll() {
        blockOwners.clear();
        blockTimestamps.clear();
        if (dataFile.exists()) {
            dataFile.delete();
        }
    }
    
    /**
     * Obtiene estadísticas del tracker
     */
    public int getTrackedBlocksCount() {
        return blockOwners.size();
    }
}
