package com.roguesmp.block.upgrade.stat;

import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class StatSheet {

    public static final StatSheet EMPTY = new StatSheet(List.of());

    private static final double EPSILON = 1e-9;

    private final Map<BlockStat, Double> percentSums = new EnumMap<>(BlockStat.class);
    private final Map<BlockStat, Double> totalProducts = new EnumMap<>(BlockStat.class);
    private final Map<BlockStat, Double> flatSums = new EnumMap<>(BlockStat.class);

    public StatSheet(List<StatModifier> modifiers) {
        for (StatModifier modifier : modifiers) {
            switch (modifier.operation()) {
                case MULTIPLY_BASE -> percentSums.merge(modifier.stat(), modifier.amount(), Double::sum);
                case MULTIPLY_TOTAL -> totalProducts.merge(modifier.stat(), Math.max(0, 1 + modifier.amount()), (a, b) -> a * b);
                case ADD -> flatSums.merge(modifier.stat(), modifier.amount(), Double::sum);
            }
        }
    }

    public double flatSum(BlockStat stat) {
        return flatSums.getOrDefault(stat, 0.0);
    }

    public double multiplier(BlockStat stat) {
        return curve(percentSums.getOrDefault(stat, 0.0)) * totalProducts.getOrDefault(stat, 1.0);
    }

    public double apply(BlockStat stat, double base) {
        return Math.max(0, base + flatSum(stat)) * multiplier(stat);
    }

    public int applyInt(BlockStat stat, int base) {
        return Math.max(base > 0 ? 1 : 0, (int) Math.round(apply(stat, base)));
    }

    public double applyChance(BlockStat stat, double chance) {
        double flatChance = Math.clamp(chance + flatSum(stat), 0.0, 1.0);
        if (flatChance <= 0 || flatChance >= 1) return flatChance;

        double odds = flatChance / (1 - flatChance) * multiplier(stat);
        return odds / (1 + odds);
    }

    public List<Component> describe() {
        List<Component> lines = new ArrayList<>();
        for (BlockStat stat : BlockStat.values()) {
            double flat = flatSum(stat);
            double percent = percentSums.getOrDefault(stat, 0.0);
            double total = totalProducts.getOrDefault(stat, 1.0);

            List<String> changes = new ArrayList<>();
            if (Math.abs(flat) > EPSILON) changes.add(ModifierOperation.ADD.format(flat));
            if (Math.abs(percent) > EPSILON) changes.add(ModifierOperation.MULTIPLY_BASE.format(percent));
            if (!changes.isEmpty()) lines.add(Utils.text(stat.label() + ": " + String.join(" ", changes), color(stat, flat + percent > 0)));
            if (Math.abs(total - 1) > EPSILON) lines.add(Utils.text(stat.label() + ": ×" + Utils.formatDecimal(total), color(stat, total > 1)));
        }
        return lines;
    }

    private static NamedTextColor color(BlockStat stat, boolean increased) {
        return increased == stat.higherIsBetter() ? NamedTextColor.GREEN : NamedTextColor.RED;
    }

    private static double curve(double percent) {
        return percent >= 0 ? 1 + percent : 1 / (1 - percent);
    }
}
