package me.apocalipsis.events.testing.scenarios;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import me.apocalipsis.events.testing.TestResult;

/**
 * Escenario: Pelea contra Guardián (Fase final)
 * 
 * Simula que los bots ataquen al guardián boss,
 * verificando mecánicas de combate y muerte.
 */
public class GuardianFightScenario extends TestScenario {
    
    private Location guardianLocation;
    private double guardianHealth = 1000.0;
    private int attacksLanded = 0;
    
    @Override
    public String getName() {
        return "Pelea contra Guardián";
    }
    
    @Override
    public String getDescription() {
        return "Simula combate cooperativo contra el boss final";
    }
    
    @Override
    public int getDurationTicks() {
        return 800; // 40 segundos
    }
    
    @Override
    public void execute() {
        // Ubicación del guardián
        guardianLocation = bots.get(0).getLocation().clone().add(30, 0, 30);
        
        // Todos los bots atacan al guardián
        for (var bot : bots) {
            if (!bot.isAlive()) continue;
            
            // Moverse al guardián
            bot.moveTo(guardianLocation);
            
            // Simular ataques periódicos
            for (int i = 0; i < 10; i++) {
                int delay = 50 + (i * 60);
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (bot.isAlive() && bot.distanceTo(guardianLocation) < 15.0) {
                        // Atacar
                        bot.attackEntity(guardianLocation);
                        
                        // Daño aleatorio
                        double damage = 50 + Math.random() * 50;
                        guardianHealth -= damage;
                        attacksLanded++;
                        
                        plugin.getLogger().info(String.format(
                            "[Test] %s atacó al Guardián (%.1f daño, vida restante: %.1f)",
                            bot.getName(),
                            damage,
                            guardianHealth
                        ));
                        
                        // Simular mecánica de ataque del guardián
                        if (Math.random() < 0.15) { // 15% de golpear al bot
                            bot.die();
                            plugin.getLogger().info(String.format(
                                "[Test] %s fue derrotado por el Guardián",
                                bot.getName()
                            ));
                        }
                    }
                }, delay);
            }
        }
        
        // Simular muerte del guardián si vida llega a 0
        Bukkit.getScheduler().runTaskTimer(plugin, (task) -> {
            if (guardianHealth <= 0) {
                plugin.getLogger().info("[Test] ¡Guardián derrotado!");
                task.cancel();
            }
        }, 0L, 20L);
    }
    
    @Override
    public TestResult validate() {
        TestResult result = new TestResult(getName(), true, getElapsedTime());
        
        // Verificar que se atacó al guardián
        if (attacksLanded == 0) {
            result.addError("No se registró ningún ataque al Guardián");
            return new TestResult(getName(), false, getElapsedTime());
        }
        
        plugin.getLogger().info(String.format(
            "[Test] Total de ataques: %d",
            attacksLanded
        ));
        
        // Verificar resultado de la pelea
        if (guardianHealth > 0) {
            result.addWarning(String.format(
                "Guardián sobrevivió con %.1f HP",
                guardianHealth
            ));
        }
        
        // Verificar muertes de jugadores
        long deadBots = bots.stream().filter(b -> !b.isAlive()).count();
        if (deadBots == bots.size()) {
            result.addError("Todos los bots murieron - balance de dificultad");
        } else if (deadBots > 0) {
            result.addWarning(String.format(
                "%d bots murieron durante la pelea",
                deadBots
            ));
        }
        
        // Verificar cooperación
        long botsQueAtacaron = bots.stream()
            .filter(b -> b.getActionsPerformed() >= 5)
            .count();
        
        if (botsQueAtacaron < 2) {
            result.addWarning("Poca participación en combate cooperativo");
        }
        
        return result;
    }
}
