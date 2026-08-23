package com.roguesmp.crafting.recipe;

import com.roguesmp.codec.Codec;
import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.input.CraftingMatrix;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A recipe matched by exact layout in the grid - a {@code "pattern"} of up to 3 rows (each up to
 * 3 characters wide) plus a {@code "key"} map from pattern character to {@link CraftingIngredient}
 * (id + how many must be stacked in that cell, defaulting to 1). Matches regardless of where in the
 * grid the shape is placed (both sides trim to their minimal bounding box before comparing - see
 * {@link CraftingMatrix#trim()}), and by default also matches its horizontal mirror image, same as
 * a vanilla shaped recipe.
 */
public final class ShapedCraftingRecipe extends CraftingRecipe {

    public static final String TYPE_KEY = "shaped";

    public static final Codec<ShapedCraftingRecipe> CODEC = Codec.composite(
            CraftingRecipe.BASE_CODEC.forGetter(CraftingRecipe::getBaseProperties),
            Codec.listOf(Codec.STRING).fieldOf("pattern").forGetter(ShapedCraftingRecipe::getPattern),
            Codec.unboundedMap(CraftingIngredient.CODEC).fieldOf("key").forGetter(ShapedCraftingRecipe::getKeyMap),
            Codec.BOOLEAN.optionalFieldOf("mirrored", true).forGetter(ShapedCraftingRecipe::isMirrored),
            ShapedCraftingRecipe::new
    );

    private final List<String> pattern;
    private final Map<String, CraftingIngredient> keyMap;
    private final boolean mirrored;

    /** Trimmed to its minimal bounding box at construction time - identity only, no amounts. */
    private final CraftingMatrix.Shape shape;

    /** Required stack size per cell of {@link #shape}, same row-major order, 0 for an empty cell. */
    private final int[] requiredAmounts;

    public ShapedCraftingRecipe(@NotNull BaseProperties base, @NotNull List<String> pattern, @NotNull Map<String, CraftingIngredient> keyMap, boolean mirrored) {
        super(base);
        this.pattern = List.copyOf(pattern);
        this.keyMap = Map.copyOf(keyMap);
        this.mirrored = mirrored;

        ShapeData built = buildShape(this.pattern, this.keyMap);
        this.shape = built.shape();
        this.requiredAmounts = built.amounts();
    }

    private record ShapeData(CraftingMatrix.Shape shape, int[] amounts) {}

    private static ShapeData buildShape(List<String> pattern, Map<String, CraftingIngredient> keyMap) {
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

        CraftingMatrix.Shape.Bounds bounds = CraftingMatrix.Shape.boundingBox(width, height, keys);
        if (bounds == null) return new ShapeData(new CraftingMatrix.Shape(0, 0, new String[0]), new int[0]);

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

        return new ShapeData(new CraftingMatrix.Shape(trimmedWidth, trimmedHeight, trimmedKeys), trimmedAmounts);
    }

    /**
     * Every trie path this recipe should be indexed under: its own canonical shape, plus its
     * horizontal mirror when {@link #isMirrored()} is true and the mirror isn't identical to the
     * original (a symmetric pattern would otherwise double-insert the same path). Identity only -
     * per-cell amount requirements are checked separately by {@link #matches}, since two recipes
     * can legitimately share a shape/ingredient path while differing only in amounts.
     */
    public @NotNull List<List<String>> getIndexPaths() {
        List<String> canonical = shape.toPath();
        List<List<String>> paths = new ArrayList<>(2);
        paths.add(canonical);

        if (mirrored) {
            List<String> flipped = shape.mirrored().toPath();
            if (!flipped.equals(canonical)) paths.add(flipped);
        }

        return paths;
    }

    @Override
    public @NotNull String getTypeId() {
        return TYPE_KEY;
    }

    /** Whether this recipe's ingredients are satisfied by the given grid. */
    public boolean matches(@NotNull CraftingMatrix matrix) {
        CraftingMatrix.Shape trimmed = matrix.trim();
        if (trimmed == null) return false;

        int[] actualAmounts = matrix.trimAmounts();
        List<String> path = trimmed.toPath();

        if (path.equals(shape.toPath()) && hasEnoughAmounts(actualAmounts, requiredAmounts)) return true;

        return mirrored
                && path.equals(shape.mirrored().toPath())
                && hasEnoughAmounts(actualAmounts, mirrorAmounts(requiredAmounts, shape.width(), shape.height()));
    }

    /**
     * Resolves what each cell of {@code matrix} should become after consuming exactly one craft.
     * Undefined (may consume nothing) if {@code matrix} doesn't actually {@link #matches} - check
     * that first.
     */
    public @NotNull ItemStack[] consume(@NotNull CraftingMatrix matrix) {
        int[] amountsToConsume = new int[matrix.width() * matrix.height()];

        CraftingMatrix.Shape trimmed = matrix.trim();
        if (trimmed == null) return applyConsumption(matrix.cellsView(), amountsToConsume);

        List<String> path = trimmed.toPath();
        int[] cellAmounts;
        if (path.equals(shape.toPath())) {
            cellAmounts = requiredAmounts;
        } else if (mirrored && path.equals(shape.mirrored().toPath())) {
            cellAmounts = mirrorAmounts(requiredAmounts, shape.width(), shape.height());
        } else {
            return applyConsumption(matrix.cellsView(), amountsToConsume); // matrix doesn't actually match this recipe
        }

        CraftingMatrix.Shape.Bounds bounds = matrix.trimBounds();
        for (int row = 0; row < trimmed.height(); row++) {
            for (int col = 0; col < trimmed.width(); col++) {
                int required = cellAmounts[row * trimmed.width() + col];
                if (required <= 0) continue;

                int matrixIndex = (bounds.minRow() + row) * matrix.width() + (bounds.minCol() + col);
                amountsToConsume[matrixIndex] = required;
            }
        }

        return applyConsumption(matrix.cellsView(), amountsToConsume);
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

    public @NotNull CraftingMatrix.Shape getShape() {
        return shape;
    }
}
