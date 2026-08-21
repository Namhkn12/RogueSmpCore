package com.roguesmp.effect;

import com.roguesmp.event.DamageEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every {@link EffectStack} on one entity, keyed by source id (e.g. an ability's name, or a
 * generic "vulnerability" shared by several spells). Sources should always carry the same
 * {@link SmpEffect} type - mixing types under one source makes magnitude comparisons within that
 * stack meaningless.
 */
public final class EntityEffects {

    private final Map<String, EffectStack> stacks = new ConcurrentHashMap<>();

    public boolean isEmpty() {
        return stacks.values().stream().allMatch(EffectStack::isEmpty);
    }

    public EffectStack getOrCreateStack(String sourceId) {
        return stacks.computeIfAbsent(sourceId, k -> new EffectStack());
    }

    /**
     * The currently active effect per source - used for display (tab list, PlaceholderAPI).
     */
    public Map<String, SmpEffect> activeEffects() {
        Map<String, SmpEffect> result = new HashMap<>();
        for (var entry : stacks.entrySet()) {
            SmpEffect active = entry.getValue().highest();
            if (active != null) result.put(entry.getKey(), active);
        }
        return result;
    }

    /**
     * Ticks every stack down, pruning any that end up empty.
     */
    public void tick(Entity entity, int periodIncrement, boolean oneHz, boolean twoHz) {
        var iterator = stacks.entrySet().iterator();
        while (iterator.hasNext()) {
            EffectStack stack = iterator.next().getValue();
            if (stack.isEmpty()) {
                iterator.remove();
                continue;
            }
            stack.tick(entity, periodIncrement, oneHz, twoHz);
        }
    }

    public void onPlayerDeath(EntityDeathEvent event) {
        stacks.values().forEach(stack -> stack.onPlayerDeath(event));
    }

    public void onNonPlayerDeath(EntityDeathEvent event) {
        stacks.values().forEach(stack -> stack.onDeath(event));
    }

    public void onDamageEntity(DamageEvent event) {
        stacks.values().forEach(stack -> stack.onDamageEntity(event));
    }

    public void onHurt(DamageEvent event) {
        stacks.values().forEach(stack -> stack.onHurt(event));
    }

    public void removeNonPersistent() {
        stacks.values().forEach(stack -> stack.removeIf(effect -> !effect.isPersistent()));
    }

    public void refresh(Entity entity) {
        stacks.values().forEach(stack -> stack.refresh(entity));
    }

    /**
     * A snapshot of every effect (active or not) per source - used for persistence.
     */
    public Map<String, List<SmpEffect>> snapshot() {
        Map<String, List<SmpEffect>> result = new HashMap<>();
        stacks.forEach((source, stack) -> result.put(source, stack.all()));
        return result;
    }

    /**
     * Rebuilds an {@code EntityEffects} from a previously-saved snapshot (see {@link #snapshot()}) -
     * effects are restored as-is, no merging or gain/lose hooks.
     */
    public static EntityEffects fromSnapshot(Map<String, List<SmpEffect>> snapshot) {
        EntityEffects entityEffects = new EntityEffects();
        snapshot.forEach((source, effects) -> entityEffects.getOrCreateStack(source).addAll(effects));
        return entityEffects;
    }
}
