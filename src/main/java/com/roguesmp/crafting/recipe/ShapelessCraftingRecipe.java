package com.roguesmp.crafting.recipe;

import com.roguesmp.codec.Codec;
import com.roguesmp.crafting.CraftingIngredient;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// Duplicate keys are separate slot requirements, not summed: 4 + 6 redstone needs two slots, one stack of 10 does not match.
public final class ShapelessCraftingRecipe extends CraftingRecipe {
    public static final String TYPE_KEY = "shapeless";

    public static final Codec<ShapelessCraftingRecipe> CODEC = Codec.composite(
            CraftingRecipe.BASE_CODEC.forGetter(CraftingRecipe::getBaseProperties),
            Codec.listOf(CraftingIngredient.CODEC).fieldOf("ingredients").forGetter(ShapelessCraftingRecipe::getIngredients),
            ShapelessCraftingRecipe::new
    );

    private final List<CraftingIngredient> ingredients;

    private final Map<String, List<Integer>> requiredCountsByKey;

    public ShapelessCraftingRecipe(@NotNull BaseProperties base, @NotNull List<CraftingIngredient> ingredients) {
        super(base, 1);
        this.ingredients = List.copyOf(ingredients);

        Map<String, List<Integer>> grouped = new TreeMap<>();
        for (CraftingIngredient ingredient : ingredients) {
            grouped.computeIfAbsent(ingredient.key(), key -> new ArrayList<>()).add(ingredient.count());
        }
        for (List<Integer> counts : grouped.values()) {
            counts.sort(Comparator.reverseOrder());
        }
        this.requiredCountsByKey = Collections.unmodifiableMap(grouped);
    }

    @Override
    public @NotNull List<RecipeInput> getIndexInputs() {
        return List.of(RecipeInput.unordered(ingredients));
    }

    @Override
    public @NotNull String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public @NotNull @Unmodifiable List<CraftingIngredient> getInputs() {
        return ingredients;
    }

    @Override
    public boolean matches(@NotNull ItemStack @NotNull [] items, int width, int height) {
        Map<String, List<SlotAmount>> actual = groupSlotsByKey(items);
        if (actual.size() != requiredCountsByKey.size()) return false;

        for (Map.Entry<String, List<Integer>> entry : requiredCountsByKey.entrySet()) {
            List<SlotAmount> haveSlots = actual.get(entry.getKey());
            List<Integer> required = entry.getValue();
            if (haveSlots == null || haveSlots.size() != required.size()) return false;

            // Both lists are sorted descending, so index-wise pairing is a valid feasibility check.
            for (int i = 0; i < required.size(); i++) {
                if (haveSlots.get(i).amount() < required.get(i)) return false;
            }
        }
        return true;
    }

    @Override
    public @NotNull ItemStack[] consume(@NotNull ItemStack @NotNull [] items, int width, int height) {
        int[] amountsToConsume = new int[items.length];
        Map<String, List<SlotAmount>> grouped = groupSlotsByKey(items);

        for (Map.Entry<String, List<Integer>> entry : requiredCountsByKey.entrySet()) {
            List<SlotAmount> haveSlots = grouped.get(entry.getKey());
            if (haveSlots == null) continue;

            List<Integer> required = entry.getValue();
            for (int i = 0; i < required.size() && i < haveSlots.size(); i++) {
                amountsToConsume[haveSlots.get(i).index()] = required.get(i);
            }
        }

        return applyConsumption(items, amountsToConsume);
    }

    private record SlotAmount(int index, int amount) {}

    private static @NotNull Map<String, List<SlotAmount>> groupSlotsByKey(@NotNull ItemStack[] items) {
        Map<String, List<SlotAmount>> grouped = new TreeMap<>();
        for (int i = 0; i < items.length; i++) {
            String key = CraftingIngredient.resolveKey(items[i]);
            if (key == null) continue;
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(new SlotAmount(i, items[i].getAmount()));
        }
        for (List<SlotAmount> slots : grouped.values()) {
            slots.sort(Comparator.comparingInt(SlotAmount::amount).reversed());
        }
        return grouped;
    }

    public @NotNull @Unmodifiable List<CraftingIngredient> getIngredients() {
        return ingredients;
    }

    public @NotNull @Unmodifiable Map<String, List<Integer>> getRequiredCountsByKey() {
        return requiredCountsByKey;
    }
}
