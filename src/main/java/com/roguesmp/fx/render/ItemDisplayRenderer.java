package com.roguesmp.fx.render;

import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.joml.Vector3f;

/** Renders a shape as real {@link ItemDisplay} entities, one per point. */
public final class ItemDisplayRenderer extends AbstractDisplayRenderer<ItemDisplay> {

    private final ItemStack item;

    public ItemDisplayRenderer(ItemStack item) {
        this(item, new Vector3f(0.5f, 0.5f, 0.5f));
    }

    public ItemDisplayRenderer(ItemStack item, Vector3f displaySize) {
        this(item, displaySize, 3);
    }

    public ItemDisplayRenderer(ItemStack item, Vector3f displaySize, int interpolationTicks) {
        super(ItemDisplay.class, displaySize, interpolationTicks);
        this.item = item;
    }

    @Override
    protected void configure(ItemDisplay display) {
        display.setItemStack(item);
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        display.setBillboard(Display.Billboard.FIXED);
    }
}
