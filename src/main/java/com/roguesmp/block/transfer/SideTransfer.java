package com.roguesmp.block.transfer;

import com.roguesmp.block.BlockPos;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.StoredItem;
import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.block.storage.ItemStorageHolder;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.jetbrains.annotations.Nullable;

public final class SideTransfer {

    private SideTransfer() {}

    public static @Nullable ItemHandler find(Block block) {
        if (!block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) return null;

        SmpBlock custom = BlockManager.getInstance().get(BlockPos.of(block));
        if (custom != null) return custom instanceof ItemStorageHolder holder ? holder.itemStorage() : null;
        return block.getState(false) instanceof Container container ? new ContainerHandler(container.getInventory()) : null;
    }

    /** Moves up to {@code budget} items, returning how many moved. */
    public static int move(ItemHandler from, ItemHandler to, int budget) {
        int moved = 0;
        for (int slot = 0; slot < from.slots() && moved < budget; slot++) {
            moved += moveSlot(from, slot, to, budget - moved);
        }
        return moved;
    }

    /** Pulls only what {@code to} demands, returning how many moved. */
    public static int smartPull(ItemHandler from, ItemHandler to, int budget) {
        int moved = 0;
        for (StoredItem wanted : to.demand()) {
            int missing = wanted.amount();
            for (int slot = 0; slot < from.slots() && missing > 0 && moved < budget; slot++) {
                StoredItem offered = from.get(slot);
                if (offered == null || !offered.item().equals(wanted.item())) continue;

                int count = moveSlot(from, slot, to, Math.min(missing, budget - moved));
                missing -= count;
                moved += count;
            }
            if (moved >= budget) break;
        }
        return moved;
    }

    private static int moveSlot(ItemHandler from, int slot, ItemHandler to, int limit) {
        StoredItem offered = from.extract(slot, limit, true);
        if (offered == null) return 0;

        int accepted = to.insert(offered, true);
        if (accepted <= 0) return 0;

        StoredItem taken = from.extract(slot, accepted, false);
        if (taken == null) return 0;
        return to.insert(taken, false);
    }
}
