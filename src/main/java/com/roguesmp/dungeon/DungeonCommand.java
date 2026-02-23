package com.roguesmp.dungeon;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class DungeonCommand {

    public void register(){
        new CommandAPICommand("dungeon")
                .withSubcommand(
                        new CommandAPICommand("start")
                                .executes((sender, args) -> {
                                    Player player = (Player) sender;
                                    //check for party
                                    //check for dungeon
                                    //run dungeon
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("ui")
                                .executes((sender, args) -> {
                                    //change logic : allow npc use the command for player who interact
                                    Player player = (Player) sender;
                                    //check for party
                                    //check for dungeon
                                    //open dungeon ui
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("stop")
                                .executes((sender, args) -> {
                                    Player player = (Player) sender;
                                    //check for dungeon
                                    //stop dungeon
                                    //delete instance
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("create")
                                .withArguments(new StringArgument("name"))
                                .withArguments(new StringArgument("description"))
                                .executesPlayer((player, args) -> {

                                    String name = (String) args.get("name");
                                    String description = (String) args.get("description");

                                    Dungeon dungeon = DungeonManager.getInstance().create(
                                            name,
                                            description,
                                            new ArrayList<>(List.of("roomiddemo"))
                                    );

                                    player.sendMessage("§aDungeon created!");
                                    player.sendMessage("§7ID: §e" + dungeon.getDgId());
                                })
                )
                .executes((sender, args) -> {
                    sender.sendMessage("Use /dungeon start or /dungeon stop");
                })
                .register();

    }
}
