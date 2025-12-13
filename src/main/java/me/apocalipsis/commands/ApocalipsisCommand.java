package me.apocalipsis.commands;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
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
            showHelp(sender, 1);
            return true;
        }
        
        // Check for help pages
        if (args[0].equalsIgnoreCase("help") && args.length > 1) {
            try {
                int page = Integer.parseInt(args[1]);
                showHelp(sender, page);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cUso: /avo help <página>");
            }
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
            case "evasion":
            case "evasiones":
                cmdEvasion(sender, args);
                break;
            case "escanear":
                cmdEscanear(sender);
                break;
            case "blockinfo":
            case "bloque":
                cmdBlockInfo(sender);
                break;
            case "blockstats":
                cmdBlockStats(sender, args);
                break;
            case "protecciones":
                cmdProtecciones(sender);
                break;
            case "eco":
                cmdEco(sender, args);
                break;
            case "eco_sombras":
                cmdEcoSombras(sender, args);
                break;
            case "evento3":
            case "susurro":
                cmdEvento3(sender, args);
                break;
            case "xp":
            case "experience":
                cmdXP(sender, args);
                break;
            case "nivel":
            case "level":
                cmdNivel(sender, args);
                break;
            case "rewards":
                cmdRewards(sender, args);
                break;
            case "autotest":
            case "test-event":
                cmdAutoTest(sender, args);
                break;
            case "habilidad":
            case "habilidades":
            case "skill":
            case "skills":
                cmdHabilidades(sender, args);
                break;
            case "mochila":
            case "backpack":
            case "bag":
                cmdMochila(sender, args);
                break;
            case "echest":
            case "enderchest":
            case "ec":
                cmdEnderChest(sender);
                break;
            case "skillstats":
                cmdSkillStats(sender, args);
                break;
            case "menu":
                cmdMenu(sender);
                break;
            case "newrank":
                cmdNewRank(sender, args);
                break;
            case "setpermrank":
                cmdSetPermRank(sender, args);
                break;
            case "removepermrank":
                cmdRemovePermRank(sender, args);
                break;
            case "listpermranks":
                cmdListPermRanks(sender);
                break;
            default:
                sender.sendMessage("§cSubcomando desconocido. Usa /avo para ver ayuda.");
                break;
        }

        return true;
    }

    /**
     * Muestra ayuda paginada con diseño mejorado
     */
    private void showHelp(CommandSender sender, int page) {
        final int CMDS_PER_PAGE = 12;
        
        // Build complete command list
        String[][] commands = {
            // Page 1: Desastres y Protecciones
            {"§6▸ Control de Desastres", ""},
            {"  §e/avo start", "§7Inicia o reanuda desastre"},
            {"  §e/avo stop", "§7Detiene desastre actual"},
            {"  §e/avo force <id>", "§7Fuerza desastre específico"},
            {"  §e/avo skip", "§7Salta al siguiente estado"},
            {"  §e/avo preparacion <min>", "§7Inicia preparación"},
            {"  §e/avo time <set|add> <min>", "§7Modifica tiempo"},
            {"§6▸ Protecciones", ""},
            {"  §e/avo escanear", "§7Escanea protecciones cercanas"},
            {"  §e/avo protecciones", "§7Guía de protecciones"},
            {"§6▸ Sistema", ""},
            {"  §e/avo tps", "§7Ver TPS y rendimiento"},
            
            // Page 2: Experiencia y Misiones
            {"§6▸ Experiencia y Progresión", ""},
            {"  §e/avo xp", "§7Ver tu XP y progreso"},
            {"  §e/avo nivel", "§7Ver tu nivel actual"},
            {"  §e/avo xp <get|add|set>", "§7Gestión XP (Admin)"},
            {"§6▸ Misiones", ""},
            {"  §e/avo newday", "§7Nuevo día + misiones"},
            {"  §e/avo endday", "§7Termina día actual"},
            {"  §e/avo status [jugador]", "§7Misiones activas"},
            {"  §e/avo setxp <jugador> <xp>", "§7Ajusta XP/rango"},
            {"  §e/avo mission <...>", "§7Gestión misiones"},
            {"", ""},
            {"", ""},
            
            // Page 3: Eventos
            {"§6▸ Evento Eco de Brasas", ""},
            {"  §e/avo eco start", "§7Inicia evento"},
            {"  §e/avo eco stop", "§7Detiene evento"},
            {"  §e/avo eco fase <1-3>", "§7Fuerza fase"},
            {"  §e/avo eco next", "§7Siguiente fase"},
            {"  §e/avo eco info", "§7Info detallada"},
            {"  §e/avo eco pulso <...>", "§7Ajusta pulso"},
            {"  §e/avo eco ancla <1-3>", "§7Completa ancla"},
            {"§6▸ Evento Eco de Sombras", ""},
            {"  §e/avo eco_sombras start", "§7Inicia evento"},
            {"  §e/avo eco_sombras stop", "§7Detiene evento"},
            {"  §e/avo eco_sombras fase <1-6>", "§7Fuerza acto"},
            {"  §e/avo eco_sombras info", "§7Info evento"},
            
            // Page 4: Admin
            {"§6▸ Sistema Avanzado", ""},
            {"  §e/avo stats", "§7Estadísticas servidor"},
            {"  §e/avo cooldown", "§7Estado cooldown"},
            {"  §e/avo backup", "§7Backup manual"},
            {"  §e/avo reload", "§7Recarga config"},
            {"  §e/avo test", "§7Toggle modo test"},
            {"  §e/avo debug <...>", "§7Control logs"},
            {"  §e/avo test-alert <jugador>", "§7Test notificaciones"},
            {"  §e/avo admin <...>", "§7Gestión excepciones"},
            {"  §e/avo evasion <...>", "§7Gestión evasiones"},
            {"", ""},
            {"", ""},
            {"", ""}
        };
        
        int totalPages = (commands.length + CMDS_PER_PAGE - 1) / CMDS_PER_PAGE;
        page = Math.max(1, Math.min(page, totalPages));
        
        int startIdx = (page - 1) * CMDS_PER_PAGE;
        int endIdx = Math.min(startIdx + CMDS_PER_PAGE, commands.length);
        
        // Header
        sender.sendMessage("§8§m                                                  ");
        sender.sendMessage("§c§lAPOCALIPSIS §8| §7Comandos §8(§e" + page + "§7/§e" + totalPages + "§8)");
        sender.sendMessage("§8§m                                                  ");
        
        // Commands
        for (int i = startIdx; i < endIdx; i++) {
            String[] cmd = commands[i];
            if (cmd[0].isEmpty()) {
                continue; // Skip empty lines
            }
            if (cmd[1].isEmpty()) {
                sender.sendMessage(cmd[0]); // Category header
            } else {
                sender.sendMessage(cmd[0] + " §8- " + cmd[1]);
            }
        }
        
        // Footer
        sender.sendMessage("§8§m                                                  ");
        if (page < totalPages) {
            sender.sendMessage("§7Usa §e/avo help " + (page + 1) + "§7 para ver más comandos");
        }
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
        
        // [DEPRECADO] endDay ya no se usa
        // Los castigos ahora se aplican automáticamente cuando se llama a /avo newday
        // que verifica misiones pendientes del día anterior
        sender.sendMessage("§7§oNota: Los castigos se aplican automáticamente en /avo newday");
        
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
     * /avo setxp <jugador> <xp|rango> - Ajusta XP de un jugador manualmente
     * Soporta números directos o nombres de rangos (NOVATO, EXPLORADOR, etc.)
     */
    private void cmdSetXp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUso: /avo setxp <jugador> <xp|rango>");
            sender.sendMessage("§7Ejemplos:");
            sender.sendMessage("§e  /avo setxp Steve 1000 §7- Asigna 1000 XP");
            sender.sendMessage("§e  /avo setxp Steve VETERANO §7- XP del rango Veterano");
            sender.sendMessage("§7Rangos disponibles: §eNOVATO, EXPLORADOR, SOBREVIVIENTE, VETERANO, LEYENDA, MAESTRO, TITAN, ABSOLUTO");
            return;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJugador no encontrado: " + args[1]);
            return;
        }

        int xp;
        String inputValue = args[2].toUpperCase();
        
        // Intentar parsear como rango primero
        try {
            me.apocalipsis.missions.MissionRank rank = me.apocalipsis.missions.MissionRank.valueOf(inputValue);
            xp = plugin.getRankService().getXpForRank(rank);
            sender.sendMessage("§7Asignando XP del rango §e" + rank.name() + "§7: §e" + xp + " XP");
        } catch (IllegalArgumentException e) {
            // No es un rango, intentar parsear como número
            try {
                xp = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                sender.sendMessage("§cValor inválido: " + args[2]);
                sender.sendMessage("§7Usa un número o un rango (NOVATO, EXPLORADOR, etc.)");
                return;
            }
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

    // ==================== BLOCK OWNERSHIP COMMANDS ====================
    
    /**
     * /avo blockinfo - Muestra info del bloque que miras
     */
    private void cmdBlockInfo(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return;
        }
        
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        Player player = (Player) sender;
        
        // Obtener bloque al que mira el jugador
        Block targetBlock = player.getTargetBlockExact(10);
        if (targetBlock == null || targetBlock.getType().isAir()) {
            player.sendMessage("§c¡Mira hacia un bloque para ver su información!");
            return;
        }
        
        var tracker = plugin.getBlockTracker();
        var info = tracker.getBlockInfo(targetBlock);
        
        player.sendMessage("§8┌─────────────────────────────────────┐");
        player.sendMessage("§6│ §e§l🔍 INFO DE BLOQUE                   §6│");
        player.sendMessage("§8├─────────────────────────────────────┤");
        player.sendMessage("§6│ §7Tipo: §f" + targetBlock.getType().name());
        player.sendMessage("§6│ §7Ubicación: §f" + 
            targetBlock.getX() + ", " + targetBlock.getY() + ", " + targetBlock.getZ());
        player.sendMessage("§8├─────────────────────────────────────┤");
        
        if (info == null) {
            player.sendMessage("§6│ §7Estado: §eSin protección              §6│");
            player.sendMessage("§6│ §7Este bloque puede ser destruido     §6│");
            player.sendMessage("§6│ §7por desastres.                      §6│");
        } else {
            player.sendMessage("§6│ §7Dueño: §a" + info.ownerName());
            player.sendMessage("§6│ §7Colocado: §e" + info.getPlacedAgo());
            player.sendMessage("§6│ §7Última conexión: §e" + info.getLastSeenAgo());
            player.sendMessage("§6│ §7Estado dueño: " + (info.ownerActive() ? "§a✓ ACTIVO" : "§c✗ INACTIVO"));
            player.sendMessage("§8├─────────────────────────────────────┤");
            if (info.ownerActive()) {
                player.sendMessage("§6│ §a✓ PROTEGIDO §8- §7No se destruirá    §6│");
            } else {
                player.sendMessage("§6│ §e⚠ EXPIRADO §8- §7Puede destruirse   §6│");
                player.sendMessage("§6│ §7(Dueño inactivo >" + tracker.getInactiveDaysToExpire() + " días)    §6│");
            }
        }
        player.sendMessage("§8└─────────────────────────────────────┘");
        
        // Partículas en el bloque
        player.getWorld().spawnParticle(
            info != null && info.ownerActive() ? org.bukkit.Particle.HAPPY_VILLAGER : org.bukkit.Particle.SMOKE,
            targetBlock.getLocation().add(0.5, 1.0, 0.5), 
            10, 0.3, 0.2, 0.3, 0.01
        );
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
    }
    
    /**
     * /avo blockstats [jugador] - Estadísticas de bloques
     */
    private void cmdBlockStats(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }
        
        var tracker = plugin.getBlockTracker();
        
        // Si hay argumento, mostrar stats de jugador específico
        if (args.length >= 2) {
            String playerName = args[1];
            org.bukkit.OfflinePlayer target = plugin.getServer().getOfflinePlayer(playerName);
            
            if (!target.hasPlayedBefore()) {
                sender.sendMessage("§cJugador no encontrado: " + playerName);
                return;
            }
            
            var stats = tracker.getPlayerStats(target.getUniqueId());
            
            sender.sendMessage("§8┌─────────────────────────────────────┐");
            sender.sendMessage("§6│ §e§l📊 STATS DE BLOQUES - " + stats.playerName());
            sender.sendMessage("§8├─────────────────────────────────────┤");
            sender.sendMessage("§6│ §7Bloques protegidos: §a" + stats.blockCount());
            sender.sendMessage("§6│ §7Última conexión: §e" + 
                (stats.daysSinceLastSeen() == 0 ? "hoy" : "hace " + stats.daysSinceLastSeen() + " días"));
            sender.sendMessage("§6│ §7Estado: " + (stats.isActive() ? "§a✓ ACTIVO" : "§c✗ INACTIVO"));
            if (!stats.isActive()) {
                sender.sendMessage("§6│ §7Sus bloques §cPUEDEN§7 ser destruidos §6│");
            }
            sender.sendMessage("§8└─────────────────────────────────────┘");
            return;
        }
        
        // Sin argumento: mostrar stats globales + top
        var globalStats = tracker.getStats();
        
        sender.sendMessage("§8┌─────────────────────────────────────┐");
        sender.sendMessage("§6│ §e§l📊 ESTADÍSTICAS BLOCK TRACKER       §6│");
        sender.sendMessage("§8├─────────────────────────────────────┤");
        sender.sendMessage("§6│ §7Bloques trackeados: §a" + globalStats.get("tracked_blocks"));
        sender.sendMessage("§6│ §7Jugadores registrados: §e" + globalStats.get("tracked_players"));
        sender.sendMessage("§6│ §7Jugadores inactivos: §c" + globalStats.get("inactive_players"));
        sender.sendMessage("§6│ §7Umbral de inactividad: §e" + globalStats.get("inactive_days_threshold") + " días");
        sender.sendMessage("§6│ §7Backup habilitado: " + 
            (Boolean.TRUE.equals(globalStats.get("backup_enabled")) ? "§a✓ Sí" : "§c✗ No"));
        
        Object lastBackup = globalStats.get("last_backup");
        if (lastBackup instanceof Long ts && ts > 0) {
            long hoursAgo = (System.currentTimeMillis() - ts) / (1000L * 60L * 60L);
            sender.sendMessage("§6│ §7Último backup: §e hace " + hoursAgo + " horas");
        } else {
            sender.sendMessage("§6│ §7Último backup: §7Nunca");
        }
        
        sender.sendMessage("§8├─────────────────────────────────────┤");
        sender.sendMessage("§6│ §e§lTOP 5 JUGADORES:                    §6│");
        
        var top = tracker.getTopBlockOwners(5);
        int rank = 1;
        for (var entry : top.entrySet()) {
            String name = plugin.getServer().getOfflinePlayer(entry.getKey()).getName();
            sender.sendMessage(String.format("§6│ §7%d. §f%-15s §8- §a%d bloques", 
                rank++, name != null ? name : "???", entry.getValue()));
        }
        
        sender.sendMessage("§8└─────────────────────────────────────┘");
        sender.sendMessage("§7Usa §e/avo blockstats <jugador>§7 para ver stats específicas");
        sender.sendMessage("§7Usa §e/avo blockinfo§7 mirando un bloque para ver su dueño");
    }
    
    // ==================== ESCANEO DE PROTECCIONES ====================

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

    /**
     * /avo eco_sombras <subcomando> - Gestión del evento El Eco de las Sombras Largas
     */
    private void cmdEcoSombras(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§8§l=== ECO DE LAS SOMBRAS LARGAS - COMANDOS ===");
            sender.sendMessage("§e/avo eco_sombras start §7- Inicia el evento");
            sender.sendMessage("§e/avo eco_sombras stop §7- Detiene el evento");
            sender.sendMessage("§e/avo eco_sombras fase <1-6> §7- Fuerza acto específico");
            sender.sendMessage("§e/avo eco_sombras next §7- Avanza al siguiente acto");
            sender.sendMessage("§e/avo eco_sombras info §7- Muestra información detallada");
            sender.sendMessage("§e/avo eco_sombras ancla <1-5> §7- Sella ancla específica");
            sender.sendMessage("§e/avo eco_sombras nucleo spawn §7- Fuerza spawn del núcleo");
            return;
        }
        
        String subCmd = args[1].toLowerCase();
        
        // Obtener instancia del evento desde EventController
        me.apocalipsis.events.EcoSombrasEvent ecoSombras = null;
        if (eventController.hasActiveEvent() && 
            eventController.getActiveEvent() instanceof me.apocalipsis.events.EcoSombrasEvent) {
            ecoSombras = (me.apocalipsis.events.EcoSombrasEvent) eventController.getActiveEvent();
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
                    String eventoActivo = eventController.getActiveEvent().getEventId();
                    sender.sendMessage("§cYa hay un evento activo: §f" + eventoActivo);
                    sender.sendMessage("§7Usa §e/avo eco" + (eventoActivo.equals("eco_brasas") ? "" : "_sombras") + " stop §7primero.");
                    return;
                }
                
                // Verificar SAFE_MODE
                if (stateManager.isSafeModeActive()) {
                    sender.sendMessage("§cNo se puede iniciar en SAFE_MODE (TPS bajo).");
                    return;
                }
                
                // Obtener configuración de jugadores mínimos desde el evento
                me.apocalipsis.events.EventBase eventoBase = eventController.getEvent("eco_sombras");
                int jugadoresMinimos = 3; // Valor por defecto
                
                if (eventoBase instanceof me.apocalipsis.events.EcoSombrasEvent) {
                    me.apocalipsis.events.EcoSombrasEvent evento = (me.apocalipsis.events.EcoSombrasEvent) eventoBase;
                    jugadoresMinimos = evento.getJugadoresMinimos();
                }
                
                // Verificar jugadores mínimos
                int jugadoresOnline = plugin.getServer().getOnlinePlayers().size();
                if (jugadoresOnline < jugadoresMinimos) {
                    sender.sendMessage("§cSe requieren mínimo " + jugadoresMinimos + " jugadores online para iniciar el evento.");
                    sender.sendMessage("§7Jugadores actuales: §e" + jugadoresOnline);
                    return;
                }
                
                // Iniciar Eco de Sombras usando EventController
                if (eventController.startEvent("eco_sombras")) {
                    sender.sendMessage("§a✓ Evento §8§lEco de las Sombras Largas §ainiciado");
                    sender.sendMessage("§7Un eco desconocido se registra en el mundo...");
                    plugin.getLogger().info(String.format("[EcoSombras] Iniciado por %s", sender.getName()));
                } else {
                    sender.sendMessage("§cNo se pudo iniciar el evento. Verifica la consola.");
                }
                break;
                
            case "stop":
            case "detener":
                if (ecoSombras == null) {
                    sender.sendMessage("§cEl evento Eco de Sombras no está activo.");
                    return;
                }
                
                eventController.stopActiveEvent();
                sender.sendMessage("§7✓ Evento §8Eco de Sombras §7detenido y limpiado");
                
                plugin.getLogger().info(String.format("[EcoSombras] Detenido por %s", sender.getName()));
                break;
                
            case "fase":
            case "acto":
                if (ecoSombras == null) {
                    sender.sendMessage("§cEl evento Eco de Sombras no está activo.");
                    return;
                }
                
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo eco_sombras fase <0-6>");
                    sender.sendMessage("§7  0 = Activación");
                    sender.sendMessage("§7  1 = Manchas de Sombra");
                    sender.sendMessage("§7  2 = Sombras Largas");
                    sender.sendMessage("§7  3 = Núcleo de Sombra");
                    sender.sendMessage("§7  4 = Anclas del Mundo");
                    sender.sendMessage("§7  5 = Ritual");
                    sender.sendMessage("§7  6 = Cliffhanger");
                    return;
                }
                
                try {
                    int faseNum = Integer.parseInt(args[2]);
                    if (faseNum < 0 || faseNum > 6) {
                        sender.sendMessage("§cFase inválida. Usa un número entre 0 y 6.");
                        return;
                    }
                    
                    ecoSombras.forzarActo(faseNum);
                    sender.sendMessage("§a✓ Forzada transición al acto: §e" + ecoSombras.getActoActual());
                    plugin.getLogger().info(String.format("[EcoSombras] %s forzó acto: %d (%s)", 
                        sender.getName(), faseNum, ecoSombras.getActoActual()));
                    
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cNúmero de fase inválido: " + args[2]);
                }
                break;
                
            case "next":
            case "siguiente":
                if (ecoSombras == null) {
                    sender.sendMessage("§cEl evento Eco de Sombras no está activo.");
                    return;
                }
                
                me.apocalipsis.events.EcoSombrasEvent.Acto actoAnterior = ecoSombras.getActoActual();
                ecoSombras.avanzarActo();
                
                sender.sendMessage("§a✓ Avanzado al siguiente acto");
                sender.sendMessage("§7De: §e" + actoAnterior + " §7→ §e" + ecoSombras.getActoActual());
                plugin.getLogger().info(String.format("[EcoSombras] %s avanzó de %s a %s", 
                    sender.getName(), actoAnterior, ecoSombras.getActoActual()));
                break;
                
            case "info":
            case "status":
                if (ecoSombras == null) {
                    sender.sendMessage("§8§l=== ECO DE SOMBRAS - INFO ===");
                    sender.sendMessage("§7Estado: §cInactivo");
                    sender.sendMessage("§7Usa §e/avo eco_sombras start §7para iniciarlo.");
                    return;
                }
                
                sender.sendMessage("§8§l=== ECO DE SOMBRAS - INFO ===");
                sender.sendMessage("§7Estado: §aActivo");
                sender.sendMessage("§7Acto actual: §e" + ecoSombras.getActoActual());
                sender.sendMessage("§7Participantes: §e" + plugin.getServer().getOnlinePlayers().size());
                
                // Info específica por acto
                String actoActual = ecoSombras.getActoActual().toString();
                if (actoActual.equals("SOMBRAS_LARGAS") || actoActual.equals("NUCLEO") || actoActual.equals("ANCLAS")) {
                    sender.sendMessage("§7Sombras eliminadas: §e" + ecoSombras.getSombrasLargasMuertas());
                }
                
                if (actoActual.equals("ANCLAS")) {
                    sender.sendMessage("§7Anclas selladas: §e" + ecoSombras.getAnclasSelladas() + "/5");
                }
                
                if (actoActual.equals("RITUAL")) {
                    sender.sendMessage("§7Oleada actual: §e" + ecoSombras.getOleadaActual() + "/3");
                }
                
                sender.sendMessage("§8§m                        ");
                break;
                
            case "ancla":
                if (ecoSombras == null) {
                    sender.sendMessage("§cEl evento Eco de Sombras no está activo.");
                    return;
                }
                
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo eco_sombras ancla <1-5>");
                    sender.sendMessage("§7Sella forzadamente el ancla especificada");
                    return;
                }
                
                try {
                    int anclaId = Integer.parseInt(args[2]) - 1; // 0-indexed internamente
                    
                    if (anclaId < 0 || anclaId >= 5) {
                        sender.sendMessage("§cID de ancla inválido. Usa 1-5.");
                        return;
                    }
                    
                    Player ejecutor = (sender instanceof Player) ? (Player) sender : null;
                    if (ejecutor != null) {
                        ecoSombras.sellarAncla(anclaId, ejecutor);
                        sender.sendMessage("§a✓ Ancla #" + (anclaId + 1) + " sellada forzadamente");
                        plugin.getLogger().info(String.format("[EcoSombras] %s selló ancla #%d", sender.getName(), anclaId + 1));
                    } else {
                        sender.sendMessage("§cDebes ser un jugador para ejecutar este comando.");
                    }
                    
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cID de ancla inválido: " + args[2]);
                }
                break;
                
            case "nucleo":
                if (ecoSombras == null) {
                    sender.sendMessage("§cEl evento Eco de Sombras no está activo.");
                    return;
                }
                
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo eco_sombras nucleo <spawn|teleport|damage>");
                    return;
                }
                
                String nucleoAction = args[2].toLowerCase();
                switch (nucleoAction) {
                    case "spawn":
                        sender.sendMessage("§a✓ Forzando spawn del Núcleo...");
                        // TODO: Implementar forzado de spawn
                        break;
                    case "teleport":
                    case "tp":
                        sender.sendMessage("§a✓ Forzando teleporte del Núcleo...");
                        // TODO: Implementar forzado de TP
                        break;
                    case "damage":
                    case "danio":
                        if (args.length < 4) {
                            sender.sendMessage("§cUso: /avo eco_sombras nucleo damage <cantidad>");
                            return;
                        }
                        try {
                            int damage = Integer.parseInt(args[3]);
                            sender.sendMessage("§a✓ Aplicando " + damage + " de daño al Núcleo...");
                            // TODO: Implementar daño forzado
                        } catch (NumberFormatException e) {
                            sender.sendMessage("§cCantidad de daño inválida: " + args[3]);
                        }
                        break;
                    default:
                        sender.sendMessage("§cAcción desconocida: " + nucleoAction);
                        break;
                }
                break;
                
            default:
                sender.sendMessage("§cSubcomando desconocido: §f" + subCmd);
                sender.sendMessage("§7Usa §e/avo eco_sombras §7para ver comandos disponibles.");
                break;
        }
    }
    
    /**
     * Comandos para el Evento 3: El Susurro en la Piedra Rota
     */
    private void cmdEvento3(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§8§l=== EL SUSURRO EN LA PIEDRA ROTA - COMANDOS ===");
            sender.sendMessage("§e/avo evento3 start §7- Inicia el evento");
            sender.sendMessage("§e/avo evento3 stop §7- Detiene el evento");
            sender.sendMessage("§e/avo evento3 acto <1-4> §7- Fuerza acto específico");
            sender.sendMessage("§e/avo evento3 next §7- Avanza al siguiente acto");
            sender.sendMessage("§e/avo evento3 info §7- Muestra información detallada");
            sender.sendMessage("§e/avo evento3 fragmento spawn §7- Fuerza spawn fragmento");
            sender.sendMessage("§e/avo evento3 grieta spawn §7- Fuerza spawn grieta");
            return;
        }
        
        String subCmd = args[1].toLowerCase();
        
        // Obtener instancia del evento
        me.apocalipsis.events.SusurroPiedraRotaEvent evento3 = null;
        if (eventController.hasActiveEvent() && 
            eventController.getActiveEvent() instanceof me.apocalipsis.events.SusurroPiedraRotaEvent) {
            evento3 = (me.apocalipsis.events.SusurroPiedraRotaEvent) eventController.getActiveEvent();
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
                    String eventoActivo = eventController.getActiveEvent().getEventId();
                    sender.sendMessage("§cYa hay un evento activo: §f" + eventoActivo);
                    sender.sendMessage("§7Usa §e/avo " + eventoActivo.replace("_", " ") + " stop §7primero.");
                    return;
                }
                
                // Verificar SAFE_MODE
                if (stateManager.isSafeModeActive()) {
                    sender.sendMessage("§cNo se puede iniciar en SAFE_MODE (TPS bajo).");
                    return;
                }
                
                // Verificar jugadores mínimos (1-6)
                int jugadoresOnline = plugin.getServer().getOnlinePlayers().size();
                if (jugadoresOnline < 1) {
                    sender.sendMessage("§cSe requiere al menos 1 jugador online para iniciar el evento.");
                    return;
                }
                
                if (jugadoresOnline > 6) {
                    sender.sendMessage("§e⚠ Este evento está diseñado para 1-6 jugadores.");
                    sender.sendMessage("§7Jugadores actuales: §e" + jugadoresOnline);
                    sender.sendMessage("§7Iniciando de todas formas...");
                }
                
                // Iniciar evento
                if (eventController.startEvent("susurro_piedra_rota")) {
                    sender.sendMessage("§a✓ Evento §5§lEl Susurro en la Piedra Rota §ainiciado");
                    sender.sendMessage("§7La piedra está susurrando algo...");
                    plugin.getLogger().info(String.format("[SusurroPiedraRota] Iniciado por %s", sender.getName()));
                } else {
                    sender.sendMessage("§cNo se pudo iniciar el evento. Verifica la consola.");
                }
                break;
                
            case "stop":
            case "detener":
                if (evento3 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                eventController.stopActiveEvent();
                sender.sendMessage("§7✓ Evento §5El Susurro en la Piedra Rota §7detenido");
                plugin.getLogger().info(String.format("[SusurroPiedraRota] Detenido por %s", sender.getName()));
                break;
                
            case "acto":
            case "fase":
                if (evento3 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo evento3 acto <1-4>");
                    sender.sendMessage("§7  1 = Piedra Despierta");
                    sender.sendMessage("§7  2 = Piedra Quiebra (Grieta)");
                    sender.sendMessage("§7  3 = El Núcleo de Forma");
                    sender.sendMessage("§7  4 = Segundo Susurro");
                    return;
                }
                
                try {
                    int actoNum = Integer.parseInt(args[2]);
                    if (actoNum < 1 || actoNum > 4) {
                        sender.sendMessage("§cActo inválido. Usa un número entre 1 y 4.");
                        return;
                    }
                    
                    evento3.forzarActo(actoNum);
                    sender.sendMessage("§a✓ Forzada transición al acto: §e" + evento3.getActoActual());
                    plugin.getLogger().info(String.format("[SusurroPiedraRota] %s forzó acto: %d", 
                        sender.getName(), actoNum));
                    
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cNúmero de acto inválido: " + args[2]);
                }
                break;
                
            case "next":
            case "siguiente":
                if (evento3 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                me.apocalipsis.events.SusurroPiedraRotaEvent.Acto actoAnterior = evento3.getActoActual();
                evento3.avanzarActo();
                
                sender.sendMessage("§a✓ Avanzado al siguiente acto");
                sender.sendMessage("§7De: §e" + actoAnterior + " §7→ §e" + evento3.getActoActual());
                plugin.getLogger().info(String.format("[SusurroPiedraRota] %s avanzó al siguiente acto", 
                    sender.getName()));
                break;
                
            case "info":
            case "status":
                if (evento3 == null) {
                    sender.sendMessage("§8§l=== SUSURRO PIEDRA ROTA - INFO ===");
                    sender.sendMessage("§7Estado: §cInactivo");
                    sender.sendMessage("§7Usa §e/avo evento3 start §7para iniciarlo.");
                    return;
                }
                
                sender.sendMessage(evento3.getInfo());
                break;
                
            case "fragmento":
                if (evento3 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo evento3 fragmento spawn");
                    return;
                }
                
                if (args[2].equalsIgnoreCase("spawn")) {
                    evento3.forzarSpawnFragmento();
                    sender.sendMessage("§a✓ Fragmento adicional spawneado");
                    plugin.getLogger().info(String.format("[SusurroPiedraRota] %s spawneó fragmento", sender.getName()));
                }
                break;
                
            case "grieta":
                if (evento3 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo evento3 grieta spawn");
                    return;
                }
                
                if (args[2].equalsIgnoreCase("spawn")) {
                    evento3.forzarSpawnGrieta();
                    sender.sendMessage("§a✓ Grieta de Forma spawneada");
                    plugin.getLogger().info(String.format("[SusurroPiedraRota] %s spawneó grieta", sender.getName()));
                }
                break;
                
            default:
                sender.sendMessage("§cSubcomando desconocido: §f" + subCmd);
                sender.sendMessage("§7Usa §e/avo evento3 §7para ver comandos disponibles.");
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
        if (args.length < 2) {
            // Cualquier jugador puede ver su propio XP
            if (sender instanceof Player player) {
                showPlayerXPInfo(player, player);
                return;
            }
            sender.sendMessage("§e=== Comandos de XP ===");
            sender.sendMessage("§7/avo xp §f- Ver tu XP y progreso");
            sender.sendMessage("§7/avo xp top [cantidad] §f- Leaderboard del día");
            sender.sendMessage("§7/avo xp stats §f- Ver estadísticas detalladas");
            sender.sendMessage("§7§o(Admin) /avo xp get|add|set|reset|horafeliz");
            return;
        }
        
        String action = args[1].toLowerCase();
        
        // Comandos públicos
        switch (action) {
            case "top" -> {
                cmdXPTop(sender, args);
                return;
            }
            case "stats" -> {
                if (sender instanceof Player player) {
                    showPlayerXPStats(player);
                } else {
                    sender.sendMessage("§cEste comando solo puede usarse como jugador.");
                }
                return;
            }
        }
        
        // Comandos admin
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos para ese subcomando.");
            return;
        }
        
        switch (action) {
            case "horafeliz" -> cmdHoraFeliz(sender, args);
            case "get" -> {
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo xp get <jugador>");
                    return;
                }
                Player target = plugin.getServer().getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage("§cJugador no encontrado.");
                    return;
                }
                showPlayerXPInfo(sender, target);
            }
            case "add", "set" -> {
                if (args.length < 4) {
                    sender.sendMessage("§cUso: /avo xp " + action + " <jugador> <cantidad>");
                    return;
                }
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
            }
            case "reset" -> {
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo xp reset <jugador>");
                    return;
                }
                Player target = plugin.getServer().getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage("§cJugador no encontrado.");
                    return;
                }
                plugin.getExperienceService().setXP(target, 0);
                sender.sendMessage("§a✓ XP reseteado para " + target.getName());
            }
            default -> sender.sendMessage("§cSubcomando desconocido. /avo xp para ver ayuda.");
        }
    }
    
    /**
     * Muestra información de XP de un jugador
     */
    private void showPlayerXPInfo(CommandSender sender, Player target) {
        int xp = plugin.getExperienceService().getXP(target);
        int nivel = plugin.getExperienceService().getLevel(target);
        int xpForNext = plugin.getExperienceService().getXPForNextLevel(target);
        
        sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("§6⭐ XP de §f" + target.getName());
        sender.sendMessage("§7Nivel: §b" + nivel);
        sender.sendMessage("§7XP Total: §e" + xp);
        sender.sendMessage("§7XP para siguiente nivel: §e" + xpForNext);
        
        // Mostrar multiplicadores activos
        if (sender instanceof Player player && player.equals(target)) {
            var xpManager = plugin.getExperienceListener().getXPManager();
            if (xpManager != null) {
                double mult = xpManager.calculateCurrentMultiplier(player);
                sender.sendMessage("");
                sender.sendMessage("§7Multiplicador actual: §a" + String.format("%.2f", mult) + "x");
                if (xpManager.isHoraFelizActiva()) {
                    sender.sendMessage("§c§l🎉 ¡HORA FELIZ ACTIVA!");
                }
            }
        }
        sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
    
    /**
     * Muestra estadísticas detalladas de XP
     */
    private void showPlayerXPStats(Player player) {
        var xpManager = plugin.getExperienceListener().getXPManager();
        if (xpManager == null) {
            player.sendMessage("§cSistema de XP no disponible.");
            return;
        }
        
        var stats = xpManager.getPlayerStats(player);
        
        player.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("§6📊 Tus Estadísticas de XP");
        player.sendMessage("");
        player.sendMessage("§7🔥 Racha de login: §e" + stats.get("streak") + " días");
        player.sendMessage("§7📈 XP ganado hoy: §e" + stats.get("xp_hoy"));
        player.sendMessage("§7⚡ Acciones hoy: §e" + stats.get("acciones_hoy"));
        player.sendMessage("§7🏆 Mejor fuente: §e" + stats.get("mejor_fuente"));
        player.sendMessage("§7✨ Multiplicador actual: §a" + String.format("%.2f", (Double) stats.get("multiplicador_actual")) + "x");
        player.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
    
    /**
     * Muestra leaderboard de XP del día
     */
    private void cmdXPTop(CommandSender sender, String[] args) {
        var xpManager = plugin.getExperienceListener().getXPManager();
        if (xpManager == null) {
            sender.sendMessage("§cSistema de XP no disponible.");
            return;
        }
        
        int limit = 10;
        if (args.length >= 3) {
            try {
                limit = Math.min(Math.max(Integer.parseInt(args[2]), 1), 50);
            } catch (NumberFormatException ignored) {}
        }
        
        var top = xpManager.getTopPlayers(limit);
        
        sender.sendMessage("");
        sender.sendMessage("§6§l🏆 LEADERBOARD XP DEL DÍA 🏆");
        sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        if (top.isEmpty()) {
            sender.sendMessage("§7Nadie ha ganado XP hoy todavía.");
        } else {
            int rank = 1;
            for (var entry : top) {
                String medal = switch (rank) {
                    case 1 -> "§6🥇";
                    case 2 -> "§f🥈";
                    case 3 -> "§c🥉";
                    default -> "§7#" + rank;
                };
                sender.sendMessage(medal + " §f" + entry.getKey() + " §7- §e" + entry.getValue() + " XP");
                rank++;
            }
        }
        
        sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("");
    }
    
    /**
     * Comando para activar/desactivar hora feliz
     */
    private void cmdHoraFeliz(CommandSender sender, String[] args) {
        var xpManager = plugin.getExperienceListener().getXPManager();
        if (xpManager == null) {
            sender.sendMessage("§cSistema de XP no disponible.");
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§e=== Hora Feliz ===");
            sender.sendMessage("§7/avo xp horafeliz start [minutos] §f- Iniciar (default 60 min)");
            sender.sendMessage("§7/avo xp horafeliz stop §f- Detener");
            sender.sendMessage("§7Estado: " + (xpManager.isHoraFelizActiva() ? "§a¡ACTIVA!" : "§7Inactiva"));
            return;
        }
        
        String subAction = args[2].toLowerCase();
        
        if (subAction.equals("start")) {
            if (xpManager.isHoraFelizActiva()) {
                sender.sendMessage("§cLa Hora Feliz ya está activa.");
                return;
            }
            int duracion = 60;
            if (args.length >= 4) {
                try {
                    duracion = Math.min(Math.max(Integer.parseInt(args[3]), 5), 480);
                } catch (NumberFormatException ignored) {}
            }
            xpManager.activarHoraFeliz(duracion, false);
            sender.sendMessage("§a✓ Hora Feliz activada por " + duracion + " minutos.");
        } else if (subAction.equals("stop")) {
            if (!xpManager.isHoraFelizActiva()) {
                sender.sendMessage("§cLa Hora Feliz no está activa.");
                return;
            }
            xpManager.desactivarHoraFeliz();
            sender.sendMessage("§a✓ Hora Feliz desactivada.");
        } else {
            sender.sendMessage("§cSubcomando inválido. Usa start o stop.");
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
    
    /**
     * Comando para gestionar evasiones de desastres
     * /avo evasion <check|clear> [jugador|all]
     */
    private void cmdEvasion(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            sender.sendMessage("§c§lGestión de Evasiones de Desastres");
            sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            sender.sendMessage("§e/avo evasion check <jugador> §7- Ver evasiones de un jugador");
            sender.sendMessage("§e/avo evasion clear <jugador|all> §7- Limpiar evasiones");
            sender.sendMessage("§e/avo evasion stats §7- Ver estadísticas globales");
            sender.sendMessage("§e/avo evasion history <jugador> §7- Ver historial de evasiones");
            sender.sendMessage("§e/avo evasion reduce <jugador> [cantidad] §7- Reducir evasiones");
            sender.sendMessage("§e/avo evasion info §7- Ver configuración actual");
            sender.sendMessage("§e/avo evasion reload §7- Recargar configuración");
            sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            return;
        }
        
        String subCmd = args[1].toLowerCase();
        
        switch (subCmd) {
            case "check":
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo evasion check <jugador>");
                    return;
                }
                
                Player target = plugin.getServer().getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage("§cJugador no encontrado.");
                    return;
                }
                
                String info = plugin.getDisasterEvasionTracker().getPlayerEvasionInfo(target.getUniqueId());
                sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                sender.sendMessage("§6Evasiones de §f" + target.getName());
                sender.sendMessage("");
                sender.sendMessage(info);
                sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                break;
                
            case "clear":
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo evasion clear <jugador|all>");
                    return;
                }
                
                if (args[2].equalsIgnoreCase("all")) {
                    plugin.getDisasterEvasionTracker().clearAllEvasions();
                    sender.sendMessage("§a✓ Todas las evasiones y castigos han sido limpiados.");
                    messageBus.broadcast("§e⚠ Todas las evasiones de desastres han sido perdonadas", "evasion_clear");
                } else {
                    Player targetClear = plugin.getServer().getPlayer(args[2]);
                    if (targetClear == null) {
                        sender.sendMessage("§cJugador no encontrado.");
                        return;
                    }
                    
                    plugin.getDisasterEvasionTracker().clearPlayerEvasions(targetClear.getUniqueId());
                    sender.sendMessage("§a✓ Evasiones y castigos de §f" + targetClear.getName() + " §alimpiados.");
                    
                    if (targetClear.isOnline()) {
                        targetClear.sendMessage("§a✓ Tus evasiones y castigos pendientes han sido perdonados.");
                    }
                }
                break;
            
            case "stats":
                showEvasionStats(sender);
                break;
            
            case "history":
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo evasion history <jugador>");
                    return;
                }
                showEvasionHistory(sender, args[2]);
                break;
            
            case "reduce":
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo evasion reduce <jugador> [cantidad]");
                    return;
                }
                int cantidad = 1;
                if (args.length >= 4) {
                    try {
                        cantidad = Integer.parseInt(args[3]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cCantidad inválida.");
                        return;
                    }
                }
                reducePlayerEvasions(sender, args[2], cantidad);
                break;
            
            case "info":
                showEvasionConfig(sender);
                break;
            
            case "reload":
                plugin.getConfigManager().reloadEvasionesConfig();
                plugin.getDisasterEvasionTracker().reloadConfig();
                sender.sendMessage("§a✓ Configuración de evasiones recargada.");
                break;
                
            default:
                sender.sendMessage("§cSubcomando desconocido. Usa: check, clear, stats, history, reduce, info o reload");
                break;
        }
    }
    
    /**
     * Muestra estadísticas globales de evasiones
     */
    private void showEvasionStats(CommandSender sender) {
        sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("§c§lEstadísticas Globales de Evasiones");
        sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        var stats = plugin.getDisasterEvasionTracker().getGlobalStats();
        sender.sendMessage("§7Total de jugadores con evasiones: §c" + stats.getOrDefault("jugadores_con_evasiones", 0));
        sender.sendMessage("§7Evasiones totales acumuladas: §c" + stats.getOrDefault("evasiones_totales", 0));
        sender.sendMessage("§7Castigos pendientes: §e" + stats.getOrDefault("castigos_pendientes", 0));
        sender.sendMessage("§7Nivel promedio de evasión: §6" + String.format("%.1f", stats.getOrDefault("nivel_promedio", 0.0)));
        sender.sendMessage("");
        sender.sendMessage("§7Jugadores online trackeados: §a" + stats.getOrDefault("jugadores_online_trackeados", 0));
        sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
    
    /**
     * Muestra historial de evasiones de un jugador
     */
    private void showEvasionHistory(CommandSender sender, String playerName) {
        Player target = plugin.getServer().getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§cJugador no encontrado.");
            return;
        }
        
        java.util.List<String> history = plugin.getDisasterEvasionTracker().getPlayerHistory(target.getUniqueId());
        
        sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("§c§lHistorial de Evasiones - " + target.getName());
        sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        if (history.isEmpty()) {
            sender.sendMessage("§aSin historial de evasiones");
        } else {
            int count = 0;
            for (int i = history.size() - 1; i >= 0 && count < 10; i--, count++) {
                String entry = history.get(i);
                String[] parts = entry.split("\\|");
                if (parts.length >= 5) {
                    long timestamp = Long.parseLong(parts[0]);
                    String tiempo = parts[2];
                    int psLoss = Integer.parseInt(parts[3]);
                    int nivel = Integer.parseInt(parts[4]);
                    String tipo = parts.length >= 6 ? parts[5] : "normal";
                    
                    java.util.Date date = new java.util.Date(timestamp);
                    String fecha = new java.text.SimpleDateFormat("dd/MM HH:mm").format(date);
                    
                    String tipoIcon = tipo.equals("late") ? "§e⏰" : "§c⚡";
                    sender.sendMessage(String.format("§7%s %s §7Nivel §c%d §7- PS: §c-%d §7(§e%s§7)", 
                        fecha, tipoIcon, nivel, psLoss, tiempo));
                }
            }
        }
        sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
    
    /**
     * Reduce evasiones de un jugador
     */
    private void reducePlayerEvasions(CommandSender sender, String playerName, int cantidad) {
        Player target = plugin.getServer().getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§cJugador no encontrado.");
            return;
        }
        
        int reduced = plugin.getDisasterEvasionTracker().reduceEvasions(target.getUniqueId(), cantidad);
        if (reduced > 0) {
            sender.sendMessage("§a✓ Reducidas §e" + reduced + " §aevasión(es) de §f" + target.getName());
            target.sendMessage("§a✓ Se te han reducido §e" + reduced + " §aevasión(es) por buen comportamiento.");
        } else {
            sender.sendMessage("§7" + target.getName() + " no tenía evasiones para reducir.");
        }
    }
    
    /**
     * Muestra la configuración actual de evasiones
     */
    private void showEvasionConfig(CommandSender sender) {
        sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("§c§lConfiguración de Evasiones");
        sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        
        var cfg = plugin.getConfigManager();
        
        sender.sendMessage("§6Tiempos:");
        sender.sendMessage("§7  • Tiempo mínimo: §e" + cfg.getEvasionMinTiempoSegundos() + "s");
        sender.sendMessage("§7  • Ventana de gracia: §e" + cfg.getEvasionVentanaGraciaSegundos() + "s");
        sender.sendMessage("§7  • Late-join threshold: §e" + cfg.getEvasionLateJoinThresholdSegundos() + "s");
        sender.sendMessage("§7  • Late-join tiempo mín: §e" + cfg.getEvasionLateJoinMinTiempoSegundos() + "s");
        sender.sendMessage("§7  • Reset automático: §e" + cfg.getEvasionResetAutomaticoHoras() + "h");
        
        sender.sendMessage("");
        sender.sendMessage("§6Penalizaciones:");
        sender.sendMessage("§7  • Tipo cálculo: §e" + cfg.getEvasionPenalizacionTipoCalculo());
        for (int i = 1; i <= 4; i++) {
            sender.sendMessage(String.format("§7  • Nivel %d: §c-%d%% PS §7+ §e%s", 
                i, 
                cfg.getEvasionPenalizacionPsPorcentaje(i),
                cfg.getEvasionPenalizacionCastigoFisico(i)));
        }
        
        sender.sendMessage("");
        sender.sendMessage("§6Features:");
        sender.sendMessage("§7  • Castigos físicos: " + (cfg.isEvasionCastigosFisicosEnabled() ? "§a✓" : "§c✗"));
        sender.sendMessage("§7  • Notificaciones admin: " + (cfg.isEvasionNotificacionesAdminsEnabled() ? "§a✓" : "§c✗"));
        sender.sendMessage("§7  • Avisos proactivos: " + (cfg.isEvasionNotificacionesJugadorEnabled() ? "§a✓" : "§c✗"));
        sender.sendMessage("§7  • Reducción por desastres: " + (cfg.isEvasionReduccionPorDesastresEnabled() ? "§a✓" : "§c✗"));
        sender.sendMessage("§7  • Reducción por tiempo: " + (cfg.isEvasionReduccionPorTiempoEnabled() ? "§a✓" : "§c✗"));
        sender.sendMessage("§7  • Estadísticas: " + (cfg.isEvasionEstadisticasEnabled() ? "§a✓" : "§c✗"));
        
        sender.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
    
    /**
     * Comando /avo rewards - Debug y gestión de sistema de recompensas
     * Subcomandos:
     *  - /avo rewards check <jugador> - Verifica estado de recompensas
     *  - /avo rewards force <jugador> <rango> - Fuerza entrega de recompensas
     *  - /avo rewards reset <jugador> - Resetea recompensas recibidas
     */
    private void cmdRewards(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permiso.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§e════════════════════════════════════");
            sender.sendMessage("§6§lAVO REWARDS - Sistema de Recompensas");
            sender.sendMessage("§e════════════════════════════════════");
            sender.sendMessage("§f/avo rewards check <jugador>");
            sender.sendMessage("  §7Verifica estado de recompensas del jugador");
            sender.sendMessage("§f/avo rewards force <jugador> <rango>");
            sender.sendMessage("  §7Fuerza entrega de recompensas de un rango");
            sender.sendMessage("  §7Rangos: EXPLORADOR, SOBREVIVIENTE, VETERANO,");
            sender.sendMessage("  §7        LEYENDA, MAESTRO, TITAN, ABSOLUTO");
            sender.sendMessage("§f/avo rewards reset <jugador>");
            sender.sendMessage("  §7Resetea todas las recompensas recibidas");
            sender.sendMessage("§e════════════════════════════════════");
            return;
        }
        
        String subCmd = args[1].toLowerCase();
        
        switch (subCmd) {
            case "check":
                // /avo rewards check <jugador>
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo rewards check <jugador>");
                    return;
                }
                Player target = plugin.getServer().getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage("§cJugador no encontrado.");
                    return;
                }
                
                me.apocalipsis.missions.MissionRank rank = plugin.getRankService().getRank(target);
                int ps = plugin.getMissionService().getPlayerPs(target);
                int level = plugin.getExperienceService().getLevel(target);
                
                sender.sendMessage("§e════════════════════════════════════");
                sender.sendMessage("§6§lEstado de Recompensas: §f" + target.getName());
                sender.sendMessage("§e════════════════════════════════════");
                sender.sendMessage("§7Rango actual: §e" + rank.getDisplayName() + " §8(" + rank.name() + ")");
                sender.sendMessage("§7PS: §e" + ps);
                sender.sendMessage("§7Nivel: §e" + level);
                sender.sendMessage("");
                sender.sendMessage("§6Recompensas de rangos:");
                
                me.apocalipsis.experience.RewardService rewards = plugin.getRewardService();
                if (rewards != null) {
                    for (me.apocalipsis.missions.MissionRank r : me.apocalipsis.missions.MissionRank.values()) {
                        if (r == me.apocalipsis.missions.MissionRank.NOVATO) continue;
                        
                        boolean received = rewards.hasReceivedRewards(target, r);
                        String status = received ? "§a✓ RECIBIDO" : "§c✗ PENDIENTE";
                        sender.sendMessage("  " + status + " §7- §f" + r.getDisplayName());
                    }
                } else {
                    sender.sendMessage("§c✗ RewardService no disponible");
                }
                sender.sendMessage("§e════════════════════════════════════");
                break;
                
            case "force":
                // /avo rewards force <jugador> <rango>
                if (args.length < 4) {
                    sender.sendMessage("§cUso: /avo rewards force <jugador> <rango>");
                    sender.sendMessage("§7Rangos válidos: EXPLORADOR, SOBREVIVIENTE, VETERANO,");
                    sender.sendMessage("§7                LEYENDA, MAESTRO, TITAN, ABSOLUTO");
                    return;
                }
                Player targetForce = plugin.getServer().getPlayer(args[2]);
                if (targetForce == null) {
                    sender.sendMessage("§cJugador no encontrado.");
                    return;
                }
                
                try {
                    me.apocalipsis.missions.MissionRank forceRank = me.apocalipsis.missions.MissionRank.valueOf(args[3].toUpperCase());
                    
                    if (forceRank == me.apocalipsis.missions.MissionRank.NOVATO) {
                        sender.sendMessage("§cEl rango NOVATO no tiene recompensas.");
                        return;
                    }
                    
                    if (plugin.getRewardService() != null) {
                        plugin.getRewardService().forceDeliverRewards(targetForce, forceRank);
                        sender.sendMessage("§a✓ Recompensas de §6" + forceRank.getDisplayName() + " §aforzadas para §f" + targetForce.getName());
                        sender.sendMessage("§7Revisa la consola para ver los comandos ejecutados.");
                        
                        if (targetForce.isOnline()) {
                            targetForce.sendMessage("§6§l★ §eHas recibido las recompensas de §6" + forceRank.getDisplayName());
                        }
                    } else {
                        sender.sendMessage("§c✗ RewardService no disponible");
                    }
                } catch (IllegalArgumentException e) {
                    sender.sendMessage("§cRango inválido: §f" + args[3]);
                    sender.sendMessage("§7Usa: EXPLORADOR, SOBREVIVIENTE, VETERANO, LEYENDA, MAESTRO, TITAN, ABSOLUTO");
                }
                break;
                
            case "reset":
                // /avo rewards reset <jugador>
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo rewards reset <jugador>");
                    return;
                }
                Player targetReset = plugin.getServer().getPlayer(args[2]);
                if (targetReset == null) {
                    sender.sendMessage("§cJugador no encontrado.");
                    return;
                }
                
                if (plugin.getRewardService() != null) {
                    plugin.getRewardService().resetPlayerRewards(targetReset.getUniqueId());
                    sender.sendMessage("§a✓ Recompensas reseteadas para §f" + targetReset.getName());
                    sender.sendMessage("§7El jugador podrá recibir todas las recompensas nuevamente.");
                    
                    if (targetReset.isOnline()) {
                        targetReset.sendMessage("§e⚠ Tus recompensas de rangos han sido reseteadas por un administrador.");
                    }
                } else {
                    sender.sendMessage("§c✗ RewardService no disponible");
                }
                break;
                
            default:
                sender.sendMessage("§cSubcomando desconocido.");
                sender.sendMessage("§7Usa: §f/avo rewards §7para ver ayuda.");
                break;
        }
    }
    
    /**
     * /avo autotest <subcomando> - Sistema de autotesting con bots
     */
    private void cmdAutoTest(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§8§l═══════════════════════════════════════════════════");
            sender.sendMessage("§6§l  SISTEMA DE AUTOTESTING - COMANDOS");
            sender.sendMessage("§8§l═══════════════════════════════════════════════════");
            sender.sendMessage("");
            sender.sendMessage("§e/avo autotest start <evento> §7- Inicia autotesting");
            sender.sendMessage("§e/avo autotest stop §7- Detiene autotesting");
            sender.sendMessage("§e/avo autotest run <escenario> §7- Ejecuta escenario");
            sender.sendMessage("§e/avo autotest suite <evento> §7- Ejecuta suite completa");
            sender.sendMessage("§e/avo autotest bots §7- Lista bots activos");
            sender.sendMessage("§e/avo autotest report §7- Genera reporte");
            sender.sendMessage("§e/avo autotest clear §7- Limpia resultados");
            sender.sendMessage("");
            sender.sendMessage("§7Eventos: §feco_brasas, eco_sombras");
            sender.sendMessage("§8§l═══════════════════════════════════════════════════");
            return;
        }
        
        String subCmd = args[1].toLowerCase();
        
        // Obtener sistema de autotesting
        var autoTestSystem = plugin.getAutoTestSystem();
        if (autoTestSystem == null) {
            sender.sendMessage("§c✗ Sistema de autotesting no disponible");
            return;
        }
        
        switch (subCmd) {
            case "start":
                // /avo autotest start <evento>
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo autotest start <evento>");
                    sender.sendMessage("§7Eventos: eco_brasas, eco_sombras, evento3, susurro_piedra_rota");
                    return;
                }
                
                String eventId = args[2].toLowerCase();
                // Normalizar nombres de eventos
                if (eventId.equals("evento3")) {
                    eventId = "susurro_piedra_rota";
                }
                
                if (!eventId.equals("eco_brasas") && !eventId.equals("eco_sombras") && !eventId.equals("susurro_piedra_rota")) {
                    sender.sendMessage("§cEvento inválido: §f" + eventId);
                    sender.sendMessage("§7Usa: eco_brasas, eco_sombras, evento3 o susurro_piedra_rota");
                    return;
                }
                
                // Obtener ubicación de spawn
                Location spawnLoc;
                if (sender instanceof Player) {
                    spawnLoc = ((Player) sender).getLocation();
                } else {
                    spawnLoc = plugin.getServer().getWorlds().get(0).getSpawnLocation();
                }
                
                autoTestSystem.startAutoTesting(eventId, spawnLoc);
                
                sender.sendMessage("§a✓ Autotesting iniciado para §e" + eventId);
                sender.sendMessage("§7Bots creados en: §f" + 
                    String.format("%.1f, %.1f, %.1f", spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ()));
                sender.sendMessage("§7Usa §e/avo autotest bots §7para ver lista de bots");
                break;
                
            case "stop":
                if (!autoTestSystem.isTestingActive()) {
                    sender.sendMessage("§cNo hay autotesting activo.");
                    return;
                }
                
                autoTestSystem.stopAutoTesting();
                sender.sendMessage("§a✓ Autotesting detenido");
                sender.sendMessage("§7Usa §e/avo autotest report §7para ver resultados");
                break;
                
            case "run":
                // /avo autotest run <escenario>
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo autotest run <escenario>");
                    sender.sendMessage("§7Escenarios disponibles:");
                    sender.sendMessage("§f  basic, grieta, ancla, guardian, death, afk, partial");
                    return;
                }
                
                if (!autoTestSystem.isTestingActive()) {
                    sender.sendMessage("§cDebes iniciar autotesting primero: §e/avo autotest start <evento>");
                    return;
                }
                
                String scenarioName = args[2].toLowerCase();
                me.apocalipsis.events.testing.scenarios.TestScenario scenario = null;
                
                // Crear escenario basado en nombre
                switch (scenarioName) {
                    case "basic":
                        scenario = new me.apocalipsis.events.testing.scenarios.BasicParticipationScenario();
                        break;
                    case "grieta":
                        scenario = new me.apocalipsis.events.testing.scenarios.GrietaClosingScenario();
                        break;
                    case "ancla":
                        scenario = new me.apocalipsis.events.testing.scenarios.AnclaCompletionScenario();
                        break;
                    case "guardian":
                        scenario = new me.apocalipsis.events.testing.scenarios.GuardianFightScenario();
                        break;
                    case "death":
                        scenario = new me.apocalipsis.events.testing.scenarios.PlayerDeathScenario();
                        break;
                    case "afk":
                        scenario = new me.apocalipsis.events.testing.scenarios.AFKPlayerScenario();
                        break;
                    case "partial":
                        scenario = new me.apocalipsis.events.testing.scenarios.PartialParticipationScenario();
                        break;
                    default:
                        sender.sendMessage("§cEscenario desconocido: §f" + scenarioName);
                        return;
                }
                
                autoTestSystem.runScenario(scenario);
                sender.sendMessage("§a✓ Ejecutando escenario: §e" + scenario.getName());
                sender.sendMessage("§7Duración estimada: §f" + (scenario.getDurationTicks() / 20) + "s");
                sender.sendMessage("§7Descripción: §f" + scenario.getDescription());
                break;
                
            case "suite":
                // /avo autotest suite <evento>
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo autotest suite <evento>");
                    return;
                }
                
                if (!autoTestSystem.isTestingActive()) {
                    sender.sendMessage("§cDebes iniciar autotesting primero: §e/avo autotest start <evento>");
                    return;
                }
                
                String suiteEventId = args[2].toLowerCase();
                autoTestSystem.runTestSuite(suiteEventId);
                
                sender.sendMessage("§a✓ Ejecutando suite completa para §e" + suiteEventId);
                sender.sendMessage("§7Esto puede tardar varios minutos...");
                sender.sendMessage("§7El reporte se generará automáticamente al finalizar");
                break;
                
            case "bots":
                if (!autoTestSystem.isTestingActive()) {
                    sender.sendMessage("§cNo hay autotesting activo.");
                    return;
                }
                
                var bots = autoTestSystem.getActiveBots();
                sender.sendMessage("§8§l═══════════════════════════════════════════════════");
                sender.sendMessage("§6§l  BOTS ACTIVOS (" + bots.size() + ")");
                sender.sendMessage("§8§l═══════════════════════════════════════════════════");
                
                for (var bot : bots) {
                    sender.sendMessage(bot.getStatsReport());
                }
                
                sender.sendMessage("§8§l═══════════════════════════════════════════════════");
                break;
                
            case "report":
                var results = autoTestSystem.getTestResults();
                if (results.isEmpty()) {
                    sender.sendMessage("§cNo hay resultados de tests disponibles.");
                    sender.sendMessage("§7Ejecuta algunos escenarios primero.");
                    return;
                }
                
                String fullReport = autoTestSystem.generateTestReport();
                
                // Enviar reporte línea por línea
                for (String line : fullReport.split("\n")) {
                    sender.sendMessage(line);
                }
                break;
                
            case "quick":
                if (autoTestSystem.getTestResults().isEmpty()) {
                    sender.sendMessage("§cNo hay resultados de tests disponibles.");
                    return;
                }
                
                String quickReport = autoTestSystem.generateQuickReport();
                sender.sendMessage(quickReport);
                break;
                
            case "clear":
                autoTestSystem.clearResults();
                sender.sendMessage("§a✓ Resultados de tests limpiados");
                break;
                
            default:
                sender.sendMessage("§cSubcomando desconocido: §f" + subCmd);
                sender.sendMessage("§7Usa §e/avo autotest §7para ver ayuda");
                break;
        }
    }
    
    // ==================== COMANDOS DE HABILIDADES ====================
    
    /**
     * Comando principal de habilidades
     * /avo habilidades [subcomando]
     */
    private void cmdHabilidades(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return;
        }
        
        if (args.length < 2) {
            // Abrir menú principal
            plugin.getSkillTreeGUI().openMainMenu(player);
            return;
        }
        
        String subCmd = args[1].toLowerCase();
        
        switch (subCmd) {
            case "menu":
            case "arbol":
            case "tree":
                plugin.getSkillTreeGUI().openMainMenu(player);
                break;
                
            case "info":
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo habilidades info <id>");
                    sender.sendMessage("§7Ejemplo: /avo habilidades info paso_ligero");
                    return;
                }
                cmdHabilidadesInfo(player, args[2]);
                break;
                
            case "mis":
            case "my":
            case "list":
                cmdHabilidadesMis(player);
                break;
                
            case "toggle":
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo habilidades toggle <id>");
                    return;
                }
                cmdHabilidadesToggle(player, args[2]);
                break;
                
            case "toggles":
                cmdHabilidadesToggles(player);
                break;
                
            case "comprar":
            case "buy":
            case "unlock":
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo habilidades comprar <id>");
                    return;
                }
                cmdHabilidadesComprar(player, args[2]);
                break;
                
            case "admin":
                if (!player.hasPermission("avo.admin")) {
                    sender.sendMessage("§cNo tienes permisos.");
                    return;
                }
                cmdHabilidadesAdmin(player, args);
                break;
            
            // === HABILIDADES DE DETECCIÓN ===
            case "rastro":
            case "oro":
            case "rastro_oro":
                plugin.getSkillService().activateRastroOro(player);
                break;
                
            case "detector":
            case "spawner":
            case "spawners":
            case "detector_spawners":
                plugin.getSkillService().activateDetectorSpawners(player);
                break;
                
            case "xray":
            case "diamantes":
            case "diamante":
            case "xray_diamantes":
                plugin.getSkillService().activateXrayDiamantes(player);
                break;
            
            // === HABILIDADES DE INVOCACIÓN ===
            case "lobo":
            case "wolf":
            case "lobos":
                plugin.getSkillService().invocarLobo(player);
                break;
                
            case "gato":
            case "cat":
            case "guardian":
                plugin.getSkillService().invocarGato(player);
                break;
                
            case "allay":
            case "recolector":
                plugin.getSkillService().invocarAllay(player);
                break;
                
            case "abejas":
            case "bees":
                plugin.getSkillService().invocarAbejas(player);
                break;
                
            case "golem":
            case "iron":
                plugin.getSkillService().invocarGolem(player);
                break;
                
            case "vex":
            case "vengador":
                // Obtener target mirando
                org.bukkit.entity.LivingEntity target = getTargetEntity(player, 30);
                plugin.getSkillService().invocarVex(player, target);
                break;
                
            case "warden":
            case "guardian_oscuro":
                plugin.getSkillService().invocarWarden(player);
                break;
                
            case "despawn":
            case "dismiss":
                plugin.getSkillService().despawnEntidades(player.getUniqueId());
                player.sendMessage("§7Tus invocaciones han sido despedidas.");
                break;
            
            // === SINERGIAS AVANZADAS ===
            case "omnipresente":
            case "vision":
            case "xray_total":
                plugin.getSkillService().activateOmnipresente(player);
                break;
                
            case "avatar":
            case "caos":
            case "avatar_caos":
                plugin.getSkillService().activateAvatarCaos(player);
                break;
                
            default:
                sender.sendMessage("§e=== Comandos de Habilidades ===");
                sender.sendMessage("§7/avo habilidades §f- Abre el árbol de habilidades");
                sender.sendMessage("§7/avo habilidades info <id> §f- Info de una habilidad");
                sender.sendMessage("§7/avo habilidades mis §f- Ver tus habilidades");
                sender.sendMessage("§7/avo habilidades toggle <id> §f- Activar/desactivar");
                sender.sendMessage("§7/avo habilidades toggles §f- Ver estado de toggles");
                sender.sendMessage("§7/avo habilidades comprar <id> §f- Comprar habilidad");
                sender.sendMessage("§e--- Detección (con cooldown) ---");
                sender.sendMessage("§7/avo habilidades rastro §f- Detectar minerales (60s cd)");
                sender.sendMessage("§7/avo habilidades detector §f- Detectar spawners (90s cd)");
                sender.sendMessage("§7/avo habilidades diamantes §f- Detectar diamantes (120s cd)");
                sender.sendMessage("§e--- Invocaciones ---");
                sender.sendMessage("§7/avo habilidades lobo §f- Lobo compañero (20min cd)");
                sender.sendMessage("§7/avo habilidades gato §f- Gato guardián (25min cd)");
                sender.sendMessage("§7/avo habilidades allay §f- Allay recolector (15min cd)");
                sender.sendMessage("§7/avo habilidades abejas §f- Abejas protectoras (10min cd)");
                sender.sendMessage("§7/avo habilidades golem §f- Gólem de hierro (10min cd)");
                sender.sendMessage("§7/avo habilidades vex §f- Vex vengadores (3min cd)");
                sender.sendMessage("§7/avo habilidades warden §f- Warden temporal (30min cd)");
                sender.sendMessage("§7/avo habilidades despawn §f- Despedir invocaciones");
                sender.sendMessage("§e--- Sinergias ---");
                sender.sendMessage("§7/avo habilidades omnipresente §f- Ver todo (2min cd)");
                sender.sendMessage("§7/avo habilidades avatar §f- Avatar del Caos (1h cd)");
                if (player.hasPermission("avo.admin")) {
                    sender.sendMessage("§c/avo habilidades admin §f- Comandos admin");
                }
                break;
        }
    }
    
    /**
     * Obtiene la entidad que el jugador está mirando
     */
    private org.bukkit.entity.LivingEntity getTargetEntity(Player player, int maxDistance) {
        // Usar ray trace
        org.bukkit.util.RayTraceResult result = player.getWorld().rayTraceEntities(
            player.getEyeLocation(),
            player.getLocation().getDirection(),
            maxDistance,
            entity -> entity instanceof org.bukkit.entity.LivingEntity && entity != player
        );
        
        if (result != null && result.getHitEntity() instanceof org.bukkit.entity.LivingEntity living) {
            return living;
        }
        return null;
    }
    
    private void cmdHabilidadesInfo(Player player, String skillId) {
        me.apocalipsis.skills.Skill skill = me.apocalipsis.skills.Skill.fromId(skillId);
        if (skill == null) {
            player.sendMessage("§cHabilidad no encontrada: §f" + skillId);
            player.sendMessage("§7Usa §e/avo habilidades §7para ver el menú.");
            return;
        }
        
        var skillService = plugin.getSkillService();
        boolean owned = skillService.hasSkill(player, skill);
        boolean meetsReqs = skillService.meetsRequirements(player, skill);
        
        player.sendMessage("§6§l═══════ §e" + skill.getDisplayName() + " §6§l═══════");
        player.sendMessage("§7" + skill.getDescription());
        player.sendMessage("");
        player.sendMessage("§7Rama: " + skill.getBranch().getDisplayName());
        player.sendMessage("§7Tier: " + skill.getTier().getDisplayName());
        player.sendMessage("§7Rareza: " + skill.getRarity().getDisplayName());
        player.sendMessage("§7Costo: §e" + skill.getBaseCost() + " XP");
        
        if (skill.isToggleable()) {
            player.sendMessage("§7Toggle: §a✓ Se puede activar/desactivar");
        }
        
        if (skill.getRequirements().length > 0) {
            player.sendMessage("");
            player.sendMessage("§7Requisitos:");
            for (String reqId : skill.getRequirements()) {
                var req = me.apocalipsis.skills.Skill.fromId(reqId);
                if (req != null) {
                    boolean hasReq = skillService.hasSkill(player, req);
                    String check = hasReq ? "§a✓" : "§c✗";
                    player.sendMessage("  " + check + " §7" + req.getDisplayName());
                }
            }
        }
        
        player.sendMessage("");
        if (owned) {
            player.sendMessage("§a§l✓ DESBLOQUEADA");
            if (skill.isToggleable()) {
                boolean enabled = skillService.isSkillEnabled(player, skill);
                player.sendMessage("§7Estado: " + (enabled ? "§aActivada" : "§cDesactivada"));
            }
        } else if (meetsReqs) {
            player.sendMessage("§e§l⬡ DISPONIBLE PARA COMPRAR");
        } else {
            player.sendMessage("§c§l✗ BLOQUEADA - Faltan requisitos");
        }
        player.sendMessage("§6§l═════════════════════════════════");
    }
    
    private void cmdHabilidadesMis(Player player) {
        var skillService = plugin.getSkillService();
        var skills = skillService.getUnlockedSkills(player);
        
        if (skills.isEmpty()) {
            player.sendMessage("§cNo tienes habilidades desbloqueadas.");
            player.sendMessage("§7Usa §e/avo habilidades §7para ver el árbol.");
            return;
        }
        
        player.sendMessage("§6§l═══════ §eTUS HABILIDADES §6§l═══════");
        
        for (var skill : skills) {
            String toggle = "";
            if (skill.isToggleable()) {
                boolean enabled = skillService.isSkillEnabled(player, skill);
                toggle = enabled ? " §a[ON]" : " §c[OFF]";
            }
            player.sendMessage("§7• " + skill.getColoredName() + toggle);
        }
        
        player.sendMessage("");
        player.sendMessage("§7Total: §b" + skills.size() + "§7/§b" + skillService.getTotalSkillCount());
        player.sendMessage("§7XP gastada: §e" + skillService.getXpGastada(player));
        player.sendMessage("§6§l════════════════════════════════");
    }
    
    private void cmdHabilidadesToggle(Player player, String skillId) {
        var skill = me.apocalipsis.skills.Skill.fromId(skillId);
        if (skill == null) {
            player.sendMessage("§cHabilidad no encontrada: §f" + skillId);
            return;
        }
        
        plugin.getSkillService().toggleSkill(player, skill);
    }
    
    private void cmdHabilidadesToggles(Player player) {
        var skillService = plugin.getSkillService();
        var toggleables = skillService.getToggleableSkills(player);
        
        if (toggleables.isEmpty()) {
            player.sendMessage("§cNo tienes habilidades toggleables desbloqueadas.");
            return;
        }
        
        player.sendMessage("§6§l═══════ §eTOGGLES §6§l═══════");
        
        for (var skill : toggleables) {
            boolean enabled = skillService.isSkillEnabled(player, skill);
            String status = enabled ? "§a[ON]" : "§c[OFF]";
            player.sendMessage(status + " §7" + skill.getDisplayName());
            player.sendMessage("  §8/avo habilidades toggle " + skill.getId());
        }
        
        player.sendMessage("§6§l══════════════════════════");
    }
    
    private void cmdHabilidadesComprar(Player player, String skillId) {
        var skill = me.apocalipsis.skills.Skill.fromId(skillId);
        if (skill == null) {
            player.sendMessage("§cHabilidad no encontrada: §f" + skillId);
            return;
        }
        
        // Abrir menú de confirmación (incluir la rama de la skill)
        plugin.getSkillTreeGUI().openConfirmMenu(player, skill, skill.getBranch());
    }
    
    private void cmdHabilidadesAdmin(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§e=== Admin de Habilidades ===");
            player.sendMessage("§7/avo habilidades admin give <jugador> <skill>");
            player.sendMessage("§7/avo habilidades admin remove <jugador> <skill>");
            player.sendMessage("§7/avo habilidades admin reset <jugador>");
            player.sendMessage("§7/avo habilidades admin list");
            return;
        }
        
        String action = args[2].toLowerCase();
        var skillService = plugin.getSkillService();
        
        switch (action) {
            case "give":
                if (args.length < 5) {
                    player.sendMessage("§cUso: /avo habilidades admin give <jugador> <skill>");
                    return;
                }
                Player targetGive = plugin.getServer().getPlayer(args[3]);
                if (targetGive == null) {
                    player.sendMessage("§cJugador no encontrado.");
                    return;
                }
                var skillGive = me.apocalipsis.skills.Skill.fromId(args[4]);
                if (skillGive == null) {
                    player.sendMessage("§cHabilidad no encontrada.");
                    return;
                }
                skillService.giveSkill(targetGive, skillGive);
                player.sendMessage("§a✓ Habilidad §e" + skillGive.getDisplayName() + " §adada a §f" + targetGive.getName());
                targetGive.sendMessage("§a✓ Has recibido la habilidad: " + skillGive.getColoredName());
                break;
                
            case "remove":
                if (args.length < 5) {
                    player.sendMessage("§cUso: /avo habilidades admin remove <jugador> <skill>");
                    return;
                }
                Player targetRemove = plugin.getServer().getPlayer(args[3]);
                if (targetRemove == null) {
                    player.sendMessage("§cJugador no encontrado.");
                    return;
                }
                var skillRemove = me.apocalipsis.skills.Skill.fromId(args[4]);
                if (skillRemove == null) {
                    player.sendMessage("§cHabilidad no encontrada.");
                    return;
                }
                skillService.removeSkill(targetRemove, skillRemove);
                player.sendMessage("§a✓ Habilidad §e" + skillRemove.getDisplayName() + " §aquitada a §f" + targetRemove.getName());
                break;
                
            case "reset":
                if (args.length < 4) {
                    player.sendMessage("§cUso: /avo habilidades admin reset <jugador>");
                    return;
                }
                Player targetReset = plugin.getServer().getPlayer(args[3]);
                if (targetReset == null) {
                    player.sendMessage("§cJugador no encontrado.");
                    return;
                }
                skillService.resetPlayer(targetReset);
                player.sendMessage("§a✓ Habilidades de §f" + targetReset.getName() + " §areseteadas.");
                break;
                
            case "list":
                player.sendMessage("§e=== Lista de Habilidades ===");
                for (var skill : me.apocalipsis.skills.Skill.values()) {
                    player.sendMessage("§7• §e" + skill.getId() + " §f- " + skill.getDisplayName());
                }
                break;
                
            default:
                player.sendMessage("§cSubcomando admin desconocido.");
                break;
        }
    }
    
    // ==================== COMANDOS DE MOCHILA ====================
    
    /**
     * Comando de mochila con subcomandos de moderación
     * /avo mochila - Abre tu mochila
     * /avo mochila ver <jugador> - Ver mochila de otro (requiere permiso)
     * /avo mochila lista - Lista mochilas con contenido (requiere permiso)
     * /avo mochila vaciar <jugador> - Vacía mochila de otro (requiere permiso)
     */
    private void cmdMochila(CommandSender sender, String[] args) {
        // Sin argumentos: abrir mochila propia
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
                return;
            }
            plugin.getBackpackService().openBackpack(player);
            return;
        }
        
        String subCmd = args[1].toLowerCase();
        
        switch (subCmd) {
            case "ver", "view", "inspect" -> {
                if (!sender.hasPermission("apocalipsis.mochila.mod")) {
                    sender.sendMessage("§c✗ No tienes permiso para ver mochilas ajenas.");
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo mochila ver <jugador>");
                    return;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
                    return;
                }
                
                String targetName = args[2];
                Player target = Bukkit.getPlayer(targetName);
                
                if (target != null) {
                    plugin.getBackpackService().openBackpackAsAdmin(player, target.getUniqueId(), target.getName());
                } else {
                    // Buscar jugador offline
                    @SuppressWarnings("deprecation")
                    org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
                    if (offline.hasPlayedBefore()) {
                        plugin.getBackpackService().openBackpackAsAdmin(player, offline.getUniqueId(), 
                            offline.getName() != null ? offline.getName() : targetName);
                    } else {
                        sender.sendMessage("§c✗ Jugador no encontrado: " + targetName);
                    }
                }
            }
            
            case "lista", "list" -> {
                if (!sender.hasPermission("apocalipsis.mochila.mod")) {
                    sender.sendMessage("§c✗ No tienes permiso para listar mochilas.");
                    return;
                }
                
                java.util.List<String> mochilas = plugin.getBackpackService().getBackpackList();
                if (mochilas.isEmpty()) {
                    sender.sendMessage("§7No hay mochilas con contenido.");
                } else {
                    sender.sendMessage("§6§l✦ §eMochilas con contenido (" + mochilas.size() + "):");
                    for (String name : mochilas) {
                        sender.sendMessage("  §7• §f" + name);
                    }
                }
            }
            
            case "vaciar", "clear", "empty" -> {
                if (!sender.hasPermission("apocalipsis.mochila.admin")) {
                    sender.sendMessage("§c✗ No tienes permiso para vaciar mochilas.");
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo mochila vaciar <jugador>");
                    return;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
                    return;
                }
                
                String targetName = args[2];
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
                
                if (offline.hasPlayedBefore() || offline.isOnline()) {
                    boolean cleared = plugin.getBackpackService().clearBackpack(offline.getUniqueId(), player);
                    if (cleared) {
                        sender.sendMessage("§a✓ Mochila de §e" + targetName + " §avaciada.");
                    } else {
                        sender.sendMessage("§c✗ La mochila de " + targetName + " está vacía o no existe.");
                    }
                } else {
                    sender.sendMessage("§c✗ Jugador no encontrado: " + targetName);
                }
            }
            
            default -> {
                sender.sendMessage("§cSubcomandos de mochila:");
                sender.sendMessage("  §e/avo mochila §7- Abre tu mochila");
                if (sender.hasPermission("apocalipsis.mochila.mod")) {
                    sender.sendMessage("  §e/avo mochila ver <jugador> §7- Ver mochila ajena");
                    sender.sendMessage("  §e/avo mochila lista §7- Listar mochilas");
                }
                if (sender.hasPermission("apocalipsis.mochila.admin")) {
                    sender.sendMessage("  §e/avo mochila vaciar <jugador> §7- Vaciar mochila");
                }
            }
        }
    }
    
    /**
     * Abre el ender chest portable
     */
    private void cmdEnderChest(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return;
        }
        
        plugin.getBackpackService().openPortableEnderChest(player);
    }
    
    /**
     * Muestra estadísticas de uso de skills
     */
    private void cmdSkillStats(CommandSender sender, String[] args) {
        if (!sender.hasPermission("apocalipsis.admin")) {
            sender.sendMessage("§cNo tienes permiso para usar este comando.");
            return;
        }
        
        var listener = plugin.getSkillEffectListener();
        if (listener == null) {
            sender.sendMessage("§cEl sistema de skills no está disponible.");
            return;
        }
        
        if (args.length >= 2 && args[1].equalsIgnoreCase("player")) {
            // Stats de un jugador específico
            if (args.length < 3) {
                sender.sendMessage("§cUso: /avo skillstats player <nombre>");
                return;
            }
            
            Player target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("§cJugador no encontrado.");
                return;
            }
            
            var playerStats = listener.getPlayerStats(target.getUniqueId());
            if (playerStats.isEmpty()) {
                sender.sendMessage("§7" + target.getName() + " no ha usado ninguna habilidad todavía.");
                return;
            }
            
            sender.sendMessage("§6§l⚡ §eEstadísticas de " + target.getName() + ":");
            playerStats.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(10)
                .forEach(e -> sender.sendMessage("  §7• §f" + e.getKey() + "§7: §a" + e.getValue() + " usos"));
                
        } else if (args.length >= 2 && args[1].equalsIgnoreCase("top")) {
            // Top skills globales
            int limit = 10;
            if (args.length >= 3) {
                try {
                    limit = Integer.parseInt(args[2]);
                } catch (NumberFormatException ignored) {}
            }
            
            var topSkills = listener.getTopSkills(limit);
            if (topSkills.isEmpty()) {
                sender.sendMessage("§7No hay estadísticas de skills todavía.");
                return;
            }
            
            sender.sendMessage("§6§l⚡ §eTop " + limit + " Skills más usadas:");
            int i = 1;
            for (var entry : topSkills) {
                sender.sendMessage("  §e#" + i + " §f" + entry.skillId() + "§7: §a" + entry.totalUses() + " usos");
                i++;
            }
            
        } else {
            // Resumen general
            var globalStats = listener.getGlobalStats();
            long totalUses = globalStats.values().stream().mapToLong(Long::longValue).sum();
            
            sender.sendMessage("§6§l⚡ §eEstadísticas de Skills:");
            sender.sendMessage("  §7Total de skills usadas: §a" + globalStats.size());
            sender.sendMessage("  §7Total de activaciones: §a" + totalUses);
            sender.sendMessage("");
            sender.sendMessage("§7Subcomandos:");
            sender.sendMessage("  §e/avo skillstats top [n] §7- Top N skills más usadas");
            sender.sendMessage("  §e/avo skillstats player <nombre> §7- Stats de un jugador");
        }
    }
    
    /**
     * Abre el menú principal para jugadores
     * Acceso centralizado a todas las funciones del plugin
     */
    private void cmdMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return;
        }
        
        if (plugin.getMainMenuManager() == null) {
            sender.sendMessage("§cEl menú principal no está disponible.");
            return;
        }
        
        plugin.getMainMenuManager().openMainMenu(player);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // COMANDOS DE RANGOS PERMANENTES
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Crea un nuevo rango permanente/personalizado
     * Uso: /avo newrank <nombre> <tipo:permanente/temporal>
     */
    private void cmdNewRank(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cSolo operadores pueden usar este comando.");
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            sender.sendMessage("§e§lCREAR RANGO PERMANENTE");
            sender.sendMessage("");
            sender.sendMessage("§7Uso: §e/avo newrank <id> <tipo>");
            sender.sendMessage("");
            sender.sendMessage("§7Tipos disponibles:");
            sender.sendMessage("  §apermanente §7- Rango fijo que no expira");
            sender.sendMessage("  §etemporal §7- Rango con duración limitada");
            sender.sendMessage("");
            sender.sendMessage("§7Después de crear el rango, edita el archivo");
            sender.sendMessage("§7§orangos_permanentes.yml §7para configurar:");
            sender.sendMessage("  §8• Display name y color");
            sender.sendMessage("  §8• Tab prefix/suffix");
            sender.sendMessage("  §8• Chat prefix");
            sender.sendMessage("  §8• Efectos de poción");
            sender.sendMessage("  §8• Prioridad");
            sender.sendMessage("  §8• Herencia de efectos del rango normal");
            sender.sendMessage("");
            sender.sendMessage("§7Ejemplo:");
            sender.sendMessage("  §e/avo newrank vip permanente");
            sender.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            return;
        }
        
        String rankId = args[1].toLowerCase();
        String tipo = args[2].toLowerCase();
        
        if (!tipo.equals("permanente") && !tipo.equals("temporal")) {
            sender.sendMessage("§cTipo inválido. Usa: permanente o temporal");
            return;
        }
        
        // Verificar si ya existe
        if (plugin.getPermRankManager().getRank(rankId) != null) {
            sender.sendMessage("§cYa existe un rango con ese ID: §e" + rankId);
            return;
        }
        
        // Crear rango con valores por defecto
        String displayName = "§f[" + rankId.toUpperCase() + "]";
        String tabPrefix = "§f[" + rankId.toUpperCase() + "] ";
        String tabSuffix = "";
        String chatPrefix = "§f[" + rankId.toUpperCase() + "] ";
        String color = "§f";
        int priority = 10;
        boolean inheritNormal = false;
        
        boolean success = plugin.getPermRankManager().createRank(
            rankId, displayName, tabPrefix, tabSuffix, chatPrefix, 
            color, priority, inheritNormal, null
        );
        
        if (success) {
            sender.sendMessage("§a✓ Rango creado: §e" + rankId);
            sender.sendMessage("§7Tipo: §e" + tipo);
            sender.sendMessage("");
            sender.sendMessage("§7Edita §orangos_permanentes.yml §7para personalizar:");
            sender.sendMessage("  §8• Display name, color, prefix/suffix");
            sender.sendMessage("  §8• Efectos de poción");
            sender.sendMessage("  §8• Prioridad (mayor = más importante)");
            sender.sendMessage("");
            sender.sendMessage("§7Luego recarga: §e/avo reload");
        } else {
            sender.sendMessage("§cError al crear el rango.");
        }
    }
    
    /**
     * Asigna un rango permanente a un jugador
     * Uso: /avo setpermrank <jugador> <rango> [tiempo]
     */
    private void cmdSetPermRank(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cSolo operadores pueden usar este comando.");
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            sender.sendMessage("§e§lASIGNAR RANGO PERMANENTE");
            sender.sendMessage("");
            sender.sendMessage("§7Uso: §e/avo setpermrank <jugador> <rango> [tiempo]");
            sender.sendMessage("");
            sender.sendMessage("§7Tiempo (opcional):");
            sender.sendMessage("  §aSin especificar §7- Permanente");
            sender.sendMessage("  §e1d, 7d, 30d §7- Días");
            sender.sendMessage("  §e1h, 24h, 168h §7- Horas");
            sender.sendMessage("  §e1m, 60m, 1440m §7- Minutos");
            sender.sendMessage("");
            sender.sendMessage("§7Rangos disponibles:");
            for (String rankId : plugin.getPermRankManager().getRankIds()) {
                var rank = plugin.getPermRankManager().getRank(rankId);
                sender.sendMessage("  " + rank.getDisplayName() + " §8(" + rankId + ")");
            }
            sender.sendMessage("");
            sender.sendMessage("§7Ejemplos:");
            sender.sendMessage("  §e/avo setpermrank Steve vip §7- VIP permanente");
            sender.sendMessage("  §e/avo setpermrank Alex admin 30d §7- Admin por 30 días");
            sender.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage("§cJugador no encontrado: §e" + args[1]);
            return;
        }
        
        String rankId = args[2].toLowerCase();
        var rank = plugin.getPermRankManager().getRank(rankId);
        
        if (rank == null) {
            sender.sendMessage("§cRango no encontrado: §e" + rankId);
            sender.sendMessage("§7Usa §e/avo listpermranks §7para ver disponibles");
            return;
        }
        
        // Parsear tiempo si se especificó
        long durationMillis = 0;
        String durationText = "permanente";
        
        if (args.length >= 4) {
            String timeStr = args[3].toLowerCase();
            try {
                long amount = Long.parseLong(timeStr.replaceAll("[^0-9]", ""));
                
                if (timeStr.endsWith("d")) {
                    durationMillis = amount * 24 * 60 * 60 * 1000; // días
                    durationText = amount + " día" + (amount != 1 ? "s" : "");
                } else if (timeStr.endsWith("h")) {
                    durationMillis = amount * 60 * 60 * 1000; // horas
                    durationText = amount + " hora" + (amount != 1 ? "s" : "");
                } else if (timeStr.endsWith("m")) {
                    durationMillis = amount * 60 * 1000; // minutos
                    durationText = amount + " minuto" + (amount != 1 ? "s" : "");
                } else {
                    sender.sendMessage("§cFormato de tiempo inválido. Usa: 30d, 24h, 60m");
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cFormato de tiempo inválido: §e" + timeStr);
                return;
            }
        }
        
        // Asignar rango
        boolean success = plugin.getPermRankManager().assignRank(
            target.getUniqueId(), rankId, durationMillis
        );
        
        if (success) {
            sender.sendMessage("§a✓ Rango asignado:");
            sender.sendMessage("  §7Jugador: §e" + target.getName());
            sender.sendMessage("  §7Rango: " + rank.getDisplayName());
            sender.sendMessage("  §7Duración: §e" + durationText);
            
            // Notificar al jugador
            target.sendMessage("");
            target.sendMessage("§6§l⭐ RANGO ASIGNADO");
            target.sendMessage("");
            target.sendMessage("§7Has recibido el rango " + rank.getDisplayName());
            target.sendMessage("§7Duración: §e" + durationText);
            target.sendMessage("");
            
            // Actualizar tab
            plugin.getPermRankManager().updatePlayerTab(target);
        } else {
            sender.sendMessage("§cError al asignar el rango.");
        }
    }
    
    /**
     * Remueve el rango permanente de un jugador
     * Uso: /avo removepermrank <jugador>
     */
    private void cmdRemovePermRank(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cSolo operadores pueden usar este comando.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§7Uso: §e/avo removepermrank <jugador>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage("§cJugador no encontrado: §e" + args[1]);
            return;
        }
        
        var currentRank = plugin.getPermRankManager().getPlayerPermRank(target.getUniqueId());
        
        if (currentRank == null) {
            sender.sendMessage("§7" + target.getName() + " no tiene un rango permanente.");
            return;
        }
        
        boolean success = plugin.getPermRankManager().removeRank(target.getUniqueId());
        
        if (success) {
            sender.sendMessage("§a✓ Rango removido de §e" + target.getName());
            sender.sendMessage("  §7Rango anterior: " + currentRank.getDisplayName());
            
            // Notificar al jugador
            target.sendMessage("§7Tu rango permanente " + currentRank.getDisplayName() + " §7ha sido removido.");
            
            // Actualizar tab
            plugin.getPermRankManager().updatePlayerTab(target);
        } else {
            sender.sendMessage("§cError al remover el rango.");
        }
    }
    
    /**
     * Lista todos los rangos permanentes disponibles
     * Uso: /avo listpermranks
     */
    private void cmdListPermRanks(CommandSender sender) {
        Set<String> rankIds = plugin.getPermRankManager().getRankIds();
        
        if (rankIds.isEmpty()) {
            sender.sendMessage("§7No hay rangos permanentes creados.");
            sender.sendMessage("§7Usa §e/avo newrank <id> <tipo> §7para crear uno.");
            return;
        }
        
        sender.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        sender.sendMessage("§e§lRANGOS PERMANENTES DISPONIBLES");
        sender.sendMessage("");
        
        List<String> sortedIds = new ArrayList<>(rankIds);
        sortedIds.sort(String::compareTo);
        
        for (String rankId : sortedIds) {
            var rank = plugin.getPermRankManager().getRank(rankId);
            sender.sendMessage(rank.getDisplayName() + " §8(" + rankId + ")");
            sender.sendMessage("  §7Prioridad: §e" + rank.getPriority());
            sender.sendMessage("  §7Tab: §f" + rank.getTabPrefix() + "JUGADOR" + rank.getTabSuffix());
            
            if (!rank.getPotionEffects().isEmpty()) {
                sender.sendMessage("  §7Efectos: §a" + rank.getPotionEffects().size() + " activos");
            }
            sender.sendMessage("");
        }
        
        sender.sendMessage("§7Total: §e" + rankIds.size() + " rangos");
        sender.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
    }
}
