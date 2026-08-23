package com.roguesmp.crafting.recipe;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;

/**
 * Every {@link CraftingRecipe} codec. Each constant registers itself into
 * {@link Registries#CRAFTING_RECIPE_CODEC} as it's initialized — call {@link #loadClass()} to
 * force that to happen.
 */
public class CraftingRecipes {

    public static final Codec<ShapedCraftingRecipe> SHAPED = register(ShapedCraftingRecipe.TYPE_KEY, ShapedCraftingRecipe.CODEC);
    public static final Codec<ShapelessCraftingRecipe> SHAPELESS = register(ShapelessCraftingRecipe.TYPE_KEY, ShapelessCraftingRecipe.CODEC);
    public static final Codec<FusionRecipe> FUSION = register(FusionRecipe.TYPE_KEY, FusionRecipe.CODEC);

    public static void loadClass() {

    }

    private static <T extends CraftingRecipe> Codec<T> register(String typeName, Codec<T> codec) {
        return Registries.CRAFTING_RECIPE_CODEC.register(typeName, codec);
    }
}
