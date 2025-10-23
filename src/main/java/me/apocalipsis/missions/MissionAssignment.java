package me.apocalipsis.missions;

public class MissionAssignment {

    private final MissionCatalog mission;
    private int progress;
    private boolean completed;
    private boolean failed;

    public MissionAssignment(MissionCatalog mission) {
        this.mission = mission;
        this.progress = 0;
        this.completed = false;
        this.failed = false;
    }

    public MissionCatalog getMission() {
        return mission;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = Math.min(progress, mission.getCantidad());
        if (this.progress >= mission.getCantidad()) {
            this.completed = true;
        }
    }

    public void addProgress(int amount) {
        setProgress(progress + amount);
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public double getProgressPercent() {
        return (double) progress / mission.getCantidad();
    }
}
