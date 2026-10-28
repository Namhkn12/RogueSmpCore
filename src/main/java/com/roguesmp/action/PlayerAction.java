package com.roguesmp.action;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

public interface PlayerAction {

    Codec<PlayerAction> CODEC = Codec.dispatch(
            PlayerAction::getTypeId,
            Registries.PLAYER_ACTION_CODEC::getOrThrow
    );

    String getTypeId();

    boolean run(ActionContext context);
}
