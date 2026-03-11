package com.roguesmp.player.ability;

import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.player.SmpPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;
import java.util.function.BiFunction;

/**
 * This class hold static information of an ability (for use in Registry, creating ability instance, description, etc...)
 */
public record AbilityInfo<T extends Ability>(String id, BiFunction<SmpPlayer, Integer, List<Component>> descriptionProvider,
                                             Component displayText, Material displayIcon,
                                             BiFunction<SmpPlayer, Integer, T> factory, AbilityTrigger trigger) {

    public static class Builder<T extends Ability> {
        private String id;
        private BiFunction<SmpPlayer, Integer, List<Component>> descriptionProvider;
        private Component displayText;
        private Material displayIcon;
        private BiFunction<SmpPlayer, Integer, T> factory;
        private AbilityTrigger trigger;

        public Builder<T> id(String id) {
            this.id = id;
            return this;
        }

        public Builder<T> descriptionProvider(BiFunction<SmpPlayer, Integer, List<Component>> descriptionProvider) {
            this.descriptionProvider = descriptionProvider;
            return this;
        }

        public Builder<T> displayText(Component displayText) {
            this.displayText = displayText;
            return this;
        }

        public Builder<T> displayIcon(Material displayIcon) {
            this.displayIcon = displayIcon;
            return this;
        }

        public Builder<T> factory(BiFunction<SmpPlayer, Integer, T> factory) {
            this.factory = factory;
            return this;
        }

        public Builder<T> trigger(AbilityTrigger trigger) {
            this.trigger = trigger;
            return this;
        }

        public AbilityInfo<T> build() {
            return new AbilityInfo<>(id, descriptionProvider, displayText, displayIcon, factory, trigger);
        }
    }
}
