package com.roguesmp.item.modifier;

import com.roguesmp.item.modifier.effect.ModifierEffect;

import java.util.List;

public record ItemModifier(String id, List<ModifierEffect> effects) {

    public void apply(ModifierContext modifierContext) {
        effects.forEach(modifierEffect -> {
            modifierEffect.apply(modifierContext);
        });
    }
}
