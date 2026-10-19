package com.roguesmp.brewing;

import com.roguesmp.codec.Codec;

public record StatRange(double min, double max) {

    public static final Codec<StatRange> CODEC = Codec.withAlternative(
            Codec.composite(
                    Codec.DOUBLE.fieldOf("min").forGetter(StatRange::min),
                    Codec.DOUBLE.fieldOf("max").forGetter(StatRange::max),
                    StatRange::new
            ),
            Codec.DOUBLE.xmap(value -> new StatRange(value, value), StatRange::min)
    );

    public StatRange {
        if (min > max) {
            double swap = min;
            min = max;
            max = swap;
        }
    }

    public boolean isFixed() {
        return min == max;
    }

    public double at(double fraction) {
        return min + fraction * (max - min);
    }
}
