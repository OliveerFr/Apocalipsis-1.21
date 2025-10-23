package me.apocalipsis.missions;

public enum MissionDifficulty {
    FACIL(3),
    MEDIA(2),
    DIFICIL(1);

    private final int weight;

    MissionDifficulty(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
