package com.roguesmp.crafting.recipe;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

public class CraftingRecipes {
    public static final RecipeKey<ShapedCraftingRecipe> SHAPED;
    public static final RecipeKey<ShapelessCraftingRecipe> SHAPELESS;
    public static final RecipeKey<FusionRecipe> FUSION;
    public static final RecipeKey<CokeOvenRecipe> COKE_OVEN;

    static {
        SHAPED = register(ShapedCraftingRecipe.TYPE_KEY, ShapedCraftingRecipe.CODEC);
        SHAPELESS = register(ShapelessCraftingRecipe.TYPE_KEY, ShapelessCraftingRecipe.CODEC);
        FUSION = register(FusionRecipe.TYPE_KEY, FusionRecipe.CODEC);
        COKE_OVEN = register(CokeOvenRecipe.TYPE_KEY, CokeOvenRecipe.CODEC);
    }

    public static void loadClass() {
    }

    private static <T extends CraftingRecipe> RecipeKey<T> register(String typeName, Codec<T> codec) {
        Registries.CRAFTING_RECIPE_CODEC.register(typeName, codec);
        return new RecipeKey<>(typeName);
    }
}
