package com.roguesmp.block;

import com.roguesmp.codec.Codec;
import com.roguesmp.utils.Utils;

/**
 * How much experience a block gives back when broken, rolled independently of its item
 * {@link BlockDrop}s.
 */
public record BlockExperience(int minAmount, int maxAmount, double chance) {

    public static final BlockExperience NONE = new BlockExperience(0, 0, 0.0);

    public static final Codec<BlockExperience> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("min_amount", 0).forGetter(BlockExperience::minAmount),
            Codec.INT.optionalFieldOf("max_amount", 0).forGetter(BlockExperience::maxAmount),
            Codec.DOUBLE.optionalFieldOf("chance", 1.0).forGetter(BlockExperience::chance),
            BlockExperience::new
    );

    public int roll() {
        if (maxAmount <= 0 || Utils.RANDOM.nextDouble() >= chance) return 0;
        return minAmount + (maxAmount > minAmount ? Utils.RANDOM.nextInt(maxAmount - minAmount + 1) : 0);
    }
}
