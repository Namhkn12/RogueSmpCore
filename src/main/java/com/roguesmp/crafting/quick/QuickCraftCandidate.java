package com.roguesmp.crafting.quick;

import com.roguesmp.crafting.recipe.CraftingRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record QuickCraftCandidate(@NotNull CraftingRecipe recipe, @NotNull List<IngredientStatus> ingredients, int maxCrafts) {

    public record IngredientStatus(@NotNull String key, int have, int need) {}
}
