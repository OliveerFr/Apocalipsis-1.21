package me.apocalipsis.experience;

/**
 * Enum que define todas las fuentes de XP del sistema
 */
public enum XPSource {
    // Combate
    KILL_HOSTILE("Matar Hostil", "matar_mobs.hostiles", "⚔"),
    KILL_PASSIVE("Matar Animal", "matar_mobs.pasivos", "🐄"),
    KILL_BOSS("Matar Jefe", "matar_mobs.jefes", "💀"),
    KILL_PLAYER("Matar Jugador", "matar_mobs.jugadores", "☠"),
    
    // Minería
    MINE_COMMON("Minar Común", "minar.comun", "⛏"),
    MINE_RARE("Minar Raro", "minar.raro", "💎"),
    MINE_EPIC("Minar Épico", "minar.epico", "✨"),
    
    // Recolección
    HARVEST("Cosechar", "cosechar", "🌾"),
    FISH("Pescar", "pescar", "🎣"),
    FISH_TREASURE("Pescar Tesoro", "pescar.tesoro", "🏆"),
    SHEAR("Esquilar", "esquilar", "✂"),
    
    // Animales
    TAME("Domar", "domar", "❤"),
    BREED("Criar", "criar", "🐣"),
    
    // Crafteo y mejoras
    CRAFT_COMMON("Craftear Común", "craftear.comun", "🔨"),
    CRAFT_RARE("Craftear Raro", "craftear.raro", "⚒"),
    CRAFT_EPIC("Craftear Épico", "craftear.epico", "🛡"),
    ENCHANT("Encantar", "encantar", "✦"),
    SMITH("Mejorar Smithing", "smithing", "🔥"),
    BREW("Pociones", "pociones", "🧪"),
    
    // Comercio
    TRADE("Comerciar", "comerciar", "💰"),
    TRADE_RARE("Comercio Raro", "comerciar.raro", "💎"),
    
    // Fundición
    SMELT("Fundir", "fundir", "🔥"),
    
    // Exploración
    ADVANCEMENT("Logro", "logros", "⭐"),
    ADVANCEMENT_RARE("Logro Raro", "logros.raro", "🌟"),
    ADVANCEMENT_EPIC("Logro Épico", "logros.epico", "✨"),
    BIOME_DISCOVER("Descubrir Bioma", "explorar.bioma", "🗺"),
    STRUCTURE_DISCOVER("Descubrir Estructura", "explorar.estructura", "🏛"),
    
    // Construcción
    PLACE_SPECIAL("Colocar Especial", "colocar", "🏗"),
    
    // Consumibles
    CONSUME_SPECIAL("Consumir Especial", "consumir", "🍎"),
    
    // Misiones
    MISSION_EASY("Misión Fácil", "misiones.facil", "📜"),
    MISSION_MEDIUM("Misión Media", "misiones.media", "📋"),
    MISSION_HARD("Misión Difícil", "misiones.dificil", "📕"),
    
    // Eventos especiales
    EVENT_PARTICIPATION("Participar Evento", "eventos", "🎉"),
    EVENT_WIN("Ganar Evento", "eventos.victoria", "🏆"),
    
    // Bonus
    STREAK_BONUS("Racha Diaria", "bonus.racha", "🔥"),
    FIRST_OF_DAY("Primero del Día", "bonus.primer_dia", "☀"),
    WEEKEND_BONUS("Bonus Fin de Semana", "bonus.finde", "🎊");
    
    private final String displayName;
    private final String configPath;
    private final String icon;
    
    XPSource(String displayName, String configPath, String icon) {
        this.displayName = displayName;
        this.configPath = configPath;
        this.icon = icon;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getConfigPath() {
        return "fuentes_xp." + configPath;
    }
    
    public String getIcon() {
        return icon;
    }
    
    /**
     * Obtiene la categoría de la fuente para agrupación
     */
    public String getCategory() {
        if (name().startsWith("KILL")) return "Combate";
        if (name().startsWith("MINE")) return "Minería";
        if (name().startsWith("HARVEST") || name().startsWith("FISH") || name().startsWith("SHEAR")) return "Recolección";
        if (name().startsWith("TAME") || name().startsWith("BREED")) return "Animales";
        if (name().startsWith("CRAFT") || name().startsWith("ENCHANT") || name().startsWith("SMITH") || name().startsWith("BREW")) return "Crafteo";
        if (name().startsWith("TRADE")) return "Comercio";
        if (name().startsWith("ADVANCEMENT") || name().startsWith("BIOME") || name().startsWith("STRUCTURE")) return "Exploración";
        if (name().startsWith("MISSION")) return "Misiones";
        if (name().startsWith("EVENT")) return "Eventos";
        if (name().startsWith("STREAK") || name().startsWith("FIRST") || name().startsWith("WEEKEND")) return "Bonus";
        return "Otros";
    }
    
    /**
     * [LEYENDA+] Determina si esta fuente de XP es "pasiva" (no misiones).
     * Los rangos LEYENDA+ solo ganan XP por misiones, no por fuentes pasivas.
     * 
     * Fuentes NO pasivas (siempre dan XP):
     * - MISSION_* (misiones)
     * - EVENT_* (eventos del servidor)
     * - ADVANCEMENT_* (logros importantes)
     * 
     * Fuentes pasivas (bloqueadas para LEYENDA+):
     * - Matar mobs, minar, craftear, pescar, etc.
     */
    public boolean isPassive() {
        // Misiones NUNCA son pasivas - siempre dan XP
        if (name().startsWith("MISSION")) return false;
        
        // Eventos del servidor tampoco son pasivos
        if (name().startsWith("EVENT")) return false;
        
        // Logros importantes no son pasivos
        if (name().startsWith("ADVANCEMENT")) return false;
        
        // Todo lo demás es pasivo (minar, matar, craftear, pescar, etc.)
        return true;
    }
}
