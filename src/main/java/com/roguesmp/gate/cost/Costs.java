package com.roguesmp.gate.cost;

import com.roguesmp.codec.Codec;
import com.roguesmp.gate.cost.impl.ExpCost;
import com.roguesmp.gate.cost.impl.ItemCost;
import com.roguesmp.gate.cost.impl.MoneyCost;
import com.roguesmp.registry.Registries;

public class Costs {

    public static final Codec<MoneyCost> MONEY = register(MoneyCost.TYPE_KEY, MoneyCost.CODEC);
    public static final Codec<ItemCost> ITEM = register(ItemCost.TYPE_KEY, ItemCost.CODEC);
    public static final Codec<ExpCost> EXP = register(ExpCost.TYPE_KEY, ExpCost.CODEC);

    public static void loadClass() {

    }

    private static <T extends Cost> Codec<T> register(String key, Codec<T> codec) {
        return Registries.COST_CODEC.register(key, codec);
    }
}
