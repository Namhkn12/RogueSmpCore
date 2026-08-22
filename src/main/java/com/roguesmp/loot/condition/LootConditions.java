package com.roguesmp.loot.condition;

import com.roguesmp.codec.Codec;
import com.roguesmp.loot.condition.impl.AndCondition;
import com.roguesmp.loot.condition.impl.ChanceCondition;
import com.roguesmp.loot.condition.impl.NotCondition;
import com.roguesmp.loot.condition.impl.OrCondition;
import com.roguesmp.loot.condition.impl.OriginCondition;
import com.roguesmp.registry.Registries;

/**
 * Every {@link LootCondition} codec. Each constant registers itself into
 * {@link Registries#LOOT_CONDITION_CODEC} as it's initialized — call {@link #loadClass()} to
 * force that to happen.
 */
public class LootConditions {

    public static final Codec<ChanceCondition> CHANCE = register(ChanceCondition.TYPE_KEY, ChanceCondition.CODEC);
    public static final Codec<OriginCondition> ORIGIN = register(OriginCondition.TYPE_KEY, OriginCondition.CODEC);
    public static final Codec<AndCondition> AND = register(AndCondition.TYPE_KEY, AndCondition.CODEC);
    public static final Codec<OrCondition> OR = register(OrCondition.TYPE_KEY, OrCondition.CODEC);
    public static final Codec<NotCondition> NOT = register(NotCondition.TYPE_KEY, NotCondition.CODEC);

    public static void loadClass() {

    }

    private static <T extends LootCondition> Codec<T> register(String typeName, Codec<T> codec) {
        return Registries.LOOT_CONDITION_CODEC.register(typeName, codec);
    }
}
