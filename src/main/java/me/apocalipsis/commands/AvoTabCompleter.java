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
                "newday", "endday", "status", "setxp", "mission",
                "tps", "stats", "backup", "cooldown", "debug", "test", "test-alert",
                "reload", "admin", "escanear", "protecciones", "eco", "eco_sombras",
                "evento3", "susurro", "xp", "experience", "nivel", "level", "evasion", "evasiones",
                "autotest", "habilidad", "habilidades", "skill", "skills",
                "blockinfo", "bloque", "blockstats", "skillstats",
                "newrank", "setpermrank", "removepermrank", "listpermranks",
                "canjear", "redeem", "navidad", "menu"
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
                case "setxp":
                case "setps": // Backward compatibility
                case "test-alert":
                case "blockstats": // Stats de bloques de un jugador
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
                
                case "eco_sombras":
                    // Sugerir subcomandos de eco_sombras
                    return Arrays.asList("start", "stop", "fase", "next", "info", "ancla", "nucleo").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                
                case "evento3":
                case "susurro":
                    // Sugerir subcomandos de evento3 (El Susurro en la Piedra Rota)
                    return Arrays.asList("start", "stop", "acto", "next", "info", "fragmento", "grieta").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                
                case "xp":
                case "experience":
                    // Sugerir subcomandos de xp
                    return Arrays.asList("get", "add", "set", "reset").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                
                case "skillstats":
                    // Sugerir subcomandos de skillstats
                    return Arrays.asList("top", "player").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                
                case "evasion":
                case "evasiones":
                    // Sugerir subcomandos de evasion
                    return Arrays.asList("check", "clear", "stats", "history", "reduce", "info", "reload").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                
                case "nivel":
                case "level":
                    // Sugerir jugadores online
                    return plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                
                case "newrank":
                    // Sugerir tipo de rango
                    return Arrays.asList("permanente", "temporal").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                
                case "setpermrank":
                case "removepermrank":
                    // Sugerir jugadores online
                    return plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                
                case "canjear":
                case "redeem":
                    // Sugerir IDs de recompensas disponibles
                    return Arrays.asList(
                        "kit_diamante", "kit_netherite", "elytra_especial", 
                        "bloque_proteccion", "mega_pack"
                    ).stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                
                case "autotest":
                    // Sugerir subcomandos de autotest
                    return Arrays.asList("start", "stop", "run", "suite", "bots", "report", "clear").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                
                case "habilidad":
                case "habilidades":
                case "skill":
                case "skills":
                    // Sugerir subcomandos de habilidades
                    return Arrays.asList("menu", "arbol", "info", "mis", "toggle", "toggles", "comprar", "admin", "reload", "recargar", "refresh").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                
                case "navidad":
                    // Sugerir subcomandos de navidad
                    return Arrays.asList("start", "stop", "status", "reset", "ambiente", "arbol", "santa", "regalos", "fragmentos", "cliffhanger").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        if (args.length == 3) {
            String subCmd = args[0].toLowerCase();
            
            // /avo navidad ambiente|arbol|santa|regalos|fragmentos
            if (subCmd.equals("navidad")) {
                String navidadSubCmd = args[1].toLowerCase();
                switch (navidadSubCmd) {
                    case "ambiente":
                        return Arrays.asList("on", "off").stream()
                            .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                    case "arbol":
                        return Arrays.asList("set", "activar", "desactivar").stream()
                            .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                    case "santa":
                        return Arrays.asList("spawn", "despawn").stream()
                            .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                    case "regalos":
                        return Arrays.asList("start", "stop").stream()
                            .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                    case "fragmentos":
                    case "fragmento":
                        return Arrays.asList("give", "giveall", "info").stream()
                            .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            
            // /avo setpermrank <jugador> <rankId>
            if (subCmd.equals("setpermrank")) {
                // Sugerir IDs de rangos permanentes disponibles
                return new ArrayList<>(plugin.getPermRankManager().getRankIds()).stream()
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
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
            
            // /avo skillstats player <jugador>
            if (subCmd.equals("skillstats") && args[1].equalsIgnoreCase("player")) {
                return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
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
            
            // /avo eco_sombras fase <1-6>
            if (subCmd.equals("eco_sombras") && args[1].equalsIgnoreCase("fase")) {
                return Arrays.asList("1", "2", "3", "4", "5", "6").stream()
                    .filter(s -> s.startsWith(args[2]))
                    .collect(Collectors.toList());
            }
            
            // /avo eco_sombras ancla <1-5>
            if (subCmd.equals("eco_sombras") && args[1].equalsIgnoreCase("ancla")) {
                return Arrays.asList("1", "2", "3", "4", "5").stream()
                    .filter(s -> s.startsWith(args[2]))
                    .collect(Collectors.toList());
            }
            
            // /avo eco_sombras nucleo <spawn|teleport|damage>
            if (subCmd.equals("eco_sombras") && args[1].equalsIgnoreCase("nucleo")) {
                return Arrays.asList("spawn", "teleport", "damage").stream()
                    .filter(s -> s.startsWith(args[2]))
                    .collect(Collectors.toList());
            }
            
            // /avo evento3 acto <1-4>
            if ((subCmd.equals("evento3") || subCmd.equals("susurro")) && args[1].equalsIgnoreCase("acto")) {
                return Arrays.asList("1", "2", "3", "4").stream()
                    .filter(s -> s.startsWith(args[2]))
                    .collect(Collectors.toList());
            }
            
            // /avo evento3 fragmento <spawn>
            if ((subCmd.equals("evento3") || subCmd.equals("susurro")) && args[1].equalsIgnoreCase("fragmento")) {
                return Arrays.asList("spawn").stream()
                    .filter(s -> s.startsWith(args[2]))
                    .collect(Collectors.toList());
            }
            
            // /avo evento3 grieta <spawn>
            if ((subCmd.equals("evento3") || subCmd.equals("susurro")) && args[1].equalsIgnoreCase("grieta")) {
                return Arrays.asList("spawn").stream()
                    .filter(s -> s.startsWith(args[2]))
                    .collect(Collectors.toList());
            }
            
            // /avo setxp <jugador> (tercer argumento puede ser rango o número)
            if (subCmd.equals("setxp") || subCmd.equals("setps")) {
                // Sugerir rangos y algunos valores comunes de XP
                List<String> suggestions = new ArrayList<>();
                suggestions.addAll(Arrays.asList("NOVATO", "EXPLORADOR", "SOBREVIVIENTE", "VETERANO", 
                                                  "LEYENDA", "MAESTRO", "TITAN", "ABSOLUTO"));
                suggestions.addAll(Arrays.asList("100", "500", "1000", "2500", "5000"));
                return suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            // /avo autotest start <evento>
            if (subCmd.equals("autotest") && args[1].equalsIgnoreCase("start")) {
                return Arrays.asList("eco_brasas", "eco_sombras", "evento3", "susurro_piedra_rota").stream()
                    .filter(s -> s.startsWith(args[2]))
                    .collect(Collectors.toList());
            }
            
            // /avo xp get|add|set|reset <jugador>
            if ((subCmd.equals("xp") || subCmd.equals("experience"))) {
                String xpSubCmd = args[1].toLowerCase();
                if (xpSubCmd.equals("get") || xpSubCmd.equals("add") || xpSubCmd.equals("set") || xpSubCmd.equals("reset")) {
                    return plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                }
            }
            
            // /avo evasion check|clear|history|reduce <jugador|all>
            if ((subCmd.equals("evasion") || subCmd.equals("evasiones"))) {
                String evasionSubCmd = args[1].toLowerCase();
                if (evasionSubCmd.equals("check") || evasionSubCmd.equals("history") || evasionSubCmd.equals("reduce")) {
                    return plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                }
                if (evasionSubCmd.equals("clear")) {
                    List<String> suggestions = new ArrayList<>(
                        plugin.getServer().getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList())
                    );
                    suggestions.add("all");
                    return suggestions.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                }
            }
            
            // /avo habilidades info|toggle|comprar <skill_id>
            if (subCmd.equals("habilidad") || subCmd.equals("habilidades") || 
                subCmd.equals("skill") || subCmd.equals("skills")) {
                String habSubCmd = args[1].toLowerCase();
                if (habSubCmd.equals("info") || habSubCmd.equals("toggle") || habSubCmd.equals("comprar") || habSubCmd.equals("buy")) {
                    return Arrays.stream(me.apocalipsis.skills.Skill.values())
                        .map(s -> s.getId())
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                }
                if (habSubCmd.equals("admin")) {
                    return Arrays.asList("give", "remove", "reset", "list").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                }
            }
        }
        
        // args.length == 4: /avo setpermrank <jugador> <rankId> [duration]
        if (args.length == 4 && args[0].equalsIgnoreCase("setpermrank")) {
            // Sugerir duraciones comunes
            return Arrays.asList("permanent", "1d", "7d", "30d", "1h", "24h", "60m").stream()
                .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        // args.length == 4: /avo navidad fragmentos give <player>
        if (args.length == 4 && args[0].equalsIgnoreCase("navidad")) {
            if ((args[1].equalsIgnoreCase("fragmentos") || args[1].equalsIgnoreCase("fragmento")) 
                && args[2].equalsIgnoreCase("give")) {
                return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        
        // args.length == 5: /avo navidad fragmentos give <player> <cantidad>
        // args.length == 4: /avo navidad fragmentos giveall <cantidad>
        if (args.length >= 4 && args[0].equalsIgnoreCase("navidad")) {
            if ((args[1].equalsIgnoreCase("fragmentos") || args[1].equalsIgnoreCase("fragmento"))) {
                if ((args[2].equalsIgnoreCase("give") && args.length == 5) ||
                    (args[2].equalsIgnoreCase("giveall") && args.length == 4)) {
                    return Arrays.asList("1", "2", "3", "5", "10", "20", "50").stream()
                        .filter(s -> s.startsWith(args[args.length - 1]))
                        .collect(Collectors.toList());
                }
            }
        }
        
        // args.length == 4: /avo evasion reduce <jugador> <cantidad>
        if (args.length == 4 && ("evasion".equalsIgnoreCase(args[0]) || "evasiones".equalsIgnoreCase(args[0]))) {
            if ("reduce".equalsIgnoreCase(args[1])) {
                return Arrays.asList("1", "2", "3", "5", "10");
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
        
        // args.length == 4: /avo xp add|set <jugador> <cantidad>
        if (args.length == 4 && ("xp".equalsIgnoreCase(args[0]) || "experience".equalsIgnoreCase(args[0]))) {
            String xpSubCmd = args[1].toLowerCase();
            if (xpSubCmd.equals("add") || xpSubCmd.equals("set")) {
                return Arrays.asList("10", "50", "100", "250", "500", "1000");
            }
        }
        
        // args.length == 4: /avo habilidades admin give|remove|reset <jugador>
        if (args.length == 4 && (args[0].equalsIgnoreCase("habilidad") || args[0].equalsIgnoreCase("habilidades") ||
                                  args[0].equalsIgnoreCase("skill") || args[0].equalsIgnoreCase("skills"))) {
            if (args[1].equalsIgnoreCase("admin")) {
                String adminSubCmd = args[2].toLowerCase();
                if (adminSubCmd.equals("give") || adminSubCmd.equals("remove") || adminSubCmd.equals("reset")) {
                    return plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
                }
            }
        }
        
        // args.length == 5: /avo habilidades admin give|remove <jugador> <skill_id>
        if (args.length == 5 && (args[0].equalsIgnoreCase("habilidad") || args[0].equalsIgnoreCase("habilidades") ||
                                  args[0].equalsIgnoreCase("skill") || args[0].equalsIgnoreCase("skills"))) {
            if (args[1].equalsIgnoreCase("admin")) {
                String adminSubCmd = args[2].toLowerCase();
                if (adminSubCmd.equals("give") || adminSubCmd.equals("remove")) {
                    return Arrays.stream(me.apocalipsis.skills.Skill.values())
                        .map(s -> s.getId())
                        .filter(s -> s.toLowerCase().startsWith(args[4].toLowerCase()))
                        .collect(Collectors.toList());
                }
            }
        }

        return completions;
    }
}
