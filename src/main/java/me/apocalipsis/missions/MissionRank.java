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
    private int xpRequired;  // Umbral acumulado desde rangos.yml
    private int misionesDiarias;
    private String tabPrefix;
    private String tabSuffix;
    private String chatPrefix;
    private String scoreboardColor;
    
    // Valores por defecto hardcodeados (fallback si rangos.yml falla)
    private static final int[] DEFAULT_XP_REQUIRED = {0, 980, 3780, 8330, 14630, 22680, 32480, 44030};
    private static final int[] DEFAULT_MISIONES_DIARIAS = {10, 8, 6, 5, 4, 3, 3, 2};

    /**
     * Configura los datos de este rango desde rangos.yml
     */
    public void configure(String displayName, int xpRequired, int misionesDiarias, 
                         String tabPrefix, String tabSuffix, String chatPrefix, String scoreboardColor) {
        this.displayName = displayName;
        this.xpRequired = xpRequired;
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
     */
    public static MissionRank fromXp(int xp) {
        MissionRank result = NOVATO;
        for (MissionRank rank : values()) {
            if (xp >= rank.getXpRequired()) {
                result = rank;
            } else {
                break;
            }
        }
        return result;
    }
    
    /**
     * Determina el rango según el nivel de XP del jugador
     * Mapeo aproximado: cada 5 niveles = 1 rango
     */
    public static MissionRank fromLevel(int level) {
        if (level < 5) return NOVATO;           // Nivel 1-4
        if (level < 10) return EXPLORADOR;      // Nivel 5-9
        if (level < 15) return SOBREVIVIENTE;   // Nivel 10-14
        if (level < 20) return VETERANO;        // Nivel 15-19
        if (level < 25) return LEYENDA;         // Nivel 20-24
        if (level < 30) return MAESTRO;         // Nivel 25-29
        if (level < 35) return TITAN;           // Nivel 30-34
        return ABSOLUTO;                        // Nivel 35+
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
