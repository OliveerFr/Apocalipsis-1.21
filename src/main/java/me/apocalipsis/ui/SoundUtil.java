package me.apocalipsis.ui;

import me.apocalipsis.Apocalipsis;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtil {

    private final Apocalipsis plugin;

    public SoundUtil(Apocalipsis plugin) {
        this.plugin = plugin;
    }

    /**
     * Reproduce un sonido para un jugador
     */
    public void playSound(Player player, Sound sound, float volume, float pitch) {
        if (!plugin.getConfigManager().isReproducirSonidos()) return;
        try {
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception e) {
            // Silenciar error si el sonido no existe
        }
    }

    /**
     * Reproduce un sonido en una ubicación
     */
    public void playSound(Location location, Sound sound, float volume, float pitch) {
        if (!plugin.getConfigManager().isReproducirSonidos()) return;
        try {
            location.getWorld().playSound(location, sound, volume, pitch);
        } catch (Exception e) {
            // Silenciar error
        }
    }

    /**
     * Reproduce un sonido para todos los jugadores
     */
    public void playSoundAll(Sound sound, float volume, float pitch) {
        if (!plugin.getConfigManager().isReproducirSonidos()) return;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            playSound(player, sound, volume, pitch);
        }
    }
}
