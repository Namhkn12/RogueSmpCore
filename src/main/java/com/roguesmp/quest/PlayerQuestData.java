package com.roguesmp.quest;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represent progress data for all player quest.
 */
public class PlayerQuestData {
    private final UUID uuid;
    private final Map<String, QuestProgress> progresses; //String is quest id
    private final Map<String, Integer> dailyCompletions = new HashMap<>();
    private long lastDailyCompletionResetTimestamp = -1;

    public PlayerQuestData(UUID uuid, Map<String, QuestProgress> progresses, Map<String, Integer> dailyCompletions, long lastDailyCompletionResetTimestamp) {
        this.uuid = uuid;
        this.progresses = progresses;
        this.dailyCompletions.putAll(dailyCompletions) ;
        this.lastDailyCompletionResetTimestamp = lastDailyCompletionResetTimestamp;
    }

    public PlayerQuestData(UUID uuid) {
        this.uuid = uuid;
        this.progresses = new HashMap<>();
    }

    public UUID getUuid() {
        return uuid;
    }

    public @Nullable Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public @Unmodifiable Map<String, QuestProgress> getQuestProgresses() {
        return Collections.unmodifiableMap(progresses);
    }

    public @Nullable QuestProgress getQuestProgress(String questId) {
        return progresses.get(questId);
    }

    public void setQuestProgress(String questId, QuestProgress progress) {
        progresses.put(questId, progress);
    }

    public @Nullable QuestProgress removeQuestProgress(String questId) {
        return progresses.remove(questId);
    }

    public int getDailyCompletionsForTag(String tagId) {
        return dailyCompletions.getOrDefault(tagId, 0);
    }

    public void incrementDailyCompletionForTag(String tagId) {
        dailyCompletions.put(tagId, getDailyCompletionsForTag(tagId) + 1);
    }

    public void clearDailyCompletions() {
        dailyCompletions.clear();
    }

    public long getLastDailyCompletionResetTimestamp() {
        return lastDailyCompletionResetTimestamp;
    }

    public void setLastDailyCompletionResetTimestamp(long timestamp) {
        this.lastDailyCompletionResetTimestamp = timestamp;
    }
}
