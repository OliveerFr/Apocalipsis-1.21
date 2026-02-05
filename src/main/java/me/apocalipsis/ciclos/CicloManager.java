package me.apocalipsis.ciclos;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import me.apocalipsis.Apocalipsis;

/**
 * Gestor principal del sistema de ciclos.
 * Coordina todos los componentes para crear mundos independientes
 * donde los jugadores pueden empezar desde cero.
 */
public class CicloManager {
    
    private final Apocalipsis plugin;
    private final File configFile;
    private FileConfiguration config;
    
    // Managers especializados
    private final WorldInventoryManager inventoryManager;
    private final WorldDataManager dataManager;
    private final ItemSanitizer itemSanitizer;
    private final CyclePreviewSystem previewSystem;
    private final CicloPersistenceManager persistenceManager;
    
    // Configuración
    private boolean enabled;
    private String originalWorld;
    private boolean backupBeforeCycle;
    private boolean debugMode;
    
    // Mundos de ciclo registrados
    private final Set<String> cycleWorlds = new HashSet<>();
    
    // Sistema de seguridad: cooldown de teleporte para prevenir spam
    private final Map<UUID, Long> teleportCooldowns = new HashMap<>();
    private static final long TELEPORT_COOLDOWN_MS = 5000; // 5 segundos
    
    public CicloManager(Apocalipsis plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "ciclos.yml");
        
        // Cargar configuración
        loadConfig();
        
        // Inicializar Multiverse-Core
        initializeMultiverse();
        
        // Inicializar managers
        this.inventoryManager = new WorldInventoryManager(plugin);
        this.dataManager = new WorldDataManager(plugin);
        
        // Inicializar sanitizador con materiales bloqueados
        Set<org.bukkit.Material> blockedMaterials = loadBlockedMaterials();
        boolean sanitizeEnabled = config.getBoolean("protecciones.sanitizar_items", true);
        this.itemSanitizer = new ItemSanitizer(blockedMaterials, sanitizeEnabled);
        
        // Inicializar sistema de preview
        this.previewSystem = new CyclePreviewSystem(plugin, this);
        
        // Inicializar sistema de persistencia (NUEVO - Seguridad total)
        this.persistenceManager = new CicloPersistenceManager(plugin);
        
        // Cargar mundos de ciclo desde persistencia
        loadCycleWorlds();
        
        // Validar integridad de datos después de actualización
        persistenceManager.validateAndRepair();
        
        // Iniciar tareas de mantenimiento de seguridad
        startSecurityTasks();
        
        plugin.getLogger().info("[CicloManager] Sistema de ciclos inicializado");
    }
    
    // ==================== GESTIÓN DE CICLOS ====================
    
    /**
     * Inicia tareas programadas de seguridad
     * SEGURIDAD: Validación automática y limpieza de cooldowns
     * AUTO-CORRECCIÓN: Verificación y corrección de spawns inseguros
     */
    private void startSecurityTasks() {
        // Validar integridad de ciclos cada 30 minutos
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            validateSingleActiveCycle();
            cleanupCooldowns();
        }, 36000L, 36000L); // 30 minutos = 36000 ticks
        
        // AUTO-CORRECCIÓN: Verificar spawns cada 1 hora
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            autoCorrectAllCycleSpawns();
        }, 72000L, 72000L); // 1 hora = 72000 ticks
        
        // Verificación inicial de spawns (5 segundos después de iniciar)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getLogger().info("[AUTO-CORRECCIÓN] Ejecutando verificación inicial de spawns...");
            autoCorrectAllCycleSpawns();
        }, 100L); // 5 segundos
        
        plugin.getLogger().info("[SEGURIDAD] Tareas de validación automática iniciadas");
        plugin.getLogger().info("  ├─ Validación de ciclos: cada 30 min");
        plugin.getLogger().info("  ├─ Auto-corrección de spawns: cada 1 hora");
        plugin.getLogger().info("  └─ Verificación inicial: en 5 segundos");
    }
    
    /**
     * Activa un nuevo ciclo en un mundo
     * 
     * @param worldName Nombre del mundo (debe existir en Multiverse)
     * @param teleportAll Si se debe teleportar a todos los jugadores
     * @return true si se activó correctamente
     */
    public boolean activateCycle(String worldName, boolean teleportAll) {
        if (!enabled) {
            plugin.getLogger().warning("[CicloManager] Sistema de ciclos deshabilitado");
            return false;
        }
        
        // Verificar que Multiverse esté instalado
        if (!isMultiverseAvailable()) {
            plugin.getLogger().severe("[CicloManager] Multiverse-Core no está instalado!");
            return false;
        }
        
        // Verificar que el mundo exista
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().severe("[CicloManager] El mundo " + worldName + " no existe!");
            return false;
        }
        
        // Hacer backup si está configurado
        if (backupBeforeCycle) {
            createBackup();
        }
        
        // Registrar el mundo como ciclo
        cycleWorlds.add(worldName);
        
        // NUEVO: Activar en sistema de persistencia
        persistenceManager.activarCiclo(worldName);
        persistenceManager.updateCiclo(worldName, world);
        
        // AUTO-CORRECCIÓN: Verificar y corregir spawn si es necesario
        plugin.getLogger().info("[CicloManager] Verificando seguridad del spawn...");
        autoCorrectWorldSpawn(worldName);
        
        // Actualizar configuración (compatibilidad)
        config.set("ciclos." + worldName + ".activo", true);
        config.set("ciclos." + worldName + ".fecha_creacion", 
                  new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        saveConfig();
        
        plugin.getLogger().info("[CicloManager] Ciclo activado en mundo: " + worldName);
        
        // Teleportar a todos los jugadores si se solicita
        if (teleportAll) {
            teleportAllPlayers(worldName);
        }
        
        return true;
    }
    
    /**
     * Desactiva un ciclo
     */
    public boolean deactivateCycle(String worldName) {
        if (!cycleWorlds.contains(worldName)) {
            return false;
        }
        
        cycleWorlds.remove(worldName);
        config.set("ciclos." + worldName + ".activo", false);
        saveConfig();
        
        plugin.getLogger().info("[CicloManager] Ciclo desactivado en mundo: " + worldName);
        return true;
    }
    
    /**
     * Teleporta a todos los jugadores online a un mundo
     */
    private void teleportAllPlayers(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return;
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            String currentWorld = player.getWorld().getName();
            
            // Guardar datos del mundo actual antes de teleportar
            handlePlayerLeaveWorld(player, currentWorld);
            
            // Teleportar al spawn del nuevo mundo
            player.teleport(world.getSpawnLocation());
            
            // El listener de cambio de mundo manejará la carga de datos
            
            String msg = getMessage("teleport_a_ciclo").replace("{ciclo}", worldName);
            player.sendMessage(msg);
        }
        
        plugin.getLogger().info("[CicloManager] Teleportados todos los jugadores a: " + worldName);
    }
    
    // ==================== MANEJO DE EVENTOS ====================
    
    /**
     * Maneja cuando un jugador cambia de mundo
     */
    public void handleWorldChange(Player player, String fromWorld, String toWorld) {
        UUID uuid = player.getUniqueId();
        
        // 1. Guardar datos del mundo anterior
        handlePlayerLeaveWorld(player, fromWorld);
        
        // 2. Cargar datos del nuevo mundo
        handlePlayerEnterWorld(player, toWorld);
        
        // 3. Mensajes
        String msgSalida = getMessage("cambio_mundo_salida").replace("{mundo}", fromWorld);
        String msgEntrada = getMessage("cambio_mundo_entrada").replace("{mundo}", toWorld);
        
        player.sendMessage(msgSalida);
        player.sendMessage(msgEntrada);
        
        if (debugMode) {
            plugin.getLogger().info("[CicloManager] " + player.getName() + 
                                    " cambió de " + fromWorld + " a " + toWorld);
        }
    }
    
    /**
     * Maneja cuando un jugador se conecta
     * SEGURIDAD: Múltiples validaciones antes de teleportar
     */
    public void handlePlayerJoin(Player player, String worldName) {
        UUID uuid = player.getUniqueId();
        
        // Verificar si hay un ciclo activo diferente al mundo donde está
        String activeCycle = getActiveCycle();
        
        // Si hay un ciclo activo y el jugador NO está en ese ciclo
        if (activeCycle != null && !worldName.equals(activeCycle)) {
            // Verificar si el jugador ya tiene datos en el ciclo activo
            boolean hasDataInActiveCycle = dataManager.hasData(uuid, activeCycle);
            
            // [FIX CRÍTICO] Si el jugador tiene datos en ciclo activo pero está en otro mundo,
            // teleportarlo sin cargar datos del mundo actual (previene pérdida de inventario)
            if (hasDataInActiveCycle) {
                plugin.getLogger().info("[CicloManager] " + player.getName() + 
                    " tiene progreso en ciclo activo '" + activeCycle + "' pero está en '" + worldName + 
                    "'. Teleportando a ciclo activo...");
                
                // Validar que el mundo activo existe
                org.bukkit.World activeWorld = org.bukkit.Bukkit.getWorld(activeCycle);
                if (activeWorld != null) {
                    org.bukkit.Location spawnLoc = activeWorld.getSpawnLocation();
                    
                    // Teleportar después de 1 tick
                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            player.teleport(spawnLoc);
                            player.sendMessage("§e¡Bienvenido de vuelta al ciclo: §b" + activeCycle + "§e!");
                            
                            // Ahora sí cargar datos del ciclo activo
                            handlePlayerEnterWorld(player, activeCycle);
                        }
                    }, 1L);
                    return; // No cargar datos del mundo actual
                } else {
                    plugin.getLogger().severe("[SEGURIDAD] Ciclo activo '" + activeCycle + 
                        "' no existe. Cargando datos del mundo actual.");
                }
            }
            
            // Si es la primera vez en el ciclo activo, teleportarlo
            if (!hasDataInActiveCycle) {
                // SEGURIDAD 1: Verificar cooldown de teleporte
                Long lastTeleport = teleportCooldowns.get(uuid);
                long now = System.currentTimeMillis();
                
                if (lastTeleport != null && (now - lastTeleport) < TELEPORT_COOLDOWN_MS) {
                    long remainingMs = TELEPORT_COOLDOWN_MS - (now - lastTeleport);
                    plugin.getLogger().warning("[SEGURIDAD] " + player.getName() + 
                        " intentó reconexión rápida. Cooldown: " + (remainingMs / 1000) + "s");
                    // Cargar datos normalmente, no teleportar
                    handlePlayerEnterWorld(player, worldName);
                    return;
                }
                
                // SEGURIDAD 2: Validar que el mundo activo existe y está cargado
                org.bukkit.World activeWorld = org.bukkit.Bukkit.getWorld(activeCycle);
                if (activeWorld == null) {
                    plugin.getLogger().severe("[SEGURIDAD] Ciclo activo '" + activeCycle + 
                        "' no existe o no está cargado. Abortando teleporte de " + player.getName());
                    handlePlayerEnterWorld(player, worldName);
                    return;
                }
                
                // SEGURIDAD 3: Validar spawn seguro
                org.bukkit.Location spawnLoc = activeWorld.getSpawnLocation();
                if (!isLocationSafe(spawnLoc)) {
                    plugin.getLogger().warning("[SEGURIDAD] Spawn de '" + activeCycle + 
                        "' no es seguro. Intentando encontrar ubicación segura...");
                    spawnLoc = findSafeLocation(activeWorld, spawnLoc);
                    
                    if (spawnLoc == null) {
                        plugin.getLogger().severe("[SEGURIDAD] No se encontró spawn seguro en '" + 
                            activeCycle + "'. Abortando teleporte de " + player.getName());
                        handlePlayerEnterWorld(player, worldName);
                        return;
                    }
                }
                
                // Logging completo para auditoría
                plugin.getLogger().info("[CicloManager] TELEPORTE AUTORIZADO: " + player.getName() + 
                    " (" + uuid + ") desde '" + worldName + "' → '" + activeCycle + "'");
                plugin.getLogger().info("  └─ Razón: Primera vez en ciclo activo");
                plugin.getLogger().info("  └─ Destino: " + spawnLoc.getBlockX() + ", " + 
                    spawnLoc.getBlockY() + ", " + spawnLoc.getBlockZ());
                
                // Registrar cooldown
                teleportCooldowns.put(uuid, now);
                
                // Teleportar después de 1 tick para evitar conflictos con el login
                final org.bukkit.Location finalSpawn = spawnLoc;
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // SEGURIDAD 4: Verificar que el jugador sigue conectado
                    if (!player.isOnline()) {
                        plugin.getLogger().warning("[SEGURIDAD] " + player.getName() + 
                            " se desconectó antes de teleporte a ciclo activo");
                        return;
                    }
                    
                    player.teleport(finalSpawn);
                    player.sendMessage("§e¡Bienvenido al ciclo activo: §b" + activeCycle + "§e!");
                    player.sendMessage("§7Empiezas desde cero en este nuevo mundo.");
                    
                    // Log de confirmación
                    plugin.getLogger().info("[CicloManager] ✓ Teleporte completado: " + player.getName());
                }, 1L);
                
                return; // No cargar datos del mundo anterior
            }
        }
        
        // Cargar inventario y datos para el mundo actual
        handlePlayerEnterWorld(player, worldName);
        
        if (debugMode) {
            plugin.getLogger().info("[CicloManager] " + player.getName() + 
                                    " se conectó en mundo: " + worldName);
        }
    }
    
    /**
     * Maneja cuando un jugador se desconecta
     */
    public void handlePlayerQuit(Player player, String worldName) {
        // Guardar inventario y datos del mundo actual
        handlePlayerLeaveWorld(player, worldName);
        
        if (debugMode) {
            plugin.getLogger().info("[CicloManager] " + player.getName() + 
                                    " se desconectó del mundo: " + worldName);
        }
    }
    
    /**
     * Procesa la salida de un jugador de un mundo
     */
    public void handlePlayerLeaveWorld(Player player, String worldName) {
        UUID uuid = player.getUniqueId();
        
        // 1. Sanitizar inventario (eliminar items problemáticos)
        ItemSanitizer.SanitizeResult result = itemSanitizer.sanitizeInventory(
            player.getInventory().getContents()
        );
        
        if (result.hadProblematicItems()) {
            String msg = getMessage("item_bloqueado") + " §7(x" + result.getTotalRemoved() + ")";
            player.sendMessage(msg);
            
            plugin.getLogger().warning("[CicloManager] Removidos " + result.getTotalRemoved() + 
                                       " items problemáticos de " + player.getName());
        }
        
        // 2. Guardar inventario
        inventoryManager.saveInventory(player, worldName);
        
        // 3. Capturar y guardar datos de progreso
        WorldDataManager.PlayerProgressData data = dataManager.captureCurrentState(uuid);
        dataManager.savePlayerData(uuid, worldName, data);
        
        // 4. Guardar archivos
        inventoryManager.saveData();
        dataManager.saveData();
    }
    
    /**
     * Procesa la entrada de un jugador a un mundo
     */
    private void handlePlayerEnterWorld(Player player, String worldName) {
        UUID uuid = player.getUniqueId();
        
        // [FIX] Si el jugador NO tiene inventario guardado en este mundo, 
        // guardar su inventario actual ANTES de cargar (previene pérdida de inventario)
        if (!inventoryManager.hasInventory(uuid, worldName)) {
            plugin.getLogger().info("[CicloManager] Primera vez de " + player.getName() + 
                                    " en mundo '" + worldName + "'. Guardando inventario actual primero.");
            inventoryManager.saveInventory(player, worldName);
        }
        
        // 1. Cargar inventario
        inventoryManager.loadInventory(player, worldName);
        
        // 2. Cargar datos de progreso
        WorldDataManager.PlayerProgressData data = dataManager.loadPlayerData(uuid, worldName);
        dataManager.applyStateToServices(uuid, data);
        
        // 3. Si es un mundo de ciclo y es primera vez, crear datos frescos
        if (isCycleWorld(worldName) && !dataManager.hasData(uuid, worldName)) {
            // Capturar estado actual para preservar datos globales (rangos/skills si está configurado)
            WorldDataManager.PlayerProgressData currentData = dataManager.captureCurrentState(uuid);
            WorldDataManager.PlayerProgressData freshData = dataManager.createFreshData(currentData);
            dataManager.savePlayerData(uuid, worldName, freshData);
            dataManager.applyStateToServices(uuid, freshData);
            
            String msg = getMessage("datos_reseteados");
            player.sendMessage(msg);
            
            plugin.getLogger().info("[CicloManager] Creados datos frescos para " + 
                                    player.getName() + " en ciclo: " + worldName);
        }
    }
    
    // ==================== UTILIDADES ====================
    
    /**
     * Verifica si un mundo es un mundo de ciclo
     */
    public boolean isCycleWorld(String worldName) {
        return cycleWorlds.contains(worldName);
    }
    
    /**
     * Obtiene los datos de un ciclo específico
     * @param worldName Nombre del mundo
     * @return Datos del ciclo o null si no existe
     */
    public CicloData getCicloData(String worldName) {
        return persistenceManager.getCicloData(worldName);
    }
    
    /**
     * Obtiene todos los ciclos registrados
     * @return Colección de todos los datos de ciclos
     */
    public Collection<CicloData> getAllCiclos() {
        return persistenceManager.getAllCiclos();
    }
    
    /**
     * Obtiene el ciclo activo más reciente (prioridad por última activación)
     * MEJORADO: Retorna el ciclo activo más recientemente activado
     * SEGURIDAD: Valida que solo haya un ciclo activo
     * @return Nombre del mundo del ciclo activo más reciente, o null si no hay ninguno
     */
    public String getActiveCycle() {
        CicloData mostRecentCycle = null;
        int activeCycleCount = 0;
        
        for (CicloData ciclo : persistenceManager.getAllCiclos()) {
            if (ciclo.isActivo()) {
                activeCycleCount++;
                
                // Seleccionar el más reciente basado en última activación
                if (mostRecentCycle == null) {
                    mostRecentCycle = ciclo;
                } else if (ciclo.getUltimaActivacion() != null) {
                    // Si el ciclo actual tiene fecha de activación más reciente
                    if (mostRecentCycle.getUltimaActivacion() == null || 
                        ciclo.getUltimaActivacion().after(mostRecentCycle.getUltimaActivacion())) {
                        mostRecentCycle = ciclo;
                    }
                }
            }
        }
        
        // SEGURIDAD: Advertir si hay múltiples ciclos activos (inconsistencia)
        if (activeCycleCount > 1) {
            plugin.getLogger().warning("[SEGURIDAD] Detectados " + activeCycleCount + 
                " ciclos activos simultáneos. Debe haber solo uno. Usando el más reciente: " +
                (mostRecentCycle != null ? mostRecentCycle.getWorldName() : "null"));
        }
        
        return mostRecentCycle != null ? mostRecentCycle.getWorldName() : null;
    }
    
    /**
     * Genera un reporte completo de todos los ciclos
     * @return String con el reporte formateado
     */
    public String generateCiclosReport() {
        return persistenceManager.generateReport();
    }
    
    /**
     * Valida y repara la integridad de los datos de ciclos
     * Útil después de actualizaciones o migraciones
     */
    public void validateCiclosIntegrity() {
        persistenceManager.validateAndRepair();
    }
    
    /**
     * Establece el spawn de un mundo en una ubicación específica
     * SEGURIDAD: Valida que la ubicación sea segura antes de establecer
     * 
     * @param worldName Nombre del mundo
     * @param location Nueva ubicación del spawn
     * @return true si se actualizó correctamente
     */
    public boolean setSpawn(String worldName, Location location) {
        // SEGURIDAD 1: Validar mundo
        World world = Bukkit.getWorld(worldName);
        
        if (world == null) {
            plugin.getLogger().warning("[SEGURIDAD] No se puede setear spawn: mundo '" + worldName + "' no cargado");
            return false;
        }
        
        // SEGURIDAD 2: Validar que la ubicación pertenece al mundo correcto
        if (location.getWorld() == null || !location.getWorld().getName().equals(worldName)) {
            plugin.getLogger().warning("[SEGURIDAD] Ubicación no pertenece al mundo '" + worldName + "'");
            return false;
        }
        
        // SEGURIDAD 3: Validar que el spawn es seguro
        if (!isLocationSafe(location)) {
            plugin.getLogger().warning("[SEGURIDAD] Ubicación de spawn no es segura en '" + worldName + "'");
            plugin.getLogger().warning("  └─ Coordenadas: " + location.getBlockX() + ", " + 
                location.getBlockY() + ", " + location.getBlockZ());
            
            // Intentar encontrar ubicación segura cercana
            Location safeLoc = findSafeLocation(world, location);
            if (safeLoc != null) {
                plugin.getLogger().info("  └─ Usando spawn seguro alternativo: " + safeLoc.getBlockX() + 
                    ", " + safeLoc.getBlockY() + ", " + safeLoc.getBlockZ());
                location = safeLoc;
            } else {
                plugin.getLogger().severe("[SEGURIDAD] No se encontró spawn seguro. Operación cancelada.");
                return false;
            }
        }
        
        try {
            // Actualizar spawn del mundo en memoria
            world.setSpawnLocation(location);
            
            plugin.getLogger().info("[CicloManager] ✓ Spawn actualizado para: " + worldName);
            plugin.getLogger().info("[CicloManager]   → Ubicación: " + 
                location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
            
            // Actualizar en sistema de persistencia
            CicloData data = persistenceManager.getCicloData(worldName);
            if (data != null) {
                data.setSpawnLocation(location.getX(), location.getY(), location.getZ());
                persistenceManager.saveData();
                plugin.getLogger().info("[CicloManager]   ✓ Spawn guardado en persistencia");
            }
            
            // Actualizar en ciclos.yml (compatibilidad)
            config.set("ciclos." + worldName + ".spawn.x", location.getX());
            config.set("ciclos." + worldName + ".spawn.y", location.getY());
            config.set("ciclos." + worldName + ".spawn.z", location.getZ());
            saveConfig();
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().severe("[CicloManager] ✗ Error al setear spawn: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Verifica si Multiverse-Core está disponible (OBSOLETO)
     * El sistema ya no requiere Multiverse - usa creación nativa de Bukkit
     * @return Siempre true
     */
    public boolean isMultiverseAvailable() {
        return true; // Sistema nativo, no requiere Multiverse
    }
    
    /**
     * Crea un backup de los datos importantes
     */
    private void createBackup() {
        try {
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File backupDir = new File(plugin.getDataFolder(), "backups");
            backupDir.mkdirs();
            
            // Backup de mission_data.yml
            File missionFile = new File(plugin.getDataFolder(), "mission_data.yml");
            if (missionFile.exists()) {
                File missionBackup = new File(backupDir, "mission_data_" + timestamp + ".yml");
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(missionFile)
                    .save(missionBackup);
            }
            
            // Backup de skill_data.yml
            File skillFile = new File(plugin.getDataFolder(), "skill_data.yml");
            if (skillFile.exists()) {
                File skillBackup = new File(backupDir, "skill_data_" + timestamp + ".yml");
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(skillFile)
                    .save(skillBackup);
            }
            
            plugin.getLogger().info("[CicloManager] Backup creado: " + timestamp);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[CicloManager] Error al crear backup", e);
        }
    }
    
    /**
     * Obtiene un mensaje traducido de la configuración
     */
    public String getMessage(String key) {
        String prefix = config.getString("config.prefix", "§8[§bCiclo§8]§r");
        String msg = config.getString("mensajes." + key, key);
        return msg.replace("{prefix}", prefix).replace("&", "§");
    }
    
    /**
     * Obtiene la lista de mundos de ciclo activos
     */
    public Set<String> getCycleWorlds() {
        return new HashSet<>(cycleWorlds);
    }
    
    /**
     * Obtiene el mundo original/principal
     */
    public String getOriginalWorld() {
        return originalWorld;
    }
    
    /**
     * Obtiene la configuración de ciclos
     */
    public FileConfiguration getCiclosConfig() {
        return config;
    }
    
    // ==================== CONFIGURACIÓN ====================
    
    private void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("ciclos.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Cargar configuración básica
        enabled = config.getBoolean("config.enabled", true);
        originalWorld = config.getString("config.mundo_original", "world");
        backupBeforeCycle = config.getBoolean("config.backup_before_cycle", true);
        debugMode = config.getBoolean("config.debug", false);
        
        plugin.getLogger().info("[CicloManager] Configuración cargada: enabled=" + enabled + 
                                ", original=" + originalWorld);
    }
    
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[CicloManager] Error al guardar configuración", e);
        }
    }
    
    private void loadCycleWorlds() {
        cycleWorlds.clear();
        
        // NUEVO: Cargar desde sistema de persistencia
        Set<String> ciclosRegistrados = persistenceManager.getCicloWorldNames();
        
        if (ciclosRegistrados.isEmpty()) {
            // Fallback: Intentar cargar desde ciclos.yml antiguo (migración)
            if (config.contains("ciclos")) {
                for (String worldName : config.getConfigurationSection("ciclos").getKeys(false)) {
                    String tipo = config.getString("ciclos." + worldName + ".tipo", "");
                    if (tipo.equals("ciclo")) {
                        cycleWorlds.add(worldName);
                        plugin.getLogger().info("[CicloManager] Migrando ciclo: " + worldName);
                        
                        // Migrar a nuevo sistema si el mundo existe
                        World world = Bukkit.getWorld(worldName);
                        if (world != null) {
                            persistenceManager.registerCiclo(worldName, world);
                        }
                    }
                }
            }
            return;
        }
        
        // Cargar todos los ciclos del sistema de persistencia
        for (String worldName : ciclosRegistrados) {
            CicloData data = persistenceManager.getCicloData(worldName);
            if (data != null && data.existe()) {
                cycleWorlds.add(worldName);
                plugin.getLogger().info("[CicloManager] ✓ Ciclo cargado: " + worldName + 
                    " (activo=" + data.isActivo() + ", jugadores=" + data.getJugadoresUnicos() + ")");
                
                // Cargar el mundo si está activo
                if (data.isActivo() && Bukkit.getWorld(worldName) == null) {
                    loadExistingWorld(worldName, data);
                }
            }
        }
    }
    
    private Set<org.bukkit.Material> loadBlockedMaterials() {
        Set<org.bukkit.Material> materials = new HashSet<>();
        
        List<String> list = config.getStringList("protecciones.materiales_bloqueados");
        for (String materialName : list) {
            try {
                org.bukkit.Material material = org.bukkit.Material.valueOf(materialName);
                materials.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[CicloManager] Material inválido: " + materialName);
            }
        }
        
        return materials;
    }
    
    // ==================== SHUTDOWN ====================
    
    public void shutdown() {
        // Guardar todos los datos
        inventoryManager.saveData();
        dataManager.saveData();
        
        // NUEVO: Guardar persistencia de ciclos
        persistenceManager.saveData();
        
        saveConfig();
        
        plugin.getLogger().info("[CicloManager] Sistema de ciclos desactivado");
    }
    
    // ==================== MULTIVERSE INTEGRATION ====================
    
    /**
     * Inicializa la integración con Multiverse-Core (OPCIONAL)
     * El sistema funciona perfectamente SIN Multiverse usando Bukkit nativo
     */
    private void initializeMultiverse() {
        // Sistema de creación nativa - NO requiere Multiverse
        plugin.getLogger().info("[CicloManager] ✓ Creación de mundos: Sistema NATIVO (Bukkit)");
        plugin.getLogger().info("[CicloManager] ✓ No se requiere Multiverse-Core");
    }
    
    /**
     * Carga un mundo existente desde disco usando datos guardados
     * Útil para recuperar ciclos después de actualizaciones del plugin
     * 
     * @param worldName Nombre del mundo a cargar
     * @param data Datos del ciclo guardados
     * @return true si se cargó exitosamente
     */
    private boolean loadExistingWorld(String worldName, CicloData data) {
        try {
            plugin.getLogger().info("[CicloManager] ═══════════════════════════════════════");
            plugin.getLogger().info("[CicloManager] Cargando mundo existente: " + worldName);
            
            // Verificar que la carpeta del mundo exista
            File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
            if (!worldFolder.exists()) {
                plugin.getLogger().warning("[CicloManager] ✗ Carpeta del mundo no existe");
                return false;
            }
            
            // Crear WorldCreator con los datos guardados
            WorldCreator creator = new WorldCreator(worldName);
            creator.environment(data.getEnvironment());
            creator.type(data.getWorldType());
            
            // Cargar el mundo
            World world = creator.createWorld();
            
            if (world == null) {
                plugin.getLogger().severe("[CicloManager] ✗ Error: No se pudo cargar el mundo");
                return false;
            }
            
            plugin.getLogger().info("[CicloManager]   ✓ Mundo cargado desde disco");
            
            // Aplicar configuraciones guardadas
            world.setDifficulty(data.getDifficulty());
            world.setPVP(data.isPvpEnabled());
            world.setKeepSpawnInMemory(data.keepSpawnInMemory());
            world.setSpawnFlags(data.spawnMonsters(), data.spawnAnimals());
            world.setAutoSave(true);
            
            // Restaurar spawn location si está guardado
            if (data.getSpawnY() > 0) {
                Location spawnLoc = new Location(world, data.getSpawnX(), data.getSpawnY(), data.getSpawnZ());
                world.setSpawnLocation(spawnLoc);
                plugin.getLogger().info("[CicloManager]   ✓ Spawn restaurado: " + 
                    (int)data.getSpawnX() + ", " + (int)data.getSpawnY() + ", " + (int)data.getSpawnZ());
            }
            
            // Vincular portales
            linkWorldPortals(worldName, data.getEnvironment());
            
            plugin.getLogger().info("[CicloManager]   ✓ Configuraciones aplicadas");
            plugin.getLogger().info("[CicloManager]   ✓ Seed: " + world.getSeed());
            plugin.getLogger().info("[CicloManager] ═══════════════════════════════════════");
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().severe("[CicloManager] ✗ Error al cargar mundo: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Crea un nuevo mundo usando Bukkit nativo (NO requiere Multiverse)
     * 
     * @param worldName Nombre del mundo a crear
     * @param environment Tipo de ambiente (NORMAL, NETHER, THE_END)
     * @param worldType Tipo de generación (NORMAL, FLAT, LARGE_BIOMES, AMPLIFIED)
     * @param difficulty Dificultad del mundo
     * @param generateStructures Si se generan estructuras
     * @return true si el mundo se creó exitosamente
     */
    public boolean createCycleWorld(String worldName, World.Environment environment, 
                                    WorldType worldType, Difficulty difficulty, 
                                    boolean generateStructures) {
        
        // Verificar que el mundo no exista ya
        if (Bukkit.getWorld(worldName) != null) {
            plugin.getLogger().warning("[CicloManager] El mundo '" + worldName + "' ya existe");
            return false;
        }
        
        try {
            plugin.getLogger().info("[CicloManager] ═══════════════════════════════════════");
            plugin.getLogger().info("[CicloManager] Creando mundo: " + worldName);
            plugin.getLogger().info("[CicloManager]   → Ambiente: " + environment.name());
            plugin.getLogger().info("[CicloManager]   → Tipo: " + worldType.name());
            plugin.getLogger().info("[CicloManager]   → Dificultad: " + difficulty.name());
            plugin.getLogger().info("[CicloManager]   → Estructuras: " + (generateStructures ? "Sí" : "No"));
            
            // Crear el mundo usando Bukkit nativo (NO requiere Multiverse)
            WorldCreator creator = new WorldCreator(worldName);
            creator.environment(environment);
            creator.type(worldType);
            creator.generateStructures(generateStructures);
            
            // Crear el mundo
            World world = creator.createWorld();
            
            if (world == null) {
                plugin.getLogger().severe("[CicloManager] ✗ Error: El mundo no se pudo crear");
                return false;
            }
            
            plugin.getLogger().info("[CicloManager]   ✓ Mundo cargado en memoria");
            plugin.getLogger().info("[CicloManager]   ✓ Carpeta del mundo: " + world.getWorldFolder().getAbsolutePath());
            
            // Forzar generación de spawn chunks para crear la carpeta física
            Location spawnLoc = world.getSpawnLocation();
            world.getChunkAt(spawnLoc).load(true);
            plugin.getLogger().info("[CicloManager]   ✓ Chunks de spawn generados");
            
            // Configurar propiedades del mundo
            world.setDifficulty(difficulty);
            world.setKeepSpawnInMemory(true);
            world.setAutoSave(true);
            world.setPVP(true);
            world.setSpawnFlags(true, true); // Mobs y animales
            
            // Establecer spawn seguro para el nuevo mundo
            Location safeSpawn = findSafeSpawnLocation(world);
            if (safeSpawn != null) {
                world.setSpawnLocation(safeSpawn);
                plugin.getLogger().info("[CicloManager]   ✓ Spawn configurado: " + 
                    safeSpawn.getBlockX() + ", " + safeSpawn.getBlockY() + ", " + safeSpawn.getBlockZ());
            }
            
            // Vincular portales Nether/End al nuevo ciclo
            linkWorldPortals(worldName, environment);
            
            // NUEVO: Registrar en sistema de persistencia
            persistenceManager.registerCiclo(worldName, world);
            cycleWorlds.add(worldName);
            
            // Mantener compatibilidad con ciclos.yml
            config.set("ciclos." + worldName + ".nombre_display", "&6" + worldName);
            config.set("ciclos." + worldName + ".descripcion", "Ciclo creado automáticamente");
            config.set("ciclos." + worldName + ".tipo", "ciclo");
            config.set("ciclos." + worldName + ".activo", false);
            config.set("ciclos." + worldName + ".fecha_creacion", 
                      new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            config.set("ciclos." + worldName + ".generador", worldType.name());
            config.set("ciclos." + worldName + ".dificultad", difficulty.name());
            config.set("ciclos." + worldName + ".pvp_enabled", true);
            config.set("ciclos." + worldName + ".spawn_monsters", true);
            config.set("ciclos." + worldName + ".spawn_animals", true);
            saveConfig();
            
            plugin.getLogger().info("[CicloManager] ✓ Mundo creado exitosamente");
            plugin.getLogger().info("[CicloManager]   ✓ Spawn: " + world.getSpawnLocation());
            plugin.getLogger().info("[CicloManager]   ✓ Seed: " + world.getSeed());
            plugin.getLogger().info("[CicloManager] ═══════════════════════════════════════");
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().severe("[CicloManager] ✗ Error al crear mundo: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Encuentra una ubicación segura para el spawn del mundo
     * Busca terreno sólido cerca del spawn natural
     */
    private Location findSafeSpawnLocation(World world) {
        try {
            // Obtener spawn natural del mundo
            Location naturalSpawn = world.getSpawnLocation();
            
            // Para mundos normales, buscar terreno seguro
            if (world.getEnvironment() == World.Environment.NORMAL) {
                // Buscar desde el spawn natural
                int startX = naturalSpawn.getBlockX();
                int startZ = naturalSpawn.getBlockZ();
                
                // Buscar en un radio de 100 bloques
                for (int radius = 0; radius < 100; radius += 10) {
                    for (int angle = 0; angle < 360; angle += 45) {
                        int x = startX + (int)(radius * Math.cos(Math.toRadians(angle)));
                        int z = startZ + (int)(radius * Math.sin(Math.toRadians(angle)));
                        
                        // Buscar Y seguro (de abajo hacia arriba)
                        for (int y = world.getMinHeight() + 1; y < world.getMaxHeight() - 2; y++) {
                            Location testLoc = new Location(world, x + 0.5, y, z + 0.5);
                            
                            // Verificar que sea seguro
                            if (isSafeSpawnLocation(testLoc)) {
                                plugin.getLogger().info("[CicloManager]   → Spawn seguro encontrado en búsqueda");
                                return testLoc;
                            }
                        }
                    }
                }
                
                // Si no se encontró, usar highestBlockAt
                Location highestBlock = world.getHighestBlockAt(startX, startZ).getLocation().add(0, 1, 0);
                if (isSafeSpawnLocation(highestBlock)) {
                    return highestBlock;
                }
            }
            
            // Para Nether/End o si no se encontró seguro, usar spawn natural
            return naturalSpawn;
            
        } catch (Exception e) {
            plugin.getLogger().warning("[CicloManager] Error buscando spawn seguro: " + e.getMessage());
            return world.getSpawnLocation();
        }
    }
    
    /**
     * Verifica si una ubicación es segura para spawn
     */
    private boolean isSafeSpawnLocation(Location loc) {
        try {
            Material blockBelow = loc.clone().subtract(0, 1, 0).getBlock().getType();
            Material blockAt = loc.getBlock().getType();
            Material blockAbove = loc.clone().add(0, 1, 0).getBlock().getType();
            
            // Debe tener bloque sólido abajo
            if (!blockBelow.isSolid() || blockBelow == Material.LAVA || blockBelow == Material.MAGMA_BLOCK) {
                return false;
            }
            
            // No debe tener bloques peligrosos
            if (blockBelow == Material.CACTUS || blockBelow == Material.FIRE || 
                blockBelow == Material.SOUL_FIRE || blockBelow == Material.SWEET_BERRY_BUSH) {
                return false;
            }
            
            // Espacio para jugador debe estar vacío o con aire/plantas
            if (blockAt.isSolid() || blockAbove.isSolid()) {
                return false;
            }
            
            // No spawn en agua (preferiblemente)
            if (blockAt == Material.WATER || blockAbove == Material.WATER) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Vincula los portales Nether/End al ciclo correcto
     * Crea los mundos asociados si no existen
     */
    private void linkWorldPortals(String overworldName, World.Environment environment) {
        try {
            // Solo procesar si es un Overworld
            if (environment != World.Environment.NORMAL) {
                plugin.getLogger().info("[CicloManager]   → Mundo no es Overworld, omitiendo vinculación de portales");
                return;
            }
            
            World overworld = Bukkit.getWorld(overworldName);
            if (overworld == null) {
                return;
            }
            
            plugin.getLogger().info("[CicloManager]   → Vinculando portales para: " + overworldName);
            
            // Nombres de mundos asociados
            String netherName = overworldName + "_nether";
            String endName = "world_the_end"; // End compartido para TODOS los ciclos
            
            // Crear Nether asociado (cada ciclo tiene su propio Nether)
            World nether = Bukkit.getWorld(netherName);
            if (nether == null) {
                plugin.getLogger().info("[CicloManager]     → Creando Nether: " + netherName);
                WorldCreator netherCreator = new WorldCreator(netherName);
                netherCreator.environment(World.Environment.NETHER);
                nether = netherCreator.createWorld();
                
                if (nether != null) {
                    nether.setKeepSpawnInMemory(true);
                    plugin.getLogger().info("[CicloManager]     ✓ Nether creado: " + netherName);
                }
            }
            
            // Verificar/crear End compartido (UNO para todos los ciclos)
            World end = Bukkit.getWorld(endName);
            if (end == null) {
                plugin.getLogger().info("[CicloManager]     → Creando End compartido: " + endName);
                WorldCreator endCreator = new WorldCreator(endName);
                endCreator.environment(World.Environment.THE_END);
                end = endCreator.createWorld();
                
                if (end != null) {
                    end.setKeepSpawnInMemory(true);
                    plugin.getLogger().info("[CicloManager]     ✓ End compartido creado: " + endName);
                }
            }
            
            // Guardar configuración de mundos vinculados
            config.set("ciclos." + overworldName + ".nether_world", netherName);
            config.set("ciclos." + overworldName + ".end_world", endName); // Mismo End para todos
            saveConfig();
            
            plugin.getLogger().info("[CicloManager]   ✓ Portales vinculados:");
            plugin.getLogger().info("[CicloManager]     - Overworld: " + overworldName);
            plugin.getLogger().info("[CicloManager]     - Nether: " + netherName + " (aislado)");
            plugin.getLogger().info("[CicloManager]     - End: " + endName + " (COMPARTIDO - protegido)");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[CicloManager] Error vinculando portales: " + e.getMessage(), e);
        }
    }
    
    /**
     *      plugin.getLogger().info("[CicloManager] ═══════════════════════════════════════");
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[CicloManager] ✗ Error al crear mundo: " + worldName, e);
            return false;
        }
    }
    
    /**
     * Crea y activa un ciclo completo (crea mundo + activa ciclo)
     * 
     * @param worldName Nombre del mundo
     * @param environment Ambiente (NORMAL, NETHER, THE_END)
     * @param difficulty Dificultad
     * @param teleportAll Si teleportar a todos los jugadores
     * @return true si se creó y activó correctamente
     */
    public boolean createAndActivateCycle(String worldName, World.Environment environment, 
                                          Difficulty difficulty, boolean teleportAll) {
        
        // 1. Crear el mundo
        if (!createCycleWorld(worldName, environment, WorldType.NORMAL, difficulty, true)) {
            return false;
        }
        
        // 2. Activar como ciclo
        return activateCycle(worldName, teleportAll);
    }
    
    // ==================== GETTERS ====================
    
    /**
     * Verifica si el sistema de ciclos está habilitado
     * @return true si está habilitado en la configuración
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Obtiene el WorldDataManager
     * @return WorldDataManager
     */
    public WorldDataManager getDataManager() {
        return dataManager;
    }
    
    /**
     * Obtiene el WorldInventoryManager
     * @return WorldInventoryManager
     */
    public WorldInventoryManager getInventoryManager() {
        return inventoryManager;
    }
    
    /**
     * Obtiene el CyclePreviewSystem
     * @return CyclePreviewSystem
     */
    public CyclePreviewSystem getPreviewSystem() {
        return previewSystem;
    }
    
    /**
     * Obtiene la configuración de ciclos.yml
     * @return FileConfiguration
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    // ==================== MÉTODOS DE SEGURIDAD ====================
    
    /**
     * Verifica si una ubicación es segura para teleportar/spawn
     * SEGURIDAD: Previene spawns en lava, void, bloques sólidos, etc.
     * 
     * @param location Ubicación a verificar
     * @return true si es segura, false si es peligrosa
     */
    private boolean isLocationSafe(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        // Verificar límites del mundo
        if (y < world.getMinHeight() || y > world.getMaxHeight() - 2) {
            return false;
        }
        
        // Verificar void (caída infinita)
        if (y < world.getMinHeight() + 5) {
            return false;
        }
        
        // Obtener bloques relevantes
        Material ground = world.getBlockAt(x, y - 1, z).getType();
        Material feet = world.getBlockAt(x, y, z).getType();
        Material head = world.getBlockAt(x, y + 1, z).getType();
        
        // Verificar materiales peligrosos en el suelo
        if (ground == Material.LAVA || ground == Material.FIRE || 
            ground == Material.MAGMA_BLOCK || ground == Material.CAMPFIRE ||
            ground == Material.SOUL_CAMPFIRE || ground == Material.SOUL_FIRE) {
            return false;
        }
        
        // Verificar que los pies y cabeza estén despejados
        if (feet.isSolid() || head.isSolid()) {
            return false;
        }
        
        // Verificar materiales peligrosos en pies/cabeza
        if (feet == Material.LAVA || feet == Material.FIRE || 
            head == Material.LAVA || head == Material.FIRE) {
            return false;
        }
        
        // Verificar que haya suelo sólido (no aire/agua)
        if (!ground.isSolid() && ground != Material.WATER && ground != Material.BARRIER) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Busca una ubicación segura cerca de la ubicación dada
     * SEGURIDAD: Intenta encontrar spawn alternativo en caso de ubicación peligrosa
     * AUTO-CORRECCIÓN: Búsqueda extendida hasta 50 bloques + búsqueda en superficie
     * 
     * @param world Mundo donde buscar
     * @param center Ubicación central de búsqueda
     * @return Ubicación segura encontrada, o null si no se encontró ninguna
     */
    private Location findSafeLocation(World world, Location center) {
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        
        // FASE 1: Buscar en espiral cerca (radio 10 bloques)
        for (int radius = 0; radius <= 10; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    
                    // Buscar verticalmente (+/- 5 bloques)
                    for (int dy = -5; dy <= 5; dy++) {
                        int checkY = centerY + dy;
                        
                        if (checkY < world.getMinHeight() || checkY > world.getMaxHeight() - 2) {
                            continue;
                        }
                        
                        Location checkLoc = new Location(world, centerX + dx, checkY, centerZ + dz);
                        
                        if (isLocationSafe(checkLoc)) {
                            plugin.getLogger().info("[AUTO-CORRECCIÓN] Spawn seguro encontrado (FASE 1) a " + 
                                radius + " bloques del original");
                            return checkLoc;
                        }
                    }
                }
            }
        }
        
        // FASE 2: Búsqueda extendida (radio 50 bloques) - Solo en superficie
        plugin.getLogger().warning("[AUTO-CORRECCIÓN] FASE 1 falló. Iniciando búsqueda extendida...");
        for (int radius = 15; radius <= 50; radius += 5) {
            for (int angle = 0; angle < 360; angle += 30) {
                double radians = Math.toRadians(angle);
                int dx = (int) (radius * Math.cos(radians));
                int dz = (int) (radius * Math.sin(radians));
                
                // Buscar desde superficie hacia abajo
                Location surfaceLoc = world.getHighestBlockAt(centerX + dx, centerZ + dz).getLocation().add(0, 1, 0);
                
                if (isLocationSafe(surfaceLoc)) {
                    plugin.getLogger().info("[AUTO-CORRECCIÓN] Spawn seguro encontrado (FASE 2) a " + 
                        radius + " bloques en superficie");
                    return surfaceLoc;
                }
            }
        }
        
        // FASE 3: Última oportunidad - Buscar spawn de Minecraft por defecto
        plugin.getLogger().severe("[AUTO-CORRECCIÓN] FASE 2 falló. Intentando spawn por defecto de Minecraft...");
        Location defaultSpawn = world.getSpawnLocation();
        Location surfaceSpawn = world.getHighestBlockAt(defaultSpawn).getLocation().add(0, 1, 0);
        
        if (isLocationSafe(surfaceSpawn)) {
            plugin.getLogger().info("[AUTO-CORRECCIÓN] Usando spawn por defecto de Minecraft (mejorado)");
            return surfaceSpawn;
        }
        
        plugin.getLogger().severe("[AUTO-CORRECCIÓN] TODAS LAS FASES FALLARON. No se encontró spawn seguro.");
        return null;
    }
    
    /**
     * Valida que solo haya un ciclo activo en el sistema
     * SEGURIDAD: Previene inconsistencias con múltiples ciclos activos
     * 
     * @return true si la validación pasa, false si hay problemas
     */
    public boolean validateSingleActiveCycle() {
        List<String> activeCycles = new ArrayList<>();
        
        for (CicloData ciclo : persistenceManager.getAllCiclos()) {
            if (ciclo.isActivo()) {
                activeCycles.add(ciclo.getWorldName());
            }
        }
        
        if (activeCycles.isEmpty()) {
            plugin.getLogger().info("[SEGURIDAD] ✓ Validación: No hay ciclos activos");
            return true;
        }
        
        if (activeCycles.size() == 1) {
            plugin.getLogger().info("[SEGURIDAD] ✓ Validación: Un ciclo activo (" + activeCycles.get(0) + ")");
            return true;
        }
        
        // Múltiples ciclos activos = ERROR
        plugin.getLogger().severe("[SEGURIDAD] ✗ FALLO DE VALIDACIÓN: " + activeCycles.size() + 
            " ciclos activos simultáneos detectados:");
        for (String cycle : activeCycles) {
            plugin.getLogger().severe("  - " + cycle);
        }
        plugin.getLogger().severe("  └─ ACCIÓN REQUERIDA: Usar /avo ciclo info y desactivar ciclos duplicados");
        
        return false;
    }
    
    /**
     * Limpia cooldowns de jugadores desconectados
     * SEGURIDAD: Previene memory leaks en el HashMap de cooldowns
     */
    public void cleanupCooldowns() {
        int removed = 0;
        long cutoffTime = System.currentTimeMillis() - (TELEPORT_COOLDOWN_MS * 10); // 10x el cooldown
        
        Iterator<Map.Entry<UUID, Long>> iterator = teleportCooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            
            // Remover si es muy antiguo (jugador desconectado hace tiempo)
            if (entry.getValue() < cutoffTime) {
                iterator.remove();
                removed++;
            }
        }
        
        if (removed > 0 && debugMode) {
            plugin.getLogger().info("[SEGURIDAD] Limpieza de cooldowns: " + removed + " entradas removidas");
        }
    }
    
    /**
     * Verifica y auto-corrige el spawn de un mundo si es inseguro
     * AUTO-CORRECCIÓN: Sistema proactivo que mejora spawns automáticamente
     * 
     * @param worldName Nombre del mundo a verificar
     * @return true si el spawn es seguro o se corrigió exitosamente
     */
    public boolean autoCorrectWorldSpawn(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("[AUTO-CORRECCIÓN] Mundo '" + worldName + "' no existe");
            return false;
        }
        
        Location currentSpawn = world.getSpawnLocation();
        
        // Verificar si el spawn actual es seguro
        if (isLocationSafe(currentSpawn)) {
            if (debugMode) {
                plugin.getLogger().info("[AUTO-CORRECCIÓN] Spawn de '" + worldName + "' es seguro. No requiere corrección.");
            }
            return true;
        }
        
        // Spawn no es seguro - Buscar alternativa
        plugin.getLogger().warning("[AUTO-CORRECCIÓN] ⚠ Spawn inseguro detectado en '" + worldName + "'");
        plugin.getLogger().warning("  └─ Ubicación actual: " + currentSpawn.getBlockX() + ", " + 
            currentSpawn.getBlockY() + ", " + currentSpawn.getBlockZ());
        
        // Diagnosticar el problema
        String problema = diagnosticarSpawnInseguro(currentSpawn);
        plugin.getLogger().warning("  └─ Problema detectado: " + problema);
        
        plugin.getLogger().info("[AUTO-CORRECCIÓN] Iniciando búsqueda de spawn seguro...");
        
        // Buscar ubicación segura
        Location safeSpawn = findSafeLocation(world, currentSpawn);
        
        if (safeSpawn == null) {
            plugin.getLogger().severe("[AUTO-CORRECCIÓN] ✗ No se pudo encontrar spawn seguro para '" + worldName + "'");
            plugin.getLogger().severe("  └─ ACCIÓN REQUERIDA: Setear spawn manualmente con /avo ciclo setspawn");
            return false;
        }
        
        // Auto-corregir el spawn
        plugin.getLogger().info("[AUTO-CORRECCIÓN] ✓ Spawn seguro encontrado!");
        plugin.getLogger().info("  └─ Nueva ubicación: " + safeSpawn.getBlockX() + ", " + 
            safeSpawn.getBlockY() + ", " + safeSpawn.getBlockZ());
        
        boolean success = setSpawn(worldName, safeSpawn);
        
        if (success) {
            plugin.getLogger().info("[AUTO-CORRECCIÓN] ✓ Spawn auto-corregido exitosamente en '" + worldName + "'");
            return true;
        } else {
            plugin.getLogger().severe("[AUTO-CORRECCIÓN] ✗ Error al aplicar spawn corregido");
            return false;
        }
    }
    
    /**
     * Diagnostica por qué un spawn es inseguro
     * @param location Ubicación a diagnosticar
     * @return Descripción del problema
     */
    private String diagnosticarSpawnInseguro(Location location) {
        if (location == null || location.getWorld() == null) {
            return "Ubicación nula o mundo nulo";
        }
        
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        // Verificar void
        if (y < world.getMinHeight() + 5) {
            return "Demasiado cerca del void (Y=" + y + ")";
        }
        
        // Verificar altura
        if (y < world.getMinHeight() || y > world.getMaxHeight() - 2) {
            return "Fuera de límites del mundo (Y=" + y + ")";
        }
        
        Material ground = world.getBlockAt(x, y - 1, z).getType();
        Material feet = world.getBlockAt(x, y, z).getType();
        Material head = world.getBlockAt(x, y + 1, z).getType();
        
        // Verificar lava/fuego
        if (ground == Material.LAVA || feet == Material.LAVA || head == Material.LAVA) {
            return "Lava detectada";
        }
        if (ground == Material.FIRE || feet == Material.FIRE || ground == Material.SOUL_FIRE) {
            return "Fuego detectado";
        }
        if (ground == Material.MAGMA_BLOCK) {
            return "Bloque de magma detectado";
        }
        
        // Verificar suffocation
        if (feet.isSolid() || head.isSolid()) {
            return "Bloques sólidos en pies/cabeza (cueva/estructura)";
        }
        
        // Verificar suelo
        if (!ground.isSolid() && ground != Material.WATER) {
            return "Sin suelo sólido (caída libre)";
        }
        
        return "Razón desconocida";
    }
    
    /**
     * Verifica y auto-corrige spawns de todos los ciclos activos
     * Ejecutado automáticamente al iniciar y periódicamente
     */
    public void autoCorrectAllCycleSpawns() {
        plugin.getLogger().info("[AUTO-CORRECCIÓN] Verificando spawns de todos los ciclos...");
        
        int checked = 0;
        int corrected = 0;
        int failed = 0;
        
        for (String worldName : cycleWorlds) {
            checked++;
            
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("[AUTO-CORRECCIÓN] Ciclo '" + worldName + "' no cargado. Saltando...");
                continue;
            }
            
            Location spawn = world.getSpawnLocation();
            
            if (!isLocationSafe(spawn)) {
                plugin.getLogger().warning("[AUTO-CORRECCIÓN] Spawn inseguro detectado en '" + worldName + "'. Corrigiendo...");
                
                if (autoCorrectWorldSpawn(worldName)) {
                    corrected++;
                } else {
                    failed++;
                }
            }
        }
        
        plugin.getLogger().info("[AUTO-CORRECCIÓN] Verificación completada:");
        plugin.getLogger().info("  ├─ Ciclos verificados: " + checked);
        plugin.getLogger().info("  ├─ Spawns corregidos: " + corrected);
        plugin.getLogger().info("  └─ Fallos: " + failed);
        
        if (failed > 0) {
            plugin.getLogger().warning("[AUTO-CORRECCIÓN] ⚠ Algunos spawns no pudieron corregirse automáticamente.");
            plugin.getLogger().warning("  └─ Usar /avo ciclo setspawn manualmente en ubicación segura.");
        }
    }
}
