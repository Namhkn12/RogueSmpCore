package com.roguesmp.registry;

import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.impl.*;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class AbilityRegistry {
    private static final Map<String, AbilityInfo<? extends Ability>> registry = new LinkedHashMap<>();

    static {
        //Swap
        register(GravityBomb.INFO);
        register(FrostNova.INFO);
        // Right click
        register(FlameWave.INFO);
        register(ArcaneSpiral.INFO);

        // Shift right click
        register(LightningStrike.INFO);

        // Passive
        register(IronSkin.INFO);
        register(VampiricStrike.INFO);
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
