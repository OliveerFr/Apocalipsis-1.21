package me.apocalipsis.ciclos;

import me.apocalipsis.Apocalipsis;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Sistema de confirmación y preview para cambios de ciclo.
 * Proporciona información clara sobre qué se perderá y countdown antes de ejecutar.
 * 
 * Características:
 * - Preview de lo que cambiará al activar un ciclo
 * - Countdown configurable antes de teleporte masivo
 * - Cancelación de operaciones pendientes
 * - Notificaciones visuales y sonoras
 * 
 * @author Riolu
 * @version 1.22.55
 */
public class CyclePreviewSystem {
    
    private final Apocalipsis plugin;
    private final CicloManager cicloManager;
    
    // Operaciones pendientes de confirmación
    private final Map<String, PendingOperation> pendingOps = new HashMap<>();
    
    public CyclePreviewSystem(Apocalipsis plugin, CicloManager cicloManager) {
        this.plugin = plugin;
        this.cicloManager = cicloManager;
    }
    
    /**
     * Genera un preview de lo que sucederá al cambiar de ciclo
     * 
     * @param player Jugador que solicita el preview
     * @param targetWorld Mundo objetivo del ciclo
     * @return Información de preview
     */
    public PreviewInfo generatePreview(Player player, String targetWorld) {
        PreviewInfo info = new PreviewInfo();
        info.playerName = player.getName();
        info.targetWorld = targetWorld;
        
        UUID uuid = player.getUniqueId();
        String currentWorld = player.getWorld().getName();
        
        // Verificar si hay datos guardados en el mundo objetivo
        boolean hasDataInTarget = cicloManager.getDataManager().hasData(uuid, targetWorld);
        info.hasExistingData = hasDataInTarget;
        
        if (hasDataInTarget) {
            // Cargar datos del mundo objetivo para mostrar
            var targetData = cicloManager.getDataManager().loadPlayerData(uuid, targetWorld);
            info.targetXP = targetData.getXp();
            info.targetLevel = targetData.getNivel();
            info.targetPS = targetData.getPuntosSupervivencia();
            info.targetSkills = targetData.getSkillsDesbloqueadas().size();
            info.targetRank = targetData.getRangoActual();
        } else {
            // Datos nuevos (jugador nuevo en ese mundo)
            info.targetXP = 0;
            info.targetLevel = 1;
            info.targetPS = 0;
            info.targetSkills = 0;
            info.targetRank = "NOVATO";
        }
        
        // Capturar datos actuales para comparación
        var currentData = cicloManager.getDataManager().captureCurrentState(uuid);
        info.currentXP = currentData.getXp();
        info.currentLevel = currentData.getNivel();
        info.currentPS = currentData.getPuntosSupervivencia();
        info.currentSkills = currentData.getSkillsDesbloqueadas().size();
        info.currentRank = currentData.getRangoActual();
        
        return info;
    }
    
    /**
     * Muestra el preview formateado al jugador
     * 
     * @param player Jugador
     * @param info Información de preview
     */
    public void showPreview(Player player, PreviewInfo info) {
        player.sendMessage("");
        player.sendMessage("§6§l⚠ PREVIEW - CAMBIO DE CICLO ⚠");
        player.sendMessage("§7▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("");
        
        player.sendMessage("§e➤ Mundo destino: §f" + info.targetWorld);
        player.sendMessage("");
        
        if (info.hasExistingData) {
            player.sendMessage("§a✓ Ya tienes datos en este mundo:");
            player.sendMessage("");
            player.sendMessage("  §7Nivel: §f" + info.targetLevel + " §8(XP: " + info.targetXP + ")");
            player.sendMessage("  §7PS: §f" + info.targetPS);
            player.sendMessage("  §7Skills: §f" + info.targetSkills);
            player.sendMessage("  §7Rango: §f" + info.targetRank);
        } else {
            player.sendMessage("§c✖ No tienes datos en este mundo");
            player.sendMessage("§7Comenzarás desde cero:");
            player.sendMessage("");
            player.sendMessage("  §7Nivel: §f1 §8(XP: 0)");
            player.sendMessage("  §7PS: §f0");
            player.sendMessage("  §7Skills: §f0");
            player.sendMessage("  §7Rango: §fNOVATO");
        }
        
        player.sendMessage("");
        player.sendMessage("§e➤ Tus datos actuales se §lGUARDARÁN§r§e:");
        player.sendMessage("");
        player.sendMessage("  §7Nivel: §f" + info.currentLevel + " §8(XP: " + info.currentXP + ")");
        player.sendMessage("  §7PS: §f" + info.currentPS);
        player.sendMessage("  §7Skills: §f" + info.currentSkills);
        player.sendMessage("  §7Rango: §f" + info.currentRank);
        
        player.sendMessage("");
        player.sendMessage("§7▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        player.sendMessage("");
    }
    
    /**
     * Inicia un countdown antes de teleportar a todos los jugadores
     * 
     * @param worldName Mundo objetivo
     * @param seconds Segundos de countdown
     * @param admin Administrador que inició el cambio
     */
    public void startCountdown(String worldName, int seconds, Player admin) {
        String opId = UUID.randomUUID().toString();
        PendingOperation op = new PendingOperation(worldName, seconds, admin.getName());
        pendingOps.put(opId, op);
        
        // Countdown task
        new BukkitRunnable() {
            int remaining = seconds;
            
            @Override
            public void run() {
                // Verificar si fue cancelado
                if (!pendingOps.containsKey(opId)) {
                    cancel();
                    return;
                }
                
                if (remaining <= 0) {
                    // Ejecutar teleporte
                    executeCountdownComplete(opId, worldName);
                    cancel();
                    return;
                }
                
                // Mensajes periódicos
                if (remaining == seconds || remaining <= 10 || remaining % 30 == 0) {
                    String msg = "§e⚠ §lCAMBIO DE CICLO EN §c" + remaining + "s§e ⚠";
                    Bukkit.broadcastMessage(msg);
                    
                    // Sonido
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    }
                }
                
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L); // Cada segundo
    }
    
    /**
     * Ejecuta el teleporte cuando el countdown termina
     */
    private void executeCountdownComplete(String opId, String worldName) {
        PendingOperation op = pendingOps.remove(opId);
        if (op == null) return;
        
        // Mensaje final
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§a§l✓ CAMBIO DE CICLO ACTIVADO ✓");
        Bukkit.broadcastMessage("§7Mundo: §f" + worldName);
        Bukkit.broadcastMessage("§7Iniciado por: §f" + op.adminName);
        Bukkit.broadcastMessage("");
        
        // Teleportar a todos
        int teleported = 0;
        org.bukkit.World targetBukkitWorld = org.bukkit.Bukkit.getWorld(worldName);
        if (targetBukkitWorld != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String currentWorld = player.getWorld().getName();
                
                // Guardar datos del mundo actual
                cicloManager.handlePlayerLeaveWorld(player, currentWorld);
                
                // Teleportar
                player.teleport(targetBukkitWorld.getSpawnLocation());
                teleported++;
            }
        }
        
        Bukkit.broadcastMessage("§a✓ " + teleported + " jugadores teleportados al nuevo ciclo");
    }
    
    /**
     * Cancela un countdown pendiente
     * 
     * @param opId ID de la operación
     * @return true si se canceló
     */
    public boolean cancelCountdown(String opId) {
        return pendingOps.remove(opId) != null;
    }
    
    /**
     * Información de preview
     */
    public static class PreviewInfo {
        public String playerName;
        public String targetWorld;
        public boolean hasExistingData;
        
        // Datos actuales
        public int currentXP;
        public int currentLevel;
        public int currentPS;
        public int currentSkills;
        public String currentRank;
        
        // Datos del mundo objetivo
        public int targetXP;
        public int targetLevel;
        public int targetPS;
        public int targetSkills;
        public String targetRank;
    }
    
    /**
     * Operación pendiente de countdown
     */
    private static class PendingOperation {
        public final String worldName;
        public final int initialSeconds;
        public final String adminName;
        public final long startTime;
        
        public PendingOperation(String worldName, int seconds, String adminName) {
            this.worldName = worldName;
            this.initialSeconds = seconds;
            this.adminName = adminName;
            this.startTime = System.currentTimeMillis();
        }
    }
}
