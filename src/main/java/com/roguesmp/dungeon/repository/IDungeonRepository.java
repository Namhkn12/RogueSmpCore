package com.roguesmp.dungeon.repository;

import com.roguesmp.dungeon.data.Dungeon;

import java.util.List;

public interface IDungeonRepository {
    List<Dungeon> loadAll();
    void save(Dungeon dungeon);
    boolean delete(String id);
}
