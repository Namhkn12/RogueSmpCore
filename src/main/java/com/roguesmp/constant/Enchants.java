package com.roguesmp.constant;

import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.enchant.impl.Greed;

public enum Enchants {
    GREED(new Greed()),
    SHARPNESS(new Greed()),
    CRITICAL(new Greed())
    ;

    private final SmpEnchant enchant;

    Enchants(SmpEnchant enchant) {
        this.enchant = enchant;
    }

    public SmpEnchant getEnchant() {
        return enchant;
    }
}
