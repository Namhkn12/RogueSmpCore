package com.roguesmp.dungeon_v2.utils;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import org.bukkit.inventory.ItemStack;

public class HiddenItemBuilder {
    public static ItemStack makeHidden(ItemStack item) {
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY,
                TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        return item;
    }
}
