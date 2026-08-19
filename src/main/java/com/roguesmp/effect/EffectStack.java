package com.roguesmp.effect;

import com.roguesmp.event.DamageEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * The effects for ONE source (e.g. {@code "consumable_speed"}, or a generic
 * {@code "vulnerability"}) on one entity. Only the highest-magnitude effect is "active" (has
 * {@code onTick}/{@code onGainEffect}/{@code onLoseEffect} called) - the rest are tracked and
 * ticked down so a weaker, longer-lasting effect takes over once the active one expires.
 * <p>
 * Backed by a plain list rather than a sorted {@code Set}: magnitude here is a comparison key for
 * picking the active effect, not an identity key - a {@code NavigableSet<SmpEffect>} (the
 * previous approach) uses {@code compareTo() == 0} for equality too, so two distinct effects that
 * happen to share a magnitude would silently collide as "duplicates".
 */
public final class EffectStack {

    private final List<SmpEffect> effects = new CopyOnWriteArrayList<>();

    public boolean isEmpty() {
        return effects.isEmpty();
    }

    /**
     * The currently active (highest-magnitude) effect, or null if this stack is empty.
     */
    public @Nullable SmpEffect highest() {
        SmpEffect best = null;
        for (SmpEffect effect : effects) {
            if (best == null || effect.getMagnitude() > best.getMagnitude()) best = effect;
        }
        return best;
    }

    /**
     * A snapshot of every effect in this stack (active or not) - used for persistence.
     */
    public List<SmpEffect> all() {
        return List.copyOf(effects);
    }

    /**
     * Restores previously-saved effects as-is (no merging, no gain/lose hooks) - used when
     * loading a player's effects back from disk.
     */
    public void addAll(List<SmpEffect> loaded) {
        effects.addAll(loaded);
    }

    /**
     * Adds a new effect, or - if one of equal magnitude and death behavior already exists with a
     * shorter duration - extends that existing effect's duration instead of stacking a duplicate.
     */
    public void add(Entity entity, SmpEffect smpEffect) {
        if (effects.isEmpty()) {
            effects.add(smpEffect);
            smpEffect.onGainEffect(entity);
            return;
        }

        SmpEffect activeBefore = highest();
        for (SmpEffect existing : effects) {
            if (Double.compare(existing.getMagnitude(), smpEffect.getMagnitude()) == 0
                    && existing.getDeathBehavior() == smpEffect.getDeathBehavior()
                    && existing.getDuration() < smpEffect.getDuration()) {
                if (existing == activeBefore) {
                    existing.onLoseEffect(entity);
                    existing.onGainEffect(entity);
                }
                existing.setDuration(smpEffect.getDuration());
                return;
            }
        }

        effects.add(smpEffect);
        if (highest() == smpEffect) {
            activeBefore.onLoseEffect(entity);
            smpEffect.onGainEffect(entity);
        }
    }

    /**
     * Ticks every effect's duration down; the active effect gets {@code onTick}. If it expires,
     * fires {@code onLoseEffect} for it and {@code onGainEffect} for whichever effect becomes
     * active next (if any remain).
     */
    public void tick(Entity entity, int periodIncrement, boolean oneHz, boolean twoHz) {
        SmpEffect activeBefore = highest();
        if (activeBefore == null) return;

        activeBefore.onTick(entity, oneHz, twoHz);

        for (SmpEffect effect : effects) {
            if (effect.tickDuration(periodIncrement)) {
                effects.remove(effect);
            }
        }

        if (!effects.contains(activeBefore)) {
            // The entity could be dead after tickDuration/onTick ran
            if (entity.isValid() && !entity.isDead()) activeBefore.onLoseEffect(entity);
            SmpEffect activeAfter = highest();
            if (activeAfter != null) activeAfter.onGainEffect(entity);
        }
    }

    /**
     * Applies each effect's {@link SmpEffect.DeathBehavior} (halve duration / remove / keep) -
     * only the active effect gets {@code onDeath} first, matching how death behaviors have always
     * only mattered for players.
     */
    public void onPlayerDeath(EntityDeathEvent event) {
        SmpEffect active = highest();
        if (active != null) active.onDeath(event);

        for (SmpEffect effect : effects) {
            switch (effect.getDeathBehavior()) {
                case HALVES_ON_DEATH -> effect.setDuration(effect.getDuration() / 2);
                case REMOVE_ON_DEATH -> effects.remove(effect);
                case KEEP_ON_DEATH -> {}
            }
        }
    }

    public void onDamageEntity(DamageEvent event) {
        SmpEffect active = highest();
        if (active != null) active.onDamageEntity(event);
    }

    public void onHurt(DamageEvent event) {
        SmpEffect active = highest();
        if (active != null) active.onHurt(event);
    }

    public void onDeath(EntityDeathEvent event) {
        SmpEffect active = highest();
        if (active != null) active.onDeath(event);
    }

    /**
     * Fires {@code onLoseEffect}/{@code onGainEffect} again for the active effect without
     * changing anything, to reapply transient state (e.g. attribute modifiers) that doesn't
     * survive being persisted - used when a player's effects are restored on join.
     */
    public void refresh(Entity entity) {
        SmpEffect active = highest();
        if (active == null) return;
        active.onLoseEffect(entity);
        active.onGainEffect(entity);
    }

    public void removeIf(Predicate<SmpEffect> predicate) {
        effects.removeIf(predicate);
    }
}
