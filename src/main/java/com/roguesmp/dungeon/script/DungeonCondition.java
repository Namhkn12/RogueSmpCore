package com.roguesmp.dungeon.script;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.FeatureContext;
import com.roguesmp.registry.Registries;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface DungeonCondition {

    Codec<DungeonCondition> CODEC = Codec.dispatch(
            DungeonCondition::getTypeId,
            Registries.DUNGEON_CONDITION_CODEC::getOrThrow
    );

    static boolean all(List<DungeonCondition> conditions, FeatureContext<?> context, @Nullable Player player) {
        return conditions.stream().allMatch(condition -> condition.test(context, player));
    }

    String getTypeId();

    boolean test(FeatureContext<?> context, @Nullable Player player);

    default void validate(DungeonValidation check) {
    }
}
