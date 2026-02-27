package com.roguesmp.entity;

import org.bukkit.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityManager {
    public static EntityManager INSTANCE;

    private final Map<UUID, SmpEntity> spawnedEntities = new HashMap<>();

    public void unload(Entity entity) {
        SmpEntity smpEntity = spawnedEntities.get(entity.getUniqueId());
        if (smpEntity == null) return;
        smpEntity.unload();
        spawnedEntities.remove(entity.getUniqueId());
    }

    public void load(SmpEntity smpEntity) {
        spawnedEntities.put(smpEntity.entity.getUniqueId(), smpEntity);
    }

    public static void init() {
        INSTANCE = new EntityManager();
    }

    public static EntityManager getInstance() {
        if (INSTANCE == null) {
            throw new RuntimeException("EntityManager is null");
        }
        return INSTANCE;
    }
}
