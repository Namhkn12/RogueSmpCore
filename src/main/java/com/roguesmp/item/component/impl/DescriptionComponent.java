package com.roguesmp.item.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record DescriptionComponent(List<String> description) implements ItemComponent {

    public static final Codec<DescriptionComponent> CODEC = Codec.listOf(Codec.STRING).xmap(DescriptionComponent::new, DescriptionComponent::description);

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        List<Component> result = new ArrayList<>();
        for (String s : description) {
            result.add(Utils.fromString(s).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        context.builder().putLines(90, result);
    }
}
