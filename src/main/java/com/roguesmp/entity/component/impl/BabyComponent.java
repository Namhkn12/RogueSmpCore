package com.roguesmp.entity.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.component.EntityComponent;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public record BabyComponent(boolean baby) implements EntityComponent {

    public static final Codec<BabyComponent> CODEC = Codec.BOOLEAN.xmap(BabyComponent::new, BabyComponent::baby);

    @Override
    public @NotNull EntityComponent copy() {
        return this;
    }

    @Override
    public void apply(LivingEntity entity) {
        if (entity instanceof Ageable ageable) {
            if (baby) {
                ageable.setAgeLock(true);
                ageable.setBaby();
            } else {
                ageable.setAgeLock(true);
                ageable.setAdult();
            }
        }
    }
}
