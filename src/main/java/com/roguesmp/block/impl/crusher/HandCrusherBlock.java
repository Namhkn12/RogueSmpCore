package com.roguesmp.block.impl.crusher;

import com.roguesmp.block.BlockProperties;
import com.roguesmp.block.data.ManualProcessorData;
import com.roguesmp.block.impl.ManualProcessorBlock;
import com.roguesmp.crafting.recipe.CraftingRecipes;
import com.roguesmp.crafting.recipe.CrusherRecipe;
import org.bukkit.Sound;

public class HandCrusherBlock extends ManualProcessorBlock<CrusherRecipe> {

    private static final Style STYLE = new Style("Nghiền", "Vật phẩm này không thể nghiền", "Đã nghiền xong",
            Sound.BLOCK_GRINDSTONE_USE, Sound.BLOCK_STONE_BREAK);

    public HandCrusherBlock(BlockProperties properties, ManualProcessorData data) {
        super(properties, data, CraftingRecipes.CRUSHER, STYLE);
    }
}
