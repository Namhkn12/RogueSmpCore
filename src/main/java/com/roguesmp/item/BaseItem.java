package com.roguesmp.item;

import com.roguesmp.constant.Keys;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ComponentKey;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.lore.LoreBuilder;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Represent base item data (prototype)
 */
public class BaseItem {
    private final String id;
    private final Material base;
    private final Map<String, ItemComponent> components;

    public BaseItem(String id, Material base, Map<String, ItemComponent> components) {
        this.id = id;
        this.base = base;
        this.components = components;
    }

    public @Nullable <T extends ItemComponent> T getComponent(ComponentKey<T> key) {
        return (T) components.get(key.id());
    }

    public ItemStack generateItemStack(SmpPlayer player, int stackAmount) {
        return new SmpItem(this).generateItemStack(player, stackAmount);
    }

    public @Unmodifiable Map<String, ItemComponent> getComponents() {
        return Collections.unmodifiableMap(components);
    }

    public <T extends ItemComponent> T getComponentOrDefault(ComponentKey<T> key, T defaultValue) {
        return (T) components.getOrDefault(key.id(), defaultValue);
    }

    public String getId() {
        return id;
    }

    public Material getBase() {
        return base;
    }
}
