package com.roguesmp.dungeon.actor.command;

import com.roguesmp.dungeon.controller.DungeonController;
import com.roguesmp.dungeon.controller.PartyController;
import com.roguesmp.dungeon.dto.ActionResult;
import com.roguesmp.dungeon.instance.DungeonInstance;
import com.roguesmp.dungeon.utils.Log4Craft;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;

public class DungeonCommand {

    private final DungeonController dungeonController;
    private final PartyController partyController;

    public DungeonCommand(DungeonController dungeonController, PartyController partyController) {
        this.dungeonController = dungeonController;
        this.partyController = partyController;
    }

    public void register() {
        new CommandAPICommand("dungeon")
                .withSubcommand(
                        new CommandAPICommand("start")
                                .executesPlayer((player, args) -> {
                                    String templateId = "dungeon_20260310231827";
                                    ActionResult<Void> result = dungeonController.handleRequestDungeon(templateId, player);
                                    if(result.isOk()){
                                        Log4Craft.info(result.getMessage());
                                    }else {
                                        Log4Craft.error(result.getMessage());
                                    }
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("ui")
                                .executesPlayer((player, args) -> {
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("stop")
                                .executesPlayer((player, args) -> {
                                    // check for dungeon
                                    // stop dungeon
                                    // delete instance
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("create")
                                .withArguments(new StringArgument("name"))
                                .withArguments(new StringArgument("description"))
                                .executesPlayer((player, args) -> {

                                })
                )
                .executes((sender, args) -> {
                    sender.sendMessage("Usage: /dungeon <start|stop|ui|create>");
                })
                .register();
    }
}