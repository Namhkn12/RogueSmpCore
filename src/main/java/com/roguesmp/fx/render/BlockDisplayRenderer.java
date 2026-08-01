package com.roguesmp.fx.render;

import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.joml.Vector3f;

/** Renders a shape as real {@link BlockDisplay} entities, one per point. */
public final class BlockDisplayRenderer extends AbstractDisplayRenderer<BlockDisplay> {

    private final BlockData blockData;

    public BlockDisplayRenderer(BlockData blockData) {
        this(blockData, new Vector3f(0.3f, 0.3f, 0.3f));
    }

    public BlockDisplayRenderer(BlockData blockData, Vector3f displaySize) {
        this(blockData, displaySize, 3);
    }

    public BlockDisplayRenderer(BlockData blockData, Vector3f displaySize, int interpolationTicks) {
        super(BlockDisplay.class, displaySize, interpolationTicks);
        this.blockData = blockData;
    }

    @Override
    protected void configure(BlockDisplay display) {
        display.setBlock(blockData);
        display.setBillboard(Display.Billboard.FIXED);
    }
}
