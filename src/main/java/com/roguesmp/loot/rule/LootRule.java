package com.roguesmp.loot.rule;

import com.roguesmp.loot.context.LootContext;

/**
 * Strategy interface for contributing a bonus roll modifier to a loot roll session.
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