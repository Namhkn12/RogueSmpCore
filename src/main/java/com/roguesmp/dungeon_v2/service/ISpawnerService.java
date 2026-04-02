package com.roguesmp.dungeon_v2.service;

import com.roguesmp.dungeon_v2.data.definition.spawner.Spawner;
import com.roguesmp.dungeon_v2.data.runtime.SpawnerInstance;
import org.bukkit.block.CreatureSpawner;

public interface ISpawnerService {
    void createSpawner(Spawner spawner);
    void applyTemplate(String sid, CreatureSpawner spawner);
    void createInstance(String siid, String sid);
    SpawnerInstance getInstance(String siid);
}
