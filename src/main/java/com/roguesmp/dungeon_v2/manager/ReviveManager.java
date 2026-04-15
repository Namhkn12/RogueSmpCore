package com.roguesmp.dungeon_v2.manager;

import com.roguesmp.dungeon_v2.data.runtime.DeadEntry;
import com.roguesmp.dungeon_v2.presentation.presenter.RevivePointPresenter;
import com.roguesmp.dungeon_v2.task.TaskScheduler;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chịu trách nhiệm quản lý và thao tác dữ liệu revive.
 * Không chứa business logic — chỉ là nơi lưu trữ và cung cấp data.
 */
public class ReviveManager {

    private static final long REVIVE_POINT_PERIOD_TICKS = 1L;

    private final Map<UUID, DeadEntry> deadEntries = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar>   bossBars    = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> revivePointTasks = new ConcurrentHashMap<>();
    private final Map<UUID, ItemDisplay> revivePointDisplays = new ConcurrentHashMap<>();
    private final TaskScheduler taskScheduler;
    private final RevivePointPresenter revivePointPresenter;
    private BukkitTask task;

    public ReviveManager(TaskScheduler taskScheduler, RevivePointPresenter revivePointPresenter) {
        this.taskScheduler = taskScheduler;
        this.revivePointPresenter = revivePointPresenter;
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
        if (player == null) return;
        addEntry(player, player.getLocation());
    }

    public void addEntry(Player player, Location deathLocation) {
        if (player == null) return;
        UUID deadUUID = player.getUniqueId();
        Location spawnLocation = resolveDeathLocation(player, deathLocation);

        deadEntries.computeIfAbsent(deadUUID, ignored -> new DeadEntry(deadUUID, spawnLocation));
        DeadEntry entry = deadEntries.get(deadUUID);
        if (entry == null || entry.getDeathLocation() == null) return;

        startOrReplaceRevivePoint(deadUUID, player, entry.getDeathLocation());
    }

    public void removeEntry(UUID uuid) {
        if (uuid == null) return;
        deadEntries.remove(uuid);
        stopRevivePoint(uuid);
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
        removeBossBar(deadUUID);
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
        new ArrayList<>(revivePointTasks.keySet()).forEach(this::stopRevivePoint);
        revivePointTasks.clear();
        revivePointDisplays.clear();
        bossBars.values().forEach(BossBar::removeAll);
        bossBars.clear();
        deadEntries.clear();
    }

    public void shutdown() {
        stopTask();
        clear();
    }

    private Location resolveDeathLocation(Player player, Location fallbackLocation) {
        if (fallbackLocation != null && fallbackLocation.getWorld() != null) {
            return fallbackLocation.clone();
        }
        return player.getLocation().clone();
    }

    private void startOrReplaceRevivePoint(UUID deadUUID, Player deadPlayer, Location deathLocation) {
        stopRevivePoint(deadUUID);

        ItemDisplay display = revivePointPresenter.spawnRevivePoint(deadPlayer, deathLocation);
        if (display == null) return;

        revivePointDisplays.put(deadUUID, display);
        AtomicInteger tickRef = new AtomicInteger(0);

        BukkitTask reviveTask = taskScheduler.runTimerCancellable(0L, REVIVE_POINT_PERIOD_TICKS, () -> {
            if (display.isDead()) {
                stopRevivePoint(deadUUID);
                return;
            }
            revivePointPresenter.tickRevivePoint(display, deathLocation, tickRef.getAndIncrement());
        });

        revivePointTasks.put(deadUUID, reviveTask);
    }

    private void stopRevivePoint(UUID deadUUID) {
        BukkitTask reviveTask = revivePointTasks.remove(deadUUID);
        if (reviveTask != null && !reviveTask.isCancelled()) {
            reviveTask.cancel();
        }

        ItemDisplay display = revivePointDisplays.remove(deadUUID);
        revivePointPresenter.removeRevivePoint(display);
    }
}
