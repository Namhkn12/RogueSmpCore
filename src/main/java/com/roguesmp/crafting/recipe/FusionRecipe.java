package com.roguesmp.crafting.recipe;

import com.roguesmp.codec.Codec;
import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.input.FusionMatrix;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A recipe for a "fusion altar" style GUI: a distinguished {@code "input"} item plus a flat,
 * position-independent list of {@code "ingredients"} (e.g. 8 pedestal slots surrounding the input -
 * see {@link FusionMatrix}'s doc for the shape this is meant for). Unlike {@link ShapelessCraftingRecipe},
 * matching is <b>exact</b>: the input's key and amount must match precisely, and the ingredient
 * slots' total count per key must match precisely too - no extra ingredient types, no more or less
 * of a listed one, and no fewer/more of the input than required.
 * <p>
 * The result replaces the input slot itself (it isn't consumed/emptied the way an ingredient can
 * be) - see {@link #getResultStack()} from the base class for what to put there.
 */
public final class FusionRecipe extends CraftingRecipe {

    public static final String TYPE_KEY = "fusion";

    public static final Codec<FusionRecipe> CODEC = Codec.composite(
            CraftingRecipe.BASE_CODEC.forGetter(CraftingRecipe::getBaseProperties),
            CraftingIngredient.CODEC.fieldOf("input").forGetter(FusionRecipe::getInput),
            Codec.listOf(CraftingIngredient.CODEC).fieldOf("ingredients").forGetter(FusionRecipe::getIngredients),
            FusionRecipe::new
    );

    private final CraftingIngredient input;
    private final List<CraftingIngredient> ingredients;

    /** {@code ingredients} merged by key (duplicates summed), sorted by key for a stable trie path. */
    private final Map<String, Integer> requiredCounts;

    public FusionRecipe(@NotNull BaseProperties base, @NotNull CraftingIngredient input, @NotNull List<CraftingIngredient> ingredients) {
        super(base);
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

    /**
     * Builds the trie path for an arbitrary input key + ingredient key set - identity only, no
     * counts (exact amounts are checked by {@link #matches}). Used both by {@link #getIndexPath()}
     * and by the crafting manager to turn a runtime {@link FusionMatrix}'s input/ingredient keys
     * into a query path directly comparable against a registered recipe's own path.
     */
    public static @NotNull List<String> toIndexPath(@NotNull String inputKey, @NotNull Set<String> ingredientKeys) {
        List<String> path = new ArrayList<>(ingredientKeys.size() + 1);
        path.add(inputKey);
        path.addAll(new TreeSet<>(ingredientKeys));
        return path;
    }

    /** Trie path: the input key first, then one segment per distinct ingredient key, sorted. */
    public @NotNull List<String> getIndexPath() {
        return toIndexPath(input.key(), requiredCounts.keySet());
    }

    public boolean matches(@NotNull FusionMatrix matrix) {
        String inputKey = matrix.inputKey();
        if (inputKey == null || !inputKey.equals(input.key())) return false;

        ItemStack inputStack = matrix.input();
        if (inputStack == null || inputStack.getAmount() != input.count()) return false; // exact, not "at least"

        Map<String, Integer> actual = matrix.aggregateIngredientCounts();
        if (actual.size() != requiredCounts.size()) return false; // an unlisted ingredient type is present, or one's missing entirely

        for (Map.Entry<String, Integer> entry : requiredCounts.entrySet()) {
            Integer have = actual.get(entry.getKey());
            if (have == null || !have.equals(entry.getValue())) return false; // exact
        }
        return true;
    }

    /**
     * Resolves what each ingredient slot of {@code matrix} should become after one fusion - same
     * indices as {@link FusionMatrix#ingredients()}. The input slot isn't part of this array; the
     * caller replaces it directly with {@link #getResultStack()}. Undefined if {@code matrix}
     * doesn't actually {@link #matches} - check that first.
     */
    public @NotNull ItemStack[] consume(@NotNull FusionMatrix matrix) {
        ItemStack[] ingredientStacks = matrix.ingredients();
        int[] amountsToConsume = new int[ingredientStacks.length];
        Map<String, Integer> stillNeeded = new HashMap<>(requiredCounts);

        for (int i = 0; i < ingredientStacks.length; i++) {
            ItemStack stack = ingredientStacks[i];
            String key = CraftingIngredient.resolveKey(stack);
            if (key == null) continue;

            Integer needed = stillNeeded.get(key);
            if (needed == null || needed <= 0) continue;

            int take = Math.min(needed, stack.getAmount());
            amountsToConsume[i] = take;
            stillNeeded.put(key, needed - take);
        }

        return applyConsumption(ingredientStacks, amountsToConsume);
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
