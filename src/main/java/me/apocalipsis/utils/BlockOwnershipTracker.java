package me.apocalipsis.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.apocalipsis.Apocalipsis;

/**
 * Sistema de rastreo de bloques para prevenir griefing entre jugadores.
 * 
 * SISTEMA INTELIGENTE DE LIMPIEZA:
 * - Los bloques están protegidos mientras el jugador esté ACTIVO
 * - Si un jugador no se conecta en X días, sus bloques pierden protección
 * - Cada vez que un jugador se conecta, se renueva la protección de sus bloques
 * - Persistencia con SQLite
 * - Backup automático diario
 * - Protección contra pistons, TNT y explosiones
 * - Protección de cofres dobles
 */
public class BlockOwnershipTracker {
    
    private final Apocalipsis plugin;
    
    // Cache en memoria para acceso rápido
    private final Map<String, UUID> blockOwners;
    private final Map<String, Long> blockTimestamps;
    
    // Cache de última conexión de jugadores
    private final Map<UUID, Long> playerLastSeen;
    
    // SQLite para persistencia
    private Connection dbConnection;
    private final File dbFile;
    private final File backupDir;
    
    // Configuración
    private int maxCacheSize;
    private int inactiveDaysToExpire; // Días de inactividad para que expiren bloques
    private boolean protectConnectedBlocks;
    private boolean protectBlockBelow;
    private int connectedBlockRadius;
    private boolean backupEnabled;
    private int backupIntervalHours;
    
    // Tasks
    private BukkitRunnable cleanupTask;
    private BukkitRunnable syncTask;
    private BukkitRunnable backupTask;
    
    // Timestamp del último backup
    private long lastBackupTime = 0;
    
    // Caras para verificar bloques conectados
    private static final BlockFace[] CONNECTED_FACES = {
        BlockFace.UP, BlockFace.DOWN,
        BlockFace.NORTH, BlockFace.SOUTH,
        BlockFace.EAST, BlockFace.WEST
    };
    
    public BlockOwnershipTracker(Apocalipsis plugin) {
        this.plugin = plugin;
        this.blockOwners = new ConcurrentHashMap<>();
        this.blockTimestamps = new ConcurrentHashMap<>();
        this.playerLastSeen = new ConcurrentHashMap<>();
        this.dbFile = new File(plugin.getDataFolder(), "block_ownership.db");
        this.backupDir = new File(plugin.getDataFolder(), "backups");
        
        // Cargar configuración
        loadConfig();
        
        // Inicializar SQLite
        initDatabase();
        
        // Cargar datos de DB a cache
        loadFromDatabase();
        
        // Iniciar tareas automáticas
        startCleanupTask();
        startSyncTask();
        startBackupTask();
    }
    
    /**
     * Carga la configuración desde protecciones.yml
     */
    private void loadConfig() {
        ConfigurationSection config = plugin.getConfigManager().getProteccionesConfig()
            .getConfigurationSection("block_ownership");
        
        if (config != null) {
            maxCacheSize = config.getInt("max_cache_size", 100000);
            inactiveDaysToExpire = config.getInt("inactive_days_to_expire", 14); // 2 semanas por defecto
            protectConnectedBlocks = config.getBoolean("protect_connected_blocks", true);
            protectBlockBelow = config.getBoolean("protect_block_below", true);
            connectedBlockRadius = config.getInt("connected_block_radius", 1);
            backupEnabled = config.getBoolean("backup_enabled", true);
            backupIntervalHours = config.getInt("backup_interval_hours", 24); // Diario por defecto
            
            plugin.getLogger().info("[BlockTracker] Config: inactividad=" + inactiveDaysToExpire + " días, " +
                "connected=" + protectConnectedBlocks + ", below=" + protectBlockBelow + 
                ", backup=" + (backupEnabled ? "cada " + backupIntervalHours + "h" : "desactivado"));
        } else {
            // Valores por defecto
            maxCacheSize = 100000;
            inactiveDaysToExpire = 14; // 2 semanas
            protectConnectedBlocks = true;
            protectBlockBelow = true;
            connectedBlockRadius = 1;
            backupEnabled = true;
            backupIntervalHours = 24;
        }
    }
    
    // ==================== SQLite ====================
    
    /**
     * Inicializa la base de datos SQLite
     */
    private void initDatabase() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            dbConnection = DriverManager.getConnection(url);
            
            try (Statement stmt = dbConnection.createStatement()) {
                // Tabla de bloques
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS block_owners (" +
                    "  block_key TEXT PRIMARY KEY," +
                    "  owner_uuid TEXT NOT NULL," +
                    "  timestamp INTEGER NOT NULL" +
                    ")"
                );
                
                // Tabla de última conexión de jugadores
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS player_last_seen (" +
                    "  player_uuid TEXT PRIMARY KEY," +
                    "  last_seen INTEGER NOT NULL" +
                    ")"
                );
                
                // Índices
                stmt.executeUpdate(
                    "CREATE INDEX IF NOT EXISTS idx_owner ON block_owners(owner_uuid)"
                );
            }
            
            plugin.getLogger().info("[BlockTracker] SQLite inicializado correctamente");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("[BlockTracker] Error inicializando SQLite: " + e.getMessage());
        }
    }
    
    /**
     * Carga datos desde la base de datos al cache
     */
    private void loadFromDatabase() {
        if (dbConnection == null) return;
        
        try {
            // Cargar última conexión de jugadores
            try (PreparedStatement stmt = dbConnection.prepareStatement(
                    "SELECT player_uuid, last_seen FROM player_last_seen")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    UUID playerId = UUID.fromString(rs.getString("player_uuid"));
                    long lastSeen = rs.getLong("last_seen");
                    playerLastSeen.put(playerId, lastSeen);
                }
            }
            
            // Identificar jugadores inactivos
            Set<UUID> inactivePlayers = getInactivePlayers();
            
            // Limpiar bloques de jugadores inactivos
            if (!inactivePlayers.isEmpty()) {
                int deleted = deleteBlocksOfInactivePlayers(inactivePlayers);
                if (deleted > 0) {
                    plugin.getLogger().info("[BlockTracker] Limpiados " + deleted + 
                        " bloques de " + inactivePlayers.size() + " jugadores inactivos (>" + inactiveDaysToExpire + " días)");
                }
            }
            
            // Cargar bloques válidos
            try (PreparedStatement stmt = dbConnection.prepareStatement(
                    "SELECT block_key, owner_uuid, timestamp FROM block_owners ORDER BY timestamp DESC LIMIT ?")) {
                stmt.setInt(1, maxCacheSize);
                ResultSet rs = stmt.executeQuery();
                
                int loaded = 0;
                while (rs.next()) {
                    String key = rs.getString("block_key");
                    UUID owner = UUID.fromString(rs.getString("owner_uuid"));
                    long timestamp = rs.getLong("timestamp");
                    
                    blockOwners.put(key, owner);
                    blockTimestamps.put(key, timestamp);
                    loaded++;
                }
                
                plugin.getLogger().info("[BlockTracker] Cargados " + loaded + " bloques desde SQLite");
            }
            
        } catch (SQLException e) {
            plugin.getLogger().warning("[BlockTracker] Error cargando desde DB: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene los jugadores que no se han conectado en X días
     */
    private Set<UUID> getInactivePlayers() {
        Set<UUID> inactive = new HashSet<>();
        long now = System.currentTimeMillis();
        long inactiveThreshold = inactiveDaysToExpire * 24L * 60L * 60L * 1000L;
        
        for (Map.Entry<UUID, Long> entry : playerLastSeen.entrySet()) {
            if (now - entry.getValue() > inactiveThreshold) {
                inactive.add(entry.getKey());
            }
        }
        
        return inactive;
    }
    
    /**
     * Elimina bloques de jugadores inactivos de la DB
     */
    private int deleteBlocksOfInactivePlayers(Set<UUID> inactivePlayers) {
        if (dbConnection == null || inactivePlayers.isEmpty()) return 0;
        
        int totalDeleted = 0;
        
        try {
            for (UUID playerId : inactivePlayers) {
                try (PreparedStatement stmt = dbConnection.prepareStatement(
                        "DELETE FROM block_owners WHERE owner_uuid = ?")) {
                    stmt.setString(1, playerId.toString());
                    totalDeleted += stmt.executeUpdate();
                }
                
                // También remover del cache
                blockOwners.entrySet().removeIf(entry -> entry.getValue().equals(playerId));
                
                // Remover de playerLastSeen
                playerLastSeen.remove(playerId);
                
                try (PreparedStatement stmt = dbConnection.prepareStatement(
                        "DELETE FROM player_last_seen WHERE player_uuid = ?")) {
                    stmt.setString(1, playerId.toString());
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[BlockTracker] Error eliminando bloques inactivos: " + e.getMessage());
        }
        
        return totalDeleted;
    }
    
    /**
     * Sincroniza el cache con la base de datos
     */
    private void syncToDatabase() {
        if (dbConnection == null) return;
        
        try {
            dbConnection.setAutoCommit(false);
            
            // Sincronizar bloques
            try (PreparedStatement stmt = dbConnection.prepareStatement(
                    "INSERT OR REPLACE INTO block_owners (block_key, owner_uuid, timestamp) VALUES (?, ?, ?)")) {
                
                int count = 0;
                for (Map.Entry<String, UUID> entry : blockOwners.entrySet()) {
                    String key = entry.getKey();
                    Long timestamp = blockTimestamps.get(key);
                    if (timestamp == null) continue;
                    
                    stmt.setString(1, key);
                    stmt.setString(2, entry.getValue().toString());
                    stmt.setLong(3, timestamp);
                    stmt.addBatch();
                    
                    count++;
                    if (count % 1000 == 0) {
                        stmt.executeBatch();
                    }
                }
                stmt.executeBatch();
            }
            
            // Sincronizar última conexión de jugadores
            try (PreparedStatement stmt = dbConnection.prepareStatement(
                    "INSERT OR REPLACE INTO player_last_seen (player_uuid, last_seen) VALUES (?, ?)")) {
                
                for (Map.Entry<UUID, Long> entry : playerLastSeen.entrySet()) {
                    stmt.setString(1, entry.getKey().toString());
                    stmt.setLong(2, entry.getValue());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            
            dbConnection.commit();
            dbConnection.setAutoCommit(true);
            
        } catch (SQLException e) {
            plugin.getLogger().warning("[BlockTracker] Error sincronizando DB: " + e.getMessage());
            try {
                dbConnection.rollback();
                dbConnection.setAutoCommit(true);
            } catch (SQLException ex) {
                // Ignorar
            }
        }
    }
    
    // ==================== Tracking ====================
    
    /**
     * Registra que un jugador colocó un bloque
     */
    public void trackBlockPlacement(Block block, Player player) {
        if (player.hasPermission("avo.bypass.blocktrack")) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        // Actualizar última vez visto
        playerLastSeen.put(playerId, now);
        
        // Limitar tamaño del cache
        if (blockOwners.size() >= maxCacheSize) {
            cleanupInactivePlayerBlocks();
        }
        
        // Registrar el bloque colocado
        registerBlock(block, playerId, now);
        
        // Proteger bloque debajo automáticamente
        if (protectBlockBelow) {
            Block below = block.getRelative(BlockFace.DOWN);
            if (below.getType().isSolid()) {
                UUID belowOwner = getBlockOwner(below);
                if (belowOwner == null || belowOwner.equals(playerId)) {
                    registerBlock(below, playerId, now);
                }
            }
        }
    }
    
    /**
     * Registra un bloque individual
     */
    private void registerBlock(Block block, UUID owner, long timestamp) {
        String key = getBlockKey(block);
        blockOwners.put(key, owner);
        blockTimestamps.put(key, timestamp);
    }
    
    /**
     * Registra que un bloque fue roto
     */
    public void trackBlockBreak(Block block) {
        String key = getBlockKey(block);
        blockOwners.remove(key);
        blockTimestamps.remove(key);
        
        // Remover de DB asíncronamente
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (dbConnection == null) return;
            try (PreparedStatement stmt = dbConnection.prepareStatement(
                    "DELETE FROM block_owners WHERE block_key = ?")) {
                stmt.setString(1, key);
                stmt.executeUpdate();
            } catch (SQLException e) {
                // Ignorar
            }
        });
    }
    
    /**
     * Actualiza la última vez que se vio a un jugador (llamar en login)
     */
    public void updatePlayerLastSeen(Player player) {
        playerLastSeen.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    /**
     * Verifica si un bloque puede ser destruido por un desastre.
     */
    public boolean canDisasterDestroyBlock(Block block, Player affectedPlayer) {
        String key = getBlockKey(block);
        UUID owner = blockOwners.get(key);
        UUID playerId = affectedPlayer.getUniqueId();
        
        // Si no tiene dueño, verificar bloques conectados
        if (owner == null) {
            if (protectConnectedBlocks) {
                UUID connectedOwner = findConnectedOwner(block, playerId);
                if (connectedOwner != null && !connectedOwner.equals(playerId)) {
                    // Verificar si el dueño conectado está activo
                    if (isPlayerActive(connectedOwner)) {
                        return false;
                    }
                }
            }
            return true;
        }
        
        // Si el dueño es el mismo jugador -> permitir
        if (owner.equals(playerId)) {
            return true;
        }
        
        // Si el dueño está INACTIVO, permitir destruir
        if (!isPlayerActive(owner)) {
            return true;
        }
        
        // Dueño activo diferente -> NO permitir
        return false;
    }
    
    /**
     * Verifica si un jugador está activo (se ha conectado recientemente)
     */
    private boolean isPlayerActive(UUID playerId) {
        Long lastSeen = playerLastSeen.get(playerId);
        
        // Si no tenemos registro, verificar con Bukkit
        if (lastSeen == null) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
            if (offline.hasPlayedBefore()) {
                lastSeen = offline.getLastPlayed();
                playerLastSeen.put(playerId, lastSeen);
            } else {
                return false; // Nunca jugó
            }
        }
        
        long now = System.currentTimeMillis();
        long inactiveThreshold = inactiveDaysToExpire * 24L * 60L * 60L * 1000L;
        
        return (now - lastSeen) <= inactiveThreshold;
    }
    
    /**
     * Busca si hay un dueño en los bloques conectados
     */
    private UUID findConnectedOwner(Block block, UUID excludePlayer) {
        for (BlockFace face : CONNECTED_FACES) {
            Block adjacent = block.getRelative(face);
            UUID adjOwner = blockOwners.get(getBlockKey(adjacent));
            
            if (adjOwner != null && !adjOwner.equals(excludePlayer)) {
                return adjOwner;
            }
        }
        
        if (connectedBlockRadius > 1) {
            Location loc = block.getLocation();
            for (int x = -connectedBlockRadius; x <= connectedBlockRadius; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -connectedBlockRadius; z <= connectedBlockRadius; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        
                        Block nearby = loc.clone().add(x, y, z).getBlock();
                        UUID nearbyOwner = blockOwners.get(getBlockKey(nearby));
                        
                        if (nearbyOwner != null && !nearbyOwner.equals(excludePlayer)) {
                            return nearbyOwner;
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Obtiene el UUID del dueño de un bloque
     */
    public UUID getBlockOwner(Block block) {
        String key = getBlockKey(block);
        return blockOwners.get(key);
    }
    
    /**
     * Genera una clave única para un bloque
     */
    private String getBlockKey(Block block) {
        Location loc = block.getLocation();
        return String.format("%s:%d:%d:%d", 
            loc.getWorld().getName(),
            loc.getBlockX(),
            loc.getBlockY(),
            loc.getBlockZ()
        );
    }
    
    // ==================== Cleanup Tasks ====================
    
    /**
     * Inicia la tarea de cleanup (cada 30 minutos)
     */
    private void startCleanupTask() {
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupInactivePlayerBlocks();
            }
        };
        // Ejecutar cada 30 minutos (36000 ticks)
        cleanupTask.runTaskTimerAsynchronously(plugin, 36000L, 36000L);
    }
    
    /**
     * Inicia la tarea de sincronización con DB (cada 5 minutos)
     */
    private void startSyncTask() {
        syncTask = new BukkitRunnable() {
            @Override
            public void run() {
                syncToDatabase();
            }
        };
        syncTask.runTaskTimerAsynchronously(plugin, 6000L, 6000L);
    }
    
    /**
     * Inicia la tarea de backup automático
     */
    private void startBackupTask() {
        if (!backupEnabled) return;
        
        // Crear directorio de backups
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        
        // Calcular intervalo en ticks (1 hora = 72000 ticks)
        long intervalTicks = backupIntervalHours * 72000L;
        
        backupTask = new BukkitRunnable() {
            @Override
            public void run() {
                performBackup();
            }
        };
        // Primer backup en 1 hora, luego según intervalo
        backupTask.runTaskTimerAsynchronously(plugin, 72000L, intervalTicks);
    }
    
    /**
     * Realiza un backup de la base de datos
     */
    private void performBackup() {
        if (!dbFile.exists()) return;
        
        try {
            // Sincronizar antes del backup
            syncToDatabase();
            
            // Generar nombre con timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
            File backupFile = new File(backupDir, "block_ownership_" + timestamp + ".db");
            
            // Copiar archivo
            Files.copy(dbFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            lastBackupTime = System.currentTimeMillis();
            plugin.getLogger().info("[BlockTracker] Backup creado: " + backupFile.getName());
            
            // Limpiar backups antiguos (mantener últimos 7)
            cleanOldBackups();
            
        } catch (IOException e) {
            plugin.getLogger().warning("[BlockTracker] Error creando backup: " + e.getMessage());
        }
    }
    
    /**
     * Elimina backups antiguos (mantiene los últimos 7)
     */
    private void cleanOldBackups() {
        File[] backups = backupDir.listFiles((dir, name) -> name.startsWith("block_ownership_") && name.endsWith(".db"));
        if (backups == null || backups.length <= 7) return;
        
        // Ordenar por fecha de modificación (más reciente primero)
        java.util.Arrays.sort(backups, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        
        // Eliminar los más antiguos
        for (int i = 7; i < backups.length; i++) {
            if (backups[i].delete()) {
                plugin.getLogger().info("[BlockTracker] Backup antiguo eliminado: " + backups[i].getName());
            }
        }
    }
    
    /**
     * Limpia bloques de jugadores inactivos usando SQL directo (optimizado)
     */
    private void cleanupInactivePlayerBlocks() {
        Set<UUID> inactivePlayers = getInactivePlayers();
        
        if (inactivePlayers.isEmpty()) return;
        
        // Limpiar cache en memoria
        int removed = 0;
        for (UUID playerId : inactivePlayers) {
            int before = blockOwners.size();
            blockOwners.entrySet().removeIf(entry -> entry.getValue().equals(playerId));
            blockTimestamps.entrySet().removeIf(entry -> {
                UUID owner = blockOwners.get(entry.getKey());
                return owner != null && owner.equals(playerId);
            });
            removed += before - blockOwners.size();
        }
        
        // Limpiar directamente en SQL (más eficiente que iterar)
        if (dbConnection != null && !inactivePlayers.isEmpty()) {
            try {
                StringBuilder sql = new StringBuilder("DELETE FROM block_owners WHERE owner_uuid IN (");
                for (int i = 0; i < inactivePlayers.size(); i++) {
                    sql.append(i > 0 ? ",?" : "?");
                }
                sql.append(")");
                
                try (PreparedStatement stmt = dbConnection.prepareStatement(sql.toString())) {
                    int idx = 1;
                    for (UUID playerId : inactivePlayers) {
                        stmt.setString(idx++, playerId.toString());
                    }
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("[BlockTracker] Error limpiando DB: " + e.getMessage());
            }
        }
        
        if (removed > 0) {
            plugin.getLogger().info("[BlockTracker] Limpiados " + removed + 
                " bloques de " + inactivePlayers.size() + " jugadores inactivos (>" + inactiveDaysToExpire + " días sin conectar)");
        }
    }
    
    // ==================== Métodos Públicos para Listeners ====================
    
    /**
     * Registra un bloque directamente con UUID (para cofres dobles)
     */
    public void trackBlockPlacementDirect(Block block, UUID owner) {
        registerBlock(block, owner, System.currentTimeMillis());
    }
    
    /**
     * Verifica si un jugador está activo (versión pública)
     */
    public boolean isPlayerActivePublic(UUID playerId) {
        return isPlayerActive(playerId);
    }
    
    /**
     * Busca dueño en bloques conectados (versión pública)
     */
    public UUID findConnectedOwnerPublic(Block block, UUID excludePlayer) {
        return findConnectedOwner(block, excludePlayer);
    }
    
    // ==================== Información de Bloques (Debug/Admin) ====================
    
    /**
     * Obtiene información detallada de un bloque específico
     */
    public BlockInfo getBlockInfo(Block block) {
        String key = getBlockKey(block);
        UUID owner = blockOwners.get(key);
        
        if (owner == null) {
            return null;
        }
        
        Long timestamp = blockTimestamps.get(key);
        Long lastSeen = playerLastSeen.get(owner);
        boolean isActive = isPlayerActive(owner);
        
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(owner);
        String playerName = offlinePlayer.getName();
        
        return new BlockInfo(
            owner,
            playerName != null ? playerName : "Desconocido",
            timestamp != null ? timestamp : 0L,
            lastSeen != null ? lastSeen : 0L,
            isActive
        );
    }
    
    /**
     * Obtiene estadísticas de un jugador específico
     */
    public PlayerBlockStats getPlayerStats(UUID playerId) {
        int blockCount = 0;
        for (UUID owner : blockOwners.values()) {
            if (owner.equals(playerId)) {
                blockCount++;
            }
        }
        
        Long lastSeen = playerLastSeen.get(playerId);
        boolean isActive = isPlayerActive(playerId);
        
        // Calcular días desde última conexión
        long daysSinceLastSeen = 0;
        if (lastSeen != null) {
            daysSinceLastSeen = (System.currentTimeMillis() - lastSeen) / (24L * 60L * 60L * 1000L);
        }
        
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
        String playerName = offlinePlayer.getName();
        
        return new PlayerBlockStats(
            playerId,
            playerName != null ? playerName : "Desconocido",
            blockCount,
            lastSeen != null ? lastSeen : 0L,
            daysSinceLastSeen,
            isActive
        );
    }
    
    /**
     * Obtiene el top de jugadores por bloques protegidos
     */
    public Map<UUID, Integer> getTopBlockOwners(int limit) {
        Map<UUID, Integer> counts = new HashMap<>();
        
        for (UUID owner : blockOwners.values()) {
            counts.merge(owner, 1, Integer::sum);
        }
        
        // Ordenar y limitar
        return counts.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .collect(java.util.LinkedHashMap::new, 
                (m, e) -> m.put(e.getKey(), e.getValue()), 
                java.util.LinkedHashMap::putAll);
    }
    
    // ==================== Lifecycle ====================
    
    /**
     * Guarda datos y cierra conexión
     */
    public void shutdown() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }
        if (syncTask != null && !syncTask.isCancelled()) {
            syncTask.cancel();
        }
        if (backupTask != null && !backupTask.isCancelled()) {
            backupTask.cancel();
        }
        
        // Backup final antes de cerrar
        if (backupEnabled) {
            performBackup();
        }
        
        syncToDatabase();
        
        if (dbConnection != null) {
            try {
                dbConnection.close();
                plugin.getLogger().info("[BlockTracker] SQLite cerrado correctamente");
            } catch (SQLException e) {
                plugin.getLogger().warning("[BlockTracker] Error cerrando SQLite: " + e.getMessage());
            }
        }
    }
    
    public void saveData() {
        syncToDatabase();
    }
    
    public void stopCleanupTask() {
        shutdown();
    }
    
    public void clearAll() {
        blockOwners.clear();
        blockTimestamps.clear();
        playerLastSeen.clear();
        
        if (dbConnection != null) {
            try (Statement stmt = dbConnection.createStatement()) {
                stmt.executeUpdate("DELETE FROM block_owners");
                stmt.executeUpdate("DELETE FROM player_last_seen");
            } catch (SQLException e) {
                plugin.getLogger().warning("[BlockTracker] Error limpiando DB: " + e.getMessage());
            }
        }
    }
    
    public int getTrackedBlocksCount() {
        return blockOwners.size();
    }
    
    public int getInactiveDaysToExpire() {
        return inactiveDaysToExpire;
    }
    
    /**
     * Obtiene estadísticas del tracker
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("tracked_blocks", blockOwners.size());
        stats.put("tracked_players", playerLastSeen.size());
        stats.put("inactive_players", getInactivePlayers().size());
        stats.put("inactive_days_threshold", inactiveDaysToExpire);
        stats.put("last_backup", lastBackupTime > 0 ? lastBackupTime : "Nunca");
        stats.put("backup_enabled", backupEnabled);
        return stats;
    }
    
    /**
     * Fuerza un backup manual
     */
    public boolean forceBackup() {
        if (!backupEnabled) return false;
        performBackup();
        return true;
    }
    
    // ==================== Records para información ====================
    
    /**
     * Información de un bloque protegido
     */
    public record BlockInfo(
        UUID ownerId,
        String ownerName,
        long placedTimestamp,
        long ownerLastSeen,
        boolean ownerActive
    ) {
        public String getPlacedAgo() {
            long days = (System.currentTimeMillis() - placedTimestamp) / (24L * 60L * 60L * 1000L);
            if (days == 0) return "hoy";
            if (days == 1) return "ayer";
            return "hace " + days + " días";
        }
        
        public String getLastSeenAgo() {
            if (ownerLastSeen == 0) return "desconocido";
            long days = (System.currentTimeMillis() - ownerLastSeen) / (24L * 60L * 60L * 1000L);
            if (days == 0) return "hoy";
            if (days == 1) return "ayer";
            return "hace " + days + " días";
        }
    }
    
    /**
     * Estadísticas de bloques de un jugador
     */
    public record PlayerBlockStats(
        UUID playerId,
        String playerName,
        int blockCount,
        long lastSeen,
        long daysSinceLastSeen,
        boolean isActive
    ) {}
}
