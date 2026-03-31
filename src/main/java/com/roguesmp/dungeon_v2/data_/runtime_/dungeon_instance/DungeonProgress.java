package com.roguesmp.dungeon_v2.data_.runtime_.dungeon_instance;

/**
 * Tiến trình gameplay của một dungeon run.
 * {@code minimumRooms} được inject từ {@code Dungeon.minium} khi khởi tạo
 * để {@link #isBossUnlocked()} không cần lookup ngược lại Dungeon.
 */
public class DungeonProgress {

    public enum Status { IN_PROGRESS, COMPLETED, FAILED, ABANDONED }

    private int    clearedRooms;
    private int    minimumRooms;   // snapshot từ Dungeon.minium
    private String currentRoomId;
    private Status status;

    public DungeonProgress() {}

    public static DungeonProgress create(int minimumRooms) {
        DungeonProgress p = new DungeonProgress();
        p.clearedRooms  = 0;
        p.minimumRooms  = minimumRooms;
        p.status        = Status.IN_PROGRESS;
        return p;
    }

    // ── Logic ─────────────────────────────────────────────────────────────────

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

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public int    getClearedRooms()              { return clearedRooms; }
    public void   setClearedRooms(int v)         { this.clearedRooms = v; }
    public int    getMinimumRooms()              { return minimumRooms; }
    public void   setMinimumRooms(int v)         { this.minimumRooms = v; }
    public String getCurrentRoomId()             { return currentRoomId; }
    public void   setCurrentRoomId(String v)     { this.currentRoomId = v; }
    public Status getStatus()                    { return status; }
    public void   setStatus(Status v)            { this.status = v; }
}