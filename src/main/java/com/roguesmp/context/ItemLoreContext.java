package com.roguesmp.context;

import com.roguesmp.item.SmpItem;
import com.roguesmp.item.lore.LoreBuilder;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ItemLoreContext(SmpItem smpItem, @Nullable SmpPlayer player, LoreBuilder builder,
                              @NotNull PersistentDataContainerView data) {
}
