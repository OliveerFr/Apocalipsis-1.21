package me.apocalipsis.disaster;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.disaster.adapters.PerformanceAdapter;
import me.apocalipsis.state.TimeService;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;

public abstract class DisasterBase implements Disaster {

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

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void start() {
        this.active = true;
        this.tickCounter = 0;
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[Cycle][DEBUG] onStart: " + id);
        }
        onStart();
    }

    @Override
    public void stop() {
        this.active = false;
        onStop();
    }

    @Override
    public void tick() {
        if (!active) return;
        
        // Early return si el estado NO es ACTIVO (leer desde state.yml)
        String estado = plugin.getStateManager().getEstado();
        if (!"ACTIVO".equals(estado)) {
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[Cycle] Desastre " + id + " detenido: estado=" + estado);
            }
            stop();
            return;
        }
        
        tickCounter++;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[Cycle][DEBUG] applyEffects: " + id + " player=" + player.getName());
            }
            applyEffects(player);
        }
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[Cycle][DEBUG] onTick: " + id);
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
