package com.roguesmp.fx.motion;

import com.roguesmp.fx.FxTransform;
import org.joml.Vector3f;

/** Translates by a per-tick velocity. Mutable so callers can steer it live instead of committing to a direction upfront. */
public final class VelocityMotion implements FxMotion {

    private volatile Vector3f velocity;

    public VelocityMotion(Vector3f velocityPerTick) {
        this.velocity = new Vector3f(velocityPerTick);
    }

    public void setVelocity(Vector3f velocityPerTick) {
        this.velocity = new Vector3f(velocityPerTick);
    }

    public Vector3f getVelocity() {
        return new Vector3f(velocity);
    }

    @Override
    public FxTransform step(FxTransform current, int tick) {
        Vector3f newPosition = current.position().add(velocity);
        return new FxTransform(newPosition, current.rotation(), current.scale());
    }
}
