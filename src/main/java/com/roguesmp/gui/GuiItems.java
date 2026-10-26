package com.roguesmp.gui;

import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class GuiItems {

    private GuiItems() {
    }

    public static ItemStack item(ItemStack base, Component name, List<Component> lore) {
        base.setData(DataComponentTypes.ITEM_NAME, name);
        if (!lore.isEmpty()) base.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return base;
    }

    public static ItemStack item(ItemStack base, String name, TextColor color, Component... lore) {
        return item(base, Utils.text(name, color), List.of(lore));
    }

    public static ItemStack item(Material material, String name, TextColor color, Component... lore) {
        return item(ItemStack.of(material), name, color, lore);
    }

    public static Component line(String text) {
        return Utils.text(text, NamedTextColor.GRAY);
    }

    public static Component hint(String text) {
        return Utils.text(text, NamedTextColor.YELLOW);
    }
}
