package com.roguesmp.loot;

import com.roguesmp.loot.context.LootContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.List;

/**
 * Represents a single pool inside a {@link LootTable}.
 *
 * <p>When a pool is rolled:
 * <ol>
 *   <li>Compute total rolls = {@code rolls} + bonus derived from the context modifier</li>
 *   <li>For each roll, pick one entry by weighted random</li>
 *   <li>Execute the chosen entry (drop item / delegate to nested table / do nothing)</li>
 * </ol>
 *
 * <p>{@code bonusRolls} is the base multiplier. The actual bonus applied depends on the
 * modifiers registered on the {@link LootContext} — contributed by the roll's caller and
 * by listeners of {@link com.roguesmp.loot.event.LootRollEvent}.
 */
public class LootPool {

    private final int rolls;
    private final double bonusRolls;
    private final @NotNull List<LootEntry> entries;

    public LootPool(int rolls, double bonusRolls, @NotNull List<LootEntry> entries) {
        this.rolls = rolls;
        this.bonusRolls = bonusRolls;
        this.entries = Collections.unmodifiableList(entries);
    }

    public int getRolls() {
        return rolls;
    }

    /**
     * Base bonus rolls value (from JSON).
     * Actual bonus is: floor(bonusRolls * contextModifier) where contextModifier
     * comes from {@link LootContext#getBonusRollModifier()}.
     */
    public double getBonusRolls() {
        return bonusRolls;
    }

    public @NotNull @Unmodifiable List<LootEntry> getEntries() {
        return entries;
    }

    public boolean hasBonusRolls() {
        return bonusRolls > 0.0;
    }
}