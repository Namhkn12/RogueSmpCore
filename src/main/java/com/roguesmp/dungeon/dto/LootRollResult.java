package com.roguesmp.dungeon.dto;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class LootRollResult {

    private static final LootRollResult EMPTY = new LootRollResult(Collections.emptyList());

    private final @NotNull List<ItemStack> items;

    LootRollResult(@NotNull List<ItemStack> items) {
        this.items = items;
    }

    public static LootRollResult empty() {
        return EMPTY;
    }

    public static LootRollResult of(@NotNull ItemStack item) {
        return new LootRollResult(List.of(item));
    }

    public static LootRollResult of(@NotNull List<ItemStack> items) {
        return new LootRollResult(items);
    }

    @NotNull
    public List<ItemStack> getItems() {
        return items;
    }
}
