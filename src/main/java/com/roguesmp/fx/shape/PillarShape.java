package com.roguesmp.fx.shape;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A vertical column of points from the origin up to {@code height}, each independently randomized
 * along that axis (uniform, not a Gaussian spread) rather than fixed/evenly spaced - re-rolled
 * fresh every {@link #points(int)} call, so a {@code once()} {@link com.roguesmp.fx.FxPart} using
 * this scatters {@code pointCount} particles at random heights each time it fires.
 */
public final class PillarShape implements FxShape {

    private final double height;
    private final int pointCount;

    public PillarShape(double height, int pointCount) {
        this.height = height;
        this.pointCount = pointCount;
    }

    @Override
    public List<Vector> points(int tick) {
        List<Vector> points = new ArrayList<>(pointCount);
        for (int i = 0; i < pointCount; i++) {
            points.add(new Vector(0, ThreadLocalRandom.current().nextDouble(height), 0));
        }
        return points;
    }
}
