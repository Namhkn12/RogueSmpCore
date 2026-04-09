package com.roguesmp.dungeon_v2.data.definition.loot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.List;

/**
 * Represents a full loot table loaded from a JSON file.
 *
 * <p>ID format mirrors the file path relative to the loot_tables root:
 * {@code "rogue:dungeons/dungeon_a_reward"} maps to
 * {@code plugins/RogueSmp/loot_tables/dungeons/dungeon_a_reward.json}
 *
 * <p>This is an immutable data object. Instances are created by
 * {@link com.roguesmp.loot.repository.LootTableRepository} and cached by
 * {@link com.roguesmp.loot.manager.LootTableManager}.
 */
public class LootTable {

    private final @NotNull String id;
    private final @NotNull List<LootPool> pools;
    private final boolean hasBonusRolls; // cached flag, propagated from children

    public LootTable(@NotNull String id, @NotNull List<LootPool> pools) {
        this.id = id;
        this.pools = Collections.unmodifiableList(pools);
        this.hasBonusRolls = pools.stream().anyMatch(LootPool::hasBonusRolls);
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull @Unmodifiable List<LootPool> getPools() {
        return pools;
    }

    /**
     * True if any pool in this table (or any nested child table that was resolved)
     * has bonus_rolls > 0. Used as a quick check before applying LootRules.
     */
    public boolean hasBonusRolls() {
        return hasBonusRolls;
    }
}
