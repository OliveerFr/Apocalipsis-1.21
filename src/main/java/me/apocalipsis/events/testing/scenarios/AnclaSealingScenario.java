package me.apocalipsis.events.testing.scenarios;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import me.apocalipsis.events.testing.TestResult;

/**
 * Escenario: Sellado de Anclas (Eco de Sombras Acto 4)
 * 
 * Simula el proceso de llevar fragmentos a anclas para sellarlas.
 */
public class AnclaSealingScenario extends TestScenario {
    
    private int numAnclas = 5;
    private boolean[] anclasSelladas;
    
    @Override
    public String getName() {
        return "Sellado de Anclas";
    }
    
    @Override
    public String getDescription() {
        return "Verifica proceso de sellar anclas con fragmentos";
    }
    
    @Override
    public int getDurationTicks() {
        return 600; // 30 segundos
    }
    
    @Override
    public void execute() {
        anclasSelladas = new boolean[numAnclas];
        
        // Dar fragmentos a los bots
        for (var bot : bots) {
            if (bot.isAlive()) {
                bot.addToInventory(Material.ECHO_SHARD, 10); // Fragmentos de sombra
            }
        }
        
        // Crear ubicaciones de anclas
        Location[] anclaLocations = new Location[numAnclas];
        for (int i = 0; i < numAnclas; i++) {
            anclaLocations[i] = bots.get(0).getLocation().clone().add(
                (i - 2) * 25,
                0,
                20
            );
        }
        
        // Asignar bots a anclas
        for (int i = 0; i < bots.size(); i++) {
            var bot = bots.get(i);
            if (!bot.isAlive()) continue;
            
            // Cada bot sella múltiples anclas
            for (int anclaIdx = i; anclaIdx < numAnclas; anclaIdx += bots.size()) {
                int finalAnclaIdx = anclaIdx;
                Location anclaLoc = anclaLocations[anclaIdx];
                
                int delay = 100 + (anclaIdx * 80);
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (bot.isAlive() && !anclasSelladas[finalAnclaIdx]) {
                        // Moverse a ancla
                        bot.moveTo(anclaLoc);
                        
                        // Sellar después de llegar
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (bot.distanceTo(anclaLoc) < 5.0 && bot.useItem(Material.ECHO_SHARD)) {
                                anclasSelladas[finalAnclaIdx] = true;
                                
                                plugin.getLogger().info(String.format(
                                    "[Test] %s selló Ancla %d",
                                    bot.getName(),
                                    finalAnclaIdx + 1
                                ));
                            }
                        }, 100L);
                    }
                }, delay);
            }
        }
    }
    
    @Override
    public TestResult validate() {
        TestResult result = new TestResult(getName(), true, getElapsedTime());
        
        // Contar anclas selladas
        int selladasCount = 0;
        for (boolean sellada : anclasSelladas) {
            if (sellada) selladasCount++;
        }
        
        if (selladasCount == 0) {
            result.addError("No se selló ninguna ancla");
            return new TestResult(getName(), false, getElapsedTime());
        }
        
        if (selladasCount < numAnclas) {
            result.addWarning(String.format(
                "Solo se sellaron %d/%d anclas",
                selladasCount,
                numAnclas
            ));
        } else {
            plugin.getLogger().info("[Test] ¡Todas las anclas selladas!");
        }
        
        return result;
    }
}
