package com.roguesmp.entity;

import org.bukkit.entity.LivingEntity;

/**
 * Used to constructs the correct {@link SmpEntity} subclass for a special entity id (bosses, minions, ...).
 * Registered per-id in {@code Registries.ENTITY_FACTORY} so spawning routes through a single lookup.
 */
@FunctionalInterface
public interface EntityFactory {
    SmpEntity create(BaseEntity base, LivingEntity living);
}
