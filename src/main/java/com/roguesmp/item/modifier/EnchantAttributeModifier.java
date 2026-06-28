package com.roguesmp.item.modifier;

import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.constant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.EnchantComponent;
import com.roguesmp.item.component.impl.EquipAttributeComponent;
import com.roguesmp.player.SmpPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class EnchantAttributeModifier implements ItemModifier {
    @Override
    public void collectAndApply(SmpItem smpItem, @Nullable SmpPlayer player) {
        EquipAttributeComponent equipAttributeComponent = smpItem.getComponent(ComponentKeys.ATTRIBUTE);
        if (equipAttributeComponent == null) return;

        EnchantComponent enchantComponent = smpItem.getComponent(ComponentKeys.ENCHANT);
        if (enchantComponent == null) return;
        EquipSlot slot = equipAttributeComponent.getSlot();
        Map<Enchants, Integer> enchantMap = enchantComponent.getTotalEnchants();
        enchantMap.forEach((enchants, integer) -> {
            if (enchants.getEnchant().getActiveSlots().contains(slot)) {
                equipAttributeComponent.putModifier(enchants.getEnchant().getId(), enchants.getEnchant().provideAttributes(integer));
            }
        });
    }
}
