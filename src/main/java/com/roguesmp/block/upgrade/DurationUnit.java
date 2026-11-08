package com.roguesmp.block.upgrade;

import com.roguesmp.utils.Utils;

public enum DurationUnit {
    TICKS,
    CYCLES;

    public String format(int amount) {
        return this == TICKS ? Utils.formatDecimal(amount / 20d) + "s" : amount + " lượt hoạt động";
    }
}
