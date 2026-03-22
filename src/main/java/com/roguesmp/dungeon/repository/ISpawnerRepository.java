package com.roguesmp.dungeon.repository;

import com.roguesmp.dungeon.data.Spawner;
import com.roguesmp.dungeon.dto.DataResult;

import java.util.Map;

public interface ISpawnerRepository {
    DataResult<Map<String, Spawner>> loadAll();
    Spawner save(Spawner spawner);
    Boolean delete(String id);
}
