package com.roguesmp.gate;

import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.ItemRef;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class GateInventoryUtils {

    private GateInventoryUtils() {
    }

    public static int count(SmpPlayer player, String itemId) {
        Player bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer == null) return 0;

        int count = 0;
        for (ItemStack stack : bukkitPlayer.getInventory().getStorageContents()) {
            if (itemId.equals(ItemRef.idOf(stack))) count += stack.getAmount();
        }
        return count;
    }

    public static void remove(Inventory inventory, String itemId, int amount) {
        int left = amount;
        int storageSize = inventory.getStorageContents().length;
        for (int i = 0; i < storageSize && left > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!itemId.equals(ItemRef.idOf(stack))) continue;

            int taken = Math.min(left, stack.getAmount());
            left -= taken;
            if (taken == stack.getAmount()) inventory.setItem(i, null);
            else stack.setAmount(stack.getAmount() - taken);
        }
    }
}
