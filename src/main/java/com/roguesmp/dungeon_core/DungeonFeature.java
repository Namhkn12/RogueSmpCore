package com.roguesmp.dungeon_core;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

public interface DungeonFeature<S> {

    Codec<DungeonFeature<?>> CODEC = Codec.dispatch(
            DungeonFeature::getTypeId,
            Registries.DUNGEON_FEATURE_CODEC::getOrThrow
    );

    String getTypeId();

    S createState();

    Codec<S> stateCodec();

    default void onStart(FeatureContext<S> context) {
    }

    // Replaces onStart for runs loaded after a restart.
    default void onRestore(FeatureContext<S> context) {
    }

    // Every second, whether the run's world is loaded or not.
    default void onTick(FeatureContext<S> context) {
    }

    default void onSignal(FeatureContext<S> context, DungeonSignal signal) {
    }

    default void onEnd(FeatureContext<S> context) {
    }
}
