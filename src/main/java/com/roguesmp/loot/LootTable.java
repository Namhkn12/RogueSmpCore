package com.roguesmp.loot;

import com.roguesmp.codec.Codec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.List;

/**
 * Represents a full loot table loaded from a JSON file.
 *
 * <p>Carries no id of its own - its id is purely the {@link com.roguesmp.registry.Registry} key
 * it's stored under ({@link com.roguesmp.registry.Registries#LOOT_TABLE}, key = file path
 * relative to the loot table root, no extension, no namespace):
 * {@code plugins/RogueSmp/loottable/dungeons/dungeon_a_reward.json} maps to id
 * {@code "dungeons/dungeon_a_reward"}.
 *
 * <p>This is an immutable data object, decoded via {@link #CODEC}.
 */
public class LootTable {

    public static final Codec<LootTable> CODEC = Codec.composite(
            Codec.listOf(LootPool.CODEC).fieldOf("pools").forGetter(LootTable::getPools),
            LootTable::new
    );

    private final @NotNull List<LootPool> pools;

    public LootTable(@NotNull List<LootPool> pools) {
        this.pools = Collections.unmodifiableList(pools);
    }

    public @NotNull @Unmodifiable List<LootPool> getPools() {
        return pools;
    }
}
