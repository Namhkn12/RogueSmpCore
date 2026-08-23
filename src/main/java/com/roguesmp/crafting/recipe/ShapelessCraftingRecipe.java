package com.roguesmp.crafting.recipe;

import com.roguesmp.codec.Codec;
import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.input.CraftingMatrix;
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
 * A recipe matched by ingredient composition alone, regardless of position - a flat
 * {@code "ingredients"} list, where the same key may appear more than once (its counts are summed).
 * Matches when the grid's total count per resolved key (see {@link CraftingMatrix#aggregateCounts()})
 * is at least this recipe's required count for every listed ingredient, and has no item of any other
 * type - a stack bigger than what's required is fine (extra just stays in the grid after crafting,
 * see {@link #consume}), it's an unlisted ingredient *type* that's rejected.
 */
public final class ShapelessCraftingRecipe extends CraftingRecipe {

    public static final String TYPE_KEY = "shapeless";

    public static final Codec<ShapelessCraftingRecipe> CODEC = Codec.composite(
            CraftingRecipe.BASE_CODEC.forGetter(CraftingRecipe::getBaseProperties),
            Codec.listOf(CraftingIngredient.CODEC).fieldOf("ingredients").forGetter(ShapelessCraftingRecipe::getIngredients),
            ShapelessCraftingRecipe::new
    );

    /**
     * Builds the trie path for an arbitrary set of ingredient keys (sorted, one segment each) -
     * identity only, no counts. Used both by {@link #getIndexPath()} and by the crafting manager to
     * turn a runtime grid's {@link CraftingMatrix#aggregateCounts()} keys into a query path that's
     * directly comparable against a registered recipe's own path. Counts aren't part of the path
     * because matching is a "least count" threshold (see the class doc) - two recipes needing
     * different amounts of the same ingredient set legitimately share one path, disambiguated by
     * {@link #matches} same as {@link ShapedCraftingRecipe}'s per-cell amount check.
     */
    public static @NotNull List<String> toIndexPath(@NotNull Set<String> keys) {
        return new ArrayList<>(new TreeSet<>(keys));
    }

    private final List<CraftingIngredient> ingredients;

    /** {@code ingredients} merged by key (duplicates summed), sorted by key for a stable trie path. */
    private final Map<String, Integer> requiredCounts;

    public ShapelessCraftingRecipe(@NotNull BaseProperties base, @NotNull List<CraftingIngredient> ingredients) {
        super(base);
        this.ingredients = List.copyOf(ingredients);

        Map<String, Integer> merged = new TreeMap<>();
        for (CraftingIngredient ingredient : ingredients) {
            merged.merge(ingredient.key(), ingredient.count(), Integer::sum);
        }
        this.requiredCounts = Collections.unmodifiableMap(merged);
    }

    /** Trie path: one segment per distinct required ingredient key, sorted - identity only, no counts. */
    public @NotNull List<String> getIndexPath() {
        return toIndexPath(requiredCounts.keySet());
    }

    @Override
    public @NotNull String getTypeId() {
        return TYPE_KEY;
    }

    /** Whether this recipe's ingredients are satisfied by the given grid. */
    public boolean matches(@NotNull CraftingMatrix matrix) {
        Map<String, Integer> actual = matrix.aggregateCounts();
        if (actual.size() != requiredCounts.size()) return false; // an unlisted item type is present, or one's missing entirely

        for (Map.Entry<String, Integer> entry : requiredCounts.entrySet()) {
            Integer have = actual.get(entry.getKey());
            if (have == null || have < entry.getValue()) return false;
        }
        return true;
    }

    /**
     * Greedily takes from occupied cells in row-major order until each key's required total is met
     * - a slot's stack may end up only partially consumed (leftover stays, no remainder) if it holds
     * more than what's still needed once earlier slots have already contributed.
     */
    public @NotNull ItemStack[] consume(@NotNull CraftingMatrix matrix) {
        int[] amountsToConsume = new int[matrix.width() * matrix.height()];
        Map<String, Integer> stillNeeded = new HashMap<>(requiredCounts);

        for (int row = 0; row < matrix.height(); row++) {
            for (int col = 0; col < matrix.width(); col++) {
                String key = matrix.keyAt(col, row);
                if (key == null) continue;

                Integer needed = stillNeeded.get(key);
                if (needed == null || needed <= 0) continue;

                ItemStack stack = matrix.stackAt(col, row);
                int take = Math.min(needed, stack.getAmount());
                amountsToConsume[row * matrix.width() + col] = take;
                stillNeeded.put(key, needed - take);
            }
        }

        return applyConsumption(matrix.cellsView(), amountsToConsume);
    }

    public @NotNull @Unmodifiable List<CraftingIngredient> getIngredients() {
        return ingredients;
    }

    public @NotNull @Unmodifiable Map<String, Integer> getRequiredCounts() {
        return requiredCounts;
    }
}
