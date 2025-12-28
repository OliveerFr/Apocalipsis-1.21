/*
 * Apocalipsis Plugin - Comando de Tutorial
 * Copyright (c) 2025 Apocalipsis Plugin
 * 
 * Licensed under the MIT License.
 */
package me.apocalipsis.tutorial;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.tutorial.ProgressiveDifficultySystem.DifficultyPhase;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Comando /tutorial para gestionar el sistema de tutorial
 */
public class TutorialCommand implements CommandExecutor, TabCompleter {
    
    private final Apocalipsis plugin;
    private final TutorialManager tutorialManager;
    private final ProgressiveDifficultySystem difficultySystem;
    private final TutorialMetrics metrics;
    
    public TutorialCommand(Apocalipsis plugin, TutorialManager tutorialManager,
                          ProgressiveDifficultySystem difficultySystem, TutorialMetrics metrics) {
        this.plugin = plugin;
        this.tutorialManager = tutorialManager;
        this.difficultySystem = difficultySystem;
        this.metrics = metrics;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Uso: /tutorial <progreso|reset|fase|stats>");
                return true;
            }
            
            // Sin argumentos, mostrar progreso
            showProgress((Player) sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "progreso":
            case "progress":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Solo jugadores pueden usar este comando.");
                    return true;
                }
                showProgress((Player) sender);
                return true;
                
            case "reset":
                if (!sender.hasPermission("apocalipsis.tutorial.admin")) {
                    sender.sendMessage(ChatColor.RED + "No tienes permiso.");
                    return true;
                }
                
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /tutorial reset <jugador>");
                    return true;
                }
                
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
                    return true;
                }
                
                tutorialManager.resetPlayerTutorial(target.getUniqueId());
                sender.sendMessage(ChatColor.GREEN + "Tutorial reseteado para " + target.getName());
                target.sendMessage(ChatColor.YELLOW + "Tu tutorial ha sido reseteado por un administrador.");
                return true;
                
            case "fase":
            case "phase":
                if (!sender.hasPermission("apocalipsis.tutorial.admin")) {
                    sender.sendMessage(ChatColor.RED + "No tienes permiso.");
                    return true;
                }
                
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /tutorial fase <jugador> <1-5>");
                    return true;
                }
                
                Player phaseTarget = Bukkit.getPlayer(args[1]);
                if (phaseTarget == null) {
                    sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
                    return true;
                }
                
                try {
                    int phase = Integer.parseInt(args[2]);
                    if (phase < 1 || phase > 5) {
                        sender.sendMessage(ChatColor.RED + "Fase debe estar entre 1 y 5.");
                        return true;
                    }
                    
                    difficultySystem.setPlayerPhase(phaseTarget.getUniqueId(), phase);
                    tutorialManager.updateTutorialBuffs(phaseTarget);
                    
                    sender.sendMessage(ChatColor.GREEN + "Fase establecida a " + phase + " para " + phaseTarget.getName());
                    phaseTarget.sendMessage(ChatColor.YELLOW + "Tu fase de tutorial ha sido cambiada a " + phase);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Fase debe ser un número entre 1 y 5.");
                }
                return true;
                
            case "stats":
            case "metricas":
                if (!sender.hasPermission("apocalipsis.tutorial.admin")) {
                    sender.sendMessage(ChatColor.RED + "No tienes permiso.");
                    return true;
                }
                
                metrics.generateReport();
                sender.sendMessage(ChatColor.GREEN + "Reporte de métricas generado en consola.");
                return true;
                
            default:
                sender.sendMessage(ChatColor.RED + "Subcomando desconocido. Usa: progreso, reset, fase, stats");
                return true;
        }
    }
    
    private void showProgress(Player player) {
        DifficultyPhase phase = difficultySystem.getPlayerPhase(player);
        long playedMinutes = difficultySystem.getPlayedTimeMinutes(player);
        long remainingMinutes = difficultySystem.getRemainingTimeToNextPhase(player);
        String timeFormatted = difficultySystem.formatRemainingTime(remainingMinutes);
        
        TutorialManager.TutorialState state = tutorialManager.getTutorialState(player.getUniqueId());
        int completedStages = state != null ? state.getCurrentStage() : 0;
        
        player.sendMessage("");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&8╔═══════════════════════════════════════════════╗"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&8║     &6&l📊 PROGRESO DEL TUTORIAL&8                  ║"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&8╚═══════════════════════════════════════════════╝"));
        player.sendMessage("");
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&e⏰ Tiempo jugado: &f" + playedMinutes + " minutos &7/ 240 minutos"));
        player.sendMessage("");
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&6🎯 Fase Actual: &f" + phase.getName()));
        
        // Barra de progreso
        int percent = phase.getPercentDifficulty();
        String progressBar = createProgressBar(percent, 10);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "   &7Dificultad: &c" + percent + "% " + progressBar));
        player.sendMessage("");
        
        if (!phase.isGlobalDifficulty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&e📈 Siguiente Fase en: &f" + timeFormatted));
            player.sendMessage("");
        }
        
        // Buffs activos
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6🔥 Buffs Activos:"));
        if (phase.hasPassiveRegeneration()) {
            int regenLevel = phase.getPhaseNumber() == 1 ? 2 : 1;
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "   &a✓ Regeneración " + regenLevel + " &7(hasta Fase 3)"));
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "   &7• Sin regeneración pasiva"));
        }
        
        if (phase.getXpBonus() > 1.0) {
            int bonusPercent = (int) ((phase.getXpBonus() - 1.0) * 100);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "   &a✓ XP Bonus +" + bonusPercent + "%"));
        }
        
        if (!phase.allowKnockback()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "   &a✓ Protección contra knockback"));
        }
        
        player.sendMessage("");
        
        // Etapas
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&6📚 Etapas Completadas: &f" + completedStages + " / 7"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "   " + (completedStages >= 1 ? "&a✓" : "&7⬜") + " &7Paso 1: Menú Principal"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "   " + (completedStages >= 2 ? "&a✓" : "&7⬜") + " &7Paso 2: Desastres"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "   " + (completedStages >= 3 ? "&a✓" : "&7⬜") + " &7Paso 3: Supervivencia"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "   " + (completedStages >= 4 ? "&a✓" : "&7⬜") + " &7Paso 4: Sistema de Rangos"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "   " + (completedStages >= 5 ? "&a✓" : "&e⏳") + " &7Paso 5: Demo de EXPLORADOR"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "   " + (completedStages >= 6 ? "&a✓" : "&7⬜") + " &7Paso 6: Habilidades"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "   " + (completedStages >= 7 ? "&a✓" : "&7⬜") + " &7Paso 7: Comandos Clave"));
        
        player.sendMessage("");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&7💡 Usa &f/avo menu &7para continuar el tutorial"));
        player.sendMessage("");
    }
    
    private String createProgressBar(int percent, int bars) {
        int filled = (percent * bars) / 100;
        StringBuilder bar = new StringBuilder("&c");
        
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("&7░");
            }
        }
        
        return ChatColor.translateAlternateColorCodes('&', bar.toString());
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("progreso", "reset", "fase", "stats"));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("fase"))) {
            // Sugerir jugadores online
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("fase")) {
            completions.addAll(Arrays.asList("1", "2", "3", "4", "5"));
        }
        
        return completions;
    }
}
