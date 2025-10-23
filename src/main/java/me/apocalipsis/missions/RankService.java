package me.apocalipsis.missions;

import me.apocalipsis.Apocalipsis;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class RankService {

    private final Apocalipsis plugin;
    private final MissionService missionService;
    private boolean umbralAcumulado = true;

    public RankService(Apocalipsis plugin, MissionService missionService) {
        this.plugin = plugin;
        this.missionService = missionService;
        loadRanksConfig();
    }

    /**
     * [RANGOS.YML] Carga la configuración de rangos desde rangos.yml
     */
    private void loadRanksConfig() {
        FileConfiguration config = plugin.getConfigManager().getRangosConfig();
        
        // Inicializar valores por defecto en todos los rangos
        for (MissionRank rank : MissionRank.values()) {
            rank.initDefaults();
        }
        
        // Cargar configuración global
        umbralAcumulado = config.getBoolean("umbral_acumulado", true);
        
        // Cargar datos de fallback visual
        ConfigurationSection visualDefaults = config.getConfigurationSection("visual_por_defecto");
        String defaultTabPrefix = visualDefaults != null ? visualDefaults.getString("tab_prefix", "") : "";
        String defaultTabSuffix = visualDefaults != null ? visualDefaults.getString("tab_suffix", "") : "";
        String defaultChatPrefix = visualDefaults != null ? visualDefaults.getString("chat_prefix", "") : "";
        String defaultScoreboardColor = visualDefaults != null ? visualDefaults.getString("scoreboard_color", "WHITE") : "WHITE";
        String defaultDisplayNameStyle = visualDefaults != null ? visualDefaults.getString("display_name_style", "&f%rango%") : "&f%rango%";
        
        // Cargar rangos desde configuración
        ConfigurationSection ranksSection = config.getConfigurationSection("ranks");
        if (ranksSection == null) {
            plugin.getLogger().warning("[RANGOS.YML] No se encontró sección 'ranks', usando valores por defecto");
            return;
        }
        
        int loaded = 0;
        int fallback = 0;
        
        for (String rankId : ranksSection.getKeys(false)) {
            try {
                // Verificar que el ID corresponde a un rango existente
                MissionRank rank = MissionRank.valueOf(rankId.toUpperCase());
                
                ConfigurationSection rankSection = ranksSection.getConfigurationSection(rankId);
                if (rankSection == null) continue;
                
                // Cargar valores (con fallback)
                String displayName = rankSection.getString("display_name");
                if (displayName == null) {
                    displayName = defaultDisplayNameStyle.replace("%rango%", rankId);
                    fallback++;
                }
                
                int psRequired = rankSection.getInt("ps_required", rank.getPsRequired());
                int misionesDiarias = rankSection.getInt("misiones_diarias", rank.getMisionesDiarias());
                String tabPrefix = rankSection.getString("tab_prefix", defaultTabPrefix);
                String tabSuffix = rankSection.getString("tab_suffix", defaultTabSuffix);
                String chatPrefix = rankSection.getString("chat_prefix", defaultChatPrefix);
                String scoreboardColor = rankSection.getString("scoreboard_color", defaultScoreboardColor);
                
                // Configurar el rango
                rank.configure(displayName, psRequired, misionesDiarias, tabPrefix, tabSuffix, chatPrefix, scoreboardColor);
                loaded++;
                
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[RANGOS.YML] ID '" + rankId + "' no corresponde a un rango existente; ignorado");
            }
        }
        
        plugin.getLogger().info("[RANGOS.YML] Cargados " + loaded + " rangos desde rangos.yml (" + fallback + " usando fallback)");
        plugin.getLogger().info("[RANGOS.YML] Modo: umbral_acumulado=" + umbralAcumulado);
    }

    /**
     * Recarga la configuración de rangos (llamado desde /avo reload si existe)
     */
    public void reloadRanksConfig() {
        plugin.getConfigManager().reload();
        loadRanksConfig();
        plugin.getLogger().info("[RANGOS.YML] Configuración de rangos recargada");
    }

    /**
     * Obtiene los PS actuales del jugador
     */
    public int getPS(Player player) {
        return missionService.getPlayerPs(player);
    }

    /**
     * Obtiene el rango actual del jugador según sus PS
     */
    public MissionRank getRank(Player player) {
        int ps = getPS(player);
        return MissionRank.fromPs(ps);
    }

    /**
     * Obtiene el umbral del siguiente rango
     * Si ya está en el rango máximo, devuelve el umbral del rango máximo
     */
    public int getNextRankThreshold(Player player) {
        MissionRank current = getRank(player);
        MissionRank next = current.getNext();
        
        if (next == null) {
            // Ya es el máximo, devolver valor muy alto
            return Integer.MAX_VALUE;
        }
        
        return next.getPsRequired();
    }

    /**
     * Verifica si el jugador está en el rango máximo
     */
    public boolean isMaxRank(Player player) {
        MissionRank current = getRank(player);
        return current == MissionRank.LEYENDA;
    }

    /**
     * Obtiene el progreso hacia el siguiente rango (0.0 a 1.0)
     */
    public double getProgressToNextRank(Player player) {
        if (isMaxRank(player)) {
            return 1.0;
        }

        MissionRank current = getRank(player);
        int ps = getPS(player);
        int currentMin = current.getPsRequired();
        int nextMin = getNextRankThreshold(player);

        if (nextMin <= currentMin) {
            return 1.0;
        }

        double progress = (double) (ps - currentMin) / (nextMin - currentMin);
        return Math.max(0.0, Math.min(1.0, progress));
    }

    /**
     * Obtiene el nombre corto del rango para UI compacta
     * Usa los primeros 3 caracteres del displayName (sin color codes)
     */
    public String getRankShort(Player player) {
        MissionRank rank = getRank(player);
        String displayName = rank.getDisplayName().replaceAll("§.", ""); // Quitar color codes
        return displayName.length() >= 3 ? displayName.substring(0, 3).toUpperCase() : displayName.toUpperCase();
    }

    /**
     * Obtiene el color del rango desde rangos.yml (extrae del displayName)
     */
    public String getRankColor(Player player) {
        MissionRank rank = getRank(player);
        String displayName = rank.getDisplayName();
        // Extraer el primer código de color (§X)
        if (displayName.length() >= 2 && displayName.charAt(0) == '§') {
            return displayName.substring(0, 2);
        }
        return "§f"; // Blanco por defecto
    }

    /**
     * Obtiene el nombre completo del rango (con colores)
     */
    public String getRankDisplayName(Player player) {
        MissionRank rank = getRank(player);
        return rank.getDisplayName();
    }

    /**
     * [FIX] Obtiene tab_prefix traducido desde rangos.yml
     */
    @SuppressWarnings("deprecation")
    public String getTabPrefix(Player player) {
        MissionRank rank = getRank(player);
        String raw = rank.getTabPrefix();
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', sanitize(raw));
    }

    /**
     * [FIX] Obtiene tab_suffix traducido desde rangos.yml
     */
    @SuppressWarnings("deprecation")
    public String getTabSuffix(Player player) {
        MissionRank rank = getRank(player);
        String raw = rank.getTabSuffix();
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', sanitize(raw));
    }

    /**
     * [FIX] Obtiene chat_prefix traducido desde rangos.yml
     */
    @SuppressWarnings("deprecation")
    public String getChatPrefix(Player player) {
        MissionRank rank = getRank(player);
        String raw = rank.getChatPrefix();
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', sanitize(raw));
    }

    /**
     * [FIX] Obtiene scoreboard_color traducido desde rangos.yml
     */
    @SuppressWarnings("deprecation")
    public org.bukkit.ChatColor getScoreboardColor(Player player) {
        MissionRank rank = getRank(player);
        String colorName = rank.getScoreboardColor();
        return safeChatColor(colorName, org.bukkit.ChatColor.WHITE);
    }

    /**
     * [FIX] Obtiene display_name traducido desde rangos.yml
     */
    @SuppressWarnings("deprecation")
    public String getTranslatedDisplayName(Player player) {
        MissionRank rank = getRank(player);
        String raw = rank.getDisplayName();
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', sanitize(raw));
    }

    /**
     * [FIX] Sanitiza prefijos/sufijos: trim y eliminar códigos duplicados
     */
    private String sanitize(String s) {
        if (s == null) return "";
        s = s.trim();
        // Eliminar dobles && que pueden aparecer por error
        while (s.contains("&&")) {
            s = s.replace("&&", "&");
        }
        return s;
    }

    /**
     * [FIX] Convierte nombre de color a ChatColor con fallback
     */
    @SuppressWarnings("deprecation")
    private org.bukkit.ChatColor safeChatColor(String name, org.bukkit.ChatColor def) {
        try {
            return org.bukkit.ChatColor.valueOf(name.toUpperCase(java.util.Locale.ROOT));
        } catch (Exception ex) {
            return def;
        }
    }
}
