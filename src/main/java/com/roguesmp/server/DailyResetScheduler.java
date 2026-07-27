package com.roguesmp.server;

import com.roguesmp.GlobalConfig;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.event.DailyResetEvent;
import com.roguesmp.utils.DateUtils;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.LocalDate;

public class DailyResetScheduler {

    private final RogueSmpCore plugin;
    private final GlobalConfig config;
    private BukkitTask task;

    public DailyResetScheduler(RogueSmpCore plugin) {
        this.plugin = plugin;
        this.config = RogueSmpCore.getGlobalConfig();
    }

    public void start() {
        // 1. Check if a reset was missed while the server was offline/restarting
        checkAndExecuteMissedReset();

        // 2. Schedule the next reset timer
        scheduleNextReset();
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    private void checkAndExecuteMissedReset() {
        LocalDate today = DateUtils.today();
        long todayEpochDay = today.toEpochDay();

        // If stored reset day is behind today's epoch day, trigger reset immediately
        if (config.getLastDailyReset() < todayEpochDay) {
            triggerReset(today);
        }
    }

    private void scheduleNextReset() {
        Duration duration = DateUtils.getDurationUntilNextReset();
        long ticksUntilReset = Math.max(1L, duration.getSeconds() * 20L);

        task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            triggerReset(DateUtils.today());
            scheduleNextReset();
        }, ticksUntilReset);
    }

    private void triggerReset(LocalDate date) {
        // Fire event
        DailyResetEvent resetEvent = new DailyResetEvent(date, DateUtils.ZONE_VN);
        Bukkit.getPluginManager().callEvent(resetEvent);

        // Update config with the epoch day of the reset and save to file
        config.setLastDailyReset(date.toEpochDay());
        Utils.runAsync(() -> config.save(plugin));
    }
}
