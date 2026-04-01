package com.roguesmp.dungeon_v2.data.runtime.session;

/**
 * Mutable gameplay progress of a dungeon run.
 */
public class DungeonProgress {

    public enum Status { IN_PROGRESS, COMPLETED, FAILED, ABANDONED }

    private int clearedRooms;
    private int minimumRooms;
    private String currentRoomId;
    private Status status;

    public DungeonProgress() {
    }

    public static DungeonProgress create(int minimumRooms) {
        DungeonProgress progress = new DungeonProgress();
        progress.clearedRooms = 0;
        progress.minimumRooms = minimumRooms;
        progress.status = Status.IN_PROGRESS;
        return progress;
    }

    public boolean isBossUnlocked() {
        return clearedRooms >= minimumRooms;
    }

    public boolean isFinished() {
        return status == Status.COMPLETED
                || status == Status.FAILED
                || status == Status.ABANDONED;
    }

    public void incrementClearedRooms() {
        clearedRooms++;
    }

    public int getClearedRooms() {
        return clearedRooms;
    }

    public void setClearedRooms(int clearedRooms) {
        this.clearedRooms = clearedRooms;
    }

    public int getMinimumRooms() {
        return minimumRooms;
    }

    public void setMinimumRooms(int minimumRooms) {
        this.minimumRooms = minimumRooms;
    }

    public String getCurrentRoomId() {
        return currentRoomId;
    }

    public void setCurrentRoomId(String currentRoomId) {
        this.currentRoomId = currentRoomId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
