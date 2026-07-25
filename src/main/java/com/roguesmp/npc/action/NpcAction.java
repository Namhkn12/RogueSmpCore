package com.roguesmp.npc.action;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public interface NpcAction {

    Codec<NpcAction> CODEC = Codec.dispatch(
            NpcAction::getTypeId,
            Registries.NPC_ACTION_CODEC::getOrThrow
    );

    String getTypeId();

    default void onRightClick(PlayerInteractEntityEvent event) {

    }

    default void onLeftClick(PrePlayerAttackEntityEvent event) {

    }

}
