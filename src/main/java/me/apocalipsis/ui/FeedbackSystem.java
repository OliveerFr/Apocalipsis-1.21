package me.apocalipsis.ui;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import me.apocalipsis.Apocalipsis;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema de feedback visual para acciones del jugador
 * 
 * Funcionalidades:
 * - Números de daño flotantes (holograma)
 * - Indicadores de hit (marcadores en pantalla)
 * - Contador de combos con bonificaciones
 * - Indicadores direccionales de daño recibido
 * - Indicadores en bordes de pantalla para amenazas
 */
public class FeedbackSystem {
    
    private final Apocalipsis plugin;
    
    // Tracking de combos
    private final Map<UUID, ComboTracker> combos = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastHitTime = new ConcurrentHashMap<>();
    private static final long COMBO_WINDOW_MS = 3000; // 3 segundos
    
    public FeedbackSystem(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // DAMAGE NUMBERS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Muestra un número de daño flotante
     */
    public void showDamageNumber(Location location, double damage, boolean isCritical) {
        new BukkitRunnable() {
            int ticks = 0;
            double y = 0;
            
            @Override
            public void run() {
                if (ticks >= 30) {
                    cancel();
                    return;
                }
                
                Location displayLoc = location.clone().add(0, y, 0);
                
                // Crear hologram temporal
                ArmorStand hologram = location.getWorld().spawn(displayLoc, ArmorStand.class);
                hologram.setVisible(false);
                hologram.setGravity(false);
                hologram.setMarker(true);
                
                if (isCritical) {
                    hologram.setCustomName("§c§l✦ " + String.format("%.1f", damage) + " §c§l✦");
                    location.getWorld().spawnParticle(Particle.CRIT, displayLoc, 3, 0.1, 0.1, 0.1, 0);
                } else {
                    hologram.setCustomName("§e" + String.format("%.1f", damage));
                }
                
                hologram.setCustomNameVisible(true);
                
                // Remover después de 1 tick
                Bukkit.getScheduler().runTaskLater(plugin, hologram::remove, 1L);
                
                y += 0.1;
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        // Partículas adicionales para críticos
        if (isCritical) {
            location.getWorld().spawnParticle(Particle.FLASH, location, 1);
        }
    }
    
    /**
     * Muestra un número de curación
     */
    public void showHealNumber(Location location, double heal) {
        new BukkitRunnable() {
            int ticks = 0;
            double y = 0;
            
            @Override
            public void run() {
                if (ticks >= 30) {
                    cancel();
                    return;
                }
                
                Location displayLoc = location.clone().add(0, y, 0);
                
                ArmorStand hologram = location.getWorld().spawn(displayLoc, ArmorStand.class);
                hologram.setVisible(false);
                hologram.setGravity(false);
                hologram.setMarker(true);
                hologram.setCustomName("§a+" + String.format("%.1f", heal));
                hologram.setCustomNameVisible(true);
                
                Bukkit.getScheduler().runTaskLater(plugin, hologram::remove, 1L);
                
                y += 0.1;
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        location.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, location, 5, 0.3, 0.3, 0.3, 0);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // HIT MARKERS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Muestra un indicador de hit
     */
    public void showHitMarker(Player player, boolean isCritical) {
        Location crosshair = player.getEyeLocation().add(player.getLocation().getDirection().multiply(2));
        
        if (isCritical) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.5f, 1.5f);
            player.spawnParticle(Particle.ENCHANTED_HIT, crosshair, 10, 0.2, 0.2, 0.2, 0);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.3f, 1.2f);
            player.spawnParticle(Particle.CRIT, crosshair, 5, 0.1, 0.1, 0.1, 0);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // COMBO SYSTEM
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Registra un hit y actualiza el combo
     */
    public void registerHit(Player player) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastHit = lastHitTime.get(uuid);
        
        // Verificar si está dentro del combo window
        boolean isCombo = lastHit != null && (currentTime - lastHit) < COMBO_WINDOW_MS;
        
        ComboTracker tracker = combos.computeIfAbsent(uuid, k -> new ComboTracker());
        
        if (isCombo) {
            tracker.increment();
            showComboCounter(player, tracker.getCount());
        } else {
            tracker.reset();
            tracker.increment();
        }
        
        lastHitTime.put(uuid, currentTime);
        
        // Auto-reset después de 3 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Long checkTime = lastHitTime.get(uuid);
            if (checkTime != null && (System.currentTimeMillis() - checkTime) >= COMBO_WINDOW_MS) {
                combos.remove(uuid);
                lastHitTime.remove(uuid);
            }
        }, 60L);
    }
    
    /**
     * Muestra el contador de combo
     */
    private void showComboCounter(Player player, int combo) {
        if (combo < 2) return;
        
        String color = combo >= 10 ? "§c§l" : combo >= 5 ? "§6§l" : "§e§l";
        String message = color + "COMBO x" + combo;
        
        player.sendActionBar(message);
        
        // Efectos especiales en hitos
        if (combo == 5) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
        } else if (combo == 10) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.5f);
            player.spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
        }
    }
    
    /**
     * Obtiene el combo actual de un jugador
     */
    public int getCombo(Player player) {
        ComboTracker tracker = combos.get(player.getUniqueId());
        return tracker != null ? tracker.getCount() : 0;
    }
    
    /**
     * Resetea el combo de un jugador
     */
    public void resetCombo(Player player) {
        combos.remove(player.getUniqueId());
        lastHitTime.remove(player.getUniqueId());
    }
    
    /**
     * Clase interna para trackear combos
     */
    private static class ComboTracker {
        private int count = 0;
        
        public void increment() {
            count++;
        }
        
        public void reset() {
            count = 0;
        }
        
        public int getCount() {
            return count;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // DIRECTIONAL DAMAGE INDICATORS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Muestra de dónde viene el daño
     */
    public void showDirectionalDamageIndicator(Player player, Location damageSource) {
        Vector playerDir = player.getLocation().getDirection();
        Vector toSource = damageSource.toVector().subtract(player.getLocation().toVector()).normalize();
        
        // Calcular ángulo
        double angle = Math.toDegrees(Math.atan2(toSource.getX(), toSource.getZ()) - Math.atan2(playerDir.getX(), playerDir.getZ()));
        if (angle < 0) angle += 360;
        
        // Determinar dirección
        String direction;
        if (angle > 315 || angle <= 45) {
            direction = "↑"; // Adelante
        } else if (angle > 45 && angle <= 135) {
            direction = "→"; // Derecha
        } else if (angle > 135 && angle <= 225) {
            direction = "↓"; // Atrás
        } else {
            direction = "←"; // Izquierda
        }
        
        player.sendActionBar("§c§l" + direction + " DAÑO RECIBIDO " + direction);
        
        // Partículas en la dirección
        Location indicatorLoc = player.getLocation().add(toSource.multiply(2)).add(0, 1, 0);
        player.spawnParticle(Particle.DAMAGE_INDICATOR, indicatorLoc, 5, 0.2, 0.2, 0.2, 0);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SCREEN EDGE INDICATORS
    // ═══════════════════════════════════════════════════════════════════
    
    public enum IndicatorType {
        DANGER,     // Rojo - Amenaza inmediata
        WARNING,    // Naranja - Precaución
        OBJECTIVE,  // Verde - Objetivo
        INFO        // Blanco - Información
    }
    
    /**
     * Muestra un indicador en el borde de la pantalla
     */
    public void showScreenEdgeIndicator(Player player, Location target, IndicatorType type, int durationTicks) {
        new BukkitRunnable() {
            int remaining = durationTicks;
            
            @Override
            public void run() {
                if (remaining <= 0 || !player.isOnline()) {
                    cancel();
                    return;
                }
                
                Location playerLoc = player.getLocation();
                Vector direction = target.toVector().subtract(playerLoc.toVector()).normalize();
                
                // Posición en el borde del campo de visión
                Location indicatorLoc = playerLoc.clone().add(direction.multiply(3)).add(0, 1.5, 0);
                
                Particle particle;
                String color;
                
                switch (type) {
                    case DANGER:
                        particle = Particle.LAVA;
                        color = "§c";
                        break;
                    case WARNING:
                        particle = Particle.FLAME;
                        color = "§6";
                        break;
                    case OBJECTIVE:
                        particle = Particle.HAPPY_VILLAGER;
                        color = "§a";
                        break;
                    case INFO:
                    default:
                        particle = Particle.END_ROD;
                        color = "§f";
                        break;
                }
                
                player.spawnParticle(particle, indicatorLoc, 2, 0.1, 0.1, 0.1, 0);
                
                double distance = playerLoc.distance(target);
                player.sendActionBar(color + "⬢ " + String.format("%.1f", distance) + "m");
                
                remaining -= 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Limpia el tracking de un jugador
     */
    public void cleanup(Player player) {
        resetCombo(player);
    }
    
    /**
     * Limpia todo el tracking
     */
    public void cleanupAll() {
        combos.clear();
        lastHitTime.clear();
    }
}
