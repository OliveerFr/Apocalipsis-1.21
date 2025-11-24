package me.apocalipsis.events.testing.scenarios;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import me.apocalipsis.events.testing.TestResult;

/**
 * Escenario: Participación parcial
 * 
 * Simula que algunos bots participen activamente mientras
 * otros solo participen mínimamente.
 */
public class PartialParticipationScenario extends TestScenario {
    
    @Override
    public String getName() {
        return "Participación Parcial";
    }
    
    @Override
    public String getDescription() {
        return "Verifica sistema de recompensas con participación variada";
    }
    
    @Override
    public int getDurationTicks() {
        return 500; // 25 segundos
    }
    
    @Override
    public void execute() {
        // Dividir bots en activos y pasivos
        int halfSize = bots.size() / 2;
        
        // Bots activos: participan mucho
        for (int i = 0; i < halfSize; i++) {
            var bot = bots.get(i);
            if (!bot.isAlive()) continue;
            
            // Dar items
            bot.addToInventory(Material.DIAMOND, 10);
            
            // Realizar muchas acciones
            for (int action = 0; action < 5; action++) {
                int delay = 50 + (action * 80);
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (bot.isAlive()) {
                        // Moverse
                        bot.wanderRandomly();
                        
                        // Usar item
                        bot.useItem(Material.DIAMOND);
                    }
                }, delay);
            }
            
            plugin.getLogger().info(String.format(
                "[Test] %s participando activamente",
                bot.getName()
            ));
        }
        
        // Bots pasivos: participan poco
        for (int i = halfSize; i < bots.size(); i++) {
            var bot = bots.get(i);
            if (!bot.isAlive()) continue;
            
            // Solo una acción
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (bot.isAlive()) {
                    bot.wanderRandomly();
                }
            }, 200L);
            
            plugin.getLogger().info(String.format(
                "[Test] %s participando pasivamente",
                bot.getName()
            ));
        }
    }
    
    @Override
    public TestResult validate() {
        TestResult result = new TestResult(getName(), true, getElapsedTime());
        
        // Analizar participación
        int highParticipation = 0;
        int lowParticipation = 0;
        
        for (var bot : bots) {
            int actions = bot.getActionsPerformed();
            
            if (actions >= 5) {
                highParticipation++;
            } else if (actions <= 2) {
                lowParticipation++;
            }
            
            plugin.getLogger().info(String.format(
                "[Test] %s: %d acciones",
                bot.getName(),
                actions
            ));
        }
        
        // Verificar que hay diversidad de participación
        if (highParticipation == 0) {
            result.addWarning("No hay bots con alta participación");
        }
        
        if (lowParticipation == 0) {
            result.addWarning("No hay bots con baja participación");
        }
        
        if (highParticipation > 0 && lowParticipation > 0) {
            plugin.getLogger().info(String.format(
                "[Test] Diversidad de participación: %d alta, %d baja",
                highParticipation,
                lowParticipation
            ));
        }
        
        return result;
    }
}
