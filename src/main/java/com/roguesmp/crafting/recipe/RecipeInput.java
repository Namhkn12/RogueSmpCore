package com.roguesmp.crafting.recipe;

import com.roguesmp.crafting.CraftingIngredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public record RecipeInput(@NotNull List<CraftingIngredient> slots, boolean positional, int width, int height) {
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
        if (maxRow < 0) return null;

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
