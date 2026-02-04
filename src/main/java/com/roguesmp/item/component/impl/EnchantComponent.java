package com.roguesmp.item.component.impl;

import com.roguesmp.annotation.GsonIgnore;
import com.roguesmp.constant.Enchants;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class EnchantComponent implements ItemComponent {
    private final Map<Enchants, Integer> enchants = new EnumMap<>(Enchants.class);

    @GsonIgnore
    private EnumMap<Enchants, Integer> modifierEnchants = null;

    public EnchantComponent(Map<Enchants, Integer> enchants) {
        this.enchants.putAll(enchants);
    }

    public int getLevel(Enchants enchants) {
        return this.enchants.getOrDefault(enchants, 0);
    }

    public int getModifierLevel(Enchants enchants) {
        return this.modifierEnchants == null ? 0 : this.modifierEnchants.getOrDefault(enchants, 0);
    }

    public @Unmodifiable Map<Enchants, Integer> getEnchants() {
        return Collections.unmodifiableMap(enchants);
    }

    public void addModifier(Modifier modifier) {
        if (modifierEnchants == null) {
            modifierEnchants = new EnumMap<>(Enchants.class);
        }

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
            int bonus = getModifierLevel(enchants);

            List<Component> lines = enchants.getEnchant().getDisplayText(level, context.player(), context.data());
            if (bonus != 0) {
                lines = appendModifierText(lines, bonus);
            }

            res.addAll(lines);
        }

        context.builder().putLines(3, res);
    }

    private List<Component> appendModifierText(List<Component> lines, int bonus) {
        List<Component> result = new ArrayList<>();
        TextColor color = NamedTextColor.GREEN;
        String prefix = "+";
        if (bonus <= 0) {
            color = NamedTextColor.RED;
            prefix = "-";
        }
        for (Component c : lines) {
            result.add(c.append(Component.text("( "+ prefix + bonus + ")").color(color)));
        }
        return result;
    }

    public record Modifier(int priority, Map<Enchants, Integer> modifiers){}
}
