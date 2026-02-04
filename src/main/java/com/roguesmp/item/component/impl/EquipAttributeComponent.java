package com.roguesmp.item.component.impl;

import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.lore.LoreBuilder;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.utils.Utils;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class EquipAttributeComponent implements ItemComponent {

    private final Map<Attributes, Double> attributes = new EnumMap<>(Attributes.class);
    private final EquipSlot slot;

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

    @Override
    public void contributeLore(ItemLoreContext context) {
        List<Component> res = new ArrayList<>();
        res.add(Component.empty());
        res.add(Utils.fromString(slot.getDisplayString()));
        attributes.forEach((attributes1, aDouble) -> {
            if (!Utils.isEffectiveZero(aDouble)) res.addAll(attributes1.getAttribute().getDisplayText(aDouble, context.player(), context.data()));
        });

        context.builder().putLines(100, res);
    }

    public double getValue(Attributes attribute) {
        return attributes.get(attribute);
    }

    @Override
    public @NotNull ItemComponent copy() {
        return new EquipAttributeComponent(attributes, slot);
    }
}
