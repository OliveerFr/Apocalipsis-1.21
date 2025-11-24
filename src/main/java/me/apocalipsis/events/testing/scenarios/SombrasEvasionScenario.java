package me.apocalipsis.events.testing.scenarios;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import me.apocalipsis.events.testing.TestResult;

/**
 * Escenario: Evasión de Sombras (Eco de Sombras)
 * 
 * Simula que las sombras persigan a los bots y estos
 * deben evadirlas.
 */
public class SombrasEvasionScenario extends TestScenario {
    
    private int sombraCount = 3;
    private Location[] sombraLocations;
    private int evasionsSuccessful = 0;
    
    @Override
    public String getName() {
        return "Evasión de Sombras";
    }
    
    @Override
    public String getDescription() {
        return "Verifica que los bots evadan correctamente las sombras enemigas";
    }
    
    @Override
    public int getDurationTicks() {
        return 400; // 20 segundos
    }
    
    @Override
    public void execute() {
        // Crear ubicaciones de sombras
        sombraLocations = new Location[sombraCount];
        
        for (int i = 0; i < sombraCount; i++) {
            sombraLocations[i] = bots.get(0).getLocation().clone().add(
                10 + Math.random() * 20,
                0,
                10 + Math.random() * 20
            );
        }
        
        // Hacer que las sombras "persigan" a los bots
        for (int tick = 0; tick < 20; tick++) {
            int delay = tick * 20;
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (var bot : bots) {
                    if (!bot.isAlive()) continue;
                    
                    // Verificar si hay sombra cerca
                    for (Location sombraLoc : sombraLocations) {
                        if (sombraLoc != null && bot.distanceTo(sombraLoc) < 10.0) {
                            // Sombra cerca! Evadir
                            bot.evadeFrom(sombraLoc);
                            evasionsSuccessful++;
                            
                            plugin.getLogger().info(String.format(
                                "[Test] %s evadiendo sombra",
                                bot.getName()
                            ));
                            
                            // Mover la sombra hacia el bot
                            Location botLoc = bot.getLocation();
                            if (botLoc != null) {
                                sombraLoc.add(
                                    (botLoc.getX() - sombraLoc.getX()) * 0.1,
                                    0,
                                    (botLoc.getZ() - sombraLoc.getZ()) * 0.1
                                );
                            }
                        }
                    }
                }
            }, delay);
        }
    }
    
    @Override
    public TestResult validate() {
        TestResult result = new TestResult(getName(), true, getElapsedTime());
        
        if (evasionsSuccessful == 0) {
            result.addWarning("No se registraron evasiones");
        }
        
        // Verificar que todos los bots sobrevivieron
        long aliveBots = bots.stream().filter(b -> b.isAlive()).count();
        if (aliveBots < bots.size()) {
            result.addError(String.format(
                "Algunos bots murieron durante evasión (%d/%d sobrevivieron)",
                aliveBots,
                bots.size()
            ));
        }
        
        plugin.getLogger().info(String.format(
            "[Test] Evasiones exitosas: %d",
            evasionsSuccessful
        ));
        
        return result;
    }
}
