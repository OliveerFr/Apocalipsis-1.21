package me.apocalipsis.commands;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.disaster.DisasterController;
import me.apocalipsis.events.EventController;
import me.apocalipsis.missions.MissionService;
import me.apocalipsis.missions.MissionType;
import me.apocalipsis.state.ServerState;
import me.apocalipsis.state.StateManager;
import me.apocalipsis.state.TimeService;
import me.apocalipsis.ui.MessageBus;

public class ApocalipsisCommand implements CommandExecutor {

    private final Apocalipsis plugin;
    private final StateManager stateManager;
    private final DisasterController disasterController;
    private final EventController eventController;
    private final MissionService missionService;
    private final MessageBus messageBus;

    public ApocalipsisCommand(Apocalipsis plugin, StateManager stateManager, DisasterController disasterController,
                             EventController eventController, MissionService missionService, TimeService timeService, MessageBus messageBus) {
        this.plugin = plugin;
        this.stateManager = stateManager;
        this.disasterController = disasterController;
        this.eventController = eventController;
        this.missionService = missionService;
        this.messageBus = messageBus;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§c§lAPOCALIPSIS §7- Comandos:");
            sender.sendMessage("§6=== Control de Desastres ===");
            sender.sendMessage("§e/avo start §7- Inicia o reanuda el desastre");
            sender.sendMessage("§e/avo stop §7- Detiene el desastre actual");
            sender.sendMessage("§e/avo force <id> §7- Fuerza un desastre específico");
            sender.sendMessage("§e/avo skip §7- Salta al siguiente estado");
            sender.sendMessage("§e/avo preparacion <min> §7- Inicia preparación");
            sender.sendMessage("§e/avo time <set|add> <min> §7- Modifica tiempo del estado");
            sender.sendMessage("§6=== Protecciones ===");
            sender.sendMessage("§e/avo escanear §7- Escanea protecciones cercanas");
            sender.sendMessage("§e/avo protecciones §7- Guía de protecciones");
            sender.sendMessage("§6=== Experiencia y Progresión ===");
            sender.sendMessage("§e/avo xp §7- Ver tu experiencia y progreso");
            sender.sendMessage("§e/avo nivel §7- Ver tu nivel actual");
            sender.sendMessage("§e/avo xp <get|add|set> §7- Gestión de XP (Admin)");
            sender.sendMessage("§6=== Evento Eco de Brasas ===");
            sender.sendMessage("§e/avo eco start §7- Inicia el evento");
            sender.sendMessage("§e/avo eco stop §7- Detiene el evento");
            sender.sendMessage("§e/avo eco fase <1|2|3> §7- Fuerza fase específica");
            sender.sendMessage("§e/avo eco next §7- Avanza a siguiente fase");
            sender.sendMessage("§e/avo eco info §7- Info detallada del evento");
            sender.sendMessage("§e/avo eco pulso <add|set> <valor> §7- Ajusta pulso");
            sender.sendMessage("§e/avo eco ancla <1-3> §7- Completa ancla");
            sender.sendMessage("§6=== Misiones ===");
            sender.sendMessage("§e/avo newday §7- Crea un nuevo día y asigna misiones");
            sender.sendMessage("§e/avo endday §7- Termina el día actual");
            sender.sendMessage("§e/avo status [jugador] §7- Muestra misiones activas");
            sender.sendMessage("§e/avo setxp <jugador> <xp> §7- Ajusta XP de un jugador");
            sender.sendMessage("§e/avo mission <give|complete|clear> §7- Gestión de misiones");
            sender.sendMessage("§6=== Sistema ===");
            sender.sendMessage("§e/avo tps §7- Ver TPS y rendimiento");
            sender.sendMessage("§e/avo stats §7- Estadísticas del servidor");
            sender.sendMessage("§e/avo cooldown §7- Estado del cooldown");
            sender.sendMessage("§e/avo backup §7- Backup manual de datos");
            sender.sendMessage("§e/avo reload §7- Recarga la configuración");
            sender.sendMessage("§e/avo test §7- Toggle modo test");
            sender.sendMessage("§e/avo debug <on|off|status|missions> §7- Control de logs");
            sender.sendMessage("§e/avo test-alert <jugador> §7- Prueba notificaciones");
            sender.sendMessage("§e/avo admin <add|remove|list> §7- Gestionar excepciones");
            return true;
        }

        String subCmd = args[0].toLowerCase();

        switch (subCmd) {
            case "start":
                cmdStart(sender);
                break;
            case "stop":
                cmdStop(sender);
                break;
            case "force":
                cmdForce(sender, args);
                break;
            case "skip":
                cmdSkip(sender);
                break;
            case "preparacion":
                cmdPreparacion(sender, args);
                break;
            case "time":
                cmdTime(sender, args);
                break;
            case "test":
                cmdTest(sender);
                break;
            case "test-alert":
                cmdTestAlert(sender, args);
                break;
            case "newday":
                cmdNewDay(sender);
                break;
            case "endday":
                cmdEndDay(sender);
                break;
            case "status":
                cmdStatus(sender, args);
                break;
            case "setxp":
            case "setps": // Backward compatibility
                cmdSetXp(sender, args);
                break;
            case "mission":
                cmdMission(sender, args);
                break;
            case "tps":
                cmdTps(sender);
                break;
            case "stats":
                cmdStats(sender);
                break;
            case "backup":
                cmdBackup(sender);
                break;
            case "cooldown":
                cmdCooldown(sender);
                break;
            case "debug":
                cmdDebug(sender, args);
                break;
            case "reload":
                cmdReload(sender);
                break;
            case "admin":
                cmdAdmin(sender, args);
                break;
            case "escanear":
                cmdEscanear(sender);
                break;
            case "protecciones":
                cmdProtecciones(sender);
                break;
            case "eco":
                cmdEco(sender, args);
                break;
            case "xp":
            case "experience":
                cmdXP(sender, args);
                break;
            case "nivel":
            case "level":
                cmdNivel(sender, args);
                break;
            default:
                sender.sendMessage("§cSubcomando desconocido. Usa /avo para ver ayuda.");
                break;
        }

        return true;
    }

    private void cmdStart(CommandSender sender) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        // Estado actual desde state.yml
        String estado = stateManager.getEstado();

        // Si ya hay un desastre en curso, no dupliques
        if ("ACTIVO".equalsIgnoreCase(estado)) {
            sender.sendMessage("§eYa hay un desastre activo, no puedes iniciar otro.");
            return;
        }

        // SAFE_MODE bloquea cualquier inicio
        if ("SAFE_MODE".equalsIgnoreCase(estado) || stateManager.isSafeModeActive()) {
            sender.sendMessage("§cNo se puede iniciar en SAFE_MODE (TPS bajo).");
            return;
        }

        // ================================
        // Arrancar el CICLO (no el desastre)
        // PREPARACION normal (no forzada) + cooldown “cumplido”
        // ================================
        long now = System.currentTimeMillis();
        long cooldownMs = plugin.getConfigManager().getCooldownFinSegundos() * 1000L;

        // Dejar todo listo para que el scheduler inicie el 1º desastre enseguida
        // Usamos preparación forzada con duración configurable (default 15 min)
        stateManager.setEstado("PREPARACION");
        stateManager.setString("desastre_actual", "");
        // Preparación forzada para que scheduleAutoNext() use end_epoch_ms y envíe alerts
        stateManager.setPrepForzada(true);
        stateManager.setLastEndEpochMs(now - cooldownMs - 1000L); // cooldown ya cumplido

        // Tiempos para UI y countdown - leer desde config
        int prepSeconds = plugin.getConfigManager().getPreparacionInicialSegundos();
        stateManager.setLong("start_epoch_ms", now);
        stateManager.setLong("end_epoch_ms", now + (prepSeconds * 1000L));

        stateManager.saveState();

        // Antirrebote + reinicio de puertas internas
        disasterController.resetStartingFlag();    // por si había un intento previo
        disasterController.resetCooldownAutoStartFlag();
        // Resetear flags de countdown para asegurar que las alertas se muestren
        disasterController.resetCountdownFlags();
        disasterController.markEnteredPreparation();
        // Programa el auto-next (el que realmente iniciará el desastre)
        disasterController.scheduleAutoNext();

        // Feedback - mostrar tiempo en formato legible
        int minutos = prepSeconds / 60;
        int segundos = prepSeconds % 60;
        String tiempoDisplay = minutos > 0 ? minutos + " min" : segundos + "s";
        sender.sendMessage(String.format("§a✅ Ciclo iniciado. El primer desastre comenzará en %s.", tiempoDisplay));
        plugin.getLogger().info(String.format("[Cycle] /avo start → PREPARACION forzada (%ds). Scheduler armado.", prepSeconds));
    }

    
    private void cmdStop(CommandSender sender) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        disasterController.stopAllDisasters(true);
        sender.sendMessage("§7Desastre detenido. Todas las tareas canceladas.");
        
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info(String.format("[Cycle] STOP ejecutado manualmente por %s", sender.getName()));
        }
    }

    private void cmdForce(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUso: /avo force <huracan|lluvia_fuego|terremoto>");
            return;
        }

        String disasterId = args[1].toLowerCase();
        if (!plugin.getDisasterRegistry().exists(disasterId)) {
            sender.sendMessage("§cDesastre no encontrado: §f" + disasterId);
            sender.sendMessage("§7Disponibles: §e" + String.join(", ", plugin.getDisasterRegistry().getIds()));
            return;
        }

        // [FIX] /avo force ignora auto_cycle, jugadores, cooldown (solo bloquea SAFE_MODE)
        if (stateManager.getCurrentState() == ServerState.SAFE_MODE) {
            sender.sendMessage("§cNo se puede forzar en SAFE_MODE (TPS bajo).");
            return;
        }

        disasterController.startDisaster(disasterId);
        sender.sendMessage("§a✓ Desastre forzado: §f" + disasterId);
        
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info(String.format("[Cycle] INICIO por /avo force: desastre=%s", disasterId));
        }
    }

    private void cmdPreparacion(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUso: /avo preparacion <minutos>");
            return;
        }

        try {
            int minutes = Integer.parseInt(args[1]);
            if (minutes <= 0) {
                sender.sendMessage("§cEl tiempo debe ser mayor a 0.");
                return;
            }

            // 1. Si hay desastre activo → pararlo (cancelar tasks), no iniciar nada
            String estadoActual = stateManager.getEstado();
            if ("ACTIVO".equals(estadoActual)) {
                disasterController.stopAllDisasters(true, true);
                plugin.getLogger().info("[Cycle] Desastre detenido por /avo preparacion");
            }

            // 2. Cancelar cualquier tryStart en curso
            disasterController.resetStartingFlag();

            // 3. Escribir en state.yml: preparación forzada
            long now = System.currentTimeMillis();
            long durationMs = plugin.getConfigManager().isTestMode() ? 5000L : (minutes * 60000L);
            
            stateManager.setEstado("PREPARACION");
            stateManager.setString("desastre_actual", "");
            stateManager.setPrepForzada(true);
            stateManager.setLong("start_epoch_ms", now);
            stateManager.setLong("end_epoch_ms", now + durationMs);
            stateManager.saveState();
            
            // 3.5. Resetear alertas de countdown para nueva preparación
            disasterController.resetCountdownFlags();
            
            // 3.6. Marcar entrada en PREPARACION para antirrebote
            disasterController.markEnteredPreparation();

            // 4. Ocultar BossBar, scoreboard muestra tiempo restante
            disasterController.hideBossBar();
            
            // 5. Asegurar que el scheduler auto-next esté activo
            disasterController.scheduleAutoNext();
            
            // Mostrar tiempo real según test mode
            String timeDisplay = plugin.getConfigManager().isTestMode() ? "5 segundos" : minutes + " minutos";
            sender.sendMessage("§e✓ Preparación forzada iniciada por §f" + timeDisplay + "§e.");
            plugin.getLogger().info(String.format("[Cycle] PREPARACION forzada %dm", minutes));
        } catch (NumberFormatException e) {
            sender.sendMessage("§cEl valor debe ser un número.");
        }
    }

    private void cmdTest(CommandSender sender) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        boolean currentMode = plugin.getConfigManager().isTestMode();
        plugin.getConfigManager().setTestMode(!currentMode);
        
        String status = !currentMode ? "§aACTIVADO" : "§cDESACTIVADO";
        sender.sendMessage("§e/avo test §7- Modo test: " + status);
        
        if (!currentMode) {
            sender.sendMessage("§7Ahora: 5s prep, 20s desastres, 3s cooldown, 1.5× densidad lluvia fuego.");
        }
    }

    private void cmdNewDay(CommandSender sender) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        stateManager.incrementDay();
        int day = stateManager.getCurrentDay();
        
        // [1.21.8] Resetear flags de celebración
        missionService.resetPlayerDailyCompleteFired();
        
        // [REMOVAL] Reseteos de EXPLORAR y ALTURA deshabilitados (tipos removidos)
        // missionService.resetExploreTrackers();
        // missionService.resetHeightCounters();
        
        // [FIX] assignMissionsForDay ahora limpia automáticamente las misiones anteriores
        missionService.assignMissionsForDay(day);
        
        int onlinePlayers = plugin.getServer().getOnlinePlayers().size();
        messageBus.broadcast("§e§l⌛ §fNuevo día iniciado: §e" + day, "newday");
        sender.sendMessage("§a✓ Día " + day + " iniciado. Misiones anteriores limpiadas y nuevas asignadas a " + onlinePlayers + " jugador(es).");
    }

    private void cmdEndDay(CommandSender sender) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        // [1.21.8] Resetear flags de celebración
        missionService.resetPlayerDailyCompleteFired();
        
        // [REMOVAL] Reseteos de EXPLORAR y ALTURA deshabilitados (tipos removidos)
        // missionService.resetExploreTrackers();
        missionService.resetHeightCounters();
        
        // Finalizar día (marca misiones como fallidas)
        missionService.endDay();
        
        messageBus.broadcast("§7⌛ §fDía finalizado. Misiones no completadas han sido marcadas como §cfallidas§f.", "endday");
        sender.sendMessage("§7Día finalizado. Usa §e/avo newday§7 para iniciar un nuevo día con misiones frescas.");
    }

    private void cmdCooldown(CommandSender sender) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }
        
        sender.sendMessage("§8§m                                          ");
        sender.sendMessage("§c§lCOOLDOWN STATUS");
        sender.sendMessage("");
        
        // Estado actual
        String estado = stateManager.getEstado();
        sender.sendMessage("§7Estado actual: §f" + estado);
        
        // Auto-cycle
        boolean autoCycle = plugin.getConfigManager().isAutoCycleEnabled();
        sender.sendMessage("§7Auto-cycle: " + (autoCycle ? "§a✓ Activado" : "§c✗ Desactivado"));
        
        // Cooldown configurado
        int cooldownSeg = plugin.getConfigManager().getCooldownFinSegundos();
        sender.sendMessage("§7Cooldown configurado: §f" + cooldownSeg + "s §8(" + (cooldownSeg / 60) + "m " + (cooldownSeg % 60) + "s)");
        
        // Último desastre
        String ultimoDesastre = stateManager.getLastDisasterId();
        if (ultimoDesastre != null && !ultimoDesastre.isEmpty()) {
            sender.sendMessage("§7Último desastre: §f" + ultimoDesastre.toUpperCase().replace("_", " "));
        } else {
            sender.sendMessage("§7Último desastre: §8Ninguno");
        }
        
        // Timestamp del último fin
        long lastEndMs = stateManager.getLastEndEpochMs();
        if (lastEndMs > 0) {
            long now = System.currentTimeMillis();
            long elapsed = (now - lastEndMs) / 1000L;
            long remaining = cooldownSeg - elapsed;
            
            sender.sendMessage("§7Tiempo desde último fin: §f" + elapsed + "s §8(" + (elapsed / 60) + "m " + (elapsed % 60) + "s)");
            
            if (remaining > 0) {
                sender.sendMessage("§7Cooldown restante: §e" + remaining + "s §8(" + (remaining / 60) + "m " + (remaining % 60) + "s)");
                sender.sendMessage("§7Estado: §e⏳ En espera");
            } else {
                sender.sendMessage("§7Cooldown restante: §a0s");
                sender.sendMessage("§7Estado: §a✓ Listo para iniciar");
                
                // Verificar bloqueos adicionales
                int minJugadores = plugin.getConfigManager().getMinJugadores();
                int online = org.bukkit.Bukkit.getOnlinePlayers().size();
                if (online < minJugadores) {
                    sender.sendMessage("  §c⚠ Bloqueado: " + online + "/" + minJugadores + " jugadores online");
                }
                
                boolean prepForzada = stateManager.isPrepForzada();
                if (prepForzada) {
                    sender.sendMessage("  §e⚠ En preparación forzada");
                }
            }
        } else {
            sender.sendMessage("§7Tiempo desde último fin: §8N/A");
            sender.sendMessage("§7Estado: §8Sin desastre previo");
        }
        
        sender.sendMessage("§8§m                                          ");
    }

    private void cmdStatus(CommandSender sender, String[] args) {
        Player target;

        if (args.length >= 2) {
            if (!sender.hasPermission("avo.admin")) {
                sender.sendMessage("§cNo tienes permisos para ver el estado de otros jugadores.");
                return;
            }
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cJugador no encontrado.");
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cDebes especificar un jugador.");
                return;
            }
            target = (Player) sender;
        }

        var assignments = missionService.getActiveAssignments(target);
        if (assignments.isEmpty()) {
            sender.sendMessage("§7" + target.getName() + " no tiene misiones activas.");
            return;
        }

        sender.sendMessage("§e§lMisiones de " + target.getName() + ":");
        for (var assignment : assignments) {
            // [REMOVAL] No mostrar tipos deshabilitados
            if (!assignment.getMission().getTipo().isEnabled()) {
                continue;
            }
            
            String status = assignment.isCompleted() ? "§a✓" : assignment.isFailed() ? "§c✗" : "§7○";
            sender.sendMessage(status + " §f" + assignment.getMission().getNombre() + 
                " §7(" + assignment.getProgress() + "/" + assignment.getMission().getCantidad() + ")");
        }
    }

    private void cmdDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§e=== DEBUG - APOCALIPSIS ===");
            sender.sendMessage("§7Comandos disponibles:");
            sender.sendMessage("§e/avo debug on §7- Activa logs de debug");
            sender.sendMessage("§e/avo debug off §7- Desactiva logs de debug");
            sender.sendMessage("§e/avo debug status §7- Estado actual");
            sender.sendMessage("§e/avo debug missions §7- Info de misiones");
            return;
        }

        String subArg = args[1].toLowerCase();
        
        if (subArg.equals("on") || subArg.equals("enable") || subArg.equals("true")) {
            plugin.getConfigManager().setDebugCiclo(true);
            sender.sendMessage("§a✓ Debug activado");
            sender.sendMessage("§7Los logs detallados ahora se mostrarán en consola");
            sender.sendMessage("§7Verás información sobre:");
            sender.sendMessage("§7  - Ciclo de desastres");
            sender.sendMessage("§7  - Instancias de desastres");
            sender.sendMessage("§7  - Ticks y estados");
            sender.sendMessage("§7  - Alertas de countdown");
            return;
        }
        
        if (subArg.equals("off") || subArg.equals("disable") || subArg.equals("false")) {
            plugin.getConfigManager().setDebugCiclo(false);
            sender.sendMessage("§c✗ Debug desactivado");
            sender.sendMessage("§7Solo se mostrarán logs importantes");
            return;
        }
        
        if (subArg.equals("status") || subArg.equals("state")) {
            boolean debugActivo = plugin.getConfigManager().isDebugCiclo();
            sender.sendMessage("§e=== ESTADO DEBUG ===");
            sender.sendMessage("§7Debug ciclo: " + (debugActivo ? "§a✓ ACTIVO" : "§c✗ INACTIVO"));
            sender.sendMessage("§7Estado actual: §e" + stateManager.getEstado());
            sender.sendMessage("§7Desastre activo: §e" + (stateManager.getActiveDisasterId() != null ? stateManager.getActiveDisasterId() : "Ninguno"));
            sender.sendMessage("§7Auto-cycle: " + (plugin.getConfigManager().isAutoCycleEnabled() ? "§a✓" : "§c✗"));
            sender.sendMessage("§7Cooldown: §e" + plugin.getConfigManager().getCooldownFinSegundos() + "s");
            return;
        }

        if (subArg.equals("missions")) {
            sender.sendMessage("§7=== DEBUG MISIONES ===");
            sender.sendMessage("§7Día actual: §e" + stateManager.getCurrentDay());
            sender.sendMessage("§7Jugadores con misiones: §e" + plugin.getServer().getOnlinePlayers().size());
            sender.sendMessage("§c[NOTA] Tipos EXPLORAR y ALTURA están deshabilitados");
            
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                var assignments = missionService.getActiveAssignments(player);
                sender.sendMessage("§e" + player.getName() + " §7tiene §f" + assignments.size() + " §7misiones.");
            }
            return;
        }
        
        if (subArg.equals("explore")) {
            // [REMOVAL] Debug explore deshabilitado (tipo removido)
            sender.sendMessage("§c[REMOVAL] El comando /avo debug explore está deshabilitado");
            sender.sendMessage("§7Las misiones tipo EXPLORAR y ALTURA han sido removidas");
            return;
        }
        
        sender.sendMessage("§cSubcomando desconocido. Usa §e/avo debug §cpara ver opciones.");
    }
    
    private void cmdReload(CommandSender sender) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        // Verificar estado ANTES de recargar
        String estadoAnterior = stateManager.getEstado();
        final String desastreActivo;
        if ("ACTIVO".equals(estadoAnterior)) {
            desastreActivo = stateManager.getActiveDisasterId();
            plugin.getLogger().info("⚠ Desastre activo detectado durante reload: " + desastreActivo + ", preservando tiempo restante...");
        } else {
            desastreActivo = null;
        }

        // Recargar TODAS las configuraciones
        plugin.getConfigManager().reload();
        
        // [FIX] Reiniciar PerformanceAdapter con nueva configuración
        plugin.getPerformanceAdapter().stopMonitoring();
        plugin.getPerformanceAdapter().startMonitoring();
        
        // Solo cancelar tareas auxiliares, NO el task principal si hay desastre activo
        if ("ACTIVO".equals(estadoAnterior)) {
            // Cancelar solo cooldown y next disaster tasks
            disasterController.cancelCooldownAndNextTasks();
        } else {
            // Cancelar TODAS las tareas si no hay desastre activo
            disasterController.cancelAllTasks();
            
            // Reiniciar scheduler de auto-next con nueva configuración
            if (plugin.getConfigManager().isAutoCycleEnabled()) {
                disasterController.scheduleAutoNext();
                sender.sendMessage("§7✓ Auto-cycle reiniciado con nueva configuración");
            }
        }
        
        // [FIX] Reiniciar ScoreboardManager y TablistManager con nueva configuración
        plugin.getScoreboardManager().cancelTask();
        plugin.getScoreboardManager().startTask();
        plugin.getTablistManager().cancelTask();
        plugin.getTablistManager().startTask();
        
        // Reaplicar UI a todos los jugadores online
        for (org.bukkit.entity.Player p : plugin.getServer().getOnlinePlayers()) {
            // Primero refrescar scoreboard (crea scoreboard individual con sidebar)
            plugin.getScoreboardManager().updatePlayer(p);
            
            // Luego reaplicar TAB/teams de rango (en el mismo scoreboard)
            plugin.getTablistManager().updatePlayer(p);
        }
        
        // Manejar reanudación si había desastre activo
        if ("ACTIVO".equals(estadoAnterior) && desastreActivo != null) {
            // Reanudar desastre manteniendo tiempo restante
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                disasterController.resumeDisaster(desastreActivo);
                sender.sendMessage("§a✓ Desastre reanudado: " + desastreActivo);
            }, 5L);
            
            sender.sendMessage("§7  - Desastre activo preservado durante reload");
        } else {
            // Reiniciar task principal solo si no había desastre
            disasterController.startTask();
        }
        
        // NO reiniciar desastres tras reload
        boolean startOnBoot = plugin.getConfigManager().isStartOnBoot();
        
        sender.sendMessage("§a✓ Reload completado:");
        sender.sendMessage("§7  - misiones_new.yml, rangos.yml, desastres.yml, recompensas.yml");
        sender.sendMessage("§7  - config.yml");
        sender.sendMessage("§7  - TAB/Scoreboard reaplicados a " + plugin.getServer().getOnlinePlayers().size() + " jugadores");
        sender.sendMessage("§7Flags de ciclo:");
        sender.sendMessage("§7  auto_cycle: §e" + plugin.getConfigManager().isAutoCycleEnabled());
        sender.sendMessage("§7  start_on_boot: §e" + startOnBoot);
        sender.sendMessage("§7  min_jugadores: §e" + plugin.getConfigManager().getMinJugadores());
        sender.sendMessage("§7  cooldown: §e" + plugin.getConfigManager().getCooldownFinSegundos() + "s");
        
        plugin.getLogger().info("[Reload] OK: misiones, rangos, desastres, recompensas aplicados. " +
            "TAB/Scoreboard reaplicados a " + plugin.getServer().getOnlinePlayers().size() + " jugadores.");
        
        // Si había un desastre activo, advertir
        if ("ACTIVO".equals(estadoAnterior)) {
            sender.sendMessage("§e⚠ Desastre activo detectado. UI reanexada desde state.yml.");
        }
    }
    
    private void cmdAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUso: /avo admin <add|remove|list> [jugador]");
            return;
        }

        String subAction = args[1].toLowerCase();

        switch (subAction) {
            case "add":
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo admin add <jugador>");
                    return;
                }
                
                Player targetAdd = plugin.getServer().getPlayer(args[2]);
                if (targetAdd == null) {
                    sender.sendMessage("§cJugador no encontrado: " + args[2]);
                    return;
                }
                
                plugin.getConfigManager().addExcepcion(targetAdd.getUniqueId());
                sender.sendMessage("§a✓ " + targetAdd.getName() + " añadido a excepciones (inmune a desastres).");
                plugin.getLogger().info("[Admin] " + sender.getName() + " añadió excepción: " + targetAdd.getName() + " (" + targetAdd.getUniqueId() + ")");
                break;
                
            case "remove":
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo admin remove <jugador>");
                    return;
                }
                
                Player targetRemove = plugin.getServer().getPlayer(args[2]);
                if (targetRemove == null) {
                    sender.sendMessage("§cJugador no encontrado: " + args[2]);
                    return;
                }
                
                plugin.getConfigManager().removeExcepcion(targetRemove.getUniqueId());
                sender.sendMessage("§a✓ " + targetRemove.getName() + " eliminado de excepciones.");
                plugin.getLogger().info("[Admin] " + sender.getName() + " eliminó excepción: " + targetRemove.getName() + " (" + targetRemove.getUniqueId() + ")");
                break;
                
            case "list":
                java.util.Set<java.util.UUID> excepciones = plugin.getConfigManager().getExcepciones();
                if (excepciones.isEmpty()) {
                    sender.sendMessage("§7No hay jugadores en la lista de excepciones.");
                    return;
                }
                
                sender.sendMessage("§e§lJugadores con excepción anti-desastre:");
                for (java.util.UUID uuid : excepciones) {
                    Player p = plugin.getServer().getPlayer(uuid);
                    if (p != null) {
                        sender.sendMessage("§f- " + p.getName() + " §7(" + uuid + ")");
                    } else {
                        sender.sendMessage("§f- §7[Offline] §f" + uuid);
                    }
                }
                break;
                
            default:
                sender.sendMessage("§cSubcomando desconocido. Usa: add, remove, list");
                break;
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // [NUEVOS COMANDOS] Utilidades adicionales para administración
    // ═══════════════════════════════════════════════════════════════════

    /**
     * /avo skip - Salta al siguiente estado (PREPARACION→ACTIVO→INACTIVO→PREPARACION)
     */
    private void cmdSkip(CommandSender sender) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        ServerState currentState = stateManager.getCurrentState();
        String newStateStr;
        
        switch (currentState) {
            case DETENIDO:
                newStateStr = "PREPARACION";
                sender.sendMessage("§a✓ Saltando a PREPARACION...");
                disasterController.startPreparation(5); // 5 min por defecto
                break;
            case PREPARACION:
                newStateStr = "ACTIVO";
                sender.sendMessage("§a✓ Saltando a ACTIVO...");
                stateManager.setEstado("ACTIVO");
                disasterController.startAuto(false);
                break;
            case ACTIVO:
                newStateStr = "DETENIDO";
                sender.sendMessage("§a✓ Saltando a DETENIDO...");
                disasterController.stopAllDisasters(true);
                break;
            default:
                sender.sendMessage("§cEstado actual no permite skip.");
                return;
        }
        
        plugin.getLogger().info("[Admin] " + sender.getName() + " skipped state: " + currentState + " → " + newStateStr);
    }

    /**
     * /avo time <set|add> <minutos> - Modifica tiempo restante del estado actual
     */
    private void cmdTime(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUso: /avo time <set|add> <minutos>");
            return;
        }

        String action = args[1].toLowerCase();
        int minutos;
        try {
            minutos = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cMinutos inválidos: " + args[2]);
            return;
        }

        // Validación de rango (0-1440 minutos = 24 horas)
        if (minutos < 0 || minutos > 1440) {
            sender.sendMessage("§cMinutos debe estar entre 0 y 1440 (24 horas)");
            return;
        }

        // Obtener TimeService para modificación en vivo
        TimeService timeService = plugin.getTimeService();
        ServerState currentState = stateManager.getCurrentState();
        
        // Debug logging
        plugin.getLogger().info("[cmdTime] Estado: " + currentState + " | TimeService running: " + timeService.isRunning());
        
        if ("set".equals(action)) {
            int segundos = minutos * 60;
            
            // Caso 1: TimeService activo (PREPARACION o ACTIVO)
            if (timeService.isRunning()) {
                // Actualizar TimeService (sistema en vivo)
                timeService.setRemainingSeconds(segundos);
                
                // Actualizar state.yml (persistencia)
                long now = System.currentTimeMillis();
                long newEndMs = now + (minutos * 60000L);
                stateManager.setLong("end_epoch_ms", newEndMs);
                stateManager.saveState();
                
                sender.sendMessage("§a✓ Tiempo establecido a " + minutos + " minutos (" + segundos + "s).");
                plugin.getLogger().info("[cmdTime] Modificado TimeService a " + segundos + "s");
            }
            // Caso 2: Estado DETENIDO con cooldown activo
            else if (currentState == ServerState.DETENIDO) {
                long lastEndMs = stateManager.getLong("last_end_epoch_ms", 0L);
                plugin.getLogger().info("[cmdTime] DETENIDO - last_end_epoch_ms: " + lastEndMs);
                
                if (lastEndMs > 0) {
                    // Hay cooldown activo - modificar last_end_epoch_ms para ajustar cooldown
                    long now = System.currentTimeMillis();
                    int cooldownTotal = plugin.getConfigManager().getCooldownFinSegundos();
                    long newLastEndMs = now - (cooldownTotal * 1000L) + (segundos * 1000L);
                    
                    plugin.getLogger().info("[cmdTime] Cooldown - now: " + now + " | cooldownTotal: " + cooldownTotal + "s | newLastEndMs: " + newLastEndMs);
                    
                    stateManager.setLong("last_end_epoch_ms", newLastEndMs);
                    stateManager.saveState();
                    
                    sender.sendMessage("§a✓ Cooldown establecido a " + minutos + " minutos (" + segundos + "s).");
                } else {
                    sender.sendMessage("§cNo hay temporizador activo ni cooldown en curso.");
                }
            }
            // Caso 3: Estado PREPARACION pero TimeService no running - INICIAR TimeService
            else if (currentState == ServerState.PREPARACION) {
                // Reiniciar TimeService para PREPARACION
                timeService.startPreparationMinutes(minutos);
                
                // Actualizar state.yml con nuevo epoch
                long now = System.currentTimeMillis();
                long newEndMs = now + (segundos * 1000L);
                stateManager.setLong("end_epoch_ms", newEndMs);
                
                // [FIX] Asegurar que prep_forzada está activa
                stateManager.setPrepForzada(true);
                stateManager.saveState();
                
                // [FIX] Resetear flags de countdown al modificar tiempo de preparación
                disasterController.resetCountdownFlags();
                
                sender.sendMessage("§a✓ Preparación establecida a " + minutos + " minutos (" + segundos + "s).");
                plugin.getLogger().info(String.format("[cmdTime] PREPARACION - TimeService reiniciado a %ds (end_epoch_ms=%d)", segundos, newEndMs));
            }
            // Caso 4: Otro estado sin TimeService running
            else {
                sender.sendMessage("§cEstado actual: " + currentState + " | TimeService: stopped");
                sender.sendMessage("§cNo se puede modificar el tiempo en este estado.");
            }
            
        } else if ("add".equals(action)) {
            int segundosAñadir = minutos * 60;
            
            // Caso 1: TimeService activo (PREPARACION o ACTIVO)
            if (timeService.isRunning()) {
                // Actualizar TimeService (sistema en vivo)
                timeService.addTime(segundosAñadir);
                
                // Actualizar state.yml (persistencia)
                long currentEndMs = stateManager.getLong("end_epoch_ms", 0L);
                long newEndMs = currentEndMs + (minutos * 60000L);
                stateManager.setLong("end_epoch_ms", newEndMs);
                stateManager.saveState();
                
                int totalMin = timeService.getRemainingSeconds() / 60;
                sender.sendMessage("§a✓ Añadidos " + minutos + " minutos (total: " + totalMin + " min).");
                plugin.getLogger().info("[cmdTime] Añadidos " + segundosAñadir + "s a TimeService");
            }
            // Caso 2: Estado DETENIDO con cooldown activo
            else if (currentState == ServerState.DETENIDO) {
                long lastEndMs = stateManager.getLong("last_end_epoch_ms", 0L);
                plugin.getLogger().info("[cmdTime] ADD - DETENIDO - last_end_epoch_ms: " + lastEndMs);
                
                if (lastEndMs > 0) {
                    // Añadir tiempo al cooldown (retrasando last_end_epoch_ms)
                    long newLastEndMs = lastEndMs - (segundosAñadir * 1000L);
                    stateManager.setLong("last_end_epoch_ms", newLastEndMs);
                    stateManager.saveState();
                    
                    // Calcular nuevo cooldown restante
                    int cooldownSeconds = plugin.getConfigManager().getCooldownFinSegundos();
                    long cooldownEndMs = newLastEndMs + (cooldownSeconds * 1000L);
                    int remainingSeconds = (int) Math.max(0, (cooldownEndMs - System.currentTimeMillis()) / 1000L);
                    int totalMin = remainingSeconds / 60;
                    
                    plugin.getLogger().info("[cmdTime] ADD Cooldown - newLastEndMs: " + newLastEndMs + " | total restante: " + remainingSeconds + "s");
                    
                    sender.sendMessage("§a✓ Añadidos " + minutos + " minutos al cooldown (total: " + totalMin + " min).");
                } else {
                    sender.sendMessage("§cNo hay cooldown activo para modificar.");
                }
            }
            // Caso 3: Estado PREPARACION/ACTIVO pero TimeService no running
            else {
                sender.sendMessage("§cEstado actual: " + currentState + " | TimeService: " + (timeService.isRunning() ? "running" : "stopped"));
                sender.sendMessage("§cNo se puede modificar el tiempo en este estado.");
            }
            
        } else {
            sender.sendMessage("§cAcción inválida. Usa: set o add");
            return;
        }
        
        plugin.getLogger().info("[Admin] " + sender.getName() + " modificó tiempo: " + action + " " + minutos + " min");
    }

    /**
     * /avo setxp <jugador> <xp> - Ajusta XP de un jugador manualmente
     */
    private void cmdSetXp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUso: /avo setxp <jugador> <xp>");
            return;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJugador no encontrado: " + args[1]);
            return;
        }

        int xp;
        try {
            xp = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cXP inválido: " + args[2]);
            return;
        }

        if (xp < 0) {
            sender.sendMessage("§cXP no puede ser negativo.");
            return;
        }

        int oldXp = plugin.getRankService().getXP(target);
        if (plugin.getExperienceService() != null) {
            plugin.getExperienceService().setXP(target, xp);
        } else {
            plugin.getMissionService().setPlayerPS(target.getUniqueId(), xp);
        }
        
        sender.sendMessage("§a✓ XP de " + target.getName() + ": §e" + oldXp + " §7→ §e" + xp);
        target.sendMessage("§6[Admin] §aTu XP fue ajustada a §e" + xp);
        
        // Actualizar UI
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updatePlayer(target);
        }
        if (plugin.getTablistManager() != null) {
            plugin.getTablistManager().applyTabPrefix(target);
        }
        
        plugin.getLogger().info("[Admin] " + sender.getName() + " ajustó XP de " + target.getName() + ": " + oldXp + " → " + xp);
    }

    /**
     * /avo mission <give|complete|clear> - Gestión de misiones
     */
    private void cmdMission(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUso: /avo mission <give|complete|clear>");
            sender.sendMessage("§7- give <jugador> <tipo> <objetivo> <meta>");
            sender.sendMessage("§7- complete <jugador> [todas]");
            sender.sendMessage("§7- clear <jugador>");
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "give":
                if (args.length < 6) {
                    sender.sendMessage("§cUso: /avo mission give <jugador> <tipo> <objetivo> <meta>");
                    sender.sendMessage("§7Ejemplo: /avo mission give Notch MATAR ZOMBIE 10");
                    return;
                }
                
                Player targetGive = plugin.getServer().getPlayer(args[2]);
                if (targetGive == null) {
                    sender.sendMessage("§cJugador no encontrado: " + args[2]);
                    return;
                }

                try {
                    MissionType tipo = MissionType.valueOf(args[3].toUpperCase());
                    String objetivo = args[4].toUpperCase();
                    int meta;
                    try {
                        meta = Integer.parseInt(args[5]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cMeta inválida: " + args[5]);
                        return;
                    }

                    if (meta <= 0) {
                        sender.sendMessage("§cLa meta debe ser mayor a 0");
                        return;
                    }

                    // Asignar misión personalizada mediante MissionService
                    boolean success = plugin.getMissionService().addCustomMission(
                        targetGive.getUniqueId(), 
                        tipo, 
                        objetivo, 
                        meta,
                        plugin.getRankService().getRank(targetGive)
                    );

                    if (success) {
                        sender.sendMessage("§a✓ Misión asignada a " + targetGive.getName());
                        sender.sendMessage("§7Tipo: §e" + tipo + " §7| Objetivo: §e" + objetivo + " §7| Meta: §e" + meta);
                        targetGive.sendMessage("§6[Misión] §eSe te ha asignado una misión especial:");
                        targetGive.sendMessage("§7" + tipo.name() + " " + meta + "x " + objetivo);
                        
                        plugin.getLogger().info("[Admin] " + sender.getName() + " asignó misión a " + targetGive.getName() + 
                            ": " + tipo + " " + objetivo + " x" + meta);
                    } else {
                        sender.sendMessage("§cError al asignar misión. Ver console para detalles.");
                    }

                } catch (IllegalArgumentException e) {
                    sender.sendMessage("§cTipo de misión inválido. Usa: MATAR, ROMPER, COLOCAR, PESCAR, CRAFTEAR");
                    return;
                }
                break;

            case "complete":
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo mission complete <jugador> [todas|auto]");
                    sender.sendMessage("§7  todas - Completa todas las misiones");
                    sender.sendMessage("§7  auto - Solo completa las autocompletables (default)");
                    return;
                }

                Player targetComplete = plugin.getServer().getPlayer(args[2]);
                if (targetComplete == null) {
                    sender.sendMessage("§cJugador no encontrado: " + args[2]);
                    return;
                }

                // Determinar si completar todas o solo autocompletables
                boolean completeAll = args.length >= 4 && "todas".equalsIgnoreCase(args[3]);
                int completed = missionService.forceCompleteAllMissions(targetComplete, completeAll);
                
                String type = completeAll ? "todas" : "autocompletables";
                sender.sendMessage("§a✓ Completadas " + completed + " misiones " + type + " de " + targetComplete.getName());
                targetComplete.sendMessage("§6[Admin] §aTus misiones " + type + " fueron completadas.");
                break;

            case "clear":
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo mission clear <jugador>");
                    return;
                }

                Player targetClear = plugin.getServer().getPlayer(args[2]);
                if (targetClear == null) {
                    sender.sendMessage("§cJugador no encontrado: " + args[2]);
                    return;
                }

                missionService.clearPlayerMissions(targetClear.getUniqueId());
                sender.sendMessage("§a✓ Misiones de " + targetClear.getName() + " eliminadas.");
                targetClear.sendMessage("§6[Admin] §eTus misiones fueron limpiadas.");
                
                if (plugin.getScoreboardManager() != null) {
                    plugin.getScoreboardManager().updatePlayer(targetClear);
                }
                break;

            default:
                sender.sendMessage("§cAcción inválida. Usa: give, complete, clear");
                break;
        }
    }

    /**
     * /avo tps - Muestra TPS actual y estado de performance
     */
    private void cmdTps(CommandSender sender) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        double tps = plugin.getPerformanceAdapter().getLastTPS();
        String tpsColor;
        
        if (tps >= 18.0) {
            tpsColor = "§a"; // Verde
        } else if (tps >= 14.0) {
            tpsColor = "§e"; // Amarillo
        } else if (tps >= 10.0) {
            tpsColor = "§6"; // Naranja
        } else {
            tpsColor = "§c"; // Rojo
        }

        sender.sendMessage("§6=== Performance del Servidor ===");
        sender.sendMessage("§7TPS: " + tpsColor + String.format("%.2f", tps) + " §7/ 20.00");
        sender.sendMessage("§7Estado: " + plugin.getPerformanceAdapter().getCurrentState().name());
        sender.sendMessage("§7Safe Mode: " + (stateManager.isSafeModeActive() ? "§cACTIVO" : "§aInactivo"));
        
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1048576L; // MB
        long totalMemory = runtime.totalMemory() / 1048576L;
        long freeMemory = runtime.freeMemory() / 1048576L;
        long usedMemory = totalMemory - freeMemory;
        
        sender.sendMessage("§7Memoria: §f" + usedMemory + " MB §7/ §f" + maxMemory + " MB");
        sender.sendMessage("§7Jugadores: §f" + plugin.getServer().getOnlinePlayers().size());
    }

    /**
     * /avo stats - Estadísticas del servidor y plugin
     */
    private void cmdStats(CommandSender sender) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        sender.sendMessage("§6§l=== Estadísticas del Servidor ===");
        sender.sendMessage("§7Estado actual: §e" + stateManager.getEstado());
        sender.sendMessage("§7Día actual: §e" + stateManager.getCurrentDay());
        sender.sendMessage("§7Desastre activo: §e" + (stateManager.getActiveDisasterId() != null ? stateManager.getActiveDisasterId() : "Ninguno"));
        sender.sendMessage("§7Último desastre: §e" + (stateManager.getLastDisasterId() != null ? stateManager.getLastDisasterId() : "N/A"));
        
        int totalPlayers = plugin.getServer().getOnlinePlayers().size();
        int exemptPlayers = plugin.getConfigManager().getExcepciones().size();
        sender.sendMessage("§7Jugadores online: §e" + totalPlayers);
        sender.sendMessage("§7Excepciones activas: §e" + exemptPlayers);
        
        sender.sendMessage("§7Modo test: " + (plugin.getConfigManager().isTestMode() ? "§aACTIVO" : "§7Inactivo"));
        sender.sendMessage("§7TPS actual: §e" + String.format("%.2f", plugin.getPerformanceAdapter().getLastTPS()));
        
        // Uptime del servidor
        long uptimeMs = System.currentTimeMillis() - plugin.getStateManager().getLastEndEpochMs();
        if (uptimeMs > 0) {
            long hours = uptimeMs / 3600000L;
            long minutes = (uptimeMs % 3600000L) / 60000L;
            sender.sendMessage("§7Uptime aproximado: §e" + hours + "h " + minutes + "m");
        }
    }

    /**
     * /avo backup - Crea backup manual de state.yml y mission_data.yml
     */
    private void cmdBackup(CommandSender sender) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        sender.sendMessage("§eCreando backup de datos...");
        
        try {
            // Guardar estados actuales
            stateManager.saveState();
            missionService.savePlayerData();
            
            // Crear copias con timestamp
            long timestamp = System.currentTimeMillis();
            String timestampStr = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date(timestamp));
            
            java.io.File dataFolder = plugin.getDataFolder();
            java.io.File stateFile = new java.io.File(dataFolder, "state.yml");
            java.io.File missionFile = new java.io.File(dataFolder, "mission_data.yml");
            
            java.io.File backupFolder = new java.io.File(dataFolder, "backups");
            if (!backupFolder.exists()) {
                backupFolder.mkdirs();
            }
            
            java.io.File stateBackup = new java.io.File(backupFolder, "state_" + timestampStr + ".yml");
            java.io.File missionBackup = new java.io.File(backupFolder, "mission_data_" + timestampStr + ".yml");
            
            if (stateFile.exists()) {
                java.nio.file.Files.copy(stateFile.toPath(), stateBackup.toPath());
            }
            if (missionFile.exists()) {
                java.nio.file.Files.copy(missionFile.toPath(), missionBackup.toPath());
            }
            
            sender.sendMessage("§a✓ Backup creado exitosamente:");
            sender.sendMessage("§7- state_" + timestampStr + ".yml");
            sender.sendMessage("§7- mission_data_" + timestampStr + ".yml");
            
            plugin.getLogger().info("[Admin] " + sender.getName() + " creó backup manual: " + timestampStr);
            
        } catch (Exception e) {
            sender.sendMessage("§c✗ Error al crear backup: " + e.getMessage());
            plugin.getLogger().warning("[Admin] Error en backup: " + e.getMessage());
        }
    }

    /**
     * /avo test-alert <jugador> - Envía notificación de prueba a un jugador
     */
    private void cmdTestAlert(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUso: /avo test-alert <jugador>");
            return;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJugador no encontrado: " + args[1]);
            return;
        }

        sender.sendMessage("§eEnviando notificación de prueba a §f" + target.getName() + "§e...");
        plugin.getLogger().info("[Test-Alert] " + sender.getName() + " enviando prueba a " + target.getName());

        // Llamar al método de prueba del DisasterController
        disasterController.testCountdownAlert(target, sender);
    }

    /**
     * /avo escanear - Escanea y muestra protecciones cercanas con partículas
     */
    private void cmdEscanear(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return;
        }

        Player player = (Player) sender;
        Location loc = player.getLocation();
        
        player.sendMessage("§8┌─────────────────────────────────────┐");
        player.sendMessage("§6│ §e§l🛡 ESCANEO DE PROTECCIONES §6        │");
        player.sendMessage("§8├─────────────────────────────────────┤");
        
        // === TERREMOTO: Bloques Absorbentes ===
        Map<Material, Integer> absorbentes = escanearBloquesAbsorbentes(loc, 6);
        int totalBloques = 0;
        for (int count : absorbentes.values()) {
            totalBloques += count;
        }
        int efectivos = Math.min(totalBloques, 5); // Cap de 5 bloques
        int reduccionShake = efectivos * 15; // 15% por bloque
        int reduccionBreak = efectivos * 20; // 20% por bloque
        int reduccionDamage = efectivos * 25; // 25% por bloque
        
        player.sendMessage("§6│ §e⛰️ Terremoto:                        §6│");
        if (totalBloques > 0) {
            for (Map.Entry<Material, Integer> entry : absorbentes.entrySet()) {
                String nombre = getNombreMaterial(entry.getKey());
                player.sendMessage(String.format("§6│  §a✓ §7%d %s                     §6│", 
                    entry.getValue(), nombre));
            }
            player.sendMessage(String.format("§6│  §7Total: §e%d §7bloques §8(§aefectivos: %d§8)§6│", 
                totalBloques, efectivos));
            player.sendMessage(String.format("§6│  §7Shake §a-%d%% §8| §7Break §a-%d%% §8| §7Daño §a-%d%%§6│", 
                reduccionShake, reduccionBreak, reduccionDamage));
            
            // Spawnear partículas verdes en bloques absorbentes
            spawnParticlesEnBloques(absorbentes, loc, Particle.HAPPY_VILLAGER);
        } else {
            player.sendMessage("§6│  §c✗ Sin bloques protectores           §6│");
            player.sendMessage("§6│  §7Usa §blana§7, §aslime§7, §bhielo     §6│");
        }
        
        player.sendMessage("§8│                                     │");
        
        // === LLUVIA DE FUEGO: Agua ===
        WaterScanResult agua = escanearAgua(loc, 3);
        player.sendMessage("§6│ §e🔥 Lluvia de Fuego:                  §6│");
        if (agua.waterBlocks > 0) {
            if (agua.hasDeepWater) {
                player.sendMessage("§6│  §a✓ Agua profunda §8(§a2+ bloques§8)    §6│");
                player.sendMessage("§6│  §7Explosión §a-60% §8| §7Fuego §aAPAGADO §6│");
            } else {
                player.sendMessage(String.format("§6│  §a✓ §b%d §7bloques de agua           §6│", 
                    agua.waterBlocks));
                player.sendMessage("§6│  §7Explosión §a-60% §8| §7Evaporación lenta§6│");
            }
            
            // Partículas azules en agua
            spawnParticlesEnAgua(loc, 3, Particle.BUBBLE_POP);
        } else {
            player.sendMessage("§6│  §c✗ Sin protección de agua           §6│");
            player.sendMessage("§6│  §7Coloca §bagua §7cerca (3 bloques)  §6│");
        }
        
        player.sendMessage("§8│                                     │");
        
        // === HURACÁN: Techo ===
        boolean tieneTecho = escanearTecho(player);
        player.sendMessage("§6│ §e🌪️ Huracán:                          §6│");
        if (tieneTecho) {
            player.sendMessage("§6│  §a✓ Techo detectado                  §6│");
            player.sendMessage("§6│  §7Empuje §a-60% §8| §7Agachado §a-55%   §6│");
            player.sendMessage("§6│  §7Combo: §a-85% §7reducción total     §6│");
            
            // Partículas arriba del jugador
            Location above = loc.clone().add(0, 5, 0);
            player.getWorld().spawnParticle(Particle.END_ROD, above, 10, 1, 0.1, 1, 0.01);
        } else {
            player.sendMessage("§6│  §c✗ Expuesto al viento               §6│");
            player.sendMessage("§6│  §7Construye §etecho §7de 5+ bloques  §6│");
        }
        
        player.sendMessage("§8└─────────────────────────────────────┘");
        
        // Sonido de confirmación
        player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        player.sendMessage("§a✓ §7Escaneo completado. §8(§7Partículas visibles 20s§8)");
    }

    /**
     * /avo protecciones - Muestra guía completa de protecciones
     */
    private void cmdProtecciones(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§8╔═══════════════════════════════════════════╗");
        sender.sendMessage("§8║ §6§l      📚 GUÍA DE PROTECCIONES          §8║");
        sender.sendMessage("§8╠═══════════════════════════════════════════╣");
        sender.sendMessage("§8║                                           §8║");
        
        // TERREMOTO
        sender.sendMessage("§8║ §e§l⛰️  TERREMOTO                           §8║");
        sender.sendMessage("§8║ §7Bloques Absorbentes §8(§76-block radius§8): §8║");
        sender.sendMessage("§8║   §f• §bLana §8(§716 colores§8) §7........... §a-15%§8║");
        sender.sendMessage("§8║   §f• §aSlime Block §7............... §a-15%§8║");
        sender.sendMessage("§8║   §f• §eHoney Block §7............... §a-15%§8║");
        sender.sendMessage("§8║   §f• §bBlue Ice §7.................. §a-10%§8║");
        sender.sendMessage("§8║   §f• §eHay Block §7................. §a-10%§8║");
        sender.sendMessage("§8║   §f• §6Sponge §7.................... §a-15%§8║");
        sender.sendMessage("§8║                                           §8║");
        sender.sendMessage("§8║ §c⚠ §7Máximo: §e5 bloques efectivos         §8║");
        sender.sendMessage("§8║ §7💡 Reduce: Shake, Break y Daño          §8║");
        sender.sendMessage("§8║ §7💡 Colócalos en radio de §e6 bloques     §8║");
        sender.sendMessage("§8║                                           §8║");
        
        // LLUVIA DE FUEGO
        sender.sendMessage("§8║ §e§l🔥 LLUVIA DE FUEGO                      §8║");
        sender.sendMessage("§8║ §7Protección de Agua §8(§73x3x3§8):          §8║");
        sender.sendMessage("§8║   §f• §bAgua Normal §7............... §a-60%§8║");
        sender.sendMessage("§8║   §f• §bAgua Profunda §8(§72+ bloques§8) §a-60%§8║");
        sender.sendMessage("§8║                                           §8║");
        sender.sendMessage("§8║ §7💧 Reduce explosiones y apaga fuego     §8║");
        sender.sendMessage("§8║ §7💧 Agua profunda: §aInmune a evaporación§8║");
        sender.sendMessage("§8║ §7💡 Coloca §b3+ bloques §7cerca de ti     §8║");
        sender.sendMessage("§8║                                           §8║");
        
        // HURACÁN
        sender.sendMessage("§8║ §e§l🌪️  HURACÁN                             §8║");
        sender.sendMessage("§8║ §7Protección Estructural:                §8║");
        sender.sendMessage("§8║   §f• §eTecho §8(§75+ bloques arriba§8) §7.. §a-60%§8║");
        sender.sendMessage("§8║   §f• §7Agacharse §8(§7Sneaking§8) §7........ §a-55%§8║");
        sender.sendMessage("§8║   §f• §a§lCombo §8(§7Techo + Agachado§8) §7. §a-85%§8║");
        sender.sendMessage("§8║                                           §8║");
        sender.sendMessage("§8║ §7🌪️ Reduce empuje del viento             §8║");
        sender.sendMessage("§8║ §7💡 Construye refugio con §etecho sólido §8║");
        sender.sendMessage("§8║ §7💡 §7Durante ráfagas: §aagáchate siempre §8║");
        sender.sendMessage("§8║                                           §8║");
        
        // CONSEJOS GENERALES
        sender.sendMessage("§8║ §6§l💡 CONSEJOS GENERALES                  §8║");
        sender.sendMessage("§8║ §71. Usa §e/avo escanear §7para verificar §8║");
        sender.sendMessage("§8║ §72. Prepara refugios §aANTES §7del desastre§8║");
        sender.sendMessage("§8║ §73. Combina múltiples protecciones       §8║");
        sender.sendMessage("§8║ §74. Revisa durabilidad con §e/avo escanear§8║");
        sender.sendMessage("§8║                                           §8║");
        sender.sendMessage("§8╚═══════════════════════════════════════════╝");
        sender.sendMessage("");
        
        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
        }
    }
    
    /**
     * /avo eco <subcomando> - Gestión del evento Eco de Brasas
     */
    private void cmdEco(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§e§l=== ECO DE BRASAS - COMANDOS ===");
            sender.sendMessage("§e/avo eco start §7- Inicia el evento");
            sender.sendMessage("§e/avo eco stop §7- Detiene el evento");
            sender.sendMessage("§e/avo eco skip §7- §cSalta cinemática/diálogo actual");
            sender.sendMessage("§e/avo eco fase <1|2|3> §7- Fuerza fase (1=Recolección, 2=Estabilización, 3=Ritual)");
            sender.sendMessage("§e/avo eco next §7- Avanza a la siguiente fase");
            sender.sendMessage("§e/avo eco info §7- Muestra información detallada");
            sender.sendMessage("§e/avo eco pulso <add|set> <valor> §7- Modifica pulso global");
            sender.sendMessage("§e/avo eco ancla <1-3> §7- Completa ancla específica");
            return;
        }
        
        String subCmd = args[1].toLowerCase();
        
        // Obtener instancia del evento desde EventController
        me.apocalipsis.events.EcoBrasasEvent ecoBrasas = null;
        if (eventController.hasActiveEvent() && 
            eventController.getActiveEvent() instanceof me.apocalipsis.events.EcoBrasasEvent) {
            ecoBrasas = (me.apocalipsis.events.EcoBrasasEvent) eventController.getActiveEvent();
        }
        
        switch (subCmd) {
            case "start":
            case "iniciar":
                // Verificar si hay desastre activo
                if (disasterController.hasActiveDisaster()) {
                    sender.sendMessage("§cYa hay un desastre activo. Usa §e/avo stop §cprimero.");
                    return;
                }
                
                // Verificar si ya hay evento activo
                if (eventController.hasActiveEvent()) {
                    sender.sendMessage("§cYa hay un evento activo. Usa §e/avo eco stop §cprimero.");
                    return;
                }
                
                // Verificar SAFE_MODE
                if (stateManager.isSafeModeActive()) {
                    sender.sendMessage("§cNo se puede iniciar en SAFE_MODE (TPS bajo).");
                    return;
                }
                
                // Iniciar Eco de Brasas usando EventController
                if (eventController.startEvent("eco_brasas")) {
                    sender.sendMessage("§a✓ Evento §5§lEco de Brasas §ainiciado");
                    sender.sendMessage("§7Aguarda... §d§ola historia comienza§7...");
                    plugin.getLogger().info(String.format("[EcoBrasas] Iniciado por %s", sender.getName()));
                } else {
                    sender.sendMessage("§cNo se pudo iniciar el evento. Verifica la consola.");
                }
                break;
                
            case "stop":
            case "detener":
                if (ecoBrasas == null) {
                    sender.sendMessage("§cEl evento Eco de Brasas no está activo.");
                    return;
                }
                
                eventController.stopActiveEvent();
                sender.sendMessage("§7✓ Evento §5Eco de Brasas §7detenido");
                
                plugin.getLogger().info(String.format("[EcoBrasas] Detenido por %s", sender.getName()));
                break;
                
            case "skip":
            case "saltar":
                if (ecoBrasas == null) {
                    sender.sendMessage("§cEl evento Eco de Brasas no está activo.");
                    return;
                }
                
                // Saltar cinemática o avanzar a siguiente fase
                if (ecoBrasas.forzarSiguienteFase()) {
                    sender.sendMessage("§a✓ Cinemática/fase saltada");
                    sender.sendMessage("§7Fase actual: §e" + ecoBrasas.getFaseActual());
                    plugin.getLogger().info(String.format("[EcoBrasas] Skip ejecutado por %s", sender.getName()));
                } else {
                    sender.sendMessage("§cYa estás en la última fase o no se pudo saltar.");
                }
                break;
                
            case "fase":
            case "phase":
                if (ecoBrasas == null) {
                    sender.sendMessage("§cEl evento Eco de Brasas no está activo.");
                    return;
                }
                
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo eco fase <1|2|3>");
                    sender.sendMessage("§7  1 = Recolección (grietas)");
                    sender.sendMessage("§7  2 = Estabilización (anclas)");
                    sender.sendMessage("§7  3 = Ritual Final (altar)");
                    return;
                }
                
                String faseArg = args[2];
                boolean success = ecoBrasas.forzarFase(faseArg);
                
                if (success) {
                    String faseNombre = "";
                    switch (faseArg) {
                        case "1":
                        case "recoleccion":
                            faseNombre = "RECOLECCIÓN";
                            break;
                        case "2":
                        case "estabilizacion":
                            faseNombre = "ESTABILIZACIÓN";
                            break;
                        case "3":
                        case "ritual":
                            faseNombre = "RITUAL FINAL";
                            break;
                    }
                    sender.sendMessage("§a✓ Fase cambiada a: §e§l" + faseNombre);
                    plugin.getLogger().info(String.format("[EcoBrasas] %s forzó fase: %s", sender.getName(), faseNombre));
                } else {
                    sender.sendMessage("§cYa estás en esa fase o fase inválida.");
                }
                break;
                
            case "next":
            case "siguiente":
                if (ecoBrasas == null) {
                    sender.sendMessage("§cEl evento Eco de Brasas no está activo.");
                    return;
                }
                
                if (ecoBrasas.forzarSiguienteFase()) {
                    sender.sendMessage("§a✓ Avanzado a la siguiente fase");
                    sender.sendMessage("§7Fase actual: §e" + ecoBrasas.getFaseActual());
                } else {
                    sender.sendMessage("§cYa estás en la última fase o no se pudo avanzar.");
                }
                break;
                
            case "info":
            case "status":
                if (ecoBrasas == null) {
                    sender.sendMessage("§cEl evento Eco de Brasas no está activo.");
                    sender.sendMessage("§7Usa §e/avo eco start §7para iniciarlo.");
                    return;
                }
                
                String info = ecoBrasas.getInfoDetallada();
                sender.sendMessage(info);
                break;
                
            case "pulso":
                if (ecoBrasas == null) {
                    sender.sendMessage("§cEl evento Eco de Brasas no está activo.");
                    return;
                }
                
                if (args.length < 4) {
                    sender.sendMessage("§cUso: /avo eco pulso <add|set> <valor>");
                    sender.sendMessage("§7Ejemplo: §e/avo eco pulso add 50 §7(añade 50%)");
                    sender.sendMessage("§7Ejemplo: §e/avo eco pulso set 100 §7(establece a 100%)");
                    return;
                }
                
                String pulsoAction = args[2].toLowerCase();
                int pulsoValor;
                
                try {
                    pulsoValor = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cValor inválido: " + args[3]);
                    return;
                }
                
                if (pulsoAction.equals("add")) {
                    ecoBrasas.addPulsoGlobal(pulsoValor);
                    sender.sendMessage("§a✓ Pulso ajustado: §e+" + pulsoValor + "%");
                } else if (pulsoAction.equals("set")) {
                    // Calcular diferencia
                    int actual = ecoBrasas.getProgresoFase();
                    ecoBrasas.addPulsoGlobal(pulsoValor - actual);
                    sender.sendMessage("§a✓ Pulso establecido a: §e" + pulsoValor + "%");
                } else {
                    sender.sendMessage("§cAcción inválida. Usa: add o set");
                    return;
                }
                
                sender.sendMessage("§7Progreso actual: §e" + ecoBrasas.getProgresoFase() + "%");
                break;
                
            case "ancla":
                if (ecoBrasas == null) {
                    sender.sendMessage("§cEl evento Eco de Brasas no está activo.");
                    return;
                }
                
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo eco ancla <1|2|3>");
                    sender.sendMessage("§7Completa forzadamente el ancla especificada");
                    return;
                }
                
                int anclaId;
                try {
                    anclaId = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cID de ancla inválido: " + args[2]);
                    return;
                }
                
                if (ecoBrasas.completarAncla(anclaId)) {
                    sender.sendMessage("§a✓ Ancla #" + anclaId + " completada forzadamente");
                    plugin.getLogger().info(String.format("[EcoBrasas] %s completó ancla #%d", sender.getName(), anclaId));
                } else {
                    sender.sendMessage("§cNo se pudo completar el ancla. Verifica:");
                    sender.sendMessage("§7- Estar en Fase 2 (Estabilización)");
                    sender.sendMessage("§7- ID válido (1-3)");
                    sender.sendMessage("§7- Ancla no completada previamente");
                }
                break;
                
            default:
                sender.sendMessage("§cSubcomando desconocido: §f" + subCmd);
                sender.sendMessage("§7Usa §e/avo eco §7para ver comandos disponibles.");
                break;
        }
    }

    // ========== MÉTODOS AUXILIARES PARA ESCANEO ==========
    
    /**
     * Escanea bloques absorbentes en un radio específico
     */
    private Map<Material, Integer> escanearBloquesAbsorbentes(Location center, int radio) {
        Map<Material, Integer> encontrados = new HashMap<>();
        Set<Material> materialesAbsorbentes = EnumSet.of(
            Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL, Material.LIGHT_BLUE_WOOL,
            Material.YELLOW_WOOL, Material.LIME_WOOL, Material.PINK_WOOL, Material.GRAY_WOOL,
            Material.LIGHT_GRAY_WOOL, Material.CYAN_WOOL, Material.PURPLE_WOOL, Material.BLUE_WOOL,
            Material.BROWN_WOOL, Material.GREEN_WOOL, Material.RED_WOOL, Material.BLACK_WOOL,
            Material.SLIME_BLOCK, Material.BLUE_ICE, Material.HAY_BLOCK, Material.SPONGE, 
            Material.WET_SPONGE, Material.HONEY_BLOCK, Material.PACKED_ICE, Material.ICE
        );
        
        for (int x = -radio; x <= radio; x++) {
            for (int y = -radio; y <= radio; y++) {
                for (int z = -radio; z <= radio; z++) {
                    Block block = center.clone().add(x, y, z).getBlock();
                    if (materialesAbsorbentes.contains(block.getType())) {
                        encontrados.put(block.getType(), 
                            encontrados.getOrDefault(block.getType(), 0) + 1);
                    }
                }
            }
        }
        
        return encontrados;
    }
    
    /**
     * Escanea agua alrededor del jugador
     */
    private WaterScanResult escanearAgua(Location center, int radio) {
        int waterBlocks = 0;
        boolean hasDeepWater = false;
        
        for (int x = -radio; x <= radio; x++) {
            for (int y = -radio; y <= radio; y++) {
                for (int z = -radio; z <= radio; z++) {
                    Block block = center.clone().add(x, y, z).getBlock();
                    if (block.getType() == Material.WATER) {
                        waterBlocks++;
                        // Verificar si hay agua arriba (agua profunda)
                        Block above = block.getRelative(0, 1, 0);
                        if (above.getType() == Material.WATER) {
                            hasDeepWater = true;
                        }
                    }
                }
            }
        }
        
        return new WaterScanResult(waterBlocks, hasDeepWater);
    }
    
    /**
     * Verifica si el jugador tiene techo
     */
    private boolean escanearTecho(Player player) {
        Location loc = player.getLocation();
        for (int i = 1; i <= 5; i++) {
            Block above = loc.clone().add(0, i, 0).getBlock();
            if (above.getType().isSolid() && above.getType() != Material.BARRIER) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Spawnea partículas en bloques encontrados
     */
    private void spawnParticlesEnBloques(Map<Material, Integer> bloques, Location playerLoc, Particle particle) {
        int radio = 6;
        int particlesSpawned = 0;
        
        for (int x = -radio; x <= radio; x++) {
            for (int y = -radio; y <= radio; y++) {
                for (int z = -radio; z <= radio; z++) {
                    Block block = playerLoc.clone().add(x, y, z).getBlock();
                    if (bloques.containsKey(block.getType())) {
                        Location particleLoc = block.getLocation().add(0.5, 0.5, 0.5);
                        playerLoc.getWorld().spawnParticle(particle, particleLoc, 3, 0.3, 0.3, 0.3, 0);
                        particlesSpawned++;
                        
                        if (particlesSpawned >= 50) return; // Limitar para evitar lag
                    }
                }
            }
        }
    }
    
    /**
     * Spawnea partículas en agua
     */
    private void spawnParticlesEnAgua(Location center, int radio, Particle particle) {
        int particlesSpawned = 0;
        
        for (int x = -radio; x <= radio; x++) {
            for (int y = -radio; y <= radio; y++) {
                for (int z = -radio; z <= radio; z++) {
                    Block block = center.clone().add(x, y, z).getBlock();
                    if (block.getType() == Material.WATER) {
                        Location particleLoc = block.getLocation().add(0.5, 0.5, 0.5);
                        center.getWorld().spawnParticle(particle, particleLoc, 2, 0.2, 0.2, 0.2, 0);
                        particlesSpawned++;
                        
                        if (particlesSpawned >= 30) return;
                    }
                }
            }
        }
    }
    
    /**
     * Obtiene nombre legible de material
     */
    private String getNombreMaterial(Material mat) {
        switch (mat) {
            case SLIME_BLOCK: return "Slime";
            case BLUE_ICE: return "Hielo Azul";
            case HAY_BLOCK: return "Heno";
            case SPONGE: case WET_SPONGE: return "Esponja";
            case HONEY_BLOCK: return "Miel";
            case PACKED_ICE: return "Hielo Compacto";
            case ICE: return "Hielo";
            default:
                if (mat.name().contains("WOOL")) {
                    String color = mat.name().replace("_WOOL", "").replace("_", " ");
                    return "Lana " + color.substring(0, 1) + color.substring(1).toLowerCase();
                }
                return mat.name();
        }
    }
    
    /**
     * Clase auxiliar para resultado de escaneo de agua
     */
    private static class WaterScanResult {
        final int waterBlocks;
        final boolean hasDeepWater;
        
        WaterScanResult(int waterBlocks, boolean hasDeepWater) {
            this.waterBlocks = waterBlocks;
            this.hasDeepWater = hasDeepWater;
        }
    }
    
    /**
     * Comando para gestionar XP
     * /avo xp <add|set|get> <jugador> [cantidad]
     */
    private void cmdXP(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§e=== Comandos de XP ===");
            sender.sendMessage("§7/avo xp get <jugador> §f- Ver XP de un jugador");
            sender.sendMessage("§7/avo xp add <jugador> <cantidad> §f- Añadir XP");
            sender.sendMessage("§7/avo xp set <jugador> <cantidad> §f- Establecer XP");
            sender.sendMessage("§7/avo xp reset <jugador> §f- Resetear XP a 0");
            return;
        }
        
        String action = args[1].toLowerCase();
        
        if (action.equals("get") && args.length >= 3) {
            Player target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("§cJugador no encontrado.");
                return;
            }
            
            int xp = plugin.getExperienceService().getXP(target);
            int nivel = plugin.getExperienceService().getLevel(target);
            int xpForNext = plugin.getExperienceService().getXPForNextLevel(target);
            
            sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            sender.sendMessage("§6XP de §f" + target.getName());
            sender.sendMessage("§7Nivel: §b" + nivel);
            sender.sendMessage("§7XP Total: §e" + xp);
            sender.sendMessage("§7XP para siguiente nivel: §e" + xpForNext);
            sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            
        } else if ((action.equals("add") || action.equals("set")) && args.length >= 4) {
            Player target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("§cJugador no encontrado.");
                return;
            }
            
            int amount;
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cCantidad inválida.");
                return;
            }
            
            if (action.equals("add")) {
                int oldXP = plugin.getExperienceService().getXP(target);
                plugin.getExperienceService().addXP(target, amount, "Admin");
                int newXP = plugin.getExperienceService().getXP(target);
                sender.sendMessage("§a✓ XP añadido a " + target.getName() + ": §e" + oldXP + " §7→ §e" + newXP);
            } else {
                plugin.getExperienceService().setXP(target, amount);
                sender.sendMessage("§a✓ XP establecido para " + target.getName() + ": §e" + amount);
            }
            
        } else if (action.equals("reset") && args.length >= 3) {
            Player target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("§cJugador no encontrado.");
                return;
            }
            
            plugin.getExperienceService().setXP(target, 0);
            sender.sendMessage("§a✓ XP reseteado para " + target.getName());
            
        } else {
            sender.sendMessage("§cUso incorrecto. /avo xp para ver ayuda.");
        }
    }
    
    /**
     * Comando para ver nivel de jugadores
     * /avo nivel [jugador]
     */
    private void cmdNivel(CommandSender sender, String[] args) {
        Player target;
        
        if (args.length >= 2) {
            if (!sender.hasPermission("avo.admin")) {
                sender.sendMessage("§cNo tienes permisos para ver el nivel de otros.");
                return;
            }
            target = plugin.getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cJugador no encontrado.");
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cDebes especificar un jugador desde consola.");
                return;
            }
            target = (Player) sender;
        }
        
        int xp = plugin.getExperienceService().getXP(target);
        int nivel = plugin.getExperienceService().getLevel(target);
        int xpForNext = plugin.getExperienceService().getXPForNextLevel(target);
        int xpCurrent = plugin.getExperienceService().getXPForLevel(nivel);
        int xpProgress = xp - xpCurrent;
        int xpNeeded = xpForNext - xpCurrent;
        
        double progress = (double) xpProgress / xpNeeded;
        int bars = (int) (progress * 20);
        StringBuilder progressBar = new StringBuilder("§a");
        for (int i = 0; i < 20; i++) {
            if (i < bars) {
                progressBar.append("█");
            } else {
                progressBar.append("§7█");
            }
        }
        
        sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("§6Nivel de §f" + target.getName());
        sender.sendMessage("");
        sender.sendMessage("§7Nivel Actual: §b§l" + nivel);
        sender.sendMessage("§7XP Total: §e" + xp);
        sender.sendMessage("");
        sender.sendMessage("§7Progreso al nivel " + (nivel + 1) + ":");
        sender.sendMessage(progressBar.toString());
        sender.sendMessage("§7" + xpProgress + " / " + xpNeeded + " XP §8(§e" + String.format("%.1f", progress * 100) + "%§8)");
        sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
}

