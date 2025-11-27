package me.apocalipsis.skills;

/**
 * Enum que define los tiers de habilidades.
 * Cada tier tiene un multiplicador de costo.
 */
public enum SkillTier {
    TIER_1(1, 1.0, "§a"),
    TIER_2(2, 2.5, "§e"),
    TIER_3(3, 5.0, "§6");
    
    private final int level;
    private final double costMultiplier;
    private final String color;
    
    SkillTier(int level, double costMultiplier, String color) {
        this.level = level;
        this.costMultiplier = costMultiplier;
        this.color = color;
    }
    
    public int getLevel() { return level; }
    public double getCostMultiplier() { return costMultiplier; }
    public String getColor() { return color; }
    
    public String getDisplayName() {
        return color + "Tier " + level;
    }
}
