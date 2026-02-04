package com.roguesmp.item.component.impl;

import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.lore.LoreBuilder;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record DescriptionComponent(List<String> description) implements ItemComponent {

    @Override
    public @NotNull ItemComponent copy() {
        return new DescriptionComponent(description);
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        context.builder().putLines(90, Utils.fromStrings(description));
    }
}
