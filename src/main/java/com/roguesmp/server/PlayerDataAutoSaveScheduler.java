package com.roguesmp.server;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.island.IslandData;
import com.roguesmp.island.IslandManager;
import com.roguesmp.player.PlayerData;
import com.roguesmp.player.PlayerManager;
import com.roguesmp.quest.PlayerQuestData;
import com.roguesmp.quest.QuestManager;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Periodically flushes dirty (unsaved) player/island/quest data that's currently cached in
 * memory (mostly online-player-linked), so a crash or ungraceful shutdown loses at most one
 * interval's worth of progress instead of everything since the last quit/plugin-disable save.
 */
public class PlayerDataAutoSaveScheduler {

    private static final long INTERVAL_MINUTES = 5;
    private static final long INTERVAL_TICKS = INTERVAL_MINUTES * 60 * 20;

    private final RogueSmpCore plugin;
    private BukkitTask task;

    public PlayerDataAutoSaveScheduler(RogueSmpCore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::runAutoSave, INTERVAL_TICKS, INTERVAL_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    // Runs on the main thread: encodes each dirty value immediately (safe - matches how they're
    // mutated elsewhere) and hands one batched transaction per store to the async writers.
    private void runAutoSave() {
        List<PlayerData> dirtyPlayers = new ArrayList<>();
        for (PlayerData data : PlayerManager.getInstance().getDataManager().getCachedPlayerData()) {
            if (data.isDirty()) dirtyPlayers.add(data);
        }

        List<IslandData> dirtyIslands = new ArrayList<>();
        for (IslandData data : IslandManager.getInstance().getIslandDataManager().getIslandDataCache().values()) {
            if (data.isDirty()) dirtyIslands.add(data);
        }

        List<PlayerQuestData> dirtyQuests = new ArrayList<>();
        for (PlayerQuestData data : QuestManager.getInstance().getQuestDataManager().getCachedQuestData()) {
            if (data.isDirty()) dirtyQuests.add(data);
        }

        int total = dirtyPlayers.size() + dirtyIslands.size() + dirtyQuests.size();
        if (total == 0) {
            return;
        }

        RogueSmpCore.LOGGER.info("[AutoSave] Saving {} dirty entrie(s) ({} player(s), {} island(s), {} quest profile(s))...",
                total, dirtyPlayers.size(), dirtyIslands.size(), dirtyQuests.size());

        for (var player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Utils.fromString("<yellow><bold>[!]</bold> <gray>Đang lưu dữ liệu server..."));
        }

        CompletableFuture.allOf(
                PlayerManager.getInstance().getDataManager().saveAllAsync(dirtyPlayers),
                IslandManager.getInstance().getIslandDataManager().saveAllAsync(dirtyIslands),
                QuestManager.getInstance().getQuestDataManager().saveAllAsync(dirtyQuests)
        ).thenRun(() -> RogueSmpCore.LOGGER.info("[AutoSave] Done."));
    }
}
