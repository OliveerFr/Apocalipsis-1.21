package me.apocalipsis.missions;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import me.apocalipsis.Apocalipsis;

/**
 * Base de datos para tokens de stream usando SQLite
 * Más eficiente y confiable que guardar en YAML
 */
public class TokenDatabase {
    
    private final Apocalipsis plugin;
    private final File dbFile;
    private Connection connection;
    
    // Cache en memoria para acceso rápido
    private final Map<UUID, Integer> tokenCache = new HashMap<>();
    
    public TokenDatabase(Apocalipsis plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "tokens.db");
        
        try {
            initDatabase();
            loadCache();
            plugin.getLogger().info("[TokenDatabase] ✓ Base de datos de tokens iniciada");
        } catch (SQLException e) {
            plugin.getLogger().severe("[TokenDatabase] ✗ Error al inicializar base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Inicializa la base de datos y crea las tablas necesarias
     */
    private void initDatabase() throws SQLException {
        // Crear directorio si no existe
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // Conectar a la base de datos
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        connection = DriverManager.getConnection(url);
        
        // Crear tabla de tokens
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS player_tokens (" +
                "uuid TEXT PRIMARY KEY NOT NULL, " +
                "tokens INTEGER NOT NULL DEFAULT 0, " +
                "total_earned INTEGER NOT NULL DEFAULT 0, " +
                "total_spent INTEGER NOT NULL DEFAULT 0, " +
                "last_updated INTEGER NOT NULL" +
                ")"
            );
            
            // Crear tabla de historial de transacciones
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS token_transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "uuid TEXT NOT NULL, " +
                "amount INTEGER NOT NULL, " +
                "type TEXT NOT NULL, " +
                "reason TEXT, " +
                "timestamp INTEGER NOT NULL" +
                ")"
            );
            
            // Crear índices para mejorar rendimiento
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_uuid ON player_tokens(uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_uuid ON token_transactions(uuid)");
        }
    }
    
    /**
     * Carga el cache desde la base de datos
     */
    private void loadCache() throws SQLException {
        String query = "SELECT uuid, tokens FROM player_tokens";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            tokenCache.clear();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                int tokens = rs.getInt("tokens");
                tokenCache.put(uuid, tokens);
            }
            
            plugin.getLogger().info("[TokenDatabase] Cache cargado: " + tokenCache.size() + " jugadores");
        }
    }
    
    /**
     * Obtiene los tokens de un jugador (desde cache)
     */
    public int getTokens(UUID uuid) {
        return tokenCache.getOrDefault(uuid, 0);
    }
    
    /**
     * Añade tokens a un jugador de forma asíncrona
     */
    public CompletableFuture<Boolean> addTokens(UUID uuid, int amount, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int currentTokens = getTokens(uuid);
                int newTotal = currentTokens + amount;
                
                // Actualizar en la base de datos
                String upsert = 
                    "INSERT INTO player_tokens (uuid, tokens, total_earned, last_updated) " +
                    "VALUES (?, ?, ?, ?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET " +
                    "tokens = tokens + ?, " +
                    "total_earned = total_earned + ?, " +
                    "last_updated = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(upsert)) {
                    long now = System.currentTimeMillis();
                    stmt.setString(1, uuid.toString());
                    stmt.setInt(2, amount);
                    stmt.setInt(3, amount);
                    stmt.setLong(4, now);
                    stmt.setInt(5, amount);
                    stmt.setInt(6, amount);
                    stmt.setLong(7, now);
                    stmt.executeUpdate();
                }
                
                // Registrar transacción
                logTransaction(uuid, amount, "ADD", reason);
                
                // Actualizar cache
                tokenCache.put(uuid, newTotal);
                
                return true;
            } catch (SQLException e) {
                plugin.getLogger().severe("[TokenDatabase] Error añadiendo tokens: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Quita tokens a un jugador de forma asíncrona
     */
    public CompletableFuture<Boolean> removeTokens(UUID uuid, int amount, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int currentTokens = getTokens(uuid);
                
                if (currentTokens < amount) {
                    return false; // No tiene suficientes tokens
                }
                
                int newTotal = currentTokens - amount;
                
                // Actualizar en la base de datos
                String update = 
                    "UPDATE player_tokens SET " +
                    "tokens = tokens - ?, " +
                    "total_spent = total_spent + ?, " +
                    "last_updated = ? " +
                    "WHERE uuid = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(update)) {
                    stmt.setInt(1, amount);
                    stmt.setInt(2, amount);
                    stmt.setLong(3, System.currentTimeMillis());
                    stmt.setString(4, uuid.toString());
                    int affected = stmt.executeUpdate();
                    
                    if (affected == 0) {
                        return false;
                    }
                }
                
                // Registrar transacción
                logTransaction(uuid, -amount, "REMOVE", reason);
                
                // Actualizar cache
                tokenCache.put(uuid, newTotal);
                
                return true;
            } catch (SQLException e) {
                plugin.getLogger().severe("[TokenDatabase] Error quitando tokens: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Registra una transacción en el historial
     */
    private void logTransaction(UUID uuid, int amount, String type, String reason) {
        try {
            String insert = 
                "INSERT INTO token_transactions (uuid, amount, type, reason, timestamp) " +
                "VALUES (?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(insert)) {
                stmt.setString(1, uuid.toString());
                stmt.setInt(2, amount);
                stmt.setString(3, type);
                stmt.setString(4, reason);
                stmt.setLong(5, System.currentTimeMillis());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("[TokenDatabase] Error registrando transacción: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene las estadísticas de un jugador
     */
    public TokenStats getStats(UUID uuid) {
        try {
            String query = "SELECT * FROM player_tokens WHERE uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new TokenStats(
                            rs.getInt("tokens"),
                            rs.getInt("total_earned"),
                            rs.getInt("total_spent")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[TokenDatabase] Error obteniendo stats: " + e.getMessage());
        }
        
        return new TokenStats(0, 0, 0);
    }
    
    /**
     * Cierra la conexión de la base de datos
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("[TokenDatabase] Base de datos cerrada correctamente");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[TokenDatabase] Error cerrando base de datos: " + e.getMessage());
        }
    }
    
    /**
     * Clase para estadísticas de tokens
     */
    public static class TokenStats {
        public final int current;
        public final int totalEarned;
        public final int totalSpent;
        
        public TokenStats(int current, int totalEarned, int totalSpent) {
            this.current = current;
            this.totalEarned = totalEarned;
            this.totalSpent = totalSpent;
        }
    }
}
