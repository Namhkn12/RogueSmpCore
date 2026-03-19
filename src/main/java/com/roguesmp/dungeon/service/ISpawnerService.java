package com.roguesmp.dungeon.service;

import com.roguesmp.dungeon.data.Spawner;
import com.roguesmp.dungeon.instance.SpawnerInstance;
import org.bukkit.block.CreatureSpawner;

import java.util.List;
import java.util.UUID;

public interface ISpawnerService {
    //create template
    void createSpawnerTemplate(String name);
    //create instance from template
    void createSpawnerInstance(UUID id, String spawnerId);
    //get list template
    List<Spawner> getListSpawnerTemplate();
    //get instance by id
    SpawnerInstance getSpawnerInstance(UUID id);

    void applyTemplateToSpawner(String template, CreatureSpawner spawner);
}
