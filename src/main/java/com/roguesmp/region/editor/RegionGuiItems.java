package com.roguesmp.region.editor;

import com.roguesmp.region.SmpRegionFlags;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.RegionGroupFlag;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import java.util.List;
import java.util.Map;

final class RegionGuiItems {

    private RegionGuiItems() {
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
