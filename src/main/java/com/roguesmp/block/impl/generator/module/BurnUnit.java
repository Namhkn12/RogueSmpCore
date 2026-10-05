package com.roguesmp.block.impl.generator.module;

import com.roguesmp.utils.Utils;

public enum BurnUnit {
    TICKS,
    HARVESTS;

    public String format(int amount) {
        return this == TICKS ? Utils.formatDecimal(amount / 20d) + "s" : amount + " lượt khai thác";
    }
}
