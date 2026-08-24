package com.roguesmp.command;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.registry.Registry;
import com.roguesmp.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;

public final class ReloadCommand {

    private ReloadCommand() {
    }

    public static void register() {
        new CommandAPICommand("smpreload")
                .withArguments(
                        new StringArgument("registry")
                                .replaceSuggestions(ArgumentSuggestions.strings(Registry.getReloadableKeys()))
                )
                .executes((sender, args) -> {

                    String key = (String) args.get("registry");

                    boolean found = RogueSmpCore.getInstance().reloadRegistry(key);

                    if (found) {
                        sender.sendMessage(Utils.fromString("<green>Registry '" + key + "' reloaded."));
                    } else {
                        sender.sendMessage(Utils.fromString("<red>No reloadable registry named '" + key + "'."));
                    }

                }).register();
    }
}
