package me.apocalipsis.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.disaster.DisasterController;
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
    private final MissionService missionService;
    private final MessageBus messageBus;

    public ApocalipsisCommand(Apocalipsis plugin, StateManager stateManager, DisasterController disasterController,
                             MissionService missionService, TimeService timeService, MessageBus messageBus) {
        this.plugin = plugin;
        this.stateManager = stateManager;
        this.disasterController = disasterController;
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
            sender.sendMessage("§6=== Misiones ===");
            sender.sendMessage("§e/avo newday §7- Crea un nuevo día y asigna misiones");
            sender.sendMessage("§e/avo endday §7- Termina el día actual");
            sender.sendMessage("§e/avo status [jugador] §7- Muestra misiones activas");
            sender.sendMessage("§e/avo setps <jugador> <ps> §7- Ajusta PS de un jugador");
            sender.sendMessage("§e/avo mission <give|complete|clear> §7- Gestión de misiones");
            sender.sendMessage("§6=== Sistema ===");
            sender.sendMessage("§e/avo tps §7- Ver TPS y rendimiento");
            sender.sendMessage("§e/avo stats §7- Estadísticas del servidor");
            sender.sendMessage("§e/avo cooldown §7- Estado del cooldown");
            sender.sendMessage("§e/avo backup §7- Backup manual de datos");
            sender.sendMessage("§e/avo reload §7- Recarga la configuración");
            sender.sendMessage("§e/avo test §7- Toggle modo test");
            sender.sendMessage("§e/avo debug missions §7- Debug de misiones");
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
            case "newday":
                cmdNewDay(sender);
                break;
            case "endday":
                cmdEndDay(sender);
                break;
            case "status":
                cmdStatus(sender, args);
                break;
            case "setps":
                cmdSetPs(sender, args);
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
        stateManager.setEstado("PREPARACION");
        stateManager.setString("desastre_actual", "");
        stateManager.setPrepForzada(false);                     // preparación NO forzada
        stateManager.setLastEndEpochMs(now - cooldownMs - 1000L); // cooldown ya cumplido

        // Tiempos solo para UI; no queremos barra azul aquí
        stateManager.setLong("start_epoch_ms", now);
        stateManager.setLong("end_epoch_ms", now);

        stateManager.saveState();

        // Antirrebote + reinicio de puertas internas
        disasterController.resetStartingFlag();    // por si había un intento previo
        disasterController.resetCooldownAutoStartFlag();
        disasterController.markEnteredPreparation();
        // Programa el auto-next (el que realmente iniciará el desastre)
        disasterController.scheduleAutoNext();

        // Feedback
        sender.sendMessage("§a✅ Ciclo iniciado. El primer desastre comenzará en breve.");
        plugin.getLogger().info("[Cycle] /avo start → PREPARACION (no forzada), cooldown cumplido. Scheduler armado.");
        }

    
    private void cmdStop(CommandSender sender) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        disasterController.stopAllDisasters(true);
        sender.sendMessage("§7Desastre detenido. Todas las tareas canceladas.");
        
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[Cycle] STOP ejecutado manualmente por " + sender.getName());
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
            plugin.getLogger().info("[Cycle] INICIO por /avo force: desastre=" + disasterId);
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
            
            // 3.5. Marcar entrada en PREPARACION para antirrebote
            disasterController.markEnteredPreparation();

            // 4. Ocultar BossBar, scoreboard muestra tiempo restante
            disasterController.hideBossBar();
            
            // 5. Asegurar que el scheduler auto-next esté activo
            disasterController.scheduleAutoNext();
            
            // Mostrar tiempo real según test mode
            String timeDisplay = plugin.getConfigManager().isTestMode() ? "5 segundos" : minutes + " minutos";
            sender.sendMessage("§e✓ Preparación forzada iniciada por §f" + timeDisplay + "§e.");
            plugin.getLogger().info("[Cycle] PREPARACION forzada " + minutes + "m");
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
        
        missionService.assignMissionsForDay(day);
        messageBus.broadcast("§e§l⌛ §fNuevo día iniciado: §e" + day, "newday");
        sender.sendMessage("§a✓ Día " + day + " iniciado. Misiones asignadas.");
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
        
        missionService.endDay();
        messageBus.broadcast("§7⌛ §fDía finalizado. Misiones no completadas han sido marcadas como §cfallidas§f.", "endday");
        sender.sendMessage("§7Día finalizado.");
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
            sender.sendMessage("§cUso: /avo debug <missions>");
            return;
        }

        if (args[1].equalsIgnoreCase("missions")) {
            sender.sendMessage("§7=== DEBUG MISIONES ===");
            sender.sendMessage("§7Día actual: §e" + stateManager.getCurrentDay());
            sender.sendMessage("§7Jugadores con misiones: §e" + plugin.getServer().getOnlinePlayers().size());
            sender.sendMessage("§c[NOTA] Tipos EXPLORAR y ALTURA están deshabilitados");
            
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                var assignments = missionService.getActiveAssignments(player);
                sender.sendMessage("§e" + player.getName() + " §7tiene §f" + assignments.size() + " §7misiones.");
            }
        } else if (args[1].equalsIgnoreCase("explore")) {
            // [REMOVAL] Debug explore deshabilitado (tipo removido)
            sender.sendMessage("§c[REMOVAL] El comando /avo debug explore está deshabilitado");
            sender.sendMessage("§7Las misiones tipo EXPLORAR y ALTURA han sido removidas");
        }
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
        
        // [AlonsoLevels] Log reload info
        org.bukkit.configuration.file.FileConfiguration alonsoConfig = plugin.getConfigManager().getAlonsoLevelsConfig();
        boolean alonsoEnabled = alonsoConfig != null && alonsoConfig.getBoolean("enabled", true);
        int alonsoCmds = alonsoConfig != null ? alonsoConfig.getStringList("commands").size() : 0;
        plugin.getLogger().info("[Alonso] Reload OK (enabled=" + alonsoEnabled + ", cmds=" + alonsoCmds + ")");
        
        sender.sendMessage("§a✓ Reload completado:");
        sender.sendMessage("§7  - misiones_new.yml, rangos.yml, desastres.yml");
        sender.sendMessage("§7  - config.yml, alonsolevels.yml");
        sender.sendMessage("§7  - AlonsoLevels: " + (alonsoEnabled ? "§aENABLED" : "§cDISABLED") + " §7(" + alonsoCmds + " cmds)");
        sender.sendMessage("§7  - TAB/Scoreboard reaplicados a " + plugin.getServer().getOnlinePlayers().size() + " jugadores");
        sender.sendMessage("§7Flags de ciclo:");
        sender.sendMessage("§7  auto_cycle: §e" + plugin.getConfigManager().isAutoCycleEnabled());
        sender.sendMessage("§7  start_on_boot: §e" + startOnBoot);
        sender.sendMessage("§7  min_jugadores: §e" + plugin.getConfigManager().getMinJugadores());
        sender.sendMessage("§7  cooldown: §e" + plugin.getConfigManager().getCooldownFinSegundos() + "s");
        
        plugin.getLogger().info("[Reload] OK: misiones, rangos, desastres, anticheat, alonso aplicados. " +
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
                
                sender.sendMessage("§a✓ Preparación establecida a " + minutos + " minutos (" + segundos + "s).");
                plugin.getLogger().info("[cmdTime] PREPARACION - TimeService reiniciado a " + segundos + "s (end_epoch_ms=" + newEndMs + ")");
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
     * /avo setps <jugador> <ps> - Ajusta PS de un jugador manualmente
     */
    private void cmdSetPs(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUso: /avo setps <jugador> <ps>");
            return;
        }

        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cJugador no encontrado: " + args[1]);
            return;
        }

        int ps;
        try {
            ps = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cPS inválido: " + args[2]);
            return;
        }

        if (ps < 0) {
            sender.sendMessage("§cPS no puede ser negativo.");
            return;
        }

        int oldPs = plugin.getRankService().getPS(target);
        plugin.getMissionService().setPlayerPS(target.getUniqueId(), ps);
        
        sender.sendMessage("§a✓ PS de " + target.getName() + ": §e" + oldPs + " §7→ §e" + ps);
        target.sendMessage("§6[Admin] §aTus PS fueron ajustados a §e" + ps);
        
        // Actualizar UI
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updatePlayer(target);
        }
        if (plugin.getTablistManager() != null) {
            plugin.getTablistManager().applyTabPrefix(target);
        }
        
        plugin.getLogger().info("[Admin] " + sender.getName() + " ajustó PS de " + target.getName() + ": " + oldPs + " → " + ps);
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
                    sender.sendMessage("§cUso: /avo mission complete <jugador> [todas]");
                    return;
                }

                Player targetComplete = plugin.getServer().getPlayer(args[2]);
                if (targetComplete == null) {
                    sender.sendMessage("§cJugador no encontrado: " + args[2]);
                    return;
                }

                boolean all = args.length >= 4 && "todas".equalsIgnoreCase(args[3]);
                if (all) {
                    int completed = missionService.forceCompleteAllMissions(targetComplete);
                    sender.sendMessage("§a✓ Completadas " + completed + " misiones de " + targetComplete.getName());
                    targetComplete.sendMessage("§6[Admin] §aTodas tus misiones fueron completadas.");
                } else {
                    sender.sendMessage("§cEspecifica 'todas' para completar todas las misiones.");
                }
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
}
