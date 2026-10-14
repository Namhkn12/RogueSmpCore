package com.roguesmp.quest.data;

import com.roguesmp.quest.QuestProgress;
import com.roguesmp.quest.holder.PlayerHolder;
import com.roguesmp.codec.Codec;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represent progress data for all player quest.
 */
public class PlayerQuestData extends QuestLog {

    public static final Codec<PlayerQuestData> CODEC = Codec.composite(
            Codec.UUID.fieldOf("uuid").forGetter(PlayerQuestData::getUuid),
            Codec.lenientUnboundedMap(QuestProgress.CODEC).optionalFieldOf("questProgress", new HashMap<>()).forGetter(PlayerQuestData::getQuestProgresses),
            Codec.unboundedMap(Codec.INT).optionalFieldOf("dailyCompletions", new HashMap<>()).forGetter(PlayerQuestData::getDailyCompletions),
            Codec.LONG.optionalFieldOf("lastDailyCompletionResetTimestamp", -1L).forGetter(PlayerQuestData::getLastDailyCompletionResetTimestamp),
            Codec.listOf(PendingQuestReward.CODEC).optionalFieldOf("pendingQuestRewards", List.of()).forGetter(PlayerQuestData::getPendingQuestRewards),
            PlayerQuestData::new
    );

    private final UUID uuid;
    private final PlayerHolder holder;
    private final Map<String, Integer> dailyCompletions = new HashMap<>();
    private long lastDailyCompletionResetTimestamp = -1;
    private final List<PendingQuestReward> pendingQuestRewards = new ArrayList<>();

    /**
     * Reconstruction constructor used when decoding from storage (see {@link #CODEC}).
     */
    public PlayerQuestData(UUID uuid, Map<String, QuestProgress> progresses, Map<String, Integer> dailyCompletions, long lastDailyCompletionResetTimestamp, List<PendingQuestReward> pendingQuestRewards) {
        this.uuid = uuid;
        this.holder = new PlayerHolder(uuid);
        this.progresses.putAll(progresses);
        this.dailyCompletions.putAll(dailyCompletions) ;
        this.lastDailyCompletionResetTimestamp = lastDailyCompletionResetTimestamp;
        this.pendingQuestRewards.addAll(pendingQuestRewards);
        this.dirty = false;
    }

    public PlayerQuestData(PlayerQuestData toClone) {
        this.uuid = toClone.getUuid();
        this.holder = toClone.holder;
        this.progresses.putAll(toClone.getQuestProgresses());
        this.dailyCompletions.putAll(toClone.getDailyCompletions());
        this.lastDailyCompletionResetTimestamp = toClone.getLastDailyCompletionResetTimestamp();
        this.pendingQuestRewards.addAll(toClone.pendingQuestRewards);
    }

    public PlayerQuestData(UUID uuid) {
        this.uuid = uuid;
        this.holder = new PlayerHolder(uuid);
    }

    @Override
    public PlayerHolder getHolder() {
        return holder;
    }

    public UUID getUuid() {
        return uuid;
    }

    public @Nullable Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public @Unmodifiable Map<String, Integer> getDailyCompletions() {
        return Collections.unmodifiableMap(dailyCompletions);
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

    public @Unmodifiable List<PendingQuestReward> getPendingQuestRewards() {
        return Collections.unmodifiableList(pendingQuestRewards);
    }

    public void addPendingQuestReward(PendingQuestReward reward) {
        if (pendingQuestRewards.stream().anyMatch(pending -> pending.matches(reward.source(), reward.questId()))) return;
        pendingQuestRewards.add(reward);
        this.dirty = true;
    }

    public boolean hasPendingQuestReward(UUID source, String questId) {
        return pendingQuestRewards.stream().anyMatch(reward -> reward.matches(source, questId));
    }

    public @Nullable PendingQuestReward removePendingQuestReward(UUID source, String questId) {
        for (Iterator<PendingQuestReward> iterator = pendingQuestRewards.iterator(); iterator.hasNext(); ) {
            PendingQuestReward reward = iterator.next();
            if (!reward.matches(source, questId)) continue;
            iterator.remove();
            this.dirty = true;
            return reward;
        }
        return null;
    }

    public void expirePendingQuestRewards(long now) {
        if (pendingQuestRewards.removeIf(reward -> reward.expiresAt() <= now)) this.dirty = true;
    }

}
