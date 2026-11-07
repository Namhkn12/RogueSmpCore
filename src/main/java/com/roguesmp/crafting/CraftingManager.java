package com.roguesmp.crafting;

import com.roguesmp.utils.ItemRef;
import com.roguesmp.RogueSmpCore;
import com.roguesmp.crafting.recipe.CraftingRecipe;
import com.roguesmp.crafting.recipe.Recipe;
import com.roguesmp.crafting.recipe.RecipeInput;
import com.roguesmp.crafting.recipe.RecipeKey;
import com.roguesmp.crafting.recipe.ShapedCraftingRecipe;
import com.roguesmp.registry.Registries;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Predicate;

public class CraftingManager {
    private static CraftingManager INSTANCE;

    private final Trie<Recipe> trie = new Trie<>();
    private final Map<String, List<Recipe>> byOutput = new HashMap<>();
    private final Map<String, List<Recipe>> byInput = new HashMap<>();

    private CraftingManager() {
        rebuild();
    }

    public static void init() {
        INSTANCE = new CraftingManager();
    }

    public static CraftingManager getInstance() {
        return INSTANCE;
    }

    public void rebuild() {
        trie.clear();
        byOutput.clear();
        byInput.clear();

        for (Recipe recipe : Registries.CRAFTING_RECIPE.getAll().values()) {
            for (RecipeInput input : recipe.getIndexInputs()) {
                insert(recipe, input.toIndexPath());
            }
            index(byOutput, recipe.getOutputs(), recipe);
            index(byInput, recipe.getInputs(), recipe);
        }
    }

    private static void index(Map<String, List<Recipe>> target, List<CraftingIngredient> ingredients, Recipe recipe) {
        for (CraftingIngredient ingredient : ingredients) {
            List<Recipe> recipes = target.computeIfAbsent(ingredient.key(), key -> new ArrayList<>());
            if (!recipes.contains(recipe)) recipes.add(recipe);
        }
    }

    public @NotNull @Unmodifiable Set<String> getCraftableItemKeys() {
        return Set.copyOf(byOutput.keySet());
    }

    public @NotNull @Unmodifiable List<Recipe> getRecipesProducing(@NotNull String itemKey) {
        return Collections.unmodifiableList(byOutput.getOrDefault(itemKey, List.of()));
    }

    public @NotNull @Unmodifiable List<Recipe> getRecipesUsing(@NotNull String itemKey) {
        return Collections.unmodifiableList(byInput.getOrDefault(itemKey, List.of()));
    }

    private void insert(Recipe recipe, List<String> path) {
        List<String> prefixed = withType(recipe.getTypeId(), path);
        warnIfColliding(prefixed, recipe);
        trie.insert(prefixed, recipe);
    }

    private void warnIfColliding(List<String> path, Recipe recipe) {
        for (Recipe existing : trie.find(path)) {
            if (existing != recipe) {
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

    public @Nullable <T extends CraftingRecipe> T match(@NotNull RecipeKey<T> key, @NotNull ItemStack[] items, int width, int height) {
        return match(key, items, width, height, recipe -> recipe.matches(items, width, height));
    }

    public @Nullable <T extends Recipe> T match(@NotNull RecipeKey<T> key, @NotNull ItemStack[] items, @NotNull Predicate<? super T> matcher) {
        return match(key, items, 0, 0, matcher);
    }

    @SuppressWarnings("unchecked")
    private @Nullable <T extends Recipe> T match(RecipeKey<T> key, ItemStack[] items, int width, int height, Predicate<? super T> matcher) {
        RecipeInput queryInput = toQueryInput(items, width, height);
        if (queryInput == null) return null;

        List<String> path = withType(key.id(), queryInput.toIndexPath());

        // The trie only narrows by shape/ingredient identity; amounts are checked by the matcher.
        for (Recipe candidate : trie.find(path)) {
            if (matcher.test((T) candidate)) return (T) candidate;
        }
        return null;
    }

    private static @Nullable RecipeInput toQueryInput(ItemStack[] items, int width, int height) {
        if (width > 0 && height > 0 && items.length != width * height) {
            throw new IllegalArgumentException("Expected " + (width * height) + " items for a " + width + "x" + height + " grid, got " + items.length);
        }

        List<CraftingIngredient> slots = new ArrayList<>(items.length);
        for (ItemStack stack : items) {
            String key = ItemRef.idOf(stack);
            slots.add(key == null ? null : new CraftingIngredient(key, 1));
        }
        if (width <= 0 || height <= 0) return RecipeInput.unordered(slots);
        return RecipeInput.positional(slots, width, height).trim();
    }

    @SuppressWarnings("unchecked")
    public @NotNull <T extends Recipe> List<T> getAll(@NotNull RecipeKey<T> key) {
        LinkedHashSet<Recipe> unique = new LinkedHashSet<>(trie.collect(List.of(key.id())));
        List<T> result = new ArrayList<>(unique.size());
        for (Recipe recipe : unique) result.add((T) recipe);
        return List.copyOf(result);
    }
}
