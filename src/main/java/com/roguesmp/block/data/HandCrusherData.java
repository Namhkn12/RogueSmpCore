package com.roguesmp.block.data;

import com.roguesmp.codec.Codec;

public record HandCrusherData(int tier, int progressPerClick, double itemHeight) {

    public static final HandCrusherData DEFAULT = new HandCrusherData(1, 10, 0.6);

    public static final Codec<HandCrusherData> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("tier", DEFAULT.tier).forGetter(HandCrusherData::tier),
            Codec.INT.optionalFieldOf("progress_per_click", DEFAULT.progressPerClick).forGetter(HandCrusherData::progressPerClick),
            Codec.DOUBLE.optionalFieldOf("item_height", DEFAULT.itemHeight).forGetter(HandCrusherData::itemHeight),
            HandCrusherData::new
    );
}
