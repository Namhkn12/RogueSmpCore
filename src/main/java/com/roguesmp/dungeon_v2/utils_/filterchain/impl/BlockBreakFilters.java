package com.roguesmp.dungeon_v2.utils_.filterchain.impl;

import com.roguesmp.dungeon_v2.utils_.filterchain.EventFilter;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakFilters {

    public static EventFilter<BlockBreakEvent> blockType(Material material) {
        return event -> event.getBlock().getType() == material;
    }

    public static EventFilter<BlockBreakEvent> inWorld(String prefix) {
        return event -> event.getBlock().getWorld().getName().startsWith(prefix);
    }

    public static EventFilter<BlockBreakEvent> hasDropItems() {
        return BlockBreakEvent::isDropItems;
    }

}