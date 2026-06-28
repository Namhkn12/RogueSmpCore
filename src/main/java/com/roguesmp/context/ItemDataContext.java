package com.roguesmp.context;

import com.roguesmp.item.SmpItem;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ItemDataContext(SmpItem smpItem, @Nullable SmpPlayer player, ItemStack newStack,
                              @NotNull PersistentDataContainerView data) {
}
