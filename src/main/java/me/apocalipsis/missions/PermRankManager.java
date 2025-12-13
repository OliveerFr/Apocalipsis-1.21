package me.apocalipsis.missions;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import me.apocalipsis.Apocalipsis;

/**
 * Gestiona los rangos permanentes/personalizados independientes del sistema de XP
 */
public class PermRankManager {
    
    private final Apocalipsis plugin;
    private File configFile;
    private FileConfiguration config;
    
    // Cache de rangos permanentes definidos
    private final Map<String, PermRank> permRanks = new ConcurrentHashMap<>();
    
    // Cache de asignaciones de jugadores (UUID -> PermRankAssignment)
    private final Map<UUID, PermRankAssignment> playerAssignments = new ConcurrentHashMap<>();
    
    // Tarea de aplicación de efectos
    private int effectTaskId = -1;
    
    public PermRankManager(Apocalipsis plugin) {
        this.plugin = plugin;
        loadConfig();
        loadRanks();
        loadAssignments();
        startEffectTask();
    }
    
    /**
     * Carga la configuración del archivo
     */
    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "rangos_permanentes.yml");
        
        if (!configFile.exists()) {
            plugin.saveResource("rangos_permanentes.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    /**
     * Recarga la configuración
     */
    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        permRanks.clear();
        playerAssignments.clear();
        loadRanks();
        loadAssignments();
        
        plugin.getLogger().info("Rangos permanentes recargados: " + permRanks.size() + " rangos, " 
                               + playerAssignments.size() + " asignaciones");
    }
    
    /**
     * Carga todos los rangos permanentes desde el config
     */
    private void loadRanks() {
        ConfigurationSection ranksSection = config.getConfigurationSection("rangos");
        if (ranksSection == null) {
            plugin.getLogger().warning("No se encontró la sección 'rangos' en rangos_permanentes.yml");
            return;
        }
        
        for (String rankId : ranksSection.getKeys(false)) {
            ConfigurationSection rankSection = ranksSection.getConfigurationSection(rankId);
            if (rankSection == null) continue;
            
            PermRank rank = new PermRank(
                rankId,
                rankSection.getString("display_name", "§f" + rankId),
                rankSection.getString("tab_prefix", ""),
                rankSection.getString("tab_suffix", ""),
                rankSection.getString("chat_prefix", ""),
                rankSection.getString("color", "§f"),
                rankSection.getInt("prioridad", 0),
                rankSection.getBoolean("heredar_efectos_rango_normal", false)
            );
            
            // Cargar efectos
            ConfigurationSection effectsSection = rankSection.getConfigurationSection("efectos");
            if (effectsSection != null && effectsSection.getBoolean("enabled", false)) {
                List<String> effects = effectsSection.getStringList("potion_effects");
                rank.setPotionEffects(parseEffects(effects));
            }
            
            permRanks.put(rankId.toLowerCase(), rank);
        }
        
        plugin.getLogger().info("Cargados " + permRanks.size() + " rangos permanentes");
    }
    
    /**
     * Parsea efectos de poción desde strings como "SPEED:1"
     */
    private List<PotionEffect> parseEffects(List<String> effectStrings) {
        List<PotionEffect> effects = new ArrayList<>();
        int duration = config.getInt("configuracion.duracion_efectos_segundos", 15) * 20; // ticks
        
        for (String effectStr : effectStrings) {
            try {
                String[] parts = effectStr.split(":");
                PotionEffectType type = PotionEffectType.getByName(parts[0]);
                int amplifier = parts.length > 1 ? Integer.parseInt(parts[1]) - 1 : 0;
                
                if (type != null) {
                    effects.add(new PotionEffect(type, duration, amplifier, false, false));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error parseando efecto: " + effectStr);
            }
        }
        
        return effects;
    }
    
    /**
     * Carga asignaciones de rangos a jugadores
     */
    private void loadAssignments() {
        ConfigurationSection assignSection = config.getConfigurationSection("asignaciones");
        if (assignSection == null) return;
        
        long now = System.currentTimeMillis();
        
        for (String uuidStr : assignSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection playerSection = assignSection.getConfigurationSection(uuidStr);
                
                String rankId = playerSection.getString("rango");
                long assignedDate = playerSection.getLong("asignado_fecha");
                long expiresDate = playerSection.getLong("expira_fecha", 0);
                
                // Verificar si el rango existe
                if (!permRanks.containsKey(rankId.toLowerCase())) {
                    plugin.getLogger().warning("Rango no encontrado para " + uuidStr + ": " + rankId);
                    continue;
                }
                
                // Verificar si expiró
                if (expiresDate > 0 && now >= expiresDate) {
                    plugin.getLogger().info("Rango expirado para " + uuidStr);
                    continue;
                }
                
                PermRankAssignment assignment = new PermRankAssignment(
                    uuid, rankId.toLowerCase(), assignedDate, expiresDate
                );
                
                playerAssignments.put(uuid, assignment);
                
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("UUID inválido en asignaciones: " + uuidStr);
            }
        }
        
        plugin.getLogger().info("Cargadas " + playerAssignments.size() + " asignaciones de rangos permanentes");
    }
    
    /**
     * Guarda las asignaciones al archivo
     */
    public void saveAssignments() {
        // Limpiar sección de asignaciones
        config.set("asignaciones", null);
        
        for (Map.Entry<UUID, PermRankAssignment> entry : playerAssignments.entrySet()) {
            String path = "asignaciones." + entry.getKey().toString();
            PermRankAssignment assignment = entry.getValue();
            
            config.set(path + ".rango", assignment.getRankId());
            config.set(path + ".asignado_fecha", assignment.getAssignedDate());
            config.set(path + ".expira_fecha", assignment.getExpiresDate());
        }
        
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando rangos_permanentes.yml: " + e.getMessage());
        }
    }
    
    /**
     * Asigna un rango permanente a un jugador
     */
    public boolean assignRank(UUID playerUUID, String rankId, long durationMillis) {
        rankId = rankId.toLowerCase();
        
        if (!permRanks.containsKey(rankId)) {
            return false;
        }
        
        long now = System.currentTimeMillis();
        long expiresDate = durationMillis > 0 ? now + durationMillis : 0;
        
        PermRankAssignment assignment = new PermRankAssignment(
            playerUUID, rankId, now, expiresDate
        );
        
        playerAssignments.put(playerUUID, assignment);
        saveAssignments();
        
        // Actualizar tab si el jugador está online
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            updatePlayerTab(player);
        }
        
        return true;
    }
    
    /**
     * Remueve el rango permanente de un jugador
     */
    public boolean removeRank(UUID playerUUID) {
        if (playerAssignments.remove(playerUUID) != null) {
            saveAssignments();
            
            // Actualizar tab
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                updatePlayerTab(player);
            }
            
            return true;
        }
        return false;
    }
    
    /**
     * Obtiene el rango permanente de un jugador (si tiene)
     */
    public PermRank getPlayerPermRank(UUID playerUUID) {
        PermRankAssignment assignment = playerAssignments.get(playerUUID);
        if (assignment == null) return null;
        
        // Verificar expiración
        if (assignment.isExpired()) {
            playerAssignments.remove(playerUUID);
            saveAssignments();
            return null;
        }
        
        return permRanks.get(assignment.getRankId());
    }
    
    /**
     * Obtiene un rango por ID
     */
    public PermRank getRank(String rankId) {
        return permRanks.get(rankId.toLowerCase());
    }
    
    /**
     * Obtiene todos los IDs de rangos disponibles
     */
    public Set<String> getRankIds() {
        return new HashSet<>(permRanks.keySet());
    }
    
    /**
     * Crea un nuevo rango permanente
     */
    public boolean createRank(String rankId, String displayName, String tabPrefix, String tabSuffix,
                            String chatPrefix, String color, int priority, boolean inheritNormalEffects,
                            List<String> potionEffects) {
        rankId = rankId.toLowerCase();
        
        if (permRanks.containsKey(rankId)) {
            return false; // Ya existe
        }
        
        PermRank rank = new PermRank(rankId, displayName, tabPrefix, tabSuffix, chatPrefix, 
                                    color, priority, inheritNormalEffects);
        
        if (potionEffects != null && !potionEffects.isEmpty()) {
            rank.setPotionEffects(parseEffects(potionEffects));
        }
        
        permRanks.put(rankId, rank);
        
        // Guardar en config
        String path = "rangos." + rankId;
        config.set(path + ".display_name", displayName);
        config.set(path + ".tab_prefix", tabPrefix);
        config.set(path + ".tab_suffix", tabSuffix);
        config.set(path + ".chat_prefix", chatPrefix);
        config.set(path + ".color", color);
        config.set(path + ".prioridad", priority);
        config.set(path + ".heredar_efectos_rango_normal", inheritNormalEffects);
        
        if (potionEffects != null && !potionEffects.isEmpty()) {
            config.set(path + ".efectos.enabled", true);
            config.set(path + ".efectos.potion_effects", potionEffects);
        }
        
        try {
            config.save(configFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Error guardando nuevo rango: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Actualiza el tab list de un jugador
     */
    public void updatePlayerTab(Player player) {
        PermRank permRank = getPlayerPermRank(player.getUniqueId());
        
        if (permRank != null) {
            // Tiene rango permanente - usarlo
            String prefix = permRank.getTabPrefix();
            String suffix = permRank.getTabSuffix();
            
            if (config.getBoolean("configuracion.tab_format.compacto", true)) {
                // Formato compacto: sin espacios extra
                player.setPlayerListName(prefix + player.getName() + suffix);
            } else {
                player.setPlayerListName(prefix + " " + player.getName() + " " + suffix);
            }
        } else {
            // No tiene rango permanente - usar rango normal
            MissionRank normalRank = plugin.getRankService().getRank(player);
            String prefix = normalRank.getTabPrefix();
            
            if (config.getBoolean("configuracion.tab_format.compacto", true)) {
                // Formato compacto
                player.setPlayerListName(prefix + player.getName());
            } else {
                player.setPlayerListName(prefix + " " + player.getName());
            }
        }
    }
    
    /**
     * Inicia la tarea de aplicación de efectos
     */
    private void startEffectTask() {
        if (!config.getBoolean("configuracion.enabled", true)) {
            return;
        }
        
        int interval = config.getInt("configuracion.actualizar_efectos_ticks", 100);
        
        effectTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                applyEffects(player);
            }
        }, 20L, interval);
    }
    
    /**
     * Aplica efectos de poción a un jugador según su rango permanente
     */
    private void applyEffects(Player player) {
        PermRank permRank = getPlayerPermRank(player.getUniqueId());
        if (permRank == null) return;
        
        // Aplicar efectos del rango permanente
        for (PotionEffect effect : permRank.getPotionEffects()) {
            player.addPotionEffect(effect, true);
        }
        
        // Si debe heredar efectos del rango normal, aplicarlos también
        // (esto se podría implementar si los rangos normales tienen efectos)
    }
    
    /**
     * Detiene la tarea de efectos
     */
    public void shutdown() {
        if (effectTaskId != -1) {
            Bukkit.getScheduler().cancelTask(effectTaskId);
        }
        saveAssignments();
    }
    
    /**
     * Clase interna: Rango permanente
     */
    public static class PermRank {
        private final String id;
        private final String displayName;
        private final String tabPrefix;
        private final String tabSuffix;
        private final String chatPrefix;
        private final String color;
        private final int priority;
        private final boolean inheritNormalEffects;
        private List<PotionEffect> potionEffects = new ArrayList<>();
        
        public PermRank(String id, String displayName, String tabPrefix, String tabSuffix,
                       String chatPrefix, String color, int priority, boolean inheritNormalEffects) {
            this.id = id;
            this.displayName = displayName;
            this.tabPrefix = tabPrefix;
            this.tabSuffix = tabSuffix;
            this.chatPrefix = chatPrefix;
            this.color = color;
            this.priority = priority;
            this.inheritNormalEffects = inheritNormalEffects;
        }
        
        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getTabPrefix() { return tabPrefix; }
        public String getTabSuffix() { return tabSuffix; }
        public String getChatPrefix() { return chatPrefix; }
        public String getColor() { return color; }
        public int getPriority() { return priority; }
        public boolean isInheritNormalEffects() { return inheritNormalEffects; }
        public List<PotionEffect> getPotionEffects() { return potionEffects; }
        
        public void setPotionEffects(List<PotionEffect> effects) {
            this.potionEffects = effects;
        }
    }
    
    /**
     * Clase interna: Asignación de rango a jugador
     */
    public static class PermRankAssignment {
        private final UUID playerUUID;
        private final String rankId;
        private final long assignedDate;
        private final long expiresDate; // 0 = permanente
        
        public PermRankAssignment(UUID playerUUID, String rankId, long assignedDate, long expiresDate) {
            this.playerUUID = playerUUID;
            this.rankId = rankId;
            this.assignedDate = assignedDate;
            this.expiresDate = expiresDate;
        }
        
        public UUID getPlayerUUID() { return playerUUID; }
        public String getRankId() { return rankId; }
        public long getAssignedDate() { return assignedDate; }
        public long getExpiresDate() { return expiresDate; }
        
        public boolean isExpired() {
            return expiresDate > 0 && System.currentTimeMillis() >= expiresDate;
        }
        
        public boolean isPermanent() {
            return expiresDate == 0;
        }
    }
}
