package me.apocalipsis.ciclos;

import me.apocalipsis.Apocalipsis;
import me.riolu.apocalipsis.ciclos.CicloDataCache;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Gestiona los datos de progreso de jugadores separados por mundo.
 * Incluye: XP, Nivel, Rangos, Skills compradas, Misiones, PS, etc.
 * 
 * Optimizaciones v1.22.55:
 * - Sistema de caché en memoria con TTL
 * - Lazy loading de datos de disco
 * - Limpieza automática de entradas expiradas
 */
public class WorldDataManager {
    
    private final Apocalipsis plugin;
    private final File dataFile;
    
    // UUID -> Mundo -> PlayerProgressData
    private final Map<UUID, Map<String, PlayerProgressData>> worldData = new HashMap<>();
    
    // Sistema de caché para reducir I/O de disco
    private final CicloDataCache cache;
    
    /**
     * Datos de progreso de un jugador en un mundo específico
     */
    public static class PlayerProgressData {
        // Experiencia del plugin (no vanilla)
        private int xp;
        private int nivel;
        
        // Skills
        private Set<String> skillsDesbloqueadas;
        private Map<String, Integer> skillLevels;
        private int xpGastadaEnSkills;
        
        // Misiones y PS
        private int puntosSupervivencia;
        private List<String> misionesActivas;
        private int misionesCompletadasHoy;
        private int misionesFallidasHoy;
        
        // Rangos
        private String rangoActual;
        
        // Recompensas entregadas (clave: "UUID:RANGO")
        private Set<String> deliveredRewards;
        
        // Timestamps
        private long lastLogin;
        private long lastLogout;
        
        public PlayerProgressData() {
            this.xp = 0;
            this.nivel = 1;
            this.skillsDesbloqueadas = new HashSet<>();
            this.skillLevels = new HashMap<>();
            this.xpGastadaEnSkills = 0;
            this.puntosSupervivencia = 0;
            this.misionesActivas = new ArrayList<>();
            this.misionesCompletadasHoy = 0;
            this.misionesFallidasHoy = 0;
            this.rangoActual = "NOVATO";
            this.deliveredRewards = new HashSet<>();
            this.lastLogin = System.currentTimeMillis();
            this.lastLogout = 0;
        }
        
        // Getters y Setters
        public int getXp() { return xp; }
        public void setXp(int xp) { this.xp = xp; }
        
        public int getNivel() { return nivel; }
        public void setNivel(int nivel) { this.nivel = nivel; }
        
        public Set<String> getSkillsDesbloqueadas() { return skillsDesbloqueadas; }
        public void setSkillsDesbloqueadas(Set<String> skills) { this.skillsDesbloqueadas = skills; }
        
        public Map<String, Integer> getSkillLevels() { return skillLevels; }
        public void setSkillLevels(Map<String, Integer> levels) { this.skillLevels = levels; }
        
        public int getXpGastadaEnSkills() { return xpGastadaEnSkills; }
        public void setXpGastadaEnSkills(int xp) { this.xpGastadaEnSkills = xp; }
        
        public int getPuntosSupervivencia() { return puntosSupervivencia; }
        public void setPuntosSupervivencia(int ps) { this.puntosSupervivencia = ps; }
        
        public List<String> getMisionesActivas() { return misionesActivas; }
        public void setMisionesActivas(List<String> misiones) { this.misionesActivas = misiones; }
        
        public int getMisionesCompletadasHoy() { return misionesCompletadasHoy; }
        public void setMisionesCompletadasHoy(int count) { this.misionesCompletadasHoy = count; }
        
        public int getMisionesFallidasHoy() { return misionesFallidasHoy; }
        public void setMisionesFallidasHoy(int count) { this.misionesFallidasHoy = count; }
        
        public String getRangoActual() { return rangoActual; }
        public void setRangoActual(String rango) { this.rangoActual = rango; }
        
        public Set<String> getDeliveredRewards() { return deliveredRewards; }
        public void setDeliveredRewards(Set<String> rewards) { this.deliveredRewards = rewards; }
        
        public long getLastLogin() { return lastLogin; }
        public void setLastLogin(long time) { this.lastLogin = time; }
        
        public long getLastLogout() { return lastLogout; }
        public void setLastLogout(long time) { this.lastLogout = time; }
    }
    
    public WorldDataManager(Apocalipsis plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "world_data.yml");
        
        // Inicializar caché con 5 minutos TTL y máximo 100 jugadores
        this.cache = new CicloDataCache(300000L, 100);
        
        loadData();
    }
    
    /**
     * Guarda los datos actuales del jugador para su mundo actual
     * También actualiza el caché
     */
    public void savePlayerData(UUID uuid, String worldName, PlayerProgressData data) {
        worldData.computeIfAbsent(uuid, k -> new HashMap<>()).put(worldName, data);
        
        // Actualizar caché con TTL de 5 minutos
        cache.put(uuid, worldName, data);
        
        plugin.getLogger().info("[WorldData] Guardados datos de " + uuid + 
                                " para mundo: " + worldName);
    }
    
    /**
     * Carga los datos del jugador para un mundo específico
     * Si no existen, retorna datos nuevos/vacíos
     * Utiliza caché para reducir lecturas de disco
     */
    public PlayerProgressData loadPlayerData(UUID uuid, String worldName) {
        // Intentar obtener del caché primero
        PlayerProgressData cachedData = cache.get(uuid, worldName);
        if (cachedData != null) {
            plugin.getLogger().fine("[WorldData] Cache hit para " + uuid + " en " + worldName);
            return cachedData;
        }
        
        // Si no está en caché, buscar en memoria
        PlayerProgressData data = worldData
            .getOrDefault(uuid, new HashMap<>())
            .get(worldName);
        
        if (data == null) {
            plugin.getLogger().info("[WorldData] No hay datos para " + uuid + 
                                    " en mundo: " + worldName + ". Creando nuevos.");
            data = new PlayerProgressData();
        }
        
        // Guardar en caché para futuras lecturas
        cache.put(uuid, worldName, data);

        
        return data;
    }
    
    /**
     * Captura el estado actual del jugador desde los servicios del plugin
     */
    public PlayerProgressData captureCurrentState(UUID uuid) {
        PlayerProgressData data = new PlayerProgressData();
        
        try {
            // Capturar XP y nivel desde ExperienceService
            if (plugin.getExperienceService() != null) {
                data.setXp(plugin.getExperienceService().getXP(uuid));
                data.setNivel(plugin.getExperienceService().getLevel(uuid));
            }
        
        // Capturar Skills desde SkillService
        if (plugin.getSkillService() != null) {
            var skillData = plugin.getSkillService().getPlayerData(uuid);
            if (skillData != null) {
                // Convertir Skills a IDs
                Set<String> skillIds = new HashSet<>();
                if (skillData.getSkills() != null) {
                    skillData.getSkills().forEach(skill -> skillIds.add(skill.getId()));
                }
                data.setSkillsDesbloqueadas(skillIds);
                
                // Convertir niveles de skills
                Map<String, Integer> levels = new HashMap<>();
                if (skillData.getSkillLevels() != null) {
                    skillData.getSkillLevels().forEach((skill, level) -> 
                        levels.put(skill.getId(), level.getLevel())
                    );
                }
                data.setSkillLevels(levels);
                
                data.setXpGastadaEnSkills(skillData.getXpGastada());
            }
        }
        
        // Capturar PS y misiones desde MissionService
        if (plugin.getMissionService() != null) {
            data.setPuntosSupervivencia(plugin.getMissionService().getPS(uuid));
            
            // Capturar misiones activas (simplificado - necesitarías acceso a los métodos)
            // Por ahora dejamos vacío, se puede expandir
            data.setMisionesActivas(new ArrayList<>());
        }
        
        // Capturar rango desde RankService
        if (plugin.getRankService() != null) {
            var rank = plugin.getRankService().getRank(uuid);
            if (rank != null) {
                data.setRangoActual(rank.name());
            }
        }
        
        // Capturar recompensas entregadas desde RewardService
        if (plugin.getRewardService() != null) {
            Set<String> rewards = plugin.getRewardService().getDeliveredRewards(uuid);
            data.setDeliveredRewards(rewards);
        }
        
            data.setLastLogout(System.currentTimeMillis());
            
            return data;
            
        } catch (Exception e) {
            plugin.getLogger().severe("[WorldData] Error capturando estado de " + uuid + ": " + e.getMessage());
            e.printStackTrace();
            
            // Retornar datos vacíos en caso de error (mejor que null)
            plugin.getLogger().warning("[WorldData] Retornando datos por defecto debido al error");
            return new PlayerProgressData();
        }
    }
    
    /**
     * Aplica los datos guardados a los servicios del plugin
     * Incluye rollback automático en caso de error
     */
    public void applyStateToServices(UUID uuid, PlayerProgressData data) {
        // Guardar estado previo para rollback
        PlayerProgressData backupData = null;
        
        try {
            // Capturar estado actual como backup
            backupData = captureCurrentState(uuid);
            
            // Aplicar XP y nivel
            if (plugin.getExperienceService() != null) {
                plugin.getExperienceService().setXP(uuid, data.getXp());
                plugin.getExperienceService().setLevel(uuid, data.getNivel());
            }
        
        // Aplicar Skills
        if (plugin.getSkillService() != null) {
            // Primero resetear para limpiar datos antiguos
            plugin.getSkillService().resetPlayer(uuid);
            
            // Luego aplicar las skills del mundo actual
            if (!data.getSkillsDesbloqueadas().isEmpty()) {
                plugin.getSkillService().applySkillData(
                    uuid, 
                    data.getSkillsDesbloqueadas(), 
                    data.getSkillLevels()
                );
            }
        }
        
        // Aplicar PS (Puntos de Supervivencia)
        if (plugin.getMissionService() != null) {
            plugin.getMissionService().setPS(uuid, data.getPuntosSupervivencia());
        }
        
        // Aplicar rango (se recalcula automáticamente basado en XP)
        if (plugin.getRankService() != null) {
            plugin.getRankService().updatePlayerRank(uuid);
        }
        
        // Aplicar recompensas entregadas desde RewardService
        if (plugin.getRewardService() != null && data.getDeliveredRewards() != null) {
            plugin.getRewardService().setDeliveredRewards(uuid, data.getDeliveredRewards());
        }
        
            plugin.getLogger().info("[WorldData] Aplicado estado completo para " + uuid + 
                " - XP: " + data.getXp() + ", Nivel: " + data.getNivel() + 
                ", PS: " + data.getPuntosSupervivencia() + 
                ", Skills: " + data.getSkillsDesbloqueadas().size());
                
        } catch (Exception e) {
            plugin.getLogger().severe("[WorldData] ERROR al aplicar estado para " + uuid + ": " + e.getMessage());
            e.printStackTrace();
            
            // Intentar rollback al estado previo
            if (backupData != null) {
                plugin.getLogger().warning("[WorldData] Intentando rollback al estado previo...");
                
                try {
                    // Restaurar XP y nivel
                    if (plugin.getExperienceService() != null) {
                        plugin.getExperienceService().setXP(uuid, backupData.getXp());
                        plugin.getExperienceService().setLevel(uuid, backupData.getNivel());
                    }
                    
                    // Restaurar Skills
                    if (plugin.getSkillService() != null) {
                        plugin.getSkillService().resetPlayer(uuid);
                        if (!backupData.getSkillsDesbloqueadas().isEmpty()) {
                            plugin.getSkillService().applySkillData(
                                uuid, 
                                backupData.getSkillsDesbloqueadas(), 
                                backupData.getSkillLevels()
                            );
                        }
                    }
                    
                    // Restaurar PS
                    if (plugin.getMissionService() != null) {
                        plugin.getMissionService().setPS(uuid, backupData.getPuntosSupervivencia());
                    }
                    
                    plugin.getLogger().info("[WorldData] Rollback completado exitosamente");
                    
                } catch (Exception rollbackError) {
                    plugin.getLogger().severe("[WorldData] FALLO EL ROLLBACK: " + rollbackError.getMessage());
                    plugin.getLogger().severe("[WorldData] El jugador " + uuid + " puede tener datos inconsistentes!");
                    rollbackError.printStackTrace();
                }
            } else {
                plugin.getLogger().severe("[WorldData] No hay backup disponible para rollback");
            }
        }
    }
    
    /**
     * Crea datos de "reseteo" (jugador nuevo) para un ciclo
     * Respeta la configuración de ciclos.yml para saber qué resetear
     * 
     * @param currentData Datos actuales del jugador (para preservar lo que NO se resetea)
     * @return Datos frescos con reset selectivo según configuración
     */
    public PlayerProgressData createFreshData(PlayerProgressData currentData) {
        PlayerProgressData data = new PlayerProgressData();
        
        // Leer configuración de reseteo
        FileConfiguration cicloConfig = plugin.getCicloConfig();
        if (cicloConfig == null) {
            plugin.getLogger().warning("[WorldData] No se pudo cargar ciclos.yml - reseteando todo por defecto");
            return data; // Reset completo por defecto
        }
        
        boolean resetearRangos = cicloConfig.getBoolean("reseteo.resetear_rangos", true);
        boolean resetearSkills = cicloConfig.getBoolean("reseteo.resetear_skills", true);
        boolean resetearXP = cicloConfig.getBoolean("reseteo.resetear_xp", true);
        boolean resetearPS = cicloConfig.getBoolean("reseteo.resetear_ps", true);
        boolean resetearMisiones = cicloConfig.getBoolean("reseteo.resetear_misiones", true);
        
        // Aplicar valores iniciales o preservar según configuración
        if (resetearXP) {
            data.setXp(cicloConfig.getInt("reseteo.xp_inicial", 0));
            data.setNivel(cicloConfig.getInt("reseteo.nivel_inicial", 1));
        } else if (currentData != null) {
            data.setXp(currentData.getXp());
            data.setNivel(currentData.getNivel());
        }
        
        if (!resetearRangos && currentData != null) {
            data.setRangoActual(currentData.getRangoActual());
        } else {
            data.setRangoActual("NOVATO");
        }
        
        if (!resetearSkills && currentData != null) {
            data.setSkillsDesbloqueadas(new HashSet<>(currentData.getSkillsDesbloqueadas()));
            data.setSkillLevels(new HashMap<>(currentData.getSkillLevels()));
            data.setXpGastadaEnSkills(currentData.getXpGastadaEnSkills());
        }
        
        if (resetearPS) {
            data.setPuntosSupervivencia(cicloConfig.getInt("reseteo.ps_inicial", 0));
        } else if (currentData != null) {
            data.setPuntosSupervivencia(currentData.getPuntosSupervivencia());
        }
        
        if (!resetearMisiones && currentData != null) {
            data.setMisionesActivas(new ArrayList<>(currentData.getMisionesActivas()));
            data.setMisionesCompletadasHoy(currentData.getMisionesCompletadasHoy());
            data.setMisionesFallidasHoy(currentData.getMisionesFallidasHoy());
        }
        
        plugin.getLogger().info("[WorldData] Datos frescos creados - Rangos globales: " + !resetearRangos + 
                                ", Skills globales: " + !resetearSkills);
        
        return data;
    }
    
    /**
     * Crea datos de reseteo sin datos previos (jugador completamente nuevo)
     */
    public PlayerProgressData createFreshData() {
        return createFreshData(null);
    }
    
    /**
     * Verifica si existen datos para un jugador en un mundo
     * También verifica el caché primero
     */
    public boolean hasData(UUID uuid, String worldName) {
        // Verificar caché primero
        if (cache.has(uuid, worldName)) {
            return true;
        }
        
        return worldData.containsKey(uuid) && 
               worldData.get(uuid).containsKey(worldName);
    }
    
    /**
     * Elimina los datos de un jugador en un mundo
     * También invalida el caché
     */
    public void deleteData(UUID uuid, String worldName) {
        if (worldData.containsKey(uuid)) {
            worldData.get(uuid).remove(worldName);
            
            // Invalidar entrada de caché
            cache.invalidate(uuid, worldName);
            
            plugin.getLogger().info("[WorldData] Eliminados datos de " + uuid + 
                                    " para mundo: " + worldName);
        }
    }
    
    /**
     * Limpia entradas expiradas del caché
     * Debe llamarse periódicamente (ej: cada 5 minutos)
     */
    public void cleanCache() {
        int removed = cache.cleanExpired();
        if (removed > 0) {
            plugin.getLogger().info("[WorldData] Limpiadas " + removed + " entradas expiradas del caché");
        }
    }
    
    /**
     * Obtiene estadísticas del caché
     */
    public Map<String, Object> getCacheStats() {
        return cache.getStats();
    }
    
    /**
     * Invalida todo el caché de un jugador
     * Útil cuando se recarga la configuración
     */
    public void invalidatePlayerCache(UUID uuid) {
        cache.invalidatePlayer(uuid);
    }
    
    /**
     * Invalida todo el caché de un mundo
     * Útil cuando se desactiva un ciclo
     */
    public void invalidateWorldCache(String worldName) {
        cache.invalidateWorld(worldName);
    }
    
    // ==================== PERSISTENCIA ====================
    
    public void loadData() {
        if (!dataFile.exists()) {
            plugin.getLogger().info("[WorldData] No existe world_data.yml, creando nuevo");
            return;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        
        for (String uuidStr : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                
                for (String worldName : config.getConfigurationSection(uuidStr).getKeys(false)) {
                    String path = uuidStr + "." + worldName;
                    
                    PlayerProgressData data = new PlayerProgressData();
                    
                    // Cargar XP y nivel
                    data.setXp(config.getInt(path + ".xp", 0));
                    data.setNivel(config.getInt(path + ".nivel", 1));
                    
                    // Cargar skills
                    List<String> skills = config.getStringList(path + ".skills");
                    data.setSkillsDesbloqueadas(new HashSet<>(skills));
                    
                    // Cargar niveles de skills
                    Map<String, Integer> skillLevels = new HashMap<>();
                    if (config.contains(path + ".skill_levels")) {
                        for (String key : config.getConfigurationSection(path + ".skill_levels").getKeys(false)) {
                            skillLevels.put(key, config.getInt(path + ".skill_levels." + key));
                        }
                    }
                    data.setSkillLevels(skillLevels);
                    
                    data.setXpGastadaEnSkills(config.getInt(path + ".xp_gastada", 0));
                    
                    // Cargar PS y misiones
                    data.setPuntosSupervivencia(config.getInt(path + ".ps", 0));
                    data.setMisionesActivas(config.getStringList(path + ".misiones_activas"));
                    data.setMisionesCompletadasHoy(config.getInt(path + ".misiones_completadas", 0));
                    data.setMisionesFallidasHoy(config.getInt(path + ".misiones_fallidas", 0));
                    
                    // Cargar rango
                    data.setRangoActual(config.getString(path + ".rango", "NOVATO"));
                    
                    // Cargar recompensas entregadas
                    List<String> rewards = config.getStringList(path + ".delivered_rewards");
                    data.setDeliveredRewards(new HashSet<>(rewards));
                    
                    // Cargar timestamps
                    data.setLastLogin(config.getLong(path + ".last_login", System.currentTimeMillis()));
                    data.setLastLogout(config.getLong(path + ".last_logout", 0));
                    
                    worldData.computeIfAbsent(uuid, k -> new HashMap<>()).put(worldName, data);
                }
                
            } catch (Exception e) {
                plugin.getLogger().warning("[WorldData] Error cargando datos: " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("[WorldData] Cargados datos de " + worldData.size() + " jugadores");
    }
    
    public void saveData() {
        FileConfiguration config = new YamlConfiguration();
        
        for (Map.Entry<UUID, Map<String, PlayerProgressData>> playerEntry : worldData.entrySet()) {
            String uuidStr = playerEntry.getKey().toString();
            
            for (Map.Entry<String, PlayerProgressData> worldEntry : playerEntry.getValue().entrySet()) {
                String worldName = worldEntry.getKey();
                PlayerProgressData data = worldEntry.getValue();
                String path = uuidStr + "." + worldName;
                
                // Guardar XP y nivel
                config.set(path + ".xp", data.getXp());
                config.set(path + ".nivel", data.getNivel());
                
                // Guardar skills
                config.set(path + ".skills", new ArrayList<>(data.getSkillsDesbloqueadas()));
                
                // Guardar niveles de skills
                for (Map.Entry<String, Integer> entry : data.getSkillLevels().entrySet()) {
                    config.set(path + ".skill_levels." + entry.getKey(), entry.getValue());
                }
                
                config.set(path + ".xp_gastada", data.getXpGastadaEnSkills());
                
                // Guardar PS y misiones
                config.set(path + ".ps", data.getPuntosSupervivencia());
                config.set(path + ".misiones_activas", data.getMisionesActivas());
                config.set(path + ".misiones_completadas", data.getMisionesCompletadasHoy());
                config.set(path + ".misiones_fallidas", data.getMisionesFallidasHoy());
                
                // Guardar rango
                config.set(path + ".rango", data.getRangoActual());
                
                // Guardar recompensas entregadas
                config.set(path + ".delivered_rewards", new ArrayList<>(data.getDeliveredRewards()));
                
                // Guardar timestamps
                config.set(path + ".last_login", data.getLastLogin());
                config.set(path + ".last_logout", data.getLastLogout());
            }
        }
        
        try {
            config.save(dataFile);
            plugin.getLogger().info("[WorldData] Guardados datos de " + worldData.size() + " jugadores");
        } catch (IOException e) {
            plugin.getLogger().severe("[WorldData] Error guardando datos: " + e.getMessage());
        }
    }
}
