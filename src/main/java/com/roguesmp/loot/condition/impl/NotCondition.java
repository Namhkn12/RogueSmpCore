package com.roguesmp.loot.condition.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.loot.condition.LootCondition;
import com.roguesmp.loot.context.LootContext;

/**
 * Inverts a single nested condition — e.g. {@code "not origin CHEST"}.
 */
public class NotCondition implements LootCondition {

    public static final String TYPE_KEY = "not";

    public static final Codec<NotCondition> CODEC = LootCondition.CODEC
            .fieldOf("condition")
            .xmap(NotCondition::new, NotCondition::getCondition)
            .codec();

    private final LootCondition condition;

    public NotCondition(LootCondition condition) {
        this.condition = condition;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public LootCondition getCondition() {
        return condition;
    }

    @Override
    public boolean test(LootContext context) {
        return !condition.test(context);
    }
}
