package com.roguesmp.item.component.impl;

import com.roguesmp.context.ItemDataContext;
import com.roguesmp.item.component.ItemComponent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public class ItemModelComponent implements ItemComponent {

    private final String modelKey;

    public ItemModelComponent(String modelKey) {
        this.modelKey = modelKey;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void modifyStack(ItemDataContext context) {
        context.newStack().setData(DataComponentTypes.ITEM_MODEL, Key.key(modelKey));
    }

    public String getModelKey() {
        return modelKey;
    }
}
