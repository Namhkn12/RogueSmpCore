package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.data.Spawner;
import com.roguesmp.dungeon.instance.SpawnerInstance;
import com.roguesmp.dungeon.manager.SpawnerInstanceManager;
import com.roguesmp.dungeon.manager.SpawnerManager;
import com.roguesmp.dungeon.service.ISpawnerService;

import java.util.List;
import java.util.UUID;

public class SpawnerService implements ISpawnerService {

    private final SpawnerManager spawnerManager;
    private final SpawnerInstanceManager instanceManager;

    public SpawnerService(SpawnerManager spawnerManager, SpawnerInstanceManager instanceManager) {
        this.spawnerManager = spawnerManager;
        this.instanceManager = instanceManager;
    }

    @Override
    public void createSpawnerTemplate(String name) {
        Spawner spawner = spawnerManager.create(name);
        if(spawner != null){
            //log success
            return;
        }
        //log false
    }

    @Override
    public void createSpawnerInstance(UUID id, String spawnerId) {
        Spawner spawner = spawnerManager.get(spawnerId);
        if(spawner == null) {
            //log fail
            return;
        }
        SpawnerInstance instance = instanceManager.createInstance(id, spawner);
        if(instance != null){
            instanceManager.addInstance(instance);
            //log success
            return;
        }
        //log fail
    }

    @Override
    public List<Spawner> getListSpawnerTemplate() {
        return spawnerManager.getAllTemplate().values().stream().toList();
    }

    @Override
    public SpawnerInstance getSpawnerInstance(UUID id) {
        return instanceManager.getInstance(id);
    }
}
