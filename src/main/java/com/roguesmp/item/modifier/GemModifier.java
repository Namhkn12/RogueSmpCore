package com.roguesmp.item.modifier;

import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.EquipAttributeComponent;
import com.roguesmp.item.component.impl.GemDataComponent;
import com.roguesmp.item.component.impl.GemSocketComponent;
import com.roguesmp.player.SmpPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class GemModifier implements ItemModifier {
    @Override
    public void collectAndApply(SmpItem smpItem, @Nullable SmpPlayer player) {

        GemSocketComponent gemSocketComponent = smpItem.getComponent(ComponentKeys.GEM_SOCKET);
        if (gemSocketComponent == null) return;

        EquipAttributeComponent equipAttributeComponent = smpItem.getComponent(ComponentKeys.ATTRIBUTE);
        if (equipAttributeComponent == null) return;
        Map<Attributes, Double> modifiers = new EnumMap<>(Attributes.class);
        gemSocketComponent.getActiveGem().forEach(baseItem -> {
            GemDataComponent gemDataComponent = baseItem.getComponent(ComponentKeys.GEM_DATA);
            if (gemDataComponent == null) return;
            Map<Attributes, Double> attributeMap = gemDataComponent.getAttributes().get(equipAttributeComponent.getSlot());
            if (attributeMap == null) return;
            attributeMap.forEach((attributes, aDouble) -> {
                modifiers.merge(attributes, aDouble, Double::sum);
            });

            equipAttributeComponent.putModifier("gem_attribute_modifier", modifiers);
        });

    }
}
