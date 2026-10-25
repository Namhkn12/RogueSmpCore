package com.roguesmp.dungeon.script;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.FeatureContext;
import com.roguesmp.registry.Registries;

import java.util.List;

public interface DungeonCondition {

    Codec<DungeonCondition> CODEC = Codec.dispatch(
            DungeonCondition::getTypeId,
            Registries.DUNGEON_CONDITION_CODEC::getOrThrow
    );

    static boolean all(List<DungeonCondition> conditions, FeatureContext<?> context) {
        return conditions.stream().allMatch(condition -> condition.test(context));
    }

    String getTypeId();

    boolean test(FeatureContext<?> context);

    default void validate(DungeonValidation check) {
    }
}
