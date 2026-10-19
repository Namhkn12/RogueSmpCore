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
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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

        stats.forEach((stat, range) -> {
            List<Component> min = stat.display(range.min(), 0);
            if (range.isFixed()) {
                min.forEach(line -> lore.add(BrewStat.indent(line)));
                return;
            }
            List<Component> max = stat.display(range.max(), 0);
            for (int i = 0; i < Math.min(min.size(), max.size()); i++) {
                lore.add(BrewStat.indent(rangeLine(min.get(i), max.get(i))));
            }
        });

        if (duration != 0) {
            String sign = duration > 0 ? "+" : "";
            lore.add(BrewStat.indent(Utils.text(sign + duration + "s thời gian hiệu lực", duration > 0 ? NamedTextColor.GREEN : NamedTextColor.RED)));
        }

        context.builder().putLines(LORE_PRIORITY, lore);
    }

    private static Component rangeLine(Component min, Component max) {
        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
        String minText = plain.serialize(min);
        String maxText = plain.serialize(max);
        int minSplit = minText.indexOf(' ');
        int maxSplit = maxText.indexOf(' ');
        if (minSplit < 0 || maxSplit < 0 || !minText.substring(minSplit).equals(maxText.substring(maxSplit))) {
            return min.append(Utils.text(" ~ ", NamedTextColor.DARK_GRAY)).append(max);
        }
        return Component.text(minText.substring(0, minSplit) + " ~ " + maxText.substring(0, maxSplit) + maxText.substring(maxSplit), max.color());
    }
}
