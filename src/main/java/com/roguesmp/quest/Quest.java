package com.roguesmp.quest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

/**
 * Represents the definition for a quest.
 */
public class Quest {
    private final String id;
    private final String name;
    private final List<String> description;
    private final List<QuestRequirement> requirements;
    private final Map<String, QuestObjective> objectives;
    private final List<QuestReward> rewards;

    public Quest(String id, String name, @NotNull List<String> description, List<QuestRequirement> requirements, @NotNull Map<String, QuestObjective> objectives, @NotNull List<QuestReward> rewards) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.requirements = requirements;
        this.objectives = objectives;
        this.rewards = rewards;
    }

    public @Nullable QuestObjective getObjective(String objectiveId) {
        return objectives.get(objectiveId);
    }

    public String getId() {
        return id;
    }

    public @Nullable String getName() {
        return name;
    }

    public @Unmodifiable List<String> getDescription() {
        return Collections.unmodifiableList(description);
    }

    public @Unmodifiable List<QuestReward> getRewards() {
        return Collections.unmodifiableList(rewards);
    }

    public @Unmodifiable List<QuestRequirement> getRequirements() {
        return Collections.unmodifiableList(requirements);
    }

    public @Unmodifiable Map<String, QuestObjective> getObjectives() {
        return Collections.unmodifiableMap(objectives);
    }
}
