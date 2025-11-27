package me.apocalipsis.events;

import me.apocalipsis.Apocalipsis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
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
 */
public class SusurroPiedraRotaListener implements Listener {
    
    private final Apocalipsis plugin;
    
    public SusurroPiedraRotaListener(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Trackear cuando un jugador daña una criatura de forma
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        
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
        
        Player player = (Player) event.getDamager();
        
        // Obtener el evento activo
        SusurroPiedraRotaEvent evento = getEventoActivo();
        if (evento == null || !evento.isActive()) {
            return;
        }
        
        // Registrar participación
        UUID uuid = player.getUniqueId();
        evento.getParticipacionCriaturas().merge(uuid, 1, Integer::sum);
    }
    
    /**
     * Manejar muerte de Criaturas de Forma
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
        
        // La lógica de tracking de criaturas muertas está en el evento mismo
        // Solo nos aseguramos de que las drops sean las correctas
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        plugin.getLogger().info("[SusurroPiedraRota] Criatura de Forma eliminada");
    }
    
    /**
     * ALTAR 2: Los drops de Ender Pearl ahora son manejados directamente
     * por procesarAltar2ResonanciaGrupal() que detecta items en el área.
     * Este listener solo previene que las perlas reboten lejos del altar.
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        SusurroPiedraRotaEvent evento = getEventoActivo();
        if (evento == null || !evento.isActive()) return;
        
        if (evento.getActoActual() != SusurroPiedraRotaEvent.Acto.PIEDRA_DESPIERTA) return;
        
        ItemStack item = event.getItemDrop().getItemStack();
        
        // Si es Ender Pearl y el altar actual es 2, dar feedback
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
        
        // Verificar si es criatura de altar
        if (!evento.getCriaturasDeAltar().contains(entity.getUniqueId())) return;
        
        // Encontrar quién la mató
        Player killer = entity.getKiller();
        if (killer == null) return;
        
        UUID uuid = killer.getUniqueId();
        
        // Incrementar contador
        int eliminadas = evento.getCriaturasEliminadasPorJugador().getOrDefault(uuid, 0);
        eliminadas++;
        evento.getCriaturasEliminadasPorJugador().put(uuid, eliminadas);
        
        // Efecto visual
        Location loc = entity.getLocation();
        loc.getWorld().spawnParticle(org.bukkit.Particle.SOUL_FIRE_FLAME, loc, 20, 0.5, 0.5, 0.5, 0.1);
        loc.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_VEX_DEATH, 0.8f, 0.6f);
        
        killer.sendMessage("§5⧖ §eRecuerdo purificado: §f" + eliminadas + "/5");
        
        // Limpiar drops
        event.getDrops().clear();
        event.setDroppedExp(0);
    }
    
    /**
     * ALTAR 4 y 5: Detectar muerte de mobs hostiles cerca del altar
     * Altar 4: La Caza - cuenta kills para progresar
     * Altar 5: La Unión - da XP generoso a los jugadores
     */
    @EventHandler
    public void onMobHostilDeath(EntityDeathEvent event) {
        SusurroPiedraRotaEvent evento = getEventoActivo();
        if (evento == null || !evento.isActive()) return;
        
        // Solo en Acto 1 (Piedra Despierta - donde están los altares)
        if (evento.getActoActual() != SusurroPiedraRotaEvent.Acto.PIEDRA_DESPIERTA) return;
        
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;
        
        int altarActual = evento.getAltarActualGlobal();
        
        // Altar 4: La Caza - procesar kill para progreso
        if (altarActual == 4) {
            evento.procesarKillMobHostilAltar4(killer, entity);
        }
        // Altar 5: La Unión - solo dar XP generoso
        else if (altarActual == 5) {
            evento.procesarKillMobAltar5(killer, entity);
        }
    }
    
    /**
     * Detectar cuando jugador pisa pressure plate del puzzle de memoria
     * NOTA: Este listener está desactivado pero se mantiene por si se reactiva
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        SusurroPiedraRotaEvent evento = getEventoActivo();
        if (evento == null || !evento.isActive()) {
            return;
        }
        
        // LISTENER DE PATRÓN ELIMINADO - Ya no hay minijuego de patrón
        // El código comentado se mantiene como referencia para futuras implementaciones
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
