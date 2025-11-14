package me.apocalipsis.events;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;

/**
 * Clase base para eventos narrativos únicos.
 * 
 * Diferencias con DisasterBase:
 * - No forma parte del ciclo automático
 * - Enfoque en narrativa e inmersión
 * - Progresión automática con cinematics
 * - No usa sistema de intensidad/cooldown de desastres
 */
public abstract class EventBase {
    
    protected final Apocalipsis plugin;
    protected final MessageBus messageBus;
    protected final SoundUtil soundUtil;
    protected final String eventId;
    
    private boolean active = false;
    private long startTimeMs = 0;
    
    public EventBase(Apocalipsis plugin, MessageBus messageBus, SoundUtil soundUtil, String eventId) {
        this.plugin = plugin;
        this.messageBus = messageBus;
        this.soundUtil = soundUtil;
        this.eventId = eventId;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // MÉTODOS ABSTRACTOS - Implementar en subclases
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Lógica de inicio del evento
     */
    public abstract void onStart();
    
    /**
     * Lógica de detención del evento
     */
    public abstract void onStop();
    
    /**
     * Tick del evento (llamado cada tick de servidor)
     */
    public abstract void onTick();
    
    /**
     * Nombre display del evento
     */
    public abstract String getDisplayName();
    
    /**
     * Descripción corta del evento
     */
    public abstract String getDescription();
    
    // ═══════════════════════════════════════════════════════════════════
    // GESTIÓN DE ESTADO
    // ═══════════════════════════════════════════════════════════════════
    
    public void start() {
        if (active) {
            plugin.getLogger().warning("[EventBase] " + eventId + " ya está activo");
            return;
        }
        
        active = true;
        startTimeMs = System.currentTimeMillis();
        
        plugin.getLogger().info("[EventBase] Iniciando evento: " + eventId);
        onStart();
    }
    
    public void stop() {
        if (!active) {
            return;
        }
        
        active = false;
        plugin.getLogger().info("[EventBase] Deteniendo evento: " + eventId);
        onStop();
    }
    
    public void tick() {
        if (!active) {
            return;
        }
        onTick();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════
    
    public boolean isActive() {
        return active;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public long getStartTimeMs() {
        return startTimeMs;
    }
    
    public long getElapsedTimeMs() {
        return active ? (System.currentTimeMillis() - startTimeMs) : 0;
    }
    
    public int getElapsedSeconds() {
        return (int) (getElapsedTimeMs() / 1000);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // UTILIDADES PARA EVENTOS
    // ═══════════════════════════════════════════════════════════════════
    
    protected void broadcastNarrative(String message) {
        messageBus.broadcast(message, eventId + "_narrative");
    }
    
    protected void sendNarrativeToPlayer(Player player, String message) {
        player.sendMessage(message);
    }
    
    protected void playSoundToAll(Sound sound, float volume, float pitch) {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            soundUtil.playSound(p, sound, volume, pitch);
        }
    }
}
