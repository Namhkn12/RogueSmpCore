package com.roguesmp.player.ability;

import com.roguesmp.player.SmpPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiFunction;

/**
 * This class hold static information of an ability (for use in Registry, creating ability instance, description, etc...)
 */
public class AbilityInfo<T extends Ability> {
    private final String id;
    private final BiFunction<SmpPlayer, Integer, Component> descriptionProvider;
    private final Component displayText;
    private final ItemStack displayItem;
    private final BiFunction<SmpPlayer, Integer, T> factory;

    public AbilityInfo(String id, BiFunction<SmpPlayer, Integer, Component> descriptionProvider, Component displayText, ItemStack displayItem, BiFunction<SmpPlayer, Integer, T> factory) {
        this.id = id;
        this.descriptionProvider = descriptionProvider;
        this.displayText = displayText;
        this.displayItem = displayItem;
        this.factory = factory;
    }

    public String getId() {
        return id;
    }

    public BiFunction<SmpPlayer, Integer, Component> getDescriptionProvider() {
        return descriptionProvider;
    }

    public Component getDisplayText() {
        return displayText;
    }

    public BiFunction<SmpPlayer, Integer, T> getFactory() {
        return factory;
    }

    public ItemStack getDisplayItem() {
        return displayItem;
    }

    public static class Builder<T extends Ability> {
        private String id;
        private BiFunction<SmpPlayer, Integer, Component> descriptionProvider;
        private Component displayText;
        private ItemStack displayItem;
        private BiFunction<SmpPlayer, Integer, T> factory;

        public Builder<T> id(String id) {
            this.id = id;
            return this;
        }

        public Builder<T> descriptionProvider(BiFunction<SmpPlayer, Integer, Component> descriptionProvider) {
            this.descriptionProvider = descriptionProvider;
            return this;
        }

        public Builder<T> displayText(Component displayText) {
            this.displayText = displayText;
            return this;
        }

        public Builder<T> displayItem(ItemStack displayItem) {
            this.displayItem = displayItem;
            return this;
        }

        public Builder<T> factory(BiFunction<SmpPlayer, Integer, T> factory) {
            this.factory = factory;
            return this;
        }

        public AbilityInfo<T> build() {
            return new AbilityInfo<>(id, descriptionProvider, displayText, displayItem, factory);
        }
    }
}
