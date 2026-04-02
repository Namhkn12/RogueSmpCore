package com.roguesmp.dungeon_v2.service.impl;

import com.roguesmp.dungeon_v2.data.definition.spawner.Spawner;
import com.roguesmp.dungeon_v2.data.runtime.SpawnerInstance;
import com.roguesmp.dungeon_v2.manager.SpawnerInstanceManager;
import com.roguesmp.dungeon_v2.manager.SpawnerManager;
import com.roguesmp.dungeon_v2.service.ISpawnerService;
import com.roguesmp.dungeon_v2.utils_.Log4Craft_;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.registry.EntityRegistry;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.spawner.SpawnerEntry;
import org.bukkit.entity.EntitySnapshot;

import java.util.ArrayList;
import java.util.List;

public class SpawnerService implements ISpawnerService {

    private final SpawnerManager spawnerManager;
    private final SpawnerInstanceManager instanceManager;
    private final Log4Craft_ logger;

    public SpawnerService(SpawnerManager spawnerManager, SpawnerInstanceManager instanceManager, Log4Craft_ logger) {
        this.spawnerManager = spawnerManager;
        this.instanceManager = instanceManager;
        this.logger = logger;
    }

    @Override
    public void createSpawner(Spawner spawner) {
        spawnerManager.create(spawner);
    }

    @Override
    public void applyTemplate(String sid, CreatureSpawner spawner) {
        Spawner template = spawnerManager.getById(sid);
        if(template == null) {
            logger.error(this.getClass(), "Spawner is null! Cannot apply data");
            return;
        }

        spawner.setDelay(template.getDelay());
        spawner.setSpawnRange(template.getSpawnRange());
        spawner.setMaxNearbyEntities(template.getMaxNearBy());
        spawner.setMaxSpawnDelay(template.getMaxDelay());
        spawner.setMinSpawnDelay(template.getMinDelay());
        spawner.setRequiredPlayerRange(template.getActiveRange());
        spawner.setSpawnCount(template.getSpawnCount());
        spawner.setSpawnedType(null);

        List<SpawnerEntry> entries = new ArrayList<>();
        template.getMobs().forEach((mid, weight) -> {
            BaseEntity baseEntity = EntityRegistry.getInstance().getBaseEntity(mid);
            if (baseEntity == null) return;
            EntitySnapshot snapshot = baseEntity.spawnOnlyEquipmentSnapshot(spawner.getLocation());
            if (snapshot == null) return;
            entries.add(new SpawnerEntry(snapshot, weight, null));
        });

        spawner.setPotentialSpawns(entries);
        spawner.update();
    }

    @Override
    public void createInstance(String siid, String sid) {
        Spawner spawner = spawnerManager.getById(sid);
        SpawnerInstance instance = instanceManager.create(siid, spawner);
        if(instance == null){
            logger.error(this.getClass(), "Couldn't create spawner instance with siid " + siid);
            return;
        }
        instanceManager.add(instance);
    }

    @Override
    public SpawnerInstance getInstance(String siid) {
        return instanceManager.get(siid);
    }
}
