package com.roguesmp.dungeon.actor.command;

import com.roguesmp.dungeon.controller.BuildingController;
import com.roguesmp.dungeon.exception.BaseException;
import com.roguesmp.dungeon.exception.GlobalException;
import com.roguesmp.dungeon.exception.impl.schemeta.SchemetaNotFoundException;
import com.roguesmp.dungeon.exception.impl.schemeta.SelectionNotFoundException;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;

public class SchemetaCommand {

    private final BuildingController buildingController;

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
                                    } catch (SelectionNotFoundException e) {
                                        GlobalException.handleAndNotify(e, player);
                                    } catch (BaseException e) {
                                        GlobalException.handleAndNotify(e, player);
                                    } catch (Exception e) {
                                        GlobalException.handleAndNotifyUnexpected(
                                                "schemeta save command", e, player, "Khong the luu schemeta");
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
                                    } catch (SchemetaNotFoundException e) {
                                        GlobalException.handleAndNotify(e, player);
                                    } catch (BaseException e) {
                                        GlobalException.handleAndNotify(e, player);
                                    } catch (Exception e) {
                                        GlobalException.handleAndNotifyUnexpected(
                                                "schemeta paste command", e, player, "Khong the paste schemeta");
                                    }
                                })
                )
                .register();
    }
}
