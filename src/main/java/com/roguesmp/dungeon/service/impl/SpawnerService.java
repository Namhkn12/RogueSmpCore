package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.data.definition.spawner.Spawner;
import com.roguesmp.dungeon.data.runtime.SpawnerInstance;
import com.roguesmp.dungeon.manager.SpawnerInstanceManager;
import com.roguesmp.dungeon.manager.SpawnerManager;
import com.roguesmp.dungeon.service.ISpawnerService;
import com.roguesmp.dungeon.utils.Log4Craft_;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.entity.EntityManager;
import org.bukkit.Location;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.spawner.SpawnRule;
import org.bukkit.block.spawner.SpawnerEntry;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;

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
        if (template == null) {
            logger.error(this.getClass(), "Spawner is null! Cannot apply data");
            return;
        }

        spawner.setDelay(template.getDelay());
        spawner.setSpawnRange(template.getSpawnRange());
        spawner.setMaxNearbyEntities(template.getMaxNearBy());
        spawner.setMinSpawnDelay(template.getMinDelay());
        spawner.setMaxSpawnDelay(template.getMaxDelay());
        spawner.setRequiredPlayerRange(template.getActiveRange());
        spawner.setSpawnCount(template.getSpawnCount());

        List<SpawnerEntry> entries = new ArrayList<>();
        template.getMobs().forEach((mid, weight) -> {
            BaseEntity baseEntity = EntityManager.getInstance().getBaseEntity(mid);
            if (baseEntity == null) return;
            EntitySnapshot snapshot = baseEntity.spawnOnlyEquipmentSnapshot(spawner.getLocation());
            if (snapshot == null) return;

            SpawnRule rule = new SpawnRule(0, 15, 0, 15);
            entries.add(new SpawnerEntry(snapshot, weight, rule));
        });

        spawner.setPotentialSpawns(entries);
        spawner.update();
    }

    @Override
    public void createInstance(String sid, String siid, Location location) {
        Spawner spawner = spawnerManager.getById(sid);
        if(spawner == null){
            logger.error(this.getClass(), "Spawner template is null! Please check");
        }
        SpawnerInstance instance = instanceManager.create(siid, spawner, location);
        logger.sucess(this.getClass(), "Create spawner instance with siid: " + siid + " and sid: " + sid);
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
