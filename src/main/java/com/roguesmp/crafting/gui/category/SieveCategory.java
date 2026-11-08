package com.roguesmp.crafting.gui.category;

import com.roguesmp.block.BlockDrop;
import com.roguesmp.block.BlockTypes;
import com.roguesmp.crafting.recipe.SieveRecipe;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class SieveCategory extends RecipeCategory<SieveRecipe> {

    private static final int FIRST_ROW = 1;
    private static final int FIRST_OUTPUT_COL = 5;
    private static final int OUTPUTS_PER_ROW = 3;

    SieveCategory() {
        super("sieve", "Sàng", Material.SCAFFOLDING, List.of(), Set.of(BlockTypes.HAND_SIEVE, BlockTypes.SIEVE));
    }

    @Override
    public void layout(SieveRecipe recipe, RecipeLayout layout) {
        layout.slot(FIRST_ROW, 1, RecipeLayout.Role.INPUT, recipe.getInput());
        layout.decoration(FIRST_ROW, 3, infoDisplay(recipe));

        int index = 0;
        for (BlockDrop drop : recipe.getDrops()) {
            int row = FIRST_ROW + index / OUTPUTS_PER_ROW;
            if (row >= RecipeLayout.ROWS) return;
            ItemStack display = CrusherCategory.extraDisplay(drop);
            if (display != null) layout.decoration(row, FIRST_OUTPUT_COL + index % OUTPUTS_PER_ROW, display);
            index++;
        }
    }

    private static ItemStack infoDisplay(SieveRecipe recipe) {
        ItemStack item = ItemStack.of(Material.SCAFFOLDING);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Sàng"));

        List<Component> lore = new ArrayList<>();
        lore.add(Utils.text("Tiến độ cần: " + recipe.getDuration(), NamedTextColor.YELLOW));
        if (recipe.getMinTier() > 1) lore.add(Utils.text("Yêu cầu sàng cấp: " + recipe.getMinTier(), NamedTextColor.GRAY));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return item;
    }
}
