package com.roguesmp.crafting.recipe;

import com.roguesmp.codec.Codec;
import com.roguesmp.crafting.CraftingIngredient;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A recipe matched by exact layout in the grid - a {@code "pattern"} of up to 3 rows (each up to
 * 3 characters wide) plus a {@code "key"} map from pattern character to {@link CraftingIngredient}
 * (id + how many must be stacked in that cell, defaulting to 1). Matches regardless of where in the
 * grid the shape is placed (both sides trim to their minimal bounding box before comparing), and by
 * default also matches its horizontal mirror image, same as a vanilla shaped recipe.
 * <p>
 * The only kind that reads {@code width}/{@code height} in {@link #matches}/{@link #consume} - see
 * {@link CraftingRecipe}'s class doc for why every kind shares that signature regardless.
 */
public final class ShapedCraftingRecipe extends CraftingRecipe {

    public static final String TYPE_KEY = "shaped";

    public static final Codec<ShapedCraftingRecipe> CODEC = Codec.composite(
            CraftingRecipe.BASE_CODEC.forGetter(CraftingRecipe::getBaseProperties),
            Codec.listOf(Codec.STRING).fieldOf("pattern").forGetter(ShapedCraftingRecipe::getPattern),
            Codec.unboundedMap(CraftingIngredient.CODEC).fieldOf("key").forGetter(ShapedCraftingRecipe::getKeyMap),
            Codec.BOOLEAN.optionalFieldOf("mirrored", true).forGetter(ShapedCraftingRecipe::isMirrored),
            Codec.unboundedMap(Codec.STRING).optionalFieldOf("remainders", Map.of()).forGetter(ShapedCraftingRecipe::getRemainders),
            ShapedCraftingRecipe::new
    );

    private final List<String> pattern;
    private final Map<String, CraftingIngredient> keyMap;
    private final boolean mirrored;

    /** Ingredient key -> remainder key (e.g. {@code "minecraft:water_bucket" -> "minecraft:bucket"}) - see {@link CraftingRecipe#applyConsumption}. */
    private final Map<String, String> remainders;

    /** Trimmed to its minimal bounding box at construction time - identity only, no amounts. */
    private final Grid shape;

    /** Required stack size per cell of {@link #shape}, same row-major order, 0 for an empty cell. */
    private final int[] requiredAmounts;

    public ShapedCraftingRecipe(@NotNull BaseProperties base, @NotNull List<String> pattern, @NotNull Map<String, CraftingIngredient> keyMap, boolean mirrored, @NotNull Map<String, String> remainders) {
        super(base);
        this.pattern = List.copyOf(pattern);
        this.keyMap = Map.copyOf(keyMap);
        this.mirrored = mirrored;
        this.remainders = Map.copyOf(remainders);

        GridData built = buildShape(this.pattern, this.keyMap);
        this.shape = built.grid();
        this.requiredAmounts = built.amounts();
    }

    // ==========================================
    // GRID - a private 2D key layout, the recipe's own equivalent of a trimmed runtime grid.
    // Not shared with any other kind (only ShapedCraftingRecipe cares about position at all).
    // ==========================================

    private record Grid(int width, int height, String[] keys) {

        record Bounds(int minRow, int maxRow, int minCol, int maxCol) {}

        static @Nullable Bounds boundingBox(int width, int height, String[] keys) {
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
         * True structural equality - a plain record-derived {@code equals()} would only compare
         * {@link #keys} by reference (records don't deep-compare array components), so this exists
         * to actually compare shape/ingredient identity. Dimensions checked first so differently
         * shaped grids never compare equal even if {@link Arrays#equals} would accept a coincidence.
         */
        boolean sameShapeAs(Grid other) {
            return width == other.width && height == other.height && Arrays.equals(keys, other.keys);
        }

        Grid mirrored() {
            String[] flipped = new String[keys.length];
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    flipped[row * width + (width - 1 - col)] = keys[row * width + col];
                }
            }
            return new Grid(width, height, flipped);
        }

        RecipeInput toRecipeInput() {
            List<CraftingIngredient> slots = new ArrayList<>(keys.length);
            for (String key : keys) slots.add(key == null ? null : new CraftingIngredient(key, 1));
            return RecipeInput.positional(slots, width, height);
        }
    }

    private record GridData(Grid grid, int[] amounts) {}

    private static GridData buildShape(List<String> pattern, Map<String, CraftingIngredient> keyMap) {
        int height = pattern.size();
        int width = height == 0 ? 0 : pattern.get(0).length();

        String[] keys = new String[width * height];
        int[] amounts = new int[width * height];
        for (int row = 0; row < height; row++) {
            String line = pattern.get(row);
            for (int col = 0; col < width; col++) {
                char symbol = col < line.length() ? line.charAt(col) : ' ';
                CraftingIngredient ingredient = symbol == ' ' ? null : keyMap.get(String.valueOf(symbol));
                int index = row * width + col;
                keys[index] = ingredient == null ? null : ingredient.key();
                amounts[index] = ingredient == null ? 0 : ingredient.count();
            }
        }

        Grid.Bounds bounds = Grid.boundingBox(width, height, keys);
        if (bounds == null) return new GridData(new Grid(0, 0, new String[0]), new int[0]);

        int trimmedWidth = bounds.maxCol() - bounds.minCol() + 1;
        int trimmedHeight = bounds.maxRow() - bounds.minRow() + 1;
        String[] trimmedKeys = new String[trimmedWidth * trimmedHeight];
        int[] trimmedAmounts = new int[trimmedWidth * trimmedHeight];
        for (int row = 0; row < trimmedHeight; row++) {
            for (int col = 0; col < trimmedWidth; col++) {
                int srcIndex = (bounds.minRow() + row) * width + (bounds.minCol() + col);
                int dstIndex = row * trimmedWidth + col;
                trimmedKeys[dstIndex] = keys[srcIndex];
                trimmedAmounts[dstIndex] = amounts[srcIndex];
            }
        }

        return new GridData(new Grid(trimmedWidth, trimmedHeight, trimmedKeys), trimmedAmounts);
    }

    // ==========================================

    @Override
    public @NotNull String getTypeId() {
        return TYPE_KEY;
    }

    /**
     * This recipe's own canonical shape, plus its horizontal mirror when {@link #isMirrored()} is
     * true and the mirror isn't identical to the original (a symmetric pattern would otherwise
     * double-insert the same path). Identity only - per-cell amount requirements are checked
     * separately by {@link #matches}, since two recipes can legitimately share a shape/ingredient
     * path while differing only in amounts.
     */
    @Override
    public @NotNull List<RecipeInput> getIndexInputs() {
        List<RecipeInput> inputs = new ArrayList<>(2);
        inputs.add(shape.toRecipeInput());

        if (mirrored) {
            Grid mirroredGrid = shape.mirrored();
            if (!mirroredGrid.sameShapeAs(shape)) inputs.add(mirroredGrid.toRecipeInput());
        }

        return inputs;
    }

    @Override
    public boolean matches(@NotNull ItemStack @NotNull [] items, int width, int height) {
        requireGridSize(items, width, height);

        TrimmedQuery query = trimQuery(items, width, height);
        if (query == null) return false;

        if (query.grid().sameShapeAs(shape) && hasEnoughAmounts(query.amounts(), requiredAmounts)) return true;

        return mirrored
                && query.grid().sameShapeAs(shape.mirrored())
                && hasEnoughAmounts(query.amounts(), mirrorAmounts(requiredAmounts, shape.width(), shape.height()));
    }

    /**
     * Resolves what each cell of {@code items} should become after consuming exactly one craft.
     * Undefined (may consume nothing) if {@code items} doesn't actually {@link #matches} - check
     * that first.
     */
    @Override
    public @NotNull ItemStack[] consume(@NotNull ItemStack @NotNull [] items, int width, int height) {
        requireGridSize(items, width, height);

        int[] amountsToConsume = new int[items.length];

        TrimmedQuery query = trimQuery(items, width, height);
        if (query == null) return applyConsumption(items, amountsToConsume, remainders);

        int[] cellAmounts;
        if (query.grid().sameShapeAs(shape)) {
            cellAmounts = requiredAmounts;
        } else if (mirrored && query.grid().sameShapeAs(shape.mirrored())) {
            cellAmounts = mirrorAmounts(requiredAmounts, shape.width(), shape.height());
        } else {
            return applyConsumption(items, amountsToConsume, remainders); // items don't actually match this recipe
        }

        Grid trimmed = query.grid();
        Grid.Bounds bounds = query.bounds();
        for (int row = 0; row < trimmed.height(); row++) {
            for (int col = 0; col < trimmed.width(); col++) {
                int required = cellAmounts[row * trimmed.width() + col];
                if (required <= 0) continue;

                int itemIndex = (bounds.minRow() + row) * width + (bounds.minCol() + col);
                amountsToConsume[itemIndex] = required;
            }
        }

        return applyConsumption(items, amountsToConsume, remainders);
    }

    /** Restores the invariant the old {@code CraftingMatrix} constructor used to enforce - a clear error instead of an {@link ArrayIndexOutOfBoundsException} deep in the grid math below. */
    private static void requireGridSize(ItemStack[] items, int width, int height) {
        if (items.length != width * height) {
            throw new IllegalArgumentException("Expected " + (width * height) + " items for a " + width + "x" + height + " grid, got " + items.length);
        }
    }

    private record TrimmedQuery(Grid grid, int[] amounts, Grid.Bounds bounds) {}

    /**
     * Resolves keys and finds the bounding box exactly once, building the trimmed key layout and
     * the trimmed per-cell amounts together in the same pass (rather than {@code matches}/
     * {@code consume} each re-resolving/re-trimming from scratch, or doing so twice internally).
     * {@code null} iff {@code items} is entirely empty.
     */
    private static @Nullable TrimmedQuery trimQuery(ItemStack[] items, int width, int height) {
        String[] keys = new String[items.length];
        for (int i = 0; i < items.length; i++) keys[i] = CraftingIngredient.resolveKey(items[i]);

        Grid.Bounds bounds = Grid.boundingBox(width, height, keys);
        if (bounds == null) return null;

        int trimmedWidth = bounds.maxCol() - bounds.minCol() + 1;
        int trimmedHeight = bounds.maxRow() - bounds.minRow() + 1;
        String[] trimmedKeys = new String[trimmedWidth * trimmedHeight];
        int[] trimmedAmounts = new int[trimmedWidth * trimmedHeight];
        for (int row = 0; row < trimmedHeight; row++) {
            for (int col = 0; col < trimmedWidth; col++) {
                int srcIndex = (bounds.minRow() + row) * width + (bounds.minCol() + col);
                int dstIndex = row * trimmedWidth + col;
                trimmedKeys[dstIndex] = keys[srcIndex];
                ItemStack stack = items[srcIndex];
                trimmedAmounts[dstIndex] = stack == null ? 0 : stack.getAmount();
            }
        }

        return new TrimmedQuery(new Grid(trimmedWidth, trimmedHeight, trimmedKeys), trimmedAmounts, bounds);
    }

    private static boolean hasEnoughAmounts(int[] actual, int[] required) {
        if (actual.length != required.length) return false;
        for (int i = 0; i < actual.length; i++) {
            if (actual[i] < required[i]) return false;
        }
        return true;
    }

    private static int[] mirrorAmounts(int[] amounts, int width, int height) {
        int[] flipped = new int[amounts.length];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                flipped[row * width + (width - 1 - col)] = amounts[row * width + col];
            }
        }
        return flipped;
    }

    public @NotNull @Unmodifiable List<String> getPattern() {
        return pattern;
    }

    public @NotNull @Unmodifiable Map<String, CraftingIngredient> getKeyMap() {
        return keyMap;
    }

    public boolean isMirrored() {
        return mirrored;
    }

    public @NotNull @Unmodifiable Map<String, String> getRemainders() {
        return remainders;
    }
}
