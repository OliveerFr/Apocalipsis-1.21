package me.apocalipsis.events.testing;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de un test ejecutado.
 * Contiene información sobre si pasó o falló, y detalles de errores.
 */
public class TestResult {
    
    private final String scenarioName;
    private final boolean passed;
    private final List<String> errors;
    private final List<String> warnings;
    private final long executionTimeMs;
    
    public TestResult(String scenarioName, boolean passed, long executionTimeMs) {
        this.scenarioName = scenarioName;
        this.passed = passed;
        this.executionTimeMs = executionTimeMs;
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }
    
    public void addError(String error) {
        errors.add(error);
    }
    
    public void addWarning(String warning) {
        warnings.add(warning);
    }
    
    public String getScenarioName() {
        return scenarioName;
    }
    
    public boolean isPassed() {
        return passed;
    }
    
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
    
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    @Override
    public String toString() {
        return String.format(
            "TestResult[%s: %s, errors=%d, warnings=%d, time=%dms]",
            scenarioName,
            passed ? "PASS" : "FAIL",
            errors.size(),
            warnings.size(),
            executionTimeMs
        );
    }
}
