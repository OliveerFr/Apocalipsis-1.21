package me.apocalipsis.listeners;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.ui.ScoreboardManager;
import me.apocalipsis.ui.TablistManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final Apocalipsis plugin;
    private final ScoreboardManager scoreboardManager;
    private final TablistManager tablistManager;

    public PlayerListener(Apocalipsis plugin, ScoreboardManager scoreboardManager,
                         TablistManager tablistManager) {
        this.plugin = plugin;
        this.scoreboardManager = scoreboardManager;
        this.tablistManager = tablistManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // [FIX DEFINITIVO] Forzar board compartido (crítico para que todos vean lo mismo)
        player.setScoreboard(org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard());
        
        // [AUTOASIGNACIÓN] Late-join: asignar misiones si el día está activo
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getMissionService().assignMissionsToPlayer(player);
        }, 2L);

        // Actualizar UI
        scoreboardManager.updatePlayer(player);
        
        // [RECONSTRUCCIÓN] Agregar jugador al BossBar único del DisasterController
        plugin.getDisasterController().addPlayerToBossBar(player);
        
        // [FIX DEFINITIVO] Aplicar TAB prefix al jugador que entra
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            tablistManager.applyTabPrefix(player);
            
            // Reaplicar a los demás también (para que te vean con prefijo al instante)
            for (Player other : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (other.equals(player)) continue;
                tablistManager.applyTabPrefix(other);
            }
            
            // Actualizar header/footer para todos
            tablistManager.updateAll();
        }, 10L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // [RECONSTRUCCIÓN] Remover jugador del BossBar único del DisasterController
        plugin.getDisasterController().removePlayerFromBossBar(player);
        
        scoreboardManager.clearPlayer(player);
    }
}
