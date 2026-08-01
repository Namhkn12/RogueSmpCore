package com.roguesmp.fx.motion;

import com.roguesmp.fx.FxTransform;
import org.joml.Vector3f;

import java.util.function.Supplier;

/** Steers toward a live target at a constant speed. The target is resolved fresh every tick, so it can be a moving thing. */
public final class SeekMotion implements FxMotion {

    private final Supplier<Vector3f> target;
    private final float speedPerTick;

    public SeekMotion(Supplier<Vector3f> target, float speedPerTick) {
        this.target = target;
        this.speedPerTick = speedPerTick;
    }

    @Override
    public FxTransform step(FxTransform current, int tick) {
        Vector3f targetPos = target.get();
        Vector3f toTarget = new Vector3f(targetPos).sub(current.position());

        float distance = toTarget.length();
        if (distance <= speedPerTick || distance == 0f) {
            return new FxTransform(new Vector3f(targetPos), current.rotation(), current.scale());
        }

        Vector3f step = toTarget.normalize().mul(speedPerTick);
        Vector3f newPosition = current.position().add(step);
        return new FxTransform(newPosition, current.rotation(), current.scale());
    }
}
