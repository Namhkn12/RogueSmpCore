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
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class CraftingManager {
    private static CraftingManager INSTANCE;

    private final Trie<CraftingRecipe> trie = new Trie<>();
    private final Map<String, List<CraftingRecipe>> byOutput = new HashMap<>();
    private final Map<String, List<CraftingRecipe>> byInput = new HashMap<>();

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

        for (CraftingRecipe recipe : Registries.CRAFTING_RECIPE.getAll().values()) {
            for (RecipeInput input : recipe.getIndexInputs()) {
                insert(recipe, input.toIndexPath());
            }
            index(byOutput, recipe.getOutputs(), recipe);
            index(byInput, recipe.getInputs(), recipe);
        }
    }

    private static void index(Map<String, List<CraftingRecipe>> target, List<CraftingIngredient> ingredients, CraftingRecipe recipe) {
        for (CraftingIngredient ingredient : ingredients) {
            List<CraftingRecipe> recipes = target.computeIfAbsent(ingredient.key(), key -> new ArrayList<>());
            if (!recipes.contains(recipe)) recipes.add(recipe);
        }
    }

    public @NotNull @Unmodifiable Set<String> getCraftableItemKeys() {
        return Set.copyOf(byOutput.keySet());
    }

    public @NotNull @Unmodifiable List<CraftingRecipe> getRecipesProducing(@NotNull String itemKey) {
        return List.copyOf(byOutput.getOrDefault(itemKey, List.of()));
    }

    public @NotNull @Unmodifiable List<CraftingRecipe> getRecipesUsing(@NotNull String itemKey) {
        return List.copyOf(byInput.getOrDefault(itemKey, List.of()));
    }

    private void insert(CraftingRecipe recipe, List<String> path) {
        List<String> prefixed = withType(recipe.getTypeId(), path);
        warnIfColliding(prefixed, recipe);
        trie.insert(prefixed, recipe);
    }

    private void warnIfColliding(List<String> path, CraftingRecipe recipe) {
        for (CraftingRecipe existing : trie.find(path)) {
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

    @SuppressWarnings("unchecked")
    public @Nullable <T extends CraftingRecipe> T match(@NotNull RecipeKey<T> key, @NotNull ItemStack[] items, int width, int height) {
        RecipeInput queryInput = toQueryInput(items, width, height);
        if (queryInput == null) return null;

        List<String> path = withType(key.id(), queryInput.toIndexPath());

        // The trie only narrows by shape/ingredient identity; amounts are checked by matches().
        for (CraftingRecipe candidate : trie.find(path)) {
            if (candidate.matches(items, width, height)) return (T) candidate;
        }
        return null;
    }

    private static @Nullable RecipeInput toQueryInput(ItemStack[] items, int width, int height) {
        if (width > 0 && height > 0 && items.length != width * height) {
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

    @SuppressWarnings("unchecked")
    public @NotNull <T extends CraftingRecipe> List<T> getAll(@NotNull RecipeKey<T> key) {
        LinkedHashSet<CraftingRecipe> unique = new LinkedHashSet<>(trie.collect(List.of(key.id())));
        List<T> result = new ArrayList<>(unique.size());
        for (CraftingRecipe recipe : unique) result.add((T) recipe);
        return List.copyOf(result);
    }
}
