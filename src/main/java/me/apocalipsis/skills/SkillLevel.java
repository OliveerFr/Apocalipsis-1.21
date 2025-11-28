package me.apocalipsis.skills;

/**
 * Enum que define los niveles de maestría de cada habilidad.
 * Nivel 1: Efecto base (100%)
 * Nivel 2: +50% efectividad
 * Nivel 3: +100% efectividad + efecto bonus
 */
public enum SkillLevel {
    LEVEL_1(1, 1.0, "§a", "I", 1.0),
    LEVEL_2(2, 1.5, "§e", "II", 2.0),
    LEVEL_3(3, 2.0, "§6", "III", 3.5);
    
    private final int level;
    private final double effectMultiplier;
    private final String color;
    private final String roman;
    private final double upgradeCostMultiplier;
    
    SkillLevel(int level, double effectMultiplier, String color, String roman, double upgradeCostMultiplier) {
        this.level = level;
        this.effectMultiplier = effectMultiplier;
        this.color = color;
        this.roman = roman;
        this.upgradeCostMultiplier = upgradeCostMultiplier;
    }
    
    public int getLevel() { return level; }
    public double getEffectMultiplier() { return effectMultiplier; }
    public String getColor() { return color; }
    public String getRoman() { return roman; }
    public double getUpgradeCostMultiplier() { return upgradeCostMultiplier; }
    
    /**
     * Obtiene el nombre formateado con número romano
     */
    public String getDisplaySuffix() {
        return color + " " + roman;
    }
    
    /**
     * Obtiene el siguiente nivel
     */
    public SkillLevel getNext() {
        return switch (this) {
            case LEVEL_1 -> LEVEL_2;
            case LEVEL_2 -> LEVEL_3;
            case LEVEL_3 -> null;
        };
    }
    
    /**
     * Obtiene el nivel anterior
     */
    public SkillLevel getPrevious() {
        return switch (this) {
            case LEVEL_1 -> null;
            case LEVEL_2 -> LEVEL_1;
            case LEVEL_3 -> LEVEL_2;
        };
    }
    
    /**
     * Verifica si es el nivel máximo
     */
    public boolean isMax() {
        return this == LEVEL_3;
    }
    
    /**
     * Obtiene nivel por número
     */
    public static SkillLevel fromNumber(int level) {
        return switch (level) {
            case 1 -> LEVEL_1;
            case 2 -> LEVEL_2;
            case 3 -> LEVEL_3;
            default -> LEVEL_1;
        };
    }
}
