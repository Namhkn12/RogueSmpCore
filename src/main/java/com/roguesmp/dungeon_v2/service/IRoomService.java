package com.roguesmp.dungeon_v2.service;

import com.roguesmp.dungeon_v2.data.definition.room.Room;
import com.roguesmp.dungeon_v2.data.definition.room.RoomType;
import org.bukkit.Location;

import java.util.List;

public interface IRoomService {
    Room getRoomByRoomType(List<String> pool, RoomType roomType);
    void openRoomDoor(Location middle);
    void closeRoomDoor(Location middle);
}
