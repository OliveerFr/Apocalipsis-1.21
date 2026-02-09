package me.apocalipsis.commands;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.security.AntiFarmSecurityManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
 * Comando /security para gestionar el sistema de seguridad anti-autoclick
 * Solo accesible por administradores
 */
public class SecurityCommand implements CommandExecutor, TabCompleter {
    
    private final Apocalipsis plugin;
    private final AntiFarmSecurityManager securityManager;
    
    public SecurityCommand(Apocalipsis plugin) {
        this.plugin = plugin;
        this.securityManager = plugin.getSecurityManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar permisos
        if (!sender.hasPermission("apocalipsis.admin") && !sender.isOp()) {
            sender.sendMessage("§c¡No tienes permiso para usar este comando!");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "info" -> handleInfo(sender, args);
            case "clear", "limpiar" -> handleClear(sender, args);
            case "reload" -> handleReload(sender);
            case "help", "ayuda" -> sendHelp(sender);
            default -> {
                sender.sendMessage("§cSubcomando desconocido. Usa §e/security help");
                return true;
            }
        }
        
        return true;
    }
    
    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUso: /security info <jugador>");
            return;
        }
        
        String playerName = args[1];
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage("§cJugador no encontrado.");
            return;
        }
        
        UUID uuid = target.getUniqueId();
        String info = securityManager.getPlayerSecurityInfo(uuid);
        
        sender.sendMessage("§e§l=== Info de Seguridad: " + target.getName() + " ===");
        sender.sendMessage(info);
    }
    
    private void handleClear(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUso: /security clear <jugador>");
            return;
        }
        
        String playerName = args[1];
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage("§cJugador no encontrado.");
            return;
        }
        
        UUID uuid = target.getUniqueId();
        securityManager.clearStrikes(uuid);
        
        sender.sendMessage("§a✓ Strikes limpiados para " + target.getName());
        
        // Notificar al jugador si está online
        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            onlineTarget.sendMessage("§a§l✓ STRIKES DE SEGURIDAD LIMPIADOS");
            onlineTarget.sendMessage("§7Tus penalizaciones han sido removidas por un administrador.");
        }
        
        plugin.getLogger().info("[Security] " + sender.getName() + " limpió strikes de " + target.getName());
    }
    
    private void handleReload(CommandSender sender) {
        try {
            securityManager.reload();
            sender.sendMessage("§a✓ Configuración de seguridad recargada.");
            plugin.getLogger().info("[Security] " + sender.getName() + " recargó la configuración de seguridad");
        } catch (Exception e) {
            sender.sendMessage("§c✗ Error al recargar: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§e§l=== Sistema de Seguridad Anti-Autoclick ===");
        sender.sendMessage("§7Este sistema detecta y previene autoclick, macros y bots.");
        sender.sendMessage("");
        sender.sendMessage("§e/security info <jugador> §7- Ver info de seguridad");
        sender.sendMessage("§e/security clear <jugador> §7- Limpiar strikes");
        sender.sendMessage("§e/security reload §7- Recargar configuración");
        sender.sendMessage("§e/security help §7- Mostrar esta ayuda");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("apocalipsis.admin") && !sender.isOp()) {
            return List.of();
        }
        
        if (args.length == 1) {
            return Arrays.asList("info", "clear", "reload", "help");
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("clear"))) {
            // Sugerir jugadores online
            List<String> players = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                players.add(p.getName());
            }
            return players;
        }
        
        return List.of();
    }
}
