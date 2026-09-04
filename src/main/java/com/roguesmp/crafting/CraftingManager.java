package com.roguesmp.crafting;

import com.roguesmp.RogueSmpCore;
import com.roguesmp.crafting.recipe.CraftingRecipe;
import com.roguesmp.crafting.recipe.RecipeInput;
import com.roguesmp.crafting.recipe.RecipeKey;
import com.roguesmp.crafting.recipe.ShapedCraftingRecipe;
import com.roguesmp.registry.Registries;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Read-facing index over {@link Registries#CRAFTING_RECIPE}: a single {@link Trie} holding every
 * recipe kind, so matching an input against every registered recipe is a couple of path walks
 * instead of a linear scan.
 * <p>
 * Every operation here is generic over the recipe kind - {@link #rebuild()} calls
 * {@link CraftingRecipe#getIndexInputs()} polymorphically (no {@code instanceof} chain), and
 * {@link #match} takes the kind you want as a {@link RecipeKey} (e.g. {@code CraftingRecipes.FUSION},
 * mirroring {@code ItemComponentKeys}) rather than a {@code Class} token, then calls
 * {@link CraftingRecipe#matches}/{@link CraftingRecipe#consume} polymorphically too. A brand-new
 * recipe kind needs zero changes here - it just needs to exist, declare a {@link RecipeKey} in
 * {@code CraftingRecipes}, and implement the {@link CraftingRecipe} contract.
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
            for (RecipeInput input : recipe.getIndexInputs()) {
                insert(recipe, input.toIndexPath());
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
                RogueSmpCore.LOGGER.warn("Crafting recipes '{}' and '{}' share the same shape/ingredient-set index path - if an input " +
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

    /**
     * Matches {@code items} against every registered recipe of {@code key}'s kind - {@code width}/
     * {@code height} describe {@code items} the same way {@link CraftingRecipe#matches} does (0/0
     * if the kind you're querying doesn't care about position). The cast back to {@code T} is safe
     * the same way {@code BaseItem#getComponent(ComponentKey)}'s is: every recipe indexed under
     * {@code key.id()}'s trie prefix is one {@code CraftingRecipes} registered as that exact kind.
     */
    @SuppressWarnings("unchecked")
    public @Nullable <T extends CraftingRecipe> T match(@NotNull RecipeKey<T> key, @NotNull ItemStack[] items, int width, int height) {
        RecipeInput queryInput = toQueryInput(items, width, height);
        if (queryInput == null) return null; // an entirely empty positional grid can't match anything

        List<String> path = withType(key.id(), queryInput.toIndexPath());

        // The trie only narrows by identity (shape/ingredient-set) - candidates sharing that path
        // can still differ in amount requirements, so each is re-checked via matches().
        for (CraftingRecipe candidate : trie.find(path)) {
            if (candidate.matches(items, width, height)) return (T) candidate;
        }
        return null;
    }

    /**
     * Builds the query {@link RecipeInput} for a raw runtime grid - positional inputs are trimmed
     * to their bounding box first (see {@link RecipeInput#trim()}), so a shape placed anywhere in a
     * larger grid (e.g. this 1-wide pattern sitting in the middle column of a 3x3) still lines up
     * with the recipe's own (already-trimmed) declared path. {@code null} iff {@code items} is
     * positional and entirely empty.
     */
    private static @Nullable RecipeInput toQueryInput(ItemStack[] items, int width, int height) {
        if (width > 0 && height > 0 && items.length != width * height) {
            // Same invariant the old CraftingMatrix constructor used to enforce - fail clearly here
            // rather than let a mismatched array surface as a confusing IndexOutOfBoundsException.
            throw new IllegalArgumentException("Expected " + (width * height) + " items for a " + width + "x" + height + " grid, got " + items.length);
        }

        List<CraftingIngredient> slots = new ArrayList<>(items.length);
        for (ItemStack stack : items) {
            String key = CraftingIngredient.resolveKey(stack);
            slots.add(key == null ? null : new CraftingIngredient(key, 1));
        }
        if (width <= 0 || height <= 0) return RecipeInput.unordered(slots);
        return RecipeInput.positional(slots, width, height).trim();
    }

    /** Every registered recipe of {@code key}'s kind, de-duplicated (a shaped recipe indexed under a mirror path is only listed once). */
    @SuppressWarnings("unchecked")
    public @NotNull <T extends CraftingRecipe> List<T> getAll(@NotNull RecipeKey<T> key) {
        LinkedHashSet<CraftingRecipe> unique = new LinkedHashSet<>(trie.collect(List.of(key.id())));
        List<T> result = new ArrayList<>(unique.size());
        for (CraftingRecipe recipe : unique) result.add((T) recipe);
        return List.copyOf(result);
    }
}
