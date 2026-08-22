package com.roguesmp.loot;

import com.roguesmp.codec.Codec;
import com.roguesmp.loot.condition.LootCondition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.List;

/**
 * Represents a single pool inside a {@link LootTable}.
 *
 * <p>When a pool is rolled:
 * <ol>
 *   <li>If any of {@code conditions} fails, the whole pool is skipped — no rolls happen at
 *       all, not even {@code EMPTY} ones.</li>
 *   <li>For each of {@code rolls} rolls, pick one entry by weighted random</li>
 *   <li>Execute the chosen entry (drop item / delegate to nested table / do nothing)</li>
 * </ol>
 */
public class LootPool {

    public static final Codec<LootPool> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("rolls", 1).forGetter(LootPool::getRolls),
            Codec.listOf(LootEntry.CODEC).fieldOf("entries").forGetter(LootPool::getEntries),
            Codec.listOf(LootCondition.CODEC).optionalFieldOf("conditions", List.of()).forGetter(LootPool::getConditions),
            LootPool::new
    );

    private final int rolls;
    private final @NotNull List<LootEntry> entries;
    private final @NotNull List<LootCondition> conditions;

    public LootPool(int rolls, @NotNull List<LootEntry> entries, @NotNull List<LootCondition> conditions) {
        this.rolls = rolls;
        this.entries = Collections.unmodifiableList(entries);
        this.conditions = Collections.unmodifiableList(conditions);
    }

    public int getRolls() {
        return rolls;
    }

    public @NotNull @Unmodifiable List<LootEntry> getEntries() {
        return entries;
    }

    /**
     * Gates the entire pool — see {@link LootCondition}. Unlike an entry's own conditions
     * (which only remove that one entry from the weighted pick), a failing pool condition
     * skips the pool outright: no rolls, no weighted pick, nothing added to the results.
     */
    public @NotNull @Unmodifiable List<LootCondition> getConditions() {
        return conditions;
    }
}
