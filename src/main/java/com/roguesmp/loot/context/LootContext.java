package com.roguesmp.loot.context;

import com.roguesmp.loot.rule.LootRule;
import com.roguesmp.player.SmpPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Carries all runtime information needed during a single loot roll session.
 *
 * <p>Created once per loot event (chest open, entity death), then passed through
 * the entire roll chain. Rules registered in this context contribute a numeric
 * modifier that influences how many bonus rolls are applied.
 *
 * <p>Usage:
 * <pre>{@code
 * LootContext ctx = LootContext.builder(player)
 *     .addRule(new LootingEnchantRule(weapon))
 *     .addRule(new DungeonTierRule(dungeon))
 *     .build();
 *
 * List<ItemStack> drops = lootEngine.roll(lootTableId, ctx);
 * }</pre>
 */
public class LootContext {

    private final SmpPlayer player;
    private final List<LootRule> rules;

    private LootContext(Builder builder) {
        this.player = builder.player;
        this.rules = Collections.unmodifiableList(builder.rules);
    }

    public SmpPlayer getPlayer() {
        return player;
    }

    public List<LootRule> getRules() {
        return rules;
    }

    /**
     * Aggregates the bonus roll modifier from all registered rules.
     *
     * <p>Each rule returns a double. The final modifier is the sum of all rule outputs.
     * The engine then computes: {@code floor(pool.bonusRolls * modifier)} extra rolls.
     *
     * <p>Example: pool has bonus_rolls=1.0, two rules each return 0.5 → modifier=1.0 → 1 extra roll.
     */
    public double getBonusRollModifier() {
        return rules.stream()
                .mapToDouble(rule -> rule.evaluate(this))
                .sum();
    }

    // --- Builder ---

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(SmpPlayer player) {
        return new Builder().player(player);
    }

    public static class Builder {
        private SmpPlayer player;
        private final List<LootRule> rules = new ArrayList<>();

        public Builder player(SmpPlayer player) {
            this.player = player;
            return this;
        }

        public Builder addRule(LootRule rule) {
            this.rules.add(rule);
            return this;
        }

        public Builder addRules(List<LootRule> rules) {
            this.rules.addAll(rules);
            return this;
        }

        public LootContext build() {
            return new LootContext(this);
        }
    }
}