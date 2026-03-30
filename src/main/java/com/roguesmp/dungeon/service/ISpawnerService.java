package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.data.Spawner;
import com.roguesmp.dungeon.instance.SpawnerInstance;
import org.bukkit.block.CreatureSpawner;

import java.util.List;

public interface ISpawnerService {
    void createTemplate(String name);
    void createInstance(String iid, String templateId);
    List<Spawner> getTemplates();
    SpawnerInstance getInstance(String iid);
    void applyTemplate(String templateId, CreatureSpawner spawner);
}
