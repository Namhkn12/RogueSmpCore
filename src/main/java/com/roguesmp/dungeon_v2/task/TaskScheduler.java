package com.roguesmp.dungeon_v2.task;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class TaskScheduler {

    private static TaskScheduler instance;

    private final Plugin plugin;

    private TaskScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    public static void init(Plugin plugin) {
        if (instance == null) {
            instance = new TaskScheduler(plugin);
        }
    }

    public static TaskScheduler getInstance() {
        if (instance == null) {
            throw new IllegalStateException("TaskScheduler not initialized.");
        }
        return instance;
    }

    /**
     * Run task later (sync)
     */
    public void runLater(long delayTicks, Runnable task) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    /**
     * Run task immediately (sync)
     */
    public void runNow(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    /**
     * Run async later
     */
    public void runAsyncLater(long delayTicks, Runnable task) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
    }

    /**
     * Run repeating task
     */
    public void runTimer(long delayTicks, long periodTicks, Runnable task) {
        Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
    }

    public BukkitTask runTimerCancellable(long delayTicks, long periodTicks, Runnable task) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
    }

    public BukkitTask runLaterCancellable(long delayTicks, Runnable task) {
        return Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }
}