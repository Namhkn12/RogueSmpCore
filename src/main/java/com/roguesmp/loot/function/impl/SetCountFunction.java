package com.roguesmp.loot.function.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.loot.context.LootContext;
import com.roguesmp.loot.function.LootFunction;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SetCountFunction implements LootFunction {

    public static final String TYPE_KEY = "set_count";

    public static final Codec<SetCountFunction> CODEC = Codec.composite(
            Codec.INT.optionalFieldOf("min", 1).forGetter(SetCountFunction::getMin),
            Codec.INT.optionalFieldOf("max", 1).forGetter(SetCountFunction::getMax),
            SetCountFunction::new
    );

    private final int min;
    private final int max;

    public SetCountFunction(int min, int max) {
        this.min = Math.max(1, min);
        this.max = Math.max(this.min, max);
    }

    @Override
    public String getTypeId() {
        return TYPE_KEY;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    @Override
    public List<ItemStack> apply(List<ItemStack> items, LootContext context) {
        return items.stream().map(this::withNewCount).toList();
    }

    private ItemStack withNewCount(ItemStack stack) {
        ItemStack copy = stack.clone();
        copy.setAmount(ThreadLocalRandom.current().nextInt(min, max + 1));
        return copy;
    }
}
