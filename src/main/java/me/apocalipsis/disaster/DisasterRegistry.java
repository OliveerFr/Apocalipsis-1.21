package me.apocalipsis.disaster;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.disaster.adapters.PerformanceAdapter;
import me.apocalipsis.state.TimeService;
import me.apocalipsis.ui.MessageBus;
import me.apocalipsis.ui.SoundUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DisasterRegistry {

    private final Map<String, Disaster> disasters = new HashMap<>();

    public void registerDefaults(Apocalipsis plugin, MessageBus messageBus, SoundUtil soundUtil, 
                                TimeService timeService, PerformanceAdapter performanceAdapter) {
        register(new HuracanNew(plugin, messageBus, soundUtil, timeService, performanceAdapter));
        register(new LluviaFuegoNew(plugin, messageBus, soundUtil, timeService, performanceAdapter));
        register(new TerremotoNew(plugin, messageBus, soundUtil, timeService, performanceAdapter));
    }

    public void register(Disaster disaster) {
        disasters.put(disaster.getId(), disaster);
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
}
