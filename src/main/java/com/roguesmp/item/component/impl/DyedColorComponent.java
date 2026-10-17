package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.context.ItemDataContext;
import com.roguesmp.item.component.ItemComponent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DyedItemColor;
import org.bukkit.Color;
import org.jetbrains.annotations.NotNull;

public record DyedColorComponent(Color color) implements ItemComponent {

    public static final Codec<DyedColorComponent> CODEC = Codec.HEX_COLOR.xmap(DyedColorComponent::new, DyedColorComponent::color);

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void modifyStack(ItemDataContext context) {
        context.newStack().setData(DataComponentTypes.DYED_COLOR, DyedItemColor.dyedItemColor(color));
    }
}
