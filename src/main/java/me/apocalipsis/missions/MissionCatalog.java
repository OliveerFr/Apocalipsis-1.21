package me.apocalipsis.missions;

import java.util.List;

public class MissionCatalog {

    private final String id;
    private final String nombre;
    private final MissionType tipo;
    private final String objetivo;
    private final int cantidad;
    private final MissionDifficulty dificultad;
    private final List<MissionRank> rangos;
    private final int recompensaPs;

    public MissionCatalog(String id, String nombre, MissionType tipo, String objetivo,
                         int cantidad, MissionDifficulty dificultad, List<MissionRank> rangos, int recompensaPs) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.objetivo = objetivo;
        this.cantidad = cantidad;
        this.dificultad = dificultad;
        this.rangos = rangos;
        this.recompensaPs = recompensaPs;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public MissionType getTipo() {
        return tipo;
    }

    public String getObjetivo() {
        return objetivo;
    }

    public int getCantidad() {
        return cantidad;
    }

    public MissionDifficulty getDificultad() {
        return dificultad;
    }

    public List<MissionRank> getRangos() {
        return rangos;
    }

    public int getRecompensaPs() {
        return recompensaPs;
    }

    public boolean isValidForRank(MissionRank rank) {
        return rangos.contains(rank);
    }
}
