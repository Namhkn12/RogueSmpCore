package com.roguesmp.entity;

import org.bukkit.entity.LivingEntity;

/**
 * Used to constructs the correct {@link SmpEntity} subclass for a special entity id (bosses, minions, ...).
 * Registered by key in {@code Registries.ENTITY_FACTORY}; an entity opts in via its JSON {@code "factory"} field.
 */
@FunctionalInterface
public interface EntityFactory<T extends SmpEntity> {
   T create(BaseEntity base, LivingEntity living);
}
