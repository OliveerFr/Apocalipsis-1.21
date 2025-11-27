package me.apocalipsis.skills;

import org.bukkit.Material;
import java.util.List;
import java.util.ArrayList;

/**
 * Enum que define todas las habilidades disponibles en el árbol.
 */
public enum Skill {
    // ================= ALMACENAMIENTO =================
    // Tier 1
    BOLSILLOS_PROFUNDOS("bolsillos_profundos", "Bolsillos Profundos", 
        "+9 slots de inventario (4ª fila)", 
        SkillBranch.ALMACENAMIENTO, SkillTier.TIER_1, SkillRarity.COMUN, 
        500, Material.CHEST, false, new String[]{}),
    
    COFRE_INTERIOR("cofre_interior", "Cofre Interior", 
        "Ender Chest +9 slots", 
        SkillBranch.ALMACENAMIENTO, SkillTier.TIER_1, SkillRarity.COMUN, 
        400, Material.ENDER_CHEST, false, new String[]{}),
    
    // Tier 2
    BOLSILLOS_SIN_FONDO("bolsillos_sin_fondo", "Bolsillos Sin Fondo", 
        "+18 slots de inventario (5ª fila)", 
        SkillBranch.ALMACENAMIENTO, SkillTier.TIER_2, SkillRarity.RARO, 
        1500, Material.CHEST, false, new String[]{"bolsillos_profundos"}),
    
    COFRE_DIMENSIONAL("cofre_dimensional", "Cofre Dimensional", 
        "Ender Chest +18 slots (total 45)", 
        SkillBranch.ALMACENAMIENTO, SkillTier.TIER_2, SkillRarity.RARO, 
        1200, Material.ENDER_CHEST, false, new String[]{"cofre_interior"}),
    
    AUTO_RECOLECCION("auto_recoleccion", "Auto-Recolección", 
        "Items van directo al inventario (radio 3 bloques)", 
        SkillBranch.ALMACENAMIENTO, SkillTier.TIER_2, SkillRarity.EPICO, 
        2000, Material.HOPPER, false, new String[]{"bolsillos_profundos"}),
    
    // Tier 3
    INVENTARIO_INFINITO("inventario_infinito", "Inventario Infinito", 
        "+27 slots (6ª fila, máximo)", 
        SkillBranch.ALMACENAMIENTO, SkillTier.TIER_3, SkillRarity.LEGENDARIO, 
        4000, Material.CHEST, false, new String[]{"bolsillos_sin_fondo"}),
    
    VOID_STORAGE("void_storage", "Void Storage", 
        "Ender Chest de 54 slots (cofre doble)", 
        SkillBranch.ALMACENAMIENTO, SkillTier.TIER_3, SkillRarity.LEGENDARIO, 
        3500, Material.ENDER_EYE, false, new String[]{"cofre_dimensional"}),
    
    // ================= UTILIDAD =================
    // Tier 1
    PASO_LIGERO("paso_ligero", "Paso Ligero", 
        "+10% velocidad de movimiento permanente", 
        SkillBranch.UTILIDAD, SkillTier.TIER_1, SkillRarity.COMUN, 
        400, Material.LEATHER_BOOTS, true, new String[]{}),
    
    MINERO_EFICIENTE("minero_eficiente", "Minero Eficiente", 
        "+15% velocidad de minado", 
        SkillBranch.UTILIDAD, SkillTier.TIER_1, SkillRarity.COMUN, 
        450, Material.IRON_PICKAXE, false, new String[]{}),
    
    ESTOMAGO_HIERRO("estomago_hierro", "Estómago de Hierro", 
        "Hambre baja 20% más lento", 
        SkillBranch.UTILIDAD, SkillTier.TIER_1, SkillRarity.COMUN, 
        350, Material.COOKED_BEEF, false, new String[]{}),
    
    // Tier 2
    ZANCADAS("zancadas", "Zancadas", 
        "+20% velocidad + salto mejorado", 
        SkillBranch.UTILIDAD, SkillTier.TIER_2, SkillRarity.RARO, 
        1400, Material.RABBIT_FOOT, true, new String[]{"paso_ligero"}),
    
    TOQUE_FORTUNA("toque_fortuna", "Toque de Fortuna", 
        "+10% drop de minerales", 
        SkillBranch.UTILIDAD, SkillTier.TIER_2, SkillRarity.RARO, 
        1600, Material.DIAMOND, false, new String[]{"minero_eficiente"}),
    
    METABOLISMO_LENTO("metabolismo_lento", "Metabolismo Lento", 
        "Hambre baja 40% más lento", 
        SkillBranch.UTILIDAD, SkillTier.TIER_2, SkillRarity.RARO, 
        1000, Material.GOLDEN_CARROT, false, new String[]{"estomago_hierro"}),
    
    CRAFTEO_RAPIDO("crafteo_rapido", "Crafteo Rápido", 
        "Shift+click craftea stacks completos", 
        SkillBranch.UTILIDAD, SkillTier.TIER_2, SkillRarity.COMUN, 
        1200, Material.CRAFTING_TABLE, false, new String[]{}),
    
    // Tier 3
    VELOCISTA("velocista", "Velocista", 
        "+30% velocidad + sin penalización sneaking", 
        SkillBranch.UTILIDAD, SkillTier.TIER_3, SkillRarity.EPICO, 
        3500, Material.FEATHER, true, new String[]{"zancadas"}),
    
    SEDA_NATURAL("seda_natural", "Toque de Seda Natural", 
        "5% chance de silk touch sin encantamiento", 
        SkillBranch.UTILIDAD, SkillTier.TIER_3, SkillRarity.LEGENDARIO, 
        4500, Material.COBWEB, false, new String[]{"toque_fortuna"}),
    
    AUTOSUFICIENTE("autosuficiente", "Autosuficiente", 
        "Regenera 0.5 hambre cada 30s", 
        SkillBranch.UTILIDAD, SkillTier.TIER_3, SkillRarity.EPICO, 
        3000, Material.CAKE, false, new String[]{"metabolismo_lento"}),
    
    MESA_PORTATIL("mesa_portatil", "Mesa Portátil", 
        "/craft - Abre mesa de crafteo en cualquier lugar", 
        SkillBranch.UTILIDAD, SkillTier.TIER_3, SkillRarity.RARO, 
        2500, Material.CRAFTING_TABLE, false, new String[]{"crafteo_rapido"}),
    
    // ================= SUPERVIVENCIA =================
    // Tier 1
    PIEL_GRUESA("piel_gruesa", "Piel Gruesa", 
        "+2 corazones permanentes", 
        SkillBranch.SUPERVIVENCIA, SkillTier.TIER_1, SkillRarity.RARO, 
        600, Material.IRON_CHESTPLATE, false, new String[]{}),
    
    CAIDA_SUAVE("caida_suave", "Caída Suave", 
        "-25% daño por caída", 
        SkillBranch.SUPERVIVENCIA, SkillTier.TIER_1, SkillRarity.COMUN, 
        400, Material.FEATHER, false, new String[]{}),
    
    RESISTENCIA_FUEGO("resistencia_fuego", "Resistencia al Fuego", 
        "-20% daño por fuego/lava", 
        SkillBranch.SUPERVIVENCIA, SkillTier.TIER_1, SkillRarity.COMUN, 
        500, Material.BLAZE_POWDER, false, new String[]{}),
    
    NADADOR("nadador", "Nadador", 
        "+30% velocidad nadando", 
        SkillBranch.SUPERVIVENCIA, SkillTier.TIER_1, SkillRarity.COMUN, 
        350, Material.COD, true, new String[]{}),
    
    // Tier 2
    TANQUE("tanque", "Tanque", 
        "+4 corazones permanentes (total +6)", 
        SkillBranch.SUPERVIVENCIA, SkillTier.TIER_2, SkillRarity.EPICO, 
        2000, Material.DIAMOND_CHESTPLATE, false, new String[]{"piel_gruesa"}),
    
    PLUMA("pluma", "Pluma", 
        "-50% daño por caída", 
        SkillBranch.SUPERVIVENCIA, SkillTier.TIER_2, SkillRarity.RARO, 
        1200, Material.FEATHER, false, new String[]{"caida_suave"}),
    
    IGNIFUGO("ignifugo", "Ignífugo", 
        "-40% daño fuego + inmune a pisar fuego", 
        SkillBranch.SUPERVIVENCIA, SkillTier.TIER_2, SkillRarity.RARO, 
        1500, Material.MAGMA_CREAM, false, new String[]{"resistencia_fuego"}),
    
    BRANQUIAS("branquias", "Branquias", 
        "+60% respiración bajo agua", 
        SkillBranch.SUPERVIVENCIA, SkillTier.TIER_2, SkillRarity.RARO, 
        1000, Material.PUFFERFISH, false, new String[]{"nadador"}),
    
    REGENERACION_PASIVA("regeneracion_pasiva", "Regeneración Pasiva", 
        "Regenera 0.5 corazones cada 20s", 
        SkillBranch.SUPERVIVENCIA, SkillTier.TIER_2, SkillRarity.EPICO, 
        1800, Material.GOLDEN_APPLE, false, new String[]{"piel_gruesa"}),
    
    // Tier 3
    INMORTAL("inmortal", "Inmortal", 
        "+8 corazones permanentes (total +14)", 
        SkillBranch.SUPERVIVENCIA, SkillTier.TIER_3, SkillRarity.LEGENDARIO, 
        5000, Material.TOTEM_OF_UNDYING, false, new String[]{"tanque"}),
    
    VUELO_EMERGENCIA("vuelo_emergencia", "Vuelo de Emergencia", 
        "Al caer de altura mortal, planeo tipo Elytra por 3s (cooldown 1 min)", 
        SkillBranch.SUPERVIVENCIA, SkillTier.TIER_3, SkillRarity.EPICO, 
        4000, Material.ELYTRA, true, new String[]{"pluma"}),
    
    FENIX("fenix", "Fénix", 
        "Al morir, revives en el lugar con 3♥ (1 vez/día)", 
        SkillBranch.SUPERVIVENCIA, SkillTier.TIER_3, SkillRarity.LEGENDARIO, 
        6000, Material.BLAZE_POWDER, false, new String[]{"regeneracion_pasiva"}),
    
    ANFIBIO("anfibio", "Anfibio", 
        "Respiración infinita bajo agua", 
        SkillBranch.SUPERVIVENCIA, SkillTier.TIER_3, SkillRarity.EPICO, 
        3000, Material.HEART_OF_THE_SEA, false, new String[]{"branquias"});
    
    // ================= PROPIEDADES =================
    private final String id;
    private final String displayName;
    private final String description;
    private final SkillBranch branch;
    private final SkillTier tier;
    private final SkillRarity rarity;
    private final int baseCost;
    private final Material icon;
    private final boolean toggleable;
    private final String[] requirements;
    
    Skill(String id, String displayName, String description, 
          SkillBranch branch, SkillTier tier, SkillRarity rarity, 
          int baseCost, Material icon, boolean toggleable, String[] requirements) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.branch = branch;
        this.tier = tier;
        this.rarity = rarity;
        this.baseCost = baseCost;
        this.icon = icon;
        this.toggleable = toggleable;
        this.requirements = requirements;
    }
    
    // ================= GETTERS =================
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public SkillBranch getBranch() { return branch; }
    public SkillTier getTier() { return tier; }
    public SkillRarity getRarity() { return rarity; }
    public int getBaseCost() { return baseCost; }
    public Material getIcon() { return icon; }
    public boolean isToggleable() { return toggleable; }
    public String[] getRequirements() { return requirements; }
    
    /**
     * Calcula el costo final de la habilidad
     * Fórmula: baseCost × tierMultiplier × rarityMultiplier
     */
    public int getFinalCost() {
        return (int) (baseCost * tier.getCostMultiplier() * rarity.getCostMultiplier());
    }
    
    /**
     * Obtiene el nombre con color de rareza
     */
    public String getColoredName() {
        return rarity.getColor() + displayName;
    }
    
    /**
     * Busca una habilidad por ID
     */
    public static Skill fromId(String id) {
        for (Skill skill : values()) {
            if (skill.id.equalsIgnoreCase(id)) {
                return skill;
            }
        }
        return null;
    }
    
    /**
     * Obtiene todas las habilidades de una rama
     */
    public static List<Skill> getByBranch(SkillBranch branch) {
        List<Skill> skills = new ArrayList<>();
        for (Skill skill : values()) {
            if (skill.branch == branch) {
                skills.add(skill);
            }
        }
        return skills;
    }
    
    /**
     * Obtiene todas las habilidades de un tier
     */
    public static List<Skill> getByTier(SkillTier tier) {
        List<Skill> skills = new ArrayList<>();
        for (Skill skill : values()) {
            if (skill.tier == tier) {
                skills.add(skill);
            }
        }
        return skills;
    }
    
    /**
     * Obtiene habilidades de una rama y tier específicos
     */
    public static List<Skill> getByBranchAndTier(SkillBranch branch, SkillTier tier) {
        List<Skill> skills = new ArrayList<>();
        for (Skill skill : values()) {
            if (skill.branch == branch && skill.tier == tier) {
                skills.add(skill);
            }
        }
        return skills;
    }
    
    /**
     * Obtiene todas las habilidades toggleables
     */
    public static List<Skill> getToggleable() {
        List<Skill> skills = new ArrayList<>();
        for (Skill skill : values()) {
            if (skill.toggleable) {
                skills.add(skill);
            }
        }
        return skills;
    }
}
