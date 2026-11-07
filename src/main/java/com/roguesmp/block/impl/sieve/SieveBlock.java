package com.roguesmp.block.impl.sieve;

import com.roguesmp.block.BlockProperties;
import com.roguesmp.block.StoredItem;
import com.roguesmp.block.data.ManualProcessorData;
import com.roguesmp.block.impl.ManualProcessorBlock;
import com.roguesmp.crafting.recipe.CraftingRecipes;
import com.roguesmp.crafting.recipe.SieveRecipe;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SieveBlock extends ManualProcessorBlock<SieveRecipe> {

    private static final Style STYLE = new Style("Sàng", "Vật phẩm này không thể sàng", "Đã sàng xong",
            Sound.BLOCK_SAND_HIT, Sound.BLOCK_GRAVEL_BREAK);

    public SieveBlock(BlockProperties properties, ManualProcessorData data) {
        super(properties, data, CraftingRecipes.SIEVE, STYLE);
    }

    @Override
    protected boolean accepts(SieveRecipe recipe, ItemStack stack) {
        return recipe.getMinTier() <= tier() && recipe.matches(stack);
    }

    @Override
    protected int inputCount(SieveRecipe recipe) {
        return recipe.getInput().count();
    }

    @Override
    protected int duration(SieveRecipe recipe) {
        return recipe.getDuration();
    }

    @Override
    protected List<StoredItem> roll(SieveRecipe recipe) {
        return recipe.roll();
    }
}
