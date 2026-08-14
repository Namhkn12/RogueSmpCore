package com.roguesmp.entity.component;

import com.roguesmp.entity.SmpEntity;

/**
 * Mixin for {@link EntityComponent}s that need periodic logic without managing their own
 * Folia-safe scheduled task. {@link SmpEntity} runs a single shared entity-scheduler task per
 * spawned instance - started in {@code SmpEntity#initialize()} only if at least one component
 * implements this, cancelled in {@code SmpEntity#unload()} - and calls {@link #tick} on every
 * component that implements it, once per {@link SmpEntity#PASSIVE_RUN_INTERVAL_DEFAULT} period.
 * Same instanceof-mixin dispatch shape {@link com.roguesmp.item.component.InteractableComponent}
 * established for items.
 * <p>
 * The shared task never runs faster than that period, so a component wanting a slower effective
 * rate should track its own countdown and subtract {@code interval} each call rather than expect
 * a per-component interval here - same convention {@code entity.boss.hellknight.TeleportBehindSpell}
 * already uses against its own externally-driven passive tick.
 */
public interface TickingComponent extends EntityComponent {

    /**
     * @param entity   the owning entity, same instance passed to every other {@link EntityComponent} hook
     * @param interval ticks since the last call - always {@link SmpEntity#PASSIVE_RUN_INTERVAL_DEFAULT}
     *                  today, since the shared task runs at a fixed period, but take the parameter
     *                  rather than the constant directly in case that ever changes
     */
    void tick(SmpEntity entity, int interval);
}
