package me.apocalipsis.events.gameplay;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import me.apocalipsis.Apocalipsis;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema de feedback visual para el jugador
 * 
 * Funcionalidades:
 * - Damage numbers flotantes sobre enemigos
 * - Hit markers visuales al golpear
 * - Screen edge indicators para ataques fuera de vista
 * - Directional damage indicator
 * - Heal numbers en verde
 * - Combo counter visible
 * - Critical hit feedback diferenciado
 */
public class FeedbackSystem {
    
    private final Apocalipsis plugin;
    private final Map<UUID, ComboTracker> comboTrackers = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastHitTime = new ConcurrentHashMap<>();
    
    public FeedbackSystem(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Muestra un damage number flotante
     */
    public void showDamageNumber(Location location, double damage, boolean isCritical) {
        new BukkitRunnable() {
            double y = 0;
            int ticks = 0;
            final int maxTicks = 30;
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                // Movimiento flotante hacia arriba
                Location displayLoc = location.clone().add(0, 2 + y, 0);
                
                // Color y formato según tipo
                String text;
                Color color;
                
                if (isCritical) {
                    text = "§c§l✦ " + String.format("%.1f", damage) + " §c§l✦";
                    color = Color.RED;
                    
                    // Partículas extra para crítico
                    location.getWorld().spawnParticle(Particle.CRIT, displayLoc, 3, 0.1, 0.1, 0.1, 0.1);
                } else {
                    text = "§e" + String.format("%.1f", damage);
                    color = Color.YELLOW;
                }
                
                // Hologram simulado con armor stands invisibles
                ArmorStand hologram = location.getWorld().spawn(displayLoc, ArmorStand.class);
                hologram.setVisible(false);
                hologram.setGravity(false);
                hologram.setMarker(true);
                hologram.setCustomName(text);
                hologram.setCustomNameVisible(true);
                hologram.setInvulnerable(true);
                
                // Remover después de 1 tick
                Bukkit.getScheduler().runTaskLater(plugin, hologram::remove, 1L);
                
                // Partículas de número
                location.getWorld().spawnParticle(Particle.DUST, displayLoc, 2, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(color, 0.8f));
                
                y += 0.1;
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * Muestra un heal number flotante en verde
     */
    public void showHealNumber(Location location, double heal) {
        new BukkitRunnable() {
            double y = 0;
            int ticks = 0;
            final int maxTicks = 30;
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                Location displayLoc = location.clone().add(0, 2 + y, 0);
                String text = "§a+" + String.format("%.1f", heal);
                
                // Hologram
                ArmorStand hologram = location.getWorld().spawn(displayLoc, ArmorStand.class);
                hologram.setVisible(false);
                hologram.setGravity(false);
                hologram.setMarker(true);
                hologram.setCustomName(text);
                hologram.setCustomNameVisible(true);
                hologram.setInvulnerable(true);
                
                Bukkit.getScheduler().runTaskLater(plugin, hologram::remove, 1L);
                
                // Partículas verdes
                location.getWorld().spawnParticle(Particle.DUST, displayLoc, 2, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(Color.GREEN, 0.8f));
                location.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, displayLoc, 1);
                
                y += 0.1;
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * Muestra un hit marker al golpear
     */
    public void showHitMarker(Player player, boolean isCritical) {
        // Sonido de hit
        if (isCritical) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.7f, 1.2f);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.3f, 2.0f);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.5f, 1.0f);
        }
        
        // Partículas en la mira del jugador
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        Location hitMarkerLoc = eyeLoc.clone().add(direction.multiply(2));
        
        if (isCritical) {
            player.spawnParticle(Particle.CRIT, hitMarkerLoc, 5, 0.1, 0.1, 0.1, 0.1);
            player.spawnParticle(Particle.FLASH, hitMarkerLoc, 1);
        } else {
            player.spawnParticle(Particle.CRIT, hitMarkerLoc, 2, 0.05, 0.05, 0.05, 0.05);
        }
        
        // Registrar hit para combo
        registerHit(player);
    }
    
    /**
     * Registra un golpe para el sistema de combos
     */
    private void registerHit(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastHit = lastHitTime.get(playerId);
        
        // Ventana de combo: 3 segundos
        boolean isCombo = lastHit != null && (currentTime - lastHit) < 3000;
        
        ComboTracker tracker = comboTrackers.computeIfAbsent(playerId, k -> new ComboTracker());
        
        if (isCombo) {
            tracker.increment();
        } else {
            tracker.reset();
            tracker.increment();
        }
        
        lastHitTime.put(playerId, currentTime);
        
        // Mostrar combo si hay 2+ hits
        if (tracker.getCount() >= 2) {
            showComboCounter(player, tracker.getCount());
        }
        
        // Auto-reset después de 3 segundos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Long lastCheck = lastHitTime.get(playerId);
            if (lastCheck != null && System.currentTimeMillis() - lastCheck >= 3000) {
                ComboTracker t = comboTrackers.get(playerId);
                if (t != null) {
                    t.reset();
                }
            }
        }, 60L);
    }
    
    /**
     * Muestra el contador de combo
     */
    private void showComboCounter(Player player, int combo) {
        String message = "§6§l" + combo + " §e§lCOMBO";
        
        // Color según combo
        if (combo >= 10) {
            message = "§c§l" + combo + " §4§lCOMBO §c§l✦";
        } else if (combo >= 5) {
            message = "§6§l" + combo + " §e§lCOMBO §6§l★";
        }
        
        player.sendActionBar(message);
        
        // Sonido especial por hitos
        if (combo == 5) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation(), 3);
        } else if (combo == 10) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            player.spawnParticle(Particle.FIREWORK, player.getLocation(), 10);
        } else if (combo % 5 == 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.3f);
        }
    }
    
    /**
     * Obtiene el combo actual de un jugador
     */
    public int getCombo(Player player) {
        ComboTracker tracker = comboTrackers.get(player.getUniqueId());
        return tracker != null ? tracker.getCount() : 0;
    }
    
    /**
     * Resetea el combo de un jugador
     */
    public void resetCombo(Player player) {
        ComboTracker tracker = comboTrackers.get(player.getUniqueId());
        if (tracker != null) {
            tracker.reset();
        }
        lastHitTime.remove(player.getUniqueId());
    }
    
    /**
     * Muestra indicador direccional de daño recibido
     */
    public void showDirectionalDamageIndicator(Player player, Location damageSource) {
        Location playerLoc = player.getLocation();
        Vector toSource = damageSource.toVector().subtract(playerLoc.toVector()).normalize();
        
        // Calcular ángulo relativo
        Vector playerDirection = playerLoc.getDirection();
        double angle = Math.atan2(toSource.getZ(), toSource.getX()) - Math.atan2(playerDirection.getZ(), playerDirection.getX());
        angle = Math.toDegrees(angle);
        
        // Normalizar ángulo
        while (angle < -180) angle += 360;
        while (angle > 180) angle -= 360;
        
        // Determinar dirección
        String direction;
        if (angle > -45 && angle <= 45) {
            direction = "→"; // Derecha
        } else if (angle > 45 && angle <= 135) {
            direction = "↓"; // Atrás
        } else if (angle > -135 && angle <= -45) {
            direction = "↑"; // Frente
        } else {
            direction = "←"; // Izquierda
        }
        
        // Mostrar indicador
        player.sendActionBar("§c§l" + direction + " DAÑO RECIBIDO " + direction);
        
        // Partículas en los bordes
        Location indicatorLoc = playerLoc.clone().add(toSource.multiply(2)).add(0, 1, 0);
        player.spawnParticle(Particle.DAMAGE_INDICATOR, indicatorLoc, 5, 0.3, 0.3, 0.3, 0);
        player.spawnParticle(Particle.DUST, indicatorLoc, 3, 0.2, 0.2, 0.2, 0,
            new Particle.DustOptions(Color.RED, 1.5f));
        
        // Sonido direccional
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.6f, 0.8f);
    }
    
    /**
     * Muestra indicadores en los bordes de la pantalla
     */
    public void showScreenEdgeIndicator(Player player, Location threat, IndicatorType type) {
        Location playerLoc = player.getLocation();
        Vector toThreat = threat.toVector().subtract(playerLoc.toVector()).normalize();
        
        // Calcular posición del indicador
        Location indicatorLoc = playerLoc.clone().add(toThreat.multiply(3)).add(0, 1, 0);
        
        // Visuales según tipo
        Particle particle;
        Color color;
        String message;
        
        switch (type) {
            case DANGER:
                particle = Particle.FLAME;
                color = Color.RED;
                message = "§c§l⚠ PELIGRO";
                break;
            case WARNING:
                particle = Particle.DUST;
                color = Color.ORANGE;
                message = "§e§l⚠ ALERTA";
                break;
            case OBJECTIVE:
                particle = Particle.END_ROD;
                color = Color.LIME;
                message = "§a§l✦ OBJETIVO";
                break;
            default:
                particle = Particle.CLOUD;
                color = Color.WHITE;
                message = "§7§l• INFO";
        }
        
        // Partículas
        player.spawnParticle(particle, indicatorLoc, 5, 0.2, 0.2, 0.2, 0.05);
        if (particle == Particle.DUST) {
            player.spawnParticle(Particle.DUST, indicatorLoc, 5, 0.2, 0.2, 0.2, 0,
                new Particle.DustOptions(color, 1.2f));
        }
        
        // Action bar
        double distance = playerLoc.distance(threat);
        player.sendActionBar(message + " §7(" + String.format("%.1f", distance) + "m)");
    }
    
    /**
     * Tipos de indicador
     */
    public enum IndicatorType {
        DANGER,
        WARNING,
        OBJECTIVE,
        INFO
    }
    
    /**
     * Tracker de combos
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
    
    /**
     * Limpia el feedback de un jugador
     */
    public void cleanup(Player player) {
        UUID playerId = player.getUniqueId();
        comboTrackers.remove(playerId);
        lastHitTime.remove(playerId);
    }
    
    /**
     * Limpia todo el feedback
     */
    public void cleanupAll() {
        comboTrackers.clear();
        lastHitTime.clear();
    }
}
