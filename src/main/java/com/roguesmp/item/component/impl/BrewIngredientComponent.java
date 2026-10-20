package com.roguesmp.item.component.impl;

import com.roguesmp.brewing.BrewStat;
import com.roguesmp.brewing.StatRange;
import com.roguesmp.codec.Codec;
import com.roguesmp.context.ItemLoreContext;
import com.roguesmp.item.component.ItemComponent;
import com.roguesmp.item.component.ItemComponentKeys;
import com.roguesmp.utils.ItemRef;
import com.roguesmp.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record BrewIngredientComponent(Map<BrewStat, StatRange> stats, int duration, String color) implements ItemComponent {

    private static final int LORE_PRIORITY = 5;
    private static final String DEFAULT_COLOR = "#8040C0";

    public static final Codec<BrewIngredientComponent> CODEC = Codec.composite(
            Codec.unboundedMap(BrewStat.CODEC, StatRange.CODEC).optionalFieldOf("stats", Map.of()).forGetter(BrewIngredientComponent::stats),
            Codec.INT.optionalFieldOf("duration", 0).forGetter(BrewIngredientComponent::duration),
            Codec.STRING.optionalFieldOf("color", DEFAULT_COLOR).forGetter(BrewIngredientComponent::color),
            BrewIngredientComponent::new
    );

    public static @Nullable BrewIngredientComponent of(String itemId) {
        return ItemRef.component(itemId, ItemComponentKeys.BREW_INGREDIENT);
    }

    public TextColor textColor() {
        TextColor parsed = TextColor.fromHexString(color);
        return parsed != null ? parsed : TextColor.fromHexString(DEFAULT_COLOR);
    }

    @Override
    public @NotNull ItemComponent copy() {
        return this;
    }

    @Override
    public void contributeLore(ItemLoreContext context) {
        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("Nguyên liệu pha chế:", NamedTextColor.GRAY));

        stats.forEach((stat, range) -> stat.displayRange(range.min(), range.max()).forEach(line -> lore.add(BrewStat.indent(line))));

        if (duration != 0) {
            String sign = duration > 0 ? "+" : "";
            lore.add(BrewStat.indent(Utils.text(sign + duration + "s thời gian hiệu lực", duration > 0 ? NamedTextColor.GREEN : NamedTextColor.RED)));
        }

        context.builder().putLines(LORE_PRIORITY, lore);
    }

}
