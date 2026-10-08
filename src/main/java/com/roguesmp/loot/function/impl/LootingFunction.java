package com.roguesmp.loot.function.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.loot.context.LootContext;
import com.roguesmp.loot.function.LootFunction;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LootingFunction implements LootFunction {

    public static final String TYPE_KEY = "looting";

    public static final Codec<LootingFunction> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("max", 1).forGetter(LootingFunction::getMax),
            Codec.INT.optionalFieldOf("limit", Integer.MAX_VALUE).forGetter(LootingFunction::getLimit),
            LootingFunction::new
    );

    private final int max;
    private final int limit;

    public LootingFunction(int max, int limit) {
        this.max = Math.max(0, max);
        this.limit = limit;
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public int getMax() {
        return max;
    }

    public int getLimit() {
        return limit;
    }

    @Override
    public List<ItemStack> apply(List<ItemStack> items, LootContext context) {
        double looting = context.getLooting();
        if (looting <= 0 || max == 0) return items;
        return items.stream().map(stack -> withBonus(stack, looting)).toList();
    }

    private ItemStack withBonus(ItemStack stack, double looting) {
        int bonus = (int) Math.round(ThreadLocalRandom.current().nextInt(max + 1) * looting);
        ItemStack copy = stack.clone();
        copy.setAmount(Math.min(limit, copy.getAmount() + bonus));
        return copy;
    }
}
