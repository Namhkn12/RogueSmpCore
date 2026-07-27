package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.context.ItemDataContext;
import com.roguesmp.item.component.ItemComponent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.jetbrains.annotations.NotNull;

public record StackSizeComponent(int size) implements ItemComponent {

    public static final Codec<StackSizeComponent> CODEC = Codec.INT.xmap(StackSizeComponent::new, StackSizeComponent::size);

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void modifyStack(ItemDataContext context) {
        context.newStack().setData(DataComponentTypes.MAX_STACK_SIZE, size);
    }
}
