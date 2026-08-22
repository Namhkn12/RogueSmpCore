package com.roguesmp.loot.condition.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.loot.condition.LootCondition;
import com.roguesmp.loot.context.LootContext;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Passes with a flat probability in [0, 1]. To make the chance vary at runtime (player stat,
 * dungeon score, ...), read that value programmatically inside a
 * {@link com.roguesmp.loot.event.LootPoolPickEvent} listener instead — override the candidate's
 * eligibility directly rather than trying to parameterize this condition further.
 */
public class ChanceCondition implements LootCondition {

    public static final String TYPE_KEY = "chance";

    public static final Codec<ChanceCondition> CODEC = Codec.DOUBLE.fieldOf("chance")
            .xmap(ChanceCondition::new, ChanceCondition::getChance)
            .codec();

    private final double chance;

    public ChanceCondition(double chance) {
        this.chance = chance;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public double getChance() {
        return chance;
    }

    @Override
    public boolean test(LootContext context) {
        return ThreadLocalRandom.current().nextDouble() < chance;
    }
}
