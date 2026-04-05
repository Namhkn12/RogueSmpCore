package com.roguesmp.dungeon_v2.service.impl;

import com.roguesmp.dungeon_v2.data.definition.room.Room;
import com.roguesmp.dungeon_v2.data.definition.room.RoomType;
import com.roguesmp.dungeon_v2.manager.RoomManager;
import com.roguesmp.dungeon_v2.service.IRoomService;

import java.util.List;
import java.util.Objects;

public class RoomService implements IRoomService {

    private final RoomManager roomManager;

    public RoomService(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    @Override
    public Room getRoomByRoomType(List<String> pool, RoomType roomType) {
        return pool.stream()
                .map(roomManager::get)
                .filter(Objects::nonNull)
                .filter(room -> room.getType() == roomType)
                .findFirst()
                .orElse(null);
    }
}
