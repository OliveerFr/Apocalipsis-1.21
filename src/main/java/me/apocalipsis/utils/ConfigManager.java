package me.apocalipsis.utils;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.apocalipsis.Apocalipsis;

public class ConfigManager {

    private final Apocalipsis plugin;
    private FileConfiguration config;
    private FileConfiguration desastresConfig;
    private FileConfiguration misionesConfig;
    private FileConfiguration rangosConfig;
    private FileConfiguration alonsoLevelsConfig;

    public ConfigManager(Apocalipsis plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.config = plugin.getConfig();
        this.desastresConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "desastres.yml"));
        this.misionesConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "misiones_new.yml"));
        this.rangosConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "rangos.yml"));
        this.alonsoLevelsConfig = loadOrCreateAlonsoLevels();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getDesastresConfig() {
        return desastresConfig;
    }

    public FileConfiguration getMisionesConfig() {
        return misionesConfig;
    }

    public FileConfiguration getRangosConfig() {
        return rangosConfig;
    }

    public FileConfiguration getAlonsoLevelsConfig() {
        return alonsoLevelsConfig;
    }

    public boolean isLluviaFuegoExtraLluvia() {
        return config.getBoolean("ajustes.lluvia_fuego_extra_lluvia", true);
    }

    public boolean isReproducirSonidos() {
        return config.getBoolean("ajustes.reproducir_sonidos", true);
    }

    public boolean isDanoAleatorio() {
        return config.getBoolean("ajustes.dano_aleatorio", true);
    }

    public boolean isTestMode() {
        return config.getBoolean("ajustes.test_mode", false);
    }

    public void setTestMode(boolean value) {
        config.set("ajustes.test_mode", value);
    }

    public double getTpsThreshold() {
        return config.getDouble("rendimiento.tps_thresholds.critical", 10.0);
    }

    public String getDefaultDisaster() {
        return config.getString("default_disaster", "random");
    }

    public int getCooldownFinSegundos() {
        // Usar ciclo.cooldown_fin_segundos como fuente de verdad
        int ciclo = desastresConfig.getInt("ciclo.cooldown_fin_segundos", -1);
        if (ciclo != -1) {
            return ciclo;
        }
        // Fallback a desastres.cooldown_fin_segundos (deprecated)
        return desastresConfig.getInt("desastres.cooldown_fin_segundos", 120);
    }

    public int getDisasterWeight(String disasterId) {
        return desastresConfig.getInt("desastres.weights." + disasterId, 1);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Getters para control de ciclo
    // ═══════════════════════════════════════════════════════════════════
    
    public boolean isAutoCycleEnabled() {
        return desastresConfig.getBoolean("ciclo.auto_cycle", false);
    }
    
    public boolean isStartOnBoot() {
        return desastresConfig.getBoolean("ciclo.start_on_boot", false);
    }
    
    public int getMinJugadores() {
        return desastresConfig.getInt("ciclo.min_jugadores", 1);
    }
    
    public boolean isRespetarCooldown() {
        return desastresConfig.getBoolean("ciclo.respectar_cooldown", true);
    }
    
    public int getPreparacionInicialSegundos() {
        return desastresConfig.getInt("ciclo.preparacion_inicial_segundos", 900);
    }
    
    public boolean isDebugCiclo() {
        // Leer desde config.yml en lugar de desastres.yml para mejor control
        return config.getBoolean("debug", false);
    }
    
    /**
     * Activa o desactiva el modo debug en tiempo real (se guarda en config.yml)
     */
    public void setDebugCiclo(boolean value) {
        config.set("debug", value);
        plugin.saveConfig();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Excepciones anti-desastre
    // ═══════════════════════════════════════════════════════════════════
    
    public boolean isExcepcionesEnabled() {
        return desastresConfig.getBoolean("excepciones.enabled", true);
    }
    
    public java.util.Set<java.util.UUID> getExcepciones() {
        java.util.List<String> list = desastresConfig.getStringList("excepciones.players");
        java.util.Set<java.util.UUID> set = new java.util.HashSet<>();
        for (String s : list) {
            try {
                set.add(java.util.UUID.fromString(s));
            } catch (IllegalArgumentException e) {
                // Ignorar UUIDs inválidos
            }
        }
        return set;
    }
    
    public void addExcepcion(java.util.UUID uuid) {
        java.util.List<String> list = desastresConfig.getStringList("excepciones.players");
        String uuidStr = uuid.toString();
        if (!list.contains(uuidStr)) {
            list.add(uuidStr);
            desastresConfig.set("excepciones.players", list);
            saveDesastresConfig();
        }
    }
    
    public void removeExcepcion(java.util.UUID uuid) {
        java.util.List<String> list = desastresConfig.getStringList("excepciones.players");
        String uuidStr = uuid.toString();
        if (list.remove(uuidStr)) {
            desastresConfig.set("excepciones.players", list);
            saveDesastresConfig();
        }
    }
    
    private void saveDesastresConfig() {
        try {
            desastresConfig.save(new java.io.File(plugin.getDataFolder(), "desastres.yml"));
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("Error guardando desastres.yml: " + e.getMessage());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Rendimiento y SAFE MODE (config.yml)
    // ═══════════════════════════════════════════════════════════════════
    
    // TPS Thresholds (PerformanceAdapter)
    public double getTpsThresholdNormal() {
        return config.getDouble("rendimiento.tps_thresholds.normal", 18.0);
    }
    
    public double getTpsThresholdDegraded() {
        return config.getDouble("rendimiento.tps_thresholds.degraded", 14.0);
    }
    
    public double getTpsThresholdCritical() {
        return config.getDouble("rendimiento.tps_thresholds.critical", 10.0);
    }
    
    public int getSafeModeHoldSecs() {
        return config.getInt("rendimiento.tps_thresholds.safe_mode_hold_secs", 10);
    }
    
    public int getRecoveryHoldSecs() {
        return config.getInt("rendimiento.tps_thresholds.recovery_hold_secs", 10);
    }
    
    // SAFE MODE directo (DisasterController)
    public boolean isSafeModeEnabled() {
        return config.getBoolean("rendimiento.safe_mode.enabled", true);
    }
    
    public double getSafeModeTPSUmbral() {
        return config.getDouble("rendimiento.safe_mode.tps_umbral", 18.0);
    }
    
    public int getSafeModeMinSegundos() {
        return config.getInt("rendimiento.safe_mode.min_segundos", 5);
    }
    
    public boolean isBloquearTNTEnSafe() {
        return config.getBoolean("rendimiento.safe_mode.bloquear_tnt_en_safe", true);
    }
    
    public boolean isBloquearExplosionesDesastres() {
        return config.getBoolean("rendimiento.safe_mode.bloquear_explosiones_desastres", true);
    }
    
    public int getSafeModeAutoSalirSegundos() {
        return config.getInt("rendimiento.safe_mode.auto_salir_segundos", 60);
    }
    
    public boolean isTNTControlEnabled() {
        return config.getBoolean("rendimiento.tnt_control.enabled", true);
    }
    
    public long getTNTVentanaMs() {
        return config.getLong("rendimiento.tnt_control.ventana_ms", 5000L);
    }
    
    public int getTNTMaxExplosiones() {
        return config.getInt("rendimiento.tnt_control.max_explosiones", 20);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // AlonsoLevels - Auto-creación y carga
    // ═══════════════════════════════════════════════════════════════════
    
    private FileConfiguration loadOrCreateAlonsoLevels() {
        File file = new File(plugin.getDataFolder(), "alonsolevels.yml");
        
        // Si no existe, crear con defaults
        if (!file.exists()) {
            try {
                file.createNewFile();
                FileConfiguration defaults = new YamlConfiguration();
                
                // Metadata
                defaults.set("version", 1);
                defaults.set("enabled", true);
                
                // Conversión
                defaults.set("conversion.use_exp", true);
                defaults.set("conversion.exp_factor", 66.67);
                defaults.set("conversion.exp_round", "FLOOR");
                defaults.set("conversion.use_levels", false);
                defaults.set("conversion.level_factor", 0.10);
                defaults.set("conversion.level_round", "FLOOR");
                
                // Multiplicadores por dificultad
                defaults.set("multipliers.enabled", true);
                defaults.set("multipliers.por_dificultad.FACIL", 1.00);
                defaults.set("multipliers.por_dificultad.MEDIA", 1.15);
                defaults.set("multipliers.por_dificultad.DIFICIL", 1.30);
                
                // Multiplicadores por rango
                defaults.set("multipliers_por_rango.enabled", false);
                defaults.set("multipliers_por_rango.NOVATO", 1.00);
                defaults.set("multipliers_por_rango.EXPLORADOR", 1.00);
                defaults.set("multipliers_por_rango.SOBREVIVIENTE", 1.05);
                defaults.set("multipliers_por_rango.VETERANO", 1.10);
                defaults.set("multipliers_por_rango.LEYENDA", 1.15);
                
                // Comandos
                java.util.List<String> cmds = new java.util.ArrayList<>();
                cmds.add("alonsolevels addexp %player% %exp%");
                defaults.set("commands", cmds);
                
                // Log
                defaults.set("log", true);
                
                defaults.save(file);
                plugin.getLogger().info("[Alonso] alonsolevels.yml creado con defaults");
            } catch (java.io.IOException e) {
                plugin.getLogger().severe("[Alonso] Error creando alonsolevels.yml: " + e.getMessage());
            }
        }
        
        // Cargar y retornar
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        int cmdCount = config.getStringList("commands").size();
        plugin.getLogger().info("[Alonso] alonsolevels.yml cargado OK (cmds=" + cmdCount + ")");
        return config;
    }
}
