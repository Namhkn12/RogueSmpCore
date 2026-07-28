package com.roguesmp.entity.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.constant.EntityAttribute;
import com.roguesmp.entity.component.EntityComponent;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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
        values.forEach((entityAttribute, value) -> {
            Attribute bukkitAttribute = entityAttribute.getBukkitAttribute();
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
