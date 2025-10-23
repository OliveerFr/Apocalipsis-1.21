package me.apocalipsis.disaster;

import org.bukkit.entity.Player;

public interface Disaster {
    
    /**
     * Inicia el desastre
     */
    void start();

    /**
     * Detiene el desastre
     */
    void stop();

    /**
     * Tick del desastre (llamado cada tick mientras está activo)
     */
    void tick();

    /**
     * Retorna el ID del desastre
     */
    String getId();

    /**
     * Retorna si el desastre está activo
     */
    boolean isActive();

    /**
     * Aplica efectos al jugador (llamado cada tick para cada jugador)
     */
    void applyEffects(Player player);
}
