package com.roguesmp.fx.motion;

import com.roguesmp.fx.FxTransform;
import org.joml.Vector3f;

/**
 * Moves by an internal velocity that's pulled down a fixed amount every tick — a thrown-debris arc
 * falls out naturally from the stepping instead of being computed as a curve to a landing spot.
 */
public final class GravityMotion implements FxMotion {

    private final float gravityPerTick;
    private Vector3f velocity;

    public GravityMotion(Vector3f initialVelocity, float gravityPerTick) {
        this.velocity = new Vector3f(initialVelocity);
        this.gravityPerTick = gravityPerTick;
    }

    @Override
    public FxTransform step(FxTransform current, int tick) {
        velocity = new Vector3f(velocity.x(), velocity.y() - gravityPerTick, velocity.z());
        Vector3f newPosition = current.position().add(velocity);
        return new FxTransform(newPosition, current.rotation(), current.scale());
    }
}
