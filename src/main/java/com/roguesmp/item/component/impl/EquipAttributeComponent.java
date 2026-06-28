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
    private final Map<String, Map<Attributes, Double>> modifiers = new HashMap<>();

    @GsonIgnore
    private final Map<Attributes, Double> finalAttributes = new EnumMap<>(Attributes.class);

    @GsonIgnore
    private boolean isDirty = true;

    public EquipAttributeComponent(Map<Attributes, Double> attributes, EquipSlot slot) {
        this.slot = slot;
        this.attributes.putAll(attributes);
    }

    public @Unmodifiable Map<Attributes, Double> getFinalAttributes() {
        update();
        return Collections.unmodifiableMap(finalAttributes);
    }

    public EquipSlot getSlot() {
        return slot;
    }

    public double getValue(Attributes attribute) {
        update();
        return finalAttributes.getOrDefault(attribute, 0d);
    }

    public void putModifier(String sourceKey, Map<Attributes, Double> attributeModifiers) {
        this.modifiers.put(sourceKey, attributeModifiers);
        this.isDirty = true;
    }

    public void removeModifier(String sourceKey) {
        if (this.modifiers.remove(sourceKey) != null) {
            this.isDirty = true;
        }
    }

    private void update() {
        if (!isDirty) return;

        this.finalAttributes.clear();

        this.finalAttributes.putAll(this.attributes);

        // Merge all active keyed modifiers (Enchants, Gems,...)
        for (Map<Attributes, Double> modifierMap : modifiers.values()) {
            modifierMap.forEach((attribute, value) ->
                    this.finalAttributes.merge(attribute, value, (aDouble, aDouble2) -> {
                        double total = aDouble + aDouble2;
                        if (Utils.isEffectiveZero(total)) return null;
                        return total;
                    })
            );
        }

        this.isDirty = false;
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        update();
        List<Component> res = new ArrayList<>();
        res.add(Component.empty());
        res.add(Utils.fromString(slot.getDisplayString()));
        finalAttributes.forEach((attributes1, aDouble) -> {
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
