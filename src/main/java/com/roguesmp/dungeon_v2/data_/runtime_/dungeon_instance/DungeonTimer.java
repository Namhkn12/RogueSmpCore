package com.roguesmp.dungeon_v2.data_.runtime_.dungeon_instance;

/**
 * Quản lý vòng đời thời gian của một dungeon run.
 * durationMs được truyền vào từ {@code Dungeon.playTime} (phút) bởi tầng service,
 * không hardcode ở đây.
 */
public class DungeonTimer {

    private long startTime;
    private long endTime;
    private boolean paused;
    private long pausedAt;

    /** No-arg constructor cho Gson / deserialization. */
    public DungeonTimer() {}

    // ── Factory ───────────────────────────────────────────────────────────────

    /**
     * Tạo timer bắt đầu ngay lập tức.
     *
     * @param durationMs thời lượng tính bằng ms — lấy từ {@code Dungeon.playTime * 60_000L}
     */
    public static DungeonTimer startNow(long durationMs) {
        DungeonTimer t = new DungeonTimer();
        t.startTime = System.currentTimeMillis();
        t.endTime   = t.startTime + durationMs;
        t.paused    = false;
        return t;
    }

    // ── Truy vấn ──────────────────────────────────────────────────────────────

    public boolean isExpired() {
        if (paused) return false;
        return System.currentTimeMillis() >= endTime;
    }

    public int getRemainingSeconds() {
        long base = paused ? pausedAt : System.currentTimeMillis();
        return (int) Math.max(0, (endTime - base) / 1000);
    }

    public boolean isPaused() { return paused; }

    // ── Điều khiển ────────────────────────────────────────────────────────────

    public void pause() {
        if (!paused) {
            paused   = true;
            pausedAt = System.currentTimeMillis();
        }
    }

    public void resume() {
        if (paused) {
            endTime += System.currentTimeMillis() - pausedAt;
            paused   = false;
        }
    }

    /** Bonus/penalty time. Truyền số âm để trừ thời gian. */
    public void addTime(long milliseconds) {
        endTime += milliseconds;
    }

    // ── Getters / Setters (cho Gson) ───────────────────────────────────────────

    public long getStartTime()              { return startTime; }
    public void setStartTime(long v)        { this.startTime = v; }
    public long getEndTime()                { return endTime; }
    public void setEndTime(long v)          { this.endTime = v; }
    public long getPausedAt()               { return pausedAt; }
    public void setPausedAt(long v)         { this.pausedAt = v; }
    public void setPaused(boolean v)        { this.paused = v; }
}