package me.apocalipsis.managers;

import me.apocalipsis.Apocalipsis;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema de confirmación para operaciones peligrosas
 * Requiere confirmación explícita antes de ejecutar comandos destructivos
 */
public class ConfirmationManager {
    
    private final Apocalipsis plugin;
    private final Map<UUID, PendingConfirmation> pendingConfirmations;
    private static final long EXPIRATION_TIME = 30000; // 30 segundos
    
    public ConfirmationManager(Apocalipsis plugin) {
        this.plugin = plugin;
        this.pendingConfirmations = new ConcurrentHashMap<>();
        
        // Limpiar confirmaciones expiradas cada 10 segundos
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, 
            this::cleanExpiredConfirmations, 200L, 200L);
    }
    
    /**
     * Registra una acción pendiente de confirmación
     */
    public void requestConfirmation(Player player, ConfirmationType type, String[] args, Runnable action) {
        UUID playerId = player.getUniqueId();
        
        PendingConfirmation confirmation = new PendingConfirmation(type, args, action, System.currentTimeMillis());
        pendingConfirmations.put(playerId, confirmation);
        
        FileConfiguration config = plugin.getCicloConfig();
        String mensaje = config.getString("mensajes.confirmacion_requerida", 
            "&e⚠ &cEsta acción requiere confirmación. &eEscribe &a/avo ciclo confirmar &epara continuar (30s)")
            .replace("&", "§");
        
        player.sendMessage(mensaje);
    }
    
    /**
     * Confirma y ejecuta la acción pendiente
     */
    public boolean confirmAction(Player player) {
        UUID playerId = player.getUniqueId();
        PendingConfirmation confirmation = pendingConfirmations.get(playerId);
        
        if (confirmation == null) {
            FileConfiguration config = plugin.getCicloConfig();
            String mensaje = config.getString("mensajes.confirmacion_expirada", 
                "&cNo tienes ninguna acción pendiente de confirmación.")
                .replace("&", "§");
            player.sendMessage(mensaje);
            return false;
        }
        
        // Verificar expiración
        if (System.currentTimeMillis() - confirmation.timestamp > EXPIRATION_TIME) {
            pendingConfirmations.remove(playerId);
            FileConfiguration config = plugin.getCicloConfig();
            String mensaje = config.getString("mensajes.confirmacion_expirada", 
                "&cLa confirmación ha expirado. Intenta de nuevo.")
                .replace("&", "§");
            player.sendMessage(mensaje);
            return false;
        }
        
        // Ejecutar acción
        pendingConfirmations.remove(playerId);
        confirmation.action.run();
        
        FileConfiguration config = plugin.getCicloConfig();
        String mensaje = config.getString("mensajes.confirmacion_exitosa", 
            "&a✓ Acción confirmada y ejecutada.")
            .replace("&", "§");
        player.sendMessage(mensaje);
        
        return true;
    }
    
    /**
     * Cancela una confirmación pendiente
     */
    public boolean cancelConfirmation(Player player) {
        UUID playerId = player.getUniqueId();
        if (pendingConfirmations.remove(playerId) != null) {
            player.sendMessage("§a✓ Confirmación cancelada.");
            return true;
        }
        return false;
    }
    
    /**
     * Verifica si un jugador tiene una confirmación pendiente
     */
    public boolean hasPendingConfirmation(Player player) {
        PendingConfirmation confirmation = pendingConfirmations.get(player.getUniqueId());
        if (confirmation == null) return false;
        
        // Verificar si no expiró
        if (System.currentTimeMillis() - confirmation.timestamp > EXPIRATION_TIME) {
            pendingConfirmations.remove(player.getUniqueId());
            return false;
        }
        
        return true;
    }
    
    /**
     * Limpia confirmaciones expiradas
     */
    private void cleanExpiredConfirmations() {
        long now = System.currentTimeMillis();
        pendingConfirmations.entrySet().removeIf(entry -> 
            now - entry.getValue().timestamp > EXPIRATION_TIME
        );
    }
    
    /**
     * Limpieza al desactivar
     */
    public void shutdown() {
        pendingConfirmations.clear();
    }
    
    /**
     * Tipos de confirmación
     */
    public enum ConfirmationType {
        CREAR_CICLO,
        ELIMINAR_CICLO,
        RENOMBRAR_CICLO,
        ACTIVAR_CICLO
    }
    
    /**
     * Datos de confirmación pendiente
     */
    private static class PendingConfirmation {
        final ConfirmationType type;
        final String[] args;
        final Runnable action;
        final long timestamp;
        
        PendingConfirmation(ConfirmationType type, String[] args, Runnable action, long timestamp) {
            this.type = type;
            this.args = args;
            this.action = action;
            this.timestamp = timestamp;
        }
    }
}
