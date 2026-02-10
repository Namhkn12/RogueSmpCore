package com.roguesmp.constant;

import com.google.gson.annotations.SerializedName;
import com.roguesmp.enchant.SmpEnchant;
import com.roguesmp.enchant.impl.Greed;

public enum Enchants {
    @SerializedName("greed")
    GREED(new Greed()),
    @SerializedName("sharpness")
    SHARPNESS(new Greed()),
    @SerializedName("critical")
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
