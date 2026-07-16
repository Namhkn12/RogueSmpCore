package com.roguesmp.utils;

import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.constant.Enchants;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.EnchantComponent;
import com.roguesmp.registry.ItemRegistry;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Provide methods to work with our custom items. Including adding, removing, modifying components and their modifiers
 */
public class SmpItemUtils {

    public static @Nullable BaseItem getBaseItem(ItemStack itemStack) {
        if (itemStack == null) return null;
        return getBaseItem(itemStack.getPersistentDataContainer());
    }

    public static @Nullable BaseItem getBaseItem(PersistentDataContainerView pdc) {
        String id = ItemStackUtils.getBaseId(pdc);
        if (id == null) return null;
        return ItemRegistry.getInstance().getBaseItem(id);
    }

    public static void addEnchant(SmpItem smpItem, Map<Enchants, Integer> enchantsData) {
        EnchantComponent enchantComponent = smpItem.getComponent(ComponentKeys.ENCHANT);

        if (enchantComponent == null) {
            return;
        }

        enchantComponent.addPersistentEnchant(new EnchantComponent.Modifier(enchantsData));
    }
}
