package com.roguesmp.crafting.input;

import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.recipe.CraftingRecipe;
import com.roguesmp.crafting.recipe.ShapedCraftingRecipe;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A read-only view over a crafting grid's contents (e.g. a crafting table's 3x3, or a player
 * inventory's 2x2), used to match against {@link CraftingRecipe}s. Cells are row-major:
 * index {@code row * width + col}.
 */
public final class CraftingMatrix {

    /** Trie path segment standing in for an empty cell - never a valid {@link CraftingIngredient} key. */
    public static final String EMPTY_SEGMENT = "$empty";

    private final ItemStack[] cells;
    private final int width;
    private final int height;

    public CraftingMatrix(@NotNull ItemStack[] cells, int width, int height) {
        if (cells.length != width * height) {
            throw new IllegalArgumentException("Expected " + (width * height) + " cells for a " + width + "x" + height + " matrix, got " + cells.length);
        }
        this.cells = cells;
        this.width = width;
        this.height = height;
    }

    public static CraftingMatrix of3x3(@NotNull ItemStack[] cells) {
        return new CraftingMatrix(cells, 3, 3);
    }

    public static CraftingMatrix of2x2(@NotNull ItemStack[] cells) {
        return new CraftingMatrix(cells, 2, 2);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public @Nullable ItemStack stackAt(int col, int row) {
        return cells[row * width + col];
    }

    public @NotNull ItemStack[] cellsView() {
        return cells;
    }

    public @Nullable String keyAt(int col, int row) {
        return CraftingIngredient.resolveKey(stackAt(col, row));
    }

    /**
     * Trims empty leading/trailing rows and columns down to the minimal bounding box of occupied
     * cells, so a recipe's own shape and a runtime grid can be compared regardless of where in the
     * 3x3 the player actually placed their items. Returns {@code null} if every cell is empty.
     */
    public @Nullable Shape trim() {
        return Shape.trim(width, height, rawKeys());
    }

    /**
     * The bounding box {@link #trim()} trimmed down to - lets a caller map a trimmed {@link Shape}'s
     * cell coordinates back to this matrix's own (untrimmed) coordinates. {@code null} iff
     * {@link #trim()} is also {@code null}.
     */
    public @Nullable Shape.Bounds trimBounds() {
        return Shape.boundingBox(width, height, rawKeys());
    }

    /**
     * The actual stack size at each cell of {@link #trim()}'s bounding box, in the same row-major
     * order as its {@link Shape#keys()} (0 for an empty cell) - used to check a shaped recipe's
     * per-cell {@link CraftingIngredient#count()} requirement against what's really in the grid.
     * {@code null} if {@link #trim()} is also {@code null} (an entirely empty grid).
     */
    public int @Nullable [] trimAmounts() {
        String[] keys = rawKeys();
        Shape.Bounds bounds = Shape.boundingBox(width, height, keys);
        if (bounds == null) return null;

        int trimmedWidth = bounds.maxCol() - bounds.minCol() + 1;
        int trimmedHeight = bounds.maxRow() - bounds.minRow() + 1;
        int[] amounts = new int[trimmedWidth * trimmedHeight];
        for (int row = 0; row < trimmedHeight; row++) {
            for (int col = 0; col < trimmedWidth; col++) {
                ItemStack stack = stackAt(bounds.minCol() + col, bounds.minRow() + row);
                amounts[row * trimmedWidth + col] = stack == null ? 0 : stack.getAmount();
            }
        }
        return amounts;
    }

    private String[] rawKeys() {
        String[] keys = new String[width * height];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                keys[row * width + col] = keyAt(col, row);
            }
        }
        return keys;
    }

    /** Sums stack sizes per resolved key across every occupied cell - used for shapeless matching. */
    public @NotNull Map<String, Integer> aggregateCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ItemStack stack : cells) {
            String key = CraftingIngredient.resolveKey(stack);
            if (key == null) continue;
            counts.merge(key, stack.getAmount(), Integer::sum);
        }
        return counts;
    }

    /** A width/height + row-major ingredient-key layout, with {@code null} standing for an empty cell. */
    public record Shape(int width, int height, String[] keys) {

        /** The minimal bounding box (inclusive) enclosing every non-null cell of a raw key layout. */
        public record Bounds(int minRow, int maxRow, int minCol, int maxCol) {}

        /**
         * Finds the minimal bounding box of non-null cells in a raw {@code width}x{@code height} key
         * layout, or {@code null} if every cell is empty. Shared by {@link #trim} and
         * {@link CraftingMatrix#trimAmounts()} so both a shape and its parallel per-cell amounts are
         * trimmed to the exact same box.
         */
        public static @Nullable Bounds boundingBox(int width, int height, String[] keys) {
            int minRow = height, maxRow = -1, minCol = width, maxCol = -1;

            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    if (keys[row * width + col] == null) continue;
                    minRow = Math.min(minRow, row);
                    maxRow = Math.max(maxRow, row);
                    minCol = Math.min(minCol, col);
                    maxCol = Math.max(maxCol, col);
                }
            }

            return maxRow < 0 ? null : new Bounds(minRow, maxRow, minCol, maxCol);
        }

        /**
         * Trims a raw {@code width}x{@code height} key layout down to the minimal bounding box of
         * non-null cells, so two layouts of the same shape placed at different offsets compare equal.
         * Returns {@code null} if every cell is empty. Shared by {@link CraftingMatrix#trim()} (a
         * runtime grid) and {@link ShapedCraftingRecipe} (a recipe's own declared pattern), so both
         * sides of a match are normalized identically.
         */
        public static @Nullable Shape trim(int width, int height, String[] keys) {
            Bounds bounds = boundingBox(width, height, keys);
            if (bounds == null) return null;

            int trimmedWidth = bounds.maxCol() - bounds.minCol() + 1;
            int trimmedHeight = bounds.maxRow() - bounds.minRow() + 1;
            String[] trimmed = new String[trimmedWidth * trimmedHeight];
            for (int row = 0; row < trimmedHeight; row++) {
                for (int col = 0; col < trimmedWidth; col++) {
                    trimmed[row * trimmedWidth + col] = keys[(bounds.minRow() + row) * width + (bounds.minCol() + col)];
                }
            }

            return new Shape(trimmedWidth, trimmedHeight, trimmed);
        }

        /** Trie path: dimensions first (so differently-shaped grids never collide), then each cell. */
        public List<String> toPath() {
            List<String> path = new ArrayList<>(keys.length + 2);
            path.add(String.valueOf(width));
            path.add(String.valueOf(height));
            for (String key : keys) {
                path.add(key == null ? EMPTY_SEGMENT : key);
            }
            return path;
        }

        /** Flips each row left-to-right - vanilla-style shaped recipes also match their mirror image. */
        public Shape mirrored() {
            String[] flipped = new String[keys.length];
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    flipped[row * width + (width - 1 - col)] = keys[row * width + col];
                }
            }
            return new Shape(width, height, flipped);
        }
    }
}
