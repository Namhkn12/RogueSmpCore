package com.roguesmp.crafting.recipe;

import com.roguesmp.block.BlockDrop;
import com.roguesmp.block.StoredItem;
import com.roguesmp.codec.Codec;
import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.utils.ItemRef;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;

public final class CrusherRecipe extends Recipe {
    public static final String TYPE_KEY = "crusher";
    public static final String TYPE_NAME = "Máy nghiền";

    public static final Codec<CrusherRecipe> CODEC = Codec.composite(
            Recipe.BASE_CODEC.forGetter(Recipe::getBaseProperties),
            CraftingIngredient.CODEC.fieldOf("input").forGetter(CrusherRecipe::getInput),
            Codec.INT.fieldOf("duration").forGetter(CrusherRecipe::getDuration),
            Codec.listOf(BlockDrop.CODEC).optionalFieldOf("extra", List.of()).forGetter(CrusherRecipe::getExtra),
            Codec.INT.optionalFieldOf("min_tier", 1).forGetter(CrusherRecipe::getMinTier),
            CrusherRecipe::new
    );

    private final CraftingIngredient input;
    private final int duration;
    private final List<BlockDrop> extra;
    private final int minTier;

    public CrusherRecipe(@NotNull BaseProperties base, @NotNull CraftingIngredient input, int duration, @NotNull List<BlockDrop> extra, int minTier) {
        super(base);
        this.input = input;
        this.duration = Math.max(1, duration);
        this.extra = List.copyOf(extra);
        this.minTier = Math.max(1, minTier);
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

    public boolean matches(@Nullable ItemStack stack) {
        String key = ItemRef.idOf(stack);
        return key != null && key.equals(input.key()) && stack.getAmount() >= input.count();
    }

    public @NotNull List<StoredItem> roll() {
        List<StoredItem> results = new ArrayList<>();
        for (CraftingIngredient output : getOutputs()) results.add(new StoredItem(output.key(), output.count()));
        for (BlockDrop drop : extra) {
            StoredItem rolled = drop.rollStored();
            if (rolled != null) results.add(rolled);
        }
        return results;
    }

    public @NotNull CraftingIngredient getInput() {
        return input;
    }

    public int getDuration() {
        return duration;
    }

    public @NotNull @Unmodifiable List<BlockDrop> getExtra() {
        return extra;
    }

    public int getMinTier() {
        return minTier;
    }
}
