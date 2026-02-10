package com.roguesmp.utils;

import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.GemSocketComponent;
import com.roguesmp.item.gem.GemData;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.GemRegistry;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Provide methods to work with our custom items. Including adding, removing, modifying components and their modifiers
 */
public class SmpItemUtils {
    public static @Nullable ItemStack addGem(ItemStack itemStack, SmpPlayer player, List<String> gemIds) {
        SmpItem smpItem = new SmpItem(itemStack);
        GemSocketComponent gemSocketComponent = smpItem.getComponent(ComponentKeys.GEM_SOCKET);
        if (gemSocketComponent == null) {
            player.getPlayer().sendMessage("Cannot add gem to this item. (NO_GEM_COMPONENT)");
            return null;
        }
        gemIds.forEach(s -> {
            GemData gemData = GemRegistry.getInstance().getGemData(s);
            if (gemData == null) return;
            if (!gemSocketComponent.addGem(gemData)) {
                player.getPlayer().sendMessage("Cannot add gem. (NOT_ENOUGH_SLOT)");
            }
        });

        return smpItem.generateItemStack(player, itemStack.getAmount());
    }
}
