package com.roguesmp.dungeon_v2.repository;

import com.roguesmp.dungeon_v2.data.definition.Dungeon;

import java.util.List;

public interface IDungeonRepository {
    List<Dungeon> loadAll();
    boolean delete(String id);
    Dungeon save(Dungeon dungeon);
}
