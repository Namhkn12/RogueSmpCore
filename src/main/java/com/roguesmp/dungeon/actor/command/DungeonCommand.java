package com.roguesmp.dungeon.actor.command;

import com.roguesmp.dungeon.data.Dungeon;
import com.roguesmp.dungeon.manager.DungeonInstanceManager;
import com.roguesmp.dungeon.manager.DungeonManager;
import com.roguesmp.dungeon.data.Party;
import com.roguesmp.dungeon.manager.PartyManager;
import com.roguesmp.dungeon.manager.RegionManager;
import com.roguesmp.dungeon.data.Room;
import com.roguesmp.dungeon.constraint.RoomType;
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
                                })
                )
                .withSubcommand(
                        new CommandAPICommand("ui")
                                .executes((sender, args) -> {
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

                                })
                )
                .executes((sender, args) -> {
                    sender.sendMessage("Use /dungeon start or /dungeon stop");
                })
                .register();

    }
}
