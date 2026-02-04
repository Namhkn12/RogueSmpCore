package com.roguesmp.item.modifier;

import com.roguesmp.item.component.ComponentKey;
import com.roguesmp.item.component.ItemComponent;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class ModifierContext {
    private final Map<String, ItemComponent> components;

    public ModifierContext(Map<String, ItemComponent> components) {
        this.components = components;
    }

    public <T extends ItemComponent> T getOrCreate(ComponentKey<T> key, @NotNull Supplier<T> factory) {
        return (T) components.computeIfAbsent(key.id(), k -> factory.get());
    }
}
