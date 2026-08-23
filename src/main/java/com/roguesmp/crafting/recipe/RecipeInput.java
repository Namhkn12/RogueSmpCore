package com.roguesmp.crafting.recipe;

import com.roguesmp.crafting.CraftingIngredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * A recipe's declared input as a flat array of slots - the one shape the trie system understands,
 * so a recipe never has to hand-build its own index path (see {@link CraftingRecipe#getIndexInputs()}).
 * A {@code null} slot means "doesn't matter" (e.g. an empty pattern cell).
 * <p>
 * {@link #positional()} {@code == true} keeps slot order and carries {@link #width()}/{@link #height()}
 * (shaped-style: position - and telling a 1x3 column apart from a 3x1 row - is part of identity).
 * {@code false} ignores order entirely and collapses to a sorted distinct-key set (shapeless/
 * fusion/scrapping-style: only which keys are present matters, not where or how many times a key
 * repeats) - {@link #width()}/{@link #height()} are unused in that case.
 * <p>
 * Only ever used for building the trie index path - actual matching (amounts, exactness, position
 * checks) stays a per-kind concern on the real recipe type, since that genuinely differs kind to
 * kind (see {@link CraftingRecipe}'s class doc). {@code CraftingManager} converts a runtime
 * {@code ItemStack[]} to this same shape before querying, so both sides of a match always build
 * their path the exact same way.
 */
public record RecipeInput(@NotNull List<CraftingIngredient> slots, boolean positional, int width, int height) {

    /** Trie path segment standing in for an empty/absent slot - never a valid {@link CraftingIngredient} key. */
    public static final String EMPTY_SEGMENT = "$empty";

    public static @NotNull RecipeInput positional(@NotNull List<@Nullable CraftingIngredient> slots, int width, int height) {
        return new RecipeInput(slots, true, width, height);
    }

    public static @NotNull RecipeInput unordered(@NotNull List<CraftingIngredient> slots) {
        return new RecipeInput(slots, false, 0, 0);
    }

    public @NotNull List<String> toIndexPath() {
        if (positional) {
            List<String> path = new ArrayList<>(slots.size() + 2);
            path.add(String.valueOf(width));
            path.add(String.valueOf(height));
            for (CraftingIngredient slot : slots) {
                path.add(slot == null ? EMPTY_SEGMENT : slot.key());
            }
            return path;
        }

        TreeSet<String> keys = new TreeSet<>();
        for (CraftingIngredient slot : slots) {
            if (slot != null) keys.add(slot.key());
        }
        return new ArrayList<>(keys);
    }

    /**
     * For a {@link #positional()} input, trims down to the minimal bounding box of non-null slots -
     * so a shape placed anywhere in a larger grid still lines up with the same recipe's own
     * (already-trimmed) declared {@link RecipeInput}. {@code null} if every slot is empty. A
     * non-positional input has no notion of position to trim, so it's returned unchanged.
     */
    public @Nullable RecipeInput trim() {
        if (!positional) return this;

        int minRow = height, maxRow = -1, minCol = width, maxCol = -1;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (slots.get(row * width + col) == null) continue;
                minRow = Math.min(minRow, row);
                maxRow = Math.max(maxRow, row);
                minCol = Math.min(minCol, col);
                maxCol = Math.max(maxCol, col);
            }
        }
        if (maxRow < 0) return null; // entirely empty

        int trimmedWidth = maxCol - minCol + 1;
        int trimmedHeight = maxRow - minRow + 1;
        List<CraftingIngredient> trimmed = new ArrayList<>(trimmedWidth * trimmedHeight);
        for (int row = 0; row < trimmedHeight; row++) {
            for (int col = 0; col < trimmedWidth; col++) {
                trimmed.add(slots.get((minRow + row) * width + (minCol + col)));
            }
        }
        return RecipeInput.positional(trimmed, trimmedWidth, trimmedHeight);
    }
}
