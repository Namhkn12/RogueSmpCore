package com.roguesmp.block.data;

import com.roguesmp.codec.Codec;

public record WirelessNodeData(int throughput) {

    public static final WirelessNodeData DEFAULT = new WirelessNodeData(40);

    public static final Codec<WirelessNodeData> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("throughput", DEFAULT.throughput()).forGetter(WirelessNodeData::throughput),
            WirelessNodeData::new
    );
}
