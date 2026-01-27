package me.riolu.apocalipsis.ciclos;

import me.apocalipsis.ciclos.WorldDataManager.PlayerProgressData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema de caché en memoria para datos de jugadores en ciclos.
 * Reduce las lecturas de disco mejorando el rendimiento en cambios de mundo frecuentes.
 * 
 * Características:
 * - TTL (Time-To-Live) configurable por entrada
 * - Limpieza automática de entradas expiradas
 * - Thread-safe con ConcurrentHashMap
 * - Invalidación manual y automática
 * 
 * @author Riolu
 * @version 1.22.55
 */
public class CicloDataCache {
    
    private final Map<String, CacheEntry<PlayerProgressData>> cache;
    private final long defaultTTL; // Time-To-Live en milisegundos
    private final int maxCacheSize;
    
    /**
     * Constructor con configuración por defecto
     * TTL: 5 minutos
     * Max Size: 100 entradas
     */
    public CicloDataCache() {
        this(300000L, 100); // 5 minutos, 100 jugadores
    }
    
    /**
     * Constructor con configuración personalizada
     * 
     * @param defaultTTL Tiempo de vida en milisegundos
     * @param maxCacheSize Tamaño máximo del cache
     */
    public CicloDataCache(long defaultTTL, int maxCacheSize) {
        this.cache = new ConcurrentHashMap<>();
        this.defaultTTL = defaultTTL;
        this.maxCacheSize = maxCacheSize;
    }
    
    /**
     * Genera clave de caché única para jugador-mundo
     * 
     * @param uuid UUID del jugador
     * @param worldName Nombre del mundo
     * @return Clave única
     */
    private String getCacheKey(UUID uuid, String worldName) {
        return uuid.toString() + ":" + worldName;
    }
    
    /**
     * Almacena datos en caché con TTL por defecto
     * 
     * @param uuid UUID del jugador
     * @param worldName Nombre del mundo
     * @param data Datos a cachear
     */
    public void put(UUID uuid, String worldName, PlayerProgressData data) {
        put(uuid, worldName, data, defaultTTL);
    }
    
    /**
     * Almacena datos en caché con TTL personalizado
     * 
     * @param uuid UUID del jugador
     * @param worldName Nombre del mundo
     * @param data Datos a cachear
     * @param ttl Tiempo de vida en milisegundos
     */
    public void put(UUID uuid, String worldName, PlayerProgressData data, long ttl) {
        // Si el cache está lleno, limpiar entradas expiradas
        if (cache.size() >= maxCacheSize) {
            cleanExpired();
        }
        
        // Si aún está lleno después de limpiar, remover la entrada más antigua
        if (cache.size() >= maxCacheSize) {
            removeOldest();
        }
        
        String key = getCacheKey(uuid, worldName);
        cache.put(key, new CacheEntry<>(data, System.currentTimeMillis() + ttl));
    }
    
    /**
     * Obtiene datos del caché si existen y no han expirado
     * 
     * @param uuid UUID del jugador
     * @param worldName Nombre del mundo
     * @return Datos cacheados o null si no existen/expiraron
     */
    public PlayerProgressData get(UUID uuid, String worldName) {
        String key = getCacheKey(uuid, worldName);
        CacheEntry<PlayerProgressData> entry = cache.get(key);
        
        if (entry == null) {
            return null;
        }
        
        // Verificar si expiró
        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        
        return entry.getData();
    }
    
    /**
     * Verifica si hay datos en caché válidos
     * 
     * @param uuid UUID del jugador
     * @param worldName Nombre del mundo
     * @return true si hay datos válidos en caché
     */
    public boolean has(UUID uuid, String worldName) {
        return get(uuid, worldName) != null;
    }
    
    /**
     * Invalida (elimina) entrada específica del caché
     * 
     * @param uuid UUID del jugador
     * @param worldName Nombre del mundo
     */
    public void invalidate(UUID uuid, String worldName) {
        String key = getCacheKey(uuid, worldName);
        cache.remove(key);
    }
    
    /**
     * Invalida todas las entradas de un jugador (todos sus mundos)
     * 
     * @param uuid UUID del jugador
     */
    public void invalidatePlayer(UUID uuid) {
        String prefix = uuid.toString() + ":";
        cache.keySet().removeIf(key -> key.startsWith(prefix));
    }
    
    /**
     * Invalida todas las entradas de un mundo (todos los jugadores)
     * 
     * @param worldName Nombre del mundo
     */
    public void invalidateWorld(String worldName) {
        String suffix = ":" + worldName;
        cache.keySet().removeIf(key -> key.endsWith(suffix));
    }
    
    /**
     * Limpia todas las entradas expiradas del caché
     * 
     * @return Número de entradas eliminadas
     */
    public int cleanExpired() {
        int removed = 0;
        Iterator<Map.Entry<String, CacheEntry<PlayerProgressData>>> iterator = cache.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, CacheEntry<PlayerProgressData>> entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                removed++;
            }
        }
        
        return removed;
    }
    
    /**
     * Elimina la entrada más antigua del caché
     */
    private void removeOldest() {
        if (cache.isEmpty()) {
            return;
        }
        
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (Map.Entry<String, CacheEntry<PlayerProgressData>> entry : cache.entrySet()) {
            long entryTime = entry.getValue().getExpirationTime();
            if (entryTime < oldestTime) {
                oldestTime = entryTime;
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            cache.remove(oldestKey);
        }
    }
    
    /**
     * Limpia todo el caché
     */
    public void clear() {
        cache.clear();
    }
    
    /**
     * Obtiene el tamaño actual del caché
     * 
     * @return Número de entradas en caché
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * Obtiene estadísticas del caché
     * 
     * @return Mapa con estadísticas
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("size", cache.size());
        stats.put("maxSize", maxCacheSize);
        stats.put("defaultTTL", defaultTTL);
        
        int expired = 0;
        for (CacheEntry<PlayerProgressData> entry : cache.values()) {
            if (entry.isExpired()) {
                expired++;
            }
        }
        stats.put("expired", expired);
        stats.put("valid", cache.size() - expired);
        
        return stats;
    }
    
    /**
     * Entrada de caché con tiempo de expiración
     */
    private static class CacheEntry<T> {
        private final T data;
        private final long expirationTime;
        
        public CacheEntry(T data, long expirationTime) {
            this.data = data;
            this.expirationTime = expirationTime;
        }
        
        public T getData() {
            return data;
        }
        
        public long getExpirationTime() {
            return expirationTime;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
}
