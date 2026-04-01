package com.roguesmp.dungeon_v2.data.runtime.session;

/**
 * Time budget of a dungeon run.
 */
public class DungeonTimer {

    private long startTime;
    private long endTime;
    private boolean paused;
    private long pausedAt;

    public DungeonTimer() {
    }

    public static DungeonTimer startNow(long durationMs) {
        DungeonTimer timer = new DungeonTimer();
        timer.startTime = System.currentTimeMillis();
        timer.endTime = timer.startTime + durationMs;
        timer.paused = false;
        return timer;
    }

    public boolean isExpired() {
        if (paused) {
            return false;
        }
        return System.currentTimeMillis() >= endTime;
    }

    public int getRemainingSeconds() {
        long base = paused ? pausedAt : System.currentTimeMillis();
        return (int) Math.max(0, (endTime - base) / 1000);
    }

    public boolean isPaused() {
        return paused;
    }

    public void pause() {
        if (!paused) {
            paused = true;
            pausedAt = System.currentTimeMillis();
        }
    }

    public void resume() {
        if (paused) {
            endTime += System.currentTimeMillis() - pausedAt;
            paused = false;
        }
    }

    public void addTime(long milliseconds) {
        endTime += milliseconds;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public long getPausedAt() {
        return pausedAt;
    }

    public void setPausedAt(long pausedAt) {
        this.pausedAt = pausedAt;
    }
}
