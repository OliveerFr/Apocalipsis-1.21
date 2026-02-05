package me.apocalipsis.missions;

public enum MissionRank {
    NOVATO,
    EXPLORADOR,
    SOBREVIVIENTE,
    VETERANO,
    LEYENDA,
    MAESTRO,
    TITAN,
    ABSOLUTO;

    // [RANGOS.YML] Datos configurables (se cargan desde rangos.yml)
    private String displayName;
    private int xpRequired;  // Umbral acumulado desde rangos.yml (deprecado, usar levelRequired)
    private int levelRequired; // NUEVO: Nivel requerido para el rango (más fácil de controlar)
    private int misionesDiarias;
    private String tabPrefix;
    private String tabSuffix;
    private String chatPrefix;
    private String scoreboardColor;
    
    // Valores por defecto hardcodeados (fallback si rangos.yml falla)
    private static final int[] DEFAULT_XP_REQUIRED = {0, 980, 3780, 8330, 14630, 22680, 32480, 44030};
    private static final int[] DEFAULT_LEVEL_REQUIRED = {1, 5, 10, 15, 20, 25, 30, 35}; // NUEVO: Niveles requeridos
    private static final int[] DEFAULT_MISIONES_DIARIAS = {10, 8, 6, 5, 4, 3, 3, 2};

    /**
     * Configura los datos de este rango desde rangos.yml
     */
    public void configure(String displayName, int xpRequired, int levelRequired, int misionesDiarias, 
                         String tabPrefix, String tabSuffix, String chatPrefix, String scoreboardColor) {
        this.displayName = displayName;
        this.xpRequired = xpRequired; // Mantenido por compatibilidad
        this.levelRequired = levelRequired;
        this.misionesDiarias = misionesDiarias;
        this.tabPrefix = tabPrefix;
        this.tabSuffix = tabSuffix;
        this.chatPrefix = chatPrefix;
        this.scoreboardColor = scoreboardColor;
    }

    /**
     * Inicializa valores por defecto (llamado antes de cargar rangos.yml)
     */
    public void initDefaults() {
        int ord = this.ordinal();
        this.displayName = "§f" + this.name();
        this.xpRequired = DEFAULT_XP_REQUIRED[ord];
        this.levelRequired = DEFAULT_LEVEL_REQUIRED[ord];
        this.misionesDiarias = DEFAULT_MISIONES_DIARIAS[ord];
        this.tabPrefix = "";
        this.tabSuffix = "";
        this.chatPrefix = "";
        this.scoreboardColor = "WHITE";
    }

    // Getters
    public String getDisplayName() {
        return displayName != null ? displayName : "§f" + this.name();
    }

    public int getXpRequired() {
        return xpRequired;
    }
    
    /**
     * Obtiene el nivel requerido para este rango (NUEVO SISTEMA - MÁS FÁCIL DE CONTROLAR)
     */
    public int getLevelRequired() {
        return levelRequired;
    }

    public int getMisionesDiarias() {
        return misionesDiarias;
    }

    public String getTabPrefix() {
        return tabPrefix != null ? tabPrefix : "";
    }

    public String getTabSuffix() {
        return tabSuffix != null ? tabSuffix : "";
    }

    public String getChatPrefix() {
        return chatPrefix != null ? chatPrefix : "";
    }

    public String getScoreboardColor() {
        return scoreboardColor != null ? scoreboardColor : "WHITE";
    }

    /**
     * Determina el rango según XP acumulados (umbral_acumulado=true)
     * CORREGIDO: Solo asigna el rango cuando el XP supera ESTRICTAMENTE el umbral,
     * evitando subir de rango antes de llenar completamente la barra de progreso.
     */
    public static MissionRank fromXp(int xp) {
        MissionRank result = NOVATO;
        for (MissionRank rank : values()) {
            // Solo asignar el rango si el XP es MAYOR al umbral (no igual)
            // Esto asegura que la barra de progreso llegue al 100% antes de subir
            if (xp > rank.getXpRequired()) {
                result = rank;
            } else {
                break;
            }
        }
        return result;
    }
    
    /**
     * Determina el rango según el nivel del jugador (MÉTODO PRINCIPAL)
     * Compara con levelRequired configurado en rangos.yml
     */
    public static MissionRank fromLevel(int level) {
        MissionRank result = NOVATO;
        for (MissionRank rank : values()) {
            // Solo asignar el rango si el nivel es MAYOR O IGUAL al requerido
            if (level >= rank.getLevelRequired()) {
                result = rank;
            } else {
                break;
            }
        }
        return result;
    }

    /**
     * Obtiene el siguiente rango (null si es el máximo)
     */
    public MissionRank getNext() {
        int nextOrdinal = this.ordinal() + 1;
        if (nextOrdinal >= values().length) {
            return null; // Ya es el máximo
        }
        return values()[nextOrdinal];
    }

    /**
     * @deprecated Usar getXpRequired() - Mantenido por compatibilidad
     */
    @Deprecated
    public int getMinPs() {
        return xpRequired;
    }

    /**
     * @deprecated Usar getXpRequired() - Mantenido por compatibilidad
     */
    @Deprecated
    public int getPsRequired() {
        return xpRequired;
    }

    /**
     * @deprecated No usar - Mantenido por compatibilidad
     */
    @Deprecated
    public int getMaxPs() {
        MissionRank next = getNext();
        return next != null ? next.getXpRequired() : Integer.MAX_VALUE;
    }
}
