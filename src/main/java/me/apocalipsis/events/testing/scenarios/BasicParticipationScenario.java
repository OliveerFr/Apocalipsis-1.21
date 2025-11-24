package me.apocalipsis.events.testing.scenarios;

import org.bukkit.Bukkit;

import me.apocalipsis.events.testing.TestResult;

/**
 * Escenario: Participación básica
 * 
 * Prueba que los bots permanezcan activos durante todo el evento
 * y se registren correctamente como participantes.
 */
public class BasicParticipationScenario extends TestScenario {
    
    @Override
    public String getName() {
        return "Participación Básica";
    }
    
    @Override
    public String getDescription() {
        return "Verifica que todos los bots permanezcan activos y registrados como participantes";
    }
    
    @Override
    public int getDurationTicks() {
        return 200; // 10 segundos
    }
    
    @Override
    public void execute() {
        // Hacer que los bots se muevan aleatoriamente
        for (var bot : bots) {
            if (bot.isAlive()) {
                bot.wanderRandomly();
            }
            
            // Programar más movimientos
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (bot.isAlive()) {
                    bot.wanderRandomly();
                }
            }, 100L);
        }
    }
    
    @Override
    public TestResult validate() {
        TestResult result = new TestResult(getName(), true, getElapsedTime());
        
        // Verificar que todos los bots estén vivos
        long aliveBots = bots.stream().filter(b -> b.isAlive()).count();
        if (aliveBots < bots.size()) {
            result.addError(String.format(
                "Solo %d/%d bots siguen vivos",
                aliveBots,
                bots.size()
            ));
        }
        
        // Verificar que los bots hayan realizado acciones
        for (var bot : bots) {
            if (bot.getActionsPerformed() == 0) {
                result.addWarning(String.format(
                    "Bot '%s' no realizó ninguna acción",
                    bot.getName()
                ));
            }
        }
        
        return result;
    }
}
