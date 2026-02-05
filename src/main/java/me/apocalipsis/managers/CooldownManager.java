package me.apocalipsis.managers;

import me.apocalipsis.Apocalipsis;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema de cooldowns para prevenir spam de comandos
 */
public class CooldownManager {
    
    private final Apocalipsis plugin;
    private final Map<UUID, Map<CooldownType, Long>> cooldowns;
    
    public CooldownManager(Apocalipsis plugin) {
        this.plugin = plugin;
        this.cooldowns = new ConcurrentHashMap<>();
    }
    
    /**
     * Verifica si un jugador puede ejecutar una acción
     * @return true si puede, false si está en cooldown
     */
    public boolean canUse(Player player, CooldownType type) {
        // Bypass para admins
        if (player.hasPermission("apocalipsis.ciclo.admin")) {
            return true;
        }
        
        UUID playerId = player.getUniqueId();
        Map<CooldownType, Long> playerCooldowns = cooldowns.get(playerId);
        
        if (playerCooldowns == null) {
            return true;
        }
        
        Long lastUse = playerCooldowns.get(type);
        if (lastUse == null) {
            return true;
        }
        
        long cooldownTime = getCooldownTime(type);
        long timePassed = System.currentTimeMillis() - lastUse;
        
        return timePassed >= cooldownTime;
    }
    
    /**
     * Aplica cooldown a un jugador
     */
    public void applyCooldown(Player player, CooldownType type) {
        if (player.hasPermission("apocalipsis.ciclo.admin")) {
            return; // Admins no tienen cooldown
        }
        
        UUID playerId = player.getUniqueId();
        cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                 .put(type, System.currentTimeMillis());
    }
    
    /**
     * Obtiene el tiempo restante de cooldown en segundos
     * @return segundos restantes, o 0 si no hay cooldown
     */
    public long getRemainingCooldown(Player player, CooldownType type) {
        if (player.hasPermission("apocalipsis.ciclo.admin")) {
            return 0;
        }
        
        UUID playerId = player.getUniqueId();
        Map<CooldownType, Long> playerCooldowns = cooldowns.get(playerId);
        
        if (playerCooldowns == null) {
            return 0;
        }
        
        Long lastUse = playerCooldowns.get(type);
        if (lastUse == null) {
            return 0;
        }
        
        long cooldownTime = getCooldownTime(type);
        long timePassed = System.currentTimeMillis() - lastUse;
        long remaining = cooldownTime - timePassed;
        
        return remaining > 0 ? (remaining / 1000) : 0;
    }
    
    /**
     * Envía mensaje de cooldown al jugador
     */
    public void sendCooldownMessage(Player player, CooldownType type) {
        long remaining = getRemainingCooldown(player, type);
        
        FileConfiguration config = plugin.getCicloConfig();
        String mensaje = config.getString("mensajes.cooldown_activo", 
            "&cDebes esperar &e{tiempo}s &cantes de usar este comando nuevamente.")
            .replace("{tiempo}", String.valueOf(remaining))
            .replace("&", "§");
        
        player.sendMessage(mensaje);
    }
    
    /**
     * Obtiene el tiempo de cooldown en milisegundos desde la configuración
     */
    private long getCooldownTime(CooldownType type) {
        FileConfiguration config = plugin.getCicloConfig();
        
        switch (type) {
            case CAMBIO_MUNDO:
                return config.getLong("cooldowns.cambio_mundo", 10) * 1000;
            case CREAR_CICLO:
                return config.getLong("cooldowns.crear_ciclo", 300) * 1000;
            case RANDOM_TP:
                return config.getLong("cooldowns.random_tp", 300) * 1000; // 5 minutos por defecto
            case END_ESCAPE:
                return config.getLong("cooldowns.end_escape", 30) * 1000; // 30 segundos por defecto
            default:
                return 0;
        }
    }
    
    /**
     * Limpia cooldowns de un jugador
     */
    public void clearCooldowns(Player player) {
        cooldowns.remove(player.getUniqueId());
    }
    
    /**
     * Limpia todos los cooldowns
     */
    public void clearAllCooldowns() {
        cooldowns.clear();
    }
    
    /**
     * Limpieza al desactivar
     */
    public void shutdown() {
        cooldowns.clear();
    }
    
    /**
     * Tipos de cooldown
     */
    public enum CooldownType {
        CAMBIO_MUNDO,    // Cambiar de mundo/ciclo
        CREAR_CICLO,     // Crear nuevo ciclo
        RANDOM_TP,       // Random Teleport (/rtp)
        END_ESCAPE       // Escapar del End (/avo volver)
    }
}
