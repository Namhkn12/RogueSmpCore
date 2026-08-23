package com.roguesmp.crafting;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.crafting.input.CraftingMatrix;
import com.roguesmp.crafting.input.FusionMatrix;
import com.roguesmp.crafting.recipe.CraftingRecipe;
import com.roguesmp.crafting.recipe.FusionRecipe;
import com.roguesmp.crafting.recipe.ShapedCraftingRecipe;
import com.roguesmp.crafting.recipe.ShapelessCraftingRecipe;
import com.roguesmp.registry.Registries;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Read-facing index over {@link Registries#CRAFTING_RECIPE}: a single {@link Trie} holding every
 * recipe kind, so matching an input against every registered recipe is a couple of path walks
 * instead of a linear scan. Each kind is matched by a fundamentally different equivalence rule
 * (see {@link ShapedCraftingRecipe}/{@link ShapelessCraftingRecipe}/{@link FusionRecipe}'s own
 * docs), so they still need their own path-building logic; what they share is the same underlying
 * trie, each kind's paths prefixed by its {@link CraftingRecipe#getTypeId()} so the different path
 * schemes can never collide with one another.
 * <p>
 * The initial build happens once at {@link #init()}; call {@link #rebuild()} again after reloading
 * recipes from disk (e.g. an admin reload command) to pick up the change.
 */
public class CraftingManager {

    private static CraftingManager INSTANCE;

    private final Trie<CraftingRecipe> trie = new Trie<>();

    private CraftingManager() {
        rebuild();
    }

    public static void init() {
        INSTANCE = new CraftingManager();
    }

    public static CraftingManager getInstance() {
        return INSTANCE;
    }

    /** Re-derives the trie from {@link Registries#CRAFTING_RECIPE}'s current entries. */
    public void rebuild() {
        trie.clear();

        for (CraftingRecipe recipe : Registries.CRAFTING_RECIPE.getAll().values()) {
            if (recipe instanceof ShapedCraftingRecipe shaped) {
                for (List<String> path : shaped.getIndexPaths()) {
                    insert(recipe, path);
                }
            } else if (recipe instanceof ShapelessCraftingRecipe shapeless) {
                insert(recipe, shapeless.getIndexPath());
            } else if (recipe instanceof FusionRecipe fusion) {
                insert(recipe, fusion.getIndexPath());
            }
        }
    }

    private void insert(CraftingRecipe recipe, List<String> path) {
        List<String> prefixed = withType(recipe.getTypeId(), path);
        warnIfColliding(prefixed, recipe);
        trie.insert(prefixed, recipe);
    }

    private void warnIfColliding(List<String> path, CraftingRecipe recipe) {
        for (CraftingRecipe existing : trie.find(path)) {
            if (!existing.getId().equals(recipe.getId())) {
                RogueSmpCore.LOGGER.warn("Crafting recipes '{}' and '{}' share the same shape/ingredient-set index path - if a grid " +
                                "satisfies both (e.g. two amount thresholds over the same ingredients), whichever is checked first wins",
                        existing.getId(), recipe.getId());
            }
        }
    }

    private static List<String> withType(String typeId, List<String> path) {
        List<String> prefixed = new ArrayList<>(path.size() + 1);
        prefixed.add(typeId);
        prefixed.addAll(path);
        return prefixed;
    }

    /** Matches a grid against every registered recipe - shaped recipes take priority over shapeless. */
    public @Nullable CraftingRecipe match(@NotNull CraftingMatrix matrix) {
        ShapedCraftingRecipe shaped = matchShaped(matrix);
        if (shaped != null) return shaped;
        return matchShapeless(matrix);
    }

    public @Nullable ShapedCraftingRecipe matchShaped(@NotNull CraftingMatrix matrix) {
        CraftingMatrix.Shape trimmed = matrix.trim();
        if (trimmed == null) return null;

        List<String> path = withType(ShapedCraftingRecipe.TYPE_KEY, trimmed.toPath());

        // The trie only narrows by shape/ingredient identity - candidates sharing that path can
        // still differ in per-cell amount requirements, so each is re-checked via matches().
        for (CraftingRecipe candidate : trie.find(path)) {
            if (candidate instanceof ShapedCraftingRecipe shaped && shaped.matches(matrix)) return shaped;
        }
        return null;
    }

    public @Nullable ShapelessCraftingRecipe matchShapeless(@NotNull CraftingMatrix matrix) {
        Map<String, Integer> counts = matrix.aggregateCounts();
        if (counts.isEmpty()) return null;

        List<String> path = withType(ShapelessCraftingRecipe.TYPE_KEY, ShapelessCraftingRecipe.toIndexPath(counts.keySet()));

        // The trie only narrows by ingredient-set identity - candidates sharing that path can still
        // differ in required amounts (a "least count" threshold, see ShapelessCraftingRecipe's doc),
        // so each is re-checked via matches().
        for (CraftingRecipe candidate : trie.find(path)) {
            if (candidate instanceof ShapelessCraftingRecipe shapeless && shapeless.matches(matrix)) return shapeless;
        }
        return null;
    }

    public @Nullable FusionRecipe matchFusion(@NotNull FusionMatrix matrix) {
        String inputKey = matrix.inputKey();
        if (inputKey == null) return null;

        List<String> path = withType(FusionRecipe.TYPE_KEY, FusionRecipe.toIndexPath(inputKey, matrix.aggregateIngredientCounts().keySet()));

        // The trie only narrows by input identity + ingredient-set identity - candidates sharing
        // that path can still differ in exact amounts, so each is re-checked via matches().
        for (CraftingRecipe candidate : trie.find(path)) {
            if (candidate instanceof FusionRecipe fusion && fusion.matches(matrix)) return fusion;
        }
        return null;
    }

    /**
     * Consumes {@code matrix} for a {@code recipe} previously returned by {@link #match}/
     * {@link #matchShaped}/{@link #matchShapeless} - a small dispatcher so callers holding a plain
     * {@link CraftingRecipe} reference don't need to know which grid-based kind it actually is.
     * {@link FusionRecipe} isn't grid-based (see {@link FusionMatrix}) so it isn't handled here -
     * call {@link FusionRecipe#consume(FusionMatrix)} directly once you have one from
     * {@link #matchFusion}.
     *
     * @throws IllegalArgumentException if {@code recipe} isn't a {@link ShapedCraftingRecipe} or {@link ShapelessCraftingRecipe}
     */
    public @NotNull ItemStack[] consume(@NotNull CraftingRecipe recipe, @NotNull CraftingMatrix matrix) {
        if (recipe instanceof ShapedCraftingRecipe shaped) return shaped.consume(matrix);
        if (recipe instanceof ShapelessCraftingRecipe shapeless) return shapeless.consume(matrix);
        throw new IllegalArgumentException("Not a grid-based recipe: '" + recipe.getId() + "' (" + recipe.getTypeId() + ")");
    }

    /**
     * Every registered shaped recipe, de-duplicated - a mirrored recipe is indexed under two trie
     * paths (see {@link ShapedCraftingRecipe#getIndexPaths()}) but should only be listed once here.
     */
    public @NotNull List<ShapedCraftingRecipe> getAllShapedRecipes() {
        return dedupCast(trie.collect(List.of(ShapedCraftingRecipe.TYPE_KEY)), ShapedCraftingRecipe.class);
    }

    public @NotNull List<ShapelessCraftingRecipe> getAllShapelessRecipes() {
        return dedupCast(trie.collect(List.of(ShapelessCraftingRecipe.TYPE_KEY)), ShapelessCraftingRecipe.class);
    }

    public @NotNull List<FusionRecipe> getAllFusionRecipes() {
        return dedupCast(trie.collect(List.of(FusionRecipe.TYPE_KEY)), FusionRecipe.class);
    }

    private static <T extends CraftingRecipe> List<T> dedupCast(List<CraftingRecipe> recipes, Class<T> type) {
        LinkedHashSet<T> unique = new LinkedHashSet<>();
        for (CraftingRecipe recipe : recipes) {
            unique.add(type.cast(recipe));
        }
        return List.copyOf(unique);
    }
}
