package com.roguesmp.block;

import com.roguesmp.codec.Codec;

import java.util.function.BiFunction;

/**
 * A type of custom block. Declare instances as constants in {@link BlockTypes}.
 * Many block ids can share one type.
 */
public final class BlockType<D, B extends SmpBlock> {

    private final String key;
    private final BiFunction<BlockProperties, D, B> factory;
    private final Codec<BlockData> dataCodec;

    @SuppressWarnings("unchecked")
    BlockType(String key, Codec<D> dataCodec, D defaults, BiFunction<BlockProperties, D, B> factory) {
        this.key = key;
        this.factory = factory;
        this.dataCodec = Codec.composite(
                BlockProperties.CODEC.forGetter(BlockData::properties),
                dataCodec.optionalFieldOf("data", defaults).forGetter((BlockData file) -> (D) file.data()),
                (properties, data) -> new BlockData(this, properties, data)
        );
    }

    /** The value of {@code "type"} in a block's JSON that selects this type. */
    public String key() {
        return key;
    }

    Codec<BlockData> dataCodec() {
        return dataCodec;
    }

    public B create(BlockProperties properties, D data) {
        return factory.apply(properties, data);
    }

    @SuppressWarnings("unchecked")
    SmpBlock create(BlockData data) {
        return factory.apply(data.properties(), (D) data.data());
    }
}
