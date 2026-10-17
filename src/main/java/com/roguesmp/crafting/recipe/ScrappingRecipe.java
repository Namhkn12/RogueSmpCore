package com.roguesmp.crafting.recipe;

import com.roguesmp.codec.Codec;
import com.roguesmp.crafting.CraftingIngredient;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public final class ScrappingRecipe extends CraftingRecipe {
    public static final String TYPE_KEY = "scrapping";
    public static final String TYPE_NAME = "Phân rã";

    public static final Codec<ScrappingRecipe> CODEC = Codec.composite(
            CraftingRecipe.BASE_CODEC.forGetter(CraftingRecipe::getBaseProperties),
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

    @Override
    public boolean matches(@NotNull ItemStack @NotNull [] items, int width, int height) {
        if (items.length == 0) return false;

        ItemStack stack = items[0];
        String key = CraftingIngredient.resolveKey(stack);
        return key != null && key.equals(input.key()) && stack.getAmount() == input.count(); // exact, not "at least"
    }

    @Override
    public @NotNull ItemStack[] consume(@NotNull ItemStack @NotNull [] items, int width, int height) {
        int[] amountsToConsume = new int[items.length];
        if (items.length > 0) amountsToConsume[0] = input.count();
        return applyConsumption(items, amountsToConsume);
    }

    public @NotNull CraftingIngredient getInput() {
        return input;
    }
}
