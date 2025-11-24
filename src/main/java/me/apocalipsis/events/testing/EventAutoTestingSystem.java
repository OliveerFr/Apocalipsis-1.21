package me.apocalipsis.events.testing;

import java.util.*;
import java.util.stream.Collectors;

import org.bukkit.*;
import org.bukkit.scheduler.BukkitTask;

import me.apocalipsis.Apocalipsis;
import me.apocalipsis.events.testing.scenarios.*;

/**
 * Sistema de autotesting para eventos.
 * Genera bots simulados que ejecutan escenarios de prueba.
 * 
 * Funcionalidades:
 * - Crear 3-5 bots con diferentes comportamientos
 * - Ejecutar escenarios de prueba predefinidos
 * - Simular diferentes situaciones del evento
 * - Generar reportes de resultados
 * - Detección de bugs y edge cases
 */
public class EventAutoTestingSystem {
    
    private final Apocalipsis plugin;
    
    // Bots activos
    private final List<EventTestBot> activeBots;
    private BukkitTask botTickTask;
    
    // Escenario actual
    private TestScenario currentScenario;
    private boolean testingActive;
    
    // Resultados
    private final List<TestResult> testResults;
    
    // Configuración
    private int minBots = 3;
    private int maxBots = 5;
    private Location spawnLocation;
    
    public EventAutoTestingSystem(Apocalipsis plugin) {
        this.plugin = plugin;
        this.activeBots = new ArrayList<>();
        this.testResults = new ArrayList<>();
        this.testingActive = false;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // GESTIÓN DE TESTING
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Inicia el sistema de autotesting
     */
    public void startAutoTesting(String eventId, Location spawn) {
        if (testingActive) {
            plugin.getLogger().warning("[AutoTest] Testing ya está activo");
            return;
        }
        
        this.spawnLocation = spawn;
        this.testingActive = true;
        
        // Crear bots
        int numBots = minBots + (int)(Math.random() * (maxBots - minBots + 1));
        createBots(numBots);
        
        // Iniciar tick de bots
        startBotTicking();
        
        plugin.getLogger().info(String.format(
            "[AutoTest] Autotesting iniciado para evento '%s' con %d bots",
            eventId, numBots
        ));
        
        // Mostrar info de bots
        for (EventTestBot bot : activeBots) {
            plugin.getLogger().info(String.format(
                "  - %s [%s]",
                bot.getName(),
                bot.getPersonality().name()
            ));
        }
    }
    
    /**
     * Detiene el autotesting
     */
    public void stopAutoTesting() {
        if (!testingActive) {
            return;
        }
        
        // Detener tick
        if (botTickTask != null) {
            botTickTask.cancel();
            botTickTask = null;
        }
        
        // Limpiar bots
        activeBots.clear();
        
        testingActive = false;
        plugin.getLogger().info("[AutoTest] Autotesting detenido");
    }
    
    /**
     * Ejecuta un escenario de prueba específico
     */
    public void runScenario(TestScenario scenario) {
        if (!testingActive) {
            plugin.getLogger().warning("[AutoTest] No se puede ejecutar escenario: testing no activo");
            return;
        }
        
        currentScenario = scenario;
        scenario.setup(plugin, activeBots);
        
        plugin.getLogger().info(String.format(
            "[AutoTest] Ejecutando escenario: %s",
            scenario.getName()
        ));
        
        // Ejecutar escenario
        scenario.execute();
        
        // Programar validación al finalizar
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            TestResult result = scenario.validate();
            testResults.add(result);
            
            plugin.getLogger().info(String.format(
                "[AutoTest] Escenario '%s' completado: %s",
                scenario.getName(),
                result.isPassed() ? "§aPASS" : "§cFAIL"
            ));
            
            if (!result.isPassed()) {
                plugin.getLogger().warning("[AutoTest] Fallos detectados:");
                for (String error : result.getErrors()) {
                    plugin.getLogger().warning("  - " + error);
                }
            }
            
            currentScenario = null;
        }, scenario.getDurationTicks());
    }
    
    /**
     * Ejecuta una suite completa de escenarios
     */
    public void runTestSuite(String eventId) {
        List<TestScenario> scenarios = getTestScenariosForEvent(eventId);
        
        plugin.getLogger().info(String.format(
            "[AutoTest] Ejecutando suite de %d escenarios para '%s'",
            scenarios.size(),
            eventId
        ));
        
        runScenarioQueue(scenarios, 0);
    }
    
    /**
     * Ejecuta escenarios en secuencia
     */
    private void runScenarioQueue(List<TestScenario> scenarios, int index) {
        if (index >= scenarios.size()) {
            // Suite completada
            generateTestReport();
            return;
        }
        
        TestScenario scenario = scenarios.get(index);
        currentScenario = scenario;
        scenario.setup(plugin, activeBots);
        
        plugin.getLogger().info(String.format(
            "[AutoTest] [%d/%d] Ejecutando: %s",
            index + 1,
            scenarios.size(),
            scenario.getName()
        ));
        
        scenario.execute();
        
        // Programar siguiente escenario
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            TestResult result = scenario.validate();
            testResults.add(result);
            
            // Delay entre escenarios
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                runScenarioQueue(scenarios, index + 1);
            }, 100L); // 5 segundos entre escenarios
            
        }, scenario.getDurationTicks());
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // GESTIÓN DE BOTS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Crea bots con diferentes perfiles
     */
    private void createBots(int count) {
        String[] botNames = {
            "TestBot_Alpha",
            "TestBot_Beta",
            "TestBot_Gamma",
            "TestBot_Delta",
            "TestBot_Epsilon"
        };
        
        BotBehaviorProfile[] profiles = {
            BotBehaviorProfile.PRO_PLAYER(),
            BotBehaviorProfile.CASUAL_PLAYER(),
            BotBehaviorProfile.NEWBIE_PLAYER(),
            BotBehaviorProfile.CHAOTIC_PLAYER(),
            BotBehaviorProfile.AFK_PLAYER()
        };
        
        for (int i = 0; i < count && i < botNames.length; i++) {
            BotBehaviorProfile profile = profiles[i % profiles.length];
            EventTestBot bot = new EventTestBot(plugin, botNames[i], profile);
            bot.setLocation(spawnLocation);
            activeBots.add(bot);
        }
    }
    
    /**
     * Inicia el tick de todos los bots
     */
    private void startBotTicking() {
        botTickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (EventTestBot bot : activeBots) {
                bot.tick();
            }
        }, 1L, 1L); // Cada tick
    }
    
    /**
     * Obtiene bot por nombre
     */
    public EventTestBot getBot(String name) {
        return activeBots.stream()
            .filter(b -> b.getName().equals(name))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Obtiene todos los bots activos
     */
    public List<EventTestBot> getActiveBots() {
        return new ArrayList<>(activeBots);
    }
    
    /**
     * Obtiene bots vivos
     */
    public List<EventTestBot> getAliveBots() {
        return activeBots.stream()
            .filter(EventTestBot::isAlive)
            .collect(Collectors.toList());
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ESCENARIOS DE PRUEBA
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Obtiene lista de escenarios para un evento específico
     */
    private List<TestScenario> getTestScenariosForEvent(String eventId) {
        List<TestScenario> scenarios = new ArrayList<>();
        
        if (eventId.equals("eco_brasas")) {
            scenarios.add(new BasicParticipationScenario());
            scenarios.add(new GrietaClosingScenario());
            scenarios.add(new AnclaCompletionScenario());
            scenarios.add(new GuardianFightScenario());
            scenarios.add(new PlayerDeathScenario());
            scenarios.add(new AFKPlayerScenario());
            scenarios.add(new PartialParticipationScenario());
        } else if (eventId.equals("eco_sombras")) {
            scenarios.add(new BasicParticipationScenario());
            scenarios.add(new SombrasEvasionScenario());
            scenarios.add(new NucleoDefeatScenario());
            scenarios.add(new AnclaSealingScenario());
            scenarios.add(new OleadaSurvivalScenario());
            scenarios.add(new GuardianFightScenario());
            scenarios.add(new PlayerDeathScenario());
        } else if (eventId.equals("susurro_piedra_rota")) {
            // Evento 3: Usar escenarios genéricos que aplican
            scenarios.add(new BasicParticipationScenario());
            scenarios.add(new AFKPlayerScenario());
            scenarios.add(new PartialParticipationScenario());
        }
        
        return scenarios;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // REPORTES
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Genera reporte completo de testing
     */
    public String generateTestReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("§e§l═══════════════════════════════════════════════════\n");
        report.append("§e§l  REPORTE DE AUTOTESTING\n");
        report.append("§e§l═══════════════════════════════════════════════════\n\n");
        
        // Estadísticas generales
        int totalTests = testResults.size();
        int passed = (int) testResults.stream().filter(TestResult::isPassed).count();
        int failed = totalTests - passed;
        double successRate = totalTests > 0 ? (passed * 100.0 / totalTests) : 0;
        
        report.append(String.format("§7Tests ejecutados: §f%d\n", totalTests));
        report.append(String.format("§aPasados: §f%d\n", passed));
        report.append(String.format("§cFallidos: §f%d\n", failed));
        report.append(String.format("§7Tasa de éxito: §f%.1f%%\n\n", successRate));
        
        // Estadísticas de bots
        report.append("§e§lESTADÍSTICAS DE BOTS:\n");
        for (EventTestBot bot : activeBots) {
            report.append(bot.getStatsReport()).append("\n");
        }
        report.append("\n");
        
        // Resultados por escenario
        report.append("§e§lRESULTADOS POR ESCENARIO:\n");
        for (TestResult result : testResults) {
            String status = result.isPassed() ? "§a✓ PASS" : "§c✗ FAIL";
            report.append(String.format("  %s §7- §f%s\n", status, result.getScenarioName()));
            
            if (!result.isPassed()) {
                for (String error : result.getErrors()) {
                    report.append(String.format("    §c⚠ %s\n", error));
                }
            }
        }
        
        report.append("\n§e§l═══════════════════════════════════════════════════\n");
        
        String reportStr = report.toString();
        plugin.getLogger().info(reportStr);
        
        return reportStr;
    }
    
    /**
     * Genera reporte resumido
     */
    public String generateQuickReport() {
        int total = testResults.size();
        int passed = (int) testResults.stream().filter(TestResult::isPassed).count();
        
        return String.format(
            "§7[AutoTest] §f%d/%d tests pasados §7(%.1f%%)",
            passed,
            total,
            total > 0 ? (passed * 100.0 / total) : 0
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════
    
    public boolean isTestingActive() {
        return testingActive;
    }
    
    public TestScenario getCurrentScenario() {
        return currentScenario;
    }
    
    public List<TestResult> getTestResults() {
        return new ArrayList<>(testResults);
    }
    
    public void clearResults() {
        testResults.clear();
    }
}
