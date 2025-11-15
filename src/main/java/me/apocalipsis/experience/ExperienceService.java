package me.apocalipsis.experience;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.missions.MissionRank;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio de experiencia y niveles independiente del sistema de misiones.
 * Maneja XP de múltiples fuentes y calcula niveles automáticamente.
 */
public class ExperienceService {
    
    private final Apocalipsis plugin;
    private final File dataFile;
    private final Map<UUID, PlayerExperienceData> playerData = new HashMap<>();
    
    // Configuración de XP
    private int nivelInicial = 100;
    private int multiplicador = 50;
    
    // Cooldowns para evitar spam
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    
    public ExperienceService(Apocalipsis plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "experience_data.yml");
        loadConfig();
        loadData();
    }
    
    /**
     * Carga la configuración desde recompensas.yml
     */
    private void loadConfig() {
        FileConfiguration config = plugin.getConfigManager().getRecompensasConfig();
        nivelInicial = config.getInt("experiencia.nivel_inicial", 100);
        multiplicador = config.getInt("experiencia.multiplicador", 50);
    }
    
    /**
     * Carga los datos de experiencia de los jugadores
     */
    private void loadData() {
        if (!dataFile.exists()) {
            return;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection playersSection = config.getConfigurationSection("players");
        
        if (playersSection == null) return;
        
        for (String uuidStr : playersSection.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            int xp = playersSection.getInt(uuidStr + ".xp", 0);
            int nivel = playersSection.getInt(uuidStr + ".nivel", 1);
            
            playerData.put(uuid, new PlayerExperienceData(xp, nivel));
        }
    }
    
    /**
     * Guarda los datos de experiencia
     */
    public void saveData() {
        FileConfiguration config = new YamlConfiguration();
        
        for (Map.Entry<UUID, PlayerExperienceData> entry : playerData.entrySet()) {
            String path = "players." + entry.getKey().toString();
            config.set(path + ".xp", entry.getValue().getXp());
            config.set(path + ".nivel", entry.getValue().getNivel());
        }
        
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[XP] Error guardando experience_data.yml: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene o crea los datos de experiencia de un jugador
     */
    private PlayerExperienceData getData(UUID uuid) {
        return playerData.computeIfAbsent(uuid, k -> new PlayerExperienceData(0, 1));
    }
    
    /**
     * Obtiene el nivel actual de un jugador
     */
    public int getLevel(Player player) {
        return getData(player.getUniqueId()).getNivel();
    }
    
    /**
     * Obtiene la XP actual de un jugador
     */
    public int getXP(Player player) {
        return getData(player.getUniqueId()).getXp();
    }
    
    /**
     * Calcula la XP necesaria para alcanzar un nivel específico
     */
    public int getXPForLevel(int nivel) {
        if (nivel <= 1) return 0;
        
        int totalXP = 0;
        for (int i = 2; i <= nivel; i++) {
            totalXP += nivelInicial + ((i - 2) * multiplicador);
        }
        return totalXP;
    }
    
    /**
     * Calcula la XP necesaria para el siguiente nivel
     */
    public int getXPForNextLevel(Player player) {
        int currentLevel = getLevel(player);
        return getXPForLevel(currentLevel + 1);
    }
    
    /**
     * Establece el XP de un jugador (para comandos admin)
     */
    public void setXP(Player player, int xp) {
        UUID uuid = player.getUniqueId();
        PlayerExperienceData data = playerData.get(uuid);
        
        if (data == null) {
            data = new PlayerExperienceData(0, 1);
            playerData.put(uuid, data);
        }
        
        int oldLevel = data.getNivel();
        data.setXp(Math.max(0, xp));
        
        // Recalcular nivel basado en nuevo XP
        int newLevel = calculateLevel(xp);
        data.setNivel(newLevel);
        
        // Sincronizar PS con XP cuando se establece manualmente
        if (plugin.getMissionService() != null) {
            plugin.getMissionService().setPS(uuid, xp);
        }
        
        // Notificar si cambió de nivel
        if (newLevel != oldLevel) {
            player.sendMessage("§e§l⬆ §6¡NIVEL ACTUALIZADO! §e§l⬆");
            player.sendMessage("§7Nuevo nivel: §bNivel " + newLevel + " §8(§e" + xp + " XP§8)");
        }
        
        saveData();
    }
    
    /**
     * Calcula el progreso hacia el siguiente nivel (0.0 - 1.0)
     */
    public double getProgressToNextLevel(Player player) {
        int currentXP = getXP(player);
        int currentLevel = getLevel(player);
        int currentLevelXP = getXPForLevel(currentLevel);
        int nextLevelXP = getXPForLevel(currentLevel + 1);
        
        if (nextLevelXP <= currentLevelXP) return 1.0;
        
        double progress = (double) (currentXP - currentLevelXP) / (nextLevelXP - currentLevelXP);
        return Math.max(0.0, Math.min(1.0, progress));
    }
    
    /**
     * Añade XP a un jugador (con verificación de cooldown)
     */
    public boolean addXP(Player player, int xp, String source) {
        return addXP(player, xp, source, true);
    }
    
    /**
     * Añade XP a un jugador
     * @param checkCooldown Si debe verificar cooldown (false para misiones)
     */
    public boolean addXP(Player player, int xp, String source, boolean checkCooldown) {
        if (xp <= 0) return false;
        
        UUID uuid = player.getUniqueId();
        
        // Verificar cooldown si es necesario
        if (checkCooldown && !checkAndSetCooldown(uuid, source)) {
            return false;
        }
        
        PlayerExperienceData data = getData(uuid);
        int oldLevel = data.getNivel();
        int oldXP = data.getXp();
        
        // Añadir XP
        data.addXp(xp);
        
        // Sincronizar PS con XP (XP = PS)
        if (plugin.getMissionService() != null) {
            plugin.getMissionService().addPS(uuid, xp, source);
        }
        
        // Verificar subida de nivel
        int newLevel = calculateLevel(data.getXp());
        boolean leveledUp = false;
        
        if (newLevel > oldLevel) {
            data.setNivel(newLevel);
            leveledUp = true;
            onLevelUp(player, oldLevel, newLevel);
        }
        
        // Notificar al jugador
        if (xp >= 10) {
            player.sendMessage("§a+§e" + xp + " XP §7(" + source + ")");
        } else if (xp > 0) {
            // Para XP pequeño, usar action bar (menos intrusivo)
            player.sendActionBar("§a+" + xp + " XP §7(" + source + ")");
        }
        
        // Guardar datos
        saveData();
        
        return leveledUp;
    }
    
    /**
     * Calcula el nivel basado en XP total
     */
    private int calculateLevel(int totalXP) {
        int nivel = 1;
        int xpNeeded = 0;
        
        while (xpNeeded <= totalXP) {
            nivel++;
            xpNeeded = getXPForLevel(nivel);
        }
        
        return nivel - 1;
    }
    
    /**
     * Verifica y establece cooldown para una fuente de XP
     */
    private boolean checkAndSetCooldown(UUID uuid, String source) {
        FileConfiguration config = plugin.getConfigManager().getRecompensasConfig();
        
        // Obtener cooldown de la configuración
        int cooldownSeconds = 0;
        if (source.equals("minar")) {
            cooldownSeconds = config.getInt("fuentes_xp.minar_bloques.cooldown_segundos", 5);
        } else if (source.equals("cosechar")) {
            cooldownSeconds = config.getInt("fuentes_xp.cosechar.cooldown_segundos", 5);
        } else if (source.equals("craftear")) {
            cooldownSeconds = config.getInt("fuentes_xp.craftear.cooldown_segundos", 10);
        } else if (source.equals("pescar")) {
            cooldownSeconds = config.getInt("fuentes_xp.pescar.cooldown_segundos", 15);
        }
        
        if (cooldownSeconds <= 0) return true; // Sin cooldown
        
        Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(uuid, k -> new HashMap<>());
        Long lastTime = playerCooldowns.get(source);
        long currentTime = System.currentTimeMillis();
        
        if (lastTime != null && (currentTime - lastTime) < (cooldownSeconds * 1000L)) {
            return false; // Todavía en cooldown
        }
        
        playerCooldowns.put(source, currentTime);
        return true;
    }
    
    /**
     * Evento cuando un jugador sube de nivel
     */
    private void onLevelUp(Player player, int oldLevel, int newLevel) {
        // Efectos visuales
        player.sendTitle("§6§lNIVEL " + newLevel, "§e¡Has subido de nivel!", 10, 40, 10);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        // Fuegos artificiales
        player.getWorld().spawnParticle(org.bukkit.Particle.FIREWORK, 
            player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        
        // Notificar por chat
        player.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("§e§l¡NIVEL " + newLevel + "!");
        player.sendMessage("§7Has alcanzado el nivel §e" + newLevel + "§7!");
        player.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
    
    /**
     * Añade XP desde misiones (sin cooldown, con multiplicadores)
     */
    public boolean addMissionXP(Player player, int baseXP, me.apocalipsis.missions.MissionDifficulty difficulty) {
        FileConfiguration config = plugin.getConfigManager().getRecompensasConfig();
        
        if (!config.getBoolean("fuentes_xp.misiones.enabled", true)) {
            return false;
        }
        
        // Aplicar multiplicador por dificultad
        double xp = baseXP;
        String diffPath = "fuentes_xp.misiones.por_dificultad." + difficulty.name();
        if (config.contains(diffPath)) {
            xp = config.getDouble(diffPath);
        }
        
        // Aplicar multiplicador por rango
        MissionRank rank = plugin.getRankService().getRank(player);
        double rankMultiplier = config.getDouble("fuentes_xp.misiones.multiplicador_por_rango." + rank.name(), 1.0);
        xp *= rankMultiplier;
        
        return addXP(player, (int) Math.round(xp), "Misión " + difficulty.name(), false);
    }
    
    /**
     * Añade XP por matar mobs
     */
    public boolean addMobKillXP(Player player, org.bukkit.entity.EntityType entityType) {
        FileConfiguration config = plugin.getConfigManager().getRecompensasConfig();
        
        if (!config.getBoolean("fuentes_xp.matar_mobs.enabled", true)) {
            return false;
        }
        
        int xp = 0;
        String source = "Mob";
        
        // Determinar tipo de mob y XP
        switch (entityType) {
            case ENDER_DRAGON:
            case WITHER:
                xp = config.getInt("fuentes_xp.matar_mobs.jefes.xp", 100);
                source = "Jefe";
                break;
            case ZOMBIE:
            case SKELETON:
            case CREEPER:
            case SPIDER:
            case ENDERMAN:
            case BLAZE:
            case WITCH:
            case WITHER_SKELETON:
            case CAVE_SPIDER:
            case PHANTOM:
            case DROWNED:
            case HUSK:
            case STRAY:
            case ZOMBIE_VILLAGER:
            case SILVERFISH:
            case ENDERMITE:
            case SHULKER:
            case GHAST:
            case MAGMA_CUBE:
            case SLIME:
            case PIGLIN:
            case PIGLIN_BRUTE:
            case HOGLIN:
            case ZOGLIN:
            case VINDICATOR:
            case EVOKER:
            case PILLAGER:
            case RAVAGER:
            case VEX:
                xp = config.getInt("fuentes_xp.matar_mobs.hostiles.xp", 2);
                source = "Mob hostil";
                break;
            case COW:
            case PIG:
            case CHICKEN:
            case SHEEP:
            case RABBIT:
            case HORSE:
            case DONKEY:
            case MULE:
            case LLAMA:
            case FOX:
            case WOLF:
            case CAT:
            case PARROT:
            case OCELOT:
            case PANDA:
            case POLAR_BEAR:
            case TURTLE:
            case BEE:
            case GOAT:
            case AXOLOTL:
            case FROG:
            case SNIFFER:
            case CAMEL:
            case ARMADILLO:
                xp = config.getInt("fuentes_xp.matar_mobs.pasivos.xp", 1);
                source = "Animal";
                break;
            default:
                return false;
        }
        
        if (xp > 0) {
            return addXP(player, xp, source, false);
        }
        
        return false;
    }
    
    /**
     * Añade XP por minar bloques
     */
    public boolean addMiningXP(Player player, org.bukkit.Material material) {
        FileConfiguration config = plugin.getConfigManager().getRecompensasConfig();
        
        if (!config.getBoolean("fuentes_xp.minar_bloques.enabled", true)) {
            return false;
        }
        
        double xp = config.getDouble("fuentes_xp.minar_bloques.bloques." + material.name(), 0);
        
        if (xp > 0) {
            // Los valores pueden ser decimales (0.5), se acumulan
            return addXP(player, (int) Math.ceil(xp), "Minería", true);
        }
        
        return false;
    }
    
    /**
     * Limpia los datos de un jugador (para testing)
     */
    public void resetPlayer(UUID uuid) {
        playerData.remove(uuid);
        cooldowns.remove(uuid);
        saveData();
    }
    
    /**
     * Clase interna para almacenar datos de experiencia
     */
    private static class PlayerExperienceData {
        private int xp;
        private int nivel;
        
        public PlayerExperienceData(int xp, int nivel) {
            this.xp = xp;
            this.nivel = nivel;
        }
        
        public int getXp() {
            return xp;
        }
        
        public void setXp(int xp) {
            this.xp = xp;
        }
        
        public void addXp(int amount) {
            this.xp += amount;
        }
        
        public int getNivel() {
            return nivel;
        }
        
        public void setNivel(int nivel) {
            this.nivel = nivel;
        }
    }
}
