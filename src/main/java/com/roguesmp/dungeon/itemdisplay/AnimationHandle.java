package com.roguesmp.dungeon.itemdisplay;

import org.bukkit.scheduler.BukkitTask;

public class AnimationHandle {

    private final Runnable cancelFn;
    private BukkitTask task;
    private boolean cancelled = false;

    public AnimationHandle(Runnable cancelFn) {
        this.cancelFn = cancelFn;
    }

    public void attachTask(BukkitTask task) {
        this.task = task;
    }

    public void cancel() {
        if (!cancelled) {
            cancelled = true;
            cancelFn.run();
            if (task != null && !task.isCancelled()) task.cancel();
        }
    }

    public boolean isCancelled() { return cancelled; }
}