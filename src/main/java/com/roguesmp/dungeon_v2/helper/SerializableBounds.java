package com.roguesmp.dungeon_v2.helper;

import org.bukkit.util.BoundingBox;

/**
 * Gson-safe representation of a BoundingBox.
 * Bukkit's BoundingBox cannot be serialized directly.
 */
public class SerializableBounds {

    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;

    public SerializableBounds() {}

    public SerializableBounds(double minX, double minY, double minZ,
                              double maxX, double maxY, double maxZ) {
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
    }

    public static SerializableBounds from(BoundingBox box) {
        return new SerializableBounds(
                box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ()
        );
    }

    public BoundingBox toBukkit() {
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public boolean contains(double x, double y, double z) {
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public double getMinX() { return minX; }
    public double getMinY() { return minY; }
    public double getMinZ() { return minZ; }
    public double getMaxX() { return maxX; }
    public double getMaxY() { return maxY; }
    public double getMaxZ() { return maxZ; }
}