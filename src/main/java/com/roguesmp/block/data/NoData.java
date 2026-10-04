package com.roguesmp.block.data;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;

/** This block hold no "data" field */
public record NoData() {

    public static final NoData INSTANCE = new NoData();
    public static final Codec<NoData> CODEC = MapCodec.unit(() -> INSTANCE);
}
