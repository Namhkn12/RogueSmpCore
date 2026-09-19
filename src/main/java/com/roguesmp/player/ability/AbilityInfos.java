package com.roguesmp.player.ability;

import com.roguesmp.player.ability.impl.active.*;
import com.roguesmp.player.ability.impl.archer.GravityBomb;
import com.roguesmp.player.ability.impl.archer.Scrapshot;
import com.roguesmp.player.ability.impl.archer.Sidearm;
import com.roguesmp.player.ability.impl.assassin.*;
import com.roguesmp.player.ability.impl.lifeline.LastBreath;
import com.roguesmp.player.ability.impl.mage.*;
import com.roguesmp.player.ability.impl.archer.Sharpshooter;
import com.roguesmp.player.ability.impl.warrior.GloriousBattle;
import com.roguesmp.player.ability.impl.warrior.Indomitable;
import com.roguesmp.player.ability.impl.warrior.ShieldBash;
import com.roguesmp.registry.Registries;

/**
 * Every hardcoded {@link AbilityInfo}. Each constant registers itself into
 * {@link Registries#ABILITY} as it's initialized - call {@link #loadClass()} to force that to
 * happen.
 */
public class AbilityInfos {

    public static final AbilityInfo<InfernalOverdrive> INFERNAL_OVERDRIVE = register(InfernalOverdrive.INFO);
    public static final AbilityInfo<AetherStance> AETHER_STANCE = register(AetherStance.INFO);
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

    //Assassin
    public static final AbilityInfo<DaggerThrow> DAGGER_THROW = register(DaggerThrow.INFO);
    public static final AbilityInfo<BodkinBlitz> BODKIN_BLITZ = register(BodkinBlitz.INFO);
    public static final AbilityInfo<AdvancingShadow> ADVANCING_SHADOW = register(AdvancingShadow.INFO);
    public static final AbilityInfo<CloakOfShadows> CLOAK_OF_SHADOWS = register(CloakOfShadows.INFO);
    public static final AbilityInfo<ArmorBreaker> ARMOR_BREAKER = register(ArmorBreaker.INFO);

    //Warrior
    public static final AbilityInfo<ShieldBash> SHIELD_BASH = register(ShieldBash.INFO);
    public static final AbilityInfo<GloriousBattle> GLORIOUS_BATTLE = register(GloriousBattle.INFO);
    public static final AbilityInfo<Indomitable> INDOMITABLE = register(Indomitable.INFO);

    public static void loadClass() {

    }

    private static <T extends Ability> AbilityInfo<T> register(AbilityInfo<T> info) {
        Registries.ABILITY.register(info.getId(), info);
        return info;
    }
}
