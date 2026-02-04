package com.roguesmp.item.modifier.effect.impl;

import com.roguesmp.constant.ComponentKeys;
import com.roguesmp.constant.Enchants;
import com.roguesmp.item.component.impl.EnchantComponent;
import com.roguesmp.item.modifier.ModifierContext;
import com.roguesmp.item.modifier.effect.ModifierEffect;

import java.util.Map;

public class EnchantModifierEffect implements ModifierEffect {

    private final Map<Enchants, Integer> modifiers;

    public EnchantModifierEffect(Map<Enchants, Integer> modifiers) {
        this.modifiers = modifiers;
    }

    @Override
    public void apply(ModifierContext context) {
        EnchantComponent component = context.getOrCreate(ComponentKeys.ENCHANT, () -> new EnchantComponent(Map.of()));
        component.addModifier(new EnchantComponent.Modifier(10, modifiers));
    }
}
