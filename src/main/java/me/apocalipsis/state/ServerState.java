package me.apocalipsis.state;

public enum ServerState {
    PREPARACION("§e§lPREPARACIÓN"),
    ACTIVO("§c§lACTIVO"),
    DETENIDO("§7§lDETENIDO"),
    SAFE_MODE("§6§lSAFE MODE");

    private final String display;

    ServerState(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
