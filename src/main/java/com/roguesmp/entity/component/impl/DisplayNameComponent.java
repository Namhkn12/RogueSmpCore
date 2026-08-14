package com.roguesmp.entity.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.component.EntityComponent;
import org.jetbrains.annotations.NotNull;

public record DisplayNameComponent(String name) implements EntityComponent {

    public static final Codec<DisplayNameComponent> CODEC = Codec.STRING
            .xmap(DisplayNameComponent::new, DisplayNameComponent::name);

    @Override
    public @NotNull EntityComponent copy() {
        return this;
    }
}
