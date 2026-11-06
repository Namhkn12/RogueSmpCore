package com.roguesmp.item;

import com.roguesmp.codec.Codec;

public record ItemTypeGroup(String display, int priority) {

    public static final Codec<ItemTypeGroup> CODEC = Codec.composite(
            Codec.STRING.fieldOf("display").forGetter(ItemTypeGroup::display),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(ItemTypeGroup::priority),
            ItemTypeGroup::new
    );
}
