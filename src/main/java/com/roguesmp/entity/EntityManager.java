package com.roguesmp.entity;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

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
        Bukkit.getLogger().info("Removed entity");
    }

    public void register(SmpEntity smpEntity) {
        spawnedEntities.put(smpEntity.entity.getUniqueId(), smpEntity);
    }

    public @Nullable SmpEntity getSmpEntity(LivingEntity living) {
        return spawnedEntities.get(living.getUniqueId());
    }

    public boolean isRegistered(LivingEntity living) {
        return spawnedEntities.containsKey(living.getUniqueId());
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
