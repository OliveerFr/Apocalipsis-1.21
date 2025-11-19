package me.apocalipsis.events.gameplay;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.function.Consumer;

/**
 * Sistema de choices (decisiones) con consecuencias que afectan
 * el desarrollo y final del evento.
 */
public class ChoiceSystem {
    
    private final Plugin plugin;
    private final Map<UUID, Integer> karmaScores = new HashMap<>();
    private final Map<UUID, List<String>> playerChoices = new HashMap<>();
    private final Map<String, PendingChoice> pendingChoices = new HashMap<>();
    
    // Tipos de karma
    public enum KarmaType {
        LIGHT(1, "§e⬢", "Luz"),
        DARK(-1, "§8⬢", "Oscuridad"),
        NEUTRAL(0, "§7⬢", "Neutro");
        
        private final int value;
        private final String symbol;
        private final String name;
        
        KarmaType(int value, String symbol, String name) {
            this.value = value;
            this.symbol = symbol;
            this.name = name;
        }
        
        public int getValue() { return value; }
        public String getSymbol() { return symbol; }
        public String getName() { return name; }
    }
    
    /**
     * Representa una decisión binaria
     */
    public static class Choice {
        private final String id;
        private final String question;
        private final String optionA;
        private final String optionB;
        private final KarmaType karmaA;
        private final KarmaType karmaB;
        private final Consumer<Player> consequenceA;
        private final Consumer<Player> consequenceB;
        private final int timeoutSeconds;
        
        public Choice(String id, String question, 
                     String optionA, KarmaType karmaA, Consumer<Player> consequenceA,
                     String optionB, KarmaType karmaB, Consumer<Player> consequenceB,
                     int timeoutSeconds) {
            this.id = id;
            this.question = question;
            this.optionA = optionA;
            this.optionB = optionB;
            this.karmaA = karmaA;
            this.karmaB = karmaB;
            this.consequenceA = consequenceA;
            this.consequenceB = consequenceB;
            this.timeoutSeconds = timeoutSeconds;
        }
        
        public String getId() { return id; }
        public String getQuestion() { return question; }
        public String getOptionA() { return optionA; }
        public String getOptionB() { return optionB; }
        public KarmaType getKarmaA() { return karmaA; }
        public KarmaType getKarmaB() { return karmaB; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        
        public void executeConsequenceA(Player player) {
            if (consequenceA != null) consequenceA.accept(player);
        }
        
        public void executeConsequenceB(Player player) {
            if (consequenceB != null) consequenceB.accept(player);
        }
    }
    
    /**
     * Choice que está esperando respuesta
     */
    private static class PendingChoice {
        private final Choice choice;
        private final long expiryTime;
        
        public PendingChoice(Choice choice, int timeoutSeconds) {
            this.choice = choice;
            this.expiryTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        }
        
        public Choice getChoice() { return choice; }
        public boolean isExpired() { return System.currentTimeMillis() > expiryTime; }
    }
    
    public ChoiceSystem(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Presenta una decisión a un jugador
     */
    public void presentChoice(Player player, Choice choice) {
        UUID uuid = player.getUniqueId();
        
        // Guardar choice pendiente
        pendingChoices.put(uuid.toString(), new PendingChoice(choice, choice.getTimeoutSeconds()));
        
        // Mostrar la decisión
        player.sendMessage("");
        player.sendMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§e§l⚡ DECISIÓN REQUERIDA ⚡");
        player.sendMessage("");
        player.sendMessage("§7" + choice.getQuestion());
        player.sendMessage("");
        player.sendMessage("§e§l[A]§r " + choice.getOptionA() + " " + choice.getKarmaA().getSymbol());
        player.sendMessage("§e§l[B]§r " + choice.getOptionB() + " " + choice.getKarmaB().getSymbol());
        player.sendMessage("");
        player.sendMessage("§7Escribe §eA §7o §eB §7en el chat");
        player.sendMessage("§7Tiempo: §e" + choice.getTimeoutSeconds() + "s");
        player.sendMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
        
        // Sonido dramático
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
        
        // Timer de expiración
        new BukkitRunnable() {
            int secondsLeft = choice.getTimeoutSeconds();
            
            @Override
            public void run() {
                if (!player.isOnline() || !pendingChoices.containsKey(uuid.toString())) {
                    cancel();
                    return;
                }
                
                if (secondsLeft <= 0) {
                    // Timeout - elección por defecto (A)
                    makeChoice(player, "A");
                    player.sendMessage("§c§l⏱ Tiempo agotado! Decisión tomada automáticamente.");
                    cancel();
                    return;
                }
                
                // Advertencias de tiempo
                if (secondsLeft <= 5) {
                    player.sendMessage("§c§l⏱ " + secondsLeft + "s restantes!");
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 2.0f);
                }
                
                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    /**
     * Procesa la elección del jugador
     * 
     * @return true si la elección fue válida y procesada
     */
    public boolean makeChoice(Player player, String option) {
        UUID uuid = player.getUniqueId();
        String key = uuid.toString();
        
        if (!pendingChoices.containsKey(key)) {
            return false;
        }
        
        PendingChoice pending = pendingChoices.get(key);
        
        if (pending.isExpired()) {
            pendingChoices.remove(key);
            return false;
        }
        
        Choice choice = pending.getChoice();
        boolean isOptionA = option.equalsIgnoreCase("A");
        boolean isOptionB = option.equalsIgnoreCase("B");
        
        if (!isOptionA && !isOptionB) {
            return false;
        }
        
        // Ejecutar consecuencias
        KarmaType karma;
        if (isOptionA) {
            choice.executeConsequenceA(player);
            karma = choice.getKarmaA();
        } else {
            choice.executeConsequenceB(player);
            karma = choice.getKarmaB();
        }
        
        // Aplicar karma
        applyKarma(player, karma);
        
        // Registrar elección
        List<String> choices = playerChoices.computeIfAbsent(uuid, k -> new ArrayList<>());
        choices.add(choice.getId() + ":" + option);
        
        // Notificar resultado
        player.sendMessage("");
        player.sendMessage("§e§l✓ Decisión registrada: §f" + (isOptionA ? choice.getOptionA() : choice.getOptionB()));
        player.sendMessage("§7Karma: " + karma.getSymbol() + " §7" + karma.getName());
        player.sendMessage("§7Balance actual: " + getKarmaDisplay(player));
        player.sendMessage("");
        
        // Sonido de confirmación
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        
        // Remover de pendientes
        pendingChoices.remove(key);
        
        return true;
    }
    
    /**
     * Aplica karma al jugador
     */
    private void applyKarma(Player player, KarmaType karma) {
        UUID uuid = player.getUniqueId();
        int currentKarma = karmaScores.getOrDefault(uuid, 0);
        karmaScores.put(uuid, currentKarma + karma.getValue());
    }
    
    /**
     * Obtiene el karma actual del jugador
     */
    public int getKarma(Player player) {
        return karmaScores.getOrDefault(player.getUniqueId(), 0);
    }
    
    /**
     * Determina la alineación del jugador según su karma
     */
    public String getAlignment(Player player) {
        int karma = getKarma(player);
        
        if (karma >= 5) return "§e§lLuminoso";
        if (karma >= 2) return "§a§lBenigno";
        if (karma >= -1) return "§7§lNeutral";
        if (karma >= -4) return "§c§lSombrío";
        return "§8§lOscuro";
    }
    
    /**
     * Muestra el karma del jugador en formato visual
     */
    public String getKarmaDisplay(Player player) {
        int karma = getKarma(player);
        String alignment = getAlignment(player);
        
        StringBuilder display = new StringBuilder();
        
        // Barras visuales
        for (int i = -5; i <= 5; i++) {
            if (i == karma) {
                display.append("§e▮");
            } else if (i < karma && i >= 0) {
                display.append("§e▯");
            } else if (i > karma && i <= 0) {
                display.append("§8▯");
            } else if (i > 0) {
                display.append("§7▯");
            } else {
                display.append("§7▯");
            }
        }
        
        return display + " §7(" + alignment + "§7)";
    }
    
    /**
     * Verifica si un jugador tomó una decisión específica
     */
    public boolean madeChoice(Player player, String choiceId, String option) {
        List<String> choices = playerChoices.get(player.getUniqueId());
        if (choices == null) return false;
        
        String target = choiceId + ":" + option;
        return choices.contains(target);
    }
    
    /**
     * Obtiene el número de elecciones tomadas por el jugador
     */
    public int getChoiceCount(Player player) {
        return playerChoices.getOrDefault(player.getUniqueId(), new ArrayList<>()).size();
    }
    
    /**
     * Choices predefinidas para el evento
     */
    
    public static Choice createAnchorChoice(Plugin plugin, LoreSystem loreSystem) {
        return new Choice(
            "anchor_seal",
            "§7Al sellar el ancla, sientes una presencia. Parece... asustada.\n§7¿Continuar con el sellado o mostrar misericordia?",
            "§eContinuar el sellado", 
            KarmaType.DARK,
            p -> {
                p.sendMessage("§8§oEl ancla grita silenciosamente mientras es sellada.");
                p.sendMessage("§8§oSientes que algo se ha perdido para siempre.");
            },
            "§eMostrar misericordia", 
            KarmaType.LIGHT,
            p -> {
                p.sendMessage("§e§oEl ancla se calma. Algo cambia en la atmósfera.");
                p.sendMessage("§e§oRecibes un fragmento de lore como agradecimiento.");
                loreSystem.revealRandomFragment(p, 2);
            },
            30
        );
    }
    
    public static Choice createGuardianChoice() {
        return new Choice(
            "guardian_fate",
            "§7El Guardián yace derrotado. Sus últimas palabras:\n§7\"No soy el enemigo... solo el primero en caer.\"\n§7¿Qué haces?",
            "§eFinalizarlo rápidamente", 
            KarmaType.DARK,
            p -> {
                p.sendMessage("§8§oEl Guardián cae en silencio.");
                p.sendMessage("§8§o\"Gracias... por terminar mi sufrimiento.\"");
            },
            "§eEscuchar sus últimas palabras", 
            KarmaType.LIGHT,
            p -> {
                p.sendMessage("§e§oEl Guardián sonríe levemente.");
                p.sendMessage("§e§o\"La verdad está en las anclas... no en el núcleo...\"");
                p.sendMessage("§e§o\"Ellos... solo querían volver a casa...\"");
            },
            20
        );
    }
    
    public static Choice createFigureChoice(LoreSystem loreSystem) {
        return new Choice(
            "figure_approach",
            "§7La figura misteriosa extiende una mano hacia ti.\n§7¿Aceptas su invitación?",
            "§eAceptar la mano", 
            KarmaType.DARK,
            p -> {
                p.sendMessage("§5§oTu mano toca la sombra.");
                p.sendMessage("§5§oVisiones de futuros posibles inundan tu mente.");
                p.sendMessage("§5§o\"Ahora formas parte del ciclo.\"");
                loreSystem.revealFragment(p, "final_truth");
            },
            "§eRechazar y retroceder", 
            KarmaType.LIGHT,
            p -> {
                p.sendMessage("§7§oRetrocedes lentamente.");
                p.sendMessage("§7§oLa figura asiente con comprensión.");
                p.sendMessage("§7§o\"Quizás en otro ciclo...\"");
            },
            45
        );
    }
    
    /**
     * Limpia todos los datos
     */
    public void cleanup() {
        karmaScores.clear();
        playerChoices.clear();
        pendingChoices.clear();
    }
}
