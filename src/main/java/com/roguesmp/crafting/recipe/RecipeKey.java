package com.roguesmp.crafting.recipe;

public record RecipeKey<T extends CraftingRecipe>(String id, String displayName) {
}
