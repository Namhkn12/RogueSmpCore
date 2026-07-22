package com.roguesmp.server;

import com.roguesmp.event.DailyResetEvent;
import com.roguesmp.utils.DateUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;

public class DailyResetScheduler {

    private final JavaPlugin plugin;
    private BukkitTask task;

    public DailyResetScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        scheduleNextReset();
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    private void scheduleNextReset() {
        Duration duration = DateUtils.getDurationUntilNextReset();
        long ticksUntilReset = Math.max(1L, duration.getSeconds() * 20L);

        task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            DailyResetEvent resetEvent = new DailyResetEvent(DateUtils.today(), DateUtils.ZONE_VN);
            Bukkit.getPluginManager().callEvent(resetEvent);

            scheduleNextReset();
        }, ticksUntilReset);
    }
}
