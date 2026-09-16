package com.roguesmp.fx.render;

import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.joml.Vector3f;

import java.util.function.BiConsumer;

/** Renders a shape as real {@link BlockDisplay} entities, one per point. */
public final class BlockDisplayRenderer extends AbstractDisplayRenderer<BlockDisplay> {

    public BlockDisplayRenderer(BlockData blockData) {
        this(blockData, new Vector3f(0.3f, 0.3f, 0.3f));
    }

    public BlockDisplayRenderer(BlockData blockData, Vector3f displaySize) {
        this(blockData, displaySize, 3);
    }

    public BlockDisplayRenderer(BlockData blockData, Vector3f displaySize, int interpolationTicks) {
        this(displaySize, interpolationTicks, (display, point) -> display.setBlock(blockData));
    }

    /** Full control: {@code onRender} is called every tick, per point, to set the block and/or nudge {@link FxPoint#position()}. */
    public BlockDisplayRenderer(Vector3f displaySize, int interpolationTicks, BiConsumer<BlockDisplay, FxPoint> onRender) {
        super(BlockDisplay.class, displaySize, interpolationTicks, onRender);
    }

    @Override
    protected void configure(BlockDisplay display) {
        display.setBillboard(Display.Billboard.FIXED);
    }
}
