package com.roguesmp.block.impl.generator;

import com.roguesmp.block.BlockPos;
import com.roguesmp.block.BlockVisual;
import com.roguesmp.block.manager.BlockManager;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class CrackDisplay {

    private @Nullable UUID overlayId;
    private int shownStage = -1;

    public void show(Block target) {
        if (isCustom(target)) {
            overlayId = BlockVisual.spawnCrackOverlay(BlockPos.of(target), 0).getUniqueId();
        } else {
            BlockVisual.sendBlockDamage(BlockPos.of(target), 0);
        }
        shownStage = 0;
    }

    public void update(Block target, int stage) {
        if (stage == shownStage) return;
        shownStage = stage;

        if (overlayId != null) {
            Entity overlay = Bukkit.getEntity(overlayId);
            if (overlay != null) BlockVisual.updateCrackOverlay(overlay, stage);
        } else {
            BlockVisual.sendBlockDamage(BlockPos.of(target), stage);
        }
    }

    public void remove(Block target) {
        if (shownStage == -1) return;

        if (overlayId != null) {
            Entity overlay = Bukkit.getEntity(overlayId);
            if (overlay != null) overlay.remove();
            overlayId = null;
        } else {
            BlockVisual.sendBlockDamage(BlockPos.of(target), -1);
        }
        shownStage = -1;
    }

    private boolean isCustom(Block target) {
        return BlockManager.getInstance().isSmpBlock(target.getLocation());
    }
}
