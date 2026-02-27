package com.roguesmp.event;

import com.roguesmp.entity.BaseEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CustomEntitySpawnEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity spawnedEntity;
    private final BaseEntity base;

    public CustomEntitySpawnEvent(LivingEntity spawnedEntity, BaseEntity base) {
        this.spawnedEntity = spawnedEntity;
        this.base = base;
    }

    public LivingEntity getSpawnedEntity() {
        return spawnedEntity;
    }

    public BaseEntity getBase() {
        return base;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
