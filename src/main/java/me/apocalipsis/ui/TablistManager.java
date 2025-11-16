package me.apocalipsis.ui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.disaster.adapters.PerformanceAdapter;
import me.apocalipsis.missions.RankService;
import me.apocalipsis.state.ServerState;
import me.apocalipsis.state.StateManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class TablistManager {

    private final Apocalipsis plugin;
    private final StateManager stateManager;
    private final PerformanceAdapter performanceAdapter;
    private final RankService rankService;
    private final java.util.Map<java.util.UUID, String> lastTabCache = new java.util.HashMap<>(); // Cache para evitar spam
    private int taskId = -1;

    public TablistManager(Apocalipsis plugin, StateManager stateManager,
                         PerformanceAdapter performanceAdapter, RankService rankService) {
        this.plugin = plugin;
        this.stateManager = stateManager;
        this.performanceAdapter = performanceAdapter;
        this.rankService = rankService;
    }

    public void startTask() {
        // Actualizar cada 3 segundos (60 ticks) para reducir paquetes
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 60L, 60L).getTaskId();
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
        // Generar contenido completo
        String newContent = generateTabContent(player);
        
        // Verificar caché para evitar spam de paquetes
        String lastContent = lastTabCache.get(player.getUniqueId());
        if (newContent.equals(lastContent)) {
            return; // No cambió, no enviar paquetes
        }
        
        // Actualizar caché
        lastTabCache.put(player.getUniqueId(), newContent);
        
        // Header: 3 líneas (sin "Jugador: ... Rango: ...")
        ServerState state = stateManager.getCurrentState();
        String stateDisplay = state.getDisplay();
        
        String disasterName = "§7Ninguno";
        if (stateManager.getActiveDisasterId() != null) {
            disasterName = "§c" + stateManager.getActiveDisasterId().toUpperCase().replace("_", " ");
        }
        
        int day = stateManager.getCurrentDay();
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        double tps = performanceAdapter.getLastTPS();
        String tpsColor = tps >= 18.0 ? "§a" : (tps >= 14.0 ? "§e" : "§c");
        String perfState = getPerformanceStateDisplay();
        
        StringBuilder header = new StringBuilder();
        
        // Header compacto y limpio - 2 líneas
        header.append("\n§c§l▸ APOCALIPSIS §8§l| §7Día §f").append(day)
              .append(" §8| §7Players §b").append(online).append("§7/§f").append(max)
              .append(" §8| §7TPS ").append(tpsColor).append(String.format("%.1f", tps));
        
        // Línea 2: Estado y desastre (solo si hay desastre activo)
        header.append("\n§8▸ ").append(stateDisplay);
        if (stateManager.getActiveDisasterId() != null) {
            header.append(" §8| §c⚠ ").append(disasterName);
        }
        header.append("\n");
        
        // Footer: diseño limpio y compacto
        StringBuilder footer = new StringBuilder();
        footer.append("\n");
        
        // Línea 1: Tiempo según estado (compacto)
        if (state == ServerState.ACTIVO) {
            String timeDisplay = calculateTimeFromStateYml();
            footer.append("§a▸ §7Tiempo: §f").append(timeDisplay);
        } else if (state == ServerState.PREPARACION) {
            boolean prepForzada = stateManager.isPrepForzada();
            
            if (prepForzada) {
                String timeDisplay = calculateTimeFromStateYml();
                footer.append("§e▸ §7Preparación: §f").append(timeDisplay);
            } else {
                String cooldownDisplay = calculateCooldownFromStateYml();
                if (!cooldownDisplay.equals("00:00")) {
                    footer.append("§e▸ §7Cooldown: §f").append(cooldownDisplay);
                } else {
                    int minJugadores = plugin.getConfigManager().getMinJugadores();
                    footer.append("§a▸ §7Listo §8(").append(online).append("§7/§f").append(minJugadores).append("§8)");
                }
            }
        } else if (state == ServerState.DETENIDO) {
            footer.append("§7▸ §7Detenido");
        } else {
            footer.append("§8▸ §7---");
        }
        
        footer.append("\n");
        
        // Línea 2-3: Rango y XP
        String rankDisplay = rankService.getTabPrefix(player);
        
        if (plugin.getExperienceService() != null) {
            int currentLevel = plugin.getExperienceService().getLevel(player);
            int currentXP = plugin.getExperienceService().getXP(player);
            
            if (!rankService.isMaxRank(player)) {
                int xpNeeded = plugin.getExperienceService().getXPForNextLevel(player);
                double percentage = (double) currentXP / xpNeeded * 100;
                String progressBar = generateProgressBar(percentage, 12);  // Barra más corta
                
                footer.append(rankDisplay).append(" §8| §7Nivel §b").append(currentLevel)
                      .append(" §8(").append(String.format("%.0f", percentage)).append("%§8)");
                footer.append("\n").append(progressBar);
                footer.append(" §7").append(currentXP).append("§8/§f").append(xpNeeded);
            } else {
                footer.append(rankDisplay).append(" §8| §6★ MÁXIMO");
                footer.append("\n§7Nivel: §6§l").append(currentLevel);
            }
        } else {
            footer.append(rankDisplay);
            if (!rankService.isMaxRank(player)) {
                int xp = rankService.getXP(player);
                int nextThreshold = rankService.getNextRankThreshold(player);
                footer.append("\n§7XP: §a").append(xp).append("§7/§f").append(nextThreshold);
            } else {
                footer.append(" §8| §6★ MÁXIMO");
            }
        }
        
        footer.append("\n");
        
        player.sendPlayerListHeaderAndFooter(Component.text(header.toString()), Component.text(footer.toString()));
        
        // [FIX DEFINITIVO] Aplicar prefijo de rango en TAB usando setPlayerListName
        applyTabPrefix(player);
    }

    private String getPerformanceStateDisplay() {
        return switch (performanceAdapter.getCurrentState()) {
            case NORMAL -> "§aNORMAL";
            case DEGRADED -> "§eDEGRADED";
            case CRITICAL -> "§cCRITICAL";
            case SAFE_MODE -> "§4SAFE MODE";
        };
    }
    
    /**
     * Genera el contenido del tablist como String para comparar con caché
     */
    private String generateTabContent(Player player) {
        StringBuilder content = new StringBuilder();
        
        ServerState state = stateManager.getCurrentState();
        String stateDisplay = state.getDisplay();
        
        String disasterName = "§7Ninguno";
        if (stateManager.getActiveDisasterId() != null) {
            disasterName = "§c" + stateManager.getActiveDisasterId().toUpperCase().replace("_", " ");
        }
        
        int day = stateManager.getCurrentDay();
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        double tps = performanceAdapter.getLastTPS();
        String tpsColor = tps >= 18.0 ? "§a" : (tps >= 14.0 ? "§e" : "§c");
        String perfState = getPerformanceStateDisplay();
        
        content.append(stateDisplay).append("|");
        content.append(disasterName).append("|");
        content.append(day).append("|");
        content.append(online).append("/").append(max).append("|");
        content.append(tpsColor).append(String.format("%.1f", tps)).append("|");
        content.append(perfState).append("|");
        
        // Tiempo/cooldown
        if (state == ServerState.ACTIVO) {
            content.append(calculateTimeFromStateYml()).append("|");
        } else if (state == ServerState.PREPARACION) {
            boolean prepForzada = stateManager.isPrepForzada();
            if (prepForzada) {
                content.append("PREP:").append(calculateTimeFromStateYml()).append("|");
            } else {
                content.append("CD:").append(calculateCooldownFromStateYml()).append("|");
            }
        } else if (state == ServerState.DETENIDO) {
            content.append("DT:").append(calculateCooldownFromStateYml()).append("|");
        }
        
        // [FIX] Usar nivel y XP en lugar de PS en caché
        if (plugin.getExperienceService() != null) {
            int currentLevel = plugin.getExperienceService().getLevel(player);
            int currentXP = plugin.getExperienceService().getXP(player);
            
            content.append("LVL:").append(currentLevel).append("|XP:").append(currentXP);
            
            if (!rankService.isMaxRank(player)) {
                int xpNeeded = plugin.getExperienceService().getXPForNextLevel(player);
                content.append("/").append(xpNeeded);
            } else {
                content.append("|MAX");
            }
        } else {
            // Fallback
            if (!rankService.isMaxRank(player)) {
                int xp = rankService.getXP(player);
                int nextThreshold = rankService.getNextRankThreshold(player);
                content.append(xp).append("/").append(nextThreshold);
            } else {
                content.append("MAX");
            }
        }
        
        return content.toString();
    }

    public void clearPlayer(Player player) {
        lastTabCache.remove(player.getUniqueId());
        player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
    }

    public void clearAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearPlayer(player);
        }
        lastTabCache.clear();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // [FIX DEFINITIVO] Sistema TAB con setPlayerListName + Teams para name tag
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * [FIX] Aplica el prefijo de rango en TAB visible para TODOS
     * Se llama en join, rankUp y reload
     * [MEJORA] Ordena jugadores por rango usando prefijos numéricos
     */
    public void applyTabPrefix(Player p) {
        // 1) Obtener rango y textos desde rangos.yml
        me.apocalipsis.missions.MissionRank rank = rankService.getRank(p);
        String rawPrefix = rankService.getTabPrefix(p);
        String prefix = sanitize(rawPrefix);

        // 2) Componer PlayerListName (TAB) - esto lo ven TODOS
        // IMPORTANTE: usa el nombre real, no displayname
        String finalTab = (prefix == null || prefix.isEmpty()) ? p.getName() : prefix + p.getName();
        
        // [1.21+] Usar Component API con deserialización de códigos legacy
        Component tabComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(finalTab);
        p.playerListName(tabComponent);

        // 3) Teams para ordenar en TAB y etiqueta sobre la cabeza
        // [MEJORA] Usar prefijos numéricos para ordenar: 1=LEYENDA, 2=VETERANO, ..., 5=NOVATO
        String teamName = getRankedTeamName(rank);
        org.bukkit.scoreboard.Scoreboard board = getPluginMainBoard();
        org.bukkit.scoreboard.Team team = ensureRankTeam(board, teamName, prefix);
        removeFromOtherRankTeams(board, p.getName(), "rank_");
        team.addEntry(p.getName());
    }
    
    /**
     * [NUEVO] Genera nombre de team con prefijo numérico para ordenar por rango
     * ABSOLUTO (01) aparece primero, NOVATO (08) aparece último
     * Usa 0X para mantener orden alfabético correcto
     */
    private String getRankedTeamName(me.apocalipsis.missions.MissionRank rank) {
        return switch (rank) {
            case ABSOLUTO -> "01_absoluto";
            case TITAN -> "02_titan";
            case MAESTRO -> "03_maestro";
            case LEYENDA -> "04_leyenda";
            case VETERANO -> "05_veterano";
            case SOBREVIVIENTE -> "06_sobreviviente";
            case EXPLORADOR -> "07_explorador";
            case NOVATO -> "08_novato";
        };
    }

    private String sanitize(String s) {
        if (s == null) return "";
        s = s.trim().replace("&&","&");
        return s;
    }

    /**
     * [FIX] Devuelve siempre el mismo scoreboard compartido
     */
    private org.bukkit.scoreboard.Scoreboard getPluginMainBoard() {
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }

    /**
     * [FIX] Crea o actualiza un team de rango con el prefix dado (solo para name tag)
     */
    private org.bukkit.scoreboard.Team ensureRankTeam(org.bukkit.scoreboard.Scoreboard scoreboard, 
                                                      String teamName, String prefix) {
        org.bukkit.scoreboard.Team team = scoreboard.getTeam(teamName);
        
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        
        // [1.21+] Establecer prefix con Component API (name tag sobre la cabeza)
        Component prefixComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix);
        team.prefix(prefixComponent);
        
        return team;
    }

    /**
     * [FIX] Quita al jugador de todos los teams de rango excepto el actual
     */
    private void removeFromOtherRankTeams(org.bukkit.scoreboard.Scoreboard scoreboard, 
                                          String playerName, String rankTeamPrefix) {
        for (org.bukkit.scoreboard.Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith(rankTeamPrefix) && team.hasEntry(playerName)) {
                team.removeEntry(playerName);
            }
        }
    }
    
    /**
     * [FIX] Forzar que todos los jugadores online usen el mismo scoreboard
     * Llamar en join y reload
     */
    public void forceSharedScoreboard() {
        org.bukkit.scoreboard.Scoreboard mainBoard = getPluginMainBoard();
        for (Player on : Bukkit.getOnlinePlayers()) {
            on.setScoreboard(mainBoard);
            applyTabPrefix(on);
        }
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
     * Genera una barra de progreso visual para XP
     * @param percentage Porcentaje completado (0-100)
     * @param length Longitud de la barra en caracteres
     * @return Barra de progreso coloreada
     */
    private String generateProgressBar(double percentage, int length) {
        int filled = (int) (percentage / 100.0 * length);
        int empty = length - filled;
        
        StringBuilder bar = new StringBuilder("§8[");
        
        // Determinar color según progreso
        String barColor;
        if (percentage >= 75) {
            barColor = "§a"; // Verde
        } else if (percentage >= 50) {
            barColor = "§e"; // Amarillo
        } else if (percentage >= 25) {
            barColor = "§6"; // Naranja
        } else {
            barColor = "§c"; // Rojo
        }
        
        // Construir barra
        for (int i = 0; i < filled; i++) {
            bar.append(barColor).append("█");
        }
        for (int i = 0; i < empty; i++) {
            bar.append("§7§m "); // Gris tenue
        }
        
        bar.append("§8]");
        return bar.toString();
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
