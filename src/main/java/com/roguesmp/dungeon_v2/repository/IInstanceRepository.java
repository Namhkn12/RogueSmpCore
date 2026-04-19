package com.roguesmp.dungeon_v2.repository;

import com.roguesmp.dungeon_v2.data.runtime.DungeonInstance;
import java.util.List;

public interface IInstanceRepository {
    void save(DungeonInstance instance);
    boolean delete(String sessionId);
    List<DungeonInstance> loadAll();
}
