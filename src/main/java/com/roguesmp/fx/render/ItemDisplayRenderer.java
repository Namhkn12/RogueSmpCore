package com.roguesmp.fx.render;

import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.joml.Vector3f;

import java.util.function.BiConsumer;

/** Renders a shape as real {@link ItemDisplay} entities, one per point. */
public final class ItemDisplayRenderer extends AbstractDisplayRenderer<ItemDisplay> {

    public ItemDisplayRenderer(ItemStack item) {
        this(item, new Vector3f(0.5f, 0.5f, 0.5f));
    }

    public ItemDisplayRenderer(ItemStack item, Vector3f displaySize) {
        this(item, displaySize, 3);
    }

    public ItemDisplayRenderer(ItemStack item, Vector3f displaySize, int interpolationTicks) {
        this(displaySize, interpolationTicks, (display, point) -> display.setItemStack(item));
    }

    /** Full control: {@code onRender} is called every tick, per point, to set the item and/or nudge {@link FxPoint#position()}. */
    public ItemDisplayRenderer(Vector3f displaySize, int interpolationTicks, BiConsumer<ItemDisplay, FxPoint> onRender) {
        super(ItemDisplay.class, displaySize, interpolationTicks, onRender);
    }

    @Override
    protected void configure(ItemDisplay display) {
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        display.setBillboard(Display.Billboard.FIXED);
    }
}
