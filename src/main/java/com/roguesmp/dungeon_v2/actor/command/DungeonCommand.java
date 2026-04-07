package com.roguesmp.dungeon_v2.actor.command;

import com.roguesmp.dungeon_v2.controller_.DungeonFlowController;
import dev.jorel.commandapi.CommandAPICommand;

public class DungeonCommand {
    private final DungeonFlowController flowController;

    public DungeonCommand(DungeonFlowController flowController) {
        this.flowController = flowController;
    }

    public void register(){
        new CommandAPICommand("dungeon")
                .withSubcommand(
                        new CommandAPICommand("start")
                                .executesPlayer(((player, commandArguments) -> {
                                    String dungeonId = "dungeon_cave_01";
                                    flowController.handleStartDungeon(dungeonId, player);
                                }))
                )
                .executes(((commandSender, commandArguments) -> {
                    commandSender.sendMessage("Usage: /dungeon <start>");
                }))
                .register();
    }
}
