package me.apocalipsis.disaster;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.state.ServerState;
import me.apocalipsis.state.StateManager;
import me.apocalipsis.state.TimeService;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;

public class DisasterController {

    private final Apocalipsis plugin;
    private final StateManager stateManager;
    private final TimeService timeService;
    private final DisasterRegistry registry;
    private final MessageBus messageBus;
    private final SoundUtil soundUtil;

    // [RECONSTRUCCIÓN] Fuente única de tiempo (evitar duplicación BossBars)
    private BukkitTask uiTask;
    private BukkitTask nextTask;
    private final AtomicBoolean starting = new AtomicBoolean(false);
    private long lastCycleLog = 0L;
    private final AtomicBoolean cooldownAutoStartAttempted = new AtomicBoolean(false);
    
    // [ANTIRREBOTE] Timestamp cuando se entra en PREPARACION (evita inicio en mismo tick)
    private long enteredPreparationAtMs = 0L;
    
    // [RATE-LIMIT] Logs antispam (1/segundo por tipo)
    private long lastCooldownBlockLog = 0L;
    private long lastMinPlayersBlockLog = 0L;
    private long lastCooldownReadyLog = 0L;
    
    // [RECONSTRUCCIÓN] BossBar ÚNICA (reutilizable)
    private BossBar bossBar;
    
    private Disaster activeDisaster;
    private int taskId = -1;
    private int cooldownTaskId = -1;
    
    // [HOTFIX] SAFE MODE: pausar desastre sin destruirlo
    private boolean disasterPaused = false;
    private Disaster pausedDisaster = null;
    
    // [#9] SAFE MODE por TPS y TNT
    private long tpsLowSince = 0L;
    private int safeModeAutoExitTaskId = -1;
    private final java.util.Deque<Long> tntExplosionTimes = new java.util.ArrayDeque<>();
    private long lastSafeLogTs = 0L;

    public DisasterController(Apocalipsis plugin, StateManager stateManager, TimeService timeService,
                             DisasterRegistry registry, MessageBus messageBus, SoundUtil soundUtil) {
        this.plugin = plugin;
        this.stateManager = stateManager;
        this.timeService = timeService;
        this.registry = registry;
        this.messageBus = messageBus;
        this.soundUtil = soundUtil;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // GUARDA CENTRAL - Verifica si se puede iniciar un desastre
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Verifica si se puede iniciar un desastre (auto o manual sin force)
     * Esta es la ÚNICA guarda centralizada que bloquea inicios no autorizados.
     * 
     * @param isForced true si es /avo force (ignora algunas restricciones)
     * @return true si se puede iniciar
     */
    private boolean puedeIniciarDesastre(boolean isForced, boolean isManual) {
        ServerState estado = stateManager.getCurrentState();
        boolean debugCiclo = plugin.getConfigManager().isDebugCiclo();
        
        // 1. SAFE_MODE bloquea todo (incluso force)
        if (stateManager.isSafeModeActive()) {
            if (debugCiclo) {
                plugin.getLogger().info("[Cycle] BLOQUEADO por SAFE_MODE activo");
            }
            return false;
        }
        
        // 2. Si es forzado, saltamos TODAS las verificaciones (excepto SAFE_MODE)
        if (isForced) {
            if (debugCiclo) {
                plugin.getLogger().info("[Cycle] PERMITIDO por /avo force (ignora estado, auto_cycle, jugadores, cooldown)");
            }
            return true;
        }
        
        // 3. Estado debe ser DETENIDO o PREPARACION (solo para no-forced)
        if (estado != ServerState.PREPARACION && estado != ServerState.DETENIDO) {
            if (debugCiclo) {
                plugin.getLogger().info("[Cycle] BLOQUEADO inicio: estado=" + estado);
            }
            return false;
        }
        
        // 4. auto_cycle debe estar habilitado SOLO para inicios automáticos (no manuales)
        boolean autoCycle = plugin.getConfigManager().isAutoCycleEnabled();
        if (!autoCycle && !isManual) {
            if (debugCiclo) {
                plugin.getLogger().info("[Cycle] BLOQUEADO: auto_cycle=false y no es inicio manual");
            }
            return false;
        }
        
        // 4. Verificar jugadores mínimos
        int minJugadores = plugin.getConfigManager().getMinJugadores();
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        if (onlinePlayers < minJugadores) {
            if (debugCiclo) {
                plugin.getLogger().info("[Cycle] BLOQUEADO: jugadores=" + onlinePlayers + " < min=" + minJugadores);
            }
            return false;
        }
        
        // 5. Verificar cooldown (solo para inicios automáticos, no manuales)
        boolean respetarCooldown = plugin.getConfigManager().isRespetarCooldown();
        if (respetarCooldown && !isManual) {
            long lastEnd = stateManager.getLastEndEpochMs();
            long cooldownMs = plugin.getConfigManager().getCooldownFinSegundos() * 1000L;
            long elapsed = System.currentTimeMillis() - lastEnd;
            
            if (elapsed < cooldownMs) {
                long remainingSeconds = (cooldownMs - elapsed) / 1000L;
                if (debugCiclo) {
                    plugin.getLogger().info("[Cycle] BLOQUEADO por cooldown: faltan " + remainingSeconds + "s");
                }
                return false;
            }
        } else if (isManual && debugCiclo) {
            plugin.getLogger().info("[Cycle] Cooldown ignorado por inicio manual");
        }
        
        // Todo OK
        if (debugCiclo) {
            plugin.getLogger().info("[Cycle] PERMITIDO: todas las condiciones satisfechas");
        }
        return true;
    }

    public void startTask() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L).getTaskId();
    }

    public void cancelTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }
    
    /**
     * Cancela TODAS las tareas programadas (tick, cooldown, auto-next)
     */
    public void cancelAllTasks() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        if (cooldownTaskId != -1) {
            Bukkit.getScheduler().cancelTask(cooldownTaskId);
            cooldownTaskId = -1;
        }
        // Solo cancelar nextTask si hay desastre activo o preparación forzada
        String estado = stateManager.getEstado();
        boolean isPrepForzada = stateManager.isPrepForzada();
        if (nextTask != null && !nextTask.isCancelled() && ("ACTIVO".equals(estado) || ("PREPARACION".equals(estado) && isPrepForzada))) {
            plugin.getLogger().info("[Cycle][DEBUG] cancelAllTasks: cancelando nextTask (auto-next)");
            nextTask.cancel();
            nextTask = null;
        }
    }
    
    /**
     * Cancela solo tareas auxiliares (cooldown, auto-next) pero mantiene el tick principal
     * Usado durante /avo reload cuando hay desastre activo
     */
    public void cancelCooldownAndNextTasks() {
        if (cooldownTaskId != -1) {
            Bukkit.getScheduler().cancelTask(cooldownTaskId);
            cooldownTaskId = -1;
        }
        if (nextTask != null && !nextTask.isCancelled()) {
            nextTask.cancel();
            nextTask = null;
        }
    }
    
    /**
     * Reset de la bandera starting para cancelar tryStart en curso
     */
    public void resetStartingFlag() {
        starting.set(false);
    }
    
    /**
     * Marca entrada en PREPARACION (usado por /avo preparacion para antirrebote)
     */
    public void markEnteredPreparation() {
        enteredPreparationAtMs = System.currentTimeMillis();
    }
    
    /**
     * Oculta la BossBar durante preparación forzada
     */
    public void hideBossBar() {
        if (bossBar != null) {
            bossBar.setVisible(false);
        }
    }

    private void tick() {
        ServerState state = stateManager.getCurrentState();

        // [#9] Check TPS para SAFE_MODE (mejorado con min_segundos)
        if (plugin.getConfigManager().isSafeModeEnabled()) {
            checkTPSForSafeMode(state);
        }
        
        // [FIX] Auto-start se maneja SOLO por scheduleAutoNext() 
        // NUNCA auto-iniciar desde tick(), solo desde el scheduler dedicado

        // Tick del desastre activo (efectos por tick)
        if (state == ServerState.ACTIVO && activeDisaster != null && activeDisaster.isActive() && !disasterPaused) {
            if (plugin.getConfigManager().isDebugCiclo() && Bukkit.getCurrentTick() % 100 == 0) {
                plugin.getLogger().info("[Cycle] Tick activo: desastre=" + activeDisaster.getId() + " tick#=" + Bukkit.getCurrentTick());
            }
            activeDisaster.tick();
        } else if (plugin.getConfigManager().isDebugCiclo() && Bukkit.getCurrentTick() % 100 == 0) {
            plugin.getLogger().info("[Cycle] Tick SALTADO: state=" + state + " activeDisaster=" + (activeDisaster != null) + " isActive=" + (activeDisaster != null && activeDisaster.isActive()) + " paused=" + disasterPaused);
        }

        // UI y tiempo cada 20 ticks (1 segundo)
        if (Bukkit.getCurrentTick() % 20 == 0) {
            // Verificar fin de tiempo
            if (timeService.isFinished()) {
                onTimeFinished();
            }
        }
    }
    
    /**
     * [#9] Verificar TPS y activar SAFE_MODE si está bajo durante min_segundos
     */
    private void checkTPSForSafeMode(ServerState state) {
        double tps = getAverageTPS();
        double umbral = plugin.getConfigManager().getSafeModeTPSUmbral();
        
        if (tps < umbral) {
            if (tpsLowSince == 0L) {
                tpsLowSince = System.currentTimeMillis();
            } else {
                long elapsed = System.currentTimeMillis() - tpsLowSince;
                int minSegundos = plugin.getConfigManager().getSafeModeMinSegundos();
                
                if (elapsed >= minSegundos * 1000L && state == ServerState.ACTIVO) {
                    enterSafeModeTPS();
                    tpsLowSince = 0L;
                }
            }
        } else {
            tpsLowSince = 0L; // Reset si TPS se recupera
        }
    }

    private void onTimeFinished() {
        ServerState state = stateManager.getCurrentState();
        
        if (state == ServerState.PREPARACION) {
            endPreparation();
        } else if (state == ServerState.ACTIVO) {
            stopDisaster();
        }
    }

    /**
     * Inicia preparación forzada (silencio de desastres por X minutos)
     * ESCRIBE en state.yml: estado=PREPARACION, start_epoch_ms, end_epoch_ms
     */
    public void startPreparation(int minutes) {
        // Cancelar cualquier desastre activo
        if (activeDisaster != null && activeDisaster.isActive()) {
            activeDisaster.stop();
            activeDisaster = null;
        }
        cancelUITicker();
        if (bossBar != null) {
            bossBar.setVisible(false);
        }
        
        // Calcular duración según test mode
        int duracionSeg;
        if (plugin.getConfigManager().isTestMode()) {
            duracionSeg = 5; // 5 segundos en test mode
        } else {
            duracionSeg = minutes * 60;
        }
        
        long now = System.currentTimeMillis();
        long startMs = now;
        long endMs = now + duracionSeg * 1000L;
        
        // ESCRIBIR en state.yml
        stateManager.setEstado(ServerState.PREPARACION.name());
        stateManager.setString("desastre_actual", "");
        stateManager.setLong("start_epoch_ms", startMs);
        stateManager.setLong("end_epoch_ms", endMs);
        stateManager.saveState();
        
        // [FIX] Iniciar TimeService para sincronización de tiempo
        timeService.startPreparationMinutes(minutes);
        
        // Iniciar UI ticker (mostrará Scoreboard con countdown, sin BossBar)
        startUiTicker();

        String timeDisplay = plugin.getConfigManager().isTestMode() ? "5 segundos" : minutes + " minutos";
        messageBus.broadcast("§e⚠ §fComienza la preparación. Tiempo: §e" + timeDisplay + "§f.", "prep_start");
        soundUtil.playSoundAll(Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        
        // Programar fin de preparación (auto-start si auto_cycle=true)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String estadoActual = stateManager.getEstado();
            if ("PREPARACION".equals(estadoActual)) {
                endPreparation();
            }
        }, duracionSeg * 20L);
    }

    /**
     * Fin de preparación forzada → auto-start si auto_cycle=true
     */
    private void endPreparation() {
        messageBus.broadcast("§e☁ §fFin de la preparación. ¡El caos regresa!", "prep_end");
        soundUtil.playSoundAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.8f);

        // Limpiar timestamps de preparación forzada
        stateManager.setLong("start_epoch_ms", 0L);
        stateManager.setLong("end_epoch_ms", 0L);
        stateManager.saveState();
        
        // Si auto_cycle=true, intentar inicio automático
        if (plugin.getConfigManager().isAutoCycleEnabled()) {
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[Cycle] Fin de preparación forzada, intentando auto-start...");
            }
            tryStartRandomDisaster("cooldown");
        } else {
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[Cycle] Fin de preparación forzada, pero auto_cycle=false");
            }
        }
    }

    /**
     * Inicia un desastre automático con selección aleatoria ponderada
     * SIEMPRE verifica puedeIniciarDesastre() antes de iniciar
     */
    /**
     * [RECONSTRUCCIÓN] Método llamado desde onEnable() o comandos
     * Usa la nueva puerta única tryStartRandomDisaster
     */
    public void startAuto(boolean excludeLast) {
        tryStartRandomDisaster("command");
    }

    /**
     * [RECONSTRUCCIÓN] Inicia un desastre específico (usado por comandos)
     * Redirige a tryStartForced para usar la nueva arquitectura
     */
    public void startDisaster(String disasterId) {
        tryStartForced(disasterId, "command");
    }

    public void resumeDisaster(String disasterId) {
        if (!registry.exists(disasterId)) {
            plugin.getLogger().warning("No se puede reanudar desastre desconocido: " + disasterId);
            return;
        }

        Disaster disaster = registry.get(disasterId);
        activeDisaster = disaster;
        stateManager.setActiveDisasterId(disasterId);
        stateManager.setState(ServerState.ACTIVO);
        disaster.start();
        
        // [RE-FIX] Iniciar UI Ticker al reanudar tras boot
        startUITicker();

        plugin.getLogger().info("Desastre reanudado: " + disasterId);
    }

    public void stopDisaster() {
        if (activeDisaster == null || !activeDisaster.isActive()) return;

        String disasterId = activeDisaster.getId();
        activeDisaster.stop();
        
        // [RE-FIX] Cancelar UI Ticker y resetear BossBar
        cancelUITicker();
        if (bossBar != null) {
            bossBar.setVisible(false);
        }

        messageBus.broadcast("§a§l¡DESASTRE FINALIZADO! §f" + disasterId.toUpperCase().replace("_", " "), "disaster_end");
        soundUtil.playSoundAll(Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        soundUtil.playSoundAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 2.0f);

        // [FIX] Guardar timestamp de finalización para cooldown
        stateManager.setLastDisasterId(disasterId);
        stateManager.setLastEndEpochMs(System.currentTimeMillis());

        activeDisaster = null;
        stateManager.setActiveDisasterId(null);
        
        // [HOTFIX] NO ir a PREPARACION, ir a DETENIDO (PREPARACION solo manual)
        stateManager.setState(ServerState.DETENIDO);
        timeService.end();
        
        // [FIX] Cancelar cualquier tarea pendiente
        cancelAllTasks();
        
        stateManager.saveState();

        // [DEPRECATED] scheduleCooldownNextDisaster() removido - usar scheduleAutoNext() que respeta todas las guardias
        // El auto-next se maneja desde onDisasterEnd() con el sistema unificado
    }

    /**
     * Detiene todos los desastres activos
     * @param announce Si true, anuncia la detención
     * @param changeState Si true, cambia el estado a DETENIDO (false para switches rápidos)
     */
    public void stopAllDisasters(boolean announce, boolean changeState) {
        if (activeDisaster != null && activeDisaster.isActive()) {
            activeDisaster.stop();
            if (announce) {
                messageBus.broadcast("§7Desastre detenido manualmente.", "disaster_stop");
            }
        }
        
        // [RE-FIX] Cancelar UI Ticker y resetear BossBar
        cancelUITicker();
        if (bossBar != null) {
            bossBar.setVisible(false);
        }
        
        activeDisaster = null;
        stateManager.setActiveDisasterId(null);
        
        // Solo cambiar estado si es una detención real (no un switch)
        if (changeState) {
            stateManager.setState(ServerState.DETENIDO);
        }
        
        timeService.end();
        
        // [FIX] Cancelar tareas programadas (cooldown, auto-next) pero NO el task principal de tick
        if (cooldownTaskId != -1) {
            Bukkit.getScheduler().cancelTask(cooldownTaskId);
            cooldownTaskId = -1;
        }
        
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[Cycle] STOP: desastre detenido, tareas auxiliares canceladas (changeState=" + changeState + ")");
        }
        
        if (changeState) {
            stateManager.saveState();
        }
    }
    
    /**
     * Sobrecarga para compatibilidad con código existente
     */
    public void stopAllDisasters(boolean announce) {
        stopAllDisasters(announce, true);
    }



    /**
     * [#9] Entrar en SAFE MODE por TPS bajo
     */
    private void enterSafeModeTPS() {
        // [HOTFIX] Debounce: no repetir log más de 1 vez cada 3s
        long now = System.currentTimeMillis();
        if (stateManager.isSafeModeActive() && now - lastSafeLogTs < 3000) {
            return;
        }
        lastSafeLogTs = now;
        
        messageBus.broadcast("§6§l⚠ SAFE MODE activado por TPS bajo. Desastre pausado.", "safe_mode");
        plugin.getLogger().warning("[SAFE MODE] Activado por TPS bajo. Desastre pausado.");
        
        enterSafeModeCommon();
    }
    
    /**
     * [#9] Entrar en SAFE MODE por TNT masiva
     */
    public void enterSafeModeTNT() {
        // [HOTFIX] Debounce: no repetir log más de 1 vez cada 3s
        long now = System.currentTimeMillis();
        if (stateManager.isSafeModeActive() && now - lastSafeLogTs < 3000) {
            return;
        }
        lastSafeLogTs = now;
        
        messageBus.broadcast("§6§l⚠ SAFE MODE activado por TNT masiva. Desastre pausado.", "safe_mode");
        plugin.getLogger().warning("[SAFE MODE] Activado por explosiones TNT masivas.");
        
        enterSafeModeCommon();
    }
    
    /**
     * [HOTFIX] Lógica común de entrada a SAFE MODE - PAUSAR sin destruir desastre
     */
    private void enterSafeModeCommon() {
        // [HOTFIX] Marcar desastre como pausado (tick() lo ignorará)
        if (activeDisaster != null && activeDisaster.isActive()) {
            disasterPaused = true;
            pausedDisaster = activeDisaster;
            plugin.getLogger().info("[SAFE MODE] Desastre pausado (tick ignorado hasta salir de SAFE)");
        }
        
        // Cancelar todas las tareas programadas (tick continúa pero ignora desastre pausado)
        if (cooldownTaskId != -1) {
            Bukkit.getScheduler().cancelTask(cooldownTaskId);
            cooldownTaskId = -1;
        }
        
        // [HOTFIX] NO cambiar estado a PREPARACION, quedarse en ACTIVO pero con flag SAFE
        stateManager.setSafeModeActive(true);
        stateManager.saveState();
        
        // [#9] Programar salida automática
        scheduleAutoExitSafeMode();
    }
    
    /**
     * [#9] Programar salida automática de SAFE MODE
     */
    private void scheduleAutoExitSafeMode() {
        if (safeModeAutoExitTaskId != -1) {
            Bukkit.getScheduler().cancelTask(safeModeAutoExitTaskId);
        }
        
        int segundos = plugin.getConfigManager().getSafeModeAutoSalirSegundos();
        
        safeModeAutoExitTaskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (stateManager.getCurrentState() == ServerState.SAFE_MODE) {
                exitSafeMode();
            }
            safeModeAutoExitTaskId = -1;
        }, segundos * 20L).getTaskId();
    }
    
    public void enterSafeMode() {
        enterSafeModeTPS();
    }

    public void exitSafeMode() {
        if (!stateManager.isSafeModeActive()) {
            return;
        }
        
        messageBus.broadcast("§a✓ TPS recuperado. Reanudando operaciones.", "safe_mode_exit");
        plugin.getLogger().info("[SAFE MODE] Desactivado. TPS estable.");
        
        // [HOTFIX] Reanudar desastre pausado si existe
        if (disasterPaused && pausedDisaster != null && stateManager.getCurrentState() == ServerState.ACTIVO) {
            plugin.getLogger().info("[CICLO] Reanudando desastre después de SAFE MODE");
            activeDisaster = pausedDisaster;
            disasterPaused = false;
            pausedDisaster = null;
            
            // El tick() ya volverá a ejecutar los efectos del desastre
        }
        
        stateManager.setSafeModeActive(false);
        
        // [#9] Cancelar tarea de auto-salida
        if (safeModeAutoExitTaskId != -1) {
            Bukkit.getScheduler().cancelTask(safeModeAutoExitTaskId);
            safeModeAutoExitTaskId = -1;
        }
        
        // [HOTFIX] NO cambiar a PREPARACION si había desastre activo
        // Solo si estaba en PREPARACION, mantener PREPARACION
    }
    
    /**
     * [HOTFIX] Registrar explosión TNT con Sliding Window real
     */
    public void onTNTExplosion() {
        if (!plugin.getConfigManager().isTNTControlEnabled()) {
            return;
        }
        
        long now = System.currentTimeMillis();
        long ventana = plugin.getConfigManager().getTNTVentanaMs();
        
        // Limpiar explosiones fuera de ventana (sliding window)
        while (!tntExplosionTimes.isEmpty() && now - tntExplosionTimes.peekFirst() > ventana) {
            tntExplosionTimes.pollFirst();
        }
        
        // Añadir explosión actual
        tntExplosionTimes.addLast(now);
        
        // Verificar si excede el límite
        int limit = plugin.getConfigManager().getTNTMaxExplosiones();
        if (!stateManager.isSafeModeActive() && tntExplosionTimes.size() >= limit) {
            // [HOTFIX] Debounce: evitar spam de logs
            if (now - lastSafeLogTs >= 3000) {
                plugin.getLogger().warning("[TNT] " + tntExplosionTimes.size() + "/" + limit + 
                    " explosiones en " + ventana + "ms → Activando SAFE MODE");
                enterSafeModeTNT();
            }
        }
    }

    public Disaster getActiveDisaster() {
        return activeDisaster;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Métodos auxiliares
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Formatea milisegundos a MM:SS
     */
    private String formatMMSS(long ms) {
        long totalSecs = ms / 1000;
        long mins = totalSecs / 60;
        long secs = totalSecs % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    private double getAverageTPS() {
        try {
            return Bukkit.getTPS()[0];
        } catch (Exception e) {
            return 20.0; // Fallback
        }
    }
    
    /**
     * Cancela el ticker de UI (usado tanto por sistema viejo como nuevo)
     */
    private void cancelUITicker() {
        if (uiTask != null && !uiTask.isCancelled()) {
            uiTask.cancel();
        }
        uiTask = null;
    }
    
    /**
     * Inicia UI ticker básico (usado por sistema viejo - será reemplazado)
     */
    private void startUITicker() {
        // Redirigir al nuevo sistema
        startUiTicker();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // [RECONSTRUCCIÓN] Métodos principales del nuevo sistema
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Asegura que existe una BossBar única (no duplicar)
     */
    private void ensureBossBar() {
        if (bossBar == null) {
            bossBar = Bukkit.createBossBar("§7Esperando...", BarColor.WHITE, BarStyle.SOLID);
            bossBar.setVisible(false);
            
            // Agregar todos los jugadores online
            for (Player p : Bukkit.getOnlinePlayers()) {
                bossBar.addPlayer(p);
            }
        }
    }
    
    /**
     * Logs con rate-limit (evitar spam)
     */
    private void logOnce(long ms, String msg) {
        long now = System.currentTimeMillis();
        if (now - lastCycleLog >= ms) {
            plugin.getLogger().info("[Apocalipsis] " + msg);
            lastCycleLog = now;
        }
    }
    
    /**
     * Scheduler único para auto-next (1/s)
     * Solo actúa en PREPARACION con cooldown expirado
     * LEE de state.yml: estado, last_end_epoch_ms
     */
    /**
     * Scheduler de auto-next (1 vez/seg, único)
     * Solo inicia si: estado==PREPARACION, auto_cycle==true, online>=min_jugadores, cooldown cumplido
     */
    public void scheduleAutoNext() {
        if (nextTask != null && !nextTask.isCancelled()) {
            nextTask.cancel();
        }
        
        nextTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();

            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[Cycle][DEBUG] scheduleAutoNext tick: now=" + now);
            }
                
            // [ANTIRREBOTE] Evitar inicio en mismo tick tras cambio a PREPARACION
            if (now - enteredPreparationAtMs < 250L) {
                return;
            }
            
            // Leer estado desde state.yml (fuente única)
            String estado = stateManager.getEstado();
            
            // Permitir funcionamiento en PREPARACION (normal, no forzada) y DETENIDO
            boolean isPrep = "PREPARACION".equals(estado);
            boolean isDetenido = "DETENIDO".equals(estado);
            boolean prepForzada = stateManager.isPrepForzada();
            long endEpochMs = stateManager.getLong("end_epoch_ms", 0L);

            // Si es preparación forzada, solo iniciar cuando termine la ventana
            if (isPrep && prepForzada) {
                if (endEpochMs > 0 && now >= endEpochMs) {
                    int minJugadores = plugin.getConfigManager().getMinJugadores();
                    if (Bukkit.getOnlinePlayers().size() >= minJugadores) {
                        plugin.getLogger().info("[Cycle] Fin de PREPARACION forzada → iniciando");
                        // Limpiar flag para evitar bucle infinito de intentos
                        stateManager.setPrepForzada(false);
                        stateManager.saveState();
                        tryStartRandomDisaster("prep_forzada_end");
                    } else {
                        plugin.getLogger().info("[Cycle] Esperando jugadores mínimos: " + Bukkit.getOnlinePlayers().size() + "/" + minJugadores);
                    }
                }
                return; // Mientras siga forzada, jamás iniciar
            }

            // Solo continuar si estamos en PREPARACION (normal, no forzada) o DETENIDO
            if (!(isPrep && !prepForzada) && !isDetenido) {
                return;
            }
            
            // 3) Verificar auto_cycle
            boolean autoCycle = plugin.getConfigManager().isAutoCycleEnabled();
            if (!autoCycle) {
                return;
            }
            
            // 4) Verificar jugadores mínimos
            int minJugadores = plugin.getConfigManager().getMinJugadores();
            int jugadoresOnline = Bukkit.getOnlinePlayers().size();
            if (jugadoresOnline < minJugadores) {
                return;
            }
            
            // 5) Verificar cooldown cumplido
            long lastEndMs = stateManager.getLastEndEpochMs();
            
            // Validación: Si no hay desastre previo, no calcular cooldown
            if (lastEndMs <= 0) {
                return;
            }
            
            long cooldownMs = plugin.getConfigManager().getCooldownFinSegundos() * 1000L;
            long elapsed = now - lastEndMs;
            
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[Cycle] Cooldown check: elapsed=" + (elapsed/1000) + "s, required=" + (cooldownMs/1000) + "s, lastEnd=" + lastEndMs);
            }
            
            if (elapsed < cooldownMs) {
                return; // Cooldown no cumplido
            }
            
            // Cooldown cumplido - mostrar tiempo transcurrido
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[Cycle] ✓ Cooldown cumplido (" + (elapsed/1000) + "s transcurridos) → intentando iniciar");
            }
            
            // 6) Intentar iniciar (puerta única con AtomicBoolean)
            if (cooldownAutoStartAttempted.compareAndSet(false, true)) {
                tryStartRandomDisaster("cooldown");
            }
            
        }, 20L, 20L); // Cada 1 segundo
    }
    
    /**
     * Puerta única de inicio con guardias correctas según especificaciones
     */
    public void tryStartRandomDisaster(String reason) {
        long now = System.currentTimeMillis();
        
        // Leer estado desde state.yml (fuente única)
        String estado = stateManager.getEstado();
        
        // 1) Estado válido: PREPARACION siempre, o DETENIDO solo si reason=command
        if (!(estado.equals("PREPARACION") || (estado.equals("DETENIDO") && ("command".equals(reason) || "cooldown".equals(reason))))) {
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[Cycle] BLOQUEADO: estado=" + estado + " reason=" + reason);
            }
            return;
        }
        
        // 2) Bloqueo por preparación forzada (ventana activa)
        if ("PREPARACION".equals(estado)) {
            boolean prepForzada = stateManager.isPrepForzada();
            long endEpochMs = stateManager.getLong("end_epoch_ms", 0L);
            
            if (prepForzada && now < endEpochMs) {
                long remainingMs = endEpochMs - now;
                long remainingMin = remainingMs / 60000L;
                long remainingSec = (remainingMs % 60000L) / 1000L;
                if (plugin.getConfigManager().isDebugCiclo()) {
                    plugin.getLogger().info("[Cycle] Bloqueado: prep_forzada activa (" + remainingMin + ":" + String.format("%02d", remainingSec) + ")");
                }
                return; // Nunca iniciar dentro de la ventana forzada
            }
        }
        
        // 3) Cooldown: solo aplica si reason == "cooldown" (NO para prep_forzada_end ni command)
        if ("cooldown".equals(reason)) {
            long lastEndMs = stateManager.getLastEndEpochMs();
            long cooldownMs = plugin.getConfigManager().getCooldownFinSegundos() * 1000L;
            long elapsed = now - lastEndMs;
            
            if (elapsed < cooldownMs) {
                long remainingSeconds = (cooldownMs - elapsed) / 1000L;
                
                // Rate-limit: solo log cada 1 segundo
                if (now - lastCooldownBlockLog >= 1000L) {
                    lastCooldownBlockLog = now;
                    if (plugin.getConfigManager().isDebugCiclo()) {
                        plugin.getLogger().info("[Cycle] Bloqueado por cooldown: faltan " + remainingSeconds + "s");
                    }
                }
                return;
            }
            
            // Cooldown cumplido: log solo una vez
            if (now - lastCooldownReadyLog >= 1000L) {
                lastCooldownReadyLog = now;
                if (plugin.getConfigManager().isDebugCiclo()) {
                    plugin.getLogger().info("[Cycle] Cooldown cumplido → intentando iniciar");
                }
            }
        }
        
        // 4) Min jugadores
        int minJugadores = plugin.getConfigManager().getMinJugadores();
        int jugadoresOnline = plugin.getServer().getOnlinePlayers().size();
        if (jugadoresOnline < minJugadores) {
            // Rate-limit: solo log cada 1 segundo
            if (now - lastMinPlayersBlockLog >= 1000L) {
                lastMinPlayersBlockLog = now;
                if (plugin.getConfigManager().isDebugCiclo()) {
                    plugin.getLogger().info("[Cycle] Bloqueado por min_jugadores (" + jugadoresOnline + "/" + minJugadores + ")");
                }
            }
            return;
        }
        
        // 5) Anti-race
        if (!starting.compareAndSet(false, true)) {
            logOnce(1000, "[CICLO] Inicio concurrente bloqueado");
            return;
        }
        
        try {
            // 6) Elegir desastre según weights (excluyendo último)
            String disasterId = elegirSegunWeight();
            if (disasterId == null) {
                logOnce(5000, "[CICLO] No se pudo elegir desastre (weights inválidos)");
                return;
            }
            
            // 7) iniciarDesastreInterno() maneja toda la configuración y guardado del estado
            iniciarDesastreInterno(disasterId);
            
        } finally {
            starting.set(false);
        }
    }
    
    /**
     * Iniciar desastre forzado (ignora restricciones excepto SAFE MODE)
     */
    public void tryStartForced(String disasterId, String reason) {
        if (!registry.exists(disasterId)) {
            plugin.getLogger().warning("Desastre no encontrado: " + disasterId);
            return;
        }
        
        plugin.getLogger().info("[Cycle] tryStartForced: desastre=" + disasterId + " reason=" + reason + " activeDisaster=" + (activeDisaster != null) + " starting=" + starting.get());
        
        // [FIX] Si hay desastre activo, detenerlo primero (sin cambiar estado a DETENIDO)
        if (activeDisaster != null && activeDisaster.isActive()) {
            plugin.getLogger().info("[Cycle] Deteniendo desastre activo antes de force...");
            stopAllDisasters(false, false); // Sin anuncio, sin cambiar estado
        }
        
        // Anti-race
        if (!starting.compareAndSet(false, true)) {
            plugin.getLogger().warning("[CICLO] Inicio concurrente bloqueado - starting flag ya está en true");
            logOnce(1000, "[CICLO] Inicio concurrente bloqueado");
            return;
        }
        
        try {
            plugin.getLogger().info("[Cycle] Verificando puedeIniciarDesastre con forced=true...");
            if (!puedeIniciarDesastre(true, false)) { // true = forced, false = no manual
                plugin.getLogger().warning("[Cycle] puedeIniciarDesastre retornó false (bloqueado por SAFE_MODE?)");
                return;
            }
            
            plugin.getLogger().info("[Cycle] Iniciando desastre interno: " + disasterId);
            iniciarDesastreInterno(disasterId);
            
        } finally {
            starting.set(false);
            plugin.getLogger().info("[Cycle] tryStartForced finalizado - starting flag liberado");
        }
    }
    
    /**
     * Elegir desastre según weights configurados
     */
    /**
     * Elegir desastre según weights, excluyendo el último desastre jugado.
     * Si solo hay un desastre disponible, permite repetirlo.
     */
    private String elegirSegunWeight() {
        ConfigurationSection weights = plugin.getConfigManager().getDesastresConfig()
            .getConfigurationSection("desastres.weights");
        
        if (weights == null) {
            return "huracan"; // Fallback
        }
        
        // Obtener último desastre desde state.yml
        String ultimoDesastre = stateManager.getLastDisasterId();
        
        // Construir pool con pesos
        List<String> pool = new ArrayList<>();
        List<String> allKeys = new ArrayList<>();
        
        for (String key : weights.getKeys(false)) {
            int weight = weights.getInt(key, 1);
            allKeys.add(key);
            
            // Excluir último desastre si hay más de una opción
            if (ultimoDesastre != null && !ultimoDesastre.isEmpty() && 
                key.equalsIgnoreCase(ultimoDesastre) && weights.getKeys(false).size() > 1) {
                continue; // Saltar el último desastre
            }
            
            for (int i = 0; i < weight; i++) {
                pool.add(key);
            }
        }
        
        // Si el pool quedó vacío (solo había un desastre y era el último), permitir repetir
        if (pool.isEmpty() && !allKeys.isEmpty()) {
            String fallback = allKeys.get(0);
            int weight = weights.getInt(fallback, 1);
            for (int i = 0; i < weight; i++) {
                pool.add(fallback);
            }
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[Cycle] Solo un desastre disponible, permitiendo repetir: " + fallback);
            }
        }
        
        if (pool.isEmpty()) {
            return "huracan";
        }
        
        Random random = new Random();
        String selected = pool.get(random.nextInt(pool.size()));
        
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[Cycle] Desastre elegido: " + selected + " (excluido: " + 
                (ultimoDesastre != null ? ultimoDesastre : "ninguno") + ")");
        }
        
        return selected;
    }
    
    /**
     * Inicio interno del desastre (core del sistema reconstruido)
     * ESCRIBE en state.yml: estado, desastre_actual, start_epoch_ms, end_epoch_ms
     */
    private void iniciarDesastreInterno(String disasterId) {
        // 1) Cancelar tareas/BossBar anteriores
        cancelUITicker();
        stopCurrentDisasterTasks();
        
        // 2) Marcar tiempos
        ConfigurationSection config = plugin.getConfigManager().getDesastresConfig()
            .getConfigurationSection("desastres." + disasterId);
        
        int durSeg = 900; // 15 min default
        if (config != null) {
            durSeg = config.getInt("duracion_segundos", 900);
        }
        
        // Modo test: 20 segundos
        if (plugin.getConfigManager().isTestMode()) {
            durSeg = 20;
        }
        
        long now = System.currentTimeMillis();
        long startMs = now;
        long endMs = now + durSeg * 1000L;
        
        // 3) ESCRIBIR en state.yml (fuente única de verdad)
        stateManager.setEstado(ServerState.ACTIVO.name());
        stateManager.setString("desastre_actual", disasterId);
        stateManager.setLong("start_epoch_ms", startMs);
        stateManager.setLong("end_epoch_ms", endMs);
        stateManager.saveState();
        
        stateManager.setActiveDisasterId(disasterId);
        
        // [FIX] Iniciar TimeService para sincronización de tiempo
        timeService.startDisaster(disasterId, durSeg);
        
        // 4) BossBar única (verde, progreso 0)
        ensureBossBar();
        bossBar.setTitle("§a" + disasterId.toUpperCase().replace("_", " ") + " §7• " + formatMMSS(durSeg * 1000L));
        bossBar.setProgress(0.0);
        bossBar.setColor(BarColor.GREEN);
        bossBar.setVisible(true);
        
        // 5) Lanzar el desastre real
        if (!registry.exists(disasterId)) {
            plugin.getLogger().warning("Desastre no existe: " + disasterId);
            return;
        }
        
        Disaster disaster = registry.get(disasterId);
        activeDisaster = disaster;
        disaster.start();
        
        // 6) Ticker UI único (lee de state.yml)
        startUiTicker();
        
        // 7) Mensajes y sonidos
        messageBus.broadcast("§c§l¡DESASTRE INICIADO! §f" + disasterId.toUpperCase().replace("_", " "), "disaster_start");
        soundUtil.playSoundAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.5f);
        soundUtil.playSoundAll(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.0f);
        
        logOnce(0, "INICIO: " + disasterId.toUpperCase() + " (" + durSeg + "s)");
    }
    
    /**
     * Ticker UI sincronizado (BossBar + Scoreboard)
     * LEE SIEMPRE de state.yml: start_epoch_ms, end_epoch_ms, estado
     */
    private void startUiTicker() {
        cancelUITicker();
        
        uiTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Leer estado desde state.yml (fuente única de verdad)
            String estado = stateManager.getEstado();
            long startMs = stateManager.getLong("start_epoch_ms", 0L);
            long endMs = stateManager.getLong("end_epoch_ms", 0L);
            long now = System.currentTimeMillis();
            
            if ("ACTIVO".equals(estado) && startMs > 0 && endMs > startMs) {
                // ACTIVO: mostrar BossBar verde con progreso
                long rem = Math.max(0L, endMs - now);
                long total = Math.max(1L, endMs - startMs);
                double prog = 1.0 - (double) rem / (double) total;
                
                if (bossBar != null) {
                    bossBar.setProgress(Math.max(0.0, Math.min(1.0, prog)));
                    
                    // Color dinámico (verde → amarillo → rojo)
                    BarColor color;
                    if (prog < 0.33) {
                        color = BarColor.GREEN;
                    } else if (prog < 0.66) {
                        color = BarColor.YELLOW;
                    } else {
                        color = BarColor.RED;
                    }
                    bossBar.setColor(color);
                    
                    String disasterId = stateManager.getString("desastre_actual", "DESASTRE");
                    String title = "§a" + disasterId.toUpperCase().replace("_", " ");
                    bossBar.setTitle(title + " §7• " + formatMMSS(rem));
                    bossBar.setVisible(true);
                }
                
                // Sincronizar Scoreboard y Tablist
                plugin.getScoreboardManager().updateAll();
                plugin.getTablistManager().updateAll();
                
                // Fin automático cuando rem <= 0
                if (rem <= 0) {
                    onDisasterEnd();
                }
                
            } else if ("PREPARACION".equals(estado)) {
                // PREPARACION: ocultar BossBar, Scoreboard muestra countdown o "----"
                if (bossBar != null) {
                    bossBar.setVisible(false);
                }
                
                // Sincronizar Scoreboard y Tablist (mostrará countdown si hay end_epoch_ms configurado)
                plugin.getScoreboardManager().updateAll();
                plugin.getTablistManager().updateAll();
                
            } else {
                // DETENIDO o SAFE_MODE sin desastre: ocultar BossBar, Scoreboard "----"
                if (bossBar != null) {
                    bossBar.setVisible(false);
                }
                plugin.getScoreboardManager().updateAll();
                plugin.getTablistManager().updateAll();
            }
            
        }, 0L, 10L); // Cada 10 ticks = 0.5s
    }
    
    /**
     * Fin del desastre → PREPARACION + cooldown + auto-next
     * ESCRIBE en state.yml: estado=PREPARACION, last_end_epoch_ms, limpia start/end/desastre_actual
     */
    private void onDisasterEnd() {
        stopCurrentDisasterTasks();
        cancelUITicker();
        
        String disasterId = activeDisaster != null ? activeDisaster.getId() : "unknown";
        
        if (bossBar != null) {
            bossBar.setProgress(1.0);
            bossBar.setVisible(false);
        }
        
        // Mensajes
        messageBus.broadcast("§a§l¡DESASTRE FINALIZADO! §f" + disasterId.toUpperCase().replace("_", " "), "disaster_end");
        soundUtil.playSoundAll(Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        soundUtil.playSoundAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 2.0f);
        
    // Al terminar un desastre → PREPARACION (no forzada) con cooldown visible
    long now = System.currentTimeMillis();
    int cooldownSeg = plugin.getConfigManager().getCooldownFinSegundos();
    long startMs = now;
    long endMs = now + cooldownSeg * 1000L;
    stateManager.setEstado("PREPARACION");
    stateManager.setString("desastre_actual", "");
    stateManager.setLastEndEpochMs(now);         // ✅ Usa setter que actualiza memoria + YAML
    stateManager.setPrepForzada(false);          // fin de desastre NO es preparación forzada
    stateManager.setLong("start_epoch_ms", startMs);
    stateManager.setLong("end_epoch_ms", endMs);
    stateManager.setLastDisasterId(disasterId);  // ultimo_desastre
    stateManager.saveState();
        
    // [ANTIRREBOTE] Marcar entrada en PREPARACION para evitar inicio en mismo tick
    enteredPreparationAtMs = now;
        
    // Mostrar mensaje de cooldown visible
    messageBus.broadcast("§e⏳ Cooldown: próximo desastre en §f" + cooldownSeg + "§es.", "cooldown_start");
        
        // Limpiar instancia activa
        activeDisaster = null;
        stateManager.setActiveDisasterId(null);
        
        // Reset flag de auto-inicio para permitir siguiente ciclo automático tras cooldown
        cooldownAutoStartAttempted.set(false);
        
        timeService.end();
        
        logOnce(0, "FIN: " + disasterId.toUpperCase() + " → PREPARACION (cooldown)");
        
        // Programar auto-next si está habilitado
        if (plugin.getConfigManager().isAutoCycleEnabled()) {
            scheduleAutoNext();
            logOnce(0, "AUTO-NEXT programado tras cooldown (" + cooldownSeg + "s)");
        }
    }
    
    /**
     * Detener tareas del desastre actual
     */
    private void stopCurrentDisasterTasks() {
        if (activeDisaster != null && activeDisaster.isActive()) {
            activeDisaster.stop();
        }
    }
    
    /**
     * Agregar jugador al BossBar (llamar en PlayerJoinEvent)
     */
    public void addPlayerToBossBar(Player player) {
        if (bossBar != null) {
            bossBar.addPlayer(player);
        }
    }
    
    /**
     * Remover jugador del BossBar (llamar en PlayerQuitEvent)
     */
    public void removePlayerFromBossBar(Player player) {
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }
    
    /**
     * Reanejar BossBar a todos los jugadores (usado en /avo reload)
     */
    public void reattachBossBarAll() {
        if (bossBar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                bossBar.addPlayer(p);
            }
        }
    }
}
