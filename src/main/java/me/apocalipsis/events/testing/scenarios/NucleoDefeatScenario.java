package me.apocalipsis.events.testing.scenarios;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import me.apocalipsis.events.testing.TestResult;

/**
 * Escenario: Derrota del Núcleo (Eco de Sombras Acto 3)
 * 
 * Simula que los bots persigan y ataquen al núcleo
 * que se teletransporta.
 */
public class NucleoDefeatScenario extends TestScenario {
    
    private Location nucleoLocation;
    private double nucleoHealth = 500.0;
    private int teleportCount = 0;
    private int attacksLanded = 0;
    
    @Override
    public String getName() {
        return "Derrota del Núcleo";
    }
    
    @Override
    public String getDescription() {
        return "Verifica mecánica de perseguir y atacar entidad que se teletransporta";
    }
    
    @Override
    public int getDurationTicks() {
        return 600; // 30 segundos
    }
    
    @Override
    public void execute() {
        // Ubicación inicial del núcleo
        nucleoLocation = bots.get(0).getLocation().clone().add(20, 0, 20);
        
        // Hacer que los bots persigan al núcleo
        for (var bot : bots) {
            if (!bot.isAlive()) continue;
            
            // Perseguir y atacar
            for (int i = 0; i < 8; i++) {
                int delay = 50 + (i * 60);
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (bot.isAlive() && nucleoHealth > 0) {
                        // Moverse hacia núcleo
                        bot.moveTo(nucleoLocation);
                        
                        // Atacar si está cerca
                        if (bot.distanceTo(nucleoLocation) < 5.0) {
                            double damage = 30 + Math.random() * 40;
                            nucleoHealth -= damage;
                            attacksLanded++;
                            
                            plugin.getLogger().info(String.format(
                                "[Test] %s atacó al Núcleo (%.1f daño, vida: %.1f)",
                                bot.getName(),
                                damage,
                                nucleoHealth
                            ));
                        }
                    }
                }, delay);
            }
        }
        
        // Simular teletransportes del núcleo
        for (int tp = 0; tp < 4; tp++) {
            int delay = 150 + (tp * 120);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (nucleoHealth > 0) {
                    // Teletransportar a ubicación aleatoria
                    nucleoLocation = bots.get(0).getLocation().clone().add(
                        -30 + Math.random() * 60,
                        0,
                        -30 + Math.random() * 60
                    );
                    
                    teleportCount++;
                    plugin.getLogger().info(String.format(
                        "[Test] Núcleo se teletransportó (teleporte #%d)",
                        teleportCount
                    ));
                    
                    // Notificar a los bots para que persigan nueva ubicación
                    for (var bot : bots) {
                        if (bot.isAlive()) {
                            bot.moveTo(nucleoLocation);
                        }
                    }
                }
            }, delay);
        }
    }
    
    @Override
    public TestResult validate() {
        TestResult result = new TestResult(getName(), true, getElapsedTime());
        
        if (attacksLanded == 0) {
            result.addError("No se atacó al Núcleo");
            return new TestResult(getName(), false, getElapsedTime());
        }
        
        if (nucleoHealth > 0) {
            result.addWarning(String.format(
                "Núcleo sobrevivió con %.1f HP",
                nucleoHealth
            ));
        } else {
            plugin.getLogger().info("[Test] ¡Núcleo derrotado!");
        }
        
        plugin.getLogger().info(String.format(
            "[Test] Ataques: %d, Teleportes: %d",
            attacksLanded,
            teleportCount
        ));
        
        // Verificar que los bots persiguieron
        long botsActivos = bots.stream()
            .filter(b -> b.getActionsPerformed() >= 3)
            .count();
        
        if (botsActivos < 2) {
            result.addWarning("Pocos bots persiguieron activamente al Núcleo");
        }
        
        return result;
    }
}
