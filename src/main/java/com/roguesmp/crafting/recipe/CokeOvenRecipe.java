package com.roguesmp.crafting.recipe;

import com.roguesmp.utils.ItemRef;
import com.roguesmp.codec.Codec;
import com.roguesmp.crafting.CraftingIngredient;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class CokeOvenRecipe extends CraftingRecipe {
    public static final String TYPE_KEY = "coke_oven";
    public static final String TYPE_NAME = "Lò luyện cốc";

    public static final Codec<CokeOvenRecipe> CODEC = Codec.composite(
            CraftingRecipe.BASE_CODEC.forGetter(CraftingRecipe::getBaseProperties),
            Codec.listOf(CraftingIngredient.CODEC).fieldOf("ingredients").forGetter(CokeOvenRecipe::getIngredients),
            Codec.INT.fieldOf("duration").forGetter(CokeOvenRecipe::getDuration),
            CokeOvenRecipe::new
    );

    private final List<CraftingIngredient> ingredients;
    private final int duration;
    private final Map<String, Integer> requiredCounts;

    public CokeOvenRecipe(@NotNull BaseProperties base, @NotNull List<CraftingIngredient> ingredients, int duration) {
        super(base);
        this.ingredients = List.copyOf(ingredients);
        this.duration = Math.max(1, duration);

        Map<String, Integer> merged = new TreeMap<>();
        for (CraftingIngredient ingredient : ingredients) merged.merge(ingredient.key(), ingredient.count(), Integer::sum);
        this.requiredCounts = Collections.unmodifiableMap(merged);
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
        return List.of(RecipeInput.unordered(ingredients));
    }

    @Override
    public @NotNull @Unmodifiable List<CraftingIngredient> getInputs() {
        return ingredients;
    }

    @Override
    public boolean matches(@NotNull ItemStack @NotNull [] items, int width, int height) {
        Map<String, Integer> actual = CraftingIngredient.aggregateCounts(items);
        if (actual.size() != requiredCounts.size()) return false;

        for (Map.Entry<String, Integer> entry : requiredCounts.entrySet()) {
            Integer have = actual.get(entry.getKey());
            if (have == null || have < entry.getValue()) return false;
        }
        return true;
    }

    @Override
    public @NotNull ItemStack[] consume(@NotNull ItemStack @NotNull [] items, int width, int height) {
        int[] amountsToConsume = new int[items.length];
        Map<String, Integer> stillNeeded = new HashMap<>(requiredCounts);
        for (int i = 0; i < items.length; i++) {
            String key = ItemRef.idOf(items[i]);
            if (key == null) continue;

            Integer needed = stillNeeded.get(key);
            if (needed == null || needed <= 0) continue;

            int take = Math.min(needed, items[i].getAmount());
            amountsToConsume[i] = take;
            stillNeeded.put(key, needed - take);
        }
        return applyConsumption(items, amountsToConsume);
    }

    public @NotNull @Unmodifiable List<CraftingIngredient> getIngredients() {
        return ingredients;
    }

    public @NotNull @Unmodifiable Map<String, Integer> getRequiredCounts() {
        return requiredCounts;
    }

    public int getDuration() {
        return duration;
    }
}
