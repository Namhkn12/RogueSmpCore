package com.roguesmp.effect;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public interface DisplayableEffect {

    Map<LivingEntity, List<Component>> CACHED_LIST_MAP = new HashMap<>();
    AtomicInteger LAST_TICK = new AtomicInteger(-1);

    int getDisplayPriority();

    @Nullable
    Component getDisplayWithTime();

    @Nullable Component getDisplay();

    static List<DisplayableEffect> getEffects(LivingEntity entity) {
        List<DisplayableEffect> effects = new ArrayList<>(EffectManager.getInstance().getActiveEffects(entity).values());

        effects.removeIf(Objects::isNull);
        return effects;
    }

    static List<DisplayableEffect> getSortedEffects(LivingEntity entity) {
        return sortEffects(getEffects(entity));
    }

    static List<DisplayableEffect> sortEffects(List<DisplayableEffect> effects) {
        effects.sort((effect1, effect2) -> effect2.getDisplayPriority() - effect1.getDisplayPriority());
        return effects;
    }

    static List<Component> getSortedEffectDisplayComponents(LivingEntity entity) {
        return getSortedEffects(entity).stream().map(DisplayableEffect::getDisplayWithTime).filter(Objects::nonNull).toList();
    }

    static List<Component> getSortedEffectDisplays(LivingEntity entity) {
        int currentTick = Bukkit.getCurrentTick();
        if (LAST_TICK.get() != currentTick) {
            LAST_TICK.set(currentTick);
            CACHED_LIST_MAP.clear();
        } else {
            List<Component> cachedList = CACHED_LIST_MAP.get(entity);
            if (cachedList != null) {
                return cachedList;
            }
        }

        List<Component> displays = getSortedEffectDisplayComponents(entity);
        CACHED_LIST_MAP.put(entity, displays);
        return displays;
    }
}
