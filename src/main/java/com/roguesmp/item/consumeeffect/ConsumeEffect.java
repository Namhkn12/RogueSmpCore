package com.roguesmp.item.consumeeffect;

import com.roguesmp.item.SmpItem;
import com.roguesmp.item.lore.LoreBuilder;
import com.roguesmp.player.SmpPlayer;
import org.bukkit.inventory.ItemStack;

public interface ConsumeEffect {
    /**
     * @param player The player using the item
     * @param item   The SmpItem being consumed
     * @param stack  The physical ItemStack (to handle amounts/durability)
     */
    void apply(SmpPlayer player, SmpItem item, ItemStack stack);

    // Optional: Add lore description for the effect
    default void addLore(LoreBuilder builder) {}
}
