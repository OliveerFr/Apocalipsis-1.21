package me.apocalipsis.skills;

/**
 * Enum que define las rarezas de habilidades.
 * Cada rareza tiene un multiplicador de costo.
 */
public enum SkillRarity {
    COMUN(1.0, "§f", "Común"),
    RARO(1.5, "§b", "Raro"),
    EPICO(2.5, "§d", "Épico"),
    LEGENDARIO(4.0, "§6", "Legendario");
    
    private final double costMultiplier;
    private final String color;
    private final String displayName;
    
    SkillRarity(double costMultiplier, String color, String displayName) {
        this.costMultiplier = costMultiplier;
        this.color = color;
        this.displayName = displayName;
    }
    
    public double getCostMultiplier() { return costMultiplier; }
    public String getColor() { return color; }
    public String getDisplayName() { return color + displayName; }
}
