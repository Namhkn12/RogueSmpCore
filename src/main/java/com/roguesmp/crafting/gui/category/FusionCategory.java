package com.roguesmp.crafting.gui.category;

import com.roguesmp.block.BlockTypes;
import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.recipe.FusionRecipe;
import org.bukkit.Material;

import java.util.List;
import java.util.Set;

final class FusionCategory extends RecipeCategory<FusionRecipe> {

    private static final int[][] RING = {{0, 1}, {0, 2}, {0, 3}, {1, 3}, {2, 3}, {2, 2}, {2, 1}, {1, 1}};

    FusionCategory() {
        super("fusion", "Dung hợp", Material.ENCHANTING_TABLE, List.of(), Set.of(BlockTypes.ALTAR_MAIN));
    }

    @Override
    public void layout(FusionRecipe recipe, RecipeLayout layout) {
        layout.slot(1, 2, RecipeLayout.Role.INPUT, recipe.getInput());

        List<CraftingIngredient> ingredients = recipe.getIngredients();
        for (int i = 0; i < ingredients.size(); i++) {
            if (i < RING.length) layout.slot(RING[i][0], RING[i][1], RecipeLayout.Role.INPUT, ingredients.get(i));
            else if (i < RING.length + 3) layout.slot(i - RING.length, 0, RecipeLayout.Role.INPUT, ingredients.get(i));
        }

        layout.decoration(1, 5, info("Vật phẩm chính ở giữa.", "Các ô xung quanh là vật phẩm quanh altar"));
        layout.slot(1, 7, RecipeLayout.Role.OUTPUT, recipe.getPrimaryOutput());
    }
}
