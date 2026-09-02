package com.roguesmp.player.ability;

import com.roguesmp.player.ability.impl.active.*;
import com.roguesmp.player.ability.impl.assassin.DaggerThrow;
import com.roguesmp.player.ability.impl.lifeline.LastBreath;
import com.roguesmp.player.ability.impl.passive.Dodging;
import com.roguesmp.player.ability.impl.passive.Sharpshooter;
import com.roguesmp.registry.Registries;

/**
 * Every hardcoded {@link AbilityInfo}. Each constant registers itself into
 * {@link Registries#ABILITY} as it's initialized - call {@link #loadClass()} to force that to
 * happen.
 */
public class AbilityInfos {

    public static final AbilityInfo<InfernalOverdrive> INFERNAL_OVERDRIVE = register(InfernalOverdrive.INFO);
    public static final AbilityInfo<AetherStance> AETHER_STANCE = register(AetherStance.INFO);
    public static final AbilityInfo<DaggerThrow> DAGGER_THROW = register(DaggerThrow.INFO);
    public static final AbilityInfo<IgneousRune> IGNEOUS_RUNE = register(IgneousRune.INFO);
    public static final AbilityInfo<Scrapshot> SCRAPSHOT = register(Scrapshot.INFO);
    public static final AbilityInfo<Sidearm> SIDEARM = register(Sidearm.INFO);
    public static final AbilityInfo<Fireball> FIREBALL = register(Fireball.INFO);

    public static final AbilityInfo<FireworkBlast> FIREWORK_BLAST = register(FireworkBlast.INFO);
    public static final AbilityInfo<Flamestrike> FLAMESTRIKE = register(Flamestrike.INFO);

    public static final AbilityInfo<GravityBomb> GRAVITY_BOMB = register(GravityBomb.INFO);
    public static final AbilityInfo<FlameSpirit> FLAME_SPIRIT = register(FlameSpirit.INFO);

    public static final AbilityInfo<VolcanicMeteor> VOLCANIC_METEOR = register(VolcanicMeteor.INFO);
    public static final AbilityInfo<Raygun> RAYGUN = register(Raygun.INFO);
    public static final AbilityInfo<Pyroblast> PYROBLAST = register(Pyroblast.INFO);

    // Lifeline
    public static final AbilityInfo<LastBreath> LAST_BREATH = register(LastBreath.INFO);

    // Passive
    public static final AbilityInfo<Dodging> DODGING = register(Dodging.INFO);
    public static final AbilityInfo<Sharpshooter> SHARPSHOOTER = register(Sharpshooter.INFO);

    public static void loadClass() {

    }

    private static <T extends Ability> AbilityInfo<T> register(AbilityInfo<T> info) {
        Registries.ABILITY.register(info.getId(), info);
        return info;
    }
}
