package com.roguesmp.entity;

import com.roguesmp.entity.component.EntityComponent;
import com.roguesmp.entity.component.EntityComponentKey;
import com.roguesmp.event.DamageEvent;
import com.roguesmp.event.SpellCastEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Represent a BaseEntity that is spawned in the world. Behavior lives entirely on this instance's
 * own (copied) {@link EntityComponent}s - there is no separate mechanic layer, dispatch just
 * forwards events to every component.
 */
public class SmpEntity {
    public static final int PASSIVE_RUN_INTERVAL_DEFAULT = 5;
    public static final int ACTIVE_RUN_INTERVAL_DEFAULT = 2;

    protected final LivingEntity entity;
    protected final BaseEntity base;

    protected boolean initialized;
    private boolean unloaded = false;
    public boolean dead = false;

    private final Map<String, EntityComponent> componentMap = new HashMap<>();

    public SmpEntity(BaseEntity base, LivingEntity entity) {
        this.entity = entity;
        this.base = base;

        for (Map.Entry<String, EntityComponent> entry : base.getComponents().entrySet()) {
            componentMap.put(entry.getKey(), entry.getValue().copy());
        }
    }

    @SuppressWarnings("unchecked")
    public @Nullable <T extends EntityComponent> T getComponent(EntityComponentKey<T> key) {
        return (T) componentMap.get(key.id());
    }

    public @NotNull <T extends EntityComponent> T getOrCreate(EntityComponentKey<T> key, Supplier<T> supplier) {
        T comp = getComponent(key);
        if (comp == null) {
            comp = supplier.get();
            componentMap.put(key.id(), comp);
        }
        return comp;
    }

    @SuppressWarnings("unchecked")
    public <T extends EntityComponent> T setComponent(EntityComponentKey<T> key, T component) {
        return (T) componentMap.put(key.id(), component);
    }

    @SuppressWarnings("unchecked")
    public <T extends EntityComponent> T unsetComponent(EntityComponentKey<T> key) {
        return (T) componentMap.remove(key.id());
    }

    public <T extends EntityComponent> boolean hasComponent(EntityComponentKey<T> key) {
        return componentMap.containsKey(key.id());
    }

    /**
     * Runs this entity's one-time setup: applies components onto the raw living entity, then lets
     * every (copied) component run its own spawn logic (e.g. {@code SpellComponent} building and
     * starting JSON-declared spells). This is {@code final} so every entity gets the same
     * guaranteed setup - subclasses that need extra startup (intro sequences, phase triggers, ...)
     * should override {@link #onInitialized()} instead, not this method.
     */
    public final void initialize() {
        if (initialized) return; // Safety check
        initialized = true;

        base.processEntity(this.entity);
        // Snapshot first: a component's onSpawn (e.g. SpellComponent) may attach new components.
        List.copyOf(componentMap.values()).forEach(component -> component.onSpawn(this));

        onInitialized();
    }

    /**
     * Called once, after component setup, right before this entity is registered with
     * {@link EntityManager}. Override for boss/entity-specific startup instead of overriding
     * {@link #initialize()} - the guard and base component setup always run first regardless.
     */
    protected void onInitialized() {

    }

    public String getId() {
        return base.getId();
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void unload() {
        /* Make sure we don't accidentally call the main unload sequence twice */
        if (!unloaded) {
            unloaded = true;
            componentMap.values().forEach(component -> component.onUnload(this));
        }
    }

    public void onDamage(DamageEvent event) {
        componentMap.values().forEach(component -> component.onDamage(event, this));
    }

    /*
     * Entity was hurt
     */
    public void onHurt(DamageEvent event) {
        componentMap.values().forEach(component -> component.onHurt(event, this));
    }

    public void onDeath(EntityDeathEvent event) {
        componentMap.values().forEach(component -> component.onDeath(event, this));
    }

    /*
     * Entity shot a projectile
     */
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        componentMap.values().forEach(component -> component.onProjectileLaunch(event, this));
    }

    /*
     * Entity-shot projectile hit something
     */
    public void onProjectileHit(ProjectileHitEvent event) {
        componentMap.values().forEach(component -> component.onProjectileHit(event, this));
    }

    public void onCastSpell(SpellCastEvent event) {
        componentMap.values().forEach(component -> component.onCastSpell(event, this));
    }

    public void onTargetEntity(EntityTargetLivingEntityEvent event) {
        componentMap.values().forEach(component -> component.onTargetEntity(event, this));
    }

    public boolean hasPlayerDeathTrigger() {
        return false;
    }

    public void onNearbyPlayerDeath(PlayerDeathEvent event) {

    }
}
