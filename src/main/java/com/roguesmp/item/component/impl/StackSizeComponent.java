package com.roguesmp.item.component.impl;

import com.roguesmp.context.ItemDataContext;
import com.roguesmp.item.component.ItemComponent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.jetbrains.annotations.NotNull;

public record StackSizeComponent(int size) implements ItemComponent {
    @Override
    public @NotNull ItemComponent copy() {
        return new StackSizeComponent(size);
    }

    @Override
    public void modifyStack(ItemDataContext context) {
        context.newStack().setData(DataComponentTypes.MAX_STACK_SIZE, size);
    }
}
