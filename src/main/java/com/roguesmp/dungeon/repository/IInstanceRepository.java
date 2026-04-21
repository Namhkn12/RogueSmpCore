package com.roguesmp.dungeon.repository;

import com.roguesmp.dungeon.data.runtime.DungeonInstance;
import java.util.List;

public interface IInstanceRepository {
    void save(DungeonInstance instance);
    boolean delete(String sessionId);
    List<DungeonInstance> loadAll();
}
