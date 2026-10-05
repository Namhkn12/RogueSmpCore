package com.roguesmp.block.impl.generator.module;

import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;

public final class ModifierLore {

    private ModifierLore() {
    }

    public static List<Component> describe(List<StatModifier> modifiers) {
        List<Component> lines = new ArrayList<>();
        for (StatModifier modifier : modifiers) {
            lines.add(line(text(modifier), color(modifier.stat(), modifier.amount())));
        }
        return lines;
    }

    private static String text(StatModifier modifier) {
        return modifier.operation().format(modifier.amount(), modifier.stat()) + " " + modifier.stat().label();
    }

    private static NamedTextColor color(GeneratorStat stat, double amount) {
        return (amount > 0) == stat.higherIsBetter() ? NamedTextColor.GREEN : NamedTextColor.RED;
    }

    public static Component line(String text, NamedTextColor color) {
        return Utils.text(text, color).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
