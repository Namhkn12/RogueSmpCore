package com.roguesmp.dungeon_core.action;

import com.roguesmp.codec.Codec;
import com.roguesmp.dungeon_core.FeatureContext;
import com.roguesmp.registry.Registries;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Something a dungeon does in response to a trigger, dispatched by its {@code "type"}.
 */
public interface DungeonAction {

    Codec<DungeonAction> CODEC = Codec.dispatch(
            DungeonAction::getTypeId,
            Registries.DUNGEON_ACTION_CODEC::getOrThrow
    );

    String getTypeId();

    /**
     * @param player the player who caused the trigger, if any
     */
    void run(FeatureContext<?> context, @Nullable Player player);
}
