package com.roguesmp.item;

import com.roguesmp.codec.Codec;
import com.roguesmp.item.component.ComponentKey;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.component.UniqueTrackingComponent;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.registry.Registries;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represent base item data (prototype)
 */
public class BaseItem {
    private final String id;
    private final Material base;
    private final Map<String, ItemComponent> components;
    private final boolean unique;

    public static final Codec<BaseItem> CODEC = Codec.composite(
            Codec.STRING.fieldOf("id").forGetter(BaseItem::getId),
            Codec.MATERIAL.fieldOf("base").forGetter(BaseItem::getBase),
            Codec.<ItemComponent>dispatchedMap(Registries.ITEM_COMPONENT_CODEC::getOrThrow)
                    .optionalFieldOf("components", new HashMap<>())
                    .forGetter(BaseItem::getComponents),
            BaseItem::new
    );

    public BaseItem(String id, Material base, Map<String, ItemComponent> components) {
        this.id = id;
        this.base = base;
        this.components = components;
        this.unique = components.values().stream().anyMatch(component -> component instanceof UniqueTrackingComponent);
    }

    @SuppressWarnings("unchecked")
    public @Nullable <T extends ItemComponent> T getComponent(ComponentKey<T> key) {
        return (T) components.get(key.id());
    }

    public <T extends ItemComponent> boolean hasComponent(ComponentKey<T> key) {
        return components.containsKey(key.id());
    }

    public ItemStack generateItemStack(@Nullable SmpPlayer player, int stackAmount) {
        return new SmpItem(this).generateItemStack(player, stackAmount);
    }

    public ItemStack generateItemStack(int stackAmount) {
        return new SmpItem(this).generateItemStack(stackAmount);
    }

    public @Unmodifiable Map<String, ItemComponent> getComponents() {
        return Collections.unmodifiableMap(components);
    }

    @SuppressWarnings("unchecked")
    public <T extends ItemComponent> T getComponentOrDefault(ComponentKey<T> key, T defaultValue) {
        return (T) components.getOrDefault(key.id(), defaultValue);
    }

    public String getId() {
        return id;
    }

    public Material getBase() {
        return base;
    }

    public boolean isUnique() {
        return unique;
    }
}
