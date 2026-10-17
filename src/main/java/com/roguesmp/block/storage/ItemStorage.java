package com.roguesmp.block.storage;

import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.StoredItem;
import com.roguesmp.block.persistence.StateSection;
import com.roguesmp.block.transfer.ItemHandler;
import com.roguesmp.block.transfer.SideConfig;
import com.roguesmp.block.transfer.SideMode;
import com.roguesmp.block.transfer.SideTransfer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public final class ItemStorage implements ItemHandler {

    private static final int TRANSFER_INTERVAL = 10;
    private static final int TRANSFER_AMOUNT = 8;

    private final SmpBlock owner;
    private final List<SlotGroup> groups = new ArrayList<>();
    private final SideConfig sides = new SideConfig();
    private Supplier<Set<BlockFace>> lockedFaces = Set::of;
    private int transferTicks;

    public ItemStorage(SmpBlock owner) {
        this.owner = owner;
    }

    public <G extends SlotGroup> G add(G group) {
        return add(group, () -> {});
    }

    public <G extends SlotGroup> G add(G group, Runnable onChange) {
        group.onChange(() -> {
            owner.markDirty();
            onChange.run();
        });
        groups.add(group);
        return group;
    }

    public ItemStorage lockFaces(Supplier<Set<BlockFace>> lockedFaces) {
        this.lockedFaces = lockedFaces;
        return this;
    }

    public boolean isLocked(BlockFace face) {
        return lockedFaces.get().contains(face);
    }

    public SideMode side(BlockFace face) {
        return isLocked(face) ? SideMode.NONE : sides.get(face);
    }

    public void setSide(BlockFace face, SideMode mode) {
        if (isLocked(face)) return;
        sides.set(face, mode);
        owner.markDirty();
    }

    public void tick() {
        if (sides.isEmpty() || ++transferTicks < TRANSFER_INTERVAL) return;
        transferTicks = 0;

        Block origin = owner.getLocation().getBlock();
        for (BlockFace face : SideConfig.FACES) {
            SideMode mode = side(face);
            if (mode == SideMode.NONE) continue;

            ItemHandler neighbour = SideTransfer.find(origin.getRelative(face));
            if (neighbour == null) continue;

            if (mode.pulls()) {
                if (mode.isSmart()) SideTransfer.smartPull(neighbour, this, TRANSFER_AMOUNT);
                else SideTransfer.move(neighbour, this, TRANSFER_AMOUNT);
            }
            if (mode.pushes()) SideTransfer.move(this, neighbour, TRANSFER_AMOUNT);
        }
    }

    /** Inserts into whichever groups a player may fill, returning how many were accepted. */
    @Override
    public int insert(StoredItem item, boolean simulate) {
        int remaining = item.amount();
        for (SlotGroup group : groups) {
            if (remaining <= 0) break;
            if (!group.access().allowsInsert()) continue;
            remaining -= group.insert(item.withAmount(remaining), simulate);
        }
        return item.amount() - remaining;
    }

    @Override
    public int slots() {
        int total = 0;
        for (SlotGroup group : groups) {
            if (group.access().allowsExtract()) total += group.size();
        }
        return total;
    }

    @Override
    public @Nullable StoredItem get(int slot) {
        for (SlotGroup group : groups) {
            if (!group.access().allowsExtract()) continue;
            if (slot < group.size()) return group.get(slot);
            slot -= group.size();
        }
        return null;
    }

    @Override
    public @Nullable StoredItem extract(int slot, int amount, boolean simulate) {
        for (SlotGroup group : groups) {
            if (!group.access().allowsExtract()) continue;
            if (slot < group.size()) {
                StoredItem stored = group.get(slot);
                return stored == null ? null : group.extract(slot, Math.min(amount, stored.maxStackSize()), simulate);
            }
            slot -= group.size();
        }
        return null;
    }

    @Override
    public List<StoredItem> demand() {
        List<StoredItem> demand = new ArrayList<>();
        for (SlotGroup group : groups) demand.addAll(group.demand());
        return demand;
    }

    public List<StoredItem> drain() {
        List<StoredItem> drained = new ArrayList<>();
        for (SlotGroup group : groups) drained.addAll(group.drain());
        return drained;
    }

    public void collectSections(List<StateSection<?>> sections) {
        for (SlotGroup group : groups) sections.add(group.section());
        sections.add(sides.section());
    }
}
