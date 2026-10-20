package com.roguesmp.gate;

import com.roguesmp.item.BaseItem;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Holder;
import com.roguesmp.utils.ItemStackUtils;
import com.roguesmp.utils.SmpItemUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class GateInventoryUtils {

    private GateInventoryUtils() {
    }

    public static int count(SmpPlayer player, Holder<BaseItem> item) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null || !item.isBound()) return 0;

        int count = 0;
        for (ItemStack stack : bukkitPlayer.getInventory().getStorageContents()) {
            if (matches(stack, item.value())) count += stack.getAmount();
        }
        return count;
    }

    public static void remove(Inventory inventory, Holder<BaseItem> item, int amount) {
        if (!item.isBound()) return;

        int left = amount;
        int storageSize = inventory.getStorageContents().length;
        for (int i = 0; i < storageSize && left > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!matches(stack, item.value())) continue;

            int taken = Math.min(left, stack.getAmount());
            left -= taken;
            if (taken == stack.getAmount()) inventory.setItem(i, null);
            else stack.setAmount(stack.getAmount() - taken);
        }
    }

    private static boolean matches(ItemStack stack, BaseItem item) {
        return ItemStackUtils.isValidItem(stack) && item.equals(SmpItemUtils.getBaseItem(stack));
    }
}
