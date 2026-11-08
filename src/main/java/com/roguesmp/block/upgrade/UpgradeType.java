package com.roguesmp.block.upgrade;

import com.roguesmp.codec.Codec;

public enum UpgradeType {
    MODULE("Mô-đun"),
    CATALYST("Chất xúc tác");

    public static final Codec<UpgradeType> CODEC = Codec.enumOf(UpgradeType.class);

    private final String displayName;

    UpgradeType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
