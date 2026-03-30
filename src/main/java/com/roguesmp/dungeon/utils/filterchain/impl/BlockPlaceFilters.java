package com.roguesmp.dungeon.utils.filterchain.impl;

import com.roguesmp.dungeon.utils.filterchain.EventFilter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class BlockPlaceFilters {
    public static EventFilter<BlockPlaceEvent> blockType(Material material) {
        return event -> event.getBlock().getType() == material;
    }

    public static EventFilter<BlockPlaceEvent> inWorld(String prefix) {
        return event -> event.getBlock().getWorld().getName().startsWith(prefix);
    }

    public static EventFilter<BlockPlaceEvent> hasMeta() {
        return event -> event.getItemInHand().hasItemMeta();
    }

    public static EventFilter<BlockPlaceEvent> hasPdc(NamespacedKey key, ItemMeta meta){
        return event -> {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            return pdc.has(key, PersistentDataType.STRING);
        };
    }

}
