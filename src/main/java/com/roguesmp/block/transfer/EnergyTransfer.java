package com.roguesmp.block.transfer;

import com.roguesmp.block.BlockPos;
import com.roguesmp.block.EnergyStorage;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.manager.BlockManager;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class EnergyTransfer {

    private EnergyTransfer() {}

    public static int distribute(EnergyStorage source, Block origin, int budget) {
        int available = Math.min(budget, source.getEnergy());
        if (available <= 0) return 0;

        List<EnergyStorage> receivers = new ArrayList<>();
        for (BlockFace face : SideConfig.FACES) {
            EnergyStorage neighbour = findReceiver(origin.getRelative(face));
            if (neighbour != null && neighbour.getEnergy() < neighbour.getMaxEnergy()) receivers.add(neighbour);
        }

        return split(source, receivers, available);
    }

    public static int split(EnergyStorage source, List<EnergyStorage> receivers, int budget) {
        int available = Math.min(budget, source.getEnergy());
        if (available <= 0) return 0;

        int moved = 0;
        for (int i = 0; i < receivers.size(); i++) {
            int share = (available - moved) / (receivers.size() - i);
            if (share <= 0) continue;
            moved += receivers.get(i).addEnergy(share);
        }
        source.removeEnergy(moved);
        return moved;
    }

    public static @Nullable EnergyStorage findReceiver(Block block) {
        if (!block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) return null;

        SmpBlock custom = BlockManager.getInstance().get(BlockPos.of(block));
        return custom instanceof EnergyStorage storage && storage.canReceiveEnergy() ? storage : null;
    }
}
