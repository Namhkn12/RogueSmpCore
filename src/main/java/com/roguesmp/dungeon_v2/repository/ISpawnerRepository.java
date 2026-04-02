package com.roguesmp.dungeon_v2.repository;

import com.roguesmp.dungeon_v2.data.definition.spawner.Spawner;

import java.util.List;

public interface ISpawnerRepository {
    List<Spawner> loadAll();
    Spawner save(Spawner spawner);
    boolean delete(String id);
}
