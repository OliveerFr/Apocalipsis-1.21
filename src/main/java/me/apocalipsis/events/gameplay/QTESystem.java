package me.apocalipsis.events.gameplay;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import me.apocalipsis.Apocalipsis;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema de Quick Time Events (QTE) para momentos críticos del evento.
 * 
 * Funcionalidades:
 * - QTE de click rápido (spam click)
 * - QTE de secuencia (patrón específico)
 * - QTE de timing perfecto (click en ventana precisa)
 * - QTE cooperativo (múltiples jugadores)
 */
public class QTESystem {
    
    private final Apocalipsis plugin;
    private final Map<UUID, ActiveQTE> activeQTEs = new ConcurrentHashMap<>();
    private final Map<UUID, QTEResult> results = new ConcurrentHashMap<>();
    
    public QTESystem(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Tipos de QTE disponibles
     */
    public enum QTEType {
        RAPID_CLICK,        // Spam clicks en tiempo limitado
        SEQUENCE,           // Secuencia de clicks específica (derecha-izquierda-derecha)
        PERFECT_TIMING,     // Click en ventana de tiempo precisa
        COOPERATIVE         // Múltiples jugadores deben clickear simultáneamente
    }
    
    /**
     * Inicia un QTE para un jugador
     */
    public void startQTE(Player player, QTEType type, int duration, QTECallback callback) {
        UUID playerId = player.getUniqueId();
        
        // Cancelar QTE anterior si existe
        if (activeQTEs.containsKey(playerId)) {
            activeQTEs.get(playerId).cancel();
        }
        
        ActiveQTE qte = new ActiveQTE(player, type, duration, callback);
        activeQTEs.put(playerId, qte);
        qte.start();
    }
    
    /**
     * Inicia un QTE cooperativo para múltiples jugadores
     */
    public void startCooperativeQTE(Collection<Player> players, int duration, int requiredSuccess, QTECallback callback) {
        Location center = calculateCenter(players);
        CooperativeQTE coopQTE = new CooperativeQTE(players, duration, requiredSuccess, callback, center);
        coopQTE.start();
    }
    
    /**
     * Registra input de jugador (click)
     */
    public void registerInput(Player player, InputType input) {
        UUID playerId = player.getUniqueId();
        if (!activeQTEs.containsKey(playerId)) return;
        
        ActiveQTE qte = activeQTEs.get(playerId);
        qte.processInput(input);
    }
    
    /**
     * Tipos de input
     */
    public enum InputType {
        LEFT_CLICK,
        RIGHT_CLICK,
        SHIFT_CLICK,
        JUMP
    }
    
    /**
     * Callback para resultados de QTE
     */
    public interface QTECallback {
        void onSuccess(Player player, int score);
        void onFailure(Player player);
        void onCooperativeComplete(Collection<Player> players, int successCount);
    }
    
    /**
     * QTE activo individual
     */
    private class ActiveQTE {
        private final Player player;
        private final QTEType type;
        private final int duration;
        private final QTECallback callback;
        private BukkitTask task;
        
        private int clicks = 0;
        private int correctClicks = 0;
        private List<InputType> sequence = new ArrayList<>();
        private List<InputType> targetSequence = new ArrayList<>();
        private int currentStep = 0;
        private long startTime;
        private boolean completed = false;
        
        public ActiveQTE(Player player, QTEType type, int duration, QTECallback callback) {
            this.player = player;
            this.type = type;
            this.duration = duration;
            this.callback = callback;
        }
        
        public void start() {
            startTime = System.currentTimeMillis();
            
            switch (type) {
                case RAPID_CLICK:
                    startRapidClick();
                    break;
                case SEQUENCE:
                    startSequence();
                    break;
                case PERFECT_TIMING:
                    startPerfectTiming();
                    break;
                default:
                    break;
            }
        }
        
        private void startRapidClick() {
            player.sendTitle("§e§l¡CLICK RÁPIDO!", "§7Clickea lo más rápido posible", 5, duration, 10);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            
            // Barra de progreso en action bar
            task = new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (!player.isOnline() || completed) {
                        cancel();
                        return;
                    }
                    
                    int remaining = duration - ticks;
                    if (remaining <= 0) {
                        complete();
                        cancel();
                        return;
                    }
                    
                    // Action bar con progreso
                    int bars = clicks;
                    String progress = "§e" + "▌".repeat(Math.min(bars, 20));
                    String empty = "§7" + "▌".repeat(Math.max(0, 20 - bars));
                    player.sendActionBar(progress + empty + " §f" + clicks + " clicks");
                    
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
        
        private void startSequence() {
            // Generar secuencia aleatoria
            Random rand = new Random();
            int sequenceLength = 5;
            InputType[] types = {InputType.LEFT_CLICK, InputType.RIGHT_CLICK};
            
            for (int i = 0; i < sequenceLength; i++) {
                targetSequence.add(types[rand.nextInt(types.length)]);
            }
            
            // Mostrar secuencia
            showSequence();
            
            task = new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (!player.isOnline() || completed) {
                        cancel();
                        return;
                    }
                    
                    int remaining = duration - ticks;
                    if (remaining <= 0) {
                        if (currentStep < targetSequence.size()) {
                            callback.onFailure(player);
                            player.sendTitle("§c§l✗ FALLADO", "", 5, 20, 5);
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.5f);
                        }
                        cleanup();
                        cancel();
                        return;
                    }
                    
                    // Mostrar progreso
                    updateSequenceDisplay();
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
        
        private void startPerfectTiming() {
            player.sendTitle("§6§l⚡ TIMING PERFECTO", "§7¡Click cuando veas el flash!", 10, duration, 10);
            
            // Momento perfecto aleatorio entre 40% y 80% del tiempo
            Random rand = new Random();
            int perfectTick = (int) (duration * (0.4 + rand.nextDouble() * 0.4));
            
            task = new BukkitRunnable() {
                int ticks = 0;
                boolean flashShown = false;
                
                @Override
                public void run() {
                    if (!player.isOnline() || completed) {
                        cancel();
                        return;
                    }
                    
                    // Mostrar flash en momento perfecto
                    if (ticks == perfectTick && !flashShown) {
                        player.sendTitle("§e§l✦ AHORA ✦", "", 0, 10, 0);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 2.0f, 2.0f);
                        player.spawnParticle(Particle.FLASH, player.getLocation(), 1);
                        flashShown = true;
                    }
                    
                    int remaining = duration - ticks;
                    if (remaining <= 0) {
                        if (!completed) {
                            callback.onFailure(player);
                            player.sendTitle("§c§l✗ DEMASIADO LENTO", "", 5, 20, 5);
                        }
                        cleanup();
                        cancel();
                        return;
                    }
                    
                    // Barra de progreso
                    int percent = (ticks * 100) / duration;
                    int bars = percent / 5;
                    String progress = "§6" + "▌".repeat(bars);
                    String empty = "§7" + "▌".repeat(20 - bars);
                    player.sendActionBar(progress + empty);
                    
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
        
        public void processInput(InputType input) {
            if (completed) return;
            
            switch (type) {
                case RAPID_CLICK:
                    clicks++;
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
                    break;
                    
                case SEQUENCE:
                    if (currentStep < targetSequence.size()) {
                        if (targetSequence.get(currentStep) == input) {
                            correctClicks++;
                            currentStep++;
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                            player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 2, 0), 3);
                            
                            if (currentStep >= targetSequence.size()) {
                                complete();
                            }
                        } else {
                            // Input incorrecto
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                            player.spawnParticle(Particle.SMOKE, player.getLocation().add(0, 2, 0), 5);
                        }
                    }
                    break;
                    
                case PERFECT_TIMING:
                    // Verificar si está en ventana perfecta
                    long elapsed = System.currentTimeMillis() - startTime;
                    long elapsedTicks = elapsed / 50;
                    int perfectTick = (int) (duration * 0.6); // Centro aproximado
                    int tolerance = 5; // 5 ticks de tolerancia
                    
                    if (Math.abs(elapsedTicks - perfectTick) <= tolerance) {
                        correctClicks = 100; // Score perfecto
                        complete();
                    } else {
                        callback.onFailure(player);
                        player.sendTitle("§c§l✗ DEMASIADO PRONTO/TARDE", "", 5, 20, 5);
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.5f);
                        cleanup();
                    }
                    break;
            }
        }
        
        private void complete() {
            if (completed) return;
            completed = true;
            
            int score = calculateScore();
            callback.onSuccess(player, score);
            
            // Efectos de éxito
            player.sendTitle("§a§l✓ ¡ÉXITO!", "§7Score: " + score, 5, 30, 10);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
            
            cleanup();
        }
        
        private int calculateScore() {
            switch (type) {
                case RAPID_CLICK:
                    return clicks * 10;
                case SEQUENCE:
                    return correctClicks * 20;
                case PERFECT_TIMING:
                    return correctClicks;
                default:
                    return 0;
            }
        }
        
        private void showSequence() {
            StringBuilder sb = new StringBuilder("§eSecuencia: ");
            for (InputType input : targetSequence) {
                switch (input) {
                    case LEFT_CLICK:
                        sb.append("§c◀ ");
                        break;
                    case RIGHT_CLICK:
                        sb.append("§a▶ ");
                        break;
                    default:
                        sb.append("§7? ");
                }
            }
            player.sendMessage(sb.toString());
        }
        
        private void updateSequenceDisplay() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < targetSequence.size(); i++) {
                if (i < currentStep) {
                    sb.append("§a✓ ");
                } else if (i == currentStep) {
                    sb.append("§e→ ");
                } else {
                    InputType input = targetSequence.get(i);
                    switch (input) {
                        case LEFT_CLICK:
                            sb.append("§7◀ ");
                            break;
                        case RIGHT_CLICK:
                            sb.append("§7▶ ");
                            break;
                        default:
                            sb.append("§7? ");
                    }
                }
            }
            player.sendActionBar(sb.toString());
        }
        
        private void cleanup() {
            activeQTEs.remove(player.getUniqueId());
        }
        
        public void cancel() {
            if (task != null) {
                task.cancel();
            }
            cleanup();
        }
    }
    
    /**
     * QTE cooperativo para múltiples jugadores
     */
    private class CooperativeQTE {
        private final Collection<Player> players;
        private final int duration;
        private final int requiredSuccess;
        private final QTECallback callback;
        private final Location center;
        private final Set<UUID> completedPlayers = new HashSet<>();
        private BukkitTask task;
        
        public CooperativeQTE(Collection<Player> players, int duration, int requiredSuccess, QTECallback callback, Location center) {
            this.players = players;
            this.duration = duration;
            this.requiredSuccess = requiredSuccess;
            this.callback = callback;
            this.center = center;
        }
        
        public void start() {
            for (Player p : players) {
                p.sendTitle("§d§l¡COOPERACIÓN!", "§7Todos deben clickear a tiempo", 10, duration, 10);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
            }
            
            task = new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (ticks >= duration) {
                        finish();
                        cancel();
                        return;
                    }
                    
                    // Visual cooperativo
                    if (ticks % 10 == 0) {
                        center.getWorld().spawnParticle(Particle.END_ROD, center, 10, 2, 2, 2, 0.1);
                    }
                    
                    // Mostrar progreso
                    String progress = String.format("§d%d/%d jugadores listos", completedPlayers.size(), requiredSuccess);
                    for (Player p : players) {
                        p.sendActionBar(progress);
                    }
                    
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
        
        public void registerPlayer(Player player) {
            completedPlayers.add(player.getUniqueId());
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 2, 0), 5);
            
            if (completedPlayers.size() >= requiredSuccess) {
                finish();
                task.cancel();
            }
        }
        
        private void finish() {
            if (completedPlayers.size() >= requiredSuccess) {
                callback.onCooperativeComplete(players, completedPlayers.size());
                
                for (Player p : players) {
                    p.sendTitle("§a§l✓ ¡ÉXITO COOPERATIVO!", "", 5, 30, 10);
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                }
            } else {
                for (Player p : players) {
                    p.sendTitle("§c§l✗ COOPERACIÓN FALLIDA", "§7" + completedPlayers.size() + "/" + requiredSuccess, 5, 30, 10);
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.5f);
                }
            }
        }
    }
    
    /**
     * Resultado de QTE
     */
    public static class QTEResult {
        public final boolean success;
        public final int score;
        
        public QTEResult(boolean success, int score) {
            this.success = success;
            this.score = score;
        }
    }
    
    /**
     * Calcula el centro de una colección de jugadores
     */
    private Location calculateCenter(Collection<Player> players) {
        if (players.isEmpty()) return null;
        
        double sumX = 0, sumY = 0, sumZ = 0;
        World world = null;
        
        for (Player p : players) {
            Location loc = p.getLocation();
            sumX += loc.getX();
            sumY += loc.getY();
            sumZ += loc.getZ();
            if (world == null) world = loc.getWorld();
        }
        
        int count = players.size();
        return new Location(world, sumX / count, sumY / count, sumZ / count);
    }
    
    /**
     * Limpia todos los QTEs activos
     */
    public void cleanup() {
        for (ActiveQTE qte : activeQTEs.values()) {
            qte.cancel();
        }
        activeQTEs.clear();
        results.clear();
    }
}
