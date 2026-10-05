package com.roguesmp.block;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The {@code "minecraft:<material>"} vs bare-id convention used wherever a block/item reference can
 * point at either a vanilla material or one of our own ids (see {@link BlockDrop}).
 */
public final class BlockRef {

    private static final String VANILLA_PREFIX = "minecraft:";

    private BlockRef() {}

    public static boolean isVanilla(String id) {
        return id.startsWith(VANILLA_PREFIX);
    }

    public static @Nullable Material vanillaMaterial(String id) {
        return isVanilla(id) ? Material.getMaterial(id.substring(VANILLA_PREFIX.length()).toUpperCase(Locale.ROOT)) : null;
    }

    /** The id a vanilla {@code material} would have in this convention - the inverse of {@link #vanillaMaterial}. */
    public static String vanillaId(Material material) {
        return VANILLA_PREFIX + material.getKey().getKey();
    }
}
