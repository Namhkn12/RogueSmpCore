package com.roguesmp.npc.action;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.npc.GuiOpenActionRegistry;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class OpenGuiInteractAction implements NpcAction {

    public static final String TYPE_KEY = "open_gui";
    public static final Codec<OpenGuiInteractAction> CODEC = Codec.composite(
            Codec.STRING.fieldOf("gui").forGetter(OpenGuiInteractAction::getId),
            OpenGuiInteractAction::new
    );
    private final String id;

    public OpenGuiInteractAction(String id) {
        this.id = id;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public String getId() {
        return id;
    }

    @Override
    public void onRightClick(PlayerInteractEntityEvent event) {
        GuiOpenActionRegistry.runOpenAction(id, event.getPlayer());
    }
}
