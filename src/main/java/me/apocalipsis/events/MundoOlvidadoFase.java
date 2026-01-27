package me.apocalipsis.events;

public enum MundoOlvidadoFase {
    INACTIVO,
    ACTO_1_NORMALIDAD,
    ACTO_2_RAREZAS,
    ACTO_3_INESTABILIDAD,
    ACTO_4_QUIEBRE,
    ACTO_5_REINICIO,
    ACTO_6_NUEVO_MUNDO,
    ACTO_7_COMPRENSION,
    ACTO_8_FRACTURA,
    ACTO_9_END_PERMANECE,
    ACTO_10_CIERRE,
    COMPLETADO;
    
    @Override
    public String toString() {
        return name().replace("_", " ");
    }
    
    public String getNombreDisplay() {
        switch (this) {
            case ACTO_1_NORMALIDAD: return "§a§lNormalidad";
            case ACTO_2_RAREZAS: return "§e§lPrimeras Rarezas";
            case ACTO_3_INESTABILIDAD: return "§6§lInestabilidad";
            case ACTO_4_QUIEBRE: return "§c§lEl Quiebre";
            case ACTO_5_REINICIO: return "§8§lEl Reinicio";
            case ACTO_6_NUEVO_MUNDO: return "§a§lNuevo Mundo";
            case ACTO_7_COMPRENSION: return "§b§lComprensión Lenta";
            case ACTO_8_FRACTURA: return "§c§lLa Fractura";
            case ACTO_9_END_PERMANECE: return "§5§lEl End Permanece";
            case ACTO_10_CIERRE: return "§8§lCierre";
            case COMPLETADO: return "§a§l✓ Completado";
            default: return "§7Inactivo";
        }
    }
}
