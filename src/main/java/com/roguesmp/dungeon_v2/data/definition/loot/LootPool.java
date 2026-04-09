package com.roguesmp.dungeon_v2.data.definition.loot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.List;

/**
 * Represents a single pool inside a {@link LootTable}.
 *
 * <p>When a pool is rolled:
 * <ol>
 *   <li>Compute total rolls = {@code rolls} + (bonus_rolls modifier from {@link com.roguesmp.loot.rule.LootRule})</li>
 *   <li>For each roll, pick one entry by weighted random</li>
 *   <li>Execute the chosen entry (drop item / delegate to nested table / do nothing)</li>
 * </ol>
 *
 * <p>{@code bonusRolls} is the base multiplier. The actual bonus applied depends on
 * how the active {@link com.roguesmp.loot.rule.LootRule}s contribute to the
 * {@link com.roguesmp.loot.context.LootContext}.
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
     * comes from {@link com.roguesmp.loot.context.LootContext#getBonusRollModifier()}.
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