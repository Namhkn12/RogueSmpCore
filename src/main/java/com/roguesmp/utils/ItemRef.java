package com.roguesmp.utils;

import com.roguesmp.item.BaseItem;
import com.roguesmp.item.component.ComponentKey;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.component.impl.NameComponent;
import com.roguesmp.item.component.impl.StackSizeComponent;
import com.roguesmp.registry.Registries;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The {@code "minecraft:<material>"} vs bare-id convention used wherever an item reference can
 * point at either a vanilla material or one of our own item ids.
 */
public final class ItemRef {

    private static final String VANILLA_PREFIX = "minecraft:";

    private ItemRef() {}

    public static boolean isVanilla(String id) {
        return id.startsWith(VANILLA_PREFIX);
    }

    public static String vanillaId(Material material) {
        return VANILLA_PREFIX + material.getKey().getKey();
    }

    public static @Nullable Material vanillaMaterial(String id) {
        return isVanilla(id) ? Material.getMaterial(id.substring(VANILLA_PREFIX.length()).toUpperCase(Locale.ROOT)) : null;
    }

    public static @Nullable BaseItem baseItem(String id) {
        return isVanilla(id) ? null : Registries.ITEM.get(id);
    }

    public static <T extends ItemComponent> @Nullable T component(String id, ComponentKey<T> key) {
        BaseItem item = baseItem(id);
        return item == null ? null : item.getComponent(key);
    }

    public static @Nullable String idOf(@Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return null;

        String customId = ItemStackUtils.getBaseId(stack);
        return customId != null ? customId : vanillaId(stack.getType());
    }

    public static @Nullable ItemStack toItemStack(@Nullable String id, int amount) {
        if (id == null) return null;

        if (isVanilla(id)) {
            Material material = vanillaMaterial(id);
            return material == null ? null : ItemStack.of(material, amount);
        }

        BaseItem item = Registries.ITEM.get(id);
        return item == null ? null : item.generateItemStack(amount);
    }

    public static Component displayName(String id) {
        if (isVanilla(id)) {
            Material material = vanillaMaterial(id);
            return material == null ? Component.text(id) : Component.translatable(material.translationKey());
        }

        NameComponent name = component(id, ItemComponentKeys.ITEM_NAME);
        return name == null ? Component.text(id) : Utils.fromString(name.value());
    }

    public static int maxStackSize(String id) {
        StackSizeComponent stackSize = component(id, ItemComponentKeys.STACK_SIZE);
        if (stackSize != null) return stackSize.size();

        BaseItem item = baseItem(id);
        Material material = item != null ? item.getBase() : vanillaMaterial(id);
        Integer size = material == null ? null : material.getDefaultData(DataComponentTypes.MAX_STACK_SIZE);
        return size == null ? 1 : size;
    }
}
