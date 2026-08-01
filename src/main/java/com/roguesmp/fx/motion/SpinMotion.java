package com.roguesmp.fx.motion;

import com.roguesmp.fx.FxTransform;
import org.apache.commons.math3.util.FastMath;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Rotates continuously around an axis at a fixed angular speed, indefinitely. */
public final class SpinMotion implements FxMotion {

    private final Vector3f axis;
    private final float degreesPerTick;

    public SpinMotion(Vector3f axis, float degreesPerTick) {
        this.axis = new Vector3f(axis).normalize();
        this.degreesPerTick = degreesPerTick;
    }

    @Override
    public FxTransform step(FxTransform current, int tick) {
        Quaternionf delta = new Quaternionf().rotateAxis((float) FastMath.toRadians(degreesPerTick), axis);
        Quaternionf newRotation = delta.mul(current.rotation());
        return new FxTransform(current.position(), newRotation, current.scale());
    }
}
