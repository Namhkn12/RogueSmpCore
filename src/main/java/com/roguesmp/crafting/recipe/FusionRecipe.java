package com.roguesmp.crafting.recipe;

import com.roguesmp.utils.ItemRef;
import com.roguesmp.codec.Codec;
import com.roguesmp.crafting.CraftingIngredient;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class FusionRecipe extends CraftingRecipe {
    public static final String TYPE_KEY = "fusion";
    public static final String TYPE_NAME = "Dung hợp";

    public static final Codec<FusionRecipe> CODEC = Codec.composite(
            CraftingRecipe.BASE_CODEC.forGetter(CraftingRecipe::getBaseProperties),
            CraftingIngredient.CODEC.fieldOf("input").forGetter(FusionRecipe::getInput),
            Codec.listOf(CraftingIngredient.CODEC).fieldOf("ingredients").forGetter(FusionRecipe::getIngredients),
            FusionRecipe::new
    );

    private final CraftingIngredient input;
    private final List<CraftingIngredient> ingredients;

    private final Map<String, Integer> requiredCounts;

    public FusionRecipe(@NotNull BaseProperties base, @NotNull CraftingIngredient input, @NotNull List<CraftingIngredient> ingredients) {
        super(base, 1);
        this.input = input;
        this.ingredients = List.copyOf(ingredients);

        Map<String, Integer> merged = new TreeMap<>();
        for (CraftingIngredient ingredient : ingredients) {
            merged.merge(ingredient.key(), ingredient.count(), Integer::sum);
        }
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
        List<CraftingIngredient> slots = new ArrayList<>(ingredients.size() + 1);
        slots.add(input);
        slots.addAll(ingredients);
        return List.of(RecipeInput.unordered(slots));
    }

    @Override
    public @NotNull @Unmodifiable List<CraftingIngredient> getInputs() {
        List<CraftingIngredient> inputs = new ArrayList<>(ingredients.size() + 1);
        inputs.add(input);
        inputs.addAll(ingredients);
        return Collections.unmodifiableList(inputs);
    }

    @Override
    public boolean matches(@NotNull ItemStack @NotNull [] items, int width, int height) {
        if (items.length == 0) return false;

        ItemStack inputStack = items[0];
        String inputKey = ItemRef.idOf(inputStack);
        if (inputKey == null || !inputKey.equals(input.key())) return false;
        if (inputStack.getAmount() != input.count()) return false; // exact, not "at least"

        Map<String, Integer> actual = CraftingIngredient.aggregateCounts(ingredientsOf(items));
        if (actual.size() != requiredCounts.size()) return false;

        for (Map.Entry<String, Integer> entry : requiredCounts.entrySet()) {
            Integer have = actual.get(entry.getKey());
            if (have == null || !have.equals(entry.getValue())) return false;
        }
        return true;
    }

    @Override
    public @NotNull ItemStack[] consume(@NotNull ItemStack @NotNull [] items, int width, int height) {
        int[] amountsToConsume = new int[items.length];
        if (items.length > 0) amountsToConsume[0] = input.count();

        Map<String, Integer> stillNeeded = new HashMap<>(requiredCounts);
        for (int i = 1; i < items.length; i++) {
            ItemStack stack = items[i];
            String key = ItemRef.idOf(stack);
            if (key == null) continue;

            Integer needed = stillNeeded.get(key);
            if (needed == null || needed <= 0) continue;

            int take = Math.min(needed, stack.getAmount());
            amountsToConsume[i] = take;
            stillNeeded.put(key, needed - take);
        }

        return applyConsumption(items, amountsToConsume);
    }

    private static ItemStack[] ingredientsOf(ItemStack[] items) {
        return items.length <= 1 ? new ItemStack[0] : Arrays.copyOfRange(items, 1, items.length);
    }

    public @NotNull CraftingIngredient getInput() {
        return input;
    }

    public @NotNull @Unmodifiable List<CraftingIngredient> getIngredients() {
        return ingredients;
    }

    public @NotNull @Unmodifiable Map<String, Integer> getRequiredCounts() {
        return requiredCounts;
    }
}
