package com.roguesmp.dungeon_v2.actor.command;

import com.roguesmp.dungeon_v2.controller_.DungeonFlowController;
import com.roguesmp.dungeon_v2.manager.DungeonManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;

public class DungeonCommand {
    private final DungeonFlowController flowController;
    private final DungeonManager dungeonManager;

    public DungeonCommand(DungeonFlowController flowController, DungeonManager dungeonManager) {
        this.flowController = flowController;
        this.dungeonManager = dungeonManager;
    }

    public void register(){
        new CommandAPICommand("dungeon")
                .withSubcommand(
                        new CommandAPICommand("start")
                                .withArguments(
                                        new StringArgument("dungeonId")
                                                .replaceSuggestions(ArgumentSuggestions.strings(
                                                        info -> dungeonManager.getAllIds().toArray(String[]::new)
                                                ))
                                )
                                .executesPlayer(((player, commandArguments) -> {
                                    String dungeonId = (String) commandArguments.get("dungeonId");
                                    if (dungeonManager.get(dungeonId) == null) {
                                        player.sendMessage("§c[Dungeon] Not found: §f" + dungeonId);
                                        return;
                                    }
                                    flowController.handleStartDungeon(dungeonId, player);
                                }))
                )
                .withSubcommand(
                        new CommandAPICommand("leave")
                                .executesPlayer((((player, commandArguments) -> {
                                    flowController.handleLeaveDungeon(player);
                                })))
                )
                .executes(((commandSender, commandArguments) -> {
                    commandSender.sendMessage("Usage: /dungeon <start>");
                }))
                .register();
    }
}
