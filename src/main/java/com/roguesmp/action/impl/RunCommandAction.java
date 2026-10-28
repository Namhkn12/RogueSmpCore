package com.roguesmp.action.impl;

import com.roguesmp.action.ActionContext;
import com.roguesmp.action.PlayerAction;
import com.roguesmp.codec.Codec;
import org.bukkit.entity.Player;

public record RunCommandAction(String command) implements PlayerAction {

    public static final String TYPE_KEY = "run_command";
    public static final Codec<RunCommandAction> CODEC = Codec.composite(
            Codec.STRING.fieldOf("command").forGetter(RunCommandAction::command),
            RunCommandAction::new
    ).codec();

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public boolean run(ActionContext context) {
        if (command == null || command.isBlank()) return false;
        Player player = context.player();
        player.performCommand(command
                .replace("@p_id", player.getUniqueId().toString())
                .replace("@p", player.getName()));
        return true;
    }
}
