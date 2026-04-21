package com.roguesmp.dungeon.dto;

import com.roguesmp.dungeon.data.definition.room.Room;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface SelectRoomCallback {
    void onRoomSelect(Player player, Block door, Room room);
}
