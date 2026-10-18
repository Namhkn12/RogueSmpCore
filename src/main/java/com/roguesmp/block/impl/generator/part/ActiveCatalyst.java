package com.roguesmp.block.impl.generator.part;

import com.roguesmp.codec.Codec;

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

    public void extend(int amount) {
        remaining += amount;
    }

    public boolean advance() {
        return --remaining <= 0;
    }
}
