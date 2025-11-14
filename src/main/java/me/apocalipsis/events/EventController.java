package me.apocalipsis.events;

import java.util.HashMap;
import java.util.Map;

import me.apocalipsis.Apocalipsis;

/**
 * Controlador para eventos narrativos únicos.
 * 
 * A diferencia de DisasterController:
 * - No tiene ciclo automático
 * - Solo puede haber un evento activo a la vez
 * - Eventos se activan manualmente por comando
 * - No usa sistema de weights/probabilidades
 */
public class EventController {
    
    private final Apocalipsis plugin;
    private final Map<String, EventBase> registeredEvents;
    private EventBase activeEvent;
    
    public EventController(Apocalipsis plugin) {
        this.plugin = plugin;
        this.registeredEvents = new HashMap<>();
        this.activeEvent = null;
    }
    
    /**
     * Registra un evento en el controlador
     */
    public void registerEvent(EventBase event) {
        registeredEvents.put(event.getEventId(), event);
        plugin.getLogger().info("[EventController] Registrado evento: " + event.getEventId());
    }
    
    /**
     * Inicia un evento por su ID
     * @return true si se inició correctamente, false si ya hay evento activo
     */
    public boolean startEvent(String eventId) {
        if (activeEvent != null) {
            plugin.getLogger().warning("[EventController] No se puede iniciar " + eventId + 
                ", ya hay evento activo: " + activeEvent.getEventId());
            return false;
        }
        
        EventBase event = registeredEvents.get(eventId);
        if (event == null) {
            plugin.getLogger().warning("[EventController] Evento no encontrado: " + eventId);
            return false;
        }
        
        activeEvent = event;
        event.start();
        
        plugin.getLogger().info("[EventController] Evento iniciado: " + eventId);
        return true;
    }
    
    /**
     * Detiene el evento activo
     */
    public void stopActiveEvent() {
        if (activeEvent == null) {
            return;
        }
        
        String eventId = activeEvent.getEventId();
        activeEvent.stop();
        activeEvent = null;
        
        plugin.getLogger().info("[EventController] Evento detenido: " + eventId);
    }
    
    /**
     * Tick del evento activo (llamar desde el main tick loop)
     */
    public void tick() {
        if (activeEvent != null) {
            activeEvent.tick();
        }
    }
    
    /**
     * Verifica si hay un evento activo
     */
    public boolean hasActiveEvent() {
        return activeEvent != null;
    }
    
    /**
     * Obtiene el evento activo
     */
    public EventBase getActiveEvent() {
        return activeEvent;
    }
    
    /**
     * Obtiene un evento registrado por ID
     */
    public EventBase getEvent(String eventId) {
        return registeredEvents.get(eventId);
    }
    
    /**
     * Verifica si existe un evento registrado
     */
    public boolean hasEvent(String eventId) {
        return registeredEvents.containsKey(eventId);
    }
}
