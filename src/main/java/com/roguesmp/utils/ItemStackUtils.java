package com.roguesmp.utils;

import com.destroystokyo.paper.MaterialTags;
import com.roguesmp.constant.Keys;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import io.papermc.paper.persistence.PersistentDataContainerView;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Tag;
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

    public static final EnumSet<Material> PROJECTILE_WEAPONS = EnumSet.of(
            Material.BOW,
            Material.CROSSBOW,
            Material.TRIDENT,
            Material.EGG,
            Material.SNOWBALL
            );
    public static boolean isProjectileWeapon(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) return false;

        return PROJECTILE_WEAPONS.contains(itemStack.getType());
    }

    public static boolean isConsumable(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) return false;

        return itemStack.hasData(DataComponentTypes.CONSUMABLE);
    }

    public static boolean isPickaxe(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) return false;

        return Tag.ITEMS_PICKAXES.isTagged(itemStack.getType());
    }

    /**
     * Get the ItemStack's id
     */
    public static @Nullable String getId(ItemStack itemStack) {
        PersistentDataContainerView pdc = itemStack.getPersistentDataContainer();
        return pdc.get(Keys.ITEM_ID, PersistentDataType.STRING);
    }
}
