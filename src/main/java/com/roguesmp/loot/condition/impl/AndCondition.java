package com.roguesmp.loot.condition.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.loot.condition.LootCondition;
import com.roguesmp.loot.context.LootContext;

import java.util.List;

/**
 * Passes only if every nested condition passes. An entry/pool's own {@code conditions} list is
 * already implicitly AND-ed, so this mostly exists to be nested inside an {@link OrCondition} —
 * e.g. {@code (A AND B) OR C}. An empty list is vacuously true.
 */
public class AndCondition implements LootCondition {

    public static final String TYPE_KEY = "and";

    public static final Codec<AndCondition> CODEC = Codec.listOf(LootCondition.CODEC)
            .fieldOf("conditions")
            .xmap(AndCondition::new, AndCondition::getConditions)
            .codec();

    private final List<LootCondition> conditions;

    public AndCondition(List<LootCondition> conditions) {
        this.conditions = conditions;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public List<LootCondition> getConditions() {
        return conditions;
    }

    @Override
    public boolean test(LootContext context) {
        for (LootCondition condition : conditions) {
            if (!condition.test(context)) return false;
        }
        return true;
    }
}
