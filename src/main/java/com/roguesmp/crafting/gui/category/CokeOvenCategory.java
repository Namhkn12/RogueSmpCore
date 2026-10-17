package com.roguesmp.crafting.gui.category;

import com.roguesmp.block.BlockTypes;
import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.recipe.CokeOvenRecipe;
import com.roguesmp.gui.BaseGui;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

final class CokeOvenCategory extends RecipeCategory<CokeOvenRecipe> {

    private static final int FIRST_ROW = 1;
    private static final int[] INPUT_COLS = {1, 2};
    private static final int PROGRESS_COL = 4;
    private static final int[] OUTPUT_COLS = {6, 7};
    private static final ItemStack INPUT_CELL = BaseGui.createDecoration(Material.ORANGE_STAINED_GLASS_PANE);
    private static final ItemStack OUTPUT_CELL = BaseGui.createDecoration(Material.LIME_STAINED_GLASS_PANE);

    CokeOvenCategory() {
        super("coke_oven", "Lò luyện cốc", Material.BLAST_FURNACE, List.of(), Set.of(BlockTypes.COKE_OVEN));
    }

    @Override
    public void layout(CokeOvenRecipe recipe, RecipeLayout layout) {
        List<CraftingIngredient> inputs = recipe.getIngredients();
        List<CraftingIngredient> outputs = recipe.getOutputs();
        int rows = Math.max((inputs.size() + 1) / INPUT_COLS.length, (outputs.size() + 1) / OUTPUT_COLS.length);
        rows = Math.clamp(rows, 1, RecipeLayout.ROWS - FIRST_ROW);

        for (int row = 0; row < rows; row++) {
            for (int i = 0; i < INPUT_COLS.length; i++) {
                int index = row * INPUT_COLS.length + i;
                if (index < inputs.size()) layout.slot(FIRST_ROW + row, INPUT_COLS[i], RecipeLayout.Role.INPUT, inputs.get(index));
                else layout.decoration(FIRST_ROW + row, INPUT_COLS[i], INPUT_CELL);
            }
            for (int i = 0; i < OUTPUT_COLS.length; i++) {
                int index = row * OUTPUT_COLS.length + i;
                if (index < outputs.size()) layout.slot(FIRST_ROW + row, OUTPUT_COLS[i], RecipeLayout.Role.OUTPUT, outputs.get(index));
                else layout.decoration(FIRST_ROW + row, OUTPUT_COLS[i], OUTPUT_CELL);
            }
        }

        layout.decoration(FIRST_ROW + (rows - 1) / 2, PROGRESS_COL, progress(Material.BLAZE_POWDER, recipe.getDuration()));
    }
}
