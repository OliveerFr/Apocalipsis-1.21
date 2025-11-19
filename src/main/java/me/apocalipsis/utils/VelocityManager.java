package me.apocalipsis.utils;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.Bukkit;
import me.apocalipsis.Apocalipsis;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestiona aplicación gradual de velocidad para evitar detección de anti-cheats
 * 
 * Este sistema aplica velocidad de forma suave distribuida en múltiples ticks,
 * evitando los picos bruscos que los anti-cheats detectan como fly/speed hacks.
 */
public class VelocityManager {
    
    private final Apocalipsis plugin;
    private final Map<UUID, VelocityTask> activeTasks = new HashMap<>();
    
    // Configuración anti-cheat friendly
    private static final double MAX_VELOCITY_PER_TICK = 0.15; // Reducido para evitar flags
    private static final int SMOOTH_DURATION_TICKS = 5; // Aplicar en 5 ticks (0.25s)
    
    public VelocityManager(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Aplica velocidad de forma suave y gradual (anti-cheat safe)
     * 
     * @param player Jugador al que aplicar velocidad
     * @param targetVelocity Vector de velocidad objetivo
     */
    public void applySmoothedVelocity(Player player, Vector targetVelocity) {
        UUID uuid = player.getUniqueId();
        
        // Cancelar task anterior si existe
        VelocityTask existing = activeTasks.get(uuid);
        if (existing != null) {
            existing.cancel();
        }
        
        // Limitar velocidad máxima
        Vector clampedVelocity = clampVelocity(targetVelocity);
        
        // Crear nueva task de aplicación gradual
        VelocityTask task = new VelocityTask(player, clampedVelocity);
        activeTasks.put(uuid, task);
        task.start();
    }
    
    /**
     * Limita velocidad a valores seguros para anti-cheat
     */
    private Vector clampVelocity(Vector velocity) {
        double x = clamp(velocity.getX(), -MAX_VELOCITY_PER_TICK * 3, MAX_VELOCITY_PER_TICK * 3);
        double y = clamp(velocity.getY(), -MAX_VELOCITY_PER_TICK * 2, MAX_VELOCITY_PER_TICK * 2);
        double z = clamp(velocity.getZ(), -MAX_VELOCITY_PER_TICK * 3, MAX_VELOCITY_PER_TICK * 3);
        return new Vector(x, y, z);
    }
    
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Task interna que aplica velocidad gradualmente
     */
    private class VelocityTask {
        private final Player player;
        private final Vector targetVelocity;
        private int taskId = -1;
        private int ticksElapsed = 0;
        
        VelocityTask(Player player, Vector targetVelocity) {
            this.player = player;
            this.targetVelocity = targetVelocity;
        }
        
        void start() {
            taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                if (!player.isOnline() || ticksElapsed >= SMOOTH_DURATION_TICKS) {
                    cancel();
                    return;
                }
                
                // Aplicar fracción de la velocidad
                double fraction = 1.0 / (SMOOTH_DURATION_TICKS - ticksElapsed);
                Vector increment = targetVelocity.clone().multiply(fraction);
                
                Vector currentVel = player.getVelocity();
                player.setVelocity(currentVel.add(increment));
                
                ticksElapsed++;
            }, 0L, 1L); // Cada tick
        }
        
        void cancel() {
            if (taskId != -1) {
                Bukkit.getScheduler().cancelTask(taskId);
                activeTasks.remove(player.getUniqueId());
            }
        }
    }
    
    /**
     * Limpieza al desactivar plugin
     */
    public void shutdown() {
        activeTasks.values().forEach(VelocityTask::cancel);
        activeTasks.clear();
    }
}
