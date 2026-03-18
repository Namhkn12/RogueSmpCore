package com.roguesmp.dungeon.repository;

import com.roguesmp.dungeon.data.Spawner;

import java.util.Map;

public interface ISpawnerRepository {
    Map<String, Spawner> loadAll();
    Spawner save(Spawner spawner);
    boolean delete(String id);
}
