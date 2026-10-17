package com.roguesmp.block.transfer;

import com.roguesmp.block.StoredItem;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Exposes a vanilla container. Stacks a {@link StoredItem} can't rebuild exactly (renamed, enchanted, unique) are never moved. */
public record ContainerHandler(Inventory inventory) implements ItemHandler {

    @Override
    public int slots() {
        return inventory.getSize();
    }

    @Override
    public @Nullable StoredItem get(int slot) {
        ItemStack stack = inventory.getItem(slot);
        return isPlain(stack) ? StoredItem.of(stack) : null;
    }

    @Override
    public @Nullable StoredItem extract(int slot, int amount, boolean simulate) {
        ItemStack stack = inventory.getItem(slot);
        if (amount <= 0 || !isPlain(stack)) return null;

        StoredItem stored = StoredItem.of(stack);
        int taken = Math.min(amount, stored.amount());
        if (!simulate) {
            int left = stack.getAmount() - taken;
            ItemStack remaining = left > 0 ? stack.clone() : null;
            if (remaining != null) remaining.setAmount(left);
            inventory.setItem(slot, remaining);
        }
        return stored.withAmount(taken);
    }

    @Override
    public int insert(StoredItem item, boolean simulate) {
        ItemStack template = item.withAmount(1).toItemStack();
        if (template == null) return 0;

        int maxStack = Math.min(item.maxStackSize(), inventory.getMaxStackSize());
        int remaining = item.amount();
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (int slot = 0; slot < inventory.getSize() && remaining > 0; slot++) {
                ItemStack existing = inventory.getItem(slot);
                boolean empty = existing == null || existing.getType().isAir();
                if (pass == 0 ? empty || !existing.isSimilar(template) : !empty) continue;

                int current = empty ? 0 : existing.getAmount();
                int added = Math.min(remaining, maxStack - current);
                if (added <= 0) continue;

                if (!simulate) {
                    ItemStack placed = template.clone();
                    placed.setAmount(current + added);
                    inventory.setItem(slot, placed);
                }
                remaining -= added;
            }
        }
        return item.amount() - remaining;
    }

    private static boolean isPlain(@Nullable ItemStack stack) {
        StoredItem stored = StoredItem.of(stack);
        if (stored == null) return false;

        ItemStack rebuilt = stored.withAmount(1).toItemStack();
        return rebuilt != null && rebuilt.isSimilar(stack);
    }
}
