package com.roguesmp.quest;

import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Registries;
import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * A reward for {@link Quest}. A quest can have many reward of the same subclass
 */
public interface QuestReward {

    Codec<QuestReward> CODEC = Codec.dispatch(
            QuestReward::getTypeId,
            Registries.QUEST_REWARD_CODEC::getOrThrow
    );

    List<Component> getDisplay(SmpPlayer smpPlayer);

    String getTypeId();

    void giveReward(SmpPlayer smpPlayer);
}
