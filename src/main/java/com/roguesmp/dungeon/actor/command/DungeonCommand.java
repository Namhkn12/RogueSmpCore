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
                                    DungeonInstanceManager dungeonInstanceManager = DungeonInstanceManager.getInstance();
                                    PartyManager partyManager = PartyManager.getInstance();
                                    // RegionManager re
                                    Player player = (Player) sender;
                                    //check for party
                                    Party party = partyManager.getParty(player);
                                    if(party == null) {
                                        // -> party null
                                        return;
                                    }
                                    //check for dungeon
                                    String demoDungeonId = "";
                                    // dungeonInstanceManager.createDungeonInstance(party.getPartyId(), demoDungeonId)
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
                                            new ArrayList<>(List.of(new Room(
                                                    "roomiddemo",
                                                    "roomnamedemo",
                                                    RoomType.START,
                                                    List.of("schemiddemo")
                                                    )
                                                )
                                            )
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
