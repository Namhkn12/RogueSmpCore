package com.roguesmp.crafting.recipe;

/**
 * A typed, referenceable handle for one {@link CraftingRecipe} kind - {@code id} is the same
 * string as that kind's own {@code TYPE_KEY}/JSON {@code "type"} value, but callers reference the
 * constant (e.g. {@link CraftingRecipes#FUSION}) instead of a bare string literal, mirroring
 * {@code com.roguesmp.item.component.ComponentKey}. Declared centrally in {@link CraftingRecipes}
 * alongside each kind's codec registration.
 */
public record RecipeKey<T extends CraftingRecipe>(String id) {
}
