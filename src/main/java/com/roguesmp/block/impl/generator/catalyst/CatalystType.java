package com.roguesmp.block.impl.generator.catalyst;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

public record CatalystType(String tagId, String displayText) {

    public static final Codec<CatalystType> CODEC = Codec.composite(
            Codec.STRING.fieldOf("tag").forGetter(CatalystType::tagId),
            Codec.STRING.fieldOf("display").forGetter(CatalystType::displayText),
            CatalystType::new
    );

    public boolean matches(String itemId) {
        return Registries.ITEM.getHolder(itemId).getTagIds().contains(tagId);
    }
}
