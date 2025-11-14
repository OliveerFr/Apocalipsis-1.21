package me.apocalipsis.events;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

/**
 * Listener para detectar interacción con grietas del Eco de Brasas
 */
public class EcoBrasasListener implements Listener {
    
    private final EcoBrasasEvent ecoBrasas;
    
    public EcoBrasasListener(EcoBrasasEvent ecoBrasas) {
        this.ecoBrasas = ecoBrasas;
    }
    
    /**
     * Detecta cuando un jugador golpea una entidad (Shulker = grieta)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        
        // Detectar Shulker (hitbox de grieta)
        if (event.getEntity() instanceof Shulker) {
            Shulker shulker = (Shulker) event.getEntity();
            
            if (shulker.getScoreboardTags().contains("eco_grieta_hitbox")) {
                event.setCancelled(true); // No matar el shulker
                ecoBrasas.onGrietaGolpeada(shulker.getLocation(), player);
                return;
            }
        }
        
        // Detectar ArmorStand (por si acaso clickean el label)
        if (event.getEntity() instanceof ArmorStand) {
            ArmorStand entity = (ArmorStand) event.getEntity();
            
            if (entity.getScoreboardTags().contains("eco_grieta_label")) {
                event.setCancelled(true);
                ecoBrasas.onGrietaGolpeada(entity.getLocation(), player);
                return;
            }
        }
    }
    
    /**
     * Detecta cuando un jugador interactúa con entidades (click derecho)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        
        // Detectar Shulker (hitbox de grieta, ancla o altar)
        if (event.getRightClicked() instanceof Shulker) {
            Shulker shulker = (Shulker) event.getRightClicked();
            
            if (shulker.getScoreboardTags().contains("eco_grieta_hitbox")) {
                event.setCancelled(true);
                ecoBrasas.onGrietaGolpeada(shulker.getLocation(), player);
                return;
            }
            
            if (shulker.getScoreboardTags().contains("eco_ancla_hitbox")) {
                event.setCancelled(true);
                ecoBrasas.onAnclaInteractuada(shulker.getLocation(), player);
                return;
            }
            
            if (shulker.getScoreboardTags().contains("eco_altar_hitbox")) {
                event.setCancelled(true);
                ecoBrasas.onAltarInteractuado(shulker.getLocation(), player);
                return;
            }
        }
        
        // ArmorStands (labels de grieta, ancla, altar)
        if (!(event.getRightClicked() instanceof ArmorStand)) return;
        
        ArmorStand entity = (ArmorStand) event.getRightClicked();
        
        // Grieta label
        if (entity.getScoreboardTags().contains("eco_grieta_label")) {
            event.setCancelled(true);
            ecoBrasas.onGrietaGolpeada(entity.getLocation(), player);
            return;
        }
        
        // Ancla label
        if (entity.getScoreboardTags().contains("eco_ancla_label")) {
            event.setCancelled(true);
            ecoBrasas.onAnclaInteractuada(entity.getLocation(), player);
            return;
        }
        
        // Altar label
        if (entity.getScoreboardTags().contains("eco_altar_label")) {
            event.setCancelled(true);
            ecoBrasas.onAltarInteractuado(entity.getLocation(), player);
            return;
        }
    }
    
    /**
     * Detecta cuando muere el guardián del altar
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onGuardianDeath(EntityDeathEvent event) {
        if (!event.getEntity().getScoreboardTags().contains("eco_guardian")) return;
        
        // LOGS: Detectar causa de muerte
        org.bukkit.event.entity.EntityDamageEvent lastDamage = event.getEntity().getLastDamageCause();
        String causa = lastDamage != null ? lastDamage.getCause().name() : "DESCONOCIDA";
        org.bukkit.Bukkit.getLogger().warning(String.format("[EcoBrasas] ¡GUARDIÁN MURIÓ! Causa: %s, Killer: %s, Location: %s",
            causa, event.getEntity().getKiller() != null ? event.getEntity().getKiller().getName() : "NULL",
            event.getEntity().getLocation()));
        
        // Limpiar drops default
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        // Determinar quien hizo más daño
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            org.bukkit.Bukkit.getLogger().warning("[EcoBrasas] Guardián murió SIN killer - no se otorga recompensa");
            return; // Nadie recibe drop si no hay killer
        }
        
        // TRACKING: Registrar participación en muerte del guardián
        ecoBrasas.trackGuardianKill(killer);
        
        // RECOMPENSA ÚNICA: Espada del Guardián Caído
        org.bukkit.inventory.ItemStack espada = EcoBrasasItems.createEspadaGuardian();
        event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), espada);
        
        // Broadcast
        killer.getWorld().playSound(killer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 2.0f, 0.8f);
        org.bukkit.Bukkit.broadcastMessage("§4§l[Eco de Brasas] §e" + killer.getName() + " §7derrotó al §cGuardián del Eco");
        killer.sendMessage("§c§l[+] §fEspada del Guardián Caído §7(solo tú la obtuviste)");
    }
}
