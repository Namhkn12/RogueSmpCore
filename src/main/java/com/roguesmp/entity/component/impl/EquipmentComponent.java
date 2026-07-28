package com.roguesmp.entity.component.impl;

import com.roguesmp.codec.Codec;
import com.roguesmp.entity.EntityEquipment;
import com.roguesmp.entity.component.EntityComponent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record EquipmentComponent(Map<EquipmentSlot, EntityEquipment> slots) implements EntityComponent {

    public static final Codec<EquipmentComponent> CODEC = Codec.lenientUnboundedMap(Codec.enumOf(EquipmentSlot.class), EntityEquipment.CODEC)
            .xmap(EquipmentComponent::new, EquipmentComponent::slots);

    @Override
    public @NotNull EntityComponent copy() {
        return this;
    }

    @Override
    public void apply(LivingEntity entity) {
        org.bukkit.inventory.EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) return;

        slots.forEach((slot, entityEquipment) -> {
            equipment.setDropChance(slot, 0f);
            equipment.setItem(slot, entityEquipment.createItemStack());
        });
    }
}
