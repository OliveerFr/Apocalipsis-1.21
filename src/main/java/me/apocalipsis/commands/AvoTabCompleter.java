package me.apocalipsis.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import me.apocalipsis.Apocalipsis;

public class AvoTabCompleter implements TabCompleter {

    private final Apocalipsis plugin;

    public AvoTabCompleter(Apocalipsis plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Nivel 1: subcomandos principales
            List<String> subcommands = Arrays.asList(
                "start", "stop", "force", "skip", "preparacion", "time",
                "newday", "endday", "status", "setps", "mission",
                "tps", "stats", "backup", "cooldown", "debug", "test", "test-alert",
                "reload", "admin", "escanear", "protecciones", "eco"
            );
            
            return subcommands.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCmd = args[0].toLowerCase();

            switch (subCmd) {
                case "force":
                    // Sugerir IDs de desastres válidos
                    return new ArrayList<>(plugin.getDisasterRegistry().getIds()).stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());

                case "status":
                case "setps":
                case "test-alert":
                    // Sugerir nombres de jugadores online
                    return plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());

                case "time":
                    return Arrays.asList("set", "add").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());

                case "mission":
                    return Arrays.asList("give", "complete", "clear").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());

                case "preparacion":
                    // Sugerir minutos comunes
                    List<String> minutes = Arrays.asList("1", "2", "3", "5", "10", "15");
                    return minutes.stream()
                        .filter(s -> s.startsWith(args[1]))
                        .collect(Collectors.toList());

                case "debug":
                    return Arrays.asList("on", "off", "status", "missions", "explore").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                
                case "admin":
                    // Sugerir subcomandos de admin
                    return Arrays.asList("add", "remove", "list").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                
                case "eco":
                    // Sugerir subcomandos de eco
                    return Arrays.asList("start", "stop", "fase", "next", "info", "pulso", "ancla").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        if (args.length == 3) {
            String subCmd = args[0].toLowerCase();
            
            // /avo admin add|remove <jugador>
            if (subCmd.equals("admin")) {
                String adminSubCmd = args[1].toLowerCase();
                if (adminSubCmd.equals("add") || adminSubCmd.equals("remove")) {
                    // Sugerir jugadores online
                    return plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                }
            }
            
            // /avo mission complete|clear <jugador>
            if (subCmd.equals("mission")) {
                String missionSubCmd = args[1].toLowerCase();
                if (missionSubCmd.equals("complete") || missionSubCmd.equals("clear")) {
                    return plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                }
            }
            
            // /avo time set|add <minutos>
            if (subCmd.equals("time")) {
                return Arrays.asList("1", "2", "3", "5", "10", "15", "20", "30").stream()
                    .filter(s -> s.startsWith(args[2]))
                    .collect(Collectors.toList());
            }
            
            // /avo mission give <jugador>
            if (subCmd.equals("mission") && args[1].equalsIgnoreCase("give")) {
                return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            // /avo eco fase <1|2|3>
            if (subCmd.equals("eco") && args[1].equalsIgnoreCase("fase")) {
                return Arrays.asList("1", "2", "3").stream()
                    .filter(s -> s.startsWith(args[2]))
                    .collect(Collectors.toList());
            }
            
            // /avo eco pulso <add|set>
            if (subCmd.equals("eco") && args[1].equalsIgnoreCase("pulso")) {
                return Arrays.asList("add", "set").stream()
                    .filter(s -> s.startsWith(args[2]))
                    .collect(Collectors.toList());
            }
            
            // /avo eco ancla <1|2|3>
            if (subCmd.equals("eco") && args[1].equalsIgnoreCase("ancla")) {
                return Arrays.asList("1", "2", "3").stream()
                    .filter(s -> s.startsWith(args[2]))
                    .collect(Collectors.toList());
            }
        }
        
        // args.length == 4: /avo mission give <jugador> <tipo>
        if (args.length == 4 && "mission".equalsIgnoreCase(args[0]) && "give".equalsIgnoreCase(args[1])) {
            return Arrays.asList("MATAR", "ROMPER", "COLOCAR", "PESCAR", "CRAFTEAR").stream()
                .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        // args.length == 5: /avo mission give <jugador> <tipo> <objetivo>
        if (args.length == 5 && "mission".equalsIgnoreCase(args[0]) && "give".equalsIgnoreCase(args[1])) {
            String tipo = args[3].toUpperCase();
            java.util.List<String> suggestions;
            if ("MATAR".equals(tipo)) {
                suggestions = Arrays.asList("ZOMBIE", "SKELETON", "CREEPER", "SPIDER", "ENDERMAN", "BLAZE");
            } else if ("ROMPER".equals(tipo) || "COLOCAR".equals(tipo)) {
                suggestions = Arrays.asList("STONE", "DIRT", "WOOD", "COBBLESTONE", "IRON_ORE", "GOLD_ORE");
            } else if ("PESCAR".equals(tipo)) {
                suggestions = Arrays.asList("COD", "SALMON", "TREASURE", "JUNK");
            } else if ("CRAFTEAR".equals(tipo)) {
                suggestions = Arrays.asList("DIAMOND_SWORD", "IRON_PICKAXE", "CRAFTING_TABLE", "FURNACE");
            } else {
                suggestions = Arrays.asList("<objetivo>");
            }
            return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[4].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        // args.length == 6: /avo mission give <jugador> <tipo> <objetivo> <meta>
        if (args.length == 6 && "mission".equalsIgnoreCase(args[0]) && "give".equalsIgnoreCase(args[1])) {
            return Arrays.asList("1", "5", "10", "25", "50", "100");
        }

        return completions;
    }
}
