package me.apocalipsis.events.testing.scenarios;

import java.util.List;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.events.testing.EventTestBot;
import me.apocalipsis.events.testing.TestResult;

/**
 * Clase base para escenarios de prueba.
 * Un escenario simula una situación específica del evento.
 */
public abstract class TestScenario {
    
    protected Apocalipsis plugin;
    protected List<EventTestBot> bots;
    protected long startTime;
    
    /**
     * Nombre del escenario
     */
    public abstract String getName();
    
    /**
     * Descripción de qué prueba este escenario
     */
    public abstract String getDescription();
    
    /**
     * Duración estimada en ticks
     */
    public abstract int getDurationTicks();
    
    /**
     * Setup inicial del escenario
     */
    public void setup(Apocalipsis plugin, List<EventTestBot> bots) {
        this.plugin = plugin;
        this.bots = bots;
        this.startTime = System.currentTimeMillis();
    }
    
    /**
     * Ejecuta el escenario (hace que los bots actúen)
     */
    public abstract void execute();
    
    /**
     * Valida si el escenario pasó o falló
     * @return resultado del test
     */
    public abstract TestResult validate();
    
    /**
     * Limpieza después del escenario
     */
    public void cleanup() {
        // Override si es necesario
    }
    
    /**
     * Utilidad: tiempo transcurrido desde inicio
     */
    protected long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
}
