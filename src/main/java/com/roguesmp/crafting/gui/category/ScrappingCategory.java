package com.roguesmp.crafting.gui.category;

import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.recipe.ScrappingRecipe;
import org.bukkit.Material;

import java.util.List;
import java.util.Set;

final class ScrappingCategory extends RecipeCategory<ScrappingRecipe> {

    private static final int FIRST_ROW = 1;
    private static final int FIRST_OUTPUT_COL = 4;
    private static final int OUTPUTS_PER_ROW = 5;

    ScrappingCategory() {
        super("scrapping", "Phân rã", Material.GRINDSTONE, List.of(), Set.of());
    }

    @Override
    public void layout(ScrappingRecipe recipe, RecipeLayout layout) {
        layout.slot(FIRST_ROW, 1, RecipeLayout.Role.INPUT, recipe.getInput());
        layout.decoration(FIRST_ROW, 2, info("Số lượng phải chính xác."));

        List<CraftingIngredient> outputs = recipe.getOutputs();
        for (int i = 0; i < outputs.size(); i++) {
            layout.slot(FIRST_ROW + i / OUTPUTS_PER_ROW, FIRST_OUTPUT_COL + i % OUTPUTS_PER_ROW, RecipeLayout.Role.OUTPUT, outputs.get(i));
        }
    }
}
