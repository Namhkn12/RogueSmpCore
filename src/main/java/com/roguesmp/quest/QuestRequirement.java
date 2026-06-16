package com.roguesmp.quest;

import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * A condition that player must meet before the {@link Quest} can be accepted. A quest can have multiple requirement of the same subclass.
 */
public interface QuestRequirement {

    List<Component> getDisplay(SmpPlayer smpPlayer);

    /**
     * Checks if the player is eligible to accept.
     */
    boolean canMeetRequirement(SmpPlayer smpPlayer);

    /**
     * Executed ONLY when the player successfully accepts the quest.
     * Use this to deduct money, take items, etc...
     */
    default void onAccept(SmpPlayer smpPlayer) {
        // Most requirements (like levels) don't consume anything.
    }
}
