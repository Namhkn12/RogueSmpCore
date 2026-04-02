package com.roguesmp.item.component.impl;

import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.constant.Enchants;
import com.roguesmp.context.ItemDataContext;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class EnchantComponent implements ItemComponent {
    private final Map<Enchants, Integer> enchants = new EnumMap<>(Enchants.class);

    @GsonIgnore
    private final EnumMap<Enchants, Integer> modifierEnchants = new EnumMap<>(Enchants.class);

    public EnchantComponent(Map<Enchants, Integer> enchants) {
        this.enchants.putAll(enchants);
    }

    public int getLevel(Enchants enchants) {
        return this.enchants.getOrDefault(enchants, 0);
    }

    public int getModifierLevel(Enchants enchants) {
        return this.modifierEnchants.getOrDefault(enchants, 0);
    }

    public @Unmodifiable Map<Enchants, Integer> getEnchants() {
        return Collections.unmodifiableMap(enchants);
    }

    public void addModifier(Modifier modifier) {

        for (var entry : modifier.modifiers().entrySet()) {
            modifierEnchants.merge(entry.getKey(), entry.getValue(), Integer::sum);
            enchants.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    @Override
    public @NotNull ItemComponent copy() {
        return new EnchantComponent(enchants);
    }

    public void contributeLore(ItemLoreContext context) {

        List<Component> res = new ArrayList<>();
        for (Enchants enchants : enchants.keySet()) {
            int level = getLevel(enchants);

            List<Component> lines = enchants.getEnchant().getDisplayText(level, context.player(), context.data());
            if (lines != null) res.addAll(lines);
        }

        context.builder().putLines(3, res);
    }

    @Override
    public void save(PersistentDataContainer pdc) {
        enchants.forEach((enchants1, integer) -> {
            enchants1.getEnchant().attachData(pdc);
        });
    }

    @Override
    public void modifyStack(ItemDataContext context) {
        Map<Enchantment, Integer> enchantMap = new HashMap<>();
        enchants.forEach((enchants1, integer) -> enchants1.getEnchant().attachVanillaEnchant(enchantMap, integer));
        if (!enchantMap.isEmpty()) {
            context.newStack().setData(DataComponentTypes.ENCHANTMENTS, ItemEnchantments.itemEnchantments(enchantMap));
        }
    }

    public record Modifier(int priority, Map<Enchants, Integer> modifiers){}
}
