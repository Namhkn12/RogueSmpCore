package com.roguesmp.dungeon.room;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;

import java.util.UUID;

public class RoomCommand {

    private final RoomManager roomManager;

    public RoomCommand(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    public void register() {

        new CommandAPICommand("room")
                .withSubcommand(
                        new CommandAPICommand("create")
                                .withArguments(new StringArgument("rName"))
                                .executes((sender, args) -> {

                                    String roomName = (String) args.get("rName");
                                    Room room = roomManager.createRoom(roomName);


                                    if (room == null) {
                                        sender.sendMessage("§cFailed to create room.");
                                        return;
                                    }

                                    //save file
                                    roomManager.saveRoom(room.getRoomId());

                                    sender.sendMessage("§aRoom created successfully: §e" + room.getRoomId());
                                })
                )
                .register();
    }
}