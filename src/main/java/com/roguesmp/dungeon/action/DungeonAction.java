package com.roguesmp.dungeon.action;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon.FeatureContext;
import com.roguesmp.registry.Registries;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface DungeonAction {

    Codec<DungeonAction> CODEC = Codec.dispatch(
            DungeonAction::getTypeId,
            Registries.DUNGEON_ACTION_CODEC::getOrThrow
    );

    String getTypeId();

    void run(FeatureContext<?> context, @Nullable Player player);
}
