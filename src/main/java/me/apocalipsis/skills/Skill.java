package me.apocalipsis.skills;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.bukkit.Material;

/**
 * Enum que define todas las habilidades disponibles en el árbol.
 * 5 ramas × ~10 skills cada una = ~50 habilidades únicas
 * 
 * NOTA: Algunas habilidades están DESHABILITADAS (ver DISABLED_SKILLS)
 * pero se mantienen en el código para reactivarlas en el futuro.
 */
public enum Skill {
    
    // ================= ALMACENAMIENTO (7 skills) =================
    // Tier 1
    BOLSILLOS_PROFUNDOS("bolsillos_profundos", "Bolsillos Profundos", 
        "Mochila virtual de 9 slots (/mochila)", 
        SkillBranch.ALMACENAMIENTO, SkillTier.TIER_1, SkillRarity.COMUN, 
        500, Material.CHEST, false, new String[]{}),
    
    COFRE_INTERIOR("cofre_interior", "Cofre Interior", 
        "Ender chest portable (3 usos/día sin bloque)", 
        SkillBranch.ALMACENAMIENTO, SkillTier.TIER_1, SkillRarity.COMUN, 
        400, Material.ENDER_CHEST, false, new String[]{}),
    
    // Tier 2
    BOLSILLOS_SIN_FONDO("bolsillos_sin_fondo", "Bolsillos Sin Fondo", 
        "Mochila de 27 slots", 
        SkillBranch.ALMACENAMIENTO, SkillTier.TIER_2, SkillRarity.RARO, 
        1500, Material.BARREL, false, new String[]{"bolsillos_profundos"}),
    
    COFRE_DIMENSIONAL("cofre_dimensional", "Acceso Ilimitado", 
        "Abre ender chest sin límite de usos (/ec)", 
        SkillBranch.ALMACENAMIENTO, SkillTier.TIER_2, SkillRarity.RARO, 
        1200, Material.ENDER_PEARL, false, new String[]{"cofre_interior"}),
    
    AUTO_RECOLECCION("auto_recoleccion", "Auto-Recolección", 
        "Items van directo al inventario al minar", 
        SkillBranch.ALMACENAMIENTO, SkillTier.TIER_2, SkillRarity.EPICO, 
        2000, Material.HOPPER, true, new String[]{"bolsillos_profundos"}),
    
    // Tier 3
    INVENTARIO_INFINITO("inventario_infinito", "Inventario Infinito", 
        "Mochila de 54 slots (cofre doble)", 
        SkillBranch.ALMACENAMIENTO, SkillTier.TIER_3, SkillRarity.LEGENDARIO, 
        4000, Material.SHULKER_BOX, false, new String[]{"bolsillos_sin_fondo"}),
    
    VOID_STORAGE("void_storage", "Void Storage", 
        "Guarda 9 items protegidos de la muerte (slot especial)", 
        SkillBranch.ALMACENAMIENTO, SkillTier.TIER_3, SkillRarity.LEGENDARIO, 
        3500, Material.ENDER_EYE, false, new String[]{"cofre_dimensional"}),
    
    // ================= UTILIDAD (10 skills) =================
    // Tier 1
    PASO_LIGERO("paso_ligero", "Paso Ligero", 
        "+10% velocidad de movimiento permanente", 
        SkillBranch.UTILIDAD, SkillTier.TIER_1, SkillRarity.COMUN, 
        400, Material.LEATHER_BOOTS, true, new String[]{}),
    
    MINERO_EFICIENTE("minero_eficiente", "Minero Eficiente", 
        "+15% velocidad de minado (Haste I)", 
        SkillBranch.UTILIDAD, SkillTier.TIER_1, SkillRarity.COMUN, 
        450, Material.IRON_PICKAXE, true, new String[]{}),
    
    ESTOMAGO_HIERRO("estomago_hierro", "Estómago de Hierro", 
        "Hambre baja 20% más lento", 
        SkillBranch.UTILIDAD, SkillTier.TIER_1, SkillRarity.COMUN, 
        350, Material.COOKED_BEEF, false, new String[]{}),
    
    LENADOR_NATO("lenador_nato", "Leñador Nato",
        "Rompe árboles completos (cooldown 5s)",
        SkillBranch.UTILIDAD, SkillTier.TIER_1, SkillRarity.RARO,
        600, Material.DIAMOND_AXE, true, new String[]{}),
    
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
    
    LENADOR_EXPERTO("lenador_experto", "Leñador Experto",
        "Cooldown reducido a 2s + más bloques",
        SkillBranch.UTILIDAD, SkillTier.TIER_2, SkillRarity.EPICO,
        1800, Material.NETHERITE_AXE, true, new String[]{"lenador_nato"}),
    
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
    
    LENADOR_MAESTRO("lenador_maestro", "Leñador Maestro",
        "¡SIN COOLDOWN! + Auto-replant + XP bonus",
        SkillBranch.UTILIDAD, SkillTier.TIER_3, SkillRarity.LEGENDARIO,
        4000, Material.ENCHANTED_GOLDEN_APPLE, true, new String[]{"lenador_experto"}),
    
    // ================= SUPERVIVENCIA (11 skills) =================
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
        1200, Material.PHANTOM_MEMBRANE, false, new String[]{"caida_suave"}),
    
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
        3000, Material.HEART_OF_THE_SEA, false, new String[]{"branquias"}),
    
    // ================= COMBATE (12 skills) =================
    // Tier 1
    GOLPE_CERTERO("golpe_certero", "Golpe Certero",
        "+5% daño de ataque base",
        SkillBranch.COMBATE, SkillTier.TIER_1, SkillRarity.COMUN,
        400, Material.IRON_SWORD, false, new String[]{}),
    
    REFLEJOS("reflejos", "Reflejos",
        "+10% velocidad de ataque",
        SkillBranch.COMBATE, SkillTier.TIER_1, SkillRarity.COMUN,
        450, Material.CLOCK, false, new String[]{}),
    
    PIEL_ESCAMAS("piel_escamas", "Piel de Escamas",
        "-5% daño recibido de mobs",
        SkillBranch.COMBATE, SkillTier.TIER_1, SkillRarity.COMUN,
        500, Material.TURTLE_HELMET, false, new String[]{}),
    
    ARQUERO("arquero", "Arquero",
        "+10% daño con arcos",
        SkillBranch.COMBATE, SkillTier.TIER_1, SkillRarity.COMUN,
        400, Material.BOW, false, new String[]{}),
    
    // Tier 2
    GUERRERO("guerrero", "Guerrero",
        "+10% daño cuerpo a cuerpo",
        SkillBranch.COMBATE, SkillTier.TIER_2, SkillRarity.RARO,
        1500, Material.DIAMOND_SWORD, false, new String[]{"golpe_certero"}),
    
    FURIA("furia", "Furia",
        "Daño aumenta +1% por cada 1% de vida perdida",
        SkillBranch.COMBATE, SkillTier.TIER_2, SkillRarity.EPICO,
        2000, Material.NETHER_STAR, true, new String[]{"golpe_certero"}),
    
    BLOQUEO_PERFECTO("bloqueo_perfecto", "Bloqueo Perfecto",
        "Con escudo: -15% daño + knockback al atacante",
        SkillBranch.COMBATE, SkillTier.TIER_2, SkillRarity.RARO,
        1400, Material.SHIELD, false, new String[]{"piel_escamas"}),
    
    FRANCOTIRADOR("francotirador", "Francotirador",
        "+20% daño con arcos a distancia >15 bloques",
        SkillBranch.COMBATE, SkillTier.TIER_2, SkillRarity.RARO,
        1600, Material.CROSSBOW, false, new String[]{"arquero"}),
    
    // Tier 3
    EJECUTOR("ejecutor", "Ejecutor",
        "+25% daño a enemigos con <30% vida",
        SkillBranch.COMBATE, SkillTier.TIER_3, SkillRarity.EPICO,
        3500, Material.NETHERITE_SWORD, false, new String[]{"guerrero"}),
    
    BERSERKER("berserker", "Berserker",
        "Con <25% vida: +40% daño, +20% velocidad",
        SkillBranch.COMBATE, SkillTier.TIER_3, SkillRarity.LEGENDARIO,
        5000, Material.NETHERITE_AXE, true, new String[]{"furia"}),
    
    VAMPIRISMO("vampirismo", "Vampirismo",
        "5% del daño infligido se convierte en vida",
        SkillBranch.COMBATE, SkillTier.TIER_3, SkillRarity.LEGENDARIO,
        4500, Material.GHAST_TEAR, false, new String[]{"ejecutor"}),
    
    MULTISHOT("multishot", "Multishot",
        "15% chance de disparar 2 flechas extra",
        SkillBranch.COMBATE, SkillTier.TIER_3, SkillRarity.EPICO,
        3800, Material.ARROW, false, new String[]{"francotirador"}),
    
    // ================= EXPLORACIÓN (12 skills) =================
    // Tier 1
    VISION_NOCTURNA("vision_nocturna", "Visión Nocturna",
        "Ve en la oscuridad (Night Vision)",
        SkillBranch.EXPLORACION, SkillTier.TIER_1, SkillRarity.COMUN,
        400, Material.GOLDEN_CARROT, true, new String[]{}),
    
    BRUJULA_INTERNA("brujula_interna", "Brújula Interna",
        "Muestra coordenadas en el HUD (desactivable si interfiere)",
        SkillBranch.EXPLORACION, SkillTier.TIER_1, SkillRarity.COMUN,
        300, Material.COMPASS, true, new String[]{}),
    
    RASTRO_ORO("rastro_oro", "Rastro de Oro",
        "/avo habilidades rastro - Detecta minerales 15 bloques (10s, cd 60s)",
        SkillBranch.EXPLORACION, SkillTier.TIER_1, SkillRarity.RARO,
        700, Material.RAW_GOLD, false, new String[]{}),
    
    PISADAS_SILENCIOSAS("pisadas_silenciosas", "Pisadas Silenciosas",
        "Mobs hostiles te detectan 30% menos",
        SkillBranch.EXPLORACION, SkillTier.TIER_1, SkillRarity.COMUN,
        450, Material.LEATHER_BOOTS, false, new String[]{}),
    
    // Tier 2
    TELESCOPIO("telescopio", "Telescopio",
        "Zoom permanente al usar catalejo",
        SkillBranch.EXPLORACION, SkillTier.TIER_2, SkillRarity.RARO,
        1000, Material.SPYGLASS, false, new String[]{"vision_nocturna"}),
    
    MAPA_MENTAL("mapa_mental", "Mapa Mental",
        "Recuerda ubicación de muerte por 10 min",
        SkillBranch.EXPLORACION, SkillTier.TIER_2, SkillRarity.RARO,
        1200, Material.FILLED_MAP, false, new String[]{"brujula_interna"}),
    
    DETECTOR_SPAWNERS("detector_spawners", "Detector de Spawners",
        "/avo habilidades detector - Detecta spawners 30 bloques (15s, cd 90s)",
        SkillBranch.EXPLORACION, SkillTier.TIER_2, SkillRarity.EPICO,
        2000, Material.SPAWNER, false, new String[]{"rastro_oro"}),
    
    SOMBRA("sombra", "Sombra",
        "Mobs hostiles te ignoran 50% (sneaking)",
        SkillBranch.EXPLORACION, SkillTier.TIER_2, SkillRarity.RARO,
        1500, Material.WITHER_ROSE, false, new String[]{"pisadas_silenciosas"}),
    
    // Tier 3
    OJO_AGUILA("ojo_aguila", "Ojo de Águila",
        "Marca enemigos cercanos con Glowing (desactivable)",
        SkillBranch.EXPLORACION, SkillTier.TIER_3, SkillRarity.EPICO,
        3500, Material.ENDER_EYE, true, new String[]{"telescopio"}),
    
    WAYPOINT("waypoint", "Waypoint",
        "/waypoint - Teletransporte a ubicación guardada (cooldown 5 min)",
        SkillBranch.EXPLORACION, SkillTier.TIER_3, SkillRarity.LEGENDARIO,
        5000, Material.LODESTONE, false, new String[]{"mapa_mental"}),
    
    XRAY_DIAMANTES("xray_diamantes", "Sentido del Diamante",
        "/avo habilidades diamantes - Detecta diamantes 12 bloques (8s, cd 120s)",
        SkillBranch.EXPLORACION, SkillTier.TIER_3, SkillRarity.LEGENDARIO,
        6000, Material.DIAMOND_ORE, false, new String[]{"detector_spawners"}),
    
    FANTASMA("fantasma", "Fantasma",
        "Invisible por 10s al recibir daño crítico (cooldown 2 min)",
        SkillBranch.EXPLORACION, SkillTier.TIER_3, SkillRarity.EPICO,
        4000, Material.PHANTOM_MEMBRANE, false, new String[]{"sombra"}),
    
    // ================= INVOCACION (8 skills) =================
    // Tier 1 - Compañeros básicos
    LOBO_COMPANERO("lobo_companero", "Lobo Compañero",
        "Invoca un lobo que te sigue y ataca enemigos (15 min)",
        SkillBranch.INVOCACION, SkillTier.TIER_1, SkillRarity.COMUN,
        600, Material.BONE, false, new String[]{}),
    
    GATO_GUARDIAN("gato_guardian", "Gato Guardián",
        "Invoca un gato que ahuyenta creepers y phantoms",
        SkillBranch.INVOCACION, SkillTier.TIER_1, SkillRarity.COMUN,
        500, Material.COD, false, new String[]{}),
    
    // Tier 2 - Compañeros mejorados
    MANADA_LOBOS("manada_lobos", "Manada de Lobos",
        "Invoca 3 lobos en lugar de 1",
        SkillBranch.INVOCACION, SkillTier.TIER_2, SkillRarity.RARO,
        1500, Material.WOLF_ARMOR, false, new String[]{"lobo_companero"}),
    
    ALLAY_RECOLECTOR("allay_recolector", "Allay Recolector",
        "Invoca un allay que recoge items cercanos por ti",
        SkillBranch.INVOCACION, SkillTier.TIER_2, SkillRarity.RARO,
        1800, Material.AMETHYST_SHARD, false, new String[]{}),
    
    ABEJAS_PROTECTORAS("abejas_protectoras", "Abejas Protectoras",
        "Invoca abejas que atacan a quien te dañe",
        SkillBranch.INVOCACION, SkillTier.TIER_2, SkillRarity.EPICO,
        2000, Material.HONEYCOMB, true, new String[]{"gato_guardian"}),
    
    // Tier 3 - Compañeros legendarios
    GOLEM_PROTECTOR("golem_protector", "Gólem Protector",
        "Invoca un golem de hierro temporal (5 min, cd 10 min)",
        SkillBranch.INVOCACION, SkillTier.TIER_3, SkillRarity.LEGENDARIO,
        5000, Material.IRON_BLOCK, false, new String[]{"manada_lobos"}),
    
    VEX_VENGADOR("vex_vengador", "Vex Vengador",
        "Invoca 2 vex que atacan a tu objetivo actual",
        SkillBranch.INVOCACION, SkillTier.TIER_3, SkillRarity.EPICO,
        4000, Material.TOTEM_OF_UNDYING, false, new String[]{"abejas_protectoras"}),
    
    WARDEN_TEMPORAL("warden_temporal", "Warden Temporal",
        "Invoca un mini-warden aliado por 30s (cd 30 min)",
        SkillBranch.INVOCACION, SkillTier.TIER_3, SkillRarity.LEGENDARIO,
        8000, Material.SCULK_CATALYST, false, new String[]{"golem_protector", "vex_vengador"}),
    
    // ================= SINERGIAS (8 skills) =================
    // Requieren habilidades de múltiples ramas
    // Tier 2 - Sinergias básicas
    CAZADOR_EXPERTO("cazador_experto", "Cazador Experto",
        "+25% daño a mobs + doble loot de carne",
        SkillBranch.SINERGIAS, SkillTier.TIER_2, SkillRarity.RARO,
        2000, Material.BOW, false, new String[]{"arquero", "detector_spawners"}),
    
    MINERO_GUERRERO("minero_guerrero", "Minero Guerrero",
        "Pico hace +50% daño a mobs de piedra/mineral",
        SkillBranch.SINERGIAS, SkillTier.TIER_2, SkillRarity.RARO,
        1800, Material.DIAMOND_PICKAXE, false, new String[]{"minero_eficiente", "golpe_certero"}),
    
    EXPLORADOR_LIGERO("explorador_ligero", "Explorador Ligero",
        "+20% velocidad cuando llevas mochila llena",
        SkillBranch.SINERGIAS, SkillTier.TIER_2, SkillRarity.RARO,
        1500, Material.FEATHER, true, new String[]{"bolsillos_profundos", "paso_ligero"}),
    
    // Tier 3 - Sinergias avanzadas
    GUERRERO_INMORTAL("guerrero_inmortal", "Guerrero Inmortal",
        "Al morir, revives con 50% HP (cd 30 min)",
        SkillBranch.SINERGIAS, SkillTier.TIER_3, SkillRarity.LEGENDARIO,
        6000, Material.TOTEM_OF_UNDYING, false, new String[]{"tanque", "regeneracion_pasiva", "berserker"}),
    
    MERCADER_SUPREMO("mercader_supremo", "Mercader Supremo",
        "Villagers dan 50% descuento + trades infinitos",
        SkillBranch.SINERGIAS, SkillTier.TIER_3, SkillRarity.EPICO,
        4500, Material.EMERALD_BLOCK, false, new String[]{"toque_fortuna", "auto_recoleccion"}),
    
    DOMADOR_BESTIAS("domador_bestias", "Domador de Bestias",
        "Todos tus compañeros invocados son 2x más fuertes",
        SkillBranch.SINERGIAS, SkillTier.TIER_3, SkillRarity.LEGENDARIO,
        5500, Material.LEAD, false, new String[]{"lobo_companero", "gato_guardian", "allay_recolector"}),
    
    OMNIPRESENTE("omnipresente", "Omnipresente",
        "Puedes ver a través de paredes por 5s (cd 2 min)",
        SkillBranch.SINERGIAS, SkillTier.TIER_3, SkillRarity.LEGENDARIO,
        7000, Material.SPECTRAL_ARROW, false, new String[]{"ojo_aguila", "xray_diamantes", "brujula_interna"}),
    
    AVATAR_CAOS("avatar_caos", "Avatar del Caos",
        "Activa TODAS tus habilidades toggleables por 30s (cd 1h)",
        SkillBranch.SINERGIAS, SkillTier.TIER_3, SkillRarity.LEGENDARIO,
        10000, Material.NETHER_STAR, false, new String[]{"guerrero_inmortal", "domador_bestias", "omnipresente"});
    
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
    
    // ================= HABILIDADES DESHABILITADAS =================
    // IDs de skills que existen pero están desactivadas temporalmente
    // Para reactivar: simplemente quitar el ID de esta lista
    private static final Set<String> DISABLED_SKILLS = Set.of(
        // UTILIDAD - Poco útiles o redundantes
        "estomago_hierro",      // Hambre no es problema real en survival
        "metabolismo_lento",     // Mejora del anterior, sigue siendo innecesario
        "autosuficiente",        // Regenera hambre muy lento (0.5 cada 30s)
        
        // SUPERVIVENCIA - Muy situacionales
        "resistencia_fuego",     // Solo útil en Nether, muy específico
        "ignifugo",              // Mejora del anterior, sigue siendo situacional
        
        // COMBATE - Poco impacto o muy específicos
        "piel_escamas",          // -5% daño es casi nada
        "reflejos",              // Velocidad ataque es menor problema
        "bloqueo_perfecto",      // Requiere escudo, muy específico
        
        // EXPLORACIÓN - Tramposos o redundantes
        "brujula_interna",       // F3 ya existe, innecesario
        "telescopio",            // Catalejo vanilla ya existe
        "mapa_mental",           // Puede anotar coords, poco útil
        "pisadas_silenciosas",   // Poco útil en práctica
        "sombra",                // Mejora del anterior, sigue siendo situacional
        "rastro_oro",            // Detector de minerales es muy tramposo
        "detector_spawners",     // Muy específico, poco uso
        "xray_diamantes",        // DEMASIADO tramposo, desbalanceado
        
        // INVOCACIÓN - Poco útiles comparados con otros
        "gato_guardian",         // Ahuyentar creepers es muy situacional
        "allay_recolector",      // Auto-recolección ya existe como skill
        
        // SINERGIAS - Demasiado específicas o complejas
        "cazador_experto",       // Muy específico (solo carne de mobs)
        "minero_guerrero",       // Raro usar pico en combate
        "explorador_ligero",     // Condición muy específica (mochila llena)
        "omnipresente"           // Ver a través de paredes es muy tramposo
    );
    
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
     * Verifica si esta habilidad está HABILITADA.
     * Las habilidades deshabilitadas se mantienen en el código pero no aparecen en el árbol.
     * Para reactivar: quitar su ID de DISABLED_SKILLS.
     */
    public boolean isEnabled() {
        return !DISABLED_SKILLS.contains(this.id);
    }
    
    /**
     * Verifica si esta habilidad puede ser mejorada.
     * Algunas habilidades son binarias (on/off) y no tiene sentido mejorarlas.
     */
    public boolean isUpgradeable() {
        // Skills que NO se pueden mejorar (binarias o con efecto fijo)
        return switch (this) {
            case COFRE_INTERIOR,           // Acceso a ender chest - binario
                 COFRE_DIMENSIONAL,         // Acceso ilimitado - binario
                 VISION_NOCTURNA,           // Night vision on/off - binario
                 BRUJULA_INTERNA,           // Muestra coords - binario
                 INVENTARIO_INFINITO,       // Ya es máximo (54 slots)
                 ANFIBIO,                   // Respiración infinita - binario
                 GOLEM_PROTECTOR,           // Siempre 1 golem
                 WARDEN_TEMPORAL,           // Siempre 1 warden
                 LOBO_COMPANERO,            // Se mejora con MANADA_LOBOS
                 GATO_GUARDIAN,             // Efecto fijo (ahuyenta mobs)
                 ALLAY_RECOLECTOR,          // Efecto fijo (recoge items)
                 FENIX,                     // Revive 1 vez/día - binario
                 WAYPOINT,                  // Teletransporte - el cooldown cambia pero es muy específico
                 AVATAR_CAOS                // Habilidad única especial
                 -> false;
            default -> true;
        };
    }

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
     * Obtiene todas las habilidades de una rama (solo habilitadas)
     */
    public static List<Skill> getByBranch(SkillBranch branch) {
        List<Skill> skills = new ArrayList<>();
        for (Skill skill : values()) {
            if (skill.branch == branch && skill.isEnabled()) {
                skills.add(skill);
            }
        }
        return skills;
    }
    
    /**
     * Obtiene todas las habilidades de un tier (solo habilitadas)
     */
    public static List<Skill> getByTier(SkillTier tier) {
        List<Skill> skills = new ArrayList<>();
        for (Skill skill : values()) {
            if (skill.tier == tier && skill.isEnabled()) {
                skills.add(skill);
            }
        }
        return skills;
    }
    
    /**
     * Obtiene habilidades de una rama y tier específicos (solo habilitadas)
     */
    public static List<Skill> getByBranchAndTier(SkillBranch branch, SkillTier tier) {
        List<Skill> skills = new ArrayList<>();
        for (Skill skill : values()) {
            if (skill.branch == branch && skill.tier == tier && skill.isEnabled()) {
                skills.add(skill);
            }
        }
        return skills;
    }
    
    /**
     * Obtiene todas las habilidades toggleables (solo habilitadas)
     */
    public static List<Skill> getToggleable() {
        List<Skill> skills = new ArrayList<>();
        for (Skill skill : values()) {
            if (skill.toggleable && skill.isEnabled()) {
                skills.add(skill);
            }
        }
        return skills;
    }
    
    /**
     * Obtiene el total de habilidades habilitadas
     */
    public static int getEnabledCount() {
        int count = 0;
        for (Skill skill : values()) {
            if (skill.isEnabled()) count++;
        }
        return count;
    }
    
    /**
     * Obtiene el total de habilidades deshabilitadas
     */
    public static int getDisabledCount() {
        return DISABLED_SKILLS.size();
    }
}
