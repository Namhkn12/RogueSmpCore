package com.roguesmp.trade.requirement;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

public class TradeRequirements {

    public static final Codec<LevelRequirement> LEVEL = register(LevelRequirement.TYPE_KEY, LevelRequirement.CODEC);
    public static final Codec<QuestCompletedRequirement> QUEST_COMPLETED = register(QuestCompletedRequirement.TYPE_KEY, QuestCompletedRequirement.CODEC);
    public static final Codec<PermissionRequirement> PERMISSION = register(PermissionRequirement.TYPE_KEY, PermissionRequirement.CODEC);
    public static final Codec<MoneyBalanceRequirement> MONEY = register(MoneyBalanceRequirement.TYPE_KEY, MoneyBalanceRequirement.CODEC);
    public static final Codec<ItemOwnedRequirement> ITEM = register(ItemOwnedRequirement.TYPE_KEY, ItemOwnedRequirement.CODEC);

    public static void loadClass() {

    }

    private static <T extends TradeRequirement> Codec<T> register(String key, Codec<T> codec) {
        return Registries.TRADE_REQUIREMENT_CODEC.register(key, codec);
    }
}
