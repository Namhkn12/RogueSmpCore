package com.roguesmp.loot.condition;

import com.roguesmp.codec.Codec;
import com.roguesmp.loot.context.LootContext;
import com.roguesmp.registry.Registries;

import java.util.List;

public interface LootCondition {
    Codec<LootCondition> CODEC = Codec.dispatch(LootCondition::getTypeId, Registries.LOOT_CONDITION_CODEC::getOrThrow);

    String getTypeId();

    boolean test(LootContext context);

    static boolean allPass(List<LootCondition> conditions, LootContext context) {
        for (LootCondition condition : conditions) {
            if (!condition.test(context)) return false;
        }
        return true;
    }
}
