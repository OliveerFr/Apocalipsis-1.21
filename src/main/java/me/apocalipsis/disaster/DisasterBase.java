package me.apocalipsis.disaster;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
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
    
    // Sistema de supervivencia y recompensas
    protected Map<UUID, Integer> playerSurvivalPhases = new HashMap<>();
    protected Map<UUID, Integer> playerDeathsDuringDisaster = new HashMap<>();
    
    // Sistema de BossBar
    protected BossBar disasterBossBar;
    protected int currentPhase = 1;
    protected int totalPhases = 5;

    public DisasterBase(Apocalipsis plugin, MessageBus messageBus, SoundUtil soundUtil,
                       TimeService timeService, PerformanceAdapter performanceAdapter, String id) {
        this.plugin = plugin;
        this.messageBus = messageBus;
        this.soundUtil = soundUtil;
        this.timeService = timeService;
        this.performanceAdapter = performanceAdapter;
        this.id = id;
        this.instanceId = INSTANCE_COUNTER.incrementAndGet();
        
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[DisasterBase] Creada instancia #" + instanceId + " de " + id);
        }
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
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[Disaster] START: " + id + " #" + instanceId);
        }
        onStart();
    }

    @Override
    public void stop() {
        if (!active) {
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().warning("[Disaster] Intento de detener " + id + " #" + instanceId + " que ya está inactivo - IGNORADO");
            }
            return;
        }
        
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[Disaster] STOP: " + id + " #" + instanceId);
        }
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
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[Disaster] STOP automático: " + id + " #" + instanceId + " estado=" + estado);
            }
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
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE MEJORAS DE DESASTRES (v1.17.0)
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Muestra advertencia previa 30 segundos antes del desastre
     * Debe llamarse desde DisasterController antes de start()
     */
    public void showPreWarning() {
        Bukkit.broadcastMessage("§6⚠ §e§lALERTA TEMPRANA §6⚠");
        Bukkit.broadcastMessage("§7Un desastre se aproxima...");
        
        // Sonido de campana para todos
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 0.5f);
            
            // Partículas de advertencia en el cielo
            Location skyLoc = p.getLocation().add(0, 20, 0);
            spawnWarningParticles(p, skyLoc);
        }
    }
    
    /**
     * Genera partículas de advertencia en el cielo
     */
    private void spawnWarningParticles(Player player, Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        
        player.spawnParticle(Particle.LARGE_SMOKE, loc, 50, 10, 5, 10, 0.1);
        player.spawnParticle(Particle.CLOUD, loc, 30, 10, 5, 10, 0.05);
    }
    
    /**
     * Crea la BossBar del desastre
     */
    protected void createDisasterBossBar(String disasterName) {
        disasterBossBar = Bukkit.createBossBar(
            "§c§l" + disasterName + " §7- §eFase 1/" + totalPhases,
            BarColor.RED,
            BarStyle.SEGMENTED_10
        );
        disasterBossBar.setProgress(1.0 / totalPhases);
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            disasterBossBar.addPlayer(p);
        }
    }
    
    /**
     * Actualiza la fase de la BossBar
     */
    protected void updateBossBarPhase(int phase) {
        if (disasterBossBar == null) return;
        
        currentPhase = phase;
        double progress = (double) phase / totalPhases;
        disasterBossBar.setProgress(progress);
        
        String phaseName = getPhaseDisplayName(phase);
        disasterBossBar.setTitle("§c§l" + getDisasterName() + " §7- §e" + phaseName);
        
        // Color según intensidad
        if (phase <= 2) {
            disasterBossBar.setColor(BarColor.YELLOW);
        } else if (phase <= 4) {
            disasterBossBar.setColor(BarColor.RED);
        } else {
            disasterBossBar.setColor(BarColor.PURPLE);
        }
    }
    
    /**
     * Remueve la BossBar
     */
    protected void removeDisasterBossBar() {
        if (disasterBossBar != null) {
            disasterBossBar.removeAll();
            disasterBossBar = null;
        }
    }
    
    /**
     * Obtiene el nombre display de la fase actual
     */
    protected String getPhaseDisplayName(int phase) {
        return "Fase " + phase + "/" + totalPhases;
    }
    
    /**
     * Obtiene el nombre del desastre para mostrar
     */
    protected abstract String getDisasterName();
    
    /**
     * Muestra títulos mejorados al cambiar de fase
     */
    protected void showPhaseTitle(int phase, String disasterName) {
        String[] phaseNames = getPhaseNames();
        
        String title = "§l" + disasterName.toUpperCase();
        String subtitle = phaseNames[Math.min(phase - 1, phaseNames.length - 1)];
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(title, subtitle, 10, 40, 20);
            
            // Sonido según fase
            Sound sound = phase < 3 ? Sound.BLOCK_NOTE_BLOCK_BASS : Sound.ENTITY_ENDER_DRAGON_GROWL;
            float pitch = 0.5f + (phase * 0.2f);
            p.playSound(p.getLocation(), sound, 1.0f, pitch);
        }
    }
    
    /**
     * Obtiene los nombres de las fases del desastre
     */
    protected abstract String[] getPhaseNames();
    
    /**
     * Registra que un jugador sobrevivió una fase
     */
    protected void trackPlayerSurvival(Player player, int phase) {
        UUID uuid = player.getUniqueId();
        playerSurvivalPhases.put(uuid, phase);
    }
    
    /**
     * Registra muerte de jugador durante el desastre
     */
    protected void handlePlayerDeathInDisaster(Player player) {
        UUID uuid = player.getUniqueId();
        playerDeathsDuringDisaster.put(uuid, 
            playerDeathsDuringDisaster.getOrDefault(uuid, 0) + 1);
        
        player.sendMessage("§c§l☠ §7Has muerto durante el desastre. §e¡No te rindas!");
    }
    
    /**
     * Obtiene la fase actual del desastre basado en el tick
     */
    protected int getCurrentPhaseFromTick(int maxTicks) {
        double progress = (double) tickCounter / maxTicks;
        return Math.min((int) (progress * totalPhases) + 1, totalPhases);
    }
}
