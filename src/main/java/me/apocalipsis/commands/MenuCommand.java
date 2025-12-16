package me.apocalipsis.commands;

import me.apocalipsis.Apocalipsis;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /menu - Abre el menú principal de Apocalipsis
 * Atajo directo para /avo menu
 */
public class MenuCommand implements CommandExecutor {

    private final Apocalipsis plugin;

    public MenuCommand(Apocalipsis plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return true;
        }

        if (plugin.getMainMenuManager() == null) {
            sender.sendMessage("§cEl menú principal no está disponible.");
            return true;
        }

        plugin.getMainMenuManager().openMainMenu(player);
        return true;
    }
}
