package com.roguesmp.quest;

import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Registries;
import com.roguesmp.registry.quest.QuestRequirementRegistry;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * A condition that player must meet before the {@link Quest} can be accepted. A quest can have multiple requirement of the same subclass.
 */
public interface QuestRequirement {

    Codec<QuestRequirement> CODEC = Codec.dispatch(
            QuestRequirement::getTypeId,
            Registries.QUEST_REQUIREMENT_CODEC::getOrThrow
    );

    List<Component> getDisplay(SmpPlayer smpPlayer);

    /**
     * Checks if the player is eligible to accept.
     */
    boolean canMeetRequirement(SmpPlayer smpPlayer);

    String getTypeId();

    /**
     * Executed ONLY when the player successfully accepts the quest.
     * Use this to deduct money, take items, etc...
     */
    default void onAccept(SmpPlayer smpPlayer) {
        // Most requirements (like levels) don't consume anything.
    }
}
