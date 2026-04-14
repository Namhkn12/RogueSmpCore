package com.roguesmp.dungeon_v2.manager;

import com.roguesmp.dungeon_v2.data.runtime.DeadEntry;
import com.roguesmp.dungeon_v2.task.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chịu trách nhiệm quản lý và thao tác dữ liệu revive.
 * Không chứa business logic — chỉ là nơi lưu trữ và cung cấp data.
 */
public class ReviveManager {

    private final Map<UUID, DeadEntry> deadEntries = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar>   bossBars    = new ConcurrentHashMap<>();
    private final TaskScheduler taskScheduler;
    private BukkitTask task;

    public ReviveManager(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    public void startTask(Runnable tickLogic) {
        task = taskScheduler.runTimerCancellable(0L, 20L, tickLogic);
    }

    public void stopTask() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    public void addEntry(Player player) {
        deadEntries.put(player.getUniqueId(), new DeadEntry(player));
    }

    public void removeEntry(UUID uuid) {
        deadEntries.remove(uuid);
    }

    public Optional<DeadEntry> getEntry(UUID uuid) {
        return Optional.ofNullable(deadEntries.get(uuid));
    }

    public List<DeadEntry> getAllEntries() {
        return new ArrayList<>(deadEntries.values());
    }

    public boolean hasEntry(UUID uuid) {
        return deadEntries.containsKey(uuid);
    }


    public void createBossBar(UUID deadUUID, Player deadPlayer) {
        BossBar bar = Bukkit.createBossBar("Waiting for rescue...", BarColor.RED, BarStyle.SOLID);
        bar.addPlayer(deadPlayer);
        bar.setProgress(1.0);
        bossBars.put(deadUUID, bar);
    }

    public void addViewerToBossBar(UUID deadUUID, Player rescuer) {
        BossBar bar = bossBars.get(deadUUID);
        if (bar == null) return;
        if (!bar.getPlayers().contains(rescuer)) {
            bar.addPlayer(rescuer);
        }
    }

    public void removeRescuerFromBossBar(UUID deadUUID, Player rescuer) {
        BossBar bar = bossBars.get(deadUUID);
        if (bar == null) return;
        bar.removePlayer(rescuer);
    }

    public void clearRescuersFromBossBar(UUID deadUUID, Player deadPlayer) {
        BossBar bar = bossBars.get(deadUUID);
        if (bar == null) return;
        new ArrayList<>(bar.getPlayers()).forEach(p -> {
            if (!p.equals(deadPlayer)) bar.removePlayer(p);
        });
    }

    public void updateBossBar(UUID deadUUID, String title, double progress, BarColor color) {
        BossBar bar = bossBars.get(deadUUID);
        if (bar == null) return;
        bar.setTitle(title);
        bar.setProgress(Math.clamp(progress, 0.0, 1.0)); // clamp 0.0–1.0
        bar.setColor(color);
    }

    public void removeBossBar(UUID deadUUID) {
        BossBar bar = bossBars.remove(deadUUID);
        if (bar != null) bar.removeAll();
    }

    public Optional<BossBar> getBossBar(UUID deadUUID) {
        return Optional.ofNullable(bossBars.get(deadUUID));
    }


    public void clear() {
        bossBars.values().forEach(BossBar::removeAll);
        bossBars.clear();
        deadEntries.clear();
    }
}