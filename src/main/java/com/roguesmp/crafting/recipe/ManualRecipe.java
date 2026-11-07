package com.roguesmp.crafting.recipe;

import com.roguesmp.block.StoredItem;
import com.roguesmp.crafting.CraftingIngredient;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ManualRecipe {

    @NotNull CraftingIngredient getInput();

    int getDuration();

    int getMinTier();

    @NotNull List<StoredItem> roll();
}
