package me.apocalipsis.disaster;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.disaster.adapters.PerformanceAdapter;
import me.apocalipsis.state.TimeService;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;

public class DisasterRegistry {

    private final Map<String, Disaster> disasters = new HashMap<>();
    private Apocalipsis plugin; // [FIX] Para logging

    public void registerDefaults(Apocalipsis plugin, MessageBus messageBus, SoundUtil soundUtil, 
                                TimeService timeService, PerformanceAdapter performanceAdapter) {
        this.plugin = plugin; // [FIX] Guardar referencia
        
        // [FIX DUPLICACIÓN] Limpiar desastres anteriores antes de registrar nuevos
        // Esto previene duplicación si el plugin se recarga
        plugin.getLogger().info(String.format("[DisasterRegistry] Limpiando %d desastres anteriores", disasters.size()));
        clearAll();
        
        plugin.getLogger().info("[DisasterRegistry] Registrando desastres nuevos...");
        register(new HuracanNew(plugin, messageBus, soundUtil, timeService, performanceAdapter));
        register(new LluviaFuegoNew(plugin, messageBus, soundUtil, timeService, performanceAdapter));
        register(new TerremotoNew(plugin, messageBus, soundUtil, timeService, performanceAdapter));
        // EcoBrasas movido a EventController - NO es un desastre automático
        plugin.getLogger().info(String.format("[DisasterRegistry] ✓ %d desastres registrados", disasters.size()));
    }

    public void register(Disaster disaster) {
        String id = disaster.getId();
        if (disasters.containsKey(id)) {
            if (plugin != null) {
                plugin.getLogger().warning(String.format("[DisasterRegistry] Reemplazando desastre existente: %s", id));
            }
        }
        disasters.put(id, disaster);
        if (plugin != null) {
            plugin.getLogger().info(String.format("[DisasterRegistry] Registrado: %s", id));
        }
    }

    public Disaster get(String id) {
        return disasters.get(id);
    }

    public Set<String> getIds() {
        return disasters.keySet();
    }

    public boolean exists(String id) {
        return disasters.containsKey(id);
    }
    
    /**
     * [FIX DUPLICACIÓN] Limpia todos los desastres registrados
     */
    public void clearAll() {
        // Detener cada desastre si está activo
        for (Disaster disaster : disasters.values()) {
            if (disaster.isActive()) {
                disaster.stop();
            }
        }
        disasters.clear();
    }
}
