package com.roguesmp.player.ability.upgrade;

import com.roguesmp.player.SmpPlayer;
import net.kyori.adventure.text.Component;

public interface UpgradeRequirement {

    boolean canFulfill(SmpPlayer player);

    void consume(SmpPlayer player);

    Component getDisplay(SmpPlayer player);
}
