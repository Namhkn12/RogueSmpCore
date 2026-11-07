package com.roguesmp.crafting.recipe;

import com.roguesmp.block.BlockDrop;
import com.roguesmp.block.StoredItem;
import com.roguesmp.codec.Codec;
import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.utils.ItemRef;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;

public final class SieveRecipe extends CraftingRecipe implements ManualRecipe {
    public static final String TYPE_KEY = "sieve";
    public static final String TYPE_NAME = "Sàng";

    public static final Codec<SieveRecipe> CODEC = Codec.composite(
            Codec.STRING.fieldOf("id").forGetter(CraftingRecipe::getId),
            CraftingIngredient.CODEC.fieldOf("input").forGetter(SieveRecipe::getInput),
            Codec.INT.fieldOf("duration").forGetter(SieveRecipe::getDuration),
            Codec.listOf(BlockDrop.CODEC).fieldOf("drops").forGetter(SieveRecipe::getDrops),
            Codec.INT.optionalFieldOf("min_tier", 1).forGetter(SieveRecipe::getMinTier),
            SieveRecipe::new
    );

    private final CraftingIngredient input;
    private final int duration;
    private final List<BlockDrop> drops;
    private final int minTier;

    public SieveRecipe(@NotNull String id, @NotNull CraftingIngredient input, int duration, @NotNull List<BlockDrop> drops, int minTier) {
        super(new BaseProperties(id, drops.stream().map(drop -> new CraftingIngredient(drop.item(), Math.max(1, drop.minAmount()))).toList(), false));
        this.input = input;
        this.duration = Math.max(1, duration);
        this.drops = List.copyOf(drops);
        this.minTier = Math.max(1, minTier);
    }

    @Override
    public @NotNull String getTypeId() {
        return TYPE_KEY;
    }

    @Override
    public @NotNull String getTypeName() {
        return TYPE_NAME;
    }

    @Override
    public @NotNull List<RecipeInput> getIndexInputs() {
        return List.of(RecipeInput.unordered(List.of(input)));
    }

    @Override
    public @NotNull @Unmodifiable List<CraftingIngredient> getInputs() {
        return List.of(input);
    }

    @Override
    public boolean matches(@NotNull ItemStack @NotNull [] items, int width, int height) {
        if (items.length == 0) return false;

        String key = ItemRef.idOf(items[0]);
        return key != null && key.equals(input.key()) && items[0].getAmount() >= input.count();
    }

    @Override
    public @NotNull ItemStack[] consume(@NotNull ItemStack @NotNull [] items, int width, int height) {
        int[] amountsToConsume = new int[items.length];
        if (items.length > 0) amountsToConsume[0] = input.count();
        return applyConsumption(items, amountsToConsume);
    }

    @Override
    public @NotNull List<StoredItem> roll() {
        List<StoredItem> results = new ArrayList<>();
        for (BlockDrop drop : drops) {
            StoredItem rolled = drop.rollStored();
            if (rolled != null) results.add(rolled);
        }
        return results;
    }

    @Override
    public @NotNull CraftingIngredient getInput() {
        return input;
    }

    @Override
    public int getDuration() {
        return duration;
    }

    public @NotNull @Unmodifiable List<BlockDrop> getDrops() {
        return drops;
    }

    @Override
    public int getMinTier() {
        return minTier;
    }
}
