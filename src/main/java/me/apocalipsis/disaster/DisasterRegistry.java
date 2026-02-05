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
        
        // Verificar si usar desastres nuevos (Ciclo 2) o viejos (Ciclo 1)
        boolean usarNuevos = plugin.getConfig().getBoolean("ciclo.usar_desastres_nuevos", true);
        
        plugin.getLogger().info("[DisasterRegistry] ═══════════════════════════════════════════════");
        plugin.getLogger().info("[DisasterRegistry] Registrando desastres " + (usarNuevos ? "NUEVOS (Ciclo 2)" : "ANTIGUOS (Ciclo 1)") + "...");
        plugin.getLogger().info("[DisasterRegistry] ═══════════════════════════════════════════════");
        
        try {
            if (usarNuevos) {
                // CICLO 2: Nuevos desastres elementales
                plugin.getLogger().info("[DisasterRegistry] Creando TormentaGlacial...");
                register(new TormentaGlacial(plugin, messageBus, soundUtil, timeService, performanceAdapter));
                
                plugin.getLogger().info("[DisasterRegistry] Creando TormentaElectrica...");
                register(new TormentaElectrica(plugin, messageBus, soundUtil, timeService, performanceAdapter));
                
                plugin.getLogger().info("[DisasterRegistry] Creando ErupcionVolcanica...");
                register(new ErupcionVolcanica(plugin, messageBus, soundUtil, timeService, performanceAdapter));
            } else {
                // CICLO 1: Desastres originales
                plugin.getLogger().info("[DisasterRegistry] Creando HuracanNew...");
                register(new HuracanNew(plugin, messageBus, soundUtil, timeService, performanceAdapter));
                
                plugin.getLogger().info("[DisasterRegistry] Creando LluviaFuegoNew...");
                register(new LluviaFuegoNew(plugin, messageBus, soundUtil, timeService, performanceAdapter));
                
                plugin.getLogger().info("[DisasterRegistry] Creando TerremotoNew...");
                register(new TerremotoNew(plugin, messageBus, soundUtil, timeService, performanceAdapter));
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[DisasterRegistry] ¡ERROR CRÍTICO al registrar desastres!");
            plugin.getLogger().severe("[DisasterRegistry] " + e.getMessage());
            e.printStackTrace();
        }
        
        // EcoBrasas movido a EventController - NO es un desastre automático
        
        // [FIX] Validación post-registro
        plugin.getLogger().info("[DisasterRegistry] ═══════════════════════════════════════════════");
        plugin.getLogger().info("[DisasterRegistry] ✓ " + disasters.size() + " desastres registrados:");
        for (String id : disasters.keySet()) {
            Disaster d = disasters.get(id);
            if (d != null) {
                plugin.getLogger().info("[DisasterRegistry]   • " + id + " (" + d.getClass().getSimpleName() + ")");
            } else {
                plugin.getLogger().severe("[DisasterRegistry]   ✗ " + id + " (NULL - ERROR)");
            }
        }
        plugin.getLogger().info("[DisasterRegistry] ═══════════════════════════════════════════════");
        
        // Verificación de integridad
        if (disasters.isEmpty()) {
            plugin.getLogger().severe("[DisasterRegistry] ¡ADVERTENCIA! Ningún desastre registrado - sistema no funcional");
        } else if (usarNuevos && disasters.size() != 3) {
            plugin.getLogger().warning("[DisasterRegistry] ¡ADVERTENCIA! Se esperaban 3 desastres (Ciclo 2) pero hay " + disasters.size());
        } else if (!usarNuevos && disasters.size() != 3) {
            plugin.getLogger().warning("[DisasterRegistry] ¡ADVERTENCIA! Se esperaban 3 desastres (Ciclo 1) pero hay " + disasters.size());
        }
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
