package com.roguesmp.trade.result;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

public class TradeResults {

    public static final Codec<ItemResult> ITEM = register(ItemResult.TYPE_KEY, ItemResult.CODEC);
    public static final Codec<MoneyResult> MONEY = register(MoneyResult.TYPE_KEY, MoneyResult.CODEC);
    public static final Codec<CommandResult> COMMAND = register(CommandResult.TYPE_KEY, CommandResult.CODEC);

    public static void loadClass() {

    }

    private static <T extends TradeResult> Codec<T> register(String key, Codec<T> codec) {
        return Registries.TRADE_RESULT_CODEC.register(key, codec);
    }
}
