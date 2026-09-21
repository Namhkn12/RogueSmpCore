package com.roguesmp.player.ability;

import com.roguesmp.player.ability.impl.archer.*;
import com.roguesmp.player.ability.impl.assassin.*;
import com.roguesmp.player.ability.impl.mage.*;
import com.roguesmp.player.ability.impl.warrior.BruteForce;
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

    //Assassin
    public static final AbilityInfo<DaggerThrow> DAGGER_THROW = register(DaggerThrow.INFO);
    public static final AbilityInfo<BodkinBlitz> BODKIN_BLITZ = register(BodkinBlitz.INFO);
    public static final AbilityInfo<AdvancingShadow> ADVANCING_SHADOW = register(AdvancingShadow.INFO);
    public static final AbilityInfo<CloakOfShadows> CLOAK_OF_SHADOWS = register(CloakOfShadows.INFO);
    public static final AbilityInfo<ArmorBreaker> ARMOR_BREAKER = register(ArmorBreaker.INFO);
    public static final AbilityInfo<Dodging> DODGING = register(Dodging.INFO);

    //Warrior
    public static final AbilityInfo<ShieldBash> SHIELD_BASH = register(ShieldBash.INFO);
    public static final AbilityInfo<GloriousBattle> GLORIOUS_BATTLE = register(GloriousBattle.INFO);
    public static final AbilityInfo<Indomitable> INDOMITABLE = register(Indomitable.INFO);
    public static final AbilityInfo<BruteForce> BRUTE_FORCE = register(BruteForce.INFO);

    //Archer
    public static final AbilityInfo<SplitArrow> SPLIT_ARROW = register(SplitArrow.INFO);
    public static final AbilityInfo<Sharpshooter> SHARPSHOOTER = register(Sharpshooter.INFO);
    public static final AbilityInfo<Scrapshot> SCRAPSHOT = register(Scrapshot.INFO);
    public static final AbilityInfo<Sidearm> SIDEARM = register(Sidearm.INFO);
    public static final AbilityInfo<GravityBomb> GRAVITY_BOMB = register(GravityBomb.INFO);

    //Mage
    public static final AbilityInfo<Fireball> FIREBALL = register(Fireball.INFO);
    public static final AbilityInfo<Flamestrike> FLAMESTRIKE = register(Flamestrike.INFO);
    public static final AbilityInfo<IgneousRune> IGNEOUS_RUNE = register(IgneousRune.INFO);
    public static final AbilityInfo<VolcanicMeteor> VOLCANIC_METEOR = register(VolcanicMeteor.INFO);
    public static final AbilityInfo<Pyroblast> PYROBLAST = register(Pyroblast.INFO);

    public static void loadClass() {

    }

    private static <T extends Ability> AbilityInfo<T> register(AbilityInfo<T> info) {
        Registries.ABILITY.register(info.getId(), info);
        return info;
    }
}
