package com.roguesmp.crafting.gui.category;

import com.roguesmp.crafting.CraftingIngredient;
import org.bukkit.inventory.ItemStack;

public interface RecipeLayout {

    int ROWS = 4;
    int COLUMNS = 9;

    enum Role {
        INPUT, OUTPUT
    }

    interface Slot {

        Slot display(ItemStack item);

        Slot lore(String... lines);
    }

    Slot slot(int row, int col, Role role, CraftingIngredient ingredient);

    void decoration(int row, int col, ItemStack item);
}
