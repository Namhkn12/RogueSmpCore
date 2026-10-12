package com.roguesmp.region.editor;

import com.roguesmp.region.SmpRegionFlags;
import com.roguesmp.utils.Utils;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.RegionGroupFlag;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

final class RegionGuiItems {

    private RegionGuiItems() {
    }

    static ItemStack item(Material material, String name, TextColor color, Component... lore) {
        ItemStack item = ItemStack.of(material);
        item.setData(DataComponentTypes.ITEM_NAME, Utils.text(name, color));
        if (lore.length > 0) item.setData(DataComponentTypes.LORE, ItemLore.lore(Arrays.asList(lore)));
        return item;
    }

    static Component line(String text) {
        return Utils.text(text, NamedTextColor.GRAY);
    }

    static Component hint(String text) {
        return Utils.text(text, NamedTextColor.YELLOW);
    }

    static String describeShape(ProtectedRegion region) {
        String shape = region instanceof ProtectedPolygonalRegion polygon ? "Polygon (" + polygon.getPoints().size() + " corners)" : "Box";
        return shape + ", y " + region.getMinimumPoint().y() + " to " + region.getMaximumPoint().y();
    }

    static String describeRegion(List<ProtectedRegion> pieces) {
        return pieces.size() == 1 ? describeShape(pieces.getFirst()) : pieces.size() + " shapes";
    }

    static List<Map.Entry<Flag<?>, Object>> visibleFlags(ProtectedRegion region) {
        return region.getFlags().entrySet().stream()
                .filter(entry -> !(entry.getKey() instanceof RegionGroupFlag) && entry.getKey() != SmpRegionFlags.GROUPED)
                .sorted(Map.Entry.comparingByKey((a, b) -> a.getName().compareTo(b.getName())))
                .toList();
    }

    @SuppressWarnings("unchecked")
    static <V> String displayValue(Flag<V> flag, Object value) {
        return String.valueOf(flag.marshal((V) value));
    }
}
