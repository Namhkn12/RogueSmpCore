package com.roguesmp.dungeon.utils.filterchain.impl;

import com.roguesmp.world.DungeonWorldManager;
import com.roguesmp.dungeon.utils.filterchain.EventFilter;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakFilters {

    public static EventFilter<BlockBreakEvent> blockType(Material material) {
        return event -> event.getBlock().getType() == material;
    }

    public static EventFilter<BlockBreakEvent> inDungeonWorld() {
        return event -> DungeonWorldManager.isAnyDungeonWorld(event.getBlock().getWorld());
    }

    public static EventFilter<BlockBreakEvent> hasDropItems() {
        return BlockBreakEvent::isDropItems;
    }

}