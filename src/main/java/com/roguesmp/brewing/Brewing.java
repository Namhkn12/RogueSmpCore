package com.roguesmp.brewing;

import com.roguesmp.item.component.impl.BrewIngredientComponent;
import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Wynncraft-style additive brewing: every ingredient's rolled stats are summed, and the potion's
 * duration is the base duration plus every ingredient's duration change.
 */
public final class Brewing {

    private Brewing() {}

    public static int durationSeconds(List<BrewIngredientComponent> ingredients, int baseSeconds) {
        int total = baseSeconds;
        for (BrewIngredientComponent ingredient : ingredients) total += ingredient.duration();
        return total;
    }

    public static BrewResult evaluate(BrewRecipe recipe, int baseSeconds, int minSeconds) {
        List<BrewIngredientComponent> ingredients = new ArrayList<>();
        Map<BrewStat, Double> totals = new LinkedHashMap<>();

        for (int i = 0; i < recipe.ingredients().size(); i++) {
            BrewIngredientComponent ingredient = BrewIngredientComponent.of(recipe.ingredients().get(i));
            if (ingredient == null) continue;

            ingredients.add(ingredient);
            int slot = i;
            ingredient.stats().forEach((stat, range) -> totals.merge(stat, range.at(rollFraction(recipe.seed(), slot, stat)), Double::sum));
        }
        totals.entrySet().removeIf(entry -> !entry.getKey().isActive(entry.getValue()));

        int durationTicks = Math.max(minSeconds, durationSeconds(ingredients, baseSeconds)) * 20;
        return new BrewResult(totals, durationTicks, mixColor(ingredients));
    }

    private static double rollFraction(long seed, int slot, BrewStat stat) {
        return new SplittableRandom(seed ^ (31L * slot + stat.id().hashCode()) * 0x9E3779B97F4A7C15L).nextDouble();
    }

    private static int mixColor(List<BrewIngredientComponent> ingredients) {
        if (ingredients.isEmpty()) return 0;

        int red = 0, green = 0, blue = 0;
        for (BrewIngredientComponent ingredient : ingredients) {
            TextColor color = ingredient.textColor();
            red += color.red();
            green += color.green();
            blue += color.blue();
        }
        int count = ingredients.size();
        return (red / count) << 16 | (green / count) << 8 | (blue / count);
    }
}
