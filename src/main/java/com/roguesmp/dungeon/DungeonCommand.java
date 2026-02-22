package com.roguesmp.dungeon;

import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.entity.Player;

public class DungeonCommand {

    public static void register(){
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
                .executes((sender, args) -> {
                    sender.sendMessage("Use /dungeon start or /dungeon stop");
                })
                .register();

    }
}
