package com.roguesmp.loot;

import org.bukkit.inventory.ItemStack;
import java.util.Collections;
import java.util.List;

public class LootRollResult {

    private static final LootRollResult EMPTY = new LootRollResult(Collections.emptyList());

    private final List<ItemStack> items;

    LootRollResult(List<ItemStack> items) {
        this.items = items;
    }

    public static LootRollResult empty() {
        return EMPTY;
    }

    public static LootRollResult of(ItemStack item) {
        return new LootRollResult(List.of(item));
    }

    public static LootRollResult of(List<ItemStack> items) {
        return new LootRollResult(items);
    }

    public List<ItemStack> getItems() {
        return items;
    }
}
