package com.roguesmp.dungeon.room;

import org.bukkit.Material;

public enum RoomType {
    SPAWN(Material.GRASS_BLOCK, false),
    WARMUP(Material.WOODEN_SWORD, false),
    NORMAL(Material.STONE, true),
    ELITE(Material.DIAMOND_SWORD, true),
    CHECKPOINT(Material.BEACON, false),
    SAFE(Material.RED_BED, false),
    TRADE(Material.EMERALD, false),
    PUZZLE(Material.COMPARATOR, false),
    BOSS(Material.NETHER_STAR, true),
    TREASURE(Material.ENDER_CHEST, false),
    TRAP(Material.TNT, false),
    LOOT(Material.CHEST, false),
    EVENT(Material.FIREWORK_ROCKET, false);

    private final Material icon;
    private final boolean combat;

    RoomType(Material icon, boolean combat) {
        this.icon = icon;
        this.combat = combat;
    }

    public Material icon() {
        return icon;
    }

    public boolean isCombat() {
        return combat;
    }
}
