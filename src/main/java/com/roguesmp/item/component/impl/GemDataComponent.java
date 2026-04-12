package com.roguesmp.item.component.impl;

import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

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
    public void contributeLore(ItemLoreContext context) {

        List<Component> lores = new ArrayList<>();

        attributes.forEach((slot, statMap) -> {
            lores.add(Utils.text("Khi khảm trang bị " + slot.getSimpleName() + ":", NamedTextColor.GRAY));

            statMap.forEach((attribute, value) -> {
                String sign = value >= 0 ? "+" : "";
                TextColor textColor = value >= 0 ? NamedTextColor.GREEN : NamedTextColor.RED;
                lores.add(Utils.text(" " + sign + Utils.formatDecimal(value) + " " + attribute.getAttribute().getSimpleName(), textColor));
            });

            lores.add(Component.empty());
        });

        lores.removeLast();

        context.builder().putLines(105, lores);
    }

    @Override
    public @NotNull ItemComponent copy() {
        return new GemDataComponent(attributes);
    }

    public @Unmodifiable Map<EquipSlot, Map<Attributes, Double>> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
}
