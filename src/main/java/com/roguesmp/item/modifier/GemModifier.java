package com.roguesmp.item.modifier;

import com.roguesmp.constant.Attributes;
import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.EquipAttributeComponent;
import com.roguesmp.item.component.impl.GemSocketComponent;
import com.roguesmp.player.SmpPlayer;

import java.util.Map;

public class GemModifier implements ItemModifier {
    @Override
    public void collectAndApply(SmpItem smpItem, SmpPlayer player) {

        GemSocketComponent gemSocketComponent = smpItem.getComponent(ComponentKeys.GEM_SOCKET);
        if (gemSocketComponent == null) return;

        EquipAttributeComponent equipAttributeComponent = smpItem.getComponent(ComponentKeys.ATTRIBUTE);
        if (equipAttributeComponent == null) return;

        gemSocketComponent.getActiveGem().forEach(gemData -> {
            Map<Attributes, Double> attributeMap = gemData.getAttributes().get(equipAttributeComponent.getSlot());
            if (attributeMap == null) return;
            equipAttributeComponent.addModifier(attributeMap);
        });

    }
}
