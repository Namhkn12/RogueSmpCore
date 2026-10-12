package com.roguesmp.region.editor;

import com.roguesmp.constant.Keys;
import com.roguesmp.utils.Utils;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class RegionWand {

    private RegionWand() {
    }

    public static ItemStack create() {
        ItemStack item = ItemStack.of(Material.BLAZE_ROD);
        item.setData(DataComponentTypes.ITEM_NAME, Utils.text("Region Wand", NamedTextColor.GOLD));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(List.of(
                Utils.text("Right click a block: add a corner", NamedTextColor.GRAY),
                Utils.text("Left click a block: undo last corner", NamedTextColor.GRAY),
                Utils.text("Sneak + left click: clear selection", NamedTextColor.GRAY),
                Utils.text("Sneak + right click: open region browser", NamedTextColor.GRAY),
                Utils.text("2 corners = box, 3+ corners = polygon", NamedTextColor.DARK_GRAY)
        )));
        item.editPersistentDataContainer(pdc -> pdc.set(Keys.REGION_WAND, PersistentDataType.BYTE, (byte) 1));
        return item;
    }

    public static boolean isWand(@Nullable ItemStack item) {
        return item != null && !item.isEmpty() && item.getPersistentDataContainer().has(Keys.REGION_WAND);
    }

    public static boolean isHolding(Player player) {
        return isWand(player.getInventory().getItemInMainHand()) || isWand(player.getInventory().getItemInOffHand());
    }
}
