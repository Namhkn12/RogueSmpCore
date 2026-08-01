package com.roguesmp.fx.shape;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * A static geometric shape: the set of local-space points it occupies at a given tick. Shapes are
 * plain geometry — movement/rotation over time is handled separately by {@code FxMotion}, not here.
 */
@FunctionalInterface
public interface FxShape {

    List<Vector> points(int tick);

    /** Merges this shape's points with another's, forming one combined point cloud. */
    default FxShape combine(FxShape other) {
        return tick -> {
            List<Vector> combined = new ArrayList<>(this.points(tick));
            combined.addAll(other.points(tick));
            return combined;
        };
    }
}
