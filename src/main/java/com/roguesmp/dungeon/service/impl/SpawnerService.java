package com.roguesmp.dungeon.service.impl;

import com.roguesmp.dungeon.data.Spawner;
import com.roguesmp.dungeon.instance.SpawnerInstance;
import com.roguesmp.dungeon.manager.SpawnerInstanceManager;
import com.roguesmp.dungeon.manager.SpawnerManager;
import com.roguesmp.dungeon.service.ISpawnerService;
import com.roguesmp.dungeon.ultis.ConsoleLogger;
import com.roguesmp.entity.BaseEntity;
import com.roguesmp.registry.EntityRegistry;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.spawner.SpawnerEntry;
import org.bukkit.entity.EntitySnapshot;

import java.util.ArrayList;
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
            ConsoleLogger.info("[CreateInstance]", "Không lấy đc template");
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

    @Override
    public void applyTemplateToSpawner(String template, CreatureSpawner spawner) {
        Spawner sp = spawnerManager.get(template);
        if(spawner == null || sp == null){
            //log
            ConsoleLogger.info("[templateToInstance]", "Stop apply template");
            return;
        }
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
            ConsoleLogger.info("[Instance]", "Bắt đầu tạo entity với mob id " + mobId);

            if (baseEntity == null) {
                //log
                ConsoleLogger.info("[Instance]", "Khoong taoj duoc base entity");

                return;
            }

            EntitySnapshot snapshot = baseEntity.spawnOnlyEquipmentSnapshot(spawner.getLocation());
            if (snapshot == null) return;
            entries.add(new SpawnerEntry(snapshot, weight, null));
        });

        spawner.setPotentialSpawns(entries);
        spawner.update();
        ConsoleLogger.info("[Marker]", "Apply thành công");

    }
}
