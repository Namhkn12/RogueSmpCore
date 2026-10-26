package com.roguesmp.crafting.gui;

import com.roguesmp.crafting.quick.QuickCraftCandidate;
import com.roguesmp.utils.ItemRef;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class QuickCraftDisplay {

    private QuickCraftDisplay() {}

    public static ItemStack render(QuickCraftCandidate candidate) {
        ItemStack display = candidate.recipe().getPrimaryOutputStack();
        if (display == null) display = ItemStack.of(Material.BARRIER);

        ItemLore existing = display.getData(DataComponentTypes.LORE);
        List<Component> lore = existing == null ? new ArrayList<>() : new ArrayList<>(existing.lines());
        lore.add(Utils.text("------------------", NamedTextColor.DARK_GRAY));
        lore.add(Utils.text("Nguyên liệu:", NamedTextColor.GRAY));
        for (QuickCraftCandidate.IngredientStatus ingredient : candidate.ingredients()) {
            lore.add(Component.text(" - ", NamedTextColor.DARK_GRAY)
                    .append(ItemRef.displayName(ingredient.key()).color(NamedTextColor.WHITE))
                    .append(Component.text(" " + ingredient.have() + "/" + ingredient.need(), NamedTextColor.GREEN))
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Utils.text("Click: chế tạo 1 lần", NamedTextColor.YELLOW));
        lore.add(Utils.text("Shift-click: chế tạo tối đa (" + candidate.maxCrafts() + " lần)", NamedTextColor.YELLOW));
        display.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return display;
    }
}
