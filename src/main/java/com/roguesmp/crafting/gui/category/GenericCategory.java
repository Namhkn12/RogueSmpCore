package com.roguesmp.crafting.gui.category;

import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.recipe.CraftingRecipe;
import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class GenericCategory extends RecipeCategory<CraftingRecipe> {

    private static final int INPUTS_PER_ROW = 4;
    private static final int FIRST_OUTPUT_COL = 6;
    private static final int OUTPUTS_PER_ROW = 3;

    GenericCategory(String typeId, String displayName) {
        super(typeId, displayName, Material.PAPER, List.of(), Set.of());
    }

    @Override
    public void layout(CraftingRecipe recipe, RecipeLayout layout) {
        Map<String, Integer> merged = new LinkedHashMap<>();
        for (CraftingIngredient input : recipe.getInputs()) merged.merge(input.key(), input.count(), Integer::sum);

        int index = 0;
        for (Map.Entry<String, Integer> entry : merged.entrySet()) {
            layout.slot(index / INPUTS_PER_ROW, index % INPUTS_PER_ROW, RecipeLayout.Role.INPUT, new CraftingIngredient(entry.getKey(), entry.getValue()));
            index++;
        }

        layout.decoration(1, 5, info("Đã gộp theo loại vật phẩm."));

        List<CraftingIngredient> outputs = recipe.getOutputs();
        for (int i = 0; i < outputs.size(); i++) {
            layout.slot(i / OUTPUTS_PER_ROW, FIRST_OUTPUT_COL + i % OUTPUTS_PER_ROW, RecipeLayout.Role.OUTPUT, outputs.get(i));
        }
    }
}
