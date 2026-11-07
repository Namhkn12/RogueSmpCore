package com.roguesmp.crafting.recipe;

import com.roguesmp.utils.ItemRef;
import com.roguesmp.codec.Codec;
import com.roguesmp.crafting.CraftingIngredient;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public final class ScrappingRecipe extends Recipe {
    public static final String TYPE_KEY = "scrapping";
    public static final String TYPE_NAME = "Phân rã";

    public static final Codec<ScrappingRecipe> CODEC = Codec.composite(
            Recipe.BASE_CODEC.forGetter(Recipe::getBaseProperties),
            CraftingIngredient.CODEC.fieldOf("input").forGetter(ScrappingRecipe::getInput),
            ScrappingRecipe::new
    );

    private final CraftingIngredient input;

    public ScrappingRecipe(@NotNull BaseProperties base, @NotNull CraftingIngredient input) {
        super(base);
        this.input = input;
    }

    @Override
    public @NotNull String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public @NotNull String getTypeName() {
        return TYPE_NAME;
    }

    @Override
    public @NotNull List<RecipeInput> getIndexInputs() {
        return List.of(RecipeInput.unordered(List.of(input)));
    }

    @Override
    public @NotNull @Unmodifiable List<CraftingIngredient> getInputs() {
        return List.of(input);
    }

    public boolean matches(@NotNull ItemStack @NotNull [] items) {
        if (items.length == 0) return false;

        ItemStack stack = items[0];
        String key = ItemRef.idOf(stack);
        return key != null && key.equals(input.key()) && stack.getAmount() == input.count(); // exact, not "at least"
    }

    public @NotNull CraftingIngredient getInput() {
        return input;
    }
}
