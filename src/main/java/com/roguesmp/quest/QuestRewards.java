package com.roguesmp.quest;

import com.roguesmp.codec.Codec;
import com.roguesmp.quest.reward.ItemReward;
import com.roguesmp.quest.reward.MoneyReward;
import com.roguesmp.registry.Registries;

/**
 * Every {@link QuestReward} codec. Each constant registers itself into
 * {@link Registries#QUEST_REWARD_CODEC} as it's initialized - call {@link #loadClass()} to force
 * that to happen.
 */
public class QuestRewards {

    public static final Codec<ItemReward> ITEM = register(ItemReward.TYPE_KEY, ItemReward.CODEC);
    public static final Codec<MoneyReward> MONEY = register(MoneyReward.TYPE_KEY, MoneyReward.CODEC);

    public static void loadClass() {

    }

    private static <T extends QuestReward> Codec<T> register(String typeName, Codec<T> codec) {
        return Registries.QUEST_REWARD_CODEC.register(typeName, codec);
    }
}
