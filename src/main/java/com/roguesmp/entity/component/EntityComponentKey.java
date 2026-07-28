package com.roguesmp.entity.component;

/**
 * Typed handle for an {@link EntityComponent}, used as the key into
 * {@link com.roguesmp.entity.BaseEntity}'s component map. Mirrors
 * {@link com.roguesmp.item.component.ComponentKey}.
 */
public record EntityComponentKey<T extends EntityComponent>(String id) {
}
