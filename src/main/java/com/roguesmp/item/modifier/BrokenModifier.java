package com.roguesmp.item.modifier;

import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.BrokenComponent;
import com.roguesmp.item.component.impl.DurabilityComponent;
import com.roguesmp.item.component.impl.NameComponent;
import com.roguesmp.player.SmpPlayer;
import org.jetbrains.annotations.Nullable;

public class BrokenModifier implements ItemModifier {
    @Override
    public void collectAndApply(SmpItem smpItem, @Nullable SmpPlayer player, boolean isPreview) {
        DurabilityComponent durabilityComponent = smpItem.getComponent(ItemComponentKeys.DURABILITY);
        if (durabilityComponent != null) {
            int current = durabilityComponent.currentDurability();
            if (current <= 0) {
                smpItem.setComponent(ItemComponentKeys.BROKEN, new BrokenComponent());
                NameComponent nameComponent = smpItem.getComponent(ItemComponentKeys.ITEM_NAME);
                if (nameComponent != null) {
                    nameComponent.setPrefix("<red><b>HỎNG ");
                }
            } else {
                smpItem.unsetComponent(ItemComponentKeys.BROKEN);
                NameComponent nameComponent = smpItem.getComponent(ItemComponentKeys.ITEM_NAME);
                if (nameComponent != null) {
                    nameComponent.setPrefix("");
                }
            }
        }
    }
}
