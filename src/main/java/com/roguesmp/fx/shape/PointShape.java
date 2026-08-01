package com.roguesmp.fx.shape;

import org.bukkit.util.Vector;

import java.util.List;

/** A single point at the shape's local origin — useful for anchoring one display entity or particle. */
public final class PointShape implements FxShape {

    private static final List<Vector> ORIGIN = List.of(new Vector(0, 0, 0));

    @Override
    public List<Vector> points(int tick) {
        return ORIGIN;
    }
}
