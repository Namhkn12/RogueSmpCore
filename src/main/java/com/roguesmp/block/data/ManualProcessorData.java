package com.roguesmp.block.data;

import com.roguesmp.codec.Codec;

public record ManualProcessorData(int tier, int progressPerClick, double itemHeight) {

    public static final ManualProcessorData DEFAULT = new ManualProcessorData(1, 10, 0.6);

    public static final Codec<ManualProcessorData> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("tier", DEFAULT.tier).forGetter(ManualProcessorData::tier),
            Codec.INT.optionalFieldOf("progress_per_click", DEFAULT.progressPerClick).forGetter(ManualProcessorData::progressPerClick),
            Codec.DOUBLE.optionalFieldOf("item_height", DEFAULT.itemHeight).forGetter(ManualProcessorData::itemHeight),
            ManualProcessorData::new
    );
}
