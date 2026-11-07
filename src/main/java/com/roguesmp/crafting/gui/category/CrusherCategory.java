package com.roguesmp.crafting.gui.category;

import com.roguesmp.block.BlockDrop;
import com.roguesmp.block.BlockTypes;
import com.roguesmp.crafting.CraftingIngredient;
import com.roguesmp.crafting.recipe.CrusherRecipe;
import com.roguesmp.utils.ItemRef;
import com.roguesmp.utils.ItemStackUtils;
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

final class CrusherCategory extends RecipeCategory<CrusherRecipe> {

    private static final int FIRST_ROW = 1;
    private static final int FIRST_OUTPUT_COL = 5;
    private static final int OUTPUTS_PER_ROW = 3;

    CrusherCategory() {
        super("crusher", "Máy nghiền", Material.GRINDSTONE, List.of(), Set.of(BlockTypes.CRUSHER, BlockTypes.HAND_CRUSHER));
    }

    @Override
    public void layout(CrusherRecipe recipe, RecipeLayout layout) {
        layout.slot(FIRST_ROW, 1, RecipeLayout.Role.INPUT, recipe.getInput());
        layout.decoration(FIRST_ROW, 3, infoDisplay(recipe.getDuration(), recipe));

        List<CraftingIngredient> outputs = recipe.getOutputs();
        int index = 0;
        for (CraftingIngredient output : outputs) {
            if (!fits(index)) return;
            layout.slot(row(index), col(index), RecipeLayout.Role.OUTPUT, output);
            index++;
        }
        for (BlockDrop drop : recipe.getExtra()) {
            if (!fits(index)) return;
            ItemStack display = extraDisplay(drop);
            if (display != null) layout.decoration(row(index), col(index), display);
            index++;
        }
    }

    private static boolean fits(int index) {
        return row(index) < RecipeLayout.ROWS;
    }

    private static int row(int index) {
        return FIRST_ROW + index / OUTPUTS_PER_ROW;
    }

    private static int col(int index) {
        return FIRST_OUTPUT_COL + index % OUTPUTS_PER_ROW;
    }

    private static ItemStack extraDisplay(BlockDrop drop) {
        ItemStack item = ItemRef.toItemStack(drop.item(), Math.max(1, drop.minAmount()));
        if (item == null) return null;

        List<Component> lore = new ArrayList<>(item.lore() != null ? item.lore() : List.of());
        String amount = drop.maxAmount() > drop.minAmount() ? drop.minAmount() + "-" + drop.maxAmount() : String.valueOf(drop.minAmount());
        lore.add(Component.empty());
        lore.add(Utils.text("Thành phẩm phụ: x" + amount, NamedTextColor.GOLD));
        lore.add(Utils.text("Tỉ lệ: " + Utils.formatDecimal(drop.chance() * 100) + "%", NamedTextColor.YELLOW));
        ItemStackUtils.setLore(item, lore);
        return item;
    }

    private static ItemStack infoDisplay(int duration, CrusherRecipe recipe) {
        ItemStack item = ItemStack.of(Material.GRINDSTONE);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Máy nghiền"));

        List<Component> list = new ArrayList<>();
        list.add(Utils.text("Thời gian: " + Utils.formatDecimal(duration / 20d) + "s", NamedTextColor.YELLOW));
        if (recipe.getMinTier() > 1) {
            list.add(Utils.text("Yêu cầu máy cấp: " + recipe.getMinTier(), NamedTextColor.GRAY));
        }
        item.setData(DataComponentTypes.LORE, ItemLore.lore(list));
        return item;
    }
}
