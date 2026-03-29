package com.roguesmp.registry;

import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.Ability;
import com.roguesmp.player.ability.AbilityInfo;
import com.roguesmp.player.ability.impl.lifeline.LastBreath;
import com.roguesmp.player.ability.impl.lifeline.SelfDestruct;
import com.roguesmp.player.ability.impl.passive.Dodging;
import com.roguesmp.player.ability.impl.passive.Sharpshooter;
import com.roguesmp.player.ability.impl.rightclick.Fireball;
import com.roguesmp.player.ability.impl.rightclick.Sidearm;
import com.roguesmp.player.ability.impl.shiftleftclick.IgneousRune;
import com.roguesmp.player.ability.impl.shiftleftclick.Scrapshot;
import com.roguesmp.player.ability.impl.shiftprojectile.Pyroblast;
import com.roguesmp.player.ability.impl.shiftprojectile.Raygun;
import com.roguesmp.player.ability.impl.shiftrightclick.FireworkBlast;
import com.roguesmp.player.ability.impl.shiftrightclick.Flamestrike;
import com.roguesmp.player.ability.impl.shiftswap.VolcanicMeteor;
import com.roguesmp.player.ability.impl.swap.FlameSpirit;
import com.roguesmp.player.ability.impl.swap.GravityBomb;
import dev.jorel.commandapi.CommandAPICommand;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class AbilityRegistry {
    private static final Map<String, AbilityInfo<? extends Ability>> registry = new LinkedHashMap<>();

    static {
        //Shift LeftClick
        register(IgneousRune.INFO);
        register(Scrapshot.INFO);
        //RightClick
        register(Sidearm.INFO);
        register(Fireball.INFO);

        //Shift RightClick
        register(FireworkBlast.INFO);
        register(Flamestrike.INFO);

        //Swap
        register(GravityBomb.INFO);
        register(FlameSpirit.INFO);

        //Shift Swap
        register(VolcanicMeteor.INFO);
        //Shift Projectile
        register(Raygun.INFO);
        register(Pyroblast.INFO);
        //Lifeline
        register(SelfDestruct.INFO);
        register(LastBreath.INFO);
        // Passive
        register(Dodging.INFO);
        register(Sharpshooter.INFO);
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
