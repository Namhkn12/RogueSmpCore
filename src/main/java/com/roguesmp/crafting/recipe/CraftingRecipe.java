package com.roguesmp.crafting.recipe;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.crafting.*;
import com.roguesmp.crafting.input.CraftingMatrix;
import com.roguesmp.crafting.input.FusionMatrix;
import com.roguesmp.registry.Registries;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

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
 * Deliberately doesn't declare a shared {@code matches(...)}/{@code consume(...)} contract here -
 * each kind is matched against a different shape of input ({@link ShapedCraftingRecipe}/
 * {@link ShapelessCraftingRecipe} against a {@link CraftingMatrix} grid, {@link FusionRecipe}
 * against a {@link FusionMatrix} input-plus-ingredients slot layout), so those live as ordinary
 * (non-override) methods on each concrete kind instead of forcing every future kind through one
 * grid-shaped signature. {@link CraftingManager} exposes typed
 * {@code match*}/{@code consume} entry points per kind rather than one polymorphic call.
 */
public abstract class CraftingRecipe {

    public static final Codec<CraftingRecipe> CODEC = Codec.dispatch(CraftingRecipe::getTypeId, Registries.CRAFTING_RECIPE_CODEC::getOrThrow);

    /**
     * Fields shared by every recipe kind. {@code result} is the single canonical output most
     * kinds have (a shaped/shapeless/fusion recipe's one result item) - it's {@code null} for a
     * kind with no single result of its own (e.g. {@link ScrappingRecipe}, whose real output is
     * its own {@code outputs} list instead).
     */
    public record BaseProperties(String id, @Nullable CraftingIngredient result, Map<String, String> remainders) {}

    public static final MapCodec<BaseProperties> BASE_CODEC = Codec.composite(
            Codec.STRING.fieldOf("id").forGetter(BaseProperties::id),
            CraftingIngredient.CODEC.optionalFieldOf("result").forGetter(props -> Optional.ofNullable(props.result())),
            Codec.unboundedMap(Codec.STRING).optionalFieldOf("remainders", Map.of()).forGetter(BaseProperties::remainders),
            (id, result, remainders) -> new BaseProperties(id, result.orElse(null), remainders)
    );

    private final BaseProperties base;

    protected CraftingRecipe(@NotNull BaseProperties base) {
        // Defensive copy - Codec.unboundedMap's decode returns a plain mutable HashMap.
        this.base = new BaseProperties(base.id(), base.result(), Map.copyOf(base.remainders()));
    }

    /** JSON {@code "type"} dispatch key - {@code "shaped"}/{@code "shapeless"}/{@code "fusion"} for the built-in kinds. */
    public abstract @NotNull String getTypeId();

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

    /** Ingredient key -> remainder key (e.g. {@code "minecraft:water_bucket" -> "minecraft:bucket"}). */
    public @NotNull @Unmodifiable Map<String, String> getRemainders() {
        return base.remainders();
    }

    /**
     * Shared by every recipe kind's own {@code consume(...)}: given the input stacks a craft draws
     * from ({@code stacks}) and how much of each this craft uses up ({@code amountsToConsume},
     * same indices as {@code stacks}, {@code 0} for a stack not involved at all), resolves each
     * index to its leftover stack (amount reduced, same item), a resolved {@link #getRemainders()}
     * item if that stack was fully drained and its ingredient key has one declared, or {@code null}
     * if fully drained with no remainder. A pure data query - doesn't mutate {@code stacks} or
     * anything else; the caller writes the result back into a real inventory.
     */
    protected @NotNull ItemStack[] applyConsumption(@NotNull ItemStack @NotNull [] stacks, int @NotNull [] amountsToConsume) {
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
            String remainderKey = key == null ? null : base.remainders().get(key);
            result[i] = remainderKey == null ? null : CraftingIngredient.resolveItemStack(remainderKey, 1);
        }

        return result;
    }
}
