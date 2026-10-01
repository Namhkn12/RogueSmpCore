package com.roguesmp.block;

import com.roguesmp.codec.Codec;
import com.roguesmp.item.BaseItem;
import com.roguesmp.player.SmpPlayer;
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

    public static final Codec<BlockDrop> CODEC = Codec.composite(
            Codec.STRING.fieldOf("item").forGetter(BlockDrop::item),
            Codec.INT.optionalFieldOf("min_amount", 1).forGetter(BlockDrop::minAmount),
            Codec.INT.optionalFieldOf("max_amount", 1).forGetter(BlockDrop::maxAmount),
            Codec.DOUBLE.optionalFieldOf("chance", 1.0).forGetter(BlockDrop::chance),
            BlockDrop::new
    );

    public @Nullable ItemStack roll(@Nullable SmpPlayer player) {
        int amount = rollAmount();
        if (amount <= 0) return null;

        ItemStack stack = resolve();
        if (stack == null) return null;

        stack.setAmount(amount);
        return stack;
    }

    /** Same roll as {@link #roll}, without resolving a live {@link ItemStack} - for id-based storage (see {@link StoredItem}). */
    public @Nullable StoredItem rollStored() {
        int amount = rollAmount();
        return amount <= 0 ? null : new StoredItem(item, amount);
    }

    private int rollAmount() {
        if (Utils.RANDOM.nextDouble() >= chance) return 0;
        return minAmount + (maxAmount > minAmount ? Utils.RANDOM.nextInt(maxAmount - minAmount + 1) : 0);
    }

    private @Nullable ItemStack resolve() {
        if (BlockRef.isVanilla(item)) {
            Material material = BlockRef.vanillaMaterial(item);
            return material == null ? null : ItemStack.of(material);
        }

        BaseItem baseItem = Registries.ITEM.get(item);
        return baseItem == null ? null : baseItem.generateItemStack(1);
    }
}
