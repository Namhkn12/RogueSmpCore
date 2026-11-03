package com.roguesmp.entity.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.effect.SmpEffect;
import com.roguesmp.entity.component.EntityComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public record EffectImmunityComponent(Set<String> effectIds) implements EntityComponent {

    public static final Codec<EffectImmunityComponent> CODEC = Codec.listOf(Codec.STRING)
            .xmap(ids -> new EffectImmunityComponent(Set.copyOf(ids)), component -> List.copyOf(component.effectIds()));

    public boolean isImmuneTo(SmpEffect effect) {
        return effectIds.contains(effect.getEffectID());
    }

    @Override
    public @NotNull EntityComponent copy() {
        return this;
    }
}
