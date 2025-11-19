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

import net.kyori.adventure.text.Component;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.disaster.DisasterController;
import me.apocalipsis.events.EventBase;
import me.apocalipsis.missions.MissionRank;
import me.apocalipsis.missions.MissionService;
import me.apocalipsis.missions.RankService;
import me.apocalipsis.state.ServerState;
import me.apocalipsis.state.StateManager;

public class ScoreboardManager {

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTANTES
    // ═══════════════════════════════════════════════════════════════════════
    private static final int PROGRESS_BAR_SIZE = 10;
    private static final int MAX_MISSION_NAME_LENGTH = 15;
    private static final int UPDATE_INTERVAL_TICKS = 40; // 2 segundos
    private static final String SEPARATOR = "§8━━━━━━━━━━━━━";
    
    // Iconos Unicode
    private static final String ICON_RANK = "⚔";
    private static final String ICON_LEVEL = "★";
    private static final String ICON_STATE = "◈";
    private static final String ICON_DISASTER = "☠";
    private static final String ICON_TIME = "⏱";
    private static final String ICON_MISSIONS = "✎";
    private static final String ICON_ONLINE = "👥";
    private static final String ICON_EVENT = "✦";
    
    private final Apocalipsis plugin;
    private final StateManager stateManager;
    private final MissionService missionService;
    private final RankService rankService;

    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private final Map<UUID, String> lastContentCache = new HashMap<>(); // Cache para evitar spam de paquetes
    
    // Cache de misiones para optimización (Sprint 3)
    private final Map<UUID, Long> lastMissionUpdate = new HashMap<>();
    private final Map<UUID, String> cachedMissions = new HashMap<>();
    private static final long MISSION_CACHE_DURATION_MS = 5000; // 5 segundos
    
    private int taskId = -1;
    private int titleAnimationTick = 0;

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
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, UPDATE_INTERVAL_TICKS, UPDATE_INTERVAL_TICKS).getTaskId();
    }

    public void cancelTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public void updateAll() {
        // Incrementar contador para animación de título
        titleAnimationTick++;
        
        // [OPTIMIZACIÓN] Usar cache en lugar de Bukkit.getOnlinePlayers()
        for (Player player : plugin.getOnlinePlayersCache().getOnlinePlayers()) {
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
                getAnimatedTitle());
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            // Actualizar título con animación
            objective.displayName(getAnimatedTitle());
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
        int ps = missionService.getPlayerPs(player);
        String displayName = rankService.getTranslatedDisplayName(player);
        MissionRank currentRank = rankService.getRank(player);
        
        // Mostrar rango actual con PS y Nivel
        if (plugin.getExperienceService() != null) {
            int nivel = plugin.getExperienceService().getLevel(player);
            content.append("§7").append(ICON_RANK).append(" Rango: ").append(displayName).append(" §8(§e").append(ps).append(" PS §7| §b").append(nivel).append(" Nv§8)\n");
        } else {
            content.append("§7").append(ICON_RANK).append(" Rango: ").append(displayName).append(" §8(§e").append(ps).append(" PS§8)\n");
        }
        
        ServerState state = stateManager.getCurrentState();
        String stateDisplay = state.getDisplay();
        String disasterName = stateManager.getActiveDisasterId() != null 
            ? stateManager.getActiveDisasterId().toUpperCase().replace("_", " ") 
            : "§7Ninguno";
        
        content.append("§7").append(ICON_STATE).append(" Estado: §f").append(stateDisplay).append("\n");
        content.append("§7").append(ICON_DISASTER).append(" Desastre: §f").append(disasterName).append("\n");
        
        // Mostrar evento activo si existe
        String activeEvent = getActiveEventDisplay();
        if (activeEvent != null) {
            content.append("§7").append(ICON_EVENT).append(" Evento: ").append(activeEvent).append("\n");
        }
        
        if (state == ServerState.ACTIVO) {
            String timeMMSS = calculateTimeFromStateYml();
            content.append("§7").append(ICON_TIME).append(" Tiempo: §a").append(timeMMSS).append("\n");
        } else if (state == ServerState.PREPARACION) {
            boolean prepForzada = stateManager.isPrepForzada();
            
            if (prepForzada) {
                String timeMMSS = calculateTimeFromStateYml();
                content.append("§7").append(ICON_TIME).append(" Preparación: §e").append(timeMMSS).append("\n");
            } else {
                String cooldownMMSS = calculateCooldownFromStateYml();
                if (!cooldownMMSS.equals("00:00")) {
                    content.append("§7").append(ICON_TIME).append(" Cooldown: §e").append(cooldownMMSS).append("\n");
                } else {
                    int minJugadores = plugin.getConfigManager().getMinJugadores();
                    // [OPTIMIZACIÓN] Usar cache de contador
                    int online = plugin.getOnlinePlayersCache().getOnlineCount();
                    content.append("§7").append(ICON_TIME).append(" Cooldown: §a¡Listo!\n");
                    if (online < minJugadores) {
                        content.append("§7Esperando: §e").append(online).append("§7/§f").append(minJugadores).append(" jugadores\n");
                    }
                }
            }
        } else if (state == ServerState.DETENIDO) {
            String cooldownMMSS = calculateCooldownFromStateYml();
            if (!cooldownMMSS.equals("00:00")) {
                content.append("§7").append(ICON_TIME).append(" Cooldown: §e").append(cooldownMMSS).append("\n");
            }
        }
        
        content.append(SEPARATOR).append("\n"); // Separador visual
        
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
        
        content.append(SEPARATOR).append("\n"); // Separador visual
        
        // Misiones con sistema de caché (Sprint 3)
        content.append(getCachedMissionsDisplay(player));
        
        content.append(SEPARATOR).append("\n"); // Separador visual
        content.append("§7").append(ICON_ONLINE).append(" Online: §f").append(plugin.getOnlinePlayersCache().getOnlineCount()).append("\n");
        
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
                    int online = plugin.getOnlinePlayersCache().getOnlineCount();
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
        objective.getScore("§7Online: §f" + plugin.getOnlinePlayersCache().getOnlineCount()).setScore(line--);
    }

    private String buildProgressBar(double progress) {
        int filled = (int) Math.max(0, Math.min(PROGRESS_BAR_SIZE, progress * PROGRESS_BAR_SIZE));
        
        // Colores dinámicos según progreso
        String fillColor = progress >= 0.75 ? "§a" : (progress >= 0.50 ? "§e" : (progress >= 0.25 ? "§6" : "§c"));
        
        StringBuilder sb = new StringBuilder(32); // Pre-allocate capacity
        sb.append("§8[");
        
        // Usar append múltiple para mejor performance
        if (filled > 0) {
            sb.append(fillColor);
            for (int i = 0; i < filled; i++) sb.append('█');
        }
        if (filled < PROGRESS_BAR_SIZE) {
            sb.append("§7");
            for (int i = filled; i < PROGRESS_BAR_SIZE; i++) sb.append('█');
        }
        
        sb.append("§8]");
        return sb.toString();
    }

    public void clearPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        playerScoreboards.remove(uuid);
        lastContentCache.remove(uuid);
        // Limpiar caché de misiones (Sprint 3)
        lastMissionUpdate.remove(uuid);
        cachedMissions.remove(uuid);
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    public void clearAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearPlayer(player);
        }
        playerScoreboards.clear();
        lastContentCache.clear();
        // Limpiar caché de misiones (Sprint 3)
        lastMissionUpdate.clear();
        cachedMissions.clear();
    }

    /**
     * Genera el título animado del scoreboard alternando colores
     * @return Component con el título animado
     */
    private Component getAnimatedTitle() {
        // Cambiar color cada segundo (cada 20 ticks = 1 segundo)
        // titleAnimationTick se incrementa cada 2 segundos (40 ticks)
        // Por tanto, cada 2 ciclos cambia el color
        boolean useDarkRed = (titleAnimationTick % 2) == 0;
        String color = useDarkRed ? "§4" : "§c";
        return Component.text(color + "§lAPOCALIPSIS");
    }

    /**
     * Obtiene el nombre del evento activo para mostrar en el scoreboard
     * @return Nombre formateado del evento o null si no hay evento activo
     */
    private String getActiveEventDisplay() {
        if (plugin.getEventController() != null) {
            EventBase activeEvent = plugin.getEventController().getActiveEvent();
            if (activeEvent != null) {
                String eventName = activeEvent.getClass().getSimpleName();
                if (eventName.equals("EcoSombrasEvent")) {
                    return "§5§lEco de Sombras";
                } else if (eventName.equals("EcoBrasasEvent")) {
                    return "§6§lEco de Brasas";
                }
                return "§f" + eventName;
            }
        }
        return null;
    }

    /**
     * Obtiene el display de misiones con sistema de caché (Sprint 3)
     * Regenera el contenido solo si han pasado más de 5 segundos desde la última actualización
     * @param player Jugador para obtener sus misiones
     * @return String con el contenido formateado de misiones
     */
    private String getCachedMissionsDisplay(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        // Verificar si necesitamos regenerar el contenido
        Long lastUpdate = lastMissionUpdate.get(uuid);
        if (lastUpdate == null || (now - lastUpdate) >= MISSION_CACHE_DURATION_MS) {
            // Regenerar contenido de misiones
            StringBuilder missionContent = new StringBuilder();
            missionContent.append("§7").append(ICON_MISSIONS).append(" §e§lMisiones:\n");
            
            var assignments = missionService.getActiveAssignments(player);
            var incompletas = assignments.stream()
                .filter(a -> !a.isCompleted() && !a.isFailed())
                .filter(a -> a.getMission().getTipo().isEnabled())
                .limit(3)
                .toList();
            
            if (incompletas.isEmpty()) {
                missionContent.append("§a§lTodas completadas ✓\n");
            } else {
                for (var assignment : incompletas) {
                    String alias = assignment.getMission().getNombre();
                    if (alias.length() > MAX_MISSION_NAME_LENGTH) {
                        alias = alias.substring(0, MAX_MISSION_NAME_LENGTH) + "...";
                    }
                    missionContent.append("§7• §f").append(alias).append(" §8(")
                        .append(assignment.getProgress()).append("/")
                        .append(assignment.getMission().getCantidad()).append(")\n");
                }
            }
            
            int completed = missionService.getCompletedCount(player);
            int total = assignments.size();
            missionContent.append("§7Completadas: §a").append(completed).append("§7/§f").append(total).append("\n");
            
            // Guardar en caché
            String cached = missionContent.toString();
            cachedMissions.put(uuid, cached);
            lastMissionUpdate.put(uuid, now);
            return cached;
        } else {
            // Usar caché
            return cachedMissions.getOrDefault(uuid, "§7✎ §e§lMisiones:\n§a§lTodas completadas ✓\n");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // [FIX] Renderizado de línea de rango usando rangos.yml (display_name + scoreboard_color)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * [FIX] Renderiza la línea de rango en el Scoreboard usando display_name traducido
     * desde rangos.yml (el display_name ya incluye códigos de color)
     * Ahora incluye PS y Nivel en una sola línea compacta
     * @return nueva posición de línea (line - 1)
     */
    private int refreshRankLine(Player player, Objective objective, int line) {
        int ps = missionService.getPlayerPs(player);
        
        // Obtener display_name traducido desde rangos.yml (ya viene con colores)
        String displayName = rankService.getTranslatedDisplayName(player);
        
        // Renderizar línea de rango: "⚔ Rango: <display_name> (PS PS | Nivel Nv)"
        if (plugin.getExperienceService() != null) {
            int nivel = plugin.getExperienceService().getLevel(player);
            String rankLine = "§7" + ICON_RANK + " Rango: " + displayName + " §8(§e" + ps + " PS §7| §b" + nivel + " Nv§8)";
            objective.getScore(rankLine).setScore(line--);
        } else {
            String rankLine = "§7" + ICON_RANK + " Rango: " + displayName + " §8(§e" + ps + " PS§8)";
            objective.getScore(rankLine).setScore(line--);
        }
        
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
