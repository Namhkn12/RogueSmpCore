package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.context.ItemDataContext;
import com.roguesmp.item.component.ItemComponent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.jetbrains.annotations.NotNull;

public record EnchantGlintComponent(boolean glint) implements ItemComponent {

    public static final Codec<EnchantGlintComponent> CODEC = Codec.BOOLEAN.xmap(EnchantGlintComponent::new, EnchantGlintComponent::glint);

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void modifyStack(ItemDataContext context) {
        context.newStack().setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, glint);
    }
}
