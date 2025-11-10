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

import me.apocalipsis.Apocalipsis;

/**
 * Sistema de rastreo de bloques para prevenir griefing entre jugadores.
 * Rastrea qué jugador colocó cada bloque para que los desastres no destruyan bloques ajenos.
 * 
 * Optimización:
 * - Usa ConcurrentHashMap para operaciones thread-safe
 * - Limita el tamaño máximo del cache (últimos 50,000 bloques)
 * - Persiste datos importantes en archivo YAML
 */
public class BlockOwnershipTracker {
    
    private final Apocalipsis plugin;
    private final Map<String, UUID> blockOwners; // "world:x:y:z" -> UUID del jugador
    private final File dataFile;
    private static final int MAX_CACHE_SIZE = 50000; // Límite para evitar uso excesivo de memoria
    
    public BlockOwnershipTracker(Apocalipsis plugin) {
        this.plugin = plugin;
        this.blockOwners = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "block_ownership.yml");
        
        // Cargar datos existentes
        loadData();
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
        
        // Limitar tamaño del cache
        if (blockOwners.size() >= MAX_CACHE_SIZE) {
            // Remover 10% de entradas más antiguas (simple cleanup)
            int toRemove = MAX_CACHE_SIZE / 10;
            blockOwners.keySet().stream()
                .limit(toRemove)
                .forEach(blockOwners::remove);
        }
        
        blockOwners.put(key, player.getUniqueId());
    }
    
    /**
     * Registra que un bloque fue roto
     */
    public void trackBlockBreak(Block block) {
        String key = getBlockKey(block);
        blockOwners.remove(key);
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
     */
    private void loadData() {
        if (!dataFile.exists()) {
            return;
        }
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            
            for (String key : config.getKeys(false)) {
                try {
                    UUID owner = UUID.fromString(config.getString(key));
                    blockOwners.put(key, owner);
                } catch (IllegalArgumentException e) {
                    // UUID inválido, ignorar
                }
            }
            
            plugin.getLogger().info("[BlockTracker] Cargados " + blockOwners.size() + " bloques rastreados");
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
     * Limpia todos los datos (útil para mantenimiento)
     */
    public void clearAll() {
        blockOwners.clear();
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
