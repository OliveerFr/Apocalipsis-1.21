package me.apocalipsis.commands;

import me.apocalipsis.Apocalipsis;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Comando /ciclo - Atajo para gestión de ciclos
 * Redirige a los subcomandos de /avo ciclo
 */
public class CicloCommand implements CommandExecutor, TabCompleter {
    
    private final Apocalipsis plugin;
    
    public CicloCommand(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Si no hay argumentos, mostrar ayuda
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        // Redirigir a ApocalipsisCommand
        // Construir argumentos: ["ciclo", ...args]
        String[] newArgs = new String[args.length + 1];
        newArgs[0] = "ciclo";
        System.arraycopy(args, 0, newArgs, 1, args.length);
        
        // Ejecutar comando principal (/avo)
        ApocalipsisCommand mainCommand = (ApocalipsisCommand) plugin.getCommand("avo").getExecutor();
        if (mainCommand != null) {
            return mainCommand.onCommand(sender, command, label, newArgs);
        }
        
        player.sendMessage("§cError interno al procesar el comando.");
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Subcomandos principales
            completions.addAll(Arrays.asList(
                "nuevo", "crear", "listar", "info", "cambiar", 
                "menu", "confirmar", "cancelar", "eliminar", "renombrar", "stats"
            ));
        } else if (args.length == 2) {
            // Segundo argumento: nombre de ciclo
            if (args[0].equalsIgnoreCase("info") || 
                args[0].equalsIgnoreCase("cambiar") ||
                args[0].equalsIgnoreCase("eliminar") ||
                args[0].equalsIgnoreCase("stats")) {
                
                // Listar ciclos disponibles
                if (plugin.getCicloConfig() != null) {
                    completions.addAll(plugin.getCicloConfig().getConfigurationSection("ciclos").getKeys(false));
                }
            }
        }
        
        // Filtrar por lo que el jugador ha escrito
        String partial = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(partial));
        
        return completions;
    }
    
    /**
     * Envía mensaje de ayuda
     */
    private void sendHelp(Player player) {
        player.sendMessage("§8§m                                                ");
        player.sendMessage("§b§lSISTEMA DE CICLOS MULTI-MUNDO");
        player.sendMessage("");
        player.sendMessage("§e/ciclo nuevo §7- Iniciar creación de ciclo");
        player.sendMessage("§e/ciclo crear <nombre> §7- Crear ciclo directo");
        player.sendMessage("§e/ciclo listar §7- Ver todos los ciclos");
        player.sendMessage("§e/ciclo info <ciclo> §7- Información detallada");
        player.sendMessage("§e/ciclo cambiar <ciclo> §7- Cambiar de ciclo");
        player.sendMessage("§e/ciclo menu §7- Abrir menú gráfico");
        player.sendMessage("");
        player.sendMessage("§c§lGESTIÓN (requiere confirmación):");
        player.sendMessage("§e/ciclo eliminar <ciclo> §7- Eliminar un ciclo");
        player.sendMessage("§e/ciclo renombrar <ciclo> <nuevo> §7- Renombrar");
        player.sendMessage("§e/ciclo stats <ciclo> §7- Estadísticas del ciclo");
        player.sendMessage("");
        player.sendMessage("§e/ciclo confirmar §7- Confirmar acción pendiente");
        player.sendMessage("§e/ciclo cancelar §7- Cancelar confirmación");
        player.sendMessage("§8§m                                                ");
    }
}
