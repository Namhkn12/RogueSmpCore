package com.roguesmp.crafting.recipe;

import com.roguesmp.codec.Codec;
import com.roguesmp.crafting.CraftingIngredient;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;

/**
 * A recipe that breaks a single {@code input} item down into one or more {@code outputs} - the
 * inverse of {@link FusionRecipe} (many ingredients -> one result): one item in, an array of
 * specified items out. Doesn't use the base {@link CraftingRecipe#getResult()} single-item slot
 * (left {@code null} - see {@link CraftingRecipe.BaseProperties}) since there's no one canonical
 * result here; {@link #getOutputs()} is the real output list.
 * <p>
 * Structure only for now - not yet registered into {@code CraftingRecipes}/{@code CraftingManager},
 * so it has no {@code matches(...)}/{@code consume(...)} or trie-indexing behavior yet.
 */
public final class ScrappingRecipe extends CraftingRecipe {

    public static final String TYPE_KEY = "scrapping";

    public static final Codec<ScrappingRecipe> CODEC = Codec.composite(
            CraftingRecipe.BASE_CODEC.forGetter(CraftingRecipe::getBaseProperties),
            CraftingIngredient.CODEC.fieldOf("input").forGetter(ScrappingRecipe::getInput),
            Codec.listOf(CraftingIngredient.CODEC).fieldOf("outputs").forGetter(ScrappingRecipe::getOutputs),
            ScrappingRecipe::new
    );

    private final CraftingIngredient input;
    private final List<CraftingIngredient> outputs;

    public ScrappingRecipe(@NotNull BaseProperties base, @NotNull CraftingIngredient input, @NotNull List<CraftingIngredient> outputs) {
        super(base);
        this.input = input;
        this.outputs = List.copyOf(outputs);
    }

    @Override
    public @NotNull String getTypeId() {
        return TYPE_KEY;
    }

    public @NotNull CraftingIngredient getInput() {
        return input;
    }

    public @NotNull @Unmodifiable List<CraftingIngredient> getOutputs() {
        return outputs;
    }

    /** Resolves every output to a real {@link ItemStack}, skipping any that fail to resolve (e.g. a stale/removed item id). */
    public @NotNull List<ItemStack> getOutputStacks() {
        List<ItemStack> stacks = new ArrayList<>(outputs.size());
        for (CraftingIngredient output : outputs) {
            ItemStack stack = output.toItemStack();
            if (stack != null) stacks.add(stack);
        }
        return stacks;
    }
}
