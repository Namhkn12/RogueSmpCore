package com.roguesmp.item;

import com.roguesmp.codec.Codec;

/**
 * Used in ItemTypeComponent. An item can be many types at once based on tags.
 */
public record ItemType(String tagId, String displayText) {

    public static final Codec<ItemType> CODEC = Codec.composite(
            Codec.STRING.fieldOf("tag").forGetter(ItemType::tagId),
            Codec.STRING.fieldOf("display").forGetter(ItemType::displayText),
            ItemType::new
    );
}
