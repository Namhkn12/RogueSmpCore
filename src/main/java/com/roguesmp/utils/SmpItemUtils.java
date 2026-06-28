package com.roguesmp.utils;

import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.constant.Enchants;
import com.roguesmp.item.BaseItem;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.EnchantComponent;
import com.roguesmp.item.component.impl.EquipAttributeComponent;
import com.roguesmp.item.component.impl.GemDataComponent;
import com.roguesmp.item.component.impl.GemSocketComponent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.ItemRegistry;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Provide methods to work with our custom items. Including adding, removing, modifying components and their modifiers
 */
public class SmpItemUtils {

    public static @Nullable BaseItem getBaseItem(ItemStack itemStack) {
        String id = ItemStackUtils.getBaseId(itemStack);
        if (id == null) return null;
        return ItemRegistry.getInstance().getBaseItem(id);
    }

    public record Result<T>(
            ItemStack itemStack,
            boolean success,
            String message,
            @Nullable T metadata
    ) {}

    public static Result<String> addGem(ItemStack itemStack, SmpPlayer player, String gemId) {
        SmpItem smpItem = SmpItem.wrap(itemStack, player);
        GemSocketComponent gemSocketComponent = smpItem.getComponent(ComponentKeys.GEM_SOCKET);

        // 1. Check for component
        if (gemSocketComponent == null) {
            return new Result<>(itemStack, false, "Vật phẩm được khảm không thể khảm ngọc", gemId);
        }

        // 2. Resolve the Gem
        BaseItem baseItem = ItemRegistry.getInstance().getBaseItem(gemId);
        if (baseItem == null) {
            return new Result<>(itemStack, false, "Không phải vật phẩm? (BASE_ITEM)", gemId);
        }

        GemDataComponent gemData = baseItem.getComponent(ComponentKeys.GEM_DATA);
        if (gemData == null) {
            return new Result<>(itemStack, false, "Vật phẩm dùng để khảm không phải là ngọc", gemId);
        }

        // 3. Check Slot Compatibility
        EquipAttributeComponent itemAttr = smpItem.getComponent(ComponentKeys.ATTRIBUTE);
        if (itemAttr == null || !gemData.getAttributes().containsKey(itemAttr.getSlot())) {
            return new Result<>(itemStack, false, "Ngọc không đúng loại", gemId);
        }

        // 4. Check Capacity
        if (!gemSocketComponent.canFitGem()) {
            return new Result<>(itemStack, false, "Vật phẩm không đủ ô ngọc", gemId);
        }

        // 5. Success Logic
        gemSocketComponent.addGem(baseItem);
        return new Result<>(smpItem.generateItemStack(player, itemStack.getAmount()), true, "Khảm ngọc thành công", gemId);
    }

    public static Result<String> removeGem(ItemStack itemStack, SmpPlayer player, String gemId) {
        SmpItem smpItem = SmpItem.wrap(itemStack, player);
        GemSocketComponent gemSocketComponent = smpItem.getComponent(ComponentKeys.GEM_SOCKET);

        // 1. Check if the item can even hold gems
        if (gemSocketComponent == null) {
            return new Result<>(itemStack, false, "Vật phẩm được khảm không thể khảm ngọc", gemId);
        }

        gemSocketComponent.removeGem(gemId);
        ItemStack resultStack = smpItem.generateItemStack(player, itemStack.getAmount());

        return new Result<>(resultStack, true, "Gỡ ngọc thành công", gemId);
    }

    public static Result<Map<Enchants, Integer>> addEnchant(ItemStack itemStack, SmpPlayer player, Map<Enchants, Integer> enchantsData) {
        SmpItem smpItem = SmpItem.wrap(itemStack, player);
        EnchantComponent enchantComponent = smpItem.getComponent(ComponentKeys.ENCHANT);

        if (enchantComponent == null) {
            return new Result<>(itemStack, false, "Không thể phù phép vật phẩm này", null);
        }

        // Apply the logic
        enchantComponent.addPersistentEnchant(new EnchantComponent.Modifier(enchantsData));

        ItemStack resultStack = smpItem.generateItemStack(player, itemStack.getAmount());

        return new Result<>(
                resultStack,
                true,
                "Phù phép thành công",
                enchantsData
        );
    }
}
