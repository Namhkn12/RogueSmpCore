package com.roguesmp.block.data;

import com.roguesmp.codec.Codec;
import net.kyori.adventure.key.Key;

public record CokeOvenData(Key modelActive) {

    public static final CokeOvenData DEFAULT = new CokeOvenData(Key.key("stone"));

    public static final Codec<CokeOvenData> CODEC = Codec.composite(
            Codec.KEY.optionalFieldOf("model_active", DEFAULT.modelActive).forGetter(CokeOvenData::modelActive),
            CokeOvenData::new
    );

}
