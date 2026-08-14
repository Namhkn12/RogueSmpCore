package com.roguesmp.entity.component;

import com.roguesmp.entity.SmpEntity;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.event.SpellCastEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Extension point for {@link com.roguesmp.entity.BaseEntity}/{@link SmpEntity} data and behavior,
 * mirroring {@link com.roguesmp.item.component.ItemComponent}'s shape: implement only the hooks a
 * given component needs. Components own their own runtime state (see {@code SpellComponent}) -
 * there is no separate "mechanic" layer, the component IS the behavior.
 */
public interface EntityComponent {

    /**
     * Copy this (possibly shared/prototype) component for a freshly spawned entity instance. Any
     * runtime state (schedules, live spell instances, ...) must start fresh here, not be shared
     * with the template this was copied from.
     */
    @NotNull EntityComponent copy();

    /**
     * Apply this component's data onto a freshly spawned living entity (attributes, equipment,
     * vanilla flags, etc). Called once, during {@link com.roguesmp.entity.BaseEntity#processEntity}.
     * Runs on the raw {@code LivingEntity} - no {@link SmpEntity} wrapper exists yet at this point. Do not spawn
     * other entities here, run the spawning code a tick later
     */
    default void apply(LivingEntity entity) {

    }

    /**
     * Called once per {@link SmpEntity} instance during {@link SmpEntity#initialize()}, after
     * {@link #apply}. Unlike {@code apply}, the full {@code SmpEntity} wrapper is available here -
     * use this for setup that needs event dispatch, scheduling, or other components. Do not spawn
     * other entities here, run the spawning code a tick later.
     */
    default void onSpawn(SmpEntity entity) {

    }

    /**
     * Called once when the owning {@link SmpEntity} unloads - cancel schedules, remove boss bars, etc.
     * Do not remove other entities here, run the remove code a tick later.
     */
    default void onUnload(SmpEntity entity) {

    }

    default void onDamage(DamageEvent event, SmpEntity entity) {

    }

    default void onHurt(DamageEvent event, SmpEntity entity) {

    }

    default void onDeath(EntityDeathEvent event, SmpEntity entity) {

    }

    default void onProjectileLaunch(ProjectileLaunchEvent event, SmpEntity entity) {

    }

    default void onProjectileHit(ProjectileHitEvent event, SmpEntity entity) {

    }

    default void onCastSpell(SpellCastEvent event, SmpEntity entity) {

    }

    default void onTargetEntity(EntityTargetLivingEntityEvent event, SmpEntity entity) {

    }
}
