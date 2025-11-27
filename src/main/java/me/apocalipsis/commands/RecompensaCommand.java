/*
 * Apocalipsis Plugin - Comando de Recompensas
 * Permite a los jugadores reclamar sus recompensas pendientes
 */
package me.apocalipsis.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.apocalipsis.Apocalipsis;

public class RecompensaCommand implements CommandExecutor {
    
    private final Apocalipsis plugin;
    
    public RecompensaCommand(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Verificar que el sistema esté disponible
        if (plugin.getRewardClaimSystem() == null) {
            player.sendMessage("§cEl sistema de recompensas no está disponible.");
            return true;
        }
        
        // Abrir menú de recompensas
        plugin.getRewardClaimSystem().openRewardsMenu(player);
        
        return true;
    }
}
