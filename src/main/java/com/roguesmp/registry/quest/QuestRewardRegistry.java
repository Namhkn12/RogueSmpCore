package com.roguesmp.registry.quest;

import com.roguesmp.codec.Codec;
import com.roguesmp.quest.QuestReward;
import com.roguesmp.quest.reward.ItemReward;
import com.roguesmp.quest.reward.MoneyReward;
import com.roguesmp.registry.Registries;

public class QuestRewardRegistry {

    public static void bootstrap() {
        register(ItemReward.TYPE_KEY, ItemReward.CODEC);
        register(MoneyReward.TYPE_KEY, MoneyReward.CODEC);
    }

    private static <T extends QuestReward> void register(String typeName, Codec<T> codec) {
        Registries.QUEST_REWARD_CODEC.register(typeName, codec);
    }
}
