package com.roguesmp.dungeon.repository;

import com.roguesmp.dungeon.data.definition.spawner.Spawner;

import java.util.List;

public interface ISpawnerRepository {
    List<Spawner> loadAll();
    Spawner save(Spawner spawner);
    boolean delete(String id);
}
