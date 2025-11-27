package me.apocalipsis.skills;

import org.bukkit.Material;

/**
 * Enum que define las ramas del árbol de habilidades.
 */
public enum SkillBranch {
    ALMACENAMIENTO("almacenamiento", "§6Almacenamiento", Material.CHEST, "📦"),
    UTILIDAD("utilidad", "§eUtilidad", Material.COMPASS, "⚡"),
    SUPERVIVENCIA("supervivencia", "§cSupervivencia", Material.SHIELD, "🛡");
    
    private final String id;
    private final String displayName;
    private final Material icon;
    private final String emoji;
    
    SkillBranch(String id, String displayName, Material icon, String emoji) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.emoji = emoji;
    }
    
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }
    public String getEmoji() { return emoji; }
    
    public static SkillBranch fromId(String id) {
        for (SkillBranch branch : values()) {
            if (branch.id.equalsIgnoreCase(id)) {
                return branch;
            }
        }
        return null;
    }
}
