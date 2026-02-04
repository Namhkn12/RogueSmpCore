package com.roguesmp.enchant;

import com.roguesmp.player.SmpPlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface StackingEnchant {
    void addData(ItemStack newStack, @Nullable ItemStack oldStack, SmpPlayer player, @NotNull PersistentDataContainer modifierData);
}
