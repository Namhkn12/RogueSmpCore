package com.roguesmp.player.ability;

import com.roguesmp.constant.AbilityTrigger;
import com.roguesmp.player.SmpPlayer;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * This class hold static information of an ability (for use in Registry, creating ability instance, description, etc...)
 */
public record AbilityInfo<T extends Ability>(String id, BiFunction<SmpPlayer, Integer, List<Component>> descriptionProvider,
                                             Component displayText, Material displayIcon,
                                             BiFunction<SmpPlayer, Integer, T> factory, AbilityTrigger trigger) {

    public ItemStack createInfoItem(SmpPlayer smpPlayer, int level) {
        ItemStack item = ItemStack.of(displayIcon);

        item.setData(DataComponentTypes.ITEM_NAME, displayText);

        List<Component> lore = new ArrayList<>();

        lore.add(Component.text("Kích hoạt: ", NamedTextColor.GRAY).append(trigger.simpleName()).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Cấp: " + level, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        lore.addAll(descriptionProvider.apply(smpPlayer, level));

        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));

        return item;
    }

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
