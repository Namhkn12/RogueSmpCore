package com.roguesmp.crafting.recipe;

import com.roguesmp.codec.Codec;
import com.roguesmp.registry.Registries;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class CraftingRecipes {
    private static final Map<String, RecipeKey<?>> BY_ID = new LinkedHashMap<>();

    public static final RecipeKey<ShapedCraftingRecipe> SHAPED;
    public static final RecipeKey<ShapelessCraftingRecipe> SHAPELESS;
    public static final RecipeKey<FusionRecipe> FUSION;
    public static final RecipeKey<CokeOvenRecipe> COKE_OVEN;

    static {
        SHAPED = register(ShapedCraftingRecipe.TYPE_KEY, ShapedCraftingRecipe.TYPE_NAME, ShapedCraftingRecipe.CODEC);
        SHAPELESS = register(ShapelessCraftingRecipe.TYPE_KEY, ShapelessCraftingRecipe.TYPE_NAME, ShapelessCraftingRecipe.CODEC);
        FUSION = register(FusionRecipe.TYPE_KEY, FusionRecipe.TYPE_NAME, FusionRecipe.CODEC);
        COKE_OVEN = register(CokeOvenRecipe.TYPE_KEY, CokeOvenRecipe.TYPE_NAME, CokeOvenRecipe.CODEC);
    }

    public static void loadClass() {
    }

    public static @Unmodifiable Collection<RecipeKey<?>> all() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }

    public static @Nullable RecipeKey<?> byId(String id) {
        return BY_ID.get(id);
    }

    private static <T extends CraftingRecipe> RecipeKey<T> register(String id, String displayName, Codec<T> codec) {
        Registries.CRAFTING_RECIPE_CODEC.register(id, codec);
        RecipeKey<T> key = new RecipeKey<>(id, displayName);
        BY_ID.put(id, key);
        return key;
    }
}
