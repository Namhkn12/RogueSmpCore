package com.roguesmp.region.editor;

import com.roguesmp.region.RegionSelection;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.jetbrains.annotations.Nullable;

import java.util.List;

record RegionOutline(List<double[]> corners, double bottom, double top) {

    static @Nullable RegionOutline of(ProtectedRegion region) {
        double bottom = region.getMinimumPoint().y();
        double top = region.getMaximumPoint().y() + 1;

        if (region instanceof ProtectedPolygonalRegion polygon) {
            return new RegionOutline(polygon.getPoints().stream().map(RegionOutline::center).toList(), bottom, top);
        }
        if (region instanceof ProtectedCuboidRegion) {
            return new RegionOutline(rectangle(region.getMinimumPoint(), region.getMaximumPoint()), bottom, top);
        }
        return null;
    }

    static RegionOutline of(RegionSelection selection) {
        List<BlockVector3> points = selection.points();
        double bottom = selection.minY();
        double top = selection.maxY() + 1;

        if (selection.isPolygon()) {
            return new RegionOutline(points.stream().map(point -> new double[]{point.x() + 0.5, point.z() + 0.5}).toList(), bottom, top);
        }

        BlockVector3 first = points.getFirst();
        BlockVector3 second = points.size() > 1 ? points.get(1) : first;
        BlockVector3 min = BlockVector3.at(Math.min(first.x(), second.x()), 0, Math.min(first.z(), second.z()));
        BlockVector3 max = BlockVector3.at(Math.max(first.x(), second.x()), 0, Math.max(first.z(), second.z()));
        return new RegionOutline(rectangle(min, max), bottom, top);
    }

    double minX() {
        return corners.stream().mapToDouble(corner -> corner[0]).min().orElse(0);
    }

    double maxX() {
        return corners.stream().mapToDouble(corner -> corner[0]).max().orElse(0);
    }

    double minZ() {
        return corners.stream().mapToDouble(corner -> corner[1]).min().orElse(0);
    }

    double maxZ() {
        return corners.stream().mapToDouble(corner -> corner[1]).max().orElse(0);
    }

    private static double[] center(BlockVector2 point) {
        return new double[]{point.x() + 0.5, point.z() + 0.5};
    }

    private static List<double[]> rectangle(BlockVector3 min, BlockVector3 max) {
        return List.of(
                new double[]{min.x(), min.z()},
                new double[]{max.x() + 1, min.z()},
                new double[]{max.x() + 1, max.z() + 1},
                new double[]{min.x(), max.z() + 1}
        );
    }
}
