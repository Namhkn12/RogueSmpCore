package com.roguesmp.crafting.recipe;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.crafting.*;
import com.roguesmp.registry.Registries;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Base type for a custom crafting recipe. Concrete kinds ({@link ShapedCraftingRecipe},
 * {@link ShapelessCraftingRecipe}, {@link FusionRecipe}) register into
 * {@link Registries#CRAFTING_RECIPE_CODEC} (see {@link CraftingRecipes}), dispatched by the
 * {@code "type"} field - same polymorphic pattern as {@code SmpEffect}/{@code LootEntry} elsewhere
 * in the plugin (see {@code com.roguesmp.codec.INFO.md} section 7).
 * <p>
 * Ingredients and results are referenced by {@link CraftingIngredient} keys rather than raw
 * {@link ItemStack}s, so a recipe can point at either a {@code BaseItem} id or a vanilla material
 * without caring which.
 * <p>
 * {@link #matches}/{@link #consume} are real shared abstract methods here - every kind, no matter
 * how positional or how its slots are laid out in a GUI, is happy to accept the single most general
 * shape: a flat {@code ItemStack[] items} plus an optional {@code width}/{@code height} (0 when
 * position doesn't matter to that kind). {@link ShapedCraftingRecipe} is the only kind that reads
 * {@code width}/{@code height} at all (for trim/mirror); {@link FusionRecipe}/{@code ScrappingRecipe}
 * ignore them and treat {@code items[0]} as their distinguished slot by convention (see each kind's
 * own doc). Nothing about matching itself is generic - each kind still implements its own rule -
 * only the *signature* is shared, which is what lets {@link CraftingManager} dispatch by a plain
 * type-id string instead of by concrete class (see {@link CraftingManager#match}).
 */
public abstract class CraftingRecipe {

    public static final Codec<CraftingRecipe> CODEC = Codec.dispatch(CraftingRecipe::getTypeId, Registries.CRAFTING_RECIPE_CODEC::getOrThrow);

    /**
     * Fields shared by every recipe kind. {@code result} is the single canonical output most
     * kinds have (a shaped/shapeless/fusion recipe's one result item) - it's {@code null} for a
     * kind with no single result of its own (e.g. {@link ScrappingRecipe}, whose real output is
     * its own {@code outputs} list instead).
     */
    public record BaseProperties(String id, @Nullable CraftingIngredient result) {}

    public static final MapCodec<BaseProperties> BASE_CODEC = Codec.composite(
            Codec.STRING.fieldOf("id").forGetter(BaseProperties::id),
            CraftingIngredient.CODEC.optionalFieldOf("result").forGetter(props -> Optional.ofNullable(props.result())),
            (id, result) -> new BaseProperties(id, result.orElse(null))
    );

    private final BaseProperties base;

    protected CraftingRecipe(@NotNull BaseProperties base) {
        this.base = base;
    }

    /** JSON {@code "type"} dispatch key - {@code "shaped"}/{@code "shapeless"}/{@code "fusion"} for the built-in kinds. */
    public abstract @NotNull String getTypeId();

    /**
     * Every {@link RecipeInput} this recipe should be indexed under (more than one only for a kind
     * like {@link ShapedCraftingRecipe} that can also match a variant, e.g. its mirror image) -
     * {@code CraftingManager} calls {@link RecipeInput#toIndexPath()} on each of these to build the
     * trie path itself, so no concrete kind needs its own path-building method.
     */
    public abstract @NotNull List<RecipeInput> getIndexInputs();

    /**
     * Whether this recipe's ingredients are satisfied by {@code items} (row-major, {@code width}x
     * {@code height} if {@code width}/{@code height} are meaningful to this kind, 0/0 otherwise).
     */
    public abstract boolean matches(@NotNull ItemStack @NotNull [] items, int width, int height);

    /**
     * Resolves what each index of {@code items} should become after consuming exactly one craft -
     * same shape as {@link #matches}. Undefined (may consume nothing) if {@code items} doesn't
     * actually {@link #matches} - check that first. A pure data query - doesn't mutate
     * {@code items}; the caller writes the result back into a real inventory.
     */
    public abstract @NotNull ItemStack[] consume(@NotNull ItemStack @NotNull [] items, int width, int height);

    public @NotNull BaseProperties getBaseProperties() {
        return base;
    }

    public @NotNull String getId() {
        return base.id();
    }

    public @Nullable CraftingIngredient getResult() {
        return base.result();
    }

    public @Nullable ItemStack getResultStack() {
        return base.result() == null ? null : base.result().toItemStack();
    }

    /** No-remainder shorthand for every kind but {@link ShapedCraftingRecipe} - see the 3-arg overload below. */
    protected @NotNull ItemStack[] applyConsumption(@NotNull ItemStack @NotNull [] stacks, int @NotNull [] amountsToConsume) {
        return applyConsumption(stacks, amountsToConsume, Map.of());
    }

    /**
     * Shared by every recipe kind's own {@link #consume}: given the input stacks a craft draws
     * from ({@code stacks}), how much of each this craft uses up ({@code amountsToConsume}, same
     * indices as {@code stacks}, {@code 0} for a stack not involved at all), and an ingredient key ->
     * remainder key map (e.g. {@code "minecraft:water_bucket" -> "minecraft:bucket"} - only
     * {@link ShapedCraftingRecipe} declares one of its own; every other kind uses the empty-map
     * overload above), resolves each index to its leftover stack (amount reduced, same item), a
     * resolved remainder item if that stack was fully drained and its ingredient key has one
     * declared, or {@code null} if fully drained with no remainder. A pure data query - doesn't
     * mutate {@code stacks} or anything else; the caller writes the result back into a real
     * inventory.
     */
    protected @NotNull ItemStack[] applyConsumption(@NotNull ItemStack @NotNull [] stacks, int @NotNull [] amountsToConsume, @NotNull Map<String, String> remainders) {
        ItemStack[] result = new ItemStack[amountsToConsume.length];

        for (int i = 0; i < amountsToConsume.length; i++) {
            int consumeAmount = amountsToConsume[i];
            if (consumeAmount <= 0) continue;

            ItemStack stack = stacks[i];
            if (stack == null) continue;

            int leftover = stack.getAmount() - consumeAmount;
            if (leftover > 0) {
                ItemStack copy = stack.clone();
                copy.setAmount(leftover);
                result[i] = copy;
                continue;
            }

            String key = CraftingIngredient.resolveKey(stack);
            String remainderKey = key == null ? null : remainders.get(key);
            result[i] = remainderKey == null ? null : CraftingIngredient.resolveItemStack(remainderKey, 1);
        }

        return result;
    }
}
