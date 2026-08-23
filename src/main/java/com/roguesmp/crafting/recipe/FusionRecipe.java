package com.roguesmp.crafting.recipe;

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

/**
 * A recipe for a "fusion altar" style GUI: a distinguished {@code "input"} item plus a flat,
 * position-independent list of {@code "ingredients"} (e.g. 8 pedestal slots surrounding the input).
 * Unlike {@link ShapelessCraftingRecipe}, matching is <b>exact</b>: the input's key and amount must
 * match precisely, and the ingredient slots' total count per key must match precisely too - no
 * extra ingredient types, no more or less of a listed one, and no fewer/more of the input than
 * required.
 * <p>
 * Ignores {@code width}/{@code height} (see {@link CraftingRecipe}'s class doc) - by convention,
 * {@code items[0]} is always the input slot and {@code items[1..]} are the ingredient slots; a
 * caller building the array (e.g. a GUI) is responsible for keeping that order.
 * <p>
 * The result replaces the input slot itself (it isn't consumed/emptied the way an ingredient can
 * be) - see {@link #getResultStack()} from the base class for what to put there. {@link #consume}
 * still resolves index 0 the same as any other slot (the input is "fully used" against its own
 * declared amount, so it becomes a resolved remainder or {@code null}) - the caller overwrites that
 * slot with {@link #getResultStack()} afterward, same as it would write any other kind's result
 * into its own separate output slot.
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

    /** The input plus every ingredient, identity only (order doesn't matter - {@link RecipeInput#toIndexPath()} sorts it) - exact amounts are checked separately by {@link #matches}. */
    @Override
    public @NotNull List<RecipeInput> getIndexInputs() {
        List<CraftingIngredient> slots = new ArrayList<>(ingredients.size() + 1);
        slots.add(input);
        slots.addAll(ingredients);
        return List.of(RecipeInput.unordered(slots));
    }

    @Override
    public boolean matches(@NotNull ItemStack @NotNull [] items, int width, int height) {
        if (items.length == 0) return false;

        ItemStack inputStack = items[0];
        String inputKey = CraftingIngredient.resolveKey(inputStack);
        if (inputKey == null || !inputKey.equals(input.key())) return false;
        if (inputStack.getAmount() != input.count()) return false; // exact, not "at least"

        Map<String, Integer> actual = CraftingIngredient.aggregateCounts(ingredientsOf(items));
        if (actual.size() != requiredCounts.size()) return false; // an unlisted ingredient type is present, or one's missing entirely

        for (Map.Entry<String, Integer> entry : requiredCounts.entrySet()) {
            Integer have = actual.get(entry.getKey());
            if (have == null || !have.equals(entry.getValue())) return false; // exact
        }
        return true;
    }

    /**
     * Resolves what each index of {@code items} should become after one fusion - index 0 (the
     * input) resolves like any fully-used ingredient (a declared remainder, or {@code null}); the
     * caller overwrites it with {@link #getResultStack()} afterward. Undefined if {@code items}
     * doesn't actually {@link #matches} - check that first.
     */
    @Override
    public @NotNull ItemStack[] consume(@NotNull ItemStack @NotNull [] items, int width, int height) {
        int[] amountsToConsume = new int[items.length];
        if (items.length > 0) amountsToConsume[0] = input.count(); // the input is always fully used

        Map<String, Integer> stillNeeded = new HashMap<>(requiredCounts);
        for (int i = 1; i < items.length; i++) {
            ItemStack stack = items[i];
            String key = CraftingIngredient.resolveKey(stack);
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
