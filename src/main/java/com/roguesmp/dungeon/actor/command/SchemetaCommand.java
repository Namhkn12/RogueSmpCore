package com.roguesmp.dungeon.actor.command;

import com.roguesmp.dungeon.controller.BuildingController;
import com.roguesmp.dungeon.manager.SchemetaManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;

public class SchemetaCommand {

    private final BuildingController  buildingController;

    public SchemetaCommand(BuildingController buildingController) {
        this.buildingController = buildingController;
    }

    public void register() {

        new CommandAPICommand("schemeta")
                .withSubcommand(
                        new CommandAPICommand("save")
                                .withArguments(new StringArgument("name"))
                                .executesPlayer((player, args) -> {
                                    String name = (String) args.get("name");
                                    try {
                                        buildingController.createNewSchematic(player, name);
                                        player.sendMessage("§aSchemeta saved: §e" + name);
                                    } catch (Exception e) {
                                        player.sendMessage("§cYou must select a region first!");
                                    }
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("paste")
                                .withArguments(
                                        new StringArgument("id")
                                                .replaceSuggestions(ArgumentSuggestions.strings(
                                                        info -> buildingController.getSchematicIdList().toArray(new String[0])
                                                ))
                                )
                                .executesPlayer((player, args) -> {
                                    String id = (String) args.get("id");
                                    try {
                                        buildingController.buildSchematicById(id, player.getLocation());
                                        player.sendMessage("§aPasted: §e" + id);
                                    } catch (Exception e) {
                                        player.sendMessage("§cSchemeta not found: §e" + id);
                                    }
                                })
                )
                .register();
    }
}