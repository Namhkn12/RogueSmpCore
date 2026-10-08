package com.roguesmp.loot;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.loot.context.LootContext;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Represents a full loot table loaded from a JSON file.
 *
 * <p>Carries no id of its own - its id is purely the {@link com.roguesmp.registry.Registry} key
 * it's stored under ({@link com.roguesmp.registry.Registries#LOOT_TABLE}, key = file path
 * relative to the loot table root, no extension, no namespace):
 * {@code plugins/RogueSmp/loottable/dungeons/dungeon_a_reward.json} maps to id
 * {@code "dungeons/dungeon_a_reward"}.
 *
 * <p>This is an immutable data object, decoded via {@link #CODEC}. It rolls itself: callers
 * resolve a table from the registry (or a {@link com.roguesmp.registry.Holder}) and call
 * {@link #roll(LootContext)}.
 */
public class LootTable {

    public static final Codec<LootTable> CODEC = Codec.composite(
            Codec.listOf(LootPool.CODEC).fieldOf("pools").forGetter(LootTable::getPools),
            LootTable::new
    );

    /** Maximum recursion depth for nested loot_table entries. */
    private static final int MAX_DEPTH = 8;

    private final @NotNull List<LootPool> pools;

    public LootTable(@NotNull List<LootPool> pools) {
        this.pools = Collections.unmodifiableList(pools);
    }

    public @NotNull @Unmodifiable List<LootPool> getPools() {
        return pools;
    }

    /**
     * Rolls every pool and returns the resulting items — an immutable list, never null, possibly
     * empty. Does not spawn or drop anything; that is the caller's job.
     */
    public @NotNull List<ItemStack> roll(@NotNull LootContext context) {
        List<ItemStack> results = new ArrayList<>();
        rollInto(context, results, 0);
        return List.copyOf(results);
    }

    /**
     * Rolls every pool, collecting into {@code results} (a private accumulator that never
     * escapes a {@link #roll} call).
     *
     * @param depth current nested-table recursion depth — aborts if it exceeds {@link #MAX_DEPTH}
     */
    public void rollInto(@NotNull LootContext context, @NotNull List<ItemStack> results, int depth) {
        if (depth > MAX_DEPTH) {
            RogueSmpCore.LOGGER.warn("[LootTable] Max recursion depth reached — possible circular nested loot_table reference.");
            return;
        }

        for (LootPool pool : pools) {
            pool.roll(context, results, depth);
        }
    }

    /**
     * Effective drop chance of every entry in every pool under {@code context} — the debug view
     * of what {@link #roll} samples.
     */
    public @NotNull LootOdds odds(@NotNull LootContext context) {
        return new LootOdds(IntStream.range(0, pools.size())
                .mapToObj(i -> new LootOdds.PoolOdds(i, pools.get(i).isActive(context), pools.get(i).odds(context)))
                .toList());
    }
}
