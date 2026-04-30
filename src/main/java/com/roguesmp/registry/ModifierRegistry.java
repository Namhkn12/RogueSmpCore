package com.roguesmp.registry;

import com.roguesmp.item.modifier.EnchantAttributeModifier;
import com.roguesmp.item.modifier.GemModifier;
import com.roguesmp.item.modifier.ItemModifier;
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

        UNMODIFIABLE_MODIFIERS = Collections.unmodifiableList(modifiers);
    }

    public static @Unmodifiable List<ItemModifier> getModifiers() {
        return UNMODIFIABLE_MODIFIERS;
    }
}
