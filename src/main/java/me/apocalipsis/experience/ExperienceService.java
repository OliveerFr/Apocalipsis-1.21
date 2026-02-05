package me.apocalipsis.experience;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.missions.MissionRank;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servicio de experiencia y niveles independiente del sistema de misiones.
 * Maneja XP de múltiples fuentes y calcula niveles automáticamente.
 */
public class ExperienceService {
    
    private final Apocalipsis plugin;
    private final File dataFile;
    private final Map<UUID, PlayerExperienceData> playerData = new HashMap<>();
    private final Map<Integer, Integer> xpLevelCache = new HashMap<>(); // Cache de XP por nivel
    private final Map<String, Double> rankMultiplierCache = new HashMap<>(); // Cache de multiplicadores de rango
    
    // Configuración de XP
    private int nivelInicial = 100;
    private int multiplicador = 50;
    
    // Cooldowns para evitar spam
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    
    // [OPTIMIZACIÓN v1.22.68] Sistema de auto-save periódico en lugar de save por cada XP
    private final AtomicBoolean hasUnsavedChanges = new AtomicBoolean(false);
    private BukkitTask autoSaveTask;
    private static final long AUTOSAVE_INTERVAL = 20L * 60 * 5; // Cada 5 minutos
    
    public ExperienceService(Apocalipsis plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "experience_data.yml");
        loadConfig();
        loadData();
        startAutoSave();
    }
    
    /**
     * Carga la configuración desde recompensas.yml
     */
    private void loadConfig() {
        FileConfiguration config = plugin.getConfigManager().getRecompensasConfig();
        nivelInicial = config.getInt("experiencia.nivel_inicial", 100);
        multiplicador = config.getInt("experiencia.multiplicador", 50);
        
        // Cachear multiplicadores de rango
        rankMultiplierCache.clear();
        ConfigurationSection rankSection = config.getConfigurationSection("fuentes_xp.misiones.multiplicador_por_rango");
        if (rankSection != null) {
            for (String rankName : rankSection.getKeys(false)) {
                double multiplier = config.getDouble("fuentes_xp.misiones.multiplicador_por_rango." + rankName, 1.0);
                rankMultiplierCache.put(rankName, multiplier);
            }
        }
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
        saveData(false); // Por defecto async
    }
    
    /**
     * [MEJORADO v1.22.68] Guarda datos de experiencia con opción async
     * @param forceSync Si true, fuerza guardado síncrono (para shutdown)
     */
    public void saveData(boolean forceSync) {
        if (!hasUnsavedChanges.get() && !forceSync) {
            return; // No hay cambios pendientes
        }
        
        // Preparar datos a guardar
        FileConfiguration config = new YamlConfiguration();
        synchronized (playerData) {
            for (Map.Entry<UUID, PlayerExperienceData> entry : playerData.entrySet()) {
                String path = "players." + entry.getKey().toString();
                config.set(path + ".xp", entry.getValue().getXp());
                config.set(path + ".nivel", entry.getValue().getNivel());
            }
        }
        
        // Ejecutar save (async o sync)
        Runnable saveTask = () -> {
            try {
                config.save(dataFile);
                hasUnsavedChanges.set(false);
            } catch (IOException e) {
                plugin.getLogger().severe("[XP] Error guardando experience_data.yml: " + e.getMessage());
            }
        };
        
        if (forceSync) {
            saveTask.run(); // Sync para shutdown
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, saveTask);
        }
    }
    
    /**
     * [NUEVO v1.22.68] Inicia el auto-save periódico
     */
    private void startAutoSave() {
        autoSaveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (hasUnsavedChanges.get()) {
                saveData(false); // Async save
            }
        }, AUTOSAVE_INTERVAL, AUTOSAVE_INTERVAL);
    }
    
    /**
     * [NUEVO v1.22.68] Detiene el auto-save y guarda cambios pendientes
     */
    public void shutdown() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        saveData(true); // Sync save al apagar
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
        return getLevel(player.getUniqueId());
    }
    
    /**
     * Obtiene el nivel actual de un jugador por UUID
     */
    public int getLevel(UUID uuid) {
        return getData(uuid).getNivel();
    }
    
    /**
     * Obtiene la XP actual de un jugador
     */
    public int getXP(Player player) {
        return getXP(player.getUniqueId());
    }
    
    /**
     * Obtiene la XP actual de un jugador por UUID
     */
    public int getXP(UUID uuid) {
        return getData(uuid).getXp();
    }
    
    /**
     * Calcula la XP necesaria para alcanzar un nivel específico
     * Usa cache para evitar recalcular niveles frecuentemente accedidos
     */
    public int getXPForLevel(int nivel) {
        if (nivel <= 1) return 0;
        
        // Check cache first
        Integer cached = xpLevelCache.get(nivel);
        if (cached != null) {
            return cached;
        }
        
        // Calculate if not cached
        int totalXP = 0;
        for (int i = 2; i <= nivel; i++) {
            totalXP += nivelInicial + ((i - 2) * multiplicador);
        }
        
        // Cache result (limit cache size to first 100 levels)
        if (nivel <= 100) {
            xpLevelCache.put(nivel, totalXP);
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
        setXP(player.getUniqueId(), xp);
    }
    
    /**
     * Establece el XP de un jugador por UUID (para sistema de ciclos)
     */
    public void setXP(UUID uuid, int xp) {
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
        
        // Notificar si el jugador está online y cambió de nivel
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline() && newLevel != oldLevel) {
            player.sendMessage("§e§l⬆ §6¡NIVEL ACTUALIZADO! §e§l⬆");
            player.sendMessage("§7Nuevo nivel: §bNivel " + newLevel + " §8(§e" + xp + " XP§8)");
        }
        
        // [OPTIMIZACIÓN v1.22.68] Marcar como modificado en lugar de save inmediato
        hasUnsavedChanges.set(true);
    }
    
    /**
     * Establece el nivel de un jugador directamente (para sistema de ciclos)
     */
    public void setLevel(UUID uuid, int level) {
        PlayerExperienceData data = playerData.get(uuid);
        
        if (data == null) {
            data = new PlayerExperienceData(0, 1);
            playerData.put(uuid, data);
        }
        
        data.setNivel(Math.max(1, level));
        // [OPTIMIZACIÓN v1.22.68] Marcar como modificado
        hasUnsavedChanges.set(true);
    }
    
    /**
     * Gasta XP del jugador (para compras de skills/upgrades)
     * @return true si se pudo gastar, false si no había suficiente
     */
    public boolean spendXP(Player player, int amount) {
        if (amount <= 0) return false;
        
        UUID uuid = player.getUniqueId();
        PlayerExperienceData data = getData(uuid);
        
        if (data.getXp() < amount) {
            return false; // No hay suficiente XP
        }
        
        // Reducir XP
        data.setXp(data.getXp() - amount);
        
        // Sincronizar PS con XP
        if (plugin.getMissionService() != null) {
            plugin.getMissionService().setPS(uuid, data.getXp());
        }
        
        // [OPTIMIZACIÓN v1.22.68] Marcar como modificado
        hasUnsavedChanges.set(true);
        return true;
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
        
        // 🎯 Guardar rango anterior ANTES de añadir XP
        MissionRank oldRank = plugin.getRankService().getRank(player);
        
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
            
            // 🎯 Verificar si subió de RANGO (más importante que nivel)
            MissionRank newRank = plugin.getRankService().getRank(player);
            if (newRank != oldRank) {
                // ¡SUBIDA DE RANGO! Efectos épicos
                onRankUp(player, oldRank, newRank);
            } else {
                // Solo subió de nivel (efectos normales)
                onLevelUp(player, oldLevel, newLevel);
            }
        }
        
        // Notificar al jugador
        if (xp >= 10) {
            player.sendMessage("§a+§e" + xp + " XP §7(" + source + ")");
        } else if (xp > 0) {
            // Para XP pequeño, usar action bar (menos intrusivo)
            player.sendActionBar(net.kyori.adventure.text.Component.text("§a+" + xp + " XP §7(" + source + ")"));
        }
        
        // [OPTIMIZACIÓN v1.22.68] Marcar como modificado
        hasUnsavedChanges.set(true);
        
        return leveledUp;
    }
    
    /**
     * Calcula el nivel basado en XP total (público para sistema de rangos)
     */
    public int calculateLevel(int totalXP) {
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
     * Evento cuando un jugador sube de RANGO (¡ÉPICO!)
     */
    private void onRankUp(Player player, MissionRank oldRank, MissionRank newRank) {
        String rankName = newRank.getDisplayName();
        
        // 🎉 Título épico
        net.kyori.adventure.title.Title title = net.kyori.adventure.title.Title.title(
            net.kyori.adventure.text.Component.text("§6§l⬆ RANGO ASCENDIDO ⬆"),
            net.kyori.adventure.text.Component.text(rankName),
            net.kyori.adventure.title.Title.Times.times(
                java.time.Duration.ofMillis(500), 
                java.time.Duration.ofMillis(4000), 
                java.time.Duration.ofMillis(1000)
            )
        );
        player.showTitle(title);
        
        // 🔊 Sonidos épicos
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 2.0f, 1.0f);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 1.2f);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.8f);
        
        // ✨ Partículas épicas
        org.bukkit.Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, loc, 100, 1.0, 1.5, 1.0, 0.1);
        player.getWorld().spawnParticle(org.bukkit.Particle.FIREWORK, loc, 50, 0.8, 0.8, 0.8, 0.2);
        player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, loc, 40, 0.6, 1.0, 0.6, 0.15);
        player.getWorld().spawnParticle(org.bukkit.Particle.ENCHANT, loc, 60, 1.2, 1.2, 1.2, 1.0);
        
        // 💬 Notificación al jugador
        player.sendMessage("§6§l═════════════════════════════════════");
        player.sendMessage("§e§l         ¡ASCENSO DE RANGO!");
        player.sendMessage("§7Has alcanzado el rango " + rankName + "§7!");
        player.sendMessage("§6§l═════════════════════════════════════");
        
        // 🌍 Mensaje GLOBAL al servidor
        String globalMessage = "§6§l★ " + player.getName() + " §7ha alcanzado el rango " + rankName + "§7! §6§l★";
        plugin.getServer().broadcast(net.kyori.adventure.text.Component.text(globalMessage));
        
        // 🎁 Entregar recompensas del rango
        if (plugin.getRewardService() != null) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getLogger().info("[XP] Entregando recompensas a " + player.getName() + " por rango " + newRank.name());
                boolean delivered = plugin.getRewardService().deliverRewards(player, newRank);
                if (delivered) {
                    plugin.getLogger().info("[XP] ✓ Recompensas entregadas exitosamente a " + player.getName());
                } else {
                    plugin.getLogger().warning("[XP] ✗ No se pudieron entregar recompensas a " + player.getName() + " (ya entregadas o sin config)");
                }
            }, 20L); // 1 segundo de delay para que vea el título primero
        } else {
            plugin.getLogger().severe("[XP] ERROR: RewardService es NULL! No se pueden entregar recompensas.");
        }
        
        // 💪 Aplicar habilidades del nuevo rango
        if (plugin.getAbilityService() != null) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getAbilityService().applyAbilities(player, true);
                player.sendMessage("§a§l✔ §7Habilidades de rango " + rankName + " §7activadas!");
            }, 40L); // 2 segundos después de las recompensas
        }
        
        plugin.getLogger().info("[XP] " + player.getName() + " subió de rango: " + oldRank.name() + " → " + newRank.name());
    }
    
    /**
     * Evento cuando un jugador sube de nivel
     */
    private void onLevelUp(Player player, int oldLevel, int newLevel) {
        // Efectos visuales
        net.kyori.adventure.title.Title title = net.kyori.adventure.title.Title.title(
            net.kyori.adventure.text.Component.text("§6§lNIVEL " + newLevel),
            net.kyori.adventure.text.Component.text("§e¡Has subido de nivel!"),
            net.kyori.adventure.title.Title.Times.times(
                java.time.Duration.ofMillis(500), 
                java.time.Duration.ofMillis(2000), 
                java.time.Duration.ofMillis(500)
            )
        );
        player.showTitle(title);
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
        
        // Aplicar multiplicador por rango (usar cache)
        MissionRank rank = plugin.getRankService().getRank(player);
        double rankMultiplier = rankMultiplierCache.getOrDefault(rank.name(), 1.0);
        xp *= rankMultiplier;
        
        return addXP(player, (int) Math.round(xp), "Misión " + difficulty.name(), false);
    }
    
    /**
     * Añade XP por matar mobs
     * @return XP otorgado (0 si no se otorgó)
     */
    public int addMobKillXP(Player player, org.bukkit.entity.EntityType entityType) {
        FileConfiguration config = plugin.getConfigManager().getRecompensasConfig();
        
        if (!config.getBoolean("fuentes_xp.matar_mobs.enabled", true)) {
            return 0;
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
            case GUARDIAN:
            case ELDER_GUARDIAN:
            case WARDEN:
            case BREEZE:
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
                return 0;
        }
        
        if (xp > 0) {
            addXP(player, xp, source, false);
            return xp;
        }
        
        return 0;
    }
    
    /**
     * Añade XP por minar bloques
     * @return XP otorgado (0 si no se otorgó)
     */
    public int addMiningXP(Player player, org.bukkit.Material material) {
        FileConfiguration config = plugin.getConfigManager().getRecompensasConfig();
        
        if (!config.getBoolean("fuentes_xp.minar_bloques.enabled", true)) {
            return 0;
        }
        
        double xp = config.getDouble("fuentes_xp.minar_bloques.bloques." + material.name(), 0);
        
        if (xp > 0) {
            int xpAmount = (int) Math.ceil(xp);
            addXP(player, xpAmount, "Minería", true);
            return xpAmount;
        }
        
        return 0;
    }
    
    /**
     * Limpia los datos de un jugador (para testing)
     */
    public void resetPlayer(UUID uuid) {
        playerData.remove(uuid);
        cooldowns.remove(uuid);
        hasUnsavedChanges.set(true);
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
