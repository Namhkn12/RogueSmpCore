package com.roguesmp.trade.requirement;

import com.roguesmp.codec.Codec;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Registries;
import net.kyori.adventure.text.Component;

public interface TradeRequirement {

    Codec<TradeRequirement> CODEC = Codec.dispatch(
            TradeRequirement::getTypeId,
            Registries.TRADE_REQUIREMENT_CODEC::getOrThrow
    );

    String getTypeId();

    boolean test(SmpPlayer player);

    Component getDisplay(SmpPlayer player);
}
