package com.roguesmp.dungeon.repository;

import com.roguesmp.dungeon.data.definition.Dungeon;

import java.util.List;

public interface IDungeonRepository {
    List<Dungeon> loadAll();
    boolean delete(String id);
    Dungeon save(Dungeon dungeon);
}
