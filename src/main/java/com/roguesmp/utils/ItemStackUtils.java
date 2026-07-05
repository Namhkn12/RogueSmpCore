package com.roguesmp.utils;

import com.roguesmp.constant.Keys;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

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

    public static void damageItem(ItemStack itemStack, int amount) {
        Integer current = itemStack.getData(DataComponentTypes.DAMAGE);
        if (current == null) return;
        itemStack.setData(DataComponentTypes.DAMAGE, Math.max(current + amount, 0));

    }

    public static final EnumSet<Material> SHOOTABLES = EnumSet.of(
            Material.BOW,
            Material.CROSSBOW,
            Material.SNOWBALL,
            Material.TRIDENT,
            Material.EGG,
            Material.ENDER_PEARL,
            Material.FIREWORK_ROCKET,
            Material.FISHING_ROD,
            Material.SPLASH_POTION,
            Material.LINGERING_POTION,
            Material.EXPERIENCE_BOTTLE,
            Material.WIND_CHARGE
    );
    public static boolean isShootableItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) return false;

        return SHOOTABLES.contains(itemStack.getType());
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
     * Get the ItemStack's BaseItem id
     */
    public static @Nullable String getBaseId(ItemStack itemStack) {
        if (itemStack == null) return null;
        return getBaseId(itemStack.getPersistentDataContainer());
    }

    public static @Nullable String getBaseId(PersistentDataContainerView pdc) {
        return pdc.get(Keys.ITEM_ID, PersistentDataType.STRING);
    }

    /**
     * Get the ItemStack UUID, used to track and cache SmpItem.
     */
    public static @Nullable UUID getUUID(ItemStack itemStack) {
        if (itemStack == null) return null;
        return getUUID(itemStack.getPersistentDataContainer());
    }

    public static @Nullable UUID getUUID(PersistentDataContainerView pdc) {
        String uuidStr = pdc.get(Keys.ITEM_UUID, PersistentDataType.STRING);
        if (uuidStr == null) return null;
        return UUID.fromString(uuidStr);
    }
}
