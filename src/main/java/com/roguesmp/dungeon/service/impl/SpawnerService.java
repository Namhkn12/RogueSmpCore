package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.data.Spawner;
import com.roguesmp.dungeon.exception.impl.InvalidInputException;
import com.roguesmp.dungeon.exception.impl.spawner.InstanceException;
import com.roguesmp.dungeon.instance.SpawnerInstance;
import com.roguesmp.dungeon.manager.SpawnerInstanceManager;
import com.roguesmp.dungeon.manager.SpawnerManager;
import com.roguesmp.dungeon.service.ISpawnerService;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.registry.entity.EntityRegistry;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.spawner.SpawnerEntry;
import org.bukkit.entity.EntitySnapshot;

import java.util.ArrayList;
import java.util.List;

public class SpawnerService implements ISpawnerService {

    private final SpawnerManager spawnerManager;
    private final SpawnerInstanceManager instanceManager;

    public SpawnerService(SpawnerManager spawnerManager, SpawnerInstanceManager instanceManager) {
        this.spawnerManager = spawnerManager;
        this.instanceManager = instanceManager;
    }

    @Override
    public void createTemplate(String name) {
        spawnerManager.create(name);
    }

    @Override
    public void createInstance(String iid, String templateId) {
        Spawner spawner = spawnerManager.get(templateId);
        SpawnerInstance instance = instanceManager.createInstance(iid, spawner);
        if (instance == null) throw new InstanceException(iid);
        instanceManager.addInstance(instance);
    }

    @Override
    public List<Spawner> getTemplates() {
        return spawnerManager.getAllTemplate().values().stream().toList();
    }

    @Override
    public SpawnerInstance getInstance(String iid) {
        return instanceManager.getInstance(iid);
    }

    @Override
    public void applyTemplate(String templateId, CreatureSpawner spawner) {
        Spawner sp = spawnerManager.get(templateId);
        if (spawner == null) throw new InvalidInputException(CreatureSpawner.class.getName(), "cannot be null");

        spawner.setDelay(sp.getDelay());
        spawner.setSpawnRange(sp.getSpawnRange());
        spawner.setMaxNearbyEntities(sp.getMaxNearBy());
        spawner.setMaxSpawnDelay(sp.getMaxDelay());
        spawner.setMinSpawnDelay(sp.getMinDelay());
        spawner.setRequiredPlayerRange(sp.getActiveRange());
        spawner.setSpawnCount(sp.getSpawnCount());
        spawner.setSpawnedType(null);

        List<SpawnerEntry> entries = new ArrayList<>();
        sp.getMobs().forEach((mobId, weight) -> {
            BaseEntity baseEntity = EntityRegistry.getInstance().getBaseEntity(mobId);
            if (baseEntity == null) return;
            EntitySnapshot snapshot = baseEntity.spawnOnlyEquipmentSnapshot(spawner.getLocation());
            if (snapshot == null) return;
            entries.add(new SpawnerEntry(snapshot, weight, null));
        });

        spawner.setPotentialSpawns(entries);
        spawner.update();
    }
}
