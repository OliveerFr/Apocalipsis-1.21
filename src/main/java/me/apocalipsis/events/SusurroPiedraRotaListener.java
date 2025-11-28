package me.apocalipsis.events;

import me.apocalipsis.Apocalipsis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Listener para el evento "El Susurro en la Piedra Rota"
 * Maneja:
 * - Death de Criaturas de Forma
 * - Tracking de participación en combate
 * - Sistema avanzado de estadísticas (muertes, daño, combos)
 */
public class SusurroPiedraRotaListener implements Listener {
    
    private final Apocalipsis plugin;
    
    public SusurroPiedraRotaListener(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SISTEMA AVANZADO DE ESTADÍSTICAS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Trackear muerte de jugadores durante el evento
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        SusurroPiedraRotaEvent evento = getEventoActivo();
        if (evento == null || !evento.isActive()) return;
        
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        
        // Registrar muerte en estadísticas
        evento.registrarMuerteJugador(uuid);
    }
    
    /**
     * Trackear TODO el daño hecho y recibido por jugadores
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        SusurroPiedraRotaEvent evento = getEventoActivo();
        if (evento == null || !evento.isActive()) return;
        
        double dano = event.getFinalDamage();
        
        // Si un jugador hace daño
        if (event.getDamager() instanceof Player) {
            Player atacante = (Player) event.getDamager();
            evento.registrarDanoHecho(atacante.getUniqueId(), dano);
        }
        
        // Si un jugador recibe daño
        if (event.getEntity() instanceof Player) {
            Player victima = (Player) event.getEntity();
            evento.registrarDanoRecibido(victima.getUniqueId(), dano);
        }
    }
    
    /**
     * Trackear kills para el sistema de combos (cualquier mob)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAnyMobDeath(EntityDeathEvent event) {
        SusurroPiedraRotaEvent evento = getEventoActivo();
        if (evento == null || !evento.isActive()) return;
        
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        
        // Registrar kill para combo
        evento.registrarKill(killer.getUniqueId());
        
        // Incrementar contador de criaturas
        evento.getParticipacionCriaturas().merge(killer.getUniqueId(), 1, Integer::sum);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // LISTENERS ORIGINALES
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Trackear cuando un jugador daña una criatura de forma
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        
        // Solo para Silverfish (criaturas de forma legacy)
        if (!(event.getEntity() instanceof Silverfish)) {
            return;
        }
        
        Silverfish criatura = (Silverfish) event.getEntity();
        
        // Verificar si es una Criatura de Forma
        if (criatura.customName() == null) {
            return;
        }
        
        String customName = criatura.customName().toString();
        if (!customName.contains("Criatura de Forma")) {
            return;
        }
        
        // El daño ya se trackea en onEntityDamage
    }
    
    /**
     * Manejar muerte de Criaturas de Forma (legacy Silverfish)
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Silverfish)) {
            return;
        }
        
        Silverfish criatura = (Silverfish) event.getEntity();
        
        // Verificar si es una Criatura de Forma
        if (criatura.customName() == null) {
            return;
        }
        
        String customName = criatura.customName().toString();
        if (!customName.contains("Criatura de Forma")) {
            return;
        }
        
        // Obtener el evento activo
        SusurroPiedraRotaEvent evento = getEventoActivo();
        if (evento == null || !evento.isActive()) {
            return;
        }
        
        // Limpiar drops
        event.getDrops().clear();
        event.setDroppedExp(0);
    }
    
    /**
     * ALTAR 2: Los drops de Ender Pearl ahora son manejados directamente
     * por procesarAltar2ResonanciaGrupal() que detecta items en el área.
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        SusurroPiedraRotaEvent evento = getEventoActivo();
        if (evento == null || !evento.isActive()) return;
        
        if (evento.getActoActual() != SusurroPiedraRotaEvent.Acto.PIEDRA_DESPIERTA) return;
        
        ItemStack item = event.getItemDrop().getItemStack();
        
        if (item.getType() == Material.ENDER_PEARL && evento.getAltarActualGlobal() == 2) {
            Player player = event.getPlayer();
            player.sendMessage("§5⧖ §7Lanza la perla hacia el altar... el altar la absorberá.");
        }
    }
    
    /**
     * ALTAR 4: Detectar muerte de criaturas de altar
     */
    @EventHandler
    public void onCriaturaAltarDeath(EntityDeathEvent event) {
        SusurroPiedraRotaEvent evento = getEventoActivo();
        if (evento == null || !evento.isActive()) return;
        
        if (evento.getActoActual() != SusurroPiedraRotaEvent.Acto.PIEDRA_DESPIERTA) return;
        
        LivingEntity entity = event.getEntity();
        
        if (!evento.getCriaturasDeAltar().contains(entity.getUniqueId())) return;
        
        Player killer = entity.getKiller();
        if (killer == null) return;
        
        UUID uuid = killer.getUniqueId();
        
        int eliminadas = evento.getCriaturasEliminadasPorJugador().getOrDefault(uuid, 0);
        eliminadas++;
        evento.getCriaturasEliminadasPorJugador().put(uuid, eliminadas);
        
        Location loc = entity.getLocation();
        loc.getWorld().spawnParticle(org.bukkit.Particle.SOUL_FIRE_FLAME, loc, 20, 0.5, 0.5, 0.5, 0.1);
        loc.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_VEX_DEATH, 0.8f, 0.6f);
        
        killer.sendMessage("§5⧖ §eRecuerdo purificado: §f" + eliminadas + "/5");
        
        event.getDrops().clear();
        event.setDroppedExp(0);
    }
    
    /**
     * ALTAR 4 y 5: Detectar muerte de mobs hostiles cerca del altar
     */
    @EventHandler
    public void onMobHostilDeath(EntityDeathEvent event) {
        SusurroPiedraRotaEvent evento = getEventoActivo();
        if (evento == null || !evento.isActive()) return;
        
        if (evento.getActoActual() != SusurroPiedraRotaEvent.Acto.PIEDRA_DESPIERTA) return;
        
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;
        
        int altarActual = evento.getAltarActualGlobal();
        
        if (altarActual == 4) {
            evento.procesarKillMobHostilAltar4(killer, entity);
        } else if (altarActual == 5) {
            evento.procesarKillMobAltar5(killer, entity);
        }
    }
    
    /**
     * Detectar cuando jugador pisa pressure plate del puzzle de memoria
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // LISTENER DE PATRÓN ELIMINADO
    }
    
    /**
     * Obtener el evento activo si existe
     */
    private SusurroPiedraRotaEvent getEventoActivo() {
        EventController controller = plugin.getEventController();
        if (controller == null) {
            return null;
        }
        
        EventBase currentEvent = controller.getActiveEvent();
        if (currentEvent instanceof SusurroPiedraRotaEvent) {
            return (SusurroPiedraRotaEvent) currentEvent;
        }
        
        return null;
    }
}
