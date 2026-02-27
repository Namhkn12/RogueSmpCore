package com.roguesmp.registry;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.constant.ItemModifierType;
import com.roguesmp.item.modifier.GemModifier;
import com.roguesmp.item.modifier.ItemModifier;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Store ways to read and write extra modifier data to ItemStack/SmpItem
 */
public class ModifierRegistry {
    private static ModifierRegistry INSTANCE = null;

    private final RogueSmpCore plugin;
    private final Map<ItemModifierType, ItemModifier> modifiers = new EnumMap<>(ItemModifierType.class);

    private ModifierRegistry(RogueSmpCore plugin) {
        this.plugin = plugin;

        registerDefault();
    }

    private void registerDefault() {
        modifiers.put(ItemModifierType.GEM, new GemModifier());
    }

    public @Unmodifiable Map<ItemModifierType, ItemModifier> getModifiers() {
        return Collections.unmodifiableMap(modifiers);
    }

    public static void init(RogueSmpCore plugin) {
        INSTANCE = new ModifierRegistry(plugin);
    }

    public static ModifierRegistry getInstance() {
        if (INSTANCE == null) {
            throw new NullPointerException("ModifierRegistry is null");
        }
        return INSTANCE;
    }
}
