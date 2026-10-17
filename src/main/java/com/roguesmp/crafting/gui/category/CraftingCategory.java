package com.roguesmp.crafting.gui.category;

import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.recipe.CraftingRecipe;
import com.roguesmp.crafting.recipe.ShapedCraftingRecipe;
import com.roguesmp.crafting.recipe.ShapelessCraftingRecipe;
import org.bukkit.Material;

import java.util.List;
import java.util.Set;

final class CraftingCategory extends RecipeCategory<CraftingRecipe> {

    private static final int GRID_COL = 1;

    CraftingCategory() {
        super("crafting", "Chế tạo", Material.CRAFTING_TABLE, List.of("minecraft:crafting_table"), Set.of());
    }

    @Override
    public void layout(CraftingRecipe recipe, RecipeLayout layout) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) layout.decoration(row, GRID_COL + col, EMPTY_CELL);
        }

        if (recipe instanceof ShapedCraftingRecipe shaped) {
            List<String> pattern = shaped.getPattern();
            for (int row = 0; row < pattern.size() && row < 3; row++) {
                String line = pattern.get(row);
                for (int col = 0; col < line.length() && col < 3; col++) {
                    char symbol = line.charAt(col);
                    CraftingIngredient ingredient = symbol == ' ' ? null : shaped.getKeyMap().get(String.valueOf(symbol));
                    if (ingredient != null) layout.slot(row, GRID_COL + col, RecipeLayout.Role.INPUT, ingredient);
                }
            }
            layout.decoration(1, 5, info());
        } else if (recipe instanceof ShapelessCraftingRecipe shapeless) {
            List<CraftingIngredient> ingredients = shapeless.getIngredients();
            for (int i = 0; i < ingredients.size() && i < 9; i++) {
                layout.slot(i / 3, GRID_COL + i % 3, RecipeLayout.Role.INPUT, ingredients.get(i));
            }
            layout.decoration(1, 5, info("Đặt ở vị trí bất kỳ."));
        }

        layout.slot(1, 7, RecipeLayout.Role.OUTPUT, recipe.getPrimaryOutput());
    }
}
