package com.roguesmp.dungeon_v2.data.definition.room;

/**
 * Canonical room categories for dungeon v2.
 */
public enum RoomType {
    SPAWN("GRASS_BLOCK"),
    WARMUP("WOODEN_SWORD"),
    NORMAL("STONE", true),
    ELITE("DIAMOND_SWORD", true),
    CHECKPOINT("BEACON"),
    SAFE("BED"),
    TRADE("EMERALD"),
    PUZZLE("COMPARATOR"),
    BOSS("NETHER_STAR", true),
    TREASURE("ENDER_CHEST"),
    TRAP("TNT"),
    LOOT("CHEST"),
    EVENT("FIREWORK_ROCKET");

    private final String iconMaterial;
    private boolean isCombat = false;

    RoomType(String iconMaterial) {
        this.iconMaterial = iconMaterial;
    }

    RoomType(String iconMaterial, boolean isCombat) {
        this.iconMaterial = iconMaterial;
        this.isCombat = isCombat;
    }

    public String getIconMaterial() {
        return iconMaterial;
    }

    public boolean isCombat() {
        return isCombat;
    }

    public void setCombat(boolean combat) {
        isCombat = combat;
    }
}
