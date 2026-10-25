package com.roguesmp.quest.goal;

import com.roguesmp.codec.Codec;

import java.util.SplittableRandom;

/**
 * An inclusive amount range, written as a bare number or as {@code {"min": 10, "max": 20}}.
 */
public record IntRange(int min, int max) {

    public static final Codec<IntRange> CODEC = Codec.withAlternative(
            Codec.composite(
                    Codec.INT.fieldOf("min").forGetter(IntRange::min),
                    Codec.INT.fieldOf("max").forGetter(IntRange::max),
                    IntRange::of
            ),
            Codec.INT.xmap(IntRange::exactly, IntRange::min)
    );

    public static IntRange of(int a, int b) {
        return new IntRange(Math.min(a, b), Math.max(a, b));
    }

    public static IntRange exactly(int value) {
        return new IntRange(value, value);
    }

    public int roll(long seed) {
        return min == max ? min : new SplittableRandom(seed).nextInt(min, max + 1);
    }

    public String display() {
        return min == max ? String.valueOf(min) : min + "-" + max;
    }
}
