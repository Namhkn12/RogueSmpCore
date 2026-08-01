package com.roguesmp.fx.motion;

import com.roguesmp.fx.FxTransform;
import org.apache.commons.math3.util.FastMath;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Supplier;

/** Circles around a center at a fixed radius and angular speed. The center is resolved fresh every tick, so it can itself move. */
public final class OrbitMotion implements FxMotion {

    private final Supplier<Vector3f> center;
    private final float radius;
    private final float degreesPerTick;
    private final Vector3f axis;
    private float angleDeg;

    public OrbitMotion(Supplier<Vector3f> center, float radius, float degreesPerTick) {
        this(center, radius, degreesPerTick, new Vector3f(0, 1, 0), 0f);
    }

    public OrbitMotion(Supplier<Vector3f> center, float radius, float degreesPerTick, Vector3f axis, float startAngleDeg) {
        this.center = center;
        this.radius = radius;
        this.degreesPerTick = degreesPerTick;
        this.axis = new Vector3f(axis).normalize();
        this.angleDeg = startAngleDeg;
    }

    @Override
    public FxTransform step(FxTransform current, int tick) {
        angleDeg += degreesPerTick;
        Quaternionf rotation = new Quaternionf().rotateAxis((float) FastMath.toRadians(angleDeg), axis);
        Vector3f offset = rotation.transform(new Vector3f(radius, 0, 0));
        Vector3f position = new Vector3f(center.get()).add(offset);
        return new FxTransform(position, rotation, current.scale());
    }
}
