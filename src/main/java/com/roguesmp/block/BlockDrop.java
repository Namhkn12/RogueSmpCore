package com.roguesmp.block;

import com.roguesmp.codec.Codec;
import com.roguesmp.item.BaseItem;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * One independent entry in a block's drop list.
 * {@code item} is either {@code "minecraft:<material>"} for a vanilla item or one of
 * custom item ids.
 */
public record BlockDrop(String item, int minAmount, int maxAmount, double chance) {

    private static final String VANILLA_PREFIX = "minecraft:";

    public static final Codec<BlockDrop> CODEC = Codec.composite(
            Codec.STRING.fieldOf("item").forGetter(BlockDrop::item),
            Codec.INT.optionalFieldOf("min_amount", 1).forGetter(BlockDrop::minAmount),
            Codec.INT.optionalFieldOf("max_amount", 1).forGetter(BlockDrop::maxAmount),
            Codec.DOUBLE.optionalFieldOf("chance", 1.0).forGetter(BlockDrop::chance),
            BlockDrop::new
    );

    public @Nullable ItemStack roll() {
        if (Utils.RANDOM.nextDouble() >= chance) return null;

        int amount = minAmount + (maxAmount > minAmount ? Utils.RANDOM.nextInt(maxAmount - minAmount + 1) : 0);
        if (amount <= 0) return null;

        ItemStack stack = resolve();
        if (stack == null) return null;

        stack.setAmount(amount);
        return stack;
    }

    private @Nullable ItemStack resolve() {
        if (item.startsWith(VANILLA_PREFIX)) {
            Material material = Material.matchMaterial(item.substring(VANILLA_PREFIX.length()));
            return material == null ? null : new ItemStack(material);
        }

        BaseItem baseItem = Registries.ITEM.get(item);
        return baseItem == null ? null : baseItem.generateItemStack(1);
    }
}
