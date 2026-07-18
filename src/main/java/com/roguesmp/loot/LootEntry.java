package com.roguesmp.loot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a single entry inside a {@link LootPool}.
 *
 * <p>Depending on {@code type}:
 * <ul>
 *   <li>ITEM       — {@code itemId}, {@code minAmount}, {@code maxAmount} are used</li>
 *   <li>LOOT_TABLE — {@code nestedTableId} is used (resolved lazily at roll time)</li>
 *   <li>EMPTY      — no extra fields are used, just the weight</li>
 * </ul>
 */
public class LootEntry {

    private final @NotNull LootEntryType type;
    private final int weight;

    // Used when type == ITEM
    private final @Nullable String itemId;
    private final int minAmount;
    private final int maxAmount;

    // Used when type == LOOT_TABLE
    private final @Nullable String nestedTableId;

    private LootEntry(Builder builder) {
        this.type = builder.type;
        this.weight = builder.weight;
        this.itemId = builder.itemId;
        this.minAmount = builder.minAmount;
        this.maxAmount = builder.maxAmount;
        this.nestedTableId = builder.nestedTableId;
    }

    // --- Getters ---

    public @NotNull LootEntryType getType() {
        return type;
    }

    public int getWeight() {
        return weight;
    }

    public @Nullable String getItemId() {
        return itemId;
    }

    public int getMinAmount() {
        return minAmount;
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    public @Nullable String getNestedTableId() {
        return nestedTableId;
    }

    // --- Builder ---

    public static Builder builder(@NotNull LootEntryType type, int weight) {
        return new Builder(type, weight);
    }

    public static class Builder {
        private final LootEntryType type;
        private final int weight;
        private String itemId;
        private int minAmount = 1;
        private int maxAmount = 1;
        private String nestedTableId;

        private Builder(LootEntryType type, int weight) {
            this.type = type;
            this.weight = weight;
        }

        public Builder itemId(String itemId) {
            this.itemId = itemId;
            return this;
        }

        public Builder amount(int min, int max) {
            this.minAmount = min;
            this.maxAmount = max;
            return this;
        }

        public Builder nestedTableId(String nestedTableId) {
            this.nestedTableId = nestedTableId;
            return this;
        }

        public LootEntry build() {
            return new LootEntry(this);
        }
    }
}
