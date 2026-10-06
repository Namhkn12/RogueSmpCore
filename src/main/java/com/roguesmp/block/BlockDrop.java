package com.roguesmp.block;

import com.roguesmp.codec.Codec;
import com.roguesmp.item.BaseItem;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Registries;
import com.roguesmp.utils.Utils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

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
        return rollStored(chance, 1.0, 0.0);
    }

    public @Nullable StoredItem rollStored(double chance, double amountMultiplier, double flatAmount) {
        int amount = rollAmount(chance, amountMultiplier, flatAmount);
        return amount <= 0 ? null : new StoredItem(item, amount);
    }

    private int rollAmount() {
        return rollAmount(chance, 1.0, 0.0);
    }

    private int rollAmount(double chance, double amountMultiplier, double flatAmount) {
        if (Utils.RANDOM.nextDouble() >= chance) return 0;

        int base = minAmount + (maxAmount > minAmount ? Utils.RANDOM.nextInt(maxAmount - minAmount + 1) : 0);
        double scaled = Math.max(0, base * amountMultiplier + flatAmount);
        int whole = (int) scaled;
        double fraction = scaled - whole;
        return fraction > 0 && Utils.RANDOM.nextDouble() < fraction ? whole + 1 : whole;
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
