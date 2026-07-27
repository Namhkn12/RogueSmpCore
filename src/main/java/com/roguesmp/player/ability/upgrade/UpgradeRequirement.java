package com.roguesmp.player.ability.upgrade;

import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Registries;
import net.kyori.adventure.text.Component;

public interface UpgradeRequirement {

    Codec<UpgradeRequirement> CODEC = Codec.dispatch(
            UpgradeRequirement::getTypeId,
            Registries.ABILITY_UPGRADE_REQUIREMENT_CODEC::getOrThrow
    );

    String getTypeId();

    boolean canFulfill(SmpPlayer player);

    void consume(SmpPlayer player);

    Component getDisplay(SmpPlayer player);
}
