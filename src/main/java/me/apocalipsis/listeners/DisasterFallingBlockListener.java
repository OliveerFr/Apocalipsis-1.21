package me.apocalipsis.listeners;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.disaster.ErupcionVolcanica;
import me.apocalipsis.disaster.TormentaGlacial;

/**
 * Listener que previene que los FallingBlock de los desastres del Ciclo 2
 * dejen bloques cuando caen al suelo. Los bloques simplemente desaparecen.
 */
public class DisasterFallingBlockListener implements Listener {
    
    private final Apocalipsis plugin;
    
    public DisasterFallingBlockListener(Apocalipsis plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Cancela el evento de colocación de bloques para FallingBlocks de desastres.
     * Esto hace que los bloques desaparezcan cuando tocan el suelo en lugar de colocarse.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onFallingBlockLand(EntityChangeBlockEvent event) {
        // Solo procesar FallingBlocks
        if (event.getEntityType() != EntityType.FALLING_BLOCK) {
            return;
        }
        
        FallingBlock fb = (FallingBlock) event.getEntity();
        
        // Verificar si este FallingBlock pertenece a un desastre activo
        if (!isDisasterFallingBlock(fb)) {
            return;
        }
        
        // Cancelar el evento para evitar que el bloque se coloque
        event.setCancelled(true);
        
        // NO eliminar el FallingBlock aquí - los desastres tienen su propia lógica
        // para manejar el impacto (explosiones, fuego, etc.) en sus BukkitRunnables
        
        if (plugin.getConfigManager().isDebugCiclo()) {
            plugin.getLogger().info("[DisasterFB] Colocación de bloque de desastre cancelada: " 
                + fb.getBlockData().getMaterial());
        }
    }
    
    /**
     * Verifica si un FallingBlock pertenece a un desastre activo.
     * Comprueba tanto TormentaGlacial como ErupcionVolcanica.
     */
    private boolean isDisasterFallingBlock(FallingBlock fb) {
        if (!plugin.getDisasterController().hasActiveDisaster()) {
            return false;
        }
        
        // Obtener el desastre activo
        Object disaster = plugin.getDisasterController().getCurrentDisaster();
        
        // Verificar si es TormentaGlacial y el bloque está en sus listas
        if (disaster instanceof TormentaGlacial) {
            TormentaGlacial tormenta = (TormentaGlacial) disaster;
            return tormenta.isCristalActivo(fb) || tormenta.isEstalactitaActiva(fb);
        }
        
        // Verificar si es ErupcionVolcanica y el bloque está en su lista
        if (disaster instanceof ErupcionVolcanica) {
            ErupcionVolcanica erupcion = (ErupcionVolcanica) disaster;
            return erupcion.isRocaActiva(fb);
        }
        
        return false;
    }
}
