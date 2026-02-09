package me.apocalipsis.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredListener;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.disaster.DisasterController;
import me.apocalipsis.events.EventController;
import me.apocalipsis.events.testing.TestResult;
import me.apocalipsis.missions.MissionAssignment;
import me.apocalipsis.missions.MissionService;
import me.apocalipsis.missions.MissionType;
import me.apocalipsis.skills.Skill;
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
                cmdNewDay(sender, args);
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
            case "evento4":
            case "caminoend":
            case "caminoalend":
            case "camino":
                cmdEvento4(sender, args);
                break;
            case "evento5":
            case "aperturaend":
                cmdEvento5(sender, args);
                break;
            case "evento6":
            case "mundoolvidado":
            case "reinicio":
                cmdEvento6(sender, args);
                break;
            case "navidad":
                cmdNavidad(sender, args);
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
            case "canjear":
            case "redeem":
                cmdCanjear(sender, args);
                break;
            case "onboarding":
                cmdOnboarding(sender, args);
                break;
            case "buddy":
            case "mentor":
                cmdBuddy(sender, args);
                break;
            case "rtp":
            case "randomtp":
            case "wild":
                cmdRandomTeleport(sender);
                break;
            case "volver":
            case "overworld":
            case "salir":
            case "escape":
                cmdVolver(sender);
                break;
            case "ciclo":
            case "cycle":
            case "mundo":
            case "world":
                // Extraer subcomandos: args[1..n] se convierte en args[0..n-1]
                String[] cicloArgs = new String[args.length - 1];
                System.arraycopy(args, 1, cicloArgs, 0, args.length - 1);
                cmdCiclo(sender, cicloArgs);
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
            {"  §e/avo newday [force]", "§7Nuevo día + misiones (force = ignorar cooldown)"},
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
            
            // Page 5: Stream Features y Rangos Permanentes
            {"§6▸ Stream Features", ""},
            {"  §e/avo canjear", "§7Ver tokens y recompensas"},
            {"  §e/avo canjear <item>", "§7Canjear tokens por item"},
            {"§7Drops especiales cuando el streamer está online!", ""},
            {"§6▸ Rangos Permanentes", ""},
            {"  §e/avo setpermrank <jugador> <rango>", "§7Asignar rango permanente"},
            {"  §e/avo removepermrank <jugador>", "§7Quitar rango permanente"},
            {"  §e/avo listpermranks", "§7Lista de rangos disponibles"},
            {"  §e/avo newrank <id> <tipo>", "§7Crear nuevo rango"},
            {"§7Rangos personalizados con efectos y prefijos!", ""},
            {"§6▸ Teleporte", ""},
            {"  §e/avo rtp", "§7TP aleatorio (1000-5000 bloques)"},
            {"  §e/avo volver", "§7Escapar del End al Overworld"},
            {"§7¡Usa /avo volver si quedas atrapado en el End!", ""},
            
            // Page 6: Ciclos Multi-Mundo
            {"§6▸ Sistema de Ciclos", ""},
            {"  §e/avo ciclo help", "§7Ayuda del sistema de ciclos"},
            {"  §e/avo ciclo info", "§7Info ciclo actual"},
            {"  §e/avo ciclo list", "§7Lista todos los ciclos"},
            {"  §e/avo ciclo create <nombre>", "§7Crear nuevo ciclo"},
            {"  §e/avo ciclo activate <nombre>", "§7Activar ciclo específico"},
            {"  §e/avo ciclo delete <nombre>", "§7Eliminar ciclo"},
            {"  §e/avo ciclo setspawn", "§7Set spawn del ciclo"},
            {"  §e/avo ciclo fixspawn [mundo]", "§7Auto-corregir spawn"},
            {"§7Sistema multi-mundo con inventarios separados!", ""},
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

    private void cmdNewDay(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        // Verificar si se quiere forzar
        boolean force = args.length > 1 && "force".equalsIgnoreCase(args[1]);
        if (force) {
            sender.sendMessage("§6⚠ §eForzando nuevo día ignorando cooldown...");
        }

        // [SEGURIDAD] Usar método mejorado con validaciones
        boolean success = stateManager.incrementDay(force);
        
        if (!success) {
            sender.sendMessage("§c✖ Error: No se pudo incrementar el día (verificar logs para detalles)");
            sender.sendMessage("§7Posibles causas: cooldown activo, límite alcanzado, o fallo de guardado");
            return;
        }
        
        int day = stateManager.getCurrentDay();
        
        // [1.21.8] Resetear flags de celebración
        missionService.resetPlayerDailyCompleteFired();
        
        // [REMOVAL] Reseteos de EXPLORAR y ALTURA deshabilitados (tipos removidos)
        // missionService.resetExploreTrackers();
        // missionService.resetHeightCounters();
        
        // [FIX] assignMissionsForDay ahora limpia automáticamente las misiones anteriores
        missionService.assignMissionsForDay(day);
        
        // [SEGURIDAD] Programar próximo día automático (+24h)
        long nextDayMs = System.currentTimeMillis() + 86400000L; // +24h
        stateManager.setNextDayEpochMs(nextDayMs);
        
        int onlinePlayers = plugin.getServer().getOnlinePlayers().size();
        messageBus.broadcast("§a§l✓ §fNuevo día iniciado: §e" + day + " §8(próximo en 24h)", "newday");
        sender.sendMessage("§a✓ Día " + day + " iniciado exitosamente.");
        sender.sendMessage("§7Misiones anteriores limpiadas y nuevas asignadas a " + onlinePlayers + " jugador(es).");
        sender.sendMessage("§7Próximo día automático: §e" + new java.util.Date(nextDayMs));
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

        java.util.List<MissionAssignment> assignments = missionService.getActiveAssignments(target);
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
                java.util.List<MissionAssignment> assignments = missionService.getActiveAssignments(player);
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

    /**
     * Comandos para el Evento El Camino al End
     * /avo evento4 <subcomando>
     */
    private void cmdEvento4(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§5§l⚡ ═══ EL CAMINO AL END ═══ ⚡");
            sender.sendMessage("§7Anomalías dimensionales aparecen...");
            sender.sendMessage("");
            sender.sendMessage("§e▸ Control Principal:");
            sender.sendMessage("  §f/avo evento4 start §7- Inicia el evento");
            sender.sendMessage("  §f/avo evento4 stop §7- Finaliza el evento");
            sender.sendMessage("  §f/avo evento4 info §7- Estado del evento");
            sender.sendMessage("");
            sender.sendMessage("§e▸ Gestión de Fases:");
            sender.sendMessage("  §f/avo evento4 fase <1-3> §7- Fuerza fase");
            sender.sendMessage("  §f/avo evento4 next §7- Siguiente fase");
            sender.sendMessage("");
            sender.sendMessage("§e▸ Utilidades:");
            sender.sendMessage("  §f/avo evento4 fragmentos <jugador> [cant] §7- Dar fragmentos");
            sender.sendMessage("  §f/avo evento4 testwarden <jugador> §7- Test Warden (34 fragmentos)");
            sender.sendMessage("  §f/avo evento4 getitemsevento4 §7- Obtener todos los items únicos");
            sender.sendMessage("  §f/avo evento4 anomalia spawn §7- Spawn anomalía");
            sender.sendMessage("  §f/avo evento4 portal spawn §7- Ver info portal");
            sender.sendMessage("  §f/avo evento4 tp <portal|anomalia> §7- Teleportarse");
            sender.sendMessage("");
            sender.sendMessage("§7Aliases: §fcaminoend, caminoalend, camino");
            return;
        }
        
        String subCmd = args[1].toLowerCase();
        
        // Obtener instancia del evento
        me.apocalipsis.events.CaminoEndEvent evento4 = null;
        if (eventController.hasActiveEvent() && 
            eventController.getActiveEvent() instanceof me.apocalipsis.events.CaminoEndEvent) {
            evento4 = (me.apocalipsis.events.CaminoEndEvent) eventController.getActiveEvent();
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
                    return;
                }
                
                // Verificar SAFE_MODE
                if (stateManager.isSafeModeActive()) {
                    sender.sendMessage("§cNo se puede iniciar en SAFE_MODE (TPS bajo).");
                    return;
                }
                
                // Iniciar evento
                if (eventController.startEvent("camino_end")) {
                    sender.sendMessage("§a✓ Evento §5§l⚡ El Camino al End §ainiciado");
                    sender.sendMessage("§7El Observador percibe algo extraño...");
                    
                    // Título y sonido para todos
                    for (Player p : plugin.getServer().getOnlinePlayers()) {
                        p.sendTitle("§5§l⚡ EL CAMINO AL END ⚡", "§7Las anomalías despiertan...", 10, 60, 20);
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.0f, 0.6f);
                        p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.8f, 0.8f);
                    }
                    
                    plugin.getLogger().info(String.format("[CaminoEnd] Iniciado por %s", sender.getName()));
                } else {
                    sender.sendMessage("§cNo se pudo iniciar el evento. Verifica la consola.");
                }
                break;
                
            case "stop":
            case "detener":
                if (evento4 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                // Título y sonido para todos
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    p.sendTitle("§8§l⚡ EVENTO FINALIZADO ⚡", "§7El camino se cierra...", 10, 50, 20);
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, 1.0f, 0.7f);
                }
                
                eventController.stopActiveEvent();
                sender.sendMessage("§7✓ Evento §5El Camino al End §7detenido");
                plugin.getLogger().info(String.format("[CaminoEnd] Detenido por %s", sender.getName()));
                break;
                
            case "info":
            case "status":
                if (evento4 == null) {
                    sender.sendMessage("§5§l⚡ EL CAMINO AL END - INFO");
                    sender.sendMessage("§7Estado: §cInactivo");
                    sender.sendMessage("§7Usa §e/avo evento4 start §7para iniciarlo.");
                    return;
                }
                
                sender.sendMessage("§5§l⚡ ═══ EL CAMINO AL END ═══ ⚡");
                sender.sendMessage("§7Estado: §aActivo");
                sender.sendMessage("§7Fase actual: §e" + evento4.getFaseActual());
                sender.sendMessage("§7Fragmentos recolectados: §e" + evento4.getFragmentosRecolectados() + "§7/§e40");
                sender.sendMessage("§7Anomalías activas: §e" + evento4.getAnomaliasActivas().size());
                
                boolean portalGenerado = evento4.getFaseActual() == me.apocalipsis.events.CaminoEndEvent.Fase.REVELACION;
                sender.sendMessage("§7Portal generado: " + (portalGenerado ? "§aSí" : "§7No"));
                
                if (portalGenerado && evento4.getFragmentosRecolectados() >= 40) {
                    sender.sendMessage("");
                    sender.sendMessage("§d§l★ LISTO PARA CLIFFHANGER ★");
                    sender.sendMessage("§7Usa §e/avo evento4 completarportal §7cuando estés listo");
                    sender.sendMessage("§7para ejecutar la cinemática final del evento");
                }
                
                sender.sendMessage("");
                sender.sendMessage("§8Usa §e/avo evento4 anomalias §8para ver ubicaciones");
                break;
                
            case "anomalias":
            case "anomalías":
                if (evento4 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                java.util.Map<Location, ?> anomaliasMap = evento4.getAnomaliasActivas();
                
                if (anomaliasMap.isEmpty()) {
                    sender.sendMessage("§5§l⚡ ANOMALÍAS ACTIVAS");
                    sender.sendMessage("§7No hay anomalías activas en este momento.");
                    sender.sendMessage("§7El sistema genera anomalías cada 10 segundos.");
                    if (evento4.getFaseActual() == me.apocalipsis.events.CaminoEndEvent.Fase.ANOMALIAS) {
                        sender.sendMessage("§7Fase actual: §eANOMALIAS §7- Espera unos segundos...");
                    }
                    return;
                }
                
                sender.sendMessage("§5§l⚡ ═══ ANOMALÍAS ACTIVAS ═══ ⚡");
                sender.sendMessage("§7Total: §e" + anomaliasMap.size());
                sender.sendMessage("§7Fase: §e" + evento4.getFaseActual());
                sender.sendMessage("");
                
                int anomaliaIndex = 1;
                Player senderPlayer = sender instanceof Player ? (Player) sender : null;
                for (Location loc : anomaliasMap.keySet()) {
                    String coords = String.format("§e%d, %d, %d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                    String distancia = "";
                    
                    if (senderPlayer != null && senderPlayer.getWorld().equals(loc.getWorld())) {
                        double dist = senderPlayer.getLocation().distance(loc);
                        distancia = String.format(" §8(§7%.0f bloques§8)", dist);
                    }
                    
                    sender.sendMessage("§7" + anomaliaIndex + ". " + coords + distancia);
                    anomaliaIndex++;
                }
                
                sender.sendMessage("");
                if (evento4.getFaseActual() == me.apocalipsis.events.CaminoEndEvent.Fase.RESONANCIA && 
                    evento4.getFragmentosRecolectados() >= 35) {
                    sender.sendMessage("§6§l⚠ Warden spawneará cuando alguien esté < 20 bloques de una anomalía");
                }
                break;
                
            case "fase":
            case "phase":
                if (evento4 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo evento4 fase <1-3>");
                    sender.sendMessage("§7  1 = ANOMALIAS (spawn anomalías)");
                    sender.sendMessage("§7  2 = RESONANCIA (recolección fragmentos)");
                    sender.sendMessage("§7  3 = REVELACION (portal incompleto)");
                    return;
                }
                
                sender.sendMessage("§e⚠ Este evento tiene transiciones automáticas.");
                sender.sendMessage("§7Usa §e/avo evento4 fragmentos §7para forzar progreso.");
                break;
                
            case "next":
            case "siguiente":
                if (evento4 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                // Obtener fase actual
                me.apocalipsis.events.CaminoEndEvent.Fase faseAnterior = evento4.getFaseActual();
                
                // Forzar siguiente fase
                evento4.forzarSiguienteFase();
                
                // Título y sonido según la nueva fase
                String titulo = "";
                String subtitulo = "";
                Sound sonido = Sound.BLOCK_END_PORTAL_FRAME_FILL;
                float pitch = 1.0f;
                
                switch (evento4.getFaseActual()) {
                    case ANOMALIAS:
                        titulo = "§9§l⚡ FASE I: ANOMALÍAS ⚡";
                        subtitulo = "§7Rastrea y estabiliza las anomalías...";
                        sonido = Sound.BLOCK_AMETHYST_BLOCK_CHIME;
                        pitch = 1.0f;
                        break;
                    case RESONANCIA:
                        titulo = "§d§l⚡ FASE II: RESONANCIA ⚡";
                        subtitulo = "§7Los fragmentos resuenan...";
                        sonido = Sound.BLOCK_BEACON_ACTIVATE;
                        pitch = 1.2f;
                        break;
                    case REVELACION:
                        titulo = "§5§l⚡ FASE III: REVELACIÓN ⚡";
                        subtitulo = "§7El portal incompleto se manifiesta...";
                        sonido = Sound.ENTITY_ENDER_DRAGON_GROWL;
                        pitch = 0.8f;
                        break;
                }
                
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    p.sendTitle(titulo, subtitulo, 10, 70, 20);
                    p.playSound(p.getLocation(), sonido, 1.0f, pitch);
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT, 0.6f, 0.6f);
                }
                
                sender.sendMessage("§a✓ Forzada siguiente fase");
                sender.sendMessage("§7De: §e" + faseAnterior + " §7→ §e" + evento4.getFaseActual());
                plugin.getLogger().info(String.format("[CaminoEnd] %s forzó siguiente fase: %s → %s", 
                    sender.getName(), faseAnterior, evento4.getFaseActual()));
                break;
                
            case "fragmentos":
                if (evento4 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo evento4 fragmentos <jugador> [cantidad]");
                    sender.sendMessage("§7Ejemplo: §e/avo evento4 fragmentos Steve 5");
                    return;
                }
                
                Player target = plugin.getServer().getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage("§cJugador no encontrado: " + args[2]);
                    return;
                }
                
                int cantidad = 1;
                if (args.length >= 4) {
                    try {
                        cantidad = Integer.parseInt(args[3]);
                        if (cantidad < 1 || cantidad > 10) {
                            sender.sendMessage("§cCantidad inválida (1-10).");
                            return;
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cCantidad inválida: " + args[3]);
                        return;
                    }
                }
                
                // Dar fragmentos (items al inventario)
                for (int i = 0; i < cantidad; i++) {
                    target.getInventory().addItem(evento4.getItems().crearFragmentoDelVacio());
                }
                
                // Incrementar el contador global también
                int fragmentosAntes = evento4.getFragmentosRecolectados();
                evento4.setFragmentosRecolectados(fragmentosAntes + cantidad);
                
                // Título y sonido para el jugador
                target.sendTitle("§5§l⚡ FRAGMENTO DEL VACÍO ⚡", "§7+" + cantidad + " fragmento" + (cantidad > 1 ? "s" : ""), 10, 40, 10);
                target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                target.playSound(target.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.2f);
                target.spawnParticle(Particle.DRAGON_BREATH, target.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);
                
                sender.sendMessage("§a✓ Dados §e" + cantidad + " §afragmentos a §e" + target.getName());
                sender.sendMessage("§7Contador global: §e" + fragmentosAntes + " §7→ §e" + evento4.getFragmentosRecolectados() + " §7(+" + cantidad + ")");
                target.sendMessage("§5§l⚡ Has recibido §e" + cantidad + " §5§lFragmento(s) del Vacío");
                
                // Avisos especiales
                if (evento4.getFragmentosRecolectados() >= 35 && fragmentosAntes < 35) {
                    sender.sendMessage("");
                    sender.sendMessage("§4§l⚠ WARDEN HABILITADO ⚠");
                    sender.sendMessage("§7El Guardián spawneará cuando alguien esté cerca de una anomalía");
                }
                break;
                
            case "testwarden":
                if (evento4 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo evento4 testwarden <jugador>");
                    sender.sendMessage("§7Configura el evento para testear el spawn del Warden");
                    return;
                }
                
                Player targetWarden = plugin.getServer().getPlayer(args[2]);
                if (targetWarden == null) {
                    sender.sendMessage("§cJugador no encontrado: " + args[2]);
                    return;
                }
                
                // 1. Asegurar que estamos en fase RESONANCIA
                if (evento4.getFaseActual() != me.apocalipsis.events.CaminoEndEvent.Fase.RESONANCIA) {
                    evento4.forzarSiguienteFase();
                    if (evento4.getFaseActual() != me.apocalipsis.events.CaminoEndEvent.Fase.RESONANCIA) {
                        evento4.forzarSiguienteFase();
                    }
                }
                
                // 2. Establecer el contador global de fragmentos a 34
                evento4.setFragmentosRecolectados(34);
                
                // 3. Verificar que hay anomalías activas
                int numAnomalias = evento4.getAnomaliasActivas().size();
                if (numAnomalias == 0) {
                    sender.sendMessage("§c⚠ No hay anomalías activas.");
                    sender.sendMessage("§7El evento está intentando generar anomalías...");
                    sender.sendMessage("§7Espera 10-20 segundos o usa §e/avo evento4 next §7si aún estás en ANOMALIAS");
                    return;
                }
                
                sender.sendMessage("§a✓ Anomalías activas detectadas: §e" + numAnomalias);
                
                // 4. Teletransportar al jugador cerca de una anomalía aleatoria
                Location anomaliaLoc = evento4.getAnomaliasActivas().keySet().iterator().next();
                Location tpLoc = anomaliaLoc.clone().add(15, 0, 15);
                tpLoc.setY(tpLoc.getWorld().getHighestBlockYAt(tpLoc) + 1);
                targetWarden.teleport(tpLoc);
                
                // Mostrar distancia a la anomalía
                double distancia = targetWarden.getLocation().distance(anomaliaLoc);
                sender.sendMessage("§7Jugador a §e" + String.format("%.1f", distancia) + " §7bloques de la anomalía");
                sender.sendMessage("§7Coordenadas anomalía: §e" + anomaliaLoc.getBlockX() + ", " + anomaliaLoc.getBlockY() + ", " + anomaliaLoc.getBlockZ());
                
                // 5. Efectos y mensajes
                targetWarden.sendTitle("§4§l⚡ TEST WARDEN ACTIVADO ⚡", "§7Acércate a la anomalía cercana", 10, 60, 20);
                targetWarden.playSound(targetWarden.getLocation(), Sound.ENTITY_WARDEN_EMERGE, 0.8f, 0.7f);
                targetWarden.playSound(targetWarden.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.0f, 0.6f);
                targetWarden.spawnParticle(Particle.SCULK_SOUL, targetWarden.getLocation().add(0, 1, 0), 50, 0.5, 1.0, 0.5, 0.1);
                targetWarden.spawnParticle(Particle.PORTAL, targetWarden.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.5);
                
                // Mensajes al admin
                sender.sendMessage("§a✓ Test del Warden configurado para §e" + targetWarden.getName());
                sender.sendMessage("§7Estado del evento:");
                sender.sendMessage("§7  - Fase: §eRESONANCIA");
                sender.sendMessage("§7  - Fragmentos globales: §e34/40 §7(Warden spawneará a los 35)");
                sender.sendMessage("§7  - Jugador teletransportado cerca de anomalía");
                sender.sendMessage("");
                sender.sendMessage("§6§l⚡ El Warden spawneará cuando:");
                sender.sendMessage("§7  1. El contador llegue a §e35 fragmentos globales");
                sender.sendMessage("§7  2. Alguien esté §ecerca de una anomalía §7(< 20 bloques)");
                sender.sendMessage("");
                sender.sendMessage("§e§lOpciones para activar el spawn:");
                sender.sendMessage("§7  A) Usar §e/avo evento4 fragmentos " + targetWarden.getName() + " 1");
                sender.sendMessage("§7     (Dará 1 fragmento item + incrementará contador a 35)");
                sender.sendMessage("§7  B) Acercarse a la anomalía y usar:");
                sender.sendMessage("§7     §e/avo evento4 setfragmentos 35");
                
                // Mensaje al jugador
                targetWarden.sendMessage("");
                targetWarden.sendMessage("§4§l⚠ ═══ TEST WARDEN ACTIVADO ═══ ⚠");
                targetWarden.sendMessage("");
                targetWarden.sendMessage("§7Has sido teletransportado cerca de una anomalía.");
                targetWarden.sendMessage("§7Fragmentos globales: §e34/40");
                targetWarden.sendMessage("");
                targetWarden.sendMessage("§c§lEl Guardián de las Profundidades §7está a punto de emerger...");
                targetWarden.sendMessage("§7Pide a un admin que use:");
                targetWarden.sendMessage("§e  /avo evento4 fragmentos " + targetWarden.getName() + " 1");
                targetWarden.sendMessage("§7O simplemente acércate más a la anomalía cuando alcancen 35 fragmentos.");
                targetWarden.sendMessage("");
                targetWarden.sendMessage("§8§o\"...Algo terrible está a punto de despertar...\"");
                targetWarden.sendMessage("");
                break;
                
            case "setfragmentos":
                if (evento4 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo evento4 setfragmentos <cantidad>");
                    sender.sendMessage("§7Establece el contador global de fragmentos recolectados");
                    sender.sendMessage("§7Actual: §e" + evento4.getFragmentosRecolectados() + "/40");
                    return;
                }
                
                try {
                    int nuevaCantidad = Integer.parseInt(args[2]);
                    if (nuevaCantidad < 0 || nuevaCantidad > 40) {
                        sender.sendMessage("§cCantidad inválida (0-40).");
                        return;
                    }
                    
                    int anterior = evento4.getFragmentosRecolectados();
                    evento4.setFragmentosRecolectados(nuevaCantidad);
                    
                    sender.sendMessage("§a✓ Fragmentos globales establecidos");
                    sender.sendMessage("§7  De: §e" + anterior + " §7→ §e" + nuevaCantidad);
                    sender.sendMessage("§7  Progreso: §e" + nuevaCantidad + "/40");
                    
                    // Avisos especiales según la cantidad
                    if (nuevaCantidad >= 35 && anterior < 35) {
                        sender.sendMessage("");
                        sender.sendMessage("§4§l⚠ WARDEN HABILITADO ⚠");
                        sender.sendMessage("§7El Warden spawneará cuando alguien esté cerca de una anomalía");
                        sender.sendMessage("§7(Distancia: < 20 bloques)");
                    }
                    
                    if (nuevaCantidad >= 40 && anterior < 40) {
                        sender.sendMessage("");
                        sender.sendMessage("§5§l⚡ OBJETIVO ALCANZADO ⚡");
                        sender.sendMessage("§7El evento debería transicionar a REVELACION");
                        sender.sendMessage("§7(Si el Warden fue derrotado)");
                    }
                    
                    // Efectos visuales
                    for (Player p : plugin.getServer().getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 1.2f);
                        if (nuevaCantidad >= 35 && anterior < 35) {
                            p.spawnParticle(Particle.SCULK_SOUL, p.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                        }
                    }
                    
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cCantidad inválida: " + args[2]);
                }
                break;
                
            case "completarportal":
            case "cliffhanger":
                if (evento4 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                sender.sendMessage("");
                sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                sender.sendMessage("§5§l⚡ COMPLETANDO PORTAL - MODO TEST ⚡");
                sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                sender.sendMessage("");
                
                try {
                    // Paso 1: Establecer fragmentos a 40
                    int fragmentosActuales = evento4.getFragmentosRecolectados();
                    if (fragmentosActuales < 40) {
                        sender.sendMessage("§71. §eEstableciendo fragmentos a 40...");
                        evento4.setFragmentosRecolectados(40);
                        sender.sendMessage("   §a✓ Fragmentos: §e" + fragmentosActuales + " §7→ §e40");
                    } else {
                        sender.sendMessage("§71. §7Fragmentos ya en 40 §a✓");
                    }
                    
                    // Paso 2: Resetear Warden (marca como derrotado)
                    sender.sendMessage("§72. §eMarcando Warden como derrotado...");
                    Class<?> claseEvento = evento4.getClass();
                    java.lang.reflect.Method metodoResetear = claseEvento.getDeclaredMethod("resetearWarden");
                    metodoResetear.setAccessible(true);
                    metodoResetear.invoke(evento4);
                    sender.sendMessage("   §a✓ Warden reseteado");
                    
                    // Paso 3: Forzar fase REVELACION si no está ya
                    sender.sendMessage("§73. §eVerificando fase del evento...");
                    java.lang.reflect.Field faseField = claseEvento.getDeclaredField("faseActual");
                    faseField.setAccessible(true);
                    Object faseActual = faseField.get(evento4);
                    
                    if (!faseActual.toString().equals("REVELACION")) {
                        sender.sendMessage("   §7Forzando transición a REVELACION...");
                        java.lang.reflect.Method metodoForzar = claseEvento.getDeclaredMethod("forzarSiguienteFase");
                        metodoForzar.setAccessible(true);
                        metodoForzar.invoke(evento4);
                        
                        // Si no está en REVELACION aún, forzar una vez más
                        faseActual = faseField.get(evento4);
                        if (!faseActual.toString().equals("REVELACION")) {
                            metodoForzar.invoke(evento4);
                        }
                        sender.sendMessage("   §a✓ Fase: §eREVELACION");
                    } else {
                        sender.sendMessage("   §7Ya está en REVELACION §a✓");
                    }
                    
                    // Paso 4: Ejecutar secuencia de cliffhanger
                    sender.sendMessage("§74. §eEjecutando secuencia de cliffhanger...");
                    sender.sendMessage("");
                    sender.sendMessage("§d§l★ INICIANDO CINEMÁTICA FINAL ★");
                    sender.sendMessage("§7Duración: §e~32 segundos");
                    sender.sendMessage("§7La secuencia revelará el misterio del portal...");
                    sender.sendMessage("");
                    
                    // Variables finales para la lambda
                    final Class<?> claseEventoFinal = claseEvento;
                    final me.apocalipsis.events.CaminoEndEvent evento4Final = evento4;
                    
                    // Esperar 2 segundos para que los jugadores lean el mensaje
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        try {
                            java.lang.reflect.Method metodoCliffhanger = claseEventoFinal.getDeclaredMethod("ejecutarCliffhangerYFinalizar");
                            metodoCliffhanger.setAccessible(true);
                            metodoCliffhanger.invoke(evento4Final);
                            
                            // Mensaje de confirmación solo al admin
                            sender.sendMessage("§a✓ Secuencia de cliffhanger iniciada correctamente");
                            sender.sendMessage("§7Observa el chat para la cinemática completa...");
                            
                        } catch (Exception ex) {
                            sender.sendMessage("§c✗ Error al ejecutar cliffhanger: " + ex.getMessage());
                            ex.printStackTrace();
                        }
                    }, 40L); // 2 segundos de espera
                    
                } catch (Exception e) {
                    sender.sendMessage("§c✗ Error al completar portal:");
                    sender.sendMessage("§c  " + e.getMessage());
                    e.printStackTrace();
                }
                break;
                
            case "getitemsevento4":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
                    return;
                }
                
                Player admin = (Player) sender;
                
                // Obtener el listener de CaminoEnd para acceder a métodos de creación de items
                me.apocalipsis.events.CaminoEndListener listener = null;
                for (HandlerList handlers : HandlerList.getHandlerLists()) {
                    for (RegisteredListener rl : handlers.getRegisteredListeners()) {
                        if (rl.getListener() instanceof me.apocalipsis.events.CaminoEndListener) {
                            listener = (me.apocalipsis.events.CaminoEndListener) rl.getListener();
                            break;
                        }
                    }
                    if (listener != null) break;
                }
                
                if (listener == null) {
                    sender.sendMessage("§cError: No se pudo acceder al sistema de items del evento.");
                    return;
                }
                
                java.util.List<ItemStack> itemsUnicos = new java.util.ArrayList<>();
                
                // Items de anomalías (Enderman guardián)
                try {
                    java.lang.reflect.Method metodoEspada = me.apocalipsis.events.CaminoEndListener.class.getDeclaredMethod("crearEspadaDelVacio");
                    metodoEspada.setAccessible(true);
                    itemsUnicos.add((ItemStack) metodoEspada.invoke(listener));
                    
                    java.lang.reflect.Method metodoPico = me.apocalipsis.events.CaminoEndListener.class.getDeclaredMethod("crearPicoDelVacio");
                    metodoPico.setAccessible(true);
                    itemsUnicos.add((ItemStack) metodoPico.invoke(listener));
                    
                    java.lang.reflect.Method metodoEscudo = me.apocalipsis.events.CaminoEndListener.class.getDeclaredMethod("crearEscudoDimensional");
                    metodoEscudo.setAccessible(true);
                    itemsUnicos.add((ItemStack) metodoEscudo.invoke(listener));
                    
                    java.lang.reflect.Method metodoCasco = me.apocalipsis.events.CaminoEndListener.class.getDeclaredMethod("crearCascoDelObservador");
                    metodoCasco.setAccessible(true);
                    itemsUnicos.add((ItemStack) metodoCasco.invoke(listener));
                    
                    java.lang.reflect.Method metodoPolvo = me.apocalipsis.events.CaminoEndListener.class.getDeclaredMethod("crearPolvoDelVacio");
                    metodoPolvo.setAccessible(true);
                    itemsUnicos.add((ItemStack) metodoPolvo.invoke(listener));
                    
                    // Items del Warden Final
                    java.lang.reflect.Method metodoCorazon = me.apocalipsis.events.CaminoEndListener.class.getDeclaredMethod("crearCorazonDelVacio");
                    metodoCorazon.setAccessible(true);
                    itemsUnicos.add((ItemStack) metodoCorazon.invoke(listener));
                    
                    java.lang.reflect.Method metodoEspadaGuardian = me.apocalipsis.events.CaminoEndListener.class.getDeclaredMethod("crearEspadaDelGuardian");
                    metodoEspadaGuardian.setAccessible(true);
                    itemsUnicos.add((ItemStack) metodoEspadaGuardian.invoke(listener));
                    
                    java.lang.reflect.Method metodoHacha = me.apocalipsis.events.CaminoEndListener.class.getDeclaredMethod("crearHachaDelGuardian");
                    metodoHacha.setAccessible(true);
                    itemsUnicos.add((ItemStack) metodoHacha.invoke(listener));
                    
                    java.lang.reflect.Method metodoPeto = me.apocalipsis.events.CaminoEndListener.class.getDeclaredMethod("crearPetoDelGuardian");
                    metodoPeto.setAccessible(true);
                    itemsUnicos.add((ItemStack) metodoPeto.invoke(listener));
                    
                    java.lang.reflect.Method metodoPantalones = me.apocalipsis.events.CaminoEndListener.class.getDeclaredMethod("crearPantalonesDelGuardian");
                    metodoPantalones.setAccessible(true);
                    itemsUnicos.add((ItemStack) metodoPantalones.invoke(listener));
                    
                } catch (Exception e) {
                    sender.sendMessage("§cError al crear items únicos: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
                
                // Dar todos los items al admin
                for (ItemStack item : itemsUnicos) {
                    admin.getInventory().addItem(item);
                }
                
                // Mensaje de confirmación
                admin.sendMessage("");
                admin.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                admin.sendMessage("§5§l⚡ ITEMS ÚNICOS DEL CAMINO AL END ⚡");
                admin.sendMessage("");
                admin.sendMessage("§7Se han entregado todos los items únicos:");
                admin.sendMessage("");
                admin.sendMessage("§d§lDROPS DE ANOMALÍAS (Enderman):");
                admin.sendMessage("  §8▪ §5Espada del Vacío");
                admin.sendMessage("  §8▪ §5Pico del Vacío");
                admin.sendMessage("  §8▪ §5Escudo Dimensional");
                admin.sendMessage("  §8▪ §5Casco del Observador");
                admin.sendMessage("  §8▪ §dPolvo del Vacío");
                admin.sendMessage("");
                admin.sendMessage("§4§lDROPS DEL GUARDIÁN DE LAS PROFUNDIDADES:");
                admin.sendMessage("  §8▪ §4§lCorazón de las Profundidades §7(LEGENDARIO)");
                admin.sendMessage("  §8▪ §7  §8§o\"Spoiler de un evento futuro...\"");
                admin.sendMessage("  §8▪ §cEspada del Guardián §7(ÉPICO)");
                admin.sendMessage("  §8▪ §cHacha del Guardián §7(ÉPICO)");
                admin.sendMessage("  §8▪ §cPeto del Guardián §7(ÉPICO)");
                admin.sendMessage("  §8▪ §cPantalones del Guardián §7(ÉPICO)");
                admin.sendMessage("");
                admin.sendMessage("§7Total: §e" + itemsUnicos.size() + " items únicos");
                admin.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                admin.sendMessage("");
                
                // Efectos visuales
                Location loc = admin.getLocation();
                admin.getWorld().playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.2f);
                admin.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(0, 1, 0), 100, 0.5, 0.5, 0.5, 1.0);
                admin.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 50, 0.3, 0.3, 0.3, 0.1);
                break;
                
            case "anomalia":
                if (evento4 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                if (args.length < 3 || !args[2].equalsIgnoreCase("spawn")) {
                    sender.sendMessage("§cUso: /avo evento4 anomalia spawn");
                    return;
                }
                
                sender.sendMessage("§a✓ Las anomalías se spawnean automáticamente cada 10 segundos.");
                sender.sendMessage("§7Máximo simultáneas: §e8");
                break;
                
            case "portal":
                if (evento4 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                if (args.length < 3 || !args[2].equalsIgnoreCase("spawn")) {
                    sender.sendMessage("§cUso: /avo evento4 portal spawn");
                    return;
                }
                
                if (evento4.getFaseActual() != me.apocalipsis.events.CaminoEndEvent.Fase.REVELACION) {
                    sender.sendMessage("§e⚠ El portal se genera automáticamente al alcanzar 40 fragmentos.");
                } else {
                    sender.sendMessage("§a✓ El portal ya está generado.");
                }
                break;
            
            case "tp":
            case "teleport":
                if (evento4 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cSolo jugadores pueden usar este comando.");
                    return;
                }
                
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo evento4 tp <portal|anomalia>");
                    return;
                }
                
                Player jugador = (Player) sender;
                String destino = args[2].toLowerCase();
                
                if (destino.equals("portal")) {
                    if (evento4.getFaseActual() != me.apocalipsis.events.CaminoEndEvent.Fase.REVELACION) {
                        sender.sendMessage("§cEl portal aún no ha sido revelado.");
                        return;
                    }
                    
                    // Obtener ubicación del portal desde evento
                    Location portalLoc = null;
                    if (portalLoc == null) {
                        sender.sendMessage("§cNo se pudo encontrar el portal.");
                        return;
                    }
                    
                    jugador.teleport(portalLoc.clone().add(0, 2, 0));
                    sender.sendMessage("§a✓ Teletransportado al §5Portal Incompleto");
                    
                } else if (destino.equals("anomalia")) {
                    if (evento4.getAnomaliasActivas().isEmpty()) {
                        sender.sendMessage("§cNo hay anomalías activas.");
                        return;
                    }
                    
                    // TP a anomalía aleatoria
                    List<Location> anomalias = new ArrayList<>(evento4.getAnomaliasActivas().keySet());
                    Location anomalia = anomalias.get(evento4.getRandom().nextInt(anomalias.size()));
                    
                    jugador.teleport(anomalia.clone().add(0, 2, 0));
                    jugador.playSound(jugador.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
                    jugador.spawnParticle(Particle.PORTAL, jugador.getLocation(), 50, 0.5, 1.0, 0.5, 0.5);
                    
                    sender.sendMessage("§a✓ Teletransportado a una §5Anomalía §7(" + 
                        evento4.getAnomaliasActivas().get(anomalia).tipo.getNombre() + "§7)");
                } else {
                    sender.sendMessage("§cDestino desconocido: §f" + destino);
                }
                break;
                
            default:
                sender.sendMessage("§cSubcomando desconocido: §f" + subCmd);
                sender.sendMessage("§7Usa §e/avo evento4 §7para ver comandos disponibles.");
                break;
        }
    }

    /**
     * Comandos para el Evento Navidad
     * /avo navidad <subcomando>
     */
    private void cmdNavidad(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§c§l✦ ════ EVENTO NAVIDAD ════ ✦");
            sender.sendMessage("§7Un momento de calma en el mundo...");
            sender.sendMessage("");
            sender.sendMessage("§e▸ Control Principal:");
            sender.sendMessage("  §f/avo navidad start §7- Inicia el evento");
            sender.sendMessage("  §f/avo navidad stop §7- Finaliza el evento");
            sender.sendMessage("  §f/avo navidad status §7- Estado del evento");
            sender.sendMessage("  §f/avo navidad reset §7- Reset de emergencia");
            sender.sendMessage("");
            sender.sendMessage("§e▸ Ambiente:");
            sender.sendMessage("  §f/avo navidad ambiente <on|off> §7- Controlar ambiente");
            sender.sendMessage("");
            sender.sendMessage("§e▸ Árbol:");
            sender.sendMessage("  §f/avo navidad arbol set §7- Definir ubicación");
            sender.sendMessage("  §f/avo navidad arbol activar §7- Intensificar efectos");
            sender.sendMessage("  §f/avo navidad arbol desactivar §7- Normalizar");
            sender.sendMessage("");
            sender.sendMessage("§e▸ Santa:");
            sender.sendMessage("  §f/avo navidad santa spawn §7- Aparecer Santa");
            sender.sendMessage("  §f/avo navidad santa despawn §7- Desaparecer Santa");
            sender.sendMessage("");
            sender.sendMessage("§e▸ Regalos:");
            sender.sendMessage("  §f/avo navidad regalos start §7- Activar regalos");
            sender.sendMessage("  §f/avo navidad regalos stop §7- Desactivar regalos");
            sender.sendMessage("");
            sender.sendMessage("§e▸ Fragmentos:");
            sender.sendMessage("  §f/avo navidad fragmentos give <player> <cantidad>");
            sender.sendMessage("  §f/avo navidad fragmentos giveall <cantidad>");
            sender.sendMessage("  §f/avo navidad fragmentos info §7- Ver tus fragmentos");
            sender.sendMessage("");
            sender.sendMessage("§e▸ Especial:");
            sender.sendMessage("  §f/avo navidad cliffhanger §7- Secuencia de cierre");
            sender.sendMessage("");
            sender.sendMessage("§e▸ Amigo Secreto:");
            sender.sendMessage("  §f/avo navidad amigo-secreto §7- Iniciar sorteo");
            sender.sendMessage("  §f/avo navidad miamigo §7- Ver tu amigo secreto asignado");
            sender.sendMessage("  §f/avo navidad entregar [mensaje] §7- Dar a tu amigo secreto");
            sender.sendMessage("  §f/avo navidad regalar <jugador> [mensaje] §7- Dar a quien quieras");
            sender.sendMessage("    §8Ejemplo: /avo navidad entregar ¡Feliz Navidad!");
            sender.sendMessage("    §8Ejemplo: /avo navidad regalar Steve Esto es para ti");
            return;
        }
        
        String subCmd = args[1].toLowerCase();
        
        // Obtener instancia del evento
        me.apocalipsis.events.NavidadEvent navidadEvent = null;
        if (eventController.hasActiveEvent() && 
            eventController.getActiveEvent() instanceof me.apocalipsis.events.NavidadEvent) {
            navidadEvent = (me.apocalipsis.events.NavidadEvent) eventController.getActiveEvent();
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
                
                // Iniciar evento
                if (eventController.startEvent("navidad")) {
                    sender.sendMessage("§a✓ Evento §c§l✦ Navidad ✦ §ainiciado");
                    sender.sendMessage("§7El mundo entra en un momento de calma...");
                    plugin.getLogger().info(String.format("[Navidad] Iniciado por %s", sender.getName()));
                } else {
                    sender.sendMessage("§cNo se pudo iniciar el evento. Verifica la consola.");
                }
                break;
                
            case "stop":
            case "detener":
                if (navidadEvent == null) {
                    sender.sendMessage("§cEl evento Navidad no está activo.");
                    return;
                }
                
                eventController.stopActiveEvent();
                sender.sendMessage("§7✓ Evento §c✦ Navidad ✦ §7finalizado");
                plugin.getLogger().info(String.format("[Navidad] Detenido por %s", sender.getName()));
                break;
                
            case "status":
            case "estado":
                if (navidadEvent == null) {
                    sender.sendMessage("§c§l✦ ═══ NAVIDAD - ESTADO ═══ ✦");
                    sender.sendMessage("§7Estado: §cInactivo");
                    sender.sendMessage("§7Usa §e/avo navidad start §7para iniciarlo.");
                    return;
                }
                
                sender.sendMessage("§c§l✦ ═══ NAVIDAD - ESTADO ═══ ✦");
                sender.sendMessage("§7Estado: §aActivo");
                sender.sendMessage("");
                sender.sendMessage("§7Ambiente: " + (navidadEvent.isAmbienteActivo() ? "§aActivado" : "§cDesactivado"));
                sender.sendMessage("§7Regalos: " + (navidadEvent.isRegalosActivos() ? "§aActivados" : "§cDesactivados"));
                sender.sendMessage("§7Árbol: " + (navidadEvent.isArbolConfigurado() ? 
                    (navidadEvent.isArbolActivado() ? "§a§lINTENSIFICADO" : "§eConfigurado") : "§cNo configurado"));
                sender.sendMessage("§7Santa: " + (navidadEvent.isSantaSpawneado() ? "§aSpawneado" : "§cNo presente"));
                
                if (navidadEvent.isArbolConfigurado()) {
                    Location arbol = navidadEvent.getArbolLocation();
                    sender.sendMessage("");
                    sender.sendMessage("§7Ubicación árbol: §f" + 
                        arbol.getWorld().getName() + " " +
                        arbol.getBlockX() + ", " + arbol.getBlockY() + ", " + arbol.getBlockZ());
                }
                break;
                
            case "reset":
                if (navidadEvent == null) {
                    sender.sendMessage("§cEl evento Navidad no está activo.");
                    return;
                }
                
                navidadEvent.reset();
                sender.sendMessage("§a✓ Evento Navidad reseteado");
                plugin.getLogger().info(String.format("[Navidad] Reset por %s", sender.getName()));
                break;
                
            case "ambiente":
                if (navidadEvent == null) {
                    sender.sendMessage("§cEl evento Navidad no está activo.");
                    return;
                }
                
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo navidad ambiente <on|off>");
                    return;
                }
                
                String ambienteAction = args[2].toLowerCase();
                if (ambienteAction.equals("on")) {
                    navidadEvent.activarAmbiente();
                    sender.sendMessage("§a✓ Ambiente navideño activado");
                } else if (ambienteAction.equals("off")) {
                    navidadEvent.desactivarAmbiente();
                    sender.sendMessage("§c✓ Ambiente navideño desactivado");
                } else {
                    sender.sendMessage("§cUso: /avo navidad ambiente <on|off>");
                }
                break;
                
            case "arbol":
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo navidad arbol <set|activar|desactivar>");
                    return;
                }
                
                String arbolAction = args[2].toLowerCase();
                switch (arbolAction) {
                    case "set":
                        if (!(sender instanceof org.bukkit.entity.Player player)) {
                            sender.sendMessage("§cSolo jugadores pueden usar este comando.");
                            return;
                        }
                        
                        if (navidadEvent == null) {
                            sender.sendMessage("§cEl evento Navidad no está activo.");
                            return;
                        }
                        
                        navidadEvent.establecerArbol(player.getLocation());
                        sender.sendMessage("§a✦ Árbol de Navidad establecido en tu ubicación actual.");
                        plugin.getLogger().info(String.format("[Navidad] Árbol establecido por %s en %s", 
                            player.getName(), player.getLocation()));
                        break;
                        
                    case "activar":
                        if (navidadEvent == null) {
                            sender.sendMessage("§cEl evento Navidad no está activo.");
                            return;
                        }
                        
                        if (!navidadEvent.isArbolConfigurado()) {
                            sender.sendMessage("§c✦ El árbol aún no ha sido configurado.");
                            sender.sendMessage("§7Usa §e/avo navidad arbol set §7primero.");
                            return;
                        }
                        
                        navidadEvent.activarArbol();
                        sender.sendMessage("§a✦ Árbol de Navidad intensificado");
                        break;
                        
                    case "desactivar":
                        if (navidadEvent == null) {
                            sender.sendMessage("§cEl evento Navidad no está activo.");
                            return;
                        }
                        
                        navidadEvent.desactivarArbol();
                        sender.sendMessage("§c✦ Árbol de Navidad normalizado");
                        break;
                        
                    default:
                        sender.sendMessage("§cUso: /avo navidad arbol <set|activar|desactivar>");
                        break;
                }
                break;
                
            case "santa":
                if (navidadEvent == null) {
                    sender.sendMessage("§cEl evento Navidad no está activo.");
                    return;
                }
                
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo navidad santa <spawn|despawn>");
                    return;
                }
                
                String santaAction = args[2].toLowerCase();
                if (santaAction.equals("spawn")) {
                    // Santa ahora es el jugador que ejecuta el comando
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("§c✦ Solo jugadores pueden convertirse en Santa.");
                        return;
                    }
                    
                    Player player = (Player) sender;
                    navidadEvent.convertirEnSanta(player);
                    plugin.getLogger().info(String.format("[Navidad] %s se convirtió en Santa", sender.getName()));
                } else if (santaAction.equals("despawn")) {
                    navidadEvent.quitarSanta();
                    sender.sendMessage("§c✦ Santa ha sido removido...");
                    plugin.getLogger().info(String.format("[Navidad] Santa removido por %s", sender.getName()));
                } else {
                    sender.sendMessage("§cUso: /avo navidad santa <spawn|despawn>");
                }
                break;
                
            case "regalos":
                if (navidadEvent == null) {
                    sender.sendMessage("§cEl evento Navidad no está activo.");
                    return;
                }
                
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo navidad regalos <start|stop>");
                    return;
                }
                
                String regalosAction = args[2].toLowerCase();
                if (regalosAction.equals("start")) {
                    navidadEvent.activarRegalos();
                    sender.sendMessage("§a✦ Los regalos han sido activados.");
                    plugin.getLogger().info(String.format("[Navidad] Regalos activados por %s", sender.getName()));
                } else if (regalosAction.equals("stop")) {
                    navidadEvent.desactivarRegalos();
                    sender.sendMessage("§c✦ Los regalos han sido desactivados.");
                    plugin.getLogger().info(String.format("[Navidad] Regalos desactivados por %s", sender.getName()));
                } else {
                    sender.sendMessage("§cUso: /avo navidad regalos <start|stop>");
                }
                break;
                
            case "fragmentos":
            case "fragmento":
                if (args.length < 3) {
                    if (sender instanceof org.bukkit.entity.Player player && navidadEvent != null) {
                        navidadEvent.mostrarInfoFragmentos(player);
                        return;
                    }
                    sender.sendMessage("§cUso: /avo navidad fragmentos <give|giveall|info>");
                    return;
                }
                
                if (navidadEvent == null) {
                    sender.sendMessage("§cEl evento Navidad no está activo.");
                    return;
                }
                
                String fragmentosAction = args[2].toLowerCase();
                switch (fragmentosAction) {
                    case "give":
                        if (args.length < 5) {
                            sender.sendMessage("§cUso: /avo navidad fragmentos give <player> <cantidad>");
                            return;
                        }
                        
                        org.bukkit.entity.Player targetPlayer = plugin.getServer().getPlayer(args[3]);
                        if (targetPlayer == null) {
                            sender.sendMessage("§cJugador no encontrado: §f" + args[3]);
                            return;
                        }
                        
                        int cantidad;
                        try {
                            cantidad = Integer.parseInt(args[4]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage("§cCantidad inválida: §f" + args[4]);
                            return;
                        }
                        
                        if (cantidad <= 0) {
                            sender.sendMessage("§cLa cantidad debe ser mayor a 0");
                            return;
                        }
                        
                        navidadEvent.darFragmentos(targetPlayer, cantidad);
                        sender.sendMessage("§a✓ Entregados §d" + cantidad + " §afragmento(s) a §f" + targetPlayer.getName());
                        plugin.getLogger().info(String.format("[Navidad] %s entregó %d fragmentos a %s", 
                            sender.getName(), cantidad, targetPlayer.getName()));
                        break;
                        
                    case "giveall":
                        if (args.length < 4) {
                            sender.sendMessage("§cUso: /avo navidad fragmentos giveall <cantidad>");
                            return;
                        }
                        
                        int cantidadAll;
                        try {
                            cantidadAll = Integer.parseInt(args[3]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage("§cCantidad inválida: §f" + args[3]);
                            return;
                        }
                        
                        if (cantidadAll <= 0) {
                            sender.sendMessage("§cLa cantidad debe ser mayor a 0");
                            return;
                        }
                        
                        navidadEvent.darFragmentosTodos(cantidadAll);
                        int jugadoresTotal = plugin.getServer().getOnlinePlayers().size();
                        sender.sendMessage("§a✓ Entregados §d" + cantidadAll + " §afragmento(s) a §f" + 
                            jugadoresTotal + " §ajugador(es)");
                        plugin.getLogger().info(String.format("[Navidad] %s entregó %d fragmentos a todos (%d jugadores)", 
                            sender.getName(), cantidadAll, jugadoresTotal));
                        break;
                        
                    case "info":
                        if (!(sender instanceof org.bukkit.entity.Player player)) {
                            sender.sendMessage("§cSolo jugadores pueden ver sus fragmentos.");
                            return;
                        }
                        
                        navidadEvent.mostrarInfoFragmentos(player);
                        break;
                        
                    default:
                        sender.sendMessage("§cUso: /avo navidad fragmentos <give|giveall|info>");
                        break;
                }
                break;
                
            case "cliffhanger":
                if (navidadEvent == null) {
                    sender.sendMessage("§cEl evento Navidad no está activo.");
                    return;
                }
                
                navidadEvent.activarCliffhanger();
                sender.sendMessage("§8§o...activando secuencia de cierre...");
                plugin.getLogger().info(String.format("[Navidad] Cliffhanger activado por %s", sender.getName()));
                break;
                
            case "amigo-secreto":
            case "sorteo":
                if (navidadEvent == null) {
                    sender.sendMessage("§cEl evento Navidad no está activo.");
                    return;
                }
                
                navidadEvent.iniciarAmigoSecreto();
                sender.sendMessage("§a§l✦ Amigo Secreto iniciado");
                sender.sendMessage("§7Se han asignado los amigos secretos aleatoriamente.");
                sender.sendMessage("§7Cada jugador recibirá un mensaje privado con su asignación.");
                plugin.getLogger().info(String.format("[Navidad] Amigo Secreto iniciado por %s", sender.getName()));
                break;
                
            case "miamigo":
            case "mi-amigo":
            case "veramigo":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cSolo jugadores pueden ver su amigo secreto.");
                    return;
                }
                
                if (navidadEvent == null) {
                    sender.sendMessage("§cEl evento Navidad no está activo.");
                    return;
                }
                
                navidadEvent.mostrarAmigoSecreto(player);
                break;
                
            case "entregar":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cSolo jugadores pueden entregar regalos.");
                    return;
                }
                
                if (navidadEvent == null) {
                    sender.sendMessage("§cEl evento Navidad no está activo.");
                    return;
                }
                
                // Obtener mensaje opcional (todo después de "entregar")
                String mensajePersonal = null;
                if (args.length > 2) {
                    // Unir todos los argumentos después de "entregar" como mensaje
                    mensajePersonal = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                }
                
                navidadEvent.entregarRegaloAmigoSecreto(player, mensajePersonal);
                break;
                
            case "regalar":
                if (!(sender instanceof Player playerRegala)) {
                    sender.sendMessage("§cSolo jugadores pueden dar regalos.");
                    return;
                }
                
                if (navidadEvent == null) {
                    sender.sendMessage("§cEl evento Navidad no está activo.");
                    return;
                }
                
                if (args.length < 3) {
                    sender.sendMessage("§cUso: §e/avo navidad regalar <jugador> [mensaje]");
                    return;
                }
                
                Player receptor = Bukkit.getPlayer(args[2]);
                if (receptor == null || !receptor.isOnline()) {
                    sender.sendMessage("§cEl jugador §e" + args[2] + " §cno está conectado.");
                    return;
                }
                
                // Obtener mensaje opcional (todo después del nombre del jugador)
                String mensajeRegalo = null;
                if (args.length > 3) {
                    mensajeRegalo = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                }
                
                navidadEvent.regalarAJugador(playerRegala, receptor, mensajeRegalo);
                break;
                
            default:
                sender.sendMessage("§cSubcomando desconocido: §f" + subCmd);
                sender.sendMessage("§7Usa §e/avo navidad §7para ver comandos disponibles.");
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
            sender.sendMessage("§e/avo evasion reputation <jugador> §7- Ver reputación de un jugador");
            sender.sendMessage("§e/avo evasion clear <jugador|all> §7- Limpiar evasiones");
            sender.sendMessage("§e/avo evasion stats §7- Ver estadísticas globales");
            sender.sendMessage("§e/avo evasion live §7- Estadísticas del desastre actual");
            sender.sendMessage("§e/avo evasion atrisk §7- Jugadores en riesgo de evasión");
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
            
            case "reputation":
            case "rep":
                if (args.length < 3) {
                    sender.sendMessage("§cUso: /avo evasion reputation <jugador>");
                    return;
                }
                
                Player targetRep = plugin.getServer().getPlayer(args[2]);
                if (targetRep == null) {
                    sender.sendMessage("§cJugador no encontrado.");
                    return;
                }
                
                String repInfo = plugin.getDisasterEvasionTracker().getReputationInfo(targetRep.getUniqueId());
                sender.sendMessage(repInfo);
                break;
            
            case "live":
                String liveStats = plugin.getDisasterEvasionTracker().getCurrentDisasterStats();
                sender.sendMessage(liveStats);
                break;
            
            case "atrisk":
            case "risk":
                java.util.List<String> atRisk = plugin.getDisasterEvasionTracker().getPlayersAtRisk();
                sender.sendMessage("§e§l━━━ JUGADORES EN RIESGO ━━━");
                if (atRisk.isEmpty()) {
                    sender.sendMessage("§a✓ Todos los jugadores están seguros");
                } else {
                    sender.sendMessage("§c" + atRisk.size() + " jugadores en riesgo:");
                    for (String player : atRisk) {
                        sender.sendMessage("§7  • " + player);
                    }
                }
                sender.sendMessage("§e§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
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
                sender.sendMessage("§cSubcomando desconocido. Usa: check, reputation, clear, stats, live, atrisk, history, reduce, info o reload");
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
                java.util.List<TestResult> results = autoTestSystem.getTestResults();
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
                
            case "reload":
            case "recargar":
            case "refresh":
                cmdHabilidadesReload(player);
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
        java.util.Set<Skill> skills = skillService.getUnlockedSkills(player);
        
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
        java.util.List<Skill> toggleables = skillService.getToggleableSkills(player);
        
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
    
    private void cmdHabilidadesReload(Player player) {
        player.sendMessage("§6§l[HABILIDADES] §7Recargando tus habilidades...");
        
        try {
            // Forzar limpieza de cache si existe
            plugin.getSkillService().clearPlayerCache(player.getUniqueId());
            
            // Reaplicar todos los efectos
            plugin.getSkillService().applySkillEffects(player);
            
            // Mensaje de éxito
            player.sendMessage("§a✓ Habilidades recargadas correctamente.");
            player.sendMessage("§7Tus efectos han sido reaplicados.");
            
            // Sonido de confirmación
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
            
            // Log para debugging
            plugin.getLogger().info("[Skills] Habilidades recargadas para " + player.getName());
            
        } catch (Exception e) {
            player.sendMessage("§c✗ Error al recargar habilidades: " + e.getMessage());
            plugin.getLogger().warning("[Skills] Error recargando habilidades para " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
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
                    sender.sendMessage("§cUso: /avo mochila ver <jugador> [mundo] [#mochila]");
                    return;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
                    return;
                }
                
                String targetName = args[2];
                String worldName = args.length > 3 ? args[3] : null;
                int backpackNumber = args.length > 4 ? tryParseInt(args[4], 1) : 1;
                
                Player target = Bukkit.getPlayer(targetName);
                
                if (target != null) {
                    if (worldName != null) {
                        plugin.getBackpackService().openBackpackAsAdmin(player, target.getUniqueId(), 
                            target.getName(), backpackNumber, worldName);
                    } else {
                        plugin.getBackpackService().openBackpackAsAdmin(player, target.getUniqueId(), 
                            target.getName(), backpackNumber);
                    }
                } else {
                    // Buscar jugador offline
                    @SuppressWarnings("deprecation")
                    org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
                    if (offline.hasPlayedBefore()) {
                        String name = offline.getName() != null ? offline.getName() : targetName;
                        if (worldName != null) {
                            plugin.getBackpackService().openBackpackAsAdmin(player, offline.getUniqueId(), 
                                name, backpackNumber, worldName);
                        } else {
                            plugin.getBackpackService().openBackpackAsAdmin(player, offline.getUniqueId(), 
                                name, backpackNumber);
                        }
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
                    sender.sendMessage("§cUso: /avo mochila vaciar <jugador> [mundo] [#mochila]");
                    return;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
                    return;
                }
                
                String targetName = args[2];
                String worldName = args.length > 3 ? args[3] : null;
                int backpackNumber = args.length > 4 ? tryParseInt(args[4], 1) : 1;
                
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
                
                if (offline.hasPlayedBefore() || offline.isOnline()) {
                    boolean cleared;
                    if (worldName != null) {
                        cleared = plugin.getBackpackService().clearBackpack(offline.getUniqueId(), 
                            player, backpackNumber, worldName);
                    } else {
                        cleared = plugin.getBackpackService().clearBackpack(offline.getUniqueId(), 
                            player, backpackNumber);
                    }
                    
                    if (cleared) {
                        String worldInfo = worldName != null ? " en §b" + worldName : "";
                        sender.sendMessage("§a✓ Mochila #" + backpackNumber + " de §e" + targetName + worldInfo + " §avaciada.");
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
                    sender.sendMessage("  §e/avo mochila ver <jugador> [mundo] [#] §7- Ver mochila ajena");
                    sender.sendMessage("  §e/avo mochila lista §7- Listar mochilas");
                }
                if (sender.hasPermission("apocalipsis.mochila.admin")) {
                    sender.sendMessage("  §e/avo mochila vaciar <jugador> [mundo] [#] §7- Vaciar mochila");
                }
                sender.sendMessage("§7El parámetro [mundo] permite ver mochilas de ciclos específicos");
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
    
    /**
     * Canjear tokens de stream por recompensas
     * Uso: /avo canjear [recompensa]
     */
    private void cmdCanjear(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return;
        }
        
        me.apocalipsis.missions.StreamFeaturesManager streamManager = plugin.getStreamFeaturesManager();
        
        if (args.length < 2) {
            // Mostrar menú de canje
            streamManager.showRedeemMenu(player);
            return;
        }
        
        String rewardId = args[1].toLowerCase();
        
        // Intentar canjear
        boolean success = streamManager.redeemReward(player, rewardId);
        
        if (success) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }
    
    /**
     * Comando /avo onboarding - Gestión del sistema de onboarding
     * Uso: /avo onboarding <check|reset|complete|stats|milestone> [jugador] [...]
     */
    private void cmdOnboarding(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§6Uso: /avo onboarding <check|reset|complete|stats|milestone|misiones> [jugador]");
            return;
        }
        
        if (plugin.getTutorialManager() == null || plugin.getTutorialManager().getOnboardingManager() == null) {
            sender.sendMessage("§cSistema de onboarding no disponible.");
            return;
        }
        
        me.apocalipsis.tutorial.OnboardingManager onboarding = plugin.getTutorialManager().getOnboardingManager();
        String subCmd = args[1].toLowerCase();
        
        switch (subCmd) {
            case "misiones":
            case "hitos":
                // Comando público para ver hitos propios o de otro jugador
                String targetName = (args.length >= 3) ? args[2] : sender.getName();
                cmdOnboardingMisiones(sender, targetName, onboarding);
                break;
            case "check":
                if (!sender.hasPermission("avo.admin")) {
                    sender.sendMessage("§cNo tienes permisos.");
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage("§6Uso: /avo onboarding check <jugador>");
                    return;
                }
                cmdOnboardingCheck(sender, args[2], onboarding);
                break;
            case "reset":
                if (!sender.hasPermission("avo.admin")) {
                    sender.sendMessage("§cNo tienes permisos.");
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage("§6Uso: /avo onboarding reset <jugador>");
                    return;
                }
                cmdOnboardingReset(sender, args[2], onboarding);
                break;
            case "complete":
                if (!sender.hasPermission("avo.admin")) {
                    sender.sendMessage("§cNo tienes permisos.");
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage("§6Uso: /avo onboarding complete <jugador>");
                    return;
                }
                cmdOnboardingComplete(sender, args[2], onboarding);
                break;
            case "stats":
                if (!sender.hasPermission("avo.admin")) {
                    sender.sendMessage("§cNo tienes permisos.");
                    return;
                }
                cmdOnboardingStats(sender, onboarding);
                break;
            case "milestone":
                if (!sender.hasPermission("avo.admin")) {
                    sender.sendMessage("§cNo tienes permisos.");
                    return;
                }
                if (args.length < 4) {
                    sender.sendMessage("§6Uso: /avo onboarding milestone <walk|craft|shelter|mission|disaster> <jugador>");
                    return;
                }
                cmdOnboardingMilestone(sender, args[2], args[3], onboarding);
                break;
            default:
                sender.sendMessage("§cSubcomando desconocido. Usa: misiones, check, reset, complete, stats, milestone");
        }
    }
    
    private void cmdOnboardingCheck(CommandSender sender, String playerName, me.apocalipsis.tutorial.OnboardingManager onboarding) {
        org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(playerName);
        java.util.UUID uuid = offlinePlayer.getUniqueId();
        
        me.apocalipsis.tutorial.OnboardingManager.OnboardingProgress progress = onboarding.getProgress(uuid);
        
        if (progress == null) {
            sender.sendMessage("§e" + playerName + "§7: No tiene onboarding en progreso (ya completado o nunca iniciado)");
            return;
        }
        
        sender.sendMessage("§6═══════════════════════════════════════");
        sender.sendMessage("§e§l  Onboarding: " + playerName);
        sender.sendMessage("§6═══════════════════════════════════════");
        sender.sendMessage("§7Bloques caminados: §f" + progress.getBlocksWalked() + "§7/100");
        sender.sendMessage("§7Primer craft: " + (progress.hasCrafted() ? "§a✓" : "§c✗"));
        sender.sendMessage("§7Bloques colocados: §f" + progress.getBlocksPlaced() + "§7/15");
        sender.sendMessage("§7Primera misión: " + (progress.hasCompletedMission() ? "§a✓" : "§c✗"));
        sender.sendMessage("§7Sobrevivió desastre: " + (progress.hasSurvivedDisaster() ? "§a✓" : "§c✗"));
        sender.sendMessage("§7Estado: " + (progress.isFullyCompleted() ? "§a§lCOMPLETADO" : "§cEN PROGRESO"));
        sender.sendMessage("§6═══════════════════════════════════════");
    }
    
    private void cmdOnboardingReset(CommandSender sender, String playerName, me.apocalipsis.tutorial.OnboardingManager onboarding) {
        org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(playerName);
        java.util.UUID uuid = offlinePlayer.getUniqueId();
        
        onboarding.removePlayer(uuid);
        sender.sendMessage("§a✓ Onboarding reiniciado para " + playerName);
        
        org.bukkit.entity.Player onlinePlayer = plugin.getServer().getPlayer(uuid);
        if (onlinePlayer != null) {
            onboarding.startOnboarding(onlinePlayer);
            onlinePlayer.sendMessage("§a§l✓ Tu progreso de onboarding ha sido reiniciado.");
        }
    }
    
    private void cmdOnboardingComplete(CommandSender sender, String playerName, me.apocalipsis.tutorial.OnboardingManager onboarding) {
        org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(playerName);
        java.util.UUID uuid = offlinePlayer.getUniqueId();
        
        me.apocalipsis.tutorial.OnboardingManager.OnboardingProgress progress = onboarding.getProgress(uuid);
        
        if (progress == null) {
            sender.sendMessage("§c" + playerName + " no tiene onboarding activo.");
            return;
        }
        
        if (progress.isFullyCompleted()) {
            sender.sendMessage("§e" + playerName + "§7 ya había completado el onboarding.");
            return;
        }
        
        // Forzar completación de todos los hitos
        progress.complete(me.apocalipsis.tutorial.OnboardingManager.OnboardingMilestone.WALK_100_BLOCKS);
        progress.complete(me.apocalipsis.tutorial.OnboardingManager.OnboardingMilestone.CRAFT_FIRST_ITEM);
        progress.complete(me.apocalipsis.tutorial.OnboardingManager.OnboardingMilestone.BUILD_SHELTER);
        progress.complete(me.apocalipsis.tutorial.OnboardingManager.OnboardingMilestone.COMPLETE_FIRST_MISSION);
        progress.complete(me.apocalipsis.tutorial.OnboardingManager.OnboardingMilestone.SURVIVE_TUTORIAL_DISASTER);
        
        sender.sendMessage("§a✓ Onboarding completado para " + playerName);
        
        org.bukkit.entity.Player onlinePlayer = plugin.getServer().getPlayer(uuid);
        if (onlinePlayer != null) {
            onlinePlayer.sendMessage("§a§l✓ Tu onboarding ha sido completado.");
        }
    }
    
    private void cmdOnboardingStats(CommandSender sender, me.apocalipsis.tutorial.OnboardingManager onboarding) {
        sender.sendMessage("§6═══════════════════════════════════════");
        sender.sendMessage("§e§l  Estadísticas de Onboarding");
        sender.sendMessage("§6═══════════════════════════════════════");
        sender.sendMessage("§7Nota: Estadísticas en memoria (no persistentes)");
        sender.sendMessage("§6═══════════════════════════════════════");
    }
    
    private void cmdOnboardingMisiones(CommandSender sender, String playerName, me.apocalipsis.tutorial.OnboardingManager onboarding) {
        org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(playerName);
        java.util.UUID uuid = offlinePlayer.getUniqueId();
        
        if (onboarding.hasCompletedOnboarding(uuid)) {
            sender.sendMessage("§a✓ " + playerName + " ya completó el onboarding.");
            return;
        }
        
        me.apocalipsis.tutorial.OnboardingManager.OnboardingProgress progress = onboarding.getProgress(uuid);
        if (progress == null) {
            sender.sendMessage("§c" + playerName + " no tiene onboarding activo.");
            return;
        }
        
        sender.sendMessage("");
        sender.sendMessage("§6§l╔═══════════════════════════════════════╗");
        sender.sendMessage("§6§l║      🎯 HITOS DE TUTORIAL 🎯        ║");
        sender.sendMessage("§6§l╚═══════════════════════════════════════╝");
        sender.sendMessage("§7Jugador: §e" + playerName);
        sender.sendMessage("");
        
        // Mostrar progreso de cada hito
        sender.sendMessage(formatMilestone("Caminar 100 bloques", progress.isCompleted(me.apocalipsis.tutorial.OnboardingManager.OnboardingMilestone.WALK_100_BLOCKS)));
        sender.sendMessage(formatMilestone("Craftear primer item", progress.isCompleted(me.apocalipsis.tutorial.OnboardingManager.OnboardingMilestone.CRAFT_FIRST_ITEM)));
        sender.sendMessage(formatMilestone("Construir refugio", progress.isCompleted(me.apocalipsis.tutorial.OnboardingManager.OnboardingMilestone.BUILD_SHELTER)));
        sender.sendMessage(formatMilestone("Completar primera misión", progress.isCompleted(me.apocalipsis.tutorial.OnboardingManager.OnboardingMilestone.COMPLETE_FIRST_MISSION)));
        sender.sendMessage(formatMilestone("Sobrevivir desastre", progress.isCompleted(me.apocalipsis.tutorial.OnboardingManager.OnboardingMilestone.SURVIVE_TUTORIAL_DISASTER)));
        sender.sendMessage("");
        
        int completados = progress.getCompletedCount();
        sender.sendMessage("§7Progreso: §e" + completados + "§7/§f5 hitos completados");
        sender.sendMessage("");
    }
    
    private String formatMilestone(String name, boolean completed) {
        if (completed) {
            return "§a✓ §7" + name + " §8(completado)";
        } else {
            return "§c✗ §7" + name + " §8(pendiente)";
        }
    }
    
    private void cmdOnboardingMilestone(CommandSender sender, String milestone, String playerName, me.apocalipsis.tutorial.OnboardingManager onboarding) {
        org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(playerName);
        java.util.UUID uuid = offlinePlayer.getUniqueId();
        
        me.apocalipsis.tutorial.OnboardingManager.OnboardingProgress progress = onboarding.getProgress(uuid);
        
        if (progress == null) {
            sender.sendMessage("§c" + playerName + " no tiene onboarding activo.");
            return;
        }
        
        me.apocalipsis.tutorial.OnboardingManager.OnboardingMilestone hito;
        try {
            hito = me.apocalipsis.tutorial.OnboardingManager.OnboardingMilestone.valueOf(milestone.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cHito desconocido: " + milestone);
            return;
        }
        
        if (!progress.isCompleted(hito)) {
            progress.complete(hito);
            sender.sendMessage("§a✓ Hito '" + hito.getDisplayName() + "' completado para " + playerName);
        } else {
            sender.sendMessage("§e" + playerName + "§7 ya había completado ese hito.");
        }
    }
    
    /**
     * Comando /avo buddy - Gestión del sistema de buddy (mentor/aprendiz)
     * Uso: /avo buddy <match|unmatch|info|list|stats|rewards> [jugador] [...]
     */
    private void cmdBuddy(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§6Uso: /avo buddy <match|unmatch|info|list|stats|rewards|diagnose> [jugador]");
            return;
        }
        
        if (plugin.getTutorialManager() == null || plugin.getTutorialManager().getBuddyService() == null) {
            sender.sendMessage("§cSistema de buddy no disponible.");
            return;
        }
        
        me.apocalipsis.tutorial.BuddyService buddy = plugin.getTutorialManager().getBuddyService();
        String subCmd = args[1].toLowerCase();
        
        switch (subCmd) {
            case "match":
                if (args.length < 4) {
                    sender.sendMessage("§6Uso: /avo buddy match <aprendiz> <mentor>");
                    return;
                }
                cmdBuddyMatch(sender, args[2], args[3], buddy);
                break;
            case "unmatch":
                if (args.length < 3) {
                    sender.sendMessage("§6Uso: /avo buddy unmatch <jugador>");
                    return;
                }
                cmdBuddyUnmatch(sender, args[2], buddy);
                break;
            case "info":
                if (args.length < 3) {
                    sender.sendMessage("§6Uso: /avo buddy info <jugador>");
                    return;
                }
                cmdBuddyInfo(sender, args[2], buddy);
                break;
            case "list":
                cmdBuddyList(sender, buddy);
                break;
            case "stats":
                cmdBuddyStats(sender, buddy);
                break;
            case "rewards":
                if (args.length < 3) {
                    sender.sendMessage("§6Uso: /avo buddy rewards <mentor>");
                    return;
                }
                cmdBuddyRewards(sender, args[2], buddy);
                break;
            case "diagnose":
                cmdBuddyDiagnose(sender, buddy);
                break;
            default:
                sender.sendMessage("§cSubcomando desconocido. Usa: match, unmatch, info, list, stats, rewards, diagnose");
        }
    }
    
    private void cmdBuddyMatch(CommandSender sender, String apprenticeName, String mentorName, me.apocalipsis.tutorial.BuddyService buddy) {
        org.bukkit.OfflinePlayer apprentice = plugin.getServer().getOfflinePlayer(apprenticeName);
        org.bukkit.OfflinePlayer mentor = plugin.getServer().getOfflinePlayer(mentorName);
        
        buddy.matchBuddy(apprentice.getUniqueId(), mentor.getUniqueId());
        sender.sendMessage("§a✓ " + apprenticeName + " emparejado con mentor " + mentorName);
        
        org.bukkit.entity.Player onlineApprentice = plugin.getServer().getPlayer(apprentice.getUniqueId());
        if (onlineApprentice != null) {
            onlineApprentice.sendMessage("§a§l✓ §fHas sido emparejado con mentor §e" + mentorName);
        }
        
        org.bukkit.entity.Player onlineMentor = plugin.getServer().getPlayer(mentor.getUniqueId());
        if (onlineMentor != null) {
            onlineMentor.sendMessage("§a§l✓ §fTienes un nuevo aprendiz: §e" + apprenticeName);
        }
    }
    
    private void cmdBuddyUnmatch(CommandSender sender, String playerName, me.apocalipsis.tutorial.BuddyService buddy) {
        org.bukkit.OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerName);
        buddy.unmatchBuddy(player.getUniqueId());
        sender.sendMessage("§a✓ Emparejamiento removido para " + playerName);
    }
    
    private void cmdBuddyInfo(CommandSender sender, String playerName, me.apocalipsis.tutorial.BuddyService buddy) {
        org.bukkit.OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerName);
        java.util.UUID uuid = player.getUniqueId();
        
        java.util.Map<String, Object> info = buddy.getBuddyInfo(uuid);
        
        if (info.isEmpty()) {
            sender.sendMessage("§c" + playerName + " no tiene buddy activo");
            return;
        }
        
        sender.sendMessage("§6═══════════════════════════════════════");
        sender.sendMessage("§e§l  Info Buddy: " + playerName);
        sender.sendMessage("§6═══════════════════════════════════════");
        
        String role = (String) info.get("role");
        if ("apprentice".equals(role)) {
            sender.sendMessage("§7Rol: §aAprendiz");
            sender.sendMessage("§7Mentor: §6" + info.get("mentorName"));
            if (info.containsKey("daysRemaining")) {
                long days = (Long) info.get("daysRemaining");
                sender.sendMessage("§7Tiempo restante: §e" + days + " días");
            }
        } else if ("mentor".equals(role)) {
            sender.sendMessage("§7Rol: §6Mentor");
            sender.sendMessage("§7Aprendiz: §a" + info.get("apprenticeName"));
            if (info.containsKey("daysRemaining")) {
                long days = (Long) info.get("daysRemaining");
                sender.sendMessage("§7Tiempo restante: §e" + days + " días");
            }
            
            if (info.containsKey("stats")) {
                me.apocalipsis.tutorial.BuddyService.BuddyStats stats = 
                    (me.apocalipsis.tutorial.BuddyService.BuddyStats) info.get("stats");
                sender.sendMessage("§6─────────────────────────────────────");
                sender.sendMessage("§7Recompensas generadas:");
                sender.sendMessage("  §7Misiones: §f" + stats.getMissionsRewarded());
                sender.sendMessage("  §7Rank ups: §f" + stats.getRankUpsRewarded());
                sender.sendMessage("  §7Desastres: §f" + stats.getDisastersRewarded());
                sender.sendMessage("  §7Tiempo juntos: §f" + stats.getDailyTimeRewarded());
                sender.sendMessage("  §7Total PS: §e" + stats.getTotalPsEarned());
                sender.sendMessage("  §7Total XP: §e" + stats.getTotalXpEarned());
            }
        }
        
        sender.sendMessage("§6═══════════════════════════════════════");
    }
    
    private void cmdBuddyList(CommandSender sender, me.apocalipsis.tutorial.BuddyService buddy) {
        java.util.Map<java.util.UUID, java.util.UUID> pairs = buddy.getAllBuddyPairs();
        
        sender.sendMessage("§6═══════════════════════════════════════");
        sender.sendMessage("§e§l  Emparejamientos Activos (" + pairs.size() + ")");
        sender.sendMessage("§6═══════════════════════════════════════");
        
        if (pairs.isEmpty()) {
            sender.sendMessage("§7No hay pares activos actualmente");
        } else {
            for (java.util.Map.Entry<java.util.UUID, java.util.UUID> entry : pairs.entrySet()) {
                java.util.UUID apprenticeUuid = entry.getKey();
                java.util.UUID mentorUuid = entry.getValue();
                
                String apprenticeName = plugin.getServer().getOfflinePlayer(apprenticeUuid).getName();
                String mentorName = plugin.getServer().getOfflinePlayer(mentorUuid).getName();
                
                java.util.Map<String, Object> info = buddy.getBuddyInfo(apprenticeUuid);
                String timeLeft = "";
                if (info.containsKey("daysRemaining")) {
                    long days = (Long) info.get("daysRemaining");
                    timeLeft = " §7(" + days + " días)";
                }
                
                sender.sendMessage("  §a" + apprenticeName + " §7→ §6" + mentorName + timeLeft);
            }
        }
        
        sender.sendMessage("§6═══════════════════════════════════════");
    }
    
    private void cmdBuddyStats(CommandSender sender, me.apocalipsis.tutorial.BuddyService buddy) {
        java.util.Map<String, Integer> stats = buddy.getGlobalStats();
        
        sender.sendMessage("§6═══════════════════════════════════════");
        sender.sendMessage("§e§l  Estadísticas Buddy");
        sender.sendMessage("§6═══════════════════════════════════════");
        sender.sendMessage("§7Pares activos: §f" + stats.get("activePairs"));
        sender.sendMessage("§6─────────────────────────────────────");
        sender.sendMessage("§7Recompensas totales otorgadas:");
        sender.sendMessage("  §7Por misiones: §f" + stats.get("totalMissions"));
        sender.sendMessage("  §7Por rank ups: §f" + stats.get("totalRankUps"));
        sender.sendMessage("  §7Por desastres: §f" + stats.get("totalDisasters"));
        sender.sendMessage("  §7Por tiempo juntos: §f" + stats.get("totalDailyTime"));
        sender.sendMessage("§6─────────────────────────────────────");
        sender.sendMessage("§7Total PS generado: §e" + stats.get("totalPs"));
        sender.sendMessage("§7Total XP generado: §e" + stats.get("totalXp"));
        sender.sendMessage("§6═══════════════════════════════════════");
    }
    
    private void cmdBuddyRewards(CommandSender sender, String mentorName, me.apocalipsis.tutorial.BuddyService buddy) {
        org.bukkit.OfflinePlayer mentor = plugin.getServer().getOfflinePlayer(mentorName);
        java.util.UUID mentorUuid = mentor.getUniqueId();
        
        me.apocalipsis.tutorial.BuddyService.BuddyStats stats = buddy.getMentorStats(mentorUuid);
        
        sender.sendMessage("§6═══════════════════════════════════════");
        sender.sendMessage("§e§l  Recompensas: " + mentorName);
        sender.sendMessage("§6═══════════════════════════════════════");
        
        if (stats.getTotalRewards() == 0) {
            sender.sendMessage("§7No hay recompensas registradas");
        } else {
            sender.sendMessage("§7Desglose de recompensas:");
            sender.sendMessage("  §7Misiones completadas: §f" + stats.getMissionsRewarded() + 
                " §7(§e" + (stats.getMissionsRewarded() * 25) + " PS§7, §e" + (stats.getMissionsRewarded() * 50) + " XP§7)");
            sender.sendMessage("  §7Rank ups: §f" + stats.getRankUpsRewarded() + 
                " §7(§e" + (stats.getRankUpsRewarded() * 100) + " PS§7, §e" + (stats.getRankUpsRewarded() * 100) + " XP§7)");
            sender.sendMessage("  §7Desastres: §f" + stats.getDisastersRewarded() + 
                " §7(§e" + (stats.getDisastersRewarded() * 50) + " PS§7, §e" + (stats.getDisastersRewarded() * 50) + " XP§7)");
            sender.sendMessage("  §7Tiempo juntos: §f" + stats.getDailyTimeRewarded() + 
                " §7(§e" + (stats.getDailyTimeRewarded() * 25) + " PS§7, §e" + (stats.getDailyTimeRewarded() * 25) + " XP§7)");
            sender.sendMessage("§6─────────────────────────────────────");
            sender.sendMessage("§7Total acumulado:");
            sender.sendMessage("  §7PS: §e" + stats.getTotalPsEarned());
            sender.sendMessage("  §7XP: §e" + stats.getTotalXpEarned());
        }
        
        sender.sendMessage("§6═══════════════════════════════════════");
    }
    
    private void cmdBuddyDiagnose(CommandSender sender, me.apocalipsis.tutorial.BuddyService buddy) {
        // Obtener información de diagnóstico
        java.util.Map<String, Object> info = buddy.getDiagnosticInfo();
        java.util.List<String> issues = buddy.validateConfiguration();
        
        sender.sendMessage("§6═══════════════════════════════════════");
        sender.sendMessage("§e§l  Diagnóstico del Sistema Buddy");
        sender.sendMessage("§6═══════════════════════════════════════");
        
        // Configuración del sistema
        sender.sendMessage("§7Configuración de rangos:");
        sender.sendMessage("  §7Total rangos: §f" + info.get("totalRanks"));
        sender.sendMessage("  §7Rango mínimo mentor: §a" + info.get("minMentorRankName") + 
                           " §7(índice " + info.get("minMentorRankIndex") + ")");
        sender.sendMessage("  §7Rango máximo aprendiz: §b" + info.get("maxApprenticeRankName") + 
                           " §7(índice " + info.get("maxApprenticeRankIndex") + ")");
        
        sender.sendMessage("§6─────────────────────────────────────");
        
        // Estado actual
        sender.sendMessage("§7Estado actual:");
        sender.sendMessage("  §7Pares activos: §f" + info.get("activeBuddies"));
        sender.sendMessage("  §7Mentores con estadísticas: §f" + info.get("mentorStats"));
        sender.sendMessage("  §7Recompensas pendientes: §f" + info.get("pendingRewards"));
        
        sender.sendMessage("§6─────────────────────────────────────");
        
        // Jugadores online elegibles
        sender.sendMessage("§7Jugadores online elegibles:");
        sender.sendMessage("  §7Mentores disponibles: §a" + info.get("potentialMentorsOnline"));
        sender.sendMessage("  §7Aprendices potenciales: §b" + info.get("potentialApprenticesOnline"));
        
        sender.sendMessage("§6─────────────────────────────────────");
        
        // Validación y problemas
        if (issues.isEmpty()) {
            sender.sendMessage("§a✓ Configuración válida, no se detectaron problemas");
        } else {
            sender.sendMessage("§c⚠ Problemas detectados:");
            for (String issue : issues) {
                if (issue.startsWith("CRÍTICO")) {
                    sender.sendMessage("  §c" + issue);
                } else if (issue.startsWith("ADVERTENCIA")) {
                    sender.sendMessage("  §e" + issue);
                } else {
                    sender.sendMessage("  §7" + issue);
                }
            }
        }
        
        sender.sendMessage("§6═══════════════════════════════════════");
    }
    
    /**
     * Comando principal del Evento 5: Apertura del End
     * /avo evento5 <subcomando>
     */
    private void cmdEvento5(CommandSender sender, String[] args) {
        if (!sender.hasPermission("avo.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§5§l⚡ ═══ LA APERTURA DEL END ═══ ⚡");
            sender.sendMessage("§7Evento raid épico contra el Desolador del Vacío");
            sender.sendMessage("");
            sender.sendMessage("§e▸ Control Principal:");
            sender.sendMessage("  §f/avo evento5 start §7- Inicia el evento inmediatamente");
            sender.sendMessage("  §f/avo evento5 start <minutos> §7- Cuenta regresiva épica");
            sender.sendMessage("  §f/avo evento5 stop §7- Finaliza el evento");
            sender.sendMessage("  §f/avo evento5 info §7- Estado del evento");
            sender.sendMessage("  §f/avo evento5 stats §7- Tus estadísticas");
            sender.sendMessage("");
            sender.sendMessage("§e▸ Admin - Testing:");
            sender.sendMessage("  §f/avo evento5 skip §7- Saltar preparación");
            sender.sendMessage("  §f/avo evento5 modo §7- Ver modo de integración");
            sender.sendMessage("  §f/avo evento5 fase <1-4> §7- Forzar fase del dragón");
            sender.sendMessage("  §f/avo evento5 damage <jugador> <cantidad> §7- Simular daño");
            sender.sendMessage("  §f/avo evento5 kill §7- Matar dragón (test)");
            sender.sendMessage("  §f/avo evento5 recompensas §7- Obtener todas las recompensas");
            sender.sendMessage("");
            sender.sendMessage("§7Ejemplos:");
            sender.sendMessage("  §e/avo evento5 start 5 §7- Empieza en 5 minutos");
            sender.sendMessage("  §e/avo evento5 start 10 §7- Empieza en 10 minutos");
            sender.sendMessage("");
            sender.sendMessage("§7Alias: §faperturaend");
            return;
        }
        
        String subCmd = args[1].toLowerCase();
        
        // Obtener instancia del evento
        me.apocalipsis.events.AperturaEndEvent evento5 = null;
        if (eventController.hasActiveEvent() && 
            eventController.getActiveEvent() instanceof me.apocalipsis.events.AperturaEndEvent) {
            evento5 = (me.apocalipsis.events.AperturaEndEvent) eventController.getActiveEvent();
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
                    return;
                }
                
                // Verificar SAFE_MODE
                if (stateManager.isSafeModeActive()) {
                    sender.sendMessage("§cNo se puede iniciar en SAFE_MODE (TPS bajo).");
                    return;
                }
                
                // Verificar si hay parámetro de tiempo (cuenta regresiva)
                if (args.length >= 3) {
                    try {
                        int minutos = Integer.parseInt(args[2]);
                        
                        if (minutos < 1) {
                            sender.sendMessage("§cEl tiempo debe ser al menos 1 minuto.");
                            return;
                        }
                        
                        if (minutos > 60) {
                            sender.sendMessage("§cEl tiempo máximo es 60 minutos.");
                            return;
                        }
                        
                        // Iniciar cuenta regresiva épica
                        iniciarCuentaRegresivaEvento5(sender, minutos);
                        return;
                        
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cTiempo inválido. Usa: §e/avo evento5 start <minutos>");
                        return;
                    }
                }
                
                // Establecer ubicación del iniciador si es un jugador
                if (sender instanceof Player) {
                    Player iniciador = (Player) sender;
                    
                    // Obtener instancia del evento ANTES de iniciarlo
                    me.apocalipsis.events.EventBase eventoBase = eventController.getEvent("apertura_end");
                    
                    if (eventoBase instanceof me.apocalipsis.events.AperturaEndEvent) {
                        me.apocalipsis.events.AperturaEndEvent evento = 
                            (me.apocalipsis.events.AperturaEndEvent) eventoBase;
                        evento.setIniciador(iniciador);  // Ahora guarda ubicación Y UUID
                    }
                }
                
                // Iniciar evento inmediatamente (sin cuenta regresiva)
                if (eventController.startEvent("apertura_end")) {
                    sender.sendMessage("§a✓ Algo ha emergido... §c¡El Observador está aterrado!");
                    
                    // Título y sonido para todos
                    for (Player p : plugin.getServer().getOnlinePlayers()) {
                        p.sendTitle("§8§l⚠ §4EMERGENCIA§8 ⚠", "§7El Observador: 'Algo... terrible ha despertado'", 10, 80, 20);
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.6f);
                        p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.5f, 1.0f);
                    }
                    
                    plugin.getLogger().info(String.format("[Apertura End] Iniciado por %s", sender.getName()));
                } else {
                    sender.sendMessage("§cNo se pudo iniciar el evento. Verifica la consola.");
                }
                break;
                
            case "stop":
            case "detener":
                if (evento5 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                // Título y sonido para todos
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    p.sendTitle("§8§l⚡ EVENTO FINALIZADO ⚡", "§7El End vuelve al silencio...", 10, 50, 20);
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 0.5f);
                }
                
                eventController.stopActiveEvent();
                sender.sendMessage("§7✓ Evento §5La Apertura del End §7detenido");
                plugin.getLogger().info(String.format("[Apertura End] Detenido por %s", sender.getName()));
                break;
                
            case "info":
            case "status":
                if (evento5 == null) {
                    sender.sendMessage("§5§l⚡ LA APERTURA DEL END - INFO");
                    sender.sendMessage("§7Estado: §cInactivo");
                    sender.sendMessage("§7Usa §e/avo evento5 start §7para iniciarlo.");
                    return;
                }
                
                sender.sendMessage(evento5.getEstadoEvento());
                break;
                
            case "stats":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cSolo jugadores pueden ver sus estadísticas.");
                    return;
                }
                
                if (evento5 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                sender.sendMessage("§5§l⚡ TUS ESTADÍSTICAS ⚡");
                sender.sendMessage("§7(WIP - Proximamente)");
                break;
                
            case "skip":
                if (evento5 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                if (evento5.getFaseEvento() != me.apocalipsis.events.AperturaEndEvent.EventPhase.DESCUBRIMIENTO) {
                    sender.sendMessage("§cSolo se puede saltar la fase de descubrimiento.");
                    return;
                }
                
                evento5.saltarPreparacion();
                sender.sendMessage("§a✓ Fase de descubrimiento omitida - Portal activándose...");
                break;
                
            case "next":
                if (evento5 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                // Obtener fase actual
                me.apocalipsis.events.AperturaEndEvent.EventPhase faseActual = evento5.getFaseEvento();
                
                switch (faseActual) {
                    case DESCUBRIMIENTO:
                        // Saltar al siguiente diálogo
                        evento5.saltarAlSiguienteDialogo();
                        sender.sendMessage("§a✓ Saltando al siguiente diálogo...");
                        sender.sendMessage("§7Usa §e/avo evento5 next §7de nuevo para continuar");
                        break;
                        
                    case LLEGADA:
                        // Forzar apertura completa del portal
                        sender.sendMessage("§a✓ [LLEGADA → PORTAL_ABIERTO] Portal abierto instantáneamente");
                        sender.sendMessage("§7Tip: Entra al portal para continuar a COMBATE");
                        break;
                        
                    case PORTAL_ABIERTO:
                        sender.sendMessage("§e⚠ Fase PORTAL_ABIERTO activa - Entra al portal para continuar");
                        sender.sendMessage("§7Tip: Usa §e/tp @a <coords del End> §7para forzar");
                        break;
                        
                    case COMBATE:
                        // Matar al dragón instantáneamente
                        evento5.matarDragon();
                        sender.sendMessage("§a✓ [COMBATE → VICTORIA] Dragón eliminado");
                        break;
                        
                    case VICTORIA:
                        sender.sendMessage("§a✓ [VICTORIA → CLIFFHANGER] Saltando a mensaje final...");
                        sender.sendMessage("§7El evento terminará en breve...");
                        break;
                        
                    case CLIFFHANGER:
                        sender.sendMessage("§e⚠ Ya estás en la fase final (CLIFFHANGER)");
                        sender.sendMessage("§7El evento terminará automáticamente");
                        break;
                        
                    case INACTIVO:
                        sender.sendMessage("§cEl evento no está activo.");
                        break;
                        
                    default:
                        sender.sendMessage("§cFase desconocida: " + faseActual);
                        break;
                }
                break;
                
            case "tp":
            case "teleport":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cSolo jugadores pueden usar este comando.");
                    return;
                }
                
                if (evento5 == null) {
                    sender.sendMessage("§cEl evento 5 no está activo.");
                    return;
                }
                
                Player player = (Player) sender;
                Location portalLoc = evento5.getPortalLocation();
                
                if (portalLoc == null) {
                    sender.sendMessage("§cEl portal aún no está definido.");
                    return;
                }
                
                // Teletransportar a 10 bloques del portal
                Location tpLoc = portalLoc.clone().add(10, 0, 10);
                tpLoc.setY(portalLoc.getWorld().getHighestBlockYAt(tpLoc) + 1);
                
                player.teleport(tpLoc);
                
                // Mostrar info de distancia
                double distancia = player.getLocation().distance(portalLoc);
                sender.sendMessage("§a✓ Teletransportado cerca del portal.");
                sender.sendMessage("§7Distancia al portal: §b" + (int)distancia + " bloques");
                sender.sendMessage("§7Usa §e/avo evento5 forzarportal §7para generar el portal inmediatamente");
                break;
                
            case "forzarportal":
                if (evento5 == null) {
                    sender.sendMessage("§cEl evento 5 no está activo.");
                    return;
                }
                
                // Forzar la generación del portal sin esperar a que lleguen jugadores
                evento5.forzarGeneracionPortal();
                sender.sendMessage("§a✓ Generación de portal forzada.");
                break;
                
            case "modo":
                sender.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                sender.sendMessage("§a  MODO DE INTEGRACIÓN");
                sender.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                sender.sendMessage("");
                sender.sendMessage("§e► Modo: §7VANILLA+ (WIP)");
                sender.sendMessage("§e► Descripción: §7Sin plugins externos");
                sender.sendMessage("§e► Características:");
                sender.sendMessage("§7  ✓ Dragón vanilla del End");
                sender.sendMessage("§7  ✓ Mecánicas vanilla completas");
                sender.sendMessage("§7  ✓ Fases programadas en Java");
                sender.sendMessage("§7  ○ MythicMobs: No detectado");
                sender.sendMessage("§7  ○ ModelEngine: No detectado");
                sender.sendMessage("");
                sender.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                break;
                
            case "fase":
                if (evento5 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                if (args.length < 3) {
                    sender.sendMessage("§cUso: §e/avo evento5 fase <1-4>");
                    return;
                }
                
                try {
                    int fase = Integer.parseInt(args[2]);
                    
                    if (fase < 1 || fase > 4) {
                        sender.sendMessage("§cLa fase debe ser entre 1 y 4.");
                        return;
                    }
                    
                    evento5.forzarFase(fase);
                    sender.sendMessage("§a✓ Dragón forzado a fase " + fase);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cNúmero de fase inválido.");
                }
                break;
                
            case "damage":
                if (evento5 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                if (args.length < 4) {
                    sender.sendMessage("§cUso: §e/avo evento5 damage <jugador> <cantidad>");
                    return;
                }
                
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage("§cJugador no encontrado: " + args[2]);
                    return;
                }
                
                try {
                    double cantidad = Double.parseDouble(args[3]);
                    evento5.añadirDaño(target, cantidad);
                    sender.sendMessage("§a✓ Añadidos " + cantidad + " HP de daño a " + target.getName());
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cCantidad inválida.");
                }
                break;
                
            case "kill":
                if (evento5 == null) {
                    sender.sendMessage("§cEl evento no está activo.");
                    return;
                }
                
                evento5.matarDragon();
                sender.sendMessage("§a✓ Dragón eliminado");
                break;
                
            case "recompensas":
            case "rewards":
                // Subcomando: /avo recompensas mundo reset
                if (args.length >= 3 && args[1].equalsIgnoreCase("mundo") && args[2].equalsIgnoreCase("reset")) {
                    if (!sender.hasPermission("avo.admin")) {
                        sender.sendMessage("§cNo tienes permisos.");
                        return;
                    }
                    
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("§cEste comando solo puede ejecutarlo un jugador.");
                        return;
                    }
                    
                    Player playerRewards = (Player) sender;
                    String worldName = playerRewards.getWorld().getName();
                    
                    // Resetear recompensas entregadas del mundo actual
                    if (plugin.getRewardService() != null) {
                        java.util.UUID uuid = playerRewards.getUniqueId();
                        plugin.getRewardService().resetPlayerRewards(uuid);
                        
                        sender.sendMessage("");
                        sender.sendMessage("§6§l⚠ RESET DE RECOMPENSAS ⚠");
                        sender.sendMessage("§7Mundo: §e" + worldName);
                        sender.sendMessage("§a✓ §7Se han reseteado todas las recompensas entregadas");
                        sender.sendMessage("§7Ahora puedes volver a reclamar recompensas de rangos");
                        sender.sendMessage("");
                        
                        plugin.getLogger().info("[RecompensasReset] " + playerRewards.getName() + " reseteó sus recompensas en " + worldName);
                    } else {
                        sender.sendMessage("§cError: RewardService no está disponible");
                    }
                    return;
                }
                
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cEste comando solo puede ejecutarlo un jugador.");
                    return;
                }
                
                Player admin = (Player) sender;
                
                // Dar todas las recompensas PLATINUM (las mejores - 23 items)
                me.apocalipsis.events.AperturaEndEvent eventoRecompensas = new me.apocalipsis.events.AperturaEndEvent(
                    plugin, messageBus, plugin.getSoundUtil()
                );
                
                List<org.bukkit.inventory.ItemStack> recompensas = new java.util.ArrayList<>();
                
                // Items base (8 items)
                recompensas.add(eventoRecompensas.getItems().crearFragmentoDelVacio(8));
                recompensas.add(new org.bukkit.inventory.ItemStack(org.bukkit.Material.ENDER_PEARL, 12));
                recompensas.add(new org.bukkit.inventory.ItemStack(org.bukkit.Material.ENDER_EYE, 12));
                
                // Items épicos Top 1 (10 items adicionales)
                recompensas.add(eventoRecompensas.getItems().crearEscamaPerfecta(5));
                recompensas.add(eventoRecompensas.getItems().crearCorazonDesolador());
                recompensas.add(new org.bukkit.inventory.ItemStack(org.bukkit.Material.END_STONE, 32));
                recompensas.add(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND, 24));
                
                // Armadura completa Desoladora (4 piezas)
                recompensas.add(eventoRecompensas.crearArmaduraDesoladora("helmet"));
                recompensas.add(eventoRecompensas.crearArmaduraDesoladora("chestplate"));
                recompensas.add(eventoRecompensas.crearArmaduraDesoladora("leggings"));
                recompensas.add(eventoRecompensas.crearArmaduraDesoladora("boots"));
                
                // Armas y herramientas (3 items)
                recompensas.add(eventoRecompensas.crearEspadaDesoladora());
                recompensas.add(eventoRecompensas.crearArcoDesolador());
                recompensas.add(eventoRecompensas.crearPicoDesolador());
                
                // Añadir al inventario
                int itemsRecibidos = 0;
                for (org.bukkit.inventory.ItemStack item : recompensas) {
                    if (admin.getInventory().firstEmpty() != -1) {
                        admin.getInventory().addItem(item);
                        itemsRecibidos++;
                    } else {
                        admin.getWorld().dropItemNaturally(admin.getLocation(), item);
                    }
                }
                
                // XP del Top 1
                plugin.getExperienceService().addXP(admin, 11000, "Admin: Evento 5 Rewards", false);
                
                // Mensaje de confirmación
                sender.sendMessage("");
                sender.sendMessage("§5§l⚡ ═══ RECOMPENSAS EVENTO 5 ═══ ⚡");
                sender.sendMessage("§7Recompensas del §ePuesto #1 §7(PLATINUM)");
                sender.sendMessage("");
                sender.sendMessage("§a✓ Items recibidos: §f" + itemsRecibidos + " §7/ §f" + recompensas.size());
                sender.sendMessage("§a✓ XP recibido: §f+11,000");
                sender.sendMessage("");
                sender.sendMessage("§7Incluye:");
                sender.sendMessage("  §8▪ §5Armadura Desoladora §7completa (4 piezas)");
                sender.sendMessage("  §8▪ §5Espada§7, §5Arco §7y §5Pico Desolador");
                sender.sendMessage("  §8▪ §5x1 Corazón Desolador §7(LEGENDARIO)");
                sender.sendMessage("  §8▪ §5x5 Escama Perfecta §7(ÉPICO)");
                sender.sendMessage("  §8▪ §7End Stone, Diamantes y más");
                sender.sendMessage("");
                break;
                
            default:
                sender.sendMessage("§cSubcomando desconocido: §f" + subCmd);
                sender.sendMessage("§7Usa §e/avo evento5 §7para ver comandos disponibles.");
                break;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // CUENTA REGRESIVA ÉPICA PARA EVENTO 5
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Inicia una cuenta regresiva épica antes de comenzar el Evento 5
     * Con mensajes, efectos visuales y sonidos dramáticos
     */
    private void iniciarCuentaRegresivaEvento5(CommandSender sender, int minutos) {
        int segundos = minutos * 60;
        
        // Guardar el jugador iniciador si es un jugador
        final Player jugadorIniciador;
        if (sender instanceof Player) {
            jugadorIniciador = (Player) sender;
        } else {
            jugadorIniciador = null;
        }
        
        // Crear BossBar misteriosa
        final org.bukkit.boss.BossBar bossBarCountdown = Bukkit.createBossBar(
            "§8⚠ El Observador detecta algo extraño...",
            org.bukkit.boss.BarColor.PURPLE,
            org.bukkit.boss.BarStyle.SEGMENTED_20
        );
        
        // Añadir todos los jugadores a la bossbar
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            bossBarCountdown.addPlayer(p);
        }
        bossBarCountdown.setVisible(true);
        
        // Anuncio inicial misterioso
        String tiempoTexto = minutos == 1 ? "1 minuto" : minutos + " minutos";
        Bukkit.getServer().broadcast(
            net.kyori.adventure.text.Component.text("§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬")
                .color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY)
        );
        Bukkit.getServer().broadcast(
            net.kyori.adventure.text.Component.text("§7§l⚠ ⚠ ⚠ ALERTA DEL OBSERVADOR ⚠ ⚠ ⚠")
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
        );
        Bukkit.getServer().broadcast(
            net.kyori.adventure.text.Component.text("§5Algo está siendo construido en las sombras...")
                .color(net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE)
        );
        Bukkit.getServer().broadcast(
            net.kyori.adventure.text.Component.text("§7Una presencia oscura se acerca en: §f§l" + tiempoTexto)
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
        );
        Bukkit.getServer().broadcast(
            net.kyori.adventure.text.Component.text("§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬")
                .color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY)
        );
        
        // Efectos iniciales
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.sendTitle("§8⚠ §5ALERTA§8 ⚠", "§7El Observador está... preocupado", 10, 60, 20);
            p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.5f, 0.8f);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.5f);
        }
        
        sender.sendMessage("§a✓ Observador preocupado - algo se está gestando en §e" + tiempoTexto);
        plugin.getLogger().info(String.format("[Apertura End] Cuenta regresiva iniciada: %d minutos por %s", minutos, sender.getName()));
        
        // TASK DE CUENTA REGRESIVA
        new org.bukkit.scheduler.BukkitRunnable() {
            int restante = segundos;
            
            @Override
            public void run() {
                restante--;
                
                // Actualizar BossBar con mensajes misteriosos
                int minRestantes = restante / 60;
                int segRestantes = restante % 60;
                double progreso = (double) restante / segundos;
                bossBarCountdown.setProgress(Math.max(0.0, Math.min(1.0, progreso)));
                
                // Cambiar color según tiempo
                if (restante <= 30) {
                    bossBarCountdown.setColor(org.bukkit.boss.BarColor.RED);
                } else if (restante <= 60) {
                    bossBarCountdown.setColor(org.bukkit.boss.BarColor.PINK);
                } else if (restante <= 120) {
                    bossBarCountdown.setColor(org.bukkit.boss.BarColor.PURPLE);
                }
                
                // Mensajes en momentos clave
                if (restante == 300) { // 5 minutos
                    bossBarCountdown.setTitle("§8⚠ Algo se está generando en la oscuridad... §75:00");
                    anunciarTiempoRestante("§e5 minutos", "§7El Observador: 'Siento una energía... desconocida'");
                    efectoIntermedio();
                } else if (restante == 180) { // 3 minutos
                    bossBarCountdown.setTitle("§5⚡ La construcción se acelera... §73:00");
                    anunciarTiempoRestante("§e3 minutos", "§7El Observador: 'Esto es... inquietante'");
                    efectoIntermedio();
                } else if (restante == 120) { // 2 minutos
                    bossBarCountdown.setTitle("§d⚡ Algo emerge de las sombras... §c2:00");
                    anunciarTiempoRestante("§c2 minutos", "§7El Observador: 'Jamás había sentido tal poder...'");
                    efectoIntermedio();
                } else if (restante == 60) { // 1 minuto
                    bossBarCountdown.setTitle("§c§l⚠ LA CONSTRUCCIÓN CASI TERMINA §c1:00");
                    anunciarTiempoRestante("§c§l1 MINUTO", "§c§lEl Observador: '¡PREPARENSE PARA LO PEOR!'");
                    efectoIntenso();
                } else if (restante == 30) { // 30 segundos
                    bossBarCountdown.setTitle("§4§l⚠⚠⚠ ALGO DESPIERTA ⚠⚠⚠ §40:30");
                    anunciarTiempoRestante("§4§l30 SEGUNDOS", "§4§lEl Observador: '¡NO... ESO ES IMPOSIBLE!'");
                    efectoIntenso();
                } else if (restante <= 10 && restante > 0) { // 10, 9, 8...
                    bossBarCountdown.setTitle("§4§l⚠⚠⚠ " + restante + " ⚠⚠⚠");
                    anunciarConteoFinal(restante);
                } else if (restante == 0) {
                    // ¡INICIAR EVENTO!
                    bossBarCountdown.removeAll();
                    iniciarEventoDespuesDeCuentaRegresiva();
                    cancel();
                    return;
                } else {
                    // Actualizar texto normal del bossbar
                    String tiempo = String.format("%d:%02d", minRestantes, segRestantes);
                    if (restante > 300) {
                        bossBarCountdown.setTitle("§8⚠ El Observador percibe algo... §7" + tiempo);
                    } else if (restante > 180) {
                        bossBarCountdown.setTitle("§8⚡ Algo se está construyendo... §7" + tiempo);
                    } else if (restante > 120) {
                        bossBarCountdown.setTitle("§5⚡ La energía aumenta... §7" + tiempo);
                    } else if (restante > 60) {
                        bossBarCountdown.setTitle("§d⚡ Una presencia se acerca... §c" + tiempo);
                    } else if (restante > 30) {
                        bossBarCountdown.setTitle("§c§l⚠ Algo está a punto de emerger... §c" + tiempo);
                    }
                }
            }
            
            private void anunciarTiempoRestante(String tiempo, String mensaje) {
                Bukkit.getServer().broadcast(
                    net.kyori.adventure.text.Component.text("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━")
                        .color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY)
                );
                Bukkit.getServer().broadcast(
                    net.kyori.adventure.text.Component.text(mensaje)
                        .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                );
                Bukkit.getServer().broadcast(
                    net.kyori.adventure.text.Component.text("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━")
                        .color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY)
                );
            }
            
            private void anunciarConteoFinal(int segundos) {
                String color = segundos <= 3 ? "§4§l" : "§c§l";
                
                Bukkit.getServer().broadcast(
                    net.kyori.adventure.text.Component.text(color + "⚠ " + segundos + " ⚠")
                        .color(net.kyori.adventure.text.format.NamedTextColor.DARK_RED)
                );
                
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    p.sendTitle(color + "⚠ " + segundos + " ⚠", "§8Algo emerge...", 0, 25, 5);
                    
                    if (segundos <= 5) {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 2.0f, 2.0f);
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 2.0f);
                    } else {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.5f, 1.5f);
                    }
                    
                    // Partículas épicas
                    org.bukkit.Location loc = p.getLocation();
                    p.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, loc, 20 + (10 - segundos) * 10, 1, 1, 1, 0.5);
                    p.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, loc.clone().add(0, 2, 0), 5, 0.5, 0.5, 0.5);
                }
            }
            
            private void efectoIntermedio() {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.6f);
                    p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 0.5f, 0.5f);
                    
                    org.bukkit.Location loc = p.getLocation();
                    p.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, loc, 30, 2, 1, 2, 0.3);
                    p.getWorld().spawnParticle(org.bukkit.Particle.ENCHANT, loc, 20, 1.5, 1, 1.5);
                }
            }
            
            private void efectoIntenso() {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.5f);
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.7f);
                    
                    org.bukkit.Location loc = p.getLocation();
                    p.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, loc, 60, 3, 2, 3, 0.5);
                    p.getWorld().spawnParticle(org.bukkit.Particle.DRAGON_BREATH, loc, 30, 2, 1, 2, 0.1);
                    p.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, loc.clone().add(0, 2, 0), 15, 1, 1, 1);
                }
            }
            
            private void iniciarEventoDespuesDeCuentaRegresiva() {
                // ANUNCIO ÉPICO FINAL - OBSERVADOR PREOCUPADO
                Bukkit.getServer().broadcast(
                    net.kyori.adventure.text.Component.text("§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬")
                        .color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY)
                );
                Bukkit.getServer().broadcast(
                    net.kyori.adventure.text.Component.text("§7§l⚠ ⚠ ⚠ EL OBSERVADOR GRITA ⚠ ⚠ ⚠")
                        .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                );
                Bukkit.getServer().broadcast(
                    net.kyori.adventure.text.Component.text("§4§l'¡ALGO HA EMERGIDO DE LAS SOMBRAS!'")
                        .color(net.kyori.adventure.text.format.NamedTextColor.DARK_RED)
                );
                Bukkit.getServer().broadcast(
                    net.kyori.adventure.text.Component.text("§5Una presencia antigua y terrible se materializa...")
                        .color(net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE)
                );
                Bukkit.getServer().broadcast(
                    net.kyori.adventure.text.Component.text("§8§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬")
                        .color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY)
                );
                
                // Efectos finales masivos
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    p.sendTitle("§8§l⚠ §4EMERGENCIA§8 ⚠", "§5§lEl Observador: '¡ESCAPEN!'", 10, 80, 20);
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.5f);
                    p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 2.0f, 1.0f);
                    p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.6f);
                    
                    org.bukkit.Location loc = p.getLocation();
                    p.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_EMITTER, loc, 3);
                    p.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, loc, 200, 5, 3, 5, 1.0);
                    p.getWorld().spawnParticle(org.bukkit.Particle.DRAGON_BREATH, loc, 100, 4, 2, 4, 0.5);
                    p.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, loc.clone().add(0, 3, 0), 50, 2, 2, 2, 0.2);
                }
                
                // Establecer el iniciador ANTES de iniciar (con UUID)
                if (jugadorIniciador != null) {
                    me.apocalipsis.events.EventBase eventoBase = eventController.getEvent("apertura_end");
                    if (eventoBase instanceof me.apocalipsis.events.AperturaEndEvent) {
                        me.apocalipsis.events.AperturaEndEvent evento = 
                            (me.apocalipsis.events.AperturaEndEvent) eventoBase;
                        evento.setIniciador(jugadorIniciador);  // Guarda ubicación Y UUID
                    }
                }
                
                // INICIAR EVENTO
                if (eventController.startEvent("apertura_end")) {
                    plugin.getLogger().info("[Apertura End] ✓ Evento iniciado automáticamente tras cuenta regresiva");
                } else {
                    plugin.getLogger().severe("[Apertura End] ✖ Error al iniciar evento tras cuenta regresiva");
                    Bukkit.getServer().broadcast(
                        net.kyori.adventure.text.Component.text("§c§l✖ ERROR: No se pudo iniciar el evento")
                            .color(net.kyori.adventure.text.format.NamedTextColor.RED)
                    );
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Cada segundo
    }
    
    /**
     * Comando principal del Evento 6: Cuando el Mundo Decide Olvidar
     * /avo evento6 <subcomando>
     */
    private void cmdEvento6(CommandSender sender, String[] args) {
        if (!sender.hasPermission("apocalipsis.evento6.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando solo puede ejecutarlo un jugador.");
            return;
        }
        
        Player player = (Player) sender;
        me.apocalipsis.events.Evento6MundoOlvidado evento6 = plugin.getEvento6();
        
        if (args.length < 2) {
            // Mostrar ayuda
            sender.sendMessage("§8§l⚠ ═══ CUANDO EL MUNDO DECIDE OLVIDAR ═══ ⚠");
            sender.sendMessage("§7El mundo se cansa. Reiniciar es más fácil que cambiar.");
            sender.sendMessage("");
            sender.sendMessage("§e▸ Control Principal:");
            sender.sendMessage("  §f/avo evento6 start §7- Inicia el evento");
            sender.sendMessage("  §f/avo evento6 stop §7- Detiene el evento");
            sender.sendMessage("  §f/avo evento6 status §7- Estado del evento");
            sender.sendMessage("  §f/avo evento6 info §7- Información detallada");
            sender.sendMessage("");
            sender.sendMessage("§e▸ Control de Fases:");
            sender.sendMessage("  §f/avo evento6 next §7- Avanza al siguiente acto (modo manual)");
            sender.sendMessage("  §f/avo evento6 skip §7- Salta al siguiente acto (modo manual)");
            sender.sendMessage("  §f/avo evento6 auto §7- Reactiva progresión automática");
            sender.sendMessage("  §f/avo evento6 participantes §7- Lista participantes");
            sender.sendMessage("");
            sender.sendMessage("§7Alias: §fmundoolvidado, reinicio");
            sender.sendMessage("");
            
            if (evento6.isEventoActivo()) {
                sender.sendMessage("§7Estado: §a✓ Activo");
                sender.sendMessage("§7Fase: §e" + evento6.getFaseActual().getNombreDisplay());
                sender.sendMessage("§7Modo: " + (evento6.isModoSkipActivo() ? "§e⚡ Manual" : "§a⚙ Automático"));
                sender.sendMessage("§7Participantes: §e" + evento6.getParticipantes().size());
                long tiempo = evento6.getTiempoTranscurridoSegundos();
                sender.sendMessage("§7Tiempo: §e" + (tiempo / 60) + "m " + (tiempo % 60) + "s");
            } else {
                sender.sendMessage("§7Estado: §c✗ Inactivo");
            }
            
            return;
        }
        
        String subCmd = args[1].toLowerCase();
        
        switch (subCmd) {
            case "start":
            case "iniciar":
                if (evento6.isEventoActivo()) {
                    sender.sendMessage("§c✗ El Evento 6 ya está activo.");
                    return;
                }
                
                // Iniciar evento
                if (evento6.iniciarEvento(player)) {
                    sender.sendMessage("§a✓ Evento 6 iniciado correctamente.");
                    sender.sendMessage("§7Duración estimada: §e~2 horas");
                    sender.sendMessage("§7El mundo comenzará a olvidar en §e50 minutos§7...");
                } else {
                    sender.sendMessage("§c✗ No se pudo iniciar el evento.");
                    sender.sendMessage("§7Verifica que el sistema de ciclos esté activo.");
                }
                break;
                
            case "stop":
            case "detener":
                if (!evento6.isEventoActivo()) {
                    sender.sendMessage("§c✗ El Evento 6 no está activo.");
                    return;
                }
                
                evento6.detenerEvento();
                sender.sendMessage("§a✓ Evento 6 detenido.");
                break;
                
            case "status":
            case "estado":
                if (evento6.isEventoActivo()) {
                    sender.sendMessage("§a✓ Evento 6 activo");
                    sender.sendMessage("§7Fase: §e" + evento6.getFaseActual().getNombreDisplay());
                    sender.sendMessage("§7Participantes: §e" + evento6.getParticipantes().size());
                    long t = evento6.getTiempoTranscurridoSegundos();
                    sender.sendMessage("§7Tiempo: §e" + (t / 60) + "m " + (t % 60) + "s");
                } else {
                    sender.sendMessage("§c✗ Evento 6 inactivo");
                }
                break;
                
            case "info":
                sender.sendMessage("§8§l━━━━━━ §7EVENTO 6 §8§l━━━━━━");
                sender.sendMessage("§7Nombre: §fCuando el Mundo Decide Olvidar");
                sender.sendMessage("");
                
                if (evento6.isEventoActivo()) {
                    sender.sendMessage("§7Estado: §a✓ Activo");
                    sender.sendMessage("§7Fase actual: §e" + evento6.getFaseActual().getNombreDisplay());
                    sender.sendMessage("§7Participantes: §e" + evento6.getParticipantes().size());
                    
                    long tiempo = evento6.getTiempoTranscurridoSegundos();
                    int minutos = (int) (tiempo / 60);
                    int segundos = (int) (tiempo % 60);
                    sender.sendMessage("§7Tiempo transcurrido: §e" + minutos + "m " + segundos + "s");
                    
                    // Mostrar siguiente acto
                    me.apocalipsis.events.MundoOlvidadoFase siguiente = obtenerSiguienteActo(evento6.getFaseActual());
                    if (siguiente != null) {
                        sender.sendMessage("§7Siguiente acto: §e" + siguiente.getNombreDisplay());
                    }
                } else {
                    sender.sendMessage("§7Estado: §c✗ Inactivo");
                }
                
                sender.sendMessage("§8§l━━━━━━━━━━━━━━━━━━");
                break;
                
            case "next":
            case "skip":
            case "siguiente":
                if (!evento6.isEventoActivo()) {
                    sender.sendMessage("§c✗ El Evento 6 no está activo.");
                    return;
                }
                
                me.apocalipsis.events.MundoOlvidadoFase actual = evento6.getFaseActual();
                me.apocalipsis.events.MundoOlvidadoFase siguiente = obtenerSiguienteActo(actual);
                
                if (siguiente == null || siguiente == me.apocalipsis.events.MundoOlvidadoFase.COMPLETADO) {
                    sender.sendMessage("§c✗ Ya estás en el último acto.");
                    return;
                }
                
                // Activar modo skip para evitar que la progresión automática lo devuelva
                evento6.activarModoSkip();
                
                // Forzar cambio de acto
                try {
                    java.lang.reflect.Method cambiarActo = evento6.getClass().getDeclaredMethod("cambiarAActo", me.apocalipsis.events.MundoOlvidadoFase.class);
                    cambiarActo.setAccessible(true);
                    cambiarActo.invoke(evento6, siguiente);
                    
                    sender.sendMessage("§a✓ Avanzado a: §e" + siguiente.getNombreDisplay());
                    sender.sendMessage("§7§o(Modo manual activo - usa §f/avo evento6 auto§7§o para volver a modo automático)");
                    Bukkit.broadcastMessage("§8[§7EVENTO 6§8] §e⚡ Avance forzado: §f" + siguiente.getNombreDisplay());
                } catch (Exception e) {
                    sender.sendMessage("§c✗ Error al avanzar al siguiente acto.");
                    plugin.getLogger().warning("[Evento 6] Error en skip: " + e.getMessage());
                }
                break;
                
            case "participantes":
            case "players":
                if (!evento6.isEventoActivo()) {
                    sender.sendMessage("§c✗ El Evento 6 no está activo.");
                    return;
                }
                
                Set<UUID> participantesEvento = evento6.getParticipantes();
                sender.sendMessage("§8§l━━ §7PARTICIPANTES EVENTO 6 §8§l━━");
                sender.sendMessage("§7Total: §e" + participantesEvento.size());
                sender.sendMessage("");
                
                int online = 0;
                for (UUID uuid : participantesEvento) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        sender.sendMessage("  §a✓ §f" + p.getName());
                        online++;
                    } else {
                        sender.sendMessage("  §c✗ §7" + Bukkit.getOfflinePlayer(uuid).getName());
                    }
                }
                
                sender.sendMessage("");
                sender.sendMessage("§7Online: §e" + online + "§7/§e" + participantesEvento.size());
                sender.sendMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━");
                break;
                
            case "auto":
            case "automatico":
                if (!evento6.isEventoActivo()) {
                    sender.sendMessage("§c✗ El Evento 6 no está activo.");
                    return;
                }
                
                if (!evento6.isModoSkipActivo()) {
                    sender.sendMessage("§e⚠ El modo automático ya está activo.");
                    return;
                }
                
                evento6.desactivarModoSkip();
                sender.sendMessage("§a✓ Modo automático reactivado.");
                sender.sendMessage("§7Los actos avanzarán según el tiempo transcurrido.");
                Bukkit.broadcastMessage("§8[§7EVENTO 6§8] §a⚙ Progresión automática reactivada");
                break;
                
            default:
                sender.sendMessage("§cSubcomando desconocido: §f" + subCmd);
                sender.sendMessage("§7Usa §e/avo evento6 §7para ver la ayuda.");
                break;
        }
    }
    
    /**
     * Obtiene el siguiente acto en la secuencia
     */
    private me.apocalipsis.events.MundoOlvidadoFase obtenerSiguienteActo(me.apocalipsis.events.MundoOlvidadoFase actual) {
        switch (actual) {
            case ACTO_1_NORMALIDAD: return me.apocalipsis.events.MundoOlvidadoFase.ACTO_2_RAREZAS;
            case ACTO_2_RAREZAS: return me.apocalipsis.events.MundoOlvidadoFase.ACTO_3_INESTABILIDAD;
            case ACTO_3_INESTABILIDAD: return me.apocalipsis.events.MundoOlvidadoFase.ACTO_4_QUIEBRE;
            case ACTO_4_QUIEBRE: return me.apocalipsis.events.MundoOlvidadoFase.ACTO_5_REINICIO;
            case ACTO_5_REINICIO: return me.apocalipsis.events.MundoOlvidadoFase.ACTO_6_NUEVO_MUNDO;
            case ACTO_6_NUEVO_MUNDO: return me.apocalipsis.events.MundoOlvidadoFase.ACTO_7_COMPRENSION;
            case ACTO_7_COMPRENSION: return me.apocalipsis.events.MundoOlvidadoFase.ACTO_8_FRACTURA;
            case ACTO_8_FRACTURA: return me.apocalipsis.events.MundoOlvidadoFase.ACTO_9_END_PERMANECE;
            case ACTO_9_END_PERMANECE: return me.apocalipsis.events.MundoOlvidadoFase.ACTO_10_CIERRE;
            case ACTO_10_CIERRE: return me.apocalipsis.events.MundoOlvidadoFase.COMPLETADO;
            default: return null;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SISTEMA DE CICLOS MULTI-MUNDO
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * /avo ciclo <subcomando>
     * Gestiona el sistema de ciclos (múltiples mundos independientes)
     * 
     * Subcomandos:
     * - nuevo <mundo> [teleport] - Activa un nuevo ciclo en un mundo
     * - desactivar <mundo> - Desactiva un ciclo
     * - listar - Lista todos los ciclos activos
     * - info <mundo> - Muestra información de un mundo/ciclo
     * - teleport <mundo> - Teleporta al mundo especificado
     */
    private void cmdCiclo(CommandSender sender, String[] args) {
        // Verificar permisos de admin
        if (!sender.hasPermission("apocalipsis.admin")) {
            sender.sendMessage("§c✖ No tienes permisos para usar este comando.");
            return;
        }
        
        me.apocalipsis.ciclos.CicloManager cicloManager = plugin.getCicloManager();
        
        if (args.length == 0) {
            // Mostrar ayuda
            sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            sender.sendMessage("§b§lSISTEMA DE CICLOS §8- §7Ayuda");
            sender.sendMessage("");
            sender.sendMessage("§a§lCREACIÓN Y GESTIÓN:");
            sender.sendMessage("§e/avo ciclo crear <mundo> [tipo] [dificultad]");
            sender.sendMessage("  §7→ Crea un nuevo mundo automáticamente");
            sender.sendMessage("  §7   Tipos: NORMAL, NETHER, THE_END");
            sender.sendMessage("  §7   Ejemplo: §f/avo ciclo crear ciclo_1 NORMAL HARD");
            sender.sendMessage("");
            sender.sendMessage("§e/avo ciclo nuevo <mundo> [teleport]");
            sender.sendMessage("  §7→ Activa un ciclo (crea mundo si no existe)");
            sender.sendMessage("  §7   Ejemplo: §f/avo ciclo nuevo world_ciclo_1 true");
            sender.sendMessage("");
            sender.sendMessage("§b§lNAVEGACIÓN:");
            sender.sendMessage("§e/avo ciclo menu");
            sender.sendMessage("  §7→ Abrir menú gráfico de ciclos");
            sender.sendMessage("§e/avo ciclo cambiar <mundo>");
            sender.sendMessage("  §7→ Cambiar a otro ciclo (con countdown)");
            sender.sendMessage("§e/avo ciclo teleport <mundo>");
            sender.sendMessage("  §7→ Teleporte directo §c[Solo Admins]");
            sender.sendMessage("");
            sender.sendMessage("§c§lGESTIÓN AVANZADA:");
            sender.sendMessage("§e/avo ciclo eliminar <mundo>");
            sender.sendMessage("  §7→ Eliminar un ciclo §7(requiere confirmación)");
            sender.sendMessage("§e/avo ciclo renombrar <mundo> <nuevo>");
            sender.sendMessage("  §7→ Renombrar un ciclo §7(requiere confirmación)");
            sender.sendMessage("§e/avo ciclo desactivar <mundo>");
            sender.sendMessage("  §7→ Desactiva un ciclo");
            sender.sendMessage("§e/avo ciclo setspawn [mundo]");
            sender.sendMessage("  §7→ Establece el spawn en tu ubicación actual");
            sender.sendMessage("");
            sender.sendMessage("§6§lCONSULTAS:");
            sender.sendMessage("§e/avo ciclo listar");
            sender.sendMessage("  §7→ Lista todos los ciclos registrados");
            sender.sendMessage("§e/avo ciclo info <mundo>");
            sender.sendMessage("  §7→ Muestra información detallada de un ciclo");
            sender.sendMessage("§e/avo ciclo stats <mundo>");
            sender.sendMessage("  §7→ Estadísticas detalladas del ciclo");
            sender.sendMessage("§e/avo ciclo reporte");
            sender.sendMessage("  §7→ Genera reporte completo de todos los ciclos");
            sender.sendMessage("§e/avo ciclo validar");
            sender.sendMessage("  §7→ Valida y repara integridad de datos");
            sender.sendMessage("");
            sender.sendMessage("§e/avo ciclo confirmar §8- §7Confirmar acción pendiente");
            sender.sendMessage("§e/avo ciclo cancelar §8- §7Cancelar confirmación");
            sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return;
        }
        
        String subCmd = args[0].toLowerCase();
        
        switch (subCmd) {
            case "crear":
            case "create":
                if (args.length < 2) {
                    sender.sendMessage("§c✖ Uso: §e/avo ciclo crear <nombre> [NORMAL|NETHER|END] [EASY|NORMAL|HARD]");
                    return;
                }
                
                String newWorldName = args[1];
                org.bukkit.World.Environment env = org.bukkit.World.Environment.NORMAL;
                org.bukkit.Difficulty diff = org.bukkit.Difficulty.HARD;
                
                // Parsear ambiente
                if (args.length >= 3) {
                    try {
                        env = org.bukkit.World.Environment.valueOf(args[2].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage("§c✖ Ambiente inválido. Usa: NORMAL, NETHER o THE_END");
                        return;
                    }
                }
                
                // Parsear dificultad
                if (args.length >= 4) {
                    try {
                        diff = org.bukkit.Difficulty.valueOf(args[3].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage("§c✖ Dificultad inválida. Usa: PEACEFUL, EASY, NORMAL o HARD");
                        return;
                    }
                }
                
                sender.sendMessage("§e⚙ Creando mundo: §b" + newWorldName);
                sender.sendMessage("§7Ambiente: §e" + env.name() + "§7, Dificultad: §e" + diff.name());
                
                // Por defecto SIEMPRE teletransporta (es un reinicio completo)
                // Solo permite NO teleportar si explícitamente se pone 'false'
                boolean teleportAfterCreate = args.length < 5 || !args[4].equalsIgnoreCase("false");
                
                if (cicloManager.createAndActivateCycle(newWorldName, env, diff, teleportAfterCreate)) {
                    sender.sendMessage("§a✓ Mundo creado y ciclo activado exitosamente!");
                    sender.sendMessage("§a✓ Todos los jugadores han sido teleportados al nuevo ciclo.");
                } else {
                    sender.sendMessage("§c✖ Error al crear el mundo. Revisa la consola.");
                }
                break;
                
            case "nuevo":
            case "new":
            case "activar":
            case "activate":
                if (args.length < 2) {
                    sender.sendMessage("§c✖ Uso: §e/avo ciclo nuevo <mundo>");
                    sender.sendMessage("§7Esto reinicia el servidor - todos serán teleportados.");
                    return;
                }
                
                String worldName = args[1];
                // Por defecto SIEMPRE teletransporta (es un reinicio completo)
                // Solo permite NO teleportar si explícitamente se pone 'false' como segundo argumento
                boolean teleportAll = args.length < 3 || !args[2].equalsIgnoreCase("false");
                
                // Verificar si el mundo existe
                org.bukkit.World existingWorld = org.bukkit.Bukkit.getWorld(worldName);
                
                if (existingWorld == null) {
                    // Mundo no existe - ofrecer crearlo automáticamente
                    sender.sendMessage("§e⚠ El mundo '§b" + worldName + "§e' no existe.");
                    sender.sendMessage("§e⚙ Creando mundo automáticamente con Multiverse...");
                    sender.sendMessage("§7→ Todos los jugadores serán teleportados al nuevo mundo.");
                    
                    if (!cicloManager.isMultiverseAvailable()) {
                        sender.sendMessage("§c✖ Multiverse-Core no está instalado!");
                        sender.sendMessage("§7Instala Multiverse-Core o crea el mundo manualmente con:");
                        sender.sendMessage("§e/mv create " + worldName + " NORMAL");
                        return;
                    }
                    
                    // Crear mundo con configuración por defecto (siempre teletransporta)
                    if (!cicloManager.createAndActivateCycle(worldName, 
                            org.bukkit.World.Environment.NORMAL, 
                            org.bukkit.Difficulty.HARD, 
                            teleportAll)) {
                        sender.sendMessage("§c✖ Error al crear el mundo. Revisa la consola.");
                        return;
                    }
                    
                    sender.sendMessage("§a✓ Mundo creado y ciclo activado exitosamente!");
                    sender.sendMessage("§a✓ Todos los jugadores han sido teleportados.");
                    return;
                }
                
                // Mundo existe - solo activar ciclo
                sender.sendMessage("§e⚙ Activando nuevo ciclo en mundo: §b" + worldName + "§e...");
                sender.sendMessage("§7→ Reinicio completo - todos los jugadores serán teleportados.");
                
                if (cicloManager.activateCycle(worldName, teleportAll)) {
                    sender.sendMessage("§a✓ Ciclo activado exitosamente!");
                    sender.sendMessage("§a✓ Todos los jugadores han sido teleportados al nuevo ciclo.");
                } else {
                    sender.sendMessage("§c✖ Error al activar el ciclo. Revisa la consola.");
                }
                break;
                
            case "desactivar":
            case "deactivate":
            case "stop":
                if (args.length < 2) {
                    sender.sendMessage("§c✖ Uso: §e/avo ciclo desactivar <mundo>");
                    return;
                }
                
                String deactivateWorld = args[1];
                
                if (cicloManager.deactivateCycle(deactivateWorld)) {
                    sender.sendMessage("§a✓ Ciclo desactivado: §e" + deactivateWorld);
                } else {
                    sender.sendMessage("§c✖ El mundo §e" + deactivateWorld + " §cno es un ciclo activo.");
                }
                break;
                
            case "listar":
            case "list":
            case "ls":
                sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                sender.sendMessage("§b§lCICLOS REGISTRADOS");
                sender.sendMessage("");
                
                // NUEVO: Usar sistema de persistencia
                java.util.Collection<me.apocalipsis.ciclos.CicloData> allCiclos = cicloManager.getAllCiclos();
                if (allCiclos.isEmpty()) {
                    sender.sendMessage("  §7No hay ciclos registrados.");
                } else {
                    int activos = 0;
                    int inactivos = 0;
                    
                    for (me.apocalipsis.ciclos.CicloData ciclo : allCiclos) {
                        String cicloWorldName = ciclo.getWorldName();
                        org.bukkit.World w = org.bukkit.Bukkit.getWorld(cicloWorldName);
                        
                        String status;
                        int players = 0;
                        
                        if (w != null) {
                            players = w.getPlayers().size();
                            status = ciclo.isActivo() ? "§a●" : "§e○";
                            activos++;
                        } else {
                            status = ciclo.existe() ? "§7○" : "§c✗";
                            inactivos++;
                        }
                        
                        String existeInfo = ciclo.existe() ? "" : " §8(eliminado)";
                        sender.sendMessage("  " + status + " §e" + cicloWorldName + " §8- §7" + players + " jugador(es)" + existeInfo);
                        sender.sendMessage("    §8└─ §7Jugadores únicos: §e" + ciclo.getJugadoresUnicos() + 
                            " §8| §7Creado: §e" + new java.text.SimpleDateFormat("dd/MM/yyyy").format(ciclo.getFechaCreacion()));
                    }
                    
                    sender.sendMessage("");
                    sender.sendMessage("  §7Total: §e" + allCiclos.size() + " §8(§a" + activos + " cargados§8, §7" + inactivos + " sin cargar§8)");
                }
                
                sender.sendMessage("");
                sender.sendMessage("§7Mundo original: §a" + cicloManager.getOriginalWorld());
                sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                break;
                
            case "info":
                if (args.length < 2) {
                    sender.sendMessage("§c✖ Uso: §e/avo ciclo info <mundo>");
                    return;
                }
                
                String infoWorld = args[1];
                
                // NUEVO: Obtener datos de persistencia
                me.apocalipsis.ciclos.CicloData cicloData = cicloManager.getCicloData(infoWorld);
                org.bukkit.World world = org.bukkit.Bukkit.getWorld(infoWorld);
                
                if (cicloData == null && world == null) {
                    sender.sendMessage("§c✖ El mundo §e" + infoWorld + " §cno existe.");
                    return;
                }
                
                boolean isCycle = cicloManager.isCycleWorld(infoWorld);
                boolean isOriginal = infoWorld.equals(cicloManager.getOriginalWorld());
                
                sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                sender.sendMessage("§b§lINFO: §f" + infoWorld);
                sender.sendMessage("");
                
                if (world != null) {
                    // Mundo está cargado - mostrar datos en vivo
                    sender.sendMessage("  §7Estado: §a✓ Cargado en memoria");
                    sender.sendMessage("  §7Tipo: " + (isCycle ? "§6Ciclo" : isOriginal ? "§aOriginal" : "§7Normal"));
                    sender.sendMessage("  §7Jugadores online: §e" + world.getPlayers().size());
                    sender.sendMessage("  §7Dificultad: §e" + world.getDifficulty());
                    sender.sendMessage("  §7Ambiente: §e" + world.getEnvironment());
                    sender.sendMessage("  §7PvP: " + (world.getPVP() ? "§a✓ Habilitado" : "§c✗ Deshabilitado"));
                    sender.sendMessage("  §7Seed: §e" + world.getSeed());
                    sender.sendMessage("  §7Spawn: §e" + world.getSpawnLocation().getBlockX() + ", " + 
                        world.getSpawnLocation().getBlockY() + ", " + world.getSpawnLocation().getBlockZ());
                }
                
                if (cicloData != null) {
                    // Mostrar datos guardados de persistencia
                    sender.sendMessage("");
                    sender.sendMessage("  §8§l» Datos Guardados:");
                    sender.sendMessage("  §7Existe en disco: " + (cicloData.existe() ? "§a✓ Sí" : "§c✗ No"));
                    sender.sendMessage("  §7Activo: " + (cicloData.isActivo() ? "§a✓ Sí" : "§7○ No"));
                    sender.sendMessage("  §7Jugadores únicos: §e" + cicloData.getJugadoresUnicos());
                    sender.sendMessage("  §7Tiempo total jugado: §e" + cicloData.getTiempoTotalJugado() + " minutos");
                    sender.sendMessage("  §7Fecha creación: §e" + 
                        new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(cicloData.getFechaCreacion()));
                    
                    if (cicloData.getUltimaActivacion() != null) {
                        sender.sendMessage("  §7Última activación: §e" + 
                            new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(cicloData.getUltimaActivacion()));
                    }
                }
                
                sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                break;
                
            case "teleport":
            case "tp":
                if (!(sender instanceof org.bukkit.entity.Player)) {
                    sender.sendMessage("§c✖ Este comando solo puede ser usado por jugadores.");
                    return;
                }
                
                if (args.length < 2) {
                    sender.sendMessage("§c✖ Uso: §e/avo ciclo teleport <mundo>");
                    return;
                }
                
                org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
                String tpWorld = args[1];
                org.bukkit.World targetWorld = org.bukkit.Bukkit.getWorld(tpWorld);
                
                if (targetWorld == null) {
                    player.sendMessage("§c✖ El mundo §e" + tpWorld + " §cno existe.");
                    return;
                }
                
                // Verificar que sea un ciclo diferente al actual
                String currentWorld = player.getWorld().getName();
                boolean isDifferentCycle = !currentWorld.equals(tpWorld) && 
                                          (cicloManager.isCycleWorld(currentWorld) || cicloManager.isCycleWorld(tpWorld));
                
                // Solo admins pueden teleportarse a ciclos diferentes
                if (isDifferentCycle && !player.hasPermission("apocalipsis.ciclo.admin")) {
                    player.sendMessage("§c✖ Solo los administradores pueden teleportarse entre ciclos diferentes.");
                    player.sendMessage("§7Permiso requerido: §eapocalipsis.ciclo.admin");
                    return;
                }
                
                // El WorldChangeListener se encargará automáticamente de guardar/cargar datos
                player.teleport(targetWorld.getSpawnLocation());
                player.sendMessage("§a✓ Teleportado a: §e" + tpWorld);
                break;
                
            case "setspawn":
                if (!(sender instanceof org.bukkit.entity.Player)) {
                    sender.sendMessage("§c✖ Este comando solo puede ser usado por jugadores.");
                    return;
                }
                
                if (!sender.hasPermission("apocalipsis.ciclo.admin")) {
                    sender.sendMessage("§c✖ No tienes permiso para usar este comando.");
                    return;
                }
                
                org.bukkit.entity.Player spawnPlayer = (org.bukkit.entity.Player) sender;
                
                if (args.length < 2) {
                    // Setear spawn del mundo actual
                    String currentWorldName = spawnPlayer.getWorld().getName();
                    org.bukkit.Location playerLoc = spawnPlayer.getLocation();
                    
                    boolean success = cicloManager.setSpawn(currentWorldName, playerLoc);
                    
                    if (success) {
                        spawnPlayer.sendMessage("§a✓ Spawn actualizado para: §e" + currentWorldName);
                        spawnPlayer.sendMessage("§7Coordenadas: §e" + playerLoc.getBlockX() + ", " + 
                            playerLoc.getBlockY() + ", " + playerLoc.getBlockZ());
                    } else {
                        spawnPlayer.sendMessage("§c✖ Error al actualizar el spawn.");
                    }
                } else {
                    // Setear spawn de un mundo específico
                    String targetWorldName = args[1];
                    org.bukkit.World targetSpawnWorld = org.bukkit.Bukkit.getWorld(targetWorldName);
                    
                    if (targetSpawnWorld == null) {
                        spawnPlayer.sendMessage("§c✖ El mundo §e" + targetWorldName + " §cno existe o no está cargado.");
                        return;
                    }
                    
                    org.bukkit.Location playerLoc = spawnPlayer.getLocation();
                    boolean success = cicloManager.setSpawn(targetWorldName, playerLoc);
                    
                    if (success) {
                        spawnPlayer.sendMessage("§a✓ Spawn actualizado para: §e" + targetWorldName);
                        spawnPlayer.sendMessage("§7Coordenadas: §e" + playerLoc.getBlockX() + ", " + 
                            playerLoc.getBlockY() + ", " + playerLoc.getBlockZ());
                    } else {
                        spawnPlayer.sendMessage("§c✖ Error al actualizar el spawn.");
                    }
                }
                break;
                
            case "security":
            case "seguridad":
                if (!sender.hasPermission("apocalipsis.ciclo.admin")) {
                    sender.sendMessage("§c✖ No tienes permiso para usar este comando.");
                    return;
                }
                
                sender.sendMessage("§e§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                sender.sendMessage("§6§lSEGURIDAD DE CICLOS");
                sender.sendMessage("");
                
                // Validar ciclo único activo
                boolean singleActive = cicloManager.validateSingleActiveCycle();
                if (singleActive) {
                    sender.sendMessage("§a✓ Ciclo activo único: CORRECTO");
                } else {
                    sender.sendMessage("§c✗ Ciclo activo único: FALLO");
                    sender.sendMessage("§7  └─ Detectados múltiples ciclos activos");
                }
                
                // Información del ciclo activo
                String activeCycle = cicloManager.getActiveCycle();
                if (activeCycle != null) {
                    sender.sendMessage("§e◆ Ciclo activo actual: §b" + activeCycle);
                    
                    // Verificar spawn seguro
                    org.bukkit.World activeWorld = org.bukkit.Bukkit.getWorld(activeCycle);
                    if (activeWorld != null) {
                        org.bukkit.Location spawn = activeWorld.getSpawnLocation();
                        sender.sendMessage("§e◆ Spawn: §7" + spawn.getBlockX() + ", " + 
                            spawn.getBlockY() + ", " + spawn.getBlockZ());
                    }
                } else {
                    sender.sendMessage("§7◆ No hay ciclo activo");
                }
                
                // Limpieza manual de cooldowns
                cicloManager.cleanupCooldowns();
                sender.sendMessage("§a✓ Cooldowns limpiados");
                
                sender.sendMessage("");
                sender.sendMessage("§e§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                break;
                
            case "fixspawn":
            case "autocorrect":
            case "repairspawn":
                if (!sender.hasPermission("apocalipsis.ciclo.admin")) {
                    sender.sendMessage("§c✖ No tienes permiso para usar este comando.");
                    return;
                }
                
                sender.sendMessage("§e§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                sender.sendMessage("§6§lAUTO-CORRECCIÓN DE SPAWNS");
                sender.sendMessage("");
                
                if (args.length >= 2) {
                    // Corregir spawn de un mundo específico
                    String fixWorldName = args[1];
                    sender.sendMessage("§eVerificando spawn de: §b" + fixWorldName);
                    sender.sendMessage("");
                    
                    boolean success = cicloManager.autoCorrectWorldSpawn(fixWorldName);
                    
                    if (success) {
                        sender.sendMessage("§a✓ Spawn verificado/corregido exitosamente");
                    } else {
                        sender.sendMessage("§c✗ No se pudo corregir el spawn automáticamente");
                        sender.sendMessage("§7  └─ Intenta setear spawn manualmente en ubicación segura");
                    }
                } else {
                    // Corregir spawns de todos los ciclos
                    sender.sendMessage("§eVerificando spawns de TODOS los ciclos...");
                    sender.sendMessage("");
                    
                    org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        cicloManager.autoCorrectAllCycleSpawns();
                        
                        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                            sender.sendMessage("§a✓ Verificación completada. Revisa la consola para detalles.");
                        });
                    });
                }
                
                sender.sendMessage("");
                sender.sendMessage("§e§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                break;
                
            case "confirmar":
            case "confirm":
                if (!(sender instanceof org.bukkit.entity.Player)) {
                    sender.sendMessage("§c✖ Este comando solo puede ser usado por jugadores.");
                    return;
                }
                
                plugin.getConfirmationManager().confirmAction((org.bukkit.entity.Player) sender);
                break;
                
            case "validar":
            case "validate":
            case "repair":
                if (!sender.hasPermission("apocalipsis.ciclo.admin")) {
                    sender.sendMessage("§c✖ No tienes permiso para usar este comando.");
                    return;
                }
                
                sender.sendMessage("§e⚙ Validando integridad de datos de ciclos...");
                cicloManager.validateCiclosIntegrity();
                sender.sendMessage("§a✓ Validación completada. Revisa la consola para más detalles.");
                break;
                
            case "reporte":
            case "report":
                if (!sender.hasPermission("apocalipsis.ciclo.admin")) {
                    sender.sendMessage("§c✖ No tienes permiso para usar este comando.");
                    return;
                }
                
                String report = cicloManager.generateCiclosReport();
                sender.sendMessage("§a✓ Reporte generado:");
                for (String line : report.split("\n")) {
                    sender.sendMessage("§7" + line);
                }
                break;
                
            case "cancelar":
            case "cancel":
                if (!(sender instanceof org.bukkit.entity.Player)) {
                    sender.sendMessage("§c✖ Este comando solo puede ser usado por jugadores.");
                    return;
                }
                
                plugin.getConfirmationManager().cancelConfirmation((org.bukkit.entity.Player) sender);
                break;
                
            case "menu":
            case "gui":
                if (!(sender instanceof org.bukkit.entity.Player)) {
                    sender.sendMessage("§c✖ Este comando solo puede ser usado por jugadores.");
                    return;
                }
                
                org.bukkit.entity.Player menuPlayer = (org.bukkit.entity.Player) sender;
                me.apocalipsis.gui.CicloMenuGUI menu = new me.apocalipsis.gui.CicloMenuGUI(plugin, menuPlayer);
                menu.open();
                break;
                
            case "cambiar":
            case "change":
            case "switch":
                if (!(sender instanceof org.bukkit.entity.Player)) {
                    sender.sendMessage("§c✖ Este comando solo puede ser usado por jugadores.");
                    return;
                }
                
                if (args.length < 2) {
                    sender.sendMessage("§c✖ Uso: §e/avo ciclo cambiar <mundo>");
                    return;
                }
                
                org.bukkit.entity.Player changePlayer = (org.bukkit.entity.Player) sender;
                String changeWorldName = args[1];
                org.bukkit.World changeWorld = org.bukkit.Bukkit.getWorld(changeWorldName);
                
                if (changeWorld == null) {
                    changePlayer.sendMessage("§c✖ El mundo §e" + changeWorldName + " §cno existe.");
                    return;
                }
                
                // Verificar cooldown
                if (!plugin.getCooldownManager().canUse(changePlayer, me.apocalipsis.managers.CooldownManager.CooldownType.CAMBIO_MUNDO)) {
                    plugin.getCooldownManager().sendCooldownMessage(changePlayer, me.apocalipsis.managers.CooldownManager.CooldownType.CAMBIO_MUNDO);
                    return;
                }
                
                // Aplicar cooldown
                plugin.getCooldownManager().applyCooldown(changePlayer, me.apocalipsis.managers.CooldownManager.CooldownType.CAMBIO_MUNDO);
                
                // Iniciar countdown y teleportar
                plugin.getCountdownManager().startTeleportCountdown(changePlayer, changeWorldName, () -> {
                    changePlayer.teleport(changeWorld.getSpawnLocation());
                    
                    // Mostrar BossBar
                    plugin.getCicloBossBarManager().showCycleBossBar(changePlayer, changeWorldName);
                });
                break;
                
            case "eliminar":
            case "delete":
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage("§c✖ Uso: §e/avo ciclo eliminar <mundo>");
                    return;
                }
                
                String deleteWorldName = args[1];
                
                // Verificar que existe
                if (!cicloManager.isCycleWorld(deleteWorldName)) {
                    sender.sendMessage("§c✖ El mundo §e" + deleteWorldName + " §cno es un ciclo activo.");
                    return;
                }
                
                // Solicitar confirmación
                if (sender instanceof org.bukkit.entity.Player) {
                    org.bukkit.entity.Player deletePlayer = (org.bukkit.entity.Player) sender;
                    
                    plugin.getConfirmationManager().requestConfirmation(
                        deletePlayer,
                        me.apocalipsis.managers.ConfirmationManager.ConfirmationType.ELIMINAR_CICLO,
                        new String[]{deleteWorldName},
                        () -> {
                            if (cicloManager.deactivateCycle(deleteWorldName)) {
                                deletePlayer.sendMessage("§a✓ Ciclo eliminado: §e" + deleteWorldName);
                                
                                // Broadcast si está habilitado
                                org.bukkit.configuration.file.FileConfiguration config = plugin.getCicloConfig();
                                if (config.getBoolean("notificaciones.ciclo_eliminado", false)) {
                                    String mensaje = config.getString("mensajes.ciclo_eliminado", 
                                        "&c⚠ El ciclo &e{mundo} &cha sido eliminado.")
                                        .replace("{mundo}", deleteWorldName)
                                        .replace("&", "§");
                                    org.bukkit.Bukkit.broadcastMessage(mensaje);
                                }
                            } else {
                                deletePlayer.sendMessage("§c✖ Error al eliminar el ciclo.");
                            }
                        }
                    );
                }
                break;
                
            case "renombrar":
            case "rename":
                if (args.length < 3) {
                    sender.sendMessage("§c✖ Uso: §e/avo ciclo renombrar <mundo> <nuevo_nombre>");
                    return;
                }
                
                String oldName = args[1];
                String newName = args[2];
                
                // Verificar que el mundo existe
                org.bukkit.World renameWorld = org.bukkit.Bukkit.getWorld(oldName);
                if (renameWorld == null) {
                    sender.sendMessage("§c✖ El mundo §e" + oldName + " §cno existe.");
                    return;
                }
                
                // Solicitar confirmación
                if (sender instanceof org.bukkit.entity.Player) {
                    org.bukkit.entity.Player renamePlayer = (org.bukkit.entity.Player) sender;
                    
                    plugin.getConfirmationManager().requestConfirmation(
                        renamePlayer,
                        me.apocalipsis.managers.ConfirmationManager.ConfirmationType.RENOMBRAR_CICLO,
                        new String[]{oldName, newName},
                        () -> {
                            renamePlayer.sendMessage("§e⚙ Renombrando ciclo...");
                            renamePlayer.sendMessage("§c⚠ NOTA: El mundo en disco NO cambia de nombre.");
                            renamePlayer.sendMessage("§7Solo se actualiza la referencia en ciclos.yml");
                            
                            // Actualizar en configuración
                            org.bukkit.configuration.file.FileConfiguration config = plugin.getCicloConfig();
                            if (config.contains("ciclos." + oldName)) {
                                config.set("ciclos." + newName, config.get("ciclos." + oldName));
                                config.set("ciclos." + oldName, null);
                                plugin.getCicloManager().saveConfig();
                                
                                renamePlayer.sendMessage("§a✓ Ciclo renombrado de §e" + oldName + " §aa §e" + newName);
                                
                                // Broadcast si está habilitado
                                if (config.getBoolean("notificaciones.ciclo_renombrado", false)) {
                                    String mensaje = config.getString("mensajes.ciclo_renombrado", 
                                        "&bEl ciclo &e{viejo} &bha sido renombrado a &a{nuevo}")
                                        .replace("{viejo}", oldName)
                                        .replace("{nuevo}", newName)
                                        .replace("&", "§");
                                    org.bukkit.Bukkit.broadcastMessage(mensaje);
                                }
                            } else {
                                renamePlayer.sendMessage("§c✖ El ciclo no está registrado en ciclos.yml");
                            }
                        }
                    );
                }
                break;
                
            case "stats":
            case "estadisticas":
                if (args.length < 2) {
                    sender.sendMessage("§c✖ Uso: §e/avo ciclo stats <mundo>");
                    return;
                }
                
                String statsWorldName = args[1];
                org.bukkit.World statsWorld = org.bukkit.Bukkit.getWorld(statsWorldName);
                
                if (statsWorld == null) {
                    sender.sendMessage("§c✖ El mundo §e" + statsWorldName + " §cno existe.");
                    return;
                }
                
                // Obtener datos del ciclo
                org.bukkit.configuration.file.FileConfiguration statsConfig = plugin.getCicloConfig();
                org.bukkit.configuration.ConfigurationSection cicloSection = statsConfig.getConfigurationSection("ciclos." + statsWorldName);
                
                sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                sender.sendMessage("§b§lESTADÍSTICAS: §f" + statsWorldName);
                sender.sendMessage("");
                
                if (cicloSection != null) {
                    String fecha = cicloSection.getString("fecha_creacion", "Desconocida");
                    boolean activo = cicloSection.getBoolean("activo", false);
                    String descripcion = cicloSection.getString("descripcion", "Sin descripción");
                    
                    sender.sendMessage("  §7Estado: " + (activo ? "§a✓ Activo" : "§7○ Inactivo"));
                    sender.sendMessage("  §7Creado: §e" + fecha);
                    sender.sendMessage("  §7Descripción: §f" + descripcion);
                }
                
                sender.sendMessage("  §7Jugadores actuales: §e" + statsWorld.getPlayers().size());
                sender.sendMessage("  §7Dificultad: §e" + statsWorld.getDifficulty());
                sender.sendMessage("  §7Ambiente: §e" + statsWorld.getEnvironment());
                sender.sendMessage("  §7PvP: " + (statsWorld.getPVP() ? "§a✓ Habilitado" : "§c✗ Deshabilitado"));
                sender.sendMessage("  §7Entidades: §e" + statsWorld.getEntities().size());
                sender.sendMessage("  §7Chunks cargados: §e" + statsWorld.getLoadedChunks().length);
                sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                break;
                
            default:
                sender.sendMessage("§c✖ Subcomando desconocido: §e" + subCmd);
                sender.sendMessage("§7Usa §e/avo ciclo §7para ver la ayuda.");
                break;
        }
    }
    
    /**
     * Comando /avo rtp - Random Teleport
     * Teletransporta al jugador a una ubicación aleatoria segura en el overworld
     */
    private void cmdRandomTeleport(CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage("§c✖ Este comando solo puede ser usado por jugadores.");
            return;
        }
        
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        
        // Verificar que esté en overworld
        if (player.getWorld().getEnvironment() != org.bukkit.World.Environment.NORMAL) {
            player.sendMessage("§c✖ Solo puedes usar /rtp en el overworld.");
            player.sendMessage("§7Vuelve a la superficie para usar este comando.");
            return;
        }
        
        // Verificar cooldown (5 minutos)
        if (!plugin.getCooldownManager().canUse(player, me.apocalipsis.managers.CooldownManager.CooldownType.RANDOM_TP)) {
            plugin.getCooldownManager().sendCooldownMessage(player, me.apocalipsis.managers.CooldownManager.CooldownType.RANDOM_TP);
            return;
        }
        
        player.sendMessage("§e⚙ Buscando ubicación aleatoria segura...");
        player.sendMessage("§7Esto puede tardar unos segundos.");
        
        // Ejecutar búsqueda asíncrona para no congelar el servidor
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            org.bukkit.World world = player.getWorld();
            org.bukkit.Location safeLoc = findRandomSafeLocation(world, player.getLocation());
            
            // Volver al thread principal para teleportar
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if (safeLoc == null) {
                    player.sendMessage("§c✖ No se pudo encontrar una ubicación segura.");
                    player.sendMessage("§7Intenta de nuevo en unos momentos.");
                    return;
                }
                
                // Aplicar cooldown
                plugin.getCooldownManager().applyCooldown(player, me.apocalipsis.managers.CooldownManager.CooldownType.RANDOM_TP);
                
                // Teleportar con efectos
                player.teleport(safeLoc);
                player.sendMessage("§a✓ ¡Teletransportado a ubicación aleatoria!");
                player.sendMessage("§7Coordenadas: §e" + safeLoc.getBlockX() + ", " + 
                    safeLoc.getBlockY() + ", " + safeLoc.getBlockZ());
                
                // Efectos visuales
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, safeLoc, 50, 0.5, 1, 0.5, 0.1);
                
                // Log para admins
                plugin.getLogger().info("[RTP] " + player.getName() + " teleportado a " + 
                    safeLoc.getBlockX() + ", " + safeLoc.getBlockY() + ", " + safeLoc.getBlockZ());
            });
        });
    }
    
    /**
     * Encuentra una ubicación aleatoria segura en el mundo
     * @param world Mundo donde buscar
     * @param playerLoc Ubicación actual del jugador
     * @return Ubicación segura encontrada o null
     */
    private org.bukkit.Location findRandomSafeLocation(org.bukkit.World world, org.bukkit.Location playerLoc) {
        java.util.Random random = new java.util.Random();
        
        // Rango de búsqueda: 1000 a 5000 bloques del spawn
        int minRadius = 1000;
        int maxRadius = 5000;
        
        // Intentar hasta 10 veces encontrar ubicación segura
        for (int attempt = 0; attempt < 10; attempt++) {
            // Generar coordenadas aleatorias
            double angle = random.nextDouble() * 2 * Math.PI;
            int distance = minRadius + random.nextInt(maxRadius - minRadius);
            
            int randomX = (int) (world.getSpawnLocation().getX() + distance * Math.cos(angle));
            int randomZ = (int) (world.getSpawnLocation().getZ() + distance * Math.sin(angle));
            
            // Obtener bloque más alto en esas coordenadas
            org.bukkit.Location checkLoc = world.getHighestBlockAt(randomX, randomZ).getLocation().add(0, 1, 0);
            
            // Verificar que la ubicación sea segura
            if (isLocationSafeForRTP(checkLoc)) {
                plugin.getLogger().info("[RTP] Ubicación segura encontrada en intento " + (attempt + 1));
                return checkLoc;
            }
        }
        
        plugin.getLogger().warning("[RTP] No se encontró ubicación segura después de 10 intentos");
        return null;
    }
    
    /**
     * Verifica si una ubicación es segura para RTP
     * @param location Ubicación a verificar
     * @return true si es segura
     */
    private boolean isLocationSafeForRTP(org.bukkit.Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        
        org.bukkit.World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        // Verificar límites de altura
        if (y < 60 || y > world.getMaxHeight() - 10) {
            return false;
        }
        
        // Obtener bloques relevantes
        org.bukkit.Material ground = world.getBlockAt(x, y - 1, z).getType();
        org.bukkit.Material feet = world.getBlockAt(x, y, z).getType();
        org.bukkit.Material head = world.getBlockAt(x, y + 1, z).getType();
        
        // Verificar materiales peligrosos
        if (ground == org.bukkit.Material.LAVA || ground == org.bukkit.Material.FIRE || 
            ground == org.bukkit.Material.MAGMA_BLOCK || ground == org.bukkit.Material.WATER ||
            ground == org.bukkit.Material.CAMPFIRE || ground == org.bukkit.Material.SOUL_CAMPFIRE) {
            return false;
        }
        
        // Verificar que pies y cabeza estén despejados
        if (feet.isSolid() || head.isSolid()) {
            return false;
        }
        
        // Verificar que haya suelo sólido
        if (!ground.isSolid()) {
            return false;
        }
        
        // Verificar bioma (evitar océanos)
        org.bukkit.block.Biome biome = world.getBiome(x, y, z);
        if (biome.name().contains("OCEAN") || biome.name().contains("RIVER")) {
            return false;
        }
        
        // Verificar que no haya jugadores muy cerca (mínimo 200 bloques)
        for (org.bukkit.entity.Player nearbyPlayer : world.getPlayers()) {
            if (nearbyPlayer.getLocation().distance(location) < 200) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Comando /avo volver - Permite escapar del End y volver al Overworld
     * Útil cuando jugadores quedan atrapados sin portal
     */
    private void cmdVolver(CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage("§c✖ Este comando solo puede ser usado por jugadores.");
            return;
        }
        
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        
        // Verificar que esté en el End o Nether (NO en Overworld)
        org.bukkit.World.Environment environment = player.getWorld().getEnvironment();
        if (environment == org.bukkit.World.Environment.NORMAL) {
            player.sendMessage("§c✖ Ya estás en el Overworld.");
            player.sendMessage("§7Este comando solo funciona en el End o Nether.");
            return;
        }
        
        // Verificar cooldown (30 segundos para evitar abuso)
        if (!plugin.getCooldownManager().canUse(player, me.apocalipsis.managers.CooldownManager.CooldownType.END_ESCAPE)) {
            plugin.getCooldownManager().sendCooldownMessage(player, me.apocalipsis.managers.CooldownManager.CooldownType.END_ESCAPE);
            return;
        }
        
        // Obtener el ciclo activo del jugador
        String activeCycle = plugin.getCicloManager().getActiveCycle();
        
        if (activeCycle == null) {
            player.sendMessage("§c✖ No hay un ciclo activo disponible.");
            player.sendMessage("§7Contacta a un administrador.");
            plugin.getLogger().warning("[VOLVER] " + player.getName() + " intentó volver pero no hay ciclo activo");
            return;
        }
        
        // Obtener el mundo del ciclo activo
        org.bukkit.World targetWorld = org.bukkit.Bukkit.getWorld(activeCycle);
        
        if (targetWorld == null || targetWorld.getEnvironment() != org.bukkit.World.Environment.NORMAL) {
            player.sendMessage("§c✖ El mundo del ciclo activo no está disponible.");
            player.sendMessage("§7Contacta a un administrador.");
            plugin.getLogger().severe("[VOLVER] Ciclo activo '" + activeCycle + "' no existe o no es overworld");
            return;
        }
        
        // Obtener spawn seguro
        org.bukkit.Location spawnLoc = targetWorld.getSpawnLocation();
        
        // Aplicar cooldown
        plugin.getCooldownManager().applyCooldown(player, me.apocalipsis.managers.CooldownManager.CooldownType.END_ESCAPE);
        
        // Teleportar al jugador
        player.teleport(spawnLoc);
        
        // Mensajes de feedback
        String dimensionName = environment == org.bukkit.World.Environment.THE_END ? "End" : "Nether";
        player.sendMessage("§a✓ ¡Has regresado al Overworld!");
        player.sendMessage("§7Fuiste teletransportado desde el §c" + dimensionName + " §7al spawn de §e" + activeCycle);
        
        // Efectos visuales y sonoros
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
        player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, spawnLoc, 100, 1, 2, 1, 0.3);
        
        // Log para administradores
        plugin.getLogger().info("[VOLVER] " + player.getName() + " escapó del " + dimensionName + " y volvió a " + 
            activeCycle + " (spawn: " + spawnLoc.getBlockX() + ", " + 
            spawnLoc.getBlockY() + ", " + spawnLoc.getBlockZ() + ")");
    }
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    /**
     * Helper para parsear int con valor por defecto
     */
    private static int tryParseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}

