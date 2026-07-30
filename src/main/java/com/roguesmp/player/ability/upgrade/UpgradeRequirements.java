package com.roguesmp.player.ability.upgrade;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

/**
 * Every {@link UpgradeRequirement} codec. Each constant registers itself into
 * {@link Registries#ABILITY_UPGRADE_REQUIREMENT_CODEC} as it's initialized - call
 * {@link #loadClass()} to force that to happen.
 */
public class UpgradeRequirements {

    public static final Codec<ItemRequirement> ITEM = register(ItemRequirement.TYPE_KEY, ItemRequirement.CODEC);
    public static final Codec<ExpRequirement> EXP = register(ExpRequirement.TYPE_KEY, ExpRequirement.CODEC);

    public static void loadClass() {

    }

    private static <T extends UpgradeRequirement> Codec<T> register(String typeName, Codec<T> codec) {
        return Registries.ABILITY_UPGRADE_REQUIREMENT_CODEC.register(typeName, codec);
    }
}
