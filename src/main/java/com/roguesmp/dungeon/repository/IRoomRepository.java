package com.roguesmp.dungeon.repository;

import com.roguesmp.dungeon.data.definition.room.Room;

import java.util.List;

public interface IRoomRepository {
    List<Room> loadAll();
    Room save(Room room);
    boolean delete(String rid);
}
