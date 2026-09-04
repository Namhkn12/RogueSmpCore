package com.roguesmp.item.modifier;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.item.component.ItemComponentKeys;
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

        GemSocketComponent gemSocketComponent = smpItem.getComponent(ItemComponentKeys.GEM_SOCKET);
        if (gemSocketComponent == null) return;

        EquipAttributeComponent equipAttributeComponent = smpItem.getComponent(ItemComponentKeys.ATTRIBUTE);
        if (equipAttributeComponent == null) return;
        Map<Attributes, Double> modifiers = new EnumMap<>(Attributes.class);
        gemSocketComponent.getActiveGem().forEach(baseItem -> {
            GemDataComponent gemDataComponent = baseItem.getComponent(ItemComponentKeys.GEM_DATA);
            if (gemDataComponent == null) return;
            Map<Attributes, Double> attributeMap = gemDataComponent.getAttributes().get(equipAttributeComponent.getSlot());
            if (attributeMap == null) return;
            attributeMap.forEach((attributes, aDouble) -> {
                modifiers.merge(attributes, aDouble, Double::sum);
            });
        });

        // Unconditional put-or-remove: if the last gem was taken out, modifiers ends up empty and
        // must still clear the previous entry, not just skip re-applying it.
        if (modifiers.isEmpty()) {
            equipAttributeComponent.removeModifier("gem_attribute_modifier");
        } else {
            equipAttributeComponent.putModifier("gem_attribute_modifier", modifiers);
        }
    }
}
