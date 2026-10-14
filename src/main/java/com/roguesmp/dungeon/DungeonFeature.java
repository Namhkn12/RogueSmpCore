package com.roguesmp.dungeon;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.registry.Registries;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public interface DungeonFeature<S> {

    Codec<DungeonFeature<?>> CODEC = Codec.dispatch(
            DungeonFeature::getTypeId,
            Registries.DUNGEON_FEATURE_CODEC::getOrThrow
    );

    String getTypeId();

    S createState();

    Codec<S> stateCodec();

    // Runs before the instance exists; may add starting members or refuse the creation.
    default DataResult<Void> prepare(Player owner, Set<UUID> members) {
        return DataResult.success(null);
    }

    default void onStart(FeatureContext<S> context) {
    }

    // Replaces onStart for runs loaded after a restart.
    default void onRestore(FeatureContext<S> context) {
    }

    // Every second, whether the run's world is loaded or not.
    default void onTick(FeatureContext<S> context) {
    }

    default void onEvent(FeatureContext<S> context, DungeonEvent signal) {
    }

    default void onEnd(FeatureContext<S> context) {
    }
}
