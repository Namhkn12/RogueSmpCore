package com.roguesmp.block.impl.generator.fuel;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

public record FuelType(String tagId, String displayText) {

    public static final Codec<FuelType> CODEC = Codec.composite(
            Codec.STRING.fieldOf("tag").forGetter(FuelType::tagId),
            Codec.STRING.fieldOf("display").forGetter(FuelType::displayText),
            FuelType::new
    );

    public boolean matches(String itemId) {
        return Registries.ITEM.getHolder(itemId).getTagIds().contains(tagId);
    }
}
