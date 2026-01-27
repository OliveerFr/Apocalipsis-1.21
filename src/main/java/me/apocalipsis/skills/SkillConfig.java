package me.apocalipsis.skills;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.*;

/**
 * Configuración extendida de habilidades.
 * Contiene efectos por nivel, condiciones y sinergias.
 */
public class SkillConfig {
    
    // ==================== EFECTOS POR NIVEL ====================
    // skill_id -> nivel -> valor del efecto
    private static final Map<String, Map<Integer, Double>> LEVEL_EFFECTS = new HashMap<>();
    
    // ==================== CONDICIONES ====================
    // skill_id -> tipo de condición
    private static final Map<String, SkillCondition> CONDITIONS = new HashMap<>();
    
    // ==================== DESCRIPCIONES POR NIVEL ====================
    private static final Map<String, String[]> LEVEL_DESCRIPTIONS = new HashMap<>();
    
    // ==================== BONUS NIVEL 3 ====================
    private static final Map<String, String> LEVEL_3_BONUS = new HashMap<>();
    
    static {
        initLevelEffects();
        initConditions();
        initDescriptions();
        initLevel3Bonuses();
    }
    
    private static void initLevelEffects() {
        // ===== ALMACENAMIENTO =====
        addLevelEffect("bolsillos_profundos", 9, 18, 27);        // slots de mochila
        addLevelEffect("cofre_interior", 1, 1, 1);                // siempre 1 (es acceso)
        addLevelEffect("bolsillos_sin_fondo", 27, 36, 45);       // slots
        addLevelEffect("auto_recoleccion", 50, 75, 100);         // % de items auto-recogidos
        addLevelEffect("inventario_infinito", 54, 54, 54);       // siempre max
        addLevelEffect("void_storage", 50, 75, 100);             // % items guardados al morir
        
        // ===== UTILIDAD =====
        addLevelEffect("paso_ligero", 10, 15, 20);               // % velocidad
        addLevelEffect("minero_eficiente", 15, 25, 35);          // % velocidad minado
        addLevelEffect("estomago_hierro", 20, 35, 50);           // % reducción hambre
        addLevelEffect("lenador_nato", 1, 2, 3);                 // árboles por activación
        addLevelEffect("zancadas", 15, 20, 25);                  // % velocidad + salto [BALANCEADO: era 20/30/40]
        addLevelEffect("toque_fortuna", 8, 12, 15);              // % drop extra [BALANCEADO: era 10/20/30]
        addLevelEffect("metabolismo_lento", 40, 60, 80);         // % reducción hambre
        addLevelEffect("velocista", 25, 30, 35);                 // % velocidad [BALANCEADO: era 30/40/50]
        addLevelEffect("seda_natural", 3, 5, 8);                 // % silk touch [BALANCEADO: era 5/10/15]
        addLevelEffect("autosuficiente", 0.5, 1.0, 1.5);         // hambre regenerada
        
        // ===== SUPERVIVENCIA =====
        addLevelEffect("piel_gruesa", 2, 3, 4);                  // corazones extra
        addLevelEffect("caida_suave", 25, 40, 60);               // % reducción caída
        addLevelEffect("resistencia_fuego", 20, 35, 50);         // % reducción fuego
        addLevelEffect("nadador", 30, 50, 70);                   // % velocidad nadando
        addLevelEffect("tanque", 3, 5, 7);                       // corazones extra (+piel) [BALANCEADO: era 4/6/8]
        addLevelEffect("pluma", 50, 70, 90);                     // % reducción caída
        addLevelEffect("ignifugo", 40, 60, 80);                  // % reducción fuego
        addLevelEffect("branquias", 60, 120, 999);               // segundos extra (999=infinito)
        addLevelEffect("regeneracion_pasiva", 0.5, 1.0, 1.5);    // corazones cada 20s
        addLevelEffect("inmortal", 6, 8, 10);                    // corazones extra total [BALANCEADO: era 8/10/14]
        addLevelEffect("vuelo_emergencia", 3, 5, 8);             // segundos de planeo
        addLevelEffect("fenix", 1, 1, 2);                        // usos por día
        addLevelEffect("anfibio", 999, 999, 999);                // respiración infinita
        
        // ===== COMBATE =====
        addLevelEffect("golpe_certero", 5, 10, 15);              // % daño extra
        addLevelEffect("reflejos", 10, 15, 20);                  // % velocidad ataque
        addLevelEffect("piel_escamas", 5, 10, 15);               // % reducción daño
        addLevelEffect("arquero", 10, 15, 25);                   // % daño arco
        addLevelEffect("guerrero", 10, 15, 25);                  // % daño melee
        addLevelEffect("furia", 1, 1.5, 2);                      // % daño por % vida perdida
        addLevelEffect("bloqueo_perfecto", 15, 25, 35);          // % reducción con escudo
        addLevelEffect("francotirador", 15, 25, 35);             // % daño a distancia [BALANCEADO: era 20/35/50]
        addLevelEffect("ejecutor", 20, 30, 40);                  // % daño a enemigos bajos [BALANCEADO: era 25/40/60]
        addLevelEffect("berserker", 30, 40, 50);                 // % daño cuando bajo vida [BALANCEADO: era 40/60/80]
        addLevelEffect("vampirismo", 4, 6, 8);                   // % lifesteal [BALANCEADO: era 5/8/12]
        addLevelEffect("multishot", 15, 25, 35);                 // % chance flechas extra
        
        // ===== EXPLORACIÓN =====
        addLevelEffect("vision_nocturna", 1, 1, 1);              // siempre activo
        addLevelEffect("brujula_interna", 1, 1, 1);              // siempre activo
        addLevelEffect("rastro_oro", 30, 20, 10);                // cooldown en segundos
        addLevelEffect("pisadas_silenciosas", 30, 50, 70);       // % menos detección
        addLevelEffect("telescopio", 2, 4, 8);                   // nivel de zoom
        addLevelEffect("mapa_mental", 10, 20, 30);               // minutos recordar muerte
        addLevelEffect("detector_spawners", 20, 35, 50);         // rango en bloques
        addLevelEffect("sombra", 50, 70, 90);                    // % ignorar mobs
        addLevelEffect("ojo_aguila", 15, 25, 40);                // rango glowing
        addLevelEffect("waypoint", 5, 3, 1);                     // cooldown en minutos
        addLevelEffect("xray_diamantes", 10, 15, 20);            // rango en bloques
        addLevelEffect("fantasma", 10, 15, 20);                  // segundos invisible
        
        // ===== INVOCACIÓN (nuevas) =====
        addLevelEffect("lobo_fiel", 1, 2, 3);                    // número de lobos
        addLevelEffect("gato_guardian", 1, 1, 2);                // número de gatos
        addLevelEffect("allay_recolector", 1, 2, 3);             // allays
        addLevelEffect("golem_temporal", 30, 60, 120);           // duración segundos
        addLevelEffect("enjambre_abejas", 3, 5, 8);              // número de abejas
        addLevelEffect("caballo_espectral", 60, 120, 300);       // duración segundos
        addLevelEffect("ejercito_esqueletos", 2, 4, 6);          // esqueletos
        addLevelEffect("dragon_mini", 1, 1, 1);                  // siempre 1
        
        // ===== INVOCACIÓN (adicionales) =====
        addLevelEffect("lobo_companero", 1, 1, 2);               // número de lobos
        addLevelEffect("manada_lobos", 2, 3, 5);                 // número de lobos en manada
        addLevelEffect("abejas_protectoras", 2, 4, 6);           // número de abejas
        addLevelEffect("golem_protector", 1, 1, 1);              // siempre 1 golem
        addLevelEffect("vex_vengador", 1, 2, 3);                 // número de vex
        addLevelEffect("warden_temporal", 30, 45, 60);           // duración segundos
        
        // ===== SINERGIAS =====
        addLevelEffect("cazador_sigiloso", 20, 30, 40);          // % daño extra sigilo [BALANCEADO: era 25/40/60]
        addLevelEffect("guerrero_inmortal", 25, 35, 50);         // % daño post-revive [BALANCEADO: era 30/50/75]
        addLevelEffect("mercader_ambulante", 1, 1, 1);           // acceso a vender
        addLevelEffect("explorador_veloz", 30, 40, 50);          // % velocidad explorar [BALANCEADO: era 40/60/80]
        addLevelEffect("guardian_bestial", 20, 35, 50);          // % daño mascotas
        addLevelEffect("maestro_elemental", 25, 40, 60);         // % resistencia total [BALANCEADO: era 30/50/75]
        
        // ===== SINERGIAS (adicionales) =====
        addLevelEffect("cazador_experto", 12, 20, 30);           // % daño extra a mobs [BALANCEADO: era 15/25/40]
        addLevelEffect("minero_guerrero", 20, 30, 40);           // % daño con pico [BALANCEADO: era 25/40/60]
        addLevelEffect("explorador_ligero", 15, 25, 35);         // % velocidad extra [BALANCEADO: era 20/35/50]
        addLevelEffect("mercader_supremo", 10, 15, 20);          // % descuento trades [BALANCEADO: era 10/20/30]
        addLevelEffect("domador_bestias", 20, 35, 50);           // % daño mascotas
        addLevelEffect("omnipresente", 50, 75, 100);             // rango teleport
        addLevelEffect("avatar_caos", 25, 35, 50);               // % stats totales [BALANCEADO: era 30/50/75]
        
        // ===== ALMACENAMIENTO (adicional) =====
        addLevelEffect("cofre_dimensional", 1, 1, 1);            // acceso ilimitado
    }
    
    private static void initConditions() {
        // Condiciones por bioma
        CONDITIONS.put("rastro_oro", new SkillCondition(ConditionType.BIOME, "BADLANDS", 1.5));
        CONDITIONS.put("nadador", new SkillCondition(ConditionType.BIOME, "OCEAN", 1.3));
        CONDITIONS.put("ignifugo", new SkillCondition(ConditionType.BIOME, "NETHER", 1.5));
        
        // Condiciones por tiempo
        CONDITIONS.put("vision_nocturna", new SkillCondition(ConditionType.TIME, "NIGHT", 1.0));
        CONDITIONS.put("sombra", new SkillCondition(ConditionType.TIME, "NIGHT", 1.5));
        CONDITIONS.put("fantasma", new SkillCondition(ConditionType.TIME, "NIGHT", 1.3));
        
        // Condiciones por clima
        CONDITIONS.put("toque_fortuna", new SkillCondition(ConditionType.WEATHER, "RAIN", 1.25));
        CONDITIONS.put("regeneracion_pasiva", new SkillCondition(ConditionType.WEATHER, "CLEAR", 1.2));
        
        // Condiciones por salud
        CONDITIONS.put("berserker", new SkillCondition(ConditionType.HEALTH_BELOW, "25", 1.0));
        CONDITIONS.put("furia", new SkillCondition(ConditionType.HEALTH_BELOW, "50", 1.0));
        CONDITIONS.put("fenix", new SkillCondition(ConditionType.HEALTH_BELOW, "0", 1.0));
    }
    
    private static void initDescriptions() {
        // Formato: {nivel1, nivel2, nivel3}
        LEVEL_DESCRIPTIONS.put("bolsillos_profundos", new String[]{
            "Mochila virtual de §e9 slots",
            "Mochila virtual de §e18 slots",
            "Mochila virtual de §e27 slots §7+ auto-ordenar"
        });
        
        LEVEL_DESCRIPTIONS.put("paso_ligero", new String[]{
            "§a+10% §7velocidad de movimiento",
            "§e+15% §7velocidad de movimiento",
            "§6+20% §7velocidad + §bsin slowdown en agua"
        });
        
        LEVEL_DESCRIPTIONS.put("vampirismo", new String[]{
            "§a5% §7del daño se convierte en vida",
            "§e8% §7del daño se convierte en vida",
            "§612% §7lifesteal + §bheal on kill"
        });
        
        LEVEL_DESCRIPTIONS.put("lobo_fiel", new String[]{
            "Invoca §a1 lobo §7que te sigue y ataca",
            "Invoca §e2 lobos §7que te siguen y atacan",
            "Invoca §63 lobos §7+ §b+20% daño de manada"
        });
        
        // Agregar más según necesidad...
    }
    
    private static void initLevel3Bonuses() {
        // Efectos especiales del nivel 3
        LEVEL_3_BONUS.put("bolsillos_profundos", "Auto-ordenar items");
        LEVEL_3_BONUS.put("paso_ligero", "Sin slowdown en agua");
        LEVEL_3_BONUS.put("minero_eficiente", "Haste II en lugar de I");
        LEVEL_3_BONUS.put("vampirismo", "Heal on kill (+2♥)");
        LEVEL_3_BONUS.put("fenix", "2 usos por día en vez de 1");
        LEVEL_3_BONUS.put("berserker", "+20% velocidad de ataque");
        LEVEL_3_BONUS.put("lobo_fiel", "+20% daño de manada");
        LEVEL_3_BONUS.put("golem_temporal", "Golem de hierro en vez de nieve");
        LEVEL_3_BONUS.put("waypoint", "Cooldown 1 minuto");
        LEVEL_3_BONUS.put("multishot", "Flechas extras causan 100% daño");
        LEVEL_3_BONUS.put("detector_spawners", "También detecta cofres");
        LEVEL_3_BONUS.put("xray_diamantes", "También detecta netherite");
        LEVEL_3_BONUS.put("sombra", "Invisible 3s al agacharse");
        LEVEL_3_BONUS.put("bloqueo_perfecto", "Refleja 10% del daño");
    }
    
    // ==================== UTILIDADES ====================
    
    private static void addLevelEffect(String skillId, double lvl1, double lvl2, double lvl3) {
        Map<Integer, Double> levels = new HashMap<>();
        levels.put(1, lvl1);
        levels.put(2, lvl2);
        levels.put(3, lvl3);
        LEVEL_EFFECTS.put(skillId, levels);
    }
    
    /**
     * Obtiene el valor del efecto para una skill y nivel
     */
    public static double getEffectValue(Skill skill, int level) {
        Map<Integer, Double> levels = LEVEL_EFFECTS.get(skill.getId());
        if (levels == null) return 0;
        return levels.getOrDefault(level, levels.get(1));
    }
    
    /**
     * Obtiene el valor del efecto con condiciones aplicadas
     */
    public static double getEffectValueWithConditions(Skill skill, int level, 
            String biome, boolean isNight, boolean isRaining, double healthPercent) {
        double base = getEffectValue(skill, level);
        
        SkillCondition condition = CONDITIONS.get(skill.getId());
        if (condition == null) return base;
        
        double multiplier = 1.0;
        
        switch (condition.type) {
            case BIOME -> {
                if (biome != null && biome.toUpperCase().contains(condition.value)) {
                    multiplier = condition.multiplier;
                }
            }
            case TIME -> {
                if ((condition.value.equals("NIGHT") && isNight) ||
                    (condition.value.equals("DAY") && !isNight)) {
                    multiplier = condition.multiplier;
                }
            }
            case WEATHER -> {
                if ((condition.value.equals("RAIN") && isRaining) ||
                    (condition.value.equals("CLEAR") && !isRaining)) {
                    multiplier = condition.multiplier;
                }
            }
            case HEALTH_BELOW -> {
                double threshold = Double.parseDouble(condition.value);
                if (healthPercent <= threshold) {
                    multiplier = condition.multiplier;
                }
            }
            case HEALTH_ABOVE -> {
                double threshold = Double.parseDouble(condition.value);
                if (healthPercent >= threshold) {
                    multiplier = condition.multiplier;
                }
            }
        }
        
        return base * multiplier;
    }
    
    /**
     * Obtiene la descripción para un nivel específico
     */
    public static String getLevelDescription(Skill skill, int level) {
        String[] descs = LEVEL_DESCRIPTIONS.get(skill.getId());
        if (descs == null || level < 1 || level > 3) {
            return skill.getDescription();
        }
        return descs[level - 1];
    }
    
    /**
     * Obtiene el bonus especial del nivel 3
     */
    public static String getLevel3Bonus(Skill skill) {
        return LEVEL_3_BONUS.get(skill.getId());
    }
    
    /**
     * Obtiene el valor del efecto para un nivel específico de una skill
     * @param skillId ID de la skill
     * @param level Nivel (1, 2 o 3)
     * @return valor del efecto o 0 si no existe
     */
    public static double getLevelEffect(String skillId, int level) {
        Map<Integer, Double> effects = LEVEL_EFFECTS.get(skillId);
        if (effects == null) return 0;
        return effects.getOrDefault(level, 0.0);
    }
    
    /**
     * Verifica si una skill tiene efectos por nivel configurados
     */
    public static boolean hasLevelEffects(String skillId) {
        return LEVEL_EFFECTS.containsKey(skillId);
    }
    
    /**
     * Verifica si una skill tiene bonus de nivel 3
     */
    public static boolean hasLevel3Bonus(Skill skill) {
        return LEVEL_3_BONUS.containsKey(skill.getId());
    }
    
    /**
     * Obtiene la condición de una skill
     */
    public static SkillCondition getCondition(Skill skill) {
        return CONDITIONS.get(skill.getId());
    }
    
    /**
     * Verifica si una skill tiene condiciones
     */
    public static boolean hasCondition(Skill skill) {
        return CONDITIONS.containsKey(skill.getId());
    }
    
    // ==================== CLASES INTERNAS ====================
    
    public enum ConditionType {
        BIOME,          // Activo en cierto bioma
        TIME,           // Activo de día/noche
        WEATHER,        // Activo con lluvia/despejado
        HEALTH_BELOW,   // Activo bajo cierta vida
        HEALTH_ABOVE    // Activo sobre cierta vida
    }
    
    public static class SkillCondition {
        public final ConditionType type;
        public final String value;
        public final double multiplier;
        
        public SkillCondition(ConditionType type, String value, double multiplier) {
            this.type = type;
            this.value = value;
            this.multiplier = multiplier;
        }
        
        public String getDescription() {
            return switch (type) {
                case BIOME -> "§d⚡ §7Bonus en bioma §e" + value + " §7(×" + multiplier + ")";
                case TIME -> "§d⚡ §7Bonus de §e" + (value.equals("NIGHT") ? "noche" : "día") + " §7(×" + multiplier + ")";
                case WEATHER -> "§d⚡ §7Bonus con §e" + (value.equals("RAIN") ? "lluvia" : "sol") + " §7(×" + multiplier + ")";
                case HEALTH_BELOW -> "§d⚡ §7Activo bajo §c" + value + "% §7vida";
                case HEALTH_ABOVE -> "§d⚡ §7Activo sobre §a" + value + "% §7vida";
            };
        }
    }
}
