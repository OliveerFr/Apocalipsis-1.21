package me.apocalipsis.disaster;

import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.disaster.adapters.PerformanceAdapter;
import me.apocalipsis.state.TimeService;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;

public abstract class DisasterBase implements Disaster {

    // [FIX DUPLICACIÓN] Contador global para IDs únicos
    private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger(0);
    private final int instanceId;
    
    protected final Apocalipsis plugin;
    protected final MessageBus messageBus;
    protected final SoundUtil soundUtil;
    protected final TimeService timeService;
    protected final PerformanceAdapter performanceAdapter;
    protected final String id;
    
    protected boolean active = false;
    protected int tickCounter = 0;

    public DisasterBase(Apocalipsis plugin, MessageBus messageBus, SoundUtil soundUtil,
                       TimeService timeService, PerformanceAdapter performanceAdapter, String id) {
        this.plugin = plugin;
        this.messageBus = messageBus;
        this.soundUtil = soundUtil;
        this.timeService = timeService;
        this.performanceAdapter = performanceAdapter;
        this.id = id;
        this.instanceId = INSTANCE_COUNTER.incrementAndGet();
        
        plugin.getLogger().info("[DisasterBase] Creada instancia #" + instanceId + " de " + id);
    }
    
    /**
     * Obtiene el multiplicador de escala basado en el rendimiento actual
     */
    protected double getPerformanceScale() {
        return performanceAdapter.getScale();
    }

    @Override
    public String getId() {
        return id;
    }
    
    /**
     * [FIX DUPLICACIÓN] Obtiene el ID único de instancia para debugging
     */
    public int getInstanceId() {
        return instanceId;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void start() {
        // [FIX DUPLICACIÓN CRÍTICO] Prevenir inicio doble
        if (active) {
            plugin.getLogger().warning("[CRÍTICO] Intento de iniciar desastre " + id + " #" + instanceId + " que ya está activo - IGNORADO");
            plugin.getLogger().warning("[CRÍTICO] Stacktrace:");
            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                plugin.getLogger().warning("  " + element.toString());
            }
            return;
        }
        
        this.active = true;
        this.tickCounter = 0;
        plugin.getLogger().info("[Disaster] START: " + id + " #" + instanceId);
        onStart();
    }

    @Override
    public void stop() {
        if (!active) {
            plugin.getLogger().warning("[Disaster] Intento de detener " + id + " #" + instanceId + " que ya está inactivo - IGNORADO");
            return;
        }
        
        plugin.getLogger().info("[Disaster] STOP: " + id + " #" + instanceId);
        this.active = false;
        onStop();
    }

    @Override
    public void tick() {
        if (!active) {
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[Disaster] TICK SALTADO: " + id + " #" + instanceId + " no está activo");
            }
            return;
        }
        
        // Early return si el estado NO es ACTIVO (leer desde state.yml)
        String estado = plugin.getStateManager().getEstado();
        if (!"ACTIVO".equals(estado)) {
            plugin.getLogger().info("[Disaster] STOP automático: " + id + " #" + instanceId + " estado=" + estado);
            stop();
            return;
        }
        
        tickCounter++;
        
        if (plugin.getConfigManager().isDebugCiclo() && tickCounter % 100 == 0) {
            plugin.getLogger().info("[Disaster] TICK #" + tickCounter + ": " + id + " #" + instanceId);
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyEffects(player);
        }
        
        onTick();
    }

    /**
     * Métodos abstractos para implementar en cada desastre
     */
    protected abstract void onStart();
    protected abstract void onStop();
    protected abstract void onTick();

    protected boolean shouldSkipTick(int interval) {
        return tickCounter % interval != 0;
    }
    
    /**
     * Verifica si un jugador es inmune por excepción administrativa
     */
    protected boolean isPlayerExempt(org.bukkit.entity.Player player) {
        if (!plugin.getConfigManager().isExcepcionesEnabled()) {
            return false;
        }
        return plugin.getConfigManager().getExcepciones().contains(player.getUniqueId());
    }
    
    /**
     * [FIX PARTÍCULAS] Muestra partículas solo a jugadores NO exentos
     * Si un jugador está en la lista de excepciones, no verá las partículas
     */
    protected void spawnParticleForNonExempt(org.bukkit.World world, org.bukkit.Particle particle, 
                                            org.bukkit.Location loc, int count, 
                                            double offsetX, double offsetY, double offsetZ, double speed) {
        for (Player player : world.getPlayers()) {
            if (!isPlayerExempt(player)) {
                player.spawnParticle(particle, loc, count, offsetX, offsetY, offsetZ, speed);
            }
        }
    }
    
    /**
     * [FIX PARTÍCULAS] Muestra partículas con blockdata solo a jugadores NO exentos
     */
    protected void spawnParticleForNonExempt(org.bukkit.World world, org.bukkit.Particle particle, 
                                            org.bukkit.Location loc, int count, 
                                            double offsetX, double offsetY, double offsetZ, double speed, Object data) {
        for (Player player : world.getPlayers()) {
            if (!isPlayerExempt(player)) {
                player.spawnParticle(particle, loc, count, offsetX, offsetY, offsetZ, speed, data);
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // MÉTODOS OPTIMIZADOS CON PERFORMANCE ADAPTER
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * [OPTIMIZADO] Spawnea partículas respetando el PerformanceAdapter.
     * Reduce la cantidad de partículas según el estado de rendimiento.
     */
    protected void spawnParticleOptimized(org.bukkit.World world, org.bukkit.Particle particle, 
                                         org.bukkit.Location loc, int baseCount, 
                                         double offsetX, double offsetY, double offsetZ, double speed) {
        double scale = getPerformanceScale();
        if (scale <= 0.0) return; // SAFE_MODE: no spawnear partículas
        
        int scaledCount = Math.max(1, (int) (baseCount * scale));
        
        for (Player player : world.getPlayers()) {
            if (!isPlayerExempt(player)) {
                player.spawnParticle(particle, loc, scaledCount, offsetX, offsetY, offsetZ, speed);
            }
        }
    }
    
    /**
     * [OPTIMIZADO] Spawnea partículas con blockdata respetando el PerformanceAdapter
     */
    protected void spawnParticleOptimized(org.bukkit.World world, org.bukkit.Particle particle, 
                                         org.bukkit.Location loc, int baseCount, 
                                         double offsetX, double offsetY, double offsetZ, double speed, Object data) {
        double scale = getPerformanceScale();
        if (scale <= 0.0) return; // SAFE_MODE: no spawnear partículas
        
        int scaledCount = Math.max(1, (int) (baseCount * scale));
        
        for (Player player : world.getPlayers()) {
            if (!isPlayerExempt(player)) {
                player.spawnParticle(particle, loc, scaledCount, offsetX, offsetY, offsetZ, speed, data);
            }
        }
    }
    
    /**
     * [OPTIMIZADO] Verifica si debe skipear efectos pesados según rendimiento
     * @return true si el rendimiento es crítico y debe reducir efectos
     */
    protected boolean shouldReduceEffects() {
        return getPerformanceScale() < 0.7;
    }
    
    /**
     * [OPTIMIZADO] Verifica si está en SAFE_MODE
     * @return true si debe pausar todos los efectos pesados
     */
    protected boolean isInSafeMode() {
        return getPerformanceScale() == 0.0;
    }
}
