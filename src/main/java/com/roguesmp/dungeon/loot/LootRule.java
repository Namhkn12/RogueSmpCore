package com.roguesmp.dungeon.loot;


import com.roguesmp.dungeon.context.LootContext;

/**
 * Strategy interface for contributing a bonus roll modifier to a loot roll session.
 *
 * <p>Implement this interface to define any custom rule that should affect
 * how many bonus rolls a player receives. The returned double is added to the
 * total modifier in {@link LootContext#getBonusRollModifier()}.
 *
 * <p>Built-in implementations:
 * <ul>
 *   <li>{@link LootingEnchantRule} — based on Looting level on weapon</li>
 *   <li>{@link PlayerLuckRule}     — based on player's custom luck stat</li>
 *   <li>{@link DungeonTierRule}    — based on dungeon difficulty</li>
 *   <li>{@link FixedBonusRule}     — constant value, useful for testing</li>
 * </ul>
 *
 * <p>Custom rules can be created by implementing this interface and passing
 * them into {@link LootContext.Builder#addRule(LootRule)}.
 */
@FunctionalInterface
public interface LootRule {

    /**
     * Evaluates this rule given the current loot context.
     *
     * @param context the current loot context (player, other rules, etc.)
     * @return a double modifier to be summed with other rules.
     *         0.0 means no contribution. Negative values are allowed (penalties).
     */
    double evaluate(LootContext context);
}