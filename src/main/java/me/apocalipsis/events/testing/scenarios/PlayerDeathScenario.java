package me.apocalipsis.events.testing.scenarios;

import org.bukkit.Bukkit;

import me.apocalipsis.events.testing.TestResult;

/**
 * Escenario: Muerte de jugadores
 * 
 * Simula que algunos bots mueran durante el evento
 * para verificar que el sistema maneje correctamente las muertes.
 */
public class PlayerDeathScenario extends TestScenario {
    
    private int botsToKill = 2;
    
    @Override
    public String getName() {
        return "Muerte de Jugadores";
    }
    
    @Override
    public String getDescription() {
        return "Verifica manejo de muertes y respawns durante el evento";
    }
    
    @Override
    public int getDurationTicks() {
        return 300; // 15 segundos
    }
    
    @Override
    public void execute() {
        // Matar algunos bots
        for (int i = 0; i < botsToKill && i < bots.size(); i++) {
            var bot = bots.get(i);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                bot.die();
                plugin.getLogger().info(String.format(
                    "[Test] %s ha muerto",
                    bot.getName()
                ));
            }, 50L + (i * 50L));
        }
        
        // Intentar respawn después
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (var bot : bots) {
                if (!bot.isAlive()) {
                    bot.respawn();
                    plugin.getLogger().info(String.format(
                        "[Test] %s ha respawneado",
                        bot.getName()
                    ));
                }
            }
        }, 200L);
    }
    
    @Override
    public TestResult validate() {
        TestResult result = new TestResult(getName(), true, getElapsedTime());
        
        // Verificar que los bots murieron
        int totalDeaths = 0;
        for (var bot : bots) {
            totalDeaths += bot.getDeaths();
        }
        
        if (totalDeaths == 0) {
            result.addWarning("No se registraron muertes");
        }
        
        // Verificar respawns
        long aliveBots = bots.stream().filter(b -> b.isAlive()).count();
        if (aliveBots < bots.size() - 1) {
            result.addWarning("Algunos bots no respawnearon correctamente");
        }
        
        plugin.getLogger().info(String.format(
            "[Test] Muertes totales: %d, Bots vivos: %d/%d",
            totalDeaths,
            aliveBots,
            bots.size()
        ));
        
        return result;
    }
}
