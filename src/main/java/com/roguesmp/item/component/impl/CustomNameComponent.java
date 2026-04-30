package com.roguesmp.item.component.impl;

import com.roguesmp.context.ItemDataContext;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.jetbrains.annotations.NotNull;

public record CustomNameComponent(String value) implements ItemComponent {

    @Override
    public @NotNull ItemComponent copy() {
        return new CustomNameComponent(value);
    }

    @Override
    public void modifyStack(ItemDataContext context) {
        context.newStack().setData(DataComponentTypes.CUSTOM_NAME, Utils.fromString(value));
    }
}
