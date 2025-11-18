package me.apocalipsis.ui;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import me.apocalipsis.Apocalipsis;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema de UI avanzado para eventos
 * 
 * Funcionalidades:
 * - Bossbars con Adventure API
 * - Scoreboards dinámicos
 * - Action bars globales y personalizados
 * - Indicadores direccionales
 * - Sistema de countdown con feedback
 * - Notificaciones con partículas
 * - Barras de progreso en chat
 */
public class UIManager {
    
    private final Apocalipsis plugin;
    
    // Bossbars Adventure API
    private final Map<UUID, BossBar> playerBossbars = new ConcurrentHashMap<>();
    private BossBar globalBossbar;
    
    // Scoreboards
    private final Map<UUID, Scoreboard> playerScoreboards = new ConcurrentHashMap<>();
    private org.bukkit.scoreboard.ScoreboardManager scoreboardManager;
    
    // Action bars
    private BukkitTask globalActionBarTask;
    private final Map<UUID, BukkitTask> playerActionBarTasks = new ConcurrentHashMap<>();
    
    // Tareas activas
    private final Map<UUID, BukkitTask> countdownTasks = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> indicatorTasks = new ConcurrentHashMap<>();
    
    public UIManager(Apocalipsis plugin) {
        this.plugin = plugin;
        this.scoreboardManager = Bukkit.getScoreboardManager();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // BOSSBAR SYSTEM (Adventure API)
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Actualiza la bossbar global para todos los jugadores
     */
    public void updateBossbar(String actName, float progress, BossBar.Color color) {
        if (globalBossbar == null) {
            globalBossbar = BossBar.bossBar(
                Component.text("⬢ " + actName + " ⬢"),
                progress,
                color,
                BossBar.Overlay.PROGRESS
            );
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showBossBar(globalBossbar);
            }
        } else {
            globalBossbar.name(Component.text("⬢ " + actName + " ⬢"));
            globalBossbar.progress(progress);
            globalBossbar.color(color);
        }
    }
    
    /**
     * Actualiza bossbar individual de un jugador
     */
    public void updatePlayerBossbar(Player player, String text, float progress, BossBar.Color color) {
        BossBar bar = playerBossbars.get(player.getUniqueId());
        
        if (bar == null) {
            bar = BossBar.bossBar(
                Component.text(text),
                progress,
                color,
                BossBar.Overlay.PROGRESS
            );
            player.showBossBar(bar);
            playerBossbars.put(player.getUniqueId(), bar);
        } else {
            bar.name(Component.text(text));
            bar.progress(progress);
            bar.color(color);
        }
    }
    
    /**
     * Oculta la bossbar global
     */
    public void hideGlobalBossbar() {
        if (globalBossbar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hideBossBar(globalBossbar);
            }
            globalBossbar = null;
        }
    }
    
    /**
     * Oculta bossbar de un jugador
     */
    public void hidePlayerBossbar(Player player) {
        BossBar bar = playerBossbars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ACTION BAR SYSTEM
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Muestra un action bar global pulsante
     */
    public void startGlobalActionBar(String message) {
        if (globalActionBarTask != null) {
            globalActionBarTask.cancel();
        }
        
        globalActionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                String coloredMessage = ticks % 20 < 10 ? "§e§l" + message : "§6§l" + message;
                
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendActionBar(coloredMessage);
                }
                
                ticks++;
            }
        }, 0L, 10L);
    }
    
    /**
     * Detiene el action bar global
     */
    public void stopGlobalActionBar() {
        if (globalActionBarTask != null) {
            globalActionBarTask.cancel();
            globalActionBarTask = null;
        }
    }
    
    /**
     * Muestra un action bar a un jugador específico
     */
    public void showPlayerActionBar(Player player, String message, int durationTicks) {
        BukkitTask existingTask = playerActionBarTasks.get(player.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        BukkitTask task = new BukkitRunnable() {
            int remaining = durationTicks;
            
            @Override
            public void run() {
                if (remaining <= 0 || !player.isOnline()) {
                    playerActionBarTasks.remove(player.getUniqueId());
                    cancel();
                    return;
                }
                
                player.sendActionBar(message);
                remaining -= 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
        
        playerActionBarTasks.put(player.getUniqueId(), task);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SCOREBOARD SYSTEM
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Muestra un scoreboard de objetivos
     */
    public void showObjectiveScoreboard(Player player, String title, Map<String, Integer> objectives) {
        Scoreboard board = scoreboardManager.getNewScoreboard();
        Objective obj = board.registerNewObjective("evento", Criteria.DUMMY, title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        int score = objectives.size();
        for (Map.Entry<String, Integer> entry : objectives.entrySet()) {
            String text = entry.getKey() + ": " + entry.getValue();
            obj.getScore(text).setScore(score--);
        }
        
        player.setScoreboard(board);
        playerScoreboards.put(player.getUniqueId(), board);
    }
    
    /**
     * Actualiza un valor en el scoreboard
     */
    public void updateObjectiveScoreboard(Player player, String key, int value) {
        Scoreboard board = playerScoreboards.get(player.getUniqueId());
        if (board == null) return;
        
        Objective obj = board.getObjective("evento");
        if (obj == null) return;
        
        // Borrar entradas antiguas con la misma key
        for (String entry : board.getEntries()) {
            if (entry.startsWith(key)) {
                board.resetScores(entry);
            }
        }
        
        // Agregar nueva entrada
        String text = key + ": " + value;
        obj.getScore(text).setScore(value);
    }
    
    /**
     * Oculta el scoreboard de un jugador
     */
    public void hideScoreboard(Player player) {
        player.setScoreboard(scoreboardManager.getNewScoreboard());
        playerScoreboards.remove(player.getUniqueId());
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // DIRECTIONAL INDICATORS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Muestra un indicador direccional hacia una ubicación
     */
    public void showDirectionalIndicator(Player player, Location target, int durationSeconds) {
        BukkitTask existingTask = indicatorTasks.get(player.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        BukkitTask task = new BukkitRunnable() {
            int ticksRemaining = durationSeconds * 20;
            
            @Override
            public void run() {
                if (ticksRemaining <= 0 || !player.isOnline()) {
                    indicatorTasks.remove(player.getUniqueId());
                    cancel();
                    return;
                }
                
                Location playerLoc = player.getLocation();
                org.bukkit.util.Vector direction = target.toVector().subtract(playerLoc.toVector()).normalize();
                Location indicatorLoc = playerLoc.clone().add(direction.multiply(3)).add(0, 2, 0);
                
                player.spawnParticle(Particle.END_ROD, indicatorLoc, 3, 0.1, 0.1, 0.1, 0);
                
                double distance = playerLoc.distance(target);
                String distanceStr = String.format("%.1f", distance);
                player.sendActionBar("§e⬢ " + distanceStr + "m §e⬢");
                
                ticksRemaining -= 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
        
        indicatorTasks.put(player.getUniqueId(), task);
    }
    
    /**
     * Detiene el indicador direccional de un jugador
     */
    public void stopDirectionalIndicator(Player player) {
        BukkitTask task = indicatorTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // COUNTDOWN SYSTEM
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Muestra una cuenta regresiva con feedback visual/sonoro
     */
    public void showCountdown(Player player, int seconds, String message, Runnable onComplete) {
        BukkitTask existingTask = countdownTasks.get(player.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        BukkitTask task = new BukkitRunnable() {
            int remaining = seconds;
            
            @Override
            public void run() {
                if (!player.isOnline()) {
                    countdownTasks.remove(player.getUniqueId());
                    cancel();
                    return;
                }
                
                if (remaining <= 0) {
                    countdownTasks.remove(player.getUniqueId());
                    cancel();
                    
                    if (onComplete != null) {
                        onComplete.run();
                    }
                    return;
                }
                
                String color = remaining <= 3 ? "§c" : remaining <= 10 ? "§e" : "§a";
                player.sendTitle(color + "§l" + remaining, "§7" + message, 0, 25, 5);
                
                float pitch = 1.0f + (0.1f * (seconds - remaining));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, pitch);
                
                if (remaining <= 3) {
                    player.spawnParticle(Particle.FLAME, player.getLocation().add(0, 2, 0), 10, 0.5, 0.5, 0.5, 0.01);
                }
                
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        
        countdownTasks.put(player.getUniqueId(), task);
    }
    
    /**
     * Cancela el countdown de un jugador
     */
    public void cancelCountdown(Player player) {
        BukkitTask task = countdownTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // NOTIFICATION SYSTEM
    // ═══════════════════════════════════════════════════════════════════
    
    public enum NotificationType {
        SUCCESS,
        WARNING,
        ERROR,
        INFO
    }
    
    /**
     * Envía una notificación estilizada
     */
    public void sendNotification(Player player, NotificationType type, String message) {
        String prefix;
        Sound sound;
        Particle particle;
        
        switch (type) {
            case SUCCESS:
                prefix = "§a✔ ";
                sound = Sound.ENTITY_PLAYER_LEVELUP;
                particle = Particle.HAPPY_VILLAGER;
                break;
            case WARNING:
                prefix = "§e⚠ ";
                sound = Sound.BLOCK_NOTE_BLOCK_BELL;
                particle = Particle.FLAME;
                break;
            case ERROR:
                prefix = "§c✗ ";
                sound = Sound.BLOCK_ANVIL_LAND;
                particle = Particle.SMOKE;
                break;
            case INFO:
            default:
                prefix = "§b⬢ ";
                sound = Sound.BLOCK_NOTE_BLOCK_CHIME;
                particle = Particle.END_ROD;
                break;
        }
        
        player.sendMessage(prefix + message);
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        player.spawnParticle(particle, player.getLocation().add(0, 2, 0), 5, 0.3, 0.3, 0.3, 0.01);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // PROGRESS BAR
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Envía una barra de progreso en el chat
     */
    public void sendProgressBar(Player player, String label, int current, int max) {
        int bars = 20;
        int filled = (int) ((double) current / max * bars);
        
        StringBuilder bar = new StringBuilder("§7[");
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                bar.append("§a█");
            } else {
                bar.append("§8█");
            }
        }
        bar.append("§7] ");
        
        int percent = (int) ((double) current / max * 100);
        bar.append("§e").append(percent).append("%");
        
        player.sendMessage("§7" + label + ": " + bar.toString());
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Limpia todos los elementos de UI de un jugador
     */
    public void cleanup(Player player) {
        hidePlayerBossbar(player);
        hideScoreboard(player);
        stopDirectionalIndicator(player);
        cancelCountdown(player);
        
        BukkitTask task = playerActionBarTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Limpia todos los elementos de UI
     */
    public void cleanupAll() {
        hideGlobalBossbar();
        stopGlobalActionBar();
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            cleanup(p);
        }
        
        playerBossbars.clear();
        playerScoreboards.clear();
        playerActionBarTasks.clear();
        countdownTasks.clear();
        indicatorTasks.clear();
    }
}
