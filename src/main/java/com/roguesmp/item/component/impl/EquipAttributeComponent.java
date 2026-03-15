package com.roguesmp.item.component.impl;

import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class EquipAttributeComponent implements ItemComponent {

    private final Map<Attributes, Double> attributes = new EnumMap<>(Attributes.class);
    private final EquipSlot slot;
    @GsonIgnore
    private final Map<Attributes, Double> modifiers = new EnumMap<>(Attributes.class);

    public EquipAttributeComponent(Map<Attributes, Double> attributes, EquipSlot slot) {
        this.slot = slot;
        this.attributes.putAll(attributes);
    }

    public @Unmodifiable Map<Attributes, Double> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public EquipSlot getSlot() {
        return slot;
    }

    public double getValue(Attributes attribute) {
        return attributes.getOrDefault(attribute, 0d);
    }

    public double getBonusValue(Attributes attribute) {
        return modifiers.getOrDefault(attribute, 0d);
    }

    public void addModifier(Map<Attributes, Double> modifiers) {

        for (var entry : modifiers.entrySet()) {
            this.modifiers.merge(entry.getKey(), entry.getValue(), Double::sum);
            attributes.merge(entry.getKey(), entry.getValue(), Double::sum);
        }
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        List<Component> res = new ArrayList<>();
        res.add(Component.empty());
        res.add(Utils.fromString(slot.getDisplayString()));
        attributes.forEach((attributes1, aDouble) -> {
            if (!Utils.isEffectiveZero(aDouble)) {
                List<Component> lines = attributes1.getAttribute().getDisplayText(aDouble, context.player(), context.data());
                if (lines != null) res.addAll(lines);
            }
        });

        context.builder().putLines(100, res);
    }

    @Override
    public @NotNull ItemComponent copy() {
        return new EquipAttributeComponent(attributes, slot);
    }
}
