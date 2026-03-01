package com.roguesmp.item.component.impl;

import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.item.component.ItemComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class GemDataComponent implements ItemComponent {

    private final Map<EquipSlot, Map<Attributes, Double>> attributes = new EnumMap<>(EquipSlot.class);

    public GemDataComponent(Map<EquipSlot, Map<Attributes, Double>> attributes) {
        attributes.forEach((equipSlot, attributesDoubleMap) -> {
            Map<Attributes, Double> copy = new EnumMap<>(Attributes.class);
            copy.putAll(attributesDoubleMap);
            this.attributes.put(equipSlot, copy);
        });
    }

    @Override
    public @NotNull ItemComponent copy() {
        return new GemDataComponent(attributes);
    }

    public @Unmodifiable Map<EquipSlot, Map<Attributes, Double>> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
}
