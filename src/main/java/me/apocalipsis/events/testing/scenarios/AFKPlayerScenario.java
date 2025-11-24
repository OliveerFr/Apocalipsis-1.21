package me.apocalipsis.events.testing.scenarios;

import me.apocalipsis.events.testing.EventTestBot.BotPersonality;
import me.apocalipsis.events.testing.TestResult;

/**
 * Escenario: Jugador AFK
 * 
 * Verifica que el sistema maneje correctamente jugadores
 * que están presentes pero inactivos.
 */
public class AFKPlayerScenario extends TestScenario {
    
    @Override
    public String getName() {
        return "Jugador AFK";
    }
    
    @Override
    public String getDescription() {
        return "Verifica manejo de jugadores inactivos/AFK";
    }
    
    @Override
    public int getDurationTicks() {
        return 400; // 20 segundos
    }
    
    @Override
    public void execute() {
        // Identificar o forzar un bot a estar AFK
        for (var bot : bots) {
            if (bot.getPersonality() == BotPersonality.AFK) {
                // Este bot ya es AFK, solo esperar
                bot.wait(400);
                plugin.getLogger().info(String.format(
                    "[Test] %s está AFK",
                    bot.getName()
                ));
                break;
            }
        }
        
        // Los demás bots actúan normalmente
        for (var bot : bots) {
            if (bot.getPersonality() != BotPersonality.AFK) {
                bot.wanderRandomly();
            }
        }
    }
    
    @Override
    public TestResult validate() {
        TestResult result = new TestResult(getName(), true, getElapsedTime());
        
        // Identificar bots AFK
        int afkBots = 0;
        for (var bot : bots) {
            if (bot.getPersonality() == BotPersonality.AFK) {
                afkBots++;
                
                // Verificar que casi no hizo acciones
                if (bot.getActionsPerformed() > 2) {
                    result.addWarning(String.format(
                        "Bot AFK '%s' realizó %d acciones (esperado: ~0)",
                        bot.getName(),
                        bot.getActionsPerformed()
                    ));
                }
            }
        }
        
        if (afkBots == 0) {
            result.addWarning("No hay bots AFK en el grupo de prueba");
        }
        
        return result;
    }
}
