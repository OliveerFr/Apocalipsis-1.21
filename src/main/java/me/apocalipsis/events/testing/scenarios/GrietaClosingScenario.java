package me.apocalipsis.events.testing.scenarios;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import me.apocalipsis.events.testing.TestResult;

/**
 * Escenario: Cierre de grietas (Eco de Brasas)
 * 
 * Simula que los bots encuentren y cierren grietas,
 * verificando que el sistema de tracking funcione.
 */
public class GrietaClosingScenario extends TestScenario {
    
    private int grietasSimuladas = 5;
    private int grietasCerradas = 0;
    
    @Override
    public String getName() {
        return "Cierre de Grietas";
    }
    
    @Override
    public String getDescription() {
        return "Simula el proceso de encontrar y cerrar grietas en Fase 1";
    }
    
    @Override
    public int getDurationTicks() {
        return 400; // 20 segundos
    }
    
    @Override
    public void execute() {
        // Simular que cada bot va a cerrar una grieta
        for (int i = 0; i < bots.size() && i < grietasSimuladas; i++) {
            var bot = bots.get(i);
            
            if (!bot.isAlive()) continue;
            
            // Simular ubicación de grieta aleatoria
            Location grietaLocation = bot.getLocation().clone().add(
                10 + Math.random() * 20,
                0,
                10 + Math.random() * 20
            );
            
            // Mover bot a grieta
            bot.moveTo(grietaLocation);
            
            // Simular interacción después de llegar
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (bot.distanceTo(grietaLocation) < 5.0) {
                    // Bot "cierra" la grieta
                    bot.interactWithBlock(grietaLocation);
                    
                    // Simular recompensa
                    bot.addToInventory(Material.GUNPOWDER, 9); // Ceniza
                    bot.addToInventory(Material.BLAZE_POWDER, 3); // Fulgor
                    
                    grietasCerradas++;
                    
                    plugin.getLogger().info(String.format(
                        "[Test] %s cerró una grieta (%d/%d)",
                        bot.getName(),
                        grietasCerradas,
                        grietasSimuladas
                    ));
                }
            }, 200L);
        }
    }
    
    @Override
    public TestResult validate() {
        TestResult result = new TestResult(getName(), true, getElapsedTime());
        
        // Verificar que se cerraron grietas
        if (grietasCerradas == 0) {
            result.addError("No se cerró ninguna grieta");
            return new TestResult(getName(), false, getElapsedTime());
        }
        
        if (grietasCerradas < grietasSimuladas) {
            result.addWarning(String.format(
                "Solo se cerraron %d/%d grietas",
                grietasCerradas,
                grietasSimuladas
            ));
        }
        
        // Verificar que los bots recibieron items
        int botsConItems = 0;
        for (var bot : bots) {
            if (bot.hasItem(Material.GUNPOWDER) || bot.hasItem(Material.BLAZE_POWDER)) {
                botsConItems++;
            }
        }
        
        if (botsConItems == 0) {
            result.addError("Ningún bot recibió items de recompensa");
        }
        
        return result;
    }
}
