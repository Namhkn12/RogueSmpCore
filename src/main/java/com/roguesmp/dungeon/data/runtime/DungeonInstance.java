package com.roguesmp.dungeon.data.runtime;

import com.roguesmp.dungeon.data.runtime.session.DungeonPlayer;
import com.roguesmp.dungeon.data.runtime.session.DungeonProgress;
import com.roguesmp.dungeon.data.runtime.session.DungeonSession;
import com.roguesmp.dungeon.data.runtime.session.DungeonTimer;

/**
 * Root runtime aggregate for a single active dungeon run.
 */
public class DungeonInstance {
    private DungeonSession session;
    private DungeonProgress progress;
    private DungeonPlayer players;
    private DungeonTimer timer;

    public DungeonInstance() {
    }

    public DungeonInstance(DungeonSession session, DungeonProgress progress, DungeonPlayer players, DungeonTimer timer) {
        this.session = session;
        this.progress = progress;
        this.players = players;
        this.timer = timer;
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

    public DungeonPlayer getDungeonPlayers() {
        return players;
    }

    public void setPlayers(DungeonPlayer players) {
        this.players = players;
    }
}
