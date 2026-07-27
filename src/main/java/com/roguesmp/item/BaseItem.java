package com.roguesmp.item;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.item.component.ComponentKey;
import com.roguesmp.item.component.ItemComponent;
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
    private final boolean unique;
    private final Map<String, ItemComponent> components;

    public static final Codec<BaseItem> CODEC = Codec.composite(
            Codec.STRING.fieldOf("id").forGetter(BaseItem::getId),
            Codec.MATERIAL.fieldOf("base").forGetter(BaseItem::getBase),
            Codec.BOOLEAN.optionalFieldOf("unique", false).forGetter(BaseItem::isUnique),
            Codec.<ItemComponent>dispatchedMap(Registries.ITEM_COMPONENT_CODEC::getOrThrow)
                    .optionalFieldOf("components", new HashMap<>())
                    .forGetter(BaseItem::getComponents),
            BaseItem::new
    );

    /**
     * Codec for referencing an already-registered BaseItem by id (e.g. from another definition's
     * JSON), instead of embedding a full item definition inline.
     */
    public static final Codec<BaseItem> REFERENCE_CODEC = Codec.STRING.comapFlatMap(
            id -> {
                BaseItem item = Registries.ITEM.get(id);
                return item != null ? DataResult.success(item) : DataResult.error("Unknown item: " + id);
            },
            BaseItem::getId
    );

    public BaseItem(String id, Material base, boolean unique, Map<String, ItemComponent> components) {
        this.id = id;
        this.base = base;
        this.unique = unique;
        this.components = components;
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
