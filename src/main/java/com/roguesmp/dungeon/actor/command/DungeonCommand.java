package com.roguesmp.dungeon.actor.command;

import com.roguesmp.dungeon.controller.DungeonController;
import com.roguesmp.dungeon.controller.response.ControllerResponse;
import com.roguesmp.dungeon.instance.DungeonInstance;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

public class DungeonCommand {

    private final DungeonController dungeonController;

    public DungeonCommand(DungeonController dungeonController) {
        this.dungeonController = dungeonController;
    }

    public void register() {
        new CommandAPICommand("dungeon")
                .withSubcommand(
                        new CommandAPICommand("start")
                                .executesPlayer((player, args) -> {
                                    String templateId = "dungeon_20260310231827";
                                    ControllerResponse<DungeonInstance> instance = dungeonController.generateDungeon(templateId, player);
                                    dungeonController.startDungeon(instance.getData());
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