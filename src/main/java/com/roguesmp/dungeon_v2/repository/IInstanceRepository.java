package com.roguesmp.dungeon_v2.repository;

import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import java.util.List;
import java.util.UUID;

public interface IInstanceRepository {
    void save(DungeonInstance instance);
    boolean delete(UUID sessionId);
    List<DungeonInstance> loadAll();
}