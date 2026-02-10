package com.roguesmp.item.modifier;

import com.roguesmp.item.SmpItem;
import com.roguesmp.player.SmpPlayer;
import org.jetbrains.annotations.Nullable;

public interface ItemModifier {
    void collectAndApply(SmpItem smpItem, @Nullable SmpPlayer player);
}
