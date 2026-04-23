package com.roguesmp.entity;

import com.roguesmp.constant.Keys;
import com.roguesmp.registry.entity.EntityRegistry;
import com.roguesmp.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityManager {
    public static EntityManager INSTANCE;

    private final Map<UUID, SmpEntity> spawnedEntities = new HashMap<>();
    private final EntityRegistry entityRegistry;

    public EntityManager(EntityRegistry entityRegistry) {
        this.entityRegistry = entityRegistry;
    }

    public void unload(Entity entity) {
        SmpEntity smpEntity = spawnedEntities.get(entity.getUniqueId());
        if (smpEntity == null) return;
        smpEntity.unload();
        spawnedEntities.remove(entity.getUniqueId());
        Bukkit.getLogger().info("Removed entity");
    }

    public void onAddToWorld(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return;
        // GUARD: If the EntityManager already tracks this, stop here.
        // This prevents double-registration from the spawn() method.
        if (this.isRegistered(living)) return;

        String entityId = entity.getPersistentDataContainer().get(Keys.MOB_ID, PersistentDataType.STRING);
        if (entityId == null) return;
        // Remove special state entity (boss)
        if (entityRegistry.isSpecialEntity(entityId)) {
            Utils.runLater(entity::remove);
            return;
        }
        BaseEntity base = entityRegistry.getBaseEntity(entityId);
        if (base == null) return;

        // Wrap, Init, and Register for world-loaded entities
        SmpEntity smpEntity = entityRegistry.wrap(base, living);
        smpEntity.initialize();
        this.register(smpEntity);
    }

    public void register(SmpEntity smpEntity) {
        spawnedEntities.put(smpEntity.entity.getUniqueId(), smpEntity);
    }

    public @Nullable SmpEntity getSmpEntity(LivingEntity living) {
        if (living instanceof Player) return null;
        return spawnedEntities.get(living.getUniqueId());
    }

    public boolean isRegistered(LivingEntity living) {
        return spawnedEntities.containsKey(living.getUniqueId());
    }

    public static void init(EntityRegistry entityRegistry) {
        INSTANCE = new EntityManager(entityRegistry);
    }

    public static EntityManager getInstance() {
        if (INSTANCE == null) {
            throw new RuntimeException("EntityManager is null");
        }
        return INSTANCE;
    }
}
