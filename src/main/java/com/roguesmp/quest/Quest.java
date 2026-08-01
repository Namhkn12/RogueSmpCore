package com.roguesmp.quest;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.Registry;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

/**
 * Represents the definition for a quest.
 */
public class Quest {

    public static final Codec<Quest> CODEC = Codec.composite(
            Codec.STRING.fieldOf("id").forGetter(Quest::getId),
            Codec.STRING.fieldOf("name").forGetter(Quest::getName),
            Codec.MATERIAL.optionalFieldOf("icon", Material.PAPER).forGetter(Quest::getIcon),
            Codec.listOf(Codec.STRING).optionalFieldOf("description", new ArrayList<>()).forGetter(Quest::getDescription),
            Codec.listOf(QuestRequirement.CODEC).optionalFieldOf("requirements", new ArrayList<>()).forGetter(Quest::getRequirements),
            Codec.unboundedMap(QuestObjective.CODEC).optionalFieldOf("objectives", new HashMap<>()).forGetter(Quest::getObjectives),
            Codec.listOf(QuestReward.CODEC).fieldOf("rewards").forGetter(Quest::getRewards),
            Quest::new
    );

    /**
     * Unwraps the {@link com.roguesmp.registry.Holder} eagerly to a plain {@code Quest} (rather
     * than exposing the Holder itself) since every current decode of this - per-player quest
     * progress, loaded on join - always runs well after {@code QUEST} has finished loading. Stays
     * a graceful {@link DataResult#error} (not a thrown exception) so a single bad/typo'd quest id
     * only drops that one entry via {@code Codec.lenientUnboundedMap} instead of failing the whole
     * player's data.
     */
    public static final Codec<Quest> REFERENCE_CODEC = Registry.referenceCodec(() -> Registries.QUEST).comapFlatMap(
            holder -> holder.isBound() ? DataResult.success(holder.value()) : DataResult.error("Unknown quest: " + holder.getId()),
            quest -> Registries.QUEST.getHolder(quest.getId())
    );

    private final String id;
    private final String name;
    private final Material icon;
    private final List<String> description;
    private final List<QuestRequirement> requirements;
    private final Map<String, QuestObjective> objectives;
    private final List<QuestReward> rewards;

    public Quest(String id, String name, Material icon, @NotNull List<String> description, List<QuestRequirement> requirements, @NotNull Map<String, QuestObjective> objectives, @NotNull List<QuestReward> rewards) {
        this.id = id;
        this.name = name;
        this.icon = icon;
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

    public Material getIcon() {
        return icon;
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
