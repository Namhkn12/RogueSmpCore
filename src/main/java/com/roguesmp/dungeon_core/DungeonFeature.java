package com.roguesmp.dungeon_core;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

/**
 * One piece of dungeon behavior, defined in a {@link DungeonDefinition} and dispatched by its {@code "type"}. The
 * feature itself is immutable; everything a run changes lives in its state {@code S}, which is saved with the run.
 */
public interface DungeonFeature<S> {

    Codec<DungeonFeature<?>> CODEC = Codec.dispatch(
            DungeonFeature::getTypeId,
            Registries.DUNGEON_FEATURE_CODEC::getOrThrow
    );

    String getTypeId();

    S createState();

    Codec<S> stateCodec();

    /**
     * Sets the run up, before the {@link DungeonSignal.Started} signal is sent.
     */
    default void onStart(FeatureContext<S> context) {
    }

    /**
     * Runs instead of {@link #onStart} for a run loaded after a restart.
     */
    default void onRestore(FeatureContext<S> context) {
    }

    /**
     * Runs every second for every run, whether its world is loaded or not.
     */
    default void onTick(FeatureContext<S> context) {
    }

    default void onSignal(FeatureContext<S> context, DungeonSignal signal) {
    }

    default void onEnd(FeatureContext<S> context) {
    }
}
