package com.roguesmp.block.upgrade;

import com.roguesmp.codec.Codec;
import com.roguesmp.item.component.impl.BlockUpgradeComponent;
import org.jetbrains.annotations.Nullable;

public final class ActiveCatalyst {

    public static final Codec<ActiveCatalyst> CODEC = Codec.composite(
            Codec.STRING.fieldOf("item").forGetter(ActiveCatalyst::item),
            Codec.INT.fieldOf("remaining").forGetter(ActiveCatalyst::remaining),
            ActiveCatalyst::new
    );

    private final String item;
    private int remaining;

    public ActiveCatalyst(String item, int remaining) {
        this.item = item;
        this.remaining = remaining;
    }

    public String item() {
        return item;
    }

    public int remaining() {
        return remaining;
    }

    public @Nullable BlockUpgradeComponent component() {
        return BlockUpgradeComponent.of(item);
    }

    public boolean countDown() {
        return --remaining <= 0;
    }
}
