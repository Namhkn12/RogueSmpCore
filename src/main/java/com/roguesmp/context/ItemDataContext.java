package com.roguesmp.context;

import com.roguesmp.item.SmpItem;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public record ItemDataContext(SmpItem smpItem, SmpPlayer player, ItemStack newStack, @NotNull ItemStack oldStack,
                              @NotNull PersistentDataContainerView data) {
}
