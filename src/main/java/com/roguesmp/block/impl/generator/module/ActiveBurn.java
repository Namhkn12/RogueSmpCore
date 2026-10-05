package com.roguesmp.block.impl.generator.module;

import com.roguesmp.codec.Codec;

public final class ActiveBurn {

    public static final Codec<ActiveBurn> CODEC = Codec.composite(
            Codec.STRING.fieldOf("item").forGetter(ActiveBurn::item),
            Codec.INT.fieldOf("remaining").forGetter(ActiveBurn::remaining),
            ActiveBurn::new
    );

    private final String item;
    private int remaining;

    public ActiveBurn(String item, int remaining) {
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
