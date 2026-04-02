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
import java.util.Set;

public class EnchantAttributeModifier implements ItemModifier {
    @Override
    public void collectAndApply(SmpItem smpItem, @Nullable SmpPlayer player) {
        EquipAttributeComponent equipAttributeComponent = smpItem.getComponent(ComponentKeys.ATTRIBUTE);
        if (equipAttributeComponent == null) return;

        EnchantComponent enchantComponent = smpItem.getComponent(ComponentKeys.ENCHANT);
        if (enchantComponent == null) return;
        EquipSlot slot = equipAttributeComponent.getSlot();
        Map<Enchants, Integer> enchantMap = enchantComponent.getEnchants();
        enchantMap.forEach((enchants, integer) -> {
            if (enchants.getEnchant().getActiveSlots().contains(slot)) {
                equipAttributeComponent.addModifier(enchants.getEnchant().provideAttributes(integer));
            }
        });
    }
}
