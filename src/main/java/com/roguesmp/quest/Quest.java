package com.roguesmp.quest;

import com.roguesmp.quest.board.QuestBoardInfo;
import com.roguesmp.quest.condition.QuestCondition;
import com.roguesmp.quest.holder.QuestHolder;
import com.roguesmp.quest.objective.QuestObjective;
import com.roguesmp.gate.requirement.Requirement;
import com.roguesmp.quest.reward.QuestReward;
import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.Registry;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

/**
 * Represents the definition for a quest.
 */
public class Quest {

    private static final Codec<Key> ICON_CODEC = Codec.STRING.comapFlatMap(text -> {
        try {
            return DataResult.success(Key.key(text.toLowerCase(Locale.ROOT)));
        } catch (InvalidKeyException e) {
            return DataResult.error("Invalid icon model: " + text);
        }
    }, Key::asString);

    public static final Codec<Quest> CODEC = Codec.composite(
            Codec.STRING.fieldOf("id").forGetter(Quest::getId),
            Codec.STRING.fieldOf("name").forGetter(Quest::getName),
            ICON_CODEC.optionalFieldOf("icon", Material.PAPER.key()).forGetter(Quest::getIcon),
            Codec.listOf(Codec.STRING).optionalFieldOf("description", new ArrayList<>()).forGetter(Quest::getDescription),
            Codec.listOf(Requirement.CODEC).optionalFieldOf("requirements", new ArrayList<>()).forGetter(Quest::getRequirements),
            Codec.unboundedMap(QuestObjective.CODEC).optionalFieldOf("objectives", new HashMap<>()).forGetter(Quest::getObjectives),
            Codec.listOf(QuestReward.CODEC).fieldOf("rewards").forGetter(Quest::getRewards),
            Codec.BOOLEAN.optionalFieldOf("autoClaimRewards", false).forGetter(Quest::isAutoClaimRewards),
            Codec.listOf(QuestCondition.CODEC).optionalFieldOf("conditions", List.of()).forGetter(Quest::getConditions),
            QuestBoardInfo.CODEC.optionalFieldOf("board").forGetter(quest -> Optional.ofNullable(quest.board)),
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
    private final Key icon;
    private final List<String> description;
    private final List<Requirement> requirements;
    private final Map<String, QuestObjective> objectives;
    private final List<QuestReward> rewards;
    private final boolean autoClaimRewards;
    private final List<QuestCondition> conditions;
    private final @Nullable QuestBoardInfo board;

    public Quest(String id, String name, Key icon, @NotNull List<String> description, List<Requirement> requirements, @NotNull Map<String, QuestObjective> objectives, @NotNull List<QuestReward> rewards, boolean autoClaimRewards, List<QuestCondition> conditions, Optional<QuestBoardInfo> board) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.description = description;
        this.requirements = requirements;
        this.objectives = objectives;
        this.rewards = rewards;
        this.autoClaimRewards = autoClaimRewards;
        this.conditions = conditions;
        this.board = board.orElse(null);
    }

    public @Nullable QuestBoardInfo getBoard() {
        return board;
    }

    /**
     * The reset window the quest is in at {@code time}; always 0 for quests outside the board.
     */
    public long cycleAt(long time) {
        return board == null ? 0 : board.reset().cycleAt(time);
    }

    public List<QuestCondition> getConditions() {
        return conditions;
    }

    public boolean canProgress(QuestHolder holder, Player player) {
        return conditions.stream().allMatch(condition -> condition.test(holder, player));
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

    public Key getIcon() {
        return icon;
    }

    public ItemStack createIcon() {
        ItemStack stack = ItemStack.of(Material.PAPER);
        stack.setData(DataComponentTypes.ITEM_MODEL, icon);
        return stack;
    }

    public @Unmodifiable List<String> getDescription() {
        return Collections.unmodifiableList(description);
    }

    public @Unmodifiable List<QuestReward> getRewards() {
        return Collections.unmodifiableList(rewards);
    }

    /**
     * Only applies to group quests: when true, online members receive the rewards as soon as the quest completes,
     * and members who were offline can still claim theirs later.
     */
    public boolean isAutoClaimRewards() {
        return autoClaimRewards;
    }

    public @Unmodifiable List<Requirement> getRequirements() {
        return Collections.unmodifiableList(requirements);
    }

    public boolean hasObjective(Class<? extends QuestObjective> type) {
        return objectives.values().stream().anyMatch(type::isInstance);
    }

    public @Unmodifiable Map<String, QuestObjective> getObjectives() {
        return Collections.unmodifiableMap(objectives);
    }
}
