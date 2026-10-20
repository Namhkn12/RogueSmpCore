package com.roguesmp.gate.requirement;

import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Registries;
import net.kyori.adventure.text.Component;

public interface Requirement {

    Codec<Requirement> CODEC = Codec.dispatch(
            Requirement::getTypeId,
            Registries.REQUIREMENT_CODEC::getOrThrow
    );

    String getTypeId();

    boolean test(SmpPlayer player);

    Component getDisplay(SmpPlayer player);
}
