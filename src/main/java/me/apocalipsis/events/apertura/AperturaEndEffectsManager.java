package me.apocalipsis.events.apertura;

import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;

/**
 * Gestiona los efectos visuales y sonoros del evento
 */
public class AperturaEndEffectsManager {
    
    private final Apocalipsis plugin;
    private final MessageBus messageBus;
    private final SoundUtil soundUtil;
    
    private BukkitTask endermanEffectsTask;
    
    public AperturaEndEffectsManager(Apocalipsis plugin, MessageBus messageBus, SoundUtil soundUtil) {
        this.plugin = plugin;
        this.messageBus = messageBus;
        this.soundUtil = soundUtil;
    }
    
    public void mostrarTareaEnderman() {
        // Efectos cinematográficos
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§8§l...", "", 15, 50, 15);
            p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1.5f, 0.6f);
            p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 0.3f);
            
            // Oscuridad envolvente
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.DARKNESS, 100, 0, false, false, false));
        }
        
        new BukkitRunnable() {
            @Override
            public void run() {
                mostrarTituloObserver("§8§o...", "§7§oEste umbral… lo recuerdo sellado.", 10, 80, 30);
                
                // Partículas de recuerdo
                for (Player p : Bukkit.getOnlinePlayers()) {
                    Location loc = p.getLocation();
                    p.getWorld().spawnParticle(Particle.ENCHANT, loc.clone().add(0, 2.5, 0), 40, 2, 0.5, 2, 0.5);
                    p.getWorld().spawnParticle(Particle.PORTAL, loc, 20, 1.5, 1, 1.5, 0.1);
                }
            }
        }.runTaskLater(plugin, 50L);
        
        // Mostrar tarea
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§d§l⚡ PRUEBA DEL VACÍO ⚡");
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§8[§7...§8] §7Emisarios del §5End §7han despertado.");
                Bukkit.broadcastMessage("§8[§7...§8] §7Tienen que §5enfrentar §7a uno.");
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§e§l► TAREA 1: §5Eliminar un §l⚡ EMISARIO DEL END §5§l⚡");
                Bukkit.broadcastMessage("§8§oAparecerán continuamente hasta que cumplas tu misión...");
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.6f);
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.0f, 0.8f);
                    p.sendTitle("§d§l⚡ PRUEBA I ⚡", "§5Enfrenten al Emisario del End", 10, 60, 20);
                }
            }
        }.runTaskLater(plugin, 120L);
    }
    
    public void mostrarTareaObsidiana() {
        // Fade oscuro dramático
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§8§l█", "§8§l█", 20, 60, 20);
            
            // Oscuridad profunda
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.DARKNESS, 160, 0, false, false, false));
        }
        
        new BukkitRunnable() {
            @Override
            public void run() {
                mostrarTituloObserver("§8§o...", "§7§oCuántas veces he visto este momento.", 10, 100, 30);
                
                // Efectos de tristeza
                for (Player p : Bukkit.getOnlinePlayers()) {
                    Location loc = p.getLocation();
                    p.getWorld().spawnParticle(Particle.WARPED_SPORE, loc.clone().add(0, 2, 0), 30, 1.5, 1, 1.5, 0.02);
                    p.getWorld().spawnParticle(Particle.SOUL, loc, 15, 1, 1, 1, 0.05);
                    p.playSound(loc, Sound.PARTICLE_SOUL_ESCAPE, 0.8f, 0.5f);
                }
            }
        }.runTaskLater(plugin, 80L);
        
        // Mostrar tarea
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§8§l⚡ PRUEBA DE LA FORJA ⚡");
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§8[§7...§8] §7Necesitan material del §0Nether§7.");
                Bukkit.broadcastMessage("§8[§7...§8] §7Obsidiana. §8Nacida del fuego y agua.");
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§e§l► TAREA 2: §8Recolectar §l10 Obsidiana");
                Bukkit.broadcastMessage("§8§oEl portal necesita materiales resistentes...");
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.7f);
                    p.sendTitle("§8§l⚡ PRUEBA II ⚡", "§8Recolecten Obsidiana", 10, 60, 20);
                }
            }
        }.runTaskLater(plugin, 180L);
    }
    
    public void mostrarTareaOjoEnder() {
        Bukkit.broadcastMessage("§8[§7...§8] §7El mundo está… apurado.");
        
        // Efectos de persecución
        for (Player p : Bukkit.getOnlinePlayers()) {
            Location loc = p.getLocation();
            p.getWorld().spawnParticle(Particle.SCULK_SOUL, loc.clone().add(0, 0.2, 0), 25, 2, 0.2, 2, 0.1);
            p.playSound(loc, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.3f, 0.8f);
        }
        
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= 10) {
                    cancel();
                    return;
                }
                
                for (Player p : Bukkit.getOnlinePlayers()) {
                    Location loc = p.getLocation().clone().add(
                        (Math.random() - 0.5) * 10,
                        Math.random() * 3,
                        (Math.random() - 0.5) * 10
                    );
                    p.getWorld().spawnParticle(Particle.SCULK_SOUL, loc, 5, 0.5, 0.5, 0.5, 0.02);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 40L, 10L);
        
        // Mostrar tarea
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§d§l⚡ PRUEBA DE LA VISIÓN ⚡");
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§8[§7...§8] §7El portal necesita §5poder§7.");
                Bukkit.broadcastMessage("§8[§7...§8] §7Un ojo que §dvea §7a través del vacío.");
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§e§l► TAREA 3: §dConseguir un §lOjo de Ender");
                Bukkit.broadcastMessage("§8§oEl portal debe reconocer su destino...");
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§8§l━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_EYE_LAUNCH, 1.0f, 1.2f);
                    p.sendTitle("§d§l⚡ PRUEBA III ⚡", "§dConsigan un Ojo de Ender", 10, 60, 20);
                }
            }
        }.runTaskLater(plugin, 140L);
    }
    
    public void iniciarEfectosEnderman() {
        endermanEffectsTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Buscar endermans del evento y aplicar efectos
                for (World world : Bukkit.getWorlds()) {
                    for (org.bukkit.entity.Entity entity : world.getEntities()) {
                        if (entity instanceof org.bukkit.entity.Enderman enderman) {
                            if (enderman.customName() != null) {
                                Location loc = enderman.getLocation();
                                
                                // Partículas púrpuras constantes
                                loc.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.1);
                                loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 2, 0), 3, 0.2, 0.3, 0.2, 0.02);
                                
                                // Rayo de luz ocasional
                                if (Math.random() < 0.1) {
                                    for (int y = 0; y < 10; y++) {
                                        loc.getWorld().spawnParticle(Particle.END_ROD, 
                                            loc.clone().add(0, y, 0), 1, 0.1, 0.1, 0.1, 0);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
    
    private void mostrarTituloObserver(String titulo, String subtitulo, int fadeIn, int stay, int fadeOut) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(titulo, subtitulo, fadeIn, stay, fadeOut);
        }
    }
    
    public void detener() {
        if (endermanEffectsTask != null) {
            endermanEffectsTask.cancel();
        }
    }
}