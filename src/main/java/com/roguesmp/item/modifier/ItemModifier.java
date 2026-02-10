package com.roguesmp.item.modifier;

import com.roguesmp.item.SmpItem;
import com.roguesmp.player.SmpPlayer;

public interface ItemModifier {
    void collectAndApply(SmpItem smpItem, SmpPlayer player);
}
