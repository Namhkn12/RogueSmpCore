package com.roguesmp.item.modifier;

import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.BrokenComponent;
import com.roguesmp.item.component.impl.DurabilityComponent;
import com.roguesmp.item.component.impl.NameComponent;
import com.roguesmp.player.SmpPlayer;
import org.jetbrains.annotations.Nullable;

public class BrokenModifier implements ItemModifier {
    @Override
    public void collectAndApply(SmpItem smpItem, @Nullable SmpPlayer player) {
        DurabilityComponent durabilityComponent = smpItem.getComponent(ComponentKeys.DURABILITY);
        if (durabilityComponent != null) {
            int current = durabilityComponent.currentDurability();
            if (current <= 0) {
                smpItem.setComponent(ComponentKeys.BROKEN, new BrokenComponent());
                NameComponent nameComponent = smpItem.getComponent(ComponentKeys.ITEM_NAME);
                if (nameComponent != null) {
                    nameComponent.setPrefix("<red><b>HỎNG ");
                }
            } else {
                smpItem.unsetComponent(ComponentKeys.BROKEN);
                NameComponent nameComponent = smpItem.getComponent(ComponentKeys.ITEM_NAME);
                if (nameComponent != null) {
                    nameComponent.setPrefix("");
                }
            }
        }
    }
}
