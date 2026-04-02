package com.roguesmp.dungeon_v2.repository;

import com.roguesmp.dungeon_v2.data.definition.Schemeta;
import com.roguesmp.dungeon_v2.data.definition.room.Room;

import java.util.List;

public interface IRoomRepository {
    List<Room> loadAll();
    Room save(Room room);
    boolean delete(String rid);
}
