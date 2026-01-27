package me.apocalipsis.managers;

import me.apocalipsis.Apocalipsis;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema de countdown visual antes de teletransportar
 */
public class CountdownManager {
    
    private final Apocalipsis plugin;
    private final Map<UUID, BukkitTask> activeCountdowns;
    
    public CountdownManager(Apocalipsis plugin) {
        this.plugin = plugin;
        this.activeCountdowns = new ConcurrentHashMap<>();
    }
    
    /**
     * Inicia countdown de teletransporte
     */
    public void startTeleportCountdown(Player player, String targetWorld, Runnable onComplete) {
        // Cancelar countdown existente si hay uno
        cancelCountdown(player);
        
        FileConfiguration config = plugin.getCicloConfig();
        int countdownSeconds = config.getInt("config.countdown_teleport", 5);
        
        UUID playerId = player.getUniqueId();
        
        // Countdown visual
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int remaining = countdownSeconds;
            
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelCountdown(player);
                    return;
                }
                
                if (remaining <= 0) {
                    // Ejecutar teletransporte
                    cancelCountdown(player);
                    onComplete.run();
                    
                    // Sonido de éxito
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    return;
                }
                
                // Mostrar título
                String color = remaining <= 2 ? "§c" : remaining <= 3 ? "§e" : "§a";
                player.sendTitle(
                    color + "§l" + remaining,
                    "§7Teletransportando a §b" + targetWorld,
                    0, 20, 10
                );
                
                // Mensaje en chat
                FileConfiguration cfg = plugin.getCicloConfig();
                String mensaje = cfg.getString("mensajes.countdown_teleport", 
                    "&aTeletransportando en &e{tiempo}s&a...")
                    .replace("{tiempo}", String.valueOf(remaining))
                    .replace("&", "§");
                player.sendMessage(mensaje);
                
                // Sonido
                float pitch = 0.5f + (0.5f * (countdownSeconds - remaining) / countdownSeconds);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, pitch);
                
                remaining--;
            }
        }, 0L, 20L); // Cada segundo
        
        activeCountdowns.put(playerId, task);
    }
    
    /**
     * Cancela countdown de un jugador
     */
    public void cancelCountdown(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitTask task = activeCountdowns.remove(playerId);
        
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Verifica si un jugador tiene countdown activo
     */
    public boolean hasActiveCountdown(Player player) {
        return activeCountdowns.containsKey(player.getUniqueId());
    }
    
    /**
     * Cancela todos los countdowns
     */
    public void cancelAllCountdowns() {
        activeCountdowns.values().forEach(BukkitTask::cancel);
        activeCountdowns.clear();
    }
    
    /**
     * Limpieza al desactivar
     */
    public void shutdown() {
        cancelAllCountdowns();
    }
}
