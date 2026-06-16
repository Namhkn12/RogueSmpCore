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

    public PlayerQuestData(UUID uuid, Map<String, QuestProgress> progresses) {
        this.uuid = uuid;
        this.progresses = progresses;
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

    public void addQuestProgress(String questId, QuestProgress progress) {
        progresses.put(questId, progress);
    }
}
