package me.apocalipsis.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.disaster.DisasterController;
import me.apocalipsis.missions.MissionRank;
import me.apocalipsis.missions.MissionService;
import me.apocalipsis.missions.RankService;
import me.apocalipsis.state.ServerState;
import me.apocalipsis.state.StateManager;

public class ScoreboardManager {

    private final Apocalipsis plugin;
    private final StateManager stateManager;
    private final MissionService missionService;
    private final RankService rankService;

    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private final Map<UUID, String> lastContentCache = new HashMap<>(); // Cache para evitar spam de paquetes
    private int taskId = -1;

    public ScoreboardManager(Apocalipsis plugin, StateManager stateManager,
                            DisasterController disasterController, MissionService missionService,
                            RankService rankService) {
        this.plugin = plugin;
        this.stateManager = stateManager;
        this.missionService = missionService;
        this.rankService = rankService;
    }

    public void startTask() {
        // Actualizar cada 2 segundos (40 ticks) para reducir paquetes
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 40L, 40L).getTaskId();
    }

    public void cancelTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    public void updatePlayer(Player player) {
        // Generar contenido primero para comparar con caché
        String newContent = generateScoreboardContent(player);
        
        // Verificar caché para evitar spam de paquetes
        String lastContent = lastContentCache.get(player.getUniqueId());
        if (newContent.equals(lastContent)) {
            return; // No cambió, no enviar paquetes
        }
        
        // Actualizar caché
        lastContentCache.put(player.getUniqueId(), newContent);
        
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            playerScoreboards.put(player.getUniqueId(), scoreboard);
            player.setScoreboard(scoreboard);
        }

        Objective objective = scoreboard.getObjective("apocalipsis");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("apocalipsis", Criteria.DUMMY, 
                net.kyori.adventure.text.Component.text("§c§lAPOCALIPSIS"));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        // Limpiar entradas anteriores
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        // Construir líneas desde el contenido generado
        applyScoreboardContent(player, objective, newContent);
    }
    
    /**
     * Genera el contenido del scoreboard como String para comparar con caché
     */
    private String generateScoreboardContent(Player player) {
        StringBuilder content = new StringBuilder();
        
        // [FIX] Sistema de rangos con display_name traducido y scoreboard_color desde rangos.yml
        int xp = rankService.getXP(player);
        String displayName = rankService.getTranslatedDisplayName(player);
        MissionRank currentRank = rankService.getRank(player);
        
        // Mostrar rango actual con XP
        content.append("§7Rango: ").append(displayName).append(" §8(§7").append(xp).append(" XP§8)\n");
        
        // Sistema de XP y Nivel (muestra progreso de nivel, NO de rango)
        if (plugin.getExperienceService() != null) {
            int nivel = plugin.getExperienceService().getLevel(player);
            int xpForNext = plugin.getExperienceService().getXPForLevel(nivel + 1);
            int xpCurrent = plugin.getExperienceService().getXPForLevel(nivel);
            int xpProgress = xp - xpCurrent;
            int xpNeeded = xpForNext - xpCurrent;
            
            content.append("§7Nivel: §b").append(nivel).append(" §8(§7").append(xpProgress).append("/").append(xpNeeded).append(" XP§8)\n");
        }
        
        ServerState state = stateManager.getCurrentState();
        String stateDisplay = state.getDisplay();
        String disasterName = stateManager.getActiveDisasterId() != null 
            ? stateManager.getActiveDisasterId().toUpperCase().replace("_", " ") 
            : "§7Ninguno";
        
        content.append("§7Estado: §f").append(stateDisplay).append("\n");
        content.append("§7Desastre: §f").append(disasterName).append("\n");
        
        if (state == ServerState.ACTIVO) {
            String timeMMSS = calculateTimeFromStateYml();
            content.append("§7Tiempo: §a").append(timeMMSS).append("\n");
        } else if (state == ServerState.PREPARACION) {
            boolean prepForzada = stateManager.isPrepForzada();
            
            if (prepForzada) {
                String timeMMSS = calculateTimeFromStateYml();
                content.append("§7Preparación: §e").append(timeMMSS).append("\n");
            } else {
                String cooldownMMSS = calculateCooldownFromStateYml();
                if (!cooldownMMSS.equals("00:00")) {
                    content.append("§7Cooldown: §e").append(cooldownMMSS).append("\n");
                } else {
                    int minJugadores = plugin.getConfigManager().getMinJugadores();
                    int online = Bukkit.getOnlinePlayers().size();
                    content.append("§7Cooldown: §a¡Listo!\n");
                    if (online < minJugadores) {
                        content.append("§7Esperando: §e").append(online).append("§7/§f").append(minJugadores).append(" jugadores\n");
                    }
                }
            }
        } else if (state == ServerState.DETENIDO) {
            String cooldownMMSS = calculateCooldownFromStateYml();
            if (!cooldownMMSS.equals("00:00")) {
                content.append("§7Cooldown: §e").append(cooldownMMSS).append("\n");
            }
        }
        
        content.append(" \n"); // Línea vacía
        
        // Progreso de rango (basado en XP según rangos.yml)
        if (!rankService.isMaxRank(player)) {
            MissionRank nextRank = currentRank.getNext();
            if (nextRank != null) {
                int currentThreshold = currentRank.getXpRequired();
                int nextThreshold = nextRank.getXpRequired();
                int xpNeeded = nextThreshold - currentThreshold;
                int xpProgress = xp - currentThreshold;
                double progress = (double) xpProgress / xpNeeded;
                progress = Math.max(0.0, Math.min(1.0, progress));
                
                content.append("§7Próx. rango: ").append(nextRank.getDisplayName()).append("\n");
                content.append(buildProgressBar(progress)).append(" §7").append(xpProgress).append("/").append(xpNeeded).append(" XP\n");
            }
        } else {
            content.append("§6§l★ RANGO MÁXIMO ★\n");
        }
        
        content.append("  \n"); // Línea vacía
        
        // Misiones
        content.append("§e§lMisiones:\n");
        var assignments = missionService.getActiveAssignments(player);
        var incompletas = assignments.stream()
            .filter(a -> !a.isCompleted() && !a.isFailed())
            .filter(a -> a.getMission().getTipo().isEnabled())
            .limit(3)
            .toList();
        
        if (incompletas.isEmpty()) {
            content.append("§a§lTodas completadas ✓\n");
        } else {
            for (var assignment : incompletas) {
                String alias = assignment.getMission().getNombre();
                if (alias.length() > 18) alias = alias.substring(0, 15) + "...";
                content.append("§7• §f").append(alias).append(" §8(")
                    .append(assignment.getProgress()).append("/")
                    .append(assignment.getMission().getCantidad()).append(")\n");
            }
        }
        
        int completed = missionService.getCompletedCount(player);
        int total = assignments.size();
        content.append("§7Completadas: §a").append(completed).append("§7/§f").append(total).append("\n");
        
        content.append("   \n"); // Línea vacía
        content.append("§7Online: §f").append(Bukkit.getOnlinePlayers().size()).append("\n");
        
        return content.toString();
    }
    
    /**
     * Aplica el contenido generado al scoreboard
     */
    private void applyScoreboardContent(Player player, Objective objective, String content) {
        // Construir líneas
        int line = 15;
        
        // [FIX] Sistema de rangos con display_name traducido y scoreboard_color desde rangos.yml
        line = refreshRankLine(player, objective, line);
        
        ServerState state = stateManager.getCurrentState();
        String stateDisplay = state.getDisplay();
        String disasterName = stateManager.getActiveDisasterId() != null 
            ? stateManager.getActiveDisasterId().toUpperCase().replace("_", " ") 
            : "§7Ninguno";
        
        objective.getScore("§7Estado: §f" + stateDisplay).setScore(line--);
        objective.getScore("§7Desastre: §f" + disasterName).setScore(line--);
        
        if (state == ServerState.ACTIVO) {
            // [FIX] Leer tiempo desde state.yml (cero-drift)
            String timeMMSS = calculateTimeFromStateYml();
            objective.getScore("§7Tiempo: §a" + timeMMSS).setScore(line--);
        } else if (state == ServerState.PREPARACION) {
            // Verificar si es preparación forzada
            boolean prepForzada = stateManager.isPrepForzada();
            
            if (prepForzada) {
                // Mostrar tiempo de ventana forzada
                String timeMMSS = calculateTimeFromStateYml();
                objective.getScore("§7Preparación: §e" + timeMMSS).setScore(line--);
            } else {
                // Mostrar cooldown en preparación normal
                String cooldownMMSS = calculateCooldownFromStateYml();
                if (!cooldownMMSS.equals("00:00")) {
                    objective.getScore("§7Cooldown: §e" + cooldownMMSS).setScore(line--);
                } else {
                    // Cooldown cumplido - verificar si hay bloqueo por jugadores
                    int minJugadores = plugin.getConfigManager().getMinJugadores();
                    int online = Bukkit.getOnlinePlayers().size();
                    if (online < minJugadores) {
                        objective.getScore("§7Cooldown: §a¡Listo!").setScore(line--);
                        objective.getScore("§7Esperando: §e" + online + "§7/§f" + minJugadores + " jugadores").setScore(line--);
                    } else {
                        objective.getScore("§7Cooldown: §a¡Listo!").setScore(line--);
                    }
                }
            }
        } else if (state == ServerState.DETENIDO) {
            // [FIX] Mostrar cooldown cuando está detenido
            String cooldownMMSS = calculateCooldownFromStateYml();
            if (!cooldownMMSS.equals("00:00")) {
                objective.getScore("§7Cooldown: §e" + cooldownMMSS).setScore(line--);
            }
        }
        
        objective.getScore(" ").setScore(line--); // Línea vacía
        
        // Progreso de rango (solo si no es max rank)
        if (!rankService.isMaxRank(player)) {
            int xp = rankService.getXP(player);
            MissionRank currentRank = rankService.getRank(player);
            int nextThreshold = rankService.getNextRankThreshold(player);
            int currentThreshold = currentRank.getXpRequired();
            double progress = rankService.getProgressToNextRank(player);
            objective.getScore("§7Progreso de rango:").setScore(line--);
            String progressBar = buildProgressBar(progress);
            objective.getScore(progressBar + " §7" + (xp - currentThreshold) + "/" + (nextThreshold - currentThreshold) + " XP").setScore(line--);
        } else {
            objective.getScore("§6§l★ RANGO MÁXIMO ★").setScore(line--);
        }
        
        objective.getScore("  ").setScore(line--); // Línea vacía
        
        // Misiones - [FIX] Mostrar solo incompletas, máximo 3
        objective.getScore("§e§lMisiones:").setScore(line--);
        
        var assignments = missionService.getActiveAssignments(player);
        
        // Filtrar solo incompletas y tipos habilitados
        var incompletas = assignments.stream()
            .filter(a -> !a.isCompleted() && !a.isFailed())
            .filter(a -> a.getMission().getTipo().isEnabled())  // [REMOVAL] Excluir tipos deshabilitados
            .limit(3) // Máximo 3 visibles
            .toList();
        
        if (incompletas.isEmpty()) {
            objective.getScore("§a§lTodas completadas ✓").setScore(line--);
        } else {
            for (var assignment : incompletas) {
                String alias = assignment.getMission().getNombre();
                if (alias.length() > 18) alias = alias.substring(0, 15) + "...";
                objective.getScore("§7• §f" + alias + " §8(" + assignment.getProgress() + "/" + assignment.getMission().getCantidad() + ")").setScore(line--);
            }
        }
        
        int completed = missionService.getCompletedCount(player);
        int total = assignments.size();
        objective.getScore("§7Completadas: §a" + completed + "§7/§f" + total).setScore(line--);
        
        objective.getScore("   ").setScore(line--); // Línea vacía
        objective.getScore("§7Online: §f" + Bukkit.getOnlinePlayers().size()).setScore(line--);
    }

    private String buildProgressBar(double progress) {
        int bars = 20;
        int filled = (int) (progress * bars);
        filled = Math.min(bars, Math.max(0, filled));
        
        StringBuilder sb = new StringBuilder("§8[");
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                sb.append("§a|");
            } else {
                sb.append("§7|");
            }
        }
        sb.append("§8]");
        return sb.toString();
    }

    public void clearPlayer(Player player) {
        playerScoreboards.remove(player.getUniqueId());
        lastContentCache.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    public void clearAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearPlayer(player);
        }
        playerScoreboards.clear();
        lastContentCache.clear();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // [FIX] Renderizado de línea de rango usando rangos.yml (display_name + scoreboard_color)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * [FIX] Renderiza la línea de rango en el Scoreboard usando display_name traducido
     * desde rangos.yml (el display_name ya incluye códigos de color)
     * @return nueva posición de línea (line - 1)
     */
    private int refreshRankLine(Player player, Objective objective, int line) {
        int xp = rankService.getXP(player);
        
        // Obtener display_name traducido desde rangos.yml (ya viene con colores)
        String displayName = rankService.getTranslatedDisplayName(player);
        
        // Renderizar línea de rango: "Rango: <display_name> (XP XP)"
        String rankLine = "§7Rango: " + displayName + " §8(§7" + xp + " XP§8)";
        objective.getScore(rankLine).setScore(line--);
        
        return line;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // [FIX] Cálculo de tiempo desde state.yml (cero-drift)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Calcula el tiempo restante leyendo start_epoch_ms y end_epoch_ms desde state.yml
     * @return Tiempo en formato MM:SS
     */
    private String calculateTimeFromStateYml() {
        long startMs = stateManager.getLong("start_epoch_ms", 0L);
        long endMs = stateManager.getLong("end_epoch_ms", 0L);
        
        if (startMs <= 0 || endMs <= 0) {
            return "00:00";
        }
        
        long nowMs = System.currentTimeMillis();
        long remainingMs = endMs - nowMs;
        
        if (remainingMs <= 0) {
            return "00:00";
        }
        
        int totalSeconds = (int) (remainingMs / 1000L);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    /**
     * Calcula el cooldown restante leyendo last_end_epoch_ms desde state.yml
     * @return Cooldown en formato MM:SS
     */
    private String calculateCooldownFromStateYml() {
        long lastEndMs = stateManager.getLong("last_end_epoch_ms", 0L);
        
        if (lastEndMs <= 0) {
            return "00:00"; // No hay cooldown activo
        }
        
        // Obtener cooldown configurado en desastres.yml (usando ConfigManager)
        int cooldownSeconds = plugin.getConfigManager().getCooldownFinSegundos();
        long cooldownEndMs = lastEndMs + (cooldownSeconds * 1000L);
        
        long nowMs = System.currentTimeMillis();
        long remainingMs = cooldownEndMs - nowMs;
        
        if (remainingMs <= 0) {
            return "00:00";
        }
        
        int totalSeconds = (int) (remainingMs / 1000L);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        
        return String.format("%02d:%02d", minutes, seconds);
    }
}
