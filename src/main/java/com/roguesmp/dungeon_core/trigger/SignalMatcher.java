package com.roguesmp.dungeon_core.trigger;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon_core.DungeonSignal;
import com.roguesmp.dungeon_core.FeatureContext;
import com.roguesmp.registry.Registries;

/**
 * Decides whether a signal fires a trigger, dispatched by its {@code "type"}.
 */
public interface SignalMatcher {

    Codec<SignalMatcher> CODEC = Codec.dispatch(
            SignalMatcher::getTypeId,
            Registries.DUNGEON_SIGNAL_MATCHER_CODEC::getOrThrow
    );

    String getTypeId();

    boolean matches(FeatureContext<?> context, DungeonSignal signal);
}
