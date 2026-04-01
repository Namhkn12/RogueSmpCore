package com.roguesmp.dungeon_v2.data.runtime;

import com.roguesmp.dungeon_v2.data.runtime.session.DungeonProgress;
import com.roguesmp.dungeon_v2.data.runtime.session.DungeonSession;
import com.roguesmp.dungeon_v2.data.runtime.session.DungeonTimer;

/**
 * Root runtime aggregate for a single active dungeon run.
 */
public class DungeonInstance {
    private RegionInstance region;
    private DungeonSession session;
    private DungeonProgress progress;
    private DungeonTimer timer;

    public DungeonInstance() {
    }

    public DungeonInstance(RegionInstance region, DungeonSession session, DungeonProgress progress, DungeonTimer timer) {
        this.region = region;
        this.session = session;
        this.progress = progress;
        this.timer = timer;
    }

    public RegionInstance getRegion() {
        return region;
    }

    public void setRegion(RegionInstance region) {
        this.region = region;
    }

    public DungeonSession getSession() {
        return session;
    }

    public void setSession(DungeonSession session) {
        this.session = session;
    }

    public DungeonProgress getProgress() {
        return progress;
    }

    public void setProgress(DungeonProgress progress) {
        this.progress = progress;
    }

    public DungeonTimer getTimer() {
        return timer;
    }

    public void setTimer(DungeonTimer timer) {
        this.timer = timer;
    }
}
