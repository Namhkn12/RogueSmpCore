package com.roguesmp.utils;

import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.Keys;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.EnchantComponent;
import com.roguesmp.item.component.impl.EquipAttributeComponent;
import com.roguesmp.item.component.impl.GemDataComponent;
import com.roguesmp.item.component.impl.GemSocketComponent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.ItemRegistry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Provide methods to work with our custom items. Including adding, removing, modifying components and their modifiers
 */
public class SmpItemUtils {

    public static @Nullable BaseItem getBaseItem(ItemStack itemStack) {
        String id = ItemStackUtils.getId(itemStack);
        if (id == null) return null;
        return ItemRegistry.getInstance().getBaseItem(id);
    }

    public static ItemStack addGem(ItemStack itemStack, SmpPlayer player, List<String> gemIds) {
        SmpItem smpItem = new SmpItem(itemStack);
        GemSocketComponent gemSocketComponent = smpItem.getComponent(ComponentKeys.GEM_SOCKET);
        if (gemSocketComponent == null) {
            player.sendMessage("Cannot add gem to this item. (NO_GEM_COMPONENT)");
            return itemStack;
        }
        gemIds.forEach(s -> {
            BaseItem baseItem = ItemRegistry.getInstance().getBaseItem(s);
            if (baseItem == null) return;
            GemDataComponent dataComponent = baseItem.getComponent(ComponentKeys.GEM_DATA);
            if (dataComponent == null) {
                player.sendMessage("Gem cannot be added. (NOT_A_GEM)");
                return;
            }
            EquipAttributeComponent equipAttributeComponent = smpItem.getComponent(ComponentKeys.ATTRIBUTE);
            if (equipAttributeComponent == null || !dataComponent.getAttributes().containsKey(equipAttributeComponent.getSlot())) {
                player.sendMessage("Cannot add gem. (INCOMPATIBLE_SLOT)");
                return;
            }
            if (!gemSocketComponent.canFitGem()) {
                player.sendMessage("Cannot add gem. (NOT_ENOUGH_SLOT)");
                return;
            }
            gemSocketComponent.addGem(baseItem);
        });

        return smpItem.generateItemStack(player, itemStack.getAmount());
    }

    public static ItemStack removeGem(ItemStack itemStack, SmpPlayer player, List<String> gemIds) {
        SmpItem smpItem = new SmpItem(itemStack);
        GemSocketComponent gemSocketComponent = smpItem.getComponent(ComponentKeys.GEM_SOCKET);
        if (gemSocketComponent == null) {
            player.sendMessage("Cannot remove gem from this item. (NO_GEM_COMPONENT)");
            return itemStack;
        }
        gemIds.forEach(gemSocketComponent::removeGem);
        return smpItem.generateItemStack(player, itemStack.getAmount());
    }

    public static ItemStack addEnchant(ItemStack itemStack, SmpPlayer player, Map<Enchants, Integer> enchantsData) {
        SmpItem smpItem = new SmpItem(itemStack);
        EnchantComponent enchantComponent = smpItem.getComponent(ComponentKeys.ENCHANT);
        if (enchantComponent == null) {
            player.sendMessage("Cannot add enchant to this item (NO_ENCHANT_COMPONENT)");
            return itemStack;
        }

        enchantComponent.addPersistentEnchant(new EnchantComponent.Modifier(enchantsData));

        return smpItem.generateItemStack(player, itemStack.getAmount());
    }
}
