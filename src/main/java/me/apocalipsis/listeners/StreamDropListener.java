package me.apocalipsis.listeners;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.missions.StreamFeaturesManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener para manejar drops especiales de stream
 * cuando el streamer está online
 */
public class StreamDropListener implements Listener {
    
    private final Apocalipsis plugin;
    private final StreamFeaturesManager streamManager;
    
    // Sistema de cooldown para evitar farmeo excesivo
    private final Map<UUID, Long> dropCooldowns = new HashMap<>();
    private long cooldownMillis; // Configurable desde stream_features.yml
    
    // Sistema anti-burst: detecta matanzas masivas y solo permite 1 drop
    private final Map<UUID, KillBurst> killBursts = new HashMap<>();
    private static final long BURST_WINDOW_MS = 1000; // 1 segundo para considerar un "burst"
    private int maxKillsPerBurst; // Configurable
    
    /**
     * Clase para rastrear ráfagas de kills
     */
    private static class KillBurst {
        long lastKillTime;
        int killCount;
        boolean dropProcessed;
        
        KillBurst() {
            this.lastKillTime = System.currentTimeMillis();
            this.killCount = 1;
            this.dropProcessed = false;
        }
        
        void addKill() {
            this.lastKillTime = System.currentTimeMillis();
            this.killCount++;
        }
        
        boolean isExpired() {
            return (System.currentTimeMillis() - lastKillTime) > BURST_WINDOW_MS;
        }
    }
    
    public StreamDropListener(Apocalipsis plugin, StreamFeaturesManager streamManager) {
        this.plugin = plugin;
        this.streamManager = streamManager;
        
        // Cargar cooldown desde config (usando recompensas.yml como fallback)
        // La configuración real está en stream_features.yml que maneja StreamFeaturesManager
        this.cooldownMillis = 5000L; // 5 segundos por defecto
        this.maxKillsPerBurst = 5;   // 5 mobs/segundo por defecto
        
        plugin.getLogger().info("[StreamDrops] Cooldown: 5s | Anti-burst: max " + 
            maxKillsPerBurst + " mobs/segundo");
        
        // Limpiar cooldowns antiguos cada 5 minutos para evitar memory leak
        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupOldData, 
            20L * 60 * 5, // Inicio: 5 minutos
            20L * 60 * 5  // Cada 5 minutos
        );
    }
    
    /**
     * Limpia cooldowns y bursts antiguos
     */
    private void cleanupOldData() {
        long now = System.currentTimeMillis();
        long tenMinutes = 10 * 60 * 1000L;
        
        dropCooldowns.entrySet().removeIf(entry -> (now - entry.getValue()) > tenMinutes);
        killBursts.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        // Solo si el streamer está online
        if (!streamManager.isStreamerOnline()) return;
        
        LivingEntity entity = event.getEntity();
        
        // Solo mobs hostiles
        if (!isHostileMob(entity.getType())) return;
        
        // Solo si fue matado por un jugador
        Player killer = entity.getKiller();
        if (killer == null) return;
        
        // ═══ ANTI-FARM: NO DAR TOKENS SI LA ENTIDAD VIENE DE SPAWNER ═══
        if (entity.fromMobSpawner()) {
            // No dar recompensas para mobs de spawner
            return;
        }
        
        UUID uuid = killer.getUniqueId();
        long now = System.currentTimeMillis();
        
        // === SISTEMA ANTI-BURST ===
        // Si el jugador está matando muchos mobs rápidamente (granjas), solo procesar 1 drop
        KillBurst burst = killBursts.get(uuid);
        
        if (burst != null && !burst.isExpired()) {
            // Estamos en un burst activo
            burst.addKill();
            
            // Si ya procesamos el drop en este burst, o excedimos el límite, ignorar
            if (burst.dropProcessed || burst.killCount > maxKillsPerBurst) {
                // Avisar al jugador solo cada 10 kills para no hacer spam
                if (burst.killCount % 10 == 0) {
                    killer.sendActionBar("§e⚠ Anti-farmeo: " + burst.killCount + " mobs/" + 
                        (BURST_WINDOW_MS/1000) + "s (límite: " + maxKillsPerBurst + ")");
                }
                return;
            }
        } else {
            // Nuevo burst
            killBursts.put(uuid, new KillBurst());
            burst = killBursts.get(uuid);
        }
        
        // === SISTEMA DE COOLDOWN ===
        if (dropCooldowns.containsKey(uuid)) {
            long lastDrop = dropCooldowns.get(uuid);
            long timeSince = now - lastDrop;
            
            if (timeSince < cooldownMillis) {
                // Aún en cooldown
                long remainingSeconds = (cooldownMillis - timeSince) / 1000;
                if (remainingSeconds <= 1 && remainingSeconds > 0) {
                    killer.sendActionBar("§e⏳ Cooldown: " + remainingSeconds + "s");
                }
                return;
            }
        }
        
        // Procesar drop especial
        boolean gotDrop = streamManager.processStreamDrop(killer);
        
        // Si obtuvo drop, actualizar estado
        if (gotDrop) {
            dropCooldowns.put(uuid, now);
            burst.dropProcessed = true;
        }
    }
    
    /**
     * Verifica si el tipo de entidad es un mob hostil
     */
    private boolean isHostileMob(EntityType type) {
        switch (type) {
            case ZOMBIE:
            case SKELETON:
            case CREEPER:
            case SPIDER:
            case CAVE_SPIDER:
            case ENDERMAN:
            case BLAZE:
            case WITCH:
            case GUARDIAN:
            case ELDER_GUARDIAN:
            case SHULKER:
            case VINDICATOR:
            case EVOKER:
            case PILLAGER:
            case RAVAGER:
            case VEX:
            case PHANTOM:
            case DROWNED:
            case HUSK:
            case STRAY:
            case WITHER_SKELETON:
            case ZOMBIFIED_PIGLIN:
            case PIGLIN:
            case PIGLIN_BRUTE:
            case HOGLIN:
            case ZOGLIN:
            case SLIME:
            case MAGMA_CUBE:
            case SILVERFISH:
            case ENDERMITE:
                return true;
            default:
                return false;
        }
    }
}
