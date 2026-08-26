package com.roguesmp.item.modifier;

import com.roguesmp.attribute.Attributes;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.enchant.Enchants;
import com.roguesmp.constant.EquipSlot;
import com.roguesmp.item.SmpItem;
import com.roguesmp.item.component.impl.EnchantComponent;
import com.roguesmp.item.component.impl.EquipAttributeComponent;
import com.roguesmp.player.SmpPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class EnchantAttributeModifier implements ItemModifier {
    @Override
    public void collectAndApply(SmpItem smpItem, @Nullable SmpPlayer player, boolean isPreview) {
        EquipAttributeComponent equipAttributeComponent = smpItem.getComponent(ItemComponentKeys.ATTRIBUTE);
        if (equipAttributeComponent == null) return;

        EnchantComponent enchantComponent = smpItem.getComponent(ItemComponentKeys.ENCHANT);
        if (enchantComponent == null) return;
        EquipSlot slot = equipAttributeComponent.getSlot();
        Map<Enchants, Integer> enchantMap = enchantComponent.getTotalEnchants();
        Map<Attributes, Double> attributesModifiers = new EnumMap<>(Attributes.class);
        enchantMap.forEach((enchants, integer) -> {
            if (enchants.getEnchant().getActiveSlots().contains(slot)) {
                Map<Attributes, Double> enchantAttributes = enchants.getEnchant().provideAttributes(integer);
                enchantAttributes.forEach((attributes, aDouble) -> {
                    attributesModifiers.merge(attributes, aDouble, Double::sum);
                });
            }
        });

        if (attributesModifiers.isEmpty()) {
            equipAttributeComponent.removeModifier("enchant_attribute_modifier");
        } else {
            equipAttributeComponent.putModifier("enchant_attribute_modifier", attributesModifiers);
        }
    }
}
