package me.apocalipsis.missions;

public enum MissionType {
    MATAR,
    ROMPER,
    CRAFTEAR,
    COCINAR,
    CONSUMIR,
    EXPLORAR,  // DESHABILITADO: ya no se asigna ni trackea
    ALTURA;    // DESHABILITADO: ya no se asigna ni trackea

    /**
     * [REMOVAL] Verifica si este tipo de misión está habilitado
     * EXPLORAR y ALTURA están permanentemente deshabilitados
     */
    public boolean isEnabled() {
        return this != EXPLORAR && this != ALTURA;
    }
}
