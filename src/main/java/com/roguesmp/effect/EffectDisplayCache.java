package com.roguesmp.effect;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-tick memoization for {@link SmpEffect#getSortedEffectDisplays(LivingEntity)} - callers like
 * the tab list and PlaceholderAPI integration ask for an entity's rendered effect lines once per
 * rendered line/placeholder, so without this the same sort+render work would repeat several times
 * per player per tick.
 */
final class EffectDisplayCache {

    private static final Map<LivingEntity, List<Component>> CACHE = new HashMap<>();
    private static final AtomicInteger LAST_TICK = new AtomicInteger(-1);

    private EffectDisplayCache() {}

    static List<Component> get(LivingEntity entity) {
        int currentTick = Bukkit.getCurrentTick();
        if (LAST_TICK.get() != currentTick) {
            LAST_TICK.set(currentTick);
            CACHE.clear();
        } else {
            List<Component> cached = CACHE.get(entity);
            if (cached != null) return cached;
        }

        List<Component> displays = SmpEffect.getSortedEffectDisplayComponents(entity);
        CACHE.put(entity, displays);
        return displays;
    }
}
