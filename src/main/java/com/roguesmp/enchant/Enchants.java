package com.roguesmp.enchant;

import com.roguesmp.enchant.impl.*;
import com.roguesmp.registry.Registries;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public enum Enchants {

    //Protection enchant
    FIRE_PROTECTION(new FireProtection()),
    BLAST_PROTECTION(new BlastProtection()),
    PROJECTILE_PROTECTION(new ProjectileProtection()),
    //Utility armor
    REGENERATION(new Regeneration()),
    RESPIRATION(new Respiration()),
    AQUA_AFFINITY(new AquaAffinity()),
    THORNS(new Thorns()),
    FEATHER_FALLING(new FeatherFalling()),
    DEPTH_STRIDER(new DepthStrider()),
    FROST_WALKER(new FrostWalker()),
    SOUL_SPEED(new SoulSpeed()),
    SWIFT_SNEAK(new SwiftSneak()),

    //Direct damage enchant
    SMITE(new Smite()),
    BANE_OF_ARTHROPODS(new BaneOfArthropods()),
    FIRE_SLAYER(new FireSlayer()),
    IMPALING(new Impaling()),
    REGICIDE(new Regicide()),

    //Effect weapon enchant
    SMASH(new Smash()),
    BLEEDING(new Bleeding()),
    POISONING(new Poisoning()),

    CHANNELING(new Channeling()),
    MULTISHOT(new Multishot()),
    QUICK_CHARGE(new QuickCharge()),
    PIERCING(new Piercing()),
    DENSITY(new Density()),
    WIND_BURST(new WindBurst()),

    //Utility weapon enchant
    EPOCH(new Epoch()),
    RETRIEVAL(new Retrieval()),
    LIFESTEAL(new Lifesteal()),

    KNOCKBACK(new Knockback()),
    SWEEPING_EDGE(new SweepingEdge()),
    PUNCH(new Punch()),
    FLAME(new Flame()),

    FIRE_ASPECT(new FireAspect()),
    ICE_ASPECT(new IceAspect()),
    //Movement
    RIPTIDE(new Riptide()),
    LUNGE(new Lunge()),

    //Utility tool enchant
    LOOTING(new Looting()),
    EFFICIENCY(new Efficiency()),
    SILK_TOUCH(new SilkTouch()),
    FORTUNE(new Fortune()),
    LUCK_OF_THE_SEA(new LuckOfTheSea()),
    LURE(new Lure()),
    MENDING(new Mending()),

    //Testing enchant only
    GREED(new Greed()),
    EXPLOSIVE(new Explosive()),

    //Infusion
    VIGOR(new Vigor()),
    FOCUS(new Focus()),
    FORTITUDE(new Fortitude()),
    PERSPICACITY(new Perspicacity()),
    CELERITY(new Celerity()),

    //Cosmetic
    GLOWING(new Glowing()),

    //Curse
    EXHAUSTION(new Exhaustion()),
    IRREPARABLE(new Irreparable()),
    UNCRITABLE(new Uncritable()),
    ;

    private static final Map<String, Enchants> reverseMap = new HashMap<>();

    private final SmpEnchant enchant;

    Enchants(SmpEnchant enchant) {
        this.enchant = enchant;
    }

    public SmpEnchant getEnchant() {
        return enchant;
    }

    public static @Nullable Enchants fromId(String id) {
        return reverseMap.get(id);
    }

    public static void bootstrap() {
        for (Enchants enchant : Enchants.values()) {
            Registries.ENCHANTS.register(enchant.getEnchant().getId(), enchant);
        }
    }

    static {
        for (Enchants enchants : Enchants.values()) {
            reverseMap.put(enchants.getEnchant().getId(), enchants);
        }
    }
}
