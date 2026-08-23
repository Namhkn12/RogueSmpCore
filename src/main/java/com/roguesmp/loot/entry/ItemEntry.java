package com.roguesmp.loot.entry;

import com.roguesmp.codec.Codec;
import com.roguesmp.loot.LootEntry;
import org.jetbrains.annotations.NotNull;

/**
 * Drops a random amount of a single {@code com.roguesmp.item.BaseItem} in {@code [min, max]}.
 */
public final class ItemEntry extends LootEntry {

    public static final String TYPE_KEY = "item";

    public static final Codec<ItemEntry> CODEC = Codec.composite(
            LootEntry.BASE_CODEC.forGetter(LootEntry::getBaseProperties),
            Codec.STRING.fieldOf("item_id").forGetter(ItemEntry::getItemId),
            Codec.INT.optionalFieldOf("min_amount", 1).forGetter(ItemEntry::getMinAmount),
            Codec.INT.optionalFieldOf("max_amount", 1).forGetter(ItemEntry::getMaxAmount),
            ItemEntry::new
    );

    private final String itemId;
    private final int minAmount;
    private final int maxAmount;

    public ItemEntry(@NotNull BaseProperties base, @NotNull String itemId, int minAmount, int maxAmount) {
        super(base);
        this.itemId = itemId;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    @Override
    public @NotNull String getTypeId() {
        return TYPE_KEY;
    }

    public @NotNull String getItemId() {
        return itemId;
    }

    public int getMinAmount() {
        return minAmount;
    }

    public int getMaxAmount() {
        return maxAmount;
    }
}
