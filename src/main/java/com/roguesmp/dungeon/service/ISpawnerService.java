package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.data.definition.spawner.Spawner;
import com.roguesmp.dungeon.data.runtime.SpawnerInstance;
import org.bukkit.Location;
import org.bukkit.block.CreatureSpawner;

public interface ISpawnerService {
    void createSpawner(Spawner spawner);
    void applyTemplate(String sid, CreatureSpawner spawner);
    void createInstance(String sid, String siid, Location location);
    SpawnerInstance getInstance(String siid);
}
