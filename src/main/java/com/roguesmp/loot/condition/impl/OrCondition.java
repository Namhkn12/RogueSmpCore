package com.roguesmp.loot.condition.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.loot.condition.LootCondition;
import com.roguesmp.loot.context.LootContext;

import java.util.List;

public class OrCondition implements LootCondition {
    public static final String TYPE_KEY = "or";

    public static final Codec<OrCondition> CODEC = Codec.listOf(LootCondition.CODEC)
            .fieldOf("conditions")
            .xmap(OrCondition::new, OrCondition::getConditions)
            .codec();

    private final List<LootCondition> conditions;

    public OrCondition(List<LootCondition> conditions) {
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
            if (condition.test(context)) return true;
        }
        return false;
    }
}
