package com.roguesmp.trade.cost;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

public class TradeCosts {

    public static final Codec<MoneyCost> MONEY = register(MoneyCost.TYPE_KEY, MoneyCost.CODEC);
    public static final Codec<ItemCost> ITEM = register(ItemCost.TYPE_KEY, ItemCost.CODEC);

    public static void loadClass() {

    }

    private static <T extends TradeCost> Codec<T> register(String key, Codec<T> codec) {
        return Registries.TRADE_COST_CODEC.register(key, codec);
    }
}
