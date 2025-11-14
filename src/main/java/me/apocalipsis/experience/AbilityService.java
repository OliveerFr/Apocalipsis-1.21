package me.apocalipsis.experience;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.missions.MissionRank;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Servicio que aplica habilidades pasivas basadas en el rango del jugador.
 * Las habilidades se renuevan automáticamente y persisten entre sesiones.
 */
public class AbilityService {
    
    private final Apocalipsis plugin;
    private int taskId = -1;
    
    // Caché de habilidades por rango
    private final Map<MissionRank, List<RankAbility>> abilitiesByRank = new HashMap<>();
    
    // Configuración
    private int intervaloRenovacion = 600; // 30 segundos
    private int duracionEfecto = 1200; // 60 segundos
    private boolean ocultarParticulas = false;
    private boolean notificarAplicacion = true;
    
    public AbilityService(Apocalipsis plugin) {
        this.plugin = plugin;
        loadAbilities();
        startTask();
    }
    
    /**
     * Carga las habilidades desde recompensas.yml
     */
    public void loadAbilities() {
        abilitiesByRank.clear();
        
        FileConfiguration config = plugin.getConfigManager().getRecompensasConfig();
        
        // Cargar configuración general
        intervaloRenovacion = config.getInt("habilidades_config.intervalo_renovacion", 600);
        duracionEfecto = config.getInt("habilidades_config.duracion_efecto", 1200);
        ocultarParticulas = config.getBoolean("habilidades_config.ocultar_particulas", false);
        notificarAplicacion = config.getBoolean("habilidades_config.notificar_aplicacion", true);
        
        // Cargar habilidades por rango
        ConfigurationSection rankSection = config.getConfigurationSection("habilidades_por_rango");
        if (rankSection == null) {
            plugin.getLogger().warning("[Abilities] No se encontró sección 'habilidades_por_rango' en recompensas.yml");
            return;
        }
        
        for (MissionRank rank : MissionRank.values()) {
            String rankKey = rank.name();
            ConfigurationSection section = rankSection.getConfigurationSection(rankKey);
            
            if (section == null) continue;
            
            List<RankAbility> abilities = new ArrayList<>();
            List<?> rawList = section.getList("habilidades");
            
            if (rawList != null) {
                for (Object obj : rawList) {
                    if (obj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> abilityMap = (Map<String, Object>) obj;
                        
                        String type = (String) abilityMap.get("type");
                        int level = ((Number) abilityMap.getOrDefault("level", 1)).intValue();
                        String description = (String) abilityMap.getOrDefault("descripcion", "");
                        
                        PotionEffectType effectType = PotionEffectType.getByName(type);
                        if (effectType != null) {
                            abilities.add(new RankAbility(effectType, level, description));
                        } else {
                            plugin.getLogger().warning("[Abilities] Tipo de efecto desconocido: " + type);
                        }
                    }
                }
            }
            
            abilitiesByRank.put(rank, abilities);
        }
        
        plugin.getLogger().info("[Abilities] Habilidades cargadas para " + abilitiesByRank.size() + " rangos");
    }
    
    /**
     * Inicia el task de renovación de habilidades
     */
    /**
     * Inicia el task de renovación de habilidades
     */
    public void startTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                applyAbilities(player, false);
            }
        }, intervaloRenovacion, intervaloRenovacion).getTaskId();
        
        plugin.getLogger().info("[Abilities] Task de renovación iniciado (cada " + (intervaloRenovacion / 20) + "s)");
    }
    
    /**
     * Detiene el task de renovación
     */
    public void stopTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }
    
    /**
     * Aplica las habilidades del rango actual a un jugador
     * @param notify Si debe notificar al jugador
     */
    public void applyAbilities(Player player, boolean notify) {
        MissionRank rank = plugin.getRankService().getRank(player);
        List<RankAbility> abilities = abilitiesByRank.get(rank);
        
        // Debug log
        plugin.getLogger().info("[Abilities] Aplicando habilidades para " + player.getName() + 
            " - Rango: " + rank.name() + " - Habilidades: " + (abilities != null ? abilities.size() : 0));
        
        if (abilities == null || abilities.isEmpty()) {
            return;
        }
        
        // Aplicar cada habilidad
        for (RankAbility ability : abilities) {
            PotionEffect effect = new PotionEffect(
                ability.getType(),
                duracionEfecto,
                ability.getLevel() - 1, // Minecraft usa nivel 0-indexed
                true, // ambient
                !ocultarParticulas, // particles
                true  // icon
            );
            
            player.addPotionEffect(effect, true);
        }
        
        // Notificar si es necesario
        if (notify && notificarAplicacion) {
            FileConfiguration config = plugin.getConfigManager().getRecompensasConfig();
            String message = config.getString("habilidades_config.mensaje_aplicacion", 
                "&aHabilidades de rango &e%rango% &aaplicadas");
            message = message.replace("%rango%", rank.getDisplayName());
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
        }
    }
    
    /**
     * Elimina todas las habilidades de un jugador
     */
    public void removeAbilities(Player player) {
        for (List<RankAbility> abilities : abilitiesByRank.values()) {
            for (RankAbility ability : abilities) {
                player.removePotionEffect(ability.getType());
            }
        }
    }
    
    /**
     * Obtiene la lista de habilidades del rango actual del jugador
     */
    public List<RankAbility> getPlayerAbilities(Player player) {
        MissionRank rank = plugin.getRankService().getRank(player);
        return abilitiesByRank.getOrDefault(rank, new ArrayList<>());
    }
    
    /**
     * Obtiene todas las habilidades de un rango específico
     */
    public List<RankAbility> getRankAbilities(MissionRank rank) {
        return abilitiesByRank.getOrDefault(rank, new ArrayList<>());
    }
    
    /**
     * Recarga las habilidades desde la configuración
     */
    public void reload() {
        stopTask();
        loadAbilities();
        startTask();
        
        // Re-aplicar a todos los jugadores online
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyAbilities(player, false);
        }
    }
    
    /**
     * Clase que representa una habilidad de rango
     */
    public static class RankAbility {
        private final PotionEffectType type;
        private final int level;
        private final String description;
        
        public RankAbility(PotionEffectType type, int level, String description) {
            this.type = type;
            this.level = level;
            this.description = description;
        }
        
        public PotionEffectType getType() {
            return type;
        }
        
        public int getLevel() {
            return level;
        }
        
        public String getDescription() {
            return org.bukkit.ChatColor.translateAlternateColorCodes('&', description);
        }
        
        public String getFormattedName() {
            return type.getName() + " " + getRomanNumeral(level);
        }
        
        private String getRomanNumeral(int number) {
            switch (number) {
                case 1: return "I";
                case 2: return "II";
                case 3: return "III";
                case 4: return "IV";
                case 5: return "V";
                case 10: return "X";
                default: return String.valueOf(number);
            }
        }
    }
}
