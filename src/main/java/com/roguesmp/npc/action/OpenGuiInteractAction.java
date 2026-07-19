package com.roguesmp.npc.action;

import com.roguesmp.registry.npc.GuiOpenActionRegistry;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class OpenGuiInteractAction implements NpcAction {

    private final String id;

    public OpenGuiInteractAction(String id) {
        this.id = id;
    }

    @Override
    public void onRightClick(PlayerInteractEntityEvent event) {
        GuiOpenActionRegistry.runOpenAction(id, event.getPlayer());
    }
}
