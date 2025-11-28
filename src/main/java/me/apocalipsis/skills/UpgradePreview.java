package me.apocalipsis.skills;

/**
 * Clase que contiene la información de preview para mejorar una skill.
 * Muestra el estado actual, el siguiente nivel, costo y si el jugador puede comprar.
 */
public class UpgradePreview {
    
    private final SkillLevel currentLevel;
    private final SkillLevel nextLevel;
    private final int cost;
    private final int playerXP;
    private final double currentEffect;
    private final double nextEffect;
    private final String level3Bonus;
    
    public UpgradePreview(SkillLevel currentLevel, SkillLevel nextLevel, 
                          int cost, int playerXP,
                          double currentEffect, double nextEffect, 
                          String level3Bonus) {
        this.currentLevel = currentLevel;
        this.nextLevel = nextLevel;
        this.cost = cost;
        this.playerXP = playerXP;
        this.currentEffect = currentEffect;
        this.nextEffect = nextEffect;
        this.level3Bonus = level3Bonus;
    }
    
    // ==================== GETTERS ====================
    
    public SkillLevel getCurrentLevel() {
        return currentLevel;
    }
    
    public SkillLevel getNextLevel() {
        return nextLevel;
    }
    
    public int getCost() {
        return cost;
    }
    
    public int getPlayerXP() {
        return playerXP;
    }
    
    public double getCurrentEffect() {
        return currentEffect;
    }
    
    public double getNextEffect() {
        return nextEffect;
    }
    
    public String getLevel3Bonus() {
        return level3Bonus;
    }
    
    // ==================== MÉTODOS ÚTILES ====================
    
    /**
     * @return true si el jugador tiene suficiente XP para la mejora
     */
    public boolean canAfford() {
        return playerXP >= cost;
    }
    
    /**
     * @return XP que falta para la mejora (0 si puede comprar)
     */
    public int getXPNeeded() {
        return Math.max(0, cost - playerXP);
    }
    
    /**
     * @return la mejora porcentual entre niveles
     */
    public double getEffectIncrease() {
        if (currentEffect == 0) return nextEffect;
        return nextEffect - currentEffect;
    }
    
    /**
     * @return true si el siguiente nivel es el nivel 3 (con bonus especial)
     */
    public boolean isNextLevelMax() {
        return nextLevel.isMax();
    }
    
    /**
     * @return true si hay un bonus especial de nivel 3 disponible
     */
    public boolean hasLevel3Bonus() {
        return level3Bonus != null && !level3Bonus.isEmpty();
    }
    
    /**
     * Genera las líneas de lore para mostrar en el GUI
     */
    public java.util.List<String> generateLore() {
        java.util.List<String> lore = new java.util.ArrayList<>();
        
        lore.add("§8§m─────────────────────");
        lore.add("§7Nivel actual: " + currentLevel.getColor() + currentLevel.getRoman());
        lore.add("§7Siguiente nivel: " + nextLevel.getColor() + nextLevel.getRoman());
        lore.add("");
        lore.add("§6▸ Efecto actual: §f" + formatEffect(currentEffect));
        lore.add("§a▸ Nuevo efecto: §f" + formatEffect(nextEffect) + 
                " §7(+" + formatEffect(getEffectIncrease()) + ")");
        
        if (isNextLevelMax() && hasLevel3Bonus()) {
            lore.add("");
            lore.add("§5§l✦ BONUS NIVEL 3:");
            lore.add("§d  " + level3Bonus);
        }
        
        lore.add("");
        lore.add("§8§m─────────────────────");
        lore.add("§6Costo: §e" + cost + " XP");
        lore.add("§7Tu XP: §f" + playerXP);
        
        if (canAfford()) {
            lore.add("");
            lore.add("§a§l▶ Click para mejorar");
        } else {
            lore.add("");
            lore.add("§c✗ Necesitas " + getXPNeeded() + " XP más");
        }
        
        return lore;
    }
    
    private String formatEffect(double value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        }
        return String.format("%.1f", value);
    }
}
