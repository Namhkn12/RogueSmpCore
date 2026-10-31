package com.roguesmp.block.impl;

import com.roguesmp.block.BlockProperties;
import com.roguesmp.block.SmpBlock;
import com.roguesmp.block.data.PaletteMarkerData;
import com.roguesmp.block.manager.BlockManager;
import com.roguesmp.dungeon.DungeonInstance;
import com.roguesmp.dungeon.DungeonManager;
import com.roguesmp.dungeon.feature.BlockPalettesFeature;
import com.roguesmp.utils.Utils;
import org.bukkit.World;
import org.bukkit.block.Block;

public class PaletteMarkerBlock extends SmpBlock {

    private final String palette;

    public PaletteMarkerBlock(BlockProperties properties, PaletteMarkerData data) {
        super(properties);
        this.palette = data.palette();
    }

    @Override
    public void onHydrated() {
        World world = getPos().getWorld();
        if (world == null) return;
        DungeonInstance instance = DungeonManager.getInstance().getByWorld(world);
        if (instance != null) Utils.runLater(() -> replace(instance));
    }

    public void replace(DungeonInstance instance) {
        BlockManager manager = BlockManager.getInstance();
        if (manager.get(getPos()) != this) return;

        Block target = getPos().getBlock();
        if (target == null) return;
        manager.breakBlock(target, null, false, false);
        BlockPalettesFeature.replace(instance, palette, target);
    }
}
