package com.roguesmp.context;

import com.roguesmp.item.SmpItem;
import com.roguesmp.player.SmpPlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

public record ItemDataContext(SmpItem smpItem, SmpPlayer player, ItemStack newStack, @NotNull ItemStack oldStack,
                              @NotNull PersistentDataContainer data) {
}
