package com.roguesmp.registry.ability;

import com.roguesmp.codec.Codec;
import com.roguesmp.player.ability.upgrade.ExpRequirement;
import com.roguesmp.player.ability.upgrade.ItemRequirement;
import com.roguesmp.player.ability.upgrade.UpgradeRequirement;
import com.roguesmp.registry.Registries;

public class UpgradeRequirementRegistry {

    public static void bootstrap() {
        register(ItemRequirement.TYPE_KEY, ItemRequirement.CODEC);
        register(ExpRequirement.TYPE_KEY, ExpRequirement.CODEC);
    }

    private static <T extends UpgradeRequirement> void register(String typeName, Codec<T> codec) {
        Registries.ABILITY_UPGRADE_REQUIREMENT_CODEC.register(typeName, codec);
    }
}
