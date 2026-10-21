package com.roguesmp.block.data;

import com.roguesmp.codec.Codec;
import net.kyori.adventure.key.Key;

public record WirelessNodeData(int throughput, Key modelActive) {

    public static final WirelessNodeData DEFAULT = new WirelessNodeData(40, Key.key("minecraft:stone"));

    public static final Codec<WirelessNodeData> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("throughput", DEFAULT.throughput()).forGetter(WirelessNodeData::throughput),
            Codec.KEY.optionalFieldOf("model_active", DEFAULT.modelActive()).forGetter(WirelessNodeData::modelActive),
            WirelessNodeData::new
    );
}
