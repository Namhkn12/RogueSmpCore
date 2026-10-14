package com.roguesmp.quest.condition;

import com.roguesmp.codec.Codec;
import com.roguesmp.quest.holder.QuestHolder;
import com.roguesmp.registry.Registries;
import org.bukkit.entity.Player;

public interface QuestCondition {

    Codec<QuestCondition> CODEC = Codec.dispatch(
            QuestCondition::getTypeId,
            Registries.QUEST_CONDITION_CODEC::getOrThrow
    );

    String getTypeId();

    boolean test(QuestHolder holder, Player player);
}
