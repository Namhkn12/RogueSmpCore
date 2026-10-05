package com.roguesmp.block.impl.generator.stat;

import com.roguesmp.utils.Utils;

public enum ModifierOperation {
    MULTIPLY_BASE,
    MULTIPLY_TOTAL,
    ADD;

    public String format(double amount) {
        return switch (this) {
            case MULTIPLY_BASE -> sign(amount) + Utils.formatDecimal(amount * 100) + "%";
            case MULTIPLY_TOTAL -> "×" + Utils.formatDecimal(1 + amount);
            case ADD -> sign(amount) + Utils.formatDecimal(amount);
        };
    }

    private static String sign(double amount) {
        return amount > 0 ? "+" : "";
    }
}
