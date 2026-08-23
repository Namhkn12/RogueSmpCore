package com.roguesmp.crafting.input;

import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.recipe.FusionRecipe;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A read-only view over a {@link FusionRecipe} GUI's contents: one distinguished input/output slot
 * plus a flat, position-independent set of ingredient slots surrounding it - e.g. a "fusion altar"
 * style layout with 8 ingredient pedestals around a central catalyst slot. Unlike {@link CraftingMatrix},
 * there's no shape/grid to it; a concrete GUI decides which of its own slots feed the input vs. the
 * ingredients array (only the count/order of {@code ingredients} matters, not their GUI slot numbers).
 */
public final class FusionMatrix {

    private final ItemStack input;
    private final ItemStack[] ingredients;

    public FusionMatrix(@Nullable ItemStack input, @NotNull ItemStack[] ingredients) {
        this.input = input;
        this.ingredients = ingredients;
    }

    public @Nullable ItemStack input() {
        return input;
    }

    public @Nullable String inputKey() {
        return CraftingIngredient.resolveKey(input);
    }

    /** The raw ingredient slot array, in whatever order the caller supplied it. */
    public @NotNull ItemStack[] ingredients() {
        return ingredients;
    }

    /** Sums stack sizes per resolved key across every occupied ingredient slot. */
    public @NotNull Map<String, Integer> aggregateIngredientCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ItemStack stack : ingredients) {
            String key = CraftingIngredient.resolveKey(stack);
            if (key == null) continue;
            counts.merge(key, stack.getAmount(), Integer::sum);
        }
        return counts;
    }
}
