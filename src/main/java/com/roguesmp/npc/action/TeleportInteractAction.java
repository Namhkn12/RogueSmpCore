package com.roguesmp.npc.action;

import com.roguesmp.codec.Codec;
import com.roguesmp.teleport.TeleportDestination;
import com.roguesmp.teleport.TeleportOptions;
import com.roguesmp.teleport.TeleportService;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public record TeleportInteractAction(TeleportDestination destination, TeleportOptions options) implements NpcAction {

    public static final String TYPE_KEY = "teleport";
    public static final Codec<TeleportInteractAction> CODEC = Codec.composite(
            TeleportDestination.CODEC.fieldOf("destination").forGetter(TeleportInteractAction::destination),
            TeleportOptions.CODEC.optionalFieldOf("options", TeleportOptions.DEFAULT).forGetter(TeleportInteractAction::options),
            TeleportInteractAction::new
    );

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public void onRightClick(PlayerInteractEntityEvent event) {
        TeleportService.getInstance().teleport(event.getPlayer(), destination, options);
    }
}
