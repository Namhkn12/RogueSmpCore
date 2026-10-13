package com.roguesmp.dungeon_core.room;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

public interface RoomEffect<S> {

    Codec<RoomEffect<?>> CODEC = Codec.dispatch(
            RoomEffect::getTypeId,
            Registries.DUNGEON_ROOM_EFFECT_CODEC::getOrThrow
    );

    String getTypeId();

    // In memory only; resets after a restart.
    S createState();

    default void onStart(RoomEffectContext<S> context) {
    }

    default void onTick(RoomEffectContext<S> context) {
    }

    default void onEnd(RoomEffectContext<S> context) {
    }
}
