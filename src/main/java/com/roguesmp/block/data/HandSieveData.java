package com.roguesmp.block.data;

import com.roguesmp.codec.Codec;

public record HandSieveData(int tier, int progressPerClick, double itemHeight) {

    public static final HandSieveData DEFAULT = new HandSieveData(1, 10, 0.6);

    public static final Codec<HandSieveData> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("tier", DEFAULT.tier).forGetter(HandSieveData::tier),
            Codec.INT.optionalFieldOf("progress_per_click", DEFAULT.progressPerClick).forGetter(HandSieveData::progressPerClick),
            Codec.DOUBLE.optionalFieldOf("item_height", DEFAULT.itemHeight).forGetter(HandSieveData::itemHeight),
            HandSieveData::new
    );
}
