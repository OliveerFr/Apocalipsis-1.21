package me.apocalipsis.events.gameplay;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sistema de diálogos cinematográficos con typing effect.
 * Soporta múltiples speakers y secuencias de diálogos.
 */
public class DialogSystem {
    
    private final Plugin plugin;
    private final Map<String, BukkitTask> activeDialogs = new HashMap<>();
    
    // Speakers predefinidos
    public enum Speaker {
        OBSERVADOR("§7§l[Observador]§r §7", Sound.AMBIENT_CAVE, 0.5f),
        GUARDIAN("§5§l[Guardián]§r §5", Sound.ENTITY_WITHER_AMBIENT, 0.8f),
        FIGURA("§8§l[???]§r §8", Sound.ENTITY_PHANTOM_AMBIENT, 0.3f),
        JUGADOR("§b§l[Tú]§r §b", Sound.ENTITY_PLAYER_BREATH, 1.0f),
        SISTEMA("§e§l[Sistema]§r §e", Sound.BLOCK_NOTE_BLOCK_BELL, 1.2f);
        
        private final String prefix;
        private final Sound sound;
        private final float pitch;
        
        Speaker(String prefix, Sound sound, float pitch) {
            this.prefix = prefix;
            this.sound = sound;
            this.pitch = pitch;
        }
        
        public String getPrefix() { return prefix; }
        public Sound getSound() { return sound; }
        public float getPitch() { return pitch; }
    }
    
    /**
     * Clase para representar un diálogo individual
     */
    public static class Dialog {
        private final Speaker speaker;
        private final String message;
        private final int delayTicks;
        
        public Dialog(Speaker speaker, String message) {
            this(speaker, message, 0);
        }
        
        public Dialog(Speaker speaker, String message, int delayTicks) {
            this.speaker = speaker;
            this.message = message;
            this.delayTicks = delayTicks;
        }
        
        public Speaker getSpeaker() { return speaker; }
        public String getMessage() { return message; }
        public int getDelayTicks() { return delayTicks; }
    }
    
    public DialogSystem(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Muestra un diálogo con typing effect a un jugador
     * 
     * @param player Jugador que recibirá el diálogo
     * @param dialog Diálogo a mostrar
     * @param ticksPerChar Velocidad del typing (ticks entre caracteres, default: 2)
     */
    public void showDialog(Player player, Dialog dialog, int ticksPerChar) {
        String fullMessage = dialog.getSpeaker().getPrefix() + "§o\"" + dialog.getMessage() + "\"";
        AtomicInteger index = new AtomicInteger(0);
        
        // Sonido inicial del speaker
        player.playSound(player.getLocation(), dialog.getSpeaker().getSound(), 0.7f, dialog.getSpeaker().getPitch());
        
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                
                if (index.get() >= fullMessage.length()) {
                    // Sonido final al completar el mensaje
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.3f, 1.5f);
                    cancel();
                    return;
                }
                
                // Mostrar mensaje parcial
                String partial = fullMessage.substring(0, index.incrementAndGet());
                player.sendMessage(partial);
                
                // Sonido de typing
                if (index.get() % 3 == 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.2f, 1.8f);
                }
            }
        }.runTaskTimer(plugin, 0L, ticksPerChar);
        
        activeDialogs.put(player.getUniqueId().toString(), task);
    }
    
    /**
     * Muestra un diálogo con velocidad predeterminada (2 ticks por carácter)
     */
    public void showDialog(Player player, Dialog dialog) {
        showDialog(player, dialog, 2);
    }
    
    /**
     * Muestra una secuencia de diálogos con delays entre ellos
     * 
     * @param player Jugador que recibirá los diálogos
     * @param dialogs Lista de diálogos a mostrar
     */
    public void showDialogSequence(Player player, List<Dialog> dialogs) {
        if (dialogs.isEmpty()) return;
        
        new BukkitRunnable() {
            int currentIndex = 0;
            int waitTicks = 0;
            
            @Override
            public void run() {
                if (!player.isOnline() || currentIndex >= dialogs.size()) {
                    cancel();
                    return;
                }
                
                Dialog dialog = dialogs.get(currentIndex);
                
                // Esperar el delay especificado antes de mostrar
                if (waitTicks < dialog.getDelayTicks()) {
                    waitTicks++;
                    return;
                }
                
                // Mostrar el diálogo
                showDialog(player, dialog);
                
                // Calcular tiempo de espera hasta el siguiente diálogo
                // (tiempo de typing + delay del siguiente)
                int typingTime = dialog.getMessage().length() * 2; // 2 ticks por carácter
                waitTicks = -typingTime; // Compensar el tiempo de typing
                
                currentIndex++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * Muestra un diálogo a todos los jugadores online
     */
    public void broadcastDialog(Dialog dialog) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            showDialog(player, dialog);
        }
    }
    
    /**
     * Muestra una secuencia de diálogos a todos los jugadores online
     */
    public void broadcastDialogSequence(List<Dialog> dialogs) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            showDialogSequence(player, dialogs);
        }
    }
    
    /**
     * Cancela todos los diálogos activos de un jugador
     */
    public void cancelDialogs(Player player) {
        String key = player.getUniqueId().toString();
        if (activeDialogs.containsKey(key)) {
            activeDialogs.get(key).cancel();
            activeDialogs.remove(key);
        }
    }
    
    /**
     * Cancela todos los diálogos activos
     */
    public void cancelAllDialogs() {
        for (BukkitTask task : activeDialogs.values()) {
            task.cancel();
        }
        activeDialogs.clear();
    }
    
    /**
     * Crea una secuencia de diálogos del Observador para el inicio del evento
     */
    public static List<Dialog> createIntroSequence() {
        List<Dialog> sequence = new ArrayList<>();
        sequence.add(new Dialog(Speaker.OBSERVADOR, "Algo se mueve en la oscuridad...", 0));
        sequence.add(new Dialog(Speaker.OBSERVADOR, "No es el mundo recordando. Viene de más lejos.", 100));
        sequence.add(new Dialog(Speaker.OBSERVADOR, "Las sombras... tienen memoria.", 100));
        return sequence;
    }
    
    /**
     * Crea una secuencia de diálogos para el encuentro con el Guardián
     */
    public static List<Dialog> createGuardianIntroSequence() {
        List<Dialog> sequence = new ArrayList<>();
        sequence.add(new Dialog(Speaker.OBSERVADOR, "El guardián despierta...", 0));
        sequence.add(new Dialog(Speaker.GUARDIAN, "Detendré su avance. Sellaré la grieta.", 80));
        sequence.add(new Dialog(Speaker.OBSERVADOR, "Pero... ¿y la fuente?", 80));
        sequence.add(new Dialog(Speaker.GUARDIAN, "¡No hay tiempo para preguntas! ¡Solo acción!", 60));
        return sequence;
    }
    
    /**
     * Crea una secuencia de diálogos para el cliffhanger final
     */
    public static List<Dialog> createCliffhangerSequence() {
        List<Dialog> sequence = new ArrayList<>();
        sequence.add(new Dialog(Speaker.OBSERVADOR, "Han sellado la grieta... pero no la fuente.", 200));
        sequence.add(new Dialog(Speaker.OBSERVADOR, "El eco persiste. La sombra recuerda.", 100));
        sequence.add(new Dialog(Speaker.OBSERVADOR, "Lo que viene... no tiene forma. Aún.", 100));
        sequence.add(new Dialog(Speaker.FIGURA, "Nos volveremos a encontrar...", 200));
        sequence.add(new Dialog(Speaker.FIGURA, "...en las sombras.", 40));
        return sequence;
    }
    
    /**
     * Muestra un susurro aleatorio (diálogo breve y misterioso)
     */
    public void whisper(Player player, String message) {
        Dialog whisper = new Dialog(Speaker.FIGURA, message);
        showDialog(player, whisper, 1); // Más rápido que diálogos normales
        
        // Efecto de sonido tenebroso
        player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_AMBIENT, 0.3f, 0.5f);
    }
    
    /**
     * Lista de susurros aleatorios para ambiente
     */
    private static final String[] WHISPERS = {
        "Observan...",
        "Recuerdan...",
        "No olvidan...",
        "Vuelven...",
        "Esperan...",
        "La oscuridad crece...",
        "El eco persiste...",
        "Nos encontraremos...",
        "La sombra sabe...",
        "No puedes huir..."
    };
    
    /**
     * Muestra un susurro aleatorio
     */
    public void randomWhisper(Player player) {
        String message = WHISPERS[(int) (Math.random() * WHISPERS.length)];
        whisper(player, message);
    }
    
    /**
     * Crea un título cinematográfico con fade in/out
     */
    public void showCinematicTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.5f);
    }
    
    /**
     * Cleanup al finalizar
     */
    public void cleanup() {
        cancelAllDialogs();
    }
}
