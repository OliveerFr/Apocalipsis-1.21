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
        header.append("§c§lAPOCALIPSIS §8(La Firma)\n");
        header.append("§7Estado: §f").append(stateDisplay).append("  §8|  §7Desastre: §f").append(disasterName).append("\n");
        header.append("§7Día §f#").append(day).append("  §8|  §7Online: §a").append(online).append("§7/§f").append(max)
              .append("  §8|  §7TPS: ").append(tpsColor).append(String.format("%.1f", tps))
              .append("  §8|  §7Perf: §f").append(perfState);
        
        // Footer: tiempo + próximo rango
        StringBuilder footer = new StringBuilder();
        if (state == ServerState.ACTIVO) {
            // [FIX] Leer tiempo desde state.yml (cero-drift)
            String timeDisplay = calculateTimeFromStateYml();
            footer.append("\n§7Tiempo restante: §a").append(timeDisplay);
        } else if (state == ServerState.PREPARACION) {
            // Verificar si es preparación forzada
            boolean prepForzada = stateManager.isPrepForzada();
            
            if (prepForzada) {
                // Mostrar tiempo de ventana forzada
                String timeDisplay = calculateTimeFromStateYml();
                footer.append("\n§7Preparación: §e").append(timeDisplay);
            } else {
                // Mostrar cooldown en preparación normal
                String cooldownDisplay = calculateCooldownFromStateYml();
                if (!cooldownDisplay.equals("00:00")) {
                    footer.append("\n§7Cooldown: §e").append(cooldownDisplay);
                } else {
                    // Cooldown cumplido - verificar si hay bloqueo por jugadores
                    int minJugadores = plugin.getConfigManager().getMinJugadores();
                    if (online < minJugadores) {
                        footer.append("\n§7Cooldown: §a¡Listo! §8(§e").append(online).append("§7/§f").append(minJugadores).append(" jugadores§8)");
                    } else {
                        footer.append("\n§7Cooldown: §a¡Listo!");
                    }
                }
            }
        } else if (state == ServerState.DETENIDO) {
            // [FIX] Mostrar cooldown cuando está detenido
            String cooldownDisplay = calculateCooldownFromStateYml();
            if (!cooldownDisplay.equals("00:00")) {
                footer.append("\n§7Cooldown: §e").append(cooldownDisplay);
            } else {
                footer.append("\n§7Tiempo restante: §7---");
            }
        } else {
            footer.append("\n§7Tiempo restante: §7---");
        }
        
        if (!rankService.isMaxRank(player)) {
            int ps = rankService.getPS(player);
            int nextThreshold = rankService.getNextRankThreshold(player);
            footer.append("  §8|  §7Próx. rango: §a").append(ps).append("§7/§f").append(nextThreshold).append(" PS");
        } else {
            footer.append("  §8|  §6§l★ RANGO MÁXIMO ★");
        }
        
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
        
        // Rango
        if (!rankService.isMaxRank(player)) {
            int ps = rankService.getPS(player);
            int nextThreshold = rankService.getNextRankThreshold(player);
            content.append(ps).append("/").append(nextThreshold);
        } else {
            content.append("MAX");
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
     */
    public void applyTabPrefix(Player p) {
        // 1) Obtener rango y textos desde rangos.yml
        me.apocalipsis.missions.MissionRank rank = rankService.getRank(p);
        String rankId = rank.name().toLowerCase();
        String rawPrefix = rankService.getTabPrefix(p);
        String prefix = sanitize(rawPrefix);

        // 2) Componer PlayerListName (TAB) - esto lo ven TODOS
        // IMPORTANTE: usa el nombre real, no displayname
        String finalTab = (prefix == null || prefix.isEmpty()) ? p.getName() : prefix + p.getName();
        
        // [1.21+] Usar Component API con deserialización de códigos legacy
        Component tabComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(finalTab);
        p.playerListName(tabComponent);

        // 3) (Opcional) Teams para la etiqueta sobre la cabeza (no TAB)
        org.bukkit.scoreboard.Scoreboard board = getPluginMainBoard();
        org.bukkit.scoreboard.Team team = ensureRankTeam(board, "rank_" + rankId, prefix);
        removeFromOtherRankTeams(board, p.getName(), "rank_");
        team.addEntry(p.getName());
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
