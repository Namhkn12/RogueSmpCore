package com.roguesmp.block.impl.generator.module;

import com.roguesmp.block.BlockDrop;
import com.roguesmp.block.StoredItem;
import com.roguesmp.block.data.GeneratorData;
import com.roguesmp.block.impl.generator.module.behavior.GeneratorBehavior;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class GeneratorStats {

    private static final double MIN_MULTIPLIER = 0.1;
    private static final double GUARANTEED_CHANCE = 1.0;
    private static final double EPSILON = 1e-9;

    private final Map<GeneratorStat, Double> baseMultiplierSums = new EnumMap<>(GeneratorStat.class);
    private final Map<GeneratorStat, Double> totalMultipliers = new EnumMap<>(GeneratorStat.class);
    private final Map<GeneratorStat, Double> flatSums = new EnumMap<>(GeneratorStat.class);
    private final List<GeneratorBehavior> behaviors = new ArrayList<>();
    private final int placeDelayTicks;
    private final int breakDelayTicks;
    private final int maxEnergy;
    private final int lootCapacity;
    private final double energyPerTick;

    public GeneratorStats(GeneratorData data, List<GeneratorEffect> effects) {
        for (GeneratorEffect effect : effects) {
            addModifiers(effect.modifiers());
            behaviors.addAll(effect.behaviors());
        }

        placeDelayTicks = applyModifiers(data.placeDelayTicks(), GeneratorStat.PLACE_DELAY);
        breakDelayTicks = applyModifiers(data.breakDelayTicks(), GeneratorStat.BREAK_DELAY);
        maxEnergy = applyModifiers(data.maxEnergy(), GeneratorStat.MAX_ENERGY);
        lootCapacity = applyModifiers(data.lootCapacity(), GeneratorStat.LOOT_CAPACITY);
        energyPerTick = Math.max(0, data.energyPerTick() * multiplier(GeneratorStat.ENERGY_PER_TICK) + flat(GeneratorStat.ENERGY_PER_TICK));
    }

    public @Nullable StoredItem rollDrop(BlockDrop drop) {
        boolean rare = drop.chance() < GUARANTEED_CHANCE;
        double chance = rare ? clampChance(drop.chance() * multiplier(GeneratorStat.RARE_DROP_CHANCE) + flat(GeneratorStat.RARE_DROP_CHANCE)) : drop.chance();
        double amountMultiplier = multiplier(GeneratorStat.DROP_AMOUNT) * (rare ? 1.0 : multiplier(GeneratorStat.COMMON_DROP_AMOUNT));
        double flatAmount = flat(GeneratorStat.DROP_AMOUNT) + (rare ? 0.0 : flat(GeneratorStat.COMMON_DROP_AMOUNT));
        return drop.rollStored(chance, amountMultiplier, flatAmount);
    }

    public List<StatModifier> summary() {
        List<StatModifier> summary = new ArrayList<>();
        for (GeneratorStat stat : GeneratorStat.values()) {
            double change = multiplier(stat) - 1;
            if (Math.abs(change) > EPSILON) summary.add(new StatModifier(stat, ModifierOperation.MULTIPLY_BASE, change));

            double flat = flat(stat);
            if (Math.abs(flat) > EPSILON) summary.add(new StatModifier(stat, ModifierOperation.ADD, flat));
        }
        return summary;
    }

    public List<GeneratorBehavior> behaviors() {
        return behaviors;
    }

    public int placeDelayTicks() {
        return placeDelayTicks;
    }

    public int breakDelayTicks() {
        return breakDelayTicks;
    }

    public int maxEnergy() {
        return maxEnergy;
    }

    public int lootCapacity() {
        return lootCapacity;
    }

    public double energyPerTick() {
        return energyPerTick;
    }

    private void addModifiers(List<StatModifier> modifiers) {
        for (StatModifier modifier : modifiers) {
            switch (modifier.operation()) {
                case MULTIPLY_BASE -> baseMultiplierSums.merge(modifier.stat(), modifier.amount(), Double::sum);
                case MULTIPLY_TOTAL -> totalMultipliers.merge(modifier.stat(), 1 + modifier.amount(), (a, b) -> a * b);
                case ADD -> flatSums.merge(modifier.stat(), modifier.amount(), Double::sum);
            }
        }
    }

    private double multiplier(GeneratorStat stat) {
        return Math.max(MIN_MULTIPLIER, (1 + baseMultiplierSums.getOrDefault(stat, 0.0)) * totalMultipliers.getOrDefault(stat, 1.0));
    }

    private double flat(GeneratorStat stat) {
        return flatSums.getOrDefault(stat, 0.0);
    }

    private double clampChance(double chance) {
        return Math.max(0, Math.min(GUARANTEED_CHANCE, chance));
    }

    private int applyModifiers(int base, GeneratorStat stat) {
        return Math.max(base > 0 ? 1 : 0, (int) Math.round(base * multiplier(stat) + flat(stat)));
    }
}
