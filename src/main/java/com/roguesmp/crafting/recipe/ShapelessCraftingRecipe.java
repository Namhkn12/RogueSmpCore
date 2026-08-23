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

/**
 * A recipe matched by ingredient composition alone, regardless of position - a flat
 * {@code "ingredients"} list, where the same key may appear more than once. A duplicate key is a
 * <b>distinct</b> requirement, not summed into one total: two {@code "minecraft:redstone"} entries
 * with counts 4 and 6 require exactly two occupied slots holding at least 4 and at least 6
 * respectively (which slot satisfies which requirement doesn't matter - that's the "shapeless" part -
 * but one big stack of 10 does <i>not</i> satisfy both, and neither does a third occupied redstone
 * slot showing up alongside the two that do). Matches when, for every distinct key, the grid holds
 * <b>exactly</b> as many occupied slots of that key as there are declared requirements for it, each
 * assignable to a requirement it's large enough to cover (see {@link #matches}) - an extra slot of an
 * already-listed key is rejected the same as an entirely unlisted key. A slot's stack may still be
 * bigger than the requirement it's assigned to (the leftover amount just stays in that same slot
 * after crafting, see {@link #consume}). Ignores {@code width}/{@code height} entirely (see
 * {@link CraftingRecipe}'s class doc).
 */
public final class ShapelessCraftingRecipe extends CraftingRecipe {

    public static final String TYPE_KEY = "shapeless";

    public static final Codec<ShapelessCraftingRecipe> CODEC = Codec.composite(
            CraftingRecipe.BASE_CODEC.forGetter(CraftingRecipe::getBaseProperties),
            Codec.listOf(CraftingIngredient.CODEC).fieldOf("ingredients").forGetter(ShapelessCraftingRecipe::getIngredients),
            ShapelessCraftingRecipe::new
    );

    private final List<CraftingIngredient> ingredients;

    /** {@code ingredients} grouped by key (duplicates kept separate, not summed) - each key's own counts sorted descending, so matching/consuming can greedily pair the largest requirement with the largest occupied slot. */
    private final Map<String, List<Integer>> requiredCountsByKey;

    public ShapelessCraftingRecipe(@NotNull BaseProperties base, @NotNull List<CraftingIngredient> ingredients) {
        super(base);
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

    /** Identity only (one segment per distinct required key, sorted by {@link RecipeInput#toIndexPath()}) - counts are checked separately by {@link #matches}. */
    @Override
    public @NotNull List<RecipeInput> getIndexInputs() {
        return List.of(RecipeInput.unordered(ingredients));
    }

    @Override
    public @NotNull String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public boolean matches(@NotNull ItemStack @NotNull [] items, int width, int height) {
        Map<String, List<SlotAmount>> actual = groupSlotsByKey(items);
        if (actual.size() != requiredCountsByKey.size()) return false; // an unlisted item type is present, or one's missing entirely

        for (Map.Entry<String, List<Integer>> entry : requiredCountsByKey.entrySet()) {
            List<SlotAmount> haveSlots = actual.get(entry.getKey());
            List<Integer> required = entry.getValue();
            // Exactly as many occupied slots of this key as declared requirements - an extra slot
            // of an already-listed key is just as much a mismatch as a missing one.
            if (haveSlots == null || haveSlots.size() != required.size()) return false;

            // Both lists are sorted descending - pairing them index-for-index is a valid feasibility
            // check for "each requirement needs its own slot with at least that much" (an exchange
            // argument shows any other pairing that works implies this sorted one works too).
            for (int i = 0; i < required.size(); i++) {
                if (haveSlots.get(i).amount() < required.get(i)) return false;
            }
        }
        return true;
    }

    /**
     * Pairs each key's requirements with that key's occupied slots (same sorted pairing
     * {@link #matches} validated - one-to-one, since matching already requires equal counts),
     * consuming exactly the declared amount from each assigned slot - a slot's stack may end up only
     * partially consumed (leftover stays) if it's bigger than the requirement it's paired with.
     */
    @Override
    public @NotNull ItemStack[] consume(@NotNull ItemStack @NotNull [] items, int width, int height) {
        int[] amountsToConsume = new int[items.length];
        Map<String, List<SlotAmount>> grouped = groupSlotsByKey(items);

        for (Map.Entry<String, List<Integer>> entry : requiredCountsByKey.entrySet()) {
            List<SlotAmount> haveSlots = grouped.get(entry.getKey());
            if (haveSlots == null) continue; // shouldn't happen if matches() was checked first

            List<Integer> required = entry.getValue();
            for (int i = 0; i < required.size() && i < haveSlots.size(); i++) {
                amountsToConsume[haveSlots.get(i).index()] = required.get(i);
            }
        }

        return applyConsumption(items, amountsToConsume);
    }

    /** One occupied slot's index into {@code items} plus its stack amount - {@link #groupSlotsByKey} groups these by resolved key, sorted descending by amount, so the largest slot pairs with the largest requirement. */
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
