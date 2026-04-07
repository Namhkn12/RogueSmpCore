package com.roguesmp.dungeon_v2.data.definition.room;

/**
 * Canonical room categories for dungeon v2.
 */
public enum RoomType {
    SPAWN("GRASS_BLOCK"),
    WARMUP("WOODEN_SWORD"),
    NORMAL("STONE"),
    ELITE("DIAMOND_SWORD"),
    CHECKPOINT("BEACON"),
    SAFE("BED"),
    TRADE("EMERALD"),
    PUZZLE("COMPARATOR"),
    BOSS("NETHER_STAR"),
    TREASURE("CHEST"),
    TRAP("TNT"),
    EVENT("FIREWORK_ROCKET");

    private final String iconMaterial;

    RoomType(String iconMaterial) {
        this.iconMaterial = iconMaterial;
    }

    public String getIconMaterial() {
        return iconMaterial;
    }
}
