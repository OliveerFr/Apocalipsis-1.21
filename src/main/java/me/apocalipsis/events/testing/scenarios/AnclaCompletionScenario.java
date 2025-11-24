package me.apocalipsis.events.testing.scenarios;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import me.apocalipsis.events.testing.TestResult;

/**
 * Escenario: Completar anclas (Eco de Brasas Fase 2)
 * 
 * Simula que los bots lleven items a las anclas y las completen
 * de forma cooperativa.
 */
public class AnclaCompletionScenario extends TestScenario {
    
    private int numAnclas = 3;
    private int[] anclaProgress = new int[3]; // Progreso de cada ancla
    
    @Override
    public String getName() {
        return "Completar Anclas";
    }
    
    @Override
    public String getDescription() {
        return "Simula la entrega cooperativa de items a las anclas";
    }
    
    @Override
    public int getDurationTicks() {
        return 600; // 30 segundos
    }
    
    @Override
    public void execute() {
        // Dar items a los bots
        for (var bot : bots) {
            if (bot.isAlive()) {
                bot.addToInventory(Material.GUNPOWDER, 20);  // Ceniza
                bot.addToInventory(Material.BLAZE_POWDER, 10); // Fulgor
                bot.addToInventory(Material.MAGMA_CREAM, 5);   // Núcleo
            }
        }
        
        // Simular ubicaciones de anclas
        Location[] anclaLocations = new Location[numAnclas];
        for (int i = 0; i < numAnclas; i++) {
            anclaLocations[i] = bots.get(0).getLocation().clone().add(
                20 * i - 20,
                0,
                20
            );
        }
        
        // Asignar bots a anclas
        for (int i = 0; i < bots.size(); i++) {
            var bot = bots.get(i);
            int anclaIndex = i % numAnclas;
            Location anclaLoc = anclaLocations[anclaIndex];
            
            if (!bot.isAlive()) continue;
            
            // Mover a ancla
            bot.moveTo(anclaLoc);
            
            // Simular entregas múltiples
            for (int delivery = 0; delivery < 3; delivery++) {
                int delay = 100 + (delivery * 150);
                int finalAnclaIndex = anclaIndex;
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (bot.distanceTo(anclaLoc) < 5.0) {
                        // Entregar items
                        boolean entregado = false;
                        
                        if (bot.useItem(Material.GUNPOWDER)) {
                            entregado = true;
                        } else if (bot.useItem(Material.BLAZE_POWDER)) {
                            entregado = true;
                        } else if (bot.useItem(Material.MAGMA_CREAM)) {
                            entregado = true;
                        }
                        
                        if (entregado) {
                            anclaProgress[finalAnclaIndex]++;
                            
                            plugin.getLogger().info(String.format(
                                "[Test] %s entregó item a Ancla %d (progreso: %d)",
                                bot.getName(),
                                finalAnclaIndex + 1,
                                anclaProgress[finalAnclaIndex]
                            ));
                        }
                    }
                }, delay);
            }
        }
    }
    
    @Override
    public TestResult validate() {
        TestResult result = new TestResult(getName(), true, getElapsedTime());
        
        // Verificar progreso de anclas
        int anclasCompletadas = 0;
        for (int i = 0; i < numAnclas; i++) {
            if (anclaProgress[i] >= 5) { // Threshold arbitrario
                anclasCompletadas++;
            }
            
            plugin.getLogger().info(String.format(
                "[Test] Ancla %d: %d entregas",
                i + 1,
                anclaProgress[i]
            ));
        }
        
        if (anclasCompletadas == 0) {
            result.addError("No se completó ninguna ancla");
            return new TestResult(getName(), false, getElapsedTime());
        }
        
        if (anclasCompletadas < numAnclas) {
            result.addWarning(String.format(
                "Solo se completaron %d/%d anclas",
                anclasCompletadas,
                numAnclas
            ));
        }
        
        // Verificar cooperación
        int botsQueEntregaron = 0;
        for (var bot : bots) {
            if (bot.getActionsPerformed() > 2) { // Al menos 3 acciones
                botsQueEntregaron++;
            }
        }
        
        if (botsQueEntregaron < 2) {
            result.addWarning("Poca cooperación entre bots");
        }
        
        return result;
    }
}
