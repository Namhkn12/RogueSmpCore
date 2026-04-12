package com.roguesmp.player.ability;

import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.player.SmpPlayer;
import com.roguesmp.player.ability.upgrade.UpgradeRequirement;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.BiFunction;

/**
 * This class hold static information of an ability (for use in Registry, creating ability instance, description, etc...)
 */
public record AbilityInfo<T extends Ability>(String id, BiFunction<SmpPlayer, Integer, List<Component>> descriptionProvider,
                                             Component displayText, Material displayIcon,
                                             BiFunction<SmpPlayer, Integer, T> factory, AbilityTrigger trigger,
                                             Map<Integer, List<UpgradeRequirement>> requirements) {

    @Override
    public @Unmodifiable Map<Integer, List<UpgradeRequirement>> requirements() {
        return Collections.unmodifiableMap(requirements);
    }

    public static class Builder<T extends Ability> {
        // Required fields
        private String id;
        private BiFunction<SmpPlayer, Integer, T> factory;

        // Optional fields with sensible defaults
        private BiFunction<SmpPlayer, Integer, List<Component>> descriptionProvider = (p, l) -> List.of();
        private Component displayText = Component.text("Unknown Display");
        private Material displayIcon = Material.BARRIER;
        private AbilityTrigger trigger = AbilityTrigger.PASSIVE; // Or your preferred default
        private Map<Integer, List<UpgradeRequirement>> requirements = new HashMap<>();

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

        public Builder<T> requirements(Map<Integer, List<UpgradeRequirement>> requirements) {
            this.requirements = requirements;
            return this;
        }

        public AbilityInfo<T> build() {
            return new AbilityInfo<>(id, descriptionProvider, displayText, displayIcon, factory, trigger, requirements);
        }
    }
}
