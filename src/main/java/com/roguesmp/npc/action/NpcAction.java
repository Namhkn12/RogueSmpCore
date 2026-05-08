package com.roguesmp.npc.action;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public interface NpcAction {
    default void onRightClick(PlayerInteractEntityEvent event) {

    }

    default void onLeftClick(PrePlayerAttackEntityEvent event) {

    }

}
