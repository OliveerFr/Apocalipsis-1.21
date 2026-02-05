package me.apocalipsis.stats;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.state.StateManager;

/**
 * Sistema de seguimiento de muertes diarias
 * Resetea automáticamente cada día del servidor
 */
public class DeathTracker {
    
    private final Apocalipsis plugin;
    private final StateManager stateManager;
    
    // UUID -> Número de muertes en el día actual
    private final Map<UUID, Integer> dailyDeaths = new HashMap<>();
    
    // Día actual del servidor (para detectar cambios)
    private int currentDay = 1;
    
    public DeathTracker(Apocalipsis plugin, StateManager stateManager) {
        this.plugin = plugin;
        this.stateManager = stateManager;
        this.currentDay = stateManager.getCurrentDay();
        
        // Iniciar tarea para verificar cambios de día cada minuto (1200 ticks)
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkDayChange, 1200L, 1200L);
    }
    
    /**
     * Registra una muerte para un jugador
     */
    public void addDeath(UUID playerUuid) {
        int deaths = dailyDeaths.getOrDefault(playerUuid, 0);
        dailyDeaths.put(playerUuid, deaths + 1);
    }
    
    /**
     * Obtiene el número de muertes del jugador en el día actual
     */
    public int getDeaths(UUID playerUuid) {
        return dailyDeaths.getOrDefault(playerUuid, 0);
    }
    
    /**
     * Resetea todas las muertes (llamado al cambiar de día)
     */
    public void resetAllDeaths() {
        dailyDeaths.clear();
        plugin.getLogger().info("[DEATH TRACKER] Muertes diarias reseteadas para nuevo día");
    }
    
    /**
     * Verifica si cambió el día del servidor y resetea estadísticas
     */
    private void checkDayChange() {
        int serverDay = stateManager.getCurrentDay();
        
        if (serverDay != currentDay) {
            plugin.getLogger().info("[DEATH TRACKER] Día cambiado: " + currentDay + " -> " + serverDay);
            currentDay = serverDay;
            resetAllDeaths();
        }
    }
    
    /**
     * Limpia las estadísticas de un jugador específico
     */
    public void clearPlayer(UUID playerUuid) {
        dailyDeaths.remove(playerUuid);
    }
    
    /**
     * Obtiene el total de jugadores con muertes registradas
     */
    public int getTotalTrackedPlayers() {
        return dailyDeaths.size();
    }
}
