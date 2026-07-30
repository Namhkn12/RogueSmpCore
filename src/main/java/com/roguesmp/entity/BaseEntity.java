package com.roguesmp.entity;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.codec.Codec;
import com.roguesmp.entity.component.EntityComponentKeys;
import com.roguesmp.constant.Keys;
import com.roguesmp.entity.component.EntityComponent;
import com.roguesmp.entity.component.EntityComponentKey;
import com.roguesmp.entity.component.impl.DisplayNameComponent;
import com.roguesmp.registry.Registries;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.Map;

/**
 * Represent the data of our smpEntity
 */
public class BaseEntity {

    public static final Codec<BaseEntity> CODEC = Codec.composite(
            Codec.STRING.fieldOf("id").forGetter(BaseEntity::getId),
            Codec.enumOf(EntityType.class).fieldOf("entityType").forGetter(BaseEntity::getEntityType),
            Codec.<EntityComponent>dispatchedMap(Registries.ENTITY_COMPONENT_CODEC::getOrThrow)
                    .optionalFieldOf("components", Map.of())
                    .forGetter(BaseEntity::getComponents),
            BaseEntity::new
    );

    private final String id; // ID should remain final as it's the unique identifier
    private final EntityType entityType; // Changing the physical type of entity usually requires a respawn
    private final Map<String, EntityComponent> components;

    public BaseEntity(String id, EntityType entityType, Map<String, EntityComponent> components) {
        this.id = id;
        this.entityType = entityType;
        this.components = components;
    }

    @SuppressWarnings("unchecked")
    public @Nullable <T extends EntityComponent> T getComponent(EntityComponentKey<T> key) {
        return (T) components.get(key.id());
    }

    public <T extends EntityComponent> boolean hasComponent(EntityComponentKey<T> key) {
        return components.containsKey(key.id());
    }

    public @Unmodifiable Map<String, EntityComponent> getComponents() {
        return Collections.unmodifiableMap(components);
    }

    /**
     * Spawn entity and start its spells
     * @param location location
     * @return SmpEntity instance
     */
    public SmpEntity spawn(Location location) {
        Entity spawned = location.getWorld().spawn(location, this.getEntityType().getEntityClass(), false, entity -> {});

        if (!(spawned instanceof LivingEntity living)) {
            throw new RuntimeException("EntityType must be a living entity!");
        }
        SmpEntity smpEntity = EntityManager.getInstance().wrap(this, living);
        smpEntity.initialize();
        EntityManager.getInstance().register(smpEntity);

        return smpEntity;
    }

    public EntitySnapshot spawnOnlyEquipmentSnapshot(Location location) {
        Entity entity = location.getWorld().createEntity(location, this.getEntityType().getEntityClass());
        processEntity(entity);
        if (!(entity instanceof LivingEntity living)) {
            RogueSmpCore.LOGGER.warn("EntityType must be a living entity!");
            throw new RuntimeException("EntityType must be a living entity!");
        }
        EntitySnapshot snapshot = living.createSnapshot();
        living.remove();
        return snapshot;
    }

    public void processEntity(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return;

        PersistentDataContainer pdc = living.getPersistentDataContainer();
        pdc.set(Keys.MOB_ID, PersistentDataType.STRING, id);

        for (EntityComponent component : components.values()) {
            component.apply(living);
        }
    }

    public String getId() {
        return id;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public @Nullable String getDisplayName() {
        DisplayNameComponent component = getComponent(EntityComponentKeys.DISPLAY_NAME);
        return component != null ? component.name() : null;
    }
}
