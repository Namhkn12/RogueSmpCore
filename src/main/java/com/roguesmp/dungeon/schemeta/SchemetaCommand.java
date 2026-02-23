package com.roguesmp.dungeon.schemeta;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;

public class SchemetaCommand {

    private final SchemetaManager manager;

    public SchemetaCommand(SchemetaManager manager) {
        this.manager = manager;
    }

    public void register() {

        new CommandAPICommand("schemeta")
                .withSubcommand(
                        new CommandAPICommand("save")
                                .withArguments(new StringArgument("name"))
                                .executesPlayer((player, args) -> {

                                    String name = (String) args.get("name");

                                    try {
                                        manager.createFromSelection(player, name);
                                        player.sendMessage("§aSchemeta saved: §e" + name);
                                    } catch (Exception e) {
                                        player.sendMessage("§cYou must select a region first!");
                                    }
                                })
                )
                .register();
    }
}