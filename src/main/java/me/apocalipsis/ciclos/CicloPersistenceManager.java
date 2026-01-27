package me.apocalipsis.ciclos;

import me.apocalipsis.Apocalipsis;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

/**
 * Gestor de persistencia de datos de ciclos
 * Guarda y carga información de todos los ciclos en ciclos_data.yml
 * Proporciona seguridad y recuperación completa después de actualizaciones
 */
public class CicloPersistenceManager {
    
    private final Apocalipsis plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    
    // Mapa de ciclos en memoria
    private final Map<String, CicloData> ciclosData = new HashMap<>();
    
    // Formato de fechas
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public CicloPersistenceManager(Apocalipsis plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "ciclos_data.yml");
        
        // Crear archivo si no existe
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
                plugin.getLogger().info("[CicloPersistence] ✓ Archivo ciclos_data.yml creado");
            } catch (Exception e) {
                plugin.getLogger().severe("[CicloPersistence] ✗ Error al crear ciclos_data.yml");
                e.printStackTrace();
            }
        }
        
        loadData();
    }
    
    // ==================== CARGA DE DATOS ====================
    
    /**
     * Carga todos los datos de ciclos desde el archivo YAML
     */
    public void loadData() {
        try {
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            ciclosData.clear();
            
            if (!dataConfig.contains("ciclos")) {
                plugin.getLogger().info("[CicloPersistence] No hay ciclos guardados");
                return;
            }
            
            ConfigurationSection ciclosSection = dataConfig.getConfigurationSection("ciclos");
            if (ciclosSection == null) {
                return;
            }
            
            int cargados = 0;
            int errores = 0;
            
            for (String worldName : ciclosSection.getKeys(false)) {
                try {
                    CicloData data = loadCicloData(worldName);
                    if (data != null) {
                        ciclosData.put(worldName, data);
                        cargados++;
                        
                        // Verificar si el mundo existe físicamente
                        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
                        data.setExiste(worldFolder.exists());
                        
                        plugin.getLogger().info("[CicloPersistence]   ✓ Cargado: " + worldName + 
                            " (existe=" + data.existe() + ", activo=" + data.isActivo() + ")");
                    }
                } catch (Exception e) {
                    errores++;
                    plugin.getLogger().warning("[CicloPersistence]   ✗ Error al cargar: " + worldName);
                    e.printStackTrace();
                }
            }
            
            plugin.getLogger().info("[CicloPersistence] ═══════════════════════════════════════");
            plugin.getLogger().info("[CicloPersistence] Ciclos cargados: " + cargados);
            if (errores > 0) {
                plugin.getLogger().warning("[CicloPersistence] Errores: " + errores);
            }
            plugin.getLogger().info("[CicloPersistence] ═══════════════════════════════════════");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[CicloPersistence] Error al cargar datos", e);
        }
    }
    
    /**
     * Carga los datos de un ciclo específico desde el YAML
     */
    private CicloData loadCicloData(String worldName) {
        String path = "ciclos." + worldName;
        
        if (!dataConfig.contains(path)) {
            return null;
        }
        
        CicloData data = new CicloData(worldName);
        
        // Estado
        data.setActivo(dataConfig.getBoolean(path + ".activo", false));
        data.setExiste(dataConfig.getBoolean(path + ".existe", false));
        
        // Configuración del mundo
        String envStr = dataConfig.getString(path + ".environment", "NORMAL");
        try {
            data.setEnvironment(World.Environment.valueOf(envStr));
        } catch (Exception e) {
            data.setEnvironment(World.Environment.NORMAL);
        }
        
        String typeStr = dataConfig.getString(path + ".world_type", "NORMAL");
        try {
            data.setWorldType(WorldType.valueOf(typeStr));
        } catch (Exception e) {
            data.setWorldType(WorldType.NORMAL);
        }
        
        String diffStr = dataConfig.getString(path + ".difficulty", "HARD");
        try {
            data.setDifficulty(Difficulty.valueOf(diffStr));
        } catch (Exception e) {
            data.setDifficulty(Difficulty.HARD);
        }
        
        data.setSeed(dataConfig.getLong(path + ".seed", 0));
        data.setGenerateStructures(dataConfig.getBoolean(path + ".generate_structures", true));
        
        // Configuraciones de gameplay
        data.setPvpEnabled(dataConfig.getBoolean(path + ".pvp_enabled", true));
        data.setSpawnMonsters(dataConfig.getBoolean(path + ".spawn_monsters", true));
        data.setSpawnAnimals(dataConfig.getBoolean(path + ".spawn_animals", true));
        data.setKeepSpawnInMemory(dataConfig.getBoolean(path + ".keep_spawn_in_memory", true));
        
        // Spawn location
        double x = dataConfig.getDouble(path + ".spawn.x", 0);
        double y = dataConfig.getDouble(path + ".spawn.y", 64);
        double z = dataConfig.getDouble(path + ".spawn.z", 0);
        data.setSpawnLocation(x, y, z);
        
        // Fechas
        String fechaCreacionStr = dataConfig.getString(path + ".fecha_creacion");
        if (fechaCreacionStr != null) {
            try {
                data.setFechaCreacion(dateFormat.parse(fechaCreacionStr));
            } catch (Exception e) {
                data.setFechaCreacion(new Date());
            }
        }
        
        String ultimaActivacionStr = dataConfig.getString(path + ".ultima_activacion");
        if (ultimaActivacionStr != null) {
            try {
                data.setUltimaActivacion(dateFormat.parse(ultimaActivacionStr));
            } catch (Exception e) {
                // No establecer si no hay activación previa
            }
        }
        
        // Estadísticas
        int jugadores = dataConfig.getInt(path + ".estadisticas.jugadores_unicos", 0);
        int tiempo = dataConfig.getInt(path + ".estadisticas.tiempo_jugado_minutos", 0);
        
        // Establecer estadísticas manualmente
        for (int i = 0; i < jugadores; i++) {
            data.incrementarJugadoresUnicos();
        }
        data.agregarTiempoJugado(tiempo);
        
        return data;
    }
    
    // ==================== GUARDADO DE DATOS ====================
    
    /**
     * Guarda todos los datos de ciclos al archivo YAML
     */
    public void saveData() {
        try {
            // Limpiar sección de ciclos
            dataConfig.set("ciclos", null);
            
            // Guardar metadata
            dataConfig.set("metadata.version", "2.0");
            dataConfig.set("metadata.ultima_actualizacion", dateFormat.format(new Date()));
            dataConfig.set("metadata.total_ciclos", ciclosData.size());
            
            // Guardar cada ciclo
            for (Map.Entry<String, CicloData> entry : ciclosData.entrySet()) {
                saveCicloData(entry.getKey(), entry.getValue());
            }
            
            // Guardar a disco
            dataConfig.save(dataFile);
            
            plugin.getLogger().info("[CicloPersistence] ✓ Datos guardados: " + ciclosData.size() + " ciclos");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[CicloPersistence] ✗ Error al guardar datos", e);
        }
    }
    
    /**
     * Guarda los datos de un ciclo específico al YAML
     */
    private void saveCicloData(String worldName, CicloData data) {
        String path = "ciclos." + worldName;
        
        // Identificadores
        dataConfig.set(path + ".display_name", data.getDisplayName());
        dataConfig.set(path + ".descripcion", data.getDescripcion());
        
        // Estado
        dataConfig.set(path + ".activo", data.isActivo());
        dataConfig.set(path + ".existe", data.existe());
        
        // Configuración del mundo
        dataConfig.set(path + ".environment", data.getEnvironment().name());
        dataConfig.set(path + ".world_type", data.getWorldType().name());
        dataConfig.set(path + ".difficulty", data.getDifficulty().name());
        dataConfig.set(path + ".seed", data.getSeed());
        dataConfig.set(path + ".generate_structures", data.generateStructures());
        
        // Configuraciones de gameplay
        dataConfig.set(path + ".pvp_enabled", data.isPvpEnabled());
        dataConfig.set(path + ".spawn_monsters", data.spawnMonsters());
        dataConfig.set(path + ".spawn_animals", data.spawnAnimals());
        dataConfig.set(path + ".keep_spawn_in_memory", data.keepSpawnInMemory());
        
        // Spawn location
        dataConfig.set(path + ".spawn.x", data.getSpawnX());
        dataConfig.set(path + ".spawn.y", data.getSpawnY());
        dataConfig.set(path + ".spawn.z", data.getSpawnZ());
        
        // Fechas
        dataConfig.set(path + ".fecha_creacion", dateFormat.format(data.getFechaCreacion()));
        if (data.getUltimaActivacion() != null) {
            dataConfig.set(path + ".ultima_activacion", dateFormat.format(data.getUltimaActivacion()));
        }
        
        // Estadísticas
        dataConfig.set(path + ".estadisticas.jugadores_unicos", data.getJugadoresUnicos());
        dataConfig.set(path + ".estadisticas.tiempo_jugado_minutos", data.getTiempoTotalJugado());
    }
    
    // ==================== GESTIÓN DE CICLOS ====================
    
    /**
     * Registra un nuevo ciclo
     */
    public void registerCiclo(String worldName, World world) {
        CicloData data = new CicloData(worldName);
        
        // Capturar datos del mundo
        data.setEnvironment(world.getEnvironment());
        data.setDifficulty(world.getDifficulty());
        data.setSeed(world.getSeed());
        data.setPvpEnabled(world.getPVP());
        data.setSpawnLocation(
            world.getSpawnLocation().getX(),
            world.getSpawnLocation().getY(),
            world.getSpawnLocation().getZ()
        );
        data.setExiste(true);
        
        ciclosData.put(worldName, data);
        saveData();
        
        plugin.getLogger().info("[CicloPersistence] ✓ Ciclo registrado: " + worldName);
    }
    
    /**
     * Actualiza los datos de un ciclo existente
     */
    public void updateCiclo(String worldName, World world) {
        CicloData data = ciclosData.get(worldName);
        if (data == null) {
            // Si no existe, registrarlo
            registerCiclo(worldName, world);
            return;
        }
        
        // Actualizar datos del mundo
        data.setSpawnLocation(
            world.getSpawnLocation().getX(),
            world.getSpawnLocation().getY(),
            world.getSpawnLocation().getZ()
        );
        data.setDifficulty(world.getDifficulty());
        data.setPvpEnabled(world.getPVP());
        data.setExiste(true);
        
        saveData();
    }
    
    /**
     * Marca un ciclo como activo
     */
    public void activarCiclo(String worldName) {
        CicloData data = ciclosData.get(worldName);
        if (data != null) {
            data.setActivo(true);
            saveData();
        }
    }
    
    /**
     * Marca un ciclo como inactivo
     */
    public void desactivarCiclo(String worldName) {
        CicloData data = ciclosData.get(worldName);
        if (data != null) {
            data.setActivo(false);
            saveData();
        }
    }
    
    /**
     * Obtiene los datos de un ciclo
     */
    public CicloData getCicloData(String worldName) {
        return ciclosData.get(worldName);
    }
    
    /**
     * Obtiene todos los ciclos registrados
     */
    public Collection<CicloData> getAllCiclos() {
        return new ArrayList<>(ciclosData.values());
    }
    
    /**
     * Obtiene los nombres de todos los mundos de ciclo
     */
    public Set<String> getCicloWorldNames() {
        return new HashSet<>(ciclosData.keySet());
    }
    
    /**
     * Verifica si un mundo está registrado como ciclo
     */
    public boolean isCiclo(String worldName) {
        return ciclosData.containsKey(worldName);
    }
    
    /**
     * Elimina un ciclo del registro (no elimina el mundo físico)
     */
    public void unregisterCiclo(String worldName) {
        ciclosData.remove(worldName);
        saveData();
        plugin.getLogger().info("[CicloPersistence] ✓ Ciclo eliminado del registro: " + worldName);
    }
    
    // ==================== RECUPERACIÓN Y VALIDACIÓN ====================
    
    /**
     * Valida y repara la integridad de los datos
     * Útil después de actualizaciones o migraciones
     */
    public void validateAndRepair() {
        plugin.getLogger().info("[CicloPersistence] ═══════════════════════════════════════");
        plugin.getLogger().info("[CicloPersistence] Validando integridad de datos...");
        
        int reparados = 0;
        int eliminados = 0;
        
        File worldContainer = Bukkit.getWorldContainer();
        
        for (Map.Entry<String, CicloData> entry : new HashMap<>(ciclosData).entrySet()) {
            String worldName = entry.getKey();
            CicloData data = entry.getValue();
            
            // Verificar si el mundo existe físicamente
            File worldFolder = new File(worldContainer, worldName);
            boolean existeFisicamente = worldFolder.exists();
            
            if (data.existe() != existeFisicamente) {
                plugin.getLogger().info("[CicloPersistence]   → Reparando estado de: " + worldName);
                data.setExiste(existeFisicamente);
                reparados++;
            }
            
            // Si no existe físicamente y nunca fue activado, eliminar del registro
            if (!existeFisicamente && data.getJugadoresUnicos() == 0) {
                plugin.getLogger().warning("[CicloPersistence]   ✗ Eliminando ciclo fantasma: " + worldName);
                ciclosData.remove(worldName);
                eliminados++;
            }
        }
        
        if (reparados > 0 || eliminados > 0) {
            saveData();
            plugin.getLogger().info("[CicloPersistence]   ✓ Reparados: " + reparados);
            plugin.getLogger().info("[CicloPersistence]   ✓ Eliminados: " + eliminados);
        } else {
            plugin.getLogger().info("[CicloPersistence]   ✓ Todos los datos están íntegros");
        }
        
        plugin.getLogger().info("[CicloPersistence] ═══════════════════════════════════════");
    }
    
    /**
     * Genera un reporte de todos los ciclos
     */
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════\n");
        sb.append("REPORTE DE CICLOS\n");
        sb.append("═══════════════════════════════════════\n");
        sb.append("Total de ciclos: ").append(ciclosData.size()).append("\n\n");
        
        for (CicloData data : ciclosData.values()) {
            sb.append("• ").append(data.getWorldName()).append("\n");
            sb.append("  - Activo: ").append(data.isActivo() ? "SÍ" : "NO").append("\n");
            sb.append("  - Existe: ").append(data.existe() ? "SÍ" : "NO").append("\n");
            sb.append("  - Ambiente: ").append(data.getEnvironment()).append("\n");
            sb.append("  - Dificultad: ").append(data.getDifficulty()).append("\n");
            sb.append("  - Jugadores: ").append(data.getJugadoresUnicos()).append("\n");
            sb.append("  - Creado: ").append(dateFormat.format(data.getFechaCreacion())).append("\n");
            sb.append("\n");
        }
        
        sb.append("═══════════════════════════════════════\n");
        return sb.toString();
    }
}
