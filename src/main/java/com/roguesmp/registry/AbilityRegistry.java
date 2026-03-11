package com.roguesmp.registry;

import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.impl.GravityBomb;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AbilityRegistry {
    private static final Map<String, AbilityInfo<? extends Ability>> registry = new HashMap<>();

    static {
        register(GravityBomb.INFO);
    }

    public static @Nullable Ability createInstance(String id, SmpPlayer smpPlayer, int level) {
        AbilityInfo<?> info = registry.get(id);
        if (info == null) return null;
        return info.factory().apply(smpPlayer, level);
    }

    public static @Nullable AbilityInfo<? extends Ability> getInfo(String id) {
        return registry.get(id);
    }

    public static @Unmodifiable Collection<AbilityInfo<?>> getAll() {
        return Collections.unmodifiableCollection(registry.values());
    }

    private static void register(AbilityInfo<? extends Ability> info) {
        registry.put(info.id(), info);
    }
}
