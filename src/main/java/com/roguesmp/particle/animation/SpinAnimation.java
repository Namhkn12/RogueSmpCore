package com.roguesmp.particle.animation;

import com.roguesmp.particle.RotationMatrix;
import org.bukkit.util.Vector;

public class SpinAnimation implements ParticleAnimation {

    private final double anglePerTick;
    private RotationMatrix matrix;

    public SpinAnimation(double anglePerTick) {
        this.anglePerTick = anglePerTick;
    }

    @Override
    public void prepare(int tick, Vector direction) {
        double angle = tick * anglePerTick;
        matrix = new RotationMatrix(direction, angle);
    }

    @Override
    public Vector apply(Vector point) {
        return matrix.apply(point);
    }
}
