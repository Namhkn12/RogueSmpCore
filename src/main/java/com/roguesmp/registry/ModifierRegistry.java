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

    static {
        modifiers.add(new GemModifier());

        modifiers.add(new EnchantAttributeModifier());
    }

    public static @Unmodifiable List<ItemModifier> getModifiers() {
        return Collections.unmodifiableList(modifiers);
    }
}
