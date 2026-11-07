package com.roguesmp.crafting.recipe;

public record RecipeKey<T extends Recipe>(String id, String displayName) {
}
