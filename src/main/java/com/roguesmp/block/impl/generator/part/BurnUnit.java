package com.roguesmp.block.impl.generator.part;

import com.roguesmp.utils.Utils;

public enum BurnUnit {
    TICKS,
    HARVESTS;

    public String format(int amount) {
        return this == TICKS ? Utils.formatDecimal(amount / 20d) + "s" : amount + " lượt khai thác";
    }
}
