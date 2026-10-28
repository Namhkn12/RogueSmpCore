package com.roguesmp.crafting.recipe;

import com.roguesmp.codec.Codec;
import com.roguesmp.codec.DataResult;
import com.roguesmp.codec.MapCodec;
import com.roguesmp.crafting.*;
import com.roguesmp.registry.Registries;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class CraftingRecipe {
    public static final Codec<CraftingRecipe> CODEC = Codec.dispatch(CraftingRecipe::getTypeId, Registries.CRAFTING_RECIPE_CODEC::getOrThrow);

    public record BaseProperties(String id, List<CraftingIngredient> outputs, boolean quickCraft) {
        public BaseProperties {
            outputs = List.copyOf(outputs);
            if (outputs.isEmpty()) throw new IllegalArgumentException("Recipe '" + id + "' declares no outputs");
        }

        public BaseProperties(String id, CraftingIngredient output, boolean quickCraft) {
            this(id, List.of(output), quickCraft);
        }
    }

    private record OutputFields(Optional<CraftingIngredient> result, Optional<List<CraftingIngredient>> results) {}

    private static final MapCodec<List<CraftingIngredient>> OUTPUTS_CODEC = Codec.composite(
            CraftingIngredient.CODEC.optionalFieldOf("result").forGetter(OutputFields::result),
            Codec.listOf(CraftingIngredient.CODEC).optionalFieldOf("results").forGetter(OutputFields::results),
            OutputFields::new
    ).comapFlatMap(CraftingRecipe::toOutputs, outputs -> outputs.size() == 1
            ? new OutputFields(Optional.of(outputs.getFirst()), Optional.empty())
            : new OutputFields(Optional.empty(), Optional.of(outputs)));

    private static DataResult<List<CraftingIngredient>> toOutputs(OutputFields fields) {
        if (fields.result().isPresent() && fields.results().isPresent()) return DataResult.error("Declare either 'result' or 'results', not both");
        if (fields.result().isPresent()) return DataResult.success(List.of(fields.result().get()));
        if (fields.results().isPresent() && !fields.results().get().isEmpty()) return DataResult.success(fields.results().get());
        return DataResult.error("Missing 'result' or 'results'");
    }

    public static final MapCodec<BaseProperties> BASE_CODEC = Codec.composite(
            Codec.STRING.fieldOf("id").forGetter(BaseProperties::id),
            OUTPUTS_CODEC.forGetter(BaseProperties::outputs),
            Codec.BOOLEAN.optionalFieldOf("quick_craft", true).forGetter(BaseProperties::quickCraft),
            BaseProperties::new
    );

    private final BaseProperties base;
    private @Nullable Map<String, Integer> inputTotals;

    protected CraftingRecipe(@NotNull BaseProperties base) {
        this.base = base;
    }

    protected CraftingRecipe(@NotNull BaseProperties base, int requiredOutputs) {
        this(base);
        if (base.outputs().size() != requiredOutputs)
            throw new IllegalArgumentException("Recipe '" + base.id() + "' of type '" + getTypeId() + "' needs exactly " + requiredOutputs + " output(s), got " + base.outputs().size());
    }

    public abstract @NotNull String getTypeId();

    public abstract @NotNull String getTypeName();

    public abstract @NotNull List<RecipeInput> getIndexInputs();

    public abstract @NotNull @Unmodifiable List<CraftingIngredient> getInputs();

    public abstract boolean matches(@NotNull ItemStack @NotNull [] items, int width, int height);

    public abstract @NotNull ItemStack[] consume(@NotNull ItemStack @NotNull [] items, int width, int height);

    public @NotNull BaseProperties getBaseProperties() {
        return base;
    }

    public @NotNull String getId() {
        return base.id();
    }

    public boolean isQuickCraftable() {
        return base.quickCraft();
    }

    public @NotNull @Unmodifiable List<CraftingIngredient> getOutputs() {
        return base.outputs();
    }

    public @NotNull CraftingIngredient getPrimaryOutput() {
        return base.outputs().getFirst();
    }

    public @Nullable ItemStack getPrimaryOutputStack() {
        return getPrimaryOutput().toItemStack();
    }

    public @NotNull @Unmodifiable Map<String, Integer> getInputTotals() {
        if (inputTotals == null) {
            Map<String, Integer> totals = new LinkedHashMap<>();
            for (CraftingIngredient ingredient : getInputs()) {
                if (ingredient != null) totals.merge(ingredient.key(), ingredient.count(), Integer::sum);
            }
            inputTotals = Collections.unmodifiableMap(totals);
        }
        return inputTotals;
    }

    public @NotNull List<ItemStack> getOutputStacks() {
        List<ItemStack> stacks = new ArrayList<>(base.outputs().size());
        for (CraftingIngredient output : base.outputs()) {
            ItemStack stack = output.toItemStack();
            if (stack != null) stacks.add(stack);
        }
        return stacks;
    }

    protected @NotNull ItemStack[] applyConsumption(@NotNull ItemStack @NotNull [] stacks, int @NotNull [] amountsToConsume) {
        ItemStack[] result = new ItemStack[amountsToConsume.length];

        for (int i = 0; i < amountsToConsume.length; i++) {
            int consumeAmount = amountsToConsume[i];
            if (consumeAmount <= 0) continue;

            ItemStack stack = stacks[i];

            int leftover = stack.getAmount() - consumeAmount;
            if (leftover <= 0) continue;

            ItemStack copy = stack.clone();
            copy.setAmount(leftover);
            result[i] = copy;
        }

        return result;
    }
}
