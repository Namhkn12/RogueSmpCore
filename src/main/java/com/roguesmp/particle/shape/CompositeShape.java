package com.roguesmp.particle.shape;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

// Combines multiple shapes into one (e.g., Double Helix)
public class CompositeShape implements ParticleShape {
    private final List<ParticleShape> shapes = new ArrayList<>();

    public CompositeShape add(ParticleShape shape) {
        shapes.add(shape);
        return this;
    }

    @Override
    public List<Vector> getPoints(int tick) {
        List<Vector> allPoints = new ArrayList<>();
        for (ParticleShape s : shapes) allPoints.addAll(s.getPoints(tick));
        return allPoints;
    }
}