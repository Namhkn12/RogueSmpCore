package com.roguesmp.utils;

import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.constant.Keys;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.GemSocketComponent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.ItemRegistry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Provide methods to work with our custom items. Including adding, removing, modifying components and their modifiers
 */
public class SmpItemUtils {

    public static @Nullable BaseItem getBaseItem(ItemStack itemStack) {
        String id = itemStack.getPersistentDataContainer().get(Keys.ITEM_ID, PersistentDataType.STRING);
        if (id == null) return null;
        return ItemRegistry.getInstance().getBaseItem(id);
    }

    public static ItemStack addGem(ItemStack itemStack, SmpPlayer player, List<String> gemIds) {
        SmpItem smpItem = new SmpItem(itemStack);
        GemSocketComponent gemSocketComponent = smpItem.getComponent(ComponentKeys.GEM_SOCKET);
        if (gemSocketComponent == null) {
            player.getBukkitPlayer().sendMessage("Cannot add gem to this item. (NO_GEM_COMPONENT)");
            return null;
        }
        gemIds.forEach(s -> {
            BaseItem baseItem = ItemRegistry.getInstance().getBaseItem(s);
            if (baseItem == null) return;
            if (baseItem.getComponent(ComponentKeys.GEM_DATA) == null) {
                player.getBukkitPlayer().sendMessage("Gem cannot be added. (NOT_A_GEM)");
                return;
            }
            if (!gemSocketComponent.addGem(baseItem)) {
                player.getBukkitPlayer().sendMessage("Cannot add gem. (NOT_ENOUGH_SLOT)");
            }
        });

        return smpItem.generateItemStack(player, itemStack.getAmount());
    }
}
