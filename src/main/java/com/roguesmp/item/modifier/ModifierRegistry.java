package com.roguesmp.item.modifier;

import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

/**
 * Store ways to read and write extra modifier data to ItemStack/SmpItem
 */
public class ModifierRegistry {

    private static final List<ItemModifier> modifiers = new ArrayList<>();
    private static final List<ItemModifier> UNMODIFIABLE_MODIFIERS;

    static {
        modifiers.add(new GemModifier());
        modifiers.add(new EnchantAttributeModifier());

        modifiers.add(new BrokenModifier());

        UNMODIFIABLE_MODIFIERS = Collections.unmodifiableList(modifiers);
    }

    public static @Unmodifiable List<ItemModifier> getModifiers() {
        return UNMODIFIABLE_MODIFIERS;
    }
}
