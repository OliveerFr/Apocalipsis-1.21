package me.apocalipsis.skills;

import org.bukkit.Material;

/**
 * Enum que define las ramas del árbol de habilidades.
 * 6 ramas + 1 especial de sinergias
 */
public enum SkillBranch {
    ALMACENAMIENTO("almacenamiento", "§6Almacenamiento", Material.CHEST, "📦", 
        "§7Aumenta capacidad de inventario", "§7y protección de items."),
    UTILIDAD("utilidad", "§eUtilidad", Material.COMPASS, "⚡",
        "§7Mejoras de velocidad, minería", "§7y eficiencia general."),
    SUPERVIVENCIA("supervivencia", "§cSupervivencia", Material.SHIELD, "🛡",
        "§7Vida extra, resistencias", "§7y habilidades defensivas."),
    COMBATE("combate", "§4Combate", Material.NETHERITE_SWORD, "⚔",
        "§7Daño aumentado, críticos", "§7y habilidades ofensivas."),
    EXPLORACION("exploracion", "§aExploración", Material.SPYGLASS, "🧭",
        "§7Visión nocturna, navegación", "§7y descubrimiento."),
    INVOCACION("invocacion", "§5Invocación", Material.BONE, "🐺",
        "§7Mascotas y familiares", "§7que te asisten."),
    SINERGIAS("sinergias", "§d✦ Sinergias", Material.NETHER_STAR, "✦",
        "§7Habilidades que combinan", "§7múltiples ramas.");
    
    private final String id;
    private final String displayName;
    private final Material icon;
    private final String emoji;
    private final String descLine1;
    private final String descLine2;
    
    SkillBranch(String id, String displayName, Material icon, String emoji,
                String descLine1, String descLine2) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.emoji = emoji;
        this.descLine1 = descLine1;
        this.descLine2 = descLine2;
    }
    
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }
    public String getEmoji() { return emoji; }
    public String getDescLine1() { return descLine1; }
    public String getDescLine2() { return descLine2; }
    
    /**
     * Verifica si es una rama principal (no sinergias)
     */
    public boolean isPrimaryBranch() {
        return this != SINERGIAS;
    }
    
    public static SkillBranch fromId(String id) {
        for (SkillBranch branch : values()) {
            if (branch.id.equalsIgnoreCase(id)) {
                return branch;
            }
        }
        return null;
    }
}
