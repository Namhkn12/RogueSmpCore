package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.context.ItemDataContext;
import com.roguesmp.item.component.ItemComponent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public class ItemModelComponent implements ItemComponent {

    public static final Codec<ItemModelComponent> CODEC = Codec.KEY
            .xmap(ItemModelComponent::new, ItemModelComponent::getModelKey);

    private final Key modelKey;

    public ItemModelComponent(Key modelKey) {
        this.modelKey = modelKey;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void modifyStack(ItemDataContext context) {
        context.newStack().setData(DataComponentTypes.ITEM_MODEL, modelKey);
    }

    public Key getModelKey() {
        return modelKey;
    }
}
