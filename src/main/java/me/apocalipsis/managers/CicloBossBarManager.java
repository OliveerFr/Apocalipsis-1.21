package me.apocalipsis.managers;

import me.apocalipsis.Apocalipsis;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema de BossBar para mostrar el ciclo actual
 */
public class CicloBossBarManager {
    
    private final Apocalipsis plugin;
    private final Map<UUID, BossBar> playerBossBars;
    private final Map<UUID, BukkitTask> autoHideTasks;
    
    public CicloBossBarManager(Apocalipsis plugin) {
        this.plugin = plugin;
        this.playerBossBars = new ConcurrentHashMap<>();
        this.autoHideTasks = new ConcurrentHashMap<>();
    }
    
    /**
     * Muestra BossBar al jugador con el nombre del ciclo actual
     */
    public void showCycleBossBar(Player player, String cycleName) {
        FileConfiguration config = plugin.getCicloConfig();
        
        // Verificar si está habilitado
        if (!config.getBoolean("config.mostrar_bossbar", true)) {
            return;
        }
        
        // Cancelar BossBar anterior si existe
        removeBossBar(player);
        
        // Obtener configuración
        String colorStr = config.getString("config.bossbar_color", "BLUE");
        String styleStr = config.getString("config.bossbar_style", "PROGRESS");
        
        BarColor color = parseBarColor(colorStr);
        BarStyle style = parseBarStyle(styleStr);
        
        // Crear BossBar
        String title = "§b§lCiclo: §f" + cycleName;
        BossBar bossBar = Bukkit.createBossBar(title, color, style);
        bossBar.setProgress(1.0);
        bossBar.addPlayer(player);
        
        playerBossBars.put(player.getUniqueId(), bossBar);
        
        // Auto-ocultar después de 10 segundos
        scheduleAutoHide(player);
    }
    
    /**
     * Actualiza el título de la BossBar
     */
    public void updateBossBarTitle(Player player, String cycleName) {
        BossBar bossBar = playerBossBars.get(player.getUniqueId());
        if (bossBar != null) {
            String title = "§b§lCiclo: §f" + cycleName;
            bossBar.setTitle(title);
        }
    }
    
    /**
     * Actualiza el progreso de la BossBar (0.0 a 1.0)
     */
    public void updateBossBarProgress(Player player, double progress) {
        BossBar bossBar = playerBossBars.get(player.getUniqueId());
        if (bossBar != null) {
            bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        }
    }
    
    /**
     * Remueve la BossBar de un jugador
     */
    public void removeBossBar(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancelar auto-hide task
        BukkitTask hideTask = autoHideTasks.remove(playerId);
        if (hideTask != null) {
            hideTask.cancel();
        }
        
        // Remover BossBar
        BossBar bossBar = playerBossBars.remove(playerId);
        if (bossBar != null) {
            bossBar.removePlayer(player);
            bossBar.removeAll();
        }
    }
    
    /**
     * Programa el auto-ocultamiento de la BossBar
     */
    private void scheduleAutoHide(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancelar task anterior si existe
        BukkitTask oldTask = autoHideTasks.get(playerId);
        if (oldTask != null) {
            oldTask.cancel();
        }
        
        // Programar nuevo auto-hide (10 segundos)
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            removeBossBar(player);
        }, 200L); // 10 segundos
        
        autoHideTasks.put(playerId, task);
    }
    
    /**
     * Muestra BossBar permanente (sin auto-hide)
     */
    public void showPermanentBossBar(Player player, String cycleName) {
        FileConfiguration config = plugin.getCicloConfig();
        
        if (!config.getBoolean("config.mostrar_bossbar", true)) {
            return;
        }
        
        removeBossBar(player);
        
        String colorStr = config.getString("config.bossbar_color", "BLUE");
        String styleStr = config.getString("config.bossbar_style", "PROGRESS");
        
        BarColor color = parseBarColor(colorStr);
        BarStyle style = parseBarStyle(styleStr);
        
        String title = "§b§lCiclo: §f" + cycleName;
        BossBar bossBar = Bukkit.createBossBar(title, color, style);
        bossBar.setProgress(1.0);
        bossBar.addPlayer(player);
        
        playerBossBars.put(player.getUniqueId(), bossBar);
        // No programar auto-hide
    }
    
    /**
     * Convierte string a BarColor
     */
    private BarColor parseBarColor(String colorStr) {
        try {
            return BarColor.valueOf(colorStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarColor.BLUE;
        }
    }
    
    /**
     * Convierte string a BarStyle
     */
    private BarStyle parseBarStyle(String styleStr) {
        try {
            return BarStyle.valueOf(styleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarStyle.SOLID;
        }
    }
    
    /**
     * Limpia todas las BossBars
     */
    public void removeAllBossBars() {
        // Cancelar todas las tasks
        autoHideTasks.values().forEach(BukkitTask::cancel);
        autoHideTasks.clear();
        
        // Remover todas las BossBars
        playerBossBars.values().forEach(bossBar -> {
            bossBar.removeAll();
        });
        playerBossBars.clear();
    }
    
    /**
     * Limpieza al desactivar
     */
    public void shutdown() {
        removeAllBossBars();
    }
}
