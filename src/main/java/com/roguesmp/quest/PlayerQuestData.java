package com.roguesmp.quest;

import com.roguesmp.codec.Codec;
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

    public static final Codec<PlayerQuestData> CODEC = Codec.composite(
            Codec.UUID.fieldOf("uuid").forGetter(PlayerQuestData::getUuid),
            Codec.lenientUnboundedMap(QuestProgress.CODEC).optionalFieldOf("questProgress", new HashMap<>()).forGetter(PlayerQuestData::getQuestProgresses),
            Codec.unboundedMap(Codec.INT).optionalFieldOf("dailyCompletions", new HashMap<>()).forGetter(PlayerQuestData::getDailyCompletions),
            Codec.LONG.optionalFieldOf("lastDailyCompletionResetTimestamp", -1L).forGetter(PlayerQuestData::getLastDailyCompletionResetTimestamp),
            PlayerQuestData::new
    );

    private boolean dirty = true;

    private final UUID uuid;
    private final Map<String, QuestProgress> progresses = new HashMap<>(); //String is quest id
    private final Map<String, Integer> dailyCompletions = new HashMap<>();
    private long lastDailyCompletionResetTimestamp = -1;

    /**
     * Reconstruction constructor used when decoding from storage (see {@link #CODEC}).
     */
    public PlayerQuestData(UUID uuid, Map<String, QuestProgress> progresses, Map<String, Integer> dailyCompletions, long lastDailyCompletionResetTimestamp) {
        this.uuid = uuid;
        this.progresses.putAll(progresses);
        this.dailyCompletions.putAll(dailyCompletions) ;
        this.lastDailyCompletionResetTimestamp = lastDailyCompletionResetTimestamp;
        this.dirty = false;
    }

    public PlayerQuestData(PlayerQuestData toClone) {
        this.uuid = toClone.getUuid();
        this.progresses.putAll(toClone.getQuestProgresses());
        this.dailyCompletions.putAll(toClone.getDailyCompletions());
        this.lastDailyCompletionResetTimestamp = toClone.getLastDailyCompletionResetTimestamp();
    }

    public PlayerQuestData(UUID uuid) {
        this.uuid = uuid;
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

    public @Unmodifiable Map<String, Integer> getDailyCompletions() {
        return Collections.unmodifiableMap(dailyCompletions);
    }

    public @Nullable QuestProgress getQuestProgress(String questId) {
        return progresses.get(questId);
    }

    public void setQuestProgress(String questId, QuestProgress progress) {
        progresses.put(questId, progress);
        this.dirty = true;
    }

    public @Nullable QuestProgress removeQuestProgress(String questId) {
        this.dirty = true;
        return progresses.remove(questId);
    }

    public int getDailyCompletionsForTag(String tagId) {
        return dailyCompletions.getOrDefault(tagId, 0);
    }

    public void incrementDailyCompletionForTag(String tagId) {
        dailyCompletions.put(tagId, getDailyCompletionsForTag(tagId) + 1);
        this.dirty = true;
    }

    public void clearDailyCompletions() {
        dailyCompletions.clear();
        this.dirty = true;
    }

    public long getLastDailyCompletionResetTimestamp() {
        return lastDailyCompletionResetTimestamp;
    }

    public void setLastDailyCompletionResetTimestamp(long timestamp) {
        this.lastDailyCompletionResetTimestamp = timestamp;
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
