package com.roguesmp.utils;

import com.roguesmp.constant.Keys;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class ItemStackUtils {
    public static boolean isValidItem(ItemStack itemStack) {
        return itemStack != null && !itemStack.getType().isAir();
    }

    public static ItemStack hideTooltip(ItemStack itemStack) {
        ItemStack res = itemStack.clone();
        res.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true));
        return res;
    }

    public static void setLore(ItemStack stack, List<Component> components) {
        stack.setData(DataComponentTypes.LORE, ItemLore.lore(components));
    }

    public static void setItemName(ItemStack stack, Component component) {
        stack.setData(DataComponentTypes.ITEM_NAME, component);
    }

    public static final EnumSet<Material> usableItems = EnumSet.of(
            Material.BOW,
            Material.CROSSBOW,
            Material.TRIDENT,
            Material.SHIELD,
            Material.FISHING_ROD,
            Material.FIREWORK_ROCKET
            );
    public static boolean isUsable(ItemStack itemStack) {
        if (itemStack.hasData(DataComponentTypes.CONSUMABLE)) return true;
        return usableItems.contains(itemStack.getType());
    }

    /**
     * Get this ItemStack's id
     */
    public static @Nullable String getId(ItemStack itemStack) {
        PersistentDataContainerView pdc = itemStack.getPersistentDataContainer();
        return pdc.get(Keys.ITEM_ID, PersistentDataType.STRING);
    }
}
