package com.roguesmp.constant;

import com.google.gson.annotations.SerializedName;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.enchant.impl.*;

public enum Enchants {
    @SerializedName("fire_protection")
    FIRE_PROTECTION(new FireProtection()),
    @SerializedName("feather_falling")
    FEATHER_FALLING(new FeatherFalling()),
    @SerializedName("blast_protection")
    BLAST_PROTECTION(new BlastProtection()),
    @SerializedName("projectile_protection")
    PROJECTILE_PROTECTION(new ProjectileProtection()),
    @SerializedName("respiration")
    RESPIRATION(new Respiration()),
    @SerializedName("aqua_affinity")
    AQUA_AFFINITY(new AquaAffinity()),
    @SerializedName("thorns")
    THORNS(new Thorns()),
    @SerializedName("depth_strider")
    DEPTH_STRIDER(new DepthStrider()),
    @SerializedName("frost_walker")
    FROST_WALKER(new FrostWalker()),
    @SerializedName("soul_speed")
    SOUL_SPEED(new SoulSpeed()),
    @SerializedName("swift_sneak")
    SWIFT_SNEAK(new SwiftSneak()),
    @SerializedName("smite")
    SMITE(new Smite()),
    @SerializedName("bane_of_arthropods")
    BANE_OF_ARTHROPODS(new BaneOfArthropods()),
    @SerializedName("knockback")
    KNOCKBACK(new Knockback()),
    @SerializedName("fire_aspect")
    FIRE_ASPECT(new FireAspect()),
    @SerializedName("looting")
    LOOTING(new Looting()),
    @SerializedName("sweeping_edge")
    SWEEPING_EDGE(new SweepingEdge()),
    @SerializedName("efficiency")
    EFFICIENCY(new Efficiency()),
    @SerializedName("silk_touch")
    SILK_TOUCH(new SilkTouch()),
    @SerializedName("fortune")
    FORTUNE(new Fortune()),
    @SerializedName("retrieval")
    RETRIEVAL(new Retrieval()),
    @SerializedName("punch")
    PUNCH(new Punch()),
    @SerializedName("flame")
    FLAME(new Flame()),
    @SerializedName("luck_of_the_sea")
    LUCK_OF_THE_SEA(new LuckOfTheSea()),
    @SerializedName("lure")
    LURE(new Lure()),
    @SerializedName("impaling")
    IMPALING(new Impaling()),
    @SerializedName("riptide")
    RIPTIDE(new Riptide()),
    @SerializedName("channeling")
    CHANNELING(new Channeling()),
    @SerializedName("multishot")
    MULTISHOT(new Multishot()),
    @SerializedName("quick_charge")
    QUICK_CHARGE(new QuickCharge()),
    @SerializedName("piercing")
    PIERCING(new Piercing()),
    @SerializedName("density")
    DENSITY(new Density()),
    @SerializedName("wind_burst")
    WIND_BURST(new WindBurst()),
    @SerializedName("mending")
    MENDING(new Mending()),
    @SerializedName("greed")
    GREED(new Greed()),
    @SerializedName("explosive")
    EXPLOSIVE(new Explosive())
    ;

    private final SmpEnchant enchant;

    Enchants(SmpEnchant enchant) {
        this.enchant = enchant;
    }

    public SmpEnchant getEnchant() {
        return enchant;
    }
}
