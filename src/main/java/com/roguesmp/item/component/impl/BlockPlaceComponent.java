package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.item.component.ItemComponent;
import org.jetbrains.annotations.NotNull;

/**
 * Marks an item as placing the custom block registered under {@link #getBlockId()}. The item and
 * block ids are independent.
 */
public class BlockPlaceComponent implements ItemComponent {

    public static final Codec<BlockPlaceComponent> CODEC = Codec.composite(
            Codec.STRING.fieldOf("block").forGetter(BlockPlaceComponent::getBlockId),
            BlockPlaceComponent::new
    );

    private final String blockId;

    public BlockPlaceComponent(String blockId) {
        this.blockId = blockId;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    public String getBlockId() {
        return blockId;
    }
}
