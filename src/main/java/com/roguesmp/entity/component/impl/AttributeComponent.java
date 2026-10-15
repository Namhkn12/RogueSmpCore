package com.roguesmp.entity.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.EntityAttribute;
import com.roguesmp.entity.component.EntityComponent;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractCubeMob;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record AttributeComponent(Map<EntityAttribute, Double> values) implements EntityComponent {

    public static final Codec<AttributeComponent> CODEC = Codec.lenientUnboundedMap(Codec.enumOf(EntityAttribute.class), Codec.DOUBLE)
            .xmap(AttributeComponent::new, AttributeComponent::values);

    @Override
    public @NotNull EntityComponent copy() {
        return this;
    }

    @Override
    public void apply(LivingEntity entity) {
        if (entity instanceof AbstractCubeMob abstractCubeMob) {
            abstractCubeMob.setSize(2);
        }
        values.forEach((entityAttribute, value) -> {
            if (entityAttribute == EntityAttribute.CUBE_SIZE) {
                if (entity instanceof AbstractCubeMob abstractCubeMob) {
                    abstractCubeMob.setSize(value.intValue());
                }
            }
            Attribute bukkitAttribute = entityAttribute.getBukkitAttribute();
            if (bukkitAttribute == null) return;
            AttributeInstance instance = entity.getAttribute(bukkitAttribute);
            if (instance == null) {
                entity.registerAttribute(bukkitAttribute);
                instance = entity.getAttribute(bukkitAttribute);
            }
            // Cannot be null since we just registered it
            instance.setBaseValue(value);
            if (bukkitAttribute == Attribute.MAX_HEALTH) entity.setHealth(value);
        });
    }
}
