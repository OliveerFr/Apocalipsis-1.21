package me.apocalipsis.events.testing.scenarios;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import me.apocalipsis.events.testing.TestResult;

/**
 * Escenario: Supervivencia de Oleadas (Eco de Sombras Acto 5)
 * 
 * Simula oleadas de enemigos atacando a los bots.
 */
public class OleadaSurvivalScenario extends TestScenario {
    
    private int numOleadas = 3;
    private int[] enemigosKilledPerWave;
    
    @Override
    public String getName() {
        return "Supervivencia de Oleadas";
    }
    
    @Override
    public String getDescription() {
        return "Verifica supervivencia y combate durante oleadas de enemigos";
    }
    
    @Override
    public int getDurationTicks() {
        return 800; // 40 segundos
    }
    
    @Override
    public void execute() {
        enemigosKilledPerWave = new int[numOleadas];
        Location arenaCenter = bots.get(0).getLocation().clone().add(30, 0, 30);
        
        // Mover todos los bots al centro del arena
        for (var bot : bots) {
            if (bot.isAlive()) {
                bot.moveTo(arenaCenter);
            }
        }
        
        // Simular oleadas
        for (int oleada = 0; oleada < numOleadas; oleada++) {
            int waveDelay = 200 + (oleada * 200);
            int finalOleada = oleada;
            int enemigosThisWave = 5 + (oleada * 2); // Aumenta dificultad
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getLogger().info(String.format(
                    "[Test] ¡Oleada %d iniciada! (%d enemigos)",
                    finalOleada + 1,
                    enemigosThisWave
                ));
                
                // Simular combate
                for (int enemy = 0; enemy < enemigosThisWave; enemy++) {
                    int enemyDelay = enemy * 20;
                    
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        // Ubicación de enemigo cerca del arena
                        Location enemyLoc = arenaCenter.clone().add(
                            -10 + Math.random() * 20,
                            0,
                            -10 + Math.random() * 20
                        );
                        
                        // Bot más cercano ataca
                        var nearestBot = bots.stream()
                            .filter(b -> b.isAlive())
                            .min((b1, b2) -> Double.compare(
                                b1.distanceTo(enemyLoc),
                                b2.distanceTo(enemyLoc)
                            ))
                            .orElse(null);
                        
                        if (nearestBot != null) {
                            nearestBot.attackEntity(enemyLoc);
                            enemigosKilledPerWave[finalOleada]++;
                        }
                        
                        // Simular daño a bots (algunos pueden morir)
                        if (Math.random() < 0.1 * (finalOleada + 1)) { // Aumenta dificultad
                            var randomBot = bots.get((int)(Math.random() * bots.size()));
                            if (randomBot.isAlive()) {
                                randomBot.die();
                                plugin.getLogger().info(String.format(
                                    "[Test] %s fue derrotado en Oleada %d",
                                    randomBot.getName(),
                                    finalOleada + 1
                                ));
                            }
                        }
                    }, enemyDelay);
                }
            }, waveDelay);
        }
    }
    
    @Override
    public TestResult validate() {
        TestResult result = new TestResult(getName(), true, getElapsedTime());
        
        // Verificar kills por oleada
        int totalKills = 0;
        for (int i = 0; i < numOleadas; i++) {
            totalKills += enemigosKilledPerWave[i];
            plugin.getLogger().info(String.format(
                "[Test] Oleada %d: %d enemigos eliminados",
                i + 1,
                enemigosKilledPerWave[i]
            ));
        }
        
        if (totalKills == 0) {
            result.addError("No se eliminaron enemigos");
            return new TestResult(getName(), false, getElapsedTime());
        }
        
        // Verificar supervivencia
        long aliveBots = bots.stream().filter(b -> b.isAlive()).count();
        if (aliveBots == 0) {
            result.addError("Todos los bots murieron - balance incorrecto");
        } else if (aliveBots < bots.size() / 2) {
            result.addWarning(String.format(
                "Muchos bots murieron (%d/%d sobrevivieron)",
                aliveBots,
                bots.size()
            ));
        }
        
        plugin.getLogger().info(String.format(
            "[Test] Total kills: %d, Supervivientes: %d/%d",
            totalKills,
            aliveBots,
            bots.size()
        ));
        
        return result;
    }
}
