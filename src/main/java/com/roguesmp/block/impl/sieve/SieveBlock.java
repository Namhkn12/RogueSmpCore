package com.roguesmp.block.impl.sieve;

import com.roguesmp.block.BlockProperties;
import com.roguesmp.block.data.ManualProcessorData;
import com.roguesmp.block.impl.ManualProcessorBlock;
import com.roguesmp.crafting.recipe.CraftingRecipes;
import com.roguesmp.crafting.recipe.SieveRecipe;
import org.bukkit.Sound;

public class SieveBlock extends ManualProcessorBlock<SieveRecipe> {

    private static final Style STYLE = new Style("Sàng", "Vật phẩm này không thể sàng", "Đã sàng xong",
            Sound.BLOCK_SAND_HIT, Sound.BLOCK_GRAVEL_BREAK);

    public SieveBlock(BlockProperties properties, ManualProcessorData data) {
        super(properties, data, CraftingRecipes.SIEVE, STYLE);
    }
}
