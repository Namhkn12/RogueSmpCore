package com.roguesmp.dungeon_v2.data.runtime;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DeadEntry {

    private final UUID deadID;
    private final Location deathLocation;

    private int progressTicks = 0;

    public static final int REVIVE_THRESHOLD_TICKS = 600;

    public static final int DECAY_PER_TICK = 20;

    public DeadEntry(Player deadPlayer){
        this.deadID = deadPlayer.getUniqueId();
        this.deathLocation = deadPlayer.getLocation().clone();
    }

    public UUID getDeadID(){
        return deadID;
    }

    public Location getDeathLocation() {
        return deathLocation;
    }

    public int getProgressTicks() {
        return progressTicks;
    }

    /** Returns progress as a 0.0–1.0 float for UI display. */
    public float getProgressPercent() {
        return (float) progressTicks / REVIVE_THRESHOLD_TICKS;
    }

    /** Called each task tick when a valid rescuer is present. */
    public void incrementProgress(int amount) {
        progressTicks = Math.min(progressTicks + amount, REVIVE_THRESHOLD_TICKS);
    }

    /** Called each task tick when NO rescuer is present — decays progress. */
    public void decayProgress() {
        progressTicks = Math.max(0, progressTicks - DECAY_PER_TICK);
    }

    public boolean isReviveComplete() {
        return progressTicks >= REVIVE_THRESHOLD_TICKS;
    }
}
