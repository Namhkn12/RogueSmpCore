package com.roguesmp.action.impl;

import com.roguesmp.action.ActionContext;
import com.roguesmp.action.PlayerAction;
import com.roguesmp.codec.Codec;
import com.roguesmp.teleport.TeleportDestination;
import com.roguesmp.teleport.TeleportOptions;
import com.roguesmp.teleport.TeleportService;

public record TeleportAction(TeleportDestination destination, TeleportOptions options) implements PlayerAction {

    public static final String TYPE_KEY = "teleport";
    public static final Codec<TeleportAction> CODEC = Codec.composite(
            TeleportDestination.CODEC.fieldOf("destination").forGetter(TeleportAction::destination),
            TeleportOptions.CODEC.optionalFieldOf("options", TeleportOptions.DEFAULT).forGetter(TeleportAction::options),
            TeleportAction::new
    ).codec();

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public boolean run(ActionContext context) {
        TeleportService.getInstance().teleport(context.player(), destination, options);
        return true;
    }
}
