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
import org.bukkit.command.CommandSender;
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


    // [GUARD] Prevenir ejecuciones concurrentes de onDisasterEnd
    private final AtomicBoolean endingDisaster = new AtomicBoolean(false);
    
    // [ANTIRREBOTE] Timestamp cuando se entra en PREPARACION (evita inicio en mismo tick)
    private long enteredPreparationAtMs = 0L;
    
    // [RATE-LIMIT] Logs antispam (1/segundo por tipo)
    private long lastCooldownBlockLog = 0L;
    private long lastMinPlayersBlockLog = 0L;
    private long lastCooldownReadyLog = 0L;
    
    // [COUNTDOWN ALERTS] Rastreo de alertas enviadas
    private boolean alert60sIssued = false;
    private boolean alert30sIssued = false;
    private boolean alert10sIssued = false;
    private boolean alert5sIssued = false;
    private boolean alert4sIssued = false;
    private boolean alert3sIssued = false;
    private boolean alert2sIssued = false;
    private boolean alert1sIssued = false;
    
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
        // [FIX DUPLICACIÓN CRÍTICO] Cancelar tarea anterior ANTES de crear una nueva
        // Esto previene la acumulación de múltiples runnables ejecutándose en paralelo
        if (taskId != -1) {
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().warning("[CRÍTICO] startTask() llamado con tarea activa (id=" + taskId + ") - cancelando primero");
            }
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L).getTaskId();
        
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[DisasterController] Task iniciada con ID=" + taskId);
        }
    }

    public void cancelTask() {
        if (taskId != -1) {
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[DisasterController] Cancelando task ID=" + taskId);
            }
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
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[Cycle][DEBUG] cancelAllTasks: cancelando nextTask (auto-next)");
            }
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
     * Reset del flag de auto-inicio para permitir nuevo intento manual
     */
    public void resetCooldownAutoStartFlag() {
        cooldownAutoStartAttempted.set(false);
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[Cycle] Flag cooldownAutoStart reseteado (manual)");
        }
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
        // [FIX] Llamar directamente a onDisasterEnd() en lugar de stopDisaster()
        plugin.getLogger().info("[Cycle] onTimeFinished() detectó fin de ACTIVO → onDisasterEnd()");
        onDisasterEnd();
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
    /**
 * Fin de preparación forzada → auto-start si auto_cycle=true
 */
    private void endPreparation() {
        messageBus.broadcast("§e☁ §fFin de la preparación. ¡El caos regresa!", "prep_end");
        soundUtil.playSoundAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.8f);

        // Limpiar timestamps y flag de preparación forzada
        stateManager.setLong("start_epoch_ms", 0L);
        stateManager.setLong("end_epoch_ms", 0L);
        stateManager.setPrepForzada(false); // ← CRÍTICO: limpiar flag
        stateManager.saveState();
        
        // Si auto_cycle=true, intentar inicio automático (ignora cooldown)
        if (plugin.getConfigManager().isAutoCycleEnabled()) {
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[Cycle] Fin de preparación forzada → auto-start (ignora cooldown)");
            }
            tryStartRandomDisaster("prep_forzada_end");
        } else {
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[Cycle] Fin de preparación forzada, pero auto_cycle=false → DETENIDO");
            }
            stateManager.setEstado("DETENIDO");
            stateManager.saveState();
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
        
        // [EVASION] Registrar inicio del desastre para todos los jugadores online
        for (org.bukkit.entity.Player p : plugin.getServer().getOnlinePlayers()) {
            if (!p.hasPermission("apocalipsis.exempt")) {
                plugin.getDisasterEvasionTracker().onDisasterStart(p);
            }
        }
        
        // [RE-FIX] Iniciar UI Ticker al reanudar tras boot
        startUITicker();

        plugin.getLogger().info("Desastre reanudado: " + disasterId);
    }

    /**
     * Fin del desastre → PREPARACION + cooldown + auto-next
     * ESCRIBE en state.yml: estado=PREPARACION, last_end_epoch_ms, limpia start/end/desastre_actual
     */
    /**
 * Fin del desastre → PREPARACION + cooldown + auto-next
 * ESCRIBE en state.yml: estado=PREPARACION, last_end_epoch_ms, limpia start/end/desastre_actual
 */
    private void onDisasterEnd() {
        plugin.getLogger().info("════════════════════════════════════════");
        plugin.getLogger().info("[Cycle] >>> onDisasterEnd() INICIADO <<<");
        plugin.getLogger().info("════════════════════════════════════════");
        
        // [GUARD] Prevenir ejecuciones concurrentes
        if (!endingDisaster.compareAndSet(false, true)) {
            plugin.getLogger().warning("[Cycle] onDisasterEnd bloqueado (ya ejecutándose)");
            return;
        }
        
        try {
            // Verificar que hay desastre
            if (activeDisaster == null) {
                plugin.getLogger().warning("[Cycle] onDisasterEnd sin activeDisaster - RESETEANDO FLAGS");
                cooldownAutoStartAttempted.set(false);
                starting.set(false);
                return;
            }
            
            final String disasterId = activeDisaster.getId();
            plugin.getLogger().info("[Cycle] Finalizando desastre: " + disasterId);
            
            stopCurrentDisasterTasks();
            
            // [CRÍTICO] Cancelar nextTask
            if (nextTask != null && !nextTask.isCancelled()) {
                nextTask.cancel();
                nextTask = null;
                plugin.getLogger().info("[Cycle] ✓ nextTask cancelado");
            }
            
            cancelUITicker();

            if (bossBar != null) {
                bossBar.setProgress(1.0);
                bossBar.setVisible(false);
            }
            
            // Mensajes
            messageBus.broadcast("§a§l¡DESASTRE FINALIZADO! §f" + disasterId.toUpperCase().replace("_", " "), "disaster_end");
            soundUtil.playSoundAll(Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            soundUtil.playSoundAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 2.0f);
            
            // PREPARACION con cooldown
            long now = System.currentTimeMillis();
            int cooldownSeg = plugin.getConfigManager().getCooldownFinSegundos();

            stateManager.setEstado("PREPARACION");
            stateManager.setString("desastre_actual", "");
            stateManager.setLastDisasterId(disasterId);
            stateManager.setLastEndEpochMs(now);
            stateManager.setLong("start_epoch_ms", now);
            stateManager.setLong("end_epoch_ms", now + cooldownSeg * 1000L);
            stateManager.setPrepForzada(false);
            enteredPreparationAtMs = now;
            stateManager.saveState();
            
            messageBus.broadcast("§e⏳ Cooldown: próximo desastre en §f" + cooldownSeg + "§es.", "cooldown_start");
            
            // Limpiar
            activeDisaster = null;
            stateManager.setActiveDisasterId(null);
            
            // [EVASION] Notificar fin del desastre (limpia registros)
            plugin.getDisasterEvasionTracker().onDisasterEnd();
            
            // [COUNTDOWN] Resetear flags de alertas
            resetCountdownFlags();
            
            // [CRÍTICO] RESETEAR FLAGS
            plugin.getLogger().info("[Cycle] ANTES: cooldown=" + cooldownAutoStartAttempted.get() + ", starting=" + starting.get());
            cooldownAutoStartAttempted.set(false);
            starting.set(false);
            plugin.getLogger().info("[Cycle] DESPUÉS: cooldown=" + cooldownAutoStartAttempted.get() + ", starting=" + starting.get());
            plugin.getLogger().info("[Cycle] ✓✓✓ FLAGS RESETEADOS ✓✓✓");
            
            timeService.end();
            
            plugin.getLogger().info("[Cycle] FIN: " + disasterId + " → PREPARACION (cooldown " + cooldownSeg + "s)");
            
            // Auto-next
            if (plugin.getConfigManager().isAutoCycleEnabled()) {
                plugin.getLogger().info("[Cycle] ✓ Llamando scheduleAutoNext()...");
                scheduleAutoNext();
            } else {
                plugin.getLogger().info("[Cycle] auto_cycle=false");
            }
            
            plugin.getLogger().info("════════════════════════════════════════");
            plugin.getLogger().info("[Cycle] >>> onDisasterEnd() COMPLETADO <<<");
            plugin.getLogger().info("════════════════════════════════════════");
            
        } catch (Exception e) {
            plugin.getLogger().severe("[Cycle] ERROR en onDisasterEnd: " + e.getMessage());
            e.printStackTrace();
            cooldownAutoStartAttempted.set(false);
            starting.set(false);
        } finally {
            endingDisaster.set(false);
        }
    }



    /**
     * Detiene todos los desastres activos
     * @param announce Si true, anuncia la detención
     * @param changeState Si true, cambia el estado a DETENIDO (false para switches rápidos)
     */
    public void stopAllDisasters(boolean announce, boolean changeState) {
        endingDisaster.set(false);
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
        
        // [FIX] Resetear flags de auto-inicio cuando se detiene manualmente
        cooldownAutoStartAttempted.set(false);
        starting.set(false);
        
        // [FIX] Cancelar tareas programadas (cooldown, auto-next) pero NO el task principal de tick
        if (cooldownTaskId != -1) {
            Bukkit.getScheduler().cancelTask(cooldownTaskId);
            cooldownTaskId = -1;
        }
        
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[Cycle] STOP: desastre detenido, flags reseteados, tareas auxiliares canceladas (changeState=" + changeState + ")");
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
    
    /**
     * Verifica si hay un desastre activo actualmente
     * @return true si hay un desastre ejecutándose, false en caso contrario
     */
    public boolean hasActiveDisaster() {
        return activeDisaster != null && activeDisaster.isActive();
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
    /**
    * Scheduler de auto-next (1 vez/seg, único)
    * Solo inicia si: estado==PREPARACION, auto_cycle==true, online>=min_jugadores, cooldown cumplido
    */
    public void scheduleAutoNext() {
        if (nextTask != null && !nextTask.isCancelled()) {
            nextTask.cancel();
        }
        
        // [FIX] Resetear flag al programar nuevo scheduler (permite reintentos frescos)
        cooldownAutoStartAttempted.set(false);
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[Cycle] scheduleAutoNext(): nuevo scheduler, flag reseteado");
        }

        nextTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();

            // Antirebote: tras entrar a PREPARACION
            if (now - enteredPreparationAtMs < 250L) {
                if (plugin.getConfigManager().isDebugCiclo()) {
                    plugin.getLogger().info("[Cycle] Antirebote activo (< 250ms desde entrada a PREPARACION)");
                }
                return;
            }

            // Fuente única: state.yml
            final String estado = stateManager.getEstado();
            final boolean isPrep = "PREPARACION".equals(estado);
            final boolean prepForzada = stateManager.isPrepForzada();

            // ⛔ JAMÁS auto-iniciar en DETENIDO / ACTIVO / SAFE_MODE
            if (!isPrep) {
                if (plugin.getConfigManager().isDebugCiclo() && now % 5000 < 1000) {
                    plugin.getLogger().info("[Cycle] No es PREPARACION: estado=" + estado);
                }
                return;
            }

            // ─────────────────────────────────────────────
            // 1) PREPARACION FORZADA: sólo iniciar al vencer end_epoch_ms
            // ─────────────────────────────────────────────
            if (prepForzada) {
                long endEpochMs = stateManager.getLong("end_epoch_ms", 0L);
                if (endEpochMs > 0 && now >= endEpochMs) {
                    int minJug = plugin.getConfigManager().getMinJugadores();
                    int online = Bukkit.getOnlinePlayers().size();
                    if (online >= minJug) {
                        plugin.getLogger().info("[Cycle] Fin de PREPARACION forzada → iniciando");
                        stateManager.setPrepForzada(false);
                        stateManager.saveState();
                        tryStartRandomDisaster("prep_forzada_end"); // ignora cooldown
                    } else if (plugin.getConfigManager().isDebugCiclo()) {
                        plugin.getLogger().info("[Cycle] Esperando jugadores mínimos: " + online + "/" + minJug);
                    }
                } else {
                    // [COUNTDOWN ALERTS] Durante preparación forzada, enviar alertas basadas en endEpochMs
                    long remainingMs = endEpochMs - now;
                    long remainingSec = remainingMs / 1000L;
                    
                    if (plugin.getConfigManager().isDebugCiclo() && now % 2000 < 1000) {
                        plugin.getLogger().info("[Countdown] PrepForzada: faltan " + remainingSec + "s (60=" + alert60sIssued + ", 30=" + alert30sIssued + ", 10=" + alert10sIssued + ")");
                    }
                    
                    if (remainingMs > 0) {
                        // Alerta de 60 segundos
                        if (remainingSec <= 60 && remainingSec > 55 && !alert60sIssued) {
                            sendCountdownAlert(60);
                            alert60sIssued = true;
                            plugin.getLogger().info("[Countdown] ✓ Enviada alerta 60s (PrepForzada)");
                        }
                        // Alerta de 30 segundos
                        else if (remainingSec <= 30 && remainingSec > 25 && !alert30sIssued) {
                            sendCountdownAlert(30);
                            alert30sIssued = true;
                            plugin.getLogger().info("[Countdown] ✓ Enviada alerta 30s (PrepForzada)");
                        }
                        // Alerta de 10 segundos
                        else if (remainingSec <= 10 && remainingSec > 8 && !alert10sIssued) {
                            sendCountdownAlert(10);
                            alert10sIssued = true;
                            plugin.getLogger().info("[Countdown] ✓ Enviada alerta 10s (PrepForzada)");
                        }
                        // Alertas finales (5, 4, 3, 2, 1)
                        else if (remainingSec == 5 && !alert5sIssued) {
                            sendCountdownAlert(5);
                            alert5sIssued = true;
                            plugin.getLogger().info("[Countdown] ✓ Enviada alerta 5s (PrepForzada)");
                        }
                        else if (remainingSec == 4 && !alert4sIssued) {
                            sendCountdownAlert(4);
                            alert4sIssued = true;
                            plugin.getLogger().info("[Countdown] ✓ Enviada alerta 4s (PrepForzada)");
                        }
                        else if (remainingSec == 3 && !alert3sIssued) {
                            sendCountdownAlert(3);
                            alert3sIssued = true;
                            plugin.getLogger().info("[Countdown] ✓ Enviada alerta 3s (PrepForzada)");
                        }
                        else if (remainingSec == 2 && !alert2sIssued) {
                            sendCountdownAlert(2);
                            alert2sIssued = true;
                            plugin.getLogger().info("[Countdown] ✓ Enviada alerta 2s (PrepForzada)");
                        }
                        else if (remainingSec == 1 && !alert1sIssued) {
                            sendCountdownAlert(1);
                            alert1sIssued = true;
                            plugin.getLogger().info("[Countdown] ✓ Enviada alerta 1s (PrepForzada)");
                        }
                    }
                    
                    if (plugin.getConfigManager().isDebugCiclo() && now % 5000 < 1000) {
                        plugin.getLogger().info("[Cycle] PrepForzada activa, faltan " + remainingSec + "s");
                    }
                }
                return; // mientras sea forzada, no mirar cooldown
            }

            // ─────────────────────────────────────────────
            // 2) PREPARACION NORMAL (cooldown)
            // ─────────────────────────────────────────────
            if (!plugin.getConfigManager().isAutoCycleEnabled()) {
                if (plugin.getConfigManager().isDebugCiclo() && now % 5000 < 1000) {
                    plugin.getLogger().info("[Cycle] auto_cycle=false, esperando comando manual");
                }
                return;
            }

            int minJug = plugin.getConfigManager().getMinJugadores();
            int online = Bukkit.getOnlinePlayers().size();
            if (online < minJug) {
                if (plugin.getConfigManager().isDebugCiclo() && now % 5000 < 1000) {
                    plugin.getLogger().info("[Cycle] Insuficientes jugadores: " + online + "/" + minJug);
                }
                return;
            }

            long lastEndMs = stateManager.getLastEndEpochMs();
            if (lastEndMs <= 0L) {
                if (plugin.getConfigManager().isDebugCiclo() && now % 5000 < 1000) {
                    plugin.getLogger().info("[Cycle] Sin desastre previo (lastEndMs=0)");
                }
                return; // aún no hubo desastre previo → no hay cooldown que cumplir
            }

            long cooldownMs = plugin.getConfigManager().getCooldownFinSegundos() * 1000L;
            long elapsed = now - lastEndMs;
            long remainingMs = cooldownMs - elapsed;
            long remainingSec = remainingMs / 1000L;

            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[Cycle] Cooldown check: elapsed=" + (elapsed / 1000) + "s / req=" + (cooldownMs / 1000) + "s, remaining=" + remainingSec + "s");
            }
            
            // [COUNTDOWN ALERTS] Enviar alertas según tiempo restante
            if (remainingMs > 0) {
                if (plugin.getConfigManager().isDebugCiclo() && now % 2000 < 1000) {
                    plugin.getLogger().info("[Countdown] Cooldown: faltan " + remainingSec + "s (60=" + alert60sIssued + ", 30=" + alert30sIssued + ", 10=" + alert10sIssued + ")");
                }
                
                // Alerta de 60 segundos
                if (remainingSec <= 60 && remainingSec > 55 && !alert60sIssued) {
                    sendCountdownAlert(60);
                    alert60sIssued = true;
                    plugin.getLogger().info("[Countdown] ✓ Enviada alerta 60s (Cooldown)");
                }
                // Alerta de 30 segundos
                else if (remainingSec <= 30 && remainingSec > 25 && !alert30sIssued) {
                    sendCountdownAlert(30);
                    alert30sIssued = true;
                    plugin.getLogger().info("[Countdown] ✓ Enviada alerta 30s (Cooldown)");
                }
                // Alerta de 10 segundos
                else if (remainingSec <= 10 && remainingSec > 8 && !alert10sIssued) {
                    sendCountdownAlert(10);
                    alert10sIssued = true;
                    plugin.getLogger().info("[Countdown] ✓ Enviada alerta 10s (Cooldown)");
                }
                // Alertas finales (5, 4, 3, 2, 1)
                else if (remainingSec == 5 && !alert5sIssued) {
                    sendCountdownAlert(5);
                    alert5sIssued = true;
                    plugin.getLogger().info("[Countdown] ✓ Enviada alerta 5s (Cooldown)");
                }
                else if (remainingSec == 4 && !alert4sIssued) {
                    sendCountdownAlert(4);
                    alert4sIssued = true;
                    plugin.getLogger().info("[Countdown] ✓ Enviada alerta 4s (Cooldown)");
                }
                else if (remainingSec == 3 && !alert3sIssued) {
                    sendCountdownAlert(3);
                    alert3sIssued = true;
                    plugin.getLogger().info("[Countdown] ✓ Enviada alerta 3s (Cooldown)");
                }
                else if (remainingSec == 2 && !alert2sIssued) {
                    sendCountdownAlert(2);
                    alert2sIssued = true;
                    plugin.getLogger().info("[Countdown] ✓ Enviada alerta 2s (Cooldown)");
                }
                else if (remainingSec == 1 && !alert1sIssued) {
                    sendCountdownAlert(1);
                    alert1sIssued = true;
                    plugin.getLogger().info("[Countdown] ✓ Enviada alerta 1s (Cooldown)");
                }
            }
            
            if (elapsed < cooldownMs) {
                if (plugin.getConfigManager().isDebugCiclo() && now % 5000 < 1000) {
                    plugin.getLogger().info("[Cycle] Cooldown en progreso: " + (elapsed / 1000) + "/" + (cooldownMs / 1000) + "s");
                }
                return;
            }
            
            // [COUNTDOWN] Resetear flags al iniciar desastre
            resetCountdownFlags();

            // ✅ Cooldown cumplido → intentar iniciar
            boolean flagBloqueado = cooldownAutoStartAttempted.get();
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[Cycle] Cooldown CUMPLIDO. Flag bloqueado=" + flagBloqueado);
            }
            
            if (!flagBloqueado) {
                if (plugin.getConfigManager().isDebugCiclo()) {
                    plugin.getLogger().info("[Cycle] Llamando tryStartRandomDisaster('cooldown')...");
                }
                
                tryStartRandomDisaster("cooldown");

                // Verifica si realmente comenzó
                String estadoDespues = stateManager.getEstado();
                if ("ACTIVO".equals(estadoDespues)) {
                    cooldownAutoStartAttempted.set(true);
                    if (plugin.getConfigManager().isDebugCiclo()) {
                        plugin.getLogger().info("[Cycle] ✅ Inicio por cooldown CONFIRMADO (estado=ACTIVO)");
                    }
                } else {
                    // No arrancó: mantener flag en false para reintentar
                    cooldownAutoStartAttempted.set(false);
                    if (plugin.getConfigManager().isDebugCiclo()) {
                        plugin.getLogger().info("[Cycle] ⚠ Intento fallido (estado=" + estadoDespues + "), reintentará el próximo tick");
                    }
                }
            } else if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[Cycle] Bloqueado por flag cooldownAutoStartAttempted=true (ya intentó iniciar)");
            }
        }, 20L, 20L); // Cada 1s
    }

    
    /**
     * Puerta única de inicio con guardias correctas según especificaciones
     */
    /**
 * Puerta única de inicio con guardias correctas según especificaciones
 */
    public void tryStartRandomDisaster(String reason) {
        long now = System.currentTimeMillis();
        
        // ═══════════════════════════════════════════════════════════════
        // 0) VERIFICAR SAFE MODE (bloquea SIEMPRE)
        // ═══════════════════════════════════════════════════════════════
        if (stateManager.isSafeModeActive()) {
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[Cycle] BLOQUEADO por SAFE_MODE activo");
            }
            return;
        }
        
        // Leer estado desde state.yml (fuente única)
        String estado = stateManager.getEstado();
        
        // ═══════════════════════════════════════════════════════════════
        // 1) VERIFICAR COOLDOWN PRIMERO (antes de cualquier otra lógica)
        // ═══════════════════════════════════════════════════════════════
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
                return; // ❌ COOLDOWN NO CUMPLIDO
            }
            
            // Cooldown cumplido: log solo una vez
            if (now - lastCooldownReadyLog >= 1000L) {
                lastCooldownReadyLog = now;
                if (plugin.getConfigManager().isDebugCiclo()) {
                    plugin.getLogger().info("[Cycle] Cooldown cumplido → intentando iniciar");
                }
            }
        }
        
        // ═══════════════════════════════════════════════════════════════
        // 2) ESTADO VÁLIDO
        // ═══════════════════════════════════════════════════════════════
        boolean estadoValido = false;
        
        if ("PREPARACION".equals(estado)) {
            // PREPARACION siempre permite inicio (cooldown ya fue verificado arriba)
            estadoValido = true;
        } else if ("DETENIDO".equals(estado) && ("command".equals(reason) || "prep_forzada_end".equals(reason))) {
            // DETENIDO solo permite comando manual o fin de prep forzada
            estadoValido = true;
        }
        
        if (!estadoValido) {
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[Cycle] BLOQUEADO: estado=" + estado + " reason=" + reason);
            }
            return;
        }
        
        // ═══════════════════════════════════════════════════════════════
        // 3) BLOQUEO POR PREPARACIÓN FORZADA (ventana activa)
        // ═══════════════════════════════════════════════════════════════
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
                return; // ❌ Nunca iniciar dentro de la ventana forzada
            }
        }
        
        // ═══════════════════════════════════════════════════════════════
        // 4) MIN JUGADORES
        // ═══════════════════════════════════════════════════════════════
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
        
        // ═══════════════════════════════════════════════════════════════
        // 5) ANTI-RACE
        // ═══════════════════════════════════════════════════════════════
        if (!starting.compareAndSet(false, true)) {
            logOnce(1000, "[CICLO] Inicio concurrente bloqueado");
            return;
        }
        
        try {
            // ═══════════════════════════════════════════════════════════════
            // 6) DETENER DESASTRE ACTIVO SI EXISTE
            // ═══════════════════════════════════════════════════════════════
            if (activeDisaster != null && activeDisaster.isActive()) {
                if (plugin.getConfigManager().isDebugCiclo()) {
                    plugin.getLogger().info("[Cycle] Deteniendo desastre activo antes de iniciar nuevo");
                }
                stopAllDisasters(false, false);
            }
            
            // ═══════════════════════════════════════════════════════════════
            // 7) ELEGIR Y LANZAR DESASTRE
            // ═══════════════════════════════════════════════════════════════
            String disasterId = elegirSegunWeight();
            if (disasterId == null) {
                logOnce(5000, "[CICLO] No se pudo elegir desastre (weights inválidos)");
                return;
            }
            
            if (!registry.exists(disasterId)) {
                plugin.getLogger().warning("[CICLO] Desastre no existe en registry: " + disasterId);
                return;
            }
            
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[Cycle] ✅ INICIANDO desastre: " + disasterId + " (reason=" + reason + ")");
            }
            
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
        // [FIX] Asegurar estado ACTIVO antes de iniciar el desastre
        stateManager.setEstado(ServerState.ACTIVO.name());
        // [FIX] Iniciar ciclo de ticks para el desastre
        startTask();
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[Cycle][DEBUG] Estado cambiado a ACTIVO y startTask llamado tras iniciar desastre: " + disasterId);
        }
        
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
        
        // [EVASION] Registrar inicio del desastre para todos los jugadores online
        for (org.bukkit.entity.Player p : plugin.getServer().getOnlinePlayers()) {
            if (!p.hasPermission("apocalipsis.exempt")) {
                plugin.getDisasterEvasionTracker().onDisasterStart(p);
            }
        }
        
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[Cycle][DEBUG] startTask llamado tras iniciar desastre: " + disasterId);
        }
        
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
    /**
     * Fin del desastre → PREPARACION + cooldown + auto-next
     * ESCRIBE en state.yml: estado=PREPARACION, last_end_epoch_ms, limpia start/end/desastre_actual
     */
/**
 * Fin del desastre → PREPARACION + cooldown + auto-next
 * ESCRIBE en state.yml: estado=PREPARACION, last_end_epoch_ms, limpia start/end/desastre_actual
 */
    
                
       
    
    /**
     * Detener tareas del desastre actual
     * [FIX DUPLICACIÓN] También cancelar el tick principal del controller
     */
    private void stopCurrentDisasterTasks() {
        if (activeDisaster != null && activeDisaster.isActive()) {
            if (plugin.getConfigManager().isDebugCiclo()) {
                plugin.getLogger().info("[DisasterController] Deteniendo desastre activo: " + activeDisaster.getId());
            }
            activeDisaster.stop();
        }
        
        // [FIX CRÍTICO] Cancelar task principal para evitar acumulación
        // Esto asegura que no haya múltiples runnables ejecutándose
        cancelTask();
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
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA DE ALERTAS DE CUENTA REGRESIVA
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Envía alertas visuales y de sonido cuando falta tiempo para el inicio del desastre
     * @param seconds Segundos restantes
     */
    private void sendCountdownAlert(int seconds) {
        // Determinar color y estilo según tiempo restante
        String color;
        String prefix;
        float pitch;
        Sound sound;
        
        if (seconds >= 30) {
            color = "§e"; // Amarillo
            prefix = "§e§l⏰";
            pitch = 1.0f;
            sound = Sound.BLOCK_NOTE_BLOCK_PLING;
        } else if (seconds >= 10) {
            color = "§6"; // Naranja
            prefix = "§6§l⚠";
            pitch = 1.3f;
            sound = Sound.BLOCK_NOTE_BLOCK_BELL;
        } else if (seconds >= 5) {
            color = "§c"; // Rojo
            prefix = "§c§l⚠";
            pitch = 1.5f;
            sound = Sound.BLOCK_NOTE_BLOCK_BELL;
        } else {
            color = "§4§l"; // Rojo oscuro + negrita
            prefix = "§4§l⚠⚠⚠";
            pitch = 1.8f + (5 - seconds) * 0.1f; // Aumenta el pitch progresivamente
            sound = Sound.ENTITY_ENDER_DRAGON_GROWL;
        }
        
        // Mensaje principal con formato mejorado
        String message;
        String separator = "§8§m━━━━━━━━━━━━━━━━━━━━━━━━━";
        
        if (seconds >= 5) {
            message = "\n" + separator + "\n" + 
                     prefix + " " + color + "§l¡DESASTRE EN " + seconds + " SEGUNDOS!" + "\n" +
                     "§7Prepárate para sobrevivir..." + "\n" +
                     separator;
        } else {
            message = prefix + " " + color + "§l" + seconds + "...";
        }
        
        // Contador de jugadores que recibirán la alerta
        int playerCount = 0;

        // Enviar a todos los jugadores (excepto los que están en la lista de excepciones de /avo admin)
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Verificar si el jugador está en la lista de excepciones (desastres.yml -> excepciones.players)
            if (plugin.getConfigManager().getExcepciones().contains(player.getUniqueId())) {
                continue;
            }

            playerCount++;

            // Línea corta y visible (garantía): ejemplo "§e¡DESASTRE EN 60s!"
            String shortLine = prefix + " " + color + "§l¡DESASTRE EN " + seconds + " SEGUNDOS!";

            // Enviar mensaje principal via MessageBus (Adventure) con fallback
            try {
                messageBus.sendMessage(player, shortLine);
                // SIEMPRE log en debug para verificar que se ejecuta
                if (plugin.getConfigManager().isDebugCiclo()) {
                    plugin.getLogger().info("[Countdown] ✓ Chat enviado via MessageBus a " + player.getName() + ": " + shortLine);
                }
            } catch (Exception e) {
                // Log el error específico
                plugin.getLogger().warning("[Countdown] MessageBus.sendMessage falló para " + player.getName() + ": " + e.getMessage());
                // Fallback directo por si MessageBus falla
                try {
                    player.sendMessage(shortLine);
                    if (plugin.getConfigManager().isDebugCiclo()) {
                        plugin.getLogger().info("[Countdown] ✓ Chat enviado via fallback directo a " + player.getName() + ": " + shortLine);
                    }
                } catch (Exception ex) {
                    plugin.getLogger().severe("[Countdown] ERROR CRÍTICO - Ambos métodos de chat fallaron para " + player.getName() + ": " + ex.getMessage());
                }
            }

            // Enviar detalle multilínea (no obligatorio, pero útil)
            try {
                String[] lines = message.split("\\n");
                for (String line : lines) {
                    if (line == null || line.isEmpty()) continue;
                    try {
                        messageBus.sendMessage(player, line);
                    } catch (Exception e) {
                        // Intentar enviar como string
                        try { player.sendMessage(line); } catch (Exception ignore) {}
                    }
                }
            } catch (Exception e) {
                // Ignorar problemas con el detalle
            }

            // ActionBar adicional para cuentas regresivas cortas (≤10s)
            if (seconds <= 10) {
                try {
                    messageBus.sendActionBar(player, shortLine);
                    if (plugin.getConfigManager().isDebugCiclo()) {
                        plugin.getLogger().info("[Countdown] ✓ ActionBar enviado a " + player.getName());
                    }
                } catch (Throwable t) {
                    plugin.getLogger().warning("[Countdown] ActionBar falló para " + player.getName() + ": " + t.getMessage());
                }
            }

            // Sonido con volumen aumentado para mayor impacto (usar SoundUtil para respetar config)
            try {
                soundUtil.playSound(player, sound, 1.5f, pitch);
                if (plugin.getConfigManager().isDebugCiclo()) {
                    plugin.getLogger().info("[Countdown] ✓ Sonido enviado a " + player.getName() + ": " + sound.name());
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("[Countdown] playSound falló para " + player.getName() + ": " + t.getMessage());
            }

            // Efecto visual adicional para últimos 5 segundos
            if (seconds <= 5) {
                try {
                    // Título grande con colores intensos
                    String titleColor = seconds <= 3 ? "§4§l" : "§c§l";
                    messageBus.sendTitle(player, titleColor + String.valueOf(seconds), "§7§l¡PREPÁRATE!", 0, 20, 5);
                } catch (Throwable t) {
                    // Fallback: usar player.showTitle directamente si Adventure falla
                    try {
                        player.showTitle(net.kyori.adventure.title.Title.title(
                            net.kyori.adventure.text.Component.text((seconds <= 3 ? "§4§l" : "§c§l") + seconds),
                            net.kyori.adventure.text.Component.text("§7§l¡PREPÁRATE!"),
                            net.kyori.adventure.title.Title.Times.times(
                                java.time.Duration.ofMillis(0),
                                java.time.Duration.ofMillis(1000),
                                java.time.Duration.ofMillis(250)
                            )
                        ));
                    } catch (Throwable ignored) {
                        if (plugin.getConfigManager().isDebugCiclo()) {
                            plugin.getLogger().warning("[Countdown] showTitle fallback fallo para " + player.getName() + ": " + ignored.getMessage());
                        }
                    }
                }
            }
        }

        // Log detallado
        plugin.getLogger().info("[Countdown] ✓ Alerta " + seconds + "s enviada a " + playerCount + " jugadores");
    }
    
    /**
     * Resetea las flags de alertas (llamar al iniciar un desastre o al entrar en preparación)
     */
    public void resetCountdownFlags() {
        alert60sIssued = false;
        alert30sIssued = false;
        alert10sIssued = false;
        alert5sIssued = false;
        alert4sIssued = false;
        alert3sIssued = false;
        alert2sIssued = false;
        alert1sIssued = false;
        
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[Countdown] Flags de alertas reseteadas (60/30/10/5/4/3/2/1)");
        }
    }
    
    /**
     * Método de prueba para verificar entrega de notificaciones a un jugador específico
     */
    public void testCountdownAlert(Player targetPlayer, CommandSender sender) {
        plugin.getLogger().info("[TEST-ALERT] Iniciando prueba de alerta para " + targetPlayer.getName());
        
        // Verificar si está en la lista de excepciones de /avo admin
        if (plugin.getConfigManager().getExcepciones().contains(targetPlayer.getUniqueId())) {
            String msg = "[TEST-ALERT] El jugador " + targetPlayer.getName() + " está en la lista de excepciones (/avo admin) - BLOQUEADO";
            plugin.getLogger().warning(msg);
            sender.sendMessage("§c" + msg);
            return;
        }
        
        // Activar debug temporal para esta prueba
        boolean debugOriginal = plugin.getConfigManager().isDebugCiclo();
        if (!debugOriginal) {
            plugin.getConfigManager().setDebugCiclo(true);
            sender.sendMessage("§eDebug activado temporalmente para la prueba...");
        }
        
        // Probar diferentes tipos de alertas
        sender.sendMessage("§eProbando alerta de 60s...");
        sendCountdownAlert(60);
        
        // Pequeña pausa y probar alerta de 5s (con título)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            sender.sendMessage("§eProbando alerta de 5s (con título)...");
            sendCountdownAlert(5);
            
            // Restaurar debug original después de un momento
            if (!debugOriginal) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getConfigManager().setDebugCiclo(false);
                    sender.sendMessage("§eDebug restaurado. Revisa el log del servidor para detalles.");
                }, 40L); // 2 segundos después
            } else {
                sender.sendMessage("§eRevisa el log del servidor para detalles de la prueba.");
            }
        }, 20L); // 1 segundo después
        
        plugin.getLogger().info("[TEST-ALERT] Prueba iniciada para " + targetPlayer.getName());
    }

}
