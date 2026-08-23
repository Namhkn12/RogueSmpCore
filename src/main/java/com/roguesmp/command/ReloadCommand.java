package com.roguesmp.command;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.registry.Registry;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

public final class ReloadCommand {

    private ReloadCommand() {
    }

    public static void register() {
        new CommandAPICommand("smpreload")
                .withArguments(
                        new StringArgument("registry")
                                .replaceSuggestions(ArgumentSuggestions.strings(info ->
                                        Registry.getReloadableKeys().toArray(String[]::new)
                                ))
                )
                .executes((sender, args) -> {

                    String key = (String) args.get("registry");

                    boolean found = RogueSmpCore.getInstance().reloadRegistry(key);

                    if (found) {
                        sender.sendMessage("§aRegistry '" + key + "' reloaded.");
                    } else {
                        sender.sendMessage("§cNo reloadable registry named '" + key + "'.");
                    }

                }).register();
    }
}
