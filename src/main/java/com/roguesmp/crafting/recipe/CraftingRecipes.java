package com.roguesmp.crafting.recipe;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

/**
 * Every {@link CraftingRecipe} kind's {@link RecipeKey} - reference these (e.g. {@link #FUSION})
 * instead of a bare type-id string, mirroring {@code com.roguesmp.item.component.ItemComponentKeys}.
 * Each constant registers its codec into {@link Registries#CRAFTING_RECIPE_CODEC} as it's
 * initialized — call {@link #loadClass()} to force that to happen.
 */
public class CraftingRecipes {

    public static final RecipeKey<ShapedCraftingRecipe> SHAPED;
    public static final RecipeKey<ShapelessCraftingRecipe> SHAPELESS;
    public static final RecipeKey<FusionRecipe> FUSION;

    static {
        SHAPED = register(ShapedCraftingRecipe.TYPE_KEY, ShapedCraftingRecipe.CODEC);
        SHAPELESS = register(ShapelessCraftingRecipe.TYPE_KEY, ShapelessCraftingRecipe.CODEC);
        FUSION = register(FusionRecipe.TYPE_KEY, FusionRecipe.CODEC);
    }

    public static void loadClass() {

    }

    private static <T extends CraftingRecipe> RecipeKey<T> register(String typeName, Codec<T> codec) {
        Registries.CRAFTING_RECIPE_CODEC.register(typeName, codec);
        return new RecipeKey<>(typeName);
    }
}
