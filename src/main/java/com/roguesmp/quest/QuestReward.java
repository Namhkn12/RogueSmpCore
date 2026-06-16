package com.roguesmp.quest;

import com.roguesmp.player.SmpPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A reward for {@link Quest}. A quest can have many reward of the same subclass
 */
public interface QuestReward {

    List<Component> getDisplay(SmpPlayer smpPlayer);

    void giveReward(SmpPlayer smpPlayer);
}
