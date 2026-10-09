package com.roguesmp.region;

import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record RegionSelection(World world, List<BlockVector3> points, @Nullable Integer fixedMinY, @Nullable Integer fixedMaxY) {

    public static RegionSelection of(World world, BlockVector3 first) {
        return new RegionSelection(world, List.of(first), null, null);
    }

    public boolean isValid() {
        return points.size() >= 2;
    }

    public boolean isPolygon() {
        return points.size() >= 3;
    }

    public int minY() {
        return fixedMinY != null ? fixedMinY : points.stream().mapToInt(BlockVector3::y).min().orElse(world.getMinHeight());
    }

    public int maxY() {
        return fixedMaxY != null ? fixedMaxY : points.stream().mapToInt(BlockVector3::y).max().orElse(world.getMaxHeight() - 1);
    }

    public RegionSelection withPoint(BlockVector3 point) {
        List<BlockVector3> updated = new ArrayList<>(points);
        updated.add(point);
        return new RegionSelection(world, List.copyOf(updated), fixedMinY, fixedMaxY);
    }

    public @Nullable RegionSelection withoutLast() {
        if (points.size() <= 1) return null;
        return new RegionSelection(world, List.copyOf(points.subList(0, points.size() - 1)), fixedMinY, fixedMaxY);
    }

    public RegionSelection withFullHeight() {
        return new RegionSelection(world, points, world.getMinHeight(), world.getMaxHeight() - 1);
    }
}
